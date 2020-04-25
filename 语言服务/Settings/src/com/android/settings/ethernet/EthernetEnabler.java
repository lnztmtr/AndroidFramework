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


import static android.net.ethernet.EthernetManager.ETH_STATE_DISABLED;
import static android.net.ethernet.EthernetManager.ETH_STATE_ENABLED;
import static android.net.ethernet.EthernetManager.ETH_STATE_UNKNOWN;

import com.android.settings.R;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.ethernet.EthernetManager;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.Config;
import android.util.Slog;
import android.widget.CompoundButton;
import android.widget.Switch;

public class EthernetEnabler implements CompoundButton.OnCheckedChangeListener  {
    private static final String TAG = "SettingsEthEnabler";

    private static final boolean LOCAL_LOGD = true;
    private EthernetManager mEthManager;
    private Switch mSwitch;
    private boolean mStateMachineEvent;
    private EthernetConfigDialog mEthConfigDialog = null;

    public void setConfigDialog(EthernetConfigDialog Dialog) {
        mEthConfigDialog = Dialog;
    }

    public EthernetEnabler(EthernetManager ethernetManager, Switch _switch) {
        mEthManager = ethernetManager;
        mSwitch = _switch;
		Slog.d(TAG, "EthernetEnabler: "+mEthManager.getEthState());
        if (mEthManager.getEthState() == ETH_STATE_ENABLED) {
            setSwitchChecked(true);
        }
    }

    public EthernetManager getManager() {
        return mEthManager;
    }

    public void resume() {
        mSwitch.setOnCheckedChangeListener(this);
        final int state = mEthManager.getEthState();
        Slog.d(TAG, "resume: " + state);
        boolean isEnabled = state == ETH_STATE_ENABLED;
        boolean isDisabled = state == ETH_STATE_DISABLED;
        mSwitch.setChecked(isEnabled);
        mSwitch.setEnabled(isEnabled || isDisabled);
    }

    public void pause() {
        mSwitch.setOnCheckedChangeListener(null);
    }

    public void setSwitch(Switch switch_) {
        if (mSwitch == switch_) return;
        mSwitch.setOnCheckedChangeListener(null);
        mSwitch = switch_;
        mSwitch.setOnCheckedChangeListener(this);

        final int state = mEthManager.getEthState();
		Slog.d(TAG, "setSwitch: " + state);
        boolean isEnabled = state == ETH_STATE_ENABLED;
        boolean isDisabled = state == ETH_STATE_DISABLED;
        mSwitch.setChecked(isEnabled);
        mSwitch.setEnabled(isEnabled || isDisabled);
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        //Do nothing if called as a result of a state machine event
        if (mStateMachineEvent) {
            return;
        }
        // Show toast message if no Ethernet interfaces
        if (isChecked && false) {
            //Toast.makeText(mContext, R.string.wifi_in_airplane_mode, Toast.LENGTH_SHORT).show();
            // Reset switch to off. No infinite check/listener loop.
            buttonView.setChecked(false);
        }

        setEthEnabled(isChecked);
        setSwitchChecked(isChecked);
    }

    private void setSwitchChecked(boolean checked) {
		Slog.d(TAG, "-------------setSwitchChecked");
        if (checked != mSwitch.isChecked()) {
			Slog.d(TAG, "setSwitchChecked: " + checked);
            mStateMachineEvent = true;
            mSwitch.setChecked(checked);
            mStateMachineEvent = false;
        }
    }

    private void setEthEnabled(final boolean enable) {
        int state = mEthManager.getEthState();

        if (LOCAL_LOGD) Slog.d(TAG, "setEthEnabled " + enable);

        mEthManager.setEthEnabled(enable);
    }
}
