/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper.view;

import com.munger.passwordkeeper.Main;
import com.munger.passwordkeeper.Prefs;
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
    
    public JCheckBox googlePlayCheckbox;
    public JButton changePasswordBtn;
    public JButton deleteLocalDataBtn;
    public JComboBox<TimeoutPair> timeoutList;
    public JLabel filePathLbl;
    public JButton filePathBtn;
    
    private void InitializeView()
    {
        setLayout(new MigLayout());
        
        JLabel lbl = new JLabel("Sync with google play");
        add(lbl);
        googlePlayCheckbox = new JCheckBox();
        add(googlePlayCheckbox, "wrap");
        
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
        filePathLbl.setText(Main.instance.mainState.prefs.getSavePath());
        
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
}