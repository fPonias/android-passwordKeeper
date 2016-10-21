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

import android.util.Log;

import com.munger.passwordkeeper.MainActivity;

public abstract class PasswordDocument 
{
	protected MainActivity context;
	protected AES256 encoder;
	protected long lastLoad;

	public ArrayList<PasswordDetails> details;
	protected int nextIndex;
	public String name;
	
	public PasswordDocument(MainActivity c, String name)
	{
		context = c;
		encoder = null;
		details = new ArrayList<PasswordDetails>();
		lastLoad = 0;
		nextIndex = 0;
		this.name = name;
	}
	
	public PasswordDocument(MainActivity c, String name, String password)
	{
		this(c, name);
		setPassword(password);
	}
	
	public void setIndex(PasswordDetails dets)
	{
		dets.index = Integer.valueOf(nextIndex).toString();
		nextIndex++;
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
					if (line.startsWith("location: ") && !line.equals("location: "))
					{
						if (dets != null)
						{
							dets.index = Integer.valueOf(details.size() + 1).toString();
							details.add(dets);
							i++;
						}
						
						dets = new PasswordDetails();
						dets.location = line.substring(10);
						dets.name = dets.location;
						dets.index = Integer.valueOf(i).toString();
					}
					else if (line.startsWith("name: "))
					{
						dets.name = line.substring(6);
					}
					else if (line.startsWith("\tkey: "))
					{
						pair = new PasswordDetails.Pair();
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
