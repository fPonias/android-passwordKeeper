package com.munger.passwordkeeper.alert;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.munger.passwordkeeper.R;

import androidx.fragment.app.DialogFragment;

public class ConfirmFragment extends DialogFragment
{
	private String message;
	private Listener listener;
	private int enabledButtons;

	public ConfirmFragment(String message, Listener l)
	{
		this(message, POSITIVE | NEUTRAL | NEGATIVE, l);
	}

	public static int POSITIVE = 0x1;
	public static int NEUTRAL = 0x2;
	public static int NEGATIVE = 0x4;

	public ConfirmFragment(String message, int enabledButtons, Listener l)
	{
		super();
		this.enabledButtons = enabledButtons;
		this.message = message;
		listener = l;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) 
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setMessage(message);

		if ((enabledButtons & POSITIVE) > 0)
		{
			builder.setPositiveButton(getString(R.string.confirm_okay), new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which)
			{
				listener.okay();
			}});
		}

		if ((enabledButtons & NEGATIVE) > 0)
		{
			builder.setNegativeButton(getString(R.string.confirm_discard), new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which)
			{
				listener.discard();
			}});
		}

		if ((enabledButtons & NEUTRAL) > 0)
		{
			builder.setNeutralButton(getString(R.string.confirm_cancel), new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which)
			{
				listener.cancel();
			}});
		}
		
		return builder.create();
	}
	
	public static class Listener
	{
		public void okay() {}
		public void cancel() {}
		public void discard() {}
	}
}