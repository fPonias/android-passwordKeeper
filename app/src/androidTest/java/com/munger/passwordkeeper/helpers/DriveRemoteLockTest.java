package com.munger.passwordkeeper.helpers;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.events.ChangeListener;
import com.google.android.gms.drive.metadata.CustomPropertyKey;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.TestingMainActivity;
import com.munger.passwordkeeper.struct.Settings;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;

import androidx.fragment.app.FragmentActivity;
import androidx.test.rule.ActivityTestRule;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Created by codymunger on 12/9/16.
 */


@RunWith(AndroidJUnit4.class)
@SmallTest
public class DriveRemoteLockTest
{

    public class MainStateDer extends MainState
    {
        @Override
        public void setupDocument()
        {
            documentMock = mock(PasswordDocumentFile.class);
            document = documentMock;
        }

        @Override
        protected void setupNavigation()
        {
            navigationMock = mock(NavigationHelper.class);
            navigationHelper = navigationMock;
        }

        @Override
        public void setupDriveHelper()
        {
        }

        @Override
        protected void setupPreferences()
        {
            settingsMock = mock(Settings.class);
            settings = settingsMock;
        }
    }

    private DriveRemoteLock target;
    private MainStateDer mainState;
    private PasswordDocument documentMock;
    private NavigationHelper navigationMock;
    private Settings settingsMock;
    private GoogleApiClient apiClientMock;
    private DriveFile driveFileMock;

    @Rule
    public ActivityTestRule<TestingMainActivity> activityRule = new ActivityTestRule<>(TestingMainActivity.class);


    @Before
    public void before()
    {
        FragmentActivity activity = activityRule.getActivity();
        mainState = new MainStateDer();
        MainState.setInstance(mainState);
        mainState.setContext(activity, activity);

        doReturn("abc").when(settingsMock).getDeviceUID();

        apiClientMock = mock(GoogleApiClient.class);
        driveFileMock = mock(DriveFile.class);
    }

    private void setupLockValue(String value)
    {
        PendingResult<?> result = mock(PendingResult.class);
        final DriveResource.MetadataResult mresult = mock(DriveResource.MetadataResult.class);
        Metadata mdata = mock(Metadata.class);
        Map<CustomPropertyKey, String> ret = new HashMap<>();

        if (value != null)
        {
            CustomPropertyKey key = new CustomPropertyKey(target.key, 0);
            ret.put(key, value);
        }

        doReturn(result).when(driveFileMock).getMetadata(apiClientMock);
        doAnswer(new Answer<DriveResource.MetadataResult>() {public DriveResource.MetadataResult answer(InvocationOnMock invocation) throws Throwable
        {
            try{Thread.sleep(100);}catch(InterruptedException e) {}
            return mresult;
        }}).when(result).await();
        doReturn(mdata).when(mresult).getMetadata();
        doReturn(ret).when(mdata).getCustomProperties();
    }

    private Map<CustomPropertyKey, String> lastProps = null;
    private String lastValue = null;

    private void setupLockUpdate(final String answer)
    {
        final PendingResult<?> result = mock(PendingResult.class);
        final DriveResource.MetadataResult mresult = mock(DriveResource.MetadataResult.class);
        Metadata mdata = mock(Metadata.class);

        doAnswer(new Answer<PendingResult>() {public PendingResult answer(InvocationOnMock invocation) throws Throwable
        {
            MetadataChangeSet set = invocation.getArgumentAt(1, MetadataChangeSet.class);

            lastProps = set.getCustomPropertyChangeMap();
            lastValue = lastProps.get(lastProps.keySet().toArray()[0]);

            return result;
        }}).when(driveFileMock).updateMetadata(eq(apiClientMock), any(MetadataChangeSet.class));

        doAnswer(new Answer<DriveResource.MetadataResult>() {public DriveResource.MetadataResult answer(InvocationOnMock invocation) throws Throwable
        {
            try{Thread.sleep(100);}catch(InterruptedException e) {}
            return mresult;
        }}).when(result).await();

        doReturn(mdata).when(mresult).getMetadata();

        doAnswer(new Answer() {public Object answer(InvocationOnMock invocation) throws Throwable
        {
            if (answer == null)
                return lastProps;
            else
            {
                Map<CustomPropertyKey, String> ret = new HashMap<CustomPropertyKey, String>();
                CustomPropertyKey key = (CustomPropertyKey) lastProps.keySet().toArray()[0];
                ret.put(key, answer);
                return ret;
            }
        }}).when(mdata).getCustomProperties();
    }

    @Test
    public void noKey()
    {
        target = new DriveRemoteLock(apiClientMock, driveFileMock);
        setupLockValue(null);
        DriveRemoteLock.RemoteLockState state = target.check();
        assertEquals(DriveRemoteLock.RemoteLockState.FREE, state);
    }

    @Test
    public void emptyKey()
    {
        target = new DriveRemoteLock(apiClientMock, driveFileMock);
        setupLockValue("");
        DriveRemoteLock.RemoteLockState state = target.check();
        assertEquals(DriveRemoteLock.RemoteLockState.FREE, state);
    }

    @Test
    public void corruptKey()
    {
        target = new DriveRemoteLock(apiClientMock, driveFileMock);
        setupLockValue("foobar");
        DriveRemoteLock.RemoteLockState state = target.check();
        assertEquals(DriveRemoteLock.RemoteLockState.FREE, state);
    }

    @Test
    public void ownedKey()
    {
        target = new DriveRemoteLock(apiClientMock, driveFileMock);
        long timestamp = System.currentTimeMillis();
        setupLockValue("abc " + timestamp);
        DriveRemoteLock.RemoteLockState state = target.check();
        assertEquals(DriveRemoteLock.RemoteLockState.OWNED, state);
    }

    @Test
    public void expiredKey()
    {
        target = new DriveRemoteLock(apiClientMock, driveFileMock);
        long timestamp = System.currentTimeMillis() - 90000;
        setupLockValue( "abc " + timestamp);
        DriveRemoteLock.RemoteLockState state = target.check();
        assertEquals(DriveRemoteLock.RemoteLockState.FREE, state);
    }

    @Test
    public void takenKey()
    {
        target = new DriveRemoteLock(apiClientMock, driveFileMock);
        long timestamp = System.currentTimeMillis();
        setupLockValue("notmyid " + timestamp);
        DriveRemoteLock.RemoteLockState state = target.check();
        assertEquals(DriveRemoteLock.RemoteLockState.LOCKED, state);
    }

    @Test
    public void claimNoContest() throws Exception
    {
        target = new DriveRemoteLock(apiClientMock, driveFileMock);
        setupLockValue("");
        long timestamp = System.currentTimeMillis();
        setupLockUpdate(null);

        target.get();

        assertTrue(target.hasRemoteLock);
    }

    @Test
    public void claimAlreadyOwned() throws Exception
    {
        target = new DriveRemoteLock(apiClientMock, driveFileMock);
        long timestamp = System.currentTimeMillis();
        setupLockValue("abc " + timestamp);
        setupLockUpdate(null);

        target.get();

        assertTrue(target.hasRemoteLock);
    }

    private ChangeListener fileMockListener;

    @Test
    public void claimNotifyFreed() throws Exception
    {
        doAnswer(new Answer<Void>() {public Void answer(InvocationOnMock invocation) throws Throwable
        {
            fileMockListener = invocation.getArgumentAt(1, ChangeListener.class);
            return null;
        }}).when(driveFileMock).addChangeListener(eq(apiClientMock), any(ChangeListener.class));

        target = new DriveRemoteLock(apiClientMock, driveFileMock);
        long timestamp = System.currentTimeMillis();

        setupLockValue("notmine " + timestamp);

        final Object lock = new Object();
        new Thread(new Runnable() {public void run()
        {
            synchronized (lock)
            {
                lock.notify();
            }

            try{target.get();}catch(Exception e){return;}

            synchronized (lock)
            {
                lock.notify();
            }
        }}, "getThread").start();

        synchronized (lock)
        {
            lock.wait();
        }
        Thread.sleep(100);
        assertFalse(target.hasRemoteLock);

        setupLockValue("");
        setupLockUpdate(null);
        synchronized (lock)
        {
            fileMockListener.onChange(null);

            lock.wait(1000);
        }

        assertTrue(target.hasRemoteLock);
    }

    private boolean status = false;

    @Test
    public void noDeadlockWhenNotWaitingForLock() throws InterruptedException
    {
        doAnswer(new Answer<Void>() {public Void answer(InvocationOnMock invocation) throws Throwable
        {
            fileMockListener = invocation.getArgumentAt(1, ChangeListener.class);
            return null;
        }}).when(driveFileMock).addChangeListener(eq(apiClientMock), any(ChangeListener.class));

        target = new DriveRemoteLock(apiClientMock, driveFileMock);
        status = false;
        final Object lock = new Object();
        new Thread(new Runnable() {public void run()
        {
            fileMockListener.onChange(null);

            synchronized (lock)
            {
                lock.notify();
            }

            status = true;
        }}).start();

        synchronized (lock)
        {
            lock.wait(1000);
        }

        assertTrue(status);
    }

    private class ShortTimeoutRemoteLock extends DriveRemoteLock
    {
        public ShortTimeoutRemoteLock(GoogleApiClient apiClient, DriveFile file)
        {
            super(apiClient, file);
            timeout = 500;
        }
    }

    @Test
    public void claimWaitExpired() throws Exception
    {
        target = new ShortTimeoutRemoteLock(apiClientMock, driveFileMock);
        long timestamp = System.currentTimeMillis();

        setupLockValue("notmine " + timestamp);

        final Object lock = new Object();
        new Thread(new Runnable() {public void run()
        {
            synchronized (lock)
            {
                lock.notify();
            }

            try{target.get();}catch(Exception e){return;}

            synchronized (lock)
            {
                lock.notify();
            }
        }}, "getThread").start();

        synchronized (lock)
        {
            lock.wait();
        }
        Thread.sleep(100);
        assertFalse(target.hasRemoteLock);

        setupLockUpdate(null);
        synchronized (lock)
        {
            lock.wait(1000);
        }

        assertTrue(target.hasRemoteLock);
    }

    @Test
    public void claimFailed()
    {
        target = new DriveRemoteLock(apiClientMock, driveFileMock);
        setupLockValue("");
        setupLockUpdate("");

        boolean thrown = false;
        try
        {
            target.get();
        }
        catch(Exception e){
            thrown = true;
        }

        assertTrue(thrown);
        assertFalse(target.hasRemoteLock);
    }

    @Test
    public void releaseNotOwned() throws Exception
    {
        target = new DriveRemoteLock(apiClientMock, driveFileMock);
        setupLockValue("");
        assertFalse(target.hasRemoteLock);

        target.release();

        assertFalse(target.hasRemoteLock);
        assertEquals(null, lastValue);
    }

    @Test
    public void release() throws Exception
    {
        target = new DriveRemoteLock(apiClientMock, driveFileMock);
        long current = System.currentTimeMillis();
        setupLockValue("abc " + current);
        target.check();
        assertTrue(target.hasRemoteLock);

        setupLockUpdate(null);
        target.release();

        assertFalse(target.hasRemoteLock);
        assertEquals("", lastValue);
    }

    @Test
    public void releaseFailed() throws Exception
    {
        target = new DriveRemoteLock(apiClientMock, driveFileMock);
        long current = System.currentTimeMillis();
        String value = "abc " + current;
        setupLockValue(value);
        target.check();
        assertTrue(target.hasRemoteLock);

        setupLockUpdate(value);

        boolean thrown = false;
        try
        {
            target.release();
        }
        catch(DriveRemoteLock.FailedToReleaseLockException e){
            thrown = true;
        }

        assertTrue(target.hasRemoteLock);
        assertTrue(thrown);
    }

    @Test
    public void cleanup()
    {
        target = new DriveRemoteLock(apiClientMock, driveFileMock);
        long current = System.currentTimeMillis();
        setupLockValue("abc " + current);
        target.check();
        assertTrue(target.hasRemoteLock);

        setupLockUpdate(null);
        target.cleanUp();

        assertFalse(target.hasRemoteLock);
        assertEquals("", lastValue);
    }
}
