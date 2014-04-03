package com.ogp.cputableau.providers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

public class BatteryTemperatureProvider extends HWProvider 
{
	private static final String TAG 			= "BatteryTemperatureProvider";

	private Context				context;
	private static Double		currentTemperature	= -1.0d;
	
	
	private BroadcastReceiver 	batteryInfoReceiver = new BroadcastReceiver()
	{
	    @Override
	    public void onReceive (Context 	arg0,
	    					   Intent 	intent)
	    {
	    	double temperature = 0.1f * (float)intent.getIntExtra (BatteryManager.EXTRA_TEMPERATURE,
	    		  								                   0);

			Log.d(TAG, String.format ("onReceive. Battery temperature: %.1f", 
									  (float)temperature));
			
			synchronized(currentTemperature)
			{
				currentTemperature = temperature;
			}
	    }
	};

	
	public BatteryTemperatureProvider(Context context)
	{
		this.context = context;

		context.registerReceiver (batteryInfoReceiver,
								  new IntentFilter (Intent.ACTION_BATTERY_CHANGED));

		Log.w(TAG, "BatteryTemperatureProvider. Battery temperature receiver registered.");
	}
	
	
	@Override
	public void finalize()
	{
		context.unregisterReceiver (batteryInfoReceiver);

		Log.w(TAG, "finalize. Battery temperature receiver unregistered.");
	}
	
	
	public String getData() 
	{
		double temp;
		
		synchronized(currentTemperature)
		{
			if (currentTemperature < 0.0f)
			{
				return null;
			}
			
			
			temp = currentTemperature;
		}
		
		return temperatureDouble2StringString (temp);
	}
}
