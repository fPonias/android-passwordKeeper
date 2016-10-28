package com.munger.passwordkeeper.struct;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by codymunger on 10/27/16.
 */

public class PasswordDocumentHistory
{
    public static enum EventType
    {
        NULL,
        ADD,
        DELETE,
        MODIFY
    };

    public static class Event
    {
        public long sequence;
        public long id;
        public EventType type;
        public PasswordDetails newDetails;
    }

    public ArrayList<Event> history;

    public void playHistory(PasswordDocument document, int sequenceStart)
    {
        int sz = history.size();
        for (int i = sequenceStart; i < sz; i++)
        {
            Event evt = history.get(i);

            switch(evt.type)
            {
                case ADD:
                    break;
                case DELETE:
                    break;
                case MODIFY:

            }
        }
    }

    public void mergeHistory(PasswordDocumentHistory mergeHistory)
    {

    }
}
