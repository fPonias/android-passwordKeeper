package com.munger.passwordkeeper.struct.documents;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
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
            settings = settingsMock;
        }
    }

    private MainStateDer mainState;
    private PasswordDocument documentMock;
    private NavigationHelper navigationMock;
    private Settings settingsMock;

    private final static boolean USE_MOCKED_DRIVE = false;

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

    @Test
    public void connects()
    {
        PasswordDocumentDrive doc = new PasswordDocumentDrive(documentMock, "password", "pass");
        doc.init();

        int i = 0;
        while (i == 0)
        {}
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
