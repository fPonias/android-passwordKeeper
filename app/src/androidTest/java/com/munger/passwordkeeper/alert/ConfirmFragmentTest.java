package com.munger.passwordkeeper.alert;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.TestingMainActivity;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.fragment.app.FragmentActivity;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
/**
 * Created by codymunger on 12/4/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ConfirmFragmentTest
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
        public void setupQuitTimer()
        {
        }
    }

    private MainStateDer mainState;
    private PasswordDocument documentMock;
    private NavigationHelper navigationMock;

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

    private class ConfirmListener implements  ConfirmFragment.Listener
    {
        public boolean okayed = false;
        public boolean cancelled = false;
        public boolean discarded = false;
        public Object lock = new Object();

        @Override
        public void okay()
        {
            okayed = true;

            synchronized (lock)
            {
                lock.notify();
            }
        }

        @Override
        public void cancel()
        {
            cancelled = true;

            synchronized (lock)
            {
                lock.notify();
            }
        }

        @Override
        public void discard()
        {
            discarded = true;

            synchronized (lock)
            {
                lock.notify();
            }
        }
    }

    @Test
    public void okay() throws InterruptedException
    {
        String message = "alert!";
        ConfirmListener listener = new ConfirmListener();
        ConfirmFragment frag = new ConfirmFragment(message, listener);
        frag.show(MainState.getInstance().activity.getSupportFragmentManager(), "invalid_fragment");

        onView(withText(message)).check(matches(isDisplayed()));
        Thread.sleep(100);
        onView(allOf(withClassName(containsString("Button")), withText(R.string.confirm_okay))).perform(click());
        synchronized (listener.lock)
        {
            try{listener.lock.wait(2000);}catch(Exception e){fail();}
        }

        assertTrue(listener.okayed);
    }

    @Test
    public void cancelled() throws InterruptedException
    {
        String message = "alert!";
        ConfirmListener listener = new ConfirmListener();
        ConfirmFragment frag = new ConfirmFragment(message, listener);
        frag.show(MainState.getInstance().activity.getSupportFragmentManager(), "invalid_fragment");

        onView(withText(message)).check(matches(isDisplayed()));
        Thread.sleep(100);
        onView(allOf(withClassName(containsString("Button")), withText(R.string.confirm_cancel))).perform(click());
        synchronized (listener.lock)
        {
            try{listener.lock.wait(2000);}catch(Exception e){fail();}
        }

        assertTrue(listener.cancelled);
    }

    @Test
    public void discarded() throws InterruptedException
    {
        String message = "alert!";
        ConfirmListener listener = new ConfirmListener();
        ConfirmFragment frag = new ConfirmFragment(message, listener);
        frag.show(MainState.getInstance().activity.getSupportFragmentManager(), "invalid_fragment");

        onView(withText(message)).check(matches(isDisplayed()));
        Thread.sleep(100);
        onView(allOf(withClassName(containsString("Button")), withText(R.string.confirm_discard))).perform(click());
        synchronized (listener.lock)
        {
            try{listener.lock.wait(2000);}catch(Exception e){fail();}
        }

        assertTrue(listener.discarded);
    }
}
