package com.ogp.cputableau;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.PorterDuff;


public class TransparentContent extends TransparentBase
{
	@SuppressWarnings("unused")
	private static final String 	TAG					= "TransparentContent";

	private static final String 	maxString			= "8888 MHz.";

	private static final int 		X_PRIME 			= 6;
	private static final int 		Y_PRIME 			= 6;
	private static final int 		TEXT_SIZE 			= 32;

	private static final int 		BACKGROUND_COLOR 	= 0x404040FF;
	private static final int 		GREY_COLOR 			= 0xA0A0A0A0;
	private static final int 		PAINT_0				= 0xD040F0F0;
	private static final int 		PAINT_1				= 0xD0F03030;
	private static final int 		PAINT_2				= 0xD0F0F020;


	private Paint 					overlayPaint[]			= new Paint[3];
	private Paint 					dashPaint 				= new Paint();
	private Paint 					backgroundPaint			= new Paint();	
	private String 					strTemperature 			= "no temp";
	private String 					strClock	 			= "no clock";
	private String 					strOnline	 			= "no cores";
	private int						storedIntParameter[]	= new int[2];
	private String					staredStrParameter[]	= new String[1];
	private Rect 					bounds 					= new Rect();

	private Point					contentSize				= new Point(160, 
																		55);
	private int 					tempDivider				= 0;
	
	
	public TransparentContent(Context context)
	{
		super(context);

		for (int i = 0; i < 3; i++)
		{
			overlayPaint[i] = new Paint();
			overlayPaint[i].setTextSize (TEXT_SIZE);
		}
		
		overlayPaint[0].setColor (PAINT_0);
		overlayPaint[1].setColor (PAINT_1);
		overlayPaint[2].setColor (PAINT_2);
		dashPaint.		setColor (GREY_COLOR);
		backgroundPaint.setColor (BACKGROUND_COLOR);
		
		
		getContentSize (contentSize);
		
		setErrorParameters();
		setEnabled (true);
				
		start();		
	}

	
	public boolean drawOverlay (Canvas canvas)
	{
		canvas.drawColor (0,
  		  		  		  PorterDuff.Mode.CLEAR);

		if (waitNoUpdate())
		{
			return false;
		}
		
		canvas.drawColor (BACKGROUND_COLOR);
		
			
		overlayPaint[0].getTextBounds (maxString, 
									   0, 
									   maxString.length(), 
									   bounds);

		int paint = 0;
		    
		if (-1 == storedIntParameter[0])
		{
			paint = 1;
		}
		else if (-2 == storedIntParameter[0])
		{
			paint = 2;
		}
		    
		canvas.drawText (strTemperature,
						 X_PRIME,
						 bounds.height() + Y_PRIME,
						 overlayPaint[paint]);
	
		canvas.drawText (strClock,
						 X_PRIME,
						 (bounds.height() + Y_PRIME) * 2,
						 overlayPaint[paint]);
	
		if (null == strOnline)
		{
			strOnline = "1";
		}
	
		canvas.drawText (strOnline,
						 X_PRIME,
						 (bounds.height() + Y_PRIME) * 3,
		 		 		 overlayPaint[paint]);
		
		return true;
	}


	public void setErrorParameters() 
	{
		if (-1 != storedIntParameter[0])
		{
			strTemperature = "  ???  ";
			storedIntParameter[0] = -1;
		}
		
		if (-1 != storedIntParameter[1])
		{
			strClock = "  ???  ";
			storedIntParameter[1] = -1;
		}

		strOnline = "???";
		
		refresh();
	}


	public void updateParameters (int 		parameter[], 
						 		  String 	online) 
	{
		if (0 == tempDivider
			||
			parameter[0] / tempDivider < 10
			||
			parameter[0] / tempDivider >= 100)
		{
			for (tempDivider = 1; tempDivider < 100000; tempDivider *= 10)
			{
				if (parameter[0] / tempDivider < 100)
				{
					break;
				}
			}
		}
		
		storedIntParameter[0] = parameter[0] / tempDivider;
		storedIntParameter[1] = parameter[1] / 1000;
		

		if (0 == storedIntParameter[0])
		{
			strTemperature = "???";
		}
		else
		{
			strTemperature = String.format ("%d°C", 
											storedIntParameter[0]);
		}
		strClock = String.format ("%d MHz", 
								  storedIntParameter[1]);
		
		strOnline = online;

		refresh();
	}


	public void getContentSize (Point contentSize) 
	{
	    overlayPaint[0].getTextBounds (maxString,
	    							   0, 
	    							   maxString.length(), 
	    							   bounds);

	    contentSize.x = bounds.width()      + X_PRIME * 2;
		contentSize.y = bounds.height() * 3 + Y_PRIME * 4;
	}
	
	
	@Override
	protected void drawPanelImage (Canvas canvas) 
	{
		drawOverlay (canvas);
	}
}
