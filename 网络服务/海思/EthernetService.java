/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import android.net.DhcpInfo;
import android.net.RouteInfo;
import android.net.NetworkUtils;
import android.net.LinkProperties;
import android.net.LinkAddress;
import android.net.NetworkInfo;
import android.net.ethernet.EthernetNative;
import android.net.ethernet.IEthernetManager;
import android.net.ethernet.EthernetManager;
import android.net.ethernet.EthernetManager;
import android.net.EthernetDataTracker;
import android.net.wifi.WifiManager;
import android.net.ConnectivityManager;
import java.net.UnknownHostException;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.http.util.EncodingUtils;
// only for fast boot Pro
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.INetworkManagementService;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import com.hisilicon.android.hisysmanager.HiSysManager;

import android.content.ContentValues;
import android.net.Uri;
import android.text.TextUtils;
import android.database.Cursor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import android.util.Log;
import java.util.HashMap;
import java.util.Map;
import java.io.InputStreamReader;

public class EthernetService extends IEthernetManager.Stub{
    private Context mContext;
    private EthernetDataTracker mTracker;
    private String mIface;
    private static final String TAG = "EthernetService";
    private static final int AID_ADB = 1011;
    private static final int AID_SHELL = 2000;
    private int isEthernetEnabled;
    private int mEthernetState;
    private boolean DEBUG = false;
    private HiSysManager mhisys;
    public EthernetService(Context context){
        mContext = context;
		//log日志过大
		if (SystemProperties.get("ro.ysten.province","master").contains("fujian")){
		    DEBUG = false;
		}
        mhisys = new HiSysManager();
        mIface = getDatabaseInterfaceName();
        mEthernetState = EthernetManager.ETHERNET_STATE_DISABLED;
        isEthernetEnabled = getEthernetPersistedState();
        if(isEthernetEnabled == EthernetManager.ETHERNET_STATE_ENABLED)
        {
            NetworkUtils.enableInterface(mIface);
            if("true".equals(SystemProperties.get("persist.ethernet.wifidisguise", "false")))
                setWifiDisguise(true);
            else
                setWifiDisguise(false);
        }
        if(SystemProperties.get("persist.sys.qb.enable","false").equals("true"))
        {
        //for Qb
        Log.i(TAG, " In EthernetDATATracker Qb ");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_QB_PROP_RELOAD);

        // Broadcast receiver for fast booting
        BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.i(TAG, " Qb onReceiver() Called ");
                    String action = intent.getAction();
                    if (action.equals(Intent.ACTION_QB_PROP_RELOAD)) {
                        Log.i(TAG, "-------------> Qb Broadcast Received <-----------------");
                        Log.i(TAG, "onReceiver(): intent= " + intent);
                        Log.i(TAG, "Qb Ethernet disable and enable");
                        Log.i(TAG, "ethernet_state get from old Secure DataBase  -----> " + getEthernetPersistedState());

                        try
                        {
                            mhisys.getFlashInfo("deviceinfo",0,17);
                            String mac = readFile("/mnt/mtdinfo");
                            Log.i(TAG,"getFlashInfo:"+mac);

                            IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
                            INetworkManagementService mNwService = INetworkManagementService.Stub.asInterface(b);
                            String oldmac = mNwService.getInterfaceConfig("eth0").getHardwareAddress().toString();
                            Log.i(TAG,"get oldmac:"+oldmac);
                            if(mac != null && oldmac != null && !mac.toLowerCase().equals(oldmac))
                            {
                                Log.i(TAG,"Qb Ethernet mac address change.");
                                mhisys.reloadMAC(mac);
                            }
                            else
                            {
                                Log.i(TAG,"Qb Ethernet mac address not change.");
                            }
                        }
                        catch(Exception ex)
                        {
                            Log.e(TAG,"Qb Ethernet reset Mac error!"+ex);
                        }
                    }
                }
            };
            mContext.registerReceiver(receiver, intentFilter);
        }
    }

    private void registerEthernetObserver() {
        mTracker.registerEthernetObserver();
    }

    private void unregisterEthernetObserver() {
        mTracker.unregisterEthernetObserver();
    }

    private boolean checkPermission()
    {
        int callingUid = Binder.getCallingUid();
        if (callingUid != AID_ADB
            && callingUid != AID_SHELL)
        {
            Log.d(TAG, "checkPermission is true");
            return true;
        }

        Log.d(TAG, "checkPermission is false");
        return false;
    }


    private synchronized void persistEthernetEnabled(boolean enabled) {
        if (!checkPermission())
        {
            return;
        }
        if(DEBUG) Log.d(TAG, "persistEthernetEnabled(boolean enabled) enabled:" + enabled);
        final ContentResolver cr = mContext.getContentResolver();
        Settings.Secure.putInt(cr, Settings.Secure.ETHERNET_ON,
                enabled ? EthernetManager.ETHERNET_STATE_ENABLED : EthernetManager.ETHERNET_STATE_DISABLED);
    }
    //add for chinamobile
    private synchronized void persistIpv4Enabled(boolean enabled) {
            if (!checkPermission())
            {
                return;
            }
            if(DEBUG) Log.i(TAG, "persistIpv4nabled(boolean enabled) enabled:" + enabled);
            final ContentResolver cr = mContext.getContentResolver();
            Settings.Secure.putInt(cr, Settings.Secure.IPV4_ON,
                    enabled ? EthernetManager.IPV4_STATE_ENABLED : EthernetManager.IPV4_STATE_DISABLED);
        }

    private synchronized void persistIpv6Enabled(boolean enabled) {
        if (!checkPermission())
        {
            return;
        }
        if(DEBUG) Log.i(TAG, "persistIpv6nabled(boolean enabled) enabled:" + enabled);
        final ContentResolver cr = mContext.getContentResolver();
        Settings.Secure.putInt(cr, Settings.Secure.IPV6_ON,
                enabled ? EthernetManager.IPV6_STATE_ENABLED : EthernetManager.IPV6_STATE_DISABLED);
    }

    public void checkAndStartEthernet() {
        if (!checkPermission())
        {
            return;
        }
        mTracker = EthernetDataTracker.getInstance();
        //add by ysten chenfeng at 20190304: check and set ipoe 
		//add by huxiang for hainan if last version v4 is pppoe pppoe v6 set defalut
		Log.i(TAG, "pppoe v6 set defalut start");
		if (SystemProperties.get("ro.ysten.sub.province","master").equals("cm201_hainan_jidi")){
			Log.i(TAG, "pppoe v6 set defalut hainan");
			EthernetManager mEthernetManager = (EthernetManager) mContext.getSystemService(Context.ETHERNET_SERVICE);
			if(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mEthernetManager.getEthernetMode())){
					//if last version v4 is pppoe,then v6 must be pppoe 
					Log.i(TAG, "pppoe v6 set defalut");
					final ContentResolver cr = mContext.getContentResolver();
                    Settings.Secure.putString(cr, Settings.Secure.ETHERNET_IPV6_MODE,"pppoe");
			}
		}
		//add end
        checkAndSetDhcpOption60();       
        /*begin:add by zhanghk at 20190301:default close 125 check except jiangxi*/
        if (SystemProperties.get("ro.ysten.province","master").contains("jiangxi")){
            if (SystemProperties.get("persist.ethernet.vlan.enabled").equals("true")) {
                if (getDhcpOption125State() != EthernetManager.OPTION125_STATE_ENABLED)
                    setDhcpOption125(true, "HGW-CT");
            }
        }
        //begin by zhangjunjian at 20190812 open 125 check for guizhou		
        else if (SystemProperties.get("ro.ysten.province","master").contains("guizhou")){
            if (SystemProperties.get("persist.ethernet.vlan.enabled").equals("true")) {
                if (getDhcpOption125State() != EthernetManager.OPTION125_STATE_ENABLED)
                    setDhcpOption125(true, "gzydiptvdhcpserver");
            }
        }
		//end by zhangjunjian at 20190812 open 125 check for guizhou
		else{
            setDhcpOption125(false, "");
        }
        /*end:add by zhanghk at 20190301:default close 125 check except jiangxi*/

        setEthernetState(isEthernetEnabled);
        if(isEthernetEnabled == EthernetManager.ETHERNET_STATE_ENABLED) {
            registerEthernetObserver();
            if("true".equals(SystemProperties.get("persist.ethernet.wifidisguise", "false")))
                setWifiDisguise(true);
            else
                setWifiDisguise(false);
        }
        if(EthernetManager.IPV6_STATE_DISABLED == getIpv6PersistedState()) {
            enableIpv6(false);
        } else {
            enableIpv6(true);
        }
    }

    public boolean isEthernetConfigured() {
        final ContentResolver cr = mContext.getContentResolver();
        String mode = getEthernetMode();
        if(null == mode) {
            return false;
        }

        if (!EthernetManager.ETHERNET_CONNECT_MODE_DHCP.equals(mode)
            && !EthernetManager.ETHERNET_CONNECT_MODE_MANUAL.equals(mode)
            && !EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mode)
            && !EthernetManager.ETHERNET_CONNECT_MODE_NONE.equals(mode)) {
            Log.d(TAG, "Database mode error: " + mode);
            return false;
        } else {
            if(DEBUG) Log.d(TAG, "isEthernetConfigured() mode: " + mode);
            return true;
        }
    }

    public DhcpInfo getDhcpInfo() {
        return mTracker.getDhcpInfo();
    }

    public synchronized void setEthernetMode(String mode, DhcpInfo dhcpInfo) {
        if (!checkPermission())
        {
            return;
        }

        if (EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mode) &&
                ("aosp".equals(SystemProperties.get("ro.product.target", "aosp")))) {
            Log.e(TAG, "pppoe not enabled");
            return;
        }

        final ContentResolver cr = mContext.getContentResolver();
        if(mIface != null) {
            if(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL.equals(mode) && dhcpInfo == null) {
                Log.i(TAG, "No input DhcpInfo, use database ip!");
                dhcpInfo = getSavedEthernetIpInfo();
            }
            Settings.Secure.putString(cr, Settings.Secure.ETHERNET_IFNAME, mIface);
            Settings.Secure.putString(cr, Settings.Secure.ETHERNET_MODE, mode);

            if(DEBUG) Log.d(TAG, "setEthernetMode mode:" + mode + " DhcpInfo: " + dhcpInfo);
            if(dhcpInfo != null && EthernetManager.ETHERNET_CONNECT_MODE_MANUAL.equals(mode)) {
                String ipAddress = NetworkUtils.intToInetAddress(dhcpInfo.ipAddress).getHostAddress();
                String route = NetworkUtils.intToInetAddress(dhcpInfo.gateway).getHostAddress();
                String dns1  = NetworkUtils.intToInetAddress(dhcpInfo.dns1).getHostAddress();
                String dns2  = NetworkUtils.intToInetAddress(dhcpInfo.dns2).getHostAddress();
                int prefixLength = NetworkUtils.netmaskIntToPrefixLength(dhcpInfo.netmask);
                if(DEBUG) Log.d(TAG, "Settings.Secure.putString:ipAddress "  +ipAddress+" prefixLength "+prefixLength+" route "+route+" dns1 "+dns1+" dns2 "+dns1);
                Settings.Secure.putString(cr, Settings.Secure.ETHERNET_IP, ipAddress);
                Settings.Secure.putInt(cr, Settings.Secure.ETHERNET_PREFIXLENGTH,prefixLength);
                Settings.Secure.putString(cr, Settings.Secure.ETHERNET_ROUTE, route);
                Settings.Secure.putString(cr, Settings.Secure.ETHERNET_DNS_1, dns1);
                Settings.Secure.putString(cr, Settings.Secure.ETHERNET_DNS_2, dns2);
            }
            if(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mode)) {
                Log.i(TAG, "Ethernet Use Pppoe, please also setPppoeMode!");
            }
        } else {
            Log.e(TAG, "ERROR! No Interface name set, please setInterfaceName first!");
        }
    }

    public String getEthernetMode() {
        final ContentResolver cr = mContext.getContentResolver();
        try {
            return Settings.Secure.getString(cr, Settings.Secure.ETHERNET_MODE);
        } catch (Exception e) {
            Log.e(TAG, "getEthernetMode Error!");
            return null;
        }
    }

    public String getDatabaseInterfaceName() {
        final ContentResolver cr = mContext.getContentResolver();
        try {
            if(DEBUG) Log.d(TAG, "getDatabaseInterfaceName() InterfaceName: "
                    + Settings.Secure.getString(cr, Settings.Secure.ETHERNET_IFNAME));
            return Settings.Secure.getString(cr, Settings.Secure.ETHERNET_IFNAME);
        } catch (Exception e) {
            Log.e(TAG, "getDatabaseInterfaceName() read Database ERROR!");
            return "eth0";
        }
    }

    public boolean setInterfaceName(String iface) {
        if (!checkPermission())
        {
            return false;
        }
        if(mIface.equals(iface)) {
            return true;
        }

        if(!mTracker.checkInterfaceName(iface)) {
            Log.e(TAG, "Ethernet Interface Name:" + iface + " ERROR! Should be ethX!");
            return false;
        }

        int count = 0;
        count = getTotalInterface();
        if(count <= 0) {
            Log.e(TAG, "ERROR! No Ethernet Interface found!");
            return false;
        }
        String[] mInterfaces = new String[count];
        mInterfaces = getDeviceNameList();
        mIface = null;
        for(int i = 0; i < count; i++) {
            if(iface.matches(mInterfaces[i])) {
                mIface = iface;
                final ContentResolver cr = mContext.getContentResolver();
                Settings.Secure.putString(cr, Settings.Secure.ETHERNET_IFNAME, mIface);
                mTracker.setInterfaceFromDatabase();
                break;
            }
        }
        if(mIface == null) {
            Log.e(TAG, "ERROR! Can not found available interface:" + iface);
            mIface = getDatabaseInterfaceName();
            return false;
        } else {
            if(DEBUG) Log.d(TAG, "setInterfaceName: " + mIface + " Success!");
            return true;
        }
    }

    private static final int ETHERNET_PPPOE_CONNECT = 1;
    Handler mPppoeConnectHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            if (ETHERNET_PPPOE_CONNECT == what) {
                Log.d(TAG, "state == EthernetManager.ETHERNET_STATE_ENABLED");
                mTracker.resetInterface();
            }
        }
    };

    public synchronized void setEthernetState(int state) {
        if (!checkPermission())
        {
            return;
        }

        String product = SystemProperties.get("ro.product.target", "ott");
        Log.i(TAG, "setEthernetState from " + mEthernetState + " to "+ state);

            mEthernetState = state;

            if (!isEthernetConfigured()) {
                Log.i(TAG, "EthernetConfigured no config, USE DHCP");
                // If user did not configure any interfaces yet, pick the first one
                // and enable it.
                setEthernetMode(EthernetManager.ETHERNET_CONNECT_MODE_DHCP, null);
            }

            if (state == EthernetManager.ETHERNET_STATE_DISABLED) {
                if(DEBUG) Log.d(TAG, "state == EthernetManager.ETHERNET_STATE_DISABLED");
                if ("shcmcc".equals(product))
                    mPppoeConnectHandler.removeMessages(ETHERNET_PPPOE_CONNECT);
                mTracker.stopInterface();
            } else {
                if(DEBUG) Log.d(TAG, "state == EthernetManager.ETHERNET_STATE_ENABLED");
                if ("shcmcc".equals(product)) {
                    String mode = getEthernetMode();
                    if(mode != null && mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE)) {
                        Message msg = new Message();
                        msg.what = ETHERNET_PPPOE_CONNECT;
                        mPppoeConnectHandler.removeMessages(ETHERNET_PPPOE_CONNECT);
                        mPppoeConnectHandler.sendMessageDelayed(msg, 300);
                    } else {
                        mTracker.resetInterface();
                    }
                } else {
                    mTracker.resetInterface();
                }
            }
    }

    public int getEthernetState( ) {
        if(DEBUG) Log.d(TAG,"get EthernetState: " + mEthernetState);
        if(EthernetManager.ETHERNET_STATE_ENABLED == getEthernetPersistedState()) {
            return mEthernetState;
        } else {
            Log.i(TAG, "getEthernetState Ethernet not Enabled!");
            return EthernetManager.ETHERNET_STATE_DISABLED;
        }
    }

    public void setWifiDisguise(boolean setEnable) {
        if (!checkPermission())
        {
            return;
        }

        if("false".equals(SystemProperties.get("persist.ethernet.wifidisguise", "false")))
            setEnable = false;

        final ContentResolver cr = mContext.getContentResolver();
        Settings.Secure.putInt(cr, Settings.Secure.WIFI_DISGUISE,
                setEnable ? EthernetManager.WIFI_DISGUISE_ENABLED : EthernetManager.WIFI_DISGUISE_DISABLED);
    }

    public int getWifiDisguiseState() {
        final ContentResolver cr = mContext.getContentResolver();
        try {
            if(DEBUG) Log.d(TAG, "getWifiDisguiseState: "
                    + Settings.Secure.getInt(cr, Settings.Secure.WIFI_DISGUISE));
            return Settings.Secure.getInt(cr, Settings.Secure.WIFI_DISGUISE);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "getWifiDisguiseState: STATE UNKNOWN");
            return EthernetManager.WIFI_DISGUISE_STATE_UNKNOWN;
        }
    }

    public int getNetLinkStatus(String ifaceName) {
        if(ifaceName == null) {
            Log.e(TAG,"The given name "+ifaceName+" is invalid, return -1");
            return -1;
        }
        if(DEBUG) Log.d(TAG, "getNetLinkStatus(" + ifaceName + "): " + NetworkUtils.getNetlinkStatus(ifaceName));

        return NetworkUtils.getNetlinkStatus(ifaceName);
    }

    public void setAutoReconnectState(boolean AutoReconnect) {
        if (!checkPermission())
        {
            return;
        }
        if(DEBUG) Log.d(TAG, "setAutoReconnectState(" + AutoReconnect + ")");
        final ContentResolver cr = mContext.getContentResolver();
        Settings.Secure.putInt(cr, Settings.Secure.ETHERNET_AUTORECONNECT,
            AutoReconnect? EthernetManager.ETHERNET_AUTORECONNECT_ENABLED : EthernetManager.ETHERNET_AUTORECONNECT_DISABLED);
    }

    public int getAutoReconnectState() {
        final ContentResolver cr = mContext.getContentResolver();
        try {
            if(DEBUG) Log.d(TAG, "getAutoReconnectState(): "
                    + Settings.Secure.getInt(cr, Settings.Secure.ETHERNET_AUTORECONNECT));
            return Settings.Secure.getInt(cr, Settings.Secure.ETHERNET_AUTORECONNECT);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "getAutoReconnectState() ETHERNET_AUTORECONNECT_STATE_UNKNOWN");
            return EthernetManager.ETHERNET_AUTORECONNECT_STATE_UNKNOWN;
        }
    }

    public void setIpv6MOAutoState(boolean MOValid) {
        if (!checkPermission())
        {
            return;
        }
        if(DEBUG) Log.d(TAG, "setIpv6MOAutoState(" + MOValid + ")");
        final ContentResolver cr = mContext.getContentResolver();
        Settings.Secure.putInt(cr, Settings.Secure.ETHERNET_IPV6_MO_VALID,
            MOValid ? EthernetManager.ETHERNET_IPV6MOAUTO_ENABLED : EthernetManager.ETHERNET_IPV6MOAUTO_DISABLED);
    }

    public int getIpv6MOAutoState() {
        final ContentResolver cr = mContext.getContentResolver();
        try {
            if(DEBUG) Log.d(TAG, "getIpv6MOAutoState(): "
                    + Settings.Secure.getInt(cr, Settings.Secure.ETHERNET_IPV6_MO_VALID));
            return Settings.Secure.getInt(cr, Settings.Secure.ETHERNET_IPV6_MO_VALID);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "getIpv6MOAutoState() ETHERNET_IPV6MOATUO_STATE_UNKNOWN");
            return EthernetManager.ETHERNET_IPV6MOATUO_STATE_UNKNOWN;
        }
    }

    public synchronized DhcpInfo getSavedEthernetIpInfo() {
    if (isEthernetConfigured()) {
        final ContentResolver cr = mContext.getContentResolver();
            String ipAddress = null;
            String dns1 = null;
            String dns2 = null;
            int prefixLength = 0;
            String getwayAddress = null;

            try{
                //read information from database
                ipAddress = Settings.Secure.getString(cr, Settings.Secure.ETHERNET_IP);
                dns1 = Settings.Secure.getString(cr, Settings.Secure.ETHERNET_DNS_1);
                dns2 = Settings.Secure.getString(cr, Settings.Secure.ETHERNET_DNS_2);
                prefixLength = Settings.Secure.getInt(cr, Settings.Secure.ETHERNET_PREFIXLENGTH);
                getwayAddress = Settings.Secure.getString(cr, Settings.Secure.ETHERNET_ROUTE);
            } catch (Exception e) {
                Log.e(TAG, "getSavedEthernetIpInfo(): Can not read Database! ERROR!");
                return null;
            }

            InetAddress ipaddr = NetworkUtils.numericToInetAddress(ipAddress);
            InetAddress getwayaddr = NetworkUtils.numericToInetAddress(getwayAddress);
            InetAddress idns1 = NetworkUtils.numericToInetAddress(dns1);
            InetAddress idns2 = NetworkUtils.numericToInetAddress(dns2);

            DhcpInfo dhcpInfo = new DhcpInfo();
            dhcpInfo.ipAddress = NetworkUtils.inetAddressToInt((Inet4Address)ipaddr);
            dhcpInfo.gateway = NetworkUtils.inetAddressToInt((Inet4Address)getwayaddr);
            dhcpInfo.netmask = NetworkUtils.prefixLengthToNetmaskInt(prefixLength);
            dhcpInfo.dns1 = NetworkUtils.inetAddressToInt((Inet4Address)idns1);
            dhcpInfo.dns2 = NetworkUtils.inetAddressToInt((Inet4Address)idns2);

            if(DEBUG) Log.i(TAG, "getSavedEthernetIpInfo() dhcpInfo = " + dhcpInfo);
            return dhcpInfo;
        }
        Log.e(TAG, "getSavedEthernetIpInfo() Ethernet is not configed, nothing in database!");
        return null;
    }

    public String[] getDeviceNameList() {
        if (getTotalInterface() > 0 ) {
            if(DEBUG) Log.d(TAG, "getDeviceNameList() ok");
            return mTracker.getEthernetInterfaceNameList();
        }
        else {
            Log.e(TAG, "getDeviceNameList() error");
            return null;
        }
    }

    public int getEthernetPersistedState() {

        final ContentResolver cr = mContext.getContentResolver();
        try {
            if(DEBUG) Log.d(TAG, "getEthernetPersistedState() ETHERNET_ON:"
                    + Settings.Secure.getInt(cr, Settings.Secure.ETHERNET_ON));
            return Settings.Secure.getInt(cr, Settings.Secure.ETHERNET_ON);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "getEthernetPersistedState() ETHERNET_STATE_UNKNOWN");
            return EthernetManager.ETHERNET_STATE_UNKNOWN;
        }
    }
    //add for chinamobile
    public int getIpv4PersistedState() {

        final ContentResolver cr = mContext.getContentResolver();
        try {
            if(DEBUG) Log.d(TAG, "getIpv4PersistedState() IPV4_ON:"
                    + Settings.Secure.getInt(cr, Settings.Secure.IPV4_ON));
            return Settings.Secure.getInt(cr, Settings.Secure.IPV4_ON);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "getIpv4PersistedState() IPV4_STATE_UNKNOWN");
            return EthernetManager.IPV4_STATE_UNKNOWN;
        }
    }

    public int getTotalInterface() {
        if(DEBUG) Log.d(TAG, "getTotalInterface(), count: " + EthernetNative.getInterfaceCnt());
        return EthernetNative.getInterfaceCnt();
    }

    public void enableEthernet(boolean setEnable) {
        if (!checkPermission())
        {
            return;
        }

        if(DEBUG) Log.d(TAG, "enableEthernet(" + setEnable + ")");
        if(mIface == null) {
            Log.i(TAG, "mIface = null, get from database!");
            mIface = getDatabaseInterfaceName();
        }
        persistEthernetEnabled(setEnable);
        if(setEnable) {
            NetworkUtils.enableInterface(mIface);
            setEthernetState(EthernetManager.ETHERNET_STATE_ENABLED);
            SystemProperties.set("persist.sys.ethernet.status", mIface + "_on");
            registerEthernetObserver();
        } else {
            unregisterEthernetObserver();
            setEthernetState(EthernetManager.ETHERNET_STATE_DISABLED);
            SystemProperties.set("persist.sys.ethernet.status", mIface + "_down");
            NetworkUtils.disableInterface(mIface);
        }
   }
    public void QbdisableEthernet(){
        if (!checkPermission())
        {
            return;
        }

        Log.i(TAG,"QbdisableEthernet");
        mTracker.Qbteardown();
        mEthernetState = 0;
    }
	
    private String queryDBValue(Context context, String key){
		    Log.i(TAG,"queryDBValue " + key);
		    String value = "";
		    Uri uri = Uri.parse("content://stbconfig/summary");
		    try{
				    Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
				    if(cursor != null){
					  	    if(cursor.moveToFirst()){
								    int index = 0;
								    if(TextUtils.equals(key, "IPOEID")){
										    index = cursor.getColumnIndex("IPOEID");
								    } else if(TextUtils.equals(key, "IPOEPassword")){
										    index = cursor.getColumnIndex("IPOEPassword");
								    } else if(TextUtils.equals(key, "IPOEIsDefault")){
										    index = cursor.getColumnIndex("IPOEIsDefault");
								    }
								    value = cursor.getString(index);
								    cursor.close();
						    }
				    }
		    } catch(Exception e){
				    Log.d(TAG, "queryDBValue Execption "+e.toString());
		    }
		    return value;
    }

        private static String queryYnDBAuth(Context context, String key){
                Log.i(TAG,"queryDBValue " + key);
                String value = "";
                Uri uri = Uri.parse("content://stbconfig/summary");
                try{
                        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
                        if(cursor != null){
                                while(cursor.moveToNext()){
                                        String name = cursor.getString(cursor.getColumnIndex("name"));
                                        if (key.equals(name)) {
                                                value = cursor.getString(cursor.getColumnIndex("value"));
                                                break;
                                        }
                                }
                                cursor.close();
                        }
                } catch(Exception e){
                        Log.d(TAG, "queryDBValue Execption "+e.toString());
                }
                return value;

        }


	    private String queryDBValueAH(Context context, String key){
		    Log.i(TAG,"queryDBValue " + key);
		    String value = "";
		    Uri uri = Uri.parse("content://stbconfig/authentication");
		    try{
				    Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
				    if(cursor != null){
					  	    if(cursor.moveToFirst()){
								    int index = 0;
								    if(TextUtils.equals(key, "IPOEID")){
										    index = cursor.getColumnIndex("IPOEID");
								    } else if(TextUtils.equals(key, "IPOEPassword")){
										    index = cursor.getColumnIndex("IPOEPassword");
								    } else if(TextUtils.equals(key, "IPOEIsDefault")){
										    index = cursor.getColumnIndex("IPOEIsDefault");
								    }
								    value = cursor.getString(index);
								    cursor.close();
						    }
				    }
		    } catch(Exception e){
				    Log.d(TAG, "queryDBValue Execption "+e.toString());
		    }
		    return value;
    }
	
	private static String queryDBAuthValue(Context context, String key){
		Log.i(TAG,"queryDBValue " + key);
		String value = "";
		Uri uri = Uri.parse("content://stbconfig/authentication");
		try{
			Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
			if(cursor != null){
				while(cursor.moveToNext()){
					String name = cursor.getString(cursor.getColumnIndex("name"));
					if (key.equals(name)) {
						value = cursor.getString(cursor.getColumnIndex("value"));
						break;
					}
				}
				cursor.close();
			}
		} catch(Exception e){
			Log.d(TAG, "queryDBValue Execption "+e.toString());
		}
		return value;

	}

	public static String getUserID(Context context)
	{
		String uname = queryDBAuthValue(context,"username");
		if(!TextUtils.isEmpty(uname)){
 		   Log.d(TAG,"getUserID "+uname);
		   return uname;
	    }else{
			return "";
		}	
	}
	
	public static String getUserPassword(Context context)
	{
		String pwd = queryDBAuthValue(context,"Password");
		if(!TextUtils.isEmpty(pwd)){
	          Log.d(TAG,"getUserPassword "+pwd);
                  return pwd;
	    }else{
			return "";
		}
	}


    private int saveDBValue(Context context, String key, String value){
		    int count = 0;
		    try {
			 	    Uri uri = Uri.parse("content://stbconfig/summary");
				    ContentValues values = new ContentValues();
				    values.put(key, value);
                    count = context.getContentResolver().update(uri, values, null, null);
		    } catch(Exception e){
				    Log.d(TAG, "saveDBValue Execption "+e.toString());
		    }
		    Log.d(TAG, "saveDBValue count "+count);
		    return count;
    }
	
    private int saveDBValueYn(Context context, String key, String value){
		    int count = 0;
		    try {
			 	    Uri uri = Uri.parse("content://stbconfig/summary");
				    ContentValues values = new ContentValues();
				    values.put("value", value);
                    count = context.getContentResolver().update(uri, values, "name='"+key+"'", null);
		    } catch(Exception e){
				    Log.d(TAG, "saveDBValueYn Execption "+e.toString());
		    }
		    Log.d(TAG, "saveDBValueYn count "+count);
		    return count;
    }
	

    public int getDhcpOption60State() {
        final ContentResolver cr = mContext.getContentResolver();
        int option60State = 0;
        try {
            option60State = Settings.Secure.getInt(cr, Settings.Secure.OPTION60_ON);
            if(DEBUG) Log.d(TAG, "getDhcpOption60State: " + option60State);
            return option60State;
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "getDhcpOption60State: STATE UNKNOWN");
            return EthernetManager.OPTION60_STATE_UNKNOWN;
        }
    }

    public String getDhcpOption60Login() {
	    String login = "";
		//add by wangrongke for yunnan ipoe
		 String  login_mac = null;
		      if (SystemProperties.get("ro.ysten.province","master").contains("yunnan")){
               login = queryYnDBAuth(mContext, "username");
               login_mac = readMac();
               Log.d(TAG,"yunnan login:"+login);
               if(TextUtils.isEmpty(login)){
                      return login_mac;
               }else if(!login.equals(login_mac)){
                      return login;
                 }else{
                      return login_mac;
                 }
           }
		//end by wangrongke for  yunnan ipoe
            final ContentResolver cr = mContext.getContentResolver();//add by ysten xumiao at 20191118: modify CM201-2_liaoning to liaoning & save username pws to db
	    if (SystemProperties.get("ro.ysten.province","master").contains("sichuan")){
	        login = queryDBValue(mContext, "IPOEID");
        if(TextUtils.isEmpty(login)){
                login = "";
        }else{
            return login;
		  }
	    }
		//add by zhangy for anhui ipoe @20191213
		if (SystemProperties.get("ro.ysten.province","master").equals("cm201_anhui_iptv")){
			Log.d(TAG, "getDhcpOption60Login : zhangy 20191213 come0 :" + login);
	        login = queryDBValueAH(mContext, "IPOEID");
            if(TextUtils.isEmpty(login)){
                login = "";
            }else{
                return login;
		    }
			Log.d(TAG, "getDhcpOption60Login : zhangy 20191213 come1 :" + login);
	    }
		//end by zhangy for anhui ipoe @20191213
        if (SystemProperties.get("ro.ysten.province","master").contains("jilin")){
	        login = queryDBAuthValue(mContext, "username");
            if(TextUtils.isEmpty(login)){
               login = "jliptv";
		    }
			return login;
	    }
        //begin:add by xumiao at 2019/12/16 get huawei usename set ipoe username
        if (SystemProperties.get("ro.ysten.province","master").contains("neimeng")){
                login = queryDBAuthValue(mContext, "username");
                if(TextUtils.isEmpty(login)){
                     login = "nmydiptv";
                }
                return login;
            }
        //end:add by xumiao at 2019/12/16 get huawei usename set ipoe username
        if (SystemProperties.get("ro.ysten.province","master").contains("cm201_guangdong")){
             login = queryDBValue(mContext, "IPOEID");
             if(TextUtils.isEmpty(login)){
                login = "";
             }else{
                return login;
		     }
        }
		//begin by zhangjunjian at 20190812 for ipoe account
        if (SystemProperties.get("ro.ysten.province","master").contains("guizhou")){
             login = queryDBValue(mContext, "IPOEID");
             if(TextUtils.isEmpty(login)){
                login = "";
             }else{
                return login;
		    }
        }
		//end by zhangjunjian at 20190812 for ipoe account
		if (SystemProperties.get("ro.ysten.province","master").contains("CM201-2_shanxi_iptv")
			||SystemProperties.get("ro.ysten.province","master").contains("cm201_beijing")){
			String temp[] = null;
	        if(readLoginInfo() != null){
				temp = readLoginInfo().split("\n");
			}
			if(temp != null && temp.length == 2){
				login = temp[0].trim();
			}
        if(TextUtils.isEmpty(login)){
                login = "";
				Log.e(TAG, "getDhcpOption60Login : null");
        }else{
			    Log.e(TAG, "getDhcpOption60Login : " + login);
                return login;
		   }
	    }
	
        // begin: add by tianchi at 20181122
        if(SystemProperties.get("ro.ysten.province","master").contains("shanxi")){
            String temp[] = null;
            if(readLoginInfo() != null){
                temp = readLoginInfo().split("\n");
            }
            if(temp != null && temp.length == 2){
                login = temp[0].trim();
            }
            if(TextUtils.isEmpty(login)){
                login = "";
                Log.e(TAG, "getDhcpOption60Login : null");
            }else{
                Log.e(TAG, "getDhcpOption60Login : " + login);
                return login;
		    }
        }
        //end: add by tianchi

          if (SystemProperties.get("ro.ysten.province","master").contains("liaoning")){//begin: add by ysten xumiao at 20191118: modify CM201-2_liaoning to liaoning & save username pws to db
             login = getUserID(mContext);
             Settings.Secure.putString(cr, Settings.Secure.DHCP_USER,login);//begin: add by ysten xumiao at 20191118: modify CM201-2_liaoning to liaoning & save username pws to db
             if(TextUtils.isEmpty(login)){
                 login = "";
		         Log.e(TAG, "getDhcpOption60Login : null");
             }else{
			     Log.e(TAG, "getDhcpOption60Login : " + login);
                 return login;
                }
	        }
       
        try {
            login = Settings.Secure.getString(cr, Settings.Secure.OPTION60_LOGIN);
            if(TextUtils.isEmpty(login))
                login = "";
			//start by lizheng 20190514 to solve fujian @tv
			if (SystemProperties.get("ro.ysten.province","master").contains("fujian")){
				if (!login.endsWith("@tv"))
				{
					login=login+"@tv";
				}
			}
			//end by lizheng 20190514 to solve fujian @tv
            if(DEBUG) Log.d(TAG, "getDhcpOption60Login: " + login);
            return login;
        } catch (Exception e) {
            Log.e(TAG, "getDhcpOption60Login Error!");
            return null;
        }
    }

    public String getDhcpOption60Password() {
	    String passwd = null;
		//add by wangrongke for  yunnan ipoe
		 String password_begin = null;
		 if (SystemProperties.get("ro.ysten.province","master").contains("yunnan")){
            passwd = queryYnDBAuth(mContext, "Password");
            password_begin = "yncmcc138@itv";
            Log.d(TAG,"yunnan, passwd:"+passwd);
            if(TextUtils.isEmpty(passwd)){
            return password_begin;
        }else if (!passwd.equals(password_begin)){       
                return passwd;
               }else{
              return password_begin;
               }
		}
		//end by wangrongke  for yunnan ipoe
            final ContentResolver cr = mContext.getContentResolver();//add by ysten xumiao at 20191118: modify CM201-2_liaoning to liaoning & save username pws to db
	    if (SystemProperties.get("ro.ysten.province","master").contains("sichuan")){
		passwd = queryDBValue(mContext, "IPOEPassword");
        if(TextUtils.isEmpty(passwd)){
            passwd = "";
        }else{
             return passwd;
		}
		}
		//add by zhangy for anhui ipoe @20191213
		if (SystemProperties.get("ro.ysten.province","master").equals("cm201_anhui_iptv")){
			Log.d(TAG, "getDhcpOption60Password : zhangy 20191213 come :" + passwd);
		    passwd = queryDBValue(mContext, "IPOEPassword");
            if(TextUtils.isEmpty(passwd)){
               passwd = "";
            }else{
               return passwd;
		    }
			Log.d(TAG, "getDhcpOption60Password : zhangy 20191213 come1 :" + passwd);
		}
		//end by zhangy for anhui ipoe @20191213
        if (SystemProperties.get("ro.ysten.province","master").contains("jilin")){
            passwd = queryDBAuthValue(mContext, "password");
            if(TextUtils.isEmpty(passwd)){
			   passwd = "123321";
			}
            return passwd;
		}
        //begin:add by xumiao at 2019/12/16 get huawei usename set ipoe password
        if (SystemProperties.get("ro.ysten.province","master").contains("neimeng")){
            passwd = queryDBAuthValue(mContext, "password");
            if(TextUtils.isEmpty(passwd)){
               passwd = "137600";
            }
            return passwd;
        }
        //end:add by xumiao at 2019/12/16 get huawei usename set ipoe password
        if (SystemProperties.get("ro.ysten.province","master").contains("cm201_guangdong")){
                passwd = queryDBValue(mContext, "IPOEPassword");
        if(TextUtils.isEmpty(passwd)){
            passwd = "";
			
        }else{
             return passwd;
		}
        }
		//begin by zhangjunjian at 20190812 for ipoe password
        if (SystemProperties.get("ro.ysten.province","master").contains("guizhou")){
            passwd = queryDBValue(mContext, "IPOEPassword");
            if(TextUtils.isEmpty(passwd)){
                passwd = "";
            }else{
                return passwd;
            }
        }
		//begin by zhangjunjian at 20190812 for ipoe password
        if (SystemProperties.get("ro.ysten.province","master").contains("CM201-2_shanxi_iptv")
			||SystemProperties.get("ro.ysten.province","master").contains("cm201_beijing")){
			String temp[] = null;
	        if(readLoginInfo() != null){
				temp = readLoginInfo().split("\n");
			}
			if(temp != null && temp.length == 2){
				passwd = temp[1].trim();
			}
        if(TextUtils.isEmpty(passwd)){
                passwd = "";
				Log.e(TAG, "getDhcpOption60Password: null");
        }else{
			Log.e(TAG, "getDhcpOption60Password: " + passwd);
			return passwd;
		}
	    }

        // begin: add by ysten tianchining at 20181126: modified getDhcpOption60Password
        if (SystemProperties.get("ro.ysten.province","master").contains("shanxi")){
            String temp[] = null;
            if(readLoginInfo() != null){
                temp = readLoginInfo().split("\n");
            }
            if(temp != null && temp.length == 2){
                passwd = temp[1].trim();
            }
            if(TextUtils.isEmpty(passwd)){
                passwd = "";
                Log.e(TAG, "getDhcpOption60Password: null");
            }else{
                Log.e(TAG, "getDhcpOption60Password: " + passwd);
				return passwd;
            }           
        }
        // end: add by ysten tianchining at 20181126: modified getDhcpOption60Password

	if (SystemProperties.get("ro.ysten.province","master").contains("liaoning")){//add by ysten xumiao at 20191118: modify CM201-2_liaoning to liaoning & save username pws to db
			passwd = getUserPassword(mContext);
			Settings.Secure.putString(cr, Settings.Secure.DHCP_PSWD, passwd);//add by ysten xumiao at 20191118: modify CM201-2_liaoning to liaoning & save username pws to db
            if(TextUtils.isEmpty(passwd)){
                passwd = "";
				Log.e(TAG, "getDhcpOption60Password: null");
            }else{
			Log.e(TAG, "getDhcpOption60Password: " + passwd);
			return passwd;
		}

	    }
        
        try {
            //passwd = Settings.Secure.getString(cr, Settings.Secure.OPTION60_PASSWORD);
            passwd = NetworkUtils.passwdDecode(Settings.Secure.getString(cr, Settings.Secure.OPTION60_PASSWORD)); //with password Decrypt
            if("".equals(passwd))
                passwd = null;
            if(DEBUG) Log.d(TAG, "getDhcpOption60Password: " + passwd);
            return passwd;
        } catch (Exception e) {
            Log.e(TAG, "getDhcpOption60Password Error!");
            return null;
        }
    }

    public void setDhcpOption60(boolean setEnable, String login, String password) {
        if (!checkPermission())
        {
            return;
        }

        if ("aosp".equals(SystemProperties.get("ro.product.target", "aosp"))) {
            Log.e(TAG, "dhcpplus is not enabled");
            return;
        }

        if(DEBUG) Log.d(TAG, "setDhcpOption60(" + setEnable + ", " + login + ", " + password + ")");
        final ContentResolver cr = mContext.getContentResolver();
        Settings.Secure.putInt(cr, Settings.Secure.OPTION60_ON,
                setEnable ? EthernetManager.OPTION60_STATE_ENABLED: EthernetManager.OPTION60_STATE_DISABLED);
        if (setEnable && SystemProperties.get("ro.ysten.province","master").contains("sichuan")){
		saveDBValue(mContext, "IPOEID", login);
        saveDBValue(mContext, "IPOEPassword", password);
		}
        if (setEnable && SystemProperties.get("ro.ysten.province","master").contains("cm201_guangdong")){
                saveDBValue(mContext, "IPOEID", login);
        saveDBValue(mContext, "IPOEPassword", password);
                }
		//begin by zhangjunjian at 20190812 for ipoe
        if (setEnable && SystemProperties.get("ro.ysten.province","master").contains("guizhou")){
            saveDBValue(mContext, "IPOEID", login);
            saveDBValue(mContext, "IPOEPassword", password);
        }
		//end by zhangjunjian at 20190812 for ipoe
		//begin add by wangrongke  for  yunnan ipoe 
		 if (setEnable && SystemProperties.get("ro.ysten.province","master").contains("yunnan")){
            saveDBValueYn(mContext, "username", login);
            saveDBValueYn(mContext, "Password", password);
        }
		//end by wangrongke  for yunnan ipoe
       //add by zhangjunjian at 20191212 for hainan ipoe
       if (SystemProperties.get("ro.ysten.province","master").contains("hainan")){
           Log.e(TAG, "changed"+login);
           Settings.Secure.putString(cr, Settings.Secure.DHCP_USER,login);
           Settings.Secure.putString(cr, Settings.Secure.DHCP_PSWD,password);
           Settings.Secure.putString(cr, Settings.Secure.OPTION60_LOGIN, login+"@iptv");
       }else{
        Settings.Secure.putString(cr, Settings.Secure.OPTION60_LOGIN, login);
       }
	   
	   if (SystemProperties.get("ro.ysten.sub.province","master").equals("cm201_hainan_jidi")){
           Log.e(TAG, "changed"+login);
           Settings.Secure.putString(cr, Settings.Secure.DHCP_USER,login);
           Settings.Secure.putString(cr, Settings.Secure.DHCP_PSWD,password);
           //Settings.Secure.putString(cr, Settings.Secure.OPTION60_LOGIN, login+"@iptv");
       }
       //end by zhangjunjian at 20191212 for hainan ipoe
        password = NetworkUtils.passwdEncode(password); //password Encrypt
        Settings.Secure.putString(cr, Settings.Secure.OPTION60_PASSWORD, password);	
       	
    }

    public int getDhcpOption125State() {
        final ContentResolver cr = mContext.getContentResolver();
        int option125State = 0;
        try {
            option125State = Settings.Secure.getInt(cr, Settings.Secure.OPTION125_ON);
            if(DEBUG) Log.d(TAG, "getDhcpOption125State: " + option125State);
            return option125State;
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "getWifiDisguiseState: STATE UNKNOWN");
            return EthernetManager.OPTION125_STATE_UNKNOWN;
        }
    }

    public String getDhcpOption125Info() {
        final ContentResolver cr = mContext.getContentResolver();
        String option125Info = null;
        try {
            option125Info = Settings.Secure.getString(cr, Settings.Secure.OPTION125_INFO);
            if("".equals(option125Info))
                option125Info = null;
            if(DEBUG) Log.d(TAG, "getDhcpOption125Info: " + option125Info);
            return option125Info;
        } catch (Exception e) {
            Log.e(TAG, "getDhcpOption125Info Error!");
            return null;
        }
    }

    public void setDhcpOption125(boolean setEnable, String option125Info) {
        if (!checkPermission())
        {
            return;
        }

        if ("aosp".equals(SystemProperties.get("ro.product.target", "aosp"))) {
            Log.e(TAG, "dhcpplus is not enabled");
            return;
        }

        if(DEBUG) Log.d(TAG, "setDhcpOption125(" + setEnable + ", " + option125Info + ")");
        final ContentResolver cr = mContext.getContentResolver();
        Settings.Secure.putInt(cr, Settings.Secure.OPTION125_ON,
                setEnable ? EthernetManager.OPTION125_STATE_ENABLED: EthernetManager.OPTION125_STATE_DISABLED);
        Settings.Secure.putString(cr, Settings.Secure.OPTION125_INFO, option125Info);
    }

    public void setWifiEthernetCoexist(boolean setEnable) {
        if (!checkPermission())
        {
            return;
        }
        if(DEBUG) Log.d(TAG, "setWifiEthernetCoexist(" + setEnable + ")");
        final ContentResolver cr = mContext.getContentResolver();

        ConnectivityManager mConnectivityManager =
            (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        EthernetManager mEthernetManager = (EthernetManager) mContext.getSystemService(Context.ETHERNET_SERVICE);
        WifiManager mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        boolean mEthernetEnabled = (EthernetManager.ETHERNET_STATE_ENABLED == mEthernetManager.getEthernetState());
        boolean mWifiEnabled = mWifiManager.isWifiEnabled();

        if(mEthernetEnabled && mWifiEnabled) {
            Log.i(TAG, "Both Network Enabled!");
            mWifiManager.setWifiEnabled(false);
            mEthernetManager.enableEthernet(false);

            Settings.Secure.putInt(cr, Settings.Secure.WIFI_ETHERNET_COEXIST,
                    setEnable ? EthernetManager.WIFI_ETHERNET_COEXIST_ENABLED: EthernetManager.WIFI_ETHERNET_COEXIST_DISABLED);

            if(mEthernetEnabled) {
                Log.i(TAG, "Coexist = " + setEnable + " Ethernet Enabled! Restart!!");
                mEthernetManager.enableEthernet(true);
            } else {
                Log.i(TAG, "Coexist = " + setEnable + " Ethernet Not Enabled! Do Not Restart!!");
            }

            if(mWifiEnabled) {
                Log.i(TAG, "Coexist = " + setEnable + " Wifi Enabled! Restart!!");
                mWifiManager.setWifiEnabled(true);
            } else {
                Log.i(TAG, "Coexist = " + setEnable + " Wifi Not Enabled! Do Not Restart!!");
            }
        } else {
            Log.i(TAG, "Only One Network Enabled, or No One Enabled Only Set Database!");
            Settings.Secure.putInt(cr, Settings.Secure.WIFI_ETHERNET_COEXIST,
                    setEnable ? EthernetManager.WIFI_ETHERNET_COEXIST_ENABLED: EthernetManager.WIFI_ETHERNET_COEXIST_DISABLED);
        }
    }

    public int getWifiEthernetCoexistState() {
        final ContentResolver cr = mContext.getContentResolver();
        try {
            if(DEBUG) Log.d(TAG, "getWifiEthernetCoexistState(): "
                    + Settings.Secure.getInt(cr, Settings.Secure.WIFI_ETHERNET_COEXIST));
            return Settings.Secure.getInt(cr, Settings.Secure.WIFI_ETHERNET_COEXIST);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "getWifiEthernetCoexistState: STATE UNKNOWN");
            return EthernetManager.WIFI_ETHERNET_COEXIST_STATE_UNKNOWN;
        }
    }
    //add for chinamobile
    public void enableIpv4(boolean setEnable) {
        String product = SystemProperties.get("ro.product.target", "ott");
        if (!checkPermission())
        {
            return;
        }
       if (!isEthernetConfigured()) {
            Log.i(TAG, "EthernetConfigured no config, USE DHCP");
            // If user did not configure any interfaces yet, pick the first one
            // and enable it.
            setEthernetMode(EthernetManager.ETHERNET_CONNECT_MODE_DHCP, null);
        }
        persistIpv4Enabled(setEnable);
        if(setEnable) {
            if(DEBUG) Log.d(TAG, "enableIpv4(" + setEnable + ") TurnOn IPV4!");
             if ("shcmcc".equals(product)) {
                    String mode = getEthernetMode();
                    if(mode != null && mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE)) {
                        Message msg = new Message();
                        msg.what = ETHERNET_PPPOE_CONNECT;
                        mPppoeConnectHandler.removeMessages(ETHERNET_PPPOE_CONNECT);
                        mPppoeConnectHandler.sendMessageDelayed(msg, 300);
                    } else {
                        mTracker.resetInterface();
                    }
                } else {
                    mTracker.resetInterface();
                }
        } else {
            Log.i(TAG, "enableIpv4(" + setEnable + ") TurnOff IPV4!");
            if ("shcmcc".equals(product))
                    mPppoeConnectHandler.removeMessages(ETHERNET_PPPOE_CONNECT);
                mTracker.stopInterface();
        }
    }

    public void connectIpv4(boolean connect){
        if(connect){
            mTracker.resetInterface();
        }else{
            //sync hisi patch, fix ipoe can link success when pwd error bug 2019.11.27 
            if((getEthernetMode().equals(EthernetManager.ETHERNET_CONNECT_MODE_DHCP))) {
              Log.d(TAG, "markyuan ethernet " + getEthernetMode() + " mode, not need release dhcp lease");
              NetworkUtils.releaseDhcpLease("eth0");
            }

            mTracker.stopInterface();
        }

    }

    void enableInterfaceINET6(String ifName, boolean setEnable) {
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        INetworkManagementService mNMService = INetworkManagementService.Stub.asInterface(b);
        Log.d(TAG, "enableInterfaceINET6(" + ifName + ", "+ setEnable + ")");
        try {
            if(setEnable) {
                mNMService.enableIpv6(ifName);
            } else {
                mNMService.disableIpv6(ifName);
            }
        } catch (Exception e) {
            Log.e(TAG, "enableInterfaceINET6("+setEnable+") failed. e="+e);
        }
    }

    public void enableIpv6(boolean setEnable) {
        if (!checkPermission())
        {
            return;
        }
        persistIpv6Enabled(setEnable);
        if(setEnable) {
            enableInterfaceINET6(mIface, setEnable); // start ipv6 in ifconfig
            if(DEBUG) Log.d(TAG, "enableIpv6(" + setEnable + ") TurnOn IPV6!");
            if(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL.equals(getEthernetMode6()) ||
               EthernetManager.ETHERNET_CONNECT_MODE_STATELESS.equals(getEthernetMode6()) ) {
                //user choose manual or stateless
            }else if(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(getEthernetMode6())){
                //PPPoE V6 started with PPPoE v6
                return;
            }else{
                Log.i(TAG, "IPV6 use DHCP");
                setEthernetMode6(EthernetManager.ETHERNET_CONNECT_MODE_DHCP);
            }
            mTracker.startIpv6();
        } else {
            Log.i(TAG, "enableIpv6(" + setEnable + ") TurnOff IPV6!");
            mTracker.stopIpv6();
            enableInterfaceINET6(mIface, setEnable); // close ipv6 in ifconfig
        }
    }

    public void connectIpv6(boolean connect){
        if(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(getEthernetMode6())) {
            if(connect){
                Log.e(TAG, "Mark Connect pppoe ipv6");
                setEthernetState(EthernetManager.ETHERNET_STATE_DISABLED);
                mTracker.stopIpv6();
                setEthernetMode(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE, null);
                setEthernetMode6(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE);
                //setIpv6PppoeStateless(true); //set stateless as default ipv6 mode for pppoe
                setEthernetState(EthernetManager.ETHERNET_STATE_ENABLED);
            }else{
                mTracker.stopIpv6();
            }
        } else {
            if(connect){
                mTracker.startIpv6();
            }else{
                mTracker.stopIpv6();
            }
        }

    }


    public int getIpv6PersistedState() {

        final ContentResolver cr = mContext.getContentResolver();
        try {
            if(DEBUG) Log.d(TAG, "getIpv6PersistedState() IPV6_ON:"
                    + Settings.Secure.getInt(cr, Settings.Secure.IPV6_ON));
            return Settings.Secure.getInt(cr, Settings.Secure.IPV6_ON);
        } catch (Exception e) {
            Log.e(TAG, "getIpv6PersistedState() IPV6_STATE_UNKNOWN");
            return EthernetManager.IPV6_STATE_UNKNOWN;
        }
    }

    public void setEthernetMode6(String mode) {
        if (!checkPermission())
        {
            return;
        }
        final ContentResolver cr = mContext.getContentResolver();
        if (mIface != null) {
            if(DEBUG) Log.d(TAG, "setEthernetMode6(" + mode + ")");
            Settings.Secure.putString(cr, Settings.Secure.ETHERNET_IPV6_MODE, mode);
        } else {
            Log.e(TAG, "ERROR! No Interface name set, please setInterfaceName first!");
        }
    }

    public synchronized String getEthernetMode6() {
        final ContentResolver cr = mContext.getContentResolver();
        try {
            if(DEBUG) Log.d(TAG, "getEthernetMode6(): "
                    + Settings.Secure.getString(cr, Settings.Secure.ETHERNET_IPV6_MODE));
            return Settings.Secure.getString(cr, Settings.Secure.ETHERNET_IPV6_MODE);
        } catch (Exception e) {
            Log.e(TAG, "getEthernetMode6 ERROR!");
            return null;
        }
    }

    public void setIpv6DatabaseInfo(String ip, int prefixlength, String gw, String dns1, String dns2) {
        if (!checkPermission())
        {
            return;
        }
        final ContentResolver cr = mContext.getContentResolver();
        if(DEBUG) Log.d(TAG, "setIpv6DatabaseInfo(" + ip + ", " + prefixlength + ", " + gw + ", " + dns1 + ", " + dns2 + ")");
        Settings.Secure.putString(cr, Settings.Secure.ETHERNET_IPV6_IP, ip);
        Settings.Secure.putInt(cr, Settings.Secure.ETHERNET_IPV6_PREFIXLENGTH, prefixlength);
        Settings.Secure.putString(cr, Settings.Secure.ETHERNET_IPV6_ROUTE, gw);
        Settings.Secure.putString(cr, Settings.Secure.ETHERNET_IPV6_DNS_1, dns1);
        Settings.Secure.putString(cr, Settings.Secure.ETHERNET_IPV6_DNS_2, dns2);
    }

    public synchronized String getIpv6DatabaseAddress() {
        final ContentResolver cr = mContext.getContentResolver();
        try {
            if(DEBUG) Log.d(TAG, "getIpv6DatabaseAddress(): "
                    + Settings.Secure.getString(cr, Settings.Secure.ETHERNET_IPV6_IP));
            return Settings.Secure.getString(cr, Settings.Secure.ETHERNET_IPV6_IP);
        } catch (Exception e) {
            Log.e(TAG, "getIpv6DatabaseAddress ERROR!");
            return null;
        }
    }

    public synchronized int getIpv6DatabasePrefixlength() {

        final ContentResolver cr = mContext.getContentResolver();
        try {
            if(DEBUG) Log.d(TAG, "getIpv6DatabasePrefixlength(): "
                    + Settings.Secure.getString(cr, Settings.Secure.ETHERNET_IPV6_PREFIXLENGTH));
            return Settings.Secure.getInt(cr, Settings.Secure.ETHERNET_IPV6_PREFIXLENGTH);
        } catch (Exception e) {
            Log.e(TAG, "getIpv6DatabasePrefixlength ERROR!");
            return 0;
        }
    }

    public synchronized String getIpv6DatabaseDns1() {

        final ContentResolver cr = mContext.getContentResolver();
        try {
            if(DEBUG) Log.d(TAG, "getIpv6DatabaseDns1(): "
                    + Settings.Secure.getString(cr, Settings.Secure.ETHERNET_IPV6_DNS_1));
            return Settings.Secure.getString(cr, Settings.Secure.ETHERNET_IPV6_DNS_1);
        } catch (Exception e) {
            Log.e(TAG, "getIpv6DatabaseDns1 ERROR!");
            return null;
        }
    }

    public synchronized String getIpv6DatabaseDns2() {

        final ContentResolver cr = mContext.getContentResolver();
        try {
            if(DEBUG) Log.d(TAG, "getIpv6DatabaseDns2(): "
                    + Settings.Secure.getString(cr, Settings.Secure.ETHERNET_IPV6_DNS_2));
            return Settings.Secure.getString(cr, Settings.Secure.ETHERNET_IPV6_DNS_2);
        } catch (Exception e) {
            Log.e(TAG, "getIpv6DatabaseDns2 ERROR!");
            return null;
        }
    }

    public synchronized String getIpv6DatabaseGateway() {

        final ContentResolver cr = mContext.getContentResolver();
        try {
            if(DEBUG) Log.d(TAG, "getIpv6DatabaseGateway(): "
                    + Settings.Secure.getString(cr, Settings.Secure.ETHERNET_IPV6_ROUTE));
            return Settings.Secure.getString(cr, Settings.Secure.ETHERNET_IPV6_ROUTE);
        } catch (Exception e) {
            Log.e(TAG, "getIpv6DatabaseGateway ERROR!");
            return null;
        }
    }

    public boolean checkDhcpv6Status(String ifname) {
        if (!checkPermission())
        {
            return false;
        }
        try {
            if(DEBUG) Log.d(TAG, "checkDhcpv6Status(" + ifname + "):" + NetworkUtils.checkDhcpv6Status(ifname));
            return NetworkUtils.checkDhcpv6Status(ifname);
        } catch (Exception e) {
            Log.e(TAG, "checkDhcpv6Status error");
            return false;
        }
    }

    public String getDhcpv6Ipaddress(String ifname) {
        try {
            if(DEBUG) Log.d(TAG, "getDhcpv6Ipaddress(" + ifname + "): " + NetworkUtils.getDhcpv6Ipaddress(ifname));
            return NetworkUtils.getDhcpv6Ipaddress(ifname);
        } catch (Exception e) {
            Log.e(TAG, "getDhcpv6Ipaddress ERROR!");
            return null;
        }
    }

    public String getIpv6LinklocalAddress(String ifname) {
        try {
            if(DEBUG) Log.d(TAG, "getIpv6LinklocalAddress(" + ifname + "): " + NetworkUtils.getIpv6LinklocalAddress(ifname));
            return NetworkUtils.getIpv6LinklocalAddress(ifname);
        } catch (Exception e) {
            Log.e(TAG, "getIpv6LinklocalAddress ERROR!");
            return null;
        }
    }

    public String getDhcpv6Gateway(String ifname) {
        try {
            if(DEBUG) Log.d(TAG, "getDhcpv6Gateway(" + ifname + "): " + NetworkUtils.getDhcpv6Gateway(ifname));
            return NetworkUtils.getDhcpv6Gateway(ifname);
        } catch (Exception e) {
            Log.e(TAG, "getDhcpv6Gateway ERROR!");
            return null;
        }
    }

    public String getDhcpv6Prefixlen(String ifname) {
        try {
            if(DEBUG) Log.d(TAG, "getDhcpv6Prefixlen(" + ifname + "): " + NetworkUtils.getDhcpv6Prefixlen(ifname));
            return NetworkUtils.getDhcpv6Prefixlen(ifname);
        } catch (Exception e) {
            Log.e(TAG, "getDhcpv6Prefixlen ERROR!");
            return null;
        }
    }

    public String getDhcpv6Dns(String ifname, int number) {
        try {
            if(DEBUG) Log.d(TAG, "getDhcpv6Dns(" + ifname + ", " + number + "): " + NetworkUtils.getDhcpv6Dns(ifname, number));
            return NetworkUtils.getDhcpv6Dns(ifname, number);
        } catch (Exception e) {
            Log.e(TAG, "getDhcpv6Dns ERROR!");
            return null;
        }
    }

    public int getDhcpv6DnsCnt(String ifname) {
        try {
            if(DEBUG) Log.d(TAG, "getDhcpv6DnsCnt(" + ifname + "): " + NetworkUtils.getDhcpv6DnsCnt(ifname));
            return NetworkUtils.getDhcpv6DnsCnt(ifname);
        } catch (Exception e) {
            Log.e(TAG, "getDhcpv6DnsCnt ERROR!");
            return 0;
        }
    }

    private String readFile(String fileName) throws IOException {
        String res = null;
        File file = new File(fileName);
        if(!file.exists()){
            return null;
        }else{
            FileInputStream fis = new FileInputStream(file);
            int length = fis.available();
            byte [] buffer = new byte[length];
            fis.read(buffer);
            res = EncodingUtils.getString(buffer, "UTF-8");
            fis.close();
        }
        return res;
    }

    public boolean releaseDhcpLease(String ifname) {
        if (!checkPermission())
        {
            return false;
        }
        if(ifname == null) {
            Log.e(TAG,"The given name "+ifname+" is invalid, return false");
            return false;
        }
        if(!(getEthernetMode().equals(EthernetManager.ETHERNET_CONNECT_MODE_DHCP))) {
            Log.d(TAG, "In ethernet " + getEthernetMode() + " mode, not need release dhcp lease");
            return false;
        }

        try {
            boolean ret = NetworkUtils.releaseDhcpLease(ifname);
            if(DEBUG) Log.d(TAG, "releaseDhcpLease(" + ifname + "): " + ret);
            return ret;
        } catch (Exception e) {
            Log.e(TAG, "releaseDhcpLease ERROR!");
            return false;
        }
    }

    public String getStatelessIpv6Address(){
       try {
           if(DEBUG) Log.d(TAG, " enter getStatelessIpv6Address()");
           return mTracker.getStatelessIpv6Address();
       } catch (Exception e) {
           Log.e(TAG, "getStatelessIpv6Address ERROR!");
           return null;
       }
    }

    public String getStatelessIpv6Gateway(String ifname){
       try {
            if(DEBUG) Log.d(TAG, "getStatelessIpv6Gateway(): " + NetworkUtils.getDhcpv6Gateway(ifname));
            return NetworkUtils.getDhcpv6Gateway(ifname);
        } catch (Exception e) {
            Log.e(TAG, "getStatelessIpv6Gateway ERROR!");
            return null;
        }
    }

    public int getStatelessIpv6Prefixlength()
    {
        try {
            if(DEBUG) Log.d(TAG, " enter getStatelessIpv6Prefixlength()");
            return mTracker.getStatelessIpv6Prefixlength();
        } catch (Exception e) {
            Log.e(TAG, "getStatelessIpv6Prefixlength ERROR!");
            return 0;
        }
    }

    public String getStatelessIpv6Dns1(){
        try {
            if(DEBUG) Log.d(TAG, " enter getStatelessIpv6Dns1()");
            return mTracker.getStatelessIpv6Dns1();
        } catch (Exception e) {
            Log.e(TAG, "getStatelessIpv6Dns1 ERROR!");
            return null;
        }
    }

    public String getStatelessIpv6Dns2(){
        try {
            if(DEBUG) Log.d(TAG, " enter getStatelessIpv6Dns2()");
            return mTracker.getStatelessIpv6Dns2();
        } catch (Exception e) {
            Log.e(TAG, "getStatelessIpv6Dns2 ERROR!");
            return null;
        }
    }

    //add for chinamobile
    public int getIpv4ConnectStatus(){
        try {
            if(DEBUG) Log.d(TAG, " enter getIpv4ConnectStatus()");
            return mTracker.getIpv4ConnectStatus();
        } catch (Exception e) {
            Log.e(TAG, "getIpv4ConnectStatus ERROR!");
            return 0;
        }
    }

    //add for chinamobile
    public int getIpv6ConnectStatus(){
        try {
            if(DEBUG) Log.d(TAG, " enter getIpv6ConnectStatus()");
            return mTracker.getIpv6ConnectStatus();
        } catch (Exception e) {
            Log.e(TAG, "getIpv6ConnectStatus ERROR!");
            return 0;
        }
    }

    //add for chinamobile
    public void setPppoeUsername(String Username) {
        if (!checkPermission())
        {
            return;
        }
        final ContentResolver cr = mContext.getContentResolver();
        try {
            if(DEBUG) Log.d(TAG, "setPppoeUsername(" + Username + ")");
            Settings.Secure.putString(cr, Settings.Secure.PPPOE_USER_NAME, Username);
        } catch (Exception e) {
            Log.e(TAG, "setPppoeUsername Error!");
        }
    }

    //add for chinamobile
    public String getPppoeUsername() {
        if (!checkPermission())
        {
            return null;
        }
        final ContentResolver cr = mContext.getContentResolver();
        try {
            if(DEBUG) Log.d(TAG, "getPppoeUsername()");
            return Settings.Secure.getString(cr, Settings.Secure.PPPOE_USER_NAME);
        } catch (Exception e) {
            Log.e(TAG, "getPppoeUsername Error!");
            return null;
        }
    }

    //add for chinamobile
    public void setPppoePassword(String Password) {
        if (!checkPermission())
        {
            return;
        }
        final ContentResolver cr = mContext.getContentResolver();
        try {
            if(DEBUG) Log.d(TAG, "setPppoePassword(" + Password + ")");
            Password = NetworkUtils.passwdEncode(Password); //password Encrypt
            Settings.Secure.putString(cr, Settings.Secure.PPPOE_USER_PASS, Password);
        } catch (Exception e) {
            Log.e(TAG, "setPppoePassword Error!");
        }
    }

    //add for chinamobile
    public String getPppoePassword() {
        if (!checkPermission())
        {
            return null;
        }
        String passwd = null;
        final ContentResolver cr = mContext.getContentResolver();
        try {
            if(DEBUG) Log.d(TAG, "getPppoePassword()");
            passwd = NetworkUtils.passwdDecode(Settings.Secure.getString(cr, Settings.Secure.PPPOE_USER_PASS)); //password Decrypt
            if("".equals(passwd))
                passwd = null;
            if(DEBUG) Log.d(TAG, "getPppoePassword: " + passwd);
            return passwd;
        } catch (Exception e) {
            Log.e(TAG, "getPppoePassword Error!");
            return null;
        }
    }

    public void setIpv6PppoeStateless(boolean enable){
        try {
            if(DEBUG) Log.d(TAG, "setIpv6PppoeStateless(" + enable + ")");
            final ContentResolver cr = mContext.getContentResolver();
            Settings.Secure.putInt(cr, Settings.Secure.ETHERNET_PPPOE_IPV6_STATELESS,
                    enable ? EthernetManager.IPV6_PPPOE_STATELESS_ENABLED : EthernetManager.IPV6_PPPOE_STATELESS_DISABLED);
        } catch (Exception e) {
            Log.e(TAG, "setIpv6PppoeStateless(" + enable + ") ERROR!");
        }
    }

    public boolean isIpv6PppoeStateless(){
        final ContentResolver cr = mContext.getContentResolver();
        int state = 0;
        try {
            state = Settings.Secure.getInt(cr, Settings.Secure.ETHERNET_PPPOE_IPV6_STATELESS);
            if(DEBUG) Log.d(TAG, "getDhcpOption60State: " + state);
            if(EthernetManager.IPV6_PPPOE_STATELESS_ENABLED == state)
                return true;
            else
                return false;
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "isIpv6PppoeStateless: STATE UNKNOWN");
            return false;
        }
    }
    
	//add by guangchao.su
	public void disconnect() {
        mTracker.teardown();
    }
	
	
	private String readLoginInfo() {
		if(fileIsExists("/data/system/ipoe.conf")){
		Log.e(TAG, "ipoe file Exists!");
		File file = new File("/data/system/ipoe.conf");
		String loginInfo = new String();
		FileReader in;
		try {
			in = new FileReader(file);
			BufferedReader bufferedreader = new BufferedReader(in);
			// username
			loginInfo = bufferedreader.readLine();
			// passwd
			loginInfo += "\n" + bufferedreader.readLine();
			bufferedreader.close();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return loginInfo;
		}else{
		return "";
		}
	}
	
	private boolean fileIsExists(String strFile)  
    {  
        try  
        {  
            File f=new File(strFile);  
            if(!f.exists())  
            {  
                    return false;  
            }  
        }  
        catch (Exception e)  
        {  
            return false;  
        }  
        return true;  
    }  	
   
    //begin add by ysten chenfeng at 20190304: check and set ipoe 
    private String formatLogin(String mac){
        String tempMac = null;
        String finalMac = null;
        //add by guangchao.su ad 20180811:add jiangxi iptv ipoe
        String userMac = SystemProperties.get("persist.sys.mac.value", "");
        String mJXtempMac = null;
        //add by guangchao.su ad 20180811:add jiangxi iptv ipoe
               if (TextUtils.isEmpty(mac)){
               return "";
               }else{
               try{
               tempMac=mac.replace(":","");
               mJXtempMac=userMac.replace(":","");
               //finalMac= tempMac.substring(0,3)+"-"+tempMac.substring(4,7)+"-"+tempMac.substring(8,11);
               if(SystemProperties.get("ro.ysten.province","master").equals("cm201_jiangxi")){
                         finalMac= mJXtempMac.substring(0,4)+"-"+mJXtempMac.substring(4,8)+"-"+mJXtempMac.substring(8)+"@iptv";
              Log.d(TAG,"finalMac is "+ finalMac+", mJXtempMac is "+mJXtempMac);
                 }else {
                         finalMac= tempMac.substring(0,3)+"-"+tempMac.substring(4,7)+"-"+tempMac.substring(8,11);
                 }
                    //finalMac= tempMac.substring(0,3)+"-"+tempMac.substring(4,7)+"-"+tempMac.substring(8,11);
                    return finalMac;
                 } catch (Exception e) {
                   e.printStackTrace();
                   return "";
                   }
                }
         }
    
    private String loadFileAsString(String filePath)
              throws java.io.IOException {
          StringBuffer fileData = new StringBuffer(1000);
          BufferedReader reader = new BufferedReader(new FileReader(
                  filePath));
          char[] buf = new char[1024];
          int numRead = 0;
          while ((numRead = reader.read(buf)) != -1) {
              String readData = String.valueOf(buf, 0, numRead);
              fileData.append(readData);
          }
          reader.close();
          return fileData.toString();
    }
    
    private String getEthMacAddress() {
          try {
              return loadFileAsString("/sys/class/net/eth0/address")
                      .toUpperCase().substring(0, 17);
          } catch (IOException e) {
              e.printStackTrace();
              return "";
          }
    }
    
    private String readMac() {
          Log.i(TAG, "readMac");
          try {
              Process p = Runtime.getRuntime().exec("/system/bin/hissecurezone -r");

              BufferedReader in = new BufferedReader(new InputStreamReader(
                      p.getInputStream()));
              String line = null;
              while ((line = in.readLine()) != null) {
                  Log.i(TAG, "readMac read line " + line);
                  if (line.contains("mac")) {
                      return line.toUpperCase().substring(16);
                  }

              }
          } catch (Exception e) {
              Log.e(TAG, "eeeeee!!!", e);
          }
          return "";
    }
	//begin by zhangjunjian at 20190812 for ipoe 
	private String getIMEI(){
		String userIMEI = SystemProperties.get("persist.sys.yst.serialno", "");
		return userIMEI;
	}
	
	private String readIMEI() {
          Log.i(TAG, "readIMEI");
          try {
              Process p = Runtime.getRuntime().exec("/system/bin/hissecurezone -r");

              BufferedReader in = new BufferedReader(new InputStreamReader(
                      p.getInputStream()));
              String line = null;
              while ((line = in.readLine()) != null) {
                  Log.i(TAG, "readIMEI read line " + line);
                  if (line.contains("IMEI")) {
                      return line.toUpperCase().substring(17);
                  }

              }
          } catch (Exception e) {
              Log.e(TAG, "eeeeee!!!", e);
          }
          return "";
    }
    //end by zhangjunjian at 20190812 for ipoe 
    
    //begin by zhuhengxuan at 20200719 for jiangsu IPOE
    private String readjiangsuAccount() {
        String value = "";
        Log.i(TAG, "readjiangsuAccount");
        String mPropFile = "/mnt/sdcard/backup_devinfo/DevInfo.prop";
        if(SystemProperties.get("ro.ysten.province","master").equals("c60_jiangsu")){
            String path = SystemProperties.get("ro.propfile.path", "/sdcard");
            mPropFile = path + "/backup_devinfo/DevInfo.prop";
        }
        File file = new File(mPropFile);
        if(file.exists()){
            value = getPropValue(mPropFile,"mobile_phonenumber");
            Log.i(TAG, "readjiangsumobile_phonenumber :" + value);
            if(TextUtils.isEmpty(value)){
                value = getPropValue(mPropFile,"account");
                Log.i(TAG, "readjiangsuaccount :" + value);
            }
        }
        return value;
    }

    public String getPropValue(String mPropFile,String key){
        String value = null;
        Log.d(TAG, "getPropValue mPropFile=" + mPropFile);
        value = readFromFile(mPropFile, key);
        if(value == null) {
            value = "";
            Log.d(TAG, "getValue account default = " + value);
        }
        Log.d(TAG, "getPropValue key=" + key + ",value=" + value);
        return value;
    }

    private String readFromFile(String fileName, String key){
        String result = null;
        Map<String, String> map = getFileMap(fileName);
        if(map != null && map.containsKey(key)){
            result = map.get(key);
        } else {
            if(map != null && map.containsKey(key)){
                map.put(key, result);
            }
        }
        return result;
    }
    
    private Map<String, String> getFileMap(String fileName){
        Map<String, String> map = new HashMap<String, String>();
        try {
            RandomAccessFile raf = new RandomAccessFile(fileName, "r");
            FileChannel fc = raf.getChannel();
            int len = (int) fc.size();
            
            MappedByteBuffer mbb = fc.map(MapMode.READ_ONLY, 0, len);
            fc.close();
            raf.close();
            byte[] buffer = new byte[len];
            mbb.get(buffer);
            mbb.clear();
            
            String str = new String(buffer, 0, len);
            
            String[] lines = str.split("\n");
            for(String line: lines){
                String[] kv = line.split("=");
                if(kv != null && kv.length == 2){
                    map.put(kv[0], kv[1]);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return map;
    }
    //end:add by zhuhengxuan at 20200719 for jiangsu IPOE

    private void checkAndSetDhcpOption60() {    
          EthernetManager mEthernetManager = (EthernetManager) mContext.getSystemService(Context.ETHERNET_SERVICE);
          int option60 = 1;
          String login = null;
          String password = null;
          //modefy by wangrongke  for yunnan ipoe
          if(SystemProperties.get("ro.ysten.province","master").contains("yunnan"))
          {
			  option60 = mEthernetManager.getDhcpOption60State();
              login = mEthernetManager.getDhcpOption60Login();
              password = mEthernetManager.getDhcpOption60Password();
              if(option60 == mEthernetManager.OPTION60_STATE_ENABLED) {
			    if(TextUtils.isEmpty(login) || TextUtils.isEmpty(password)){
                    login = readMac();
                    password = "yncmcc138@itv";
                 }
                 mEthernetManager.setDhcpOption60(true, login, password);
			  }
          }
		  //end by wangrongke  for yunnan  ipoe 
	      //begin: add by tianchining at 20191106: shandong ipoe 
          else if(SystemProperties.get("ro.ysten.province","master").contains("shandong")){
            login = mEthernetManager.getDhcpOption60Login();
            password = mEthernetManager.getDhcpOption60Password();
            if(SystemProperties.get("persist.sys.boot.ipoe", "true").equals("true")){
                option60 = mEthernetManager.OPTION60_STATE_ENABLED;
                SystemProperties.set("persist.sys.boot.ipoe", "false");                                            
            }else{
                option60 = mEthernetManager.getDhcpOption60State();
            }
            if(option60 == mEthernetManager.OPTION60_STATE_ENABLED){
                if(TextUtils.isEmpty(login) || TextUtils.isEmpty(password)){
                    login = "sdmccstbipoe@otv";
                    password = "IuCXxTMT7OwuBllh";
                 }

                 Log.d(TAG, "TCN_ADD, shandong: ipoe mode, login: " + login + ": password " + password);
                 //persist.sys.ipoe.enable
                 SystemProperties.set("persist.sys.ipoe.enable", "1");
                 mEthernetManager.setDhcpOption60(true, login, password);
            }else{
                 Log.d(TAG, "TCN_ADD: shandong: dhcp mode !");
            }
          }
          //end: add by tianchining at 20191106: shandong ipoe
          else if(SystemProperties.get("ro.ysten.province","master").contains("cm201_guangdong")) {
              option60 = mEthernetManager.getDhcpOption60State();
              login = mEthernetManager.getDhcpOption60Login();
              password = mEthernetManager.getDhcpOption60Password();
              if(option60 == mEthernetManager.OPTION60_STATE_ENABLED) {
                  if(TextUtils.isEmpty(login)) {
                      login = getEthMacAddress();
                      if(TextUtils.isEmpty(login)){
                          Log.d(TAG,"read eth mac from hissercurezone");
                          login = readMac();
                      }
                  }
                  if(TextUtils.isEmpty(password)) {
                      password = "GdMCC68@OTV";
                  }
                  mEthernetManager.setDhcpOption60(true,login,password);
              }
              Log.i(TAG,"ipoe enable "+ option60 +" login "+ login +" password "+password);
          }
		  //begin by zhangjunjian at 20190812 for ipoe password 
		   else if(SystemProperties.get("ro.ysten.province","master").contains("guizhou")) {
              option60 = 1;
              login = mEthernetManager.getDhcpOption60Login();
              password = mEthernetManager.getDhcpOption60Password();
              if(option60 == mEthernetManager.OPTION60_STATE_ENABLED) {
                  if(TextUtils.isEmpty(login)) {
                      login = getIMEI();
					  //login = "";
                      if(TextUtils.isEmpty(login)){
                          Log.d(TAG,"read eth mac from hissercurezone");
                          login = readIMEI();
                      }
                  }
                  if(TextUtils.isEmpty(password)) {
                      password = "Gz68CMcc@iptv";
                  }
                  mEthernetManager.setDhcpOption60(true,login,password);
              }
              Log.i(TAG,"ipoe enable "+ option60 +" login "+ login +" password "+password);
          }
		  //end by zhangjunjian at 20190812 for ipoe password
                  //begin by zhangjunjian at 20191209 for hainan ipoe password
                   else if(SystemProperties.get("ro.ysten.province","master").contains("hainan")) {
              option60 = 1;
              String datalogin = mEthernetManager.getDhcpOption60Login();
              String datapassword = mEthernetManager.getDhcpOption60Password();
              if(option60 == mEthernetManager.OPTION60_STATE_ENABLED) {
                 
                      login = getIMEI();
                                          //login = "";
                      if(TextUtils.isEmpty(login)){
                          Log.d(TAG,"read eth mac from hissercurezone");
                          login = readIMEI();
                      }
                  
                      password = "hainan@iptv";
                  
                  if(!"".equals(datalogin) || !"".equals(datapassword))
                {
                  mEthernetManager.setDhcpOption60(true,login,password);
               }
              }
              Log.i(TAG,"ipoe enable "+ option60 +" login "+ login +" password "+password);
          }
                  //end by zhangjunjian at 20191209 for ipoe password                 
            else if(SystemProperties.get("ro.ysten.sub.province","master").equals("cm201_hainan_jidi")) {
              option60 = mEthernetManager.getDhcpOption60State();
			  Log.i(TAG,"cm201_hainan_jidi option60="+option60);
              String datalogin = mEthernetManager.getDhcpOption60Login();
              String datapassword = mEthernetManager.getDhcpOption60Password();
              if(option60 == mEthernetManager.OPTION60_STATE_ENABLED) {
                 
                      login = getIMEI();
                                          //login = "";
                      if(TextUtils.isEmpty(login)){
                          Log.d(TAG,"read eth mac from hissercurezone");
                          login = readIMEI()+"@itv";
                      }else{
						  login = login+"@itv";
					  }
                  
                      password = "123456";
                  
                  if(!"".equals(datalogin) || !"".equals(datapassword))
                {
                  mEthernetManager.setDhcpOption60(true,login,password);
               }
              }
              Log.i(TAG,"ipoe enable "+ option60 +" login "+ login +" password "+password);
          }
                  //end by zhangjunjian at 20191209 for ipoe password
          else if(SystemProperties.get("ro.ysten.province","master").contains("cm201_neimeng")) {
              option60 = mEthernetManager.getDhcpOption60State();
              login = mEthernetManager.getDhcpOption60Login();
              password = mEthernetManager.getDhcpOption60Password();
              if(option60 == mEthernetManager.OPTION60_STATE_ENABLED) {
                  if(TextUtils.isEmpty(login)) {      
                       Log.d(TAG,"read eth mac from hissercurezone");
                       login = "nmydiptv";               
                  }
                  if(TextUtils.isEmpty(password)) {
                          password = "137600";
                  }
                  mEthernetManager.setDhcpOption60(true,login,password);
              }
              Log.i(TAG,"ipoe enable "+ option60 +" login "+ login +" password "+password);
          }
		  //begain by zhangy at 20191028 for henan cm201-2 ipoe 
          else if(SystemProperties.get("ro.ysten.province","master").contains("cm201_henan")) {
              option60 = mEthernetManager.getDhcpOption60State();
              login = mEthernetManager.getDhcpOption60Login();
              password = mEthernetManager.getDhcpOption60Password();
              if(option60 == mEthernetManager.OPTION60_STATE_ENABLED) {
                  if(TextUtils.isEmpty(login)) {      
                       Log.d(TAG,"read eth mac from hissercurezone");
					   if(TextUtils.isEmpty(getUserID(mContext))){
						   login = "ha10086";					   
					   }else{
						   login=getUserID(mContext);
					   }
                        Log.d(TAG,"the login is :"+login);              
                  }
                  if(TextUtils.isEmpty(password)) {
					  if(TextUtils.isEmpty(getUserPassword(mContext))){
						  password = "hatv30";
					  }else{
						  password=getUserPassword(mContext);
					  }					  					
                       Log.d(TAG,"the password is :"+login);      
                  }
                  mEthernetManager.setDhcpOption60(true,login,password);
              }
              Log.i(TAG,"ipoe enable "+ option60 +" login "+ login +" password "+password);
          }
		  //end by zhangy @20191028 for henan cm201-2 ipoe
		  else if(SystemProperties.get("ro.ysten.province","master").contains("cm201_anhui_iptv")) {
			    Log.i(TAG,"anhui ipoe come!");
                option60 = mEthernetManager.getDhcpOption60State();
                login = mEthernetManager.getDhcpOption60Login();
                password = mEthernetManager.getDhcpOption60Password();
				Log.i(TAG,"anhui ipoe: "+" option60: "+option60+" login: "+login+" password: "+password+" mEthernetManager.OPTION60_STATE_ENABLED  "+mEthernetManager.OPTION60_STATE_ENABLED);
				//begin:add by zhanghk:modify anhui iptv ipoe way
                if(option60 == mEthernetManager.OPTION60_STATE_ENABLED) {
                    if(TextUtils.isEmpty(login)) {
                        login = "iptvdefault@preahiptv";
						Log.d(TAG,"zhangy 0");
                    }else{
						login = login+"@ahiptv";
						Log.d(TAG,"zhangy 1");
                    }
                    if(TextUtils.isEmpty(password)) {
                        password = "iptvdefault";
						Log.d(TAG,"2");
                    }
                    mEthernetManager.setDhcpOption60(true,login,password);
                }
				//end:add by zhanghk:modify anhui iptv ipoe way
            }
          else if(SystemProperties.get("ro.ysten.province","master").contains("sichuan")) {
              option60 = mEthernetManager.getDhcpOption60State();
              login = mEthernetManager.getDhcpOption60Login();
              password = mEthernetManager.getDhcpOption60Password();
              if(option60 == mEthernetManager.OPTION60_STATE_ENABLED) {
                  if(TextUtils.isEmpty(login)) {
                      login = getEthMacAddress();
                      if(TextUtils.isEmpty(login)){
                          Log.d(TAG,"read eth mac from hissercurezone");
                          login = readMac();
                      }
                  }
                  if(TextUtils.isEmpty(password)) {
                      password = "ScMCC68@OTV";
                  }
                  mEthernetManager.setDhcpOption60(true,login,password);
              }
              Log.i(TAG,"ipoe enable "+ option60 +" login "+ login +" password "+password);
              // add by YSTen }}
          }else if(SystemProperties.get("ro.ysten.province","master").contains("liaoning")) {//add by ysten xumiao at 20191118: modify CM201-2_liaoning to liaoning & save username pws to db
              option60 = mEthernetManager.getDhcpOption60State();
              option60 = 1;
              login = mEthernetManager.getDhcpOption60Login();
              password = mEthernetManager.getDhcpOption60Password();
              if(option60 == mEthernetManager.OPTION60_STATE_ENABLED) {
                  if(TextUtils.isEmpty(login)) {
                      login = "lnott@ott";
                  }
                  if(TextUtils.isEmpty(password)) {
                      password = "123456";
                  }
                  mEthernetManager.setDhcpOption60(true,login,password);
              }
            Log.i(TAG,"ipoe enable "+ option60 +" login "+ login +" password "+password);
           // add by YSTen }}
          //begin:add by zhanghk at 20190527:add hunan IPOE username and password
          }else if(SystemProperties.get("ro.ysten.province","master").contains("cm201_hunan")) {
              option60 = mEthernetManager.getDhcpOption60State();
              Log.d(TAG, "TCN_ADD: getDhcpOption60State: " + option60);
              option60 = 1;
              //login = mEthernetManager.getDhcpOption60Login();
              //password = mEthernetManager.getDhcpOption60Password();
              login = Settings.Secure.getString(mContext.getContentResolver(), "dhcp_user");
              password = Settings.Secure.getString(mContext.getContentResolver(), "dhcp_pswd");
              Log.d(TAG, "TCN_ADD: ipoe(" + login + ", " + password + ")");
              if( "4".equals(SystemProperties.get("default_network_priority", "4")) ){
                Log.d(TAG, "TCN_ADD: enableIpv4!");
                enableIpv4(true);      
              }else{
                Log.d(TAG, "TCN_ADD: enableIpv6!");
                enableIpv6(true);
              }              

              if(option60 == mEthernetManager.OPTION60_STATE_ENABLED) {
                  if(TextUtils.isEmpty(login)) {
                      login = "12345678901d001";
                  }
                  if(TextUtils.isEmpty(password)) {
                      password = "100086";
                  }
                  mEthernetManager.setDhcpOption60(true,login,password);
              }
            Log.i(TAG,"ipoe enable "+ option60 +" login "+ login +" password "+password);
            //end:add by zhanghk at 20190527:add hunan IPOE username and password
        }else if(SystemProperties.get("ro.ysten.province","master").contains("CM201-2_shanxi_iptv")) {
            option60 = mEthernetManager.getDhcpOption60State();
            option60 = 1;
            login = mEthernetManager.getDhcpOption60Login();
            password = mEthernetManager.getDhcpOption60Password();
            Log.i(TAG,"ipoe shanxi " +" login "+ login +" password "+password);
            if(option60 == mEthernetManager.OPTION60_STATE_ENABLED) {
                if(TextUtils.isEmpty(login)) {
                    login = "";
                }
                if(TextUtils.isEmpty(password)) {
                    password = "";
                }
                mEthernetManager.setDhcpOption60(true,login,password);          
            }
            Log.i(TAG,"ipoe enable "+ option60 +" login "+ login +" password "+password);
            // add by YSTen }}
        //begin: add by ysten zhuhengxuan at 20200708 for jiangsu ipoe 
        }else if(SystemProperties.get("ro.ysten.province","master").contains("c60_jiangsu")){
            Log.i(TAG, "ipoe zhuhx test1 ");
            option60 = mEthernetManager.getDhcpOption60State();
            String login1 = readjiangsuAccount();
            if(TextUtils.isEmpty(SystemProperties.get("persist.sys.launcher.account", ""))){
                if(TextUtils.isEmpty(login1)){
                    login = "";
                }else{
                    login = login1.replace("reg","");
                }
                SystemProperties.set("persist.sys.launcher.account",login);
            }
            password = "JS@HGU";
            SystemProperties.set("persist.sys.launcher.password",password);
            Log.i(TAG, "ipoe c60_jiangsu " +" login "+ login +" password "+password);
            Log.i(TAG, "ipoe enable "+ option60 +" login "+ login +" password "+password);
         //end: add by ysten zhuhengxuan at 20200708 for jiangsu ipoe
        }else if(SystemProperties.get("ro.ysten.province","master").contains("cm201_beijing")) {
            Log.i(TAG,"ipoe beijing " +" login "+ login +" password "+password);
               
            final ContentResolver cr = mContext.getContentResolver();
            Settings.Secure.putInt(cr, Settings.Secure.OPTION60_ON,EthernetManager.OPTION60_STATE_ENABLED);
            String password_tmp = "CMC10086cmcc.ott";
            String login_tmp = "2019cmcc10086.bjyd.ott";
            password_tmp = NetworkUtils.passwdEncode(password_tmp); //password Encrypt
            Settings.Secure.putString(cr, Settings.Secure.OPTION60_PASSWORD, password_tmp);         
            Settings.Secure.putString(cr, Settings.Secure.OPTION60_LOGIN, login_tmp);
            // add by YSTen }}
         }
		else if(SystemProperties.get("ro.ysten.province","master").contains("shanxi")){
            // begin: add by ysten tianchining at 20181126: add ipoe 
            option60 = mEthernetManager.getDhcpOption60State();
            login = mEthernetManager.getDhcpOption60Login();
            password = mEthernetManager.getDhcpOption60Password();
            Log.i(TAG, "ipoe shanxi " +" login "+ login +" password "+password);
            if(option60 == mEthernetManager.OPTION60_STATE_ENABLED) {
                if(TextUtils.isEmpty(login)) {
                    login = "";
                }
                if(TextUtils.isEmpty(password)) {
                    password = "";
                }
                mEthernetManager.setDhcpOption60(true,login,password);
            }
            Log.i(TAG, "ipoe enable "+ option60 +" login "+ login +" password "+password);
            //end: add by ysten tianchining at 20181126: add ipoe 
         }else if(SystemProperties.get("ro.ysten.province","master").contains("cm201_jiangxi")
		&&SystemProperties.get("persist.sys.launcher.value", "-1").equals("2")) {
            option60 = mEthernetManager.getDhcpOption60State();
            login = mEthernetManager.getDhcpOption60Login();
            password = mEthernetManager.getDhcpOption60Password();
            //add by guangchao.su ad 20180811:add jiangxi iptv ipoe
            int newoption60 = 1;
            Log.i("ysten_cm201","JX ipoe enable "+ option60 +" login "+ login +" password "+password+", newoption60 is "+newoption60);
            if(newoption60/*option60*/ == mEthernetManager.OPTION60_STATE_ENABLED) {
                /*if(TextUtils.isEmpty(login)) {
                    login = getEthMacAddress();
                    if(TextUtils.isEmpty(login)){
                        Log.d(TAG,"read eth mac from hissercurezone");
                        login = formatLogin(readMac());
                        Log.d(TAG,"final IPOE Account is "+ login);
                    }
                }*/
                login = formatLogin(readMac());
                Log.d(TAG,"IPOE Account is "+ login);
                Log.d(TAG,"final IPOE Account is "+ login);
                //add by guangchao.su ad 20180811:add jiangxi iptv ipoe
                if(TextUtils.isEmpty(password)) {
                    password = "JXcmcc18iptv";
                }
            mEthernetManager.setDhcpOption60(true,login,password);
            }
            Log.i(TAG,"ipoe enable "+ option60 +" login "+ login +" password "+password);
            // add by YSTen }}
         }
    }
    //end add by ysten chenfeng at 20190304: check and set ipoe 
}
