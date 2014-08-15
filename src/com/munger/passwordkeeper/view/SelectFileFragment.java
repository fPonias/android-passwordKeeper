package com.munger.passwordkeeper.view;

import java.io.File;
import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.alert.ConfirmFragment;
import com.munger.passwordkeeper.view.widget.TextInputWidget;

/**
 * This fragment allows the user to select a file from the saved files directory and have the activity load it up
 */
public class SelectFileFragment extends Fragment 
{
	private MainActivity parent;
	private View rootView;
	
	private ArrayList<String> fileList;
	private String targetDir;

	private LinearLayout listView;
	
	public SelectFileFragment()
	{
		super();
	}
	
	public static String getName()
	{
		return "Select";
	}
	
	/**
	 * get references to important components and update visual components
	 */
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		this.parent = (MainActivity) getActivity();
		targetDir = parent.getFilesDir().getAbsolutePath() + "/saved";

		setHasOptionsMenu(true);
		
		File dir = new File(targetDir);
		
		if (!dir.exists())
			dir.mkdirs();
		
		
		rootView = inflater.inflate(R.layout.fragment_selectfile, container, false);
		
		listView = (LinearLayout) rootView.findViewById(R.id.selectfile_filelist);
		
		return rootView;
	}
	
	@Override
	public void onCreateOptionsMenu(android.view.Menu menu, android.view.MenuInflater inflater) 
	{
		inflater.inflate(R.menu.open_file, menu);
	};
	
	/**
	 * Handle a selection from the options menu.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		//do something with the selected item
		int id = item.getItemId();
		if (id == R.id.action_edit)
		{
			setEditable(!editable);
		}
		else if (id == R.id.action_addfile)
		{
			parent.addFile(CreateFileFragment.TYPE_CREATE);
		}
		else if (id == R.id.action_import)
		{
			parent.openImportFile();
		}
		
		return true;
	}
	
	private boolean editable = false;
	
	/**
	 * Set the UI to be editable or not
	 */
	public void setEditable(boolean editable)
	{
		View v = getView();
		if (v == null)
			return;
		
		//bring up the delete button on all listed files
		int sz = listView.getChildCount();
		for (int i = 0; i < sz; i++)
		{
			TextInputWidget ti = (TextInputWidget) listView.getChildAt(i);
			ti.setEditable(editable);
		}
	}
	
	@Override
	public void onResume() 
	{
		update();
		super.onResume();
	};
	
	public void update()
	{	
		//update the file list
		File dir = new File(targetDir);
		String[] files = dir.list();
		fileList = new ArrayList<String>();
		
		for (String file : files)
		{
			fileList.add(file);
		}
		
		if (rootView == null)
			return;
			
		//setup create or reuse filelist widgets
		int i = 0;
		int sz = listView.getChildCount();
		for (String file : fileList)
		{
			final TextInputWidget widget;
			
			//if the widget exists then recycle it
			if (i < sz)
			{
				widget = (TextInputWidget) listView.getChildAt(i);
				widget.setText(file);
			}
			//otherwise create the widget
			//and setup event handlers
			else
			{
				widget = new TextInputWidget(getActivity());
				widget.setText(file);
				
				//open the file on tap
				widget.setLabelClickedListener(new TextInputWidget.LabelClickedListener() {public void clicked() 
				{
					parent.openFile(widget.getText());
				}});

				//delete the file is the delete button is pressed
				widget.setDeleteClickedListener(new TextInputWidget.DeleteClickedListener() {public void clicked() 
				{
					deleteFile(widget.getText());
				}});

				listView.addView(widget);
			}
			
			i++;
		}

		//remove non-recycled views
		int flsz = fileList.size();
		for (i = flsz; i < sz; i++)
		{
			listView.removeViewAt(flsz);
		}
		
		setEditable(((MainActivity) getActivity()).getEditable());
	}
		
	/**
	 * Delete the file permanently if confirmed.
	 * Bring up a prompt to ask the user if they really want to delete the selected file.
	 * @param name the name of the file to be deleted.
	 */
	private void deleteFile(final String name)
	{
		final SelectFileFragment that = this;
		ConfirmFragment frag = new ConfirmFragment("Are you sure you want to delete " + name + "?", new ConfirmFragment.Listener() 
		{
			public void okay() 
			{
				parent.removeFile(name);
				that.onResume();
			}
			
			public void cancel() 
			{}
		});
		frag.show(parent.getSupportFragmentManager(), "confirm_fragment");
	}
}
