/**
 * Copyright 2014 Cody Munger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.munger.passwordkeeper.struct;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.dropbox.sync.android.DbxAccount;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.dropbox.sync.android.DbxException.Unauthorized;
import com.munger.passwordkeeper.MainActivity;

public class PasswordDocumentDropbox extends PasswordDocument 
{
	private DbxAccount account;
	private DbxPath path;
	private DbxFileSystem dbfs;
	
	public PasswordDocumentDropbox(MainActivity c, String name)
	{
		super(c, name);
		
		account = c.getDropboxAccount();
		path = new DbxPath(name);
		
		try
		{
			dbfs = DbxFileSystem.forAccount(account);
		}
		catch(Unauthorized e){
			dbfs = null;
		}
	}
	
	public PasswordDocumentDropbox(MainActivity c, String name, String password)
	{
		this(c, name);
		setPassword(password);
	}
	
	public void save() 
	{
		DbxFile file = null;
		try
		{
			if (!dbfs.exists(path))
			{
				file = dbfs.create(path);
			}
			else
			{
				file = dbfs.open(path);
			}
			
			String data = toString(true);
			file.writeString(data);
		}
		catch(DbxException e1){
			
		}
		catch(IOException e2){
			
		}
		finally
		{
			if (file != null)
				file.close();
		}
	}

	public void load(boolean force) 
	{
		DbxFile file = null;
		try
		{
			if (!dbfs.exists(path))
			{
				this.details = new ArrayList<PasswordDetails>();
				return;
			}
			
			DbxFileInfo info = dbfs.getFileInfo(path);
			if (info.modifiedTime.getTime() < lastLoad && !force)
				return;
			
			file = dbfs.open(path);
			String encoded = file.readString();
			
			fromString(encoded, true);
			file.close();
		}
		catch(DbxException e1){
			
		}
		catch(IOException e2){
			
		}
		finally{
			if (file != null)
				file.close();
		}
	}

	public void delete() 
	{
		try
		{
			if (dbfs.exists(path))
				dbfs.delete(path);
		}
		catch(DbxException e2){
			
		}
	}

	public boolean testPassword() 
	{
		boolean ret = false;
		DbxFile file = null;
		
		try
		{
			if (dbfs.exists(path))
			{
				file = dbfs.open(path);
				String encoded = file.readString();
				String[] lines = encoded.split("\n");
				
				if (lines.length > 0)
				{
					String decoded = encoder.decode(lines[0]);
					if (decoded.equals("test string"))
						ret = true;
				}
			}
		}
		catch(DbxException e1){
			
		}
		catch(IOException e2){
			
		}
		finally{
			if (file != null)
				file.close();
		}
		
		return ret;
	}
	
	public PasswordDocumentDropbox convertLocal(PasswordDocumentFile file)
	{
		PasswordDocumentDropbox ret = new PasswordDocumentDropbox(file.context, file.name);
		ret.encoder = file.encoder;
		String contents = file.toString();
		ret.fromString(contents, false);
		
		return ret;
	}
	
	public static ArrayList<PasswordDocument> getList(MainActivity c)
	{
		ArrayList<PasswordDocument> ret = new ArrayList<PasswordDocument>();
		
		try
		{
			DbxFileSystem dbfs = DbxFileSystem.forAccount(c.getDropboxAccount());
			List<DbxFileInfo> files = dbfs.listFolder(new DbxPath(""));
			
			for (DbxFileInfo f : files)
			{
				PasswordDocument item = new PasswordDocumentDropbox(c, f.path.getName());
				ret.add(item);
			}
		}
		catch(Unauthorized e){
			
		}
		catch(DbxException e2){
			
		}
			
		return ret;
	}
}
