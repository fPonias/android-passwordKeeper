package com.munger.passwordkeeper.alert;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.widget.EditText;

public class PasswordFragment extends DialogFragment 
{
	private String message;
	private String prompt;
	private Listener listener;
	
	public PasswordFragment(String message, String prompt, Listener l)
	{
		super();
		this.message = message;
		this.prompt = prompt;
		listener = l;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) 
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(message);
		
		final EditText passInput = new EditText(getActivity());
		passInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		passInput.setHint(prompt);
		builder.setView(passInput);
		
		builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which) 
		{
			listener.okay(passInput.getText().toString());
		}});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which)
		{
			listener.cancel();
		}});
		
		return builder.create();
	}
	
	public static interface Listener
	{
		public void okay(String password);
		public void cancel();
	}
}
