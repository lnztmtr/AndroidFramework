/*
 * Copyright (C) 2009 The Android Open Source Project
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

///#define LOG_NDEBUG 0
#define LOG_TAG "AmlPlayerMetadataRetriever0"
#include "AmlPlayerMetadataRetriever0.h"
#include <utils/Log.h>

namespace android
{

static URLProtocol android_protocol;

status_t AmlPlayerMetadataRetriever0::BasicInit()
{
    static int have_inited = 0;
    if (!have_inited) {

        URLProtocol *prot = &android_protocol;
        prot->name = "amthumb";
        prot->url_open = (int (*)(URLContext *, const char *, int))vp_open;
        prot->url_read = (int (*)(URLContext *, unsigned char *, int))vp_read;
        prot->url_write = (int (*)(URLContext *, unsigned char *, int))vp_write;
        prot->url_seek = (int64_t (*)(URLContext *, int64_t , int))vp_seek;
        prot->url_close = (int (*)(URLContext *))vp_close;
        prot->url_get_file_handle = (int (*)(URLContext *))vp_get_file_handle;
        av_register_protocol(prot);
        have_inited++;
    }
    return 0;
}

int AmlPlayerMetadataRetriever0::vp_open(URLContext *h, const char *filename, int flags)
{
    /*
    sprintf(file,"amthumb:AmlogicPlayer=[%x:%x],AmlogicPlayer_fd=[%x:%x]",
    */
    ALOGV("vp_open=%s\n", filename);
    if (strncmp(filename, "amthumb", strlen("amthumb")) == 0) {
        unsigned int fd = 0, fd1 = 0;
        char *str = strstr(filename, "AmlogicPlayer_fd");
        if (str == NULL) {
            return -1;
        }
        sscanf(str, "AmlogicPlayer_fd=[%x:%x]\n", (unsigned int*)&fd, (unsigned int*)&fd1);
        if (fd != 0 && ((unsigned int)fd1 == ~(unsigned int)fd)) {
            AmlogicPlayer_File0* af = (AmlogicPlayer_File0*)fd;
            h->priv_data = (void*) fd;
            if (af != NULL && af->fd_valid) {
                lseek(af->fd, af->mOffset, SEEK_SET);
                ALOGV("android_open %s OK,h->priv_data=%p\n", filename, h->priv_data);
                return 0;
            } else {
                ALOGV("android_open %s Faild\n", filename);
                return -1;
            }
        }
    }
    return -1;
}

int AmlPlayerMetadataRetriever0::vp_read(URLContext *h, unsigned char *buf, int size)
{
    AmlogicPlayer_File0* af = (AmlogicPlayer_File0*)h->priv_data;
    int ret;
    //ALOGV("start%s,pos=%lld,size=%d,ret=%d\n",__FUNCTION__,(int64_t)lseek(af->fd, 0, SEEK_CUR),size,ret);
    if(af->fd >= 0){
        ret = read(af->fd, buf, size);
    }else{
        ret = -1;
    }
    if(ret < 0 && af->fd >0){
        close(af->fd);
        af->fd_valid = 0;
    }
    //ALOGV("end %s,size=%d,ret=%d\n",__FUNCTION__,size,ret);
    return ret;
}

int AmlPlayerMetadataRetriever0::vp_write(URLContext *h, unsigned char *buf, int size)
{
    AmlogicPlayer_File0* af = (AmlogicPlayer_File0*)h->priv_data;

    return -1;
}

int64_t AmlPlayerMetadataRetriever0::vp_seek(URLContext *h, int64_t pos, int whence)
{
    AmlogicPlayer_File0* af = (AmlogicPlayer_File0*)h->priv_data;
    int64_t ret;
    //ALOGV("%sret=%lld,pos=%lld,whence=%d,tell=%lld\n",__FUNCTION__,(int64_t)0,pos,whence,(int64_t)lseek(af->fd,0,SEEK_CUR));
    if (whence == AVSEEK_SIZE) {
        return af->mLength;
#if 0
        struct stat filesize;
        if (fstat(af->fd, &filesize) < 0) {
            int64_t size;
            int64_t oldpos;
            oldpos = lseek(af->fd, 0, SEEK_CUR);
            if ((size = lseek(af->fd, -1, SEEK_END)) < 0) {
                return size;
            }
            size++;
            lseek(af->fd, oldpos, SEEK_SET);
            return size;
        } else {
            return filesize.st_size;
        }
#endif
    }
    switch (whence) {
    case SEEK_CUR:
    case SEEK_END:
        ret = lseek(af->fd, pos, whence);
        return ret - af->mOffset;
    case SEEK_SET:
        ret = lseek(af->fd, pos + af->mOffset, whence);
        if (ret < 0) {
            return ret;
        } else {
            return ret - af->mOffset;
        }
    default:
        return -1;
    }
    return -1;
}

int AmlPlayerMetadataRetriever0::vp_close(URLContext *h)
{
    FILE* fp = (FILE*)h->priv_data;
    ALOGV("%s\n", __FUNCTION__);
    return 0; /*don't close file here*/
    //return fclose(fp);
}

int AmlPlayerMetadataRetriever0::vp_get_file_handle(URLContext *h)
{
    ALOGV("%s\n", __FUNCTION__);
    return (intptr_t) h->priv_data;
}

AmlPlayerMetadataRetriever0::AmlPlayerMetadataRetriever0()
    : mClient(NULL),
      mFileName(NULL),
      mAlbumArt(NULL),
      mParsedMetaData(false)
{
    ALOGV("AmlPlayerMetadataRetriever0()");

    mClient = thumbnail_res_alloc();
    if (!mClient) {
        ALOGV("Thumbnail register decoder failed!\n");
    }

    AmlPlayerMetadataRetriever0::BasicInit();
    mAmlogicFile.fd_valid = 0;
}

AmlPlayerMetadataRetriever0::~AmlPlayerMetadataRetriever0()
{
    ALOGV("~AmlPlayerMetadataRetriever0()");

    delete mAlbumArt;
    mAlbumArt = NULL;

    if (mFileName) {
        free(mFileName);
        mFileName = NULL;
    }

    thumbnail_res_free(mClient);
    mClient = NULL;
    if (mAmlogicFile.fd_valid) {
        close(mAmlogicFile.fd);
    }
    mAmlogicFile.fd_valid = 0;
}

status_t AmlPlayerMetadataRetriever0::setDataSource(const char * url, const KeyedVector<String8, String8> *headers)
{
    ALOGV("setDataSource(%s)", url);
    mParsedMetaData = false;
    mMetaData.clear();
    delete mAlbumArt;
    mAlbumArt = NULL;

    return setdatasource(url, -1, 0, 0x7ffffffffffffffLL);
}

status_t AmlPlayerMetadataRetriever0::setDataSource(int fd, int64_t offset, int64_t length)
{
    ALOGV("setDataSource(%d, %lld, %lld)", fd, offset, length);
    mParsedMetaData = false;
    mMetaData.clear();

    return setdatasource(NULL, fd, offset, length);
}

status_t AmlPlayerMetadataRetriever0::setdatasource(const char* url, int fd, int64_t offset, int64_t length)
{
    char* file;
    mAmlogicFile.fd_valid = 0;

    if (url == NULL) {
        if (fd < 0 || offset < 0) {
            return -1;
        }

        file = (char *)malloc(128);
        if (file == NULL) {
            return NO_MEMORY;
        }

        mAmlogicFile.fd = dup(fd);
        mAmlogicFile.fd_valid = 1;
        mAmlogicFile.mOffset = offset;
        mAmlogicFile.mLength = length;
        sprintf(file, "amthumb:AmlogicPlayer=[%x:%x],AmlogicPlayer_fd=[%x:%x]",
                (unsigned int)this, (~(unsigned int)this), (unsigned int)&mAmlogicFile, (~(unsigned int)&mAmlogicFile));
    } else {
        file = (char *)malloc(strlen(url) + 1);
        if (file == NULL) {
            return NO_MEMORY;
        }

        strcpy(file, url);
    }

    mFileName = file;

    return OK;
}

VideoFrame *AmlPlayerMetadataRetriever0::getFrameAtTime(int64_t timeUs, int option)
{
    ALOGV("getFrameAtTime: %lld us option: %d", timeUs, option);

    int err;
    int32_t width, height;
    char *rgbbuf;
    int rotation;

    err = thumbnail_decoder_open(mClient, mFileName);
    if (err != 0) {
        ALOGV("Thumbnail decode init failed!\n");
        return NULL;
    }

    err = thumbnail_extract_video_frame(mClient, timeUs, 0);
    if (err != 0) {
        ALOGV("Thumbnail decode frame failed, give a default pic!\n");
        VideoFrame *frameDef = new VideoFrame;
        frameDef->mWidth = 640;
        frameDef->mHeight = 480;
        frameDef->mDisplayWidth = 640;
        frameDef->mDisplayHeight = 480;
        frameDef->mSize = 640 * 480 * 2;
        frameDef->mRotationAngle = 0;
        frameDef->mData = new uint8_t[frameDef->mSize];
        memset(frameDef->mData, 0, frameDef->mSize);
        memset(frameDef->mData, 0, frameDef->mSize);
        memset(frameDef->mData, 0, frameDef->mSize);
        return frameDef;
    }

    thumbnail_get_video_size(mClient, &width, &height);
    ALOGV("width: %d, height: %d \n", width, height);
    thumbnail_get_video_rotation(mClient, &rotation);
    ALOGV("rotation: %d \n", rotation);


    VideoFrame *frame = new VideoFrame;
    frame->mWidth = width;
    frame->mHeight = height;
    frame->mDisplayWidth = width;
    frame->mDisplayHeight = height;
    frame->mSize = width * height * 2;
    frame->mRotationAngle = rotation;
    frame->mData = new uint8_t[frame->mSize];

    thumbnail_read_frame(mClient, (char *)frame->mData);

    thumbnail_decoder_close(mClient);

    return frame;
}

const char* AmlPlayerMetadataRetriever0::extractMetadata(int keyCode)
{
    if (mClient == NULL) {
        ALOGV("Client is not crated !\n");
        return NULL;
    }

    if (!mParsedMetaData) {
        parseMetaData();
        mParsedMetaData = true;
    }

    ssize_t index = mMetaData.indexOfKey(keyCode);

    if (index < 0) {
        return NULL;
    }

    return strdup(mMetaData.valueAt(index).string());
}

MediaAlbumArt *AmlPlayerMetadataRetriever0::extractAlbumArt()
{
    if (mClient == NULL) {
        ALOGV("Client is not crated !\n");
        return NULL;
    }

    if (!mParsedMetaData) {
        parseMetaData();

        mParsedMetaData = true;
    }

    if (mAlbumArt) {
        return new MediaAlbumArt(*mAlbumArt);
    }

    return NULL;
}

void AmlPlayerMetadataRetriever0::parseMetaData()
{
    if (thumbnail_find_stream_info(mClient, mFileName)) {
        ALOGV("Thumbnail find stream info failed!\n");
        return ;
    }

    struct Map {
        char* from;
        int      to;
    };
    static const struct Map kMap[] = {
        { "title", METADATA_KEY_TITLE },
        { "artist", METADATA_KEY_ARTIST },
        { "album", METADATA_KEY_ALBUM },
        { "genre", METADATA_KEY_GENRE },
        { "album_artist", METADATA_KEY_ALBUMARTIST },
        { "track", METADATA_KEY_CD_TRACK_NUMBER },
        { "disc", METADATA_KEY_DISC_NUMBER },
        { "composer", METADATA_KEY_COMPOSER },
        { "date", METADATA_KEY_YEAR },
        { "GPSCoordinates", METADATA_KEY_LOCATION },
        { "rotate", METADATA_KEY_VIDEO_ROTATION }
    };
    static const size_t kNumMapEntries = sizeof(kMap) / sizeof(kMap[0]);

    for (size_t i = 0; i < kNumMapEntries; ++i) {
        const char *value;
        if (thumbnail_get_key_metadata(mClient, kMap[i].from, &value)) {
            ALOGV("get %s: %s \n", kMap[i].from, value);
            mMetaData.add(kMap[i].to, String8(value));
        }
    }
    int v, a, s;
    v = a = s = 0;
    thumbnail_get_tracks_info(mClient, &v, &a, &s);
    if (v > 0) {
        mMetaData.add(METADATA_KEY_HAS_VIDEO, String8("yes"));
    }
    if (a > 0) {
        mMetaData.add(METADATA_KEY_HAS_AUDIO, String8("yes"));
    }

    const void* data;
    int dataSize;
    if (thumbnail_get_key_data(mClient, "cover_pic", &data, &dataSize)) {
        mAlbumArt = new MediaAlbumArt;
        mAlbumArt->mSize = dataSize;
        mAlbumArt->mData = new uint8_t[dataSize];
        memcpy(mAlbumArt->mData, (char *)data, dataSize);
    }
    int width=0,height=0;
    thumbnail_get_video_size(mClient, &width, &height);
    if(width >0 && height>0){
        char tmp[32];
        sprintf(tmp, "%d", width);
        mMetaData.add(METADATA_KEY_VIDEO_WIDTH,String8(tmp));
        sprintf(tmp, "%d", height);
        mMetaData.add(METADATA_KEY_VIDEO_HEIGHT,String8(tmp));
    }
    char tmp[32];
    int64_t durationUs;
    thumbnail_get_duration(mClient, &durationUs);
    sprintf(tmp, "%lld", (durationUs + 500) / 1000);
    mMetaData.add(METADATA_KEY_DURATION, String8(tmp));

    thumbnail_find_stream_info_end(mClient);
}
}
