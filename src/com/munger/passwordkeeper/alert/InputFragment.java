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

public class InputFragment extends DialogFragment 
{
	private String message;
	private String prompt;
	private Listener listener;
	
	public InputFragment(String message, String prompt, Listener l)
	{
		super();
		this.message = message;
		this.prompt = prompt;
		listener = l;
	}
	
	private EditText inputView = null;
	
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
		
		//we want the dialog listener to give the option to close this dialogue
		//so the action okay handler is below
		builder.setPositiveButton("Okay", null);
		
		builder.setNegativeButton("Cacnel", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which) 
		{
			listener.cancel();
		}});
		
		return builder.create();
	}
	
	@Override
	public void onStart()
	{
	    super.onStart();    //super.onStart() is where dialog.show() is actually called on the underlying dialog, so we have to do it after this point
	    AlertDialog d = (AlertDialog)getDialog();
	    if(d != null)
	    {
	    	//this is the actual okay handler
	        Button positiveButton = (Button) d.getButton(Dialog.BUTTON_POSITIVE);
	        positiveButton.setOnClickListener(new View.OnClickListener() {public void onClick(View v)
            {
				okay();
            }});
	    }
	}
	
	public void okay()
	{
		boolean passed = listener.okay(inputView.getText().toString());
        
		if(passed)
            dismiss();
	}
	
	public static interface Listener
	{
		public boolean okay(String password);
		public void cancel();
	}
}
