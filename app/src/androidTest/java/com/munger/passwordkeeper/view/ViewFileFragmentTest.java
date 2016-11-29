package com.munger.passwordkeeper.view;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.FragmentActivity;

import com.munger.passwordkeeper.Helper;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Created by codymunger on 11/28/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ViewFileFragmentTest
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

    private ViewFileFragment fragment;
    private ViewFileFragment fragmentSpy;
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

        fragment = new ViewFileFragment();
        fragmentSpy = spy(fragment);
        activityRule.getActivity().setFragment(fragmentSpy);
    }

    @After
    public void after()
    {
        activityRule.getActivity().setFragment(null);
    }

    @Test
    public void emptyFileView()
    {

    }

    @Test
    public void smallFileView()
    {

    }

    @Test
    public void largeFileView()
    {

    }

    @Test
    public void select()
    {

    }

    @Test
    public void scrollAndSelect()
    {

    }

    @Test
    public void searchAndSelect()
    {

    }

    @Test
    public void searchNoResults()
    {
        
    }

    @Test
    public void toggleEdit()
    {

    }

    @Test
    public void newEntry()
    {

    }

    @Test
    public void deleteEntry()
    {

    }

    @Test
    public void deleteAllEntries()
    {

    }

    @Test
    public void settingsClicked()
    {

    }
}
