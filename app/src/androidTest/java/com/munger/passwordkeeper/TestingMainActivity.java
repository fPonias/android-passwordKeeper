package com.munger.passwordkeeper;

/**
 * Created by codymunger on 11/29/16.
 */

public class TestingMainActivity extends MainActivity
{
    @Override
    protected void init()
    {
        backCalledCount = 0;
    }

    private int backCalledCount;


    public void resetBackCalledCount()
    {
        backCalledCount = 0;
    }

    public int getBackCalledCount()
    {
        return backCalledCount;
    }

    protected void realOnBackPressed()
    {
        backCalledCount++;
    }

    @Override
    public void onUserInteraction() {}
}
