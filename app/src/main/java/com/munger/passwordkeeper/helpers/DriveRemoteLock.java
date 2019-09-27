package com.munger.passwordkeeper.helpers;

import android.util.Log;

import com.munger.passwordkeeper.MainState;

import java.util.Map;

/**
 * Created by codymunger on 12/8/16.
 */

public class DriveRemoteLock extends IDriveRemoteLock
{
    @Override
    protected String awaitLockValue() {
        return null;
    }

    @Override
    protected String setLockValue(String value) {
        return null;
    }

    @Override
    protected String getDeviceUID() {
        return null;
    }
}
