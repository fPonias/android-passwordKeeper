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
		super(c, name);
		
		if (rootPath == null)
			rootPath = c.getFilesDir().getAbsolutePath() + "/";
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
			inDialog.show(context.getSupportFragmentManager(), "invalid_fragment");
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
	                		break;
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
		}
		catch(IOException e){
			AlertFragment inDialog = new AlertFragment("Unable to load file: " + name);
			inDialog.show(context.getSupportFragmentManager(), "invalid_fragment");
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
