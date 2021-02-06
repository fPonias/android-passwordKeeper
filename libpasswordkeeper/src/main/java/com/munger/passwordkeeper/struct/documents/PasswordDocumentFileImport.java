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
import java.nio.Buffer;

/**
 * Created by codymunger on 11/7/16.
 */

public class PasswordDocumentFileImport extends PasswordDocumentFile
{
    public String path;

    public PasswordDocumentFileImport(String path, String name)
    {
        super(name);
        this.path = path;
        setHistoryLoaded();
    }

    public PasswordDocumentFileImport(String path, String name, String password)
    {
        super(name, password);
        this.path = path;
        setHistoryLoaded();
    }

    @Override
    public void onLoad(boolean force) throws IOException, PasswordDocumentHistory.HistoryPlaybackException
    {
        BufferedReader br = getReader();
        String line =  null;

        currentDets = null;
        while ((line = br.readLine()) != null)
        {
            parseLine(line);
        }

        if (currentDets != null)
            addDetails(currentDets);

        if (br != null)
            br.close();
    }

    protected BufferedReader getReader() throws IOException
    {
        File f = new File(path);

        if (!f.exists() || !f.canRead())
            throw new IOException();

        FileReader fr = new FileReader(f);
        BufferedReader br = new BufferedReader(fr);
        return br;
    }

    private PasswordDetails currentDets = null;

    private void parseLine(String line) throws IOException, PasswordDocumentHistory.HistoryPlaybackException
    {
        if (line.length() == 0)
            return;

        if (line.charAt(0) != '\t')
        {
            if (currentDets != null)
                addDetails(currentDets);

            currentDets = parseDetails(line);
        }
        else
        {
            if (currentDets == null)
                throw new IOException("line " + line + " could add pair to null details");

            PasswordDetailsPair pair = parsePair(line);
        }
    }

    private PasswordDetails parseDetails(String line) throws IOException
    {
        PasswordDetails dets = new PasswordDetails();
        String[] parts = line.split("\t");
        dets.setName(parts[0]);

        if (parts.length > 1)
            dets.setLocation(parts[1]);
        else
            dets.setLocation(parts[0]);

        return dets;
    }

    private PasswordDetailsPair parsePair(String line) throws IOException
    {
        String[] parts = line.substring(1).split("\t");
        PasswordDetailsPair pair = null;

        if (parts.length >= 1)
        {
            pair = currentDets.addEmptyPair();
            pair.setKey(parts[0]);

            if (parts.length >= 2)
                pair.setValue(parts[1]);

            if (parts.length >= 3)
                throw new IOException("line " + line + " had too many fields for pair line");
        }

        return pair;
    }

    protected BufferedWriter getWriter() throws IOException
    {
        File f = new File(path + "-bak");

        if (!f.exists() || !f.canRead())
            throw new IOException();

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
        }
    }
}
