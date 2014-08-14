package com.munger.passwordkeeper;

import java.io.File;
import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.munger.passwordkeeper.alert.ConfirmFragment;
import com.munger.passwordkeeper.widget.TextInputWidget;

public class SelectFileFragment extends Fragment 
{
	private MainActivity parent;
	private ArrayList<String> fileList;
	private String targetDir;
	
	private Button addButton;
	private LinearLayout listView;
	
	public SelectFileFragment()
	{
		super();
	}
	
	public static String getName()
	{
		return "Select";
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		this.parent = (MainActivity) getActivity();
		targetDir = parent.getFilesDir().getAbsolutePath() + "/saved";
		
		File dir = new File(targetDir);
		
		if (!dir.exists())
			dir.mkdirs();
		
		
		View rootView = inflater.inflate(R.layout.fragment_selectfile, container, false);
		
		listView = (LinearLayout) rootView.findViewById(R.id.selectfile_filelist);
		
		return rootView;
	}
	
	public void setEditable(boolean editable)
	{
		View v = getView();
		if (v == null)
			return;
		
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
		update(getView());
		super.onResume();
	};
	
	public void update(View rootView)
	{	
		File dir = new File(targetDir);
		String[] files = dir.list();
		fileList = new ArrayList<String>();
		
		for (String file : files)
		{
			fileList.add(file);
		}
		
		if (rootView == null)
			rootView = getView();
		
		int i = 0;
		int sz = listView.getChildCount();
		for (String file : fileList)
		{
			final TextInputWidget widget;
			
			if (i < sz)
			{
				widget = (TextInputWidget) listView.getChildAt(i);
				widget.setText(file);
			}
			else
			{
				widget = new TextInputWidget(getActivity());
				widget.setText(file);
				
				widget.setLabelClickedListener(new TextInputWidget.LabelClickedListener() {public void clicked() 
				{
					parent.openFile(widget.getText());
				}});

				widget.setDeleteClickedListener(new TextInputWidget.DeleteClickedListener() {public void clicked() 
				{
					deleteFile(widget.getText());
				}});

				listView.addView(widget);
			}
			
			i++;
		}

		int flsz = fileList.size();
		for (i = flsz; i < sz; i++)
		{
			listView.removeViewAt(flsz);
		}
		
		setEditable(((MainActivity) getActivity()).getEditable());
	}
		
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
