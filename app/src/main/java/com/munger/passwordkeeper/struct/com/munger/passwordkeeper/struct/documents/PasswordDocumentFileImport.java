package com.munger.passwordkeeper.struct.com.munger.passwordkeeper.struct.documents;

import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import com.munger.passwordkeeper.struct.PasswordDocumentHistory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

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
    }

    public PasswordDocumentFileImport(String path, String name, String password)
    {
        super(name, password);
        this.path = path;
    }

    @Override
    public void load(boolean force) throws IOException, PasswordDocumentHistory.HistoryPlaybackException
    {
        File f = new File(path);

        if (!f.exists() || !f.canRead())
            throw new IOException();

        FileReader fr = new FileReader(f);
        BufferedReader br = new BufferedReader(fr);
        String line =  null;

        currentDets = null;
        while ((line = br.readLine()) != null)
        {
            parseLine(line);
        }

        if (currentDets != null)
            playSubHistory(currentDets.getHistory());

        if (br != null)
            br.close();

        if (fr != null)
            fr.close();
    }

    private PasswordDetails currentDets = null;

    private void parseLine(String line) throws IOException, PasswordDocumentHistory.HistoryPlaybackException
    {
        if (line.length() == 0)
            return;

        if (line.charAt(0) != '\t')
        {
            if (currentDets != null)
                playSubHistory(currentDets.getHistory());

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
        PasswordDetails dets = addEmptyEntry();
        dets = dets.copy();
        String[] parts = line.split("\t");
        dets.setName(parts[0]);
        dets.setLocation(parts[0]);

        if (parts.length >= 2)
        {
            PasswordDetailsPair pair = dets.addEmptyPair();
            pair.setKey(parts[1]);

            if (parts.length >= 3)
                pair.setValue(parts[2]);

            if (parts.length >= 4)
                throw new IOException("line " + line + " had too many fields for details line");
        }

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
}
