package com.munger.passwordkeeper.struct.documents;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.media.MediaHttpDownloader;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import com.munger.passwordkeeper.Main;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.File;

public class PasswordDocumentDrive {

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
    private Drive drive;

    public PasswordDocumentDrive() throws Exception
    {
        init();
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
        Credential credential = authorize();
        // set up the global Drive instance
        drive = new Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
    }
    
    public void fileList() throws Exception
    {
        FileList result = drive.files().list().execute();
        for(com.google.api.services.drive.model.File file: result.getFiles()) 
        {
            System.out.printf("Found file: %s (%s)\n", file.getName(), file.getId());
        }
    }
}
