package com.ogp.cputableau.providers;

import com.ogp.cputableau.ShellInterface;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

public class CPUClockProvider extends HWProvider
{
	private static final String 	TAG 				= "CPUClockProvider";

	private static final String		freqFiles			= "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";
	private static final int		KHZ2MHZ				= 1000;


	public CPUClockProvider(Context context)
	{
		try
		{
			if (0 > readFileInt (freqFiles))
			{
				new ShellInterface();
				ShellInterface.isSuAvailable();			
 				ShellInterface.runCommand ("chmod 404 " + freqFiles);
			}

			if (0 < readFileInt (freqFiles))
			{
				Log.w(TAG, "CPUClockProvider. CPU clock file found.");
			}
			else
			{			
				Log.e(TAG, "CPUClockProvider. CPU clock file not found.");
			}
		}
		
		catch(Exception e)
		{
			Log.e(TAG, "CPUClockProvider. EXC(1)");
		}
	}
	
	
	@SuppressLint("DefaultLocale")
	public String getData() 
	{
		try
		{
			int result = readFileInt (freqFiles) / KHZ2MHZ;
				
			if (result <= 0)
 			{
 				Log.e(TAG, "getData. Error recognizing CPU clock.");
			}
			else
			{
 				Log.v(TAG, String.format ("getData. CPU clock recognized: %d MHz",
 										  result));
 				
 				return String.format ("%d MHz", 
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
