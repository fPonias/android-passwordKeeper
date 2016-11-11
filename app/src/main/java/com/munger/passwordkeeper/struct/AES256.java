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

	@Override
	protected void finalize() throws Throwable 
	{
		destroy(context);
		super.finalize();
	};

	public String encode(String target)
	{
		String ret = encode(context, target);
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

	public void doCallback(float progress)
	{
		if (decodeCallbackWaiter != null)
			decodeCallbackWaiter.doDecodeCallback(progress);
	}

	private native int init(String password);
	private native void destroy(int context);
	private native String encode(int context, String target);
	private native String decode(int context, String target);
}
