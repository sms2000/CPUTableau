package com.ogp.cputableau;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;


public class TransparentContent extends TransparentBase implements View.OnTouchListener
{
	private static final String 	TAG					= "TransparentContent";

	private static final String 	maxString			= "1300 MHz";

	private static final int 		X_PRIME 			= 6;
	private static final int 		Y_PRIME 			= 6;
	private static final int 		TEXT_SIZE 			= 32;

	private static final long 		CLICK_TIME 			= 300;
	

	private Paint 					overlayPaint[]		= new Paint[3];
	private Paint 					dashPaint 			= new Paint();
	private String 					strTemperature 		= "???";
	private String 					strClock	 		= "???";
	private String 					strOnline	 		= "???";
	private int						tempStored[]		= new int[2];
	private Rect 					bounds 				= new Rect();
	private	Point					coords				= new Point();
	private	boolean					motionHappen		= false;
	private CPUTableauService		service;
	private WindowManager  			windowManager		= null;	
	private int						widthDisplay;
	private int						heightDisplay;
	private Point					contentSize			= new Point(160, 
																	55);
	private int 					halfWidth			= 2;
	private int 					halfHeight			= 2;

	private int 					tempDivider			= 0;

	private long 					downTime			= 0;
	private long 					lastClickTime		= 0;
	

	private class InitiateActivity implements Runnable
	{
		public void run() 
		{
			initiateActivity();
		}
	}
	

	public TransparentContent(CPUTableauService 		 service)
	{
		super(service);

		this.service		= service;
		this.windowManager	= (WindowManager)service.getSystemService (Context.WINDOW_SERVICE);


		widthDisplay  = windowManager.getDefaultDisplay().getWidth();
		heightDisplay = windowManager.getDefaultDisplay().getHeight();
		
		for (int i = 0; i < 3; i++)
		{
			overlayPaint[i] = new Paint();
			overlayPaint[i].setTextSize (TEXT_SIZE);
		}
		
		overlayPaint[0].setARGB (0xD0,
							  	 0x40,
							     0xF0,
							     0xF0);

		overlayPaint[1].setARGB (0xD0,
							  	 0xF0,
							     0x30,
							     0x30);

		overlayPaint[2].setARGB (0xD0,
							  	 0xF0,
							     0xF0,
							     0x20);

		dashPaint.setARGB (0xA0,
						   0xA0,
					       0xA0,
						   0xA0);
		
		setOnTouchListener (this);
		
		
		getContentSize (contentSize);
		
		
		float X = service.loadDefaultX();
		float Y = service.loadDefaultY();
		
		
		windowManager.addView (this,
							   new WindowManager.LayoutParams(contentSize.x,
									   						  contentSize.y,
									   						  (int)(X * widthDisplay),
									   						  (int)(Y * heightDisplay),
									   						  WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
						                					  WindowManager.LayoutParams.FLAG_FULLSCREEN 		|
					                					  	  WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS	|
						                					  WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN 	|
						                					  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
						                					  PixelFormat.TRANSLUCENT));
		
	}


	@Override
	protected void onDraw (Canvas canvas)
	{
		drawOverlay (canvas);
	}

	
	@Override
	protected void onLayout (boolean 	changed, 
							 int 		l, 
							 int 		t, 
							 int 		r, 
							 int 		b)
	{
		super.onLayout (changed, 
						l, 
						t, 
						r, 
						b);
		
	    if (widthDisplay != windowManager.getDefaultDisplay().getWidth())
	    {
	    	invalidate();
	    }
	}
	
	
	public boolean drawOverlay (Canvas canvas)
	{
	    overlayPaint[0].getTextBounds ("1300 MHz", 
									   0, 
									   strTemperature.length(), 
									   bounds);

	    halfWidth  = bounds.width()  / 2;
	    halfHeight = bounds.height() / 2;
	    
	    if (widthDisplay != windowManager.getDefaultDisplay().getWidth())
	    {
	    	widthDisplay  = windowManager.getDefaultDisplay().getWidth();
	    	heightDisplay = windowManager.getDefaultDisplay().getHeight();
	    	
			WindowManager.LayoutParams params = (WindowManager.LayoutParams)getLayoutParams();
			
			float X = service.loadDefaultX();
			float Y = service.loadDefaultY();
			
			params.x = (int)(X * widthDisplay);
			params.y = (int)(Y * heightDisplay);
			
			windowManager.updateViewLayout (this, 
											params);
	    }
	    
	    
	    setBackgroundColor (0x50C000C0);
		
	    int paint = 0;
	    
	    if (-1 == tempStored[0])
	    {
	    	paint = 1;
	    }
	    else if (-2 == tempStored[0])
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


	public void errorTemp() 
	{
		if (-1 != tempStored[0])
		{
			strTemperature = "  ???  ";
			tempStored[0] = -1;
		}
		
		if (-1 != tempStored[1])
		{
			strClock = "  ???  ";
			tempStored[1] = -1;
		}

		strOnline = "???";
		
		invalidate();
	}


	public void setTemp (int 	temp[], 
						 String online) 
	{
		if (0 == tempDivider
			||
			temp[0] / tempDivider < 10
			||
			temp[0] / tempDivider >= 100)
		{
			for (tempDivider = 1; tempDivider < 100000; tempDivider *= 10)
			{
				if (temp[0] / tempDivider < 100)
				{
					break;
				}
			}
		}
		
		tempStored[0] = temp[0] / tempDivider;
		tempStored[1] = temp[1] / 1000;
		

		if (0 == tempStored[0])
		{
			strTemperature = "???";
		}
		else
		{
			strTemperature = String.format ("%d°C", 
											tempStored[0]);
		}
		strClock = String.format ("%d MHz", 
								  tempStored[1]);
		
		strOnline = online;

		invalidate();
	}


	public boolean onTouch (View 		v, 
							MotionEvent event) 
	{
		Log.d(TAG, String.format ("A: %d  X/Y:%d/%d",
								  event.getAction(),
								  (int)event.getX(),
								  (int)event.getY()));
		
		int location[] = new int[2];
		
		location[0] = (int)event.getX();
		location[1] = (int)event.getY();
		
		switch (event.getAction())
		{
		case MotionEvent.ACTION_DOWN:
			downTime = System.currentTimeMillis();
			
			coords.x = location[0];
			coords.y = location[1];
			motionHappen = false;
			break;
			
		case MotionEvent.ACTION_UP:
			long timeNow = System.currentTimeMillis();
			if (timeNow - downTime < CLICK_TIME)
			{
				if (timeNow - lastClickTime < CLICK_TIME)
				{
					lastClickTime 	= 0;
					motionHappen 	= false;
					
					new Handler().post (new InitiateActivity());
					break;
				}
				else
				{
					lastClickTime = timeNow;
				}
			}
			
			if (!motionHappen)
			{
				if (Math.abs(location[0] - coords.x) < 3 
					&&
					Math.abs(location[1] - coords.y) < 3)
				{
					Log.d(TAG, "TAP encountered. Do whatever...");
					break;
				}
			}
			
		case MotionEvent.ACTION_MOVE:
			motionHappen = true;
			
			WindowManager.LayoutParams params = (WindowManager.LayoutParams)getLayoutParams();
			
			params.x += location[0] - coords.x;
			params.y += location[1] - coords.y;
			
			if (params.x < -(widthDisplay + halfWidth) / 2)
			{
				params.x = -(widthDisplay + halfWidth) / 2;
			}
			else if (params.x > (widthDisplay + halfWidth) / 2)
			{
				params.x = (widthDisplay + halfWidth) / 2;
			}
			
			if (params.y < -(heightDisplay + halfHeight) / 2)
			{
				params.y = -(heightDisplay + halfHeight) / 2;
			}
			else if (params.y > (heightDisplay + halfHeight) / 2)
			{
				params.y = (heightDisplay + halfHeight) / 2;
			}
			
			windowManager.updateViewLayout (this, 
											params);
			
			if (MotionEvent.ACTION_UP == event.getAction())
			{
				service.saveDefaultXY ((float)params.x / widthDisplay, 
									   (float)params.y / heightDisplay);
			}
			
			break; 
		}
		
		return true;
	}


	public void getContentSize (Point contentSize) 
	{
	    overlayPaint[0].getTextBounds ("1300 MHz",
	    							   0, 
	    							   maxString.length(), 
	    							   bounds);

	    contentSize.x = bounds.width()      + X_PRIME * 2;
		contentSize.y = bounds.height() * 3 + Y_PRIME * 4;

		halfWidth  = contentSize.x / 2;
	    halfHeight = contentSize.y / 2;
	}
	
	
	private void initiateActivity()
	{
		Intent intent = new Intent(service,
								   CPUTableauActivity.class);
		
		intent.addFlags (Intent.FLAG_ACTIVITY_NEW_TASK);
		service.startActivity (intent);	
	}
}
