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
public class AlertFragmentTest
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

    private class State
    {
        public boolean closed = false;
    }

    @Test
    public void works()
    {
        final State state = new State();
        final Object lock = new Object();
        AlertFragment frag = new AlertFragment("alert!");
        frag.show(MainState.getInstance().activity.getSupportFragmentManager(), "invalid_fragment");
        frag.setCloseCallback(new AlertFragment.CloseCallback() {public void closed()
        {
            state.closed = true;
            try{Thread.sleep(5);}catch(Exception e){fail();}
            synchronized (lock)
            {
                lock.notify();
            }
        }});

        onView(withText("alert!")).check(matches(isDisplayed()));
        onView(allOf(withClassName(containsString("Button")), withText(R.string.alert_okay))).perform(click());
        synchronized (lock)
        {
            try{lock.wait(2000);}catch(Exception e){fail();}
        }

        assertTrue(state.closed);
    }
}
