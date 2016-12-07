package com.munger.passwordkeeper.struct.documents;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.helpers.NavigationHelper;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

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
        MainState.getInstance().driveHelper.awaitConnection();

        if (!MainState.getInstance().driveHelper.isConnected())
        {
            Log.d("password", "google drive init failed");
            return;
        }

        lastRemoteUpdate = MainState.getInstance().settings.getLastRemoteUpdate();

        updateFromSource();
        handleConnected();
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
        }
    }

    private void handleConnected()
    {
        rootFolder = Drive.DriveApi.getRootFolder(apiClient);
        Query q = new Query.Builder()
                .addFilter(Filters.contains(SearchableField.TITLE, name))
                .build();
        rootFolder.queryChildren(apiClient, q).setResultCallback(new ResolvingResultCallbacks<DriveApi.MetadataBufferResult>(MainState.getInstance().activity, FILE_EXISTS_REQUEST)
        {
            @Override
            public void onSuccess(@NonNull DriveApi.MetadataBufferResult metadataBufferResult)
            {
                Log.v("password", "file query success");
                int sz = metadataBufferResult.getMetadataBuffer().getCount();

                if (sz == 0)
                {
                    metadataBufferResult.release();
                    createEmpty(new Callback() {public void callback(boolean result)
                    {
                        save();
                    }});
                }
                else
                {
                    Metadata metadata = metadataBufferResult.getMetadataBuffer().get(0);
                    DriveFile f = metadata.getDriveId().asDriveFile();
                    setTargetFile(f);
                    metadataBufferResult.release();

                    load(false);
                }
            }

            @Override
            public void onUnresolvableFailure(@NonNull Status status)
            {
                Log.v("password", "file query failed");
            }
        });
    }

    public interface Callback
    {
        void callback(boolean success);
    }


    protected void createEmpty(final Callback callback)
    {
        MetadataChangeSet set = new MetadataChangeSet.Builder()
                .setTitle(name)
                .setMimeType("application/octet-stream")
                .build();

        rootFolder.createFile(apiClient, set, null).setResultCallback(new ResolvingResultCallbacks<DriveFolder.DriveFileResult>(MainState.getInstance().activity, FILE_CREATE_REQUEST)
        {
            @Override
            public void onSuccess(@NonNull DriveFolder.DriveFileResult driveFileResult)
            {
                DriveFile f = driveFileResult.getDriveFile();
                setTargetFile(f);
                callback.callback(true);
            }

            @Override
            public void onUnresolvableFailure(@NonNull Status status)
            {
                Log.v("password", "file query failed");
            }
        });
    }

    protected ChangeListener listener = new ChangeListener()
    {
        @Override
        public void onChange(ChangeEvent changeEvent)
        {
            load(false);
        }
    };

    protected void setTargetFile(DriveFile file)
    {
        targetFile = file;
        targetFile.addChangeListener(apiClient, listener);
    }

    protected boolean hasRemoteLock()
    {
        return false;
    }

    protected void getRemoteLock()
    {
        if (targetFile == null)
        {
            Log.v("password", "target file never opened");
        }


    }

    protected void releaseRemoteLock()
    {
        if (targetFile == null)
        {
            Log.v("password", "target file never opened");
        }
    }

    public void save()
    {
        if (targetFile == null)
        {
            Log.v("password", "target file never opened");
        }

        getRemoteLock();
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
        getRemoteLock();

        AsyncTask t = new AsyncTask() {protected Object doInBackground(Object[] params)
        {
            DriveResource.MetadataResult metadata = targetFile.getMetadata(apiClient).await();
            long sz = metadata.getMetadata().getFileSize();
            if (metadata.getMetadata().isTrashed())
            {
                targetFile.untrash(apiClient);
            }

            PendingResult<DriveApi.DriveContentsResult> res = targetFile.open(apiClient, DriveFile.MODE_READ_ONLY, null);
            DriveApi.DriveContentsResult result = res.await();
            InputStream str = result.getDriveContents().getInputStream();
            DataInputStream dis = new DataInputStream(str);
            PasswordDocument remoteDoc = null;

            try
            {
                remoteDoc = new PasswordDocumentStream(dis, sz);
                remoteDoc.encoder = encoder;
                remoteDoc.load(false);
            }
            catch(Exception e){
                Log.v("password", "file load failed");
            }
            finally{
                try
                {
                    dis.close();
                }
                catch(Exception e){}

                result.getDriveContents().discard(apiClient);
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
        targetFile.removeChangeListener(apiClient, listener);

        if (hasRemoteLock())
            releaseRemoteLock();
    }

    public boolean testPassword()
    {
        return true;
    }
}
