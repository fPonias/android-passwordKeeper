package com.munger.passwordkeeper.struct;

import com.munger.passwordkeeper.HelperNoInst;
import com.munger.passwordkeeper.helpers.ThreadedCallbackWaiter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AES256Test
{
    private static String DEFAULT_PASSWORD = "pass";
    private static String DEFAULT_STRING = "test";

    public AES256Test()
    {

    }

    private AES256 encoder;

    @Before
    public void init()
    {
        encoder = new AES256(DEFAULT_PASSWORD, AES256.HashType.SHA);
    }

    @After
    public void cleanUp() throws InterruptedException
    {
        if (encoder != null)
            encoder.cleanUp();
    }

    @Test
    public void testloadsAndCleansUpWithNativeLibraries()
    {
        assertNotNull(encoder);
    }

    @Test
    public void testMD5Empty()
    {
        String hash = encoder.hash("");
        assertEquals(32, hash.length());
    }

    @Test
    public void testMD5Typical()
    {
        String hash = encoder.hash(DEFAULT_STRING);
        assertEquals(32, hash.length());
    }

    @Test
    public void testMD5Big()
    {
        String longStr = HelperNoInst.longString();
        String hash = encoder.hash(longStr);
        assertEquals(32, hash.length());
    }

    @Test
    public void encodeEmpty()
    {
        String cipher = encoder.encode("");
        assertEquals(0, cipher.length());
        String plain = encoder.decode(cipher);
        assertEquals("", plain);
    }

    @Test
    public void encodeTypical()
    {
        String cipher = encoder.encode(DEFAULT_STRING);
        assertNotEquals(DEFAULT_STRING, cipher);
        assertNotEquals(0, cipher.length());

        String plain = encoder.decode(cipher);
        assertEquals(DEFAULT_STRING, plain);
    }

    @Test
    public void encodeLarge()
    {
        String longStr = HelperNoInst.longString();
        String cipher = encoder.encode(longStr);
        assertNotEquals(longStr, cipher);
        assertNotEquals(0, cipher.length());

        String plain = encoder.decode(cipher);
        assertEquals(longStr, plain);
    }

    @Test
    public void encodeBytesEmpty()
    {
        byte[] cipher = encoder.encodeToBytes("");
        assertEquals(0, cipher.length);
        String plain = encoder.decodeFromByteArray(cipher);
        assertEquals("", plain);
    }

    @Test
    public void encodeBytesTypical()
    {
        byte[] cipher = encoder.encodeToBytes(DEFAULT_STRING);
        assertNotEquals(DEFAULT_STRING, cipher);
        assertNotEquals(0, cipher.length);

        String plain = encoder.decodeFromByteArray(cipher);
        assertEquals(DEFAULT_STRING, plain);
    }

    @Test
    public void encodeBytesLarge()
    {
        String longStr = HelperNoInst.longString();
        byte[] cipher = encoder.encodeToBytes(longStr);
        assertNotEquals(longStr, cipher);
        assertNotEquals(0, cipher.length);

        String plain = encoder.decodeFromByteArray(cipher);
        assertEquals(longStr, plain);
    }

    @Test
    public void differentPasswords() throws InterruptedException
    {
        String cipher1 = encoder.encode(DEFAULT_STRING);
        String plain1 = encoder.decode(cipher1);

        AES256 encoder2 = new AES256("", AES256.HashType.SHA);
        String cipher2 = encoder2.encode(DEFAULT_STRING);
        String plain2 = encoder2.decode(cipher2);

        AES256 encoder3 = new AES256("a different password", AES256.HashType.SHA);
        String cipher3 = encoder3.encode(DEFAULT_STRING);
        String plain3 = encoder3.decode(cipher3);

        String longStr = HelperNoInst.longString();
        AES256 encoder4 = new AES256(longStr, AES256.HashType.SHA);
        String cipher4 = encoder4.encode(DEFAULT_STRING);
        String plain4 = encoder4.decode(cipher4);

        assertNotEquals(cipher1, cipher2);
        assertNotEquals(cipher1, cipher3);
        assertNotEquals(cipher1, cipher4);
        assertNotEquals(cipher2, cipher3);
        assertNotEquals(cipher2, cipher4);
        assertNotEquals(cipher3, cipher4);

        assertEquals(plain1, plain2);
        assertEquals(plain2, plain3);
        assertEquals(plain3, plain4);

        encoder2.cleanUp();
        encoder3.cleanUp();
        encoder4.cleanUp();
    }

    @Test
    public void decodeCallbacks()
    {
        final Object locker = new Object();

        class Status
        {
            int wasCalled = 0;
            boolean wasCompleted = false;
            float lastProgress = -1.0f;
        }

        final Status status = new Status();
        final ThreadedCallbackWaiter waiter = new ThreadedCallbackWaiter(new ThreadedCallbackWaiter.Callback() {public void callback(float progress)
        {
            System.out.println("decode progress " + progress + " called");
            assertTrue(progress > status.lastProgress);
            assertTrue(progress >= 0 && progress <= 1.0f);
            status.lastProgress = progress;

            status.wasCalled++;

            if (progress >= 1.0f)
            {
                status.wasCompleted = true;

                System.out.println("decode progress was called " + status.wasCalled + " times and was completed " + status.wasCompleted);
                synchronized (locker)
                {
                    locker.notify();
                }
            }
        }});

        Thread decodeThread = new Thread(new Runnable() { public void run()
        {
            String longStr = HelperNoInst.longString();
            byte[] cipher = encoder.encodeToBytes(longStr);
            System.out.println("aes256test decrypting");
            String plain = encoder.decodeFromByteArray(cipher, waiter);
        }});
        decodeThread.start();

        System.out.println("aes256test waiting");
        synchronized (locker)
        {
            try{locker.wait(2000);} catch(Exception e){}
        }

        System.out.println("aes256test done");
        assertTrue(status.wasCalled >= 1);
        assertTrue(status.wasCompleted);
    }
}
