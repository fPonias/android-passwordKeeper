/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper.view.widget;

import com.munger.passwordkeeper.struct.PasswordDetails;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author hallmarklabs
 */
public class DetailWidget extends JPanel
{
    public JButton deleteBtn;
    public JLabel nameLbl;
    
    public DetailWidget()
    {
        initializeComponent();
    }
    
    private void initializeComponent()
    {
        MigLayout layout = new MigLayout("insets 0", "[grow,fill][]");
        setLayout(layout);
        
        nameLbl = new JLabel();
        add(nameLbl);
        deleteBtn = new JButton("-");
        add(deleteBtn);
        
        setEditable(editable);
    }
    
    private void update()
    {
        String name = details.getName();
        if (name.length() == 0)
            name = "<new entry>";
        
        nameLbl.setText(name);
    }
    
    private boolean editable = false;
    public boolean getEditable()
    {
        return editable;
    }
    public void setEditable(boolean value)
    {
        editable = value;
        
        deleteBtn.setVisible(editable);
    }
    
    private PasswordDetails details;
    
    public PasswordDetails getDetails()
    {
        return details;
    }
    
    public void setDetails(PasswordDetails value)
    {
        details = value;
        update();
    }
}
