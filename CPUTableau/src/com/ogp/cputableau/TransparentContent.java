package com.ogp.cputableau;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.PorterDuff;
import android.util.Log;


@SuppressLint("DefaultLocale")
public class TransparentContent extends TransparentBase
{
	private static final String 	TAG					= "TransparentContent";

	private static final String 	maxString			= "99bC / 99.9bC";

	private static final int 		X_PRIME 			= 6;
	private static final int 		Y_PRIME 			= 6;
	private static final int 		TEXT_SIZE 			= 32;

	private static final int 		BACKGROUND_COLOR 	= 0x00707040;
	private static final int 		GREY_COLOR 			= 0xA0A0A0A0;
	private static final int 		PAINT_0				= 0xD040F0F0;
	private static final int 		PAINT_1				= 0xD0F03030;
	private static final int 		PAINT_2				= 0xD0F0F020;

	private static final int 		MAX_PARAMS 			= 3;


	private Paint 					overlayPaint[]			= new Paint[3];
	private Paint 					dashPaint 				= new Paint();
	private Paint 					backgroundPaint			= new Paint();	
	private String 					strTemperature 			= "no temp";
	private String 					strClock	 			= "no clock";
	private String 					strOnline	 			= "no cores";
	private String 					strCharge				= null;
	private int						storedIntParameter[]	= new int[MAX_PARAMS];
	private Rect 					bounds 					= new Rect();

	private Point					contentSize				= new Point(160, 
																		55);
	private int 					tempDivider				= 0;

	private TransparentContentCallback transparentContentCallback = null;

	
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
		
		canvas.drawColor (BACKGROUND_COLOR | (StateMachine.getTransparency() << 24));
		
			
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
		
		if (null != strCharge)
		{
			canvas.drawText (strCharge,
					 		 X_PRIME,
					 		 (bounds.height() + Y_PRIME) * 4,
					 		 overlayPaint[paint]);
		}
		
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
		strCharge = null;
		
		refresh (true);
	}


	@SuppressLint("DefaultLocale")
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
		storedIntParameter[2] = parameter[2];
				

		if (0 == storedIntParameter[0])
		{
			strTemperature = "???";
		}
		else
		{
			if (0.0f < StateMachine.getBatteryTemp())
			{
				strTemperature = String.format ("%d�C / %.1f�C", 
												storedIntParameter[0],
												StateMachine.getBatteryTemp());
			}
			else
			{
				strTemperature = String.format ("%d�C", 
												storedIntParameter[0]);
			}
		}
		
		strClock = String.format ("%d MHz", 
								  storedIntParameter[1]);
		
		strOnline = online;

		
		String strNewCharge = storedIntParameter[2] <= 0 ? null : String.format ("%d mA", storedIntParameter[2]);
		boolean overlaySizeChanged = (strNewCharge == null) != (strCharge == null);
		strCharge = strNewCharge;

		if (overlaySizeChanged)
		{
			Log.i(TAG, "updateParameters. Overlay size changed.");
		}
		
		if (StateMachine.getExtensiveDebug())
		{
			Log.v(TAG, String.format ("updateParameters. Charge: %s.",
									  null != strNewCharge ? strNewCharge : "<no charge>"));
		}
		
		refresh (overlaySizeChanged);
	}


	public void getContentSize (Point contentSize) 
	{
	    overlayPaint[0].getTextBounds (maxString,
	    							   0, 
	    							   maxString.length(), 
	    							   bounds);

	    contentSize.x = bounds.width() + X_PRIME * 2;
	    
	    if (strCharge == null)
	    {
	    	contentSize.y = bounds.height() * 3 + Y_PRIME * 4; 
	    }
	    else
	    {
	    	contentSize.y = bounds.height() * 4 + Y_PRIME * 5;
	    }
	}
	
	
	@Override
	protected void drawPanelImage (Canvas canvas) 
	{
		drawOverlay (canvas);
	}

	
	public void refresh (boolean overlaySizeChanged)
    {
		if (overlaySizeChanged && transparentContentCallback != null) 
		{
			transparentContentCallback.contentSizeChanged();
		}
		
		super.refresh();
    }


	public void setContentCallback (TransparentContentCallback transparentContentCallback) 
	{
		this.transparentContentCallback = transparentContentCallback;
	}
}
