package com.munger.passwordkeeper.struct.com.munger.passwordkeeper.struct.documents;

import android.provider.ContactsContract;

import java.io.DataInput;
import java.io.DataInputStream;

/**
 * Created by codymunger on 11/18/16.
 */

public class PasswordDocumentStream extends PasswordDocument
{
    private DataInput stream;

    public PasswordDocumentStream(DataInput stream)
    {
        this.stream = stream;
    }

    public void save() throws Exception
    {

    }

    public void load(boolean force) throws Exception
    {
        deltasFromEncryptedString(stream);
    }

    protected void onClose() throws Exception
    {

    }

    public void delete() throws Exception
    {

    }

    public boolean testPassword()
    {
        return true;
    }
}
