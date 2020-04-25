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

#ifndef AM_THUMBNAIL_H
#define  AM_THUMBNAIL_H

#include <utils/threads.h>
#include <utils/Errors.h>
#include <utils/String8.h>

extern "C" {
#include <libavutil/avstring.h>
#include <libavcodec/avcodec.h>
#include <libavformat/avio.h>
#include <libavformat/avformat.h>
#include <libavformat/url.h>
#include <libswscale/swscale.h>
#include <libavutil/opt.h>
#undef CodecType

#  ifdef lseek
#   undef lseek
#  endif
#define lseek(f,p,w) lseek64((f), (p), (w))
}

#define DEST_FMT PIX_FMT_RGB565

typedef struct {
    int num;    //numerator
    int den;    //denominator
} rational;

typedef struct stream {
    int videoStream;
    AVFormatContext *pFormatCtx;
    AVCodecContext  *pCodecCtx;
    AVCodec         *pCodec;
    AVFrame         *pFrameYUV;
    AVFrame         *pFrameRGB;
} stream_t;

typedef struct AmlogicPlayer_File {
    int               fd;
    int           fd_valid;
    int64_t          mOffset;
    int64_t          mLength;
} AmlogicPlayer_File;

namespace android
{

class AmThumbnailInt
{
public:
    AmThumbnailInt();
    ~AmThumbnailInt();
    void amthumbnail_clear();
    int amthumbnail_decoder_open(const char* filename);
    int amthumbnail_extract_video_frame(int64_t time, int flag);
    int amthumbnail_read_frame(char* buffer);
    void amthumbnail_get_video_size(int* width, int* height);
    float amthumbnail_get_aspect_ratio();
    void amthumbnail_get_duration(int64_t *duration);
    int amthumbnail_get_key_metadata(char* key, const char** value);
    int amthumbnail_get_key_data(char* key, const void** data, int* data_size);
    void amthumbnail_get_video_rotation(int* rotation);
    int amthumbnail_decoder_close();
    int amthumbnail_get_tracks_info(int *vtracks, int *atracks, int *stracks);

private:
    stream_t mStream;
    int mVwidth;
    int mVheight;
    int64_t mDuration;
    int64_t mThumbnailTime;
    int64_t mThumbnailOffset;
    rational mDisplayAspectRatio;
    int mDataSize;
    uint8_t *mData;
    int mMaxframesize;
    char *mkey_metadata;
    
    void calc_aspect_ratio(rational *ratio, struct stream *stream);
    int av_read_next_video_frame(AVFormatContext *pFormatCtx, AVPacket *pkt, int vindex);
    void find_best_keyframe(AVFormatContext *pFormatCtx, int video_index, int count, int64_t *time, int64_t *offset, int *maxsize);
    float amPropGetFloat(const char* str, float def = 0.0);

    static status_t BasicInit();
    static int      vp_open(URLContext *h, const char *filename, int flags);
    static int      vp_read(URLContext *h, unsigned char *buf, int size);
    static int      vp_write(URLContext *h, const unsigned char *buf, int size);
    static int64_t  vp_seek(URLContext *h, int64_t pos, int whence);
    static int      vp_close(URLContext *h);
    static int      vp_get_file_handle(URLContext *h);

	bool is_slow_media;
};
}

#endif

