package com.munger.passwordkeeper.alert;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.R;

public class InputFragment extends DialogFragment 
{
	protected String message;
	protected String prompt;
	protected Listener listener;
	protected boolean cancelEnabled = true;

	public InputFragment()
	{
		super();
	}

	public InputFragment(String message, String prompt, Listener l)
	{
		super();
		setMessage(message);
		setPrompt(prompt);
		setListener(l);
	}

	public void setMessage(String value)
	{
		this.message = value;
	}

	public void setPrompt(String value)
	{
		this.prompt = value;
	}

	public void setListener(Listener value)
	{
		this.listener = value;
	}

	public void setCancelEnabled(boolean value)
	{
		this.cancelEnabled = value;
		setCancelable(false);
	}

	protected EditText inputView = null;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) 
	{
		inputView = new EditText(getActivity());
		inputView.setHint(prompt);
		inputView.setSingleLine();

		inputView.setOnKeyListener(new View.OnKeyListener() {public boolean onKey(View v, int keyCode, KeyEvent event) 
		{
			if (keyCode == KeyEvent.KEYCODE_ENTER)
			{
				okay();
				return true;
			}
			else
				return false;
		}});
		
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setView(inputView);
		builder.setMessage(message);

		builder.setPositiveButton(R.string.input_okay, null);

		final InputFragment that = this;
		if (cancelEnabled)
			builder.setNegativeButton(R.string.input_cancel, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which)
		{
			listener.cancel(that);
		}});
		
		Dialog ret = builder.create();
		return ret;
	}
	
	@Override
	public void onStart()
	{
	    super.onStart();
	    AlertDialog d = (AlertDialog)getDialog();
	    if(d != null)
	    {
	        Button positiveButton = (Button) d.getButton(Dialog.BUTTON_POSITIVE);
	        positiveButton.setOnClickListener(new View.OnClickListener() {public void onClick(View v)
            {
	        	okay();
            }});
	    }
	}

	protected void okay()
	{
		boolean passed = listener.okay(this, inputView.getText().toString());
        
    	if(passed)
    		dismiss();
	}

	public static interface Listener
	{
		public boolean okay(InputFragment that, String inputText);
		public void cancel(InputFragment that);
	}
}
