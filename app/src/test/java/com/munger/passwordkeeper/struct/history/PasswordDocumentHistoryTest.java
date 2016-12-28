package com.munger.passwordkeeper.struct.history;

/**
 * Created by codymunger on 11/24/16.
 */

import com.munger.passwordkeeper.HelperNoInst;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class PasswordDocumentHistoryTest
{
    private HistoryEventFactory eventFactory;

    @Before
    public void before()
    {
        eventFactory = new HistoryEventFactory();
    }

    @Test
    public void constructors()
    {
        PasswordDocumentHistory hist = new PasswordDocumentHistory();
    }

    @Test
    public void manipulation()
    {
        PasswordDocumentHistory hist = new PasswordDocumentHistory();
        HistoryEvent event1 = eventFactory.buildEvent(HistoryEventFactory.Types.DETAILS_CREATE);
        event1.id = "id1";
        hist.addEvent(event1);
        assertEquals(1, hist.count());
        assertEquals(2, hist.getSequenceCount());
        assertEquals(1, event1.sequenceId);

        HistoryEvent event2 = eventFactory.buildEvent(HistoryEventFactory.Types.DETAILS_UPDATE);
        event2.id = "id2";
        hist.addEvent(event2);
        assertEquals(2, hist.count());
        assertEquals(3, hist.getSequenceCount());
        assertEquals(2, event2.sequenceId);

        HistoryEvent event3 = eventFactory.buildEvent(HistoryEventFactory.Types.DETAILS_UPDATE);
        event3.id = "id3";
        hist.setSequenceCount(1000);
        hist.addEvent(event3);
        assertEquals(3, hist.count());
        assertEquals(1001, hist.getSequenceCount());
        assertEquals(1000, event3.sequenceId);

        HistoryEvent two = hist.getEvent(1);
        assertEquals(two.id, event2.id);
    }

    @Test
    public void clean()
    {
        PasswordDocumentHistory hist = new PasswordDocumentHistory();
        HistoryEvent event = eventFactory.buildEvent(HistoryEventFactory.Types.DETAILS_CREATE);
        event.id = "id1";
        hist.addEvent(event);
        event = eventFactory.buildEvent(HistoryEventFactory.Types.DETAILS_UPDATE);
        event.id = "id1";
        event.property = "prop1"; event.value = "value1";
        hist.addEvent(event);
        event = eventFactory.buildEvent(HistoryEventFactory.Types.DETAILS_UPDATE);
        event.id = "id1";
        event.property = "prop2"; event.value = "value1";
        hist.addEvent(event);

        HistoryPairEvent pevent = (HistoryPairEvent) eventFactory.buildEvent(HistoryEventFactory.Types.PAIR_CREATE);
        pevent.id = "id1";  pevent.pairid = "pid1";
        hist.addEvent(pevent);
        pevent = (HistoryPairEvent) eventFactory.buildEvent(HistoryEventFactory.Types.PAIR_UPDATE);
        pevent.id = "id1";  pevent.pairid = "pid1";
        pevent.property = "prop1"; pevent.value = "value1";
        hist.addEvent(pevent);
        pevent = (HistoryPairEvent) eventFactory.buildEvent(HistoryEventFactory.Types.PAIR_UPDATE);
        pevent.id = "id1";  pevent.pairid = "pid1";
        pevent.property = "prop1"; pevent.value = "value2";
        hist.addEvent(pevent);
        pevent = (HistoryPairEvent) eventFactory.buildEvent(HistoryEventFactory.Types.PAIR_UPDATE);
        pevent.id = "id1";  pevent.pairid = "pid1";
        pevent.property = "prop2"; pevent.value = "value1";
        hist.addEvent(pevent);

        int sz = hist.count();
        hist.clean();
        int newsz = hist.count();
        assertEquals(sz - 1, newsz);

        pevent = (HistoryPairEvent) eventFactory.buildEvent(HistoryEventFactory.Types.PAIR_CREATE);
        pevent.id = "id1";  pevent.pairid = "pid2";
        hist.addEvent(pevent);
        pevent = (HistoryPairEvent) eventFactory.buildEvent(HistoryEventFactory.Types.PAIR_DELETE);
        pevent.id = "id1";  pevent.pairid = "pid2";
        hist.addEvent(pevent);

        sz = hist.count();
        hist.clean();
        newsz = hist.count();
        assertEquals(sz - 1, newsz);

        pevent = (HistoryPairEvent) eventFactory.buildEvent(HistoryEventFactory.Types.PAIR_DELETE);
        pevent.id = "id1";  pevent.pairid = "pid1";
        hist.addEvent(pevent);

        sz = hist.count();
        hist.clean();
        newsz = hist.count();
        assertEquals(sz - 3, newsz);

        pevent = (HistoryPairEvent) eventFactory.buildEvent(HistoryEventFactory.Types.PAIR_CREATE);
        pevent.id = "id1";  pevent.pairid = "pid1";
        hist.addEvent(pevent);
        pevent = (HistoryPairEvent) eventFactory.buildEvent(HistoryEventFactory.Types.PAIR_UPDATE);
        pevent.id = "id1";  pevent.pairid = "pid1";
        pevent.property = "prop1"; pevent.value = "value1";
        hist.addEvent(pevent);
        pevent = (HistoryPairEvent) eventFactory.buildEvent(HistoryEventFactory.Types.PAIR_UPDATE);
        pevent.id = "id1";  pevent.pairid = "pid1";
        pevent.property = "prop1"; pevent.value = "value2";
        hist.addEvent(pevent);
        pevent = (HistoryPairEvent) eventFactory.buildEvent(HistoryEventFactory.Types.PAIR_UPDATE);
        pevent.id = "id1";  pevent.pairid = "pid1";
        pevent.property = "prop2"; pevent.value = "value1";
        hist.addEvent(pevent);

        event = eventFactory.buildEvent(HistoryEventFactory.Types.DETAILS_DELETE);
        event.id = "id1";
        hist.addEvent(event);
        sz = hist.count();
        hist.clean();
        assertEquals(1, hist.count());
    }

    @Test
    public void play() throws Exception
    {
        PasswordDocument doc1 = HelperNoInst.generateDocument(5, 5);
        HelperNoInst.PasswordDocumentImpl doc2 = new HelperNoInst.PasswordDocumentImpl();
        doc1.getHistory().playHistory(doc2);

        assertTrue(doc1.equals(doc2));
    }

    @Test
    public void toFromString() throws Exception
    {
        PasswordDocument doc1 = HelperNoInst.generateDocument(5, 5);
        String out = doc1.getHistory().toString();
        PasswordDocumentHistory hist = new PasswordDocumentHistory();
        hist.fromString(out);

        assertTrue(hist.equals(doc1.getHistory()));
    }

    @Test
    public void partToFromString() throws Exception
    {
        PasswordDocument doc1 = HelperNoInst.generateDocument(5, 5);
        PasswordDocumentHistory hist = doc1.getHistory();
        String out = hist.partToString(10, 10);
        PasswordDocumentHistory partHist = new PasswordDocumentHistory();
        partHist.partFromString(out);

        assertEquals(10, partHist.count());

        HistoryEvent event = eventFactory.buildEvent(HistoryEventFactory.Types.DETAILS_DELETE);
        event.id = doc1.getDetails(0).getId();
        hist.addEvent(event);
        hist.clean();
        out = hist.partToString(0, 10);
        partHist = new PasswordDocumentHistory();
        partHist.partFromString(out);
    }

    @Test
    public void cloneTest()
    {
        PasswordDocument doc1 = HelperNoInst.generateDocument(5, 5);
        String out = doc1.getHistory().toString();
        PasswordDocumentHistory hist = doc1.getHistory().clone();

        assertNotSame(hist, doc1.getHistory());
        assertTrue(hist.equals(doc1.getHistory()));
    }

    @Test
    public void noChangeMerge() throws Exception
    {
        PasswordDocument doc1 = HelperNoInst.generateDocument(2, 2);
        PasswordDocument doc2 = new HelperNoInst.PasswordDocumentImpl();
        doc1.getHistory().playHistory(doc2);

        PasswordDocumentHistory hist = doc1.getHistory().mergeHistory(doc2.getHistory());
        PasswordDocument doc3 = new HelperNoInst.PasswordDocumentImpl();
        hist.playHistory(doc3);

        assertEquals(2, doc3.count());
        PasswordDetails dets = doc3.getDetails(0);
        assertEquals(2, dets.count());
        dets = doc3.getDetails(1);
        assertEquals(2, dets.count());
    }

    @Test
    public void localSidedMerge() throws Exception
    {
        PasswordDocument doc1 = HelperNoInst.generateDocument(2, 2);
        PasswordDocument doc2 = new HelperNoInst.PasswordDocumentImpl();
        doc1.getHistory().playHistory(doc2);

        PasswordDetails dets = new PasswordDetails();
        dets.setName("foo");
        dets.setLocation("bar");
        doc1.addDetails(dets);


        PasswordDocumentHistory hist = doc1.getHistory().mergeHistory(doc2.getHistory());
        PasswordDocument doc3 = new HelperNoInst.PasswordDocumentImpl();
        hist.playHistory(doc3);

        assertEquals(3, doc3.count());
        dets = doc3.getDetails(0);
        assertEquals(2, dets.count());
        dets = doc3.getDetails(1);
        assertEquals(2, dets.count());
        dets = doc3.getDetails(2);
        assertEquals("foo", dets.getName());
    }

    @Test
    public void remoteSidedMerge() throws Exception
    {
        PasswordDocument doc1 = HelperNoInst.generateDocument(2, 2);
        PasswordDocument doc2 = new HelperNoInst.PasswordDocumentImpl();
        doc1.getHistory().playHistory(doc2);

        PasswordDetails dets = new PasswordDetails();
        dets.setName("foo");
        dets.setLocation("bar");
        doc2.addDetails(dets);


        PasswordDocumentHistory hist = doc1.getHistory().mergeHistory(doc2.getHistory());
        PasswordDocument doc3 = new HelperNoInst.PasswordDocumentImpl();
        hist.playHistory(doc3);

        assertEquals(3, doc3.count());
        dets = doc3.getDetails(0);
        assertEquals(2, dets.count());
        dets = doc3.getDetails(1);
        assertEquals(2, dets.count());
        dets = doc3.getDetails(2);
        assertEquals("foo", dets.getName());
    }

    @Test
    public void straightMerge() throws Exception
    {
        PasswordDocument doc1 = HelperNoInst.generateDocument(2, 2);
        PasswordDocument doc2 = new HelperNoInst.PasswordDocumentImpl();
        doc1.getHistory().playHistory(doc2);
        int oldSz = doc1.getHistory().count();

        PasswordDetails dets = new PasswordDetails();
        dets.setName("foo");
        dets.setLocation("bar");
        PasswordDetailsPair pair = dets.addEmptyPair();
        pair.setKey("foobar");
        doc1.addDetails(dets);
        dets = doc1.getDetails(1);
        dets.removePair(dets.getPair(0));

        doc2.removeDetails(doc2.getDetails(0));
        dets = doc2.getDetails(0);
        dets.removePair(dets.getPair(1));
        dets = new PasswordDetails();
        dets.setName("foo2");
        dets.setLocation("bar2");
        doc2.addDetails(dets);


        PasswordDocumentHistory hist = doc1.getHistory().mergeHistory(doc2.getHistory());
        int newSz = hist.count();
        assertEquals(newSz - 11, oldSz);
        PasswordDocument doc3 = new HelperNoInst.PasswordDocumentImpl();
        hist.playHistory(doc3);
    }

    @Test
    public void conflictedMerge() throws Exception
    {
        PasswordDocument doc1 = HelperNoInst.generateDocument(2, 2);
        PasswordDocument doc2 = new HelperNoInst.PasswordDocumentImpl();
        doc1.getHistory().playHistory(doc2);
        int oldSz = doc1.getHistory().count();

        PasswordDetails det1 = doc1.getDetails(0);
        det1.setName("det value 1");
        PasswordDetailsPair pair1 = det1.getPair(0);
        pair1.setValue("new value 1");

        PasswordDetails det2 = doc2.getDetails(0);
        det2.setName("det value 2");
        PasswordDetailsPair pair2 = det2.getPair(0);
        pair2.setValue("new value 2");

        //local changes take priority over remote changes
        PasswordDocumentHistory hist = doc1.getHistory().mergeHistory(doc2.getHistory());
        int newSz = hist.count();
        assertEquals(newSz - 4, oldSz);
        PasswordDocument doc3 = new HelperNoInst.PasswordDocumentImpl();
        hist.playHistory(doc3);
        PasswordDetails det3 = doc3.getDetails(0);
        assertEquals(det3.getName(), "det value 1");
        PasswordDetailsPair pair3 = det3.getPair(0);
        assertEquals(pair3.getValue(), "new value 1");
    }

    @Test
    public void conflictedMergeDeletedPair() throws Exception
    {
        PasswordDocument doc1 = HelperNoInst.generateDocument(2, 2);
        PasswordDocument doc2 = new HelperNoInst.PasswordDocumentImpl();
        doc1.getHistory().playHistory(doc2);
        int oldSz = doc1.getHistory().count();

        //modification events after delete don't throw errors
        PasswordDetails det1 = doc1.getDetails(0);
        PasswordDetailsPair pair1 = det1.getPair(0);
        det1.removePair(pair1);
        pair1 = det1.getPair(0);
        pair1.setValue("new value 4");

        PasswordDetails det2 = doc2.getDetails(0);
        PasswordDetailsPair pair2 = det2.getPair(0);
        pair2.setValue("new value 3");
        pair2 = det2.getPair(1);
        det2.removePair(pair2);

        PasswordDocumentHistory hist = doc1.getHistory().mergeHistory(doc2.getHistory());
        PasswordDocument doc3 = new HelperNoInst.PasswordDocumentImpl();
        hist.playHistory(doc3);
        PasswordDetails det3 = doc3.getDetails(0);
        assertEquals(0, det3.count());
    }

    @Test
    public void conflictedMergeDeletedDetails() throws Exception
    {
        PasswordDocument doc1 = HelperNoInst.generateDocument(2, 2);
        PasswordDocument doc2 = new HelperNoInst.PasswordDocumentImpl();
        doc1.getHistory().playHistory(doc2);
        int oldSz = doc1.getHistory().count();

        //modification events after delete don't throw errors
        PasswordDetails det1 = doc1.getDetails(0);
        doc1.removeDetails(det1);
        det1 = doc1.getDetails(0);
        det1.setName("name one");

        PasswordDetails det2 = doc2.getDetails(0);
        det2.setName("name two");
        det2 = doc2.getDetails(1);
        doc2.removeDetails(det2);

        PasswordDocumentHistory hist = doc1.getHistory().mergeHistory(doc2.getHistory());
        PasswordDocument doc3 = new HelperNoInst.PasswordDocumentImpl();
        hist.playHistory(doc3);
        assertEquals(0, doc3.count());
    }
}
