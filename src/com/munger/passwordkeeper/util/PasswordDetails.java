package com.munger.passwordkeeper.util;

import java.util.ArrayList;

public class PasswordDetails 
{
	public String name;
	public String location;
	public ArrayList<Pair> details;
	public int index;
	
	public static class Pair
	{
		public String key;
		public String value;
		
		public Pair()
		{
			key = "";
			value = "";
		}
		
		public Pair(String key, String value)
		{
			this.key = key;
			this.value = value;
		}
		
		public Pair copy()
		{
			Pair ret = new Pair(key, value);
			return ret;
		}
	}
	
	public PasswordDetails()
	{
		name = "";
		location = "";
		details = new ArrayList<Pair>();
		index = 0;
	}
	
	public void setIndex(int i)
	{
		index = i;
	}
	
	public int getIndex()
	{
		return index;
	}
	
	public PasswordDetails copy()
	{
		PasswordDetails ret = new PasswordDetails();
		ret.name = name;
		ret.location = location;
		ret.index = index;
		
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
		builder.append("name: ").append(name).append('\n');
		builder.append("location: ").append(location).append('\n');

		for (Pair item : details)
		{
			builder.append("key: ").append(item.key).append('\n');
			builder.append("value: ").append(item.value).append('\n');
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
	        if (line.startsWith("name: "))
	        {
	        	name = line.substring(6);
	        }
	        else if (line.startsWith("location: "))
	        {
	        	location = line.substring(10);
	        }
	        else if (line.startsWith("key: "))
	        {
	        	curPair = new Pair();
	        	curPair.key = line.substring(5);
	        }
	        else if (line.startsWith("value: "))
	        {
	        	curPair.value = line.substring(7);
	        	details.add(curPair);
	        }
	    }
	}
}
