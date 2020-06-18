package com.munger.passwordkeeper.struct.documents;

import com.munger.passwordkeeper.struct.AES256;
import com.munger.passwordkeeper.struct.IEncoder;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PlainText;
import com.munger.passwordkeeper.struct.history.HistoryEvent;
import com.munger.passwordkeeper.struct.history.HistoryEventFactory;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

public abstract class PasswordDocument 
{
	protected IEncoder encoder;
	protected long lastLoad;

	protected ArrayList<PasswordDetails> details;
	protected HashMap<String, PasswordDocumentHistory.HistoryEventListener> detailsListeners;
	public String name;
	protected TreeSet<ILoadEvents> loadEvents = new TreeSet<>();
	protected EncoderType encoderType = EncoderType.AES_SHA;


	protected PasswordDocumentHistory history;
	protected String mostRecentHistoryEvent = null;
	protected boolean historyLoaded = false;
	protected Object historyLoadedLock = new Object();


	protected final int HISTORY_BATCH_SIZE = 10;
	protected final String testString = "test string";
	public static final String emptyEntryTitle = "new entry";
	public static final String defaultName = "passwords";

	public enum EncoderType
	{
		AES_MD5,
		AES_SHA,
		PLAINTEXT
	}

	public PasswordDocument()
	{
		encoder = null;
		details = new ArrayList<PasswordDetails>();
		detailsListeners = new HashMap<>();
		detailsIndex = new HashMap<String, PasswordDetails>();
		history = new PasswordDocumentHistory();
		lastLoad = 0;
		this.name = defaultName;
	}

	public PasswordDocument(String name)
	{
		this();
		this.name = name;
	}
	
	public PasswordDocument(String name, String password)
	{
		this(name);
		setPassword(password);
	}

	public void setPassword(String password)
	{
		if (encoderType == EncoderType.AES_SHA)
			encoder = new AES256(password, AES256.HashType.SHA);
		else if (encoderType == EncoderType.AES_MD5)
			encoder = new AES256(password, AES256.HashType.MD5);
		else
			encoder = new PlainText(password);
	}

	public void setEncoderType(EncoderType encoderType)
	{
		this.encoderType = encoderType;
	}

	public EncoderType getHashType()
	{
		return encoderType;
	}

	public void changePassword(String password) throws Exception
	{
		setPassword(password);
	}

	IEncoder getEncoder()
	{
		return encoder;
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

	protected void setHistoryUpdate(float progress)
	{
		synchronized (historyLoadedLock)
		{
			for(ILoadEvents evt : loadEvents)
			{
				evt.historyProgress(progress);
			}
		}
	}

        public boolean isHistoryLoaded()
        {
            return historyLoaded;
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

						historyLoadedLock.wait();
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

	public void deltasFromString(String str) throws IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
	{
		history = new PasswordDocumentHistory();
		historyLoaded = false;
		history.fromString(str);
		setHistoryLoaded();
	}

	public void updateEncryptedDeltas(DataInput dis, DataOutput dos) throws IOException
	{
		awaitHistoryLoaded();

		String line = readLine(dis);
		if (!line.equals(testString))
			return;

		long idx = dis.readLong();
		long inBatch = idx / HISTORY_BATCH_SIZE;
		long outBatch = history.getSequenceCount() / HISTORY_BATCH_SIZE;


		writeLine(dos, testString);

		long maxIdx = history.getSequenceCount();
		dos.writeLong(maxIdx);


		if (inBatch > outBatch)
			return;

		for (int i = 0; i < inBatch; i++)
		{
			int index = dis.readInt();
			int sz = dis.readInt();

			transferBatch(index, sz, dis, dos);
		}

		for (int j = (int) inBatch * HISTORY_BATCH_SIZE; j < maxIdx; j += HISTORY_BATCH_SIZE)
		{
			writeBatch(j, dos);
		}
	}

	private void transferBatch(int index, int sz, DataInput dis, DataOutput dos) throws IOException
	{
		dos.writeInt(index);

		if (sz == 0)
		{
			dos.writeInt(0);
			return;
		}

		byte[] hash = new byte[32];
		dis.readFully(hash);
		String hashStr = new String(hash);
		byte[] lineEnc = new byte[sz];
		dis.readFully(lineEnc);

		String batchLine = history.partToString(index, HISTORY_BATCH_SIZE);
		String batchHash = encoder.hash(batchLine);

		if (batchHash.equals(hashStr))
		{
			dos.writeInt(sz);
			dos.write(hash);
			dos.write(lineEnc);
		}
		else
		{
			lineEnc = encoder.encodeToBytes(batchLine);

			dos.writeInt(lineEnc.length);
			dos.writeBytes(batchHash);
			dos.write(lineEnc);
		}
	}

	public void deltasToEncryptedString(DataOutput dos) throws IOException
	{
		awaitHistoryLoaded();

		writeLine(dos, testString);

		long maxIdx = history.getSequenceCount();
		dos.writeLong(maxIdx);

		for (int i = 0; i < maxIdx; i += HISTORY_BATCH_SIZE)
		{
			System.out.println("writing batch " + i + " to password history");
			writeBatch(i, dos);
		}
	}

	protected void writeBatch(int idx, DataOutput dos) throws IOException
	{
		String line = history.partToString(idx, HISTORY_BATCH_SIZE);

		if (line != null && line != "")
		{
			byte[] lineEnc = encoder.encodeToBytes(line);
			String hash = encoder.hash(line);

			dos.writeInt(idx);
			dos.writeInt(lineEnc.length);
			dos.writeBytes(hash);
			dos.write(lineEnc);
		}
		else
		{
			dos.writeInt(idx);
			dos.writeInt(0);
		}
	}

	public void deltasFromEncryptedString(DataInput inArr, long maxSz) throws PasswordDocument.IncorrectPasswordException, IOException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException
	{
		long total = 0;
		historyLoaded = false;
		history = new PasswordDocumentHistory();

		String line = readLine(inArr);
		System.out.println("document test line: " + line);
		if (!line.equals(testString))
			throw new IncorrectPasswordException();

		total += line.length() + 1;
		long idx = inArr.readLong();
		history.setSequenceCount(idx);
		total += 8;

		int hashSize = encoder.hashSize();
		int count = 0;
		int i = 0;
		while (true)
		{
			float progress = (float) i / (float) (idx);
			setHistoryUpdate(progress);

			try
			{
				i = inArr.readInt();
			}
			catch(EOFException e){
				break;
			}

			int sz = inArr.readInt();
			total += 4;

			if (maxSz > -1 && total + sz > maxSz)
				throw new IOException("parse error on history document");

			if (sz > 0)
			{
				byte[] hash = new byte[hashSize];
				inArr.readFully(hash);
				String hashStr = new String(hash);
				String batchLine = readLine(inArr, sz);
				history.partFromString(batchLine);
			}

			count++;
		}

		setHistoryUpdate(1.0f);
		setHistoryLoaded();
	}

	private String readLine(DataInput inArr, int sz) throws IOException
	{
		byte[] lineEnc = new byte[sz];
		inArr.readFully(lineEnc);
		String ret = encoder.decodeFromByteArray(lineEnc);

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
		writeLine(dos, testString);

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

	public void detailsFromString(BufferedReader reader) throws IOException
	{
		details = new ArrayList<>();
		mostRecentHistoryEvent = null;
		int count = -1;
		PasswordDetails lastDets = null;

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
				if (line.startsWith("id:"))
				{
					if (lastDets != null)
						try{addDetails(lastDets);}catch(Exception e){}

					lastDets = new PasswordDetails();
					lastDets.fromToken(line);
					lastDets.setHistory(new PasswordDocumentHistory());
				}
				else
				{
					lastDets.fromToken(line);
				}
			}
		}

		if (lastDets != null)
			try{addDetails(lastDets);}catch(Exception e){}
	}

	public void detailsFromEncryptedString(DataInput dis) throws IOException
	{
		details = new ArrayList<>();
		mostRecentHistoryEvent = null;

		String test = readLine(dis);
		if (!test.equals(testString))
			throw new IOException("incorrect password");

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

			try{addDetails(dets);}catch(Exception e){}
		}
	}

	public void playHistory() throws PasswordDocumentHistory.PlaybackException
	{
		awaitHistoryLoaded();
		history.playHistory(this);

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

	protected boolean initting = false;

	public static interface DocumentEvents
	{
		void initFailed(Exception e);
		void initted();
		void saved();
		void loaded();
		void deleted();
		void closed();
	}

	protected ArrayList<DocumentEvents> listeners = new ArrayList<>();
	public void addListener(DocumentEvents listener)
	{
		listeners.add(listener);
	}

	public void removeListener(DocumentEvents listener)
	{
		listeners.remove(listener);
	}

	public void removeListeners()
	{
		listeners = new ArrayList<>();
	}

	protected void notifyInitted()
	{
		if(!initting)
			return;

		initting = false;
		int sz = listeners.size();
		for (int i = sz - 1; i >= 0; i--)
			listeners.get(i).initted();
	}

	protected void notifyInitError(Exception e)
	{
		initting = false;
		int sz = listeners.size();
		for (int i = sz - 1; i >= 0; i--)
			listeners.get(i).initFailed(e);
	}

	protected void notifySaved()
	{
		int sz = listeners.size();
		for (int i = sz - 1; i >= 0; i--)
			listeners.get(i).saved();
	}

	protected void notifyLoaded()
	{
		int sz = listeners.size();
		for (int i = sz - 1; i >= 0; i--)
			listeners.get(i).loaded();
	}

	protected void notifyDeleted()
	{
		int sz = listeners.size();
		for (int i = sz - 1; i >= 0; i--)
			listeners.get(i).deleted();
	}

	protected void notifyClosed()
	{
		int sz = listeners.size();
		for (int i = sz - 1; i >= 0; i--)
			listeners.get(i).closed();
	}

	public static class IncorrectPasswordException extends Exception
	{}

	abstract protected void onSave() throws Exception;
	abstract protected void onLoad(boolean force) throws IncorrectPasswordException, Exception;
	abstract protected void onClose() throws Exception;
	abstract protected void onDelete() throws Exception;
	abstract public boolean testPassword(String password);

	public void save() throws Exception
	{
		onSave();
		notifySaved();
	}

	public void load(boolean force) throws Exception
	{
		onLoad(force);
		notifyLoaded();
	}

	public void delete() throws Exception
	{
		onDelete();
		notifyDeleted();
	}

	public void close() throws Exception
	{
		encoder.cleanUp();
		encoder = null;

		lastLoad = 0;

		details = new ArrayList<>();
		loadEvents = new TreeSet<>();

		history = new PasswordDocumentHistory();
		mostRecentHistoryEvent = null;
		historyLoaded = true;

		onClose();
		notifyClosed();
	}

	protected HashMap<String, PasswordDetails> detailsIndex;

	public int count()
	{
		return details.size();
	}

	public PasswordDocumentHistory getHistory()
	{
		awaitHistoryLoaded();
		return history;
	}

	public void playSubHistory(PasswordDocumentHistory subHistory) throws PasswordDocumentHistory.PlaybackException
	{
		awaitHistoryLoaded();

		subHistory.playHistory(this);

	}

	public void copySubHistory(PasswordDocumentHistory subHistory)
	{
		awaitHistoryLoaded();

		int sz = subHistory.count();
		for (int i = 0; i < sz; i++)
		{
			HistoryEvent event = subHistory.getEvent(i);
			history.addEvent(event);
		}
	}

	public void addDetails(PasswordDetails orig) throws PasswordDocumentHistory.HistoryPlaybackException
	{
		String detid = orig.getId();
		if (detailsIndex.containsKey(detid))
			throw new PasswordDocumentHistory.HistoryPlaybackException();

		details.add(orig);

		awaitHistoryLoaded();
		HistoryEvent evt = new HistoryEventFactory().buildEvent(HistoryEventFactory.Types.DETAILS_CREATE);
		evt.id = detid;
		history.addEvent(evt);
		detailsIndex.put(detid, orig);
		copySubHistory(orig.getHistory());

		PasswordDocumentHistory.HistoryEventListener listener = new PasswordDocumentHistory.HistoryEventListener() {public void occurred(HistoryEvent event)
		{
			try
			{
				detailsEventHandler(event);
			}
			catch(Exception e){}
		}};
		detailsListeners.put(detid, listener);
		orig.addListener(listener);
	}

	protected void detailsEventHandler(HistoryEvent event) throws PasswordDocumentHistory.HistoryPlaybackException
	{
		if (!detailsIndex.containsKey(event.id))
			throw new PasswordDocumentHistory.HistoryPlaybackException();

		history.addEvent(event);
	}

	public void replaceDetails(PasswordDetails dets) throws PasswordDocumentHistory.HistoryPlaybackException, PasswordDocumentHistory.PlaybackException
	{
		awaitHistoryLoaded();
		String detid = dets.getId();
		if (!detailsIndex.containsKey(detid))
			throw new PasswordDocumentHistory.HistoryPlaybackException();

		detailsListeners.remove(detid);
		playSubHistory(dets.getHistory());
		PasswordDocumentHistory.HistoryEventListener listener = new PasswordDocumentHistory.HistoryEventListener() {public void occurred(HistoryEvent event)
		{
			try
			{
				detailsEventHandler(event);
			}
			catch(Exception e){}
		}};
		detailsListeners.put(detid, listener);
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
		detailsListeners.remove(detid);

		awaitHistoryLoaded();
		HistoryEvent evt = new HistoryEventFactory().buildEvent(HistoryEventFactory.Types.DETAILS_DELETE);
		evt.id = detid;
		history.addEvent(evt);
	}

	public ArrayList<PasswordDetails> getDetailsList()
	{
		return details;
	}

	public boolean equals(PasswordDocument doc)
	{
		int sz = doc.details.size();
		if (details.size() != sz)
			return false;

		for (int i = 0; i < sz; i++)
		{
			PasswordDetails dets = details.get(i);
			if (dets.diff(doc.details.get(i)))
				return false;
		}

		return true;
	}
}
