package com.android.settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.ContentObserver;
import android.os.BatteryManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import android.os.Handler;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;

import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

public class AutoBrightnessService extends Service{
	
	private static final String TAG = "AutoBrightnessService";
	
    private  int mMinimumBacklight;
    private  int mMaximumBacklight;
    private  IPowerManager mPower;
    private PowerManager mPM = null;
    private SharedPreferences mPref = null;
    private static final int BATTERY_NONE_PUGGED = 0;
	
    private static int mBatteryStatus = -1;
    private static int mBatteryHealth = -1;
    private static int mBatteryLevel = -1;
    private static int mBatteryVoltage = -1;
    private static int mBatteryScale = -1;
    private static int mBatteryPlugType = -1;
    private static int mOldBatteryPlugType = -1;
    
    private static HashMap<Integer , Integer > mDataMapNormal = null;
    private static HashMap<Integer , Integer > mDataMapCharging = null;
    private static ArrayList<Integer> mDataList = null;
    
    private BatteryReceiver mReceiver = null;
    
    private boolean mCurrSettingsOn = false;

    private static int sCurrLimit = -1;
    private boolean mSavePowerModel = false;
    
    private ArrayList<Integer> mTemps = new ArrayList<Integer>();

	class BatteryReceiver extends BroadcastReceiver 
	{

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if(intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
		        
		        mBatteryStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
		        mBatteryHealth = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
		        mBatteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		        mBatteryVoltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
		        mBatteryScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                mBatteryPlugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, BATTERY_NONE_PUGGED);

		        
		        Log.d(TAG,"onReceiver: mBatteryStatus="+mBatteryStatus+
		        		             " mBatteryHealth="+mBatteryHealth+
		        		             " mBatteryLevel="+mBatteryLevel+
		        		             " mBatteryVoltage="+mBatteryVoltage+
		        		             " mBatteryScale="+mBatteryScale+
                                     " mBatteryPlugType="+mBatteryPlugType);

                if(mBatteryPlugType == BatteryManager.BATTERY_PLUGGED_AC) {
                    Log.d(TAG,"ac plugged");
                } else if(mBatteryPlugType == BatteryManager.BATTERY_PLUGGED_USB) {
                    Log.d(TAG,"usb plugged");
                } else if(mBatteryPlugType == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
                    Log.d(TAG,"wireless plugged");
                } else {
                    Log.d(TAG,"none plugged");
                }
		        
		        generateCurrLimit(mBatteryLevel);
		        updateBrightness();
			}
		}
		
	}

    private void  loadData(int dataXmlRes) {
    	mDataMapNormal.clear();
        mDataMapCharging.clear();
        mDataList.clear();

        try {
            XmlResourceParser parser = this.getResources().getXml(dataXmlRes);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            
            int type;
            while ((type = parser.next()) != XmlPullParser.START_TAG
                    && type != XmlPullParser.END_DOCUMENT) {
                ;
            }

            final int depth = parser.getDepth();

            while (((type = parser.next()) != XmlPullParser.END_TAG ||
                    parser.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {

                if (type != XmlPullParser.START_TAG) {
                    continue;
                }

                final String name = parser.getName();
                
                TypedArray a = this.obtainStyledAttributes(attrs, R.styleable.DataItem);

                String battery = a.getString(R.styleable.DataItem_batteryLevel);
                String brightnessNormal = a.getString(R.styleable.DataItem_brightnessLevel);
                String brightnessCharging = a.getString(R.styleable.DataItem_brightnessChargingLevel);
                
                int batteryLevel = Integer.parseInt(battery);
                int brightnessLevelNormal = Integer.parseInt(brightnessNormal);
                int brightnessLevelCharging = Integer.parseInt(brightnessCharging);
                
                Log.d(TAG,"add item: batteryLevel="+batteryLevel+" brightnessNormal="+brightnessLevelNormal+
                           " brightnessCharging="+brightnessLevelCharging);
                
                if(!mDataList.contains(batteryLevel)) {
                	mDataList.add(batteryLevel);
                    mDataMapNormal.put(batteryLevel, brightnessLevelNormal);
                    mDataMapCharging.put(batteryLevel, brightnessLevelCharging);
                }

                if ("DataItem".equals(name)) {
                	
                }

                a.recycle();
            }
        } catch (XmlPullParserException e) {
            Log.w(TAG, "Got exception parsing favorites.", e);
        } catch (IOException e) {
            Log.w(TAG, "Got exception parsing favorites.", e);
        } catch (RuntimeException e) {
            Log.w(TAG, "Got exception parsing favorites.", e);
        }

    }
    
    private void generateCurrLimit(int batteryLevel)
    {
    	mTemps.clear();
    	for(int tem:mDataList) {
    		if(batteryLevel <= tem) {
    			mTemps.add(tem);
    		}
    	}

    	if(mTemps.size() == 0) {
            sCurrLimit = -1;
            return;
        }
    	
    	int min = mTemps.get(0);
    	for(int tem : mTemps) {
    		if(min > tem)  min = tem;
    	}

    	sCurrLimit = min;
    }

    private int getCurrLimitBrightness(int batteryLevel)
    {
        if(mBatteryPlugType == BATTERY_NONE_PUGGED) {
            return mDataMapNormal.get(batteryLevel);
        } else {
            return mDataMapCharging.get(batteryLevel);
        }
    }
    
    private void updateBrightness()
    {

    	if(mDataList.isEmpty()) {
    		loadData(R.xml.auto_brightness_map);
    	}
    	mCurrSettingsOn = mPref.getBoolean(AutoBrightnessSwitch.AUTO_PREF_ON_OFF, 
                                                    AutoBrightnessSwitch.DEFAULT);


    	Log.d(TAG,"curr Settings:"+mCurrSettingsOn);
    	if(mCurrSettingsOn) {
            mSavePowerModel = true;
            android.os.SystemProperties.set("sys.brightness.sp","on");
        } else {
            android.os.SystemProperties.set("sys.brightness.sp","off");
            if(mSavePowerModel) {
                mSavePowerModel = false;
            } else {
                return;
            }
        }

        Log.d(TAG,"do updateBrightness");
    
        if(mSavePowerModel) {

            /*
            Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            */
            int currMode = Settings.System.getInt(this.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS_MODE, 
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);

            if(currMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                if(sCurrLimit < 0) {
                    mPM.setBrightnessSavePower(-1);
                } else {
                    mPM.setBrightnessSavePower(getCurrLimitBrightness(sCurrLimit)); 
                }
            }else{
                int currVal = Settings.System.getInt(this.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS,255);
                if(sCurrLimit < 0) {
                    mPM.setBrightnessSavePower(-1);
                } else {
                    int limitVal = getCurrLimitBrightness(sCurrLimit); 
                    mPM.setBrightnessSavePower(limitVal < currVal ? limitVal : currVal);
                }
            }
        } else {
            mPM.setBrightnessSavePower(-1);
        }
    }

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private ContentObserver mBrightnessObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
        	updateBrightness();
        }
  };

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		Log.d(TAG,"service create");

        sCurrLimit = -1;
		mReceiver = new BatteryReceiver();
		mDataMapNormal = new HashMap<Integer , Integer>();
        mDataMapCharging = new HashMap<Integer , Integer>();
		mDataList = new ArrayList<Integer>();
		mPref = this.getSharedPreferences(AutoBrightnessSwitch.AUTO_PREF_NAME, Context.MODE_PRIVATE);

        mPM = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
        mMinimumBacklight = mPM.getMinimumScreenBrightnessSetting();
        mMaximumBacklight = mPM.getMaximumScreenBrightnessSetting();
        
        mPower = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
        
		loadData(R.xml.auto_brightness_map);
		
		IntentFilter infl = new IntentFilter();
		infl.addAction(Intent.ACTION_BATTERY_CHANGED);
		this.registerReceiver(mReceiver, infl);
		this.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), true,
                mBrightnessObserver);
	}

	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		Log.d(TAG,"service onDestroy");
		this.unregisterReceiver(mReceiver);
        mPM.setBrightnessSavePower(-1);
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		Log.d(TAG,"service onStartCommand");
        updateBrightness();
		return super.onStartCommand(intent, flags, startId);
	}

}
