package com.munger.passwordkeeper.struct;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public class PasswordDetails implements Parcelable
{
	public String id;
	public String name;
	public String location;

	protected ArrayList<Pair> details;

	public ArrayList<Pair> getList()
	{
		return details;
	}

	public static class Pair
	{
		public String key;
		public String value;
		public String id;
		
		public Pair()
		{
			id = "";
			key = "";
			value = "";
		}
		
		public Pair(String key, String value)
		{
			this.id = generateId();
			this.key = key;
			this.value = value;
		}

		public Pair(String id, String key, String value)
		{
			this.id = id;
			this.key = key;
			this.value = value;
		}
		
		public Pair copy()
		{
			Pair ret = new Pair(id, key, value);
			return ret;
		}

		public static String generateId()
		{
			long timestamp = System.currentTimeMillis();
			long rand = (long) Math.floor(Math.random() * 10000.0);
			String ret = timestamp + "-" + rand;
			return ret;
		}
	}
	
	public PasswordDetails()
	{
		id = "";
		name = "";
		location = "";
		details = new ArrayList<Pair>();
	}

	public int count()
	{
		return details.size();
	}

	public void addPair(Pair p)
	{
		addPair(p, false);
	}

	public Pair getPairAt(int index)
	{
		return details.get(index);
	}

	public void addPair(Pair p, boolean keepId)
	{
		if (!keepId)
			p.id = Pair.generateId();

		details.add(p);
	}

	public void removePair(Pair p)
	{
		int idx = -1;
		int sz = details.size();
		for (int i = 0; i < sz; i++)
		{
			Pair oldPair = details.get(i);
			if (oldPair.id.equals(p.id))
			{
				details.remove(i);
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
			Pair p = details.get(i);
			ret.details.add(p.copy());
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
			Pair det1 = dets.details.get(i);
			Pair det2 = details.get(i);

			if (!det1.id.equals(det2.id))
				return true;
			if (!det1.key.equals(det2.key))
				return true;
			if (!det1.value.equals(det2.value))
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

		for (Pair item : details)
		{
			builder.append("\tpairid: ").append(item.id).append('\n');
			builder.append("\tkey: ").append(item.key).append('\n');
			builder.append("\tvalue: ").append(item.value).append('\n');
		}
		
		return builder.toString();
	}

	public void fromString(String source)
	{
		details = new ArrayList<Pair>();
	    Pair curPair = null;
	    
	    String[] parts = source.split("\n");
	    for (String line : parts)
	    {
			if (line.startsWith("id: "))
			{
				id = line.substring(5);
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
				curPair = new Pair();
				curPair.id = line.substring(9);
			}
	        else if (line.startsWith("\tkey: "))
	        {
	        	curPair.key = line.substring(6);
	        }
	        else if (line.startsWith("\tvalue: "))
	        {
	        	curPair.value = line.substring(8);
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
