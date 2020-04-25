#ifndef ANDROID_CMCCMEDIAPLAYERMANAGER_H
#define ANDROID_CMCCMEDIAPLAYERMANAGER_H

#include <media/MediaPlayerInterface.h>
#include <media/CMCCPlayer.h>

namespace android 
{

class CMCCMediaPlayerManager : public MediaPlayerInterface 
{
public:
    					CMCCMediaPlayerManager();
    virtual 			~CMCCMediaPlayerManager();
	virtual status_t 	initCheck();
    virtual status_t 	setUID(uid_t uid) {return NO_ERROR;}
    virtual status_t 	setDataSource(const char *url, const KeyedVector<String8, String8> *headers);
    virtual status_t 	setDataSource(int fd, int64_t offset, int64_t length) {return NO_ERROR;}
    virtual status_t 	setDataSource(const sp<IStreamSource> &source) {return NO_ERROR;}
#if 0//ANDROID_API_LEVE <= 17
	virtual status_t 	setVideoSurfaceTexture(const sp<ISurfaceTexture> &surfaceTexture);
#else
    virtual status_t 	setVideoSurfaceTexture(const sp<IGraphicBufferProducer> &bufferProducer);
#endif
    virtual status_t 	prepare();
    virtual status_t 	prepareAsync();
    virtual status_t 	start();
    virtual status_t 	stop();
    virtual status_t 	pause();
    virtual bool 		isPlaying();
    virtual status_t 	seekTo(int msec);
    virtual status_t 	getCurrentPosition(int *msec);
    virtual status_t 	getDuration(int *msec);
    virtual status_t 	reset();
    virtual status_t 	setLooping(int loop);
    virtual player_type playerType();
    virtual status_t 	invoke(const Parcel &request, Parcel *reply);
    virtual status_t	setParameter(int key, const Parcel &request);
    virtual status_t	getParameter(int key, Parcel *reply) {return NO_ERROR;}
private:
    CMCCPlayer *mPlayer;
};

}  // namespace android

#endif  // CMCCMEDIAPLAYERMANAGER_H
