package com.munger.passwordkeeper;

import android.Manifest;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Environment;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import com.munger.passwordkeeper.struct.Settings;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.test.espresso.DataInteraction;
import androidx.test.rule.ActivityTestRule;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

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
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


/**
 * Created by codymunger on 12/6/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FunctionalTest
{
    private MainStateDer mainState;

    public class SettingsDer extends Settings
    {
        public float timeout = 1.0f;

        @Override
        public float getTimeout()
        {
            return timeout;
        }

        public float getRealTimeout() {return super.getTimeout();}

        public boolean saveToCloud = false;

        @Override
        public boolean getSaveToCloud() {
            return saveToCloud;
        }

        public boolean getRealSaveToCloud(){
            return super.getSaveToCloud();
        }
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
    }

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10); // 10 seconds max per method tested

    @Rule
    public ActivityTestRule<TestingMainActivity> activityRule = new ActivityTestRule<>(TestingMainActivity.class);

    @Before
    public void before() throws Exception
    {
        FragmentActivity activity = activityRule.getActivity();
        mainState = new MainStateDer();
        MainState.setInstance(mainState);
        mainState.setContext(activity, activity);
        mainState.document.delete();
    }

    @Test
    public void createFileFail() throws Exception
    {
        activityRule.getActivity().superInit();

        onView(withId(R.id.createfile_password1ipt)).perform(typeText("pass"));
        onView(withId(R.id.createfile_password2ipt)).perform(typeText("no match"));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        onView(allOf(withClassName(containsString("Button")), withText(R.string.alert_okay))).perform(click());

        onView(withId(R.id.createfile_password1ipt)).perform(clearText()).perform(typeText("pass"));
        onView(withId(R.id.createfile_password2ipt)).perform(clearText()).perform(typeText("pass"));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        onView(allOf(withClassName(containsString("Button")), withText(R.string.alert_okay))).check(doesNotExist());
    }

    @Test
    public void openFileFail() throws Exception
    {
        String password = "pass";
        mainState.document.setPassword(password);
        mainState.document.save();

        activityRule.getActivity().superInit();

        onView(withClassName(containsString("EditText"))).perform(typeText("wrongpass"));
        onView(allOf(withClassName(containsString("Button")), withText(R.string.input_okay))).perform(click());

        onView(allOf(withClassName(containsString("Button")), withText(R.string.alert_okay))).perform(click());


        onView(withClassName(containsString("EditText"))).perform(clearText()).perform(typeText(password));
        onView(allOf(withClassName(containsString("Button")), withText(R.string.input_okay))).perform(click());

        onView(allOf(withClassName(containsString("Button")), withText(R.string.alert_okay))).check(doesNotExist());

    }

    @Test
    public void createFileEditDefault() throws Exception
    {
        //password view
        activityRule.getActivity().superInit();

        onView(withId(R.id.createfile_password1ipt)).perform(clearText()).perform(typeText("pass"));
        onView(withId(R.id.createfile_password2ipt)).perform(clearText()).perform(typeText("pass"));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        //document view
        Fragment frag = ((MainActivity) mainState.activity).getCurrentFagment();
        assertTrue(frag instanceof ViewFileFragment);
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

        onView(withId(R.id.createfile_password1ipt)).perform(clearText()).perform(typeText("pass"));
        onView(withId(R.id.createfile_password2ipt)).perform(clearText()).perform(typeText("pass"));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        //document view
        MainActivity activity = (MainActivity) MainState.getInstance().activity;
        Fragment frag = activity.getCurrentFagment();
        assertTrue(frag instanceof ViewFileFragment);
        onData(is(instanceOf(PasswordDetails.class))).atPosition(0).perform(click());

        //details view
        frag = activity.getCurrentFagment();
        assertTrue(frag instanceof ViewDetailFragment);
        ViewDetailFragment detFrag = (ViewDetailFragment) frag;
        assertEquals(true, detFrag.getEditable());

        onView(withId(R.id.viewdetail_namelbl)).check(matches(hasDescendant(withText(""))));
        onView(withId(R.id.viewdetail_locationlbl)).check(matches(hasDescendant(withText(""))));
        DataInteraction inter = onData(is(instanceOf(PasswordDetailsPair.class))).atPosition(0);
        inter.check(matches(allOf(pairWithKey(""), pairWithValue(""))));

        boolean thrown = false;
        try
        {
            onData(is(instanceOf(PasswordDetailsPair.class))).atPosition(1).check(doesNotExist());
        }
        catch(Exception e){
            thrown = true;
        }
        assertTrue(thrown);

        //edit details
        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_namelbl)),withClassName(containsString("EditText")))).perform(clearText(), typeText(name));
        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_locationlbl)),withClassName(containsString("EditText")))).perform(clearText(), typeText(location));
        onData(anything()).atPosition(0).onChildView(withId(R.id.detailitem_keyinput)).perform(clearText(), typeText(detKey1));
        Thread.sleep(25);
        onData(anything()).atPosition(0).onChildView(withId(R.id.detailitem_valueinput)).perform(clearText(), typeText(detValue1));
        Thread.sleep(25);

        onView(withId(R.id.viewdetail_addbtn)).perform(click());
        DataInteraction intera = onData(anything()).atPosition(1);
        intera.onChildView(withId(R.id.detailitem_keyinput)).perform(clearText(), typeText(detKey2));
        Thread.sleep(25);
        intera.onChildView(withId(R.id.detailitem_valueinput)).perform(clearText(), typeText(detValue2));
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
        Fragment frag = ((MainActivity) mainState.activity).getCurrentFagment();
        assertTrue(frag instanceof ViewFileFragment);
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
        Fragment frag = activityRule.getActivity().getCurrentFagment();
        assertTrue(frag instanceof ViewFileFragment);
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
        onView(withId(R.id.createfile_password1ipt)).perform(clearText()).perform(typeText("pass"));
        onView(withId(R.id.createfile_password2ipt)).perform(clearText()).perform(typeText("pass"));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        //document view
        TestingMainActivity activity = (TestingMainActivity) MainState.getInstance().activity;
        Fragment frag = activity.getCurrentFagment();
        assertTrue(frag instanceof ViewFileFragment);

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
        PasswordDetails dets1 = new PasswordDetails();
        dets1.setName(existingTitle);
        dets1.setLocation("location");
        dets1.addPair(new PasswordDetailsPair(detKey1, detValue1));
        dets1.addPair(new PasswordDetailsPair(detKey2, detValue2));
        mainState.document.addDetails(dets1);
        mainState.document.save();
    }

    protected void createAndOpenExistingFile() throws Exception
    {
        createExistingFile();

        //password view
        activityRule.getActivity().superInit();

        onView(allOf(withClassName(containsString("EditText")))).perform(typeText("pass"));
        onView(allOf(withClassName(containsString("Button")), withText("Okay"))).perform(click());

        //document view
        Thread.sleep(25);
        MainActivity activity = (MainActivity) MainState.getInstance().activity;
        Fragment frag = activity.getCurrentFagment();
        assertTrue(frag instanceof ViewFileFragment);
    }

    @Test
    public void openFileEditExistingEntryCreateNewEntry() throws Exception
    {
        createAndOpenExistingFile();

        onView(withId(R.id.action_edit)).perform(click());
        onView(withId(R.id.viewfile_addbtn)).perform(click());

        //details view
        String newName = "new name";
        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_namelbl)),withClassName(containsString("EditText")))).perform(clearText(), typeText(newName));
        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_locationlbl)),withClassName(containsString("EditText")))).perform(clearText(), typeText(location));
        onData(anything()).atPosition(0).onChildView(withId(R.id.detailitem_keyinput)).perform(clearText(), typeText(detKey1));
        Thread.sleep(25);
        onData(anything()).atPosition(0).onChildView(withId(R.id.detailitem_valueinput)).perform(clearText(), typeText(detValue1));
        Thread.sleep(25);

        //back out
        activityRule.getActivity().resetBackCalledCount();
        while (activityRule.getActivity().getBackTriggeredCount() == 0)
        {
            pressBack();
            Thread.sleep(50);
        }

        onView(withText(R.string.confirm_okay)).perform(click());

        Thread.sleep(50);
        //document view
        Fragment frag = ((MainActivity) mainState.activity).getCurrentFagment();
        assertTrue(frag instanceof ViewFileFragment);
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

        onView(allOf(withClassName(containsString("EditText")))).perform(typeText("pass"));
        onView(allOf(withClassName(containsString("Button")), withText("Okay"))).perform(click());

        //document view
        onView(allOf(withId(R.id.action_search))).perform(click());
        onView(isAssignableFrom(EditText.class)).perform(typeText(searchShortTitle));

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

        MainActivity activity = (MainActivity) MainState.getInstance().activity;
        Fragment frag = activity.getCurrentFagment();
        assertTrue(frag instanceof ViewFileFragment);
    }

    @Test
    public void changePassword() throws Exception
    {
        createAndOpenExistingFile();

        onView(withId(R.id.action_settings)).perform(click());

        onView(withText(R.string.settings_password_title)).perform(click());

        //change password view
        onView(withId(R.id.createfile_oldpasswordipt)).perform(typeText("pass"));
        onView(withId(R.id.createfile_password1ipt)).perform(typeText("password"));
        onView(withId(R.id.createfile_password2ipt)).perform(typeText("password"));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        //settings view
        MainActivity activity = (MainActivity) MainState.getInstance().activity;
        Fragment frag = activity.getCurrentFagment();
        assertTrue(frag instanceof SettingsFragment);

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
        frag = activity.getCurrentFagment();
        assertTrue(frag instanceof ViewFileFragment);
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

    @Test
    public void remoteSync() throws Exception
    {
        if (mainState.mySettings.getRealSaveToCloud() != mainState.mySettings.saveToCloud)
            mainState.mySettings.setSaveToCloud(false);

        createAndOpenExistingFile();

        onView(withId(R.id.action_settings)).perform(click());

        mainState.mySettings.saveToCloud = true;
        onView(withText(R.string.settings_cloud_title)).perform(click());
        selectAccountIfNeeded();

        mainState.driveHelper.awaitConnection();
        Thread.sleep(250);

        assertNotNull(mainState.driveDocument);
    }

    @Test
    public void remoteSyncDisable() throws Exception
    {
        mainState.mySettings.saveToCloud = true;
        createAndOpenExistingFile();

        if (mainState.mySettings.getRealSaveToCloud() != mainState.mySettings.saveToCloud)
            mainState.mySettings.setSaveToCloud(true);

        selectAccountIfNeeded();
        mainState.driveHelper.awaitConnection();
        Thread.sleep(250);

        assertNotNull(mainState.driveDocument);

        onView(withId(R.id.action_settings)).perform(click());

        mainState.mySettings.saveToCloud = false;
        onView(withText(R.string.settings_cloud_title)).perform(click());

        Thread.sleep(250);

        assertNull(mainState.driveDocument);
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

        onData(is("Download/")).perform(click());
        onData(is(filename)).perform(click());

        onView(withText(R.string.confirm_okay)).perform(click());

        //settings view
        MainActivity activity = (MainActivity) MainState.getInstance().activity;
        Fragment frag = activity.getCurrentFagment();
        assertTrue(frag instanceof SettingsFragment);

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
        frag = activity.getCurrentFagment();
        assertTrue(frag instanceof ViewFileFragment);

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

        MainActivity activity = (MainActivity) MainState.getInstance().activity;
        Fragment frag = activity.getCurrentFagment();
        assertTrue(frag instanceof SettingsFragment);

        pressBack();
        Thread.sleep(50);

        //document view
        frag = activity.getCurrentFagment();
        assertTrue(frag instanceof ViewFileFragment);

        onData(is(instanceOf(PasswordDetails.class))).atPosition(0).check(matches(hasDescendant(withText(existingTitle))));

        thrown = false;

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
    public void about() throws Exception
    {
        createAndOpenExistingFile();

        onView(withId(R.id.action_settings)).perform(click());

        onView(withText(R.string.settings_about_title)).perform(click());

        MainActivity activity = (MainActivity) MainState.getInstance().activity;
        Fragment frag = activity.getCurrentFagment();
        assertTrue(frag instanceof AboutFragment);

        pressBack();

        frag = activity.getCurrentFagment();
        assertTrue(frag instanceof SettingsFragment);
    }

    @Test
    public void deleteLocalYes() throws Exception
    {
        createAndOpenExistingFile();
        onView(withId(R.id.action_settings)).perform(click());

        onView(withText(R.string.settings_delete_title)).perform(click());

        onView(withText(R.string.alert_positive)).perform(click());

        MainActivity activity = (MainActivity) MainState.getInstance().activity;
        Fragment frag = activity.getCurrentFagment();
        assertTrue(frag instanceof CreateFileFragment);
        assertTrue(((CreateFileFragment) frag).getIsCreating());
    }

    @Test
    public void deleteLocalNo() throws Exception
    {
        createAndOpenExistingFile();
        onView(withId(R.id.action_settings)).perform(click());

        onView(withText(R.string.settings_delete_title)).perform(click());

        onView(withText(R.string.alert_negative)).perform(click());

        MainActivity activity = (MainActivity) MainState.getInstance().activity;
        Fragment frag = activity.getCurrentFagment();
        assertTrue(frag instanceof SettingsFragment);
    }
}
