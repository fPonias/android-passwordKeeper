package com.munger.passwordkeeper.view;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.alert.FileDialog;
import com.munger.passwordkeeper.struct.PasswordDetails;

import java.io.File;

public class SettingsFragment extends PreferenceFragmentCompat
{
    private View root;
    CheckBoxPreference saveToCloudBox;
    ListPreference timeoutList;
    Preference changePasswordBtn;
    Preference importFileBtn;
    Preference aboutBtn;

    public static String getName()
    {
        return "Settings";
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null)
        {
            //MainActivity.getInstance().fragmentExists(this);
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.fragment_settings, rootKey);

        saveToCloudBox = (CheckBoxPreference) findPreference("settings_saveToCloud");
        timeoutList = (ListPreference) findPreference("settings_timeout");
        changePasswordBtn = findPreference("settings_changePassword");
        importFileBtn = findPreference("settings_importFile");
        aboutBtn = findPreference("settings_about");

        importFileBtn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {public boolean onPreferenceClick(Preference preference)
        {
            doImport();
            return false;
        }});

        aboutBtn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {public boolean onPreferenceClick(Preference preference)
        {
            MainActivity.getInstance().about();
            return false;
        }});

        changePasswordBtn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {public boolean onPreferenceClick(Preference preference)
        {
            doPasswordChange();
            return false;
        }});

        saveToCloudBox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {public boolean onPreferenceChange(Preference preference, Object newValue)
        {
            boolean selected = (Boolean) newValue;

            if (selected)
                doDriveSync();
            else
                doDriveCleanup();

            return false;
        }});

        loadSettings();
    }

    private void doImport()
    {
        File mPath = new File(Environment.getExternalStorageDirectory() + "//DIR//");
        FileDialog fileDialog = new FileDialog(MainActivity.getInstance(), mPath);

        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {public void fileSelected(File file)
        {
            Log.d(getClass().getName(), "selected file " + file.toString());
            MainActivity.getInstance().importFile(file.getPath());
        }});

        fileDialog.showDialog();
    }

    private void doPasswordChange()
    {

    }

    private void doDriveSync()
    {

    }

    private void doDriveCleanup()
    {

    }

    private void loadSettings()
    {
        if (MainActivity.getInstance().config.enableImportOption == true)
            importFileBtn.setVisible(true);
        else
            importFileBtn.setVisible(false);
    }
}
