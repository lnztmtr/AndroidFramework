/*
 * This program is protected under international and U.S. copyright laws as
 * an unpublished work. These changes are confidential and proprietary to the
 * copyright owners. Reproduction or disclosure, in whole or in part, or the
 * production of derivative works therefrom without the express permission of
 * the copyright owners is prohibited.
 *
 *                Copyright (C) 2011-2012 by Dolby Laboratories,
 *                            All rights reserved.
 */
package com.android.settings;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.dolby.DsClient;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SoundDolbyProfilePreference extends Preference implements
        DialogInterface.OnDismissListener, AdapterView.OnItemClickListener,
        CompoundButton.OnCheckedChangeListener {

    private static final int PREDEFINED_PROFILE_COUNT = 4;

    private static DsClient mDsClient;

    private Dialog mDialog;

    private ListView mList;

    private ProfilesAdapter mListAdapter;

    private int mSelectedPosition = -1;

    public static void setDsClient(DsClient dsClient) {
        mDsClient = dsClient;
    }

    public SoundDolbyProfilePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        DolbyAssets.init(context);

    }

    public SoundDolbyProfilePreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        DolbyAssets.init(context);
    }

    @Override
    protected void onClick() {
        showProfilesPopup();
    }

    private void showProfilesPopup() {
        DsClient dsClient = mDsClient;
        if (dsClient == null) {
            return;
        }

        final boolean dolbyOn;
        try {
            mSelectedPosition = dsClient.getSelectedProfile();
            dolbyOn = dsClient.getDsOn();
        } catch (Exception e) {
            e.printStackTrace();
            onDsClientError();
            return;
        }

        final LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.dolby_profiles_list, null);
        mList = (ListView) view.findViewById(R.id.dolby_profiles_list);
        mListAdapter = new ProfilesAdapter();
        mList.setAdapter(mListAdapter);
        mList.setOnItemClickListener(dolbyOn ? this : null);
        mList.setEnabled(dolbyOn);
        final CheckBox vPower = (CheckBox) view.findViewById(R.id.dolby_power);
        vPower.setOnCheckedChangeListener(this);
        vPower.setChecked(dolbyOn);
        onCheckedChanged(vPower, dolbyOn);

        final Dialog d = new Dialog(getContext());
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(view);
        d.setOnDismissListener(this);
        mDialog = d;
        d.show();
    }

    private void onDsClientError() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        callChangeListener(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == -1 || position == mSelectedPosition) {
            return;
        }
        final DsClient dsClient = mDsClient;
        if (dsClient == null) {
            return;
        }

        try {
            dsClient.setSelectedProfile(position);
        } catch (Exception e) {
            e.printStackTrace();
            onDsClientError();
            return;
        }

        mSelectedPosition = position;
        mListAdapter.notifyDataSetChanged();
        callChangeListener(null);
    }

    @Override
    public void onDismiss(DialogInterface d) {
        mDialog = null;
        mList = null;
        mListAdapter = null;
        callChangeListener(null);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        buttonView.setText(getContext().getString(
                isChecked ? R.string.dolby_on : R.string.dolby_off));
        DsClient dsClient = mDsClient;
        if (dsClient != null) {
            try {
                dsClient.setDsOn(isChecked);
            } catch (Exception e) {
                e.printStackTrace();
                onDsClientError();
            }
        }
        if (mDialog != null && mDialog.isShowing()) {
            if (mListAdapter != null) {
                mListAdapter.notifyDataSetChanged();
            }
            if (mList != null) {
                mList.setEnabled(isChecked);
                mList.setOnItemClickListener(isChecked ? this : null);
            }
            callChangeListener(null);
        }
    }

    private class ProfilesAdapter extends BaseAdapter {

        private final Profile mProfiles[];

        private final String mDefaultProfileNames[];

        private ProfilesAdapter() {
            final Context context = getContext();
            mDefaultProfileNames = new String[6];
            mDefaultProfileNames[0] = context.getString(R.string.dolby_profile_movie);
            mDefaultProfileNames[1] = context.getString(R.string.dolby_profile_music);
            mDefaultProfileNames[2] = context.getString(R.string.dolby_profile_game);
            mDefaultProfileNames[3] = context.getString(R.string.dolby_profile_voice);
            mDefaultProfileNames[4] = context.getString(R.string.dolby_profile_preset_1);
            mDefaultProfileNames[5] = context.getString(R.string.dolby_profile_preset_2);

            // create default set of presets
            mProfiles = new Profile[6];
            mProfiles[0] = new Profile(R.drawable.dolby_movieon, R.drawable.dolby_movieoff,
                    R.drawable.dolby_moviedis);
            mProfiles[1] = new Profile(R.drawable.dolby_musicon, R.drawable.dolby_musicoff,
                    R.drawable.dolby_musicdis);
            mProfiles[2] = new Profile(R.drawable.dolby_gameon, R.drawable.dolby_gameoff,
                    R.drawable.dolby_gamedis);
            mProfiles[3] = new Profile(R.drawable.dolby_voiceon, R.drawable.dolby_voiceoff,
                    R.drawable.dolby_voicedis);
            mProfiles[4] = new Profile(R.drawable.dolby_preset1on, R.drawable.dolby_preset1off,
                    R.drawable.dolby_preset1dis);
            mProfiles[5] = new Profile(R.drawable.dolby_preset2on, R.drawable.dolby_preset2off,
                    R.drawable.dolby_preset2dis);
        }

        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final DsClient dsClient = mDsClient;

            View row = convertView;

            if (row == null) {
                row = LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.dolby_preset_list_item, null);
                // one-time initialization
                TextView tv = (TextView) row.findViewById(R.id.dolby_name);
                if (tv != null) {
                    tv.setTypeface(DolbyAssets.getFont());
                }
            }

            final Profile item = mProfiles[position];
            final boolean engineEnabled = parent.isEnabled();
            String itemName = null;
            boolean enabled;
            if (position < PREDEFINED_PROFILE_COUNT) {
                itemName = mDefaultProfileNames[position];
                enabled = true;
            } else {
                itemName = null;
                if (dsClient != null) {
                    try {
                        itemName = dsClient.getProfileNames()[position];
                    } catch (Exception e) {
                        e.printStackTrace();
                        onDsClientError();
                    }
                }
                enabled = itemName != null;
                if (itemName == null) {
                    itemName = mDefaultProfileNames[position];
                }
            }
            enabled = enabled && engineEnabled;

            final boolean selected = (position == mSelectedPosition) && engineEnabled;
            final TextView nameTextView = (TextView) row.findViewById(R.id.dolby_name);
            final ImageView icon = (ImageView) row.findViewById(R.id.dolby_icon);

            if (nameTextView != null) {
                nameTextView.setText(itemName);
                nameTextView.setTextColor(parent.getResources().getColor(
                        enabled ? R.color.dolby_white : R.color.dolby_disabledblue));
            }
            icon.setImageResource(item.getIcon(selected, enabled));

            row.setBackgroundResource(selected ? R.drawable.dolby_highlight : 0);

            return row;
        }

        @Override
        public int getCount() {
            return mProfiles.length;
        }

        @Override
        public Object getItem(int position) {
            return mProfiles[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

    }

    private class Profile {

        private int mIconSelected, mIconNormal, mIconDisabled;

        public Profile(int iconSelected, int iconNormal, int iconDisabled) {
            mIconSelected = iconSelected;
            mIconNormal = iconNormal;
            mIconDisabled = iconDisabled;
        }

        public int getIcon(boolean selected, boolean enabled) {
            return selected ? mIconSelected : (enabled ? mIconNormal : mIconDisabled);
        }
    }

    private static boolean isLandscapeScreenOrientation(Context context) {
        return Configuration.ORIENTATION_LANDSCAPE == context.getResources().getConfiguration().orientation;
    }

}
