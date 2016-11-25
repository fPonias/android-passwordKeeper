package com.munger.passwordkeeper.struct.history;

import java.util.HashSet;

/**
 * Created by codymunger on 11/24/16.
 */

public class HistoryMerger
{
    private PasswordDocumentHistory ret;
    private PasswordDocumentHistory local;
    private PasswordDocumentHistory remote;

    private int localIdx;
    private int localSz;
    private int remoteIdx;
    private int remoteSz;

    private HashSet<String> deletedDetails = new HashSet<>();
    private HashSet<String> deletedPairs = new HashSet<>();

    public HistoryMerger(PasswordDocumentHistory local, PasswordDocumentHistory remote)
    {
        this.local = local;
        this.remote = remote;

        localSz = local.count();
        remoteSz = remote.count();
    }

    public PasswordDocumentHistory doMerge()
    {
        ret = new PasswordDocumentHistory();

        localIdx = local.findClosestIndex(remote);
        remoteIdx = remote.findClosestIndex(local);

        if (localIdx > -1)
            copyTo(localIdx);
        else
        {
            copyTo(localSz);

            if (remoteIdx == -1)
                return ret;
        }


        deletedDetails = new HashSet<>();
        deletedPairs = new HashSet<>();

        appendRemote();
        appendLocal();

        return ret;
    }

    private void appendRemote()
    {
        if (remoteIdx == -1)
            return;

        for (int mi = remoteIdx; mi < remoteSz; mi++)
        {
            HistoryEvent evt = remote.history.get(mi);

            if (evt.type == HistoryEventFactory.Types.DETAILS_DELETE)
                deletedDetails.add(evt.id);
            if (evt.type == HistoryEventFactory.Types.PAIR_DELETE)
                deletedPairs.add(((HistoryPairEvent) evt).pairid);

            ret.addEvent(evt);
        }
    }

    private void appendLocal()
    {
        if (localIdx == -1)
            return;

        for (int myi = localIdx; myi < localSz; myi++)
        {
            HistoryEvent evt = local.history.get(myi);
            if (!deletedDetails.contains(evt.id))
            {
                if (!(evt instanceof HistoryPairEvent))
                {
                    ret.addEvent(evt);
                }
                else
                {
                    if (!deletedPairs.contains(((HistoryPairEvent) evt).pairid))
                    {
                        ret.addEvent(evt);
                    }
                }
            }
        }
    }

    private void copyTo(int max)
    {
        for (int myi = 0; myi < max; myi++)
            ret.history.add(local.history.get(myi));

        if (max > 0)
            ret.setSequenceCount(local.history.get(max - 1).sequenceId);
    }
}
