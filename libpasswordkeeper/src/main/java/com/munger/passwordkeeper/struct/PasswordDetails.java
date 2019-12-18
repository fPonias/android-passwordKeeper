package com.munger.passwordkeeper.struct;

import java.util.ArrayList;

import com.munger.passwordkeeper.struct.history.HistoryEvent;
import com.munger.passwordkeeper.struct.history.HistoryEventFactory;
import com.munger.passwordkeeper.struct.history.HistoryPairEvent;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;

public class PasswordDetails
{
	private String id;
	private String name;
	private String location;

	private ArrayList<PasswordDetailsPair> details;

	private PasswordDocumentHistory.HistoryEventListener historyListener;
	private PasswordDocumentHistory history;

	public ArrayList<PasswordDetailsPair> getList()
	{
		return details;
	}

	public PasswordDetails(String id)
	{
		this();

		this.id = id;
	}

	public PasswordDetails()
	{
		id = generateId();
		name = "";
		location = "";
		details = new ArrayList<PasswordDetailsPair>();

		historyListener = new PasswordDocumentHistory.HistoryEventListener() {public void occurred(HistoryEvent event)
		{
			event.id = id;
			notifyListeners(event);
		}};
		history = new PasswordDocumentHistory();
	}

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof PasswordDetails)
        {
            return ((PasswordDetails)obj).id == id;
        }
        else if (obj instanceof String)
        {
            return ((String) obj).equals(name);
        }
        else
        {
            return super.equals(obj);
        }
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

	public String getName()
	{
		return name;
	}

	public String getLocation()
	{
		return location;
	}

	public void setName(String value)
	{
		if (value.equals(name))
			return;

		name = value;

		HistoryEvent evt = new HistoryEventFactory().buildEvent(HistoryEventFactory.Types.DETAILS_UPDATE);
		evt.id = id;
		evt.property = "name";
		evt.value = value;

		notifyListeners(evt);
	}

	public void setLocation(String value)
	{
		if (value.equals(location))
			return;

		location = value;

		HistoryEvent evt = new HistoryEventFactory().buildEvent(HistoryEventFactory.Types.DETAILS_UPDATE);
		evt.id = id;
		evt.property = "location";
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

	private void notifyListeners(HistoryEvent evt)
	{
		history.addEvent(evt);

		for(PasswordDocumentHistory.HistoryEventListener listener : listeners)
			listener.occurred(evt);
	}

	public void setHistory(PasswordDocumentHistory history)
	{
		this.history = history;
	}

	public PasswordDocumentHistory getHistory()
	{
		return history;
	}

	public int count()
	{
		return details.size();
	}

	public PasswordDetailsPair getPair(int index)
	{
		return details.get(index);
	}

	public PasswordDetailsPair getPair(String id)
	{
		int sz = details.size();
		for (int i = 0; i < sz; i++)
		{
			PasswordDetailsPair p = details.get(i);
			if (p.getId().equals(id))
				return p;
		}

		return null;
	}

	public PasswordDetailsPair addEmptyPair()
	{
		PasswordDetailsPair pair = new PasswordDetailsPair();

		pair.addListener(historyListener);
		details.add(pair);

		HistoryPairEvent evt = (HistoryPairEvent) new HistoryEventFactory().buildEvent(HistoryEventFactory.Types.PAIR_CREATE);
		evt.id = id;
		evt.pairid = pair.getId();

		notifyListeners(evt);

		return pair;
	}

	public void addPair(PasswordDetailsPair p)
	{
		details.add(p);

		p.addListener(historyListener);

		HistoryPairEvent evt = (HistoryPairEvent) new HistoryEventFactory().buildEvent(HistoryEventFactory.Types.PAIR_CREATE);
		evt.id = id;
		evt.pairid = p.getId();

		notifyListeners(evt);
	}

	public void removePair(PasswordDetailsPair p)
	{
		int idx = -1;
		int sz = details.size();
		for (int i = 0; i < sz; i++)
		{
			PasswordDetailsPair oldPair = details.get(i);
			if (oldPair.getId().equals(p.getId()))
			{
				details.remove(i);

				oldPair.removeListener(historyListener);

				HistoryPairEvent evt = (HistoryPairEvent) new HistoryEventFactory().buildEvent(HistoryEventFactory.Types.PAIR_DELETE);
				evt.id = id;
				evt.pairid = p.getId();

				notifyListeners(evt);

				return;
			}
		}
	}

	public PasswordDetails copy()
	{
		PasswordDetails ret = new PasswordDetails();
		ret.id = id;
		ret.name = name;
		ret.location = location;
		
		int sz = details.size();
		for (int i = 0; i < sz; i++)
		{
			PasswordDetailsPair p = details.get(i);
			PasswordDetailsPair copy = p.copy();
			ret.details.add(copy);
			copy.addListener(ret.historyListener);
		}

		sz = history.count();
		for (int i = 0; i < sz; i++)
		{
			ret.history.addEvent(history.getEvent(i));
		}
		
		return ret;
	}

	public boolean diff(PasswordDetails dets)
	{
		if (!dets.id.equals(id))
			return true;

		if (!dets.name.equals(name))
			return true;
		
		if (!dets.location.equals(location))
			return true;
		
		if (dets.details.size() != details.size())
			return true;
		
		int sz = details.size();
		for (int i = 0; i < sz; i++)
		{
			PasswordDetailsPair det1 = dets.details.get(i);
			PasswordDetailsPair det2 = details.get(i);

			if (det1.diff(det2))
				return true;
		}
		
		return false;
	}

	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("id: ").append(id).append('\n');
		builder.append("location: ").append(location).append('\n');
		builder.append("name: ").append(name).append('\n');

		for (PasswordDetailsPair item : details)
		{
			builder.append(item.toString());
		}
		
		return builder.toString();
	}

	public PasswordDetailsPair curPair = null;

	public void fromToken(String token)
	{
		if (token.startsWith("id: "))
		{
			id = token.substring(4);
		}
		else if (token.startsWith("name: "))
		{
			name = token.substring(6);
		}
		else if (token.startsWith("location: "))
		{
			location = token.substring(10);
		}
		else if (token.startsWith("\tpairid: "))
		{
			curPair = new PasswordDetailsPair(token.substring(9));
		}
		else if (token.startsWith("\tkey: "))
		{
			curPair.setKey(token.substring(6));
		}
		else if (token.startsWith("\tvalue: "))
		{
			curPair.setValue(token.substring(8));
			details.add(curPair);
		}
	}

	public void fromString(String source)
	{
		details = new ArrayList<PasswordDetailsPair>();
	    
	    String[] parts = source.split("\n");
	    for (String line : parts)
	    {
			fromToken(line);
	    }
	}
}
