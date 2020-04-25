package com.android.settings.pppoe;

import android.net.pppoe.PppoeDevInfo;
import android.util.Log;
import android.os.Handler;
import android.os.Message;

public class PPPoEHandler extends Handler {
    private static final String TAG = "PPPoEHandler";

    public static final int MSG_START_DIAL = 0x1;
    public static final int MSG_MANDATORY_DIAL = 0x2;
    public static final int MSG_CONNECT_TIMEOUT = 0x3;
    public static final int MSG_DISCONNECT = 0x4;
    private String mInterfaceName = null;
    private PPPoEConfig mConfig = null;
    
    public PPPoEHandler(PPPoEConfig config) {
        mConfig = config;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        switch (msg.what) {
        case MSG_START_DIAL:
            Log.d(TAG, "handleMessage: MSG_START_DIAL");
            if((mConfig != null) && (msg.obj instanceof PppoeDevInfo)) {
                PppoeDevInfo info = (PppoeDevInfo)msg.obj;
                mConfig.connect(info);
            }
            break;

        case MSG_MANDATORY_DIAL:
            int timeout = msg.arg1;
			int Ift = msg.arg2;
            Log.d(TAG, "handleMessage: MSG_MANDATORY_DIAL timeout: " + timeout);
            if(mConfig != null) {
				if(Ift == 2)
                    mInterfaceName = "wlan0";
				else
                    mInterfaceName = "eth0";                    
				mConfig.disconnect(mInterfaceName);
            }
            
            Message message = new Message();
            message.what = MSG_START_DIAL;
            message.obj = msg.obj;
            this.removeMessages(MSG_START_DIAL);
            this.sendMessageDelayed(message, timeout);
            break;

        case MSG_DISCONNECT:
           Log.d(TAG, "handleMessage: MSG_DISCONNECT");
            if(mConfig != null) {
                mConfig.disconnect(mInterfaceName);
            }
            break;

        default:
            Log.d(TAG, "handleMessage: " + msg.what);
            break;
        }
    }
}

