package com.android.server;

import android.content.Intent;
import android.content.Context;
import android.app.IMboxOutputModeService;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.Handler;
import android.os.Message;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Parcel;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Slog;
import android.util.Log;
import android.text.TextUtils;


import android.view.WindowManagerPolicy;
  
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;  
import java.util.Map.Entry; 
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import android.app.Application;
import android.app.ActivityThread;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.view.KeyEvent;
import android.os.display.DisplayManager;

class MboxOutputModeService extends IMboxOutputModeService.Stub {
    private static final String TAG = "MboxOutputModeService";
    private static final boolean DEBUG = true;

    private static final String CVBS_MODE_PROP = "ubootenv.var.cvbsmode";
    private static final String HDMI_MODE_PROP = "ubootenv.var.hdmimode";
    private static final String COMMON_MODE_PROP = "ubootenv.var.outputmode";
    private final String PASSTHROUGH_PROPERTY = "ubootenv.var.digitaudiooutput";

    private final String AUDIO_OUTPUTMODE = "sys.audio.outputmode";
    private final String SCREENMODE_PROPERTY = "ubootenv.var.screenmode";

    private final String DigitalRawFile = "/sys/class/audiodsp/digital_raw";
    private final String AC3_DRC_CONTROL = "/sys/class/audiodsp/ac3_drc_control";
    private final String DTS_DEC_CONTROL = "/sys/class/audiodsp/dts_dec_control";
    private final String mAudoCapFile = "/sys/class/amhdmitx/amhdmitx0/aud_cap";
    private final String HDMI_AUIDO_SWITCH = "/sys/class/amhdmitx/amhdmitx0/config";
    private final String SYS_DEVICES = "/sys/devices/";

    private static final String FreescaleFb0File = "/sys/class/graphics/fb0/free_scale";
    private static final String FreescaleAxisFb0File = "/sys/class/graphics/fb0/free_scale_axis";
    private static final String FreescaleModeFb0File = "/sys/class/graphics/fb0/freescale_mode";
    private static final String mHdmiPluggedVdac = "/sys/class/aml_mod/mod_off";
    private static final String mHdmiUnpluggedVdac = "/sys/class/aml_mod/mod_on";
    private static final String HDMI_SUPPORT_LIST_SYSFS = "/sys/class/amhdmitx/amhdmitx0/disp_cap";
    private static final String HDMI_10bitSUPPORT_LIST_SYSFS = "/sys/class/amhdmitx/amhdmitx0/dc_cap";
    private static final String HDMI_EDID_SYSFS = "/sys/class/amhdmitx/amhdmitx0/edid";
    private static final String HDMI_HPD_STATE = "/sys/class/amhdmitx/amhdmitx0/hpd_state";
    private static final String HDMI_HDCP_VER = "/sys/class/amhdmitx/amhdmitx0/hdcp_ver";//RX support HDCP version
    private static final String HDMI_HDCP_MODE = "/sys/class/amhdmitx/amhdmitx0/hdcp_mode";//set HDCP mode
    private static final String HDMI_HDCP_AUTH = "/sys/module/hdmitx20/parameters/hdmi_authenticated";//HDCP Authentication
    private static final String HDMI_HDCP_CONF = "/sys/class/amhdmitx/amhdmitx0/hdcp_ctrl"; //HDCP config
    private static final String HDMI_HDCP_KEY = "/sys/class/amhdmitx/amhdmitx0/hdcp_lstore";//TX have 22 or 14 or none key
    private static final String HDMI_HDCP_STOP14 = "stop14"; //stop HDCP1.4 authenticate
    private static final String HDMI_HDCP_STOP22 = "stop22"; //stop HDCP2.2 authenticate
    private static final String HDMI_HDCP_14 = "1"; //start HDCP authenticate
    private static final String HDMI_HDCP_22 = "2"; //start HDCP authenticate
    private static final String HDMI_AVMUTE = "/sys/class/amhdmitx/amhdmitx0/avmute";
    private static final String HDMI_HDR = "/sys/class/amhdmitx/amhdmitx0/hdr_cap";

    private static final String VideoAxisFile = "/sys/class/video/axis";
    private static final String Video2CloneFile = "/sys/class/video2/clone";
    private static final String OutputModeFile = "/sys/class/display/mode";
    private static final String Output2ModeFile = "/sys/class/display2/mode";
    private static final String Output2EnableFile = "/sys/class/display2/enable";
    private static final String windowAxisFile = "/sys/class/graphics/fb0/window_axis";
    private static final String blankFb0File = "/sys/class/graphics/fb0/blank";
    private static final String SYS_HDR_MODE = "/sys/module/am_vecm/parameters/hdr_mode";

    private static final String SCREEN_MODE_PATH = "/sys/class/video/screen_mode";
    private static final String FULL_SCREEN_MODE = "1";
    private static final String NORMAL_SCREEN_MODE = "0";
    private static final String FOUT_THREE_SCREEN_MODE = "2";
    private static final String SIXTEEN_NINE_SCREEN_MODE = "3";

    private final static String sel_480ioutput_x = "ubootenv.var.480i_x";
    private final static String sel_480ioutput_y = "ubootenv.var.480i_y";
    private final static String sel_480ioutput_width = "ubootenv.var.480i_w";
    private final static String sel_480ioutput_height = "ubootenv.var.480i_h";

    private final static String sel_480poutput_x = "ubootenv.var.480p_x";
    private final static String sel_480poutput_y = "ubootenv.var.480p_y";
    private final static String sel_480poutput_width = "ubootenv.var.480p_w";
    private final static String sel_480poutput_height = "ubootenv.var.480p_h";

    private final static String sel_576ioutput_x = "ubootenv.var.576i_x";
    private final static String sel_576ioutput_y = "ubootenv.var.576i_y";
    private final static String sel_576ioutput_width = "ubootenv.var.576i_w";
    private final static String sel_576ioutput_height = "ubootenv.var.576i_h";

    private final static String sel_576poutput_x = "ubootenv.var.576p_x";
    private final static String sel_576poutput_y = "ubootenv.var.576p_y";
    private final static String sel_576poutput_width = "ubootenv.var.576p_w";
    private final static String sel_576poutput_height = "ubootenv.var.576p_h";

    private final static String sel_720poutput_x = "ubootenv.var.720p_x";
    private final static String sel_720poutput_y = "ubootenv.var.720p_y";
    private final static String sel_720poutput_width = "ubootenv.var.720p_w";
    private final static String sel_720poutput_height = "ubootenv.var.720p_h";

    private final static String sel_1080ioutput_x = "ubootenv.var.1080i_x";
    private final static String sel_1080ioutput_y = "ubootenv.var.1080i_y";
    private final static String sel_1080ioutput_width = "ubootenv.var.1080i_w";
    private final static String sel_1080ioutput_height = "ubootenv.var.1080i_h";

    private final static String sel_1080poutput_x = "ubootenv.var.1080p_x";
    private final static String sel_1080poutput_y = "ubootenv.var.1080p_y";
    private final static String sel_1080poutput_width = "ubootenv.var.1080p_w";
    private final static String sel_1080poutput_height = "ubootenv.var.1080p_h";

    private final static String sel_4k2koutput_x = "ubootenv.var.4k2k_x";
    private final static String sel_4k2koutput_y = "ubootenv.var.4k2k_y";
    private final static String sel_4k2koutput_width = "ubootenv.var.4k2k_w";
    private final static String sel_4k2koutput_height = "ubootenv.var.4k2k_h";

    private final static String sel_4k2ksmpteoutput_x = "ubootenv.var.4k2ksmpte_x";
    private final static String sel_4k2ksmpteoutput_y = "ubootenv.var.4k2ksmpte_y";
    private final static String sel_4k2ksmpteoutput_width = "ubootenv.var.4k2ksmpte_w";
    private final static String sel_4k2ksmpteoutput_height = "ubootenv.var.4k2ksmpte_h";

    private static final int OUTPUT480_FULL_WIDTH = 720;
    private static final int OUTPUT480_FULL_HEIGHT = 480;
    private static final int OUTPUT576_FULL_WIDTH = 720;
    private static final int OUTPUT576_FULL_HEIGHT = 576;
    private static final int OUTPUT720_FULL_WIDTH = 1280;
    private static final int OUTPUT720_FULL_HEIGHT = 720;
    private static final int OUTPUT1080_FULL_WIDTH = 1920;
    private static final int OUTPUT1080_FULL_HEIGHT = 1080;
    private static final int OUTPUT4k2k_FULL_WIDTH = 3840;
    private static final int OUTPUT4k2k_FULL_HEIGHT = 2160;
    private static final int OUTPUT4k2ksmpte_FULL_WIDTH = 4096;

	private static final int COLOR_SPACE_UNKNOWN = 0;
	private static final int COLOR_SPACE_YUV_444_8BIT = 1;
	private static final int COLOR_SPACE_YUV_422_8BIT = 2;
	private static final int COLOR_SPACE_YUV_420_8BIT = 3;
	private static final int COLOR_SPACE_YUV_444_10BIT = 4;
	private static final int COLOR_SPACE_YUV_422_10BIT = 5;
	private static final int COLOR_SPACE_YUV_420_10BIT = 6;
	private static final int COLOR_SPACE_YUV_444_12BIT = 7;
	private static final int COLOR_SPACE_YUV_422_12BIT = 8;
	private static final int COLOR_SPACE_YUV_420_12BIT = 9;
	private static final int COLOR_SPACE_RGB_8BIT = 10;
	private static final int COLOR_SPACE_RGB_10BIT = 11;
	private static final int COLOR_SPACE_RGB_12BIT = 12;
	private static final int COLOR_SPACE_AUTO = 13;


    private static final int OUTPUT4k2ksmpte_FULL_HEIGHT = 2160;

    private static  String DEFAULT_OUTPUT_MODE = "720p50hz";
    private static final String DEFAULT_COLORSPACE_MODE = "  Y420 10bit";
    private static final String COLORSPACE_MODE_Y420_8BIT = "  Y420 8bit";
    private static final String COLORSPACE_MODE_Y444_8BIT = "  Y444 8bit";
    private static final String COLORSPACE_MODE_Y444_10BIT = "  Y444 10bit";
    private static final String COLORSPACE_MODE_Y420_10BIT = "  Y420 10bit";
    private static final String  HDMI_MODE_2160P50HZ = "2160p50hz";
    private static final String  HDMI_MODE_2160P60HZ = "2160p60hz";
    private static final String  HDMI_MODE_2160P50HZ420 = "2160p50hz420";
    private static final String  HDMI_MODE_2160P60HZ420 = "2160p60hz420";
    private static final String  HDMI_MODE_2160P30HZ44410BIT = "2160p30hz44410bit";
    private static final String  HDMI_MODE_2160P25HZ44410BIT = "2160p25hz44410bit";
    private static final String  HDMI_MODE_2160P50HZ42010BIT = "2160p50hz42010bit";
    private static final String  HDMI_MODE_2160P60HZ42010BIT = "2160p60hz42010bit";
    private static final String  HDMI_MODE_2160P50HZ42212BIT = "2160p50hz42212bit";
    private static final String  HDMI_MODE_2160P60HZ42212BIT = "2160p60hz42212bit";
    private String[] filterModesAray = null;
    private final static int margin_init_2 = 2;
    private final static int margin_init_5 = 5;
    private int mtmp_unit = 1;
    private int mcurWightAndHeight[] = {1080, 720};
    private int l_gap = 0;//as margin unit
    private int t_gap = 0;//as margin unit
    private int r_gap = 0;//as margin unit
    private int b_gap = 0;//as margin unit

    private static boolean ifModeSetting = false;
    private final Context mContext;
    private Handler mHandler;
    private HandlerThread thr;
    private static int DELAY = 1*500;
    private static int SAVE_PARAMETER = 0;
    private int mleft = 0;
    private int mtop = 0;
    private int mright = 0;
    private int mbottom = 0;
    private int initialwidth = 0;
    private int initialheight = 0;
    final Object mLock = new Object[0];
    private Thread mHdcpThread = null;
    private Dialog mDialog;
    private DisplayManager mDisplayManager = null;
    private static final String[] filteroutputmode_array = {
        "1080p30hz" , "1080p24hz" ,"1080p25hz" ,"2160p24hz",
        "smpte24hz", "smpte25hz", "smpte30hz","smpte60hz420",
        "smpte50hz420", "smpte50hz", "smpte60hz"};
    public class HdcpInfo {
        private boolean useHdcp22;
        private boolean useHdcp14;

        public HdcpInfo() {
            useHdcp22 = false;
            useHdcp14 = false;
        }

        public boolean getUseHdcp22() {
            return useHdcp22;
			}

        public boolean getUseHdcp14() {
            return useHdcp14;
			}

        public void setUseHdcp22(boolean use) {
            useHdcp22 = use;
        }

        public void setUseHdcp14(boolean use) {
            useHdcp14 = use;
        }
    }	
	

    public MboxOutputModeService(Context context) {
        super();
        mContext = context;
        String filterModes = getPropertyString("ro.platform.filter.modes", null);
        Slog.i(TAG, "filterModes: " + filterModes);
        if(filterModes != null) {
            filterModesAray = filterModes.split(",");
        }
        //beging:add by ysten zengzhiliang at 20181204:default resolution 1080
        if("CM201_beijing".equals(SystemProperties.get("ro.ysten.province")))
        {
            DEFAULT_OUTPUT_MODE = "1080p50hz";
        }
        //end:add by ysten zengzhiliang at 20181204:default resolution 1080
	//begin:add by ysten xumiao at 20190327:gansu add defult HDMI
        if(SystemProperties.get("ro.ysten.province").contains("CM201_IPTV_gansu")){
           DEFAULT_OUTPUT_MODE="720p60hz";
        }
        //end:add by ysten xumiao at 20190327:gansu add defult HDMI
        int hdr_mode = Settings.Secure.getInt(mContext.getContentResolver(), Settings.Secure.HDR_MODE, 2);
        Slog.i(TAG, "hdr_mode: " + hdr_mode);
        writeSysfs(SYS_HDR_MODE, Integer.toString(hdr_mode));
        thr = new HandlerThread("DisplayServiceThread");
        thr.start();
        mHandler = new SaveHandler(thr.getLooper());
    }
	
    private class SaveHandler extends Handler {
        public SaveHandler(Looper looper){
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            super.handleMessage(msg);
            if (what == SAVE_PARAMETER){
                setPosition(getCurrentRealMode(), mleft, mtop, initialwidth - mleft - mright,
					initialheight - mtop - mbottom);
            }
        }
    }
	
    private boolean isOutputFilter(String mode) {
        if(filterModesAray != null) {
            int size = filterModesAray.length;
            for(int i=0; i<size; i++) {
                if((filterModesAray[i] != null) && (filterModesAray[i].equals(mode)))
                    return true;
            }
        }
        return false;
    }

    public void setOutputMode(final String mode) {
        if (isDualOutPut()) {
            if (mode.contains("cvbs")) {
                if (!isUnicom()) {
                    setOutputModeNowLocked(mode);
                }
                setOutput2Mode(mode);
            } else {
                setOutputModeNowLocked(mode);
                reSetOutput2Mode(mode);
            }
        } else {
            setOutputModeNowLocked(mode);
        }
    }

    private void setOutput2Mode(String mode) {
        if(!mode.contains("480i") && !mode.contains("576i")) {
            writeSysfs(Output2ModeFile, mode);
        }
    }

    private void reSetOutput2Mode(String mode) {
        if(!mode.contains("480i") && !mode.contains("576i")) {
            String cvbsmode = getCurrentOutPut2Mode();
            writeSysfs(Output2ModeFile, "null");
            writeSysfs(Output2ModeFile, cvbsmode);
        }
    }

    public boolean isDualOutPut() {
        try {
            if (getPropertyBoolean("ro.platform.has.cvbsmode", false) &&
                    Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.DUAL_DISPLAY) == 1 && 
                    isHDMIPlugged()) {
                return true;
            }
        } catch (Settings.SettingNotFoundException se) {
            Slog.d(TAG, "Error: " + se);
        }
        return false;
    }

    public void openCVBS() {
        try {
            if (getPropertyBoolean("ro.platform.has.cvbsmode", false) &&
                    Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.DUAL_DISPLAY) == 0) {
                writeSysfs(Output2EnableFile, "1");
            }
        } catch (Settings.SettingNotFoundException se) {
            Slog.d(TAG, "Error: "+se);
        }
    }

    public void setOutput2On() {
        if (getPropertyBoolean("ro.platform.has.cvbsmode", false)) {
            String cvbsmode = getCurrentOutPut2Mode();
            if(readSysfs(Video2CloneFile).equals("0")) {
                writeSysfs(Video2CloneFile, "1");
            }
            if(cvbsmode.equals("null") || cvbsmode.equals("")) {
                writeSysfs(Output2ModeFile, "480cvbs");
            } else {
                writeSysfs(Output2ModeFile, "null");
                writeSysfs(Output2ModeFile, cvbsmode);
            }
        }
    }

    public void setOutput2Off() {
        if (getPropertyBoolean("ro.platform.has.cvbsmode", false)) {
            if(readSysfs(Video2CloneFile).equals("1")) {
                writeSysfs(Video2CloneFile, "0");
            }
            if(isHDMIPlugged()) {
                writeSysfs(Output2EnableFile, "0");//close cvbs
            }
        }
    }

    public void setDisplayMode(String mode) { //"4:3"  "16:9"
    	Slog.i(TAG, "setDisplayMode");
        int bit_w = 16;
        int bit_h = 9;
        if("4:3".equals(mode)) {
            bit_w = 4;
            bit_h = 3;
        } else if("16:9".equals(mode)) {
            bit_w = 16;
            bit_h = 9;
        }

        String curMode = getCurrentRealMode();
        if(curMode == null) {
            Slog.w(TAG, "curMode is null!");
            return;
        }
        int disp_w = 0;
        int disp_h = 0;
        String prop_x = null;
        String prop_y = null;
        String prop_w = null;
        String prop_h = null;
        if(curMode.contains("480")) {
            disp_w = OUTPUT480_FULL_WIDTH;
            disp_h = OUTPUT480_FULL_HEIGHT;
            if(curMode.contains("480p")) {
                prop_x = sel_480poutput_x;
                prop_y = sel_480poutput_y;
                prop_w = sel_480poutput_width;
                prop_h = sel_480poutput_height;
            } else {
                prop_x = sel_480ioutput_x;
                prop_y = sel_480ioutput_y;
                prop_w = sel_480ioutput_width;
                prop_h = sel_480ioutput_height;
            }
        } else if(curMode.contains("576")) {
            disp_w = OUTPUT576_FULL_WIDTH;
            disp_h = OUTPUT576_FULL_HEIGHT;
            if(curMode.contains("576p")) {
                prop_x = sel_576poutput_x;
                prop_y = sel_576poutput_y;
                prop_w = sel_576poutput_width;
                prop_h = sel_576poutput_height;
            } else {
                prop_x = sel_576ioutput_x;
                prop_y = sel_576ioutput_y;
                prop_w = sel_576ioutput_width;
                prop_h = sel_576ioutput_height;
            }
        } else if(curMode.contains("720")) {
            disp_w = OUTPUT720_FULL_WIDTH;
            disp_h = OUTPUT720_FULL_HEIGHT;
            prop_x = sel_720poutput_x;
            prop_y = sel_720poutput_y;
            prop_w = sel_720poutput_width;
            prop_h = sel_720poutput_height;
        } else if(curMode.contains("1080")) {
            disp_w = OUTPUT1080_FULL_WIDTH;
            disp_h = OUTPUT1080_FULL_HEIGHT;
            if(curMode.contains("1080p")) {
                prop_x = sel_1080poutput_x;
                prop_y = sel_1080poutput_y;
                prop_w = sel_1080poutput_width;
                prop_h = sel_1080poutput_height;
            } else {
                prop_x = sel_1080ioutput_x;
                prop_y = sel_1080ioutput_y;
                prop_w = sel_1080ioutput_width;
                prop_h = sel_1080ioutput_height;
            }
        } else if(curMode.contains("smpte")) {
            disp_w = OUTPUT4k2ksmpte_FULL_WIDTH;
            disp_h = OUTPUT4k2ksmpte_FULL_HEIGHT;
            prop_x = sel_4k2ksmpteoutput_x;
            prop_y = sel_4k2ksmpteoutput_y;
            prop_w = sel_4k2ksmpteoutput_width;
            prop_h = sel_4k2ksmpteoutput_height;
        } else if(curMode.contains("2160p")) {
            disp_w = OUTPUT4k2k_FULL_WIDTH;
            disp_h = OUTPUT4k2k_FULL_HEIGHT;
            prop_x = sel_4k2koutput_x;
            prop_y = sel_4k2koutput_y;
            prop_w = sel_4k2koutput_width;
            prop_h = sel_4k2koutput_height;
        }

        Slog.d(TAG, "Display width: " + disp_w + " height: " + disp_h);
        if((disp_w > 0) && (disp_h > 0)) {
            String mWinAxis = null;
            int calc_x = 0;
            int calc_y = 0;
            int calc_w = disp_w;
            int calc_h = disp_h;
            if((disp_w * bit_h) != (disp_h * bit_w)) {
                int bit = (disp_w/bit_w > disp_h/bit_h) ? disp_h/bit_h : disp_w/bit_w;
                calc_w = bit_w*bit;
                calc_h = bit_h*bit;
                calc_x = (disp_w - calc_w)/2;
                calc_y = (disp_h - calc_h)/2;
            }
            mWinAxis = calc_x + " " + calc_y + " " + (calc_w + calc_x -1) + " " + (calc_h + calc_y -1);
            Slog.d(TAG, "prop_x: " + prop_x);
            Slog.d(TAG, "prop_y: " + prop_y);
            Slog.d(TAG, "prop_w: " + prop_w);
            Slog.d(TAG, "prop_h: " + prop_h);
            Slog.d(TAG, "mWinAxis: " + mWinAxis);
            
            if((prop_x != null) && (prop_y != null) && (prop_w != null) && (prop_h != null)) {
                writeSysfs(windowAxisFile, mWinAxis);
                writeSysfs(FreescaleFb0File, "0x10001");
                setProperty(prop_x, String.valueOf(calc_x));
                setProperty(prop_y, String.valueOf(calc_y));
                setProperty(prop_w, String.valueOf(calc_w));
                setProperty(prop_h, String.valueOf(calc_h));
            }
        }
    }

    private boolean hdcpInit(HdcpInfo hdcpInfo) {
		Slog.i(TAG , "..........Hdcp Init.....");
        boolean useHdcp22 = false;
        boolean useHdcp14 = false;

        String hdcpRxVer;
        String hdcpTxKey;

        //14 22 00 HDCP TX
        hdcpTxKey = readSysfs(HDMI_HDCP_KEY);
        Slog.i(TAG, "HDCP TX key:" + hdcpTxKey);
        if (hdcpTxKey.isEmpty() || hdcpTxKey.equals("00"))
            return false;

        //14 22 00 HDCP RX
        hdcpRxVer = readSysfs(HDMI_HDCP_VER);
        Slog.i(TAG, "HDCP RX version:" + hdcpRxVer);
        if (hdcpRxVer.isEmpty() || hdcpRxVer.equals("00"))
            return false;

        //stop HDCP 2.2
        Slog.i(TAG, "HDCP init, first stop hdcp_tx22 and hdcp 1.4");
        setProperty("ctl.stop", "hdcp_tx22");
        //stop HDCP 1.4
        writeSysfs(HDMI_HDCP_CONF, HDMI_HDCP_STOP14);
        writeSysfs(HDMI_HDCP_CONF, HDMI_HDCP_STOP22);
       
        
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {}

        if (hdcpRxVer.contains("22") && hdcpTxKey.contains("22")) {
            useHdcp22 = true;

            //Slog.i(TAG, "HDCP 2.2, stop hdcp_tx22, init will kill hdcp_tx22");
            setProperty("ctl.stop", "hdcp_tx22");
        }

        if (!useHdcp22 && hdcpRxVer.contains("14") && hdcpTxKey.contains("14")) {
            useHdcp14 = true;
            Slog.i(TAG, "HDCP 1.4");
        }

        if (!useHdcp22 && !useHdcp14) {
            //do not support hdcp1.4 and hdcp2.2
            Slog.e(TAG, "device do not support hdcp1.4 or hdcp2.2");
            return false;
        }

        if (useHdcp22) {
            writeSysfs(HDMI_HDCP_MODE, HDMI_HDCP_22);
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {}

            Slog.i(TAG, "HDCP 2.2, start hdcp_tx22");
            setProperty("ctl.start", "hdcp_tx22");
			writeSysfs(HDMI_AVMUTE, "-1");
        }
        else if (useHdcp14) {
            Slog.i(TAG, "HDCP 1.4, start hdcp_tx14");
            writeSysfs(HDMI_HDCP_MODE, HDMI_HDCP_14);
			writeSysfs(HDMI_AVMUTE, "-1");
        }

        hdcpInfo.setUseHdcp22(useHdcp22);
        hdcpInfo.setUseHdcp14(useHdcp14);
        return true;
    }

    public void StarthdcpAuthenticate(){
	hdcpThreadStart();
    }

    private void hdcpAuthenticate(boolean useHdcp22, boolean useHdcp14) {
        Slog.i(TAG, "begin to authenticate");
        int count = 0;
        while (true) {
            try {
                Thread.sleep(200);//sleep 200ms
            } catch (InterruptedException e) {}

            if (readSysfs(HDMI_HDCP_AUTH).equals("1")) { //Authenticate is OK
                Slog.i(TAG, "Authenticate is OK");
                break;
            }

            count++;
            if (count > 300) { //max 200msx25 = 5s it will authenticate completely
                if (useHdcp22) {
                    Slog.e(TAG, "HDCP22 authenticate fail, 5s timeout");

                    count = 0;
                    useHdcp22 = false;
                    useHdcp14 = true;
                    //if support hdcp22, must support hdcp14
                    setProperty("ctl.stop", "hdcp_tx22");
					writeSysfs(HDMI_HDCP_CONF, HDMI_HDCP_STOP14);
        			writeSysfs(HDMI_HDCP_CONF, HDMI_HDCP_STOP22);
                    writeSysfs(HDMI_HDCP_MODE, HDMI_HDCP_14);
                    continue;
                }
                else if (useHdcp14) {
                    Slog.e(TAG, "HDCP14 authenticate fail, 5s timeout");
                    writeSysfs(HDMI_HDCP_CONF, HDMI_HDCP_STOP14);
					//writeSysfs(HDMI_AVMUTE, "-1");
                }
                break;
            }
        }
        Slog.i(TAG, "authenticate finish");
    }

    private void hdcpThreadStart() {
        mHdcpThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Slog.i(TAG, "Hdcp Thread Start ......");
                    writeSysfs(HDMI_AVMUTE, "1");
                    HdcpInfo hdcpInfo = new HdcpInfo();
                    if (hdcpInit(hdcpInfo)) {
                        Slog.i(TAG, "HDCP 2.2 use status:" + hdcpInfo.getUseHdcp22()
                            + ", HDCP 1.4 use status:" + hdcpInfo.getUseHdcp14());
                        //first close osd, after HDCP authenticate completely, then open osd
                        writeSysfs(blankFb0File, "1");

                        hdcpAuthenticate(hdcpInfo.getUseHdcp22(), hdcpInfo.getUseHdcp14());

                        writeSysfs(blankFb0File, "0");
                        writeSysfs(FreescaleFb0File, "0x10001");
                    }
                    else {
						writeSysfs(HDMI_AVMUTE, "-1");
                        Slog.e(TAG, "Hdcp Init fail!!");
                    }
                } catch (Exception e) {
                    Slog.e(TAG, "Unexpected exception collecting process stats", e);
                }
            }
        });
        mHdcpThread.start();
    }
    public void setOutputModeNowLocked(final String mode) {
	 Log.d("huxiang", "===========setOutputModeNowLocked===========,mode is "+mode);

        synchronized (mLock) {
            String curMode = getCurrentRealMode();
            String newMode = mode;
			Slog.d("huxiang" , "newMode is : "+newMode+" curMode is "+curMode);
            if(null != newMode && "AUTO".equals(newMode)){
                newMode = getBestMatchResolution();
                Slog.d(TAG , "newMode is : "+newMode);
                setProperty("persist.sys.outputmode" , "AUTO");
            }else
                setProperty("persist.sys.outputmode" , "null");

            String proj_type = getPropertyString("sys.proj.type", null);
            String tender_type = getPropertyString("sys.proj.tender.type", null);

            if(curMode == null || curMode.length() < 4){
                Slog.w(TAG, "===== something wrong!, curMode: " + curMode);
                curMode = DEFAULT_OUTPUT_MODE ;
            }
            if(newMode == null || newMode.length() < 4) {
                Slog.w(TAG, "===== something wrong!, newMode: " + newMode);
                newMode = DEFAULT_OUTPUT_MODE;
            }
            Slog.d(TAG,"===== change mode from *" + curMode + "* to *" + newMode + "* ");
			/*
            if(newMode.equals(curMode)) {
                Slog.w(TAG,"===== The same mode as current, do nothing !");
                return;
            }
			*/
            newMode = checkOutputSupport(newMode, DEFAULT_OUTPUT_MODE);
			Slog.d("huxiang","newMode *" + newMode);
            if(newMode == null) {
                return;
            }

            if (isTelecomJicai()) {
                if (newMode.contains("cvbs")) {
                   setProperty("persist.sys.audiooutputmode", "CVBS");
                } else {
                   setProperty("persist.sys.audiooutputmode", getPropertyString("ubootenv.var.digitaudiooutput", null));
                }
                String hdmihdr = readSysfs(HDMI_HDR);
                if (null != hdmihdr) {
                    if (hdmihdr.contains("Traditional HDR: 1")
                        || hdmihdr.contains("SMPTE ST 2084: 1")
                        || hdmihdr.contains("Hybrif Log-Gamma: 1")) {
                        setProperty("persist.sys.tv.Supporthdr", "1");
                    } else if(hdmihdr.contains("Traditional HDR: 0")
                        && hdmihdr.contains("SMPTE ST 2084: 0")
                        &&hdmihdr.contains("Hybrif Log-Gamma: 0")) {
                        setProperty("persist.sys.tv.Supporthdr", "2");
                    } else {
                        setProperty("persist.sys.tv.Supporthdr", "0");
                    }
                 } else {
                     setProperty("persist.sys.tv.Supporthdr", "0");
                 }
            }
			//if ("telecom".equals(proj_type) && "jicai".equals(tender_type))
            //{ 
              //if(curMode.equals("2160p50hz")&& newMode.contains("2160p50hz420")) 
              // curMode = newMode;
            //}
            if (!isUnicom()) {
                if (null == mDisplayManager) {
                    mDisplayManager = (DisplayManager)mContext.getSystemService(Context.DISPLAY_MANAGER_SERVICE);
                }
                int[] distances = mDisplayManager.getScreenMargin();
                l_gap = distances[0];//as margin unit
                t_gap = distances[1];//as margin unit
                r_gap = distances[2];//as margin unit
                b_gap = distances[3];//as margin unit
                Log.d(TAG, "===========setOutputModeNowLocked====distances =======");
            }

            if (newMode.equals(curMode)) {
                if (isUnicom()) {
                    Slog.w(TAG,"===== The same mode as current new, do nothing !");
                    return;
                }

            if (!getPropertyString("persist.sys.firsttime.boot", "false").equals("true")) {
                    Slog.w(TAG,"===== The same mode as current new, do nothing !");
                    Slog.w(TAG,"===== The persist.sys.firsttime.boot if false, do nothing !");
                    return;
                }
            }

            //shadowScreen(curMode);
            if(newMode.contains("cvbs")) {
                openVdac(newMode);
            } else {
                closeVdac(newMode);
            }
            //get video axis
            int axis[] = {0, 0, 0, 0};
            String axisStr = readSysfs(VideoAxisFile);
            String[] axisArray = axisStr.split(" ");
            for(int i=0; i<axisArray.length; i++) {
                if(i == axis.length) {
                    break;
                }
                try {
                    axis[i] = Integer.parseInt(axisArray[i]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            VideoAxisObj mCurAxis = new VideoAxisObj();
            mCurAxis.top = axis[0];
            mCurAxis.left = axis[1];
            mCurAxis.width = axis[2] - axis[0] + 1;
            mCurAxis.height = axis[3] - axis[1] + 1;

            if (isTelecomJicai()) {
                if (newMode.equals("4096x2160p24hz"))
                    newMode = "smpte24hz";
                else if (newMode.equals("4096x2160p25hz"))
                    newMode = "smpte25hz";
                else if (newMode.equals("4096x2160p30hz"))
                    newMode = "smpte30hz";
                else if (newMode.equals("4096x2160p50hz"))
                    newMode = "smpte50hz";
                else if (newMode.equals("4096x2160p60hz"))
                    newMode = "smpte60hz";
                else if (newMode.equals("4096x2160p50hz420"))
                    newMode = "smpte50hz420";
                else if (newMode.equals("4096x2160p60hz420"))
                    newMode = "smpte60hz420";
            }

            if (null != curMode && curMode.contains("2160p")) {
                 Slog.d(TAG,"write null to mode, curMode:" + curMode + " newMode:" + newMode);
                 writeSysfs(OutputModeFile, "null");
                 try {
                       Thread.sleep(500);
                 } catch (InterruptedException e){
                       e.printStackTrace();
                 }
            }
            writeSysfs(OutputModeFile, newMode);
            int[] oldPosition = getPosition(curMode);
            int[] curPosition = getPosition(newMode);
            setProperty("persist.sys.firsttime.boot", "false");

            VideoAxisObj mOldPos = new VideoAxisObj();
            mOldPos.top = oldPosition[0];
            mOldPos.left = oldPosition[1];
            mOldPos.width = oldPosition[2];
            mOldPos.height = oldPosition[3];

            VideoAxisObj mCurPos = new VideoAxisObj();
            mCurPos.top = curPosition[0];
            mCurPos.left = curPosition[1];
            mCurPos.width = curPosition[2];
            mCurPos.height = curPosition[3];


            String mWinAxis = null;
            if (isUnicom()) {
                mWinAxis = curPosition[0]+" "+curPosition[1]+" "+(curPosition[0]+curPosition[2]-1)+" "+(curPosition[1]+curPosition[3]-1);
            } else {
                setProperty("persist.sys.firsttime.boot", "false");
                int left = curPosition[0] > 100*mtmp_unit ? 100*mtmp_unit : curPosition[0];
                int top = curPosition[1] > 100*mtmp_unit ? 100*mtmp_unit : curPosition[1];
                int right = (mcurWightAndHeight[0]-(curPosition[0]+curPosition[2])) > 100*mtmp_unit ? mcurWightAndHeight[0]-100*mtmp_unit-1 : curPosition[0]+curPosition[2];
                int bottom = (mcurWightAndHeight[1]-(curPosition[1]+curPosition[3])) >100*mtmp_unit ? mcurWightAndHeight[1]-100*mtmp_unit-1 : curPosition[1]+curPosition[3];

                if (left < 0) {
                    left = 0 ;
                }
                if (top < 0) {
                   top = 0;
                }

                if ((mcurWightAndHeight[0]-(curPosition[0]+curPosition[2])) < 0) {
                    right = mcurWightAndHeight[0] - 1;
                    Slog.w(TAG, "right positon < 0,then make it max!-----> =" + right);
                }
                if ((mcurWightAndHeight[1]-(curPosition[1]+curPosition[3])) < 0) {
                    bottom = mcurWightAndHeight[1] - 1;
                    Slog.w(TAG, "bottom positon < 0,then make it max!-----> =" + right);
                }
                mWinAxis = left+" "+top+" "+right+" "+bottom;
            }
            if (getPropertyBoolean("ro.platform.has.realoutputmode", false)) {
                if (SystemProperties.get("ubootenv.var.uimode", "720p").equals("1080p")) {
                    if(newMode.contains("2160") || newMode.contains("smpte")) {
                        //open freescale ,  scale up from 1080p to 4k
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 1919 1079");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    } else if(newMode.contains("1080")) {
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 1919 1079");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    } else if(newMode.contains("720")) {
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 1919 1079");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    } else if(newMode.contains("576")) {
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 1919 1079");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    } else if(newMode.contains("480")){
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 1919 1079");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    }else if(newMode.contains("2160")){
                        //open freescale,  scale up from 720p to 4k
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 1919 1079");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    }else if(newMode.contains("smpte")){
                        //open freescale,  scale up from 720p to 4k
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 1919 1079");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    } else {
                        Slog.w(TAG,"===== can't support this mode : " + newMode);
                        return;
                    }
                } else if (SystemProperties.get("ubootenv.var.uimode", "720p").equals("720p")) {
                    if(newMode.contains("1080")) {
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 1279 719");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    } else if(newMode.contains("720")) {
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 1279 719");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    } else if(newMode.contains("576")) {
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 1279 719");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    } else if(newMode.contains("480")) {
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 1279 719");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    } else if(newMode.contains("2160")){
                        //open freescale,  scale up from 720p to 4k
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 1279 719");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    }else if(newMode.contains("smpte")){
                        //open freescale,  scale up from 720p to 4k
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 1279 719");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    }else {
                        Slog.w(TAG,"===== can't support this mode: " + newMode);
                        return;
                    }
                }else if (SystemProperties.get("ubootenv.var.uimode", "720p").equals("576p")) {
                    if (newMode.contains("1080")) {
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 719 575");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    } else if (newMode.contains("720")) {
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 719 575");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    } else if (newMode.contains("576")) {
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 719 575");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    } else if (newMode.contains("480")) {
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 719 575");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    } else if (newMode.contains("2160")){
                        //open freescale,  scale up from 720p to 4k
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 719 575");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    }else if (newMode.contains("smpte")){
                        //open freescale,  scale up from 720p to 4k
                        writeSysfs(FreescaleModeFb0File, "1");
                        writeSysfs(FreescaleAxisFb0File, "0 0 719 575");
                        writeSysfs(windowAxisFile, mWinAxis);
                        writeSysfs(FreescaleFb0File, "0x10001");
                    } else {
                        Slog.w(TAG,"===== can't support this mode: " + newMode);
                        return;
                    }
                }
                setVideoAxis(curMode, newMode, mOldPos, mCurPos, mCurAxis);
            }
            setProperty(COMMON_MODE_PROP, newMode);
            saveNewMode2Prop(newMode);
            if (isTelecomJicai()) {
                setPosition(getCurrentRealMode(), curPosition[0], curPosition[1], curPosition[2],curPosition[3]);
            }

			
            //start HDCP authenticate
            //only HDMI mode need HDCP authenticate
            if (newMode.contains("cvbs")) {
                Slog.i(TAG, "CVBS mode need stop hdcp_tx22 daemon");
                //setProperty("ctl.stop", "hdcp_tx22");
            }
            else {
                hdcpThreadStart();
            }
            //finish HDCP authenticate

            Intent intent = new Intent(WindowManagerPolicy.ACTION_HDMI_MODE_CHANGED);
            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
            intent.putExtra(WindowManagerPolicy.EXTRA_HDMI_MODE, newMode);
            mContext.sendStickyBroadcastAsUser(intent, UserHandle.OWNER);
        }
        return;
    }

    private void saveNewMode2Prop(String newMode) {
        if((newMode != null) && newMode.contains("cvbs")) {
            setProperty(CVBS_MODE_PROP, newMode);
        } else {
            setProperty(HDMI_MODE_PROP, newMode);
        }         
    }

    private void closeVdac(String outputmode){
        if(getPropertyBoolean("ro.platform.hdmionly", false)) {
            if(!outputmode.contains("cvbs")) {
                writeSysfs(mHdmiPluggedVdac, "vdac");
            }
        }
    }

    private void openVdac(String outputmode){
        if(getPropertyBoolean("ro.platform.hdmionly", false)) {
            if(outputmode.contains("cvbs")) {
                writeSysfs(mHdmiUnpluggedVdac, "vdac"); 
            }     
        }
    }

    public void changeWindow(int left, int top, int right, int bottom) {
        //if(DEBUG) {
            Slog.d(TAG, "changeWindow(), left: " + left + ", top: " + top
                    + ", right: " + right + ", bottom: " + bottom);
        //}
        writeSysfs(windowAxisFile, left + " " + top + " " + right + " " + bottom);
    }

    public void setPosition(String mode, int left, int top, int width, int height) {
        String x = String.valueOf(left);
        String y = String.valueOf(top);
        String w = String.valueOf(width);
        String h = String.valueOf(height);
        //if(DEBUG) {
            Slog.d(TAG, "setPosition(), left: " + left + ", top: " + top
                    + ", width: " + width + ", height: " + height
                    + ", mode: " + mode);
        //}
        if(mode == null) {
            Slog.w(TAG, "setPosition, mode is null!");
            return;
        } else if(mode.contains("480cvbs")|| mode.contains("480i")) {
            setProperty(sel_480ioutput_x, x);
            setProperty(sel_480ioutput_y, y);
            setProperty(sel_480ioutput_width, w);
            setProperty(sel_480ioutput_height, h);
        } else if(mode.contains("576cvbs") || mode.contains("576i")) {
            setProperty(sel_576ioutput_x, x);
            setProperty(sel_576ioutput_y, y);
            setProperty(sel_576ioutput_width, w);
            setProperty(sel_576ioutput_height, h);
        } else if(mode.contains("480p")) {
            setProperty(sel_480poutput_x, x);
            setProperty(sel_480poutput_y, y);
            setProperty(sel_480poutput_width, w);
            setProperty(sel_480poutput_height, h);
        } else if(mode.contains("576p")) {
            setProperty(sel_576poutput_x, x);
            setProperty(sel_576poutput_y, y);
            setProperty(sel_576poutput_width, w);
            setProperty(sel_576poutput_height, h);
        } else if(mode.contains("720p")) {
            setProperty(sel_720poutput_x, x);
            setProperty(sel_720poutput_y, y);
            setProperty(sel_720poutput_width, w);
            setProperty(sel_720poutput_height, h);
        } else if(mode.contains("1080i")) {
            setProperty(sel_1080ioutput_x, x);
            setProperty(sel_1080ioutput_y, y);
            setProperty(sel_1080ioutput_width, w);
            setProperty(sel_1080ioutput_height, h);
        } else if(mode.contains("1080p")) {
            setProperty(sel_1080poutput_x, x);
            setProperty(sel_1080poutput_y, y);
            setProperty(sel_1080poutput_width, w);
            setProperty(sel_1080poutput_height, h);
        } else if(mode.contains("2160p")) {
            setProperty(sel_4k2koutput_x, x);
            setProperty(sel_4k2koutput_y, y);
            setProperty(sel_4k2koutput_width, w);
            setProperty(sel_4k2koutput_height, h);
        } else if(mode.contains("smpte")) {
            setProperty(sel_4k2ksmpteoutput_x, x);
            setProperty(sel_4k2ksmpteoutput_y, y);
            setProperty(sel_4k2ksmpteoutput_width, w);
            setProperty(sel_4k2ksmpteoutput_height, h);
        } else {
            Slog.w(TAG, "setPosition, no support this mode(" + mode + ")");
            return;
        }
        if (isUnicom()) {
            writeSysfs(VideoAxisFile, left + " " + top
                + " " + (left + width - 1) + " " + (top + height - 1));
        }
    }

    public int[] getPosition(String mode) {
    int[] curPosition = { 0, 0, 1280, 720, 1280, 720, };
    boolean bfirstboot = false;

    if (isUnicom()) {
        if (mode.contains("480cvbs")|| mode.contains("480i")) {
            curPosition[0] = getPropertyInt(sel_480ioutput_x, 0);
            curPosition[1] = getPropertyInt(sel_480ioutput_y, 0);
            curPosition[2] = getPropertyInt(sel_480ioutput_width, OUTPUT480_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_480ioutput_height, OUTPUT480_FULL_HEIGHT);
            curPosition[4] = OUTPUT480_FULL_WIDTH;
            curPosition[5] = OUTPUT480_FULL_HEIGHT;
        } else if (mode.contains("576cvbs") || mode.contains("576i")) {
            curPosition[0] = getPropertyInt(sel_576ioutput_x, 0);
            curPosition[1] = getPropertyInt(sel_576ioutput_y, 0);
            curPosition[2] = getPropertyInt(sel_576ioutput_width, OUTPUT576_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_576ioutput_height, OUTPUT576_FULL_HEIGHT);
            curPosition[4] = OUTPUT576_FULL_WIDTH;
            curPosition[5] = OUTPUT576_FULL_HEIGHT;
        } else if (mode.contains("480p")) {
            curPosition[0] = getPropertyInt(sel_480poutput_x, 0);
            curPosition[1] = getPropertyInt(sel_480poutput_y, 0);
            curPosition[2] = getPropertyInt(sel_480poutput_width, OUTPUT480_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_480poutput_height, OUTPUT480_FULL_HEIGHT);
            curPosition[4] = OUTPUT480_FULL_WIDTH;
            curPosition[5] = OUTPUT480_FULL_HEIGHT;
        } else if (mode.contains("576p")) {
            curPosition[0] = getPropertyInt(sel_576poutput_x, 0);
            curPosition[1] = getPropertyInt(sel_576poutput_y, 0);
            curPosition[2] = getPropertyInt(sel_576poutput_width, OUTPUT576_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_576poutput_height, OUTPUT576_FULL_HEIGHT);
            curPosition[4] = OUTPUT576_FULL_WIDTH;
            curPosition[5] = OUTPUT576_FULL_HEIGHT;
        } else if (mode.contains("720p")) {
            curPosition[0] = getPropertyInt(sel_720poutput_x, 0);
            curPosition[1] = getPropertyInt(sel_720poutput_y, 0);
            curPosition[2] = getPropertyInt(sel_720poutput_width, OUTPUT720_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_720poutput_height, OUTPUT720_FULL_HEIGHT);
            curPosition[4] = OUTPUT720_FULL_WIDTH;
            curPosition[5] = OUTPUT720_FULL_HEIGHT;
        } else if (mode.contains("1080i")) {
            curPosition[0] = getPropertyInt(sel_1080ioutput_x, 0);
            curPosition[1] = getPropertyInt(sel_1080ioutput_y, 0);
            curPosition[2] = getPropertyInt(sel_1080ioutput_width, OUTPUT1080_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_1080ioutput_height, OUTPUT1080_FULL_HEIGHT);
            curPosition[4] = OUTPUT1080_FULL_WIDTH;
            curPosition[5] = OUTPUT1080_FULL_HEIGHT;
        } else if (mode.contains("1080p")) {
            curPosition[0] = getPropertyInt(sel_1080poutput_x, 0);
            curPosition[1] = getPropertyInt(sel_1080poutput_y, 0);
            curPosition[2] = getPropertyInt(sel_1080poutput_width, OUTPUT1080_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_1080poutput_height, OUTPUT1080_FULL_HEIGHT);
            curPosition[4] = OUTPUT1080_FULL_WIDTH;
            curPosition[5] = OUTPUT1080_FULL_HEIGHT;
        } else if (mode.contains("2160p")) {
            curPosition[0] = getPropertyInt(sel_4k2koutput_x, 0);
            curPosition[1] = getPropertyInt(sel_4k2koutput_y, 0);
            curPosition[2] = getPropertyInt(sel_4k2koutput_width, OUTPUT4k2k_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_4k2koutput_height, OUTPUT4k2k_FULL_HEIGHT);
            curPosition[4] = OUTPUT4k2k_FULL_WIDTH;
            curPosition[5] = OUTPUT4k2k_FULL_HEIGHT;
        } else if (mode.contains("smpte")) {
            curPosition[0] = getPropertyInt(sel_4k2ksmpteoutput_x, 0);
            curPosition[1] = getPropertyInt(sel_4k2ksmpteoutput_y, 0);
            curPosition[2] = getPropertyInt(sel_4k2ksmpteoutput_width, OUTPUT4k2ksmpte_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_4k2ksmpteoutput_height, OUTPUT4k2ksmpte_FULL_HEIGHT);
            curPosition[4] = OUTPUT4k2ksmpte_FULL_WIDTH;
            curPosition[5] = OUTPUT4k2ksmpte_FULL_HEIGHT;
        } else {
            curPosition[0] = getPropertyInt(sel_720poutput_x, 0);
            curPosition[1] = getPropertyInt(sel_720poutput_y, 0);
            curPosition[2] = getPropertyInt(sel_720poutput_width, OUTPUT720_FULL_WIDTH);
            curPosition[3] = getPropertyInt(sel_720poutput_height, OUTPUT720_FULL_HEIGHT);
            curPosition[4] = OUTPUT720_FULL_WIDTH;
            curPosition[5] = OUTPUT720_FULL_HEIGHT;
        }
        return curPosition;
    }

    if (getPropertyString("persist.sys.firsttime.boot", "false").equals("true")) {
        bfirstboot = true;
    }
    Slog.w(TAG, "getPosition mode = "+mode+"------------------------->");
    Slog.w(TAG, "distances = "+l_gap+", "+t_gap+", "+r_gap+", "+b_gap);
		if(mode == null) {
            Slog.w(TAG, "getPosition, mode is null!");
        } else if(mode.contains("480")) {
            curPosition[4] = OUTPUT480_FULL_WIDTH;
            curPosition[5] = OUTPUT480_FULL_HEIGHT;
            if (bfirstboot) {
                curPosition[0] = 0;
                curPosition[1] = 0;
                curPosition[2] = curPosition[4]-1;
                curPosition[3] = curPosition[5]-1;
            } else {
                curPosition[0] = l_gap;
                curPosition[1] = t_gap;
                curPosition[2] = curPosition[4]- r_gap - l_gap-1;
                curPosition[3] = curPosition[5] - t_gap - b_gap-1;
            }
            mcurWightAndHeight[0] = OUTPUT480_FULL_WIDTH;
            mcurWightAndHeight[1] = OUTPUT480_FULL_HEIGHT;
            mtmp_unit = 1;
        } else if(mode.contains("576")) {
            curPosition[4] = OUTPUT576_FULL_WIDTH;
            curPosition[5] = OUTPUT576_FULL_HEIGHT;
            mcurWightAndHeight[0] = OUTPUT576_FULL_WIDTH;
            mcurWightAndHeight[1] = OUTPUT576_FULL_HEIGHT;
            if (bfirstboot) {
                curPosition[0] = 0;
                curPosition[1] = 0;
                curPosition[2] = curPosition[4]-1;
                curPosition[3] = curPosition[5]-1;
            } else {
                curPosition[0] = l_gap;
                curPosition[1] = t_gap ;
                curPosition[2] = curPosition[4] - r_gap - l_gap-1;
                curPosition[3] = curPosition[5] - t_gap - b_gap-1;
                mtmp_unit = 1;
            }

        } else if(mode.contains("720")) {
            curPosition[4] = OUTPUT720_FULL_WIDTH;
            curPosition[5] = OUTPUT720_FULL_HEIGHT;
            mcurWightAndHeight[0] = OUTPUT720_FULL_WIDTH;
            mcurWightAndHeight[1] = OUTPUT720_FULL_HEIGHT;
            if (bfirstboot) {
                curPosition[0] = 0;
                curPosition[1] = 0;
                curPosition[2] = curPosition[4]-1;
                curPosition[3] = curPosition[5]-1;
            } else {
                curPosition[0] = l_gap;
                curPosition[1] = t_gap ;
                curPosition[2] = curPosition[4] - r_gap - l_gap-1;
                curPosition[3] = curPosition[5] - t_gap - b_gap-1;
                mtmp_unit = 1;
            }

        } else if(mode.contains("1080")) {
            curPosition[4] = OUTPUT1080_FULL_WIDTH;
            curPosition[5] = OUTPUT1080_FULL_HEIGHT;
            mcurWightAndHeight[0] = OUTPUT1080_FULL_WIDTH;
            mcurWightAndHeight[1] = OUTPUT1080_FULL_HEIGHT;
            if (bfirstboot) {
                curPosition[0] = 0;
                curPosition[1] = 0;
                curPosition[2] = curPosition[4]-1;
                curPosition[3] = curPosition[5]-1;
            } else {
                curPosition[0] = l_gap*margin_init_2;
                curPosition[1] = t_gap*margin_init_2;
                curPosition[2] = curPosition[4] - r_gap*margin_init_2 - l_gap*margin_init_2-1;
                curPosition[3] = curPosition[5] - t_gap*margin_init_2 - b_gap*margin_init_2-1;
                mtmp_unit = margin_init_2;
            }

        }  else if(mode.contains("2160")) {
            curPosition[4] = OUTPUT4k2k_FULL_WIDTH;
            curPosition[5] = OUTPUT4k2k_FULL_HEIGHT;
            mcurWightAndHeight[0] = OUTPUT4k2k_FULL_WIDTH;
            mcurWightAndHeight[1] = OUTPUT4k2k_FULL_HEIGHT;
            if (bfirstboot) {
                curPosition[0] = 0;
                curPosition[1] = 0;
                curPosition[2] = curPosition[4]-1;
                curPosition[3] = curPosition[5]-1;
            } else {
                curPosition[0] = l_gap*margin_init_5;
                curPosition[1] = t_gap*margin_init_5;
                curPosition[2] = curPosition[4] - r_gap*margin_init_5 - l_gap*margin_init_5-1;
                curPosition[3] = curPosition[5] - t_gap*margin_init_5 - b_gap*margin_init_5-1;
                mtmp_unit = margin_init_5;
            }

        } else if(mode.contains("smpte")) {
            curPosition[4] = OUTPUT4k2ksmpte_FULL_WIDTH;
            curPosition[5] = OUTPUT4k2ksmpte_FULL_HEIGHT;
            mcurWightAndHeight[0] = OUTPUT4k2ksmpte_FULL_WIDTH;
            mcurWightAndHeight[1] = OUTPUT4k2ksmpte_FULL_HEIGHT;
            if (bfirstboot) {
                curPosition[0] = 0;
                curPosition[1] = 0;
                curPosition[2] = curPosition[4]-1;
                curPosition[3] = curPosition[5]-1;
            } else {
                curPosition[0] = l_gap*margin_init_5;
                curPosition[1] = t_gap*margin_init_5;
                curPosition[2] = curPosition[4] - r_gap*margin_init_5 - l_gap*margin_init_5-1;
                curPosition[3] = curPosition[5] - t_gap*margin_init_5 - b_gap*margin_init_5-1;
                mtmp_unit = margin_init_5;
            }
        } else {
            Slog.w(TAG, "default ----------> getPosition mode = "+mode+"------------------------->");
            curPosition[4] = OUTPUT720_FULL_WIDTH;
            curPosition[5] = OUTPUT720_FULL_HEIGHT;
            curPosition[0] = l_gap*curPosition[4]/OUTPUT480_FULL_WIDTH;
            curPosition[1] = t_gap*curPosition[5]/OUTPUT480_FULL_HEIGHT;
            curPosition[2] = curPosition[4] - r_gap*curPosition[4]/OUTPUT480_FULL_WIDTH - l_gap*curPosition[4]/OUTPUT480_FULL_WIDTH-1;
            curPosition[3] = curPosition[5] - t_gap*curPosition[5]/OUTPUT480_FULL_HEIGHT - b_gap*curPosition[5]/OUTPUT480_FULL_HEIGHT-1;
            mtmp_unit = 1;
            mcurWightAndHeight[0] = OUTPUT480_FULL_WIDTH;
            mcurWightAndHeight[1] = OUTPUT480_FULL_HEIGHT;
        }
        return curPosition;
    }

    public int[] getOffsetPosition() {
        String current_mode = getCurrentRealMode();
        int[] curPosition = getPosition(current_mode);
        int[] position = {0, 0, 0, 0};
        position[0] = curPosition[0];
        position[1] = curPosition[1];
        position[2] = curPosition[4] - curPosition[2] - curPosition[0];       
	position[3] = curPosition[5] - curPosition[3] - curPosition[1];
        return position;
    }
	
    public int setOffsetPosition(int left, int top, int right, int bottom) {
        String current_mode = getCurrentRealMode();
        int[] curPosition = getPosition(current_mode);
        mleft = curPosition[0] + left;
        mtop = curPosition[1] + top;
        mright = curPosition[4] - curPosition[2] - curPosition[0] + right;
        mbottom = curPosition[5] - curPosition[3] - curPosition[1] + bottom;
        initialwidth = curPosition[4];
        initialheight = curPosition[5];

        if ((mleft < 0) || (mleft > curPosition[4]/10) || (mtop < 0) || (mtop > curPosition[5]/10) || (mright < 0) 
            || (mright > curPosition[4]/10) || (mbottom < 0) || (mbottom > curPosition[5]/10)){
            return -1;
        }

        changeWindow(mleft, mtop, curPosition[4] - mright -1, curPosition[5] - mbottom - 1);
	 writeSysfs(FreescaleFb0File, "0x10001");
	 mHandler.removeMessages(SAVE_PARAMETER);
	 mHandler.sendEmptyMessageDelayed(SAVE_PARAMETER, DELAY);
        return 0;
    }

    private String checkOutputSupport(String new_mode, String default_mode) {
        if (isHDMIPlugged()) {
            boolean isSupport = false;
            ArrayList<OutputMode> mOutputModeList = readSupportList();
            if((mOutputModeList == null) || (new_mode == null)) {
                Slog.w(TAG, "get edid error, set output mode to " + default_mode);
                return default_mode;
            } else {
                int size = mOutputModeList.size();
                if(DEBUG) Slog.i(TAG, "checkOutputSupport, output size: " + size);
                if(size <= 0) {
                    Slog.w(TAG, "get edid error(mode list is null), set output mode to " + default_mode);
                    return default_mode;
                }
                for (int index = 0; index < size; index++) {
                    OutputMode output = mOutputModeList.get(index);
                    /*if (DEBUG) */Slog.i(TAG,"checkOutputSupport, output: " + output.mode + " new_mode: " + new_mode);
                    //String proj_type = getPropertyString("sys.proj.type", null);
                    //String tender_type = getPropertyString("sys.proj.tender.type", null);
                    //if (("telecom".equals(proj_type) && "shanghai".equals(tender_type))||("telecom".equals(proj_type) && "jicai".equals(tender_type))) {
                       //Slog.d(TAG , "It's shang hai telecom board");
                        //if(new_mode.equals("2160p50hz") && (output.mode).contains(new_mode))
                            //return "2160p50hz420";
						// else if(new_mode.equals("2160p60hz") && (output.mode).contains(new_mode))
                          //  return "2160p60hz420";//
                    //}   
                    
                    if (new_mode.equals(output.mode)) {
                            isSupport = true;					
                    }
                }

                checkColorSpaceMode(new_mode);

                String cmode = switchColorSpaceMode(new_mode);
                if(cmode != null){
                    isSupport = true;
                    new_mode = cmode;
                }

                if(getPropertyBoolean("sys.output.only_420" , false)){
                     if(new_mode.equals("2160p60hz")){
                         //new_mode = "2160p60hz420";
                     }else if(new_mode.equals("2160p50hz")){
                         //new_mode = "2160p50hz420";
                     }
                     Slog.d(TAG , "only_420, NewMode : " + new_mode);
                     isSupport = true;					
                }
                if(isSupport)
                    return new_mode;

                Slog.w(TAG,"===== can't support this mode : " + new_mode);
                return null;
            }
        } else {
            if(new_mode.contains("cvbs"))
                return new_mode;
            else {
                Slog.w(TAG,"===== can't support this mode : " + new_mode);
                return null;
            }
        }
    }

    private boolean isTelecomAutomode(){
        String Automode = getPropertyString("persist.sys.outputmode", "AUTO");
        if(null != Automode && ("AUTO").equals(Automode))
		return true;
        return false;
    }
	
	private boolean isCustomAutomode() {
        String Automode = getPropertyString("persist.sys.outputmode", "AUTO");
        if(null != Automode && ("AUTO").equals(Automode))
			return true;
		
        return false;		
	}

    private boolean isTelecomJicai(){
        String proj_type = getPropertyString("sys.proj.type", null);
        String tender_type = getPropertyString("sys.proj.tender.type", null);
        if ("telecom".equals(proj_type) && "jicai".equals(tender_type))
		return true;
        return false;
    }

     private boolean isUnicom(){
        String proj_type = getPropertyString("sys.proj.type", null);
		//begin by lizheng 20190129 
        if ("unicom".equals(proj_type) || SystemProperties.get("ro.ysten.province").contains("fujian"))
		//end by lizheng 20190129 
		return true;
        return false;
    }

     private boolean isMobile(){
        String proj_type = getPropertyString("sys.proj.type", null);
        if ("mobile".equals(proj_type))
		return true;
        return false;
    }

    public String getCurrentOutPutMode() {
        String curMode = readSysfs(OutputModeFile);
        /*if(DEBUG) */Slog.d(TAG, "getCurrentOutPutMode, mode: " + curMode);
		if (isTelecomJicai() || isUnicom()) {
			if(curMode.contains("10bit")) {
				if(curMode.contains("2160p50")){
					curMode = "2160p50hz420";
				}else if(curMode.contains("2160p60")){
					curMode = "2160p60hz420";
				}
			}else if(curMode.contains("12bit")) {
				if(curMode.contains("2160p50")){
					curMode = "2160p50hz";
				}else if(curMode.contains("2160p60")){
					curMode = "2160p60hz";
				}
			}
			
			if(isTelecomAutomode())
				curMode = "AUTO";
		}
		
		if (isTelecomJicai()) {
			if (curMode.equals("smpte24hz"))
				curMode = "4096x2160p24hz";
			else if (curMode.equals("smpte25hz"))
				curMode = "4096x2160p25hz";
			else if (curMode.equals("smpte30hz"))
				curMode = "4096x2160p30hz";
			else if (curMode.equals("smpte50hz"))
				curMode = "4096x2160p50hz";
			else if (curMode.equals("smpte60hz"))
				curMode = "4096x2160p60hz";
			else if (curMode.equals("smpte50hz420"))
				curMode = "4096x2160p50hz420";
			else if (curMode.equals("smpte60hz420"))
				curMode = "4096x2160p60hz420";
		}
		
		if(isMobile()) {
            //begin: add by ysten wenglei at 20200318: 广东卓影中间件分辨率设置异常
			if("CM201_guangdong_zhuoying".equals(SystemProperties.get("ro.ysten.province"))) {
				if(isCustomAutomode())
					curMode = "AUTO";
            }
            //end: add by ysten wenglei at 20200318: 广东卓影中间件分辨率设置异常
		}
		
		Slog.e(TAG,"getCurrentOutPutMode:" + curMode);
		return curMode;		
    }

    public String getCurrentRealMode() {
        String curMode = readSysfs(OutputModeFile);
        /*if(DEBUG) */Slog.d(TAG, "getCurrentRealMode, mode: " + curMode);
        return curMode;
    }
    
    public String getCurrentOutPut2Mode() {
        String curMode = readSysfs(Output2ModeFile);
        /*if(DEBUG) */Slog.d(TAG, "getCurrentOutPut2Mode, mode: " + curMode);
        return curMode;
    }
    
    private boolean checkBestResolution(ArrayList<OutputMode> mOutputModeList, String checkmode) {
        int size = mOutputModeList.size();
        for (int index = 0; index < size; index++) {
            OutputMode output = mOutputModeList.get(index);
            if (DEBUG) Slog.i(TAG,"checkBestResolution, output: " + output.mode);
            if (output.mode.equals(checkmode)) {
                Slog.i(TAG, "checkBestResolution, return best mode: " + output.mode);
                return true;
            }
        }
        return false;
    }

    public String getBestMatchResolution() {
        ArrayList<OutputMode> mOutputModeList = readSupportList();
        if (mOutputModeList != null && isHDMIPlugged()){
            //add by zhanghk at 20181117:jiangsu use default maximal resolution
            //add by xumiao at 20181121:hubei use default maximal resolution set auto output to 1080p
            if (isUnicom() ||isTelecomJicai()){
                String  cmode  = null;
                if (checkBestResolution(mOutputModeList, "2160p60hz420"))
                    cmode =  "2160p60hz420";
                else if (checkBestResolution(mOutputModeList, "2160p60hz"))
                    cmode =  "2160p60hz";
                else if (checkBestResolution(mOutputModeList, "2160p50hz420"))
                    cmode =  "2160p50hz420";
                else if (checkBestResolution(mOutputModeList, "2160p50hz"))
                    cmode =  "2160p50hz";
                else if (checkBestResolution(mOutputModeList, "2160p30hz"))
                    cmode =  "2160p30hz";
                else if (checkBestResolution(mOutputModeList, "2160p25hz"))
                    cmode =  "2160p25hz";
                else if (checkBestResolution(mOutputModeList, "2160p24hz"))
                    cmode =  "2160p24hz";
                else if (checkBestResolution(mOutputModeList, "1080p60hz"))
                    cmode =  "1080p60hz";
                else if (checkBestResolution(mOutputModeList, "1080p50hz"))
                    cmode =  "1080p50hz";
                /*
                else if (checkBestResolution(mOutputModeList, "1080i60hz"))
                    cmode = "1080i60hz";
                */
                else if (checkBestResolution(mOutputModeList, "1080i50hz"))
                    cmode =  "1080i50hz";
                else if (checkBestResolution(mOutputModeList, "720p60hz"))
                    cmode =  "720p60hz";
                else if (checkBestResolution(mOutputModeList, "720p50hz"))
                    cmode =   "720p50hz";
                else if (checkBestResolution(mOutputModeList, "576i50hz"))
                    cmode =   "576i50hz";
                else if (checkBestResolution(mOutputModeList, "576p50hz"))
                    cmode =   "576p50hz";
                else if (checkBestResolution(mOutputModeList, "480p60hz"))
                    cmode =   "480p60hz";

                String  switch_mode = switchColorSpaceMode(cmode);
				Log.i(TAG,"switch_mode="+switch_mode);
				Log.i(TAG,"cmode="+cmode);
                if(switch_mode != null){
                    cmode = switch_mode;
                }
                return  cmode;
            }else if(isMobile()){
				String  cmode  = null;
				//begin: add by ysten wenglei at 20200318: 广东卓影中间件分辨率设置Auto模式异常
				if("CM201_guangdong_zhuoying".equals(SystemProperties.get("ro.ysten.province"))) {
					int size = mOutputModeList.size();
					if(DEBUG) Slog.i(TAG, "getBestMatchResolution, output size: " + size);
					if(size > 0 ) {
						for (int index = 0; index < size; index++) {
							OutputMode output = mOutputModeList.get(index);
							if (DEBUG) Slog.i(TAG,"getBestMatchResolution, output: " + output.mode + " isBestMode: " + output.isBestMode);
							if (output.isBestMode) {
								Slog.i(TAG, "getBestMatchResolution, return best mode: " + output.mode);
								return output.mode;
							}
						}
					}
                }
                //end: add by ysten wenglei at 20200318: 广东卓影中间件分辨率设置Auto模式异常
			   
                //beging:add by ysten zengzhiliang at 20181204:default resolution 1080
                if("CM201_beijing".equals(SystemProperties.get("ro.ysten.province")))
                {
                   if (checkBestResolution(mOutputModeList, "1080p50hz")) {
                       cmode = "1080p50hz";
                       return cmode;
                   }
                }
				//end:add by ysten zengzhiliang at 20181204:default resolution 1080
                //begin:add by ysten xumiao at 20190327:gansu add defult HDMI
				if(SystemProperties.get("ro.ysten.province").contains("CM201_IPTV_gansu") 
					|| SystemProperties.get("ro.ysten.province").contains("hubei")){
                    if (checkBestResolution(mOutputModeList, "720p60hz")) {
                       cmode = "720p60hz";
                       return cmode;
                    }
                }
                //end:add by ysten xumiao at 20190327:gansu add defult HDMI
				Slog.i(TAG, "-------------------------ro.ysten.province:" + SystemProperties.get("ro.ysten.province"));
				if("CM201_jiangsu".equals(SystemProperties.get("ro.ysten.province"))
                       ||"CM201_hubei".equals(SystemProperties.get("ro.ysten.province"))
                       //add by ysten zengzhiliang:yunnan BestMatchResolution
                       ||"CM201_yunnan".equals(SystemProperties.get("ro.ysten.province"))) {
					Slog.i(TAG, "CM201 getBestMatchResolution, output size: " + mOutputModeList.size());
					for (int index = 0; index < mOutputModeList.size(); index++) {
						OutputMode outputMode = mOutputModeList.get(index);
						if (DEBUG) Slog.i(TAG,"CM201 getBestMatchResolution, output: " + outputMode.mode + " isBestMode: " + outputMode.isBestMode);
						if (outputMode.isBestMode) {
							Slog.i(TAG, "CM201 getBestMatchResolution, return best mode: " + outputMode.mode);
							return outputMode.mode;
						}
					}
                        
					Slog.i(TAG, "getBestMatchResolution not find");
                
					if (checkBestResolution(mOutputModeList, "2160p60hz420"))
						cmode =  "2160p60hz420";
					else if (checkBestResolution(mOutputModeList, "2160p60hz"))
						cmode =  "2160p60hz";
					else if (checkBestResolution(mOutputModeList, "2160p50hz420"))
						cmode =  "2160p50hz420";
					else if (checkBestResolution(mOutputModeList, "2160p50hz"))
						cmode =  "2160p50hz";
					else if (checkBestResolution(mOutputModeList, "2160p30hz"))
						cmode =  "2160p30hz";
					else if (checkBestResolution(mOutputModeList, "2160p25hz"))
						cmode =  "2160p25hz";
					else if (checkBestResolution(mOutputModeList, "2160p24hz"))
						cmode =  "2160p24hz";
					else if (checkBestResolution(mOutputModeList, "1080p60hz"))
						cmode =  "1080p60hz";
					else if (checkBestResolution(mOutputModeList, "1080p50hz"))
						cmode =  "1080p50hz";
					/*
					else if (checkBestResolution(mOutputModeList, "1080i60hz"))
						cmode = "1080i60hz";
					*/
					else if (checkBestResolution(mOutputModeList, "1080i50hz"))
						cmode =  "1080i50hz";
					else if (checkBestResolution(mOutputModeList, "720p60hz"))
						cmode =  "720p60hz";
					else if (checkBestResolution(mOutputModeList, "720p50hz"))
						cmode =   "720p50hz";
					else if (checkBestResolution(mOutputModeList, "576i50hz"))
						cmode =   "576i50hz";
					else if (checkBestResolution(mOutputModeList, "576p50hz"))
						cmode =   "576p50hz";
					else if (checkBestResolution(mOutputModeList, "480p60hz"))
						cmode =   "480p60hz";

					String  switch_mode = switchColorSpaceMode(cmode);
					Log.i(TAG,"switch_mode="+switch_mode);
					Log.i(TAG,"cmode="+cmode);
					if(switch_mode != null){
						cmode = switch_mode;
					}
					//end:add by ysten zengzhiliang at 20181204:default resolution 1080
					return  cmode;	
               }
               else if(checkBestResolution(mOutputModeList, "720p50hz")) {
                   cmode = "720p50hz";
                   return cmode;
               }
            }else{
                int size = mOutputModeList.size();
                if(DEBUG) Slog.i(TAG, "getBestMatchResolution, output size: " + size);
                for (int index = 0; index < size; index++) {
                    OutputMode output = mOutputModeList.get(index);
                    if (DEBUG) Slog.i(TAG,"getBestMatchResolution, output: " + output.mode + " isBestMode: " + output.isBestMode);
                    if (output.isBestMode) {
                        Slog.i(TAG, "getBestMatchResolution, return best mode: " + output.mode);
                        return output.mode;
                    }
                }
            }
        }else if(!isHDMIPlugged()){
            return "576cvbs";
        }
        String default_mode = getPropertyString("ro.platform.best_outputmode", DEFAULT_OUTPUT_MODE);
        Slog.w(TAG, "getBestMatchResolution, return defalut outputmode: " + default_mode);
        return default_mode;
    }
    
    public String getSupportResoulutionList() {
        if (isHDMIPlugged()) {
            ArrayList<OutputMode> mOutputModeList = readSupportList();
            if (mOutputModeList != null){
                StringBuffer strbuf = new StringBuffer();
                int size = mOutputModeList.size();
                int index = 0;
                if(DEBUG) Slog.i(TAG, "getSupportResoulutionList, output size: " + size);
                if (isTelecomJicai()) {
                    int size1 = 0;
                    for (index = 0; index < size; index++) {
                        OutputMode output = mOutputModeList.get(index);
                        if(DEBUG) Slog.i(TAG,"getSupportResoulutionList, output: " + output.mode + " isBestMode: " + output.isBestMode);
                        if(0 == size1)
						strbuf.append("AUTO");
                        size1++;
                        strbuf.append("," + output.mode);
                    }
                    Slog.i(TAG, "TV support list is: " + strbuf.toString());
                    if(size1 > 0)
                        return new String(strbuf);
                } else {
					//begin: add by ysten wenglei at 20200318: 广东卓影中间件分辨率设置异常
					if("CM201_guangdong_zhuoying".equals(SystemProperties.get("ro.ysten.province"))) {
						if(size > 0) {
							strbuf.append("AUTO,");
						}
                    }
                    //end: add by ysten wenglei at 20200318: 广东卓影中间件分辨率设置异常

                    for (index = 0; index < size; index++) {
                        OutputMode output = mOutputModeList.get(index);
                        if(DEBUG) Slog.i(TAG,"getSupportResoulutionList, output: " + output.mode + " isBestMode: " + output.isBestMode);
                        if (isUnicom() && !output.mode.contains("smpte")) {
                            if(index != (size - 1))
                                strbuf.append(output.mode + ",");
                            else
                                strbuf.append(output.mode);
                        }else {
                            if(index != (size - 1))
                                strbuf.append(output.mode + ",");
                            else
                                strbuf.append(output.mode);
                        }
                    }
                    Slog.i(TAG, "TV support list is: " + strbuf.toString());
                    if(size > 0)
                        return new String(strbuf);
                }
            }
            Slog.w(TAG, "getSupportResoulutionList error, output list is null!");
            return null;
        } else {
            if (isTelecomJicai())
				return new String("AUTO,480cvbs,576cvbs");
            else
				return new String("480cvbs,576cvbs");
        }
    }

    public String getSupportedResolution() {
        String curMode = getPropertyString("ubootenv.var.hdmimode", DEFAULT_OUTPUT_MODE);
        ArrayList<OutputMode> mOutputModeList = readSupportList();
        boolean surpport720p = false;
        boolean surpport1080p = false;
        boolean surpport2160p = false;
        boolean surpportCurrent = false;
        if(mOutputModeList == null) {
            Slog.w(TAG, "mOutputModeList is null!");
            return getPropertyString("ro.platform.best_outputmode", DEFAULT_OUTPUT_MODE);
        }
        int size = mOutputModeList.size();
        if(DEBUG) Slog.i(TAG, "getSupportedResolution, output size: " + size);
        for (int index = 0; index < size; index++) {
            OutputMode output = mOutputModeList.get(index);
            if(DEBUG) Slog.i(TAG,"getSupportedResolution, output: " + output.mode);
            if (isTelecomJicai()) {
                if (curMode.equals("smpte24hz"))
                    curMode = "4096x2160p24hz";
                else if (curMode.equals("smpte25hz"))
                    curMode = "4096x2160p25hz";
                else if (curMode.equals("smpte30hz"))
                    curMode = "4096x2160p30hz";
                else if (curMode.equals("smpte50hz"))
                    curMode = "4096x2160p50hz";
                else if (curMode.equals("smpte60hz"))
                    curMode = "4096x2160p60hz";
                else if (curMode.equals("smpte50hz420"))
                    curMode = "4096x2160p50hz420";
                else if (curMode.equals("smpte60hz420"))
                    curMode = "4096x2160p60hz420";
            }

            if (curMode.equals(output.mode)) {
                surpportCurrent = true;
            }
            if("720p50hz".equals(output.mode))
                surpport720p = true;
            if("1080p50hz".equals(output.mode))
                surpport1080p = true;
            if(output.mode.contains("2160p50") || output.mode.contains("2160p60"))
                surpport2160p = true;
        }

        String cmode = switchColorSpaceMode(curMode);
        if(cmode != null && surpport2160p){
            return cmode;
        }

        if(surpportCurrent)
             return curMode;
        if (isMobile()) {
            Slog.d(TAG , "cann't surpport mode : "+curMode);
            if(surpport720p)
                return "720p50hz";
            else if(surpport1080p)
                return "1080p50hz";
        }
        if(isTelecomJicai() && !surpportCurrent)
		return "AUTO";
        curMode = getBestMatchResolution();
        return curMode;
    }

    public boolean isSupported(String mode){
        ArrayList<OutputMode> mOutputModeList = readSupportList();
        int size = mOutputModeList.size();
        for (int index = 0; index < size; index++) {
            OutputMode output = mOutputModeList.get(index);
            if (mode != null && output.mode.equals(mode)){
                Slog.i(TAG, "supported :: " + mode);
                return true;
             }
        }
        return false;
    }

    public void initOutputMode(){
        String curMode = getCurrentRealMode();
        if (isHDMIPlugged()){
            setHdmiPlugged();
        } else {
            setHdmiUnPlugged();
        }
    }

    public void setHdmiUnPlugged(){
        Slog.i(TAG,"===== hdmiUnPlugged()");
        openCVBS();
        if(getPropertyBoolean("ro.platform.hdmionly", true)) {         
            String cvbsmode = getPropertyString("ubootenv.var.cvbsmode", "576cvbs");
            if (isTelecomJicai() && isTelecomAutomode())
                setOutputMode("AUTO");
            else
                setOutputMode(cvbsmode);
            synchronized (mLock) {
                writeSysfs(mHdmiUnpluggedVdac, "vdac");//open vdac 
            }
        }
        forceFreshOsd();
    }

    private void getTvPara() {
        String edidLineStr = null;
        String tvName = null;
        String tvModel = null;
        String tvDip = null;
        int tvWidth = 0;
        int tvHeight = 0;

        try {
            String disPlayMode = readSysfs(OutputModeFile);
            if (disPlayMode!=null && !disPlayMode.equals("")) {
                if (disPlayMode.contains("2160") || disPlayMode.contains("smpte")) {
                    setProperty("persist.sys.tv.dpi", "3840*2160");
                } else if (disPlayMode.contains("1080")) {
                    setProperty("persist.sys.tv.dpi", "1920*1080");
                } else if (disPlayMode.contains("720")) {
                    setProperty("persist.sys.tv.dpi", "1280*720");
                } else if (disPlayMode.contains("576")) {
                    setProperty("persist.sys.tv.dpi", "720*576");
                } else if (disPlayMode.contains("480")) {
                    setProperty("persist.sys.tv.dpi", "720*480");
                } else {
                    setProperty("persist.sys.tv.dpi", "null");
                }
            } else {
                setProperty("persist.sys.tv.dpi", "null");
            }

            FileReader fr = new FileReader(HDMI_EDID_SYSFS);
            BufferedReader br = new BufferedReader(fr);
            try {
                while ((edidLineStr = br.readLine()) != null) {
                    Slog.i(TAG, "TV Para : " + edidLineStr);
                    if (edidLineStr.startsWith("Rx Brand Name: ")) {
                        tvName = edidLineStr.substring("Rx Brand Name: ".length());
                        if (tvName!=null && !tvName.equals(""))
                            setProperty("persist.sys.tv.name", tvName);
                        else
                            setProperty("persist.sys.tv.name", "null");
                    } else if (edidLineStr.startsWith("Rx Product Name: ")) {
                        tvModel = edidLineStr.substring("Rx Product Name: ".length());
                        if (tvModel!=null && !tvModel.equals(""))
                            setProperty("persist.sys.tv.type", tvModel);
                        else
                            setProperty("persist.sys.tv.type", "null");
                    } else if (edidLineStr.startsWith("Physcial size(cm): ")) {
                        tvDip = edidLineStr.substring("Physcial size(cm): ".length());
                        if (tvDip!=null && !tvDip.equals("")) {
                            tvWidth = Integer.parseInt(tvDip.substring(0,tvDip.indexOf(" x ")));
                            tvHeight = Integer.parseInt(tvDip.substring(tvDip.indexOf(" x ")+3));
                            setProperty("persist.sys.tv.size", "" + (new Double(Math.sqrt(tvWidth*tvWidth+tvHeight*tvHeight)/2.54+0.5)).intValue());
                        } else
                            setProperty("persist.sys.tv.size", "null");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        if (getPropertyString("persist.sys.tv.name", null) == null)
            setProperty("persist.sys.tv.name", "null");
        if (getPropertyString("persist.sys.tv.type", null) == null)
            setProperty("persist.sys.tv.type", "null");
        if (getPropertyString("persist.sys.tv.size", null) == null)
            setProperty("persist.sys.tv.size", "null");
    }

    public void setHdmiPlugged(){
        int isAutoHdmiMode = 0;
        String proj_type = getPropertyString("sys.proj.type", null);
        String tender_type = getPropertyString("sys.proj.tender.type", null);
        boolean auto = false;
        if ("telecom".equals(proj_type) && ("jicai".equals(tender_type) || "yueme".equals(tender_type))){
		if(isTelecomAutomode())
			auto = true;
        }

        String colorMode = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE);
        Slog.i(TAG, "color mode: " + colorMode);
        if(colorMode == null&& getPropertyBoolean("sys.output.10bit" , false)){
             Slog.i(TAG, "switch to Auto color mode");
             setBestColorMode();
        }

        try {
            isAutoHdmiMode = Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.DISPLAY_OUTPUTMODE_AUTO);
			//add by huxiang at 2019/8/14 for 不能取消自动适应屏幕
			if("CM201_hubei".equals(SystemProperties.get("ro.ysten.province"))){
			    isAutoHdmiMode = SystemProperties.getInt("persist.sys.optimalfmt.enable",0);
			}
			Slog.e("huxiang", "isAutoHdmiMode: " + isAutoHdmiMode);
			//add end by huxiang at 2019/8/14
        } catch (Settings.SettingNotFoundException se) {
            Slog.e(TAG, "Error: " + se);
        }
        //begin: add by tianchi at 20190925: 分辨率自适应
	//modify by zhaolianghua for guizhou start @20191104
        if("CM201_ningxia".equals(SystemProperties.get("ro.ysten.province"))||"cm201_guizhou".equals(SystemProperties.get("ro.ysten.province"))){
            isAutoHdmiMode = SystemProperties.getInt("persist.sys.optimalfmt.enable",0);
        }
	//modify by zhaolianghua for guizhou end @20191104
        //end: add by tianchi at 20190925: 分辨率自适应
        Slog.i(TAG,"===== hdmiPlugged(): " + isAutoHdmiMode);
        if(getPropertyBoolean("ro.platform.has.realoutputmode", false)){ 
            if(getPropertyBoolean("ro.platform.hdmionly", true)) {
                writeSysfs(mHdmiPluggedVdac, "vdac");
		if(isAutoHdmiMode != 0 || auto) {
			if(auto)
				setOutputMode("AUTO");
			else
				setOutputMode(getBestMatchResolution());
        } else {
                    setOutputMode(getSupportedResolution());
                }
            }
            switchHdmiPassthough();
        } else {
            if(getPropertyBoolean("ro.platform.hdmionly", true)) {
                writeSysfs(mHdmiPluggedVdac, "vdac");
                if(isAutoHdmiMode != 0) {
                    setOutputMode(getBestMatchResolution());
                } else {
                    setOutputMode(getSupportedResolution());
                }
                switchHdmiPassthough();
                writeSysfs(blankFb0File, "0");
            }
        }
        forceFreshOsd();
		
		getTvPara();
    }

    public boolean isHDMIPlugged() {
        String status = readSysfs(HDMI_HPD_STATE);
        Slog.d(TAG, "hpd_state: " + status);
        if ("1".equals(status))
            return true;
        else
            return false;
    }

    private ArrayList<OutputMode> readSupportList() {
        String str = null;
        ArrayList<OutputMode> mOutputModeList = new ArrayList<OutputMode>();
        try {
            FileReader fr = new FileReader(HDMI_SUPPORT_LIST_SYSFS);
            BufferedReader br = new BufferedReader(fr);
            try {
                while ((str = br.readLine()) != null) {
                    if(str != null){
                        //if(DEBUG) Slog.i(TAG, "Output: " + str);
                        boolean filter = false;
                        OutputMode output = new OutputMode();
                        if(str.contains("null edid")) {
                            Slog.w(TAG, "readSupportList error, disp_cap: " + str);
                            return null;
                        }
                        if (isTelecomJicai()) {
				if (getPropertyBoolean("sys.output.filter" , true)) {
	                            for (int i = 0; i < filteroutputmode_array.length; i++) {
	                              if (filteroutputmode_array[i].equalsIgnoreCase(str)) {
                                        filter = true;
                                        break;
                                      }
                                    }
                            }
                            if (filter)
                              continue;
                            if (str.equals("smpte24hz"))
                              str = "4096x2160p24hz";
                            else if (str.equals("smpte25hz"))
                              str = "4096x2160p25hz";
                            else if (str.equals("smpte30hz"))
                              str = "4096x2160p30hz";
                            else if (str.equals("smpte50hz"))
                              str = "4096x2160p50hz";
                            else if (str.equals("smpte60hz"))
                              str = "4096x2160p60hz";
                            else if (str.equals("smpte50hz420"))
                              str = "4096x2160p50hz420";
                            else if (str.equals("smpte60hz420"))
                              str = "4096x2160p60hz420";
                        }
                        if(str.contains("*")) {
                            output.mode = new String(str.substring(0, str.length()-1));
                            output.isBestMode = true;
                        } else {
                            output.mode = new String(str);
                            output.isBestMode = false;
                        }
                        //if(DEBUG) Slog.i(TAG, "readSupportList, Output: " + output.mode + ", isBestMode: " + output.isBestMode);
                        if(isOutputFilter(output.mode)) {
                            Slog.w(TAG, "readSupportList, filter this mode: " + output.mode);
                        } else {
                            mOutputModeList.add(output);
                        }
                    }
                };
                fr.close();
                br.close();
                return resolutionSort(mOutputModeList);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private ArrayList<OutputMode> resolutionSort(ArrayList<OutputMode> modes){
            Collections.sort(modes,new Comparator<OutputMode>() {
                  @Override
                  public int compare(OutputMode o1, OutputMode o2) {
                     if (o1.mode.startsWith("smpte") || o2.mode.startsWith("smpte")) {
                            if (o1.mode.startsWith("smpte") && o2.mode.startsWith("smpte")) {
                                return compareResolution(o1, o2);
                            } else {
                                if (o1.mode.startsWith("smpte")) {
                                    return -1;
                                } else {
                                    return 1;
                                }
                            }
                     } else {
                            if (Character.isDigit(o1.mode.charAt(3)) && Character.isDigit(o2.mode.charAt(3))) {
                                 return compareResolution(o1, o2);
                            } else if (Character.isDigit(o1.mode.charAt(3)) && !Character.isDigit(o2.mode.charAt(3))) {
                                    return -1;
                            } else if (!Character.isDigit(o1.mode.charAt(3)) && Character.isDigit(o2.mode.charAt(3))) {
                                    return 1;
                            } else {
                                 return compareResolution(o1, o2);
                            }
                     }
                  }
            });
            return modes;
    }

    private static int compareResolution(OutputMode o1, OutputMode o2) {
          if (o1.mode.substring(0, 4).equals(o2.mode.substring(0, 4))) {
              if (!TextUtils.isEmpty(o1.mode.substring(o1.mode.indexOf("hz")+2)) && TextUtils.isEmpty(o2.mode.substring(o2.mode.indexOf("hz")+2))) {
                  return -1;
              } else if (TextUtils.isEmpty(o1.mode.substring(o1.mode.indexOf("hz")+2)) && !TextUtils.isEmpty(o2.mode.substring(o2.mode.indexOf("hz")+2))) {
                  return 1;
              }
          }
          return -o1.mode.compareTo(o2.mode);
    }

   private String switchColorSpaceMode(String curMode){

     if(curMode == null){
         return null;
     }
	  
	 String switch_mode = null;
	 String color_mode = getColorspaceMode();
	 Slog.i(TAG, "getColorspaceMode color_mode : " + color_mode);

	 if(color_mode != null  && color_mode.contains("Auto")){
          color_mode = getAutoColorMode(curMode);
          Slog.i(TAG, "getColorspaceMode Auto switch to:" + color_mode);
	 }

	if (getPropertyBoolean("sys.output.10bit" , false) &&
	     isColorSpaceSupport(curMode, color_mode) && (!curMode.contains("4096"))) {
	     int type = convertColorSpace(color_mode);
            switch (type){
	     case COLOR_SPACE_YUV_420_8BIT:
                    if(curMode.contains("2160p50")){
                         switch_mode = "2160p50hz420";
                    }else if(curMode.contains("2160p60")){
                         switch_mode = "2160p60hz420";
                    }
                    else if (curMode.contains("2160p30"))
                         switch_mode = "2160p30hz420";
                    else if (curMode.contains("2160p25"))
                         switch_mode = "2160p25hz420";
                    else
                         switch_mode = curMode;
		 break;
            case COLOR_SPACE_YUV_420_10BIT:
                    if(curMode.contains("10bit")){
                         Slog.w(TAG, "curMode use : " + curMode);
                         switch_mode =  curMode;
                    }else if(curMode.contains("2160p50")){
                         switch_mode =  "2160p50hz42010bit";
                    }else if(curMode.contains("2160p60")){
                         switch_mode =   "2160p60hz42010bit";
                    }
                    else if (curMode.contains("2160p30"))
                         switch_mode = "2160p30hz42010bit";
                    else if (curMode.contains("2160p25"))
                         switch_mode = "2160p25hz42010bit";
                    else
                         switch_mode = curMode;
                break;
            case COLOR_SPACE_YUV_444_10BIT:
                    if (curMode.contains("10bit")) {
                         Slog.w(TAG, "curMode use : " + curMode);
                         switch_mode =  curMode;
                    } else if (curMode.contains("2160p30")) {
                         switch_mode =  "2160p30hz44410bit";
                    } else if (curMode.contains("2160p25")) {
                         switch_mode =   "2160p25hz44410bit";
                    } else
                         switch_mode = curMode;
                break;
            case COLOR_SPACE_YUV_422_12BIT:
                    if(curMode.contains("12bit")){
                         Slog.w(TAG, "curMode use : " + curMode);
                         switch_mode =  curMode;
                    }else if(curMode.contains("2160p50")){
                         switch_mode =  "2160p50hz42212bit";
                    }else if(curMode.contains("2160p60")){
                         switch_mode =  "2160p60hz42212bit";
                    }
                break;
            case COLOR_SPACE_YUV_444_8BIT:
                    if(curMode.contains("2160p50")){
                        switch_mode = "2160p50hz";
                    }else if(curMode.contains("2160p60")){
                        switch_mode = "2160p60hz";
                    } else if (curMode.contains("2160p30")) {
                        switch_mode = "2160p30hz";
                    } else if (curMode.contains("2160p25")) {
                        switch_mode = "2160p25hz";
                    }
                    if (!isSupported(switch_mode)){
                        switch_mode = null;
                    }

                    break;
            case COLOR_SPACE_AUTO:
                    Slog.e(TAG,"COLOR_SPACE_AUTO Code error.\n");
                    break;
            }
       
	}

      Slog.d(TAG,"switchColorSpaceMode : " + switch_mode);

      return switch_mode;
   }

    private int convertColorSpace(String color_mode){
	   int type = COLOR_SPACE_UNKNOWN; 
	   Slog.d(TAG,"convertColorSpace orig:" + color_mode);
	   if(color_mode == null)
	       return type;

	   if(color_mode.contains("Y444 10bit")){
           type = COLOR_SPACE_YUV_444_10BIT;
	   }else if(color_mode.contains("Y422 10bit")){
           type = COLOR_SPACE_YUV_422_10BIT;
	   }else if(color_mode.contains("Y420 10bit")){
           type = COLOR_SPACE_YUV_420_10BIT;
	   }else if(color_mode.contains("Y444 12bit")){
           type = COLOR_SPACE_YUV_444_12BIT;
	   }else if(color_mode.contains("Y422 12bit")){
           type = COLOR_SPACE_YUV_422_12BIT;
	   }else if(color_mode.contains("Y420 12bit")){
           type = COLOR_SPACE_YUV_420_12BIT;
	   }else if(color_mode.contains("Y444 8bit")){
           type = COLOR_SPACE_YUV_444_8BIT;
	   }else if(color_mode.contains("Y422 8bit")){
           type = COLOR_SPACE_YUV_422_8BIT;
	   }else if(color_mode.contains("Y420 8bit")){
           type = COLOR_SPACE_YUV_420_8BIT;
	   }else if(color_mode.contains("RGB 10bit")){
           type = COLOR_SPACE_RGB_10BIT;
	   }else if(color_mode.contains("RGB 12bit")){
           type = COLOR_SPACE_RGB_12BIT;
	   }else if(color_mode.contains("RGB 8bit")){
           type = COLOR_SPACE_RGB_8BIT;
	   }else if(color_mode.contains("Auto")){
           type = COLOR_SPACE_AUTO;
	   }

       Slog.d(TAG,"convertColorSpace final:" + type);
       return type;
	
   }

    private final Runnable showDialogHandler = new Runnable(){
        public void run(){
            if((mDialog != null) && mDialog.isShowing()) {
                mDialog.dismiss();
                mDialog = null;
            }
        showDialog();
        }
    };
    private void showDialog(){
        View view = View.inflate(mContext, com.android.internal.R.layout.error_dialog, null);
        mDialog = new Dialog(mContext, com.android.internal.R.style.error_Dialog);
		mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mDialog.setContentView(view);
        Window window = mDialog.getWindow();
        window.setType(WindowManager.LayoutParams.TYPE_TOAST);
        mDialog.show();
        mHandler.removeCallbacks(showDialogTimeHandler);
        mHandler.postDelayed(showDialogTimeHandler, 1000);
    }

    private final Runnable showDialogTimeHandler = new Runnable(){
        public void run(){
            if((mDialog != null) && mDialog.isShowing()) {
                mDialog.dismiss();
                mDialog = null;
            }
        }
    };

    public boolean isTvSupportColor(String mode){
        Slog.d(TAG , "isColorSpaceSupport: " + mode);
        if(mode == null)
            return false;

        if(mode.contains(COLORSPACE_MODE_Y420_8BIT) ||
            mode.contains(COLORSPACE_MODE_Y444_8BIT) ||
            mode.contains("Auto")){
            return true;
        }

        ArrayList<String> mOutput10bitModeList = read10bitList();
        int size = mOutput10bitModeList.size();
        for (int index = 0; index < size; index++) {
             String output10bitcolor = mOutput10bitModeList.get(index);
             Slog.d(TAG , "output10bitcolor is : "+output10bitcolor);
             if(output10bitcolor.contains(mode)){
                 Slog.d(TAG , "Support : " + mode);
                 return true;
             }
        }

        return false;

    }

    public void checkColorSpaceMode(String new_mode){

        String color_mode = getColorspaceMode();

        if(isColorSpaceSupport(new_mode, color_mode) == false){
            color_mode = getAutoColorMode(new_mode);
            Slog.d(TAG , "don't support current color mode ,  switch to : " + color_mode);
            if (!Settings.Secure.putString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE, color_mode)) {
                Slog.e(TAG, "Settings.Secure.putInt(color_space_mode) error!  " + color_mode);
            }
        }

    }

    public boolean isColorSpaceSupport(String hdmimode, String mode){

        if(mode == null || isTvSupportColor(mode) == false){
            Slog.d(TAG , "isColorSpaceSupport dont't support: " + mode);
            return false;
        }

        Boolean boolSupport = false;
        String curMode = hdmimode;
		int type = convertColorSpace(mode);
            switch (type){
	        case COLOR_SPACE_YUV_420_8BIT:
            case COLOR_SPACE_YUV_420_10BIT:
                 if(curMode.equals(HDMI_MODE_2160P50HZ420) ||
                       curMode.equals(HDMI_MODE_2160P60HZ420) ||
                       curMode.equals(HDMI_MODE_2160P50HZ42010BIT) ||
                       curMode.equals(HDMI_MODE_2160P60HZ42010BIT) ){
                       boolSupport = true;
                       break;
                 }
                 if (isSupported("2160p30hz420") || isSupported("2160p25hz420")) {
                   boolSupport = true;
                 }
                 break;
            case COLOR_SPACE_YUV_444_8BIT:
                 if (curMode.equals(HDMI_MODE_2160P50HZ420) ||
                       curMode.equals(HDMI_MODE_2160P60HZ420) ||
                       curMode.equals(HDMI_MODE_2160P50HZ42010BIT) ||
                       curMode.equals(HDMI_MODE_2160P60HZ42010BIT)) {
                       boolSupport = false;
                       break;
                 }
                 boolSupport = true;
                 break;
            case COLOR_SPACE_YUV_444_10BIT:
                 if ((isSupported("2160p25hz") && curMode.equals(HDMI_MODE_2160P25HZ44410BIT))
                     || (isSupported("2160p30hz") && curMode.equals(HDMI_MODE_2160P30HZ44410BIT))) {
                       boolSupport = true;
                       break;
                 }
                 boolSupport = false;
                 break;
            case COLOR_SPACE_YUV_422_12BIT:
                 if (curMode.equals(HDMI_MODE_2160P50HZ) ||
                       curMode.equals(HDMI_MODE_2160P60HZ) ||
                       curMode.equals(HDMI_MODE_2160P50HZ42212BIT) ||
                       curMode.equals(HDMI_MODE_2160P60HZ42212BIT)) {
                       boolSupport = true;
                       break;
                 }
                 boolSupport = false;
                 break;
            case COLOR_SPACE_AUTO:
                 boolSupport = true;
                 break;
            default:
                 break;
        }

        Slog.d(TAG , "hdmimode:" + curMode + " ,isColorSpaceSupport: " + mode + " ,boolSupport:" + boolSupport);

        return boolSupport;
    }


    private  String getAutoColorMode(String mode){

	    String colormode = COLORSPACE_MODE_Y444_8BIT;

        Slog.e(TAG, "getCurrent_AutoSpaceMode HDMI mode :" + mode);

        if(mode.contains("2160p50") || mode.contains("2160p60")){
            if(isColorSpaceSupport(mode, COLORSPACE_MODE_Y420_10BIT)){
                colormode = COLORSPACE_MODE_Y420_10BIT;
            }else if(isColorSpaceSupport(mode, COLORSPACE_MODE_Y420_8BIT)){
                colormode = COLORSPACE_MODE_Y420_8BIT;
            }
	    }
	if (isSupported("2160p30hz420") || isSupported("2160p25hz420")) {
		if (isTvSupportColor(COLORSPACE_MODE_Y420_10BIT))
			colormode = COLORSPACE_MODE_Y420_10BIT;
		else if (isTvSupportColor(COLORSPACE_MODE_Y420_8BIT))
			colormode = COLORSPACE_MODE_Y420_8BIT;
	}

        Slog.e(TAG, "getCurrent_AutoSpaceMode color space mode :" + colormode);

        return colormode;

    }

    public void setBestColorMode(){
		Slog.i("huxiang", "setBestColorMode in!  ");
        if (!Settings.Secure.putString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE, "  Auto")){
            Slog.e(TAG, "Settings.Secure.putInt(color_space_mode) Auto error!  ");
        }
        String new_mode = getBestMatchResolution();
        setOutputModeNowLocked(new_mode);

        return ;
    }

    private String covertOutPutMode(String outputmode , String colormode) {
        String curMode = outputmode;
        int type = convertColorSpace(colormode);
        if (null != curMode && curMode.contains("2160p")) {
        switch (type) {
              case COLOR_SPACE_YUV_422_12BIT:
              curMode = outputmode.contains("2160p60") ?
                "2160p60hz42212bit":outputmode.contains("2160p50") ?
                "2160p50hz42212bit":outputmode;
              break;
              case COLOR_SPACE_YUV_444_8BIT:
              curMode = outputmode.contains("2160p60") ?
                "2160p60hz":outputmode.contains("2160p50") ?
                "2160p50hz":outputmode.contains("2160p30") ?
                "2160p30hz":"2160p25hz";
              break;
              case COLOR_SPACE_YUV_420_8BIT:
              curMode = outputmode.contains("2160p60") ?
                "2160p60hz420":outputmode.contains("2160p50") ?
                "2160p50hz420":outputmode.contains("2160p30") ?
                "2160p30hz420":"2160p25hz420";
              break;
              case COLOR_SPACE_YUV_420_10BIT:
              curMode = outputmode.contains("2160p60") ?
                "2160p60hz42010bit":outputmode.contains("2160p50") ?
                "2160p50hz42010bit":outputmode.contains("2160p30") ?
                "2160p30hz42010bit":"2160p25hz42010bit";
              break;
              case COLOR_SPACE_YUV_444_10BIT:
              curMode = outputmode.contains("2160p30") ?
                "2160p30hz44410bit":"2160p25hz44410bit";
              break;
              case COLOR_SPACE_AUTO:
              if (isTvSupportColor(COLORSPACE_MODE_Y420_10BIT) && isSupported("2160p60hz420")
                  && outputmode.contains("2160p60"))
                curMode = "2160p60hz42010bit";
              else if (isTvSupportColor(COLORSPACE_MODE_Y420_10BIT) && isSupported("2160p50hz420")
                       && outputmode.contains("2160p50"))
                curMode = "2160p50hz42010bit";
              else if (isTvSupportColor(COLORSPACE_MODE_Y420_8BIT) && isSupported("2160p50hz420")
                       && outputmode.contains("2160p50"))
                curMode = "2160p50hz420";
              else if (isTvSupportColor(COLORSPACE_MODE_Y420_8BIT) && isSupported("2160p60hz420")
                       && outputmode.contains("2160p60"))
                curMode = "2160p60hz420";
              else if (isTvSupportColor(COLORSPACE_MODE_Y444_8BIT) && isSupported("2160p50hz")
                       && outputmode.contains("2160p50"))
                curMode = "2160p50hz";
              else if (isTvSupportColor(COLORSPACE_MODE_Y444_8BIT) && isSupported("2160p60hz")
                       && outputmode.contains("2160p60"))
                curMode = "2160p60hz";
              else if (isTvSupportColor(COLORSPACE_MODE_Y420_10BIT) && isSupported("2160p30hz420")
                       && outputmode.contains("2160p30"))
                curMode = "2160p30hz42010bit";
              else if (isTvSupportColor(COLORSPACE_MODE_Y420_10BIT) && isSupported("2160p25hz420")
                       && outputmode.contains("2160p25"))
                curMode = "2160p25hz42010bit";
              else if (isTvSupportColor(COLORSPACE_MODE_Y420_8BIT) && isSupported("2160p25hz420")
                       && outputmode.contains("2160p25"))
                curMode = "2160p25hz420";
              else if (isTvSupportColor(COLORSPACE_MODE_Y420_8BIT) && isSupported("2160p30hz420")
                       && outputmode.contains("2160p30"))
                curMode = "2160p30hz420";
              else if (isTvSupportColor(COLORSPACE_MODE_Y444_8BIT) && isSupported("2160p25hz420")
                       && outputmode.contains("2160p25"))
                curMode = "2160p25hz";
              else if (isTvSupportColor(COLORSPACE_MODE_Y444_8BIT) && isSupported("2160p30hz")
                       && outputmode.contains("2160p30"))
                curMode = "2160p30hz";
              break;
              default:
              break;
        }
        }
      Slog.d(TAG , "in covertoutputmode curMode is :"+curMode);
      return curMode;
    }

    public boolean setColorspaceMode(String mode){
       Slog.e(TAG, "setColorspaceMode :" + mode);

	   String curMode = getCurrentRealMode();
       /*if((mode != null && (mode.contains("Auto") || mode.contains("auto"))) || isColorSpaceSupport(curMode, mode)){
           Slog.i(TAG, "when setSolorSpaceMode set Settings.Global.DISPLAY_OUTPUTMODE_AUTO as 0");
           if (!Settings.Secure.putInt(mContext.getContentResolver(), Settings.Global.DISPLAY_OUTPUTMODE_AUTO, 0)){
               Slog.e(TAG, "Settings.Secure.putInt(color_space_mode) error!  " + Settings.Global.DISPLAY_OUTPUTMODE_AUTO);
           }
       }*/
       String beforeAutomode = getPropertyString("persist.sys.outputmode", "AUTO");
       if(mode != null && (mode.contains("Auto") || mode.contains("auto"))){
	    String colorMode = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE);
           if (!Settings.Secure.putString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE, "  Auto")){
                Slog.e(TAG, "Settings.Secure.putInt(color_space_mode) error!  " + mode);
            }
            if (isTelecomJicai())
               curMode = covertOutPutMode(curMode , mode);
            String cmode = switchColorSpaceMode(curMode);
            if(cmode != null){
               curMode = cmode;
            }
            setOutputModeNowLocked(curMode);
            setProperty("persist.sys.outputmode" , beforeAutomode);
            return true;
       }
       if (isTvSupportColor(mode) && null != curMode && isTelecomJicai()) {
         curMode = covertOutPutMode(curMode , mode);
       }
       if(isColorSpaceSupport(curMode, mode)){

           if (!Settings.Secure.putString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE, mode)) {
               Slog.e(TAG, "Settings.Secure.putInt(color_space_mode) error!  " + mode);
           }
           String cmode = switchColorSpaceMode(curMode);
           if(cmode != null){
               curMode = cmode;
           }

           setOutputModeNowLocked(curMode);
           Slog.i(TAG, "setColorspaceMode: " + mode + " hdmi_mode: " + curMode);
           setProperty("persist.sys.outputmode" , beforeAutomode);
           return true;
       }else{
         if (!isTelecomJicai()) {
           mHandler.removeCallbacks(showDialogHandler);
           mHandler.postDelayed(showDialogHandler, 100);
         }
         return false;
       }
    }

    public String getColorspaceMode(){
        String colorMode = null;
        colorMode = Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.COLOR_SPACE_MODE);
        Slog.i(TAG, "getColorspaceMode: " + colorMode);
        return colorMode;
   }

    public String getSupportColorSpaceList(){
          ArrayList<String> mColorList = read10bitList();
		  String proj_type = SystemProperties.get("sys.proj.type", null);
		  StringBuffer strbuf = new StringBuffer();

		  if (isUnicom()||"telecom".equals(proj_type))
		  {
			strbuf.append("  Auto");
			if (isSupported("2160p50hz420") || isSupported("2160p60hz420")
				|| isSupported("2160p30hz420") || isSupported("2160p25hz420")) {
				strbuf.append("," + COLORSPACE_MODE_Y420_8BIT);
			}
			strbuf.append("," + COLORSPACE_MODE_Y444_8BIT);
		  }

          if (mColorList!= null){
             int size = mColorList.size();
             int index = 0;
             int size1 = 0;
             for (index = 1; index < size; index++) {
                 String color = mColorList.get(index);

				 if (isUnicom() ||"telecom".equals(proj_type))
				 {
					if(color.contains("Y420 10bit"))
					{
						strbuf.append("," + color);
					}
					else if(color.contains("Y422 12bit"))
					{
						strbuf.append("," + color);
					}
					/*else if(color.contains("Y444 10bit"))
					{
						strbuf.append("," + color);
					}*/
				 }
				 else
				 {
				   if(size1 > 0)
                   {
                      strbuf.append("," + color);
                   }
                   else
                   {
                     	strbuf.append(color);
                   }
				 }

                 size1++;

             }
             if(size > 0 && size1 > 0){
                return new String(strbuf);
             }
			 else
			 {
				if (isUnicom() || "telecom".equals(proj_type))
				{
					return new String(strbuf);
				}
			 }
          }
		  return null;

    }

    private ArrayList<String> read10bitList() {
        String str = null;
	 boolean support422 = false;
	 int index = -1 , size = -1;
        ArrayList<String> mOutput10bitModeList = new ArrayList<String>();
	 ArrayList<OutputMode> mOutputModeList = readSupportList();
        try {
            FileReader fr = new FileReader(HDMI_10bitSUPPORT_LIST_SYSFS);
            BufferedReader br = new BufferedReader(fr);
            try {
                while ((str = br.readLine()) != null) {
                    if(str != null){
                        /*if(DEBUG) */Slog.i(TAG, "DeepColor: " + str);
			    mOutput10bitModeList.add(str);
                    }
                }
                fr.close();
                br.close();
		 size = mOutput10bitModeList.size();
		 if(null != mOutputModeList && null != mOutput10bitModeList){
			for (index = 0; index < size; index++) {
				String colormode = mOutput10bitModeList.get(index);
				if (colormode.contains("Y422")){
					for(int i  = 0 ; i < mOutputModeList.size() ; i++){
						OutputMode getmode = mOutputModeList.get(i);
						if(getmode.mode.equals("2160p60hz") || getmode.mode.equals("2160p50hz")){
							support422 = true;
							break;
						}
					}
					if(support422)
						break;
					else{
						mOutput10bitModeList.remove(index);
						index =  index -1;
						size = mOutput10bitModeList.size();
					}
				}
			}
		 }
		 if(getPropertyBoolean("sys.color.supportdebug" , false)){
			for(int i = 0 ; i < mOutput10bitModeList.size() ; i++){
				Slog.d(TAG , "== in read10bitList=="+mOutput10bitModeList.get(i));
			}
		 } 
                return mOutput10bitModeList;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }


    public boolean ifModeIsSetting() {
        return ifModeSetting;
    }

    private void shadowScreen(final String mode){
        writeSysfs(blankFb0File, "1");
        Thread task = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ifModeSetting = true;
                    Thread.sleep(1000);
                    String value = getPropertyString("init.svc.bootvideo", "stopped");
                    if(value.contains("running")) {
                        Slog.i(TAG, "service bootvideo is running, keep shadow OSD");
                    } else {
                        writeSysfs(blankFb0File, "0");
                    }
                    ifModeSetting = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        task.start();
    }

    private String getProperty(String key) {
        String value = SystemProperties.get(key);
        //if(DEBUG) Slog.i(TAG, "getProperty key: " + key + " value: " + value);
        return value;
    }

    private String getPropertyString(String key, String def) {
        String value = SystemProperties.get(key, def);
        //if(DEBUG) Slog.i(TAG, "getPropertyString key: " + key + " def: " + def + " value: " + value);
        return value;
    }

    private int getPropertyInt(String key, int def) {
        int value = SystemProperties.getInt(key, def);
        //if(DEBUG) Slog.i(TAG, "getPropertyInt key: " + key + " def: " + def + " value: " + value);
        return value;
    }

    private long getPropertyLong(String key, long def) {
        long value = SystemProperties.getLong(key, def);
        //if(DEBUG) Slog.i(TAG, "getPropertyLong key: " + key + " def: " + def + " value: " + value);
        return value;
    }

    private boolean getPropertyBoolean(String key, boolean def) {
        boolean value = SystemProperties.getBoolean(key, def);
        //if(DEBUG) Slog.i(TAG, "getPropertyBoolean key: " + key + " def: " + def + " value: " + value);
        return value;
    }    

    private void setProperty(String key, String value) {
        //if(DEBUG) Slog.i(TAG, "setProperty key: " + key + " value: " + value);
        SystemProperties.set(key,value);
    }
     
    private String readSysfs(String path) {
        if (!new File(path).exists()) {
            Slog.w(TAG, "readSysfs File not found: " + path);
            return null; 
        }

        String str = null;
        StringBuilder value = new StringBuilder();

        //if(DEBUG) Slog.i(TAG, "readSysfs path: " + path);

        try {
            FileReader fr = new FileReader(path);
            BufferedReader br = new BufferedReader(fr);
            try {
                while ((str = br.readLine()) != null) {
                    if(str != null)
                        value.append(str);
                };
                fr.close();
                br.close();
                if(DEBUG) Slog.i(TAG, "readSysfs value: " + value.toString());
                return value.toString();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean writeSysfs(String path, String value) {
        if(DEBUG) Slog.i(TAG, "writeSysfs path: " + path + " value: " + value);
        if (!new File(path).exists()) {
            Slog.w(TAG, "writeSysfs File not found: " + path);
            return false;
        }

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path), 64);
            try {
                writer.write(value);
            } finally {
                writer.close();
            }
		//add by liuxl at 20181128 for A20 start
		String isA20 = SystemProperties.get("persist.sys.isa20", "false");
		if("true".equals(isA20)){
			String mode = getPropertyString("persist.sys.audio.setmode", "0");
			if("1".equals(mode)){
				String bootFlag = getPropertyString("sys.boot_completed", "0");
				if(DEBUG) Slog.i(TAG, "writeSysfs bootFlag:" + bootFlag);
				if(HDMI_AUIDO_SWITCH.equals(path) && "audio_on".equals(value) && "1".equals(bootFlag)){
					if(DEBUG) Slog.i(TAG, "writeSysfs mod send broadcast");
					Intent intent = new Intent("com.ysten.tv.input");
					mContext.sendStickyBroadcastAsUser(intent, UserHandle.OWNER);
				}
			}
		}
		//add by liuxl at 20181128 for A20 end
            return true;
        } catch (IOException e) {
            Slog.e(TAG, "IO Exception when write: " + path, e);
            return false;
        }
    }

    private void switchHdmiPassthough() {
        String default_value = "PCM";
        if (isTelecomJicai()) {
            default_value = "HDMI&SPDIF PCM";
        } else if(isUnicom()){
            default_value = "HDMI Only PCM";
        }
        String value = getPropertyString(PASSTHROUGH_PROPERTY, default_value);

        if(value.contains(":auto")) {
            autoSwitchHdmiPassthough();
        } else {
            setDigitalVoiceValue(value);
        }
    }

	private String get_spdif_node(){
	
		File dir = new File(SYS_DEVICES);
		File[] files = dir.listFiles(); 
		for(int i = 0; i < files.length; i++)
		{
		   if(files[i].getName().contains("spdif")){
               Slog.i(TAG, "find spdif:" + files[i]);
               return SYS_DEVICES + files[i].getName() + "/spdif_mute";
		   }
		}
		return "none";
	}
	
    public int autoSwitchHdmiPassthough () {
        String mAudioCapInfo = readSysfs(mAudoCapFile);
        String spdif_node = get_spdif_node();
        if(mAudioCapInfo.contains("Dobly_Digital+")) {
            writeSysfs(DigitalRawFile, "2");
            writeSysfs(spdif_node, "spdif_mute");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
            setProperty(PASSTHROUGH_PROPERTY, "HDMI passthrough:auto");
            return 2;
        } else if(mAudioCapInfo.contains("AC-3")) {
            writeSysfs(DigitalRawFile, "1");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
            writeSysfs(spdif_node, "spdif_unmute");
            setProperty(PASSTHROUGH_PROPERTY, "SPDIF passthrough:auto");
            return 1;
        } else {
            writeSysfs(DigitalRawFile, "0");
            writeSysfs(spdif_node, "spdif_unmute");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
            setProperty(PASSTHROUGH_PROPERTY, "PCM:auto");
            return 0;
        }
    }

    public void setScreenModeValue(String value) {
        if(value == null){
            Slog.e(TAG,"vsetScreenModeValue value is null");
            return;
        }

        if(value.equals("full") || value.equals("normal") || value.equals("16_9") || value.equals("4_3")){
            if(DEBUG) Slog.i(TAG, "Update Screen Mode: " + value);
            setProperty(SCREENMODE_PROPERTY, value);
            return ;
        }
        Slog.e(TAG, "Wrong Screen Mode: " + value);
    }

    public String GetScreenModeValue(){
        return getPropertyString(SCREENMODE_PROPERTY, "full");
    }

    public boolean setVideoScreenModeValue(int value) {		
		String strvalue = null;
		if (3 < value)
		{
			return false;
		}

		switch (value)
		{
			case 0:
			{
				strvalue = "full";	
				writeSysfs(SCREEN_MODE_PATH, FULL_SCREEN_MODE);
				break;
			}
			case 1:
			{
				strvalue = "normal";		
				writeSysfs(SCREEN_MODE_PATH, NORMAL_SCREEN_MODE);	
				break;
			}
			case 2:
			{
				strvalue = "4_3";	
				writeSysfs(SCREEN_MODE_PATH, FOUT_THREE_SCREEN_MODE);	
				break;
			}
			case 3:
			{
				strvalue = "16_9";
				writeSysfs(SCREEN_MODE_PATH, SIXTEEN_NINE_SCREEN_MODE);		
				break;
			}		
			default:
			{
				break;
			}
		}
		
		setProperty(SCREENMODE_PROPERTY, strvalue);
        Slog.i(TAG, "set Screen Mode: " + value + ",---------->" +readSysfs(SCREEN_MODE_PATH));
		return true;
    }
	
    public int  GetVideoScreenModeValue(){
        Slog.i(TAG, "get Screen Mode:--------->" +readSysfs(SCREEN_MODE_PATH));
        String strvalue = getPropertyString(SCREENMODE_PROPERTY, "full");
		if (strvalue.equals("full")) {
			return 0;
		} else if (strvalue.equals("normal")) {
			return 1;
		} else if (strvalue.equals("4_3")) {
			return 2;
		} else if (strvalue.equals("16_9"))	{
			return 3;
		} else {
		  return 0;
		}
    }

    public void setDigitalVoiceValue(String value) {
        String mode = getPropertyString(AUDIO_OUTPUTMODE, "Common");
        if(mode.equals("Expand")) {
            setDigitalVoiceValueTelecomMode(value);
        } else {
            setDigitalVoiceValueCommon(value);
        }
    }

    public String getDigitalVoiceValue(){
        String value = getPropertyString(PASSTHROUGH_PROPERTY, null);
        return value;
    }
    
    public void setDigitalVoiceValueTelecomMode(String value) {
        // value: 
        // "HDMI Only PCM", "HDMI Only Passthrough"
        // "SPDIF Only PCM", "SPDIF Only Passthrough"
        // "HDMI&SPDIF PCM", "HDMI&SPDIF Passthrough"
        // "HDMI&SPDIF Mute", "HDMI Only Auto"
        setProperty(PASSTHROUGH_PROPERTY, value);
        String spdif_node = get_spdif_node();

        if ("HDMI Only PCM".equals(value)) {
            writeSysfs(DigitalRawFile, "0");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
            writeSysfs(spdif_node, "spdif_mute");
        } else if ("HDMI Only Passthrough".equals(value)) {
            writeSysfs(DigitalRawFile, "2");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
            writeSysfs(spdif_node, "spdif_mute");
        } else if ("SPDIF Only PCM".equals(value)) {
            writeSysfs(DigitalRawFile, "0");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_off");
            writeSysfs(spdif_node, "spdif_unmute");
        } else if ("SPDIF Only Passthrough".equals(value)) {
            writeSysfs(DigitalRawFile, "1");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_off");
            writeSysfs(spdif_node, "spdif_unmute");
        } else if ("HDMI&SPDIF PCM".equals(value)) {
            writeSysfs(DigitalRawFile, "0");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
            writeSysfs(spdif_node, "spdif_unmute");
        } else if ("HDMI&SPDIF Passthrough".equals(value)) {
            writeSysfs(DigitalRawFile, "1");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
            writeSysfs(spdif_node, "spdif_unmute");
        } else if ("HDMI&SPDIF Mute".equals(value)) {
            writeSysfs(DigitalRawFile, "0");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_off");
            writeSysfs(spdif_node, "spdif_mute");
        } else if ("HDMI Only Auto".equals(value)) {
            String mAudioCapInfo = readSysfs(mAudoCapFile);
            if(mAudioCapInfo.contains("Dobly_Digital+")) {
                writeSysfs(DigitalRawFile, "2");
                writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
                writeSysfs(spdif_node, "spdif_mute");
            } else if(mAudioCapInfo.contains("AC-3")) {
                writeSysfs(DigitalRawFile, "1");
                writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
                writeSysfs(spdif_node, "spdif_unmute");
            } else {
                writeSysfs(DigitalRawFile, "0");
                writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
                writeSysfs(spdif_node, "spdif_mute");
            }
        } else {
            setDigitalVoiceValueCommon(value);
        }
    }

    private void setDigitalVoiceValueCommon(String value) {
        // value: "PCM", "RAW", "SPDIF passthrough", "HDMI passthrough"
        setProperty(PASSTHROUGH_PROPERTY, value);
        String spdif_node = get_spdif_node();

        if ("PCM".equals(value)) {
            writeSysfs(DigitalRawFile, "0");
            writeSysfs(spdif_node, "spdif_unmute");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
        } else if ("RAW".equals(value)) {
            writeSysfs(DigitalRawFile, "1");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_off");
            writeSysfs(spdif_node, "spdif_unmute");
        } else if ("SPDIF passthrough".equals(value)) {
            writeSysfs(DigitalRawFile, "1");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_off");
            writeSysfs(spdif_node, "spdif_unmute");
        } else if ("HDMI passthrough".equals(value)) {
            writeSysfs(DigitalRawFile, "2");
            writeSysfs(spdif_node, "spdif_mute");
            writeSysfs(HDMI_AUIDO_SWITCH, "audio_on");
        }
    }

    public void enableDobly_DRC(boolean enable){
        if (enable){ //open DRC
            writeSysfs(AC3_DRC_CONTROL, "drchighcutscale 0x64");
            writeSysfs(AC3_DRC_CONTROL, "drclowboostscale 0x64");
        } else {           //close DRC
            writeSysfs(AC3_DRC_CONTROL, "drchighcutscale 0");
            writeSysfs(AC3_DRC_CONTROL, "drclowboostscale 0");
        }
    }

    public void setDoblyMode(String mode){
        //"CUSTOM_0","CUSTOM_1","LINE","RF"; default use "LINE" 
        int i = Integer.parseInt(mode);
        if (i >= 0 && i <= 3){
            writeSysfs(AC3_DRC_CONTROL, "drcmode" + " " + mode);
        } else {
            writeSysfs(AC3_DRC_CONTROL, "drcmode" + " " + "2");
        }
    }

    public void setDTS_DownmixMode(String mode){
        // 0: Lo/Ro;   1: Lt/Rt;  default 0
        int i = Integer.parseInt(mode);
        if (i >= 0 && i <= 1){
            writeSysfs(DTS_DEC_CONTROL, "dtsdmxmode" + " " + mode);
        } else {
            writeSysfs(DTS_DEC_CONTROL, "dtsdmxmode" + " " + "0");
        }
    }

    public void enableDTS_DRC_scale_control(boolean enable){
        if (enable) {
            writeSysfs(DTS_DEC_CONTROL, "dtsdrcscale 0x64");
        } else {
            writeSysfs(DTS_DEC_CONTROL, "dtsdrcscale 0");
        }
    }

    public void enableDTS_Dial_Norm_control(boolean enable){
        if (enable) {
            writeSysfs(DTS_DEC_CONTROL, "dtsdialnorm 1");
        } else {
            writeSysfs(DTS_DEC_CONTROL, "dtsdialnorm 0");
        }
    }

    public int getHdrMode(){
        String mode = readSysfs(SYS_HDR_MODE);
        Slog.i(TAG, "getHdrMode  " + mode);
		if (mode == null) {
            return 2; //auto
        } else if ("0".equals(mode)) {
            return 0; //off
        } else if ("1".equals(mode)) {
            return 1; //on
        } else if ("2".equals(mode)) {
            return 2; //auto
        } else {
            return 2; //auto
        }
    }

    public void setHdrMode(int mode){
        Slog.i(TAG, "setHdrMode  " + mode);

        if (!Settings.Secure.putInt(mContext.getContentResolver(), Settings.Secure.HDR_MODE, mode)) {
            Slog.e(TAG, "Settings.Secure.putInt(hdr_mode) error!  " + mode);
            return;
        }

        if (mode == 0) {//off
            writeSysfs(SYS_HDR_MODE, "0");
		} else if (mode == 1) {//on
            writeSysfs(SYS_HDR_MODE, "1");
        } else if (mode == 2) {//auto
            writeSysfs(SYS_HDR_MODE, "2");
        } else {//auto
            writeSysfs(SYS_HDR_MODE, "2");
        }
    }

    private class OutputMode {
        public String mode;
        public boolean isBestMode;
    }

    private class VideoAxisObj {
        public int top = 0;
        public int left = 0;
        public int width = 0;
        public int height = 0;
        
        public String toString() {
        	return "top=" + top + " left=" + left
        	        + " width=" + width + " height=" + height;
        }
    }
    
    private class VideoAxisMap {
        private HashMap<String, VideoAxisObj> mMap = new HashMap<String, VideoAxisObj>();
        
        public VideoAxisMap() {
            mMap.clear();
        }
        
        public void clear() {
            mMap.clear();
        }

        public boolean addItem(String output, VideoAxisObj axis) {
            if((output != null) && (axis != null)) {
                mMap.put(output, axis);
                return true;
            }
            Slog.w(TAG, "VideoAxisMap->addItem, para is error, output: " + output);
            return false;
        }
        
        public VideoAxisObj getItem(String output) {
            if(output != null) {
                return mMap.get(output);
            }
            Slog.w(TAG, "VideoAxisMap->getItem, output is null");
            return null;
        }

        public String toString() {
            StringBuffer strbuf = new StringBuffer();
            Set<Entry<String, VideoAxisObj>> sets = mMap.entrySet();
            for(Entry<String, VideoAxisObj> entry : sets) {
            	 strbuf.append(entry.getKey());
            	 VideoAxisObj value = (VideoAxisObj)entry.getValue();
            	 if(value != null)
            	     strbuf.append(" " + value.toString());
            	 strbuf.append("\n");
            }
            return new String(strbuf);
        }
    }

    private VideoAxisMap mVideoAxisMap = new VideoAxisMap();

    private boolean checkAxisSame(VideoAxisObj axis1, VideoAxisObj axis2) {
        Slog.d(TAG, axis1.toString() + " / " + axis2.toString());
        boolean check1 = (axis1.top >= 0) && (axis1.left >= 0) && (axis1.width > 0) && (axis1.height > 0);
        boolean check2 = (axis2.top >= 0) && (axis2.left >= 0) && (axis2.width > 0) && (axis2.height > 0);
        boolean check3 = (axis2.top >= (axis1.top - 1)) && (axis2.top <= (axis1.top + 1));
        boolean check4 = (axis2.left >= (axis1.left - 1)) && (axis2.left <= (axis1.left + 1));
        boolean check5 = (axis2.width >= (axis1.width - 1)) && (axis2.width <= (axis1.width + 1));
        boolean check6 = (axis2.height >= (axis1.height - 1)) && (axis2.height <= (axis1.height + 1));
        return check1 && check2 && check3 && check4 && check5 && check6;
    }

    private VideoAxisObj calcVideoAxis(VideoAxisObj mOldPos, VideoAxisObj mCurPos, VideoAxisObj mCurAxis) {
        VideoAxisObj mNewAxis = new VideoAxisObj();
        mNewAxis.top = Math.round((float)(((mCurAxis.top - mOldPos.top) * mCurPos.width * 1.0f) / mOldPos.width + mCurPos.top));
        mNewAxis.left = Math.round((float)(((mCurAxis.left - mOldPos.left) * mCurPos.height * 1.0f) / mOldPos.height + mCurPos.left));
        mNewAxis.width = Math.round((float)((mCurAxis.width * mCurPos.width * 1.0f) / mOldPos.width));
        mNewAxis.height = Math.round((float)((mCurAxis.height * mCurPos.height * 1.0f) / mOldPos.height));
        Slog.i(TAG, "New video axis: " + mNewAxis.toString());
        return mNewAxis;
    }

    private void setVideoAxis(String oldoutput, String newoutput, VideoAxisObj mOldPos, VideoAxisObj mCurPos, VideoAxisObj mCurAxis) {
        if(DEBUG) {
            Slog.i(TAG, "Old output: " + oldoutput + ", New output: " + newoutput);
            Slog.i(TAG, "Old video axis: " + mCurAxis.toString());
            Slog.i(TAG, "Old position: " + mOldPos.toString());
            Slog.i(TAG, "New position: " + mCurPos.toString());
        }

        if(((mCurAxis.top == 0) && (mCurAxis.left == 0)
                && (mCurAxis.width == 0) && (mCurAxis.height == 0))
                || ((mCurAxis.top == 0) && (mCurAxis.left == 0)
                    && (mCurAxis.width == -1) && (mCurAxis.height == -1))
                || ((mCurAxis.top <= mOldPos.top) && (mCurAxis.left <= mOldPos.left)
                    && (mCurAxis.width >= mOldPos.width) && (mCurAxis.height >= mOldPos.height))) {
            String mVideoAxis = mCurPos.top + " " + mCurPos.left + " " +
                    (mCurPos.width + mCurPos.top - 1) + " " + (mCurPos.height + mCurPos.left - 1);
            writeSysfs(VideoAxisFile, mVideoAxis);
            return;
        }

        Slog.i(TAG, "map, " + mVideoAxisMap.toString());
        VideoAxisObj mAxis = mVideoAxisMap.getItem(oldoutput);
        if(mAxis != null) {
            if(checkAxisSame(mAxis, mCurAxis)) {
                mVideoAxisMap.addItem(oldoutput, mCurAxis);
                VideoAxisObj mNewAxis = mVideoAxisMap.getItem(newoutput);
                if(mNewAxis == null) {
                    mNewAxis = calcVideoAxis(mOldPos, mCurPos, mCurAxis);
                    mVideoAxisMap.addItem(newoutput, mNewAxis);
                }
                String mVideoAxis = mNewAxis.top + " " + mNewAxis.left + " " +
                        (mNewAxis.width + mNewAxis.top - 1) + " " + (mNewAxis.height + mNewAxis.left - 1);
                writeSysfs(VideoAxisFile, mVideoAxis);
                Slog.i(TAG, "read map: " + readSysfs(VideoAxisFile));
                return;
            }
        }
        mVideoAxisMap.clear();
        mVideoAxisMap.addItem(oldoutput, mCurAxis);
        VideoAxisObj mNewAxis = calcVideoAxis(mOldPos, mCurPos, mCurAxis);
        mVideoAxisMap.addItem(newoutput, mNewAxis);

        String mVideoAxis = mNewAxis.top + " " + mNewAxis.left + " " +
                (mNewAxis.width + mNewAxis.top - 1) + " " + (mNewAxis.height + mNewAxis.left - 1);
        writeSysfs(VideoAxisFile, mVideoAxis);
        Slog.i(TAG, "read: " + readSysfs(VideoAxisFile));
    }

    private void forceFreshOsd(){
        Thread task = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    IBinder surfaceFlinger = ServiceManager.getService("SurfaceFlinger");
                    if (surfaceFlinger != null) {
                        for (int i = 0; i < 6; i++){
                            Parcel data = Parcel.obtain();
                            data.writeInterfaceToken("android.ui.ISurfaceComposer");
                            surfaceFlinger.transact(1004, data, null, 0);
                            data.recycle();
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e){
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        });
        task.start();
    }

    /*
        updatelogo interface added by qian.liao
    */
    public int updateLogo(String path){

        Slog.i(TAG,"updateLogo is " + path);
        if(path == null){
             Slog.i(TAG,"updateLogo is null, return");
             return 0;
        }
        ConfigServer cs = new ConfigServer();
        return cs.updateLogo(path);
    }
}
