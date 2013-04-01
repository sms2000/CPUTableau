package com.ogp.cputableau;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class BootLoader extends BroadcastReceiver
{
	public BootLoader()
	{
		super();
	}


	@Override
	public void onReceive (Context 		context,
						   Intent 		intent)
	{
		try
		{
			String str = intent.getAction();
			if (str.equals ("android.intent.action.BOOT_COMPLETED"))
			{
				StateMachine.init (context);
				
				CPUTableauService.loadService (context);
			}

		}
		catch(Exception e)
		{
		}
	}
}
