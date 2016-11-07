package com.munger.passwordkeeper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import com.munger.passwordkeeper.alert.AlertFragment;
import com.munger.passwordkeeper.alert.PasswordFragment;
import com.munger.passwordkeeper.helpers.KeyboardListener;
import com.munger.passwordkeeper.struct.Config;
import com.munger.passwordkeeper.struct.HistoryEventFactory;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import com.munger.passwordkeeper.struct.PasswordDocument;
import com.munger.passwordkeeper.struct.PasswordDocumentFile;
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

	private CreateFileFragment createFileFragment;
	private ViewFileFragment viewFileFragment;
	private ViewDetailFragment viewDetailFragment;
	private SettingsFragment settingsFragment;

	private Object quitLock = new Object();
	private Long quitTime;
	private Long quitDelta = 90000L;
	private Thread quitThread;

	private boolean editable = false;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		instance = this;

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);


		if (savedInstanceState == null)
		{
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
				String doc = savedInstanceState.getString("document");
				String name = savedInstanceState.getString("name");
				password = savedInstanceState.getString("password");
				document = new PasswordDocumentFile(this, name, password);

				try
				{
					document.fromString(doc, false);
				}
				catch(Exception e){
					Log.e("password", "failed to load file " + doc);
				}
			}

			if (savedInstanceState.containsKey("details"))
			{
				String detStr = savedInstanceState.getString("details");
				details = new PasswordDetails();
				details.fromString(detStr);
			}
		}
	}

	protected void setPasswordFile()
	{
		document = new PasswordDocumentFile(this, config.localDataFilePath);

		if (!((PasswordDocumentFile)document).exists())
		{
			createFileFragment = new CreateFileFragment();
			getSupportFragmentManager().beginTransaction().add(R.id.container, createFileFragment).commit();
		}
		else
		{
			getPassword();
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
				invalidateOptionsMenu();
				setEditable(false);
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

	protected void getPassword()
	{
		final MainActivity that = this;

		PasswordFragment inDialog = new PasswordFragment("Input the document password", "password", new PasswordFragment.Listener()
		{
			public boolean okay(String password)
			{
				document.setPassword(password);
				boolean passed = document.testPassword();

				if (!passed)
				{
					AlertFragment frag = new AlertFragment("Incorrect password.");
					frag.show(that.getSupportFragmentManager(), "invalid_fragment");
					return false;
				}
				else
				{
					that.password = password;
					openFile();
					return true;
				}
			}

			public void cancel()
			{
				System.exit(0);
			}
		});
		inDialog.show(that.getSupportFragmentManager(), "invalid_fragment");
	}

	public void openFile()
	{
		try
		{
			document.load(true);

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
		catch(Exception e){
			AlertFragment frag = new AlertFragment("Failed to open the document: " + document.name);
			frag.show(getSupportFragmentManager(), "invalid_fragment");
		}
	}

	public void importFile(String path)
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
		document.delete();
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

	public void saveDetail(PasswordDetails detail)
	{
		try
		{
			document.replaceDetails(detail);
			document.save();
		}
		catch(Exception e){
			Log.e("password", "failed to update password file");
		}
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
