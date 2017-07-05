/*
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper.view.widget;

import com.munger.passwordkeeper.Main;
import java.awt.Dimension;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author hallmarklabs
 */
public class LabelAndTextEdit extends javax.swing.JPanel {

    /**
     * Creates new form LabelAndTextEdit
     */
    public LabelAndTextEdit() {
        initComponents();
        setEditable(editable);
        setupListeners();
    }
    
    private boolean editable = false;
    
    public boolean getEditable()
    {
        return editable;
    }
    
    public void setEditable(boolean value)
    {
        editable = value;
        
        remove(input);
        remove(label);

        if (editable)
            add(input);
        else
            addLabel();
    }
    
    private void addLabel()
    {
        add(label, "pos 6 0");
    }
    
    private void setupListeners()
    {
        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) 
            {
                updateLabel();
            }

            @Override
            public void removeUpdate(DocumentEvent e) 
            {
                updateLabel();
            }

            @Override
            public void changedUpdate(DocumentEvent e) 
            {
                updateLabel();
            }
        });
    }
    
    private void updateLabel()
    {
        String text = input.getText();
        label.setText(text);
        notifyOnValueChanged(text);
    }
    
    public String getText()
    {
        if (editable)
            return input.getText();
        else
            return label.getText();
    }
    
    public void setText(String value)
    {
        input.setText(value);
        label.setText(value);
        notifyOnValueChanged(value);
    }
                       
    private void initComponents() 
    {
        setLayout(new MigLayout("wrap 1,insets 0", "[grow,fill]", "[24]"));

        label = new javax.swing.JLabel();
        label.setMinimumSize(new Dimension(100, 27));
        addLabel();

        input = new javax.swing.JTextField();
        add(input);
    }                      

                  
    private javax.swing.JTextField input;
    private javax.swing.JLabel label;  
    
    public interface IValueChangedListener
    {
        public void updated(String value);
    }
    
    private ArrayList<WeakReference<IValueChangedListener>> changeListeners = new ArrayList<>();
    public void addOnValueChangedListener(IValueChangedListener listener)
    {
        WeakReference<IValueChangedListener> ref = new WeakReference<>(listener);
        changeListeners.add(ref);
    }
    private void notifyOnValueChanged(String value)
    {
        int sz = changeListeners.size();
        for (int i = 0; i < sz; i++)
        {
            WeakReference<IValueChangedListener> ret = changeListeners.get(i);
            IValueChangedListener listener = ret.get();
            if (listener == null)
            {
                changeListeners.remove(i);
                i--;
                sz--;
            }
            else
            {
                listener.updated(value);
            }
        }
    }
}
