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

#define LOG_TAG "BootAnimation"

#include <stdint.h>
#include <sys/types.h>
#include <math.h>
#include <fcntl.h>
#include <utils/misc.h>
#include <signal.h>
#include <dirent.h>
#include <string.h>

#include <cutils/properties.h>

#include <androidfw/AssetManager.h>
#include <binder/IPCThreadState.h>
#include <utils/Atomic.h>
#include <utils/Errors.h>
#include <utils/Log.h>
#include <utils/threads.h>

#include <ui/PixelFormat.h>
#include <ui/Rect.h>
#include <ui/Region.h>
#include <ui/DisplayInfo.h>
#include <ui/FramebufferNativeWindow.h>

#include <gui/ISurfaceComposer.h>
#include <gui/Surface.h>
#include <gui/SurfaceComposerClient.h>

#include <core/SkBitmap.h>
#include <core/SkCanvas.h>
#include <core/SkStream.h>
#include <core/SkImageDecoder.h>
#include <core/SkTypeface.h>

#include <GLES/gl.h>
#include <GLES/glext.h>
#include <EGL/eglext.h>

#include <android/native_window.h>
#include "BootAnimation.h"

#include <binder/IServiceManager.h>
#include "HiMediaPlayerInvoke.h"
#include "HiMediaDefine.h"

#include "IHiAoService.h"

#include <hi_unf_pdm.h>
#include <iostream>
#include <fstream>
#include <json/json.h> // or jsoncpp/json.h , or json/json.h etc.

#define USER_BOOTVIDEOANIMATION_FILE "/data/local/bootVideoAnimation.zip"
#define USER_BOOTANIMATION_FILE "/data/local/bootanimation.zip"
#define SYSTEM_BOOTANIMATION_FILE "/system/media/bootanimation.zip"
#define QUICKBOOT_BOOTANIMATION_FILE "/system/media/quickboot.zip"
#define SYSTEM_ENCRYPTED_BOOTANIMATION_FILE "/system/media/bootanimation-encrypted.zip"
#define EXIT_PROP_NAME "service.bootanim.exit"
#define BUFLEN 92
/*add by zhaolianghua for hebei bootvideo source name @20190130*/
#ifdef PROVINCE_TYPE_CM201_HE
#define USER_BOOT_VIDEO "/data/local/bootvideo.ts"
#else
#define USER_BOOT_VIDEO "/data/local/boot.ts"
#endif
/*add end*/
#define USER_BOOT_VIDEO_DEFAULT "/system/media/boot.ts"
#define SWAP_PROP_NAME "persist.sys.swap.enable"
#define VIDEO_PROP_NAME "service.bootanim.bootvideo.exit"
#define BOOTANIM_CUSTOMIZATION_PATH "/data/local/"
#define BOOTANIM_CUSTOMIZATION_CFG "/data/local/configs.txt"
#define BOOTANIM_CUSTOMIZATION_CFG_JSON "/data/local/miguaddata.txt"
#define BOOTANIM_CUSTOMIZATION_PATH_DEFAULT "/system/media/"
#define BOOTANIM_CUSTOMIZATION_CFG_DEFAULT "/system/media/configs.txt"
#define BOOTANIM_CUSTOMIZATION_CFG_JSON_DEFAULT "/system/media/miguaddata.txt"
#define BOOT_ANIM_PLAYING "service.bootvideo.playing"

//added by yzs for zhejiang zj add begin
#define BOOTANIM_CUSTOMIZATION_ZJ_CFG "/data/local/zj/default_configs.txt"
#define BOOTANIM_CUSTOMIZATION_ZJ_PATH "/data/local/zj/"
//end

// bootanimation volume
#define MAX_VOlUME_IN_BASE     100
#define DEFAULT_BASE_VOLUME    30

static const char* gVideoPath = NULL;

static int gSystemVolume = 0;
// which volume will use in bootanimation
/**
* VOLUME_SOURCE    'null' or 'false'    means get volume from base
* VOLUME_SOURCE    'true'               means get volume from system
* SYSTEM_VOLUME    system volume
*/
#define VOLUME_FROM_SYSTEM     "persist.animation.use.sysvolume"
#define SYSTEM_VOLUME          "persist.sys.audio.volume"
#define DEFAULT_SYSTEM_VOLUME  "42"

extern "C" int clock_nanosleep(clockid_t clock_id, int flags,
                           const struct timespec *request,
                           struct timespec *remain);

namespace android {

int setBootAnimationVolume();
// ---------------------------------------------------------------------------
extern bool hasFastplay();

class MediaListner: public MediaPlayerListener
{
public:
    bool isPlayCompleted;
    bool isFirstFrame;
    MediaListner();
    ~MediaListner();
    virtual void notify(int msg, int ext1, int ext2, const Parcel *obj);
};

MediaListner::MediaListner()
{
    isPlayCompleted = false;
    isFirstFrame = false;
}

MediaListner::~MediaListner()
{
}

void MediaListner::notify(int msg, int ext1, int ext2, const Parcel *obj)
{
    if (MEDIA_PLAYBACK_COMPLETE == msg) {
        isPlayCompleted = true;
    }
    else if (MEDIA_INFO == msg) {
        if (MEDIA_INFO_NOT_SUPPORT == ext1) {
            isPlayCompleted = true;
        }else if(3 == ext1){
            isFirstFrame = true;
        }else if(MEDIA_INFO_VIDEO_FAIL == ext1){
            isPlayCompleted = true;
        }
    }
}

BootAnimation::BootAnimation() : Thread(false)
{
    mSession = new SurfaceComposerClient();
}

BootAnimation::~BootAnimation() {
}

void BootAnimation::onFirstRef() {

    int ret = setBootAnimationVolume();
    if (ret != HI_SUCCESS)
    {
        ALOGE("Set BootAnimation Volume Failed");
    }
    else
    {
        ALOGE("Set BootAnimation Volume Successed");
    }

    status_t err = mSession->linkToComposerDeath(this);
    ALOGE_IF(err, "linkToComposerDeath failed (%s) ", strerror(-err));
    if (err == NO_ERROR) {
        property_set(SWAP_PROP_NAME, "0");//surfaceflinger swap disable
        run("BootAnimation", PRIORITY_DISPLAY);
    }else{
        // make sure if bootanimation failed , we stop fastplay to avoid fastplay always playing
        if (hasFastplay()){
            char buffer[PROPERTY_VALUE_MAX] = {0};
            property_get("persist.sys.fastplay.fullyplay", buffer, "false");
            if (!strcasecmp("true", buffer)){
                system("echo stop count 1 > /proc/msp/mce");
            }else{
                system("echo stop time 0 > /proc/msp/mce");
            }
        }
        property_set(SWAP_PROP_NAME, "1");//surfaceflinger swap enable
    }
}

sp<SurfaceComposerClient> BootAnimation::session() const {
    return mSession;
}


void BootAnimation::binderDied(const wp<IBinder>& who)
{
    // woah, surfaceflinger died!
    ALOGD("SurfaceFlinger died, exiting...");

    // calling requestExit() is not enough here because the Surface code
    // might be blocked on a condition variable that will never be updated.
    kill( getpid(), SIGKILL );
    requestExit();
}

status_t BootAnimation::initTexture(Texture* texture, AssetManager& assets,
        const char* name) {
    Asset* asset = assets.open(name, Asset::ACCESS_BUFFER);
    if (!asset)
        return NO_INIT;
    SkBitmap bitmap;
    SkImageDecoder::DecodeMemory(asset->getBuffer(false), asset->getLength(),
            &bitmap, SkBitmap::kNo_Config, SkImageDecoder::kDecodePixels_Mode);
    asset->close();
    delete asset;

    // ensure we can call getPixels(). No need to call unlock, since the
    // bitmap will go out of scope when we return from this method.
    bitmap.lockPixels();

    const int w = bitmap.width();
    const int h = bitmap.height();
    const void* p = bitmap.getPixels();

    GLint crop[4] = { 0, h, w, -h };
    texture->w = w;
    texture->h = h;

    glGenTextures(1, &texture->name);
    glBindTexture(GL_TEXTURE_2D, texture->name);

    switch (bitmap.getConfig()) {
        case SkBitmap::kA8_Config:
            glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, w, h, 0, GL_ALPHA,
                    GL_UNSIGNED_BYTE, p);
            break;
        case SkBitmap::kARGB_4444_Config:
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA,
                    GL_UNSIGNED_SHORT_4_4_4_4, p);
            break;
        case SkBitmap::kARGB_8888_Config:
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA,
                    GL_UNSIGNED_BYTE, p);
            break;
        case SkBitmap::kRGB_565_Config:
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, w, h, 0, GL_RGB,
                    GL_UNSIGNED_SHORT_5_6_5, p);
            break;
        default:
            break;
    }

    glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_CROP_RECT_OES, crop);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    return NO_ERROR;
}

status_t BootAnimation::initTexture(void* buffer, size_t len)
{
    //StopWatch watch("blah");

    SkBitmap bitmap;
    SkMemoryStream  stream(buffer, len);
    SkImageDecoder* codec = SkImageDecoder::Factory(&stream);
    if (codec) {
        codec->setDitherImage(false);
        codec->decode(&stream, &bitmap,
                SkBitmap::kARGB_8888_Config,
                SkImageDecoder::kDecodePixels_Mode);
        delete codec;
    }

    // ensure we can call getPixels(). No need to call unlock, since the
    // bitmap will go out of scope when we return from this method.
    bitmap.lockPixels();

    const int w = bitmap.width();
    const int h = bitmap.height();
    const void* p = bitmap.getPixels();

    GLint crop[4] = { 0, h, w, -h };
    int tw = 1 << (31 - __builtin_clz(w));
    int th = 1 << (31 - __builtin_clz(h));
    if (tw < w) tw <<= 1;
    if (th < h) th <<= 1;

    switch (bitmap.getConfig()) {
        case SkBitmap::kARGB_8888_Config:
            if (tw != w || th != h) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, tw, th, 0, GL_RGBA,
                        GL_UNSIGNED_BYTE, 0);
                glTexSubImage2D(GL_TEXTURE_2D, 0,
                        0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, p);
            } else {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, tw, th, 0, GL_RGBA,
                        GL_UNSIGNED_BYTE, p);
            }
            break;

        case SkBitmap::kRGB_565_Config:
            if (tw != w || th != h) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, tw, th, 0, GL_RGB,
                        GL_UNSIGNED_SHORT_5_6_5, 0);
                glTexSubImage2D(GL_TEXTURE_2D, 0,
                        0, 0, w, h, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, p);
            } else {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, tw, th, 0, GL_RGB,
                        GL_UNSIGNED_SHORT_5_6_5, p);
            }
            break;
        default:
            break;
    }

    glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_CROP_RECT_OES, crop);

    return NO_ERROR;
}

status_t BootAnimation::readyToRun() {
    mAssets.addDefaultAssets();

    sp<IBinder> dtoken(SurfaceComposerClient::getBuiltInDisplay(
            ISurfaceComposer::eDisplayIdMain));
    DisplayInfo dinfo;
    status_t status = SurfaceComposerClient::getDisplayInfo(dtoken, &dinfo);
    if (status)
        return -1;

    // create the native surface
    sp<SurfaceControl> control = session()->createSurface(String8("BootAnimation"),
            dinfo.w, dinfo.h, PIXEL_FORMAT_RGB_565);

    SurfaceComposerClient::openGlobalTransaction();
    control->setLayer(0x40000000);
    SurfaceComposerClient::closeGlobalTransaction();

    sp<Surface> s = control->getSurface();

    // initialize opengl and egl
    const EGLint attribs[] = {
            EGL_RED_SIZE,   8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE,  8,
            EGL_DEPTH_SIZE, 0,
            EGL_NONE
    };
    EGLint w, h, dummy;
    EGLint numConfigs;
    EGLConfig config;
    EGLSurface surface;
    EGLContext context;

    EGLDisplay display = eglGetDisplay(EGL_DEFAULT_DISPLAY);

    eglInitialize(display, 0, 0);
    eglChooseConfig(display, attribs, &config, 1, &numConfigs);
    surface = eglCreateWindowSurface(display, config, s.get(), NULL);
    context = eglCreateContext(display, config, NULL, NULL);
    eglQuerySurface(display, surface, EGL_WIDTH, &w);
    eglQuerySurface(display, surface, EGL_HEIGHT, &h);

    if (eglMakeCurrent(display, surface, surface, context) == EGL_FALSE)
        return NO_INIT;

    mDisplay = display;
    mContext = context;
    mSurface = surface;
    mWidth = w;
    mHeight = h;
    mFlingerSurfaceControl = control;
    mFlingerSurface = s;

    mAndroidAnimation = true;

    // If the device has encryption turned on or is in process
    // of being encrypted we show the encrypted boot animation.
    char decrypt[PROPERTY_VALUE_MAX];
    property_get("vold.decrypt", decrypt, "");

    bool encryptedAnimation = atoi(decrypt) != 0 || !strcmp("trigger_restart_min_framework", decrypt);

    char quickboot_enable[PROPERTY_VALUE_MAX];
    char quickboot_flag[PROPERTY_VALUE_MAX];
    char quickboot_frist[PROPERTY_VALUE_MAX];
    property_get("persist.sys.qb.enable", quickboot_enable, "false");
    property_get("persist.sys.qb.flag", quickboot_flag, "false");
    property_get("persist.sys.firstboot.flag", quickboot_frist, "false");
    
    if((strcmp("true", quickboot_enable) == 0) && (strcmp("false", quickboot_flag) == 0) && (strcmp("false", quickboot_frist) == 0))
    {
        if ((encryptedAnimation &&
                (access(SYSTEM_ENCRYPTED_BOOTANIMATION_FILE, R_OK) == 0) &&
                (mZip.open(SYSTEM_ENCRYPTED_BOOTANIMATION_FILE) == NO_ERROR)) ||

                ((access(USER_BOOTANIMATION_FILE, R_OK) == 0) &&
                (mZip.open(USER_BOOTANIMATION_FILE) == NO_ERROR)) ||

                ((access(QUICKBOOT_BOOTANIMATION_FILE, R_OK) == 0) &&
                (mZip.open(QUICKBOOT_BOOTANIMATION_FILE) == NO_ERROR))) {
            mAndroidAnimation = false;
        }
    }
    else
    {
        if ((encryptedAnimation &&
                (access(SYSTEM_ENCRYPTED_BOOTANIMATION_FILE, R_OK) == 0) &&
                (mZip.open(SYSTEM_ENCRYPTED_BOOTANIMATION_FILE) == NO_ERROR)) ||

                ((access(USER_BOOTANIMATION_FILE, R_OK) == 0) &&
                (mZip.open(USER_BOOTANIMATION_FILE) == NO_ERROR)) ||

                ((access(SYSTEM_BOOTANIMATION_FILE, R_OK) == 0) &&
                (mZip.open(SYSTEM_BOOTANIMATION_FILE) == NO_ERROR))) {
            mAndroidAnimation = false;
        }
    }
    if(( access(USER_BOOTVIDEOANIMATION_FILE,R_OK)==0  )&&
            (vZip.open(USER_BOOTVIDEOANIMATION_FILE)==NO_ERROR)) {
        mBootVideoAnimation = true;
    }

    mFullyPlayFastplay = false;

    return NO_ERROR;
}

bool bootTsExist()
{
    return (access(USER_BOOT_VIDEO, R_OK) == 0 || access(USER_BOOT_VIDEO_DEFAULT, R_OK) == 0);
}

int setOutputVolume(int volume)
{
    int ret = HI_FAILURE;
    sp<IBinder> binder = defaultServiceManager()->getService(String16("hiaoservice"));
    sp<IHiAoService> mHiAoService = interface_cast<IHiAoService>(binder);

    if (mHiAoService.get() == NULL)
    {
        ALOGE("Can not get HiAoService!");
        ret = HI_FAILURE;
    }
    else
    {
        ALOGE("volume = [%d] will be set", volume);
        ret = mHiAoService->setSndVolume(volume);
    }
    return ret;
}

int setBootAnimationVolume()
{
    int ret = HI_FAILURE;
    int bootAnimationVol;
    int i;
    char bootAnimVolBuf[PROPERTY_VALUE_MAX] = {0};

    property_get(VOLUME_FROM_SYSTEM, bootAnimVolBuf, "false");

    // Get volume from Android System
    if (!strcasecmp("true", bootAnimVolBuf))
    {
        memset(bootAnimVolBuf, 0, sizeof(bootAnimVolBuf));
        property_get(SYSTEM_VOLUME, bootAnimVolBuf, DEFAULT_SYSTEM_VOLUME);
        bootAnimationVol = atoi(bootAnimVolBuf);
        if (bootAnimationVol < 0 || bootAnimationVol > 100)
        {
            bootAnimationVol = atoi(DEFAULT_SYSTEM_VOLUME);
        }
        ALOGE("Get BootAnimation Volume from Android System, Volume = %d", bootAnimationVol);
    }
    // Get volume from Base
    else
    {
        bootAnimationVol = DEFAULT_BASE_VOLUME;
        HI_UNF_PDM_SOUND_PARAM_S stBaseParamSound0;

        memset(&stBaseParamSound0,0,sizeof(HI_UNF_PDM_SOUND_PARAM_S));
        ret = HI_UNF_PDM_GetBaseParam(HI_UNF_PDM_BASEPARAM_SOUND0, &stBaseParamSound0);

        if(HI_SUCCESS != ret)
        {
            ALOGE("ERR: HI_UNF_PDM_GetBaseParam, ret = %#x\n", ret);
            return HI_FAILURE;
        }

        for (i = 0; i < HI_UNF_SND_OUTPUTPORT_MAX; i++)
        {
            // Get HDMI Volume Channle
            if(stBaseParamSound0.stOutport[i].enOutPort == HI_UNF_SND_OUTPUTPORT_HDMI0)
            {
                bootAnimationVol = stBaseParamSound0.au32Volume[i];
                if (bootAnimationVol < 0 || bootAnimationVol > 100)
                {
                    bootAnimationVol = DEFAULT_BASE_VOLUME;
                }
                ALOGE("Get BootAnimation Volume from Base HDMI, Volume = %d", bootAnimationVol);
                break;
            }
        }
    }
    gSystemVolume = bootAnimationVol;
    return setOutputVolume(bootAnimationVol);
}

bool BootAnimation::threadLoop()
{
    bool r = false;

    char buffer[PROPERTY_VALUE_MAX] = {0};
    bool fastplayExists = hasFastplay();
    if (fastplayExists) {
        property_get("persist.sys.fastplay.fullyplay", buffer, "false");
        //stop fastplay & hold its last frame
        if (!strcasecmp("true", buffer)) {
            mFullyPlayFastplay = true;
            system("echo stoponly count 1 > /proc/msp/mce");
        }else{
            mFullyPlayFastplay = false;
            // system("echo stoponly time 0 > /proc/msp/mce");
        }
    }

    bool bShowned = false;
    property_set(BOOT_ANIM_PLAYING, "true");
    setOutputVolume(0);
    if(hasCustomization()) {

        memset(buffer, 0, sizeof(buffer));
        property_get("persist.sys.bootvideo.completed",buffer,"true");
        bool isVideoComplete = true;
        if(0 == strcmp("false",buffer)){
            isVideoComplete = false;
        }

        /*First, play data/local config mode, this mode can not be interrupted.
          If the system does not start, continue play system/media, This mode can be interrupted.
          If not played data/local config, system/media directory will play completely.*/
        ALOGD("play custom configs");
        bShowned |= playCustomizedAnimation(false, !isVideoComplete);
        if(!checkExit()){
            if(bShowned){
                ALOGD("play default configs");
                bShowned |= playCustomizedAnimation(true/*use default*/, true/* check exit*/);
            }else{
                ALOGD("play default configs, not custom config");
                bShowned |= playCustomizedAnimation(true/*use default*/, !isVideoComplete);
            }
            if(bShowned) {
                const float MAX_FPS = 60.0f;
                const float CHECK_DELAY = ns2us(s2ns(1) / MAX_FPS);
                while(!checkExit()) {
                    ALOGD("be showned, check exit, and wait");
                    usleep(CHECK_DELAY);
                }
            }
        }
    }
    
    if(!bShowned) {
        if(mBootVideoAnimation){
            r = playBootZip();
        } else if (bootTsExist()) {
            r = playBootTs();
        } else {
            memset(buffer, 0, sizeof(buffer));
            property_get("persist.sys.bootanim.enable", buffer, "true");
            if (!strcasecmp("true", buffer) || !fastplayExists) {
                if (mAndroidAnimation) {
                    r = android(false);
                } else {
                    r = movie(false);
                }
            }
        }
    }
    // fix if use combo "logo + fastplay + Launcher " without bootanimation
    // maike Launcher blue screen problem
    property_set(SWAP_PROP_NAME, "1");

    // No need to force exit anymore
    property_set(EXIT_PROP_NAME, "0");

    mVideoControl.clear();
    mVideoSurface.clear();
    property_set(BOOT_ANIM_PLAYING,"false");
    eglMakeCurrent(mDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
    eglDestroyContext(mDisplay, mContext);
    eglDestroySurface(mDisplay, mSurface);
    mFlingerSurface.clear();
    mFlingerSurfaceControl.clear();
    eglTerminate(mDisplay);
    setOutputVolume(gSystemVolume);
    IPCThreadState::self()->stopProcess();
    return r;
}

bool hasFastplay()
{
    char buffer[PROPERTY_VALUE_MAX];
    FILE* file;
    bool ret = false;

    file = fopen("/proc/msp/mce", "r");
    if (file == NULL) {
        return ret;
    }

    fgets(buffer, sizeof(buffer), file);
    memset(buffer, 0, sizeof(buffer));
    fgets(buffer, sizeof(buffer), file); //read second line
    if (strstr(buffer, "1")) {
        ret = true;
    } else {
        ret = false;
    }

    fclose(file);
    return ret;
}

void waitForMediaService()
{
    sp<IServiceManager> sm = defaultServiceManager();
    sp<IBinder> binder;

    do {
        binder = sm->checkService(String16("media.player"));
        if (binder != 0) {
            break;
        }
        ALOGD("media player not published, waiting...");
        usleep(50000); // 0.05 s
    } while (true);
}

void setStopMode(sp<MediaPlayer> mp, int type/*0:stop at last frame; 1:stop as black screen*/)
{
    if (mp == NULL) {
        return;
    }

    Parcel request;
    Parcel reply;
    request.writeInterfaceToken(String16("android.media.IMediaPlayer"));
    request.writeInt32(CMD_SET_VIDEO_FREEZE_MODE);
    request.writeInt32(type);

    mp->invoke(request, &reply);
}

/*0:restore ; 1:only */
bool BootAnimation::setTrackOnly(sp<MediaPlayer> mp, int type){
    if (mp == NULL) {
        return false;
    }
    Parcel request;
    Parcel reply;
    request.writeInterfaceToken(String16("android.media.IMediaPlayer"));
    request.writeInt32(CMD_SET_TRACK_ONLY_FLAG);
    request.writeInt32(type);
    mp->invoke(request, &reply);
    reply.setDataPosition(0);
    int result = reply.readInt32();
    if(result != NO_ERROR){
        ALOGE("set track only failed");
        return false;
    }
    return true;
}

void stopMediaPlayer(sp<MediaPlayer> mp, bool isBlackFrame = true)
{
    if(!isBlackFrame){
        if(mp == NULL){
            return;
        }
        mp->stop();
        mp->reset();
        mp->disconnect();
        return;
    }
    char* proc_path = "/proc/msp/";
    char proc_buf[256] = {0};
    int use_win_proc = 0;
    DIR *dir;
    struct dirent *file;

    dir = opendir(proc_path);
    if(dir){
        while((file = readdir(dir)) != NULL){
            /* Ignore '.' & '..' Dir */
            if(strncmp(file->d_name, ".", 1) == 0)
                continue;
            if(strstr(file->d_name, "win")){
                ALOGD("find window = [%s], use echo to reset window !", file->d_name);
                char *node = strdup(file->d_name);
                snprintf(proc_buf, sizeof(proc_buf), "echo reset black > /proc/msp/%s", node);
                /* reset winXXXX by using 'black' frame */
                system(proc_buf);
                use_win_proc = 1;
            }
        }
        closedir(dir);
    }else{
        ALOGD("can't opendir: [%s] !", proc_path);
    }

    if (!use_win_proc){
        if (mp == NULL) {
            return;
        }else{
            //close the video layer to make video layer black. otherwise last frame will be kept on video layer
            ALOGD("use mediaplayer to close video layer !");
            mp->reset();
            if(gVideoPath != NULL) {
                mp->setDataSource(gVideoPath, NULL);
                mp->prepare();
	        mp->setAudioStreamType(AUDIO_STREAM_ENFORCED_AUDIBLE);
                setStopMode(mp, 1);
                mp->start();
               //usleep(10*1000);
               mp->stop();
           }
        }
    }
    // release mediaplayer
    if(mp != NULL) {
        mp->reset();
        mp->disconnect();
    }
}

bool BootAnimation::android(bool stopMp, sp<MediaPlayer> mp)
{
    initTexture(&mAndroid[0], mAssets, "images/android-logo-mask.png");
    initTexture(&mAndroid[1], mAssets, "images/android-logo-shine.png");

    bool fastplayExists = hasFastplay();
    bool isMceExited = false;

    //stop fastplay & hold its last frame
    //system("echo stoponly time 0 > /proc/msp/mce");
    if(!mFullyPlayFastplay) {  // stop fast play with static frame
        system("echo stoponly time 0 > /proc/msp/mce");
    }

    // clear screen

    glShadeModel(GL_FLAT);
    glDisable(GL_DITHER);
    glDisable(GL_SCISSOR_TEST);
    glClearColor(0,0,0,1);
    glClear(GL_COLOR_BUFFER_BIT);
#if 0
    eglSwapBuffers(mDisplay, mSurface);
#endif
    property_set(SWAP_PROP_NAME, "1");//surfaceflinger swap enable

    glEnable(GL_TEXTURE_2D);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

    const GLint xc = (mWidth  - mAndroid[0].w) / 2;
    const GLint yc = (mHeight - mAndroid[0].h) / 2;
    const Rect updateRect(xc, yc, xc + mAndroid[0].w, yc + mAndroid[0].h);

    glScissor(updateRect.left, mHeight - updateRect.bottom, updateRect.width(),
            updateRect.height());

    // Blend state
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

    const nsecs_t startTime = systemTime();
    bool isMpStopped = false;
    do {
        nsecs_t now = systemTime();
        double time = now - startTime;
        float t = 4.0f * float(time / us2ns(16667)) / mAndroid[1].w;
        GLint offset = (1 - (t - floorf(t))) * mAndroid[1].w;
        GLint x = xc - offset;

        glDisable(GL_SCISSOR_TEST);
        glClear(GL_COLOR_BUFFER_BIT);

        glEnable(GL_SCISSOR_TEST);
        glDisable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, mAndroid[1].name);
        glDrawTexiOES(x,                 yc, 0, mAndroid[1].w, mAndroid[1].h);
        glDrawTexiOES(x + mAndroid[1].w, yc, 0, mAndroid[1].w, mAndroid[1].h);

        glEnable(GL_BLEND);
        glBindTexture(GL_TEXTURE_2D, mAndroid[0].name);
        glDrawTexiOES(xc, yc, 0, mAndroid[0].w, mAndroid[0].h);

        EGLBoolean res = eglSwapBuffers(mDisplay, mSurface);

        if (!isMpStopped && stopMp && (mp != NULL)) {
            stopMediaPlayer(mp);
            isMpStopped = true;
        }

        // After showing the first frame, exit mce & clear 'video' to black
        if (!stopMp && fastplayExists && !isMceExited) {
            isMceExited = true;
            system("echo exit > /proc/msp/mce");
        }

        if (res == EGL_FALSE)
            break;

        // 12fps: don't animate too fast to preserve CPU
        const nsecs_t sleepTime = 83333 - ns2us(systemTime() - now);
        if (sleepTime > 0)
            usleep(sleepTime);

        checkExit();
    } while (!exitPending());

    glDeleteTextures(1, &mAndroid[0].name);
    glDeleteTextures(1, &mAndroid[1].name);
    return false;
}


bool BootAnimation::checkExit() {
    // Allow surface flinger to gracefully request shutdown
    char value[PROPERTY_VALUE_MAX];
    property_get(EXIT_PROP_NAME, value, "0");
    int exitnow = atoi(value);
    if (exitnow) {
        requestExit();
        return true;
    }

    return false;
}

bool BootAnimation::movie(bool stopMp, sp<MediaPlayer> mp)
{
    ZipFileRO& zip(mZip);
    bool isMpStopped = false;
    bool isMceExited = false;
    bool fastplayExists = hasFastplay();
    int CompressMethod;
    size_t CompressUncompLen;
    size_t CompressCompLen;
    char* s2;

    size_t numEntries = zip.getNumEntries();
    ZipEntryRO desc = zip.findEntryByName("desc.txt");
    zip.getEntryInfo(desc, &CompressMethod, &CompressUncompLen, &CompressCompLen, 0, 0, 0);
    char outBuf[CompressUncompLen];

    if(!mFullyPlayFastplay) {  // stop fast play with static frame
        system("echo stoponly time 0 > /proc/msp/mce");
    }

    FileMap* descMap = zip.createEntryFileMap(desc);
    ALOGE_IF(!descMap, "descMap is null");
    if (!descMap) {
        if (!isMpStopped && stopMp && (mp != NULL)) {
            stopMediaPlayer(mp);
            isMpStopped = true;
        }
        if (!stopMp && fastplayExists && !isMceExited) {
            isMceExited = true;
            system("echo exit > /proc/msp/mce");
        }
        property_set(SWAP_PROP_NAME, "1");//surfaceflinger swap enable
        return false;
    }

    String8 desString((char const*)descMap->getDataPtr(),
            descMap->getDataLength());
    char const* s = desString.string();

    if (CompressMethod == ZipFileRO::kCompressDeflated) {
        ALOGE("ENTERN ZipFileRO: kCompressDeflated");
        if (true == zip.uncompressEntry(desc,  outBuf)){
            ALOGE("uncompressEntry CompressUncompLen : %d\n",CompressUncompLen);
        }
        s2 = (char *)malloc(CompressUncompLen+1);
        memset(s2, 0, CompressUncompLen+1);
        memcpy(s2, outBuf, CompressUncompLen);
        ALOGE("s2 : %s\n",s2);
    }
    Animation animation;

    // Parse the description file
    //HISILICON add begin
    //ensure bootanim be compeletly played when configured
    bool bPlayCompltly = false;
    animationPlayMode playMode = eFreeze;
    {
        char buffer[PROPERTY_VALUE_MAX] = {0};
        property_get("persist.sys.bootanim.compeleted", buffer, "false");
        if(strcmp(buffer,"true") == 0) {
            bPlayCompltly = true;
        }

        memset(buffer, 0x0, sizeof(buffer));
        property_get("persist.sys.bootanim.playmode", buffer, "freeze");
        if(strcmp(buffer, "black") == 0) {
            playMode = eBlack;
        } else if(strcmp(buffer, "loop") == 0) {
            playMode = eLooply;
        } else {
            playMode = eFreeze;
        }

        ALOGD("movie: playcompletly=%s, playMode=%d",
                        bPlayCompltly?"true":"false", playMode);
    }
    //HISILICON add end

    if (CompressMethod == ZipFileRO::kCompressStored) {
    for (;;) {
        const char* endl = strstr(s, "\n");
        if (!endl) break;
        String8 line(s, endl - s);
        const char* l = line.string();
        int fps, width, height, count, pause;
        char path[256];
        char pathType;
        if (sscanf(l, "%d %d %d", &width, &height, &fps) == 3) {
            /*begin:add by zhanghk at 20200408:enable 1280*720 bootanimation.zip can play full-screen when DPI240*/
            char province[128];
            property_get("ro.ysten.province", province, "master");
            if(!strcmp(province, "cm201_zhejiang")){
                 char dpi[128];
                 property_get("ro.sf.lcd_density", dpi, "160");
                 if(!strcmp("240",dpi)){
                     if(width < 1920)
                          width = 1920;
                     if(height < 1080)
                          height = 1080; 				  
                 }
             }
             /*end:add by zhanghk at 20200408:enable 1280*720 bootanimation.zip can play full-screen when DPI240*/
            //LOGD("> w=%d, h=%d, fps=%d", width, height, fps);
            animation.width = width;
            animation.height = height;
            // HISILICON add begin
            // the position of image shoudle be center
            // ensure the position be integer, otherwise, the (mWidth-width) shoudle be even number
            if((mWidth % 2) != (width % 2)) {
                animation.width = width + 1;
            }
            if((mHeight % 2) != (height % 2)) {
                animation.height = height + 1;
            }

            char isfullscreen[PROPERTY_VALUE_MAX] = {0};
            property_get("bootanim.pic.isfullscreen", isfullscreen, "false");
            if(0 == strcmp("true",isfullscreen)){
                animation.height = mHeight;
                animation.width = mWidth;
            }

            // HISILICON add end
            animation.fps = fps;
        }
        else if (sscanf(l, " %c %d %d %s", &pathType, &count, &pause, path) == 4) {
            //LOGD("> type=%c, count=%d, pause=%d, path=%s", pathType, count, pause, path);
            Animation::Part part;
            part.playUntilComplete = pathType == 'c';
            //HISILICON add begin
            //ensure bootanim be compeletly played when configured
            if( bPlayCompltly){
                part.playUntilComplete = true;
            } else {
                part.playUntilComplete = false;
            }
            //HISILICON add end
            part.count = count;
            part.pause = pause;
            part.path = path;
            animation.parts.add(part);
        }

            s = ++endl;
        }
    }else if (CompressMethod == ZipFileRO::kCompressDeflated) {
        for (;;) {
            char* endl2 = strstr(s2, "\n");
            if (!endl2) break;
            String8 line(s2, endl2 - s2);
            const char* l = line.string();
            int fps, width, height, count, pause;
            char path[256];
            char pathType;
            if (sscanf(l, "%d %d %d", &width, &height, &fps) == 3) {
                //LOGD("> w=%d, h=%d, fps=%d", width, height, fps);
                animation.width = width;
                animation.height = height;
                // HISILICON add begin
                // the position of image shoudle be center
                // ensure the position be integer, otherwise, the (mWidth-width) shoudle be even number
                if((mWidth % 2) != (width % 2)) {
                    animation.width = width + 1;
                }
                if((mHeight % 2) != (height % 2)) {
                    animation.height = height + 1;
                }

                char isfullscreen[PROPERTY_VALUE_MAX] = {0};
                property_get("bootanim.pic.isfullscreen", isfullscreen, "false");
                if(0 == strcmp("true",isfullscreen)){
                    animation.height = mHeight;
                    animation.width = mWidth;
                }

                // HISILICON add end
                animation.fps = fps;
            }
            else if (sscanf(l, " %c %d %d %s", &pathType, &count, &pause, path) == 4) {
                //LOGD("> type=%c, count=%d, pause=%d, path=%s", pathType, count, pause, path);
                Animation::Part part;
                part.playUntilComplete = pathType == 'c';
                //HISILICON add begin
                //ensure bootanim be compeletly played when configured
                if( bPlayCompltly){
                    part.playUntilComplete = true;
                } else {
                    part.playUntilComplete = false;
                }
                //HISILICON add end
                part.count = count;
                part.pause = pause;
                part.path = path;
                animation.parts.add(part);
            }

            s2 = ++endl2;
        }
    }
    // read all the data structures
    const size_t pcount = animation.parts.size();
    for (size_t i=0 ; i<numEntries ; i++) {
        char name[256];
        ZipEntryRO entry = zip.findEntryByIndex(i);
        if (zip.getEntryFileName(entry, name, 256) == 0) {
            const String8 entryName(name);
            const String8 path(entryName.getPathDir());
            const String8 leaf(entryName.getPathLeaf());
            if (leaf.size() > 0) {
                for (int j=0 ; j<pcount ; j++) {
                    if (path == animation.parts[j].path) {
                        int method;
                        size_t UncompLen2;
                        size_t CompLen2;
                        // supports only stored png files
                        if (zip.getEntryInfo(entry, &method, &UncompLen2, &CompLen2, 0, 0, 0)) {
                            if (method == ZipFileRO::kCompressStored) {
                                FileMap* map = zip.createEntryFileMap(entry);
                                if (map) {
                                    Animation::Frame frame;
                                    frame.name = leaf;
                                    frame.map = map;
                                    Animation::Part& part(animation.parts.editItemAt(j));
                                    part.frames.add(frame);
                                }
                            } else if (method == ZipFileRO::kCompressDeflated) {
                                ALOGE("This is CompressDeflated entry\n");
                                char outBuf2[UncompLen2];
                                if (true == zip.uncompressEntry(entry, outBuf2)) {
                                     Animation::Frame frame;
                                     frame.name = leaf;
                                     frame.pbuf = (char*)malloc(UncompLen2);
                                     memcpy(frame.pbuf , outBuf2, UncompLen2);
                                     frame.psize = UncompLen2;
                                     Animation::Part& part(animation.parts.editItemAt(j));
                                     part.frames.add(frame);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    property_set(SWAP_PROP_NAME, "1");//surfaceflinger swap enable

    glClearColor(0,0,0,1);
    glBindTexture(GL_TEXTURE_2D, 0);
    glEnable(GL_TEXTURE_2D);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

    playAnimation(animation, stopMp, mp);
    char myProvince[128];
    property_get("ro.ysten.province", myProvince, "master");
    switch(playMode) {
        case eLooply:
        { // play animation looply untile boot finished
            while(!exitPending()) {
                playAnimation(animation, stopMp, mp);
            }
        }
        break;
        case eBlack:
            // exit bootanimation
            break;
        case eFreeze:
        default:
        { // freeze last image untile boot finished
            float MAX_FPS = 60.0f;
            float CHECK_DELAY = ns2us(s2ns(1) / MAX_FPS);
            while(!exitPending()) {
                if(!strcmp(myProvince, "cm201_shaanxi")|| !strcmp(myProvince, "cm201_jiangxi")){
                    property_set("sys.ysten.bootanim.exit","1");
                }
                ALOGD("check exit, wait");
                usleep(CHECK_DELAY);
                checkExit();
            }
	    checkForReportIptvError();
        }
        break;
    }

    return false;
}

bool BootAnimation::playAnimation(const Animation& animation, bool stopMp, sp<MediaPlayer> mp)
{
    ZipFileRO& zip(mZip);
    bool isMpStopped = false;
    bool isMceExited = false;
    bool fastplayExists = hasFastplay();
    int method;
    size_t UncompLen;
    size_t CompLen;
    ZipEntryRO desc = zip.findEntryByName("desc.txt");
    zip.getEntryInfo(desc, &method, &UncompLen, &CompLen, 0, 0, 0);

    const int xc = (mWidth - animation.width) / 2;
    const int yc = ((mHeight - animation.height) / 2);
    nsecs_t lastFrame = systemTime();
    nsecs_t frameDuration = s2ns(1) / animation.fps;

    char buffer[PROPERTY_VALUE_MAX] = {0};
    nsecs_t frame_time = 0;
    property_get("persist.sys.bootanim.frametime", buffer, "");
    if(buffer[0] != '\0') {  // frametime unit is ms
        int value = atoi(buffer);
        if(value <= 0) {
            ALOGD("invalid frametime");
        } else {
            frame_time = ms2ns(value);
        }
    }
    frameDuration = (frameDuration > frame_time && frame_time > 0)?frame_time:frameDuration;
    ALOGD("frame time is %lld ms", ns2ms(frameDuration));

    Region clearReg(Rect(mWidth, mHeight));
    clearReg.subtractSelf(Rect(xc, yc, xc+animation.width, yc+animation.height));

    const size_t pcount = animation.parts.size();
    for (int i=0 ; i<pcount ; i++) {
        const Animation::Part& part(animation.parts[i]);
        const size_t fcount = part.frames.size();
        glBindTexture(GL_TEXTURE_2D, 0);
        for (int r=0 ; (!part.count && r == part.count) || r<part.count ; r++) {
            // Exit any non playuntil complete parts immediately
            if(exitPending() && !part.playUntilComplete)
                break;

            for (int j=0 ; j<fcount && (!exitPending() || part.playUntilComplete) ; j++) {
                const Animation::Frame& frame(part.frames[j]);
                nsecs_t lastFrame = systemTime();

                if (r > 0) {
                    glBindTexture(GL_TEXTURE_2D, frame.tid);
                } else {
                    if (part.count != 1) {
                        glGenTextures(1, &frame.tid);
                        glBindTexture(GL_TEXTURE_2D, frame.tid);
                        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                    }
                    if (method == ZipFileRO::kCompressStored) {
                        initTexture(
                                frame.map->getDataPtr(),
                                frame.map->getDataLength());
                    } else if (method == ZipFileRO::kCompressDeflated) {
                        initTexture(frame.pbuf, frame.psize);
                    }
                }

                if (!clearReg.isEmpty()) {
                    Region::const_iterator head(clearReg.begin());
                    Region::const_iterator tail(clearReg.end());
                    glEnable(GL_SCISSOR_TEST);
                    while (head != tail) {
                        const Rect& r(*head++);
                        glScissor(r.left, mHeight - r.bottom,
                                r.width(), r.height());
                        glClear(GL_COLOR_BUFFER_BIT);
                    }
                    glDisable(GL_SCISSOR_TEST);
                }
                glDrawTexiOES(xc, yc, 0, animation.width, animation.height);
                eglSwapBuffers(mDisplay, mSurface);

                if (!isMpStopped && stopMp && (mp != NULL)) {
                    stopMediaPlayer(mp);
                    isMpStopped = true;
                }

                // After showing the first frame, exit mce & clear 'video' to black
                if (!stopMp && fastplayExists && !isMceExited) {
                    isMceExited = true;
                    system("echo exit > /proc/msp/mce");
                }

                nsecs_t now = systemTime();
                nsecs_t delay = frameDuration - (now - lastFrame);
                //ALOGD("%lld, %lld", ns2ms(now - lastFrame), ns2ms(delay));
                lastFrame = now;

                if (delay > 0) {
                    struct timespec spec;
                    spec.tv_sec  = (now + delay) / 1000000000;
                    spec.tv_nsec = (now + delay) % 1000000000;
                    int err;
                    do {
                        err = clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &spec, NULL);
                    } while (err<0 && errno == EINTR);
                }

                checkExit();
            }

            // hisilicon modify begin
            // check exit every 20ms
            int loopTimes = part.pause * ns2us(frameDuration)/20000;
            for(int i = 0 ;i<loopTimes;i++){
                checkExit();
                usleep(20000);
                if(exitPending()&& !part.playUntilComplete){
                    break;
                }
            }
            // hisilicon modify end

            // For infinite parts, we've now played them at least once, so perhaps exit
            //if(exitPending() && !part.count)
            //    break;
        }

        if (!isMpStopped && stopMp && (mp != NULL)) {
            stopMediaPlayer(mp);
            isMpStopped = true;
        }

        if (!stopMp && fastplayExists && !isMceExited) {
            isMceExited = true;
            system("echo exit > /proc/msp/mce");
        }

        // free the textures for this part
        if (part.count != 1) {
            for (int j=0 ; j<fcount ; j++) {
                const Animation::Frame& frame(part.frames[j]);
                glDeleteTextures(1, &frame.tid);
                if (method == ZipFileRO::kCompressDeflated && frame.pbuf != NULL) {
                     free(frame.pbuf);
                }
            }
        }
    }

    return false;
}
status_t BootAnimation::clearScreen(bool swap)
{
    // clear screen with tranparent. without this, eglSwapBuffer does not work.
    glShadeModel(GL_FLAT);
    glDisable(GL_DITHER);
    glDisable(GL_SCISSOR_TEST);
    glDisable(GL_BLEND);
    glClearColor(0,0,0,0);
    glClear(GL_COLOR_BUFFER_BIT);
    if(swap){
        eglSwapBuffers(mDisplay, mSurface);//make surfaceflinger swap works. without this. hide hifb0 does not work
    }
    return NO_ERROR;
}

status_t BootAnimation::showVideo(const char *url,const KeyedVector<String8, String8> *headers,
                                  int fd, int64_t offset, int64_t length)
{
   if(mPlayer != NULL && mListner != NULL) {
        mPlayer->reset();
        mPlayer.clear();
        mListner.clear();
    }
   // create video layer
    if(mVideoSurface == NULL) {
        mVideoControl.clear();
        mVideoControl =  session()->createSurface(String8("BootVideoLayer"),
            mWidth, mHeight, PIXEL_FORMAT_RGBA_8888);
        if(mVideoControl == NULL) {
            ALOGE("create surface(BootVideoLayer) failed");
            return false;
        }
        SurfaceComposerClient::openGlobalTransaction();
        mVideoControl->setLayer(0x3fffffff);
        SurfaceComposerClient::closeGlobalTransaction();
        mVideoSurface = mVideoControl->getSurface();
    }

    sp<MediaPlayer> mp = new MediaPlayer();
    sp<MediaListner> listener = new MediaListner();
    mp->setListener(listener);
    mp->reset();
    if(url != NULL)
    {
        mp->setDataSource(url, headers);
    }
    else
    {
        mp->setDataSource(fd,offset, length);
    }

    mp->setLooping(false);
    mp->setAudioStreamType(AUDIO_STREAM_ENFORCED_AUDIBLE);
    mp->setVideoSurfaceTexture(mVideoSurface->getIGraphicBufferProducer());
    setStopMode(mp, 0);
    if(mp->prepare() != NO_ERROR) {
        ALOGE("mediaplayer prepare failed");
        if(mp!=NULL){
            mp->reset();
            mp->disconnect();
        }
        return UNKNOWN_ERROR;
    }
    bool isTrack = false;
    if(setTrackOnly(mp,1)){
        isTrack = true;
    }

    if(mp->start() != NO_ERROR) {
        ALOGE("mediaplayer strart return error");
        if(mp!=NULL){
            mp->reset();
            mp->disconnect();
        }
        return UNKNOWN_ERROR;
    }
    if(isTrack){
        usleep(75 * 1000);//sleep 75ms
        //setOutputVolume(gSystemVolume);
        setOutputVolume(0);
    }
    mPlayer = mp;
    mListner = listener;
    int loopTemp = 0;
    while (!mListner->isFirstFrame)
    {
        loopTemp++;
        if (loopTemp >= 10 || mListner->isPlayCompleted)
        {
            if(mp != NULL) {
                mp->reset();
                mp->disconnect();
            }
            return UNKNOWN_ERROR;
        }
        usleep(100 * 1000);
    }
    mListner->isFirstFrame = false;
    if(isTrack){
        setOutputVolume(gSystemVolume);
    }
    return NO_ERROR;
}

bool BootAnimation::playBootTs()
{

    const float MAX_FPS = 60.0f;
    const float CHECK_DELAY = ns2us(s2ns(1) / MAX_FPS);
    bool fastplayExists = hasFastplay();
    bool isMceExited = false;

    property_set(VIDEO_PROP_NAME, "0");

    waitForMediaService();
    if(!mFullyPlayFastplay) {  // stop fast play with static frame
        system("echo stoponly time 0 > /proc/msp/mce");
    }

    if(access(USER_BOOT_VIDEO, R_OK) == 0) {
        gVideoPath = USER_BOOT_VIDEO;
    } else if(access(USER_BOOT_VIDEO_DEFAULT, R_OK) == 0) {
        gVideoPath = USER_BOOT_VIDEO_DEFAULT;
    } else {
        ALOGE("no boot video");
    }

    if(gVideoPath != NULL) {
        showVideo(gVideoPath);

        if (fastplayExists && !isMceExited) {
            isMceExited = true;
            system("echo exit > /proc/msp/mce");
        }

        property_set(SWAP_PROP_NAME, "1");//surfaceflinger swap enable
        clearScreen();

        /*begin:add by zhanghk at 20181219:shaanxi boot max time is 30s*/
        char province[128];
        property_get("ro.ysten.province", province, "master");
        if(!strcmp(province, "cm201_shaanxi")){
                int duration = 0;
                int currentPosition = 0;
                mPlayer->getDuration(&duration);
                mPlayer->getCurrentPosition(&currentPosition);
                ALOGD("**duration %d**",duration);
                ALOGD("**currentPosition %d**",currentPosition);
                ALOGD("**CHECK_DELAY %d**",CHECK_DELAY);
                while ((!mListner->isPlayCompleted) && (currentPosition < 30000)) {
                        mPlayer->getCurrentPosition(&currentPosition);
                        usleep(CHECK_DELAY);
                }
        //start add by ysten chenfeng at 20190121 for jiangsu play boot video less than 60s
        } else if(strstr(province, "jiangsu")!=NULL) {
            int duration = 0;
            int currentPosition = 0;
            mPlayer->getDuration(&duration);
            mPlayer->getCurrentPosition(&currentPosition);
            ALOGD("**duration %d**",duration);
            ALOGD("**currentPosition %d**",currentPosition);
            ALOGD("**CHECK_DELAY %d**",CHECK_DELAY);

            while ((!mListner->isPlayCompleted) && (currentPosition < 60000)) {
                mPlayer->getCurrentPosition(&currentPosition);
                usleep(CHECK_DELAY);
            }
        //end add by ysten chenfeng at 20190121 for jiangsu play boot video less than 60s
        }else{
                while ((mListner != NULL) && !mListner->isPlayCompleted) {
                    usleep(CHECK_DELAY);
                }
        }
        /*end:add by zhanghk at 20181219:shaanxi boot max time is 30s*/
    }
    setOutputVolume(0);
    setTrackOnly(mPlayer,0);
    property_set(VIDEO_PROP_NAME, "1");

    char buffer[PROPERTY_VALUE_MAX] = {0};
    property_get("persist.sys.bootanim.enable", buffer, "true");
    /*begin:add by zhanghk at 20181224:not play bootanimation when after play video*/
    char province[128]; 
    property_get("ro.ysten.province", province, "master");
    if (!strcasecmp("false", buffer) || !strcmp(province, "cm201_shaanxi") || !strcmp(province, "cm201_xinjiang") || !strcmp(province, "cm201_hebei")) {
    /*end:add by zhanghk at 20181224:not play bootanimation when after play video*/
        //wait for terminated signal
        do {
            checkExit();
            usleep(CHECK_DELAY);
        } while (!exitPending());
        stopMediaPlayer(mPlayer,false);
    } else {
        stopMediaPlayer(mPlayer);
        //play bootanimation
        if (mAndroidAnimation) {
            android(true, mPlayer);
        } else {
            movie(true, mPlayer);
        }
    }


    return false;
}

bool BootAnimation::playBootZip()
{
    ALOGD("**start %s**",__FUNCTION__);
    ZipFileRO& zip(vZip);
    bool isMceExited = false;
    bool fastplayExists = hasFastplay();
    const float MAX_FPS = 60.0f;
    const float CHECK_DELAY = ns2us(s2ns(1) / MAX_FPS);
    Vector <VideoAnimationPart> parts;
    int fd;
    size_t numEntries = zip.getNumEntries();
    ZipEntryRO desc = zip.findEntryByName("desc.txt");
    FileMap* descMap = zip.createEntryFileMap(desc);
    ALOGE_IF(!descMap, "descMap is null");
    if (!descMap) {
        if (fastplayExists && !isMceExited) {
            if(!mFullyPlayFastplay) {  // stop fast play with static frame
                system("echo stoponly time 0 > /proc/msp/mce");
            }
            isMceExited = true;
            system("echo exit > /proc/msp/mce");
        }
        property_set(SWAP_PROP_NAME, "1");//surfaceflinger swap enable
        return false;
    }
    String8 desString((char const*)descMap->getDataPtr(),
            descMap->getDataLength());
    char const* s = desString.string();

    // Parse the description file
    ALOGD("***%s(%d):start Parse the description file*****",__FUNCTION__,__LINE__);
    char buffer[PROPERTY_VALUE_MAX] = {0};
    for (;strlen(s) > 0;) {
        const char* endl = strstr(s, "\n");
        const char* l = s;
        if (endl ) {
            String8 line(s, endl - s);
            l = line.string();
        }
        char path[256];
        char pathType;
      ALOGD("**l=%s***",l);
        if (sscanf(l, " %c %s", &pathType, path) == 2) {
            VideoAnimationPart part;
            part.path=path;
            parts.add(part);
        }
        s += strlen(l) + 1;
    }

   //creat videoAnimationparts
    ALOGD("***%s(%d):start creat videoAnimationparts*****",__FUNCTION__,__LINE__);
    const size_t pcount = parts.size();
    for (size_t i=0 ; i<numEntries ; i++) {
        char name[256];
        ZipEntryRO entry = zip.findEntryByIndex(i);
        if (zip.getEntryFileName(entry, name, 256) == 0) {
            const String8 entryName(name);
            const String8 path(entryName.getPathDir());
            const String8 leaf(entryName.getPathLeaf());
            if (leaf.size() > 0) {
                for (size_t j=0 ; j<pcount ; j++) {
                    if (path == parts[j].path) {
                        int method;
                        size_t lenth;
                        if (zip.getEntryInfo(entry, &method, 0, 0, 0, 0, 0)) {
                            if (method == ZipFileRO::kCompressStored) {
                                FileMap *map = zip.createEntryFileMap(entry);
                                if(map){
                                    VideoAnimationPart::Video video;
                                    video.path = leaf;
                                    ALOGD("***%s(%d):leaf=%s,j=%d,path=%s***",__FUNCTION__,__LINE__,
                                            leaf.string(),j,path.string());
                                    video.map = map;
                                    VideoAnimationPart & part(parts.editItemAt(j));
                                    part.videos.add(video);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
//init the env
    if((fd=open(USER_BOOTVIDEOANIMATION_FILE,O_RDONLY)) < 0){
        ALOGD("open bootVideoAnimation:%s",strerror(errno));
        return false;
    }
    property_set(VIDEO_PROP_NAME, "0");
    waitForMediaService();
    if(!mFullyPlayFastplay) {  // stop fast play with static frame
        system("echo stoponly time 0 > /proc/msp/mce");
    }

//start play
    ALOGD("***%s(%d):start play videoAnimationparts*****",__FUNCTION__,__LINE__);
    for(size_t i = 0; i<pcount;i++){
        const VideoAnimationPart &part(parts[i]);
        const size_t vcount = part.videos.size();
        for(size_t j = 0;j <vcount;j++){
            showVideo(NULL,NULL,fd,part.videos[j].map->getDataOffset(), part.videos[j].map->getDataLength());

            if (0 == i && 0 == j){
                if (fastplayExists && !isMceExited) {
                    isMceExited = true;
                    system("echo exit > /proc/msp/mce");
                }

                property_set(SWAP_PROP_NAME, "1");//surfaceflinger swap enable
                clearScreen();
            }
            while ((mListner != NULL) && !mListner->isPlayCompleted) {
                usleep(CHECK_DELAY);
            }
            ALOGD("***%s(%d):one video play completed (%s/%s)*",__FUNCTION__,__LINE__,part.path.string(),part.videos[j].path.string());
        }
    }
    setOutputVolume(0);
    setTrackOnly(mPlayer,0);
    property_set(VIDEO_PROP_NAME, "1");
    do {
        checkExit();
        usleep(CHECK_DELAY);
    } while (!exitPending());
    stopMediaPlayer(mPlayer, false);
    ALOGD("**stop %s**",__FUNCTION__);
    return false;
}

status_t BootAnimation::initTexture(Texture* texture, SkBitmap& bitmap)
{
    ALOGD("initTexture with bitmap");
    // ensure we can call getPixels(). No need to call unlock, since the
    // bitmap will go out of scope when we return from this method.
    bitmap.lockPixels();

    const int w = bitmap.width();
    const int h = bitmap.height();
    const void* p = bitmap.getPixels();

    GLint crop[4] = { 0, h, w, -h };
    texture->w = w;
    texture->h = h;

    glGenTextures(1, &texture->name);
    glBindTexture(GL_TEXTURE_2D, texture->name);

    switch (bitmap.getConfig()) {
        case SkBitmap::kA8_Config:
            glTexImage2D(GL_TEXTURE_2D, 0, GL_ALPHA, w, h, 0, GL_ALPHA,
                    GL_UNSIGNED_BYTE, p);

            break;
        case SkBitmap::kARGB_4444_Config:
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA,
                    GL_UNSIGNED_SHORT_4_4_4_4, p);
            break;
        case SkBitmap::kARGB_8888_Config:
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w, h, 0, GL_RGBA,
                    GL_UNSIGNED_BYTE, p);
            break;
        case SkBitmap::kRGB_565_Config:
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, w, h, 0, GL_RGB,
                    GL_UNSIGNED_SHORT_5_6_5, p);
            break;
        default:
            break;
    }

    glTexParameteriv(GL_TEXTURE_2D, GL_TEXTURE_CROP_RECT_OES, crop);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    return NO_ERROR;

}
bool getAssetBitmap(const char *file, SkBitmap* bitmap,AssetManager& assets){
    bool res;
    Asset* asset = assets.open(file, Asset::ACCESS_BUFFER);
    if (!asset){
        ALOGE("can not open file %s",file);
        return false;
    }
    res = SkImageDecoder::DecodeMemory(asset->getBuffer(false), asset->getLength(),
            bitmap, SkBitmap::kNo_Config, SkImageDecoder::kDecodePixels_Mode);
    asset->close();
    delete asset;
    return res;
}

void drawTextRect(SkCanvas* canvas,SkRect& rect,char* text){
    SkPaint paint;
    paint.setStyle(SkPaint::kFill_Style);
    // set typeface
    //SkTypeface* typeface = SkTypeface::CreateFromFile("/system/fonts/DroidSansFallback.ttf");
    static SkTypeface* typeface;
    if(typeface == NULL){
        typeface = SkTypeface::CreateFromFile("/system/fonts/SourceHanSansCN-Normal.otf");
    }
    if(typeface == NULL) {
        ALOGE("create typeface failed");
    } else {
        paint.setTypeface(typeface);
    }
    paint.setTextSize(28);
    paint.setColor(0xffffffff);
    paint.setTextAlign(SkPaint::kCenter_Align);
    SkPaint::FontMetrics fontMetrics;
    paint.getFontMetrics(&fontMetrics, 0);
    float distance=(fontMetrics.fBottom - fontMetrics.fTop)/2 - fontMetrics.fBottom;
    float baseline=rect.centerY()+distance;
    canvas->drawText(text, strlen(text),rect.centerX(), baseline, paint);

}

bool BootAnimation::showCfgPic(customAnimConfig& cfg)
{
    ALOGD("showPic customization");
    nsecs_t frame_time_begin = systemTime();
    SkFILEStream stream(cfg.filename.string());
    if(mDecodeData.fileName.isEmpty() || mDecodeData.fileName != cfg.filename) {
        mDecodeData.bitmap.reset();
        SkFILEStream stream(cfg.filename.string());
        SkImageDecoder::Format format = SkImageDecoder::GetStreamFormat(&stream);
        SkAutoTDelete<SkImageDecoder> decoder(SkImageDecoder::Factory(&stream));
        if (decoder.get() == 0) {
            ALOGE("couldn't decode %s", cfg.filename.string());
            return false;
        }
        bool ret = decoder->decode(&stream, &mDecodeData.bitmap, SkBitmap::kARGB_8888_Config,
                                     SkImageDecoder::kDecodePixels_Mode);
        if (!ret) {
            ALOGE("Fail to decode %s", cfg.filename.string());
            return false;
        }
        mDecodeData.fileName = cfg.filename;
    }

    ALOGI("w=%d, h=%d, bpp=%d, size=%d", mDecodeData.bitmap.width(), mDecodeData.bitmap.height(),
                 mDecodeData.bitmap.bytesPerPixel(), mDecodeData.bitmap.getSize());

    // position
    int w = mDecodeData.bitmap.width();
    int h = mDecodeData.bitmap.height();
    char isfullscreen[PROPERTY_VALUE_MAX] = {0};
    property_get("bootanim.pic.isfullscreen", isfullscreen, "false");
    if(0 == strcmp("true",isfullscreen)){
        w = mWidth;
        h = mHeight;
    }
    if (w > mWidth) w = mWidth;
    if (h > mHeight) h = mHeight;

    const int xc = (mWidth - w) / 2;
    const int yc = ((mHeight - h) / 2);
    ALOGD("position(%d, %d)", xc, yc);

    property_set(SWAP_PROP_NAME, "1");//surfaceflinger swap enable

    if(mNeedClearUI) {
        ALOGD("clear buffer");
        mNeedClearUI = false;
        glClearColor(0,0,0,255);
        glBindTexture(GL_TEXTURE_2D, 0);
        glEnable(GL_TEXTURE_2D);
        glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    }

    // clear region
    Region clearReg(Rect(mWidth, mHeight));
    //clearReg.subtractSelf(calculateCountDownRegion());
    clearReg.subtractSelf(Rect(xc, yc, xc + w, yc + h));

    initTexture(&mTexture[0], mDecodeData.bitmap);

    // if not fullscreen, needs to clear region
    //glEnable(GL_SCISSOR_TEST);
    //glDisable(GL_BLEND);
    if (!clearReg.isEmpty()) {
        Region::const_iterator head(clearReg.begin());
        Region::const_iterator tail(clearReg.end());
        glEnable(GL_SCISSOR_TEST);
        while (head != tail) {
            const Rect& r(*head++);
            ALOGD("clear rect(%d, %d:%d, %d)", r.left, r.top, r.right, r.bottom);
            glScissor(r.left, mHeight - r.bottom,
                        r.width(), r.height());
            glClear(GL_COLOR_BUFFER_BIT);
        }
        glDisable(GL_SCISSOR_TEST);
    }
    glBindTexture(GL_TEXTURE_2D, mTexture[0].name);
    glDrawTexiOES(xc, yc, 0, w, h);

    // display UI after draw count down
    // display
    //eglSwapBuffers(mDisplay, mSurface);

    glDeleteTextures(1, &mTexture[0].name);

    return true;
}

bool BootAnimation::showCfgVideo(customAnimConfig& cfg)
{
    ALOGD("showCfgVideo customization");
    const float MAX_FPS = 60.0f;
    const float CHECK_DELAY = ns2us(s2ns(1) / MAX_FPS);
    bool fastplayExists = hasFastplay();
    bool isMceExited = false;
    int showRes;
    bool playSuccess= true;
    waitForMediaService();
    showRes = showVideo(cfg.filename.string());
    if(showRes != NO_ERROR){
         playSuccess = false;
    }
    property_set(SWAP_PROP_NAME, "1");//surfaceflinger swap enable
    clearScreen(false);
    mNeedClearUI = true;
    return playSuccess;
}

bool BootAnimation::showCountDown()
{
    ALOGD("showCountDown customization");

#if 0
    eglMakeCurrent(mDisplay, mSurface, mSurface, mContext);
    glClearColor(0,0,0,0);
    glBindTexture(GL_TEXTURE_2D, 0);
    glEnable(GL_TEXTURE_2D);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
#endif


    bool ret = drawCountDown(mPlayTime--);
    if(mPlayTime < 0) {
        mPlayTime = 0;
    }

    // display
    //eglSwapBuffers(mDisplay, mSurface);

    glDeleteTextures(1, &mTexture[1].name);

    //eglMakeCurrent(mDisplay, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);

    return ret;
}

// drawAdvTextAndBg will call every 1s wheher show
bool BootAnimation::drawMiguadDataCountDown(customAnimConfig& cfg)
{
    ALOGD("drawMiguadDataCountDown show %d %d mPlayTime ",cfg.isAdv,mPlayTime);
#if 0
    eglMakeCurrent(mDisplay, mSurface, mSurface, mContext);
    glClearColor(0,0,0,0);
    glBindTexture(GL_TEXTURE_2D, 0);
    glEnable(GL_TEXTURE_2D);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
#endif
    /* draw the content */
    bool ret = drawMiguadDataCountDownContent(mPlayTime,cfg);
    /*deducte the time*/
    mPlayTime--;
    if(mPlayTime < 0) {
        mPlayTime = 0;
    }
    glDeleteTextures(1, &mTexture[1].name);
    return ret;
}

Rect BootAnimation::calculateCountDownRegion()
{
    int width = 300;
    int height = 60;
    int x = mWidth - width;
    int y = mHeight - height;

    Rect rec(x, y ,x + width, y + height);

    return rec;
}

// draw the background and text
bool BootAnimation::drawMiguadDataCountDownContent(int time, customAnimConfig& cfg){
    ALOGD("drawMiguadDataCountDownContent time %d ",time);
    if(time < 0) {
        ALOGE("invalid time(%d)", time);
        return false;
    }
    GLint width = mWidth;
    GLint height = mHeight;

    /* init a bitmap the content will draw on the bitmap*/
    SkBitmap bitmap;
    bitmap.setConfig(SkBitmap::kARGB_8888_Config, width, height);
    static char * bitmap_data = new char[width*height*4];
    if(bitmap_data == NULL) {
        ALOGE("new buffer[%d] failed", width*height*4);
        return false;
    }
    memset(bitmap_data, 0x0, width*height*4);
    bitmap.setPixels((void*)bitmap_data);

    /* init the canvas and paint the text color is 0xffff00FF*/
    SkCanvas canvas(bitmap);

    SkPaint paint;
    paint.setStyle(SkPaint::kFill_Style);
    paint.setColor(SK_ColorYELLOW);
    paint.setStrokeWidth(12);
    paint.setColor(0xffff00FF);

    SkBitmap countdownBg;
    SkRect rectText;
    if (cfg.isAdv)
    {
	    /*draw the left advertisement backgroundbg*/
        getAssetBitmap("images/advert_bg.png", &countdownBg, mAssets);
        canvas.drawBitmap(countdownBg, 36, 45 , &paint);
        /*draw the left advertisement text*/
        SkRect rectText = SkRect::MakeXYWH(36,45,countdownBg.width(), countdownBg.height());
        drawTextRect(&canvas , rectText, "广告");
	}
	
    /*draw the right countdown background*/
    getAssetBitmap("images/countdown_bg.png", &countdownBg, mAssets);
    int x = width - countdownBg.width() - 85;
    canvas.drawBitmap(countdownBg, x, 46 , &paint);
    /*draw the right countdown time text*/
    char text_buf[64] = {0};
    memset(text_buf, 0x0, 64);
    snprintf(text_buf, 64, "%d", time);
    rectText = SkRect::MakeXYWH(x,46,countdownBg.width(), countdownBg.height());
    drawTextRect(&canvas , rectText, text_buf);

    //draw bitmap
    initTexture(&mTexture[1], bitmap);
    glEnable (GL_BLEND);
    glBlendFunc (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glBindTexture(GL_TEXTURE_2D, mTexture[1].name);
    glDrawTexiOES(0, 0, 0, mTexture[1].w, mTexture[1].h);
    bitmap.reset();
    return true;
}
bool BootAnimation::drawCountDown(int time)
{
    ALOGD("drawCountDown customization");
    if(time < 0) {
        ALOGE("invalid time(%d)", time);
        return false;
    }

    char text_buf[64] = {0};
    ssize_t text_len = 0;
    int text_size = 32;

    // calculate bounds
    GLint width = 300;
    GLint height = 60;
    GLint x = mWidth - width;
    GLint y =  mHeight - height;

    SkBitmap textBitmap;
    //textBitmap.allocN32Pixels(text_size*4, (text_size*3)>>1);
    textBitmap.setConfig(SkBitmap::kARGB_8888_Config, width, height);
    char * bitmap_data = new char[width*height*4];
    if(bitmap_data == NULL) {
        ALOGE("new buffer[%d] failed", width*height*4);
        return false;
    }
    memset(bitmap_data, 0x0, width*height*4);
    for(int i = 0; i < height; i++) {
        for(int j = 0; j < width; j++) {
            bitmap_data[j*4] = 0xff;
        }
    }
    //textBitmap.allocPixels();
    textBitmap.setPixels((void*)bitmap_data);

    SkPaint paint;
    paint.setColor(SkColorSetARGBInline(0xFF, 0xF1, 0xF1, 0xF1));
    paint.setTextSize(SkIntToScalar(text_size));
    // set typeface
    //SkTypeface* typeface = SkTypeface::CreateFromName("DroidSansFallback", SkTypeface::kNormal);
    SkTypeface* typeface = SkTypeface::CreateFromFile("/system/fonts/DroidSansFallback.ttf");
    if(typeface == NULL) {
        ALOGE("create typeface failed");
    } else {
        SkSafeUnref(paint.setTypeface(typeface));
        SkString name;
        typeface->getFamilyName(&name);
        ALOGD("type face name:%s", name.writable_str());
    }

    /*
     * 中文字显示颜色 #F1F1F1
     * 数字显示颜色 #FFC600
    */
    int posX = 20;
    int posY = 50;
    // phase 1
{
    memset(text_buf, 0x0, 64);
    //begin:add by zhanghk:modify anhui migu advertisement tips
    char province[128];
    property_get("ro.ysten.province", province, "master");
    if(strstr(province, "anhui") != NULL){
        snprintf(text_buf, 64, "节目准备中...");
    }else{
        snprintf(text_buf, 64, "广告还剩余");
    }
    //end:add by zhanghk:modify anhui migu advertisement tips
    
    ALOGD("text_buf=%s", text_buf);
    text_len = strlen(text_buf);
    paint.setColor(SkColorSetARGBInline(0xFF, 0xF1, 0xF1, 0xF1));
    SkCanvas textCanvas(textBitmap);
    textCanvas.drawText(text_buf, text_len, posX, posY, paint);
    textCanvas.save();
    posX += 5 * text_size;
}

{
    //begin:add by zhanghk:modify anhui migu advertisement tips
    char province[128];
    property_get("ro.ysten.province", province, "master");
    if(strstr(province, "anhui") == NULL){
        // phase 2
        memset(text_buf, 0x0, 64);
        snprintf(text_buf, 64, "%d", time);
        ALOGD("text_buf=%s", text_buf);
        text_len = strlen(text_buf);
        paint.setColor(SkColorSetARGBInline(0xFF, 0xFF, 0xC6, 0x00));
        SkCanvas textCanvas(textBitmap);
        //textCanvas.drawText(text_buf, text_len, text_size*2, text_size, paint);
        textCanvas.drawText(text_buf, text_len, posX, posY, paint);
        textCanvas.save();
        posX += text_size;
    }
    //end:add by zhanghk:modify anhui migu advertisement tips
}

{
    //begin:add by zhanghk:modify anhui migu advertisement tips
    char province[128];
    property_get("ro.ysten.province", province, "master");
    if(strstr(province, "anhui") == NULL){
        // phase 3
        memset(text_buf, 0x0, 64);
        snprintf(text_buf, 64, "秒");
        ALOGD("text_buf=%s", text_buf);
        text_len = strlen(text_buf);
        paint.setColor(SkColorSetARGBInline(0xFF, 0xF1, 0xF1, 0xF1));
        SkCanvas textCanvas(textBitmap);
        //textCanvas.drawText(text_buf, text_len, text_size*2, text_size, paint);
        textCanvas.drawText(text_buf, text_len, posX, posY, paint);
        textCanvas.save();
    }
    //end:add by zhanghk:modify anhui migu advertisement tips
}

    initTexture(&mTexture[1], textBitmap);

    glEnable (GL_BLEND);
    //glBlendFunc( GL_ZERO , GL_ONE);
    glBlendFunc (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

    //glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
    glBindTexture(GL_TEXTURE_2D, mTexture[1].name);
    ALOGD("text posion(%d,%d:%d,%d)", x, y, x+mTexture[1].w, y+mTexture[1].h);
    glDrawTexiOES(x, y, 0, mTexture[1].w, mTexture[1].h);

    //clear textCanvas
    //textCanvas.clear(SK_ColorTRANSPARENT);
    textBitmap.reset();

    return true;
}


bool BootAnimation::hasCustomization()
{
#ifdef PROVINCE_TYPE_CM201_JX
    char launchMode[PROPERTY_VALUE_MAX] = {0};
    property_get("persist.sys.launcher.value", launchMode, "-1");
    if (!strcmp("2", launchMode)){
        return false;
    }
#endif
    if(access(BOOTANIM_CUSTOMIZATION_CFG, R_OK) == 0) {
        return true;
    }
    if(access(BOOTANIM_CUSTOMIZATION_CFG_DEFAULT, R_OK) == 0) {
        return true;
    }
    if(access(BOOTANIM_CUSTOMIZATION_CFG_JSON, R_OK) == 0) {
        return true;
    }
    if(access(BOOTANIM_CUSTOMIZATION_CFG_JSON_DEFAULT, R_OK) == 0) {
        return true;
    }
	
	if(access(BOOTANIM_CUSTOMIZATION_ZJ_CFG, R_OK) == 0){
		return true;
	}

    return false;
}
void BootAnimation::checkForReportIptvError()
{
	//begin:add by ysten guxin for jiangxin iptv bootanimation and logo error 
	char province[PROPERTY_VALUE_MAX];
	char launcher_value[PROPERTY_VALUE_MAX];
	property_get("ro.ysten.province", province, "master");
    property_get("persist.sys.launcher.value", launcher_value, "0");//0default 1ott 2 iptv
	ALOGE("province = %s launcher_value =%s",province ,launcher_value);
    if ((strcmp("cm201_jiangxi", province) == 0)&& (strcmp("2", launcher_value) == 0)){
		char logo_staus[PROPERTY_VALUE_MAX];
		property_get("persist.sys.yst.updatelogo",logo_staus, "0");//1 error
		ALOGE("logo_staus = %s ",logo_staus);
		if(!(access(USER_BOOTANIMATION_FILE, R_OK) == 0) &&
		(mZip.open(USER_BOOTANIMATION_FILE) == NO_ERROR)){
			//animation error
			if ((strcmp("1", logo_staus) == 0)){
				system("am broadcast -a android.intent.action.BOOTPIC_SHOW_FAIL --es errorCode 3");
				property_set("persist.sys.yst.updatelogo","0");
			}else {
			    system("am broadcast -a android.intent.action.BOOTPIC_SHOW_FAIL --es errorCode 2");	
			}
		}else {
			if ((strcmp("1", logo_staus) == 0)){
				system("am broadcast -a android.intent.action.BOOTPIC_SHOW_FAIL --es errorCode 1");	
				property_set("persist.sys.yst.updatelogo","0");
			}
		} 
       
	}
	//end:dd by ysten guxin for jiangxin iptv bootanimation and logo error 
}

bool getConfigFileType(Json::Value& obj, char &type ){
    bool res = true;
    Json::Value  defaultValue("");
    Json::Value value;
    value = obj.get("kind", defaultValue);
    if(value.asString().compare("p") == 0) {
        type = 'p';
    }else if(value.asString().compare("v") == 0) {
        type = 'v';
    }else {
        res = false;
    }
    ALOGD("getConfigFileType %c res %d ", type, res);
    return res;
}
bool getConfigFileName(Json::Value& obj, const char* media_path, String8& filename){
    bool res = false;
    Json::Value  defaultValue("");
    Json::Value value;
    value = obj.get("filename", defaultValue);
    if(value.asString().compare("") != 0) {
        filename = String8(media_path) + String8(value.asCString());
        // check permission
        if(access(filename, R_OK) != 0) {
            ALOGE("getConfigFileName can not access %s", filename.string());
        }else{
            res = true;
        }
    }
    return res;
}
bool getConfigFileFmt(Json::Value& obj, char type, String8& fmt) {
    bool res = false;
    ALOGD("getConfigFileFmt %s ", obj.asCString());
    Json::Value  defaultValue("");
    Json::Value value;
    value = obj.get("type",defaultValue);
    if(value.asString().compare("") == 0){
        return false;
    }
    fmt = String8(value.asString().c_str());
    ALOGD("file format is %s", value.asString().c_str());
    static char* support_p_fmt[] = {
        "jpg", "png", "bmp",  NULL
    };
    static char* support_v_fmt[] = {
        "mp4", "ts", NULL
    };
    char** fmt_str = NULL;
    if(type == 'p') {
        fmt_str = support_p_fmt;
    } else if(type == 'v') {
        fmt_str = support_v_fmt;
    }
    ALOGD("file format is %d", (unsigned int)fmt_str);
    for(int i = 0; fmt_str[i] != NULL; i++) {
        if(strcmp(fmt, fmt_str[i]) == 0) {
            res = true;
            break;
        }
    }
    return res;
}
bool getConfigDurion(Json::Value& obj, unsigned int& duration){
    Json::Value defaultvValue = "";
    Json::Value value;
    value = obj.get("duration",defaultvValue);
    duration = atoi(value.asCString());
    return (duration > 0);
}
bool getConfigIsAdv(Json::Value& obj, bool& isAdv){
    Json::Value defaultvValue = "";
    Json::Value value;
    ALOGD("getConfigIsAdv %s ", obj.asCString());
    value = obj.get("marker", defaultvValue);
    if(!value.asString().compare("广告")){
        isAdv = true;
    }else{
        isAdv = false;
    }
    ALOGD("getConfigIsAdv sleep 1 ");
    return true;
}
bool checkFileSize(String8& filename){
    FILE *file = fopen(filename,"r");
    if(NULL == file){
        fclose(file);
        return false;
    }
    fseek(file,0,SEEK_END);
    long size = ftell(file);
    fclose(file);
    ALOGD("file size = %ld",size);
    if(size == 0){
        return false;
    }
    return true;
}
bool BootAnimation::loadCustomizedConfigJson(const char *config_name,const char* media_path){
        ALOGD("loadCustomizedConfigJson %s filename",config_name);
        std::ifstream ifs(config_name);
        Json::Reader reader;
        Json::Value obj;
        //reader.parse(ifs, obj); // reader can also read strings
        if (!reader.parse(ifs, obj)) {
            ALOGD("loadCustomizedConfigJson %s json parse failed",config_name);
            return false;
        }
        int totalTime = 0;
        Json::Value  defaultValue("");
        if(obj.isArray()){
            for (int i = 0; i < obj.size(); i++){
                BootAnimation::customAnimConfig config;
                Json::Value value;
                //get kind -> type
                value = obj[i].get("kind", defaultValue);
                if( getConfigFileType(obj[i], config.type) &&
                    getConfigFileName(obj[i], media_path, config.filename) &&
                    getConfigFileFmt(obj[i], config.type, config.format) &&
                    getConfigDurion(obj[i], config.duration) &&
                    getConfigIsAdv(obj[i],config.isAdv)){

                    if(!checkFileSize(config.filename))
                        continue;

                    mCustomAnimCfgs.add(config);
                    totalTime +=  config.duration;
                    ALOGD("AnimConfig type: %c filename: %s format: %s duration: %d isAdv: %d totalTime: %d",
                        config.type,config.filename.string(),config.format.string(),
                        config.duration, config.isAdv,totalTime);
                }
            }
        }
   // fclose(config_txt);
    mPlayTime = totalTime;
    return true;
}

bool BootAnimation::loadCustomizedConfig(const char *config_name,const char* media_path)
{
    FILE *config_txt = NULL;
    int totalTime = 0;
    #define MAX_LINE_LEN (256)

    config_txt = fopen(config_name, "r");
    if(NULL == config_txt) {
        ALOGE("open %s failed", config_name);
        return false;
    }

    // parse config file
    {
        /*
        * 1. media type: one char
        p:picture; v:video
        2. file name: string
        3. play time: integer
        4. file fomat: string
        e.g.:jpg/png/bmp/mp4/ts
        */
        char buf[MAX_LINE_LEN];
        memset(buf, 0x0, MAX_LINE_LEN);
        while(fgets(buf, MAX_LINE_LEN, config_txt))
        {
            int len = strlen(buf);
            customAnimConfig config;
            char type;
            int time = 0;
            String8 name;
            char fmt[16] = {0};

            // media type
            type = buf[0];
            if((type != 'p' && type != 'v') || (buf[1] != ',')) {
                memset(buf, 0x0, MAX_LINE_LEN);
                ALOGE("bad media type, next..");
                continue;
            }
            // file name
            char* next = buf + 2;
            char* p = strchr(buf + 2, ',');
            if(p == NULL) {
                memset(buf, 0x0, MAX_LINE_LEN);
                ALOGE("bad config, can not read fime name, next..");
                continue;
            }
            size_t l = p - next;
            name = String8(next, l);

            // play time & file format
            next = p + 1;
            if(sscanf(next, "%d,%s", &time, fmt) != 2) {
                memset(buf, 0x0, MAX_LINE_LEN);
                ALOGE("config(%s) is not standardization, next..", buf);
                continue;
            }

            ALOGD("file format is '%s'", fmt);
            static char* support_p_fmt[] = {
                "jpg", "png", "bmp",  NULL
            };
            static char* support_v_fmt[] = {
                "mp4", "ts", NULL
            };
            char** fmt_str = NULL;
            if(type == 'p')  {
                fmt_str = support_p_fmt;
            } else if(type == 'v') {
                fmt_str = support_v_fmt;
            }
            ALOGD("file extension=%s", name.getPathExtension().string());
            int i = 0;
            for(i = 0; fmt_str[i] != NULL; i++) {
                String8 ext = String8(".") + String8(fmt_str[i]);
                if((strcmp(fmt, fmt_str[i]) == 0) && (name.getPathExtension() == ext)) {
                    break;
                }
            }
            if(fmt_str[i] == NULL) {
                ALOGE("invalid format(%s), or file(%s)", fmt, name.string());
                continue;
            }

            if(time <= 0){
                ALOGE("invalid time <= 0");
                continue;
            }
            config.type = type;
            config.duration = time;
            config.format = fmt;
            config.filename = String8(media_path) + name;
            
            if(!checkFileSize(config.filename))
                continue;

            // check permission
            if(access(config.filename.string(), R_OK) != 0) {
                ALOGE("can not access '%s', next..", config.filename.string());
                continue;
            }
            mCustomAnimCfgs.add(config);
            totalTime += time;
            ALOGD("read config:type=%c, name=%s, play time=%d, format=%s", type, name.string(), time, fmt);

            memset(buf, 0x0, MAX_LINE_LEN);
        }
    }
    fclose(config_txt);
    mPlayTime = totalTime;
    return true;
}

bool BootAnimation::playCustomizedAnimation(bool useDefault, bool checkBoot)
{
    mNeedClearUI = false;
    bool isMiguadData = false;
    ALOGD("playCustomizedAnimation");
    char * config_name = NULL;
    char * configjson_name = NULL;
    char * media_path = NULL;
    bool bNeedShowCountDown = true;
    nsecs_t frame_time = s2ns(1);
    if(!useDefault) {
        if(access(BOOTANIM_CUSTOMIZATION_CFG_JSON, R_OK) == 0) {
            configjson_name = BOOTANIM_CUSTOMIZATION_CFG_JSON;
            media_path = BOOTANIM_CUSTOMIZATION_PATH;
        }else if(access(BOOTANIM_CUSTOMIZATION_ZJ_CFG, R_OK) == 0){//added by yzs for zhejiang
		    configjson_name = BOOTANIM_CUSTOMIZATION_ZJ_CFG;
            media_path = BOOTANIM_CUSTOMIZATION_ZJ_PATH;
        } else if(access(BOOTANIM_CUSTOMIZATION_CFG, R_OK) == 0) {
            config_name = BOOTANIM_CUSTOMIZATION_CFG;
            media_path = BOOTANIM_CUSTOMIZATION_PATH;
        } else {
            ALOGE("can not access '%s' and '%s'", BOOTANIM_CUSTOMIZATION_CFG, BOOTANIM_CUSTOMIZATION_CFG_DEFAULT);
            return false;
        }
    }else if(access(BOOTANIM_CUSTOMIZATION_ZJ_CFG, R_OK) == 0){//added by yzs for zhejiang
		config_name = BOOTANIM_CUSTOMIZATION_ZJ_CFG;
        media_path = BOOTANIM_CUSTOMIZATION_ZJ_PATH;
        bNeedShowCountDown = true;		
	}else if(access(BOOTANIM_CUSTOMIZATION_CFG_JSON_DEFAULT, R_OK) == 0) {
        configjson_name = BOOTANIM_CUSTOMIZATION_CFG_JSON_DEFAULT;
        media_path = BOOTANIM_CUSTOMIZATION_PATH_DEFAULT;
        bNeedShowCountDown = false;
    }else if(access(BOOTANIM_CUSTOMIZATION_CFG_DEFAULT, R_OK) == 0) {
        config_name = BOOTANIM_CUSTOMIZATION_CFG_DEFAULT;
        media_path = BOOTANIM_CUSTOMIZATION_PATH_DEFAULT;
        bNeedShowCountDown = false;
    } else {
        ALOGE("no config file");
        return false;
    }
    if(configjson_name != NULL){
        if(loadCustomizedConfigJson(configjson_name,media_path)){
            isMiguadData = true;
        }
    }
    if(!isMiguadData){
        if (!loadCustomizedConfig(config_name,media_path)){
            ALOGE("load config file failed");
            return false;
        }
    }
    if(mCustomAnimCfgs.size() == 0) {
        if(!useDefault && !checkBoot) {
            // if no config in '/data/local', play default
            return playCustomizedAnimation(true/*default*/);
        }
        // else
        ALOGE("no config");
        return false;
    }
    char buffer[PROPERTY_VALUE_MAX] = {0};
    property_get("persist.sys.bootanim.frametime", buffer, "");
    if(buffer[0] != '\0'){
        int value = atoi(buffer);
        if(value > 0 && value < 1000){
            frame_time = ms2ns(value);
            bNeedShowCountDown = false;
        }
    }
    // play animation
    //property_set(SWAP_PROP_NAME, "1");//surfaceflinger swap enable
    bool needWaitLauncher = false;
    if(checkBoot) { // start launcher no block
        property_set(VIDEO_PROP_NAME, "1");
    } else {
        property_set(VIDEO_PROP_NAME, "0");
        needWaitLauncher = true;
    }

    // clear UI buffer
    glClearColor(0,0,0,255);
    glBindTexture(GL_TEXTURE_2D, 0);
    glEnable(GL_TEXTURE_2D);
    glTexEnvx(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameterx(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

    const size_t count = mCustomAnimCfgs.size();
    bool bShowned = false;
    int index = 0;
    int timePassed = 0;
    int playtime = mPlayTime;
    bool clearVideoLayer = false;
    while(playtime && (index < count)) {
       timePassed++;
       bool hasNoError = true;
       //ALOGD("play time = %d, index=%d, count=%d, time passed =%d", playtime, index, count, timePassed);
       customAnimConfig cfg(mCustomAnimCfgs[index]);
       if(cfg.type == 'p') {
            clearVideoLayer = true;
            hasNoError = showCfgPic(cfg);
       } else if(cfg.type == 'v') {
            clearVideoLayer = false;
            if( timePassed == 1) { // first time
                hasNoError = showCfgVideo(cfg);
                if(!bNeedShowCountDown){
                    eglSwapBuffers(mDisplay, mSurface);//showCfgVideo do not swap buffer
                }
            }
        }

        if(!hasNoError) {
            ALOGD("has error ocurred, next..");
            index++;
            timePassed = 0;
            mPlayTime -= cfg.duration;
            playtime -= cfg.duration;
            continue;
        }
        bShowned |= hasNoError;
        if(cfg.duration <= timePassed) {
            // the next..
            timePassed = 0;
           index++;
       }

       nsecs_t showTiming = systemTime(CLOCK_MONOTONIC);
	   //added by yzs for fujian migu bootanimation begin
	   char province[128];
       property_get("ro.ysten.province", province, "master");
       if(!strcmp(province, "m302h_fujian")||!strcmp(province, "cm201_fujian")||strstr(province, "anhui")!=NULL){             
	   	    isMiguadData = false;
        }
		if(!strcmp(province, "cm201_zhejiang")){             
	   	    isMiguadData = true;
        }
		//end	 	
        //drawCountDown(playtime);
        if(bNeedShowCountDown) {
            if(isMiguadData){
                drawMiguadDataCountDown(cfg);
            }else{
                showCountDown();
            }
        }
        // display
        if(bNeedShowCountDown || (cfg.type == 'p')) {
            // swap buffer for display UI
           // only the UI(not video) has be update, do it
           eglSwapBuffers(mDisplay, mSurface);
       }

       if(clearVideoLayer) {
           if(mPlayer != NULL && mListner != NULL) {
               setOutputVolume(0);
               setTrackOnly(mPlayer,0);
               stopMediaPlayer(mPlayer);
               mPlayer.clear();
               mListner.clear();
               if(mVideoControl != NULL) {
                   mVideoControl.clear();
               }
               if(mVideoSurface != NULL) {
                   mVideoSurface.clear();
               }
          }

       }

       if(checkBoot && checkExit()) {
           break;
        }

        nsecs_t now = systemTime(CLOCK_MONOTONIC);
        nsecs_t delayTime = frame_time - (now - showTiming);
        if(cfg.type == 'v'){
            delayTime = s2ns(1) - (now - showTiming);
        }
        if(delayTime > 0) {
            struct timespec spec;
            spec.tv_sec  = (now + delayTime) / 1000000000;
            spec.tv_nsec = (now + delayTime) % 1000000000;
            int err;
            do {
                err = clock_nanosleep(CLOCK_MONOTONIC, TIMER_ABSTIME, &spec, NULL);
           } while (err<0 && errno == EINTR);
       } else {
           ALOGE("time out 1s");
       }

       playtime--;
   }

    if(bNeedShowCountDown && (playtime == 0)){
         customAnimConfig cfg(mCustomAnimCfgs[count - 1]);
         if(cfg.type == 'p'){
             showCfgPic(cfg);
         }
         if(isMiguadData){
            drawMiguadDataCountDown(cfg);
         }else{
             showCountDown();
         }

         eglSwapBuffers(mDisplay, mSurface);
    }

    // play animation done
    if(mPlayer != NULL && mListner != NULL) {
        setOutputVolume(0);
        setTrackOnly(mPlayer,0);
        stopMediaPlayer(mPlayer, false);
        mPlayer.clear();
        mListner.clear();
    }

    property_set(VIDEO_PROP_NAME, "1");

    mCustomAnimCfgs.clear();

    if(!bShowned) {
        ALOGE("not be showned");
        return false;
    }

    // start laucher after play animation done
    // avoid enter default bootanimation, need to wait launcher be started
    //ALOGD("wait for launcher 1s");
    if(needWaitLauncher) { // the launcher may be blocked, wait it for 1s
        float MAX_FPS = 60.0f;
        float CHECK_DELAY = ns2us(s2ns(1) / MAX_FPS);
        float waitTime = 0.0;
        while(!checkExit() && (waitTime < ns2us(s2ns(1))) ) {
            usleep(CHECK_DELAY);
            waitTime += CHECK_DELAY;
        }
    }
    //ALOGD("launcher started");

    // wait boot finish if needed
    if(checkBoot) {
        float MAX_FPS = 60.0f;
        float CHECK_DELAY = ns2us(s2ns(1) / MAX_FPS);
        while(!checkExit()) {
            ALOGD("check exit, wait");
            usleep(CHECK_DELAY);
        }
    }

    return true;
}

// ---------------------------------------------------------------------------

}
; // namespace android
