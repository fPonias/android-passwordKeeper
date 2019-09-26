package com.munger.passwordkeeper.helpers;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.TestingMainActivity;
import com.munger.passwordkeeper.struct.Settings;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.fragment.app.FragmentActivity;
import androidx.test.rule.ActivityTestRule;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Created by codymunger on 12/5/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class QuitTimerTest
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
        public void setupDriveHelper()
        {
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

    @Rule
    public ActivityTestRule<TestingMainActivity> activityRule = new ActivityTestRule<>(TestingMainActivity.class);

    @Before
    public void before()
    {
        FragmentActivity activity = activityRule.getActivity();
        mainState = new MainStateDer();
        MainState.setInstance(mainState);
        mainState.setContext(activity, activity);
    }

    @Test
    public void timesOut() throws InterruptedException
    {
        float timeout = 100.0f / 60000.0f;
        long checkPeriod = 20;
        doReturn(timeout).when(settingsMock).getTimeout();
        QuitTimer timer = new QuitTimer();
        timer.setCheckPeriod(checkPeriod);
        timer.reset();

        Thread.sleep(200);
        verify(navigationMock).reset();
    }

    @Test
    public void timesOutAfterReset() throws InterruptedException
    {
        float timeout = 100.0f / 60000.0f;
        long checkPeriod = 20;
        doReturn(timeout).when(settingsMock).getTimeout();
        QuitTimer timer = new QuitTimer();
        timer.setCheckPeriod(checkPeriod);
        timer.reset();

        Thread.sleep(50);
        verify(navigationMock, never()).reset();
        timer.reset();

        Thread.sleep(200);
        verify(navigationMock).reset();
    }

    @Test
    public void timesOutAfterManyResets() throws InterruptedException
    {
        float timeout = 100.0f / 60000.0f;
        long checkPeriod = 20;
        doReturn(timeout).when(settingsMock).getTimeout();
        QuitTimer timer = new QuitTimer();
        timer.setCheckPeriod(checkPeriod);
        timer.reset();

        for (int i = 0; i < 25; i++)
        {
            Thread.sleep(50);
            verify(navigationMock, never()).reset();
            timer.reset();
        }

        Thread.sleep(200);
        verify(navigationMock).reset();
    }

    @Test
    public void stops() throws InterruptedException
    {
        float timeout = 100.0f / 60000.0f;
        long checkPeriod = 20;
        doReturn(timeout).when(settingsMock).getTimeout();
        QuitTimer timer = new QuitTimer();
        timer.setCheckPeriod(checkPeriod);
        timer.reset();

        Thread.sleep(50);
        verify(navigationMock, never()).reset();
        timer.stop();

        Thread.sleep(200);
        verify(navigationMock, never()).reset();
    }
}
