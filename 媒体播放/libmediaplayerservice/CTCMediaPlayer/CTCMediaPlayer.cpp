#define LOG_NDEBUG 0
#define LOG_TAG "CTCMediaPlayer"
#include "CTCMediaPlayer.h"
#include <cutils/properties.h>
#include <utils/String8.h>





namespace android {

CTC_MCNotify::CTC_MCNotify() {
    TRACE();
}

CTC_MCNotify::~CTC_MCNotify() {
    TRACE();
}

void CTC_MCNotify::setPlayer(void* cookie) {
    TRACE();
    mPlayer = cookie;
}

void CTC_MCNotify::OnNotify(int event, const char* msg) {
    DEBUG("event:%d, msg:%s", event, msg);
    CTCMediaPlayer* player = (CTCMediaPlayer*)mPlayer;
    DEBUG("player: %p", player);
    if (event == TYPE_EVENT_RTSP_COMPLETE) {
        if (player != NULL) {
            player->prepareEnd();
        }
    } else if ((event == TYPE_EVENT_MEDIA_ERROR) || (event == TYPE_EVENT_PLAYERR)) {
        if (player != NULL) {
            player->sendEvent(MEDIA_ERROR, MEDIA_ERROR_UNKNOWN);
        }
    } else if (event == TYPE_EVENT_MEDIA_END) {
        if (player != NULL) {
            player->playEnd();
        }
    }
}

CTCMediaPlayer::CTCMediaPlayer()
        : mState(STATE_UNKOWN) {
    TRACE();
    init();
    dl_handle = dlopen("/system/lib/libCTC_AmMediaControl.so", RTLD_LAZY);
    if (dl_handle == NULL) {
        ERROR("dlopen failed errmsg: %s", dlerror());
    } else {
        gGetMediaControl = (FGetMediaControl)dlsym(dl_handle, "GetMediaControl");
        gFreeMediaControl = (FFreeMediaControl)dlsym(dl_handle, "FreeMediaControl");

        if (gGetMediaControl != NULL) {
            //pNotify = new CTC_MCNotify();
           // pNotify->setPlayer(this);
            mPlayer = gGetMediaControl();
            mState = STATE_IDLE;
            DEBUG("create ctc player");
            //if (mPlayer != NULL)
            //    mPlayer->SetListenNotify(pNotify);
        } else {
            ERROR("dlopen failed gGetMediaControl is null!");
        }
    }
}

CTCMediaPlayer::~CTCMediaPlayer() {
    TRACE();
    if (dl_handle != NULL) {
        dlclose(dl_handle);
        dl_handle = NULL;
    }
    TRACE();
    reset();
    TRACE();
    if (mSouceProtocol.get() != NULL) {
        DEBUG("---destruct ~CTCMediaPlayer mSouceProtocol.clear();\n");
        mSouceProtocol.clear();
    }
    DEBUG("---destruct ~CTCMediaPlayer mSouceProtocol.clear() ok\n");
    if (pNotify != NULL) {
        delete pNotify;
        pNotify = NULL;
    }
    TRACE();
    init();
    TRACE();
    DEBUG("---destruct ~CTCMediaPlayer ok\n");
}

bool CTCMediaPlayer::PropIsEnable(const char* str)
{
	char value[PROPERTY_VALUE_MAX];
	if(property_get(str, value, NULL)>0)
	{
		if ((!strcmp(value, "1") || !strcmp(value, "true")))
		{
			LOGI("%s is enabled\n",str);
			return true;
		}
	}
	LOGI("%s is disabled\n",str);
	return false;
}


void CTCMediaPlayer::init() {
    TRACE();
    dl_handle = NULL;
    gGetMediaControl = NULL;
    gFreeMediaControl = NULL;
    mPlayer = NULL;
    pNotify = NULL;
    mUrl = NULL;
    mState = STATE_UNKOWN;
}

status_t CTCMediaPlayer::initCheck() {
    DEBUG("mState: %s", dumpState());
    if (mState != STATE_IDLE) {
        return INVALID_OPERATION;
    }
    return NO_ERROR;
}

status_t CTCMediaPlayer::setDataSource(const char *url, const KeyedVector<String8, String8> *headers) {
    DEBUG("url: %s, mState: %s", url, dumpState());
    if (mState != STATE_IDLE) {
        return INVALID_OPERATION;
    }

    if (mUrl != NULL) {
        free(mUrl);
        mUrl = NULL;
    }

    if (url != NULL) {
        int len = strlen(url);
        DEBUG("len: %d", len);
        mUrl = strdup(url);
        if (mUrl != NULL) {
            mUrl[len] = 0;
            DEBUG("mUrl: %s", mUrl);
            mState = STATE_UNPREPARED;
            return NO_ERROR;
        } else {
            ERROR("url malloc fail!");
            return INVALID_OPERATION;
        }
    } else {
        ERROR("url is null!");
        return INVALID_OPERATION;
    }

    //DEBUG("-fd-mState: %s", dumpState());
    return NO_ERROR;

}


status_t CTCMediaPlayer::setdatasource(const char *path, int fd, int64_t offset, int64_t length)
{
    int num;
    char * file = NULL;
    //int dur_update = PropIsEnable("media.amplayer.dur_update", 0);

    int time;
    file = (char *)malloc(strlen(path) + 10);
    if (file == NULL) {
        return NO_MEMORY;
    }

    LOGV("setdatasource: path=%s\n", path);


    num = sprintf(file, "%s", path);
    file[num] = '\0';

    LOGV("setdatasource: file=%s, num =%d\n", file, num);

    if (mUrl != NULL) {
        free(mUrl);
        mUrl = NULL;
    }

    if (file != 0) {
        int len = strlen(file);
        DEBUG("len: %d", len);
        mUrl = strdup(file);
        if (mUrl != NULL) {
            mUrl[len] = 0;
            DEBUG("mUrl: %s", mUrl);
            mState = STATE_UNPREPARED;
            return NO_ERROR;
        } else {
            ERROR("url malloc fail!");
            return INVALID_OPERATION;
        }
    } else {
        ERROR("url is null!");
        return INVALID_OPERATION;
    }

    LOGI("sendEvent  setDataSource\n");
    return NO_ERROR;

}


#if 0
status_t CTCMediaPlayer::setdatasource(const char *path, int fd, int64_t offset, int64_t length)
{
    int num;
    char * file = NULL;
    //int dur_update = PropIsEnable("media.amplayer.dur_update", 0);

    int time;
    file = (char *)malloc(strlen(path) + 10);
    if (file == NULL) {
        return NO_MEMORY;
    }

    LOGV("setdatasource: path=%s\n", path);


    num = sprintf(file, "%s", path);
    file[num] = '\0';

    LOGV("setdatasource: file=%s, num =%d\n", file, num);

    if (mUrl != NULL) {
        free(mUrl);
        mUrl = NULL;
    }

    if (file != 0) {
        int len = strlen(file);
        DEBUG("len: %d", len);
        mUrl = strdup(file);
        if (mUrl != NULL) {
            mUrl[len] = 0;
            DEBUG("mUrl: %s", mUrl);
            mState = STATE_UNPREPARED;
            return NO_ERROR;
        } else {
            ERROR("url malloc fail!");
            return INVALID_OPERATION;
        }
    } else {
        ERROR("url is null!");
        return INVALID_OPERATION;
    }

    LOGI("sendEvent  setDataSource\n");
    return NO_ERROR;

}
#endif


status_t CTCMediaPlayer::setDataSource(int fd, int64_t offset, int64_t length) {
    DEBUG("--setDataSource: fd=%d, mState: %s", fd, dumpState());
    if (mState != STATE_IDLE) {
        return INVALID_OPERATION;
    }

    //sendEvent(MEDIA_SET_DATASOURCE);

    if (mUrl != NULL) {
        free(mUrl);
        mUrl = NULL;
    }

#if 1
    if (PropIsEnable("media.amplayer.dsource4local")) {

        mSouceProtocol = AmCTCDataSouceProtocol::CreateFromFD(fd, offset, length);
        mSouceProtocol->BasicInit();
        if (mSouceProtocol.get() != NULL) {
            return setdatasource(mSouceProtocol->GetPathString(), fd, 0, 0x7ffffffffffffffLL);
        }
    }


#endif

    mState = STATE_UNPREPARED;



    //usleep(500000);

#if 1
    //mfd = fd;

#if 0
    int count;
    int ret = 0;
    char data[2048] = {0};
    while (count++ <5) {
        ret = ::read(fd, data, 5);
        ALOGE("count:%d, ret=%d, data[0-4]=0x%x, 0x%x, 0x%x, 0x%x,0x%x\n",
            count, ret, data[0], data[1], data[2], data[3], data[4]);
        ret = read(fd, data, 5);
        ALOGE("count:%d, ret=%d, data[0-4]=0x%x, 0x%x, 0x%x, 0x%x,0x%x\n",
            count, ret, data[0], data[1], data[2], data[3], data[4]);
        ret = ::read(mfd, data, 5);
        ALOGE("count:%d, ret=%d, data[0-4]=0x%x, 0x%x, 0x%x, 0x%x,0x%x\n",
            count, ret, data[0], data[1], data[2], data[3], data[4]);
    }
#endif



#else



    if (fd != 0) {
        int len = strlen("/storage/external_storage/sda1/gopro_0.ts");
        DEBUG("len: %d", len);
        mUrl = strdup("/storage/external_storage/sda1/gopro_0.ts");
        if (mUrl != NULL) {
            mUrl[len] = 0;
            DEBUG("mUrl: %s", mUrl);
            mState = STATE_UNPREPARED;
            return NO_ERROR;
        } else {
            ERROR("url malloc fail!");
            return INVALID_OPERATION;
        }
    } else {
        ERROR("url is null!");
        return INVALID_OPERATION;
    }

    LOGI("sendEvent  setDataSource, mfd=%d, mUrl=%s\n",mfd, mUrl);
    return NO_ERROR;

#endif
    //DEBUG("-fd-mState: %s", dumpState());
    return NO_ERROR;

}


#if 0
status_t  CTCMediaPlayer::setParameter(int key, const Parcel &request)
{
    //Mutex::Autolock autoLock(mMutex);
    LOGI("setParameter %d\n", key);
    switch (key) {
    case KEY_PARAMETER_CTC_PLAYER_SET_URL: {

        const String16 uri16 = request.readString16();
        String8 keyStr = String8(uri16);

        LOGI("--ppp, setParameter %d=[%s]\n", key, keyStr.string());
        int len = strlen(keyStr.string());
        DEBUG("setParameter:len: %d", len);
#if 0
        mUrl = strdup(keyStr.string());
        if (mUrl != NULL) {
            mUrl[len] = 0;
            DEBUG("mUrl: %s", mUrl);
            mState = STATE_UNPREPARED;
            return NO_ERROR;
        } else {
            ERROR("url malloc fail!");
            return INVALID_OPERATION;
        }
#endif
        break;
    }

    default:
        LOGI("unsupport setParameter value!=%d\n", key);
    }
    return OK;
}
#endif

status_t CTCMediaPlayer::setVideoSurfaceTexture(
        const sp<IGraphicBufferProducer> &bufferProducer) {
    DEBUG("setVideoSurfaceTexture:mState: %s", dumpState());
    int nativeWidth = 32;
    int nativeHeight = 32;
    switch (mState) {
        case STATE_RESETED:
            return INVALID_OPERATION;
        default:
            break;
    }
    sp<ANativeWindow> mNativeWindow;
    sp<Surface> mSoftSurface;
    ANativeWindowBuffer* buf;
    char* vaddr;
    if(bufferProducer != NULL) {
        //mNativeWindow = new Surface(bufferProducer);
        mSoftSurface = new Surface(bufferProducer);
    } else {
        ERROR("setVideoSurfaceTexture, bufferProducer is NULL!\n");
        return INVALID_OPERATION;
    }
    //Surface *mSurface = getSelf(mNativeWindow);
    mPlayer->SetSurface(mSoftSurface.get());
#if 0
    DEBUG("native window set aml overlay");
    native_window_set_buffer_count(mNativeWindow.get(), 4);
    native_window_set_usage(mNativeWindow.get(), GRALLOC_USAGE_HW_TEXTURE | GRALLOC_USAGE_EXTERNAL_DISP | GRALLOC_USAGE_AML_VIDEO_OVERLAY);
    //native_window_set_buffers_format(mNativeWindow.get(), WINDOW_FORMAT_RGBA_8888);
    native_window_set_buffers_geometry(mNativeWindow.get(), ALIGN(nativeWidth, 32), nativeHeight, WINDOW_FORMAT_RGBA_8888);
    native_window_set_scaling_mode(mNativeWindow.get(), NATIVE_WINDOW_SCALING_MODE_SCALE_TO_WINDOW);
    DEBUG("dequeueBuffer start");
    int err = mNativeWindow->dequeueBuffer_DEPRECATED(mNativeWindow.get(), &buf);
    if (err != 0) {
        ERROR("dequeueBuffer failed: %s (%d)", strerror(-err), -err);
        return INVALID_OPERATION;
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
#endif
    return NO_ERROR;
}

status_t CTCMediaPlayer::prepareEnd() {
    DEBUG("mState: %s", dumpState());
    switch (mState) {
        case STATE_PREPARING:
            mState = STATE_PREPARED;
            sendEvent(MEDIA_PREPARED);
            break;
        default:
            return INVALID_OPERATION;
    };
    return NO_ERROR;
}

status_t CTCMediaPlayer::prepare() {
    DEBUG("mState: %s", dumpState());
    return prepareAsync();
}

status_t CTCMediaPlayer::prepareAsync() {
    DEBUG("mState: %s", dumpState());
    switch (mState) {
        case STATE_PREPARING:
            break;
        case STATE_UNPREPARED:
            DEBUG("--prepareAsync, mPlayer=%p, mUrl=%s", mPlayer, mUrl);
            if (mUrl != NULL) {
                mState = STATE_PREPARING;
                mPlayer->PlayFromStart(mUrl);
                #if 1
                mState = STATE_PREPARED;
                sendEvent(MEDIA_PREPARED);
                DEBUG("-after-prepareAsync, sendEvent(MEDIA_PREPARED) ");
                #endif
                break;
            } else {
                return INVALID_OPERATION;
            }
        default:
            return INVALID_OPERATION;
    };
    return NO_ERROR;
}

status_t CTCMediaPlayer::start() {
    DEBUG("url: %s, mState: %s", mUrl, dumpState());
    switch (mState) {
        case STATE_UNPREPARED:
            prepareAsync();
            return NO_ERROR;
        case STATE_PREPARING:
        case STATE_PREPARED:
            sendEvent(MEDIA_STARTED);
            break;
        case STATE_STOPPED:
            if (mUrl != NULL) {
                mPlayer->PlayFromStart(mUrl);
                sendEvent(MEDIA_STARTED);
                break;
            } else
                return INVALID_OPERATION;
        case STATE_RUNNING:
            break;

        case STATE_PAUSED:
            mPlayer->Resume();
            //sendEvent(MEDIA_RESUME);
            sendEvent(MEDIA_STARTED);
            break;

        default:
            return INVALID_OPERATION;
    }

    mState = STATE_RUNNING;
    return NO_ERROR;
}

status_t CTCMediaPlayer::stop() {
    DEBUG("stop:mState: %s", dumpState());
    switch (mState) {
        case STATE_STOPPED:
            return NO_ERROR;
        case STATE_PREPARING:
        case STATE_PREPARED:
        case STATE_PAUSED:
        case STATE_RUNNING:
            DEBUG("stop:before mPlayer->Stop()\n");
            mPlayer->Stop();
            DEBUG("stop:after mPlayer->Stop()\n");
            sendEvent(MEDIA_STOPPED);
            break;
        default:
            return INVALID_OPERATION;
    }

    mState = STATE_STOPPED;
    DEBUG("stop:ok, STATE_STOPPED\n");
    return NO_ERROR;
}

status_t CTCMediaPlayer::playEnd() {
    DEBUG("mState: %s", dumpState());
    switch (mState) {
        case STATE_RUNNING:
            mState = STATE_STOPPED;
            sendEvent(MEDIA_PLAYBACK_COMPLETE);
            break;
        default:
            return INVALID_OPERATION;
    };
    return NO_ERROR;
}

status_t CTCMediaPlayer::pause() {
    TRACE();
    switch (mState) {
        case STATE_PAUSED:
            return NO_ERROR;
        case STATE_PREPARING:
        case STATE_PREPARED:
        case STATE_RUNNING:
            mPlayer->Pause();
            sendEvent(MEDIA_PAUSED);
            break;

        default:
            return INVALID_OPERATION;
    }

    mState = STATE_PAUSED;
    return NO_ERROR;
}

bool CTCMediaPlayer::isPlaying() {
    DEBUG("mState: %s", dumpState());
    return (mState == STATE_RUNNING);
}

status_t CTCMediaPlayer::seekTo(int msec) {
    TRACE();
    switch (mState) {
        case STATE_PREPARING:
        case STATE_PREPARED:
        case STATE_RUNNING:
        case STATE_PAUSED:
            sendEvent(MEDIA_SEEK_START);
            mPlayer->PlayByTime(msec);
            sendEvent(MEDIA_SEEK_COMPLETE);
            break;
        default:
            return INVALID_OPERATION;
    }

    mState = STATE_RUNNING;
    return NO_ERROR;
}

status_t CTCMediaPlayer::invoke(const Parcel &request, Parcel *reply)
{
    DEBUG("invoke:---\n");
    if (NULL == reply) {
        return android::BAD_VALUE;
    }
    int32_t methodId;
    status_t ret = request.readInt32(&methodId);
    if (ret != android::OK) {
        return ret;
    }
    switch (methodId) {
        case INVOKE_ID_SET_TRACK_VOLUME:{
            float left_volume = request.readFloat();
            float right_volume = request.readFloat();
            DEBUG("invoke:Set left volume:%f, right volume = %f\n",left_volume,right_volume);
            mPlayer->SetVolume(left_volume, right_volume);
            return OK;
            //return setVolume(left_volume,right_volume);
        }
        default: {
            return OK;
        }
    }

}

status_t CTCMediaPlayer::getCurrentPosition(int *msec) {
    TRACE();
    if (mPlayer == NULL) {
        return INVALID_OPERATION;
    }
    *msec = mPlayer->GetCurrentPlayTime() * 1000;
    DEBUG("msec: %d", *msec);
    return NO_ERROR;
}

status_t CTCMediaPlayer::getDuration(int *msec) {
    TRACE();
    *msec = mPlayer->GetDuration() * 1000;
    DEBUG("msec: %d", *msec);
    return NO_ERROR;
}

status_t CTCMediaPlayer::reset() {
    DEBUG("CTCMediaPlayer::reset, %s\n", dumpState());
    switch (mState) {
        case STATE_RESETED:
            return NO_ERROR;
        case STATE_UNKOWN:
            return INVALID_OPERATION;
        default:
            break;
    };
    stop();
    if ((mPlayer != NULL) && (gFreeMediaControl != NULL)) {
        DEBUG("free ctc player");
        gFreeMediaControl(mPlayer);
        DEBUG("free ctc player ok");
    }
    if (mUrl != NULL) {
        free(mUrl);
        mUrl = NULL;
    }
    //sendEvent(MEDIA_EXIT);

    mState = STATE_RESETED;
    DEBUG("CTCMediaPlayer::reset end\n");
    return NO_ERROR;
}

status_t CTCMediaPlayer::setLooping(int loop) {
    TRACE();
    return NO_ERROR;
}

player_type CTCMediaPlayer::playerType() {
    TRACE();
    return CTC_PLAYER;
}

char * CTCMediaPlayer::dumpState() {
    switch (mState) {
        case STATE_UNKOWN:
            return "STATE_UNKOWN";
        case STATE_IDLE:
            return "STATE_IDLE";
        case STATE_UNPREPARED:
            return "STATE_UNPREPARED";
        case STATE_PREPARING:
            return "STATE_PREPARING";
        case STATE_PREPARED:
            return "STATE_PREPARED";
        case STATE_RUNNING:
            return "STATE_RUNNING";
        case STATE_PAUSED:
            return "STATE_PAUSED";
        case STATE_STOPPED:
            return "STATE_STOPPED";
        case STATE_RESETED:
            return "STATE_RESETED";
    }
    return "STATE_UNKOWN";
}

}  // namespace android
