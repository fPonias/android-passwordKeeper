package com.munger.passwordkeeper.view;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

    /**
     * Storage Access Framework launcher for picking a file to import.
     * MUST be registered during onCreate (before the fragment is started).
     * Uses the system file picker, which works on all Android versions and
     * needs no runtime permissions.
     */
    private ActivityResultLauncher<String[]> importPickerLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        importPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null)
                        return;     // user cancelled the picker
                    handleImportUri(uri);
                });

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
        // Launch the system file picker. "*/*" lets the user pick anything; if the
        // password export files have a known MIME type or extension, this can be
        // narrowed (e.g. new String[]{"text/plain"} or {"application/octet-stream"}).
        importPickerLauncher.launch(new String[]{"*/*"});
    }

    /**
     * Called from the SAF picker callback. Copies the chosen content URI into a
     * cache file so the existing path-based import API keeps working unchanged.
     */
    private void handleImportUri(Uri uri)
    {
        Log.d(getClass().getName(), "selected uri " + uri.toString());

        File tempFile;
        try
        {
            tempFile = copyUriToCache(uri);
        }
        catch (IOException e)
        {
            Log.e(getClass().getName(), "failed to copy import uri to cache", e);
            MainState.getInstance().navigationHelper.showAlert("Could not read the selected file.");
            return;
        }

        MainState.getInstance().navigationHelper.importFile(tempFile.getPath(), new NavigationHelper.Callback() {public void callback(Object result)
        {
            boolean success = (boolean) result;
            // best-effort cleanup of the cache copy
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();

            if (!success)
                return;

            try
            {
                MainState.getInstance().document.save();
            }
            catch (Exception e) {
                MainState.getInstance().navigationHelper.showAlert("Failed to import external data.");
                success = false;
            }

            if (success)
                MainState.getInstance().navigationHelper.onBackPressed(null);
        }});
    }

    /**
     * Copies the contents of a content:// URI into a temp file in the app's
     * cache directory. The existing import code path expects a file path, not
     * a URI, so this bridges the two worlds.
     */
    private File copyUriToCache(Uri uri) throws IOException
    {
        File cacheDir = requireContext().getCacheDir();
        File out = File.createTempFile("import_", ".tmp", cacheDir);

        try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(out))
        {
            if (in == null)
                throw new IOException("ContentResolver returned null InputStream for " + uri);

            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0)
                os.write(buf, 0, n);
        }
        return out;
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
