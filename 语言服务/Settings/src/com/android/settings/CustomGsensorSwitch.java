package com.android.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.SystemProperties;
import android.content.SharedPreferences;
import android.preference.SwitchPreference;
import android.util.AttributeSet;

@SuppressLint("NewApi")
public class CustomGsensorSwitch extends SwitchPreference{
	
	Context mContext = null;
	
	SharedPreferences pref = null;
	public static final String CUSTOM_PREF_NAME = "CustomGsensorPref";
	public static final String CUSTOM_PREF_ON_OFF = "ON";
	public static final boolean DEFAULT = false;
	private boolean mON = false;

	public CustomGsensorSwitch(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		init(context);
	}
	
	public CustomGsensorSwitch(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		init(context);
	}
	
	public CustomGsensorSwitch(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		init(context);
	}

	void init(Context c)
	{
		mContext = c;
		pref = mContext.getSharedPreferences(CUSTOM_PREF_NAME, Context.MODE_PRIVATE);
		mON = pref.getBoolean(CUSTOM_PREF_ON_OFF, DEFAULT);

		setChecked(mON);
	}
	

	@Override
	public void setChecked(boolean checked) {
		// TODO Auto-generated method stub
		pref.edit().putBoolean(CUSTOM_PREF_ON_OFF, checked).commit();
		SystemProperties.set("persist.sys.gsensor",checked ? "on" : "off");
		super.setChecked(checked);
	}

}
