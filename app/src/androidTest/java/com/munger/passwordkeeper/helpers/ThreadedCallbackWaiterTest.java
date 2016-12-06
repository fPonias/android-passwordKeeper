package com.munger.passwordkeeper.helpers;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.munger.passwordkeeper.struct.AES256;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by codymunger on 12/6/16.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ThreadedCallbackWaiterTest
{

    @Before
    public void init()
    {

    }

    @After
    public void cleanUp()
    {
    }

    public class Status
    {
        public Object lock = new Object();
        public int calls = 0;
        public float lastProgress = -1.0f;
    }

    @Test
    public void standardWait() throws InterruptedException
    {
        final Status status = new Status();

        ThreadedCallbackWaiter waiter = new ThreadedCallbackWaiter(new ThreadedCallbackWaiter.Callback() {public void callback(float progress)
        {
            status.calls++;
            status.lastProgress = progress;

            synchronized (status.lock)
            {
                status.lock.notify();
            }
        }});

        int count = 0;
        float[] values = new float[]{0.0f, 0.25f, 0.5f, 0.75f, 1.0f};
        for(float value : values)
        {
            waiter.doDecodeCallback(value);
            synchronized (status.lock)
            {
                status.lock.wait(200);
            }
            count++;
            assertEquals(count, status.calls);
            assertTrue(value == status.lastProgress);
        }

        waiter.CleanUp();
    }
}
