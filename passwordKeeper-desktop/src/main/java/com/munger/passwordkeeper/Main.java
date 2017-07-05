/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper;

import com.munger.passwordkeeper.ctrl.MenuCtrl;
import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFileImport;
import com.munger.passwordkeeper.view.*;
import com.munger.passwordkeeper.view.helper.ClickListener;
import com.munger.passwordkeeper.view.helper.KeyTypedListener;
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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
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
    public MenuCtrl menuCtrl;
    public Page currentView = null;
    
    /**
     * Creates new form Main
     */
    public Main() 
    {
        instance = this;
        
        initComponents();
        
        try
        {
            mainState = new MainState();
            menuCtrl = new MenuCtrl(this);
        }
        catch(Exception e){
            displayAlert("failed to initialize app");
            quit();
        }
        
        setupListeners();
        loadInitialView();
    }

    private void displayAlert(String message)
    {
        JOptionPane.showMessageDialog(this, message);
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
        try
        {
            PasswordDocumentDrive testDoc = new PasswordDocumentDrive();
            testDoc.fileList();
        }
        catch(Exception e){
            int i = 0; int j = i;
        }
        
        if (mainState.document.exists())
            currentView = new OpenFile();
        else
            currentView = new NewFile(false);
        
        changeView(false);
    }
    
    public void loadNewView()
    {
        currentView = new NewFile(true);
        changeView(false);
    }
    
    private final Object locker = new Object();
    private boolean loaded = false;
    
    public void loadFile(File target)
    {
        mainState.prefs.setSavePath(target.getAbsolutePath());
        currentView = new OpenFile();
    }
    
    public void openFile()
    {
        privateOpenFile(new openFileAction() 
        {
            @Override
            public void loaded() 
            {
                loadDocumentView();
            }

            @Override
            public void failed(Exception e) 
            {
                showAlert("Failed to open the document: " + mainState.document.name);
            }
        });
    }
    
    private void updateRecents()
    {
        String path = mainState.prefs.getSavePath();
        String[] recents = mainState.prefs.getRecentFiles();
        int foundIdx = -1;
        int sz = recents.length;
        for (int i = 0; i < sz; i++)
        {
            if (recents[i].equals(path))
            {
                foundIdx = i;
                break;
            }
        }

        java.util.List<String> list = java.util.Arrays.asList(recents);

        if (foundIdx > -1)
            list.remove(foundIdx);

        list.add(0, path);
        
        recents = (String[]) list.toArray();
        mainState.prefs.setRecentFiles(recents);
    }
    
    private interface openFileAction
    {
        public void loaded();
        public void failed(Exception e);
    }
    
    private void privateOpenFile(openFileAction action)
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
                action.failed(e);
                synchronized(locker)
                {
                    locker.notify();
                }
            }
        }});
        loadTask.start();
        
        synchronized(locker)
        {
            if (!loaded)
                try{locker.wait();} catch(InterruptedException e){}
        }

        mainState.document.removeLoadEvents(listener);
        
        if (loaded)
            action.loaded();
    }

    public void showAlert(String alert)
    {}
    
    public void loadDocumentView()
    {
        backStack = new ArrayList<>();
        currentView = new DocumentView();
        changeView(false);
        mainState.startQuitTimer();
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
        JOptionPane.showMessageDialog(this, new AboutView());
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
    
    public void loadPreferencesView()
    {
        currentView = new PreferencesView();
        changeView(true);
    }
    
    public void closeDocument()
    {
        doClose();
        loadInitialView();
    }
    
    public void newDocument()
    {
        doClose();
        loadNewView();
    }
    
    private void doClose()
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
        mainState.stopQuitTimer();
        
        backStack = new ArrayList<>();
    }
    
    public void enableEditActions(boolean enabled)
    {
        importItem.setEnabled(enabled);
    }
    
    public void enableDetailsActions(boolean enabled)
    {
        generatePasswordItem.setEnabled(enabled);
    }
    
    public void enableResetActions(boolean enabled)
    {
        closeItem.setEnabled(enabled);
    }
    
    public void quit()
    {
        System.exit(0);
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
        newItem = new javax.swing.JMenuItem();
        openItem = new javax.swing.JMenuItem();
        recentMenu = new javax.swing.JMenu();
        closeItem = new javax.swing.JMenuItem();
        importItem = new javax.swing.JMenuItem();
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

        newItem.setText("New ...");
        jMenu1.add(newItem);

        openItem.setText("Open ...");
        jMenu1.add(openItem);

        recentMenu.setText("Recent");
        jMenu1.add(recentMenu);

        closeItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_W, java.awt.event.InputEvent.CTRL_MASK));
        closeItem.setText("Close");
        jMenu1.add(closeItem);

        importItem.setText("Import ...");
        jMenu1.add(importItem);
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
    public javax.swing.JMenuItem aboutItem;
    public javax.swing.JMenuItem closeItem;
    public javax.swing.JMenuItem copyItem;
    public javax.swing.JMenuItem cutItem;
    public javax.swing.JMenuItem generatePasswordItem;
    public javax.swing.JMenuItem importItem;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    public javax.swing.JMenuItem newItem;
    public javax.swing.JMenuItem openItem;
    public javax.swing.JMenuItem pasteItem;
    public javax.swing.JMenuItem prefsItem;
    public javax.swing.JMenuItem quitItem;
    public javax.swing.JMenu recentMenu;
    public javax.swing.JMenuItem selectAllItem;
    // End of variables declaration//GEN-END:variables

    private void setupListeners()
    {
        getContentPane().addMouseListener(new ClickListener() {public void mouseClicked(MouseEvent e) 
        {
            mainState.resetQuitTimer();
        }});
        
        getContentPane().addMouseMotionListener(new MouseMotionListener() 
        {
            public void mouseDragged(MouseEvent e) 
            {
                mainState.resetQuitTimer();
            }

            public void mouseMoved(MouseEvent e) 
            {
                mainState.resetQuitTimer();
            }
        });
        
        getContentPane().addKeyListener(new KeyTypedListener() {public void keyTyped(KeyEvent e) 
        {
            mainState.resetQuitTimer();
        }});   
        
        mainState.prefs.addListener((Prefs.Types key) -> 
        {
            if (key == Prefs.Types.savePath)
            {
                Thread t = new Thread(() ->
                {
                    try { Thread.sleep(100); } catch (InterruptedException e) {}
                    loadInitialView();                    
                }, "reloadInitView");
                t.start();
            }
        });
    }
    
}
