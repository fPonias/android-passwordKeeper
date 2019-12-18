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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.base.Predicate;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.alert.ConfirmFragment;
import com.munger.passwordkeeper.helpers.KeyboardListener;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.view.widget.DetailItemWidget;
import com.munger.passwordkeeper.view.widget.TextInputWidget;

import java.util.ArrayList;

import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;

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
	};

	private boolean initialEditable;

	public ViewDetailFragment()
	{
		super();
		initialEditable = false;
	}

	public ViewDetailFragment(boolean initialEditable)
	{
		super();
		this.initialEditable = initialEditable;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState != null)
		{
		}
		else
		{
			MainState.getInstance().keyboardListener.addKeyboardChangedListener(new KeyboardListener.OnKeyboardChangedListener()
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

		actionCallback = new EditActionCallback(this);
	};

	protected View lastFocus;
	protected long lastFocusStamp = 0;
	protected boolean keyboardOpened = false;
	private ActionMode actionMode = null;
	private TextView actionSelected = null;
	private EditActionCallback actionCallback = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{	
		root = inflater.inflate(R.layout.fragment_viewdetail, container, false);

		copyMenuListener = new View.OnLongClickListener() {public boolean onLongClick(View v)
		{
			if (!(v instanceof TextView))
				return false;

			if (actionSelected != null)
				selectText(actionSelected, false);

			if (editable)
				actionSelected = (EditText) v;
			else
				actionSelected = (TextView) v;
			actionSelected.requestFocus();
			//selectText(actionSelected, true);

			if (actionMode == null)
			{
				actionMode = MainState.getInstance().activity.startActionMode(actionCallback);
			}

			return true;
		}};


		setHasOptionsMenu(true);

		nameLabel = (TextInputWidget) root.findViewById(R.id.viewdetail_namelbl);
		locationLabel = (TextInputWidget) root.findViewById(R.id.viewdetail_locationlbl);
		itemList = (ListView) root.findViewById(R.id.viewdetail_itemlist);
		addButton = (Button) root.findViewById(R.id.viewdetail_addbtn);
		
		nameLabel.getLabel().setOnLongClickListener(copyMenuListener);
		nameLabel.getInput().setOnLongClickListener(copyMenuListener);
		locationLabel.getLabel().setOnLongClickListener(copyMenuListener);
		locationLabel.getInput().setOnLongClickListener(copyMenuListener);

		if (details != null)
			setupFields();

		itemList.setFocusable(false);

		nameLabel.getInput().setOnEditorActionListener(new TextView.OnEditorActionListener() { public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
		{
			if (actionId == EditorInfo.IME_ACTION_NEXT)
			{
				locationLabel.requestFocus();
				return true;
			}
			return false;
		}});

		nameLabel.getInput().setOnFocusChangeListener(new View.OnFocusChangeListener() {public void onFocusChange(View v, boolean hasFocus)
		{
			if (!hasFocus)
			{
				details.setName(nameLabel.getText());
			}
		}});

		locationLabel.getInput().setOnEditorActionListener(new TextView.OnEditorActionListener() { public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
		{
			if (actionId == EditorInfo.IME_ACTION_NEXT)
			{
				View next = addButton;
				if (itemList.getChildCount() > 0)
				{
					PairDetailItemWidget view = (PairDetailItemWidget) itemList.getChildAt(0);
					next = view.findViewById(view.getKeyInputId());
				}

				next.requestFocus();
				return true;
			}
			return false;
		}});

		locationLabel.getInput().setOnFocusChangeListener(new View.OnFocusChangeListener() {public void onFocusChange(View v, boolean hasFocus)
		{
			if (!hasFocus)
			{
				details.setLocation(locationLabel.getText());
			}

		}});
		

		addButton.setOnClickListener(new View.OnClickListener() {public void onClick(View arg0) 
		{
			if (details == null)
				return;
			
			addPair();
		}});

		itemList.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
			@Override
			public void onChildViewAdded(View parent, View child) {
				updateFocusListeners();
			}

			@Override
			public void onChildViewRemoved(View parent, View child) {
				updateFocusListeners();
			}
		});

		editable = initialEditable;
		setupEditable();
		
		return root;
	}

	private class ValueEditorListener implements TextView.OnEditorActionListener
	{
		public int position;
		public EditText target;
		public ValueEditorListener(EditText target, int position)
		{
			this.target = target;
			this.position = position;
		}

		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
		{
			if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT)
			{
				int sz = itemList.getChildCount();
				View target = null;
				if (position < sz - 1)
				{
					PairDetailItemWidget tWidget = (PairDetailItemWidget) itemList.getChildAt(position + 1);
					target = tWidget.findViewById(tWidget.getKeyInputId());
				}
				else
				{
					target = addButton;
				}

				target.requestFocus();
				return true;
			}

			return false;
		}
	}

	private void updateFocusListeners()
	{
		int sz = itemList.getChildCount();
		for (int i = 0; i < sz; i++)
		{
			PairDetailItemWidget widget = (PairDetailItemWidget) itemList.getChildAt(i);
			EditText target = (EditText) widget.findViewById(widget.getValueInputId());
			target.setOnEditorActionListener(new ValueEditorListener(target, i));
		}
	}

	private boolean useFiltered = false;
	private DetailArrayAdapter pairListAdapter = null;
	private DetailArrayAdapter filterAdapter = null;
	private ArrayList<PasswordDetailsPair> filtered = new ArrayList<PasswordDetailsPair>();

	public ArrayList<PasswordDetailsPair> searchDetails(PasswordDetails orig, String search)
	{
		ArrayList<PasswordDetailsPair> ret = new ArrayList<PasswordDetailsPair>();
		
		search = search.toLowerCase();

		int sz = orig.count();
		for(int i = 0; i < sz; i++)
		{
			PasswordDetailsPair pair = orig.getPair(i);
			if (pair.getKey().toLowerCase().contains(search) || pair.getValue().toLowerCase().contains(search))
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
						filtered = searchDetails(details, arg0);

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

	private void deletePair(PasswordDetailsPair p)
	{
		details.removePair(p);
		pairListAdapter.notifyDataSetChanged();
		
		if (useFiltered)
		{
			filtered.remove(p);
			filterAdapter.remove(p);
			filterAdapter.notifyDataSetChanged();
		}
	}

	private int nextSelect = -1;

	private boolean pairAdded = false;

	private void addPair()
	{
		pairAdded = true;
		PasswordDetailsPair pair = details.addEmptyPair();
		pairListAdapter.notifyDataSetChanged();

		nextSelect = details.count() - 1;
	}

	private boolean delayedCreateEmptyPair = false;

	public void setDetails(PasswordDetails dets)
	{
		if (dets.getName().isEmpty() || (dets.getName().equals(PasswordDocument.emptyEntryTitle) && dets.getLocation().isEmpty()))
		{
			if (dets.count() == 0)
			{
				delayedCreateEmptyPair = true;
			}
		}

		originalDetails = dets;
		details = dets.copy();

		if (root != null)
		{
			setupFields();
		}
	}

	public PasswordDetails getDetails()
	{
		return details;
	}

	public PasswordDetails getOriginalDetails()
	{
		return originalDetails;
	}

	private void setupFields()
	{

		pairListAdapter = new DetailArrayAdapter(this, MainState.getInstance().context, details.getList());
		itemList.setAdapter(pairListAdapter);

		nameLabel.setText(details.getName());
		locationLabel.setText(details.getLocation());

		if (delayedCreateEmptyPair)
		{
			addPair();
			setEditable(true);
			delayedCreateEmptyPair = false;
		}
	}
	
	private boolean editable = false;

	public boolean getEditable()
	{
		return editable;
	}

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

	public void backPressed(final NavigationHelper.Callback callback)
	{
		if (editable && originalDetails != null && originalDetails.diff(details))
		{
			//if there are changes that need to be saved, a confirmation popup is brought up and the back action is cancelled.
			goingBack = true;
			saveDetails(new Predicate<Boolean>() {public boolean apply(Boolean aBoolean)
			{
				if (initialEditable == false)
					MainState.getInstance().navigationHelper.setEditable(false);

				callback.callback(aBoolean);
				return true;
			}});
		}
		else
		{
			callback.callback(true);
			if (initialEditable == false)
				MainState.getInstance().navigationHelper.setEditable(false);
		}
	}
	
	private boolean goingBack = false;

	private void saveDetails()
	{
		saveDetails(null);
	}

	private void saveDetails(final Predicate<Boolean> callback)
	{
		View currentFocus = MainState.getInstance().activity.getCurrentFocus();
		if (currentFocus != null)
			currentFocus.clearFocus();

		ConfirmFragment frag = new ConfirmFragment("Save changes?", new ConfirmFragment.Listener()
		{
			public void okay() 
			{
				MainState.getInstance().navigationHelper.saveDetail(details, new NavigationHelper.Callback() {public void callback(Object o)
				{
					PasswordDetails dets = (PasswordDetails) o;
					setDetails(dets);

					if (!goingBack)
						onResume();

					if (callback != null)
						callback.apply(true);
				}});
			}
			
			public void discard()
			{
				setDetails(originalDetails);

				if (!goingBack)
					onResume();

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
		frag.show(MainState.getInstance().activity.getSupportFragmentManager(), "confirm_fragment");
	}

	private class PairDetailItemWidget extends DetailItemWidget
	{
		private PasswordDetailsPair pair;
		private ViewDetailFragment parent;
		private int keyId;
		private int valueId;
		
		public PairDetailItemWidget(PasswordDetailsPair p, ViewDetailFragment par, Context context)
		{
			super(context);
			parent = par;

			keyLabel.setInputType(EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
			valueLabel.setInputType(EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

			setPair(p);
			

			keyLabel.setOnLongClickListener(parent.copyMenuListener);
			valueLabel.setOnLongClickListener(parent.copyMenuListener);

			keyInput.setOnFocusChangeListener(new OnFocusChangeListener() {public void onFocusChange(View v, boolean hasFocus)
			{
				if (!hasFocus)
					pair.setKey(keyInput.getText().toString());
			}});
			keyInput.setOnLongClickListener(parent.copyMenuListener);
			keyInput.setOnKeyListener(new OnKeyListener() {
				@Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					return false;
				}
			});
			keyInput.setOnEditorActionListener(new TextView.OnEditorActionListener() { public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
			{
				if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE)
				{
					valueInput.requestFocus();
					return true;
				}
				return false;
			}});

			valueInput.setOnFocusChangeListener(new OnFocusChangeListener() {public void onFocusChange(View v, boolean hasFocus)
			{
				if (!hasFocus)
					pair.setValue(valueInput.getText().toString());
			}});
			valueInput.setOnLongClickListener(parent.copyMenuListener);

			deleteBtn.setOnClickListener(new OnClickListener() {public void onClick(View arg0) 
			{
				parent.deletePair(pair);
			}});

			keyId = View.generateViewId();
			valueId = View.generateViewId();
			keyInput.setId(keyId);
			valueInput.setId(valueId);
		}

		public int getKeyInputId()
		{
			return keyId;
		}

		public int getValueInputId()
		{
			return valueId;
		}

		public void setPair(PasswordDetailsPair pair)
		{
			this.pair = pair;
			
			if (pair != null)
			{
				setKey(pair.getKey());
				setValue(pair.getValue());
			}
			else
			{
				setKey("");
				setValue("");
			}
		}
		
		@SuppressWarnings("unused")
		public PasswordDetailsPair getPair()
		{
			return pair;
		}
	}

	private class DetailArrayAdapter extends ArrayAdapter<PasswordDetailsPair>
	{
		private ViewDetailFragment host;
		
		public DetailArrayAdapter(ViewDetailFragment host, Context context, ArrayList<PasswordDetailsPair> objects)
		{
			super(context, 0, objects);
			
			this.host = host;
		}

		@Override
		public View getView(int position, View convertView, final ViewGroup par)
		{
			final PasswordDetailsPair pair = getItem(position);

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


	public static class EditActionCallback implements ActionMode.Callback
	{
		public boolean isPrepared = false;
		public ViewDetailFragment parent;

		public EditActionCallback(ViewDetailFragment parent)
		{
			this.parent = parent;
		}

		public boolean onPrepareActionMode(ActionMode arg0, Menu arg1) 
		{
			isPrepared = false;

			return false;
		}

		public void onDestroyActionMode(ActionMode arg0) 
		{
			parent.actionMode = null;
			parent.actionSelected = null;
		}

		public boolean onCreateActionMode(ActionMode mode, Menu menu) 
		{
			MenuInflater inf = mode.getMenuInflater();
			
			if (!parent.editable)
				inf.inflate(R.menu.detail_action, menu);
			else
				inf.inflate(R.menu.detailedit_action, menu);

			isPrepared = true;

			return true;
		}

		public boolean onActionItemClicked(ActionMode mode, MenuItem item) 
		{
			int id = item.getItemId();
			
			final ClipboardManager clipboard = (ClipboardManager) MainState.getInstance().context.getSystemService(Context.CLIPBOARD_SERVICE);
			
			if (id == R.id.action_detail_copy)
			{
				ClipData clip = ClipData.newPlainText("password-keeper", parent.actionSelected.getText().toString());
				clipboard.setPrimaryClip(clip);
				mode.finish();
				return true;
			}
			else if (id == R.id.action_detail_paste)
			{
				ClipData clip = clipboard.getPrimaryClip();
				int sz = clip.getItemCount();
				
				if (sz > 0)
				{
					ClipData.Item it = clip.getItemAt(0);
					String data = it.coerceToText(MainState.getInstance().context).toString();
					parent.actionSelected.setText(data);
					mode.finish();

					return true;
				}
			}
			else if (id == R.id.action_detail_random)
			{
				String pw = parent.generateRandomPassword(8);
				parent.actionSelected.setText(pw);

				ClipData clip = ClipData.newPlainText("password-keeper", pw);
				clipboard.setPrimaryClip(clip);

				mode.finish();
				return true;
			}
			
			return false;
		}
	};

	public boolean isActionMenuPrepared()
	{
		return actionCallback.isPrepared;
	}

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

		if (editable && nameLabel.getText().equals(PasswordDocument.emptyEntryTitle))
		{
			nameLabel.setText("");
			nameLabel.requestFocus();
		}
		else if (!editable && nameLabel.getText().isEmpty())
			nameLabel.setText(PasswordDocument.emptyEntryTitle);

		locationLabel.setEditable(editable);
		
		addButton.setVisibility(editable ? View.VISIBLE : View.GONE);
		
		int sz = details.count();
		
		if (sz == 0)
		{
			PasswordDetailsPair p = details.addEmptyPair();
			pairListAdapter.notifyDataSetChanged();
		}
		
		
		sz = itemList.getCount();
		for (int i = 0; i < sz; i++)
		{
			DetailItemWidget item = (DetailItemWidget) itemList.getChildAt(i);
			
			if (item != null)
				item.setEditable(editable);
		}

		if (editable)
			MainState.getInstance().keyboardListener.forceOpenKeyboard(nameLabel.getInput());

		root.invalidate();
	}
}
