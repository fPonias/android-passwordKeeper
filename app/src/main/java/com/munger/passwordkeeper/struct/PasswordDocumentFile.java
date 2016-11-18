package com.munger.passwordkeeper.struct;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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

	public void onClose()
	{
	}

	public boolean exists()
	{
		String path = rootPath + name;
		File target = new File(path);

		return target.exists();
	}

	public void save() throws IOException
	{
		saveHistory();
		saveDetails();
		lastLoad = System.currentTimeMillis();
	}

	private void saveDetails() throws IOException
	{
		String path = rootPath + name;
		File target = new File(path);
		if (!target.exists())
			target.createNewFile();

		FileOutputStream fis = new FileOutputStream(target);
		DataOutputStream dis = new DataOutputStream(fis);
		detailsToEncryptedString(dis);

		dis.flush();
		dis.close();
		fis.close();
	}

	private void saveHistory() throws IOException
	{
		String path = rootPath + name + "-history";
		File target = new File(path);

		if (!target.exists())
		{
			saveNewHistory(target);
		}
		else
		{
			saveUpdatedHistory(target);
		}
	}

	protected void saveNewHistory(File target) throws IOException
	{
		if (target.exists())
			target.delete();

		target.createNewFile();

		FileOutputStream fos = new FileOutputStream(target);
		DataOutputStream dos = new DataOutputStream(fos);

		deltasToEncryptedString(dos);

		dos.flush();
		dos.close();
		fos.close();
	}

	protected void saveUpdatedHistory(File target) throws IOException
	{
		FileInputStream fis = new FileInputStream(target);
		DataInputStream dis = new DataInputStream(fis);

		String tmpPath = rootPath + name + "-history-tmp";
		File tmpTarget = new File(tmpPath);

		if (tmpTarget.exists())
			tmpTarget.delete();

		tmpTarget.createNewFile();

		FileOutputStream fos = new FileOutputStream(tmpTarget);
		DataOutputStream dos = new DataOutputStream(fos);

		updateEncryptedDeltas(dis, dos);

		dis.close();
		dos.flush();
		dos.close();
		fos.close();

		replaceWithTemp(target, tmpTarget);
	}

	protected void replaceWithTemp(File target, File tmpTarget) throws IOException
	{
		target.delete();

		FileChannel inChannel = new FileInputStream(tmpTarget).getChannel();
		FileChannel outChannel = new FileOutputStream(target).getChannel();
		outChannel.transferFrom(inChannel, 0, inChannel.size());
		inChannel.close();
		outChannel.close();

		tmpTarget.delete();
	}

	public void load(boolean force) throws IOException, PasswordDocumentHistory.HistoryPlaybackException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
	{
		String path = rootPath + name;
		File target = new File(path);

		long lastMod = target.lastModified();

		if (!force && lastLoad > lastMod)
			return;

		lastLoad = System.currentTimeMillis();
		
		loadDetails();

		for(ILoadEvents evt : loadEvents)
			evt.detailsLoaded();

		loadHistory();
		setHistoryLoaded();
	}

	private void loadDetails() throws IOException
	{
		String path = rootPath + name;
		File target = new File(path);
		FileInputStream fis = new FileInputStream(target);
		DataInputStream dis = new DataInputStream(fis);

		details = new ArrayList<PasswordDetails>();
		detailsFromEncryptedString(dis);

		dis.close();
		fis.close();
	}

	private void loadHistory() throws IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
	{
		historyLoaded = false;
		String path = rootPath + name + "-history";
		FileInputStream fis = new FileInputStream(path);
		DataInputStream dis = new DataInputStream(fis);

		history = new PasswordDocumentHistory();
		deltasFromEncryptedString(dis);

		dis.close();
		fis.close();
	}
	
	public boolean testPassword()
	{
		String path = rootPath + name;
		File target = new File(path);
		FileInputStream fis = null;
		DataInputStream dis = null;
		boolean ret = false;
		
		try
		{
			fis = new FileInputStream(target);
			dis = new DataInputStream(fis);

			int sz = dis.readInt();
			byte[] lineEnc = new byte[sz];
			dis.read(lineEnc);
			String line = encoder.decodeFromBytes(lineEnc);

			if (line.equals("test string"))
			{
				ret = true;
			}
		}
		catch(IOException e){
			Log.d("password", "Failed to open password file");
		}
		finally{
			try
			{
				if (dis != null)
					dis.close();
				if (fis != null)
					fis.close();
			}
			catch(Exception e){}
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
