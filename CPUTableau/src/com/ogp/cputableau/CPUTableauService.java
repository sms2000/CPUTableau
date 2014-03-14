package com.ogp.cputableau;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;


public class CPUTableauService extends Service implements WatchdogCallback
{
	private static final String 	TAG 					= "CPUTableauService";

	private static final float 		PERCENT_LIMIT 			= 0.49f;

	private static CPUTableauService	thisService			= null; 

	private TelephonyManager 		telephonyManager 		= null;
	private PhoneStateListener		phoneStateListener		= new MyPhoneStateListener();
    private int						callState				= -1;
	
	private WatchdogThread			watchdogThread 			= null;
	private TransparentFrame	 	transparentFrame;

	private BroadcastReceiver 		batteryInfoReceiver;
	private BroadcastReceiver 		screenInfoReceiverOn;
	private BroadcastReceiver 		screenInfoReceiverOff;
	private boolean 				isForeground			= false;
	private WakeLock 				partialWakelock			= null;
	private WakeLock 				screenWakelock			= null;
	private Handler					handler					= new Handler();
	
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

	private static final String[]	chargeFiles				= new String[]{"/sys/class/power_supply/battery/current_now"};
	
	
	private class ActivatePane implements Runnable
	{
		public void run() 
		{
			transparentFrame = new TransparentFrame(CPUTableauService.this, 
													new TransparentContent(CPUTableauService.this)); 
		}
	}
	
	
	private class MyPhoneStateListener extends PhoneStateListener
	{
		public void onCallStateChanged (int 		state, 
										String 		incomingNumber) 
		{
			Log.v(TAG, String.format ("In-call state: %d", 
									  state));
			
			setNewCallState (state);
			
			
			super.onCallStateChanged (state, 
									  incomingNumber);
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
		
		setOverlayPaneInternal();
		
		telephonyManager = (TelephonyManager)thisService.getSystemService (Context.TELEPHONY_SERVICE);
		
		PowerManager pm = (PowerManager)getSystemService (Context.POWER_SERVICE);

		partialWakelock = pm.newWakeLock (PowerManager.PARTIAL_WAKE_LOCK, 
										  "Permanent PWL");
 
		screenWakelock = pm.newWakeLock (PowerManager.SCREEN_DIM_WAKE_LOCK, 
				  						 "Screen dimmed WL");
		
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

		telephonyManager.listen (phoneStateListener, 
		                         PhoneStateListener.LISTEN_CALL_STATE);
		
		wakeUp (true);
	}


	@Override
	public void onDestroy()
	{
    	Log.d(TAG, "onDestroy processing...");

		thisService = null;

		wakeUp (false);

		telephonyManager.listen (phoneStateListener, 
                				 PhoneStateListener.LISTEN_NONE);

    	unregisterReceiver (batteryInfoReceiver);
    	unregisterReceiver (screenInfoReceiverOn);
    	unregisterReceiver (screenInfoReceiverOff);
    	
    	if (null != transparentFrame)
    	{
    		transparentFrame.dismiss();
    		transparentFrame = null;
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
		}
		
    	pwlProcessing (wakeUp);
	}

	
	@SuppressLint("Wakelock")
	private void pwlProcessing (boolean wakeUp) 
	{
		if (null != partialWakelock)
		{
			if (wakeUp)
			{
				if (partialWakelock.isHeld())
				{
					partialWakelock.release();
	
					Log.w(TAG, "Permanent PWL is dropped successfully.");
				}
			}
			else if (StateMachine.getPWL())
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
			
			Log.d(TAG, "setItForeground. Bringing the service foreground...");
		}
	}
	
	
	private void stopItForeground() 
	{
	    if (isForeground) 
	    {
	    	isForeground = false;
	    	stopForeground (true);

	    	Log.d(TAG, "stopItForeground. Releasing the service from foreground...");
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

			Log.w(TAG, "Loader finished loading the Service.");
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

			Log.w(TAG, "Loader finished stopping the Service.");
		}
		catch(Exception e)
		{
			Log.e(TAG, "Loader failed to stop the Service. Exception.");
			e.printStackTrace();
		}
	}
	
	
	public static void setOverlayPane()
	{
		if (null != thisService)
		{
			thisService.setOverlayPaneInternal();
		}
	}
	
	
	private void setOverlayPaneInternal()
	{
		boolean now = StateMachine.getOverlay();
		
		if (now)
		{
			if (null == transparentFrame)
			{
				handler.post (new ActivatePane());
			}
			
			watchdogThread = new WatchdogThread(tempFiles, 
												freqFiles,
												onlineFiles,
												chargeFiles,
												this);

			setItForeground();
		}
		else
		{
			if (null != transparentFrame)
			{
				transparentFrame.dismiss();
				transparentFrame = null;
			}
			
    		if (null != watchdogThread)
    		{
    			watchdogThread.finalize();
    			watchdogThread = null;
    		}
		}
	}
		
	
	public void errorTemp() 
	{
		try
		{
			transparentFrame.errorTemp();
		}
		catch(Exception e)
		{
		}
	}


	public void setParams (int 		params[], 
						   String 	online) 
	{
		try
		{
			transparentFrame.setParams (params, 	
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
		
		float X = sharedPrefs.getFloat ("X", 
									 	0f);
		
		if (X <= -PERCENT_LIMIT)
		{
			X = -PERCENT_LIMIT;
		}
		else if (X > PERCENT_LIMIT)
		{
			X = PERCENT_LIMIT;
		}
		
		return X;
	}

	
	public float loadDefaultY()
	{
		SharedPreferences	sharedPrefs = getSharedPreferences ("Defaults", 
																0);

		float Y = sharedPrefs.getFloat ("Y", 
			 							0f);

		if (Y <= -PERCENT_LIMIT)
		{
			Y = -PERCENT_LIMIT;
		}
		else if (Y > PERCENT_LIMIT)
		{
			Y = PERCENT_LIMIT;
		}

		return Y;
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
		
		editor.commit();
	}

	
	public void setNewCallState (int newCallState) 
	{
		if (TelephonyManager.CALL_STATE_IDLE != newCallState)
		{
			newCallState = TelephonyManager.CALL_STATE_OFFHOOK;
		}
		
		if (newCallState == callState
			||
			!StateMachine.getBTSL())
		{
			return;
		}
		
		
		Log.e(TAG, String.format ("setNewCallState. New call state: [%s]",
								  (newCallState != TelephonyManager.CALL_STATE_IDLE) ? "active" : "off"));
		
		callState = newCallState;
		
		setNewScreenLockForBTState (newCallState != TelephonyManager.CALL_STATE_IDLE);
	}


	@SuppressLint("Wakelock")
	private void setNewScreenLockForBTState (boolean newBTSLState) 
	{
		if (null != screenWakelock)
		{
			if (!newBTSLState)
			{
				if (screenWakelock.isHeld())
				{
					screenWakelock.release();
	
					Log.w(TAG, "Screen dimmed WL is dropped successfully.");
				}
			}
			else if (StateMachine.getBTSL())
			{
				screenWakelock.acquire();
					
				Log.w(TAG, "Screen dimmed WL is acquired successfully.");
			}
		}
	}
}
