package com.ogp.cputableau.providers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import android.annotation.SuppressLint;
import com.ogp.cputableau.StateMachine;

public abstract class HWProvider 
{
	protected static int readFileInt (String 	path)
	{
		File file = new File(path);

		try 
		{
			BufferedReader br = new BufferedReader(new FileReader(file));
		    String str = br.readLine();
		    br.close();
		    
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


	protected static String readFileString (String 	path)
	{
		File file = new File(path);

		try 
		{
			BufferedReader br = new BufferedReader(new FileReader(file));
		    String str = br.readLine();
		    br.close();
		    
		    return str;
		} 
		catch (Exception e) 
		{
			if (StateMachine.getExtensiveDebug())
			{
				e.printStackTrace();
			}
		}
	    
		return null;
	}

	
	@SuppressLint("DefaultLocale")
	protected static String temperatureDouble2StringString (double dres) 
	{
		if (StateMachine.isFahrenheit())
		{
			return String.format ("%.1f°F", dres * 1.8f + 32.0f);
		}
		else
		{
			return String.format ("%.1f°C", dres);
		}
	}

	
	public abstract String  	getData();
	
	public void finalize()
	{
	}
}
