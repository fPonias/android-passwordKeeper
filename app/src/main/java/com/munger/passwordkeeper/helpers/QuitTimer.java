package com.munger.passwordkeeper.helpers;

import android.util.Log;

import com.munger.passwordkeeper.MainState;

/**
 * Created by codymunger on 11/26/16.
 */

public class QuitTimer
{
    private Thread thread;
    private long CHECK_PERIOD = 1000;
    private long time;
    private Object lock = new Object();
    private boolean checkerRunning;

    public QuitTimer()
    {
        reset();
    }

    public void setCheckPeriod(long value)
    {
        CHECK_PERIOD = value;
    }

    public long getCheckPeriod()
    {
        return CHECK_PERIOD;
    }

    public void reset()
    {
        float value = MainState.getInstance().settings.getTimeout();

        boolean doStart = false;
        boolean doStop = false;
        synchronized (lock)
        {
            if (value > 0)
            {
                if (thread == null)
                    doStart = true;

                time = System.currentTimeMillis() + (int)(value * 60000);
            }
            else if (value == -1)
            {
                if (thread != null)
                    doStop = true;

                time = Long.MAX_VALUE;
            }
        }

        if (doStop)
            stop();

        if (doStart)
            start();
    }

    private void start()
    {
        synchronized (lock)
        {
            if (checkerRunning)
                return;

            checkerRunning = true;
            thread = new Thread(new Runnable() {public void run()
            {
                Log.d("password", "Timeout thread started");
                long currentTime;
                while (true)
                {
                    synchronized (lock)
                    {
                        if (!checkerRunning)
                            return;

                        currentTime = System.currentTimeMillis();
                        long diff = time - currentTime;

                        if (diff < 0)
                        {
                            MainState.getInstance().handler.post(new Runnable() {public void run()
                            {
                                Log.d("password", "Timeout reached.  Quitting");
                                checkerRunning = false;
                                thread = null;

                                MainState.getInstance().navigationHelper.reset();
                                return;
                            }});
                        }
                    }

                    try
                    {
                        Thread.sleep(CHECK_PERIOD);
                    }
                    catch(Exception e){
                        return;
                    }
                }
            }}, "Quit Thread");
            thread.start();
        }
    }

    public void stop()
    {
        Thread toQuitThread = null;
        synchronized (lock)
        {
            if (!checkerRunning)
                return;

            checkerRunning = false;
            toQuitThread = thread;
            thread = null;
        }

        try{toQuitThread.join();}catch(InterruptedException e){}
    }
}
