package com.munger.passwordkeeper.view.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DateTimeKeyListener;
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

public class TextInputWidget extends ViewSwitcher
{	
	public TextInputWidget(Context context)
	{
		this(context, null);
	}

	public TextInputWidget(Context context, AttributeSet attrs) 
	{
		super(context, attrs);

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TextInputWidget);
		
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.widget_textinput, this, true);

		switcher = (ViewSwitcher) getChildAt(0);
		label = (TextView) switcher.getChildAt(0);
		input = (EditText) ((RelativeLayout) switcher.getChildAt(1)).getChildAt(0);
		deleteBtn = (Button) ((RelativeLayout) switcher.getChildAt(1)).getChildAt(1);

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

		label.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
		{
			if(labelListener != null)
				labelListener.clicked();
		}});

		deleteBtn.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
		{
			if (deleteListener != null)
				deleteListener.clicked();
		}});
	}
	
	private ViewSwitcher switcher;
	private TextView label;
	private EditText input;
	private Button deleteBtn;
	
	private boolean editable = false;
	private boolean changed = false;

	private long lastFocus = 0;
	
	public void setEditable(boolean editable)
	{
		if (this.editable != editable)
		{
			switcher.showNext();
			this.editable = editable;
		}
	}

	@Override
	protected void dispatchRestoreInstanceState(android.util.SparseArray<android.os.Parcelable> container) 
	{
		
	};

	public void setText(String text)
	{
		label.setText(text);
		input.setText(text);
		
		changed = false;
	}

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

	public void showDeleteBtn(boolean show)
	{
		deleteBtn.setVisibility(show ? View.VISIBLE : View.GONE);
	}

	public boolean getChanged()
	{
		return changed;
	}

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
