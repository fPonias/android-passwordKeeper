/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper.view;

import com.munger.passwordkeeper.Main;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.history.PasswordDocumentHistory;
import com.munger.passwordkeeper.view.helper.ClickListener;
import com.munger.passwordkeeper.view.widget.DetailWidget;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author hallmarklabs
 */
public class DocumentView extends Page
{
    public DocumentView()
    {
        init();
        setupListeners();
        checkDocument();
        update();
    }
    
    private JTextField searchInput;
    private JToggleButton editBtn;
    private JButton addBtn;
    private JPanel content;
    
    private void init()
    {
        MigLayout layout = new MigLayout("wrap 1,insets 0", "[grow,fill]");
        setLayout(layout);
        
        JPanel topBar = new JPanel();
        topBar.setLayout(new MigLayout("insets 0","10[]10[grow]10[]10", "10[]5"));
        topBar.add(new JLabel("Search"));
        searchInput = new JTextField();
        topBar.add(searchInput, "growx");
        editBtn = new JToggleButton("edit");
        topBar.add(editBtn);
        
        add(topBar);
        
        JPanel contentOuter = new JPanel();
        contentOuter.setLayout(new BorderLayout());
        content = new JPanel();
        content.setLayout(new MigLayout("wrap 1,gap 1 1,insets 10 0 0 0", "10[grow,fill]10", ""));
        contentOuter.add(content, BorderLayout.NORTH);
        
        addBtn = new JButton("+");
        addBtn.setVisible(false);
        addBtn.setPreferredSize(new Dimension(30, 30));
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
            boolean editable = (editBtn.isSelected()) ? true : false;
            
            addBtn.setVisible(editable);
            
            Component[] childs = content.getComponents();
            int sz = childs.length;
            for (int i = 0; i < sz - 1; i++)
            {
                DetailWidget widget = (DetailWidget) childs[i];
                widget.setEditable(editable);
            }
            
            revalidate();
            repaint();
        });
        
        addBtn.addActionListener((ActionEvent e) -> {
            PasswordDocument doc = Main.instance.mainState.document;
            PasswordDetails dets = new PasswordDetails();
            try 
            {
                doc.addDetails(dets);
                doc.save();
            }
            catch(Exception e2)
            {}
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
        
        searchInput.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) 
            {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) 
            {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) 
            {
                update();
            }
        });
    }
    
    private void addWidgetListeners(DetailWidget widget)
    {
        widget.deleteBtn.addActionListener((ActionEvent e) -> {
            PasswordDetails dets = widget.getDetails();
            PasswordDocument doc = Main.instance.mainState.document;
            doc.removeDetails(dets);
            update();
        });
        
        widget.nameLbl.addMouseListener(new ClickListener() {public void mouseClicked(MouseEvent e) 
        {
            Main.instance.loadDetailsView(widget.getDetails());
        }});
    }
    
    private void checkDocument()
    {
        PasswordDocument doc = Main.instance.mainState.document;
        
        int sz = doc.count();
        if (sz == 0)
        {
            PasswordDetails dets = new PasswordDetails();
            try {doc.addDetails(dets);}catch(PasswordDocumentHistory.HistoryPlaybackException e){}
            sz++;
        }
    }
    
    private void update()
    {
        ArrayList<PasswordDetails> list = filterDetails();
        int sz = list.size();
        int wsz = content.getComponentCount() - 1;
        
        for (int i = 0; i < sz; i++)
        {
            DetailWidget widget;
            if (i < wsz)
                widget = (DetailWidget) content.getComponent(i);
            else
            {
                widget = new DetailWidget();
                
                addWidgetListeners(widget);
                content.add(widget, wsz);
            }
            
            widget.setDetails(list.get(i));
            widget.setEditable(editBtn.isSelected());
        }
        
        for (int i = sz; i < wsz; i++)
        {
            content.remove(sz);
        }
        
        revalidate();
        repaint();
    }
    
    private ArrayList<PasswordDetails> filterDetails()
    {
        PasswordDocument doc = Main.instance.mainState.document;
        String searchStr = searchInput.getText().toLowerCase();
        
        if (searchStr.length() == 0)
            return doc.getDetailsList();
        
        ArrayList<PasswordDetails> ret = new ArrayList<>();
        ArrayList<PasswordDetails> orig = doc.getDetailsList();
        for(PasswordDetails det : orig)
        {
            if (det.getName().toLowerCase().contains(searchStr))
            {
                ret.add(det);
            }
        }
        
        return ret;
    }
}
