/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

public class HdmiSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private static final String TAG = "HdmiSettings";

    private static final String KEY_DUAL_DISP = "dual_disp";
    private static final String KEY_SPDIF = "spdif";
    private static final String KEY_AUTO_SWITCH = "auto_switch";

    private CheckBoxPreference mDualDispPref;
    private ListPreference mSpdifPref;
    private CheckBoxPreference mAutoSwitchPref;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.hdmi_prefs);
        
        if (!Utils.platformHasHdmiDualDisp())
            getPreferenceScreen().removePreference(findPreference(KEY_DUAL_DISP));
        
        if (!Utils.platformHasHdmiSpdif())
            getPreferenceScreen().removePreference(findPreference(KEY_SPDIF));
            
        if (!Utils.platformHasHdmiAutoSwitch())
            getPreferenceScreen().removePreference(findPreference(KEY_AUTO_SWITCH));            
        
        initUI();
        updateUI();
    }

    private void initUI() {
        mDualDispPref = (CheckBoxPreference) findPreference(KEY_DUAL_DISP);        
        if (mDualDispPref != null)
            mDualDispPref.setOnPreferenceChangeListener(this);
        
        mSpdifPref = (ListPreference) findPreference(KEY_SPDIF);
        if (mSpdifPref != null)
            mSpdifPref.setOnPreferenceChangeListener(this);
            
        mAutoSwitchPref = (CheckBoxPreference) findPreference(KEY_AUTO_SWITCH);        
        if (mAutoSwitchPref != null)
            mAutoSwitchPref.setOnPreferenceChangeListener(this);            
        
    }
    
    private void updateUI() {
        if (mDualDispPref != null) {      
            mDualDispPref.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.HDMI_DUAL_DISP, 1) != 0);
        }
        
        if (mSpdifPref != null) {
            mSpdifPref.setValue(String.valueOf(Settings.System.getInt(getContentResolver(),
                    Settings.System.HDMI_SPDIF, 0)));
            mSpdifPref.setSummary(mSpdifPref.getEntry());
        }
        
        if (mAutoSwitchPref != null) {      
            mAutoSwitchPref.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.HDMI_AUTO_SWITCH, 1) != 0);
        }        
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
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();

        if (KEY_SPDIF.equals(key)) {
            Settings.System.putInt(getContentResolver(), Settings.System.HDMI_SPDIF,
                    Integer.parseInt((String) objValue));
        }
        
        updateUI();
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mDualDispPref) {
            Settings.System.putInt(getContentResolver(), Settings.System.HDMI_DUAL_DISP,
                    mDualDispPref.isChecked() ? 1 : 0);            
        } else if (preference == mAutoSwitchPref) {
            Settings.System.putInt(getContentResolver(), Settings.System.HDMI_AUTO_SWITCH,
                    mAutoSwitchPref.isChecked() ? 1 : 0);              
        }
        
        updateUI();
        return true;
    }


}
