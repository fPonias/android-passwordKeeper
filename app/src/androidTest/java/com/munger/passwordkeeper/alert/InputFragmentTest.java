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
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Created by codymunger on 12/5/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class InputFragmentTest
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

    private class ConfirmListener implements InputFragment.Listener
    {
        public Object lock = new Object();
        public boolean okayPressed = false;
        public String lastOkay = null;
        public boolean cancelPressed = false;

        private boolean okayReturn = true;
        public void setOkayReturn(boolean value)
        {
            okayReturn = value;
        }

        @Override
        public void cancel(InputFragment that)
        {
            cancelPressed = true;

            synchronized (lock)
            {
                lock.notify();
            }
        }

        @Override
        public boolean okay(InputFragment that, String inputText)
        {
            okayPressed = true;
            lastOkay = inputText;

            synchronized (lock)
            {
                lock.notify();
            }

            return okayReturn;
        }
    }

    @Test
    public void okayPassed()
    {
        String message = "alert!";
        String prompt = "prompt";
        String newInput = "input!";
        ConfirmListener listener = new ConfirmListener();
        InputFragment frag = new InputFragment(message, prompt, listener);
        frag.show(MainState.getInstance().activity.getSupportFragmentManager(), "invalid_fragment");

        onView(withText(message)).check(matches(isDisplayed()));
        onView(withClassName(containsString("EditText"))).perform(typeText(newInput));
        onView(allOf(withClassName(containsString("Button")), withText(R.string.input_okay))).perform(click());
        synchronized (listener.lock)
        {
            try{listener.lock.wait(250);}catch(Exception e){fail();}
        }

        assertTrue(listener.okayPressed);
        assertEquals(listener.lastOkay, newInput);
        assertFalse(frag.isAdded());
    }

    @Test
    public void okayLoop()
    {
        String message = "alert!";
        String prompt = "prompt";
        String newInput = "input!";
        ConfirmListener listener = new ConfirmListener();
        listener.setOkayReturn(false);
        InputFragment frag = new InputFragment(message, prompt, listener);
        frag.show(MainState.getInstance().activity.getSupportFragmentManager(), "invalid_fragment");

        onView(withText(message)).check(matches(isDisplayed()));
        onView(withClassName(containsString("EditText"))).perform(typeText(newInput));
        onView(allOf(withClassName(containsString("Button")), withText(R.string.input_okay))).perform(click());
        synchronized (listener.lock)
        {
            try{listener.lock.wait(250);}catch(Exception e){fail();}
        }

        assertTrue(listener.okayPressed);
        assertEquals(listener.lastOkay, newInput);
        assertTrue(frag.isAdded());


        newInput = "input2";
        listener.okayPressed = false;
        listener.setOkayReturn(true);
        onView(withClassName(containsString("EditText"))).perform(clearText(), typeText(newInput));
        onView(allOf(withClassName(containsString("Button")), withText(R.string.input_okay))).perform(click());
        synchronized (listener.lock)
        {
            try{listener.lock.wait(250);}catch(Exception e){fail();}
        }

        assertTrue(listener.okayPressed);
        assertEquals(listener.lastOkay, newInput);
        assertFalse(frag.isAdded());
    }

    @Test
    public void cancel()
    {
        String message = "alert!";
        String prompt = "prompt";
        String newInput = "input!";
        ConfirmListener listener = new ConfirmListener();
        InputFragment frag = new InputFragment(message, prompt, listener);
        frag.show(MainState.getInstance().activity.getSupportFragmentManager(), "invalid_fragment");

        onView(withText(message)).check(matches(isDisplayed()));
        onView(withClassName(containsString("EditText"))).perform(typeText(newInput));
        onView(allOf(withClassName(containsString("Button")), withText(R.string.input_cancel))).perform(click());
        synchronized (listener.lock)
        {
            try{listener.lock.wait(250);}catch(Exception e){fail();}
        }

        assertTrue(listener.cancelPressed);
        assertFalse(frag.isAdded());
    }

    @Test
    public void disableCancel()
    {
        String message = "alert!";
        String prompt = "prompt";
        String newInput = "input!";
        ConfirmListener listener = new ConfirmListener();
        InputFragment frag = new InputFragment(message, prompt, listener);
        frag.setCancelEnabled(false);
        frag.show(MainState.getInstance().activity.getSupportFragmentManager(), "invalid_fragment");

        onView(withText(message)).check(matches(isDisplayed()));
        onView(withClassName(containsString("EditText"))).perform(typeText(newInput));

        boolean caught = false;
        try
        {
            onView(allOf(withClassName(containsString("Button")), withText(R.string.input_cancel))).check(matches(not(isDisplayed())));
        }
        catch(NoMatchingViewException e){
            caught = true;
        }
        assertTrue(caught);
    }
}
