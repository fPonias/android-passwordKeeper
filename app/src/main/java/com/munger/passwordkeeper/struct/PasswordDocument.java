package com.munger.passwordkeeper.struct;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;

import android.util.Log;

import com.munger.passwordkeeper.MainActivity;

public abstract class PasswordDocument 
{
	protected AES256 encoder;
	protected long lastLoad;

	protected ArrayList<PasswordDetails> details;
	public String name;
	
	public PasswordDocument(String name)
	{
		encoder = null;
		details = new ArrayList<PasswordDetails>();
		detailsIndex = new HashMap<String, PasswordDetails>();
		lastLoad = 0;
		this.name = name;
	}
	
	public PasswordDocument(String name, String password)
	{
		this(name);
		setPassword(password);
	}

	public void setPassword(String password)
	{
		encoder = new AES256(password);
	}

	public String toString()
	{
		return toString(false);
	}

	public String toString(boolean encrypt)
	{
		StringBuilder builder = new StringBuilder();
		
		if (encrypt)
		{
			String enc = encoder.encode("test string");
			builder.append(enc).append('\n');
		}

		for (PasswordDetails det : details)
		{
			String line = det.toString();
			
			if (encrypt)
			{
				String enc = encoder.encode(line);
				builder.append(enc).append('\n');
			}
			else
			{
				builder.append(line);
			}
		}
		
		return builder.toString();
	}

	public void fromString(String text, boolean decrypt)
	{
		details = new ArrayList<PasswordDetails>();
	    if (decrypt)
	    {
	    	String[] parts = text.split("\n");
	    	int sz = parts.length;
	    	for (int i = 0; i < sz; i++)
	    	{
	    		if (parts[i].length() > 0)
	    		{
	    			String dec = encoder.decode(parts[i]);

	                if (i == 0)
	                {
	                	if (!dec.equals("test string"))
	                		return;
	                }
	                else
	                {
	                	PasswordDetails item = new PasswordDetails();
	                	item.fromString(dec);
	                	details.add(item);
	                }
	            }
	        }
	    }
	    else
	    {
	    	try
	    	{
		    	ByteArrayInputStream bais = new ByteArrayInputStream(text.getBytes());
		    	importFromStream(bais);
		    	bais.close();
	    	}
	    	catch(IOException e){
	    		Log.v("password", "failed to import encoded string");
	    	}
	    }
	}
	
	abstract public void save();
	abstract public void load(boolean force);
	abstract public void delete();
	abstract public boolean testPassword();

	protected HashMap<String, PasswordDetails> detailsIndex;

	public int count()
	{
		return details.size();
	}

	public void putDetails(PasswordDetails dets)
	{
		putDetails(dets, false);
	}

	public void putDetails(PasswordDetails dets, boolean keepId)
	{
		if (!keepId)
			dets.id = generateId();

		details.add(dets);
		detailsIndex.put(dets.id, dets);
	}

	public static String emptyEntryTitle = "new entry";

	public PasswordDetails addEmptyEntry()
	{
		PasswordDetails det = new PasswordDetails();
		det.name = emptyEntryTitle;
		putDetails(det);

		return det;
	}

	private String generateId()
	{
		long timestamp = System.currentTimeMillis();
		long rand = (long) Math.floor(Math.random() * 10000.0);
		String ret = timestamp + "-" + rand;
		return ret;
	}

	public void replaceDetails(PasswordDetails dets)
	{
		if (!detailsIndex.containsKey(dets.id))
			return;

		PasswordDetails oldDets = detailsIndex.get(dets.id);
		int idx = details.indexOf(oldDets);
		details.remove(idx);
		details.add(idx, dets);

		detailsIndex.remove(dets.id);
		detailsIndex.put(dets.id, dets);
	}

	public PasswordDetails getDetails(int index)
	{
		return details.get(index);
	}

	public PasswordDetails getDetails(String id)
	{
		return detailsIndex.get(id);
	}

	public void removeDetails(PasswordDetails dets)
	{
		if (!detailsIndex.containsKey(dets.id))
			return;

		PasswordDetails oldDets = detailsIndex.get(dets.id);
		int idx = details.indexOf(oldDets);
		details.remove(idx);

		detailsIndex.remove(dets.id);
	}

	public ArrayList<PasswordDetails> getDetailsList()
	{
		return details;
	}

	public void importFromFile(String path) throws FileNotFoundException, IOException
	{
		FileInputStream fis = new FileInputStream(new File(path));
		
		IOException ret = null;
		
		try
		{
			importFromStream(fis);
		}
		catch(IOException e){
			ret = e;
		}
		finally{
			fis.close();
		}
		
		if (ret != null)
			throw(ret);
	}

	public void importFromStream(InputStream stream) throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		details = new ArrayList<PasswordDetails>();
		PasswordDetails dets = null;
		PasswordDetails.Pair pair = null;
		
		String line;
		int i = 0;
		IOException ret = null;
		try
		{
			while ((line = reader.readLine()) != null)
			{
				if (line.length() > 0)
				{
					if (line.startsWith("id: ") && !line.equals("id: "))
					{
						if (dets != null)
						{
							details.add(dets);
							i++;
						}
						
						dets = new PasswordDetails();
						dets.id = line.substring(5);

						detailsIndex.put(dets.id, dets);
					}
					if (line.startsWith("location: "))
					{
						dets.location = line.substring(10);
						dets.name = dets.location;
					}
					else if (line.startsWith("name: "))
					{
						dets.name = line.substring(6);
					}
					else if (line.startsWith("pairid: "))
					{
						pair = new PasswordDetails.Pair();
						pair.id = line.substring(9);

						String key = dets.id + ":" + pair.id;
					}
					else if (line.startsWith("\tkey: "))
					{
						pair.key = line.substring(6);
					}
					else if (line.startsWith("\tvalue: "))
					{
						pair.value = line.substring(8);
						dets.details.add(pair);
					}
					else
					{
						throw new IOException("Couldn't parse import file");
					}
				}
			}
		}
		catch(IOException e){
			ret = e;
		}
		
		if (ret != null)
			throw(ret);
	}
}
