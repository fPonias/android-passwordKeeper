package com.munger.passwordkeeper.struct;

import java.util.ArrayList;

public class PasswordDetailsPair
{
    private String key;
    private String value;
    private String id;

    public PasswordDetailsPair()
    {
        id = generateId();
        key = "";
        value = "";
    }

    public PasswordDetailsPair(String id)
    {
        this.id = id;
    }

    public PasswordDetailsPair(String key, String value)
    {
        this.id = generateId();
        this.key = key;
        this.value = value;
    }

    public PasswordDetailsPair(String id, String key, String value)
    {
        this.id = id;
        this.key = key;
        this.value = value;
    }

    public PasswordDetailsPair copy()
    {
        PasswordDetailsPair ret = new PasswordDetailsPair(id, key, value);
        return ret;
    }

    public static String generateId()
    {
        long timestamp = System.currentTimeMillis();
        long rand = (long) Math.floor(Math.random() * 10000.0);
        String ret = timestamp + "-" + rand;
        return ret;
    }

    public String getId()
    {
        return id;
    }

    public String getKey()
    {
        return key;
    }

    public String getValue()
    {
        return value;
    }

    public void setKey(String value)
    {
        key = value;

        HistoryEventFactory.DetailsPairUpdate evt = new HistoryEventFactory.DetailsPairUpdate();
        evt.pairid = id;
        evt.property = "key";
        evt.value = value;

        notifyListeners(evt);
    }

    public void setValue(String value)
    {
        this.value = value;

        HistoryEventFactory.DetailsPairUpdate evt = new HistoryEventFactory.DetailsPairUpdate();
        evt.pairid = id;
        evt.property = "value";
        evt.value = value;

        notifyListeners(evt);
    }

    private ArrayList<PasswordDocumentHistory.HistoryEventListener> listeners = new ArrayList<>();

    public void addListener(PasswordDocumentHistory.HistoryEventListener listener)
    {
        if (listeners.contains(listener))
            return;

        else listeners.add(listener);
    }

    public void removeListener(PasswordDocumentHistory.HistoryEventListener listener)
    {
        listeners.remove(listener);
    }

    private void notifyListeners(HistoryEventFactory.HistoryEvent evt)
    {
        for(PasswordDocumentHistory.HistoryEventListener listener : listeners)
            listener.occurred(evt);
    }

    public boolean diff(PasswordDetailsPair p2)
    {
        if (!id.equals(p2.id))
            return true;
        if (!key.equals(p2.key))
            return true;
        if (!value.equals(p2.value))
            return true;

        return false;
    }

    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("\tpairid: ").append(id).append('\n');
        builder.append("\tkey: ").append(key).append('\n');
        builder.append("\tvalue: ").append(value).append('\n');

        return builder.toString();
    }
}
