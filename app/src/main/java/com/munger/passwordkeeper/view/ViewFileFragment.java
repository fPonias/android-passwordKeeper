package com.munger.passwordkeeper.view;


import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.alert.ConfirmFragment;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDocument;
import com.munger.passwordkeeper.view.widget.TextWidget;

public class ViewFileFragment extends Fragment 
{
	private View root;
	
	private TextView title;
	private Button addButton;
	private ListView detailList;

	private DetailListAdapter detailListAdapter;
	
	private PasswordDocument document = null;
	private String file = "";
	
	private boolean editable = false;
	
	public static String getName()
	{
		return "File";
	}

	private static class DetailListAdapter extends ArrayAdapter<PasswordDetails>
	{
		private ViewFileFragment parent;

		public DetailListAdapter(ViewFileFragment parent, Context context, List<PasswordDetails> objects) 
		{
			super(context, 0, objects);
			
			this.parent = parent;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup par) 
		{
			final DetailView ret;
			final DetailListAdapter that = this;
			PasswordDetails details = getItem(position);

			if (convertView != null)
			{
				ret = (DetailView) convertView;
				ret.setDetails(details);
			}
			else
			{
				ret = new DetailView(details, getContext());
				ret.setListener(new TextWidget.Listener() 
				{
					public void labelClicked() 
					{
						that.parent.openDetail(ret.getDetails());
					}
					
					public void deleteClicked() 
					{
						that.parent.deleteClicked(ret.getDetails());
					}
				});
			}
			
			ret.setEditable(parent.editable);
			
			return ret;
		}
	}

	private static class DetailView extends TextWidget
	{
		private PasswordDetails details;
		
		public DetailView(PasswordDetails details, Context context) 
		{
			super(context, null);

			setDetails(details);
		}
		
		public void setDetails(PasswordDetails dets)
		{
			details = dets;
			
			if (details != null)
				setText(dets.getName());
		}
		
		public PasswordDetails getDetails()
		{
			return details;
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) 
	{
		outState.putString("password", MainActivity.getInstance().password);
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		setDocument(MainActivity.getInstance().document);
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final ViewFileFragment that = this;
		
		setHasOptionsMenu(true);
		
		root = inflater.inflate(R.layout.fragment_viewfile, container, false);

		title = (TextView) root.findViewById(R.id.viewfile_name);
		addButton = (Button) root.findViewById(R.id.viewfile_addbtn);
		detailList = (ListView) root.findViewById(R.id.viewfile_detaillist);

		if (document != null)
			setupDetailListAdapter();
		

		addButton.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
		{
			if (document != null)
			{
				PasswordDetails details = document.addEmptyEntry();
				//that.detailListAdapter.add(details);
				
				try
				{
					document.save();
				}
				catch(Exception e){
					Log.e("passwordkeeper", "failed to save file " + file);
				}
				
				
				that.detailListAdapter.notifyDataSetChanged();
			}
		}});
		
		return root;
	}

	private ArrayList<PasswordDetails> searchDetails(PasswordDocument details, String search)
	{
		search = search.toLowerCase();
		ArrayList<PasswordDetails> ret = new ArrayList<PasswordDetails>();

		int sz = details.count();
		for (int i = 0; i < sz; i++)
		{
			PasswordDetails dets = details.getDetails(i);
			if (dets.getName().toLowerCase().contains(search))
				ret.add(dets);
		}
		
		return ret;
	}

	private ArrayList<PasswordDetails> filtered = new ArrayList<PasswordDetails>();
	private boolean useFiltered = false;
	private DetailListAdapter filterAdapter;


	@Override
	public void onCreateOptionsMenu(android.view.Menu menu, android.view.MenuInflater inflater) 
	{
		inflater.inflate(R.menu.main, menu);
		
		MenuItem searchItem = menu.findItem(R.id.action_search);
	    SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
	    
	    
	    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() 
	    {
	    	public boolean onQueryTextSubmit(String arg0) 
	    	{
				return false;
			}

			public boolean onQueryTextChange(String arg0) 
			{
				if (arg0.isEmpty())
				{
					if (useFiltered == true)
					{
						useFiltered = false;
						detailList.setAdapter(detailListAdapter);
					}
				}
				else
				{
					if (document != null)
					{
						filtered = searchDetails(document, arg0);

						filterAdapter.clear();
						filterAdapter.addAll(filtered);
						
						if (useFiltered == false)
						{
							useFiltered = true;
							detailList.setAdapter(filterAdapter);
						}

						filterAdapter.notifyDataSetChanged();
					}
				}
				
				return false;
			}
		});
	};

	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		int id = item.getItemId();
		
		if (id == R.id.action_edit)
		{
			setEditable(!editable);
		}
		
		return true;
	};
	
	public void setEditable(boolean editable)
	{
		this.editable = editable;
		
		if (root == null)
			return;
		
		setupEditable();
	}

	public void setDocument(PasswordDocument document)
	{
		this.document = document;
		this.file = document.name;
		
		if (root != null)
		{
			setupDetailListAdapter();
		}
	}

	private void setupDetailListAdapter()
	{
		detailListAdapter = new DetailListAdapter(this, getActivity(), this.document.getDetailsList());
		filterAdapter = new DetailListAdapter(this, getActivity(), this.filtered);
		
		detailList.setAdapter(detailListAdapter);
		
		title.setText(file);
	}

	@Override
	public void onResume() 
	{
		super.onResume();
		
		if (document == null)
			return;
		
		title.setText(file);
		
		setupEditable();
		root.invalidate();
	}

	private void setupEditable()
	{
		int sz = detailList.getChildCount();
		
		for (int i = 0; i < sz; i++)
		{
			TextWidget w = (TextWidget) detailList.getChildAt(i);
			w.setEditable(editable);
		}
		
		addButton.setVisibility((editable) ? View.VISIBLE : View.GONE);
	}

	private void openDetail(final PasswordDetails dets)
	{
		MainActivity.getInstance().openDetail(dets);
	}

	private void deleteClicked(final PasswordDetails dets)
	{
		final ViewFileFragment that = this;
		ConfirmFragment frag = new ConfirmFragment("Delete \"" + dets.getName() + "\"?", new ConfirmFragment.Listener()
		{
			public void okay() 
			{
				document.removeDetails(dets);
				detailListAdapter.remove(dets);
				detailListAdapter.notifyDataSetChanged();

				try
				{
					document.save();
				}
				catch(Exception e){
					Log.e("passwordkeeper", "failed to save file " + file);
				}
				
				if (useFiltered)
				{
					filterAdapter.remove(dets);
					filterAdapter.notifyDataSetChanged();
				}
				
				that.onResume();
			}
			
			public void cancel() 
			{	
			}

			public void discard()
			{}
		});
		frag.show(MainActivity.getInstance().getSupportFragmentManager(), "confirm_fragment");
	}
}
