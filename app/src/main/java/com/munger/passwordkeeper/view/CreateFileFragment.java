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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;

import androidx.fragment.app.Fragment;

public class CreateFileFragment extends Fragment
{
	public CreateFileFragment()
	{
		super();
	}

	public static String getName()
	{
		return "Create";
	}


	private Button okayBtn;
	private EditText oldpassIn;
	private EditText pass1In;
	private EditText pass2In;

	private TextView titleLbl;
	private TextView subtitleLbl;

	public static final int MIN_PASSWORD_LENGTH = 3;
	public static final int MAX_PASSWORD_LENGTH = 40;
	
	private View root = null;

	private boolean isCreating = true;

	public boolean getIsCreating()
	{
		return isCreating;
	}

	public void setIsCreating(boolean value)
	{
		isCreating = value;

		if (root == null)
			return;

		if (!isCreating)
		{
			oldpassIn.setVisibility(View.VISIBLE);
			titleLbl.setText("Changing current password");
			subtitleLbl.setVisibility(View.VISIBLE);
		}
		else
		{
			oldpassIn.setVisibility(View.GONE);
			titleLbl.setText("Welcome to Password Crypt.  Please set your master password.");
			subtitleLbl.setVisibility(View.GONE);
		}
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		root = inflater.inflate(R.layout.fragment_createfile, container, false);

		okayBtn = (Button) root.findViewById(R.id.createfile_okaybtn);
		pass1In = (EditText) root.findViewById(R.id.createfile_password1ipt);
		pass2In = (EditText) root.findViewById(R.id.createfile_password2ipt);
		oldpassIn = (EditText) root.findViewById(R.id.createfile_oldpasswordipt);

		titleLbl = (TextView) root.findViewById(R.id.createfile_title);
		subtitleLbl = (TextView) root.findViewById(R.id.createfile_subtitle);

		okayBtn.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
		{
			submit();
		}});

		setIsCreating(isCreating);

		oldpassIn.requestFocus();
		MainState.getInstance().keyboardListener.forceOpenKeyboard(oldpassIn);

		return root;
	}

	private void submit()
	{
		boolean valid = validate();
		if (!valid)
			return;

		valid = saveFile();

		if (!valid)
			return;

		if (submittedListener != null)
			submittedListener.submitted();
	}

	protected boolean validate()
	{
		if (!isCreating)
		{
			boolean result = validateOldPassword();

			if (!result)
				return false;
		}

		String pass1 = pass1In.getText().toString();

		if (pass1.length() < MIN_PASSWORD_LENGTH)
		{
			showError("password length is too short");
			return false;
		}

		if (pass1.length() > MAX_PASSWORD_LENGTH)
		{
			showError("massword length is too long");
			return false;
		}

		String pass2 = pass2In.getText().toString();

		if (!pass1.equals(pass2))
		{
			showError("passwords must match");
			return false;
		}

		return true;
	}

	protected boolean validateOldPassword()
	{
		String oldPass = oldpassIn.getText().toString();

		PasswordDocument oldDoc = MainState.getInstance().document;
		MainState state = MainState.getInstance();
		state.setupDocument();

		boolean testResult = state.document.testPassword(oldPass);

		if (!testResult)
		{
			showError("incorrect current password");
			return false;
		}

		state.document = oldDoc;


		return true;
	}

	protected boolean saveFile()
	{
		String pass1 = pass1In.getText().toString();

		try
		{
			MainState.getInstance().document.setPassword(pass1);
			MainState.getInstance().document.save();
		}
		catch(Exception e) {
			showError("failed to save new password file");
			return false;
		}

		return true;
	}

	protected void showError(String message)
	{
		MainState.getInstance().navigationHelper.showAlert(message);
	}

	public interface ISubmittedListener
	{
		public void submitted();
	}

	public ISubmittedListener submittedListener = null;
}
