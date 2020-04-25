//#define LOG_NDEBUG 0
#define LOG_TAG "CMPlayer"
#include <utils/Log.h>
#include <cutils/properties.h>

#include "CMCCMediaPlayerManager.h"

namespace android {

CMCCMediaPlayerManager::CMCCMediaPlayerManager()
    : mPlayer(new CMCCPlayer()) {
    ALOGV("CMCCMediaPlayerManager");
	mPlayer->setListener(this);
}

CMCCMediaPlayerManager::~CMCCMediaPlayerManager() {
    ALOGV("~CMCCMediaPlayerManager");
    reset();
    delete mPlayer;
    mPlayer = NULL;
}

status_t CMCCMediaPlayerManager::initCheck() {
    ALOGV("initCheck");
    return OK;
}

status_t CMCCMediaPlayerManager::setDataSource(
        const char *url, const KeyedVector<String8, String8> *headers) {
	ALOGV("setDataSource url:%s", url);
    return mPlayer->setDataSource(url, headers);
}

status_t CMCCMediaPlayerManager::setVideoSurfaceTexture(
        const sp<IGraphicBufferProducer> &bufferProducer) {
    ALOGV("setVideoSurfaceTexture");
    return mPlayer->setVideoSurfaceTexture((void *)(bufferProducer.get()));
}


status_t CMCCMediaPlayerManager::prepare() {
	ALOGV("prepare");
    return mPlayer->prepare();
}

status_t CMCCMediaPlayerManager::prepareAsync() {
	ALOGV("prepareAsync");
    return mPlayer->prepareAsync();
}

status_t CMCCMediaPlayerManager::start() {
    ALOGV("start");
	property_set("service.media.playstatus","running");
    return mPlayer->start();
}

status_t CMCCMediaPlayerManager::stop() {
    ALOGV("stop");
    property_set("service.media.playstatus","stopped");
    return mPlayer->stop();
}

status_t CMCCMediaPlayerManager::pause() {
    ALOGV("pause");
    property_set("service.media.playstatus","stopped");
    return mPlayer->pause();
}

bool CMCCMediaPlayerManager::isPlaying() {
    ALOGV("isPlaying");
    return mPlayer->isPlaying();
}

status_t CMCCMediaPlayerManager::seekTo(int msec) {
    ALOGV("seek to %d msec",msec);
    return mPlayer->seekTo(msec);
}

status_t CMCCMediaPlayerManager::getCurrentPosition(int *msec) {
    ALOGV("getCurrentPosition");
    status_t err = mPlayer->getCurrentPosition(msec);
    if (err != OK) {
        return err;
    }
    return OK;
}

status_t CMCCMediaPlayerManager::getDuration(int *msec) {
    ALOGV("getDuration");
    status_t err = mPlayer->getDuration(msec);
    if (err != OK) {
        *msec = 0;
        return OK;
    }
    return OK;
}

status_t CMCCMediaPlayerManager::reset() {
    ALOGV("reset");
    mPlayer->reset();
    return OK;
}

status_t CMCCMediaPlayerManager::setLooping(int loop) {
    ALOGV("setLooping");
    return mPlayer->setLooping(loop);
}

player_type CMCCMediaPlayerManager::playerType() {
    ALOGV("playerType");
    return CMCC_PLAYER;
}

status_t CMCCMediaPlayerManager::invoke(const Parcel &request, Parcel *reply) {
   return mPlayer->invoke(request, reply);
}

status_t CMCCMediaPlayerManager::setParameter(int key, const Parcel &request) {
   return mPlayer->setParameter(key, request);
}

}  // namespace android
