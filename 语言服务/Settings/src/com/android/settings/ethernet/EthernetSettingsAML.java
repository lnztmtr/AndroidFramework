/*
 * Copyright (C) 2010 The Android-x86 Open Source Project
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
 *
 * Author: Yi Sun <beyounn@gmail.com>
 */

package com.android.settings.ethernet;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.net.ethernet.EthernetManager;
import android.net.pppoe.PppoeManager;
import android.net.pppoe.PppoeDevInfo;
import android.net.ethernet.EthernetDevInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
//import android.net.wifi.WifiManager;
import android.net.pppoe.PppoeManager;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.Gravity;
import android.widget.Switch;
import android.widget.Toast;
import android.util.Log;
import android.os.SystemProperties;
import com.android.settings.pppoe.*;
import android.app.ProgressDialog;

public class EthernetSettingsAML extends SettingsPreferenceFragment 
	implements Preference.OnPreferenceChangeListener{
    private static final String TAG = "EthernetSettingsAML";
    private static final String KEY_CONF_ETH = "ETHERNET_config";
    private static final String KEY_CONF_IPV6 = "IPV6_config";
    private static final String KEY_CONF_PPPOE = "PPPOE_config";
    private static final String KEY_IPV4_SWITCH = "ipv4_swtich";
    private static final String KEY_IPV6_SWITCH = "ipv6_switch";

    private EthernetEnabler mEthEnabler;
    private EthernetConfigDialog mEthConfigDialog;
    private Ipv6ConfigDialog mIpv6ConfigDialog;
    private Preference mEthConfigPref;
    private Preference mIpv6ConfigPref;
    private SwitchPreference mIpv4_switch;
    private SwitchPreference mIpv6_switch;
    private EthernetManager mEthManager;
    //private WifiManager mWifiManager;
    private PppoeManager mPppoeManager;
    private ConnectivityManager mConnMgr;
    private String mInterfaceName = "eth0";
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        if (preference == mEthConfigPref) {
            int state = mEthManager.getEthState();
            if(state != EthernetManager.ETH_STATE_ENABLED){
                Toast.makeText(getActivity(), R.string.eth_tint, Toast.LENGTH_SHORT).show();
                return false;
            }
            mEthConfigDialog = null;
            mEthConfigDialog = new EthernetConfigDialog(
                    getActivity(),
                    (EthernetManager)getSystemService(Context.ETH_SERVICE));
            mEthConfigDialog.show();
        }
        if (preference == mIpv6ConfigPref) {
            int state6 = mEthManager.getIpv6PersistedState();
            if(state6 == EthernetManager.IPV6_STATE_DISABLED){
                Toast.makeText(getActivity(), R.string.ethv6_tint, Toast.LENGTH_SHORT).show();
                return false;
            }
            mIpv6ConfigDialog = new Ipv6ConfigDialog(getActivity(),(EthernetManager)getSystemService(Context.ETH_SERVICE),(PppoeManager)getSystemService(Context.PPPOE_SERVICE));
            mIpv6ConfigDialog.show();
        }
        return false;
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if(KEY_IPV4_SWITCH.equals(key)) {
            Log.d("EthernetSetting", "-----------status:-----" + (Boolean) objValue);

            if((Boolean) objValue) {
                setEthEnabled(true);
            } else {
                setEthEnabled(false);
            }
            
        }
        if(KEY_IPV6_SWITCH.equals(key)) {
            if((Boolean) objValue){
                Log.d("EthernetSetting", "ipv6 is openning...");
                mEthManager.enableIpv6(true);
                mEthManager.connectIpv6();
            } else {
                Log.d("EthernetSetting", "ipv6 is closed...");
                mEthManager.disconnectIpv6();
                mEthManager.enableIpv6(false);
            }
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.ethernet_settings);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        mEthConfigPref = preferenceScreen.findPreference(KEY_CONF_ETH);
        mIpv6ConfigPref = preferenceScreen.findPreference(KEY_CONF_IPV6);
        mIpv4_switch = (SwitchPreference)findPreference(KEY_IPV4_SWITCH);
        mIpv6_switch =(SwitchPreference)findPreference(KEY_IPV6_SWITCH);
        mEthManager = (EthernetManager)getSystemService(Context.ETH_SERVICE);

        //mWifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        mPppoeManager = (PppoeManager)getSystemService(Context.PPPOE_SERVICE);
        mConnMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        int state = mEthManager.getEthState();
        if(state == EthernetManager.ETH_STATE_ENABLED){
            mIpv4_switch.setChecked(true);
        } else {
            mIpv4_switch.setChecked(false);
        }
        int state6 = mEthManager.getIpv6PersistedState();
        if(state6 == EthernetManager.IPV6_STATE_ENABLED){
            mIpv6_switch.setChecked(true);
        } else {
            mIpv6_switch.setChecked(false);
        }

        mIpv4_switch.setOnPreferenceChangeListener(this);
        mIpv6_switch.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mEthEnabler != null) {
            //mEthEnabler.resume();
        }
        registerForBroadcasts();
    }

    @Override
    public void onStart() {
        super.onStart();
        initToggles();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mEthEnabler != null) {
            mEthEnabler.pause();
        }
        unregisterForBroadcasts();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(Utils.platformHasMbxUiMode()){
	        final Activity activity = getActivity();
	        activity.getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
	        activity.getActionBar().setCustomView(null);
        }
    }
    
    private void initToggles() {
        // For MultiPane preference, the switch is on the left column header.
        // Other layouts unsupported for now.
        
        /*final Activity activity = getActivity();
        Switch actionBarSwitch = new Switch(activity);
        if (activity instanceof PreferenceActivity) {
            PreferenceActivity preferenceActivity = (PreferenceActivity) activity;
            if (Utils.platformHasMbxUiMode()) {
                final int padding = activity.getResources().getDimensionPixelSize(
                        R.dimen.action_bar_switch_padding);
                actionBarSwitch.setPadding(0, 0, padding, 0);
                activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM);
                activity.getActionBar().setCustomView(actionBarSwitch, new ActionBar.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.RIGHT));
            }
            else if (preferenceActivity.onIsHidingHeaders() || !preferenceActivity.onIsMultiPane()) {
                final int padding = activity.getResources().getDimensionPixelSize(
                        R.dimen.action_bar_switch_padding);
                actionBarSwitch.setPadding(0, 0, padding, 0);
                activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM);
                activity.getActionBar().setCustomView(actionBarSwitch, new ActionBar.LayoutParams(
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER_VERTICAL | Gravity.RIGHT));
            }
            mEthEnabler = new EthernetEnabler(
                    (EthernetManager)getSystemService(Context.ETH_SERVICE),
                    actionBarSwitch);
            mEthConfigDialog = new EthernetConfigDialog(
                    getActivity(),
                    (EthernetManager)getSystemService(Context.ETH_SERVICE));
            mEthEnabler.setConfigDialog(mEthConfigDialog);
        }*/
        mEthConfigDialog = new EthernetConfigDialog(
                    getActivity(),
                    (EthernetManager)getSystemService(Context.ETH_SERVICE));
        if (!Utils.platformHasMbxUiMode()) {
            mEthConfigDialog = new EthernetConfigDialog(getActivity(),
                    (EthernetManager) getSystemService(Context.ETH_SERVICE));
        }    
    }

    private void setEthEnabled(final boolean enable) { 
	 /*final ProgressDialog waiting_dialog = new ProgressDialog(getActivity());
        if(mPppoeManager != null && mConnMgr != null){
              NetworkInfo netinfo = mConnMgr.getNetworkInfo(ConnectivityManager.TYPE_PPPOE);
              if(netinfo != null){
                   boolean isAvailable = netinfo.isAvailable();
                   boolean isConnected = netinfo.isConnected();
                   if (isAvailable && isConnected){
                        waiting_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        waiting_dialog.setCancelable(false);
                        waiting_dialog.setTitle("PPPoE disconnecting");
                        waiting_dialog.setMessage("Please Wait..........");
                        waiting_dialog.show();
                        new Thread(new Runnable(){
                        public void run() {
                        mPppoeManager.disconnect(mInterfaceName);
                        try{
                             Log.d("EthernetSetting","SLEEP 3000");
                             Thread.sleep(3000);
                           }catch (InterruptedException ignored) {
                         }
                         Log.d("EthernetSetting", "--Disconnect pppoe");
                        waiting_dialog.cancel();
                      }
                   }).start();
                }
             }
         }*/
        if(enable) {
            /*WifiInfo mWifiinfo = mWifiManager.getConnectionInfo();
            if(mWifiinfo != null) {
                mWifiManager.setWifiEnabled(false);
            }*/
            mEthManager.setEthEnabled(true);
        } else {			
            mEthManager.setEthEnabled(false);
        }
    }

    private void registerForBroadcasts() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PppoeManager.PPPOE_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.ACTION_IP_ADDRESS_CONFLICTED);
        intentFilter.addAction(EthernetManager.IPV6_STATE_CHANGED_ACTION);
        getActivity().registerReceiver(mReceiver, intentFilter);
    }

    private void unregisterForBroadcasts() {
        try {
            getActivity().unregisterReceiver(mReceiver);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
	
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Action is " + intent.getAction());
            if(intent.getAction().equals(PppoeManager.PPPOE_STATE_CHANGED_ACTION)) {
                int state = intent.getIntExtra(PppoeManager.EXTRA_PPPOE_STATE, -1);
                String errormsg = intent.getStringExtra(PppoeManager.EXTRA_PPPOE_ERRMSG);
                Log.d(TAG, "pppoe state is " + state);
                Log.d(TAG, "pppoe error msg is " + errormsg);
                if(state == PppoeManager.EVENT_CONNECT_SUCCESSED) {
                    Toast.makeText(getActivity(), R.string.pppoe_settings_successed, Toast.LENGTH_LONG).show();
                } else if(state == PppoeManager.EVENT_CONNECT_FAILED) {
                    if(errormsg != null) {
                        Toast.makeText(getActivity(), errormsg, Toast.LENGTH_LONG).show();
                    }
                }  
            }
            else if (intent.getAction().equals(ConnectivityManager.ACTION_IP_ADDRESS_CONFLICTED)){
                Log.d(TAG, "IP address conflict ");
                Toast.makeText(context,"IP address conflicted ,please change the IP address",
                    Toast.LENGTH_SHORT).show();

            }
            else if (intent.getAction().equals(EthernetManager.IPV6_STATE_CHANGED_ACTION)){
                int eth_event = intent.getIntExtra(EthernetManager.EXTRA_ETHERNET_STATE,-1);
                if(eth_event == EthernetManager.EVENT_DHCPV6_CONNECT_SUCCESSED){
                     Toast.makeText(context,"DHCPV6 Sussced ",Toast.LENGTH_SHORT).show();
                }else if(eth_event == EthernetManager.EVENT_DHCPV6_CONNECT_FAILED){
                     Toast.makeText(context,"DHCPV6 Failed ",Toast.LENGTH_SHORT).show();
                }else if(eth_event == EthernetManager.EVENT_STATIC6_CONNECT_SUCCESSED){
                     Toast.makeText(context,"STATICV6 Sussced ",Toast.LENGTH_SHORT).show();
                }else if(eth_event == EthernetManager.EVENT_STATIC6_CONNECT_FAILED){
                     Toast.makeText(context,"STATICV6 Failed ",Toast.LENGTH_SHORT).show();
                }
           }
        }
    };
}
