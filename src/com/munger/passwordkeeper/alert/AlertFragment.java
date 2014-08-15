package com.munger.passwordkeeper.alert;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Popup fragment that contains an alert message and an okay button.
 * There's nothing special about this and frankly I'm surpised Android doesn't have a simpler way to do this.
 *
 */
public class AlertFragment extends DialogFragment
{
	private String message;
	
	/**
	 * Display an alert with the specified message.
	 * @param message
	 */
	public AlertFragment(String message)
	{
		super();
		this.message = message;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) 
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(message);
		builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which) 
		{
		}});
		return builder.create();
	}
}
