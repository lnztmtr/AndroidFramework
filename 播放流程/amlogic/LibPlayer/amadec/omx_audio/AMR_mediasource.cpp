
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <android/log.h>


#include <cutils/properties.h>
#include "AMR_mediasource.h"
#include "audio-dec.h"
extern "C" int read_buffer(unsigned char *buffer,int size);

#define LOG_TAG "AMRExtractor"

#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)



namespace android {


static size_t getFrameSize(bool isWide, unsigned FT){
    static const size_t kFrameSizeNB[16] = {
        95, 103, 118, 134, 148, 159, 204, 244,
        39, 43, 38, 37, // SID
        0, 0, 0, // future use
        0 // no data
    };
    static const size_t kFrameSizeWB[16] = {
        132, 177, 253, 285, 317, 365, 397, 461, 477,
        40, // SID
        0, 0, 0, 0, // future use
        0, // speech lost
        0 // no data
    };

    if (FT > 15 || (isWide && FT > 9 && FT < 14) || (!isWide && FT > 11 && FT < 15)) {
        ALOGE("illegal AMR frame type %d", FT);
        return 0;
    }

    size_t frameSize = isWide ? kFrameSizeWB[FT] : kFrameSizeNB[FT];

    // Round up bits to bytes and add 1 for the header byte.
    frameSize = (frameSize + 7) / 8 + 1;

    return frameSize;
}
int AMR_mediasource::set_Amr_MetaData(aml_audio_dec_t *audec){

    sample_rate=audec->samplerate;
    ChNum      =audec->channels;
    mMeta->setInt32(kKeySampleRate,audec->samplerate );
    mMeta->setInt32(kKeyChannelCount, audec->channels);
    mMeta->setInt32(kKeyBitRate,audec->bitrate);
    mMeta->setInt32(kKeyCodecID,audec->codec_id);

    ALOGI("Amr-->channels/%d samplerate/%d bitrate/%d  codec_ID/0x%x extradata_size/%d block_align/%d\n",ChNum,sample_rate,
    audec->bitrate,audec->codec_id,audec->extradata_size,block_align);
    if (audec->codec_id == CODEC_ID_AMR_NB_OMX) {
       mIsWide = 0;
       mMeta->setCString(kKeyMIMEType,"audio/3gpp");
   }
   if (audec->codec_id == CODEC_ID_AMR_WB_OMX) {
       mIsWide = 1;
       mMeta->setCString(kKeyMIMEType,"audio/amr-wb");
   }
    return 0;
}
int AMR_mediasource::MediaSourceRead_buffer(unsigned char *buffer,int size){
   int readcnt=0;
   int readsum=0;
   if (fpread_buffer != NULL) {
       int sleep_time=0;
       while ((readsum<size) && (*pStop_ReadBuf_Flag == 0)) {
          readcnt=fpread_buffer(buffer+readsum,size-readsum);
          if (readcnt<(size-readsum)) {
             sleep_time++;
             usleep(10000);
          }
          readsum+=readcnt;
       }
       bytes_readed_sum +=readsum;
       if (*pStop_ReadBuf_Flag == 1) {
            ALOGI("[%s] End of Stream: *pStop_ReadBuf_Flag==1\n ", __FUNCTION__);
       }
       return readsum;
   } else {
        ALOGI("[%s]ERR: fpread_buffer=NULL\n ", __FUNCTION__);
        return 0;
   }
}

AMR_mediasource::AMR_mediasource(void *read_buffer, aml_audio_dec_t *audec){
    ALOGI("[%s] in line (%d) \n",__FUNCTION__,__LINE__);
    mStarted=false;
    mMeta=new MetaData;
    mDataSource=NULL;
    mGroup=NULL;
    mCurrentTimeUs=0;
    pStop_ReadBuf_Flag=NULL;
    fpread_buffer=(fp_read_buffer)read_buffer;
    sample_rate=0;
    ChNum=0;
    frame_size=0;
    block_align=0;
    bytes_readed_sum_pre=0;
    bytes_readed_sum=0;
    frame_num =0;
    set_Amr_MetaData(audec);

}

AMR_mediasource::~AMR_mediasource() {
    ALOGI("[%s] in line (%d) \n",__FUNCTION__,__LINE__);
    if (mStarted)
        stop();
}

int AMR_mediasource::GetSampleRate(){
    return sample_rate;
}

int AMR_mediasource::GetChNum(){
   return ChNum;
}

int* AMR_mediasource::Get_pStop_ReadBuf_Flag(){
    return pStop_ReadBuf_Flag;
}

int AMR_mediasource::Set_pStop_ReadBuf_Flag(int *pStop){
    pStop_ReadBuf_Flag = pStop;
    return 0;
}

int AMR_mediasource::GetReadedBytes(){
     return block_align;
}

sp<MetaData> AMR_mediasource::getFormat(){
    ALOGI("[%s] in line (%d) \n",__FUNCTION__,__LINE__);
    return mMeta;
}

status_t AMR_mediasource::start(MetaData *params){
    ALOGI("[%s] in line (%d) \n",__FUNCTION__,__LINE__);
    mGroup = new MediaBufferGroup;
    mGroup->add_buffer(new MediaBuffer(128));
    mStarted = true;
    return OK;
}

status_t AMR_mediasource::stop(){
    ALOGI("[%s] in line (%d) \n",__FUNCTION__,__LINE__);
    delete mGroup;
    mGroup = NULL;
    mStarted = false;
    return OK;
}

status_t AMR_mediasource::read(
        MediaBuffer **out, const ReadOptions *options) {
    *out = NULL;
    if (*pStop_ReadBuf_Flag == 1) {
         ALOGI("Stop_ReadBuf_Flag==1 stop read_buf [%s %d]",__FUNCTION__,__LINE__);
         return ERROR_END_OF_STREAM;
    }
    uint8_t header;
    if (MediaSourceRead_buffer(&header,1) != 1) {
        return ERROR_END_OF_STREAM;
    }
    if (header & 0x83) {
        // Padding bits must be 0.

        ALOGE("padding bits must be 0, header is 0x%02x", header);

        return ERROR_MALFORMED;
    }

    unsigned FT = (header >> 3) & 0x0f;

    size_t frameSize = getFrameSize(mIsWide, FT);
    if (frameSize == 0) {
        return ERROR_MALFORMED;
    }
    block_align = frameSize;
    MediaBuffer *buffer;
    status_t err = mGroup->acquire_buffer(&buffer);
    if (err != OK) {
        return err;
    }
   memcpy(buffer->data(),&header,1);
   if (block_align -1 > 0) {
       if (MediaSourceRead_buffer((unsigned char*)(buffer->data()+1),block_align -1) != block_align -1) {
           buffer->release();
           buffer = NULL;
           return ERROR_END_OF_STREAM;
       }
    }
    buffer->set_range(0, block_align);
    buffer->meta_data()->setInt64(kKeyTime, mCurrentTimeUs);

    *out = buffer;
    return OK;
}

}  // namespace android
