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

package com.munger.passwordkeeper.view.widget;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.munger.passwordkeeper.R;

/**
 * A widget that displays two labels/inputs side by side with a delete button on the far right.
 * This widget can be swapped to editable and viewable mode.
 * This was intended only to render PasswordDetails.Pair objects, which accounts for the names of the fields: key and value
 */
public class DetailItemWidget extends LinearLayout
{
	public DetailItemWidget(Context context)
	{
		this(context, null);
	}
	
	/**
	 * Get references to relevant views and setup event listeners.
	 * @param context The application context
	 * @param not used
	 */
	public DetailItemWidget(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		
		LayoutInflater inflater = LayoutInflater.from(context);
		
		//get relevant view references
		//ViewFlipper v = new ViewFlipper(context);
		inflater.inflate(R.layout.widget_detailitem, this, true);
		
		ViewGroup root = (LinearLayout) getChildAt(0);
		view1 = (LinearLayout) root.getChildAt(0);
		view2 = (LinearLayout) root.getChildAt(1);
		
		keyLabel = (TextView) findViewById(R.id.detailitem_keylabel);
		keyInput = (EditText) findViewById(R.id.detailitem_keyinput);
		valueLabel = (TextView) findViewById(R.id.detailitem_valuelabel);
		valueInput = (EditText) findViewById(R.id.detailitem_valueinput);
		deleteBtn = (Button) findViewById(R.id.detailitem_deletebtn);
		
		
		//make the matching view label match the edit text
		keyInput.addTextChangedListener(new TextWatcher() 
		{
			public void onTextChanged(CharSequence s, int start, int before, int count) 
			{}
			
			public void beforeTextChanged(CharSequence s, int start, int count, int after) 
			{}
			
			public void afterTextChanged(Editable s) 
			{
				keyLabel.setText(keyInput.getText());
			}
		});

		//make the matching view label match the edit text
		valueInput.addTextChangedListener(new TextWatcher() 
		{
			public void onTextChanged(CharSequence s, int start, int before, int count) 
			{}
			
			public void beforeTextChanged(CharSequence s, int start, int count, int after) 
			{}
			
			public void afterTextChanged(Editable s) 
			{
				valueLabel.setText(valueInput.getText());
			}
		});
		
		editable = true;
	}
	
	/** The non-editable view */ protected LinearLayout view1;
	/** the editable view */ protected LinearLayout view2;
	
	/** the left side non-editable label */ protected TextView keyLabel;
	/** the right side non-editable label */ protected TextView valueLabel;
	/** the left side editable input */ protected EditText keyInput;
	/** the right side editable input */ protected EditText valueInput;
	/** the far right delete button */ protected Button deleteBtn;
	
	/** which mode is this widget in? */ private boolean editable;
	
	/**
	 * Swap the editable/non-editable view.
	 * the ViewSwitcher functionality wasn't working so I manually overrode it.
	 * @param editable
	 */
	public void setEditable(boolean editable)
	{
		view1.setVisibility(editable ? View.GONE : View.VISIBLE);
		view2.setVisibility(editable ? View.VISIBLE : View.GONE);
		
		this.editable = editable;
	}
	
	public boolean getEditable()
	{
		return this.editable;
	}
	
	/**
	 * Set the key value for both key views
	 * @param key the value of the key views
	 */
	public void setKey(String key)
	{
		keyLabel.setText(key);
		keyInput.setText(key);
	}
	
	/**
	 * Get the current edit view version of the key
	 * @return the key string
	 */
	public String getKey()
	{
		return keyInput.getText().toString();
	}
	
	/**
	 * Set the value value for both value views
	 * @param value the value of the value views
	 */
	public void setValue(String value)
	{
		valueLabel.setText(value);
		valueInput.setText(value);
	}
	
	/**
	 * Get the current edit view version of the value
	 * @return the value string
	 */
	public String getValue()
	{
		return valueInput.getText().toString();
	}
}
