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
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.munger.passwordkeeper.R;

/**
 * General purpose text input widget composed of a label or text input and a delete button.
 * There are two views here that can be switch by setting it as editable or not.
 * @author codymunger
 *
 */
public class TextInputWidget extends ViewSwitcher
{	
	public TextInputWidget(Context context)
	{
		this(context, null);
	}
	
	/**
	 * Constructor
	 * @param context the application context
	 * @param attrs There are three accepted extra attributes for this widget:  align, noDelete, and hint<br/>
	 * align can be left, center, or right<br/>
	 * noDelete can be true or false and hides the delete button<br/>
	 * hint provides a hint for the input text box
	 */
	public TextInputWidget(Context context, AttributeSet attrs) 
	{
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TextInputWidget);
		
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.widget_textinput, this, true);
		
		//get relevant view references
		switcher = (ViewSwitcher) getChildAt(0);
		//label = (TextView) switcher.findViewById(R.id.widget_ti_label);
		label = (TextView) switcher.getChildAt(0);
		//input = (EditText) switcher.findViewById(R.id.widget_ti_editText);
		//deleteBtn = (Button) switcher.findViewById(R.id.widget_ti_deleteBtn);
		input = (EditText) ((RelativeLayout) switcher.getChildAt(1)).getChildAt(0);
		deleteBtn = (Button) ((RelativeLayout) switcher.getChildAt(1)).getChildAt(1);
		
		//interpret the custom xml attributes
		CharSequence s = a.getString(R.styleable.TextInputWidget_align);
	    if (s != null)
	    {
	    	int gravity = Gravity.LEFT;
	    	if (s.equals("left"))
	    		gravity = Gravity.LEFT;
	    	else if (s.equals("center"))
	    		gravity = Gravity.CENTER;
	    	else if (s.equals("right"))
	    		gravity = Gravity.RIGHT;
	    	
	   		label.setGravity(gravity);
	   		input.setGravity(gravity);
	    }
	    
	    boolean b = a.getBoolean(R.styleable.TextInputWidget_noDelete, false);
	    deleteBtn.setVisibility((b) ? View.GONE : View.VISIBLE);
		
	    s = a.getString(R.styleable.TextInputWidget_hint);
	    if (s != null)
	    {
	    	input.setHint(s);
	    }
	    
	    //add a key listener that sets the label text to mirror the input text
	    input.addTextChangedListener(new TextWatcher() 
	    {
	    	public void onTextChanged(CharSequence s, int start, int before, int count) 
	    	{
			}
			
			public void beforeTextChanged(CharSequence s, int start, int count, int after) 
			{
			}
			
			public void afterTextChanged(Editable s) 
			{
				if (input.getText().toString().equals(label.getText()))
					changed = false;
				else
				{
					label.setText(input.getText());
					changed = true;
					
					if (inputListener != null)
						inputListener.changed();
				}
			}
		});
		
	    //let the provided listener know there was a click on the non-editable label
		label.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
		{
			if(labelListener != null)
				labelListener.clicked();
		}});
		
		//let the provided listener know there was a click on the delete button
		deleteBtn.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
		{
			if (deleteListener != null)
				deleteListener.clicked();
		}});
	}
	
	/** the root view that switches between editable and non-editable mode */ private ViewSwitcher switcher;
	/** the non-editable label */ private TextView label;
	/** the editable input */ private EditText input;
	/** the delete button */ private Button deleteBtn;
	
	/** is this input editable? */ private boolean editable = false;
	/** has the input changed since initially set? */ private boolean changed = false;
	
	public void setEditable(boolean editable)
	{
		if (this.editable != editable)
		{
			//this view switcher actually works for some reason
			switcher.showNext();
			this.editable = editable;
		}
	}
	
	/**
	 * Some bug in Android was causing all instances of TextInputWidget to share the same savedBundleResources object.
	 * This function had to be overriden to avoid every instance of this object being assigned the same string.
	 */
	@Override
	protected void dispatchRestoreInstanceState(android.util.SparseArray<android.os.Parcelable> container) 
	{
		
	};
	
	/**
	 * Set the editable input and non-editable label text.
	 * @param text
	 */
	public void setText(String text)
	{
		label.setText(text);
		input.setText(text);
		
		changed = false;
	}
	
	/**
	 * Get the editable input text 
	 * @return
	 */
	public String getText()
	{
		return input.getText().toString();
	}
	
	public TextView getLabel()
	{
		return label;
	}
	
	public EditText getInput()
	{
		return input;
	}
	
	public Button getDeleteButton()
	{
		return deleteBtn;
	}
	
	/**
	 * Hide the delete button if we just want the input/label view
	 * @param show
	 */
	public void showDeleteBtn(boolean show)
	{
		deleteBtn.setVisibility(show ? View.VISIBLE : View.GONE);
	}
	
	/**
	 * Was the input changed?
	 * @return
	 */
	public boolean getChanged()
	{
		return changed;
	}
	
	//the listeners are self explanatory
	
	public static interface LabelClickedListener
	{
		public void clicked();
	}
	
	private LabelClickedListener labelListener = null;
	
	public void setLabelClickedListener(LabelClickedListener listener)
	{
		labelListener = listener;
	}
	
	public static interface DeleteClickedListener
	{
		public void clicked();
	}
	
	private DeleteClickedListener deleteListener = null;
	
	public void setDeleteClickedListener(DeleteClickedListener listener)
	{
		deleteListener = listener;
	}
	
	public static interface InputChangedListener
	{
		public void changed();
	}
	
	private InputChangedListener inputListener = null;
	
	public void setInputChangeListener(InputChangedListener listener)
	{
		inputListener = listener;
	}
}
