package com.munger.passwordkeeper;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.munger.passwordkeeper.helpers.KeyboardListener;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.helpers.QuitTimer;

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
		MainState.getInstance().quitTimer.reset();
		super.onUserInteraction();
	}

	@Override
	public void onBackPressed()
	{
		boolean handled = MainState.getInstance().navigationHelper.onBackPressed();

		if (!handled)
			super.onBackPressed();
	}

	public interface DocumentReset
	{
		void callback();
	}

	private ArrayList<DocumentReset> resetListeners = new ArrayList<>();

	public void addResetListener(DocumentReset listener)
	{
		if (resetListeners.contains(listener))
			return;

		resetListeners.add(listener);
	}

	public void removeResetListener(DocumentReset listener)
	{
		if (!resetListeners.contains(listener))
			return;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		MainState.getInstance().navigationHelper.notifyPermissionResults(requestCode);
	}
}
