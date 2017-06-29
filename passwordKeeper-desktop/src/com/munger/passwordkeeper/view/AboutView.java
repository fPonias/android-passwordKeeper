/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author hallmarklabs
 */
public class AboutView extends Page
{
    public AboutView()
    {
        initializeComponent();
    }
    
    private void initializeComponent()
    {
        setLayout(new MigLayout());
        Font font = getFont();
        
        JLabel lbl = new JLabel("Password Crypt"); 
        lbl.setFont(new Font(font.getName(), Font.BOLD, 18));
        add(lbl, "wrap");
        
        addSeperator();
        
        lbl = new JLabel("Version"); add(lbl, "wrap");
        lbl = new JLabel("0.51"); add(lbl, "wrap");
     
        addSeperator();
        
        lbl = new JLabel("DEVELOPED BY"); add(lbl, "wrap");
        
        addSeperator();
        
        lbl = new JLabel("Cody Munger"); add(lbl, "wrap");
        lbl = new JLabel("Developer/Designer"); add(lbl, "wrap");
        
        addSeperator();
        
        lbl = new JLabel("SOURCE"); add(lbl, "wrap");
        
        addSeperator();
        
        lbl = new JLabel("Source code at"); add(lbl, "wrap");
        lbl = new JLabel("https://github.com/fPonias/android-passwordKeeper"); add(lbl, "wrap");
        
        addSeperator();
        
        lbl = new JLabel("LIBRARIES USED"); add(lbl, "wrap");
        
        addSeperator();
        
        lbl = new JLabel("Byte-oriented AES-256 implementation"); add(lbl, "wrap");
        lbl = new JLabel("http://www.literatecode.com"); add(lbl, "wrap");
        lbl = new JLabel("Ilya O. Levin"); add(lbl, "wrap");
        lbl = new JLabel("Hal Finney"); add(lbl, "wrap");
        
        addSeperator();
        
        lbl = new JLabel("MD5 Message Digest Algorithm"); add(lbl, "wrap");
        lbl = new JLabel("RSA Data Security, Inc."); add(lbl, "wrap");
    }
    
    private void addSeperator()
    {
        JSeparator box = new JSeparator();
        box.setPreferredSize(new Dimension(10000, 1));
        add(box, "wrap, span 100");
    }
}
