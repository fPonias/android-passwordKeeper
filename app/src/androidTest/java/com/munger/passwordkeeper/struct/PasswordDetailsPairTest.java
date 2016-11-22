package com.munger.passwordkeeper.struct;

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

public class PasswordDetailsPairTest
{
    @Test
    public void constructorsWork()
    {
        PasswordDetailsPair p1 = new PasswordDetailsPair();
        PasswordDetailsPair p2 = new PasswordDetailsPair("id");
        PasswordDetailsPair p3 = new PasswordDetailsPair("key", "value");
        PasswordDetailsPair p4 = new PasswordDetailsPair("id", "key", "value");
    }

    @Test
    public void copy()
    {
        PasswordDetailsPair p1 = new PasswordDetailsPair();
        PasswordDetailsPair p2 = p1.copy();

        assertNotSame(p2, p1);
        assertEquals(p1.getId(), p2.getId());
        assertEquals(p1.getKey(), p2.getKey());
        assertEquals(p1.getValue(), p2.getValue());
    }

    @Test
    public void diff()
    {
        PasswordDetailsPair p1 = new PasswordDetailsPair("id", "key", "value");
        assertFalse(p1.diff(p1));
        PasswordDetailsPair p2 = p1.copy();

        assertNotSame(p2, p1);
        assertFalse(p1.diff(p2));
        p1.setKey("foo");
        assertTrue(p1.diff(p2));
        p1.setKey("key");
        assertFalse(p1.diff(p2));
        p1.setValue("bar");
        assertTrue(p1.diff(p2));
        p1.setValue("value");
        assertFalse(p1.diff(p2));

        PasswordDetailsPair p3 = new PasswordDetailsPair("key", "value");
        assertTrue(p1.diff(p3));
    }

    @Test
    public void testToString()
    {
        PasswordDetailsPair p1 = new PasswordDetailsPair("id", "key", "value");
        String str = p1.toString();
        assertNotEquals(0, str.length());
        assertTrue(str.indexOf("null") == -1);
    }

    @Test
    public void listenersWork()
    {
        class Counters
        {
            public int count;
        }

        final Counters count1 = new Counters();
        final Counters count2 = new Counters();

        PasswordDocumentHistory.HistoryEventListener listener1 = new PasswordDocumentHistory.HistoryEventListener()
        {
            public void occurred(HistoryEvent event)
            {
                count1.count++;

                if (count1.count == 1)
                {
                    assertEquals(HistoryEventFactory.Types.PAIR_UPDATE, event.type);
                    assertEquals("key", event.property);
                    assertEquals("key", event.value);
                }
                else if (count1.count == 2)
                {
                    assertEquals(HistoryEventFactory.Types.PAIR_UPDATE, event.type);
                    assertEquals("value", event.property);
                    assertEquals("value", event.value);
                }
            }
        };

        PasswordDocumentHistory.HistoryEventListener listener2 = new PasswordDocumentHistory.HistoryEventListener()
        {
            public void occurred(HistoryEvent event)
            {
                count2.count++;
                if (count2.count == 1)
                {
                    assertEquals(HistoryEventFactory.Types.PAIR_UPDATE, event.type);
                    assertEquals("key", event.property);
                    assertEquals("key", event.value);
                }
                else if (count2.count == 2)
                {
                    assertEquals(HistoryEventFactory.Types.PAIR_UPDATE, event.type);
                    assertEquals("value", event.property);
                    assertEquals("value", event.value);
                }
                else if (count2.count == 3)
                {
                    assertEquals(HistoryEventFactory.Types.PAIR_UPDATE, event.type);
                    assertEquals("value", event.property);
                    assertEquals("value2", event.value);
                }
            }
        };

        PasswordDetailsPair pair1 = new PasswordDetailsPair();
        pair1.setKey("foo");
        pair1.addListener(listener1);
        pair1.addListener(listener2);
        pair1.setKey("key");
        pair1.setValue("value");
        pair1.removeListener(listener1);
        pair1.setValue("value2");

        assertEquals(3, count2.count);
        assertEquals(2, count1.count);
    }
}
