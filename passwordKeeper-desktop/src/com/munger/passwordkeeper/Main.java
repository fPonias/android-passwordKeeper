/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper;

import com.munger.passwordkeeper.struct.PasswordDetails;
import com.munger.passwordkeeper.struct.documents.PasswordDocument;
import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;
import com.munger.passwordkeeper.view.*;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import javax.swing.JPanel;

/**
 *
 * @author hallmarklabs
 */
public class Main extends javax.swing.JFrame 
{
    public static Main instance;
    
    public MainState mainState;
    public JPanel currentView = null;
    
    /**
     * Creates new form Main
     */
    public Main() 
    {
        instance = this;
        mainState = new MainState();
        
        initComponents();
        loadInitialView();
    }

    private ArrayList<Component> backStack = new ArrayList<>();
    
    private void changeView(boolean pushStack)
    {
        Container root = getContentPane();
        
        if (pushStack && root.getComponentCount() > 0)
        {
            Component oldView = root.getComponent(0);
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
        
        currentView = (JPanel) backStack.get(sz - 1);
        backStack.remove(sz - 1);
        
        changeView(false);
    }
    
    public void loadInitialView()
    {
        if (mainState.document.exists())
        {
            PasswordDetails dets = new PasswordDetails();
            loadDetailsView(dets);
        }
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
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(500, 700));

        pack();
    }// </editor-fold>//GEN-END:initComponents

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
    // End of variables declaration//GEN-END:variables
}
