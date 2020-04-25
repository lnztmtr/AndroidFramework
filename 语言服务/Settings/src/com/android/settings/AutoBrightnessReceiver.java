package com.android.settings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;

public class AutoBrightnessReceiver extends BroadcastReceiver{
	
	private static String TAG = "AutoBrightnessReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Log.d(TAG,"on boot");
			SharedPreferences pref = context.getSharedPreferences(AutoBrightnessSwitch.AUTO_PREF_NAME, 
					Context.MODE_PRIVATE);
			boolean isOn = pref.getBoolean(AutoBrightnessSwitch.AUTO_PREF_ON_OFF,
                                            AutoBrightnessSwitch.DEFAULT);
			if(isOn) {
				Intent it = new Intent();
				it.setClass(context, AutoBrightnessService.class);
				context.startService(it);
			}
		}
	}

}
