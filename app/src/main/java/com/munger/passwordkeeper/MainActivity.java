package com.munger.passwordkeeper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.munger.passwordkeeper.alert.AlertFragment;
import com.munger.passwordkeeper.alert.PasswordFragment;
import com.munger.passwordkeeper.helpers.KeyboardListener;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDocument;
import com.munger.passwordkeeper.struct.PasswordDocumentFile;
import com.munger.passwordkeeper.view.AboutFragment;
import com.munger.passwordkeeper.view.CreateFileFragment;
import com.munger.passwordkeeper.view.ImportFileFragment;
import com.munger.passwordkeeper.view.ViewDetailFragment;
import com.munger.passwordkeeper.view.ViewFileFragment;

public class MainActivity extends AppCompatActivity
{
	public PasswordDocument document;
	public String password;

	public KeyboardListener keyboardListener;

	private CreateFileFragment createFileFragment;
	private ViewFileFragment viewFileFragment;
	private ViewDetailFragment viewDetailFragment;
	private ImportFileFragment importFileFragment;
	private Object quitLock = new Object();
	private Long quitTime;
	private Long quitDelta = 90000L;
	private Thread quitThread;
	private boolean editable = false;
	private PasswordDetails details;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);


		if (savedInstanceState == null)
		{
			keyboardListener = new KeyboardListener(this);

			viewDetailFragment = null;
			importFileFragment = null;
			createFileFragment = null;

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
				document.fromString(doc, false);
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
		document = new PasswordDocumentFile(this, "passwords");

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
		else if (frag instanceof ImportFileFragment)
			importFileFragment = (ImportFileFragment) frag;
		else if (frag instanceof CreateFileFragment)
			createFileFragment = (CreateFileFragment) frag;
	};

	private void setupSample() throws IOException
	{
		String path = getFilesDir().getAbsolutePath() + "/saved/";
		File fpath = new File(path);

		if (!fpath.exists())
			fpath.mkdirs();

		if (fpath.list().length > 0)
			return;

		path += "password is sample";
		File f = new File(path);

		if (f.exists())
			return;

		InputStream ins = getAssets().open("sample");
		PasswordDocument doc = new PasswordDocumentFile(this, "password is sample");
		doc.importFromStream(ins);
		doc.setPassword("sample");
		doc.save();
	}

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

			if (document.details.size() == 0)
			{
				PasswordDetails det = new PasswordDetails();
				det.name = "new entry";
				document.setIndex(det);
				document.details.add(det);
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

	public void setFile(String password)
	{
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

	public void setDetails(String index)
	{
		int sz = document.details.size();
		for (int i = 0; i < sz; i++)
		{
			PasswordDetails dets = document.details.get(i);

			if (dets.index.equals(index))
			{
				details = dets;
				break;
			}
		}
	}

	public void openDetail(PasswordDetails detail)
	{
		details = detail;
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		viewDetailFragment = new ViewDetailFragment();

		if (details.name.isEmpty() || (details.name.equals("new entry") && details.location.isEmpty() && details.details.size() == 0))
		{
			if (details.details.size() == 0)
			{
				details.details.add(new PasswordDetails.Pair());
				setEditable(true);
			}
		}
		else
			setEditable(editable);


		viewDetailFragment.setDetails(details);
		trans.replace(R.id.container, viewDetailFragment);
		trans.addToBackStack(ViewDetailFragment.getName());
		trans.commit();
	}

	public void saveDetail(PasswordDetails detail)
	{
		int listIdx = -1;
		int sz = document.details.size();
		for (int i = 0; i < sz; i++)
		{
			PasswordDetails d = document.details.get(i);
			if (d.index == detail.index)
			{
				listIdx = i;
				break;
			}
		}

		if (listIdx == -1)
			return;

		document.details.remove(listIdx);
		document.details.add(listIdx, detail);

		document.save();
	}

	public void openImportFile()
	{
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		importFileFragment = new ImportFileFragment();

		File sdcardDir = Environment.getExternalStorageDirectory();
		importFileFragment.setDirectory(sdcardDir.getAbsolutePath());

		trans.replace(R.id.container, importFileFragment);
		trans.addToBackStack(ImportFileFragment.getName());
		trans.commit();
	}

	public void importFile(String path)
	{
		try
		{
			document = new PasswordDocumentFile(this, path);
			document.importFromFile(path);

			onBackPressed();
		}
		catch(Exception e){
			AlertFragment inDialog = new AlertFragment("Unable to import file: " + path);
			inDialog.show(getSupportFragmentManager(), "invalid_fragment");
			return;
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
}
