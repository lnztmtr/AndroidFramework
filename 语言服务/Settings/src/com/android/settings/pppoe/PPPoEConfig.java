package com.android.settings.pppoe;

import android.content.Context;
import android.util.Log;
import android.net.ConnectivityManager;
import android.net.pppoe.PppoeDevInfo;
import android.net.pppoe.PppoeManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.ethernet.EthernetManager;
import android.content.SharedPreferences;
import android.os.SystemProperties;

public class PPPoEConfig {
    private static final String TAG = "PPPoEConfig";

    //pppoe state flag
    public static final String pppoe_running_flag = "net.pppoe.running";
    public static final String ethernet_dhcp_repeat_flag = "net.dhcp.repeat";

    private PppoeManager mPppoeManager;
    private EthernetManager mEthManager;
    private WifiManager mWifiManager;
    private Context mContext;

    public PPPoEConfig(Context context) {
        mContext = context;
        mPppoeManager = (PppoeManager)mContext.getSystemService(Context.PPPOE_SERVICE);
        mEthManager = (EthernetManager)mContext.getSystemService(Context.ETH_SERVICE);
        mWifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
    }

    public String getNetworkInterface(Context context) {
        if(mPppoeManager != null) {
            PppoeDevInfo info = mPppoeManager.getSavedPppoeConfig();
            return info.getIfName();
        }
        return null;
    }

    public boolean getAutoDialFlag(Context context) {
        if(mPppoeManager != null) {
            PppoeDevInfo info = mPppoeManager.getSavedPppoeConfig();
            return PppoeDevInfo.PPPOE_DIAL_MODE_AUTO.equals(info.getDialMode());
        }
        return false;
    }

    public String getUserName(Context context) {
        if(mPppoeManager != null) {
            PppoeDevInfo info = mPppoeManager.getSavedPppoeConfig();
            return info.getAccount();
        }
        return null;
    }

    public String getPassword(Context context) {
        if(mPppoeManager != null) {
            PppoeDevInfo info = mPppoeManager.getSavedPppoeConfig();
            return info.getPassword();
        }
        return null;
    }
	
	public String getWifiSsid(Context context) {
        if(mPppoeManager != null) {
            PppoeDevInfo info = mPppoeManager.getSavedPppoeConfig();
            return info.getWifissid();
        }
        return null;
    }

    public void disconnect(String ift) {
        Log.d(TAG, "connect2 start");
        if(ift == null)
            return;
        mPppoeManager.disconnect(ift);
        if(ift.equals("wlan0")) {
            SystemProperties.set("net.wifi.pppoe","disabled");
        }
        Log.d(TAG, "connect2 stop");
    }

    public void connect(PppoeDevInfo info) {
        PppoeDevInfo sinfo = mPppoeManager.getSavedPppoeConfig();
        String username = (sinfo != null) ? sinfo.getAccount() : null;
        String password = (sinfo != null) ? sinfo.getPassword() : null;
        String ifaceName = (sinfo != null) ? sinfo.getIfName() : null;
        String ssid = (sinfo != null) ? sinfo.getWifissid() : null;
        String dialmode = (sinfo != null) ? sinfo.getDialMode() : null;
        if(((username != null) && !username.equals(info.getAccount())) ||
                ((password != null) && !password.equals(info.getPassword())) ||
                ((ifaceName != null) && !ifaceName.equals(info.getIfName())) ||
                ((dialmode != null) && !dialmode.equals(info.getDialMode()))) {
            disconnect(ifaceName);
        }
        mPppoeManager.UpdatePppoeDevInfo(info);
        if("wlan0".equals(info.getIfName())) {
            if(mWifiManager != null) {
                if(isConnected(mContext,ConnectivityManager.TYPE_WIFI)){
                    Log.d(TAG,"Start real connect");
                    mPppoeManager.connect(info.getAccount(), info.getPassword(), "wlan0", info.getDialMode());
                }
            }
        } else{
            mPppoeManager.connect(info.getAccount(), info.getPassword(), info.getIfName(), info.getDialMode());
            Log.d(TAG, "connect end ");
        }
    }

    public NetworkInfo getNetworkInfo(Context context, int type) {
        ConnectivityManager connMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		    return ((connMgr != null) ? connMgr.getNetworkInfo(type) : null);
    }

    public boolean isConnected(Context context, int type) {
        NetworkInfo netinfo = getNetworkInfo(context, type);
        if(netinfo != null) {
            boolean isAvailable = netinfo.isAvailable();
            boolean isConnected = netinfo.isConnected();
            return (isAvailable && isConnected);
        }
        return false;
    }
    
    public boolean isPppOverEthernet() {
        return SystemProperties.get("net.pppoe.phyif", "unknown").startsWith("eth");
    }

    public boolean isPppOverWifi() {
        return SystemProperties.get("net.pppoe.phyif", "unknown").startsWith("wlan");
    }
}
