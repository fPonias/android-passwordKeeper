package com.munger.passwordkeeper.struct.documents;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.test.mock.MockContext;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResolvingResultCallbacks;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.events.ChangeEvent;
import com.google.android.gms.drive.events.ChangeListener;
import com.google.android.gms.drive.metadata.CustomPropertyKey;
import com.google.android.gms.drive.metadata.SearchableMetadataField;
import com.google.android.gms.drive.metadata.internal.MetadataBundle;
import com.google.android.gms.drive.query.Filter;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.munger.passwordkeeper.HelperNoInst;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.helpers.DriveHelper;
import com.munger.passwordkeeper.helpers.DriveRemoteLock;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.struct.Config;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import com.munger.passwordkeeper.struct.Settings;

import org.mockito.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by codymunger on 12/7/16.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({MetadataBuffer.class, Log.class, Filters.class, Query.Builder.class, MetadataBundle.class})
public class PasswordDocumentDriveTest
{
    public class MainStateDer extends MainState
    {
        @Override
        public void setupDocument()
        {
            this.document = doc;
        }

        @Override
        protected void setupNavigation()
        {
            navigationHelper = navigationMock;
        }

        @Override
        public void setupDriveHelper()
        {
            driveHelper = driveHelperMock;
        }

        @Override
        protected void setupPreferences()
        {
            settings = settingsMock;
        }

        @Override
        protected void setupConfig()
        {
            config = configMock;
        }
    }

    public class PasswordDocumentDriveDer extends PasswordDocumentDrive
    {
        public PasswordDocumentDriveDer(PasswordDocument source)
        {
            super(source);
        }

        public PasswordDocumentDriveDer(PasswordDocument source, String name)
        {
            this(source);
        }

        public PasswordDocumentDriveDer(PasswordDocument source, String name, String password)
        {
            this(source, name);
        }

        @Override
        protected void setupDriveApi()
        {
            driveApi = driveApiMock;
        }

        @Override
        protected Query.Builder getQueryBuilder()
        {
            Query q = mock(Query.class);
            Query.Builder builder = mock(Query.Builder.class);
            doReturn(builder).when(builder).addFilter(any(Filter.class));
            doReturn(q).when(builder).build();

            return builder;
        }

        @Override
        protected void setTargetFile(DriveFile target)
        {
            targetFile = target;
            targetLock = new DriveRemoteLockDer(googleApiMock, target);
        }

        @Override
        protected void doSave()
        {
            Thread t = new Thread(new Runnable() {public void run()
            {
                try{Thread.sleep(100);}catch(Exception e){}
                notifySaved();
            }});
            t.start();
        }

        @Override
        public void onLoad(boolean force)
        {
            Thread t = new Thread(new Runnable() {public void run()
            {
                try{Thread.sleep(100);}catch(Exception e){}
                notifyLoaded();
            }});
            t.start();
        }
    }

    public class DriveRemoteLockDer extends DriveRemoteLock
    {
        public DriveRemoteLockDer(GoogleApiClient apiClient, DriveFile file)
        {
            super(apiClient, file);
        }

        @Override
        protected MetadataChangeSet.Builder getMetadataBuilder()
        {
            return metadataBuilderMock;
        }
    }

    private MainStateDer mainState;
    private Context contextMock;
    private FragmentActivity activityMock;
    private PasswordDocument doc;
    private NavigationHelper navigationMock;
    private Settings settingsMock;
    private DriveHelper driveHelperMock;
    private GoogleApiClient googleApiMock;
    private DriveApi driveApiMock;
    private Config configMock;
    private Filter filterMock;
    private DriveFile driveFileMock;
    private DriveFolder driveRootMock;
    private MetadataChangeSet.Builder metadataBuilderMock;
    private ArrayList<ChangeListener> fileChangeListeners;

    private final static String DEFAULT_FILENAME = "password-test";
    private final static String DEFAULT_PASSWORD = "pass";

    @Before
    public void before() throws Exception
    {
        PowerMockito.mockStatic(Log.class);
        PowerMockito.mockStatic(Filters.class);

        filterMock = mock(Filter.class);
        when(Filters.contains(any(SearchableMetadataField.class), any(String.class))).thenReturn(filterMock);
        driveFileMock = Mockito.mock(DriveFile.class);
        fileChangeListeners = new ArrayList<>();
        doAnswer(new Answer<PendingResult<com.google.android.gms.common.api.Status>>() {public PendingResult<com.google.android.gms.common.api.Status> answer(InvocationOnMock invocation) throws Throwable
        {
            ChangeListener listener = invocation.getArgumentAt(1, ChangeListener.class);
            fileChangeListeners.add(listener);
            PendingResult<com.google.android.gms.common.api.Status> ret = mock(PendingResult.class);
            return ret;
        }}).when(driveFileMock).addChangeListener(any(GoogleApiClient.class), any(ChangeListener.class));
        driveRootMock = Mockito.mock(DriveFolder.class);
        metadataBuilderMock = mock(MetadataChangeSet.Builder.class);

        contextMock = Mockito.mock(MockContext.class);
        activityMock = Mockito.mock(FragmentActivity.class);

        driveApiMock = Mockito.mock(DriveApi.class);

        navigationMock = Mockito.mock(NavigationHelper.class);

        settingsMock = Mockito.mock(Settings.class);
        doReturn("123").when(settingsMock).getDeviceUID();

        configMock = new Config();
        configMock.enableImportOption = true;
        File tmpPath = new File("./tmp");
        if (!tmpPath.exists())
            tmpPath.mkdir();
        configMock.localDataFilePath = tmpPath.getAbsolutePath();

        driveHelperMock = mock(DriveHelper.class);

        googleApiMock = Mockito.mock(GoogleApiClient.class);
        doReturn(googleApiMock).when(driveHelperMock).getClient();
        doReturn(googleApiMock).when(driveHelperMock).connect();

        mainState = new MainStateDer();
        MainState.setInstance(mainState);
        mainState.setContext(contextMock, activityMock);

        status = new Status();
    }

    private class Status
    {
        public int wasCalled = 0;
        public boolean initted = false;
        public boolean wasSaved = false;
        public boolean wasUpdated = false;
        public boolean failed = false;
    }

    protected class DefaultEventHandler implements PasswordDocumentDrive.DocumentEvents
    {
        @Override
        public void initFailed(Exception e)
        {
            status.wasCalled++;
            status.initted = false;
        }

        @Override
        public void initted()
        {
            status.wasCalled++;
            status.initted = true;
        }

        @Override
        public void saved()
        {
            status.wasCalled++;
            status.wasSaved = true;
        }

        @Override
        public void loaded()
        {
            status.wasCalled++;
            status.wasUpdated = true;
        }

        public void deleted()
        {}

        public void closed()
        {}
    }

    private Object lock = new Object();
    private Status status;
    private PasswordDocumentDrive.DocumentEvents listener;

    public class driveMockOptions
    {
        public boolean isConnected = true;
        public int driveFileCount = 1;
        public long driveFileSize = 0;
        public boolean isTrashed = false;
        public boolean throwQueryError = false;
        public boolean throwDeleteError = false;
        public PasswordDocument remoteDoc = null;
    }

    private void setupDriveMock(final driveMockOptions options)
    {
        PendingResult<?> result = Mockito.mock(PendingResult.class);
        final DriveApi.MetadataBufferResult bresult = Mockito.mock(DriveApi.MetadataBufferResult.class);
        MetadataBuffer mbuf = Mockito.mock(MetadataBuffer.class);
        Metadata metadata = Mockito.mock(Metadata.class);
        DriveId driveIdmock = Mockito.mock(DriveId.class);



        doAnswer(new Answer<Void>() {public Void answer(InvocationOnMock invocation) throws Throwable
        {
            Thread.sleep(250);
            return null;
        }}).when(driveHelperMock).awaitConnection();

        doReturn(options.isConnected).when(driveHelperMock).isConnected();
        doReturn(driveRootMock).when(driveApiMock).getRootFolder(any(GoogleApiClient.class));
        doReturn(result).when(driveRootMock).queryChildren(Matchers.any(GoogleApiClient.class), Matchers.any(Query.class));

        doAnswer(new Answer<Void>() {public Void answer(InvocationOnMock invocation) throws Throwable
        {
            ResolvingResultCallbacks<DriveApi.MetadataBufferResult> arg = (ResolvingResultCallbacks<DriveApi.MetadataBufferResult>) invocation.getArgumentAt(0, ResolvingResultCallbacks.class);
            Thread.sleep(250);

            if (!options.throwQueryError)
                arg.onSuccess(bresult);
            else
                arg.onUnresolvableFailure(new com.google.android.gms.common.api.Status(0));

            return null;
        }}).when(result).setResultCallback(Matchers.any(ResolvingResultCallbacks.class));

        doReturn(mbuf).when(bresult).getMetadataBuffer();
        doReturn(options.driveFileCount).when(mbuf).getCount();
        doReturn(metadata).when(mbuf).get(0);
        doReturn(driveIdmock).when(metadata).getDriveId();
        doReturn(driveFileMock).when(driveIdmock).asDriveFile();
        doReturn(options.isTrashed).when(metadata).isTrashed();
        doReturn(options.driveFileSize).when(metadata).getFileSize();
    }

    public class MockedMetadata
    {
        public String lockValue = "";

        public PendingResult<?> result;
        public DriveResource.MetadataResult mresult;
        public Metadata metadata;
        public Map<CustomPropertyKey, String> props;

        public MockedMetadata()
        {
            this("");
        }

        public MockedMetadata(String value)
        {
            this(value, System.currentTimeMillis());
        }

        public MockedMetadata(String value, long modifiedDate)
        {
            lockValue = value;

            result = Mockito.mock(PendingResult.class);
            mresult = Mockito.mock(DriveResource.MetadataResult.class);
            metadata = Mockito.mock(Metadata.class);

            props = mock(HashMap.class);
            doAnswer(new Answer<String>() {public String answer(InvocationOnMock invocation) throws Throwable
            {
                return lockValue;
            }}).when(props).get(any(CustomPropertyKey.class));
            doReturn(true).when(props).containsKey(any(CustomPropertyKey.class));

            doReturn(result).when(driveFileMock).getMetadata(any(GoogleApiClient.class));
            doAnswer(new Answer<DriveResource.MetadataResult>() {public DriveResource.MetadataResult answer(InvocationOnMock invocation) throws Throwable
            {
                Thread.sleep(250);

                return mresult;
            }}).when(result).await();

            doReturn(metadata).when(mresult).getMetadata();
            doReturn(props).when(metadata).getCustomProperties();

            Date lastMod = new Date(modifiedDate);
            doReturn(lastMod).when(metadata).getModifiedDate();
        }
    }

    private void setupLockClaim(final MockedMetadata data)
    {
        setupLockClaim(data, null);
    }

    private void setupLockClaim(final MockedMetadata data, final String retValue)
    {
        final MetadataBundle b = PowerMockito.mock(MetadataBundle.class);
        //when(b.zzbcz()).then(new Answer<MetadataBundle>() {public MetadataBundle answer(InvocationOnMock invocation) throws Throwable  { return b; }});
        PowerMockito.mockStatic(MetadataBundle.class);
        //when(MetadataBundle.zzbcy()).then(new Answer<MetadataBundle>() {public MetadataBundle answer(InvocationOnMock invocation) throws Throwable  { return b; }});
        MetadataChangeSet set = new MetadataChangeSet(b);
        doAnswer(new Answer<MetadataChangeSet.Builder>() {public MetadataChangeSet.Builder answer(InvocationOnMock invocation) throws Throwable
        {
            if (retValue == null)
            {
                String value = invocation.getArgumentAt(1, String.class);
                data.lockValue = value;
            }
            else
                data.lockValue = retValue;

            return metadataBuilderMock;
        }}).when(metadataBuilderMock).setCustomProperty(any(CustomPropertyKey.class), any(String.class));
        doReturn(set).when(metadataBuilderMock).build();

        doReturn(data.result).when(driveFileMock).updateMetadata(any(GoogleApiClient.class), any(MetadataChangeSet.class));
    }

    private PasswordDocumentDrive awaitInitEvents(int count) throws InterruptedException
    {
        return awaitInitEvents(count, true);
    }

    private PasswordDocumentDrive awaitInitEvents(int count, boolean doTimeout) throws InterruptedException
    {
        listener = new DefaultEventHandler()
        {
            @Override
            public void initted()
            {
                super.initted();
                synchronized (lock){ lock.notify(); }
            }

            @Override
            public void initFailed(Exception e)
            {
                super.initFailed(e);
                synchronized (lock){lock.notify(); }
            }

            @Override
            public void loaded() {
                super.loaded();
                synchronized (lock){lock.notify();}
            }
        };

        doc = HelperNoInst.generateDocument(2, 2);
        final PasswordDocumentDriveDer driveDoc = new PasswordDocumentDriveDer(doc, DEFAULT_FILENAME, DEFAULT_PASSWORD);
        driveDoc.addListener(listener);

        new Thread(new Runnable() {public void run()
        {
            driveDoc.init();
        }}, "driveDocInit").start();

        for (int i = 0; i < count; i++)
        {
            synchronized (lock)
            {
                if (doTimeout)
                    lock.wait(5000);
                else
                    lock.wait();
            }
        }

        return driveDoc;
    }

    @Test
    public void connects() throws InterruptedException
    {
        driveMockOptions options = new driveMockOptions();
        options.isConnected = true;
        options.driveFileCount = 1;
        options.driveFileSize = 1000;
        setupDriveMock(options);

        MockedMetadata data = new MockedMetadata();
        setupLockClaim(data);

        awaitInitEvents(2);

        assertTrue(status.wasCalled >= 1);
        assertTrue(status.initted);
    }

    @Test
    public void handlesConnectFail() throws InterruptedException
    {
        driveMockOptions options = new driveMockOptions();
        options.isConnected = false;
        setupDriveMock(options);

        MockedMetadata data = new MockedMetadata();
        setupLockClaim(data);

        awaitInitEvents(1);

        assertEquals(1, status.wasCalled);
        assertFalse(status.initted);
    }

    private void setupCreateFile(final boolean callSuccess)
    {
        PendingResult<DriveFolder.DriveFileResult> presult = mock(PendingResult.class);

        doReturn(presult).when(driveRootMock).createFile(any(GoogleApiClient.class), any(MetadataChangeSet.class), any(DriveContents.class));
        doAnswer(new Answer<DriveFolder>() {public DriveFolder answer(InvocationOnMock invocation) throws Throwable
        {
            final ResolvingResultCallbacks<DriveFolder.DriveFileResult> callback = invocation.getArgumentAt(0, ResolvingResultCallbacks.class);
            Thread t = new Thread(new Runnable() {public void run()
            {
                try{Thread.sleep(250);}catch(Exception e){return;}

                if (callSuccess)
                {
                    DriveFolder.DriveFileResult result = mock(DriveFolder.DriveFileResult.class);
                    doReturn(driveFileMock).when(result).getDriveFile();
                    callback.onSuccess(result);
                }
                else
                {
                    com.google.android.gms.common.api.Status status = new com.google.android.gms.common.api.Status(0);
                    callback.onFailure(status);
                }
            }});
            t.start();

            return null;
        }}).when(presult).setResultCallback(any(ResultCallback.class));
    }

    private void setupDeleteFile(final boolean callSuccess)
    {
        PendingResult<com.google.android.gms.common.api.Status> presult = mock(PendingResult.class);
        doReturn(presult).when(driveFileMock).delete(any(GoogleApiClient.class));
        doAnswer(new Answer<com.google.android.gms.common.api.Status>() {public com.google.android.gms.common.api.Status answer(InvocationOnMock invocation) throws Throwable
        {
            Thread.sleep(250);

            if (!callSuccess)
                throw new Exception("delete failed");

            return new com.google.android.gms.common.api.Status(0);
        }}).when(presult).await();
    }

    @Test
    public void createsEmptyFileOnMissingTarget() throws InterruptedException
    {
        driveMockOptions options = new driveMockOptions();
        options.isConnected = true;
        options.driveFileCount = 0;
        setupDriveMock(options);
        setupCreateFile(true);

        MockedMetadata data = new MockedMetadata();
        setupLockClaim(data);

        awaitInitEvents(2);

        verify(driveRootMock, times(1)).createFile(any(GoogleApiClient.class), any(MetadataChangeSet.class), any(DriveContents.class));
        assertEquals(2, status.wasCalled);
        assertEquals(true, status.initted);
        assertEquals(true, status.wasSaved);
    }

    @Test
    public void loadFileOnPresentTarget() throws InterruptedException
    {
        driveMockOptions options = new driveMockOptions();
        options.isConnected = true;
        options.driveFileCount = 1;
        options.driveFileSize = 1000;
        setupDriveMock(options);

        MockedMetadata data = new MockedMetadata();
        setupLockClaim(data);

        awaitInitEvents(2);

        assertEquals(2, status.wasCalled);
        assertEquals(true, status.initted);
        assertEquals(true, status.wasUpdated);
    }

    @Test
    public void deletesTrashedFileAndCreatesEmpty() throws InterruptedException
    {
        driveMockOptions options = new driveMockOptions();
        options.isConnected = true;
        options.driveFileCount = 1;
        options.isTrashed = true;
        setupDriveMock(options);
        setupCreateFile(true);
        setupDeleteFile(true);

        MockedMetadata data = new MockedMetadata();
        setupLockClaim(data);

        awaitInitEvents(2);

        verify(driveFileMock, times(1)).delete(any(GoogleApiClient.class));
        verify(driveRootMock, times(1)).createFile(any(GoogleApiClient.class), any(MetadataChangeSet.class), any(DriveContents.class));
        assertEquals(2, status.wasCalled);
        assertEquals(true, status.initted);
        assertEquals(true, status.wasSaved);
    }

    @Test
    public void handleQueryError() throws InterruptedException
    {
        driveMockOptions options = new driveMockOptions();
        options.isConnected = true;
        options.driveFileCount = 1;
        options.driveFileSize = 1000;
        options.isTrashed = false;
        options.throwQueryError = true;
        setupDriveMock(options);
        setupCreateFile(true);

        MockedMetadata data = new MockedMetadata();
        setupLockClaim(data);

        awaitInitEvents(1);

        assertEquals(1, status.wasCalled);
        assertEquals(false, status.initted);
    }

    @Test
    public void handleCreateError() throws InterruptedException
    {
        driveMockOptions options = new driveMockOptions();
        options.isConnected = true;
        options.driveFileCount = 0;
        options.isTrashed = false;
        setupDriveMock(options);
        setupCreateFile(false);

        MockedMetadata data = new MockedMetadata();
        setupLockClaim(data);

        awaitInitEvents(1);

        verify(driveRootMock, times(1)).createFile(any(GoogleApiClient.class), any(MetadataChangeSet.class), any(DriveContents.class));
        assertEquals(1, status.wasCalled);
        assertEquals(false, status.initted);
    }

    @Test
    public void handleDeleteError() throws InterruptedException
    {
        driveMockOptions options = new driveMockOptions();
        options.isConnected = true;
        options.driveFileCount = 1;
        options.isTrashed = true;
        options.throwDeleteError = true;
        setupDriveMock(options);
        setupCreateFile(true);
        setupDeleteFile(false);

        MockedMetadata data = new MockedMetadata();
        setupLockClaim(data);

        awaitInitEvents(1);

        verify(driveFileMock, times(1)).delete(any(GoogleApiClient.class));
        assertEquals(1, status.wasCalled);
        assertEquals(false, status.initted);
    }


    private PasswordDocumentDrive doLoad() throws InterruptedException
    {
        return doLoad(true);
    }

    private PasswordDocumentDrive doLoad(boolean doTimeout) throws InterruptedException
    {
        driveMockOptions options = new driveMockOptions();
        options.isConnected = true;
        options.driveFileCount = 1;
        options.driveFileSize = 0;
        setupDriveMock(options);

        MockedMetadata data = new MockedMetadata();
        setupLockClaim(data);

        PasswordDocumentDrive doc = awaitInitEvents(2, doTimeout);

        assertEquals(2, status.wasCalled);
        assertEquals(true, status.initted);
        assertEquals(true, status.wasUpdated);

        doc.removeListeners();

        return doc;
    }

    @Test
    public void loadsOnUpdateEvent() throws InterruptedException
    {
        doLoad();
    }

    @Test
    public void savesOnLocalSaveEvent() throws InterruptedException
    {
        final PasswordDocumentDrive doc = doLoad();


        status = new Status();
        listener = new DefaultEventHandler()
        {
            @Override
            public void saved() {
                super.saved();
                synchronized (lock){lock.notify();}
            }
        };

        doc.addListener(listener);

        new Thread(new Runnable() {public void run()
        {
            PasswordDetails dets = new PasswordDetails();
            PasswordDetailsPair pair = dets.addEmptyPair();
            pair.setKey("key9");
            pair.setValue("value9");

            try
            {
                doc.addDetails(dets);
                doc.save();
            }
            catch(Exception e){
                status.failed = true;
                synchronized (lock){lock.notify();}
            }
        }}, "driveDocSave").start();

        for (int i = 0; i < 1; i++)
        {
            synchronized (lock)
            {
                lock.wait(5000);
            }
        }

        assertEquals(1, status.wasCalled);
        assertFalse(status.failed);
        assertTrue(status.wasSaved);
    }

    @Test
    public void remoteChanges() throws InterruptedException
    {
        final PasswordDocumentDrive doc = doLoad();

        status = new Status();
        listener = new DefaultEventHandler()
        {
            @Override
            public void loaded()
            {
                super.loaded();
                synchronized (lock){lock.notify();}
            }
        };

        doc.addListener(listener);

        new Thread(new Runnable() {public void run()
        {
            try{Thread.sleep(10);}catch(Exception e){}
            //there is no way to simulate actual drive change events
            doc.onLoad(false);
        }}, "driveDocLoad").start();

        for (int i = 0; i < 1; i++)
        {
            synchronized (lock)
            {
                lock.wait();
            }
        }

        assertEquals(1, status.wasCalled);
        assertFalse(status.failed);
        assertTrue(status.wasUpdated);
    }
}
