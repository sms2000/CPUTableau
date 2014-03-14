package com.ogp.cputableau;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;


public class CPUTableauActivity extends Activity
{
    @SuppressWarnings("unused")
	private static final String 	TAG 						= "CPUTableauActivity";
	
    private CheckBox 				cbEnableOverlay		= null;
    private CheckBox 				cbEnableDebug		= null;
    private CheckBox 				cbEnableNotify 		= null;
    private CheckBox 				cbEnablePWL 		= null;
    private CheckBox 				cbEnableBTSL 		= null;
    
    
    @Override
    public void onCreate (Bundle savedInstanceState)
    {
        super.onCreate (savedInstanceState);
        
        StateMachine.init (this);
        StateMachine.setActivityRun (true);
        
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
        cbEnableNotify  = (CheckBox)viewGroup.findViewById (R.id.cbNotify);
        cbEnablePWL		= (CheckBox)viewGroup.findViewById (R.id.cbWakelock);
        cbEnableBTSL	= (CheckBox)viewGroup.findViewById (R.id.cbBTScreenLock);
        
        cbEnableOverlay.setChecked (StateMachine.getOverlay());
        cbEnableDebug.  setChecked (StateMachine.getExtensiveDebug());
        cbEnableNotify. setChecked (StateMachine.getUseNotify());
        cbEnablePWL. 	setChecked (StateMachine.getPWL());
        cbEnableBTSL. 	setChecked (StateMachine.getBTSL());
        
        if (StateMachine.getOverlay())
        {
        	CPUTableauService.loadService (this);
        }
    }

    
    protected void saveAndFinish()
    {
    	StateMachine.setOverlay 		(cbEnableOverlay.isChecked());
		StateMachine.setExtensiveDebug 	(cbEnableDebug.isChecked());
		StateMachine.setUseNotify	   	(cbEnableNotify.isChecked());
		StateMachine.setPWL	 		  	(cbEnablePWL.isChecked());
		StateMachine.setBTSL 		  	(cbEnableBTSL.isChecked());

        StateMachine.writeToPersistantStorage();

        CPUTableauService.setOverlayPane();			// Order of calls is critical!
        CPUTableauService.reloadForeground();
        
        
    	finish();

    	StateMachine.setActivityRun (false);
    }
}
