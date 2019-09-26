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

import com.munger.passwordkeeper.R;

import androidx.fragment.app.DialogFragment;

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
		builder.setPositiveButton(R.string.alert_okay, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which)
		{
			if (callback != null)
				callback.closed();
		}});
		return builder.create();
	}

	public interface CloseCallback
	{
		void closed();
	}

	private CloseCallback callback = null;

	public void setCloseCallback(CloseCallback callback)
	{
		this.callback = callback;
	}
}
