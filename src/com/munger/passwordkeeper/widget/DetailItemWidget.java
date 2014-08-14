package com.munger.passwordkeeper.widget;

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
import android.widget.ViewFlipper;

import com.munger.passwordkeeper.R;

public class DetailItemWidget extends LinearLayout
{
	public DetailItemWidget(Context context)
	{
		this(context, null);
	}
	
	public DetailItemWidget(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		
		LayoutInflater inflater = LayoutInflater.from(context);
		
		ViewFlipper v = new ViewFlipper(context);
		inflater.inflate(R.layout.widget_detailitem, this, true);
		
		ViewGroup root = (LinearLayout) getChildAt(0);
		view1 = (LinearLayout) root.getChildAt(0);
		view2 = (LinearLayout) root.getChildAt(1);
		
		keyLabel = (TextView) findViewById(R.id.detailitem_keylabel);
		keyInput = (EditText) findViewById(R.id.detailitem_keyinput);
		valueLabel = (TextView) findViewById(R.id.detailitem_valuelabel);
		valueInput = (EditText) findViewById(R.id.detailitem_valueinput);
		deleteBtn = (Button) findViewById(R.id.detailitem_deletebtn);
		
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
	
	protected LinearLayout view1;
	protected LinearLayout view2;
	
	protected TextView keyLabel;
	protected TextView valueLabel;
	protected EditText keyInput;
	protected EditText valueInput;
	protected Button deleteBtn;
	
	private boolean editable;
	
	public void setEditable(boolean editable)
	{
		view1.setVisibility(editable ? View.GONE : View.VISIBLE);
		view2.setVisibility(editable ? View.VISIBLE : View.GONE);
		
		this.editable = editable;
	}
	
	public void setKey(String key)
	{
		keyLabel.setText(key);
		keyInput.setText(key);
	}
	
	public String getKey()
	{
		return keyInput.getText().toString();
	}
	
	public void setValue(String value)
	{
		valueLabel.setText(value);
		valueInput.setText(value);
	}
	
	public String getValue()
	{
		return valueInput.getText().toString();
	}
}
