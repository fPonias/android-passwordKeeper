package com.munger.passwordkeeper.struct.documents;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.munger.passwordkeeper.Helper;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.TestingMainActivity;
import com.munger.passwordkeeper.helpers.DriveHelper;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.helpers.QuitTimerTest;
import com.munger.passwordkeeper.struct.Settings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Created by codymunger on 12/7/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PasswordDocumentDriveTest
{
    public class MainStateDer extends MainState
    {
        @Override
        protected void setupDocument()
        {
            this.document = doc;
        }

        @Override
        protected void setupNavigation()
        {
            navigationMock = mock(NavigationHelper.class);
            navigationHelper = navigationMock;
        }

        @Override
        public void setupDriveHelper()
        {
            driveHelper = new DriveHelper();
            driveHelper.connect();
        }

        @Override
        protected void setupPreferences()
        {
            settingsMock = mock(Settings.class);
            doReturn("123").when(settingsMock).getDeviceUID();
            settings = settingsMock;
        }
    }

    private MainStateDer mainState;
    private PasswordDocument doc;
    private NavigationHelper navigationMock;
    private Settings settingsMock;

    private final static String DEFAULT_FILENAME = "password-test";
    private final static String DEFAULT_PASSWORD = "pass";

    @Rule
    public ActivityTestRule<TestingMainActivity> activityRule = new ActivityTestRule<>(TestingMainActivity.class);

    @Before
    public void before()
    {
        Context context = InstrumentationRegistry.getContext();
        FragmentActivity activity = activityRule.getActivity();
        mainState = new MainStateDer();
        MainState.setInstance(mainState);
        mainState.setContext(activity, activity);
        status = new Status();
    }

    private class Status
    {
        public int wasCalled = 0;
        public boolean initted = false;
        public boolean wasSaved = false;
        public boolean wasUpdated = false;
    }

    private class DefaultEventHandler implements PasswordDocumentDrive.DocumentEvents
    {
        @Override
        public void initFailed(Exception e)
        {
            status.wasCalled++;
            status.initted = false;
        }

        @Override
        public void initted()
        {
            status.wasCalled++;
            status.initted = true;
        }

        @Override
        public void saved()
        {
            status.wasCalled++;
            status.wasSaved = true;
        }

        @Override
        public void updated()
        {
            status.wasCalled++;
            status.wasUpdated = true;
        }
    }

    private Object lock = new Object();
    private Status status;
    private PasswordDocumentDrive.DocumentEvents listener;

    @Test
    public void connects() throws InterruptedException
    {
        listener = new DefaultEventHandler()
        {
            @Override
            public void initted()
            {
                super.initted();
                synchronized (lock){ lock.notify(); }
            }

            @Override
            public void initFailed(Exception e)
            {
                super.initFailed(e);
                synchronized (lock){lock.notify(); }
            }
        };
        doc = Helper.generateDocument(2, 2);
        final PasswordDocumentDrive driveDoc = new PasswordDocumentDrive(doc, DEFAULT_FILENAME, DEFAULT_PASSWORD);
        driveDoc.addListener(listener);
        Log.d("password", "created document");

        new Thread(new Runnable() {public void run()
        {
            Log.d("password", "initting document");
            driveDoc.init();
        }}, "driveDocInit").start();

        synchronized (lock){
            lock.wait(10000);
        }

        assertEquals(1, status.wasCalled);
        assertTrue(status.initted);
    }

    @Test
    public void handlesPermissionDenied()
    {
    }

    @Test
    public void handlesConnectFail()
    {
    }

    @Test
    public void createsEmptyFileOnMissingTarget()
    {
    }

    @Test
    public void deletesTrashedFileAndCreatesEmpty()
    {
    }

    @Test
    public void handleQueryError()
    {
    }

    @Test
    public void handleCreateError()
    {
    }

    @Test
    public void obtainsLockNoContest()
    {

    }

    @Test
    public void obtainsLockAfterRelease()
    {

    }

    @Test
    public void obtainsLockAfterTimeout()
    {

    }

    @Test
    public void handlesLockError()
    {

    }
}
