package com.android.settings.pppoe;

import com.android.settings.R;

import java.util.Timer;
import java.util.TimerTask;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.util.Log;
import android.net.wifi.WifiManager;
import android.net.ethernet.EthernetStateTracker;
import android.net.ethernet.EthernetManager;
import android.net.pppoe.PppoeDevInfo;
import android.net.pppoe.PppoeManager;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

public class PPPoEReceiver extends BroadcastReceiver {
    private static final String TAG = "PPPoEReceiver";

    private Handler mHandler = null;
    private boolean mAutoDialFlag = false;
    private String mInterface = null;
    private String mUserName = null;
    private String mPassword = null;
    private PPPoEConfig mConfig = null;
    private static boolean mFirstAutoDialDone = false;
    Timer mandatory_dial_timer = null; 

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG , ">>>>>onReceive: " + intent.getAction());
        
        mConfig = new PPPoEConfig(context);
        mInterface = mConfig.getNetworkInterface(context);
        mAutoDialFlag = mConfig.getAutoDialFlag(context);
        mUserName = mConfig.getUserName(context);
        mPassword = mConfig.getPassword(context);
        
        if(PppoeManager.PPPOE_STATE_CHANGED_ACTION.equals(action)) {
            int event = intent.getIntExtra(PppoeManager.EXTRA_PPPOE_STATE,PppoeManager.PPPOE_STATE_UNKNOWN);
            Log.d(TAG, "#####event " + event);
            if(event == PppoeManager._EVENT_CONNECTED) {
                Toast.makeText(context, R.string.pppoe_connected, Toast.LENGTH_SHORT).show();
            }

            if(event == PppoeManager._EVENT_DISCONNECTED) {
            }

            if(event == PppoeManager._EVENT_CONNECT_FAILED) {
                String ppp_err = intent.getStringExtra(PppoeManager.EXTRA_PPPOE_ERRCODE);
                Log.d(TAG, "#####errcode: " + ppp_err);
            }
        }

        if ((null == mInterface) || !mAutoDialFlag
            || (null == mUserName) || (null == mPassword))
            return;

        PppoeDevInfo info = new PppoeDevInfo();
        info.setIfName(mInterface);
        info.setAccount(mUserName);
        info.setPassword(mPassword);
        info.setDialMode(PppoeDevInfo.PPPOE_DIAL_MODE_AUTO);
        if (mHandler == null) {
            mHandler = new PPPoEHandler(mConfig);
        }

        if (EthernetManager.ETH_STATE_CHANGED_ACTION.equals(action)) {
            mFirstAutoDialDone = true;
            if (!mInterface.startsWith("eth"))
                return;

            int event = intent.getIntExtra(EthernetManager.EXTRA_ETH_STATE, -1);
            if (event == EthernetStateTracker.EVENT_HW_PHYCONNECTED) {
                Log.d(TAG, "EVENT_HW_PHYCONNECTED");
                if (mandatory_dial_timer != null) {
                    mandatory_dial_timer.cancel();
                    mandatory_dial_timer = null;
                }
                
                Log.d(TAG, "EVENT_HW_PHYCONNECTED trigger AUTO DIAL");
                Message message = new Message();
                message.what = PPPoEHandler.MSG_MANDATORY_DIAL;
                message.arg1 = 5000;
                message.obj = info;
                //mHandler.sendMessage(message);
            }
            else {
                if (event == EthernetStateTracker.EVENT_HW_DISCONNECTED ) {
                    Log.d(TAG, "EVENT_HW_DISCONNECTED");
                }
                else if (event == EthernetStateTracker.EVENT_HW_CONNECTED )
                    Log.d(TAG, "EVENT_HW_CONNECTED");
                else
                    Log.d(TAG, "EVENT=" + event);

                if (event != EthernetStateTracker.EVENT_HW_DISCONNECTED&& !mFirstAutoDialDone) {
                    Log.d(TAG, "################################");
                    Log.d(TAG, "@@@EVENT_HW_PHYCONNECTED LOST@@@");
                    Log.d(TAG, "################################");
                    mFirstAutoDialDone = true;
                    mandatory_dial_timer = new Timer();   
                    TimerTask check_task = new TimerTask() {   
                        public void run() {
                            Log.d(TAG, "Send MSG_MANDATORY_DIAL");
                            PppoeDevInfo pinfo = new PppoeDevInfo();
                            pinfo.setIfName(mInterface);
                            pinfo.setAccount(mUserName);
                            pinfo.setPassword(mPassword);
                            pinfo.setDialMode(PppoeDevInfo.PPPOE_DIAL_MODE_AUTO);
                            Message message = new Message();
                            message.what = PPPoEHandler.MSG_MANDATORY_DIAL;
                            message.arg1 = 5000;
                            message.obj = pinfo;
                            //mHandler.sendMessage(message);
                        }
                    };

                    //Timeout after 5 seconds
                    mandatory_dial_timer.schedule(check_task, 6000);
                }
            }
        }
    }
}

