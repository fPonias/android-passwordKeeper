package com.munger.passwordkeeper.struct;

import com.munger.passwordkeeper.helpers.ThreadedCallbackWaiter;

import java.io.File;

public class AES256
{
	static
	{
		try
		{
			System.loadLibrary("aes256");
		}
		catch(UnsatisfiedLinkError e){
			String jniPath = "./src/main/cpp/libaes256.so";
			File jniPathFile = new File(jniPath);
			String path = jniPathFile.getAbsolutePath();

			System.load(path);
		}
	}

	private String password;
	private byte[] context;
	private ThreadedCallbackWaiter decodeCallbackWaiter;
	private Thread progressPoller;
	private Object lock;
	private boolean runPoller;

	public AES256(String password)
	{
		this.password = password;
		context = init(password);

		runPoller = true;
		lock = new Object();
		progressPoller = new Thread(new Runnable() {public void run()
		{
			float lastProgress = -1.0f;
			while(runPoller)
			{
				try{Thread.sleep(25);}catch(InterruptedException e){return;}

				synchronized (lock)
				{
					doCallback();
				}
			}
		}}, "progress poller");
		progressPoller.start();
	}

	public void cleanUp() throws InterruptedException
	{
		if (progressPoller == null || runPoller == false)
			return;

		runPoller = false;

		if (progressPoller != null)
		{
			progressPoller.join();
			progressPoller = null;
		}
	}

	public String encode(String target)
	{
		String ret = encode(context, target);
		return ret;
	}

	public byte[] encodeToBytes(String target)
	{
		byte[] ret = encodeToBytes(context, target);
		return ret;
	}

	public void clearCallbacks()
	{
		decodeCallbackWaiter = null;
	}

	public String decode(String target)
	{
		return decode(target, null);
	}

	public String decode(String target, ThreadedCallbackWaiter callbackWaiter)
	{
		decodeCallbackWaiter = callbackWaiter;
		return decode(context, target);
	}

	public String decodeFromByteArray(byte[] target) { return decodeFromByteArray(target, null); }

	public String decodeFromByteArray(byte[] target, ThreadedCallbackWaiter callbackWaiter)
	{
		decodeCallbackWaiter = callbackWaiter;
		return decodeFromBytes(context, target);
	}

	public void doCallback()
	{
		if (decodeCallbackWaiter != null)
		{
			float progress = getDecodeProgress();
			decodeCallbackWaiter.doDecodeCallback(progress);

			if (progress >= 1.0f)
			{
				decodeCallbackWaiter.CleanUp();
				decodeCallbackWaiter = null;
			}
		}
	}

	private native byte[] init(String password);
	private native void destroy(byte[] context);
	private native String encode(byte[] context, String target);
	private native String decode(byte[] context, String target);
	private native byte[] encodeToBytes(byte[] context, String target);
	private native String decodeFromBytes(byte[] context, byte[] target);
	public native String md5Hash(String target);
	private native float getDecodeProgress();
}
