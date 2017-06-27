/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import net.miginfocom.swing.MigLayout;

/**
 *
 * @author hallmarklabs
 */
public class DetailsView extends JPanel
{
    public DetailsView()
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
}
