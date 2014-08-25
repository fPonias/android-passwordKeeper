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
