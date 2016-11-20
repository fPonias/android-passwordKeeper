package com.munger.passwordkeeper.struct.com.munger.passwordkeeper.struct.history;

import com.munger.passwordkeeper.struct.com.munger.passwordkeeper.struct.documents.PasswordDocument;

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
        public void occurred(HistoryEvent event);
    }

    private long sequenceCount = 1;
    private ArrayList<HistoryEvent> history;

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
        playHistory(document, 0);
    }

    public void playHistory(PasswordDocument document, int sequenceStart) throws HistoryPlaybackException
    {
        int startIdx = findClosestIndex(sequenceStart);

        if (startIdx < 0)
            return;

        int sz = history.size();

        for (int i = startIdx; i < sz; i++)
        {
            HistoryEvent evt = history.get(i);
            evt.apply(document);
        }
    }

    public ArrayList<HistoryEvent> mergeHistory(PasswordDocumentHistory mergeHistory, int sequenceStart)
    {
        ArrayList<HistoryEvent> merged = new ArrayList<>();

        int myidx = findClosestIndex(sequenceStart);
        int mysz = history.size();

        int mergeidx = mergeHistory.findClosestIndex(sequenceStart);
        int mergesz = mergeHistory.history.size();

        for (int myi = 0; myi < myidx; myi++)
            merged.add(history.get(myi));

        HashSet<String> deletedDetails = new HashSet<>();
        HashSet<String> deletedPairs = new HashSet<>();

        for (int mi = mergeidx; mi < mergesz; mi++)
        {
            HistoryEvent evt = mergeHistory.history.get(mi);

            if (evt.type == HistoryEventFactory.Types.DETAILS_DELETE);
                deletedDetails.add(evt.id);
            if (evt.type == HistoryEventFactory.Types.PAIR_DELETE)
                deletedPairs.add(((HistoryPairEvent) evt).pairid);

            merged.add(evt);
        }

        for (int myi = myidx; myi < mysz; myi++)
        {
            HistoryEvent evt = history.get(myi);
            if (!deletedDetails.contains(evt.id))
            {
                if (!(evt instanceof HistoryPairEvent))
                {
                    merged.add(evt);
                }
                else
                {
                    if (!deletedPairs.contains(((HistoryPairEvent) evt).pairid))
                    {
                        merged.add(evt);
                    }
                }
            }
        }

        return merged;
    }

    private int findClosestIndex(int sequenceNum)
    {
        int startIdx = -1;
        int sz = history.size();
        for (int i = 0; i < sz; i++)
        {
            HistoryEvent nextEvent = history.get(i);

            if (nextEvent.sequenceId >= sequenceNum) {
                startIdx = i;
                break;
            }
        }

        return startIdx;
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
            return null;


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

        return ret;
    }
}
