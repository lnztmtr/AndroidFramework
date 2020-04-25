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
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import android.os.ParcelUuid;

import com.android.settings.R;

/**
 * BluetoothAuthorizeRequest extends BroadcastReceiver to receive the ACTION_AUTHORIZE_REQUEST
 * intent, typically originated from a Bluetooth authorization request. This receiver is
 * staticlly declared in AndroidManifest,xml. BluetoothAuthorizeRequest will be instantiated
 * by the system when a ACTION_AUTHORIZE_REQUEST is broadcasted. Upon receiving
 * ACTION_AUTHORIZE_REQUEST intent, BluetoothAuthorizeRequest brings up the authorization
 * dialog implemented in BluetoothAuthorizeDialog.
 */
public class BluetoothAuthorizeRequest extends BroadcastReceiver {
    private static final String TAG = "BluetoothAuthorizeRequest";

    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive");

        String action = intent.getAction();

        /*
        if (action.equals(BluetoothDevice.ACTION_AUTHORIZE_REQUEST)) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            ParcelUuid  service = ParcelUuid.fromString(intent.getStringExtra(BluetoothDevice.EXTRA_UUID));*/

            /* sap_weak_linkkey is a special "service" string to notify us that
             * the link key is weak (for SAP)

            if (service.equalsIgnoreCase("sap_weak_linkkey")) {
                Log.v(TAG, "Rejecting SAP connection authorization due to weak link key");
                device.authorizeService( service, false, false);
                Toast.makeText(context, R.string.bluetooth_sap_weak_linkkey, Toast.LENGTH_LONG).show();
                return;
            }
              */
            /*
            intent.setClass(context, BluetoothAuthorizeDialog.class);
            intent.setAction(BluetoothDevice.ACTION_AUTHORIZE_REQUEST);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else {
            Log.e(TAG, "Unknown intent action:  " + action);
        }
        
        // Always pop up the dialog since notifications may not be seen in fullscreen apps
        context.startActivity(intent);
        */
    }
}
