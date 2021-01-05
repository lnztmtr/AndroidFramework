package com.android.silenceinstaller;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.util.Log;


public class StreamGobbler extends Thread 
{
	InputStream is;
    String type;
    String LastLine;
    
    StreamGobbler(InputStream is, String type)
    {
        this.is = is;
        this.type = type;
        this.LastLine = "";
    }
    
    public void run()
    {
    	try
        {
    		String CurrentLine;
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            while ((CurrentLine = br.readLine()) != null)
            {
           		Log.i("SilenceInstallService", type + ">" + CurrentLine);
           		LastLine = CurrentLine;
            }
        } 
        catch (IOException ioe)
        {
            ioe.printStackTrace();  
        } 
/*    	catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	*/
    }
    
    public String GetLastResponse()
    {
    	return LastLine;
    }
}
