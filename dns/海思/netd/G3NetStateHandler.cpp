#include <stdlib.h>
#include <pthread.h>
#include <unistd.h>
#define LOG_TAG "G3NetStateHandler"
#include <cutils/log.h>
#include <errno.h>
#include <sys/eventfd.h>
#include <string.h>

#include "G3NetStateHandler.h"
#include "ResponseCode.h"
#include "hi_3g_intf.h"

#define CHECK_AND_FREE_POINTER(ptr) \
        do {                        \
            if(ptr) {               \
                free(ptr);          \
                ptr = NULL;         \
            }                       \
        } while(0)

#define DEBUG true
#define GAP_PRINT 20
#define MSG_SIZE 255

#define ERR  "3G error:"
#define WARN "3G warning:"

#define CHECK_NULL_POINTER(ptr, ret, msg)  \
        do {                               \
            if(!(ptr)){                    \
                ALOGD(ERR msg);            \
                return ret;              \
            }                              \
        } while(0)

const int G3NetStateHandler::MAX_CMD_RETRIES = 5;
const int G3NetStateHandler::G3_CARD_ADD     = 0x12345678;
const int G3NetStateHandler::G3_CARD_REMOVE  = 0x87654321;

G3NetStateHandler::G3NetStateHandler(G3NetSteteManager *nm)
{
    mGm = nm;
    mRetryTimes = 0;
    evfd = eventfd(0, EFD_NONBLOCK|EFD_CLOEXEC);
    if (evfd < 0) {
        ALOGD("eventfd failed:%s, G3NetStateHandler return",strerror(errno));
        return;
    }
}

G3NetStateHandler::~G3NetStateHandler() {
    CHECK_AND_FREE_POINTER(mAddMsg);
    close(evfd);
    FD_ZERO(&readfds);
}

int G3NetStateHandler::start()
{
    mThreadIsRuning = true;
    mIsFirstNotify  = true;
    mRetryTimes = 0;
    mLastG3NetState = G3_CARD_REMOVE;

    if (pthread_create(&mThread, NULL, G3NetStateMonitor, this)) {
        ALOGE("pthread_create fail: %s", strerror(errno));
        return -1;
    }

    return 0;
}

int G3NetStateHandler::stop()
{
    mThreadIsRuning = false;
    unsigned long long num = 0x1234;
    int ret;
    char msg[MSG_SIZE];

    ret = write(evfd, &num, sizeof(num));
    if (ret != sizeof(num))
        ALOGD("stop: write error");

    if (pthread_join(mThread, NULL)) {
        ALOGE("Error joining to listener thread: %s", strerror(errno));
        return -1;
    }

    mLastG3NetState = G3_CARD_REMOVE;
    snprintf(msg, sizeof(msg), "Iface removed %s", "3G");
    mGm->getBroadcaster()->sendBroadcast(ResponseCode::InterfaceChange,
        msg, false);

    return 0;
}

void G3NetStateHandler::notifyG3CardAdded(void *hiCard, const char *name)
{
    char msg[MSG_SIZE];
    HI_3G_CARD_S *card;
    char tmp1[64] = {0};

    CHECK_NULL_POINTER(hiCard, , "notifyG3CardAdded(null):hiCard");
    CHECK_NULL_POINTER(name, , "notifyG3CardAdded(null):name");

    if (++mRetryTimes < 10) {
        if(mIsFirstNotify && hiCard){
            mAddMsg = (char *)malloc(MSG_SIZE);
            if (!mAddMsg) {
                ALOGD("malloc failed in notifyG3CardAdded");
                return;
            }

            memset(mAddMsg, 0, MSG_SIZE);

            card = reinterpret_cast<HI_3G_CARD_S *>(hiCard);
            if(initCard(card)){
                ALOGD(ERR "initCard failed");
                return;
            }
            snprintf(mAddMsg, MSG_SIZE, "Iface added %s_%s", name, card->amanufacturer);
            HI_3G_DeInitCard(card);
            mIsFirstNotify = false;
        }

        sscanf(mAddMsg, "Iface added 3G_%[^_]", tmp1);
        snprintf(mAddMsg, MSG_SIZE, "Iface added 3G_%s_%d", tmp1, mRetryTimes);
        ALOGD("notifyG3CardAdded(): %s", mAddMsg);
        mGm->getBroadcaster()->sendBroadcast(ResponseCode::InterfaceChange,
                mAddMsg, false);

        mLastG3NetState = G3_CARD_ADD;
        asyncTimer(2000);
    } else if(mLastG3NetState == G3_CARD_REMOVE) {
        CHECK_AND_FREE_POINTER(mAddMsg);

        if (hiCard) {
            card = reinterpret_cast<HI_3G_CARD_S *>(hiCard);

            initCard(card);

            snprintf(msg, sizeof(msg), "Iface added %s_%s", name, card->amanufacturer);
            HI_3G_DeInitCard(card);
            ALOGD("notifyG3CardAdded(): %s", msg);
            mGm->getBroadcaster()->sendBroadcast(ResponseCode::InterfaceChange,
                    msg, false);
            mLastG3NetState = G3_CARD_ADD;
        }
    }
}

void G3NetStateHandler::notifyG3CardRemoved(const char *name)
{
    char msg[MSG_SIZE];
    if(mLastG3NetState == G3_CARD_ADD){
        snprintf(msg, sizeof(msg), "Iface removed %s", name);
        mGm->getBroadcaster()->sendBroadcast(ResponseCode::InterfaceChange,
            msg, false);
        mLastG3NetState = G3_CARD_REMOVE;
    }
}

void* G3NetStateHandler::G3NetStateMonitor(void *obj)
{
    G3NetStateHandler *handler = reinterpret_cast<G3NetStateHandler *>(obj);
    handler->runMonitor(NULL);
    pthread_exit(NULL);
    return NULL;
}

int G3NetStateHandler::asyncTimer(int msec)
{
    struct timeval tv;
    int ret;
    unsigned long long num;

    if (msec >= 1000) {
        tv.tv_sec = msec/1000;
        tv.tv_usec = (msec%1000)*1000;
    } else {
        tv.tv_sec = 0;
        tv.tv_usec = msec*1000;
    }

    FD_ZERO(&readfds);
    FD_SET(evfd, &readfds);
    ret = select(evfd+1, &readfds, NULL, NULL, &tv);
    if (ret < 0) {
        ALOGD("asyncTimer: select error");
        return -1;
    } else if (ret > 0) {
        FD_ISSET(evfd, &readfds);
        ret = read(evfd, &num, sizeof(num));
        if (ret != sizeof(num)) {
            ALOGD("asyncTimer: read error %d", ret);
            return -1;
        }
        ALOGD("asyncTimer: recv %lld", num);
    }
    return 0;
}

int G3NetStateHandler::initCard(void  *card_)
{
    HI_3G_CARD_S* card = (HI_3G_CARD_S*)card_;
    int ret = -1;
    int ix = 0;

    CHECK_NULL_POINTER(card, -1, "initCard(null)");

    /* for HUAWEI E303s, should try several times, make sure init card ok*/
    do {
        ret = HI_3G_InitCard(card);
        if (!ret)
            break;

        asyncTimer(1000);
    } while (++ix < MAX_CMD_RETRIES);

    if (ix >= MAX_CMD_RETRIES) {
        ALOGD("initCard failed, ret:%d\n", ret);
        return -1;
    }

    ALOGD(" Card Information Print Out :\n");
    ALOGD(" %-20s: %s \n","Card amanufacturer", card->amanufacturer);
    ALOGD(" %-20s: %s \n","Card aproductname", card->aproductname);
    ALOGD(" %-20s: %s \n","Card adevice", card->adevice);
    ALOGD(" %-20s: %s \n","Card apcui", card->apcui);
    ALOGD("netd: Init card Success, retry times:%d.\n", ix);

    return 0;
}

int G3NetStateHandler::scan3GCard(void *hiCard, int iMaxCardNum, int *iCardNum, int cardStatus)
{
    CHECK_NULL_POINTER(hiCard, -1, "scan3GCard(null)");

    HI_3G_CARD_S *card = reinterpret_cast<HI_3G_CARD_S *>(hiCard);

    if (cardStatus != G3_CARD_ADD) {
        return HI_3G_ScanCard(card, iMaxCardNum, iCardNum);
    } else {
        if (!access(card->adevice, F_OK) && !access(card->apcui, F_OK))
            return 0;
        else {
            ALOGD("scan3GCard(): card is removed!");
            memset(card->adevice, 0, sizeof(card->adevice));
            memset(card->apcui, 0, sizeof(card->apcui));
            return -1;
        }
    }

    return -1;
}

void * G3NetStateHandler::runMonitor(void *arg)
{
    HI_3G_CARD_S *hiCard;
    int iMaxCardNum = MAX_CARD_NUM;
    int iCardNum = 0;
    int iRet = 0;
    int ix = 0;

    hiCard = (HI_3G_CARD_S *)malloc(sizeof(HI_3G_CARD_S) * iMaxCardNum);
    if (hiCard == NULL) {
        ALOGE("malloc memory for cards failed!\n");
        mThreadIsRuning = false;
    }

    int counter = 0;

    while (mThreadIsRuning) {
        iRet = scan3GCard(hiCard, iMaxCardNum, &iCardNum, mLastG3NetState);

        if (iRet != 0) {
            if (counter>=GAP_PRINT) {
                ALOGE("scan 3g card failed(ret=%d)\n", iRet);
                counter = 0;
            }
            counter++;
            //if(mLastG3NetState == G3_CARD_ADD)
                //HI_3G_DeInitCard(hiCard);
            notifyG3CardRemoved("3G");
        } else {
            if (counter>=GAP_PRINT) {
                ALOGI("found 3g card \n");
                counter = 0;
            }
            counter++;
            notifyG3CardAdded(hiCard, "3G");
        }
        asyncTimer(1000);
    }

    CHECK_AND_FREE_POINTER(hiCard);
    pthread_exit(NULL);

    return NULL;
}