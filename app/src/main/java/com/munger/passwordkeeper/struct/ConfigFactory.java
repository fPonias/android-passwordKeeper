package com.munger.passwordkeeper.struct;

import android.content.res.AssetManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.MainState;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import androidx.fragment.app.FragmentActivity;

public class ConfigFactory
{
    public ConfigFactory()
    {
    }

    public Config load() throws IOException
    {
        InputStreamReader inr = getStream();
        Gson gson = new GsonBuilder().create();
        Config ret = gson.fromJson(inr, Config.class);
        inr.close();

        return ret;
    }

    public InputStreamReader getStream() throws IOException
    {
        FragmentActivity act = MainState.getInstance().activity;
        AssetManager assetManager = act.getAssets();
        InputStream ims = assetManager.open("config.json");
        return new InputStreamReader(ims);
    }
}
