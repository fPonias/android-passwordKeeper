package com.munger.passwordkeeper.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.telecom.Call;
import android.view.View;
import android.widget.EditText;

import com.munger.passwordkeeper.CustomMatchers;
import com.munger.passwordkeeper.Helper;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.TestingMainActivity;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    public class NavigationHelperDer extends NavigationHelper
    {
        public NavigationHelperDer()
        {
            super();
        }

        public void setFragment(ViewDetailFragment fragment)
        {
            viewDetailFragment = fragment;
        }

        private boolean successOnSaveDetails = true;

        public void setSuccessOfSaveDetails(boolean success)
        {
            successOnSaveDetails = success;
        }

        @Override
        public void saveDetail(PasswordDetails detail, Callback callback)
        {
            callback.callback(successOnSaveDetails);
        }

        public boolean lastBackResult;
        public Callback backCallback = null;
        private Object lock = new Object();

        public void waitOnBack()
        {
            synchronized (lock)
            {
                if (backCallback == null)
                    return;

                try{lock.wait(2000);}catch(Exception e){fail();}
            }
        }

        @Override
        public void onBackPressed(final Callback cb)
        {
            synchronized (lock)
            {
                backCallback = new Callback() {public void callback(Object result)
                {
                    lastBackResult = (Boolean) result;
                    cb.callback(result);

                    synchronized (lock)
                    {
                        lock.notify();
                    }
                }};
            }
            super.onBackPressed(backCallback);
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
            navigationMock = new NavigationHelperDer();
            navigationHelper = navigationMock;
        }

        @Override
        public void setupDriveHelper() {
        }

        @Override
        public void setupQuitTimer()
        {
        }
    }

    @Rule
    public ActivityTestRule<TestingMainActivity> activityRule = new ActivityTestRule<>(TestingMainActivity.class);

    private ViewDetailFragment fragment;
    private MainStateDer mainState;
    private PasswordDocImplDer documentMock;
    private NavigationHelperDer navigationMock;
    private EditMenuIdler editMenuIdler;

    @Before
    public void before()
    {
        Context context = InstrumentationRegistry.getContext();
        FragmentActivity activity = activityRule.getActivity();
        documentMock = new PasswordDocImplDer();
        mainState = new MainStateDer();
        MainState.setInstance(mainState);
        mainState.setContext(activity, activity);

        editMenuIdler = new EditMenuIdler();
        registerIdlingResources(editMenuIdler);
    }

    @After
    public void after()
    {
        //activityRule.getActivity().setFragment(null);
        unregisterIdlingResources(editMenuIdler);
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
        navigationMock.setFragment(fragment);
        editMenuIdler.setFragment(fragment);

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
            navigationMock.setSuccessOfSaveDetails(true);
            onView(allOf(withClassName(containsString("Button")), withText("Okay"))).perform(click());
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

        assertEquals(1, fragment.getDetails().count());
        checkDetails(fragment.getDetails());
    }

    @Test
    public void generatePassword()
    {
        populateDetails("name", "location", 1);
        toggleEditable();

        onData(anything()).atPosition(0).onChildView(withChild(withId(R.id.detailitem_valueinput))).perform(longClick());
        onView(withId(R.id.action_detail_random)).perform(click());
        onData(anything()).atPosition(0).onChildView(withId(R.id.detailitem_valueinput)).check(matches(withText(not(isEmptyString()))));

        toggleEditableAndSave();
        assertNotEquals(0, fragment.getDetails().getPair(0).getValue().length());

        checkDetails(fragment.getDetails());
    }

    private void verifyClipboard(String expected)
    {
        ClipboardManager clipboard = (ClipboardManager) activityRule.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboard.getPrimaryClip();
        ClipData.Item it = clip.getItemAt(0);
        String data = it.coerceToText(MainState.getInstance().context).toString();
        assertEquals(expected, data);
    }

    @Test
    public void contextCancel()
    {

    }

    private static class EditMenuIdler implements IdlingResource
    {
        private ResourceCallback callback;
        private ViewDetailFragment target;
        public EditMenuIdler()
        {
        }

        public void setFragment(ViewDetailFragment target)
        {
            this.target = target;
        }

        @Override
        public String getName()
        {
            return "edit menu idler";
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback callback)
        {
            this.callback = callback;
        }

        @Override
        public boolean isIdleNow()
        {
            if (target == null)
                return false;

            if (!target.isActionMenuPrepared())
            {
                callback.onTransitionToIdle();
                return true;
            }
            else
                return false;
        }
    }

    private void verifyEditMenuOnlyCopyVisible()
    {
        onView(withId(R.id.action_detail_copy)).check(matches(isDisplayed()));

        try {
            onView(withId(R.id.action_detail_paste)).check(matches(not(isDisplayed())));
        }catch(NoMatchingViewException e){}

        try {
            onView(withId(R.id.action_detail_random)).check(matches(not(isDisplayed())));
        }catch(NoMatchingViewException e){}
    }

    private void verifyEditMenuAllVisible()
    {
        onView(withId(R.id.action_detail_copy)).check(matches(isDisplayed()));
        onView(withId(R.id.action_detail_paste)).check(matches(isDisplayed()));
        onView(withId(R.id.action_detail_random)).check(matches(isDisplayed()));
    }

    private void verifyEditMenuClosed()
    {
        try {
            onView(withId(R.id.action_detail_copy)).check(matches(not(isDisplayed())));
        }catch(NoMatchingViewException e){}

        try {
            onView(withId(R.id.action_detail_paste)).check(matches(not(isDisplayed())));
        }catch(NoMatchingViewException e){}

        try {
            onView(withId(R.id.action_detail_random)).check(matches(not(isDisplayed())));
        }catch(NoMatchingViewException e){}
    }

    @Test
    public void copyNoEdit()
    {
        populateDetails("name", "location", 1);
        PasswordDetails copy = fragment.getDetails().copy();
        assertFalse(fragment.getEditable());

        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_namelbl)),withClassName(containsString("TextView")))).perform(longClick());
        verifyEditMenuOnlyCopyVisible();
        onView(withId(R.id.action_detail_copy)).perform(click());
        verifyEditMenuClosed();
        verifyClipboard("name");

        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_locationlbl)),withClassName(containsString("TextView")))).perform(longClick());
        verifyEditMenuOnlyCopyVisible();
        onView(withId(R.id.action_detail_copy)).perform(click());
        verifyEditMenuClosed();
        verifyClipboard("location");

        onData(anything()).atPosition(0).onChildView(withId(R.id.detailitem_keylabel)).perform(longClick());
        verifyEditMenuOnlyCopyVisible();
        onView(withId(R.id.action_detail_copy)).perform(click());
        verifyEditMenuClosed();
        verifyClipboard(fragment.getDetails().getPair(0).getKey());

        onData(anything()).atPosition(0).onChildView(withId(R.id.detailitem_valuelabel)).perform(longClick());
        verifyEditMenuOnlyCopyVisible();
        onView(withId(R.id.action_detail_copy)).perform(click());
        verifyEditMenuClosed();
        verifyClipboard(fragment.getDetails().getPair(0).getValue());

        assertFalse(copy.diff(fragment.getDetails()));
    }

    @Test
    public void copy()
    {
        populateDetails("name", "location", 1);
        PasswordDetails copy = fragment.getDetails().copy();

        toggleEditable();
        assertTrue(fragment.getEditable());

        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_namelbl)),withClassName(containsString("EditText")))).perform(longClick());
        verifyEditMenuAllVisible();
        onView(withId(R.id.action_detail_copy)).perform(click());
        verifyEditMenuClosed();
        verifyClipboard("name");

        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_locationlbl)),withClassName(containsString("EditText")))).perform(longClick());
        verifyEditMenuAllVisible();
        onView(withId(R.id.action_detail_copy)).perform(click());
        verifyEditMenuClosed();
        verifyClipboard("location");

        onData(anything()).atPosition(0).onChildView(withId(R.id.detailitem_keyinput)).perform(longClick());
        verifyEditMenuAllVisible();
        onView(withId(R.id.action_detail_copy)).perform(click());
        verifyEditMenuClosed();
        verifyClipboard(fragment.getDetails().getPair(0).getKey());

        onData(anything()).atPosition(0).onChildView(withId(R.id.detailitem_valueinput)).perform(longClick());
        verifyEditMenuAllVisible();
        onView(withId(R.id.action_detail_copy)).perform(click());
        verifyEditMenuClosed();
        verifyClipboard(fragment.getDetails().getPair(0).getValue());

        toggleEditable();
        assertFalse(fragment.getEditable());

        assertFalse(copy.diff(fragment.getDetails()));
    }

    @Test
    public void paste()
    {
        final String targetText = "foo";
        final Object lock = new Object();

        MainState.getInstance().handler.post(new Runnable() {public void run()
        {
            ClipboardManager clipboard = (ClipboardManager) activityRule.getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("password-keeper", targetText);
            clipboard.setPrimaryClip(clip);

            synchronized (lock){ lock.notify();}
        }});

        synchronized (lock)
        {
            try{lock.wait(2000);}catch(Exception e){fail();}
        }

        populateDetails("name", "location", 1);
        PasswordDetails copy = fragment.getDetails().copy();

        toggleEditable();
        assertTrue(fragment.getEditable());

        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_namelbl)),withClassName(containsString("EditText")))).perform(longClick());
        verifyEditMenuAllVisible();
        onView(withId(R.id.action_detail_paste)).perform(click());
        verifyEditMenuClosed();
        verifyClipboard(targetText);

        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_locationlbl)),withClassName(containsString("EditText")))).perform(longClick());
        verifyEditMenuAllVisible();
        onView(withId(R.id.action_detail_paste)).perform(click());
        verifyEditMenuClosed();
        verifyClipboard(targetText);

        onData(anything()).atPosition(0).onChildView(withId(R.id.detailitem_keyinput)).perform(longClick());
        verifyEditMenuAllVisible();
        onView(withId(R.id.action_detail_paste)).perform(click());
        verifyEditMenuClosed();
        verifyClipboard(targetText);

        onData(anything()).atPosition(0).onChildView(withId(R.id.detailitem_valueinput)).perform(longClick());
        verifyEditMenuAllVisible();
        onView(withId(R.id.action_detail_paste)).perform(click());
        verifyEditMenuClosed();
        verifyClipboard(targetText);

        toggleEditableAndSave();

        assertTrue(copy.diff(fragment.getDetails()));
    }

    @Test
    public void menuRandom()
    {
        populateDetails("name", "location", 1);
        PasswordDetails copy = fragment.getDetails().copy();

        toggleEditable();
        assertTrue(fragment.getEditable());

        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_namelbl)),withClassName(containsString("EditText")))).perform(longClick());
        verifyEditMenuAllVisible();
        onView(withId(R.id.action_detail_random)).perform(click());
        verifyEditMenuClosed();

        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_locationlbl)),withClassName(containsString("EditText")))).perform(longClick());
        verifyEditMenuAllVisible();
        onView(withId(R.id.action_detail_random)).perform(click());
        verifyEditMenuClosed();

        onData(anything()).atPosition(0).onChildView(withId(R.id.detailitem_keyinput)).perform(longClick());
        verifyEditMenuAllVisible();
        onView(withId(R.id.action_detail_random)).perform(click());
        verifyEditMenuClosed();

        onData(anything()).atPosition(0).onChildView(withId(R.id.detailitem_valueinput)).perform(longClick());
        verifyEditMenuAllVisible();
        onView(withId(R.id.action_detail_random)).perform(click());
        verifyEditMenuClosed();

        toggleEditableAndSave();

        assertTrue(copy.diff(fragment.getDetails()));
    }

    private void doInteractions()
    {
        fragment.getDetails().setHistory(new PasswordDocumentHistory());

        toggleEditable();

        onData(anything()).atPosition(0).onChildView(withId(R.id.detailitem_keyinput)).perform(clearText(), typeText("value3"));
        onData(anything()).atPosition(0).onChildView(withId(R.id.detailitem_valueinput)).perform(clearText(), typeText("value4"));
        onView(withId(R.id.viewdetail_addbtn)).perform(click());
        onData(anything()).atPosition(1).onChildView(withId(R.id.detailitem_keyinput)).perform(clearText(), typeText("value5"));
        onData(anything()).atPosition(1).onChildView(withId(R.id.detailitem_valueinput)).perform(clearText(), typeText("value6"));
        onData(anything()).atPosition(0).onChildView(withId(R.id.detailitem_deletebtn)).perform(click());
        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_namelbl)),withClassName(containsString("EditText")))).perform(clearText(), typeText("value1"));
        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_locationlbl)),withClassName(containsString("EditText")))).perform(clearText(), typeText("value2"));
    }

    @Test
    public void save()
    {
        populateDetails("name", "location", 1);
        PasswordDetails copy = fragment.getDetails().copy();
        doInteractions();

        toggleEditableAndSave();

        PasswordDetails newDets = fragment.getDetails();
        assertTrue(copy.diff(newDets));
        int cnt = newDets.getHistory().count();
        assertTrue(8 <= cnt && cnt <= 14);
        assertEquals("value1", newDets.getName());
        assertEquals("value2", newDets.getLocation());
        assertEquals(1, newDets.count());
        assertEquals("value5", newDets.getPair(0).getKey());
        assertEquals("value6", newDets.getPair(0).getValue());
        assertEquals(0, activityRule.getActivity().getBackCalledCount());
    }

    @Test
    public void saveOnBack()
    {
        populateDetails("name", "location", 1);
        PasswordDetails copy = fragment.getDetails().copy();
        doInteractions();

        Espresso.closeSoftKeyboard();
        Espresso.pressBack();
        onView(allOf(withClassName(containsString("Button")), withText("Okay"))).perform(click());
        navigationMock.waitOnBack();

        PasswordDetails newDets = fragment.getDetails();
        assertTrue(copy.diff(newDets));
        int cnt = newDets.getHistory().count();
        assertTrue(8 <= cnt && cnt <= 14);
        assertEquals("value1", newDets.getName());
        assertEquals("value2", newDets.getLocation());
        assertEquals(1, newDets.count());
        assertEquals("value5", newDets.getPair(0).getKey());
        assertEquals("value6", newDets.getPair(0).getValue());

        assertEquals(1, activityRule.getActivity().getBackCalledCount());
    }

    @Test
    public void cancel()
    {
        populateDetails("name", "location", 1);
        PasswordDetails copy = fragment.getDetails().copy();
        doInteractions();

        checkEditable();
        onView(withId(R.id.action_edit)).perform(click());
        onView(allOf(withClassName(containsString("Button")), withText("Cancel"))).perform(click());
        checkEditable();
        assertEquals(0, activityRule.getActivity().getBackCalledCount());

        PasswordDetails newDets = fragment.getDetails();
        assertTrue(copy.diff(newDets));
    }

    @Test
    public void cancelOnBack()
    {
        populateDetails("name", "location", 1);
        PasswordDetails copy = fragment.getDetails().copy();
        doInteractions();

        checkEditable();
        Espresso.closeSoftKeyboard();
        Espresso.pressBack();
        onView(allOf(withClassName(containsString("Button")), withText("Cancel"))).perform(click());
        navigationMock.waitOnBack();
        checkEditable();
        assertEquals(0, activityRule.getActivity().getBackCalledCount());
        assertFalse(navigationMock.lastBackResult);

        PasswordDetails newDets = fragment.getDetails();
        assertTrue(copy.diff(newDets));
    }

    @Test
    public void discard()
    {
        populateDetails("name", "location", 1);
        PasswordDetails copy = fragment.getDetails().copy();
        doInteractions();

        checkEditable();
        onView(withId(R.id.action_edit)).perform(click());
        onView(allOf(withClassName(containsString("Button")), withText("Discard"))).perform(click());
        checkNotEditable();
        assertEquals(0, activityRule.getActivity().getBackCalledCount());

        PasswordDetails newDets = fragment.getDetails();
        assertFalse(copy.diff(newDets));
    }

    @Test
    public void discardOnBack()
    {
        populateDetails("name", "location", 1);
        PasswordDetails copy = fragment.getDetails().copy();
        doInteractions();

        checkEditable();
        Espresso.closeSoftKeyboard();
        Espresso.pressBack();
        onView(allOf(withClassName(containsString("Button")), withText("Discard"))).perform(click());
        navigationMock.waitOnBack();
        checkEditable();
        assertTrue(navigationMock.lastBackResult);

        PasswordDetails newDets = fragment.getDetails();
        assertFalse(copy.diff(newDets));
    }
}
