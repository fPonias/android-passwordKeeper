package com.munger.passwordkeeper.struct.documents;

import org.junit.Test;

/**
 * Created by codymunger on 11/23/16.
 */

public class PasswordDocumentFileTest
{
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

        }
    }

    @Test
    public void constructors()
    {

    }
}
