package com.munger.passwordkeeper.struct;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
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

	protected PasswordDocumentHistory history;
	
	public PasswordDocument(String name)
	{
		encoder = null;
		details = new ArrayList<PasswordDetails>();
		detailsIndex = new HashMap<String, PasswordDetails>();
		history = new PasswordDocumentHistory();
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
		String output = "";
		if (encrypt)
		{
			String enc = encoder.encode("test string");
			output += enc + '\n';
		}

		String histOut = history.toString();

		if (encrypt)
		{

			histOut = encoder.encode(histOut);
		}

		output += histOut;
		return output;
	}

	public void fromString(String text, boolean decrypt) throws PasswordDocumentHistory.HistoryPlaybackException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
	{
		details = new ArrayList<PasswordDetails>();
	    if (decrypt)
	    {
			text = encoder.decode(text);

			if (!text.startsWith("test string"))
				return;

			text = text.substring(12);
			text = encoder.decode(text);
	    }

		history.fromString(text);
		playHistory();
	}

	public void playHistory() throws PasswordDocumentHistory.HistoryPlaybackException
	{
		history.playHistory(this, 0);

		for(PasswordDetails dets : details)
		{
			dets.setHistory(new PasswordDocumentHistory());
		}
	}
	
	abstract public void save() throws Exception;
	abstract public void load(boolean force) throws Exception;
	abstract public void delete() throws Exception;
	abstract public boolean testPassword();

	protected HashMap<String, PasswordDetails> detailsIndex;

	public int count()
	{
		return details.size();
	}

	public void putDetails(PasswordDetails dets)
	{
		details.add(dets);
		detailsIndex.put(dets.getId(), dets);
	}

	public static String emptyEntryTitle = "new entry";

	public PasswordDetails addEmptyEntry()
	{
		PasswordDetails det = new PasswordDetails();
		det.setName(emptyEntryTitle);
		putDetails(det);

		HistoryEventFactory.PasswordDetailsCreate evt = new HistoryEventFactory.PasswordDetailsCreate();
		evt.id = det.getId();
		history.addEvent(evt);

		return det;
	}

	public void playSubHistory(PasswordDocumentHistory subHistory) throws PasswordDocumentHistory.HistoryPlaybackException
	{
		subHistory.clean();
		subHistory.playHistory(this, 0);

		int sz = subHistory.count();
		for (int i = 0; i < sz; i++)
		{
			HistoryEventFactory.HistoryEvent evt = subHistory.getEvent(i);
			history.addEvent(evt);
		}
	}

	public void replaceDetails(PasswordDetails dets) throws PasswordDocumentHistory.HistoryPlaybackException
	{
		String detid = dets.getId();
		if (!detailsIndex.containsKey(detid))
			return;

		playSubHistory(dets.getHistory());
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
		String detid = dets.getId();
		if (!detailsIndex.containsKey(detid))
			return;

		PasswordDetails oldDets = detailsIndex.get(detid);
		int idx = details.indexOf(oldDets);
		details.remove(idx);

		detailsIndex.remove(detid);
	}

	public ArrayList<PasswordDetails> getDetailsList()
	{
		return details;
	}

	public void appendDocument(PasswordDocument doc) throws PasswordDocumentHistory.HistoryPlaybackException
	{
		playSubHistory(doc.history);
	}
}
