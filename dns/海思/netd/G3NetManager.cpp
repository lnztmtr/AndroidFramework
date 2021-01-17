#include <stdlib.h>
#include <pthread.h>
#include <cutils/log.h>

#include "hi_3g_intf.h"
#include "G3NetManager.h"
#include "G3NetStateHandler.h"

G3NetSteteManager *G3NetSteteManager::sInstance = NULL;

G3NetSteteManager *G3NetSteteManager::Instance() {
    if (!sInstance) {
        sInstance = new G3NetSteteManager();
        if(sInstance == NULL)
            return NULL;
    }
    return sInstance;
}

G3NetSteteManager::G3NetSteteManager() {
    mBroadcaster = NULL;
    mHandler = new G3NetStateHandler(this);
    if(!mHandler)
        ALOGD("new G3NetStateHandler error");
}

G3NetSteteManager::~G3NetSteteManager() {
    if(mHandler){
        delete mHandler;
        mHandler = NULL;
    }
}

int G3NetSteteManager::start()
{
    if (!mHandler) {
        ALOGD("start: G3NetStateHandler is null");
        return -1;
    }

    mHandler->start();
    return 0;
}

int G3NetSteteManager::stop()
{
    if (!mHandler) {
        ALOGD("stop: G3NetStateHandler is null");
        return -1;
    }

    if(mHandler){
        mHandler->stop();
    }
    return 0;
}
