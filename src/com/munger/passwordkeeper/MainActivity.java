/**
 * Copyright 2014 Cody Munger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.munger.passwordkeeper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.munger.passwordkeeper.alert.AlertFragment;
import com.munger.passwordkeeper.alert.PasswordFragment;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDocument;
import com.munger.passwordkeeper.struct.PasswordDocumentFile;
import com.munger.passwordkeeper.view.AboutFragment;
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
	
	private DbxAccountManager dropboxAcctMgr;
	private DbxAccount dropboxAcct;
	private boolean hasDropboxLink;
	
	/**
	 * Gather fragments and bring up the initial screen.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//setup dropbox
	    hasDropboxLink = false;
	    dropboxAcctMgr = DbxAccountManager.getInstance(this.getApplicationContext(), "5xyvkb536ur7sue", "w6wthm9amap6xo4");

		if (dropboxAcctMgr.hasLinkedAccount()) 
		{
			hasDropboxLink = true;
	        dropboxAcct = dropboxAcctMgr.getLinkedAccount();
		}
		
		
		//import the sample data if there is no new data
		try
		{
			setupSample();
		}
		catch(Exception e){
			Log.v("password", "failed to import sample");
		}
		
		//if the app is just starting, bring up the first screen
		if (savedInstanceState == null) 
		{
			selectFileFragment = null;
			createFileFragment = null;
			viewFileFragment = null;
			viewDetailFragment = null;
			importFileFragment = null;
			
			selectFileFragment = new SelectFileFragment();
			getSupportFragmentManager().beginTransaction().add(R.id.container, selectFileFragment).commit();
		}
		else
		{
			if (savedInstanceState.containsKey("document"))
			{
				String doc = savedInstanceState.getString("document");
				String name = savedInstanceState.getString("name");
				password = savedInstanceState.getString("password");
				currentDoc = savedInstanceState.getString("file");
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
	
	
	public void fragmentExists(Fragment frag)
	{
		if (frag instanceof ViewDetailFragment)
			viewDetailFragment = (ViewDetailFragment) frag;
		else if (frag instanceof ViewFileFragment)
			viewFileFragment = (ViewFileFragment) frag;
		else if (frag instanceof SelectFileFragment)
			selectFileFragment = (SelectFileFragment) frag;
		else if (frag instanceof ImportFileFragment)
			importFileFragment = (ImportFileFragment) frag;
		else if (frag instanceof CreateFileFragment)
			createFileFragment = (CreateFileFragment) frag;
	}
	
	/**
	 * if the data directory is empty, populate it with the provided sample file
	 */
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
	
	public void reset()
	{
		//FragmentManager fm = getSupportFragmentManager();
		//int sz = fm.getBackStackEntryCount();
		//for(int i = 0; i < sz; i++)
		//{    
		//	fm.popBackStack();
		//}
	}
	
	/** 
	 * The current document containing passwords.  
	 * Fragments should refer to this variable to determine what data to display.
	 */
	public PasswordDocument document;
	/** The password of the currently loaded password document. */ public String password;
	/** The filename of the currently loaded password document. */ public String currentDoc;
	
	/**
	 * Prompt the user for a password to open the specified file.
	 * Proceed to openFile2 if the password is correct.
	 * @param file the name of the file in the filesystem to decode.
	 */
	public void openFile(final PasswordDocument doc)
	{
		final MainActivity that = this;
		currentDoc = doc.name;
		
		//create an okay/cancel password dialog to get the document password
		PasswordFragment inDialog = new PasswordFragment("Input the document password", "password", new PasswordFragment.Listener() 
		{
			//attempt to decode the file with the provided password
			public boolean okay(String password) 
			{
				document = doc;
				doc.setPassword(password);
				boolean passed = document.testPassword();
				
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
					that.password = password;
					openFile2();
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
	private void openFile2()
	{
		try
		{
			document.load(true);
			
			//open the file viewer fragment
			FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
			viewFileFragment = new ViewFileFragment();
			
			//if this is a new file, it will be empty
			//give it one sample entry for user ease
			if (document.details.size() == 0)
			{
				PasswordDetails det = new PasswordDetails();
				det.name = "new entry";
				document.setIndex(det);
				document.details.add(det);
			}
			
			//make the entries clickable on first view
			setEditable(false);
			
			viewFileFragment.setDocument(document);
			
			//setup the back button
			trans.replace(R.id.container, viewFileFragment);
			trans.addToBackStack(ViewFileFragment.getName());
			trans.commit();
		}
		catch(Exception e){
			AlertFragment frag = new AlertFragment("Failed to open the document: " + document.name);
			frag.show(getSupportFragmentManager(), "invalid_fragment");
		}
	}
	
	public void setFile(String name, String password)
	{
		currentDoc = name;
		document = new PasswordDocumentFile(this, name, password);
		this.password = password;
		boolean passed = document.testPassword();
		
		//display an error of failure
		if (!passed)
			document = null;
		else
			document.load(true);
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
	
	/**
	 * Open the fragment that allows the user to create a new document.
	 * @param type Type can either be CreateFileFragment.TYPE_CREATE or CreateFileFragment.TYPE_IMPORT
	 */
	public void addFile(int type)
	{
		if (!(type == CreateFileFragment.TYPE_CREATE || type == CreateFileFragment.TYPE_IMPORT || type == CreateFileFragment.TYPE_CREATE_DROPBOX))
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
	public void removeFile()
	{
		document.delete();
	}
	
	/**
	 * The currently loaded password details used by the ViewDetailFragment fragment.
	 */
	private PasswordDetails details;
	
	public PasswordDetails getDetails()
	{
		return details;
	}
	
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
		
		document.save();
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
			document = new PasswordDocumentFile(this, path);
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
	
	public boolean hasDropbox()
	{
		return hasDropboxLink;
	}
	
	public DbxAccountManager getDropboxManager()
	{
		return dropboxAcctMgr;
	}
	
	public DbxAccount getDropboxAccount()
	{
		return dropboxAcct;
	}
	
	public void startDropbox()
	{
		dropboxAcctMgr.startLink(this, 1);
	}
	
	public static interface dropboxListener
	{
		public void connected();
	}
	
	private ArrayList<dropboxListener> dblisteners = new ArrayList<dropboxListener>();
	
	public void addDropboxListener(dropboxListener listener)
	{
		if (dblisteners.contains(listener))
			return;
		
		dblisteners.add(listener);
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) 
	{
	    if (requestCode == 1) 
	    {
	        if (resultCode == Activity.RESULT_OK) 
	        {
	            dropboxAcct = dropboxAcctMgr.getLinkedAccount();
	            hasDropboxLink = true;
	            
	            int sz = dblisteners.size();
	            for (int i = 0; i < sz; i++)
	            {
	            	dropboxListener l = dblisteners.get(i);
	            	l.connected();
	            }
	        } 
	        else 
	        {
	        	
	        }
	    } 
	    else 
	    {
	        super.onActivityResult(requestCode, resultCode, data);
	    }
	}
	
	public void about()
	{
		//open the file viewer fragment
		FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
		AboutFragment frag = new AboutFragment();
		
		//setup the back button
		trans.replace(R.id.container, frag);
		trans.addToBackStack(AboutFragment.getName());
		trans.commit();
	}
}
