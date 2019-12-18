package com.munger.passwordkeeper.view;

/**
 * Created by codymunger on 11/27/16.
 */

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.TestingMainActivity;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.helpers.QuitTimer;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import androidx.fragment.app.FragmentActivity;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.munger.passwordkeeper.CustomMatchers.withIndex;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class SettingsFragmentTest
{
    public class MainStateDer extends MainState
    {
        @Override
        public void setupDocument()
        {
            documentMock = mock(PasswordDocumentFile.class);
            document = documentMock;
        }

        @Override
        protected void setupNavigation()
        {
            navigationMock = mock(NavigationHelper.class);
            navigationHelper = navigationMock;
        }

        @Override
        public void setupQuitTimer() {
            quitTimerMock = mock(QuitTimer.class);
            quitTimer = quitTimerMock;
        }

        public int driveHelperCalled = 0;
        @Override
        public void setupDriveHelper()
        {
            driveHelperCalled++;
        }

        public int deleteDataCalled = 0;
        @Override
        public void deleteData()
        {
            deleteDataCalled++;
        }

        public int deleteRemoteDataCalled = 0;
        @Override
        public void deleteRemoteData()
        {
            deleteRemoteDataCalled++;
        }
    }

    public static class SettingsFragmentDer extends SettingsFragment
    {
        protected File defaultDir;

        public void setDetaultDirectory(File dir)
        {
            defaultDir = dir;
        }

        @Override
        protected File getDefaultDirectory()
        {
            return defaultDir;
        }
    }

    @Rule
    public ActivityTestRule<TestingMainActivity> activityRule = new ActivityTestRule<>(TestingMainActivity.class);

    private SettingsFragmentDer fragment;
    private SettingsFragmentTest.MainStateDer mainState;
    private PasswordDocument documentMock;
    private QuitTimer quitTimerMock;
    private NavigationHelper navigationMock;

    @Before
    public void before() throws Exception
    {
        FragmentActivity activity = activityRule.getActivity();
        mainState = new SettingsFragmentTest.MainStateDer();
        MainState.setInstance(mainState);
        mainState.setContext(activity, activity);

        parsePreferences();
        createFragment();
    }

    private void createFragment() throws InterruptedException
    {
        if (fragment != null)
            activityRule.getActivity().setFragment(null);

        final Object lock = new Object();

        mainState.handler.post(new Runnable() {public void run()
        {
            fragment = new SettingsFragmentDer();
            setupImportDialog();

            synchronized (lock)
            {
                lock.notify();
            }
        }});

        synchronized (lock)
        {
            lock.wait();
        }

        activityRule.getActivity().setFragment(fragment);
    }

    private void setupImportDialog()
    {
        doReturn(true).when(navigationMock).hasPermission(any(String.class));

        File[] fileList = new File[1];
        File filemock = mock(File.class);
        doReturn(true).when(filemock).exists();
        doReturn("a").when(filemock).getName();
        doReturn(false).when(filemock).isDirectory();
        doReturn(true).when(filemock).canRead();
        fileList[0] = filemock;

        final File rootmock = mock(File.class);
        doReturn(true).when(rootmock).exists();
        doReturn(true).when(rootmock).isDirectory();
        doReturn("..").when(rootmock).getName();
        doReturn(true).when(rootmock).canRead();
        doReturn(fileList).when(rootmock).listFiles();

        fragment.setDetaultDirectory(rootmock);
    }

    private ArrayList<String> prefKeys;
    private ArrayList<String> prefTitles;

    private void parsePreferences() throws XmlPullParserException, IOException
    {
        prefKeys = new ArrayList<>();
        prefTitles = new ArrayList<>();

        Resources res = activityRule.getActivity().getResources();
        XmlResourceParser xrp = res.getXml(SettingsFragment.PREFERENCES_RESOURCE);
        int eventType = xrp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT)
        {
            if (eventType == XmlPullParser.START_TAG)
            {
                String name = xrp.getName();
                int depth = xrp.getDepth();

                if (name.contains("Preference") && depth > 1)
                {
                    int attrSz = xrp.getAttributeCount();
                    for (int i = 0; i < attrSz; i++)
                    {
                        String attrName = xrp.getAttributeName(i);
                        if (attrName.equals("key"))
                            prefKeys.add(xrp.getAttributeValue(i));
                        else if (attrName.equals("title"))
                        {
                            String val = xrp.getAttributeValue(i);

                            if (val.startsWith("@"))
                            {
                                val = val.substring(1);
                                int id = Integer.parseInt(val);
                                val = res.getString(id);
                            }

                            prefTitles.add(val);
                        }
                    }
                }
            }
            eventType = xrp.nextToken();
        }
    }

    private int getIndex(String key)
    {
        return prefKeys.indexOf(key);
    }


    @Test
    public void passwordChange()
    {
        int index = getIndex(SettingsFragment.PREF_CHANGE_PASSWORD);
        assertTrue(index > -1);

        onView(withClassName(containsString("RecyclerView")))
                .perform(RecyclerViewActions.actionOnItemAtPosition(index, click()));

        verify(navigationMock).changePassword();
    }

    @Test
    public void about()
    {
        int index = getIndex(SettingsFragment.PREF_ABOUT);
        assertTrue(index > -1);

        onView(withClassName(containsString("RecyclerView")))
                .perform(RecyclerViewActions.actionOnItemAtPosition(index, click()));

        verify(navigationMock).about();
    }

    @Test
    public void saveToCloud()
    {
        mainState.driveHelperCalled = 0;
        int index = getIndex(SettingsFragment.PREF_NAME_SAVE_TO_CLOUD);
        assertTrue(index > -1);

        onView(withClassName(containsString("RecyclerView")))
                .perform(RecyclerViewActions.actionOnItemAtPosition(index, click()));


        assertEquals(1, mainState.driveHelperCalled);
    }

    @Test
    public void timeouts()
    {
        int index = getIndex(SettingsFragment.PREF_NAME_TIMEOUT_LIST);
        assertTrue(index > -1);

        onView(withClassName(containsString("RecyclerView")))
                .perform(RecyclerViewActions.actionOnItemAtPosition(index, click()));

        onView(withIndex(0, withClassName(containsString("CheckedTextView")))).perform(click());

        verify(quitTimerMock).reset();
    }

    @Test
    public void deleteYes()
    {
        mainState.deleteRemoteDataCalled = 0;
        mainState.deleteDataCalled = 0;

        int index = getIndex(SettingsFragment.PREF_DELETE_FILE);
        assertTrue(index > -1);

        onView(withClassName(containsString("RecyclerView")))
                .perform(RecyclerViewActions.actionOnItemAtPosition(index, click()));

        onView(withText("Yes")).perform(click());

        assertEquals(1, mainState.deleteDataCalled);
        assertEquals(1, mainState.deleteRemoteDataCalled);
        verify(navigationMock).openInitialView();
    }

    @Test
    public void deleteNo()
    {
        mainState.deleteRemoteDataCalled = 0;
        mainState.deleteDataCalled = 0;

        int index = getIndex(SettingsFragment.PREF_DELETE_FILE);
        assertTrue(index > -1);

        onView(withClassName(containsString("RecyclerView")))
                .perform(RecyclerViewActions.actionOnItemAtPosition(index, click()));

        onView(withText("No")).perform(click());

        assertEquals(0, mainState.deleteDataCalled);
        assertEquals(0, mainState.deleteRemoteDataCalled);
        verify(navigationMock, never()).openInitialView();
    }

    private void doImport(final boolean futureStatus) throws Exception
    {
        final Object lock = new Object();

        doAnswer(new Answer<Void>() {public Void answer(final InvocationOnMock invo)
        {
            Thread t = new Thread(new Runnable() {public void run()
            {
                NavigationHelper.Callback callback = invo.getArgumentAt(1, NavigationHelper.Callback.class);
                callback.callback(futureStatus);

                synchronized (lock)
                {
                    lock.notify();
                }
            }});
            t.start();

            return null;
        }}).when(navigationMock).importFile(anyString(), org.mockito.Matchers.any(NavigationHelper.Callback.class));



        int index = getIndex(SettingsFragment.PREF_IMPORT_FILE);
        assertTrue(index > -1);

        onView(withClassName(containsString("RecyclerView")))
                .perform(RecyclerViewActions.actionOnItemAtPosition(index, click()));

        onData(not(Matchers.endsWith("/"))).inAdapterView(withClassName(containsString("ListView"))).perform(click());

        synchronized (lock)
        {
            lock.wait(2000);
        }
    }

    @Test
    public void importSuccess() throws Exception
    {
        doImport(true);

        verify(documentMock).save();
        verify(navigationMock).onBackPressed(any(NavigationHelper.Callback.class));
    }

    @Test
    public void importFail() throws Exception
    {
        doImport(false);

        verify(documentMock, never()).save();
        verify(navigationMock, never()).onBackPressed(any(NavigationHelper.Callback.class));
    }

    @Test
    public void importSaveFail() throws Exception
    {
        doThrow(new IOException("failed to save file"))
                .when(documentMock).save();

        doImport(true);

        verify(documentMock).save();
        verify(navigationMock).showAlert(anyString());
        verify(navigationMock, never()).onBackPressed(any(NavigationHelper.Callback.class));
    }

    @Test
    public void importHiddenWhenDisabled() throws Exception
    {
        int index = getIndex(SettingsFragment.PREF_IMPORT_FILE);
        assertTrue(index > -1);
        String title = prefTitles.get(index);


        mainState.config.enableImportOption = true;
        createFragment();

        onView(withClassName(containsString("RecyclerView"))).perform(RecyclerViewActions.scrollToPosition(index));
        onView(allOf(withParent(withClassName(containsString("RecyclerView"))), hasDescendant(withText(title)))).check(matches(isDisplayed()));


        mainState.config.enableImportOption = false;
        createFragment();

        onView(withClassName(containsString("RecyclerView"))).perform(RecyclerViewActions.scrollToPosition(index));
        boolean thrown = false;
        try
        {
            onView(allOf(withParent(withClassName(containsString("RecyclerView"))), hasDescendant(withText(title)))).check(matches(isDisplayed()));
        }
        catch(NoMatchingViewException e){
            thrown = true;
        }

        assertTrue(thrown);
    }
}
