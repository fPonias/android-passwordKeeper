/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper.view;

import javax.swing.JPanel;

/**
 *
 * @author hallmarklabs
 */
public abstract class Page extends JPanel
{
    public String onCopy() {return null;}
    public String onCut() {return null;}
    public void onPaste(String value) {}
    public void onSelectAll() {}
}
