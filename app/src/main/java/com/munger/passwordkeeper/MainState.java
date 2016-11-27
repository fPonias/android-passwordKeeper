package com.munger.passwordkeeper;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.FragmentActivity;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.munger.passwordkeeper.alert.AlertFragment;
import com.munger.passwordkeeper.helpers.DriveHelper;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.struct.Config;
import com.munger.passwordkeeper.struct.ConfigFactory;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentDrive;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFileImport;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;
import com.munger.passwordkeeper.view.SettingsFragment;

/**
 * Created by codymunger on 11/25/16.
 */

public class MainState
{
    private volatile static MainState instance = null;

    public static MainState getInstance()
    {
        return instance;
    }
    public static void setInstance(MainState inst)
    {
        instance = inst;
    }

    public FragmentActivity mainActivity;

    public PasswordDocument document;
    private PasswordDetails details;
    public String password;

    private boolean editable = false;

    public Config config;
    public SharedPreferences preferences;

    public DriveHelper driveHelper;

    public NavigationHelper navigationHelper;

    public Handler handler;
    public boolean isActive = false;

    public void setMainActivity(FragmentActivity activity)
    {
        mainActivity = activity;
        handler = new Handler(Looper.getMainLooper());
        preferences = PreferenceManager.getDefaultSharedPreferences(MainState.getInstance().mainActivity);


        preferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            if (key.equals(SettingsFragment.PREF_NAME_SAVE_TO_CLOUD))
            {
                MainState.getInstance().setupDriveHelper();
            }
        }});

        setupDriveHelper();
        navigationHelper = new NavigationHelper();
    }

    public void cleanUp()
    {
        try
        {
            document.close();
        }
        catch(Exception e){}
    }

    public MainState()
    {
        try
        {
            config = new ConfigFactory().load();
        }
        catch(Exception e){
            throw new RuntimeException("system config not present");
        }

        document = new PasswordDocumentFile(config.localDataFilePath);
    }
    private PasswordDocumentDrive driveDocument;

    public void setupDriveHelper()
    {
        if (driveHelper == null)
            driveHelper = new DriveHelper();

        boolean enable = preferences.getBoolean(SettingsFragment.PREF_NAME_SAVE_TO_CLOUD, false);
        if (enable && driveDocument == null)
        {
            driveHelper.connect();
            driveDocument = new PasswordDocumentDrive(document);
        }
        else if (!enable && driveDocument != null)
        {
            driveHelper.cleanUp();
            driveDocument = null;
        }
    }

    public void openFile()
    {
        final ProgressDialog loadingDialog = new ProgressDialog(mainActivity);
        loadingDialog.setMessage("Decrypting password data");
        loadingDialog.show();

        final PasswordDocument.ILoadEvents listener = new PasswordDocumentFile.ILoadEvents() {
            @Override
            public void detailsLoaded()
            {
                handler.post(new Runnable() {public void run()
                {
                    if (loadingDialog.isShowing())
                        loadingDialog.dismiss();

                    openFile2();
                }});
            }

            @Override
            public void historyLoaded()
            {
                MainState.getInstance().setupDriveHelper();
            }

            @Override
            public void historyProgress(float progress) {

            }
        };
        document.addLoadEvents(listener);

        AsyncTask t = new AsyncTask()
        {
            protected Object doInBackground(Object[] params)
            {
                try
                {
                    document.load(true);
                }
                catch(Exception e){
                    AlertFragment frag = new AlertFragment("Failed to open the document: " + document.name);
                    frag.show(mainActivity.getSupportFragmentManager(), "invalid_fragment");
                }

                return null;
            }

            protected void onPostExecute(Object o)
            {
                if (loadingDialog.isShowing())
                    loadingDialog.dismiss();

                document.removeLoadEvents(listener);
            }
        };

        t.execute(new Object[]{});
    }

    private void openFile2()
    {
        if (MainState.getInstance().document.count() == 0)
        {
            PasswordDetails dets = new PasswordDetails();
            try{MainState.getInstance().document.addDetails(dets);}catch(Exception e){}
        }

        navigationHelper.openFileView();
    }

    public void importFile(final String path, final Callback callback)
    {
        final ProgressDialog loadingDialog = new ProgressDialog(mainActivity);
        loadingDialog.setMessage("Importing password data");
        loadingDialog.show();

        AsyncTask t = new AsyncTask()
        {
            protected Object doInBackground(Object[] params)
            {
                try
                {
                    PasswordDocumentFileImport fileImport = new PasswordDocumentFileImport(path, "import");
                    fileImport.load(false);
                    document.playSubHistory(fileImport.getHistory());
                    document.save();
                }
                catch(Exception e){
                    AlertFragment frag = new AlertFragment("Failed to import the document: " + path);
                    frag.show(mainActivity.getSupportFragmentManager(), "invalid_fragment");
                    return false;
                }

                return true;
            }

            protected void onPostExecute(Object o)
            {
                loadingDialog.dismiss();

                AlertFragment frag = new AlertFragment("Successfully imported!");
                frag.show(mainActivity.getSupportFragmentManager(), "invalid_fragment");

                callback.callback(o);
            }
        };
        t.execute(new Object[]{});
    }

    public void deleteData()
    {
        try
        {
            document.delete();
        }
        catch(Exception e){
            AlertFragment frag = new AlertFragment("Failed to delete local password data");
            frag.show(mainActivity.getSupportFragmentManager(), "invalid_fragment");
            return;
        }

        setPasswordFile();
    }

    protected void setPasswordFile()
    {
        document = new PasswordDocumentFile(config.localDataFilePath);
        navigationHelper.openInitialView();
    }

    public void deleteRemoteData()
    {

    }

    public void setFile(String password)
    {
        this.password = password;
        document.setPassword(password);
        openFile();
    }

    public void removeFile()
    {
        try
        {
            document.delete();
        }
        catch(Exception e){
            AlertFragment frag = new AlertFragment("Failed to delete the document: " + document.name);
            frag.show(mainActivity.getSupportFragmentManager(), "invalid_fragment");
        }
    }

    public PasswordDetails getDetails()
    {
        return details;
    }

    public void setDetails(PasswordDetails dets)
    {
        details = dets;
    }

    public interface Callback
    {
        void callback(Object result);
    }

    public void saveDetail(final PasswordDetails detail, final Callback callback)
    {
        final ProgressDialog loadingDialog = new ProgressDialog(mainActivity);
        loadingDialog.setMessage("Saving password data");
        loadingDialog.show();

        AsyncTask t = new AsyncTask() {
            protected Object doInBackground(Object[] params)
            {
                try
                {
                    document.replaceDetails(detail);
                    document.save();
                    detail.setHistory(new PasswordDocumentHistory());
                }
                catch(Exception e){
                    Log.e("password", "failed to update password file");
                }

                return null;
            }

            @Override
            protected void onPostExecute(Object o)
            {
                loadingDialog.dismiss();
                callback.callback(null);
            }
        };
        t.execute(new Object(){});
    }

}
