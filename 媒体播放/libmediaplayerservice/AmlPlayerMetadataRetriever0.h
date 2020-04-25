/*
**
** Copyright 2009, The Android Open Source Project
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

#ifndef ANDROID_AmlPlayerMetadataRetriever0_H
#define ANDROID_AmlPlayerMetadataRetriever0_H

#include <utils/threads.h>
#include <utils/Errors.h>
#include <media/MediaMetadataRetrieverInterface.h>
#include <utils/KeyedVector.h>
#include <utils/String8.h>

extern "C" {
#include "libavutil/avstring.h"
#include "libavformat/avformat.h"
}

#include <player_thumbnail.h>

typedef struct AmlogicPlayer_File0 {
    int               fd;
    int           fd_valid;
    int64_t          mOffset;
    int64_t          mLength;
} AmlogicPlayer_File0;

namespace android
{

class String8;

class AmlPlayerMetadataRetriever0 : public MediaMetadataRetrieverInterface
{
public:
    static status_t BasicInit();
    AmlPlayerMetadataRetriever0();
    virtual ~AmlPlayerMetadataRetriever0();

    virtual status_t setDataSource(const char *url, const KeyedVector<String8, String8> *headers);
    virtual status_t setDataSource(int fd, int64_t offset, int64_t length);
    virtual VideoFrame *getFrameAtTime(int64_t timeUs, int option);
    virtual MediaAlbumArt *extractAlbumArt();
    virtual const char* extractMetadata(int keyCode);

private:
    void * mClient;
    char * mFileName;
    bool mParsedMetaData;
    AmlogicPlayer_File0  mAmlogicFile;
    KeyedVector<int, String8> mMetaData;
    MediaAlbumArt *mAlbumArt;

    status_t setdatasource(const char* url, int fd, int64_t offset, int64_t length);
    void parseMetaData();

    static int          vp_open(URLContext *h, const char *filename, int flags);
    static int          vp_read(URLContext *h, unsigned char *buf, int size);
    static int          vp_write(URLContext *h, unsigned char *buf, int size);
    static int64_t      vp_seek(URLContext *h, int64_t pos, int whence);
    static int          vp_close(URLContext *h);
    static int          vp_get_file_handle(URLContext *h);

};

}

#endif
