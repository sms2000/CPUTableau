package com.ogp.cputableau.providers;

import android.content.Context;
import android.util.Log;

public class CPUTemperatureProvider extends HWProvider 
{
	private static final String TAG = "CPUTemperatureProvider";

	
	private static final String[]	tempFiles		= new String[]{"/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
		   														   "/sys/devices/system/cpu/cpu0/cpufreq/FakeShmoo_cpu_temp",
		   														   "/sys/class/thermal/thermal_zone1/temp",							// HTC Evo 3D
		   														   "/sys/class/i2c-adapter/i2c-4/4-004c/temperature",
		   														   "/sys/devices/platform/omap/omap_temp_sensor.0/temperature",
		   														   "/sys/devices/platform/tegra_tmon/temp1_input",					// Atrix 4G
		   														   "/sys/kernel/debug/tegra_thermal/temp_tj",
		   														   "/sys/devices/platform/s5p-tmu/temperature",       				// Galaxy S3, Note 2
		   														   "/sys/class/thermal/thermal_zone0/temp",
		   														   "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq",
																   };

	private static Integer			tempIndex	= -1;
	
	
	public CPUTemperatureProvider(Context context)
	{
		synchronized(tempIndex)
		{
			try
			{
				if (-1 == tempIndex)
				{
	 				for (tempIndex = 0; 
	 					 0 >= readFileData (tempFiles[tempIndex]) 
	 					 && 
	 					 tempIndex < tempFiles.length; 
	 					 tempIndex++);
					
	 				if (tempIndex >= tempFiles.length)
	 				{
	 					tempIndex = -2;
	 					Log.e(TAG, "CPUTemperatureProvider. Temperature file not found.");
	 				}
				}
			}
			catch(Exception e)
			{
				Log.e(TAG, "CPUTemperatureProvider. EXC(1)");
			}
		}
	}
		
	
	public String getData() 
	{
		synchronized(tempIndex)
		{
			if (0 > tempIndex)
			{
				return null;
			}
		}
		
		try
		{
			int result = readFileData (tempFiles[tempIndex]);
				
			if (result <= 0)
			{
				Log.e(TAG, "Error recognizing CPU temp.");
			}
			else
			{
// Normalizing
				double dres = (double)result;
					
				while (dres >= 100)
				{
					dres *= 0.1;
				}
 					
				return temperatureDouble2StringString (dres);
			}
		}
		catch(Exception e)
		{
			Log.e(TAG, "Result: exception.");
		}

		return null;
	}
}
