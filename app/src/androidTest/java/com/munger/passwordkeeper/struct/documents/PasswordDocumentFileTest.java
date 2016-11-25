package com.munger.passwordkeeper.struct.documents;

import com.munger.passwordkeeper.Helper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Created by codymunger on 11/23/16.
 */

public class PasswordDocumentFileTest
{
    private TemporaryFolder tmp;

    class PasswordDocumentFileOver extends PasswordDocumentFile
    {
        public PasswordDocumentFileOver(String name)
        {
            super(name);
        }

        public PasswordDocumentFileOver(String name, String password)
        {
            super(name, password);
        }

        @Override
        protected void updateRootPath()
        {
            if (rootPath == null)
                rootPath = tmp.getRoot().getAbsolutePath() + "/";
        }

        public int updateCalled = 0;
        public int overwriteCalled = 0;

        @Override
        protected void saveNewHistory() throws IOException
        {
            overwriteCalled++;
            super.saveNewHistory();
        }

        @Override
        protected void saveUpdatedHistory() throws IOException
        {
            updateCalled++;
            super.saveUpdatedHistory();
        }
    }

    @Before
    public void before() throws IOException
    {
        tmp = new TemporaryFolder();
        tmp.create();
    }

    @After
    public void after() throws IOException
    {
        tmp.delete();
    }

    @Test
    public void constructors() throws IOException
    {
        PasswordDocumentFileOver doc = new PasswordDocumentFileOver(Helper.DEFAULT_NAME, Helper.DEFAULT_PASSWORD);
    }

    @Test
    public void save() throws Exception
    {
        dosave();
    }

    private PasswordDocumentFileOver dosave() throws Exception
    {
        PasswordDocumentFileOver doc = new PasswordDocumentFileOver(Helper.DEFAULT_NAME, Helper.DEFAULT_PASSWORD);
        assertFalse(doc.exists());
        Helper.fillDocument(doc, 5, 5);
        doc.save();


        File tmpFolder = tmp.getRoot();
        File[] subFiles = tmpFolder.listFiles();

        int count = 0;
        for(File file : subFiles)
        {
            String path = file.getAbsolutePath();
            if (path.equals(doc.getDetailsFilePath()))
            {
                count++;
                assertTrue(file.length() > 0);
            }
            else if (path.equals(doc.getHistoryFilePath()))
            {
                count++;
                assertTrue(file.length() > 0);
            }
        }

        assertEquals(2, count);
        assertEquals(1, doc.overwriteCalled);
        return doc;
    }

    @Test
    public void update() throws Exception
    {
        PasswordDocumentFileOver doc = dosave();
        assertEquals(1, doc.overwriteCalled);
        Helper.fillDocument(doc, 3, 5);

        assertTrue(doc.count() > 3);
        doc.save();
        assertEquals(1, doc.overwriteCalled);
        assertEquals(1, doc.updateCalled);
    }

    @Test
    public void load() throws Exception
    {
        PasswordDocumentFileOver doc = dosave();

        PasswordDocumentFileOver doc2 = new PasswordDocumentFileOver(Helper.DEFAULT_NAME, Helper.DEFAULT_PASSWORD);
        assertTrue(doc2.exists());
        assertTrue(doc2.testPassword());
        doc2.load(true);

        assertTrue(doc2.equals(doc));

        doc2.delete();
        assertFalse(doc2.exists());
    }
}
