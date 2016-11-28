package com.munger.passwordkeeper;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.munger.passwordkeeper.alert.AlertFragment;
import com.munger.passwordkeeper.helpers.DriveHelper;
import com.munger.passwordkeeper.helpers.KeyboardListener;
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

import java.util.ArrayList;

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

    public Context context;
    public FragmentActivity activity;

    public PasswordDocument document;
    public PasswordDetails details;
    public String password;

    private boolean editable = false;

    public Config config;
    public SharedPreferences preferences;

    public DriveHelper driveHelper;
    public NavigationHelper navigationHelper;
    public KeyboardListener keyboardListener;

    public Handler handler;
    public boolean isActive = false;

    public void setContext(Context context, FragmentActivity activity)
    {
        this.activity = activity;
        this.context = context;
        handler = new Handler(Looper.getMainLooper());

        keyboardListener = new KeyboardListener(activity);

        setupNavigation();
        setupPreferences();
        setupConfig();
        setupDocument();

        setupDriveHelper();
    }

    protected void setupNavigation()
    {
        navigationHelper = new NavigationHelper();
    }

    protected void setupPreferences()
    {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        preferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            if (key.equals(SettingsFragment.PREF_NAME_SAVE_TO_CLOUD))
            {
                MainState.getInstance().setupDriveHelper();
            }
        }});
    }

    protected void setupConfig()
    {
        try
        {
            config = new ConfigFactory().load();
        }
        catch(Exception e){
            throw new RuntimeException("system config not present");
        }
    }

    protected void setupDocument()
    {
        document = new PasswordDocumentFile(config.localDataFilePath);
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
}
