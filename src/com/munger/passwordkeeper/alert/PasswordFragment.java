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
