package com.munger.passwordkeeper.alert;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.munger.passwordkeeper.MainActivity;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class FileDialog
{
    private static final String PARENT_DIR = "..";
    private final String TAG = getClass().getName();
    private String[] fileList;
    private File currentPath;

    public interface FileSelectedListener
    {
        void fileSelected(File file);
    }

    public interface DirectorySelectedListener
    {
        void directorySelected(File directory);
    }
    private ListenerList<FileSelectedListener> fileListenerList = new ListenerList<FileDialog.FileSelectedListener>();
    private ListenerList<DirectorySelectedListener> dirListenerList = new ListenerList<FileDialog.DirectorySelectedListener>();
    private final Activity activity;
    private boolean selectDirectoryOption;
    private String fileEndsWith;
    private boolean initted;

    /**
     * @param activity
     * @param initialPath
     */
    public FileDialog(Activity activity, File initialPath)
    {
        this(activity, initialPath, null);
    }

    public FileDialog(Activity activity, final File initialPath, String fileEndsWith)
    {
        initted = false;
        this.activity = activity;
        setFileEndsWith(fileEndsWith);

        if (ContextCompat.checkSelfPermission(MainActivity.getInstance(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.getInstance(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

            MainActivity.getInstance().addPermisionResultListener(new MainActivity.IPermissionResult() {public void result(int requestCode)
            {
                if (requestCode != 1)
                    return;

                if (ContextCompat.checkSelfPermission(MainActivity.getInstance(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                    init(initialPath);
                else
                {
                    if (initListener != null)
                        initListener.initted(false);
                }
            }});
        }
        else
            init(initialPath);
    }

    private interface InitListener
    {
        void initted(boolean success);
    }

    private InitListener initListener = null;

    private void init(File initialPath)
    {
        initted = true;

        if (!initialPath.exists())
            initialPath = Environment.getExternalStorageDirectory();

        loadFileList(initialPath);

        if (initListener != null)
            initListener.initted(true);
    }

    /**
     * @return file dialog
     */
    public Dialog createFileDialog()
    {
        Dialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(currentPath.getPath());
        if (selectDirectoryOption)
        {
            builder.setPositiveButton("Select directory", new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which)
            {
                Log.d(TAG, currentPath.getPath());
                fireDirectorySelectedEvent(currentPath);
            }});
        }

        builder.setItems(fileList, new DialogInterface.OnClickListener() {public void onClick(DialogInterface dialog, int which)
        {
            String fileChosen = fileList[which];
            File chosenFile = getChosenFile(fileChosen);
            if (chosenFile.isDirectory())
            {
                loadFileList(chosenFile);
                dialog.cancel();
                dialog.dismiss();
                showDialog();
            }
            else
                fireFileSelectedEvent(chosenFile);
        }});

        dialog = builder.show();
        return dialog;
    }


    public void addFileListener(FileSelectedListener listener)
    {
        fileListenerList.add(listener);
    }

    public void removeFileListener(FileSelectedListener listener)
    {
        fileListenerList.remove(listener);
    }

    public void setSelectDirectoryOption(boolean selectDirectoryOption)
    {
        this.selectDirectoryOption = selectDirectoryOption;
    }

    public void addDirectoryListener(DirectorySelectedListener listener)
    {
        dirListenerList.add(listener);
    }

    public void removeDirectoryListener(DirectorySelectedListener listener)
    {
        dirListenerList.remove(listener);
    }

    public void showDialog()
    {
        if (!initted)
        {
            initListener = new InitListener() {public void initted(boolean success)
            {
                if (success)
                    showDialog();

                initListener = null;
            }};
        }
        else
            createFileDialog().show();
    }

    private void fireFileSelectedEvent(final File file)
    {
        fileListenerList.fireEvent(new FireHandler<FileSelectedListener>() {public void fireEvent(FileSelectedListener listener)
        {
            listener.fileSelected(file);
        }});
    }

    private void fireDirectorySelectedEvent(final File directory)
    {
        dirListenerList.fireEvent(new FireHandler<DirectorySelectedListener>() {public void fireEvent(DirectorySelectedListener listener)
        {
            listener.directorySelected(directory);
        }});
    }

    private void loadFileList(File path)
    {
        this.currentPath = path;
        List<File> r = new ArrayList<>();

        if (path.exists())
        {
            File parent = path.getParentFile();
            if (parent != null)
            {
                //filtering on PARENT_DIR doesn't work, use getParentFile instead.
                if (doFilter(parent))
                    r.add(new File(PARENT_DIR));
            }

            File[] fileList1 = path.listFiles();
            for (File file : fileList1)
            {
                if (doFilter(file))
                    r.add(file);
            }
        }

        int sz = r.size();
        fileList = new String[sz];
        for (int i = 0; i < sz; i++)
        {
            File f = r.get(i);

            if (f.isDirectory())
                fileList[i] = f.getName() + '/';
            else
                fileList[i] = f.getName();
        }
    }

    private boolean doFilter(File sel)
    {
        if (!sel.canRead())
            return false;

        if (sel.isDirectory())
        {
            String[] list = sel.list();
            if (list != null)
                return true;
        }

        String filename = sel.getName().toLowerCase();
        if (filename != ".." && filename.startsWith("."))
            return false;

        if (selectDirectoryOption)
            return sel.isDirectory();

        if (fileEndsWith == null)
            return true;

        if (filename.endsWith(fileEndsWith))
            return true;

        return false;
    }

    private File getChosenFile(String fileChosen)
    {
        if (fileChosen.equals(PARENT_DIR))
            return currentPath.getParentFile();
        else
            return new File(currentPath, fileChosen);
    }

    private void setFileEndsWith(String fileEndsWith)
    {
        this.fileEndsWith = fileEndsWith != null ? fileEndsWith.toLowerCase() : fileEndsWith;
    }

    public interface FireHandler<L>
    {
        void fireEvent(L listener);
    }

    class ListenerList<L>
    {
        private List<L> listenerList = new ArrayList<L>();


        public void add(L listener)
        {
            listenerList.add(listener);
        }

        public void fireEvent(FireHandler<L> fireHandler)
        {
            List<L> copy = new ArrayList<L>(listenerList);
            for (L l : copy)
            {
                fireHandler.fireEvent(l);
            }
        }

        public void remove(L listener)
        {
            listenerList.remove(listener);
        }

        public List<L> getListenerList()
        {
            return listenerList;
        }
    }
}
