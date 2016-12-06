package com.munger.passwordkeeper.struct;

import android.content.SharedPreferences;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceManager;

import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.view.SettingsFragment;

/**
 * Created by codymunger on 12/5/16.
 */

public class Settings
{
    private SharedPreferences preferences;

    public Settings()
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainState.getInstance().activity);
    }

    public boolean getSaveToCloud()
    {
        boolean enable = preferences.getBoolean(SettingsFragment.PREF_NAME_SAVE_TO_CLOUD, false);
        return enable;
    }

    public float getTimeout()
    {
        String valueStr = preferences.getString(SettingsFragment.PREF_NAME_TIMEOUT_LIST, "5");
        return Float.parseFloat(valueStr);
    }

    public long getLastRemoteUpdate()
    {
        return preferences.getLong("lastRemoteUpdate", 0);
    }
}
