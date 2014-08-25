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

package com.munger.passwordkeeper.view;

import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class AboutFragment extends Fragment
{
	private MainActivity parent;
	private View root;

	/**
	 * The tag this fragment is supposed to use.
	 * @return
	 */
	public static String getName()
	{
		return "About";
	}
	
	/**
	 * Once the view is created, get references for any interactive views and setup default event listeners.
	 */
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		this.parent = (MainActivity) getActivity();
		root = inflater.inflate(R.layout.fragment_about, container, false);
		return root;
	}	
}
