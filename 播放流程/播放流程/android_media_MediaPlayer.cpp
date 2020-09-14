/*
**
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

//#define LOG_NDEBUG 0
#define LOG_TAG "MediaPlayer-JNI"
#include "utils/Log.h"

#include <media/mediaplayer.h>
#include <media/MediaPlayerInterface.h>
#include <stdio.h>
#include <assert.h>
#include <limits.h>
#include <unistd.h>
#include <fcntl.h>
#include <utils/threads.h>
#include "jni.h"
#include "JNIHelp.h"
#include "android_runtime/AndroidRuntime.h"
#include "android_runtime/android_view_Surface.h"
#include "android_runtime/Log.h"
#include "utils/Errors.h"  // for status_t
#include "utils/KeyedVector.h"
#include "utils/String8.h"
#include "android_media_Utils.h"

#include "android_os_Parcel.h"
#include "android_util_Binder.h"
#include <binder/Parcel.h>
#include <gui/IGraphicBufferProducer.h>
#include <gui/Surface.h>
#include <binder/IPCThreadState.h>
#include <binder/IServiceManager.h>

#include <ISystemWriteService.h>

#include <core/SkBitmap.h>

#include <fcntl.h>
// ----------------------------------------------------------------------------

using namespace android;

// ----------------------------------------------------------------------------

struct fields_t {
    jfieldID    context;
    jfieldID    surface_texture;

    jmethodID   post_event;
    jmethodID   print_log;

    jmethodID   proxyConfigGetHost;
    jmethodID   proxyConfigGetPort;
    jmethodID   proxyConfigGetExclusionList;
    int disp_last_frame;
    jclass bitmapClazz;  // Must be a global ref
    jfieldID nativeBitmap;
    jmethodID createBitmapMethod;
    jclass configClazz;  // Must be a global ref
    jmethodID createConfigMethod;
};
static fields_t fields;

static Mutex sLock;

sp<IMemory> bitmapMemory;
int bitmapW;
int bitmapH;
int bitmapSize;
static int current_time_log;

//-----------------
/*enum print_log_event_type {
    PRINT_LOG_SETPLAYER_SOURCE= 300,
    PRINT_LOG_CREATE_PLAYER = 301,
    PRINT_LOG_SET_VIDEOSURFACETEXTURE = 302,
    PRINT_LOG_PREPAREASYNC = 303,
    PRINT_LOG_PLAYMODE = 304,
    PRINT_LOG_FRAME_SIZE = 305,
    PRINT_LOG_CODEC_INFO = 306,
    PRINT_LOG_START_DECODER = 307,
    PRINT_LOG_PREPARED = 308,
    PRINT_LOG_START = 309,
    PRINT_LOG_FIRST_FRMAE_SHOWN = 310,
    PRINT_LOG_PAUSE = 311,
    PRINT_LOG_SEEKTO = 312,
    PRINT_LOG_SEEK_COMPLETE = 313,
    PRINT_LOG_STOP = 314,
    PRINT_LOG_DESTORY_PLAYER = 315,
    PRINT_LOG_CONNECT_SERVER = 316,
    PRINT_LOG_STATUS_CODE = 317,
    PRINT_LOG_REDIRECT_URL = 318,
    PRINT_LOG_TS_DOWNLOAD_BAGIN = 319,
    PRINT_LOG_TS_DOWNLOAD_COMPLETE = 320,
    PRINT_LOG_CHANNEL_SWITCH = 321,
    PRINT_LOG_BUFFER_START = 322,
    PRINT_LOG_BUFFER_FINISH = 323,
    PRINT_LOG_CURRENT_PLAYTIME = 324,
    PRINT_LOG_ERROR = 325,
    PRINT_LOG_pLAY_END = 326,
};*/

//------------------
// ----------------------------------------------------------------------------
// ref-counted object for callbacks
class JNIMediaPlayerListener: public MediaPlayerListener
{
public:
    JNIMediaPlayerListener(JNIEnv* env, jobject thiz, jobject weak_thiz);
    ~JNIMediaPlayerListener();
    virtual void notify(int msg, int ext1, int ext2, const Parcel *obj = NULL);
private:
    JNIMediaPlayerListener();
    jclass      mClass;     // Reference to MediaPlayer class
    jobject     mObject;    // Weak ref to MediaPlayer Java object to call on
};

JNIMediaPlayerListener::JNIMediaPlayerListener(JNIEnv* env, jobject thiz, jobject weak_thiz)
{

    // Hold onto the MediaPlayer class for use in calling the static method
    // that posts events to the application thread.
    jclass clazz = env->GetObjectClass(thiz);
    if (clazz == NULL) {
        ALOGE("Can't find android/media/MediaPlayer");
        jniThrowException(env, "java/lang/Exception", NULL);
        return;
    }
    mClass = (jclass)env->NewGlobalRef(clazz);

    // We use a weak reference so the MediaPlayer object can be garbage collected.
    // The reference is only used as a proxy for callbacks.
    mObject  = env->NewGlobalRef(weak_thiz);
}

JNIMediaPlayerListener::~JNIMediaPlayerListener()
{
    // remove global references
    JNIEnv *env = AndroidRuntime::getJNIEnv();
    env->DeleteGlobalRef(mObject);
    env->DeleteGlobalRef(mClass);
}

void JNIMediaPlayerListener::notify(int msg, int ext1, int ext2, const Parcel *obj)
{
    JNIEnv *env = AndroidRuntime::getJNIEnv();
    if (obj && obj->dataSize() > 0) {
        jobject jParcel = createJavaParcelObject(env);
        if (jParcel != NULL) {
            Parcel* nativeParcel = parcelForJavaObject(env, jParcel);

            if (MEDIA_SUBTITLE_DATA == msg && obj != NULL) {
                obj->setDataPosition(4*sizeof(int));
                bitmapW = obj->readInt32();
                bitmapH = obj->readInt32();
                bitmapSize = obj->readInt32();
                bitmapMemory = interface_cast<IMemory>(obj->readStrongBinder());
                ALOGE("bitmapW:%d,bitmapH:%d,bitmapSize:%d\n", bitmapW, bitmapH, bitmapSize);
            }
            nativeParcel->setData(obj->data(), obj->dataSize());
            env->CallStaticVoidMethod(mClass, fields.post_event, mObject,
                    msg, ext1, ext2, jParcel);
			env->DeleteLocalRef(jParcel);
        }
    } else {
        env->CallStaticVoidMethod(mClass, fields.post_event, mObject,
                msg, ext1, ext2, NULL);
    }
    if (env->ExceptionCheck()) {
        ALOGW("An exception occurred while notifying an event.");
        LOGW_EX(env);
        env->ExceptionClear();
    }
}

// ----------------------------------------------------------------------------

static sp<MediaPlayer> getMediaPlayer(JNIEnv* env, jobject thiz)
{
    Mutex::Autolock l(sLock);
    MediaPlayer* const p = (MediaPlayer*)env->GetIntField(thiz, fields.context);
    return sp<MediaPlayer>(p);
}

static sp<MediaPlayer> setMediaPlayer(JNIEnv* env, jobject thiz, const sp<MediaPlayer>& player)
{
    Mutex::Autolock l(sLock);
    sp<MediaPlayer> old = (MediaPlayer*)env->GetIntField(thiz, fields.context);
    if (player.get()) {
        player->incStrong((void*)setMediaPlayer);
    }
    if (old != 0) {
        old->decStrong((void*)setMediaPlayer);
    }
    env->SetIntField(thiz, fields.context, (int)player.get());
    return old;
}

// If exception is NULL and opStatus is not OK, this method sends an error
// event to the client application; otherwise, if exception is not NULL and
// opStatus is not OK, this method throws the given exception to the client
// application.
static void process_media_player_call(JNIEnv *env, jobject thiz, status_t opStatus, const char* exception, const char *message)
{
    if (exception == NULL) {  // Don't throw exception. Instead, send an event.
        if (opStatus != (status_t) OK) {
            sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
            if (mp != 0) mp->notify(MEDIA_ERROR, opStatus, 0);
        }
    } else {  // Throw exception!
        if ( opStatus == (status_t) INVALID_OPERATION ) {
            jniThrowException(env, "java/lang/IllegalStateException", NULL);
        } else if ( opStatus == (status_t) PERMISSION_DENIED ) {
            jniThrowException(env, "java/lang/SecurityException", NULL);
        } else if ( opStatus != (status_t) OK ) {
            if (strlen(message) > 230) {
               // if the message is too long, don't bother displaying the status code
               jniThrowException( env, exception, message);
            } else {
               char msg[256];
                // append the status code to the message
               sprintf(msg, "%s: status=0x%X", message, opStatus);
               jniThrowException( env, exception, msg);
            }
        }
    }
}

static void
android_media_MediaPlayer_setDataSourceAndHeaders(
        JNIEnv *env, jobject thiz, jstring path,
        jobjectArray keys, jobjectArray values) {

    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }

    if (path == NULL) {
        jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
        return;
    }

    const char *tmp = env->GetStringUTFChars(path, NULL);
    if (tmp == NULL) {  // Out of memory
        return;
    }
    ALOGV("setDataSource: path %s", tmp);

    String8 pathStr(tmp);
    env->ReleaseStringUTFChars(path, tmp);
    tmp = NULL;

    // We build a KeyedVector out of the key and val arrays
    KeyedVector<String8, String8> headersVector;
    if (!ConvertKeyValueArraysToKeyedVector(
            env, keys, values, &headersVector)) {
        return;
    }

    status_t opStatus =
        mp->setDataSource(
                pathStr,
                headersVector.size() > 0? &headersVector : NULL);

    process_media_player_call(
            env, thiz, opStatus, "java/io/IOException",
            "setDataSource failed." );
}

static void
android_media_MediaPlayer_setDataSourceFD(JNIEnv *env, jobject thiz, jobject fileDescriptor, jlong offset, jlong length)
{
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }

    if (fileDescriptor == NULL) {
        jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
        return;
    }
    int fd = jniGetFDFromFileDescriptor(env, fileDescriptor);
    ALOGV("setDataSourceFD: fd %d", fd);
    process_media_player_call( env, thiz, mp->setDataSource(fd, offset, length), NULL, "setDataSourceFD failed." );
}

static sp<IGraphicBufferProducer>
getVideoSurfaceTexture(JNIEnv* env, jobject thiz) {
    IGraphicBufferProducer * const p = (IGraphicBufferProducer*)env->GetIntField(thiz, fields.surface_texture);
    return sp<IGraphicBufferProducer>(p);
}

static void
decVideoSurfaceRef(JNIEnv *env, jobject thiz)
{
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        return;
    }

    sp<IGraphicBufferProducer> old_st = getVideoSurfaceTexture(env, thiz);
    if (old_st != NULL) {
        old_st->decStrong((void*)decVideoSurfaceRef);
    }
}

static void
setVideoSurface(JNIEnv *env, jobject thiz, jobject jsurface, jboolean mediaPlayerMustBeAlive)
{
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL) {
        if (mediaPlayerMustBeAlive) {
            jniThrowException(env, "java/lang/IllegalStateException", NULL);
        }
        return;
    }

    decVideoSurfaceRef(env, thiz);

    sp<IGraphicBufferProducer> new_st;
    if (jsurface) {
        sp<Surface> surface(android_view_Surface_getSurface(env, jsurface));
        if (surface != NULL) {
            new_st = surface->getIGraphicBufferProducer();
            if (new_st == NULL) {
                jniThrowException(env, "java/lang/IllegalArgumentException",
                    "The surface does not have a binding SurfaceTexture!");
                return;
            }
            new_st->incStrong((void*)decVideoSurfaceRef);
        } else {
            jniThrowException(env, "java/lang/IllegalArgumentException",
                    "The surface has been released");
            return;
        }
    }

    env->SetIntField(thiz, fields.surface_texture, (int)new_st.get());

    //add for save player's log in file
    env->CallVoidMethod(thiz,fields.print_log,PRINT_LOG_SET_VIDEOSURFACETEXTURE,0);

    // This will fail if the media player has not been initialized yet. This
    // can be the case if setDisplay() on MediaPlayer.java has been called
    // before setDataSource(). The redundant call to setVideoSurfaceTexture()
    // in prepare/prepareAsync covers for this case.
    mp->setVideoSurfaceTexture(new_st);
}

static void
android_media_MediaPlayer_setVideoSurface(JNIEnv *env, jobject thiz, jobject jsurface)
{
    setVideoSurface(env, thiz, jsurface, true /* mediaPlayerMustBeAlive */);
}

static void
android_media_MediaPlayer_prepare(JNIEnv *env, jobject thiz)
{
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    
    //add for save player's log in file
    env->CallVoidMethod(thiz,fields.print_log,PRINT_LOG_SET_VIDEOSURFACETEXTURE,0);

    // Handle the case where the display surface was set before the mp was
    // initialized. We try again to make it stick.
    sp<IGraphicBufferProducer> st = getVideoSurfaceTexture(env, thiz);
    mp->setVideoSurfaceTexture(st);
    current_time_log=0;
    process_media_player_call( env, thiz, mp->prepare(), "java/io/IOException", "Prepare failed." );
}

static void
android_media_MediaPlayer_prepareAsync(JNIEnv *env, jobject thiz)
{
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    //add for save player's log in file
    env->CallVoidMethod(thiz,fields.print_log,PRINT_LOG_SET_VIDEOSURFACETEXTURE,0);

    // Handle the case where the display surface was set before the mp was
    // initialized. We try again to make it stick.
    sp<IGraphicBufferProducer> st = getVideoSurfaceTexture(env, thiz);
    mp->setVideoSurfaceTexture(st);
    
    //add for save player's log in file
    env->CallVoidMethod(thiz, fields.print_log, PRINT_LOG_PREPAREASYNC, 0);
    current_time_log=0;
    process_media_player_call( env, thiz, mp->prepareAsync(), "java/io/IOException", "Prepare Async failed." );
}

static void
android_media_MediaPlayer_start(JNIEnv *env, jobject thiz)
{
    ALOGV("start");
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    
    if(fields.disp_last_frame == 1) {
        jobject jParcel = createJavaParcelObject(env);
        if (jParcel != NULL) {
            ALOGV("set last frmae, create jParcel");
            Parcel* nativeParcel = parcelForJavaObject(env, jParcel);
            nativeParcel->writeInt32(1);
            mp->setParameter(KEY_PARAMETER_AML_PLAYER_SET_DISP_LASTFRAME, *nativeParcel);
        } else {
            ALOGV("set last frmae, jParcel is null!");
        }
        fields.disp_last_frame = 0;
    }
    
    process_media_player_call( env, thiz, mp->start(), NULL, NULL );
}

static void
android_media_MediaPlayer_stop(JNIEnv *env, jobject thiz)
{
    ALOGV("stop");
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    process_media_player_call( env, thiz, mp->stop(), NULL, NULL );
}

static void
android_media_MediaPlayer_pause(JNIEnv *env, jobject thiz)
{
    ALOGV("pause");
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }

    current_time_log=0;
    process_media_player_call( env, thiz, mp->pause(), NULL, NULL );
}

static jboolean
android_media_MediaPlayer_isPlaying(JNIEnv *env, jobject thiz)
{
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return false;
    }
    const jboolean is_playing = mp->isPlaying();

    ALOGV("isPlaying: %d", is_playing);
    return is_playing;
}

static void
android_media_MediaPlayer_seekTo(JNIEnv *env, jobject thiz, int msec)
{
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    ALOGV("seekTo: %d(msec)", msec);
    current_time_log=0;
    env->CallVoidMethod(thiz,fields.print_log,PRINT_LOG_SEEKTO ,msec);
    process_media_player_call( env, thiz, mp->seekTo(msec), NULL, NULL );
}

static int
android_media_MediaPlayer_getVideoWidth(JNIEnv *env, jobject thiz)
{
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }
    int w;
    if (0 != mp->getVideoWidth(&w)) {
        ALOGE("getVideoWidth failed");
        w = 0;
    }
    ALOGV("getVideoWidth: %d", w);
    return w;
}

static int
android_media_MediaPlayer_getVideoHeight(JNIEnv *env, jobject thiz)
{
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }
    int h;
    if (0 != mp->getVideoHeight(&h)) {
        ALOGE("getVideoHeight failed");
        h = 0;
    }
    ALOGV("getVideoHeight: %d", h);
    return h;
}


static int
android_media_MediaPlayer_getCurrentPosition(JNIEnv *env, jobject thiz)
{
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }
    int msec;
    process_media_player_call( env, thiz, mp->getCurrentPosition(&msec), NULL, NULL );
    ALOGV("getCurrentPosition: %d (msec)", msec);
    if(msec/1000-current_time_log/1000>=1 || current_time_log/1000-msec/1000>=1 || current_time_log<=0){
        env->CallVoidMethod(thiz,fields.print_log,PRINT_LOG_CURRENT_PLAYTIME ,msec);
        current_time_log = 	msec;	
    }
    return msec;
}

static int
android_media_MediaPlayer_getDuration(JNIEnv *env, jobject thiz)
{
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }
    int msec;
    process_media_player_call( env, thiz, mp->getDuration(&msec), NULL, NULL );
    ALOGV("getDuration: %d (msec)", msec);
    return msec;
}

static void
android_media_MediaPlayer_reset(JNIEnv *env, jobject thiz)
{
    ALOGV("reset");
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    process_media_player_call( env, thiz, mp->reset(), NULL, NULL );
}

static void
android_media_MediaPlayer_setAudioStreamType(JNIEnv *env, jobject thiz, int streamtype)
{
    ALOGV("setAudioStreamType: %d", streamtype);
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    process_media_player_call( env, thiz, mp->setAudioStreamType((audio_stream_type_t) streamtype) , NULL, NULL );
}

static void
android_media_MediaPlayer_setLooping(JNIEnv *env, jobject thiz, jboolean looping)
{
    ALOGV("setLooping: %d", looping);
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    process_media_player_call( env, thiz, mp->setLooping(looping), NULL, NULL );
}

static jboolean
android_media_MediaPlayer_isLooping(JNIEnv *env, jobject thiz)
{
    ALOGV("isLooping");
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return false;
    }
    return mp->isLooping();
}

static void
android_media_MediaPlayer_setVolume(JNIEnv *env, jobject thiz, float leftVolume, float rightVolume)
{
    ALOGV("setVolume: left %f  right %f", leftVolume, rightVolume);
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    process_media_player_call( env, thiz, mp->setVolume(leftVolume, rightVolume), NULL, NULL );
}

// Sends the request and reply parcels to the media player via the
// binder interface.
static jint
android_media_MediaPlayer_invoke(JNIEnv *env, jobject thiz,
                                 jobject java_request, jobject java_reply)
{
    sp<MediaPlayer> media_player = getMediaPlayer(env, thiz);
    if (media_player == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return UNKNOWN_ERROR;
    }

    Parcel *request = parcelForJavaObject(env, java_request);
    Parcel *reply = parcelForJavaObject(env, java_reply);

    // Don't use process_media_player_call which use the async loop to
    // report errors, instead returns the status.
    return media_player->invoke(*request, reply);
}

// Sends the new filter to the client.
static jint
android_media_MediaPlayer_setMetadataFilter(JNIEnv *env, jobject thiz, jobject request)
{
    sp<MediaPlayer> media_player = getMediaPlayer(env, thiz);
    if (media_player == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return UNKNOWN_ERROR;
    }

    Parcel *filter = parcelForJavaObject(env, request);

    if (filter == NULL ) {
        jniThrowException(env, "java/lang/RuntimeException", "Filter is null");
        return UNKNOWN_ERROR;
    }

    return media_player->setMetadataFilter(*filter);
}

static jboolean
android_media_MediaPlayer_getMetadata(JNIEnv *env, jobject thiz, jboolean update_only,
                                      jboolean apply_filter, jobject reply)
{
    sp<MediaPlayer> media_player = getMediaPlayer(env, thiz);
    if (media_player == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return false;
    }

    Parcel *metadata = parcelForJavaObject(env, reply);

    if (metadata == NULL ) {
        jniThrowException(env, "java/lang/RuntimeException", "Reply parcel is null");
        return false;
    }

    metadata->freeData();
    // On return metadata is positioned at the beginning of the
    // metadata. Note however that the parcel actually starts with the
    // return code so you should not rewind the parcel using
    // setDataPosition(0).
    return media_player->getMetadata(update_only, apply_filter, metadata) == OK;
}

// This function gets some field IDs, which in turn causes class initialization.
// It is called from a static block in MediaPlayer, which won't run until the
// first time an instance of this class is used.
static void
android_media_MediaPlayer_native_init(JNIEnv *env)
{
    jclass clazz;

    clazz = env->FindClass("android/media/MediaPlayer");
    if (clazz == NULL) {
        return;
    }

    fields.context = env->GetFieldID(clazz, "mNativeContext", "I");
    if (fields.context == NULL) {
        return;
    }

    // wxl add for bitmap subtitle transfer
    jclass bitmapClazz = env->FindClass("android/graphics/Bitmap");
    if (bitmapClazz == NULL) {
        return;
    }
    fields.bitmapClazz = (jclass) env->NewGlobalRef(bitmapClazz);
    if (fields.bitmapClazz == NULL) {
        return;
    }
    fields.createBitmapMethod =
            env->GetStaticMethodID(fields.bitmapClazz, "createBitmap",
                    "(IILandroid/graphics/Bitmap$Config;)"
                    "Landroid/graphics/Bitmap;");
    if (fields.createBitmapMethod == NULL) {
        return;
    }

    fields.nativeBitmap = env->GetFieldID(fields.bitmapClazz, "mNativeBitmap", "I");
    if (fields.nativeBitmap == NULL) {
        return;
    }

    jclass configClazz = env->FindClass("android/graphics/Bitmap$Config");
    if (configClazz == NULL) {
        return;
    }
    fields.configClazz = (jclass) env->NewGlobalRef(configClazz);
    if (fields.configClazz == NULL) {
        return;
    }
    fields.createConfigMethod =
            env->GetStaticMethodID(fields.configClazz, "nativeToConfig",
                    "(I)Landroid/graphics/Bitmap$Config;");
    if (fields.createConfigMethod == NULL) {
        return;
    }
    //----end add-------------------------------------------------------------
    fields.post_event = env->GetStaticMethodID(clazz, "postEventFromNative",
                                               "(Ljava/lang/Object;IIILjava/lang/Object;)V");
    if (fields.post_event == NULL) {
        return;
    }

    fields.print_log = env->GetMethodID(clazz, "printLogInfoFromNative","(II)V");
    if (fields.print_log == NULL) {
        return;
    }

    fields.surface_texture = env->GetFieldID(clazz, "mNativeSurfaceTexture", "I");
    if (fields.surface_texture == NULL) {
        return;
    }

    clazz = env->FindClass("android/net/ProxyProperties");
    if (clazz == NULL) {
        return;
    }

    fields.proxyConfigGetHost =
        env->GetMethodID(clazz, "getHost", "()Ljava/lang/String;");

    fields.proxyConfigGetPort =
        env->GetMethodID(clazz, "getPort", "()I");

    fields.proxyConfigGetExclusionList =
        env->GetMethodID(clazz, "getExclusionList", "()Ljava/lang/String;");
        
    fields.disp_last_frame = 0;
}

static void
android_media_MediaPlayer_native_setup(JNIEnv *env, jobject thiz, jobject weak_this)
{
    ALOGV("native_setup");
    sp<MediaPlayer> mp = new MediaPlayer();
    if (mp == NULL) {
        jniThrowException(env, "java/lang/RuntimeException", "Out of memory");
        return;
    }

    // create new listener and give it to MediaPlayer
    sp<JNIMediaPlayerListener> listener = new JNIMediaPlayerListener(env, thiz, weak_this);
    mp->setListener(listener);

    // Stow our new C++ MediaPlayer in an opaque field in the Java object.
    setMediaPlayer(env, thiz, mp);
}

static void
android_media_MediaPlayer_release(JNIEnv *env, jobject thiz)
{
    ALOGV("release");
    decVideoSurfaceRef(env, thiz);
    sp<MediaPlayer> mp = setMediaPlayer(env, thiz, 0);
    if (mp != NULL) {
        // this prevents native callbacks after the object is released
        mp->setListener(0);
        mp->disconnect();
    }
}

static void
android_media_MediaPlayer_native_finalize(JNIEnv *env, jobject thiz)
{
    ALOGV("native_finalize");
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp != NULL) {
        ALOGW("MediaPlayer finalized without being released");
    }
    android_media_MediaPlayer_release(env, thiz);
}

static void android_media_MediaPlayer_set_audio_session_id(JNIEnv *env,  jobject thiz, jint sessionId) {
    ALOGV("set_session_id(): %d", sessionId);
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    process_media_player_call( env, thiz, mp->setAudioSessionId(sessionId), NULL, NULL );
}

static jint android_media_MediaPlayer_get_audio_session_id(JNIEnv *env,  jobject thiz) {
    ALOGV("get_session_id()");
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }

    return mp->getAudioSessionId();
}

static void
android_media_MediaPlayer_setAuxEffectSendLevel(JNIEnv *env, jobject thiz, jfloat level)
{
    ALOGV("setAuxEffectSendLevel: level %f", level);
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    process_media_player_call( env, thiz, mp->setAuxEffectSendLevel(level), NULL, NULL );
}

static void android_media_MediaPlayer_attachAuxEffect(JNIEnv *env,  jobject thiz, jint effectId) {
    ALOGV("attachAuxEffect(): %d", effectId);
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }
    process_media_player_call( env, thiz, mp->attachAuxEffect(effectId), NULL, NULL );
}

static jint
android_media_MediaPlayer_pullBatteryData(JNIEnv *env, jobject thiz, jobject java_reply)
{
    sp<IBinder> binder = defaultServiceManager()->getService(String16("media.player"));
    sp<IMediaPlayerService> service = interface_cast<IMediaPlayerService>(binder);
    if (service.get() == NULL) {
        jniThrowException(env, "java/lang/RuntimeException", "cannot get MediaPlayerService");
        return UNKNOWN_ERROR;
    }

    Parcel *reply = parcelForJavaObject(env, java_reply);

    return service->pullBatteryData(reply);
}

static void
android_media_MediaPlayer_setprop(JNIEnv *env, jobject thiz,
                                                jstring prop, jstring value) {
    sp<IServiceManager> sm = defaultServiceManager();
    if (sm == NULL) {
        ALOGE("Couldn't get default ServiceManager\n");
    }
    sp<ISystemWriteService> systemWriteService = interface_cast<ISystemWriteService>(sm->getService(String16("system_write")));
    if (systemWriteService == NULL) {
        ALOGE("Couldn't get connection to SystemWriteService\n");
        return;
    }
    char* props = const_cast< char* > (env->GetStringUTFChars(prop, NULL));
    char* values = const_cast< char* > (env->GetStringUTFChars(value, NULL));
    systemWriteService->setProperty(String16(props), String16(values));
    env->ReleaseStringUTFChars(prop, props);
    env->ReleaseStringUTFChars(value, values);
}
static void
android_media_MediaPlayer_sendDataReport(JNIEnv *env, jobject thiz,
                                                jstring buf, int len) {
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }

    char* bufs = const_cast< char* > (env->GetStringUTFChars(buf, NULL));
    status_t err = mp->sendDataReport(bufs, len);
    if (err == OK) {
        return;
    } else {
        return;
    }
    env->ReleaseStringUTFChars(buf, bufs);
}

static jstring
android_media_MediaPlayer_getprop(JNIEnv *env, jobject thiz,
                                                jstring prop) {
    sp<IServiceManager> sm = defaultServiceManager();
    if (sm == NULL) {
        ALOGE("Couldn't get default ServiceManager\n");
    }
    sp<ISystemWriteService> systemWriteService = interface_cast<ISystemWriteService>(sm->getService(String16("system_write")));
    if (systemWriteService == NULL) {
        ALOGE("Couldn't get connection to SystemWriteService\n");
        return env->NewStringUTF("Couldn't get connection to SystemWriteService");
    }
    char* props = const_cast< char* > (env->GetStringUTFChars(prop, NULL));
    String16 value;
    systemWriteService->getProperty(String16(props),value);
    env->ReleaseStringUTFChars(prop, props);
    String8 propvalue(value);
    return env->NewStringUTF(propvalue.string());
}

static void
android_media_MediaPlayer_writeSysfs(JNIEnv *env, jobject thiz,
                                                jstring path, jstring value) {
    sp<IServiceManager> sm = defaultServiceManager();
    if (sm == NULL) {
        ALOGE("Couldn't get default ServiceManager\n");
    }
    sp<ISystemWriteService> systemWriteService = interface_cast<ISystemWriteService>(sm->getService(String16("system_write")));
    if (systemWriteService == NULL) {
        ALOGE("Couldn't get connection to SystemWriteService\n");
        return;
    }
    char* paths = const_cast< char* > (env->GetStringUTFChars(path, NULL));
    char* values = const_cast< char* > (env->GetStringUTFChars(value, NULL));
    String16 sysfs_values(values);
    systemWriteService->writeSysfs(String16(paths), sysfs_values);
    env->ReleaseStringUTFChars(path, paths);
    env->ReleaseStringUTFChars(value, values);
}

static jstring
android_media_MediaPlayer_native_readSysfs(JNIEnv *env, jobject thiz,
                                                jstring path) {
    sp<IServiceManager> sm = defaultServiceManager();
    if (sm == NULL) {
        ALOGE("Couldn't get default ServiceManager\n");
    }
    sp<ISystemWriteService> systemWriteService = interface_cast<ISystemWriteService>(sm->getService(String16("system_write")));
    if (systemWriteService == NULL) {
        ALOGE("Couldn't get connection to SystemWriteService\n");
        return env->NewStringUTF("Couldn't get connection to SystemWriteService");
    }
    char* paths = const_cast< char* > (env->GetStringUTFChars(path, NULL));
    String16 value;
    systemWriteService->readSysfs(String16(paths),value);
    env->ReleaseStringUTFChars(path, paths);
    String8 pathvalue(value);
    return env->NewStringUTF(pathvalue.string());
}

static jint
android_media_MediaPlayer_setRetransmitEndpoint(JNIEnv *env, jobject thiz,
                                                jstring addrString, jint port) {
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return INVALID_OPERATION;
    }

    const char *cAddrString = NULL;

    if (NULL != addrString) {
        cAddrString = env->GetStringUTFChars(addrString, NULL);
        if (cAddrString == NULL) {  // Out of memory
            return NO_MEMORY;
        }
    }
    ALOGV("setRetransmitEndpoint: %s:%d",
            cAddrString ? cAddrString : "(null)", port);

    status_t ret;
    if (cAddrString && (port > 0xFFFF)) {
        ret = BAD_VALUE;
    } else {
        ret = mp->setRetransmitEndpoint(cAddrString,
                static_cast<uint16_t>(port));
    }

    if (NULL != addrString) {
        env->ReleaseStringUTFChars(addrString, cAddrString);
    }

    if (ret == INVALID_OPERATION ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
    }

    return ret;
}

static jboolean
android_media_MediaPlayer_setParameter(JNIEnv *env, jobject thiz, jint key, jobject java_request)
{
    ALOGV("setParameter: key %d", key);
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return false;
    }

    Parcel *request = parcelForJavaObject(env, java_request);
    if(key == KEY_PARAMETER_AML_PLAYER_SET_DISP_LASTFRAME) {
        jobject jParcel = createJavaParcelObject(env);
        if (jParcel != NULL) {
            Parcel* nativeParcel = parcelForJavaObject(env, jParcel);
            nativeParcel->setData(request->data(), request->dataSize());
            int value = nativeParcel->readInt32();
            ALOGV("setParameter(%d), value=%d", __LINE__, value);
            fields.disp_last_frame = value;
        }
    }
    status_t err = mp->setParameter(key, *request);
    if (err == OK) {
        return true;
    } else {
        return false;
    }
}

static void
android_media_MediaPlayer_getParameter(JNIEnv *env, jobject thiz, jint key, jobject java_reply)
{
    ALOGV("getParameter: key %d", key);
    sp<MediaPlayer> mp = getMediaPlayer(env, thiz);
    if (mp == NULL ) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return;
    }

    Parcel *reply = parcelForJavaObject(env, java_reply);
    process_media_player_call(env, thiz, mp->getParameter(key, reply), NULL, NULL );
}

static void
android_media_MediaPlayer_setNextMediaPlayer(JNIEnv *env, jobject thiz, jobject java_player)
{
    ALOGV("setNextMediaPlayer");
    sp<MediaPlayer> thisplayer = getMediaPlayer(env, thiz);
    if (thisplayer == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", "This player not initialized");
        return;
    }
    sp<MediaPlayer> nextplayer = (java_player == NULL) ? NULL : getMediaPlayer(env, java_player);
    if (nextplayer == NULL && java_player != NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", "That player not initialized");
        return;
    }

    if (nextplayer == thisplayer) {
        jniThrowException(env, "java/lang/IllegalArgumentException", "Next player can't be self");
        return;
    }
    // tie the two players together
    process_media_player_call(
            env, thiz, thisplayer->setNextMediaPlayer(nextplayer),
            "java/lang/IllegalArgumentException",
            "setNextMediaPlayer failed." );
    ;
}

static void
android_media_MediaPlayer_updateProxyConfig(
        JNIEnv *env, jobject thiz, jobject proxyProps)
{
    ALOGV("updateProxyConfig");
    sp<MediaPlayer> thisplayer = getMediaPlayer(env, thiz);
    if (thisplayer == NULL) {
        return;
    }

    if (proxyProps == NULL) {
        thisplayer->updateProxyConfig(
                NULL /* host */, 0 /* port */, NULL /* exclusionList */);
    } else {
        jstring hostObj = (jstring)env->CallObjectMethod(
                proxyProps, fields.proxyConfigGetHost);

        const char *host = env->GetStringUTFChars(hostObj, NULL);

        int port = env->CallIntMethod(proxyProps, fields.proxyConfigGetPort);

        jstring exclusionListObj = (jstring)env->CallObjectMethod(
                proxyProps, fields.proxyConfigGetExclusionList);

        const char *exclusionList =
            env->GetStringUTFChars(exclusionListObj, NULL);

        if (host != NULL && exclusionListObj != NULL) {
            thisplayer->updateProxyConfig(host, port, exclusionList);
        }

        if (exclusionList != NULL) {
            env->ReleaseStringUTFChars(exclusionListObj, exclusionList);
            exclusionList = NULL;
        }

        if (host != NULL) {
            env->ReleaseStringUTFChars(hostObj, host);
            host = NULL;
        }
    }
}
static jobject android_media_MediaPlayer_getSubtitleBitmap(JNIEnv *env, jobject thiz)
{
	size_t width, height;
    width = bitmapW;
    height = bitmapH;
	if((width <= 0) || (height <= 0))
	{
		ALOGV("return NULL,width = 0\n");
		return NULL;
	}

    jobject config = env->CallStaticObjectMethod(
                        fields.configClazz,
                        fields.createConfigMethod,
                        //SkBitmap::kRGB_565_Config);
                        SkBitmap::kARGB_8888_Config);

    jobject jBitmap = env->CallStaticObjectMethod(
                            fields.bitmapClazz,
                            fields.createBitmapMethod,
                            width,
                            height,
                            config);
    if (bitmapMemory != NULL) {
        SkBitmap *bitmap = (SkBitmap *) env->GetIntField(jBitmap, fields.nativeBitmap);
        bitmap->lockPixels();
        //ALOGE("getSubtitleBitmap width*height(%d*%d=%d), width*height*4:%d, bitmapSize:%d\n", width, height, width * height,width * height *4, bitmapSize);
        memcpy((uint16_t*)bitmap->getPixels(), (void *)bitmapMemory->pointer(), /*bitmapSize*/width * height *4);
        bitmap->unlockPixels();
        bitmapMemory = NULL;
    }
        
    return jBitmap;

}

// ----------------------------------------------------------------------------

static JNINativeMethod gMethods[] = {
    {
        "_setDataSource",
        "(Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)V",
        (void *)android_media_MediaPlayer_setDataSourceAndHeaders
    },

    {"_setDataSource",       "(Ljava/io/FileDescriptor;JJ)V",    (void *)android_media_MediaPlayer_setDataSourceFD},
    {"_setVideoSurface",    "(Landroid/view/Surface;)V",        (void *)android_media_MediaPlayer_setVideoSurface},
    {"prepare",             "()V",                              (void *)android_media_MediaPlayer_prepare},
    {"prepareAsync",        "()V",                              (void *)android_media_MediaPlayer_prepareAsync},
    {"_start",              "()V",                              (void *)android_media_MediaPlayer_start},
    {"_stop",               "()V",                              (void *)android_media_MediaPlayer_stop},
    {"getVideoWidth",       "()I",                              (void *)android_media_MediaPlayer_getVideoWidth},
    {"getVideoHeight",      "()I",                              (void *)android_media_MediaPlayer_getVideoHeight},
    {"_seekTo",              "(I)V",                             (void *)android_media_MediaPlayer_seekTo},
    {"_pause",              "()V",                              (void *)android_media_MediaPlayer_pause},
    {"isPlaying",           "()Z",                              (void *)android_media_MediaPlayer_isPlaying},
    {"getCurrentPosition",  "()I",                              (void *)android_media_MediaPlayer_getCurrentPosition},
    {"getDuration",         "()I",                              (void *)android_media_MediaPlayer_getDuration},
    {"_release",            "()V",                              (void *)android_media_MediaPlayer_release},
    {"_reset",              "()V",                              (void *)android_media_MediaPlayer_reset},
    {"setAudioStreamType",  "(I)V",                             (void *)android_media_MediaPlayer_setAudioStreamType},
    {"setLooping",          "(Z)V",                             (void *)android_media_MediaPlayer_setLooping},
    {"isLooping",           "()Z",                              (void *)android_media_MediaPlayer_isLooping},
    {"setVolume",           "(FF)V",                            (void *)android_media_MediaPlayer_setVolume},
    {"native_invoke",       "(Landroid/os/Parcel;Landroid/os/Parcel;)I",(void *)android_media_MediaPlayer_invoke},
    {"native_setMetadataFilter", "(Landroid/os/Parcel;)I",      (void *)android_media_MediaPlayer_setMetadataFilter},
    {"native_getMetadata", "(ZZLandroid/os/Parcel;)Z",          (void *)android_media_MediaPlayer_getMetadata},
    {"native_init",         "()V",                              (void *)android_media_MediaPlayer_native_init},
    {"native_setup",        "(Ljava/lang/Object;)V",            (void *)android_media_MediaPlayer_native_setup},
    {"native_finalize",     "()V",                              (void *)android_media_MediaPlayer_native_finalize},
    {"getAudioSessionId",   "()I",                              (void *)android_media_MediaPlayer_get_audio_session_id},
    {"setAudioSessionId",   "(I)V",                             (void *)android_media_MediaPlayer_set_audio_session_id},
    {"setAuxEffectSendLevel", "(F)V",                           (void *)android_media_MediaPlayer_setAuxEffectSendLevel},
    {"attachAuxEffect",     "(I)V",                             (void *)android_media_MediaPlayer_attachAuxEffect},
    {"native_pullBatteryData", "(Landroid/os/Parcel;)I",        (void *)android_media_MediaPlayer_pullBatteryData},
    {"setParameter",        "(ILandroid/os/Parcel;)Z",          (void *)android_media_MediaPlayer_setParameter},
    {"getParameter",        "(ILandroid/os/Parcel;)V",          (void *)android_media_MediaPlayer_getParameter},
    {"native_setRetransmitEndpoint", "(Ljava/lang/String;I)I",  (void *)android_media_MediaPlayer_setRetransmitEndpoint},
    {"setNextMediaPlayer",  "(Landroid/media/MediaPlayer;)V",   (void *)android_media_MediaPlayer_setNextMediaPlayer},
    {"updateProxyConfig", "(Landroid/net/ProxyProperties;)V", (void *)android_media_MediaPlayer_updateProxyConfig},
    {"getSubtitleBitmap",  "()Landroid/graphics/Bitmap;",     (void *)android_media_MediaPlayer_getSubtitleBitmap},
    {"setprop", "(Ljava/lang/String;Ljava/lang/String;)V",      (void *)android_media_MediaPlayer_setprop},
    {"getprop", "(Ljava/lang/String;)Ljava/lang/String;",       (void *)android_media_MediaPlayer_getprop},
    {"writeSysfs", "(Ljava/lang/String;Ljava/lang/String;)V",      (void *)android_media_MediaPlayer_writeSysfs},
    {"native_readSysfs", "(Ljava/lang/String;)Ljava/lang/String;",       (void *)android_media_MediaPlayer_native_readSysfs},
    {"sendDataReport", "(Ljava/lang/String;I)V",      (void *)android_media_MediaPlayer_sendDataReport},

};

static const char* const kClassPathName = "android/media/MediaPlayer";

// This function only registers the native methods
static int register_android_media_MediaPlayer(JNIEnv *env)
{
    return AndroidRuntime::registerNativeMethods(env,
                "android/media/MediaPlayer", gMethods, NELEM(gMethods));
}

extern int register_android_media_ImageReader(JNIEnv *env);
extern int register_android_media_Crypto(JNIEnv *env);
extern int register_android_media_Drm(JNIEnv *env);
extern int register_android_media_MediaCodec(JNIEnv *env);
extern int register_android_media_MediaExtractor(JNIEnv *env);
extern int register_android_media_MediaCodecList(JNIEnv *env);
extern int register_android_media_MediaMetadataRetriever(JNIEnv *env);
extern int register_android_media_MediaMuxer(JNIEnv *env);
extern int register_android_media_MediaRecorder(JNIEnv *env);
extern int register_android_media_MediaScanner(JNIEnv *env);
extern int register_android_media_ResampleInputStream(JNIEnv *env);
extern int register_android_media_MediaProfiles(JNIEnv *env);
extern int register_android_media_AmrInputStream(JNIEnv *env);
extern int register_android_mtp_MtpDatabase(JNIEnv *env);
extern int register_android_mtp_MtpDevice(JNIEnv *env);
extern int register_android_mtp_MtpServer(JNIEnv *env);

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
    JNIEnv* env = NULL;
    jint result = -1;

    if (vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        ALOGE("ERROR: GetEnv failed\n");
        goto bail;
    }
    assert(env != NULL);

    if (register_android_media_ImageReader(env) < 0) {
        ALOGE("ERROR: ImageReader native registration failed");
        goto bail;
    }

    if (register_android_media_MediaPlayer(env) < 0) {
        ALOGE("ERROR: MediaPlayer native registration failed\n");
        goto bail;
    }

    if (register_android_media_MediaRecorder(env) < 0) {
        ALOGE("ERROR: MediaRecorder native registration failed\n");
        goto bail;
    }

    if (register_android_media_MediaScanner(env) < 0) {
        ALOGE("ERROR: MediaScanner native registration failed\n");
        goto bail;
    }

    if (register_android_media_MediaMetadataRetriever(env) < 0) {
        ALOGE("ERROR: MediaMetadataRetriever native registration failed\n");
        goto bail;
    }

    if (register_android_media_AmrInputStream(env) < 0) {
        ALOGE("ERROR: AmrInputStream native registration failed\n");
        goto bail;
    }

    if (register_android_media_ResampleInputStream(env) < 0) {
        ALOGE("ERROR: ResampleInputStream native registration failed\n");
        goto bail;
    }

    if (register_android_media_MediaProfiles(env) < 0) {
        ALOGE("ERROR: MediaProfiles native registration failed");
        goto bail;
    }

    if (register_android_mtp_MtpDatabase(env) < 0) {
        ALOGE("ERROR: MtpDatabase native registration failed");
        goto bail;
    }

    if (register_android_mtp_MtpDevice(env) < 0) {
        ALOGE("ERROR: MtpDevice native registration failed");
        goto bail;
    }

    if (register_android_mtp_MtpServer(env) < 0) {
        ALOGE("ERROR: MtpServer native registration failed");
        goto bail;
    }

    if (register_android_media_MediaCodec(env) < 0) {
        ALOGE("ERROR: MediaCodec native registration failed");
        goto bail;
    }

    if (register_android_media_MediaExtractor(env) < 0) {
        ALOGE("ERROR: MediaCodec native registration failed");
        goto bail;
    }

    if (register_android_media_MediaMuxer(env) < 0) {
        ALOGE("ERROR: MediaMuxer native registration failed");
        goto bail;
    }

    if (register_android_media_MediaCodecList(env) < 0) {
        ALOGE("ERROR: MediaCodec native registration failed");
        goto bail;
    }

    if (register_android_media_Crypto(env) < 0) {
        ALOGE("ERROR: MediaCodec native registration failed");
        goto bail;
    }

    if (register_android_media_Drm(env) < 0) {
        ALOGE("ERROR: MediaDrm native registration failed");
        goto bail;
    }

    /* success -- return valid version number */
    result = JNI_VERSION_1_4;

bail:
    return result;
}

// KTHXBYE
