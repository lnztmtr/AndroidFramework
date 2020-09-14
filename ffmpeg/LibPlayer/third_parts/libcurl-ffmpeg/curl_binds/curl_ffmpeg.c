/*
 * libcurl registered as ffmpeg protocol
 * Copyright (c) amlogic,2013, senbai.tao<senbai.tao@amlogic.com>
 *
 * This file is part of FFmpeg.
 *
 * FFmpeg is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * FFmpeg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with FFmpeg; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

#include "amconfigutils.h"
#include <amthreadpool.h>
#include "libavformat/avformat.h"
#include "libavformat/internal.h"
#include "libavformat/url.h"
#include "libavutil/avstring.h"
#include "libavutil/opt.h"
#include "curl_fetch.h"
#include "curl_log.h"

typedef struct _CURLFFContext {
    const AVClass *class;
    char uri[MAX_CURL_URI_SIZE];
    int read_retry;
	int force_interrupt;
    int64_t read_waittime_s;
    CFContext * cfc_h;
    int64_t latest_get_time_ms;
    int report_flag;
} CURLFFContext;

static const AVOption options[] = {{NULL}};
static const AVClass curlffmpeg_class = {
    .class_name     = "Amlcurlffmpeg",
    .item_name      = av_default_item_name,
    .option             = options,
    .version            = LIBAVUTIL_VERSION_INT,
};

static int curl_interrupt_call_cb(void * handle)
{
	CURLFFContext * curl_handle = (CURLFFContext *)handle;
    if (url_interrupt_cb() || curl_handle->force_interrupt == 1) {
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
        p += 6; /* skip "rrsip" */
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
    if (ls != p)
    {
            av_strlcpy(hostname, p,
                       FFMIN(ls + 1 - p, hostname_size));
    }
}
/*add end*/

// add exception case in this function when need retry.
static int curl_ffmpeg_need_retry(int arg)
{
    int ret = -1;
    switch(arg) {
    case CURLERROR(CURLE_RECV_ERROR  + C_ERROR_PERFORM_BASE_ERROR):  // recv failure
    case CURLERROR(CURLE_PARTIAL_FILE  + C_ERROR_PERFORM_BASE_ERROR):  // partial file
    case CURLERROR(C_ERROR_PERFORM_SELECT_ERROR):
        ret = 0;
        break;
    case CURLERROR(CURLE_COULDNT_CONNECT  + C_ERROR_PERFORM_BASE_ERROR): // couldn't connect
    case CURLERROR(CURLE_COULDNT_RESOLVE_HOST  + C_ERROR_PERFORM_BASE_ERROR): // couldn't resolve host
    case CURLERROR(CURLE_OPERATION_TIMEDOUT + C_ERROR_PERFORM_BASE_ERROR):
    case CURLERROR(CURLE_GOT_NOTHING + C_ERROR_PERFORM_BASE_ERROR):
        ret = 1;
        break;
    default:
        break;
    }
    return ret;
}

static void curl_ffmpeg_register_interrupt(CURLFFContext *h, interruptcallbackwithpid pfunc)
{
    if (!h || !h->cfc_h) {
        return;
    }
    curl_fetch_register_interrupt_pid(h->cfc_h, pfunc);
    curl_fetch_set_parent_pid(h->cfc_h, h);
    return;
}
/*add by zhaolianghua for jiangxi RongZai @20170117*/
void parser_rrsip(const char* uri, char *url){

   CLOGE("parser rrsip, uri:%s\n",uri);
   char orig_hostname[256];
   char rrsip_hostname[256];
   char path[MAX_URL_SIZE];
   int port = 0;
   memset(url,0,MAX_CURL_URI_SIZE);
   char *p;
   //memcpy(url,uri,strlen(uri));
   if(strstr(uri,"curl:")){
      p = uri + 5;
   }else{
      p = uri;
   }
   av_url_split(NULL, 0, NULL, 0, orig_hostname, sizeof(orig_hostname), &port,
                 path, sizeof(path), p);
   av_rrsip_url_split(rrsip_hostname, sizeof(rrsip_hostname),path,sizeof(path),p);

   CLOGE("get hostname: %s,rrsip_hostname:%s\n",orig_hostname, rrsip_hostname);

   sprintf(url ,"%s%s%s","shttp://",rrsip_hostname,p + 8 + strlen(orig_hostname));

   CLOGE("parser rrsip, rrsip_url:%s\n",url);

}
/*add end*/

#define PLAYER_EVENTS_ERROR 3
static int curl_ffmpeg_open(URLContext *h, const char *uri, int flags)
{
    CLOGI("curl_ffmpeg_open enter, flags=%d\n", flags);
    int ret = -1;
    char redirect_url[MAX_CURL_URI_SIZE] = {0};
    /*add by zhaolianghua for jiangxi RongZai @20170117*/
    char rrsip_url[MAX_CURL_URI_SIZE] = {0};
    /*add end*/
    int clear_redirect_url = 0;
    CURLFFContext * handle = NULL;
    if (!uri || strlen(uri) < 1 || strlen(uri) > MAX_CURL_URI_SIZE) {
        CLOGE("Invalid curl-ffmpeg uri\n");
        return ret;
    }
    int retries = 0;
    int max_retry = am_getconfig_int_def("libplayer.curl.openretry", 20);
    int retry_503_cnt = am_getconfig_int_def("libplayer.curl.503_retry", 0);
    /*add by zhaolianghua for jiangxi RongZai @20170117*/
    int rrsip_support = am_getconfig_int_def("libplayer.rrsip.support", 0);
    int rrsip_host = 0;
    /*add end*/

RETRY:
    if (url_interrupt_cb()) {
        return AVERROR(EINTR);
    }
    handle = (CURLFFContext *)av_mallocz(sizeof(CURLFFContext));
    if (!handle) {
        CLOGE("Failed to allocate memory for CURLFFContext handle\n");
        return ret;
    }
    memset(handle->uri, 0, sizeof(handle->uri));
    if (ret == 1 && redirect_url[0] != '\0') {
        av_strlcpy(handle->uri, redirect_url, strlen(redirect_url));
	/*add by zhaolianghua for jiangxi RongZai @20170117*/
	} else if(rrsip_host){
		av_strlcpy(handle->uri, rrsip_url, strlen(rrsip_url));
	/*add end*/
    } else {
        if (av_stristart(uri, "curl:", NULL)) {
            av_strlcpy(handle->uri, uri + 5, sizeof(handle->uri));
        } else {
            av_strlcpy(handle->uri, uri, sizeof(handle->uri));
        }
    }
    handle->cfc_h = curl_fetch_init(handle->uri, h->headers, clear_redirect_url);
    if (!handle->cfc_h) {
        CLOGE("curl_fetch_init failed\n");
	c_free(handle);
        return ret;
    }
    /*add by zhaolianghua for jiangxi RongZai @20170117*/
    if(strstr(uri,"rrsip") && rrsip_support){
        rrsip_host = 1;
        parser_rrsip(uri,rrsip_url);
        CLOGE("has rrsip host, new uri:%s\n",rrsip_url);
    }
    /*add end*/
	
    curl_ffmpeg_register_interrupt(handle, curl_interrupt_call_cb);
    handle->force_interrupt = 0;
    ret = curl_fetch_open(handle->cfc_h);
    if (ret) {
        h->http_code = handle->cfc_h->http_code;
        CLOGI("curl_fetch_open ret:%d, httpcode:%d\n", ret, h->http_code);
        if (ret == 1 && redirect_url[0] == '\0') {
            av_strlcpy(redirect_url, handle->cfc_h->relocation, strlen(handle->cfc_h->relocation));
            clear_redirect_url = 0;
        } else {
            if (h->http_code == 503 && retry_503_cnt > 0
                && handle->cfc_h != NULL && handle->cfc_h->relocation != NULL
                && handle->cfc_h->relocation[0] != '\0') {
                av_strlcpy(redirect_url, handle->cfc_h->relocation, strlen(handle->cfc_h->relocation));
                clear_redirect_url = 0;
                retry_503_cnt--;
            } else {
                clear_redirect_url = 1; //need clear redirect url
                redirect_url[0] = '\0';
            }
        }
        curl_fetch_close(handle->cfc_h);
        av_free(handle);
        handle = NULL;
        if (++retries < max_retry) {
            goto RETRY;
        }
        if ((h->http_code >= 400 && h->http_code < 600 && h->http_code != 401) || h->http_code == ETIMEDOUT)
            return (-h->http_code);
        else
            return ret;
    }
    handle->read_retry = (int)am_getconfig_float_def("libplayer.curl.readretry", 10);
    handle->read_waittime_s = (int64_t)am_getconfig_float_def("libplayer.curl.readwaitS", 3600);
    h->http_code = handle->cfc_h->http_code;
    h->is_slowmedia = 1;
    h->is_streamed = handle->cfc_h->seekable ? 0 : 1;
    h->location = handle->cfc_h->relocation;
    h->priv_data = handle;
    return ret;
}

static int curl_ffmpeg_read(URLContext *h, uint8_t *buf, int size)
{
    int ret = -1;
    int64_t curtime;
    CURLFFContext * s = (CURLFFContext *)h->priv_data;
    if (!s) {
        CLOGE("CURLFFContext invalid\n");
        return ret;
    }
    int counts = 1000, retries = 0;// 10s
    int wait_flag = 0;
    int64_t start_watitime_s = 0;
#if 1
    do {
        if (url_interrupt_cb()) {
            return AVERROR(EINTR);
        }
        ret = curl_fetch_read(s->cfc_h, buf, size);

        if (ret == C_ERROR_EAGAIN) {
            amthreadpool_thread_usleep(10 * 1000);
        }
		if(ret <= 0){
            curtime = av_gettime();
            if(s->latest_get_time_ms <= 0)
                s->latest_get_time_ms = curtime;
             if(am_getconfig_bool("media.player.gd_report.enable")&&(s->report_flag==0)&&
                (curtime >  s->latest_get_time_ms + am_getconfig_int_def("media.player.read_report.timeout", 30)*1000*1000)){
                s->report_flag = 1;
                ffmpeg_notify(h, PLAYER_EVENTS_ERROR, 54000, 0);
                CLOGI("%s %d curtime:%lld latest_get_time_ms:%lld ret:%d\n",__FUNCTION__,__LINE__, curtime, s->latest_get_time_ms, ret);
                CLOGI("%s %d report error code 54000\n",__FUNCTION__,__LINE__);
             }else if(am_getconfig_bool("media.player.cmcc_report.enable")&&(s->report_flag==0)&&(curtime >  s->latest_get_time_ms + 30*1000*1000)){
                s->report_flag = 1;
                ffmpeg_notify(h, PLAYER_EVENTS_ERROR, 10002, 0);
             }


		}else{
	        s->latest_get_time_ms = 0;
	        s->report_flag = 0;
			}
        if (ret > 0 || ret == C_ERROR_UNKNOW) {
            break;
        }

        if(ret <= 0) {
            if(am_getconfig_int_def("net.ethwifi.up",3)==0) {
                return C_ERROR_EAGAIN;
            }
        } 
      
        /* just temporary, need to modify later */
        if (ret < C_ERROR_EAGAIN) {
            if (!curl_ffmpeg_need_retry(ret)) {
                CLOGI("curl_ffmpeg_read need retry! retries=%d, ret=%d\n", retries, ret);
                if (retries++ < s->read_retry) {
                    curl_fetch_seek(s->cfc_h, s->cfc_h->cwd->size, SEEK_SET);
                    counts = 200;
                }
            } else if (curl_ffmpeg_need_retry(ret) == 1) {
                if (!wait_flag) {
                    start_watitime_s = av_gettime() / 1000000;
                    wait_flag = 1;
                }
                CLOGI("curl_ffmpeg_read need wait to reconnect!\n");
                if (av_gettime() / 1000000 - start_watitime_s <= s->read_waittime_s) {
                    curl_fetch_seek(s->cfc_h, s->cfc_h->cwd->size, SEEK_SET);
                    amthreadpool_thread_usleep(100 * 1000);
                    counts = 200;
                }
            } else if (ret == CURLERROR(33 + C_ERROR_PERFORM_BASE_ERROR)) {
                /*live stream with file size > 0, return -33 CURLE_RANGE_ERROR*/
                ret = AVERROR(ENOSR);
            } else {
                ret = AVERROR(ENETRESET);
                break;
            }
        }
        else if(s->cfc_h->cwc_h &&(s->cfc_h->cwc_h->is_use_block_request==1)&&(ret == 0) && (s->cfc_h->cwd->size<s->cfc_h->filesize-1)){
            if(retries++ < s->read_retry) {
                curl_fetch_seek(s->cfc_h, s->cfc_h->cwd->size, SEEK_SET);
                counts = 200;
            }	    
    	    CLOGI("%s %d use block request\n",__FUNCTION__,__LINE__);			
        }
        /*change the code place,date read fail  do not seek right postion*/
        if(am_getconfig_int_def("net.ethwifi.up",3)==0){

            return AVERROR(EINTR);
        }
    } while (counts-- > 0);
#else
    ret = curl_fetch_read(s->cfc_h, buf, size);
#endif


#if 0
    FILE * fp = fopen("/temp/curl_dump.dat", "ab+");
    if (fp) {
        fwrite(buf, 1, ret, fp);
        fflush(fp);
        fclose(fp);
    }
#endif

    return ret;
}

static int64_t curl_ffmpeg_seek(URLContext *h, int64_t off, int whence)
{
    CLOGI("curl_ffmpeg_seek enter\n");
    int64_t ret = -1;
    CURLFFContext * s = (CURLFFContext *)h->priv_data;
    if (!s) {
        CLOGE("CURLFFContext invalid\n");
        return ret;
    }
    if (!s->cfc_h) {
        CLOGE("CURLFFContext invalid CFContext handle\n");
        return ret;
    }
    if ((whence == SEEK_CUR && !off) ||
        (whence == SEEK_END && off < 0)) {
        s->force_interrupt = 0;
    } else {
        s->force_interrupt = 1;
    }
    if (whence == AVSEEK_CURL_HTTP_KEEPALIVE) {
        ret = curl_fetch_http_keepalive_open(s->cfc_h, NULL);
    } else if (whence == AVSEEK_SIZE) {
        ret = s->cfc_h->filesize;
    } else {
        ret = curl_fetch_seek(s->cfc_h, off, whence);
    }
    s->force_interrupt = 0;
    return ret;
}

static int curl_ffmpeg_close(URLContext *h)
{
    CLOGI("curl_ffmpeg_close enter\n");
    CURLFFContext * s = (CURLFFContext *)h->priv_data;
    if (!s) {
        return -1;
    }
    s->force_interrupt = 1;
    curl_fetch_close(s->cfc_h);
    av_free(s);
    s = NULL;
    return 0;
}

static int curl_ffmpeg_get_info(URLContext *h, uint32_t  cmd, uint32_t flag, int64_t *info)
{
    CURLFFContext * s = (CURLFFContext *)h->priv_data;
    if (!s) {
        return -1;
    }
    int ret = 0;
    if (cmd == AVCMD_GET_NETSTREAMINFO) {
        if (flag == 1) {
            double tmp_info = 0.0;
            ret = curl_fetch_get_info(s->cfc_h, C_INFO_SPEED_DOWNLOAD, flag, (void *)&tmp_info);
            if (!ret) {
                *info = (int64_t)(tmp_info * 8);
            } else {
                *info = 0;
            }
        } else if (flag == 3) {
            int64_t http_code = 0;
            ret = curl_fetch_get_info(s->cfc_h, C_INFO_RESPONSE_CODE, flag, (void * )&http_code);
            if(!ret) {
                *info = http_code;
            } else {
                *info = 0;
            }
        }
    }
    return 0;
}

URLProtocol ff_curl_protocol = {
    .name               = "curl",
    .url_open           = curl_ffmpeg_open,
    .url_read           = curl_ffmpeg_read,
    .url_write          = NULL,
    .url_seek           = curl_ffmpeg_seek,
    //.url_exseek           = curl_ffmpeg_seek,
    .url_close          = curl_ffmpeg_close,
    .url_getinfo            = curl_ffmpeg_get_info,
    .url_get_file_handle    = NULL,
    .url_check          = NULL,
    .priv_data_size       = 0,
    .priv_data_class      = NULL,
};
