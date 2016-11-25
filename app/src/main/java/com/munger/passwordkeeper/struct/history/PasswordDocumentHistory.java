package com.munger.passwordkeeper.struct.history;

import com.munger.passwordkeeper.struct.documents.PasswordDocument;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;


public class PasswordDocumentHistory
{
    public PasswordDocumentHistory()
    {
        history = new ArrayList<>();
    }

    public static class HistoryPlaybackException extends Exception
    {}

    public interface HistoryEventListener
    {
        void occurred(HistoryEvent event);
    }

    private long sequenceCount = 1;
    //friend variable
    ArrayList<HistoryEvent> history;

    public void addEvent(HistoryEvent evt)
    {
        evt.sequenceId = sequenceCount;
        sequenceCount++;

        history.add(evt);
    }

    public long getSequenceCount()
    {
        return sequenceCount;
    }

    public void setSequenceCount(long value)
    {
        int sz = history.size();

        if (sz > 0)
        {
            long max = history.get(history.size() - 1).sequenceId;
            sequenceCount = (max < value) ? value : max;
        }
        else
            sequenceCount = value;
    }

    public int count()
    {
        return history.size();
    }

    public HistoryEvent getEvent(int index)
    {
        return history.get(index);
    }

    public void playHistory(PasswordDocument document) throws HistoryPlaybackException
    {
        if (history.size() == 0)
            return;

        int startIdx = findClosestIndex(document.getHistory());

        if (startIdx < 0)
            return;

        int sz = history.size();

        for (int i = startIdx; i < sz; i++)
        {
            HistoryEvent evt = history.get(i);
            evt.apply(document);

            PasswordDocumentHistory docHist = document.getHistory();
            int dsz = docHist.count();

            //check if executing the event didn't already add the event to the document's history
            if (dsz > 0)
            {
                HistoryEvent event = docHist.getEvent(dsz - 1);
                if (!HistoryEventFactory.equals(event, evt))
                    document.getHistory().addEvent(evt);
            }
            else
                document.getHistory().addEvent(evt);
        }
    }

    public PasswordDocumentHistory mergeHistory(PasswordDocumentHistory mergeHistory)
    {
        PasswordDocumentHistory ret = new HistoryMerger(this, mergeHistory).doMerge();
        return ret;
    }

    //friend function
    int findClosestIndex(PasswordDocumentHistory mergeHistory)
    {
        if (history.size() == 0)
            return -1;

        int mergeStart = 0;

        int lsz = history.size();
        int msz = mergeHistory.history.size();

        if (msz == 0)
            return 0;

        HistoryEvent firstEvent = history.get(0);

        for (int i = 0; i < msz; i++)
        {
            HistoryEvent mergeEvent = mergeHistory.history.get(i);
            if (HistoryEventFactory.equals(mergeEvent, firstEvent))
            {
                mergeStart = i;
                break;
            }
        }

        int m = mergeStart;
        int l = 0;
        while (m < msz && l < lsz)
        {
            HistoryEvent localEvent = history.get(l);
            HistoryEvent mergeEvent = mergeHistory.history.get(m);

            if (!HistoryEventFactory.equals(localEvent, mergeEvent))
                return l;

            l++; m++;
        }

        if (m == msz)
            return l;
        else
            return -1;
    }

    public void clean()
    {
        cleanRedundant();
        cleanDeleted();
    }

    private void cleanRedundant()
    {
        HashSet<String> indices = new HashSet<>();

        int sz = history.size();
        for (int i = sz - 1; i >= 0; i--)
        {
            HistoryEvent evt = history.get(i);
            String index = evt.getPropertySignature();

            if (indices.contains(index))
                history.remove(i);
            else
                indices.add(index);
        }
    }

    private void cleanDeleted()
    {
        HashSet<String> deleted = new HashSet<>();

        int sz = history.size();
        for (int i = sz - 1; i >= 0; i--)
        {
            HistoryEvent evt = history.get(i);
            String deletedPairIdx = evt.getPairIDSignature();
            String deletedDetsIdx = evt.getIDSignature();

            if (evt instanceof HistoryPairEvent)
            {
                if (evt.type == HistoryEventFactory.Types.PAIR_DELETE)
                {
                    deleted.add(deletedPairIdx);
                    deletedPairIdx = null;
                }
            }
            else
            {
                if (evt.type == HistoryEventFactory.Types.DETAILS_DELETE)
                {
                    deleted.add(deletedDetsIdx);
                    deletedDetsIdx = null;
                }
            }

            if (deletedDetsIdx != null && deleted.contains(deletedDetsIdx))
                history.remove(i);
            if (deletedPairIdx != null && deleted.contains(deletedPairIdx))
                history.remove(i);
        }
    }

    public String partToString(int idx, int sz)
    {
        ArrayList<HistoryEvent> arr = new ArrayList<>();
        for (HistoryEvent evt : history)
        {
            if (evt.sequenceId >= idx && evt.sequenceId < idx + sz)
                arr.add(evt);
        }

        if (arr.size() == 0)
            return "";


        HistoryEventFactory factory = new HistoryEventFactory();
        StringBuilder b = new StringBuilder();

        for(HistoryEvent evt : arr)
        {
            String str = factory.toString(evt);
            b.append(str).append('\n');
        }

        return b.toString();
    }

    public String toString()
    {
        HistoryEventFactory factory = new HistoryEventFactory();
        StringBuilder b = new StringBuilder();

        b.append(sequenceCount).append('\n');

        int sz = history.size();
        for (int i = 0; i < sz; i++)
        {
            String str = factory.toString(history.get(i));
            b.append(str).append('\n');
        }

        return b.toString();
    }

    public void partFromString(String text) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
    {
        if (text == "")
            return;

        HistoryEventFactory factory = new HistoryEventFactory();

        String[] parts = text.split("\n");
        int sz = parts.length;
        for (int i = 0; i < sz; i++)
        {
            HistoryEvent evt = factory.fromString(parts[i]);
            history.add(evt);
        }
    }

    public void fromString(String text) throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
    {
        HistoryEventFactory factory = new HistoryEventFactory();

        history = new ArrayList<>();
        String[] parts = text.split("\n");
        int sz = parts.length;
        for (int i = 0; i < sz; i++)
        {
            if (i == 0)
            {
                Integer count = Integer.parseInt(parts[i]);
                sequenceCount = count;
            }
            else
            {
                HistoryEvent evt = factory.fromString(parts[i]);
                history.add(evt);
            }
        }
    }

    public PasswordDocumentHistory clone()
    {
        HistoryEventFactory fac = new HistoryEventFactory();
        PasswordDocumentHistory ret = new PasswordDocumentHistory();
        int sz = history.size();
        for (int i = 0; i < sz; i++)
        {
            HistoryEvent evt = history.get(i);
            ret.history.add(fac.clone(evt));
        }

        ret.setSequenceCount(sequenceCount);
        return ret;
    }

    public boolean equals(PasswordDocumentHistory hist)
    {
        int sz = history.size();
        if (hist.history.size() != sz)
            return false;

        if (hist.getSequenceCount() != getSequenceCount())
            return false;

        for (int i = 0; i < sz; i++)
        {
            HistoryEvent hevent = hist.history.get(i);
            HistoryEvent event = history.get(i);

            if (!HistoryEventFactory.equals(hevent, event))
                return false;
        }

        return true;
    }
}
