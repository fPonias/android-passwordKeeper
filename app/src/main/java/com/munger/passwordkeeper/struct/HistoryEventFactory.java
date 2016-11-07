package com.munger.passwordkeeper.struct;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.ParseException;
import java.util.ArrayList;

public class HistoryEventFactory
{
    public HistoryEvent fromString(String input) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
    {
        int idx = input.indexOf(" ");
        String type = input.substring(0, idx);

        Class<?> clazz = Class.forName(type);
        Constructor<?> ctor = clazz.getConstructor();
        Object obj = ctor.newInstance();

        HistoryEvent ret = (HistoryEvent) obj;
        ret.fromString(input.substring(idx + 1));
        return ret;
    }

    public String toString(HistoryEvent evt)
    {
        String ret = evt.getClass().getName() + " " + evt.toString();
        return ret;
    }

    public HistoryEvent clone(HistoryEvent evt)
    {
        HistoryEvent obj = null;
        try
        {
            Class<?> clazz = evt.getClass();
            Constructor<?> ctor = clazz.getConstructor();
            obj = (HistoryEvent) ctor.newInstance();
        }
        catch(Exception e){
            return null;
        }

        obj.id = evt.id;
        obj.sequenceId = evt.sequenceId;
        obj.property = evt.property;

        if (evt instanceof HistoryPairEvent)
        {
            ((HistoryPairEvent)obj).pairid = ((HistoryPairEvent) evt).pairid;
        }

        return obj;
    }

    public static abstract class HistoryEvent
    {
        public long sequenceId;
        public String id;
        public String property;
        public String value;

        public void fromString(String input)
        {
            int spcIdx = 0;
            int nextSpcIdx = -1;
            int idx = 0;
            while(idx < 4)
            {
                spcIdx = nextSpcIdx + 1;
                nextSpcIdx = input.indexOf(' ', spcIdx);
                String part = input.substring(spcIdx, nextSpcIdx);

                if (idx == 0)
                    id = part;
                else if (idx == 1)
                    sequenceId = Long.parseLong(part);
                else if (idx == 2)
                    property = part;
                else if (idx == 3)
                    value = input.substring(spcIdx);

                idx++;
            }
        }
        public String toString()
        {
            StringBuilder b = new StringBuilder();
            b.append(id).append(" ");
            b.append(sequenceId).append(" ");

            if (property != null)
                b.append(property);

            b.append(" ");

            if (value != null)
                b.append(value);

            return b.toString();
        }

        public String getPropertySignature()
        {
            return getClass().getName() + " " + id + " " + property;
        }

        public String getIDSignature()
        {
            return "detail:" + id;
        }

        public String getPairIDSignature()
        {
            return null;
        }

        public abstract void apply(PasswordDocument doc) throws PasswordDocumentHistory.HistoryPlaybackException;
    }

    public static class PasswordDetailsCreate extends HistoryEvent
    {
        public void apply(PasswordDocument doc) throws PasswordDocumentHistory.HistoryPlaybackException
        {
            if (doc.getDetails(id) != null)
                throw new PasswordDocumentHistory.HistoryPlaybackException();

            PasswordDetails dets = new PasswordDetails(id);
            doc.putDetails(dets);
        }
    }

    public static class PasswordDetailsUpdate extends HistoryEvent
    {
        public void apply(PasswordDocument doc) throws PasswordDocumentHistory.HistoryPlaybackException
        {
            PasswordDetails dets = doc.getDetails(id);
            if (dets == null)
                throw new PasswordDocumentHistory.HistoryPlaybackException();

            if (property == "name")
                dets.setName(value);
            else if (property == "location")
                dets.setLocation(value);
            else
                throw new PasswordDocumentHistory.HistoryPlaybackException();
        }
    }

    public static class PasswordDetailsDelete extends HistoryEvent
    {
        public void apply(PasswordDocument doc) throws PasswordDocumentHistory.HistoryPlaybackException
        {
            PasswordDetails dets = doc.getDetails(id);
            if (dets == null)
                throw new PasswordDocumentHistory.HistoryPlaybackException();

            doc.removeDetails(dets);
        }
    }

    public static abstract class HistoryPairEvent extends HistoryEvent
    {
        public String pairid;

        @Override
        public void fromString(String input)
        {
            int spcIdx = input.indexOf(' ');
            String part = input.substring(0, spcIdx);

            pairid = part;

            input = input.substring(spcIdx + 1);
            super.fromString(input);
        }
        @Override
        public String toString()
        {
            String ret = pairid + " ";
            ret += super.toString();

            return ret;
        }

        @Override
        public String getPairIDSignature()
        {
            return "pair:" + id + "-" + pairid;
        }

        @Override
        public String getPropertySignature()
        {
            return getClass().getName() + " " + id + " " + pairid + " " + property;
        }
    }

    public static class DetailsPairCreate extends HistoryPairEvent
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

    public static class DetailsPairUpdate extends HistoryPairEvent
    {
        public void apply(PasswordDocument doc) throws PasswordDocumentHistory.HistoryPlaybackException
        {
            PasswordDetails dets = doc.getDetails(id);
            if (dets == null)
                throw new PasswordDocumentHistory.HistoryPlaybackException();

            PasswordDetailsPair pair = dets.getPair(pairid);
            if (pair == null)
                throw new PasswordDocumentHistory.HistoryPlaybackException();


            if (property == "key")
                pair.setKey(value);
            else if (property == "value")
                pair.setValue(value);
            else
                throw new PasswordDocumentHistory.HistoryPlaybackException();
        }
    }

    public static class DetailsPairDelete extends HistoryPairEvent
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

