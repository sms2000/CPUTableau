package com.ogp.cputableau.providers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

public class CPUCoresProvider extends HWProvider 
{
	private static final String 	TAG 					= "CPUCoresProvider";

	private static final String		onlineFilesFormat		= "/sys/devices/system/cpu/cpu%d/online";

	private static final int 		MAX_CORES 				= 16;

	
	private static String[]			onlineFiles 			= new String[MAX_CORES];
	private static Integer			maxCore				    = -1;
	private static Boolean			coresError				= false;
	
	
	@SuppressLint("DefaultLocale")
	public CPUCoresProvider(Context	context)
	{
		synchronized(maxCore)
		{
			if (0 > maxCore)
			{
				for (int i = 0; i < MAX_CORES; i++)
				{
					String cpu = String.format (onlineFilesFormat, 
												i);
					
					File file = new File(cpu);

					try 
					{
						BufferedReader br = new BufferedReader(new FileReader(file));
					    br.readLine();
					    br.close();
					    onlineFiles[i] = String.format (onlineFilesFormat, 
					    							    ++maxCore);
					} 
					catch (Exception e) 
					{
						if (maxCore < 0)
						{
							Log.e(TAG, "CPUCoresProvider. No cores activity info found.");
							maxCore = 0;
						}
						
						break;
					}
				}
			}
		}

		Log.w(TAG, String.format ("CPUCoresProvider. Found %d cores.", 
								  maxCore + 1));
	}
	

	public String getData() 
	{
		String  coreData 		= "";
		boolean discoveredCores = false;
		
		
		for (int i = 0; i <= maxCore; i++)
		{
			File file = new File(onlineFiles[i]);

			try 
			{
				BufferedReader br = new BufferedReader(new FileReader(file));
			    String str = br.readLine();
			    br.close();
			    if (0 < i)
			    {
			    	coreData += "-";
			    }
			    
		    	coreData += str;
		    	discoveredCores = true;
			} 
			catch (Exception e) 
			{
				if (!coresError)
				{
					coresError = true;
					Log.e(TAG, "getData. No cores activity info found.");
				}
				
				return null;
			}
		}

		Log.v(TAG, String.format ("getData. Cores activity found: [%s].", 
								  coreData));
		return discoveredCores ? coreData : "1";
	}
}
