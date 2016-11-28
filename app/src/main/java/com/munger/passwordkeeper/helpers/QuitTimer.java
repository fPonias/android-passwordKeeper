package com.munger.passwordkeeper.helpers;

import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.view.SettingsFragment;

/**
 * Created by codymunger on 11/26/16.
 */

public class QuitTimer
{
    private Thread thread;
    private final long CHECK_PERIOD = 1000;
    private long time;
    private Object lock = new Object();
    private boolean checkerRunning;

    public QuitTimer()
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainState.getInstance().activity);
        preferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            if (key.equals(SettingsFragment.PREF_NAME_TIMEOUT_LIST))
            {
                reset();
            }
        }});

        reset();
    }

    public void reset()
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainState.getInstance().activity);
        String valueStr = preferences.getString(SettingsFragment.PREF_NAME_TIMEOUT_LIST, "5");
        int value = Integer.valueOf(valueStr);

        boolean doStart = false;
        boolean doStop = false;
        synchronized (lock)
        {
            if (value > 0)
            {
                if (thread == null)
                    doStart = true;

                time = System.currentTimeMillis() + value * 60000;
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
                long currentTime;
                while (true)
                {
                    synchronized (lock)
                    {
                        if (!checkerRunning)
                            return;

                        currentTime = System.currentTimeMillis();

                        if (currentTime > time)
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
