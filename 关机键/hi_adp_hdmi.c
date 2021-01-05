#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <cutils/log.h>
#include <pthread.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include "hi_unf_hdmi.h"
#include "hi_unf_edid.h"
#include "hi_unf_disp.h"
#include "hi_unf_pdm.h"
#include "hi_adp.h"
#include "hi_adp_hdmi.h"
#include <hidisplay.h>
#include <cutils/properties.h>
#include "hi_drv_vinput.h"
#include "hi_flash.h"
#include <time.h>

#define DEBUG_HDMI_INIT 1
#define BUFLEN  PROP_VALUE_MAX
#undef LOG_TAG
#define LOG_TAG "HI_ADP_HDMI"
#define HI_UNF_DISP_HD0 (HI_UNF_DISPLAY1)
#define BUFLEN  PROP_VALUE_MAX


#define KPC_EV_KEY_PRESS          (0x1)
#define KPC_EV_KEY_RELEASE        (0x0)

#define Vinput_FILE               "/dev/vinput"

HI_U8 hdmiEDIDBuf[512];
HI_U32 hdmiEDIDBuflen = 512;
HI_U32 hdmiEDIDFlag = 0;
HI_S32 TvPowerStatus = TV_UNKNOW; //TV_STANDBY;//off
HI_U8 checktimes = 0;
#define DAY_SECOND  86400

HI_BOOL TV_feedback_flag = HI_FALSE;
//hidisplay debug log flag
HI_U8 dbuglog = HI_FALSE;
HI_BOOL alrm_set_flag = HI_FALSE;// alrm default not set
HI_BOOL alrm_cancel_flag = HI_TRUE;//default hasn't alrm, same as cancel
HI_BOOL isEdidChange = HI_TRUE;
HI_UNF_HDMI_EVENT_TYPE_E suspendevent = HI_UNF_HDMI_EVENT_HOTPLUG;

typedef struct hiHDMI_ARGS_S
{
    HI_UNF_HDMI_ID_E  enHdmi;
}HDMI_ARGS_S;

static HDMI_ARGS_S g_stHdmiArgs;
HI_BOOL Hdcp14KEYLoadFlag   = HI_FALSE;
HI_U32 g_HDMI_Bebug       = HI_FALSE;

HI_U32 g_enDefaultMode    = HI_UNF_HDMI_DEFAULT_ACTION_HDMI;//HI_UNF_HDMI_DEFAULT_ACTION_NULL;
HI_UNF_HDMI_CALLBACK_FUNC_S g_stCallbackFunc;
HI_UNF_HDMI_CALLBACK_FUNC_S g_stCallbackSleep;
HI_U32 get_HDMI_Suspend_Time();

extern int displayType;
extern int cvbs_dac_port;
extern HI_BOOL hdmi_enable;
extern HI_BOOL cvbs_enable;
extern HI_U32 TVHMax ;
extern HI_U32 TVWMax ;
extern colorspace_deepcolor_match_tyep_e clr_match_type;
extern void store_format(display_format_e format, display_format_e format_sd);

extern display_format_e mFormat;
extern HI_BOOL attachtodisp1;
extern HI_BOOL is_restart ;
extern HI_BOOL Dolby_cert ;
extern HI_BOOL is_playing_dolby;
extern int match_format_colorspace( display_format_e format, HI_UNF_HDMI_VIDEO_MODE_E *curcolorspace, HI_UNF_HDMI_DEEP_COLOR_E *curdeepcolor);
extern struct display_context_t *dispCtx;
extern HI_VOID notify_callback(hdmi_event_t *ev);
HI_VOID *pwthread_loop(void* arg);

HI_VOID HDMI_Suspend_Callback(HI_UNF_HDMI_EVENT_TYPE_E event, HI_VOID *pPrivateData);
HI_BOOL is_format_support(HI_UNF_EDID_BASE_INFO_S *stSinkCap,HI_UNF_ENC_FMT_E format);
HI_S32 set_HDMI_Suspend_Enable(int iEnable);
HI_S32 get_HDMI_Suspend_Enable();
HI_BOOL InitComplete = HI_FALSE;
User_HDMI_CallBack pfnHdmiUserCallback = NULL;
HI_UNF_HDMI_STATUS_S           stHdmiStatus;
HI_UNF_HDMI_ATTR_S             stHdmiAttr;
HI_UNF_EDID_BASE_INFO_S        stSinkCap;

HI_U32 g_SinkCECSupport  = HI_FALSE;
HI_U32 HDCPFailCount = 0;
HI_U32 videoalhpa = 100;

static int HDMI_TV_MAX_SUPPORT_FMT[] = {
    HI_UNF_ENC_FMT_4096X2160_60,
    HI_UNF_ENC_FMT_4096X2160_50,
    HI_UNF_ENC_FMT_4096X2160_30,
    HI_UNF_ENC_FMT_4096X2160_25,
    HI_UNF_ENC_FMT_4096X2160_24,
    HI_UNF_ENC_FMT_3840X2160_60,     /**<4K 60 Hz*/
    HI_UNF_ENC_FMT_3840X2160_50,     /**<4K 50 Hz*/
    HI_UNF_ENC_FMT_3840X2160_30,     /**<4K 30 Hz*/
    HI_UNF_ENC_FMT_3840X2160_25,     /**<4K 25 Hz*/
    HI_UNF_ENC_FMT_3840X2160_24,     /**<4K 24 Hz*/
    HI_UNF_ENC_FMT_1080P_60,         /**<1080p 60 Hz*/
    HI_UNF_ENC_FMT_1080P_50,         /**<1080p 50 Hz*/
    HI_UNF_ENC_FMT_1080P_30,         /**<1080p 30 Hz*/
    HI_UNF_ENC_FMT_1080P_25,         /**<1080p 25 Hz*/
    HI_UNF_ENC_FMT_1080P_24,         /**<1080p 24 Hz*/

    HI_UNF_ENC_FMT_1080i_60,         /**<1080i 60 Hz*/
    HI_UNF_ENC_FMT_1080i_50,         /**<1080i 50 Hz*/

    HI_UNF_ENC_FMT_720P_60,          /**<720p 60 Hz*/
    HI_UNF_ENC_FMT_720P_50,          /**<720p 50 Hz */

    HI_UNF_ENC_FMT_576P_50,          /**<576p 50 Hz*/
    HI_UNF_ENC_FMT_480P_60,          /**<480p 60 Hz*/

    HI_UNF_ENC_FMT_PAL,              /* B D G H I PAL */
    HI_UNF_ENC_FMT_PAL_N,            /* (N)PAL        */
    HI_UNF_ENC_FMT_PAL_Nc,           /* (Nc)PAL       */

    HI_UNF_ENC_FMT_NTSC,             /* (M)NTSC       */
    HI_UNF_ENC_FMT_NTSC_J,           /* NTSC-J        */
    HI_UNF_ENC_FMT_NTSC_PAL_M,       /* (M)PAL        */

    HI_UNF_ENC_FMT_SECAM_SIN,        /**< SECAM_SIN*/
    HI_UNF_ENC_FMT_SECAM_COS,        /**< SECAM_COS*/
};

static HI_UNF_ENC_FMT_E VESA_FMT_LIST[] = {
    HI_UNF_ENC_FMT_VESA_1024X768_60,
    HI_UNF_ENC_FMT_VESA_1440X900_60_RB,
    HI_UNF_ENC_FMT_VESA_1440X900_60,
    HI_UNF_ENC_FMT_VESA_1280X720_60,//25
    HI_UNF_ENC_FMT_VESA_800X600_60,
    HI_UNF_ENC_FMT_VESA_1280X800_60,
    HI_UNF_ENC_FMT_VESA_1280X1024_60,
    HI_UNF_ENC_FMT_VESA_1360X768_60,
    HI_UNF_ENC_FMT_VESA_1366X768_60,
    HI_UNF_ENC_FMT_VESA_1400X1050_60,//3
    HI_UNF_ENC_FMT_VESA_1600X900_60_RB,
    HI_UNF_ENC_FMT_VESA_1600X1200_60,
    HI_UNF_ENC_FMT_VESA_1680X1050_60,//35
    HI_UNF_ENC_FMT_VESA_1680X1050_60_RB,
    HI_UNF_ENC_FMT_VESA_1920X1080_60,
    HI_UNF_ENC_FMT_VESA_1920X1200_60,
    HI_UNF_ENC_FMT_VESA_1920X1440_60,
    HI_UNF_ENC_FMT_VESA_2048X1152_60,//40
    HI_UNF_ENC_FMT_VESA_2560X1440_60_RB,
    HI_UNF_ENC_FMT_VESA_2560X1600_60_RB,
};

int Customer_fmt_list[] = {

/*customer specific format list sample */
};

//STB hdmi cec info
HI_UNF_CEC_LOGICALADD_S stHdmiSTBLogicAdrrs ;
HI_U8 stHdmiSTBPhysicalAddr [4];
HI_BOOL stHdmiCecEnable = HI_FALSE;
//#ifdef HI_HDCP_SUPPORT
const HI_CHAR * pstencryptedHdcpKey = "/system/etc/EncryptedKey_332bytes.bin";
#define HDCP_KEY_LOCATION           "deviceinfo"
#define DRM_KEY_OFFSET              (128 * 1024)  // DRM KEY from bottom
#define HDCP_KEY_OFFSET_DEFAULT     (4 * 1024)    // HDMI KEY offset base on DRM KEY
#define HDCP_ENABLE_OFFSET_TO_KEY    (3 * 1024)      //HDCP enable data offset base on HDCP KEY
#define DEVICEINFO_MAX              (2 * 1024 * 1024) // deviceinfo 2M lenght
#define HDCP_KEY_LEN                (332)
//#endif

static HI_CHAR *g_pDispFmtString[HI_UNF_ENC_FMT_BUTT+1] = {
    "1080P_60",
    "1080P_50",
    "1080P_30",
    "1080P_25",
    "1080P_24",
    "1080i_60",
    "1080i_50",

    "720P_60",
    "720P_50",

    "576P_50",
    "480P_60",

    "PAL",
    "PAL_N",
    "PAL_Nc",

    "NTSC",
    "NTSC_J",
    "NTSC_PAL_M",

    "SECAM_SIN",
    "SECAM_COS",

    "1080P_24_FRAME_PACKING",
    "720P_60_FRAME_PACKING",
    "720P_50_FRAME_PACKING",

    "861D_640X480_60",
    "VESA_800X600_60",
    "VESA_1024X768_60",
    "VESA_1280X720_60",
    "VESA_1280X800_60",
    "VESA_1280X1024_60",
    "VESA_1360X768_60",
    "VESA_1366X768_60",
    "VESA_1400X1050_60",
    "VESA_1440X900_60",
    "VESA_1440X900_60_RB",
    "VESA_1600X900_60_RB",
    "VESA_1600X1200_60",
    "VESA_1680X1050_60",
    "VESA_1680X1050_60_RB",
    "VESA_1920X1080_60",
    "VESA_1920X1200_60",
    "VESA_1920X1440_60",
    "VESA_2048X1152_60",
    "VESA_2560X1440_60_RB",
    "VESA_2560X1600_60_RB",

    "3840X2160_24",
    "3840X2160_25",
    "3840X2160_30",
    "3840X2160_50",
    "3840X2160_60",

    "4096X2160_24",
    "4096X2160_25",
    "4096X2160_30",
    "4096X2160_50",
    "4096X2160_60",

    "3840x2160_23.976",
    "3840x2160_29.97",
    "720P_59.94",
    "1080P_59.94",
    "1080P_29.97",
    "1080P_23.976",
    "1080i_59.94",

    "BUTT"
};

extern int get_format(display_format_e *format);
extern int set_format(display_format_e format);
extern int set_HDRType(int HDRType);
extern int baseparam_save(void);
extern HI_U32 get_HDMI_CEC_Suspend_Enable();
extern HI_VOID HDMI_CEC_Proc(HI_UNF_HDMI_ID_E enHdmi, HI_UNF_HDMI_CEC_CMD_S *pstCECCmd, HI_VOID *pData);
extern int check_chip_dolby_capablity();
int isFormatAdaptEnable();
int isCapabilityChanged(int getCapResult,HI_UNF_EDID_BASE_INFO_S *pstSinkAttr);
void capToString(char* buffer,HI_UNF_EDID_BASE_INFO_S cap);
HI_S32          getCapRet = HI_FAILURE;

void enable_debug_log(HI_BOOL enalbe)
{
    char buffer[BUFLEN]={0};
    property_get("hidisp.debug.dblog", buffer, "false");
    if(strcmp(buffer,"true")==0)
        dbuglog =HI_TRUE;
    else
      dbuglog =HI_FALSE;
}

void setTVproperty(display_format_e format)
{
    int w = 0;
    int h = 0;
    int newhdrsuport = 2;
    int oldhdrsport = 2;
    char hei[5] ={0};
    char dpi[15] = {0};
    char size[10] = {0};
    char buffer[BUFLEN] = {0};

    framebuffer_get_max_screen_resolution(format,&w,&h);
    ALOGI("format %d , w: %d  ,h: %d,",format, w, h);
    sprintf(dpi, "%d", w);
    strcat(dpi,"*");
    sprintf(hei, "%d", h);
    strcat(dpi,hei);
    ALOGI("dpi: %s", dpi);

    sprintf(size,"%d",(int)(sqrt(TVHMax*TVHMax +TVWMax*TVWMax)/2.54 +0.5));
    ALOGE("TVWidth:%d,Height:%d,TVSzie:%s", TVWMax, TVHMax, size);

    property_set("persist.sys.tv.name",stSinkCap.stMfrsInfo.u8MfrsName);
    property_set("persist.sys.tv.type",stSinkCap.stMfrsInfo.u8pSinkName);
    property_set("persist.sys.tv.size",size);
    property_set("persist.sys.tv.dpi",dpi);

    memset(buffer, 0, sizeof(buffer));
    property_get("persist.sys.tv.Supporthdr", buffer , "2");

    oldhdrsport = atoi(buffer);
    if(HI_TRUE == stSinkCap.bHdrSupport && stSinkCap.stHdr.stEotf.bEotfSmpteSt2084)
        newhdrsuport = 1;//yes 1
    else if (HI_FALSE == stSinkCap.bHdrSupport)
        newhdrsuport = 2;//no 2
    else
        newhdrsuport = 0;//other 0

    //HSCP2018042722609
    if(newhdrsuport != oldhdrsport)
    {
        sprintf(buffer, "%d", newhdrsuport);
        property_set("persist.sys.tv.Supporthdr",buffer);
    }

}

HI_UNF_ENC_FMT_E stringToUnfFmt(HI_CHAR *pszFmt)
{
    HI_S32 i;
    HI_UNF_ENC_FMT_E fmtReturn = HI_UNF_ENC_FMT_BUTT;

    if (NULL == pszFmt)
    {
        return HI_UNF_ENC_FMT_BUTT;
    }

    for (i = 0; i < HI_UNF_ENC_FMT_BUTT; i++)
    {
        if (strcasestr(pszFmt, g_pDispFmtString[i]))
        {
            fmtReturn = i;
            break;
        }
    }

    if (i >= HI_UNF_ENC_FMT_BUTT)
    {
        i = HI_UNF_ENC_FMT_720P_50;
        fmtReturn = i;
        printf("\n!!! Can NOT match format, set format to is '%s'/%d.\n\n", g_pDispFmtString[i], i);
    }
    else
    {
        printf("\n!!! The format is '%s'/%d.\n\n", g_pDispFmtString[i], i);
    }
    return fmtReturn;
}

HI_S32 getCurrentMaxSupportFmt()
{
    HI_S32 listCount = 0;
    HI_S32 MaxFmtListLen = 0;
    HI_BOOL bSupport = HI_FALSE;

    MaxFmtListLen = sizeof(HDMI_TV_MAX_SUPPORT_FMT) / sizeof(HDMI_TV_MAX_SUPPORT_FMT[0]);
    ALOGI("MaxFmtListLen = %d", MaxFmtListLen);

    for (listCount = 0; listCount < MaxFmtListLen; listCount++)
    {
        if (HI_TRUE == is_format_support(&stSinkCap,HDMI_TV_MAX_SUPPORT_FMT[listCount]))
        {
            ALOGI("max support fmt is:%d",HDMI_TV_MAX_SUPPORT_FMT[listCount]);
            return HDMI_TV_MAX_SUPPORT_FMT[listCount];
        }
    }

    ALOGI("Can't Find Max Support Format, getCurrentMaxFormat return:720P_50 !");
    return HI_UNF_ENC_FMT_720P_50;
}

HI_UNF_ENC_FMT_E getVesaAdapt_format()
{

    HI_S32 listCount = 0;
    HI_S32 MaxFmtListLen = 0;
    HI_BOOL bSupport = HI_FALSE;

    MaxFmtListLen = sizeof(VESA_FMT_LIST) / sizeof(VESA_FMT_LIST[0]);
    ALOGI("total have %d vesa fmt", MaxFmtListLen);

    for (listCount = 0; listCount < MaxFmtListLen; listCount++)
    {
        if (HI_TRUE == is_format_support(&stSinkCap,VESA_FMT_LIST[listCount]))
        {
            ALOGI("fin vesa support fmt is:%d",VESA_FMT_LIST[listCount]);
            return VESA_FMT_LIST[listCount];
        }
    }

    ALOGI("Can't find vesa support fmt in edid,default to : FMT_861D_640X480_60");
    return HI_UNF_ENC_FMT_861D_640X480_60;

}
static HI_VOID HDMI_PrintAttr(HI_UNF_HDMI_ATTR_S *pstHDMIAttr)
{

    ALOGI_IF(dbuglog,"=====HI_UNF_HDMI_SetAttr=====\n"
           "bEnableHdmi:%d\n"
           "bEnableVideo:%d\n"
           "enVidOutMode:%d\n"
           "enDeepColorMode:%d\n"
           "bxvYCCMode:%d\n\n"
           "bEnableAudio:%d\n"
           "bEnableAviInfoFrame:%d\n"
           "bEnableAudInfoFrame:%d\n"
           "bEnableSpdInfoFrame:%d\n"
           "bEnableMpegInfoFrame:%d\n\n"
           "bHDCPEnable:%d\n"
           "bEnableVidModeAdapt:%d\n"
           "bEnableDeepClrAdapt:%d\n"
           "==============================\n",
           pstHDMIAttr->bEnableHdmi,
           pstHDMIAttr->bEnableVideo,
           pstHDMIAttr->enVidOutMode,pstHDMIAttr->enDeepColorMode,pstHDMIAttr->bxvYCCMode,
           pstHDMIAttr->bEnableAudio,
           pstHDMIAttr->bEnableAudInfoFrame,pstHDMIAttr->bEnableAudInfoFrame,
           pstHDMIAttr->bEnableSpdInfoFrame,pstHDMIAttr->bEnableMpegInfoFrame,
           pstHDMIAttr->bHDCPEnable,
           pstHDMIAttr->bEnableVidModeAdapt,pstHDMIAttr->bEnableDeepClrAdapt);
    return;
}

HI_BOOL isMutexStrategyEnabledForHdmiAndCvbs()
{
    char buf_cvbs_enable[BUFLEN]={0};
    char buf_dmacert_enable[BUFLEN]={0};
    char buf_iptvcert_enable[BUFLEN]={0};
    char buf_dvbcert_enable[BUFLEN]={0};

    memset(buf_cvbs_enable, 0, sizeof(buf_cvbs_enable));
    memset(buf_dmacert_enable, 0, sizeof(buf_dmacert_enable));
    memset(buf_iptvcert_enable, 0, sizeof(buf_iptvcert_enable));
    memset(buf_dvbcert_enable, 0, sizeof(buf_dvbcert_enable));

    property_get("persist.sys.cvbs.and.hdmi", buf_cvbs_enable, "false");
//    property_get("ro.dolby.dmacert.enable", buf_dmacert_enable, "false");
//    property_get("ro.dolby.iptvcert.enable", buf_iptvcert_enable, "false");
//    property_get("ro.dolby.dvbcert.enable", buf_dvbcert_enable, "false");

    if ((!strcmp(buf_cvbs_enable, "false")))
//        && (!strcmp(buf_dmacert_enable, "false"))
//        && (!strcmp(buf_iptvcert_enable, "false"))
 //       && (!strcmp(buf_dvbcert_enable, "false")))
    {
        return HI_TRUE;
    }
    return HI_FALSE;
}

HI_S32 HDMI_CEC_Standy(){
    HI_U32 ret = HI_SUCCESS;

    ret = get_HDMI_CEC_Suspend_Enable();
    if(ret!= 1)
        return ret;

    HI_UNF_HDMI_CEC_STATUS_S cecStatuss;

    int i ;
    for(i= 0;i<10 ;i++)
    {
        ret = HI_UNF_HDMI_CECStatus(HI_UNF_HDMI_ID_0,&cecStatuss);
        if(ret != HI_SUCCESS)
        {
            ALOGE("HI_UNF_HDMI_CECStatus fail ret = %d",ret);
        }
        stHdmiCecEnable = cecStatuss.bEnable;
        if(cecStatuss.bEnable == HI_TRUE)
        {
            stHdmiSTBLogicAdrrs = cecStatuss.u8LogicalAddr;
            stHdmiSTBPhysicalAddr[0] = cecStatuss.u8PhysicalAddr[0];
            stHdmiSTBPhysicalAddr[1] = cecStatuss.u8PhysicalAddr[1];
            stHdmiSTBPhysicalAddr[2] = cecStatuss.u8PhysicalAddr[2];
            stHdmiSTBPhysicalAddr[3] = cecStatuss.u8PhysicalAddr[3];
            break;
        }
        usleep(500*1000);
    }

    HI_UNF_HDMI_CEC_CMD_S  CECCmd;
    memset(&CECCmd, 0, sizeof(HI_UNF_HDMI_CEC_CMD_S));
    CECCmd.enSrcAdd = stHdmiSTBLogicAdrrs;
    CECCmd.enDstAdd = HI_UNF_CEC_LOGICALADD_BROADCAST;//HI_UNF_CEC_LOGICALADD_TV;
    CECCmd.u8Opcode = CEC_OPCODE_STANDBY;
    CECCmd.unOperand.stRawData.u8Length = 0x00;

    ret = HI_UNF_HDMI_SetCECCommand(g_stHdmiArgs.enHdmi, &CECCmd);
    ALOGE("\n \033[41;37m CEC hdmi_cec_cmd HDMI_CEC_Standby\33[0m ret = %d \n ",ret);
    ALOGI("Sleep 0.2 second to ensure hdmi drv send out cec standby cmd to tv");
    usleep(200*1000);
    TvPowerStatus = TV_STANDBY;
    checktimes = 0;
    return ret;
}

void HDMI_CEC_Audio_Resume(){

    int ret ;
    HI_UNF_HDMI_CEC_STATUS_S cecStatus;
    HI_U8 myu8LogicalAddr = HI_UNF_CEC_LOGICALADD_PLAYDEV_1;
    int i ;
    for(i= 0;i<10 ;i++) {
        ret = HI_UNF_HDMI_CECStatus(HI_UNF_HDMI_ID_0,&cecStatus);
        if(cecStatus.bEnable == HI_TRUE)
        {
            myu8LogicalAddr = cecStatus.u8LogicalAddr;
            stHdmiSTBLogicAdrrs = (HI_UNF_CEC_LOGICALADD_S)myu8LogicalAddr;
            stHdmiSTBPhysicalAddr[0] = cecStatus.u8PhysicalAddr[0];
            stHdmiSTBPhysicalAddr[1] = cecStatus.u8PhysicalAddr[1];
            stHdmiSTBPhysicalAddr[2] = cecStatus.u8PhysicalAddr[2];
            stHdmiSTBPhysicalAddr[3] = cecStatus.u8PhysicalAddr[3];
            break;
        }
        usleep(500*1000);
    }
    ALOGI("CECStatus.bEnable = %d checktimes = %d",cecStatus.bEnable,(i+1));
    HI_UNF_HDMI_CEC_CMD_S  CECCmd;

    CECCmd.enSrcAdd = (HI_UNF_CEC_LOGICALADD_S)myu8LogicalAddr;
    CECCmd.enDstAdd = HI_UNF_CEC_LOGICALADD_AUDIOSYSTEM;

    CECCmd.unOperand.stRawData.u8Length      = 2;

    //The Amplifier comes out of the Standby state (if necessary)
    CECCmd.u8Opcode = CEC_OPCODE_SYSTEM_AUDIO_MODE_REQUEST;
    CECCmd.unOperand.stRawData.u8Data[0]     = stHdmiSTBPhysicalAddr[0];
    CECCmd.unOperand.stRawData.u8Data[0]    <<= 4;
    CECCmd.unOperand.stRawData.u8Data[0]    |= stHdmiSTBPhysicalAddr[1];
    CECCmd.unOperand.stRawData.u8Data[1]     = stHdmiSTBPhysicalAddr[2];
    CECCmd.unOperand.stRawData.u8Data[1]    <<= 4;
    CECCmd.unOperand.stRawData.u8Data[1]    |= stHdmiSTBPhysicalAddr[3];
    ret = HI_UNF_HDMI_SetCECCommand(HI_UNF_HDMI_ID_0, &CECCmd);
    ALOGI("\033[42;31m resume audio msg >HI_UNF_HDMI_SetCECCommand  \33[0m ret = %d",ret);

    ALOGI(" ---> CEC CMD HAD SEND <---\n enSrcAdd %d\n enDstAdd %d\n opcode=%#x\n  pOperand = %#x %#x %#x\n pOperand.length=%d\nUICode=%#x\n",
        CECCmd.enSrcAdd,
        CECCmd.enDstAdd,
        CECCmd.u8Opcode,
        CECCmd.unOperand.stRawData.u8Data[0],
        CECCmd.unOperand.stRawData.u8Data[1],
        CECCmd.unOperand.stRawData.u8Data[2],
        CECCmd.unOperand.stRawData.u8Length,
        CECCmd.unOperand.stUIOpcode);
}

void HDMI_CEC_Resume(){

    HI_U32 ret = HI_SUCCESS;
    ret = get_HDMI_CEC_Suspend_Enable();
    if(ret != 1) return;
    ret= HI_UNF_HDMI_CEC_Enable(HI_UNF_HDMI_ID_0);
    ALOGI("cec resume > HI_UNF_HDMI_CEC_Enable ret = %d ",ret);
    HI_UNF_HDMI_CEC_STATUS_S cecStatuss;

    //icsl init
    cecStatuss.u8Network[HI_UNF_CEC_LOGICALADD_AUDIOSYSTEM] = HI_FALSE;
    cecStatuss.bEnable = HI_FALSE;
    cecStatuss.u8PhysicalAddr[0]= 0;
    cecStatuss.u8PhysicalAddr[1]= 0;
    cecStatuss.u8PhysicalAddr[2]= 0;
    cecStatuss.u8PhysicalAddr[3]= 0;
    cecStatuss.u8LogicalAddr = 0;

    int i ;
    HI_UNF_HDMI_CEC_CMD_S  CECCmd;
    memset(&CECCmd, 0, sizeof(HI_UNF_HDMI_CEC_CMD_S));

    for(i= 0;i<3 ;i++)
    {
        //send cec command to resume tv
        CECCmd.enSrcAdd = stHdmiSTBLogicAdrrs;
        CECCmd.enDstAdd = HI_UNF_CEC_LOGICALADD_TV;
        CECCmd.u8Opcode = CEC_OPCODE_IMAGE_VIEW_ON;//04
        CECCmd.unOperand.stRawData.u8Length = 0x00;
        if(TvPowerStatus == TV_STANDBY || TvPowerStatus == TV_UNKNOW)
        {
            ret = HI_UNF_HDMI_SetCECCommand(HI_UNF_HDMI_ID_0, &CECCmd);
            ALOGI("\n \033[42;31m CEC hdmi_cec_cmd HI_UNF_HDMI_SetCECCommand 04 \33[0m ret = %d",ret);

            CECCmd.u8Opcode = CEC_OPCODE_TEXT_VIEW_ON;//0d
            ret = HI_UNF_HDMI_SetCECCommand(HI_UNF_HDMI_ID_0, &CECCmd);
            ALOGI("\n \033[42;31m CEC hdmi_cec_cmd HI_UNF_HDMI_SetCECCommand 0d \33[0m ret = %d",ret);

            CECCmd.u8Opcode = CEC_OPCODE_GIVE_DEVICE_POWER_STATUS;//check TV Power status  cmd
            ret = HI_UNF_HDMI_SetCECCommand(HI_UNF_HDMI_ID_0, &CECCmd);
            ALOGI("\033[42;31m CEC hdmi_cec_cmd HI_UNF_HDMI_SetCECCommand 8F \33[0m ret = %d",ret);
        }
        else if (TvPowerStatus == TV_TURN_ON) break;

        ret = HI_UNF_HDMI_CECStatus(HI_UNF_HDMI_ID_0,&cecStatuss);
        if(ret != HI_SUCCESS)
        {
            ALOGE("HI_UNF_HDMI_CECStatus fail ret = %d",ret);
        }
        stHdmiCecEnable = cecStatuss.bEnable;
        if(cecStatuss.bEnable == HI_TRUE)
        {
            stHdmiSTBLogicAdrrs = cecStatuss.u8LogicalAddr;
            stHdmiSTBPhysicalAddr[0] = cecStatuss.u8PhysicalAddr[0];
            stHdmiSTBPhysicalAddr[1] = cecStatuss.u8PhysicalAddr[1];
            stHdmiSTBPhysicalAddr[2] = cecStatuss.u8PhysicalAddr[2];
            stHdmiSTBPhysicalAddr[3] = cecStatuss.u8PhysicalAddr[3];
            continue;
        }
        else if(cecStatuss.bEnable != HI_TRUE)//Key code
        {
            ALOGI("CECStatus.bEnable = %d checktimes = %d",cecStatuss.bEnable,i);
            ret= HI_UNF_HDMI_CEC_Enable(HI_UNF_HDMI_ID_0);
            ALOGI("resume > HI_UNF_HDMI_CEC_Enable ret = %d times%d",ret,i);
        }
        ALOGI("CECStatus.bEnable = %d checktimes = %d",cecStatuss.bEnable,(i+1));
        usleep(200*1000);
    }

    if(cecStatuss.u8Network[HI_UNF_CEC_LOGICALADD_AUDIOSYSTEM]==HI_TRUE)
    {
        HDMI_CEC_Audio_Resume();
    }
}

HI_VOID HDMI_CEC_RESPONSE_PHYSICAL_ADDRESS(HI_UNF_HDMI_ID_E enHdmi,HI_UNF_HDMI_CEC_CMD_S *pstCECCmd){
    HI_S32 ret = HI_FALSE;
    if(stHdmiCecEnable==HI_TRUE){

        ALOGI_IF(dbuglog,"\n \033[44;31m===> responese box physical address to tv<====== \33[0m\n");

        HI_UNF_HDMI_CEC_CMD_S respCECCmd ;
        respCECCmd.enDstAdd = HI_UNF_CEC_LOGICALADD_BROADCAST;

        ALOGI_IF(dbuglog,"TO TV : respCECCmd.enDstAdd =0x%x",respCECCmd.enDstAdd);

        respCECCmd.enSrcAdd = stHdmiSTBLogicAdrrs;
        ALOGI_IF(dbuglog,"TO TV : box LogicAdrrs respCECCmd.enSrcAdd =0x%x ",stHdmiSTBLogicAdrrs);
        respCECCmd.u8Opcode   = CEC_OPCODE_REPORT_PHYSICAL_ADDRESS;//0x84
        ALOGI_IF(dbuglog,"\nTO TV :respCECCmd.u8Opcode =0x%x ,",respCECCmd.u8Opcode);
        respCECCmd.unOperand.stRawData.u8Length          = 3;
        respCECCmd.unOperand.stRawData.u8Data[0]     = stHdmiSTBPhysicalAddr[0];
        respCECCmd.unOperand.stRawData.u8Data[0]    <<= 4;
        respCECCmd.unOperand.stRawData.u8Data[0]    |= stHdmiSTBPhysicalAddr[1];

        respCECCmd.unOperand.stRawData.u8Data[1]     = stHdmiSTBPhysicalAddr[2];
        respCECCmd.unOperand.stRawData.u8Data[1]    <<= 4;
        respCECCmd.unOperand.stRawData.u8Data[1]    |= stHdmiSTBPhysicalAddr[3];

        respCECCmd.unOperand.stRawData.u8Data[2]     = 4;//device type is playback
        ALOGI_IF(dbuglog,"PhysicalAddr response to tv ph[0]%x ph[1]%x Dst[2]%x ",respCECCmd.unOperand.stRawData.u8Data[0],
            respCECCmd.unOperand.stRawData.u8Data[1],
            respCECCmd.unOperand.stRawData.u8Data[2]);
        ret = HI_UNF_HDMI_SetCECCommand(enHdmi, &respCECCmd);
        ALOGI_IF(dbuglog,"HDMI_CEC_RESPONSE_PHYSICAL_ADDRESS > HI_UNF_HDMI_SetCECCommand(enHdmi, &respCECCmd) ret = %d",ret);

    }
}
HI_VOID HDMI_CEC_RESPONSE_CEC_VERSION(HI_UNF_HDMI_ID_E enHdmi,HI_UNF_HDMI_CEC_CMD_S *pstCECCmd)
{
    HI_UNF_HDMI_CEC_CMD_S respCECCmd ;
    respCECCmd.enDstAdd = pstCECCmd->enSrcAdd;
    respCECCmd.enSrcAdd = stHdmiSTBLogicAdrrs;
    respCECCmd.u8Opcode   = CEC_OPCODE_CEC_VERSION;
    respCECCmd.unOperand.stRawData.u8Length         = 1;
    respCECCmd.unOperand.stRawData.u8Data[0]     = 0x05;
    HI_UNF_HDMI_SetCECCommand(enHdmi, &respCECCmd);
}
HI_VOID HDMI_CEC_RESPONSE_CEC_DEVICE_VENDOR_ID(HI_UNF_HDMI_ID_E enHdmi,HI_UNF_HDMI_CEC_CMD_S *pstCECCmd)
{
    HI_UNF_HDMI_CEC_CMD_S respCECCmd ;
    respCECCmd.enDstAdd = HI_UNF_CEC_LOGICALADD_BROADCAST;
    respCECCmd.enSrcAdd = stHdmiSTBLogicAdrrs;
    respCECCmd.u8Opcode   = CEC_OPCODE_DEVICE_VENDOR_ID;
    respCECCmd.unOperand.stRawData.u8Length         = 3;
    respCECCmd.unOperand.stRawData.u8Data[0]     = 'h';
    respCECCmd.unOperand.stRawData.u8Data[1]     = 'i' ;
    respCECCmd.unOperand.stRawData.u8Data[2]     = 's' ;
    HI_UNF_HDMI_SetCECCommand(enHdmi, &respCECCmd);
}
HI_VOID HDMI_CEC_RESPONSE_CEC_OSD_NAME(HI_UNF_HDMI_ID_E enHdmi,HI_UNF_HDMI_CEC_CMD_S *pstCECCmd)
{
    HI_S32  ret = HI_SUCCESS;
    HI_UNF_HDMI_CEC_CMD_S respCECCmd ;
    respCECCmd.enDstAdd = HI_UNF_CEC_LOGICALADD_TV;
    respCECCmd.enSrcAdd = stHdmiSTBLogicAdrrs;
    ALOGI_IF(dbuglog,"HDMI_CEC_RESPONSE_CEC_OSD_NAME > respCECcmd.enSrcAdd = %d",stHdmiSTBLogicAdrrs);
    respCECCmd.u8Opcode   = CEC_OPCODE_SET_OSD_NAME;
    respCECCmd.unOperand.stRawData.u8Length         = 3;
    respCECCmd.unOperand.stRawData.u8Data[0]     = 'h';
    respCECCmd.unOperand.stRawData.u8Data[1]     = 'i' ;
    respCECCmd.unOperand.stRawData.u8Data[2]     = 's' ;
    ret = HI_UNF_HDMI_SetCECCommand(enHdmi, &respCECCmd);
    ALOGI_IF(dbuglog,"HDMI_CEC_RESPONSE_CEC_OSD_NAME>HI_UNF_HDMI_SetCECCommand ret = %d",ret);
}
HI_VOID HDMI_CEC_RESPONSE_DEVICE_POWER_STATUS(HI_UNF_HDMI_ID_E enHdmi,HI_UNF_HDMI_CEC_CMD_S *pstCECCmd)
{
    HI_UNF_HDMI_CEC_CMD_S respCECCmd ;
    respCECCmd.enDstAdd = pstCECCmd->enSrcAdd;
    respCECCmd.enSrcAdd = stHdmiSTBLogicAdrrs;
    respCECCmd.u8Opcode   = CEC_OPCODE_REPORT_POWER_STATUS;
    respCECCmd.unOperand.stRawData.u8Length         = 1;
    respCECCmd.unOperand.stRawData.u8Data[0]     = 0x00;
    HI_UNF_HDMI_SetCECCommand(enHdmi, &respCECCmd);
}
HI_VOID HDMI_CEC_RESPONSE_CHECK_TV_POWER_STATUS()
{
    HI_U32 ret;

    HI_UNF_HDMI_CEC_CMD_S respCECCmd ;
    respCECCmd.enDstAdd = HI_UNF_CEC_LOGICALADD_TV;
    respCECCmd.enSrcAdd = stHdmiSTBLogicAdrrs;
    respCECCmd.u8Opcode = CEC_OPCODE_GIVE_DEVICE_POWER_STATUS;
    respCECCmd.unOperand.stRawData.u8Length = 0;
    ret = HI_UNF_HDMI_SetCECCommand(HI_UNF_HDMI_ID_0, &respCECCmd);

    ALOGI("send cec cmd to tv for checking tv power status > HI_UNF_HDMI_SetCECCommand ret =%d",ret);
}
HI_VOID HDMI_CEC_RECEIVE_TV_POWER_STATUS(HI_UNF_HDMI_CEC_CMD_S *pstCECCmd)
{
    HI_U8 current_status = pstCECCmd->unOperand.stRawData.u8Data[0];
    if(current_status == 0 || current_status == 2)
    {
        ALOGI("\033[42;34m Tv feed back status is ON %d \33[m",current_status);
        TvPowerStatus = TV_TURN_ON;
    }
    else
    {
        ALOGI("\033[41;37m Tv feed back status is OFF :%d\33[m",current_status);
        TvPowerStatus = TV_STANDBY;
    }
}
HI_VOID HDMI_CEC_RESPONSE_REQUEST_ACTIVE_SOURCE(HI_UNF_HDMI_ID_E enHdmi,HI_UNF_HDMI_CEC_CMD_S *pstCECCmd)
{
    HI_UNF_HDMI_CEC_CMD_S respCECCmd ;
    respCECCmd.enDstAdd = HI_UNF_CEC_LOGICALADD_BROADCAST;
    respCECCmd.enSrcAdd = stHdmiSTBLogicAdrrs;
    respCECCmd.u8Opcode   = CEC_OPCODE_ACTIVE_SOURCE;
    respCECCmd.unOperand.stRawData.u8Length          = 2;
    respCECCmd.unOperand.stRawData.u8Data[0]     = stHdmiSTBPhysicalAddr[0];
    respCECCmd.unOperand.stRawData.u8Data[0]    <<= 4;
    respCECCmd.unOperand.stRawData.u8Data[0]    |= stHdmiSTBPhysicalAddr[1];
    respCECCmd.unOperand.stRawData.u8Data[1]     = stHdmiSTBPhysicalAddr[2];
    respCECCmd.unOperand.stRawData.u8Data[1]    <<= 4;
    respCECCmd.unOperand.stRawData.u8Data[1]    |= stHdmiSTBPhysicalAddr[3];
    //respCECCmd.unOperand.stRawData.u8Data[2]     = pstCECCmd->enSrcAdd;
    HI_UNF_HDMI_SetCECCommand(enHdmi, &respCECCmd);
}
HI_VOID HDMI_CEC_RESPONSE_VENDOR_COMMAND_WITH_ID(HI_UNF_HDMI_ID_E enHdmi,HI_UNF_HDMI_CEC_CMD_S *pstCECCmd)
{
    HI_UNF_HDMI_CEC_CMD_S respCECCmd ;
    respCECCmd.enDstAdd =HI_UNF_CEC_LOGICALADD_TV;// pstCECCmd->enSrcAdd;
    respCECCmd.enSrcAdd = stHdmiSTBLogicAdrrs;
    respCECCmd.u8Opcode   = CEC_OPCODE_VENDOR_COMMAND_WITH_ID;
    respCECCmd.unOperand.stRawData.u8Length         = 3;
    respCECCmd.unOperand.stRawData.u8Data[0]     = 's';
    respCECCmd.unOperand.stRawData.u8Data[1]     = 't' ;
    respCECCmd.unOperand.stRawData.u8Data[2]     = 'b' ;
    HI_UNF_HDMI_SetCECCommand(enHdmi, &respCECCmd);
}
HI_VOID HDMI_CEC_RESPONSE_CONTROL_PRESSED(HI_UNF_HDMI_ID_E enHdmi,HI_UNF_HDMI_CEC_CMD_S *pstCECCmd)
{
    //TV ir contral
    HI_U8 irCode = pstCECCmd->unOperand.stUIOpcode;

    ALOGE(" -- HDMI_CEC_RESPONSE_CONTROL_PRESSED irCode = 0x%x",irCode);
    switch(irCode){
        case HI_UNF_CEC_UICMD_SELECT:
            HDMI_Suspend_ReportKeyEvent(KEY_ENTER, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_ENTER, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_UP:
            HDMI_Suspend_ReportKeyEvent(KEY_UP, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_UP, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_DOWN:
            HDMI_Suspend_ReportKeyEvent(KEY_DOWN, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_DOWN, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_LEFT:
            HDMI_Suspend_ReportKeyEvent(KEY_LEFT, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_LEFT, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_RIGHT:
            HDMI_Suspend_ReportKeyEvent(KEY_RIGHT, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_RIGHT, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_EXIT:
            HDMI_Suspend_ReportKeyEvent(KEY_BACK, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_BACK, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_ROOT_MENU:
            HDMI_Suspend_ReportKeyEvent(KEY_MENU, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_MENU, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_PAGE_UP:
            HDMI_Suspend_ReportKeyEvent(KEY_PAGE_UP, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_PAGE_UP, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_PAGE_DOWN:
            HDMI_Suspend_ReportKeyEvent(KEY_PAGE_DOWN, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_PAGE_DOWN, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_FAST_FORWARD:
            HDMI_Suspend_ReportKeyEvent(KEY_FAST_FORWARD, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_FAST_FORWARD, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_REWIND:
            HDMI_Suspend_ReportKeyEvent(KEY_REWIND, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_REWIND, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_NUM_0:
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_0, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_0, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_NUM_1:
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_1, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_1, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_NUM_2:
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_2, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_2, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_NUM_3:
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_3, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_3, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_NUM_4:
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_4, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_4, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_NUM_5:
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_5, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_5, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_NUM_6:
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_6, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_6, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_NUM_7:
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_7, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_7, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_NUM_8:
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_8, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_8, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_NUM_9:
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_9, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_NUM_9, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_PLAY:
            HDMI_Suspend_ReportKeyEvent(KEY_PLAY, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_PLAY, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_STOP:
            HDMI_Suspend_ReportKeyEvent(KEY_STOP, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_STOP, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_PAUSE:
            HDMI_Suspend_ReportKeyEvent(KEY_PLAY, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_PLAY, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_VOLUME_UP:
            HDMI_Suspend_ReportKeyEvent(KEY_VOLUME_UP, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_VOLUME_UP, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_VOLUME_DOWN:
            HDMI_Suspend_ReportKeyEvent(KEY_VOLUME_DOWN, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_VOLUME_DOWN, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_FORWARD:
            HDMI_Suspend_ReportKeyEvent(KEY_PAGE_DOWN, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_PAGE_DOWN, KPC_EV_KEY_RELEASE);
            break;
        case HI_UNF_CEC_UICMD_BACKWARD:
            HDMI_Suspend_ReportKeyEvent(KEY_PAGE_UP, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_PAGE_UP, KPC_EV_KEY_RELEASE);
            break;
    }
}
HI_VOID HDMI_CEC_RESPONSE_CONTROL_RELEASED(HI_UNF_HDMI_ID_E enHdmi,HI_UNF_HDMI_CEC_CMD_S *pstCECCmd)
{
    //TV ir contral release
}

HI_VOID HDMI_CEC_RESPONSE_TV_RESUME_BOX(HI_UNF_HDMI_ID_E enHdmi,HI_UNF_HDMI_CEC_STATUS_S cecStatuss,HI_U8 opcode)
{
    if(cecStatuss.bEnable == HI_TRUE && get_HDMI_CEC_Suspend_Status() == HI_TRUE)
    {
        //HDMI_Suspend_ReportKeyEvent(KEY_POWER, KPC_EV_KEY_PRESS);
        //HDMI_Suspend_ReportKeyEvent(KEY_POWER, KPC_EV_KEY_RELEASE);
		system("am broadcast -a android.ysten.systemupdate -e hdmipowercontrol hdmipoweroff &");
        ALOGI("HDMI_CEC_Proc: send power key to awake  opcode :%d",opcode);
    }
}
HI_VOID HDMI_CEC_RESPONSE_MENU_REQUEST(HI_UNF_HDMI_CEC_CMD_S *pstCECCmd)
{
    HI_S32 ret = HI_SUCCESS;
    ALOGI("Response menu request.");
    HI_UNF_HDMI_CEC_CMD_S respCECCmd ;
    respCECCmd.enDstAdd = pstCECCmd->enSrcAdd;
    respCECCmd.enSrcAdd = stHdmiSTBLogicAdrrs;
    respCECCmd.u8Opcode   = CEC_OPCODE_MENU_STATUS;
    respCECCmd.unOperand.stRawData.u8Length         = 1;
    respCECCmd.unOperand.stRawData.u8Data[0]     = 0x00;
    ret = HI_UNF_HDMI_SetCECCommand(HI_UNF_HDMI_ID_0, &respCECCmd);
    if(HI_SUCCESS != ret)
    {
        ALOGE("UNF SetCECCommand fail %#x",ret);
    }
}

HI_BOOL isHdmiEdidChanged()
{
    HI_S32   ret                     = HI_SUCCESS;
    HI_U32   edidLen                 = 0;
    HI_U32   headindex               = 0;
    HI_U32   tailindex               = 0;
    HI_U8    Edid[512]               = {0};
    HI_U8    index                   = 0;
    HI_U8    byte_storelenth         = (PROP_VALUE_MAX-1)/2;//91
    HI_U8    getindex                = 0;
    HI_U8    persis_num_for_store    = 0;
    HI_U8    store_num               = 0;
    HI_BOOL  isChange                = HI_TRUE;
    HI_CHAR  tansbuffer[5]           = {0};
    HI_CHAR  new_edidbuffer[BUFLEN]  = {0};
    HI_CHAR  old_edidbuffer[BUFLEN]  = {0};
    HI_CHAR  store_buffer[25]        = {0};
    HI_CHAR  property[]              = "persist.sys.hdmi.edid";

    ALOGI("one property can store %d byte totally !",byte_storelenth);

    //Read hdmi edid original data
    ret = HI_UNF_HDMI_ReadEDID(Edid,&edidLen);
    if (HI_SUCCESS == ret)
    {
        ALOGI("ReadEDID date ret:%#x  there are %d bytes data .",ret ,edidLen);

        persis_num_for_store = (edidLen/byte_storelenth) + 1;
        ALOGI("Needed %d property to store the edid data",persis_num_for_store);
        memset(new_edidbuffer, 0, sizeof(BUFLEN));

        for(store_num; store_num < persis_num_for_store; store_num++)
        {
            memset(old_edidbuffer, 0, sizeof(BUFLEN));

            //tansbuffer <----------- num (index)
            sprintf(tansbuffer,"%d",store_num);

            //store_buffer <-- persist.sys.hdmi.cap
            strcpy(store_buffer, property);

            //persist.sys.hdmi.cap --> persist.sys.hdmi.cap(index)
            strcat(store_buffer, tansbuffer);

            //get the old edid and put to--> old_edid buffer.
            property_get(store_buffer, old_edidbuffer,"xxx");

            headindex = (store_num*10) +7;
            tailindex = edidLen - (store_num*35);

            //ALOGI("00================%d",store_num);
            for (index = 0; index < byte_storelenth; index ++)
            {
                if(index < 10)
                {
                    getindex = headindex = headindex +1;
                    //ALOGI("=========> %d",headindex);
                }
                else
                {
                    getindex = tailindex = tailindex -1;
                    if(tailindex == headindex)break;
                    //ALOGI("                    %d <==================",tailindex);
                }
                sprintf(&new_edidbuffer[index*2], "%02x", Edid[getindex]);
            }

            if(strcmp(old_edidbuffer,new_edidbuffer) != 0)
            {
                ALOGI_IF(dbuglog,"%d is diff current Store to %s",store_num,store_buffer);
                property_set(store_buffer, new_edidbuffer);
            }
            else
            {
                isChange = HI_FALSE;
                ALOGI_IF(dbuglog,"same no need set to %s again",new_edidbuffer);
            }
            ALOGI_IF(dbuglog,"[new %d] :%s",store_num,new_edidbuffer);
            ALOGI_IF(dbuglog,"[old %d] :%s",store_num,old_edidbuffer);

        }
    }
    else
        ALOGE("HI_UNF_HDMI_ReadEDID error ! %#x",ret);

    ALOGI("edid change : %s",isChange ? "YES." : "NO.");
    return isChange;
}


void HDMI_CEC_Suspend_Timeout()
{

    HDMI_CEC_RESPONSE_CHECK_TV_POWER_STATUS();
    sleep(3);

    ALOGI("cec suspend time out funtion @!  ");
    ALOGI("Current tv power status flag: %d",TvPowerStatus);

    if(TvPowerStatus == TV_STANDBY)
    {
        property_set("persist.sys.cec.status", "true");
        property_set("suspend.from.tv.cec", "true");

        //HDMI_Suspend_ReportKeyEvent(KEY_POWER, KPC_EV_KEY_PRESS);
        //HDMI_Suspend_ReportKeyEvent(KEY_POWER, KPC_EV_KEY_RELEASE);
		system("am broadcast -a android.ysten.systemupdate -e hdmipowercontrol hdmipoweroff &");
        ALOGW("\033[31m cec suspend time out : send power key to system! \33[0m\n");
    }
    else
    {
        ALOGW("\033[32m cec suspend time out : tv feed back power status isn't OFF , don't go to standby ! \33[0m\n");
    }
    alrm_set_flag       = HI_FALSE;
    alrm_cancel_flag    = HI_TRUE;
}

HI_VOID HDMI_CEC_Proc(HI_UNF_HDMI_ID_E enHdmi, HI_UNF_HDMI_CEC_CMD_S *pstCECCmd, HI_VOID *pData)
{
    TV_feedback_flag = HI_TRUE;

    HI_U8       opcode          = pstCECCmd->u8Opcode;
    HI_S32      ret             = HI_SUCCESS;
    HI_U32      suspendtime     = 0;// second
    HI_CHAR     buffer[BUFLEN]  = {0};

    ALOGI_IF(dbuglog, " ---> HDMI_CEC_Proc <---\nenSrcAdd %#x\nenDstAdd %#x\nopcode= %#x\npOperand = %#x %#x %#x\npOperand.length=%d",
        pstCECCmd->enSrcAdd,
        pstCECCmd->enDstAdd,
        opcode,
        pstCECCmd->unOperand.stRawData.u8Data[0],
        pstCECCmd->unOperand.stRawData.u8Data[1],
        pstCECCmd->unOperand.stRawData.u8Data[2],
        pstCECCmd->unOperand.stRawData.u8Length);
    HI_UNF_HDMI_CEC_CMD_S respCECCmd ;
    HI_UNF_HDMI_CEC_STATUS_S cecStatuss;
    ret = HI_UNF_HDMI_CECStatus(enHdmi,&cecStatuss);

    if(cecStatuss.bEnable == HI_TRUE)
    {
        stHdmiSTBLogicAdrrs = cecStatuss.u8LogicalAddr;
        stHdmiSTBPhysicalAddr[0] = cecStatuss.u8PhysicalAddr[0];
        stHdmiSTBPhysicalAddr[1] = cecStatuss.u8PhysicalAddr[1];
        stHdmiSTBPhysicalAddr[2] = cecStatuss.u8PhysicalAddr[2];
        stHdmiSTBPhysicalAddr[3] = cecStatuss.u8PhysicalAddr[3];
        ALOGI_IF(dbuglog, "CEC HI_UNF_HDMI_CECStatus  bEnable true myu8LogicalAddr = %d ",stHdmiSTBLogicAdrrs);
    }
    stHdmiCecEnable = cecStatuss.bEnable;

    ALOGI_IF(dbuglog,"HI_UNF_HDMI_CECStatus ret = %d cecStatuss.bEnable =%d",ret,cecStatuss.bEnable);
    switch(opcode)
    {
        case CEC_OPCODE_STANDBY:
            TvPowerStatus = TV_STANDBY;
            ret = get_HDMI_CEC_Suspend_Enable();
            if(ret!= 1)
                return;
            HDMI_CEC_RESPONSE_CHECK_TV_POWER_STATUS();

            property_get("persist.sys.hdmicecsuspend.time", buffer, "0");
            suspendtime = atoi(buffer);
            ALOGI("\n\n\033[41;37m hdmi cec suspend time\33[0m %d \n\n",suspendtime);

            if(0 == suspendtime
            && HI_TRUE == cecStatuss.bEnable
                && HI_FALSE == get_HDMI_CEC_Suspend_Status()
               )
             {
                 property_set("persist.sys.cec.status", "true");
                 property_set("suspend.from.tv.cec", "true");
                 //HDMI_Suspend_ReportKeyEvent(KEY_POWER, KPC_EV_KEY_PRESS);
                 //HDMI_Suspend_ReportKeyEvent(KEY_POWER, KPC_EV_KEY_RELEASE);
		        system("am broadcast -a android.ysten.systemupdate -e hdmipowercontrol hdmipoweroff &");
                 ALOGE("HDMI_CEC_Proc: send power key to suspend");
                 break;
            }
            if(HI_TRUE == cecStatuss.bEnable == HI_TRUE
                && HI_FALSE == get_HDMI_CEC_Suspend_Status()
                && HI_FALSE == alrm_set_flag
                && HI_TRUE  == alrm_cancel_flag)
            {
                signal(SIGALRM, HDMI_CEC_Suspend_Timeout);
                ALOGI("Tv cec standby msg %#x ",opcode);
                ret = alarm(0);
                ALOGI("alarm(0) ret = %lu",ret);
                ret = alarm(suspendtime*60 - 3);
                ALOGI("alarm(sus*60) ret = %lu",ret);
                ALOGI("\033[32m set cec suspend alarm time:\33[0m %d minute",suspendtime);
                alrm_set_flag       = HI_TRUE;
                alrm_cancel_flag    = HI_FALSE;
            }
            break;
        case CEC_OPCODE_REPORT_PHYSICAL_ADDRESS:
            {
                HI_U8 type= pstCECCmd->unOperand.stRawData.u8Data[2];
                if(type==0){
                    ALOGE("---> HDMI_CEC_Proc  is TV");
                }
            }
            break;
        case CEC_OPCODE_GET_CEC_VERSION:
            if(pstCECCmd->enDstAdd!=0x0f)
                HDMI_CEC_RESPONSE_CEC_VERSION(enHdmi,pstCECCmd);
            break;
        case CEC_OPCODE_GIVE_PHYSICAL_ADDRESS://0x83 131
            if(pstCECCmd->enDstAdd!=0x0f)
                //response physical address
                HDMI_CEC_RESPONSE_PHYSICAL_ADDRESS(enHdmi,pstCECCmd);
//            HDMI_CEC_RESPONSE_TV_RESUME_BOX(enHdmi,cecStatuss,opcode);
            break;
        case CEC_OPCODE_GIVE_DEVICE_VENDOR_ID:
        if(pstCECCmd->enDstAdd!=0x0f)
            //response stb device vendor id
            HDMI_CEC_RESPONSE_CEC_DEVICE_VENDOR_ID(enHdmi,pstCECCmd);
            break;
        case CEC_OPCODE_GIVE_OSD_NAME://0x46   70 for tv scand and find the box
            if(pstCECCmd->enDstAdd!=0x0f)
                HDMI_CEC_RESPONSE_CEC_OSD_NAME(enHdmi,pstCECCmd);
            break;
        case CEC_OPCODE_VENDOR_COMMAND_WITH_ID://0xA0
            //HDMI_CEC_RESPONSE_VENDOR_COMMAND_WITH_ID(enHdmi,pstCECCmd);
            break;
        case CEC_OPCODE_GIVE_DEVICE_POWER_STATUS:
            if(pstCECCmd->enDstAdd!=0x0f)//
                HDMI_CEC_RESPONSE_DEVICE_POWER_STATUS(enHdmi,pstCECCmd);
            break;
        case CEC_OPCODE_REPORT_POWER_STATUS://0x90
            if(pstCECCmd->enDstAdd!=0x0f)
                HDMI_CEC_RECEIVE_TV_POWER_STATUS(pstCECCmd);
            break;
        case CEC_OPCODE_REQUEST_ACTIVE_SOURCE://0x85 133
            if(pstCECCmd->enDstAdd==0x0f)
                HDMI_CEC_RESPONSE_REQUEST_ACTIVE_SOURCE(enHdmi,pstCECCmd);
//            HDMI_CEC_RESPONSE_TV_RESUME_BOX(enHdmi,cecStatuss,opcode);
            break;
        case CEC_OPCODE_USER_CONTROL_PRESSED:
            if(pstCECCmd->enDstAdd!=0x0f)
                //deal with TV button
                ret = get_HDMI_CEC_Suspend_Enable();
            if(ret!= 1)
                break;
            HDMI_CEC_RESPONSE_CONTROL_PRESSED(enHdmi,pstCECCmd);
            break;
        case CEC_OPCODE_USER_CONTROL_RELEASED:
        if(pstCECCmd->enDstAdd!=0x0f)
            HDMI_CEC_RESPONSE_CONTROL_RELEASED(enHdmi,pstCECCmd);
            break;
        case CEC_OPCODE_MENU_REQUEST:
            if((pstCECCmd->enSrcAdd!=0x0f&&pstCECCmd->enDstAdd!=0x0f&&pstCECCmd->unOperand.stRawData.u8Length==1)
            &&(pstCECCmd->unOperand.stRawData.u8Data[0]>-1&&pstCECCmd->unOperand.stRawData.u8Data[0]<3))
                HDMI_CEC_RESPONSE_MENU_REQUEST(pstCECCmd);
            break;
        case CEC_OPCODE_IMAGE_VIEW_ON://0x04                 4
        case CEC_OPCODE_ROUTING_CHANGE://0x80                128
        case CEC_OPCODE_ACTIVE_SOURCE://0x82                 130
//            HDMI_CEC_RESPONSE_TV_RESUME_BOX(enHdmi,cecStatuss,opcode);
            break;
        case CEC_OPCODE_SET_STREAM_PATH:
            if (pstCECCmd->unOperand.stRawData.u8Data[0] == ((stHdmiSTBPhysicalAddr[0]<<4)|stHdmiSTBPhysicalAddr[1])
                && pstCECCmd->unOperand.stRawData.u8Data[1] == ((stHdmiSTBPhysicalAddr[2]<<4)|stHdmiSTBPhysicalAddr[3]) )
                HDMI_CEC_RESPONSE_REQUEST_ACTIVE_SOURCE(enHdmi,pstCECCmd);
            break;
    }
}

/**
 * get the TV status through HDMI
 * @return
 *      -99  if TV not support CEC or HotPlug
 *      -1    hdmi disconnect
 *       0   TV is standby
 *       1   TV is turn on
 */
HI_S32 get_TV_Status()
{
    HI_S32          ret = HI_FAILURE;
    HI_UNF_HDMI_STATUS_S hdmiStatus;
    HI_UNF_HDMI_CEC_STATUS_S cecStatus;
    //icsl init
    hdmiStatus.bConnected = HI_TRUE;
    hdmiStatus.bSinkPowerOn = HI_TRUE;

    ALOGE("get_TV_Status > TV_feedback_flag = %d ps=%d",TV_feedback_flag,TvPowerStatus);

    ALOGE("stSinkCap.stCECAddr.bPhyAddrValid :%d",stSinkCap.stCECAddr.bPhyAddrValid);
    HDMI_CEC_RESPONSE_CHECK_TV_POWER_STATUS(HI_UNF_HDMI_ID_0);

    ret = HI_UNF_HDMI_GetStatus(HI_UNF_HDMI_ID_0, &hdmiStatus);
    if(ret != HI_SUCCESS)
    {
        ALOGE("HI_UNF_HDMI_GetStatus Fail ret = %d",ret);
    }

    ret = HI_UNF_HDMI_CECStatus(HI_UNF_HDMI_ID_0,&cecStatus);

    ALOGE("cecStatus.enCap :  :%d",cecStatus.enCap);

    if(cecStatus.enCap == HI_UNF_HDMI_CEC_CAP_UNKNOWN)// && TV_UNKNOW == TvPowerStatus)
    {
        sleep(1);
    }
    ALOGE("cecen:%d TV_feedback_flag = %d ps :%d ",stHdmiCecEnable,TV_feedback_flag,TvPowerStatus);

    if(cecStatus.enCap == HI_UNF_HDMI_CEC_CAP_UNSUPPORT)
    {
        ALOGI("ps : %d",TvPowerStatus);
        ret = TV_NO_SUPPORT_CEC;//-- 99
        return ret;
    }

    if(hdmiStatus.bConnected == HI_FALSE || hdmi_enable == HI_FALSE)
    {
        ALOGW("TV Status:hdmi disconnected ");
        ret = TV_DISCONNECT;//--  -1
    }
    else if(cecStatus.enCap == HI_UNF_HDMI_CEC_CAP_SUPPORT)
    {
        ALOGI("support & has hpd ps :%d ",TvPowerStatus);
        ret = TV_TURN_ON;
    }

    if(TV_feedback_flag == HI_TRUE )
    {
        ALOGI("CEC has is enable & tv pws is %s",TvPowerStatus == TV_TURN_ON?" ON":" OFF");
        ret = TvPowerStatus;
    }

    return ret;
}
HI_VOID  Hdmicap_NativeFormat_Strategy(int getcapret)
{
    char perfer[BUFLEN] = {0};
    char province[BUFLEN] = {0};

    property_get("persist.sys.optimalfmt.perfer", perfer, "native");
    ALOGI("persist.sys.optimalfmt.perfer=%s", perfer);

    if (HI_SUCCESS != getcapret)
    {
        if (DEVICE_HDMI_TV == displayType)
        {
            ALOGI("---------->is TV<------------ cap get failed\n");
            if ((strcmp("i50hz", perfer) == 0) || (strcmp("p50hz", perfer) == 0))
            {
                stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_720P_50;
                /*begin:add by zhanghk at 20200321:set defalut resolution is 1080P50hz*/
                property_get("ro.ysten.province", province, "master");
                if(strstr(province,"anhui") != 0){
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_1080P_50;
                }
                /*end:add by zhanghk at 20200321:set defalut resolution is 1080P50hz*/
            }
            else if ((strcmp("i60hz", perfer) == 0) || (strcmp("p60hz", perfer) == 0))
            {
                stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_720P_60;
            }
            /*add by zhaolianghua for AH211 to set default be 1080p @20200319 */
            else if ((strcmp("1080p60hz", perfer) == 0))
            {
                stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_1080P_60;
            }
            /*add by zhaolianghua end*/
            else
            {
                stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_720P_50;
            }
        }
        else
        {
            ALOGI("---------->is PC<------------ cap get failed to 1280*720 vesa\n");
            stSinkCap.enNativeFormat = DISPLAY_FMT_VESA_1280X720_60;
        }
    }
    else
    {
        if (DEVICE_HDMI_TV == displayType)
        {
            if (strcmp("i50hz", perfer) == 0)
            {
                //upgrade to 4096
                if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_4096X2160_50))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_4096X2160_50;
                }
                else if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_4096X2160_25))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_4096X2160_25;
                }
                //brcause 4k format doesn't has i format ,inorder adatpt to max format  when perfer is i50hz , add  3840X2160_50
                else if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_3840X2160_50))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_3840X2160_50;
                }
                else if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_3840X2160_25))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_3840X2160_25;
                }
                else if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_1080i_50))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_1080i_50;
                }
                else
                {
                    ALOGI("perfer is i50hz ,for the not support 1080i to 4k case, adapt result to 720p50");
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_720P_50;
                }
            }
            else if (strcmp("p50hz", perfer) == 0)
            {
                if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_4096X2160_50))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_4096X2160_50;
                }
                else if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_4096X2160_25))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_4096X2160_25;
                }
                //since tv capability is upgrade now , add max fmt lever to 3840X2160 P50
                else if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_3840X2160_50))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_3840X2160_50;
                }
                else if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_3840X2160_25))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_3840X2160_25;
                }
                else if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_1080P_50))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_1080P_50;
                }
                else if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_720P_50))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_720P_50;
                }
                else //if(stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_576P_50)
                {
                    ALOGI("Lowest default to 576p");
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_576P_50;
                }
                /*begin:add by zhanghk at 20200321:set defalut resolution is 1080P50hz*/
                property_get("ro.ysten.province", province, "master");
                if(strstr(province,"anhui") != 0){
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_1080P_50;
                }
                /*end:add by zhanghk at 20200321:set defalut resolution is 1080P50hz*/
            }
            else if (strcmp("i60hz", perfer) == 0)
            {
                if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_4096X2160_60))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_4096X2160_60;
                }
                else if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_4096X2160_30))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_4096X2160_30;
                }
                 //brcause 4k format doesn't has i format ,inorder adatpt to max format
                 //when perfer is i60hz and tv support  , add  3840X2160_60
                else if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_3840X2160_60))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_3840X2160_60;
                }
                else if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_3840X2160_30))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_3840X2160_30;
                }
                else if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_1080i_60))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_1080i_60;
                }
                else
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_720P_60;
                }
            }
            else if (strcmp("p60hz", perfer) == 0)
            {
                if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_4096X2160_60))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_4096X2160_60;
                }
                else if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_4096X2160_30))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_4096X2160_30;
                }
                else if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_3840X2160_60))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_3840X2160_60;
                }
                else if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_3840X2160_30))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_3840X2160_30;
                }
                else if (HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_1080P_60))
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_1080P_60;
                }
                else
                {
                    stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_720P_60;
                }
            }
            else if (0 == strcmp("max_fmt", perfer))
            {
                stSinkCap.enNativeFormat = getCurrentMaxSupportFmt();
            }
            else//native case
            {
                int custsize = sizeof(Customer_fmt_list)/sizeof(Customer_fmt_list[0]);
                if(custsize != 0)
                {
                    int i;
                    for(i=0;i< custsize;i++)
                    {
                        if(HI_TRUE == is_format_support(&stSinkCap,Customer_fmt_list[i]))
                        {
                            stSinkCap.enNativeFormat = (HI_UNF_ENC_FMT_E)Customer_fmt_list[i];
                            ALOGI("use list not empty, use the first sp in user list as native ");
                            break;
                        }
                    }
                    //ALOGI("use list not empty, not found sp in u list ,use read edid ");
                    //if customer list not found support; use edid native
                }
                else
                    //defautl edid native
                    ALOGI("default case use edid native !!!!!!");
            }

            // native format err case
            if(0 == strcmp("native", perfer) &&
                stSinkCap.enNativeFormat > HI_UNF_ENC_FMT_720P_50)
            {
                if ((stSinkCap.enNativeFormat < HI_UNF_ENC_FMT_3840X2160_24)
                    || (stSinkCap.enNativeFormat == HI_UNF_ENC_FMT_BUTT))
                {
                    ALOGE("\n tv: get optimum format failed \n");
                    if(HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_1080P_60)) {
                        stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_1080P_60;
                    } else if(HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_1080P_50)) {
                        stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_1080P_50;
                    } else if(HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_1080i_60)) {
                        stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_1080i_60;
                    } else if(HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_1080i_50)) {
                        stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_1080i_50;
                    } else if(HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_720P_60)) {
                        stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_720P_60;
                    } else if(HI_TRUE == is_format_support(&stSinkCap,HI_UNF_ENC_FMT_720P_50)) {
                        stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_720P_50;
                    } else {
                        stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_720P_60;
                    }
                }
            }
        }
        else if (displayType == DEVICE_HDMI_PC_MONITOR)
        {
            stSinkCap.enNativeFormat = getVesaAdapt_format();
            ALOGI("It's pc monitor mode,and hdmi cap get od, vesa adatp fmt is: %d",stSinkCap.enNativeFormat);
        }
        else
            ALOGE("Device Type error :%d",displayType);
    }
    ALOGI("enNativeFormat %s result is : %d \n",perfer, stSinkCap.enNativeFormat);
}

HI_VOID cvbs_and_hdmi_strategy(HI_BOOL ishpd)
{
    HI_UNF_DISP_INTF_S stIntf_disp;
    memset(&stIntf_disp, 0, sizeof(stIntf_disp));

    stIntf_disp.enIntfType = HI_UNF_DISP_INTF_TYPE_CVBS;
    stIntf_disp.unIntf.stCVBS.u8Dac = cvbs_dac_port;
    ALOGI("is : %s",ishpd ? "hotplug ": "uplug");

    HI_S32 ret = 0;

    HI_BOOL isMute = isMutexStrategyEnabledForHdmiAndCvbs();
    ALOGI("isMutexStrategyEnabledForHdmiAndCvbs %s",isMute ? "YES" : "NO");
    if(isMute)//fei tong yuan
    {
        if(ishpd)//hot plug
        {
            set_output_enable(DISPLAY_CVBS,HI_FALSE);

            //for customer old base case, cvbs maybe on disp0
            ret = HI_UNF_DISP_DetachIntf(HI_UNF_DISPLAY0, &stIntf_disp, 1);
            if (HI_SUCCESS != ret)
            {
                ALOGE("HD HI_UNF_DISP_DetachIntf failed! %#x",ret);
            }
            ALOGI("detach cvbs from disp0 %#x",ret);
           
            //add by xue
         system("am broadcast -a android.ysten.systemupdate -e hdmiplugevent hotpulg &");
             //add by xue

        }
        else//uplug
        {
            /*open cvbs port end*/
            set_output_enable(DISPLAY_CVBS,HI_TRUE);
         system("am broadcast -a android.ysten.systemupdate -e hdmiplugevent unpulg &");
        }
    }
    else
    {
        if(ishpd)//hot plug
        {
            if(attachtodisp1==HI_TRUE)
            {
                ret = HI_UNF_DISP_DetachIntf(HI_UNF_DISPLAY1, &stIntf_disp, 1);
                if (HI_SUCCESS != ret)
                {
                    ALOGE("HD HI_UNF_DISP_DetachIntf failed! %#x",ret);
                }

                ret = HI_UNF_DISP_AttachIntf(HI_UNF_DISPLAY0, &stIntf_disp, 1);
                if (HI_SUCCESS != ret)
                {
                    ALOGE("HD HI_UNF_DISP_AttachIntf failed! %#x",ret);
                }
            }
        }
    }
    enable_debug_log(DEBUG_HDMI_INIT);
}

HI_VOID Hdmicap_check_device_support(HI_S32 getCapResult)
{
    if (HI_SUCCESS == getCapResult)
    {
        if (HI_TRUE == stSinkCap.bSupportHdmi)
        {
            displayType            = DEVICE_HDMI_TV;
        }
        else
        {
            //read real edid ok && sink not support hdmi,then we run in dvi mode
            displayType            = DEVICE_HDMI_PC_MONITOR;
        }
        ALOGI("HDMI capability get success, display type :%d [%s]\n",displayType,displayType == 1 ? "TV" : "PC");
    }
    else
    {
        //when get capability fail,use default mode
        if(HI_UNF_HDMI_DEFAULT_ACTION_HDMI == g_enDefaultMode)
        {
            displayType            = DEVICE_HDMI_TV;
        }
        else
        {
            displayType            = DEVICE_HDMI_PC_MONITOR;
        }
        ALOGI("HDMI capability get fail, displaytype: %d look up defalt type is[%s]\n",displayType,displayType == DEVICE_HDMI_TV? "TV" : "PC");
    }

    return;
}

HI_VOID Hdmicap_Set_VDP(HI_S32 getCapResult)
{
    HI_S32 ret       = HI_SUCCESS;
    HI_S32 hdr_value = -1;

    if (HI_SUCCESS != getCapResult)
    {
        ALOGE("getCapResult is HI_FALSE!\n");
        return;
    }

    ALOGI("\nSink Native format is %d \n",(int)stSinkCap.enNativeFormat);

    ret = HI_UNF_DISP_SetSinkCapability(HI_UNF_DISPLAY1,&stSinkCap);
    if(HI_SUCCESS != ret)
    {
        ALOGE("Hdmicap_Set_VDP>HI_UNF_DISP_SetSinkCapability failed! %#x",ret);
    }

    ALOGI("Set hdmi sink cap to disp drv %#x", ret);

    return;
}

HI_VOID hdr_type_strategy(HI_BOOL edidstate)
{
    HI_S32 hdr_value       = 0;
    HI_CHAR buffer[BUFLEN] = {0};

    if(edidstate)
    {
        property_set("persist.sys.hdrmode", "-1");
        hdr_value = HDRTYPE_AUTO;
        ALOGI("hdmi edid is change, hdr_value is AUTO\n");
    }
    else
    {
        property_get("persist.sys.hdrmode", buffer, "-1");
        hdr_value = atoi(buffer);
        ALOGI("hpd hdmi edid not changed hdr set to before %d\n", hdr_value);
    }

    //for dolby cert only
    property_get("persist.sys.dolbycert",buffer,"false");
    if(strcmp("true",buffer)==0)
    {
        Dolby_cert = HI_TRUE;
        ALOGI("dolby cert , we auto base on hdmi tv cap %d\n", hdr_value);
        set_HDRType(HDRTYPE_AUTO);// sdr dolby hdr hlg ...
        return ;
    }

    if(HI_TRUE == is_playing_dolby)
    {
        ALOGI("dolby is playing!");
        hdr_value = STREAM_DB;
    }

    set_HDRType(hdr_value);// sdr dolby hdr hlg ...

    return;
}

HI_VOID Hdmicap_get_adapt_format(HI_S32 getCapRet,HI_S32 cr_fmt_ret , display_format_e *hpd_fmt , display_format_e current_format)
{
    ALOGI("\n--Hdmicap_get_adapt_format--begin");

    HI_S32 ret = HI_SUCCESS;
    HI_CHAR buffer[BUFLEN] ={0};
    int basefmt = 0;
    int userfmt = 0;
    int isFmtadp_Enable = isFormatAdaptEnable();

    property_get("persist.sys.baseformat",buffer,"8");
    basefmt = atoi(buffer);

    memset(buffer, 0, sizeof(buffer));
    property_get("persist.sys.userfmt",buffer,"xxx");
    if(0 != strcmp(buffer,"xxx"))
    {
        ALOGI("User set fmt before is %s",buffer);
        userfmt= atoi(buffer);
    }
    else
    {
        ALOGI("get user set fmt error %s , to base",buffer);
        userfmt= basefmt;
    }
    if (HI_SUCCESS == getCapRet)
    {
        if(displayType == DEVICE_HDMI_TV) //tv case
        {
            if (HI_TRUE == isFmtadp_Enable)
            {
                *hpd_fmt = (display_format_e)stSinkCap.enNativeFormat;
                ALOGI("Optimal format is enabled, to adatp result %d",*hpd_fmt);
            }
            else
            {
                if (HI_TRUE == is_format_support(&stSinkCap,userfmt))
                {
                    *hpd_fmt = userfmt;
                    ALOGI("Optimal format is disable & support user format before set %d",*hpd_fmt);
                }
                else
                {
                    *hpd_fmt = (display_format_e)stSinkCap.enNativeFormat;
                    ALOGI("Optimal format is disable & not support the format user set before, to adapt result: %d ",current_format);
                }
            }
        }
        else
        {
            ALOGI("---------->is PC Monitor<------------\n");
            if (HI_TRUE == isFmtadp_Enable)
            {
                *hpd_fmt = (display_format_e)stSinkCap.enNativeFormat;
                ALOGI("fmt adapt is enable to adatp result:%d",*hpd_fmt);
            }
            else
            {
                ALOGI("fmt adapt disable mode .");
                if(HI_TRUE == is_format_support(&stSinkCap,current_format))
                {
                    *hpd_fmt = current_format;
                    ALOGI("current format is support !%d",*hpd_fmt);
                }
                else
                {
                    *hpd_fmt = (display_format_e)stSinkCap.enNativeFormat;
                    ALOGE("current format not support ,to adatp result:%d",*hpd_fmt);
                }
            }

        }
    }
    else
    {   // sink cap fail case
        if (DEVICE_HDMI_TV == displayType) //tv case
        {
            if(((userfmt >= DISPLAY_FMT_3840X2160_24) && (userfmt<=DISPLAY_FMT_3840X2160_29_97))
                || (userfmt  >= DISPLAY_FMT_BUTT ))
            {
                ALOGW("TV cap get fail ,userfmt is 4K:%d or error,change to :%d",userfmt,stSinkCap.enNativeFormat);
                *hpd_fmt = stSinkCap.enNativeFormat;//720P
            }
            else
            {
                *hpd_fmt = userfmt;
                ALOGW("TV cap get fail ,but set is not 4k,keep %d",userfmt);
            }
        }
        else
        {
            ALOGW("---------->is PC Monitor<------------\n");
            *hpd_fmt = (display_format_e)stSinkCap.enNativeFormat;
            ALOGW("cap faile to adatp result %d",*hpd_fmt);
        }
    }

    ALOGI("--Hdmicap_get_adapt_format--end hpd_fmt: %d \n\n",*hpd_fmt);
}
HI_VOID Hdmicap_check_4kP50P60_support(HI_S32 capret)
{
    HI_U8 formatCout  = 0 ;
    HI_U8 P50P60Num = 4;
    HI_BOOL capok  = HI_FALSE;
    HI_U32 Fmt4k_P50P60[4] = {
        HI_UNF_ENC_FMT_3840X2160_50,
        HI_UNF_ENC_FMT_3840X2160_60,
        HI_UNF_ENC_FMT_4096X2160_50,
        HI_UNF_ENC_FMT_4096X2160_60
    };

    if(HI_SUCCESS == capret)
    {
        capok = HI_TRUE;
    }

    do
    {
        ALOGI("4k[%d] = %d",Fmt4k_P50P60[formatCout],capok ? is_format_support(&stSinkCap,Fmt4k_P50P60[formatCout]) : HI_FALSE);
        formatCout++;
    }
    while(formatCout< P50P60Num);
}

HI_VOID Hdmicap_get_new_hdmiattr(display_format_e hpd_format,HI_BOOL edidChange)
{
    HI_S32                   ret = HI_SUCCESS;
    HI_CHAR                  buffer[BUFLEN] = {0};
    HI_UNF_HDMI_VIDEO_MODE_E matchColorSpace = HI_UNF_HDMI_VIDEO_MODE_YCBCR444;
    HI_UNF_HDMI_DEEP_COLOR_E matchDeepColor  = HI_UNF_HDMI_DEEP_COLOR_24BIT;

    if (HI_TRUE == edidChange)
    {
        //clear user set colorspace flag,match agian base on tv cap.
        //because colorspace capability has a strong relativity with format,
        //so, here we cann't use user flag as first choice when match colorspace,
        property_get("persist.sys.colorspacesettype", buffer, "-1");
        if(strcmp("-1",buffer)!= 0)
        {
            ALOGI("hdmi edid change clear user flag");
            property_set("persist.sys.colorspacesettype","-1");
        }
    }

    //color space & deepColor adapt
    ret = match_format_colorspace(hpd_format, &matchColorSpace, &matchDeepColor);

    //update color space & deep color  to match result to global variable (for later save base action)
    baseparam_disp_set(&matchColorSpace, COLORSPACE, HI_UNF_DISP_HD0);
    baseparam_disp_set(&matchDeepColor, DEEPCOLOR, HI_UNF_DISP_HD0);
    ALOGI("update match result to global variable for later save, clr %d dep %d",matchColorSpace,matchDeepColor);
    if(HI_SUCCESS == ret)
    {
        stHdmiAttr.enVidOutMode    = matchColorSpace;
        stHdmiAttr.enDeepColorMode = matchDeepColor;
    }

    if (DEVICE_HDMI_TV == displayType)//TV
    {
        stHdmiAttr.bEnableHdmi          = HI_TRUE;
        stHdmiAttr.bEnableAudio         = HI_TRUE;
        stHdmiAttr.bEnableVideo         = HI_TRUE;
        stHdmiAttr.bEnableAudInfoFrame  = HI_TRUE;
        stHdmiAttr.bEnableAviInfoFrame  = HI_TRUE;
    }
    else//pc case
    {
        stHdmiAttr.bEnableHdmi          = HI_FALSE;
        stHdmiAttr.bEnableAudio         = HI_FALSE;
        stHdmiAttr.bEnableVideo         = HI_TRUE;
        stHdmiAttr.bEnableAudInfoFrame  = HI_FALSE;
        stHdmiAttr.bEnableAviInfoFrame  = HI_FALSE;
    }

}
HI_BOOL check_hdmi_hpd_attr_changed()
{
    HI_BOOL isChage = HI_FALSE;
    HI_S32 ret = HI_SUCCESS;

    HI_UNF_HDMI_ATTR_S             current_Attr;

    ALOGI_IF(dbuglog,"checking attr :");
    HDMI_PrintAttr(&stHdmiAttr);

    ret =  HI_UNF_HDMI_GetAttr(HI_UNF_HDMI_ID_0, &current_Attr);

    ALOGI_IF(dbuglog,"current  attr :");
    HDMI_PrintAttr(&current_Attr);

    if(  stHdmiAttr.bEnableHdmi         != current_Attr.bEnableHdmi
      || stHdmiAttr.bEnableAudio        != current_Attr.bEnableAudio
      || stHdmiAttr.bEnableVideo        != current_Attr.bEnableVideo
      || stHdmiAttr.bEnableAudInfoFrame != current_Attr.bEnableAudInfoFrame
      || stHdmiAttr.bEnableAviInfoFrame != current_Attr.bEnableAviInfoFrame
      || stHdmiAttr.enVidOutMode        != current_Attr.enVidOutMode
      || stHdmiAttr.enDeepColorMode     != current_Attr.enDeepColorMode)
    {
        isChage = HI_TRUE;
    }

    ALOGI("Hdmi attr Change : %s",isChage ? "YES ." : "NO .");

    return isChage;

}


HI_S32 reset_format_and_hdmiAttr(HI_UNF_HDMI_ATTR_S stHdmiAttr, display_format_e format, HI_BOOL isFormatChange,HI_BOOL isAttrChage)
{

    HI_S32 ret = HI_SUCCESS;
    const HI_U32 channel_num = 2;
    HI_UNF_DISP_ISOGENY_ATTR_S isogeny_attr[channel_num];
    int format_sd = HI_UNF_ENC_FMT_PAL;
    hdmi_event_t ev;

    if(format >= DISPLAY_FMT_BUTT)
    {
        ALOGE("set format exceed limits %d",format);
    }
    ALOGI("FORMAT %D  ATTR %D",isFormatChange,isAttrChage);

    if(isFormatChange == HI_FALSE
        && isAttrChage == HI_FALSE)
    {
        ALOGI("Both format and hdmi attr not change at all !\n");
        return HI_SUCCESS;
    }

    ret = HI_UNF_HDMI_Stop(HI_UNF_HDMI_ID_0);
    if (HI_SUCCESS != ret)
    {
         ALOGE("HI_UNF_HDMI_Stop failed, ret=%p in set_format()", (void*)ret);
    }

    if(isFormatChange)
    {
        ALOGI("format change --> retset %d",format);
#ifdef TURN_ON_SD0
        memset(isogeny_attr, 0, sizeof(HI_UNF_DISP_ISOGENY_ATTR_S) * channel_num);
        isogeny_attr[0].enDisp = HI_UNF_DISPLAY1;
        isogeny_attr[0].enFormat = (HI_UNF_ENC_FMT_E)format;
        isogeny_attr[1].enDisp = HI_UNF_DISPLAY0;
        get_sd_format(format,&(isogeny_attr[1].enFormat));

        ret = HI_UNF_DISP_SetIsogenyAttr(isogeny_attr, channel_num);
        if (HI_SUCCESS != ret)
        {
            ALOGE("HI_UNF_DISP_SetIsogenyAttr Err--- %p", (void*)ret);
        }
        ALOGI("hpd set format[hd:%d] [sd:%d]ret %d ",isogeny_attr[0].enFormat,isogeny_attr[1].enFormat,ret);
#else
        ALOGI("HD HI_UNF_DISP_SetFormat:%d!",format);
        ret = HI_UNF_DISP_SetFormat(HI_UNF_DISP_HD0, (HI_UNF_ENC_FMT_E)format);
        if (HI_SUCCESS != ret)
        {
            ALOGE("HD HI_UNF_DISP_SetFormat failed!");
            return ret;
        }
        ALOGI("set format[hd:%d]ret %d ",ret,format);
#endif

        ev.event_type   = (int)disp_format_change;
        ev.event_msg    = (int)format;
        notify_callback(&ev);
    }

    if(isAttrChage)
    {
        ret = HI_UNF_HDMI_SetAttr(HI_UNF_HDMI_ID_0, &stHdmiAttr);
        ALOGI("attr change set hdmi attr ret:%d , dpcl %d clrs %d",ret,stHdmiAttr.enVidOutMode,stHdmiAttr.enDeepColorMode);
    }

    //updata format to global variable
    store_format((display_format_e)format, (display_format_e)format_sd);

    return ret;
}

HI_VOID Hdmi_cec_strategy(HI_BOOL edidChage)
{
    HI_S32 ret = HI_SUCCESS;
    TvPowerStatus = TV_UNKNOW;
    HDMI_CEC_RESPONSE_CHECK_TV_POWER_STATUS(HI_UNF_HDMI_ID_0);

    if(edidChage)
    {
        //when change tv ,we clear the cec status .
//        TvPowerStatus = TV_UNKNOW;
//        TV_feedback_flag = HI_FALSE; //HAS BEEN CLEAR IN UNPLUG
//      HDMI_CEC_RESPONSE_CHECK_TV_POWER_STATUS(HI_UNF_HDMI_ID_0);
    }
    if(get_HDMI_CEC_Suspend_Enable() == HI_TRUE
        && get_HDMI_CEC_Suspend_Status() == HI_TRUE)
    {
        //TvPowerStatus = TV_STANDBY;
        //for not block displaysetting init complete
        pthread_t sendtrd;
        ret = pthread_create(&sendtrd, NULL,HDMI_CEC_Resume,NULL);//new thread
        if(ret != 0)
            ALOGE("hot plug:resume command sending thread create fail");
        else
            ALOGI("hot plug: resume command sending thread create success");
    }

    if(HI_TRUE == stSinkCap.stCECAddr.bPhyAddrValid )
    {
        property_set("persist.sys.cecsupport", "true");
    }
    else
    {
        property_set("persist.sys.cecsupport", "false");
    }

    property_set("persist.sys.cec.status", "false");
}

HI_VOID Hdmicap_hdcp_strategy()
{
    HI_S32 ret = HI_FAILURE;
    //HI_CHAR buffer[BUFLEN] ={0};
    //HI_CHAR buffer_new[BUFLEN] = {0};
    HI_S32 User_enabledata = 0;
    HI_UNF_HDMI_ATTR_S             current_Attr;

    ret =  HI_UNF_HDMI_GetAttr(HI_UNF_HDMI_ID_0, &current_Attr);
    if(ret != HI_SUCCESS)
    {
        ALOGE("hdcp_strategy:HI_UNF_HDMI_GetAttr failed! %#x",ret);
    }

    User_enabledata = isHdmiHDCPenable();
    if(!(0 == User_enabledata || 1== User_enabledata))
    {
        ALOGE("user enable data invalid : %d ",User_enabledata);
        User_enabledata = 0;
    }
/*
    memset(buffer, 0, sizeof(BUFLEN));
    ret = property_get("persist.sys.hdcpcap",buffer,"none");
    ALOGI("get: %d %s",ret,buffer);

    if(HI_TRUE == stSinkCap.stHDCPSupport.bHdcp14Support
        && HI_TRUE == stSinkCap.stHDCPSupport.bHdcp22Support
      )
    {
        strcat(buffer_new,"1422");
    }
    else if(HI_FALSE == stSinkCap.stHDCPSupport.bHdcp14Support
        && HI_TRUE == stSinkCap.stHDCPSupport.bHdcp22Support
      )
    {
        strcat(buffer_new,"22");
    }
    else if(HI_TRUE == stSinkCap.stHDCPSupport.bHdcp14Support
        && HI_FALSE == stSinkCap.stHDCPSupport.bHdcp22Support
      )
    {
        strcat(buffer_new,"14");
    }
    else
    {
        ALOGW("Not support both 2.2 andr 1.4 version hdcp !");
        strcat(buffer_new,"none");
    }

    if(strcmp(buffer_new,buffer) != 0)
    {
        ret = property_set("persist.sys.hdcpcap",buffer_new);
        ALOGI("hdcp cap of tv: %s ret :%d",buffer_new,ret);
    }
*/
    //HDCP enable flag
    stHdmiAttr.bHDCPEnable = (HI_BOOL)User_enabledata;

    if(HI_FALSE == stHdmiAttr.bHDCPEnable )
    {
         ret = HI_UNF_DISP_GetVideoAlpha(HI_UNF_DISPLAY1,&videoalhpa);
         if(HI_SUCCESS != ret)
         {
             ALOGE("HI_UNF_DISP_GetVideoAlpha fail ret =%#x !",ret);
         }
         if(videoalhpa != 100)
         {
             ret = HI_UNF_DISP_SetVideoAlpha(HI_UNF_DISPLAY1,100);
             if(HI_SUCCESS != ret)
             {
                 ALOGE("HI_UNF_DISP_SetVideoAlpha fail ret =%#x !",ret);
             }
        }
        HDCPFailCount = 0;
    }

    if(HI_TRUE == stHdmiAttr.bHDCPEnable
        && HI_TRUE == stSinkCap.stHDCPSupport.bHdcp22Support)
    {
        stHdmiAttr.enHDCPMode = HI_UNF_HDMI_HDCP_MODE_2_2;
        ALOGI("2.2 mode ");
    }
    else if(HI_TRUE == stHdmiAttr.bHDCPEnable
        && HI_TRUE == stSinkCap.stHDCPSupport.bHdcp14Support)
    {
        ALOGI("1.4 mode key: %s",Hdcp14KEYLoadFlag ? "LOAD ." : "NOT LOAD .");
        stHdmiAttr.enHDCPMode = HI_UNF_HDMI_HDCP_MODE_1_4;
        stHdmiAttr.bHDCPEnable = Hdcp14KEYLoadFlag;
    }
    else
    {
        ALOGW("User not enable HDCP MODE %d: or hdcp not support",User_enabledata);
        is_restart = HI_FALSE;
        return ;
    }

    if(!(stHdmiAttr.bHDCPEnable  == current_Attr.bHDCPEnable
        && stHdmiAttr.enHDCPMode  == current_Attr.enHDCPMode))
    {
        if(HI_TRUE == is_restart)
        {
            ALOGI("is first restart , not stop hdmi!");
        }
        else
        {
            ALOGI(" not first restart, stop hdmi!");
            ret = HI_UNF_HDMI_Stop(HI_UNF_HDMI_ID_0);
            if(ret != HI_SUCCESS)
            {
                ALOGE("HI_UNF_HDMI_Stop Err--- %p", (void*)ret);
            }
        }
        ret = HI_UNF_HDMI_SetAttr(HI_UNF_HDMI_ID_0, &stHdmiAttr);
        ALOGI("hdcp :%d , en: %d mode: %d",ret,stHdmiAttr.bHDCPEnable,stHdmiAttr.enHDCPMode);
    }
    else
        ALOGI("Hdcp attr ,both mode and enable param not change!,no need to set again.");

}


HI_VOID customer_tv_property(display_format_e format,HI_BOOL edidChage,HI_S32 capret)
{
    HI_CHAR buffer[BUFLEN] ={0};
    HI_CHAR buffer2[BUFLEN] ={0};

    property_get("ro.product.target", buffer, "0");
    ALOGI("hpd:%s",buffer);

    TVWMax = (HI_U32) stSinkCap.stBaseDispPara.u8MaxImageWidth;
    TVHMax = (HI_U32) stSinkCap.stBaseDispPara.u8MaxImageHeight;
    if(HI_TRUE == edidChage
        && capret == HI_SUCCESS
        && (strcmp(buffer,"unicom")==0||strcmp(buffer,"telecom")==0)
    )
    {
        setTVproperty(format);
    }

    //store_buffer <-- persist.sys.hdmi.cap
    strcpy(buffer, stSinkCap.stMfrsInfo.u8MfrsName);
    sprintf(buffer2,"%d",stSinkCap.stMfrsInfo.u32ProductCode);

    strcat(buffer, buffer2);

    property_set("pq.procuct.tv.name",buffer);
}

HI_BOOL is_format_support(HI_UNF_EDID_BASE_INFO_S *stSinkCap,HI_UNF_ENC_FMT_E format)
{
    HI_BOOL support = HI_FALSE;
    int cus_fmt_size =  sizeof(Customer_fmt_list)/sizeof(Customer_fmt_list[0]);
    int i = 0;
    if(cus_fmt_size != 0)
    {
        for(i = 0; i < cus_fmt_size; i++)
        {
            //ALOGI(">>>>>>> list[%d] = %d",i,Customer_fmt_list[i]);
            if(format == Customer_fmt_list[i])
            {
                ALOGI("user list contain this fmt,then check the read edid for support it or nost !");
                goto REALCAP;
            }
        }
        //ALOGI("not in user list ,not support !");
        return HI_FALSE;
    }
    else
    {   //default case fmt list is null
        //ALOGI("user list is null ");
    }
REALCAP:
    if(format == HI_UNF_ENC_FMT_3840X2160_50
    || format == HI_UNF_ENC_FMT_3840X2160_60
    || format == HI_UNF_ENC_FMT_4096X2160_50
    || format == HI_UNF_ENC_FMT_4096X2160_60)
    {
        if (HI_TRUE== stSinkCap->bSupportY420Format[format]&&
            HI_TRUE== stSinkCap->stColorSpace.bYCbCr420&&
            get_pixel_clock(format)/2 <= stSinkCap->u32MaxTMDSClock * 1000)
        {
            support = HI_TRUE;
        }
        else if ((HI_TRUE == stSinkCap->bSupportFormat[format])&&
            get_pixel_clock(format) <= stSinkCap->u32MaxTMDSClock * 1000)
        {
            support = HI_TRUE;
        }//else is defaut false
    }
    else
    {
        support = stSinkCap->bSupportFormat[format];
    }
    ALOGI_IF(dbuglog,"is format:[%d] support: %s ",format,support ? "YES ." : "NO .");
    return support;
}

void HDMI_HotPlug_Proc(HI_VOID *pPrivateData)
{

    ALOGI_IF(DEBUG_HDMI_INIT, "HDMI Hotplug event enter %s(%d)" , __func__, __LINE__);
    HI_S32  ret            = HI_SUCCESS;
    HI_S32  fmtRet         = HI_SUCCESS;
    HI_BOOL attrChage      = HI_FALSE;
    HI_BOOL formatChage    = HI_FALSE;

    HDMI_ARGS_S      *pArgs = (HDMI_ARGS_S*)pPrivateData;
    HI_UNF_HDMI_ID_E  hHdmi =  pArgs->enHdmi;

    display_format_e current_format  = DISPLAY_FMT_1080i_60;
    display_format_e hpd_adp_fmt     = DISPLAY_FMT_1080i_60;

    ALOGI("\n --- Get HDMI event: HOTPLUG. --- \n");

    //cvbs and hdmi strategy
    cvbs_and_hdmi_strategy(HI_TRUE);

    //get hdmi sink capability
    getCapRet = HI_UNF_HDMI_GetSinkCapability(hHdmi, &stSinkCap);
    ALOGI("\nGet hdmiCap from UNF ret:%d %s\n", getCapRet,getCapRet == HI_SUCCESS ? "success":"fail");

    //check 4K50/60 support of sink capability, store in the same android 4K cap
    Hdmicap_check_4kP50P60_support(getCapRet);

    //Set new SinkCap to VDP
    Hdmicap_Set_VDP(getCapRet);

    //check device support capability from hdmi cap
    Hdmicap_check_device_support(getCapRet);

    //get hdmi edid and compare with before
    isEdidChange = isHdmiEdidChanged();

    //get hdmi attr .
    ret = HI_UNF_HDMI_GetAttr(hHdmi,&stHdmiAttr);
    if(ret != HI_SUCCESS)
    {
        ALOGE("hotplug>HI_UNF_HDMI_GetAttr failed! %#x",ret);
    }
    ALOGI("crt clr :%d dep: %d ",stHdmiAttr.enVidOutMode,stHdmiAttr.enDeepColorMode);
    //format adapt base on customer perfer and hdmi cap
    Hdmicap_NativeFormat_Strategy(getCapRet);

    fmtRet = HI_UNF_DISP_GetFormat(HI_UNF_DISP_HD0,&current_format);
    if(fmtRet != HI_SUCCESS)
    {
        ALOGE("hotplug>HI_UNF_DISP_GetFormat failed!### ret %d",fmtRet);
    }
    ALOGI("Get current disp1 format from UNF is %d ",current_format);

    ALOGI("display Type[%d] is %s",displayType,displayType==1 ? "TV" :"PC");
    Hdmicap_get_adapt_format(getCapRet,fmtRet, &hpd_adp_fmt, current_format);

    formatChage = (hpd_adp_fmt == current_format)? HI_FALSE : HI_TRUE;

    ALOGI("format adapt result [%d] %s",hpd_adp_fmt,formatChage ? "Change ." : "Not Change .");

    Hdmicap_get_new_hdmiattr(hpd_adp_fmt,isEdidChange);

    //check attr change or not
    attrChage = check_hdmi_hpd_attr_changed();

    reset_format_and_hdmiAttr(stHdmiAttr,hpd_adp_fmt,formatChage,attrChage);

    Hdmicap_hdcp_strategy();

    ret = HI_MPI_HDMI_GetResumeState(hHdmi);
    if (ret == HI_TRUE)
    {
        ret = HI_UNF_HDMI_Start(hHdmi);
        if (HI_SUCCESS != ret)
        {
            ALOGE("HI_UNF_HDMI_Start failed:%#x\n",ret);
        }
        ALOGI("not under suspend, start hdmi %d\n",ret);
    }
    else
    {
        ALOGI("under suspend, not start hdmi %d\n",ret);
    }
    ALOGI("hpd save base");
    baseparam_save();
    //do HDR   strategy
    hdr_type_strategy(isEdidChange);

    Hdmi_cec_strategy(isEdidChange);// new a thread to send cec resume cmd

    customer_tv_property(hpd_adp_fmt,isEdidChange,getCapRet);

    ret = set_HDMI_Suspend_Enable(get_HDMI_Suspend_Enable());
    if(HI_SUCCESS != ret) {
        ALOGW("hdmi suspend enable error %d",ret);
    }
    if(InitComplete == HI_FALSE)
    {
        InitComplete = HI_TRUE;
    }
    ALOGI_IF(DEBUG_HDMI_INIT, "HDMI Hotplug event done %s(%d)\n\n\n" , __func__, __LINE__);
    return;

}

HI_VOID HDMI_UnPlug_Proc(HI_VOID *pPrivateData)
{
    ALOGI("\n ----------HDMI UnPlug-----------\n");
    HI_UNF_DISP_INTF_S  stIntf_disp;
    HDMI_ARGS_S         *pArgs      = (HDMI_ARGS_S*)pPrivateData;
    HI_UNF_HDMI_ID_E    hHdmi       =  pArgs->enHdmi;
    HI_S32              ret         = HI_SUCCESS;
    HI_CHAR             buffer[5]   = {0};
    HI_UNF_ENC_FMT_E    uplug_fmt   = HI_UNF_ENC_FMT_PAL;

    hdmi_event_t ev;
    ret = HI_UNF_DISP_GetFormat(HI_UNF_DISPLAY1,&uplug_fmt);
    if(HI_SUCCESS != ret)
    {
        ALOGE("HI_UNF_DISP_GetFormatErr--- %p", (void*)ret);
    }

    sprintf(buffer,"%d", uplug_fmt);
    ALOGI("get display1 format is %d",uplug_fmt);

    memset(&stIntf_disp, 0, sizeof(stIntf_disp));

    stIntf_disp.enIntfType              = HI_UNF_DISP_INTF_TYPE_CVBS;
    stIntf_disp.unIntf.stCVBS.u8Dac     = cvbs_dac_port;

    ret = HI_UNF_HDMI_Stop(hHdmi);
    if(ret != HI_SUCCESS)
    {
        ALOGE("HI_UNF_HDMI_Stop Err--- %p", (void*)ret);
    }

    stHdmiStatus.bConnected = HI_FALSE;
    displayType = DEVICE_HDMI_NO_CONNECT;

    //cvbs_and_hdmi_strategy(HI_FALSE);//uplug

    //to sdr
    ret = HI_UNF_DISP_SetHDRType(HI_UNF_DISPLAY1, HI_UNF_DISP_HDR_TYPE_NONE);
    if (ret != HI_SUCCESS)
    {
        ALOGE("HI_UNF_DISP_SetHDRType to sdr %#x",ret);
    }

    //set bt601
    ret = HI_UNF_DISP_SetOutputColorSpace(HI_UNF_DISPLAY1, HI_UNF_DISP_COLOR_SPACE_BT601);
    if (ret != HI_SUCCESS)
    {
        ALOGE("attach cvbs to display1  HI_UNF_DISP_AttachIntf %#x",ret);
    }

    get_sd_format(uplug_fmt,&mFormat);
    //set standard format.
    if(uplug_fmt != DISPLAY_FMT_PAL &&
       uplug_fmt != DISPLAY_FMT_NTSC)
    {
        ret = HI_UNF_DISP_DetachIntf(HI_UNF_DISP_HD0, &stIntf_disp, 1);
        if(HI_SUCCESS != ret)ALOGE("HI_UNF_DISP_DetachIntf error %p", (void*)ret);

        //we must sleep a while  for cvbs closed complete!
        //or there is a bad effect on screen e.g. screen dazzling
        sleep(1);
        if(TvPowerStatus != TV_STANDBY)
        {
            ALOGI("not under cec stanby state ,set fmt! %d",mFormat);
            ret = HI_UNF_DISP_SetFormat(HI_UNF_DISP_HD0,mFormat);
            if(HI_SUCCESS != ret)
                ALOGE("HI_UNF_DISP_SetFormat error %p", (void*)ret);
            ev.event_type   = (int)disp_format_change;
            ev.event_msg    = (int)mFormat;
            notify_callback(&ev);
        }
        else
            ALOGI("has  under cec stanby state ,not set fmt!");
    }

    stSinkCap.enNativeFormat = mFormat;

    //attach cvbs to display1.
    ret = HI_UNF_DISP_AttachIntf(HI_UNF_DISPLAY1, &stIntf_disp, 1);
    if (ret != HI_SUCCESS)
    {
        ALOGE("attach cvbs to display1  HI_UNF_DISP_AttachIntf %#x",ret);
    }
    attachtodisp1 = HI_TRUE;
    cvbs_enable= HI_TRUE;
    TV_feedback_flag = HI_FALSE;

    ret = system("sync");
    if (HI_FAILURE == ret)
    {
        ALOGE("system sync ret %#x",ret);
    }
    if(InitComplete == HI_FALSE)
    {
        InitComplete = HI_TRUE;
    }

    ALOGI("\n ----------HDMI UnPlug done-----------\n");
    is_restart = HI_FALSE;
    return;
}

int formatAdaptEnable(int able)
{
    ALOGI("set format adapt enable %d",able);
    int ret = 0;
    char buffer[BUFLEN] = {0};
    sprintf(buffer,"%d", able);
    property_set("persist.sys.optimalfmt.enable", buffer);
    if(1 == able)
    {
        if (stHdmiAttr.bEnableHdmi == HI_TRUE)//for tv
        {
            ALOGI("setOptimal  tv: set format to optimum format %d \n",stSinkCap.enNativeFormat);
            set_format((display_format_e)stSinkCap.enNativeFormat);
        }
        else//for pc
        {
            ALOGE("setOptimal  pc: set format to optimum format \n");
            set_format(DISPLAY_FMT_VESA_1024X768_60);
        }
        //set_HDRType(HDRTYPE_AUTO);
        //property_set("persist.sys.hdrmode", "-1");
        baseparam_save();
    }
    else
    {
        ret = get_format(&able);
        ALOGI("ret %d set current format %d! for remenber user set",ret,able);
        set_format(able);
    }
    ret = system("sync");
    if (HI_FAILURE == ret)
    {
        ALOGE("system sync ret %#x",ret);
    }
    return 0;
}

int isFormatAdaptEnable()//0 is disable; 1 is enable
{
    char buffer[BUFLEN] = {0};
    int value = 0;

    char property_buffer[BUFLEN] = {0};
    property_get("persist.sys.qb.enable", property_buffer, "false");
    if(strcmp(property_buffer,"true")==0)
    {
        if(hdmiEDIDFlag == 1)
            value = 1;
        else
            value = 0;
    }
    else
    {
        property_get("persist.sys.optimalfmt.enable", buffer, "1");
        ALOGI("format adapt Enable buffer %s", buffer);
        value = strcmp(buffer,"1") == 0 ? 1 : atoi(buffer);
        ALOGI("format adapt Enable value %d", value);
        return value;
    }
    return value;
}

int isCapabilityChanged(int getCapResult,HI_UNF_EDID_BASE_INFO_S *pstSinkAttr)
{
    char  oldbuffer[BUFLEN] = {0,0,0};
    //icls init
    char  buffer[BUFLEN] ={0};
    int ret = 0;
    int index = 0;

    HI_UNF_PDM_HDMI_PARAM_S stHdmiParam;
    HI_S32 s32Ret = HI_SUCCESS;
    HI_U32 i=0;

    char property_buffer[BUFLEN]={0};
    property_get("persist.sys.qb.enable", property_buffer, "false");
    if(strcmp(property_buffer,"true")==0)
    {

        if(getCapResult == HI_SUCCESS)
        {
            stHdmiParam.pu8EDID = &(hdmiEDIDBuf);
            stHdmiParam.pu32EDIDLen = &hdmiEDIDBuflen;
            memset(hdmiEDIDBuf, 0x0, sizeof(hdmiEDIDBuf));
            hdmiEDIDBuflen = 512;
            s32Ret = HI_UNF_PDM_GetBaseParam(HI_UNF_PDM_BASEPARAM_HDMI, &stHdmiParam);
            ALOGE("HDMI EDID before update length:0x%x, %d, %d\n", s32Ret, *(stHdmiParam.pu32EDIDLen), hdmiEDIDBuflen);
            ALOGE("HDMI EDID before update length:0x%x, %s, %s\n", s32Ret, stHdmiParam.pu8EDID, hdmiEDIDBuf);
            capToString(buffer,*pstSinkAttr);
            int cmpRet = strcmp(hdmiEDIDBuf,buffer);
            if(cmpRet != 0)
            {
                stHdmiParam.pu8EDID = &(buffer);
                HI_U32 bufferLen = sizeof(buffer);
                stHdmiParam.pu32EDIDLen = &bufferLen;
                ALOGE("HDMI EDID after update length:%s, %d\n", stHdmiParam.pu8EDID, *(stHdmiParam.pu32EDIDLen));
                s32Ret = HI_UNF_PDM_UpdateBaseParam(HI_UNF_PDM_BASEPARAM_HDMI, &stHdmiParam);
                if(ret != HI_SUCCESS)
                {
                    ALOGE("UpdateBaseParam HDMI Err--- %p", (void*)ret);
                }
                ret = 1;
                hdmiEDIDFlag = 1;
            }
            else
                hdmiEDIDFlag = 0;
        }
        else
        {
            stHdmiParam.pu8EDID = &(hdmiEDIDBuf);
            stHdmiParam.pu32EDIDLen = &hdmiEDIDBuflen;
            memset(hdmiEDIDBuf, 0x0, sizeof(hdmiEDIDBuf));
            hdmiEDIDBuflen = 0;
            s32Ret = HI_UNF_PDM_UpdateBaseParam(HI_UNF_PDM_BASEPARAM_HDMI, &stHdmiParam);
            if(ret != HI_SUCCESS)
            {
                ALOGE("UpdateBaseParam HDMI Err--- %p", (void*)ret);
            }
            ret = 1;
            hdmiEDIDFlag = 1;
        }

    } else {

        if(getCapResult == HI_SUCCESS) {
            property_get("persist.sys.hdmi.cap", oldbuffer,"");
            capToString(buffer,*pstSinkAttr);

            int cmpRet = strcmp(oldbuffer,buffer);
            if(cmpRet != 0)
            {
                property_set("persist.sys.hdmi.cap", buffer);
                ret = 1;
            }
        } else {
            property_set("persist.sys.hdmi.cap", "0");
            ret = 1;
        }
    }
    return ret;

}

void capToString(char* buffer,HI_UNF_EDID_BASE_INFO_S cap)
{
    int index = 0;
    for(index = 0; index <= HI_UNF_ENC_FMT_VESA_2560X1600_60_RB; index++)
    {
        //icsl deadcode delete
        //if (index >= HI_UNF_ENC_FMT_BUTT)
        //{
        //    break;
        //}
        *(buffer+index) = '0'+(int)cap.bSupportFormat[index];
    }
    //HI_UNF_ENC_FMT_3840X2160_24 = 0x40,
    //HI_UNF_ENC_FMT_3840X2160_25,
    //HI_UNF_ENC_FMT_3840X2160_30,
    //HI_UNF_ENC_FMT_3840X2160_50,
    //HI_UNF_ENC_FMT_3840X2160_60,
    //HI_UNF_ENC_FMT_4096X2160_24,
    //HI_UNF_ENC_FMT_4096X2160_25,
    //HI_UNF_ENC_FMT_4096X2160_30,
    //HI_UNF_ENC_FMT_4096X2160_50,
    //HI_UNF_ENC_FMT_4096X2160_60,
    int j = 0;
    for(j = 0; j < (HI_UNF_ENC_FMT_4096X2160_60 - HI_UNF_ENC_FMT_3840X2160_24 + 1); j++)
    {
        //icsl deadcode delete
        //if ((HI_UNF_ENC_FMT_3840X2160_24 + j) >= HI_UNF_ENC_FMT_BUTT)
        //{
        //    break;
        //}
        *(buffer+index) = '0'+(int)cap.bSupportFormat[HI_UNF_ENC_FMT_3840X2160_24 + j];
        index++;
    }
    //HI_UNF_ENC_FMT_3840X2160_23_976,
    //HI_UNF_ENC_FMT_3840X2160_29_97,
    //HI_UNF_ENC_FMT_720P_59_94,
    //HI_UNF_ENC_FMT_1080P_59_94,
    //HI_UNF_ENC_FMT_1080P_29_97,
    //HI_UNF_ENC_FMT_1080P_23_976,
    //HI_UNF_ENC_FMT_1080i_59_94,
    int k = 0;
    for(k = 0; k < (HI_UNF_ENC_FMT_1080i_59_94 - HI_UNF_ENC_FMT_3840X2160_23_976 + 1); k++)
    {
        //icsl deadcode delete
        //if ((HI_UNF_ENC_FMT_3840X2160_23_976 + k) >= HI_UNF_ENC_FMT_BUTT)
        //{
        //    break;
        //}
        *(buffer+index) = '0'+(int)cap.bSupportFormat[HI_UNF_ENC_FMT_3840X2160_23_976 + k];
        index++;
    }
    *(buffer+index) = '\0';
}

HI_U32 set_HDMI_Suspend_Time(int iTime)
{
    ALOGE("iTime is %d",iTime);
    HI_U32 ret = -1;
    char  buffer[BUFLEN]={0} ;
    sprintf(buffer,"%d", iTime);
    ret = property_set("persist.hdmi.suspend.time", buffer);
    return ret;
}

HI_U32 get_HDMI_Suspend_Time()
{
    char buffer[BUFLEN]={0};
    HI_U32 value = -1;

    property_get("persist.hdmi.suspend.time", buffer, "0");
    value = atoi(buffer);
    ALOGE("get_hdmi_suspend_time: value is :%d",value);
    return value;
}

HI_S32 set_HDMI_Suspend_Enable(int iEnable)
{
    ALOGE("set_hdmi_suspend_enable: iEnable is :%d",iEnable);
    HI_S32 ret = -1;
    char tmp[PROP_VALUE_MAX];
    char  buffer[BUFLEN]={0};
    sprintf(buffer,"%d", iEnable);
    property_get("ro.ysten.province", tmp, "master");
    ALOGE("%s(%d)province is :%s",__func__,__LINE__,tmp);
    //add by ysten zengzhiliang at 20181109:add m301h_anhui
    if(!strcmp(tmp, "cm201_heilongjiang")||!strcmp(tmp, "cm201_anhui")||!strcmp(tmp, "m301h_anhui")||!strcmp(tmp, "cm201_shaanxi")||!strcmp(tmp, "cm201_anhui_iptv"))
    {
        ret = property_set("persist.sys.hdmi.suspend.enable", buffer);
    }
    else
    {
        ret = property_set("persist.hdmi.suspend.enable", buffer);
    }
    if(HI_SUCCESS != ret)ALOGE("propertyset error %d",ret);
    if(iEnable == HI_TRUE)
    {
        if(!check_hdmi_connect())
        {
            ALOGE("hdmi not connect , cann't enable hdmi susepnd!!");
            return HI_FAILURE;
        }

        g_stCallbackSleep.pfnHdmiEventCallback = HDMI_Suspend_Callback;
        g_stCallbackSleep.pPrivateData = &g_stHdmiArgs;
        ret = HI_UNF_HDMI_RegCallbackFunc(g_stHdmiArgs.enHdmi, &g_stCallbackSleep);
        ALOGI("regist HDMI_Suspend_Callback ret =%#x, for hdmi uplug susp ",ret);

    } else {
        ret = HI_UNF_HDMI_UnRegCallbackFunc(g_stHdmiArgs.enHdmi,&g_stCallbackSleep);
        ALOGI("unregist HDMI_Suspend_Callback ret =%#x, for hdmi uplug susp ",ret);

    }
    sprintf(buffer,"%d", iEnable);
    ret = property_set("persist.hdmi.suspend.enable", buffer);
    return ret;
}

HI_S32 get_HDMI_Suspend_Enable()
{
    char buffer[BUFLEN]={0};
    HI_U32 value = -1;

    char tmp[PROP_VALUE_MAX];
    property_get("ro.ysten.province", tmp, "master");
    ALOGE("%s(%d)province is :%s",__func__,__LINE__,tmp);
    //add by ysten zengzhiliang at 20181109:add m301h_anhui
    if(!strcmp(tmp, "cm201_anhui")||!strcmp(tmp, "m301h_anhui")||!strcmp(tmp, "cm201_shaanxi")||!strcmp(tmp, "cm201_anhui_iptv"))
    {
        property_get("persist.sys.hdmi.suspend.enable", buffer, "0");
    }
    else
    {
        property_get("persist.hdmi.suspend.enable", buffer, "0");
    }
    value = atoi(buffer);
    ALOGI("get_hdmi_suspend_enable: value is :%d",value);
    return value;
}
HI_U32 get_HDMI_CEC_Suspend_Enable()
{
    char buffer[BUFLEN]={0};
    HI_U32 value = -1;

    property_get("persist.sys.hdmi.cec", buffer, "false");
    ALOGI_IF(dbuglog, "get_HDMI_CEC_Suspend_Enable is :%s",buffer);
    if(strcmp("true",buffer)==0){
        value = 1;
    }
    else{
        value = 0;
    }
    return value;
}
int get_HDMI_CEC_Suspend_Status()
{
    char buffer[BUFLEN]={0};
    HI_U32 value = -1;

    property_get("persist.sys.cec.status", buffer, "false");
    ALOGI("get_persist_sys.cec.status is :%s",buffer);
    if(strcmp("true",buffer)==0){
        value = 1;
    }
    else{
        value = 0;
    }
    return value;
}

HI_U32 set_HDMI_CEC_Suspend_Enable(int iEnable)
{
    ALOGI("set_HDMI_CEC_Suspend_Enable: iEnable is :%d",iEnable);

    HI_U32 ret = -1;

    if(iEnable == HI_TRUE){
        ret = property_set("persist.sys.hdmi.cec", "true");
        HI_UNF_HDMI_RegCECCallBackFunc(g_stHdmiArgs.enHdmi, HDMI_CEC_Proc);
        HI_UNF_HDMI_CEC_Enable(g_stHdmiArgs.enHdmi);
    } else {
        ret = property_set("persist.sys.hdmi.cec", "false");
        HI_UNF_HDMI_UnRegCECCallBackFunc(g_stHdmiArgs.enHdmi, HDMI_CEC_Proc);
        HI_UNF_HDMI_CEC_Disable(g_stHdmiArgs.enHdmi);
    }

    return ret;
}


int HDMI_Suspend_ReportKeyEvent(int iKeyValue,int iStatus)
{
    int s_fdVinput = -1;
    int mousedata[4]={0,0,0,0};
    s_fdVinput = open(Vinput_FILE, O_RDONLY);
    HI_S32 ret = 0;
    if(s_fdVinput < 0)
    {
        ALOGE("can't open Vinput,%s\n", Vinput_FILE);
        return -1;
    }
    mousedata[0]=iKeyValue;
    mousedata[1]=iStatus;
    mousedata[2]=0;
    mousedata[3]=0;
    ret = ioctl(s_fdVinput, IOCTK_KBD_STATUS, (void *)mousedata);
    if(ret != HI_SUCCESS)
    {
        ALOGE("ioctl error ret %d",ret);
    }
    close(s_fdVinput);
    return 0;
}

void HDMI_Suspend_Timeout()
{
    ALOGI("HDMI_Suspend_Timeout: hdmi connect status flag hdmi_enable =%d",hdmi_enable);
    char  buffer[BUFLEN] = {0};

    HI_UNF_HDMI_STATUS_S  hdmiStatus;
    //icsl init
    hdmiStatus.bConnected = HI_FALSE;
    HI_U8 is_under_cec_suspend = get_HDMI_CEC_Suspend_Status();
    HI_UNF_HDMI_GetStatus(0,&hdmiStatus);
    ALOGI("hdmiStatus.bConnected =%d",hdmiStatus.bConnected);

    /*if(hdmi_enable == HI_TRUE// hdmi plug in flag :  1 mean hdmi replug in or hdmi rsen reconnect befor time out
        || is_under_cec_suspend == 1// if is under cec suspend
      )
    {
        ALOGI("hdmi_enable =%d is_under_cec_suspend %d  hdmiStatus.bConnected = %d",hdmi_enable,is_under_cec_suspend,hdmiStatus.bConnected);
    }
    else*/
    {//hdmi uplug  or hdmi rsen disconnect event suspend time out, send a power key to system
        #ifdef CHIP_TYPE_Hi3798MV300_YSTEN_JS
        ALOGW("CHIP_TYPE_Hi3798MV300_YSTEN_JS");
        HDMI_Suspend_ReportKeyEvent(KEY_POWER, KPC_EV_KEY_PRESS);
        HDMI_Suspend_ReportKeyEvent(KEY_POWER, KPC_EV_KEY_RELEASE);
        #else
		property_get("ro.ysten.province", buffer, "master");
        //start by lizheng 20190326 to solve 32a suspend
        if(strcmp(buffer,"cm201_fujian")==0 ||
           strcmp(buffer,"m302h_fujian")==0 
            || strcmp(buffer,"HDC-32A_fujian")==0)
		{
		ALOGW("fujian hdmi suspend");
        //end by lizheng 20190326 to solve 32a suspend
        HDMI_Suspend_ReportKeyEvent(KEY_POWER, KPC_EV_KEY_PRESS);
        HDMI_Suspend_ReportKeyEvent(KEY_POWER, KPC_EV_KEY_RELEASE);
        }else{
        //add change by guangchao.su for hdmi stand by
        //HDMI_Suspend_ReportKeyEvent(KEY_POWER, KPC_EV_KEY_PRESS);
        //HDMI_Suspend_ReportKeyEvent(KEY_POWER, KPC_EV_KEY_RELEASE);
        //add by xue
        //system("am broadcast -a android.ysten.systemupdate -e powercontrol poweroff &");
        ALOGW("send power off broadcast");
		system("am broadcast -a android.ysten.systemupdate -e hdmipowercontrol hdmipoweroff &");
        //add by xue
		}
        #endif
        ALOGW("\033[31mHDMI_Suspend_Timeout: send power key to suspend \33[0m\n");
        property_get("ro.product.target", buffer, "0");
        if(strcmp(buffer,"shcmcc")==0)
        {
            sleep(4);//unit : second
            HDMI_Suspend_ReportKeyEvent(KEY_RIGHT, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_RIGHT, KPC_EV_KEY_RELEASE);
            sleep(2);
            HDMI_Suspend_ReportKeyEvent(KEY_ENTER, KPC_EV_KEY_PRESS);
            HDMI_Suspend_ReportKeyEvent(KEY_ENTER, KPC_EV_KEY_RELEASE);
        }
        else
        {
            ALOGI("only power key");
        }
    }
}

static void *timeoutfun(void *args)
{
    HI_S32 ret = -1;
    hdmi_event_t *ev = (hdmi_event_t*)args;

    HI_U32 userTime = get_HDMI_Suspend_Time();

    if(0 == userTime)
    {
        ALOGE("time for count down can not be zero !");
        pthread_detach(pthread_self());
        return NULL;
    }

    int eventType = (int)ev->event_type;
    ALOGI("hdmi susend signal %d %s",eventType,eventType == hdmi_unplug ? ":unplug" :" rsens_dis");
    time_t time_in,time_out ;
    struct tm *tm_now ;
    HI_UNF_HDMI_STATUS_S  hdmiStatus;

    time(&time_in);
    tm_now = localtime(&time_in);
    if(tm_now == NULL)
    {
        ALOGE("get localtime error !");
        pthread_detach(pthread_self());
        return NULL;
    }
    ALOGI("\033[32m Current time is : %d-%d-%d %d:%d:%d \33[0m",
    tm_now->tm_year+1900, tm_now->tm_mon+1, tm_now->tm_mday, tm_now->tm_hour, tm_now->tm_min, tm_now->tm_sec) ;
    tm_now->tm_min = tm_now->tm_min +userTime;

    time_out = mktime(tm_now);
    if(time_out < 0)
    {
        ALOGE("mktime error %ld ",time_out);
    }

    ALOGI("\033[32m Timeout time is : %d-%d-%d %d:%d:%d \33[0m",
    tm_now->tm_year+1900, tm_now->tm_mon+1, tm_now->tm_mday, tm_now->tm_hour, tm_now->tm_min, tm_now->tm_sec) ;
    int scap = 0;
    char province[BUFLEN] = {0};
    property_get("ro.ysten.province", province, "master");
    while(1)
    {
        sleep(4);
        ret = HI_UNF_HDMI_GetStatus(HI_UNF_HDMI_ID_0,&hdmiStatus);
        if(ret != HI_SUCCESS)
        {
            ALOGE("HI_UNF_HDMI_GetStatus Fail ret = %#x",ret);
        }
        ALOGI_IF(dbuglog,"hdmiStatus.bConnected    = %d",hdmiStatus.bConnected);
        ALOGI_IF(dbuglog,"hdmiStatus.bSinkPowerOn  = %d",hdmiStatus.bSinkPowerOn);

        time_in = time(NULL);
        scap = time_out -time_in;

        /*begin:add by zhanghk at 20200323:standby when TV power off*/
        if(strstr(province, "anhui") != NULL){
            tm_now = localtime(&time_in);
            if(scap > 0){
                if(HI_TRUE == hdmiStatus.bSinkPowerOn){
                    //power on after power off
                    break;
                }else{
                    continue;
                }
            }else{
                HDMI_Suspend_Timeout();
                break;
            }
        }
        /*end:add by zhanghk at 20200323:standby when TV power off*/
        if(hdmi_unplug == eventType && HI_TRUE == hdmiStatus.bConnected )
        {
            ALOGI("\033[32m hotplug replug in at %d second before timeout, cancel !\33[0m",scap);
            break;
        }
        else if(hdmi_rsen_disconnect == eventType && HI_TRUE == hdmiStatus.bSinkPowerOn)
        {
            ALOGI("\033[32m resens reconnect at %d second before timeout, cancel !\33[0m",scap);
            break;
        }
        else if(scap <=0)
        {//gap time less the one day !
            tm_now = localtime(&time_in);
            ALOGI("time out is  :%lu ",time_out);
            ALOGI("time now is  :%lu ",time_in);
            ALOGI("\033[32m now time is : %d-%d-%d %d:%d:%d \33[0m",
            tm_now->tm_year+1900, tm_now->tm_mon+1, tm_now->tm_mday, tm_now->tm_hour, tm_now->tm_min, tm_now->tm_sec);
            ALOGI("\033[32m time out %d  ,excute and break  phd %d rens %d!\33[0m",scap,hdmiStatus.bConnected,hdmiStatus.bSinkPowerOn);
            if(scap > (-DAY_SECOND))
            {
                ALOGI("\033[32m time out %d  gap time less one day ok !\33[0m",scap);
                HDMI_Suspend_Timeout();
            }
            else
            {
                ALOGI("\033[31m time out %d  gap time more one day error !\33[0m",scap);
            }
            break;
        }
        else
            ALOGI_IF(dbuglog,"\033[32m Current to --> Endtime stil have :%d seconds. \33[0m",scap);
    }

    alrm_cancel_flag = HI_TRUE;
    alrm_set_flag = HI_FALSE;
    pthread_detach(pthread_self());

    return NULL;
}

hdmi_event_t ev;
HI_VOID HDMI_Suspend_Callback(HI_UNF_HDMI_EVENT_TYPE_E event, HI_VOID *pPrivateData)
{

    //1.1 if hdmi suspend is disable, do nothing
    if(0 == get_HDMI_Suspend_Enable())
    {
        ALOGI_IF(dbuglog,"\n hdmi_callback > hdmi uplug suspend switch not enable %#x",event);
        return;
    }
    if(HI_FALSE == InitComplete)
    {
        ALOGE("hdmi not init complete, not response hdmi uplug suspend,even enable!");
        return;
    }
    HI_S32 ret = HI_FALSE;

    switch ( event )
    {
        case HI_UNF_HDMI_EVENT_HOTPLUG://0x10
        case HI_UNF_HDMI_EVENT_RSEN_CONNECT://0x15
            if(alrm_set_flag == HI_TRUE && alrm_cancel_flag == HI_FALSE)
            {
                alrm_cancel_flag = HI_TRUE;
                alrm_set_flag = HI_FALSE;
            }
            break;

        case HI_UNF_HDMI_EVENT_NO_PLUG://0x11
        case HI_UNF_HDMI_EVENT_RSEN_DISCONNECT://0x16

            if(alrm_cancel_flag == HI_TRUE && alrm_set_flag == HI_FALSE)
            {
                if(event == HI_UNF_HDMI_EVENT_NO_PLUG)
                    ev.event_type = hdmi_unplug;
                else
                    ev.event_type = hdmi_rsen_disconnect;
                pthread_t timeout_thread;
                int ret;
                ret= pthread_create(&timeout_thread, NULL,timeoutfun,(HI_VOID *)&ev);//new thread
                if(ret != 0)
                    ALOGE("hdmi suspend timeout thread create fail");
                else
                    ALOGI("hdmi suspend timeout threadcreate success");
                alrm_set_flag = HI_TRUE;
                alrm_cancel_flag = HI_FALSE;
            }
            break;

        default:
            break;
    }
}

HI_VOID HDMI_HdcpFail_Proc(HI_VOID *pPrivateData)
{

    HI_S32 ret= HI_FALSE;
    HI_UNF_HDCP_STATUS_S stHdcpstatus;

    ALOGI("\n --- Get HDMI event: HDCP_FAIL. --- \n");

    ret =  HI_UNF_HDMI_GetHdcpStatus(HI_UNF_HDMI_ID_0,&stHdcpstatus);
    if(HI_SUCCESS != ret)
    {
        ALOGE("HI_UNF_HDMI_GetHdcpStatus fail ret =%#x set vo alpha to 0!",ret);
        ret = HI_UNF_DISP_SetVideoAlpha(HI_UNF_DISPLAY1,0);
        if(HI_SUCCESS != ret)
        {
            ALOGE("HI_UNF_DISP_SetVideoAlpha fail ret =%#x !",ret);
        }
    }

    if(//stHdcpstatus.enHdcpErrCode == HI_UNF_HDMI_HDCP_ERR_UNHPD
    //||stHdcpstatus.enHdcpErrCode == HI_UNF_HDMI_HDCP_ERR_NO_TIMMING
    //||stHdcpstatus.enHdcpErrCode == HI_UNF_HDMI_HDCP_ERR_UNDO
      stHdcpstatus.enHdcpErrCode == HI_UNF_HDMI_HDCP_ERR_LOADKEY_FAIL)
    {
        ALOGW("hdcp error %#x set vo alhpa to 0",stHdcpstatus.enHdcpErrCode);
        videoalhpa = 0;
    }
    else if(stHdcpstatus.enHdcpErrCode >= HI_UNF_HDMI_HDCP_ERR_ON_REVOCATION_LIST
        && stHdcpstatus.enHdcpErrCode <= HI_UNF_HDMI_HDCP_ERR_REPEATER_FAIL
        && HDCPFailCount > 5)
    {
        ALOGW("hdcp error %#x set vo alhpa to 0,fail times:%d",stHdcpstatus.enHdcpErrCode,HDCPFailCount);
        videoalhpa = 0;
        if(HI_SUCCESS != ret)
        {
            ALOGE("HI_UNF_DISP_GetVideoAlpha fail ret =%#x !",ret);
        }
    }
    else
    {
        ALOGE("other hdcp status error case : %#x ,not handle, vo alpha keep current state:%d!",stHdcpstatus.enHdcpErrCode,videoalhpa);
    }

    HDCPFailCount++;
    ALOGI("video output alpha set to :%d",videoalhpa);
    ret = HI_UNF_DISP_SetVideoAlpha(HI_UNF_DISPLAY1,videoalhpa);
    if(HI_SUCCESS != ret)
    {
        ALOGE("HI_UNF_DISP_SetVideoAlpha fail ret =%#x !",ret);
    }

    return;
}

HI_VOID HDMI_HdcpSuccess_Proc(HI_VOID *pPrivateData)
{
    HI_S32 ret= HI_FALSE;
    HDCPFailCount = 0;

    ret = HI_UNF_DISP_GetVideoAlpha(HI_UNF_DISPLAY1,&videoalhpa);
    if(HI_SUCCESS != ret)
    {
        ALOGE("HI_UNF_DISP_GetVideoAlpha fail ret =%#x !",ret);
    }
    ALOGI("\n --- Get HDMI event: HDCP_SUCCESS. --- \ncurrent alpha :%d to 100",videoalhpa);
    if(videoalhpa != 100)
    {
        videoalhpa = 100;
        ret = HI_UNF_DISP_SetVideoAlpha(HI_UNF_DISPLAY1,videoalhpa);
        if(HI_SUCCESS != ret)
        {
            ALOGE("HI_UNF_DISP_SetVideoAlpha fail ret =%#x !",ret);
        }
    }

    return;
}

HI_VOID HDMI_Event_Proc(HI_UNF_HDMI_EVENT_TYPE_E event, HI_VOID *pPrivateData)
{
    hdmi_event_t ev;
    ev.event_type = (int)hdmi_hotplug;
    ev.event_msg  = (int)HI_UNF_HDMI_ID_0;
    char start_type[BUFLEN] = {0};
    switch ( event )
    {
        case HI_UNF_HDMI_EVENT_HOTPLUG:
            hdmi_enable = HI_TRUE;
            HDMI_HotPlug_Proc(pPrivateData);
            clr_match_type = NORMOR_MATCH;
            if(is_restart == HI_TRUE)
            {
                is_restart =HI_FALSE;
                property_set("persist.sys.startwithhdmi","yes");
            }
            ev.event_type   = (int)hdmi_hotplug;
            break;
        case HI_UNF_HDMI_EVENT_NO_PLUG:
            ev.event_type   = (int)hdmi_unplug;
            notify_callback(&ev);
            clr_match_type = NORMOR_MATCH;
            hdmi_enable = HI_FALSE;
            HDCPFailCount = 0;
            if(TvPowerStatus != TV_STANDBY)
            {
                ALOGI("not under cec suspend.handle unplug %#x",event);
                HDMI_UnPlug_Proc(pPrivateData);
            }
            else
                ALOGI("under cec suspend not hanle unplug %#x",event);
            if(is_restart == HI_TRUE)
            {
                is_restart =HI_FALSE;
                property_set("persist.sys.startwithhdmi","no");
            }
            return;
        case HI_UNF_HDMI_EVENT_EDID_FAIL:
            ev.event_type   = (int)hdmi_edid_fail;
            break;
        case HI_UNF_HDMI_EVENT_HDCP_FAIL:
            HDMI_HdcpFail_Proc(pPrivateData);
            ev.event_type   = (int)hdmi_hdcp_fail;
            break;
        case HI_UNF_HDMI_EVENT_HDCP_SUCCESS:
            HDMI_HdcpSuccess_Proc(pPrivateData);
            ev.event_type   = (int)hdmi_hdcp_success;
            break;
        case HI_UNF_HDMI_EVENT_RSEN_CONNECT:
            ALOGI_IF(dbuglog, "HI_UNF_HDMI_EVENT_RSEN_CONNECT**********\n");
            ev.event_type   = (int)hdmi_rsen_connect;
            break;
        case HI_UNF_HDMI_EVENT_RSEN_DISCONNECT:
            ALOGI_IF(dbuglog, "HI_UNF_HDMI_EVENT_RSEN_DISCONNECT**********\n");
            ev.event_type   = (int)hdmi_rsen_disconnect;
            break;
        default:
            break;
    }
    ev.event_msg = (int)HI_UNF_HDMI_ID_0;
    notify_callback(&ev);
    return;
}


/**
 * Function: getFlashDataByName
 * Brif    : get 'deviceinfo' data from ----> <Flash Bottom>
 * Param   : mtdname ---- flash name e.g "deviceinfo"
 *           offset  ---- offset in flash from bottom
 *           pBuffer ---- Data out Buffer
 *           offlen  ---- read size
 */
HI_S32 getFlashDataByName(const char *mtdname, unsigned long offset, unsigned long offlen, char *pBuffer)
{
    HI_S32 u32Ret;
    char *pbuf = NULL;
    unsigned int uBlkSize = 0;
    unsigned int totalsize = 0;
    unsigned int PartitionSize = 0;
    HI_Flash_InterInfo_S info;

    HI_HANDLE handle = HI_Flash_OpenByName((HI_CHAR *)mtdname);
    if (HI_INVALID_HANDLE == handle)
    {
        ALOGE("HI_Flash_OpenByName failed, mtdname[%s]\n", mtdname);
        return HI_FAILURE;
    }

    u32Ret = HI_Flash_GetInfo(handle, &info);
    if(u32Ret != HI_SUCCESS)//check return value
    {
        ALOGE("HI_Flash_GetInfo failed, ret: %#x",u32Ret);
    }
    PartitionSize = (unsigned int)info.PartSize;
    uBlkSize = info.BlockSize;
    if (uBlkSize == 0)
    {
        uBlkSize = 512;
    }
    //totalsize = ((offlen - 1) / uBlkSize + 1) * uBlkSize; // Block alignment
    if(offset < uBlkSize) ALOGW("WARNING OFFSET VALUE SMALL THAN BLOCK SIZE !");
    totalsize = ((offset - 1) / uBlkSize + 1) * uBlkSize; // Block alignment
    if (totalsize > PartitionSize)
    {
        ALOGE("flash size read overbrim ,error");
        HI_Flash_Close(handle);
        return HI_FAILURE;
    }

    ALOGI("getFlashDataByName >>> \ntotalsize=%u,pagesize=%u,offset=%lu,offlen=%lu", totalsize, uBlkSize, offset, offlen);
    //hdcp enable data:totalsize=139264,pagesize=8192,offset=132096,offlen=11
    if(totalsize > 0)
    {
        pbuf = (char *)malloc(totalsize);
    }
    else{
        ALOGE("malloc size must > 0 %d", totalsize);
        HI_Flash_Close(handle);
        return HI_FAILURE;
    }
    if(NULL == pbuf)
    {
        ALOGE("malloc error, totalsize=[%d]", totalsize);
        HI_Flash_Close(handle);
        return HI_FAILURE;
    }
    memset(pbuf, 0, totalsize);

    // Read data from bottom of deviceinfo, bottom 128K data is DRM Key
    // We get HDMI1.4 HDCP Key 4K data over 128K from bottom of deviceinfo
    u32Ret = HI_Flash_Read(handle, PartitionSize - totalsize, (HI_U8 *)pbuf, totalsize, HI_FLASH_RW_FLAG_RAW);
    if (HI_FAILURE == u32Ret)
    {
        ALOGE("HI_Flash_Read Failed:%d\n", u32Ret);
        HI_Flash_Close(handle);
        free(pbuf);
        pbuf = NULL;
        if(pbuf != NULL)
            ALOGD("free pbuf %#x",pbuf);
        return HI_FAILURE;
    }
    HI_Flash_Close(handle);
    // hdcpkey enable data:  pbuf + (8 - 129%8) = pbuf +(8-1) =pbuf +7
    // hdcpkey :             pbuf + (8 - 132%8) = pbuf+(8-4) = pbuf +4
    //<--------------------------------------deviceinfo---2M ---------------------------->|
    //--------------------------------------------------------------------------|--uBlock-|
    //--------------------------------------------------------------------------|----8k---|
    //----------------------------|<----17*ublock --------|<----16*ublock---------------- |
    //----------------------------136k--------------------128k----------------------------|2M: bottom
    //----------------------------|--|--|--|--|--|--|--|--|---------128k-drm--------------|
    //---------------------------pbuf------------|<--d1 ->|
    //-HI_Flash_Read---->>pbuf--->|<---d2------->|
    //-------------------------------------------|<-----------offset--------------- ------|
    //d1 = offset % uBlkSize
    //d2 = uBlkSize -d1 = uBlkSize - offset % uBlkSize ,so
    //pbuf + d2 is right offset begin place we get the data ,it's: pbuf + (uBlkSize - offset % uBlkSize)
    memcpy(pBuffer, pbuf + (uBlkSize - offset % uBlkSize), offlen);
    memset(pbuf, 0, totalsize);
    free(pbuf);
    pbuf = NULL;
        if(pbuf != NULL)
            ALOGD("free pbuf %#x",pbuf);
    return HI_SUCCESS;
}

/*
 * read the hdcp EncryptedKey from deviceinfo zone ,
 * and load it to cipher >HI_UNF_HDMI_LoadHDCPKey
 * HDMI1.4 HDCP Key
 * Localtion: deviceInfo
 * OffSet:default 132K from bottom
 * Key Real size :  332B
*/

HI_S32 HIADP_HDMI_SetHDCPKey_DeviceInfo(HI_UNF_HDMI_ID_E enHDMIId)
{
    HI_U32 u32Ret;

    HI_UNF_HDMI_LOAD_KEY_S stLoadKey;
    const char *device_name = HDCP_KEY_LOCATION;
    unsigned long offset;
    unsigned long data_len = HDCP_KEY_LEN;
    char *pBuf = NULL;
    char offset_buf[PROP_VALUE_MAX]={0};

    /* Attention: This offset property is from 'deviceinfo' bottom offset
       if User config this, it must make sure this value more than 128K + 4K
       Because the bottom 128K is DRM KEY, the 4K use to store HDMI HDCP KEY */
    property_get("persist.sys.hdmi.hdcp.offset", offset_buf, "0");
    offset = atoi(offset_buf);
    if (offset == 0)
    {
        ALOGI("Read HDCPKey data from DeviceInfo partition, use default offset > %ld",offset);
        offset = HDCP_KEY_OFFSET_DEFAULT + DRM_KEY_OFFSET; // default 4k+128k from bottom
    }
    else if (offset < (HDCP_KEY_OFFSET_DEFAULT + DRM_KEY_OFFSET))
    {
        ALOGE("HIADP_HDMI_SetHDCPKey_DeviceInfo, use customer property hdmi hdcp offset, But Length is too short!!");
        return HI_FAILURE;
    }

    pBuf = (char *)malloc(data_len);
    if (pBuf == NULL)//ICSL pBuf != 0
    {
        ALOGE("HIADP_HDMI_SetHDCPKey_DeviceInfo, malloc failed");
        return HI_FAILURE;
    }
    memset(pBuf, 0, data_len);

    ALOGI("device_name = %s", device_name);
    ALOGI("offset = %ld", offset);
    ALOGI("data_len = %ld", data_len);

    u32Ret = getFlashDataByName(device_name, offset, data_len, pBuf);

    if (HI_SUCCESS == u32Ret)
    {
        ALOGE("getFlashDataByName, Success!");
        stLoadKey.u32KeyLength = data_len;
        stLoadKey.pu8InputEncryptedKey  = (char *)malloc(data_len);
        if(HI_NULL == stLoadKey.pu8InputEncryptedKey)
        {
            ALOGE("malloc stLoadKey.pu8InputEncryptedKey erro!\n");
            free(pBuf);
            pBuf = NULL;
            if(pBuf != NULL)
                ALOGI("pBuf %#x",pBuf);
            return HI_FAILURE;
        }
        stLoadKey.pu8InputEncryptedKey = pBuf;
        u32Ret = HI_UNF_HDMI_LoadHDCPKey(enHDMIId,&stLoadKey);
        if (HI_SUCCESS == u32Ret)
        {
            ALOGE("HI_UNF_HDMI_LoadHDCPKey  Success!\n");
        }
        else
        {
            ALOGE("HI_UNF_HDMI_LoadHDCPKey  Failed %#x!\n",u32Ret);
            free(pBuf);
            pBuf = NULL;
            if(pBuf != NULL)
                ALOGD("free pbuf %#x",pBuf);
            return HI_FAILURE;

        }
    }
    else
    {
        ALOGE("getFlashDataByName  Failed !\n");
    }
    free(pBuf);
    pBuf = NULL;
    if(pBuf != NULL)
        ALOGD("free pbuf %#x",pBuf);
    return u32Ret;
}
// read deviceinfo hdcp enable flag to determine whether hdcp enable or not
// return 1-->enable, 0 -->disable ,-1 -->error
int isHdmiHDCPenable()
{
    int ret = -1;
    const char *device_name = HDCP_KEY_LOCATION;
    unsigned long offset;
    char hdcp_enable_data[] = {'H','D','C','P','E','N','A','B','L','E','1'};
    char hdcp_disable_data[] ={'H','D','C','P','D','I','S','A','B','L','E'};
    unsigned int data_len = sizeof(hdcp_enable_data);
    char hdcp_data[126] ={0};
    char *pBuf = NULL;
    char offset_buf[PROP_VALUE_MAX]={0};
    long offset_prop = 0;

    property_get("persist.sys.hdmi.hdcp.offset",offset_buf, "0");
    offset_prop = atoi(offset_buf);
    if (offset_prop == 0)
    {
        offset = HDCP_KEY_OFFSET_DEFAULT + DRM_KEY_OFFSET - HDCP_ENABLE_OFFSET_TO_KEY;//129k =129x1024 = 132096 byte
    }
    else if (offset_prop < (HDCP_KEY_OFFSET_DEFAULT + DRM_KEY_OFFSET) || offset_prop > (DEVICEINFO_MAX - HDCP_KEY_OFFSET_DEFAULT))
    {
        offset = HDCP_KEY_OFFSET_DEFAULT + DRM_KEY_OFFSET - HDCP_ENABLE_OFFSET_TO_KEY;
        ALOGE("getHdmiHDCPEnable: use  customer property hdmi hdcp offset,but the offset is invalide!");
    }
    else
    {
        offset = offset_prop - HDCP_ENABLE_OFFSET_TO_KEY;
    }

    pBuf = (char *)malloc(data_len);
    if (pBuf == NULL)
    {
        ALOGE("HIADP_HDMI_SetHDCPKey_DeviceInfo, malloc failed");
        return -1;
    }
    memset(pBuf, 0, data_len);
    ALOGI("isHdmiHDCPenable >>> \ndevice_name = %s offset =%ld  data_len = %u", device_name,offset,data_len);
    //here,getFlashDataByName :get the hdcp enable data from deviceinfo set by user.
    if(getFlashDataByName(device_name, offset, data_len, pBuf) != HI_SUCCESS)
    {
       ALOGE("get flash enable data failure!");
       goto EXIT;
    }

    memcpy(hdcp_data,pBuf,data_len);
    ALOGE("hdcp enable flag from device: %s",hdcp_data);
    if(0 == memcmp(pBuf, hdcp_enable_data,data_len))//compare enable data indeviceinfo with hdcp_enable_data[]
    {
        ALOGE("in deviceinfo partition,HDMI HDCP data flag is enable!\n");
        ret = HI_TRUE;
    }
    else if (0 == memcmp(pBuf, hdcp_disable_data,data_len))
    {
        ALOGE("in deviceinfo partition,HDMI HDCP data flag is disabled!\n");
        ret = HI_FALSE;
    }
    else
    {
        ALOGE("in deviceinfo partition,HDMI HDCP data flag is invalidate!\n");
        ret = HI_FAILURE;
    }

EXIT:
    free(pBuf);
    pBuf = NULL;
    if(pBuf != NULL)
        ALOGI("pBuf %#x",pBuf);
    return ret;
}

int loadHDCPKey()
{   //here when user invoke this interface from application ,will read the hdcpkey data
    //from deviceinfo & load it to cipher
    if(HIADP_HDMI_SetHDCPKey_DeviceInfo(HI_UNF_HDMI_ID_0) == HI_SUCCESS)
    {
        ALOGI("loadHDCPKey from deviceinfo SUCCESS!\n");
        Hdcp14KEYLoadFlag = HI_TRUE;
        return HI_SUCCESS;
    }
    ALOGE("loadHDCPKey from deviceinfo failed!\n");
    Hdcp14KEYLoadFlag = HI_FALSE;
    return HI_FAILURE;
}

HI_S32 HIADP_HDMI_Init(HI_UNF_HDMI_ID_E enHDMIId, HI_UNF_ENC_FMT_E enWantFmt)
{
    ALOGI_IF(DEBUG_HDMI_INIT, "HDMI init enter %s(%d)" , __func__, __LINE__);
    HI_S32 Ret = HI_FAILURE;
    HI_UNF_HDMI_OPEN_PARA_S stOpenParam;
    display_format_e            format = DISPLAY_FMT_1080i_60;
    HI_UNF_HDMI_DELAY_S  stDelay;
    HI_UNF_HDMI_CEC_STATUS_S cecStatus;
    char  buffer[BUFLEN] ={0};
    char tmp[PROP_VALUE_MAX];

    g_stHdmiArgs.enHdmi       = enHDMIId;

    property_set("persist.sys.cec.status", "true");
    Ret = HI_UNF_HDMI_Init();
    if (HI_SUCCESS != Ret)
    {
        ALOGE("HI_UNF_HDMI_Init failed:%#x\n",Ret);
        return HI_FAILURE;
    }

    //load onetimes is enoght .no need to load in every hpd.
    Ret = loadHDCPKey();
    if(HI_SUCCESS != Ret)
    {
        ALOGE("load hdcp14 key failed: %d ",Ret);
    }

    property_get("persist.hdmi.fmtdelaytime",buffer,"500");
    HI_UNF_HDMI_GetDelay(0,&stDelay);
    stDelay.bForceFmtDelay = HI_TRUE;
    stDelay.bForceMuteDelay = HI_TRUE;
    stDelay.u32FmtDelay = atoi(buffer);;
    stDelay.u32MuteDelay = 120;
    HI_UNF_HDMI_SetDelay(0,&stDelay);
    ALOGE("hdmi FmtDelay %d ",stDelay.u32FmtDelay);

    ALOGI_IF(DEBUG_HDMI_INIT, "Reg HDMI Event callback >HDMI_Event_Proc< for disp adp %d", __LINE__);
    g_stCallbackFunc.pfnHdmiEventCallback = HDMI_Event_Proc;
    g_stCallbackFunc.pPrivateData = &g_stHdmiArgs;
    Ret = HI_UNF_HDMI_RegCallbackFunc(enHDMIId, &g_stCallbackFunc);
    if (Ret != HI_SUCCESS)
    {
        ALOGE("hdmi reg failed:%#x\n",Ret);
        HI_UNF_HDMI_DeInit();
        return HI_FAILURE;
    }

    
    property_get("ro.ysten.province", tmp, "master");
    //add by ysten zengzhiliang at 20180920:fix HDMI can not suspend bug
    if(!strcmp(tmp, "cm201_heilongjiang")||!strcmp(tmp, "cm201_hebei")
    //add by ysten zengzhiliang at 20181109:add m301h_anhui
        ||!strcmp(tmp, "cm201_zhejiang")||!strcmp(tmp, "cm201_anhui")||!strcmp(tmp, "m301h_anhui")||!strcmp(tmp, "cm201_beijing")||!strcmp(tmp, "cm201_shaanxi")||!strcmp(tmp, "cm201_anhui_iptv"))
    {
        ALOGE("#### Set  Suspend_Enable  Init add by zengzhiliang");
        Ret = set_HDMI_Suspend_Enable(get_HDMI_Suspend_Enable());
        if(HI_SUCCESS != Ret) {
            ALOGW("hdmi suspend enable error %d",Ret);
        }
    }

    stOpenParam.enDefaultMode = g_enDefaultMode;//HI_UNF_HDMI_FORCE_NULL;
    ALOGI_IF(DEBUG_HDMI_INIT, "HDMI open  %s(%d)" , __func__, __LINE__);
    Ret = HI_UNF_HDMI_Open(enHDMIId, &stOpenParam);
    if (Ret != HI_SUCCESS)
    {
        ALOGE("HI_UNF_HDMI_Open failed:%#x\n",Ret);
        HI_UNF_HDMI_DeInit();
        return HI_FAILURE;
    }
    stHdmiStatus.bConnected = HI_FALSE;
    Ret = HI_UNF_HDMI_GetStatus(enHDMIId,&stHdmiStatus);
    if (Ret != HI_SUCCESS)
    {
        ALOGE("HI_UNF_HDMI_Open failed:%#x\n",Ret);
    }

    ALOGI("HI_UNF_HDMI_GetStatus, stHdmiStatus.bConnected = %d\n",stHdmiStatus.bConnected);
    // for cvbs,if source is cvbs,bConnected will be false.
    if (HI_FALSE == stHdmiStatus.bConnected)
    {
        ALOGE("HDMI no connected \n");
        displayType = DEVICE_HDMI_NO_CONNECT;
        stSinkCap.enNativeFormat = HI_UNF_ENC_FMT_720P_60;

        //when get capability fail,use default mode
        if(g_enDefaultMode == HI_UNF_HDMI_DEFAULT_ACTION_HDMI) {
            stHdmiAttr.bEnableHdmi = HI_TRUE;
            displayType = DEVICE_HDMI_TV;
        } else {
            stHdmiAttr.bEnableHdmi = HI_FALSE;
            displayType = DEVICE_HDMI_PC_MONITOR;
        }

        int fmtRet = HI_SUCCESS;
        fmtRet = get_format(&format);
        ALOGE("\nget_format, ret = %d, format = %d \n", fmtRet, (int)format);
        if(fmtRet != HI_SUCCESS) {
            ALOGE("get format err and set nativeformat");
            set_format((display_format_e)stSinkCap.enNativeFormat);
            baseparam_save();
        }
        hdmi_enable = HI_FALSE;
        cvbs_enable = HI_TRUE;
    }

    Ret= HI_UNF_HDMI_CECStatus(enHDMIId,&cecStatus);
    ALOGI("HDMI_HotPlug_Proc>..HI_UNF_HDMI_CECStatus ret=%d cecStatus.bEnable = %d",Ret,cecStatus.bEnable);

    property_get("persist.sys.hdmi.cec", buffer,"true");
    ALOGI("enable cec default %s",buffer);
    int value = strcmp("true",buffer)==0 ? 1: 0;
    set_HDMI_CEC_Suspend_Enable(value);

    if(!(cecStatus.bEnable == HI_TRUE) && get_HDMI_CEC_Suspend_Enable()== HI_TRUE)// if cec not enable
    {
        Ret = HI_UNF_HDMI_RegCECCallBackFunc(enHDMIId, HDMI_CEC_Proc);
        ALOGI("Hdmi init > cecstatus.enable!=true reg cec callback ret = %d",Ret);
        if (Ret != HI_SUCCESS)
        {
            ALOGE("----->HI_UNF_HDMI_RegCECCallBackFunc fail ret =%d",Ret);
        }
        Ret= HI_UNF_HDMI_CEC_Enable(enHDMIId);
        ALOGI("Hdmi init > HI_UNF_HDMI_CEC_Enable ret = %d",Ret);

    }
    else
    {
        ALOGI("Hdmi init >cec has been enable");
    }

    ALOGI_IF(DEBUG_HDMI_INIT, "HDMI init exit %s(%d)" , __func__, __LINE__);
    //if switch is enable and hdmi is connect , we send send cec command
    //otherwise , sending command is useless, e.g. when only have cvbs case.
    if(get_HDMI_CEC_Suspend_Enable() == HI_TRUE
        && get_HDMI_CEC_Suspend_Status() == HI_TRUE
       )
    {
        //for not block displaysetting init complete
        pthread_t sendtrd;
        Ret= pthread_create(&sendtrd, NULL,HDMI_CEC_Resume,NULL);//new thread
        if(Ret != 0)
            ALOGE("resume command sending thread create fail");
        else
            ALOGI("resume command sending thread create success");
    }

    return HI_SUCCESS;
}

HI_S32 HIADP_HDMI_DeInit(HI_UNF_HDMI_ID_E enHDMIId)
{
    HI_S32 ret=0;
    ret = HI_UNF_HDMI_Stop(enHDMIId);
    if(ret != HI_SUCCESS)
    {
        ALOGE("HI_UNF_HDMI_Stop Err--- %p", (void*)ret);
    }

    ret = HI_UNF_HDMI_Close(enHDMIId);
    if(ret != HI_SUCCESS)
    {
        ALOGE("HI_UNF_HDMI_Close Err--- %p", (void*)ret);
    }

    ret = HI_UNF_HDMI_UnRegCallbackFunc(enHDMIId, &g_stCallbackFunc);
    if(ret != HI_SUCCESS)
    {
        ALOGE("HI_UNF_HDMI_UnRegCallbackFunc Err--- %p", (void*)ret);
    }

    ret = HI_UNF_HDMI_DeInit();
    if(ret != HI_SUCCESS)
    {
        ALOGE("HI_UNF_HDMI_DeInit Err--- %p", (void*)ret);
    }

    return ret;
}

HI_S32 set_HDMI_Start()
{
    //这个接口是为了和Close接口对应添加的
    ALOGI("\n HI_UNF_HDMI_Start \n");
    HI_S32 Ret = HI_UNF_HDMI_Start(HI_UNF_HDMI_ID_0);
    if (Ret != HI_SUCCESS)
    {
        ALOGE("HI_UNF_HDMI_Start failed:%#x\n",Ret);
        return HI_FAILURE;
    }
    return Ret;
}

HI_S32 set_HDMI_Stop()
{
    ALOGI("\n set_HDMI_Stop\n");

    HI_S32 Ret = HI_UNF_HDMI_Stop(0);
    if (Ret != HI_SUCCESS)
    {
        ALOGE("HI_UNF_HDMI_Stop failed:%#x\n",Ret);
        return HI_FAILURE;
    }
    return Ret;
}
