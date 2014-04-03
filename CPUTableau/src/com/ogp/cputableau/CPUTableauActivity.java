package com.ogp.cputableau;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;


public class CPUTableauActivity extends Activity
{
    @SuppressWarnings("unused")
	private static final String 	TAG 				= "CPUTableauActivity";

	private static final int 		MIN_FONT_SIZE 		= 5;
	private static final int 		MAX_FONT_SIZE 		= 50;
	private static final int 		MIN_REFRESH_MS 		= 100;
	private static final int 		MAX_REFRESH_MS 		= 5000;
	private static final int 		MIN_CLICK_TIME_MS 	= 50;
	private static final int 		MAX_CLICK_TIME_MS 	= 300;
	private static final int 		MIN_TAP_RADIUS_PC 	= 1;
	private static final int 		MAX_TAP_RADIUS_PC 	= 10;

	
    private CheckBox 				cbEnableOverlay		= null;
    private CheckBox 				cbEnableDebug		= null;
    private CheckBox 				cbEnableNotify 		= null;
    private CheckBox 				cbEnablePWL 		= null;
    private CheckBox 				cbEnableBTSL 		= null;
    private CheckBox 				cbUseFaherenheit	= null;

    private SeekBar 				sbTransparency		= null;
    private SeekBar 				sbFontSize			= null;
    private SeekBar 				sbRefreshMs			= null;
    private SeekBar 				sbClickTimeMs		= null;
    private SeekBar 				sbTapRadiusPC		= null;
    
    
    @Override
    public void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);
        
        StateMachine.init (this);
        
        requestWindowFeature (Window.FEATURE_NO_TITLE); 

        LayoutInflater li = getLayoutInflater();
        
        ViewGroup viewGroup = (ViewGroup)li.inflate (R.layout.setup, 
        								  			 null);
        setContentView (viewGroup);

        Button btOK = (Button)viewGroup.findViewById(R.id.btOK);
        btOK.setOnClickListener(new View.OnClickListener() 
        {
        	public void onClick (View paramView) 
			{
        		saveAndFinish();
			}
        });
        
        
        cbEnableOverlay = (CheckBox)viewGroup.findViewById (R.id.cbEnable);
        cbEnableOverlay.setOnCheckedChangeListener (new OnCheckedChangeListener()
        {
			public void onCheckedChanged (CompoundButton 	buttonView,
										  boolean 			isChecked) 
			{
				reloadOverlay();
			}
		});
        
        cbEnableDebug   	= (CheckBox)viewGroup.findViewById (R.id.cbDebug);
        cbEnableNotify  	= (CheckBox)viewGroup.findViewById (R.id.cbNotify);
        cbEnablePWL			= (CheckBox)viewGroup.findViewById (R.id.cbWakelock);
        cbEnableBTSL		= (CheckBox)viewGroup.findViewById (R.id.cbBTScreenLock);

        cbUseFaherenheit	= (CheckBox)viewGroup.findViewById (R.id.cbFaherenheit);
        cbUseFaherenheit.setOnCheckedChangeListener (new OnCheckedChangeListener()
        {
			public void onCheckedChanged (CompoundButton 	buttonView,
										  boolean 			isChecked) 
			{
				changeFaherenheit();
			}
		});

        sbTransparency  = (SeekBar) viewGroup.findViewById (R.id.sbTransparecy);
        sbTransparency.setOnSeekBarChangeListener (new OnSeekBarChangeListener()
        {
			public void onProgressChanged (SeekBar 	seekBar, 
										   int 		progress,
										   boolean 	fromUser) 
			{
				if (fromUser)
				{	
					changeTransparency (progress);
				}
			}


			public void onStartTrackingTouch (SeekBar seekBar) 
			{
			}


			public void onStopTrackingTouch (SeekBar seekBar) 
			{
			}
		});
        
        sbFontSize = (SeekBar) viewGroup.findViewById (R.id.sbFontSize);
        sbFontSize.setOnSeekBarChangeListener (new OnSeekBarChangeListener()
        {
			public void onProgressChanged (SeekBar 	seekBar, 
										   int 		progress,
										   boolean 	fromUser) 
			{
				if (fromUser)
				{	
					changeFontSize (progress);
				}
			}


			public void onStartTrackingTouch (SeekBar seekBar) 
			{
			}


			public void onStopTrackingTouch (SeekBar seekBar) 
			{
			}
		});

		sbRefreshMs = (SeekBar)viewGroup.findViewById (R.id.sbrefreshMs);
		sbRefreshMs.setOnSeekBarChangeListener (new OnSeekBarChangeListener()
        {
			public void onProgressChanged (SeekBar 	seekBar, 
										   int 		progress,
										   boolean 	fromUser) 
			{
				if (fromUser)
				{	
					changeRefreshMs (progress);
				}
			}


			public void onStartTrackingTouch (SeekBar seekBar) 
			{
			}


			public void onStopTrackingTouch (SeekBar seekBar) 
			{
			}
		});
        
		sbClickTimeMs = (SeekBar) viewGroup.findViewById (R.id.sbclickTimeMs);
		sbClickTimeMs.setOnSeekBarChangeListener (new OnSeekBarChangeListener()
        {
			public void onProgressChanged (SeekBar 	seekBar, 
										   int 		progress,
										   boolean 	fromUser) 
			{
				if (fromUser)
				{	
					changeclickTimeMs (progress);
				}
			}


			public void onStartTrackingTouch (SeekBar seekBar) 
			{
			}


			public void onStopTrackingTouch (SeekBar seekBar) 
			{
			}
		});

		sbTapRadiusPC = (SeekBar) viewGroup.findViewById (R.id.sbtapRadiusPercent);
		sbTapRadiusPC.setOnSeekBarChangeListener (new OnSeekBarChangeListener()
        {
			public void onProgressChanged (SeekBar 	seekBar, 
										   int 		progress,
										   boolean 	fromUser) 
			{
				if (fromUser)
				{	
					changeTapRadiusPC (progress);
				}
			}


			public void onStartTrackingTouch (SeekBar seekBar) 
			{
			}


			public void onStopTrackingTouch (SeekBar seekBar) 
			{
			}
		});

		
       	CPUTableauService.loadService (this);
    }
    
    
	@Override
	public void onResume()
	{
		super.onResume();

		StateMachine.setActivityRun (true);
		
        cbEnableOverlay.	setChecked  (StateMachine.getOverlay());
        cbEnableDebug.  	setChecked  (StateMachine.getExtensiveDebug());
        cbEnableNotify. 	setChecked  (StateMachine.getUseNotify());
        cbEnablePWL. 		setChecked  (StateMachine.getPWL());
        cbEnableBTSL. 		setChecked  (StateMachine.getBTSL());
        cbUseFaherenheit.	setChecked  (StateMachine.isFahrenheit());
        
        sbTransparency. 	setProgress (StateMachine.getTransparency());
        
        sbFontSize.			setMax		(MAX_FONT_SIZE - MIN_FONT_SIZE);
        sbFontSize. 		setProgress (StateMachine.getFontSize() - MIN_FONT_SIZE);
        
        sbRefreshMs.		setMax		(MAX_REFRESH_MS - MIN_REFRESH_MS);
        sbRefreshMs.		setProgress (StateMachine.getRefreshMs() - MIN_REFRESH_MS);
        
        sbClickTimeMs.		setMax		(MAX_CLICK_TIME_MS - MIN_CLICK_TIME_MS);
        sbClickTimeMs.		setProgress (StateMachine.getClickTimeMs() - MIN_CLICK_TIME_MS);

        sbTapRadiusPC.		setMax		(MAX_TAP_RADIUS_PC - MIN_TAP_RADIUS_PC);
        sbTapRadiusPC.		setProgress (StateMachine.getTapRadiusPercent() - MIN_TAP_RADIUS_PC);
    }

	
	@Override
	public void onPause()
	{
		super.onPause();
		
    	StateMachine.setOverlay 			(cbEnableOverlay.isChecked());

    	StateMachine.setExtensiveDebug 		(cbEnableDebug.isChecked());
		StateMachine.setUseNotify	  	 	(cbEnableNotify.isChecked());
		StateMachine.setPWL	 		  		(cbEnablePWL.isChecked());
		StateMachine.setBTSL 		  		(cbEnableBTSL.isChecked());
		StateMachine.setTransparency  		(sbTransparency.getProgress());
		StateMachine.setTransparency  		(sbTransparency.getProgress());
		StateMachine.setFontSize			(sbFontSize.getProgress() + MIN_FONT_SIZE);
		StateMachine.setFahrenheit			(cbUseFaherenheit.isChecked());
		StateMachine.setClickTimeMs			(sbClickTimeMs.getProgress() + MIN_CLICK_TIME_MS);
		StateMachine.setRefreshMs			(sbRefreshMs.getProgress() + MIN_REFRESH_MS);
		StateMachine.setTapRadiusPercent	(sbTapRadiusPC.getProgress() + MIN_TAP_RADIUS_PC);
		
        StateMachine.writeToPersistantStorage();
        
    	StateMachine.setActivityRun (false);
	}
    
	
    protected void changeTapRadiusPC (int progress) 
    {
		StateMachine.setTapRadiusPercent (sbTapRadiusPC.getProgress() + MIN_TAP_RADIUS_PC);

		CPUTableauService.quickUpdate();
	}


	protected void changeclickTimeMs (int progress) 
	{
		StateMachine.setClickTimeMs	(sbClickTimeMs.getProgress() + MIN_CLICK_TIME_MS);

		CPUTableauService.quickUpdate();
	}


	protected void changeRefreshMs (int progress) 
	{
		StateMachine.setRefreshMs (sbRefreshMs.getProgress() + MIN_REFRESH_MS);

		CPUTableauService.quickUpdate();
	}


	protected void changeFaherenheit() 
	{
		StateMachine.setFahrenheit (cbUseFaherenheit.isChecked());

		CPUTableauService.quickUpdate();
	}


	protected void changeTransparency (int progress) 
    {
		StateMachine.setTransparency (sbTransparency.getProgress());
		
		CPUTableauService.quickUpdate();
	}


    protected void changeFontSize (int progress) 
    {
		StateMachine.setFontSize (sbFontSize.getProgress() + MIN_FONT_SIZE);
		
		CPUTableauService.fullUpdate();
	}


    protected void reloadOverlay()
    {
    	StateMachine.setOverlay (cbEnableOverlay.isChecked());

    	CPUTableauService.setOverlayPane();			// Order of calls is critical!
        CPUTableauService.reloadForeground();
    }
    
    
    protected void saveAndFinish()
    {
    	finish();
    }
}
