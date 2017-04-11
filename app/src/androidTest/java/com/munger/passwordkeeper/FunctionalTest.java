package com.munger.passwordkeeper;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.munger.passwordkeeper.helpers.KeyboardListenerTest;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.helpers.QuitTimer;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import com.munger.passwordkeeper.struct.Settings;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;
import com.munger.passwordkeeper.view.ViewDetailFragment;
import com.munger.passwordkeeper.view.ViewFileFragment;
import com.munger.passwordkeeper.view.widget.DetailItemWidget;
import com.munger.passwordkeeper.view.widget.TextInputWidget;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.clearText;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created by codymunger on 12/6/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FunctionalTest
{
    private MainState mainState;
    private Context context;

    @Rule
    public ActivityTestRule<TestingMainActivity> activityRule = new ActivityTestRule<>(TestingMainActivity.class);


    @Before
    public void before() throws Exception
    {
        context = InstrumentationRegistry.getContext();
        FragmentActivity activity = activityRule.getActivity();
        mainState = MainState.getInstance();
        mainState.setContext(activity, activity);
        mainState.document.delete();
    }

    @Test
    public void createFileFail() throws Exception
    {
        activityRule.getActivity().superInit();

        onView(withId(R.id.createfile_password1ipt)).perform(typeText("pass"));
        onView(withId(R.id.createfile_password2ipt)).perform(typeText("no match"));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        onView(allOf(withClassName(containsString("Button")), withText(R.string.alert_okay))).perform(click());

        onView(withId(R.id.createfile_password1ipt)).perform(clearText()).perform(typeText("pass"));
        onView(withId(R.id.createfile_password2ipt)).perform(clearText()).perform(typeText("pass"));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        onView(allOf(withClassName(containsString("Button")), withText(R.string.alert_okay))).check(doesNotExist());
    }

    @Test
    public void openFileFail() throws Exception
    {
        String password = "pass";
        mainState.document.setPassword(password);
        mainState.document.save();

        activityRule.getActivity().superInit();

        onView(withClassName(containsString("EditText"))).perform(typeText("wrongpass"));
        onView(allOf(withClassName(containsString("Button")), withText(R.string.input_okay))).perform(click());

        onView(allOf(withClassName(containsString("Button")), withText(R.string.alert_okay))).perform(click());


        onView(withClassName(containsString("EditText"))).perform(clearText()).perform(typeText(password));
        onView(allOf(withClassName(containsString("Button")), withText(R.string.input_okay))).perform(click());

        onView(allOf(withClassName(containsString("Button")), withText(R.string.alert_okay))).check(doesNotExist());

    }

    @Test
    public void createFileEditDefault() throws Exception
    {
        //password view
        activityRule.getActivity().superInit();

        onView(withId(R.id.createfile_password1ipt)).perform(clearText()).perform(typeText("pass"));
        onView(withId(R.id.createfile_password2ipt)).perform(clearText()).perform(typeText("pass"));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        //document view
        Fragment frag = ((MainActivity) mainState.activity).getCurrentFagment();
        assertTrue(frag instanceof ViewFileFragment);
        onData(is(instanceOf(PasswordDetails.class))).atPosition(0).check(matches(hasDescendant(withText(R.string.new_entry_title))));

        boolean thrown = false;

        try
        {
            onData(is(instanceOf(PasswordDetails.class))).atPosition(1).check(doesNotExist());
        }
        catch(Exception e){
            thrown = true;
        }

        assertTrue(thrown);
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

    public static Matcher<View> inputWithHint(final String value)
    {
        return new TypeSafeMatcher<View>()
        {
            @Override
            protected boolean matchesSafely(View view)
            {
                if (!(view instanceof TextInputWidget))
                    return false;

                String hintVal = ((TextInputWidget)view).getInput().getHint().toString();
                if (hintVal == null)
                    return false;

                return hintVal.equals(value);
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText("input widget with hint: ");
                description.appendValue(value);
            }
        };
    }

    private String name = "a name";
    private String location = "a location";
    private String detKey1 = "a key 1";
    private String detValue1 = "a value 1";
    private String detKey2 = "a key 2";
    private String detValue2 = "a value 2";

    private void createFileEditNewEntry() throws Exception
    {
        //password view
        activityRule.getActivity().superInit();

        onView(withId(R.id.createfile_password1ipt)).perform(clearText()).perform(typeText("pass"));
        onView(withId(R.id.createfile_password2ipt)).perform(clearText()).perform(typeText("pass"));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        //document view
        MainActivity activity = (MainActivity) MainState.getInstance().activity;
        Fragment frag = activity.getCurrentFagment();
        assertTrue(frag instanceof ViewFileFragment);
        onData(is(instanceOf(PasswordDetails.class))).atPosition(0).perform(click());

        //details view
        frag = activity.getCurrentFagment();
        assertTrue(frag instanceof ViewDetailFragment);
        ViewDetailFragment detFrag = (ViewDetailFragment) frag;
        assertEquals(true, detFrag.getEditable());

        onView(withId(R.id.viewdetail_namelbl)).check(matches(hasDescendant(withText(""))));
        onView(withId(R.id.viewdetail_locationlbl)).check(matches(hasDescendant(withText(""))));
        DataInteraction inter = onData(is(instanceOf(PasswordDetailsPair.class))).atPosition(0);
        inter.check(matches(allOf(pairWithKey(""), pairWithValue(""))));

        boolean thrown = false;
        try
        {
            onData(is(instanceOf(PasswordDetailsPair.class))).atPosition(1).check(doesNotExist());
        }
        catch(Exception e){
            thrown = true;
        }
        assertTrue(thrown);

        //edit details
        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_namelbl)),withClassName(containsString("EditText")))).perform(clearText(), typeText(name));
        onView(allOf(isDescendantOfA(withId(R.id.viewdetail_locationlbl)),withClassName(containsString("EditText")))).perform(clearText(), typeText(location));
        onData(anything()).atPosition(0).onChildView(withId(R.id.detailitem_keyinput)).perform(clearText(), typeText(detKey1));
        Thread.sleep(25);
        onData(anything()).atPosition(0).onChildView(withId(R.id.detailitem_valueinput)).perform(clearText(), typeText(detValue1));
        Thread.sleep(25);

        onView(withId(R.id.viewdetail_addbtn)).perform(click());
        onData(anything()).atPosition(1).onChildView(withId(R.id.detailitem_keyinput)).perform(clearText(), typeText(detKey2));
        Thread.sleep(25);
        onData(anything()).atPosition(1).onChildView(withId(R.id.detailitem_valueinput)).perform(clearText(), typeText(detValue2));
    }

    @Test
    public void createFileEditNewEntryAndSave() throws Exception
    {
        createFileEditNewEntry();

        //back out
        activityRule.getActivity().resetBackCalledCount();
        while (activityRule.getActivity().getBackCalledCount() == 0)
        {
            pressBack();
            Thread.sleep(50);
        }

        onView(withText(R.string.confirm_okay)).perform(click());

        //document view
        Fragment frag = ((MainActivity) mainState.activity).getCurrentFagment();
        assertTrue(frag instanceof ViewFileFragment);
        onData(is(instanceOf(PasswordDetails.class))).atPosition(0).check(matches(hasDescendant(withText(name))));

        boolean thrown = false;

        try
        {
            onData(is(instanceOf(PasswordDetails.class))).atPosition(1).check(doesNotExist());
        }
        catch(Exception e){
            thrown = true;
        }

        assertTrue(thrown);
    }

    @Test
    public void createFileEditNewEntryAndDiscard() throws Exception
    {
        createFileEditNewEntry();

        //back out
        activityRule.getActivity().resetBackCalledCount();
        while (activityRule.getActivity().getBackCalledCount() == 0)
        {
            pressBack();
            Thread.sleep(50);
        }

        onView(withText(R.string.confirm_discard)).perform(click());

        //document view
        Fragment frag = activityRule.getActivity().getCurrentFagment();
        assertTrue(frag instanceof ViewFileFragment);
        onData(is(instanceOf(PasswordDetails.class))).atPosition(0).check(matches(hasDescendant(withText(R.string.new_entry_title))));

        boolean thrown = false;

        try
        {
            onData(is(instanceOf(PasswordDetails.class))).atPosition(1).check(doesNotExist());
        }
        catch(Exception e){
            thrown = true;
        }

        assertTrue(thrown);
    }

    @Test
    public void createFileBackOut() throws Exception
    {
        //password view
        activityRule.getActivity().superInit();

        onView(withId(R.id.createfile_password1ipt)).perform(clearText()).perform(typeText("pass"));
        onView(withId(R.id.createfile_password2ipt)).perform(clearText()).perform(typeText("pass"));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        //document view
        TestingMainActivity activity = (TestingMainActivity) MainState.getInstance().activity;
        Fragment frag = activity.getCurrentFagment();
        assertTrue(frag instanceof ViewFileFragment);

        //back out
        activityRule.getActivity().resetBackCalledCount();
        while (activityRule.getActivity().getBackCalledCount() == 0)
        {
            pressBack();
            Thread.sleep(50);
        }

        assertFalse(activityRule.getActivity().getDoexitCalled());
    }

    @Test
    public void openFileEditExistingEntryCreateNewEntry()
    {

    }

    @Test
    public void openFileSearchEntry()
    {

    }

    @Test
    public void openFileEditEntrySaveFail()
    {

    }

    @Test
    public void timeout()
    {

    }

    @Test
    public void settingsBackOut()
    {

    }

    @Test
    public void changePassword()
    {

    }

    @Test
    public void remoteSync()
    {

    }

    @Test
    public void remoteSyncWarn()
    {

    }

    @Test
    public void remoteSyncDisable()
    {

    }

    @Test
    public void importFile()
    {

    }

    @Test
    public void about()
    {

    }

    @Test
    public void deleteLocal()
    {

    }
}
