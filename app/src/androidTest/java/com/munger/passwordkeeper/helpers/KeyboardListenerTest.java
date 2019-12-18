package com.munger.passwordkeeper.helpers;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.TestingMainActivity;
import com.munger.passwordkeeper.alert.InputFragment;
import com.munger.passwordkeeper.struct.Settings;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.fragment.app.FragmentActivity;
import androidx.test.espresso.Espresso;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Created by codymunger on 12/6/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class KeyboardListenerTest
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

        InputFragment frag = new InputFragment("message", "prompt", null);
        frag.show(MainState.getInstance().activity.getSupportFragmentManager(), "invalid_fragment");
    }

    private class Status
    {
        public boolean opened = false;
        public boolean closed = false;
        public Object lock = new Object();
    }

    @Test
    public void testKeyboard() throws InterruptedException
    {
        final Status status = new Status();
        KeyboardListener listener = MainState.getInstance().keyboardListener;
        listener.addKeyboardChangedListener(new KeyboardListener.OnKeyboardChangedListener()
        {
            @Override
            public void OnKeyboardOpened()
            {
                status.opened = true;

                synchronized (status.lock)
                {
                    status.lock.notify();
                }
            }

            @Override
            public void OnKeyboardClosed()
            {
                status.closed = true;

                synchronized (status.lock)
                {
                    status.lock.notify();
                }
            }
        });

        onView(withClassName(containsString("EditText"))).perform(typeText(" "));

        synchronized (status.lock)
        {
            status.lock.wait(200);
        }

        assertTrue(status.opened);
        status.opened = false;

        Espresso.closeSoftKeyboard();

        synchronized (status.lock)
        {
            status.lock.wait(200);
        }

        assertTrue(status.closed);
    }



    @Test
    public void testForceOpen() throws InterruptedException
    {
        final Status status = new Status();
        KeyboardListener listener = MainState.getInstance().keyboardListener;
        listener.addKeyboardChangedListener(new KeyboardListener.OnKeyboardChangedListener()
        {
            @Override
            public void OnKeyboardOpened()
            {
                status.opened = true;

                synchronized (status.lock)
                {
                    status.lock.notify();
                }
            }

            @Override
            public void OnKeyboardClosed()
            {
                status.closed = true;

                synchronized (status.lock)
                {
                    status.lock.notify();
                }
            }
        });

        listener.forceOpenKeyboard(true);

        synchronized (status.lock)
        {
            status.lock.wait(200);
        }

        assertTrue(status.opened);
        status.opened = false;


        listener.forceOpenKeyboard(false);

        synchronized (status.lock)
        {
            status.lock.wait(200);
        }

        assertTrue(status.closed);
        status.closed = false;
    }
}
