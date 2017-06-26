package com.munger.passwordkeeper;

import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.munger.passwordkeeper.helpers.KeyboardListener;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.helpers.QuitTimer;
import com.munger.passwordkeeper.view.ViewDetailFragment;

public class MainActivity extends AppCompatActivity
{
	public KeyboardListener keyboardListener;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Intent i = getIntent();

		if (i.hasExtra("reset"))
		{
			savedInstanceState = null;
		}

		if (savedInstanceState == null)
		{
		}
		else
		{
			if (savedInstanceState.containsKey("document"))
			{
			}

			if (savedInstanceState.containsKey("details"))
			{
			}
		}


		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		init();
	}

	protected void init()
	{
		MainState.getInstance().setContext(this, this);

		MainState.getInstance().navigationHelper.openInitialView();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		return MainState.getInstance().navigationHelper.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy()
	{
		if (MainState.getInstance().quitTimer != null)
			MainState.getInstance().quitTimer.stop();

		super.onDestroy();
	}

	@Override
	protected void onResume()
	{
		MainState.getInstance().isActive = true;
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		MainState.getInstance().isActive = false;
		super.onPause();
	}

	@Override
	public void onUserInteraction()
	{
		if (MainState.getInstance().quitTimer != null)
			MainState.getInstance().quitTimer.reset();

		super.onUserInteraction();
	}

	@Override
	public void onBackPressed()
	{
		MainState.getInstance().navigationHelper.onBackPressed(new NavigationHelper.Callback() {public void callback(Object result)
		{
			boolean doBack = (Boolean) result;

			if (doBack)
				realOnBackPressed();
		}});

	}

	@Override
	public void onAttachFragment(Fragment frag)
	{
		if (MainState.getInstance() == null)
			return;
		if (MainState.getInstance().navigationHelper == null)
			return;

		MainState.getInstance().navigationHelper.fragmentAttached(frag);
	}

	public void doexit()
    {
        System.exit(0);
    }

	public void realOnBackPressed()
	{
		super.onBackPressed();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		MainState.getInstance().navigationHelper.notifyPermissionResults(requestCode);
	}

	private Fragment currentFagment = null;

    //only used in test cases
	public void setFragment(Fragment fragment)
	{
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();

		if (fragment == null && currentFagment != null)
			trans.remove(currentFagment);
		else if (fragment != null)
		{
			if (currentFagment != null)
				trans.remove(currentFagment);

			trans.add(R.id.container, fragment);
		}

		currentFagment = fragment;
		trans.commit();
	}

    public Fragment getCurrentFagment()
    {
        FragmentManager fragmentManager = getSupportFragmentManager();
        int sz = fragmentManager.getBackStackEntryCount();
        if (sz == 0)
            return null;

        String tag = fragmentManager.getBackStackEntryAt(sz - 1).getName();
        return fragmentManager.findFragmentByTag(tag);
    }
}
