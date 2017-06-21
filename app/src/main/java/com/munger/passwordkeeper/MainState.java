package com.munger.passwordkeeper;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.munger.passwordkeeper.alert.AlertFragment;
import com.munger.passwordkeeper.helpers.DriveHelper;
import com.munger.passwordkeeper.helpers.KeyboardListener;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.helpers.QuitTimer;
import com.munger.passwordkeeper.struct.Config;
import com.munger.passwordkeeper.struct.ConfigFactory;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.Settings;
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
        if (instance == null)
            instance = new MainState();


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
    public Settings settings;

    public DriveHelper driveHelper;
    public NavigationHelper navigationHelper;
    public KeyboardListener keyboardListener;
    public QuitTimer quitTimer;

    public Handler handler;
    public boolean isActive = false;

    public void setContext(Context context, FragmentActivity activity)
    {
        this.activity = activity;
        this.context = context;
        handler = new Handler(context.getMainLooper());

        keyboardListener = new KeyboardListener(activity);

        setupNavigation();
        setupPreferences();
        setupConfig();
        setupDocument();
        setupQuitTimer();

        setupDriveHelper();
    }

    protected void setupNavigation()
    {
        navigationHelper = new NavigationHelper();
    }

    protected void setupPreferences()
    {
        settings = new Settings();
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

    public void setupDocument()
    {
        document = createDocument();
    }

    protected PasswordDocument createDocument()
    {
        PasswordDocument doc = new PasswordDocumentFile(config.localDataFilePath);
        return doc;
    }

    public PasswordDocument createTmpDocument()
    {
        PasswordDocument doc = new PasswordDocumentFile(config.localDataFilePath + "-tmp");
        return doc;
    }

    public void cleanUp()
    {
        try
        {
            document.close();
        }
        catch(Exception e){}
    }

    public void setupQuitTimer()
    {
        quitTimer = new QuitTimer();

    }

    public MainState()
    {
    }

    public void deleteData()
    {
        try
        {
            MainState.getInstance().document.delete();
        }
        catch(Exception e){
            navigationHelper.showAlert("Failed to delete local password data");
            return;
        }
    }

    public void deleteRemoteData()
    {

    }

    private PasswordDocumentDrive driveDocument;

    private Object setupLock = new Object();
    private boolean settingUp = false;

    public void setupDriveHelper()
    {
        synchronized (setupLock)
        {
            if (settingUp)
                return;

            settingUp = true;
        }

        Thread t = new Thread(new Runnable() {public void run()
        {
            if (driveHelper == null)
                driveHelper = new DriveHelper();

            boolean enable = settings.getSaveToCloud();
            if (enable && driveDocument == null)
            {
                driveHelper.connect();
                driveHelper.awaitConnection();

                if (!driveHelper.isConnected())
                {
                    driveHelper.cleanUp();
                    driveDocument = null;
                }
                else
                {
                    driveDocument = new PasswordDocumentDrive(document);
                    driveDocument.init();
                }
            }
            else if (!enable && driveDocument != null)
            {
                driveHelper.cleanUp();
                driveDocument = null;
            }

            synchronized (setupLock)
            {
                settingUp = false;
            }
        }});
        t.start();
    }
}
