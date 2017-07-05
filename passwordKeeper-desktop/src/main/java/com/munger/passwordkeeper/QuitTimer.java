/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper;

/**
 *
 * @author hallmarklabs
 */
public class QuitTimer 
{
    final private Object lock = new Object();
    private boolean isRunning = false;
    private long lastReset;
    private long quitCheckInterval = 60000;
    
    public QuitTimer()
    {
        Thread t = new Thread(() -> 
        {
            synchronized(lock)
            {
                isRunning = true;
                lastReset = System.currentTimeMillis();
            }
            
            runThread();
            
            if (listener != null)
                listener.quit();
        }, "quit timer");
        t.start();
    }
    
    private void runThread()
    {
        while(true)
        {
            long toWait = 0;
            synchronized(lock)
            {
                if (!isRunning)
                    return;
                
                long now = System.currentTimeMillis();
                long diff = now - lastReset;
                
                if (diff > quitCheckInterval)
                    return;
                
                toWait = quitCheckInterval - diff;
                
                try{ lock.wait(toWait); } catch(InterruptedException e) { return; }
            }
        }
    }
    
    public void reset()
    {
        synchronized(lock)
        {
            lastReset = System.currentTimeMillis();
        }
    }
    
    public void stop()
    {
        synchronized(lock)
        {
            isRunning = false;
            lock.notify();
        }
    }
    
    public interface QuitListener
    {
        public void quit();
    }
    
    public QuitListener listener;
    
    public void setQuitListener(QuitListener listener)
    {
        this.listener = listener;
    }
    
    public void setQuitCheckInterval(long value)
    {
        quitCheckInterval = value;
    }
}
