package com.munger.passwordkeeper;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.Nullable;
import androidx.test.espresso.ViewInteraction;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.android.material.shape.CutCornerTreatment;
import com.google.api.client.util.DateTime;
import com.munger.passwordkeeper.helpers.DriveHelper;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import com.munger.passwordkeeper.struct.Settings;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentStream;
import com.munger.passwordkeeper.view.AboutFragment;
import com.munger.passwordkeeper.view.CreateFileFragment;
import com.munger.passwordkeeper.view.SettingsFragment;
import com.munger.passwordkeeper.view.ViewDetailFragment;
import com.munger.passwordkeeper.view.ViewFileFragment;
import com.munger.passwordkeeper.view.widget.DetailItemWidget;
import com.munger.passwordkeeper.view.widget.TextInputWidget;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.test.espresso.DataInteraction;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.DisableOnAndroidDebug;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.munger.passwordkeeper.CustomMatchers.assertDoesExist;
import static com.munger.passwordkeeper.CustomMatchers.withIndex;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasXPath;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;


/**
 * Created by codymunger on 12/6/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FunctionalTest
{
    private MainStateDer mainState;
    public float appTimeout = Float.MAX_VALUE;

    public class SettingsDer extends Settings
    {
        public float timeout = appTimeout;

        @Override
        public float getTimeout()
        {
            return timeout;
        }

        public float getRealTimeout() {return super.getTimeout();}
    }

    public class NavDer extends NavigationHelper
    {
        public boolean exitCalled = false;

        @Override
        protected void doExit()
        {
            exitCalled = true;
        }
    }


    public Object docLock = new Object();
    public class PasswordDocumentFileDer extends PasswordDocumentFile
    {
        public PasswordDocumentFileDer(String name) {
            super(name);
        }

        public int saveCalled = 0;

        @Override
        public void save() throws Exception
        {
            super.save();

            synchronized (docLock)
            {
                System.out.println("save " + name + " called");
                saveCalled++;
                docLock.notify();
            }
        }
    }

    public PasswordDocumentFile remoteFile = null;

    public class DriveHelperDer extends DriveHelper
    {
        public PasswordDocumentFileDer doc;

        @Override
        public void connect()
        {
            connected = true;

            synchronized (lock)
            {
                lock.notify();
            }
        }

        private String fileId = "testId";

        @Override
        public String getOrCreateFile(String name) throws IOException
        {
            return fileId;
        }

        public int metadataCalled = 0;
        public int getRemoteCalled = 0;
        public int updateRemoteCalled = 0;

        public DateTime lastModified = new DateTime(System.currentTimeMillis());
        public boolean isTrashed = false;
        public long size = 0;

        @Override
        public Meta getMetadata(String fileId) throws IOException
        {
            assertSame(fileId, this.fileId);
            Meta ret = new Meta();
            ret.modified = lastModified;

            if (remoteFile != null)
            {
                File remotePath = new File(remoteFile.getHistoryFilePath());
                ret.size = remotePath.length();
            }
            else
                ret.size = 0;

            ret.trashed = isTrashed;
            metadataCalled++;

            return ret;
        }

        public void trashRemoteFile(String fileId, boolean trashed) throws Exception
        {
            isTrashed = trashed;
        }

        @Override
        public InputStream getRemoteFile(String fileId) throws IOException
        {
            assertSame(fileId, this.fileId);

            if (remoteFile == null)
                return null;

            File remotePath = new File(remoteFile.getHistoryFilePath());
            getRemoteCalled++;
            return new FileInputStream(remotePath);
        }

        @Override
        public DateTime updateRemoteByPath(String fileId, String path) throws IOException
        {
            assertSame(fileId, this.fileId);

            if (remoteFile == null)
                throw new IOException("failed to update remote file");

            copyFile(path, remoteFile.getHistoryFilePath());
            updateRemoteCalled++;

            return new DateTime(System.currentTimeMillis());
        }
    }

    void copyFile(String inpath, String outpath) throws IOException
    {
        File out = new File(outpath);
        FileOutputStream fos = new FileOutputStream(out);
        File in = new File(inpath);
        FileInputStream fis = new FileInputStream(in);

        int chunk = 4098;
        long sz = in.length();
        long idx = 0;
        byte[] buf = new byte[chunk];
        while (idx < sz)
        {
            long remaining = sz - idx;
            int readSz = (int) Math.min(chunk, remaining);
            fis.read(buf, 0, readSz);
            fos.write(buf, 0, readSz);

            idx += readSz;
        }

        fis.close();
        fos.close();
    }

    PasswordDocumentFileDer localDoc = null;

    public class MainStateDer extends MainState
    {
        protected SettingsDer mySettings;
        public void updateTimeout(float minutes)
        {
            mySettings.timeout = minutes;
            quitTimer.reset();
        }

        @Override
        protected void setupPreferences()
        {
            mySettings = new SettingsDer();
            settings = mySettings;
        }

        protected NavDer myNavigationHelper;
        @Override
        protected void setupNavigation()
        {
            myNavigationHelper = new NavDer();
            navigationHelper = myNavigationHelper;
        }

        protected DriveHelperDer myDriveHelper;
        @Override
        protected DriveHelper createDriveHelper()
        {
            if (myDriveHelper == null)
                myDriveHelper = new DriveHelperDer();

            return myDriveHelper;
        }

        @Override
        protected PasswordDocument createDocument()
        {
            return localDoc;
        }

        @Override
        public void setupDocument()
        {
            if (localDoc != null)
                document = localDoc;

            super.setupDocument();
        }
    }

    //@Rule
    //public Timeout globalTimeout = Timeout.seconds(20); // 10 seconds max per method tested

    @Rule public TestName testName = new TestName();

    public class ActivityRule extends ActivityTestRule
    {
        public ActivityRule()
        {
            super(TestingMainActivity.class);
        }

        @Override
        protected void afterActivityLaunched()
        {
            super.afterActivityLaunched();

            String meth = testName.getMethodName();
            if (!meth.contains("emote"))
                try {before(); } catch(Exception e){}
        }

        @Override
        protected void afterActivityFinished()
        {
            mainState.quitTimer.stop();
            super.afterActivityFinished();
        }

        @Override
        protected Intent getActivityIntent()
        {
            Intent i = super.getActivityIntent();
            return i;
        }
    }

    public class DebugTimeoutRule implements TestRule
    {
        public DebugTimeoutRule() {  }

        @Override
        public Statement apply(Statement base, org.junit.runner.Description description)
        {
            appTimeout = 0.33f;
            return base;
        }
    }

    @Rule
    public ActivityTestRule<TestingMainActivity> activityRule = new ActivityRule();

    @Rule
    public TestRule timeoutRule = new DisableOnAndroidDebug(new DebugTimeoutRule());

    public void before() throws Exception
    {
        TestingMainActivity activity = activityRule.getActivity();

        if (mainState == null)
            mainState = new MainStateDer();

        MainState.setInstance(mainState);

        mainState.setContext(activity, activity);

        if (mainState.mySettings.getSaveToCloud() == true);
            mainState.mySettings.setSaveToCloud(false);

        if (mainState.document == null && localDoc == null)
        {
            localDoc = new PasswordDocumentFileDer("local");
            localDoc.setRootPath(activity.getFilesDir().getAbsolutePath());
            localDoc.deleteFiles();
        }

        if (mainState.document == null)
        {
            mainState.setupDocument();
        }
    }

    @After
    public void after() throws Exception
    {
        localDoc = null;
        remoteFile = null;
    }

    @Test
    public void createFileFail() throws Exception
    {
        localDoc.delete();
        activityRule.getActivity().superInit();

        CustomMatchers.assertDoesExist(onView(withId(R.id.createfile_password1ipt)));
        onView(withId(R.id.createfile_password1ipt)).perform(typeText("pass"));
        onView(withId(R.id.createfile_password2ipt)).perform(typeText("no match"), closeSoftKeyboard());
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        onView(allOf(withClassName(containsString("Button")), withText(R.string.alert_okay))).perform(click());

        onView(withId(R.id.createfile_password1ipt)).perform(clearText()).perform(typeText("pass"));
        onView(withId(R.id.createfile_password2ipt)).perform(clearText()).perform(typeText("pass"), closeSoftKeyboard());
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        onView(allOf(withClassName(containsString("Button")), withText(R.string.alert_okay))).check(doesNotExist());
    }

    @Test
    public void openFileFail() throws Exception
    {
        String password = "pass";
        localDoc.setPassword(password);
        localDoc.save();

        activityRule.getActivity().superInit();
        CustomMatchers.assertDoesExist(onView(withClassName(containsString("EditText"))));
        onView(withClassName(containsString("EditText"))).perform(typeText("wrongpass"), closeSoftKeyboard());
        onView(allOf(withClassName(containsString("Button")), withText(R.string.input_okay))).perform(click());

        onView(allOf(withClassName(containsString("Button")), withText(R.string.alert_okay))).perform(click());


        onView(withClassName(containsString("EditText"))).perform(clearText()).perform(typeText(password), closeSoftKeyboard());
        onView(allOf(withClassName(containsString("Button")), withText(R.string.input_okay))).perform(click());

        onView(allOf(withClassName(containsString("Button")), withText(R.string.alert_okay))).check(doesNotExist());

    }

    @Test
    public void createFileEditDefault() throws Exception
    {
        //password view
        activityRule.getActivity().superInit();
        CustomMatchers.assertDoesExist(onView(withId(R.id.createfile_password1ipt)));
        onView(withId(R.id.createfile_password1ipt)).perform(clearText(), typeText("pass"));
        onView(withId(R.id.createfile_password2ipt)).perform(clearText(), typeText("pass"), closeSoftKeyboard());
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        //document view
        CustomMatchers.assertInView((MainActivity) mainState.activity, ViewFileFragment.class);
        onData(is(instanceOf(PasswordDetails.class))).atPosition(0).check(matches(hasDescendant(withText(R.string.new_entry_title))));

        boolean thrown = false;

        try
        {
            onData(is(instanceOf(PasswordDetails.class))).atPosition(1).check(doesNotExist());
        }
        catch(Exception e){
            thrown = true;
        }

        assertTrue(thrown);
    }

    public static Matcher<View> pairWithKey(final String key)
    {
        return new TypeSafeMatcher<View>()
        {
            int currentIndex = 0;

            @Override
            public void describeTo(Description description)
            {
                description.appendText("pair widget with key: ");
                description.appendValue(key);
            }

            @Override
            public boolean matchesSafely(View view)
            {
                if (!(view instanceof DetailItemWidget))
                    return false;

                String wkey = ((DetailItemWidget)view).getKey();
                if (wkey == null)
                    return false;

                return wkey.equals(key);
            }
        };
    }

    public static Matcher<View> pairWithValue(final String value)
    {
        return new TypeSafeMatcher<View>()
        {
            int currentIndex = 0;

            @Override
            public void describeTo(Description description)
            {
                description.appendText("pair widget with value: ");
                description.appendValue(value);
            }

            @Override
            public boolean matchesSafely(View view)
            {
                if (!(view instanceof DetailItemWidget))
                    return false;

                String wval = ((DetailItemWidget)view).getValue();
                if (wval == null)
                    return false;

                return wval.equals(value);
            }
        };
    }

    public static Matcher<View> inputWithHint(final String value)
    {
        return new TypeSafeMatcher<View>()
        {
            @Override
            protected boolean matchesSafely(View view)
            {
                if (!(view instanceof TextInputWidget))
                    return false;

                String hintVal = ((TextInputWidget)view).getInput().getHint().toString();
                if (hintVal == null)
                    return false;

                return hintVal.equals(value);
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText("input widget with hint: ");
                description.appendValue(value);
            }
        };
    }

    private String name = "a name";
    private String location = "a location";
    private String detKey1 = "a key 1";
    private String detValue1 = "a value 1";
    private String detKey2 = "a key 2";
    private String detValue2 = "a value 2";

    private void createFileEditNewEntry() throws Exception
    {
        //password view
        activityRule.getActivity().superInit();
        CustomMatchers.assertDoesExist(onView(withId(R.id.createfile_password1ipt)));
        onView(withId(R.id.createfile_password1ipt)).perform(clearText(), typeText("pass"));
        onView(withId(R.id.createfile_password2ipt)).perform(clearText(), typeText("pass"), closeSoftKeyboard());
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        //document view
        CustomMatchers.assertInView((MainActivity) mainState.activity, ViewFileFragment.class);
        onData(is(instanceOf(PasswordDetails.class))).atPosition(0).perform(click());

        //details view
        CustomMatchers.assertInView((MainActivity) mainState.activity, ViewDetailFragment.class);
        ViewDetailFragment detFrag = (ViewDetailFragment) ((MainActivity) mainState.activity).getCurrentFagment();
        assertEquals(true, detFrag.getEditable());

        onView(withId(R.id.viewdetail_namelbl)).check(matches(hasDescendant(withText(""))));
        onView(withId(R.id.viewdetail_locationlbl)).check(matches(hasDescendant(withText(""))));
        DataInteraction inter = onData(is(instanceOf(PasswordDetailsPair.class))).atPosition(0);
        inter.check(matches(allOf(pairWithKey(""), pairWithValue(""))));

        CustomMatchers.assertHiddenOrDoesNotExist(onData(is(instanceOf(PasswordDetailsPair.class))).atPosition(1));

        //edit details
        CustomMatchers.assertDoesExist(onView(allOf(isDescendantOfA(withId(R.id.viewdetail_namelbl)),withClassName(containsString("EditText")))));
        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_namelbl)),withClassName(containsString("EditText")))).perform(clearText(), typeText(name));
        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_locationlbl)),withClassName(containsString("EditText")))).perform(clearText(), typeText(location));
        DataInteraction dint = onData(is(instanceOf(PasswordDetailsPair.class))).atPosition(0);
        DataInteraction din2 = dint.onChildView(allOf(withHint("Key"), withClassName(containsString("EditText"))));
        din2.perform(clearText(), typeText(detKey1));
        Thread.sleep(25);
        din2 = dint.onChildView(allOf(withHint("Value"), withClassName(containsString("EditText"))));
        din2.perform(clearText(), typeText(detValue1), closeSoftKeyboard());
        Thread.sleep(25);

        onView(withId(R.id.viewdetail_addbtn)).perform(click());
        dint = onData(is(instanceOf(PasswordDetailsPair.class))).atPosition(1);
        din2 = dint.onChildView(allOf(withHint("Key"), withClassName(containsString("EditText"))));
        din2.perform(clearText(), typeText(detKey2));
        Thread.sleep(25);
        din2 = dint.onChildView(allOf(withHint("Value"), withClassName(containsString("EditText"))));
        din2.perform(clearText(), typeText(detValue2), closeSoftKeyboard());
    }

    @Test
    public void createFileEditNewEntryAndSave() throws Exception
    {
        createFileEditNewEntry();

        //back out
        activityRule.getActivity().resetBackCalledCount();
        while (activityRule.getActivity().getBackTriggeredCount() == 0)
        {
            pressBack();
            Thread.sleep(50);
        }

        onView(withText(R.string.confirm_okay)).perform(click());

        //document view
        CustomMatchers.assertInView((MainActivity) mainState.activity, ViewFileFragment.class);
        assertDoesExist(onData(is(instanceOf(PasswordDetails.class))).atPosition(0));
        onData(is(instanceOf(PasswordDetails.class))).atPosition(0).check(matches(hasDescendant(withText(name))));

        boolean thrown = false;

        try
        {
            onData(is(instanceOf(PasswordDetails.class))).atPosition(1).check(doesNotExist());
        }
        catch(Exception e){
            thrown = true;
        }

        assertTrue(thrown);
    }

    @Test
    public void createFileEditNewEntryAndDiscard() throws Exception
    {
        createFileEditNewEntry();

        //back out
        activityRule.getActivity().resetBackCalledCount();
        while (activityRule.getActivity().getBackTriggeredCount() == 0)
        {
            pressBack();
            Thread.sleep(50);
        }

        onView(withText(R.string.confirm_discard)).perform(click());

        //document view
        CustomMatchers.assertInView((MainActivity) mainState.activity, ViewFileFragment.class);
        onData(is(instanceOf(PasswordDetails.class))).atPosition(0).check(matches(hasDescendant(withText(R.string.new_entry_title))));

        boolean thrown = false;

        try
        {
            onData(is(instanceOf(PasswordDetails.class))).atPosition(1).check(doesNotExist());
        }
        catch(Exception e){
            thrown = true;
        }

        assertTrue(thrown);
    }

    @Test
    public void createFileBackOut() throws Exception
    {
        //password view
        activityRule.getActivity().superInit();

        Log.d("password keeper", "debug password1ipt " + R.id.createfile_password1ipt);
        CustomMatchers.assertDoesExist(onView(withId(R.id.createfile_password1ipt)));
        onView(withId(R.id.createfile_password1ipt)).perform(clearText(), typeText("pass"));
        onView(withId(R.id.createfile_password2ipt)).perform(clearText(), typeText("pass"), closeSoftKeyboard());
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        //document view
        CustomMatchers.assertInView((MainActivity) mainState.activity, ViewFileFragment.class);

        //back out
        int tries = 0;
        activityRule.getActivity().resetBackCalledCount();
        while (activityRule.getActivity().getBackCalledCount() == 0 && tries < 5)
        {
            pressBack();
            Thread.sleep(50);
            tries++;
        }

        assertFalse(activityRule.getActivity().getDoexitCalled());
    }

    private String existingTitle = "name";

    protected void createExistingFile() throws Exception
    {
        String password = "pass";
        mainState.document.setPassword(password);
        String path = MainState.getInstance().context.getFilesDir().getAbsolutePath() + "/";

        createExistingFile(mainState.document, path);
    }

    protected void createExistingFile(PasswordDocument target, String path) throws Exception
    {
        if (target instanceof PasswordDocumentFile)
        {
            ((PasswordDocumentFile) target).setRootPath(path);
            ((PasswordDocumentFile) target).delete();
        }

        PasswordDetails dets1 = new PasswordDetails();
        dets1.setName(existingTitle);
        dets1.setLocation("location");
        dets1.addPair(new PasswordDetailsPair(detKey1, detValue1));
        dets1.addPair(new PasswordDetailsPair(detKey2, detValue2));
        target.addDetails(dets1);

        target.save();
    }

    protected void openExistingFile() throws Exception
    {
        //password view
        activityRule.getActivity().superInit();

        assertDoesExist(onView(allOf(withClassName(containsString("EditText")))));
        onView(allOf(withClassName(containsString("EditText")))).perform(typeText("pass"), closeSoftKeyboard());
        onView(allOf(withClassName(containsString("Button")), withText("Okay"))).perform(click());
    }

    protected void createAndOpenExistingFile() throws Exception
    {
        createExistingFile();
        openExistingFile();
        CustomMatchers.assertInView((MainActivity) mainState.activity, ViewFileFragment.class);
    }

    private interface RepeatAction
    {
        void perform();
    }

    private void awaitNextFragment(Class cls, RepeatAction action) throws InterruptedException
    {
        Thread.sleep(150);

        int attempt = 0;
        Fragment frag;
        do {
            if (action != null)
                action.perform();

            Thread.sleep(100);

            //document view
            MainActivity activity = (MainActivity) MainState.getInstance().activity;
            frag = activity.getCurrentFagment();
            attempt++;
        } while ((frag == null || !(frag.getClass().equals(cls))) && attempt < 10);

        assertTrue(frag.getClass().equals(cls));
    }

    @Test
    public void openFileEditExistingEntryCreateNewEntry() throws Exception
    {
        createAndOpenExistingFile();

        onView(withId(R.id.action_edit)).perform(click());
        onView(withId(R.id.viewfile_addbtn)).perform(click());

        //details view
        CustomMatchers.assertDoesExist(onView(allOf(isDescendantOfA(withId(R.id.viewdetail_namelbl)),withClassName(containsString("EditText")))));
        String newName = "new name";
        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_namelbl)),withClassName(containsString("EditText")))).perform(clearText(), typeText(newName));
        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_locationlbl)),withClassName(containsString("EditText")))).perform(clearText(), typeText(location));
        DataInteraction dint = onData(is(instanceOf(PasswordDetailsPair.class))).atPosition(0);
        DataInteraction din2 = dint.onChildView(allOf(withHint("Key"), withClassName(containsString("EditText"))));
        din2.perform(clearText(), typeText(detKey1));
        Thread.sleep(25);
        din2 = dint.onChildView(allOf(withHint("Value"), withClassName(containsString("EditText"))));
        din2.perform(clearText(), typeText(detValue1), closeSoftKeyboard());
        Thread.sleep(25);

        //back out
        activityRule.getActivity().resetBackCalledCount();
        while (activityRule.getActivity().getBackTriggeredCount() == 0)
        {
            pressBack();
            Thread.sleep(50);
        }

        assertDoesExist(onView(withText(R.string.confirm_okay)));
        onView(withText(R.string.confirm_okay)).perform(click());
        assertDoesExist(onData(is(instanceOf(PasswordDetails.class))).atPosition(1));
        onData(is(instanceOf(PasswordDetails.class))).atPosition(1).check(matches(hasDescendant(withText(newName))));

        boolean thrown = false;

        try
        {
            onData(is(instanceOf(PasswordDetails.class))).atPosition(2).check(doesNotExist());
        }
        catch(Exception e){
            thrown = true;
        }

        assertTrue(thrown);
    }

    @Test
    public void openFileSearchEntry() throws Exception
    {
        String password = "pass";
        String searchShortTitle = "search";
        String searchTitle = "searchable";
        mainState.document.setPassword(password);
        mainState.document.load(true);
        int randIdx = new Random().nextInt(10);
        int randIdx2 = new Random().nextInt(10) + 10;

        for (int i = 0; i < 20; i++)
        {
            PasswordDetails dets1 = new PasswordDetails();

            if (i == randIdx)
                dets1.setName(searchTitle);
            else if (i == randIdx2)
                dets1.setName(searchShortTitle);
            else
                dets1.setName("name" + i);

            mainState.document.addDetails(dets1);
        }

        mainState.document.save();

        //password view
        activityRule.getActivity().superInit();
        CustomMatchers.assertDoesExist(onView(allOf(withClassName(containsString("EditText")))));
        onView(allOf(withClassName(containsString("EditText")))).perform(typeText("pass"), closeSoftKeyboard());
        onView(allOf(withClassName(containsString("Button")), withText("Okay"))).perform(click());

        //document view
        onView(allOf(withId(R.id.action_search))).perform(click());
        onView(isAssignableFrom(EditText.class)).perform(typeText(searchShortTitle), closeSoftKeyboard());

        onData(is(instanceOf(PasswordDetails.class))).atPosition(0).check(matches(hasDescendant(withText(searchTitle))));
        onData(is(instanceOf(PasswordDetails.class))).atPosition(1).check(matches(hasDescendant(withText(searchShortTitle))));

        boolean thrown = false;

        try
        {
            onData(is(instanceOf(PasswordDetails.class))).atPosition(2).check(doesNotExist());
        }
        catch(Exception e){
            thrown = true;
        }

        assertTrue(thrown);
    }

    @Test
    public void timeout() throws Exception
    {
        createAndOpenExistingFile();

        mainState.updateTimeout(0.05f);
        Thread.sleep(4000);

        assertTrue(mainState.myNavigationHelper.exitCalled);
    }

    @Test
    public void settingsTimeoutChange() throws Exception
    {
        createAndOpenExistingFile();

        onView(withId(R.id.action_settings)).perform(click());

        onView(withText(R.string.settings_timeout_title)).perform(click());
        Resources res = MainState.getInstance().activity.getApplicationContext().getResources();
        String[] arr = res.getStringArray(R.array.timeoutStrings);
        String[] arrvals = res.getStringArray(R.array.timeoutValues);

        onView(withText(arr[1])).perform(click());

        Thread.sleep(50);
        float timeout = mainState.mySettings.getRealTimeout();
        float timeoutVal = Float.parseFloat(arrvals[1]);
        assertTrue(Math.abs(timeout - timeoutVal) < 0.0001);
    }

    @Test
    public void settingsBackOut() throws Exception
    {
        createAndOpenExistingFile();

        onView(withId(R.id.action_settings)).perform(click());

        pressBack();

        CustomMatchers.assertInView((MainActivity) mainState.activity, ViewFileFragment.class);
    }

    @Test
    public void changePassword() throws Exception
    {
        createAndOpenExistingFile();

        onView(withId(R.id.action_settings)).perform(click());

        onView(withText(R.string.settings_password_title)).perform(click());

        //change password view
        CustomMatchers.assertDoesExist(onView(withId(R.id.createfile_oldpasswordipt)));
        onView(withId(R.id.createfile_oldpasswordipt)).perform(typeText("pass"));
        onView(withId(R.id.createfile_password1ipt)).perform(typeText("password"));
        onView(withId(R.id.createfile_password2ipt)).perform(typeText("password"), closeSoftKeyboard());
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        //settings view
        CustomMatchers.assertInView((MainActivity) mainState.activity, SettingsFragment.class);

        //back out
        int tries = 0;
        activityRule.getActivity().resetBackCalledCount();
        while (activityRule.getActivity().getBackCalledCount() == 0 && tries < 5)
        {
            pressBack();
            Thread.sleep(50);
            tries++;
        }

        //document view
        CustomMatchers.assertInView((MainActivity) mainState.activity, ViewFileFragment.class);
    }

    private void selectAccountIfNeeded() throws Exception
    {
        if (Build.VERSION.SDK_INT >= 23) {
            UiDevice device = UiDevice.getInstance(getInstrumentation());
            UiObject allowPermissions = device.findObject(new UiSelector().text("mungerc@gmail.com"));
            if (allowPermissions.exists()) {
                try {
                    allowPermissions.click();
                } catch (UiObjectNotFoundException e) {
                }
            }
        }
    }

    private void allowPermissionsIfNeeded() throws Exception
    {
        if (Build.VERSION.SDK_INT >= 23) {
            UiDevice device = UiDevice.getInstance(getInstrumentation());
            UiObject allowPermissions = device.findObject(new UiSelector().text("Allow"));
            if (allowPermissions.exists()) {
                try {
                    allowPermissions.click();
                } catch (UiObjectNotFoundException e) {
                }
            }
        }
    }

    protected boolean hasReadPermission = false;
    protected boolean hasWritePermission = false;
    protected boolean requestedReturned = false;

    protected void writeImportFile(String content, String fileName) throws Exception
    {
        final Object lock = new Object();
        final String[] permissions =
        {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        hasWritePermission = MainState.getInstance().navigationHelper.hasPermission(permissions[0]);
        hasReadPermission = MainState.getInstance().navigationHelper.hasPermission(permissions[1]);
        requestedReturned = false;
        if (!hasWritePermission)
        {
            MainState.getInstance().navigationHelper.requestPermissions(permissions, new NavigationHelper.Callback(){public void callback(Object result)
            {
                synchronized (lock)
                {
                    requestedReturned = true;
                    hasWritePermission = (boolean) result;
                    lock.notify();
                }
            }});

            allowPermissionsIfNeeded();

            synchronized (lock)
            {
                if (!requestedReturned)
                    lock.wait();
            }
        }

        if (!hasWritePermission)
            throw new IOException("No permission to write test file");
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String tmpPage = downloadDir.getAbsolutePath() + "/" + fileName;
        File tmpFile = new File(tmpPage);

        if (tmpFile.exists())
            tmpFile.delete();

        tmpFile.createNewFile();

        FileWriter fw = new FileWriter(tmpFile);
        fw.write(content);
        fw.flush();fw.close();
    }

    @Test
    public void importFile() throws Exception
    {
        createAndOpenExistingFile();

        onView(withId(R.id.action_settings)).perform(click());

        onView(withText(R.string.settings_import_title)).perform(click());
        allowPermissionsIfNeeded();

        String filename = "password-keeper-tmp";
        String firstEntryTitle = "test";
        StringBuilder sb = new StringBuilder();
        sb.append(firstEntryTitle);
        sb.append("\tuser1\tpass1\n");
        sb.append("\tuser2\tpass2\n");
        sb.append("\tuser3\tpass3\n");
        writeImportFile(sb.toString(), filename);
        Thread.sleep(50);

        onData(is("Download/")).perform(click());
        onData(is(filename)).perform(click());

        onView(withText(R.string.confirm_okay)).perform(click());

        //settings view
        CustomMatchers.assertInView((MainActivity) mainState.activity, SettingsFragment.class);

        //back out
        int tries = 0;
        activityRule.getActivity().resetBackCalledCount();
        while (activityRule.getActivity().getBackCalledCount() == 0 && tries < 10)
        {
            pressBack();
            Thread.sleep(100);
            tries++;
        }

        CustomMatchers.assertInView((MainActivity) mainState.activity, ViewFileFragment.class);

        onData(is(instanceOf(PasswordDetails.class))).atPosition(0).check(matches(hasDescendant(withText(existingTitle))));
        onData(is(instanceOf(PasswordDetails.class))).atPosition(1).check(matches(hasDescendant(withText(firstEntryTitle))));

        boolean thrown = false;

        try
        {
            onData(is(instanceOf(PasswordDetails.class))).atPosition(2).check(doesNotExist());
        }
        catch(Exception e){
            thrown = true;
        }

        assertTrue(thrown);
    }

    @Test
    public void importFileFail() throws Exception
    {
        createAndOpenExistingFile();

        onView(withId(R.id.action_settings)).perform(click());

        onView(withText(R.string.settings_import_title)).perform(click());
        allowPermissionsIfNeeded();

        String filename = "password-keeper-tmp";
        String firstEntryTitle = "test";
        StringBuilder sb = new StringBuilder();
        sb.append(firstEntryTitle);
        sb.append("\tuser1\tpass1\n");
        sb.append("\tthis\twill\tcause\ta\tparse\tfail\n");
        sb.append("\tuser3\tpass3\n");
        writeImportFile(sb.toString(), filename);

        onData(is("Download/")).perform(click());
        onData(is(filename)).perform(click());

        onView(withText(R.string.confirm_okay)).perform(click());


        boolean thrown = false;

        try
        {
            onView(withText(R.string.confirm_okay)).perform(click());
        }
        catch(Exception e){
            thrown = true;
        }

        assertTrue(thrown);


        CustomMatchers.assertInView((MainActivity) mainState.activity, SettingsFragment.class);

        pressBack();
        Thread.sleep(50);

        //document view
        CustomMatchers.assertInView((MainActivity) mainState.activity, ViewFileFragment.class);

        CustomMatchers.assertDoesExist(
            onData(is(instanceOf(PasswordDetails.class))).atPosition(0).check(matches(hasDescendant(withText(existingTitle))))
        );

        CustomMatchers.assertHiddenOrDoesNotExist(onData(is(instanceOf(PasswordDetails.class))).atPosition(1));
    }

    @Test
    public void about() throws Exception
    {
        createAndOpenExistingFile();

        onView(withId(R.id.action_settings)).perform(click());

        onView(withText(R.string.settings_about_title)).perform(click());


        CustomMatchers.assertInView((MainActivity) mainState.activity, AboutFragment.class);

        pressBack();

        CustomMatchers.assertInView((MainActivity) mainState.activity, SettingsFragment.class);
    }

    @Test
    public void deleteLocalYes() throws Exception
    {
        createAndOpenExistingFile();
        onView(withId(R.id.action_settings)).perform(click());

        onView(withText(R.string.settings_delete_title)).perform(click());

        onView(withText(R.string.alert_positive)).perform(click());


        CustomMatchers.assertInView((MainActivity) mainState.activity, CreateFileFragment.class);
        assertTrue(((CreateFileFragment) ((MainActivity) mainState.activity).getCurrentFagment()).getIsCreating());
    }

    @Test
    public void deleteLocalNo() throws Exception
    {
        createAndOpenExistingFile();
        onView(withId(R.id.action_settings)).perform(click());

        onView(withText(R.string.settings_delete_title)).perform(click());

        onView(withText(R.string.alert_negative)).perform(click());

        CustomMatchers.assertInView((MainActivity) mainState.activity, SettingsFragment.class);
    }

    private void doUpdate(PasswordDocument target, int index) throws Exception
    {
        PasswordDetails dets = target.getDetails(index);
        PasswordDetailsPair pair = dets.getPair(index);
        dets.setName("foo" + index);
        dets.setLocation("foo" + (index + 1));
        pair.setKey("foo3" + (index + 2));
        pair.setValue("foo4" + (index + 3));
    }

    @Test
    public void enableRemoteSync() throws Exception
    {
        before();
        if (mainState.mySettings.getSaveToCloud() == true);
            mainState.mySettings.setSaveToCloud(false);

        createAndOpenExistingFile();
        onView(withId(R.id.action_settings)).perform(click());
        onView(withText(R.string.settings_cloud_title)).perform(click());

        CustomMatchers.assertInView((MainActivity) mainState.activity, SettingsFragment.class);

        Thread.sleep(250);

        assertTrue(mainState.mySettings.getSaveToCloud() == true);
    }

    @Test
    public void restartWithEnabledRemoteSync() throws Exception
    {
        FragmentActivity activity = activityRule.getActivity();
        File foo = new File(activity.getFilesDir().getAbsolutePath() + "/local-history");
        PasswordDocumentFile base = new PasswordDocumentFile("local", "pass");
        createExistingFile(base, activity.getFilesDir().getAbsolutePath() + "/");
        base.save();

        Thread.sleep(300);
        remoteFile = new PasswordDocumentFile("remote", "pass");
        remoteFile.setRootPath(activity.getFilesDir().getAbsolutePath());
        copyFile(base.getHistoryFilePath(), remoteFile.getHistoryFilePath());

        localDoc = new PasswordDocumentFileDer("local");
        localDoc.setRootPath(activity.getFilesDir().getAbsolutePath());


        before();

        Thread.sleep(300);

        if (mainState.mySettings.getSaveToCloud() == false);
            mainState.mySettings.setSaveToCloud(true);

        assertTrue(mainState.mySettings.getSaveToCloud() == true);

        openExistingFile();
        Thread.sleep(100);

        assertTrue(mainState.myDriveHelper.metadataCalled > 0);
        assertTrue(mainState.myDriveHelper.getRemoteCalled > 0);
        assertTrue(mainState.myDriveHelper.updateRemoteCalled == 0);
    }

    protected void setupRemoteMismatch() throws Exception
    {
        FragmentActivity activity = activityRule.getActivity();
        File foo = new File(activity.getFilesDir().getAbsolutePath() + "/local-history");
        PasswordDocumentFile base = new PasswordDocumentFile("local", "pass");
        createExistingFile(base, activity.getFilesDir().getAbsolutePath() + "/");
        base.save();

        Thread.sleep(300);
        remoteFile = new PasswordDocumentFile("remote", "mismatch");
        createExistingFile(remoteFile, activity.getFilesDir().getAbsolutePath() + "/");

        localDoc = new PasswordDocumentFileDer("local");
        localDoc.setRootPath(activity.getFilesDir().getAbsolutePath());


        before();

        Thread.sleep(300);

        if (mainState.mySettings.getSaveToCloud() == false);
        mainState.mySettings.setSaveToCloud(true);

        assertTrue(mainState.mySettings.getSaveToCloud() == true);

        //password view
        activityRule.getActivity().superInit();
        assertDoesExist(onView(allOf(withClassName(containsString("EditText")))));
        onView(allOf(withClassName(containsString("EditText")))).perform(typeText("pass"), closeSoftKeyboard());
        onView(allOf(withClassName(containsString("Button")), withText("Okay"))).perform(click());
        Thread.sleep(300);

        assertTrue(mainState.myDriveHelper.metadataCalled > 0);
        assertTrue(mainState.myDriveHelper.getRemoteCalled > 0);
        assertTrue(mainState.myDriveHelper.updateRemoteCalled == 0);
    }

    @Test
    public void remoteMismatchDelete() throws Exception
    {
        setupRemoteMismatch();

        onView(allOf(withClassName(containsString("Button")), withText(R.string.nav_mismatched_remote_pw_overwrite))).perform(click());
        Thread.sleep(100);

        assertTrue(mainState.myDriveHelper.updateRemoteCalled > 0);
        assertTrue(remoteFile.testPassword("pass"));
        remoteFile.setPassword("pass");

        CustomMatchers.assertInView((MainActivity) mainState.activity, ViewFileFragment.class);
    }

    @Test
    public void remoteMismatchDisable() throws Exception
    {
        setupRemoteMismatch();

        onView(allOf(withClassName(containsString("Button")), withText(R.string.nav_mismatched_remote_pw_disable))).perform(click());
        Thread.sleep(100);

        assertTrue(mainState.mySettings.getSaveToCloud() == false);
        assertTrue(mainState.driveHelper.isConnected() == false);

        CustomMatchers.assertInView((MainActivity) mainState.activity, ViewFileFragment.class);
    }

    @Test
    public void remoteMismatchPasswordChange() throws Exception
    {
        setupRemoteMismatch();

        onView(allOf(withClassName(containsString("Button")), withText(R.string.nav_mismatched_remote_pw_change_local))).perform(click());
        Thread.sleep(100);

        mainState.myDriveHelper.getRemoteCalled = 0;

        assertDoesExist(onView(allOf(withClassName(containsString("EditText")), withHint("current password"))));
        onView(allOf(withClassName(containsString("EditText")), withHint("current password"))).perform(typeText("pass"));
        onView(allOf(withClassName(containsString("EditText")), withHint("password"))).perform(typeText("mismatch"));
        onView(allOf(withClassName(containsString("EditText")), withHint("re-enter password"))).perform(typeText("mismatch"), closeSoftKeyboard());
        onView(allOf(withClassName(containsString("Button")), withText("Okay"))).perform(click());

        Thread.sleep(100);

        assertTrue(mainState.myDriveHelper.getRemoteCalled > 0);

        CustomMatchers.assertInView((MainActivity) mainState.activity, ViewFileFragment.class);

        assertTrue(localDoc.testPassword("mismatch"));
        assertTrue(remoteFile.testPassword("mismatch"));
    }

    protected void remoteTrashed() throws Exception
    {
        FragmentActivity activity = activityRule.getActivity();
        File foo = new File(activity.getFilesDir().getAbsolutePath() + "/local-history");
        PasswordDocumentFile base = new PasswordDocumentFile("local", "pass");
        createExistingFile(base, activity.getFilesDir().getAbsolutePath() + "/");
        base.save();

        Thread.sleep(300);
        remoteFile = new PasswordDocumentFile("remote", "pass");
        remoteFile.setRootPath(activity.getFilesDir().getAbsolutePath());
        copyFile(base.getHistoryFilePath(), remoteFile.getHistoryFilePath());

        localDoc = new PasswordDocumentFileDer("local");
        localDoc.setRootPath(activity.getFilesDir().getAbsolutePath());


        before();
        Thread.sleep(300);
        if (mainState.mySettings.getSaveToCloud() == false);
        mainState.mySettings.setSaveToCloud(true);
        mainState.myDriveHelper.isTrashed = true;

        //password view
        activityRule.getActivity().superInit();

        CustomMatchers.assertDoesExist(onView(allOf(withClassName(containsString("EditText")))));
        onView(allOf(withClassName(containsString("EditText")))).perform(typeText("pass"), closeSoftKeyboard());
        onView(allOf(withClassName(containsString("Button")), withText("OKAY"))).perform(click());
    }

    @Test
    public void remoteTrashedDisable() throws Exception
    {
        remoteTrashed();

        CustomMatchers.assertDoesExist(onView(allOf(withClassName(containsString("Button")), withText(R.string.nav_trashed_remote_disable))));
        onView(allOf(withClassName(containsString("Button")), withText(R.string.nav_trashed_remote_disable))).perform(click());
        Thread.sleep(100);

        assertTrue(mainState.mySettings.getSaveToCloud() == false);
        assertTrue(mainState.driveHelper.isConnected() == false);
        assertTrue(mainState.myDriveHelper.isTrashed == true);

        CustomMatchers.assertInView((MainActivity) mainState.activity, ViewFileFragment.class);
    }

    @Test
    public void remoteTrashedReinstate() throws Exception
    {
        remoteTrashed();

        CustomMatchers.assertDoesExist(onView(allOf(withClassName(containsString("Button")), withText(R.string.nav_trashed_remote_untrash))));
        onView(allOf(withClassName(containsString("Button")), withText(R.string.nav_trashed_remote_untrash))).perform(click());
        Thread.sleep(100);

        assertTrue(mainState.mySettings.getSaveToCloud() == true);
        assertTrue(mainState.driveHelper.isConnected() == true);
        assertTrue(mainState.myDriveHelper.isTrashed == false);

        CustomMatchers.assertInView((MainActivity) mainState.activity, ViewFileFragment.class);
    }

    @Test
    public void remoteUpdate() throws Exception
    {
        FragmentActivity activity = activityRule.getActivity();
        File foo = new File(activity.getFilesDir().getAbsolutePath() + "/local-history");
        PasswordDocumentFile base = new PasswordDocumentFile("local", "pass");
        createExistingFile(base, activity.getFilesDir().getAbsolutePath() + "/");
        base.save();

        Thread.sleep(300);
        remoteFile = new PasswordDocumentFile("remote", "pass");
        remoteFile.setRootPath(activity.getFilesDir().getAbsolutePath());
        copyFile(base.getHistoryFilePath(), remoteFile.getHistoryFilePath());
        remoteFile.load(true);
        PasswordDetails newdets = new PasswordDetails();
        newdets.setName("newname");
        newdets.setLocation("newloc");
        remoteFile.addDetails(newdets);
        remoteFile.save();

        localDoc = new PasswordDocumentFileDer("local");
        localDoc.setRootPath(activity.getFilesDir().getAbsolutePath());
        assertTrue(!localDoc.equals(remoteFile));

        before();

        Thread.sleep(300);
        if (mainState.mySettings.getSaveToCloud() == false);
        mainState.mySettings.setSaveToCloud(true);

        activityRule.getActivity().superInit();
        CustomMatchers.assertDoesExist(onView(allOf(withClassName(containsString("EditText")))));
        onView(allOf(withClassName(containsString("EditText")))).perform(typeText("pass"), closeSoftKeyboard());
        onView(allOf(withClassName(containsString("Button")), withText("OKAY"))).perform(click());
        Thread.sleep(100);

        assertTrue(mainState.driveHelper.isConnected() == true);
        assertTrue(mainState.myDriveHelper.getRemoteCalled > 0);
        assertTrue(mainState.myDriveHelper.updateRemoteCalled == 0);
        assertTrue(localDoc.equals(remoteFile));
    }

    @Test
    public void localUpdateToRemote() throws Exception
    {
        FragmentActivity activity = activityRule.getActivity();
        File foo = new File(activity.getFilesDir().getAbsolutePath() + "/local-history");
        PasswordDocumentFile base = new PasswordDocumentFile("local", "pass");
        createExistingFile(base, activity.getFilesDir().getAbsolutePath() + "/");
        base.save();

        Thread.sleep(300);
        remoteFile = new PasswordDocumentFile("remote", "pass");
        remoteFile.setRootPath(activity.getFilesDir().getAbsolutePath());
        copyFile(base.getHistoryFilePath(), remoteFile.getHistoryFilePath());

        localDoc = new PasswordDocumentFileDer("local");
        localDoc.setRootPath(activity.getFilesDir().getAbsolutePath());

        before();

        Thread.sleep(300);
        if (mainState.mySettings.getSaveToCloud() == false);
        mainState.mySettings.setSaveToCloud(true);

        activityRule.getActivity().superInit();
        CustomMatchers.assertDoesExist(onView(allOf(withClassName(containsString("EditText")))));
        onView(allOf(withClassName(containsString("EditText")))).perform(typeText("pass"), closeSoftKeyboard());
        onView(allOf(withClassName(containsString("Button")), withText("OKAY"))).perform(click());
        Thread.sleep(100);

        assertTrue(mainState.driveHelper.isConnected() == true);
        assertTrue(mainState.myDriveHelper.getRemoteCalled > 0);
        assertTrue(mainState.myDriveHelper.updateRemoteCalled == 0);
        remoteFile.load(true);
        assertTrue(localDoc.equals(remoteFile));

        onView(withId(R.id.action_edit)).perform(click());
        onView(withId(R.id.viewfile_addbtn)).perform(click());
        ViewInteraction din2 = onView(allOf(withHint("Detail name"), withClassName(containsString("EditText"))));
        din2.perform(clearText(), typeText("localdetail"));
        Thread.sleep(25);
        din2 = onView(allOf(withHint("URL / Location"), withClassName(containsString("EditText"))));
        din2.perform(clearText(), typeText("localLocation"), closeSoftKeyboard());
        onView(withId(R.id.action_edit)).perform(click());
        onView(allOf(withClassName(containsString("Button")), withText(R.string.alert_okay))).perform(click());
        Thread.sleep(100);

        assertTrue(localDoc.saveCalled > 0);
        assertTrue(mainState.driveHelper.isConnected() == true);
        assertTrue(mainState.myDriveHelper.updateRemoteCalled > 1);
        remoteFile = new PasswordDocumentFile("remote", "pass");
        remoteFile.setRootPath(activity.getFilesDir().getAbsolutePath());
        remoteFile.load(true);
        assertTrue(localDoc.equals(remoteFile));
    }

    @Test
    public void remoteAndLocalUpdate() throws Exception
    {
        FragmentActivity activity = activityRule.getActivity();
        File foo = new File(activity.getFilesDir().getAbsolutePath() + "/local-history");
        PasswordDocumentFile base = new PasswordDocumentFile("local", "pass");
        createExistingFile(base, activity.getFilesDir().getAbsolutePath() + "/");
        base.save();

        Thread.sleep(300);
        remoteFile = new PasswordDocumentFile("remote", "pass");
        remoteFile.setRootPath(activity.getFilesDir().getAbsolutePath());
        copyFile(base.getHistoryFilePath(), remoteFile.getHistoryFilePath());
        remoteFile.load(true);
        PasswordDetails newdets = new PasswordDetails();
        newdets.setName("newname");
        newdets.setLocation("newloc");
        remoteFile.addDetails(newdets);
        remoteFile.save();

        localDoc = new PasswordDocumentFileDer("local");
        localDoc.setRootPath(activity.getFilesDir().getAbsolutePath());
        assertTrue(!localDoc.equals(remoteFile));

        before();

        Thread.sleep(300);
        if (mainState.mySettings.getSaveToCloud() == false);
        mainState.mySettings.setSaveToCloud(true);

        activityRule.getActivity().superInit();
        CustomMatchers.assertDoesExist(onView(allOf(withClassName(containsString("EditText")))));
        onView(allOf(withClassName(containsString("EditText")))).perform(typeText("pass"), closeSoftKeyboard());
        onView(allOf(withClassName(containsString("Button")), withText("OKAY"))).perform(click());
        Thread.sleep(100);

        assertTrue(mainState.driveHelper.isConnected() == true);
        assertTrue(mainState.myDriveHelper.getRemoteCalled > 0);
        assertTrue(mainState.myDriveHelper.updateRemoteCalled == 0);
        remoteFile = new PasswordDocumentFile("remote", "pass");
        remoteFile.setRootPath(activity.getFilesDir().getAbsolutePath());
        remoteFile.load(true);
        assertTrue(localDoc.equals(remoteFile));

        onView(withId(R.id.action_edit)).perform(click());
        onView(withId(R.id.viewfile_addbtn)).perform(click());
        ViewInteraction din2 = onView(allOf(withHint("Detail name"), withClassName(containsString("EditText"))));
        CustomMatchers.assertDoesExist(din2);
        din2.perform(clearText(), typeText("localdetail"));
        Thread.sleep(25);
        din2 = onView(allOf(withHint("URL / Location"), withClassName(containsString("EditText"))));
        din2.perform(clearText(), typeText("localLocation"), closeSoftKeyboard());
        onView(withId(R.id.action_edit)).perform(click());
        onView(allOf(withClassName(containsString("Button")), withText(R.string.alert_okay))).perform(click());
        Thread.sleep(100);

        assertTrue(localDoc.saveCalled > 0);
        assertTrue(mainState.driveHelper.isConnected() == true);
        assertTrue(mainState.myDriveHelper.updateRemoteCalled > 1);
        remoteFile = new PasswordDocumentFile("remote", "pass");
        remoteFile.setRootPath(activity.getFilesDir().getAbsolutePath());
        remoteFile.load(true);
        assertTrue(localDoc.equals(remoteFile));
    }
}
