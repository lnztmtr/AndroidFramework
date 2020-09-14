#include <unistd.h>
#include "libavformat/avio.h"
#include "hls_m3ulivesession.h"
#include "amconfigutils.h"
#include "hls_cmf_impl.h"
#include <amthreadpool.h>

#ifndef INT_MAX
#define INT_MAX   2147483647
#endif

#define FFMPEG_HLS_TAG "amffmpeg-hls"

#define RLOG(...) av_tag_log(FFMPEG_HLS_TAG,__VA_ARGS__)


typedef struct _FFMPEG_HLS_CONTEXT {
    int bandwidth_num;
    int64_t durationUs;
    int codec_buf_level;
    int codec_vbuf_size;
    int codec_abuf_size;
    int codec_vdat_size;
    int codec_adat_size;
    int debug_level;
    int ff_fb;
    FILE * dump_handle;
    void* hls_ctx;
    CmfPrivContext_t* cmf_ctx;
    int64_t latest_get_time_ms;
    int report_flag;
} FFMPEG_HLS_CONTEXT;

typedef enum _SysPropType {
    PROP_DEBUG_LEVEL = 0,
    PROP_CMF_SUPPORT = 1,

} SysPropType;
/*add by zhaolianghua for jiangxi RongZai @20170117*/
char *main_rrs_url = NULL;
char *spare_rrs_url = NULL;
/*add end*/
static float _get_system_prop(SysPropType type)
{
    float value = 0.0;
    float ret = -1;
    switch (type) {
    case PROP_DEBUG_LEVEL:
        ret = am_getconfig_float("libplayer.hls.debug", &value);
        if (ret < 0) {
            ret = 0;
            value = 0;
        }
        break;
    case PROP_CMF_SUPPORT:
        ret = am_getconfig_bool("libplayer.hls.cmf");
        if (ret < 0) {
            value = -1;
        } else {
            value = ret;
        }
        break;
    default:
        RLOG("Never see this line,type:%d\n", type);
        break;
    }
    if (ret < 0 || value < 0) {
        ret = -1;
    } else {
        ret = value;
    }

    return ret;

}
static int interrupt_call_cb(void)
{
    if (url_interrupt_cb()) {
        //RLOG("[%s]:url_interrupt_cb\n", __FUNCTION__);
        return 1;
    }
    return 0;

}

/*add by zhaolianghua for jiangxi RongZai @20170117*/
static void av_rrsip_url_split(char *hostname, int hostname_size,
                  char *path, int path_size,
                  const char *url)
{
    const char *p, *ls, *at, *col, *brk;
    if (hostname_size > 0)      hostname[0] = 0;
    if (path_size > 0)          path[0] = 0;
    /* parse protocol */
    if ((p = strstr(url, "rrsip")))
    {
        p += 6; /* skip "rrsip=" */
    }
    else
    {
        /* no protocol means plain filename */
        av_strlcpy(path, url, path_size);
        return;
    }
    /* separate path from hostname */
    ls = strchr(p, '&');
    if (!ls)
        ls = strchr(p, '?');
    if (ls)
        av_strlcpy(path, ls, path_size);
    else
        ls = &p[strlen(p)]; // XXX
    /* the rest is hostname, use that to parse auth/port */
    if (ls != p){
            av_strlcpy(hostname, p,
                       FFMIN(ls + 1 - p, hostname_size));
     }
}

void parser_rrsip(const char* uri, char *url){
	RLOG("parser rrsip, uri:%s\n",uri);
	char orig_hostname[256];
	char rrsip_hostname[256];
	char path[2048];
	int port = 0;
	memset(url,0,2048);
	char *p;
	if(strstr(uri,"vhls:")){
		p = uri + 5;
	}else{
		p = uri;
	}
	av_url_split(NULL, 0, NULL, 0, orig_hostname, sizeof(orig_hostname), &port,
			path, sizeof(path), p);
	av_rrsip_url_split(rrsip_hostname, sizeof(rrsip_hostname),path,sizeof(path),p);
	RLOG("get hostname: %s,rrsip_hostname:%s\n",orig_hostname, rrsip_hostname);
	sprintf(url ,"%s%s%s","vhls:shttp://",rrsip_hostname,p + 8 + strlen(orig_hostname));
	RLOG("parser rrsip, rrsip_url:%s\n",url);
}
/*add by zhaolianghua end @20170117*/

static int ffmpeg_hls_open(URLContext *h, const char *filename, int flags)
{
    if (h == NULL || filename == NULL || (strstr(filename, "vhls:") == NULL && strstr(filename, "vrwc:") == NULL)) {
        return -1;
    }
    /*modify by zhaolianghua for jiangxi RongZai @20170117*/
    int ret = -1;
    char *main_ip = NULL;
    char *spare_ip = NULL;
    void* session = NULL;
    int prop_prefix_size = strlen("vhls:");
    int retrycnt = am_getconfig_int_def("libplayer.hlsopen_retry", 10);
    int rrsip_host = 0;
    int rrsip_support = am_getconfig_int_def("libplayer.rrsip.support", 0);
    char rrsip_url[2048] = {0};
    char * uri = NULL;
openretry:
    if(rrsip_host){
	uri = rrsip_url;
    }else{		
	uri = filename;
	RLOG("has rrsip host, new uri:%s\n",uri);
    }
    ret = m3u_session_open(uri + prop_prefix_size, h->headers, &session, h);
    if (ret < 0) {
	RLOG("Failed to open session and retrycnt %d , ret:%d\n", retrycnt, ret);
         //0,no switch;1,switch only once;2,switch to the other each time
         int type_v4v6_switch = am_getconfig_int_def("media.libplayer.switch_v4v6", 0);
         RLOG("[%s,%d],type_v4v6_switch %d\n", __FUNCTION__, __LINE__,type_v4v6_switch);
         if (0 != type_v4v6_switch)
         {
             int get_ip_priority = am_getconfig_int_def("libplayer.tcp.get_ip_priority", 0);
             static int m_set = 0;
             RLOG("[%s,%d],get_ip_priority %d,m_set:%d\n", __FUNCTION__, __LINE__,get_ip_priority,m_set);
             if(1 == type_v4v6_switch && 0 == m_set)
             {
                 am_setconfig_int("libplayer.tcp.get_ip_priority", get_ip_priority > 0 ? 0 : 1);
                 m_set = 1;
             }
             else if (2 == type_v4v6_switch)
             {
                 am_setconfig_int("libplayer.tcp.get_ip_priority", get_ip_priority > 0 ? 0 : 1);
             }
         }
    if(strstr(filename,"rrsip") && rrsip_support){
        rrsip_host = 1;
        parser_rrsip(filename,rrsip_url);
        RLOG("has rrsip host, new uri:%s\n",rrsip_url);
    }
	/*modify by zhaolianghua end@20190117*/
        h->http_code = abs(ret);
        if ((am_getconfig_int_def("net.ethwifi.up", 3) != 0) || (am_getconfig_int_def("media.libplayer.apk_diagnostic", 0) == 1)) {
            retrycnt--;
        }
        if (retrycnt > 0 && url_interrupt_cb() <= 0&&(ret!=-ERROR_URL_NOT_M3U8)) {
            goto openretry;
        } else {
            return ret;
        }
    }

    FFMPEG_HLS_CONTEXT* f = av_mallocz(sizeof(FFMPEG_HLS_CONTEXT));

    m3u_session_register_interrupt(session, interrupt_call_cb);

    int stream_num = 0;
    ret = m3u_session_get_stream_num(session, &stream_num);
    f->bandwidth_num = stream_num;

    int64_t dur;
    ret = m3u_session_get_durationUs(session, &dur);
    f->durationUs = dur;
    f->debug_level = (int)_get_system_prop(PROP_DEBUG_LEVEL);
    if (dur > 0) {
        h->support_time_seek = 1;
    } else { //live streaming
        h->support_time_seek = 0;
        //h->flags|=URL_MINI_BUFFER;//no lpbuffer
        //h->flags|=URL_NO_LP_BUFFER;
    }
    if (_get_system_prop(PROP_CMF_SUPPORT) > 0) {
        f->cmf_ctx = av_mallocz(sizeof(CmfPrivContext_t));
        f->cmf_ctx->totol_clip_num = hls_cmf_get_clip_num(session);
        f->cmf_ctx->interrupt_func_cb = interrupt_call_cb;
    }
    h->is_slowmedia = 1;
    h->is_streamed = 1;
    h->is_segment_media = 1;
    f->hls_ctx = session;
    f->ff_fb = 0;
    f->dump_handle = fopen("/data/tmp/iframe.dat", "wb+");
    h->priv_data = f;
    return 0;
}
static int ffmpeg_hls_read(URLContext *h, unsigned char *buf, int size)
{
    if (h == NULL || h->priv_data == NULL || size <= 0) {
        RLOG("failed to read data\n");
        return -1;
    }
    FFMPEG_HLS_CONTEXT* ctx = (FFMPEG_HLS_CONTEXT*)h->priv_data;
    void * hSession = ctx->hls_ctx;
    M3ULiveSession* session = NULL;
    int len = AVERROR(EIO);
    int counts = 10;
    int64_t curtime;
    static int get_data_timeout_error;

    do {
        if (url_interrupt_cb() > 0) {
            RLOG("url_interrupt_cb\n");
            len = AVERROR(EIO);
            break;
        }
        len = m3u_session_read_data(hSession, buf, size); //wait read.
        if(len<0){
            curtime = av_gettime();
            if(ctx->latest_get_time_ms <= 0)
                ctx->latest_get_time_ms = curtime;
            if((am_getconfig_bool("media.player.gd_report.enable") ||
               am_getconfig_bool("media.player.cmcc_report.enable"))&&(ctx->report_flag==0)&&(curtime >  ctx->latest_get_time_ms + 30*1000*1000)){
                #define PLAYER_EVENTS_ERROR 3 
                ctx->report_flag = 1;
                if(am_getconfig_bool("media.player.gd_report.enable"))
                    get_data_timeout_error = 54000;
                else if (am_getconfig_bool("media.player.cmcc_report.enable"))
                     get_data_timeout_error = 10002;
                ffmpeg_notify(h, PLAYER_EVENTS_ERROR, get_data_timeout_error, 0);
            }
        }else{
            ctx->latest_get_time_ms = 0;
            ctx->report_flag = 0;
        }
        if ((am_getconfig_int_def("net.ethwifi.up", 3) == 0) && (len < 0)) {
            return AVERROR(EAGAIN);
        }

        if (len < 0) {
            if (len == AVERROR(EAGAIN)) {
                ;///amthreadpool_thread_usleep(1000*100);
            } else {
                break;
            }
        }
        if (len >= 0) {
            break;
        }
    } while (counts-- > 0);
    if (ctx->debug_level > 5) {
        RLOG("%s,want:%d,got:%d\n", __FUNCTION__, size, len);
    }
    if (ctx->ff_fb > 0) {
		if(len != 188)
       		RLOG("not return 0 end read iframe data! ret=%d", len);
		session = (M3ULiveSession*)hSession;
		if(session != NULL){
			if((ctx->ff_fb==1)&&(session->fffb_endflag == 1)&&(len == -11)){
				RLOG("ff in end return 0 fffb end\n");
				len = 0;
			}
		}
        if (ctx->dump_handle && len > 0) {
            fwrite(buf, 1, len, ctx->dump_handle);
            fflush(ctx->dump_handle);
        }
    }
    return len;
}

static int64_t _ffmpeg_cmf_seek(URLContext *h, int64_t pos, int whence)
{
    FFMPEG_HLS_CONTEXT* ctx = (FFMPEG_HLS_CONTEXT*)h->priv_data;
    void * hSession = ctx->hls_ctx;
    if (hSession == NULL || ctx->cmf_ctx == NULL) {
        RLOG("Failed call :%s\n", __FUNCTION__);
        return -1;
    }
    RLOG("%s:pos:%lld,whence:%x\n", __FUNCTION__, pos, whence);
    if (whence == SEEK_SET) { //just seek within one slice
        int64_t ret = hls_cmf_seek_by_size(hSession, ctx->cmf_ctx, pos);
        RLOG("Seek to clip pos:%lld,rv:%lld\n", pos, ret);
        if (ret < 0) {
            return -1;
        } else {
            return pos;
        }
    } else if (whence == AVSEEK_ITEM_TIME) {
        if (ctx->durationUs > 0) {
            int64_t item_st = m3u_session_get_next_segment_st(hSession);
            if (ctx->debug_level > 3) {
                RLOG("Got next item start time:%lld us\n", item_st);
            }
            return item_st / 1000000;
        }
    } else if (whence == AVSEEK_SLICE_BYINDEX) {

        int64_t timeUs = hls_cmf_seek_by_index(hSession, ctx->cmf_ctx, pos);
        RLOG("Seek to clip pos:%lld,timeUs:%lld\n", pos, timeUs);


        return pos;
    } else if (whence == AVSEEK_SLICE_BYTIME) {
        int64_t index = hls_cmf_shift_index_by_time(hSession, ctx->cmf_ctx, pos * 1000);
        RLOG("Get clip index by time,time:%lld ms,index:%lld\n", pos, index);
        return index;
    }
    return -1;
}
static int64_t ffmpeg_hls_seek(URLContext *h, int64_t pos, int whence)
{
    if (h == NULL || h->priv_data == NULL) {
        RLOG("Failed call :%s\n", __FUNCTION__);
        return -1;
    }
    FFMPEG_HLS_CONTEXT* ctx = (FFMPEG_HLS_CONTEXT*)h->priv_data;
    void * hSession = ctx->hls_ctx;

    if (whence == AVSEEK_SIZE) {
        RLOG("%s:pos:%lld,whence:%x\n", __FUNCTION__, pos, whence);
        return AVERROR_STREAM_SIZE_NOTVALID;
    } else if (_get_system_prop(PROP_CMF_SUPPORT) > 0 && ctx->durationUs > 0) {
        return _ffmpeg_cmf_seek(h, pos, whence);
    }

    return -1;
}
static int64_t _ffmpeg_cmf_exseek(URLContext *h, int64_t pos, int whence)
{
    FFMPEG_HLS_CONTEXT* ctx = (FFMPEG_HLS_CONTEXT*)h->priv_data;
    int ret = -1;
    void * hSession = ctx->hls_ctx;
    if (whence == AVSEEK_CMF_TS_TIME) {
        int64_t seekToUs = ctx->cmf_ctx->cur_clip_st + pos * 1000000;
        int64_t seekLimitUs = ctx->cmf_ctx->cur_clip_end;
        if (ctx->durationUs > 0 && pos >= 0 && seekToUs <= seekLimitUs) {
            int64_t seekTo = m3u_session_seekUs(hSession, seekToUs, interrupt_call_cb);
            RLOG("Seek to clip time:%lld,pos\n", seekTo, pos);
            return (seekTo / 1000000);
        }
    }
    return -1;

}
static int64_t ffmpeg_hls_exseek(URLContext *h, int64_t pos, int whence)
{
    if (h == NULL || h->priv_data == NULL) {
        RLOG("Failed call :%s\n", __FUNCTION__);
        return -1;
    }
    FFMPEG_HLS_CONTEXT* ctx = (FFMPEG_HLS_CONTEXT*)h->priv_data;
    void * hSession = ctx->hls_ctx;
    int ret;
    if (whence == AVSEEK_FULLTIME) {
        if (ctx->durationUs > 0) {
            return (int64_t)(ctx->durationUs / 1000000);
        } else {
            return -1;
        }

    } else if (whence == AVSEEK_BUFFERED_TIME) {
        int sec;
        m3u_session_get_cached_data_time(hSession, &sec);
        if (ctx->debug_level > 3) {
            RLOG("Got buffered time:%d\n", sec);
        }
        return sec;

    } else if (whence == AVSEEK_TO_TIME) {
#ifdef LIVEPLAY_SEEK
        if (pos >= 0) {
#else
        if (ctx->durationUs > 0 && pos >= 0 && (pos * 1000000) < ctx->durationUs) {
#endif
            int64_t seekToUs = m3u_session_seekUs(hSession, pos * 1000000, interrupt_call_cb);
            RLOG("Seek to time:%lld\n", seekToUs);
            return (seekToUs / 1000000);
        }

    } else if (whence == AVSEEK_LOOPBUFFER_OFFSET) {
        if (ctx->durationUs > 0 && pos >= 0 && (pos * 1000000) < ctx->durationUs) {
            RLOG("loopbuffer offset Seek to time :%lld\n", pos);
            int64_t seekToUs = m3u_session_seekUs_offset(hSession, pos * 1000000, &(h->priv_info));
            if (seekToUs >= 0) {
                return seekToUs / 1000000;
            }
            return -1;
        }
    } else if (whence == AVSEEK_ITEM_TIME) { //just for get will-download item time,for xiaomi,by zc
        if (ctx->durationUs > 0) {
            int64_t item_st = m3u_session_get_next_segment_st(hSession);
            if (ctx->debug_level > 3) {
                RLOG("Got next item start time:%lld us\n", item_st);
            }
            return item_st / 1000000;
        }
    } else if (_get_system_prop(PROP_CMF_SUPPORT) > 0 && ctx->durationUs > 0) { //only vod
        return _ffmpeg_cmf_exseek(h, pos, whence);
    }
    RLOG("Never support this case,pos:%lld,whence:%d\n", (long long)pos, whence);
    return -1;
}
static int ffmpeg_hls_close(URLContext *h)
{
    if (h == NULL) {
        RLOG("Failed call :%s\n", __FUNCTION__);
        return -1;
    }
    FFMPEG_HLS_CONTEXT* ctx = (FFMPEG_HLS_CONTEXT*)h->priv_data;
    if (ctx->dump_handle) {
        fclose(ctx->dump_handle);
    }
    if (ctx != NULL && ctx->hls_ctx != NULL) {
        m3u_session_close(ctx->hls_ctx);
    }
    if (_get_system_prop(PROP_CMF_SUPPORT) > 0 && ctx->cmf_ctx != NULL) {
        av_free(ctx->cmf_ctx);
    }
    av_free(h->priv_data);

    return 0;

}

static int ffmpeg_hls_get_handle(URLContext *h)
{
    return (intptr_t)h->priv_data;
}

static int _hls_estimate_buffer_time(FFMPEG_HLS_CONTEXT* ctx, int cur_bw)
{
    int dat_len = 0;
    int buffer_t = 0;
    if (ctx->codec_buf_level > 0 && cur_bw > 0) {
        dat_len = ctx->codec_vdat_size + ctx->codec_adat_size;
        buffer_t = dat_len * 8 / cur_bw;
    }
    if (ctx->debug_level > 3) {
        RLOG("Current stream in codec buffer can last %d seconds for playback\n", buffer_t);
    }
    return buffer_t;
}
static int ffmpeg_hls_setopt(URLContext *h, int cmd, int flag, unsigned long info, int64_t info1)
{
    if (h == NULL || h->priv_data == NULL) {
        RLOG("Failed call :%s\n", __FUNCTION__);
        return -1;
    }
    FFMPEG_HLS_CONTEXT* ctx = (FFMPEG_HLS_CONTEXT*)h->priv_data;
    int ret = -1;
    if (AVCMD_SET_CODEC_BUFFER_INFO == cmd) {
        if (flag == 0) {
            ctx->codec_buf_level = info;
        } else if (flag == 1) {
            ctx->codec_vbuf_size = info;
        } else if (flag == 2) {
            ctx->codec_abuf_size = info;
        } else if (flag == 3) {
            ctx->codec_vdat_size = info;
        } else if (flag == 4) {
            ctx->codec_adat_size = info;
        }
        if (ctx->bandwidth_num > 0) {
            int bw = 0;
            m3u_session_get_cur_bandwidth(ctx->hls_ctx, &bw);
            int sec = _hls_estimate_buffer_time(ctx, bw);
            m3u_session_set_codec_data(ctx->hls_ctx, sec);

        }

        if (ctx->debug_level > 3) {
            RLOG("set codec buffer,type = %d,info=%d\n", flag, (int)info);
        }
        ret = 0;
    }else if (AVCMD_HLS_SWITCH_TIMESHIFT == cmd) {
        m3u_session_set_livemode(ctx->hls_ctx, 1); // switch to timeshift only
    }else if (AVCMD_SET_FF_FB_INFO == cmd) {
        ctx->ff_fb = flag;
        ret = m3u_session_ff_fb(ctx->hls_ctx, flag, info, info1);
    }
    return ret;
}
static int _ffmpeg_cmf_getopt(URLContext *h, uint32_t  cmd, uint32_t flag, int64_t* info)
{
    FFMPEG_HLS_CONTEXT* ctx = (FFMPEG_HLS_CONTEXT*)h->priv_data;
    int ret = -1;
    CmfPrivContext_t* cmf_ctx = ctx->cmf_ctx;
    RLOG("[%s]:cmd:%d,flag:%d\n", __FUNCTION__, cmd, flag);
    if (ctx == NULL || cmf_ctx == NULL) {
        RLOG("Failed call :%s\n", __FUNCTION__);
        return -1;
    }
    if (cmd == AVCMD_TOTAL_NUM) {
        *info = cmf_ctx->totol_clip_num;
        RLOG("Get total clip num:%d\n", cmf_ctx->totol_clip_num);
    } else if (cmd == AVCMD_TOTAL_DURATION) {
        *info = ctx->durationUs / 1000;
        RLOG("Get duration:%lld\n", *info);
    } else if (cmd == AVCMD_SLICE_START_OFFSET) {
        *info = -1;
        RLOG("Get clip start offset\n");
    } else if (cmd == AVCMD_SLICE_INDEX) {
        *info = cmf_ctx->cur_clip_index;
        RLOG("Get clip index:%d\n", cmf_ctx->cur_clip_index);
    } else if (cmd == AVCMD_SLICE_SIZE) {
        if (cmf_ctx->cur_clip_len > 0) {
            *info = cmf_ctx->cur_clip_len;
        } else { //ugly codes
            int64_t ret = hls_cmf_get_fsize(ctx->hls_ctx, cmf_ctx, 1);
            if (ret <= 0) { //10s,maybe chunk stream,can't get size from server
                int counts = 100;
                while (counts-- > 0 && ret <= 0) {
                    if (url_interrupt_cb()) {
                        RLOG("url_interrupt_cb,%d\n", __LINE__);
                        ret = 0;
                        break;
                    }
                    ret = hls_cmf_get_fsize(ctx->hls_ctx, cmf_ctx, 2);
                    amthreadpool_thread_usleep(100 * 1000); //100ms
                }
                if (ret <= 0 && url_interrupt_cb() == 0) {
                    ret = hls_cmf_get_fsize(ctx->hls_ctx, cmf_ctx, 3);
                }
            }
            *info = ret;

        }
        RLOG("Get clip length:%lld\n", *info);
    } else if (cmd == AVCMD_SLICE_STARTTIME) {
        if (ctx->durationUs <= 0) {
            *info = -1;
        } else {
            *info = cmf_ctx->cur_clip_st / 1000;
        }
        RLOG("Get clip start time:%lld\n", *info);
    } else if (cmd == AVCMD_SLICE_ENDTIME) {
        if (ctx->durationUs <= 0) {
            *info = -1;
        } else {
            *info = cmf_ctx->cur_clip_end / 1000;
        }
        RLOG("Get clip end time:%lld\n", *info);
    } else {
        RLOG("Can't excute this case\n");
    }
    return 0;
}
static int ffmpeg_hls_getopt(URLContext *h, int  cmd, int flag, void* info)
{
    if (h == NULL || h->priv_data == NULL) {
        RLOG("Failed call :%s\n", __FUNCTION__);
        return -1;
    }
    FFMPEG_HLS_CONTEXT* ctx = (FFMPEG_HLS_CONTEXT*)h->priv_data;
    int val = 0;
    int64_t *hlsinfo = info;
    if (cmd == AVCMD_GET_NETSTREAMINFO) {
        if (flag == 1) {
            m3u_session_get_estimate_bandwidth(ctx->hls_ctx, &val);
            *hlsinfo = val;
            if (ctx->debug_level > 5) {
                RLOG("Get measured bandwidth: %0.3f kbps\n", (float)*hlsinfo / 1000.000);
            }
        } else if (flag == 2) {
            m3u_session_get_cur_bandwidth(ctx->hls_ctx, &val);
            *hlsinfo = val;
            if (ctx->debug_level > 5) {
                RLOG("Get current bandwidth: %0.3f kbps\n", (float)*hlsinfo / 1000.000);
            }
        } else if (flag == 3) {
            m3u_session_get_error_code(ctx->hls_ctx, &val);
            *hlsinfo = val;

            if (ctx->debug_level > 5) {
                RLOG("Get cache http error code: %d\n", val);
            }
        } else if (flag == 5) {
            m3u_session_get_livemode(ctx->hls_ctx, &val);
            *hlsinfo = val;

            if (ctx->debug_level > 5) {
                RLOG("Get current hls livemode: %d\n", *hlsinfo);
            }
        } else if (flag == 6) {
            m3u_session_get_estimate_bps(ctx->hls_ctx, &val);
            *hlsinfo = val;
            if (ctx->debug_level > 5) {
                RLOG("Get measured bps: %0.3f kbps\n", (float)*hlsinfo / 1000.000);
            }
        } else if (flag == 7) {
            *hlsinfo = m3u_session_have_endlist(ctx->hls_ctx);
            if (ctx->debug_level > 5) {
                RLOG("Get current hls livemode: %d\n", *hlsinfo);
            }
        } else if (flag == 8) {
           m3u_session_get_fffb_end(ctx->hls_ctx, &val);
           *hlsinfo = val;
            if (ctx->debug_level > 5) {
               RLOG("end flag = %d\n",*hlsinfo);
           }
       }else if (flag == 9) {
           m3u_session_get_cached_data_bytes(ctx->hls_ctx, &val);
           *hlsinfo = val;
            if (ctx->debug_level > 5) {
               RLOG("cached bytes= %d\n",*hlsinfo);
           }
       }else if (flag == 10) {
           *hlsinfo =  m3u_session_has_iframe_list(ctx->hls_ctx);
            if (ctx->debug_level > 5) {
               RLOG("has iframe list = %d\n",*hlsinfo);
           }
        }
    } else if (cmd == AVCMD_HLS_STREAMTYPE) {
        if (ctx->durationUs > 0) {
            *hlsinfo = 1;
        } else {
            *hlsinfo = 0;
        }
        RLOG("Get hls stream type,:%d\n", *hlsinfo);
    } else if (_get_system_prop(PROP_CMF_SUPPORT) > 0 && ctx->durationUs > 0) {
        return _ffmpeg_cmf_getopt(h, cmd, flag, info);
    } else if (cmd == AVCMD_GET_HLSINFO) {
        m3u8_get_hls_para_info(ctx->hls_ctx,  flag,  info);
    }

    return 0;
}


URLProtocol vhls_protocol = {
    .name                   = "vhls",
    .url_open               = ffmpeg_hls_open,
    .url_read               = ffmpeg_hls_read,
    .url_seek               = ffmpeg_hls_seek,
    .url_close              = ffmpeg_hls_close,
    .url_exseek             = ffmpeg_hls_exseek, /*same as seek is ok*/
    .url_get_file_handle    = ffmpeg_hls_get_handle,
    .url_setcmd             = ffmpeg_hls_setopt,
    .url_getinfo            = ffmpeg_hls_getopt,
};

URLProtocol vrwc_protocol = {//for verimatrix drm link
    .name                   = "vrwc",
    .url_open               = ffmpeg_hls_open,
    .url_read               = ffmpeg_hls_read,
    .url_seek               = ffmpeg_hls_seek,
    .url_close              = ffmpeg_hls_close,
    .url_exseek             = ffmpeg_hls_exseek, /*same as seek is ok*/
    .url_get_file_handle    = ffmpeg_hls_get_handle,
    .url_setcmd             = ffmpeg_hls_setopt,
    .url_getinfo            = ffmpeg_hls_getopt,
};

