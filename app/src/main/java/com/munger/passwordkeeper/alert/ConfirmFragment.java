package com.munger.passwordkeeper.alert;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class ConfirmFragment extends DialogFragment
{
	private String message;
	private Listener listener;

	public ConfirmFragment(String message, Listener l)
	{
		super();
		this.message = message;
		listener = l;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) 
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(message);
		builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which) 
		{
			listener.okay();
		}});
		builder.setNegativeButton("Discard", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which)
		{
			listener.discard();
		}});
		builder.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which)
		{
			listener.cancel();
		}});
		
		return builder.create();
	}
	
	public static interface Listener
	{
		public void okay();
		public void cancel();
		public void discard();
	}
}