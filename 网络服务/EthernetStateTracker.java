package android.net.ethernet;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.http.util.EncodingUtils;
import java.io.FileInputStream;
import java.io.IOException;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothHeadset;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.provider.Settings;
import android.net.ConnectivityManager;
import android.net.pppoe.PppoeManager;
import android.net.DhcpInfo;
import android.net.DhcpInfoInternal;
import android.net.DhcpResults;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.LinkCapabilities;
import android.net.LinkProperties;
import android.net.LinkQualityInfo;
import android.net.NetworkStateTracker;
import android.net.NetworkUtils;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.pppoe.PppoeDevInfo;
import android.net.ProxyProperties;
import android.net.RouteInfo;
import android.net.SamplingDataTracker;
import android.net.vlan.VlanDevInfo;
import android.net.vlan.VlanManager;
import android.net.wifi.WifiManager;
import android.os.FileUtils;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.*;
import android.provider.Settings;

/**
 * Track the state of Ethernet connectivity. All event handling is done here,
 * and all changes in connectivity state are initiated here.
 *
 * @hide
 */
public class EthernetStateTracker implements NetworkStateTracker {

    private static final String TAG="EthernetStateTracker";

    public static final int EVENT_DHCP_START                        = 0;
    public static final int EVENT_INTERFACE_CONFIGURATION_SUCCEEDED = 1;
    public static final int EVENT_INTERFACE_CONFIGURATION_FAILED    = 2;
    public static final int EVENT_HW_CONNECTED                      = 3;
    public static final int EVENT_HW_DISCONNECTED                   = 4;
    public static final int EVENT_HW_PHYCONNECTED                   = 5;
    public static final int EVENT_HW_PHYDISCONNECTED                = 6;
    //temporary event for Settings until this supports multiple interfaces
    public static final int EVENT_HW_CHANGED                        = 7;
    public static final int EVENT_DEVICE_ADDED                      = 8;
    public static final int EVENT_DEVICE_REMOVED                    = 9;
    public static final int EVENT_IP_REMOVED                        = 10;
    public static final int EVENT_REQUEST_RESET                     = 11;

    private EthernetManager mEM;
    private PppoeManager mPppoeManager;
    private WifiManager mWifiManager;
    private VlanManager mVM;
    private boolean mServiceStarted;


    private AtomicBoolean mTeardownRequested = new AtomicBoolean(false);
    private AtomicBoolean mPrivateDnsRouteSet = new AtomicBoolean(false);
    private AtomicBoolean mDefaultRouteSet = new AtomicBoolean(false);

    private LinkProperties mLinkProperties;
    private LinkCapabilities mLinkCapabilities;
    private NetworkInfo mNetworkInfo;
    private NetworkInfo.State mLastState = NetworkInfo.State.UNKNOWN;

    private boolean mStackConnected;
    private boolean mHWConnected;
    private boolean mInterfaceStopped;
    private Handler mDhcpTarget;
    private String mInterfaceName = null;
    private DhcpInfoInternal mDhcpInfoInternal;
    private boolean mStartingDhcp;
    private Handler mTarget;

    private Handler mTrackerTarget;
    private Context mContext;
    private EthernetStateMachine mESM;
    private DhcpResults mDhcpResults = null;
    private boolean mPppoeTerminateDone = false;
    private boolean mEthEnable = false;
    private Handler mDualStackTarget;
    private boolean mSystemReady = false;
    private int mDhcpRetry = 0;
    private int mDhcpRetryCount = 3;

    public static boolean IFCONFIG = false;//#PD96694

    private final String DEFAULT_ETHER_IFNAME = "eth0";
    private final String pppoe_running_flag = "net.pppoe.running";
    private final String pppoe_phyif = "net.pppoe.phyif";

    /** add below variables for fix bug PD#138906 */
    private boolean firstlinkdown = true;
    private boolean fakeDown = false;
    private Thread postNotificationThread ;
    private boolean first_reset_interface = true;
    /** end added */
    private static int mNAP_TIME = 20; /* wait for 20ms at a time */
                                       /* when polling for property values */
    private String PROPERTIES_DEBUG = "persist.sys.amlogic.log.debug";

    public EthernetStateTracker(int netType, String networkName) {
        log(3, TAG,"Starts...");

        mNetworkInfo = new NetworkInfo(netType, 0, networkName, "");
        mLinkProperties = new LinkProperties();
        mLinkCapabilities = new LinkCapabilities();

        mNetworkInfo.setIsAvailable(false);
        setTeardownRequested(false);

        if (EthernetNative.initEthernetNative() != 0 ) {
            log(0, TAG,"Can not init ethernet device layers");
            return;
        }
        log(3, TAG,"Success");
        mServiceStarted = true;
        mSystemReady = false;
        mDhcpRetryCount = SystemProperties.getInt("net.dhcp.repeat.count", 3);

    }

    private int waitForProperty(String name, String desired_value, int maxwait_ms) {
        if (null == name) {
            log(3, TAG, "wait_for_property invalid argument");
            return -1;
        }
        int maxnaps = (maxwait_ms) / mNAP_TIME;

        if (maxnaps < 1) {
            maxnaps = 1;
        }

        while (maxnaps-- > 0) {
            if (desired_value == null ||
                    desired_value.equals(SystemProperties.get(name, "none")))
            {
                return 0;
            }
            try {
                Thread.sleep(mNAP_TIME);
            }catch (Exception e){};
        }
        return -1; /* failure */
    }

    public void setTeardownRequested(boolean isRequested) {
        mTeardownRequested.set(isRequested);
    }

    public boolean isTeardownRequested() {
        return mTeardownRequested.get();
    }

    private void pppoeConnect() {
        if (mPppoeManager != null) {
            PppoeDevInfo info = mPppoeManager.getSavedPppoeConfig();
            String usr = info.getAccount();
            String pwd = info.getPassword();
            String ifname = info.getIfName();
            String dialmode = info.getDialMode();
            String iptype = info.getIpType();
            log(3, TAG, "pppoeConnect .....");
            log(3, TAG, "Username:[" + usr + "], Password:[" + pwd
                    + "], ifname:[" + ifname + "], dialmode:[" + dialmode + "]");
            if ((usr == null || usr.equals("")) ||
               (pwd == null || pwd.equals(""))) {
                log(1, TAG, "INVALID USERNAME OR PASSWORD");
            } else if(!DEFAULT_ETHER_IFNAME.equals(ifname)) {
                log(1, TAG, "INVALID inferface: " + ifname);
            } else if(!PppoeDevInfo.PPPOE_DIAL_MODE_AUTO.equals(dialmode)) {
                log(3, TAG, "not auto dial: " + dialmode);
            } else if (!PppoeDevInfo.PPPOE_IP_V4.equals(iptype) &&
                      !PppoeDevInfo.PPPOE_IP_V4_AND_V6.equals(iptype)) {
                log(3, TAG, "over IPv6");
            }
            else {
                mPppoeManager.connect(usr, pwd, DEFAULT_ETHER_IFNAME, dialmode);
            }
        }
        else {
            log(3, TAG, "pppoeConnect faield for mPppoeManager uniinitialized");
        }
    }

    private void pppoeDisconnect() {
        if (mPppoeManager != null) {
            String ethif_name = DEFAULT_ETHER_IFNAME;
            log(3, TAG, "pppoeDisconnect .....");
            if (mInterfaceName != null)
                ethif_name = mInterfaceName;
            mPppoeManager.disconnect(ethif_name);
            log(3, TAG, "WAIT PPPOE disconnect END");
        }
    }

    public boolean isIpAddress(String ipaddr) {
        if (ipaddr == null) {
            log(0, TAG, "isIpAddress, ip addr is null");
            return false;
        }
        String[] ips = ipaddr.split("\\.");
        if (ips.length != 4) {
            log(0, TAG, "isIpAddress, ip addr is error, ip addr is " + ipaddr);
            return false;
        }
        for (int i=0; i<4; i++) {
            try {
                int block = Integer.parseInt(ips[i]);
                if ((block > 255) || (block < 0)) {
                    log(0, TAG, "isIpAddress, ip addr is error, ips[" + i + "]: " + ips[i]);
                    return false;
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public boolean stopInterface(boolean suspend) {
        if (mEM != null) {
            EthernetDevInfo info = mEM.getSavedEthConfig();
            if (info != null && mEM.ethConfigured()) {
                mInterfaceStopped = true;
                log(3, TAG, "stop dhcp and interface with suspend:" + suspend );
                mStartingDhcp = false;
                mStackConnected = false;
                String ifname = mInterfaceName;

                if (mInterfaceName != null)
                    NetworkUtils.stopDhcp(mInterfaceName);
                mDhcpResults = null;
                mLinkProperties = null;
                mESM.stopDhcp();
                String v6mode = mEM.getEthernetMode6();
                if (EthernetDevInfo.ETH_CONN_MODE_PPPOE.equals(v6mode)) {
                        log(3, TAG, "ipv4 resetinterface clear ipv4 addr from ppp0");
                        NetworkUtils.resetConnections("ppp0", NetworkUtils.RESET_IPV4_ADDRESSES);
                }else {
                        log(3, TAG, "ipv4 resetinterface disconnect pppoe");
                        mPppoeManager.disconnect(DEFAULT_ETHER_IFNAME);
                }
                if (ifname != null)
                    NetworkUtils.resetConnections(ifname, NetworkUtils.RESET_IPV4_ADDRESSES);
                if (!suspend) {
                    //mHWConnected = false;
                    setEthState(false, EVENT_HW_DISCONNECTED);
                }
            }
        }
        Slog.d(TAG, "Leave stopInterface");
        return true;
    }

    public boolean stopInterface_when_suspend() {
        if (mEM != null) {
            EthernetDevInfo info = mEM.getSavedEthConfig();
            if (info != null && mEM.ethConfigured()) {
                mInterfaceStopped = true;
                log(3, TAG, "stop dhcp and interface when suspend" );
                mStartingDhcp = false;
                mStackConnected = false;
                String ifname = mInterfaceName;

                mESM.stopDhcp();
                if (ifname != null)
                    NetworkUtils.resetConnections(ifname, NetworkUtils.RESET_IPV4_ADDRESSES);

                mEthEnable = false;
                mHWConnected = false;
                NetworkUtils.disableInterface(ifname);
            }
        }

        return true;
    }

    private boolean configureInterfaceStatic(EthernetDevInfo info, DhcpInfoInternal dhcpInfoInternal) {
        final String ifname = info.getIfName();
        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
        INetworkManagementService netd = INetworkManagementService.Stub.asInterface(b);
        InterfaceConfiguration ifcg = new InterfaceConfiguration();
        ifcg.setLinkAddress(dhcpInfoInternal.makeLinkAddress());
        ifcg.setInterfaceUp();
        try {
            netd.setInterfaceConfig(ifname, ifcg);
            mLinkProperties = dhcpInfoInternal.makeLinkProperties();
            mLinkProperties.setInterfaceName(ifname);
            if (info.hasProxy())
                mLinkProperties.setHttpProxy(new ProxyProperties(info.getProxyHost(),
                    info.getProxyPort(), info.getProxyExclusionList()));
            else
                mLinkProperties.setHttpProxy(null);

            log(3, TAG, "LinkProperties " + mLinkProperties.toString());
            mDhcpResults = new DhcpResults(mLinkProperties);

            log(4, TAG, "Static IP configuration succeeded");
            return true;
        } catch (RemoteException re) {
            log(4, TAG, "Static IP configuration failed: " + re);
            return false;
        } catch (IllegalStateException e) {
            log(4, TAG, "Static IP configuration failed: " + e);
            return false;
        }
    }


    private boolean configureInterface(EthernetDevInfo info) throws UnknownHostException {
        mInterfaceName = info.getIfName();
        //mStackConnected = false;
        //mHWConnected = false;
        mInterfaceStopped = false;

        log(3, TAG, "set " + mInterfaceName + " as current interface. " + info.getConnectMode());

        mDhcpInfoInternal = new DhcpInfoInternal();

        if (info.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_DHCP) ||
                info.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_DHCP_AUTH)) {
            if(mInterfaceName != null) {
                /*when configure bridge interface(br0), we cannot getEthernetDeviceUp immediately
                after reset operation, so lets take some time to retry */
                int cnt = 10;
                while (!mEM.isEthernetDeviceUp(mInterfaceName) &&cnt-- > 0){
                    try {
                        Thread.sleep(200);
                        } catch (Exception e){};
                }
                if (!mEM.isEthernetDeviceUp(mInterfaceName)) {
                        log(2, TAG, mInterfaceName + " is not up!");
                        return false;
                }
            }

	        if (info.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_DHCP_AUTH)) {
				if (mInterfaceName != null && !mEM.isEthernetDeviceUp(mInterfaceName)) {
					Slog.d(TAG, mInterfaceName + " is not up!");
					return false;
				}
				if (mDualStackTarget != null) {
					String authUsr = mEM.getAuthUsername();
					String authPwd = mEM.getAuthPassword();
					Slog.d(TAG, "configureInterface, startDualStackIpoe authUsr: " + authUsr + " authPwd: " + authPwd);
					if ((authUsr != null) && (authPwd != null)) {
						startDualStackIpoe(info);
					}
				} else {
					if (!mStartingDhcp) {
						Slog.i(TAG, "trigger dhcp for device " + info.getIfName());
						mStartingDhcp = true;
						mESM.startDhcp(info.getIfName());
					}
				}
	        } else if (info.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_DHCP)) {
                if (!mStartingDhcp) {
                    log(3, TAG, "trigger dhcp for device " + info.getIfName());
                    mStartingDhcp = true;
                    mESM.startDhcp(info.getIfName());
                }
			}
        } else if (info.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_MANUAL)) {
            int event;
            try {
                if (!isIpAddress(info.getIpAddress()) || !isIpAddress(info.getNetMask())) {
                    log(0, TAG, "set ip manually error, ip addr is " + info.getIpAddress()
                            + " netmask is " + info.getNetMask());
                    return false;
                } else if (mEM != null && mEM.isEthernetDeviceAdded(mInterfaceName)) {
                    mDhcpInfoInternal.ipAddress = info.getIpAddress();

                    InetAddress ia = NetworkUtils.numericToInetAddress(info.getNetMask());
                    mDhcpInfoInternal.prefixLength = NetworkUtils.netmaskIntToPrefixLength(
                            NetworkUtils.inetAddressToInt((Inet4Address)ia));

                    LinkAddress myLinkAddr = new LinkAddress(info.getIpAddress() + "/"
                            + mDhcpInfoInternal.prefixLength);
                    mDhcpInfoInternal.addRoute(new RouteInfo(myLinkAddr));
                    mDhcpInfoInternal.addRoute(new RouteInfo(
                            NetworkUtils.numericToInetAddress(info.getRouteAddr())));

                    mDhcpInfoInternal.dns1 = info.getDns1Addr();
                    mDhcpInfoInternal.dns2 = info.getDns2Addr();
                    log(3, TAG, "set ip manually " + mDhcpInfoInternal.toString());
                    if (info.getIfName() != null)
                        NetworkUtils.resetConnections(info.getIfName(), NetworkUtils.RESET_IPV4_ADDRESSES);

                    if (configureInterfaceStatic(info, mDhcpInfoInternal)) {
                        event = EVENT_INTERFACE_CONFIGURATION_SUCCEEDED;
                        log(4, TAG, "Static IP configuration succeeded");
                    } else {
                        event = EVENT_INTERFACE_CONFIGURATION_FAILED;
                        log(4, TAG, "Static IP configuration failed");
                    }
                    mTrackerTarget.sendEmptyMessage(event);
                } else {
                    log(0, TAG, "set ip manually error, mEM is null or eth0 is down!");
                    return false;
                }
            } catch (IllegalArgumentException e) {
                event = EVENT_INTERFACE_CONFIGURATION_FAILED;
                log(0, TAG, "IllegalArgumentException: " + e);
            }
        }

        return true;
    }


    public boolean resetInterface() throws UnknownHostException {
        /* This will guide us to enabled the enabled device */
        if (mEM != null) {
            EthernetDevInfo info;
            if (mEM.ethConfigured()) {
                info = mEM.getSavedEthConfig();
            } else if (mInterfaceName != null) {
                info = new EthernetDevInfo();
                info.setIfName(mInterfaceName);
            } else {
                log(0, TAG, "EthernetConfig is EMPTY and mInterfaceName is EMPTY, !resetInterface");
                return true;
            }

            synchronized (this) {
                mInterfaceName = info.getIfName();
                log(3, TAG, "reset device " + mInterfaceName);
                if (mInterfaceName != null)
                    NetworkUtils.resetConnections(mInterfaceName, NetworkUtils.RESET_IPV4_ADDRESSES);
					String v6mode = mEM.getEthernetMode6();
                    if (EthernetDevInfo.ETH_CONN_MODE_PPPOE.equals(v6mode)) {
                        log(3, TAG, "ipv4 resetinterface clear ipv4 addr from ppp0");
                        NetworkUtils.resetConnections("ppp0", NetworkUtils.RESET_IPV4_ADDRESSES);
                    }
                    else {
                        log(3, TAG, "ipv4 resetinterface disconnect pppoe");
                        mPppoeManager.disconnect(DEFAULT_ETHER_IFNAME);
                    }

                if(!mHWConnected) {
                    log(3, TAG, "Eth is removed, do not configureInterface!");
                    setEthState( false, EVENT_HW_DISCONNECTED);

                    if(!mEthEnable) {
                        if (mInterfaceName != null)
                            NetworkUtils.enableInterface(mInterfaceName);
                            if(mInterfaceName != null && !"eth0".equals(mInterfaceName))
                                NetworkUtils.enableInterface("eth0");
                        mEthEnable = true;
                    }
                    return true;
                }

                if(mStackConnected || mStartingDhcp) {
                    log(3, TAG, "resetInterface: stop dhcp");
                    mStackConnected = false;
                    mStartingDhcp = false;
                    mESM.stopDhcp();
                    log(3, TAG, "Force the connection disconnected before configuration");
                    setEthState( false, EVENT_HW_DISCONNECTED);
                }

                configureInterface(info);

                if (info.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_PPPOE)) {
                  if (!first_reset_interface) {
                    log(1, TAG, "configureInterface, do nothing when mode is " + info.getConnectMode());
                  } else {
                    first_reset_interface = false;
                    boolean newNetworkstate = mNetworkInfo.isConnected();
                    if (mEM.isEthernetDeviceAdded(mInterfaceName)) {
                        log(1, TAG, "FIXME: configureInterface, PPPoE auto dial");
                        // disconnect before pppoeConnect first.
                        setEthState(newNetworkstate, EVENT_HW_PHYCONNECTED);
                        pppoeConnect();
                    }
                  }
                }
            }
        }
        return true;
    }

    public String getTcpBufferSizesPropName() {
        return "net.tcp.buffersize.ethernet";
    }

    public void StartPolling() {
        log(4, TAG, "start polling");
		final String net_file = "/sys/class/net/eth0/operstate";
		String res = "";
		try {
			FileInputStream fin = new FileInputStream(net_file);
			int length = fin.available();
			byte[] buffer = new byte[length];
			fin.read(buffer);
			res = EncodingUtils.getString(buffer, "UTF-8");
			fin.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		if (null != res) {
			if ("up".equals(res.trim())) {
				log(4, TAG, "eth0 already up before startMonitoring");
				mHWConnected = true;
				mEthEnable = true;
			}
		}
    }

    public boolean isAvailable() {
        // Only say available if we have interfaces and user did not disable us.
        return ((mEM.getTotalInterface() != 0) && (mEM.getEthState() != EthernetManager.ETH_STATE_DISABLED));
    }

    public boolean reconnect() {
        log(3, TAG, ">>>reconnect");
        try {
            synchronized (this) {
                if (mHWConnected && mStackConnected) {
                    log(3, TAG, "$$reconnect() returns DIRECTLY)");
                    return true;
                }
            }
            if (mEM.getEthState() == EthernetManager.ETH_STATE_UNKNOWN ) {
                return true;
            } else if (mEM.getEthState() != EthernetManager.ETH_STATE_DISABLED ) {
                // maybe this is the first time we run, so set it to enabled
                mEM.setEthEnabled(true);
                if (!mEM.ethConfigured()) {
                    mEM.ethSetDefaultConf();
                }
                log(3, TAG, "$$reconnect call resetInterface()");
                return resetInterface();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return false;

    }

    public boolean setRadio(boolean turnOn) {
        return false;
    }

    public void startMonitoring(Context context, Handler target) {
        log(3, TAG, "start to monitor the Ethernet devices");
        if (mServiceStarted) {
            mContext = context;
            int state = 0;
            final ContentResolver cr = mContext.getContentResolver();
            mEM = (EthernetManager)mContext.getSystemService(Context.ETH_SERVICE);
            mPppoeManager = (PppoeManager)mContext.getSystemService(Context.PPPOE_SERVICE);
            mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            mVM = (VlanManager) mContext.getSystemService(Context.VLAN_SERVICE);
            mTarget = target;
            mTrackerTarget = new Handler(target.getLooper(), mTrackerHandlerCallback);
            mESM.setTrackerTarget(mTrackerTarget);
            try {
			state = Settings.Secure.getInt(cr, Settings.Secure.ETH_ON);
            } catch (Settings.SettingNotFoundException e) {}
            //int state = mEM.getEthState();
            log(2, TAG, "startMonitoring state is : "+state);
            if (state != EthernetManager.ETH_STATE_DISABLED) {
                if (state == EthernetManager.ETH_STATE_UNKNOWN || state == EthernetManager.ETH_STATE_ENABLED){
                    log(3, TAG, "maybe this is the first time we run, so set it to enabled");
                    mEM.setEthEnabled(true);
                    int mtu =  SystemProperties.getInt("sys.net.mtu",0);
                    if (mtu > 1000) {
                        IBinder b = ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE);
                        INetworkManagementService netd = INetworkManagementService.Stub.asInterface(b);
                        try {
                            netd.setMtu("eth0", mtu);
                            log(3, TAG, "set eth0 mtu to:" + mtu);
                        } catch (Exception e) {
                            log(0, TAG, "exception in setMtu()" + e);
                        }
                    }
                } else {
                    log(3, TAG, "$$ DISABLE startMonitoring call resetInterface()");
                }
            }
        }
    }

    public void setUserDataEnable(boolean enabled) {
        log(2, TAG, "ignoring setUserDataEnable(" + enabled + ")");
    }

    public void setPolicyDataEnable(boolean enabled) {
        log(2, TAG, "ignoring setPolicyDataEnable(" + enabled + ")");
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
     * Fetch NetworkInfo for the network
     */
    public NetworkInfo getNetworkInfo() {
        return new NetworkInfo(mNetworkInfo);
    }

    /**
     * Fetch LinkProperties for the network
     */
    public LinkProperties getLinkProperties() {
        return new LinkProperties(mLinkProperties);
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

    public boolean teardown() {
        return (mEM != null) ? stopInterface(false) : false;
    }

    public void captivePortalCheckComplete() {
        //TODO
    }

    private void postNotification(int event) {
        if (!mSystemReady) {
            log(1, TAG, "postNotification system not ready, event: " + event);
            if(event == EVENT_INTERFACE_CONFIGURATION_SUCCEEDED && mEM.isEthDeviceAdded())
              log(2, TAG, "ip configed...");
          else
            return;
        }
        if(EthernetStateTracker.EVENT_INTERFACE_CONFIGURATION_SUCCEEDED == event){
            IFCONFIG = true;
            log(2, TAG, "the interface config completed and the flag is :"+IFCONFIG);
        }
        log(2, TAG, "postNotification, event --> event : " + event);
        /*chinamobile-settings recevice intent with ETHERNET_STATE_CHANGED_ACTION & EXTRA_ETHERNET_STATE only, changed by wei.wang*/
        final Intent intent = new Intent(EthernetManager.ETHERNET_STATE_CHANGED_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        intent.putExtra(EthernetManager.EXTRA_ETHERNET_STATE, getEventForCMoblie(event));

        if ("sticky".equals(SystemProperties.get("sys.broadcast.policy"))){
            log(2, TAG, "postNotification policy sticy");
            mContext.sendStickyBroadcastAsUser(intent, UserHandle.OWNER);
        }
        else {
            mContext.sendBroadcast(intent);
        }
    }

    private String getEventDesc(int event) {
        if ( EVENT_DHCP_START == event ) {
            return "EVENT_DHCP_START";
        } else if ( EVENT_INTERFACE_CONFIGURATION_SUCCEEDED == event ) {
            return "EVENT_IF_CFG_SUCCEEDED";
        } else if ( EVENT_INTERFACE_CONFIGURATION_FAILED == event ) {
            return "EVENT_IF_CFG_FAILED";
        } else if ( EVENT_HW_CONNECTED == event ) {
            return "EVENT_HW_CONNECTED";
        } else if ( EVENT_HW_DISCONNECTED == event ) {
            return "EVENT_HW_DISCONNECTED";
        } else if ( EVENT_HW_PHYCONNECTED == event ) {
            return "EVENT_HW_PHYCONNECTED";
        } else if ( EVENT_HW_PHYDISCONNECTED == event ) {
            return "EVENT_HW_PHYDISCONNECTED";
        } else if ( EVENT_HW_CHANGED == event ) {
            return "EVENT_HW_CHANGED";
        } else if ( EVENT_DEVICE_ADDED == event ) {
            return "EVENT_DEVICE_ADDED";
        } else if ( EVENT_DEVICE_REMOVED == event ) {
            return "EVENT_DEVICE_REMOVED";
        }  else {
            return "EVENT_UNKOWN";
        }
    }


    /*the eth event is redefined by chinamobile ,add by wei.wang*/
    private int getEventForCMoblie(int event){
        String mode = mEM.getEthernetMode();
        int invertEvent = event;
        if(event == EVENT_HW_PHYCONNECTED) {
            invertEvent = EthernetManager.EVENT_PHY_LINK_UP;
        } else if(event == EVENT_HW_PHYDISCONNECTED) {
            invertEvent = EthernetManager.EVENT_PHY_LINK_DOWN;
        } else {
            if(mode.equals(EthernetManager.ETHERNET_CONNECT_MODE_MANUAL)) {
                if((event == EVENT_INTERFACE_CONFIGURATION_SUCCEEDED && mEM.isEthDeviceAdded())
                    || event == EVENT_HW_CONNECTED) {
                    invertEvent = EthernetManager.EVENT_STATIC_CONNECT_SUCCESSED;
                }else if(event == EVENT_HW_DISCONNECTED) {
                    invertEvent = EthernetManager.EVENT_STATIC_DISCONNECT_SUCCESSED;
                }else if(event == EVENT_INTERFACE_CONFIGURATION_FAILED) {
                    invertEvent = EthernetManager.EVENT_STATIC_CONNECT_FAILED;
                }
            } else {
                if ((event == EVENT_INTERFACE_CONFIGURATION_SUCCEEDED && mEM.isEthDeviceAdded())
                        || event == EVENT_HW_CONNECTED) {
                    invertEvent = EthernetManager.EVENT_DHCP_CONNECT_SUCCESSED;
                }else if(event == EVENT_HW_DISCONNECTED) {
                    invertEvent = EthernetManager.EVENT_DHCP_DISCONNECT_SUCCESSED;
                }else if(event == EVENT_INTERFACE_CONFIGURATION_FAILED) {
                    invertEvent = EthernetManager.EVENT_DHCP_CONNECT_FAILED;
                }
            }
        }
        log(2, TAG, "postNotification, invertEvent : " + invertEvent);
        return invertEvent;
    }

    private void setEthState(boolean state, int event) {
        log(2, TAG, "setEthState state=" + mNetworkInfo.isConnected() + "->" + state +
                    " event=" + getEventDesc(event) + "("+ event +")");

        if (mNetworkInfo.isConnected() != state) {
            if (state) {
                log(2, TAG, "***isConnected: " + mNetworkInfo.isConnected());
                mNetworkInfo.setDetailedState(DetailedState.CONNECTED, null, null);
                log(2, TAG, "***isConnected: " + mNetworkInfo.isConnected());
            } else {
                log(2, TAG, "***isConnected: " + mNetworkInfo.isConnected());
                mNetworkInfo.setDetailedState(DetailedState.DISCONNECTED, null, null);
                log(2, TAG, "***isConnected: " + mNetworkInfo.isConnected());
                if((EVENT_HW_DISCONNECTED == event) || (EVENT_HW_PHYDISCONNECTED == event)) {
                    log(2, TAG, "EVENT_HW_DISCONNECTED: StopInterface");
                    stopInterface(true);
                }
            }
            mNetworkInfo.setIsAvailable(state);
            Message msg = mTarget.obtainMessage(EVENT_STATE_CHANGED, mNetworkInfo);
            msg.sendToTarget();
        }
        else if (!mNetworkInfo.isConnected() &&
                (EVENT_HW_PHYDISCONNECTED == event || EVENT_HW_DISCONNECTED == event) &&
                mInterfaceName != null) {
            log(2, TAG, "FIXME: clear IP ABSOLUTELY on " + mInterfaceName);
            NetworkUtils.resetConnections(mInterfaceName, NetworkUtils.RESET_IPV4_ADDRESSES);
        }
      if ("unicom".equals(SystemProperties.get("sys.proj.type"))
          && "shandong".equals(SystemProperties.get("sys.proj.tender.type"))) {
        if (event == EVENT_HW_PHYCONNECTED) {
          if ( postNotificationThread != null) {
            Thread.State postthradstate = postNotificationThread.getState();
            log(3, TAG, ">>postthradstate is " + postthradstate);
            if (postthradstate !=Thread.State.TERMINATED)
              fakeDown = true;
            //postNotificationThread.destroy();
          }
        }
        if (firstlinkdown && event == EVENT_HW_PHYDISCONNECTED) {
          firstlinkdown = false;
          log(3, TAG, ">>new postNotification thread");
          postNotificationThread = new Thread(new Runnable() {
            public void run() {
              try {
                Thread.sleep(2500);
              } catch (Exception e){
              } finally {
                if (!fakeDown) {
                  log(3, TAG, ">>post Notification after 1.5s: EVENT_HW_PHYDISCONNECTED");
                  postNotification(EVENT_HW_PHYDISCONNECTED);
                }
              }
            }
          });
          postNotificationThread.start();
        } else {
          log(3, TAG, ">>post Notification: " + event);
          postNotification(event);
        }
      } else {
        postNotification(event);
      }
    }

    public DhcpInfo getDhcpInfo() {
        if(mDhcpResults == null){
            if("mobile".equals(SystemProperties.get("sys.proj.type"))) 
                return null;
	    if(mDhcpInfoInternal == null)
            	mDhcpInfoInternal = new DhcpInfoInternal();
            DhcpInfo mDhcpInfo = mDhcpInfoInternal.makeDhcpInfo();
            return mDhcpInfo;
        }
        if (mDhcpResults.linkProperties == null) 
            return null;
        DhcpInfo info = new DhcpInfo();
		
        //Always send null info if mHWConnected=false
        if(!mHWConnected)
            return info;
		
        for (LinkAddress la : mDhcpResults.linkProperties.getLinkAddresses()) {
            InetAddress addr = la.getAddress();
            if (addr instanceof Inet4Address) {
                info.ipAddress = NetworkUtils.inetAddressToInt((Inet4Address)addr);
                break;
            }
        }
        for (RouteInfo r : mDhcpResults.linkProperties.getRoutes()) {
            if (r.isDefaultRoute()) {
                InetAddress gateway = r.getGateway();
                if (gateway instanceof Inet4Address) {
                    info.gateway = NetworkUtils.inetAddressToInt((Inet4Address)gateway);
                }
            } else if (r.hasGateway() == false) {
                LinkAddress dest = r.getDestination();
                if (dest.getAddress() instanceof Inet4Address) {
                    info.netmask = NetworkUtils.prefixLengthToNetmaskInt(
                            dest.getNetworkPrefixLength());
                }
            }
        }
        int dnsFound = 0;
        for (InetAddress dns : mDhcpResults.linkProperties.getDnses()) {
            if (dns instanceof Inet4Address) {
                if (dnsFound == 0) {
                    info.dns1 = NetworkUtils.inetAddressToInt((Inet4Address)dns);
                } else {
                    info.dns2 = NetworkUtils.inetAddressToInt((Inet4Address)dns);
                }
                if (++dnsFound > 1) break;
            }
        }
        InetAddress serverAddress = mDhcpResults.serverAddress;
        if (serverAddress instanceof Inet4Address) {
            info.serverAddress = NetworkUtils.inetAddressToInt((Inet4Address)serverAddress);
        }
        info.leaseDuration = mDhcpResults.leaseDuration;

        if(!mHWConnected){
            info.ipAddress = 0;
            info.netmask = 0;
            info.dns1 = 0;
            info.gateway = 0;
        }
        return info;

    }

    private Handler.Callback mTrackerHandlerCallback = new Handler.Callback() {
        String ifname;
        /** {@inheritDoc} */
        public boolean handleMessage(Message msg) {
            if (mInterfaceName == null) {
                log(3, TAG, "mInterfaceName undefined. DISCARD message");
                return true;
            }
            synchronized (this) { //TODO correct 'this' object?
                EthernetDevInfo info;
                boolean newNetworkstate = false;
                if (mEM.getEthState() == EthernetManager.ETH_STATE_DISABLED){
                     if(msg.what == EVENT_HW_PHYCONNECTED) {
                           mHWConnected = true;
                     }else if(msg.what == EVENT_HW_PHYDISCONNECTED) {
                           mHWConnected = false;
                     }
                }
                if (mEM.getEthState() == EthernetManager.ETH_STATE_DISABLED &&
                    msg.what != EVENT_HW_DISCONNECTED) {
                    log(3, TAG, "Ethernet is DISABLED. DISCARD message");
                    return true;
                }
                switch (msg.what) {
                case EVENT_DEVICE_ADDED:
                    log(3, TAG, "[EVENT_DEVICE_ADDED]");
                    log(3, TAG, mInterfaceName + " added, Force to up it to driver dhcp");
                    NetworkUtils.enableInterface(mInterfaceName);
                    break;

                case EVENT_DEVICE_REMOVED:
                    log(3, TAG, "[EVENT_DEVICE_REMOVED]");
                    log(3, TAG, "post Notification: EVENT_HW_PHYDISCONNECTED");
                    mDhcpResults = null;
                    mLinkProperties = null;
                    postNotification(EVENT_HW_PHYDISCONNECTED);
                    break;

                case EVENT_INTERFACE_CONFIGURATION_SUCCEEDED:
                    log(2, TAG, "[EVENT_INTERFACE_CONFIGURATION_SUCCEEDED]");
                    mStackConnected = true;
                    //mHWConnected = true;
                    mDhcpRetry = 0;
                    mTrackerTarget.removeMessages(EVENT_REQUEST_RESET);
                    mEM.rewrite_conf(SystemProperties.getInt("dhcp."+ mInterfaceName +".dec_pw", 0));
                    info = mEM.getSavedEthConfig();
                    if ((mDualStackTarget != null) && (info != null) &&
                            (info.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_DHCP_AUTH)
                            || info.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_PPPOE))) {
                        Slog.i(TAG, "Send EVENT_IPOE_SUCCEEDED to DualStackService");
                        mDualStackTarget.sendEmptyMessage(DualStackManager.EVENT_IPOE_SUCCEEDED);
                    }
                    if ((info != null) && (info.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_DHCP)
                            || info.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_DHCP_AUTH))) {
                        mDhcpResults = (DhcpResults)msg.obj;
                        if(mDhcpResults == null) {
                            log(1, TAG, "mDhcpResults is null");
                            mStackConnected = false;
                            return true;
                        }
                        log(2, TAG, mDhcpResults.toString() );
                        mLinkProperties = mDhcpResults.linkProperties;
                        mLinkProperties.setInterfaceName(mInterfaceName);
                        if (mEM.ethConfigured() && info.hasProxy())
                            mLinkProperties.setHttpProxy(new ProxyProperties(info.getProxyHost(),
                                    info.getProxyPort(), info.getProxyExclusionList()));
                        else
                            mLinkProperties.setHttpProxy(null);
                    }

                    if (mEM.isEthernetDeviceAdded(mInterfaceName)) {
                        log(3, TAG, "Ether "+ mInterfaceName + " is added" );
                        newNetworkstate = true;
                    }

                    String vlanID = null;
                    if ("unicom".equals(SystemProperties.get("sys.proj.type"))
                            && "shandong".equals(SystemProperties.get("sys.proj.tender.type"))) {
                        vlanID = SystemProperties.get("persist.sys.dhcp.vlan_id", null);
                        log(2, TAG, "persist.sys.dhcp.vlan_id=" + vlanID);
                    } else {
                        vlanID = SystemProperties.get("dhcp.plus.vlan_id", null);
                        log(2, TAG, "dhcp.plus.vlan_id=" + vlanID);
                    }
                    int vlan_id = 0;
                    try {
                        vlan_id = Integer.parseInt(vlanID);
                    } catch(Exception e) {
                        vlan_id = 0;
                    }
                    if (vlan_id > 0 && info.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_DHCP))
                        NetworkUtils.resetConnections("eth0."+vlanID, NetworkUtils.RESET_IPV4_ADDRESSES);
		if (vlan_id > 0 && ((null != info && null != info.getConnectMode()
                        && info.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_DHCP_AUTH))
                        || SystemProperties.getBoolean("sys.start.Bplan", true))) {
                        if (mVM != null) {
                            if(mVM.getVlanState() != VlanManager.VLAN_STATE_ENABLED) {
                                mVM.setVlanEnabled(true);
                                log(2, TAG, "enable vlan!");
                            }
                            String vlan_if = "eth0." + vlanID;
                            log(2, TAG, "start vlan ifname: " + vlan_if);
                            VlanDevInfo vinfo = new VlanDevInfo();
                            vinfo.setConnectMode(VlanDevInfo.VLAN_CONN_MODE_DHCP_AUTH);
                            vinfo.setIfName(vlan_if);
                            vinfo.setIpAddress(null);
                            vinfo.setRouteAddr(null);
                            vinfo.setDnsAddr(null);
                            vinfo.setNetMask(null);
                            mVM.updateVlanDevInfo(vinfo);
                        }
                  	}
                    setEthState(newNetworkstate, msg.what);
                    break;
                case EVENT_INTERFACE_CONFIGURATION_FAILED:
                    log(3, TAG, "[EVENT_INTERFACE_CONFIGURATION_FAILED]");
                    mStackConnected = false;
                    setEthState(newNetworkstate, msg.what);

                    String prop_val = SystemProperties.get("net.dhcp.repeat", "disable");
                    if ((mDualStackTarget == null) && prop_val.equals("enabled")) {
                        log(3, TAG, "net.dhcp.repeat is enabled");
                        info = mEM.getSavedEthConfig();
                        if ((info != null) && (info.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_DHCP)
                                || info.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_DHCP_AUTH))) {
                            Message message = new Message();
                            message.what = EVENT_REQUEST_RESET;
                            mTrackerTarget.removeMessages(EVENT_REQUEST_RESET);
                            mTrackerTarget.sendMessageDelayed(message, 3000);
                        }
                    }

                    break;
                case EVENT_REQUEST_RESET:
                    log(2, TAG , "===EVENT_REQUEST_RESET==, retry: " + mDhcpRetry);
                    if (mDhcpRetry >= mDhcpRetryCount) {
                        log(2, TAG , "EVENT_REQUEST_RESET, retry " + mDhcpRetry + " over " + mDhcpRetryCount);
			/*add by zhaolianghua for guizhou IPOE @20191025 start*/
			if(SystemProperties.get("ro.ysten.province","master").equalsIgnoreCase("cm201_guizhou")){
				/*IPOE fail 3 times will change to dhcp mode*/
				info = mEM.getSavedEthConfig();
				info.setConnectMode(EthernetDevInfo.ETH_CONN_MODE_DHCP);
				mEM.updateEthDevInfo(info);
			}
			/*add by zhaolianghua for guizhou IPOE @20191025 end*/
                    } else {
                        info = mEM.getSavedEthConfig();
                        mEM.updateEthDevInfo(info);
                        mDhcpRetry++;
                    }
                    break;
                case EVENT_HW_CONNECTED:
                    log(3, TAG, "[EVENT: IP is configured]");
                    //mHWConnected = true;
                    if (mEM.isEthernetDeviceAdded(mInterfaceName)){
                        log(3, TAG, "Ether " + mInterfaceName + "is added" );
                        newNetworkstate = true;
                    }

                    SystemProperties.set("net.eth0.hw.status", "connected");
                    //setEthState(newNetworkstate, msg.what);
                    break;
                case EVENT_HW_DISCONNECTED:
                case EVENT_HW_PHYDISCONNECTED:
                    log(3, TAG, "[EVENT: ether(" + (String)msg.obj + ") is removed]");
                    SystemProperties.set("net.eth0.status", "disconnected");
                    SystemProperties.set("net.eth0.hw.status", "disconnected");
                    SystemProperties.set("dhcp6c.eth0.ipaddress", "");

                    mDhcpRetry = 0;
                    mTrackerTarget.removeMessages(EVENT_REQUEST_RESET);
                    if ((mWifiManager != null) && "true".equals(SystemProperties.get("net.ethwifi.mutex"))) {
                        log(3, TAG, "Ethernet is Link down, open wifi!");
                        mWifiManager.setWifiEnabled(true);
                    }
                    if (mVM != null) {
                        String vlanIerface = mVM.getInterfaceName();
                        log(2, TAG, "remove vlan: " + vlanIerface);
                        if ((vlanIerface != null) && vlanIerface.contains("eth0.") &&
                                mVM.getVlanState() == VlanManager.VLAN_STATE_ENABLED) {
                            NetworkUtils.removeVlan(vlanIerface);
                            log(2, TAG, "removed vlan!");
                        }
                    }

                    mDhcpResults = null;
                    mLinkProperties = null;
                    mHWConnected = false;
                    mStackConnected = false;
                    setEthState( false, msg.what);
                    if (mSystemReady) {
                        Intent intent = new Intent("android.net.ethernet.unplug.cable");
                        //intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
                        //mContext.sendBroadcastAsUser(intent, UserHandle.OWNER);
                        log(2, TAG, "android.net.ethernet.unplug.cable!!!!!!!");
                        mContext.sendBroadcast(intent);
                    }
                    break;
                case EVENT_IP_REMOVED:
                    log(3, TAG, "[EVENT: IP is cleared]");

                    //mHWConnected = true;
                    setEthState( false, msg.what);
                    break;
                case EVENT_HW_PHYCONNECTED:
                    ifname = (String) msg.obj;
                    log(3, TAG, "[EVENT: Ether(" + ifname + ") is up]");
                    SystemProperties.set("net.eth0.hw.status", "connected");

                    if(mHWConnected &&mEM.isEthDeviceUp()&&ifname.equals(mInterfaceName)) {
                        log(3, TAG, "Ethernet is Link up, do not do again!");
                        return true;
                    }
                    if(!mEM.isEthDeviceUp()&&ifname.equals(mInterfaceName)) {
                        log(3, TAG, "Ethernet is Link down, do not do again!");
                        return true;
                    }
                    if ((mWifiManager != null) && "true".equals(SystemProperties.get("net.ethwifi.mutex"))) {
                        log(3, TAG, "Ethernet is Link up, close wifi!");
                        mWifiManager.setWifiEnabled(false);
                    }
                    mHWConnected = true;
                    newNetworkstate = mNetworkInfo.isConnected();
                    info = mEM.getSavedEthConfig();
                    if (mEM.isEthernetDeviceAdded(mInterfaceName) && (mDualStackTarget == null) && (info != null)) {
                        if (info.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_PPPOE)) {
                            setEthState(newNetworkstate, EVENT_HW_PHYCONNECTED);
                            pppoeConnect();
                            return true;
                        }
                    } 

                    setEthState(newNetworkstate, EVENT_HW_PHYCONNECTED);
                    int state = mEM.getEthState();
                    if (state != EthernetManager.ETH_STATE_DISABLED) {
                        info = mEM.getSavedEthConfig();
                        log(1, TAG, "info:"+info+" is Configured"+mEM.ethConfigured() );
                        if (info == null || !mEM.ethConfigured()) {
                            String ifname = (String)msg.obj;
                            info = new EthernetDevInfo();
                            info.setIfName(ifname);
                        }
                        info.setIfName(ifname);
                        mEM.updateEthDevInfo(info);
                    }
                    break;
                }
            }
            return true;
        }
    };

    public void notifyPhyConnected(String ifname) {
        log(3, TAG, "report interface is up for " + ifname);
        synchronized(this) {
            if (mTrackerTarget == null) {
                log(3, TAG, "mTrackerTarget == null");
                return;
            }
            if (mInterfaceName == null) mInterfaceName = ifname;
            Message msg = mTrackerTarget.obtainMessage(EVENT_HW_PHYCONNECTED, ifname);
            msg.sendToTarget();
        }
    }

    public void notifyPhyDisConnected(String ifname) {
        log(3, TAG, "report interface is down for " + ifname);
        synchronized(this) {
            if (mTrackerTarget == null) {
                 try {
                        Thread.sleep(200);
                        if (mTrackerTarget == null) {
                             log(3, TAG, "mTrackerTarget null return");
                             return;
                        }
                     } catch (Exception e){};
            }
            log(3, TAG, "send hw disconnect msg");
            if (mInterfaceName == null) mInterfaceName = ifname;
            Message msg = mTrackerTarget.obtainMessage(EVENT_HW_PHYDISCONNECTED, ifname);
            msg.sendToTarget();
        }
    }

    public void notifyDeviceAdded(String ifname) {
        log(3, TAG, ifname + " ADDED");
        log(3, TAG, "mInterfaceName: " + mInterfaceName);

        if ((mInterfaceName == null) || ifname.equals(mInterfaceName)) {
            if (mInterfaceName == null) mInterfaceName = ifname;
            synchronized(this) {
                Message msg = mTrackerTarget.obtainMessage(EVENT_DEVICE_ADDED, ifname);
                msg.sendToTarget();
            }
        }
    }

    public void notifyDeviceRemoved(String ifname) {
        log(3, TAG, ifname + " REMOVED");
        if (ifname.equals(mInterfaceName)) {
            synchronized(this) {
                Message msg = mTrackerTarget.obtainMessage(EVENT_DEVICE_REMOVED, ifname);
                msg.sendToTarget();
            }
        }
    }

    public void notifyIpRemoved(String ifname) {
        log(3, TAG, ifname + " IP removed");
        if (ifname.equals(mInterfaceName)) {
            synchronized(this) {
                Message msg = mTrackerTarget.obtainMessage(EVENT_IP_REMOVED, ifname);
                msg.sendToTarget();
            }
        }
    }


    public void notifyStateChange(String ifname, DetailedState state) {
        log(4, TAG, "report new state " + state.toString() + " on dev " + ifname + " current=" + mInterfaceName);
        if (mInterfaceName == null) mInterfaceName = ifname;
        if (ifname.equals("eth0") || ifname.equals("usbnet0") || ifname.equals("br0")) {
            log(4, TAG, "update network state tracker");
            synchronized(this) {
                int hw_state = state.equals(DetailedState.CONNECTED) ? EVENT_HW_CONNECTED : EVENT_HW_DISCONNECTED;
                Message msg = mTrackerTarget.obtainMessage(hw_state, ifname);
                msg.sendToTarget();
            }
        }
    }

    public void setDependencyMet(boolean met) {
        // not supported on this network
    }

    /**
     * Informs the state tracker that another interface is stacked on top of it.
     **/
    public void addStackedLink(LinkProperties link){
    }

    /**
     * Informs the state tracker that a stacked interface has been removed.
     **/
    public void removeStackedLink(LinkProperties link){
    }

    /*
     * Called once to setup async channel between this and
     * the underlying network specific code.
     */
    public void supplyMessenger(Messenger messenger){
    }

    /*
     * Network interface name that we'll lookup for sampling data
     */
    public String getNetworkInterfaceName(){
        return null;
    }

    /*
     * Save the starting sample
     */
    public void startSampling(SamplingDataTracker.SamplingSnapshot s){
    }

    /*
     * Save the ending sample
     */
    public void stopSampling(SamplingDataTracker.SamplingSnapshot s) {
    }

    @Override
    public void captivePortalCheckCompleted(boolean isCaptivePortal) {
        // not implemented
    }


    /**
     * Get interesting information about this network link
     * @return a copy of link information, null if not available
     */
    public LinkQualityInfo getLinkQualityInfo(){
        return null;
    }

    public void setEthernetStateMachine(EthernetStateMachine esm) {
        mESM = esm;
    }

    private void startDualStackIpoe(EthernetDevInfo info) {
        if (!mStartingDhcp) {
            Slog.i(TAG, "startDualStackIpoe, trigger dhcp for device " + info.getIfName());
            mStartingDhcp = true;
            mESM.startDhcp(info.getIfName());
            if (mDualStackTarget != null) {
                Slog.i(TAG, "Send EVENT_IPOE_START to DualStackService");
                mDualStackTarget.sendEmptyMessage(DualStackManager.EVENT_IPOE_START);
            }
        } else {
            Slog.w(TAG, "startDualStackIpoe mStartingDhcp is true");
        }
    }

    public void startDualStackIpoe() {
        EthernetDevInfo info = mEM.getSavedEthConfig();
        if (info != null) {
            startDualStackIpoe(info);
        } else {
            Slog.w(TAG, "startDualStackIpoe info is null!");
        }
    }

    public void disconnectDualStackIpoe() {
        Slog.i(TAG, "disconnectDualStackIpoe mStartingDhcp is " + mStartingDhcp);
        if (mStartingDhcp) {
            mStartingDhcp = false;
            if (mInterfaceName != null)
                NetworkUtils.stopDhcp(mInterfaceName);
            if (mESM != null) mESM.stopDhcp();
            setEthState(false, EVENT_HW_DISCONNECTED);
        }
    }

    public void setDualStackTarget(Handler target) {
        mDualStackTarget = target;
    }

    public void systemReady() {
        log(2, TAG, "systemReady, mHWConnected: " + mHWConnected);
        mSystemReady = true;
        if (!mHWConnected) {
        		if (!"unicom".equals(SystemProperties.get("sys.proj.type"))
							&& !"shandong".equals(SystemProperties.get("sys.proj.tender.type"))) {
								postNotification(EVENT_HW_PHYDISCONNECTED);
						}
        }
    }

    void setNetLed(boolean st) {
        if (getNetworkInfo().isConnected()) {
            // if ipv4 connected already, ipv6 not impact the LED.
            Slog.w(TAG, "ipv4 already connected, keep net LED turn on");
            return;
        }
        String val = st?"on":"off";
        try {
            final String prefix = "/proc/ledlight/netled/state";
            FileUtils.stringToFile(prefix, val);
        } catch (IOException e) {
            Slog.e(TAG, "Can't set net led:" + e);
        }
    }

    private void log(int Log_id , String TAG , String msg){
      if (SystemProperties.getInt(PROPERTIES_DEBUG, 1) == 1) {
        if (0 == Log_id)
          Slog.e(TAG , msg);
        else if (1 == Log_id)
          Slog.w(TAG , msg);
        else if (2 == Log_id)
          Slog.d(TAG , msg);
        else if (3 == Log_id)
          Slog.i(TAG , msg);
        else if (4 == Log_id)
          Slog.v(TAG , msg);
      }
    }

}
