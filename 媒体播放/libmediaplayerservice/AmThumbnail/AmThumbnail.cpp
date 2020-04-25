/*
 * Copyright (C) 2012 The Android Open Source Project
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
#define LOG_TAG "AmThumbnail"
#include <utils/Log.h>

#include <AmThumbnail.h>
#include <cutils/properties.h>

namespace android
{

#define TRY_DECODE_MAX (50)
#define READ_FRAME_MAX (10*25)
#define READ_FRAME_MIN (2*25)
#define MAX(a, b) (((a) > (b)) ? (a) : (b))

static URLProtocol android_protocol;

status_t AmThumbnailInt::BasicInit()
{
    static int have_inited = 0;
    if (!have_inited) {

        URLProtocol *prot = &android_protocol;
        prot->name = "amthumb";
        prot->url_open = (int (*)(URLContext *, const char *, int))vp_open;
        prot->url_read = (int (*)(URLContext *, unsigned char *, int))vp_read;
        prot->url_write = (int (*)(URLContext *, const unsigned char *, int))vp_write;
        prot->url_seek = (int64_t (*)(URLContext *, int64_t , int))vp_seek;
        prot->url_close = (int (*)(URLContext *))vp_close;
        prot->url_get_file_handle = (int (*)(URLContext *))vp_get_file_handle;
        ffurl_register_protocol(prot, sizeof(*prot));
        av_register_all();
        have_inited++;
        avformat_network_init();
    }
    return 0;
}

int AmThumbnailInt::vp_open(URLContext *h, const char *filename, int flags)
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
            AmlogicPlayer_File* af = (AmlogicPlayer_File*)fd;
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

int AmThumbnailInt::vp_read(URLContext *h, unsigned char *buf, int size)
{
    AmlogicPlayer_File* af = (AmlogicPlayer_File*)h->priv_data;
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

int AmThumbnailInt::vp_write(URLContext *h, const unsigned char *buf, int size)
{
    AmlogicPlayer_File* af = (AmlogicPlayer_File*)h->priv_data;

    return -1;
}

int64_t AmThumbnailInt::vp_seek(URLContext *h, int64_t pos, int whence)
{
    AmlogicPlayer_File* af = (AmlogicPlayer_File*)h->priv_data;
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

int AmThumbnailInt::vp_close(URLContext *h)
{
    FILE* fp = (FILE*)h->priv_data;
    ALOGV("%s\n", __FUNCTION__);
    return 0; /*don't close file here*/
    //return fclose(fp);
}

int AmThumbnailInt::vp_get_file_handle(URLContext *h)
{
    ALOGV("%s\n", __FUNCTION__);
    return (intptr_t) h->priv_data;
}

AmThumbnailInt::AmThumbnailInt()
{
    ALOGV("AmThumbnailInt\n");

    mVwidth = 0 ;
    mVheight = 0;
    mDuration = 0;
    mThumbnailTime = 0;
    mThumbnailOffset = 0;
    mDataSize = 0;
    mData = NULL;
    mMaxframesize = 0;
    mDisplayAspectRatio.den = 0;
    mDisplayAspectRatio.num = 0;
    memset(&mStream, 0, sizeof(struct stream));
    mStream.videoStream = -1;
    mkey_metadata = NULL;
    
    BasicInit();
}

AmThumbnailInt::~AmThumbnailInt()
{
    ALOGV("~AmThumbnailInt\n");
    if (mData) {
        free(mData);
        mData = NULL;
    }

    if (mStream.pFrameRGB) {
        av_free(mStream.pFrameRGB);
        mStream.pFrameRGB = NULL;
    }
    if (mStream.pFrameYUV) {
        av_free(mStream.pFrameYUV);
        mStream.pFrameYUV = NULL;
    }
    if (mStream.pCodecCtx) {
        avcodec_close(mStream.pCodecCtx);
    }
    if (mStream.pFormatCtx) {
        avformat_close_input(&mStream.pFormatCtx);
        //av_close_input_file(mStream.pFormatCtx);
    }
    if (mkey_metadata) {
        free(mkey_metadata);
        mkey_metadata = NULL;
    }    
}

float AmThumbnailInt::amPropGetFloat(const char * str, float def)
{
    char value[92];
    float ret = def;
    if (property_get(str, value, NULL) > 0) {
        if ((sscanf(value, "%f", &ret)) > 0) {
            ALOGV("%s is set to %f\n", str, ret);
            return ret;
        }
    }
    ALOGV("%s is not set used def=%f\n", str, ret);
    return ret;
}

void AmThumbnailInt::calc_aspect_ratio(rational *ratio, struct stream *stream)
{
    int num, den;

    av_reduce(&num, &den,
              stream->pCodecCtx->width * stream->pCodecCtx->sample_aspect_ratio.num,
              stream->pCodecCtx->height * stream->pCodecCtx->sample_aspect_ratio.den,
              1024 * 1024);
    ratio->num = num;
    ratio->den = den;
}

int AmThumbnailInt::av_read_next_video_frame(AVFormatContext *pFormatCtx, AVPacket *pkt, int vindex)
{
    int r = -1;
    int retry = 500;
    do {
        r = av_read_frame(pFormatCtx, pkt);
        if (r == 0 && pkt->stream_index == vindex) {
            break;
        }
        av_free_packet(pkt);
        if (r < 0) {
            break;
        } else {
            r = -2;    /*a audio or other frame.*/
        }
    } while (retry-- > 0);
    return r;
}

void AmThumbnailInt::find_best_keyframe(AVFormatContext *pFormatCtx, int video_index, int count, int64_t *time, int64_t *offset, int *maxsize)
{
    int i = 0;
    int maxFrameSize = 0;
    int64_t thumbTime = 0;
    int64_t thumbOffset = 0;
    AVPacket packet;
    int r = 0;
    int find_ok = 0;
    int keyframe_index = 0;
    AVStream *st = pFormatCtx->streams[video_index];
    int havepts = 0;
    int nopts = 0;
    *maxsize = 0;
    if (count <= 0) {
        float newcnt = amPropGetFloat("libplayer.thumbnail.scan.count");
        count = 100;
        if (newcnt >= 1) {
            count = (int)newcnt;
        }
    }

    do {
        r = av_read_next_video_frame(pFormatCtx, &packet, video_index);
        if (r < 0) {
            break;
        }
        //ALOGV("[find_best_keyframe][%d]read frame packet.size=%d,pts=%lld\n", i, packet.size, packet.pts);
        havepts = (packet.pts > 0 || havepts);
        nopts = (i > 10) && !havepts;
        if (packet.size > maxFrameSize && (packet.pts >= 0 || nopts)) { //packet.pts>=0 can used for seek.
            maxFrameSize = packet.size;
            thumbTime = packet.pts;
            thumbOffset = avio_tell(pFormatCtx->pb) - packet.size;
            keyframe_index = i;
            find_ok = 1;
        }

        av_free_packet(&packet);
        if (i > 5 && find_ok && maxFrameSize > 100 * 1024 / (1 + i / 20)) {
            break;
        }
    } while (i++ < count);

    if (find_ok) {
        ALOGV("[%s]return thumbTime=%lld thumbOffset=%llx\n", __FUNCTION__, thumbTime, thumbOffset);
        if (i <= 5) {
            ALOGV("[%s:%d]not so much frames %d, set single thread decode\n", __FUNCTION__, __LINE__, i);
            av_opt_set_int(mStream.pCodecCtx, "threads", 1, 0);
        }
        if (thumbTime >= 0 && thumbTime != AV_NOPTS_VALUE) {
            *time = av_rescale_q(thumbTime, st->time_base, AV_TIME_BASE_Q);
        } else {
            *time = AV_NOPTS_VALUE;
        }
        *offset = thumbOffset;
        *maxsize = maxFrameSize;
        r = 0;
    } else {
        //ALOGV("[%s]find_best_keyframe failed\n", __FUNCTION__);
    }

    return;
}

int AmThumbnailInt::amthumbnail_decoder_open(const char* filename)
{
    unsigned int i;
    int video_index, audio_index;
    if((strncmp(filename, "http", strlen("http")) == 0 ||
        strncmp(filename, "https", strlen("https")) == 0)){
        is_slow_media = true;
    }else{
        is_slow_media = false;
    }

    if (avformat_open_input(&mStream.pFormatCtx, filename, NULL, NULL) != 0) {
        ALOGV("Coundn't open file %s !\n", filename);
        goto err;
    }

    if (avformat_find_stream_info(mStream.pFormatCtx, NULL) < 0) {
        ALOGV("Coundn't find stream information !\n");
        goto err1;
    }
#ifdef DUMP_INDEX
{
    int i, j;
    AVStream *pStream;
	av_dump_format(mStream.pFormatCtx, 0,filename, 0);
    ALOGV("*********************************************\n");
    for (i = 0; i < mStream.pFormatCtx->nb_streams; i ++) {
        pStream = mStream.pFormatCtx->streams[i];
        if (pStream) {
            for (j = 0; j < pStream->nb_index_entries; j++) {
                ALOGV("stream[%d]:idx[%d] pos:%llx time:%llx\n", i, j, pStream->index_entries[j].pos, pStream->index_entries[j].timestamp);
            }
        }
    }
    ALOGV("*********************************************\n");
}	
#endif

    for (i = 0; i < mStream.pFormatCtx->nb_streams; i++) {
        if (mStream.pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
            mStream.videoStream = i;
            break;
        }
    }

    video_index = mStream.videoStream;
    if (video_index == -1) {
        ALOGV("Didn't find a video stream!\n");
        audio_index = -1;
        for (i = 0; i < mStream.pFormatCtx->nb_streams; i++) {
            if (mStream.pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO) {
                audio_index = i;
                break;
            }
        }
        
        if (audio_index == -1) {
            ALOGV("Didn't find a audio stream, too!\n");
            goto err1;
        } else {
            return 0;
        }
    } else {
        mStream.pCodecCtx = mStream.pFormatCtx->streams[video_index]->codec;
        if (mStream.pCodecCtx == NULL) {
            ALOGV("pCodecCtx is NULL !\n");
            goto err1;
        } else {
            mVwidth = mStream.pCodecCtx->width;
            mVheight = mStream.pCodecCtx->height;
        }

        if(mVwidth > 1920 && mVheight > 1088) {
            ALOGV("Couldn't support 4K!\n");
            goto err1;
        }

        mStream.pCodec = avcodec_find_decoder(mStream.pCodecCtx->codec_id);
        if (mStream.pCodec == NULL) {
            ALOGV("Didn't find codec!\n");
            goto err1;
        }

        /* detect frames */
        if(!is_slow_media)
            find_best_keyframe(mStream.pFormatCtx, video_index, 0, &mThumbnailTime, &mThumbnailOffset, &mMaxframesize);

        if (avcodec_open2(mStream.pCodecCtx, mStream.pCodec, NULL) < 0) {
            ALOGV("Couldn't open codec!\n");
            goto err1;
        }

        mDuration = mStream.pFormatCtx->duration;

        mStream.pFrameYUV = avcodec_alloc_frame();
        if (mStream.pFrameYUV == NULL) {
            ALOGV("alloc YUV frame failed!\n");
            goto err2;
        }

        mStream.pFrameRGB = avcodec_alloc_frame();
        if (mStream.pFrameRGB == NULL) {
            ALOGV("alloc RGB frame failed!\n");
            goto err3;
        }

        mDataSize = avpicture_get_size(DEST_FMT, mVwidth, mVheight);
        mData = (uint8_t *)malloc(mDataSize);
        if (mData == NULL) {
            ALOGV("alloc buffer failed!\n");
            goto err4;
        }

        avpicture_fill((AVPicture *)mStream.pFrameRGB, mData, DEST_FMT, mVwidth, mVheight);

        return 0;
    }

err4:
    av_free(mStream.pFrameRGB);
    mStream.pFrameRGB = NULL;
err3:
    av_free(mStream.pFrameYUV);
    mStream.pFrameYUV = NULL;
err2:
    avcodec_close(mStream.pCodecCtx);
err1:
    avformat_close_input(&mStream.pFormatCtx);
    //av_close_input_file(mStream.pFormatCtx);
err:
    memset(&mStream, 0, sizeof(struct stream));
    return -1;
}

int AmThumbnailInt::amthumbnail_extract_video_frame(int64_t time, int flag)
{
    int frameFinished = 0;
    int tryNum = 0;
    int i = 0;
    int64_t ret ;
    int64_t timestamp = 1;

    struct stream *stream = &mStream;
    AVFormatContext *pFormatCtx = stream->pFormatCtx;

    ALOGV("[%s:%d]video streamindex %d", __FUNCTION__, __LINE__, stream->videoStream);
    if ((stream->videoStream < 0) || (stream->videoStream >= (int)pFormatCtx->nb_streams)) {
        ALOGV("[%s:%d]Illigle video streamindex %d", __FUNCTION__, __LINE__, stream->videoStream);
        return -1;
    }
    
    AVPacket        packet;
    AVStream *pStream = pFormatCtx->streams[stream->videoStream];
    AVCodecContext *pCodecCtx = pFormatCtx->streams[stream->videoStream]->codec;

    float starttime = amPropGetFloat("libplayer.thumbnail.starttime");
    int duration = pFormatCtx->duration / AV_TIME_BASE;

    if (time >= 0) {
        timestamp = time;
        if (pFormatCtx->start_time != (int64_t)AV_NOPTS_VALUE) {
            timestamp += pFormatCtx->start_time;
        }
    } else {
        if(is_slow_media){
            timestamp = 0;
        }else if (starttime >= 0) {
            timestamp = (int64_t)starttime;
            if (timestamp >= duration) {
                timestamp = duration - 1;
            }
            if (timestamp <= 0) {
                timestamp = 0;
            }
        } else {
            if (duration > 360) {
                timestamp = 120;/*long file.don't do more seek..*/
            } else if (duration > 6) {
                timestamp = duration / 3;
            } else {
                timestamp = 0;/*file is too short,try from start.*/
            }
        }

        timestamp = timestamp * AV_TIME_BASE + pFormatCtx->start_time;
    }

    ALOGV("[%s:%d]time %lld, timestamp %lld, starttime %f, duration %d %lld\n", 
        __FUNCTION__, __LINE__, time, timestamp, starttime, duration, pFormatCtx->duration);

    if (!strcmp(pFormatCtx->iformat->name, "mpegts") || !strcmp(pFormatCtx->iformat->name, "mpeg"))
    {
        int64_t file_offset = 0;
        if (timestamp == pFormatCtx->start_time) {
            ALOGV("[%s:%d]%s file seek to file head\n", __FUNCTION__, __LINE__, pFormatCtx->iformat->name);
            avio_seek(pFormatCtx->pb, file_offset, SEEK_SET); // just seek to head if from 0 for ts/ps
        } else {
            int64_t file_size = avio_size(pFormatCtx->pb);
            if ((pFormatCtx->duration > 0) && (file_size > 0)) {
                file_offset = 1.0 * timestamp * file_size / pFormatCtx->duration;
                ALOGV("[%s:%d]%s file seek to 0x%llx\n", __FUNCTION__, __LINE__, pFormatCtx->iformat->name, file_offset);
                avio_seek(pFormatCtx->pb, file_offset, SEEK_SET);
            } else {
                timestamp = av_rescale(timestamp, pStream->time_base.den, AV_TIME_BASE * (int64_t)pStream->time_base.num);
                ALOGV("[thumbnail_extract_video_frame:%d]time=%lld time=%lld  offset=%lld timestamp=%lld!\n",
                      __LINE__, time, mThumbnailTime, mThumbnailOffset, timestamp);

                if (av_seek_frame(pFormatCtx, stream->videoStream, timestamp, AVSEEK_FLAG_BACKWARD) < 0) {
                    ALOGV("[%s:%d]av_seek_frame failed!", __FUNCTION__, __LINE__);
                }
            }
        }
    } else {
        timestamp = av_rescale(timestamp, pStream->time_base.den, AV_TIME_BASE * (int64_t)pStream->time_base.num);
        ALOGV("[thumbnail_extract_video_frame:%d]time=%lld time=%lld  offset=%lld timestamp=%lld!\n",
              __LINE__, time, mThumbnailTime, mThumbnailOffset, timestamp);

        if (av_seek_frame(pFormatCtx, stream->videoStream, timestamp, AVSEEK_FLAG_BACKWARD) < 0) {
            ALOGV("[%s:%d]av_seek_frame failed!", __FUNCTION__, __LINE__);
        }
    }

    avcodec_flush_buffers(stream->pCodecCtx);

    i = 0;
    while (av_read_next_video_frame(pFormatCtx, &packet, stream->videoStream) >= 0) {
        AVFrame *pFrame = NULL;
        int temp_ret;
        ALOGV("[%s] av_read_frame frame size=%d,pts=%lld\n", __FUNCTION__, packet.size, packet.pts);
        i++;
        if (!is_slow_media && packet.size < MAX(mMaxframesize / 10, packet.size) && i < READ_FRAME_MIN) {
            continue;/*skip small size packets,it maybe a black frame*/
        }

        temp_ret = avcodec_decode_video2(stream->pCodecCtx, stream->pFrameYUV, &frameFinished, &packet);
        pFrame = stream->pFrameYUV;
        ALOGV("[%s]decode video frame, finish=%d key=%d offset=%llx type=%dcodec_id=%x,quality=%d tryNum=%d ret %d\n",
              __FUNCTION__, frameFinished, pFrame->key_frame, avio_tell(pFormatCtx->pb), pFrame->pict_type, pCodecCtx->codec_id, pFrame->quality, tryNum, temp_ret);
        if (frameFinished &&
            ((pFrame->key_frame && pFrame->pict_type == AV_PICTURE_TYPE_I) ||
             (pFrame->key_frame && pFrame->pict_type == AV_PICTURE_TYPE_SI) ||
             (pFrame->key_frame && pFrame->pict_type == AV_PICTURE_TYPE_BI) ||
             (tryNum > 4 && pFrame->key_frame) ||
             (tryNum > 5 && pFrame->pict_type == AV_PICTURE_TYPE_I) ||
             (tryNum > 6 && pFrame->pict_type == AV_PICTURE_TYPE_SI) ||
             (tryNum > 7 && pFrame->pict_type == AV_PICTURE_TYPE_BI) ||
             (tryNum > (TRY_DECODE_MAX - 1) && i > READ_FRAME_MAX && pFrame->pict_type == AV_PICTURE_TYPE_P) ||
             (tryNum > (TRY_DECODE_MAX - 1) && i > READ_FRAME_MAX && pFrame->pict_type == AV_PICTURE_TYPE_B) ||
             (tryNum > (TRY_DECODE_MAX - 1) && i > READ_FRAME_MAX && pFrame->pict_type == AV_PICTURE_TYPE_S))) { /*not find a I FRAME too long,try normal frame*/
            ALOGV("[%s]pCodecCtx->codec_id=%x tryNum=%d\n", __FUNCTION__, pCodecCtx->codec_id, tryNum);

            struct SwsContext *img_convert_ctx;
            img_convert_ctx = sws_getContext(stream->pCodecCtx->width, stream->pCodecCtx->height, stream->pCodecCtx->pix_fmt,
                                             mVwidth, mVheight, DEST_FMT, SWS_BICUBIC, NULL, NULL, NULL);
            if (img_convert_ctx == NULL) {
                ALOGV("can not initialize the coversion context!\n");
                av_free_packet(&packet);
                break;
            }

            sws_scale(img_convert_ctx, stream->pFrameYUV->data, stream->pFrameYUV->linesize, 0,
                      mVheight, stream->pFrameRGB->data, stream->pFrameRGB->linesize);
            sws_freeContext(img_convert_ctx);
            av_free_packet(&packet);
            goto ret;
        }
        av_free_packet(&packet);

        if (tryNum++ > TRY_DECODE_MAX && i > READ_FRAME_MAX) {
            break;
        }
        if (tryNum % 10 == 0) {
            usleep(100);
        }
    }

    if (mData) {
        free(mData);
        mData = NULL;
    }
    if (stream->pFrameRGB) {
        av_free(stream->pFrameRGB);
        stream->pFrameRGB = NULL;
    }
    if (stream->pFrameYUV) {
        av_free(stream->pFrameYUV);
        stream->pFrameYUV = NULL;
    }
    avcodec_close(stream->pCodecCtx);
    memset(&mStream, 0, sizeof(struct stream));
    return -1;

ret:
    return 0;
}

int AmThumbnailInt::amthumbnail_read_frame(char* buffer)
{
    int i;
    int index = 0;

    for (i = 0; i < mVheight; i++) {
        memcpy(buffer + index, mStream.pFrameRGB->data[0] + i * mStream.pFrameRGB->linesize[0], mVwidth * 2);
        index += mVwidth * 2;
    }

    return 0;
}

void AmThumbnailInt::amthumbnail_get_video_size(int* width, int* height)
{
    *width = mVwidth;
    *height = mVheight;
}

float AmThumbnailInt::amthumbnail_get_aspect_ratio()
{
    calc_aspect_ratio(&mDisplayAspectRatio, &mStream);

    if (!mDisplayAspectRatio.num || !mDisplayAspectRatio.den) {
        return (float)mVwidth / mVheight;
    } else {
        return (float)mDisplayAspectRatio.num / mDisplayAspectRatio.den;
    }
}

void AmThumbnailInt::amthumbnail_get_duration(int64_t *duration)
{
    *duration = mStream.pFormatCtx->duration;
    ALOGV("amthumbnail_get_duration duration:%lld \n", *duration);
}

int AmThumbnailInt::amthumbnail_get_key_metadata(char* key, const char** value)
{
    AVDictionaryEntry *tag = NULL;

    if (!mStream.pFormatCtx->metadata) {
        return 0;
    }

    if (!memcmp(key, "rotate", 6)) {
        int rot;
        if (mStream.videoStream == -1) { /*no video.*/
            return 0;
        }
        amthumbnail_get_video_rotation(&rot);
        if (mkey_metadata == NULL) {
            mkey_metadata= (char*)av_malloc(32);
        }
        memset(mkey_metadata, 0x00, 32);
        sprintf(mkey_metadata, "%d", rot);
        *value = mkey_metadata;
        return 1;
    }
    tag = av_dict_get(mStream.pFormatCtx->metadata, key, tag, 0);
    if (tag) {
        *value = tag->value;
        return 1;
    }

    return 0;
}

int AmThumbnailInt::amthumbnail_get_key_data(char* key, const void** data, int* data_size)
{
    AVDictionaryEntry *tag = NULL;

    if (!mStream.pFormatCtx->metadata) {
        return 0;
    }

    if (av_dict_get(mStream.pFormatCtx->metadata, key, tag, AV_DICT_IGNORE_SUFFIX)) {
        *data = mStream.pFormatCtx->cover_data;
        *data_size = mStream.pFormatCtx->cover_data_len;
        return 1;
    }

    return 0;
}

int AmThumbnailInt::amthumbnail_get_tracks_info(int *vtracks, int *atracks, int *stracks)
{
    AVStream **avs = mStream.pFormatCtx->streams;
    unsigned int i;
    *vtracks = 0;
    *atracks = 0;
    *stracks = 0;

    for (i = 0; i < mStream.pFormatCtx->nb_streams; i++) {
        if (avs[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
            (*vtracks)++;
        } else if (avs[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO) {
            (*atracks)++;
        } else if (avs[i]->codec->codec_type == AVMEDIA_TYPE_SUBTITLE) {
            (*stracks)++;
        }
    }
    ALOGV("thumbnail_get_tracks_info v:%d a:%d s:%d \n", *vtracks, *atracks, *stracks);
    return 0;
}

void AmThumbnailInt::amthumbnail_get_video_rotation(int* rotation)
{
    int stream_rotation = mStream.pFormatCtx->streams[mStream.videoStream]->rotation_degree;

    switch (stream_rotation) {
    case 1:
        *rotation = 90;
        break;

    case 2:
        *rotation = 180;
        break;

    case 3:
        *rotation = 270;
        break;

    default:
        *rotation = 0;
        break;
    }

    return;
}

int AmThumbnailInt::amthumbnail_decoder_close()
{
    if (mData) {
        free(mData);
        mData = NULL;
    }
    if (mStream.pFrameRGB) {
        av_free(mStream.pFrameRGB);
        mStream.pFrameRGB = NULL;
    }
    if (mStream.pFrameYUV) {
        av_free(mStream.pFrameYUV);
        mStream.pFrameYUV = NULL;
    }
    if (mkey_metadata) {
        free(mkey_metadata);
        mkey_metadata = NULL;
    }

    avcodec_close(mStream.pCodecCtx);

    return 0;
}

}
