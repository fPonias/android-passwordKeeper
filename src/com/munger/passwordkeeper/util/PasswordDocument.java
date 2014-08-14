package com.munger.passwordkeeper.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;

public class PasswordDocument 
{
	private Context context;
	private AES256 encoder;
	private long lastLoad;

	public ArrayList<PasswordDetails> details;
	
	public PasswordDocument(Context c)
	{
		context = c;
		encoder = null;
		details = new ArrayList<PasswordDetails>();
		lastLoad = 0;
	}
	
	public PasswordDocument(Context c, String password)
	{
		context = c;
		encoder = new AES256(password);
		details = new ArrayList<PasswordDetails>();
		lastLoad = 0;
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
				builder.append(line).append("\n*****\n");
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
	    	String[] parts = text.split("\n*****\n");
	    	for (String part : parts)
	    	{
	    		if (part.length() > 0)
	    		{
	    			PasswordDetails item = new PasswordDetails();
	    			item.fromString(part);
	    			details.add(item);
	            }
	        }
	    }
	}
	
	public void saveToFile(String name) throws IOException
	{
		String path = context.getFilesDir().getAbsolutePath() + "/saved/" + name;
		File target = new File(path);
		String content = toString(true);
		FileOutputStream fos = new FileOutputStream(target);
		fos.write(content.getBytes());
		fos.close();
		
		lastLoad = System.currentTimeMillis();
	}
	
	public void forceLoadFromFile(String name) throws FileNotFoundException, IOException
	{
		loadFromFile(name, true);
	}
	
	public void loadFromFile(String name) throws FileNotFoundException, IOException
	{
		loadFromFile(name, false);
	}
	
	public void loadFromFile(String name, boolean force) throws FileNotFoundException, IOException
	{
		String path = context.getFilesDir().getAbsolutePath() + "/saved/" + name;
		File target = new File(path);
		

		long lastMod = target.lastModified();
		
		if (!force && lastLoad > lastMod)
			return;
		
		lastLoad = System.currentTimeMillis();
		

		details = new ArrayList<PasswordDetails>();
		BufferedReader reader = new BufferedReader(new FileReader(target));
		
		String line;
		boolean first = true;
		int i = 0;
		while ((line = reader.readLine()) != null)
		{
    		if (line.length() > 0)
    		{
    			String dec = encoder.decode(line);
                
                if (first)
                {
                	if (!dec.equals("test string"))
                	{
                		reader.close();
                		return;
                	}
                	
                	first = false;
                }
                else
                {
                	PasswordDetails item = new PasswordDetails();
                	item.fromString(dec);
                	details.add(item);
                	item.setIndex(i);
                	i++;
                }
            }
        }
		
		reader.close();
	}
	
	public void importFromFile(String path) throws FileNotFoundException, IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(path));
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
							details.add(dets);
							i++;
						}
						
						dets = new PasswordDetails();
						dets.location = line.substring(10);
						dets.name = dets.location;
						dets.index = i;
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
		finally{
			reader.close();
		}
		
		if (ret != null)
			throw(ret);
	}
	
	public static void deleteFile(Context c, String name)
	{
		String path = c.getFilesDir().getAbsolutePath() + "/saved/" + name;
		File target = new File(path);
		target.delete();
	}
	
	public static boolean testPassword(Context c, String name, String password)
	{
		if (password.length() == 0)
			return false;
		
		String path = c.getFilesDir().getAbsolutePath() + "/saved/" + name;
		File target = new File(path);
		boolean ret = false;
		BufferedReader reader = null;
		AES256 encoder = new AES256(password);
		
		try
		{
			reader = new BufferedReader(new FileReader(target));
			
			String line = reader.readLine();
			
    		if (line.length() > 0)
    		{
    			String dec = encoder.decode(line);
    			if (dec.equals("test string"))
    				ret = true;
    		}
		}
		catch(Exception e){
		}
		finally{
			if (reader != null)
				try{reader.close();}catch(Exception e){}
		}
		
		return ret;
	}
}
