package com.munger.passwordkeeper.struct.documents;

import android.util.Log;

import com.google.api.client.util.DateTime;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.helpers.DriveHelper;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by codymunger on 11/18/16.
 */

public class PasswordDocumentDrive
{
    public static final String version = "0.1";

    protected String fileId;
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
        lastUpdate = MainState.getInstance().settings.getLastCloudUpdate();

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
            Log.d("password", "drive document local saved");
            System.out.println("drive document local saved");

            Thread t = new Thread(new Runnable() {public void run()
            {
                Log.d("password", "drive document local saved thread started");
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
                    Log.e("password", "failed to update remote data");
                }
            }}, "drive updater");
            t.start();
        }

        @Override
        public void loaded()
        {
            Thread t = new Thread(new Runnable() {public void run()
            {
                Boolean connected = MainState.getInstance().driveHelper.isConnected();
                if (connected == null)
                {
                    MainState.getInstance().driveHelper.awaitConnection();
                    connected = MainState.getInstance().driveHelper.isConnected();
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

    public static class InitListener
    {
        public void success(){}
        public void error(Exception e){}
    }

    protected ArrayList<InitListener> listeners = new ArrayList<>();

    public void addListener(InitListener listener)
    {
        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public void removeListener(InitListener listener)
    {
        if (listeners.contains(listener))
            listeners.remove(listener);
    }

    protected Exception initException = null;

    public void notifyInitError(Exception e)
    {
        initException = e;

        for(InitListener listener : listeners)
        {
            listener.error(e);
        }
    }

    public void notifyInitSuccess()
    {
        for (InitListener listener : listeners)
        {
            listener.success();
        }
    }

    public Exception getInitException()
    {
        return initException;
    }

    public void init()
    {
        MainState.getInstance().driveHelper.awaitConnection();
        Log.d("password", "connected to google");

        if (!MainState.getInstance().driveHelper.isConnected())
        {
            Log.d("password", "google drive init failed");
            notifyInitError(new Exception("google drive init failed"));
            return;
        }

        if (sourceDoc.historyLoaded)
            handleConnected();
    }

    protected boolean connectionHandled = false;

    public static class RemoteNoFileException extends Exception {};

    private void handleConnected()
    {
        Log.d("password", "checking for remote document");
        System.out.println("checking for remote document");

        try
        {
            File histFile = new File(((PasswordDocumentFile) sourceDoc).getHistoryFilePath());
            if (!histFile.exists())
                fileId = MainState.getInstance().driveHelper.getRemoteFileId(name);
            else
                fileId = MainState.getInstance().driveHelper.getOrCreateFile(name);
        }
        catch (IOException e) {
            fileId = null;
        }

        if (fileId == null)
        {
            Log.e("password", "Failed to get file id");
            notifyInitError(new RemoteNoFileException());
            return;
        }

        connectionHandled = true;

        try
        {
            remoteUpdate(true);
        }
        catch(Exception e){
            Log.e("password", "Initial remote update failed with " + e.getMessage());
            notifyInitError(e);
            return;
        }

        notifyInitSuccess();
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

    public static class RemoteTrashedException extends Exception {};
    public static class RemoteFileEmptyException extends Exception {};
    public static class RemoteIOException extends Exception {};
    public static class UnplayableHistoryException extends Exception {};

    public void remoteUpdate(boolean force) throws PasswordDocument.IncorrectPasswordException, RemoteTrashedException, RemoteFileEmptyException, RemoteIOException, UnplayableHistoryException
    {
        synchronized (updateLock)
        {
            if (isUpdating)
                return;

            isUpdating = true;
        }

        Log.d("password", "document sync started");
        System.out.println("document sync started");
        try
        {
            DriveHelper.Meta meta = MainState.getInstance().driveHelper.getMetadata(fileId);
            System.out.println("meta.modified " + meta.modified + " lastUpdate " + lastUpdate);

            if (meta.trashed)
            {
                System.out.println("remote file trashed");

                synchronized (updateLock) {isUpdating = false;}
                throw new RemoteTrashedException();
            }

            if (meta.size == 0 && sourceDoc.history.count() == 0)
            {
                System.out.println("empty remote file");

                synchronized (updateLock) {isUpdating = false;}
                throw new RemoteFileEmptyException();
            }

            if ((lastUpdate == null || meta.modified.getValue() > lastUpdate.getValue() || force) && meta.size > 0)
            {
                Log.d("password", "remote password data was updated.  syncing");
                System.out.println("remote password data was updated.  syncing");
                InputStream ins = MainState.getInstance().driveHelper.getRemoteFile(fileId);
                DataInputStream dais = new DataInputStream(ins);

                remoteDoc = new PasswordDocumentStream(dais, meta.size);
                remoteDoc.encoder = sourceDoc.encoder;
                remoteDoc.onLoad(true);

                ins.close();

                if (!remoteDoc.history.equals(sourceDoc.history))
                {
                    Log.d("password", "remote password data doesn't match");
                    System.out.println("remote password data doesn't match");

                    PasswordDocumentHistory tmpHist = sourceDoc.history.mergeHistory(remoteDoc.history);
                    boolean success = verifyHistory(tmpHist);

                    if (!success)
                        throw new UnplayableHistoryException();

                    sourceDoc.delete();
                    sourceDoc.playSubHistory(tmpHist);
                    sourceDoc.save();
                }
                else
                {
                    Log.d("password", "remote password data matches, doing nothing");
                    System.out.println("remote password data matches, doing nothing");
                }

                lastUpdate = meta.modified;
                MainState.getInstance().settings.setLastCloudUpdate(lastUpdate);
            }
        }
        catch(PasswordDocument.IncorrectPasswordException e)
        {
            Log.e("password", "mismatched remote password");
            synchronized (updateLock) {isUpdating = false;}
            throw new PasswordDocument.IncorrectPasswordException();
        }
        catch (Exception e1){
            Log.e("password", "Failed to read remote data");
            System.out.println("Failed to read remote data");

            if (e1.getMessage().contains("trashed"))
            {
                Log.e("password", "trashed remote file");
                synchronized (updateLock) {isUpdating = false;}
                throw new RemoteTrashedException();
            }
            else if (force == false)
            {
                synchronized (updateLock) {isUpdating = false;}
                throw new RemoteIOException();
            }
        }

        if (force == false && remoteDoc == null)
        {
            Log.d("password", "remote password data is null, exiting");
            System.out.println("remote password data is null, exiting");
            synchronized (updateLock) {isUpdating = false;}
            return;
        }

        if ((force == true && (remoteDoc == null || !sourceDoc.history.equals(remoteDoc.history))) || (remoteDoc != null && !sourceDoc.history.equals(remoteDoc.history)))
        {
            Log.d("password", "local password data was updated.  syncing");
            System.out.println("local password data was updated.  syncing");
            overwrite();
        }

        synchronized (updateLock) {isUpdating = false;}
    }

    private boolean verifyHistory(PasswordDocumentHistory history)
    {
        Log.d("password", "verifying history playback");

        try
        {
            PasswordDocumentFile verifier = new PasswordDocumentFile("foo");
            verifier.deleteFiles();
            verifier.setPassword("");
            verifier.onLoad(true);
            verifier.playSubHistory(history);
            verifier.deleteFiles();
        } catch (Exception e){
            Log.d("password", "unplayable history generated");
            return false;
        }

        return true;
    }

    public void overwrite() throws RemoteIOException, UnplayableHistoryException
    {
        if (isOverwriting)
            return;

        isOverwriting = true;

        boolean success = verifyHistory(sourceDoc.getHistory());
        if (!success)
            throw new UnplayableHistoryException();

        try
        {
            String path = ((PasswordDocumentFile) sourceDoc).getHistoryFilePath();
            lastUpdate = MainState.getInstance().driveHelper.updateRemoteByPath(fileId, path);
        }
        catch (Exception e){
            Log.e("password", "Failed to write remote data");
            System.out.println("Failed to write remote data");
            synchronized (updateLock) {isUpdating = false;}
            isOverwriting = false;
            throw new RemoteIOException();
        }

        isOverwriting = false;
    }

    protected boolean isOverwriting = false;

    public void changePassword(String password) throws Exception
    {
        sourceDoc.setPassword(password);
        isOverwriting = true;
        sourceDoc.save();
    }

    public void delete() throws Exception
    {
        DriveHelper helper = MainState.getInstance().driveHelper;
        if (!helper.isConnected())
            return;

        helper.trashRemoteFile(fileId, true);
        sourceDoc.removeListener(fileListener);
        sourceDoc = null;
    }

    public void undelete() throws Exception
    {
        DriveHelper helper = MainState.getInstance().driveHelper;
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
