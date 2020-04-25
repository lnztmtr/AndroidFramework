#ifndef ANDROID_CTCMEDIAPLAYER_H
#define ANDROID_CTCMEDIAPLAYER_H

#include <fcntl.h>
#include "dlfcn.h"
#include <gui/Surface.h>
#include <android/native_window.h>
#include <media/MediaPlayerInterface.h>
#include <media/CTC_MediaControl.h>
#include "AmCTCDataSouceProtocol.h"

namespace android
{

#define TRACE()	    ALOGV("[%s::%d]\n", __FUNCTION__, __LINE__)
#define DEBUG(format, ...)	ALOGV("[%s::%d] "#format, __FUNCTION__, __LINE__, ## __VA_ARGS__)
#define ERROR(format, ...)	ALOGE("[%s::%d] "#format, __FUNCTION__, __LINE__, ## __VA_ARGS__)

#define ALIGN(x, a) (((x)+(a)-1)&~((a)-1))

class CTC_MCNotify : public ICTC_MCNotify{
public:
    CTC_MCNotify();
    virtual ~CTC_MCNotify();
public:
    virtual void OnNotify(int event, const char* msg);
    virtual void setPlayer(void* cookie);
    void* mPlayer;
};

class CTCMediaPlayer : public MediaPlayerInterface
{
public:
    CTCMediaPlayer();
    virtual 			~CTCMediaPlayer();
    virtual status_t 	initCheck();
    virtual status_t 	setDataSource(const char *url, const KeyedVector<String8, String8> *headers);
    virtual status_t 	setDataSource(int fd, int64_t offset, int64_t length);
    virtual status_t 	setDataSource(const sp<IStreamSource> &source) {return NO_ERROR;}
    virtual status_t 	setVideoSurfaceTexture(const sp<IGraphicBufferProducer> &bufferProducer);
    virtual status_t 	prepare();
    virtual status_t 	prepareAsync();
    virtual status_t 	start();
    virtual status_t 	stop();
    virtual status_t 	pause();
    virtual bool 		  isPlaying();
    virtual status_t 	seekTo(int msec);
    virtual status_t 	getCurrentPosition(int *msec);
    virtual status_t 	getDuration(int *msec);
    virtual status_t 	reset();
    virtual status_t 	setLooping(int loop);
    virtual player_type playerType();
    virtual status_t 	invoke(const Parcel &request, Parcel *reply);
    virtual status_t	setParameter(int key, const Parcel &request) {return NO_ERROR;}
    virtual status_t	getParameter(int key, Parcel *reply) {return NO_ERROR;}
    virtual status_t  prepareEnd();
    virtual status_t  playEnd();
private:
    status_t    setdatasource(const char *path, int fd, int64_t offset, int64_t length);
    bool        PropIsEnable(const char* str);
private:
    enum State {
        STATE_UNKOWN = 0,
        STATE_IDLE,
        STATE_UNPREPARED,
        STATE_PREPARING,
        STATE_PREPARED,
        STATE_RUNNING,
        STATE_PAUSED,
        STATE_STOPPED,
        STATE_RESETED,
    };
    virtual void       init();
    virtual char *     dumpState();
    ICTC_MediaControl* mPlayer;
    CTC_MCNotify*      pNotify;
    void*              dl_handle;
    FGetMediaControl   gGetMediaControl;
    FFreeMediaControl  gFreeMediaControl;
    sp <AmCTCDataSouceProtocol> mSouceProtocol;
    char*              mUrl;
    State              mState;
};

}  // namespace android

#endif  // ANDROID_CTCMEDIAPLAYER_H
