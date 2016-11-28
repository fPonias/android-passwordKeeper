package com.munger.passwordkeeper.view;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.renderscript.ScriptGroup;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.test.mock.MockContext;
import android.view.View;

import com.munger.passwordkeeper.Helper;
import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.struct.ConfigFactory;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.matchers.Null;


import java.io.IOException;
import java.io.InputStream;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.action.ViewActions.*;
import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by codymunger on 11/25/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class CreateFileFragmentTest
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
    }

    @Rule
    public ActivityTestRule<Helper.BlankActivity> activityRule = new ActivityTestRule<>(Helper.BlankActivity.class);

    private CreateFileFragment fragment;
    private CreateFileFragment fragmentSpy;
    private MainStateDer mainState;
    private PasswordDocument documentMock;
    private NavigationHelper navigationMock;

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
    }

    private void testValidPassword(String input) throws Exception
    {
        onView(withId(R.id.createfile_password1ipt)).perform(typeText(input));
        onView(withId(R.id.createfile_password2ipt)).perform(typeText(input));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        verify(documentMock).setPassword(input);
        verify(documentMock).save();
        verify(navigationMock).openFile();

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
        verify(navigationMock, never()).openFile();

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
        verify(navigationMock, never()).openFile();
        verify(fragmentSpy).showError(anyString());
    }
}
