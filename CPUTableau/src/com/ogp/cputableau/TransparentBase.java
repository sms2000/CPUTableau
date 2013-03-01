package com.ogp.cputableau;

import android.content.Context;
import android.widget.RelativeLayout;


public class TransparentBase extends RelativeLayout
{
	private static final String TAG					= "TransparentBase";

	
	public TransparentBase(Context context)
	{
		super(context);
		
	    setBackgroundColor (0x01000000);
	}
}
