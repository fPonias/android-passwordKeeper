package com.munger.passwordkeeper.view;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.DataInteraction;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.munger.passwordkeeper.CustomMatchers;
import com.munger.passwordkeeper.Helper;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.TestingMainActivity;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import com.munger.passwordkeeper.view.widget.DetailItemWidget;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.action.ViewActions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.mock;

/**
 * Created by codymunger on 11/29/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ViewDetailFragmentTest
{
    public class PasswordDocImplDer extends Helper.PasswordDocumentImpl
    {

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

    private ViewDetailFragment fragment;
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

    public static Matcher<View> pairWithKey(final String key)
    {
        return new TypeSafeMatcher<View>()
        {
            int currentIndex = 0;

            @Override
            public void describeTo(Description description)
            {
                description.appendText("pair widget with key: ");
                description.appendValue(key);
            }

            @Override
            public boolean matchesSafely(View view)
            {
                if (!(view instanceof DetailItemWidget))
                    return false;

                String wkey = ((DetailItemWidget)view).getKey();
                if (wkey == null)
                    return false;

                return wkey.equals(key);
            }
        };
    }

    public static Matcher<View> pairWithValue(final String value)
    {
        return new TypeSafeMatcher<View>()
        {
            int currentIndex = 0;

            @Override
            public void describeTo(Description description)
            {
                description.appendText("pair widget with value: ");
                description.appendValue(value);
            }

            @Override
            public boolean matchesSafely(View view)
            {
                if (!(view instanceof DetailItemWidget))
                    return false;

                String wval = ((DetailItemWidget)view).getValue();
                if (wval == null)
                    return false;

                return wval.equals(value);
            }
        };
    }

    private void checkDetails(PasswordDetails dets)
    {
        String name = dets.getName();
        onView(withId(R.id.viewdetail_namelbl)).check(matches(hasDescendant(withText(name))));

        String loc = dets.getLocation();
        onView(withId(R.id.viewdetail_locationlbl)).check(matches(hasDescendant(withText(loc))));

        int sz = dets.count();
        for (int i = 0; i < sz; i++)
        {
            PasswordDetailsPair pair = dets.getPair(i);
            DataInteraction inter = onData(is(instanceOf(PasswordDetailsPair.class))).atPosition(i);
            inter.check(matches(allOf(pairWithKey(pair.getKey()), pairWithValue(pair.getValue()))));
        }

        if (sz > 0)
        {
            boolean thrown = false;
            try
            {
                onData(is(instanceOf(PasswordDetailsPair.class))).atPosition(sz).check(doesNotExist());
            }
            catch(Exception e){
                thrown = true;
            }
            assertTrue(thrown);
        }
        else
        {
            DataInteraction inter = onData(is(instanceOf(PasswordDetailsPair.class))).atPosition(0);
            inter.check(matches(allOf(pairWithKey(""), pairWithValue(""))));
        }

    }

    private PasswordDetails populateDetails(String name, String loc, int pairSz)
    {
        PasswordDetails dets = new PasswordDetails();
        fragment = new ViewDetailFragment();

        if (name != null)
            dets.setName(name);

        if (loc != null)
            dets.setLocation(loc);

        for (int i = 0; i < pairSz; i++)
        {
            PasswordDetailsPair pair = dets.addEmptyPair();
            pair.setKey("key" + i);
            pair.setValue("value" + i);
        }

        fragment.setDetails(dets);
        activityRule.getActivity().setFragment(fragment);

        return dets;
    }

    @Test
    public void emptyDetails()
    {
        PasswordDetails dets = populateDetails(null, null, 0);
        checkDetails(dets);
    }

    @Test
    public void emptyDetailsWithName()
    {
        PasswordDetails dets = populateDetails("name", null, 0);
        checkDetails(dets);
    }

    @Test
    public void emptyDetailsWithLocation()
    {
        PasswordDetails dets = populateDetails(null, "location", 0);
        checkDetails(dets);
    }

    @Test
    public void detailsWithAFewPairs()
    {
        PasswordDetails dets = populateDetails("name", "location", 3);
        checkDetails(dets);
    }

    @Test
    public void detailsWithManyPairs()
    {
        PasswordDetails dets = populateDetails("name", "location", 100);
        checkDetails(dets);
    }

    private void toggleEditable()
    {
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

    private void toggleEditableAndSave()
    {
        boolean editable = fragment.getEditable();
        if (editable)
            checkEditable();
        else
            checkNotEditable();

        onView(withId(R.id.action_edit)).perform(click());


        if (editable)
        {
            final Object lock = new Object();
            doAnswer(new Answer() {public Object answer(final InvocationOnMock invocationOnMock) throws Throwable
            {
                MainState.getInstance().handler.post(new Runnable() {public void run()
                {
                    NavigationHelper.Callback callback = invocationOnMock.getArgumentAt(1, NavigationHelper.Callback.class);
                    callback.callback(true);

                    synchronized (lock)
                    {
                        lock.notify();
                    }
                }});

                return null;
            }}).when(navigationMock).saveDetail(any(PasswordDetails.class), any(NavigationHelper.Callback.class));

            onView(allOf(withClassName(containsString("Button")), withText("Okay"))).perform(click());

            synchronized (lock){
                try{lock.wait(2000);}catch(Exception e){fail();}
            }
        }

        assertNotEquals(editable, fragment.getEditable());
        if (fragment.getEditable())
            checkEditable();
        else
            checkNotEditable();
    }

    private void checkEditable()
    {
        int sz = fragment.getDetails().count();

        onView(withId(R.id.viewdetail_addbtn)).check(matches(isDisplayed()));
        for (int i = 0; i < sz; i++)
            onData(is(instanceOf(PasswordDetailsPair.class))).atPosition(i).check(selectedDescendantsMatch(is(withClassName(containsString("Button"))),is(isDisplayed())));
    }

    private void checkNotEditable()
    {
        int sz = fragment.getDetails().count();

        CustomMatchers.assertHiddenOrDoesNotExist(onView(withId(R.id.viewdetail_addbtn)));
        for (int i = 0; i < sz; i++)
            onData(is(instanceOf(PasswordDetailsPair.class))).atPosition(i).check(selectedDescendantsMatch(is(withClassName(containsString("Button"))),is(not(isDisplayed()))));
    }



    @Test
    public void toggleEdit()
    {
        populateDetails("name", "location", 10);
        toggleEditable();
        toggleEditable();
    }

    @Test
    public void addPairs()
    {
        populateDetails("name", "location", 3);
        toggleEditable();

        for (int i = 0; i < 10; i++)
            onView(withId(R.id.viewdetail_addbtn)).perform(click());

        assertEquals(13, fragment.getDetails().count());

        toggleEditableAndSave();

        checkDetails(fragment.getDetails());
    }

    @Test
    public void deletePairs()
    {
        populateDetails("name", "location", 5);
        toggleEditable();

        PasswordDetailsPair toDelete = fragment.getDetails().getPair(1);
        onData(anything()).atPosition(1).onChildView(withClassName(containsString("Button"))).perform(click());

        assertEquals(4, fragment.getDetails().count());
        assertNull(fragment.getDetails().getPair(toDelete.getId()));
        checkDetails(fragment.getDetails());

        toggleEditableAndSave();
        toggleEditable();

        toDelete = fragment.getDetails().getPair(3);
        onData(anything()).atPosition(3).onChildView(withClassName(containsString("Button"))).perform(click());

        assertEquals(3, fragment.getDetails().count());
        assertNull(fragment.getDetails().getPair(toDelete.getId()));
        checkDetails(fragment.getDetails());

        toggleEditableAndSave();

        checkDetails(fragment.getDetails());
    }

    @Test
    public void deleteAllPairs()
    {
        populateDetails("name", "location", 10);
        toggleEditable();

        for (int i = 0; i < 10; i++)
        {
            onData(anything()).atPosition(0).onChildView(withClassName(containsString("Button"))).perform(click());
        }

        toggleEditableAndSave();

        assertEquals(0, fragment.getDetails().count());
        checkDetails(fragment.getDetails());
    }

    @Test
    public void generatePassword()
    {
        populateDetails("name", "location", 0);
        toggleEditable();


        toggleEditableAndSave();

        checkDetails(fragment.getDetails());
    }

    @Test
    public void copy()
    {

    }

    @Test
    public void paste()
    {

    }

    @Test
    public void save()
    {

    }

    @Test
    public void cancel()
    {

    }

    @Test
    public void discard()
    {

    }
}
