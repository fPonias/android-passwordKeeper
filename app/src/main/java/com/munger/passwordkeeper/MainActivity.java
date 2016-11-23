package com.munger.passwordkeeper;

import java.util.ArrayList;

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
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;

import com.munger.passwordkeeper.alert.AlertFragment;
import com.munger.passwordkeeper.alert.InputFragment;
import com.munger.passwordkeeper.alert.PasswordFragment;
import com.munger.passwordkeeper.helpers.DriveHelper;
import com.munger.passwordkeeper.helpers.KeyboardListener;
import com.munger.passwordkeeper.struct.Config;
import com.munger.passwordkeeper.struct.ConfigFactory;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentDrive;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFileImport;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;
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
	public static void setInstance(MainActivity inst)
	{
		instance = inst;
	}

	public PasswordDocument document;
	private PasswordDetails details;
	public String password;

	public Config config;

	public KeyboardListener keyboardListener;
	public SharedPreferences preferences;
	public DriveHelper driveHelper;

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
		instance = this;

		Intent i = getIntent();

		if (i.hasExtra("reset"))
		{
			savedInstanceState = null;
		}

		if (savedInstanceState == null)
		{
			try
			{
				config = new ConfigFactory().load();
			}
			catch(Exception e){
				throw new RuntimeException("system config not present");
			}

			viewDetailFragment = null;
			createFileFragment = null;
			settingsFragment = null;
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
				setupDriveHelper();
			}
		}});

		resetQuitTimer();


		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		handler = new Handler(Looper.getMainLooper());
		keyboardListener = new KeyboardListener(this);
		setPasswordFile();
	}

	@Override
	protected void onDestroy()
	{
		try
		{
			document.close();
		}
		catch(Exception e){}

		super.onDestroy();
	}

	protected boolean isActive = false;

	@Override
	protected void onResume()
	{
		isActive = true;

		super.onResume();
	}

	@Override
	protected void onPause()
	{
		isActive = false;

		super.onPause();
	}

	public void reset()
	{
		try
		{
			document.close();
		}
		catch(Exception e){}

		if (isActive)
		{
			if (gettingPassword)
				System.exit(0);
			else
			{
				Intent i = new Intent(this, MainActivity.class);
				i.putExtra("reset", true);
				startActivity(i);
			}
		}
		else
		{
			System.exit(0);
		}
	}

	private PasswordDocumentDrive driveDocument;

	public void setupDriveHelper()
	{
		if (driveHelper == null)
			driveHelper = new DriveHelper();

		boolean enable = preferences.getBoolean(SettingsFragment.PREF_NAME_SAVE_TO_CLOUD, false);
		if (enable && driveDocument == null)
		{
			driveHelper.connect();
			driveDocument = new PasswordDocumentDrive(document);
		}
		else if (!enable && driveDocument != null)
		{
			driveHelper.cleanUp();
			driveDocument = null;
		}
	}

	@Override
	public void onUserInteraction()
	{
		resetQuitTimer();
		super.onUserInteraction();
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
								quitCheckerRunning = false;
								quitThread = null;

								reset();
								return;
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

	protected boolean gettingPassword = false;

	protected void startGetPassword()
	{
		gettingPassword = true;
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
					gettingPassword = false;
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
				setupDriveHelper();
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
			PasswordDetails dets = new PasswordDetails();
			try{document.addDetails(dets);}catch(Exception e){}
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
					document.playSubHistory(fileImport.getHistory());
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
