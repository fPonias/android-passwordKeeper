package com.munger.passwordkeeper;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.munger.passwordkeeper.helpers.KeyboardListener;
import com.munger.passwordkeeper.helpers.QuitTimer;

public class MainActivity extends AppCompatActivity
{
	private volatile static MainActivity instance = null;

	public static MainActivity getInstance()
	{
		return instance;
	}
	public static void setInstance(MainActivity inst)
	{
		instance = inst;
	}

	public KeyboardListener keyboardListener;
	private QuitTimer quitTimer;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		instance = this;

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

		MainState.getInstance().setMainActivity(this);
		quitTimer = new QuitTimer();
		keyboardListener = new KeyboardListener(this);

		MainState.getInstance().navigationHelper.openInitialView();
	}

	@Override
	protected void onDestroy()
	{
		quitTimer.stop();

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
		quitTimer.reset();
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
		for(IPermissionResult listener : permissionResults)
			listener.result(requestCode);

		permissionResults = new ArrayList<>();
	}

	public interface IPermissionResult
	{
		void result(int requestCode);
	}

	private ArrayList<IPermissionResult> permissionResults = new ArrayList<>();

	public void addPermisionResultListener(IPermissionResult listener)
	{
		permissionResults.add(listener);
	}
}
