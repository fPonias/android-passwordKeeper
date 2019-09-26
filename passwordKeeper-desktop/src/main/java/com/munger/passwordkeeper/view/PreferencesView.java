/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper.view;

import com.google.api.services.drive.model.User;
import com.munger.passwordkeeper.Main;
import com.munger.passwordkeeper.MainState;
import com.munger.passwordkeeper.Prefs;
import com.munger.passwordkeeper.drive.DriveHelper;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author hallmarklabs
 */
public class PreferencesView extends Page 
{
    public PreferencesView()
    {
        InitializeView();
        SetupListeners();
        Populate();
    }
    
    public JLabel googlePlayUserLbl;
    public JCheckBox googlePlayCheckbox;
    public JLabel remoteFilePrefLbl;
    public JLabel remoteFilePathLbl;
    public JCheckBox remoteFileBtn;
    public JButton changePasswordBtn;
    public JButton deleteLocalDataBtn;
    public JComboBox<TimeoutPair> timeoutList;
    
    private void InitializeView()
    {
        setLayout(new MigLayout());
        
        JLabel lbl = new JLabel("Google Play authorization");
        add(lbl);
        googlePlayUserLbl = new JLabel();
        add(googlePlayUserLbl);
        googlePlayCheckbox = new JCheckBox();
        add(googlePlayCheckbox, "wrap");
        
        remoteFilePrefLbl = new JLabel("Sync remote file");
        add(remoteFilePrefLbl);
        remoteFilePathLbl = new JLabel();
        add(remoteFilePathLbl);
        remoteFileBtn = new JCheckBox();
        add(remoteFileBtn, "wrap");
        
        lbl = new JLabel("Auto-close");
        add(lbl);
        timeoutList = new JComboBox<>();
        add(timeoutList, "wrap");
        
        lbl = new JLabel("Change password");
        add(lbl);
        changePasswordBtn = new JButton("...");
        add(changePasswordBtn, "wrap");
    }
    
    private void SetupListeners()
    {
        timeoutList.addActionListener((ActionEvent e) -> 
        {
            TimeoutPair selected = (TimeoutPair) timeoutList.getSelectedItem();
            Main.instance.mainState.prefs.setTimeout(selected.value);
        });
        
        changePasswordBtn.addActionListener((ActionEvent e) ->
        {
            changePassword();
        });
        
        remoteFileBtn.addActionListener((ActionEvent e) ->
        {
            if (remoteFileBtn.isSelected())
                disableRemoteSync();
            else
                enableRemoteSync();
        });
    }
    
    private class TimeoutPair
    {
        public long value;
        public String key;
        
        public TimeoutPair(String key, long value)
        {
            this.value = value;
            this.key = key;
        }
        
        public String toString() { return key; }
    }
   
    private void Populate()
    {
        if (!Main.instance.mainState.driveHelper.isAuthorized())
        {
            googlePlayUserLbl.setText("");
            googlePlayCheckbox.setSelected(false);
            
            remoteFilePrefLbl.setEnabled(false);
            remoteFilePathLbl.setEnabled(false);
            remoteFilePathLbl.setText("");
            remoteFileBtn.setEnabled(false);
            remoteFileBtn.setSelected(false);
        }
        else
        {
            User user = Main.instance.mainState.driveHelper.getUser();
            String userTxt = user.getDisplayName() + " (" + user.getEmailAddress() + ")";
            googlePlayUserLbl.setText(userTxt);
            googlePlayCheckbox.setSelected(true);
            
            boolean remoteSync = Main.instance.mainState.prefs.getSyncToDrive();
            remoteFileBtn.setSelected(remoteSync);

            DriveHelper.DriveFileStruct remoteFile = Main.instance.mainState.prefs.getSyncFile();
            if (remoteFile != null)
                remoteFilePathLbl.setText(remoteFile.name);
            else
                remoteFilePathLbl.setText("");
        }
        
        TimeoutPair[] valueList = new TimeoutPair[]
        {
            new TimeoutPair("1 min",     60000),
            new TimeoutPair("5 mins",   300000),
            new TimeoutPair("10 mins",  600000),
            new TimeoutPair("30 mins", 1800000),
            new TimeoutPair("never", -1)
        };
        timeoutList.setModel(new DefaultComboBoxModel<>(valueList));
    }
    
    private void changePassword()
    {
        
    }
    
    private void enableRemoteSync()
    {
        try
        {
            DriveHelper.DriveFileStruct str = Main.instance.mainState.prefs.getSyncFile();
            if (str == null)
                str = Main.instance.mainState.driveHelper.openFileChooser();
            
            if (str == null)
                remoteFileBtn.setSelected(false);
            
            if (str.id == DriveHelper.NEW_ID)
            {
                remoteFileBtn.setSelected(false);
                return;
            }
            
            //Main.instance.mainState.driveHelper.download(str)
        }
        catch(Exception e)
        {}
    }
    
    private void disableRemoteSync()
    {
        
    }
}