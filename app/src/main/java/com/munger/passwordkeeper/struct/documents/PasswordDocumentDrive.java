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
import com.google.android.gms.drive.events.ChangeEvent;
import com.google.android.gms.drive.events.ChangeListener;
import com.google.android.gms.drive.query.Filter;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.helpers.DriveRemoteLock;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

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

    protected DriveApi driveApi;
    private GoogleApiClient apiClient;
    private DriveFolder rootFolder;
    protected DriveFile targetFile;
    protected DriveRemoteLock targetLock;
    protected ChangeListener fileListener;
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
        setupDriveApi();

        updateFromSource();
        Log.d("password", "syncing with remote");
        handleConnected();
    }

    protected void setupDriveApi()
    {
        driveApi = Drive.DriveApi;
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

    protected Query.Builder getQueryBuilder()
    {
        return new Query.Builder();
    }

    private void handleConnected()
    {
        Log.d("password", "checking for remote document");
        rootFolder = driveApi.getRootFolder(apiClient);
        Filter f = Filters.contains(SearchableField.TITLE, name);
        Query q = getQueryBuilder().addFilter(f).build();
        PendingResult<DriveApi.MetadataBufferResult> result = rootFolder.queryChildren(apiClient, q);
        result.setResultCallback(new ResolvingResultCallbacks<DriveApi.MetadataBufferResult>(MainState.getInstance().activity, FILE_EXISTS_REQUEST)
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

    @Override
    protected void notifyInitted()
    {
        Log.d("password", "fetching document metadata");
        DriveResource.MetadataResult metadata = targetFile.getMetadata(apiClient).await();
        Date dt = metadata.getMetadata().getModifiedDate();
        lastRemoteUpdate = dt.getTime();

        super.notifyInitted();
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
                try{save();} catch(Exception e) {return;}
                notifyInitted();
            }});
        }
        else
        {
            Metadata metadata = metadataBufferResult.getMetadataBuffer().get(0);
            DriveFile f = metadata.getDriveId().asDriveFile();
            if (metadata.isTrashed())
            {
                Log.d("password", "replacing trashed remote document");
                try
                {
                    PendingResult<com.google.android.gms.common.api.Status> result =  f.delete(apiClient);
                    result.await();
                }
                catch(Exception e){
                    Log.d("password", "failed to delete remote trashed file");
                    notifyInitError(new Exception("failed to delete remote trashed file"));
                }

                metadataBufferResult.release();
                createEmpty(new Callback() {public void callback(boolean result)
                {
                    try{save();} catch(Exception e) {return;}
                    notifyInitted();
                }});
            }
            else
            {
                setTargetFile(f);

                if (metadata.getFileSize() == 0)
                {
                    try
                    {
                        save();
                        notifyLoaded();
                    }
                    catch(Exception e) {return;}
                }
                else
                    try{load(false);} catch(Exception e) {return;}

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
        if (targetFile != null)
        {
            targetFile.removeChangeListener(apiClient, fileListener);
        }

        targetFile = target;
        targetLock = new DriveRemoteLock(apiClient, target);
        targetFile.addChangeListener(apiClient, new ChangeListener() {public void onChange(ChangeEvent changeEvent)
        {
            if (changeEvent.hasContentChanged())
            {
                onLoad(false);
            }
        }});
    }

    public void onSave()
    {
        final long currentDt = System.currentTimeMillis();

        Thread t = new Thread(new Runnable() {public void run()
        {
            Log.d("password", "updating remote document");

            try{targetLock.get();} catch(DriveRemoteLock.FailedToAttainLockException e){return;}

            Log.d("password", "fetching document metadata");
            DriveResource.MetadataResult metadata = targetFile.getMetadata(apiClient).await();
            Date dt = metadata.getMetadata().getModifiedDate();
            long remoteDt = dt.getTime();

            if (remoteDt != lastRemoteUpdate)
                doUpdate();
            else
                doSave();
        }}, "save thread");

        t.start();
    }

    protected void doUpdate()
    {
        try{targetLock.release();} catch(DriveRemoteLock.FailedToReleaseLockException e1){return;}
    }

    @Override
    public void save() throws Exception
    {
        onSave();
    }

    protected void doSave()
    {
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
            result.getDriveContents().commit(apiClient, null).setResultCallback(new ResolvingResultCallbacks<com.google.android.gms.common.api.Status>(MainState.getInstance().activity, FILE_SAVE_REQUEST) {
                @Override
                public void onSuccess(@NonNull com.google.android.gms.common.api.Status status)
                {
                    notifySaved();
                    Log.v("password", "file update succeeded");
                    try{targetLock.release();} catch(DriveRemoteLock.FailedToReleaseLockException e){return;}
                }

                @Override
                public void onUnresolvableFailure(@NonNull com.google.android.gms.common.api.Status status)
                {
                    try{targetLock.release();} catch(DriveRemoteLock.FailedToReleaseLockException e){return;}
                }
            });
        }
        catch(Exception e){
            Log.v("password", "file update failed");
            try{targetLock.release();} catch(DriveRemoteLock.FailedToReleaseLockException e1){return;}
        }
        finally{
            try
            {
                dis.close();

                if (dos != null)
                    dos.close();
            }
            catch(Exception e){}
        }
    }

    @Override
    public void load(boolean force) throws Exception
    {
        onLoad(force);
    }

    public void onLoad(boolean force)
    {
        Log.d("password", "loading remote document");
        try{targetLock.get();} catch(DriveRemoteLock.FailedToAttainLockException e){return;}

        Thread t = new Thread(new Runnable() {public void run()
        {
            Log.d("password", "fetching document metadata");
            DriveResource.MetadataResult metadataRes = targetFile.getMetadata(apiClient).await();
            Metadata metadata = metadataRes.getMetadata();
            long sz = metadata.getFileSize();
            Date modDt = metadata.getModifiedDate();

            PendingResult<DriveApi.DriveContentsResult> res = targetFile.open(apiClient, DriveFile.MODE_READ_ONLY, null);
            DriveApi.DriveContentsResult result = res.await();
            InputStream str = result.getDriveContents().getInputStream();
            DataInputStream dis = new DataInputStream(str);
            PasswordDocument remoteDoc = null;
            boolean loaded = false;

            try
            {
                Log.d("password", "reading remote document");
                remoteDoc = new PasswordDocumentStream(dis, sz);
                remoteDoc.encoder = encoder;
                remoteDoc.load(false);
                loaded = true;
                lastRemoteUpdate = modDt.getTime();

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
                try{targetLock.release();} catch(DriveRemoteLock.FailedToReleaseLockException e){}
            }

            if (!loaded)
                return;

            notifyLoaded();
            try {merge(remoteDoc);} catch(PasswordDocumentHistory.HistoryPlaybackException e){}
        }});

        t.start();

    }

    public void onDelete() throws Exception
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

    protected void merge(PasswordDocument doc) throws PasswordDocumentHistory.HistoryPlaybackException
    {
        PasswordDocumentHistory newHist = history.mergeHistory(doc.getHistory());

        if (newHist.equals(history))
            return;

        history = newHist;
        details = new ArrayList<>();
        playHistory();
        try{save();}catch(Exception e){}
    }
}
