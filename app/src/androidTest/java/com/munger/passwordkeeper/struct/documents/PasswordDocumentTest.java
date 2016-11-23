package com.munger.passwordkeeper.struct.documents;

import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;

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
    private class PasswordDocumentImpl extends PasswordDocument
    {
        public PasswordDocumentImpl(String pass)
        {
            super(pass);
        }

        public PasswordDocumentImpl(String name, String pass)
        {
            super(name, pass);
        }

        public void save() throws Exception
        {

        }

        public void load(boolean force) throws Exception
        {

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

    private final String DEFAULT_NAME = "name";
    private final String DEFAULT_PASSWORD = "pass";

    @Test
    public void constructors()
    {
        PasswordDocumentImpl doc = new PasswordDocumentImpl(DEFAULT_NAME, DEFAULT_PASSWORD);
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
        PasswordDocumentImpl doc = new PasswordDocumentImpl(DEFAULT_NAME, DEFAULT_PASSWORD);
        PasswordDetails dets = new PasswordDetails();
        doc.addDetails(dets);
        int eventCount = 1;

        assertEquals(1, doc.count());
        assertEquals(eventCount, doc.history.count());

        eventCount += fillDetails(dets);

        assertEquals(1, doc.count());
        assertEquals(eventCount, doc.history.count());

        doc.replaceDetails(dets);
        assertEquals(1, doc.count());
        assertEquals(eventCount, doc.history.count());

        doc.removeDetails(dets);
        eventCount++;
        assertEquals(0, doc.count());
        assertEquals(eventCount, doc.history.count());

        //modifying a removed detail will not effect the document history
        PasswordDetailsPair pair = dets.addEmptyPair();

        assertEquals(0, doc.count());
        assertEquals(eventCount, doc.history.count());
    }

    @Test
    public void historyUpdatesFromSubHistoryCorrectly() throws PasswordDocumentHistory.HistoryPlaybackException
    {
        //try once with empty history
        PasswordDocumentImpl doc = new PasswordDocumentImpl(DEFAULT_NAME, DEFAULT_PASSWORD);
        PasswordDetails dets = new PasswordDetails();
        int eventCount = 0;

        assertEquals(0, doc.count());
        assertEquals(0, doc.history.count());

        eventCount += fillDetails(dets);

        doc.addDetails(dets);
        eventCount++;
        assertEquals(1, doc.count());
        assertEquals(eventCount, doc.history.count());

        //try again
        int oldEventCount = eventCount;
        dets = new PasswordDetails();

        assertEquals(1, doc.count());
        assertEquals(oldEventCount, doc.history.count());

        eventCount += fillDetails(dets);

        doc.addDetails(dets);
        eventCount++;
        assertEquals(2, doc.count());
        assertEquals(eventCount, doc.history.count());
    }

    @Test
    public void historyUpdatesFromReplacementCorrectly() throws PasswordDocumentHistory.HistoryPlaybackException
    {
        PasswordDocumentImpl doc = new PasswordDocumentImpl(DEFAULT_NAME, DEFAULT_PASSWORD);
        PasswordDetails dets = new PasswordDetails();
        dets.setName("foo");
        dets.setLocation("bar");
        doc.addDetails(dets);
        int eventCount = 3;

        assertEquals(1, doc.count());
        assertEquals(eventCount, doc.history.count());

        PasswordDetails copy = dets.copy();
        int oldEventCount = eventCount;
        eventCount += fillDetails(copy);

        assertEquals(1, doc.count());
        assertEquals(oldEventCount, doc.history.count());

        doc.replaceDetails(copy);
        assertEquals(1, doc.count());
        assertEquals(eventCount, doc.history.count());
    }

    private PasswordDocumentImpl generateDocument(int detSz, int pairSz)
    {
        PasswordDocumentImpl doc = new PasswordDocumentImpl(DEFAULT_NAME, DEFAULT_PASSWORD);
        for (int i = 0; i < detSz; i++)
        {
            PasswordDetails dets = new PasswordDetails();
            dets.setName("name" + i);
            dets.setLocation("loc" + i);

            for (int j = 0; j < pairSz; j++)
            {
                PasswordDetailsPair pair = dets.addEmptyPair();
                pair.setKey("key" + i + j);
                pair.setValue("val" + i + j);
            }

            try {
                doc.addDetails(dets);
            } catch(Exception e){}
        }

        return doc;
    }

    @Test
    public void detailsToFromString() throws Exception
    {
        PasswordDocument doc = generateDocument(5, 5);
        String detout = doc.detailsToString();

        PasswordDocumentImpl doc2 = new PasswordDocumentImpl(DEFAULT_NAME, DEFAULT_PASSWORD);
        StringReader strread = new StringReader(detout);
        BufferedReader bufread = new BufferedReader(strread);
        doc2.detailsFromString(bufread);
        bufread.close();  strread.close();

        assertTrue(doc.equals(doc2));
    }
    @Test
    public void detailsToFromCipher() throws Exception
    {
        PasswordDocument doc = generateDocument(5, 5);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        doc.detailsToEncryptedString(dos);
        byte[] output = baos.toByteArray();
        dos.close();  baos.close();

        PasswordDocumentImpl doc2 = new PasswordDocumentImpl(DEFAULT_NAME, DEFAULT_PASSWORD);
        ByteArrayInputStream bais = new ByteArrayInputStream(output);
        DataInputStream dis = new DataInputStream(bais);
        doc2.detailsFromEncryptedString(dis);
        dis.close();  bais.close();

        assertTrue(doc.equals(doc2));

        PasswordDocumentImpl doc3 = new PasswordDocumentImpl(DEFAULT_NAME, "wrong password");
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
        PasswordDocument doc = generateDocument(5, 5);
        String histout = doc.deltasToString();

        PasswordDocumentImpl doc2 = new PasswordDocumentImpl(DEFAULT_NAME, DEFAULT_PASSWORD);
        StringReader strread = new StringReader(histout);
        BufferedReader bufread = new BufferedReader(strread);
        doc2.deltasFromString(bufread);
        bufread.close();  strread.close();

        assertTrue(doc.getHistory().equals(doc2.getHistory()));
        assertEquals(0, doc2.details.size());
    }

    @Test
    public void historyToFromCipher() throws Exception
    {
        PasswordDocument doc = generateDocument(5, 5);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        doc.deltasToEncryptedString(dos);
        byte[] output = baos.toByteArray();

        PasswordDocumentImpl doc2 = new PasswordDocumentImpl(DEFAULT_NAME, DEFAULT_PASSWORD);
        ByteArrayInputStream bais = new ByteArrayInputStream(output);
        DataInputStream dis = new DataInputStream(bais);
        doc2.deltasFromEncryptedString(dis);
        dis.close();  bais.close();

        assertTrue(doc.getHistory().equals(doc2.getHistory()));
        assertEquals(0, doc2.details.size());

        boolean exception = false;

        doc2 = new PasswordDocumentImpl(DEFAULT_NAME, "wrong password");
        bais = new ByteArrayInputStream(output);
        dis = new DataInputStream(bais);

        try
        {
            doc2.deltasFromEncryptedString(dis);
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
        PasswordDocument doc = generateDocument(10, 10);
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

        final PasswordDocumentImpl doc2 = new PasswordDocumentImpl(DEFAULT_NAME, DEFAULT_PASSWORD);
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

        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final DataInputStream dis = new DataInputStream(bais);

        Thread t = new Thread(new Runnable() {public void run()
        {
            try
            {
                doc2.deltasFromEncryptedString(dis);
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
        PasswordDocument doc = generateDocument(3, 3);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        doc.deltasToEncryptedString(dos);
        final byte[] output = baos.toByteArray();
        final PasswordDocumentImpl doc2 = new PasswordDocumentImpl(DEFAULT_NAME, DEFAULT_PASSWORD);
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
                doc2.deltasFromEncryptedString(dis);
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
        PasswordDocumentImpl doc = new PasswordDocumentImpl(DEFAULT_NAME, DEFAULT_PASSWORD);
        PasswordDetails dets1 = new PasswordDetails();
        dets1.setName("name");
        doc.addDetails(dets1);

        assertEquals(1, doc.count());
        assertTrue(!dets1.diff(doc.getDetails(0)));
        assertTrue(!dets1.diff(doc.getDetails(dets1.getId())));

        PasswordDetails dets2 = new PasswordDetails();
        dets2.setName("name2");
        doc.addDetails(dets2);

        assertEquals(2, doc.count());
        assertTrue(!dets1.diff(doc.getDetails(0)));
        assertTrue(!dets1.diff(doc.getDetails(dets1.getId())));
        assertTrue(!dets2.diff(doc.getDetails(1)));
        assertTrue(!dets2.diff(doc.getDetails(dets2.getId())));

        doc.removeDetails(dets1);
        assertEquals(1, doc.count());
        assertTrue(!dets2.diff(doc.getDetails(0)));
        assertTrue(!dets2.diff(doc.getDetails(dets2.getId())));
    }

    @Test
    public void password() throws Exception
    {
        String password1 = "password";
        String password2 = "another password";
        PasswordDocument doc1 = generateDocument(2, 2);

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
