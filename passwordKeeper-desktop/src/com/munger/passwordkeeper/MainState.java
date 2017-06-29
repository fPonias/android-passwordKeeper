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
        openDoc();
    }
    
    public void openDoc()
    {
        openDoc("password");
    }
    
    public void openDoc(String name)
    {
        document = new PasswordDocumentFile("password");
        document.setRootPath("/Users/hallmarklabs/pw-tmp/");
    }
    
    public void setupDriveHelper()
    {}
}
