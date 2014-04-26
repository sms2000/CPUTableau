package com.ogp.cputableau;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;


public class StateMachine
{
	public static final int			 		MIN_FONT_SIZE 			= 5;
	public static final int			 		DEF_FONT_SIZE 			= 24;
	public static final int 				MAX_FONT_SIZE 			= 50;
	
	public static final int 				MIN_REFRESH_MS 			= 100;
	public static final int 				DEF_REFRESH_MS 			= 250;
	public static final int 				MAX_REFRESH_MS 			= 5000;
	
	public static final int 				MIN_CLICK_TIME_MS 		= 50;
	public static final int 				DEF_CLICK_TIME_MS 		= 250;
	public static final int 				MAX_CLICK_TIME_MS 		= 300;
	
	public static final int 				MIN_LONG_PRESS_MS 		= 500;
	public static final int 				DEF_LONG_PRESS_MS 		= 1000;
	public static final int 				MAX_LONG_PRESS_MS 		= 1500;
	
	public static final int 				MIN_TAP_RADIUS_PC 		= 1;
	public static final int 				DEF_TAP_RADIUS_PC 		= 5;
	public static final int 				MAX_TAP_RADIUS_PC 		= 10;
	
	public static final int 				DEF_TRANSPARENCY		= 200;

	
	private static final String 			PERSISTANT_STORAGE 		= "T2TB";
	private static final String 			USE_OVERLAY 			= "UseOverlay";
	private static final String 			EXTENSIVE_DEBUG			= "ExtensiveDebug";
	private static final String 			USE_NOTIFY				= "UseNotify";
	private static final String 			USE_PWL					= "UsePWL";
	private static final String 			BT_SC_LOCK				= "BTScreenLock";
	private static final String 			TRANSPARENCY			= "Transparency";
	private static final String 			FONT_SIZE				= "FontSize";
	private static final String 			TEMP_SCALE				= "TempScale";
	private static final String 			REFRESH_MS				= "RefreshMs";
	private static final String 			CLICK_TIME_MS			= "ClickTimeMs";
	private static final String 			LONG_PRESS_MS			= "LongPressMs";
	private static final String 			TAP_RADIUS_PERCENT		= "TapRadiusPercent";
	private static final String 			SHOW_CURRENT			= "ShowCurrent";
		
	private static Context					appContext;
	
	private static boolean 					extensiveDebug;
	private static boolean 					useNotify;
	private static boolean 					useOverlay;
	private static boolean 					usePWL;
	private static boolean 					useBTSL;

	private static boolean 					activityRun;
	private static int 						transparency;
	private static int 						fontSize;
	private static boolean 					useFaherenheit;
	private static int 						refreshMs;
	private static int 						clickTimeMs;
	private static int 						longPressMs;
	private static int 						tapRadiusPercent;

	
	private static boolean 					screenOn;
	private static float 					batteryTemp;
	
	private static boolean					showChargeCurrent;
	
	
	private StateMachine()
	{
	}
	

	public static void init (Context		context)
	{
		appContext = context.getApplicationContext();
		
		
		activityRun					= false;
		screenOn					= true;
		batteryTemp					= -1.0f;
				
// Defaults
		useOverlay					= true;
		extensiveDebug				= false;
		useNotify					= false;
		usePWL						= false;
		useBTSL						= false;
		useFaherenheit				= false;
		showChargeCurrent			= false;
		
		refreshMs					= DEF_REFRESH_MS;
		clickTimeMs					= DEF_CLICK_TIME_MS;
		longPressMs					= DEF_LONG_PRESS_MS;
		tapRadiusPercent			= DEF_TAP_RADIUS_PC;
		
		transparency				= DEF_TRANSPARENCY;
		fontSize					= DEF_FONT_SIZE;
		
		readFromPersistantStorage();
	}
	
	
	public static void readFromPersistantStorage() 
	{
		SharedPreferences pref = appContext.getSharedPreferences (PERSISTANT_STORAGE, 
				  												  Context.MODE_PRIVATE);
		
		useOverlay			= pref.getBoolean 	(USE_OVERLAY, 			useOverlay);
		extensiveDebug 		= pref.getBoolean 	(EXTENSIVE_DEBUG, 		extensiveDebug);
		useNotify	 		= pref.getBoolean 	(USE_NOTIFY, 			useNotify);
		usePWL				= pref.getBoolean 	(USE_PWL, 				usePWL);
		useBTSL				= pref.getBoolean 	(BT_SC_LOCK,			useBTSL);
		transparency		= pref.getInt		(TRANSPARENCY, 			transparency);
		fontSize			= pref.getInt		(FONT_SIZE, 			fontSize);
		useFaherenheit		= pref.getBoolean 	(TEMP_SCALE,			useFaherenheit);
		refreshMs			= pref.getInt		(REFRESH_MS, 			refreshMs);
		clickTimeMs			= pref.getInt		(CLICK_TIME_MS, 		clickTimeMs);
		longPressMs			= pref.getInt		(LONG_PRESS_MS, 		longPressMs);
		tapRadiusPercent	= pref.getInt		(TAP_RADIUS_PERCENT,	tapRadiusPercent);
		showChargeCurrent	= pref.getBoolean 	(SHOW_CURRENT,			showChargeCurrent);
	}

	
	public static void writeToPersistantStorage() 
	{
		SharedPreferences pref = appContext.getSharedPreferences (PERSISTANT_STORAGE, 
																  Context.MODE_PRIVATE);
		
		Editor editor = pref.edit();
		
		editor.putBoolean	(USE_OVERLAY, 			useOverlay);
		editor.putBoolean	(EXTENSIVE_DEBUG, 		extensiveDebug);
		editor.putBoolean	(USE_NOTIFY, 			useNotify);
		editor.putBoolean	(USE_PWL, 				usePWL);
		editor.putBoolean	(BT_SC_LOCK, 			useBTSL);
		editor.putInt		(TRANSPARENCY, 			transparency);
		editor.putInt		(FONT_SIZE, 			fontSize);
		editor.putBoolean	(TEMP_SCALE, 			useFaherenheit);
		editor.putInt		(REFRESH_MS, 			refreshMs);
		editor.putInt		(CLICK_TIME_MS, 		clickTimeMs);
		editor.putInt		(LONG_PRESS_MS, 		longPressMs);
		editor.putInt		(TAP_RADIUS_PERCENT, 	tapRadiusPercent);
		editor.putBoolean	(SHOW_CURRENT, 			showChargeCurrent);
			
		editor.commit();
	}

	
// Preserved (global state)	
	public static boolean 	getExtensiveDebug		()				{return extensiveDebug;}
	public static void 		setExtensiveDebug		(boolean value)	{extensiveDebug = value;}

	public static boolean	getUseNotify			() 				{return useNotify;}
	public static void 		setUseNotify			(boolean value) {useNotify = value;}

	public static boolean 	getOverlay				() 				{return useOverlay;}
	public static void		setOverlay				(boolean value)	{useOverlay = value;}

	public static boolean 	getPWL					() 				{return usePWL;}
	public static void		setPWL					(boolean value)	{usePWL = value;}

	public static boolean 	getBTSL					() 				{return useBTSL;}
	public static void		setBTSL					(boolean value)	{useBTSL = value;}

	public static int 		getTransparency			() 				{return transparency;}
	public static void 		setTransparency			(int	 value)	{transparency = value;}

	public static int	 	getFontSize				() 				{return fontSize;}
	public static void		setFontSize				(int	 value) {fontSize = value;}

	public static boolean 	isFahrenheit			() 				{return useFaherenheit;}
	public static void		setFahrenheit			(boolean value)	{useFaherenheit = value;}

	public static int	 	getRefreshMs			() 				{return refreshMs;}
	public static void		setRefreshMs			(int	 value) {refreshMs = value;}

	public static int	 	getClickTimeMs			() 				{return clickTimeMs;}
	public static void		setClickTimeMs			(int	 value) {clickTimeMs = value;}

	public static int 		getLongPressTimeMs		() 				{return longPressMs;}
	public static void		setLongPressTimeMs		(int value) 	{longPressMs = value;}

	public static int	 	getTapRadiusPercent		() 				{return tapRadiusPercent;}
	public static void		setTapRadiusPercent		(int	 value) {tapRadiusPercent = value;}

	public static boolean	getChargeCurrent		()				{return showChargeCurrent;} 
	public static void		setChargeCurrent		(boolean value)	{showChargeCurrent = value;} 

	
// Not preserved (local state)	
	public static void 		setActivityRun			(boolean value) {activityRun = value;}
	public static boolean	isActivityRun			() 				{return activityRun;}

	public static void 		setScreenOn				(boolean value) {screenOn = value;}
	public static boolean	getScreenOn				() 				{return screenOn;}

	public static void      setBatteryTemp			(float value)	{batteryTemp = value;}
	public static float     getBatteryTemp			() 				{return batteryTemp;}
}

