package com.munger.passwordkeeper.struct;

/**
 * Created by codymunger on 11/20/16.
 */

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConfigTest
{
    private ConfigFactoryTest factory;

    private class ConfigFactoryTest extends ConfigFactory
    {
        private String sampleConfig = "{\n" +
                "  \"localDataFilePath\": \"passwords\",\n" +
                "  \"enableImportOption\": true\n" +
                "}";

        private InputStreamReader inr;

        public ConfigFactoryTest()
        {
        }

        public void cleanUp() throws IOException
        {
            inr.close();
        }

        public InputStreamReader getStream()
        {
            InputStream ins = new ByteArrayInputStream(sampleConfig.getBytes());
            inr = new InputStreamReader(ins);
            return inr;
        }
    }

    @Before
    public void before() throws IOException
    {
        factory = new ConfigFactoryTest();
    }

    @After
    public void after() throws IOException
    {
        factory.cleanUp();
    }

    @Test
    public void loads() throws IOException
    {
        Config c = factory.load();
    }
}
