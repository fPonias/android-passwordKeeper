package com.munger.passwordkeeper.struct;

import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A struct that contains specific details for the PasswordDocument
 * Details are composed of a name, URL or location, and key/value pairs associated with this location.
 * Each key/value pair in meant to be a username/password pair or perhaps a question/answer pair.
 * All values can be blank if necessary.
 */
public class PasswordDetails implements Parcelable
{
	public String name;
	public String location;
	public ArrayList<Pair> details;
	
	/**
	 * index is a unique identifier for these details usable more by the containing password document.
	 * It became necessary as sometimes the entire data structure was rewritten on an edit
	 * and there was no way to link it to the original details object
	 */
	public String index;
	
	/**
	 * Key/Value pairs intrinsically linked to the base location of these details.
	 */
	public static class Pair
	{
		public String key;
		public String value;
		public String id;
		
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
		index = "0";
	}
	
	public void setIndex(int i)
	{
		index = Integer.valueOf(i).toString();
	}
	
	public String getIndex()
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
	
	/**
	 * check if the provided details are the same as this object's details
	 * @param dets the details to compare to this one's
	 * @return true if they are both the same and false if they differ.  This does not check if the index ids are the same.
	 */
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
	
	/**
	 * Convert the details to a string for easier debugging and encrypting.
	 */
	public String toString()
	{
		StringBuilder builder = new StringBuilder();
		builder.append("location: ").append(location).append('\n');
		builder.append("name: ").append(name).append('\n');
		builder.append("id: ").append(index).append('\n');

		for (Pair item : details)
		{
			builder.append("\tkey: ").append(item.key).append('\n');
			builder.append("\tvalue: ").append(item.value).append('\n');
		}
		
		return builder.toString();
	}

	/**
	 * decode a serialized string.
	 * Most likely used after decryption
	 * @param source
	 */
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
	        else if (line.startsWith("id: "))
	        {
	        	index = line.substring(4);
	        }
	        else if (line.startsWith("\tkey: "))
	        {
	        	curPair = new Pair();
	        	curPair.key = line.substring(6);
	        }
	        else if (line.startsWith("\tvalue: "))
	        {
	        	curPair.value = line.substring(8);
	        	details.add(curPair);
	        }
	    }
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel arg0, int arg1) 
	{
		arg0.writeString(toString());
	}
}
