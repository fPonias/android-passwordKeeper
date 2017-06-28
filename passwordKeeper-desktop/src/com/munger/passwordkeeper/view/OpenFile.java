/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper.view;

import com.munger.passwordkeeper.Main;
import com.munger.passwordkeeper.view.helper.KeyTypedListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 *
 * @author hallmarklabs
 */
public class OpenFile extends Page
{

    /**
     * Creates new form OpenFile
     */
    public OpenFile() {
        initComponents();
        
        Main.instance.enableEditActions(false);
        Main.instance.enableResetActions(false);
        Main.instance.enableDetailsActions(false);
        
        submitBtn.addActionListener((ActionEvent e) -> {
            submit();
        });
        
        passwordInput.addKeyListener(new KeyTypedListener() {public void keyTyped(KeyEvent e) 
        {
            if (e.getExtendedKeyCode() == KeyEvent.VK_ENTER)
            {
                submit();
            }
        }});
    }
    
    private void submit()
    {
        char[] pw = passwordInput.getPassword();
        Main.instance.mainState.document.setPassword(new String(pw));
        
        if (Main.instance.mainState.document.testPassword())
        {
            Main.instance.openFile();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        passwordLabel = new javax.swing.JLabel();
        passwordInput = new javax.swing.JPasswordField();
        submitBtn = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        passwordLabel.setText("Password:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.ipadx = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        add(passwordLabel, gridBagConstraints);

        passwordInput.setPreferredSize(new java.awt.Dimension(120, 26));
        add(passwordInput, new java.awt.GridBagConstraints());

        submitBtn.setText("Open Password Repo");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new java.awt.Insets(40, 0, 0, 0);
        add(submitBtn, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPasswordField passwordInput;
    private javax.swing.JLabel passwordLabel;
    private javax.swing.JButton submitBtn;
    // End of variables declaration//GEN-END:variables
}
