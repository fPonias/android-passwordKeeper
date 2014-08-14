package com.munger.passwordkeeper;

import java.io.File;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.munger.passwordkeeper.alert.AlertFragment;
import com.munger.passwordkeeper.widget.FileItemWidget;

public class ImportFileFragment extends Fragment 
{
	private RelativeLayout root;
	private MainActivity parent;
	private ImageButton upButton;
	private ImageButton reloadButton;
	private EditText pathInput;
	private ListView directoryList;
	
	private ArrayAdapter<String> directoryListAdapter;
	private String currentDir = null;
	private String nextDir = null;
	
	
	public ImportFileFragment()
	{
		
	}
	
	public static String getName()
	{
		return "Import";
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		this.parent = (MainActivity) getActivity();
	
		setHasOptionsMenu(true);
		
		root = (RelativeLayout) inflater.inflate(R.layout.fragment_importfile, container, false);
		upButton = (ImageButton) root.findViewById(R.id.import_uplevelbtn);
		reloadButton = (ImageButton) root.findViewById(R.id.import_refreshbtn);
		pathInput = (EditText) root.findViewById(R.id.import_pathinput);
		directoryList = (ListView) root.findViewById(R.id.import_filelist);
		
		if (currentDir != null)
		{
			populateViews();
		}
		
		upButton.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
		{
			navigateUp();
		}});
		
		reloadButton.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
		{
			populateViews();
		}});
		
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
	
	@Override
	public void onCreateOptionsMenu(android.view.Menu menu, android.view.MenuInflater inflater) 
	{
		int sz = menu.size();
		
		for (int i = 0; i < sz; i++)
		{
			MenuItem item = menu.getItem(0);
			menu.removeItem(item.getItemId());
		}
	};
	
	public void setDirectory(String directory)
	{
		nextDir = directory;
		currentDir = nextDir;
		
		if (parent != null)
			populateViews();
	}
	
	private void populateViews()
	{
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
		
		String[] files = f.list();
		
		directoryListAdapter = new DetailArrayAdapter(this, this.parent, files);
		directoryList.setAdapter(directoryListAdapter);
		
		currentDir = nextDir;
		pathInput.setText(currentDir);
	}
	
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
			super.setLabel(name);
			
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
	
	private static class DetailArrayAdapter extends ArrayAdapter<String>
	{
		private ImportFileFragment host;
		
		public DetailArrayAdapter(ImportFileFragment host, Context context, String[] objects) 
		{
			super(context, R.layout.widget_fileitem, objects);
			
			this.host = host;
		}
		
		@Override
		public View getView(int position, View convertView, final ViewGroup par) 
		{	
			final String item = getItem(position);
			final MyFileItemWidget ret;
			
			if (convertView == null)
			{
				ret = new MyFileItemWidget(host.currentDir, item, getContext());
				
				ret.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
				{
					File f = new File(ret.path + "/" + ret.name);
					
					if (f.isDirectory())
					{
						host.nextDir = f.getAbsolutePath();
						host.populateViews();
					}
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
	
	private void navigateUp()
	{
		String[] parts = currentDir.split("/");
		
		int sz = parts.length;
		int i = sz - 1;
		
		while (i > 0 && parts[i].isEmpty())
			i--;
		
		i--;
		
		if (i < 0)
			nextDir = "/";
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
		
		populateViews();
	}
}
