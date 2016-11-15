package com.munger.passwordkeeper.struct;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import android.util.Log;

import com.munger.passwordkeeper.MainActivity;

public abstract class PasswordDocument 
{
	protected AES256 encoder;
	protected long lastLoad;

	protected ArrayList<PasswordDetails> details;
	public String name;

	protected PasswordDocumentHistory history;
	protected String mostRecentHistoryEvent = null;
	protected boolean historyLoaded = true;
	protected Object historyLoadedLock = new Object();
	
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

	public static abstract class ILoadEvents implements Comparable<ILoadEvents>
	{
		public static int counter = 1;
		private int id;
		public ILoadEvents()
		{
			counter++;
			id = counter;
		}

		@Override
		public int compareTo(ILoadEvents o)
		{
			return id - o.id;
		}

		public abstract void detailsLoaded();
		public abstract void historyLoaded();
		public abstract void historyProgress(float progress);
	}

	protected ConcurrentSkipListSet<ILoadEvents> loadEvents = new ConcurrentSkipListSet<>();

	public void addLoadEvents(ILoadEvents events)
	{
		synchronized (historyLoadedLock)
		{
			if (loadEvents.contains(events))
				return;

			loadEvents.add(events);
		}
	}

	public void removeLoadEvents(ILoadEvents events)
	{
		synchronized (historyLoadedLock)
		{
			if (loadEvents.contains(events))
				loadEvents.remove(events);
		}
	}

	public String deltasToString()
	{
		return history.toString();
	}

	protected void setHistoryLoaded()
	{
		synchronized(historyLoadedLock)
		{
			historyLoaded = true;

			for(ILoadEvents evt : loadEvents)
			{
				evt.historyLoaded();
			}
		}
	}

	protected void awaitHistoryLoaded()
	{
		synchronized (historyLoadedLock)
		{
			if (historyLoaded)
				return;
		}

		final ILoadEvents eventListener = new ILoadEvents()
		{
			public void historyProgress(float progress) {}
			public void detailsLoaded() {}

			public void historyLoaded()
			{
				synchronized (historyLoadedLock)
				{
					Log.d("password", "history loaded, sending notify signal");
					historyLoadedLock.notify();
				}
			}
		};
		final Thread t;

		synchronized (historyLoadedLock)
		{
			addLoadEvents(eventListener);

			t = new Thread(new Runnable() {public void run()
			{
				synchronized (historyLoadedLock)
				{
					try
					{
						if (historyLoaded)
							return;

						Log.d("password", "awaiting history loaded signal");
						historyLoadedLock.wait();
						Log.d("password", "history loaded signal received");
					}
					catch(InterruptedException e){
					}

					removeLoadEvents(eventListener);
				}
			}}, "Await History Load");
			t.start();
		}

		try
		{
			t.join();
		}
		catch(InterruptedException e){
		}
 	}

	public void deltasFromString(BufferedReader reader) throws IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
	{
		history = new PasswordDocumentHistory();
		historyLoaded = false;
		history.fromString(reader.readLine());
		setHistoryLoaded();
	}

	public void deltasToEncryptedString(DataOutput dos) throws IOException
	{
		awaitHistoryLoaded();

		writeLine(dos, "test string");

		long maxIdx = history.getSequenceCount();
		writeLine(dos, String.valueOf(maxIdx));

		ArrayList<Integer> sizes = new ArrayList<>();
		ArrayList<byte[]> entries = new ArrayList<>();

		byte[] lineEnc;
		int batchSize = 10;
		for (int i = 0; i < maxIdx; i += batchSize)
		{
			String line = history.partToString(i, batchSize);

			if (line != null)
			{
				lineEnc = encoder.encodeToBytes(line);
				sizes.add(lineEnc.length);
				entries.add(lineEnc);
			}
			else
				sizes.add(0);
		}

		String sizeHeader = "";
		for(int sz : sizes)
		{
			if (!sizeHeader.isEmpty())
				sizeHeader += ',';
			sizeHeader += sz;
		}

		writeLine(dos, sizeHeader);

		for(byte[] entry : entries)
		{
			dos.write(entry);
		}
	}

	public void deltasFromEncryptedString(DataInput inArr) throws IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
	{
		historyLoaded = false;
		history = new PasswordDocumentHistory();
		String text = readLine(inArr);

		if (!text.equals("test string"))
			return;

		text = readLine(inArr);
		long seq = Long.parseLong(text);
		history.setSequenceCount(seq);

		text = readLine(inArr);
		String[] sizeArr = text.split(",");

		for(String sizeStr : sizeArr)
		{
			int sz = Integer.parseInt(sizeStr);

			if (sz > 0)
			{
				text = readLine(inArr, sz);
				history.partFromString(text);
			}
		}

		setHistoryLoaded();
	}

	private String readLine(DataInput inArr, int sz) throws IOException
	{
		byte[] lineEnc = new byte[sz];
		inArr.readFully(lineEnc);
		String ret = encoder.decodeFromBytes(lineEnc);
		return ret;
	}

	private String readLine(DataInput inArr) throws IOException
	{
		int sz = inArr.readInt();
		return readLine(inArr, sz);
	}

	private void writeLine(DataOutput dos, String line) throws IOException
	{
		byte[] enc = encoder.encodeToBytes(line);
		dos.writeInt(enc.length);
		dos.write(enc);
	}

	public String detailsToString()
	{
		StringBuilder output = new StringBuilder();

		String line = "mostRecentHistoryEvent: " + mostRecentHistoryEvent;
		output.append(line).append('\n');

		for(PasswordDetails dets : details)
		{
			line = dets.toString();
			output.append(line).append('\n');
		}

		return output.toString();
	}

	public void detailsToEncryptedString(DataOutput dos) throws IOException
	{
		writeLine(dos, "test string");

		String line = (mostRecentHistoryEvent != null) ? mostRecentHistoryEvent : "null";
		writeLine(dos, line);

		int sz = details.size();
		writeLine(dos, String.valueOf(sz));

		for(PasswordDetails dets : details)
		{
			line = dets.toString();
			writeLine(dos, line);
		}
	}

	public void fromDetailsString(BufferedReader reader) throws IOException
	{
		details = new ArrayList<>();
		mostRecentHistoryEvent = null;
		int count = -1;

		String line;
		while((line = reader.readLine()) != null)
		{
			count++;

			if (count == 0)
			{
				mostRecentHistoryEvent = (line.equals("null")) ? null : line;
			}
			else
			{
				PasswordDetails dets = new PasswordDetails();
				dets.fromString(line);
				dets.setHistory(new PasswordDocumentHistory());

				putDetails(dets);
			}
		}
	}

	public void detailsFromEncryptedString(DataInput dis) throws IOException
	{
		details = new ArrayList<>();
		mostRecentHistoryEvent = null;

		String test = readLine(dis);
		if (!test.equals("test string"))
			return;

		String line = readLine(dis);
		mostRecentHistoryEvent = line;

		line = readLine(dis);
		int sz = Integer.parseInt(line);

		for (int i = 0; i < sz; i++)
		{
			line = readLine(dis);

			PasswordDetails dets = new PasswordDetails();
			dets.fromString(line);
			dets.setHistory(new PasswordDocumentHistory());

			putDetails(dets);
		}
	}

	public void playHistory() throws PasswordDocumentHistory.HistoryPlaybackException
	{
		awaitHistoryLoaded();
		history.playHistory(this, 0);

		for(PasswordDetails dets : details)
		{
			dets.setHistory(new PasswordDocumentHistory());
		}

		int sz = history.count();
		if (sz > 0)
			mostRecentHistoryEvent = history.getEvent(sz - 1).getIDSignature();
		else
			mostRecentHistoryEvent = null;
	}

	public interface LoadUpdate
	{
		public void callback(float progress);
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

		awaitHistoryLoaded();
		HistoryEventFactory.PasswordDetailsCreate evt = new HistoryEventFactory.PasswordDetailsCreate();
		evt.id = det.getId();
		history.addEvent(evt);

		return det;
	}

	public void playSubHistory(PasswordDocumentHistory subHistory) throws PasswordDocumentHistory.HistoryPlaybackException
	{
		awaitHistoryLoaded();
		subHistory.clean();
		subHistory.playHistory(this, 0);

		int sz = subHistory.count();
		for (int i = 0; i < sz; i++)
		{
			HistoryEventFactory.HistoryEvent evt = subHistory.getEvent(i);
			history.addEvent(evt);
		}

		if (sz > 0)
			mostRecentHistoryEvent = subHistory.getEvent(sz - 1).getIDSignature();
	}

	public void replaceDetails(PasswordDetails dets) throws PasswordDocumentHistory.HistoryPlaybackException
	{
		awaitHistoryLoaded();
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
		awaitHistoryLoaded();
		playSubHistory(doc.history);
	}
}
