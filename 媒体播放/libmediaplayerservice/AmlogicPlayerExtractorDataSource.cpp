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

#define LOG_NDEBUG 0
#define LOG_TAG "AmlogicPlayerStreamSource"
#include <utils/Log.h>
#include <media/stagefright/MetaData.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include"AmlogicPlayerExtractorDataSource.h"

#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/FileSource.h>
#include <media/stagefright/MediaErrors.h>
#include <utils/String8.h>

#include <cutils/properties.h>
namespace android
{

AmlogicPlayerExtractorDataSource::AmlogicPlayerExtractorDataSource(AVProbeData * pb)
{
    Init();
    mAVProbeData = pb;
    if (pb->s != NULL) {
        URLContext *url = (URLContext *)pb->s->opaque;
        if (url && url->prot && !strcmp(url->prot->name, "DataSouce")) {
            mDataSouceProtocol = (AmlogicPlayerDataSouceProtocol*)ffurl_get_file_handle(url);
        }
    }
}
AmlogicPlayerExtractorDataSource::AmlogicPlayerExtractorDataSource(AVIOContext *avio)
{
    Init();
    mAVIOContextPB = avio;
    if (avio->opaque != NULL) {
        URLContext *url = (URLContext *)avio->opaque;
        if (url && url->prot && !strcmp(url->prot->name, "DataSouce")) {
            mDataSouceProtocol = (AmlogicPlayerDataSouceProtocol*)ffurl_get_file_handle(url);
        };
    }
}
AmlogicPlayerExtractorDataSource::~AmlogicPlayerExtractorDataSource()
{
}

Mutex AmlogicPlayerExtractorDataSource::gExtractorSnifferMutex;
List<AmlogicPlayerExtractorDataSource::SnifferFunc> AmlogicPlayerExtractorDataSource::gExtractorSniffers;

//static
int AmlogicPlayerExtractorDataSource::RegisterExtractorSniffer(SnifferFunc func)
{
    Mutex::Autolock autoLock(gExtractorSnifferMutex);

    for (List<SnifferFunc>::iterator it = gExtractorSniffers.begin();
         it != gExtractorSniffers.end(); ++it) {
        if (*it == func) {
            return -1;
        }
    }

    gExtractorSniffers.push_back(func);
    return 0;
}

int AmlogicPlayerExtractorDataSource::SimpleExtractorSniffer(String8 *mimeType, float *confidence, sp<AMessage> *meta)
{
    *mimeType = "";
    *confidence = 0.0f;
    meta->clear();

    Mutex::Autolock autoLock(gExtractorSnifferMutex);
    for (List<SnifferFunc>::iterator it = gExtractorSniffers.begin();
         it != gExtractorSniffers.end(); ++it) {
        String8 newMimeType;
        float newConfidence;
        sp<AMessage> newMeta;
        if ((*it)(this, &newMimeType, &newConfidence, &newMeta)) {
            if (newConfidence > *confidence) {
                *mimeType = newMimeType;
                *confidence = newConfidence;
                *meta = newMeta;
            }
        }
    }

    LOGV("confidence=%.02f\n", *confidence);
    return *confidence > 0.0;
}

int AmlogicPlayerExtractorDataSource::Init(void)
{
    mOffset = 0;
    mAVProbeData = NULL;
    mAVIOContextPB = NULL;
    mDataSouceProtocol = NULL;
    return 0;
}
// static
sp<DataSource> AmlogicPlayerExtractorDataSource::CreateFromProbeData(AVProbeData *pb)
{
    return new AmlogicPlayerExtractorDataSource(pb);
}


// static
sp<DataSource> AmlogicPlayerExtractorDataSource::CreateFromPbData(AVIOContext *pb)
{
    return new AmlogicPlayerExtractorDataSource(pb);
}


status_t AmlogicPlayerExtractorDataSource::initCheck() const
{
    if (mAVProbeData == NULL && mAVIOContextPB == NULL) {
        return  NO_INIT;
    } else {
        return OK;
    }
}
ssize_t AmlogicPlayerExtractorDataSource::readAt(off64_t offset, void *data, size_t size)
{
    if (mAVProbeData != NULL) {
        int len = size;
        if (offset >= mAVProbeData->buf_size) {
            return 0;    //out of data range.
        }
        if (len > (mAVProbeData->buf_size - offset)) {
            len = (mAVProbeData->buf_size - offset);
        }
        memcpy(data, mAVProbeData->buf, len);
        mOffset = offset + len;
        return len;
    } else if (mAVIOContextPB != NULL) {
        if (mOffset != offset) {
            mOffset = avio_seek(mAVIOContextPB, offset, SEEK_SET);
        }
        return (ssize_t)avio_read(mAVIOContextPB, (unsigned char *)data, size);
    }
    return NO_INIT;
}
status_t AmlogicPlayerExtractorDataSource::getSize(off64_t *size)
{
    *size = 0;
    if (mAVProbeData != NULL) {
        *size = mAVProbeData->buf_size;
    } else if (mAVIOContextPB != NULL) {
        *size = avio_size(mAVIOContextPB);
    } else {
        return NO_INIT;
    }
    if (*size > 0) {
        return OK;
    } else {
        return ERROR_UNSUPPORTED;
    }
}



sp<DecryptHandle> AmlogicPlayerExtractorDataSource::DrmInitialization(const char *mime)
{
    if (mDataSouceProtocol != NULL) {
        return mDataSouceProtocol->DrmInitialization(mime);
    } else {
        return NULL;
    }
}
void AmlogicPlayerExtractorDataSource::getDrmInfo(sp<DecryptHandle> &handle, DrmManagerClient **client)
{
    if (mDataSouceProtocol != NULL) {
        mDataSouceProtocol->getDrmInfo(handle, client);
    }
}
int AmlogicPlayerExtractorDataSource::release(void)
{
    return  Init();
}
String8 AmlogicPlayerExtractorDataSource::getUri()
{
    if (mDataSouceProtocol != NULL) {
        //LOGV("[%s]url=%s", __FUNCTION__, String8(AmlogicPlayerDataSouceProtocol::mConvertUrl).string());
        return String8(AmlogicPlayerDataSouceProtocol::mConvertUrl);
    }
    return String8();
}

};

