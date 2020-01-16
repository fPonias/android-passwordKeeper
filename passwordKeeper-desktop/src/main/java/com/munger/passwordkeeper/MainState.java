/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper;

import com.munger.passwordkeeper.ctrl.MenuCtrl;
import com.munger.passwordkeeper.drive.DriveHelper;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;
import java.util.prefs.BackingStoreException;

/**
 *
 * @author hallmarklabs
 */
public class MainState 
{
    public PasswordDocumentFile document;
    public QuitTimer quitTimer;
    public Prefs prefs;
    public DriveHelper driveHelper;
    
    public MenuCtrl menuCtrl;
    
    public MainState() throws BackingStoreException
    {
        prefs = new Prefs();
        prefs.addListener((Prefs.Types key) -> 
        {
            doUpdate(key);
        });
        
        try
        {
            driveHelper = new DriveHelper();
            driveHelper.init();
        }
        catch(Exception e){}
        
        openDoc();
    }
    
    private void doUpdate(Prefs.Types key)
    {
        if (key == Prefs.Types.timeout)
            updateTimer();
        else if (key == Prefs.Types.savePath)
            updateDoc();
    }
    
    private void updateTimer()
    {
        long timeout = prefs.getTimeout();
        if (timeout == -1)
            stopQuitTimer();
        else
        {
            if (quitTimer == null)
                startQuitTimer();

            quitTimer.setQuitCheckInterval(timeout);
            quitTimer.reset();
        }
    }
    
    private void updateDoc()
    {
        try
        {
            if (document != null)
                document.close();
            
            document = null;
            
            openDoc();
        }
        catch(Exception e){
            Main.instance.showAlert("failed to reopen password file at " + document.getRootPath() + document.name);
        }
    }
    
    public void openDoc()
    {
        String path = prefs.getSavePath();
        String[] parts = path.split("/");
        String name = parts[parts.length - 1];
        
        path = "";
        for (int i = 0; i < parts.length - 1; i++)
            path += parts[i] + "/";
        
        document = new PasswordDocumentFile(name);
        document.setRootPath(path);
    }
    
    public void setupDriveHelper()
    {}
    
    public void startQuitTimer()
    {
        quitTimer = new QuitTimer();
        quitTimer.setQuitListener(() -> 
        {
            Main.instance.closeDocument();
        });
    }
    
    public void resetQuitTimer()
    {
        if (quitTimer != null)
            quitTimer.reset();
    }
    
    public void stopQuitTimer()
    {
        if (quitTimer != null)
            quitTimer.stop();
    }
}
