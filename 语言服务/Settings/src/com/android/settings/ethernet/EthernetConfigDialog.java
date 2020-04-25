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

import com.android.settings.R;
import com.android.settings.pppoe.*;
import android.util.Log;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.DhcpInfo;
import android.net.ethernet.EthernetManager;
import android.net.ethernet.EthernetDevInfo;
import android.net.pppoe.PppoeDevInfo;
import android.net.ProxyProperties;
import android.net.wifi.WifiConfiguration.ProxySettings;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.TextUtils;
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
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.ProgressDialog;

public class EthernetConfigDialog extends AlertDialog implements
        DialogInterface.OnClickListener, DialogInterface.OnShowListener,
        DialogInterface.OnDismissListener {
    private final String TAG = "EthConfDialog";
    private static final boolean localLOGV = true;

    private static final boolean ENABLE_PROXY = true;
    /* These values come from "wifi_proxy_settings" resource array */
    public static final int PROXY_NONE = 0;
    public static final int PROXY_STATIC = 1;

    /* These values come from "network_ip_settings" resource array */
    private static final int DHCP = 0;
    private static final int STATIC_IP = 1;

    // Matches blank input, ips, and domain names
    private static final String HOSTNAME_REGEXP =
            "^$|^[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*(\\.[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*)*$";
    private static final Pattern HOSTNAME_PATTERN;
    private static final String EXCLLIST_REGEXP =
            "$|^(.?[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*(\\.[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*)*)+" +
            "(,(.?[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*(\\.[a-zA-Z0-9]+(\\-[a-zA-Z0-9]+)*)*))*$";
    private static final Pattern EXCLLIST_PATTERN;
    static {
        HOSTNAME_PATTERN = Pattern.compile(HOSTNAME_REGEXP);
        EXCLLIST_PATTERN = Pattern.compile(EXCLLIST_REGEXP);
    }

    private View mView;
    private Spinner mDevList;
    private TextView mDevs;
    private RadioButton mConTypeDhcp;
    private RadioButton mConTypeManual;
    private RadioButton mConTypeAuthDhcp;
    private RadioButton mConTypePPPoE;
    private RadioButton mAuthDhcpEnable;
    private RadioButton mAuthDhcpDisable;
    private TextView mIpaddrText;
    private TextView mDnsText;
    private TextView mGwText;
    private TextView mMaskText;
    private TextView mUserText;
    private TextView mPwdText;
    private EditText mIpaddr;
    private EditText mDns;
    private EditText mGw;
    private EditText mMask;
    private EditText mUser;
    private EditText mPwd;
    //private CheckBox mAutoDial;

    // Indicates if we are in the process of setting up values and should not validate them yet.
    private boolean mSettingUpValues;
    private Spinner mProxySettingsSpinner;
    private TextView mProxyHostView;
    private TextView mProxyPortView;
    private TextView mProxyExclusionListView;
    private final TextWatcher textWatcher = new TextWatcherImpl();

    private EthernetLayer mEthLayer;
    private EthernetManager mEthManager;
    private EthernetDevInfo mEthInfo;
    private boolean mEnablePending;

    private Context mContext;
    
    private PPPoEConfig mPPPoEConfig;
    private Handler mHandler;
    private String mInterfaceName = "eth0";

    public EthernetConfigDialog(Context context, EthernetManager ethManager) {
        super(context);
        mEthManager = ethManager;
        mEthLayer = new EthernetLayer(this, ethManager);
        mContext = context;
        mPPPoEConfig = new PPPoEConfig(context);
        buildDialogContent(context);
        setOnShowListener(this);
        setOnDismissListener(this);
        enableAfterConfig();
    }

    public void onShow(DialogInterface dialog) {
        if (localLOGV) Slog.d(TAG, "onShow");
        mEthLayer.resume();
        // soft keyboard pops up on the disabled EditText. Hide it.
        if (getCurrentFocus() != null && getCurrentFocus().getWindowToken() != null) {
            InputMethodManager imm = (InputMethodManager)mContext.getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
    }

    public void onDismiss(DialogInterface dialog) {
        if (localLOGV) Slog.d(TAG, "onDismiss");
        mEthLayer.pause();
    }

    private static String getAddress(int addr) {
        return NetworkUtils.intToInetAddress(addr).getHostAddress();
    }


    /* proxy */
    private void showProxyFields() {
        if (mProxySettingsSpinner.getSelectedItemPosition() == PROXY_STATIC) {
            mView.findViewById(R.id.proxy_fields).setVisibility(View.VISIBLE);
        } else {
            mView.findViewById(R.id.proxy_fields).setVisibility(View.GONE);
        }
    }

    private void enableSubmitIfAppropriate() {
        //setPositiveButtonEnabled(isProxyFieldsValid() && isIpFieldsValid());
        isProxyFieldsValid();
        //skip disabling PositveButton for now
    }

    private boolean isProxyFieldsValid() {
        if (mProxySettingsSpinner.getSelectedItemPosition() == PROXY_STATIC) {
            return validateProxyFields();
        }
        return true;
    }

    public static boolean isValidIpAddress(String ipAddress, boolean allowEmptyValue) {
        if (ipAddress == null || ipAddress.length() == 0) {
            return allowEmptyValue;
        }

        try {
            InetAddress.getByName(ipAddress);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Validates string with proxy exclusion list.
     *
     * @param exclList string to validate.
     * @return resource id of error message string or 0 if valid.
     */
    public static int validateProxyExclusionList(String exclList) {
        Matcher listMatch = EXCLLIST_PATTERN.matcher(exclList);
        return !listMatch.matches() ? R.string.proxy_error_invalid_exclusion_list : 0;
    }

    private boolean validateProxyFields() {
        if (!ENABLE_PROXY) {
            return true;
        }

        final Context context = getContext();
        boolean errors = false;

        if (isValidIpAddress(mProxyHostView.getText().toString(), false)) {
            mProxyHostView.setError(null);
        } else {
            mProxyHostView.setError(
                    context.getString(R.string.wifi_ip_settings_invalid_ip_address));
            errors = true;
        }

        int port = -1;
        try {
            port = Integer.parseInt(mProxyPortView.getText().toString());
            mProxyPortView.setError(null);
        } catch (NumberFormatException e) {
            // Intentionally left blank
        }
        if (port < 0) {
            mProxyPortView.setError(context.getString(R.string.proxy_error_invalid_port));
            errors = true;
        }

        final String exclusionList = mProxyExclusionListView.getText().toString();
        final int listResult = validateProxyExclusionList(exclusionList);
        if (listResult == 0) {
            mProxyExclusionListView.setError(null);
        } else {
            mProxyExclusionListView.setError(context.getString(listResult));
            errors = true;
        }

        return !errors;
    }

    private void setPositiveButtonEnabled(boolean enabled) {
        getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(enabled);
    }

    private class TextWatcherImpl implements TextWatcher {
        @Override
        public void afterTextChanged(Editable s) {
            // Do not validate fields while values are being setted up.
            if (!mSettingUpValues) {
                enableSubmitIfAppropriate();
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    }

    private void setProxyPropertiesFromEdits(EthernetDevInfo info) {
        final ProxySettings proxySettings =
                ENABLE_PROXY && mProxySettingsSpinner.getSelectedItemPosition() == PROXY_STATIC
                        ? ProxySettings.STATIC : ProxySettings.NONE;

        if (proxySettings == ProxySettings.STATIC) {
            String port = mProxyPortView.getText().toString();
            if (TextUtils.isEmpty(port))
                port = "0";
            try {
                info.setProxy(
                        mProxyHostView.getText().toString(),
                        Integer.parseInt(port),
                        mProxyExclusionListView.getText().toString());
            } catch (IllegalArgumentException e) {
                // Should not happen if validations are done right
                throw new RuntimeException(e);
            }
        } else {
            info.setProxy(null, 0, null);
        }
    }

    private void buildProxyContent() {
        mProxySettingsSpinner = (Spinner) mView.findViewById(R.id.proxy_settings);
        mProxySettingsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                showProxyFields();
                enableSubmitIfAppropriate();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        mProxySettingsSpinner.setVisibility(View.VISIBLE);

        mProxyHostView = (TextView) mView.findViewById(R.id.proxy_hostname);
        mProxyHostView.addTextChangedListener(textWatcher);

        mProxyPortView = (TextView) mView.findViewById(R.id.proxy_port);
        mProxyPortView.addTextChangedListener(textWatcher);

        mProxyExclusionListView = (TextView) mView.findViewById(R.id.proxy_exclusionlist);
        mProxyExclusionListView.addTextChangedListener(textWatcher);

    }
    /* /proxy */

    public int buildDialogContent(Context context) {
        mSettingUpValues = true;
        this.setTitle(R.string.eth_config_title);
        this.setView(mView = getLayoutInflater().inflate(R.layout.eth_configure, null));
        mDevs = (TextView) mView.findViewById(R.id.eth_dev_list_text);
        mDevList = (Spinner) mView.findViewById(R.id.eth_dev_spinner);
        mConTypeDhcp = (RadioButton) mView.findViewById(R.id.dhcp_radio);
        mConTypeManual = (RadioButton) mView.findViewById(R.id.manual_radio);
        mConTypeAuthDhcp = (RadioButton) mView.findViewById(R.id.auth_dhcp_radio);
        mConTypePPPoE = (RadioButton) mView.findViewById(R.id.pppoe_radio);
        mAuthDhcpEnable = (RadioButton) mView.findViewById(R.id.authdhcp_enable);
        mAuthDhcpDisable = (RadioButton) mView.findViewById(R.id.authdhcp_disable);
        mIpaddrText = (TextView)mView.findViewById(R.id.ipaddr_text);
        mMaskText = (TextView)mView.findViewById(R.id.netmask_text);
        mDnsText = (TextView)mView.findViewById(R.id.dns_text);
        mGwText = (TextView)mView.findViewById(R.id.gw_text);
        mUserText = (TextView)mView.findViewById(R.id.user_text);
        mPwdText = (TextView)mView.findViewById(R.id.password_text);
        mIpaddr = (EditText)mView.findViewById(R.id.ipaddr_edit);
        mMask = (EditText)mView.findViewById(R.id.netmask_edit);
        mDns = (EditText)mView.findViewById(R.id.eth_dns_edit);
        mGw = (EditText)mView.findViewById(R.id.eth_gw_edit);
        mUser = (EditText)mView.findViewById(R.id.eth_user_edit);
        mPwd = (EditText)mView.findViewById(R.id.eth_password_edit);
        //mAutoDial = (CheckBox)mView.findViewById(R.id.auto_dial_checkbox);
        
        mEthInfo = mEthManager.getSavedEthConfig();

        if(mEthInfo.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_PPPOE)) {
            mConTypeDhcp.setChecked(false);
            mConTypeManual.setChecked(false);
            mConTypeAuthDhcp.setChecked(false);
            mConTypePPPoE.setChecked(true);
        } else if(mEthInfo.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_MANUAL)) {
            mConTypeDhcp.setChecked(false);
            mConTypeManual.setChecked(true);
            mConTypeAuthDhcp.setChecked(false);
            mConTypePPPoE.setChecked(false);
        } else if(mEthInfo.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_DHCP_AUTH)) {
            mConTypeDhcp.setChecked(false);
            mConTypeManual.setChecked(false);
            mConTypeAuthDhcp.setChecked(true);
            mConTypePPPoE.setChecked(false);
        } else {
            mConTypeDhcp.setChecked(true);
            mConTypeManual.setChecked(false);
            mConTypeAuthDhcp.setChecked(false); 
            mConTypePPPoE.setChecked(false);
        }

        mIpaddrText.setVisibility(View.GONE);
        mMaskText.setVisibility(View.GONE);
        mDnsText.setVisibility(View.GONE);
        mGwText.setVisibility(View.GONE);
        mUserText.setVisibility(View.GONE);
        mPwdText.setVisibility(View.GONE);
        mIpaddr.setVisibility(View.GONE);
        mMask.setVisibility(View.GONE);
        mDns.setVisibility(View.GONE);
        mGw.setVisibility(View.GONE);
        mUser.setVisibility(View.GONE);
        mPwd.setVisibility(View.GONE);
        //mAutoDial.setVisibility(View.GONE);
        mConTypeManual.setOnClickListener(new RadioButton.OnClickListener() {
            public void onClick(View v) {
                mView.findViewById(R.id.eth_static_fields).setVisibility(View.VISIBLE);
                mEthInfo = mEthManager.getSavedEthConfig();
                mIpaddrText.setVisibility(View.VISIBLE);
                mMaskText.setVisibility(View.VISIBLE);
                mDnsText.setVisibility(View.VISIBLE);
                mGwText.setVisibility(View.VISIBLE);
                mUserText.setVisibility(View.GONE);
                mPwdText.setVisibility(View.GONE);
                mIpaddr.setVisibility(View.VISIBLE);
                mMask.setVisibility(View.VISIBLE);
                mDns.setVisibility(View.VISIBLE);
                mGw.setVisibility(View.VISIBLE);
                mUser.setVisibility(View.GONE);
                mPwd.setVisibility(View.GONE);
                mAuthDhcpEnable.setVisibility(View.GONE);
                mAuthDhcpDisable.setVisibility(View.GONE);
                //mAutoDial.setVisibility(View.GONE);
                mIpaddr.setEnabled(true);
                mDns.setEnabled(true);
                mGw.setEnabled(true);
                mMask.setEnabled(true);
                if(mEthInfo != null) {
                    mIpaddr.setText(mEthInfo.getIpAddress());
                    mMask.setText(mEthInfo.getNetMask());
                    mGw.setText(mEthInfo.getRouteAddr());
                    mDns.setText(mEthInfo.getDnsAddr());
                }
            }
        });

        mConTypeDhcp.setOnClickListener(new RadioButton.OnClickListener() {
            public void onClick(View v) {
                mView.findViewById(R.id.eth_static_fields).setVisibility(View.GONE);
                mIpaddrText.setVisibility(View.GONE);
                mMaskText.setVisibility(View.GONE);
                mDnsText.setVisibility(View.GONE);
                mGwText.setVisibility(View.GONE);
                mUserText.setVisibility(View.GONE);
                mPwdText.setVisibility(View.GONE);
                mIpaddr.setVisibility(View.GONE);
                mMask.setVisibility(View.GONE);
                mDns.setVisibility(View.GONE);
                mGw.setVisibility(View.GONE);
                mUser.setVisibility(View.GONE);
                mPwd.setVisibility(View.GONE);
                mAuthDhcpEnable.setVisibility(View.GONE);
                mAuthDhcpDisable.setVisibility(View.GONE);
                //mAutoDial.setVisibility(View.GONE);
            }
        });

        mConTypeAuthDhcp.setOnClickListener(new RadioButton.OnClickListener() {
            public void onClick(View v) {
                mView.findViewById(R.id.eth_static_fields).setVisibility(View.VISIBLE);
                mIpaddrText.setVisibility(View.GONE);
                mMaskText.setVisibility(View.GONE);
                mDnsText.setVisibility(View.GONE);
                mGwText.setVisibility(View.GONE);
                mUserText.setVisibility(View.VISIBLE);
                mPwdText.setVisibility(View.VISIBLE);
                mIpaddr.setVisibility(View.GONE);
                mMask.setVisibility(View.GONE);
                mDns.setVisibility(View.GONE);
                mGw.setVisibility(View.GONE);
                mAuthDhcpEnable.setVisibility(View.VISIBLE);
                mAuthDhcpDisable.setVisibility(View.VISIBLE);
                mUser.setVisibility(View.VISIBLE);
                mPwd.setVisibility(View.VISIBLE);
                //mAutoDial.setVisibility(View.GONE);
                mUser.setEnabled(true);
                mPwd.setEnabled(true);
                if(mEthManager.getAuthState()) {
                    mAuthDhcpEnable.setChecked(true);
                } else {
                    mAuthDhcpDisable.setChecked(true);
                }
            }
        });

        mAuthDhcpEnable.setOnClickListener(new RadioButton.OnClickListener() {
            public void onClick(View v) {
                mEthManager.setAuthState(mUser.getText().toString(), mPwd.getText().toString(), 
                        SystemProperties.get("dhcp.auth.vendorid", "CTCIPTVDHCPAAA"), true);
            }
        });

        mAuthDhcpDisable.setOnClickListener(new RadioButton.OnClickListener() {
            public void onClick(View v) {
                mEthManager.setAuthState(mUser.getText().toString(), mPwd.getText().toString(), 
                        SystemProperties.get("dhcp.auth.vendorid", "CTCIPTVDHCPAAA"), false);
            }
        });
        
        mConTypePPPoE.setOnClickListener(new RadioButton.OnClickListener() {
            public void onClick(View v) {
                mView.findViewById(R.id.eth_static_fields).setVisibility(View.VISIBLE);
                mIpaddrText.setVisibility(View.GONE);
                mMaskText.setVisibility(View.GONE);
                mDnsText.setVisibility(View.GONE);
                mGwText.setVisibility(View.GONE);
                mUserText.setVisibility(View.VISIBLE);
                mPwdText.setVisibility(View.VISIBLE);
                mIpaddr.setVisibility(View.GONE);
                mMask.setVisibility(View.GONE);
                mDns.setVisibility(View.GONE);
                mGw.setVisibility(View.GONE);
                mUser.setVisibility(View.VISIBLE);
                mPwd.setVisibility(View.VISIBLE);
                //mAutoDial.setVisibility(View.VISIBLE);
                mUser.setEnabled(true);
                mPwd.setEnabled(true);
                
                String mUsrStr = mPPPoEConfig.getUserName(mContext);
                String mPwdStr = mPPPoEConfig.getPassword(mContext);
                //boolean isChecked = mPPPoEConfig.getAutoDialFlag(mContext);
                //mAutoDial.setChecked(isChecked);
                if((mUsrStr != null) && (mPwdStr != null)) {
                    mUser.setText(mUsrStr);
                    mPwd.setText(mPwdStr);
                } else {
                    mUser.setText(null);
                    mPwd.setText(null);
                }
            }
        });

        buildProxyContent();

        this.setInverseBackgroundForced(true);
        if(mPPPoEConfig.isConnected(mContext, ConnectivityManager.TYPE_PPPOE)) {
            this.setButton(BUTTON_NEUTRAL, context.getText(R.string.menu_disconnect), this);
        }
        this.setButton(BUTTON_POSITIVE, context.getText(R.string.menu_save), this);
        this.setButton(BUTTON_NEGATIVE, context.getText(R.string.menu_cancel), this);
        String[] Devs = mEthManager.getDeviceNameList();
        updateDevNameList(Devs);
        if (Devs != null) {
            if (mEthManager.isEthConfigured()) {
                String propties = Utils.getEtherProperties(mContext);
                Slog.d(TAG, "Properties: " + propties);

                for (int i = 0 ; i < Devs.length; i++) {
                    if (Devs[i].equals(mEthInfo.getIfName())) {
                        mDevList.setSelection(i);
                        break;
                    }
                }
                if(mEthInfo.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_PPPOE)) {
                    mView.findViewById(R.id.eth_static_fields).setVisibility(View.VISIBLE);
                    mUser.setText(mPPPoEConfig.getUserName(mContext));
                    mPwd.setText(mPPPoEConfig.getPassword(mContext));

                    mUserText.setVisibility(View.VISIBLE);
                    mPwdText.setVisibility(View.VISIBLE);

                    mUser.setVisibility(View.VISIBLE);
                    mPwd.setVisibility(View.VISIBLE);

                    mUser.setEnabled(true);
                    mPwd.setEnabled(true);
                } else if(mEthInfo.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_MANUAL)) {
                    mView.findViewById(R.id.eth_static_fields).setVisibility(View.VISIBLE);
                    mIpaddr.setText(mEthInfo.getIpAddress());
                    mMask.setText(mEthInfo.getNetMask());
                    mGw.setText(mEthInfo.getRouteAddr());
                    mDns.setText(mEthInfo.getDnsAddr());

                    mIpaddrText.setVisibility(View.VISIBLE);
                    mMaskText.setVisibility(View.VISIBLE);
                    mDnsText.setVisibility(View.VISIBLE);
                    mGwText.setVisibility(View.VISIBLE);

                    mIpaddr.setVisibility(View.VISIBLE);
                    mMask.setVisibility(View.VISIBLE);
                    mDns.setVisibility(View.VISIBLE);
                    mGw.setVisibility(View.VISIBLE);
                    mView.findViewById(R.id.eth_static_fields).setVisibility(View.VISIBLE);
                    mIpaddr.setEnabled(true);
                    mDns.setEnabled(true);
                    mGw.setEnabled(true);
                    mMask.setEnabled(true);
                } else if(mEthInfo.getConnectMode().equals(EthernetDevInfo.ETH_CONN_MODE_DHCP_AUTH)) {
                    mView.findViewById(R.id.eth_static_fields).setVisibility(View.VISIBLE);
                    mUser.setText(mEthManager.getAuthUsername());
                    mPwd.setText(mEthManager.getAuthPassword());
                    mConTypeDhcp.setChecked(false);
                    mConTypeManual.setChecked(false);
                    mConTypeAuthDhcp.setChecked(true);

                    mAuthDhcpEnable.setVisibility(View.VISIBLE);
                    mAuthDhcpDisable.setVisibility(View.VISIBLE);

                    mUserText.setVisibility(View.VISIBLE);
                    mPwdText.setVisibility(View.VISIBLE);

                    mUser.setVisibility(View.VISIBLE);
                    mPwd.setVisibility(View.VISIBLE);

                    mUser.setEnabled(true);
                    mPwd.setEnabled(true);
                    if(mEthManager.getAuthState()) {
                        mAuthDhcpEnable.setChecked(true);
                    } else {
                        mAuthDhcpDisable.setChecked(true);
                    }
                }
                if (ENABLE_PROXY) {
                    if (mEthInfo.hasProxy()) {
                        mProxySettingsSpinner.setSelection(PROXY_STATIC);
                        mProxyHostView.setText(mEthInfo.getProxyHost());
                        mProxyPortView.setText(String.valueOf(mEthInfo.getProxyPort()));
                        mProxyExclusionListView.setText(mEthInfo.getProxyExclusionList());
                    }
                }
            }
        }
        mSettingUpValues = false;
        return 0;
    }
    
    private void stopPPPoE() {
           Log.d(TAG,"Swtich ethernet mode, stop pppoe");	   
           mPPPoEConfig.disconnect(mInterfaceName);
    }
    
    private int handle_saveconf() {
        String selected = null;
        if (mDevList.getSelectedItem() != null)
            selected = mDevList.getSelectedItem().toString();
        if (selected == null || selected.isEmpty())
            return 0;
        EthernetDevInfo info = new EthernetDevInfo();
        info.setIfName(selected);
        if (localLOGV)
            Slog.v(TAG, "Config device for " + selected);
        if (mConTypeDhcp.isChecked()) {
            stopPPPoE();
            mEthManager.setEthDhcp(true);
			
            info.setConnectMode(EthernetDevInfo.ETH_CONN_MODE_DHCP);
            SystemProperties.set("ubootenv.var.ipv6_dhcp_config","false");
            mEthManager.setAuthState(null,null,null,false);
        } else if(mConTypeAuthDhcp.isChecked()){
            stopPPPoE();
            info.setConnectMode(EthernetDevInfo.ETH_CONN_MODE_DHCP_AUTH);
            SystemProperties.set("ubootenv.var.ipv6_dhcp_config","false");
            mEthManager.setAuthState(mUser.getText().toString(), mPwd.getText().toString(), 
            		    SystemProperties.get("dhcp.auth.vendorid", "CTCIPTVDHCPAAA"), mAuthDhcpEnable.isChecked());
        } else if(mConTypePPPoE.isChecked()) {
            Slog.i(TAG,"kejun 001");
            String username = mUser.getText().toString();
            String password = mPwd.getText().toString();
            if((username == null) || (username.length() == 0)) {
                Toast.makeText(mContext, R.string.pppoe_settings_error1, Toast.LENGTH_LONG).show();
                return -1;
            } else if((password == null) || (password.length() == 0)) {
                Toast.makeText(mContext, R.string.pppoe_settings_error2, Toast.LENGTH_LONG).show();
                return -1;
            } else {
                mEthManager.setEthDhcp(false);
                info.setConnectMode(EthernetDevInfo.ETH_CONN_MODE_PPPOE);
                mEthManager.updateEthDevInfo(info);

                final PppoeDevInfo pinfo = new PppoeDevInfo();
                pinfo.setIfName(mInterfaceName);
                pinfo.setAccount(username);
                pinfo.setPassword(password);
                pinfo.setDialMode(PppoeDevInfo.PPPOE_DIAL_MODE_AUTO);
                pinfo.setIpType(PppoeDevInfo.PPPOE_IP_V4);
                
                if(mConTypePPPoE.isChecked() 
                        &&  mPPPoEConfig.isConnected(mContext, ConnectivityManager.TYPE_PPPOE)
                        && mPPPoEConfig.isPppOverEthernet()) {
                    mPPPoEConfig.disconnect(mInterfaceName);
                    new Thread(new Runnable(){
                        public void run() {
                            try{
                                Log.d(TAG,"SLEEP 3000");
                                Thread.sleep(3000);
                            }catch (InterruptedException ignored) {
                            }
                            mPPPoEConfig.connect(pinfo);
                        }
                    }).start();
                } else {
                    mPPPoEConfig.connect(pinfo);
                }
            }
            return 1;
        } else {
            stopPPPoE();
            SystemProperties.set("ubootenv.var.ipv6_config","false");
            Slog.i(TAG,"mode manual");
            mEthManager.setAuthState(null,null,null,false);
            String ipaddr = mIpaddr.getText().toString();
            String gwaddr = mGw.getText().toString();
            String dnsaddr = mDns.getText().toString();
            String maskaddr = mMask.getText().toString();
            if (isIpAddress(ipaddr)
                    && isIpAddress(gwaddr)
                    && isIpAddress(dnsaddr)
                    && isIpAddress(maskaddr)) {
                int type = getNetMaskType(maskaddr);
                if(type < 0) {
                    Toast.makeText(mContext, R.string.eth_settings_error1, Toast.LENGTH_LONG).show();
                    return -1;
                }
                if(!isInRange(ipaddr, gwaddr, type)) {
                    Toast.makeText(mContext, R.string.eth_settings_error2, Toast.LENGTH_LONG).show();
                    return -1;
                }
                
                info.setConnectMode(EthernetDevInfo.ETH_CONN_MODE_MANUAL);
                info.setIpAddress(ipaddr);
                info.setRouteAddr(gwaddr);
                info.setDnsAddr(dnsaddr);
                info.setNetMask(maskaddr);
            } else {
                Toast.makeText(mContext, R.string.eth_settings_error, Toast.LENGTH_LONG).show();
                return -1;
            }
        }

        setProxyPropertiesFromEdits(info);
        mEthManager.updateEthDevInfo(info);
        if (mEnablePending) {
            if(mEthManager.getEthState()==mEthManager.ETH_STATE_ENABLED) {
                mEthManager.setEthEnabled(true);
            }
            mEnablePending = false;
        }
        return 0;
    }

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
        final ProgressDialog waiting_dialog = new ProgressDialog(this.getContext());
        switch (which) {
            case BUTTON_POSITIVE:
                int ret = handle_saveconf();
                if(ret == 0) {
                    waiting_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    waiting_dialog.setCancelable(false);
                    waiting_dialog.setTitle("Ethernet Configuration");
                    waiting_dialog.setMessage("Saving...Please Wait");
                    waiting_dialog.show();
                    new Thread(new Runnable(){
                        public void run() {
                            try{
                                Log.d(TAG,"SLEEP 1000");
                                Thread.sleep(1000);
                            }catch (InterruptedException ignored) {
                            }
                            waiting_dialog.cancel();
                        }
                    }).start();
                } else if(ret == 1) {
                    waiting_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    waiting_dialog.setCancelable(false);
                    waiting_dialog.setTitle("Ethernet Configuration");
                    waiting_dialog.setMessage("Saving...Please Wait");
                    waiting_dialog.show();
                    new Thread(new Runnable(){
                        public void run() {
                            try{
                                Log.d(TAG,"SLEEP 3000");
                                Thread.sleep(3000);
                            }catch (InterruptedException ignored) {
                            }
                            waiting_dialog.cancel();
                        }
                    }).start();
                }

                break;
            case BUTTON_NEUTRAL:
                waiting_dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                waiting_dialog.setCancelable(false);
                waiting_dialog.setTitle("PPPoE disconnecting");
                waiting_dialog.setMessage("Please Wait..........");
                waiting_dialog.show();
                new Thread(new Runnable(){
                    public void run() {
                        if(mConTypePPPoE.isChecked() &&
                                mPPPoEConfig.isConnected(mContext, ConnectivityManager.TYPE_PPPOE) &&
                                mPPPoEConfig.isPppOverEthernet()) {
                            mPPPoEConfig.disconnect(mInterfaceName);
                        }
                        waiting_dialog.cancel();
                    }
                }).start();
                break;
            case BUTTON_NEGATIVE:
                //Don't need to do anything
                if(mConTypeAuthDhcp.isChecked()){
                    mEthManager.setAuthState(null,null,null,false);
                }
                break;
            default:
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

    public int getNetMaskType(String ip) {
        String[] ips = ip.split("\\.");
        int ipAddr = (Integer.parseInt(ips[0]) << 24) | (Integer.parseInt(ips[1]) << 16)
                | (Integer.parseInt(ips[2]) << 8) | Integer.parseInt(ips[3]);
        String binStr = Integer.toBinaryString(ipAddr);
        Slog.i(TAG, ip + ": " + binStr);
        int len = binStr.length();
        if(len != 32)
            return -1;
        int type = 0;
        int checkZero = 0;
        for(int i=0; i<len; i++) {
            if(binStr.charAt(i) == '1') {
                if(checkZero == 1) {
                    return -1;
                }
                type++;
            } else {
                checkZero = 1;
            }
        }
        return type;
    }

    public boolean isInRange(String ip, String gw, int type) {
        Slog.i(TAG, ip + ", " + gw + "/" + type);
        String[] ips = ip.split("\\.");
        int ipAddr = (Integer.parseInt(ips[0]) << 24) | (Integer.parseInt(ips[1]) << 16)
                | (Integer.parseInt(ips[2]) << 8) | Integer.parseInt(ips[3]);
        int mask = 0xFFFFFFFF << (32 - type);
        String[] gwIps = gw.split("\\.");
        int gwIpAddr = (Integer.parseInt(gwIps[0]) << 24) | (Integer.parseInt(gwIps[1]) << 16)
                | (Integer.parseInt(gwIps[2]) << 8) | Integer.parseInt(gwIps[3]);

        return (ipAddr & mask) == (gwIpAddr & mask);
    }
}
