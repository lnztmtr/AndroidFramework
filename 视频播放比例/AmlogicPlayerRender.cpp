
//#define LOG_NDEBUG 0
#define LOG_TAG "AmlogicPlayerRender"
#include "utils/Log.h"
#include <stdio.h>

#include "AmlogicPlayerRender.h"

#include <gui/Surface.h>
#include <gui/ISurfaceTexture.h>
//#include <gui/SurfaceTextureClient.h>
#include <gui/ISurfaceComposer.h>

#include <android/native_window.h>
#include <cutils/properties.h>

#include "AmlogicPlayer.h"

#include <Amavutils.h>
#include <gralloc_priv.h>
#include <stdio.h>
#include <fcntl.h>

#define ALIGN(x, a) (((x)+(a)-1)&~((a)-1))
//#define  TRACE()    LOGV("[%s::%d]\n",__FUNCTION__,__LINE__)
#define  TRACE()
#define DEFAULT_UPDATE_INTERVAL_MS 0
#define OSDVIDEO_UPDATE_INTERVAL_MS 10
#define NORMAL_UPDATE_INTERVAL_MS 100
#define PAUSED_UPDATE_INTERVAL_MS 1000
#define BUFFERNUM 4
namespace android
{

AmlogicPlayerRender::AmlogicPlayerRender()
{
    AmlogicPlayerRender(NULL);
}

AmlogicPlayerRender::AmlogicPlayerRender(const sp<ANativeWindow> &nativeWindow, int flags)
    : Thread(false), mNativeWindow(nativeWindow)
{
	LOGI("AmlogicPlayerRender");
    /*make sure first setting,*/
    mnewRect = Rect(0, 0);
    moldRect = Rect(2, 2);
    mcurRect = Rect(3, 3);
    mVideoTransfer = 0;
    mVideoTransferChanged = true;
    mWindowChanged = 3;
    mUpdateInterval_ms = DEFAULT_UPDATE_INTERVAL_MS;
    mRunning = false;
    mPaused = true;
    mOSDisScaled = false;
    nativeWidth = 0;
    nativeHeight = 0;
    ionvideo_dev = NULL;
    mVideoScalingMode = NATIVE_WINDOW_SCALING_MODE_SCALE_TO_WINDOW;
    mFlags = flags;
    mLatestUpdateNum = 0;
    mV4LFormat = V4L2_PIX_FMT_NV21;
    mNativeFormat = HAL_PIXEL_FORMAT_YCrCb_420_SP;
    mBytePerPixel = 1.5;
    mInitNum = 0;
    mEnableOsd = 0;
    mBufferList = NULL;
    mMinUndequeuedBufs = 2;
    mNativeWindowChanged = 0;
	mIonInitIsOk = false;
	mOsdInitIsOk = false;
    return;
}


AmlogicPlayerRender::~AmlogicPlayerRender()
{
    /* to keep the last frame when switching channel, it should not reset screen_mode*/
    int disable_video = amsysfs_get_sysfs_int16("/sys/class/video/disable_video");
    Stop();
    updateOSDscaling(0);
    if (mEnableOsd) {
        SwitchToOSDVideo(0);
    }else{
        char value[PROPERTY_VALUE_MAX];
        if (property_get("media.video.screenmode", value, NULL) > 0) {
            LOGI("get media.video.screenmode [%s]\n", value);
        }
        if((strcmp(value, "true") != 0) && disable_video)
            amsysfs_set_sysfs_int("/sys/class/video/screen_mode",0);//reset to normal mode,
    }
    LOGI("~AmlogicPlayerRender");
    return;
}
status_t AmlogicPlayerRender::SwitchNativeWindow(const sp<ANativeWindow> &nativeWindow)
{
    TRACE();
    if (mEnableOsd) {
        Mutex::Autolock l(mMutex);
        mInitNum = 0;
        mNativeWindow = nativeWindow;
        mNativeWindowChanged = 1;
        if(mBufferList==NULL){
        	return OK;
        }
        memset(mBufferList, 0x0, sizeof(mBufferList) * BUFFERNUM);
        for (int i = 0; i < BUFFERNUM; i++){
            mBufferList[i].index = -1;
        }
        return OK;
    } else {
        Mutex::Autolock l(mMutex);
        mNativeWindow = nativeWindow;
        NativeWindowInit();
        return OK;
    }
    return OK;
}
bool AmlogicPlayerRender::PlatformWantOSDscale(void)
{
    char mode[32] = "panel";
#define OSDSCALESET                 "rw.fb.need2xscale"
#define PLAYERER_ENABLE_SCALER      "media.amplayer.osd2xenable"
#define DISP_MODE_PATH                  "/sys/class/display/mode"

    if (AmlogicPlayer::PropIsEnable(PLAYERER_ENABLE_SCALER) && /*Player has enabled scaler*/
        AmlogicPlayer::PropIsEnable(OSDSCALESET) && /*Player framebuffer have enable*/
        (!amsysfs_get_sysfs_str(DISP_MODE_PATH, mode, 32) && !strncmp(mode, "1080p", 5))) { /*hdmi  1080p*/
        LOGI("PlatformWantOSDscale true\n");

        return true;
    }

    return false;
}
status_t AmlogicPlayerRender::setVideoScalingMode_locked(int mode)
{
    mVideoScalingMode = mode;
    LOGV("setVideoScalingMode %d\n", mode);
    if (mNativeWindow.get() != NULL) {
        status_t err = native_window_set_scaling_mode(mNativeWindow.get(), mVideoScalingMode);
        if (err != OK) {
            ALOGW("Failed to set scaling mode: %d", err);
        }
        if (!mEnableOsd) {
            int videoscreenmode = 1; //0.normal mode,1.full,2.4:3,3,16:9
            switch (mode) {
            case NATIVE_WINDOW_SCALING_MODE_FREEZE:
                videoscreenmode = 0;
                break;
            case NATIVE_WINDOW_SCALING_MODE_SCALE_TO_WINDOW:
                videoscreenmode = 1; ///full strecch
                break;
            case NATIVE_WINDOW_SCALING_MODE_SCALE_CROP:
            case NATIVE_WINDOW_SCALING_MODE_NO_SCALE_CROP:
            default:
                videoscreenmode = 0; ///normal mode
            }
#ifndef UNUSE_SCREEN_MODE
            LOGV("set video screenmode to %d\n", videoscreenmode);
            char value[PROPERTY_VALUE_MAX];
            if (property_get("media.video.screenmode", value, NULL) > 0) {
                LOGI("get media.video.screenmode [%s]\n", value);
            }
            if(strcmp(value, "true") != 0)
                amsysfs_set_sysfs_int("/sys/class/video/screen_mode", videoscreenmode);
#endif
        }
        return err;
    }
    return UNKNOWN_ERROR;
}

status_t AmlogicPlayerRender::setVideoScalingMode(int mode) {
    Mutex::Autolock l(mMutex);
    return setVideoScalingMode_locked(mode);
}

int  AmlogicPlayerRender::updateOSDscaling(int enable)
{
    bool platneedscale;
    int needed = 0;
    if (mVideoTransfer == 0 ||
        mVideoTransfer == HAL_TRANSFORM_ROT_180) {
        needed = 1;
        //only scale on equal or large than 720p
    }
    platneedscale = PlatformWantOSDscale(); /*platform need it*/
    if (enable && needed && !mOSDisScaled && platneedscale) {
        mOSDisScaled = true;
        LOGI("Enabled width scaling\n");
        amdisplay_utils_set_scale_mode(2, 1);
    } else if ((mOSDisScaled && !enable) || (mOSDisScaled && !needed)) {
        LOGI("Disable width scaling\n");
        amdisplay_utils_set_scale_mode(1, 1);
    } else {
        /*no changes do nothing*/
    }
    return 0;
}
//static
int  AmlogicPlayerRender::SwitchToOSDVideo(int enable)
{
    int ret = 0;;
    char newsetting[128];
    char buf[512] = {0};
    char tmp[64] = {0};
    amsysfs_get_sysfs_str("/sys/class/vfm/map", buf, 512);
    //LOGI("get vfm map [%s]\n", buf);
    char *ptr = NULL;
    ptr = strstr(buf, "default { ");
    if(ptr != NULL) {
        int i=0;
        int j=0;
        for(i=10; i<512; i++) {
            if((ptr[i] == '}') || (ptr[i] == '\n')
                    || (ptr[i] == 0)) {
                break;
            } else if((ptr[i] != '(') && (ptr[i] != '0') 
                    && (ptr[i] != '1') && (ptr[i] != ')')) {
                tmp[j++] = ptr[i];
            }
        }
    }
    LOGI("parser vfm map [%s]\n", tmp);

    if (enable) {
        amsysfs_set_sysfs_str("/sys/module/ionvideo/parameters/freerun_mode", "0");
        if(strcmp("decoder ionvideo", tmp) != 0) {
            LOGI("enable osd video  rm default");
            ret =  amsysfs_set_sysfs_str("/sys/class/vfm/map", "rm default");
            if (ret == -1) {
                LOGI("enable osd video  rm default failed");
                return ret;
            }
            LOGI("enable osd video  rm default %d", ret);
            ret = amsysfs_set_sysfs_str("/sys/class/vfm/map", "add default decoder ionvideo");
        }
        LOGI("enable osd video ...%d", ret);
    } else {
        char value[PROPERTY_VALUE_MAX];
        if (property_get("media.decoder.vfm.defmap", value, NULL) > 0) {
            LOGI("get def maping [%s]\n", value);
        } else {
            strcpy(value, "decoder ppmgr amvideo ");
        }
        amsysfs_set_sysfs_str("/sys/module/ionvideo/parameters/freerun_mode", "1");
        if(strcmp(value, tmp) != 0) {
            strcpy(newsetting, "add default ");
            strcat(newsetting, value);
            ret = amsysfs_set_sysfs_str("/sys/class/vfm/map", "rm default");
            LOGI("disable osd video  rm default= %d", ret);
            ret = amsysfs_set_sysfs_str("/sys/class/vfm/map", newsetting);
        }
        LOGI("disable osd video ...%d", ret);
    }

    return ret;
}
//static
int  AmlogicPlayerRender::SupportOSDVideo(void)
{
    int s1 = access("/sys/class/vfm/map", R_OK | W_OK);
    int s2 = access("/sys/module/ionvideo/parameters/freerun_mode", R_OK | W_OK);
    int s3 = access("/dev/video13", R_OK | W_OK);
    if (s1 || s2 || s3) {
        LOGI("Kernel Not suport OSD video,access.map=%d,freerun_mode=%d,/dev/video13=%d\n", s1, s2, s3);
        return 0;
    }
    return 1;
}
status_t AmlogicPlayerRender::NativeWindowInit(void)
{
    TRACE();
    if (!mNativeWindow.get()) {
        return UNKNOWN_ERROR;
    }
    native_window_set_buffer_count(mNativeWindow.get(), BUFFERNUM);
    if (ionvideo_dev) {
        native_window_set_usage(mNativeWindow.get(), GRALLOC_USAGE_AML_DMA_BUFFER | GRALLOC_USAGE_HW_TEXTURE | GRALLOC_USAGE_EXTERNAL_DISP);
        native_window_set_buffers_geometry(mNativeWindow.get(), ALIGN(nativeWidth, 32), nativeHeight, mNativeFormat);
        int err = mNativeWindow->query(mNativeWindow.get(), NATIVE_WINDOW_MIN_UNDEQUEUED_BUFFERS, &mMinUndequeuedBufs);
        if (err != 0) {
            ALOGE("NATIVE_WINDOW_MIN_UNDEQUEUED_BUFFERS query failed: %s (%d)", strerror(-err), -err);
            return err;
        }
        LOGI("mMinUndequeuedBufs:%d", mMinUndequeuedBufs);
    } else {
	    nativeWidth = 32;
		nativeHeight = 32;
        native_window_set_usage(mNativeWindow.get(), GRALLOC_USAGE_HW_TEXTURE | GRALLOC_USAGE_EXTERNAL_DISP | GRALLOC_USAGE_AML_VIDEO_OVERLAY);
        //native_window_set_buffers_format(mNativeWindow.get(), WINDOW_FORMAT_RGBA_8888);
		native_window_set_buffers_geometry(mNativeWindow.get(), ALIGN(nativeWidth, 32), nativeHeight, WINDOW_FORMAT_RGBA_8888);
    }
    setVideoScalingMode_locked(mVideoScalingMode);
    return OK;
}

status_t AmlogicPlayerRender::OSDVideoInit(void)
{
    int ret;

    ionvideo_dev = new_ionvideo(FLAGS_V4L_MODE);
    if (!ionvideo_dev) {
        LOGE("new_ionvideo error");
        return -1;
    }
    ret = ionvideo_init(ionvideo_dev, 0, nativeWidth, nativeHeight, mV4LFormat, BUFFERNUM);
    if (ret != 0) {
        LOGE("amvideo_init failed =%d\n", ret);
        ionvideo_release(ionvideo_dev);
        ionvideo_dev = NULL;
        return -1;
    }
    ionvideo_start(ionvideo_dev);
    LOGI("OSDVideoInit video size nativeWidth=%d,nativeHeight=%d\n", nativeWidth, nativeHeight);
    mBufferList = (BufferList*) malloc(sizeof(BufferList) * BUFFERNUM);
    memset(mBufferList, 0x0, sizeof(mBufferList) * BUFFERNUM);
    for (int i = 0; i < BUFFERNUM; i++){
        mBufferList[i].index = -1;
    }
    mInitNum = 0;
    return 0;
}
void AmlogicPlayerRender::onFirstRef()
{
    TRACE();
	LOGI("AmlogicPlayerRender::onFirstRef");
    Mutex::Autolock l(mMutex);
	int wait_time=0;
	mAmstreamCanBeOpened = false;

	do {
		int handle = open("/dev/amstream_vbuf", O_RDWR | O_NONBLOCK);
	    if (handle < 0) {
	        LOGE("amstream_vbuf opend failed!***********************");
			usleep(100000);
			wait_time++;
	    } else {
	        int ret = close(handle);
			mAmstreamCanBeOpened = true;
	        LOGI("onFirstRef___________amstream_vbuf_______OK");
			break;
	    }
	} while (wait_time < 50);
	
    if (mFlags & 1 &&
        AmlogicPlayer::PropIsEnable("media.amplayer.v4osd.enable", true) &&
        ionvideo_dev == NULL &&
        SupportOSDVideo()) {
        if (mAmstreamCanBeOpened == false)
            return;
		int ret = SwitchToOSDVideo(1);
		mEnableOsd = 1;
		if (ret != 0) {
			LOGE("SwitchToOSDVideo(1) error");
			return;
		}
		mOsdInitIsOk = true;
        ret = OSDVideoInit();
		if (ret != 0) {
			LOGE("OSDVideoInit error");
			return;
		}
		mIonInitIsOk = true;
        //NativeWindowInit();
        
    } else {
        NativeWindowInit();
        mEnableOsd = 0;
    }
}

status_t AmlogicPlayerRender::initCheck()
{
    TRACE();
    LOGI("initCheck");
    return OK;
}

status_t AmlogicPlayerRender::readyToRun()
{
    TRACE();

    return OK;
}


int  AmlogicPlayerRender::VideoFrameUpdate()
{
    Mutex::Autolock l(mMutex);
    char* vaddr;
    int ret = 0;
    ANativeWindowBuffer* buf;

    if (mNativeWindow.get() == NULL) {
        return 0;
    }
    int err = mNativeWindow->dequeueBuffer_DEPRECATED(mNativeWindow.get(), &buf);
    if (err != 0) {
        LOGE("dequeueBuffer failed: %s (%d)", strerror(-err), -err);
        return -1;
    }
    mNativeWindow->lockBuffer_DEPRECATED(mNativeWindow.get(), buf);
    sp<GraphicBuffer> graphicBuffer(new GraphicBuffer(buf, false));
    graphicBuffer->lock(1, (void **)&vaddr);
    if (vaddr != NULL) {
        memset(vaddr, 0x0, graphicBuffer->getWidth() * graphicBuffer->getHeight() * 4); /*to show video in osd hole...*/
    }
    graphicBuffer->unlock();
    graphicBuffer.clear();

    return mNativeWindow->queueBuffer_DEPRECATED(mNativeWindow.get(), buf);
}


status_t AmlogicPlayerRender::Update()
{
    if (mNativeWindow.get() != NULL) {
        mLatestUpdateNum = VideoFrameUpdate();
    }
    if (mWindowChanged) {
        mWindowChanged--;
        updateOSDscaling(1);
    }
    return OK;
}
status_t AmlogicPlayerRender::ScheduleOnce()
{
    TRACE();
    mCondition.signal();
    return OK;
}


status_t AmlogicPlayerRender::Start()
{
    TRACE();
    if (mAmstreamCanBeOpened == false) {
        LOGE("AmlogicPlayerRender::Start error, mAmstreamCanBeOpened == false");
        return -1;
    }
    if (mFlags & 1) {
	    if (!mOsdInitIsOk || !mIonInitIsOk || ionvideo_dev==NULL) {
		    LOGE("AmlogicPlayerRender::Start  error");
		    return -1;
	    }
    }
    if (!mRunning) {
        run("AmplayerRender");
    }
    Mutex::Autolock l(mMutex);
    mRunning = true;
    mPaused = false;
    ScheduleOnce();
    return OK;
}

status_t AmlogicPlayerRender::Stop()
{
    TRACE();
    {
        Mutex::Autolock l(mMutex);
        mRunning = false;
    }
	mCondition.signal();
    requestExitAndWait();
    if (mEnableOsd) {
        Mutex::Autolock l(mMutex);
        if (ionvideo_dev) {
            ionvideo_release (ionvideo_dev);
            ionvideo_dev = NULL;
			mIonInitIsOk = false;
        }
        if (mBufferList != NULL) {
            free (mBufferList);
            mBufferList = NULL;
        }
    }
    return OK;
}

status_t AmlogicPlayerRender::Pause()
{
    TRACE();
    Mutex::Autolock l(mMutex);
    mPaused = true;
    mUpdateInterval_ms = PAUSED_UPDATE_INTERVAL_MS;
    mLatestUpdateNum = 0;
    ScheduleOnce();
    return OK;
}

status_t AmlogicPlayerRender::onSizeChanged(Rect newR, Rect oldR)
{
    TRACE();
    Mutex::Autolock l(mMutex);
    if (0/*!mEnableOsd*/) {
        nativeWidth = oldR.getWidth();
        nativeHeight = oldR.getHeight();
    }
    return OK;
}
bool AmlogicPlayerRender::windowUpdate()
{
	TRACE();
	return (mWindowChanged != 3);
}
 

bool AmlogicPlayerRender::threadLoop()
{
    if (mAmstreamCanBeOpened == false)
        return false;
    if (mFlags & 1) {
        if (!mIonInitIsOk || !mOsdInitIsOk || ionvideo_dev==NULL)
            return false;
    }
    if (mEnableOsd) {
        return dequeueThread();
    } else {
        if (mRunning && mLatestUpdateNum <= 0) {
            Mutex::Autolock l(mMutex);
            mCondition.waitRelative(mMutex, milliseconds_to_nanoseconds(mUpdateInterval_ms));
        }
	if(mUpdateInterval_ms == DEFAULT_UPDATE_INTERVAL_MS) {
            mUpdateInterval_ms = NORMAL_UPDATE_INTERVAL_MS;
        }
        if (mRunning && !mPaused) {
            Update();
        }
    }
    return true;
}

bool AmlogicPlayerRender::dequeueThread()
{
    TRACE();
    Mutex::Autolock l(mMutex);
    ANativeWindowBuffer* buf;
    vframebuf_t vf;
    int ret = 0;
    int err = 0;
    int w, h, fmt;

    mCondition.waitRelative(mMutex, milliseconds_to_nanoseconds(5));
    if (mNativeWindow.get() == NULL) {
        LOGE("mNativeWindow.get() NULL");
        return true;
    }

    ret = ionvideo_getparameters(ionvideo_dev, &w, &h, &fmt);
    if(ret == -EINVAL) {
        return true;
    }
    if (((w != nativeWidth || h != nativeHeight) && ret != -EAGAIN) || mNativeWindowChanged) {
        nativeWidth = w;
        nativeHeight = h;
        mNativeWindowChanged = 0;
        ionvideo_release (ionvideo_dev);
        ionvideo_dev = new_ionvideo(FLAGS_V4L_MODE);
        if (!ionvideo_dev) {
            LOGE("new_ionvideo error");
            return false;
        }
        ret = ionvideo_init(ionvideo_dev, 0, nativeWidth, nativeHeight, mV4LFormat, BUFFERNUM);
        if (ret != 0) {
            LOGE("ionvideo_reset failed =%d\n", ret);
            return false;
        }
        LOGI("ionvideo size changed nativeWidth=%d,nativeHeight=%d\n", nativeWidth, nativeHeight);
        for (int i = 0; i < BUFFERNUM; i++){
            if (mBufferList[i].index != -1) {
                err = mNativeWindow->cancelBuffer_DEPRECATED(mNativeWindow.get(), mBufferList[i].nativeBuffer);
                if (err) {
                    LOGE("cancelBuffer_DEPRECATED error:%d index:%d", err, i);
                    return false;
                }
            }
        }
        memset(mBufferList, 0x0, sizeof(mBufferList) * BUFFERNUM);
        for (int i = 0; i < BUFFERNUM; i++){
            mBufferList[i].index = -1;
        }
        ionvideo_start(ionvideo_dev);
        NativeWindowInit();
        for (int i = 0; i < BUFFERNUM - mMinUndequeuedBufs; i++) {
            err = mNativeWindow->dequeueBuffer_DEPRECATED(mNativeWindow.get(), &buf);
            if (err) {
                LOGE("mNativeWindow dequeueBuffer failed: %s (%d)", strerror(-err), -err);
                return false;
            }
            vf.index = i;
            vf.fd = ((private_handle_t*)buf->handle)->share_fd;
            vf.length = nativeWidth * nativeHeight * mBytePerPixel;
            ret = ionv4l_queuebuf(ionvideo_dev, &vf);
            if (ret) {
                LOGE("ionv4l_queuebuf error, errno:%d\n", ret);
                return false;
            }
            mBufferList[i].shareFd = vf.fd;
            mBufferList[i].nativeBuffer = buf;
            mBufferList[i].index = i;
            LOGI("init mBufferList fd:%d buf:%x index:%d", vf.fd, buf, i);
        }
        mInitNum = BUFFERNUM - mMinUndequeuedBufs;
    }
    ////////////////////////////////////////////////////////////////////////////
    ret = ionv4l_dequeuebuf(ionvideo_dev, &vf);
    if (ret == -EAGAIN) {
        //mCondition.waitRelative(mMutex, milliseconds_to_nanoseconds(2));
        return true;
    } else if (ret) {
        LOGE("ionv4l_dequeuebuf error, errno:%d\n", errno);
        return false;
    }
    err = mNativeWindow->queueBuffer_DEPRECATED(mNativeWindow.get(), mBufferList[vf.index].nativeBuffer);
    if (err) {
        LOGE("mNativeWindow dequeueBuffer failed: %s (%d)", strerror(-err), -err);
        return false;
    }
    mBufferList[vf.index].index = -1;
#if 0
    {//timestamp test//
        struct timeval tv;
        gettimeofday(&tv, NULL);
        uint64_t nowUs = tv.tv_sec * 1000000ll + tv.tv_usec;
        static uint64_t lastUs = 0;
        LOGI("========%llu=", nowUs - lastUs);
        lastUs = nowUs;
    }
#endif
    ////////////////////////////////////////////////////////////////////////////
    err = mNativeWindow->dequeueBuffer_DEPRECATED(mNativeWindow.get(), &buf);
    if (err) {
        LOGE("mNativeWindow dequeueBuffer failed: %s (%d)", strerror(-err), -err);
        return false;
    }
    vf.fd = ((private_handle_t*)buf->handle)->share_fd;
    if (mInitNum < BUFFERNUM) {
        int state = 0;
        for (int i = 0; i < mInitNum; i++) {
            if (mBufferList[i].shareFd == vf.fd) {
                state = 1;
                break;
            }
        }
        if (!state) {
            mBufferList[mInitNum].shareFd = vf.fd;
            mBufferList[mInitNum++].nativeBuffer = buf;
        }
    }
    vf.index = 0;
    while(vf.index < BUFFERNUM && mBufferList[vf.index].shareFd != vf.fd) {
        vf.index += 1;
    }
    mBufferList[vf.index].index = vf.index;
    vf.length = nativeWidth * nativeHeight * mBytePerPixel;
    //LOGI("ionv4l_queuebuf fd:%d buf:%x index:%d", vf.fd, buf, vf.index);
    ret = ionv4l_queuebuf(ionvideo_dev, &vf);
    if (ret != 0) {
        LOGE("ionv4l_queuebuf error, errno:%d\n", errno);
        return false;
    }

    return true;
}

}
