package com.munger.passwordkeeper.struct;

public class AES256 
{
	static{
		System.loadLibrary("aes256");
	}

	private String password;
	private int context;

	public AES256(String password)
	{
		this.password = password;
		context = init(password);
	}

	@Override
	protected void finalize() throws Throwable 
	{
		destroy(context);
		super.finalize();
	};

	public String encode(String target)
	{
		return encode(context, target);
	}

	public String decode(String target)
	{
		return decode(context, target);
	}

	public interface DecodeCallback
	{
		void callback(float progress);
	}

	private DecodeCallback _decodeCallback = null;
	private float decodeProgress = 0;
	private Thread decodeCallbackThread;
	private boolean decodeCallbackKill;
	private Object decodeCallbackLock = new Object();
	private Runnable decodeRoutine = new Runnable() {public void run()
	{
		while (decodeCallbackKill == false)
		{
			synchronized(decodeCallbackLock)
			{
				try
				{
					decodeCallbackLock.wait();
				}
				catch(InterruptedException e){
					return;
				}

				if (_decodeCallback != null)
					_decodeCallback.callback(decodeProgress);
			}
		}
	}};

	public void setDecodeCallback(DecodeCallback callback)
	{
		synchronized(decodeCallbackLock)
		{
			_decodeCallback = callback;

			if (_decodeCallback != null && decodeCallbackThread == null)
			{
				decodeCallbackKill = false;
				decodeCallbackThread = new Thread(decodeRoutine);
				decodeCallbackThread.start();
			}
			else if (_decodeCallback == null && decodeCallbackThread != null)
			{
				try
				{
					decodeCallbackKill = true;
					decodeCallbackLock.notify();
					decodeCallbackThread.join(100);
				}
				catch(InterruptedException e){
				}
				finally{
					decodeCallbackThread = null;
				}
			}
		}
	}

	public void doDecodeCallback(float progress)
	{
		decodeProgress = progress;
		synchronized (decodeCallbackLock)
		{
			if (decodeCallbackThread == null)
				return;

			decodeCallbackLock.notify();
		}
	}

	private native int init(String password);
	private native void destroy(int context);
	private native String encode(int context, String target);
	private native String decode(int context, String target);
}
