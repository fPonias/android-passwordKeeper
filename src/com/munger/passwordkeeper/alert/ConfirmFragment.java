package com.munger.passwordkeeper.alert;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * A popup fragment that contains a message, an okay button, and a cancel button.
 * The popup also requires an event listener from the caller so they can handle the okay and cancel events.
 * @author codymunger
 *
 */
public class ConfirmFragment extends DialogFragment
{
	private String message;
	private Listener listener;
	
	/**
	 * Create the popup with the specified message, okay button, and cancel button
	 * @param message the message to display
	 * @param l a provided listener to handle the okay and cancel events
	 */
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
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which)
		{
			listener.cancel();
		}});
		
		return builder.create();
	}
	
	public static interface Listener
	{
		public void okay();
		public void cancel();
	}
}