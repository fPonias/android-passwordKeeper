package com.munger.passwordkeeper;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;

import com.munger.passwordkeeper.alert.AlertFragment;
import com.munger.passwordkeeper.alert.InputFragment;
import com.munger.passwordkeeper.alert.PasswordFragment;
import com.munger.passwordkeeper.helpers.KeyboardListener;
import com.munger.passwordkeeper.struct.Config;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDocument;
import com.munger.passwordkeeper.struct.PasswordDocumentFile;
import com.munger.passwordkeeper.struct.PasswordDocumentFileImport;
import com.munger.passwordkeeper.struct.PasswordDocumentHistory;
import com.munger.passwordkeeper.view.AboutFragment;
import com.munger.passwordkeeper.view.CreateFileFragment;
import com.munger.passwordkeeper.view.SettingsFragment;
import com.munger.passwordkeeper.view.ViewDetailFragment;
import com.munger.passwordkeeper.view.ViewFileFragment;

public class MainActivity extends AppCompatActivity
{
	private volatile static MainActivity instance = null;

	public static MainActivity getInstance()
	{
		return instance;
	}

	public PasswordDocument document;
	private PasswordDetails details;
	public String password;

	public Config config;

	public KeyboardListener keyboardListener;
	private SharedPreferences preferences;

	private CreateFileFragment createFileFragment;
	private ViewFileFragment viewFileFragment;
	private ViewDetailFragment viewDetailFragment;
	private SettingsFragment settingsFragment;

	private Thread quitThread;
	private final long QUIT_CHECK_PERIOD = 1000;
	private long quitTime;
	private Object quitLock = new Object();
	private boolean quitCheckerRunning;

	private boolean editable = false;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Intent i = getIntent();
		if (i.hasExtra("wakeup"))
		{
			Log.d("password", "received wake intent");
			long intentQuitTime = i.getLongExtra("quitTime", -1);

			if (intentQuitTime == -1 || (intentQuitTime > 0 && System.currentTimeMillis() > intentQuitTime))
			{
				reset();
			}
		}

		instance = this;

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);


		if (savedInstanceState == null)
		{
			handler = new Handler(Looper.getMainLooper());
			keyboardListener = new KeyboardListener(this);

			try
			{
				config = Config.load();
			}
			catch(Exception e){
				throw new RuntimeException("system config not present");
			}

			viewDetailFragment = null;
			createFileFragment = null;
			settingsFragment = null;

			setPasswordFile();
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

		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		preferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
		{
			if (key.equals(SettingsFragment.PREF_NAME_TIMEOUT_LIST))
			{
				resetQuitTimer();
			}
			else if (key.equals(SettingsFragment.PREF_NAME_SAVE_TO_CLOUD))
			{

			}
		}});

		resetQuitTimer();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		resetQuitTimer();

		return super.onTouchEvent(event);
	}

	protected PendingIntent wakeupIntent = null;

	@Override
	protected void onPause()
	{
		AlarmManager mgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		Intent alarmIntent = new Intent(this, MainActivity.class);
		alarmIntent.putExtra("wakeup", true);
		alarmIntent.putExtra("quitTime", quitTime);
		alarmIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		wakeupIntent = PendingIntent.getActivity(this, 1, alarmIntent, PendingIntent.FLAG_ONE_SHOT);
		mgr.set(AlarmManager.RTC_WAKEUP, quitTime, wakeupIntent);

		super.onPause();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if (wakeupIntent != null)
			wakeupIntent.cancel();
	}

	public void resetQuitTimer()
	{
		String valueStr = preferences.getString(SettingsFragment.PREF_NAME_TIMEOUT_LIST, "5");
		int value = Integer.valueOf(valueStr);
		boolean doStart = false;
		boolean doStop = false;
		synchronized (quitLock)
		{
			if (value > 0)
			{
				if (quitThread == null)
					doStart = true;

				quitTime = System.currentTimeMillis() + value * 60000;
			}
			else if (value == -1)
			{
				if (quitThread != null)
					doStop = true;

				quitTime = Long.MAX_VALUE;
			}
		}

		if (doStop)
			stopQuitTimer();

		if (doStart)
			startQuitTimer();
	}

	private void startQuitTimer()
	{
		synchronized (quitLock)
		{
			if (quitCheckerRunning)
				return;

			quitCheckerRunning = true;
			quitThread = new Thread(new Runnable() {public void run()
			{
				long currentTime;
				while (true)
				{
					synchronized (quitLock)
					{
						if (!quitCheckerRunning)
							return;

						currentTime = System.currentTimeMillis();

						if (currentTime > quitTime)
						{
							handler.post(new Runnable() {public void run()
							{
								Log.d("password", "Timeout reached.  Quitting");
								reset();
							}});
						}
					}

					try
					{
						Thread.sleep(QUIT_CHECK_PERIOD);
					}
					catch(Exception e){
						return;
					}
				}
			}}, "Quit Thread");
			quitThread.start();
		}
	}

	public void stopQuitTimer()
	{
		Thread toQuitThread = null;
		synchronized (quitLock)
		{
			if (!quitCheckerRunning)
				return;

			quitCheckerRunning = false;
			toQuitThread = quitThread;
			quitThread = null;
		}

		try{toQuitThread.join();}catch(InterruptedException e){}
	}

	private Handler handler;
	public Handler getHandler()
	{
		return handler;
	}

	protected void setPasswordFile()
	{
		document = new PasswordDocumentFile(config.localDataFilePath);

		if (!((PasswordDocumentFile)document).exists())
		{
			createFileFragment = new CreateFileFragment();
			getSupportFragmentManager().beginTransaction().add(R.id.container, createFileFragment).commit();
		}
		else
		{
			startGetPassword();
		}
	}

	public void fragmentExists(Fragment frag)
	{
		if (frag instanceof ViewDetailFragment)
			viewDetailFragment = (ViewDetailFragment) frag;
		else if (frag instanceof ViewFileFragment)
			viewFileFragment = (ViewFileFragment) frag;
		else if (frag instanceof CreateFileFragment)
			createFileFragment = (CreateFileFragment) frag;
		else if (frag instanceof SettingsFragment)
			settingsFragment = (SettingsFragment) frag;
	};

	public boolean getEditable()
	{
		return editable;
	}

	public void setEditable(boolean editable)
	{
		this.editable = editable;

		if (viewFileFragment != null)
			viewFileFragment.setEditable(editable);
		if (viewDetailFragment != null)
			viewDetailFragment.setEditable(editable);
	}

@Override
	public void onBackPressed()
	{
		FragmentManager mgr = getSupportFragmentManager();
		int cnt = mgr.getBackStackEntryCount();

		if (cnt > 0)
		{
			BackStackEntry entry = mgr.getBackStackEntryAt(cnt - 1);
			String name = entry.getName();

			boolean keepGoing = true;

			if (name.equals(ViewDetailFragment.getName()))
			{
				keepGoing = viewDetailFragment.backPressed();
			}

			if (!keepGoing)
				return;

			if (cnt > 1)
			{
				super.onBackPressed();
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.action_edit)
		{
			setEditable(!editable);
			return true;
		}
		else if (id == R.id.action_settings)
		{
			openSettings();
			return true;
		}

		return false;
	};

	public void openSettings()
	{
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		settingsFragment = new SettingsFragment();

		trans.replace(R.id.container, settingsFragment);
		trans.addToBackStack(SettingsFragment.getName());
		trans.commit();
	}

	protected void startGetPassword()
	{
		AsyncTask t = new AsyncTask() {protected Object doInBackground(Object[] params)
		{
			getPassword();
			return null;
		}};
		t.execute(new Object(){});
	}

	protected void getPassword()
	{
		final PasswordFragment inDialog = new PasswordFragment("Input the document password", "password", new PasswordFragment.Listener()
		{
			public boolean okay(InputFragment that, String password)
			{
				document.setPassword(password);
				boolean passed = document.testPassword();

				if (!passed)
				{
					that.dismiss();

					AlertFragment frag = new AlertFragment("Incorrect password.");
					frag.show(MainActivity.getInstance().getSupportFragmentManager(), "invalid_fragment");
					frag.setCloseCallback(new AlertFragment.CloseCallback() {public void closed()
					{
						startGetPassword();
					}});

					return false;
				}
				else
				{
					MainActivity.getInstance().password = password;
					openFile();
					return true;
				}
			}

			public void cancel(InputFragment that)
			{
				System.exit(0);
			}
		});
		inDialog.show(getSupportFragmentManager(), "invalid_fragment");
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

	public void reset()
	{
		try
		{
			document.close();
		}
		catch(Exception e){}

		document = null;

		FragmentManager mgr = getSupportFragmentManager();
		int cnt = mgr.getBackStackEntryCount();

		for (int i = 0; i < cnt; i++)
		{
			invalidateOptionsMenu();
			setEditable(false);
			mgr.popBackStack();
		}

		setPasswordFile();
	}

	public void openFile()
	{
		final ProgressDialog loadingDialog = new ProgressDialog(this);
		loadingDialog.setMessage("Decrypting password data");
		loadingDialog.show();

		final PasswordDocument.ILoadEvents listener = new PasswordDocumentFile.ILoadEvents() {
			@Override
			public void detailsLoaded()
			{
				handler.post(new Runnable() {public void run()
				{
					if (loadingDialog.isShowing())
						loadingDialog.dismiss();

					openFile2();
				}});
			}

			@Override
			public void historyLoaded()
			{
			}

			@Override
			public void historyProgress(float progress) {

			}
		};
		document.addLoadEvents(listener);

		AsyncTask t = new AsyncTask()
		{
			protected Object doInBackground(Object[] params)
			{
				try
				{
					document.load(true);
				}
				catch(Exception e){
					AlertFragment frag = new AlertFragment("Failed to open the document: " + document.name);
					frag.show(getSupportFragmentManager(), "invalid_fragment");
				}

				return null;
			}

			protected void onPostExecute(Object o)
			{
				if (loadingDialog.isShowing())
					loadingDialog.dismiss();

				document.removeLoadEvents(listener);
			}
		};

		t.execute(new Object[]{});
	}

	private void openFile2()
	{
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		viewFileFragment = new ViewFileFragment();

		if (document.count() == 0)
		{
			document.addEmptyEntry();
		}

		setEditable(false);

		viewFileFragment.setDocument(document);

		trans.replace(R.id.container, viewFileFragment);
		trans.addToBackStack(ViewFileFragment.getName());
		trans.commit();
	}

	public void importFile(final String path, final Callback callback)
	{
		final ProgressDialog loadingDialog = new ProgressDialog(this);
		loadingDialog.setMessage("Importing password data");
		loadingDialog.show();

		AsyncTask t = new AsyncTask()
		{
			protected Object doInBackground(Object[] params)
			{
				try
				{
					PasswordDocumentFileImport fileImport = new PasswordDocumentFileImport(path, "import");
					fileImport.load(false);
					document.appendDocument(fileImport);
					document.save();
				}
				catch(Exception e){
					AlertFragment frag = new AlertFragment("Failed to import the document: " + path);
					frag.show(getSupportFragmentManager(), "invalid_fragment");
					return false;
				}

				return true;
			}

			protected void onPostExecute(Object o)
			{
				loadingDialog.dismiss();

				AlertFragment frag = new AlertFragment("Successfully imported!");
				frag.show(getSupportFragmentManager(), "invalid_fragment");

				callback.callback(o);
			}
		};
		t.execute(new Object[]{});
	}

	public void deleteData()
	{
		try
		{
			document.delete();
		}
		catch(Exception e){
			AlertFragment frag = new AlertFragment("Failed to delete local password data");
			frag.show(getSupportFragmentManager(), "invalid_fragment");
			return;
		}

		setPasswordFile();
	}

	public void deleteRemoteData()
	{

	}

	public void setFile(String password)
	{
		this.password = password;
		document.setPassword(password);
		openFile();
	}

	public void removeFile()
	{
		try
		{
			document.delete();
		}
		catch(Exception e){
			AlertFragment frag = new AlertFragment("Failed to delete the document: " + document.name);
			frag.show(getSupportFragmentManager(), "invalid_fragment");
		}
	}

	public PasswordDetails getDetails()
	{
		return details;
	}

	public void setDetails(String id)
	{
		PasswordDetails dets = document.getDetails(id);
		details = dets;
	}

	public void openDetail(PasswordDetails detail)
	{
		details = detail;
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();

		viewDetailFragment = new ViewDetailFragment();
		viewDetailFragment.setDetails(details);
		viewDetailFragment.setEditable(editable);

		trans.replace(R.id.container, viewDetailFragment);
		trans.addToBackStack(ViewDetailFragment.getName());
		trans.commit();

	}

	public interface Callback
	{
		void callback(Object result);
	}

	public void saveDetail(final PasswordDetails detail, final Callback callback)
	{
		final ProgressDialog loadingDialog = new ProgressDialog(this);
		loadingDialog.setMessage("Saving password data");
		loadingDialog.show();

		AsyncTask t = new AsyncTask() {
			protected Object doInBackground(Object[] params)
			{
				try
				{
					document.replaceDetails(detail);
					document.save();
					detail.setHistory(new PasswordDocumentHistory());
				}
				catch(Exception e){
					Log.e("password", "failed to update password file");
				}

				return null;
			}

			@Override
			protected void onPostExecute(Object o)
			{
				loadingDialog.dismiss();
				callback.callback(null);
			}
		};
		t.execute(new Object(){});
	}

	public void about()
	{
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		AboutFragment frag = new AboutFragment();

		trans.replace(R.id.container, frag);
		trans.addToBackStack(AboutFragment.getName());
		trans.commit();
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
