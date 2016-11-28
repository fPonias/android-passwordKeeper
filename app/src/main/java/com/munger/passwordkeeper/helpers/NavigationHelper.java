package com.munger.passwordkeeper.helpers;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.MenuItem;

import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.alert.AlertFragment;
import com.munger.passwordkeeper.alert.InputFragment;
import com.munger.passwordkeeper.alert.PasswordFragment;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFileImport;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;
import com.munger.passwordkeeper.view.AboutFragment;
import com.munger.passwordkeeper.view.CreateFileFragment;
import com.munger.passwordkeeper.view.SettingsFragment;
import com.munger.passwordkeeper.view.ViewDetailFragment;
import com.munger.passwordkeeper.view.ViewFileFragment;

import java.util.ArrayList;

/**
 * Created by codymunger on 11/26/16.
 */

public class NavigationHelper
{
    private CreateFileFragment createFileFragment;
    private ViewFileFragment viewFileFragment;
    private ViewDetailFragment viewDetailFragment;
    private SettingsFragment settingsFragment;

    private boolean editable;

    public NavigationHelper ()
    {
    }

    public void openFileView()
    {
        FragmentTransaction trans = MainState.getInstance().activity.getSupportFragmentManager().beginTransaction();
        viewFileFragment = new ViewFileFragment();

        setEditable(false);

        viewFileFragment.setDocument(MainState.getInstance().document);

        trans.replace(R.id.container, viewFileFragment);
        trans.addToBackStack(ViewFileFragment.getName());
        trans.commit();
    }

    public void openInitialView()
    {
        PasswordDocumentFile document = (PasswordDocumentFile) MainState.getInstance().document;
        if (!document.exists())
        {
            createFileFragment = new CreateFileFragment();
            MainState.getInstance().activity.getSupportFragmentManager().beginTransaction().add(R.id.container, createFileFragment).commit();
        }
        else
        {
            startGetPassword();
        }
    }

    protected boolean gettingPassword = false;

    protected void startGetPassword()
    {
        gettingPassword = true;
        AsyncTask t = new AsyncTask() {protected Object doInBackground(Object[] params)
        {
            getPassword();
            return null;
        }};
        t.execute(new Object(){});
    }

    protected void getPassword()
    {
        final PasswordFragment inDialog = new PasswordFragment("Input the document password", "password", new PasswordFragment.Listener()
        {
            public boolean okay(InputFragment that, String password)
            {
                PasswordDocument document = MainState.getInstance().document;
                document.setPassword(password);
                boolean passed = document.testPassword();

                if (!passed)
                {
                    that.dismiss();

                    AlertFragment frag = new AlertFragment("Incorrect password.");
                    frag.show(MainState.getInstance().activity.getSupportFragmentManager(), "invalid_fragment");
                    frag.setCloseCallback(new AlertFragment.CloseCallback() {public void closed()
                    {
                        startGetPassword();
                    }});

                    return false;
                }
                else
                {
                    gettingPassword = false;
                    MainState.getInstance().password = password;
                    openFile();
                    return true;
                }
            }

            public void cancel(InputFragment that)
            {
                System.exit(0);
            }
        });

        inDialog.show(MainState.getInstance().activity.getSupportFragmentManager(), "invalid_fragment");
    }


    public boolean getEditable()
    {
        return editable;
    }

    public void setEditable(boolean editable)
    {
        this.editable = editable;

        if (viewFileFragment != null)
            viewFileFragment.setEditable(editable);
        if (viewDetailFragment != null)
            viewDetailFragment.setEditable(editable);
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.action_edit)
        {
            setEditable(!editable);
            return true;
        }
        else if (id == R.id.action_settings)
        {
            openSettings();
            return true;
        }

        return false;
    };

    public void openSettings()
    {
        FragmentTransaction trans = MainState.getInstance().activity.getSupportFragmentManager().beginTransaction();
        settingsFragment = new SettingsFragment();

        trans.replace(R.id.container, settingsFragment);
        trans.addToBackStack(SettingsFragment.getName());
        trans.commit();
    }

    public void openDetail(PasswordDetails detail)
    {
        MainState.getInstance().details = detail;
        FragmentTransaction trans = MainState.getInstance().activity.getSupportFragmentManager().beginTransaction();

        viewDetailFragment = new ViewDetailFragment();
        viewDetailFragment.setDetails(detail);
        viewDetailFragment.setEditable(editable);

        trans.replace(R.id.container, viewDetailFragment);
        trans.addToBackStack(ViewDetailFragment.getName());
        trans.commit();

    }

    public void about()
    {
        FragmentTransaction trans = MainState.getInstance().activity.getSupportFragmentManager().beginTransaction();
        AboutFragment frag = new AboutFragment();

        trans.replace(R.id.container, frag);
        trans.addToBackStack(AboutFragment.getName());
        trans.commit();
    }

    public boolean onBackPressed()
    {
        FragmentManager mgr = MainState.getInstance().activity.getSupportFragmentManager();
        int cnt = mgr.getBackStackEntryCount();

        if (cnt > 0)
        {
            FragmentManager.BackStackEntry entry = mgr.getBackStackEntryAt(cnt - 1);
            String name = entry.getName();

            boolean keepGoing = true;

            if (name.equals(ViewDetailFragment.getName()))
            {
                keepGoing = viewDetailFragment.backPressed();
            }

            if (!keepGoing)
                return false;

            if (cnt > 1)
            {
                return true;
            }
        }

        return true;
    }

    public void fragmentExists(Fragment frag)
    {
        if (frag instanceof ViewDetailFragment)
            viewDetailFragment = (ViewDetailFragment) frag;
        else if (frag instanceof ViewFileFragment)
            viewFileFragment = (ViewFileFragment) frag;
        else if (frag instanceof CreateFileFragment)
            createFileFragment = (CreateFileFragment) frag;
        else if (frag instanceof SettingsFragment)
            settingsFragment = (SettingsFragment) frag;
    }

    public void reset()
    {
        try
        {
            MainState.getInstance().document.close();
        }
        catch(Exception e){}

        if (MainState.getInstance().isActive)
        {
            if (gettingPassword)
                System.exit(0);
            else
            {
                Intent i = new Intent(MainState.getInstance().context, MainActivity.class);
                i.putExtra("reset", true);
                MainState.getInstance().context.startActivity(i);
            }
        }
        else
        {
            System.exit(0);
        }
    }

    public void showAlert(String message)
    {
        AlertFragment frag = new AlertFragment(message);
        frag.show(MainState.getInstance().activity.getSupportFragmentManager(), "invalid_fragment");
    }

    public void openFile()
    {
        final ProgressDialog loadingDialog = new ProgressDialog(MainState.getInstance().context);
        loadingDialog.setMessage("Decrypting password data");
        loadingDialog.show();

        final PasswordDocument.ILoadEvents listener = new PasswordDocumentFile.ILoadEvents() {
            @Override
            public void detailsLoaded()
            {
                MainState.getInstance().handler.post(new Runnable() {public void run()
                {
                    if (loadingDialog.isShowing())
                        loadingDialog.dismiss();

                    openFile2();
                }});
            }

            @Override
            public void historyLoaded()
            {
                MainState.getInstance().setupDriveHelper();
            }

            @Override
            public void historyProgress(float progress) {

            }
        };
        MainState.getInstance().document.addLoadEvents(listener);

        AsyncTask t = new AsyncTask()
        {
            protected Object doInBackground(Object[] params)
            {
                try
                {
                    MainState.getInstance().document.load(true);
                }
                catch(Exception e){
                    showAlert("Failed to open the document: " + MainState.getInstance().document.name);
                }

                return null;
            }

            protected void onPostExecute(Object o)
            {
                if (loadingDialog.isShowing())
                    loadingDialog.dismiss();

                MainState.getInstance().document.removeLoadEvents(listener);
            }
        };

        t.execute(new Object[]{});
    }

    private void openFile2()
    {
        if (MainState.getInstance().document.count() == 0)
        {
            PasswordDetails dets = new PasswordDetails();
            try{MainState.getInstance().document.addDetails(dets);}catch(Exception e){}
        }

        openFileView();
    }

    public void importFile(final String path, final Callback callback)
    {
        final ProgressDialog loadingDialog = new ProgressDialog(MainState.getInstance().context);
        loadingDialog.setMessage("Importing password data");
        loadingDialog.show();

        AsyncTask t = new AsyncTask()
        {
            protected Object doInBackground(Object[] params)
            {
                try
                {
                    PasswordDocumentFileImport fileImport = new PasswordDocumentFileImport(path, "import");
                    fileImport.load(false);
                    MainState.getInstance().document.playSubHistory(fileImport.getHistory());
                    MainState.getInstance().document.save();
                }
                catch(Exception e){
                    showAlert("Failed to import the document: " + path);
                    return false;
                }

                return true;
            }

            protected void onPostExecute(Object o)
            {
                loadingDialog.dismiss();

                showAlert("Successfully imported!");

                callback.callback(o);
            }
        };
        t.execute(new Object[]{});
    }

    public void deleteData()
    {
        try
        {
            MainState.getInstance().document.delete();
        }
        catch(Exception e){
            showAlert("Failed to delete local password data");
            return;
        }

        setPasswordFile();
    }

    protected void setPasswordFile()
    {
        MainState.getInstance().document = new PasswordDocumentFile(MainState.getInstance().config.localDataFilePath);
        openInitialView();
    }

    public void deleteRemoteData()
    {

    }

    public void setFile(String password)
    {
        MainState.getInstance().password = password;
        MainState.getInstance().document.setPassword(password);
        openFile();
    }

    public void removeFile()
    {
        try
        {
            MainState.getInstance().document.delete();
        }
        catch(Exception e){
            showAlert("Failed to delete the document: " + MainState.getInstance().document.name);
        }
    }

    public interface Callback
    {
        void callback(Object result);
    }

    public void saveDetail(final PasswordDetails detail, final Callback callback)
    {
        final ProgressDialog loadingDialog = new ProgressDialog(MainState.getInstance().context);
        loadingDialog.setMessage("Saving password data");
        loadingDialog.show();

        AsyncTask t = new AsyncTask() {
            protected Object doInBackground(Object[] params)
            {
                try
                {
                    MainState.getInstance().document.replaceDetails(detail);
                    MainState.getInstance().document.save();
                    detail.setHistory(new PasswordDocumentHistory());
                }
                catch(Exception e){
                    Log.e("password", "failed to update password file");
                }

                return null;
            }

            @Override
            protected void onPostExecute(Object o)
            {
                loadingDialog.dismiss();
                callback.callback(null);
            }
        };
        t.execute(new Object(){});
    }

    public void notifyPermissionResults(int requestCode)
    {
        for(IPermissionResult listener : permissionResults)
            listener.result(requestCode);

        permissionResults = new ArrayList<>();
    }

    public interface IPermissionResult
    {
        void result(int requestCode);
    }

    private ArrayList<IPermissionResult> permissionResults = new ArrayList<>();

    public void addPermisionResultListener(IPermissionResult listener)
    {
        permissionResults.add(listener);
    }
}
