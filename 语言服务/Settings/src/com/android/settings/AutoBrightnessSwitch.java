package com.android.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

@SuppressLint("NewApi")
public class AutoBrightnessSwitch extends SwitchPreference{
	
	Context mContext = null;
	
	SharedPreferences pref = null;
	public static final String AUTO_PREF_NAME = "AutoBrightnessPref";
	public static final String AUTO_PREF_ON_OFF = "ON";
	public static final boolean DEFAULT = true;
	private boolean mON = false;
	private Intent mIntent =  null;

	public AutoBrightnessSwitch(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		init(context);
	}
	
	public AutoBrightnessSwitch(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		init(context);
	}
	
	public AutoBrightnessSwitch(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		init(context);
	}

	void init(Context c)
	{
		mContext = c;
		pref = mContext.getSharedPreferences(AUTO_PREF_NAME, Context.MODE_PRIVATE);
		mON = pref.getBoolean(AUTO_PREF_ON_OFF, DEFAULT);
        mIntent = new Intent();
	    mIntent.setClass(mContext, AutoBrightnessService.class);

		setChecked(mON);
	}
	
	@Override
	protected void onClick() {
		// TODO Auto-generated method stub
        super.onClick();
        //setChecked(!isChecked());
	}

	@Override
	public void setChecked(boolean checked) {
		// TODO Auto-generated method stub
		pref.edit().putBoolean(AUTO_PREF_ON_OFF, checked).commit();
	    mContext.startService(mIntent);
		super.setChecked(checked);
	}

}
