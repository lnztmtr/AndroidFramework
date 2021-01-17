

#ifndef _3GNETSTATEHANDLER_H
#define _3GNETSTATEHANDLER_H
#include <sys/time.h>
#include <sys/types.h>
#include <unistd.h>
#include "G3NetManager.h"

class G3NetStateHandler
{
    G3NetSteteManager *mGm;
    bool mThreadIsRuning;
    bool mIsFirstNotify;
    int  mLastG3NetState;
    int  mRetryTimes;
    char *mAddMsg;
    pthread_t mThread;
public:
    G3NetStateHandler(G3NetSteteManager *nm);
    virtual ~G3NetStateHandler();

    int start(void);
    int stop(void);
    static const int MAX_CMD_RETRIES;
    static const int G3_CARD_ADD;
    static const int G3_CARD_REMOVE;
protected:

    void notifyG3CardAdded(void *hiCard, const char *name);
    void notifyG3CardRemoved(const char *name);
    int  scan3GCard(void *hiCard, int iMaxCardNum, int *iCardNum, int cardStatus);
    int  asyncTimer(int msec);
private:
    static void* G3NetStateMonitor(void *arg);
    void* runMonitor(void *arg);
    int initCard(void *card);
    int evfd;
    fd_set readfds;
};

#endif
