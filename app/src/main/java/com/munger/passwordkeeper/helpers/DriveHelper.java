package com.munger.passwordkeeper.helpers;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentDrive;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class DriveHelper
{
    public DriveHelper()
    {

    }

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    protected Drive service;

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws java.io.IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException
    {
        // Load client secrets.
        AssetManager am = MainState.getInstance().context.getAssets();
        InputStream in = am.open("credentials.json");

        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        String TOKENS_DIRECTORY_PATH = MainState.getInstance().context.getFilesDir().getPath() + "/tokens";

        NetHttpTransport trans = new NetHttpTransport.Builder().build();
        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                trans, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        AuthorizationCodeInstalledApp app = new AuthorizationCodeInstalledApp(flow, receiver, new AuthorizationCodeInstalledApp.Browser() {
            @Override
            public void browse(String url) throws IOException {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                MainState.getInstance().context.startActivity(i);
            }
        });
        return app.authorize("user");
    }

    public void connect()
    {
        service = null;
        connected = false;

        try {
            // Build a new authorized API client service.
            int nameId = MainState.getInstance().context.getApplicationInfo().labelRes;
            String APPLICATION_NAME = MainState.getInstance().context.getResources().getString(nameId);
            final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();
            Credential credential = getCredentials(HTTP_TRANSPORT);
            service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            // Print the names and IDs for up to 10 files.
            FileList result = service.files().list()
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name, trashed)")
                    .execute();
            List<File> files = result.getFiles();

            if (files !=  null)
                connected = true;
        }
        catch (IOException e){
            Log.d("foo", "IOException");

            connected = false;
            service = null;
        }

        synchronized (lock)
        {
            lock.notify();
        }
    }

    public void cleanUp()
    {
        service = null;
        connected = false;
    }

    protected Object lock = new Object();
    protected Boolean connected = null;

    public Boolean isConnected()
    {
        return connected;
    }

    public void awaitConnection()
    {
        synchronized (lock)
        {
            if (connected != null)
                return;

            try{lock.wait();}catch(InterruptedException e){return;}
        }
    }

    public String getRemoteFileId(String name) throws IOException
    {
        if (!connected)
            return null;

        FileList listObj = service.files().list().execute();
        List<File> list = listObj.getFiles();
        for (File f : list)
        {
            if (f.getName().equals(name))
                return f.getId();
        }

        return null;
    }

    public String getOrCreateFile(String name) throws IOException
    {
        if (!connected)
            return null;

        String ret = getRemoteFileId(name);

        if (ret != null)
            return ret;

        File fl = new File();
        fl.setName(name);
        fl = service.files().create(fl).setFields("id").execute();

        return fl.getId();
    }

    public void trashRemoteFile(String fileId, boolean trashed) throws Exception
    {
        if (!connected)
            return;

        File meta = new File();
        meta.setTrashed(trashed);
        File fields = service.files().update(fileId, meta).setFields("trashed").execute();

        if (fields.getTrashed() != trashed)
            throw new Exception("failed to set trashed state on remote file " + fileId + " to " + trashed);
    }

    public static class Meta
    {
        public DateTime modified;
        public long size;
        public boolean trashed;
    }

    public Meta getMetadata(String fileId) throws IOException
    {
        Meta ret = new Meta();

        File f = service.files().get(fileId).setFields("size,modifiedTime,trashed").execute();

        ret.modified = f.getModifiedTime();
        ret.size = f.getSize();
        ret.trashed = f.getTrashed();

        return ret;
    }

    public InputStream getRemoteFile(String fileId) throws IOException
    {
        InputStream ins = service.files().get(fileId).executeMediaAsInputStream();
        return ins;
    }

    public DateTime updateRemoteByPath(String fileId, String path) throws IOException
    {
        FileContent newContent = new FileContent("application/octet-stream", new java.io.File(path));
        File f = service.files().update(fileId, null, newContent).setFields("modifiedTime").execute();
        DateTime ret = f.getModifiedTime();

        return ret;
    }
}