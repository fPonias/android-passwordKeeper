package com.munger.passwordkeeper;

import java.io.File;
import java.io.IOException;

import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.munger.passwordkeeper.alert.AlertFragment;
import com.munger.passwordkeeper.alert.PasswordFragment;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDocument;
import com.munger.passwordkeeper.view.CreateFileFragment;
import com.munger.passwordkeeper.view.ImportFileFragment;
import com.munger.passwordkeeper.view.SelectFileFragment;
import com.munger.passwordkeeper.view.ViewDetailFragment;
import com.munger.passwordkeeper.view.ViewFileFragment;

/**
 * This is the main activity launched by the manifest.
 * MainActivity contains the fragments for creating new documents, importing documents, 
 * and viewing documents.
 *
 */
public class MainActivity extends ActionBarActivity 
{	
	/** Fragment for selecting an existing document */ private SelectFileFragment selectFileFragment;
	/** Fragment for creating a new document */ private CreateFileFragment createFileFragment;
	/** Fragment for viewing an existing document */ private ViewFileFragment viewFileFragment;
	/** Fragment for viewing a detail in a document */ private ViewDetailFragment viewDetailFragment;
	/** Fragment for importing an external document */ private ImportFileFragment importFileFragment;
	
	
	/**
	 * Gather fragments and bring up the initial screen.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		selectFileFragment = null;
		createFileFragment = null;
		viewFileFragment = null;
		viewDetailFragment = null;
		importFileFragment = null;
		
		//if the app is just starting, bring up the first screen
		if (savedInstanceState == null) 
		{
			selectFileFragment = new SelectFileFragment();
			getSupportFragmentManager().beginTransaction().add(R.id.container, selectFileFragment).commit();
		}
	}

	/** Keep track of the editable state of this Activity */ private boolean editable = false;
	
	/**
	 * Sets the editable mode for this Activity.
	 * Most of the views in this Activity have two states:  editable and viewable.
	 * It works to pass on the editable information to the fragments whenever it gets set.
	 * @param editable set true to set all fragments to editable mode
	 */
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
	
	/**
	 * Get the current editable state.
	 */
	public boolean getEditable()
	{
		return editable;
	}
	
	/**
	 * Handle the behaviour of the back button.
	 */
	@Override
	public void onBackPressed()
	{
		FragmentManager mgr = getSupportFragmentManager();
		int cnt = mgr.getBackStackEntryCount();
		
		if (cnt > 0)
		{
			BackStackEntry entry = mgr.getBackStackEntryAt(cnt - 1);
			String name = entry.getName();
			
			//warn any fragments that care that the back button has been pressed.
			//if they return true then cancel the back button action
			boolean keepGoing = true;
			
			if (name.equals(ViewDetailFragment.getName()))
			{
				keepGoing = viewDetailFragment.backPressed();
			}
			
			if (!keepGoing)
				return;
			
			//reset the view every time a fragment is popped off the stack
			super.onBackPressed();
			invalidateOptionsMenu();
			setEditable(false);
		}
	};
	
	/** 
	 * The current document containing passwords.  
	 * Fragments should refer to this variable to determine what data to display.
	 */
	public PasswordDocument document;
	
	/** The filename of the currently loaded password document. */ public String currentDoc;
	
	/**
	 * Prompt the user for a password to open the specified file.
	 * Proceed to openFile2 if the password is correct.
	 * @param file the name of the file in the filesystem to decode.
	 */
	public void openFile(final String file)
	{
		final MainActivity that = this;
		currentDoc = file;
		
		//create an okay/cancel password dialog to get the document password
		PasswordFragment inDialog = new PasswordFragment("Input the document password", "password", new PasswordFragment.Listener() 
		{
			//attempt to decode the file with the provided password
			public boolean okay(String password) 
			{
				boolean passed = PasswordDocument.testPassword(that, file, password);
				
				//display an error of failure
				if (!passed)
				{
					AlertFragment frag = new AlertFragment("Incorrect password.");
					frag.show(that.getSupportFragmentManager(), "invalid_fragment");
					return false;
				}
				//move on to actually opening the file otherwise
				else
				{
					openFile2(file, password);
					return true;
				}
			}
			
			//do nothing on cancel
			public void cancel() 
			{
			}
		});
		inDialog.show(that.getSupportFragmentManager(), "invalid_fragment");
	}
	
	/**
	 * Load the specified file into memory.
	 * @param file the name of the file to load in the filesystem.
	 * @param password the password used to decrypt the file.
	 */
	private void openFile2(String file, String password)
	{
		try
		{
			document = new PasswordDocument(this, password);
			document.loadFromFile(file);
			
			//open the file viewer fragment
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			viewFileFragment = new ViewFileFragment();
			
			//if this is a new file, it will be empty
			//give it one sample entry for user ease
			if (document.details.size() == 0)
			{
				PasswordDetails det = new PasswordDetails();
				det.name = "new entry";
				document.details.add(det);
			}
			
			//make the entries clickable on first view
			setEditable(false);
			
			viewFileFragment.setDocument(file, document);
			
			//setup the back button
			trans.replace(R.id.container, viewFileFragment);
			trans.addToBackStack(ViewFileFragment.getName());
			trans.commit();
		}
		catch(Exception e){
			AlertFragment frag = new AlertFragment("Failed to open the document: " + file);
			frag.show(getSupportFragmentManager(), "invalid_fragment");
		}
	}
	
	/**
	 * Open the fragment that allows the user to create a new document.
	 * @param type Type can either be CreateFileFragment.TYPE_CREATE or CreateFileFragment.TYPE_IMPORT
	 */
	public void addFile(int type)
	{
		if (!(type == CreateFileFragment.TYPE_CREATE || type == CreateFileFragment.TYPE_IMPORT))
			return;
			
		//open the create file fragment
		setEditable(false);
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		createFileFragment = new CreateFileFragment();
		createFileFragment.settype(type);
		
		//setup the back button
		trans.replace(R.id.container, createFileFragment);
		trans.addToBackStack(CreateFileFragment.getName());
		trans.commit();
	}
	
	/**
	 * Delete the specified file from the file system irrecoverably.
	 * @param name the name of the file the delete.
	 */
	public void removeFile(String name)
	{
		PasswordDocument.deleteFile(this, name);
	}
	
	/**
	 * The currently loaded password details used by the ViewDetailFragment fragment.
	 */
	public PasswordDetails details;
	
	/**
	 * Open the specified detail into a new ViewDetailFragment
	 * @param detail
	 */
	public void openDetail(PasswordDetails detail)
	{
		details = detail;
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		viewDetailFragment = new ViewDetailFragment();
		
		//if the detail has no data, give it a sample to ease the user experience
		//make it editable as well.
		if (details.name.isEmpty() || (details.name.equals("new entry") && details.location.isEmpty() && details.details.size() == 0))
		{
			if (details.details.size() == 0)
			{
				details.details.add(new PasswordDetails.Pair());
				setEditable(true);
			}
		}
		else
			setEditable(false);
		
		//setup the back button
		viewDetailFragment.setDetails(details);
		trans.replace(R.id.container, viewDetailFragment);
		trans.addToBackStack(ViewDetailFragment.getName());
		trans.commit();
	}
	
	/**
	 * Save the modified details to the opened password document
	 * @param detail
	 */
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
			AlertFragment inDialog = new AlertFragment("Unable to save file: " + currentDoc);
			inDialog.show(getSupportFragmentManager(), "invalid_fragment");
		}
	}
	
	/**
	 * Bring up the import Fragment to select a file to import
	 */
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
	
	/**
	 * Attempt to import the selected file.
	 * @param path
	 */
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
