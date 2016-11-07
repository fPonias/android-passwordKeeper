package com.munger.passwordkeeper.struct;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.alert.AlertFragment;

public class PasswordDocumentFile extends PasswordDocument 
{
	private static String rootPath;
	
	public PasswordDocumentFile(MainActivity c, String name)
	{
		super(name);

		if (rootPath == null)
			rootPath = c.getFilesDir().getAbsolutePath() + "/";

		if (name.indexOf("/") != -1)
		{
			int idx = name.lastIndexOf("/");

			if (name.startsWith("/"))
				rootPath = "";

			rootPath += name.substring(0, idx + 1);
			this.name = name.substring(idx + 1);
		}
	}
	
	public PasswordDocumentFile(MainActivity c, String name, String password)
	{
		this(c, name);
		setPassword(password);
	}

	public boolean exists()
	{
		String path = rootPath + name;
		File target = new File(path);

		return target.exists();
	}

	public void save()
	{
		String path = rootPath + name;
		File target = new File(path);
		String content = toString(true);
		
		try
		{
			if (!target.exists())
				target.createNewFile();

			FileOutputStream fos = new FileOutputStream(target);
			fos.write(content.getBytes());
			fos.close();
		}
		catch(IOException e){
			AlertFragment inDialog = new AlertFragment("Unable to save file: " + name);
			inDialog.show(MainActivity.getInstance().getSupportFragmentManager(), "invalid_fragment");
		}
		
		lastLoad = System.currentTimeMillis();
	}

	public void load(boolean force)
	{
		String path = rootPath + name;
		File target = new File(path);
		

		long lastMod = target.lastModified();

		if (!force && lastLoad > lastMod)
			return;
		
		lastLoad = System.currentTimeMillis();
		
		
		details = new ArrayList<PasswordDetails>();
		BufferedReader reader = null;
		
		try
		{
			reader = new BufferedReader(new FileReader(target));

			String line;
			boolean first = true;
			while ((line = reader.readLine()) != null)
			{
	    		if (line.length() > 0)
	    		{
	    			String dec = encoder.decode(line);
	                
	                if (first)
	                {
	                	if (!dec.equals("test string"))
	                	{
	                		break;
	                	}
	                	
	                	first = false;
	                }
	                else
	                {
	                	PasswordDetails item = new PasswordDetails();
	                	item.fromString(dec);
						putDetails(item);
	                }
	            }
	        }
		}
		catch(IOException e){
			AlertFragment inDialog = new AlertFragment("Unable to load file: " + name);
			inDialog.show(MainActivity.getInstance().getSupportFragmentManager(), "invalid_fragment");
		}
		finally{
			if (reader != null)
				try{reader.close();} catch(IOException e){}
		}
	}
	
	public boolean testPassword()
	{
		String path = rootPath + name;
		File target = new File(path);
		BufferedReader reader = null;
		boolean ret = false;
		
		try
		{
			reader = new BufferedReader(new FileReader(target));
			
			//load up the details one line at a time
			String line = reader.readLine();
			if (line != null && line.length() > 0)
    		{
    			String dec = encoder.decode(line);
                if (dec.equals("test string"))
                {
                	ret = true;
                }
    		}
		}
		catch(IOException e){
			
		}
		finally{
			if (reader != null)
				try{reader.close();} catch(IOException e){}
		}
		
		return ret;
	}

	public void delete()
	{
		String path = rootPath + name;
		File target = new File(path);
		target.delete();
	}
	
	public static ArrayList<PasswordDocument> getList(MainActivity act)
	{
		ArrayList<PasswordDocument> ret = new ArrayList<PasswordDocument>();

		File f = new File(rootPath);
		String[] list = f.list();
		
		for (String item : list)
		{
			PasswordDocument i = new PasswordDocumentFile(act, item);
			ret.add(i);
		}
		
		return ret;
	}
}
