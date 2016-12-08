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
            if (!USE_MOCKED_DRIVE)
            {
                driveHelper = new DriveHelper();
                driveHelper.connect();
            }
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

    private final static boolean USE_MOCKED_DRIVE = false;
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
    }

    private class Status
    {
        public boolean initted = false;
    }

    @Test
    public void connects() throws InterruptedException
    {
        final Object lock = new Object();
        final Status status = new Status();
        PasswordDocumentDrive.DocumentEvents listener = new PasswordDocumentDrive.DocumentEvents()
        {
            @Override
            public void initFailed(Exception e)
            {
                status.initted = false;
                synchronized (lock)
                {
                    lock.notify();
                }
            }

            @Override
            public void initted()
            {
                status.initted = true;
                synchronized (lock)
                {
                    lock.notify();
                }
            }

            @Override
            public void saved()
            {

            }

            @Override
            public void updated()
            {

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
