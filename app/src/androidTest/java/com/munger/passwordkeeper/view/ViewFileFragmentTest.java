package com.munger.passwordkeeper.view;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.SearchView;
import android.view.MenuItem;
import android.widget.Button;

import com.munger.passwordkeeper.CustomMatchers;
import com.munger.passwordkeeper.Helper;
import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.TestingMainActivity;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.action.ViewActions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created by codymunger on 11/28/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ViewFileFragmentTest
{
    public class PasswordDocImplDer extends Helper.PasswordDocumentImpl
    {
        public int saveCount = 0;
        @Override
        public void save() throws Exception
        {
            saveCount++;
            super.save();
        }
    }

    public class MainStateDer extends MainState
    {
        @Override
        protected void setupDocument()
        {
            document = documentMock;
        }

        @Override
        protected void setupNavigation()
        {
            navigationMock = mock(NavigationHelper.class);
            navigationHelper = navigationMock;
        }

        @Override
        public void setupDriveHelper() {
        }
    }

    @Rule
    public ActivityTestRule<TestingMainActivity> activityRule = new ActivityTestRule<>(TestingMainActivity.class);

    private ViewFileFragment fragment;
    private MainStateDer mainState;
    private PasswordDocImplDer documentMock;
    private NavigationHelper navigationMock;

    @Before
    public void before()
    {
        Context context = InstrumentationRegistry.getContext();
        FragmentActivity activity = activityRule.getActivity();
        documentMock = new PasswordDocImplDer();
        mainState = new MainStateDer();
        MainState.setInstance(mainState);
        mainState.setContext(activity, activity);
    }

    @After
    public void after()
    {
        activityRule.getActivity().setFragment(null);
    }

    @Test
    public void emptyFileView()
    {
        int sz = 0;
        Helper.fillDocument(documentMock, sz, 1);
        assertEquals(sz, documentMock.count());

        fragment = new ViewFileFragment();
        activityRule.getActivity().setFragment(fragment);
        checkRepresentation();
    }

    public static Matcher<PasswordDetails> detailHasId(final String id)
    {
        return new TypeSafeMatcher<PasswordDetails>()
        {
            @Override
            protected boolean matchesSafely(PasswordDetails item)
            {
                return item.getId().equals(id);
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText("password detail id: ");
                description.appendValue(id);
            }
        };
    }

    private void checkRepresentation()
    {
        int sz = documentMock.count();

        for (int i = 0; i < sz; i++)
        {
            PasswordDetails dets = documentMock.getDetails(i);
            onData(is(instanceOf(PasswordDetails.class))).atPosition(i).check(matches(hasDescendant(withText(dets.getName()))));
        }

        boolean thrown = false;

        try
        {
            onData(is(instanceOf(PasswordDetails.class))).atPosition(sz).check(doesNotExist());
        }
        catch(Exception e){
            thrown = true;
        }

        assertTrue(thrown);
    }

    @Test
    public void smallFileView()
    {
        int sz = 3;
        Helper.fillDocument(documentMock, sz, 1);
        assertEquals(sz, documentMock.count());

        fragment = new ViewFileFragment();
        activityRule.getActivity().setFragment(fragment);
        checkRepresentation();
    }

    @Test
    public void largeFileView()
    {
        int sz = 100;
        Helper.fillDocument(documentMock, sz, 1);
        assertEquals(sz, documentMock.count());

        fragment = new ViewFileFragment();
        activityRule.getActivity().setFragment(fragment);
        checkRepresentation();
    }

    private void doSelect(int index)
    {
        PasswordDetails dets = documentMock.getDetails(index);
        onData(is(instanceOf(PasswordDetails.class))).atPosition(index).perform(click());

        verify(navigationMock).openDetail(dets);
    }

    @Test
    public void selectFirst()
    {
        Helper.fillDocument(documentMock, 3, 1);
        assertEquals(3, documentMock.count());

        fragment = new ViewFileFragment();
        activityRule.getActivity().setFragment(fragment);

        doSelect(0);
    }

    @Test
    public void selectNotFirst()
    {
        Helper.fillDocument(documentMock, 10, 1);
        assertEquals(10, documentMock.count());

        fragment = new ViewFileFragment();
        activityRule.getActivity().setFragment(fragment);

        doSelect(4);
    }

    @Test
    public void scrollAndSelect()
    {
        Helper.fillDocument(documentMock, 30, 1);
        assertEquals(30, documentMock.count());

        fragment = new ViewFileFragment();
        activityRule.getActivity().setFragment(fragment);

        doSelect(20);
    }

    @Test
    public void scrollAndSelectLast()
    {
        Helper.fillDocument(documentMock, 30, 1);
        assertEquals(30, documentMock.count());

        fragment = new ViewFileFragment();
        activityRule.getActivity().setFragment(fragment);

        doSelect(29);
    }

    @Test
    public void searchAndSelect() throws Exception
    {
        PasswordDetails dets = new PasswordDetails("1");
        dets.setName("foo1");
        documentMock.addDetails(dets);
        dets = new PasswordDetails("2");
        dets.setName("bar1");
        documentMock.addDetails(dets);
        dets = new PasswordDetails("3");
        dets.setName("2foo2");
        documentMock.addDetails(dets);
        dets = new PasswordDetails("4");
        dets.setName("2bar2");
        documentMock.addDetails(dets);
        dets = new PasswordDetails("5");
        dets.setName("foo1");
        documentMock.addDetails(dets);
        dets = new PasswordDetails("6");
        dets.setName("bar1");
        documentMock.addDetails(dets);
        dets = new PasswordDetails("7");
        dets.setName("1foo");
        documentMock.addDetails(dets);
        dets = new PasswordDetails("8");
        dets.setName("1bar");
        documentMock.addDetails(dets);
        dets = new PasswordDetails("9");
        dets.setName("foo");
        documentMock.addDetails(dets);
        dets = new PasswordDetails("10");
        dets.setName("bar");
        documentMock.addDetails(dets);
        dets = new PasswordDetails("11");
        dets.setName("foo foo foo foo");
        documentMock.addDetails(dets);
        dets = new PasswordDetails("12");
        dets.setName("bar bar bar bar");
        documentMock.addDetails(dets);
        dets = new PasswordDetails("13");
        dets.setName("confusing text foo");
        documentMock.addDetails(dets);
        dets = new PasswordDetails("14");
        dets.setName("confusing text bar");
        documentMock.addDetails(dets);
        dets = new PasswordDetails("15");
        dets.setName("doesn't match");
        documentMock.addDetails(dets);

        fragment = new ViewFileFragment();
        activityRule.getActivity().setFragment(fragment);

        int sz = documentMock.count();
        assertTrue(sz > 0);

        onView(withId(R.id.action_search)).perform(click());
        onView(withClassName(containsString("SearchAutoComplete"))).perform(typeText("foo"));
        checkResults(new String[] {"1", "3", "5", "7", "9", "11", "13"});

        onView(withClassName(containsString("SearchAutoComplete"))).perform(clearText(), typeText("bar"));
        checkResults(new String[] {"2", "4", "6", "8", "10", "12", "14"});

        onView(withClassName(containsString("SearchAutoComplete"))).perform(clearText(), typeText("none"));
        checkResults(new String[] {});

        onView(allOf(withContentDescription("Collapse"), isDescendantOfA(withClassName(containsString("ActionBar"))))).perform(click());

        boolean thrown = false;
        try
        {
            onView(withClassName(containsString("SearchAutoComplete"))).check(matches(is(not(isDisplayed()))));
        }
        catch(Exception e){
            thrown = true;
        }
        assertTrue(thrown);
    }

    private void checkResults(String[] expectedIds)
    {
        int expectedMatches = expectedIds.length;

        for (int i = 0; i < expectedIds.length; i++)
        {
            String id = expectedIds[i];
            PasswordDetails match = documentMock.getDetails(id);

            onData(is(instanceOf(PasswordDetails.class))).atPosition(i).perform(click());
            verify(navigationMock).openDetail(match);
        }

        boolean thrown = false;

        try
        {
            onData(is(instanceOf(PasswordDetails.class))).atPosition(expectedMatches).check(doesNotExist());
        }
        catch(Exception e){
            thrown = true;
        }

        assertTrue(thrown);
    }

    private void checkEditable()
    {
        int sz = documentMock.count();

        onView(withId(R.id.viewfile_addbtn)).check(matches(isDisplayed()));
        for (int i = 0; i < sz; i++)
            onData(is(instanceOf(PasswordDetails.class))).atPosition(i).check(selectedDescendantsMatch(is(withClassName(containsString("Button"))),is(isDisplayed())));
    }

    private void checkNotEditable()
    {
        int sz = documentMock.count();

        onView(withId(R.id.viewfile_addbtn)).check(matches(not(isDisplayed())));
        for (int i = 0; i < sz; i++)
            onData(is(instanceOf(PasswordDetails.class))).atPosition(i).check(selectedDescendantsMatch(is(withClassName(containsString("Button"))),is(not(isDisplayed()))));
    }

    private void doToggle()
    {
        int sz = documentMock.count();

        boolean editable = fragment.getEditable();
        if (editable)
            checkEditable();
        else
            checkNotEditable();

        onView(withId(R.id.action_edit)).perform(click());

        assertNotEquals(editable, fragment.getEditable());
        if (fragment.getEditable())
            checkEditable();
        else
            checkNotEditable();
    }


    @Test
    public void toggleEdit()
    {
        int sz = 3;
        Helper.fillDocument(documentMock, sz, 1);
        assertEquals(sz, documentMock.count());

        fragment = new ViewFileFragment();
        activityRule.getActivity().setFragment(fragment);

        doToggle();
    }

    @Test
    public void newEntry()
    {
        int sz = 3;
        Helper.fillDocument(documentMock, sz, 1);
        assertEquals(sz, documentMock.count());

        fragment = new ViewFileFragment();
        activityRule.getActivity().setFragment(fragment);

        doToggle();

        for (int i = 0; i < 10; i++)
        {
            onView(withId(R.id.viewfile_addbtn)).perform(click());
            assertEquals(sz + i + 1, documentMock.count());
            assertEquals(i + 1, documentMock.saveCount);
        }

        doToggle();
        checkRepresentation();
    }

    @Test
    public void deleteEntryYes()
    {
        int sz = 5;
        Helper.fillDocument(documentMock, sz, 1);
        assertEquals(sz, documentMock.count());

        fragment = new ViewFileFragment();
        activityRule.getActivity().setFragment(fragment);

        doToggle();

        for (int i = 0; i < 2; i++)
        {
            onData(is(instanceOf(PasswordDetails.class))).atPosition(1).onChildView(is(withClassName(containsString("Button")))).perform(click());

            onView(withText("Okay")).perform(click());

            assertEquals(sz - i - 1, documentMock.count());
            assertEquals(i + 1, documentMock.saveCount);
        }

        doToggle();
        checkRepresentation();
    }

    @Test
    public void deleteEntryNo()
    {
        int sz = 5;
        Helper.fillDocument(documentMock, sz, 1);
        assertEquals(sz, documentMock.count());

        fragment = new ViewFileFragment();
        activityRule.getActivity().setFragment(fragment);

        doToggle();

        for (int i = 0; i < 2; i++)
        {
            onData(is(instanceOf(PasswordDetails.class))).atPosition(1).onChildView(is(withClassName(containsString("Button")))).perform(click());

            onView(withText("Cancel")).perform(click());

            assertEquals(sz, documentMock.count());
            assertEquals(0, documentMock.saveCount);
        }

        doToggle();
        checkRepresentation();
    }

    @Test
    public void deleteAllEntries()
    {
        int sz = 5;
        Helper.fillDocument(documentMock, sz, 1);
        assertEquals(sz, documentMock.count());

        fragment = new ViewFileFragment();
        activityRule.getActivity().setFragment(fragment);

        doToggle();

        for (int i = 0; i < sz; i++)
        {
            onData(is(instanceOf(PasswordDetails.class))).atPosition(0).onChildView(is(withClassName(containsString("Button")))).perform(click());

            onView(withText("Okay")).perform(click());

            assertEquals(sz - i - 1, documentMock.count());
            assertEquals(i + 1, documentMock.saveCount);
        }

        doToggle();
        checkRepresentation();
    }
}
