package com.munger.passwordkeeper.struct;

import android.content.SharedPreferences;

import com.google.api.client.util.DateTime;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.view.SettingsFragment;

import java.util.UUID;

import androidx.preference.PreferenceManager;

/**
 * Created by codymunger on 12/5/16.
 */

public class Settings
{
    private SharedPreferences preferences;

    public Settings()
    {
        setPreferences();
    }

    public void setPreferences()
    {
        preferences = PreferenceManager.getDefaultSharedPreferences(MainState.getInstance().activity);
    }

    public boolean getSaveToCloud()
    {
        boolean enable = preferences.getBoolean(SettingsFragment.PREF_NAME_SAVE_TO_CLOUD, false);
        return enable;
    }

    public void setSaveToCloud(boolean value)
    {
        preferences.edit().putBoolean(SettingsFragment.PREF_NAME_SAVE_TO_CLOUD, value).apply();
    }

    public float getTimeout()
    {
        String valueStr = preferences.getString(SettingsFragment.PREF_NAME_TIMEOUT_LIST, "5");

        if (valueStr == null)
            return 5;

        return Float.parseFloat(valueStr);
    }

    public void setTimeout(float value)
    {
        preferences.edit().putFloat(SettingsFragment.PREF_NAME_TIMEOUT_LIST, value).apply();
    }

    public String getDeviceUID()
    {
        if (!preferences.contains("UUID"))
        {
            UUID newid = UUID.randomUUID();
            preferences.edit().putString("UUID", newid.toString()).apply();
        }

        String ret = preferences.getString("UUID", null);
        if(ret == null)
            throw new RuntimeException("unable to create unique program id");

        return ret;
    }

    public DateTime getLastCloudUpdate()
    {
        long val = preferences.getLong("lastCloudUpdate", 0);
        DateTime ret = new DateTime(val);
        return ret;
    }

    public void setLastCloudUpdate(DateTime value)
    {
        long val2 = value.getValue();
        preferences.edit().putLong("lastCloudUpdate", val2).apply();
    }
}
