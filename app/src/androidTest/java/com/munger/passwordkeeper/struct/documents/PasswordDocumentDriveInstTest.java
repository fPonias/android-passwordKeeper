package com.munger.passwordkeeper.struct.documents;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.DriveApi;
import com.munger.passwordkeeper.Helper;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.TestingMainActivity;
import com.munger.passwordkeeper.helpers.DriveHelper;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.struct.Settings;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.InitializationError;
import org.mockito.Mockito;

import androidx.fragment.app.FragmentActivity;
import androidx.test.rule.ActivityTestRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by codymunger on 12/18/16.
 */

public class PasswordDocumentDriveInstTest
{
    public class MainStateDer extends MainState
    {
        @Override
        public void setupDocument()
        {
            this.document = doc;
        }

        @Override
        protected void setupNavigation()
        {
            navigationMock = Mockito.mock(NavigationHelper.class);
            navigationHelper = navigationMock;
        }

        @Override
        public void setupDriveHelper()
        {
            driveHelperMock = Mockito.mock(DriveHelper.class);
            driveHelper = driveHelperMock;
            googleApiMock = Mockito.mock(GoogleApiClient.class);
            Mockito.doReturn(googleApiMock).when(driveHelper).getClient();
            Mockito.doReturn(googleApiMock).when(driveHelper).connect();
        }

        @Override
        protected void setupPreferences()
        {
            settingsMock = Mockito.mock(Settings.class);
            Mockito.doReturn("123").when(settingsMock).getDeviceUID();
            settings = settingsMock;
        }
    }

    public class MainStateDerRealDrive extends MainStateDer
    {
        @Override
        public void setupDriveHelper()
        {
            driveHelper = new DriveHelper();
            driveHelper.connect();
        }
    }

    public class PasswordDocumentDriveDer extends PasswordDocumentDrive
    {
        public PasswordDocumentDriveDer(PasswordDocument source)
        {
            super(source);
        }

        public PasswordDocumentDriveDer(PasswordDocument source, String name)
        {
            this(source);
        }

        public PasswordDocumentDriveDer(PasswordDocument source, String name, String password)
        {
            this(source, name);
        }

        @Override
        protected void setupDriveApi()
        {
            driveApiMock = Mockito.mock(DriveApi.class);
            driveApi = driveApiMock;
        }
    }

    private MainStateDer mainState;
    private PasswordDocument doc;
    private NavigationHelper navigationMock;
    private Settings settingsMock;
    private DriveHelper driveHelperMock;
    private GoogleApiClient googleApiMock;
    private DriveApi driveApiMock;

    private final static String DEFAULT_FILENAME = "password-test";
    private final static String DEFAULT_PASSWORD = "pass";

    @Rule
    public ActivityTestRule<TestingMainActivity> activityRule = new ActivityTestRule<>(TestingMainActivity.class);

    @Before
    public void before()
    {
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
        public void loaded()
        {
            status.wasCalled++;
            status.wasUpdated = true;
        }

        public void deleted()
        {}

        public void closed()
        {}
    }

    private Object lock = new Object();
    private Status status;
    private PasswordDocumentDrive.DocumentEvents listener;

    @Test
    public void connectsForReal() throws Exception
    {
        FragmentActivity activity = activityRule.getActivity();
        mainState = new MainStateDerRealDrive();
        MainState.setInstance(mainState);
        mainState.setContext(activity, activity);
        status = new Status();

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
}
