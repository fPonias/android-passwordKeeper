package com.munger.passwordkeeper.alert;

import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;

/**
 * This is the same as the input alert.
 * The only difference is the input is password obscured.
 * @author codymunger
 *
 */
public class PasswordFragment extends InputFragment 
{	
	public PasswordFragment(String message, String prompt, Listener l)
	{
		super(message, prompt, l);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) 
	{
		Dialog ret = super.onCreateDialog(savedInstanceState);
		
		inputView.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		
		return ret;
	}
}
