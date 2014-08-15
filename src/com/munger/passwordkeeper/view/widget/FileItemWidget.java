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
