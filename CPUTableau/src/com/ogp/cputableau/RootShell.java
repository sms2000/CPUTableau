package com.ogp.cputableau;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class RootShell {
	private static Process chperm = null;
    private static BufferedReader reader;
    private static BufferedWriter writer;
    
    
    static {
		try {
			chperm = Runtime.getRuntime().exec("su");
            reader = new BufferedReader(new InputStreamReader(chperm.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(chperm.getOutputStream()));
		} catch (IOException e) {
			chperm = null;	
		}
	}
	
	
	public static boolean executeOnRoot (String command) {
		boolean success = false;
		
		if (null != chperm) {
			try {
				writer.write(command, 0, command.length());
				writer.flush();
				Thread.currentThread().wait(100);
				success = true;
			} catch (Exception ignored) {
			}
		}
		
		return success;
	}
}
