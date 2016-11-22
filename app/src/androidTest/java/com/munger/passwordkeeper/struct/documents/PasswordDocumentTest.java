package com.munger.passwordkeeper.struct.documents;

import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by codymunger on 11/22/16.
 */

public class PasswordDocumentTest
{
    private class PasswordDocumentImpl extends PasswordDocument
    {
        public PasswordDocumentImpl(String pass)
        {
            super(pass);
        }

        public PasswordDocumentImpl(String name, String pass)
        {
            super(name, pass);
        }

        public void save() throws Exception
        {

        }

        public void load(boolean force) throws Exception
        {

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

    private final String DEFAULT_NAME = "name";
    private final String DEFAULT_PASSWORD = "pass";

    @Test
    public void constructors()
    {
        PasswordDocumentImpl doc = new PasswordDocumentImpl(DEFAULT_NAME, DEFAULT_PASSWORD);
    }

    @Test
    public void basicManipulation()
    {
        PasswordDocumentImpl doc = new PasswordDocumentImpl(DEFAULT_NAME, DEFAULT_PASSWORD);
        PasswordDetails dets = doc.addEmptyEntry();
        dets.setName("one");
        dets.setLocation("two");
        PasswordDetailsPair pair = dets.addEmptyPair();
        pair.setKey("three");
        pair.setValue("four");
        PasswordDetailsPair pair2 = dets.addEmptyPair();
        pair2.setKey("three");
        pair2.setValue("four");

        assertEquals(1, doc.count());
        assertEquals(1, doc.history.count());

        doc.removeDetails(dets);
        assertEquals(0, doc.count());
        assertEquals(10, doc.history.count());

    }

    @Test
    public void historyLoadedAwaiter() throws Exception
    {
        throw new Exception("not implemented");
    }


}
