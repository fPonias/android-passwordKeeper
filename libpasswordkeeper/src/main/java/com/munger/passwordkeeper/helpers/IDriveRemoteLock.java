package com.munger.passwordkeeper.helpers;

public abstract class IDriveRemoteLock
{
    protected boolean hasRemoteLock = false;
    protected String lastValue = null;
    protected Object lock = new Object();
    protected long timeout = 15000;
    protected boolean waitingForChange = false;
    protected String key = "remoteLock";

    public IDriveRemoteLock()
    {}

    public void cleanUp()
    {
        if (hasRemoteLock)
            try{release();}catch(FailedToReleaseLockException e){}
    }

    protected enum RemoteLockState
    {
        FREE,
        LOCKED,
        OWNED
    }

    public class FailedToAttainLockException extends Exception
    {}

    public class FailedToReleaseLockException extends Exception
    {}

    protected abstract String awaitLockValue();
    protected abstract String setLockValue(String value);
    protected abstract String getDeviceUID();

    protected RemoteLockState check()
    {
        synchronized (lock)
        {
            hasRemoteLock = false;

            String value = awaitLockValue();
            if (value == null)
                return RemoteLockState.FREE;

            lastValue = value;
            if (value.length() == 0)
                return RemoteLockState.FREE;

            String[] parts = value.split(" ");
            if (parts.length != 2)
                return RemoteLockState.FREE;

            long current = System.currentTimeMillis();

            long stamp = 0;
            try {stamp = Long.parseLong(parts[1]);} catch(NumberFormatException e){}
            if (current - stamp >= timeout)
                return RemoteLockState.FREE;

            String uid = getDeviceUID();
            if (uid.equals(parts[0]))
            {
                synchronized (lock)
                {
                    hasRemoteLock = true;
                }

                return RemoteLockState.OWNED;
            }
            else
                return RemoteLockState.LOCKED;
        }
    }

    public void get() throws FailedToAttainLockException
    {
        while(true)
        {
            RemoteLockState state = check();
            if (state == RemoteLockState.OWNED)
            {
                return;
            }
            else if (state == RemoteLockState.FREE)
            {
                claim();
                return;
            }

            synchronized (lock)
            {
                waitingForChange = true;

                String[] parts = lastValue.split(" ");
                long current = System.currentTimeMillis();
                long then = 0;
                try{then = Long.parseLong(parts[1]);}catch(Exception e){}
                long diff = timeout - (current - then);

                if (diff > 0)
                {
                    try{lock.wait(diff);} catch(InterruptedException e){return;}
                }

                waitingForChange = false;
            }
        }
    }

    protected void claim() throws FailedToAttainLockException
    {
        synchronized (lock)
        {
            String uid = getDeviceUID();
            long current = System.currentTimeMillis();
            String newval = uid + " " + current;

            lastValue = setLockValue(newval);

            if (lastValue.equals(newval))
            {
                hasRemoteLock = true;
            }
            else
            {
                hasRemoteLock = false;
                throw new FailedToAttainLockException();
            }
        }
    }

    public void release() throws FailedToReleaseLockException
    {
        RemoteLockState state = check();
        if (state != RemoteLockState.OWNED)
            return;

        synchronized (lock)
        {
            String newval = "";
            lastValue = setLockValue(newval);

            if (!lastValue.equals(newval))
                return;
            else
                hasRemoteLock = false;
        }
    }
}
