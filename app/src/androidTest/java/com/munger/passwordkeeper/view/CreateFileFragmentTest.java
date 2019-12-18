package com.munger.passwordkeeper.view;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.munger.passwordkeeper.CustomMatchers;
import com.munger.passwordkeeper.Helper;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.TestingMainActivity;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;

import androidx.fragment.app.FragmentActivity;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

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
                doReturn(mock2PasswordTest).when(documentMock2).testPassword(anyString());
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
    public void before() throws InterruptedException
    {
        FragmentActivity activity = activityRule.getActivity();

        mainState = new MainStateDer();
        MainState.setInstance(mainState);
        mainState.setContext(activity, activity);

        fragment = new CreateFileFragment();
        fragmentSpy = spy(fragment);
        activityRule.getActivity().setFragment(fragmentSpy);
        doCallRealMethod().when(fragmentSpy).validate();
        doCallRealMethod().when(fragmentSpy).validateOldPassword();

        submittedListener = mock(CreateFileFragment.ISubmittedListener.class);

        fragmentSpy.submittedListener = submittedListener;
    }

    private void testValidPassword(String input) throws Exception
    {
        CustomMatchers.assertDoesExist(onView(withId(R.id.createfile_password1ipt)));
        onView(withId(R.id.createfile_password1ipt)).perform(typeText(input));
        onView(withId(R.id.createfile_password2ipt)).perform(typeText(input), closeSoftKeyboard());
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
        onView(withId(R.id.createfile_password2ipt)).perform(typeText(input2), closeSoftKeyboard());
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        verify(documentMock, never()).setPassword(anyString());
        verify(documentMock, never()).save();
        verify(submittedListener, never()).submitted();

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
        onView(withId(R.id.createfile_password2ipt)).perform(typeText(Helper.DEFAULT_PASSWORD), closeSoftKeyboard());
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        verify(documentMock).setPassword(Helper.DEFAULT_PASSWORD);
        verify(documentMock).save();
        verify(submittedListener, never()).submitted();
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
    public void currentPasswordMatch() throws Exception
    {
        String oldPass = "oldPass";
        String newPass = "pass";
        documentMock.setPassword(oldPass);
        documentMock.save();
        reset(documentMock);

        setIsCreating(false);

        doReturn(true).when(documentMock).testPassword(oldPass);

        onView(withId(R.id.createfile_oldpasswordipt)).perform(typeText(oldPass));
        onView(withId(R.id.createfile_password1ipt)).perform(typeText(newPass));
        onView(withId(R.id.createfile_password2ipt)).perform(typeText(newPass), closeSoftKeyboard());
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
        onView(withId(R.id.createfile_password2ipt)).perform(typeText(newPass), closeSoftKeyboard());
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        verify(documentMock, never()).setPassword(matches(newPass));
        verify(documentMock, never()).save();
        verify(submittedListener, never()).submitted();
    }
}
