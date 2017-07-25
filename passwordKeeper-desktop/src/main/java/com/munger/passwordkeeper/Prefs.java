/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 *
 * @author hallmarklabs
 */
public class Prefs 
{
    private Preferences preferences;
    
    public Prefs() throws BackingStoreException
    {
        preferences = Preferences.systemNodeForPackage(Main.class);
        if (preferences.keys().length > 0)
            delete();
    }
    
    private void delete() throws BackingStoreException
    {
        preferences.clear();
    }
    
    public enum Types
    {
        savePath,
        syncToDrive,
        timeout,
        recents
    };
    
    public String getSavePath()
    {
        byte[] bytes = preferences.getByteArray(Types.savePath.toString(), null);
        
        if (bytes != null)
            return new String(bytes);
        
        String val = System.getProperty("user.home") + "/pw-tmp/passwords";
        preferences.putByteArray(Types.savePath.toString(), val.getBytes());
        notifyListeners(Types.savePath);
        return val;
    }
    
    public void setSavePath(String value)
    {
        preferences.putByteArray(Types.savePath.toString(), value.getBytes());
        notifyListeners(Types.savePath);
    }
    
    public boolean getSyncToDrive()
    {
        boolean value = preferences.getBoolean(Types.syncToDrive.toString(), false);
        return value;
    }
    
    public void setSyncToDrive(boolean value)
    {
        preferences.putBoolean(Types.syncToDrive.toString(), value);
        notifyListeners(Types.syncToDrive);
    }
    
    public long getTimeout()
    {
        long value = preferences.getLong(Types.timeout.toString(), 60000);
        return value;
    }
    
    public void setTimeout(long timeout)
    {
        preferences.putLong(Types.timeout.toString(), timeout);
        notifyListeners(Types.timeout);
    }
    
    public String[] getRecentFiles()
    {
        byte[] bytes = preferences.getByteArray(Types.recents.toString(), null);
        if (bytes == null)
            return new String[0];
        
        ObjectInputStream ois = null;
        String[] ret = new String[0];
        try
        {
            ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            ret = (String[]) ois.readObject();
        }
        catch(Exception e){}
        
        if (ois != null)
            try {ois.close();} catch(Exception e) {}
        
        return ret;
    }
    
    public void setRecentFiles(String[] files)
    {
        ObjectOutputStream ois = null;
        
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ois = new ObjectOutputStream(baos);
            ois.writeObject(files);
            ois.flush();
            
            preferences.putByteArray(Types.recents.toString(), baos.toByteArray());
        }
        catch(Exception e){}
        
        if (ois != null)
            try{ois.close();} catch(Exception e){}
        
        notifyListeners(Types.recents);
    }
    
    public interface IPreferenceListener
    {
        public void updated(Types key);
    }
    
    private ArrayList<WeakReference<IPreferenceListener>> listeners = new ArrayList<>();
    
    public void addListener(IPreferenceListener listener)
    {
        listeners.add(new WeakReference<>(listener));
    }
    
    private void notifyListeners(Types type)
    {
        int sz = listeners.size();
        for (int i = sz - 1; i >= 0; i--)
        {
            WeakReference<IPreferenceListener> ref = listeners.get(i);
            IPreferenceListener listener = ref.get();
            
            if (listener != null)
                listener.updated(type);
        }
    }
    
    public String getDeviceUID()
    {
        String ret = (preferences.get("uuid", null));
        
        if (ret == null)
        {
            ret = UUID.randomUUID().toString();
            preferences.put("uuid", ret);
        }
        
        return ret;
    }
}
