package com.munger.passwordkeeper.struct;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.alert.AlertFragment;

public class PasswordDocumentFile extends PasswordDocument 
{
	private static String rootPath;
	
	public PasswordDocumentFile(String name)
	{
		super(name);

		if (rootPath == null)
			rootPath = MainActivity.getInstance().getFilesDir().getAbsolutePath() + "/";

		if (name.indexOf("/") != -1)
		{
			int idx = name.lastIndexOf("/");

			if (name.startsWith("/"))
				rootPath = "";

			rootPath += name.substring(0, idx + 1);
			this.name = name.substring(idx + 1);
		}
	}
	
	public PasswordDocumentFile(String name, String password)
	{
		this(name);
		setPassword(password);
	}

	public boolean exists()
	{
		String path = rootPath + name;
		File target = new File(path);

		return target.exists();
	}

	public void save() throws IOException
	{
		String path = rootPath + name;
		File target = new File(path);
		String content = toString(true);

		if (!target.exists())
			target.createNewFile();

		FileOutputStream fos = new FileOutputStream(target);
		fos.write(content.getBytes());
		fos.close();
		
		lastLoad = System.currentTimeMillis();
	}

	private LoadUpdate loadUpdate = null;

	public void load(final LoadUpdate update) throws IOException, PasswordDocumentHistory.HistoryPlaybackException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
	{
		loadUpdate = update;
		load(false);
		loadUpdate = null;
	}

	public void load(boolean force) throws IOException, PasswordDocumentHistory.HistoryPlaybackException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
	{
		String path = rootPath + name;
		File target = new File(path);
		

		long lastMod = target.lastModified();

		if (!force && lastLoad > lastMod)
			return;
		
		lastLoad = System.currentTimeMillis();
		
		
		details = new ArrayList<PasswordDetails>();
		BufferedReader reader = null;

		reader = new BufferedReader(new FileReader(target));

		String line = reader.readLine();
		if (line != null && line.length() > 0)
		{
			String dec = encoder.decode(line);

			if (!dec.equals("test string"))
				return;
		}

		line = reader.readLine();
		if (line != null && line.length() > 0)
		{
			ThreadedCallbackWaiter decodeCallback = new ThreadedCallbackWaiter(new ThreadedCallbackWaiter.Callback() {public void callback(float progress)
			{
				if (loadUpdate != null)
					loadUpdate.callback(progress);
			}});

			String dec = encoder.decode(line, decodeCallback);

			decodeCallback.CleanUp();

			history = new PasswordDocumentHistory();
			history.fromString(dec);
			playHistory();
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
	
	public static ArrayList<PasswordDocument> getList()
	{
		ArrayList<PasswordDocument> ret = new ArrayList<PasswordDocument>();

		File f = new File(rootPath);
		String[] list = f.list();
		
		for (String item : list)
		{
			PasswordDocument i = new PasswordDocumentFile(item);
			ret.add(i);
		}
		
		return ret;
	}
}
