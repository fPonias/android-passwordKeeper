package com.munger.passwordkeeper.drive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpDownloaderProgressListener;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.Get;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.About;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.ChangeList;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.StartPageToken;
import com.google.api.services.drive.model.User;
import com.munger.passwordkeeper.Main;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;

public class DriveHelper
{
    private final String APPLICATION_NAME = "password crypt desktop";

    /**
     * Directory to store user credentials.
     */
    private final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".passwordKeeper/driveDataStore");

    /**
     * Global instance of the {@link DataStoreFactory}. The best practice is to
     * make it a single globally shared instance across your application.
     */
    private FileDataStoreFactory dataStoreFactory;

    /**
     * Global instance of the HTTP transport.
     */
    private HttpTransport httpTransport;

    /**
     * Global instance of the JSON factory.
     */
    private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /**
     * Global Drive API client.
     */
    private com.google.api.services.drive.Drive drive;
    
    private Credential credential = null;
    private User user = null;

    public DriveHelper() throws Exception
    {
        init();
    }
    
    public boolean isAuthorized()
    {
        return (user != null);
    }
    
    public User getUser()
    {
        return user;
    }
    
    private Credential authorize() throws Exception 
    {
        InputStream str = Main.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(str));
        
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, Collections.singleton(DriveScopes.DRIVE_FILE)
        ).setDataStoreFactory(dataStoreFactory).build();
        
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    public void init() throws Exception
    {
        httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        dataStoreFactory = new FileDataStoreFactory(DATA_STORE_DIR);
        // authorization
        credential = authorize();
        // set up the global Drive instance
        drive = new com.google.api.services.drive.Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
        About aboot = drive.about().get().setFields("user").execute();
        user = aboot.getUser();
    }
    
    public static String NEW_ID = "<new>" + System.currentTimeMillis();
        
    public class DriveFileStruct implements Serializable
    {
        public String id;
        public String name;
        public String path;
        
        @Override
        public String toString()
        {
            return name;
        }
    }
    
    public DriveFileStruct createNewFile(String name) throws Exception
    {
        com.google.api.services.drive.model.File file = new File();
        file.setName(name);
        file.setMimeType("application/octet-stream");

        File ret = drive.files().create(file)
                .setFields("id")
                .execute();
        
        DriveFileStruct str = new DriveFileStruct();
        str.id = ret.getId();
        str.name = ret.getName();
        str.path = "/";
        
        return str;
    }
    
    public DriveFileStruct openFileChooser() throws Exception
    {
        DriveFileStruct[] list = fileList();
        DriveFileStruct[] listWithBlank = new DriveFileStruct[list.length + 1];
        listWithBlank[0] = new DriveFileStruct();
        listWithBlank[0].id = NEW_ID;
        listWithBlank[0].name = "<new>";
        listWithBlank[0].path = "/";
        
        int i = 1;
        for (DriveFileStruct str : list)
        {
            listWithBlank[i] = str;
            i++;
        }
        
        DriveFileStruct result = (DriveFileStruct) JOptionPane.showInputDialog(Main.instance, "Choose a remote file", "", JOptionPane.PLAIN_MESSAGE, null, listWithBlank, null);
        return result;
    }
    
    public DriveFileStruct[] fileList() throws Exception
    {
        ArrayList<DriveFileStruct> list = new ArrayList<>();
        FileList result = drive.files().list().execute();
        for(com.google.api.services.drive.model.File file: result.getFiles()) 
        {
            DriveFileStruct item = new DriveFileStruct();
            item.id = file.getId();
            item.name = file.getName();
            item.path = "/";
            list.add(item);
        }
                    
        DriveFileStruct[] arr = new DriveFileStruct[list.size()];
        int i = 0;
        for (DriveFileStruct str : list)
        {
            arr[i] = str;
            i++;
        }
        return arr;
    }
    
    private class DownloadListener implements MediaHttpDownloaderProgressListener
    {
        @Override
        public void progressChanged(MediaHttpDownloader downloader) throws IOException 
        {
            switch (downloader.getDownloadState()) 
            {
            case MEDIA_IN_PROGRESS:
              System.out.println(downloader.getProgress());
              break;
            case MEDIA_COMPLETE:
              System.out.println("Download is complete!");
                lastUpdate = System.currentTimeMillis();
            }
        }
    }
    
    public byte[] download(DriveFileStruct file) throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        Drive.Files.Get request = drive.files().get(file.id);
        request.getMediaHttpDownloader().setProgressListener(new DownloadListener());
        request.getMediaHttpDownloader().setDirectDownloadEnabled(true);
        request.executeMediaAndDownloadTo(out);
        
        return out.toByteArray();
    }
    
    private class UploadListener implements MediaHttpUploaderProgressListener
    {
        @Override
        public void progressChanged(MediaHttpUploader uploader) throws IOException 
        {
            switch(uploader.getUploadState())
            {
                case MEDIA_IN_PROGRESS:
                    System.out.println(uploader.getProgress());
                    break;
                case MEDIA_COMPLETE:
                    System.out.println("upload finished");
                    lastUpdate = System.currentTimeMillis();
                    break;
            }
        }
        
    }
    
    public void upload(DriveFileStruct str, byte[] content) throws Exception
    {        
        InputStreamContent mediaContent = new InputStreamContent("application/octet-stream", new ByteArrayInputStream(content));
        mediaContent.setLength(content.length);
        Drive.Files.Update request = drive.files().update(str.id, null, mediaContent);
        request.getMediaHttpUploader().setProgressListener(new UploadListener());
        request.getMediaHttpUploader().setDirectUploadEnabled(true);
        request.execute();
    }
    
    private String startToken = null;
    private long lastUpdate = 0;
    
    public boolean changed(DriveFileStruct str) throws Exception
    {
        if (startToken == null)
        {
            Drive.Changes.GetStartPageToken startReq = drive.changes().getStartPageToken();
            StartPageToken startTokenStr = startReq.execute();
            startToken = startTokenStr.getStartPageToken();
        }
        
        boolean ret = false;
        Drive.Changes.List request = drive.changes().list(startToken);
        while(true)
        {
            try
            {
                ChangeList changes = request.execute();
                for(Change change : changes.getChanges())
                {
                    if (change.getFileId().equals(str.id) && change.getTime().getValue() > lastUpdate)
                        ret = true;
                }
                
                String nextToken = changes.getNextPageToken();
                
                if (nextToken == null)
                    break;

                startToken = nextToken;
                request.setPageToken(nextToken);
            }
            catch(IOException e){
                break;
            }
        }
        
        return true;
    }
    
    public long getLastModifiedDate(DriveFileStruct file)
    {
        try
        {
            Get get = drive.files().get(file.id);
            File f = get.execute();
            DateTime dt = f.getModifiedTime();
            return dt.getValue();
        }
        catch(IOException e){
            return -1;
        }
    }
    
    public String getMetadataValue(DriveFileStruct file, String key)
    {
        try
        { 
            Get get = drive.files().get(file.id);
            File f = get.execute();
            Map<String, String> props = f.getProperties();
            
            if(props.containsKey(key))
                return props.get(key);
        }
        catch(IOException e){
        }
        
        return null;
    }
    
    
    public boolean setMetadataValue(DriveFileStruct file, String key, String value)
    {
        try
        {
            Get get = drive.files().get(file.id);
            File f = get.execute();
            Map<String, String> props = f.getProperties();
            props.put(key, value);
            f.setProperties(props);
            
            Drive.Files.Update update = drive.files().update(file.id, f);
            update.execute();
            return true;
        }
        catch(IOException e){
            return false;
        }
    }
}

