/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper.view.helper;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 *
 * @author hallmarklabs
 */
public abstract class KeyTypedListener implements KeyListener
{

    @Override
    public abstract void keyTyped(KeyEvent e);

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}
    
}
