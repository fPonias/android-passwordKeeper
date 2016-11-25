package com.munger.passwordkeeper.struct;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

import com.munger.passwordkeeper.struct.history.HistoryEvent;
import com.munger.passwordkeeper.struct.history.HistoryEventFactory;
import com.munger.passwordkeeper.struct.history.HistoryPairEvent;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;

public class PasswordDetails implements Parcelable
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
		for(PasswordDocumentHistory.HistoryEventListener listener : listeners)
			listener.occurred(evt);

		history.addEvent(evt);
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

	public void fromString(String source)
	{
		details = new ArrayList<PasswordDetailsPair>();
		PasswordDetailsPair curPair = null;
	    
	    String[] parts = source.split("\n");
	    for (String line : parts)
	    {
			if (line.startsWith("id: "))
			{
				id = line.substring(4);
			}
	        else if (line.startsWith("name: "))
	        {
	        	name = line.substring(6);
	        }
	        else if (line.startsWith("location: "))
	        {
	        	location = line.substring(10);
	        }
			else if (line.startsWith("\tpairid: "))
			{
				curPair = new PasswordDetailsPair(line.substring(9));
			}
	        else if (line.startsWith("\tkey: "))
	        {
	        	curPair.setKey(line.substring(6));
	        }
	        else if (line.startsWith("\tvalue: "))
	        {
	        	curPair.setValue(line.substring(8));
	        	details.add(curPair);
	        }
	    }
	}

	public static Parcelable.Creator<PasswordDetails> CREATOR = new Creator<PasswordDetails>()
	{
		@Override
		public PasswordDetails createFromParcel(Parcel source)
		{
			String str = source.readString();
			PasswordDetails ret = new PasswordDetails();
			ret.fromString(str);
			return ret;
		}

		@Override
		public PasswordDetails[] newArray(int size)
		{
			return new PasswordDetails[size];
		}
	};

	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel arg0, int arg1) 
	{
		arg0.writeString(toString());
	}
}
