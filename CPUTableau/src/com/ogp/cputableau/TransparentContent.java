package com.ogp.cputableau;

import com.ogp.cputableau.providers.BatteryTemperatureProvider;
import com.ogp.cputableau.providers.CPUClockProvider;
import com.ogp.cputableau.providers.CPUCoresProvider;
import com.ogp.cputableau.providers.CPUTemperatureProvider;
import com.ogp.cputableau.providers.ChargingProvider;
import com.ogp.cputableau.providers.HWProvider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;


@SuppressLint("DefaultLocale")
public class TransparentContent extends TransparentBase 
{
	private static final String 	TAG								= "TransparentContent";

	private static final String 	TXT_LONG_STRING					= "199.9°F/199.9°F_";
	private static final String 	TXT_NO_CLOCK 					= "No clock";

	private static final int 		X_PRIME 						= 10;
	private static final int 		Y_PRIME 						= 6;
	private static final int 		TEXT_SIZE 						= 24;

	private static final int 		BACKGROUND_COLOR 				= 0x00707040;
	private static final int 		GREY_COLOR 						= 0xA0A0A0A0;
	private static final int 		PAINT_0							= 0xD040F0F0;
	private static final int 		PAINT_1							= 0xD0F03030;
	private static final int 		PAINT_2							= 0xD0F0F020;

	private static final int 		MAX_PROVIDERS 					= 5;
	
	private static final int 		MAX_PERMANENT_PARAMS 			= 3;
	private static final int 		MAX_OPTIONAL_PARAMS				= 1;

	private static final int 		DATA_TEMPERATURE				= 0;
	private static final int 		DATA_CLOCK						= 1;
	private static final int 		DATA_CORES						= 2;
	private static final int 		DATA_CHARGE						= 3;

	private static final int 		PRV_CPU_TEMPERATURE				= 0;
	private static final int 		PRV_BAT_TEMPERATURE				= 1;
	private static final int 		PRV_CLOCK						= 2;
	private static final int 		PRV_CORES						= 3;
	private static final int 		PRV_CHARGE						= 4;


	private TransparentContentInterface
									transparentFrame;
	private Handler					handler							= new Handler();
	
	private Paint 					overlayPaint[]					= new Paint[3];
	private Paint 					dashPaint 						= new Paint();
	private Paint 					backgroundPaint					= new Paint();
	
	private String					dataStrings[]					= new String[MAX_PERMANENT_PARAMS + MAX_OPTIONAL_PARAMS];
	private String					newDataStrings[] 				= new String[MAX_PERMANENT_PARAMS + MAX_OPTIONAL_PARAMS];
	private boolean					dataError						= false;
	
	private Rect 					bounds		 					= new Rect();

	private Point					contentSize						= new Point(160, 
																				60);

	private HWProvider				providers[]						= new HWProvider[MAX_PROVIDERS];

	
	
	public enum EUpdated {SAME, UPDATED, SIZE_CHANGED};
	

	public TransparentContent(Context context)
	{
		super(context);
	}
	

	private class UpdateFrame implements Runnable
	{
		public void run() 
		{
			try
			{
				transparentFrame.contentSizeChanged();
				Log.w(TAG, "UpdateFrame::run. contentSizeChanged.");
			}
			catch(Exception e)
			{
				Log.e(TAG, "UpdateFrame::run. EXC(1)");
			}
		}
	}
	
	
	public TransparentContent(Context 						context, 
							  TransparentContentInterface	transparentFrame)
	{
		super(context);

		this.transparentFrame = transparentFrame;
		
		Log.v(TAG, "TransparentContent. Entry...");
		
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
		
		
		contentSize = getContentSize();
		
		setError();
		setEnabled (true);
		
		providers[0] = new CPUTemperatureProvider 		(context);
		providers[1] = new BatteryTemperatureProvider	(context);
		providers[2] = new CPUClockProvider 			(context);
		providers[3] = new CPUCoresProvider 			(context);
		providers[4] = new ChargingProvider 			(context);
		
		start();		

		Log.v(TAG, "TransparentContent. ... Exit.");
	}

	
	public void finalize() 
	{
		Log.v(TAG, "finalize. Entry...");
		
		for (int i = 0; i < MAX_PROVIDERS; i++)
		{
			providers[i].finalize();
		}
		
		super.finalize();

		Log.v(TAG, "finalize. ... Exit.");
	}

	
	public boolean drawOverlay (Canvas 		canvas, 
							    boolean 	unconditional)
	{
// Request data update.
		EUpdated updated = requestDataUpdate();
		if (EUpdated.SIZE_CHANGED == updated)
		{
			handler.post (new UpdateFrame());
			return true;
		}
		
		if (EUpdated.SAME == updated 
			&&
			!unconditional)
		{
			return true;
		}
		
		
// Update		
		canvas.drawColor (0,
  		  		  		  PorterDuff.Mode.CLEAR);

		
		canvas.drawColor (BACKGROUND_COLOR | (StateMachine.getTransparency() << 24));
			
		overlayPaint[0].getTextBounds (TXT_LONG_STRING, 
									   0, 
									   TXT_LONG_STRING.length(), 
									   bounds);

		int paintIndex = 0;
		    
		if (!dataError)
		{
			paintIndex = 1;
		}
		    
		canvas.drawText (dataStrings[DATA_TEMPERATURE],
						 X_PRIME,
						 bounds.height() + Y_PRIME,
						 overlayPaint[paintIndex]);
	
		canvas.drawText (dataStrings[DATA_CLOCK],
						 X_PRIME,
						 (bounds.height() + Y_PRIME) * 2,
						 overlayPaint[paintIndex]);
	
		canvas.drawText (dataStrings[DATA_CORES],
						 X_PRIME,
						 (bounds.height() + Y_PRIME) * 3,
		 		 		 overlayPaint[paintIndex]);
		
		if (null != dataStrings[DATA_CHARGE])
		{
			canvas.drawText (dataStrings[DATA_CHARGE],
					 		 X_PRIME,
					 		 (bounds.height() + Y_PRIME) * 4,
					 		 overlayPaint[2]);
		}
		
		return true;
	}


	public void updateFontSize() 
	{
		Log.v(TAG, "updateFontSize. Entry...");
		
		
		overlayPaint[0].setTextSize (StateMachine.getFontSize());
		overlayPaint[1].setTextSize (StateMachine.getFontSize());
		overlayPaint[2].setTextSize (StateMachine.getFontSize());
		
		contentSize = getContentSize();
		
		refresh();

		Log.w(TAG, "updateFontSize. Font size updated.");
		Log.v(TAG, "updateFontSize. ... Exit.");
	}
	
	
	public Point getContentSize() 
	{
	    overlayPaint[0].getTextBounds (TXT_LONG_STRING,
	    							   0, 
	    							   TXT_LONG_STRING.length(), 
	    							   bounds);

	    contentSize.x = bounds.width() + X_PRIME * 2;
	    
    	contentSize.y = (Y_PRIME + bounds.height()) * MAX_PERMANENT_PARAMS + Y_PRIME * 3 / 2; 
    	
    	if (null != dataStrings[DATA_CHARGE])
	    {
	    	contentSize.y += bounds.height() + Y_PRIME;
	    	
	    	Log.w(TAG, "getContentSize. 4 strings.");
	    }
    	else
    	{
	    	Log.w(TAG, "getContentSize. 3 strings.");
    	}
    	
    	
    	contentSize.x = (contentSize.x + 1) & ~1;		// Even numbers only 
    	contentSize.y = (contentSize.y + 1) & ~1;
    	
    	return contentSize; 
	}
	

	
	@Override
	protected void drawPanelImage (Canvas 	canvas,
								   boolean 	unconditional) 
	{
		drawOverlay (canvas, 
					 unconditional);
	}

	
	private boolean setError() 
	{
		Log.v(TAG, "setError. Entry...");

		boolean justSet = false;
		
		if (!dataError)
		{
			dataStrings[DATA_TEMPERATURE] 	= " No temp";
			dataStrings[DATA_CLOCK] 		= " No clock";
			dataStrings[DATA_CORES] 		= " No cores";
			dataStrings[DATA_CHARGE] 		= null;
			dataError = true;
			
			refresh();
			
			justSet = true;
		}
		
		Log.v(TAG, "setError. ... Exit.");
		
		return justSet;
	}

	
	private EUpdated requestDataUpdate() 
	{
// Temperature of the CPU		
		String answer = providers[PRV_CPU_TEMPERATURE].getData();
		if (null == answer)
		{
			newDataStrings[DATA_TEMPERATURE] = "??";
		}
		else
		{
			newDataStrings[DATA_TEMPERATURE] = answer;
		}
		

// Temperature of the battery		
		answer = providers[PRV_BAT_TEMPERATURE].getData();
		if (null != answer)
		{
			newDataStrings[DATA_TEMPERATURE] += "/";
			newDataStrings[DATA_TEMPERATURE] += answer;
		}

		
// CPU clock		
		answer = providers[PRV_CLOCK].getData();
		if (null != answer)
		{
			newDataStrings[DATA_CLOCK] = answer;
		}
		else
		{
			newDataStrings[DATA_CLOCK] = TXT_NO_CLOCK;
		}
		
		
// CPU cores
		newDataStrings[DATA_CORES] = providers[PRV_CORES].getData();  
		
		
// Charge		
		newDataStrings[DATA_CHARGE] = providers[PRV_CHARGE].getData();
		boolean sizeChanged = (null == newDataStrings[DATA_CHARGE]) != (null == dataStrings[DATA_CHARGE]);
		dataStrings[DATA_CHARGE] = newDataStrings[DATA_CHARGE];
		
// Compare new vs old
		boolean updated = false;
		
		for (int i = 0; i < MAX_PERMANENT_PARAMS + MAX_OPTIONAL_PARAMS; i++)
		{
			if (compStrings (dataStrings[i], 
							 newDataStrings[i]))
			{
				dataStrings[i] = newDataStrings[i]; 
				updated = true;
			}
		}

		
		if (sizeChanged)
		{
			Log.w(TAG, "requestDataUpdate. Size changed.");
			return EUpdated.SIZE_CHANGED; 
		}
		else
		{
			Log.w(TAG, "requestDataUpdate. " + (updated ? "Updated." : "Not updated"));
			return updated ? EUpdated.UPDATED : EUpdated.SAME;
		}
	}
	
	
	private boolean compStrings (String 	v1, 
								 String 	v2)
	{
		if ((null == v1) != (null == v2))
		{
			return true;
		}
		else if (null == v1)
		{
			return false;
		}
		else 
		{
			return !v1.equals (v2);
		}
	}
}
