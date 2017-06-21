package com.munger.passwordkeeper.view;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.FragmentActivity;

import com.munger.passwordkeeper.Helper;
import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.TestingMainActivity;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;

import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static org.junit.Assert.*;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created by codymunger on 11/25/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CreateFileFragmentTest
{
    public class MainStateDer extends MainState
    {
        public boolean mock2PasswordTest = true;

        @Override
        protected PasswordDocument createDocument()
        {
            if (documentMock == null)
            {
                documentMock = mock(PasswordDocumentFile.class);
                return documentMock;
            }
            else
            {
                documentMock2 = mock(PasswordDocumentFile.class);
                doReturn(mock2PasswordTest).when(documentMock2).testPassword();
                return documentMock2;
            }
        }

        @Override
        protected void setupNavigation()
        {
            navigationMock = mock(NavigationHelper.class);

            doAnswer(new Answer<Void>() {public Void answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                NavigationHelper.Callback cb = invocationOnMock.getArgumentAt(0, NavigationHelper.Callback.class);
                cb.callback(true);
                return null;
            }}).when(navigationMock).onBackPressed(any(NavigationHelper.Callback.class));

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

    @Rule
    public ActivityTestRule<TestingMainActivity> activityRule = new ActivityTestRule<>(TestingMainActivity.class);

    private CreateFileFragment fragment;
    private CreateFileFragment fragmentSpy;
    private MainStateDer mainState;
    private PasswordDocument documentMock;
    private PasswordDocument documentMock2;
    private NavigationHelper navigationMock;

    private CreateFileFragment.ISubmittedListener submittedListener;

    @Before
    public void before()
    {
        Context context = InstrumentationRegistry.getContext();
        FragmentActivity activity = activityRule.getActivity();
        mainState = new MainStateDer();
        MainState.setInstance(mainState);
        mainState.setContext(activity, activity);

        fragment = new CreateFileFragment();
        fragmentSpy = spy(fragment);
        activityRule.getActivity().setFragment(fragmentSpy);

        submittedListener = mock(CreateFileFragment.ISubmittedListener.class);

        fragmentSpy.submittedListener = submittedListener;
    }

    @After
    public void after()
    {
        activityRule.getActivity().setFragment(null);
    }

    private void testValidPassword(String input) throws Exception
    {
        onView(withId(R.id.createfile_password1ipt)).perform(typeText(input));
        onView(withId(R.id.createfile_password2ipt)).perform(typeText(input));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        verify(documentMock).setPassword(input);
        verify(documentMock).save();
        verify(submittedListener).submitted();

        verify(fragmentSpy, never()).showError(anyString());
    }

    @Test
    public void normalPassword() throws Exception
    {
        testValidPassword(Helper.DEFAULT_PASSWORD);
    }

    @Test
    public void shortPassword() throws Exception
    {
        String shorty = Helper.randomString(CreateFileFragment.MIN_PASSWORD_LENGTH);
        testValidPassword(shorty);
    }

    @Test
    public void longPassword() throws Exception
    {
        String longOne = Helper.randomString(CreateFileFragment.MAX_PASSWORD_LENGTH);
        testValidPassword(longOne);
    }

    private void testInputError(String input1, String input2) throws Exception
    {
        onView(withId(R.id.createfile_password1ipt)).perform(typeText(input1));
        onView(withId(R.id.createfile_password2ipt)).perform(typeText(input2));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        verify(documentMock, never()).setPassword(anyString());
        verify(documentMock, never()).save();
        verify(submittedListener).submitted();

        verify(fragmentSpy).showError(anyString());
    }

    @Test
    public void mismatchPassword() throws Exception
    {
        testInputError(Helper.DEFAULT_PASSWORD, "wrong password");
    }

    @Test
    public void emptyPassword() throws Exception
    {
        testInputError("", "");
    }

    @Test
    public void tooLongPassword() throws Exception
    {
        String longPassword = Helper.randomString(50);
        testInputError(longPassword, longPassword);
    }

    @Test
    public void fileSaveError() throws Exception
    {
        doThrow(new IOException("fail!")).when(documentMock).save();

        onView(withId(R.id.createfile_password1ipt)).perform(typeText(Helper.DEFAULT_PASSWORD));
        onView(withId(R.id.createfile_password2ipt)).perform(typeText(Helper.DEFAULT_PASSWORD));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        verify(documentMock).setPassword(Helper.DEFAULT_PASSWORD);
        verify(documentMock).save();
        verify(submittedListener).submitted();
        verify(fragmentSpy).showError(anyString());
    }

    protected void setIsCreating(final boolean value) throws Exception
    {
        final Object lock = new Object();
        mainState.activity.runOnUiThread(new Runnable() {public void run()
        {
            fragmentSpy.setIsCreating(value);

            synchronized (lock)
            {
                lock.notify();
            }
        }});

        synchronized (lock)
        {
            lock.wait(2000);
        }

        if (value == false)
            onView(withId(R.id.createfile_oldpasswordipt)).check(ViewAssertions.matches(isDisplayed()));
        else
            onView(withId(R.id.createfile_oldpasswordipt)).check(ViewAssertions.matches(Matchers.not(isDisplayed())));
    }

    @Test
    public void isCreatingToggle() throws Exception
    {
        onView(withId(R.id.createfile_oldpasswordipt)).check(ViewAssertions.matches(Matchers.not(isDisplayed())));

        setIsCreating(true);
        setIsCreating(false);
    }

    @Test
    public void currentPasswordMatch() throws Exception
    {
        String oldPass = "oldPass";
        String newPass = "pass";
        documentMock.setPassword(oldPass);
        documentMock.save();
        reset(documentMock);

        setIsCreating(false);

        mainState.mock2PasswordTest = true;

        onView(withId(R.id.createfile_oldpasswordipt)).perform(typeText(oldPass));
        onView(withId(R.id.createfile_password1ipt)).perform(typeText(newPass));
        onView(withId(R.id.createfile_password2ipt)).perform(typeText(newPass));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        verify(documentMock).setPassword(matches(newPass));
        verify(documentMock).save();
        verify(submittedListener).submitted();
    }

    @Test
    public void currentPasswordMismatch() throws Exception
    {
        String oldPass = "oldPass";
        String newPass = "pass";
        documentMock.setPassword(oldPass);
        documentMock.save();
        reset(documentMock);

        setIsCreating(false);

        mainState.mock2PasswordTest = false;

        onView(withId(R.id.createfile_oldpasswordipt)).perform(typeText("not the right password"));
        onView(withId(R.id.createfile_password1ipt)).perform(typeText(newPass));
        onView(withId(R.id.createfile_password2ipt)).perform(typeText(newPass));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        verify(documentMock, never()).setPassword(matches(newPass));
        verify(documentMock, never()).save();
        verify(submittedListener, never()).submitted();
    }
}
