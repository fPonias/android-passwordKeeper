package com.munger.passwordkeeper.struct;

import com.munger.passwordkeeper.helpers.ThreadedCallbackWaiter;

public interface IEncoder
{
    void cleanUp() throws InterruptedException;
    boolean equals(IEncoder encoder);
    String encode(String target);
    byte[] encodeToBytes(String target);
    String decode(String target);
    void clearCallbacks();
    String decode(String target, ThreadedCallbackWaiter callbackWaiter);
    String decodeFromByteArray(byte[] target);
    String decodeFromByteArray(byte[] target, ThreadedCallbackWaiter callbackWaiter);
    void doCallback();
    String hash(String target);
    int hashSize();
}
