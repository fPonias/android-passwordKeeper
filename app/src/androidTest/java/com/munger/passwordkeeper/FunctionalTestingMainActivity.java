package com.munger.passwordkeeper;

/**
 * Created by codymunger on 3/20/17.
 */

public class FunctionalTestingMainActivity extends MainActivity
{
    @Override
    protected void init()
    {
        backCalledCount = 0;
        doexitCalled = false;
        super.init();
    }

    private int backCalledCount;
    private boolean doexitCalled;

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
    public void onUserInteraction()
    {
        super.onUserInteraction();
    }
}
