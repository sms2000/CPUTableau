package com.ogp.cputableau;


public interface WatchdogCallback 
{
	public abstract void errorTemp 		();
	public abstract void setParams 		(int		params[], 
										 String 	online);

	public abstract void activateNow	();
}
