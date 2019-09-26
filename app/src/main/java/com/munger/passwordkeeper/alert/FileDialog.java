package com.munger.passwordkeeper.alert;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Environment;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.helpers.NavigationHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import androidx.appcompat.app.AlertDialog;

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

    private ListenerList<FileSelectedListener> fileListenerList = new ListenerList<FileDialog.FileSelectedListener>();
    private final Activity activity;
    private String fileEndsWith;
    private boolean initted;

    public boolean getInitted()
    {
        return initted;
    }

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

        final String permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        if (!MainState.getInstance().navigationHelper.hasPermission(permission))
        {
            MainState.getInstance().navigationHelper.requestPermission(permission, new NavigationHelper.Callback(){public void callback(Object result)
            {
                if ((boolean) result == true)
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
    protected Dialog createFileDialog()
    {
        Dialog dialog = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(currentPath.getPath());

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

    private List<File> fileData;
    private HashMap<String, File> fileIndex;

    private void loadFileList(File path)
    {
        this.currentPath = path;
        List<File> fileData = new ArrayList<>();
        fileIndex = new HashMap<>();

        if (path.exists())
        {
            File parent = path.getParentFile();
            if (parent != null)
            {
                //filtering on PARENT_DIR doesn't work, use getParentFile instead.
                if (doFilter(parent))
                    fileData.add(parent);
            }

            File[] fileList1 = path.listFiles();
            for (File file : fileList1)
            {
                if (doFilter(file))
                    fileData.add(file);
            }
        }

        int sz = fileData.size();
        fileList = new String[sz];
        for (int i = 0; i < sz; i++)
        {
            File f = fileData.get(i);

            if (f.isDirectory())
                fileList[i] = f.getName() + '/';
            else
                fileList[i] = f.getName();

            fileIndex.put(fileList[i], f);
        }
    }

    private boolean doFilter(File sel)
    {
        if (!sel.canRead())
            return false;

        if (sel.isDirectory())
        {
            File[] list = sel.listFiles();
            if (list != null)
                return true;
        }

        String filename = sel.getName().toLowerCase();
        if (filename != ".." && filename.startsWith("."))
            return false;

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
            return fileIndex.get(fileChosen);
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
