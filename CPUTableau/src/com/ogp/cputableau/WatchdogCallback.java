package com.ogp.cputableau;


public interface WatchdogCallback 
{
	public abstract void errorTemp ();
	public abstract void setTemp   (int	temp[], 
									String online);
}
