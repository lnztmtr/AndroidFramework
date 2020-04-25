package com.android.settings;


import android.os.Bundle;
import android.app.Activity;
import android.app.MboxOutputModeManager;
import android.content.Context;
import android.view.KeyEvent;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.TextView;
import android.view.Menu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.settings.R;


public class ScreenPositionManagerDisPlay extends Activity {
    private String TAG = "ScreenPositionManagerDisPlay";
    private static MboxOutputModeManager mMOMM = null;

    private int mCurrentLeft = 0;
    private int mCurrentTop = 0;
    private int mCurrentWidth = 0;
    private int mCurrentHeight = 0;
    private int mCurrentRight = 0;
    private int mCurrentBottom = 0;

    private int mPreLeft = 0;
    private int mPreTop = 0;
    private int mPreRight = 0;
    private int mPreBottom = 0;
    private int mPreWidth = 0;
    private int mPreHeight = 0;

    private  String mCurrentMode = null;
    private final int MAX_Height = 100;
    private final int MIN_Height = 80;
    private int mMaxRight = 0;
    private int mMaxBottom=0;
    private int offsetStep = 2;  // because 20% is too large ,so we divide a value to smooth the view
    private int screen_rate = MIN_Height;
    private ImageView img_num_hundred = null;
    private ImageView img_num_ten = null;
    private ImageView img_num_unit = null;
    private ImageView img_progress_bg;
    private ImageButton btn_position_zoom_out = null;
    private ImageButton btn_position_zoom_in = null;
    public static int Num[] = {
        R.drawable.ic_num0, R.drawable.ic_num1,
        R.drawable.ic_num2, R.drawable.ic_num3, R.drawable.ic_num4,
        R.drawable.ic_num5, R.drawable.ic_num6, R.drawable.ic_num7,
        R.drawable.ic_num8, R.drawable.ic_num9};
    public static int progressNum[] = { R.drawable.ic_per_81,
        R.drawable.ic_per_82, R.drawable.ic_per_83, R.drawable.ic_per_84,
        R.drawable.ic_per_85, R.drawable.ic_per_86, R.drawable.ic_per_87,
        R.drawable.ic_per_88, R.drawable.ic_per_89, R.drawable.ic_per_90,
        R.drawable.ic_per_91, R.drawable.ic_per_92, R.drawable.ic_per_93,
        R.drawable.ic_per_94, R.drawable.ic_per_95, R.drawable.ic_per_96,
        R.drawable.ic_per_97, R.drawable.ic_per_98, R.drawable.ic_per_99,
        R.drawable.ic_per_100 };

    public void initPostion() {
        mCurrentMode = mMOMM.getCurrentOutPutMode();
        Log.d(TAG,"initPostion(), mCurrentMode: " + mCurrentMode);
        int[] curPosition = mMOMM.getPosition(mCurrentMode);
        mPreLeft = curPosition[0];
        mPreTop = curPosition[1];
        mPreWidth = curPosition[2];
        mPreHeight = curPosition[3];
        mMaxRight = curPosition[4] - 1;
        mMaxBottom = curPosition[5] - 1;
        if((mMaxRight <= 0) || (mMaxBottom <= 0)) {
            Log.w(TAG, "mMaxRight or mMaxBottom is error");
        }
        mCurrentLeft = mPreLeft;
        mCurrentTop  = mPreTop;
        mCurrentWidth = mPreWidth; 
        mCurrentHeight= mPreHeight;
	 Log.d(TAG, "initPostion(), mPreLeft: " + mPreLeft + ", mPreTop: " + mPreTop
                    + ", mPreWidth: " + mPreWidth + ", mPreHeight: " + mPreHeight
                    + ", mMaxRight: " + mMaxRight + ", mMaxBottom: " + mMaxBottom);
    }

    public int getRateValue() {
        mCurrentMode = mMOMM.getCurrentOutPutMode();
        Log.d(TAG,"getRateValue(), mCurrentMode: " + mCurrentMode);
        int[] curPosition = mMOMM.getPosition(mCurrentMode);
        mMaxRight = curPosition[4] - 1;
        mMaxBottom = curPosition[5] - 1;
        Log.d(TAG,"getRateValue(), mMaxRight: " + mMaxRight + ", mMaxBottom: " + mMaxBottom);
        if((mMaxRight <= 0) || (mMaxBottom <= 0)) {
            Log.w(TAG, "mMaxRight or mMaxBottom is error");
        }
        int m = (100*2*offsetStep)*mPreLeft ;
        if(m == 0) {
            return 100;
        }
        int rate =  100 - m/(mMaxRight+1) - 1;
        Log.d(TAG,"getRateValue(), value: " + rate);
        return rate;
    }

    public void savePostion() {
        if( !isScreenPositionChanged())
            return;
        mMOMM.setPosition(mCurrentMode, mCurrentLeft, mCurrentTop, mCurrentWidth, mCurrentHeight);
    }

    public void zoomByPercent(int percent){
        if(percent > 100 ){
            percent = 100;
            return ;
        }

        if(percent < 80 ){
            percent = 80;
            return ;
        }

        mCurrentMode = mMOMM.getCurrentOutPutMode();
        Log.d(TAG,"zoomByPercent(), mCurrentMode: " + mCurrentMode);
        int[] curPosition = mMOMM.getPosition(mCurrentMode);
        mMaxRight = curPosition[4] - 1;
        mMaxBottom = curPosition[5] - 1;
        Log.d(TAG,"zoomByPercent(), mMaxRight: " + mMaxRight + ", mMaxBottom: " + mMaxBottom);
        if((mMaxRight <= 0) || (mMaxBottom <= 0)) {
            Log.w(TAG, "mMaxRight or mMaxBottom is error");
        }
        mCurrentLeft = (100-percent)*(mMaxRight)/(100*2*offsetStep);
        mCurrentTop  = (100-percent)*(mMaxBottom)/(100*2*offsetStep);
        mCurrentRight = mMaxRight - mCurrentLeft; 
        mCurrentBottom = mMaxBottom - mCurrentTop;
        mCurrentWidth = mCurrentRight - mCurrentLeft + 1;
        mCurrentHeight = mCurrentBottom - mCurrentTop + 1;
            Log.d(TAG,"====== zoomByPercent(), mCurrentLeft : " +mCurrentLeft);
            Log.d(TAG,"====== zoomByPercent(), mCurrentTop : " +mCurrentTop);
            Log.d(TAG,"====== zoomByPercent(), mCurrentRight : " +mCurrentRight);
            Log.d(TAG,"====== zoomByPercent(), mCurrentBottom : " +mCurrentBottom);
            Log.d(TAG,"====== zoomByPercent(), mCurrentWidth : " +mCurrentWidth);
            Log.d(TAG,"====== zoomByPercent(), mCurrentHeight : " +mCurrentHeight);

        setPosition(mCurrentLeft, mCurrentTop,mCurrentRight, mCurrentBottom, 0);
    }
    
    private void setPosition(int l, int t, int r, int b, int mode) {
        String str = "";
        int left =  l;
        int top =  t;
        int right =  r;
        int bottom =  b;
        int width = mCurrentWidth;
        int hight = mCurrentHeight;

        if(left < 0) {
            left = 0 ;
        }

        if(top < 0){
            top = 0 ;
        }
        right = Math.min(right, mMaxRight);
        bottom = Math.min(bottom, mMaxBottom);
        mMOMM.changeWindow(left, top, right, bottom);
        Log.d(TAG,"====== setPosition: " + left + "  " + top + "  " + right + "  " + bottom);  
    }

    public boolean isScreenPositionChanged() {
        if((mPreLeft == mCurrentLeft) && (mPreTop == mCurrentTop)
                && (mPreWidth == mCurrentWidth) && (mPreHeight == mCurrentHeight))
            return false;
        else
            return true;
    }
    private void showProgressUI(int step) {
        screen_rate = screen_rate + step;
        if(screen_rate >MAX_Height){
            screen_rate = MAX_Height;
        }
        if(screen_rate <MIN_Height){
            screen_rate = MIN_Height ;
        }
       Log.d(TAG,"===== showProgressUI() ,screen_rate="+ screen_rate);
        if (screen_rate ==100) {
            int hundred = Num[(int) screen_rate / 100];
            img_num_hundred.setVisibility(View.VISIBLE);
            img_num_hundred.setBackgroundResource(hundred);
            int ten = Num[(screen_rate -100)/10] ;
            img_num_ten.setBackgroundResource(ten);
            int unit = Num[(screen_rate -100)%10];
            img_num_unit.setBackgroundResource(unit);
            if (screen_rate - MIN_Height>= 0 && screen_rate - MIN_Height <= 20)
                img_progress_bg.setBackgroundResource(progressNum[screen_rate - MIN_Height-1]);
        } else if (screen_rate >= 10 && screen_rate <= 99) {
            img_num_hundred.setVisibility(View.GONE);
            int ten = Num[(int) (screen_rate / 10)];
            int unit = Num[(int) (screen_rate % 10)];
            img_num_ten.setBackgroundResource(ten);
            img_num_unit.setBackgroundResource(unit);
            if (screen_rate - MIN_Height >= 0 && screen_rate - MIN_Height <= 19)
                img_progress_bg.setBackgroundResource(progressNum[screen_rate - MIN_Height]);
        } else if (screen_rate >= 0 && screen_rate <= 9) {
            int unit = Num[screen_rate];
            img_num_unit.setBackgroundResource(unit);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.position_adjust);
	 /*final LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	  View mView = layoutInflater.inflate(R.xml.position_adjust, null);
	  mView.setVisibility(View.VISIBLE);
        addPreferencesFromResource(R.xml.position_adjust);*/
	 img_num_hundred = (ImageView) findViewById(R.id.img_num_hundred);
        img_num_ten = (ImageView)findViewById(R.id.img_num_ten);
        img_num_unit = (ImageView) findViewById(R.id.img_num_unit);
        img_progress_bg = (ImageView)findViewById(R.id.img_progress_bg);
        btn_position_zoom_out = (ImageButton)findViewById(R.id.btn_position_zoom_out);
        //btn_position_zoom_out.setOnClickListener(this);
        btn_position_zoom_in = (ImageButton)findViewById(R.id.btn_position_zoom_in);
        //btn_position_zoom_in.setOnClickListener(this);
	 TextView screen_tip_01 = (TextView)findViewById(R.id.screen_tip_01);
        //screen_tip_01.requestFocus();
	 mMOMM = (MboxOutputModeManager) this.getSystemService(Context.MBOX_OUTPUTMODE_SERVICE);
	 initPostion();
	 screen_rate = getRateValue();
	 showProgressUI(0);
    }

	@Override
	public void onStart() {
		super.onStart();		
	}
	
	@Override
	public void onStop() {
		super.onStop();	
		Log.d(TAG , "##onStop##");
		if(isScreenPositionChanged())
			savePostion();
	}


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }
    public boolean onKeyDown(int keyCode, KeyEvent event) {
		 if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                btn_position_zoom_in.setBackgroundResource(R.drawable.minus_unfocus);
                btn_position_zoom_out.setBackgroundResource(R.drawable.plus_focus);
                if(screen_rate < MAX_Height){
                    Log.d(TAG,"==== zoomIn ,screen_rate="+screen_rate);
                    showProgressUI(1);
                    zoomByPercent(screen_rate);
                }
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (screen_rate > MIN_Height) {
                    Log.d(TAG,"==== zoomOut,screen_rate="+screen_rate);
                    showProgressUI(-1);
                    zoomByPercent(screen_rate);
                }
                btn_position_zoom_in.setBackgroundResource(R.drawable.minus_focus);
                btn_position_zoom_out.setBackgroundResource(R.drawable.plus_unfocus);
            }
		return super.onKeyDown(keyCode, event);
    	
    }
}

