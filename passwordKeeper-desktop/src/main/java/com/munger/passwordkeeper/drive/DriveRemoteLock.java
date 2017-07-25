package com.munger.passwordkeeper.drive;

import com.munger.passwordkeeper.Main;
import com.munger.passwordkeeper.helpers.IDriveRemoteLock;

public class DriveRemoteLock extends IDriveRemoteLock
{
    protected DriveHelper helper;
    protected DriveHelper.DriveFileStruct targetFile;

    public DriveRemoteLock(DriveHelper helper, DriveHelper.DriveFileStruct target)
    {
        this.helper = helper;
        this.targetFile = target;
    }

    public void cleanUp()
    {
        super.cleanUp();
    }

    protected String awaitLockValue()
    {
        String value = helper.getMetadataValue(targetFile, key);
        return value;
    }

    protected String setLockValue(String value)
    {
        boolean success = helper.setMetadataValue(targetFile, key, value);
        
        if (!success)
            return null;
        
        String newValue = helper.getMetadataValue(targetFile, key);
        return newValue;
    }

    protected String getDeviceUID()
    {
        return Main.instance.mainState.prefs.getDeviceUID();
    }
}