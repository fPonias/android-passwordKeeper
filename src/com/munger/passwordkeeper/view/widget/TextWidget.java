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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.munger.passwordkeeper.R;

/**
 * Simple widget that is a label and a delete button.
 * There is no editable mode to this widget
 * @author codymunger
 *
 */
public class TextWidget extends RelativeLayout 
{
	public TextWidget(Context context)
	{
		this(context, null);
	}
	
	public TextWidget(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.widget_text, this, true);
		
		label = (TextView) this.findViewById(R.id.widget_t_label);
		deleteBtn = (Button) this.findViewById(R.id.widget_t_deleteBtn);
		
		label.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
		{
			if (listener != null)
				listener.labelClicked();
		}});
		
		deleteBtn.setOnClickListener(new View.OnClickListener() {public void onClick(View v)
		{
			if (listener != null)
				listener.deleteClicked();
		}});
	}
	
	private TextView label;
	private Button deleteBtn;
		
	public void setText(String text)
	{
		label.setText(text);
	}
	
	public String getText()
	{
		return label.getText().toString();
	}
	
	public void setEditable(boolean editable)
	{
		deleteBtn.setVisibility(editable ? View.VISIBLE : View.GONE);
	}
	
	public static interface Listener
	{
		public void deleteClicked();
		public void labelClicked();
	}
	
	private Listener listener = null;
	
	public void setListener(Listener l)
	{
		listener = l;
	}
}
