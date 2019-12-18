package com.munger.passwordkeeper;

import android.content.Context;
import android.os.Handler;

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

import androidx.fragment.app.FragmentActivity;

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
        setupDriveHelper();
        setupDocument();
        setupQuitTimer();
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
        if (config != null)
            return;

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
        if (document == null)
            document = createDocument();

        try
        {
            boolean enable = settings.getSaveToCloud();
            if (enable)
                setupDriveDocument();
        }
        catch(Exception e){
        }
    }

    protected PasswordDocument createDocument()
    {
        PasswordDocument doc = new PasswordDocumentFile(config.localDataFilePath);

        if (config.localDataFilePath.startsWith("./"))
            ((PasswordDocumentFile) doc).setRootPath(NavigationHelper.getRootPath());

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
        if (quitTimer == null)
            quitTimer = new QuitTimer();
        else
            quitTimer.reset();
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

    public PasswordDocumentDrive driveDocument;

    protected void  setupDriveDocument() throws Exception
    {
        if (driveHelper.isConnected() == null)
            driveHelper.awaitConnection();

        if (driveDocument != null)
            return;

        driveDocument = new PasswordDocumentDrive(document, MainState.getInstance().config.remoteDataFilePath);
    }

    private Object setupLock = new Object();
    private boolean settingUp = false;

    protected DriveHelper createDriveHelper()
    {
        return new DriveHelper();
    }

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
                driveHelper = createDriveHelper();

            boolean enable = settings.getSaveToCloud();
            if (enable && driveDocument == null && document != null)
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
                    try
                    {
                        setupDriveDocument();
                    }
                    catch(Exception e){
                        driveDocument = null;
                        driveHelper.cleanUp();
                    }

                    if (driveDocument != null)
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

    public void cleanUpDriveHelper()
    {
        if (driveHelper == null || driveDocument == null)
            return;

        driveHelper.cleanUp();
        driveDocument.cleanUp();
        driveDocument = null;
    }
}
