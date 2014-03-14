package com.ogp.cputableau;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;


public class TransparentFrame extends RelativeLayout implements View.OnTouchListener, TransparentContentCallback
{
	private static final String 	TAG					= "TransparentFrame";

	private static final long 		CLICK_TIME 			= 300;
	private static final long 		WAIT_UPDATE 		= 200;
	private static final int 		MOTION_RADIUS 		= 3;


	private TransparentContent		transparentClient;
	private	Point					coords				= new Point();
	private	boolean					motionHappen		= false;

	private Object					lock				= new Object();
	private CPUTableauService		service;
	private WindowManager  			windowManager		= null;
	private WindowManager.LayoutParams	layoutParams	= null;
	private int						widthDisplay;
	private int						heightDisplay;
	private Point					contentSize			= new Point();
	private Point					contentHalfSize		= new Point();
	private Point  					downPoint			= new Point();
	private long 					downTime			= 0;
	private long 					lastClickTime		= 0;
	private Handler					handler				= new Handler();


	private class VerifySingleClick implements Runnable
	{
		public void run() 
		{
			verifySingleClick();			
		}
	}
	
	
	private class ActivateMove implements Runnable
	{
		private WindowManager.LayoutParams 	params;
		private int							paddingX;
		private int							paddingY;
		
		
		private ActivateMove(WindowManager.LayoutParams params,
							 int						paddingX,
							 int 						paddingY)
		{
			this.params 	= params;
			this.paddingX	= paddingX;
			this.paddingY	= paddingY;
		}
		

		public void run() 
		{
			setPadding (paddingX, 
					    paddingY, 
						0,
						0);
			
			windowManager.updateViewLayout (TransparentFrame.this, 
		   									params);

			transparentClient.refresh (false);
		}
	}
	
	private class InitiateActivity implements Runnable
	{
		public void run() 
		{
			initiateActivity();
		}
	}
	

	public TransparentFrame(CPUTableauService 		 	service,
							TransparentContent			transparentClient)
	{
		super(service);

		this.service				= service;
		this.transparentClient		= transparentClient;
		this.windowManager			= (WindowManager)service.getSystemService (Context.WINDOW_SERVICE);

		
		transparentClient.setContentCallback (this);

		widthDisplay  = windowManager.getDefaultDisplay().getWidth();
		heightDisplay = windowManager.getDefaultDisplay().getHeight();
		
		setOnTouchListener (this);
		
		loadContentSize();
		setContentWindow();
	}
	
	
	private void loadContentSize()
	{
		transparentClient.getContentSize (contentSize);		
		
		contentHalfSize.x = contentSize.x / 2;
		contentHalfSize.y = contentSize.y / 2;
	}
	
	
	private void setContentWindow()
	{
		float X = service.loadDefaultX();
		float Y = service.loadDefaultY();
		
		layoutParams = new WindowManager.LayoutParams(contentSize.x,
							   						  contentSize.y,
							   						  (int)(X * widthDisplay),
							   						  (int)(Y * heightDisplay),
							   						  WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
				                					  WindowManager.LayoutParams.FLAG_FULLSCREEN 		|
			                					  	  WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS	|
			                					  	  WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN 	|
				                					  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
				                					  PixelFormat.TRANSLUCENT);

		windowManager.addView (this,
							   layoutParams);
		
		addView (transparentClient,
				 new FrameLayout.LayoutParams (FrameLayout.LayoutParams.FILL_PARENT,
						 					   FrameLayout.LayoutParams.FILL_PARENT));
		
		reposition();
	}
	
	
	private void adjustContentWindow()
	{
		windowManager.removeView (this);
		
		layoutParams.width  = contentSize.x;
		layoutParams.height = contentSize.y;
		
		windowManager.addView (this,
				   			   layoutParams);
	}
	
	
	public void dismiss()
	{
		removeView (transparentClient);
		transparentClient.stop();
		transparentClient = null;
		
		try
		{
			windowManager.removeView (this);
		}
		catch(Exception e)
		{
			Log.e(TAG, "dismiss(). EXC(1)");
		}
	}


	@Override
	public void onLayout (boolean	changed, 
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
		
		if (windowManager.getDefaultDisplay().getWidth() != widthDisplay)
		{
			widthDisplay  = windowManager.getDefaultDisplay().getWidth();
			heightDisplay = windowManager.getDefaultDisplay().getHeight();

			reposition();
		}

	}
	
	
	public boolean onTouch (View 		v, 
							MotionEvent event) 
	{
		WindowManager.LayoutParams params;
		
		if (StateMachine.getExtensiveDebug())
		{
			Log.v(TAG, String.format ("A: %d  X/Y:%d/%d",
									  event.getAction(),
									  (int)event.getX(),
									  (int)event.getY()));
		}
		
		int location[] = new int[2];
		
		location[0] = (int)event.getX();
		location[1] = (int)event.getY();

		int X = location[0] - coords.x;
		int Y = location[1] - coords.y; 
		
		
		switch (event.getAction())
		{
		case MotionEvent.ACTION_DOWN:
			downTime 		= System.currentTimeMillis();
			params 			= (WindowManager.LayoutParams)getLayoutParams();
			coords.x	 	= location[0];
			coords.y 		= location[1];
			motionHappen 	= false;

			synchronized(lock)
			{
				downPoint.x = params.x + (widthDisplay  - contentSize.x) / 2;
				downPoint.y = params.y + (heightDisplay - contentSize.y) / 2;
				
				params.x = 0; 
				params.y = 0; 
	
				params.width  = widthDisplay; 
				params.height = heightDisplay; 
				
				transparentClient.waitUpdate (WAIT_UPDATE);
				transparentClient.refresh (false);
			}
			
			setPadding (downPoint.x, 
						downPoint.y, 
						0,
						0);
		
			windowManager.updateViewLayout (TransparentFrame.this, 
	   										params);

			break;

			
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			if (!StateMachine.isActivityRun())
			{
				long timeNow = System.currentTimeMillis();
				if (timeNow - downTime < CLICK_TIME)
				{
					if (timeNow - lastClickTime < CLICK_TIME)
					{
						Log.d(TAG, "Double click encountered. Do whatever...");

						lastClickTime 	= 0;
						motionHappen 	= false;
						
						new Handler().post (new InitiateActivity());
					}
					else
					{
						lastClickTime = timeNow;

						new Handler().postDelayed (new VerifySingleClick(),
												   CLICK_TIME);
					}
				}
			}
			
			
		case MotionEvent.ACTION_MOVE:
			motionHappen = Math.abs(X - downPoint.x) > MOTION_RADIUS
						   ||
						   Math.abs(Y - downPoint.y) > MOTION_RADIUS;
			
			
			
			if (X < -contentHalfSize.x)
			{
				X = -contentHalfSize.x;
			}
			else if (X > widthDisplay - contentHalfSize.x)
			{
				X = widthDisplay - contentHalfSize.x;
			}
			
			if (Y < -contentHalfSize.y)
			{
				Y = -contentHalfSize.y;
			}
			else if (Y > heightDisplay - contentHalfSize.y)
			{
				Y = heightDisplay - contentHalfSize.y;
			}

			
			synchronized(lock)
			{
				downPoint.x = X;
				downPoint.y = Y;

				setPadding (downPoint.x, 
							downPoint.y, 
							0, 
							0);
			}


			if (MotionEvent.ACTION_MOVE != event.getAction())
			{
				params = (WindowManager.LayoutParams)getLayoutParams();

				synchronized(lock)
				{
					params.x = X -  widthDisplay / 2 + contentHalfSize.x;
					params.y = Y - heightDisplay / 2 + contentHalfSize.y;
					
					params.width  = contentSize.x; 
					params.height = contentSize.y; 

					setPadding (0, 
								0, 
								0, 
								0);
					
			 	}

				windowManager.updateViewLayout (this, 
			   									params);


				service.saveDefaultXY ((float)params.x / widthDisplay, 
						   			   (float)params.y / heightDisplay);
			}

			
			transparentClient.refresh (false);
			break;
		}
		
		return true;
	}


	private void verifySingleClick()
	{
		if (0 < lastClickTime
			&&
			!motionHappen)
		{
			lastClickTime = 0;
			
			Log.d(TAG, "Single click encountered. Do whatever...");			
//
//  TODO: use single click if required...
//			

		}
	}
	
	
	private void initiateActivity()
	{
		Intent intent = new Intent(service,
								   CPUTableauActivity.class);
		
		intent.addFlags (Intent.FLAG_ACTIVITY_NEW_TASK);
		service.startActivity (intent);	
	}


	public void errorTemp() 
	{
		transparentClient.setErrorParameters();
	}


	public void setParams (int[] 	params, 
						   String 	online) 
	{
		transparentClient.updateParameters (params, 
								   			online);
	}

	
	private void reposition() 
	{
		WindowManager.LayoutParams params = (WindowManager.LayoutParams)getLayoutParams(); 

		params.x = (int)(service.loadDefaultX() * widthDisplay);
		params.y = (int)(service.loadDefaultY() * heightDisplay);
		
		handler.post (new ActivateMove(params, 
									   0, 
									   0));
	}


	public void contentSizeChanged() 
	{
		Log.d(TAG, "TransparentFrame size changed.");

		loadContentSize();
		adjustContentWindow();
		
		transparentClient.refresh();
	}
}
