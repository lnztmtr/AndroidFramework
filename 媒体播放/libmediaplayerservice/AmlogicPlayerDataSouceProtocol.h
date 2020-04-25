
#ifndef AMLOGICPLAYERDATASOURCE__HH
#define AMLOGICPLAYERDATASOURCE__HH

#include "am_media_private.h"

#ifdef __cplusplus
extern "C" {
#include "libavutil/avstring.h"
#include "libavformat/avformat.h"
}
#include <sys/types.h>
#include <utils/Errors.h>
#include <utils/KeyedVector.h>
#include <utils/String8.h>
#include <utils/RefBase.h>
#include <media/stagefright/DataSource.h>
namespace android
{


class AmlogicPlayerDataSouceProtocol: public RefBase
{
public:
    AmlogicPlayerDataSouceProtocol(const sp<DataSource> &source, const char *url, int flags);
    ~AmlogicPlayerDataSouceProtocol();
    char    *GetPathString();
    static sp<AmlogicPlayerDataSouceProtocol> CreateFromUrl(
        const char *uri, const KeyedVector<String8, String8> *headers);
    static sp<AmlogicPlayerDataSouceProtocol>  CreateFromFD(
        int fd, int64_t offset, int64_t length);
    static int BasicInit();
    static void RegisterAndroidHttpProt();
    sp<DecryptHandle> DrmInitialization(const char *mime);
    void getDrmInfo(sp<DecryptHandle> &handle, DrmManagerClient **client);
    static String8 mConvertUrl;

private:
    static int      data_open(URLContext *h, const char *filename, int flags);
    static int      data_read(URLContext *h, unsigned char *buf, int size);
    static int      data_write(URLContext *h, unsigned char *buf, int size);
    static int64_t  data_seek(URLContext *h, int64_t pos, int whence);
    static int      data_close(URLContext *h);
    static int      data_get_file_handle(URLContext *h);

    int     DataOpen(URLContext *h);
    int     DataRead(unsigned char *buf, int size);
    int     DataWrite(unsigned char *buf, int size);
    int64_t DataSeek(int64_t pos, int whence);
    int     DataClose();
    int     DataGetFlags();

    char    sourcestring[128];
    sp<DataSource> mSource;
    int64_t mOffset;
    int64_t mSize;

    String8 mOurl;
    int       mFLags;
#define   DSP_FLAG_CREATE_FROM_STATIC_OPEN 0x70000000
    URLContext *mURLContent;

};

}; ////namespace android
#endif

#endif


