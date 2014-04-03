package com.ogp.cputableau;

import android.view.WindowManager;


public interface ServiceInterface 
{
	public WindowManager			getWindowManager();
	public PointF 					loadDefaultXY	();
	public void 					saveDefaultXY	(float x, float y);
	
}
