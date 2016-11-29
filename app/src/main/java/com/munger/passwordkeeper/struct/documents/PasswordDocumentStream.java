package com.munger.passwordkeeper.struct.documents;

import java.io.DataInput;

/**
 * Created by codymunger on 11/18/16.
 */

public class PasswordDocumentStream extends PasswordDocument
{
    private DataInput stream;
    private long size;

    public PasswordDocumentStream(DataInput stream, long sz)
    {
        this.stream = stream;
        this.size = sz;
    }

    public void save() throws Exception
    {

    }

    public void load(boolean force) throws Exception
    {
        deltasFromEncryptedString(stream, size);
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
