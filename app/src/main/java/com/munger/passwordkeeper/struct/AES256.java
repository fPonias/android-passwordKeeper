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

	private native int init(String password);
	private native void destroy(int context);
	private native String encode(int context, String target);
	private native String decode(int context, String target);
}
