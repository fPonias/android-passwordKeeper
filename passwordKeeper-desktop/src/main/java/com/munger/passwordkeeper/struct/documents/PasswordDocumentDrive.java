/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper.struct.documents;

import com.google.api.client.http.FileContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.struct.AES256;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentStream;

import com.munger.passwordkeeper.Main;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by codymunger on 11/18/16.
 */

public class PasswordDocumentDrive
{
    public static final String version = "0.1";

    protected DriveHelper.DriveFileStruct file;
    private PasswordDocument sourceDoc;
    private PasswordDocumentStream remoteDoc;
    private String name;

    public PasswordDocumentDrive(String name)
    {
        super();
        this.name = name;
    }

    public PasswordDocumentDrive(PasswordDocument source, String name)
    {
        this(name);

        setSourceDoc(source);
    }

    public PasswordDocumentDrive(PasswordDocument source, String name, String password)
    {
        this(source, name);
    }

    public void setSourceDoc(PasswordDocument source)
    {
        sourceDoc = source;
        lastUpdate = Main.instance.mainState.prefs.getLastCloudUpdate();

        sourceDoc.addListener(fileListener);
    }

    public void cleanUp()
    {
        if (sourceDoc == null)
            return;

        sourceDoc.removeListener(fileListener);
        sourceDoc = null;
    }

    protected PasswordDocument.DocumentEvents fileListener = new PasswordDocument.DocumentEvents()
    {
        @Override
        public void initFailed(Exception e)
        {

        }

        @Override
        public void initted()
        {

        }

        @Override
        public void saved()
        {
            System.out.println("drive document local saved");

            Thread t = new Thread(new Runnable() {public void run()
            {
                System.out.println("drive document local saved thread started");
                if (!connectionHandled)
                {
                    handleConnected();
                    return;
                }

                try
                {
                    if (!isOverwriting)
                        remoteUpdate();
                    else
                        overwrite();
                }
                catch(Exception e){
                    System.out.println("failed to update remote data");
                }
            }}, "drive updater");
            t.start();
        }

        @Override
        public void loaded()
        {
            Thread t = new Thread(new Runnable() {public void run()
            {
                Boolean connected = Main.instance.mainState.driveHelper.isConnected();
                if (connected == null)
                {
                    Main.instance.mainState.driveHelper.awaitConnection();
                    connected = Main.instance.mainState.driveHelper.isConnected();
                }

                if (connected == false)
                    return;

                handleConnected();
            }}, "drive updater");
            t.start();
        }

        @Override
        public void deleted()
        {

        }

        @Override
        public void closed()
        {

        }
    };

    protected Exception initException = null;

    public void notifyInitError(Exception e)
    {
        initException = e;
    }

    public Exception getInitException()
    {
        return initException;
    }

    public void init()
    {
        Main.instance.mainState.driveHelper.awaitConnection();
        System.out.println("connected to google");

        if (!Main.instance.mainState.driveHelper.isConnected())
        {
            System.out.println("google drive init failed");
            notifyInitError(new Exception("google drive init failed"));
            return;
        }

        if (sourceDoc.isHistoryLoaded())
            handleConnected();
    }

    protected boolean connectionHandled = false;

    private void handleConnected()
    {
        System.out.println("checking for remote document");

        try
        {
            file = Main.instance.mainState.driveHelper.getOrCreateFile(name);
        }
        catch (IOException e) {
            file = null;
        }

        if (file == null)
        {
            System.out.println("Failed to get file id");
            notifyInitError(new Exception("failed to get file id"));
            return;
        }

        connectionHandled = true;

        try
        {
            remoteUpdate(true);
        }
        catch(Exception e){
            System.out.println("Initial remote update failed with " + e.getMessage());
            notifyInitError(e);
            return;
        }
    }

    private DateTime lastUpdate = null;
    private boolean isUpdating = false;
    private final Object updateLock = new Object();

    public boolean getIsUpdating()
    {
        return isUpdating;
    }

    public void remoteUpdate() throws Exception
    {
        remoteUpdate(false);
    }

    public static class TrashedFileException extends Exception
    {

    }

    public void remoteUpdate(boolean force) throws PasswordDocument.IncorrectPasswordException, Exception
    {
        synchronized (updateLock)
        {
            if (isUpdating)
                return;

            isUpdating = true;
        }

        System.out.println("document sync started");
        try
        {
            DriveHelper.Meta meta = Main.instance.mainState.driveHelper.getMetadata(fileId);
            System.out.println("meta.modified " + meta.modified + " lastUpdate " + lastUpdate);

            if (meta.trashed)
            {
                System.out.println("remote file trashed");

                synchronized (updateLock) {isUpdating = false;}
                throw new Exception("remote file trashed");
            }

            if ((lastUpdate == null || meta.modified.getValue() > lastUpdate.getValue() || force) && meta.size > 0)
            {
                System.out.println("remote password data was updated.  syncing");
                System.out.println("remote password data was updated.  syncing");
                InputStream ins = Main.instance.mainState.driveHelper.getRemoteFile(fileId);
                DataInputStream dais = new DataInputStream(ins);

                remoteDoc = new PasswordDocumentStream(dais, meta.size);
                remoteDoc.encoder = sourceDoc.encoder;
                remoteDoc.onLoad(true);

                ins.close();

                if (!remoteDoc.history.equals(sourceDoc.history))
                {
                    System.out.println("remote password data doesn't match");
                    System.out.println("remote password data doesn't match");

                    PasswordDocumentHistory tmpHist = sourceDoc.history.mergeHistory(remoteDoc.history);
                    sourceDoc.delete();
                    sourceDoc.playSubHistory(tmpHist);
                    sourceDoc.save();
                }
                else
                {
                    System.out.println("remote password data matches, doing nothing");
                }

                lastUpdate = meta.modified;
                Main.instance.mainState.settings.setLastCloudUpdate(lastUpdate);
            }
        }
        catch(PasswordDocument.IncorrectPasswordException e)
        {
            System.out.println("mismatched remote password");
            synchronized (updateLock) {isUpdating = false;}
            throw new PasswordDocument.IncorrectPasswordException();
        }
        catch (Exception e1){
            System.out.println("Failed to read remote data");

            if (e1.getMessage().contains("trashed"))
            {
                synchronized (updateLock) {isUpdating = false;}
                throw new TrashedFileException();
            }
            else if (force == false)
            {
                synchronized (updateLock) {isUpdating = false;}
                throw new Exception("failed to update local file");
            }
        }

        if (force == false && remoteDoc == null)
        {
            System.out.println("remote password data is null, exiting");
            synchronized (updateLock) {isUpdating = false;}
            return;
        }

        if ((force == true && (remoteDoc == null || !sourceDoc.history.equals(remoteDoc.history))) || (remoteDoc != null && !sourceDoc.history.equals(remoteDoc.history)))
        {
            System.out.println("local password data was updated.  syncing");
            overwrite();
        }

        synchronized (updateLock) {isUpdating = false;}
    }

    public void overwrite() throws Exception
    {
        if (isOverwriting)
            return;

        isOverwriting = true;

        try
        {
            String path = ((PasswordDocumentFile) sourceDoc).getHistoryFilePath();
            lastUpdate = Main.instance.mainState.driveHelper.updateRemoteByPath(fileId, path);
        }
        catch (Exception e){
            System.out.println("Failed to write remote data");
            synchronized (updateLock) {isUpdating = false;}
            isOverwriting = false;
            throw new Exception(("failed to update remote file"));
        }

        isOverwriting = false;
    }

    protected boolean isOverwriting = false;

    public void setPassword(String password) throws Exception
    {
        sourceDoc.setPassword(password);
        isOverwriting = true;
        sourceDoc.save();
    }

    public void delete() throws Exception
    {
        DriveHelper helper = Main.instance.mainState.driveHelper;
        if (!helper.isConnected())
            return;

        helper.trashRemoteFile(fileId, true);
        sourceDoc.removeListener(fileListener);
        sourceDoc = null;
    }

    public void undelete() throws Exception
    {
        DriveHelper helper = Main.instance.mainState.driveHelper;
        if (!helper.isConnected())
            return;

        helper.trashRemoteFile(fileId, false);
        handleConnected();
    }

    public PasswordDocument getSource()
    {
        return sourceDoc;
    }
}
