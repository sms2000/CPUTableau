package com.ogp.cputableau;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import android.util.Log;


public class AccelThread extends Thread
{
	private static final String TAG 			= "AccelThread";

	private static final String ACCEL_FILE_DATA	= "/sys/class/sensors/proximity_sensor/state";
	
	
	private boolean				threadRun 		= true;
	private long				pollingTime		= 250;
	

	public AccelThread()		
	{
		initAccelThread();
	}
	
	
	public void run() 
	{
		loop();
	}
	
	
	public void finalize()
	{
		threadRun = false;

		interrupt();
	}
	
	
	private void initAccelThread()
	{
		start();
	}
	
	
	protected void loop()
	{
		while (threadRun)
		{
			try
			{
 				String str = readOnlineFileData (ACCEL_FILE_DATA);
				
				if (StateMachine.getExtensiveDebug())
				Log.d(TAG, "Result: " + str);
			}
			catch(Exception e)
			{
				Log.d(TAG, "Result: exception.");
			}
			

			try 
			{
				sleep (pollingTime);
			} 
			catch (InterruptedException e) 
			{
			}
		}

		Log.w(TAG, "Thread finished.");
	}
	
	
	private String readOnlineFileData (String onlineFiles) 
	{
		String output = "???";
		File   file   = new File(onlineFiles);

		try 
		{
			BufferedReader br = new BufferedReader(new FileReader(file));
			output = br.readLine();
		} 
		catch (Exception e) 
		{
		}
		
		return output;
	}
}
