package com.munger.passwordkeeper.struct;

import com.munger.passwordkeeper.Helper;
import com.munger.passwordkeeper.struct.history.HistoryEvent;
import com.munger.passwordkeeper.struct.history.HistoryEventFactory;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

/**
 * Created by codymunger on 11/20/16.
 */

public class PasswordDetailsTest
{
    @Test
    public void constructorsWork()
    {
        PasswordDetails p1 = new PasswordDetails();
        PasswordDetails p2 = new PasswordDetails("id");

        assertEquals(p2.getId(), "id");
        assertNotEquals(p2.getId(), p1.getId());
    }

    @Test
    public void manipulation()
    {
        //different ids detected
        PasswordDetails p1 = new PasswordDetails("id");
        PasswordDetails p2 = new PasswordDetails();
        assertTrue(p1.diff(p2));
        p2 = new PasswordDetails("id");
        assertFalse(p1.diff(p2));

        //different names and locations detected
        p1.setName("name");
        assertTrue(p1.diff(p2));
        p2.setName("name");
        assertFalse(p1.diff(p2));
        p1.setLocation("loc");
        assertTrue(p1.diff(p2));
        p2.setLocation("loc");
        assertFalse(p1.diff(p2));

        //similar pairs are detected
        //add empty pair
        PasswordDetailsPair pair1 = p1.addEmptyPair();
        assertTrue(p1.diff(p2));
        pair1.setKey("key1");
        pair1.setValue("value1");
        PasswordDetailsPair pair2 = pair1.copy();
        //add premade pair
        p2.addPair(pair2);
        assertFalse(p1.diff(p2));

        //multiple pairs are detected
        PasswordDetailsPair pair3 = p1.addEmptyPair();
        assertTrue(p1.diff(p2));
        pair3.setKey("key2");
        pair3.setValue("value2");
        PasswordDetailsPair pair4 = pair3.copy();
        p2.addPair(pair4);
        assertFalse(p1.diff(p2));

        //mismatched pairs are detected
        pair4.setValue("foo");
        assertTrue(p1.diff(p2));

        //out of order pairs are detected
        //get pair by index
        //remove pair by pair object
        p2.removePair(p2.getPair(1));
        p1.removePair(p1.getPair(1));
        assertFalse(p1.diff(p2));
        //get pair by id
        p1.removePair(p1.getPair(pair1.getId()));
        p2.removePair(p2.getPair(pair2.getId()));
        pair4 = pair3.copy();
        p1.addPair(pair1);
        p2.addPair(pair4);
        p1.addPair(pair2);
        p2.addPair(pair3);
        assertTrue(p1.diff(p2));
    }

    @Test
    public void copy()
    {
        PasswordDetails dets = new PasswordDetails("id");
        dets.setName("name");
        dets.setLocation("location");

        PasswordDetailsPair pair = dets.addEmptyPair();
        pair.setKey("key");
        pair.setValue("value");

        PasswordDetailsPair pair2 = dets.addEmptyPair();
        pair2.setKey("key2");
        pair2.setValue("value2");

        dets.removePair(pair);

        PasswordDetails dets2 = dets.copy();
        assertFalse(dets.diff(dets2));


        PasswordDocumentHistory h1 = dets.getHistory();
        PasswordDocumentHistory h2 = dets2.getHistory();

        assertEquals(h1.count(), h2.count());
        int sz = h1.count();
        for (int i = 0; i < sz; i++)
        {
            HistoryEvent evt1 = h1.getEvent(i);
            HistoryEvent evt2 = h2.getEvent(i);
            assertTrue(HistoryEventFactory.equals(evt1, evt2));
        }
    }

    private void doSerialization(PasswordDetails dets)
    {
        String enc = dets.toString();
        PasswordDetails copy = new PasswordDetails();
        copy.fromString(enc);
        assertFalse(dets.diff(copy));
    }

    @Test
    public void serialization()
    {
        PasswordDetails dets = new PasswordDetails();
        doSerialization(dets);

        dets.setName("name");
        dets.setLocation("loc");
        doSerialization(dets);

        dets.setName("name name");
        dets.setLocation("loc loc");
        doSerialization(dets);

        dets.setName(Helper.longString());
        dets.setLocation(Helper.longString());
        doSerialization(dets);

        PasswordDetailsPair pair = dets.addEmptyPair();
        doSerialization(dets);

        pair.setKey("key");
        pair.setValue("value");
        doSerialization(dets);

        PasswordDetailsPair pair2 = dets.addEmptyPair();
        doSerialization(dets);

        pair2.setKey("key key");
        pair2.setValue("value value");
        doSerialization(dets);

        for (int i = 0; i < 100; i++)
        {
            PasswordDetailsPair p = dets.addEmptyPair();
            p.setKey(String.valueOf(i));
            p.setValue(String.valueOf(i));
        }
        doSerialization(dets);

        pair.setKey(Helper.longString());
        pair.setValue(Helper.longString());
        doSerialization(dets);
    }

    @Test
    public void notifications()
    {
        class Counter
        {
            public int c = 0;
        }

        final Counter count1 = new Counter();
        PasswordDocumentHistory.HistoryEventListener listener1 = new PasswordDocumentHistory.HistoryEventListener() {public void occurred(HistoryEvent event)
        {
            count1.c++;

            if (count1.c == 1)
            {
                assertEquals(HistoryEventFactory.Types.DETAILS_UPDATE, event.type);
                assertEquals("name", event.property);
                assertEquals("det", event.value);
            }
            else if (count1.c == 2)
            {
                assertEquals(HistoryEventFactory.Types.DETAILS_UPDATE, event.type);
                assertEquals("location", event.property);
                assertEquals("loc", event.value);
            }
            else if (count1.c == 3)
            {
                assertEquals(HistoryEventFactory.Types.PAIR_CREATE, event.type);
            }
            else if (count1.c == 4)
            {
                assertEquals(HistoryEventFactory.Types.PAIR_UPDATE, event.type);
                assertEquals("key", event.property);
                assertEquals("key1", event.value);
            }
            else if (count1.c == 5)
            {
                assertEquals(HistoryEventFactory.Types.PAIR_UPDATE, event.type);
                assertEquals("value", event.property);
                assertEquals("val1", event.value);
            }
            else if (count1.c == 6)
            {
                assertEquals(HistoryEventFactory.Types.PAIR_DELETE, event.type);
            }
        }};

        PasswordDetails dets = new PasswordDetails();
        dets.addListener(listener1);
        dets.setName("det");
        dets.setLocation("loc");
        PasswordDetailsPair pair = dets.addEmptyPair();
        pair.setKey("key1");
        pair.setValue("val1");
        dets.removePair(pair);

        dets.removeListener(listener1);
        dets.setName("d");
        dets.setLocation("l");
        pair = dets.addEmptyPair();
        pair.setKey("k");
        pair.setValue("v");
        dets.removePair(pair);

        assertEquals(6, count1.c);

    }
}
