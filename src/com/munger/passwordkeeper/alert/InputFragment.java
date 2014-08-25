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

/**
 * popup that asks the user for a text input.
 *
 */
public class InputFragment extends DialogFragment 
{
	/** The message to display above the input */ protected String message;
	/** The message to display in the blank input */ protected String prompt;
	/** The okay/cancel handler */ protected Listener listener;
	
	/**
	 * Create a new alert that has an input box, an okay, and cancel button
	 * @param message the message to display above the input
	 * @param prompt the message to display in the blank input
	 * @param l the listener that handles the okay and cancel events
	 */
	public InputFragment(String message, String prompt, Listener l)
	{
		super();
		this.message = message;
		this.prompt = prompt;
		listener = l;
	}
	
	/**
	 * The input view
	 */
	protected EditText inputView = null;
	
	/**
	 * grab references to all the alert components and setup event handlers.
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) 
	{
		inputView = new EditText(getActivity());
		inputView.setHint(prompt);
		inputView.setSingleLine();
		
		//submit on keyboard enter
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
		
		//this is a blank handler, the real one is in the onStart method
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
	    	//it was created like this so the event listener would have the chance to keep the alert open 
	        Button positiveButton = (Button) d.getButton(Dialog.BUTTON_POSITIVE);
	        positiveButton.setOnClickListener(new View.OnClickListener() {public void onClick(View v)
            {
	        	okay();
            }});
	    }
	}
	
	/**
	 * The default action to take when the okay button is pressed.
	 */
	protected void okay()
	{
		boolean passed = listener.okay(inputView.getText().toString());
        
    	if(passed)
    		dismiss();
	}
	
	/**
	 * Use this interface to handle okay and cancel events from this popup.
	 */
	public static interface Listener
	{
		/**
		 * This is called when okay is clicked with the text currently in the input.
		 * @param inputText the final text the user input.
		 * @return return true if you want the dialog to close, false if you want it to stay open.
		 */
		public boolean okay(String inputText);
		/**
		 * This is called when cancel is clicked.
		 */
		public void cancel();
	}
}
