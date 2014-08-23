package com.munger.passwordkeeper.view;

import java.io.IOException;

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
import com.munger.passwordkeeper.struct.PasswordDocument;

/**
 * User interface used to gather information in creating a new passwordDocument
 */
public class CreateFileFragment extends Fragment
{
	/** Activity to get variables from and trigger Activity events */ private MainActivity parent;
	
	public CreateFileFragment()
	{
		type = TYPE_CREATE;
	}
	
	/**
	 * The tag this fragment is supposed to use.
	 * @return
	 */
	public static String getName()
	{
		return "Create";
	}
	
	public void setEditable(boolean editable)
	{}
	
	/** Create a new document */ public static final int TYPE_CREATE = 1;
	/** Import the new document */ public static final int TYPE_IMPORT = 2;
	
	/**
	 * Type TYPE_CREATE or TYPE_IMPORT?
	 */
	private int type;
	
	
	/** button used to quit this fragment */ private Button cancelBtn;
	/** button used to submit the information in this fragment */ private Button okayBtn;
	/** input for the filename where the new document is saved */ private EditText nameIn;
	/** input for the document password */ private EditText pass1In;
	/** input to verify the document password */ private EditText pass2In;
	/** The title text label */ private TextView nameLbl;
	
	/** the view that contains all of the views in this fragment */ private View root;
	
	/**
	 * Set the view to be in create mode or import mode
	 * @param type
	 */
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
	
	/**
	 * Once the view is created, get references for any interactive views and setup default event listeners.
	 */
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		this.parent = (MainActivity) getActivity();
		
		//grab references to all important views
		root = inflater.inflate(R.layout.fragment_createfile, container, false);
		nameLbl = (TextView) root.findViewById(R.id.createfile_title);
		cancelBtn = (Button) root.findViewById(R.id.createfile_cancelbtn);
		okayBtn = (Button) root.findViewById(R.id.createfile_okaybtn);
		nameIn = (EditText) root.findViewById(R.id.createfile_nameipt);
		pass1In = (EditText) root.findViewById(R.id.createfile_password1ipt);
		pass2In = (EditText) root.findViewById(R.id.createfile_password2ipt);
		
		
		//setup button listeners
		cancelBtn.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
		{
			parent.onBackPressed();
		}});
		
		okayBtn.setOnClickListener(new View.OnClickListener() {public void onClick(View v) 
		{
			submit();
		}});
		
		//setup CREATE or IMPORT views
		setupView();
		
		return root;
	}
	
	/**
	 * switch the UI to reflect CREATE or IMPORT mode.
	 */
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
	
	/**
	 * Validate the form inputs and either create or import the new document
	 */
	private void submit()
	{
		boolean valid = true;
		String message = "";
		
		//is the document name valid?
		String name = nameIn.getText().toString().trim();
		
		if (name.length() == 0)
		{
			valid = false;
			message += "invalid name was provided\n";
		}
		
		
		//is the password valid?
		String pass1 = pass1In.getText().toString();
		
		if (pass1.length() < 3)
		{
			valid = false;
			message += "password length is too short\n";
		}
		
		//does the other password match?
		String pass2 = pass2In.getText().toString();
		
		if (!pass1.equals(pass2))
		{
			valid = false;
			message += "passwords must match\n";
		}
		
		//did the form validate?
		if (!valid)
		{
			AlertFragment frag = new AlertFragment(message);
			frag.show(parent.getSupportFragmentManager(), "invalid_fragment");
		}
		//finally create or import the document
		else
		{
			PasswordDocument doc = null;
			if (type == TYPE_CREATE)
				doc = new PasswordDocument(parent, name, PasswordDocument.Type.FILE, pass1);
			else if (type == TYPE_IMPORT)
			{
				doc = parent.document;
				doc.name = name;
				doc.setPassword(pass1);
			}
			
			//save the new document and tell the activity to open it
			try
			{
				doc.saveToFile();
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
