/*
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

#define LOG_NDEBUG 0
#define LOG_TAG "AmSubSource"
#include "utils/Log.h"
#include <stdio.h>
#include <assert.h>
#include <limits.h>
#include <unistd.h>
#include <fcntl.h>
#include <sched.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <utils/String8.h>

#include <AmSubSource.h>
extern "C"
{
    #include "stdio.h"
};


#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)


#include <cutils/properties.h>

// ----------------------------------------------------------------------------



namespace android {
#ifndef MIN
#define MIN(x,y) ((x)<(y)?(x):(y))
#endif

#define INIT_SUB_SIZE 1024
AmSubSource::AmSubSource()
    : mDataSource(NULL),
      mFirstFramePos(-1),
      mFixedHeader(0),
      mCurrentPos(0),
      mCurrentTimeUs(0),
      mLastTimeMs(0),
      mStarted(false),
      mBasisTimeUs(0),
      mSamplesRead(0) {
      mSubCurId=-1;
      mSubNum=0;
      mSubHandle=-1;
      mStartPts = 0;
      memset(&amsub_info, 0, sizeof(amsub_info));
      memset(mFileName,0,sizeof(mFileName));
      mState = 0;

      mMeta = new MetaData;
      mMeta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_TEXT_AMSUB);

      LOGE("AmSubSource::AmSubSource\n");
      //mMeta=new MetaData;
      //mMeta->setCString(kKeyMIMEType, MEDIA_MIMETYPE_TEXT_3GPP);
}

AmSubSource::~AmSubSource() {
    ALOGD("delete ~AmSubSource\n");
    if (mStarted) {
        mSubHandle=-1;
        mSubCurId = -1;
        mMeta->clear();
        stop();     
    }
}

status_t AmSubSource::start(MetaData *) {
    //CHECK(!mStarted);
    //open amstreamer fd

    //sub_handle=open_sub_device();
    LOGE("AmSubSource::start,sub_handle:%d \n",mSubHandle);
    mStarted = true;
    return OK;
}

status_t AmSubSource::stop() {
    //CHECK(mStarted);
    // if(sub_handle>=0)
        //close_sub_device(sub_handle);
    
    LOGE("AmSubSource::stop-sub_handle=%d\n", mSubHandle);
    mStarted = false;
    memset(mFileName,0,sizeof(mFileName));
    mState = 0;
    mLastTimeMs = 0;
    return OK;
}

sp<MetaData> AmSubSource::getFormat() {

    return mMeta;
    
}

int AmSubSource::setFileName(const char *filename){
    int ret = 0;
    LOGE("AmSubSource::setFileName,filename=%s !\n",filename);
    strcpy(mFileName,filename);
    
    return 0;
}



/*
type 1: 3gpp
type 2 :---
*/
/*
int AmSubSource::addType(int index,int type)
{
    mSubNum++;
    LOGE("---SubSource::addType---sub_num=%d---\n",mSubNum);
    if(mSubNum <= 8){
        mMeta[mSubNum-1]=new MetaData;
        mMeta[mSubNum-1]->setCString(kKeyMIMEType, MEDIA_MIMETYPE_TEXT_AMSUB);
    }
    if(mSubCurId==-1)
        mSubCurId=0;
    return 0;
}
*/
int AmSubSource::initHandle(int player_pid)
{
    mSubHandle = player_pid;
    LOGE("SubSource::inithandle,sub_handle=%d\n",mSubHandle);
    return 0;

}


int AmSubSource::dumpSubtitleDataInfo(amsub_info_t* amsub_info)
{
    LOGE("\n\n subtitle data info:\n");
    LOGE("sub_type     :  %d--\n",amsub_info->sub_type);
    LOGE("sub_pts      :  %u--\n",amsub_info->pts);
    LOGE("sub_delay    :  %u--\n",amsub_info->m_delay);
    LOGE("sub_width    :  %d--\n",amsub_info->sub_width);
    LOGE("sub_height   :  %d--\n",amsub_info->sub_height);
    LOGE("sub_buf_size :  %d--\n",amsub_info->buffer_size);
    LOGE("amsub_data   :  %p---\n",amsub_info->odata);

    return 0;

}

int AmSubSource::subTextOrImage(int sub_type)
{
    switch (sub_type){
        case mSubtitleVOB:
        case mSubtitlePGS:
        case mSubtitleDVB:
        case mSubtitleIDX_SUB:
            return 1;
        case mSubtitleSSA:
        case mSubtitleTMD_TXT:
            return 0;
        default:
            return 1;
    }
}


status_t AmSubSource::read(
        MediaBuffer **out, const ReadOptions *options) 
{

    *out = NULL;
    
    int ret=0;
    int i;
    unsigned sync_bytes=0x414d4c55;
    int64_t mStartTimeUs = 0;
    int64_t mEndTimeUs = 0;
    int mSubType = -1;

    int cur_sub_id = -1;
    
    LOGE("AmSubSource::read,sub_handle=%d\n",mSubHandle);
    if(mSubHandle<0){
        return WOULD_BLOCK;
    }

    memset(&amsub_info, 0, sizeof(amsub_info));

    //add for idx+sub
    if(strlen(mFileName) && !mState){
        ret = player_set_sub_filename(mSubHandle,mFileName);
        if(ret!=0){
            LOGE("%s,setFileName failed\n",__FUNCTION__);
            return WOULD_BLOCK;
        }
        mState = 1;
    }

    if(strlen(mFileName) && mState){
        player_get_current_time(mSubHandle,&amsub_info.curr_timeMs);
        LOGE("%s,amsub_info.curr_timeMs=%d !\n",__FUNCTION__,amsub_info.curr_timeMs);
        
        if(amsub_info.curr_timeMs == 0){
            return WOULD_BLOCK;
        }
    }

    if(!strlen(mFileName)){

        //selectrack not show previous subtitle
        player_get_curr_sub_id(mSubHandle,&cur_sub_id);

        LOGE("%s,cur_sub_id=%d,mSubCurId=%d\n",__FUNCTION__,cur_sub_id,mSubCurId);
        if(mSubCurId < 0 || cur_sub_id != mSubCurId){
            return WOULD_BLOCK;
        }
    }
    

    //get the video player start pts to sync
    
    if(mStartPts==0){
        ret = player_get_sub_start_pts(mSubHandle,&mStartPts);
        LOGE("%s,mStartPts=%u !\n",__FUNCTION__,mStartPts);
        if(ret != 0){
            LOGE("%s,not get subtitle start pts!\n",__FUNCTION__);
            return WOULD_BLOCK;
        }
    }

    
    //get the subtitle decode data 
    ret = player_get_sub_odata(mSubHandle,&amsub_info);
    if(ret!=0){
        LOGE("%s,get data failed,return WOULD_BLOCK!\n",__FUNCTION__);

        return WOULD_BLOCK;
    }

    dumpSubtitleDataInfo(&amsub_info);

    //according different sub type to get starttime and endtime
    if((amsub_info.sub_type == mSubtitleSSA) || (amsub_info.sub_type == mSubtitleIDX_SUB)){
        mStartTimeUs = (int64_t)amsub_info.pts; 
        mEndTimeUs = (int64_t)amsub_info.m_delay;
    }else if(amsub_info.m_delay > 0){
        mStartTimeUs = (int64_t)amsub_info.pts - (int64_t)mStartPts;
        mEndTimeUs = (int64_t)amsub_info.m_delay - (int64_t)mStartPts;
    }else{
        mStartTimeUs = (int64_t)amsub_info.pts - (int64_t)mStartPts;
        mEndTimeUs = (int64_t)amsub_info.m_delay;
    }
    mStartTimeUs = mStartTimeUs*100/9;
    mEndTimeUs = mEndTimeUs*100/9;

    if(mStartTimeUs <= 0 || (amsub_info.m_delay !=0 && (amsub_info.pts > amsub_info.m_delay))){
        LOGE("%s,get the wrong subtitle para!\n",__FUNCTION__);
        if(amsub_info.odata){
            free(amsub_info.odata);
            amsub_info.odata = NULL;
        }
        return WOULD_BLOCK; 
    }

    mSubType = subTextOrImage(amsub_info.sub_type);

    if(mSubType && ((amsub_info.sub_width==0 || amsub_info.sub_height==0)&&(amsub_info.buffer_size > 0))){

        LOGE("%s,get the wrong subtitle para!\n",__FUNCTION__);
        if(amsub_info.odata){
            free(amsub_info.odata);
            amsub_info.odata = NULL;
        }
        return WOULD_BLOCK; 
    }

    mLastTimeMs = mStartTimeUs/1000;

    //copy data and subtitle info to MediaBuffer
    MediaBuffer *buffer=new MediaBuffer(amsub_info.buffer_size);

    memcpy(buffer->data(),(char*)amsub_info.odata,amsub_info.buffer_size);

    buffer->meta_data()->setInt32(kKeyFileType,mSubType);
    buffer->meta_data()->setInt64(kKeyTime,mStartTimeUs);
    buffer->meta_data()->setInt64(kKeyDuration,mEndTimeUs);

    LOGE("mStartTimeUs=%lld,mEndTimeUs=%lld-size=%d,mSubType=%d-\n",mStartTimeUs,mEndTimeUs,amsub_info.buffer_size,mSubType);

    if(mSubType){
        buffer->meta_data()->setInt32(kKeyWidth,amsub_info.sub_width);
        buffer->meta_data()->setInt32(kKeyHeight,amsub_info.sub_height);
    }
    
    *out=buffer;
    //LOGE("---amsub_info.odata=%p--\n",amsub_info.odata);
    if(amsub_info.odata){
        free(amsub_info.odata);
        amsub_info.odata = NULL;
    }
    
    return OK;

}

}
