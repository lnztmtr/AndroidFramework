
#ifndef _AMCTCDATASOURCE__H_
#define _AMCTCDATASOURCE__H_

#include "am_media_private.h"

#ifdef __cplusplus

extern "C" {
#include "ctc_url.h"
}
#include <sys/types.h>
#include <utils/Errors.h>
#include <utils/KeyedVector.h>
#include <utils/String8.h>
#include <utils/RefBase.h>
#include <media/stagefright/DataSource.h>
#include "dlfcn.h"



namespace android
{


class AmCTCDataSouceProtocol: public RefBase
{
public:
    AmCTCDataSouceProtocol(const sp<DataSource> &source, const char *url, int flags);
    ~AmCTCDataSouceProtocol();
    char    *GetPathString();
    static sp<AmCTCDataSouceProtocol>  CreateFromFD(
        int fd, int64_t offset, int64_t length);
    int BasicInit();

private:
    static int      data_open(URLContext *h, const char *filename, int flags);
    static int      data_read(URLContext *h, unsigned char *buf, int size);
    static int      data_write(URLContext *h, const unsigned char *buf, int size);
    static int64_t  data_seek(URLContext *h, int64_t pos, int whence);
    static int      data_close(URLContext *h);
    static int      data_get_file_handle(URLContext *h);

    int     DataOpen(URLContext *h);
    int     DataRead(unsigned char *buf, int size);
    int     DataWrite(const unsigned char *buf, int size);
    int64_t DataSeek(int64_t pos, int whence);
    int     DataClose();
    int     DataGetFlags();

    char    sourcestring[128];
    sp<DataSource> mSource;
    int64_t mOffset;
    int64_t mSize;

    String8 mOurl;
    int       mFLags;
    URLContext *mURLContent;
    void *dl_handle;
    //int* pffurl_register_protocol;

};



}; ////namespace android
#endif

#endif


