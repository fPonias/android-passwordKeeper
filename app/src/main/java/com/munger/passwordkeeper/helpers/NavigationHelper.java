package com.munger.passwordkeeper.helpers;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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
import com.munger.passwordkeeper.view.AboutFragment;
import com.munger.passwordkeeper.view.CreateFileFragment;
import com.munger.passwordkeeper.view.SettingsFragment;
import com.munger.passwordkeeper.view.ViewDetailFragment;
import com.munger.passwordkeeper.view.ViewFileFragment;

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
        FragmentTransaction trans = MainState.getInstance().mainActivity.getSupportFragmentManager().beginTransaction();
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
            MainState.getInstance().mainActivity.getSupportFragmentManager().beginTransaction().add(R.id.container, createFileFragment).commit();
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
                    frag.show(MainActivity.getInstance().getSupportFragmentManager(), "invalid_fragment");
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
                    MainState.getInstance().openFile();
                    return true;
                }
            }

            public void cancel(InputFragment that)
            {
                System.exit(0);
            }
        });

        inDialog.show(MainState.getInstance().mainActivity.getSupportFragmentManager(), "invalid_fragment");
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
        FragmentTransaction trans = MainState.getInstance().mainActivity.getSupportFragmentManager().beginTransaction();
        settingsFragment = new SettingsFragment();

        trans.replace(R.id.container, settingsFragment);
        trans.addToBackStack(SettingsFragment.getName());
        trans.commit();
    }

    public void openDetail(PasswordDetails detail)
    {
        MainState.getInstance().setDetails(detail);
        FragmentTransaction trans = MainState.getInstance().mainActivity.getSupportFragmentManager().beginTransaction();

        viewDetailFragment = new ViewDetailFragment();
        viewDetailFragment.setDetails(detail);
        viewDetailFragment.setEditable(editable);

        trans.replace(R.id.container, viewDetailFragment);
        trans.addToBackStack(ViewDetailFragment.getName());
        trans.commit();

    }

    public void about()
    {
        FragmentTransaction trans = MainState.getInstance().mainActivity.getSupportFragmentManager().beginTransaction();
        AboutFragment frag = new AboutFragment();

        trans.replace(R.id.container, frag);
        trans.addToBackStack(AboutFragment.getName());
        trans.commit();
    }

    public boolean onBackPressed()
    {
        FragmentManager mgr = MainState.getInstance().mainActivity.getSupportFragmentManager();
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
                Intent i = new Intent(MainState.getInstance().mainActivity, MainActivity.class);
                i.putExtra("reset", true);
                MainState.getInstance().mainActivity.startActivity(i);
            }
        }
        else
        {
            System.exit(0);
        }
    }
}
