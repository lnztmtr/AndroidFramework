#ifndef AMR_EXTRACTOR_H_

#define AMR_EXTRACTOR_H_

#include  "MediaSource.h"
#include  "DataSource.h"
#include  "MediaBufferGroup.h"
#include  "MetaData.h"
#include  "audio_mediasource.h"
#include  "../audio-dec.h"

#define CODEC_ID_AMR_NB_OMX 0x12000
#define CODEC_ID_AMR_WB_OMX 0x12001

namespace android {
typedef int (*fp_read_buffer)(unsigned char *, int);
struct AMessage;
class String8;
#define OFFSET_TABLE_LEN    300

class AMR_mediasource : public AudioMediaSource {
public:
    AMR_mediasource(void *read_buffer, aml_audio_dec_t *audec);

    status_t start(MetaData *params = NULL);
    status_t stop();
    sp<MetaData> getFormat();
    status_t read(MediaBuffer **buffer, const ReadOptions *options = NULL);
    int  GetReadedBytes();
    int GetSampleRate();
    int GetChNum();
    int* Get_pStop_ReadBuf_Flag();
    int Set_pStop_ReadBuf_Flag(int *pStop);

    int set_Amr_MetaData(aml_audio_dec_t *audec);
    int MediaSourceRead_buffer(unsigned char *buffer, int size);


    fp_read_buffer fpread_buffer;

     int sample_rate;
    int ChNum;
    int frame_size;
    int *pStop_ReadBuf_Flag;
    int extradata_size;
    int64_t bytes_readed_sum_pre;
    int64_t bytes_readed_sum;
    int frame_num;
    bool mIsWide;
protected:
    virtual ~AMR_mediasource();

private:
    sp<DataSource> mDataSource;
    sp<MetaData> mMeta;
    MediaBufferGroup *mGroup;
    bool mStarted;

    int64_t mCurrentTimeUs;
    int     mBytesReaded;
    int block_align;
    off64_t mOffsetTable[OFFSET_TABLE_LEN]; //5 min
    size_t mOffsetTableLength;

    AMR_mediasource(const AMR_mediasource &);
    AMR_mediasource &operator=(const AMR_mediasource &);
};

}  // namespace android

#endif  // AMR_EXTRACTOR_H_
