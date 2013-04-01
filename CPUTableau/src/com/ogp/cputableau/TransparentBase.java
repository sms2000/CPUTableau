package com.ogp.cputableau;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public abstract class TransparentBase extends SurfaceView
{
	private static final int 	THREAD_OPERATIVE 	= 100;

	private Object				forceRedraw			= new Object();
	private SurfaceHolder 		surfaceHolder;
	private Thread				thread				= null;
	private boolean				running				= false;
	private long				waitTill			= 0;
	
	
	public TransparentBase(Context context)
	{
		super(context);
		
	    setBackgroundColor (0x01000000);
	}

	
	public void start()
	{
		if (null != thread)
		{
			return;
		}

		running = true;

		thread = new Thread()
		{
			@Override			
			public void run()
			{
				while (running)
				{
					synchronized(forceRedraw)
					{
						try
						{
							forceRedraw.wait (THREAD_OPERATIVE);
					    } 
						catch(InterruptedException e)
						{
					    }
					}

					
					if (null == surfaceHolder)
					{
						surfaceHolder = getHolder();
						if (null == surfaceHolder)
						{
							continue;
						}
						
						surfaceHolder.setFormat (PixelFormat.TRANSPARENT);
					}
					

					Surface sf = surfaceHolder.getSurface();

					if (sf.isValid())
					{
						Canvas canvas = surfaceHolder.lockCanvas();
						if (null != canvas)
						{
							drawPanelImage (canvas);

							surfaceHolder.unlockCanvasAndPost (canvas);
						}
						else if (!running)
						{
							break;
						}
				    }
					else if (!running)
					{
						break;
					}
				}
			}
			
		};
		
		thread.setPriority (Thread.NORM_PRIORITY + 2);
		thread.setName ("SurfaceView");
		thread.start();
	}
    
    
	public void stop()
	{
		if (null == thread)
		{
			return;
		}
	
		
		boolean retry = true;
		running = false;

		while (retry)
		{
			try
			{
				thread.join();
				retry = false;
			}
			catch (InterruptedException e)
			{
			}
		}

		thread = null;
	}


	public void refresh()
    {
    	synchronized(forceRedraw)
    	{
    		forceRedraw.notify();
    	}
    }
   
	
	public void waitUpdate (long ms)
	{
		waitTill = System.currentTimeMillis() + ms; 
	}

	
	public boolean waitNoUpdate()
	{
		return System.currentTimeMillis() < waitTill; 
	}
	
	
	abstract protected void drawPanelImage (Canvas canvas); 
}
