package com.ogp.cputableau.providers;

import com.ogp.cputableau.StateMachine;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

public class ChargingProvider extends HWProvider 
{
	private static final String TAG 			= "ChargingProvider";

	private static final String	chargeFiles		= "/sys/class/power_supply/battery/current_now";
	private static final String	statusFiles		= "/sys/class/power_supply/battery/status";

	private static int		savedCurrent		= -1;
	
	public ChargingProvider(Context context)
	{
		try
		{
			if (0 < readFileInt (chargeFiles))
			{
				Log.w(TAG, "ChargingProvider. CPU clock file found.");
			}
			else
			{			
				Log.e(TAG, "ChargingProvider. CPU clock file not found.");
			}
		}
		
		catch(Exception e)
		{
			Log.e(TAG, "ChargingProvider. EXC(1)");
		}
		
	}

	
	@SuppressLint("DefaultLocale")
	@Override
	public String getData() 
	{
		if (!StateMachine.getChargeCurrent())
		{
			return null;
		}

		
		try
		{
			String data = readFileString (statusFiles);
			if (null == data 
				|| 
				'D' == data.getBytes()[0])
			{
				savedCurrent = -1;
 				Log.v(TAG, "getData. No charging now...");
 				
 				return "Discharge";
			}
			else if ('F' == data.getBytes()[0])
			{
				savedCurrent = -1;
 				Log.v(TAG, "getData. Full battery...");
 				
 				return "Full";
			}
			else
			{
				int result = readFileInt (chargeFiles);
					
				if (0 < result)
	 			{
					savedCurrent = result;
				}
				
				if (0 < savedCurrent)
				{
	 				Log.v(TAG, String.format ("getData. Charging current recognized: %d mA",
	 										  result));
		 			
		 				
	 				return String.format ("%d mA", 
	 									  savedCurrent);
				}

				Log.v(TAG, "getData. Charging current could not be retrived yet.");
			}
		}
		catch(Exception e)
		{
			Log.e(TAG, "getData. EXC(1)");
		}
		
		return null;
	}
}
