package com.munger.passwordkeeper;

import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

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
                return matcher.matches(view) && currentIndex++ == index;
            }
        };
    }
}
