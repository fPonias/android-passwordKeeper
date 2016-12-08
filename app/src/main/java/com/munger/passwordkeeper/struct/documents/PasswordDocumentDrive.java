package com.munger.passwordkeeper.struct.documents;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvingResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.helpers.DriveRemoteLock;

import java.io.DataInputStream;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by codymunger on 11/18/16.
 */

public class PasswordDocumentDrive extends PasswordDocument
{
    public static final String version = "0.1";
    private static final int LIST_CHILDREN_REQUEST = 1000;
    private static final int FILE_EXISTS_REQUEST = 1001;
    private static final int FILE_CREATE_REQUEST = 1002;
    private static final int FILE_SAVE_REQUEST = 1003;

    private GoogleApiClient apiClient;
    private DriveFolder rootFolder;
    private DriveFile targetFile;
    private DriveRemoteLock targetLock;
    private PasswordDocument sourceDoc;

    private long lastRemoteUpdate;

    public PasswordDocumentDrive(PasswordDocument source)
    {
        super();

        sourceDoc = source;
        name += ".pwc";
    }

    public PasswordDocumentDrive(PasswordDocument source, String name)
    {
        this(source);
    }

    public PasswordDocumentDrive(PasswordDocument source, String name, String password)
    {
        this(source, name);
    }

    private boolean initting = false;

    public void init()
    {
        initting = true;
        MainState.getInstance().driveHelper.awaitConnection();
        Log.d("password", "connected to google");

        if (!MainState.getInstance().driveHelper.isConnected())
        {
            Log.d("password", "google drive init failed");
            notifyInitError(new Exception("google drive init failed"));
            return;
        }

        apiClient = MainState.getInstance().driveHelper.getClient();
        lastRemoteUpdate = MainState.getInstance().settings.getLastRemoteUpdate();

        updateFromSource();
        Log.d("password", "syncing with remote");
        handleConnected();
    }

    public static interface DocumentEvents
    {
        void initFailed(Exception e);
        void initted();
        void saved();
        void updated();
    }

    protected ArrayList<DocumentEvents> listeners = new ArrayList<>();
    public void addListener(DocumentEvents listener)
    {
        listeners.add(listener);
    }

    public void removeListener(DocumentEvents listener)
    {
        listeners.remove(listener);
    }

    protected void notifyInitted()
    {
        if(!initting)
            return;

        initting = false;
        int sz = listeners.size();
        for (int i = sz - 1; i >= 0; i--)
            listeners.get(i).initted();
    }

    protected void notifyInitError(Exception e)
    {
        initting = false;
        int sz = listeners.size();
        for (int i = sz - 1; i >= 0; i--)
            listeners.get(i).initFailed(e);
    }

    protected void notifySaved()
    {
        int sz = listeners.size();
        for (int i = sz - 1; i >= 0; i--)
            listeners.get(i).saved();
    }

    protected void notifyUpdated()
    {
        int sz = listeners.size();
        for (int i = sz - 1; i >= 0; i--)
            listeners.get(i).updated();
    }



    private void updateFromSource()
    {
        sourceDoc.awaitHistoryLoaded();
        encoder = sourceDoc.getEncoder();

        try
        {
            playSubHistory(sourceDoc.history);
        }
        catch(Exception e){
            Log.e("password", "failed to update from source document");
            notifyInitError(new Exception("failed to update from source document"));
        }
    }

    private void handleConnected()
    {
        Log.d("password", "checking for remote document");
        rootFolder = Drive.DriveApi.getRootFolder(apiClient);
        Query q = new Query.Builder()
                .addFilter(Filters.contains(SearchableField.TITLE, name))
                .build();
        rootFolder.queryChildren(apiClient, q).setResultCallback(new ResolvingResultCallbacks<DriveApi.MetadataBufferResult>(MainState.getInstance().activity, FILE_EXISTS_REQUEST)
        {
            @Override
            public void onSuccess(@NonNull final DriveApi.MetadataBufferResult metadataBufferResult)
            {
                new Thread(new Runnable() {public void run()
                {
                    handleConnectedSuccess(metadataBufferResult);
                }}).start();
            }

            @Override
            public void onUnresolvableFailure(@NonNull Status status)
            {
                Log.v("password", "file query failed");
                notifyInitError(new Exception("remote file query failed"));
            }
        });
    }

    private void handleConnectedSuccess(DriveApi.MetadataBufferResult metadataBufferResult)
    {
        Log.d("password", "file query success");
        int sz = metadataBufferResult.getMetadataBuffer().getCount();

        if (sz == 0)
        {
            Log.d("password", "found no remote document");
            metadataBufferResult.release();
            createEmpty(new Callback() {public void callback(boolean result)
            {
                save();
                notifyInitted();
            }});
        }
        else
        {
            Metadata metadata = metadataBufferResult.getMetadataBuffer().get(0);
            if (metadata.isTrashed())
            {
                Log.d("password", "replacing trashed remote document");
                try
                {
                    PendingResult<com.google.android.gms.common.api.Status> result =  targetFile.delete(apiClient);
                    result.await();
                }
                catch(Exception e){
                    Log.d("password", "failed to delete remote trashed file");
                    notifyInitError(new Exception("failed to delete remote trashed file"));
                }

                metadataBufferResult.release();
                createEmpty(new Callback() {public void callback(boolean result)
                {
                    save();
                    notifyInitted();
                }});
            }
            else
            {
                DriveFile f = metadata.getDriveId().asDriveFile();
                setTargetFile(f);

                if (metadata.getFileSize() == 0)
                    save();
                else
                    load(false);

                metadataBufferResult.release();
                notifyInitted();
            }
        }
    }

    public interface Callback
    {
        void callback(boolean success);
    }


    protected void createEmpty(final Callback callback)
    {
        Log.d("password", "creating empty remote document");
        MetadataChangeSet set = new MetadataChangeSet.Builder()
                .setTitle(name)
                .setMimeType("application/octet-stream")
                .build();

        rootFolder.createFile(apiClient, set, null).setResultCallback(new ResolvingResultCallbacks<DriveFolder.DriveFileResult>(MainState.getInstance().activity, FILE_CREATE_REQUEST)
        {
            @Override
            public void onSuccess(@NonNull DriveFolder.DriveFileResult driveFileResult)
            {
                Log.d("password", "created remote document");
                DriveFile f = driveFileResult.getDriveFile();
                setTargetFile(f);
                callback.callback(true);
            }

            @Override
            public void onUnresolvableFailure(@NonNull Status status)
            {
                Log.v("password", "file query failed");
                notifyInitError(new Exception("file query failed"));
            }
        });
    }

    protected void setTargetFile(DriveFile target)
    {
        targetFile = target;
        targetLock = new DriveRemoteLock(apiClient, target);
    }

    public void save()
    {
        Log.d("password", "updating remote document");

        targetLock.get();
        targetLock.release();
    }

        /*
        PendingResult<DriveApi.DriveContentsResult> res = targetFile.open(apiClient, DriveFile.MODE_READ_WRITE, null);
        DriveApi.DriveContentsResult result = res.await();

        InputStream str = result.getDriveContents().getInputStream();
        DataInputStream dis = new DataInputStream(str);
        DataOutputStream dos = null;

        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            dos = new DataOutputStream(baos);

            updateEncryptedDeltas(dis, dos);

            dos.flush();
            result.getDriveContents().commit(apiClient, null).setResultCallback(new ResolvingResultCallbacks<Status>(MainState.getInstance().activity, FILE_SAVE_REQUEST) {
                @Override
                public void onSuccess(@NonNull Status status)
                {
                    Log.v("password", "file update succeeded");
                }

                @Override
                public void onUnresolvableFailure(@NonNull Status status)
                {
                    Log.v("password", "file update failed");
                }
            });
        }
        catch(Exception e){
            Log.v("password", "file update failed");
        }
        finally{
            try
            {
                dis.close();

                if (dos != null)
                    dos.close();
            }
            catch(Exception e){}
        }*/

    public void load(boolean force)
    {
        Log.d("password", "loading remote document");
        targetLock.get();

        AsyncTask t = new AsyncTask() {protected Object doInBackground(Object[] params)
        {
            Log.d("password", "fetching document metadata");
            DriveResource.MetadataResult metadata = targetFile.getMetadata(apiClient).await();
            long sz = metadata.getMetadata().getFileSize();

            PendingResult<DriveApi.DriveContentsResult> res = targetFile.open(apiClient, DriveFile.MODE_READ_ONLY, null);
            DriveApi.DriveContentsResult result = res.await();
            InputStream str = result.getDriveContents().getInputStream();
            DataInputStream dis = new DataInputStream(str);
            PasswordDocument remoteDoc = null;

            try
            {
                Log.d("password", "reading remote document");
                remoteDoc = new PasswordDocumentStream(dis, sz);
                remoteDoc.encoder = encoder;
                remoteDoc.load(false);
            }
            catch(Exception e){
                Log.v("password", "file load failed");
            }
            finally{
                Log.d("password", "cleaning up remote document read");
                try
                {
                    dis.close();
                }
                catch(Exception e){}

                result.getDriveContents().discard(apiClient);
                targetLock.release();
            }

            return null;
        }};

        t.execute(new Object[] {});
    }

    public void delete() throws Exception
    {

    }

    @Override
    protected void onClose() throws Exception
    {
        targetLock.cleanUp();
    }

    public boolean testPassword()
    {
        return true;
    }
}
