package com.munger.passwordkeeper.helpers;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.MainState;

public class DriveHelper implements GoogleApiClient.OnConnectionFailedListener
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
        if (mGoogleApiClient == null)
        {
            mGoogleApiClient = new GoogleApiClient.Builder(MainState.getInstance().context)
                    .enableAutoManage(MainState.getInstance().activity, this)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .build();
        }

        mGoogleApiClient.connect();
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

    @Override
    public void onConnectionFailed(ConnectionResult result)
    {
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