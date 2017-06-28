/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper.view;

import com.munger.passwordkeeper.Main;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.PasswordDetailsPair;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;
import com.munger.passwordkeeper.view.helper.ClickListener;
import com.munger.passwordkeeper.view.widget.DetailWidget;
import com.munger.passwordkeeper.view.widget.LabelAndTextEdit;
import com.munger.passwordkeeper.view.widget.PairWidget;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author hallmarklabs
 */
public class DetailsView extends Page
{
    public DetailsView()
    {
        init();
        setupListeners();
        update();
    }
    
    private JButton backBtn;
    private JToggleButton editBtn;
    private JButton addBtn;
    private JPanel content;
    private LabelAndTextEdit nameIpt;
    private LabelAndTextEdit locationIpt;
    
    private PasswordDetails originalDetails;
    private PasswordDetails details;
    public PasswordDetails getOriginalDetails()
    {
        return originalDetails;
    }
    public PasswordDetails getDetails()
    {
        return details;
    }
    public void setDetails(PasswordDetails value)
    {
        originalDetails = value;
        details = value.copy();
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
    
    private void init()
    {
        Main.instance.enableEditActions(false);
        Main.instance.enableResetActions(true);
        Main.instance.enableDetailsActions(true);
        
        MigLayout layout = new MigLayout("wrap 1,insets 0", "[grow,fill]");
        setLayout(layout);
        
        JPanel topBar = new JPanel();
        topBar.setLayout(new MigLayout("insets 0", "[][grow,fill][]"));
        backBtn = new JButton("<-");
        topBar.add(backBtn);
        Box spacer = Box.createHorizontalBox();
        topBar.add(spacer);
        editBtn = new JToggleButton("edit");
        topBar.add(editBtn);
        
        add(topBar);
        
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new MigLayout("insets 0 10 0 10", "[][grow,fill]"));
        JLabel nameLbl = new JLabel("Name:");
        namePanel.add(nameLbl);
        nameIpt = new LabelAndTextEdit();
        namePanel.add(nameIpt, "wrap");
        JLabel locationLbl = new JLabel("Location:");
        namePanel.add(locationLbl);
        locationIpt = new LabelAndTextEdit();
        namePanel.add(locationIpt);
       
        add(namePanel);
        
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new MigLayout("gap 1 1,insets 10 0 0 0", "10[grow,fill]10", ""));
        JLabel keyTitleLbl = new JLabel("Username/Key:", SwingConstants.CENTER);
        titlePanel.add(keyTitleLbl);
        JLabel valueTitleLbl = new JLabel("Password/Value:", SwingConstants.CENTER);
        titlePanel.add(valueTitleLbl);
        
        add(titlePanel);
        
        JPanel contentOuter = new JPanel();
        contentOuter.setLayout(new BorderLayout());
        content = new JPanel();
        content.setLayout(new MigLayout("wrap 1,gap 1 1,insets 10 0 0 0", "10[grow,fill]10", ""));
        contentOuter.add(content, BorderLayout.NORTH);
        
        
        addBtn = new JButton("+");
        addBtn.setVisible(false);
        addBtn.setMaximumSize(new Dimension(30, 30));
        content.add(addBtn, "align center");
        
        JScrollPane scroller = new JScrollPane(contentOuter);
        scroller.setPreferredSize(new Dimension(10000, 10000));
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scroller, "grow 100 100");
    }
    
    private void setupListeners()
    {
        editBtn.addActionListener((ActionEvent e) -> {
            boolean oldValue = editable;
            boolean newValue = (editBtn.isSelected()) ? true : false;
            
            if (oldValue == newValue)
                return;
            
            setEditable(newValue);
            
            if (newValue == false)
            {
                promptSave();
            }
        });
        
        addBtn.addActionListener((ActionEvent e) -> {
            details.addEmptyPair();
            update();
        });
        
        backBtn.addActionListener((ActionEvent e) -> {
            goBack();
        });
       
        PasswordDocument doc = Main.instance.mainState.document;
        doc.addListener(new PasswordDocument.DocumentEvents() {
            @Override
            public void initFailed(Exception e) 
            {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void initted() 
            {
                update();
            }

            @Override
            public void saved() 
            {
                update();
            }

            @Override
            public void loaded() 
            {
                update();
            }

            @Override
            public void deleted() 
            {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void closed() 
            {
                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        });
        
        nameIpt.addOnValueChangedListener((String value) -> 
        {
            details.setName(value);
        });
        
        locationIpt.addOnValueChangedListener((String value) ->
        {
            details.setLocation(value);
        });
    }
      
    private void goBack()
    {
        boolean doBack = promptSave();
        
        if (doBack)
            Main.instance.goBack();
    }
    
    private boolean promptSave()
    {
        if (!originalDetails.diff(details))
           return true;

        int result = JOptionPane.showConfirmDialog(this, "Save your changes?", "", JOptionPane.YES_NO_CANCEL_OPTION);

        switch (result) {
            case JOptionPane.CANCEL_OPTION:
                return false;
            case JOptionPane.NO_OPTION:
                return true;
            case JOptionPane.YES_OPTION:
            default:
                try
                {
                    PasswordDocumentHistory hist = details.getHistory();
                    Main.instance.mainState.document.playSubHistory(hist);
                    Main.instance.mainState.document.save();
                }
                catch(Exception e1){}
                return true;
        }
    }
    
    private void addWidgetListeners(PairWidget widget)
    {
        widget.deleteBtn.addActionListener((ActionEvent e) -> {
            PasswordDetailsPair pair = widget.getPair();
            details.removePair(pair);
            update();
        });
        
        widget.keyIpt.addOnValueChangedListener((String value) ->
        {
            widget.getPair().setKey(value);
        });
        
        widget.valueIpt.addOnValueChangedListener((String value) ->
        {
            widget.getPair().setValue(value);
        });
    }
    
    private void update()
    {
        if (details != null)
        {
            nameIpt.setText(details.getName());
            locationIpt.setText(details.getLocation());
        }
        
        ArrayList<PasswordDetailsPair> list = (details != null) ? details.getList() : new ArrayList<>();
        int sz = list.size();
        int wsz = content.getComponentCount() - 1;
        
        for (int i = 0; i < sz; i++)
        {
            PairWidget widget;
            if (i < wsz)
                widget = (PairWidget) content.getComponent(i);
            else
            {
                widget = new PairWidget();
                
                addWidgetListeners(widget);
                content.add(widget, wsz);
            }
            
            widget.setPair(list.get(i));
            widget.setEditable(editBtn.isSelected());
        }
        
        for (int i = sz; i < wsz; i++)
        {
            content.remove(sz);
        }
        
        revalidate();
        repaint();
    }
    
    private void updateEditable()
    {
        editBtn.setSelected(editable);
        addBtn.setVisible(editable);

        nameIpt.setEditable(editable);
        locationIpt.setEditable(editable);
        
        Component[] childs = content.getComponents();
        int sz = childs.length;
        for (int i = 0; i < sz - 1; i++)
        {
            PairWidget widget = (PairWidget) childs[i];
            widget.setEditable(editable);
        }

        revalidate();
        repaint();
    }
}
