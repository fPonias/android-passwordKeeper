package com.munger.passwordkeeper.alert;

/**
 * Created by codymunger on 12/5/16.
 */


import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.FragmentActivity;

import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.TestingMainActivity;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;
import com.munger.passwordkeeper.view.CreateFileFragment;
import com.munger.passwordkeeper.view.CreateFileFragmentTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.File;

import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;
import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static android.support.test.espresso.action.ViewActions.*;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class FileDialogTest
{
    public class MainStateDer extends MainState
    {
        @Override
        public void setupDocument()
        {
            documentMock = mock(PasswordDocumentFile.class);
            document = documentMock;
        }

        @Override
        protected void setupNavigation()
        {
            navigationMock = mock(NavigationHelper.class);
            doReturn(true).when(navigationMock).hasPermission(any(String.class));
            navigationHelper = navigationMock;
        }

        @Override
        public void setupDriveHelper()
        {
        }

        @Override
        public void setupQuitTimer()
        {
        }
    }

    private MainStateDer mainState;
    private PasswordDocument documentMock;
    private NavigationHelper navigationMock;

    @Rule
    public ActivityTestRule<TestingMainActivity> activityRule = new ActivityTestRule<>(TestingMainActivity.class);

    @Before
    public void before()
    {
        Context context = InstrumentationRegistry.getContext();
        FragmentActivity activity = activityRule.getActivity();
        mainState = new MainStateDer();
        MainState.setInstance(mainState);
        mainState.setContext(activity, activity);
    }

    public class FileStruct
    {
        public FileStruct(String name, boolean isDirectory, boolean canRead)
        {
            this.name = name;
            this.isDirectory = isDirectory;
            this.canRead = canRead;
        }

        public String name;
        public boolean isDirectory;
        public boolean canRead;
    }

    public File[] getFileList(FileStruct[] files)
    {
        int sz = files.length;
        File[] ret = new File[sz];
        for (int i = 0; i < sz; i++)
        {
            File filemock = mock(File.class);
            doReturn(true).when(filemock).exists();
            doReturn(files[i].name).when(filemock).getName();
            doReturn(files[i].isDirectory).when(filemock).isDirectory();
            doReturn(files[i].canRead).when(filemock).canRead();
            ret[i] = filemock;
        }

        return ret;
    }

    private File getRootMock()
    {
        final File filemock = mock(File.class);
        doReturn(true).when(filemock).exists();
        doReturn(true).when(filemock).isDirectory();
        doReturn("..").when(filemock).getName();
        doReturn(true).when(filemock).canRead();

        return filemock;
    }

    private void showDialog(final File rootMock)
    {
        showDialog(rootMock, null);
    }

    private FileDialog dialog;

    private void showDialog(final File rootMock, final String endFilter)
    {
        final Object lock = new Object();

        MainState.getInstance().handler.post(new Runnable() {public void run()
        {
            dialog = new FileDialog(MainState.getInstance().activity, rootMock, endFilter);
            dialog.showDialog();

            synchronized (lock)
            {
                lock.notify();
            }
        }});

        synchronized (lock)
        {
            try{lock.wait(2000);}catch(InterruptedException e){fail();}
        }
    }

    private void checkList(String[] expected)
    {
        int sz = expected.length;
        for (int i = 0; i < sz; i++)
        {
            onData(anything()).atPosition(i).check(matches(allOf(withClassName(containsString("TextView")), withText(expected[i]))));
        }

        boolean thrown = false;
        try
        {
            onData(anything()).atPosition(sz).check(doesNotExist());
        }
        catch(Exception e){
            thrown = true;
        }
        assertTrue(thrown);
    }

    @Test
    public void displaysDirectoryContents()
    {
        FileStruct[] list = new FileStruct[]{
                new FileStruct("a", false, true),
                new FileStruct("b", false, true)
        };

        File filemock = getRootMock();
        doReturn(getFileList(list)).when(filemock).listFiles();

        showDialog(filemock);

        String[] expected = new String[]{
            "a", "b"
        };
        checkList(expected);
    }

    @Test
    public void includesParent()
    {
        FileStruct[] list = new FileStruct[]{
                new FileStruct("a", false, true),
                new FileStruct("b", false, true)
        };

        File filemock = getRootMock();
        doReturn(getFileList(list)).when(filemock).listFiles();
        doReturn(filemock).when(filemock).getParentFile();

        showDialog(filemock);

        String[] expected = new String[]{
                "../", "a", "b"
        };
        checkList(expected);
    }

    @Test
    public void excludesNonReadable()
    {
        FileStruct[] list = new FileStruct[]{
                new FileStruct("a", false, false),
                new FileStruct("b", false, true)
        };

        File filemock = getRootMock();
        doReturn(getFileList(list)).when(filemock).listFiles();

        showDialog(filemock);

        String[] expected = new String[]{
                "b"
        };
        checkList(expected);
    }

    @Test
    public void endsWithFilter()
    {
        FileStruct[] list = new FileStruct[]{
                new FileStruct("a.foo", false, true),
                new FileStruct("b.txt", false, true)
        };

        File filemock = getRootMock();
        doReturn(getFileList(list)).when(filemock).listFiles();

        showDialog(filemock, ".txt");

        String[] expected = new String[]{
                "b.txt"
        };
        checkList(expected);
    }

    @Test
    public void subdirectoryDisplay()
    {
        FileStruct[] list = new FileStruct[]{
                new FileStruct("a", true, true),
                new FileStruct("b", false, true)
        };

        FileStruct[] sublist = new FileStruct[]{
                new FileStruct("c", false, true),
                new FileStruct("d", false, true)
        };

        File filemock = getRootMock();
        File[] files = getFileList(list);
        doReturn(files).when(filemock).listFiles();
        doReturn(getFileList(sublist)).when(files[0]).listFiles();
        doReturn(filemock).when(files[0]).getParentFile();

        showDialog(filemock);

        String[] expected = new String[]{
                "a/", "b"
        };
        checkList(expected);


        onData(anything()).atPosition(0).perform(click());

        expected = new String[]{
                "../", "c", "d"
        };
        checkList(expected);


        onData(anything()).atPosition(0).perform(click());

        expected = new String[]{
                "a/", "b"
        };
        checkList(expected);
    }

    private class Status
    {
        boolean called = false;
    }

    @Test
    public void fileSelectable()
    {
        FileStruct[] list = new FileStruct[]{
                new FileStruct("a", false, true)
        };

        File filemock = getRootMock();
        doReturn(getFileList(list)).when(filemock).listFiles();

        final Status status = new Status();
        final Object lock = new Object();
        FileDialog.FileSelectedListener listener = new FileDialog.FileSelectedListener() {public void fileSelected(File file)
        {
            status.called = true;
            assertEquals(file.getName(), "a");
            synchronized (lock)
            {
                lock.notify();
            }
        }};

        showDialog(filemock);
        dialog.addFileListener(listener);

        String[] expected = new String[]{"a"};
        checkList(expected);

        onData(anything()).atPosition(0).perform(click());

        synchronized (lock)
        {
            try {lock.wait(2000);} catch(InterruptedException e){fail();}
        }

        assertTrue(status.called);


        status.called = false;
        showDialog(filemock);
        dialog.addFileListener(listener);
        dialog.removeFileListener(listener);

        expected = new String[]{"a"};
        checkList(expected);

        onData(anything()).atPosition(0).perform(click());

        synchronized (lock)
        {
            try {lock.wait(250);} catch(InterruptedException e){fail();}
        }

        assertFalse(status.called);
    }

    private void permissionTest(final boolean grant)
    {
        final Object lock = new Object();
        doAnswer(new Answer<Void>() {public Void answer(InvocationOnMock invocationOnMock) throws Throwable
        {
            final NavigationHelper.Callback callback = invocationOnMock.getArgumentAt(1, NavigationHelper.Callback.class);

            MainState.getInstance().handler.post(new Runnable() {public void run()
            {
                callback.callback(grant);

                synchronized (lock)
                {
                    lock.notify();
                }
            }});

            return null;
        }}).when(navigationMock).requestPermission(any(String.class), any(NavigationHelper.Callback.class));
        doReturn(false).when(navigationMock).hasPermission(any(String.class));


        FileStruct[] list = new FileStruct[]{
                new FileStruct("a", false, true)
        };

        File filemock = getRootMock();
        doReturn(getFileList(list)).when(filemock).listFiles();

        showDialog(filemock);

        synchronized (lock)
        {
            try{lock.wait(250);}catch(InterruptedException e){fail();}
        }
    }

    @Test
    public void permissionGranted()
    {
        permissionTest(true);
        assertTrue(dialog.getInitted());
    }

    @Test
    public void permissionDenied()
    {
        permissionTest(false);
        assertFalse(dialog.getInitted());
    }
}
