package com.munger.passwordkeeper;

/**
 * Created by codymunger on 11/29/16.
 */

public class TestingMainActivity extends MainActivity
{
    @Override
    protected void init()
    {
        backTriggeredCount = 0;
        backCalledCount = 0;
        doexitCalled = false;
        pauseCalled = false;
    }

    public void superInit()
    {
        super.init();
        try {Thread.sleep(50);} catch(InterruptedException e){}

    }

    private int backTriggeredCount;
    private int backCalledCount;
    private boolean doexitCalled;
    private boolean pauseCalled;

    public boolean getDoexitCalled()
    {
        return doexitCalled;
    }

    public void resetBackCalledCount()
    {
        backTriggeredCount = 0;
        backCalledCount = 0;
    }

    public int getBackCalledCount()
    {
        return backCalledCount;
    }

    public int getBackTriggeredCount()
    {
        return backTriggeredCount;
    }

    public boolean getPauseCalled()
    {
        return pauseCalled;
    }

    public void realOnBackPressed()
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
    public void onBackPressed()
    {
        backTriggeredCount++;
        super.onBackPressed();
    }

    @Override
    public void onUserInteraction() {}
}
