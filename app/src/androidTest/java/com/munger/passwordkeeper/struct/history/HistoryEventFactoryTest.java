package com.munger.passwordkeeper.struct.history;

/**
 * Created by codymunger on 11/20/16.
 */

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

public class HistoryEventFactoryTest
{
    @Test
    public void constructors()
    {
        HistoryEventFactory factory = new HistoryEventFactory();
        HistoryEvent evt = factory.buildEvent(HistoryEventFactory.Types.DETAILS_CREATE);
        assertEquals(evt.type, HistoryEventFactory.Types.DETAILS_CREATE);

        evt = factory.buildEvent(HistoryEventFactory.Types.DETAILS_UPDATE);
        assertEquals(evt.type, HistoryEventFactory.Types.DETAILS_UPDATE);

        evt = factory.buildEvent(HistoryEventFactory.Types.DETAILS_DELETE);
        assertEquals(evt.type, HistoryEventFactory.Types.DETAILS_DELETE);

        evt = factory.buildEvent(HistoryEventFactory.Types.PAIR_CREATE);
        assertEquals(evt.type, HistoryEventFactory.Types.PAIR_CREATE);
        assertTrue(evt instanceof HistoryPairEvent);

        evt = factory.buildEvent(HistoryEventFactory.Types.PAIR_UPDATE);
        assertEquals(evt.type, HistoryEventFactory.Types.PAIR_UPDATE);
        assertTrue(evt instanceof HistoryPairEvent);

        evt = factory.buildEvent(HistoryEventFactory.Types.PAIR_DELETE);
        assertEquals(evt.type, HistoryEventFactory.Types.PAIR_DELETE);
        assertTrue(evt instanceof HistoryPairEvent);
    }

    private void translateTest(HistoryEventFactory factory, HistoryEventFactory.Types type)
    {
        HistoryEvent evt = factory.buildEvent(type);
        evt.id = "anId";
        evt.sequenceId = 100;
        evt.property = "prop";
        evt.value = "value";

        if (evt instanceof HistoryPairEvent)
        {
            ((HistoryPairEvent) evt).pairid = "pairid";
        }

        String str = factory.toString(evt);

        HistoryEvent parsed = null;
        try {
            parsed = factory.fromString(str);
        }
        catch(Exception e){
            fail("failed to parse event");
        }
        assertTrue(HistoryEventFactory.equals(evt, parsed));
        assertEquals(type, parsed.type);
    }

    @Test
    public void toFromString()
    {
        HistoryEventFactory factory = new HistoryEventFactory();
        translateTest(factory, HistoryEventFactory.Types.DETAILS_CREATE);
        translateTest(factory, HistoryEventFactory.Types.DETAILS_UPDATE);
        translateTest(factory, HistoryEventFactory.Types.DETAILS_DELETE);
        translateTest(factory, HistoryEventFactory.Types.PAIR_CREATE);
        translateTest(factory, HistoryEventFactory.Types.PAIR_UPDATE);
        translateTest(factory, HistoryEventFactory.Types.PAIR_DELETE);
    }

    @Test
    public void equals()
    {
        HistoryEventFactory factory = new HistoryEventFactory();
        HistoryEvent evt = factory.buildEvent(HistoryEventFactory.Types.DETAILS_CREATE);
        evt.id = "ID";
        evt.sequenceId = 100;
        evt.property = "prop";
        evt.value = "value";
        HistoryEvent target = factory.clone(evt);
        assertNotSame(target, evt);
        assertTrue(factory.equals(evt, target));

        target.sequenceId = 101;
        assertFalse(factory.equals(evt, target));

        target = factory.clone(evt);
        target.property = "blah";
        assertFalse(factory.equals(evt, target));

        target = factory.clone(evt);
        target.value = "blah";
        assertFalse(factory.equals(evt, target));

        evt = factory.buildEvent(HistoryEventFactory.Types.PAIR_CREATE);
        ((HistoryPairEvent) evt).pairid = "foo";
        target = factory.clone(evt);
        ((HistoryPairEvent) target).pairid = "bar";
        assertFalse(factory.equals(evt, target));
    }
}
