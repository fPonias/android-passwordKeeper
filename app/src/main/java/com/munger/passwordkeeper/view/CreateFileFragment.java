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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.alert.AlertFragment;

public class CreateFileFragment extends Fragment
{
	public CreateFileFragment()
	{

	}

	public static String getName()
	{
		return "Create";
	}
	
	public void setEditable(boolean editable)
	{}

	private int type;
	
	
	private Button cancelBtn;
	private Button okayBtn;
	private EditText nameIn;
	private EditText pass1In;
	private EditText pass2In;
	private TextView nameLbl;
	
	private View root;

	public void settype(int type)
	{
		this.type = type;
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		root = inflater.inflate(R.layout.fragment_createfile, container, false);
		nameLbl = (TextView) root.findViewById(R.id.createfile_title);
		okayBtn = (Button) root.findViewById(R.id.createfile_okaybtn);
		pass1In = (EditText) root.findViewById(R.id.createfile_password1ipt);
		pass2In = (EditText) root.findViewById(R.id.createfile_password2ipt);

		okayBtn.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
		{
			submit();
		}});

		return root;
	}

	private void submit()
	{
		boolean valid = true;
		String message = "";
		

		String pass1 = pass1In.getText().toString();
		
		if (pass1.length() < 3)
		{
			valid = false;
			message += "password length is too short\n";
		}

		String pass2 = pass2In.getText().toString();
		
		if (!pass1.equals(pass2))
		{
			valid = false;
			message += "passwords must match\n";
		}

		if (!valid)
		{
			AlertFragment frag = new AlertFragment(message);
			frag.show(MainActivity.getInstance().getSupportFragmentManager(), "invalid_fragment");
		}
		else
		{
			MainActivity.getInstance().document.setPassword(pass1);

			try
			{
				MainActivity.getInstance().document.save();
			}
			catch(Exception e){
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setMessage(message);
				builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which)
				{
					System.exit(0);
				}});
				builder.create();
				return;
			}

			MainActivity.getInstance().onBackPressed();
			MainActivity.getInstance().openFile();
		}
	}
}
