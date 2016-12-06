package com.munger.passwordkeeper;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.FragmentActivity;

import com.munger.passwordkeeper.helpers.KeyboardListenerTest;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.helpers.QuitTimer;
import com.munger.passwordkeeper.struct.Settings;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;

/**
 * Created by codymunger on 12/6/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FunctionalTest
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
        }

        @Override
        protected void setupPreferences()
        {
            settingsMock = mock(Settings.class);
            settings = settingsMock;
        }

        @Override
        public void setupQuitTimer()
        {
            quitTimerMock = mock(QuitTimer.class);
            quitTimer = quitTimerMock;
        }
    }

    private MainStateDer mainState;
    private PasswordDocument documentMock;
    private NavigationHelper navigationMock;
    private QuitTimer quitTimerMock;
    private Settings settingsMock;

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

    private void createFile()
    {

    }

    private void openFile()
    {

    }

    private void editEntry()
    {

    }

    @Test
    public void createFileFail()
    {

    }

    @Test
    public void openFileFail()
    {

    }

    @Test
    public void createFileEditDefault()
    {

    }

    @Test
    public void createFileEditNewEntry()
    {

    }

    @Test
    public void createFileBackOut()
    {

    }

    @Test
    public void openFileEditExistingEntryCreateNewEntry()
    {

    }

    @Test
    public void openFileBackOut()
    {

    }

    @Test
    public void openFileSearchEntry()
    {

    }

    @Test
    public void openFileEditEntrySaveFail()
    {

    }

    @Test
    public void timeout()
    {

    }

    @Test
    public void settingsBackOut()
    {

    }

    @Test
    public void changePassword()
    {

    }

    @Test
    public void remoteSync()
    {

    }

    @Test
    public void remoteSyncWarn()
    {

    }

    @Test
    public void remoteSyncDisable()
    {

    }

    @Test
    public void importFile()
    {

    }

    @Test
    public void about()
    {

    }

    @Test
    public void deleteLocal()
    {

    }
}
