package com.munger.passwordkeeper;

import java.io.File;
import java.io.IOException;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.munger.passwordkeeper.alert.AlertFragment;
import com.munger.passwordkeeper.alert.PasswordFragment;
import com.munger.passwordkeeper.util.PasswordDetails;
import com.munger.passwordkeeper.util.PasswordDocument;

public class MainActivity extends ActionBarActivity 
{	
	private SelectFileFragment selectFileFragment;
	private CreateFileFragment createFileFragment;
	private ViewFileFragment viewFileFragment;
	private ViewDetailFragment viewDetailFragment;
	private ImportFileFragment importFileFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		Log.v("password", "main onCreate called");
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		selectFileFragment = null;
		createFileFragment = null;
		viewFileFragment = null;
		viewDetailFragment = null;
		importFileFragment = null;
		
		
		if (savedInstanceState == null) 
		{
			selectFileFragment = new SelectFileFragment();
			getSupportFragmentManager().beginTransaction().add(R.id.container, selectFileFragment).commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		FragmentManager mgr = getSupportFragmentManager();
		int cnt = mgr.getBackStackEntryCount();
		
		if (cnt > 0)
		{
			getMenuInflater().inflate(R.menu.main, menu);
		}
		else
		{
			getMenuInflater().inflate(R.menu.open_file, menu);
		}
		
		return true;
	}

	private long lastSelected = 0;
	private MenuItem lastItem = null;
	private boolean editable = false;
	
	public void setEditable(boolean editable)
	{
		this.editable = editable;
		
		if (selectFileFragment != null)
			selectFileFragment.setEditable(editable);
		if (viewFileFragment != null)
			viewFileFragment.setEditable(editable);
		if (viewDetailFragment != null)
			viewDetailFragment.setEditable(editable);
	}
	
	public boolean getEditable()
	{
		return editable;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		long now = System.currentTimeMillis();
		if (now - lastSelected < 10)
		{
			if (lastItem.getItemId() == item.getItemId())
				return false;
		}
		
		lastSelected = now;
		lastItem = item;
		
		int id = item.getItemId();
		if (id == R.id.action_edit)
		{
			setEditable(!editable);
		}
		else if (id == R.id.action_addfile)
		{
			addFile(CreateFileFragment.TYPE_CREATE);
		}
		else if (id == R.id.action_import)
		{
			openImportFile();
		}
		
		return super.onOptionsItemSelected(item);
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
			
			super.onBackPressed();
			invalidateOptionsMenu();
			setEditable(false);
		}
	};
	
	public PasswordDocument document;
	public String currentDoc;
	
	public void openFile(final String file)
	{
		final MainActivity that = this;
		currentDoc = file;
		
		PasswordFragment inDialog = new PasswordFragment("Input the document password", "password", new PasswordFragment.Listener() 
		{
			public void okay(String password) 
			{
				boolean passed = PasswordDocument.testPassword(that, file, password);
				
				if (!passed)
				{
					AlertFragment frag = new AlertFragment("Incorrect password.");
					frag.show(that.getSupportFragmentManager(), "invalid_fragment");
				}
				else
				{
					openFile2(file, password);
				}
			}
			
			public void cancel() 
			{
			}
		});
		inDialog.show(that.getSupportFragmentManager(), "invalid_fragment");
	}
	
	private void openFile2(String file, String password)
	{
		try
		{
			document = new PasswordDocument(this, password);
			document.loadFromFile(file);
			
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			viewFileFragment = new ViewFileFragment();
			
			if (document.details.size() == 0)
			{
				PasswordDetails det = new PasswordDetails();
				det.name = "new entry";
				document.details.add(det);
				
				setEditable(true);
			}
			else
				setEditable(false);
			
			viewFileFragment.setDocument(file, document);
			
			trans.replace(R.id.container, viewFileFragment);
			trans.addToBackStack(ViewFileFragment.getName());
			trans.commit();
		}
		catch(Exception e){
		}
	}
	
	public void addFile(int type)
	{
		if (!(type == CreateFileFragment.TYPE_CREATE || type == CreateFileFragment.TYPE_IMPORT))
			return;
			
		setEditable(false);
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		createFileFragment = new CreateFileFragment();
		createFileFragment.settype(type);
		
		trans.replace(R.id.container, createFileFragment);
		trans.addToBackStack(CreateFileFragment.getName());
		trans.commit();
	}
	
	public void removeFile(String name)
	{
		PasswordDocument.deleteFile(this, name);
	}
	
	public PasswordDetails details;
	
	public void openDetail(PasswordDetails detail)
	{
		details = detail;
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		viewDetailFragment = new ViewDetailFragment();
		
		if (details.name.isEmpty())
		{
			if (details.details.size() == 0)
			{
				details.details.add(new PasswordDetails.Pair());
				setEditable(true);
			}
		}
		else
			setEditable(false);
		
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
		
		try
		{
			document.saveToFile(currentDoc);
		}
		catch(IOException e){
		}
	}
	
	private void openImportFile()
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
			document = new PasswordDocument(this);
			document.importFromFile(path);
			
			onBackPressed();
			
			addFile(CreateFileFragment.TYPE_IMPORT);
		}
		catch(Exception e){
			AlertFragment inDialog = new AlertFragment("Unable to import file: " + path);
			inDialog.show(getSupportFragmentManager(), "invalid_fragment");
			return;
		}
	}
}
