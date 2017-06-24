/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.munger.passwordkeeper;

import com.munger.passwordkeeper.struct.documents.PasswordDocumentFile;

/**
 *
 * @author hallmarklabs
 */
public class MainState 
{
    public PasswordDocumentFile document;
    
    public MainState()
    {
        document = new PasswordDocumentFile("password");
        document.setRootPath("~");
    }
}
