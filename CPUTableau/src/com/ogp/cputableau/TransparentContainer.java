package com.ogp.cputableau;

import android.content.Context;
import android.graphics.Point;
import android.widget.RelativeLayout;


public class TransparentContainer extends RelativeLayout
{
	private static final 	String 	TAG					= "TransparentContainer";

	private static final 	int 	TOP_BASE			= 1;
	private static final 	int 	LEFT_BASE			= 2;
	private static final 	int		CONTENT				= 3;

	
	
	private 	TransparentContent 	clientView;
	private		TransparentBase		topBase;
	private		TransparentBase		leftBase;
	private		LayoutParams		topParams;	
	private		LayoutParams		leftParams;	
	
	private		Point				clientSize			= new Point(160,
																	50);
	
	public TransparentContainer(Context 			context, 
								TransparentContent	clientView)
	{
		super(context);

		this.clientView = clientView;
		
		topBase  = new TransparentBase(context);
		leftBase = new TransparentBase(context);

		topBase.   setId (TOP_BASE);
		leftBase.  setId (LEFT_BASE);
		clientView.setId (CONTENT);
		
		topParams  = new LayoutParams (1, 
									   1);
		leftParams = new LayoutParams (1, 
									   1);
		
		addView (topBase,
				 topParams);
		
		leftParams.addRule (RelativeLayout.BELOW, 
							TOP_BASE);
		
		addView (leftBase,
				 leftParams);

		LayoutParams params = new RelativeLayout.LayoutParams (clientSize.x, 
				  											   clientSize.y); 
		params.addRule (RelativeLayout.BELOW, 
						TOP_BASE);
		params.addRule (RelativeLayout.RIGHT_OF, 
						LEFT_BASE);
		
		addView (clientView,
				 params);
	}


	public void setTemp (int 	temp[], 
						 String online) 
	{
		clientView.setTemp (temp, 
							online);
	}


	public void errorTemp() 
	{
		clientView.errorTemp();
	}
}
