package com.munger.passwordkeeper.struct;

import android.content.res.AssetManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.munger.passwordkeeper.MainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Config
{
    public String localDataFilePath;
    public boolean enableImportOption;

    public Config() throws IOException
    {
    }

    public static Config load() throws IOException
    {
        AssetManager assetManager = MainActivity.getInstance().getAssets();
        InputStream ims = assetManager.open("config.json");
        InputStreamReader inr = new InputStreamReader(ims);
        Gson gson = new GsonBuilder().create();
        Config ret = gson.fromJson(inr, Config.class);
        inr.close(); ims.close();

        return ret;
    }
}
