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

import java.util.List;
import java.lang.Integer;

import com.android.settings.R;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.DhcpInfo;
import android.net.ethernet.EthernetManager;
import android.net.ethernet.EthernetDevInfo;
import android.net.pppoe.PppoeDevInfo;
import android.net.pppoe.PppoeManager;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Slog;
import com.android.settings.Utils;
import android.os.SystemProperties;

import java.net.NetworkInterface;
import android.os.IBinder;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.net.InterfaceConfiguration;
import android.net.NetworkUtils;
import java.net.InetAddress;
import android.net.LinkAddress;
import android.net.RouteInfo;
import android.net.DhcpInfoInternal;

public class Ipv6ConfigDialog extends AlertDialog implements
        DialogInterface.OnClickListener, DialogInterface.OnShowListener,
        DialogInterface.OnDismissListener {
    private final String TAG = "Ipv6ConfDialog";
    private static final boolean localLOGV = false;
    public static final String IPV6_DHCP_CONNECT = "ipv6_dhcp_connect"; 

    private View mView;
    private Spinner mDevList;
    private TextView mDevs;
    private RadioButton mConTypeDhcp;
    private RadioButton mConTypeManual;
    private RadioButton mConTypeIPOE;
    private RadioButton mConTypePPPOE;
    private EditText mIpaddr;
    private EditText mDns;
    private EditText mGw;
    private EditText mMask;
    private EditText mUser;
    private EditText mPwd;
    private TextView mUserText;
    private TextView mPwdText;
    private TextView mIpaddrText;
    private TextView mDnsText;
    private TextView mGwText;
    private TextView mMaskText;


    private EthernetLayer mIpv6Layer;
    private EthernetManager mEthManager;
    private PppoeManager mPppoeManager;
    private EthernetDevInfo mEthInfo;
    private boolean mEnablePending;

    private Context mContext;

    public Ipv6ConfigDialog(Context context, EthernetManager ethManager,PppoeManager pppoeManager) {
        super(context);
        mEthManager = ethManager;
        mPppoeManager = pppoeManager;
        mContext = context;
        buildDialogContent(context);
        setOnShowListener(this);
        setOnDismissListener(this);
        enableAfterConfig();
    }

    public void onShow(DialogInterface dialog) {
        if (localLOGV) Slog.d(TAG, "onShow");
        // soft keyboard pops up on the disabled EditText. Hide it.
        InputMethodManager imm = (InputMethodManager)mContext.getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    public void onDismiss(DialogInterface dialog) {
        if (localLOGV) Slog.d(TAG, "onDismiss");
    }

    private static String getAddress(int addr) {
        return NetworkUtils.intToInetAddress(addr).getHostAddress();
    }

    public int buildDialogContent(Context context) {
        this.setTitle(R.string.ipv6_config_title);
        this.setView(mView = getLayoutInflater().inflate(R.layout.ipv6_configure, null));
        mDevs = (TextView) mView.findViewById(R.id.eth_dev_list_text);
        mDevList = (Spinner) mView.findViewById(R.id.eth_dev_spinner);
        mConTypeDhcp = (RadioButton) mView.findViewById(R.id.dhcp_radio);
        mConTypeManual = (RadioButton) mView.findViewById(R.id.manual_radio);
        mConTypeIPOE = (RadioButton) mView.findViewById(R.id.ipoe_radio);
        mConTypePPPOE = (RadioButton) mView.findViewById(R.id.pppoe_radio);
        mIpaddrText = (TextView)mView.findViewById(R.id.ipaddr_text);
        mMaskText = (TextView)mView.findViewById(R.id.netmask_text);
        mDnsText = (TextView)mView.findViewById(R.id.dns_text);
        mGwText = (TextView)mView.findViewById(R.id.gw_text);
        mIpaddr = (EditText)mView.findViewById(R.id.ipaddr_edit);
        mMask = (EditText)mView.findViewById(R.id.netmask_edit);
        mDns = (EditText)mView.findViewById(R.id.eth_dns_edit);
        mGw = (EditText)mView.findViewById(R.id.eth_gw_edit);
        mUserText = (TextView)mView.findViewById(R.id.user_text);
        mPwdText = (TextView)mView.findViewById(R.id.password_text);
        mUser = (EditText)mView.findViewById(R.id.eth_user_edit);
        mPwd = (EditText)mView.findViewById(R.id.eth_password_edit);

        if(mEthManager.getEthernetMode6() != null && mEthManager.getEthernetMode6().equals(EthernetDevInfo.ETH_CONN_MODE_MANUAL)){
            mConTypeDhcp.setChecked(false);
            mConTypeManual.setChecked(true);
            mConTypeIPOE.setChecked(false);
            mConTypePPPOE.setChecked(false);
            mIpaddr.setVisibility(View.GONE);
            mDns.setVisibility(View.GONE);
            mGw.setVisibility(View.GONE);
            mMask.setVisibility(View.GONE);
            mUserText.setVisibility(View.GONE);
            mPwdText.setVisibility(View.GONE);
            mUser.setVisibility(View.GONE);
            mPwd.setVisibility(View.GONE);
            mIpaddrText.setVisibility(View.GONE);
            mMaskText.setVisibility(View.GONE);
            mDnsText.setVisibility(View.GONE);
            mGwText.setVisibility(View.GONE);
        }else if(mEthManager.getEthernetMode6() != null && mEthManager.getEthernetMode6().equals(EthernetDevInfo.ETH_CONN_MODE_DHCP)){
            mConTypeDhcp.setChecked(true);
            mConTypeManual.setChecked(false);
            mConTypeIPOE.setChecked(false);
            mConTypePPPOE.setChecked(false);
            mIpaddr.setEnabled(false);
            mMask.setEnabled(false);
            mDns.setEnabled(false);
            mGw.setEnabled(false);
            mIpaddr.setVisibility(View.GONE);
            mDns.setVisibility(View.GONE);
            mGw.setVisibility(View.GONE);
            mMask.setVisibility(View.GONE);
            mUserText.setVisibility(View.GONE);
            mPwdText.setVisibility(View.GONE);
            mUser.setVisibility(View.GONE);
            mPwd.setVisibility(View.GONE);
            mIpaddrText.setVisibility(View.GONE);
            mMaskText.setVisibility(View.GONE);
            mDnsText.setVisibility(View.GONE);
            mGwText.setVisibility(View.GONE);
        }else if(mEthManager.getEthernetMode6() != null && mEthManager.getEthernetMode6().equals(EthernetDevInfo.ETH_CONN_MODE_DHCP_AUTH)){
            mConTypeDhcp.setChecked(false);
            mConTypeManual.setChecked(false);
            mConTypeIPOE.setChecked(true);
            mConTypePPPOE.setChecked(false);
            mIpaddr.setEnabled(false);
            mMask.setEnabled(false);
            mDns.setEnabled(false);
            mGw.setEnabled(false);
            mIpaddr.setVisibility(View.GONE);
            mDns.setVisibility(View.GONE);
            mGw.setVisibility(View.GONE);
            mMask.setVisibility(View.GONE);
            String mUsrStr = mEthManager.getV6AuthUsername();
            String mPwdStr = mEthManager.getV6AuthPassword();
            if((mUsrStr != null) && (mPwdStr != null)) {
                   mUser.setText(mUsrStr);
                   mPwd.setText(mPwdStr);
            } else {
                   mUser.setText(null);
                   mPwd.setText(null);
            }
            mUserText.setVisibility(View.VISIBLE);
            mPwdText.setVisibility(View.VISIBLE);
            mUser.setVisibility(View.VISIBLE);
            mPwd.setVisibility(View.VISIBLE);
            mUser.setEnabled(true);
            mPwd.setEnabled(true);
            mIpaddrText.setVisibility(View.GONE);
            mMaskText.setVisibility(View.GONE);
            mDnsText.setVisibility(View.GONE);
            mGwText.setVisibility(View.GONE);
        }else if(mEthManager.getEthernetMode6() != null && mEthManager.getEthernetMode6().equals(EthernetDevInfo.ETH_CONN_MODE_PPPOE)){
            mConTypeDhcp.setChecked(false);
            mConTypeManual.setChecked(false);
            mConTypeIPOE.setChecked(false);
            mConTypePPPOE.setChecked(true);
            mIpaddr.setEnabled(false);
            mMask.setEnabled(false);
            mDns.setEnabled(false);
            mGw.setEnabled(false);
            mIpaddr.setVisibility(View.GONE);
            mDns.setVisibility(View.GONE);
            mGw.setVisibility(View.GONE);
            mMask.setVisibility(View.GONE);
            mUserText.setVisibility(View.VISIBLE);
            mPwdText.setVisibility(View.VISIBLE);
            mUser.setVisibility(View.VISIBLE);
            mPwd.setVisibility(View.VISIBLE);
            mUser.setEnabled(true);
            mPwd.setEnabled(true);
            mIpaddrText.setVisibility(View.GONE);
            mMaskText.setVisibility(View.GONE);
            mDnsText.setVisibility(View.GONE);
            mGwText.setVisibility(View.GONE);
            PppoeDevInfo info = mPppoeManager.getSavedPppoeConfig();
            String mUsrStr = info.getPassword();
            String mPwdStr = info.getAccount();
            if((mUsrStr != null) && (mPwdStr != null)) {
                   mUser.setText(mUsrStr);
                   mPwd.setText(mPwdStr);
            } else {
                   mUser.setText(null);
                   mPwd.setText(null);
            }
        }
		
		if(mEthManager.getIpv6DatabaseAddress() != null)
		     mIpaddr.setText(mEthManager.getIpv6DatabaseAddress());
		if(mEthManager.getIpv6DatabasePrefixlength() != 0)
		     mMask.setText(mEthManager.getIpv6DatabasePrefixlength() + "");
		if(mEthManager.getIpv6DatabaseDns1() != null)
		     mDns.setText(mEthManager.getIpv6DatabaseDns1());
		if(mEthManager.getIpv6DatabaseGateway() != null)
		     mGw.setText(mEthManager.getIpv6DatabaseGateway());


        mConTypeManual.setOnClickListener(new RadioButton.OnClickListener() {
            public void onClick(View v) {
                mIpaddr.setEnabled(true);
                mDns.setEnabled(true);
                mGw.setEnabled(true);
                mMask.setEnabled(true);
                mUserText.setVisibility(View.GONE);
                mPwdText.setVisibility(View.GONE);
                mUser.setVisibility(View.GONE);
                mPwd.setVisibility(View.GONE);
                mIpaddr.setVisibility(View.VISIBLE);
                mDns.setVisibility(View.VISIBLE);
                mGw.setVisibility(View.VISIBLE);
                mMask.setVisibility(View.VISIBLE);
                mIpaddrText.setVisibility(View.VISIBLE);
                mMaskText.setVisibility(View.VISIBLE);
                mDnsText.setVisibility(View.VISIBLE);
                mGwText.setVisibility(View.VISIBLE);
            }
        });

        mConTypeDhcp.setOnClickListener(new RadioButton.OnClickListener() {
            public void onClick(View v) {
                mIpaddr.setVisibility(View.GONE);
                mDns.setVisibility(View.GONE);
                mGw.setVisibility(View.GONE);
                mMask.setVisibility(View.GONE);
                mUserText.setVisibility(View.GONE);
                mPwdText.setVisibility(View.GONE);
                mUser.setVisibility(View.GONE);
                mPwd.setVisibility(View.GONE);
                mIpaddrText.setVisibility(View.GONE);
                mMaskText.setVisibility(View.GONE);
                mDnsText.setVisibility(View.GONE);
                mGwText.setVisibility(View.GONE);
            }
        });

        mConTypeIPOE.setOnClickListener(new RadioButton.OnClickListener() {
            public void onClick(View v) {
                mIpaddr.setVisibility(View.GONE);
                mDns.setVisibility(View.GONE);
                mGw.setVisibility(View.GONE);
                mMask.setVisibility(View.GONE);
                mUserText.setVisibility(View.VISIBLE);
                mPwdText.setVisibility(View.VISIBLE);
                mUser.setVisibility(View.VISIBLE);
                mPwd.setVisibility(View.VISIBLE);
                mUser.setEnabled(true);
                mPwd.setEnabled(true);
                mIpaddrText.setVisibility(View.GONE);
                mMaskText.setVisibility(View.GONE);
                mDnsText.setVisibility(View.GONE);
                mGwText.setVisibility(View.GONE);
                String mUsrStr = mEthManager.getV6AuthUsername();
                String mPwdStr = mEthManager.getV6AuthPassword();
                if((mUsrStr != null) && (mPwdStr != null)) {
                    mUser.setText(mUsrStr);
                    mPwd.setText(mPwdStr);
                } else {
                    mUser.setText(null);
                    mPwd.setText(null);
                }
            }
        });

        mConTypePPPOE.setOnClickListener(new RadioButton.OnClickListener() {
            public void onClick(View v) {
                mIpaddr.setVisibility(View.GONE);
                mDns.setVisibility(View.GONE);
                mGw.setVisibility(View.GONE);
                mMask.setVisibility(View.GONE);
                mUserText.setVisibility(View.VISIBLE);
                mPwdText.setVisibility(View.VISIBLE);
                mUser.setVisibility(View.VISIBLE);
                mPwd.setVisibility(View.VISIBLE);
                mUser.setEnabled(true);
                mPwd.setEnabled(true);
                mIpaddrText.setVisibility(View.GONE);
                mMaskText.setVisibility(View.GONE);
                mDnsText.setVisibility(View.GONE);
                mGwText.setVisibility(View.GONE);
                PppoeDevInfo info = mPppoeManager.getSavedPppoeConfig();
                String mUsrStr = info.getPassword();
                String mPwdStr = info.getAccount();
                if((mUsrStr != null) && (mPwdStr != null)) {
                     mUser.setText(mUsrStr);
                     mPwd.setText(mPwdStr);
                } else {
                     mUser.setText(null);
                     mPwd.setText(null);
                }
            }
        });


        this.setInverseBackgroundForced(true);
        this.setButton(BUTTON_POSITIVE, context.getText(R.string.menu_save), this);
        this.setButton(BUTTON_NEGATIVE, context.getText(R.string.menu_cancel), this);
        String[] Devs = mEthManager.getDeviceNameList();
        updateDevNameList(Devs);
        if (Devs != null) {
            if (mEthManager.isEthConfigured()) {
                String propties = Utils.getEtherProperties(mContext);
                Slog.d(TAG, "Properties: " + propties);
                mEthInfo = mEthManager.getSavedEthConfig();
                for (int i = 0 ; i < Devs.length; i++) {
                    if (Devs[i].equals(mEthInfo.getIfName())) {
                        mDevList.setSelection(i);
                        break;
                    }
                }
            }
        }
        return 0;
    }

    private void handle_saveconf() {
        String selected = null;
        if (mDevList.getSelectedItem() != null)
            selected = mDevList.getSelectedItem().toString();
        if (selected == null || selected.isEmpty())
            return;
        EthernetDevInfo info = new EthernetDevInfo();
        info.setIfName(selected);
        mEthManager.disconnectIpv6();
        mPppoeManager.disconnect("eth0");  
        if (localLOGV)
            Slog.v(TAG, "Config device for " + selected);
        if (mConTypeDhcp.isChecked()) {
            mEthManager.setEthernetMode6(EthernetDevInfo.ETH_CONN_MODE_DHCP);
            mEthManager.connectIpv6();
            Slog.v(TAG, "IPV6 ConnectType:DHCP");
        }else if(mConTypeIPOE.isChecked()){
            Slog.v(TAG, "IPV6 ConnectType:IPOE");
			if(mUser.getText().toString() != null && mPwd.getText().toString()!= null){
                mEthManager.setEthernetMode6(EthernetDevInfo.ETH_CONN_MODE_DHCP_AUTH);
                mEthManager.setV6AuthState(mUser.getText().toString(), mPwd.getText().toString(),"IPTVAAA");
                mEthManager.connectIpv6();
			}
        }else if(mConTypePPPOE.isChecked()){
            Slog.v(TAG, "IPV6 ConnectType:PPPOE");
			if(mUser.getText().toString() != null && mPwd.getText().toString()!= null){
                final PppoeDevInfo pinfo = new PppoeDevInfo();
			    pinfo.setIfName("eth0");
			    pinfo.setAccount(mUser.getText().toString());
			    pinfo.setPassword(mPwd.getText().toString());
			    pinfo.setDialMode(PppoeDevInfo.PPPOE_DIAL_MODE_AUTO);
			    pinfo.setIpType(PppoeDevInfo.PPPOE_IP_V6);
                mEthManager.setEthernetMode6(EthernetDevInfo.ETH_CONN_MODE_PPPOE);
			    mPppoeManager.UpdatePppoeDevInfo(pinfo);
			    mPppoeManager.connect(mUser.getText().toString(), mPwd.getText().toString(),"eth0");
			 }
        }else if(mConTypeManual.isChecked()){
            //close manual on ipv6 temporary
            Slog.i(TAG,"ipv6 mode manual");
            Slog.i(TAG,"Ipaddr : " + mIpaddr.getText().toString());
            Slog.i(TAG,"NetMask: " + mMask.getText().toString());
            Slog.i(TAG,"Gateway : " + mGw.getText().toString());
            Slog.i(TAG,"Dns : " + mDns.getText().toString());
            mEthManager.setEthernetMode6(EthernetDevInfo.ETH_CONN_MODE_MANUAL);
            mEthManager.setIpv6DatabaseInfo(mIpaddr.getText().toString(), Integer.parseInt(mMask.getText().toString()), mGw.getText().toString(), mDns.getText().toString(),mDns.getText().toString());
            mEthManager.connectIpv6();
        }
    }

    // need to modify isIpAddress methord on ipv6
    private boolean isIpAddress(String value) {
        int start = 0;
        int end = value.indexOf('.');
        int numBlocks = 0;

        while (start < value.length()) {
            if (end == -1) {
                end = value.length();
            }

            try {
                int block = Integer.parseInt(value.substring(start, end));
                if ((block > 255) || (block < 0)) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }

            numBlocks++;

            start = end + 1;
            end = value.indexOf('.', start);
        }
        return numBlocks == 4;
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                handle_saveconf();
                break;
            case BUTTON_NEGATIVE:
                //Don't need to do anything
                break;
            default:
                break;
        }
    }

    public void updateDevNameList(String[] DevList) {
        if (DevList == null) {
            DevList = new String[] {};
        }
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
                getContext(), android.R.layout.simple_spinner_item, DevList);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        mDevList.setAdapter(adapter);
    }

    public void enableAfterConfig() {
        mEnablePending = true;
    }
}
