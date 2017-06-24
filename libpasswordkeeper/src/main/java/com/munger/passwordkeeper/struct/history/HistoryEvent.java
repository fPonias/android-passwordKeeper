package com.munger.passwordkeeper.struct.history;

import com.munger.passwordkeeper.struct.documents.PasswordDocument;

/**
 * Created by codymunger on 11/19/16.
 */
public abstract class HistoryEvent {
    public long sequenceId = -1;
    public String id = "";
    public String property = "";
    public String value = "";
    public HistoryEventFactory.Types type;

    public void fromString(String input) {
        int spcIdx = 0;
        int nextSpcIdx = -1;
        int idx = 0;
        while (idx < 4) {
            spcIdx = nextSpcIdx + 1;
            nextSpcIdx = input.indexOf(' ', spcIdx);
            String part = (nextSpcIdx > -1) ? input.substring(spcIdx, nextSpcIdx) : input.substring(spcIdx);

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

    public String toString() {
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

    public String getPropertySignature() {
        return type.ordinal() + " " + id + " " + property;
    }

    public String getIDSignature() {
        return "detail:" + id;
    }

    public String getPairIDSignature() {
        return null;
    }

    public abstract void apply(PasswordDocument doc) throws PasswordDocumentHistory.HistoryPlaybackException;
}
