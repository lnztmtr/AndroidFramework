package com.hisilicon.android.hidisplaymanager;

import android.util.Log;
import android.os.ServiceManager;
import android.graphics.Rect;
import com.hisilicon.android.hidisplaymanager.DispFmt;
import com.hisilicon.android.hidisplaymanager.IHdmiListener;
import android.os.RemoteException;
import java.lang.RuntimeException;
import android.os.SystemProperties;
/**
 * HiDisplayManager interface.
 */
public class HiDisplayManager  {

    /**
     * Display format: 1080p60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_1080P_60 = 0;
    /**
     * Display format: 1080p50hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_1080P_50 = 1;
    /**
     * Display format: 1080p30hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_1080P_30 = 2;
    /**
     * Display format: 1080p25hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_1080P_25 = 3;
    /**
     * Display format: 1080p24hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_1080P_24 = 4;
    /**
     * Display format: 1080i60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_1080i_60 = 5;
    /**
     * Display format: 1080i50hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_1080i_50 = 6;
    /**
     * Display format: 720p60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_720P_60 = 7;
    /**
     * Display format: 720p50hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_720P_50 = 8;
    /**
     * Display format: 576p50hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_576P_50 = 9;
    /**
     * Display format: 480p60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_480P_60 = 10;
    /**
     * Display format: BDGHIPAL.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_PAL = 11;
    /**
     * Display format: (N)PAL.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_PAL_N = 12;
    /**
     * Display format: (Nc)PAL.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_PAL_Nc = 13;
    /**
     * Display format: (M)NTSC.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_NTSC = 14;
    /**
     * Display format: NTSC-J.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_NTSC_J = 15;
    /**
     * Display format: (M)PAL.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_NTSC_PAL_M = 16;
    /**
     * Display format: SECAM_SIN.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_SECAM_SIN = 17;
    /**
     * Display format: SECAM_COS.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_SECAM_COS = 18;

    /**
     * Display format: 1080p24hz frame packing.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_1080P_24_FRAME_PACKING = 19;
    /**
     * Display format: 720p60hz frame packing.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_720P_60_FRAME_PACKING = 20;
    /**
     * Display format: 720p50hz frame packing.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_720P_50_FRAME_PACKING = 21;

    /**
     * Display format: 861D 640x480 60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_861D_640X480_60 = 22;
    /**
     * Display format: VESA 800x600 60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_800X600_60 = 23;
    /**
     * Display format: VESA 1024x768 60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_1024X768_60 =  24;
    /**
     * Display format: VESA 1280x720 60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_1280X720_60 = 25;
    /**
     * Display format: VESA 1280x800 60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_1280X800_60 = 26;
    /**
     * Display format: VESA 1280x1024 60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_1280X1024_60 = 27;
    /**
     * Display format: VESA 1360x768 60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_1360X768_60 = 28;
    /**
     * Display format: VESA 1366x768 60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_1366X768_60 = 29;
    /**
     * Display format: VESA 1400x1050 60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_1400X1050_60 =   30;
    /**
     * Display format: VESA 1440x900 60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_1440X900_60 =    31;
    /**
     * Display format: VESA 1440x900 60hz RB.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_1440X900_60_RB = 32;
    /**
     * Display format: VESA 1600x900 60hz RB.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_1600X900_60_RB =  33;
    /**
     * Display format: VESA 1600x1200 60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_1600X1200_60 =   34;
    /**
     * Display format: VESA 1680x1050 60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_1680X1050_60 =   35;
    /**
     * Display format: VESA 1680x1050 60hz RB.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_1680X1050_60_RB =  36;
    /**
     * Display format: VESA 1920x1080 60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_1920X1080_60 =   37;
    /**
     * Display format: VESA 1920x1200 60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_1920X1200_60 =   38;
    /**
     * Display format: VESA 1920x1440 60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_1920X1440_60 =   39;
    /**
     * Display format: VESA 2048x1152 60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_2048X1152_60 =   40;
    /**
     * Display format: VESA 2560x1440 60hz RB.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_2560X1440_60_RB =   41;
    /**
     * Display format: VESA 2560x1600 60hz RB.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_VESA_2560X1600_60_RB =   42;

    /**
     * Display format: 3840x2160p 24hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_3840X2160_24             = 0x40;
    /**
     * Display format: 3840x2160p 25hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_3840X2160_25             = 0x41;
    /**
     * Display format: 3840x2160p 30hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_3840X2160_30             = 0x42;
    /**
     * Display format: 3840x2160p 50hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_3840X2160_50             = 0x43;
    /**
     * Display format: 3840x2160p 60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_3840X2160_60             = 0x44;

    /**
     * Display format: 4096x2160p 24hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_4096X2160_24             = 0x45;
    /**
     * Display format: 4096x2160p 25hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_4096X2160_25             = 0x46;
    /**
     * Display format: 4096x2160p 30hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_4096X2160_30             = 0x47;
    /**
     * Display format: 4096x2160p 50hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_4096X2160_50             = 0x48;
    /**
     * Display format: 4096x2160p 60hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_4096X2160_60             = 0x49;

    /**
     * Display format: 3840x2160p 23.976hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_3840X2160_23_976         = 0x4a;
    /**
     * Display format: 3840x2160p 29.97hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_3840X2160_29_97          = 0x4b;
    /**
     * Display format: 720p59.94hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_720P_59_94               = 0x4c;
    /**
     * Display format: 1080p59.94hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_1080P_59_94              = 0x4d;
    /**
     * Display format: 1080p29.97hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_1080P_29_97              = 0x4e;
    /**
     * Display format: 1080p23.976hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_1080P_23_976             = 0x4f;
    /**
     * Display format: 1080i59.94hz.<br>
     * It must be defined same as device/hisilicon/bigfish/frameworks/hidisplaymanager/libs/displaydef.h.
     */
    public final static int ENC_FMT_1080i_59_94              = 0x50;

    /** 3D mode. Type: 2D mode */
    public final static int HI_DISP_MODE_NORMAL = 0;
    /** 3D mode. Type: frame packing */
    public final static int HI_DISP_MODE_FRAME_PACKING = 1;
    /** 3D mode. Type: side by side half */
    public final static int HI_DISP_MODE_SIDE_BY_SIDE = 2;
    /** 3D mode. Type: top and bottom */
    public final static int HI_DISP_MODE_TOP_BOTTOM = 3;

    /** Video color space mode. Type: RGB444 output mode */
    public final static int HI_HDMI_VIDEO_MODE_RGB444 = 0;
    /** Video color space mode. Type: YCBCR422 output mode */
    public final static int HI_HDMI_VIDEO_MODE_YCBCR422 = 1;
    /** Video color space mode. Type: YCBCR444 output mode */
    public final static int HI_HDMI_VIDEO_MODE_YCBCR444 = 2;
    /** Video color space mode. Type: YCBCR420 output mode */
    public final static int HI_HDMI_VIDEO_MODE_YCBCR420 = 3;

    /** HDMI deep color mode. Type: HDMI deep color 8bit mode */
    public final static int HI_HDMI_DEEP_COLOR_8BIT = 0;
    /** HDMI deep color mode. Type: HDMI deep color 10bit mode */
    public final static int HI_HDMI_DEEP_COLOR_10BIT = 1;

    /** stb out put hdr type (on android layer)*/
    public final static int HI_HDR_TYPE_AUTO        = -1;//android layer auto, base on hdmi tv cap
    public final static int HI_HDR_TYPE_SDR         = 0;
    public final static int HI_HDR_TYPE_DOLBY       = 2;
    public final static int HI_HDR_TYPE_HDR10       = 3;
    public final static int HI_HDR_TYPE_HLG         = 4;

    /**xdr engine ,must keep same as HI_DRV_DISP_XDR_ENGINE_E in  hi_drv_disp.h */
    public final static int XDR_ENGINE_AUTO = 0;
    public final static int XDR_ENGIN_DOLBY = 5;

    private IHiDisplayManager m_Display;
    private static String TAG = "DisplayManagerClient";

    /**
     * Constructor.
     * <p>
     * Constructor of class HiDisplayManager.<br>
     * <br>
     */
    public HiDisplayManager()
    {
        m_Display = IHiDisplayManager.Stub.asInterface(ServiceManager.getService("HiDisplay"));
    }

    /**
     * Set image brightness.
     * <p>
     * Set the image brightness value.<br>
     * <br>
     * @param brightness image brightness value. The range is 0~100, and 0 means the min brightness value.
     * @return 0 if the brightness is set successfully, -1 otherwise.
     */
    public int setBrightness(int brightness)
    {
        try
        {
            return m_Display.setBrightness(brightness);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Regist listener for oberser the event from native service.
     *include the hdmi events and display format changed event.
     * @return 0 regist successfully, -1 otherwise.
     */
    public int registerListener(IHdmiListener listener)
    {
        try
        {
            return m_Display.registerListener(listener);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }
    }

    /**
     * unregist the listener registed before while app destroy.
     * @return 0 regist successfully, -1 otherwise.
     */
    public int unRegisterListener(IHdmiListener listener)
    {
        try
        {
            return m_Display.unRegisterListener(listener);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }


    /**
     * Get image brightness.
     * <p>
     * Get the current image brightness value.<br>
     * <br>
     * @return the current image brightness if getting successfully, -1 otherwise.
     */
    public int getBrightness()
    {
        try
        {
            return m_Display.getBrightness();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Set image saturation.
     * <p>
     * Set the image saturation value.<br>
     * <br>
     * @param saturation image saturation value. The range is 0~100, and 0 means the min saturation value.
     * @return 0 if the saturation is set successfully, -1 otherwise.
     */
    public int setSaturation(int saturation)
    {
        try
        {
            return m_Display.setSaturation(saturation);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get image saturation.
     * <p>
     * Get the current image saturation value.<br>
     * <br>
     * @return the current image saturation if getting successfully, -1 otherwise.
     */
    public int getSaturation()
    {
        try
        {
            return m_Display.getSaturation();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Set image contrast.
     * <p>
     * Set the image contrast value.<br>
     * <br>
     * @param contrast image contrast value. The range is 0~100, and 0 means the min contrast value.
     * @return 0 if the contrast is set successfully, -1 otherwise.
     */
    public int setContrast(int contrast)
    {
        try
        {
            return m_Display.setContrast(contrast);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get image contrast.
     * <p>
     * Get the current image contrast value.<br>
     * <br>
     * @return the current image contrast if getting successfully, -1 otherwise.
     */
    public int getContrast()
    {
        try
        {
            return m_Display.getContrast();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Set image hue.
     * <p>
     * Set the image hue value.<br>
     * <br>
     * @param hue image hue value. The range is 0~100, and 0 means the min hue value.
     * @return 0 if the hue is set successfully, -1 otherwise.
     */
    public int setHue(int hue)
    {
        try
        {
            return m_Display.setHue(hue);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get image hue.
     * <p>
     * Get the current image hue value.<br>
     * <br>
     * @return the current image hue if getting successfully, -1 otherwise.
     */
    public int getHue()
    {
        try
        {
            return m_Display.getHue();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Set display format.
     * <p>
     * Set the display format value.<br>
     * <br>
     * @param fmt display format.
     * @return 0 if the format is set successfully, -1 otherwise.
     */
    public int setFmt(int fmt)
    {
        try
        {
            return m_Display.setFmt(fmt);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get display format.
     * <p>
     * Get the current display format value.<br>
     * <br>
     * @return the current display format if getting successfully, -1 otherwise.
     */
    public int getFmt()
    {
        try
        {
            return (int)m_Display.getFmt();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Set color space mode and deep color mode.
     * <p>
     * Set video color space mode and HDMI deep color mode.<br>
     * <br>
     * @param colorSpace video color space mode.
     * @param deepColor HDMI deep color mode.
     * @return 0 if the modes are set successfully, -1 otherwise.
     */
    public int setColorSpaceAndDeepColor(int colorSpace, int deepColor)
    {
        try
        {
            return m_Display.setColorSpaceAndDeepColor(colorSpace, deepColor);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get video color space mode.
     * <p>
     * Get the current video color space mode.<br>
     * <br>
     * @return the current color space if getting successfully, -1 otherwise.
     */
    public int getColorSpaceMode()
    {
        try
        {
            return (int)m_Display.getColorSpaceMode();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get HDMI deep color mode.
     * <p>
     * Get the current HDMI deep color mode.<br>
     * <br>
     * @return the current deep color if getting successfully, -1 otherwise.
     */
    public int getDeepColorMode()
    {
        try
        {
            return (int)m_Display.getDeepColorMode();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }


    /**
     * Set screen display area size.
     * <p>
     * Set the size of screen display area.<br>
     * The size is described by offsets of display area in real screen.
     * <br>
     * @param left left offset in pixel. The range is [0, 200].
     * @param top top offset in pixel. The range is [0, 200].
     * @param right right offset in pixel. The range is [0, 200].
     * @param bottom bottom offset in pixel. The range is [0, 200].
     * @return 0 if the size is set successfully, -1 otherwise.
     */
    public int setGraphicOutRange(int left, int top, int right, int bottom)
    {
        try
        {
            return m_Display.setOutRange(left, top, right, bottom);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get screen display area size.
     * <p>
     * Get the size of screen display area.<br>
     * The size is described by offsets of display area in real screen.
     * <br>
     * @return a Rect object if getting successfully, null otherwise.
     */
    public Rect getGraphicOutRange()
    {
        try
        {
            return m_Display.getOutRange();
        }
        catch(RuntimeException ex)
        {
            return null;
        }
        catch(RemoteException ex)
        {
            return null;
        }

    }

    /**
     * Set Macrovision mode.
     * <p>
     * Set Macrovision output type.<br>
     * <br>
     * @param mode Macrovision mode.
     * @return 0 if the mode is set successfully, -1 otherwise.
     */
    public int setMacroVision(int mode)
    {
        try
        {
            return m_Display.setMacroVision(mode);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get Macrovision mode.
     * <p>
     * Get the current Macrovision output type.<br>
     * <br>
     * @return the current Macrovision mode if getting successfully, -1 otherwise.
     */
    public int getMacroVision()
    {
        try
        {
            return m_Display.getMacroVision();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Set HDCP mode.
     * <p>
     * Set status of HDCP mode.<br>
     * <br>
     * @param enabled enabled status of HDCP mode.
     * @return 0 if the mode is set successfully, -1 otherwise.
     */
    public int setHdcp(boolean enabled)
    {
        try
        {
            return m_Display.setHdcp(enabled);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get HDCP mode.
     * <p>
     * Get the current enabled status of HDCP mode.<br>
     * <br>
     * @return the current enabled status if getting successfully, -1 otherwise.
     */
    public int getHdcp()
    {
        try
        {
            return m_Display.getHdcp();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Set 3D output mode.
     * <p>
     * Set 3D display mode and adapt format base on videoFps.<br>
     * The format will be decided by video fps.
     * <br>
     * @param mode 3D mode :
     *          0   2D mode
     *          1   3D frame packing mode
     *          2   3D side by side mode
     *          3   3D top and bottom mode
     * e.g. see as #HI_DISP_MODE_SIDE_BY_SIDE
     * @param videoFps The frame frequency persecond of Stream
     * @return 0 if set successfully, -1 otherwise.
     */
    public int set3DMode(int mode, int videoFps)
    {
        try
        {
            if (mode < HI_DISP_MODE_NORMAL || mode > HI_DISP_MODE_TOP_BOTTOM)
            {
                Log.e(TAG, "3D mode must be 0, 1, 2 or 3. Please check it.");
                return -1;
            }
            return m_Display.setStereoOutMode(mode, videoFps);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get 3D output mode.
     * <p>
     * Get 3D display mode.<br>
     * <br>
     * @return the current 3D mode if getting successfully, -1 otherwise.
     */
    public int get3DMode()
    {
        try
        {
            return m_Display.getStereoOutMode();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Set right eye priority.
     * <p>
     * Set right eye first for 3D output.<br>
     * <br>
     * @param priority priority of right eye. The range is [0, 1].
     * @return 0 if the priority is set successfully, -1 otherwise.
     */
    public int setRightEyeFirst(int priority)
    {
        try
        {
            return m_Display.setRightEyeFirst(priority);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get right eye priority.
     * <p>
     * Get the current right eye priority for 3D output.<br>
     * <br>
     * @return the current priority if getting successfully, -1 otherwise.
     */
    public int getRightEyeFirst()
    {
        try
        {
            return m_Display.getRightEyeFirst();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get HDMI sink capability.
     * <p>
     * Get the capability of HDMI sink.<br>
     * The capability is described by class DispFmt.
     * <br>
     * @return a DispFmt object if getting successfully, null otherwise.
     */
    public DispFmt getDisplayCapability()
    {
        try
        {
            return m_Display.getDisplayCapability();
        }
        catch(RuntimeException ex)
        {
            return null;
        }
        catch(RemoteException ex)
        {
            return null;
        }

    }
    /**
     * Get TV Manufacture Information.
     * <p>
     * Get TV Manufacture Information.<br>
     * The Information can be obtained includes TV manufacture name,TV sink name,
     * Product code,Serial numeber of Manufacture,the week of manufacture,the year of manufacture.
     * <br>
     * @return a ManufactureInfo object if getting successfully, null otherwise.
     */
    public ManufactureInfo getManufactureInfo()
    {
        try
        {
            return m_Display.getManufactureInfo();
        }
        catch(RuntimeException ex)
        {
            return null;
        }
        catch(RemoteException ex)
        {
            return null;
        }
    }
    /**
     * Set video aspect ratio.
     * <p>
     * Set video aspect ratio.<br>
     * Set aspect ratio attribute of display device.
     * <br>
     * @param ratio aspect ratio. 0 auto, 1 4:3, 2 16:9.
     * @return 0 if the ratio is set successfully, -1 otherwise.
     */
    public int setAspectRatio(int ratio)
    {
        try
        {
            return m_Display.setAspectRatio(ratio);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get video aspect ratio.
     * <p>
     * Get the current video aspect ratio.<br>
     * <br>
     * @return the current aspect ratio if getting successfully(0 auto, 1 4:3, 2 16:9), -1 otherwise.
     */
    public int getAspectRatio()
    {
        try
        {
			//解决福建302H恢复出厂设置后，宽高比选项默认为“全屏”选项
	    if(SystemProperties.get("ro.ysten.province","master").equals("m302h_fujian")
				&& SystemProperties.get("persist.sys.default_ratio","0").equals("0")){
					SystemProperties.set("persist.sys.default_ratio","1");
				return 0;
			}else{
				return m_Display.getAspectRatio();
			}
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Set maintain aspect ratio.
     * <p>
     * Set maintain aspect ratio.<br>
     * <br>
     * @param cvrs aspect ratio. 0 extrude, 1 add black.
     * @return 0 if the ratio is set successfully, -1 otherwise.
     */
    public int setAspectCvrs(int cvrs)
    {
        try
        {
            return m_Display.setAspectCvrs(cvrs);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get maintain aspect ratio.
     * <p>
     * Get the current maintain aspect ratio.<br>
     * <br>
     * @return the current cvrs if getting successfully(0 extrude, 1 add black), -1 otherwise.
     */
    public int getAspectCvrs()
    {
        try
        {
            return m_Display.getAspectCvrs();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Set optimal display format.
     * <p>
     * Set the enabled status of optimal display format.<br>
     * <br>
     * @param enabled enabled status of optimal display format. 0 disabled, 1 enabled.
     * @return 0 if the status is set successfully, -1 otherwise.
     */
    public int setOptimalFormatEnable(int enabled)
    {
        try
        {
            return m_Display.setOptimalFormatEnable(enabled);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get optimal display format.
     * <p>
     * Whether optimal display format is enabled or not.<br>
     * <br>
     * @return the current status if getting successfully(0 disabled, 1 enabled), -1 otherwise.
     */
    public int getOptimalFormatEnable()
    {
        try
        {
            return m_Display.getOptimalFormatEnable();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get display device type.
     * <p>
     * Get the current display device type.<br>
     * <br>
     * @return the current device type if getting successfully(0 hdmi is not connected, 1 tv, 2 pc), -1 otherwise.
     */
    public int getDisplayDeviceType()
    {
        try
        {
            return m_Display.getDisplayDeviceType();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Save parameter.
     * <p>
     * Save display parameters.<br>
     * <br>
     * @return 0 if parameters is saved successfully, -1 otherwise
     */
    public int saveParam()
    {
        try
        {
            return m_Display.saveParam();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Attach interface.
     * <p>
     * Attach the interface by setting display interface parameter.<br>
     * <br>
     * @return 0 if attaching successfully, -1 otherwise.
     */
    public int attachIntf()
    {
        try
        {
            return m_Display.attachIntf();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Detach interface.
     * <p>
     * Detach the interface by canceling display interface parameter.<br>
     * <br>
     * @return 0 if detaching successfully, -1 otherwise.
     */
    public int detachIntf()
    {
        try
        {
            return m_Display.detachIntf();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }
    }

    /**
     * Set virtual screen.
     * <p>
     * Set the resolution of virtual screen.<br>
     * <br>
     * @param outFmt resolution of virtual screen. 0: width 1280, height 720; 1: width 1920, height 1080.
     * @return 0 if the resolution is set successfully, -1 otherwise.
     */
    public int setVirtScreen(int outFmt)
    {
        try
        {
            return m_Display.setVirtScreen(outFmt);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get virtual screen.
     * <p>
     * Get the current resolution of virtual screen.<br>
     * <br>
     * @return the current resolution if getting successfully(0 720p, 1 1080p), -1 otherwise.
     */
    public int getVirtScreen()
    {
        try
        {
            return m_Display.getVirtScreen();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }
    }

    /**
     * Get virtual screen size.
     * <p>
     * Get the current size of virtual screen.<br>
     * <br>
     * @return the current virtual screen rect if getting successfully, null otherwise.
     */
    public Rect getVirtScreenSize()
    {
        try
        {
            return m_Display.getVirtScreenSize();
        }
        catch(RuntimeException ex)
        {
            return null;
        }
        catch(RemoteException ex)
        {
            return null;
        }
    }

    /**
     * Reset parameter.
     * <p>
     * Reset display parameters to default values.<br>
     * <br>
     * @return 0 if parameters is reset successfully, -1 otherwise
     */
    public int reset()
    {
        try
        {
            return m_Display.reset();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Set hdmi suspend time.
     * <p>
     * Set the delay time of hdmi suspend.<br>
     * <br>
     * @param time delay time in millisecond.
     * @return 0 if the time is set successfully, -1 otherwise.
     */
    public int setHDMISuspendTime(int time)
    {
        try
        {
            return m_Display.setHDMISuspendTime(time);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get hdmi suspend time.
     * <p>
     * Get the current delay time of hdmi suspend.<br>
     * <br>
     * @return the current time if getting successfully, -1 otherwise.
     */
    public int getHDMISuspendTime()
    {
        try
        {
            return m_Display.getHDMISuspendTime();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Set hdmi suspend enable.
     * <p>
     * Set the enabled status of hdmi suspend.<br>
     * <br>
     * @param enabled enabled status of hdmi suspend. 0 disabled, 1 enabled.
     * @return 0 if the status is set successfully, -1 otherwise.
     */
    public int setHDMISuspendEnable(int enabled)
    {
		 Log.d("zhanghui", "henan run setHDMISuspendEnable() and enable is"+enabled);
        try
        {
            return m_Display.setHDMISuspendEnable(enabled);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get hdmi suspend enable.
     * <p>
     * Whether hdmi suspend is enabled or not.<br>
     * <br>
     * @return the current status if getting successfully(0 disabled, 1 enabled), -1 otherwise.
     */
    public int getHDMISuspendEnable()
    {
        try
        {
            return m_Display.getHDMISuspendEnable();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Reload parameter.
     * <p>
     * Reload display parameters from baseparam.img.<br>
     * <br>
     * @return 0 if parameters is reloaded successfully, -1 otherwise
     */
    public int reload()
    {
        try
        {
            return m_Display.reload();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Set output port enable.
     * <p>
     * Set output port status -- enable output port or not.<br>
     * <br>
     * @param port output port. 0 -> HDMI, 1 -> CVBS, 2 -> YPbPr.
     * @param enable enable status. 0 -> disabled (close this port), 1 -> enabled (open this port).
     * @return 0 if the status is set successfully, -1 otherwise
     */
    public int setOutputEnable(int port, int enable)
    {
        try
        {
            return m_Display.setOutputEnable(port, enable);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * Get output port enable.
     * <p>
     * Get enable status of output port.<br>
     * <br>
     * @param port output port. 0 -> HDMI, 1 -> CVBS, 2 -> YPbPr.
     * @return 0 disabled, -1 enabled
     */
    public int getOutputEnable(int port)
    {
        try
        {
            return m_Display.getOutputEnable(port);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     *
     * get HDR Type from vo drv
     * @return 0 -> SDR, 2 -> DOLBY, 3 -> HDR10,4--> HLG
     *         1 : is auto mode define in vo drv,
     *             but not support currenttly
     */
    public int getHDRType()
    {
        try
        {
            return m_Display.getHDRType();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
     * set hdr out put mode
     *  0 -> SDR, 2 -> DOLBY, 3 -> HDR10,4--> HLG, -1 AUTO base on tv cap
     *
     * @return 0 successfully, -1 otherwise
     */
    public int setHDRType(int type)
    {
        try
        {
            return m_Display.setHDRType(type);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
    * set color gamut to vo drv
    *
    * @return 0 if the status is set successfully, -1 otherwise
    */
    public int setColorGamut(int type) {
        try
        {
            return m_Display.setColorGamut(type);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
    * get color gamut from vo drv
    *
    * @return successfully : color gamut value , otherwise -1
    */
    public int getColorGamut() {
        try
        {
            return m_Display.getColorGamut();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    public int getstereoDepth()
    {
        try
        {
            return m_Display.getStereoDepth();
        }
        catch(RuntimeException ex)
        {
            return -1;
        } catch(RemoteException ex)
        {
            return -1;
        }
    }
    public int setStereoDepth(int depth){
        try
        {
            return m_Display.setStereoDepth(depth);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }
    }
    /**
     * set CEC suspend
     * <p>
     * Get enable status of output port.<br>
     * <br>
     * @param port output port. 0 -> HDMI, 1 -> CVBS.
     * @return 0 disabled, -1 enabled
     */
    public int setCECSuspend()
    {
        try
        {
            return m_Display.setCECSuspend();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }
    public int getHDMICECSuspendEnable(){
        try
        {
            return m_Display.getHDMICECSuspendEnable();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }
    public int setHDMICECSuspendEnable(int enable){
        try
        {
            return m_Display.setHDMICECSuspendEnable(enable);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }
    /**
     * check the chip's dolby support capabliby
     *
     * @return 1 : support  others : _notsupport
     */
    public int checkChipDolbyCapablity(){
        try
        {
            return m_Display.checkChipDolbyCapablity();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
    * set HDMI Open
    *
    * @return 0 if the status is set successfully, -1 otherwise
    */
    public int setHDMIOpen(){
        try
        {
            return m_Display.setHDMIOpen();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    /**
    * set HDMI Close
    *
    * @return 0 if the status is set successfully, -1 otherwise
    */
    public int setHDMIClose(){
        try
        {
            return m_Display.setHDMIClose();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }
    /**
     * load the 332 byte hdcp EncryptedKey form deviceinfo zone
     * <p>
     * @return 0 -> invalide, 1 -> OK
     */
    public int loadHDCPKey()
    {
        try
        {
            return m_Display.loadHDCPKey();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }
    /**
     * get Hdmi HDCP enable
     * <p>
      * @return 1 enable, 0 disable
     */
    public int isHdmiHDCPEnable()
    {
        try
        {
            return m_Display.isHdmiHDCPEnable();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }

    }

    public int getTVStatus(){
        try
        {
            return m_Display.getTVStatus();
        }
        catch(RuntimeException ex)
        {
            return -2;
        }
        catch(RemoteException ex)
        {
            return -2;
        }

    }


    /**
     * Set xdr engine
        * @param engine : engine mode
        * @see e.g: #XDR_ENGIN_SDR above.
     * @return 0 if the saturation is set successfully, -1 otherwise.
     */
    /** {@hide}  */
    public int setXdrEngine(int engine)
    {
        if(engine != XDR_ENGINE_AUTO
            && engine != XDR_ENGIN_DOLBY)
        {
            Log.e(TAG,"Only support to set auto and dolby engine !");
            return -1;
        }
        try
        {
            return m_Display.setXdrEngine(engine);
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }
    }

    /**
     * get current xdr engine
     *
     * @return current xdr engine , @see e.g: #XDR_ENGIN_SDR above.
     *-1 otherwise.
     */
    /** {@hide}  */
    public int getXdrEngine()
    {
        try
        {
            return m_Display.getXdrEngine();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }
    }

    /**
     * clearResumeState interface.
     * <p>
     * Clear Resume State for hdmi.<br>
     * <br>
     * @return 0 if detaching successfully, -1 otherwise.
     */
    public int clearResumeState()
    {
        try
        {
            return m_Display.clearResumeState();
        }
        catch(RuntimeException ex)
        {
            return -1;
        }
        catch(RemoteException ex)
        {
            return -1;
        }
    }
}
