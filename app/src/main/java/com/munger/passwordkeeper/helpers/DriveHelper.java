package com.munger.passwordkeeper.helpers;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.MainState;

public class DriveHelper implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks
{
    private GoogleApiClient mGoogleApiClient;

    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final String DIALOG_ERROR = "dialog_error";
    private boolean mResolvingError = false;

    public DriveHelper()
    {
    }

    public GoogleApiClient connect()
    {
        synchronized (lock)
        {
            if (connected != null)
                return mGoogleApiClient;

            connecting = true;
        }

        if (mGoogleApiClient == null)
        {
            mGoogleApiClient = new GoogleApiClient.Builder(MainState.getInstance().context)
                    .enableAutoManage(MainState.getInstance().activity, this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .build();
        }

        mGoogleApiClient.connect();

        synchronized (lock)
        {
            if (mGoogleApiClient.isConnected())
            {
                onConnected(null);
            }
        }

        return mGoogleApiClient;
    }

    public GoogleApiClient getClient()
    {
        return mGoogleApiClient;
    }

    public void cleanUp()
    {
        if (mGoogleApiClient == null)
            return;

        mGoogleApiClient.disconnect();
    }

    private Object lock = new Object();
    private boolean connecting = false;
    private Boolean connected = null;

    public Boolean isConnected()
    {
        return connected;
    }

    public void awaitConnection()
    {
        synchronized (lock)
        {
            if (connected != null)
                return;

            try{lock.wait();}catch(InterruptedException e){return;}
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        synchronized (lock)
        {
            connected = true;
            connecting = false;

            lock.notify();
        }
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        synchronized (lock)
        {
            connected = false;
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result)
    {
        synchronized (lock)
        {
            connected = false;
            connecting = false;

            lock.notify();
        }

        Log.v("password", "Google API connection failed");
        showErrorDialog(result.getErrorCode());
        mResolvingError = true;
    }

    private void showErrorDialog(int errorCode)
    {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment().setParent(this);
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(MainState.getInstance().activity.getSupportFragmentManager(), "errordialog");
    }

    public void onDialogDismissed()
    {
        mResolvingError = false;
    }

    public static class ErrorDialogFragment extends DialogFragment
    {
        public ErrorDialogFragment() { }

        private DriveHelper parent;

        public ErrorDialogFragment setParent(DriveHelper parent)
        {
            this.parent = parent;
            return this;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState)
        {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog)
        {
            parent.onDialogDismissed();
        }
    }
}