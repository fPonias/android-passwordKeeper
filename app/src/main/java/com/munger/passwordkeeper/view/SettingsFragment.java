package com.munger.passwordkeeper.view;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.alert.FileDialog;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;

import java.io.File;

public class SettingsFragment extends PreferenceFragmentCompat
{
    private View root;
    CheckBoxPreference saveToCloudBox;
    ListPreference timeoutList;
    Preference changePasswordBtn;
    Preference importFileBtn;
    Preference exportFileBtn;
    Preference deleteBtn;
    Preference aboutBtn;
    Preference testBtn;

    public static final String PREF_NAME_SAVE_TO_CLOUD = "settings_saveToCloud";
    public static final String PREF_NAME_TIMEOUT_LIST = "settings_timeout";
    public static final String PREF_CHANGE_PASSWORD = "settings_changePassword";
    public static final String PREF_IMPORT_FILE = "settings_importFile";
    public static final String PREF_EXPORT_FILE = "settings_exportFile";
    public static final String PREF_DELETE_FILE = "settings_deleteFile";
    public static final String PREF_ABOUT = "settings_about";
    public static final String PREF_TEST = "settings_test";

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
        exportFileBtn = findPreference(PREF_EXPORT_FILE);
        deleteBtn = findPreference(PREF_DELETE_FILE);
        aboutBtn = findPreference(PREF_ABOUT);
        testBtn = findPreference(PREF_TEST);

        importFileBtn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {public boolean onPreferenceClick(Preference preference)
        {
            doImport();
            return false;
        }});

        exportFileBtn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
            doExport();
            return false;
            }
        });

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

        testBtn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                runTest();
                return false;
            }
        });

        testBtn.setVisible(false);

        loadSettings();
    }

    protected File getDefaultDirectory()
    {
        File mPath = new File(Environment.getExternalStorageDirectory() + "//DIR//");
        return mPath;
    }

    private boolean isExporting = false;
    private final Object exportLock = new Object();

    private void doExport()
    {
        synchronized (exportLock)
        {
            if (isExporting)
                return;

            isExporting = true;
        }

        final ProgressDialog loadingDialog = new ProgressDialog(MainState.getInstance().context);
        loadingDialog.setMessage("Backing up data");
        loadingDialog.show();

        Thread t = new Thread(new Runnable() {public void run() {
            final File result = MainState.getInstance().navigationHelper.exportFile("backup");

            MainState.getInstance().activity.runOnUiThread(new Runnable() {public void run()
            {
                loadingDialog.dismiss();
                String message = (result == null) ? "Failed to save backup data." : "Saved backup to: " + result.getPath();
                MainState.getInstance().navigationHelper.showAlert(message);
            }});

            synchronized (exportLock)
            {
                isExporting = false;
            }
        }});
        t.start();
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

    private void runTest()
    {
        try
        {
            MainState.getInstance().driveDocument.remoteUpdate(true);
        }
        catch (PasswordDocument.IncorrectPasswordException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
