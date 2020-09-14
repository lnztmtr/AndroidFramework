/*********************************************
* init date: 2015.11.05
* description: hls demuxer for media group
* author: senbai.tao<senbai.tao@amlogic.com>
*********************************************/

#include "libavutil/opt.h"
#include "libavformat/internal.h"
#include "hls_m3ulivesession.h"

AVRational HLS_Rational = {1, AV_TIME_BASE};

#define AVCMD_GET_HLSINFO                        (1200+3)

#define HLS_DEMUXER_BUFFER_SIZE_DEFAULT (32 * 1024)
#define HLS_DEMUXER_LPBUFFER_SIZE_DEFAULT (10* 1024 * 1024)
#define HLS_DEMUXER_MEDIA_READ_RETRY_S (30) // second
#define HLS_DEMUXER_TAG "mhls"
#define HLOG(...) av_tag_log(HLS_DEMUXER_TAG, __VA_ARGS__)

static int hls_interrupt_call_cb(void) {
    if (url_interrupt_cb()) {
        HLOG("[%s] : url_interrupt_cb !", __FUNCTION__);
        return 1;
    }
    return 0;
}

typedef struct _HLS_STREAM_CONTEXT {
    int64_t            next_pts;
    int64_t            inner_segment_start_pts;
    int64_t            segment_start_pts;
    int64_t            duration;
    int                stream_id;
    MediaType          type;
    int                need_drop;
} HLS_STREAM_CONTEXT;

typedef struct _FFMPEG_HLS_STREAM_CONTEXT {
    AVFormatContext * ctx;
    AVIOContext * pb;
    HLS_STREAM_CONTEXT ** stream_info_array;
    MediaType stream_type;
    int stream_nb;
    unsigned char * buffer;
    int buffer_size;
    int eof;
    int forbid;
    int parsed;
} FFMPEG_HLS_STREAM_CONTEXT;

typedef struct _FFMPEG_HLS_SESSION_CONTEXT {
    const AVClass *class;
    URLContext *h;
    int notify_id;
    int64_t audio_anchor_timeUs;
    int64_t last_audio_read_timeUs;
    int64_t last_sub_read_timeUs;
    int64_t sub_next_read_timeUs;
    int64_t sub_read_reference_timeUs;
    M3ULiveSession * session_ctx;
    FFMPEG_HLS_STREAM_CONTEXT ** stream_array;
    int nb_session;
    int prev_read_session_index;
    int codec_vbuf_size;
    int codec_abuf_size;
    int codec_vdata_len;
    int codec_adata_len;
    int sub_index;
    int sub_read_flag;
    int audio_stream_index;
    int video_stream_index;
    int ff_fb;
    int fffb_changed;
    int seek_changed;
    FILE * vhandle;
    FILE * ahandle;
    pthread_mutex_t sub_lock; // lock between read and switch.
} FFMPEG_HLS_SESSION_CONTEXT;

static int64_t _get_clock_monotonic_us(void)
{
    struct timespec new_time;
    int64_t cpu_clock_us = 0;

    clock_gettime(CLOCK_MONOTONIC, &new_time);
    cpu_clock_us = ((int64_t)new_time.tv_nsec / 1000 + (int64_t)new_time.tv_sec * 1000000);
    return cpu_clock_us;
}

static int _hls_iocontext_read(void * opaque, uint8_t * buf, int buf_size) {
    URLContext * uc = (URLContext *)opaque;
    M3ULiveSession * session = (M3ULiveSession *)uc->priv_data;

    int ret = m3u_session_media_read_data((void *)session, uc->stream_index, buf, buf_size);
    return ret;
}

static void _release_hls_stream_context(AVFormatContext * s) {
    FFMPEG_HLS_SESSION_CONTEXT * hls_session_ctx = (FFMPEG_HLS_SESSION_CONTEXT *)(s->priv_data);

    int i = 0;
    if (hls_session_ctx->nb_session > 0) {
        for (; i < hls_session_ctx->nb_session; i++) {
            if (hls_session_ctx->stream_array[i]->stream_nb > 0) {
                int j = 0;
                for (; j < hls_session_ctx->stream_array[i]->stream_nb; j++) {
                    if (hls_session_ctx->stream_array[i]->stream_info_array[j]) {
                        free(hls_session_ctx->stream_array[i]->stream_info_array[j]);
                    }
                }
                free(hls_session_ctx->stream_array[i]->stream_info_array);
            }
            free(hls_session_ctx->stream_array[i]);
        }
        free(hls_session_ctx->stream_array);
    }
    pthread_mutex_destroy(&hls_session_ctx->sub_lock);
}

static int _select_hls_session(AVFormatContext * s) {
    FFMPEG_HLS_SESSION_CONTEXT * hls_session_ctx = (FFMPEG_HLS_SESSION_CONTEXT *)(s->priv_data);

    if (hls_session_ctx->nb_session <= 0) {
        HLOG("[%s:%d] session invalid !", __FUNCTION__, __LINE__);
        return -1;
    }
#if 1
    int i, non_eof = 0;
    float a_level = 0.0;
    float v_level = 0.0;
    float min_thres = 0.0;
    // TODO: this logic maybe need to modify.
    if (hls_session_ctx->prev_read_session_index < 0) { // init
        for (i = 0; i < hls_session_ctx->nb_session; i++) {
            if (hls_session_ctx->stream_array[i]->stream_type < TYPE_SUBS
                && hls_session_ctx->stream_array[i]->forbid == 0) {
                hls_session_ctx->prev_read_session_index = i;
                return i;
            }
        }
    } else {
        for (i = 0; i < hls_session_ctx->nb_session; i++) {
            if (hls_session_ctx->stream_array[i]->stream_type >= TYPE_SUBS
                || hls_session_ctx->stream_array[i]->eof == 1
                || hls_session_ctx->stream_array[i]->forbid) {
                continue;
            }
            if (hls_session_ctx->stream_array[i]->eof < 1) {
                non_eof = 1;
            }
            if(hls_session_ctx->ff_fb >= 1){
               // HLOG("In ff fb,return video index\n");
                if (hls_session_ctx->stream_array[i]->stream_type == TYPE_VIDEO){
                    hls_session_ctx->prev_read_session_index = i;
                    return i;
                }else
                continue;
            }
			if(hls_session_ctx->codec_abuf_size > 0){
                a_level = (float)hls_session_ctx->codec_adata_len / hls_session_ctx->codec_abuf_size;
                a_level = a_level > 1 ? 1 : a_level;
            }
            if(hls_session_ctx->codec_vbuf_size > 0){
                v_level = (float)hls_session_ctx->codec_vdata_len / hls_session_ctx->codec_vbuf_size;
                v_level = v_level > 1 ? 1 : v_level;
            }
            min_thres = in_get_sys_prop_float("libplayer.hls.minthres");
            if(min_thres <= 0)
                min_thres = 0.1;

            if(((0 < v_level) &&(v_level < min_thres)) && (a_level > min_thres)){
                if (hls_session_ctx->stream_array[i]->stream_type == TYPE_VIDEO){
                    hls_session_ctx->prev_read_session_index = i;
                    HLOG("video underflow,v_level:%f,a_level:%f,min_thres:%f", v_level, a_level, min_thres);
                    return i;
                }else
                continue;
            }

            if(((0 < a_level) &&(a_level < min_thres)) && (v_level > min_thres)){
                if (hls_session_ctx->stream_array[i]->stream_type == TYPE_AUDIO){
                    hls_session_ctx->prev_read_session_index = i;
                //    HLOG("audio underflow,v_level:%f,a_level:%f,min_thres:%f", v_level, a_level, min_thres);
                    return i;
                }else
                continue;
            }
            if (i != hls_session_ctx->prev_read_session_index) {
                hls_session_ctx->prev_read_session_index = i;
                return i;
            }
        }
    }
    if (non_eof) {
        return hls_session_ctx->prev_read_session_index;
    } else {
        return -1;
    }
#else

    int i = 0, index = -1;
    int64_t pts0 = -1, min_pts = -1;
    for (; i < hls_session_ctx->nb_session; i++) {
        if (hls_session_ctx->stream_array[i]->stream_type > TYPE_VIDEO
            || hls_session_ctx->stream_array[i]->eof == 1
            || hls_session_ctx->stream_array[i]->forbid) {
            continue;
        }
        pts0 = hls_session_ctx->stream_array[i]->stream_info_array[0]->next_pts; // assume it solo stream, maybe need to modify.
        if (index == -1 || min_pts > pts0) {
            min_pts = pts0;
            index = i;
        }
    }
    return index;
#endif
}

static int _hls_parse_next_segment(AVFormatContext * s, int session_index, int first) {
    FFMPEG_HLS_SESSION_CONTEXT * hls_session_ctx = (FFMPEG_HLS_SESSION_CONTEXT *)(s->priv_data);
    FFMPEG_HLS_STREAM_CONTEXT * hls_stream_ctx = hls_session_ctx->stream_array[session_index];
    if (!hls_stream_ctx) {
        return AVERROR(EINVAL);
    }

    HLOG("[%s:%d] parse next segment, index : %d, first : %d", __FUNCTION__, __LINE__, session_index, first);
    int ret = 0, nb = 0;
    AVFormatContext * format = hls_stream_ctx->ctx;
    if (first > 0) {
        int lpbuffersize;
        if ((lpbuffersize = in_get_sys_prop_float("libplayer.ffmpeg.lpbufsizemax")) < 0) {
            lpbuffersize = HLS_DEMUXER_LPBUFFER_SIZE_DEFAULT;
        }
        hls_stream_ctx->buffer_size = HLS_DEMUXER_BUFFER_SIZE_DEFAULT;
        hls_stream_ctx->buffer = (unsigned char *)av_malloc(hls_stream_ctx->buffer_size);
        if (!hls_stream_ctx->buffer) {
            return AVERROR(ENOMEM);
        }
        URLContext * uc = (URLContext *)av_mallocz(sizeof(URLContext));
        if (!uc) {
            return AVERROR(ENOMEM);
        }
        uc->priv_data = (void *)(hls_session_ctx->session_ctx);
        uc->stream_index = session_index;
        URLProtocol * prot = (URLProtocol *)av_mallocz(sizeof(URLProtocol));
        uc->prot = prot;
        ret = url_lpopen_ex(uc, lpbuffersize, 0, _hls_iocontext_read, NULL);
        if (ret) {
            HLOG("[%s:%d] loop buffer open failed ! index : %d, ret(%d)", __FUNCTION__, __LINE__, session_index, ret);
            return AVERROR(ENOMEM);
        }
        AVIOContext * pb = (void *)avio_alloc_context(hls_stream_ctx->buffer, hls_stream_ctx->buffer_size, 0, uc, (void *)url_lpread, NULL, (void *)url_lpseek);
        if (!pb) {
            return AVERROR(ENOMEM);
        }
        if ((pb->mhls_read_retry_s = in_get_sys_prop_float("libplayer.hls.media_read_s")) < 0) {
            pb->mhls_read_retry_s = HLS_DEMUXER_MEDIA_READ_RETRY_S;
        }
        pb->support_time_seek = 0;
        pb->is_slowmedia = 1;
        pb->is_streamed = 1;
        pb->seekable = 0;
        pb->mhls_inner_format = 1;
        hls_stream_ctx->pb = pb;
        HLOG("[%s:%d] allocated iocontext,lpbufsize:%d, index : %d", __FUNCTION__, __LINE__, lpbuffersize, session_index);
    } else {
        if (memcmp(format->iformat->name, "mpegts", 6) != 0) {
            if (hls_stream_ctx->pb) {
                avio_reset(hls_stream_ctx->pb, AVIO_FLAG_READ);
                hls_stream_ctx->pb->support_time_seek = 0;
                hls_stream_ctx->pb->is_slowmedia = 1;
                hls_stream_ctx->pb->is_streamed = 1;
                hls_stream_ctx->pb->seekable = 0;
                hls_stream_ctx->pb->mhls_inner_format = 1;
                if ((hls_stream_ctx->pb->mhls_read_retry_s = in_get_sys_prop_float("libplayer.hls.media_read_s")) < 0) {
                    hls_stream_ctx->pb->mhls_read_retry_s = HLS_DEMUXER_MEDIA_READ_RETRY_S;
                }
            }
        }
    }

    if (first > 0 || (memcmp(format->iformat->name, "mpegts", 6) != 0)) {

        if (format) {
            av_close_input_stream(format);
        }
        format = NULL;
        if (!(format = avformat_alloc_context())) {
            HLOG("[%s:%d] index : %d, avformat_alloc_context failed !", __FUNCTION__, __LINE__, session_index);
            return AVERROR(ENOMEM);
        }
        format->pb = hls_stream_ctx->pb;
        format->is_hls_demuxer = 1;
        ret = avformat_open_input(&format, "", NULL, NULL);
        if (ret < 0) {
            HLOG("[%s:%d] index : %d, avformat_open_input failed !", __FUNCTION__, __LINE__, session_index);
            if (format) {
                av_close_input_stream(format);
            }
            return ret;
        }
        ret = av_find_stream_info(format);
        if (ret < 0) {
            HLOG("[%s:%d] index : %d, av_find_stream_info failed !", __FUNCTION__, __LINE__, session_index);
            if (format) {
                av_close_input_stream(format);
            }
            return ret;
        }
#if 0
        if (memcmp(format->iformat->name, "mpegts", 6) == 0) {
            int64_t ret64 = avio_seek(format->pb, 0, SEEK_SET);
            if (ret64 < 0) {
                HLOG("[%s:%d] index : %d, reset mpegts's pb to beginning, ret(%lld) !", __FUNCTION__, __LINE__, session_index, ret64);
            }
        }
#endif
    }

    if (first > 0) {
        for (nb = 0; nb < format->nb_streams; nb++) {
            AVStream * st = av_new_stream(s, hls_stream_ctx->stream_nb);
            if (!st) {
                HLOG("[%s:%d] av_new_stream failed !", __FUNCTION__, __LINE__);
                if (format) {
                    av_close_input_stream(format);
                }
                return AVERROR(ENOMEM);
            }
            st->time_base.num = HLS_Rational.num;
            st->time_base.den = HLS_Rational.den;
            st->r_frame_rate.num = format->streams[nb]->r_frame_rate.num;
            st->r_frame_rate.den = format->streams[nb]->r_frame_rate.den;
            avcodec_copy_context(st->codec, format->streams[nb]->codec);
            HLS_STREAM_CONTEXT * stream_ctx = (HLS_STREAM_CONTEXT *)av_mallocz(sizeof(HLS_STREAM_CONTEXT));
            if (st->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
                stream_ctx->type = TYPE_VIDEO;
            } else if (st->codec->codec_type == AVMEDIA_TYPE_AUDIO) {
                stream_ctx->type = TYPE_AUDIO;
            } else {
                stream_ctx->type = TYPE_NONE;
            }
            stream_ctx->need_drop = 0;
            stream_ctx->stream_id = st->index;
            stream_ctx->inner_segment_start_pts = -1;
            stream_ctx->segment_start_pts = 0;
            stream_ctx->next_pts = 0;
            stream_ctx->duration = 0;
            in_dynarray_add(&hls_stream_ctx->stream_info_array, &hls_stream_ctx->stream_nb, stream_ctx);
        }
        if (hls_stream_ctx->stream_nb > 1) {
            int i = 0;
            for (; i < hls_stream_ctx->stream_nb; i++) {
                if (hls_stream_ctx->stream_info_array[i]->type != hls_stream_ctx->stream_type) {
                    int j = 0;
                    for (; j < hls_session_ctx->nb_session; j++) {
                        if (hls_stream_ctx->stream_info_array[i]->type == hls_session_ctx->stream_array[j]->stream_type
                            && !hls_session_ctx->stream_array[j]->forbid) {
                            hls_stream_ctx->stream_info_array[i]->need_drop = 1;
                            HLOG("[%s:%d] stream(%d) need to drop !", __FUNCTION__, __LINE__, hls_stream_ctx->stream_info_array[i]->type);
                        }
                    }
                }
            }
        }
        if (s->duration <= 0) {
            s->duration = hls_session_ctx->session_ctx->durationUs;
        }
    } else {
        for (nb = 0; nb < hls_stream_ctx->stream_nb; nb++) {
            hls_stream_ctx->stream_info_array[nb]->inner_segment_start_pts = -1;
            hls_stream_ctx->stream_info_array[nb]->segment_start_pts = hls_stream_ctx->stream_info_array[nb]->next_pts;
        }
    }
    HLOG("[%s:%d] parse new segment, session index : %d, nb_streams : %d",
        __FUNCTION__, __LINE__, session_index, hls_stream_ctx->stream_nb);
    hls_stream_ctx->ctx = format;
    hls_stream_ctx->parsed = 1;
    return 0;
}

static int hls_read_probe(AVProbeData * p) {
    HLOG("[%s:%d] read probe ! filename : %s ", __FUNCTION__, __LINE__, p->filename);
    if (av_strstart(p->filename, "mhls:", NULL) != 0) {
        HLOG("[%s:%d] hls demuxer has been probed !", __FUNCTION__, __LINE__);
        return AVPROBE_SCORE_MAX;
    }
    return 0;
}

static int hls_read_header(AVFormatContext * s, AVFormatParameters * ap) {
    FFMPEG_HLS_SESSION_CONTEXT * hls_session_ctx = (FFMPEG_HLS_SESSION_CONTEXT *)(s->priv_data);

    int ret = 0;
    void * session = NULL;
    char * inner_url = s->filename;
    if (av_strstart(inner_url, "mhls:", NULL) != 0
        || av_strstart(inner_url, "vhls:", NULL) != 0) {
        inner_url = s->filename + 5;
    }

    URLContext *h = malloc(sizeof(URLContext));
    h->notify_id = hls_session_ctx->notify_id;
    hls_session_ctx->h = h;
    ret = m3u_session_open(inner_url, s->headers, &session, h);
    if (ret < 0) {
        HLOG("[%s:%d] Failed to open session !", __FUNCTION__, __LINE__);
        return ret;
    }
    if(s->pb){
        s->pb->is_segment_media = 1;
    }
    m3u_session_register_interrupt(session, hls_interrupt_call_cb);
    M3ULiveSession * hls_session = (M3ULiveSession *)session;
    hls_session_ctx->session_ctx = hls_session;
    hls_session_ctx->prev_read_session_index = -1;
    hls_session_ctx->audio_anchor_timeUs = -1;
    hls_session_ctx->last_audio_read_timeUs = -1;
    hls_session_ctx->last_sub_read_timeUs = -1;
    hls_session_ctx->sub_next_read_timeUs = -1;
    hls_session_ctx->sub_read_reference_timeUs = -1;
    hls_session_ctx->sub_index = -1;
    hls_session_ctx->sub_read_flag = 0;
    hls_session_ctx->audio_stream_index = -1;
    hls_session_ctx->video_stream_index = -1;
    hls_session_ctx->vhandle = NULL;
    hls_session_ctx->ahandle = NULL;
    hls_session_ctx->ff_fb = 0;
    hls_session_ctx->fffb_changed = 0;
    hls_session_ctx->seek_changed = 0;
    pthread_mutex_init(&hls_session_ctx->sub_lock, NULL);

    int dumpmode = in_get_sys_prop_float("libplayer.hls.demuxer_dump"); // 1:audio, 2:video 3:both
    char dump_path[MAX_URL_SIZE] = {0};
    if (dumpmode == 1 || dumpmode == 3) {
        snprintf(dump_path, MAX_URL_SIZE, "/data/tmp/audio_demuxer_dump.dat");
        hls_session_ctx->ahandle = fopen(dump_path, "ab+");
    }
    if (dumpmode == 2 || dumpmode == 3) {
        snprintf(dump_path, MAX_URL_SIZE, "/data/tmp/video_demuxer_dump.dat");
        hls_session_ctx->vhandle = fopen(dump_path, "ab+");
    }

    int session_index = 0;
    for (session_index = 0; session_index < hls_session->media_item_num; session_index++) {
        FFMPEG_HLS_STREAM_CONTEXT * stream_ctx = (FFMPEG_HLS_STREAM_CONTEXT *)av_mallocz(sizeof(FFMPEG_HLS_STREAM_CONTEXT));
        stream_ctx->ctx = NULL;
        stream_ctx->pb = NULL;
        stream_ctx->stream_nb = 0;
        stream_ctx->eof = 0;
        stream_ctx->parsed = 0;
        stream_ctx->stream_type = hls_session->media_item_array[session_index]->media_type;
        stream_ctx->forbid = (hls_session->media_item_array[session_index]->media_url[0] == '\0');
        stream_ctx->stream_info_array = NULL;
        if (stream_ctx->stream_type == TYPE_SUBS) {
            hls_session_ctx->sub_index = session_index;
        }
        in_dynarray_add(&hls_session_ctx->stream_array, &hls_session_ctx->nb_session, stream_ctx);
        HLOG("[%s:%d] add stream item, index : %d, media type : %d",
            __FUNCTION__, __LINE__, session_index, hls_session->media_item_array[session_index]->media_type);
    }
    int64_t min_start_time = -1;
    for (session_index = 0; session_index < hls_session_ctx->nb_session; session_index++) {
        if (hls_session_ctx->stream_array[session_index]->stream_type >= TYPE_SUBS
            || hls_session_ctx->stream_array[session_index]->forbid) { // skip
            continue;
        }

        ret = _hls_parse_next_segment(s, session_index, 1);
        if (ret) {
            HLOG("[%s:%d] read header failed ! need to release hls session context !", __FUNCTION__, __LINE__);
            m3u_session_close(session);
            _release_hls_stream_context(s);
            return ret;
        }
        if (hls_session_ctx->stream_array[session_index]->ctx->start_time > 0) {
            if (min_start_time == -1 || hls_session_ctx->stream_array[session_index]->ctx->start_time < min_start_time) {
                min_start_time = hls_session_ctx->stream_array[session_index]->ctx->start_time;
            }
        }
    }
    s->start_time = min_start_time;
    HLOG("[%s:%d] read header success ! demuxer stream number is : %d, media item number is : %d, start time(%lld us)",
        __FUNCTION__, __LINE__, hls_session_ctx->nb_session, hls_session->media_item_num, s->start_time);
    return 0;
}

static int hls_read_packet(AVFormatContext * s, AVPacket * pkt) {
    FFMPEG_HLS_SESSION_CONTEXT * hls_session_ctx = (FFMPEG_HLS_SESSION_CONTEXT *)(s->priv_data);

    int ret = 0;
    int prev_stream_index = 0;
    int cur_index = 0;
    int index = 0;
    AVFormatContext * format = NULL;

    if(hls_session_ctx->fffb_changed){
        for (index = 0; index < hls_session_ctx->nb_session; index++) {
            format = hls_session_ctx->stream_array[index]->ctx;
            if (!format) {
                HLOG("[%s:%d] index:%d, format context is null !", __FUNCTION__, __LINE__,index);
                return AVERROR(EINVAL);
            }
            HLOG("flush index:%d cur_pkt\n",index);
            ff_read_frame_flush(format);
        }

        if(hls_session_ctx->seek_changed)
            hls_session_ctx->seek_changed = 0;
        if(hls_session_ctx->fffb_changed)
            hls_session_ctx->fffb_changed = 0;
    }

RETRY_SELECT:
    // read alternatively.
    cur_index = _select_hls_session(s);
    if (cur_index < 0) {
        HLOG("[%s:%d] no hls session to read !", __FUNCTION__, __LINE__);
        return AVERROR_EOF;
    }

RETRY_READ:

    if(hls_interrupt_call_cb())
        return AVERROR_EOF;

    // TODO: there may exist solo and multiplex streams in m3u8, need to modify here.
    format = hls_session_ctx->stream_array[cur_index]->ctx;
    if (!format) {
        HLOG("[%s:%d] format context is null !", __FUNCTION__, __LINE__);
        return AVERROR(EINVAL);
    }

    ret = av_read_frame(format, pkt);
    if (ret == AVERROR_EOF) {
        hls_session_ctx->stream_array[cur_index]->eof = 1;
        HLOG("[%s:%d] stream(%d) read to EOF !", __FUNCTION__, __LINE__, cur_index);
        if(hls_session_ctx->ff_fb >= 1){
            return AVERROR_EOF;
        }
        goto RETRY_SELECT;
    } else if (ret == AVERROR(EAGAIN)) {
        //HLOG("[%s:%d] stream(%d) eagain, retry read !", __FUNCTION__, __LINE__, cur_index);
        goto RETRY_SELECT;
    } else if (ret == AVERROR_EXIT) {
        HLOG("[%s:%d] stream(%d) exit !", __FUNCTION__, __LINE__, cur_index);
        return ret;
    } else if (ret == HLS_STREAM_EOF) {
        hls_session_ctx->stream_array[cur_index]->eof = 1;
        HLOG("[%s:%d] stream(%d) read to the end !", __FUNCTION__, __LINE__, cur_index);
        if(hls_session_ctx->ff_fb >= 1){
            return AVERROR_EOF;
        }
        goto RETRY_SELECT;
    } else if (ret < 0) {
        HLOG("[%s:%d] stream(%d) unknown err : %d !", __FUNCTION__, __LINE__, cur_index, ret);
    }

    if (ret < 0) {
        // no need to reopen format context if ts.
        if (memcmp(format->iformat->name, "mpegts", 6) != 0) {
            ret = _hls_parse_next_segment(s, cur_index, 0);
            if (ret >= 0) {
                HLOG("[%s:%d] stream(%d) parse next segment, continue to read !", __FUNCTION__, __LINE__, cur_index);
                goto RETRY_READ;
            } else if (ret == AVERROR_EXIT) {
                HLOG("[%s:%d] stream(%d) parse next segment, exit !", __FUNCTION__, __LINE__, cur_index);
                return ret;
            } else if (ret == HLS_STREAM_EOF) {
                HLOG("[%s:%d] stream(%d) parse next segment, read to the end !", __FUNCTION__, __LINE__, cur_index);
                goto RETRY_SELECT;
            } else {
                HLOG("[%s:%d] stream(%d) parse next segment, failed(%d) !", __FUNCTION__, __LINE__, cur_index, ret);
                return ret;
            }
        } else {
            return ret;
        }
    }

    prev_stream_index = pkt->stream_index;
    if (pkt->pts != AV_NOPTS_VALUE) {
        pkt->pts = av_rescale_q(pkt->pts, format->streams[prev_stream_index]->time_base, HLS_Rational);
    }
    if (pkt->dts != AV_NOPTS_VALUE) {
        pkt->dts = av_rescale_q(pkt->dts, format->streams[prev_stream_index]->time_base, HLS_Rational);
    }

    HLS_STREAM_CONTEXT * stream_ctx = hls_session_ctx->stream_array[cur_index]->stream_info_array[prev_stream_index];
    if ((stream_ctx->type != TYPE_AUDIO && stream_ctx->type != TYPE_VIDEO)
        || stream_ctx->need_drop) {
        av_free_packet(pkt);
        goto RETRY_READ;
    }
    if (hls_session_ctx->audio_stream_index < 0 && stream_ctx->type == TYPE_AUDIO) {
        hls_session_ctx->audio_stream_index = stream_ctx->stream_id;
    } else if (hls_session_ctx->video_stream_index < 0 && stream_ctx->type == TYPE_VIDEO) {
        hls_session_ctx->video_stream_index = stream_ctx->stream_id;
    }
    if (stream_ctx->type == TYPE_AUDIO) {
        pkt->stream_index = hls_session_ctx->audio_stream_index;
    } else if (stream_ctx->type == TYPE_VIDEO) {
        pkt->stream_index = hls_session_ctx->video_stream_index;
    }

    if (pkt->duration > 0 && !stream_ctx->duration) {
        stream_ctx->duration = pkt->duration;
    }

    if (pkt->pts != AV_NOPTS_VALUE) {
#if 0
        // pts offset inside segment.
        if (stream_ctx->inner_segment_start_pts == -1) {
            stream_ctx->inner_segment_start_pts = pkt->pts;
        }
        pkt->pts -= stream_ctx->inner_segment_start_pts;
        pkt->dts -= stream_ctx->inner_segment_start_pts;
        // pts offset inside stream.
        pkt->pts += stream_ctx->segment_start_pts;
        pkt->dts += stream_ctx->segment_start_pts;
#endif
        if (s->start_time > 0) {
            pkt->pts -= s->start_time;
            pkt->dts -= s->start_time;
        }
        if (pkt->duration > 0) {
            stream_ctx->next_pts = pkt->pts + pkt->duration;
        } else {
            stream_ctx->next_pts = pkt->pts + stream_ctx->duration;
        }
    }

    // drop audio packet to switch point.
    if (hls_session_ctx->stream_array[cur_index]->stream_type == TYPE_AUDIO
        && hls_session_ctx->audio_anchor_timeUs >= 0) {
        if (pkt->pts < hls_session_ctx->audio_anchor_timeUs) {
            av_free_packet(pkt);
            goto RETRY_READ;
        } else {
            hls_session_ctx->audio_anchor_timeUs = -1;
            HLOG("[%s:%d] audio start reading after switch !", __FUNCTION__, __LINE__);
        }
    }
    if (pkt->pts >= 0) {
        hls_session_ctx->sub_read_reference_timeUs = _get_clock_monotonic_us() - pkt->pts;
    }

    if (hls_session_ctx->vhandle && stream_ctx->type == TYPE_VIDEO) {
        fwrite(pkt->data, 1, pkt->size, hls_session_ctx->vhandle);
        fflush(hls_session_ctx->vhandle);
    }
    if (hls_session_ctx->ahandle && stream_ctx->type == TYPE_AUDIO) {
        fwrite(pkt->data, 1, pkt->size, hls_session_ctx->ahandle);
        fflush(hls_session_ctx->ahandle);
    }

    return 0;
}

static int hls_read_seek(AVFormatContext * s, int stream_index, int64_t timestamp, int flags) {
    FFMPEG_HLS_SESSION_CONTEXT * hls_session_ctx = (FFMPEG_HLS_SESSION_CONTEXT *)(s->priv_data);

    int index = 0, nb = 0;
    int64_t seek_pos = 0;
    int64_t real_pos = 0;
    // pts->ms
    if (stream_index == -1) {
        seek_pos = av_rescale_rnd(timestamp, 1000, AV_TIME_BASE, AV_ROUND_ZERO);
    } else {
        AVStream * st = s->streams[stream_index];
        seek_pos = av_rescale_rnd(timestamp, st->time_base.num * 1000, st->time_base.den, AV_ROUND_ZERO);
    }
    seek_pos *= 1000; // ms->us
    real_pos = m3u_session_seekUs((void *)(hls_session_ctx->session_ctx), seek_pos, hls_interrupt_call_cb);
    HLOG("[%s:%d] seek to (%lld)us, return (%lld)us", __FUNCTION__, __LINE__, seek_pos, real_pos);

    hls_session_ctx->prev_read_session_index = -1;
    for (; index < hls_session_ctx->nb_session; index++) {
        if (hls_session_ctx->stream_array[index]->pb) {
            avio_reset(hls_session_ctx->stream_array[index]->pb, AVIO_FLAG_READ);
            hls_session_ctx->stream_array[index]->eof = 0;

            AVFormatContext * format = hls_session_ctx->stream_array[index]->ctx;
            if (!format) {
                HLOG("[%s:%d] index:%d, format context is null !", __FUNCTION__, __LINE__,index);
                continue;
            }
            HLOG("flush index:%d cur_pkt\n",index);
            ff_read_frame_flush(format);
        }
    }
    hls_session_ctx->seek_changed = 0;
#if 0
    for (index = 0; index < hls_session_ctx->nb_session; index++) {
        if (hls_session_ctx->stream_array[index]->stream_type > TYPE_VIDEO
            || hls_session_ctx->stream_array[index]->forbid) { // skip
            if (hls_session_ctx->stream_array[index]->stream_type == TYPE_SUBS) {
                hls_session_ctx->sub_read_reference_timeUs = _get_clock_monotonic_us() - real_pos;
            }
            continue;
        }
        if (_hls_parse_next_segment(s, index, 0) != 0) {
            HLOG("[%s:%d] parse next segment failed, index : %d", __FUNCTION__, __LINE__, index);
            return -1;
        }
        for (nb = 0; nb < hls_session_ctx->stream_array[index]->stream_nb; nb++) {
            // TODO: to fix this seek pts
            hls_session_ctx->stream_array[index]->stream_info_array[nb]->segment_start_pts = real_pos;
            hls_session_ctx->stream_array[index]->stream_info_array[nb]->next_pts = 0;
            hls_session_ctx->stream_array[index]->stream_info_array[nb]->inner_segment_start_pts = -1;
            HLOG("[%s:%d] session index(%d), stream index(%d), segment start pts(%lld)us",
                __FUNCTION__, __LINE__, index, nb, hls_session_ctx->stream_array[index]->stream_info_array[nb]->segment_start_pts);
        }
    }
#endif
    return 0;
}

static int hls_read_close(AVFormatContext * s) {
    FFMPEG_HLS_SESSION_CONTEXT * hls_session_ctx = (FFMPEG_HLS_SESSION_CONTEXT *)(s->priv_data);

    HLOG("[%s:%d] enter read close !", __FUNCTION__, __LINE__);
    AVFormatContext * format = NULL;
    AVIOContext * pb = NULL;
    URLContext * h =NULL;
    int index = 0;
    for (; index < hls_session_ctx->nb_session; index++) {
        format = hls_session_ctx->stream_array[index]->ctx;
        if (format) {
            av_close_input_stream(format);
            hls_session_ctx->stream_array[index]->ctx = NULL;
        }
        pb = hls_session_ctx->stream_array[index]->pb;
        if (pb) {
            h = pb->opaque;
            if (pb && pb->buffer) {
                av_free(pb->buffer);
                av_free(pb);
            }
            if (h) {
                if (h->lpbuf) {
                    url_lpfree(h);
                }
                if (h->prot) {
                    av_free(h->prot);
                }
                av_free(h);
            }
            hls_session_ctx->stream_array[index]->pb = NULL;
        }
    }
    m3u_session_close((void *)(hls_session_ctx->session_ctx));
    _release_hls_stream_context(s);
    if (hls_session_ctx->vhandle) {
        fclose(hls_session_ctx->vhandle);
    }
    if (hls_session_ctx->ahandle) {
        fclose(hls_session_ctx->ahandle);
    }
    if(hls_session_ctx->h) {
        free(hls_session_ctx->h);
        hls_session_ctx->h = NULL;
    }
    HLOG("[%s:%d] read close successfully !", __FUNCTION__, __LINE__);
    return 0;
}

static int hls_set_parameter(AVFormatContext * s, int para, int type, int64_t value, int64_t value1) {
    FFMPEG_HLS_SESSION_CONTEXT * hls_session_ctx = (FFMPEG_HLS_SESSION_CONTEXT *)(s->priv_data);

    int codec_vbuf_data_len = 0;
    int codec_abuf_data_len = 0;
    int index = 0;
    int ret = -1;
    MediaType type_to_set = TYPE_NONE;
    if (AVCMD_SET_CODEC_BUFFER_INFO == para) {
        if (type == 5) { // audio pts, ms->us
            hls_session_ctx->last_audio_read_timeUs = value * 1000;
        } else {
            if (type == 1) { // video buffer size
                hls_session_ctx->codec_vbuf_size = (int)value;
                return 0;
            } else if (type == 2) { // audio buffer size
                hls_session_ctx->codec_abuf_size = (int)value;
                return 0;
            } else if (type == 3) { // video buffer data length
				hls_session_ctx->codec_vdata_len = (int)value;
                codec_vbuf_data_len = (int)value;
                type_to_set = TYPE_VIDEO;
            } else if (type == 4) { // audio buffer data length
				hls_session_ctx->codec_adata_len = (int)value;
                codec_abuf_data_len = (int)value;
                type_to_set = TYPE_AUDIO;
            }
            int bw = 0, buffer_time_s = 0;
            for (; index < hls_session_ctx->nb_session; index++) {
                if (hls_session_ctx->stream_array[index]->stream_type == type_to_set) {
                    break;
                }
            }
            if (index == hls_session_ctx->nb_session) {
                //HLOG("[%s:%d] not found valid stream, type(%d) !", __FUNCTION__, __LINE__, type);
                return -1;
            }
            m3u_session_media_get_current_bandwidth((void *)hls_session_ctx->session_ctx, index, &bw);
            if (type == 4) {
                if (((float)codec_abuf_data_len / hls_session_ctx->codec_abuf_size) > 0.9) { // high buffer level, assume it be full.
                    buffer_time_s = MEDIA_CACHED_BUFFER_THREASHOLD + 1; // hack
                } else {
                    if (bw > 0) {
                        buffer_time_s = codec_abuf_data_len * 8 / bw;
                    }
                }
            } else if (type == 3) {
                if (((float)codec_vbuf_data_len / hls_session_ctx->codec_vbuf_size) > 0.9) { // high buffer level, assume it be full.
                    buffer_time_s = MEDIA_CACHED_BUFFER_THREASHOLD + 1; // hack
                } else {
                    if (bw > 0) {
                        buffer_time_s = codec_vbuf_data_len * 8 / bw;
                    }
                }
            }
            m3u_session_media_set_codec_buffer_time((void *)hls_session_ctx->session_ctx, index, buffer_time_s);
        }
    }else if (AVCMD_SET_FF_FB_INFO == para) {
        hls_session_ctx->fffb_changed = 1;
        hls_session_ctx->ff_fb = type;
        ret = m3u_media_session_ff_fb((void *)hls_session_ctx->session_ctx, type, value, value1); 
        int index = 0;
        for (; index < hls_session_ctx->nb_session; index++) {
            if (hls_session_ctx->stream_array[index]->pb) {
                avio_reset(hls_session_ctx->stream_array[index]->pb, AVIO_FLAG_READ);
                hls_session_ctx->stream_array[index]->eof = 0;
            }
        }
    } else {
        HLOG("[%s:%d] unsupported parameter(0x%x) !", __FUNCTION__, __LINE__, para);
        return -1;
    }
    return 0;
}

static int hls_get_parameter(AVFormatContext * s, int para, int data1, void * info1, void *** info2) {
    FFMPEG_HLS_SESSION_CONTEXT * hls_session_ctx = (FFMPEG_HLS_SESSION_CONTEXT *)(s->priv_data);

    HLOG("[%s:%d] get parameter, para(%d)", __FUNCTION__, __LINE__, para);
    int ret = -1, i = 0;
    if (para == 1) { // get track info
        int validTrackCount = 0;
        M3uTrackInfo * trackInfo = NULL;
        int trackCount = m3u_session_media_get_track_count((void *)(hls_session_ctx->session_ctx));
        for (; i < trackCount; i++) {
            trackInfo = m3u_session_media_get_track_info((void *)(hls_session_ctx->session_ctx), i);
            if (trackInfo) {
                AVStreamInfo * tmp_info = (AVStreamInfo *)av_mallocz(sizeof(AVStreamInfo));
                memcpy(tmp_info, trackInfo, sizeof(AVStreamInfo)); // M3uTrackInfo must be the same with AVStreamInfo.
                free(trackInfo); // free inner ptr in player.
                in_dynarray_add(info2, &validTrackCount, tmp_info);
            }
        }
        if (validTrackCount > 0 && validTrackCount == trackCount) {
            *((int *)info1) = validTrackCount;
            ret = 0;
            HLOG("[%s:%d] get track info success !", __FUNCTION__, __LINE__);
        }
    } else if (para == 2) { // get selected track
        int selected_index = m3u_session_media_get_selected_track((void *)(hls_session_ctx->session_ctx), data1);
        if (selected_index >= 0) {
            *((int *)info1) = selected_index;
            ret = 0;
            HLOG("[%s:%d] get selected track success !", __FUNCTION__, __LINE__);
        }
    } else if (para == 3) { // get track count
        int trackCount = m3u_session_media_get_track_count((void *)(hls_session_ctx->session_ctx));
        if (trackCount > 0) {
            *((int *)info1) = trackCount;
            ret = 0;
            HLOG("[%s:%d] get track count success !", __FUNCTION__, __LINE__);
        }
    } else if (para == 4) { // get track type
        *((int *)info1) = m3u_session_media_get_type_by_index((void *)(hls_session_ctx->session_ctx), data1);
        if (*((int *)info1) > 0) {
            ret = 0;
        }
    }else if (para == 5) { // get Iframe list info
    *((int *)info1) = m3u_session_has_iframe_list(hls_session_ctx->session_ctx);
    } else if (para == AVCMD_GET_HLSINFO) {
        m3u8_get_hls_para_info((void *)(hls_session_ctx->session_ctx), data1, info1);
    }
    return ret;
}

static int hls_select_stream(AVFormatContext * s, int index, int select) {
    FFMPEG_HLS_SESSION_CONTEXT * hls_session_ctx = (FFMPEG_HLS_SESSION_CONTEXT *)(s->priv_data);

    int ret = -1;
    MediaType type = m3u_session_media_get_type_by_index((void *)(hls_session_ctx->session_ctx), index);
    HLOG("[%s:%d] switch stream, index(%d), select(%d), type(%d)", __FUNCTION__, __LINE__, index, select, type);
    // only support audio/sub now.
    if (type == TYPE_AUDIO) {
        HLOG("[%s:%d] switch audio stream, switch pos(%lld us)", __FUNCTION__, __LINE__, hls_session_ctx->last_audio_read_timeUs);
        ret = m3u_session_media_select_track((void *)(hls_session_ctx->session_ctx), index, select, (hls_session_ctx->last_audio_read_timeUs == -1) ? 0 : hls_session_ctx->last_audio_read_timeUs);
        if (!ret) {
            hls_session_ctx->audio_anchor_timeUs = hls_session_ctx->last_audio_read_timeUs;
        }
    } else if (type == TYPE_SUBS) {
        pthread_mutex_lock(&hls_session_ctx->sub_lock);
        HLOG("[%s:%d] switch sub stream, switch pos(%lld us)", __FUNCTION__, __LINE__, hls_session_ctx->last_sub_read_timeUs);
        ret = m3u_session_media_select_track((void *)(hls_session_ctx->session_ctx), index, select, (hls_session_ctx->last_sub_read_timeUs == -1) ? 0 : hls_session_ctx->last_sub_read_timeUs);
        if (!ret) {
            hls_session_ctx->sub_next_read_timeUs = -1;
            hls_session_ctx->sub_read_flag = select;
        }
        pthread_mutex_unlock(&hls_session_ctx->sub_lock);
    }
    if (!ret) {
        int i = 0, stream_index = 0;
        for (; i < hls_session_ctx->nb_session; i++) {
            if (type == hls_session_ctx->stream_array[i]->stream_type) {
                stream_index = i;
                break;
            }
        }
        if (hls_session_ctx->stream_array[stream_index]->pb) {
            avio_reset(hls_session_ctx->stream_array[stream_index]->pb, AVIO_FLAG_READ);
            hls_session_ctx->stream_array[stream_index]->eof = 0;
        }
        if (type <= TYPE_VIDEO && !hls_session_ctx->stream_array[stream_index]->parsed) {
            ret = _hls_parse_next_segment(s, stream_index, 1);
            if (ret) {
                HLOG("[%s:%d] type(%d) parse failed !", __FUNCTION__, __LINE__, type);
                return ret;
            }
        }
        hls_session_ctx->stream_array[stream_index]->forbid = (hls_session_ctx->session_ctx->media_item_array[stream_index]->media_url[0] == '\0');
        FFMPEG_HLS_STREAM_CONTEXT * hls_stream_ctx = NULL;
        int k = 0;
        for (; k < hls_session_ctx->nb_session; k++) {
            hls_stream_ctx = hls_session_ctx->stream_array[k];
            if (hls_stream_ctx->stream_nb > 1) {
                int i = 0;
                for (; i < hls_stream_ctx->stream_nb; i++) {
                    hls_stream_ctx->stream_info_array[i]->need_drop = 0;
                    if (hls_stream_ctx->stream_info_array[i]->type != hls_stream_ctx->stream_type) {
                        int j = 0;
                        for (; j < hls_session_ctx->nb_session; j++) {
                            if (hls_stream_ctx->stream_info_array[i]->type == hls_session_ctx->stream_array[j]->stream_type
                                && !hls_session_ctx->stream_array[j]->forbid) {
                                hls_stream_ctx->stream_info_array[i]->need_drop = 1;
                                HLOG("[%s:%d] stream(%d) need to drop !", __FUNCTION__, __LINE__, hls_stream_ctx->stream_info_array[i]->type);
                            }
                        }
                    }
                }
            }
        }
        HLOG("[%s:%d] switch stream success !", __FUNCTION__, __LINE__);
    }
    return ret;
}

static void * hls_read_subtitle(AVFormatContext * s) {
    FFMPEG_HLS_SESSION_CONTEXT * hls_session_ctx = (FFMPEG_HLS_SESSION_CONTEXT *)(s->priv_data);

    pthread_mutex_lock(&hls_session_ctx->sub_lock);
    if (!hls_session_ctx->sub_read_flag) {
        pthread_mutex_unlock(&hls_session_ctx->sub_lock);
        return NULL;
    }
#if 0
    if (_get_clock_monotonic_us() < hls_session_ctx->sub_next_read_timeUs) {
        pthread_mutex_unlock(&hls_session_ctx->sub_lock);
        return NULL;
    }
#endif
    M3uSubtitleData * sub = m3u_session_media_read_subtitle((void *)(hls_session_ctx->session_ctx), hls_session_ctx->sub_index);
    if (sub) {
        hls_session_ctx->sub_next_read_timeUs = hls_session_ctx->sub_read_reference_timeUs + sub->sub_timeUs;
        hls_session_ctx->last_sub_read_timeUs = sub->sub_timeUs;
    }
    pthread_mutex_unlock(&hls_session_ctx->sub_lock);
    return sub;
}

static const AVOption options[] = {
    {"pid", "set notify pid", offsetof(FFMPEG_HLS_SESSION_CONTEXT, notify_id), FF_OPT_TYPE_INT, 0, 0, 100},
    {NULL},
};

static const AVClass hls_class = {
    .class_name = "hls demuxer",
    .item_name  = av_default_item_name,
    .option     = options,
    .version    = LIBAVUTIL_VERSION_INT,
};

AVInputFormat ff_mhls_demuxer = {
    .name            = "mhls",
    .long_name       = NULL,
    .priv_data_size  = sizeof(FFMPEG_HLS_SESSION_CONTEXT),
    .read_probe      = hls_read_probe,
    .read_header     = hls_read_header,
    .read_packet     = hls_read_packet,
    .read_close      = hls_read_close,
    .read_seek       = hls_read_seek,
    .flags           = AVFMT_NOFILE,
    .set_parameter   = hls_set_parameter,
    .get_parameter   = hls_get_parameter,
    .select_stream   = hls_select_stream,
    .read_subtitle   = hls_read_subtitle,
    .priv_class      = &hls_class,
};
