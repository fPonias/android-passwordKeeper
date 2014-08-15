package com.munger.passwordkeeper.view;

import java.util.ArrayList;
import java.util.List;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
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
import com.munger.passwordkeeper.view.widget.DetailItemWidget;
import com.munger.passwordkeeper.view.widget.TextInputWidget;

public class ViewDetailFragment extends Fragment 
{
	private MainActivity parent;
	private View root = null;
	
	/** label to display the detail name */ private TextInputWidget nameLabel;
	/** label to display the detail URL/location */ private TextInputWidget locationLabel;
	/** list to display pair widgets */ private ListView itemList;
	/** button to add a new pair in edit mode */ private Button addButton;
	
	/** the current details we're working on */ private PasswordDetails details = null;
	/** the original details in case we want to revert changes */ private PasswordDetails originalDetails = null;
	
	public static String getName()
	{
		return "Detail";
	}

	/** listener opens the copy submenu on a long click */ private View.OnLongClickListener copyMenuListener;
	
	@Override
	public void onSaveInstanceState(Bundle outState) 
	{
		outState.putString("file", parent.currentDoc);
		outState.putString("password", parent.password);
		outState.putInt("index", details.index);
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
			int index = savedInstanceState.getInt("index");
			
			parent.setFile(file, password);
			parent.setDetails(index);
			parent.fragmentExists(this);
		}
		
		setDetails(parent.getDetails());
	};
	
	/**
	 * Gather references to commonly used components and setup event handlers.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{	
		root = inflater.inflate(R.layout.fragment_viewdetail, container, false);
	
		setHasOptionsMenu(true);
		
		//get common components for later reference
		nameLabel = (TextInputWidget) root.findViewById(R.id.viewdetail_namelbl);
		locationLabel = (TextInputWidget) root.findViewById(R.id.viewdetail_locationlbl);
		itemList = (ListView) root.findViewById(R.id.viewdetail_itemlist);
		addButton = (Button) root.findViewById(R.id.viewdetail_addbtn);
		
		nameLabel.getLabel().setOnLongClickListener(copyMenuListener);
		locationLabel.getLabel().setOnLongClickListener(copyMenuListener);

		if (details != null)
			setupFields();
		
		//copy any field that gets a long click
		copyMenuListener = new View.OnLongClickListener() {public boolean onLongClick(View v) 
		{
			if (actionMode != null)
				return false;
			
			if (!(v instanceof TextView))
				return false;
			
			actionSelected = (TextView) v;
			selectText(actionSelected, true);
			actionMode = parent.startActionMode(actionCallback);
			return true;
		}};
		
		//update the changeable copy of the details whenever a change to any text input is done
		nameLabel.setInputChangeListener(new TextInputWidget.InputChangedListener() {public void changed() 
		{
			if (details != null)
				details.name = nameLabel.getText();
		}});
		
		locationLabel.setInputChangeListener(new TextInputWidget.InputChangedListener() {public void changed() 
		{
			if (details != null)
				details.location = locationLabel.getText();
		}});
		
		
		//add a pair to the current details if the add button is clicked
		addButton.setOnClickListener(new View.OnClickListener() {public void onClick(View arg0) 
		{
			if (details == null)
				return;
			
			addPair();
		}});
		
		
		setupEditable();
		
		return root;
	}

	/** currently displaying the filtered list of pairs */ private boolean useFiltered = false;
	/** list generator for the unfiltered pairs list */ private DetailArrayAdapter pairListAdapter = null;
	/** list generator for the filtered pairs list */ private DetailArrayAdapter filterAdapter = null;
	/** filtered list filled in on a search */ private ArrayList<PasswordDetails.Pair> filtered = new ArrayList<PasswordDetails.Pair>();
	
	/**
	 * Filter the pairs list when a search is run.
	 * Pairs will be filtered based on weather the key or value contains the provided string
	 * @param orig The complete list of pairs belongs to this password detail
	 * @param search The string to search on.
	 * @return the filtered list of pairs
	 */
	public ArrayList<PasswordDetails.Pair> searchDetails(ArrayList<PasswordDetails.Pair> orig, String search)
	{
		ArrayList<PasswordDetails.Pair> ret = new ArrayList<PasswordDetails.Pair>();
		
		search = search.toLowerCase();
		
		for (PasswordDetails.Pair pair : orig)
		{
			if (pair.key.toLowerCase().contains(search) || pair.value.toLowerCase().contains(search))
			{
				ret.add(pair);
			}
		}
		
		return ret;
	}
	
	/**
	 * Setup handlers for the edit mode and searching
	 */
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) 
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
			
	    	//switch to the filtered view when the serach bar has text in it
			public boolean onQueryTextChange(String arg0) 
			{
				if (arg0.isEmpty())
				{
					if (useFiltered == true)
					{
						useFiltered = false;
						itemList.setAdapter(pairListAdapter);
					}
				}
				else
				{
					if (details != null)
					{
						filtered = searchDetails(details.details, arg0);

						filterAdapter.clear();
						filterAdapter.addAll(filtered);
						
						if (useFiltered == false)
						{
							useFiltered = true;
							itemList.setAdapter(filterAdapter);
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
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	};
	
	/**
	 * Select the textView by changing the background
	 * An interesting feature of android is changing the background on a textview will cause the padding to get reset.
	 * @param v the view to be selected
	 * @param selected are we selecting or deselecting the view?
	 */
	private void selectText(TextView v, boolean selected)
	{
		int left = v.getPaddingLeft();
		int right = v.getPaddingRight();
		int top = v.getPaddingTop();
		int bott = v.getPaddingBottom();
		
		int resid = 0;
		if (selected)
			resid = R.drawable.abc_list_selector_background_transition_holo_dark;
		
		v.setBackgroundResource(resid);
		v.setPadding(left, top, right, bott);
	}
	
	/**
	 * Delete the specified pair from the current pairs.
	 * @param p the pair to be deleted
	 */
	private void deletePair(PasswordDetails.Pair p)
	{
		details.details.remove(p);
		pairListAdapter.notifyDataSetChanged();
		
		if (useFiltered)
		{
			filtered.remove(p);
			filterAdapter.remove(p);
			filterAdapter.notifyDataSetChanged();
		}
	}
	
	/**
	 * Add a new blank pair to the current pairs list.
	 */
	private void addPair()
	{
		PasswordDetails.Pair pair = new PasswordDetails.Pair();
		details.details.add(pair);
		pairListAdapter.notifyDataSetChanged();
	}
	
	/**
	 * Set the details this fragments renders and update the view
	 * @param dets the details to set on this fragment.
	 */
	public void setDetails(PasswordDetails dets)
	{
		originalDetails = dets;
		details = dets.copy();

		if (root != null)
		{
			setupFields();
		}
	}
	
	/**
	 * populate all text labels with the current details information
	 */
	private void setupFields()
	{
		pairListAdapter = new DetailArrayAdapter(this, parent, details.details);
		filterAdapter = new DetailArrayAdapter(this, parent, filtered);
		itemList.setAdapter(pairListAdapter);

		nameLabel.setText(details.name);
		locationLabel.setText(details.location);
	}
	
	/** is this fragment currently in editing mode? */ private boolean editable = false;
	
	/** 
	 * change the editing mode of this fragment.
	 * switches all the contained views to editing/view mode.
	 * @param editable
	 */
	public void setEditable(boolean editable)
	{
		this.editable = editable;
		setupEditable();	

		if (isVisible())
		{
			if (!editable && originalDetails != null && originalDetails.diff(details))
			{
				saveDetails();
			}
		}
	}
	
	/**
	 * handle exiting from this view.
	 * @return true if we want the back action to proceed.
	 */
	public boolean backPressed()
	{
		if (editable && originalDetails != null && originalDetails.diff(details))
		{
			//if there are changes that need to be saved, a confirmation popup is brought up and the back action is cancelled.
			goingBack = true;
			saveDetails();
			return false;
		}
		
		return true;
	}
	
	private boolean goingBack = false;
	
	/**
	 * bring up a popup that asks the user if they want to save changes to the details.
	 */
	private void saveDetails()
	{
		ConfirmFragment frag = new ConfirmFragment("Save changes?", new ConfirmFragment.Listener() 
		{
			public void okay() 
			{
				//save the details and initiate a new back action, 
				//this will succeed because the two copies of the details will be identical.
				parent.saveDetail(details);
				originalDetails = details;
				
				if (!goingBack)
					onResume();
				else
					parent.onBackPressed();
			}
			
			public void cancel() 
			{
				setDetails(originalDetails);
			}
		});
		frag.show(parent.getSupportFragmentManager(), "confirm_fragment");
	}
	
	/**
	 * A DetailItemWidget with data and event listeners added.
	 */
	private static class PairDetailItemWidget extends DetailItemWidget
	{
		private PasswordDetails.Pair pair;
		private ViewDetailFragment parent;
		
		public PairDetailItemWidget(PasswordDetails.Pair p, ViewDetailFragment par, Context context) 
		{
			super(context);
			parent = par;
			
			//set the data for this widget
			setPair(p);
			
			
			//bring up the copy sub menu on long click
			keyLabel.setOnLongClickListener(parent.copyMenuListener);
			valueLabel.setOnLongClickListener(parent.copyMenuListener);
			
			//set up change listeners to keep the backing data up to date
			keyInput.addTextChangedListener(new TextWatcher() 
			{
				public void onTextChanged(CharSequence s, int start, int before, int count) 
				{}
				
				public void beforeTextChanged(CharSequence s, int start, int count,	int after) 
				{}
				
				public void afterTextChanged(Editable s) 
				{
					pair.key = keyInput.getText().toString();
				}
			});
			
			valueInput.addTextChangedListener(new TextWatcher() 
			{
				public void onTextChanged(CharSequence s, int start, int before, int count) 
				{}
				
				public void beforeTextChanged(CharSequence s, int start, int count,	int after) 
				{}
				
				public void afterTextChanged(Editable s) 
				{
					pair.value = valueInput.getText().toString();
				}
			});
			
			//bring up the copy/edit sub menu on a long click
			valueInput.setOnLongClickListener(parent.copyMenuListener);
			
			//delete this pair on delete click
			deleteBtn.setOnClickListener(new OnClickListener() {public void onClick(View arg0) 
			{
				parent.deletePair(pair);
			}});
		}
		
		/**
		 * update the UI when the underlying pair is updated
		 * @param pair
		 */
		public void setPair(PasswordDetails.Pair pair)
		{
			this.pair = pair;
			
			if (pair != null)
			{
				setKey(pair.key);
				setValue(pair.value);
			}
			else
			{
				setKey("");
				setValue("");
			}
		}
		
		@SuppressWarnings("unused")
		public PasswordDetails.Pair getPair()
		{
			return pair;
		}
	}
	
	/**
	 * Generator for widgets when a list view is assigned a list of password details pairs.
	 * Should generate a PairDetailItemWidget for each PasswordDetails.Pair in the list.
	 */
	private static class DetailArrayAdapter extends ArrayAdapter<PasswordDetails.Pair>
	{
		private ViewDetailFragment host;
		
		public DetailArrayAdapter(ViewDetailFragment host, Context context, List<PasswordDetails.Pair> objects) 
		{
			super(context, 0, objects);
			
			this.host = host;
		}
		
		/**
		 * create a new widget and update inputs and events
		 */
		@Override
		public View getView(int position, View convertView, final ViewGroup par) 
		{	
			final PasswordDetails.Pair pair = getItem(position);
			
			PairDetailItemWidget ret;
			
			//just update the views if it's a recycled item
			if (convertView == null)
			{
				ret = new PairDetailItemWidget(pair, host, getContext());
			}
			else
			{
				ret = (PairDetailItemWidget) convertView;
				ret.setPair(pair);
			}
			
			ret.setEditable(host.editable);
			
			return ret;
		}
	}
	
	/** Context menu created on all inputs */ private ActionMode actionMode = null;
	/** The current input selected for a context menu */ private TextView actionSelected = null;
	
	/**
	 * setup the context menu that will be assigned to all editiable input fields.
	 * This menu will allow copy, paste, and random password generation
	 */
	private ActionMode.Callback actionCallback = new ActionMode.Callback() 
	{
		public boolean onPrepareActionMode(ActionMode arg0, Menu arg1) 
		{
			return false;
		}
		
		/**
		 * Deselect inputs when they are no longer selected
		 */
		public void onDestroyActionMode(ActionMode arg0) 
		{
			actionMode = null;
			
			selectText(actionSelected, false);
			
			actionSelected = null;
		}
		
		/**
		 * Create the context menu based on the current state of the input.
		 * uneditable are just copyable and editable are copy/paste/random
		 */
		public boolean onCreateActionMode(ActionMode mode, Menu menu) 
		{
			MenuInflater inf = mode.getMenuInflater();
			
			if (!editable)
				inf.inflate(R.menu.detail_action, menu);
			else
				inf.inflate(R.menu.detailedit_action, menu);
				
			return true;
		}
		
		/**
		 * perform the selected action on the selected input
		 */
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) 
		{
			int id = item.getItemId();
			
			ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
			
			if (id == R.id.action_detail_copy)
			{ 
				//copy the text to the clipboard
				ClipData clip = ClipData.newPlainText("password-keeper", actionSelected.getText().toString());
				clipboard.setPrimaryClip(clip);
			}
			else if (id == R.id.action_detail_paste)
			{
				//paste the clipboard text to the input
				ClipData clip = clipboard.getPrimaryClip();
				int sz = clip.getItemCount();
				
				if (sz > 0)
				{
					ClipData.Item it = clip.getItemAt(0);
					String data = it.coerceToText(parent).toString();
					actionSelected.setText(data);
				}
			}
			else if (id == R.id.action_detail_random)
			{
				//generate a password and copy the resulting password to the clipboard
				String pw = generateRandomPassword(8);
				actionSelected.setText(pw);
				ClipData clip = ClipData.newPlainText("password-keeper", pw);
				clipboard.setPrimaryClip(clip);
			}
			
			return false;
		}
	};
	
	/**
	 * Generate a password of length size.
	 * The password is guaranteed to have at least one lower case, upper case, and numeric letter in it.
	 * @param length the length in characters of the generated password
	 * @return the generated password
	 */
	public String generateRandomPassword(int length)
	{
		StringBuilder ret;
		int capCount;
		int lowerCount;
		int numCount;
		
		//loop until each type of character has been generated at least once
		do
		{
			capCount = 0;
			lowerCount = 0;
			numCount = 0;
			ret = new StringBuilder();
			
			for (int i = 0; i < length; i++)
			{
				int type = (int) (Math.random() * (double) 3);
				
				//add a lower case letter
				if (type == 0)
				{
					int n = (int) (Math.random() * (double) 26);
					ret.append((char) ('a' + n));
					lowerCount++;
				}
				//add an upper case letter
				else if (type == 1)
				{
					int n = (int) (Math.random() * (double) 26);
					ret.append((char) ('A' + n));
					capCount++;
				}
				//add a number
				else
				{
					int n = (int) (Math.random() * (double) 10);
					ret.append((char) ('0' + n));
					numCount++;
				}
			}
		} while(capCount == 0 || lowerCount == 0 || numCount == 0);
		
		return ret.toString();
	}

	/**
	 * swap all fragment components to the proper editing mode
	 */
	private void setupEditable()
	{
		if (root == null)
			return;
		
		nameLabel.setEditable(editable);
		
		//remove/replace default data if it exists
		if (editable && nameLabel.getText().equals("new entry"))
		{
			nameLabel.setText("");
			nameLabel.requestFocus();
		}
		else if (!editable && nameLabel.getText().isEmpty())
			nameLabel.setText("new entry");
		
		//set all components in the fragment to editable mode
		locationLabel.setEditable(editable);
		
		addButton.setVisibility(editable ? View.VISIBLE : View.GONE);
		
		int sz = details.details.size();
		
		if (sz == 0)
		{
			pairListAdapter.add(new PasswordDetails.Pair());
			pairListAdapter.notifyDataSetChanged();
		}
		
		
		sz = itemList.getCount();
		for (int i = 0; i < sz; i++)
		{
			DetailItemWidget item = (DetailItemWidget) itemList.getChildAt(i);
			
			if (item != null)
				item.setEditable(editable);
		}
		
		root.invalidate();
	}
}
