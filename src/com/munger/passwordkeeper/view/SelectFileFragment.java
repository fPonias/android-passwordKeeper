package com.munger.passwordkeeper.view;

import java.io.File;
import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.DropBoxManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxAccountManager;
import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.alert.ConfirmFragment;
import com.munger.passwordkeeper.struct.PasswordDocument;
import com.munger.passwordkeeper.struct.PasswordDocument.Type;
import com.munger.passwordkeeper.view.widget.FileItemWidget;

/**
 * This fragment allows the user to select a file from the saved files directory and have the activity load it up
 */
public class SelectFileFragment extends Fragment 
{
	private MainActivity parent;
	private View rootView;
	
	private ArrayList<PasswordDocument> fileList;
	private String targetDir;

	private View buttonsView;
	private Button newFileButton;
	private Button newDropboxButton;
	private ListView listView;
	private FileListAdapter fileListAdapter;
	
	public SelectFileFragment()
	{
		super();
	}
	
	public static String getName()
	{
		return "Select";
	}
	
	private static class FileListAdapter extends ArrayAdapter<PasswordDocument>
	{
		private SelectFileFragment parent;
		
		/**
		 * Constructor.
		 * @param parent The fragment this object belongs to for passing events on to.
		 * @param context The application context
		 * @param objects the list of object to covert to the list
		 */
		public FileListAdapter(SelectFileFragment parent, Context context, ArrayList<PasswordDocument> fileList) 
		{
			super(context, 0, fileList);
			
			this.parent = parent;
		}
		
		/**
		 * extention of the fileitem widget that adds fragment specific event handlers to the widget
		 * fileitemwidget display a file item in the directory with an icon and name.
		 */
		private static class MyFileItemWidget extends FileItemWidget
		{
			private PasswordDocument doc;

			public MyFileItemWidget(Context context) 
			{
				super(context);
			}
			
			public void setDocument(PasswordDocument d)
			{
				//set the label with the file name
				super.setLabel(d.name);

				Resources r = getContext().getResources();
								
				if (d.type == PasswordDocument.Type.FILE)
				{
					Drawable dr = r.getDrawable(R.drawable.ic_action_storage);
					setIcon(dr);
				}
				else
				{
					Drawable dr = r.getDrawable(R.drawable.ic_action_web_site);
					setIcon(dr);
				}
			}
		}
		
		/**
		 * Render the list item to a widget
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup par) 
		{
			final MyFileItemWidget ret;
			final FileListAdapter that = this;
			final PasswordDocument details = getItem(position);
			
			//if we're recycling the view
			if (convertView != null)
			{
				ret = (MyFileItemWidget) convertView;
				ret.setDocument(details);
			}
			//if it's a new view
			//setup event listeners as well
			else
			{
				ret = new MyFileItemWidget(this.parent.parent);
				ret.setDocument(details);
				ret.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
				{
					that.parent.parent.openFile(details.name);
				}});
			}
		
			return ret;
		}
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		this.parent = (MainActivity) getActivity();
	    super.onCreate(savedInstanceState);
	};
	
	/**
	 * get references to important components and update visual components
	 */
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		targetDir = parent.getFilesDir().getAbsolutePath() + "/saved";

		setHasOptionsMenu(true);
		
		File dir = new File(targetDir);
		
		if (!dir.exists())
			dir.mkdirs();
		
		
		rootView = inflater.inflate(R.layout.fragment_selectfile, container, false);
		
		listView = (ListView) rootView.findViewById(R.id.selectfile_filelist);
		fileList = new ArrayList<PasswordDocument>();
		fileListAdapter = new FileListAdapter(this, parent, fileList);
		listView.setAdapter(fileListAdapter);
		
		buttonsView = rootView.findViewById(R.id.selectfile_addFileBtns);
		newFileButton = (Button) rootView.findViewById(R.id.selectfile_addfile);
		newDropboxButton = (Button) rootView.findViewById(R.id.selectfile_dropbox);
		
		newFileButton.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
		{
			parent.addFile(CreateFileFragment.TYPE_CREATE);
		}});
		
		MainActivity.dropboxListener listener = new MainActivity.dropboxListener() 
		{
			public void connected() 
			{
				if (parent.hasDropbox())
				{
					newDropboxButton.setText("Dropbox Create");
				}
				else
				{
					newDropboxButton.setText("Dropbox Connect");
				}
			}
		};
		parent.addDropboxListener(listener);
		
		newDropboxButton.setOnClickListener(new View.OnClickListener() {public void onClick(View v)
		{
			if (!parent.hasDropbox())
				parent.startDropbox();
			else
			{
				//create dropbox file
			}
		}});
		
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
		this.editable = editable;
		
		if (rootView == null)
			return;
		
		buttonsView.setVisibility(editable ? View.VISIBLE : View.GONE);
	}
	
	@Override
	public void onResume() 
	{
		update();
		super.onResume();
	};
	
	public void update()
	{	
		if (rootView == null)
			return;
		
		//update the file list
		PasswordDocument d = new PasswordDocument(parent, "tmp", PasswordDocument.Type.NONE);
		ArrayList<PasswordDocument> files = PasswordDocument.getList(parent);
		fileListAdapter.clear();
		fileListAdapter.addAll(files);		
		fileListAdapter.notifyDataSetChanged();
				
		setEditable(((MainActivity) getActivity()).getEditable());
	}
		
	/**
	 * Delete the file permanently if confirmed.
	 * Bring up a prompt to ask the user if they really want to delete the selected file.
	 * @param name the name of the file to be deleted.
	 */
	private void deleteFile(final PasswordDocument doc)
	{
		final SelectFileFragment that = this;
		ConfirmFragment frag = new ConfirmFragment("Are you sure you want to delete " + doc.name + "?", new ConfirmFragment.Listener() 
		{
			public void okay() 
			{
				parent.removeFile(doc.name);
				that.onResume();
			}
			
			public void cancel() 
			{}
		});
		frag.show(parent.getSupportFragmentManager(), "confirm_fragment");
	}
}
