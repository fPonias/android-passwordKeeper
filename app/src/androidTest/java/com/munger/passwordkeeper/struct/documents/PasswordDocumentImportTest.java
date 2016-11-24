package com.munger.passwordkeeper.struct.documents;

/**
 * Created by codymunger on 11/23/16.
 */


import com.munger.passwordkeeper.struct.PasswordDetails;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.Assert.*;

public class PasswordDocumentImportTest
{
    private class ImportTester extends PasswordDocumentFileImport
    {
        public ImportTester(String path, String name)
        {
            super(path, name);
        }

        public ImportTester(String path, String name, String password)
        {
            super(path, name, password);
        }

        public String sampleInput;

        protected BufferedReader getReader()
        {
            StringReader sr = new StringReader(sampleInput);
            BufferedReader ret = new BufferedReader(sr);
            return ret;
        }

        protected void setRootPath()
        {
            rootPath = "";
        }
    }

    private String sampleInput = "name\tusername\tpassword\n" +
            "\n" +
            "name2\tusername2\tpassword2\n" +
            "\tun\tpw\n" +
            "\n" +
            "name3\tusername3\tpassword3\n" +
            "\tun2\tpw2\n" +
            "\tun3\tpw3\n";

    @Test
    public void importTest() throws Exception
    {
        ImportTester tester = new ImportTester("path", "name", "pass");
        tester.sampleInput = this.sampleInput;
        tester.load(false);

        assertEquals(3, tester.count());

        PasswordDetails dets = tester.getDetails(0);
        assertEquals("name", dets.getName());
        assertEquals(1, dets.count());

        dets = tester.getDetails(1);
        assertEquals("name2", dets.getName());
        assertEquals(2, dets.count());

        dets = tester.getDetails(2);
        assertEquals("name3", dets.getName());
        assertEquals(3, dets.count());
    }
}
