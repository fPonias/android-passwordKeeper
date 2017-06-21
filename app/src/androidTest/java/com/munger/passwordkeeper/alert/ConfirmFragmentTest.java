package com.munger.passwordkeeper.alert;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.FragmentActivity;

import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.TestingMainActivity;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;
import com.munger.passwordkeeper.view.CreateFileFragment;
import com.munger.passwordkeeper.view.CreateFileFragmentTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static org.mockito.Mockito.mock;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.action.ViewActions.*;
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
        Context context = InstrumentationRegistry.getContext();
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
    public void okay()
    {
        String message = "alert!";
        ConfirmListener listener = new ConfirmListener();
        ConfirmFragment frag = new ConfirmFragment(message, listener);
        frag.show(MainState.getInstance().activity.getSupportFragmentManager(), "invalid_fragment");

        onView(withText(message)).check(matches(isDisplayed()));
        onView(allOf(withClassName(containsString("Button")), withText(R.string.confirm_okay))).perform(click());
        synchronized (listener.lock)
        {
            try{listener.lock.wait(2000);}catch(Exception e){fail();}
        }

        assertTrue(listener.okayed);
    }

    @Test
    public void cancelled()
    {
        String message = "alert!";
        ConfirmListener listener = new ConfirmListener();
        ConfirmFragment frag = new ConfirmFragment(message, listener);
        frag.show(MainState.getInstance().activity.getSupportFragmentManager(), "invalid_fragment");

        onView(withText(message)).check(matches(isDisplayed()));
        onView(allOf(withClassName(containsString("Button")), withText(R.string.confirm_cancel))).perform(click());
        synchronized (listener.lock)
        {
            try{listener.lock.wait(2000);}catch(Exception e){fail();}
        }

        assertTrue(listener.cancelled);
    }

    @Test
    public void discarded()
    {
        String message = "alert!";
        ConfirmListener listener = new ConfirmListener();
        ConfirmFragment frag = new ConfirmFragment(message, listener);
        frag.show(MainState.getInstance().activity.getSupportFragmentManager(), "invalid_fragment");

        onView(withText(message)).check(matches(isDisplayed()));
        onView(allOf(withClassName(containsString("Button")), withText(R.string.confirm_discard))).perform(click());
        synchronized (listener.lock)
        {
            try{listener.lock.wait(2000);}catch(Exception e){fail();}
        }

        assertTrue(listener.discarded);
    }
}
