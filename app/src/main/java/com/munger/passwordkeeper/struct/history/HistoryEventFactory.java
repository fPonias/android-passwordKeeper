package com.munger.passwordkeeper.struct.history;

import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class HistoryEventFactory
{
    private static HashMap<Types, Class> types;

    public enum Types
    {
        NULL,
        DETAILS_CREATE,
        DETAILS_UPDATE,
        DETAILS_DELETE,
        PAIR_CREATE,
        PAIR_UPDATE,
        PAIR_DELETE
    }

    static
    {
        types = new HashMap<>();
        types.put(Types.DETAILS_CREATE, PasswordDetailsCreate.class);
        types.put(Types.DETAILS_UPDATE, PasswordDetailsUpdate.class);
        types.put(Types.DETAILS_DELETE, PasswordDetailsDelete.class);
        types.put(Types.PAIR_CREATE, DetailsPairCreate.class);
        types.put(Types.PAIR_UPDATE, DetailsPairUpdate.class);
        types.put(Types.PAIR_DELETE, DetailsPairDelete.class);
    }

    public HistoryEvent buildEvent(Types type)
    {
            HistoryEvent ret = null;
            switch(type)
            {
                case DETAILS_CREATE:
                    ret = new PasswordDetailsCreate();
                    break;
                case DETAILS_UPDATE:
                    ret = new PasswordDetailsUpdate();
                    break;
                case DETAILS_DELETE:
                    ret = new PasswordDetailsDelete();
                    break;
                case PAIR_CREATE:
                    ret = new DetailsPairCreate();
                    break;
                case PAIR_UPDATE:
                    ret = new DetailsPairUpdate();
                    break;
                case PAIR_DELETE:
                    ret = new DetailsPairDelete();
                    break;
                default:
                    return null;
            }

            ret.type = type;
            return ret;
    }

    public HistoryEvent fromString(String input) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, NullPointerException
    {
        int idx = input.indexOf(" ");

        String type = input.substring(0, idx);
        int index = Integer.parseInt(type);
        Types t = Types.values()[index];

        HistoryEvent ret = buildEvent(t);
        ret.fromString(input.substring(idx + 1));
        return ret;
    }

    public String toString(HistoryEvent evt)
    {
        String ret = evt.type.ordinal() + " " + evt.toString();
        return ret;
    }

    public HistoryEvent clone(HistoryEvent evt)
    {
        HistoryEvent obj = buildEvent(evt.type);

        obj.id = evt.id;
        obj.sequenceId = evt.sequenceId;
        obj.property = evt.property;
        obj.value = evt.value;

        if (evt instanceof HistoryPairEvent)
        {
            ((HistoryPairEvent)obj).pairid = ((HistoryPairEvent) evt).pairid;
        }

        return obj;
    }

    public static boolean equals(HistoryEvent evt1, HistoryEvent evt2)
    {
        if (!evt1.id.equals(evt2.id))
            return false;

        if (evt1.type != evt2.type)
            return false;

        if (evt1.sequenceId != evt2.sequenceId)
            return false;

        if (!evt1.property.equals(evt2.property))
            return false;

        if (!evt1.value.equals(evt2.value))
            return false;

        if (evt1 instanceof HistoryPairEvent)
        {
            if (!((HistoryPairEvent) evt1).pairid.equals(((HistoryPairEvent) evt2).pairid))
                return false;
        }

        return true;
    }

    private static class PasswordDetailsCreate extends HistoryEvent
    {
        public void apply(PasswordDocument doc) throws PasswordDocumentHistory.HistoryPlaybackException
        {
            if (doc.getDetails(id) != null)
                throw new PasswordDocumentHistory.HistoryPlaybackException();

            PasswordDetails dets = new PasswordDetails(id);
            doc.putDetails(dets);
        }
    }

    private static class PasswordDetailsUpdate extends HistoryEvent
    {
        public void apply(PasswordDocument doc) throws PasswordDocumentHistory.HistoryPlaybackException
        {
            PasswordDetails dets = doc.getDetails(id);
            if (dets == null)
                throw new PasswordDocumentHistory.HistoryPlaybackException();

            if (property.equals("name"))
                dets.setName(value);
            else if (property.equals("location"))
                dets.setLocation(value);
            else
                throw new PasswordDocumentHistory.HistoryPlaybackException();
        }
    }

    private static class PasswordDetailsDelete extends HistoryEvent
    {
        public void apply(PasswordDocument doc) throws PasswordDocumentHistory.HistoryPlaybackException
        {
            PasswordDetails dets = doc.getDetails(id);
            if (dets == null)
                throw new PasswordDocumentHistory.HistoryPlaybackException();

            doc.removeDetails(dets);
        }
    }

    private static class DetailsPairCreate extends HistoryPairEvent
    {
        public void apply(PasswordDocument doc) throws PasswordDocumentHistory.HistoryPlaybackException
        {
            PasswordDetails dets = doc.getDetails(id);
            if (dets == null)
                throw new PasswordDocumentHistory.HistoryPlaybackException();

            PasswordDetailsPair pair = dets.getPair(pairid);
            if (pair != null)
                throw new PasswordDocumentHistory.HistoryPlaybackException();

            pair = new PasswordDetailsPair(pairid);
            dets.addPair(pair);
        }
    }

    private static class DetailsPairUpdate extends HistoryPairEvent
    {
        public void apply(PasswordDocument doc) throws PasswordDocumentHistory.HistoryPlaybackException
        {
            PasswordDetails dets = doc.getDetails(id);
            if (dets == null)
                throw new PasswordDocumentHistory.HistoryPlaybackException();

            PasswordDetailsPair pair = dets.getPair(pairid);
            if (pair == null)
                throw new PasswordDocumentHistory.HistoryPlaybackException();


            if (property.equals("key"))
                pair.setKey(value);
            else if (property.equals("value"))
                pair.setValue(value);
            else
                throw new PasswordDocumentHistory.HistoryPlaybackException();
        }
    }

    private static class DetailsPairDelete extends HistoryPairEvent
    {
        public void apply(PasswordDocument doc) throws PasswordDocumentHistory.HistoryPlaybackException
        {
            PasswordDetails dets = doc.getDetails(id);
            if (dets == null)
                throw new PasswordDocumentHistory.HistoryPlaybackException();

            PasswordDetailsPair pair = dets.getPair(pairid);
            if (pair == null)
                throw new PasswordDocumentHistory.HistoryPlaybackException();

            dets.removePair(pair);
        }
    }
}

