package com.ogp.cputableau;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.WindowManager;


public class CPUTableauService extends Service implements WatchdogCallback, 
															 SensorEventListener
{
	private static final String 	TAG 					= "CPUTableauService";

	private static final int 		EVENT_DELTA 			= 500000;
	private static final int 		WAKELOCK_RELEASE		= 5000;				// 5 seconds
	
	private static CPUTableauService	thisService			= null; 

	private WatchdogThread			watchdogThread 			= null;
    private AccelThread				accelThread 			= null;
	private TransparentContent	 	transparentContent;

	private BroadcastReceiver 		batteryInfoReceiver;
	private BroadcastReceiver 		screenInfoReceiverOn;
	private BroadcastReceiver 		screenInfoReceiverOff;
	private boolean 				isForeground			= false;
	private SensorManager 			sensorManager;
	private Sensor 					hardwareSensor;
	private boolean 				registeredSensor		= false;
	private	boolean					phoneWaking				= false;
	private WakeLock 				wakeLock				= null;
	private WakeLock 				partialWakelock			= null;
	
	
	private static final String[]	tempFiles				= new String[]{
																			"/sys/devices/system/cpu/cpu0/cpufreq/cpu_temp",
																			"/sys/devices/system/cpu/cpu0/cpufreq/FakeShmoo_cpu_temp",
																			"/sys/class/thermal/thermal_zone1/temp",						// HTC Evo 3D
																			"/sys/class/i2c-adapter/i2c-4/4-004c/temperature",
																			"/sys/devices/platform/omap/omap_temp_sensor.0/temperature",
																			"/sys/devices/platform/tegra_tmon/temp1_input",					// Atrix 4G
																			"/sys/kernel/debug/tegra_thermal/temp_tj",
																			"/sys/devices/platform/s5p-tmu/temperature",       				// Galaxy S3, Note 2
																			"/sys/class/thermal/thermal_zone0/temp",
																			"/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_cur_freq",
																		   };
	
	private static final String[]	freqFiles				= new String[]{"/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"};

	private static final String		onlineFiles				= "/sys/devices/system/cpu/cpu%d/online";

	
	private class ReleaseWakeLock implements Runnable
	{
		public void run() 
		{
			releaseWakeLock();
		}
	}
	

	@Override
	public IBinder onBind (Intent intent)
	{
		return null;
	}


	@Override
	public void onCreate()
	{
		super.onCreate();

		Log.d(TAG, "onCreate");

		thisService = this;
		
		StateMachine.init (this);
		
		transparentContent 	= new TransparentContent(this);
		sensorManager		= (SensorManager)getSystemService (SENSOR_SERVICE);
		
		hardwareSensor = sensorManager.getDefaultSensor (Sensor.TYPE_ACCELEROMETER);
 
		batteryInfoReceiver = new BroadcastReceiver()
		{
		    @Override
		    public void onReceive (Context 	arg0,
		    					   Intent 	intent)
		    {
		    	int temperature = intent.getIntExtra (BatteryManager.EXTRA_TEMPERATURE,
		    		  								  0);

				if (StateMachine.getExtensiveDebug())
		    	Log.d(TAG, "Battery temperature: " + (float)temperature / 10.0);
		    }
		};

		registerReceiver (batteryInfoReceiver,
						  new IntentFilter (Intent.ACTION_BATTERY_CHANGED));

		screenInfoReceiverOn = new BroadcastReceiver()
		{
		    @Override
		    public void onReceive (Context 	arg0,
		    					   Intent 	intent)
		    {
		    	Log.d(TAG, "Screen ON");
		    	
	    		wakeUp (true);
		    }
		};

		registerReceiver (screenInfoReceiverOn,
						  new IntentFilter (Intent.ACTION_SCREEN_ON));

		screenInfoReceiverOff = new BroadcastReceiver()
		{
		    @Override
		    public void onReceive (Context 	arg0,
		    					   Intent 	intent)
		    {
		    	Log.d(TAG, "Screen OFF");
		    	
		    	wakeUp (false);
		    }
		};

		
		registerReceiver (screenInfoReceiverOff,
				  		  new IntentFilter (Intent.ACTION_SCREEN_OFF));

		wakeUp (true);
		
// MMMMMMMMMMM		accelThread = new AccelThread();
	}


	@Override
	public void onDestroy()
	{
    	Log.d(TAG, "onDestroy processing...");

		thisService = null;

		if (null != accelThread)
		{
			accelThread.finalize();
			accelThread = null;
		}

		wakeUp (false);
    	registerShakeDetector (false);

    	unregisterReceiver (batteryInfoReceiver);
    	unregisterReceiver (screenInfoReceiverOn);
    	unregisterReceiver (screenInfoReceiverOff);
    	
		WindowManager windowManager = (WindowManager)getSystemService (Context.WINDOW_SERVICE);

		try
		{
			windowManager.removeView (transparentContent);
			transparentContent = null;
		}
		catch(Exception e)
		{
		}
    	
		super.onDestroy();
	}


	@Override
	public void onLowMemory()
	{
		super.onLowMemory();
		
		Log.e(TAG, "onLowMemory");
	}

	
	private void wakeUp (boolean wakeUp)
	{
		if (!wakeUp)
		{
	    	stopItForeground();
	    	
// MMMMMMMMMMMMMMM	    	registerShakeDetector (true);
		}
		
		if (null != watchdogThread)
		{
			watchdogThread.finalize();
			watchdogThread = null;
		}
    	
    	if (wakeUp)
		{
			if (null != watchdogThread)
			{
				return;
			}
			
			watchdogThread = new WatchdogThread (tempFiles, 
												 freqFiles,
												 onlineFiles,
												 this);
			
	    	setItForeground();

// MMMMMMMMMMMMMMM	    	registerShakeDetector (false);
		}
    	
    	
    	pwlProcessing (wakeUp);
	}

	
	private void pwlProcessing (boolean wakeUp) 
	{
		if (wakeUp)
		{
			try
			{
				partialWakelock.release();

				Log.w(TAG, "Permanent PWL is dropped successfully.");
			}
			catch(Exception e)
			{
				Log.w(TAG, "Permanent PWL is not dropped.");
			}

			partialWakelock = null;
		}
		else if (StateMachine.getPWL())
		{
			PowerManager pm = (PowerManager)getSystemService (Context.POWER_SERVICE);

			partialWakelock = pm.newWakeLock (PowerManager.PARTIAL_WAKE_LOCK, 
											  "Permanent PWL");
			
			if (null != partialWakelock)
			{
				partialWakelock.acquire();
				
				Log.w(TAG, "Permanent PWL is acquired successfully.");
			}
		}
	}


	private void setItForeground()
	{
		if (!isForeground
			&&
			StateMachine.getUseNotify())
		{
			isForeground = true;
			
			Notification note = new Notification(R.drawable.icon,
											     getResources().getString (R.string.notify),
											     System.currentTimeMillis());

			Intent intent = new Intent(this, 
				 					   CPUTableauService.class);

			intent.setFlags (Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

			PendingIntent pi = PendingIntent.getActivity (this, 
				 									   	  0,
				 									   	  intent, 
				 									   	  0);

			note.setLatestEventInfo (this, 
								     getResources().getString (R.string.app_name),
				 				  	 getResources().getString (R.string.notify_active),
				 				  	 pi);
		 
			note.flags |= Notification.FLAG_NO_CLEAR;

			startForeground (1, 
				 		  	 note);
		}
	}
	
	
	private void stopItForeground() 
	{
	    if (isForeground) 
	    {
	    	isForeground = false;
	    	stopForeground (true);
	    }
	}

	
	static public void reloadForeground()
	{
		if (null != thisService)
		{
			thisService.reloadForegroundInternal();
		}
	}
	
	
	private void reloadForegroundInternal()
	{
		stopItForeground();
		
		setItForeground();
	}
	
	
	static void loadService (Context context)
	{
		Log.w(TAG, "Loader is attempting to load the Service.");

		try
		{
			Intent intent = new Intent(context,
								   	   CPUTableauService.class);

			context.startService (intent);

			Log.w(TAG, "Loader finised loading the Service.");
		}
		catch(Exception e)
		{
			Log.e(TAG, "Loader failed to load the Service. Exception.");
			e.printStackTrace();
		}
	}

	
	static void stopService (Context context)
	{
		Log.w(TAG, "Loader is attempting to stop the Service.");

		try
		{
			Intent intent = new Intent(context,
								   	   CPUTableauService.class);

			context.stopService (intent);

			Log.w(TAG, "Loader finised stopping the Service.");
		}
		catch(Exception e)
		{
			Log.e(TAG, "Loader failed to stop the Service. Exception.");
			e.printStackTrace();
		}
	}
	
	
	
	public void errorTemp() 
	{
		try
		{
			transparentContent.errorTemp();
		}
		catch(Exception e)
		{
		}
	}


	public void setTemp (int 		temp[], 
						 String 	online) 
	{
		try
		{
			transparentContent.setTemp (temp, 	
										online);
		}
		catch(Exception e)
		{
		}
	}
	
	
	public float loadDefaultX()
	{
		SharedPreferences	sharedPrefs = getSharedPreferences ("Defaults", 
																0);
		
		return sharedPrefs.getFloat ("X", 
									 0f);
	}

	
	public float loadDefaultY()
	{
		SharedPreferences	sharedPrefs = getSharedPreferences ("Defaults", 
																0);

		return sharedPrefs.getFloat ("Y", 
									 0f);
	}
	
	
	public void saveDefaultXY (float x,
							   float y)
	{
		SharedPreferences	sharedPrefs = getSharedPreferences ("Defaults", 
																0);
		
		Editor editor = sharedPrefs.edit();

		editor.putFloat ("X", 
						 x);

		editor.putFloat ("Y", 
				 		 y);
		
		editor.apply();
	}

	
	private void registerShakeDetector (boolean register) 
	{
		if (registeredSensor)
		{
			sensorManager.unregisterListener (this);
			
			registeredSensor = false;
		}
			
		
		if (register)
		{
			sensorManager.registerListener (this, 
											hardwareSensor, 
											EVENT_DELTA);			// 2 events per second
			
			registeredSensor = true;
		}
	}


	public void onAccuracyChanged (Sensor 	sensor, 
								   int 		accuracy) 
	{
	}


	public void onSensorChanged (SensorEvent event) 
	{
		Log.v(TAG, String.format ("onSensorChanged. Values: %.2f  %.2f  %.2f",
							      event.values[0],
							      event.values[1],
							      event.values[2]));

		
		
/*		if (!useLinear)
		{
// Manual linear accelerometer
			final float alpha = 0.8f;

			gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
			gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
			gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

			event.values[0] -= gravity[0];
			event.values[1] -= gravity[1];
			event.values[2] -= gravity[2];		
		}
		
		float value = Math.abs (event.values[1]);
		
		memory[memoryCounter++] = value;
		memoryCounter %= memoryLimit;

		float aver = 0;
		for (int i = 0; i < memoryLimit; i++)
		{
			aver += memory[i]; 
		}
		
		
		aver /= memoryLimit;
		
		Log.v(TAG, String.format ("onSensorChanged. Value (aver) Y: %.1f",
				  					aver));

		if (aver > StateMachine.getAccelLimit()
			&&
			StateMachine.getAccelLimit() > 0)
		{
			wakeThePhone();
		}  */
	}


	@SuppressWarnings("unused")
	private void wakeThePhone() 
	{
		if (phoneWaking)
		{
			return;
		}
		
		
		phoneWaking = true;
		
		PowerManager pm = (PowerManager) getSystemService (Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock (PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, 
								   TAG);
		wakeLock.acquire();
		Log.d(TAG, "WakeLock acquired.");
		
		new Handler().postDelayed (new ReleaseWakeLock(),
								   WAKELOCK_RELEASE);	
	}
	
	
	private void releaseWakeLock()
	{
		if (phoneWaking)
		{
			try
			{
				wakeLock.release();
				Log.d(TAG, "WakeLock released.");
			}
			catch(Exception e)
			{
				
			}
			
			wakeLock 	= null;		
			phoneWaking = false;
		}
	}
}
