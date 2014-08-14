package com.munger.passwordkeeper;

import java.io.IOException;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.munger.passwordkeeper.alert.AlertFragment;
import com.munger.passwordkeeper.util.PasswordDocument;

public class CreateFileFragment extends Fragment 
{
	private MainActivity parent;
	
	public CreateFileFragment()
	{
		type = TYPE_CREATE;
	}
	
	public static String getName()
	{
		return "Create";
	}
	
	public static final int TYPE_CREATE = 1;
	public static final int TYPE_IMPORT = 2;
	
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
		if (type != TYPE_CREATE && type != TYPE_IMPORT)
		{
			return;
		}
		
		this.type = type;
		
		if (root != null)
			setupView();
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		this.parent = (MainActivity) getActivity();
		
		setHasOptionsMenu(true);
		
		root = inflater.inflate(R.layout.fragment_createfile, container, false);
		nameLbl = (TextView) root.findViewById(R.id.createfile_title);
		cancelBtn = (Button) root.findViewById(R.id.createfile_cancelbtn);
		okayBtn = (Button) root.findViewById(R.id.createfile_okaybtn);
		nameIn = (EditText) root.findViewById(R.id.createfile_nameipt);
		pass1In = (EditText) root.findViewById(R.id.createfile_password1ipt);
		pass2In = (EditText) root.findViewById(R.id.createfile_password2ipt);
		
		
		cancelBtn.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
		{
			parent.onBackPressed();
		}});
		
		okayBtn.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
		{
			submit();
		}});
		
		setupView();
		
		return root;
	}
	
	@Override
	public void onCreateOptionsMenu(android.view.Menu menu, android.view.MenuInflater inflater) 
	{
		int sz = menu.size();
		
		for (int i = 0; i < sz; i++)
		{
			MenuItem item = menu.getItem(0);
			menu.removeItem(item.getItemId());
		}
	};
	
	public void setupView()
	{
		if (type == TYPE_CREATE)
		{
			nameLbl.setText("Document creation details");
		}
		else
		{
			nameLbl.setText("Document import details");
		}
	}
	
	private void submit()
	{
		boolean valid = true;
		String message = "";
		
		String name = nameIn.getText().toString().trim();
		
		if (name.length() == 0)
		{
			valid = false;
			message += "invalid name was provided\n";
		}
		
		
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
			frag.show(parent.getSupportFragmentManager(), "invalid_fragment");
		}
		else
		{
			PasswordDocument doc = null;
			if (type == TYPE_CREATE)
				doc = new PasswordDocument(parent, pass1);
			else if (type == TYPE_IMPORT)
			{
				doc = parent.document;
				doc.setPassword(pass1);
			}
			
			try
			{
				doc.saveToFile(name);
				parent.onBackPressed();
				parent.openFile(name);
			}
			catch(IOException e){
				AlertFragment frag = new AlertFragment("failed to create new password file " + name);
				frag.show(parent.getSupportFragmentManager(), "invalid_fragment");
			}
		}
	}
}
