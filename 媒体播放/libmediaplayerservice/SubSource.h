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

#ifndef ANDROID_SUBSOURCE_H
#define ANDROID_SUBSOURCE_H


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

namespace android {
    #define SUBTITLE_READ_DEVICE    "/dev/amstream_sub_read"
    #define Max_Inband_Size 8
    class SubSource : public MediaSource {
    public:
        SubSource();
    
        virtual status_t start(MetaData *params = NULL);
        virtual status_t stop();
    
        virtual sp<MetaData> getFormat();
    
        virtual status_t read(
                MediaBuffer **buffer, const ReadOptions *options = NULL);
        int read_sub_data(int sub_fd, char *buf, unsigned int length);
        int addType(int index,int type);
        status_t  find_sub_header(char *header);
        int sub_cur_id;
    protected:
        virtual ~SubSource();
    
    private:
        static const size_t kMaxFrameSize;
        sp<MetaData> mMeta[Max_Inband_Size];
        sp<DataSource> mDataSource;
        off64_t mFirstFramePos;
        uint32_t mFixedHeader;
        off64_t mCurrentPos;
        int64_t mCurrentTimeUs;
        bool mStarted;
    
        int64_t mBasisTimeUs;
        int64_t mSamplesRead;
        int sub_handle;
        int sub_num;
    
        struct pollfd {
            int fd;
            short events;  /* events to look for */
            short revents; /* events that occurred */
        };
    
        SubSource(const SubSource &);
        SubSource &operator=(const SubSource &);
        
        
    };

}; // namespace android

#endif // ANDROID_SUBSOURCE_H

