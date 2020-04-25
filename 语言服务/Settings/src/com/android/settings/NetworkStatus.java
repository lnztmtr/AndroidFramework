package com.android.settings;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;

import android.app.Activity;
import android.app.Fragment;
import android.content.ClipData.Item;
import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.net.ConnectivityManager;
import android.net.NetworkUtils;
import android.net.NetworkInfo;
import android.net.DhcpInfo;
import android.net.DhcpResults;
import android.net.LinkAddress;
import android.net.pppoe.PppoeManager;
import android.net.RouteInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.ethernet.EthernetDevInfo;
import android.net.ethernet.EthernetManager;
import android.text.StaticLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.ListView;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemProperties;

import com.android.settings.R;
import com.android.settings.Utils;

public class NetworkStatus extends Fragment implements
        TabHost.TabContentFactory, TabHost.OnTabChangeListener {
    private static final String TAG = "NetworkStatus";
    private static final boolean DEBUG = false;
    
    private EthernetManager mEthManager;
    private WifiManager mWifiManager;
    private PppoeManager mPppoeManager;

    // attributes used as keys when passing values to InstalledAppDetails activity
    public static final String MAC_ADDRESS = "/sys/class/net/eth0/address";
    public static final String ETHERNET_DEV_STATUS = "/sys/class/net/eth0/operstate";
    public static final String WIFI_DEV_STATUS = "/sys/class/net/wlan0/operstate";
    public static final String PPPOE_DEV_STATUS = "/sys/class/net/ppp0/operstate";

    // layout inflater object used to inflate views
    private LayoutInflater mInflater;
    private TabHost mTabHost;
    private View mRootView;
    private String mDefaultTab = null;
    private View mListContainer;
    private View mListAdapter;
    private View mLoadingContainer;
    private ListView mListView;
    private TextView mStatusName;
    private TextView mStatusInfo;

    static final String TAB_WIFI = "WIFI";
    static final String TAB_ETHNET = "IPV4";
    static final String TAB_PPPOE = "PPPOE";
    static final String TAB_IPV6 = "IPV6";
    String[] netstatusname;
    String[] netstatusinfo;

    static class TextViewHoder{
        TextView statusname;
        TextView statusinfo;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mWifiManager = (WifiManager) getActivity().getSystemService(getActivity().WIFI_SERVICE);
        mEthManager = (EthernetManager) getActivity().getSystemService(Context.ETH_SERVICE);
        mPppoeManager = (PppoeManager) getActivity().getSystemService(Context.PPPOE_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mRootView = inflater.inflate(R.layout.network_status, container, false);
        mListContainer = mRootView.findViewById(R.id.list_view);
        ListView lv = (ListView) mListContainer.findViewById(R.id.listview);
        Resources res =getResources();
        netstatusname = res.getStringArray(R.array.network_status_entries);
        mListView =lv;
        // this tmpinfo array should be wifi status info,now have no idea to get wifi info,so set null at present
        String ip_address = null;
        String subnet_mask = null;
        String gate_way = null;
        String dns = null;
        String mac_address = null;
        mac_address = getWifiMacAddress();
        String [] tmpinfo;
        String wifi_dev_status = getIfDevStatus(WIFI_DEV_STATUS);
        if("up".equals(wifi_dev_status)) {
            DhcpInfo dinfo = mWifiManager.getDhcpInfo();
            if(dinfo != null) {
                ip_address = NetworkUtils.intToInetAddress(dinfo.ipAddress).getHostAddress();
                subnet_mask = NetworkUtils.intToInetAddress(dinfo.netmask).getHostAddress();
                gate_way = NetworkUtils.intToInetAddress(dinfo.gateway).getHostAddress();
                dns = NetworkUtils.intToInetAddress(dinfo.dns1).getHostAddress();
            }
        }
        tmpinfo = new String[] {ip_address, subnet_mask, gate_way, dns, mac_address};
        NetStatusAdapter netStatusAdapter = new NetStatusAdapter(getActivity(), netstatusname, tmpinfo);
        mListView.setAdapter(netStatusAdapter);
        mTabHost = (TabHost) inflater.inflate(R.layout.network_status_tabhost, container, false);
        mTabHost.setup();
        mTabHost.addTab(mTabHost.newTabSpec(TAB_WIFI)
                .setIndicator(getActivity().getString(R.string.wifistatus),
                    getActivity().getResources().getDrawable(R.drawable.ic_tab_download))
                .setContent(this));
        mTabHost.addTab(mTabHost.newTabSpec(TAB_ETHNET)
                .setIndicator(getActivity().getString(R.string.ethnet_status),
                    getActivity().getResources().getDrawable(R.drawable.ic_tab_sdcard))
                .setContent(this));
        mTabHost.addTab(mTabHost.newTabSpec(TAB_PPPOE)
                .setIndicator(getActivity().getString(R.string.ppoe_status),
                    getActivity().getResources().getDrawable(R.drawable.ic_tab_running))
                .setContent(this));
        mTabHost.addTab(mTabHost.newTabSpec(TAB_IPV6)
                .setIndicator(getActivity().getString(R.string.ipv6_status),
                    getActivity().getResources().getDrawable(R.drawable.ic_tab_all))
                .setContent(this));
        mTabHost.setCurrentTabByTag(TAB_WIFI);
        mTabHost.setOnTabChangedListener(this);
        //if the numbers of tab is four, use this marked codes to resolve last tab cannot move 
        View mView = mTabHost.getTabWidget().getChildTabViewAt(1);
        if(mView != null) mView.setId(1);
        View mView1 = mTabHost.getTabWidget().getChildTabViewAt(2);
        if(mView1 != null)  mView1.setNextFocusLeftId(1);
        return mTabHost;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onTabChanged(String tabId) {
        try {
            String []statusinfo = showCurrentTab();
            NetStatusAdapter netStatusAdapter = new NetStatusAdapter(getActivity(), netstatusname, statusinfo);
            mListView.setAdapter(netStatusAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isPppOverEthernet() {
        return SystemProperties.get("net.pppoe.phyif", "unknown").startsWith("eth");
    }

    private boolean isPppOverWifi() {
        return SystemProperties.get("net.pppoe.phyif", "unknown").startsWith("wlan");
    }
    
    public String [] showCurrentTab() {
        String ip_address = null;
        String subnet_mask = null;
        String gate_way = null;
        String dns = null;
        String mac_address = null;
        String tabId = mTabHost.getCurrentTabTag();
        String ethernet_dev_status = getIfDevStatus(ETHERNET_DEV_STATUS);
        String wifi_dev_status = getIfDevStatus(WIFI_DEV_STATUS);
        String pppoe_dev_status = getIfDevStatus(PPPOE_DEV_STATUS);
        String statusinfo[];
        EthernetDevInfo info = mEthManager.getSavedEthConfig();
        statusinfo  = new String[] {null,null,null,null,null};
        if (TAB_WIFI.equalsIgnoreCase(tabId)) {
            mac_address = getWifiMacAddress();
            if("up".equals(wifi_dev_status)) {
                DhcpInfo dinfo = mWifiManager.getDhcpInfo();
                if(dinfo != null) {
                    ip_address = NetworkUtils.intToInetAddress(dinfo.ipAddress).getHostAddress();
                    subnet_mask = NetworkUtils.intToInetAddress(dinfo.netmask).getHostAddress();
                    gate_way = NetworkUtils.intToInetAddress(dinfo.gateway).getHostAddress();
                    dns = NetworkUtils.intToInetAddress(dinfo.dns1).getHostAddress();
                }
            }
            statusinfo = new String[] {ip_address, subnet_mask, gate_way, dns, mac_address};
        } else if (TAB_ETHNET.equalsIgnoreCase(tabId)) {
            mac_address = readMacAddress();
            if("up".equals(ethernet_dev_status) &&
                    (info.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_DHCP)
                    || info.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_DHCP_AUTH))) {
                Log.d(TAG, "----------connected mode:dhcp");
                DhcpInfo dinfo = mEthManager.getDhcpInfo();
                if(dinfo != null) {
                    ip_address = NetworkUtils.intToInetAddress(dinfo.ipAddress).getHostAddress();
                    subnet_mask = NetworkUtils.intToInetAddress(dinfo.netmask).getHostAddress();
                    gate_way = NetworkUtils.intToInetAddress(dinfo.gateway).getHostAddress();
                    dns = NetworkUtils.intToInetAddress(dinfo.dns1).getHostAddress();
                }
                statusinfo = new String[] {ip_address, subnet_mask, gate_way, dns, mac_address};
            } else if("up".equals(ethernet_dev_status) &&
                    info.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_MANUAL)) {
                Log.d(TAG, "----------connected mode:manual");
                ip_address = info.getIpAddress();
                subnet_mask = info.getNetMask();
                gate_way = info.getRouteAddr() ;
                dns = info.getDnsAddr();
                statusinfo = new String[] {ip_address, subnet_mask, gate_way, dns, mac_address};
            } else {
                statusinfo = new String[] {null, null, null, null, mac_address};
            }
        } else if (TAB_PPPOE.equalsIgnoreCase(tabId)) {
            if(isPppOverEthernet()) {
                mac_address = readMacAddress();
            } else if(isPppOverWifi()) {
                mac_address = getWifiMacAddress();
            }
            if((pppoe_dev_status != null) && !"down".equals(pppoe_dev_status)) {
                DhcpInfo dinfo = mPppoeManager.getDhcpInfo();
                if(dinfo != null) {
                    ip_address = NetworkUtils.intToInetAddress(dinfo.ipAddress).getHostAddress();
                    subnet_mask = NetworkUtils.intToInetAddress(dinfo.netmask).getHostAddress();
                    gate_way = NetworkUtils.intToInetAddress(dinfo.gateway).getHostAddress();
                    dns = NetworkUtils.intToInetAddress(dinfo.dns1).getHostAddress();
                }
            }
            statusinfo = new String[] {ip_address, subnet_mask, gate_way, dns, mac_address};
        } else if (TAB_IPV6.equalsIgnoreCase(tabId)) {
            mac_address = readMacAddress();
            Log.d(TAG,"----------------ipv6 status");
			if(mEthManager.getEthernetMode6() != null && mEthManager.getEthernetMode6().equals(EthernetDevInfo.ETH_CONN_MODE_PPPOE)){
                Log.d(TAG,"--------ipv6 pppoe connected");
                DhcpResults dr = mPppoeManager.getDhcpv6Result();
                if(dr != null) {

                    if (dr.linkProperties == null) return null;

                    for (LinkAddress la : dr.linkProperties.getLinkAddresses()) {
                        InetAddress addr = la.getAddress();
                        if (addr instanceof Inet6Address) {
                            ip_address = addr.getHostAddress();
                            break;
                        }
                    }

                    for (RouteInfo r : dr.linkProperties.getRoutes()) {
                       if (r.isDefaultRoute()) {
                           InetAddress gateways = r.getGateway();
                           if (gateways instanceof Inet6Address) {
                               gate_way = gateways.getHostAddress();
                           }
                       } else if (r.hasGateway() == false) {
                           LinkAddress dest = r.getDestination();
                           if (dest.getAddress() instanceof Inet6Address) {
                               subnet_mask = dest.getNetworkPrefixLength()+"";
                           }
                       }
                    }

                    int dnsFound = 0;
                    for (InetAddress dns_it : dr.linkProperties.getDnses()) {
                       if (dns_it instanceof Inet6Address) {
                           if (dnsFound == 0) {
                               dns = dns_it.getHostAddress();
                           } else {
                               dns = dns_it.getHostAddress();
                           }
                           if (++dnsFound > 1) break;
                       }
                    }
				}
			}else if(mEthManager.checkDhcpv6Status("eth0")){
                Log.d(TAG,"--------iptv connected");
                ip_address = mEthManager.getDhcpv6Ipaddress("eth0");
                int prefix = mEthManager.getDhcpv6Prefixlen("eth0");
                subnet_mask = Integer.toString(prefix);
                gate_way = mEthManager.getDhcpv6Gateway();
                dns = mEthManager.getDhcpv6Dns("eth0",1);
            }
            statusinfo = new String[] {ip_address, subnet_mask, gate_way, dns, mac_address};
        } else {
            // Invalid option. Do nothing
            // return;
        }
        return  statusinfo;
    }

    private String readMacAddress(){
        String mMac_address = null;
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(MAC_ADDRESS);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        BufferedReader bufferedReader = null;
        bufferedReader = new BufferedReader(fileReader);
        try {
            mMac_address = bufferedReader.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mMac_address;
    }

    private String getIfDevStatus(String path) {
        String dev_status = null;
        FileReader fileReader = null;

        if(path == null) return null;

        try {
            fileReader = new FileReader(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }

        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(fileReader);
            dev_status = bufferedReader.readLine();
            bufferedReader.close();
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return dev_status;
    }

    private String getWifiMacAddress() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        return macAddress;
    }

    private String getIpv4Ipaddress() {
        String ipaddress = null;
        ipaddress = Utils.getDefaultIpAddresses(getActivity());
        return ipaddress;
    }

    private String getWifiIpaddress() {
        String ipaddress = null;
        ipaddress =  Utils.getWifiIpAddresses(getActivity());
        return ipaddress;
    }

    private class NetStatusAdapter extends BaseAdapter{
        String [] netstatusname;
        String [] netstatusinfo;
        private LayoutInflater mInflater;
        public NetStatusAdapter(Context context,String [] nane,String [] info) {
            mInflater = LayoutInflater.from(context);
            netstatusname = nane;
            netstatusinfo = info;
        }

        @Override
        public int getCount() {
            return netstatusname.length;
        }

        @Override
        public Object getItem(int position) {
            return netstatusname[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextViewHoder textViewHoder = new TextViewHoder();
            convertView = mInflater.inflate(R.layout.list_content, null);

            // Creates a ViewHolder and store references to the two children views
            textViewHoder.statusname = (TextView) convertView.findViewById(R.id.status_name);
            textViewHoder.statusinfo = (TextView) convertView.findViewById(R.id.status_info);
            textViewHoder.statusname.setText(netstatusname[position]);
            textViewHoder.statusinfo.setText(netstatusinfo[position]);
            convertView.setTag(textViewHoder);
            return convertView;
        }
    }

    @Override
    public View createTabContent(String tag) {
        return mRootView;
    }
}
