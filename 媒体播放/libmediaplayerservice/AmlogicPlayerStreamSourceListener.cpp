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

////#define LOG_NDEBUG 0
#define LOG_TAG "AmlogicPlayerStreamSourceListener"
#include <utils/Log.h>

#include "am_media_private.h"

#include "AmlogicPlayerStreamSourceListener.h"

#include <binder/MemoryDealer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/MediaErrors.h>
#define kWhatMoreDataQueued             = 'more'
namespace android
{

AmlogicPlayerStreamSourceListener::AmlogicPlayerStreamSourceListener(
    const sp<IStreamSource> &source,
    ALooper::handler_id id)
    : mSource(source),
      mTargetID(id),
      mEOS(false),
      mSendDataNotification(true)
{
    mSource->setListener(this);

    mMemoryDealer = new MemoryDealer(kNumBuffers * kBufferSize);
    for (size_t i = 0; i < kNumBuffers; ++i) {
        sp<IMemory> mem = mMemoryDealer->allocate(kBufferSize);
        CHECK(mem != NULL);

        mBuffers.push(mem);
    }
    valid_data_buf_num = 0;
    mSource->setBuffers(mBuffers);

}

void AmlogicPlayerStreamSourceListener::start()
{
    for (size_t i = 0; i < kNumBuffers; ++i) {
        mSource->onBufferAvailable(i);
    }
	mOffset = 0;
	mDataLen = 0;
	mOnWaitData = false;
}

void AmlogicPlayerStreamSourceListener::stop()
{
        mSource.clear();
}

void AmlogicPlayerStreamSourceListener::queueBuffer(size_t index, size_t size)
{
    QueueEntry entry;
    entry.mIsCommand = false;
    entry.mIndex = index;
    entry.mSize = size;
    entry.mOffset = 0;
	
    Mutex::Autolock autoLock(mLock);
    mQueue.push_back(entry);
    valid_data_buf_num++;
	mOffset+=size;
	mDataLen+=size;
    LOGV("queueBuffer=%d,size=%d,valid_data_buf_num=%d", index, size, valid_data_buf_num);
    if (mSendDataNotification) {
        mSendDataNotification = false;

        //   if (mTargetID != 0) {
        //       (new AMessage(kWhatMoreDataQueued, mTargetID))->post();
        //  }
    }
	if(mOnWaitData)
		mCondition.signal();
	mOnWaitData = false;
}

void AmlogicPlayerStreamSourceListener::issueCommand(
    Command cmd, bool synchronous, const sp<AMessage> &extra)
{
    CHECK(!synchronous);

    QueueEntry entry;
    entry.mIsCommand = true;
    entry.mCommand = cmd;
    entry.mExtra = extra;

    Mutex::Autolock autoLock(mLock);
    mQueue.push_back(entry);

    if (mSendDataNotification) {
        mSendDataNotification = false;

        //     if (mTargetID != 0) {
        //        (new AMessage(kWhatMoreDataQueued, mTargetID))->post();
        //    }
    }
}

ssize_t AmlogicPlayerStreamSourceListener::read(
    void *data, size_t size, sp<AMessage> *extra,int blockMode)
{
    CHECK_GT(size, 0u);

    extra->clear();

    Mutex::Autolock autoLock(mLock);

    if (mEOS) {
        return 0;
    }

    if (mQueue.empty()) {
        mSendDataNotification = true;
        mSource->onBufferAvailable(0x80000001);
        if(blockMode){
            mOnWaitData = true;
            mCondition.waitRelative(mLock, milliseconds_to_nanoseconds(10));
        }else{
            return -EWOULDBLOCK;
        }
        if (mQueue.empty())
            return -EWOULDBLOCK;
    }else{
    	mOnWaitData = false;
    }

    QueueEntry *entry = &*mQueue.begin();

    if (entry->mIsCommand) {
        switch (entry->mCommand) {
        case EOS: {
            mQueue.erase(mQueue.begin());
            entry = NULL;

            mEOS = true;
            return 0;
        }

        case DISCONTINUITY: {
            *extra = entry->mExtra;

            mQueue.erase(mQueue.begin());
            entry = NULL;

            return INFO_DISCONTINUITY;
        }

        default:
            TRESPASS();
            break;
        }
    }

    size_t copy = entry->mSize;
    if (copy > size) {
        copy = size;
    }

    memcpy(data,
           (const uint8_t *)mBuffers.editItemAt(entry->mIndex)->pointer()
           + entry->mOffset,
           copy);

    entry->mOffset += copy;
    entry->mSize -= copy;
	mDataLen-=copy;
       
    if (entry->mSize == 0) {
        valid_data_buf_num--;
        mSource->onBufferAvailable(entry->mIndex);
        LOGV("free Index=%d,valid_data_buf_num=%d", entry->mIndex, valid_data_buf_num);
        mQueue.erase(mQueue.begin());
        entry = NULL;
    }

    return copy;
}

}  // namespace android

