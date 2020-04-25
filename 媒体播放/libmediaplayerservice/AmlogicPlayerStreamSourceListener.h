/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef AmlogicPlayer_STREAM_LISTENER_H_

#define AmlogicPlayer_STREAM_LISTENER_H_

#include <media/MediaPlayerInterface.h>
#include <media/stagefright/foundation/AHandler.h>
#include <media/IStreamSource.h>

namespace android
{

struct MemoryDealer;

struct  AmlogicPlayerStreamSourceListener : public BnStreamListener {
    AmlogicPlayerStreamSourceListener(
        const sp<IStreamSource> &source,
        ALooper::handler_id targetID);

    virtual void queueBuffer(size_t index, size_t size);

    virtual void issueCommand(
        Command cmd, bool synchronous, const sp<AMessage> &extra);

    void start();
    void stop();
    ssize_t read(void *data, size_t size, sp<AMessage> *extra,int blockMode);

private:
    enum {
        kNumBuffers = 8,
        kBufferSize = 188 * 10
    };

    struct QueueEntry {
        bool mIsCommand;

        size_t mIndex;
        size_t mSize;
        size_t mOffset;

        Command mCommand;
        sp<AMessage> mExtra;
    };

    Mutex mLock;
    int valid_data_buf_num;
    sp<IStreamSource> mSource;
    ALooper::handler_id mTargetID;
    sp<MemoryDealer> mMemoryDealer;
    Vector<sp<IMemory> > mBuffers;
    List<QueueEntry> mQueue;
    bool mEOS;
    bool mSendDataNotification;
	int64_t mOffset;
	int64_t mDataLen;
	Condition mCondition;
	bool mOnWaitData;
    DISALLOW_EVIL_CONSTRUCTORS(AmlogicPlayerStreamSourceListener);
};

}  // namespace android

#endif // NUPLAYER_STREAM_LISTENER_H_

