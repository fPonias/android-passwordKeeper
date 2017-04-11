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
        doexitCalled = false;
        pauseCalled = false;
    }

    public void superInit()
    {
        super.init();
    }

    private int backCalledCount;
    private boolean doexitCalled;
    private boolean pauseCalled;

    public boolean getDoexitCalled()
    {
        return doexitCalled;
    }

    public void resetBackCalledCount()
    {
        backCalledCount = 0;
    }

    public int getBackCalledCount()
    {
        return backCalledCount;
    }

    public boolean getPauseCalled()
    {
        return pauseCalled;
    }

    protected void realOnBackPressed()
    {
        backCalledCount++;
        super.realOnBackPressed();
    }

    public void doexit()
    {
        doexitCalled = true;
    }

    @Override
    protected void onPause()
    {
        pauseCalled = true;
        super.onPause();
    }

    @Override
    public void onUserInteraction() {}
}
