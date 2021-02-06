package com.munger.passwordkeeper.struct.documents;

import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class PasswordDocumentFileExport extends PasswordDocument
{
    public String path;

    public PasswordDocumentFileExport(String path, String name)
    {
        super(name);
        this.path = path;
        setHistoryLoaded();
    }

    public PasswordDocumentFileExport(String path, String name, String password)
    {
        super(name, password);
        this.path = path;
        setHistoryLoaded();
    }

    @Override
    public void onLoad(boolean force) throws IOException, PasswordDocumentHistory.HistoryPlaybackException
    {
        historyLoaded = true;
    }

    @Override
    protected void onClose() throws Exception {

    }

    @Override
    protected void onDelete() throws Exception {

    }

    @Override
    public boolean testPassword(String password) {
        return false;
    }

    protected BufferedWriter getWriter() throws IOException
    {
        File f = new File(path + "-bak");

        FileWriter fw = new FileWriter(f);
        BufferedWriter bw = new BufferedWriter(fw);
        return bw;
    }

    public void onSave() throws IOException
    {
        BufferedWriter bw = getWriter();

        for (PasswordDetails dets : details)
        {
            bw.write(dets.getName());
            bw.write('\t');
            bw.write(dets.getLocation());
            bw.write('\n');

            int sz = dets.count();
            for (int i = 0; i < sz; i++)
            {
                PasswordDetailsPair pair = dets.getPair(i);
                bw.write('\t');
                bw.write(pair.getKey());
                bw.write('\t');
                bw.write(pair.getValue());
                bw.write('\n');
            }

            bw.flush();
        }

        bw.close();
    }
}
