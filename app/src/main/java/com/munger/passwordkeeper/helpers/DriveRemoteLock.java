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

public class DriveRemoteLock extends IDriveRemoteLock
{
    protected GoogleApiClient apiClient;
    protected DriveFile targetFile;

    protected CustomPropertyKey ckey;

    protected ChangeListener listener = new ChangeListener()
    {
        @Override
        public void onChange(ChangeEvent changeEvent)
        {
            if (changeEvent.hasMetadataChanged())
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
        }
    };

    public DriveRemoteLock(GoogleApiClient apiClient, DriveFile file)
    {
        this.apiClient = apiClient;
        this.targetFile = file;
        targetFile.addChangeListener(apiClient, listener);

        ckey = new CustomPropertyKey(key, CustomPropertyKey.PUBLIC);
    }

    public void cleanUp()
    {
        targetFile.removeChangeListener(apiClient, listener);

        super.cleanUp();
    }

    @Override
    protected String awaitLockValue()
    {
        PendingResult<DriveResource.MetadataResult> result =  targetFile.getMetadata(apiClient);
        DriveResource.MetadataResult mresult = result.await();
        Map<CustomPropertyKey, String> props =  mresult.getMetadata().getCustomProperties();

        return props.get(ckey);
    }

    @Override
    protected String setLockValue(String value)
    {

        Log.d("remote lock", "setting remote lock to " + value);
        MetadataChangeSet.Builder builder = getMetadataBuilder();
        builder = builder.setCustomProperty(ckey, value);
        MetadataChangeSet set = builder.build();
        PendingResult<DriveResource.MetadataResult> result = targetFile.updateMetadata(apiClient, set);
        DriveResource.MetadataResult mresult = result.await();
        String ret = mresult.getMetadata().getCustomProperties().get(ckey);

        return ret;
    }

    @Override
    protected String getDeviceUID()
    {
        return MainState.getInstance().settings.getDeviceUID();
    }

    protected MetadataChangeSet.Builder getMetadataBuilder()
    {
        return new MetadataChangeSet.Builder();
    }
}
