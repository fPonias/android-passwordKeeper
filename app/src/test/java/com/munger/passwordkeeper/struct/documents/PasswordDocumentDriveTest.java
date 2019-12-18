package com.munger.passwordkeeper.struct.documents;

import com.google.api.client.util.DateTime;
import com.munger.passwordkeeper.Helper;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.R;
import com.munger.passwordkeeper.TestingMainActivity;
import com.munger.passwordkeeper.helpers.DriveHelper;
import com.munger.passwordkeeper.helpers.NavigationHelper;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import com.munger.passwordkeeper.struct.Settings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import androidx.fragment.app.FragmentActivity;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertSame;

/**
 * Created by codymunger on 9/30/19.
 */

@RunWith(AndroidJUnit4.class)
@SmallTest
public class PasswordDocumentDriveInstTest
{

    public class SettingsDer extends Settings
    {
        public float timeout = 120.0f;

        @Override
        public float getTimeout()
        {
            return timeout;
        }

        public float getRealTimeout() {return super.getTimeout();}

        public boolean saveToCloud = true;

        @Override
        public boolean getSaveToCloud() {
            return saveToCloud;
        }

        public boolean getRealSaveToCloud(){
            return super.getSaveToCloud();
        }

        @Override
        public DateTime getLastCloudUpdate()
        {
            return new DateTime(System.currentTimeMillis() - 31536000000L);
        }
    }

    private int fillDetails(PasswordDetails dets)
    {
        dets.setName("one");
        dets.setLocation("two");
        PasswordDetailsPair pair = dets.addEmptyPair();
        pair.setKey("three");
        pair.setValue("four");
        PasswordDetailsPair pair2 = dets.addEmptyPair();
        pair2.setKey("three");
        pair2.setValue("four");

        return 8;
    }

    public class DriveHelperDer extends DriveHelper
    {
        public PasswordDocumentFile doc;

        @Override
        public void connect()
        {
            try
            {
                doc = new PasswordDocumentFile("test", "pass");
                PasswordDocument doc = HelperNoInst.generateDocument(5, 5);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                doc.detailsToEncryptedString(dos);
                byte[] output = baos.toByteArray();
            } catch (Exception e){}

            connected = true;

            synchronized (lock)
            {
                lock.notify();
            }
        }

        private String fileId = "testId";

        @Override
        public String getOrCreateFile(String name) throws IOException
        {
            return fileId;
        }

        @Override
        public Meta getMetadata(String fileId) throws IOException
        {
            assertSame(fileId, this.fileId);
            Meta ret = new Meta();
            ret.modified = new DateTime(System.currentTimeMillis());
            ret.size = new File(doc.getHistoryFilePath()).length();

            return ret;
        }

        @Override
        public InputStream getRemoteFile(String fileId) throws IOException
        {
            File f = new File(doc.getHistoryFilePath());
            InputStream fis = new FileInputStream(f);
            return fis;
        }
    }


    public class MainStateDer extends MainState
    {
        protected SettingsDer mySettings;
        public void updateTimeout(float minutes)
        {
            mySettings.timeout = minutes;
            quitTimer.reset();
        }

        @Override
        protected void setupPreferences()
        {
            mySettings = new SettingsDer();
            settings = mySettings;
        }

        @Override
        protected DriveHelper createDriveHelper()
        {
            return new DriveHelperDer();
        }
    }

    @Rule
    public ActivityTestRule<TestingMainActivity> activityRule = new ActivityTestRule<>(TestingMainActivity.class);

    private MainStateDer mainState;

    @Before
    public void before() throws Exception
    {
        FragmentActivity activity = activityRule.getActivity();
        mainState = new MainStateDer();
        MainState.setInstance(mainState);
        mainState.setContext(activity, activity);

        ((PasswordDocumentFile) mainState.document).setRootPath(NavigationHelper.getRootPath());
        mainState.document.delete();
    }

    @Test
    public void load() throws Exception
    {
        activityRule.getActivity().superInit();

        onView(withId(R.id.createfile_password1ipt)).perform(typeText("pass"));
        onView(withId(R.id.createfile_password2ipt)).perform(typeText("pass"));
        onView(withId(R.id.createfile_okaybtn)).perform(click());

        Thread.sleep(200000);
    }

    @Test
    public void localUpdate()
    {

    }

    @Test
    public void remoteUpdate()
    {

    }

    @Test
    public void localAndRemoteUpdateNoConflicts()
    {

    }

    @Test
    public void localAndRemoteUpdateWithConflicts()
    {

    }

    @Test
    public void permissionsGranted()
    {

    }

    @Test
    public void permissionsDenied()
    {

    }

    @Test
    public void updateUploadFail()
    {

    }

    @Test
    public void updateDownloadFail()
    {

    }
}
