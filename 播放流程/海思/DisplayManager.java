/*
 * Copyright (C) 2007 The Android Open Source Project
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

package android.os.display;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.app.ActivityManagerNative;
import android.util.Log;
import java.util.Map;
import java.util.HashMap;
import android.os.SystemProperties;

/**
* HiDisplayManager interface<br>
* CN: HiDisplayManager鎺ュ彛
*/
public class DisplayManager  {
    private static final String TAG = "CMCCDisplayManager";
    private IDisplayManager mdisplay;

    /* CMCC FMT */
    public final static int DISPLAY_STANDARD_1080P_60 = 0;
    public final static int DISPLAY_STANDARD_1080P_50 = 1;
    public final static int DISPLAY_STANDARD_1080P_30 = 2;
    public final static int DISPLAY_STANDARD_1080P_25 = 3;
    public final static int DISPLAY_STANDARD_1080P_24 = 4;
    public final static int DISPLAY_STANDARD_1080I_60 = 5;
    public final static int DISPLAY_STANDARD_1080I_50 = 6;
    public final static int DISPLAY_STANDARD_720P_60 = 7;
    public final static int DISPLAY_STANDARD_720P_50 = 8;
    public final static int DISPLAY_STANDARD_576P_50 = 9;
    public final static int DISPLAY_STANDARD_480P_60 = 10;
    public final static int DISPLAY_STANDARD_PAL = 11;
    public final static int DISPLAY_STANDARD_NTSC = 12;
    public final static int DISPLAY_STANDARD_3840_2160P_24 = 0x100;
    public final static int DISPLAY_STANDARD_3840_2160P_25 = 0x101;
    public final static int DISPLAY_STANDARD_3840_2160P_30 = 0x102;
    public final static int DISPLAY_STANDARD_3840_2160P_60 = 0x103;
    public final static int DISPLAY_STANDARD_4096_2160P_24 = 0x200;
    public final static int DISPLAY_STANDARD_4096_2160P_25 = 0x201;
    public final static int DISPLAY_STANDARD_4096_2160P_30 = 0x202;
    public final static int DISPLAY_STANDARD_4096_2160P_60 = 0x203;
    public final static int DISPLAY_STANDARD_3840_2160P_50 = 0x104;
    public final static int DISPLAY_STANDARD_4096_2160P_50 = 0x204;

    private int[] mStandard;
    private int[] mAllDisplayStandard = {

        DISPLAY_STANDARD_1080P_60,
        DISPLAY_STANDARD_1080P_50,
        DISPLAY_STANDARD_1080P_30,
        DISPLAY_STANDARD_1080P_25,
        DISPLAY_STANDARD_1080P_24,
        DISPLAY_STANDARD_1080I_60,
        DISPLAY_STANDARD_1080I_50,
        DISPLAY_STANDARD_720P_60,
        DISPLAY_STANDARD_720P_50,
        DISPLAY_STANDARD_576P_50,
        DISPLAY_STANDARD_480P_60,
        DISPLAY_STANDARD_PAL,
        DISPLAY_STANDARD_NTSC,
        DISPLAY_STANDARD_3840_2160P_24,
        DISPLAY_STANDARD_3840_2160P_25,
        DISPLAY_STANDARD_3840_2160P_30,
        DISPLAY_STANDARD_3840_2160P_50,
        DISPLAY_STANDARD_3840_2160P_60,
        DISPLAY_STANDARD_4096_2160P_24,
        DISPLAY_STANDARD_4096_2160P_25,
        DISPLAY_STANDARD_4096_2160P_30,
        DISPLAY_STANDARD_4096_2160P_50,
        DISPLAY_STANDARD_4096_2160P_60,
        };

    /* Hisi FMT */
    public final static int ENC_FMT_1080P_60 = 0;         /*1080p60hz*/
    public final static int ENC_FMT_1080P_50 = 1;         /*1080p50hz*/
    public final static int ENC_FMT_1080P_30 = 2;         /*1080p30hz*/
    public final static int ENC_FMT_1080P_25 = 3;         /*1080p25hz*/
    public final static int ENC_FMT_1080P_24 = 4;         /*1080p24hz*/
    public final static int ENC_FMT_1080i_60 = 5;         /*1080i60hz*/
    public final static int ENC_FMT_1080i_50 = 6;         /*1080i50hz*/
    public final static int ENC_FMT_720P_60 = 7;          /*720p60hz*/
    public final static int ENC_FMT_720P_50 = 8;          /*720p50hz*/
    public final static int ENC_FMT_576P_50 = 9;          /*576p50hz*/
    public final static int ENC_FMT_480P_60 = 10;         /*480p60hz*/
    public final static int ENC_FMT_PAL = 11;             /*BDGHIPAL*/
    public final static int ENC_FMT_NTSC = 14;            /*(M)NTSC*/
    public final static int ENC_FMT_3840X2160_24             = 0x40;
    public final static int ENC_FMT_3840X2160_25             = 0x41;
    public final static int ENC_FMT_3840X2160_30             = 0x42;
    public final static int ENC_FMT_3840X2160_50             = 0x43;
    public final static int ENC_FMT_3840X2160_60             = 0x44;
    public final static int ENC_FMT_4096X2160_24             = 0x45;
    public final static int ENC_FMT_4096X2160_25             = 0x46;
    public final static int ENC_FMT_4096X2160_30             = 0x47;
    public final static int ENC_FMT_4096X2160_50             = 0x48;
    public final static int ENC_FMT_4096X2160_60             = 0x49;

    //cmcc format
    public final static int DISPLAY_HDMI_MODE_PCM            = 0;
    public final static int DISPLAY_HDMI_MODE_RAW            = 1;
    public final static int DISPLAY_HDMI_MODE_AUTO           = 2;

    public final static int DISPLAY_SPDIF_MODE_PCM           = 0;
    public final static int DISPLAY_SPDIF_MODE_RAW           = 1;

    //hisilicon standard
    public final static int OUTPUT_HDMI_MODE_LPCM            = 2;
    public final static int OUTPUT_HDMI_MODE_RAW             = 3;
    public final static int OUTPUT_HDMI_MODE_AUTO            = 1;

    public final static int OUTPUT_SPDIF_MODE_LPCM           = 2;
    public final static int OUTPUT_SPDIF_MODE_RAW            = 3;

    public Map<Integer,Integer> mMapEncFmtToIndex = new HashMap<Integer,Integer>();

    //The way to get this manager in apk is via:
    //this.getSystemService(Context.DISPLAY_MANAGER_SERVICE)
    public DisplayManager(IDisplayManager server) {
        mdisplay = server;

        //see display.c::get_hdmi_capability
        //                  hisi format value                fmt cap index
        mMapEncFmtToIndex.put(ENC_FMT_1080P_60                  ,   1 );
        mMapEncFmtToIndex.put(ENC_FMT_1080P_50                  ,   2 );
        mMapEncFmtToIndex.put(ENC_FMT_1080P_30                  ,   3 );
        mMapEncFmtToIndex.put(ENC_FMT_1080P_25                  ,   4 );
        mMapEncFmtToIndex.put(ENC_FMT_1080P_24                  ,   5 );
        mMapEncFmtToIndex.put(ENC_FMT_1080i_60                  ,   6 );
        mMapEncFmtToIndex.put(ENC_FMT_1080i_50                  ,   7 );
        mMapEncFmtToIndex.put(ENC_FMT_720P_60                   ,   8 );
        mMapEncFmtToIndex.put(ENC_FMT_720P_50                   ,   9 );
        mMapEncFmtToIndex.put(ENC_FMT_576P_50                   ,   10);
        mMapEncFmtToIndex.put(ENC_FMT_480P_60                   ,   11);
        mMapEncFmtToIndex.put(ENC_FMT_PAL                       ,   12);
        mMapEncFmtToIndex.put(ENC_FMT_NTSC                      ,   15);
        mMapEncFmtToIndex.put(ENC_FMT_3840X2160_24              ,   44);
        mMapEncFmtToIndex.put(ENC_FMT_3840X2160_25              ,   45);
        mMapEncFmtToIndex.put(ENC_FMT_3840X2160_30              ,   46);
        mMapEncFmtToIndex.put(ENC_FMT_3840X2160_50              ,   47);
        mMapEncFmtToIndex.put(ENC_FMT_3840X2160_60              ,   48);
        mMapEncFmtToIndex.put(ENC_FMT_4096X2160_24              ,   49);
        mMapEncFmtToIndex.put(ENC_FMT_4096X2160_25              ,   50);
        mMapEncFmtToIndex.put(ENC_FMT_4096X2160_30              ,   51);
        mMapEncFmtToIndex.put(ENC_FMT_4096X2160_50              ,   52);
        mMapEncFmtToIndex.put(ENC_FMT_4096X2160_60              ,   53);

        try{
            int[] dispCapability = mdisplay.getDisplayCapability();
            for(int i = 0; i < dispCapability.length; i++){
                Log.d("DisplayManager.java", "dispCapability[" + i + "]=" + dispCapability[i]);
            }

            if(dispCapability != null){
                int supportFmtCnt = 0;
                int[] supportFmt = new int[mAllDisplayStandard.length];
                for(int i = 0; i < mAllDisplayStandard.length; i++){
                    if(dispCapability[mMapEncFmtToIndex.get(covertCMCCFmtToHisi(mAllDisplayStandard[i]))] == 1){
                        supportFmt[supportFmtCnt] = mAllDisplayStandard[i];
                        supportFmtCnt++;
                        Log.d("DisplayManager.java", "supportFmt:" + mAllDisplayStandard[i]);
                    }
                    Log.d("DisplayManager.java", "supportFmtCnt:" + supportFmtCnt);
                }
                mStandard = new int[supportFmtCnt];
                System.arraycopy(supportFmt, 0, mStandard, 0, supportFmtCnt);
            }else{
                mStandard = new int[0];
            }
        }
        catch(Exception ex){
            mStandard = new int[0];
        }

    }

    public boolean isSupportStandard(int standard) {
        boolean ret = false;
        for (int i = 0; i < mStandard.length; i++) {
            if(standard == mStandard[i]){
                ret = true;
                break;
            }
        }
        return ret;
    }

    public int[] getAllSupportStandards() {
        return mStandard;
    }

    public void setDisplayStandard(int standard) {
        int hisiFmt = -1;
        int ret = -1;
        if(isSupportStandard(standard)){
            try {
                hisiFmt = covertCMCCFmtToHisi(standard);
                if (hisiFmt >= ENC_FMT_1080P_60) {
		    if("cm201_hebei".equals(SystemProperties.get("ro.ysten.province"))){
		    	mdisplay.SetOptimalFormatEnable(0);
		    }
                    ret = mdisplay.setFmt(hisiFmt);
                }
                //begin:ysten xumiao at 20190107 添加保存分辨率功能
                if(!"cm201_hebei".equals(SystemProperties.get("ro.ysten.province"))){
                    mdisplay.SetOptimalFormatEnable(0);
                    Log.e(TAG,"unspecified(" + standard + ")");
                    ret = mdisplay.setFmt(hisiFmt);
                }
                //end:ysten xumiao at 20190107 添加保存分辨率功能
            } catch(Exception ex) {
                Log.e(TAG,"setDisplayStandard: " + ex);
            }
        } else {
            Log.e(TAG, "setDisplayStandard: unsupport(" + standard + ")");
        }
        Log.i(TAG, "setDisplayStandard: standard=" + standard + ", ret=" + ret);
    }

    public int getCurrentStandard() {
        int hisiFmt = -1;
        int cmccFmt = -1;
        try {
            hisiFmt = mdisplay.getFmt();
            cmccFmt = covertHisiFmtToCMCC(hisiFmt);
        } catch (RemoteException e) {
            Log.e(TAG, "getCurrentStandard: " + e);
        }
        if(isSupportStandard(cmccFmt)){
            return cmccFmt;
        } else {
            Log.e(TAG, "getCurrentStandard: CMCC unsupport(" + hisiFmt + ")");
            return -1;
        }
    }

    private int covertHisiFmtToCMCC( int standard){
        int ret = -1;
        if( standard >= ENC_FMT_1080P_60 && standard <= ENC_FMT_PAL ){
            ret = standard;
        } else if( standard == ENC_FMT_NTSC) {
            ret = DISPLAY_STANDARD_NTSC;
        } else if( standard == ENC_FMT_3840X2160_24) {
            ret = DISPLAY_STANDARD_3840_2160P_24;
        } else if( standard == ENC_FMT_3840X2160_25) {
            ret = DISPLAY_STANDARD_3840_2160P_25;
        } else if( standard == ENC_FMT_3840X2160_30) {
            ret = DISPLAY_STANDARD_3840_2160P_30;
        } else if( standard == ENC_FMT_3840X2160_50) {
            ret = DISPLAY_STANDARD_3840_2160P_50;
        } else if( standard == ENC_FMT_3840X2160_60) {
            ret = DISPLAY_STANDARD_3840_2160P_60;
        } else if( standard == ENC_FMT_4096X2160_24) {
            ret = DISPLAY_STANDARD_4096_2160P_24;
        } else if( standard == ENC_FMT_4096X2160_25) {
            ret = DISPLAY_STANDARD_4096_2160P_25;
        } else if( standard == ENC_FMT_4096X2160_30) {
            ret = DISPLAY_STANDARD_4096_2160P_30;
        } else if( standard == ENC_FMT_4096X2160_50) {
            ret = DISPLAY_STANDARD_4096_2160P_50;
        } else if( standard == ENC_FMT_4096X2160_60) {
            ret = DISPLAY_STANDARD_4096_2160P_60;
        }
        Log.d(TAG, "covertHisiFmtToCMCC: covert [" + standard + "] to [" + ret + "]");
        return ret;
    }

    private int covertCMCCFmtToHisi( int standard){
        int ret = -1;
        if( standard >= DISPLAY_STANDARD_1080P_60 && standard <= DISPLAY_STANDARD_PAL ){
            ret = standard;
        } else if( standard == DISPLAY_STANDARD_NTSC) {
            ret = ENC_FMT_NTSC;
        } else if( standard == DISPLAY_STANDARD_3840_2160P_24) {
            ret = ENC_FMT_3840X2160_24;
        } else if( standard == DISPLAY_STANDARD_3840_2160P_25) {
            ret = ENC_FMT_3840X2160_25;
        } else if( standard == DISPLAY_STANDARD_3840_2160P_30) {
            ret = ENC_FMT_3840X2160_30;
        } else if( standard == DISPLAY_STANDARD_3840_2160P_50) {
            ret = ENC_FMT_3840X2160_50;
        } else if( standard == DISPLAY_STANDARD_3840_2160P_60) {
            ret = ENC_FMT_3840X2160_60;
        } else if( standard == DISPLAY_STANDARD_4096_2160P_24) {
            ret = ENC_FMT_4096X2160_24;
        } else if( standard == DISPLAY_STANDARD_4096_2160P_25) {
            ret = ENC_FMT_4096X2160_25;
        } else if( standard == DISPLAY_STANDARD_4096_2160P_30) {
            ret = ENC_FMT_4096X2160_30;
        } else if( standard == DISPLAY_STANDARD_4096_2160P_50) {
            ret = ENC_FMT_4096X2160_50;
        } else if( standard == DISPLAY_STANDARD_4096_2160P_60) {
            ret = ENC_FMT_4096X2160_60;
        }
        Log.d(TAG, "covertCMCCFmtToHisi: covert [" + standard + "] to [" + ret + "]");
        return ret;
    }

    public void setScreenMargin(int left, int top, int right, int bottom) {
        try {
            mdisplay.setOutRange(left, top, right,bottom);
        } catch (RemoteException e) {
            Log.e(TAG,"setScreenMargin: " + e);
        }
    }

    public int[] getScreenMargin() {
        int left = 0;
        int top = 0;
        int right = 0;
        int bottom = 0;
        try {
            Rect rect = mdisplay.getOutRange();
            left = rect.left;
            top = rect.top;
            right = rect.right;
            bottom = rect.bottom;
        } catch (RemoteException e) {
            // TODO Auto-generated catch block
            Log.e(TAG,"getScreenMargin: " + e);
        }
        return new int[] { left, top, right, bottom };
    }

    public void saveParams(){
        try {
            mdisplay.saveParams();
        } catch (RemoteException e) {
            Log.e(TAG,"saveParams: " + e);
        }
    }

    private int covertCMCC_HDMIPASSMode_ToHisi(int mode){
        int ret = -1;
        if(mode == DISPLAY_HDMI_MODE_PCM)//--0
            ret = OUTPUT_HDMI_MODE_LPCM;//--2
        else if(mode == DISPLAY_HDMI_MODE_RAW)//--1
            ret = OUTPUT_HDMI_MODE_RAW;//--3
        else if(mode == DISPLAY_HDMI_MODE_AUTO)
            ret = OUTPUT_HDMI_MODE_AUTO;
        else {
            Log.e(TAG,"Hdmi PassThrough mode:"+mode+" not define ret -1 ");
            return -1 ;
        }
        Log.d(TAG, "covertCMCC_HDMIPASSMODE_TO_ToHisi: covert [" + mode + "] to [" + ret + "]");
        return ret ;
    }

    private int covertCMCC_SPDIFMODE_TO_ToHisi(int mode){
        int ret = -1;
        if(mode == DISPLAY_SPDIF_MODE_PCM)
            ret = OUTPUT_SPDIF_MODE_LPCM;
        else if(mode == DISPLAY_SPDIF_MODE_RAW)
            ret = OUTPUT_SPDIF_MODE_RAW;
        else {
            Log.e(TAG,"Spdif mode mode:"+mode+" not define ret -1 ");
            return -1 ;
        }
        Log.d(TAG, "covertCMCC_SPDIFMODE_TO_ToHisi: covert [" + mode + "] to [" + ret + "]");
        return ret ;
    }

    /**
     * get the 3D support capability of tv
     *
     * @return true : support
     *        false : not support
     */
    public boolean isTVSupport3D() {
        boolean Tdflag = false;
        try {
            Tdflag = mdisplay.isSupport3D();
        } catch (RemoteException e) {
            Log.e(TAG,"isTVSupport3D : " + e);
        }
        return Tdflag;
    }

    /**
     * set HDMI pass through mode
     * * 1:auto,2:LPCM,3:RAW
     * @param mode
     *           0 DISPLAY_HDMI_MODE_PCM
     *           1 DISPLAY_HDMI_MODE_RAW
     *           2 DISPLAY_HDMI_MODE_AUTO
     */
    public void setHDMIPassThrough(int mode) {
        int ret = -1;
        ret = covertCMCC_HDMIPASSMode_ToHisi(mode);
        if(ret != -1)
        {
            try {
                ret = mdisplay.setHdmiPassThrough(ret);
                Log.i(TAG,"setHDMIPassThrough > mdisplay.setHdmiPassThrough(mode) ret ="+ret);
            } catch (RemoteException e) {
                Log.e(TAG,"setHDMIPassThrough: " + e);
            }
        }
        else
            Log.e(TAG,"invalid hdmi pass througt mode: " + ret);
    }

    /**
     * set SPDIF pass throught mode
     * @param mode
     *      0 DISPLAY_SPDIF_MODE_PCM
     *      1 DISPLAY_SPDIF_MODE_RAW
     */
    public void setSPDFPassThrough(int mode) {
        int ret = -1;
        ret = covertCMCC_SPDIFMODE_TO_ToHisi(mode);
        if(ret != -1){
            try {
                ret = mdisplay.setSpdfPassThrough(ret);
                Log.i(TAG,"setSPDFPassThrough > mdisplay.setSPDFPassThrough(mode) ret ="+ret);
            } catch (RemoteException e) {
                Log.e(TAG,"setSPDFPassThrough: " + e);
            }
        }
        else
            Log.e(TAG,"invalid spdif pass througt mode: " + ret);
    }

    /**
      * read the TV status by hdmi
     * @return -99 if TV not support CEC or HotPlug
     * -1  hdmi disconnect
     * 0   TV is standby
     * 1   TV is turn on
     */
    public int getTVState(){
        int ret = -2;
        try {
            ret = mdisplay.getTvStatus();
            Log.i(TAG,"getTVState ret ="+ret);
        } catch (RemoteException e) {
            Log.e(TAG,"getTVStatus: " + e);
        }
        return ret;
   }

    public byte[] getTVEDID(){

        int [] ret = {-1};
        try{
            ret = mdisplay.getTVEDID();
            Log.i(TAG,"get tv edid ret "+ret);
        }catch(RemoteException e){
            Log.e(TAG,"gettvedid: " + e);
        }

        byte [] r = new byte[ret.length];
        for(int i = 0; i < ret.length; i++){
            r[i] = (byte)ret[i];
        }
       return r;
    }

	//add by guangchao.su
	public HashMap<Integer, String> getAllSupportStandardsText() {
           HashMap<Integer, String> dis = new HashMap<Integer, String>();
	       dis.put(256, "HDMI 2160P 24Hz");
	       dis.put(257, "HDMI 2160P 25Hz");
	       dis.put(258, "HDMI 2160P 30Hz");
               dis.put(260, "HDMI 2160P 50Hz");
               dis.put(259, "HDMI 2160P 60Hz");
           dis.put(0, "HDMI 1080P 60Hz");
           dis.put(1, "HDMI 1080P 50Hz");
           dis.put(2, "HDMI 1080P 30Hz");
           dis.put(3, "HDMI 1080P 25Hz");
           dis.put(4, "HDMI 1080P 24Hz");
           dis.put(5, "HDMI 1080I 60Hz");
           dis.put(6, "HDMI 1080I 50Hz");
           dis.put(7, "HDMI 720P 60Hz");
           dis.put(8, "HDMI 720P 50Hz");
           dis.put(9, "HDMI 576P 50Hz");
           dis.put(10, "HDMI 480P 60Hz");
           dis.put(11, "PAL");
           dis.put(12, "NTSC");
           return dis;
       }
	   
    public void setDisplayFmt(int format) {
        int hisiFmt = -1;
        int ret = -1;
        if(isSupportStandard(format)){
            try {
                hisiFmt = covertCMCCFmtToHisi(format);
                if (hisiFmt >= ENC_FMT_1080P_60) {
		    if("cm201_hebei".equals(SystemProperties.get("ro.ysten.province"))){
		    	mdisplay.SetOptimalFormatEnable(0);
		    }
                    ret = mdisplay.setFmt(hisiFmt);
                }
                //begin:ysten xumiao at 20190107 添加保存分辨率功能
                if(!"cm201_hebei".equals(SystemProperties.get("ro.ysten.province"))){
                    mdisplay.SetOptimalFormatEnable(0);
                    Log.e(TAG,"unspecified(" + format + ")");
                    ret = mdisplay.setFmt(hisiFmt);
                }
                //end:ysten xumiao at 20190107 添加保存分辨率功能
            } catch(Exception ex) {
                Log.e(TAG,"setDisplayFmt: " + ex);
            }
        } else {
            Log.e(TAG, "setDisplayFmt: unsupport(" + format + ")");
        }
        Log.i(TAG, "setDisplayFmt: format=" + format + ", ret=" + ret);
    }

    public int getDisplayFmt() {

        int hisiFmt = -1;
        int cmccFmt = -1;
        try {
            hisiFmt = mdisplay.getFmt();
            cmccFmt = covertHisiFmtToCMCC(hisiFmt);
        } catch (RemoteException e) {
            Log.e(TAG, "getDisplayFmt: " + e);
        }
        if(isSupportStandard(cmccFmt)){
            return cmccFmt;
        } else {
            Log.e(TAG, "getDisplayFmt: CMCC unsupport(" + hisiFmt + ")");
            return -1;
        }

    }

    public int[] getAllFmts() {
        return mAllDisplayStandard;
    }

    public int[] getEDIDSupportFmts() {
        return mStandard;
    }

}
