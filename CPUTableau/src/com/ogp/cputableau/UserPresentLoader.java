package com.ogp.cputableau;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class UserPresentLoader extends BroadcastReceiver
{
	private static final String 	TAG 					= "UserPresentLoader";

	
	public UserPresentLoader()
	{
		super();
	}


	@Override
	public void onReceive (Context 		context,
						   Intent 		intent)
	{
		Log.v(TAG, "onReceive. Entry...");
		
		try
		{
			String str = intent.getAction();
			if (str.equals ("android.intent.action.USER_PRESENT"))
			{
				StateMachine.init (context);
				
				Log.d(TAG, "onReceive. User activated. Starting service if destroyed.");
				CPUTableauService.loadService (context);
			}

		}
		catch(Exception e)
		{
		}

		Log.v(TAG, "onReceive. Exit.");
	}
}
