package com.munger.passwordkeeper.struct.documents;

import com.munger.passwordkeeper.HelperNoInst;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;


import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.StringReader;

import static org.junit.Assert.*;

/**
 * Created by codymunger on 11/22/16.
 */

public class PasswordDocumentTest
{
    @Test
    public void constructors()
    {
        HelperNoInst.PasswordDocumentImpl doc = new HelperNoInst.PasswordDocumentImpl();
    }

    private int fillDetails(PasswordDetails dets)
    {
        dets.setName("one");
        dets.setLocation("two");
        PasswordDetailsPair pair = dets.addEmptyPair();
        pair.setKey("three");
        pair.setValue("four");
        PasswordDetailsPair pair2 = dets.addEmptyPair();
        pair2.setKey("three");
        pair2.setValue("four");

        return 8;
    }

    @Test
    public void historyUpdatesFromEmptyCorrectly() throws PasswordDocumentHistory.HistoryPlaybackException
    {
        HelperNoInst.PasswordDocumentImpl doc = new HelperNoInst.PasswordDocumentImpl();
        PasswordDetails dets = new PasswordDetails();
        doc.addDetails(dets);
        int eventCount = 1;

        Assert.assertEquals(1, doc.count());
        Assert.assertEquals(eventCount, doc.history.count());

        eventCount += fillDetails(dets);

        Assert.assertEquals(1, doc.count());
        Assert.assertEquals(eventCount, doc.history.count());

        doc.replaceDetails(dets);
        Assert.assertEquals(1, doc.count());
        Assert.assertEquals(eventCount, doc.history.count());

        doc.removeDetails(dets);
        eventCount++;
        Assert.assertEquals(0, doc.count());
        Assert.assertEquals(eventCount, doc.history.count());

        //modifying a removed detail will not effect the document history
        PasswordDetailsPair pair = dets.addEmptyPair();

        Assert.assertEquals(0, doc.count());
        Assert.assertEquals(eventCount, doc.history.count());
    }

    @Test
    public void historyUpdatesFromSubHistoryCorrectly() throws PasswordDocumentHistory.HistoryPlaybackException
    {
        //try once with empty history
        HelperNoInst.PasswordDocumentImpl doc = new HelperNoInst.PasswordDocumentImpl();
        PasswordDetails dets = new PasswordDetails();
        int eventCount = 0;

        Assert.assertEquals(0, doc.count());
        Assert.assertEquals(0, doc.history.count());

        eventCount += fillDetails(dets);

        doc.addDetails(dets);
        eventCount++;
        Assert.assertEquals(1, doc.count());
        Assert.assertEquals(eventCount, doc.history.count());

        //try again
        int oldEventCount = eventCount;
        dets = new PasswordDetails();

        Assert.assertEquals(1, doc.count());
        Assert.assertEquals(oldEventCount, doc.history.count());

        eventCount += fillDetails(dets);

        doc.addDetails(dets);
        eventCount++;
        Assert.assertEquals(2, doc.count());
        Assert.assertEquals(eventCount, doc.history.count());
    }

    @Test
    public void historyUpdatesFromReplacementCorrectly() throws PasswordDocumentHistory.HistoryPlaybackException
    {
        HelperNoInst.PasswordDocumentImpl doc = new HelperNoInst.PasswordDocumentImpl();
        PasswordDetails dets = new PasswordDetails();
        dets.setName("foo");
        dets.setLocation("bar");
        doc.addDetails(dets);
        int eventCount = 3;

        Assert.assertEquals(1, doc.count());
        Assert.assertEquals(eventCount, doc.history.count());

        PasswordDetails copy = dets.copy();
        int oldEventCount = eventCount;
        eventCount += fillDetails(copy);

        Assert.assertEquals(1, doc.count());
        Assert.assertEquals(oldEventCount, doc.history.count());

        doc.replaceDetails(copy);
        Assert.assertEquals(1, doc.count());
        Assert.assertEquals(eventCount, doc.history.count());
    }

    @Test
    public void detailsToFromString() throws Exception
    {
        PasswordDocument doc = HelperNoInst.generateDocument(5, 5);
        String detout = doc.detailsToString();

        HelperNoInst.PasswordDocumentImpl doc2 = new HelperNoInst.PasswordDocumentImpl();
        StringReader strread = new StringReader(detout);
        BufferedReader bufread = new BufferedReader(strread);
        doc2.detailsFromString(bufread);
        bufread.close();  strread.close();

        assertTrue(doc.equals(doc2));
    }
    @Test
    public void detailsToFromCipher() throws Exception
    {
        PasswordDocument doc = HelperNoInst.generateDocument(5, 5);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        doc.detailsToEncryptedString(dos);
        byte[] output = baos.toByteArray();
        dos.close();  baos.close();

        HelperNoInst.PasswordDocumentImpl doc2 = new HelperNoInst.PasswordDocumentImpl();
        ByteArrayInputStream bais = new ByteArrayInputStream(output);
        DataInputStream dis = new DataInputStream(bais);
        doc2.detailsFromEncryptedString(dis);
        dis.close();  bais.close();

        assertTrue(doc.equals(doc2));

        HelperNoInst.PasswordDocumentImpl doc3 = new HelperNoInst.PasswordDocumentImpl(HelperNoInst.DEFAULT_NAME, "wrong password");
        bais = new ByteArrayInputStream(output);
        dis = new DataInputStream(bais);

        boolean exception = false;
        try
        {
            doc3.detailsFromEncryptedString(dis);
        }
        catch(Exception e){
            exception = true;
        }

        assertTrue(exception);

        dis.close();  bais.close();
    }

    @Test
    public void historyToFromString() throws Exception
    {
        PasswordDocument doc = HelperNoInst.generateDocument(5, 5);
        String histout = doc.deltasToString();

        HelperNoInst.PasswordDocumentImpl doc2 = new HelperNoInst.PasswordDocumentImpl();
        doc2.deltasFromString(histout);

        assertTrue(doc.getHistory().equals(doc2.getHistory()));
        Assert.assertEquals(0, doc2.details.size());
    }

    @Test
    public void emptyHistoryToFromCipher() throws Exception
    {
        PasswordDocument doc = new HelperNoInst.PasswordDocumentImpl();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        doc.deltasToEncryptedString(dos);
        byte[] output = baos.toByteArray();

        HelperNoInst.PasswordDocumentImpl doc2 = new HelperNoInst.PasswordDocumentImpl();
        ByteArrayInputStream bais = new ByteArrayInputStream(output);
        DataInputStream dis = new DataInputStream(bais);
        doc2.deltasFromEncryptedString(dis, output.length);
        dis.close();  bais.close();

        assertTrue(doc.getHistory().equals(doc2.getHistory()));
        Assert.assertEquals(0, doc2.details.size());
    }

    @Test
    public void historyToFromCipher() throws Exception
    {
        PasswordDocument doc = HelperNoInst.generateDocument(5, 5);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        doc.deltasToEncryptedString(dos);
        byte[] output = baos.toByteArray();

        HelperNoInst.PasswordDocumentImpl doc2 = new HelperNoInst.PasswordDocumentImpl();
        ByteArrayInputStream bais = new ByteArrayInputStream(output);
        DataInputStream dis = new DataInputStream(bais);
        doc2.deltasFromEncryptedString(dis, output.length);
        dis.close();  bais.close();

        assertTrue(doc.getHistory().equals(doc2.getHistory()));
        Assert.assertEquals(0, doc2.details.size());

        boolean exception = false;

        doc2 = new HelperNoInst.PasswordDocumentImpl(HelperNoInst.DEFAULT_NAME, "wrong password");
        bais = new ByteArrayInputStream(output);
        dis = new DataInputStream(bais);

        try
        {
            doc2.deltasFromEncryptedString(dis, output.length);
        }
        catch(Exception e){
            exception = true;
        }

        dis.close(); bais.close();

        assertTrue(exception);
    }

    @Test
    public void historyEvents() throws Exception
    {
        PasswordDocument doc = HelperNoInst.generateDocument(10, 10);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        doc.deltasToEncryptedString(dos);

        class Results
        {
            public int progressCount = 0;
            public float lastProgress = 0;
            public boolean historyLoaded = false;
        }

        final Results results = new Results();
        final Object lock = new Object();

        final HelperNoInst.PasswordDocumentImpl doc2 = new HelperNoInst.PasswordDocumentImpl();
        doc2.addLoadEvents(new PasswordDocument.ILoadEvents() {
            @Override
            public void detailsLoaded() {}

            @Override
            public void historyLoaded()
            {
                results.historyLoaded = true;

                synchronized (lock)
                {
                    lock.notify();
                }
            }

            @Override
            public void historyProgress(float progress)
            {
                results.progressCount++;

                assertTrue(progress >= results.lastProgress);
                results.lastProgress = progress;
            }
        });

        final byte[] bytes = baos.toByteArray();
        final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        final DataInputStream dis = new DataInputStream(bais);

        Thread t = new Thread(new Runnable() {public void run()
        {
            try
            {
                doc2.deltasFromEncryptedString(dis, bytes.length);
            }
            catch(Exception e){
                fail("failed to decrypt output");
            }
        }});
        t.start();

        synchronized (lock)
        {
            lock.wait(2000);
        }

        bais.close();
        dis.close();

        assertTrue(results.progressCount > 0);
        assertTrue(results.historyLoaded);
        assertTrue(1.0f == results.lastProgress);
    }

    @Test
    public void historyLoadedAwaiter() throws Exception
    {
        PasswordDocument doc = HelperNoInst.generateDocument(3, 3);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        doc.deltasToEncryptedString(dos);
        final byte[] output = baos.toByteArray();
        final HelperNoInst.PasswordDocumentImpl doc2 = new HelperNoInst.PasswordDocumentImpl();
        final Object lock = new Object();

        class Results
        {
            public boolean finished = false;
        }
        final Results results = new Results();

        Thread t1 = new Thread(new Runnable() {public void run()
        {
            try
            {
                ByteArrayInputStream bais = new ByteArrayInputStream(output);
                DataInputStream dis = new DataInputStream(bais);
                doc2.deltasFromEncryptedString(dis, output.length);
                dis.close();  bais.close();
            }
            catch(Exception e){
                fail("failed to decrypt history output");
            }
        }});
        t1.run();

        Thread t2 = new Thread(new Runnable() {public void run()
        {
            doc2.awaitHistoryLoaded();
            results.finished = true;

            synchronized (lock){
                lock.notify();
            }
        }});
        t2.start();

        synchronized (lock){
            lock.wait(2000);
        }

        assertTrue(doc2.historyLoaded);
        assertTrue(results.finished);
    }

    @Test
    public void addRemoveDetails() throws Exception
    {
        HelperNoInst.PasswordDocumentImpl doc = new HelperNoInst.PasswordDocumentImpl();
        PasswordDetails dets1 = new PasswordDetails();
        dets1.setName("name");
        doc.addDetails(dets1);

        Assert.assertEquals(1, doc.count());
        assertTrue(!dets1.diff(doc.getDetails(0)));
        assertTrue(!dets1.diff(doc.getDetails(dets1.getId())));

        PasswordDetails dets2 = new PasswordDetails();
        dets2.setName("name2");
        doc.addDetails(dets2);

        Assert.assertEquals(2, doc.count());
        assertTrue(!dets1.diff(doc.getDetails(0)));
        assertTrue(!dets1.diff(doc.getDetails(dets1.getId())));
        assertTrue(!dets2.diff(doc.getDetails(1)));
        assertTrue(!dets2.diff(doc.getDetails(dets2.getId())));

        doc.removeDetails(dets1);
        Assert.assertEquals(1, doc.count());
        assertTrue(!dets2.diff(doc.getDetails(0)));
        assertTrue(!dets2.diff(doc.getDetails(dets2.getId())));
    }

    @Test
    public void password() throws Exception
    {
        String password1 = "password";
        String password2 = "another password";
        PasswordDocument doc1 = HelperNoInst.generateDocument(2, 2);

        doc1.setPassword(password1);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        doc1.detailsToEncryptedString(dos);
        byte[] cipher1 = baos.toByteArray();
        dos.close(); baos.close();

        doc1.setPassword(password2);
        baos = new ByteArrayOutputStream();
        dos = new DataOutputStream(baos);
        doc1.detailsToEncryptedString(dos);
        byte[] cipher2 = baos.toByteArray();
        dos.close(); baos.close();

        assertNotEquals(new String(cipher1), new String(cipher2));

        boolean exception = false;
        ByteArrayInputStream bais = new ByteArrayInputStream(cipher2);
        DataInputStream dis = new DataInputStream(bais);
        try
        {
            doc1.setPassword(password1);
            doc1.detailsFromEncryptedString(dis);
        }
        catch(Exception e){
            exception = true;
        }
        bais.close();  dis.close();
        assertTrue(exception);


        exception = false;
        bais = new ByteArrayInputStream(cipher1);
        dis = new DataInputStream(bais);
        try
        {
            doc1.setPassword(password2);
            doc1.detailsFromEncryptedString(dis);
        }
        catch(Exception e){
            exception = true;
        }
        bais.close();  dis.close();
        assertTrue(exception);


        exception = false;
        bais = new ByteArrayInputStream(cipher1);
        dis = new DataInputStream(bais);
        try
        {
            doc1.setPassword(password1);
            doc1.detailsFromEncryptedString(dis);
        }
        catch(Exception e){
            exception = true;
        }
        bais.close();  dis.close();
        assertFalse(exception);


        exception = false;
        bais = new ByteArrayInputStream(cipher2);
        dis = new DataInputStream(bais);
        try
        {
            doc1.setPassword(password2);
            doc1.detailsFromEncryptedString(dis);
        }
        catch(Exception e){
            exception = true;
        }
        bais.close();  dis.close();
        assertFalse(exception);

    }

    @Test
    public void documentAppend()
    {

    }
}
