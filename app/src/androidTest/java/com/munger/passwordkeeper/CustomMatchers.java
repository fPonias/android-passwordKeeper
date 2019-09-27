package com.munger.passwordkeeper;

import androidx.test.espresso.ViewInteraction;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
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
}
