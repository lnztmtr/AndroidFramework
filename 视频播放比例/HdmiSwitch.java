package com.amlogic.HdmiSwitch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.os.SystemProperties;
import android.view.WindowManagerPolicy;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

public class HdmiSwitch extends Activity {

    private static final String TAG = "HdmiSwitch";

    //private static PowerManager.WakeLock mWakeLock;

    static {
        System.loadLibrary("hdmiswitchjni");
    }

    //public native static int scaleFrameBufferJni(int flag);	
    public native static int freeScaleSetModeJni(int mode);
    public native static int freeScaleForDisplay2Jni(int mode);
    public native static int DisableFreeScaleJni(int mode);
    public native static int EnableFreeScaleJni(int mode);
    public native static int DisableFreeScaleForDisplay2Jni(int mode);
    public native static boolean isExternalSinglePortraitDisplayJni();

    public static final String DISP_CAP_PATH = "/sys/class/amhdmitx/amhdmitx0/disp_cap";
    public static final String MODE_PATH = "/sys/class/display/mode";
    public static final String MODE_PATH_VOUT2 = "/sys/class/display2/mode";	
    public static final String AXIS_PATH = "/sys/class/display/axis";

    public static final String CODEC_REG = "/sys/devices/platform/soc-audio/codec_reg";
    public static final String SPK_MUTE = "12 b063";
    public static final String SPK_UNMUTE = "12 b073";

    public static final String DISP_MODE_PATH = "/sys/class/amhdmitx/amhdmitx0/disp_mode";
    public static final String HDMI_OFF = "aaa";

    public static final String BRIGHTNESS_PATH = "/sys/class/backlight/aml-bl/brightness";
    public static final String FB0_BLANK_PATH = "/sys/class/graphics/fb0/blank";	
    public static final String FB1_BLANK_PATH = "/sys/class/graphics/fb1/blank";

    public static final String DISABLE_VIDEO_PATH = "/sys/class/video/disable_video";
    public static final String REQUEST2XSCALE_PATH = "/sys/class/graphics/fb0/request2XScale";

    public static final String WINDOW_AXIS = "/sys/class/graphics/fb0/window_axis";
    //public static final String SCALE_FB0_PATH = "/sys/class/graphics/fb0/scale";
    //public static final String SCALE_FB1_PATH = "/sys/class/graphics/fb1/scale";

    private static String propHdmi480p = "ro.hdmi480p.enable";
    private static String propHdmi720p50Hz = "ro.hdmi720p50Hz.enable";
    private static String propHdmi720p60Hz = "ro.hdmi720p60Hz.enable";
    private static String propHdmi1080p24Hz = "ro.hdmi1080p24Hz.enable";
    private static String propHdmi1080p50Hz = "ro.hdmi1080p50Hz.enable";
    private static String propHdmi1080p60Hz = "ro.hdmi1080p60Hz.enable";

    private static final int CONFIRM_DIALOG_ID = 0;
    private static final int MAX_PROGRESS = 15;
    private static final int STOP_PROGRESS = -1;
    private int mProgress;
    private int mProgress2;
    private Handler mProgressHandler;

    private static final boolean HDMI_CONNECTED = true;
    private static final boolean HDMI_DISCONNECTED = false;
    private static boolean hdmi_stat = HDMI_DISCONNECTED;
    private static boolean hdmi_stat_old = HDMI_DISCONNECTED;

    private AlertDialog confirm_dialog;	
    private static String old_mode = "panel";

    private ListView lv;
    private static int mHdmiVppRotation = 0; 

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        /* set window size */
        WindowManager wm = getWindowManager();
        Display display = wm.getDefaultDisplay();
        LayoutParams lp = getWindow().getAttributes();
        if (display.getHeight() > display.getWidth()) {
            //lp.height = (int) (display.getHeight() * 0.5);
            lp.width = (int) (display.getWidth() * 1.0);       	
        } else {
            //lp.height = (int) (display.getHeight() * 0.75);
            lp.width = (int) (display.getWidth() * 0.5);            	
        }
        getWindow().setAttributes(lp);        
        mHdmiVppRotation = SystemProperties.getInt("ro.vpp.hdmi.rotation", 0);

        //        /* close button listener */
        //        Button btn_close = (Button) findViewById(R.id.title_btn_right);  
        //        btn_close.setOnClickListener(new OnClickListener() {
        //			public void onClick(View v) {				
        //				finish();
        //			}        	
        //        }); 

        /* check driver interface */        
        TextView tv = (TextView) findViewById(R.id.hdmi_state_str);        
        File file = new File(DISP_CAP_PATH);
        if (!file.exists()) {
            tv.setText(getText(R.string.driver_api_err) + "[001]");
            return;
        }
        file = new File(MODE_PATH);
        if (!file.exists()) {
            tv.setText(getText(R.string.driver_api_err) + "[010]");
            return;
        }
        file = new File(AXIS_PATH);
        if (!file.exists()) {
            tv.setText(getText(R.string.driver_api_err) + "[100]");
            return;
        }


        /* update hdmi_state_str*/
        if (isHdmiConnected()){
            tv.setText(getText(R.string.hdmi_state_str1));
        }else{
            tv.setText(getText(R.string.hdmi_state_str2));
        }

        //        /* update hdmi_info_str*/
        //        TextView tv2 = (TextView) findViewById(R.id.hdmi_info_str); 
        //        if (getCurMode().equals("panel"))
        //        	tv2.setVisibility(View.GONE);
        //        else {
        //        	tv2.setVisibility(View.VISIBLE);
        //        	tv2.setText(getText(R.string.hdmi_info_str1));
        //        }

        /* setup video mode list */
        lv = (ListView) findViewById(R.id.listview); 
        SimpleAdapter adapter = new SimpleAdapter(this,getListData(),R.layout.list_item,        		
                                                                    new String[]{"item_text","item_img"},        		
                                                                    new int[]{R.id.item_text,R.id.item_img});        		
        lv.setAdapter(adapter);

        /* mode select listener */
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                Map<String, Object> item = (Map<String, Object>)parent.getItemAtPosition(pos);
                if (item.get("item_img").equals(R.drawable.item_img_unsel)) {					
                    old_mode = getCurMode();
                    //					setMode((String)item.get("mode"));							
                    //					notifyModeChanged();
                    //					updateListDisplay();					
                    //					
                    //					if (!getCurMode().equals("panel"))
                    //						showDialog(CONFIRM_DIALOG_ID);
                    //					else
                    //						finish();

                    if (SystemProperties.getBoolean("ro.vout.dualdisplay2", false)
                        || SystemProperties.getBoolean("ro.vout.dualdisplay3", false)) {
                        String isCameraBusy = SystemProperties.get("camera.busy", "0");
                        if (!isCameraBusy.equals("0")) {
                            Log.w(TAG, "setDualDisplay, camera is busy");
                            Toast.makeText(HdmiSwitch.this,
                            getText(R.string.Toast_msg_camera_busy),
                            Toast.LENGTH_LONG).show(); 
                            return;
                        }                                                         
                    }
                    final String mode = (String)item.get("mode");

                    Log.v(TAG,"handle creat mode "+mode);
                    new Thread("setMode") {
                        @Override
                        public void run() {
                            Log.v(TAG,"handle creat mode "+mode);
                            setMode(mode);
                            mProgressHandler.sendEmptyMessage(2);
                        }
                    }.start();  						
                }
            }        	
        });    

        /* progress handler*/
        mProgressHandler = new HdmiSwitchProgressHandler(); 

    }

    /** onResume() */
    @Override
    public void onResume() {
        super.onResume();    

        /* check driver interface */        
        File file = new File(HdmiSwitch.DISP_CAP_PATH);
        if (!file.exists()) {        	
            return;
        }
        file = new File(HdmiSwitch.MODE_PATH);
        if (!file.exists()) {        	
            return;
        }
        file = new File(HdmiSwitch.AXIS_PATH);
        if (!file.exists()) {        	
            return;
        }

        mProgress2 = 0;
        mProgressHandler.sendEmptyMessageDelayed(1, 100); 
    }

    /** onPause() */
    @Override
    public void onPause() {
        super.onPause();

        hdmi_stat_old = isHdmiConnected(); 
        mProgress = STOP_PROGRESS;  
        mProgress2 = STOP_PROGRESS;
    }


    /** Confirm Dialog */
    @Override
    protected Dialog onCreateDialog(int id) {
        if (id == CONFIRM_DIALOG_ID) {
                confirm_dialog =  new AlertDialog.Builder(HdmiSwitch.this)
                //.setIcon(R.drawable.dialog_icon)
                .setTitle(R.string.dialog_title)
                .setPositiveButton(R.string.dialog_str_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {   
                        mProgress = STOP_PROGRESS;   
                        finish();
                        /* User clicked OK so do some stuff */
                    }
                })
                .setNegativeButton(R.string.dialog_str_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mProgress = STOP_PROGRESS;
                        final String mode = old_mode;

                        Log.v(TAG,"handle click mode "+mode);
                        new Thread("setMode") {
                            @Override
                            public void run() {
                                Log.v(TAG,"handle click mode "+mode);
                                setMode(mode);
                                mProgressHandler.sendEmptyMessage(3);
                            }
                        }.start(); 
                        /* User clicked Cancel so do some stuff */                    	
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {						
                        mProgress = STOP_PROGRESS;  										
                    }
                })				
                .create();  

                return confirm_dialog;
        }

        return null;    	
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        if (id == CONFIRM_DIALOG_ID) {
                WindowManager wm = getWindowManager();
                Display display = wm.getDefaultDisplay();
                LayoutParams lp = dialog.getWindow().getAttributes();
                if (display.getHeight() > display.getWidth()) {            	
                    lp.width = (int) (display.getWidth() * 1.0);       	
                } else {        		
                    lp.width = (int) (display.getWidth() * 0.5);            	
                }
                dialog.getWindow().setAttributes(lp);

                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE)
                .setText(getText(R.string.dialog_str_cancel) 
                    + " (" + MAX_PROGRESS + ")");            

                mProgress = 0;	                
                mProgressHandler.sendEmptyMessageDelayed(0, 1000);
        }
    }  

    /** getListData */
    private List<Map<String, Object>> getListData() {    	
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();	 

        for (String modeStr : getAllMode()) {
            Map<String, Object> map = new HashMap<String, Object>();  
            map.put("mode", modeStr);
            map.put("item_text", getText((Integer)MODE_STR_TABLE.get(modeStr)));
            if (modeStr.equals(getCurMode())){
                map.put("item_img", R.drawable.item_img_sel);
            }else{
                map.put("item_img", R.drawable.item_img_unsel);
            }
            list.add(map);
        }

        return list;
    } 

    /** updateListDisplay */
    private void updateListDisplay() {
        Map<String, Object> list_item;
        
        for (int i = 0; i < lv.getAdapter().getCount(); i++) {
            list_item = (Map<String, Object>)lv.getAdapter().getItem(i);
            if (list_item.get("mode").equals(getCurMode())){
                list_item.put("item_img", R.drawable.item_img_sel);
            }else{
                list_item.put("item_img", R.drawable.item_img_unsel);
            }
        }  
        ((BaseAdapter) lv.getAdapter()).notifyDataSetChanged();  
    }

    /** updateActivityDisplay */
    private void updateActivityDisplay() {
        /* update hdmi_state_str*/
        TextView tv = (TextView) findViewById(R.id.hdmi_state_str);
        if (isHdmiConnected()){
            tv.setText(getText(R.string.hdmi_state_str1));
        }else{
            tv.setText(getText(R.string.hdmi_state_str2));
        }

        /* update video mode list */
        lv = (ListView) findViewById(R.id.listview);        
        SimpleAdapter adapter = new SimpleAdapter(this,getListData(),R.layout.list_item,        		
        new String[]{"item_text","item_img"},        		
        new int[]{R.id.item_text,R.id.item_img});        		
        lv.setAdapter(adapter);    	

        ((BaseAdapter) lv.getAdapter()).notifyDataSetChanged();  
    }

    /** check hdmi connection*/
    public static boolean isHdmiConnected() {    
        boolean plugged = false;
        
        // watch for HDMI plug messages if the hdmi switch exists
        if (new File("/sys/devices/virtual/switch/hdmi/state").exists()) {	
            final String filename = "/sys/class/switch/hdmi/state";
            FileReader reader = null;
            try {
                reader = new FileReader(filename);
                char[] buf = new char[15];
                int n = reader.read(buf);
                if (n > 1) {
                    plugged = 0 != Integer.parseInt(new String(buf, 0, n-1));
                }
            } catch (IOException ex) {
                Log.w(TAG, "Couldn't read hdmi state from " + filename + ": " + ex);
            } catch (NumberFormatException ex) {
                Log.w(TAG, "Couldn't read hdmi state from " + filename + ": " + ex);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ex) {
                    }
                }
            }
        }

        return plugged;
    }

    /** get all support mode*/
    private List<String> getAllMode() {
        List<String> list = new ArrayList<String>();
        String modeStr;

        WindowManager mWm = (WindowManager)HdmiSwitch.this.getSystemService(Context.WINDOW_SERVICE);
        Display display = mWm.getDefaultDisplay();
        int mWScreenx = display.getWidth();
        int mWScreeny = display.getHeight();
        boolean skip480p = false;
        if(((mWScreenx > 1920) && (mWScreeny > 1080)) || (( mWScreeny > 1920) && (mWScreenx > 1080))) {
            skip480p = true;
        }

        if (SystemProperties.getBoolean("ro.vout.dualdisplay", false)) {
            if(!skip480p){
                list.add("480p");
            }
            list.add("720p50hz");
            list.add("720p");
            list.add("1080p24hz");
            list.add("1080p50hz");
            list.add("1080p");
            return list;
        }

        if (SystemProperties.getBoolean("ro.vout.dualdisplay4", false)) {
            list.add("null");  
        } else {    
            list.add("panel");  
        }   	

        //list.add("480i");
        if(SystemProperties.getBoolean(propHdmi480p, true)){
            if(!skip480p)
            list.add("480p");
        }
        if(SystemProperties.getBoolean(propHdmi720p50Hz, false)) {
            list.add("720p50hz");
        }
        if(SystemProperties.getBoolean(propHdmi720p60Hz, true)) {
            list.add("720p");
        }
        if(SystemProperties.getBoolean(propHdmi1080p24Hz, false)) {
            list.add("1080p24hz");
        }
        if(SystemProperties.getBoolean(propHdmi1080p50Hz, false)) {
            list.add("1080p50hz");
        }
        if(SystemProperties.getBoolean(propHdmi1080p60Hz, true)) {
            list.add("1080p");
        }
        return list;
    }

    /** get current mode*/
    public static String getCurMode() {
        String modeStr;
        if (SystemProperties.getBoolean("ro.vout.dualdisplay4", false)) {
            try {
                BufferedReader reader2 = new BufferedReader(new FileReader(MODE_PATH_VOUT2), 32);
                try {
                    modeStr = reader2.readLine();  
                } finally {
                    reader2.close();
                } 		
                return ((modeStr == null) || modeStr.equals(""))? "null" : modeStr;   	

            } catch (IOException e) { 
                Log.e(TAG, "IO Exception when read: " + MODE_PATH_VOUT2, e);
                return "null";
            }    	    
        }    	

        if (SystemProperties.getBoolean("ro.vout.dualdisplay", false)) {
            try {
                BufferedReader reader2 = new BufferedReader(new FileReader(MODE_PATH_VOUT2), 32);
                try {
                    modeStr = reader2.readLine();  
                } finally {
                    reader2.close();
                }    		
                return (modeStr == null)? "720p" : modeStr;   	

            } catch (IOException e) { 
                Log.e(TAG, "IO Exception when read: " + MODE_PATH_VOUT2, e);
                return "720p";
            }    	    
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(MODE_PATH), 32);
            try {
                modeStr = reader.readLine();  
            } finally {
                reader.close();
            }    		
            return (modeStr == null)? "panel" : modeStr;   	

        } catch (IOException e) { 
            Log.e(TAG, "IO Exception when read: " + MODE_PATH, e);
            return "panel";
        }    	
    }

    /** sendTvOutIntent **/
    private void sendTvOutIntent( boolean plugged ) {
        Intent intent = new Intent(WindowManagerPolicy.ACTION_HDMI_PLUGGED);
        intent.putExtra(WindowManagerPolicy.EXTRA_HDMI_PLUGGED_STATE, plugged);
        sendStickyBroadcast(intent);
    }

    private void notifyModeChanged() {
        if (SystemProperties.getBoolean("ro.vout.dualdisplay", false)){
            return;
        }

        if (SystemProperties.getBoolean("ro.vout.dualdisplay4", false)) {
            sendTvOutIntent(getCurMode().equals("null") ? false : true);
            return;
        }	        

        if (getCurMode().equals("panel")){
            sendTvOutIntent(false);
        }else{
            sendTvOutIntent(true);
        }
    }

    /** set mode */
    public static int setMode(String modeStr) {   
        Log.i(TAG, "Set mode 2 " + modeStr);	
        if (SystemProperties.getBoolean("ro.vout.dualdisplay4", false)) {
            if (modeStr.equals("panel")){
                modeStr = "null";
            }
                
            if (!modeStr.equals("null")) {
                if (!isHdmiConnected()){
                    return 0;
                }
            }
            if (modeStr.equals(getCurMode())){
                return 0;  
            }
                	    
            setDisplay2Mode(modeStr);
            return 0;
        }    	
    	
        if (!modeStr.equals("panel")) {
            if (!isHdmiConnected()){
            	return 0;
            }
        }else if (!isExternalSinglePortraitDisplayJni()){
            //if mode is reset to 'panel',we need to reset window_axis to zero avoid libplayer changing video/axis 
            writeSysfs(WINDOW_AXIS,"0 0 0 0");
        }
        if (modeStr.equals(getCurMode())){
            return 0; 
        }
    	
        if (SystemProperties.getBoolean("ro.vout.dualdisplay", false)) {    		
            try {
                BufferedWriter writer2 = new BufferedWriter(new FileWriter(MODE_PATH_VOUT2), 32);
                try {
                    writer2.write(modeStr + "\r\n");    			
                } finally {
                    writer2.close();
                }     		
            } catch (IOException e) { 
                Log.e(TAG, "IO Exception when write: " + MODE_PATH_VOUT2, e);    		
                return 1;
            }    	        
            return 0;
        }

        String briStr = "128";
        if (modeStr.equals("panel")) {
            disableHdmi();
            briStr = getBrightness();
            setBrightness("0");
            //setFb0Blank("1");
            disableVideo(true);
        }
            
        boolean playerRunning = SystemProperties.getBoolean("vplayer.playing", false);
        boolean playerExitWhenSwitch = SystemProperties.getBoolean("ro.vout.player.exit", true);
        boolean freescaleOff = !playerExitWhenSwitch && playerRunning;
        if (SystemProperties.getBoolean("ro.module.dualscaler", false)){
            freescaleOff = false; //for android 4.3,we use new freescale mode ,and there is no need to disabele freescale when videoplay is running
        }

        //do free_scale    		
        if (modeStr.equals("panel")) {
            //setFb0Blank("1");
            freeScaleSetModeJni(0);
            //nap(1);
            disableVideo(false);
            setBrightness(briStr);
            if (!isExternalSinglePortraitDisplayJni()){
                writeSysfs(REQUEST2XSCALE_PATH, "2");
            }
            //setFb0Blank("0");
        }else if (modeStr.equals("480p")) {
            setFb0Blank("1");	
            if (freescaleOff){
                DisableFreeScaleJni(1);
            }else{
                freeScaleSetModeJni(1);  
            }
        } else if (modeStr.equals("720p50hz")) {
            if (freescaleOff){
                DisableFreeScaleJni(2);
            }else{
                freeScaleSetModeJni(2);  
            }
        } else if (modeStr.equals("720p")) {
            if (freescaleOff){
                DisableFreeScaleJni(3);
            }else{    		    
                freeScaleSetModeJni(3);  
            }
        } else if (modeStr.equals("1080i")) {
            if (freescaleOff){
                DisableFreeScaleJni(4);
            }else{
                freeScaleSetModeJni(4);  
            }
        } else if (modeStr.equals("1080p24hz")) {
            if (freescaleOff){
                DisableFreeScaleJni(5);
            }else{
                freeScaleSetModeJni(5);  
            }
        } else if (modeStr.equals("1080p50hz")) {
            if (freescaleOff){
                DisableFreeScaleJni(6);
            }else{
                freeScaleSetModeJni(6);  
            }
        } else if (modeStr.equals("1080p")) {
            if (freescaleOff){
                DisableFreeScaleJni(7);
            }else{
                freeScaleSetModeJni(7);  
            }
        }

        return 0;
    }

    /**
    * Sleep for a period of time.
    * @param secs the number of seconds to sleep
    */
    private static void nap(int secs) {
        try {
            Thread.sleep(secs * 1000);
        } catch (InterruptedException ignore) {
        }
    }
    private static void napMs(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignore) {
        }
    } 
        
    /** disable Hdmi*/
    public static int disableHdmi() {
        //Log.i(TAG, "--disableHdmi");	
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(DISP_MODE_PATH), 32);
            try {
                writer.write(HDMI_OFF + "\r\n");
            } finally {
                writer.close();
            }    		
            return 0;

        } catch (IOException e) { 
            Log.e(TAG, "IO Exception when write: " + DISP_MODE_PATH, e);
            return 1;
        }    	
    }     
        
    /** set axis*/
    public static int setAxis(String axisStr) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(AXIS_PATH), 32);
            try {
                writer.write(axisStr + "\r\n");
            } finally {
                writer.close();
            }    		
            return 0;

        } catch (IOException e) { 
            Log.e(TAG, "IO Exception when write: " + AXIS_PATH, e);
            return 1;
        }    	
    }    

    private static final String VIDEO2_CTRL_PATH = "/sys/class/video2/clone";
    private static final String VFM_CTRL_PATH = "/sys/class/vfm/map";
    private static final String VIDEO2_FRAME_RATE_PATH = "/sys/module/amvideo2/parameters/clone_frame_rate";
    private static final String VIDEO2_FRAME_WIDTH_PATH = "/sys/module/amvideo2/parameters/clone_frame_scale_width";
    private static final String VIDEO2_SCREEN_MODE_PATH = "/sys/class/video2/screen_mode";
    private static final String VIDEO2_ZOOM_PATH = "/sys/class/video2/zoom";

    private static final String FB2_BLANK_PATH = "/sys/class/graphics/fb2/blank";
    private static final String FB2_CLONE_PATH = "/sys/class/graphics/fb2/clone";
    private static final String REG_PATH = "/sys/class/display2/venc_mux";    

    private static int writeSysfs(String path, String val) { 
        if (!new File(path).exists()) {
            Log.e(TAG, "File not found: " + path);
            return 1; 
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path), 64);
            try {
                writer.write(val);
            } finally {
                writer.close();
            }    		
            return 0;

        } catch (IOException e) { 
            Log.e(TAG, "IO Exception when write: " + path, e);
            return 1;
        }                
    }

    private static String readSysfs(String path) {
        String val = null;

        if (!new File(path).exists()) {
            Log.e(TAG, "File not found: " + path);
            return null; 
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(path), 64);
            try {
            val = reader.readLine();
            } finally {
            reader.close();
            }    		
            return val;

        } catch (IOException e) { 
            Log.e(TAG, "IO Exception when write: " + path, e);
            return null;
        }                 
    }

    private void setDualDisplay(boolean hdmiPlugged) {
        String isCameraBusy = SystemProperties.get("camera.busy", "0");

        if (!isCameraBusy.equals("0")) {
            Log.w(TAG, "setDualDisplay, camera is busy");
            return;
        }    
        if (SystemProperties.getBoolean("ro.vout.dualdisplay2", false)) {        
            if (hdmiPlugged) {
                writeSysfs(VIDEO2_CTRL_PATH, "0");
                writeSysfs(VFM_CTRL_PATH, "rm default_ext");
                if(mHdmiVppRotation > 0){
                    writeSysfs(VFM_CTRL_PATH, "add default_ext vdin freescale amvideo2");
                }else{
                    writeSysfs(VFM_CTRL_PATH, "add default_ext vdin amvideo2");
                }
                writeSysfs(VIDEO2_CTRL_PATH, "1");

                if (getCurMode().equals("720p50hz") || getCurMode().equals("720p")) {
                    writeSysfs(VIDEO2_FRAME_WIDTH_PATH, "640");
                } else if (getCurMode().equals("1080p24hz") ||getCurMode().equals("1080p50hz") || getCurMode().equals("1080p")) {
                    writeSysfs(VIDEO2_FRAME_WIDTH_PATH, "800");
                } else {
                    writeSysfs(VIDEO2_FRAME_WIDTH_PATH, "0");
                }
                writeSysfs(VIDEO2_ZOOM_PATH, "105");

                if (getDualDisplayState() == 1) {
                    writeSysfs(VIDEO2_SCREEN_MODE_PATH, "1");
                    writeSysfs(MODE_PATH_VOUT2, "null");
                    writeSysfs(MODE_PATH_VOUT2, "panel");
                }
            } else {
                writeSysfs(VIDEO2_CTRL_PATH, "0");
                writeSysfs(VFM_CTRL_PATH, "rm default_ext");
                writeSysfs(VFM_CTRL_PATH, "add default_ext vdin vm amvideo");
                writeSysfs(MODE_PATH_VOUT2, "null");
            } 
        } else if (SystemProperties.getBoolean("ro.vout.dualdisplay3", false)) {
            if (SystemProperties.getBoolean("ro.module.dualscaler", false)){
                if (hdmiPlugged && (getDualDisplayState() == 1)) {
                    writeSysfs(FB2_BLANK_PATH, "1");
                    writeSysfs(FB2_CLONE_PATH, "1");
                    //writeSysfs(MODE_PATH_VOUT2, "null");
                    //writeSysfs(MODE_PATH_VOUT2, "panel");
                    //writeSysfs(REG_PATH, "2");
                    writeSysfs(FB2_BLANK_PATH, "0");
                } else {
                    writeSysfs(FB2_BLANK_PATH, "1");
                    writeSysfs(FB2_CLONE_PATH, "0");
                    writeSysfs(MODE_PATH_VOUT2, "null");
                    //writeSysfs(REG_PATH, "0");            
                }   
            }     
        }
    }

    private int getDualDisplayState() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.HDMI_DUAL_DISP, 1);
    }    
        
    public static void setDualDisplayStatic(boolean hdmiPlugged, boolean dualEnabled) {
        String isCameraBusy = SystemProperties.get("camera.busy", "0");
        int hdmiVppRotation = SystemProperties.getInt("ro.vpp.hdmi.rotation", 0);

        if (!isCameraBusy.equals("0")) {
            Log.w(TAG, "setDualDisplay, camera is busy");
            return;
        }    
        if (SystemProperties.getBoolean("ro.vout.dualdisplay2", false)) { 
            if (hdmiPlugged) {
                writeSysfs(VIDEO2_CTRL_PATH, "0");
                writeSysfs(VFM_CTRL_PATH, "rm default_ext");            
                if(hdmiVppRotation > 0){
                    writeSysfs(VFM_CTRL_PATH, "add default_ext vdin freescale amvideo2");
                }else{
                    writeSysfs(VFM_CTRL_PATH, "add default_ext vdin amvideo2");
                }
                writeSysfs(VIDEO2_CTRL_PATH, "1");

                if (getCurMode().equals("720p50hz") || getCurMode().equals("720p")) {
                    writeSysfs(VIDEO2_FRAME_WIDTH_PATH, "640");
                } else if (getCurMode().equals("1080p24hz") || getCurMode().equals("1080p50hz") || getCurMode().equals("1080p")) {
                    writeSysfs(VIDEO2_FRAME_WIDTH_PATH, "800");
                } else {
                    writeSysfs(VIDEO2_FRAME_WIDTH_PATH, "0");
                }
                writeSysfs(VIDEO2_ZOOM_PATH, "105");

                if (dualEnabled) {
                    writeSysfs(VIDEO2_SCREEN_MODE_PATH, "1");
                    writeSysfs(MODE_PATH_VOUT2, "null");
                    writeSysfs(MODE_PATH_VOUT2, "panel");
                }
            } else {
                writeSysfs(VIDEO2_CTRL_PATH, "0");
                writeSysfs(VFM_CTRL_PATH, "rm default_ext");
                writeSysfs(VFM_CTRL_PATH, "add default_ext vdin vm amvideo");
                writeSysfs(MODE_PATH_VOUT2, "null");
            } 
        } else if (SystemProperties.getBoolean("ro.vout.dualdisplay3", false)) {
            if (hdmiPlugged && dualEnabled) {
                if(SystemProperties.getBoolean("ro.screen.portrait", false)==false){
                    writeSysfs(FB2_BLANK_PATH, "1");
                    writeSysfs(FB2_CLONE_PATH, "1");
                    writeSysfs(MODE_PATH_VOUT2, "null");
                    writeSysfs(MODE_PATH_VOUT2, "panel");
                    //writeSysfs(REG_PATH, "2");
                    writeSysfs(FB2_BLANK_PATH, "0");              
                }else{
                    Log.v(TAG,"setDualDisplayStatic 2 connect");
                }

            } else {
                if(SystemProperties.getBoolean("ro.screen.portrait", false)==false)
                {
                    writeSysfs(FB2_BLANK_PATH, "1");
                    writeSysfs(FB2_CLONE_PATH, "0");
                    writeSysfs(MODE_PATH_VOUT2, "null");
                    //writeSysfs(REG_PATH, "0");     
                }else
                {
                    Log.v(TAG,"setDualDisplayStatic  2 disconnect");
                }
            }        
        } 	   	
    }    
        
    public static void setVout2OffStatic() {
        if (SystemProperties.getBoolean("ro.vout.dualdisplay2", false))
            setFb0Blank("1");
        else if (SystemProperties.getBoolean("ro.vout.dualdisplay3", false))
            writeSysfs(FB2_BLANK_PATH, "1");

        //writeSysfs(MODE_PATH_VOUT2, "null");
    }    
        
    private static final String FB2_ANGLE = "/sys/class/graphics/fb2/angle";
    private static final String FB2_MODE = MODE_PATH_VOUT2;
    private static final String FB2_VENC = "/sys/class/display2/venc_mux";
    private static final String DISABLE_VIDEO = "/sys/class/video/disable_video";
    private static final String VIDEO_DEV = "/sys/module/amvideo/parameters/cur_dev_idx";
    private static final String PPMGR_DISP = "sys/class/ppmgr/disp";
    private static final String PPMGR_ANGLE = "sys/class/ppmgr/angle";    
      
    public static void setDisplay2Mode(String mode) {
        Log.d(TAG, "setDisplay2Mode---------------------" + mode);
        boolean verPanel = SystemProperties.getBoolean("ro.vout.dualdisplay4.ver-panel", false);
        boolean verPanelReverse = SystemProperties.getBoolean("ro.ver-panel.reverse", false);

        if (!mode.equals("null")) {
            writeSysfs(VFM_CTRL_PATH, "add dual_display osd_ext amvideo4osd");

            writeSysfs(FB2_BLANK_PATH, "1");
            writeSysfs(DISABLE_VIDEO, "1");

            writeSysfs(PPMGR_DISP, getFbSize(1)); 
            if (verPanel) {                
                writeSysfs(PPMGR_ANGLE, "0");
            } 

            writeSysfs(FB2_CLONE_PATH, "0");
            writeSysfs(FB2_MODE, mode);
            freeScaleForDisplay2(mode);
            if (verPanel) {
                if (verPanelReverse)
                    writeSysfs(FB2_ANGLE, "1");
                else            
                    writeSysfs(FB2_ANGLE, "3");
            } else {
                writeSysfs(FB2_ANGLE, "4");
            }
            writeSysfs(FB2_VENC, "0x8");
            writeSysfs(FB2_CLONE_PATH, "1");

            writeSysfs(VIDEO_DEV, "1");                       
            napMs(500);
            writeSysfs(DISABLE_VIDEO, "2");
            
            writeSysfs(FB2_BLANK_PATH, "0");

        } else {
            writeSysfs(FB2_BLANK_PATH, "1");          
            writeSysfs(DISABLE_VIDEO, "1");  

            writeSysfs(PPMGR_DISP, getFbSize(0)); 
            if (verPanel) {
                if (verPanelReverse)                
                    writeSysfs(PPMGR_ANGLE, "3");
                else
                    writeSysfs(PPMGR_ANGLE, "1");
            }

            writeSysfs(FB2_CLONE_PATH, "0");
            writeSysfs(FB2_MODE, "null");
            writeSysfs(FB2_VENC, "0x0");
            freeScaleForDisplay2(mode);             

            writeSysfs(VIDEO_DEV, "0");
            napMs(300);
            writeSysfs(DISABLE_VIDEO, "2");            
        }
    }

    private static void freeScaleForDisplay2(String mode) {
        boolean playerRunning = SystemProperties.getBoolean("vplayer.playing", false);
        boolean playerExitWhenSwitch = SystemProperties.getBoolean("ro.vout.player.exit", true);
        boolean freescaleOff = !playerExitWhenSwitch && playerRunning;        

        if (mode.equals("null")){
            freeScaleForDisplay2Jni(0);
        } else if (mode.equals("480p")) {
            if(freescaleOff){
                DisableFreeScaleForDisplay2Jni(1);
            }else{
                freeScaleForDisplay2Jni(1);
            }
        } else if (mode.equals("720p50hz")) {
            if(freescaleOff){
                DisableFreeScaleForDisplay2Jni(2);
            }else{
                freeScaleForDisplay2Jni(2);
            }
        } else if (mode.equals("720p")) {
            if(freescaleOff)
                DisableFreeScaleForDisplay2Jni(3);
            else
                freeScaleForDisplay2Jni(3);
        } else if (mode.equals("1080p24hz")) {
            if(freescaleOff){
                DisableFreeScaleForDisplay2Jni(5);
            }else{
                freeScaleForDisplay2Jni(5);
            }
        } else if (mode.equals("1080p50hz")) {
            if(freescaleOff){
                DisableFreeScaleForDisplay2Jni(6);
            }else{
                freeScaleForDisplay2Jni(6);
            }
        } else if (mode.equals("1080p")) {
            if(freescaleOff){
                DisableFreeScaleForDisplay2Jni(7);
            }else{
                freeScaleForDisplay2Jni(7);
            }
        }
    }
        
    private static String getFbSize(int fb) {
        String val = null;

        if (fb > 0) {
            val = readSysfs("/sys/class/graphics/fb2/virtual_size");
        } else {
            val = readSysfs("/sys/class/graphics/fb0/virtual_size");
        }

        if (val != null) {
            String widthStr = null;
            String heightStr = null;
            int width, height;
            widthStr = val.split(",")[0];
            heightStr = val.split(",")[1];
            if (widthStr != null && heightStr != null) {
                width = Integer.parseInt(widthStr);
                height = Integer.parseInt(heightStr);
                return new String("" + width + " " + (height/2));
            }
        }
        return null;
    }


    /** video layer control */
    private static int disableVideo(boolean disable) {
        //Log.i(TAG, "---disableVideo: " + disable);
        File file = new File(DISABLE_VIDEO_PATH);
        if (!file.exists()) {        	
            return 0;
        }    	
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(DISABLE_VIDEO_PATH), 32);
            try {
                if (disable)
                    writer.write("1");
                else
                    writer.write("2");
            } finally {
                writer.close();
            }    		
            return 0;

        } catch (IOException e) { 
            Log.e(TAG, "IO Exception when write: " + DISABLE_VIDEO_PATH, e);
            return 1;
        }    	
    }    

    /** set osd blank*/    
    public static int setFb0Blank(String blankStr) {
        //Log.i(TAG, "----setFb0Blank: " + blankStr);
        File file = new File(FB0_BLANK_PATH);
        if (!file.exists()) {        	
            return 0;
        }    	
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(FB0_BLANK_PATH), 32);
            try {
                writer.write(blankStr);
            } finally {
                writer.close();
            }    		
            return 0;

        } catch (IOException e) { 
            Log.e(TAG, "IO Exception when write: " + FB0_BLANK_PATH, e);
            return 1;
        }    	
    }

    private static int setFb1Blank(String blankStr) {
        //Log.i(TAG, "setFb1Blank: " + blankStr);
        File file = new File(FB1_BLANK_PATH);
        if (!file.exists()) {        	
            return 0;
        }    	
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(FB1_BLANK_PATH), 32);
            try {
                writer.write(blankStr);
            } finally {
                writer.close();
            }    		
            return 0;

        } catch (IOException e) { 
            Log.e(TAG, "IO Exception when write: " + FB1_BLANK_PATH, e);
            return 1;
        }    	
    }    
            
    /** set brightness*/
    public static int setBrightness(String briStr) {
        //Log.i(TAG, "---setBrightness: " + briStr);
        File file = new File(BRIGHTNESS_PATH);
        if (!file.exists()) {        	
            return 0;
        }    	
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(BRIGHTNESS_PATH), 32);
            try {
                writer.write(briStr);
            } finally {
                writer.close();
            }    		
            return 0;

        } catch (IOException e) { 
            Log.e(TAG, "IO Exception when write: " + BRIGHTNESS_PATH, e);
            return 1;
        }    	
    }

    /** get brightness*/
    public static String getBrightness() {
        String briStr = "128";
        //Log.i(TAG, "--getBrightness");
        File file = new File(BRIGHTNESS_PATH);
        if (!file.exists()) {        	
            return briStr;
        }     	
        try {
            BufferedReader reader = new BufferedReader(new FileReader(BRIGHTNESS_PATH), 32);
            try {
                briStr = reader.readLine();  
            } finally {
                reader.close();
            }    		
            return (briStr == null)? "128" : briStr;   	

        } catch (IOException e) { 
            Log.e(TAG, "IO Exception when read: " + BRIGHTNESS_PATH, e);
            return "128";
        }    	
    }

    /** set Audio*/
    public static int setAudio(String audioStr) {
        File file = new File(CODEC_REG);
        if (!file.exists()) {        
            //Log.w(TAG, "File does not exist: " + CODEC_REG);
            return 0;  
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(CODEC_REG), 64);
            try {
                writer.write(audioStr);        			
            } finally {
                writer.close();
            }    		
            return 0;

        } catch (IOException e) { 
            Log.e(TAG, "IO Exception when write: " + CODEC_REG, e);
            return 1;
        }    	
    }    

    /** process handler */
    private class HdmiSwitchProgressHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:		// confirm dialog 
                    if (mProgress == STOP_PROGRESS) 
                        return;                     

                    if (mProgress >= MAX_PROGRESS) {  
                        //                	setMode(old_mode);
                        //                	notifyModeChanged();
                        //                	updateListDisplay();                 	
                        final String mode = old_mode;
                        Log.v(TAG,"handle message mode "+mode);
                        new Thread("setMode") {
                            @Override
                            public void run() {
                                Log.v(TAG,"handle message mode "+mode);
                                setMode(mode);
                                mProgressHandler.sendEmptyMessage(3);
                            }
                        }.start();                 	

                        confirm_dialog.dismiss();
                    } else {
                        mProgress++;                    
                        confirm_dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setText(getText(R.string.dialog_str_cancel) 
                            + " (" + (MAX_PROGRESS - mProgress) + ")");

                        mProgressHandler.sendEmptyMessageDelayed(0, 1000);                    
                    }
                break;   

                case 1:		// hdmi check
                    if (mProgress2 == STOP_PROGRESS) 
                        return;  

                    hdmi_stat = HdmiSwitch.isHdmiConnected(); 
                    if (hdmi_stat_old == HDMI_DISCONNECTED) {            	 
                        if (hdmi_stat == HDMI_CONNECTED) {
                            hdmi_stat_old = hdmi_stat;

                            if (confirm_dialog != null) {
                                mProgress = STOP_PROGRESS;
                                confirm_dialog.dismiss();
                            }
                            updateActivityDisplay();
                        }
                    } else {            	
                        if (hdmi_stat == HDMI_DISCONNECTED) {
                            hdmi_stat_old = hdmi_stat;  

                            if (confirm_dialog != null) {
                            mProgress = STOP_PROGRESS;
                            confirm_dialog.dismiss();
                            }

                            //                		if (!HdmiSwitch.getCurMode().equals("panel")) {
                            //                     		HdmiSwitch.setMode("panel");
                            //                     		notifyModeChanged();
                            //                		}

                            updateActivityDisplay();
                        }
                    }  
                    mProgressHandler.sendEmptyMessageDelayed(1, 3000); 
                break;

                case 2:		// setMode finish, show confirm dialog
                    if (SystemProperties.getBoolean("ro.vout.dualdisplay4", false)) {
                        notifyModeChanged();
                        updateListDisplay(); 
                        finish();                   
                        break;
                    }
                    {
                        boolean hdmiPlugged = !getCurMode().equals("panel");
                        if (hdmiPlugged){
                            if (SystemProperties.getBoolean("ro.vout.dualdisplay2", false)) {
                                setFb0Blank("1");
                            }  
                            if (SystemProperties.getBoolean("ro.vout.dualdisplay3", false)) {
                                writeSysfs(FB2_BLANK_PATH, "1");
                            }                       
                        }
                        if (!SystemProperties.getBoolean("ro.real.externaldisplay", false)){
                            setDualDisplay(hdmiPlugged);
                        }
                        if (hdmiPlugged) mProgressHandler.sendEmptyMessageDelayed(4, 1000); 
                    }
                    notifyModeChanged();
                    updateListDisplay();					
                    if (!SystemProperties.getBoolean("ro.vout.dualdisplay", false)) {
                        if (!getCurMode().equals("panel"))
                            showDialog(CONFIRM_DIALOG_ID);
                        else
                            finish();
                    }   

                break;	

                case 3:		// setMode finish
                    if (SystemProperties.getBoolean("ro.vout.dualdisplay4", false)) {
                        notifyModeChanged();
                        updateListDisplay();
                        break;
                    }
                    {
                        boolean hdmiPlugged = !getCurMode().equals("panel");
                        if (hdmiPlugged){
                            if (SystemProperties.getBoolean("ro.vout.dualdisplay2", false)) {
                                setFb0Blank("1");
                            }  
                            if (SystemProperties.getBoolean("ro.vout.dualdisplay3", false)) {
                                writeSysfs(FB2_BLANK_PATH, "1");
                            }                       
                        }
                        if (!SystemProperties.getBoolean("sys.sf.hotplug", false)){
                            setDualDisplay(hdmiPlugged);
                        }
                        if (hdmiPlugged) mProgressHandler.sendEmptyMessageDelayed(4, 1000); 
                    }
                    notifyModeChanged();
                    updateListDisplay();

                break;

                case 4:     // delayed panel on
                    if (SystemProperties.getBoolean("ro.vout.dualdisplay2", false)) {
                        setFb0Blank("0");
                    }
                    if (SystemProperties.getBoolean("ro.vout.dualdisplay3", false)) {
                        setFb0Blank("0");
                        writeSysfs(FB2_BLANK_PATH, "0");;
                    }                
                break;            	
            }
        }
    }

    /** mode <-> mode_str/axis */
    private static final Map<String, Object> MODE_STR_TABLE = new HashMap<String, Object>();
    private static final Map<String, String> MODE_AXIS_TABLE = new HashMap<String, String>();
    static {
        MODE_STR_TABLE.put("panel", R.string.mode_str_panel);
        MODE_STR_TABLE.put("null", R.string.mode_str_panel);
        //MODE_STR_TABLE.put("480i", R.string.mode_str_480i);
        MODE_STR_TABLE.put("480p", R.string.mode_str_480p);
        //MODE_STR_TABLE.put("576i", R.string.mode_str_576i);
        //MODE_STR_TABLE.put("576p", R.string.mode_str_576p);
        MODE_STR_TABLE.put("720p50hz", R.string.mode_str_720p50hz);
        MODE_STR_TABLE.put("720p", R.string.mode_str_720p);
        //MODE_STR_TABLE.put("1080i", R.string.mode_str_1080i);
        MODE_STR_TABLE.put("1080p24hz", R.string.mode_str_1080p24hz);
        MODE_STR_TABLE.put("1080p50hz", R.string.mode_str_1080p50hz);
        MODE_STR_TABLE.put("1080p", R.string.mode_str_1080p);

        MODE_AXIS_TABLE.put("panel", "0 0 800 480 0 0 18 18");
        MODE_AXIS_TABLE.put("480i", "0 0 800 480 0 0 18 18");
        MODE_AXIS_TABLE.put("480p", "0 0 800 480 0 0 18 18");
        MODE_AXIS_TABLE.put("576i", "0 48 800 480 0 48 18 18");
        MODE_AXIS_TABLE.put("576p", "0 48 800 480 0 48 18 18");
        MODE_AXIS_TABLE.put("720p", "240 120 800 480 240 120 18 18");
        MODE_AXIS_TABLE.put("1080i", "560 300 800 480 560 300 18 18");
        //MODE_AXIS_TABLE.put("1080p", "560 300 800 480 560 300 18 18");
        MODE_AXIS_TABLE.put("1080p", "160 60 1600 960 160 60 36 36");	//2x scale	
    }

    /** fastSwitch func for amlplayer*/
    public static int fastSwitch() {		
        /* check driver interface */        
        File file = new File(HdmiSwitch.DISP_CAP_PATH);
        if (!file.exists()) {        	
            return 0;
        }
        file = new File(HdmiSwitch.MODE_PATH);
        if (!file.exists()) {        	
            return 0;
        }
        file = new File(HdmiSwitch.AXIS_PATH);
        if (!file.exists()) {        	
            return 0;
        }	

        /* panel <-> TV*/
        if (getCurMode().equals("panel")) {
            String mode = getBestMode();
            Log.v(TAG,"fast switch  best mode");
            if (mode != null){
                setMode(mode);
            }
            return 1;
        } else {
            setMode("panel");
            Log.v(TAG,"fast switch to panel");
            return 1;
        }   
    }

    /** get the best mode */
    private static String getBestMode() {
        List<String> list = new ArrayList<String>();    	
        String modeStr;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(DISP_CAP_PATH), 256);
            try {
                while ((modeStr = reader.readLine()) != null) {
                    modeStr = modeStr.split("\\*")[0]; //720p* to 720p

                    if (MODE_STR_TABLE.containsKey(modeStr))
                        list.add(modeStr);	
                }
            } finally {
                reader.close();
            }   

        } catch (IOException e) { 
            Log.e(TAG, "IO Exception when read: " + DISP_CAP_PATH, e);    		
        }    	

        if (list.size() > 0) {    		
            return list.get(list.size() - 1);
        } else{
            return null;
        }
    }

    //option menu    
    public boolean onCreateOptionsMenu(Menu menu){
        String ver_str = null;
        try {
            ver_str = getPackageManager().getPackageInfo("com.amlogic.HdmiSwitch", 0).versionName;			
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        menu.add(0, 0, 0, getText(R.string.app_name) + " v" + ver_str);
        return true;
    }    

    //fix free_scale for video 
    public static int doBeforePlayVideo() {
        if (!isHdmiConnected())
            return 0;

        if (!getCurMode().equals("panel")) {
            if (getCurMode().equals("480p"))
                DisableFreeScaleJni(1);  
            else if (getCurMode().equals("720p50hz"))
                DisableFreeScaleJni(2);
            else if (getCurMode().equals("720p"))
                DisableFreeScaleJni(3);
            else if (getCurMode().equals("1080i"))
                DisableFreeScaleJni(4);  
            else if (getCurMode().equals("1080p24hz"))
                DisableFreeScaleJni(5); 
            else if (getCurMode().equals("1080p50hz"))
                DisableFreeScaleJni(6); 
            else if (getCurMode().equals("1080p"))
                DisableFreeScaleJni(7); 
        }    	
        return 0;    	
    }

    public static int doAfterPlayVideo() {
        if (!isHdmiConnected())
            return 0;

        if (!getCurMode().equals("panel")) {
            if (getCurMode().equals("480p"))
                EnableFreeScaleJni(1);  
            else if (getCurMode().equals("720p50hz"))
                EnableFreeScaleJni(2); 
            else if (getCurMode().equals("720p"))
                EnableFreeScaleJni(3); 
            else if (getCurMode().equals("1080i"))
                EnableFreeScaleJni(4);  
            else if (getCurMode().equals("1080p24hz"))
                EnableFreeScaleJni(5); 
            else if (getCurMode().equals("1080p50hz"))
                EnableFreeScaleJni(6);
            else if (getCurMode().equals("1080p"))
                EnableFreeScaleJni(7);
        }    	
        return 0;   	
    }    

    /// patch for videoplayer crashed
    private static final String OSD_BLANK_PATH = "/sys/class/graphics/fb0/blank";
    private static final String OSD_BLOCK_MODE_PATH = "/sys/class/graphics/fb0/block_mode";

    public static int onVideoPlayerCrashed() {        
        SystemProperties.set("vplayer.playing","false"); 
        SystemProperties.set("vplayer.hideStatusBar.enable","false");
        writeSysfs(OSD_BLANK_PATH, "0");
        writeSysfs(OSD_BLOCK_MODE_PATH, "0"); 

        if (!isHdmiConnected())
            return 0; 

        if (SystemProperties.getBoolean("ro.vout.dualdisplay4", false)) {
            if (!getCurMode().equals("null")) {
                if (getCurMode().equals("480p"))
                    freeScaleForDisplay2Jni(1);  
                else if (getCurMode().equals("720p50hz"))
                    freeScaleForDisplay2Jni(2);
                else if (getCurMode().equals("720p"))
                    freeScaleForDisplay2Jni(3);
                else if (getCurMode().equals("1080p24hz"))
                    freeScaleForDisplay2Jni(5);
                else if (getCurMode().equals("1080p50hz"))
                    freeScaleForDisplay2Jni(6); 
                else if (getCurMode().equals("1080p"))
                    freeScaleForDisplay2Jni(7); 
            }             
            return 0;
        }            

        if (!getCurMode().equals("panel")) {
            if (getCurMode().equals("480p"))
                EnableFreeScaleJni(1);  
            else if (getCurMode().equals("720p50hz"))
                EnableFreeScaleJni(2); 
            else if (getCurMode().equals("720p"))
                EnableFreeScaleJni(3); 
            else if (getCurMode().equals("1080i"))
                EnableFreeScaleJni(4);  
            else if (getCurMode().equals("1080p24hz"))
                EnableFreeScaleJni(5); 
            else if (getCurMode().equals("1080p50hz"))
                EnableFreeScaleJni(6);
            else if (getCurMode().equals("1080p"))
                EnableFreeScaleJni(7);
        }    	
        return 0;                     
    }
        

    //Cling Related
    public static final String PRESS_KEY = "com.amlogic.HdmiSwitch.prefs";
    private static String SHOW_CLING="com.amlogic.HdmiSwitch.action.SHOW_CLING";
    
    private boolean isClingsEnabled() {
        // disable clings when running in a test harness
        if(ActivityManager.isRunningInTestHarness()) return false;
        return true;
    }    

    private void removeCling(int id) {
        final View cling = findViewById(id);
        if (cling != null) {
            final ViewGroup parent = (ViewGroup) cling.getParent();
            parent.post(new Runnable() {
                
                public void run() {
                    parent.removeView(cling);
                }
            });
        }
    }
  
    public int showFirstRunHdmiCling(String mode) {
        // chose the key according to the mod
    	String key;
    	if (mode.equals("first")) {
    		key = HdmiCling.CLING_DISMISS_FIRST;
    	} else { 
		if (!isHdmiConnected()) {
			return -1;
		}
		if (mode.equals("480p")) {
    			key = HdmiCling.CLING_DISMISS_KEY_480P;
    		} else if (mode.equals("720p")) {
    			key = HdmiCling.CLING_DISMISS_KEY_720P;
    		} else if (mode.equals("1080i")) {
    			key = HdmiCling.CLING_DISMISS_KEY_1080I;
    		} else if (mode.equals("1080p")) {
    			key = HdmiCling.CLING_DISMISS_KEY_1080P;
    		} else 
    			return -1;
	}

	if (mode.equals(getCurMode()))
		return -1;

    	SharedPreferences prefs = getSharedPreferences(PRESS_KEY, Context.MODE_PRIVATE);
    	if (isClingsEnabled() && !prefs.getBoolean(key, false)) {
    	//   Intent clingIntent = new Intent();
    	//	clingIntent.setAction(HdmiSwitch.SHOW_CLING); 
    		Intent clingIntent = new Intent(getApplicationContext(),ShowCling.class);
    		clingIntent.putExtra("on_which", key);
    		clingIntent.putExtra("which_cling", "first");
    		startActivity(clingIntent);    		
        } else {
        	removeCling(R.id.hdmi_cling);
        }

	return 1;

    }
    
}
