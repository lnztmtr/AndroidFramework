
#ifndef AMLOGICEXTRATORDATASOURCE__HH
#define AMLOGICEXTRATORDATASOURCE__HH


#ifdef __cplusplus
extern "C" {
#include "libavutil/avstring.h"
#include "libavformat/avformat.h"
}

#include <media/stagefright/DataSource.h>
#include <media/stagefright/MediaErrors.h>
#include <media/MediaPlayerInterface.h>
#include <media/AudioTrack.h>
#include <sys/types.h>
#include <utils/Errors.h>
#include <utils/KeyedVector.h>
#include <utils/String8.h>
#include <utils/RefBase.h>
#include"AmlogicPlayerDataSouceProtocol.h"
namespace android
{

class AmlogicPlayerExtractorDataSource : public DataSource
{
public:
    AmlogicPlayerExtractorDataSource(AVProbeData * pb);
    AmlogicPlayerExtractorDataSource(AVIOContext * avio);
    ~AmlogicPlayerExtractorDataSource();
    virtual status_t initCheck() const;
    virtual ssize_t readAt(off64_t offset, void *data, size_t size);
    virtual status_t getSize(off64_t *size);

    int SimpleExtractorSniffer(String8 *mimeType, float *confidence, sp<AMessage> *meta) ;

    //public static
    static int RegisterExtractorSniffer(SnifferFunc func);
    static sp<DataSource> CreateFromProbeData(AVProbeData *p);
    static sp<DataSource> CreateFromPbData(AVIOContext *pb);
    int release(void);
    // for DRM
    virtual sp<DecryptHandle> DrmInitialization(const char *mime);
    virtual String8 getUri();
    virtual void getDrmInfo(sp<DecryptHandle> &handle, DrmManagerClient **client);

    AVProbeData *mAVProbeData;
private:
    int Init(void);
    AVIOContext *mAVIOContextPB;
    off64_t     mOffset;
    static Mutex gExtractorSnifferMutex;
    static List<SnifferFunc> gExtractorSniffers;
    AmlogicPlayerDataSouceProtocol * mDataSouceProtocol;
};





};//namespace android
#endif
#endif
