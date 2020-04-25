package android.net.ethernet;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.DhcpInfoInternal;
import android.net.DhcpStateMachine;
import android.net.Dhcpv6StateMachine;
import android.net.InterfaceConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.Handler;
import android.provider.Settings;
import android.util.EventLog;
import android.util.Log;
import android.util.LruCache;

import com.android.internal.util.Protocol;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import android.util.*;


/**
 * Track the state of Ethernet connectivity. All event handling is done here,
 * and all changes in connectivity state are initiated here.
 *
 *
 * @hide
 */
public class EthernetStateMachine extends StateMachine {

    private static final String TAG = "EthernetStateMachine";

    private static final boolean DBG = true;
    private EthernetManager mEM = null;

    private Handler mTrackerTarget = null;

    private ConnectivityManager mCm;

    private LinkProperties mLinkProperties;

    private Context mContext;

    private DhcpInfoInternal mDhcpInfoInternal;
    private NetworkInfo mNetworkInfo;
    private DhcpStateMachine mDhcpStateMachine;
    private Dhcpv6StateMachine mV6DhcpStateMachine;


    /* The base for wifi message types */
    static final int BASE = Protocol.BASE_WIFI;

    /* Indicates Static IP succeded */
    static final int CMD_STATIC_IP_SUCCESS                = BASE + 15;
    /* Indicates Static IP failed */
    static final int CMD_STATIC_IP_FAILURE                = BASE + 16;

    /* Default parent state */
    private State mDefaultState = new DefaultState();
    /* Temporary initial state */
    private State mInitialState = new InitialState();

    private State mEtherRemovedState = new EtherRemovedState();

    private State mEtherAddedState = new EtherAddedState();

    /* Fetching IP */
    private State mConnectingState = new ConnectingState();

    /* Connected with IP addr */
    private State mConnectedState = new ConnectedState();

    /* disconnect issued, waiting for network disconnect confirmation */
    private State mDisconnectingState = new DisconnectingState();

    /* Network is not connected */
    private State mDisconnectedState = new DisconnectedState();


    public EthernetStateMachine(Context context,int networkType) {
        super((networkType == ConnectivityManager.TYPE_VLAN) ? "VlanStateMachine" : TAG);

        mContext = context;

        mNetworkInfo = new NetworkInfo(networkType, 0, ConnectivityManager.getNetworkTypeName(networkType), "");

        mDhcpInfoInternal = new DhcpInfoInternal();

        mLinkProperties = new LinkProperties();

        mNetworkInfo.setIsAvailable(false);
        mLinkProperties.clear();


        addState(mDefaultState);
            addState(mInitialState, mDefaultState);
            addState(mEtherAddedState, mDefaultState);
            addState(mEtherRemovedState, mDefaultState);
            addState(mConnectingState, mDefaultState);
            addState(mConnectedState, mDefaultState);
            addState(mDisconnectingState, mDefaultState);
            addState(mDisconnectedState, mDefaultState);

        setInitialState(mInitialState);

        if (DBG) setDbg(true);

        //start the state machine
        start();
    }

    private boolean isDhcpConnectMode() {
        final ContentResolver cr = mContext.getContentResolver();
        String connectMode = Settings.Secure.getString(cr, Settings.Secure.ETH_MODE);
        Slog.d(TAG, "ETH_MODE: " + connectMode + "\n");
        String dualStack = Settings.Secure.getString(cr, Settings.Secure.ETH_DUALSTACK);
        logd("ETH_MODE: " + connectMode + "\n");
        logd("DUALSTACK: " + dualStack + "\n");
        if ("true".equals(dualStack)) {
            return true;
        }
        if (EthernetDevInfo.ETH_CONN_MODE_DHCP_AUTH.equals(connectMode)
                || EthernetDevInfo.ETH_CONN_MODE_DHCP.equals(connectMode)) {
            return true;
        } else {
            return false;
        }
    }
    private boolean isV6DhcpConnectMode() {
        final ContentResolver cr = mContext.getContentResolver();
        String connectMode = Settings.Secure.getString(cr, Settings.Secure.ETHV6_MODE);
        Slog.d(TAG, "ETH_MODE: " + connectMode + "\n");
        String dualStack = Settings.Secure.getString(cr, Settings.Secure.ETH_DUALSTACK);
        logd("ETH_MODE: " + connectMode + "\n");
        logd("DUALSTACK: " + dualStack + "\n");
        if ("true".equals(dualStack)) {
            return true;
        }
        if (EthernetDevInfo.ETH_CONN_MODE_DHCP_AUTH.equals(connectMode)
                || EthernetDevInfo.ETH_CONN_MODE_DHCP.equals(connectMode)) {
            return true;
        } else {
            return false;
        }
    }

    /*********************************************************
     * Methods exposed for public use
     ********************************************************/
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        String LS = System.getProperty("line.separator");
        sb.append("current HSM state: ").append(getCurrentState().getName()).append(LS);
        sb.append("mLinkProperties ").append(mLinkProperties).append(LS);
        sb.append("mDhcpInfoInternal ").append(mDhcpInfoInternal).append(LS);
        sb.append("mNetworkInfo ").append(mNetworkInfo).append(LS);

        return sb.toString();
    }


    void handlePreDhcpSetup() {
        Slog.d(TAG, "Received CMD_PRE_DHCP_ACTION, DO NOTHING currently :-)\n");
    }


    void handlePostDhcpSetup() {
        Slog.d(TAG, "Received CMD_POST_DHCP_ACTION, DO NOTHING currently :-)\n");
    }

    void handleStopDhcpSetup() {
        Slog.d(TAG, "Received CMD_STOP_DHCP_ACTION, DO NOTHING currently :-)\n");
    }

    void handlePreDhcpV6Setup() {
        Slog.d(TAG, "Received CMD_PRE_DHCPV6_ACTION, DO NOTHING currently :-)\n");
    }


    void handlePostDhcpV6Setup() {
        Slog.d(TAG, "Received CMD_POST_DHCPV6_ACTION, DO NOTHING currently :-)\n");
    }

    void handleStopDhcpV6Setup() {
        Slog.d(TAG, "Received CMD_STOP_DHCPV6_ACTION, DO NOTHING currently :-)\n");
    }


    public void startDhcp(String ifname) {
        if (DBG) Slog.d(TAG, "startDhcp: send CMD_START_DHCP");
        if (mDhcpStateMachine == null) {
            mDhcpStateMachine = DhcpStateMachine.makeDhcpStateMachine(
                    mContext, EthernetStateMachine.this, ifname);
        }
        else if (!ifname.equals(mDhcpStateMachine.getInterfaceName())) {
            if (DBG) Slog.d(TAG, "startDhcp on another intferface" );
            mDhcpStateMachine.sendMessage(DhcpStateMachine.CMD_STOP_DHCP);
            mDhcpStateMachine = DhcpStateMachine.makeDhcpStateMachine(
                    mContext, EthernetStateMachine.this, ifname);
        }
        else {
            if (DBG) Slog.d(TAG, "startDhcp: DhcpStateMachine exists!!!");
        }
        //mDhcpStateMachine.registerForPreDhcpNotification();
        mDhcpStateMachine.sendMessage(DhcpStateMachine.CMD_START_DHCP);
    }

    public void startDhcpV6(String ifname) {
        if (DBG) Slog.d(TAG, "startDhcp: send CMD_START_DHCPV6");
        if (mV6DhcpStateMachine == null) {
            mV6DhcpStateMachine = Dhcpv6StateMachine.makeDhcpv6StateMachine(
                    mContext, EthernetStateMachine.this, ifname);
        }
        else if (!ifname.equals(mV6DhcpStateMachine.getInterfaceName())) {
            if (DBG) Slog.d(TAG, "startDhcp on another intferface" );
            mV6DhcpStateMachine.sendMessage(Dhcpv6StateMachine.CMD_STOP_DHCPV6);
            mV6DhcpStateMachine = Dhcpv6StateMachine.makeDhcpv6StateMachine(
                    mContext, EthernetStateMachine.this, ifname);
        }
        else {
            if (DBG) Slog.d(TAG, "startDhcp: DhcpV6StateMachine exists!!!");
        }
        //mDhcpStateMachine.registerForPreDhcpNotification();
        mV6DhcpStateMachine.sendMessage(Dhcpv6StateMachine.CMD_START_DHCPV6);
    }


    public void stopDhcp() {
        if (DBG && mDhcpStateMachine == null) Slog.d(TAG, "stopDhcp: DO NOTHING");
        if (mDhcpStateMachine != null) {
            if (DBG) Slog.d(TAG, "stopDhcp: send CMD_STOP_DHCP");
            mDhcpStateMachine.sendMessage(DhcpStateMachine.CMD_STOP_DHCP);
        }
    }

    public void stopDhcpV6() {
        if (DBG && mV6DhcpStateMachine == null) Slog.d(TAG, "stopDhcpV6: DO NOTHING");
        if (mV6DhcpStateMachine != null) {
            if (DBG) Slog.d(TAG, "stopDhcp: send CMD_STOP_DHCPV6");
            mV6DhcpStateMachine.sendMessage(Dhcpv6StateMachine.CMD_STOP_DHCPV6);
        }
    }

    /********************************************************
     * HSM states
     *******************************************************/

    class DefaultState extends State {
        @Override
        public boolean processMessage(Message message) {
            if (DBG) Slog.d(TAG, getName() + message.toString() + "\n");

            return HANDLED;
        }
    }

    class InitialState extends State {
        @Override
        public void enter() {
            if (DBG) Slog.d(TAG, getName() + "\n");
            if (mEM == null) {
                Slog.d(TAG, "EthernetManager not inited\n");
            }
        }

        @Override
        public boolean processMessage(Message message) {
            if (DBG) Slog.d(TAG, getName() + message.toString() + "\n");
            switch(message.what) {
            case DhcpStateMachine.CMD_PRE_DHCP_ACTION:
                handlePreDhcpSetup();
                mDhcpStateMachine.sendMessage(DhcpStateMachine.CMD_PRE_DHCP_ACTION_COMPLETE);
                break;
            case DhcpStateMachine.CMD_POST_DHCP_ACTION:
                handlePostDhcpSetup();
                if(!isDhcpConnectMode()) {
                    Slog.d(TAG, "ConnectMode is changed, is not dhcp, do not send dhcp fail or success msg!\n");
                    break;
                }
                if (message.arg1 == DhcpStateMachine.DHCP_SUCCESS) {
                    Message msg = mTrackerTarget.obtainMessage(EthernetStateTracker.EVENT_INTERFACE_CONFIGURATION_SUCCEEDED, message.obj);
                    msg.sendToTarget();
                } else if (message.arg1 == DhcpStateMachine.DHCP_FAILURE) {
                    mTrackerTarget.sendEmptyMessage(EthernetStateTracker.EVENT_INTERFACE_CONFIGURATION_FAILED);
                }
                break;
            case DhcpStateMachine.CMD_STOP_DHCP:
                handleStopDhcpSetup();
                break;
            case Dhcpv6StateMachine.CMD_PRE_DHCPV6_ACTION:
                handlePreDhcpV6Setup();
                mV6DhcpStateMachine.sendMessage(Dhcpv6StateMachine.CMD_PRE_DHCPV6_ACTION_COMPLETE);
                break;
            case Dhcpv6StateMachine.CMD_POST_DHCPV6_ACTION:
                handlePostDhcpV6Setup();
                if (!isV6DhcpConnectMode()) {
                    Slog.d(TAG, "ConnectMode is changed, is not dhcp, do not send dhcp fail or success msg!\n");
                    break;
                }
                if (message.arg1 == Dhcpv6StateMachine.DHCPV6_SUCCESS) {
                    Message msg = mTrackerTarget.obtainMessage(EthernetV6StateTracker.EVENT_INTERFACE_CONFIGURATION_SUCCEEDED, message.obj);
                    msg.sendToTarget();
                } else if (message.arg1 == Dhcpv6StateMachine.DHCPV6_FAILURE) {
                    mTrackerTarget.sendEmptyMessage(EthernetV6StateTracker.EVENT_INTERFACE_CONFIGURATION_FAILED);
                }
                break;
            case Dhcpv6StateMachine.CMD_STOP_DHCPV6:
                handleStopDhcpV6Setup();
                break;
            default:
                return NOT_HANDLED;
            }

            return HANDLED;
        }
    }


    class EtherAddedState extends State {
        @Override
        public void enter() {
            if (DBG) Slog.d(TAG, getName() + "\n");
        }

        @Override
        public boolean processMessage(Message message) {
            if (DBG) Slog.d(TAG, getName() + message.toString() + "\n");

            return HANDLED;
        }

        @Override
        public void exit() {
        }
    }


    class EtherRemovedState extends State {
        @Override
        public void enter() {
            if (DBG) Slog.d(TAG, getName() + "\n");
        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) Slog.d(TAG, getName() + message.toString() + "\n");

            return HANDLED;
        }

        @Override
        public void exit() {
        }
    }


    class ConnectingState extends State {

        @Override
        public void enter() {
            if (DBG) Slog.d(TAG, getName() + "\n");
        }
      @Override
      public boolean processMessage(Message message) {
          if (DBG) Slog.d(TAG, getName() + message.toString() + "\n");

          return HANDLED;
      }
    }

    class ConnectedState extends State {
        @Override
        public void enter() {
            if (DBG) Slog.d(TAG, getName() + "\n");

        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) Slog.d(TAG, getName() + message.toString() + "\n");

            return HANDLED;
        }
        @Override
        public void exit() {

        }
    }

    class DisconnectingState extends State {
        @Override
        public void enter() {
            if (DBG) Slog.d(TAG, getName() + "\n");

        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) Slog.d(TAG, getName() + message.toString() + "\n");

            return HANDLED;
        }
    }

    class DisconnectedState extends State {
        @Override
        public void enter() {
            if (DBG) Slog.d(TAG, getName() + "\n");


        }
        @Override
        public boolean processMessage(Message message) {
            if (DBG) Slog.d(TAG, getName() + message.toString() + "\n");

            return HANDLED;
        }

        @Override
        public void exit() {
        }
    }

    public void setTrackerTarget(Handler trackerTarget) {
        mTrackerTarget = trackerTarget;
    }
}

