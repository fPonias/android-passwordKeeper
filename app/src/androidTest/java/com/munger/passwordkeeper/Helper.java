package com.munger.passwordkeeper;

import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentTest;

import java.util.Random;

/**
 * Created by codymunger on 11/20/16.
 */

public class Helper
{
    public static char[] lowercase = new char[] {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    public static char[] uppercase = new char[] {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'Q', 'R', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
    public static char[] numbers = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    public static char[] symbols = new char[] {'~','`','!','@','#','$','%','^','&','*','(',')','{','[','}',']','\\','|',':',';','"','\'','<',',','>','.','?','/'};

    public static String longString()
    {
        Random r = new Random();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 1024; i++)
        {
            int type = r.nextInt(4);
            int index = 0;

            switch(type)
            {
                case 0:
                    index = r.nextInt(lowercase.length);
                    b.append(lowercase[index]);
                    break;
                case 1:
                    index = r.nextInt(uppercase.length);
                    b.append(uppercase[index]);
                    break;
                case 2:
                    index = r.nextInt(numbers.length);
                    b.append(numbers[index]);
                    break;
                case 3:
                    index = r.nextInt(symbols.length);
                    b.append(symbols[index]);
                    break;
            }
        }

        return b.toString();
    }


    public static class PasswordDocumentImpl extends PasswordDocument
    {
        public PasswordDocumentImpl()
        {
            super(DEFAULT_NAME, DEFAULT_PASSWORD);
        }

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

    public static final String DEFAULT_NAME = "name";
    public static final String DEFAULT_PASSWORD = "pass";

    public static PasswordDocumentImpl generateDocument(int detSz, int pairSz)
    {
        PasswordDocumentImpl doc = new PasswordDocumentImpl(DEFAULT_NAME, DEFAULT_PASSWORD);
        for (int i = 0; i < detSz; i++)
        {
            PasswordDetails dets = new PasswordDetails();
            dets.setName("name" + i);
            dets.setLocation("loc" + i);

            for (int j = 0; j < pairSz; j++)
            {
                PasswordDetailsPair pair = dets.addEmptyPair();
                pair.setKey("key" + i + j);
                pair.setValue("val" + i + j);
            }

            try {
                doc.addDetails(dets);
            } catch(Exception e){}
        }

        return doc;
    }
}
