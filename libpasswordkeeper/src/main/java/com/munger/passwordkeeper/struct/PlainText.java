package com.munger.passwordkeeper.struct;

import com.munger.passwordkeeper.helpers.ThreadedCallbackWaiter;

public class PlainText implements IEncoder
{
    private AES256 subencoder;
    public PlainText(String password)
    {
        subencoder = new AES256(password, AES256.HashType.SHA);
    }

    @Override
    public boolean equals(IEncoder encoder)
    {
        return (encoder instanceof PlainText);
    }

    @Override
    public String encode(String target)
    {
        return target;
    }

    @Override
    public byte[] encodeToBytes(String target)
    {
        return target.getBytes();
    }

    @Override
    public String decode(String target)
    {
        return target;
    }

    @Override
    public void clearCallbacks()
    {

    }

    @Override
    public String decode(String target, ThreadedCallbackWaiter callbackWaiter)
    {
        return target;
    }

    @Override
    public String decodeFromByteArray(byte[] target)
    {
        String ret = new String(target);
        return ret;
    }

    @Override
    public String decodeFromByteArray(byte[] target, ThreadedCallbackWaiter callbackWaiter) {
        String ret = new String(target);
        return ret;
    }

    @Override
    public void doCallback()
    {}

    @Override
    public String hash(String target)
    {
        return subencoder.hash(target);
    }

    public int hashSize()
    {
        return subencoder.hashSize();
    }

    @Override
    public void cleanUp() throws InterruptedException
    {
        subencoder.cleanUp();
    }
}
