package com.ogp.cputableau;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import android.os.Handler;
import android.util.Log;


public class WatchdogThread extends Thread
{
	private static final String TAG 		= "WatchdogThread";

	private static final int 	MAX_STRINGS = 4;
	
	
	private boolean				threadRun 	= true;
	private String[]			tempFiles;
	private String[]			freqFiles;
	private String				onlineFiles;	
	private String[] 			chargeFiles;
	private WatchdogCallback 	watchdogCallback;
	private Handler				mainHandler;				
	private long				pollingTime	= 1000;
	private int					tempIndex	= -1;
	private int					freqIndex	= -1;
	private int					onlineCPUs	= -1;
	private int					chargeIndex	= -1;
	
	enum DataStatus {None, OK, Error};
	
	private DataStatus dataStatus = DataStatus.None;

	private String 				oldStrOnline	= "";
	private int					oldRes[]		= new int[MAX_STRINGS];

	
    private class ErrorTempTask implements Runnable
    {

		public void run() 
		{
			watchdogCallback.errorTemp();
		}
    	
    }
    
    
    private class NewTempTask implements Runnable
    {
    	private int 	newParams[];
    	private String	online;
    	
    	
    	NewTempTask(int 	params[], 
    				String 	online)
    	{
    		this.newParams 	= params;
    		this.online		= online;
    	}
    	
    	
		public void run() 
		{
			watchdogCallback.setParams (newParams, 
									    online);
		}
    	
    }

    
    public WatchdogThread(String			tempFiles[],
    					  String			freqFiles[],
    					  String			onlineFiles,
						  String[] 			chargeFiles, 
						  WatchdogCallback	callback)		
	{
    	
		initWatchdogThread (tempFiles,
							freqFiles,
							onlineFiles,
							chargeFiles,
							callback);
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
	
	
	private void initWatchdogThread (String[]			tempFiles,
			  						 String[]			freqFiles,
			  						 String				onlineFiles,
								     String[] 			chargeFiles, 
								     WatchdogCallback	callback)
	{
		this.tempFiles 		  = tempFiles;
		this.freqFiles 		  = freqFiles;
		this.onlineFiles	  = onlineFiles;
		this.chargeFiles	  = chargeFiles;
		this.watchdogCallback = callback;
		this.mainHandler	  = new Handler();
		
		start();
	}
	
	
	protected void loop()
	{
		int res[] = new int[MAX_STRINGS];
		
		while (threadRun)
		{
			boolean resOK = false;
			
			try
			{
				
				if (-1 == tempIndex)
				{
	 				for (tempIndex = 0; 0 >= readFileData (tempFiles[tempIndex]) && tempIndex < tempFiles.length; tempIndex++);
					
	 				if (tempIndex >= tempFiles.length)
	 				{
	 					tempIndex = -2;
	 					Log.e(TAG, "Temperature file not found.");
	 				}
				}
				
				if (tempIndex != -2)
				{
	 				res[0] = readFileData (tempFiles[tempIndex]);
					
	 				if (res[0] <= 0)
	 				{
	 					Log.e(TAG, "Error recognizing CPU temp.");
	 				}
	 				else
	 				{
	 					resOK = true;
	 				}
				}
			}
			catch(Exception e)
			{
				Log.e(TAG, "Result: exception.");
			}
			
			try
			{
				if (-1 == freqIndex)
				{
 					if (0 > readFileData (freqFiles[0]))
 					{
	 					new ShellInterface();
	 					ShellInterface.isSuAvailable();			
	 					
	 					for (freqIndex = 0; 
	 						 freqIndex < freqFiles.length; 
	 						 freqIndex++)
	 					{
	 						ShellInterface.runCommand ("chmod 404 " + freqFiles[freqIndex]);
	 					}
 					}
 					

 					for (freqIndex = 0; 
 						 freqIndex < freqFiles.length; 
 						 freqIndex++)
	 				{
	 					if (0 < readFileData (freqFiles[freqIndex]))
	 					{
	 						break;
	 					}
	 				}
					
	 				if (freqIndex >= freqFiles.length)
	 				{
	 					freqIndex = -2;
	 					Log.e(TAG, "CPU clock file not found.");
	 				}
				}
				
				if (freqIndex != -2)
				{
					res[1] = readFileData (freqFiles[freqIndex]);
					
					
	 				if (res[1] <= 0)
	 				{
	 					Log.e(TAG, "Error recognizing CPU clock.");
	 					mainHandler.post (new ErrorTempTask());
	 				}
	 				else
	 				{
	 					resOK = true;
	 				}
				}
			}
			catch(Exception e)
			{
				Log.e(TAG, "Result: exception.");
			}

			
			try
			{
				if (-1 == chargeIndex)
				{
 					for (chargeIndex = 0; 
 						 chargeIndex < chargeFiles.length; 
 						 chargeIndex++)
	 				{
	 					if (0 < readFileData (chargeFiles[chargeIndex]))
	 					{
	 						break;
	 					}
	 				}
					
	 				if (chargeIndex >= chargeFiles.length)
	 				{
	 					chargeIndex = -2;
	 					Log.e(TAG, "Charge current file not found.");
	 				}
				}
				
				if (chargeIndex != -2)
				{
					res[2] = readFileData (chargeFiles[freqIndex]);
					
 					resOK = true;
				}
			}
			catch(Exception e)
			{
				Log.e(TAG, "Result: exception.");
			}

			
			String strOnline = null;
			
			try
			{
				strOnline = readOnlineFilesData (onlineFiles);
				
				resOK = true;
			}
			catch(Exception e)
			{
				Log.e(TAG, "Result: exception.");
			}

			
			if (StateMachine.getExtensiveDebug())
			{
				Log.d(TAG, "Result: " + res);
			}
			
			
			if (resOK)
			{
				boolean updated = updateOldData  (res, 
												  strOnline);

				
				if (DataStatus.OK != dataStatus 
					|| 
					updated)
				{
					dataStatus = DataStatus.OK; 
					
					mainHandler.post (new NewTempTask (res, 
													   strOnline));
				}
			}
			else
			{
				if (DataStatus.Error != dataStatus)
				{
					dataStatus = DataStatus.Error; 
					mainHandler.post (new ErrorTempTask());
				}
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
	
	
	private boolean updateOldData (int[] 		res, 
								   String 		strOnline) 
	{
		boolean updated = false;
		
		if (!oldStrOnline.equals (strOnline))
		{
			oldStrOnline = strOnline;
			updated = true;
		}

		for (int i = 0; i < MAX_STRINGS; i++)
		{
			if (oldRes[i] != res[i])
			{
				oldRes[i] = res[i];
				updated = true;
			}
		}
		
		return updated;
	}


	private String readOnlineFilesData (String onlineFiles) 
	{
		String output = null;
		
		if (-1 == onlineCPUs)
		{
			onlineCPUs = 16;
		}
		
		for (int i = 0; i < onlineCPUs; i++)
		{
			String cpu = String.format (onlineFiles, 
										i);
			
			File 			file = new File(cpu);

			try 
			{
				BufferedReader br = new BufferedReader(new FileReader(file));
			    String str = br.readLine();
			    
			    if (i > 0)
			    {
			    	output += "-";
				    output += str;
			    }
			    else
			    {
			    	output = str;
			    }
			} 
			catch (Exception e) 
			{
				onlineCPUs = i;
				break;
			}
		}
		
		return output;
	}


	private int readFileData (String 	path)
	{
		File 			file = new File(path);

		try 
		{
			BufferedReader br = new BufferedReader(new FileReader(file));
		    String str = br.readLine();
		    
		    return Integer.parseInt (str);
		} 
		catch (Exception e) 
		{
			if (StateMachine.getExtensiveDebug())
			{
				e.printStackTrace();
			}
		}
	    
		return -1;
	}
}
