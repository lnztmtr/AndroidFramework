/*
**
** Copyright 2008, The Android Open Source Project
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

#ifndef ANDROID_AMSUBSOURCE_H
#define ANDROID_AMSUBSOURCE_H


#include <utils/threads.h>

#include <drm/DrmManagerClient.h>
#include <media/MediaPlayerInterface.h>
#include <media/AudioTrack.h>
#include <media/stagefright/MediaSource.h>
//#include <ui/Overlay.h>


#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>
#include <player.h>


namespace android {

    class AmSubSource : public MediaSource {
    public:
        AmSubSource();
    
        virtual status_t start(MetaData *params = NULL);
        virtual status_t stop();
    
        virtual sp<MetaData> getFormat();
    
        virtual status_t read(
                MediaBuffer **buffer, const ReadOptions *options = NULL);
       // int read_sub_data(int sub_fd, char *buf, unsigned int length);
        int addType(int index,int type);
        int initHandle(int player_pid);
        int dumpSubtitleDataInfo(amsub_info_t* amsub_info);
        int subTextOrImage(int sub_type);
        int setFileName(const char *filename);
       // status_t  find_sub_header(char *header);
        int mSubCurId;
        int mSubCurIndex;
        int64_t mLastTimeMs;
    protected:
        virtual ~AmSubSource();
    
    private:
        #define mMaxInbandSize 8
        static const size_t kMaxFrameSize;
       // sp<MetaData> mMeta[mMaxInbandSize];
        sp<MetaData> mMeta;
        sp<DataSource> mDataSource;
        off64_t mFirstFramePos;
        uint32_t mFixedHeader;
        off64_t mCurrentPos;
        int64_t mCurrentTimeUs;
        bool mStarted;
    
        int64_t mBasisTimeUs;
        int64_t mSamplesRead;
        int mSubHandle;
        int mSubNum;
        uint32_t mStartPts;
        char mFileName[512]; // for idx+sub
        int mState;

        amsub_info_t amsub_info;

        #define mSubtitleVOB      1
        #define mSubtitlePGS      2
        #define mSubtitleMKV_STR  3
        #define mSubtitleMKV_VOB  4
        #define mSubtitleSSA      5    
        #define mSubtitleDVB      6
        #define mSubtitleTMD_TXT  7
        #define mSubtitleIDX_SUB  8
    
        struct pollfd {
            int fd;
            short events;  /* events to look for */
            short revents; /* events that occurred */
        };
    
        AmSubSource(const AmSubSource &);
        AmSubSource &operator=(const AmSubSource &);
        
        
    };

}; // namespace android

#endif // ANDROID_SUBSOURCE_H

