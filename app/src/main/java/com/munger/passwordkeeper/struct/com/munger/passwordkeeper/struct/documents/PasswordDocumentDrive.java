package com.munger.passwordkeeper.struct.com.munger.passwordkeeper.struct.documents;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvingResultCallbacks;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filter;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.munger.passwordkeeper.MainActivity;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
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

    public PasswordDocumentDrive(PasswordDocument source)
    {
        super();

        sourceDoc = source;
        name += ".pwc";

        init();
    }

    public PasswordDocumentDrive(PasswordDocument source, String name)
    {
        this(source);
    }

    public PasswordDocumentDrive(PasswordDocument source, String name, String password)
    {
        this(source, name);
    }

    protected void init()
    {
        AsyncTask t = new AsyncTask() {protected Object doInBackground(Object[] params)
        {
            updateFromSource();
            setupGoogleApi();
            return null;
        }};
        t.execute(new Object[]{});
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

    private void setupGoogleApi()
    {
        apiClient = MainActivity.getInstance().driveHelper.getClient();
        apiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks()
        {
            @Override
            public void onConnected(@Nullable Bundle bundle)
            {
                handleConnected();
            }

            @Override
            public void onConnectionSuspended(int i)
            {

            }
        });

        if (apiClient.isConnected())
            handleConnected();
    }

    private void handleConnected()
    {
        rootFolder = Drive.DriveApi.getRootFolder(apiClient);
        Query q = new Query.Builder()
                .addFilter(Filters.contains(SearchableField.TITLE, name))
                .build();
        rootFolder.queryChildren(apiClient, q).setResultCallback(new ResolvingResultCallbacks<DriveApi.MetadataBufferResult>(MainActivity.getInstance(), FILE_EXISTS_REQUEST)
        {
            @Override
            public void onSuccess(@NonNull DriveApi.MetadataBufferResult metadataBufferResult)
            {
                Log.v("password", "file query success");
                int sz = metadataBufferResult.getMetadataBuffer().getCount();

                try
                {
                    if (sz == 0)
                    {
                        save();
                    }
                    else
                    {
                        Metadata metadata = metadataBufferResult.getMetadataBuffer().get(0);
                        targetFile = metadata.getDriveId().asDriveFile();

                        load(false);
                    }
                }
                catch(Exception e){
                    Log.e("password", "initial remote file load failed");
                }
            }

            @Override
            public void onUnresolvableFailure(@NonNull Status status)
            {
                Log.v("password", "file query failed");
            }
        });
    }

    public void save() throws Exception
    {
        if (targetFile == null)
        {
            createEmpty();
            return;
        }

        AsyncTask t = new AsyncTask() {protected Object doInBackground(Object[] params)
        {
            awaitHistoryLoaded();

            PendingResult<DriveResource.MetadataResult> pres = targetFile.getMetadata(apiClient);
            DriveResource.MetadataResult mres = pres.await();
            long sz = mres.getMetadata().getFileSize();

            if (sz > 0)
            {
                saveFresh();
            }
            else
            {
                update();
            }
            return null;
        }};
        t.execute(new Object[] {});
    }

    private void saveFresh()
    {
        PendingResult<DriveApi.DriveContentsResult> res = targetFile.open(apiClient, DriveFile.MODE_WRITE_ONLY, null);
        DriveApi.DriveContentsResult result = res.await();
        OutputStream os = result.getDriveContents().getOutputStream();
        DataOutputStream dos = new DataOutputStream(os);

        try
        {
            deltasToEncryptedString(dos);

            dos.flush();
        }
        catch(Exception e){
            Log.v("password", "file update failed");
        }
        finally{
            try
            {
                dos.close();
                result.getDriveContents().commit(apiClient, null).setResultCallback(new ResolvingResultCallbacks<Status>(MainActivity.getInstance(), FILE_SAVE_REQUEST) {
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
            catch(Exception e){}
        }
    }

    private void update()
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
            result.getDriveContents().commit(apiClient, null).setResultCallback(new ResolvingResultCallbacks<Status>(MainActivity.getInstance(), FILE_SAVE_REQUEST) {
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
        }
    }

    private void createEmpty()
    {
        MetadataChangeSet set = new MetadataChangeSet.Builder()
                .setTitle(name)
                .setMimeType("application/octet-stream")
                .build();

        rootFolder.createFile(apiClient, set, null).setResultCallback(new ResolvingResultCallbacks<DriveFolder.DriveFileResult>(MainActivity.getInstance(), FILE_CREATE_REQUEST)
        {
            @Override
            public void onSuccess(@NonNull DriveFolder.DriveFileResult driveFileResult)
            {
                targetFile = driveFileResult.getDriveFile();

                try
                {
                    save();
                }
                catch(Exception e){

                }
            }

            @Override
            public void onUnresolvableFailure(@NonNull Status status)
            {
                Log.v("password", "file query failed");
            }
        });
    }

    public void load(boolean force) throws Exception
    {
        AsyncTask t = new AsyncTask() {protected Object doInBackground(Object[] params)
        {
            PendingResult<DriveApi.DriveContentsResult> res = targetFile.open(apiClient, DriveFile.MODE_READ_ONLY, null);
            DriveApi.DriveContentsResult result = res.await();
            InputStream str = result.getDriveContents().getInputStream();
            DataInputStream dis = new DataInputStream(str);
            PasswordDocument remoteDoc = null;

            try
            {
                remoteDoc = new PasswordDocumentStream(dis);
                remoteDoc.encoder = encoder;
                remoteDoc.load(false);
            }
            catch(EOFException e1){
                saveFresh();
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

    protected void onClose() throws Exception
    {

    }

    public void delete() throws Exception
    {

    }

    public boolean testPassword()
    {
        return true;
    }
}
