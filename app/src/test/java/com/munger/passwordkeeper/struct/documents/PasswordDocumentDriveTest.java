package com.munger.passwordkeeper.struct.documents;

import android.util.Log;

import com.google.api.client.util.DateTime;
import com.munger.passwordkeeper.HelperNoInst;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.helpers.DriveHelper;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import com.munger.passwordkeeper.struct.Settings;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestName;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.model.Statement;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Created by codymunger on 9/30/19.
 */
public class PasswordDocumentDriveTest
{
    public class SettingsDer extends Settings
    {
        @Override
        public void setPreferences() {

        }

        @Override
        public DateTime getLastCloudUpdate()
        {
            return lastCloudUpdate;
        }

        public DateTime lastCloudUpdate = new DateTime(System.currentTimeMillis() - 1000);
        @Override
        public void setLastCloudUpdate(DateTime value) {
            lastCloudUpdate = value;
        }
    }

    public class MainStateDer extends MainState
    {
        @Override
        protected DriveHelper createDriveHelper()
        {
            if (driveHelper == null)
                driveHelper = new DriveHelperDer();

            return driveHelper;
        }

        @Override
        protected void setupPreferences()
        {
            settings = new SettingsDer();
        }
    }

    public class DriveHelperDer extends DriveHelper
    {
        public PasswordDocumentFileDer doc;

        public void connectEmptyDocument()
        {
            connected = true;
            synchronized (lock)
            {
                lock.notify();
            }
        }

        @Override
        public void connect()
        {
            try
            {
                doc = new PasswordDocumentFileDer("remote", "pass");
                doc.delete();
                doc.playSubHistory(tmp.history);
                doc.save();

            } catch (Exception e){}

            connected = true;

            synchronized (lock)
            {
                lock.notify();
            }
        }

        private String fileId = "testId";

        @Override
        public String getOrCreateFile(String name) throws IOException
        {
            return fileId;
        }

        public DateTime lastModified = new DateTime(System.currentTimeMillis());
        public boolean isTrashed = false;

        @Override
        public Meta getMetadata(String fileId) throws IOException
        {
            getMetadataInitiated++;

            if (failOnGetMetadata)
                throw new IOException("metadata force fail");

            assertSame(fileId, this.fileId);
            Meta ret = new Meta();
            ret.modified = lastModified;
            ret.size = new File(doc.getHistoryFilePath()).length();
            ret.trashed = isTrashed;

            return ret;
        }

        @Override
        public InputStream getRemoteFile(String fileId) throws IOException
        {
            getRemoteInitiated ++;

            if (failOnGetRemote)
                throw new IOException("force remote get fail");

            File f = new File(doc.getHistoryFilePath());
            InputStream fis = new FileInputStream(f);
            return fis;
        }

        public int updateRemoteCalled = 0;
        public int updateRemoteInitiated = 0;
        public boolean failOnUpdateRemote = false;
        public boolean failOnGetMetadata = false;
        public int getMetadataInitiated = 0;
        public boolean failOnGetRemote = false;
        public int getRemoteInitiated = 0;

        @Override
        public DateTime updateRemoteByPath(String fileId, String path) throws IOException
        {
            System.out.println("save remote called");

            synchronized (docLock)
            {
                updateRemoteInitiated++;
                docLock.notify();
            }

            if (failOnUpdateRemote)
                throw new IOException("failed to upload file");

            File f = new File(path);
            long sz = f.length();
            InputStream ins = new FileInputStream(f);
            DataInputStream dis = new DataInputStream(ins);
            PasswordDocumentStream pds = new PasswordDocumentStream(dis, sz);
            pds.setPassword("pass");
            try {
                pds.load(false);

                if (doc == null)
                {
                    doc = new PasswordDocumentFileDer("remote", "pass");
                }

                doc.delete();
                doc.playSubHistory(pds.history);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ins.close();

            synchronized (docLock)
            {
                updateRemoteCalled++;
                lastModified = new DateTime(System.currentTimeMillis());
                docLock.notify();
            }

            return lastModified;
        }
    }

    public Object docLock = new Object();
    public class PasswordDocumentFileDer extends PasswordDocumentFile
    {
        public PasswordDocumentFileDer(String name, String password) {
            super(name, password);
        }

        public int saveCalled = 0;

        @Override
        public void save() throws Exception
        {
            super.save();

            synchronized (docLock)
            {
                System.out.println("save " + name + " called");
                saveCalled++;
                docLock.notify();
            }
        }
    }


    public MainStateDer mainState;
    public DriveHelperDer driveHelper;
    public HelperNoInst.PasswordDocumentImpl tmp;

    @ClassRule
    public static TestRule timeout = new DisableOnDebug(new TestName());

    @Rule
    public TestRule selectiveBefore = new TestRule() { public Statement apply(final Statement base, final Description description)
    {
        if (!(base instanceof InvokeMethod))
            return base;

        return new Statement() {public void evaluate() throws Throwable
        {
            if (!description.getMethodName().equals("createNewRemote"))
                before();

            base.evaluate();
        }};
    }};

    public void before()
    {
        tmp = HelperNoInst.generateDocument(5, 5);

        mainState = new MainStateDer();
        MainState.setInstance(mainState);
        mainState.setupPreferences();
        mainState.createDriveHelper();
        driveHelper = (DriveHelperDer) mainState.driveHelper;
        driveHelper.connect();
    }

    protected PasswordDocumentFileDer local;
    protected PasswordDocumentDrive target;


    public void load(String pass) throws Exception
    {
        local = new PasswordDocumentFileDer("test2", pass);
        load();
    }

    @Test
    public void load() throws Exception
    {
        //load two identical files into memory
        if (local == null)
            local = new PasswordDocumentFileDer("test2", "pass");

        local.delete();
        local.playSubHistory(tmp.history);
        local.save();

        Thread.sleep(100);
        target = new PasswordDocumentDrive(local, "test");
        local.load(false);

        //load function triggers an async load function in the drive document
        Thread.sleep(100);

        //ensure neither update method was called.
        assertSame(1, local.saveCalled);
        assertSame(0, driveHelper.updateRemoteCalled);
        assertTrue(local.history.equals(driveHelper.doc.history));
    }

    private static interface Condition
    {
        boolean satisfied();
    }

    private void doUpdate(PasswordDocument target) throws Exception
    {
        doUpdate(target, 1);
    }

    private void doUpdate(PasswordDocument target, int index) throws Exception
    {
        PasswordDetails dets = target.getDetails(index);
        PasswordDetailsPair pair = dets.getPair(index);
        dets.setName("foo" + index);
        dets.setLocation("foo" + (index + 1));
        pair.setKey("foo3" + (index + 2));
        pair.setValue("foo4" + (index + 3));
    }

    private void waitForCondition(Condition condition) throws Exception
    {
        synchronized (docLock)
        {
            int attempt = 0;
            while(!condition.satisfied())
            {
                if (((DisableOnDebug)timeout).isDebugging())
                    docLock.wait();
                else
                    docLock.wait(500);
                attempt++;

                if (attempt == 5)
                    break;
            }
        }

        Thread.sleep(100);
    }

    @Test
    public void localUpdate() throws Exception
    {
        load();
        doUpdate(local);
        local.save();
        waitForCondition(new Condition() {public boolean satisfied()
        {
            return !(local.saveCalled == 1 || driveHelper.doc.saveCalled == 0);
        }});

        Log.d("password", "test finished");
        System.out.println("test finished");
        assertTrue(local.saveCalled > 1);
        assertTrue(driveHelper.updateRemoteInitiated == 1);
        assertTrue(driveHelper.updateRemoteCalled == 1);
        assertTrue(local.history.equals(driveHelper.doc.history));
    }

    @Test
    public void localUpdateOfflineUpload() throws Exception
    {
        load();
        doUpdate(local);
        driveHelper.failOnUpdateRemote = true;
        local.save();
        waitForCondition(new Condition() {public boolean satisfied()
        {
            return !(local.saveCalled == 1 || driveHelper.updateRemoteInitiated == 0);
        }});

        Log.d("password", "test finished");
        System.out.println("test finished");
        assertTrue(local.saveCalled > 1);
        assertTrue(driveHelper.updateRemoteInitiated == 1);
        assertFalse(driveHelper.updateRemoteCalled == 1);
        assertFalse(local.history.equals(driveHelper.doc.history));
    }


    @Test
    public void localUpdateOffline() throws Exception
    {
        load();
        doUpdate(local);
        driveHelper.failOnGetMetadata = true;
        driveHelper.getMetadataInitiated = 0;
        local.saveCalled = 0;
        local.save();
        waitForCondition(new Condition() {public boolean satisfied()
        {
            return !(local.saveCalled == 0 || driveHelper.getMetadataInitiated == 0);
        }});

        Log.d("password", "test finished");
        System.out.println("test finished");
        assertTrue(local.saveCalled == 1);
        assertTrue(driveHelper.getMetadataInitiated == 1);
        assertFalse(driveHelper.updateRemoteCalled == 1);
        assertFalse(local.history.equals(driveHelper.doc.history));
    }

    @Test
    public void remoteUpdate() throws Exception
    {
        load();
        doUpdate(driveHelper.doc);
        driveHelper.doc.save();
        driveHelper.lastModified = new DateTime(System.currentTimeMillis());

        assertFalse(local.history.equals(driveHelper.doc.history));

        driveHelper.doc.saveCalled = 0;
        local.saveCalled = 0;
        driveHelper.updateRemoteInitiated = 0;
        driveHelper.updateRemoteCalled = 0;

        target.remoteUpdate();
        Thread.sleep(100);

        assertTrue(local.saveCalled > 0);
        assertTrue(driveHelper.updateRemoteInitiated == 0);
        assertTrue(driveHelper.doc.saveCalled == 0);
        assertTrue(local.history.equals(driveHelper.doc.history));
    }

    @Test
    public void localAndRemoteUpdate() throws Exception
    {
        load();
        doUpdate(driveHelper.doc, 0);
        driveHelper.doc.save();
        driveHelper.lastModified = new DateTime(System.currentTimeMillis());
        Thread.sleep(100);

        driveHelper.doc.saveCalled = 0;
        local.saveCalled = 0;
        driveHelper.updateRemoteInitiated = 0;
        driveHelper.updateRemoteCalled = 0;

        doUpdate(local, 1);
        local.save();

        Thread.sleep(250);

        assertTrue(local.saveCalled > 0);
        assertTrue(driveHelper.updateRemoteCalled > 0);
        assertTrue(local.history.equals(driveHelper.doc.history));
    }

    @Test
    public void mismatchedPassword() throws Exception
    {
        load("mismatch");

        Thread.sleep(100);
        assertTrue(target.getInitException() != null);
        assertFalse(local.getEncoder().equals(driveHelper.doc.getEncoder()));
    }

    @Test
    public void mismatchedPasswordReencode() throws Exception
    {
        mismatchedPassword();

        target.initException = null;
        local.saveCalled = 0;
        driveHelper.updateRemoteCalled = 0;

        target.setPassword("pass");
        Thread.sleep(100);

        assertTrue(local.getEncoder().equals(driveHelper.doc.getEncoder()));
        assertTrue(local.saveCalled > 0);
        assertTrue(driveHelper.updateRemoteCalled > 0);
    }

    @Test
    public void remotelyTrashed() throws Exception
    {
        driveHelper.isTrashed = true;
        load();
        Thread.sleep(100);

        assertTrue(target.getInitException() != null);
    }

    @Test
    public void remotelyDelete() throws Exception
    {
        load();
        Thread.sleep(100);

        target.delete();
        Thread.sleep(100);

        assertTrue(target.getSource() == null);
    }

    @Test
    public void createNewRemote() throws Exception
    {
        mainState = new MainStateDer();
        MainState.setInstance(mainState);
        mainState.setupPreferences();
        mainState.createDriveHelper();
        driveHelper = (DriveHelperDer) mainState.driveHelper;
        tmp = HelperNoInst.generateDocument(0,0);

        local = new PasswordDocumentFileDer("test2", "pass");
        local.delete();
        local.save();

        driveHelper.updateRemoteCalled = 0;
        local.saveCalled = 0;

        driveHelper.connect();

        Thread.sleep(100);

        assertTrue(driveHelper.updateRemoteCalled > 0);
        assertTrue(local.saveCalled == 0);
    }
}
