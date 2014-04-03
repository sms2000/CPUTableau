package com.ogp.cputableau;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public abstract class TransparentBase extends SurfaceView
{
	private static final String TAG 				= "TransparentBase";

	protected static final long MIN_WAIT 			= 100;	// 100ms
	
	private SurfaceHolder 		surfaceHolder;
	private Thread				thread				= null;
	private boolean				running				= false;
	private int					forceInflicted		= 0;
	private CountDownLatch 		lock				= null;
	
	
	public TransparentBase(Context context)
	{
		super(context);
		
	    setBackgroundColor (0x01000000);
	}

	
	public void finalize() 
	{
		stop();
	}

	
	protected void start()
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
							drawPanelImage (canvas, 
											forceInflicted > 0);

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

					
					try
					{
						if (0 < forceInflicted)
						{
							forceInflicted--;
							lock = new CountDownLatch(1);
							lock.await (MIN_WAIT, 
										TimeUnit.MILLISECONDS);
							
						}
						else
						{
							lock.await (StateMachine.getRefreshMs(), 
										TimeUnit.MILLISECONDS);
						}
				    } 
					catch(Exception e)
					{
						Log.e(TAG, "run. EXC(1)");
					} 
				}
			}
		};
		
		
		thread.setPriority (Thread.NORM_PRIORITY + 2);
		thread.setName ("SurfaceView");
		thread.start();
	}
    
    
	protected void stop()
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
		try
		{
			forceInflicted += 3;		// Next 3 iteration done in fast
			
			lock.countDown();
			
			Log.w(TAG, "refresh. Applyed.");
		}
		catch(Exception e)
		{
			Log.e(TAG, "refresh. EXC(1)");
		}
    }
   
	
	abstract protected void drawPanelImage (Canvas 		canvas,
			   								boolean 	unconditional); 
}
