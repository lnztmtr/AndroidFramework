/************************************************************************************
 *
 *  Copyright (C) 2009-2012 Broadcom Corporation
 *
 *  This program is the proprietary software of Broadcom Corporation and/or its
 *  licensors, and may only be used, duplicated, modified or distributed
 *  pursuant to the terms and conditions of a separate, written license
 *  agreement executed between you and Broadcom (an "Authorized License").
 *  Except as set forth in an Authorized License, Broadcom grants no license
 *  (express or implied), right to use, or waiver of any kind with respect to
 *  the Software, and Broadcom expressly reserves all rights in and to the
 *  Software and all intellectual property rights therein.
 *  IF YOU HAVE NO AUTHORIZED LICENSE, THEN YOU HAVE NO RIGHT TO USE THIS
 *  SOFTWARE IN ANY WAY, AND SHOULD IMMEDIATELY NOTIFY BROADCOM AND DISCONTINUE
 *  ALL USE OF THE SOFTWARE.
 *
 *  Except as expressly set forth in the Authorized License,
 *
 *  1.     This program, including its structure, sequence and organization,
 *         constitutes the valuable trade secrets of Broadcom, and you shall
 *         use all reasonable efforts to protect the confidentiality thereof,
 *         and to use this information only in connection with your use of
 *         Broadcom integrated circuit products.
 *
 *  2.     TO THE MAXIMUM EXTENT PERMITTED BY LAW, THE SOFTWARE IS PROVIDED
 *         "AS IS" AND WITH ALL FAULTS AND BROADCOM MAKES NO PROMISES,
 *         REPRESENTATIONS OR WARRANTIES, EITHER EXPRESS, IMPLIED, STATUTORY,
 *         OR OTHERWISE, WITH RESPECT TO THE SOFTWARE.  BROADCOM SPECIFICALLY
 *         DISCLAIMS ANY AND ALL IMPLIED WARRANTIES OF TITLE, MERCHANTABILITY,
 *         NONINFRINGEMENT, FITNESS FOR A PARTICULAR PURPOSE, LACK OF VIRUSES,
 *         ACCURACY OR COMPLETENESS, QUIET ENJOYMENT, QUIET POSSESSION OR
 *         CORRESPONDENCE TO DESCRIPTION. YOU ASSUME THE ENTIRE RISK ARISING OUT
 *         OF USE OR PERFORMANCE OF THE SOFTWARE.
 *
 *  3.     TO THE MAXIMUM EXTENT PERMITTED BY LAW, IN NO EVENT SHALL BROADCOM OR
 *         ITS LICENSORS BE LIABLE FOR
 *         (i)   CONSEQUENTIAL, INCIDENTAL, SPECIAL, INDIRECT, OR EXEMPLARY
 *               DAMAGES WHATSOEVER ARISING OUT OF OR IN ANY WAY RELATING TO
 *               YOUR USE OF OR INABILITY TO USE THE SOFTWARE EVEN IF BROADCOM
 *               HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES; OR
 *         (ii)  ANY AMOUNT IN EXCESS OF THE AMOUNT ACTUALLY PAID FOR THE
 *               SOFTWARE ITSELF OR U.S. $1, WHICHEVER IS GREATER. THESE
 *               LIMITATIONS SHALL APPLY NOTWITHSTANDING ANY FAILURE OF
 *               ESSENTIAL PURPOSE OF ANY LIMITED REMEDY.
 *
 ************************************************************************************/

package com.android.settings.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;


/**
 * BluetoothAuthorizeDialog asks the user to confirm that they approve automatic
 * pairing of devices.
 */
public class BluetoothAuthorizeDialog extends AlertActivity implements
        DialogInterface.OnClickListener{
    private static final String TAG = "BluetoothAuthorizeDialog";
    private static final boolean V = true;
    private BluetoothDevice mDevice;
    private LocalBluetoothManager mLocalManager;
    private ParcelUuid mServiceUuid;
    private String mName;
    private boolean mTemporaryKey = false;
    private ListenForPairingCancel mBrcvr;
    private static Context mContext;
    private static PowerManager pm;
    private static PowerManager.WakeLock wl;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
        if (V) Log.v(TAG, "onCreate");

        Intent intent = getIntent();
        String action = intent.getAction();
        if (!action.equals(BluetoothDevice.ACTION_AUTHORIZE_REQUEST)) {
            Log.e(TAG, "onCreate: Unknown intent " + action);
            finish();
            return;
        }

        mDevice       = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        mServiceUuid  = ParcelUuid.fromString(intent.getStringExtra(BluetoothDevice.EXTRA_UUID));

        mBrcvr = new ListenForPairingCancel();

        IntentFilter intFltr = new IntentFilter();
        intFltr.addAction(BluetoothDevice.ACTION_PAIRING_CANCEL);
        intFltr.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        this.getBaseContext().registerReceiver(mBrcvr, intFltr);

        mLocalManager = LocalBluetoothManager.getInstance(this);
        mContext = mLocalManager.getContext();
        mName = mLocalManager.getCachedDeviceManager().getName(mDevice);

        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP |
                            PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                            PowerManager.ON_AFTER_RELEASE, TAG);

        String svc = getServiceName(mServiceUuid);


        // Set up the "dialog"
        final AlertController.AlertParams p = mAlertParams;
        p.mIconId = android.R.drawable.ic_dialog_info;
        p.mTitle = getString(R.string.authdlg_title, mName);
        p.mView = createView(mName, svc);

        p.mPositiveButtonText = getString(R.string.authdlg_service_accept);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonText = getString(R.string.authdlg_service_decline);
        p.mNegativeButtonListener = this;

        setupAlert();

        boolean isScreenOn = pm.isScreenOn();
        if (!isScreenOn) {
            wl.acquire();
            if (V) Log.v(TAG,"Wake Lock acquired");
        }*/
    }

    private View createView(String deviceName, String svc) {
        Log.v(TAG, "createView");
        View view = getLayoutInflater().inflate(
                R.layout.bluetooth_authorize_service, null);

        TextView msgView = (TextView) view.findViewById(R.id.message1);
        msgView.setText(getString(R.string.authdlg_message, deviceName,svc));

        if (mTemporaryKey) {
            CheckBox checkbox = (CheckBox) view.findViewById(R.id.checkbox1);
            TextView textview = (TextView) view.findViewById(R.id.autoaccept_text);

            checkbox.setEnabled(false);
            textview.setEnabled(false);
        }

        return view;
    }

    private boolean isAutoReply() {
        /* when temporary key is used, checkbox is hidden */
        if (mTemporaryKey)
            return false;
        CheckBox checkbox = (CheckBox) findViewById(R.id.checkbox1);
        boolean isChecked =  checkbox.isChecked();
        Log.v(TAG, "isChecked =" + isChecked);
        return isChecked;
    }

    private void onAuthorize() {
        Log.v(TAG, "onAuthorize");
        //mDevice.authorizeService(mServiceUuid, true, isAutoReply());
    }

    private void onDecline() {
        Log.v(TAG, "onDecline");
        //mDevice.authorizeService(mServiceUuid, false, isAutoReply());
    }

    public void onClick(DialogInterface dialog, int which) {
        Log.v(TAG, "onClick");
        switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
            if (wl.isHeld()) {
                wl.release();
                if (V) Log.v(TAG,"Wake Lock released");
            }
            onAuthorize();
            break;

        case DialogInterface.BUTTON_NEGATIVE:
            if (wl.isHeld()) {
                wl.release();
                if (V) Log.v(TAG,"Wake Lock released");
            }
            onDecline();
            break;
        }
    }

    protected void onDestroy() {
        this.getBaseContext().unregisterReceiver(mBrcvr);
        super.onDestroy();
    }


    private void quitActivity() {
        if (wl.isHeld()) {
            wl.release();
            if (V) Log.v(TAG,"Wake Lock released");
        }
        finish();
    }

    private String getServiceName (ParcelUuid uuid) {
       //if ( BluetoothUuid.isPbap(uuid))  return getString(R.string.authdlg_service_pbap);
       //if ( BluetoothUuid.isOpp(uuid))  return getString(R.string.authdlg_service_opp);

       return getString(R.string.authdlg_service_default);
    }

    private class ListenForPairingCancel extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            /* Show the error message & dismiss the dialog when ACL got disconnected
             * or when Agent Cancel user action occur
             */

            /*
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction()) ||
                BluetoothDevice.ACTION_PAIRING_CANCEL.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null || device.equals(mDevice)) {
                    String name = mName;
                    if (name == null) {
                        mContext.getString(R.string.bluetooth_remote_device);
                    }

                    Utils.showError(mContext, name, R.string.authorizaion_failed_error);
                    quitActivity();
                }
            }
            else {
                quitActivity();
            }*/
        }
    }
}
