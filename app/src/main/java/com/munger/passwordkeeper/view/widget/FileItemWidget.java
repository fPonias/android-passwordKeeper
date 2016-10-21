package com.munger.passwordkeeper.view.widget;

import com.munger.passwordkeeper.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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

	public void setIcon(Drawable d)
	{
		if (d != null)
			icon.setImageDrawable(d);
		else
			icon.setImageResource(0);
	}
}
