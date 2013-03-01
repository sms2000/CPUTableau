package com.ogp.cputableau;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import android.os.Handler;
import android.util.Log;
import android.widget.Toast;


public class WatchdogThread extends Thread
{
	private static final String TAG 		= "WatchdogThread";
	
	private boolean				threadRun 	= true;
	private String[]			tempFiles;
	private String[]			freqFiles;
	private String				onlineFiles;			
	private WatchdogCallback 	watchdogCallback;
	private Handler				mainHandler;				
	private long				pollingTime	= 250;
	private boolean 			suRecall  = false;
	private int					tempIndex	= -1;
	private int					freqIndex	= -1;
	private int					onlineCPUs	= -1;
	
	
    private class ErrorTempTask implements Runnable
    {

		public void run() 
		{
			watchdogCallback.errorTemp();
		}
    	
    }
    
    
    private class NewTempTask implements Runnable
    {
    	private int 	newTemp[];
    	private String	online;
    	
    	
    	NewTempTask(int temp[], 
    				String online)
    	{
    		this.newTemp 	= temp;
    		this.online		= online;
    	}
    	
    	
		public void run() 
		{
			watchdogCallback.setTemp (newTemp, 
									  online);
		}
    	
    }

    
    public WatchdogThread(String			tempFiles[],
    					  String			freqFiles[],
    					  String			onlineFiles,
						  WatchdogCallback	callback)		
	{
		initWatchdogThread (tempFiles,
							freqFiles,
							onlineFiles,
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
								     WatchdogCallback	callback)
	{
		this.tempFiles 		  = tempFiles;
		this.freqFiles 		  = freqFiles;
		this.onlineFiles	  = onlineFiles;
		this.watchdogCallback = callback;
		this.mainHandler	  = new Handler();
		
		start();
	}
	
	
	protected void loop()
	{
		int res[] = new int[3];
		
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
	 				}
				}
				
				if (tempIndex == -2)
				{
					break;
				}
				
 				res[0] = readFileData (tempFiles[tempIndex]);
				
 				if (res[0] <= 0)
 				{
 					Log.e(TAG, "Error recognizing CPU temp.");
 				}
 				else
 				{
 					if (StateMachine.getExtensiveDebug())
 					Log.d(TAG, "Result: " + res);
 					resOK = true;
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
	 				for (freqIndex = 0; freqIndex < freqFiles.length; freqIndex++)
	 				{
	 					new ShellInterface();
	 					ShellInterface.isSuAvailable();			
	 					ShellInterface.runCommand ("chmod 404 " + freqFiles[freqIndex]);
	 					
	 					if (0 < readFileData (freqFiles[freqIndex]))
	 					{
	 						break;
	 					}
	 				}
					
	 				if (freqIndex >= freqFiles.length)
	 				{
	 					freqIndex = -2;
	 				}
				}
				
				if (freqIndex == -2)
				{
					break;
				}

				
				res[1] = readFileData (freqFiles[freqIndex]);
				
				
 				if (res[1] <= 0)
 				{
 					Log.e(TAG, "Error recognizing CPU clock.");
 					mainHandler.post (new ErrorTempTask());
 				}
 				else
 				{
 					if (StateMachine.getExtensiveDebug())
 					Log.d(TAG, "Result: " + res);
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

			
			if (resOK)
			{
				mainHandler.post (new NewTempTask (res, 
												   strOnline));
			}
			else
			{
				mainHandler.post (new ErrorTempTask());
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
			e.printStackTrace();
		}
	    
		return -1;
	}
}
