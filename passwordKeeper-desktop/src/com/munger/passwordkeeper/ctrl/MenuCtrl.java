/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper.ctrl;

import com.munger.passwordkeeper.Main;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JRootPane;
import javax.swing.text.JTextComponent;

/**
 *
 * @author hallmarklabs
 */
public class MenuCtrl 
{
    private Main target;
    
    public MenuCtrl(Main target)
    {
        this.target = target;
        initializeListeners();
        populateItems();
    }
    
    private void initializeListeners()
    {
        target.newItem.addActionListener((ActionEvent e) ->
        {
            target.loadNewView();
        });
        
        target.openItem.addActionListener((ActionEvent e) ->
        {
            String path = Main.instance.mainState.document.getRootPath();
            JFileChooser chooser = new JFileChooser(new File(path));
            chooser.showDialog(target, "Open");
            
            File chosen = chooser.getSelectedFile();
            if (chosen == null)
                return;
            
            Main.instance.loadFile(chosen);
        });
        
        target.importItem.addActionListener((ActionEvent e) ->
        {
            target.loadImportView();
        });
        
        target.prefsItem.addActionListener((ActionEvent e) ->
        {
            target.loadPreferencesView();
        });
        
        target.aboutItem.addActionListener((ActionEvent e) -> 
        {
            target.loadAboutView();
        });
        
        target.closeItem.addActionListener((ActionEvent e) ->
        {
            target.closeDocument();
        });
        
        target.quitItem.addActionListener((ActionEvent e) ->
        {
            target.quit();
        });
        
        target.copyItem.addActionListener((ActionEvent e) ->
        {
            Component c = target.getFocusOwner();
            if (c instanceof JTextComponent)
            {
                ((JTextComponent) c).copy();
            }
            else if (lastFocused instanceof JTextComponent)
            {
                ((JTextComponent)  lastFocused).copy();
            }
            else
            {
                String copied = target.currentView.onCopy();
                putInClipboard(copied);
            }
        });
        
        target.cutItem.addActionListener((ActionEvent e) ->
        {
            Component c = target.getFocusOwner();
            if (c instanceof JTextComponent)
            {
                ((JTextComponent) c).cut();
            }
            else if (lastFocused instanceof JTextComponent)
            {
                ((JTextComponent)  lastFocused).cut();
            }
            else
            {
                String cut = target.currentView.onCut();
                putInClipboard(cut);
            }
        });
        
        target.pasteItem.addActionListener((ActionEvent e) ->
        {
            Component c = target.getFocusOwner();
            if (c instanceof JTextComponent)
            {
                ((JTextComponent) c).paste();
            }
            else if (lastFocused instanceof JTextComponent)
            {
                ((JTextComponent) lastFocused).paste();
            }
            else
            {
                String paste = getFromClipboard();
                target.currentView.onPaste(paste);
            }
        });
        
        target.selectAllItem.addActionListener((ActionEvent e) ->
        {
            Component c = target.getFocusOwner();
            if (c instanceof JTextComponent)
            {
                ((JTextComponent) c).selectAll();
            }
            else if (lastFocused instanceof JTextComponent)
            {
                ((JTextComponent)  lastFocused).selectAll();
            }
            else
            {
                target.currentView.onSelectAll();
            }
        });
        
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener((PropertyChangeEvent evt) -> 
        {
            Object old = evt.getOldValue();
            Object neu = evt.getNewValue();
            
            if (neu == null && old != null)
                lastFocused = old;
            else if (neu instanceof JRootPane)
            {}
            else
                lastFocused = null;
        });
    }
    
    private Object lastFocused;
    
    private void putInClipboard(String val)
    {
        if (val == null)
            return;
        
        StringSelection sel = new StringSelection(val);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(sel, sel);
    }
    
    private String getFromClipboard()
    {      
        try
        {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable t = clipboard.getContents(null);
            String ret = (String) t.getTransferData(DataFlavor.stringFlavor);
            return ret;
        }
        catch(HeadlessException | UnsupportedFlavorException | IOException e) {
            return "";
        }
    }
    
    private class RecentItemListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e) 
        {
            JMenuItem source = (JMenuItem) e.getSource();
            String path = source.getText();
            
            Main.instance.mainState.prefs.setSavePath(path);
        }
    }
    
    private RecentItemListener recentItemListener = new RecentItemListener();
    
    private void populateItems()
    {
        String[] recents = Main.instance.mainState.prefs.getRecentFiles();
        
        int mysz = target.recentMenu.getItemCount();
        int sz = recents.length;
        for (int i = 0; i < sz; i++)
        {
            JMenuItem item;
            if (i < mysz)
            {
                item = target.recentMenu.getItem(i);
                item.setEnabled(true);
            }
            else
            {
                item = new JMenuItem();
                item.addActionListener(recentItemListener);
                target.recentMenu.add(item);
            }
            
            item.setText(recents[i]);
        }
        
        for (int i = sz; i < mysz; i++)
        {
            JMenuItem item = target.recentMenu.getItem(i);
            item.removeActionListener(recentItemListener);
            target.recentMenu.remove(item);
        }
        
        if (sz == 0)
        {
            JMenuItem item = new JMenuItem("No recent loads available");
            item.setEnabled(false);
            target.recentMenu.add(item);
        }
    }
}
