package com.munger.passwordkeeper.struct;

public class AES256 
{
	static{
		System.loadLibrary("aes256");
	}

	private String password;
	private int context;
	private ThreadedCallbackWaiter decodeCallbackWaiter;

	public AES256(String password)
	{
		this.password = password;
		context = init(password);
	}

	public void cleanUp()
	{
		destroy(context);
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

	public String decode(String target)
	{
		return decode(target, null);
	}

	public String decode(String target, ThreadedCallbackWaiter callbackWaiter)
	{
		decodeCallbackWaiter = callbackWaiter;
		return decode(context, target);
	}

	public String decodeFromBytes(byte[] target) { return decodeFromBytes(target, null); }

	public String decodeFromBytes(byte[] target, ThreadedCallbackWaiter callbackWaiter)
	{
		decodeCallbackWaiter = callbackWaiter;
		return decodeFromBytes(context, target);
	}

	public void doCallback(float progress)
	{
		if (decodeCallbackWaiter != null)
			decodeCallbackWaiter.doDecodeCallback(progress);
	}

	private native int init(String password);
	private native void destroy(int context);
	private native String encode(int context, String target);
	private native String decode(int context, String target);
	private native byte[] encodeToBytes(int context, String target);
	private native String decodeFromBytes(int content, byte[] target);
	public native String md5Hash(String target);
}
