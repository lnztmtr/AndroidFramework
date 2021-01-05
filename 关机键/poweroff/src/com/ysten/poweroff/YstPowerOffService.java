package com.ysten.poweroff;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import android.os.SystemProperties;
import com.hisilicon.android.hisysmanager.HiSysManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.content.ContentResolver;
public class YstPowerOffService extends Service {

	private static final String TAG = "YstPowerOffService";
	private PowerManager mPowerManager;
	private PoweroffDialog dialog;
	private int HomeKeyStatus = 0;
	HiSysManager hisys;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate");
	}

	@Override
	@Deprecated
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		Log.d(TAG, "onStart");
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		Log.d(TAG, "onDestroy");
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onStartCommand");
		//modify by zws ,fix killed restart
		showPoweroffDialog();
		return START_NOT_STICKY;
	}

	private void showPoweroffDialog() {
		Log.d(TAG, "....showPoweroffDialog.......");
		HomeKeyStatus = Settings.System.getInt(this.getContentResolver(),
				"IS_RESPOND_HOME_KEY", 0);
		Log.d(TAG, "0000HomeKeyStatus="+HomeKeyStatus);
		Settings.System.putInt(this.getContentResolver(),
				"IS_RESPOND_HOME_KEY", 1);
		Settings.System.putInt(this.getContentResolver(),
				"IS_RESPOND_MENU_KEY", 1);
		Settings.System.putInt(this.getContentResolver(),
				"IS_RESPOND_SETTING_KEY", 1);
		if (dialog == null)
			dialog = new PoweroffDialog(this, R.style.dialog);
		dialog.getWindow()
				.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		Log.d(TAG,
				"....showPoweroffDialog...dialog.isShowing()..."
						+ dialog.isShowing());
		if (!dialog.isShowing())
			dialog.show();

		dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

			@Override
			public void onDismiss(DialogInterface arg0) {
				// TODO Auto-generated method stub
				Log.d(TAG, "onDismiss");
				Log.d(TAG, "HomeKeyStatus="+HomeKeyStatus);
				Settings.System.putInt(getContentResolver(),
						"IS_RESPOND_HOME_KEY", HomeKeyStatus);
				Settings.System.putInt(getContentResolver(),
						"IS_RESPOND_MENU_KEY", 0);
				Settings.System.putInt(getContentResolver(),
						"IS_RESPOND_SETTING_KEY", 0);

			}
		});

		/* set size & pos */
		WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
		WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		lp.width = (int) (display.getWidth() * 1.0);
		lp.height = (int) (display.getHeight() * 1.0);
		dialog.getWindow().setAttributes(lp);

	}

	public class PoweroffDialog extends AlertDialog implements
			android.view.View.OnClickListener {

		private ImageView mBtnStandby;
		private ImageView mBtnPoweroff;
		private ImageView mBtnReboot;
		private ImageView mBtnCancel;
		private LinearLayout mBtnLayout;
		private LinearLayout mPoweroffIngLayout;
        private ContentResolver mResolver;
        public PoweroffDialog(Context context, int theme) {
            super(context, theme);
            mResolver = context.getContentResolver();

		}

		public PoweroffDialog(Context context) {
			super(context);
            mResolver = context.getContentResolver();
		}

		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.poweroff_dialog);
			mBtnLayout = (LinearLayout) findViewById(R.id.poweroff_button_layout);
			mPoweroffIngLayout = (LinearLayout) findViewById(R.id.poweroff_ing_layout);
			mBtnStandby = (ImageView) findViewById(R.id.standby);
			mBtnPoweroff = (ImageView) findViewById(R.id.poweroff);
			mBtnReboot = (ImageView) findViewById(R.id.reboot);
			mBtnCancel = (ImageView) findViewById(R.id.cancel);
			mBtnStandby.setOnClickListener(this);
			mBtnPoweroff.setOnClickListener(this);
			mBtnReboot.setOnClickListener(this);
			mBtnCancel.setOnClickListener(this);
			mBtnPoweroff.requestFocus();
		}

		// hide button.show poweroff ing text
		public void showPoweroffText() {
			mBtnLayout.setVisibility(View.GONE);
			mPoweroffIngLayout.setVisibility(View.VISIBLE);
		}

		@Override
		public void onClick(View view) {
			Log.d(TAG, "view.getId(-->>" + view.getId());
			hisys=new HiSysManager();
			Intent mIntent;
			switch (view.getId()) {
			
			case R.id.standby:
                dismiss();
                //add for jiangxi smart_suspend 
                if(SystemProperties.get("ro.ysten.province","master").contains("jiangxi")){
                    SystemProperties.set("persist.sys.smartsuspendin", "1");
                    int sleepDelay = Settings.System.getInt(mResolver,"sleep_time",-1);
                    if (sleepDelay > 0){
                        Log.d(TAG,"smart_suspend in jiangxi sleepdelay = "+ sleepDelay);
                        SystemProperties.set("persist.sys.screentimeout", "true");
                        Settings.System.putInt(mResolver, Settings.System.SCREEN_OFF_TIMEOUT, sleepDelay*60*1000);
                    }else{
                        SystemProperties.set("persist.sys.screentimeout", "false");
                        //Settings.System.putInt(mResolver, Settings.System.SCREEN_OFF_TIMEOUT, -1);
                    } 
                    SystemProperties.set("persist.suspend.mode", "smart_suspend");
                    hisys.setWakeUpAttr("smart_suspend"); 
                }else if(SystemProperties.get("ro.ysten.province","master").contains("neimeng")){
		    //begin :add by ysten wenglei at 20201022: standby to poweroff
		    showPoweroffText();
		    hisys.setProperty("persist.suspend.mode", "deep_restart");
		    SystemProperties.set("persist.sys.smartsuspendin", "0");
		    /*	Intent mIntent = new Intent(Intent.ACTION_REQUEST_SHUTDOWN);
			mIntent.putExtra(Intent.EXTRA_KEY_CONFIRM, false);
			mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(mIntent); */
			//SystemProperties.set("persist.suspend.mode", "deep_resume");
			//SystemProperties.set("persist.suspend.mode", "deep_restart");
			try
			{
			    Process p = Runtime.getRuntime().exec("/system/bin/sh /system/bin/hello1.sh 1");
			    Log.i("ysten_sgc", "set hello1.sh to 1 ");

                    	    String data = "";
			    BufferedReader ie = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			    BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
			    String error = null;
			    while ((error = ie.readLine()) != null && !error.equals("null")) {
				data += error + "\n";
			    }
			    String line = null;
			    while ((line = in.readLine()) != null && !line.equals("null")) {
				data += line + "\n";
				//Log.v("ls", line);
			    }
			    Log.i("xwj", "Show Suspend Dialog ok"+data);
			}
			catch(IOException e)
			{
			    Log.i("xwj", "Show Suspend Dialog error");
			}
		//          mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		//	    mPowerManager.goToSleep(SystemClock.uptimeMillis());
			//end :add by ysten wenglei at 20201022: standby to poweroff
		}else{
                    //SystemProperties.set("persist.suspend.mode", "smart_suspend");
                    hisys.setProperty("persist.suspend.mode", "smart_suspend");
                    SystemProperties.set("persist.sys.smartsuspendin", "1");  
                    try{
                        Process p = Runtime.getRuntime().exec("/system/bin/sh /system/bin/hello1.sh 0");
                        Log.i("ysten_sgc", "set hello1.sh to 0 ");
                        String data = "";
                        BufferedReader ie = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        String error = null;
                        while ((error = ie.readLine()) != null && !error.equals("null")) {
                            data += error + "\n";
                        }
                        String line = null;
                        while ((line = in.readLine()) != null && !line.equals("null")) {
                            data += line + "\n";
                            //Log.v("ls", line);
                        }
                        Log.i("xwj", "Show Suspend Dialog ok"+data);
                    }catch(IOException e){
                        Log.i("xwj", "Show Suspend Dialog error");
                    }
                }
		//		mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		//		mPowerManager.goToSleep(SystemClock.uptimeMillis());
		        mIntent = new Intent("android.ysten.systemupdate");
				mIntent.putExtra("powercontrol", "poweroff");
				sendBroadcast(mIntent);
				break;
			
			case R.id.poweroff:
				showPoweroffText();
				hisys.setProperty("persist.suspend.mode", "deep_restart");
				SystemProperties.set("persist.sys.smartsuspendin", "0");
			/*	Intent mIntent = new Intent(Intent.ACTION_REQUEST_SHUTDOWN);
				mIntent.putExtra(Intent.EXTRA_KEY_CONFIRM, false);
				mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(mIntent); */
				//SystemProperties.set("persist.suspend.mode", "deep_resume");
				//SystemProperties.set("persist.suspend.mode", "deep_restart");
				try
				{
			Process p = Runtime.getRuntime().exec("/system/bin/sh /system/bin/hello1.sh 1");
			Log.i("ysten_sgc", "set hello1.sh to 1 ");

                String data = "";
		BufferedReader ie = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String error = null;
		while ((error = ie.readLine()) != null && !error.equals("null")) {
			data += error + "\n";
			}
		String line = null;
		while ((line = in.readLine()) != null && !line.equals("null")) {
			data += line + "\n";
			//Log.v("ls", line);
			}
								Log.i("xwj", "Show Suspend Dialog ok"+data);
								}
								catch(IOException e)
								{
								Log.i("xwj", "Show Suspend Dialog error");
								}
		//		mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		//		mPowerManager.goToSleep(SystemClock.uptimeMillis());
				mIntent = new Intent("android.ysten.systemupdate");
				mIntent.putExtra("powercontrol", "poweroff");
				sendBroadcast(mIntent);
				break;
			case R.id.reboot:
			    SystemProperties.set("persist.sys.smartsuspendin", "0");
				dismiss();
				mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
				mPowerManager.reboot("");
				break;
			case R.id.cancel:
				dismiss();
				break;
			default:
				break;
			}
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onBind");
		return null;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onUnbind");
		return super.onUnbind(intent);
	}

}
