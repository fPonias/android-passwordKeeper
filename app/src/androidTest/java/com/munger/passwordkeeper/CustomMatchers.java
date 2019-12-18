package com.munger.passwordkeeper;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.test.espresso.AmbiguousViewMatcherException;
import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewFinder;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.ViewMatchers;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

import com.munger.passwordkeeper.view.ViewFileFragment;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.action.ViewActions.*;

/**
 * Created by codymunger on 11/28/16.
 */

public class CustomMatchers
{
    public static Matcher<View> withIndex(final int index, final Matcher<View> matcher)
    {
        return new TypeSafeMatcher<View>()
        {
            int currentIndex = 0;

            @Override
            public void describeTo(Description description)
            {
                description.appendText("with index: ");
                description.appendValue(index);
                matcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view)
            {
                if (!matcher.matches(view))
                    return false;

                boolean match = (index == currentIndex);

                currentIndex++;
                return match;
            }
        };
    }

    public static void assertHiddenOrDoesNotExist(ViewInteraction match)
    {
        boolean thrown = false;
        try
        {
            match.check(matches(not(isDisplayed())));
            return;
        }
        catch(Exception e){
            thrown = true;
        }

        assertTrue(thrown);
    }

    public static void assertHiddenOrDoesNotExist(DataInteraction match)
    {
        boolean thrown = false;
        try
        {
            match.check(matches(not(isDisplayed())));
            return;
        }
        catch(Exception e){
            thrown = true;
        }

        assertTrue(thrown);
    }

    public static class DoesNotExistException extends Exception
    {}

    protected interface assertDoesExistAction
    {
        void perform();
    }

    protected static void assertDoesExist(assertDoesExistAction action) throws DoesNotExistException
    {
        int attempts = 0;
        int tries = 10;
        int pause = 100;
        boolean thrown = true;
        while (thrown && attempts < tries)
        {
            try
            {
                action.perform();
                thrown = false;
            }
            catch(Exception e){
                try { Thread.sleep(pause); } catch (InterruptedException e1) {}
            }

            attempts++;
        }

        if (attempts == tries)
        {
            throw new DoesNotExistException();
        }
    }

    public static void assertDoesExist(final DataInteraction match) throws DoesNotExistException
    {
        assertDoesExist(new assertDoesExistAction() {public void perform()
        {
            match.check(matches(isDisplayed()));
        }});
    }

    public static void assertDoesExist(final ViewInteraction match) throws DoesNotExistException
    {
        assertDoesExist(new assertDoesExistAction() {public void perform()
        {
            match.check(matches(isDisplayed()));
        }});
    }

    public static void assertInView(final MainActivity activity, final Class cls) throws DoesNotExistException, InterruptedException
    {
        assertDoesExist(new assertDoesExistAction() {public void perform()
        {
            Fragment frag = activity.getCurrentFagment();
            assertTrue(cls.isInstance(frag));
        }});

        Thread.sleep(300);
    }
}
