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

/**
 * Fragment used to view all the defined entries in a passwordDocument.
 *
 */
public class ViewFileFragment extends Fragment 
{
	private MainActivity parent;
	private View root;
	
	/** the title of this password document */ private TextView title;
	/** a button used to add new entries to this document */ private Button addButton;
	/** a list displaying all the current entries in the document */ private ListView detailList;
	
	/** 
	 * an adapter that converts the list of document entries to view widgets.
	 * PassowrdDetails -> DetailView
	*/
	private DetailListAdapter detailListAdapter;
	
	/** the currently opened password document */ private PasswordDocument document = null;
	/** the file where this document is stored */ private String file = "";
	
	private boolean editable = false;
	
	public static String getName()
	{
		return "File";
	}
	
	/**
	 * Class the populates the specified ListView with a collections of DetailsViews.
	 * Each DetailWidget should match an entry in the list of PasswordDetails belonging
	 * to the currently opened document.
	 */
	private static class DetailListAdapter extends ArrayAdapter<PasswordDetails>
	{
		private ViewFileFragment parent;
		
		/**
		 * Constructor.
		 * @param parent The fragment this object belongs to for passing events on to.
		 * @param context The application context
		 * @param objects the list of object to covert to the list
		 */
		public DetailListAdapter(ViewFileFragment parent, Context context, List<PasswordDetails> objects) 
		{
			super(context, 0, objects);
			
			this.parent = parent;
		}
		
		/**
		 * Render the list item to a widget
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup par) 
		{
			final DetailView ret;
			final DetailListAdapter that = this;
			PasswordDetails details = getItem(position);
			
			//if we're recycling the view
			if (convertView != null)
			{
				ret = (DetailView) convertView;
				ret.setDetails(details);
			}
			//if it's a new view
			//setup event listeners as well
			else
			{
				ret = new DetailView(details, getContext());
				ret.setListener(new TextWidget.Listener() 
				{
					public void labelClicked() 
					{
						//open a ViewDetailFragment
						that.parent.openDetail(ret.getDetails());
					}
					
					public void deleteClicked() 
					{
						//delete the selected detail
						that.parent.deleteClicked(ret.getDetails());
					}
				});
			}
			
			ret.setEditable(parent.editable);
			
			return ret;
		}
	}
	
	/**
	 * Modifies the existing TextWidget class to render its assigned details
	 */
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
				setText(dets.name);
		}
		
		public PasswordDetails getDetails()
		{
			return details;
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) 
	{
		outState.putString("file", parent.currentDoc);
		outState.putString("password", parent.password);
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		parent = (MainActivity) getActivity();
		
		if (savedInstanceState != null)
		{
			String file = savedInstanceState.getString("file");
			String password = savedInstanceState.getString("password");
			
			parent.setFile(file, password);
			parent.fragmentExists(this);
		}
		
		setDocument(parent.document);
	};
	
	/**
	 * Get references to relevant components and setup events for most components
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final ViewFileFragment that = this;
		this.parent = (MainActivity) getActivity();
		
		setHasOptionsMenu(true);
		
		root = inflater.inflate(R.layout.fragment_viewfile, container, false);
		
		//get component references
		title = (TextView) root.findViewById(R.id.viewfile_name);
		addButton = (Button) root.findViewById(R.id.viewfile_addbtn);
		detailList = (ListView) root.findViewById(R.id.viewfile_detaillist);
		
		//set up the detailList renderer
		if (document != null)
			setupDetailListAdapter();
		
		
		//setup the create new details button
		addButton.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
		{
			if (document != null)
			{
				PasswordDetails details = new PasswordDetails();
				details.name = "new details";
				that.detailListAdapter.add(details);
				
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
	
	@Override
	public void onPause() 
	{
		parent.reset();
		super.onPause();
	};
	
	/**
	 * Filter the list of detials down to matching details.
	 * Detail names are the only thing search.  Matches detail names that contain the search string anywhere in the string.
	 * @param details the list of details to filter
	 * @param search the search string to filter on.
	 * @return the filtered list of details
	 */
	private ArrayList<PasswordDetails> searchDetails(ArrayList<PasswordDetails> details, String search)
	{
		search = search.toLowerCase();
		ArrayList<PasswordDetails> ret = new ArrayList<PasswordDetails>();
		
		for (PasswordDetails dets : document.details)
		{
			if (dets.name.toLowerCase().contains(search))
				ret.add(dets);
		}
		
		return ret;
	}
	
	/** the filtered list of details */ private ArrayList<PasswordDetails> filtered = new ArrayList<PasswordDetails>();
	/** render with the filtered list or full list? */ private boolean useFiltered = false;
	/** The filter list widget generator */ private DetailListAdapter filterAdapter;
	
	/**
	 * setup the options menu.
	 * Add events to each option
	 */
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
			
	    	/**
	    	 * filter a new list for the filter view on each keypress.
	    	 * return to the default list if the search input is empty.
	    	 */
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
						filtered = searchDetails(document.details, arg0);

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
	
	/**
	 * events for the rest of the options menu items.
	 * setup editable mode.
	 */
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
	
	/**
	 * Set the default document this fragment is rendering
	 * @param file
	 * @param document
	 */
	public void setDocument(PasswordDocument document)
	{
		this.document = document;
		this.file = document.name;
		
		if (root != null)
		{
			setupDetailListAdapter();
		}
	}
	
	/**
	 * setup of the list to widget generators
	 */
	private void setupDetailListAdapter()
	{
		detailListAdapter = new DetailListAdapter(this, getActivity(), this.document.details);
		filterAdapter = new DetailListAdapter(this, getActivity(), this.filtered);
		
		detailList.setAdapter(detailListAdapter);
		
		title.setText(file);
	}
	
	/**
	 * make sure the view is updated
	 */
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

	/**
	 * set each component to its editable mode.
	 */
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
	
	/**
	 * Open the specified details in a new ViewDetailFragment.
	 * @param dets
	 */
	private void openDetail(final PasswordDetails dets)
	{
		parent.openDetail(dets);
	}
	
	/**
	 * Delete the specified details from the passwordDocument.
	 * A confirmation dialog is brought up because this action is permanent.
	 * @param dets
	 */
	private void deleteClicked(final PasswordDetails dets)
	{
		final ViewFileFragment that = this;
		ConfirmFragment frag = new ConfirmFragment("Delete \"" + dets.name + "\"?", new ConfirmFragment.Listener() 
		{
			public void okay() 
			{
				//update all of the widget generator's backing lists
				document.details.remove(dets);
				detailListAdapter.remove(dets);
				detailListAdapter.notifyDataSetChanged();
				
				//save the changes permanently
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
		});
		frag.show(parent.getSupportFragmentManager(), "confirm_fragment");
	}
}
