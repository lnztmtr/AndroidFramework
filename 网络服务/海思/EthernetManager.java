/*
 * Copyright (C) 2008 The Android Open Source Project
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

package android.net.ethernet;

import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.net.DhcpInfo;
import android.net.IpConfiguration;
import android.net.ethernet.IEthernetManager;
//add by guangchao.su
import android.os.ServiceManager;
import android.os.IBinder;
import android.content.Context;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.util.ArrayList;
import android.net.IpInfo;
import android.net.StaticIpConfiguration;
import android.net.NetworkUtils;
import android.net.LinkAddress;
import android.net.pppoe.PppoeManager;

public class EthernetManager {
    public static final String TAG = "EthernetManager";
    public static final int ETHERNET_DEVICE_SCAN_RESULT_READY = 0;
    public static final String ETHERNET_STATE_CHANGED_ACTION =
            "android.net.ethernet.ETHERNET_STATE_CHANGE";
    public static final String IPV4_STATE_CHANGED_ACTION =
            "android.net.ethernet.IPV4_STATE_CHANGE";
    public static final String IPV6_STATE_CHANGED_ACTION =
            "android.net.ethernet.IPV6_STATE_CHANGE";
    public static final String NETWORK_STATE_CHANGED_ACTION =
            "android.net.ethernet.STATE_CHANGE";

    public static final String EXTRA_NETWORK_INFO = "networkInfo";
    public static final String EXTRA_ETHERNET_STATE= "ethernet_state";
    public static final String EXTRA_PREVIOUS_ETHERNET_STATE= "previous_ethernet_state";

    public static final String EXTRA_ETHERNET_FAILED_REASON = "ethernet_failed_reason";
    public static final String PERSIST_STATELESS6_DNS1 = "persist.sys.stateless6.dns1";
    public static final String PERSIST_STATELESS6_DNS2 = "persist.sys.stateless6.dns2";

    public static final int ETHERNET_STATE_DISABLED = 0;
    public static final int ETHERNET_STATE_ENABLED  = 1;
    public static final int ETHERNET_STATE_UNKNOWN  = 2;

    public static final int EVENT_DHCP_CONNECT_SUCCESSED    = 10;
    public static final int EVENT_DHCP_CONNECT_FAILED       = 11;
    public static final int EVENT_DHCP_DISCONNECT_SUCCESSED = 12;
    public static final int EVENT_DHCP_DISCONNECT_FAILED    = 13;

    public static final int EVENT_STATIC_CONNECT_SUCCESSED    = 14;
    public static final int EVENT_STATIC_CONNECT_FAILED       = 15;
    public static final int EVENT_STATIC_DISCONNECT_SUCCESSED = 16;
    public static final int EVENT_STATIC_DISCONNECT_FAILED    = 17;

    public static final int EVENT_PHY_LINK_UP   = 18;
    public static final int EVENT_PHY_LINK_DOWN = 19;

    public static final int EVENT_IPV4_CONNECT_SUCCESSED = 100;
    public static final int EVENT_IPV4_CONNECT_FAILED = 101;
    public static final int EVENT_IPV4_DISCONNECT_SUCCESSED = 102;
    public static final int EVENT_IPV4_DISCONNECT_FAILED = 103;

    public static final int EVENT_IPV6_CONNECT_SUCCESSED = 100;
    public static final int EVENT_IPV6_CONNECT_FAILED = 101;
    public static final int EVENT_IPV6_DISCONNECT_SUCCESSED = 102;
    public static final int EVENT_IPV6_DISCONNECT_FAILED = 103;

    public static final int ETHERNET_CONNECT_STATE_UNKNOWN       = 4;
    public static final int ETHERNET_CONNECT_STATE_CONNECT       = 5;
    public static final int ETHERNET_CONNECT_STATE_DISCONNECT    = 6;
    public static final int ETHERNET_CONNECT_STATE_CONNECTING    = 7;
    public static final int ETHERNET_CONNECT_STATE_DISCONNECTING = 8;

    public static final int IPV4_STATE_DISABLED = 0;
    public static final int IPV4_STATE_ENABLED  = 1;
    public static final int IPV4_STATE_UNKNOWN  = 2;

    public static final int IPV6_STATE_DISABLED = 0;
    public static final int IPV6_STATE_ENABLED  = 1;
    public static final int IPV6_STATE_UNKNOWN  = 2;

    public static final int EVENT_DHCPV6_CONNECT_SUCCESSED        = 20;
    public static final int EVENT_DHCPV6_CONNECT_FAILED           = 21;
    public static final int EVENT_DHCPV6_DISCONNECT_SUCCESSED     = 22;
    public static final int EVENT_DHCPV6_DISCONNECT_FAILED        = 23;
    public static final int EVENT_STATIC6_CONNECT_SUCCESSED       = 24;
    public static final int EVENT_STATIC6_CONNECT_FAILED          = 25;
    public static final int EVENT_STATIC6_DISCONNECT_SUCCESSED    = 26;
    public static final int EVENT_STATIC6_DISCONNECT_FAILED       = 27;
    public static final int EVENT_STATELESS6_CONNECT_SUCCESSED    = 28;
    public static final int EVENT_STATELESS6_CONNECT_FAILED       = 29;
    public static final int EVENT_STATELESS6_DISCONNECT_SUCCESSED = 30;
    public static final int EVENT_STATELESS6_DISCONNECT_FAILED    = 31;
    public static final int EVENT_PPPOE_DHCPV6_CONNECT_SUCCESSED        = 32;
    public static final int EVENT_PPPOE_DHCPV6_CONNECT_FAILED           = 33;
    public static final int EVENT_PPPOE_DHCPV6_DISCONNECT_SUCCESSED     = 34;
    public static final int EVENT_PPPOE_DHCPV6_DISCONNECT_FAILED        = 35;
    public static final int EVENT_PPPOE_STATELESS6_CONNECT_SUCCESSED    = 36;
    public static final int EVENT_PPPOE_STATELESS6_CONNECT_FAILED       = 37;
    public static final int EVENT_PPPOE_STATELESS6_DISCONNECT_SUCCESSED = 38;
    public static final int EVENT_PPPOE_STATELESS6_DISCONNECT_FAILED    = 39;

    public static final int ETHERNET_IPV6_CONNECT_STATE_UNKNOWN       = 4;
    public static final int ETHERNET_IPV6_CONNECT_STATE_CONNECT       = 5;
    public static final int ETHERNET_IPV6_CONNECT_STATE_DISCONNECT    = 6;
    public static final int ETHERNET_IPV6_CONNECT_STATE_CONNECTING    = 7;
    public static final int ETHERNET_IPV6_CONNECT_STATE_DISCONNECTING = 8;

    public static final int WIFI_DISGUISE_DISABLED      = 0;
    public static final int WIFI_DISGUISE_ENABLED       = 1;
    public static final int WIFI_DISGUISE_STATE_UNKNOWN = 2;

    public static final int ETHERNET_AUTORECONNECT  = 40;
    public static final int ETHERNET_RECONNECT_ONCE = 41;
    public static final int DHCP6_AUTORUN = 45;
    public static final int ETHERNET_RECONNECT_TIME = 20000;

    public static final int ETHERNET_PPPOE_STATELESS6_AUTORUN      = 46;
    public static final int ETHERNET_PPPOE_STATELESS6_AUTORUN_TIME = 5000;

    public static final int ETHERNET_AUTORECONNECT_DISABLED            = 0;
    public static final int ETHERNET_AUTORECONNECT_ENABLED             = 1;
    public static final int ETHERNET_AUTORECONNECT_STATE_UNKNOWN       = 2;

    public static final int EVENT_DHCP_AUTORECONNECTING       = 50;
    public static final int EVENT_DHCPV6_AUTORECONNECTING     = 51;

    public static final int OPTION60_STATE_DISABLED = 0;
    public static final int OPTION60_STATE_ENABLED  = 1;
    public static final int OPTION60_STATE_UNKNOWN  = 2;

    public static final int OPTION125_STATE_DISABLED = 0;
    public static final int OPTION125_STATE_ENABLED  = 1;
    public static final int OPTION125_STATE_UNKNOWN  = 2;

    public static final int WIFI_ETHERNET_COEXIST_DISABLED      = 0;
    public static final int WIFI_ETHERNET_COEXIST_ENABLED       = 1;
    public static final int WIFI_ETHERNET_COEXIST_STATE_UNKNOWN = 2;

    //Broadcast error value definition, add for chinamobile
    public static final int FAILED_REASON_DISCOVER_TIMEOUT = 1; // discover timeout
    public static final int FAILED_REASON_REQUEST_TIMEOUT = 2;  // request timeout
    public static final int FAILED_REASON_IPOE_AUTH_FAIL = 3;   // IPOE auth failed
    public static final int FAILED_REASON_PPPOE_AUTH_FAILED = 4;// PPPOE auth failed
    public static final int FAILED_REASON_PPPOE_TIMEOUT = 5;    // PPPOE timeout
    public static final int FAILED_REASON_INVALID_PARAMETER = 6;// Invalid parameter

    public static final int IPV6_PPPOE_STATELESS_DISABLED = 0;
    public static final int IPV6_PPPOE_STATELESS_ENABLED  = 1;
    public static final int IPV6_PPPOE_STATELESS_UNKNOWN  = 2;

    public static final int ETHERNET_IPV6MOAUTO_DISABLED            = 0;
    public static final int ETHERNET_IPV6MOAUTO_ENABLED             = 1;
    public static final int ETHERNET_IPV6MOATUO_STATE_UNKNOWN       = 2;

    /**
     * The ethernet interface is configured by dhcp
     */
    public static final String ETHERNET_CONNECT_MODE_DHCP = "dhcp";

    /**
     * The ethernet interface is configured manually
     */
    public static final String ETHERNET_CONNECT_MODE_MANUAL = "manual";

    /**
     * The ethernet interface is configured manually
     */
    public static final String ETHERNET_CONNECT_MODE_STATELESS = "stateless";

    /**
     * The ethernet interface is configured by pppoe
     */
    public static final String ETHERNET_CONNECT_MODE_PPPOE = "pppoe";

    /**
     * The ethernet interface is configured by ipoe
     */
    public static final String ETHERNET_CONNECT_MODE_IPOE = "ipoe";

    /**
     * The ethernet interface is configured none
     */
    public static final String ETHERNET_CONNECT_MODE_NONE = "none";

    private boolean DEBUG = false;

    IEthernetManager mService;
    Handler mHandler;
	//add by guangchao.su
	private static EthernetManager mEthManager;

    public EthernetManager(IEthernetManager service, Handler handler) {
        if(DEBUG) Log.d(TAG, "Init EthernetManager");
        mService = service;
        mHandler = handler;
    }

    /**
    * Get If Ethernet Obtain Ip Mode Is Configured
    *
    * @return ture for configured, false for not configured
    */
    public boolean isEthernetConfigured() {
        try {
            if(DEBUG) Log.d(TAG, "isEthernetConfigured():" + mService.isEthernetConfigured());
            return mService.isEthernetConfigured();
        } catch (RemoteException e) {
            Log.e(TAG, "isEthernetConfigured()! ERROR!");
            return false;
        }
    }

    /**
    * Get Current Network DhcpInfo
    *
    * @return current network DhcpInfo
    */
    public DhcpInfo getDhcpInfo() {
        try {
            if(DEBUG) Log.d(TAG, "getDhcpInfo():" + mService.getDhcpInfo());
            return mService.getDhcpInfo();
        } catch (RemoteException e) {
            Log.e(TAG, "getDhcpInfo() ERROR!");
            return null;
        }
    }

    /**
    * Save Ethernet Obtain IP Mode And IP Info For Static IP.
    *
    * @param mode
    * ETHERNET_CONNECT_MODE_DHCP
    * ETHERNET_CONNECT_MODE_MANUAL
    * ETHERNET_CONNECT_MODE_PPPOE
    * @param dhcpInfo
    * if mode is ETHERNET_CONNECT_MODE_MANUAL，it is Required.
    */
    public void setEthernetMode(String mode, DhcpInfo dhcpInfo) {
        try {
            if(DEBUG) Log.d(TAG, "setEthernetMode(" + mode + ", " + dhcpInfo + ")");
            mService.setEthernetMode(mode, dhcpInfo);
        } catch (RemoteException e) {
            Log.e(TAG, "setEthernetMode(" + mode + ", " + dhcpInfo + ") ERROR!");
        }
    }

    /**
    * Get Etherent Obtain IP mode
    *
    * @return
    *  ETHERNET_CONNECT_MODE_DHCP
    *  ETHERNET_CONNECT_MODE_MANUAL
    *  ETHERNET_CONNECT_MODE_PPPOE
    *  null for error
    */
    public String getEthernetMode() {
        try {
            if(DEBUG) Log.d(TAG, "getEthernetMode(): " + mService.getEthernetMode());
            return mService.getEthernetMode();
        } catch (RemoteException e) {
            Log.e(TAG, "getEthernetMode() ERROR!");
            return null;
        }
    }

    /**
    * Get Ethernet Interface Name
    *
    * @return Ethernet Interface Name
    */
    public String getInterfaceName() {
        try {
            if(DEBUG) Log.d(TAG, "getDatabaseInterfaceName(): " + mService.getDatabaseInterfaceName());
            return mService.getDatabaseInterfaceName();
        } catch (RemoteException e) {
            Log.e(TAG, "getInterfaceName() read Database ERROR!");
            return null;
        }
    }


    /**
    * Set Ethernet Interface Name
    *
    * @param iface eth0, eth1, etc.
    * @return ture for Success, false for Fail
    */
    public boolean setInterfaceName(String iface) {
        try {
            if(DEBUG) Log.d(TAG, "setInterfaceName(" + iface + ")");
            return mService.setInterfaceName(iface);
        } catch (RemoteException e) {
            Log.e(TAG, "setInterfaceName(" + iface + ") ERROR!");
            return false;
        }
    }

    /**
    * Enable/Disable Ethernet
    * <p>
    *   Disable
    *     dhcp: stop dhcp, clear interface ip
    *     static: clear interface ip
    *   Enable:
    *     start dhcp/static ip and configure interface
    * </p>
    * @param enable ETHERNET_STATE_DISABLED or ETHERNET_STATE_ENABLED
    */
    public void setEthernetEnabled(boolean enable) {
        try {
            if(DEBUG) Log.d(TAG, "setEthernetEnabled(" + enable + ")");
            mService.setEthernetState(enable ? ETHERNET_STATE_ENABLED:ETHERNET_STATE_DISABLED);
        } catch (RemoteException e) {
            Log.e(TAG, "setEthernetEnabled(" + enable + ") ERROR!");
        }
    }

    /**
    * Get Real Time Ethernet State
    * @return
    *  ETHERNET_STATE_DISABLED
    *  ETHERNET_STATE_ENABLED
    *  ETHERNET_STATE_UNKNOWN
    */
    public int getEthernetState() {
        try {
            if(DEBUG) Log.d(TAG, "getEthernetState(): " + mService.getEthernetState());
            return mService.getEthernetState();
        } catch (RemoteException e) {
            Log.e(TAG, "getEthernetState() ETHERNET_STATE_UNKNOWN!");
            return ETHERNET_STATE_UNKNOWN;
        }
    }

    /**
    *  Set Ethernet Disguise Wifi Enable/Disable
    * <p>
    *    If some apk only work through Wifi, please Enable this.
    * </p>
    * @param enable
    */
    public void setWifiDisguise(boolean enable) {
        try {
            if(DEBUG) Log.d(TAG, "setWifiDisguise(" + enable + ")");
            mService.setWifiDisguise(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "setWifiDisguise(" + enable + ") ERROR!");
        }
    }

    /**
    * Get Ethernet Disguise Wifi State
    * @return
    *  WIFI_DISGUISE_ENABLED
    *  WIFI_DISGUISE_DISABLED
    *  WIFI_DISGUISE_STATE_UNKNOWN
    */
    public int getWifiDisguiseState() {
        try {
            if(DEBUG) Log.d(TAG, "getWifiDisguiseState(): " + mService.getWifiDisguiseState());
            return mService.getWifiDisguiseState();
        } catch (RemoteException e) {
            Log.e(TAG, "getWifiDisguiseState() WIFI_DISGUISE_STATE_UNKNOWN!");
            return WIFI_DISGUISE_STATE_UNKNOWN;
        }
    }

    /**
    * Get Default Interface Physical Connection State
    * @return true or false
    */
    public boolean getNetLinkStatus() {
        if(DEBUG) Log.d(TAG, "getNetLinkStatus()" + (getNetLinkStatus(getInterfaceName()) > 0) );
        return getNetLinkStatus(getInterfaceName()) > 0;
    }

    /**
    * Get Physical Connection State
    * @param ifaceName
    * @return 1 for LinkUp, 0 for LinkDown, -1 for No Interface
    */
    public int getNetLinkStatus(String ifaceName) {
        try {
            if(DEBUG) Log.d(TAG, "getNetLinkStatus(" + ifaceName + "): " + mService.getNetLinkStatus(ifaceName));
            return mService.getNetLinkStatus(ifaceName);
        } catch (RemoteException e) {
            Log.e(TAG, "getNetLinkStatus ERROR!");
            return -1;
        }
    }

    /**
    * Set Ethernet Auto Reconnect Option Enable/Disable In Database
    * @param AutoReconnect ethernet auto reconnect option enable/disable
    */
    public void setAutoReconnectState(boolean AutoReconnect) {
        try {
            if(DEBUG) Log.d(TAG, "setAutoReconnectState(" + AutoReconnect + ")");
            mService.setAutoReconnectState(AutoReconnect);
        } catch (RemoteException e) {
            Log.e(TAG, "getAutoReconnectState(" + AutoReconnect + ") ERROR!");
        }
    }

    /**
    * Get Ethernet Auto Reconnect Option State In Database
    * @return ethernet auto reconnect option state saved in database
    */
    public int getAutoReconnectState() {
        try {
            if(DEBUG) Log.d(TAG, "getAutoReconnectState(): " + mService.getAutoReconnectState());
            return mService.getAutoReconnectState();
        } catch (RemoteException e) {
            Log.e(TAG, "getAutoReconnectState() ETHERNET_AUTORECONNECT_STATE_UNKNOWN!");
            return ETHERNET_AUTORECONNECT_STATE_UNKNOWN;
        }
    }

    /**
    * Set Ethernet IPV6 stateless/statefull Auto Change Option Enable/Disable In Database
    * @param auto change funtion enable/disable
    */
    public void setIpv6MOAutoState(boolean MOValid) {
        try {
            if(DEBUG) Log.d(TAG, "setAutoReconnectState(" + MOValid + ")");
            mService.setIpv6MOAutoState(MOValid);
        } catch (RemoteException e) {
            Log.e(TAG, "setIpv6MOAutoState(" + MOValid + ") ERROR!");
        }
    }

    /**
    * Get Ethernet IPv6 stateless/statefull Auto Change Option State In Database
    * @return  option state saved in database
    */
    public int getIpv6MOAutoState() {
        try {
            if(DEBUG) Log.d(TAG, "getIpv6MOAutoState(): " + mService.getIpv6MOAutoState());
            return mService.getIpv6MOAutoState();
        } catch (RemoteException e) {
            Log.e(TAG, "getIpv6MOAutoState() ETHERNET_IPV6MOATUO_STATE_UNKNOWN!");
            return ETHERNET_IPV6MOATUO_STATE_UNKNOWN;
        }
    }

    /**
    * Get Static IP Info Saved In Database
    * @return static ip info saved in database
    */
    public DhcpInfo getSavedEthernetIpInfo() {
        try {
            if(DEBUG) Log.d(TAG, "getSavedEthernetIpInfo(): " + mService.getSavedEthernetIpInfo());
            return mService.getSavedEthernetIpInfo();
        } catch (RemoteException e) {
            Log.e(TAG, "getSavedEthernetIpInfo()! ERROR!");
            return null;
        }
    }

    /**
    * Get All Available Physical Interface Name
    * @return all available physical interface name list
    */
    public String[] getDeviceNameList() {
        try {
            if(DEBUG) Log.d(TAG, "getDeviceNameList() ok");
            return mService.getDeviceNameList();
        } catch (RemoteException e) {
            Log.e(TAG, "getDeviceNameList() ERROR!");
            return null;
        }
    }

    /**
    * Get Ethernet State Saved In Database
    * @return
    *  ETHERNET_STATE_DISABLED
    *  ETHERNET_STATE_ENABLED
    *  ETHERNET_STATE_UNKNOWN
    */
    public int getEthernetPersistedState() {
        try {
            if (DEBUG) Log.d(TAG, "getEthernetPersistedState()");
            return mService.getEthernetPersistedState();
        } catch (RemoteException e) {
            Log.e(TAG, "getEthernetPersistedState() ETHERNET_STATE_UNKNOWN");
            return ETHERNET_STATE_UNKNOWN;
        }
    }

    /**
    * Get IPv4 State, add for chinamobile
    * @return
    *  IPV4_STATE_ENABLED
    *  IPV4_STATE_DISABLED
    *  IPV4_STATE_UNKNOWN
    */
    public int getIpv4PersistedState() {
        try {
            if(DEBUG) Log.d(TAG, "getIpv4PersistedState(): " + mService.getIpv4PersistedState());
            return mService.getIpv4PersistedState();
        } catch (Exception e) {
            Log.e(TAG, "getIpv4PersistedState IPV4_STATE_UNKNOWN");
            return IPV4_STATE_UNKNOWN;
        }
    }


    /**
    * Get All Available Interface Number
    * @return the number of all available interface
    */
    public int getTotalInterface() {
        try {
            if(DEBUG) Log.d(TAG, "getTotalInterface(): " + mService.getTotalInterface());
            return mService.getTotalInterface();
        } catch (RemoteException e) {
            Log.e(TAG, "getTotalInterface() ERROR!");
            return 0;
        }
    }

    /**
    *  Set Ethernet Enable/Disable
    * <p>
    *    Set Interface Up/Down And Start Configue Interface
    * </p>
    * @param enable
    */
    public void enableEthernet(boolean enable) {
        try {
            if(DEBUG) Log.d(TAG, "enableEthernet(" + enable + ")");
            mService.enableEthernet(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "enableEthernet(" + enable + ") ERROR!");
        }
    }


    /**
    * Get Dhcp Option60/Option61 State
    * @return
    *  OPTION60_STATE_DISABLED
    *  OPTION60_STATE_ENABLED
    *  OPTION60_STATE_UNKNOWN
    */
    public int getDhcpOption60State() {
        try {
            if(DEBUG) Log.d(TAG, "getDhcpOption60State(): " + mService.getDhcpOption60State());
            return mService.getDhcpOption60State();
        } catch (RemoteException e) {
            Log.e(TAG, "getDhcpOption60State OPTION60_STATE_UNKNOWN!");
            return OPTION60_STATE_UNKNOWN;
        }
    }

    /**
    * Get Dhcp Option60 VendorID or Option61 username
    * @return vendorid or username string
    */
    public String getDhcpOption60Login() {
        try {
            if(DEBUG) Log.d(TAG, "getDhcpOption60Login(): " + mService.getDhcpOption60Login());
            return mService.getDhcpOption60Login();
        } catch (RemoteException e) {
            Log.e(TAG, "getDhcpOption60Login() read Database ERROR!");
            return null;
        }
    }

    /**
    * Get Dhcp Option61 password
    * <p>
    * if password is null, use option60
    * if password is not null, use option61
    * </p>
    * @return password string
    */
    public String getDhcpOption60Password() {
        try {
            if(DEBUG) Log.d(TAG, "getDhcpOption60Password(): " + mService.getDhcpOption60Password());
            return mService.getDhcpOption60Password();
        } catch (RemoteException e) {
            Log.e(TAG, "getDhcpOption60Password() read Database ERROR!");
            return null;
        }
    }

    /**
    * Set Dhcp Option60/Option61 Info
    * <p>
    * if password is null, use option60
    * if password is not null, use option61
    * </p>
    * @param enable Option60/Option61 Enabled/Disabled
    * @param login Option60 VendorID or Option61 username
    * @param password null for Option60, not null for Option61 password
    */
    public void setDhcpOption60(boolean enable, String login, String password) {
        try {
            if(DEBUG) Log.d(TAG, "setDhcpOption60(" + enable + ", " + login + ", " + password + ")");
            mService.setDhcpOption60(enable, login, password);
        } catch (RemoteException e) {
            Log.e(TAG, "setDhcpOption60(" + enable + ") ERROR!");
        }
    }

    /**
    * Get Dhcp Option125 State
    * @return
    *  OPTION125_STATE_DISABLED
    *  OPTION125_STATE_ENABLED
    *  OPTION125_STATE_UNKNOWN
    */
    public int getDhcpOption125State() {
        try {
            if(DEBUG) Log.d(TAG, "getDhcpOption125State(): " + mService.getDhcpOption125State());
            return mService.getDhcpOption125State();
        } catch (RemoteException e) {
            Log.e(TAG, "getDhcpOption125State OPTION125_STATE_UNKNOWN!");
            return OPTION125_STATE_UNKNOWN;
        }
    }

    /**
    * Get Dhcp Option125 String
    * @return option125 string
    */
    public String getDhcpOption125Info() {
        try {
            if(DEBUG) Log.d(TAG, "getDhcpOption125Info(): " + mService.getDhcpOption125Info());
            return mService.getDhcpOption125Info();
        } catch (RemoteException e) {
            Log.e(TAG, "getDhcpOption125Info() read Database ERROR!");
            return null;
        }
    }

    /**
    * Set Dhcp Option125 Info
    * @param enable Option125 Enabled/Disabled
    * @param option125Info Option125 Info String
    */
    public void setDhcpOption125(boolean enable, String option125Info) {
        try {
            if(DEBUG) Log.d(TAG, "setDhcpOption125(" + enable + ", " + option125Info + ")");
            mService.setDhcpOption125(enable, option125Info);
        } catch (RemoteException e) {
            Log.e(TAG, "setDhcpOption125(" + enable + ") ERROR!");
        }
    }

    /**
    * Set Wifi and Ethernet Coexist Enable/Disable
    * @param enable Wifi and Ethernet Coexist Enabled/Disabled
    */
    public void setWifiEthernetCoexist(boolean setEnable) {
        try {
            if(DEBUG) Log.d(TAG, "setWifiEthernetCoexist(" + setEnable + ")");
            mService.setWifiEthernetCoexist(setEnable);
        } catch (RemoteException e) {
            Log.e(TAG, "setWifiEthernetCoexist(" + setEnable + ")! ERROR!");
        }
    }

    /**
    * Get Wifi and Ethernet Coexist State
    * @return
    *  WIFI_ETHERNET_COEXIST_DISABLED
    *  WIFI_ETHERNET_COEXIST_ENABLED
    *  WIFI_ETHERNET_COEXIST_STATE_UNKNOWN
    */
    public int getWifiEthernetCoexistState() {
        try {
            if(DEBUG) Log.d(TAG, "getWifiEthernetCoexistState" + mService.getWifiEthernetCoexistState());
            return mService.getWifiEthernetCoexistState();
        } catch (RemoteException e) {
            Log.e(TAG, "getWifiEthernetCoexistState ERROR!");
            return WIFI_ETHERNET_COEXIST_STATE_UNKNOWN;
        }
    }


    /**
    * Enable/Disable DHCPV6
    * @param enable DHCPV6 Enabled/Disabled
    */
    public void enableIpv6(boolean enable) {
        try {
            if(DEBUG) Log.d(TAG, "enableIpv6(" + enable + ")");
            mService.enableIpv6(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "enableIpv6(" + enable + ") ERROR!");
        }
    }

    /**
    * Get IPV6 State
    * @return
    *  IPV6_STATE_ENABLED
    *  IPV6_STATE_DISABLED
    *  IPV6_STATE_UNKNOWN
    */
    public int getIpv6PersistedState() {
        try {
            if(DEBUG) Log.d(TAG, "getIpv6PersistedState(): " + mService.getIpv6PersistedState());
            return mService.getIpv6PersistedState();
        } catch (Exception e) {
            Log.e(TAG, "getIpv6PersistedState IPV6_STATE_UNKNOWN");
            return IPV6_STATE_UNKNOWN;
        }
    }

    /**
    * Save Ethernet IPV6 Obtain IP Mode
    * @param mode
    * ETHERNET_CONNECT_MODE_DHCP
    * ETHERNET_CONNECT_MODE_MANUAL
    * ETHERNET_CONNECT_MODE_STATELESS
    * ETHERNET_CONNECT_MODE_PPPOE
    */
    public void setEthernetMode6(String mode) {
        try {
            if(DEBUG) Log.d(TAG, "setEthernetMode6(" + mode + ")");
            mService.setEthernetMode6(mode);
        } catch (RemoteException e) {
            Log.e(TAG, "Can not set Ethernet IPV6 Mode! ERROR!");
        }
    }

    /**
    * Set Ethernet IPV6 Default Obtain IP Mode DHCPV6
    */
    public void setEthernetDefaultConf6() {
        try {
            if(DEBUG) Log.d(TAG, "setEthernetDefaultConf6() DHCP");
            mService.setEthernetMode6(EthernetManager.ETHERNET_CONNECT_MODE_DHCP);
        } catch (RemoteException e) {
            Log.e(TAG, "setEthernetDefaultConf6() ERROR!");
        }
    }

    /**
    * Get Ethernet IPV6 Obtain IP Mode
    */
    public String getEthernetMode6() {
        try {
            if(DEBUG) Log.d(TAG, "getEthernetMode6() Mode:" + mService.getEthernetMode6());
            return mService.getEthernetMode6();
        } catch (RemoteException e) {
            Log.e(TAG, "Can not get Ethernet IPV6 Mode! ERROR!");
            return null;
        }
    }

    /**
    * Save Ethernet IPV6 Ip, Prefixlength, Gateway, Dns1, Dns2 in Database
    * @param ip ipv6 static ip address
    * @param prefixlength ipv6 static ip prefixlength
    * @param gw ipv6 static ip gateway
    * @param dns1 ipv6 static ip dns1
    * @param dns2 ipv6 static ip dns2
    */
    public void setIpv6DatabaseInfo(String ip, int prefixlength, String gw, String dns1, String dns2) {
        try {
            if(DEBUG) Log.d(TAG, "getIpv6DatabaseAddress(" + ip + ", " + prefixlength + ", " + gw + ", " + dns1 + ", " + dns2 + ")");
            mService.setIpv6DatabaseInfo(ip, prefixlength, gw, dns1, dns2);
        } catch (RemoteException e) {
            Log.e(TAG, "setIpv6DatabaseInfo() Error!");
        }
    }

    /**
    * Get Ethernet IPV6 Static Ip Address From Database
    * @return ipv6 static ip address
    */
    public String getIpv6DatabaseAddress() {
        try {
            if(DEBUG) Log.d(TAG, "getIpv6DatabaseAddress():" + mService.getIpv6DatabaseAddress());
            return mService.getIpv6DatabaseAddress();
        } catch (RemoteException e) {
            Log.e(TAG, "getIpv6DatabaseAddress() Error!");
            return null;
        }
    }

    /**
    * Get Ethernet IPV6 Static Ip Prefixlength From Database
    * @return ipv6 static ip prefixlength
    */
    public int getIpv6DatabasePrefixlength() {
        try {
            if(DEBUG) Log.d(TAG, "getIpv6DatabasePrefixlength():" + mService.getIpv6DatabasePrefixlength());
            return mService.getIpv6DatabasePrefixlength();
        } catch (RemoteException e) {
            Log.e(TAG, "getIpv6DatabasePrefixlength() Error!");
            return 0;
        }
    }

    /**
    * Get Ethernet IPV6 Static Ip Dns1 From Database
    * @return ipv6 static ip dns1
    */
    public String getIpv6DatabaseDns1() {
        try {
            if(DEBUG) Log.d(TAG, "getIpv6DatabaseDns1():" + mService.getIpv6DatabaseDns1());
            return mService.getIpv6DatabaseDns1();
        } catch (RemoteException e) {
            Log.e(TAG, "getIpv6DatabaseDns1() Error!");
            return null;
        }
    }

    /**
    * Get Ethernet IPV6 Static Ip Dns2 From Database
    * @return ipv6 static ip dns2
    */
    public String getIpv6DatabaseDns2() {
        try {
            if(DEBUG) Log.d(TAG, "getIpv6DatabaseDns2():" + mService.getIpv6DatabaseDns2());
            return mService.getIpv6DatabaseDns2();
        } catch (RemoteException e) {
            Log.e(TAG, "getIpv6DatabaseDns2() Error!");
            return null;
        }
    }

    /**
    * Get Ethernet IPV6 Static Ip Gateway From Database
    * @return ipv6 static ip gateway
    */
    public String getIpv6DatabaseGateway() {
        try {
            if(DEBUG) Log.d(TAG, "getIpv6DatabaseGateway():" + mService.getIpv6DatabaseGateway());
            return mService.getIpv6DatabaseGateway();
        } catch (RemoteException e) {
            Log.e(TAG, "getIpv6DatabaseGateway() Error!");
            return null;
        }
    }

    /**
    * Get DHCPV6 Proccess Status
    * @param ifname interface name
    * @return
    *  true for finished
    *  false for unfinished
    */
    public boolean checkDhcpv6Status(String ifname) {
        try {
            if(DEBUG) Log.d(TAG, "checkDhcpv6Status(" + ifname +"): " + mService.checkDhcpv6Status(ifname));
            return mService.checkDhcpv6Status(ifname);
        } catch (Exception e) {
            Log.e(TAG ,"checkDhcpv6Status error");
            return false;
        }
    }

    /**
    * Get DHCPV6 Ipaddress
    * @param ifname interface name
    * @return
    *  dhcpv6 ipaddress, null for Error
    */
    public String getDhcpv6Ipaddress(String ifname) {
        try {
            if(DEBUG) Log.d(TAG, "getDhcpv6Ipaddress(" + ifname +"): " + mService.getDhcpv6Ipaddress(ifname));
            return mService.getDhcpv6Ipaddress(ifname);
        } catch (Exception e) {
            Log.e(TAG, "getDhcpv6Ipaddress fail");
            return null;
        }
    }

    public String getIpv6LinklocalAddress(String ifname) {
        try {
            if(DEBUG) Log.d(TAG, "getIpv6LinklocalAddress(" + ifname +"): " + mService.getIpv6LinklocalAddress(ifname));
            return mService.getIpv6LinklocalAddress(ifname);
        } catch (Exception e) {
            Log.e(TAG, "getIpv6LinklocalAddress fail");
            return null;
        }
    }

    /**
    * Get DHCPV6 Gateway
    * @param ifname interface name
    * @return
    *  dhcpv6 gateway address, null for Error
    */
    public String getDhcpv6Gateway() {
        return getDhcpv6Gateway( getInterfaceName() );
    }

    public String getDhcpv6Gateway(String ifname) {
        if(ifname == null){
            if(DEBUG) Log.d(TAG, "getDhcpv6Gateway ifname is null");
            return null;
        }

        try {
            if(DEBUG) Log.d(TAG, "getDhcpv6Gateway(" + ifname + "): " + mService.getDhcpv6Gateway(ifname));
            return mService.getDhcpv6Gateway(ifname);
        } catch (Exception e) {
            Log.e(TAG, "getDhcpv6Gateway fail");
            return null;
        }
    }

    /**
    * Get DHCPV6 Prefixlen
    * @param ifname interface name
    * @return
    *  dhcpv6 prefixlen, null for Error
    */
    public String getDhcpv6Prefixlen(String ifname) {
        try {
            if(DEBUG) Log.d(TAG, "getDhcpv6Prefixlen(" + ifname + "): " + mService.getDhcpv6Prefixlen(ifname));
            return mService.getDhcpv6Prefixlen(ifname);
        } catch (Exception e) {
            Log.e(TAG, "getDhcpv6Prefixlen fail");
            return null;
        }
    }

    /**
    * Get DHCPV6 DNS I
    * @param ifname interface name
    * @param number the number of dns
    * @return
    *  dhcpv6 dns i, null for Error
    */
    public String getDhcpv6Dns(String ifname, int number) {
        try {
            if(DEBUG) Log.d(TAG, "getDhcpv6Dns(" + ifname + ", " + number + "): " + mService.getDhcpv6Dns(ifname, number));
            return mService.getDhcpv6Dns(ifname, number);
        } catch (Exception e) {
            Log.e(TAG, "getDhcpv6Dns fail");
            return null;
        }
    }

    /**
    * Get The Number of DHCPV6 Dnses
    * @param ifname interface name
    * @return
    *  the number of dhcpv6 dnses
    */
    public int getDhcpv6DnsCnt(String ifname) {
        try {
            if(DEBUG) Log.d(TAG, "getDhcpv6DnsCnt(" + ifname + "): " + mService.getDhcpv6DnsCnt(ifname));
            return mService.getDhcpv6DnsCnt(ifname);
        } catch (Exception e) {
            Log.e(TAG, "getDhcpv6DnsCnt fail");
            return 0;
        }
    }

    /**
    * release dhcp lease, when disable ethernet, dhcp lease will be released automatically
    * it also can be released manually
    * @param ifname interface name
    * @return
    *  true if release success, otherwise false
    */
    public boolean releaseDhcpLease(String ifname) {
        try {
            boolean ret = mService.releaseDhcpLease(ifname);
            if(DEBUG) Log.d(TAG, "releaseDhcpLease(" + ifname + "): " + ret);
            return ret;
        } catch (Exception e) {
            Log.e(TAG, "releaseDhcpLease fail");
            return false;
        }
    }

    /**
     * get ethernet stateless ipv6 ip address
     * @return stateless ipv6 ip address
     **/
    public String getStatelessIpv6Address(){
        try {
            if(DEBUG) Log.d(TAG, "getStatelessIpv6Address():" + mService.getStatelessIpv6Address());
            return mService.getStatelessIpv6Address();
        } catch (RemoteException e) {
            Log.e(TAG, "getStatelessIpv6Address() Error!");
            return null;
        }
    }

    /**
    * get ethernet stateless ipv6 ip Gateway
    * @return stateless ipv6 ip Gateway
    **/
    public String getStatelessIpv6Gateway(String ifname){
        try {
            if(DEBUG) Log.d(TAG, "getStatelessIpv6Gateway():" + mService.getStatelessIpv6Gateway(ifname));
            return mService.getStatelessIpv6Gateway(ifname);
        } catch (RemoteException e) {
            Log.e(TAG, "getStatelessGateway() Error!");
            return null;
        }
    }

    /**
     * get ethernet stateless ipv6 ip prefixlength
     * @return stateless ipv6 ip prefixlength
     **/
    public int getStatelessIpv6Prefixlength(){
        try {
            if(DEBUG) Log.d(TAG, "getStatelessIpv6Prefixlength():" + mService.getStatelessIpv6Prefixlength());
            return mService.getStatelessIpv6Prefixlength();
        } catch (RemoteException e) {
            Log.e(TAG, "getStatelessIpv6Prefixlength() Error!");
            return 0;
        }
    }

    /**
     * get ethernet stateless ipv6 ip dns
     * @return stateless ipv6 ip dns
     **/
    public String getStatelessIpv6Dns1(){
        try {
            if(DEBUG) Log.d(TAG, "getStatelessIpv6Dns1():" + mService.getStatelessIpv6Dns1());
            return mService.getStatelessIpv6Dns1();
        } catch (RemoteException e) {
            Log.e(TAG, "getStatelessIpv6Dns1() Error!");
            return null;
        }
    }

    public String getStatelessIpv6Dns2(){
        try {
            if(DEBUG) Log.d(TAG, "getStatelessIpv6Dns2():" + mService.getStatelessIpv6Dns2());
            return mService.getStatelessIpv6Dns2();
        } catch (RemoteException e) {
            Log.e(TAG, "getStatelessIpv6Dns2() Error!");
            return null;
        }
    }
    //add for chinamobile
    private int getIpv4ConnectStatus(){
        try {
            if(DEBUG) Log.d(TAG, "getIpv4ConnectStatus():" + mService.getIpv4ConnectStatus());
            return mService.getIpv4ConnectStatus();
        } catch (RemoteException e) {
            Log.e(TAG, "getIpv4ConnectStatus() Error!");
            return ETHERNET_CONNECT_STATE_UNKNOWN;
        }
    }
    //add for chinamobile
    private int getIpv6ConnectStatus(){
        try {
            if(DEBUG) Log.d(TAG, "getIpv6ConnectStatus():" + mService.getIpv6ConnectStatus());
            return mService.getIpv6ConnectStatus();
        } catch (RemoteException e) {
            Log.e(TAG, "getIpv6ConnectStatus() Error!");
            return ETHERNET_IPV6_CONNECT_STATE_UNKNOWN;
        }
    }

   /**
    * 设置有线IPv4连接模式, add for chinamobile
    *
    * @param config网络信息配置对象
    * （IpAssignment支持DHCP、STATIC、PPPOE、IPOE）
    *
    */
    public void setIpv4Configuration(IpConfiguration config) {
        if(null != config){
            String mode = convertIpAssignmentToEthernetMode(config);
            DhcpInfo dhcpInfo = new DhcpInfo();
            try{
                dhcpInfo = convertIpConfigurationToDhcpInfo(config);
                if(DEBUG){
                    Log.d(TAG, "setIpv4Configuration(): mode="+mode);
                    if(null != dhcpInfo){
                        Log.d(TAG, "setIpv4Configuration(): dhcpInfo="+dhcpInfo.toString());
                    }
                }
            }catch(IllegalArgumentException e){
                Log.e(TAG, "setIpv4Configuration ERROR: Invalid parameter!");
                return;
            }
            setEthernetMode(mode, dhcpInfo);
        }else{
            Log.e(TAG, "setIpv4Configuration ERROR: Invalid parameter!");
        }
    }

    /**
    * 获取当前IPv4网络信息, add for chinamobile
    *
    * @return
    */
    public IpConfiguration getIpv4Configuration() {
        String mode = getEthernetMode();
        DhcpInfo dhcpInfo = getSavedEthernetIpInfo();
        IpConfiguration ipConfiguration = convertDhcpInfoToIpConfiguration(mode, dhcpInfo);
        if(DEBUG) Log.d(TAG, "getIpv4Configuration(): ipConfiguration="+ipConfiguration.toString());
        return ipConfiguration;
    }

    /**
    * 使能ipv4网络状态，并保存设置状态, add for chinamobile
    *
    * @param enable
    * true 开启IPv4连接 恢复原来网络连接
    * false 关闭IP4连接 dhcp模式，停止dhcp获取,并清除网络配置信息
    * static模式，清除网络配置信息
    * pppoe模式，停止pppoe请求，并清除网络配置信息
    * ipoe模式，停止ipoe请求，并清除网络配置信息
    */
    public void enableIpv4(boolean enabled) {
        //setEthernetEnabled(enabled);
        try {
            if(DEBUG) Log.d(TAG, "enableIpv4(" + enabled + ")");
            mService.enableIpv4(enabled);
        } catch (RemoteException e) {
            Log.e(TAG, "enableIpv4(" + enabled + ") ERROR!");
        }
    }

    /**
    * 获取ipv4是否使能, add for chinamobile
    *
    * @return
    */
    public boolean isEnableIpv4() {
        if(DEBUG) Log.d(TAG, "isEnableIpv4() :getIpv4PersistedState()="+getIpv4PersistedState());
        return IPV4_STATE_ENABLED == getIpv4PersistedState();
    }

    /**
    * 连接ipv4网络 不注册网卡，也不改变使能状态, add for chinamobile
    * enable为false时或正在连接时，调用无效
    */
    public void connectIpv4() {
        if(DEBUG) Log.d(TAG, "connectIpv4(): isEnableIpv4()="+isEnableIpv4()+" isIpv4Connecting()="+isIpv4Connecting());
        if(!isEnableIpv4()||isIpv4Connecting()) {
            Log.i(TAG, "connectIpv4() do nothing: isEnableIpv4()="+isEnableIpv4()+" isIpv4Connecting()="+isIpv4Connecting());
            return;
        }
        try {
            if(DEBUG) Log.d(TAG, "connectIpv4()");
            mService.connectIpv4(true);
        } catch (RemoteException e) {
            Log.e(TAG, "connectIpv4() ERROR!");
        }
    }

    /**
    * 断开ipv4网络 不注销网卡，也不改变使能状态, add for chinamobile
    * enable为false时，调用无效
    */
    public void disconnectIpv4() {
        if(!isEnableIpv4()){
            Log.i(TAG, "disconnectIpv4() do nothing: isEnableIpv4()= "+isEnableIpv4());
            return;
        }
        try {
            if(DEBUG) Log.d(TAG, "disconnectIpv4()");
            mService.connectIpv4(false);
        } catch (RemoteException e) {
            Log.e(TAG, "disconnectIpv4() ERROR!");
        }
    }

    /**
    * 获取当前IPv4网络信息(从网卡获取实际IP信息，pppoe时获取pppoe网卡"如ppp0"信息), add for chinamobile
    * @return
    */
    public IpInfo getIPv4Info() {
        int ipv4ConnectStatus = getIpv4ConnectStatus();
        if (ipv4ConnectStatus != ETHERNET_CONNECT_STATE_CONNECT
                && ipv4ConnectStatus != PppoeManager.PPPOE_STATE_CONNECT) {
            Log.i(TAG, "ipv4 is disconnected, ipv4ConnectStatus = " + ipv4ConnectStatus);
            return null;
        }

        IpInfo ipInfo = new IpInfo();
        if(null != getDhcpInfo()){
            DhcpInfo dhcpInfo = getDhcpInfo();
            if(DEBUG) Log.d(TAG, "getIPv4Info() dhcpInfo="+dhcpInfo.toString());
            try{
                ipInfo.ip = NetworkUtils.intToInetAddress(dhcpInfo.ipAddress);
                ipInfo.gateway = NetworkUtils.intToInetAddress(dhcpInfo.gateway);
                ipInfo.mask = NetworkUtils.netmaskIntToPrefixLength(dhcpInfo.netmask);
                ipInfo.dnsServers.add(NetworkUtils.intToInetAddress(dhcpInfo.dns1));
                ipInfo.dnsServers.add(NetworkUtils.intToInetAddress(dhcpInfo.dns2));
            }catch(Exception e){
                Log.e(TAG, "getIPv4Info() ERROR!");
            }
        }else{
            Log.e(TAG, "getIPv4Info() ERROR!");
        }
        if(DEBUG) Log.d(TAG, "getIPv4Info IpInfo=" + ipInfo.toString());
        return ipInfo;
    }

    /**
    * 设置有线IPv6连接模式, add for chinamobile
    *
    * @param config 网络信息配置对象
    * （IpAssignment支持DHCP、STATIC即可）
    *
    */
    public void setIpv6Configuration(IpConfiguration config) {
        if(null != config){
            String mode = convertIpAssignmentToEthernetMode(config);
            StaticIpConfiguration staticIpConfiguration = config.getStaticIpConfiguration();
            if(DEBUG) Log.d(TAG, "setIpv6Configuration(): mode="+mode);
            if(config.ipAssignment == IpConfiguration.IpAssignment.STATIC){
                String ip = "";
                String gateway = "";
                String dns1 = "";
                String dns2 = "";
                int prefixLength = 0;
                try{
                    if (staticIpConfiguration.ipAddress != null) {
                        ip = staticIpConfiguration.ipAddress.getAddress().getHostAddress();
                        prefixLength = staticIpConfiguration.ipAddress.getNetworkPrefixLength();
                    }
                    if (staticIpConfiguration.gateway != null) {
                        gateway = staticIpConfiguration.gateway.getHostAddress();
                    }
                    ArrayList<InetAddress> dnsServers = staticIpConfiguration.dnsServers;
                    if (dnsServers != null && dnsServers.size() > 0) {
                        dns1 = dnsServers.get(0).getHostAddress();
                    }
                    if (dnsServers != null && dnsServers.size() > 1) {
                        dns2 = dnsServers.get(1).getHostAddress();
                    }
                }catch(Exception e){
                    Log.e(TAG, "setIpv6Configuration ERROR: Invalid parameter!");
                    return;
                }
                if(DEBUG) Log.d(TAG, "setIpv6Configuration() :STATIC ip="+ip+" gateway="+gateway+" dns1="+dns1+" dns2"+dns2+" prefixLength="+prefixLength);
                setIpv6DatabaseInfo(ip, prefixLength, gateway, dns1, dns2);
            }
            setEthernetMode6(mode);
        }else{
            Log.e(TAG, "setIpv6Configuration ERROR: Invalid parameter!");
        }
    }

    /**
    * 获取当前IPv6网络信息, add for chinamobile
    *
    * @return
    */
    public IpConfiguration getIpv6Configuration() {
        IpConfiguration config = new IpConfiguration();
        String mode = getEthernetMode6();
        if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL)) {
            config.ipAssignment = IpConfiguration.IpAssignment.STATIC;
            StaticIpConfiguration staticIpConfiguration = new StaticIpConfiguration();
            try{
                staticIpConfiguration.ipAddress = new LinkAddress(NetworkUtils.numericToInetAddress(getIpv6DatabaseAddress()),getIpv6DatabasePrefixlength());
                staticIpConfiguration.gateway = NetworkUtils.numericToInetAddress(getIpv6DatabaseGateway());
                staticIpConfiguration.dnsServers.add(NetworkUtils.numericToInetAddress(getIpv6DatabaseDns1()));
                staticIpConfiguration.dnsServers.add(NetworkUtils.numericToInetAddress(getIpv6DatabaseDns2()));
                Log.i(TAG, "getIpv6Configuration, mode static ipv6, ip:" + getIpv6DatabaseAddress() +
                    ",gw:" + getIpv6DatabaseGateway() + ",prefix:" + getIpv6DatabasePrefixlength() +
                    ", dns1:"+ getIpv6DatabaseDns1() + ", dns2:" + getIpv6DatabaseDns2());
            }catch(Exception e){
                Log.e(TAG, "getIpv6Configuration() ERROR!");
            }
            config.staticIpConfiguration = staticIpConfiguration;
        }else if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_DHCP)){
            //IPOE or DHCP
            if(getDhcpOption60State() == OPTION60_STATE_ENABLED){
                config.ipAssignment = IpConfiguration.IpAssignment.IPOE;
                config.ipoeUserName = getDhcpOption60Login();
                config.ipoePassword = getDhcpOption60Password();
				//add by huxiang for IPOE V6 username and password blank
				config.pppoeUserName = getPppoeUsername();
                config.pppoePassword = getPppoePassword();
				//add by huxiang for IPOE V6 username and password blank
            }else{
                config.ipAssignment = IpConfiguration.IpAssignment.AUTO;
				//add by huxiang for IPOE V6 username and password blank
				config.ipoeUserName = getDhcpOption60Login();
                config.ipoePassword = getDhcpOption60Password();
				config.pppoeUserName = getPppoeUsername();
                config.pppoePassword = getPppoePassword();
				//add by huxiang for IPOE V6 username and password blank
            }
            config.staticIpConfiguration = null;
        } else if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_STATELESS)){
            config.ipAssignment = IpConfiguration.IpAssignment.AUTO;
			//add by huxiang for IPOE V6 username and password blank
			 config.ipoeUserName = getDhcpOption60Login();
             config.ipoePassword = getDhcpOption60Password();
			 config.pppoeUserName = getPppoeUsername();
             config.pppoePassword = getPppoePassword();
			 //add by huxiang for IPOE V6 username and password blank
            config.staticIpConfiguration = null;
        } else if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE)){
            config.ipAssignment = IpConfiguration.IpAssignment.PPPOE;
            config.pppoeUserName = getPppoeUsername();
            config.pppoePassword = getPppoePassword();
			//add by huxiang for IPOE V6 username and password blank
			config.ipoeUserName = getDhcpOption60Login();
            config.ipoePassword = getDhcpOption60Password();
			//add by huxiang for IPOE V6 username and password blank
            config.staticIpConfiguration = null;
        }else if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_NONE)){
            config.ipAssignment = IpConfiguration.IpAssignment.UNASSIGNED;
        }
		
        Log.d(TAG, "getIpv6Configuration(): IpConfiguration="+config);
		Log.d(TAG, "getIpv6Configuration(): ipoeUserName="+config.ipoeUserName+"ipoePassword="+config.ipoePassword);
        return config;
    }

    /**
    * 使能ipv6 网络状态, already have before
    *
    * @param enable
    * true 开启IPv6连接 恢复原来网络连接
    * false 关闭IPv6连接 dhcp模式，停止dhcp获取,并清除网络配置信息
    * static模式，清除网络配置信息
    */
    //public void enableIpv6(boolean enabled) {

    //}

    /**
    * 获取ipv6是否使能, add for chinamobile
    *
    * @return
    */
    public boolean isEnableIpv6() {
        if(DEBUG) Log.d(TAG, "isEnableIpv6() :getIpv6PersistedState()="+getIpv6PersistedState());
        return IPV6_STATE_ENABLED == getIpv6PersistedState();
    }

    /**
    * 连接ipv6网络 不注册网卡 ，也不改变使能状态, add for chinamobile
    *
    * enable为false时或正在连接时，调用无效
    */
    public void connectIpv6() {
        if(DEBUG) Log.d(TAG, "connectIpv6(): isEnableIpv6()="+isEnableIpv6()+" isIpv6Connecting()= "+isIpv6Connecting());
        if(!isEnableIpv6() || isIpv6Connecting()){
            Log.i(TAG, "connectIpv6() do nothing: isEnableIpv6()="+isEnableIpv6()+" isIpv6Connecting()= "+isIpv6Connecting());
            return;
        }
        try {
            if(DEBUG) Log.d(TAG, "connectIpv6()");
            mService.connectIpv6(true);
        } catch (RemoteException e) {
            Log.e(TAG, "connectIpv6() ERROR!");
        }
    }

    /**
    * 断开ipv6网络 不注销网卡，也不改变使能状态, add for chinamobile
    *
    * enable为false时，调用无效
    */
    public void disconnectIpv6() {
        if(!isEnableIpv6()){
            Log.i(TAG, "disconnectIpv6() do nothing: isEnableIpv6()= "+isEnableIpv6());
            return;
        }
        try {
            if(DEBUG) Log.d(TAG, "disconnectIpv6()");
            mService.connectIpv6(false);
        } catch (RemoteException e) {
            Log.e(TAG, "disconnectIpv6() ERROR!");
        }
    }

    /**
    * 获取当前IPv6网络信息(从网卡获取实际IP信息), add for chinamobile
    *
    * @return
    */
    public IpInfo getIPv6Info() {
        int ipv6ConnectStatus = getIpv6ConnectStatus();
        if (ipv6ConnectStatus != ETHERNET_IPV6_CONNECT_STATE_CONNECT) {
            Log.i(TAG, "ipv6 is disconnected, ipv6ConnectStatus = " + ipv6ConnectStatus);
            return null;
        }

        IpInfo ipInfo = new IpInfo();
        if(ETHERNET_CONNECT_MODE_DHCP.equals(getEthernetMode6()) ) {
            String iface = getInterfaceName();
            try{
                //ipInfo.ip = InetAddress.getByName(getDhcpv6Ipaddress(iface));
                ipInfo.ip = NetworkUtils.numericToInetAddress(getDhcpv6Ipaddress(iface));
                ipInfo.gateway = NetworkUtils.numericToInetAddress(getDhcpv6Gateway());
                if(null != getDhcpv6Prefixlen(iface)){
                    ipInfo.mask = Integer.parseInt(getDhcpv6Prefixlen(iface));
                    //ipInfo.mask = NetworkUtils.prefixLengthToNetmaskInt(Integer.parseInt(getDhcpv6Prefixlen(iface)))
                }
                ipInfo.dnsServers.add(NetworkUtils.numericToInetAddress(getDhcpv6Dns(iface, 1)));
                ipInfo.dnsServers.add(NetworkUtils.numericToInetAddress(getDhcpv6Dns(iface, 2)));
            }catch(Exception e){
                Log.e(TAG, "getIPv6Info() ERROR");
            }
            Log.i(TAG, "getIPv6Info from iface,mode dhcp  ip:" + getDhcpv6Ipaddress(iface) +
                ",gw:" + getDhcpv6Gateway() + ",prefix:" + getDhcpv6Prefixlen(iface) +
                ", dns1:"+ getDhcpv6Dns(iface, 1) + ", dns2:" + getDhcpv6Dns(iface, 2));
       }else if(EthernetManager.ETHERNET_CONNECT_MODE_STATELESS.equals(getEthernetMode6())) {
            try{
                ipInfo.ip = NetworkUtils.numericToInetAddress(getStatelessIpv6Address());
                ipInfo.gateway = NetworkUtils.numericToInetAddress(getDhcpv6Gateway());
                ipInfo.mask = getStatelessIpv6Prefixlength();
                ipInfo.dnsServers.add(NetworkUtils.numericToInetAddress(getStatelessIpv6Dns1()));
                ipInfo.dnsServers.add(NetworkUtils.numericToInetAddress(getStatelessIpv6Dns2()));
            }catch(Exception e){
                Log.e(TAG, "getIPv6Info() ERROR");
            }

            Log.i(TAG, "getIPv6Info, mode stateless, ip:" + getStatelessIpv6Address() +
               ",gw:" + getDhcpv6Gateway() + ",prefix:" + getStatelessIpv6Prefixlength() +
               ", dns1:"+ getStatelessIpv6Dns1() + ", dns2:" + getStatelessIpv6Dns2());
        } else if(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(getEthernetMode6())) {
            String iface = "ppp0";
            if(isIpv6PppoeStateless()) { //pppoe stateless
                try{
                    ipInfo.ip = NetworkUtils.numericToInetAddress(getStatelessIpv6Address());
                    ipInfo.gateway = NetworkUtils.numericToInetAddress(getDhcpv6Gateway(iface));
                    ipInfo.mask = getStatelessIpv6Prefixlength();
                    ipInfo.dnsServers.add(NetworkUtils.numericToInetAddress(getStatelessIpv6Dns1()));
                    ipInfo.dnsServers.add(NetworkUtils.numericToInetAddress(getStatelessIpv6Dns2()));
                }catch(Exception e){
                    Log.e(TAG, "getIPv6Info() ERROR");
                }

                Log.i(TAG, "getIPv6Info, mode pppoe stateless, ip:" + getStatelessIpv6Address() +
                   ",gw:" + getDhcpv6Gateway(iface) + ",prefix:" + getStatelessIpv6Prefixlength() +
                   ", dns1:"+ getStatelessIpv6Dns1() + ", dns2:" + getStatelessIpv6Dns2());
           } else { //pppoe stateful
                try{
                    ipInfo.ip = NetworkUtils.numericToInetAddress(getDhcpv6Ipaddress(iface));
                    ipInfo.gateway = NetworkUtils.numericToInetAddress(getDhcpv6Gateway(iface));
                    if(null != getDhcpv6Prefixlen(iface)){
                        ipInfo.mask = Integer.parseInt(getDhcpv6Prefixlen(iface));
                    }
                    ipInfo.dnsServers.add(NetworkUtils.numericToInetAddress(getDhcpv6Dns(iface, 1)));
                    ipInfo.dnsServers.add(NetworkUtils.numericToInetAddress(getDhcpv6Dns(iface, 2)));
                }catch(Exception e){
                    Log.e(TAG, "getIPv6Info() ERROR");
                }
                Log.i(TAG, "getIPv6Info from iface,mode pppoe dhcp  ip:" + getDhcpv6Ipaddress(iface) +
                    ",gw:" + getDhcpv6Gateway(iface) + ",prefix:" + getDhcpv6Prefixlen(iface) +
                    ", dns1:"+ getDhcpv6Dns(iface, 1) + ", dns2:" + getDhcpv6Dns(iface, 2));
                }
	   } else {
            try{
                ipInfo.ip = NetworkUtils.numericToInetAddress(getIpv6DatabaseAddress());
                ipInfo.gateway = NetworkUtils.numericToInetAddress(getIpv6DatabaseGateway());
                ipInfo.mask = getIpv6DatabasePrefixlength();
                ipInfo.dnsServers.add(NetworkUtils.numericToInetAddress(getIpv6DatabaseDns1()));
                ipInfo.dnsServers.add(NetworkUtils.numericToInetAddress(getIpv6DatabaseDns2()));
            }catch(Exception e){
                Log.e(TAG, "getIPv6Info() ERROR");
            }
            Log.i(TAG, "show_ipv6Info, mode static, ip:" + getIpv6DatabaseAddress() +
                ",gw:" + getIpv6DatabaseGateway() + ",prefix:" + getIpv6DatabasePrefixlength() +
                ", dns1:"+ getIpv6DatabaseDns1() + ", dns2:" + getIpv6DatabaseDns2());
       }
        if(DEBUG) Log.d(TAG, "getIPv6Info IpInfo=" + ipInfo.toString());
        return ipInfo;
    }

    /**
    * convert IpConfiguration to DhcpInfo, add for chinamobile
    *
    * @return DhcpInfo
    */
    private DhcpInfo convertIpConfigurationToDhcpInfo(IpConfiguration config) throws IllegalArgumentException{
        if(config.ipAssignment != IpConfiguration.IpAssignment.STATIC){
            return null;
        }
        StaticIpConfiguration staticIpConfiguration = config.getStaticIpConfiguration();
        DhcpInfo dhcpInfo = new DhcpInfo();
        InetAddress ipAddress = NetworkUtils.numericToInetAddress("");
        InetAddress gateway = NetworkUtils.numericToInetAddress("");
        InetAddress dns1 = NetworkUtils.numericToInetAddress("");
        InetAddress dns2 = NetworkUtils.numericToInetAddress("");
        int prefixLength = 0;
        if(null != staticIpConfiguration){
            if(null != staticIpConfiguration.ipAddress){
                if(null != staticIpConfiguration.ipAddress.getAddress())
                    ipAddress = staticIpConfiguration.ipAddress.getAddress();
                prefixLength=staticIpConfiguration.ipAddress.getNetworkPrefixLength();
            }
            if(null != staticIpConfiguration.gateway)
                gateway = staticIpConfiguration.gateway;
            if(staticIpConfiguration.dnsServers.size()>=1)
                dns1 = staticIpConfiguration.dnsServers.get(0);
            if(staticIpConfiguration.dnsServers.size()>=2)
                dns2 = staticIpConfiguration.dnsServers.get(1);
        }
        try{
            dhcpInfo.ipAddress = NetworkUtils.inetAddressToInt(ipAddress);
            dhcpInfo.gateway = NetworkUtils.inetAddressToInt(gateway);
            dhcpInfo.dns1 = NetworkUtils.inetAddressToInt(dns1);
            dhcpInfo.dns2 = NetworkUtils.inetAddressToInt(dns2);
            dhcpInfo.netmask = NetworkUtils.prefixLengthToNetmaskInt(prefixLength);
        }catch(IllegalArgumentException e){
            throw new IllegalArgumentException("Invalid parameter");
        }
        return dhcpInfo;
    }

    /**
    * convert DhcpInfo to IpConfiguration, add for chinamobile
    *
    * @return IpConfiguration
    */
    private IpConfiguration convertDhcpInfoToIpConfiguration(String mode,DhcpInfo dhcpInfo){
        IpConfiguration config = new IpConfiguration();
        if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL)) {
            if (null == dhcpInfo)
                return null;
            config.ipAssignment = IpConfiguration.IpAssignment.STATIC;
            StaticIpConfiguration staticIpConfiguration = new StaticIpConfiguration();
            int prefixLength = NetworkUtils.netmaskIntToPrefixLength(dhcpInfo.netmask);
            LinkAddress linkAddr = new LinkAddress(NetworkUtils.intToInetAddress(dhcpInfo.ipAddress),prefixLength);
            if (linkAddr.getAddress() instanceof Inet4Address ) {
                staticIpConfiguration.ipAddress = linkAddr;
            }else{
                return null;//TODO need to check if return ok?
            }
            staticIpConfiguration.gateway = NetworkUtils.intToInetAddress(dhcpInfo.gateway);
            staticIpConfiguration.dnsServers.add(NetworkUtils.intToInetAddress(dhcpInfo.dns1));
            staticIpConfiguration.dnsServers.add(NetworkUtils.intToInetAddress(dhcpInfo.dns2));
            config.staticIpConfiguration = staticIpConfiguration;
        }else if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_DHCP)){
            //IPOE or DHCP
            if(getDhcpOption60State() == OPTION60_STATE_ENABLED){
                config.ipAssignment = IpConfiguration.IpAssignment.IPOE;
                config.ipoeUserName = getDhcpOption60Login();
                config.ipoePassword = getDhcpOption60Password();
            }else{
                config.ipAssignment = IpConfiguration.IpAssignment.DHCP;
            }
            config.staticIpConfiguration = null;
        } else if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE)){
            config.ipAssignment = IpConfiguration.IpAssignment.PPPOE;
            config.pppoeUserName = getPppoeUsername();
            config.pppoePassword = getPppoePassword();
            config.staticIpConfiguration = null;
        }else if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_NONE)){
            config.ipAssignment = IpConfiguration.IpAssignment.UNASSIGNED;
        }
        return config;
    }

    /**
    * convert IpAssignment to EthernetMode, add for chinamobile
    *
    * @return EthernetMode
    */
    public String convertIpAssignmentToEthernetMode(IpConfiguration config) {
        switch(config.ipAssignment){
            case DHCP:
            {
                setDhcpOption60(false, getDhcpOption60Login(), getDhcpOption60Password());
                return EthernetManager.ETHERNET_CONNECT_MODE_DHCP;
            }
            case IPOE:
            {
                setDhcpOption60(true, config.ipoeUserName, config.ipoePassword);
                return EthernetManager.ETHERNET_CONNECT_MODE_DHCP;
            }
            case STATIC:
            {
                return EthernetManager.ETHERNET_CONNECT_MODE_MANUAL;
            }
            case PPPOE:
            {
                setPppoeUsername(config.pppoeUserName);
                setPppoePassword(config.pppoePassword);
                return EthernetManager.ETHERNET_CONNECT_MODE_PPPOE;
            }
            case UNASSIGNED:
            {
                return EthernetManager.ETHERNET_CONNECT_MODE_NONE;
            }
            case AUTO:
            {
                //default set to STATELESS,auto turn to dhcpv6(if not connected after 5s)
                return EthernetManager.ETHERNET_CONNECT_MODE_STATELESS;
            }
            default:
                return EthernetManager.ETHERNET_CONNECT_MODE_DHCP;
        }
    }

    /**
    * get is ipv4 on connecting, add for chinamobile
    *
    * @return is ipv4 on connecting
    */
    private boolean isIpv4Connecting(){
        if(EthernetManager.ETHERNET_CONNECT_STATE_CONNECTING == getIpv4ConnectStatus() ||
            PppoeManager.PPPOE_STATE_CONNECTING == getIpv4ConnectStatus()){
            return true;
        }else{
            return false;
        }
    }

    /**
    * get is ipv6 on connecting, add for chinamobile
    *
    * @return is ipv6 on connecting
    */
    private boolean isIpv6Connecting(){
        if(EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECTING == getIpv6ConnectStatus()){
            return true;
        }else{
            return false;
        }
    }

    /**
    * Set Pppoe Login Username
    * @param Username pppoe login username
    */
    public void setPppoeUsername(String Username) {
        try {
            if(DEBUG) Log.d(TAG, "setPppoeUsername(" + Username + ")");
            mService.setPppoeUsername(Username);
        } catch (RemoteException e) {
            Log.e(TAG, "setPppoeUsername ERROR!");
        }
    }

    /**
    * Set Pppoe Login Password
    * @param Password pppoe login password
    */
    public void setPppoePassword(String Password) {
        try {
            if(DEBUG) Log.d(TAG, "setPppoePassword(" + Password + ")");
            mService.setPppoePassword(Password);
        } catch (RemoteException e) {
            Log.e(TAG, "setPppoePassword ERROR!");
        }
    }

    /**
    * Get pppoe username
    * @return pppoe username
    */
    public String getPppoeUsername() {
        try {
            if(DEBUG) Log.d(TAG, "getPppoeUsername(): " + mService.getPppoeUsername());
            return mService.getPppoeUsername();
        } catch (RemoteException e) {
            Log.e(TAG, "getPppoeUsername() read Database ERROR!");
            return null;
        }
    }

    /**
    * Get pppoe password
    * @return password string
    */
    public String getPppoePassword() {
        try {
            if(DEBUG) Log.d(TAG, "getPppoePassword(): " + mService.getPppoePassword());
            return mService.getPppoePassword();
        } catch (RemoteException e) {
            Log.e(TAG, "getPppoePassword() read Database ERROR!");
            return null;
        }
    }

    /**
    * Enable/Disable PPPOE ipv6 stateless
    * @param enable ipv6 PPPoe stateless Enabled/Disabled
    */
    public void setIpv6PppoeStateless(boolean enable){
        try {
            if(DEBUG) Log.d(TAG, "setIpv6PppoeStateless(" + enable + ")");
            mService.setIpv6PppoeStateless(enable);
        } catch (RemoteException e) {
            Log.e(TAG, "setIpv6PppoeStateless(" + enable + ") ERROR!");
        }
    }
    /**
    * Get If Ipv6 pppoe is stateless mode
    *
    * @return ture for stateless, false for not stateful
    */
    public boolean isIpv6PppoeStateless(){
        try {
            if(DEBUG) Log.d(TAG, "isIpv6PppoeStateless():" + mService.isIpv6PppoeStateless());
            return mService.isIpv6PppoeStateless();
        } catch (RemoteException e) {
            Log.e(TAG, "isIpv6PppoeStateless()! ERROR!");
            return false;
        }
    }
	
	 //add by guangchao.su
    /**
    * Get the selected network interface info
    */
    public void disconnect() {
        try {
            mService.disconnect();
        }
         catch (RemoteException e) {
            Log.e(TAG, "disconnect failed!");
        }
    }
    public static synchronized EthernetManager getInstance() {
        if (mEthManager == null)
            mEthManager = new EthernetManager();
        return mEthManager;
    }
    private EthernetManager() {
        IBinder b = ServiceManager.getService(Context.ETHERNET_SERVICE);
        if (mService == null)
            mService = IEthernetManager.Stub.asInterface(b);
    }
}
