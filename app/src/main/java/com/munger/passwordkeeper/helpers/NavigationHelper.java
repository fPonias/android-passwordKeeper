package com.munger.passwordkeeper.helpers;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.munger.passwordkeeper.MainActivity;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.alert.AlertFragment;
import com.munger.passwordkeeper.alert.InputFragment;
import com.munger.passwordkeeper.alert.PasswordFragment;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentDrive;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFileImport;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;
import com.munger.passwordkeeper.view.AboutFragment;
import com.munger.passwordkeeper.view.CreateFileFragment;
import com.munger.passwordkeeper.view.SettingsFragment;
import com.munger.passwordkeeper.view.ViewDetailFragment;
import com.munger.passwordkeeper.view.ViewFileFragment;

import org.mortbay.jetty.Main;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

/**
 * Created by codymunger on 11/26/16.
 */

public class NavigationHelper
{
    protected CreateFileFragment createFileFragment;
    protected ViewFileFragment viewFileFragment;
    protected ViewDetailFragment viewDetailFragment;
    protected SettingsFragment settingsFragment;

    protected boolean editable;

    public NavigationHelper ()
    {
    }

    public void openFileView()
    {
        FragmentTransaction trans = MainState.getInstance().activity.getSupportFragmentManager().beginTransaction();
        viewFileFragment = new ViewFileFragment();

        setEditable(false);

        viewFileFragment.setDocument(MainState.getInstance().document);

        trans.replace(R.id.container, viewFileFragment, ViewFileFragment.getName());
        trans.addToBackStack(ViewFileFragment.getName());
        trans.commit();
    }

    public CreateFileFragment openCreateFileFragment(boolean isCreating)
    {
        FragmentTransaction trans = MainState.getInstance().activity.getSupportFragmentManager().beginTransaction();
        CreateFileFragment frag = new CreateFileFragment();
        frag.setIsCreating(isCreating);

        trans.replace(R.id.container, frag, CreateFileFragment.getName());
        trans.addToBackStack(CreateFileFragment.getName());
        trans.commit();

        return frag;
    }

    public static String getRootPath()
    {
        return MainState.getInstance().context.getFilesDir().getAbsolutePath() + "/";
    }

    public void openInitialView()
    {
        FragmentTransaction trans = MainState.getInstance().activity.getSupportFragmentManager().beginTransaction();

        PasswordDocumentFile document = (PasswordDocumentFile) MainState.getInstance().document;
        document.setRootPath(getRootPath());
        if (!document.exists())
        {
            createFileFragment = openCreateFileFragment(true);
            createFileFragment.submittedListener = new CreateFileFragment.ISubmittedListener() {public void submitted()
            {
                onBackPressed(new NavigationHelper.Callback() {public void callback(Object result)
                {
                    openFile();
                }});
            }};
        }
        else
        {
            startGetPassword();
        }
    }

    public void changePassword(CreateFileFragment.ISubmittedListener listener)
    {
        CreateFileFragment frag = openCreateFileFragment(false);
        frag.submittedListener = listener;
    }

    public void changePassword()
    {
        CreateFileFragment.ISubmittedListener listener = new CreateFileFragment.ISubmittedListener() {public void submitted()
        {
            ((MainActivity) MainState.getInstance().activity).realOnBackPressed();
        }};

        changePassword(listener);
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
                boolean passed;

                try
                {
                    passed = document.testPassword(password);
                }
                catch(Exception e){
                    passed = false;
                }

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
                    document.setPassword(password);
                    MainState.getInstance().password = password;
                    openFile();
                    return true;
                }
            }

            public void cancel(InputFragment that)
            {
                doExit();
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

        if (!editable)
            MainState.getInstance().keyboardListener.forceCloseKeyboard();
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

        trans.replace(R.id.container, settingsFragment, SettingsFragment.getName());
        trans.addToBackStack(SettingsFragment.getName());
        trans.commit();
    }

    public void openDetail(PasswordDetails detail)
    {
        MainState.getInstance().details = detail;
        FragmentTransaction trans = MainState.getInstance().activity.getSupportFragmentManager().beginTransaction();

        viewDetailFragment = new ViewDetailFragment(editable);
        viewDetailFragment.setDetails(detail);

        trans.replace(R.id.container, viewDetailFragment, ViewDetailFragment.getName());
        trans.addToBackStack(ViewDetailFragment.getName());
        trans.commit();

    }

    public void about()
    {
        FragmentTransaction trans = MainState.getInstance().activity.getSupportFragmentManager().beginTransaction();
        AboutFragment frag = new AboutFragment();

        trans.replace(R.id.container, frag, AboutFragment.getName());
        trans.addToBackStack(AboutFragment.getName());
        trans.commit();
    }

    List<WeakReference<Fragment>> fragList = new ArrayList<>();

    public void fragmentAttached(Fragment frag)
    {
        int sz = fragList.size();
        for(int i = sz - 1; i >= 0; i--)
        {
            WeakReference<Fragment> ref = fragList.get(i);
            Fragment f = ref.get();
            if (f == null || (f != null && f.isDetached()))
                fragList.remove(i);
        }

        fragList.add(new WeakReference<Fragment>(frag));
    }

    public List<Fragment> getActiveFragments()
    {
        ArrayList<Fragment> ret = new ArrayList<>();
        for(WeakReference<Fragment> ref : fragList)
        {
            Fragment f = ref.get();
            if (f != null && f.isVisible())
                ret.add(f);
        }

        return ret;
    }

    public void onBackPressed(final Callback cb)
    {
        FragmentManager mgr = MainState.getInstance().activity.getSupportFragmentManager();
        List<Fragment> fragments = getActiveFragments();
        int sz = fragments.size();
        if (sz > 0)
        {
            Fragment f = fragments.get(sz - 1);
            boolean keepGoing = true;

            if (f instanceof ViewDetailFragment)
            {
                ((ViewDetailFragment) f).backPressed(new Callback() {public void callback(Object result)
                {
                    if (cb != null)
                        cb.callback(result);
                }});
            }
            else if (!(f instanceof ViewFileFragment))
            {
                if (cb != null)
                    cb.callback(true);
            }
        }

        if (cb != null)
            cb.callback(false);
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
            if (!gettingPassword)
            {
                Intent i = new Intent(MainState.getInstance().context, MainActivity.class);
                i.putExtra("reset", true);
                MainState.getInstance().context.startActivity(i);
            }
        }
        else
        {
            doExit();
        }
    }

    protected void doExit()
    {
        ((MainActivity)MainState.getInstance().activity).doexit();
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

        final DialogInterface.OnClickListener pwListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                if (which == -1) //password change
                {
                    CreateFileFragment frag = openCreateFileFragment(false);
                    frag.submittedListener = new CreateFileFragment.ISubmittedListener() {public void submitted()
                    {
                        onBackPressed(new Callback() {public void callback(Object result)
                        {
                            openFile2();
                        }});
                    }};
                }
                else if (which == -2) //remote overwrite
                {
                    AsyncTask t = new AsyncTask()
                    {
                        protected Object doInBackground(Object[] params)
                        {
                            try
                            {
                                MainState.getInstance().driveDocument.overwrite();
                            }
                            catch(Exception e){}

                            return null;
                        }

                        protected void onPostExecute(Object o)
                        {
                            openFile2();
                        }
                    };

                    t.execute(new Object[]{});

                }
                else // which == -3 . turn off remote sync
                {
                    PreferenceManager mgr = new PreferenceManager(MainState.getInstance().context);
                    mgr.getSharedPreferences().edit().putBoolean(SettingsFragment.PREF_NAME_SAVE_TO_CLOUD, false).apply();
                    MainState.getInstance().cleanUpDriveHelper();

                    openFile2();
                }
            }
        };

        final DialogInterface.OnClickListener trashedListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                if (which == -1) //untrash
                {
                    try
                    {
                        MainState.getInstance().driveDocument.undelete();
                        openFile2();
                    }
                    catch(Exception e){}
                }
                else //disable
                {
                    PreferenceManager mgr = new PreferenceManager(MainState.getInstance().context);
                    mgr.getSharedPreferences().edit().putBoolean(SettingsFragment.PREF_NAME_SAVE_TO_CLOUD, false).apply();
                    MainState.getInstance().cleanUpDriveHelper();

                    openFile2();
                }
            }
        };

        final PasswordDocument.ILoadEvents listener = new PasswordDocumentFile.ILoadEvents() {
            @Override
            public void detailsLoaded()
            {
                MainState.getInstance().handler.post(new Runnable() {public void run()
                {
                    if (loadingDialog.isShowing())
                        loadingDialog.dismiss();

                    MainState mainState = MainState.getInstance();
                    Boolean isConn = mainState.driveHelper.isConnected();
                    if (isConn == null || isConn == false)
                    {
                        openFile2();
                        return;
                    }

                    Exception e = mainState.driveDocument.getInitException();
                    if (e == null)
                    {
                        openFile2();
                        return;
                    }

                    if (e instanceof PasswordDocument.IncorrectPasswordException)
                    {
                        final AlertDialog remotePasswordDialog = new AlertDialog.Builder(MainState.getInstance().context)
                            .setMessage(R.string.nav_mismatched_remote_pw_message)
                            .setPositiveButton(R.string.nav_mismatched_remote_pw_change_local, pwListener)
                            .setNegativeButton(R.string.nav_mismatched_remote_pw_overwrite, pwListener)
                            .setNeutralButton(R.string.nav_mismatched_remote_pw_disable, pwListener)
                            .create();

                        remotePasswordDialog.show();
                    }
                    else if (e instanceof PasswordDocumentDrive.TrashedFileException)
                    {
                        final AlertDialog remotePasswordDialog = new AlertDialog.Builder(MainState.getInstance().context)
                                .setMessage(R.string.nav_trashed_remote_message)
                                .setPositiveButton(R.string.nav_trashed_remote_untrash, trashedListener)
                                .setNegativeButton(R.string.nav_trashed_remote_disable, trashedListener)
                                .create();

                        remotePasswordDialog.show();
                    }
                    else
                    {
                        openFile2();
                    }
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
        MainState.getInstance().handler.post(new Runnable() {public void run()
        {
            if (MainState.getInstance().document.count() == 0)
            {
                PasswordDetails dets = new PasswordDetails();
                try{MainState.getInstance().document.addDetails(dets);}catch(Exception e){}
            }

            openFileView();
        }});
    }

    private boolean importing = false;
    private boolean importSuccess = false;
    private Object importLock = new Object();

    public void importFile(final String path, final Callback callback)
    {
        synchronized (importLock)
        {
            if (importing)
                return;

            importing = true;
            importSuccess = false;
        }

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
                    PasswordDocument doc =  MainState.getInstance().document;
                    doc.playSubHistory(fileImport.getHistory());
                    doc.save();
                }
                catch(Exception e){
                    showAlert("Failed to import the document: " + path);

                    synchronized (importLock)
                    {
                        importSuccess = false;
                    }

                    return false;
                }

                synchronized (importLock)
                {
                    importSuccess = true;
                }

                return true;
            }

            protected void onPostExecute(Object o)
            {
                loadingDialog.dismiss();

                synchronized (importLock)
                {
                    importing = false;

                    if (!importSuccess)
                        return;
                }

                showAlert("Successfully imported!");
                callback.callback(o);
            }
        };
        t.execute(new Object[]{});
    }

    protected void setPasswordFile()
    {
        MainState.getInstance().document = new PasswordDocumentFile(MainState.getInstance().config.localDataFilePath);
        openInitialView();
    }

    public void setFile(String password) throws Exception
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

    protected ProgressDialog loadingDialog;

    public void saveDetail(final PasswordDetails detail, final Callback callback)
    {
        final PasswordDetails dets = detail.copy();
        loadingDialog = new ProgressDialog(MainState.getInstance().context);
        loadingDialog.setMessage("Saving password data");
        loadingDialog.show();

        Thread t = new Thread(new Runnable() { public void run()
        {
            try
            {
                MainState.getInstance().document.replaceDetails(dets);
                MainState.getInstance().document.save();
                dets.setHistory(new PasswordDocumentHistory());
            }
            catch(Exception e){
                Log.e("password", "failed to update password file");
            }

            MainState.getInstance().activity.runOnUiThread(new Runnable() {public void run()
            {
                loadingDialog.dismiss();
                callback.callback(dets);
            }});
        }});

        t.start();
    }

    public boolean hasPermission(String permission)
    {
        if (ContextCompat.checkSelfPermission(MainState.getInstance().context, permission) == PackageManager.PERMISSION_GRANTED)
            return true;
        else
            return false;
    }

    protected class PermissionStruct
    {
        public int requestNumber;
        public Callback callback;
        public String[] permissions;
    }

    protected static int permissionRequestNumber = 1;
    protected HashMap<Integer, PermissionStruct> permissionRequestCallbacks = new HashMap<>();

    public void requestPermission(final String permission, final Callback callback)
    {
        requestPermissions(new String[] {permission}, callback);
    }

    public void requestPermissions(final String[] permissions, final Callback callback)
    {
        PermissionStruct str = new PermissionStruct();
        str.requestNumber = permissionRequestNumber++;
        str.callback = callback;
        str.permissions = permissions;

        permissionRequestCallbacks.put(str.requestNumber, str);
        ActivityCompat.requestPermissions(MainState.getInstance().activity, permissions, str.requestNumber);
    }

    public void notifyPermissionResults(int requestCode)
    {
        PermissionStruct str = permissionRequestCallbacks.get(requestCode);

        if (str == null)
            return;

        boolean granted = false;
        for(String permission : str.permissions)
        {
            granted = hasPermission(permission);

            if (!granted)
                break;
        }
        str.callback.callback(granted);

        permissionRequestCallbacks.remove(requestCode);
    }
}
