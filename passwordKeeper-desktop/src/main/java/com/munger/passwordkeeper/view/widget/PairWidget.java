/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper.view.widget;

import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import javax.swing.JButton;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author hallmarklabs
 */
public class PairWidget extends JPanel
{
    public PairWidget()
    {
        initializeComponent();
        updateEditable();
    }
    
    public LabelAndTextEdit keyIpt;
    public LabelAndTextEdit valueIpt;
    public JButton deleteBtn;
    
    private PasswordDetailsPair pair;
    public PasswordDetailsPair getPair()
    {
        return pair;
    }
    public void setPair(PasswordDetailsPair value)
    {
        pair = value;
        update();
    }
    
    private boolean editable;
    public boolean getEditable()
    {
        return editable;
    }
    public void setEditable(boolean value)
    {
        editable = value;
        updateEditable();
    }
    
    private void initializeComponent()
    {
        setLayout(new MigLayout("insets 0", "[grow,fill][grow,fill][]"));
        
        keyIpt = new LabelAndTextEdit();
        add(keyIpt, "width 50%-25px");
        valueIpt = new LabelAndTextEdit();
        add(valueIpt, "width 50%-25px");
        deleteBtn = new JButton("-");
        add(deleteBtn);
    }
    
    private void update()
    {
        if (pair == null)
            return;
        
        keyIpt.setText(pair.getKey());
        valueIpt.setText(pair.getValue());
    }
    
    private void updateEditable()
    {
        deleteBtn.setVisible(editable);
        keyIpt.setEditable(editable);
        valueIpt.setEditable(editable);
    }
}
