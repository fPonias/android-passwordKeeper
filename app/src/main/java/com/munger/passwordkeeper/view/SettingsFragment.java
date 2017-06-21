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
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.alert.AlertFragment;
import com.munger.passwordkeeper.alert.FileDialog;
import com.munger.passwordkeeper.helpers.NavigationHelper;
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

    public static final String PREF_NAME_SAVE_TO_CLOUD = "settings_saveToCloud";
    public static final String PREF_NAME_TIMEOUT_LIST = "settings_timeout";
    public static final String PREF_CHANGE_PASSWORD = "settings_changePassword";
    public static final String PREF_IMPORT_FILE = "settings_importFile";
    public static final String PREF_DELETE_FILE = "settings_deleteFile";
    public static final String PREF_ABOUT = "settings_about";

    public static final int PREFERENCES_RESOURCE = R.xml.fragment_settings;

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
            //MainState.getInstance().context.fragmentExists(this);
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        // Load the preferences from an XML resource
        setPreferencesFromResource(PREFERENCES_RESOURCE, rootKey);

        saveToCloudBox = (CheckBoxPreference) findPreference(PREF_NAME_SAVE_TO_CLOUD);
        timeoutList = (ListPreference) findPreference(PREF_NAME_TIMEOUT_LIST);
        changePasswordBtn = findPreference(PREF_CHANGE_PASSWORD);
        importFileBtn = findPreference(PREF_IMPORT_FILE);
        deleteBtn = findPreference(PREF_DELETE_FILE);
        aboutBtn = findPreference(PREF_ABOUT);

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
            MainState.getInstance().navigationHelper.about();
            return false;
        }});

        changePasswordBtn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {public boolean onPreferenceClick(Preference preference)
        {
            MainState.getInstance().navigationHelper.changePassword();
            return false;
        }});

        saveToCloudBox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {public boolean onPreferenceChange(Preference preference, Object newValue)
        {
            MainState.getInstance().setupDriveHelper();
            return true;
        }});

        timeoutList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {public boolean onPreferenceChange(Preference preference, Object newValue)
        {
            if (MainState.getInstance().quitTimer != null)
                MainState.getInstance().quitTimer.reset();
            return true;
        }});

        loadSettings();
    }

    protected File getDefaultDirectory()
    {
        File mPath = new File(Environment.getExternalStorageDirectory() + "//DIR//");
        return mPath;
    }

    private void doImport()
    {
        FileDialog fileDialog = new FileDialog(MainState.getInstance().activity, getDefaultDirectory());

        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {public void fileSelected(File file)
        {
            Log.d(getClass().getName(), "selected file " + file.toString());
            MainState.getInstance().navigationHelper.importFile(file.getPath(), new NavigationHelper.Callback() {public void callback(Object result)
            {
                boolean success = (boolean) result;

                if (!success)
                    return;

                try
                {
                    MainState.getInstance().document.save();
                }
                catch(Exception e){
                    MainState.getInstance().navigationHelper.showAlert("Failed to import external data.");
                    success = false;
                }

                if (success)
                    MainState.getInstance().navigationHelper.onBackPressed(null);
            }});
        }});

        fileDialog.showDialog();
    }

    private void doDelete()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("Are you sure you want to delete all of your password data?");
        builder.setPositiveButton(R.string.alert_positive, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which)
        {
            MainState.getInstance().deleteData();
            MainState.getInstance().deleteRemoteData();
            MainState.getInstance().navigationHelper.openInitialView();
        }});
        builder.setNeutralButton(R.string.alert_negative, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which)
        {
        }});

        AlertDialog dialog = builder.create();
        dialog.show();

        return;
    }

    private void loadSettings()
    {
        if (MainState.getInstance().config.enableImportOption == true)
            importFileBtn.setVisible(true);
        else
            importFileBtn.setVisible(false);
    }
}
