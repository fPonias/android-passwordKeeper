package com.munger.passwordkeeper.struct.documents;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import com.munger.passwordkeeper.struct.AES256;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;

public class PasswordDocumentFile extends PasswordDocument 
{
	protected String rootPath = ".";
	
	public PasswordDocumentFile(String name)
	{
		super(name);
	}
	
	public PasswordDocumentFile(String name, String password)
	{
		this(name);
		setPassword(password);
	}

	@Override
	public void setPassword(String password)
	{
		super.setPassword(password);

		String path = getDetailsFilePath();
		File target = new File(path);

		if (!target.exists() || target.length() == 0)
		{
                    setHistoryLoaded();
		}

	}

        public String getRootPath()
        {
            return rootPath;
        }
                
	public void setRootPath(String value)
	{
		rootPath = value;

		if (name.indexOf("/") != -1)
		{
			int idx = name.lastIndexOf("/");

			if (name.startsWith("/"))
				rootPath = "";

			rootPath += name.substring(0, idx + 1);
			this.name = name.substring(idx + 1);
		}
	}

	public void changePassword(String password) throws Exception
	{
		PasswordDocumentFile tmp = new PasswordDocumentFile("tmp", password);
		tmp.playSubHistory(getHistory());
		tmp.save();

		replaceWithTemp(new File(getHistoryFilePath()), new File(tmp.getHistoryFilePath()));
		replaceWithTemp(new File(getDetailsFilePath()), new File(tmp.getDetailsFilePath()));

		tmp.delete();

		setPassword(password);
	}

	public void onClose()
	{
	}

	public boolean exists()
	{
		String path = getDetailsFilePath();
		File target = new File(path);

		return target.exists();
	}

	public String getDetailsFilePath()
	{
		return rootPath + name;
	}

	public String getHistoryFilePath()
	{
		return rootPath + name + "-history";
	}

	public String getDetailsTmpFilePath()
	{
		return rootPath + name + "-tmp";
	}

	public String getHistoryTmpFilePath()
	{
		return rootPath + name + "-history-tmp";
	}

	public void onSave() throws IOException
	{
		File hist = new File(getHistoryFilePath());
		if (!hist.exists())
			setHistoryLoaded();

		saveTempHistory();
		saveTempDetails();

		//save copying for last to minimize data corruption
		replaceWithTemp(new File(getHistoryFilePath()), new File(getHistoryTmpFilePath()));
		replaceWithTemp(new File(getDetailsFilePath()), new File(getDetailsTmpFilePath()));

		lastLoad = System.currentTimeMillis();
	}

	private void saveTempDetails() throws IOException
	{
		String path = getDetailsTmpFilePath();
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

	private void saveTempHistory() throws IOException
	{
		String tmpPath = getHistoryTmpFilePath();
		String path = getHistoryFilePath();
		File target = new File(path);

		if (!target.exists())
		{
			saveNewHistory();
		}
		else
		{
			saveUpdatedHistory();
		}
	}

	protected void saveNewHistory() throws IOException
	{
		String path = getHistoryTmpFilePath();
		File target = new File(path);

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

	protected void saveUpdatedHistory() throws IOException
	{
		String path = getHistoryFilePath();
		File target = new File(path);

		FileInputStream fis = new FileInputStream(target);
		DataInputStream dis = new DataInputStream(fis);

		String tmpPath = getHistoryTmpFilePath();
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

	public void onLoad(boolean force) throws IOException, PasswordDocumentHistory.HistoryPlaybackException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
	{
		String path = getDetailsFilePath();
		File target = new File(path);

		if (!target.exists() || target.length() == 0)
		{
			setHistoryLoaded();
			return;
		}

		long lastMod = target.lastModified();

		if (!force && lastLoad > lastMod)
			return;

		lastLoad = System.currentTimeMillis();

		loadHistory();
		setHistoryLoaded();

		loadDetails();

		for(ILoadEvents evt : loadEvents)
			evt.detailsLoaded();
	}

	private void loadDetails() throws IOException
	{
		String path = getDetailsFilePath();
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
		String path = getHistoryFilePath();
		File f = new File(path);
		long sz = f.length();
		FileInputStream fis = new FileInputStream(path);
		DataInputStream dis = new DataInputStream(fis);

		history = new PasswordDocumentHistory();
		deltasFromEncryptedString(dis, sz);

		dis.close();
		fis.close();
	}
	
	public boolean testPassword()
	{
		String path = getDetailsFilePath();
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
			String line = encoder.decodeFromByteArray(lineEnc);

			if (line.equals("test string"))
			{
				ret = true;
			}
		}
		catch(IOException e){
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

	public void onDelete()
	{
		String path = getDetailsFilePath();
		File target = new File(path);
		target.delete();

		path = getHistoryFilePath();
		target = new File(path);
		target.delete();
	}
}
