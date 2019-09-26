package com.munger.passwordkeeper.alert;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.SmallTest;

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
public class AlertFragmentTest
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
