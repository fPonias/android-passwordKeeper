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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.util.Predicate;
import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.alert.ConfirmFragment;
import com.munger.passwordkeeper.helpers.KeyboardListener;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDocumentFile;
import com.munger.passwordkeeper.view.widget.DetailItemWidget;
import com.munger.passwordkeeper.view.widget.TextInputWidget;

public class ViewDetailFragment extends Fragment 
{
	private View root = null;
	
	private TextInputWidget nameLabel;
	private TextInputWidget locationLabel;
	private ListView itemList;
	private Button addButton;
	
	private PasswordDetails details = null;
	private PasswordDetails originalDetails = null;
	
	public static String getName()
	{
		return "Detail";
	}

	private View.OnLongClickListener copyMenuListener;
	
	@Override
	public void onSaveInstanceState(Bundle outState) 
	{
		if (details != null && MainActivity.getInstance().document instanceof PasswordDocumentFile)
		{
			outState.putString("file", MainActivity.getInstance().document.name);
		}
		
		outState.putString("password", MainActivity.getInstance().password);
		outState.putString("index", details.index);
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState != null)
		{
			String password = savedInstanceState.getString("password");
			String index = savedInstanceState.getString("index");
			
			MainActivity.getInstance().setFile(password);
			MainActivity.getInstance().setDetails(index);
			MainActivity.getInstance().fragmentExists(this);
			
			setDetails(MainActivity.getInstance().getDetails());
		}
		else
		{
			MainActivity.getInstance().keyboardListener.addKeyboardChangedListener(new KeyboardListener.OnKeyboardChangedListener()
			{
				public void OnKeyboardOpened()
				{
					keyboardOpened = true;
					Log.d("password", "keyboard open detected");
				}

				public void OnKeyboardClosed()
				{
					keyboardOpened = false;
					lastFocus = null;
					lastFocusStamp = 0;
					Log.d("password", "keyboard closed detected");
				}
			});
		}
	};

	protected View lastFocus;
	protected long lastFocusStamp = 0;
	protected boolean keyboardOpened = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{	
		root = inflater.inflate(R.layout.fragment_viewdetail, container, false);

		root.addOnLayoutChangeListener(new View.OnLayoutChangeListener()
		{
			public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom)
			{
				View newFocus = MainActivity.getInstance().getCurrentFocus();
				long newFocusStamp = System.currentTimeMillis();
				long diff = newFocusStamp - lastFocusStamp;
				Log.d("password", "last focus diff " + diff);

				if (newFocus != lastFocus)
				{
					if (lastFocus != null && diff < 150 && keyboardOpened)
					{
						Log.d("password", "refocusing last focus");
						lastFocus.requestFocus();
					}
					else if (diff > 150)
					{
						Log.d("password", "setting last focus");
						lastFocus = newFocus;
						lastFocusStamp = newFocusStamp;
					}
				}
			}
		});


		setHasOptionsMenu(true);

		nameLabel = (TextInputWidget) root.findViewById(R.id.viewdetail_namelbl);
		locationLabel = (TextInputWidget) root.findViewById(R.id.viewdetail_locationlbl);
		itemList = (ListView) root.findViewById(R.id.viewdetail_itemlist);
		addButton = (Button) root.findViewById(R.id.viewdetail_addbtn);
		
		nameLabel.getLabel().setOnLongClickListener(copyMenuListener);
		locationLabel.getLabel().setOnLongClickListener(copyMenuListener);

		if (details != null)
			setupFields();

		itemList.setFocusable(false);

		copyMenuListener = new View.OnLongClickListener() {public boolean onLongClick(View v) 
		{
			if (actionMode != null)
				return false;
			
			if (!(v instanceof TextView))
				return false;
			
			actionSelected = (TextView) v;
			selectText(actionSelected, true);
			actionMode = MainActivity.getInstance().startActionMode(actionCallback);
			return true;
		}};

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
		

		addButton.setOnClickListener(new View.OnClickListener() {public void onClick(View arg0) 
		{
			if (details == null)
				return;
			
			addPair();
		}});
		
		
		setupEditable();
		
		return root;
	}

	private boolean useFiltered = false;
	private DetailArrayAdapter pairListAdapter = null;
	private DetailArrayAdapter filterAdapter = null;
	private ArrayList<PasswordDetails.Pair> filtered = new ArrayList<PasswordDetails.Pair>();

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

	private int nextSelect = -1;

	private void addPair()
	{
		PasswordDetails.Pair pair = new PasswordDetails.Pair();
		details.details.add(pair);
		pairListAdapter.notifyDataSetChanged();

		nextSelect = details.details.size() - 1;
		itemList.setSelection(nextSelect);
	}

	public void setDetails(PasswordDetails dets)
	{
		originalDetails = dets;
		details = dets.copy();

		if (root != null)
		{
			setupFields();
		}
	}

	private void setupFields()
	{
		pairListAdapter = new DetailArrayAdapter(this, MainActivity.getInstance(), details.details);
		filterAdapter = new DetailArrayAdapter(this, MainActivity.getInstance(), filtered);
		itemList.setAdapter(pairListAdapter);

		nameLabel.setText(details.name);
		locationLabel.setText(details.location);
	}
	
	private boolean editable = false;

	public void setEditable(final boolean editable)
	{
		this.editable = editable;

		if (root == null)
			return;

		if (isVisible())
		{
			if (!editable && originalDetails != null && originalDetails.diff(details))
			{
				saveDetails(new Predicate<Boolean>() {public boolean apply(Boolean aBoolean)
				{
					if (aBoolean)
						setupEditable();
					return true;
				}});

				return;
			}
		}

		setupEditable();
	}

	public boolean backPressed()
	{
		if (editable && originalDetails != null && originalDetails.diff(details))
		{
			//if there are changes that need to be saved, a confirmation popup is brought up and the back action is cancelled.
			goingBack = true;
			saveDetails(null);
			return false;
		}
		
		return true;
	}
	
	private boolean goingBack = false;

	private void saveDetails()
	{
		saveDetails(null);
	}

	private void saveDetails(final Predicate<Boolean> callback)
	{
		ConfirmFragment frag = new ConfirmFragment("Save changes?", new ConfirmFragment.Listener()
		{
			public void okay() 
			{
				MainActivity.getInstance().saveDetail(details);
				setDetails(details);
				
				if (!goingBack)
					onResume();
				else
					MainActivity.getInstance().onBackPressed();

				if (callback != null)
					callback.apply(true);
			}
			
			public void discard()
			{
				setDetails(originalDetails);

				if (!goingBack)
					onResume();
				else
					MainActivity.getInstance().onBackPressed();

				if (callback != null)
					callback.apply(true);
			}

			public void cancel()
			{
				onResume();

				if (callback != null)
					callback.apply(false);
			}
		});
		frag.show(MainActivity.getInstance().getSupportFragmentManager(), "confirm_fragment");
	}

	private static class PairDetailItemWidget extends DetailItemWidget
	{
		private PasswordDetails.Pair pair;
		private ViewDetailFragment parent;
		
		public PairDetailItemWidget(PasswordDetails.Pair p, ViewDetailFragment par, Context context) 
		{
			super(context);
			parent = par;

			keyLabel.setInputType(EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			valueLabel.setInputType(EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

			setPair(p);
			

			keyLabel.setOnLongClickListener(parent.copyMenuListener);
			valueLabel.setOnLongClickListener(parent.copyMenuListener);

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

			valueInput.setOnLongClickListener(parent.copyMenuListener);

			deleteBtn.setOnClickListener(new OnClickListener() {public void onClick(View arg0) 
			{
				parent.deletePair(pair);
			}});
		}

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

	private static class DetailArrayAdapter extends ArrayAdapter<PasswordDetails.Pair>
	{
		private ViewDetailFragment host;
		
		public DetailArrayAdapter(ViewDetailFragment host, Context context, List<PasswordDetails.Pair> objects) 
		{
			super(context, 0, objects);
			
			this.host = host;
		}

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

			if (host.nextSelect == position)
			{
				ret.requestFocus();
				host.nextSelect = -1;
			}

			return ret;
		}
	}
	
	private ActionMode actionMode = null;
	private TextView actionSelected = null;

	private ActionMode.Callback actionCallback = new ActionMode.Callback() 
	{
		public boolean onPrepareActionMode(ActionMode arg0, Menu arg1) 
		{
			return false;
		}

		public void onDestroyActionMode(ActionMode arg0) 
		{
			actionMode = null;
			
			selectText(actionSelected, false);
			
			actionSelected = null;
		}

		public boolean onCreateActionMode(ActionMode mode, Menu menu) 
		{
			MenuInflater inf = mode.getMenuInflater();
			
			if (!editable)
				inf.inflate(R.menu.detail_action, menu);
			else
				inf.inflate(R.menu.detailedit_action, menu);
				
			return true;
		}

		public boolean onActionItemClicked(ActionMode mode, MenuItem item) 
		{
			int id = item.getItemId();
			
			ClipboardManager clipboard = (ClipboardManager) MainActivity.getInstance().getSystemService(Context.CLIPBOARD_SERVICE);
			
			if (id == R.id.action_detail_copy)
			{
				ClipData clip = ClipData.newPlainText("password-keeper", actionSelected.getText().toString());
				clipboard.setPrimaryClip(clip);
			}
			else if (id == R.id.action_detail_paste)
			{
				ClipData clip = clipboard.getPrimaryClip();
				int sz = clip.getItemCount();
				
				if (sz > 0)
				{
					ClipData.Item it = clip.getItemAt(0);
					String data = it.coerceToText(MainActivity.getInstance()).toString();
					actionSelected.setText(data);
				}
			}
			else if (id == R.id.action_detail_random)
			{
				String pw = generateRandomPassword(8);
				actionSelected.setText(pw);
				ClipData clip = ClipData.newPlainText("password-keeper", pw);
				clipboard.setPrimaryClip(clip);
			}
			
			return false;
		}
	};

	public String generateRandomPassword(int length)
	{
		StringBuilder ret;
		int capCount;
		int lowerCount;
		int numCount;

		do
		{
			capCount = 0;
			lowerCount = 0;
			numCount = 0;
			ret = new StringBuilder();
			
			for (int i = 0; i < length; i++)
			{
				int type = (int) (Math.random() * (double) 3);

				if (type == 0)
				{
					int n = (int) (Math.random() * (double) 26);
					ret.append((char) ('a' + n));
					lowerCount++;
				}
				else if (type == 1)
				{
					int n = (int) (Math.random() * (double) 26);
					ret.append((char) ('A' + n));
					capCount++;
				}
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

	private void setupEditable()
	{
		if (root == null)
			return;
		
		nameLabel.setEditable(editable);

		if (editable)
		{
			nameLabel.requestFocus();
		}

		if (editable && nameLabel.getText().equals("new entry"))
		{
			nameLabel.setText("");
			nameLabel.requestFocus();
		}
		else if (!editable && nameLabel.getText().isEmpty())
			nameLabel.setText("new entry");

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

		MainActivity.getInstance().keyboardListener.forceOpenKeyboard(editable);

		root.invalidate();
	}
}
