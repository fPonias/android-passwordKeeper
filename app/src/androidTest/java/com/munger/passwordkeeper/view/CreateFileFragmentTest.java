package com.munger.passwordkeeper.view;

import android.os.Bundle;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.rule.ActivityTestRule;

import com.munger.passwordkeeper.Helper;
import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.action.ViewActions.*;
import static junit.framework.Assert.*;

/**
 * Created by codymunger on 11/25/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CreateFileFragmentTest
{
    public static class CreateFileFragmentDer extends CreateFileFragment
    {
        public boolean mainCalled = false;
        public boolean errorCalled = false;
        public String errMessage = null;

        @Override
        protected void showError(String message)
        {
            errorCalled = true;
            errMessage = message;
        }

        @Override
        protected void loadMain()
        {
            mainCalled = true;
        }
    }

    public static class MainStateDer extends MainState
    {

    }

    @Rule
    public ActivityTestRule<Helper.BlankActivity> activityRule = new ActivityTestRule<>(Helper.BlankActivity.class);

    private CreateFileFragmentDer fragment;

    @Before
    public void before()
    {
        final Object lock = new Object();

        Looper.getMainLooper();
        MainState.setInstance(new MainStateDer());

        fragment = new CreateFileFragmentDer();
        activityRule.getActivity().setFragment(fragment);
    }

    @Test
    public void test()
    {
        onView(withId(R.id.createfile_password1ipt)).perform(typeText(Helper.DEFAULT_PASSWORD));
        onView(withId(R.id.createfile_password2ipt)).perform(typeText(Helper.DEFAULT_PASSWORD));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        assertTrue(fragment.mainCalled);
        assertFalse(fragment.errorCalled);
    }
}
