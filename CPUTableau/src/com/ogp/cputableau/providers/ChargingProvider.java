package com.ogp.cputableau.providers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

public class ChargingProvider extends HWProvider 
{
	private static final String TAG 			= "ChargingProvider";

	private static final String	chargeFiles		= "/sys/class/power_supply/battery/current_now";

	
	public ChargingProvider(Context context)
	{
		try
		{
			if (0 < readFileData (chargeFiles))
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
		try
		{
			int result = readFileData (chargeFiles);
				
			if (0 >= result)
 			{
 				Log.e(TAG, "getData. Error recognizing charging current.");
			}
			else
			{
 				Log.v(TAG, String.format ("getData. Charging current recognized: %d mA",
 										  result));
 				
 				return String.format ("%d mA", 
 									  result);
			}
		}
		catch(Exception e)
		{
			Log.e(TAG, "getData. EXC(1)");
		}
		
		return null;
	}
}
