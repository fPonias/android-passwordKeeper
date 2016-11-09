package com.munger.passwordkeeper.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import com.munger.passwordkeeper.alert.AlertFragment;
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
    Preference deleteBtn;
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
        deleteBtn = findPreference("settings_deleteFile");
        aboutBtn = findPreference("settings_about");

        importFileBtn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {public boolean onPreferenceClick(Preference preference)
        {
            doImport();
            return false;
        }});

        deleteBtn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {public boolean onPreferenceClick(Preference preference)
        {
            doDelete();
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
            boolean success = MainActivity.getInstance().importFile(file.getPath());

            if (success)
                MainActivity.getInstance().onBackPressed();
        }});

        fileDialog.showDialog();
    }

    private void doDelete()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Are you sure you want to delete all of your password data?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which)
        {
            MainActivity.getInstance().deleteData();
            MainActivity.getInstance().deleteRemoteData();
        }});
        builder.setNeutralButton("No", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which)
        {
        }});

        AlertDialog dialog = builder.create();
        dialog.show();

        return;
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
