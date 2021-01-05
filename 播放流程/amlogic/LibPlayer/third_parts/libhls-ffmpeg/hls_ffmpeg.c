#include "libavutil/opt.h"
#include "libavformat/internal.h"

#include "hls_session.h"

#define HLS_DEMUXER_TAG "hls-ffmpeg"
#define HLOG(...) av_tag_log(HLS_DEMUXER_TAG, __VA_ARGS__)

// hls code porting from ffmpeg
struct FFMPEG_HLS_CONTEXT {
    AVClass *class;
    AVFormatContext *ctx;
    struct hls_session *session;

    char *user_agent;                    ///< holds HTTP user agent set as an AVOption to the HTTP protocol context
    char *cookies;                       ///< holds HTTP cookie values set in either the initial response or as an AVOption to the HTTP protocol context
    char *headers;                       ///< holds HTTP headers set as an AVOption to the HTTP protocol context
    char *http_proxy;                    ///< holds the address of the HTTP proxy server
    int (*interrupt)(void);
};

static int hls_read_header(AVFormatContext *s)
{
    HLOG("Use hls ffmpeg mod read header \n");
    int ret = 0;
    struct FFMPEG_HLS_CONTEXT *context = (struct FFMPEG_HLS_CONTEXT *)s->priv_data;
    memset(context, 0, sizeof(struct FFMPEG_HLS_CONTEXT));
    struct hls_session *session = (struct hls_session *)malloc(sizeof(struct hls_session));
    if(!session)
        return -1;
    memset(session, 0, sizeof(struct hls_session));
    context->session = session;
    session->ctx = s;
    char * inner_url = s->filename;
    if (av_strstart(inner_url, "mhls:", NULL) != 0
        || av_strstart(inner_url, "vhls:", NULL) != 0
        || av_strstart(inner_url, "list:", NULL) != 0) {
        inner_url = s->filename + 6;
    }
    session->url = strdup(inner_url);
    ret = hls_session_open(session);
    return ret;
}

static int hls_read_packet(AVFormatContext *s, AVPacket *pkt)
{
    struct FFMPEG_HLS_CONTEXT *context = (struct FFMPEG_HLS_CONTEXT *)s->priv_data;
    return hls_session_read_packet(context->session, pkt);
}

static int hls_close(AVFormatContext *s)
{
    struct FFMPEG_HLS_CONTEXT *context = (struct FFMPEG_HLS_CONTEXT *)s->priv_data;
    return hls_session_close(context->session);
}

static int hls_read_seek(AVFormatContext *s, int stream_index, int64_t timestamp, int flags)
{
    struct FFMPEG_HLS_CONTEXT *context = (struct FFMPEG_HLS_CONTEXT *)s->priv_data;
    return hls_session_read_seek(context->session, stream_index, timestamp, flags);
}

static int hls_probe(AVProbeData *p)
{
    HLOG("[%s:%d] read probe ! filename : %s ", __FUNCTION__, __LINE__, p->filename);
    if (av_strstart(p->filename, "mhls:", NULL) != 0 || av_strstart(p->filename, "list:", NULL) != 0) {
        HLOG("[%s:%d] hls demuxer has been probed !", __FUNCTION__, __LINE__);
        return AVPROBE_SCORE_MAX;
    }
    return 0;

    /*  Require #EXTM3U at the start, and either one of the ones below
     * somewhere for a proper match. */
    if (strncmp(p->buf, "#EXTM3U", 7))
        return 0;
    if (strstr(p->buf, "#EXT-X-STREAM-INF:")     ||
        strstr(p->buf, "#EXT-X-TARGETDURATION:") ||
        strstr(p->buf, "#EXT-X-MEDIA-SEQUENCE:"))
        return AVPROBE_SCORE_MAX;
    return 0;
}

static const AVOption hls_options[] = {
    {NULL},
};

static const AVClass hls_class = {
    .class_name = "aml hls demuxer",
    .item_name  = av_default_item_name,
    .option     = hls_options,
    .version    = LIBAVUTIL_VERSION_INT,
};

AVInputFormat aml_hls_demuxer = {
    .name            = "mhls",
    .long_name       = NULL,
    .priv_class      = &hls_class,
    .priv_data_size  = sizeof(struct FFMPEG_HLS_CONTEXT),
    .read_probe      = hls_probe,
    .read_header     = hls_read_header,
    .read_packet     = hls_read_packet,
    .read_close      = hls_close,
    .read_seek       = hls_read_seek,
    .flags           = AVFMT_NOFILE,
};
