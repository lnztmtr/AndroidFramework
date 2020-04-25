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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.net.ethernet.EthernetManager;
import android.net.ethernet.EthernetStateTracker;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class EthernetLayer {
    private static final String TAG = "EthernetLayer";

    private EthernetManager mEthManager;
    private String[] mDevList;
    private EthernetConfigDialog mDialog;
    private final IntentFilter mIntentFilter;
    private Handler mHandler;

    EthernetLayer(EthernetConfigDialog configdialog, EthernetManager ethManager) {
        mDialog = configdialog;
        mEthManager = ethManager;
        mIntentFilter = new IntentFilter(EthernetManager.ETH_STATE_CHANGED_ACTION);
        mHandler = new Handler();
    }

    public void resume() {
        mDialog.getContext().registerReceiver(mReceiver, mIntentFilter);
    }

    public void pause() {
        mDialog.getContext().unregisterReceiver(mReceiver);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(EthernetManager.EXTRA_ETH_STATE,
                    EthernetStateTracker.EVENT_HW_DISCONNECTED);
            Log.d(TAG, " recv state=" + state);
            if (state == EthernetStateTracker.EVENT_HW_CONNECTED ||
                    state == EthernetStateTracker.EVENT_HW_PHYCONNECTED) {
                handleDevListChanges();
            } else if (state == EthernetStateTracker.EVENT_HW_DISCONNECTED ||
                    state == EthernetStateTracker.EVENT_HW_CHANGED) {
                // Unfortunately, the interface will still be listed when this
                // intent is sent, so delay updating.
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        handleDevListChanges();
                    }
                }, 700);
            }
        }
    };

    private void handleDevListChanges() {
        mDevList = mEthManager.getDeviceNameList();
        mDialog.updateDevNameList(mDevList);
        mDevList = null;
    }
}
