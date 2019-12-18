package com.munger.passwordkeeper.struct;

import com.munger.passwordkeeper.helpers.ThreadedCallbackWaiter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AES256
{
	static
	{
		System.out.println("cwd: " + System.getProperty("user.dir"));
		String platform = System.getProperty("os.name");
		String vendor = System.getProperty("java.vendor");
		System.out.println("current platform: " + platform + " vendor: " + vendor);

		if (platform.equals("Linux") && vendor.contains("Android"))
		{
			try
			{
				System.loadLibrary("aes256");
			}
			catch(UnsatisfiedLinkError e){
				try
				{
					loadExternalLib("./libpasswordkeeper/src/main/java/cpp/libaes256.so");
				}
				catch(UnsatisfiedLinkError e2){
					loadExternalLib("./src/main/java/cpp/libaes256.so");
				}
			}
		}
		else
		{
                    File libFile = new File(System.getProperty("user.home"), ".passwordKeeper/libaes256-GNU-MacOSX.dylib");
                    
                    if (!libFile.exists())
                    {
                        try
                        {
                            InputStream str = AES256.class.getResourceAsStream("/libaes256-GNU-MacOSX.dylib");
                            byte[] buffer = new byte[str.available()];
                            str.read(buffer);

                            OutputStream outStream = new FileOutputStream(libFile);
                            outStream.write(buffer);
                        }
                        catch(IOException e){
                            System.out.println("Failed to load aes library");
                        }
                    }
                        
                    loadExternalLib(libFile.getAbsolutePath());
		}
	}

	private static void loadExternalLib(String jniPath) throws UnsatisfiedLinkError
	{
		File jniPathFile = new File(jniPath);
		String path = jniPathFile.getAbsolutePath();

		System.load(path);
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

					float progress = getDecodeProgress();
					if (progress >= 1.0f)
						runPoller = false;
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

	public boolean equals(AES256 encoder)
	{
		return password.equals(encoder.password);
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
		float progress = getDecodeProgress();
		if (decodeCallbackWaiter != null)
		{
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
