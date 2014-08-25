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

package com.munger.passwordkeeper.view;

import java.io.File;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.alert.AlertFragment;
import com.munger.passwordkeeper.view.widget.FileItemWidget;

/**
 * Provides a file explorer for selecting a file to import.
 * Once the file is slected, the CreateFileFragment is summoned to completed the document creation process
 * @author codymunger
 *
 */
public class ImportFileFragment extends Fragment 
{
	/** One view to contain them all */ private RelativeLayout root;
	/** One activity to bind them */ private MainActivity parent;
	
	/** button to go the next directory up in the file system */ private ImageButton upButton;
	/** button to reload the directory contents */ private ImageButton reloadButton;
	/** button that displays the current working directory */ private EditText pathInput;
	/** a list of files in the current directory */ private ListView directoryList;
	
	/** object that converts the file list to widgets for the directoryList */ private ArrayAdapter<String> directoryListAdapter;
	/** the current working directory */ private String currentDir = null;
	/** a potential directory this fragment will move to */ private String nextDir = null;
	
	
	public ImportFileFragment()
	{
		
	}
	
	public static String getName()
	{
		return "Import";
	}
	
	public void setEditable(boolean editable)
	{}
	
	@Override
	public void onSaveInstanceState(Bundle outState) 
	{
		outState.putString("path", currentDir);
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		this.parent = (MainActivity) getActivity();
	
		if (savedInstanceState != null)
		{
			setDirectory(savedInstanceState.getString("path"));
		}
		
		super.onCreate(savedInstanceState);
	};
	
	/**
	 * get references to important components, setup the event handlers
	 */
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		//get component references
		root = (RelativeLayout) inflater.inflate(R.layout.fragment_importfile, container, false);
		upButton = (ImageButton) root.findViewById(R.id.import_uplevelbtn);
		reloadButton = (ImageButton) root.findViewById(R.id.import_refreshbtn);
		pathInput = (EditText) root.findViewById(R.id.import_pathinput);
		directoryList = (ListView) root.findViewById(R.id.import_filelist);
		
		if (currentDir != null)
		{
			populateViews();
		}
		
		//setup event listeners
		//move to the previous directory
		upButton.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
		{
			navigateUp();
		}});
		
		//reload the directory list view
		reloadButton.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
		{
			populateViews();
		}});
		
		//set the current working directory and reload if the path is manually inputted
		pathInput.setOnKeyListener(new View.OnKeyListener() {public boolean onKey(View v, int keyCode, KeyEvent event) 
		{
			if (keyCode == KeyEvent.KEYCODE_ENTER)
			{
				nextDir = pathInput.getText().toString();
				populateViews();
				return true;
			}
			
			return false;
		}});
		
		return root;
	}
	
	/**
	 * set the current working directory of this view.
	 * @param directory the directory to scan
	 */
	public void setDirectory(String directory)
	{
		nextDir = directory;
		currentDir = nextDir;
		
		if (root != null)
			populateViews();
	}
	
	/**
	 * check if the new directory is valid and build a new file list based on it
	 */
	private void populateViews()
	{
		//error checking
		File f = new File(nextDir);
		if (!f.exists())
		{
			AlertFragment inDialog = new AlertFragment("Invalid File Path");
			inDialog.show(parent.getSupportFragmentManager(), "invalid_fragment");
			return;
		}
		
		if (!f.isDirectory())
		{
			AlertFragment inDialog = new AlertFragment("Provided path is not a directory");
			inDialog.show(parent.getSupportFragmentManager(), "invalid_fragment");
			return;
		}
		
		//populate the directoryList
		String[] files = f.list();
		
		directoryListAdapter = new DetailArrayAdapter(this, this.parent, files);
		directoryList.setAdapter(directoryListAdapter);
		
		//update the cwd
		currentDir = nextDir;
		pathInput.setText(currentDir);
	}
	
	/**
	 * extention of the fileitem widget that adds fragment specific event handlers to the widget
	 * fileitemwidget display a file item in the directory with an icon and name.
	 */
	private static class MyFileItemWidget extends FileItemWidget
	{
		private String name;
		private String path;

		public MyFileItemWidget(String path, String name, Context context) 
		{
			super(context);
			
			this.path = path;
			this.name = name;
			
			setLabel(name);
		}
		
		public void setLabel(String path, String name)
		{
			//set the label with the file name
			super.setLabel(name);
			
			//check if the file is a file or directory and set the icon accordingly
			this.path = path;
			this.name = name;
			File f = new File(path + "/" + name);

			Resources r = getContext().getResources();
			
			if (f.isDirectory())
			{
				Drawable d = r.getDrawable(R.drawable.ic_action_storage);
				setIcon(d);
			}
			else
			{
				Drawable d = r.getDrawable(R.drawable.abc_ab_bottom_transparent_dark_holo);
				setIcon(d);
			}
		}
	}
	
	/**
	 * An object to convert the file list in to a listView full of FileItemWidget views
	 */
	private static class DetailArrayAdapter extends ArrayAdapter<String>
	{
		private ImportFileFragment host;
		
		public DetailArrayAdapter(ImportFileFragment host, Context context, String[] objects) 
		{
			super(context, R.layout.widget_fileitem, objects);
			
			this.host = host;
		}
		
		/**
		 * Convert a directory entry into a FileItemWidget view.
		 * 
		 */
		@Override
		public View getView(int position, View convertView, final ViewGroup par) 
		{	
			final String item = getItem(position);
			final MyFileItemWidget ret;
			
			//create the view and event handlers if it's not being recycled
			if (convertView == null)
			{
				ret = new MyFileItemWidget(host.currentDir, item, getContext());
				
				ret.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
				{
					File f = new File(ret.path + "/" + ret.name);

					//if this view is representing a directory, then try to open that directory
					if (f.isDirectory())
					{
						host.nextDir = f.getAbsolutePath();
						host.populateViews();
					}
					//if this view is representing a file, they try to import that file
					else if (f.isFile())
					{
						host.parent.importFile(f.getAbsolutePath());
					}
				}});
			}
			else
				ret = (MyFileItemWidget) convertView;
			
			ret.setLabel(host.currentDir, item);
			
			return ret;
		}
	}
	
	/**
	 * change directory to .. if possible
	 */
	private void navigateUp()
	{
		//split the path and discard the last directory
		String[] parts = currentDir.split("/");
		
		int sz = parts.length;
		int i = sz - 1;
		
		while (i > 0 && parts[i].isEmpty())
			i--;
		
		i--;
		
		//set to "/" if there's no path left
		if (i < 0)
			nextDir = "/";
		//otherwise reconstruct the path
		else
		{
			nextDir = "";
			
			for (int j = 0; j <= i; j++)
			{
				if (j > 0)
					nextDir += "/";
				
				nextDir += parts[j];
			}
		}
		
		//update all the views
		populateViews();
	}
}
