/*
 * Copyright (C) 2010 The Android Open Source Project
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
 */

package com.android.settings;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.app.SystemWriteManager;
import android.app.MboxOutputModeManager;

import com.android.internal.view.RotationPolicy;
import com.android.settings.DreamSettings;
import com.android.settings.DreamBackend.DreamInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String TAG = "DisplaySettings";
    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_ACCELEROMETER = "accelerometer";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_DISPLAY_STATUS = "display_status";
    private static final String KEY_HDR_MODE = "hdr_mode";
    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_Brightness = "brightness";
    private static final String KEY_DEFAULT_FREQUENCY = "default_frequency";
    private static final String KEY_SCREEN_SAVER = "screensaver";
    private static final String KEY_DREAM = "dream";
    private static final String KEY_POSITION = "position_display";
    private static final String KEY_WALLPAPER = "wallpaper";
    private static final String KEY_AUTOBRIGHTNESS = "auto_brightness";
    private static final String KEY_GSNEOSR_CONFIG = "gsensor_config";
    private static final String KEY_APP_CHANGE_ORIENTATION = "app_change_orientation";
    private static final String HDMI_SUPPORT_LIST_SYSFS = "/sys/class/amhdmitx/amhdmitx0/disp_cap";
    private final static String mCurrentResolution = "/sys/class/display/mode";
	
    private static final int DLG_GLOBAL_CHANGE_WARNING = 1;
    public static SystemWriteManager sw;

    private CheckBoxPreference mAccelerometer;
    private WarnedListPreference mFontSizePref;
    private WarnedListPreference mDisplayStatusPref;
    private WarnedListPreference mHdrModePref;
    private CheckBoxPreference mNotificationPulse;
    private MboxOutputModeManager mMOMM;
    private final Configuration mCurConfig = new Configuration();
    
    private ListPreference mScreenTimeoutPreference;
    private Preference mScreenSaverPreference;
    private Preference mDreamPreference;
    private Preference mPosiotionPreference;

    private ListPreference  mDefaultFrequency;
    private static final String STR_DEFAULT_FREQUENCY_VAR="ubootenv.var.defaulttvfrequency";
    private static final String BOOTENV_ACCELEROMTER = "ubootenv.var.has.accelerometer";
    private CharSequence[] mDefaultFrequencyEntries;

    private CheckBoxPreference mAppChangeOrientation;

    private int resumeSecond = 30;
    private String curOutputMode = null;
    private AlertDialog confirm_dialog = null;
    private static final int MSG_COUNT_DOWN = 0xE1;//random value

    private final RotationPolicy.RotationPolicyListener mRotationPolicyListener =
            new RotationPolicy.RotationPolicyListener() {
        @Override
        public void onChange() {
            updateAccelerometerRotationCheckbox();
        }
    };
    
    public void onDestroy(){
        super.onDestroy();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMOMM = (MboxOutputModeManager) this.getSystemService(Context.MBOX_OUTPUTMODE_SERVICE);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.display_settings);
		
        String autoBrightness = SystemProperties.get("prop.sp.brightness","on");
        if(autoBrightness.equals("off")) {
            getActivity().getSharedPreferences(AutoBrightnessSwitch.AUTO_PREF_NAME, 
                            Context.MODE_PRIVATE).edit()
                       .putBoolean(AutoBrightnessSwitch.AUTO_PREF_ON_OFF, false).commit();
            getPreferenceScreen().removePreference(findPreference(KEY_AUTOBRIGHTNESS));
        }
		
        sw = (SystemWriteManager)getSystemService("system_write"); 
        mAccelerometer = (CheckBoxPreference) findPreference(KEY_ACCELEROMETER);
        mAccelerometer.setPersistent(false);
        if (!RotationPolicy.isRotationSupported(getActivity())
                || RotationPolicy.isRotationLockToggleSupported(getActivity())) {
            // If rotation lock is supported, then we do not provide this option in
            // Display settings.  However, is still available in Accessibility settings,
            // if the device supports rotation.
            getPreferenceScreen().removePreference(mAccelerometer);
        }

        mScreenSaverPreference = findPreference(KEY_SCREEN_SAVER);
        if ((mScreenSaverPreference != null && 
                !getResources().getBoolean(com.android.internal.R.bool.config_dreamsSupported)) 
                || (Utils.platformHasMbxUiMode())) {
            getPreferenceScreen().removePreference(mScreenSaverPreference);
        }

        mDreamPreference = findPreference(KEY_DREAM);
        if (mDreamPreference != null &&
            !getResources().getBoolean(com.android.internal.R.bool.config_dreamsSupported)) {
            getPreferenceScreen().removePreference(mDreamPreference);
        } else {
            mDreamPreference.setOnPreferenceClickListener(this);
        }

        mPosiotionPreference = findPreference(KEY_POSITION);
        mPosiotionPreference.setOnPreferenceChangeListener(this);
        mPosiotionPreference.setOnPreferenceClickListener(this);

        mScreenTimeoutPreference = (ListPreference) findPreference(KEY_SCREEN_TIMEOUT);
        final long currentTimeout = Settings.System.getLong(resolver, SCREEN_OFF_TIMEOUT,
                FALLBACK_SCREEN_TIMEOUT_VALUE);
        mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
        mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
        disableUnusableTimeouts(mScreenTimeoutPreference);
        updateTimeoutPreferenceDescription(currentTimeout);

        mFontSizePref = (WarnedListPreference) findPreference(KEY_FONT_SIZE);
        mFontSizePref.setOnPreferenceChangeListener(this);
        mFontSizePref.setOnPreferenceClickListener(this);

        mDisplayStatusPref = (WarnedListPreference) findPreference(KEY_DISPLAY_STATUS);
        mDisplayStatusPref.setOnPreferenceChangeListener(this);
        mDisplayStatusPref.setOnPreferenceClickListener(this);
        updateDisplayStatusPref();

        mHdrModePref = (WarnedListPreference) findPreference(KEY_HDR_MODE);
        mHdrModePref.setOnPreferenceChangeListener(this);
        mHdrModePref.setOnPreferenceClickListener(this);
        //updateHdrModePref();

        mNotificationPulse = (CheckBoxPreference) findPreference(KEY_NOTIFICATION_PULSE);
        if (mNotificationPulse != null
                && getResources().getBoolean(
                        com.android.internal.R.bool.config_intrusiveNotificationLed) == false) {
            getPreferenceScreen().removePreference(mNotificationPulse);
        } else {
            try {
                mNotificationPulse.setChecked(Settings.System.getInt(resolver,
                        Settings.System.NOTIFICATION_LIGHT_PULSE) == 1);
                mNotificationPulse.setOnPreferenceChangeListener(this);
            } catch (SettingNotFoundException snfe) {
                Log.e(TAG, Settings.System.NOTIFICATION_LIGHT_PULSE + " not found");
            }
        }

        mAppChangeOrientation = (CheckBoxPreference) findPreference(KEY_APP_CHANGE_ORIENTATION);
        mAppChangeOrientation.setChecked(SystemProperties.getBoolean(BOOTENV_ACCELEROMTER, true));
        mAppChangeOrientation.setOnPreferenceClickListener(this);

        String gSensorConfig = SystemProperties.get("prop.sp.gsensor","false");
        if(gSensorConfig.equals("false")) {
            getActivity().getSharedPreferences(CustomGsensorSwitch.CUSTOM_PREF_NAME, 
                    Context.MODE_PRIVATE).edit().putBoolean(CustomGsensorSwitch.CUSTOM_PREF_ON_OFF, false).commit();
            getPreferenceScreen().removePreference(findPreference(KEY_GSNEOSR_CONFIG));
            SystemProperties.set("persist.sys.gsensor","off");
        }

        if(Utils.platformHasMbxUiMode()) {
            getPreferenceScreen().removePreference(findPreference(KEY_WALLPAPER));
        }

        if(!Utils.platformHasScreenBrightness()) {
            getPreferenceScreen().removePreference(findPreference(KEY_Brightness));
        }

        if(!Utils.platformHasScreenTimeout()) {
            getPreferenceScreen().removePreference(mScreenTimeoutPreference);
        }

        if(!Utils.platformHasScreenFontSize()) {
            getPreferenceScreen().removePreference(mFontSizePref);
        }

        if(Utils.platformHasDefaultTVFreq()) {
            mDefaultFrequency = (ListPreference) findPreference(KEY_DEFAULT_FREQUENCY);
            mDefaultFrequency.setOnPreferenceChangeListener(this);
            String valDefaultFrequency = SystemProperties.get(STR_DEFAULT_FREQUENCY_VAR);
            mDefaultFrequencyEntries = getResources().getStringArray(R.array.default_frequency_entries);
            if(valDefaultFrequency.equals("")) {
                valDefaultFrequency = getResources().getString(R.string.tv_default_frequency_summary);
            }
            int index_DF = findIndexOfEntry(valDefaultFrequency, mDefaultFrequencyEntries);
            mDefaultFrequency.setValueIndex(index_DF);
            mDefaultFrequency.setSummary(valDefaultFrequency);
        } else {
            getPreferenceScreen().removePreference(findPreference(KEY_DEFAULT_FREQUENCY));
        }

        if (!Utils.platformHasTvUiMode()) {
            getPreferenceScreen().removePreference(findPreference(KEY_APP_CHANGE_ORIENTATION));
        }
    }

    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        ListPreference preference = mScreenTimeoutPreference;
        String summary;
        if (currentTimeout < 0) {
            // Unsupported value
            summary = "";
        } else {
            final CharSequence[] entries = preference.getEntries();
            final CharSequence[] values = preference.getEntryValues();
            if (entries == null || entries.length == 0) {
                summary = "";
            } else {
                int best = 0;
                for (int i = 0; i < values.length; i++) {
                    long timeout = Long.parseLong(values[i].toString());
                    if (currentTimeout >= timeout) {
                        best = i;
                    }
                }
                if(currentTimeout >= (Integer.MAX_VALUE-1))
                {
                    summary = entries[best].toString();
                }else{
                    summary = preference.getContext().getString(R.string.screen_timeout_summary,entries[best]);
                }

            }
        }
        preference.setSummary(summary);
    }

    private void disableUnusableTimeouts(ListPreference screenTimeoutPreference) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        final long maxTimeout = dpm != null ? dpm.getMaximumTimeToLock(null) : 0;
        if (maxTimeout == 0) {
            return; // policy not enforced
        }
        final CharSequence[] entries = screenTimeoutPreference.getEntries();
        final CharSequence[] values = screenTimeoutPreference.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.parseLong(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            final int userPreference = Integer.parseInt(screenTimeoutPreference.getValue());
            screenTimeoutPreference.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            screenTimeoutPreference.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            if (userPreference <= maxTimeout) {
                screenTimeoutPreference.setValue(String.valueOf(userPreference));
            } else if (revisedValues.size() > 0
                    && Long.parseLong(revisedValues.get(revisedValues.size() - 1).toString())
                    == maxTimeout) {
                // If the last one happens to be the same as the max timeout, select that
                screenTimeoutPreference.setValue(String.valueOf(maxTimeout));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        screenTimeoutPreference.setEnabled(revisedEntries.size() > 0);
    }

    int floatToIndex(float val) {
        String[] indices = getResources().getStringArray(R.array.entryvalues_font_size);
        float lastVal = Float.parseFloat(indices[0]);
        for (int i=1; i<indices.length; i++) {
            float thisVal = Float.parseFloat(indices[i]);
            if (val < (lastVal + (thisVal-lastVal)*.5f)) {
                return i-1;
            }
            lastVal = thisVal;
        }
        return indices.length-1;
    }
    
    public void readFontSizePreference(ListPreference pref) {
        try {
            mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to retrieve font size");
        }

        // mark the appropriate item in the preferences list
        int index = floatToIndex(mCurConfig.fontScale);
        pref.setValueIndex(index);

        // report the current size in the summary text
        final Resources res = getResources();
        String[] fontSizeNames = res.getStringArray(R.array.entries_font_size);
        pref.setSummary(String.format(res.getString(R.string.summary_font_size),
                fontSizeNames[index]));
    }
    
    @Override
    public void onResume() {
        super.onResume();       
        updateState();

        RotationPolicy.registerRotationPolicyListener(getActivity(),
                mRotationPolicyListener);

        updateState();
    }

    @Override
    public void onPause() {
        super.onPause();

        RotationPolicy.unregisterRotationPolicyListener(getActivity(),
                mRotationPolicyListener);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DLG_GLOBAL_CHANGE_WARNING) {
            return Utils.buildGlobalChangeWarningDialog(getActivity(),
                    R.string.global_font_change_title,
                    new Runnable() {
                        public void run() {
                            mFontSizePref.click();
                        }
                    });
        }
        return null;
    }
    
    private void updateDisplayStatusPref() {
        String supportModes = mMOMM.getSupportResoulutionList();
        Log.d(TAG, "supportModes: " + supportModes);
        String[] supportList = null;
        if(supportModes != null) {
            supportList = supportModes.split(",");
        }
        CharSequence[] mEntries = new CharSequence[supportList.length];
        for (int index = 0; index < supportList.length; index++) {
            mEntries[index] = supportList[index];
        }
        mDisplayStatusPref.setEntries(mEntries);
        mDisplayStatusPref.setEntryValues(mEntries);
    }

    private void updateHdrModePref() {
        CharSequence[] mEntries = new CharSequence[3];
        mEntries[0] = "Off";
        mEntries[1] = "On";
        mEntries[2] = "Auto";

        mHdrModePref.setEntries(mEntries);
        mHdrModePref.setEntryValues(mEntries);
    }

    private void updateHdrMode(){
        updateHdrModePref();
        int mode  = mMOMM.getHdrMode();
        Log.d(TAG, "updateHdrMode, mode: " + mode);

        String hdr_mode;
        if (mode == 0) {
            hdr_mode = "Off";
        } else if (mode == 1) {
            hdr_mode = "On";
        } else {
            hdr_mode = "Auto";
        }

        CharSequence output = hdr_mode;
        mHdrModePref.setValue(hdr_mode);
        mHdrModePref.setSummary(output);
    }

    private void updateState() {
        updateAccelerometerRotationCheckbox();
        readFontSizePreference(mFontSizePref);
        updateScreenSaverSummary();
        updateRequestRotationCheckbox();
        updatedisplaysetting();
        updateHdrMode();
    }

    private void updateScreenSaverSummary() {
        if (mScreenSaverPreference != null) {
            mScreenSaverPreference.setSummary(
                    DreamSettings.getSummaryTextWithDreamName(getActivity()));
        }
    }

    private void updateAccelerometerRotationCheckbox() {
        if (getActivity() == null) return;

        mAccelerometer.setChecked(!RotationPolicy.isRotationLocked(getActivity()));
    }

    private void updateRequestRotationCheckbox() {
        if (getActivity() == null) return;
    }

    private void updatedisplaysetting(){
        updateDisplayStatusPref();
        String outputmode  = mMOMM.getCurrentOutPutMode();
        Log.d(TAG, "updatedisplaysetting, outputmode: " + outputmode);
        CharSequence output = outputmode;
        mDisplayStatusPref.setValue(outputmode);
        mDisplayStatusPref.setSummary(output);
    }
	
    public void writeFontSizePreference(Object objValue) {
        try {
            mCurConfig.fontScale = Float.parseFloat(objValue.toString());
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to save font size");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mAccelerometer) {
            RotationPolicy.setRotationLockForAccessibility(
                    getActivity(), !mAccelerometer.isChecked());
        } else if (preference == mNotificationPulse) {
            boolean value = mNotificationPulse.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.NOTIFICATION_LIGHT_PULSE,
                    value ? 1 : 0);
            return true;
        
        }    
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void showConfirmDialog() {
        resumeSecond = 30;
        confirm_dialog = new AlertDialog.Builder(getActivity())
            .setMessage(R.string.display_confirm) 
            .setPositiveButton(getActivity().getResources().getString(R.string.display_confirm_ok) + " ( "+resumeSecond+" )",  
                new DialogInterface.OnClickListener() {  
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }  
                })  
            .show(); 
        confirm_dialog.setOnDismissListener(new confirmDismissListener());
        ResumeCountdown();
    }

    protected void ResumeCountdown() {
        final Handler handler = new Handler(){   	  
            public void handleMessage(Message msg) {   
                switch (msg.what) {       
                    case MSG_COUNT_DOWN:
                    if(confirm_dialog.isShowing()) {
                        if(resumeSecond > 0) {
                            String cancel = getActivity().getResources().getString(R.string.display_confirm_ok);
                            confirm_dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(cancel+" ( "+(--resumeSecond)+" )");
                            ResumeCountdown();
                        }
                        else {
                            confirm_dialog.dismiss();
                            mMOMM.setOutputMode(curOutputMode);
                            updatedisplaysetting();
                            curOutputMode = null;
                        }
                    }
                    break;       
                }       
                super.handleMessage(msg);   
            }  
        };

        TimerTask task = new TimerTask(){   
            public void run() {   
                Message message = Message.obtain();
                message.what = MSG_COUNT_DOWN;       
                handler.sendMessage(message);     
            }   
        };   
        
        Timer resumeTimer = new Timer();
        resumeTimer.schedule(task, 1000);
    }
    
    private class confirmDismissListener implements DialogInterface.OnDismissListener {
        public void onDismiss(DialogInterface arg0) {
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_SCREEN_TIMEOUT.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            try {
                Settings.System.putInt(getContentResolver(), SCREEN_OFF_TIMEOUT, value);
                updateTimeoutPreferenceDescription(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        }
        if (KEY_FONT_SIZE.equals(key)) {
            writeFontSizePreference(objValue);
        }
        if (KEY_DISPLAY_STATUS.equals(key)) {
            curOutputMode = mMOMM.getCurrentOutPutMode();
            mMOMM.setOutputMode(objValue.toString());
            updatedisplaysetting();
            showConfirmDialog();
        }
        if (KEY_HDR_MODE.equals(key)) {
            String hdr_mode = objValue.toString();
            int mode = 2;
            if ("Off".equals(hdr_mode)) {
                mode = 0;
            } else if ("On".equals(hdr_mode)) {
                mode = 1;
            } else {
                mode = 2;
            }

            mMOMM.setHdrMode(mode);
            updateHdrMode();
        }

        if (KEY_DEFAULT_FREQUENCY.equals(key)) {
            try {
                int frequency_index = Integer.parseInt((String) objValue);
                mDefaultFrequency.setSummary(mDefaultFrequencyEntries[frequency_index]);
                SystemProperties.set(STR_DEFAULT_FREQUENCY_VAR, mDefaultFrequencyEntries[frequency_index].toString());
            } catch(NumberFormatException e) {
                Log.e(TAG, "could not persist default TV frequency setting", e);
            }
        }

        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mFontSizePref) {
            if (Utils.hasMultipleUsers(getActivity())) {
                showDialog(DLG_GLOBAL_CHANGE_WARNING);
                return true;
            } else {
                mFontSizePref.click();
            }
        } else if(preference == mDisplayStatusPref) {
            Log.d(TAG , "display settings");
            mDisplayStatusPref.click();
        } else if(preference == mHdrModePref) {
            Log.d(TAG , "hdr mode");
            mHdrModePref.click();
        } else if(preference == mPosiotionPreference) {
            Log.d(TAG , "display posiotion settings");
            Intent mintent = new Intent();
            mintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ComponentName componentName = new ComponentName("com.android.settings", "com.android.settings.ScreenPositionManagerDisPlay");
            mintent.setComponent(componentName); 
            startActivity(mintent);
        } else if (preference == mDreamPreference) {
            DreamBackend backend = new DreamBackend(getActivity());
            List<DreamInfo> dreamInfos = backend.getDreamInfos();
            for (DreamInfo info : dreamInfos) {
                if (info.toString().contains("FlipperDreamSettings")) {
                    backend.launchSettings(info);
                }
            }
        } else if (preference == mAppChangeOrientation) {
            if (mAppChangeOrientation.isChecked()) {
                SystemProperties.set(BOOTENV_ACCELEROMTER, "true");
            } else {
                SystemProperties.set(BOOTENV_ACCELEROMTER, "false");
            }
        }
        return false;
    }
    
    private int findIndexOfEntry(String value, CharSequence[] entry) {
        if (value != null && entry != null) {
            for (int i = entry.length - 1; i >= 0; i--) {
                if (entry[i].equals(value)) {
                    return i;
                }
            }
        }
		
        return getResources().getInteger(R.integer.outputmode_default_values);  //set 720p as default
    }
}
