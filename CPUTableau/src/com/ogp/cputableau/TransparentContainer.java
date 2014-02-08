package com.ogp.cputableau;

import android.content.Context;
import android.graphics.Point;
import android.widget.RelativeLayout;


public class TransparentContainer extends RelativeLayout
{
	@SuppressWarnings("unused")
	private static final 	String 	TAG					= "TransparentContainer";

	private static final 	int 	TOP_BASE			= 1;
	private static final 	int 	LEFT_BASE			= 2;
	private static final 	int		CONTENT				= 3;

	
	
	private 	TransparentContent 	clientView;
	
	private		Point				clientSize			= new Point(160,
																	50);
	
	public TransparentContainer(Context 			context, 
								TransparentContent	clientView)
	{
		super(context);

		this.clientView = clientView;
		
		clientView.setId (CONTENT);
		
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
		clientView.updateParameters (temp, 
							online);
	}


	public void errorTemp() 
	{
		clientView.setErrorParameters();
	}
}
