package com.munger.passwordkeeper.struct.documents;

/**
 * Created by codymunger on 11/23/16.
 */


import com.munger.passwordkeeper.Helper;
import com.munger.passwordkeeper.struct.history.HistoryEventFactory;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import static org.junit.Assert.*;

public class PasswordDocumentStreamTest
{
    @Test
    public void works() throws Exception
    {
        PasswordDocument doc = Helper.generateDocument(3, 3);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        doc.deltasToEncryptedString(dos);
        byte[] bytes = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        DataInputStream dis = new DataInputStream(bais);
        PasswordDocumentStream docin = new PasswordDocumentStream(dis, bytes.length);
        docin.setPassword(Helper.DEFAULT_PASSWORD);
        docin.load(false);
        dis.close(); bais.close();

        assertTrue(doc.history.equals(docin.history));
    }
}
