package com.munger.passwordkeeper.helpers;

/**
 * Created by codymunger on 11/11/16.
 */

public class ThreadedCallbackWaiter
{
    public interface Callback
    {
        void callback(float progress);
    }

    private Callback _callback = null;
    private float progress = 0;
    private Thread thread;
    private boolean kill;
    private Object lock = new Object();

    public ThreadedCallbackWaiter(Callback callback)
    {
        _callback = callback;

        synchronized(lock)
        {
            kill = false;
            thread = new Thread(routine);
            thread.start();

            try{lock.wait();}catch(InterruptedException e){return;}
        }
    }

    public void CleanUp()
    {
        synchronized (lock)
        {
            try
            {
                kill = true;
                lock.notify();
                thread.join(100);
            }
            catch(InterruptedException e){
            }
            finally{
                thread = null;
            }
        }
    }

    private Runnable routine = new Runnable() {public void run()
    {
        synchronized (lock)
        {
            lock.notify();
        }

        while (kill == false)
        {
            synchronized(lock)
            {
                try
                {
                    lock.wait();
                }
                catch(InterruptedException e){
                    return;
                }

                if (kill == true)
                    return;

                if (_callback != null)
                    _callback.callback(progress);
            }
        }
    }};

    public void doDecodeCallback(float progress)
    {
        this.progress = progress;
        synchronized (lock)
        {
            if (thread == null)
                return;

            lock.notify();
        }
    }
}
