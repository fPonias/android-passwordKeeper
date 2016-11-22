package com.munger.passwordkeeper.struct;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.munger.passwordkeeper.Helper;
import com.munger.passwordkeeper.helpers.ThreadedCallbackWaiter;

import junit.framework.JUnit4TestAdapter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

public class AES256Test
{
    private static String DEFAULT_PASSWORD = "pass";
    private static String DEFAULT_STRING = "test";

    private AES256 encoder;

    public AES256Test()
    {

    }

    @Before
    public void init()
    {
        encoder = new AES256(DEFAULT_PASSWORD);
    }

    @After
    public void cleanUp()
    {
        if (encoder != null)
            encoder.cleanUp();
    }

    @Test
    public void testloadsAndCleansUpWithNativeLibraries()
    {
        Context appContext = InstrumentationRegistry.getTargetContext();
        assertNotNull(encoder);
    }

    @Test
    public void testMD5Empty()
    {
        String hash = encoder.md5Hash("");
        assertEquals(32, hash.length());
    }

    @Test
    public void testMD5Typical()
    {
        String hash = encoder.md5Hash(DEFAULT_STRING);
        assertEquals(32, hash.length());
    }

    @Test
    public void testMD5Big()
    {
        String longStr = Helper.longString();
        String hash = encoder.md5Hash(longStr);
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
        String longStr = Helper.longString();
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
        String plain = encoder.decodeFromBytes(cipher);
        assertEquals("", plain);
    }

    @Test
    public void encodeBytesTypical()
    {
        byte[] cipher = encoder.encodeToBytes(DEFAULT_STRING);
        assertNotEquals(DEFAULT_STRING, cipher);
        assertNotEquals(0, cipher.length);

        String plain = encoder.decodeFromBytes(cipher);
        assertEquals(DEFAULT_STRING, plain);
    }

    @Test
    public void encodeBytesLarge()
    {
        String longStr = Helper.longString();
        byte[] cipher = encoder.encodeToBytes(longStr);
        assertNotEquals(longStr, cipher);
        assertNotEquals(0, cipher.length);

        String plain = encoder.decodeFromBytes(cipher);
        assertEquals(longStr, plain);
    }

    @Test
    public void differentPasswords()
    {
        String cipher1 = encoder.encode(DEFAULT_STRING);
        String plain1 = encoder.decode(cipher1);

        AES256 encoder2 = new AES256("");
        String cipher2 = encoder2.encode(DEFAULT_STRING);
        String plain2 = encoder2.decode(cipher2);

        AES256 encoder3 = new AES256("a different password");
        String cipher3 = encoder3.encode(DEFAULT_STRING);
        String plain3 = encoder3.decode(cipher3);

        String longStr = Helper.longString();
        AES256 encoder4 = new AES256(longStr);
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

        ThreadedCallbackWaiter waiter = new ThreadedCallbackWaiter(new ThreadedCallbackWaiter.Callback() {public void callback(float progress)
        {
            assertTrue(progress > status.lastProgress);
            assertTrue(progress >= 0 && progress <= 1.0f);
            status.lastProgress = progress;

            status.wasCalled++;

            if (progress == 1.0f)
            {
                status.wasCompleted = true;
                synchronized (locker)
                {
                    locker.notify();
                }
            }
        }});

        String longStr = Helper.longString();
        byte[] cipher = encoder.encodeToBytes(longStr);
        String plain = encoder.decodeFromBytes(cipher, waiter);

        synchronized (locker)
        {
            try{locker.wait(250);} catch(Exception e){}
        }

        assertTrue(status.wasCalled > 1);
        assertTrue(status.wasCompleted);
    }
}
