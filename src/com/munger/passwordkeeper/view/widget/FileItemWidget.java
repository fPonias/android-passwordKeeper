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

package com.munger.passwordkeeper.view.widget;

import com.munger.passwordkeeper.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A widget composed of an icon representing file type and a label showing the file name.
 * @author codymunger
 *
 */
public class FileItemWidget extends LinearLayout 
{
	protected ImageView icon;
	protected TextView label;
	
	public FileItemWidget(Context context) 
	{
		super(context, null, 0);
		
		LayoutInflater inflater = LayoutInflater.from(context);
		inflater.inflate(R.layout.widget_fileitem, this, true);
		
		icon = (ImageView) this.findViewById(R.id.fileitem_icon);
		label = (TextView) this.findViewById(R.id.fileitem_label);
	}
	
	public void setLabel(String name)
	{
		label.setText(name);
	}

	/**
	 * display the specified icon or nothing
	 * @param d the icon or null if you want transparent
	 */
	public void setIcon(Drawable d)
	{
		if (d != null)
			icon.setImageDrawable(d);
		else
			icon.setImageResource(0);
	}
}
