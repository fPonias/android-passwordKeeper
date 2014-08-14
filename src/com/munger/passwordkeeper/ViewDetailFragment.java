package com.munger.passwordkeeper;

import java.util.List;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import com.munger.passwordkeeper.alert.ConfirmFragment;
import com.munger.passwordkeeper.util.PasswordDetails;
import com.munger.passwordkeeper.widget.DetailItemWidget;
import com.munger.passwordkeeper.widget.TextInputWidget;

public class ViewDetailFragment extends Fragment 
{
	private MainActivity parent;
	private View root = null;
	
	private TextInputWidget nameLabel;
	private TextInputWidget locationLabel;
	private ListView itemList;
	private Button addButton;
	
	private DetailArrayAdapter pairListAdapter = null;
	
	private PasswordDetails details = null;
	private PasswordDetails originalDetails = null;
	
	private View.OnLongClickListener copyMenuListener;
	
	public static String getName()
	{
		return "Detail";
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{	
		this.parent = (MainActivity) getActivity();
		root = inflater.inflate(R.layout.fragment_viewdetail, container, false);
	
		setHasOptionsMenu(true);
		
		nameLabel = (TextInputWidget) root.findViewById(R.id.viewdetail_namelbl);
		locationLabel = (TextInputWidget) root.findViewById(R.id.viewdetail_locationlbl);
		itemList = (ListView) root.findViewById(R.id.viewdetail_itemlist);
		addButton = (Button) root.findViewById(R.id.viewdetail_addbtn);
		
		if (details != null)
			setupFields();
		
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
		
		nameLabel.setInputChangeListener(new TextInputWidget.InputChangedListener() {public void changed() 
		{
			if (details != null)
				details.name = nameLabel.getText();
		}});
		
		nameLabel.getLabel().setOnLongClickListener(copyMenuListener);
		
		locationLabel.setInputChangeListener(new TextInputWidget.InputChangedListener() {public void changed() 
		{
			if (details != null)
				details.location = locationLabel.getText();
		}});
		
		locationLabel.getLabel().setOnLongClickListener(copyMenuListener);
		
		addButton.setOnClickListener(new View.OnClickListener() {public void onClick(View arg0) 
		{
			if (details == null)
				return;
			
			addPair();
		}});
		
		return root;
	}
	
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
	}
	
	private void addPair()
	{
		PasswordDetails.Pair pair = new PasswordDetails.Pair();
		details.details.add(pair);
		pairListAdapter.notifyDataSetChanged();
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
		pairListAdapter = new DetailArrayAdapter(this, parent, R.layout.widget_detailitem, details.details);
		itemList.setAdapter(pairListAdapter);

		nameLabel.setText(details.name);
		locationLabel.setText(details.location);
	}
	
	private boolean editable = false;
	
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
	
	public boolean backPressed()
	{
		if (editable && originalDetails != null && originalDetails.diff(details))
		{
			goingBack = true;
			saveDetails();
			return false;
		}
		
		return true;
	}
	
	private boolean goingBack = false;
	
	private void saveDetails()
	{
		ConfirmFragment frag = new ConfirmFragment("Save changes?", new ConfirmFragment.Listener() 
		{
			public void okay() 
			{
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
	
	private static class PairDetailItemWidget extends DetailItemWidget
	{
		private PasswordDetails.Pair pair;
		private ViewDetailFragment parent;
		
		public PairDetailItemWidget(PasswordDetails.Pair p, ViewDetailFragment par, Context context) 
		{
			super(context);
			parent = par;
			
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
		
		public DetailArrayAdapter(ViewDetailFragment host, Context context, int resource, List<PasswordDetails.Pair> objects) 
		{
			super(context, resource, objects);
			
			this.host = host;
		}
		
		@Override
		public View getView(int position, View convertView, final ViewGroup par) 
		{	
			final PasswordDetails.Pair pair = getItem(position);
			
			PairDetailItemWidget ret;
			
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
			
			ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
			
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
					String data = it.coerceToText(parent).toString();
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
	
	@Override
	public void onResume()
	{
		super.onResume();
		
		if (details == null)
			return;
		
		if (root == null)
			return;
		
		setupEditable();
	}

	private void setupEditable()
	{
		if (root == null)
			return;
		
		nameLabel.setEditable(editable);
		locationLabel.setEditable(editable);
		
		addButton.setVisibility(editable ? View.VISIBLE : View.GONE);
		
		int sz = itemList.getChildCount();
		for (int i = 0; i < sz; i++)
		{
			DetailItemWidget item = (DetailItemWidget) itemList.getChildAt(i);
			item.setEditable(editable);
		}
		
		root.invalidate();
	}
}
