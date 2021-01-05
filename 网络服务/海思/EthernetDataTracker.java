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

package android.net;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.net.UnknownHostException;
import android.net.LinkProperties.CompareResult;
import android.app.NotificationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import com.android.server.net.BaseNetworkObserver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.net.pppoe.PppoeManager;
import android.net.pppoe.PppoeNative;
import android.net.ethernet.EthernetManager;
import android.net.ethernet.HiEthernetManager;
import android.net.ethernet.EthernetNative;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.webkit.ProxyParser;
// only for fast boot Pro
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.provider.Settings;
import android.net.ethernet.EthernetManager;
import android.os.SystemProperties;
//add for stateless IPV6
import java.lang.Integer;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import android.os.SystemClock;
import android.net.ethernet.EthernetManager;
/**
 * This class tracks the data connection associated with Ethernet
 * This is a singleton class and an instance will be created by
 * ConnectivityService.
 * @hide
 */
public class EthernetDataTracker extends BaseNetworkStateTracker {
    private static final String NETWORKTYPE = "ETHERNET";
    private static final String TAG = "EthernetDataTracker";
    private static final String TAG_SU = "EthernetDataTracker_SU";
    private static final String IPV6_PPP_RESULT = "pppoe.ipv6.result";

    private static int       moFlags         = 0x40;
    private static final int IF_RA_OTHERCONF = 0x80;
    private static final int IF_RA_MANAGED   = 0x40;
    private static final int IFA_F_DADFAILED = 0x08;

    private AtomicBoolean mTeardownRequested = new AtomicBoolean(false);
    private AtomicBoolean mPrivateDnsRouteSet = new AtomicBoolean(false);
    private AtomicInteger mDefaultGatewayAddr = new AtomicInteger(0);
    private AtomicBoolean mDefaultRouteSet = new AtomicBoolean(false);

    private static final int  REMOVE_IPV4_LINK = 0x8801;
    private static final int  REMOVE_IPV6_LINK = 0x8802;

    private static boolean mLinkUp;
    private static boolean pLinkUp;
    private static int mConnectStatus;
    private static int mIpv6ConnectStatus;

    private LinkProperties mLinkProperties;
    private LinkProperties mIpv4LinkProperties;
    private LinkCapabilities mLinkCapabilities;
    private NetworkInfo mNetworkInfo;
    private InterfaceObserver mInterfaceObserver;
    private String mHwAddr;

    /* For sending events to connectivity service handler */
    private Handler mCsHandler;
    private Context mContext;

    private static EthernetDataTracker sInstance;
    private static String sIfaceMatch = "";

    private static String[] mEthernetInterfaceNameList;
    private static String mIface = "";
    private static String pIface = "ppp0";

    private static INetworkManagementService mNetworkManagementService;
    private static IBinder mNetworkManagementServiceBinder;
    private INetworkManagementService mNMService;

    private static EthernetManager mEthernetManager;
    private static HiEthernetManager mHiEthernetManager;
    private static PppoeManager mPppoeManager;
    private static ConnectivityManager mConnectivityManager;
    private static DhcpInfo mDhcpInfo;
    private static NotificationManager mNotificationManager;

    private Collection<InetAddress> ipv6LastDns;
    private Collection<InetAddress> ipv4LastDns;

    /*add for stateless ip address*/
    private static String mStatelessIpAddress = "";
    private static String mStatelessIpv6Dns1 = "";
    private static String mStatelessIpv6Dns2 = "";
    private static int mStatelessPrefixlength = 0;
    /*add for pppoe stateless ip address*/
    private static String mPPPoeStatelessIpAddress = "";
    private static String mPPPoeStatelessIpv6Dns1 = "";
    private static String mPPPoeStatelessIpv6Dns2 = "";
    private static int mPPPoeStatelessPrefixlength = 0;
    private static final String PPPOE_CONNECT_ONLY_PPPOE_IPV6 = "118";

    private static boolean DEBUG = false;
    private static boolean userGestureIpv6Stateless = false;
    private static boolean mLinklocal6ProbeRunning = false;
    private static boolean isCMCCbaseMode = false; //china mobile base mode is true, other mode is false;
    private static long mDhcpv4StartTime = -1;
    private static Timer mDhcpv6DelayTimer = null;
    private static HashSet<String> ipv6AddrsUpdate = new HashSet<String>();
    private static class InterfaceObserver extends BaseNetworkObserver {
        private EthernetDataTracker mTracker;

        InterfaceObserver(EthernetDataTracker tracker) {
            super();
            mTracker = tracker;
        }

        public void interfaceStatusChanged(String iface, boolean up) {
            Log.d(TAG, "Interface status changed: " + iface + (up ? "up" : "down"));
        }

        public void interfaceLinkStateChanged(String iface, boolean up) {
            if(DEBUG) Log.d(TAG, "interfaceLinkStateChanged Interface " + iface + " link " + (up ? "up" : "down"));
            if(DEBUG) Log.d(TAG, "mIface: " + mIface + " mLinkUp: " + mLinkUp);
            if(mIface.equals(iface)) mLinkUp = up;
            if(pIface.equals(iface)) pLinkUp = up;
            Log.d(TAG, "Interface " + iface + " link " + (up ? "up" : "down"));
            //begin:add by zengzhiliang at 20200403:gui zhou fix can not DHCP IP
            if(SystemProperties.get("ro.ysten.province","master").equals("cm201_guizhou")&&up){
	           		SystemProperties.set("sys.yst.ipoe_enable","true");
            }
            //end:add by zengzhiliang at 20200403:gui zhou fix can not DHCP IP
            if (mLinkUp == true && pLinkUp == false && (mConnectStatus == PppoeManager.PPPOE_STATE_CONNECT)
            && EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mEthernetManager.getEthernetMode()) ) {//Server disconnect
                Log.i(TAG, "Server disconnect, send PppoeManager.EVENT_CONNECT_FAILED event!");
                mTracker.postNotification(PppoeManager.PPPOE_STATE_CHANGED_ACTION,
                                 PppoeManager.EXTRA_PPPOE_STATE,
                                 PppoeManager.EVENT_CONNECT_FAILED,
                                 PppoeManager.EXTRA_PPPOE_ERRMSG,
                                 mPppoeManager.getErrorMessage(pIface));
                mTracker.postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_IPV4_CONNECT_FAILED,
                                 null,null);
                mTracker.stopInterface();
                mTracker.pppoeAutoConnect();
                return;
            }

            if(mLinkUp == true && mIface.equals(iface)) {
                /*if(mConnectStatus != EthernetManager.ETHERNET_CONNECT_STATE_CONNECT
                       && mConnectStatus != PppoeManager.PPPOE_STATE_CONNECT) {
                    Log.i(TAG, "PHY Link Up, send EthernetManager.EVENT_PHY_LINK_UP");
                    mTracker.postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                                              EthernetManager.EXTRA_ETHERNET_STATE,
                                              EthernetManager.EVENT_PHY_LINK_UP,
                                              null,null);
                    mTracker.postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                              EthernetManager.EXTRA_ETHERNET_STATE,
                                              EthernetManager.EVENT_PHY_LINK_UP,
                                              null,null);
                    mTracker.resetInterface();
                    mTracker.startIpv6();
                }*/
		updateInterfaceUpState();
            } else if(mLinkUp == false && mIface.equals(iface)) {
                Log.i(TAG, "PHY Link Down, send EthernetManager.EVENT_PHY_LINK_DOWN");
                mTracker.postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                                          EthernetManager.EXTRA_ETHERNET_STATE,
                                          EthernetManager.EVENT_PHY_LINK_DOWN,
                                          null, null);
                if(mConnectStatus == EthernetManager.ETHERNET_CONNECT_STATE_CONNECT
                       || mConnectStatus == PppoeManager.PPPOE_STATE_CONNECT
                       || mConnectStatus == PppoeManager.PPPOE_STATE_CONNECTING)
                    mTracker.stopInterface();
                mTracker.stopIpv6();
            }
        }

        private void updateInterfaceUpState() {
            if (mConnectStatus != EthernetManager.ETHERNET_CONNECT_STATE_CONNECT
                    && mConnectStatus != EthernetManager.ETHERNET_CONNECT_STATE_CONNECTING
                    && mConnectStatus != PppoeManager.PPPOE_STATE_CONNECT
                    && mConnectStatus != PppoeManager.PPPOE_CONNECT_RESULT_CONNECTING) {
                Log.i(TAG, "PHY Link Up v4, send EthernetManager.EVENT_PHY_LINK_UP");
                mTracker.postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                        EthernetManager.EXTRA_ETHERNET_STATE,
                        EthernetManager.EVENT_PHY_LINK_UP,
                        null, null);
                mTracker.resetInterface();
            }
            if (mIpv6ConnectStatus != EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT
                    && mIpv6ConnectStatus != EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECTING) {
                Log.i(TAG, "PHY Link Up v6, send EthernetManager.EVENT_PHY_LINK_UP");
                mTracker.postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                        EthernetManager.EXTRA_ETHERNET_STATE,
                        EthernetManager.EVENT_PHY_LINK_UP,
                        null, null);
                mTracker.startIpv6();
            }
        }

        public void interfaceAdded(String iface) {
            mTracker.interfaceAdded(iface);
        }

        public void interfaceRemoved(String iface) {
            mTracker.interfaceRemoved(iface);
        }

        /**
        * ip address updated
        *
        * we can judge ip conflict by flag
        * @param flags
        *     IFA_F_SECONDARY       0x01
        *     IFA_F_TEMPORARY       IFA_F_SECONDARY
        *     IFA_F_NODAD           0x02
        *     IFA_F_OPTIMISTIC      0x04
        *     IFA_F_DADFAILED       0x08    means ip conflict
        *     IFA_F_HOMEADDRESS     0x10
        *     IFA_F_DEPRECATED      0x20
        *     IFA_F_TENTATIVE       0x40
        *     IFA_F_PERMANENT       0x80
        *     IFA_F_MANAGETEMPADDR  0x100
        *     IFA_F_NOPREFIXROUTE   0x200
        */
        public void addressUpdated(String address, String iface, int flags, int scope){
            // default no-op
            LinkAddress targetAddress = new LinkAddress(address);
            if(DEBUG) Log.d(TAG,"-----------ifcae: " + iface + " addressupdate address: " + address + " flags = "+flags);
            if ((mIface.equals(iface) || "ppp0".equals(iface))
                    && (targetAddress.getAddress() instanceof Inet6Address)) {
                if (DEBUG) Log.d(TAG,"ipv6AddrsUpdate add address: " + address);
                ipv6AddrsUpdate.add(address);
            }
            if(mIface.equals(iface) && targetAddress.getAddress() instanceof Inet6Address) {
                String[] addrs = address.split("/");
                if ( addrs.length < 2 )
                    return;

                //Judging ip conflict by flag
                if((flags & IFA_F_DADFAILED) != 0){
                    //ipconflict
                    if(DEBUG) Log.d(TAG,"ipv6 ip conflict");
                    String action = "android.net.conn.IPV6_ADDRESS_CONFLICTED";
                    String key = "ipv6_addr";
                    mTracker.postNotification(action, null, 0, key, address);
                }

                //stateless IP Address dispose
                //tmp stateless address 2404:1a8:f007:a:f028:34ff:fed1:6f4a
                //              or             2000::2200:207:63ff:fe83:212/64
                //(length - 7) need is ff:fe
                String statelessPrefixStr = addrs[1];
                address = (addrs[0].split("%"))[0];
                String tmpaddress = address.substring(0,address.lastIndexOf(":"));
                int len = tmpaddress.length();
                if( tmpaddress.regionMatches(true,(len-7),"ff:fe",0,5) && !tmpaddress.startsWith("fe80:") ) {
                    mStatelessIpAddress = address;

                    try {
                        mStatelessPrefixlength = Integer.parseInt(statelessPrefixStr);
                    } catch (Exception e) {
                        Log.e(TAG,"Integer.parseInt throw exception" + e);
                        mStatelessPrefixlength = 64;
                    }
                    if(EthernetManager.ETHERNET_CONNECT_MODE_STATELESS.equals(mEthernetManager.getEthernetMode6())) {/*if Settings don't need stateless info, don't get it */
                        if(!userGestureIpv6Stateless){/*kernel may sent stateless addressUpdate frequently, just handle user control*/
                            return;
                        }

                        mTracker.setStatelessv6LinkProperties(mIface);// do here,because this is stateless first know
                        mTracker.getIpv6StalessDns();

                        mIpv6ConnectStatus = EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT;
                        if(isCMCCbaseMode) {
                            mTracker.postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION, //stateless get ip broadcast
                                             EthernetManager.EXTRA_ETHERNET_STATE,
                                             EthernetManager.EVENT_IPV6_CONNECT_SUCCESSED,
                                             null, null);
                        } else {
                            mTracker.postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION, //stateless get ip broadcast
                                             EthernetManager.EXTRA_ETHERNET_STATE,
                                             EthernetManager.EVENT_STATELESS6_CONNECT_SUCCESSED,
                                             null, null);
                        }

                        userGestureIpv6Stateless = false;
                        Log.d(TAG, "EVENT_STATELESS6_CONNECT_SUCCESSED #2" );
                    }
                    Log.d(TAG,"--------stateless Address: " + mStatelessIpAddress + " Prefixlength:" + mStatelessPrefixlength);
                }
            }
            Log.d(TAG,"--------ppp0: "+ mEthernetManager.getEthernetMode6());
            if( EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mEthernetManager.getEthernetMode6())
                                && iface.equals("ppp0") && targetAddress.getAddress() instanceof Inet6Address ) {/*pppoe ipv6 function*/
                if (!mEthernetManager.isIpv6PppoeStateless()){//use statefull mode
                    if ( address.startsWith("fe80:") ){
                        Log.d(TAG,"--------ppp0 run pppoe dhcpv6" );
                        mTracker.runPPPoeDhcpv6();//ask for statefull address
                    }
                    Log.d(TAG,"--------ppp0 in statefull mode " );
                    return;
                }
                String[] addrs = address.split("/");
                if ( addrs.length < 2 )
                    return;
                String statelessPrefixStr = addrs[1];
                address = (addrs[0].split("%"))[0];
                String tmpaddress = address.substring(0,address.lastIndexOf(":"));

                int len = tmpaddress.length();
                /*addr eg: inet6 addr: 2000:0:9:101:92bf:a667:6c40:347a/64 Scope:Global
                           inet6 addr: fe80::92bf:a667:6c40:347a/10 Scope:Link          */
                if( !tmpaddress.startsWith("fe80:") ) { /*for stateless function*/
                    mPPPoeStatelessIpAddress = address;
                    SystemProperties.set(IPV6_PPP_RESULT, PPPOE_CONNECT_ONLY_PPPOE_IPV6); //for only ipv6 pppoe,no ipv4,118

                    try {
                        mPPPoeStatelessPrefixlength = Integer.parseInt(statelessPrefixStr);
                    } catch (Exception e) {
                        Log.e(TAG,"Integer.parseInt throw exception" + e);
                        mPPPoeStatelessPrefixlength = 64;
                    }
                    //TODO: need gesture?
                    if(!userGestureIpv6Stateless){/*kernel may sent stateless addressUpdate frequently, just handle user control*/
                        return;
                    }

                    mTracker.setPPPoeStatelessv6LinkProperties("ppp0");// do here,because this is stateless first know
                    mTracker.getPPPoeIpv6StalessDns();

                    mIpv6ConnectStatus = EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT;
                    mTracker.postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION, //stateless get ip broadcast
                                     EthernetManager.EXTRA_ETHERNET_STATE,
                                     EthernetManager.EVENT_PPPOE_STATELESS6_CONNECT_SUCCESSED,
                                     null, null);

                    userGestureIpv6Stateless = false;
                    Log.d(TAG, "EVENT_PPPOE_STATELESS6_CONNECT_SUCCESSED #2" );
                    Log.d(TAG,"--------ppp0 stateless Address: " + mStatelessIpAddress + " Prefixlength:" + mStatelessPrefixlength);
                }
            }/*end of pppoe!!*/

        }

        public void moFlagChanged(String iface, int flags){
            String currentIpv6Mode = mEthernetManager.getEthernetMode6();
            Log.d(TAG,"-----------iface: " + iface + ", moflags=: " + flags + ", currentIpv6Mode = " + currentIpv6Mode);
            Log.d(TAG, "moflags: M = " + (flags & IF_RA_MANAGED) + ", O = " + (flags & IF_RA_OTHERCONF));
            if(EthernetManager.ETHERNET_IPV6MOAUTO_DISABLED == mEthernetManager.getIpv6MOAutoState()) {
                Log.i(TAG, "MO auto change Function is Disabled!");
                return;
            }
            if (currentIpv6Mode == null
                    || EthernetManager.ETHERNET_CONNECT_MODE_NONE.equals(currentIpv6Mode)
                    || EthernetManager.ETHERNET_CONNECT_MODE_MANUAL.equals(currentIpv6Mode)
                    || (EthernetManager.ETHERNET_CONNECT_MODE_DHCP.equals(currentIpv6Mode)
                       && EthernetManager.OPTION60_STATE_ENABLED == mEthernetManager.getDhcpOption60State())) {
                Log.i(TAG, "Don't chanage mode when ipv6 is closed or manual or ipoe");
                return;
            }

            if("ppp0".equals(iface)) { //handle ppp0
                moFlags = flags;
                if (!EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(currentIpv6Mode)) {
                    return;
                }
                if((flags & IF_RA_MANAGED) == 0){ //stateless: M == 0
                    if(mEthernetManager.isIpv6PppoeStateless()){
                        Log.d(TAG,"ppp0 moflags=already in stateless, return...");
                        return;
                    }
                    if ( (flags & IF_RA_OTHERCONF) != 0) { //stateless: M == 0, O != 0
                        Log.d(TAG, "ppp0 moflags=change to stateless, dns for server");
                    } else if ((flags & IF_RA_OTHERCONF) == 0) { //stateless: M == 0, O = 0
                        Log.d(TAG, "ppp0 moflags=change to stateless, dns for manual");
                    }
                    Log.d(TAG,"ppp0 moflags=change to stateless, Ipv6ConnectStatus="+(mIpv6ConnectStatus == EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT));
                    //already connected ipv6, only need change mode, for don't restart ipv4
                    try {
                        mEthernetManager.setIpv6PppoeStateless(true);
                        //mTracker.mNMService.disableIpv6("ppp0"); //don't restart ipv6 function, in pppoe it will clear link-local address
                        //mTracker.mNMService.enableIpv6("ppp0");
                    } catch (Exception e) {
                        Log.e(TAG, "mNMServices in MOflag disable/enable Ipv6 error:" + e);
                    }
                } else { //statefull: M != 0
                    if(!mEthernetManager.isIpv6PppoeStateless()){
                        Log.d(TAG,"ppp0 moflags=already in statefull, return...");
                        return;
                    }
                    Log.d(TAG,"ppp0 moflags=change to statefull, Ipv6ConnectStatus="+(mIpv6ConnectStatus == EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT));
                    //already connected ipv6, only need change mode, for don't restart ipv4
                    mEthernetManager.setIpv6PppoeStateless(false);
                    mTracker.runPPPoeDhcpv6();//ask for statefull address
                }
                return;
            } //end ppp0

            if(mIface.equals(iface)) { //handle ethX
                moFlags = flags;
                if (!EthernetManager.ETHERNET_CONNECT_MODE_DHCP.equals(currentIpv6Mode)
                        && !EthernetManager.ETHERNET_CONNECT_MODE_STATELESS.equals(currentIpv6Mode)) {
                    return;
                }
                if((flags & IF_RA_MANAGED) == 0){ //stateless: M == 0
                    if(EthernetManager.ETHERNET_CONNECT_MODE_STATELESS.equals(currentIpv6Mode)){
                        Log.d(TAG,"moflags=already in stateless, return...");
                        return;
                    }
                    if ( (flags & IF_RA_OTHERCONF) != 0) { //stateless: M == 0, O != 0
                        Log.d(TAG, "moflags=change to stateless, dns for server");
                    } else if ((flags & IF_RA_OTHERCONF) == 0) { //stateless: M == 0, O = 0
                        Log.d(TAG, "moflags=change to stateless, dns for manual");
                    }
                    mTracker.stopIpv6();
                    mTracker.mEthernetManager.setEthernetMode6(EthernetManager.ETHERNET_CONNECT_MODE_STATELESS); //dbgstateless
                    mTracker.startIpv6();
                } else { //statefull: M != 0
                    if (EthernetManager.ETHERNET_CONNECT_MODE_DHCP.equals(currentIpv6Mode)) {
                        Log.d(TAG, "ethX moflags=already in statefull, return...");
                        return;
                    }

                    Log.d(TAG,"moflags=change to statefull");
                    mTracker.stopIpv6();
                    mTracker.mEthernetManager.setEthernetMode6(EthernetManager.ETHERNET_CONNECT_MODE_DHCP);
                    mTracker.startIpv6();
                }
            }
        }
    }

    private EthernetDataTracker() {
        mNetworkInfo = new NetworkInfo(ConnectivityManager.TYPE_ETHERNET, 0, NETWORKTYPE, "");
        mLinkProperties = new LinkProperties();
        mLinkCapabilities = new LinkCapabilities();
        mDhcpInfo = new DhcpInfo();
        mLinkUp = false;
        mConnectStatus = EthernetManager.ETHERNET_CONNECT_STATE_UNKNOWN;
        mIpv6ConnectStatus = EthernetManager.ETHERNET_IPV6_CONNECT_STATE_UNKNOWN;

        if (EthernetNative.initEthernetNative() != 0) {
            Log.e(TAG,"Can not init ethernet device layers");
            return;
        }
        if(DEBUG) Log.d(TAG,"initEthernetNative() Success!");

        mNetworkInfo.setIsAvailable(false);
        setTeardownRequested(false);
        if ("shcmcc".equals(SystemProperties.get("ro.product.target", "aosp"))) { //china mobile base mode will use shcmcc
            isCMCCbaseMode = true;
        } else {
            isCMCCbaseMode = false;
        }
    }

    private void interfaceAdded(String iface) {
        if(!checkInterfaceName(iface))
            return;

        reconnect();
    }

    private void interfaceRemoved(String iface) {
        if (!iface.equals(mIface))
            return;

        Log.d(TAG, "Removing " + iface);

        teardown();
    }

    private void getIpv6StalessDns() {
        Thread dhcpv6dnsThread = new Thread(new Runnable() {
            public void run() {
                getIpv6StalessDns(mIface);
                setStatelessDnsProperties();
                //TODO send Message

                mNetworkInfo.setIsAvailable(true);
                mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, null);
                Message msg = mCsHandler.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
                msg.sendToTarget();
            }
        });
        dhcpv6dnsThread.start();
    }

    private void getIpv6StalessDns(String iface) {
        if (((moFlags & IF_RA_MANAGED) == 0) && ((moFlags & IF_RA_OTHERCONF) == 0)) { //stateless: M == 0, O == 0
            Log.d(TAG, "moflags=change to stateless, dns by manual");
            String default_stateless6_dns1 = SystemProperties.get(EthernetManager.PERSIST_STATELESS6_DNS1,
                    "2000::123:1");
            String default_stateless6_dns2 = SystemProperties.get(EthernetManager.PERSIST_STATELESS6_DNS2,
                    "2000::123:2");
            SystemProperties.set("dhclient."+iface+".dns1", default_stateless6_dns1);
            SystemProperties.set("dhclient."+iface+".dns2", default_stateless6_dns2);
        } else {
            if (!waitingLinklocal6(iface)) { // this may sleep for a while when Link-local not ready.
                return;
            }
            mLinklocal6ProbeRunning = false;
            NetworkUtils.runDhcpv6Dns(iface);
        }
    }

    private void setStatelessDnsProperties() {
        String dns6_1 = SystemProperties.get("dhclient."+mIface+".dns1");
        String dns6_2 = SystemProperties.get("dhclient."+mIface+".dns2");
        ipv6LastDns = new ArrayList<InetAddress>();
        Log.i(TAG, "setStatelessDnsProperties dns6_1:" + dns6_1 + "  dns6_2:"+dns6_2);

        if((dns6_1 != null) && !("".equals(dns6_1))) {
            Collection<InetAddress> dnses1 = mLinkProperties.getDnses();
            if(dnses1 != null && !dnses1.contains(NetworkUtils.numericToInetAddress(dns6_1)))
            {
                mStatelessIpv6Dns1 = dns6_1;
                mLinkProperties.addDns(NetworkUtils.numericToInetAddress(dns6_1));
            }
            ipv6LastDns.add(NetworkUtils.numericToInetAddress(dns6_1));
        }

        if((dns6_2 != null) && !("".equals(dns6_2))) {
            Collection<InetAddress> dnses2 = mLinkProperties.getDnses();
            if(dnses2 != null && !dnses2.contains(NetworkUtils.numericToInetAddress(dns6_2)))
            {
                mStatelessIpv6Dns2 = dns6_2;
                mLinkProperties.addDns(NetworkUtils.numericToInetAddress(dns6_2));
            }
            ipv6LastDns.add(NetworkUtils.numericToInetAddress(dns6_2));
        }
        Log.d(TAG, "EVENT_STATELESS6_CONNECT_SUCCESSED #3" );
        if(isCMCCbaseMode) {
            postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION, //stateless get ip broadcast
                             EthernetManager.EXTRA_ETHERNET_STATE,
                             EthernetManager.EVENT_IPV6_CONNECT_SUCCESSED,
                             null, null);
        } else {
            postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION, //stateless get ip broadcast
                             EthernetManager.EXTRA_ETHERNET_STATE,
                             EthernetManager.EVENT_STATELESS6_CONNECT_SUCCESSED,
                             null, null);
        }
    }

    private void getPPPoeIpv6StalessDns() {
        Thread dhcpv6dnsThread = new Thread(new Runnable() {
            public void run() {
                getIpv6StalessDns("ppp0");
                setPPPoeStatelessDnsProperties();
                //TODO send Message

                mNetworkInfo.setIsAvailable(true);
                mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, null);
                Message msg = mCsHandler.obtainMessage(EVENT_CONFIGURATION_CHANGED, mNetworkInfo);
                msg.sendToTarget();
            }
        });
        dhcpv6dnsThread.start();
    }

    private void setPPPoeStatelessDnsProperties() {
        String dns6_1 = SystemProperties.get("dhclient."+"ppp0"+".dns1");
        String dns6_2 = SystemProperties.get("dhclient."+"ppp0"+".dns2");
        Log.i(TAG, "setPPPoeStatelessDnsProperties dns6_1:" + dns6_1 + "  dns6_2:"+dns6_2);

        if((dns6_1 != null) && !("".equals(dns6_1))) {
            Collection<InetAddress> dnses1 = mLinkProperties.getDnses();
            if(dnses1 != null && !dnses1.contains(NetworkUtils.numericToInetAddress(dns6_1)))
            {
                mPPPoeStatelessIpv6Dns1 = dns6_1;
                mLinkProperties.addDns(NetworkUtils.numericToInetAddress(dns6_1));
            }
        }

        if((dns6_2 != null) && !("".equals(dns6_2))) {
            Collection<InetAddress> dnses2 = mLinkProperties.getDnses();
            if(dnses2 != null && !dnses2.contains(NetworkUtils.numericToInetAddress(dns6_2)))
            {
                mPPPoeStatelessIpv6Dns2 = dns6_2;
                mLinkProperties.addDns(NetworkUtils.numericToInetAddress(dns6_2));
            }
        }
        Log.d(TAG, "EVENT_PPPOE_STATELESS6_CONNECT_SUCCESSED #3" );
        postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION, //stateless get ip broadcast
                         EthernetManager.EXTRA_ETHERNET_STATE,
                         EthernetManager.EVENT_PPPOE_STATELESS6_CONNECT_SUCCESSED,
                         null, null);
    }

    public void unregisterEthernetObserver() {
        // unregister for notifications from NetworkManagement Service
        try {
            mNetworkManagementService.unregisterObserver(mInterfaceObserver);
            if(DEBUG) Log.d(TAG, "unregister InterfaceObserver Success!");
        } catch (RemoteException e) {
            Log.e(TAG, "Could not unregister InterfaceObserver " + e);
        }
    }

    public void registerEthernetObserver() {
        // register for notifications from NetworkManagement Service
        try {
            mNetworkManagementService.registerObserver(mInterfaceObserver);
            if(DEBUG) Log.d(TAG, "register InterfaceObserver Success!");
        } catch (RemoteException e) {
            Log.e(TAG, "Could not register InterfaceObserver " + e);
        }
    }

    private void pppoeAutoConnect() {
        Message msg_reconnect = new Message();
        msg_reconnect.what = EthernetManager.ETHERNET_AUTORECONNECT;
        msg_reconnect.arg1 = PppoeManager.EVENT_AUTORECONNECTING;
        mEthernetAutoReconnectHandler.sendMessageDelayed(msg_reconnect, EthernetManager.ETHERNET_RECONNECT_TIME);
    }
    //add for chinamobile
    private void dhcp6AutoConnect() {
        //mEthernetManager.setEthernetMode6(EthernetManager.ETHERNET_CONNECT_MODE_DHCP);
        Message msg_reconnect = new Message();
        msg_reconnect.what = EthernetManager.DHCP6_AUTORUN;
        //msg_reconnect.arg1 = EthernetManager.EVENT_DHCPV6_AUTORECONNECTING;
        mEthernetAutoReconnectHandler.sendMessageDelayed(msg_reconnect, EthernetManager.ETHERNET_RECONNECT_TIME);
    }

    private void pppoeStateless6AutoConnect() {
        Message msg_reconnect = new Message();
        msg_reconnect.what = EthernetManager.ETHERNET_PPPOE_STATELESS6_AUTORUN;
        mEthernetAutoReconnectHandler.sendMessageDelayed(msg_reconnect, EthernetManager
                .ETHERNET_PPPOE_STATELESS6_AUTORUN_TIME);
    }

    // For http proxy information
    private String proxy_host = null;
    private int    proxy_port = 0;
    private String exclude_list = null;

    // Load http proxy information from environment variables
    private boolean updateHttpProxy() {
        ProxyParser pp = ProxyParser.getInstance();
        if (pp != null) {
            proxy_host = pp.getHttp_proxy_host();
            proxy_port = pp.getHttp_proxy_port();
        }
        Log.d(TAG, "Latest proxy_host -- host:" + proxy_host + "; port:" + proxy_port + "; exclude_list:" + exclude_list);
        return (proxy_host != null && proxy_port != 0);
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
		return null;
	}
	
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
            Log.d(TAG_SU,"finalMac is "+ finalMac+", mJXtempMac is "+mJXtempMac);
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
            return null;
        }
    }

    private void runDhcp() {
	    //begin:add by ysten xumiao at 20191118: liaoning anytime ipv6 after ipv4 mDhcpv4StartTime discover
		if(SystemProperties.get("ro.ysten.province","master").contains("liaoning")) {
        //if (mDhcpv4StartTime < 0) {
            mDhcpv4StartTime = SystemClock.elapsedRealtime();
            Log.i(TAG, "runDhcp mDhcpv4StartTime=" + mDhcpv4StartTime);
        //}
		}else{
			if (mDhcpv4StartTime < 0) {
                mDhcpv4StartTime = SystemClock.elapsedRealtime();
                Log.i(TAG, "else runDhcp mDhcpv4StartTime=" + mDhcpv4StartTime);
           }
		}
		//end:add by ysten xumiao at 20191118:  liaoning anytime ipv6 after ipv4 mDhcpv4StartTime discover
        Thread dhcpThread = new Thread(new Runnable() {
            public void run() {
                Log.i(TAG, "runDhcp start");
                mConnectStatus = EthernetManager.ETHERNET_CONNECT_STATE_CONNECTING;
                  mDhcpInfo.clear(); // clear mDhcpInfo before set new value
				DhcpResults dhcpResults = new DhcpResults();
                int option60 = mEthernetManager.getDhcpOption60State();
                String login = mEthernetManager.getDhcpOption60Login();
                String password = mEthernetManager.getDhcpOption60Password();
                int option125 = mEthernetManager.getDhcpOption125State();
                String option125Info = mEthernetManager.getDhcpOption125Info();
            //begin:add by zengzhiliang at 20200403:gui zhou fix can not DHCP IP
				 if(SystemProperties.get("ro.ysten.province","master").equals("cm201_guizhou")
				 	&&SystemProperties.get("sys.yst.ipoe_enable","null").equals("false"))
				 	{
				 		option60 = 0;
				 	}
            //end:add by zengzhiliang at 20200403:gui zhou fix can not DHCP IP
                Log.d(TAG,"......option60 login:"+ login + "  password:"+password + "  loginstate:"+option60);
                SystemProperties.set("dhcp.option125","");
                SystemProperties.set("dhcp.process","");
                if(DEBUG) Log.d(TAG,"DHCP before: dhcp.option125 = "+SystemProperties.get("dhcp.option125")+" dhcp.process = "+SystemProperties.get("dhcp.process"));
                if (isNeedRelease(getDhcpMode(), 4)) {
                    Log.i(TAG, "releaseDhcpLease " + mIface);
                    NetworkUtils.releaseDhcpLease(mIface);
                    Log.i(TAG, "releaseDhcpLease " + mIface + " done");
                    if (mConnectStatus != EthernetManager.ETHERNET_CONNECT_STATE_CONNECTING) {
                        Log.e(TAG, "mConnectStatus is not connecting after release, no do dhcp anymore");
                        return;
                    }
                }
                boolean result = NetworkUtils.runDhcpPlus(mIface, dhcpResults, option60, login, password, option125, option125Info);
                Log.i(TAG, "runDhcp return " + result);
                if(!result) {
                    Log.e(TAG, "DHCP request error:" + NetworkUtils.getDhcpError());
                    if(mLinkUp != false && (mConnectStatus == EthernetManager.ETHERNET_CONNECT_STATE_CONNECTING)){
                        postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                                         EthernetManager.EXTRA_ETHERNET_STATE,
                                         EthernetManager.EVENT_DHCP_CONNECT_FAILED,
                                         null, null);

                        int ethernetFailedReason = -1;
                        String dhcpOption125 = SystemProperties.get("dhcp.option125");
                        String dhcpProcess = SystemProperties.get("dhcp.process");
                        if(DEBUG) Log.d(TAG,"DHCP failed: dhcp.option125 = "+SystemProperties.get("dhcp.option125")+" dhcp.process = "+SystemProperties.get("dhcp.process"));
                        if(dhcpOption125.equals("FAILED") && mEthernetManager.getIpv4Configuration().ipAssignment == IpConfiguration.IpAssignment.IPOE){
                            ethernetFailedReason = EthernetManager.FAILED_REASON_IPOE_AUTH_FAIL;
                        }else if(NetworkUtils.getDhcpError().equals("Timed out waiting for DHCP to finish") || NetworkUtils.getDhcpError().equals("Timed out waiting for DHCP Renew to finish")){
                            if(dhcpProcess.equals("DISCOVER")){
                                ethernetFailedReason = EthernetManager.FAILED_REASON_DISCOVER_TIMEOUT;
                            }else if(dhcpProcess.equals("REQUEST")){
                                ethernetFailedReason = EthernetManager.FAILED_REASON_REQUEST_TIMEOUT;
                            }
                        }
                        postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                         EthernetManager.EXTRA_ETHERNET_STATE,
                                         EthernetManager.EVENT_IPV4_CONNECT_FAILED,
                                         EthernetManager.EXTRA_ETHERNET_FAILED_REASON,
                                         ethernetFailedReason);
                    }
                    if (!EthernetManager.ETHERNET_CONNECT_MODE_DHCP.equals(mEthernetManager.getEthernetMode()))
                        return;
                    mConnectStatus = EthernetManager.ETHERNET_CONNECT_STATE_DISCONNECT;
                    Log.d(TAG, "dhcd connect failed, persist.ethernet.doublestack:" +
                        SystemProperties.get("persist.ethernet.doublestack", "false") +
                        ", option60:" + option60 + ", option125:" + option125);

                    if (SystemProperties.get("persist.ethernet.doublestack", "false").equals("true") &&
                       ((option60 == EthernetManager.OPTION60_STATE_ENABLED) ||
                        (option125 == EthernetManager.OPTION125_STATE_ENABLED))) {
                        mEthernetManager.setEthernetMode(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE, null);

                        Message msg_reconnect = new Message();
                        msg_reconnect.what = EthernetManager.ETHERNET_AUTORECONNECT;
                        msg_reconnect.arg1 = PppoeManager.EVENT_AUTORECONNECTING;
                        mEthernetAutoReconnectHandler.sendMessageDelayed(msg_reconnect, 500);
                    } else {
            //begin:add by zengzhiliang at 20200403:gui zhou fix can not DHCP IP
                        /*modify by zhaolianghua for guizhou IPOE requirment start @20191129*/
                        if(SystemProperties.get("ro.ysten.province","master").equals("cm201_guizhou")){
                           // boolean dhcpRes = NetworkUtils.runDhcp(mIface, dhcpResults);
                           if(!SystemProperties.get("sys.yst.ipoe_enable","null").equals("false"))
                           	{
                           		SystemProperties.set("sys.yst.ipoe_enable","false");
                           	}
                            Log.d(TAG,"guizhou dhcp failed in 300s");
                        }
					    Message msg_reconnect = new Message();
                        msg_reconnect.what = EthernetManager.;
                        msg_reconnect.arg1 = EthernetManager.EVENT_DHCP_AUTORECETHERNET_AUTORECONNECTONNECTING;
                        mEthernetAutoReconnectHandler.sendMessageDelayed(msg_reconnect, EthernetManager.ETHERNET_RECONNECT_TIME);
                        /*modify by zhaolianghua end @20191129*/
                    }
            //end:add by zengzhiliang at 20200403:gui zhou fix can not DHCP IP
                    return ;
                }

                if(SystemProperties.get("persist.ethernet.vlan.enabled", "false").equals("true")
                        && SystemProperties.get("dhcp.vlan.enable", "false").equals("true")) {
                    if (SystemProperties.get("persist.ethernet.vlan.area", "none").equals("shanghai"))
                        mHiEthernetManager.setEthernetEnabled(true);
                }

                mIpv4LinkProperties = dhcpResults.linkProperties;
                mDhcpInfo = dhcpResults.makeDhcpInfo();

                if (updateHttpProxy())
                    mLinkProperties.setHttpProxy(new ProxyProperties(proxy_host, proxy_port, exclude_list));
                Log.i(TAG, "runDhcp, mIpv4LinkProperties:" + mIpv4LinkProperties);
                mConnectStatus = EthernetManager.ETHERNET_CONNECT_STATE_CONNECT;
                setIpv4LinkProperties(mIpv4LinkProperties);

                mNetworkInfo.setIsAvailable(true);
                mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, mHwAddr);
                Message msg = mCsHandler.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
                msg.sendToTarget();
                if(SystemProperties.get("ro.ysten.province","master").contains("liaoning")) {
                   SystemProperties.set("persist.sys.wifistate", "0");//0 off 1 on
                 }
                if(DEBUG) Log.d(TAG, "send EthernetManager.EVENT_DHCP_CONNECT_SUCCESSED in runDhcp()");
                postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_DHCP_CONNECT_SUCCESSED,
                                 null, null);
                postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_IPV4_CONNECT_SUCCESSED,
                                 null, null);
            }
        });
        dhcpThread.start();
    }

    private void runCheckIpConflict() {
        Thread ipConflictThread = new Thread(new Runnable() {
            public void run() {
                DhcpInfo dhcpInfo = mEthernetManager.getSavedEthernetIpInfo();
                int result;
                String action = "android.net.conn.IP_ADDRESS_CONFLICTED";

                if (dhcpInfo == null) {
                    Log.i(TAG, "dhcpinfo null, don't ip conflict check");
                } else {
                    try {
                        result = NetworkUtils.checkIpConflict(mIface, NetworkUtils.intToInetAddress(dhcpInfo.ipAddress).toString());
                    } catch (Exception e) {
                        Log.e(TAG, "runCheckIpConflict intToInetAddress error");
                        result  = 0;
                    }

                    if (result == 1) {
                        Log.i(TAG, "ip conflict");
                        postNotification(action, null, 0,
                                         null, null);
                    }else
                        Log.i(TAG, "ip not conflict");
                }
            }
        });
        ipConflictThread.start();
    }

    private void configureInterface() {
        DhcpInfo dhcpInfo = mEthernetManager.getSavedEthernetIpInfo();

        if (dhcpInfo == null) {
            postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                             EthernetManager.EXTRA_ETHERNET_STATE,
                             EthernetManager.EVENT_STATIC_CONNECT_FAILED,
                             null, null);
            postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                             EthernetManager.EXTRA_ETHERNET_STATE,
                             EthernetManager.EVENT_IPV4_CONNECT_FAILED,
                             null, null);
            Log.e(TAG, "Get Interface configuration from Database FAIL!");
            mConnectStatus = EthernetManager.ETHERNET_CONNECT_STATE_DISCONNECT;
        } else {
            Log.i(TAG, "Set IP manually " + dhcpInfo.toString());
            InterfaceConfiguration ifcg = new InterfaceConfiguration();
            int prefixLength = NetworkUtils.netmaskIntToPrefixLength(dhcpInfo.netmask);
            LinkAddress linkAddress = new LinkAddress(NetworkUtils.intToInetAddress(dhcpInfo.ipAddress), prefixLength);

            ifcg.setLinkAddress(linkAddress);
            ifcg.setInterfaceUp();

            try {
                mNetworkManagementService.setInterfaceConfig(mIface, ifcg);

                DhcpResults dhcpResults = new DhcpResults();
                dhcpResults.setInterfaceName(mIface);
                dhcpResults.addLinkAddress(NetworkUtils.intToInetAddress(dhcpInfo.ipAddress).getHostAddress(), prefixLength);
                dhcpResults.addGateway(NetworkUtils.intToInetAddress(dhcpInfo.gateway).getHostAddress());
                dhcpResults.addDns(NetworkUtils.intToInetAddress(dhcpInfo.dns1).getHostAddress());
                dhcpResults.addDns(NetworkUtils.intToInetAddress(dhcpInfo.dns2).getHostAddress());

                mIpv4LinkProperties = dhcpResults.linkProperties;
                mDhcpInfo = dhcpResults.makeDhcpInfo();

                if (updateHttpProxy())
                    mLinkProperties.setHttpProxy(new ProxyProperties(proxy_host, proxy_port, exclude_list));

                Log.i(TAG, "configureInterface, mIpv4LinkProperties:" + mIpv4LinkProperties);
                mConnectStatus = EthernetManager.ETHERNET_CONNECT_STATE_CONNECT;
                setIpv4LinkProperties(mIpv4LinkProperties);

                mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, null);
                mNetworkInfo.setIsAvailable(true);

                if(DEBUG) Log.d(TAG, "Static IP configuration succeeded");
                if(DEBUG) Log.d(TAG, "send EthernetManager.EVENT_STATIC_CONNECT_SUCCESSED in configureInterface()");
                postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_STATIC_CONNECT_SUCCESSED,
                                 null, null);
                postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_IPV4_CONNECT_SUCCESSED,
                                 null, null);

                Message msg = mCsHandler.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
                msg.sendToTarget();

            } catch (RemoteException re) {
                postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_STATIC_CONNECT_FAILED,
                                 null, null);
                postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_IPV4_CONNECT_FAILED,
                                 null, null);
                Log.e(TAG, "Static IP configuration failed: " + re);
                mConnectStatus = EthernetManager.ETHERNET_CONNECT_STATE_DISCONNECT;
            } catch (IllegalStateException e) {
                postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_STATIC_CONNECT_FAILED,
                                 null, null);
                postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_IPV4_CONNECT_FAILED,
                                 null, null);
                Log.e(TAG, "Static IP configuration failed: " + e);
                mConnectStatus = EthernetManager.ETHERNET_CONNECT_STATE_DISCONNECT;
            }
            runCheckIpConflict();
        }
    }

    private synchronized void runPppoe(){
        Thread pppoeThread = new Thread(new Runnable() {
            public void run() {
                if(mConnectStatus == PppoeManager.PPPOE_STATE_CONNECT) {
                    Log.d(TAG, "Pppoe is Connected! return!");
                    return;
                }
                if(mIface == null || mIface.length() == 0){
                    Log.e(TAG, "data store error, pppoe_ifname is null");
                    postNotification(PppoeManager.PPPOE_STATE_CHANGED_ACTION,
                                     PppoeManager.EXTRA_PPPOE_STATE,
                                     PppoeManager.EVENT_CONNECT_FAILED,
                                     PppoeManager.EXTRA_PPPOE_ERRMSG,
                                     mPppoeManager.getErrorMessage(pIface));
                    postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                     EthernetManager.EXTRA_ETHERNET_STATE,
                                     EthernetManager.EVENT_IPV4_CONNECT_FAILED,
                                     EthernetManager.EXTRA_ETHERNET_FAILED_REASON,
                                     EthernetManager.FAILED_REASON_INVALID_PARAMETER);

                    mConnectStatus = PppoeManager.PPPOE_STATE_DISCONNECT;
                    return;
                }
                String userName = mPppoeManager.getPppoeUsername();
                if(userName == null || userName.length() == 0){
                    Log.e(TAG, "data store error, pppoe_user_name is null");
                    postNotification(PppoeManager.PPPOE_STATE_CHANGED_ACTION,
                                     PppoeManager.EXTRA_PPPOE_STATE,
                                     PppoeManager.EVENT_CONNECT_FAILED,
                                     PppoeManager.EXTRA_PPPOE_ERRMSG,
                                     mPppoeManager.getErrorMessage(pIface));
                    postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                     EthernetManager.EXTRA_ETHERNET_STATE,
                                     EthernetManager.EVENT_IPV4_CONNECT_FAILED,
                                     EthernetManager.EXTRA_ETHERNET_FAILED_REASON,
                                     EthernetManager.FAILED_REASON_INVALID_PARAMETER);
                    mConnectStatus = PppoeManager.PPPOE_STATE_DISCONNECT;
                    return;
                }
                String userPass = mPppoeManager.getPppoePassword();
                if(userPass == null || userName.length() == 0){
                    Log.e(TAG, "data store error, pppoe_user_pass is null");
                    postNotification(PppoeManager.PPPOE_STATE_CHANGED_ACTION,
                                     PppoeManager.EXTRA_PPPOE_STATE,
                                     PppoeManager.EVENT_CONNECT_FAILED,
                                     PppoeManager.EXTRA_PPPOE_ERRMSG,
                                     mPppoeManager.getErrorMessage(pIface));
                    postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                     EthernetManager.EXTRA_ETHERNET_STATE,
                                     EthernetManager.EVENT_IPV4_CONNECT_FAILED,
                                     EthernetManager.EXTRA_ETHERNET_FAILED_REASON,
                                     EthernetManager.FAILED_REASON_INVALID_PARAMETER);
                    mConnectStatus = PppoeManager.PPPOE_STATE_DISCONNECT;
                    return;
                }

                Log.i(TAG, "mConnectStatus = " + mConnectStatus);
				//add by huxiang for hainan PPPoE can not get IP
               if ("true".equals(SystemProperties.get("persist.sys.firstboot", "false"))) {
				        	 if (EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mEthernetManager.getEthernetMode())){
				        	 	 reStartppp0Ipv6interface();
								 mEthernetManager.setEthernetMode(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE,null); 
				                 mEthernetManager.setEthernetMode6(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE);
                                 mEthernetManager.setIpv6PppoeStateless(true);
                                 mEthernetManager.setIpv6MOAutoState(true);
								 SystemProperties.set("persist.sys.firstboot", "false");
				        	 }
				}
				//add end
                if(mConnectStatus != PppoeManager.PPPOE_STATE_CONNECTING
                       && mConnectStatus != PppoeManager.PPPOE_STATE_CONNECT) {
                        Log.i(TAG, "start Pppoe! ifname[" + mIface + "],user[" + userName + "],pass[" + userPass + "]");
                        mConnectStatus = PppoeManager.PPPOE_STATE_CONNECTING;
                        postNotification(PppoeManager.PPPOE_STATE_CHANGED_ACTION,
                                         PppoeManager.EXTRA_PPPOE_STATE,
                                         PppoeManager.EVENT_CONNECTING,
                                         null, null);
                        PppoeNative.connectPppoe(mIface, userName, userPass);
                    int result = mPppoeManager.getConnectResult(mIface);
                    Log.i(TAG, "connect result: " + result);
                    mDhcpInfo.clear(); // clear mDhcpInfo before set new value
                    if( !(PppoeManager.PPPOE_CONNECT_RESULT_CONNECT == result)
                        && (  !"".equals(mPPPoeStatelessIpAddress) ||
                              !"".equals(SystemProperties.get("dhclient.ipaddress.ppp0", "")) ) ){
                        /*pppv4 connect fail,but pppv6 get stateless or statefull addr, don't reconnect*/
                        Log.i(TAG, "pppoev6: pppv4 connect fail,but pppv6 get stateless or statefull addr, don't reconnect");
                        return;
                    }

                    if(PppoeManager.PPPOE_CONNECT_RESULT_CONNECT == result) {//Connect Success
                        mConnectStatus = PppoeManager.PPPOE_STATE_CONNECT;

                        DhcpResults dhcpResults = new DhcpResults();
                        String ipAddress = mPppoeManager.getIpaddr(pIface);
                        String getwayAddress = mPppoeManager.getGateway(pIface);
                        String dns1 = mPppoeManager.getDns1(pIface);
                        String dns2 = mPppoeManager.getDns2(pIface);
                        InetAddress netmask = NetworkUtils.numericToInetAddress(mPppoeManager.getNetmask(pIface));
                        int Netmask = 0;
                        try {
                            Netmask = NetworkUtils.inetAddressToInt(netmask);
                        } catch (IllegalArgumentException e) {
                            Log.e(TAG, "inetAddressToInt error: " + ((netmask == Inet6Address.LOOPBACK)?"ipv6 loopback":"other"));
                            Netmask = 0;
                        }

                        if ((ipAddress == null) || (getwayAddress == null) || (Netmask == 0)) {
                            Log.e(TAG, "runPppoe: pppoe connect failed with"+ pIface +" can't get ip");
                            if(mLinkUp != false) {
                                postNotification(PppoeManager.PPPOE_STATE_CHANGED_ACTION,
                                                 PppoeManager.EXTRA_PPPOE_STATE,
                                                 PppoeManager.EVENT_CONNECT_FAILED,
                                                 null, null);
                                postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                                 EthernetManager.EVENT_IPV4_CONNECT_FAILED,
                                                 EthernetManager.EXTRA_ETHERNET_FAILED_REASON,
                                                 EthernetManager.FAILED_REASON_PPPOE_TIMEOUT);
                                mConnectStatus = PppoeManager.PPPOE_STATE_DISCONNECT;
                                Message msg_reconnect = new Message();
                                msg_reconnect.what = EthernetManager.ETHERNET_AUTORECONNECT;
                                msg_reconnect.arg1 = PppoeManager.EVENT_AUTORECONNECTING;
                                mEthernetAutoReconnectHandler.sendMessageDelayed(msg_reconnect, EthernetManager.ETHERNET_RECONNECT_TIME);
                            }
                            return;
                        }

                        int prefixLength = NetworkUtils.netmaskIntToPrefixLength(Netmask);
                        dhcpResults.addLinkAddress(ipAddress, prefixLength);
                        dhcpResults.addGateway(getwayAddress);
                        dhcpResults.addDns(dns1);
                        dhcpResults.addDns(dns2);
                        dhcpResults.setInterfaceName(pIface);
                        mLinkProperties = dhcpResults.linkProperties;
                        mDhcpInfo = dhcpResults.makeDhcpInfo();
                        ipv4LastDns = mLinkProperties.getDnses();

                        //setIpv4LinkProperties(mIpv4LinkProperties);

                        mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, null);
                        mNetworkInfo.setIsAvailable(true);

                        postNotification(PppoeManager.PPPOE_STATE_CHANGED_ACTION,
                                         PppoeManager.EXTRA_PPPOE_STATE,
                                         PppoeManager.EVENT_CONNECT_SUCCESSED,
                                         null, null);
                        postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                         EthernetManager.EXTRA_ETHERNET_STATE,
                                         EthernetManager.EVENT_IPV4_CONNECT_SUCCESSED,
                                         null,null);

                        Message msg = mCsHandler.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
                        msg.sendToTarget();
                        try {
                            mNetworkManagementService.setIpForwardingEnabled(true);
                        } catch (RemoteException e) {
                           Log.e(TAG,"pppoe connect success, setIpForwardingEnabled failed");
                        }
                    }
                    else if(PppoeManager.PPPOE_CONNECT_RESULT_AUTH_FAIL == result) {//Auth Fail
                        //TODO
                        Log.d(TAG, "PPPOE_CONNECT_RESULT_AUTH_FAIL");
                        mConnectStatus = PppoeManager.PPPOE_STATE_DISCONNECT;

                        if("shcmcc".equals(SystemProperties.get("ro.product.target", "aosp"))) {
                            postNotification(PppoeManager.PPPOE_STATE_CHANGED_ACTION,
                                             PppoeManager.EXTRA_PPPOE_STATE,
                                             PppoeManager.EVENT_CONNECT_FAILED,
                                             null, null);
                            postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                             EthernetManager.EXTRA_ETHERNET_STATE,
                                             EthernetManager.EVENT_IPV4_CONNECT_FAILED,
                                             EthernetManager.EXTRA_ETHERNET_FAILED_REASON,
                                             EthernetManager.FAILED_REASON_PPPOE_AUTH_FAILED);
                        }
                        int option60 = mEthernetManager.getDhcpOption60State();
                        int option125 = mEthernetManager.getDhcpOption125State();
                        Log.d(TAG,
                            "pppoe auth failed: option60 =" + option60 +
                                ", option125 =" + option125);
                        Log.d(TAG,
                             "pppoe auth failed, persist.ethernet.doublestack:" +
                             SystemProperties.get(
                                        "persist.ethernet.doublestack", "false"
                                 ));

                        if (SystemProperties.get(
                            "persist.ethernet.doublestack",
                                "false"
                                    ).equals("true") &&
                              ((option60 == EthernetManager.OPTION60_STATE_ENABLED) ||
                              (option125 == EthernetManager.OPTION125_STATE_ENABLED))) {

                            mEthernetManager.setEthernetMode(EthernetManager.ETHERNET_CONNECT_MODE_DHCP,
                                        null);

                            Message msg_reconnect = new Message();
                            msg_reconnect.what = EthernetManager.ETHERNET_AUTORECONNECT;
                            msg_reconnect.arg1 = EthernetManager.EVENT_DHCP_AUTORECONNECTING;
                            mEthernetAutoReconnectHandler.sendMessageDelayed(msg_reconnect,
                                        500);
                        } else {
                            /*begin by chenfeng at 20200317 pppoe reconnect when auth error*/
                            if (SystemProperties.get("ro.ysten.province","master").contains("jiangsu")){
                                Message msg_reconnect = new Message();
                                msg_reconnect.what = EthernetManager.ETHERNET_AUTORECONNECT;
                                msg_reconnect.arg1 = PppoeManager.EVENT_AUTORECONNECTING;
                                mEthernetAutoReconnectHandler.sendMessageDelayed(msg_reconnect, EthernetManager.ETHERNET_RECONNECT_TIME);
                            }
                            /*end by chenfeng at 20200317 pppoe reconnect when auth error*/
                            else {
                            postNotification(PppoeManager.PPPOE_STATE_CHANGED_ACTION,
                                             PppoeManager.EXTRA_PPPOE_STATE,
                                             PppoeManager.EVENT_CONNECT_FAILED_AUTH_FAIL,
                                             PppoeManager.EXTRA_PPPOE_ERRMSG,
                                             mPppoeManager.getErrorMessage(pIface));
                            postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                             EthernetManager.EXTRA_ETHERNET_STATE,
                                             EthernetManager.EVENT_IPV4_CONNECT_FAILED,
                                             EthernetManager.EXTRA_ETHERNET_FAILED_REASON,
                                             EthernetManager.FAILED_REASON_PPPOE_AUTH_FAILED);
                            }
                            
                        }
                    }
                    else if (PppoeManager.PPPOE_CONNECT_RESULT_CONNECTING == result) {//already connecting do nothing
                        if(DEBUG) Log.d(TAG, "Pppoe is already connecting!!");
                        mConnectStatus = PppoeManager.PPPOE_STATE_CONNECTING;
                    }
                    else if (PppoeManager.PPPOE_CONNECT_RESULT_DISCONNECTING == result) {//disconnecting try again later
                        if(DEBUG) Log.d(TAG, "Pppoe is disconnecting, try again!");
                        mConnectStatus = PppoeManager.PPPOE_STATE_DISCONNECT;
                        Message msg_reconnect = new Message();
                        msg_reconnect.what = EthernetManager.ETHERNET_RECONNECT_ONCE;
                        msg_reconnect.arg1 = PppoeManager.EVENT_AUTORECONNECTING;
                        mEthernetAutoReconnectHandler.sendMessageDelayed(msg_reconnect, EthernetManager.ETHERNET_RECONNECT_TIME);
                    }
                    else {//Connect Fail
                        //TODO
                        int option60 = mEthernetManager.getDhcpOption60State();
                        int option125 = mEthernetManager.getDhcpOption125State();

                        mConnectStatus = PppoeManager.PPPOE_STATE_DISCONNECT;
                        if(mLinkUp != false) {
                            postNotification(PppoeManager.PPPOE_STATE_CHANGED_ACTION,
                                             PppoeManager.EXTRA_PPPOE_STATE,
                                             PppoeManager.EVENT_CONNECT_FAILED,
                                             PppoeManager.EXTRA_PPPOE_ERRMSG,
                                             mPppoeManager.getErrorMessage(pIface));
                            postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                             EthernetManager.EXTRA_ETHERNET_STATE,
                                             EthernetManager.EVENT_IPV4_CONNECT_FAILED,
                                             null,null);
                            Log.d(TAG, "pppoe connct failed, persist.ethernet.doublestack:"+
                            SystemProperties.get("persist.ethernet.doublestack", "false"));
                            if (SystemProperties.get("persist.ethernet.doublestack", "false").equals("true") &&
                                ((option60 == EthernetManager.OPTION60_STATE_ENABLED) ||
                                 (option125 == EthernetManager.OPTION125_STATE_ENABLED))) {
                                mEthernetManager.setEthernetMode(EthernetManager.ETHERNET_CONNECT_MODE_DHCP, null);
                                Message msg_reconnect = new Message();
                                msg_reconnect.what = EthernetManager.ETHERNET_AUTORECONNECT;
                                msg_reconnect.arg1 = EthernetManager.EVENT_DHCP_AUTORECONNECTING;
                                mEthernetAutoReconnectHandler.sendMessageDelayed(msg_reconnect, 500);
                            } else {
                                Message msg_reconnect = new Message();
                                msg_reconnect.what = EthernetManager.ETHERNET_AUTORECONNECT;
                                msg_reconnect.arg1 = PppoeManager.EVENT_AUTORECONNECTING;
                                mEthernetAutoReconnectHandler.sendMessageDelayed(msg_reconnect, EthernetManager.ETHERNET_RECONNECT_TIME);
                            }
                        }
                    }
                }
            }
        });
        pppoeThread.start();
    }

    private void setIpv4LinkProperties(LinkProperties lp) {
        CompareResult<LinkAddress> compAddres = null;
        CompareResult<RouteInfo> compRoutes = null;
        CompareResult<InetAddress> compDnses =  null;
        InetAddress zero_addr = null;

        try {
            zero_addr = InetAddress.getByName("0.0.0.0");
        } catch (UnknownHostException e) {
            Log.e(TAG, "setIpv4LinkProperties invalid param");
        }

        if(mLinkProperties != null) {
            if(mIpv6ConnectStatus != EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT){
                synchronized (mLinkProperties) {
                    if(DEBUG) Log.i(TAG,"mLinkProperties.clear() in setIpv4LinkProperties()");
                    mLinkProperties.clear();
                }
            }
            compAddres = mLinkProperties.compareAddresses(lp);
            compRoutes = mLinkProperties.compareAllRoutes(lp);
            compDnses = mLinkProperties.compareDnses(lp);
        } else
            return;

        mLinkProperties.setInterfaceName(lp.getInterfaceName());
        ipv4LastDns = lp.getDnses();

        Log.i(TAG, "runDhcp compAddres:" + compAddres);
        Log.i(TAG, "runDhcp compRoutes:" + compRoutes);
        Log.i(TAG, "runDhcp compDnses:" + compDnses);

        if((compAddres != null) && compAddres.added.size() != 0) {
            for(LinkAddress l :compAddres.added)
                if(!zero_addr.equals(l.getAddress()))
                mLinkProperties.addLinkAddress(l);
        }

        if((compRoutes != null) && compRoutes.added.size() != 0) {
            for(RouteInfo r :compRoutes.added) {
                Log.i(TAG, "route interface:" + r.getInterface());
                try {
                    RouteInfo tmp = new RouteInfo(r.getDestination(), r.getGateway(), lp.getInterfaceName());
                    mLinkProperties.addRoute(tmp);
                }catch(IllegalArgumentException e) {
                    Log.e(TAG, "setIpv4LinkProperties addRoute failed");
                }
            }
        }

        if((compDnses != null) && compDnses.added.size() != 0) {
            for(InetAddress d :compDnses.added)
                if(!zero_addr.equals(d))
                    mLinkProperties.addDns(d);
        }

        /* pppoe switch to dhcp may drop dhcpv6 info */
        if (mIpv6ConnectStatus == EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT)
            setDhcpv6LinkProperties(mIface);

        Log.i(TAG, "runDhcp mIpv4LinkProperties:" + lp);
        Log.i(TAG, "runDhcp mLinkProperties:" + mLinkProperties);
    }

    Handler mEthernetAutoReconnectHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            Log.i(TAG, "Before Ethernet Auto Reconnect , mConnectStatus is:" + mConnectStatus +
                " mIpv6ConnectStatus:" + mIpv6ConnectStatus + ", what:" + what + ", arg1:" + msg.arg1 +
                ", mode:" + mEthernetManager.getEthernetMode());
            switch(what) {
            //add for chinamobile
            case EthernetManager.DHCP6_AUTORUN:
                if(DEBUG) Log.d(TAG, "stateless auto run");
                if(EthernetManager.IPV6_STATE_ENABLED != mEthernetManager.getIpv6PersistedState() || mIpv6ConnectStatus == EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT){
                    Log.i(TAG,"exit DHCP6_AUTORUN, getIpv6PersistedState()="+mEthernetManager.getIpv6PersistedState()+" mIpv6ConnectStatus="+mIpv6ConnectStatus);
                    return;
                }
                if(mIpv6ConnectStatus == EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECTING){
                    dhcp6AutoConnect();
                }else{
                    startIpv6();
                }
                break;
            case EthernetManager.ETHERNET_AUTORECONNECT:
                if(EthernetManager.ETHERNET_AUTORECONNECT_ENABLED != mEthernetManager.getAutoReconnectState()) {
                    Log.i(TAG, "AutoReconnect Function is not Enabled!");
                    return;
                }
                if(EthernetManager.EVENT_DHCPV6_AUTORECONNECTING == msg.arg1 &&
                        mIpv6ConnectStatus != EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT &&
                        mIpv6ConnectStatus != EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECTING &&
                        EthernetManager.ETHERNET_CONNECT_MODE_DHCP.equals(mEthernetManager.getEthernetMode6())) {
                    if(mEthernetManager.IPV6_STATE_DISABLED == mEthernetManager.getIpv6PersistedState()) {
                        Log.i(TAG, "exit DHCPV6 AutoReconnect, getIpv6PersistedState()="+mEthernetManager.getIpv6PersistedState());
                        return;
                    }
                    stopIpv6();
                    Log.i(TAG, "DHCPV6 Auto Reconnect!");
                    postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                     EthernetManager.EXTRA_ETHERNET_STATE,
                                     EthernetManager.EVENT_DHCPV6_AUTORECONNECTING,
                                     null, null);

                    startIpv6();
                } else if (EthernetManager.EVENT_DHCP_AUTORECONNECTING == msg.arg1 &&
                        mConnectStatus != EthernetManager.ETHERNET_CONNECT_STATE_CONNECT &&
                        mConnectStatus != EthernetManager.ETHERNET_CONNECT_STATE_CONNECTING &&
                        EthernetManager.ETHERNET_CONNECT_MODE_DHCP.equals(mEthernetManager.getEthernetMode())) {
                    if(mEthernetManager.IPV4_STATE_DISABLED == mEthernetManager.getIpv4PersistedState()) {
                        Log.i(TAG, "exit DHCP AutoReconnect, getIpv4PersistedState()="+mEthernetManager.getIpv4PersistedState());
                        return;
                    }
                    teardown();
                    Log.i(TAG, "DHCP Auto Reconnect!");
                    postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                                     EthernetManager.EXTRA_ETHERNET_STATE,
                                     EthernetManager.EVENT_DHCP_AUTORECONNECTING,
                                     null, null);
                    reconnect();
                } else if (PppoeManager.EVENT_AUTORECONNECTING == msg.arg1 &&
                        mConnectStatus != PppoeManager.PPPOE_STATE_CONNECT &&
                        mConnectStatus != PppoeManager.PPPOE_STATE_CONNECTING &&
                        EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mEthernetManager.getEthernetMode())) {
                    if(mEthernetManager.IPV4_STATE_DISABLED == mEthernetManager.getIpv4PersistedState()) {
                        Log.i(TAG, "exit PPPOE AutoReconnect, getIpv4PersistedState()="+mEthernetManager.getIpv4PersistedState());
                        return;
                    }
                    teardown();
                    Log.i(TAG, "PPPOE Auto Reconnect!");
                    postNotification(PppoeManager.PPPOE_STATE_CHANGED_ACTION,
                                     PppoeManager.EXTRA_PPPOE_STATE,
                                     PppoeManager.EVENT_AUTORECONNECTING,
                                     null, null);
                    reconnect();
                } else {
                    Log.d(TAG, "Auto Reconnect Message Error, msg.arg1:" + msg.arg1 +
                        ", mConnectStatus is:" + mConnectStatus + ", mIpv6ConnectStatus:" + mIpv6ConnectStatus);
                }
                break;

            case EthernetManager.ETHERNET_RECONNECT_ONCE:
                if(EthernetManager.ETHERNET_AUTORECONNECT_ENABLED != mEthernetManager.getAutoReconnectState()) {
                    Log.i(TAG, "AutoReconnect Function is not Enabled!");
                    return;
                }
                if(EthernetManager.EVENT_DHCPV6_AUTORECONNECTING == msg.arg1 &&
                        mIpv6ConnectStatus != EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT &&
                        mIpv6ConnectStatus != EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECTING) {
                    if(mEthernetManager.IPV6_STATE_DISABLED == mEthernetManager.getIpv6PersistedState()) {
                        Log.i(TAG, "exit DHCPV6 AutoReconnectOnce, getIpv6PersistedState()="+mEthernetManager.getIpv6PersistedState());
                        return;
                    }
                    stopIpv6();
                    Log.i(TAG, "DHCPV6 Auto Reconnect Once!");
                    postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                     EthernetManager.EXTRA_ETHERNET_STATE,
                                     EthernetManager.EVENT_DHCPV6_AUTORECONNECTING,
                                     null, null);
                    startIpv6();
                } else if (EthernetManager.EVENT_DHCP_AUTORECONNECTING == msg.arg1 &&
                        mConnectStatus != EthernetManager.ETHERNET_CONNECT_STATE_CONNECT &&
                        mConnectStatus != EthernetManager.ETHERNET_CONNECT_STATE_CONNECTING) {
                    if(mEthernetManager.IPV4_STATE_DISABLED == mEthernetManager.getIpv4PersistedState()) {
                        Log.i(TAG, "exit DHCP AutoReconnect, getIpv4PersistedState()="+mEthernetManager.getIpv4PersistedState());
                        return;
                    }
                    teardown();
                    Log.i(TAG, "DHCP Auto Reconnect Once!");
                    postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                                     EthernetManager.EXTRA_ETHERNET_STATE,
                                     EthernetManager.EVENT_DHCP_AUTORECONNECTING,
                                     null, null);
                    reconnect();
                } else if (PppoeManager.EVENT_AUTORECONNECTING == msg.arg1 &&
                        mConnectStatus != PppoeManager.PPPOE_STATE_CONNECT &&
                        mConnectStatus != PppoeManager.PPPOE_STATE_CONNECTING) {
                    if(mEthernetManager.IPV4_STATE_DISABLED == mEthernetManager.getIpv4PersistedState()) {
                        Log.i(TAG, "exit PPPOE AutoReconnectOnce, getIpv4PersistedState()="+mEthernetManager.getIpv4PersistedState());
                        return;
                    }
                    teardown();
                    Log.i(TAG, "PPPOE Auto Reconnect Once!");
                    postNotification(PppoeManager.PPPOE_STATE_CHANGED_ACTION,
                                     PppoeManager.EXTRA_PPPOE_STATE,
                                     PppoeManager.EVENT_AUTORECONNECTING,
                                     null, null);
                    reconnect();
                } else {
                    Log.d(TAG, "Auto Reconnect Once Message Error!!");
                }
                break;
            case EthernetManager.ETHERNET_PPPOE_STATELESS6_AUTORUN:
                if (mIpv6ConnectStatus == EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT) {
                    Log.d(TAG, "exit reconnect, already connect");
                    return;
                }

                if (mIpv6ConnectStatus == EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECTING) {
                    pppoeStateless6AutoConnect();
                    return;
                }

                Log.d(TAG, "reconnect again for pppoe stateless ipv6");
                runPppoe();
                break;
            }
            super.handleMessage(msg);
        }
    };

    private void postNotification(String action, String key, int event, String errKey, String errMsg) {
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        final Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        if ((key != null) && (!"".equals(key)))
            intent.putExtra(key, event);
        if((errKey != null) && (errMsg != null) && (!"".equals(errKey)))
            intent.putExtra(errKey, errMsg);
        mContext.sendStickyBroadcast(intent);
    }
    //add for chinamobile
    private void postNotification(String action, String key, int event, String errKey, int errMsg) {
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        final Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        if ((key != null) && (!"".equals(key)))
            intent.putExtra(key, event);
        if((errKey != null) && (!"".equals(errKey)))
            intent.putExtra(errKey, errMsg);
        mContext.sendStickyBroadcast(intent);
    }

    public static synchronized EthernetDataTracker getInstance() {
        if (sInstance == null) sInstance = new EthernetDataTracker();
        return sInstance;
    }

    public Object Clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public void setTeardownRequested(boolean isRequested) {
        mTeardownRequested.set(isRequested);
    }

    public boolean isTeardownRequested() {
        return mTeardownRequested.get();
    }

    /**
     * Begin monitoring connectivity
     */
    public void startMonitoring(Context context, Handler target) {
        mContext = context;
        mCsHandler = target;

        // register for notifications from NetworkManagement Service
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        mNMService = INetworkManagementService.Stub.asInterface(b);
        mInterfaceObserver = new InterfaceObserver(this);

        mNetworkManagementServiceBinder = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        mNetworkManagementService = INetworkManagementService.Stub.asInterface(mNetworkManagementServiceBinder);

        sIfaceMatch = mContext.getResources().getString(
            com.android.internal.R.string.config_ethernet_iface_regex);

        mEthernetManager = (EthernetManager) mContext.getSystemService(Context.ETHERNET_SERVICE);
        mHiEthernetManager = (HiEthernetManager) mContext.getSystemService(Context.HI_ETHERNET_SERVICE);
        mPppoeManager = (PppoeManager) mContext.getSystemService(Context.PPPOE_SERVICE);

        setInterfaceFromDatabase();
    }

    public void setInterfaceFromDatabase() {
        mIface = mEthernetManager.getInterfaceName();
        mLinkUp = false;
    }

    public String[] getEthernetInterfaceNameList() {
        try {
            final String[] ifaces = mNetworkManagementService.listInterfaces();

            int count = 0;
            if ((count = EthernetNative.getInterfaceCnt()) != 0) {
                Log.i(TAG, "total found " + count + " net devices");
                mEthernetInterfaceNameList = new String[count];
            }

            int i = 0;
            for (String iface : ifaces) {
                if (checkInterfaceName(iface)) {
                    mEthernetInterfaceNameList[i] = iface;
                    i++;
                    if(i == count) break;
                }
            }
            return mEthernetInterfaceNameList;
        } catch (RemoteException e) {
            Log.e(TAG, "Could not get list of interfaces " + e);
            return null;
        }
    }

    public boolean checkInterfaceName(String iface) {
        return iface.matches(sIfaceMatch);
    }

    /**
     * Disable connectivity to a network
     * TODO: do away with return value after making MobileDataStateTracker async
     */
    public boolean teardown() {
        //close ethernet setting need release ipv6 information
        //close ipv4 related setting dont release ipv6 information
        if(EthernetManager.ETHERNET_STATE_DISABLED == mEthernetManager.getEthernetPersistedState())
        {
            stopIpv6();
        }

        mConnectStatus = EthernetManager.ETHERNET_CONNECT_STATE_DISCONNECTING;

        if(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL.equals(mEthernetManager.getEthernetMode())
        || EthernetManager.ETHERNET_CONNECT_MODE_NONE.equals(mEthernetManager.getEthernetMode())) {
            if(DEBUG) Log.d(TAG, "teardown Static Network, Nothing needs to do here, just send success message!");
            if(DEBUG) Log.d(TAG, "teardown success, send message EthernetManager.EVENT_STATIC_DISCONNECT_SUCCESSED");
            if(mLinkUp != false || "shcmcc".equals(SystemProperties.get("ro.product.target", "aosp"))){
                postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_STATIC_DISCONNECT_SUCCESSED,
                                 null, null);
                postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_IPV4_DISCONNECT_SUCCESSED,
                                 null, null);

            }
            mConnectStatus = EthernetManager.ETHERNET_CONNECT_STATE_DISCONNECT;
        } else if(EthernetManager.ETHERNET_CONNECT_MODE_DHCP.equals(mEthernetManager.getEthernetMode())) {
            if (isNeedRelease(getDhcpMode(), 4)) {
                Log.i(TAG, "releaseDhcpLease " + mIface);
                NetworkUtils.releaseDhcpLease(mIface);
                Log.i(TAG, "releaseDhcpLease " + mIface + " done");
            }
            if(NetworkUtils.stopDhcp(mIface)) {
                if(DEBUG) Log.d(TAG, "teardown success, send message EthernetManager.EVENT_DHCP_DISCONNECT_SUCCESSED");
                if(mLinkUp != false || "shcmcc".equals(SystemProperties.get("ro.product.target", "aosp"))){
                    postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                                     EthernetManager.EXTRA_ETHERNET_STATE,
                                     EthernetManager.EVENT_DHCP_DISCONNECT_SUCCESSED,
                                     null, null);
                    postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                     EthernetManager.EXTRA_ETHERNET_STATE,
                                     EthernetManager.EVENT_IPV4_DISCONNECT_SUCCESSED,
                                     null, null);
                }
                mConnectStatus = EthernetManager.ETHERNET_CONNECT_STATE_DISCONNECT;
            } else {
                Log.e(TAG, "teardown fail, send message EthernetManager.EVENT_DHCP_DISCONNECT_FAILED");
                if(mLinkUp != false){
                    postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                                     EthernetManager.EXTRA_ETHERNET_STATE,
                                     EthernetManager.EVENT_DHCP_DISCONNECT_FAILED,
                                     null, null);
                    postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                     EthernetManager.EXTRA_ETHERNET_STATE,
                                     EthernetManager.EVENT_IPV4_DISCONNECT_FAILED,
                                     null, null);
                }
                mConnectStatus = EthernetManager.ETHERNET_CONNECT_STATE_DISCONNECT;
            }
        } else if(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mEthernetManager.getEthernetMode())) {
            if( EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mEthernetManager.getEthernetMode6())){//if pppoe ipv6 also on
                if( !mEthernetManager.isIpv6PppoeStateless() ){ //statefull
                    NetworkUtils.stopDhcpv6("ppp0");
                    postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                     EthernetManager.EXTRA_ETHERNET_STATE,
                                     EthernetManager.EVENT_PPPOE_DHCPV6_DISCONNECT_SUCCESSED,
                                     null, null);
                }else{ //stateless
                    mPPPoeStatelessIpAddress = "";
                    mPPPoeStatelessIpv6Dns1 = "";
                    mPPPoeStatelessIpv6Dns2 = "";
                    mPPPoeStatelessPrefixlength = 0;
                    postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                     EthernetManager.EXTRA_ETHERNET_STATE,
                                     EthernetManager.EVENT_PPPOE_STATELESS6_DISCONNECT_SUCCESSED,
                                     null, null);
                }
                mIpv6ConnectStatus = EthernetManager.ETHERNET_IPV6_CONNECT_STATE_DISCONNECT;
            }
            PppoeNative.disconnectPppoe(mIface);

            if(DEBUG) Log.d(TAG, "teardown success, send message PppoeManager.EVENT_DISCONNECT_SUCCESSED");
            postNotification(PppoeManager.PPPOE_STATE_CHANGED_ACTION,
                             PppoeManager.EXTRA_PPPOE_STATE,
                             PppoeManager.EVENT_DISCONNECT_SUCCESSED,
                             null, null);
            postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                             EthernetManager.EXTRA_ETHERNET_STATE,
                             EthernetManager.EVENT_IPV4_DISCONNECT_SUCCESSED,
                             null, null);
            mConnectStatus = PppoeManager.PPPOE_STATE_DISCONNECT;
        } else {
            Log.d(TAG, "No such Ethernet config ip mode! Teardown Error! Return!");
            mConnectStatus = PppoeManager.PPPOE_STATE_DISCONNECT;
            return false;
        }

        NetworkUtils.clearIpv4Addresses(mIface);
        updateLinkProperties(REMOVE_IPV4_LINK);
        mDhcpInfo.clear();

        Message msg = null;

        if ((mIpv6ConnectStatus != EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT)
            && (mConnectStatus != EthernetManager.ETHERNET_CONNECT_STATE_CONNECT)
            && (mConnectStatus != PppoeManager.PPPOE_STATE_CONNECT)){
            mTeardownRequested.set(true);

            mNetworkInfo.setIsAvailable(false);
            mNetworkInfo.setDetailedState(DetailedState.DISCONNECTED, null, null);
            msg = mCsHandler.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
            msg.sendToTarget();
        } else {
            mTeardownRequested.set(false);

            mNetworkInfo.setIsAvailable(true);
            mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, null);
            msg = mCsHandler.obtainMessage(EVENT_CONFIGURATION_CHANGED, mNetworkInfo);
            msg.sendToTarget();
        }

        return true;
    }

    /*--------------------------------------------------------------------------*/
    public boolean Qbteardown() {
       if(NetworkUtils.stopDhcp(mIface)) {
            if(DEBUG) Log.d(TAG, "----teardown success, send message EthernetManager.EVENT_DHCP_DISCONNECT_SUCCESSED");
            if(mLinkUp != false){
                postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_DHCP_DISCONNECT_SUCCESSED,
                                 null, null);
                postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_IPV4_DISCONNECT_SUCCESSED,
                                 null, null);
            }
            mConnectStatus = EthernetManager.ETHERNET_CONNECT_STATE_DISCONNECT;
        } else {
            Log.e(TAG, "----teardown fail, send message EthernetManager.EVENT_DHCP_DISCONNECT_FAILED");
            if(mLinkUp != false){
                postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_DHCP_DISCONNECT_FAILED,
                                 null, null);
                postNotification(EthernetManager.IPV4_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_IPV4_DISCONNECT_FAILED,
                                 null, null);
            }
            mConnectStatus = EthernetManager.ETHERNET_CONNECT_STATE_DISCONNECT;
            return false;
        }

        NetworkUtils.clearIpv4Addresses(mIface);
        updateLinkProperties(REMOVE_IPV4_LINK);
        mDhcpInfo.clear();

        Message msg = null;

        if ((mIpv6ConnectStatus != EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT)
            && (mConnectStatus != EthernetManager.ETHERNET_CONNECT_STATE_CONNECT)
            && (mConnectStatus != PppoeManager.PPPOE_STATE_CONNECT)){
            mTeardownRequested.set(true);

            mNetworkInfo.setIsAvailable(false);
            mNetworkInfo.setDetailedState(DetailedState.DISCONNECTED, null, null);
            msg = mCsHandler.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
            msg.sendToTarget();
        } else {
            mTeardownRequested.set(false);

            mNetworkInfo.setIsAvailable(true);
            mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, null);
            msg = mCsHandler.obtainMessage(EVENT_CONFIGURATION_CHANGED, mNetworkInfo);
            msg.sendToTarget();
        }

        return true;
    }
    private String strCurrentConnectState(){
        String state = "";
        switch(mConnectStatus){
            case EthernetManager.ETHERNET_CONNECT_STATE_DISCONNECT:
                state = "ETHERNET_CONNECT_STATE_DISCONNECT";
                break;
            case EthernetManager.ETHERNET_CONNECT_STATE_CONNECT:
                state = "ETHERNET_CONNECT_STATE_CONNECT";
                break;
            case EthernetManager.ETHERNET_CONNECT_STATE_CONNECTING:
                state = "ETHERNET_CONNECT_STATE_CONNECTING";
                break;
            case EthernetManager.ETHERNET_CONNECT_STATE_DISCONNECTING:
                state = "ETHERNET_CONNECT_STATE_DISCONNECTING";
                break;
            case EthernetManager.ETHERNET_CONNECT_STATE_UNKNOWN:
                state = "ETHERNET_CONNECT_STATE_UNKNOWN";
                break;
            case PppoeManager.PPPOE_STATE_DISCONNECT:
                state = "PPPOE_STATE_DISCONNECT";
                break;
            case PppoeManager.PPPOE_STATE_CONNECT:
                state = "PPPOE_STATE_CONNECT";
                break;
            case PppoeManager.PPPOE_STATE_CONNECTING:
                state = "PPPOE_STATE_CONNECTING";
                break;
            case PppoeManager.PPPOE_STATE_UNKNOWN:
                state = "PPPOE_STATE_UNKNOWN";
                break;
            default:
                state = "UNRECOGNISE_STATE";
                break;
        }
        return state;
    }

    /**
     * Re-enable connectivity to a network after a {@link #teardown()}.
     */
    public boolean reconnect() {
        if(DEBUG) Log.d(TAG, "reconnect() Check PHY Linkstatus!");
        if(DEBUG) Log.d(TAG, "mIface: " + mIface);
        if(DEBUG) Log.d(TAG, "mLinkUp: " + mLinkUp);
        if(DEBUG) Log.d(TAG, "mConnectStatus: " + mConnectStatus);

        if ((EthernetManager.ETHERNET_CONNECT_MODE_DHCP.equals(mEthernetManager.getEthernetMode()))
         || (EthernetManager.ETHERNET_CONNECT_MODE_MANUAL.equals(mEthernetManager.getEthernetMode())) ) {
            if ((mConnectStatus == EthernetManager.ETHERNET_CONNECT_STATE_CONNECT)
             || (mConnectStatus == EthernetManager.ETHERNET_CONNECT_STATE_CONNECTING)
             || (mConnectStatus == EthernetManager.ETHERNET_CONNECT_STATE_DISCONNECTING)) {
                Log.e(TAG, "reconnect() in dhcp mode not allowed, Ethernet state:" + strCurrentConnectState());
                return true;
            }
        } else if (EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mEthernetManager.getEthernetMode())) {
            if ((mConnectStatus == PppoeManager.PPPOE_STATE_CONNECT)
             || (mConnectStatus == PppoeManager.PPPOE_STATE_CONNECTING)) {
                Log.e(TAG, "reconnect() in pppoe mode not allowed, Ethernet state:" + strCurrentConnectState());
                return true;
            }
        } else
            return true;


        if(1 == NetworkUtils.getNetlinkStatus(mIface))
            mLinkUp = true;
        else
            mLinkUp = false;

        if(!mLinkUp) {
            postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                             EthernetManager.EXTRA_ETHERNET_STATE,
                             EthernetManager.EVENT_PHY_LINK_DOWN,
                             null, null);
            Log.e(TAG, "Physical Connection ERROR! please check your wire");
            if(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mEthernetManager.getEthernetMode()))
                mConnectStatus = PppoeManager.PPPOE_STATE_DISCONNECT;
            else
                mConnectStatus = EthernetManager.ETHERNET_CONNECT_STATE_DISCONNECT;
            return false;
        }
        if(mEthernetManager.getEthernetState() != EthernetManager.ETHERNET_STATE_ENABLED) {
            Log.e(TAG, "Ethernet is not Enabled");
            if(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mEthernetManager.getEthernetMode()))
                mConnectStatus = PppoeManager.PPPOE_STATE_DISCONNECT;
            else
                mConnectStatus = EthernetManager.ETHERNET_CONNECT_STATE_DISCONNECT;
            return false;
        }
        if ("shcmcc".equals(SystemProperties.get("ro.product.target", "aosp"))) {
            if (EthernetManager.IPV4_STATE_DISABLED == mEthernetManager.getIpv4PersistedState()) {
                Log.e(TAG, "ipv4 state is disabled");
                return false;
            }
        }

        mTeardownRequested.set(false);
        if(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL.equals(mEthernetManager.getEthernetMode())) {
            Log.i(TAG, "configure ethernet manually, run ConfigureInterface()");
            configureInterface();
        } else if(EthernetManager.ETHERNET_CONNECT_MODE_DHCP.equals(mEthernetManager.getEthernetMode())) {
            Log.i(TAG, "configure ethernet by dhcpcd, run runDchp()");
            runDhcp();
        } else if(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mEthernetManager.getEthernetMode())) {
            if(PppoeManager.PPPOE_STATE_CONNECTING != mConnectStatus) {
                String usedInterfaceName = SystemProperties.get("pppoe.ifname", "eth0");
                if ("wlan0".equals(usedInterfaceName)) {
                    Log.i(TAG, "disconnect wifi pppoe");
                    PppoeNative.disconnectPppoe("wlan0");
                    WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                    wifiManager.setWifiEnabled(false);
                }
                if (EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mEthernetManager.getEthernetMode6())) {
                    userGestureIpv6Stateless = true;
                    if (mEthernetManager.isIpv6PppoeStateless()) {
                        pppoeStateless6AutoConnect();
                    }
                }
                Log.i(TAG, "configure ethernet by pppoe, run runPppoe()");
                runPppoe();
            }
            else{
                Log.i(TAG, "Pppoe is already Running! Return!");
                return false;
            }
        }else if(EthernetManager.ETHERNET_CONNECT_MODE_NONE.equals(mEthernetManager.getEthernetMode())){
                Log.i(TAG, "configure ethernet none, return now");
                return false;
        }else {
            Log.i(TAG, "No such Ethernet config ip mode! Reconnect Error! Return!");
            return false;
        }

        return true;
    }


    @Override
    public void captivePortalCheckCompleted(boolean isCaptivePortal) {
        // not implemented
    }

    /**
     * Turn the wireless radio off for a network.
     * @param turnOn {@code true} to turn the radio on, {@code false}
     */
    public boolean setRadio(boolean turnOn) {
        return true;
    }

    /**
     * @return true - If are we currently tethered with another device.
     */
    public synchronized boolean isAvailable() {
        return mNetworkInfo.isAvailable();
    }

    @Override
    public void captivePortalCheckComplete() {
        // not implemented
    }
    /**
     * Tells the underlying networking system that the caller wants to
     * begin using the named feature. The interpretation of {@code feature}
     * is completely up to each networking implementation.
     * @param feature the name of the feature to be used
     * @param callingPid the process ID of the process that is issuing this request
     * @param callingUid the user ID of the process that is issuing this request
     * @return an integer value representing the outcome of the request.
     * The interpretation of this value is specific to each networking
     * implementation+feature combination, except that the value {@code -1}
     * always indicates failure.
     * TODO: needs to go away
     */
    public int startUsingNetworkFeature(String feature, int callingPid, int callingUid) {
        return -1;
    }

    /**
     * Tells the underlying networking system that the caller is finished
     * using the named feature. The interpretation of {@code feature}
     * is completely up to each networking implementation.
     * @param feature the name of the feature that is no longer needed.
     * @param callingPid the process ID of the process that is issuing this request
     * @param callingUid the user ID of the process that is issuing this request
     * @return an integer value representing the outcome of the request.
     * The interpretation of this value is specific to each networking
     * implementation+feature combination, except that the value {@code -1}
     * always indicates failure.
     * TODO: needs to go away
     */
    public int stopUsingNetworkFeature(String feature, int callingPid, int callingUid) {
        return -1;
    }

    @Override
    public void setUserDataEnable(boolean enabled) {
        Log.w(TAG, "ignoring setUserDataEnable(" + enabled + ")");
    }

    @Override
    public void setPolicyDataEnable(boolean enabled) {
        Log.w(TAG, "ignoring setPolicyDataEnable(" + enabled + ")");
    }

    /**
     * Check if private DNS route is set for the network
     */
    public boolean isPrivateDnsRouteSet() {
        return mPrivateDnsRouteSet.get();
    }

    /**
     * Set a flag indicating private DNS route is set
     */
    public void privateDnsRouteSet(boolean enabled) {
        mPrivateDnsRouteSet.set(enabled);
    }

    /**
     * Fetch NetworkInfo for the network
     */
    public synchronized NetworkInfo getNetworkInfo() {
        mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = mConnectivityManager.getWifiStateWithoutDisguise();
        //Log.d(TAG, "ethernet getNetworkInfo, coexist state:" + mEthernetManager.getWifiEthernetCoexistState()
        //    + ", wifi state:" + ((info != null)?info.getState():"null") + "wifi disguise: " +
        //    mEthernetManager.getWifiDisguiseState());
        if((mEthernetManager.WIFI_ETHERNET_COEXIST_ENABLED == mEthernetManager.getWifiEthernetCoexistState())
        &&(info != null) && (info.getState() == NetworkInfo.State.CONNECTED)){
            return new NetworkInfo(mNetworkInfo);
        }

        if(mEthernetManager.WIFI_DISGUISE_ENABLED ==
            mEthernetManager.getWifiDisguiseState()) {
            NetworkInfo disguisedNetworkInfo = new NetworkInfo(mNetworkInfo);
            if(disguisedNetworkInfo.isConnected()) {
                if(DEBUG) Log.d(TAG, "getNetworkInfo Wifi disguise!");
                disguisedNetworkInfo.setNetworkType(ConnectivityManager.TYPE_WIFI, "WIFI");
            }
            return disguisedNetworkInfo;
        } else {
            return new NetworkInfo(mNetworkInfo);
        }
    }

    /**
     * Fetch LinkProperties for the network
     */
    public synchronized LinkProperties getLinkProperties() {
        synchronized (mLinkProperties) {
            if(DEBUG) Log.i(TAG,"getLinkProperties()");
            return new LinkProperties(mLinkProperties);
        }
    }

   /**
     * A capability is an Integer/String pair, the capabilities
     * are defined in the class LinkSocket#Key.
     *
     * @return a copy of this connections capabilities, may be empty but never null.
     */
    public LinkCapabilities getLinkCapabilities() {
        return new LinkCapabilities(mLinkCapabilities);
    }

    /**
     * Fetch default gateway address for the network
     */
    public int getDefaultGatewayAddr() {
        return mDefaultGatewayAddr.get();
    }

    /**
     * Check if default route is set
     */
    public boolean isDefaultRouteSet() {
        return mDefaultRouteSet.get();
    }

    /**
     * Set a flag indicating default route is set for the network
     */
    public void defaultRouteSet(boolean enabled) {
        mDefaultRouteSet.set(enabled);
    }

    /**
     * Return the system properties name associated with the tcp buffer sizes
     * for this network.
     */
    public String getTcpBufferSizesPropName() {
        return "net.tcp.buffersize.wifi";
    }

    public void setDependencyMet(boolean met) {
        // not supported on this network
    }
    public boolean stopInterface() {
        if(DEBUG) Log.d(TAG, "stopInterface mIface:" + mIface);
        interfaceRemoved(mIface);
        return true;
    }

    public boolean resetInterface() {
        if(SystemProperties.get("persist.sys.qb.enable","false").equals("true")){
            mIface = mEthernetManager.getInterfaceName();
        }
        interfaceAdded(mIface);
        if(DEBUG) Log.d(TAG, "resetInterface:" + mIface);
        return true;
    }

    public synchronized DhcpInfo getDhcpInfo() {
        return new DhcpInfo(mDhcpInfo);
    }

    public synchronized int getConnectStatue() {
        return mConnectStatus;
    }

    @Override
    public void addStackedLink(LinkProperties link) {
        mLinkProperties.addStackedLink(link);
    }

    @Override
    public void removeStackedLink(LinkProperties link) {
        mLinkProperties.removeStackedLink(link);
    }

    @Override
    public void supplyMessenger(Messenger messenger) {
        // not supported on this network
    }

    private void runPPPoeDhcpv6() {
        Thread dhcpv6PppoeThread = new Thread(new Runnable() {
            public void run() {
                mIpv6ConnectStatus = EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECTING;
                Log.d(TAG,"--------ppp0 runPPPoeDhcpv6 1 " );
                ipv6AddrsUpdate.clear();
                if(!NetworkUtils.runDhcpv6("ppp0")){
                    Log.e(TAG, "PPPoeDHCPv6 request error:" + NetworkUtils.getDhcpv6Error());
                    if (mIpv6ConnectStatus == EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT) {
                        Log.d(TAG, "run pppoedhcpv6 return as already connected");
                        return;
                    }
                    if(mLinkUp != false)
                        postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                         EthernetManager.EXTRA_ETHERNET_STATE,
                                         EthernetManager.EVENT_PPPOE_DHCPV6_CONNECT_FAILED,
                                         null, null);
                    mIpv6ConnectStatus = EthernetManager.ETHERNET_IPV6_CONNECT_STATE_DISCONNECT;
                    NetworkUtils.stopDhcpv6("ppp0");
                    Message msg_reconnect = new Message();
                    msg_reconnect.what = EthernetManager.ETHERNET_AUTORECONNECT;
                    msg_reconnect.arg1 = EthernetManager.EVENT_DHCPV6_AUTORECONNECTING;
                    mEthernetAutoReconnectHandler.sendMessageDelayed(msg_reconnect, EthernetManager.ETHERNET_RECONNECT_TIME);
                    return;
                }
                setDhcpv6LinkProperties("ppp0");

                mNetworkInfo.setIsAvailable(true);
                mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, null);
                Message msg = mCsHandler.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
                msg.sendToTarget();

                if (waitingIPv6AddrsUpdate("ppp0")) {
                if(DEBUG) Log.d(TAG, "send EthernetManager.EVENT_PPPOE_DHCPV6_CONNECT_SUCCESSED in runPPPoeDhcpv6()");
                mIpv6ConnectStatus = EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT;
                postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_PPPOE_DHCPV6_CONNECT_SUCCESSED,
                                 null, null);
                if(DEBUG) Log.d(TAG, "set pppoe.ipv6.result 118");
                SystemProperties.set(IPV6_PPP_RESULT, PPPOE_CONNECT_ONLY_PPPOE_IPV6); //for only ipv6 pppoe,no ipv4, 118
            }
            }
        });
        dhcpv6PppoeThread.start();
    }

    private boolean waitingLinklocal6(String iface) {
        if (mLinklocal6ProbeRunning == true){
            Log.d(TAG, "WaitingLinkLocal6 is running, return...");
            return false;
        }
        mLinklocal6ProbeRunning = true;

        Log.d(TAG, "IPV6 Link-local address: " + NetworkUtils.getIpv6LinklocalAddress(iface)
                + " , interface = " + iface);
        while(NetworkUtils.getIpv6LinklocalAddress(iface) == null){
            try {
                Log.d(TAG, "Sleep to wait for IPV6 Link-local address, command dhclient -6 need it...");
                Thread.sleep(300);
                if(!mLinklocal6ProbeRunning){
                    return false;
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return true;
    }

    private boolean waitingIPv6AddrsUpdate(String iface) {
        String ipv6Addr = SystemProperties.get("dhclient.ipaddress." + iface);
        String prefixLen = SystemProperties.get("dhclient.prefixlen." + iface);
        ipv6Addr = ipv6Addr + "/" + prefixLen;
        if (DEBUG) Log.d(TAG, "waitingIPv6AddrsUpdate: iface = " + iface + ", ipv6Addr = " + ipv6Addr);
        if (TextUtils.isEmpty(ipv6Addr)) {
            return false;
        }
        int tryNumber = 0;
        while (!ipv6AddrsUpdate.contains(ipv6Addr)) {
            try {
                if (DEBUG) Log.d(TAG, "waitingIPv6AddrsUpdate: tryNumber = " + tryNumber);
                Thread.sleep(500); // set 500ms as interval time
                if (mIpv6ConnectStatus != EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECTING) {
                    Log.e(TAG, "waitingIPv6AddrsUpdate failed: mIpv6ConnectStatus = " + mIpv6ConnectStatus);
                    return false;
                }
                tryNumber++;
                if (tryNumber > 6) { // set the max try times as 6
                    return false;
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "waitingIPv6AddrsUpdate failed: " + e);
            }
        }
        return true;
    }

    /* MEDIASTB-6520
     * if dhcpv4 && dhcpv6 startup at the same time,
     * some dhcp server will not reply to our dhcp request.
     * WARN: this request that runDhcp is called before runDhcpv6.
     */
    private long getDhcpv6DelayTime() {
        String reqDelayStr = SystemProperties.get("persist.ethernet.dhcpv6.delayms", "0");
        int    reqDelayMs  = Integer.parseInt(reqDelayStr);
        long   delayMs = 0;

        if (reqDelayMs <= 0 || mDhcpv4StartTime < 0) {
            return 0;
        }
        delayMs = reqDelayMs - (SystemClock.elapsedRealtime() - mDhcpv4StartTime);
        if (delayMs < 0) {
            delayMs = 0;
        }

        return delayMs;
     }

    private void runDhcpv6() {
        long delayMs = getDhcpv6DelayTime();

        Log.i(TAG, "runDhcpv6 in, delayMs=" + delayMs);
        synchronized(this) {
            if (mDhcpv6DelayTimer == null) {
                mDhcpv6DelayTimer = new Timer();
            }
            mDhcpv6DelayTimer.schedule(new TimerTask(){
                public void run() {
                    Log.i(TAG, "runDhcpv6 start");
                    mIpv6ConnectStatus = EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECTING;
                    if(!waitingLinklocal6(mIface)){ // this may sleep for a while when Link-local not ready.
                        return;
                    }
                    mLinklocal6ProbeRunning = false;
                    if (isNeedRelease(getDhcpMode(), 6)) {
                        Log.i(TAG, "releaseDhcpv6lease " + mIface);
                        NetworkUtils.releaseDhcpv6lease(mIface);
                        Log.i(TAG, "releaseDhcpv6lease " + mIface + " done");
                        if (mIpv6ConnectStatus != EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECTING) {
                            Log.e(TAG, "mIpv6ConnectStatus is not connecting after release, no do dhcpv6 anymore");
                            return;
                        }
                    }
                    int option60 = mEthernetManager.getDhcpOption60State();
                    String login = mEthernetManager.getDhcpOption60Login();
                    String password = mEthernetManager.getDhcpOption60Password();
                    int option125 = mEthernetManager.getDhcpOption125State();
                    String option125Info = mEthernetManager.getDhcpOption125Info();
                    ipv6AddrsUpdate.clear();
                    if(!NetworkUtils.runDhcpv6Plus(mIface, option60, login, password, option125, option125Info)){
                        Log.e(TAG, "DHCPv6 request error:" + NetworkUtils.getDhcpv6Error());
                        if (mIpv6ConnectStatus == EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT) {
                            Log.d(TAG, "run dhcpv6 return as already connected");
                            return;
                        }
                        if(mLinkUp != false){
                            if(isCMCCbaseMode) {
                                postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                                 EthernetManager.EVENT_IPV6_CONNECT_FAILED,
                                                 null,null);
                            } else {
                                postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                                 EthernetManager.EVENT_DHCPV6_CONNECT_FAILED,
                                                 null, null);
                            }
                        }
                        mIpv6ConnectStatus = EthernetManager.ETHERNET_IPV6_CONNECT_STATE_DISCONNECT;
                        NetworkUtils.stopDhcpv6(mIface);
                        Message msg_reconnect = new Message();
                        msg_reconnect.what = EthernetManager.ETHERNET_AUTORECONNECT;
                        msg_reconnect.arg1 = EthernetManager.EVENT_DHCPV6_AUTORECONNECTING;
                        mEthernetAutoReconnectHandler.sendMessageDelayed(msg_reconnect, EthernetManager.ETHERNET_RECONNECT_TIME);
                        return;
                    }
                    setDhcpv6LinkProperties(mIface);

                    mNetworkInfo.setIsAvailable(true);
                    mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, null);
                    Message msg = mCsHandler.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
                    msg.sendToTarget();

                    if (waitingIPv6AddrsUpdate(mIface)) {
                        if(DEBUG) Log.d(TAG, "send EthernetManager.EVENT_DHCPV6_CONNECT_SUCCESSED in runDhcpv6()");
                        mIpv6ConnectStatus = EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT;
                        if(isCMCCbaseMode) {
                            postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                    EthernetManager.EXTRA_ETHERNET_STATE,
                                    EthernetManager.EVENT_IPV6_CONNECT_SUCCESSED,
                                    null, null);
                        } else {
                            postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                    EthernetManager.EXTRA_ETHERNET_STATE,
                                    EthernetManager.EVENT_DHCPV6_CONNECT_SUCCESSED,
                                    null, null);
                        }
                    }
                }
            }, delayMs);
        }
        Log.i(TAG, "runDhcpv6 out");
    }

    public void startIpv6() {
        if(SystemProperties.get("persist.sys.qb.enable","false").equals("true")){
            mIface = mEthernetManager.getInterfaceName();
        }

        if(!checkInterfaceName(mIface))
            return;

        if(1 == NetworkUtils.getNetlinkStatus(mIface))
            mLinkUp = true;
        else
            mLinkUp = false;

        if(!mLinkUp) {
            mIpv6ConnectStatus = EthernetManager.ETHERNET_IPV6_CONNECT_STATE_DISCONNECT;
            postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                             EthernetManager.EXTRA_ETHERNET_STATE,
                             EthernetManager.EVENT_PHY_LINK_DOWN,
                             null, null);
            postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                             EthernetManager.EXTRA_ETHERNET_STATE,
                             EthernetManager.EVENT_PHY_LINK_DOWN,
                             null, null);
            Log.e(TAG, "Physical Connection ERROR! please check your wire");
            return ;
        }

        //clear IPV6 addr
        NetworkUtils.clearIpv6Addresses(mIface);
        clearStatelessAddresses();

        if(EthernetManager.IPV6_STATE_ENABLED == mEthernetManager.getIpv6PersistedState()){
            if(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL.equals(mEthernetManager.getEthernetMode6())) {
                enableStateless(mIface, false);
                String address6 = mEthernetManager.getIpv6DatabaseAddress();
                String gateway6 = mEthernetManager.getIpv6DatabaseGateway();
                String dns6_1 = mEthernetManager.getIpv6DatabaseDns1();
                String dns6_2 = mEthernetManager.getIpv6DatabaseDns2();
                int prefixlen6 = mEthernetManager.getIpv6DatabasePrefixlength();
                String dns6[] = new String[2];

                ipv6LastDns = new ArrayList<InetAddress>();
                if(null != address6 && null != gateway6 && null != dns6_1) {
                    if(NetworkUtils.configure6Interface(mIface, address6, prefixlen6, gateway6, dns6_1, dns6_2)) {
                        if (mLinkProperties != null) {
                            if((mConnectStatus != EthernetManager.ETHERNET_CONNECT_STATE_CONNECT)
                                && (mConnectStatus != PppoeManager.PPPOE_STATE_CONNECT)){
                                synchronized (mLinkProperties) {
                                    if(DEBUG) Log.i(TAG,"mLinkProperties.clear() in startIpv6()");
                                    mLinkProperties.clear();
                                }
                            }
                            String linkInterface = mLinkProperties.getInterfaceName();
                            if (linkInterface == null || !mIface.equals(linkInterface))
                                mLinkProperties.setInterfaceName(mIface);
                            if((address6 != null) && !("".equals(address6)))
                                mLinkProperties.addLinkAddress(new LinkAddress(NetworkUtils.numericToInetAddress(address6), prefixlen6));
                            if((dns6_1 != null) && !("".equals(dns6_1))) {
                                mLinkProperties.addDns(NetworkUtils.numericToInetAddress(dns6_1));
                                ipv6LastDns.add(NetworkUtils.numericToInetAddress(dns6_1));
                            }
                            if((dns6_2 != null) && !("".equals(dns6_2))) {
                                mLinkProperties.addDns(NetworkUtils.numericToInetAddress(dns6_2));
                                ipv6LastDns.add(NetworkUtils.numericToInetAddress(dns6_2));
                            }
                            if((gateway6 != null) && !("".equals(gateway6)))
                                mLinkProperties.addRoute(new RouteInfo(NetworkUtils.numericToInetAddress(gateway6)));
                        }

                        Log.i(TAG, "startIpv6 After add, mLinkProperties" + mLinkProperties);

                        mTeardownRequested.set(false);

                        mNetworkInfo.setIsAvailable(true);
                        mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, null);
                        Message msg = mCsHandler.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
                        msg.sendToTarget();

                        mIpv6ConnectStatus = EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT;
                        if(isCMCCbaseMode) {
                            postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                             EthernetManager.EXTRA_ETHERNET_STATE,
                                             EthernetManager.EVENT_IPV6_CONNECT_SUCCESSED,
                                             null, null);
                        } else {
                            postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                             EthernetManager.EXTRA_ETHERNET_STATE,
                                             EthernetManager.EVENT_STATIC6_CONNECT_SUCCESSED,
                                             null, null);
                        }
                        Log.i(TAG, "IPV6 static ip configure success!!");
                    } else {
                        mIpv6ConnectStatus = EthernetManager.ETHERNET_IPV6_CONNECT_STATE_DISCONNECT;
                        if(isCMCCbaseMode) {
                            postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                             EthernetManager.EXTRA_ETHERNET_STATE,
                                             EthernetManager.EVENT_IPV6_CONNECT_FAILED,
                                             null, null);
                        } else {
                            postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                             EthernetManager.EXTRA_ETHERNET_STATE,
                                             EthernetManager.EVENT_STATIC6_CONNECT_FAILED,
                                             null, null);
                        }
                        Log.e(TAG, "IPV6 static ip configure fail!!");
                    }
                }
            }else if(EthernetManager.ETHERNET_CONNECT_MODE_STATELESS.equals(mEthernetManager.getEthernetMode6())) {
                enableStateless(mIface, true);
                //auto run dhcpv6(if not connected after 5s)
                if(DEBUG) Log.d(TAG, "stateless start");
                dhcp6AutoConnect();
                if(mNMService != null){
                    try {
                        mNMService.disableIpv6(mIface);
                        userGestureIpv6Stateless = true;
                        mNMService.enableIpv6(mIface);
			if(SystemProperties.get("ro.ysten.province", "master").contains("cm201_hubei")){
			    mIpv6ConnectStatus = EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT;
                            postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_DHCPV6_CONNECT_SUCCESSED,
                                 null, null);
			}
                        //-> update ipLinkProperties at setStatelessv6LinkProperties(mIface);
                        //-> update DNS at getIpv6StalessDns() -> setStatelessDnsProperties();
                        //-> update networkInfo at getIpv6StalessDns()
                        Log.d(TAG, "EVENT_STATELESS6_CONNECT_SUCCESSED #1" );
                    } catch (Exception e) {
                        Log.e(TAG, "mNMServices disable/enable Ipv6 error:" + e);
                        mIpv6ConnectStatus = EthernetManager.ETHERNET_IPV6_CONNECT_STATE_DISCONNECT;
                        if(isCMCCbaseMode) {
                            postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                             EthernetManager.EXTRA_ETHERNET_STATE,
                                             EthernetManager.EVENT_IPV6_CONNECT_FAILED,
                                             null, null);
                        } else {
                            postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                             EthernetManager.EXTRA_ETHERNET_STATE,
                                             EthernetManager.EVENT_STATELESS6_CONNECT_FAILED,
                                             null, null);
                        }
                    }
                }else{
                    mIpv6ConnectStatus = EthernetManager.ETHERNET_IPV6_CONNECT_STATE_DISCONNECT;
                    if(isCMCCbaseMode) {
                        postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                         EthernetManager.EXTRA_ETHERNET_STATE,
                                         EthernetManager.EVENT_IPV6_CONNECT_FAILED,
                                         null, null);
                    } else {
                        postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                         EthernetManager.EXTRA_ETHERNET_STATE,
                                         EthernetManager.EVENT_STATELESS6_CONNECT_FAILED,
                                         null, null);
                    }
                }
            }else if(EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mEthernetManager.getEthernetMode6())) {
                //PPPOEv6 restart is along with  PPPoEv4
                Log.i(TAG, "pppoe v6 return in startIpv6()");
                return;
            } else {
                enableStateless(mIface, false);
                mEthernetManager.setEthernetDefaultConf6();
                Log.i(TAG, "runDhcpv6");
                if(!mEthernetManager.checkDhcpv6Status(mIface)) {
				//add by huxiang for hainan IPOE can not get gateway address
					Log.i(TAG, "startIpv6 begin5");
					reStartIpv6interface();
					//add end 
                    runDhcpv6();
                } else {
                    Log.i(TAG, "DHCPV6 already connected, do nothing");
                }
            }
        } else {
            Log.i(TAG, "IPV6 not enabled!");
        }
    }


    /**
     * restart ipv6 interface to send Router Solicitation, so that to get Router Advertisement
     */
    private void reStartppp0Ipv6interface() {
        if (SystemProperties.getInt("persist.sys.ipv6.disable", 1) == 0) {
            Log.d(TAG, "reStartppp0Ipv6interface Try to trigger Router Solicitation");
            try {
				 //mNMService.disableIpv6("ppp0"); //default pppoev6
				 //mNMService.enableIpv6("ppp0");
				 mEthernetManager.enableIpv6(false);
		         mEthernetManager.enableIpv6(true);
            } catch (Exception e) {
                Log.e(TAG, "mNMServices disable/enable ipv6 error:" + e);
            }
        }
    }
 
    private boolean isNeedRelease(String connectMode, int ipVersion) {
        String maskStr = SystemProperties.get("persist.ethernet.releasemode", "0");

        if (mLinkUp != true ||
            maskStr == null || connectMode == null ||
            (ipVersion != 6 && ipVersion != 4)) {
            Log.i(TAG, "mLinkUp=" + mLinkUp);
            return false;
        }

        if (!connectMode.equals(EthernetManager.ETHERNET_CONNECT_MODE_DHCP) &&
            !connectMode.equals(EthernetManager.ETHERNET_CONNECT_MODE_IPOE)) {
            Log.i(TAG, "connectMode=" + connectMode);
            return false;
        }

        if (ipVersion == 6 && NetworkUtils.getIpv6LinklocalAddress(mIface) == null) {
            Log.i(TAG, "getIpv6LinklocalAddress=null");
            return false;
        }

        Log.i(TAG, "maskStr=" + maskStr);
        if (maskStr.contains(connectMode + ipVersion)) {
            Log.i(TAG, "connectMode " + connectMode + ipVersion + " need release!");
            return true;
        }

        return false;
   }

    private String getDhcpMode() {
        int option60 = mEthernetManager.getDhcpOption60State();
        int option125 = mEthernetManager.getDhcpOption125State();
        String mode = EthernetManager.ETHERNET_CONNECT_MODE_DHCP;
        if (option60 == 1 || option125 == 1) {
            mode = EthernetManager.ETHERNET_CONNECT_MODE_IPOE;
        }

        return mode;
    }

    public void stopIpv6() {
        if(!mLinkUp) {
            mIpv6ConnectStatus = EthernetManager.ETHERNET_IPV6_CONNECT_STATE_DISCONNECT;
            postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                             EthernetManager.EXTRA_ETHERNET_STATE,
                             EthernetManager.EVENT_PHY_LINK_DOWN,
                             null, null);
            postNotification(EthernetManager.ETHERNET_STATE_CHANGED_ACTION,
                             EthernetManager.EXTRA_ETHERNET_STATE,
                             EthernetManager.EVENT_PHY_LINK_DOWN,
                             null, null);
            Log.i(TAG, "stopIpv6(): Physical Connection ERROR! please check your wire");
        }

        if(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL.equals(mEthernetManager.getEthernetMode6())) {
            Log.i(TAG, "Static IPV6, do nothting, clear ipv6 address on " + mIface);
        } else {
            synchronized(this) {
                if (mDhcpv6DelayTimer != null) {
                    mDhcpv6DelayTimer.cancel();
                    mDhcpv6DelayTimer = null;
                    Log.i(TAG, "stopIpv6 mDhcpv6DelayTimer.cancel");
                }
            }

            if (isNeedRelease(getDhcpMode(), 6)) {
                Log.i(TAG, "releaseDhcpv6lease " + mIface);
                NetworkUtils.releaseDhcpv6lease(mIface);
                Log.i(TAG, "releaseDhcpv6lease " + mIface + " done");
            }
            NetworkUtils.stopDhcpv6(mIface);
        }
        mLinklocal6ProbeRunning = false;

        Log.i(TAG, "stopIpv6() success, send message EVENT_DHCPV6_DISCONNECT_SUCCESSED");
        NetworkUtils.clearIpv6Addresses(mIface);
        clearIpv6Routes();
        clearStatelessAddresses();

        mIpv6ConnectStatus = EthernetManager.ETHERNET_IPV6_CONNECT_STATE_DISCONNECT;
        if(mLinkUp != false)//do not send disconnect result when link down
        {
            if(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL.equals(mEthernetManager.getEthernetMode6()))
            {
                postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_STATIC6_DISCONNECT_SUCCESSED,
                                 null, null);
            }
            else if(EthernetManager.ETHERNET_CONNECT_MODE_STATELESS.equals(mEthernetManager.getEthernetMode6()))
            {
                postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_STATELESS6_DISCONNECT_SUCCESSED,
                                 null, null);
            }
            else
            {
                postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_DHCPV6_DISCONNECT_SUCCESSED,
                                 null, null);
            }

            if(isCMCCbaseMode) { //cover the Notification of  EVENT_xxx_DISCONNECT_SUCCESSED if in CMCC base mode
                postNotification(EthernetManager.IPV6_STATE_CHANGED_ACTION,
                                 EthernetManager.EXTRA_ETHERNET_STATE,
                                 EthernetManager.EVENT_IPV6_DISCONNECT_SUCCESSED,
                                 null, null);
            }

        }
        updateLinkProperties(REMOVE_IPV6_LINK);

        Message msg = null;
        if ((mIpv6ConnectStatus != EthernetManager.ETHERNET_IPV6_CONNECT_STATE_CONNECT)
            && (mConnectStatus != EthernetManager.ETHERNET_CONNECT_STATE_CONNECT)
            && (mConnectStatus != PppoeManager.PPPOE_STATE_CONNECT)){
            mTeardownRequested.set(true);

            mNetworkInfo.setIsAvailable(false);
            mNetworkInfo.setDetailedState(DetailedState.DISCONNECTED, null, null);
            msg = mCsHandler.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
            msg.sendToTarget();
        } else {
            mTeardownRequested.set(false);

            mNetworkInfo.setIsAvailable(true);
            mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, null);
            msg = mCsHandler.obtainMessage(EVENT_CONFIGURATION_CHANGED, mNetworkInfo);
            msg.sendToTarget();
        }
    }



    private void updateLinkProperties(int which) {
        if(mLinkProperties == null)
            return;

        Collection<LinkAddress> oldLinkAddrs = new ArrayList<LinkAddress>(mLinkProperties.getLinkAddresses());
        Collection<RouteInfo> oldRoutes = new ArrayList<RouteInfo>(mLinkProperties.getRoutes());
        Collection<InetAddress> oldDnses = new ArrayList<InetAddress>(mLinkProperties.getDnses());
        Log.i(TAG, "updateLinkProperties Before remove, mLinkProperties:"+mLinkProperties);

        Collection<LinkAddress> remainLinkaddrs = new ArrayList<LinkAddress>();
        Collection<RouteInfo> remainRoutes = new ArrayList<RouteInfo>();
        Collection<InetAddress> remainDnses = new ArrayList<InetAddress>();

        for (LinkAddress addr: oldLinkAddrs) {
            if (addr != null) {
                if ((which == REMOVE_IPV4_LINK) && (addr.getAddress() instanceof Inet4Address))
                    continue;
                else if ((which == REMOVE_IPV6_LINK) && (addr.getAddress() instanceof Inet6Address))
                    continue;
                remainLinkaddrs.add(addr);
            }
        }

        for(RouteInfo route: oldRoutes) {
            if (route != null) {
                if ((which == REMOVE_IPV4_LINK) && (route.getGateway() instanceof Inet4Address))
                    continue;
                else if ((which == REMOVE_IPV6_LINK) && (route.getGateway() instanceof Inet6Address))
                    continue;
                remainRoutes.add(route);
            }
        }

        Log.i(TAG, "ipv4LastDns:" + ipv4LastDns + " ipv6LastDns:" + ipv6LastDns);
        for(InetAddress dns: oldDnses) {
            if(dns != null) {
                if ((which == REMOVE_IPV4_LINK) && (ipv4LastDns != null)
                    && (ipv4LastDns.contains(dns)))
                    continue;
                else if ((which == REMOVE_IPV6_LINK) && (ipv6LastDns != null)
                    && (ipv6LastDns.contains(dns)))
                    continue;
                remainDnses.add(dns);
            }
        }

        if(which == REMOVE_IPV4_LINK)
            ipv4LastDns = null;
        else if (which == REMOVE_IPV6_LINK)
            ipv6LastDns = null;


        Log.i(TAG, "updateLinkProperties remainLinkaddrs:" + remainLinkaddrs +
            "  remainRoutes:" + remainRoutes + " remainDnses:" + remainDnses);

        String ifname = mLinkProperties.getInterfaceName();
        int mtu = mLinkProperties.getMtu();
        synchronized (mLinkProperties) {
            if(DEBUG) Log.i(TAG,"mLinkProperties.clear() in updateLinkProperties()");
            mLinkProperties.clear();
        }
        mLinkProperties.setInterfaceName(ifname);
        mLinkProperties.setMtu(mtu);

        for (LinkAddress l: remainLinkaddrs) {
            mLinkProperties.addLinkAddress(l);
        }

        for (RouteInfo r: remainRoutes) {
            try {
                RouteInfo tmp = new RouteInfo(r.getDestination(), r.getGateway(), mLinkProperties.getInterfaceName());
                mLinkProperties.addRoute(tmp);
            }catch(IllegalArgumentException e) {
                Log.e(TAG, "setIpv4LinkProperties addRoute failed");
            }
        }

        for(InetAddress d: remainDnses) {
            mLinkProperties.addDns(d);
        }

        Log.i(TAG, "updateLinkProperties After remove, mLinkProperties:"+mLinkProperties);

    }

    private void setDhcpv6LinkProperties(String ifname) {
        DhcpInfoInternal dhcpinfo = new DhcpInfoInternal();

        String dns6_1 = SystemProperties.get("dhclient."+ifname+".dns1");
        String dns6_2 = SystemProperties.get("dhclient."+ifname+".dns2");
        String ipaddr6 = SystemProperties.get("dhclient.ipaddress."+ifname);
        String prefixlen = SystemProperties.get("dhclient.prefixlen."+ifname);
        Log.i(TAG, "dns6_1:" + dns6_1 + "  dns6_2:"+dns6_2+"  ipaddr6:"+ipaddr6+"  prefixlen:"+prefixlen);
        Log.i(TAG, "setDhcpv6LinkProperties Before add mLinkProperties:" + mLinkProperties);
        ipv6LastDns = new ArrayList<InetAddress>();

        int netmask = 0;
        if (prefixlen != null && !("".equals(prefixlen)))
            netmask = Integer.parseInt(prefixlen);
        if (mLinkProperties != null) {

            if((mConnectStatus != EthernetManager.ETHERNET_CONNECT_STATE_CONNECT)
                && (mConnectStatus != PppoeManager.PPPOE_STATE_CONNECT)){
                synchronized (mLinkProperties) {
                    if(DEBUG) Log.i(TAG,"mLinkProperties.clear() in setDhcpv6LinkProperties()");
                    mLinkProperties.clear();
                }
            }
            String linkInterface = mLinkProperties.getInterfaceName();
            if (linkInterface == null || !ifname.equals(linkInterface))
                mLinkProperties.setInterfaceName(ifname);

            if((ipaddr6 != null) && !("".equals(ipaddr6))) {
                Collection<InetAddress> addrs = mLinkProperties.getAddresses();
                if(addrs != null && !addrs.contains(NetworkUtils.numericToInetAddress(ipaddr6)))
                    mLinkProperties.addLinkAddress(new LinkAddress(NetworkUtils.numericToInetAddress(ipaddr6), netmask));
            }

            if((dns6_1 != null) && !("".equals(dns6_1))) {

                Collection<InetAddress> dnses1 = mLinkProperties.getDnses();
                if(dnses1 != null && !dnses1.contains(NetworkUtils.numericToInetAddress(dns6_1)))
                    mLinkProperties.addDns(NetworkUtils.numericToInetAddress(dns6_1));
                ipv6LastDns.add(NetworkUtils.numericToInetAddress(dns6_1));
            }

            if((dns6_2 != null) && !("".equals(dns6_2))) {
                Collection<InetAddress> dnses2 = mLinkProperties.getDnses();
                if(dnses2 != null && !dnses2.contains(NetworkUtils.numericToInetAddress(dns6_2)))
                    mLinkProperties.addDns(NetworkUtils.numericToInetAddress(dns6_2));
                ipv6LastDns.add(NetworkUtils.numericToInetAddress(dns6_2));
            }
        }
        Log.i(TAG, "setDhcpv6LinkProperties After add mLinkProperties:" + mLinkProperties);
    }

    private void setStatelessv6LinkProperties(String ifname) {

        Log.i(TAG, "StatelessIpaddr6:"+mStatelessIpAddress+"  StatelessPrefixlen:"+mStatelessPrefixlength);
        Log.i(TAG, "setDhcpv6LinkProperties Before add mLinkProperties:" + mLinkProperties);

        if (mLinkProperties != null) {

            if((mConnectStatus != EthernetManager.ETHERNET_CONNECT_STATE_CONNECT)
                && (mConnectStatus != PppoeManager.PPPOE_STATE_CONNECT)){
                synchronized (mLinkProperties) {
                    if(DEBUG) Log.i(TAG,"mLinkProperties.clear() in setStatelessv6LinkProperties()");
                    mLinkProperties.clear();
                }
            }
            String linkInterface = mLinkProperties.getInterfaceName();
            if (linkInterface == null || !ifname.equals(linkInterface))
                mLinkProperties.setInterfaceName(ifname);

            if((mStatelessIpAddress != null) && !("".equals(mStatelessIpAddress))) {
                Collection<InetAddress> addrs = mLinkProperties.getAddresses();
                if(addrs != null && !addrs.contains(NetworkUtils.numericToInetAddress(mStatelessIpAddress)))
                    mLinkProperties.addLinkAddress(new LinkAddress(NetworkUtils.numericToInetAddress(mStatelessIpAddress), mStatelessPrefixlength));
            }

        }
        Log.i(TAG, "setStatelessv6LinkProperties After add mLinkProperties:" + mLinkProperties);
    }

    private void setPPPoeStatelessv6LinkProperties(String ifname) {
        Log.i(TAG, "PPPoeStatelessIpaddr6:"+mPPPoeStatelessIpAddress +"  PPPoeStatelessPrefixlen:"+mPPPoeStatelessPrefixlength );
        Log.i(TAG, "setDhcpv6LinkProperties Before add mLinkProperties:" + mLinkProperties);

        if (mLinkProperties != null) {

            if((mConnectStatus != EthernetManager.ETHERNET_CONNECT_STATE_CONNECT)
                && (mConnectStatus != PppoeManager.PPPOE_STATE_CONNECT)){
                synchronized (mLinkProperties) {
                    if(DEBUG) Log.i(TAG,"mLinkProperties.clear() in setStatelessv6LinkProperties()");
                    mLinkProperties.clear();
                }
            }
            String linkInterface = mLinkProperties.getInterfaceName();
            if (linkInterface == null || !ifname.equals(linkInterface))
                mLinkProperties.setInterfaceName(ifname);

            if((mPPPoeStatelessIpAddress != null) && !("".equals(mPPPoeStatelessIpAddress))) {
                Collection<InetAddress> addrs = mLinkProperties.getAddresses();
                if(addrs != null && !addrs.contains(NetworkUtils.numericToInetAddress(mPPPoeStatelessIpAddress)))
                    mLinkProperties.addLinkAddress(new LinkAddress(NetworkUtils.numericToInetAddress(mPPPoeStatelessIpAddress), mPPPoeStatelessPrefixlength));
            }

        }
        Log.i(TAG, "setPPPoeStatelessv6LinkProperties After add mLinkProperties:" + mLinkProperties);
    }

    public String getStatelessIpv6Address()
    {
        if( EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mEthernetManager.getEthernetMode6()) && mEthernetManager.isIpv6PppoeStateless() ){
            Log.i(TAG, "return pppoe stateless address");
            return mPPPoeStatelessIpAddress;
        }
        return mStatelessIpAddress;
    }

    public int getStatelessIpv6Prefixlength()
    {
        if( EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mEthernetManager.getEthernetMode6()) && mEthernetManager.isIpv6PppoeStateless() ){
            return mPPPoeStatelessPrefixlength;
        }
        return mStatelessPrefixlength;
    }

    public String getStatelessIpv6Dns1()
    {
        if( EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mEthernetManager.getEthernetMode6()) && mEthernetManager.isIpv6PppoeStateless() ){
            return mPPPoeStatelessIpv6Dns1;
        }
        return mStatelessIpv6Dns1;
    }

    public String getStatelessIpv6Dns2()
    {
        if( EthernetManager.ETHERNET_CONNECT_MODE_PPPOE.equals(mEthernetManager.getEthernetMode6()) && mEthernetManager.isIpv6PppoeStateless() ){
            return mPPPoeStatelessIpv6Dns2;
        }
        return mStatelessIpv6Dns2;
    }

    public void clearStatelessAddresses()
    {
        mStatelessIpAddress = null;
        mStatelessIpv6Dns1 = null;
        mStatelessIpv6Dns2 = null;
        mStatelessPrefixlength = 0;
        Log.i(TAG, "clearStatelessAddresses:" + mStatelessIpv6Dns1 );
    }
    public void clearIpv6Routes()
    {
        Log.i(TAG, "clearIpv6Routes");
        try {
            for (RouteInfo route : mNetworkManagementService.getRoutes(mIface)) {
                if(route.getDestination().getAddress() instanceof Inet6Address){
                    if(DEBUG) Log.d(TAG, "remove " + route + " on " + mIface);
                    mNetworkManagementService.removeRoute(mIface, route);
                }
            }
        } catch (Exception e) {
            // never crash - catch them all
            Log.e(TAG, "Exception trying to remove a route: " + e);
        }
    }

    //add for chinamobile
    public int getIpv4ConnectStatus() {
        return mConnectStatus;
    }
    //add for chinamobile
    public int getIpv6ConnectStatus() {
        return mIpv6ConnectStatus;
    }

    /**
     * Set IPv6 stateless Enable/Disable
     */
    private void enableStateless(String iface, boolean enable) {
        String cmd = String.format("stateless_%s:%s", iface, enable ? 1 : 0);
        if (DEBUG) Log.i(TAG, "enableStateless: " + cmd);
        SystemProperties.set("ctl.start", cmd);
    }

    /**
     * restart ipv6 interface to send Router Solicitation, so that to get Router Advertisement
     */
    private void reStartIpv6interface() {
        if (SystemProperties.getInt("persist.sys.ipv6.disable", 1) == 0) {
            Log.d(TAG, "Try to trigger Router Solicitation");
            try {
                mNMService.disableIpv6(mIface);
                mNMService.enableIpv6(mIface);
            } catch (Exception e) {
                Log.e(TAG, "mNMServices disable/enable ipv6 error:" + e);
            }
        }
    }
}
