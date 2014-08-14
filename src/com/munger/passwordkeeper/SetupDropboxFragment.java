package com.munger.passwordkeeper;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

public class SetupDropboxFragment extends Fragment 
{
	final static private String APP_KEY = "5xyvkb536ur7sue";
	final static private String APP_SECRET = "w6wthm9amap6xo4";
	final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
	
	private DropboxAPI<AndroidAuthSession> mDBApi;
	private Context context;
	
	public SetupDropboxFragment(Context c) 
	{
		this.context = c;
		init();
	}
	
	@SuppressWarnings("deprecation")
	private void init()
	{
		AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
		AndroidAuthSession session = new AndroidAuthSession(appKeys, ACCESS_TYPE);
		mDBApi = new DropboxAPI<AndroidAuthSession>(session);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		View rootView = inflater.inflate(R.layout.fragment_main, container, false);
		
		mDBApi.getSession().startOAuth2Authentication(context);
		
		return rootView;
	}
	
	public void onResume() 
	{
	    super.onResume();

	    if (mDBApi.getSession().authenticationSuccessful()) 
	    {
	        try 
	        {
	            // Required to complete auth, sets the access token on the session
	            mDBApi.getSession().finishAuthentication();

	            String accessToken = mDBApi.getSession().getOAuth2AccessToken();
	        } 
	        catch (IllegalStateException e) {
	            Log.i("DbAuthLog", "Error authenticating", e);
	        }
	    }
	}
}
