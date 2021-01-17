#ifndef _3GSTATEMONITOR_H
#define _3GSTATEMONITOR_H

#include <sysutils/SocketListener.h>
#include <sysutils/NetlinkListener.h>

class G3NetStateHandler;
class G3NetSteteManager
{
private:
    static G3NetSteteManager *sInstance;

private:
    SocketListener       *mBroadcaster;

public:
    virtual ~G3NetSteteManager();

    int start();
    int stop();

    void setBroadcaster(SocketListener *sl) { mBroadcaster = sl; }
    SocketListener *getBroadcaster() { return mBroadcaster; }

    static G3NetSteteManager *Instance();

private:
    G3NetSteteManager();
    G3NetStateHandler *mHandler;
};

#endif
