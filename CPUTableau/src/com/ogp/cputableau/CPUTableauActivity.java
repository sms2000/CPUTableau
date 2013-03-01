package com.ogp.cputableau;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;


public class CPUTableauActivity extends Activity
{
    private static final String 	TAG 						= "CPUTableauActivity";
	
	private static final int 		MULTIPLY_ACCEL 				= 10;
	private static final int 		MAX_ACCEL 					= 20 * MULTIPLY_ACCEL;		// ~2g
    
    private CheckBox 				cbEnableOverlay		= null;
    private CheckBox 				cbEnableDebug		= null;
    private CheckBox 				cbEnableNotify 		= null;
    private SeekBar 				sbAccelValue		= null;
    
    
    @Override
    public void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);
        
        StateMachine.init (this);
        
        requestWindowFeature (Window.FEATURE_NO_TITLE); 

        LayoutInflater li = getLayoutInflater();
        
        ViewGroup viewGroup = (ViewGroup) li.inflate (R.layout.setup, 
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
        cbEnableDebug   = (CheckBox)viewGroup.findViewById (R.id.cbDebug);
        sbAccelValue	= (SeekBar) viewGroup.findViewById (R.id.sbAccel);
        cbEnableNotify  = (CheckBox)viewGroup.findViewById (R.id.cbNotify);
        
        cbEnableOverlay.setChecked (StateMachine.getOverlay());
        cbEnableDebug.  setChecked (StateMachine.getExtensiveDebug());
        cbEnableNotify. setChecked (StateMachine.getUseNotify());
        
        if (StateMachine.getOverlay())
        {
        	CPUTableauService.loadService (this);
        }
        
        
        sbAccelValue.setMax (MAX_ACCEL);
        sbAccelValue.setProgress ((int)(StateMachine.getAccelLimit() * MULTIPLY_ACCEL));
    }

    
    protected void saveAndFinish()
    {
    	if (StateMachine.getOverlay() != cbEnableOverlay.isChecked())
        {
        	if (StateMachine.getOverlay())
        	{
        		CPUTableauService.stopService (this);
        	}
        	else
        	{
        		CPUTableauService.loadService (this);
        	}
        }
    	

    	StateMachine.setOverlay 		(cbEnableOverlay.isChecked());
		StateMachine.setAccelLimit 		((float)sbAccelValue.getProgress() / MULTIPLY_ACCEL);
		StateMachine.setExtensiveDebug 	(cbEnableDebug.isChecked());
		StateMachine.setUseNotify	   	(cbEnableNotify.isChecked());

        StateMachine.writeToPersistantStorage();

        CPUTableauService.reloadForeground();
        
    	finish();
    }
}
