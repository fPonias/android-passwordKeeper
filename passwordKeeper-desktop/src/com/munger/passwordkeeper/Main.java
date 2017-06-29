/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper;

import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFileImport;
import com.munger.passwordkeeper.view.*;
import java.awt.Component;
import java.awt.Container;
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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.text.JTextComponent;

/**
 *
 * @author hallmarklabs
 */
public class Main extends javax.swing.JFrame 
{
    public static Main instance;
    
    public MainState mainState;
    public Page currentView = null;
    
    /**
     * Creates new form Main
     */
    public Main() 
    {
        instance = this;
        mainState = new MainState();
        
        initComponents();
        setupListeners();
        loadInitialView();
    }

    private ArrayList<Page> backStack = new ArrayList<>();
    
    private void changeView(boolean pushStack)
    {
        Container root = getContentPane();
        
        if (pushStack && root.getComponentCount() > 0)
        {
            Page oldView = (Page) root.getComponent(0);
            backStack.add(oldView);
        }
        
        root.removeAll();
        root.add(currentView, java.awt.BorderLayout.CENTER);
        root.revalidate();
        root.repaint();
    }
    
    public void goBack()
    {
        int sz = backStack.size();
        if (sz == 0)
            return;
        
        currentView = backStack.get(sz - 1);
        backStack.remove(sz - 1);
        
        changeView(false);
    }
    
    public void loadInitialView()
    {
        if (mainState.document.exists())
            currentView = new OpenFile();
        else
            currentView = new NewFile();
        
        changeView(false);
    }
    
    private final Object locker = new Object();
    private boolean loaded = false;
    
    public void openFile()
    {        
        final PasswordDocument.ILoadEvents listener = new PasswordDocumentFile.ILoadEvents() {
            @Override
            public void detailsLoaded()
            {
                synchronized(locker)
                {
                    loaded = true;
                    locker.notify();
                }
            }

            @Override
            public void historyLoaded()
            {
                mainState.setupDriveHelper();
            }

            @Override
            public void historyProgress(float progress) {

            }
        };
        mainState.document.addLoadEvents(listener);
        
        Thread loadTask = new Thread(new Runnable() {public void run() 
        {
            try
            {
                mainState.document.load(true);
            }
            catch(Exception e){
                showAlert("Failed to open the document: " + mainState.document.name);
            }
        }});
        loadTask.start();
        
        synchronized(locker)
        {
            if (!loaded)
                try{locker.wait();} catch(InterruptedException e){}
        }

        mainState.document.removeLoadEvents(listener);
        loadDocumentView();
    }

    public void showAlert(String alert)
    {}
    
    public void loadDocumentView()
    {
        backStack = new ArrayList<>();
        currentView = new DocumentView();
        changeView(false);
    }
    
    public void loadDetailsView(PasswordDetails dets)
    {
        currentView = new DetailsView();

        if (dets.getName().length() == 0)
        {
            ((DetailsView) currentView).setEditable(true);
            
            if (dets.count() == 0)
                dets.addEmptyPair();
        }
        
        ((DetailsView) currentView).setDetails(dets);

        changeView(true);
    }
    
    public void loadAboutView()
    {
        currentView = new AboutView();
        changeView(true);
    }
    
    public void loadImportView()
    {
        JFileChooser chooser = new JFileChooser();
        int returnVal = chooser.showOpenDialog(this);
        
        if (returnVal != JFileChooser.APPROVE_OPTION)
            return;
        
        String path = chooser.getSelectedFile().getAbsolutePath();
        PasswordDocumentFileImport fileImport = new PasswordDocumentFileImport(path, "import");
        
        try
        {
            fileImport.load(true);
            
            if (mainState.document.count() == 1)
            {
                PasswordDetails dets = mainState.document.getDetails(0);
                if (dets.getName().length() == 0 && dets.getLocation().length() == 0)
                    mainState.document.removeDetails(dets);
            }
            
            mainState.document.playSubHistory(fileImport.getHistory());
            mainState.document.save();
        } catch(Exception e){
            showAlert("Failed to import " + path);
        }
    }
    
    public void loadExportView()
    {
        JFileChooser chooser = new JFileChooser();
        int returnVal = chooser.showOpenDialog(this);
        
        if (returnVal != JFileChooser.APPROVE_OPTION)
            return;
        
        File selectedFile = chooser.getSelectedFile();
        String rootDir = selectedFile.getParentFile().getAbsolutePath();
        String name = selectedFile.getName();
        String oldRootDir = mainState.document.getRootPath();
        String oldName = mainState.document.name;
        
        try
        {
            mainState.document.name = name;
            mainState.document.setRootPath(rootDir + "/");
            mainState.document.save();
        }
        catch(Exception e){
            showAlert("Failed to export " + rootDir + "/" + name);
        }
        
        mainState.document.name = oldName;
        mainState.document.setRootPath(oldRootDir);
    }
    
    public void loadPreferencesView()
    {
        
    }
    
    public void closeDocument()
    {
        try
        {
            mainState.document.close();
        }
        catch(Exception e){
            
        }
        
        synchronized(locker)
        {
            loaded = false;
        }
        
        mainState.openDoc();
        
        backStack = new ArrayList<>();
        loadInitialView();
    }
    
    public void enableEditActions(boolean enabled)
    {
        importItem.setEnabled(enabled);
        exportItem.setEnabled(enabled);
    }
    
    public void enableDetailsActions(boolean enabled)
    {
        generatePasswordItem.setEnabled(enabled);
    }
    
    public void enableResetActions(boolean enabled)
    {
        closeItem.setEnabled(enabled);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        closeItem = new javax.swing.JMenuItem();
        importItem = new javax.swing.JMenuItem();
        exportItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        quitItem = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        cutItem = new javax.swing.JMenuItem();
        copyItem = new javax.swing.JMenuItem();
        pasteItem = new javax.swing.JMenuItem();
        selectAllItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        generatePasswordItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        prefsItem = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        aboutItem = new javax.swing.JMenuItem();

        jMenuItem2.setText("jMenuItem2");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(500, 700));

        jMenu1.setText("File");

        closeItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_MASK));
        closeItem.setText("Close");
        jMenu1.add(closeItem);

        importItem.setText("Import ...");
        jMenu1.add(importItem);

        exportItem.setText("Export ...");
        jMenu1.add(exportItem);
        jMenu1.add(jSeparator3);

        quitItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        quitItem.setMnemonic('Q');
        quitItem.setText("Quit");
        quitItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitItemActionPerformed(evt);
            }
        });
        jMenu1.add(quitItem);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");

        cutItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_MASK));
        cutItem.setMnemonic('u');
        cutItem.setText("Cut");
        jMenu2.add(cutItem);

        copyItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.CTRL_MASK));
        copyItem.setMnemonic('o');
        copyItem.setText("Copy");
        jMenu2.add(copyItem);

        pasteItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_MASK));
        pasteItem.setMnemonic('p');
        pasteItem.setText("Paste");
        pasteItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pasteItemActionPerformed(evt);
            }
        });
        jMenu2.add(pasteItem);

        selectAllItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
        selectAllItem.setMnemonic('a');
        selectAllItem.setText("Select All");
        jMenu2.add(selectAllItem);
        jMenu2.add(jSeparator2);

        generatePasswordItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.CTRL_MASK));
        generatePasswordItem.setMnemonic('g');
        generatePasswordItem.setText("Generate Password");
        generatePasswordItem.setToolTipText("");
        jMenu2.add(generatePasswordItem);
        jMenu2.add(jSeparator1);

        prefsItem.setMnemonic('p');
        prefsItem.setText("Preferences ...");
        jMenu2.add(prefsItem);

        jMenuBar1.add(jMenu2);

        jMenu3.setText("Help");

        aboutItem.setText("About ...");
        jMenu3.add(aboutItem);

        jMenuBar1.add(jMenu3);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void quitItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quitItemActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_quitItemActionPerformed

    private void pasteItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pasteItemActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_pasteItemActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Main().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutItem;
    private javax.swing.JMenuItem closeItem;
    private javax.swing.JMenuItem copyItem;
    private javax.swing.JMenuItem cutItem;
    private javax.swing.JMenuItem exportItem;
    private javax.swing.JMenuItem generatePasswordItem;
    private javax.swing.JMenuItem importItem;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JMenuItem pasteItem;
    private javax.swing.JMenuItem prefsItem;
    private javax.swing.JMenuItem quitItem;
    private javax.swing.JMenuItem selectAllItem;
    // End of variables declaration//GEN-END:variables

    private void setupListeners()
    {
        importItem.addActionListener((ActionEvent e) ->
        {
            loadImportView();
        });
        
        exportItem.addActionListener((ActionEvent e) ->
        {
            loadExportView();
        });
        
        prefsItem.addActionListener((ActionEvent e) ->
        {
            loadPreferencesView();
        });
        
        aboutItem.addActionListener((ActionEvent e) -> 
        {
            loadAboutView();
        });
        
        closeItem.addActionListener((ActionEvent e) ->
        {
            closeDocument();
        });
        
        quitItem.addActionListener((ActionEvent e) ->
        {
            System.exit(0);
        });
        
        copyItem.addActionListener((ActionEvent e) ->
        {
            Component c = getFocusOwner();
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
                String copied = currentView.onCopy();
                putInClipboard(copied);
            }
        });
        
        cutItem.addActionListener((ActionEvent e) ->
        {
            Component c = getFocusOwner();
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
                String cut = currentView.onCut();
                putInClipboard(cut);
            }
        });
        
        pasteItem.addActionListener((ActionEvent e) ->
        {
            Component c = getFocusOwner();
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
                currentView.onPaste(paste);
            }
        });
        
        selectAllItem.addActionListener((ActionEvent e) ->
        {
            Component c = getFocusOwner();
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
                currentView.onSelectAll();
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
}
