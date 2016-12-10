package com.munger.passwordkeeper.helpers;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.events.ChangeEvent;
import com.google.android.gms.drive.events.ChangeListener;
import com.google.android.gms.drive.metadata.CustomPropertyKey;
import com.munger.passwordkeeper.MainState;

import java.util.Map;

/**
 * Created by codymunger on 12/8/16.
 */

public class DriveRemoteLock
{
    protected GoogleApiClient apiClient;
    protected DriveFile targetFile;

    protected boolean hasRemoteLock = false;
    protected String lastValue = null;
    protected Object lock = new Object();
    protected CustomPropertyKey key = new CustomPropertyKey("remoteLock", CustomPropertyKey.PUBLIC);
    protected long timeout = 15000;
    protected boolean waitingForChange = false;

    protected ChangeListener listener = new ChangeListener()
    {
        @Override
        public void onChange(ChangeEvent changeEvent)
        {
            synchronized (lock)
            {
                if (waitingForChange)
                {
                    Log.d("remote lock", "remote metadata changed");
                    lock.notify();
                }
            }
        }
    };

    public DriveRemoteLock(GoogleApiClient apiClient, DriveFile file)
    {
        this.apiClient = apiClient;
        this.targetFile = file;
        targetFile.addChangeListener(apiClient, listener);
    }

    public void cleanUp()
    {
        targetFile.removeChangeListener(apiClient, listener);

        if (hasRemoteLock)
            release();
    }

    protected enum RemoteLockState
    {
        FREE,
        LOCKED,
        OWNED
    }

    protected RemoteLockState check()
    {
        Log.d("remote lock", "checking for remote lock");
        synchronized (lock)
        {
            hasRemoteLock = false;

            PendingResult<DriveResource.MetadataResult> result =  targetFile.getMetadata(apiClient);
            DriveResource.MetadataResult mresult = result.await();
            Map<CustomPropertyKey, String> props =  mresult.getMetadata().getCustomProperties();

            if (!props.containsKey(key))
            {
                Log.d("remote lock", "no lock found");
                return RemoteLockState.FREE;
            }

            String value = props.get(key);
            lastValue = value;
            if (value.length() == 0)
            {
                Log.d("remote lock", "no lock found");
                return RemoteLockState.FREE;
            }

            String[] parts = value.split(" ");
            if (parts.length != 2)
            {
                Log.d("remote lock", "corrupt lock found");
                return RemoteLockState.FREE;
            }

            String uid = MainState.getInstance().settings.getDeviceUID();
            long current = System.currentTimeMillis();

            long stamp = 0;
            try {stamp = Long.parseLong(parts[1]);} catch(NumberFormatException e){}
            if (current - stamp >= timeout)
            {
                Log.d("remote lock", "expired lock found");
                return RemoteLockState.FREE;
            }

            if (uid.equals(parts[0]))
            {
                Log.d("remote lock", "owned current lock found");

                synchronized (lock)
                {
                    hasRemoteLock = true;
                }

                return RemoteLockState.OWNED;
            }
            else
            {
                Log.d("remote lock", "current unowned lock found");
                return RemoteLockState.LOCKED;
            }
        }
    }

    public void get()
    {
        Log.d("remote lock", "attaining remote lock");
        while(true)
        {
            Log.d("remote lock", "checking remote lock");
            RemoteLockState state = check();
            if (state == RemoteLockState.OWNED)
            {
                Log.d("remote lock", "already owned remote lock");
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
                    Log.d("remote lock", "waiting for remote lock release");
                    try{lock.wait(diff);} catch(InterruptedException e){return;}
                }

                waitingForChange = false;
            }
        }
    }

    protected void claim()
    {
        Log.d("remote lock", "claiming remote lock");
        synchronized (lock)
        {
            String uid = MainState.getInstance().settings.getDeviceUID();
            long current = System.currentTimeMillis();
            String newval = uid + " " + current;
            Log.d("remote lock", "setting remote lock to " + newval);
            MetadataChangeSet set = new MetadataChangeSet.Builder().setCustomProperty(key, newval).build();
            PendingResult<DriveResource.MetadataResult> result = targetFile.updateMetadata(apiClient, set);
            DriveResource.MetadataResult mresult = result.await();
            lastValue = mresult.getMetadata().getCustomProperties().get(key);

            if (lastValue.equals(newval))
            {
                Log.d("remote lock", "remote lock verified");
                hasRemoteLock = true;
            }
            else
            {
                hasRemoteLock = false;
                Log.d("password", "failed to obtain remote file lock");
            }
        }
    }

    public void release()
    {
        Log.d("remote lock", "releasing remote lock");
        RemoteLockState state = check();
        if (state != RemoteLockState.OWNED)
        {
            Log.d("password", "remote lock already released");
            return;
        }

        synchronized (lock)
        {
            String newval = "";
            MetadataChangeSet set = new MetadataChangeSet.Builder().setCustomProperty(key, newval).build();
            Log.d("remote lock", "setting remote lock to ''");
            PendingResult<DriveResource.MetadataResult> result = targetFile.updateMetadata(apiClient, set);
            DriveResource.MetadataResult mresult = result.await();
            lastValue = mresult.getMetadata().getCustomProperties().get(key);

            if (!lastValue.equals(newval))
                Log.d("password", "failed to release remote file lock");
            else
                hasRemoteLock = false;
        }
    }
}
