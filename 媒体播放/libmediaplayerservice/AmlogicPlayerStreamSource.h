
#ifndef AMLOGICPLAYERSTREAMSOURCE__HH
#define AMLOGICPLAYERSTREAMSOURCE__HH


#ifdef __cplusplus
extern "C" {
#include "libavutil/avstring.h"
#include "libavformat/avformat.h"
}
#include "AmlogicPlayerStreamSourceListener.h"
namespace android
{

#include <sys/types.h>
#include <utils/Errors.h>
#include <utils/KeyedVector.h>
#include <utils/String8.h>
#include <utils/RefBase.h>


class AmlogicPlayerStreamSource : public RefBase
{

public:
    AmlogicPlayerStreamSource(const sp<IStreamSource> &source);
    virtual               ~AmlogicPlayerStreamSource();
    static int            init(void);
    char                *GetPathString();

private:
    static int          amopen(URLContext *h, const char *filename, int flags);
    static int          amread(URLContext *h, unsigned char *buf, int size);
    static int          amwrite(URLContext *h, unsigned char *buf, int size);
    static int64_t    amseek(URLContext *h, int64_t pos, int whence);
    static int          amclose(URLContext *h);
    static int          get_file_handle(URLContext *h);
    ///*-------------------------------------------------------*//
    int              Source_open();
    int              Source_read(unsigned char *buf, int size);
    int              Source_write(unsigned char *buf, int size);
    int64_t        Source_seek(int64_t pos, int whence);
    int              Source_close();

private:
    sp<IStreamSource> mSource;
    sp<AmlogicPlayerStreamSourceListener> mStreamListener;
    char sourcestring[128];
    Mutex mMoreDataLock;
    Condition   mWaitCondition;
    char localbuf[188];
    int    localdatasize;
    int64_t  pos;
    int dropdatalen;

};


}////namespace android
#endif

#endif
