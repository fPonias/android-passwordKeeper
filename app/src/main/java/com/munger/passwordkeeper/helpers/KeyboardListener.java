package com.munger.passwordkeeper.helpers;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;

import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.R;

import java.util.ArrayList;

/**
 * Created by codymunger on 10/18/16.
 */

public class KeyboardListener
{
    private Activity parent;
    private boolean isOpen = false;

    public KeyboardListener(final Activity parent)
    {
        this.parent = parent;

        keyboardChangedListeners = new ArrayList<>();

        final View activityRootView = parent.findViewById(R.id.container);
        if (activityRootView == null)
            return;

        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                int heightDiff = activityRootView.getRootView().getHeight() - activityRootView.getHeight();
                if (heightDiff > dpToPx(200))
                {
                    if (isOpen)
                        return;

                    isOpen = true;
                    int sz = keyboardChangedListeners.size();
                    for (int i = sz - 1; i >= 0; i--)
                        keyboardChangedListeners.get(i).OnKeyboardOpened();
                }
                else
                {
                    if (!isOpen)
                        return;

                    isOpen = false;
                    int sz = keyboardChangedListeners.size();
                    for (int i = sz - 1; i >= 0; i--)
                        keyboardChangedListeners.get(i).OnKeyboardClosed();
                }
            }
        });
    }

    public float dpToPx(float valueInDp)
    {
        DisplayMetrics metrics = parent.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, valueInDp, metrics);
    }

    public interface OnKeyboardChangedListener
    {
        void OnKeyboardOpened();
        void OnKeyboardClosed();
    }

    private ArrayList<OnKeyboardChangedListener> keyboardChangedListeners;

    public void addKeyboardChangedListener(OnKeyboardChangedListener listener)
    {
        if (!keyboardChangedListeners.contains(listener))
            keyboardChangedListeners.add(listener);
    }

    public void removeKeyboardChangedListener(OnKeyboardChangedListener listener)
    {
        if (keyboardChangedListeners.contains(listener))
            keyboardChangedListeners.remove(listener);
    }

    public void forceOpenKeyboard(boolean open)
    {
        InputMethodManager imm = (InputMethodManager) parent.getSystemService(Context.INPUT_METHOD_SERVICE);

        if (open)
        {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
        else
        {
            View focus = parent.getCurrentFocus();

            if (focus == null)
                return;

            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }
}
