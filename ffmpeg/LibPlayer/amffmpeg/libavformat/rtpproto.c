/*
 * RTP network protocol
 * Copyright (c) 2002 Fabrice Bellard
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

/**
 * @file
 * RTP protocol
 */

#include "libavutil/parseutils.h"
#include "libavutil/avstring.h"
#include "avformat.h"
#include "avio_internal.h"
#include "rtpdec.h"
#include "url.h"

#include <unistd.h>
#include <stdarg.h>
#include "internal.h"
#include "network.h"
#include "os_support.h"
#include <fcntl.h>
#if HAVE_POLL_H
#include <sys/poll.h>
#endif
#include <sys/time.h>
#include "libavcodec/get_bits.h"
#include <amthreadpool.h>
#include <itemlist.h>
#include "RS_fec.h"
#include "bandwidth_measure.h"
#include <cutils/properties.h>

#define RTP_TX_BUF_SIZE  (64 * 1024)
#define RTP_RX_BUF_SIZE  (128 * 1024)
#define RTPPROTO_RECVBUF_SIZE 3 * RTP_MAX_PACKET_LENGTH
#define MIN_CACHE_PACKET_SIZE 5

#ifndef min
#define min(x, y) ((x) < (y) ? (x) : (y))
#endif
#define FEC_RECVBUF_SIZE 3000
#define MAX_FEC_RTP_PACKET_NUM 300
#define MAX_FEC_PACKET_NUM 10
#define MAX_FEC_MAP_NUM 310

#define EXTRA_BUFFER_PACKET_NUM 20

#define FORCE_OUTPUT_PACKET_NUM_THRESHOLD 1000
#define FORCE_OUTPUT_PACKET_NUM 20
#define FEC_PAYLOAD_TYPE 127
typedef struct FEC_DATA_STRUCT {
    uint16_t rtp_begin_seq; 
    uint16_t rtp_end_seq; 
    uint8_t redund_num;
    uint8_t redund_idx; 
    uint16_t fec_len; 
    uint16_t rtp_len; 
    uint16_t rsv;
    uint8_t *fec_data;					// point to rtp buffer
} FEC_DATA_STRUCT;

typedef struct RTPFECPacket {
    uint16_t seq;
    uint8_t payload_type;
    uint8_t *buf;						//recv buffer
    int len;

    FEC_DATA_STRUCT * fec;			// fec struct
} RTPFECPacket;

typedef struct RTPFECContext {
    URLContext *rtp_hd, *fec_hd;
    int rtp_fd, fec_fd;
    int pre_fec_lost, pre_fec_lost_last;
    int after_fec_lost, after_fec_lost_last;
    int total_num, total_num_last;
    long last_time;
    int pre_fec_ratio;
    int after_fec_ratio;


    volatile uint8_t brunning;
    pthread_t recv_thread;

    uint8_t bdecode;
    struct itemlist recvlist;		
    struct itemlist outlist;
    struct itemlist feclist;
/*
    RTPFECPacket *fec_packet[MAX_FEC_PACKET_NUM];
    RTPFECPacket *rtp_packet[MAX_FEC_RTP_PACKET_NUM];

    uint8_t *fec_data_array[MAX_FEC_PACKET_NUM];
    uint8_t *rtp_data_array[MAX_FEC_RTP_PACKET_NUM];
    uint8_t lost_map[MAX_FEC_MAP_NUM];
*/    
    FEC_DATA_STRUCT * cur_fec;
    uint16_t rtp_last_decode_seq;
    uint16_t rtp_media_packet_sum; 
	uint8_t rtp_seq_discontinue;
	uint8_t fec_seq_discontinue;

    T_RS_FEC_MONDE *fec_handle;
    void* bandwidth_measure;
} RTPFECContext;

static RTPFECPacket *fec_packet[MAX_FEC_PACKET_NUM];
static RTPFECPacket *rtp_packet[MAX_FEC_RTP_PACKET_NUM];

static PBYTE fec_data_array[MAX_FEC_PACKET_NUM];
static PBYTE rtp_data_array[MAX_FEC_RTP_PACKET_NUM];
static int lost_map[MAX_FEC_MAP_NUM];

//#define TRACE() av_log(NULL, AV_LOG_INFO, "[%s:%d]\n", __FUNCTION__, __LINE__);
#define TRACE()

/**
 * If no filename is given to av_open_input_file because you want to
 * get the local port first, then you must call this function to set
 * the remote server address.
 *
 * @param h media file context
 * @param uri of the remote server
 * @return zero if no error.
 */
static int gd_report_error_enable = 0;
static int get_data_timeout_error = 0;
static int get_data_timeout = 30;
#define PLAYER_EVENTS_ERROR 3

/* +[SE] [REQ][IPTV-19][jungle.wang]:add fast channel switch module */
static int max_rtp_buf = 10000;//also used for rtp without fcc
static int wait_min_queue_size = 10;
static int wait_order_timeout = 100;    //100ms
static int wait_order_packet_low = 30;
static int sequence_order_range = 500;
static int normal_wait_first_rtcp_timeout = 600;
static int fast_wait_first_rtcp_timeout = 80;


typedef struct ContextItem
{
    int Fd;
    //-1 stop;0,init;1,soket setup
    //muticast:1,has join multicast;2,has left multicast
    //unicast:2,setup unicast stream socket;3,receive unicast stream;
    //signalling:4,have sent bye cmd;
    volatile int8_t Status;
    int LastSeqNum;
    int firstSeqNum;
    RTPPacket* bak_pkt;
    char stopReceive;
    uint32_t Cnt;
    //server
    uint32_t Ip;
    uint16_t Port;
    char StrIp[256];
    char StrPort[50];
    //local
    uint16_t LocalPort;
    URLContext *Uc;
}ContextItem;

typedef struct {
    time_t expiredTimestamp;
    uint32_t redirectedFccIp;
    uint16_t redirectedFccPort;
    char redirectedFccStrIp[100];
    char redirectedFccStrPort[20];
}FccServerInfo;
static FccServerInfo s_fccServerInfo;

typedef enum {
    FCC_NORMAL_CONNECTING = 0,
    FCC_FAST_CONNECTING,
    FCC_CONNECT_FINISH
}FccConnectState;

typedef struct RtpFccContext
{
    pthread_t RecvThread;
    //0,init;1,creat success;2,creat fail;3,during receive;4,sth wrong;5,process over;0xff quit receive loop.
    volatile int8_t ThreadStatus;
    struct itemlist Recvlist;
    struct item *CurItem;
    int FirstMulticastSeq;
    int LastSeqNum;
    //three socket context
    ContextItem Unicast;
    ContextItem Multicast;
    ContextItem Signalling;
    ContextItem *CurSock;
    unsigned int try_direct_read;
    char first_packet_get;
    char first_packet_read;
    char first_rtcp_response;
    int64_t first_rtcp_send_time;

    char url[MAX_URL_SIZE];
    int flags;
    int network_down;
    FccConnectState connectState;
    void* bandwidth_measure;
} RtpFccContext;

int judge_seq_discontinuity(int seq1, int seq2, int seq3);
int parse_rtp_ts_packet(RTPPacket* lpkt);

static inline time_t getMonotonicTime()
{
    struct timespec tc = {0};
    clock_gettime(CLOCK_MONOTONIC, &tc);

    return tc.tv_sec;
}

static FccConnectState initFccConnectState(const char* debug_str)
{
    if (s_fccServerInfo.redirectedFccPort != 0) {
        time_t curSec = getMonotonicTime();

        if (curSec < s_fccServerInfo.expiredTimestamp) {
            av_log(NULL, AV_LOG_INFO, "[%s:%d], %s FCC_FAST_CONNECTING\n", __FUNCTION__, __LINE__, debug_str);
            return FCC_FAST_CONNECTING;
        }
    }

    av_log(NULL, AV_LOG_INFO, "[%s:%d], %s FCC_NORMAL_CONNECTING\n", __FUNCTION__, __LINE__, debug_str);
    return FCC_NORMAL_CONNECTING;
}

static void resetFccServerInfo()
{
    s_fccServerInfo.expiredTimestamp = 0;
    s_fccServerInfo.redirectedFccPort = 0;
    s_fccServerInfo.redirectedFccIp = 0;
    s_fccServerInfo.redirectedFccStrIp[0] = 0;
    s_fccServerInfo.redirectedFccStrPort[0] = 0;
}

static int SetupUdpSocket(URLContext **puc,char *StrIp,char *StrPort,int Port,int LocalPort,int flags);
static int MakeNewRtcpPac(RtpFccContext *Rfc,uint8_t *BufPac,uint8_t Fmt,int Fmps);
static int fccNormalStart(RtpFccContext *s)
{
    //new signalling socket
    if (NULL != s->Signalling.Uc)
    {
        ffurl_close(s->Signalling.Uc);
        s->Signalling.Fd = -1;
        s->Signalling.Uc = NULL;
    }

    int ret = SetupUdpSocket(&s->Signalling.Uc, s->Signalling.StrIp, s->Signalling.StrPort, s->Signalling.Port,-1,0);
    if (ret < 0) {
        return ret;
    }

    s->Signalling.Fd = ffurl_get_file_handle(s->Signalling.Uc);
    av_log(NULL, AV_LOG_INFO, "[%s:%d],s->Signalling.Fd:%d\n", __FUNCTION__, __LINE__,s->Signalling.Fd);
    s->Signalling.LocalPort =ff_udp_get_local_port(s->Signalling.Uc);
    s->Unicast.LocalPort = s->Signalling.LocalPort-1;
    av_log(NULL, AV_LOG_INFO, "[%s:%d],s->Signalling.LocalPort:%d,s->Unicast.LocalPort:%d\n", __FUNCTION__,__LINE__,s->Signalling.LocalPort,s->Unicast.LocalPort);
    //
    s->Signalling.Uc->flags = AVIO_FLAG_READ_WRITE;
    s->Signalling.Status = 1;

    if (NULL != s->Unicast.Uc) {
        ffurl_close(s->Unicast.Uc);
        s->Unicast.Uc = NULL;
        s->Unicast.Fd = -1;
    }

    av_log(NULL, AV_LOG_INFO, "[%s:%d]create unicast socket!\n", __FUNCTION__, __LINE__);
    //setup the unicast socket to receive the unicast stream //unicast stream local socket
    s->Unicast.Port = 0;
    ret = SetupUdpSocket(&s->Unicast.Uc, "", "", 0, s->Unicast.LocalPort,1);
    if (0 == ret)
    {
        s->Unicast.Fd = ffurl_get_file_handle(s->Unicast.Uc);
        s->Unicast.Status = 1;
        av_log(NULL, AV_LOG_INFO, "[%s:%d],s->Unicast.Fd:%d,s->Status:%d\n", __FUNCTION__, __LINE__,s->Unicast.Fd,s->Unicast.Status);
    }
    else
    {
        av_log(NULL, AV_LOG_INFO, "[%s:%d],build unicast socekt fail\n", __FUNCTION__, __LINE__);
    }

    //send rtcp request
    uint8_t RtcpPac[40];
    uint32_t RtcpLen = 40;
    MakeNewRtcpPac(s,RtcpPac, 2,-1);
    av_log(NULL, AV_LOG_INFO, "[%s:%d]\n",__FUNCTION__,__LINE__);
    ret = ffurl_write(s->Signalling.Uc, RtcpPac, RtcpLen);
    av_log(NULL, AV_LOG_INFO, "[%s:%d],ret:%d\n",__FUNCTION__,__LINE__,ret);

    s->first_rtcp_send_time = av_gettime();
    s->first_rtcp_response = 0;

    return ret;
}

static int SendByeRtcp(RtpFccContext *Rfc,int LastSeq);
static void onFccFastStartFailure(RtpFccContext* s)
{
    resetFccServerInfo();

    SendByeRtcp(s, -1);


    s->Signalling.Status = 0;
    s->Unicast.Status = 0;
    s->Multicast.Status = 0;

    fccNormalStart(s);
}

/* -[SE] [REQ][IPTV-19][jungle.wang]:add fast channel switch module */

static int rtpfcc_close(URLContext *h);
static int init_def_settings()
{
    static int inited =0;
    if (inited>0)
        return 0;
    inited++;
    gd_report_error_enable = (int)am_getconfig_bool_def("media.player.gd_report.enable",0) ||
                             (int)am_getconfig_bool_def("media.player.cmcc_report.enable",0);
    if (am_getconfig_bool_def("media.player.gd_report.enable",0))
        get_data_timeout_error = 54000;
    else if (am_getconfig_bool_def("media.player.cmcc_report.enable",0))
        get_data_timeout_error = 10002;
    get_data_timeout = am_getconfig_int_def("media.player.read_report.timeout",30);
 //   rtp_queue_cnt = am_getconfig_int_def("media.player.rtpqueue",5);
    max_rtp_buf = am_getconfig_int_def("media.amplayer.rtp_max",10000);
    av_log(NULL, AV_LOG_ERROR, "udp config: gd_report enable:%d\n\n", gd_report_error_enable);
    av_log(NULL, AV_LOG_INFO, "get_data timeout error=%d get_data_timeout:%dS,max_rtp_buf:%d\n",get_data_timeout_error,get_data_timeout,max_rtp_buf);
    return 0;
}

int rtp_set_remote_url(URLContext *h, const char *uri)
{
    RTPContext *s = h->priv_data;
    char hostname[256];
    int port;

    char buf[1024];
    char path[1024];

    av_url_split(NULL, 0, NULL, 0, hostname, sizeof(hostname), &port,
                 path, sizeof(path), uri);

    ff_url_join(buf, sizeof(buf), "udp", NULL, hostname, port, "%s", path);
    ff_udp_set_remote_url(s->rtp_hd, buf);

    ff_url_join(buf, sizeof(buf), "udp", NULL, hostname, port + 1, "%s", path);
    ff_udp_set_remote_url(s->rtcp_hd, buf);
    return 0;
}


/**
 * add option to url of the form:
 * "http://host:port/path?option1=val1&option2=val2...
 */

static void url_add_option(char *buf, int buf_size, const char *fmt, ...)
{
    char buf1[1024];
    va_list ap;

    va_start(ap, fmt);
    if (strchr(buf, '?'))
        av_strlcat(buf, "&", buf_size);
    else
        av_strlcat(buf, "?", buf_size);
    vsnprintf(buf1, sizeof(buf1), fmt, ap);
    av_strlcat(buf, buf1, buf_size);
    va_end(ap);
}

static void build_udp_url(char *buf, int buf_size,
                          const char *hostname, int port,
                          int local_port, int ttl,
                          int max_packet_size, int connect,int setbufsize)
{
    ff_url_join(buf, buf_size, "udp", NULL, hostname, port, NULL);
    if (local_port >= 0)
        url_add_option(buf, buf_size, "localport=%d", local_port);
    if (ttl >= 0)
        url_add_option(buf, buf_size, "ttl=%d", ttl);
    if (max_packet_size >=0)
        url_add_option(buf, buf_size, "pkt_size=%d", max_packet_size);
    if (connect)
        url_add_option(buf, buf_size, "connect=1");
    if (setbufsize > 0)
    	 url_add_option(buf, buf_size, "buffer_size=655360");

    url_add_option(buf, buf_size, "fifo_size=0");
}

#define MAX_RTP_SEQ 65536
#define MAX_RTP_SEQ_SPAN 60000
static int seq_greater(int first,int second) {
	if (first == second) {
		return 0;
	}
	else if (abs(first-second) > MAX_RTP_SEQ_SPAN) {
		if (first < second)
			return 1;
		else
			return 0;
	}
	else if (first > second) {
		return 1;
	}
	else
		return 0;

}

static int seq_less(int first,int second) {
	if (first == second) {
		return 0;
	}
	else if (abs(first-second) > MAX_RTP_SEQ_SPAN) {
		if (first>second)
			return 1;
		else
			return 0;
	}
	else if (first < second) {
		return 1;
	}
	else
		return 0;

}

static int seq_greater_and_equal(int first,int second) {
	if (first == second)
		return 1;
	else
		return seq_greater(first,second);
}

static int seq_less_and_equal(int first,int second) {
	if (first == second)
		return 1;
	else
		return seq_less(first,second);
}

static int seq_subtraction(int first,int second) {
	if (first == second) {
		return 0;
	}
	else if(abs(first-second)>MAX_RTP_SEQ_SPAN){
		if (first < second)
			return first + MAX_RTP_SEQ - second;
		else
			return first - second - MAX_RTP_SEQ;
	}
	else {
		return first-second;
	}
}

static int rtp_free_packet(void * apkt)
{
    RTPPacket * lpkt = apkt;
    if (lpkt != NULL)
    {
        if (lpkt->buf != NULL)
            av_free(lpkt->buf);
        av_free(lpkt);
    }
    apkt = NULL;
    return 0;
}

static int inner_rtp_read(RTPContext *s, uint8_t *buf, int size,URLContext* h)
{
    struct sockaddr_storage from;
    socklen_t from_len;
    int len, n;
    int64_t starttime = ff_network_gettime();
    int64_t curtime;
    struct pollfd p[2] = {{s->rtp_fd, POLLIN, 0}, {s->rtcp_fd, POLLIN, 0}};

    bandwidth_measure_start_read(s->bandwidth_measure);
    for(;;) {
        if (url_interrupt_cb()) {
            bandwidth_measure_finish_read(s->bandwidth_measure,0);
            return AVERROR_EXIT;
        }

        /* build fdset to listen to RTP and RTCP packets */
        n = poll(p, 2, 100);
        if (n > 0) {
            /* first try RTCP */
            if (p[1].revents & POLLIN) {
                from_len = sizeof(from);
                len = recvfrom (s->rtcp_fd, buf, size, 0,
                                (struct sockaddr *)&from, &from_len);
                if (len < 0) {
                    if (ff_neterrno() == AVERROR(EAGAIN) ||
                        ff_neterrno() == AVERROR(EINTR))
                        continue;
                    bandwidth_measure_finish_read(s->bandwidth_measure,0);
                    return AVERROR(EIO);
                }
                break;
            }
            /* then RTP */
            if (p[0].revents & POLLIN) {
                starttime = 0;
                 s->report_flag = 0;
                from_len = sizeof(from);
                len = recvfrom (s->rtp_fd, buf, size, 0,
                                (struct sockaddr *)&from, &from_len);
                if (len < 0) {
                    if (ff_neterrno() == AVERROR(EAGAIN) ||
                        ff_neterrno() == AVERROR(EINTR))
                        continue;
                    bandwidth_measure_finish_read(s->bandwidth_measure,0);
                    return AVERROR(EIO);
                }
                break;
            }
        } else if (n < 0) {
            if (ff_neterrno() == AVERROR(EINTR))
                continue;
            bandwidth_measure_finish_read(s->bandwidth_measure,0);
            return AVERROR(EIO);
        }
        curtime = ff_network_gettime();
        if (starttime <= 0)
            starttime = curtime;
        if (gd_report_error_enable && curtime > starttime + get_data_timeout*1000*1000 && !s->report_flag) {
            s->report_flag = 1;
            ffmpeg_notify(h, PLAYER_EVENTS_ERROR, get_data_timeout_error, 0);
        }
    }

    if (len > 0) {
        bandwidth_measure_finish_read(s->bandwidth_measure, len);
    } else {
        bandwidth_measure_finish_read(s->bandwidth_measure, 0);
    }

    return len;
}

static int inner_rtp_read1(RTPContext *s, uint8_t *buf, int size)
{
    struct sockaddr_storage from;
    socklen_t from_len;
    int len, n;
    struct pollfd p[1] = {{s->rtp_fd, POLLIN, 0}};

    bandwidth_measure_start_read(s->bandwidth_measure);
    for(;;) {
        if (url_interrupt_cb()) {
            bandwidth_measure_finish_read(s->bandwidth_measure,0);
            return AVERROR_EXIT;
        }

        /* build fdset to listen to only RTP packets */
        n = poll(p, 1, 100);
        if (n > 0) {
            /* then RTP */
            if (p[0].revents & POLLIN) {
                from_len = sizeof(from);
                len = recvfrom (s->rtp_fd, buf, size, 0,
                                (struct sockaddr *)&from, &from_len);
                if (len < 0) {
                    if (ff_neterrno() == AVERROR(EAGAIN) ||
                        ff_neterrno() == AVERROR(EINTR))
                        continue;
                    bandwidth_measure_finish_read(s->bandwidth_measure,0);
                    return AVERROR(EIO);
                }                
                break;
            }
        } else if (n < 0) {
            if (ff_neterrno() == AVERROR(EINTR))
                continue;
            bandwidth_measure_finish_read(s->bandwidth_measure,0);
            return AVERROR(EIO);
        }
        else {
            bandwidth_measure_finish_read(s->bandwidth_measure,0);
            return AVERROR(EAGAIN);
        }
    }

    if (len > 0) {
        bandwidth_measure_finish_read(s->bandwidth_measure,len);
    } else  {
        bandwidth_measure_finish_read(s->bandwidth_measure,0);
    }
    return len;
}


static int rtp_enqueue_packet(struct itemlist *itemlist, RTPPacket * lpkt)
{
    RTPPacket *ltailpkt=NULL;
    struct item *newitem=NULL;
    RTPPacket *headpkt=NULL;
    int ret = 0;

    itemlist_peek_tail_data(itemlist, (unsigned long)&ltailpkt) ;

    if (NULL == ltailpkt || (ltailpkt != NULL &&seq_less(ltailpkt->seq,lpkt->seq) == 1))
    {
        // append to the tail
        if (NULL != ltailpkt && NULL != lpkt && 1 != (lpkt->seq-ltailpkt->seq & MAX_RTP_SEQ-1))
        {
            av_log(NULL, AV_LOG_INFO, "[%s:%d],tailSeq:%d,insertSeq:%d\n", __FUNCTION__, __LINE__,ltailpkt->seq,lpkt->seq);
        }
        ret= itemlist_add_tail_data(itemlist, (unsigned long)lpkt);
        if (ret != 0)
            rtp_free_packet(lpkt);
        return 0;
    }

    itemlist_peek_head_data(itemlist, (unsigned long)&headpkt);
    if (headpkt != NULL && seq_less(lpkt->seq, headpkt->seq)) {
        av_log(NULL, AV_LOG_INFO, "[%s:%d],headSeq:%d,insertSeq:%d\n", __FUNCTION__, __LINE__,headpkt->seq,lpkt->seq);
        newitem = item_alloc(itemlist->item_ext_buf_size);
        if (newitem == NULL)
        {
            av_log(NULL, AV_LOG_INFO, "[%s:%d]\n", __FUNCTION__, __LINE__);
            rtp_free_packet(lpkt);
            return -12;//noMEM
        }
        newitem->item_data = (unsigned long)lpkt;
        ITEM_LOCK(itemlist);
        list_add(&(newitem->list), &(itemlist->list));
        itemlist->item_count++;
        ITEM_UNLOCK(itemlist);
        return 0;
    }

    // insert to the queue
    struct item *item = NULL;
    struct item *nextItem = NULL;
    struct list_head *llist=NULL, *tmplist=NULL;
    RTPPacket *nextRtpPac = NULL;
    RTPPacket *llistpkt=NULL;
    int CntList = 0;
    char used = 0;

    newitem = item_alloc(itemlist->item_ext_buf_size);
    if (newitem == NULL)
    {
        av_log(NULL, AV_LOG_INFO, "[%s:%d],CntList:%d\n", __FUNCTION__, __LINE__,CntList);
        rtp_free_packet(lpkt);
        return -12;//noMEM
    }
    newitem->item_data = (unsigned long)lpkt;


    ITEM_LOCK(itemlist);
    list_for_each_prev_safe(llist, tmplist, &itemlist->list)
    {
        CntList++;
        item = list_entry(llist, struct item, list);
        llistpkt = (RTPPacket *)(item->item_data);
        if (lpkt->seq == llistpkt->seq)
        {
            av_log(NULL, AV_LOG_INFO, "[%s:%d]The Replication packet, seq=%d\n", __FUNCTION__, __LINE__,lpkt->seq);
            item_free(newitem);
            rtp_free_packet((void *)lpkt);
            lpkt=NULL;
            used = 1;
            break;
        }
        else if (seq_less(llistpkt->seq, lpkt->seq)==1)
        {
            // insert to front

            if (NULL != nextItem)
            {
                nextRtpPac = (RTPPacket *)nextItem->item_data;
                av_log(NULL, AV_LOG_INFO, "[%s:%d],middle insert pre:%d,insert:%d,next:%d, item_count:%d\n", __FUNCTION__, __LINE__,llistpkt->seq, lpkt->seq,nextRtpPac->seq, itemlist->item_count);
            } else {
                av_log(NULL, AV_LOG_INFO, "[%s:%d],middle pac,lpkt->seq:%d,llistpkt->seq:%d\n", __FUNCTION__, __LINE__,lpkt->seq,llistpkt->seq);
            }

            list_add(&(newitem->list), &(item->list));
            itemlist->item_count++;
            used = 1;
            break;
        }
        nextItem = item;
    }

    if (!used) {
        av_log(NULL, AV_LOG_INFO, "[%s:%d]\n", __FUNCTION__, __LINE__);
        if (!list_empty(&itemlist->list)) {
            item = list_entry(itemlist->list.next, struct item, list);
            headpkt = (RTPPacket *)(item->item_data);
            av_log(NULL, AV_LOG_INFO, "[%s:%d] insert failed, try check head again! head seq:%d, pkt seq:%d\n", __FUNCTION__, __LINE__, headpkt->seq, lpkt->seq);
            if (seq_subtraction(headpkt->seq, lpkt->seq) != 1) {
                item_free(newitem);
                rtp_free_packet(lpkt);
                goto exit;
            }
        }
        list_add(&(newitem->list), &(itemlist->list));
        itemlist->item_count++;
    }

exit:
    ITEM_UNLOCK(itemlist);

    return 0;
}

/*
FILE *g_dumpFile=NULL;
static void dump(char *lpkt_buf,int len) {
	if (lpkt_buf[0] & 0x20) {					// remove the padding data
		int padding = lpkt_buf[len - 1];
		if (len >= 12 + padding)
		    len -= padding;
	}

	if (len <= 12) {
		av_log(NULL, AV_LOG_ERROR, "[%s:%d]len<=12,len=%d\n",__FUNCTION__,__LINE__,len);
		return;
	}

	// output the playload data
	int offset = 12 ;
	uint8_t * lpoffset = lpkt_buf + 12;

	int ext = lpkt_buf[0] & 0x10;
	if (ext > 0) {
		if (len < offset + 4) {
			av_log(NULL, AV_LOG_ERROR, "[%s:%d]len < offset + 4\n",__FUNCTION__,__LINE__);
			return;
		}

		ext = (AV_RB16(lpoffset + 2) + 1) << 2;
		if (len < ext + offset) {
			av_log(NULL, AV_LOG_ERROR, "[%s:%d]len < ext + offset\n",__FUNCTION__,__LINE__);
			return;
		}
		offset+=ext ;
		lpoffset+=ext ;
	}

	if (g_dumpFile == NULL)
		g_dumpFile=fopen("/data/tmp/rtp1.ts","wb");

	if (g_dumpFile)
		fwrite(lpoffset,1,len - offset,g_dumpFile);

}
*/

static void *rtp_recv_task( void *_RTPContext)
{
    av_log(NULL, AV_LOG_INFO, "[%s:%d]rtp recv_buffer_task start running!!!\n", __FUNCTION__, __LINE__);
    RTPContext * s=(RTPContext *)_RTPContext;
    if (NULL == s)
    {
        av_log(NULL, AV_LOG_INFO, "[%s:%d]Null handle!!!\n", __FUNCTION__, __LINE__);
        goto rtp_thread_end;
    }
    RTPPacket * lpkt = NULL;
    int datalen=0 ;
    int payload_type=0;
    uint8_t * lpoffset=NULL;
    int offset=0;
    uint8_t * lpkt_buf=NULL;
    int len=0;
    int ext=0;
    int csrc = 0;
    int SleepTime = 0;

    while (s->brunning > 0)
    {
        if (url_interrupt_cb())
        {
            goto rtp_thread_end;
        }

       if (s->recvlist.item_count >= max_rtp_buf)
        {
            if (0 == SleepTime ||  10000 == SleepTime)
            {
                av_log(NULL, AV_LOG_INFO, "[%s:%d]two much rtp pac in buffer,s->recvlist.item_count:%d\n", __FUNCTION__,__LINE__,s->recvlist.item_count);
                SleepTime = 0;
            }

            amthreadpool_thread_usleep(1);
            SleepTime++;
            continue;
        }

        if (lpkt != NULL)
        {
            rtp_free_packet((void *)lpkt);
            lpkt=NULL;
        }

        // malloc the packet buffer
        lpkt = av_mallocz(sizeof(RTPPacket));
        if (NULL == lpkt)
        {
            goto rtp_thread_end;
        }
        lpkt->buf= av_malloc(RTPPROTO_RECVBUF_SIZE);
        if (NULL == lpkt->buf)
        {
            goto rtp_thread_end;
        }
        // recv data
        lpkt->len = inner_rtp_read1(s, lpkt->buf, RTPPROTO_RECVBUF_SIZE);
        if (lpkt->len <=12)
        {
            av_log(NULL, AV_LOG_INFO, "[%s:%d]receive wrong packet len=%d \n", __FUNCTION__, __LINE__,lpkt->len);
            amthreadpool_thread_usleep(10);
            continue;
        }
        // paser data and buffer the packat
        payload_type = lpkt->buf[1] & 0x7f;
        lpkt->seq = AV_RB16(lpkt->buf + 2);

        if (33 == payload_type)
        {
            // parse the rtp playload data
            lpkt_buf=lpkt->buf;
            len=lpkt->len;

            if (lpkt_buf[0] & 0x20)
            {
                // remove the padding data
                int padding = lpkt_buf[len - 1];
                if (len >= 12 + padding)
                {
                    len -= padding;
                }
            }

            if (len <= 12)
            {
                av_log(NULL, AV_LOG_ERROR, "[%s:%d]len<=12,len=%d\n",__FUNCTION__,__LINE__,len);
                continue;
            }

            // output the playload data
            offset = 12 ;
            lpoffset = lpkt_buf + 12;
            csrc = lpkt_buf[0] & 0x0f;
            ext = lpkt_buf[0] & 0x10;
            if (ext > 0)
            {
                offset += 4*csrc;
                lpoffset += 4*csrc;
                if (len < offset + 4)
                {
                    av_log(NULL, AV_LOG_ERROR, "[%s:%d]len < offset + 4\n",__FUNCTION__,__LINE__);
                    continue;
                }

                ext = (AV_RB16(lpoffset + 2) + 1) << 2;
                if (len < ext + offset)
                {
                    av_log(NULL, AV_LOG_ERROR, "[%s:%d]len < ext + offset\n",__FUNCTION__,__LINE__);
                    continue;
                }
                offset+=ext ;
                lpoffset+=ext ;
            }
            lpkt->valid_data_offset=offset;

            if (rtp_enqueue_packet(&(s->recvlist), lpkt)<0)
            {
                goto rtp_thread_end;
            }
        }
        else
        {
            av_log(NULL, AV_LOG_ERROR, "[%s:%d]unknow payload type = %d, seq=%d\n", __FUNCTION__, __LINE__, payload_type,lpkt->seq);
            continue;
        }

        lpkt = NULL;
    }

    rtp_thread_end:
    s->brunning =0;
    av_log(NULL, AV_LOG_ERROR, "[%s:%d]rtp recv_buffer_task end!!!\n", __FUNCTION__, __LINE__);
    return NULL;
}


/**
 * url syntax: rtp://host:port[?option=val...]
 * option: 'ttl=n'            : set the ttl value (for multicast only)
 *         'rtcpport=n'       : set the remote rtcp port to n
 *         'localrtpport=n'   : set the local rtp port to n
 *         'localrtcpport=n'  : set the local rtcp port to n
 *         'pkt_size=n'       : set max packet size
 *         'connect=0/1'      : do a connect() on the UDP socket
 * deprecated option:
 *         'localport=n'      : set the local port to n
 *
 * if rtcpport isn't set the rtcp port will be the rtp port + 1
 * if local rtp port isn't set any available port will be used for the local
 * rtp and rtcp ports
 * if the local rtcp port is not set it will be the local rtp port + 1
 */


static int rtp_open(URLContext *h, const char *uri, int flags)
{
    RTPContext *s;
    int rtp_port, rtcp_port,
    ttl, connect,
    local_rtp_port, local_rtcp_port, max_packet_size;
    char hostname[256];
    char buf[1024];
    char path[1024];
    const char *p;
    av_log(NULL, AV_LOG_INFO, "rtp_open %s\n", uri);
    s = av_mallocz(sizeof(RTPContext));
    if (!s)
        return AVERROR(ENOMEM);
    h->priv_data = s;
    init_def_settings();

    av_url_split(NULL, 0, NULL, 0, hostname, sizeof(hostname), &rtp_port,path, sizeof(path), uri);
    /* extract parameters */
    ttl = -1;
    rtcp_port = rtp_port+1;
    local_rtp_port = -1;
    local_rtcp_port = -1;
    max_packet_size = -1;
    connect = 0;

    p = strchr(uri, '?');
    if (p) {
        if (av_find_info_tag(buf, sizeof(buf), "ttl", p)) {
            ttl = strtol(buf, NULL, 10);
        }
        if (av_find_info_tag(buf, sizeof(buf), "rtcpport", p)) {
            rtcp_port = strtol(buf, NULL, 10);
        }
        if (av_find_info_tag(buf, sizeof(buf), "localport", p)) {
            local_rtp_port = strtol(buf, NULL, 10);
        }
        if (av_find_info_tag(buf, sizeof(buf), "localrtpport", p)) {
            local_rtp_port = strtol(buf, NULL, 10);
        }
        if (av_find_info_tag(buf, sizeof(buf), "localrtcpport", p)) {
            local_rtcp_port = strtol(buf, NULL, 10);
        }
        if (av_find_info_tag(buf, sizeof(buf), "pkt_size", p)) {
            max_packet_size = strtol(buf, NULL, 10);
        }
        if (av_find_info_tag(buf, sizeof(buf), "connect", p)) {
            connect = strtol(buf, NULL, 10);
        }/*
        if (av_find_info_tag(buf, sizeof(buf), "use_cache", p)) {
            s->use_cache = strtol(buf, NULL, 10);
        }  */
    }
    s->use_cache =(flags&AVIO_FLAG_CACHE);

    build_udp_url(buf, sizeof(buf),
    hostname, rtp_port, local_rtp_port, ttl, max_packet_size,connect,1);
    av_log(NULL, AV_LOG_INFO, "[%s:%d]Setup udp session:%s\n",__FUNCTION__,__LINE__,buf);
    if (ffurl_open(&s->rtp_hd, buf, flags) < 0)
        goto fail;
    /* just to ease handle access. XXX: need to suppress direct handle
    access */
    s->rtp_fd = ffurl_get_file_handle(s->rtp_hd);

    if (!s->use_cache) {
        if (local_rtp_port >= 0 && local_rtcp_port < 0)
        local_rtcp_port = ff_udp_get_local_port(s->rtp_hd) + 1;

        build_udp_url(buf, sizeof(buf),
        hostname, rtcp_port, local_rtcp_port, ttl, max_packet_size,connect,0);
        av_log(NULL, AV_LOG_INFO, "[%s:%d]Setup udp session:%s\n",__FUNCTION__,__LINE__,buf);
        if (ffurl_open(&s->rtcp_hd, buf, flags) < 0)
            goto fail;
        /* just to ease handle access. XXX: need to suppress direct handle
        access */
        s->rtcp_fd = ffurl_get_file_handle(s->rtcp_hd);
    }

    if (s->use_cache)
    {
        s->recvlist.max_items = max_rtp_buf;
        s->recvlist.item_ext_buf_size = 0;
        s->recvlist.muti_threads_access = 1;
        s->recvlist.reject_same_item_data = 0;
        itemlist_init(&s->recvlist) ;
        s->cur_item = NULL;
        s->brunning = 1;
        av_log(NULL, AV_LOG_INFO, "[%s:%d]use cache mode\n",__FUNCTION__,__LINE__);
        if (amthreadpool_pthread_create_name(&(s->recv_thread), NULL, rtp_recv_task, s,"ffmpeg_rtp"))
        {
            av_log(NULL, AV_LOG_ERROR, "[%s:%d]ffmpeg_pthread_create failed\n",__FUNCTION__,__LINE__);
            goto fail;
        }
    }
    h->max_packet_size = s->rtp_hd->max_packet_size;
    h->is_streamed = 1;
    h->is_slowmedia = 1;

    s->bandwidth_measure=bandwidth_measure_alloc(100,0);
    av_log(NULL, AV_LOG_INFO, "[%s:%d]s->bandwidth_measure:%p\n",__FUNCTION__,__LINE__,s->bandwidth_measure);

    return 0;

    fail:
    if (s->bandwidth_measure != NULL) {
        bandwidth_measure_free(s->bandwidth_measure);
        s->bandwidth_measure = NULL;
    }

    if (s->rtp_hd)
        ffurl_close(s->rtp_hd);
    if (s->rtcp_hd)
        ffurl_close(s->rtcp_hd);
    av_free(s);
    return AVERROR(EIO);
}

static int check_net_phy_conn_status(void)
{
    int nNetDownOrUp = am_getconfig_int_def("net.ethwifi.up",3);//0-eth&wifi both down, 1-eth up, 2-wifi up, 3-eth&wifi both up

    return nNetDownOrUp;
}

/*
FILE *g_dumpFile=NULL;
static void dumpFile(char *buf,int len) {
    if (g_dumpFile == NULL)
g_dumpFile=fopen("/data/tmp/rtp.ts","wb");

if (g_dumpFile)
    fwrite(buf,1,len,g_dumpFile);
}

*/
static int rtp_close(URLContext *h);
static int rtp_read(URLContext *h, uint8_t *buf, int size)
{
    RTPContext *s = h->priv_data;
    int64_t starttime = ff_network_gettime();
    int64_t curtime;

    // Handle Network Down
    if (check_net_phy_conn_status() == 0) {
        // Network down
        if (s->network_down == 0) {
            s->network_down = 1;
            av_log(NULL, NULL, "network down.\n");
        }
    } else if (check_net_phy_conn_status() > 0) {
        // Network up
        if (s->network_down == 1) {
            // reset rtp connection
            char *url = h->filename;
            int flags = h->flags | AVIO_FLAG_CACHE;
            rtp_close(h);
            rtp_open(h, url, flags);
            av_log(NULL, NULL, "network up.rtp protocal reset finish.\n");
        }
    }

    if (s->network_down == 1)
        return AVERROR(EAGAIN);

    if (s->use_cache) {
        RTPPacket *lpkt = NULL;
        //uint8_t * lpkt_buf=NULL;
        //int len=0;
        int readsize=0;
        int single_readsize=0;
        while (s->brunning > 0 && size>readsize) {
            if (url_interrupt_cb())
                return AVERROR(EIO);

            if (check_net_phy_conn_status() == 0)
                break;

            if (s->recvlist.item_count <= 5) {
                curtime = ff_network_gettime();
                if (starttime <= 0)
                    starttime = curtime;
                if (gd_report_error_enable && (curtime > starttime + get_data_timeout*1000*1000) && !s->report_flag) {
                    s->report_flag = 1;
                    ffmpeg_notify(h, PLAYER_EVENTS_ERROR, get_data_timeout_error, 0);
                }
                amthreadpool_thread_usleep(10);
                continue;
            }

            if (s->cur_item == NULL)
                s->cur_item = itemlist_get_head(&s->recvlist);

            if (s->cur_item  == NULL) {
                amthreadpool_thread_usleep(10);
                continue;
            }
            lpkt = s->cur_item->item_data;
            starttime = 0;
            s->report_flag = 0;

            single_readsize=min(lpkt->len-lpkt->valid_data_offset, size-readsize);
            memcpy(buf+readsize,lpkt->buf+lpkt->valid_data_offset,single_readsize);

            readsize+=single_readsize;
            lpkt->valid_data_offset+=single_readsize;
            if (lpkt->valid_data_offset >= lpkt->len) {
                if ((s->last_seq+1)%MAX_RTP_SEQ != lpkt->seq) {
                    av_log(NULL, AV_LOG_ERROR, "[%s:%d]discontinuity seq=%d, the right seq=%d\n",__FUNCTION__,__LINE__, lpkt->seq,(s->last_seq+1)%MAX_RTP_SEQ);
                }
                s->last_seq=lpkt->seq;
                // already read, no valid data clean it
                item_free(s->cur_item);
                s->cur_item = NULL;
                rtp_free_packet((void *)lpkt);
                lpkt=NULL;
            }
        }

        return readsize;
    } else {
        return inner_rtp_read(s,buf,size,h);
    }
}

static int rtp_write(URLContext *h, const uint8_t *buf, int size)
{
    RTPContext *s = h->priv_data;
    int ret;
    URLContext *hd;

    if (buf[1] >= RTCP_SR && buf[1] <= RTCP_APP) {
        /* RTCP payload type */
        hd = s->rtcp_hd;
    } else {
        /* RTP payload type */
        hd = s->rtp_hd;
    }

    ret = ffurl_write(hd, buf, size);
#if 0
    {
        struct timespec ts;
        ts.tv_sec = 0;
        ts.tv_nsec = 10 * 1000000;
        nanosleep(&ts, NULL);
    }
#endif
    return ret;
}

static int rtp_close(URLContext *h)
{
    RTPContext *s = h->priv_data;

    if (s->use_cache) {
        s->brunning = 0;
        amthreadpool_pthread_join(s->recv_thread, NULL);
        s->recv_thread = 0;
        if (s->cur_item) {
            rtp_free_packet(s->cur_item->item_data);
            s->cur_item->item_data = NULL;
            item_free(s->cur_item);
            s->cur_item = NULL;
        }
        itemlist_clean(&s->recvlist, rtp_free_packet);
    }

    if (s->rtp_hd)
        ffurl_close(s->rtp_hd);
    if (s->rtcp_hd)
        ffurl_close(s->rtcp_hd);

    if (s->bandwidth_measure)
        bandwidth_measure_free(s->bandwidth_measure);
    s->bandwidth_measure = NULL;
    av_free(s);
    return 0;
}

/**
 * Return the local rtp port used by the RTP connection
 * @param h media file context
 * @return the local port number
 */

int rtp_get_local_rtp_port(URLContext *h)
{
    RTPContext *s = h->priv_data;
    return ff_udp_get_local_port(s->rtp_hd);
}

/**
 * Return the local rtcp port used by the RTP connection
 * @param h media file context
 * @return the local port number
 */

int rtp_get_local_rtcp_port(URLContext *h)
{
    RTPContext *s = h->priv_data;
    return ff_udp_get_local_port(s->rtcp_hd);
}

static int rtp_get_file_handle(URLContext *h)
{
    RTPContext *s = h->priv_data;
    return s->rtp_fd;
}

int rtp_get_rtcp_file_handle(URLContext *h) {
    RTPContext *s = h->priv_data;
    return s->rtcp_fd;
}

// ---------------------------------------------------------------------
// rtpfec protocol


/*
#define seq_equal
#define seq_greater
#define seq_less

int seq_compare(int first,int second)
{
	if (first == second) {
		return seq_equal;
	}
	else if (abs(first-second)>MAX_RTP_SEQ_SPAN) {
		if (first<second)
			return seq_greater;
		else
			return seq_less;
	}
	else if (first<second) {
		return seq_less;
	}
	else
		return seq_greater;
}
*/
static int check_time_interrupt(long *old_msecond, int interval_ms)
{
    int ret = 0;     struct timeval  new_time;
    long new_time_mseconds;
    gettimeofday(&new_time, NULL);
    new_time_mseconds = (new_time.tv_usec / 1000 + new_time.tv_sec * 1000);
    if (new_time_mseconds > (*old_msecond + interval_ms)) {
        ret = 1;
        *old_msecond = new_time_mseconds;
    } else if (new_time_mseconds < *old_msecond) {
        *old_msecond = new_time_mseconds; /*update time only.*/
    }
    return ret;
}

static int rtp_fec_calcuate(RTPFECContext *s)
{
    int total_pkt;
    if (s->last_time == 0)
        s->last_time = av_gettime()/1000;
    if (check_time_interrupt(&s->last_time, 10000)) {

        total_pkt = s->total_num - s->total_num_last;
		av_log(NULL,NULL,"total_pkt=%d,total_num=%d,total_num_last=%d\n",total_pkt,s->total_num,
				s->total_num_last);
        if (total_pkt != 0) {
            s->pre_fec_ratio = 100*(s->pre_fec_lost - s->pre_fec_lost_last)/total_pkt;
            s->after_fec_ratio = 100*(s->after_fec_lost - s->after_fec_lost_last)/total_pkt;
            av_log(NULL,NULL,"pre_fec_ratio:%d, after_fec_ratio:%d\n", s->pre_fec_ratio, s->after_fec_ratio);
			av_log(NULL,NULL,"pre_fec_lost=%d,pre_fec_lost_last=%d,total_pkt=%d\n",s->pre_fec_lost,s->pre_fec_lost_last,total_pkt);
        }
        s->pre_fec_lost_last = s->pre_fec_lost;
        s->after_fec_lost_last = s->after_fec_lost;
		s->total_num_last = s->total_num;
    }
    return 0;
}


static int rtpfec_free_packet(void * apkt)
{
    RTPFECPacket * lpkt = apkt;
    if (lpkt != NULL) {
        if (lpkt->buf != NULL)
            av_free(lpkt->buf);
        if (lpkt->fec != NULL)
            av_free(lpkt->fec);
        av_free(lpkt);
    }
    apkt = NULL;
    return 0;
}

static int rtpfec_read_data(RTPFECContext * s, uint8_t *buf, int size)
{
    struct sockaddr_storage from;
    socklen_t from_len;
    int len, n;
    struct pollfd p[2] = {{s->rtp_fd, POLLIN, 0}, {s->fec_fd, POLLIN, 0}};

    bandwidth_measure_start_read(s->bandwidth_measure);
    for(;;) {
        if (url_interrupt_cb()) {
            bandwidth_measure_finish_read(s->bandwidth_measure,0);
            return AVERROR_EXIT;
        }

        /* build fdset to listen to RTP and fec packets */
        n = poll(p, 2, 100);
        if (n > 0) {
            /* first try FEC */
            if (p[1].revents & POLLIN) {
                from_len = sizeof(from);
                len = recvfrom (s->fec_fd, buf, size, 0,
                                (struct sockaddr *)&from, &from_len);
                if (len < 0) {
                    if (ff_neterrno() == AVERROR(EAGAIN) ||
                        ff_neterrno() == AVERROR(EINTR)) {
                        TRACE()
                        usleep(10);
                        continue;
                    }
                    bandwidth_measure_finish_read(s->bandwidth_measure,0);
                    return AVERROR(EIO);
                }
                break;
            }

            /* then RTP */
            if (p[0].revents & POLLIN) {
                from_len = sizeof(from);
                len = recvfrom (s->rtp_fd, buf, size, 0,
                                (struct sockaddr *)&from, &from_len);
                if (len < 0) {
                    if (ff_neterrno() == AVERROR(EAGAIN) ||
                        ff_neterrno() == AVERROR(EINTR)) {
                        TRACE()
                        usleep(10);
                        continue;
                    }
                    bandwidth_measure_finish_read(s->bandwidth_measure,0);
                    return AVERROR(EIO);
                }

                break;
            }
            TRACE()
        } else if (n < 0) {
            av_log(NULL, AV_LOG_INFO, "[%s:%d]network error n=%d\n", __FUNCTION__, __LINE__,n);
            if (ff_neterrno() == AVERROR(EINTR)) {
                usleep(10);
                continue;
            }
            bandwidth_measure_finish_read(s->bandwidth_measure,0);
            return AVERROR(EIO);
        }

        TRACE()
        usleep(10);
    }

    if (len > 0) {
        bandwidth_measure_finish_read(s->bandwidth_measure, len);
    } else {
        bandwidth_measure_finish_read(s->bandwidth_measure, 0);
    }

    return len;
}

static int rtpfec_enqueue_packet(struct itemlist *itemlist, RTPFECPacket * lpkt) {
    int ret=0;
    TRACE()
    RTPFECPacket *ltailpkt=NULL;
    itemlist_peek_tail_data(itemlist, (unsigned long)&ltailpkt) ;
    if (NULL == ltailpkt || (ltailpkt != NULL &&seq_less_and_equal(ltailpkt->seq,lpkt->seq) == 1)) {
        // append to the tail
        TRACE()
        ret=itemlist_add_tail_data(itemlist, (unsigned long)lpkt) ;
    }
    else{
        // insert to the queue
        struct item *item = NULL;
        struct item *newitem = NULL;
        struct list_head *llist=NULL, *tmplist=NULL;
        RTPFECPacket *llistpkt=NULL;

        TRACE()
        ITEM_LOCK(itemlist);
        if (itemlist->max_items > 0 && itemlist->max_items <= itemlist->item_count) {
            av_log(NULL, AV_LOG_INFO, "[%s:%d]before return\n", __FUNCTION__, __LINE__);
            ITEM_UNLOCK(itemlist);
            return -1;
        }

        list_for_each_safe(llist, tmplist, &itemlist->list) {
            item = list_entry(llist, struct item, list);
            llistpkt = (RTPFECPacket *)(item->item_data);
            if (seq_less(lpkt->seq,llistpkt->seq) == 1) {
                // insert to front
                newitem = item_alloc(itemlist->item_ext_buf_size);
                if (newitem == NULL) {
                    ITEM_UNLOCK(itemlist);
                    return -12;//noMEM
                }
                newitem->item_data = (unsigned long)lpkt;

                list_add_tail(&(newitem->list), &(item->list));
                itemlist->item_count++;
                break;
            }
        }
        ITEM_UNLOCK(itemlist);
    }
    TRACE()
    return ret;
}

static int rtpfec_enqueue_outpacket(RTPFECContext * s, RTPFECPacket * lpkt) {
	int try_cnt=1;
	int ret=itemlist_add_tail_data(&(s->outlist), (unsigned long)lpkt) ;
	while (ret<0) {		// keyinfo try 6
		if (url_interrupt_cb()) {
			rtpfec_free_packet(lpkt);
		    	return -1;
		}
		amthreadpool_thread_usleep(10);

		// retry
		ret=itemlist_add_tail_data(&(s->outlist), (unsigned long)lpkt) ;
		try_cnt++;
	}

	return ret;
}

static void do_decode_output(RTPFECContext * s) {
    // decode
    RTPFECPacket *lpkt=NULL;
    if (s->fec_handle == NULL || s->cur_fec == NULL) {
 	av_log(NULL, AV_LOG_INFO, "[%s:%d]fec_handle=%x cur_fec=%x\n", __FUNCTION__, __LINE__,s->fec_handle,s->cur_fec);	
    	return;
    }

    memset(rtp_data_array,0,sizeof(rtp_data_array));
    memset(rtp_packet,0,sizeof(rtp_packet));
    memset(fec_data_array,0,sizeof(fec_data_array));
    memset(fec_packet,0,sizeof(fec_packet));
    memset(lost_map,0,sizeof(lost_map));

 	int blose_packet=0;
    int has_packet=0;
    int index=0;
	int i=0;
	int rtp_valid_cnt=0;

    // put the fec packet of same group to decoder vector
    lpkt=NULL;
    while (itemlist_peek_head_data(&(s->feclist), (unsigned long)&lpkt) == 0 && lpkt != NULL &&
		s->cur_fec->rtp_begin_seq==lpkt->fec->rtp_begin_seq&&s->cur_fec->rtp_end_seq==lpkt->fec->rtp_end_seq) {
	itemlist_get_head_data(&(s->feclist), (unsigned long)&lpkt) ;
	if (lpkt != NULL && lpkt->fec->redund_idx < MAX_FEC_PACKET_NUM) {
		fec_packet[lpkt->fec->redund_idx]=lpkt;
		fec_data_array[lpkt->fec->redund_idx]=lpkt->fec->fec_data;
		lost_map[s->rtp_media_packet_sum+lpkt->fec->redund_idx]=1;
		//av_log(NULL, AV_LOG_INFO, "[%s:%d]put to fec decoder vector. idx=%d\n", __FUNCTION__, __LINE__,lpkt->fec->redund_idx);	
	}
	else if (lpkt!=NULL) {
		av_log(NULL, AV_LOG_INFO, "[%s:%d]fec out of boundary. idx=%d\n", __FUNCTION__, __LINE__,lpkt->fec->redund_idx);	
		rtpfec_free_packet((void *)lpkt);
	}
	lpkt=NULL;
    }
/*
	// lose fec, to direct output
	lpkt=NULL;
	for (i=0;i<s->cur_fec->redund_num;i++) {
		if (lost_map[s->rtp_media_packet_sum+i] == 0) {
			int direct_output_cnt=0;
			for (i=0;i<s->rtp_media_packet_sum;i++) {
				if (lost_map[i] == 1) {
					itemlist_add_tail_data(&(s->outlist), (unsigned long)(rtp_packet[i])) ;
					direct_output_cnt++;
				}
			}
			av_log(NULL, AV_LOG_INFO, "[%s:%d]lost the fec,directly output.output_cnt=%d\n", __FUNCTION__, __LINE__,direct_output_cnt);	
		goto QUIT_DECODE;

		}
	}
*/
    // put the rtp packet of same group to decoder vector
    lpkt=NULL;
    while (itemlist_peek_head_data(&s->recvlist, (unsigned long)&lpkt) == 0 && lpkt != NULL &&
    		seq_less_and_equal(s->cur_fec->rtp_begin_seq,lpkt->seq)==1&&seq_less_and_equal(lpkt->seq,s->cur_fec->rtp_end_seq)==1) {
	has_packet=1;
	itemlist_get_head_data(&s->recvlist, (unsigned long)&lpkt);
	//if (lpkt != NULL&&lpkt->seq<=s->cur_fec->rtp_end_seq-3) {
	if (lpkt != NULL) {
		index=seq_subtraction(lpkt->seq, s->cur_fec->rtp_begin_seq);
		if (0 <= index && index < s->rtp_media_packet_sum) {
			rtp_packet[index]=lpkt;
			rtp_data_array[index]=lpkt->buf;
			lost_map[index]=1;
			rtp_valid_cnt++;
			//av_log(NULL, AV_LOG_INFO, "[%s:%d]input rtp data idx=%d,seq=%d\n", __FUNCTION__, __LINE__,index,lpkt->seq);	
		}
		else{
			av_log(NULL, AV_LOG_INFO, "[%s:%d]rtp out of boundary. idx=%d\n", __FUNCTION__, __LINE__,index);	
			rtpfec_free_packet((void *)lpkt);
		}
	}/*
	else if (lpkt != NULL) {
		index=lpkt->seq - s->cur_fec->rtp_begin_seq;
		av_log(NULL, AV_LOG_INFO, "[%s:%d]to discard . idx=%d,seq=%d\n", __FUNCTION__, __LINE__,index,lpkt->seq);	
		rtpfec_free_packet((void *)lpkt);
	}*/
	lpkt=NULL;
    }
	//av_log(NULL, AV_LOG_INFO, "[%s:%d]rtp_media_sum=%d,rtp_valid_cnt=%d\n", __FUNCTION__, __LINE__,s->rtp_media_packet_sum,rtp_valid_cnt);	

	if ((s->rtp_media_packet_sum-rtp_valid_cnt)>s->cur_fec->redund_num) {
		int direct_output_cnt=0;
		for (i=0;i<s->rtp_media_packet_sum;i++) {
			if (lost_map[i] == 1) {
				rtpfec_enqueue_outpacket(s,rtp_packet[i]);
				direct_output_cnt++;
			}
		}
		av_log(NULL, AV_LOG_INFO, "[%s:%d]To lose too much,directly output.output_cnt=%d\n", __FUNCTION__, __LINE__,direct_output_cnt);	
		goto QUIT_DECODE;
	}

	if (has_packet) {

	// malloc the lose packet to the fec decoder vector
	lpkt=NULL;
	for (i=0;i<s->cur_fec->redund_num;i++) {
		if (lost_map[s->rtp_media_packet_sum+i] == 0) {
			lpkt = av_mallocz(sizeof(RTPFECPacket));
			if (lpkt == NULL) {
				av_log(NULL, AV_LOG_INFO, "[%s:%d]lpkt == NULL\n", __FUNCTION__, __LINE__);
				continue;
			}

			lpkt->buf= av_malloc(FEC_RECVBUF_SIZE);
			lpkt->len=s->cur_fec->fec_len;
			fec_packet[i]=lpkt;
			fec_data_array[i]=lpkt->buf;
			lpkt->fec=NULL;
			av_log(NULL, AV_LOG_INFO, "[%s:%d]lose fec packet,index=%d,lost_map index=%d\n", __FUNCTION__, __LINE__,i,s->rtp_media_packet_sum+i);
		}
	}

	// malloc the lose packet to the rtp decoder vector
	lpkt=NULL;
	for (i=0;i<s->rtp_media_packet_sum;i++) {
		if (lost_map[i] == 0) {
			blose_packet=1;
			lpkt = av_mallocz(sizeof(RTPFECPacket));
			if (lpkt == NULL) {
				av_log(NULL, AV_LOG_INFO, "[%s:%d]lpkt == NULL\n", __FUNCTION__, __LINE__);
				continue;
			}

			lpkt->buf= av_malloc(FEC_RECVBUF_SIZE);
			lpkt->len=s->cur_fec->rtp_len;
			lpkt->seq=(s->cur_fec->rtp_begin_seq+i)%MAX_RTP_SEQ;
			rtp_packet[i]=lpkt;
			rtp_data_array[i]=lpkt->buf;
			s->pre_fec_lost++;
			av_log(NULL, AV_LOG_INFO, "[%s:%d]lose rtp packet,lost_map index=%d,req=%d,pre_fec_lost=%d\n", __FUNCTION__, __LINE__,i,lpkt->seq, s->pre_fec_lost);
		}
		lpkt=NULL;
	}

	// decoder the packet
	if (blose_packet == 1 && s->fec_handle != NULL) {
		//av_log(NULL, AV_LOG_INFO, "[%s:%d]lose rtp packet, do decode i0=%d i1=%d,lostaddr=%x\n", __FUNCTION__, __LINE__,lost_map[0],lost_map[1],lost_map);
		int ret=fec_decode(s->fec_handle, rtp_data_array, fec_data_array, lost_map,s->cur_fec->rtp_len);
		if (ret != 0) {
			for (i=0;i<s->rtp_media_packet_sum;i++) {
				if (lost_map[i] == 1)
					rtpfec_enqueue_outpacket(s,rtp_packet[i]);
				else{
					if (rtp_packet[i] != NULL)
						rtpfec_free_packet((void *)(rtp_packet[i]));
					rtp_packet[i]=NULL;
					rtp_data_array[i]=NULL;
				}
			}
			av_log(NULL, AV_LOG_INFO, "[%s:%d]decode failed ret=%d, to output the valide data\n", __FUNCTION__, __LINE__,ret);
			s->after_fec_lost++;
			goto QUIT_DECODE;
		}
		else
			av_log(NULL, AV_LOG_INFO, "[%s:%d]decode success ret=%d\n", __FUNCTION__, __LINE__,ret);
	}

	// all output
	for (i=0;i<s->rtp_media_packet_sum;i++) {
		rtpfec_enqueue_outpacket(s,rtp_packet[i]);
	}

	//av_log(NULL, AV_LOG_INFO, "[%s:%d]output packet num=%d\n", __FUNCTION__, __LINE__,s->rtp_media_packet_sum);
    }

QUIT_DECODE:
    s->rtp_last_decode_seq=s->cur_fec->rtp_end_seq;
    for (int i=0;i<s->cur_fec->redund_num&&i<MAX_FEC_PACKET_NUM;i++) {
        if (fec_packet[i] != NULL)
            rtpfec_free_packet((void *)(fec_packet[i]));
    }
    s->cur_fec=NULL;
    //av_log(NULL, AV_LOG_INFO, "[%s:%d]reset_fecdata last_decode_seq=%d\n", __FUNCTION__, __LINE__,s->rtp_last_decode_seq);
}

static void rtpfec_output_packet(RTPFECContext * s) {
    RTPFECPacket *lheadpkt=NULL;
    RTPFECPacket *lpkt=NULL;

    if (s->bdecode) {
        TRACE()
        if (s->cur_fec == NULL) {
            // to check the fec array is full, to set the cur_fec
            itemlist_peek_head_data(&(s->feclist), (unsigned long)&lheadpkt) ;
            if (lheadpkt != NULL && s->feclist.item_count > lheadpkt->fec->redund_num) {
                int rtp_begin_seq=lheadpkt->fec->rtp_begin_seq;
                int rtp_end_seq=lheadpkt->fec->rtp_end_seq;
                s->cur_fec=lheadpkt->fec;

                if (s->fec_handle == NULL) {
                    s->rtp_media_packet_sum = seq_subtraction(rtp_end_seq,rtp_begin_seq)+1;
                    s->total_num +=  s->rtp_media_packet_sum;
                    av_log(NULL,NULL,"s->total_num=%d\n",s->total_num);

                    init_RS_fec();
                    s->fec_handle=RS_fec_new(s->rtp_media_packet_sum, s->cur_fec->redund_num);
                }

                av_log(NULL, AV_LOG_INFO, "[%s:%d]req=%d,rtp_sum=%d,redund_num=%d,begin=%d,end=%d,rtp_len=%d\n", __FUNCTION__, __LINE__,
                lheadpkt->seq,s->rtp_media_packet_sum,lheadpkt->fec->redund_num,rtp_begin_seq,rtp_end_seq,lheadpkt->fec->rtp_len);
            }
        }
        TRACE()
        if (s->cur_fec != NULL) {
            // output the forward packet directly
            lpkt=NULL;
            while (itemlist_peek_head_data(&(s->recvlist), (unsigned long)&lpkt) == 0 && lpkt != NULL && seq_less(lpkt->seq, s->cur_fec->rtp_begin_seq)) {
                itemlist_get_head_data(&s->recvlist, (unsigned long)&lpkt) ;
                if (lpkt != NULL)
                    rtpfec_enqueue_outpacket(s,lpkt);
                lpkt=NULL;
            }

            //int reset_fecdata=0;
            if (s->recvlist.item_count > s->rtp_media_packet_sum+EXTRA_BUFFER_PACKET_NUM) {
                // if the receive buffer enough packet , to do decode and output
                //av_log(NULL, AV_LOG_INFO, "[%s:%d]do_decode_output item count=%d,sum\n", __FUNCTION__, __LINE__,s->recvlist.item_count,s->rtp_media_packet_sum);
                int fec_enough = (int)(s->feclist.item_count >= s->cur_fec->redund_num);
                int rtp_enough = (int)(s->recvlist.item_count >= s->rtp_media_packet_sum);
                if (fec_enough && rtp_enough)
                    do_decode_output(s);
                //reset_fecdata=1;
            }
        }
        TRACE()
    }

    if (s->recvlist.item_count > FORCE_OUTPUT_PACKET_NUM_THRESHOLD && s->cur_fec == NULL) {
        av_log(NULL, AV_LOG_INFO, "[%s:%d]direct to output the packet item_count=%d num=%d\n", __FUNCTION__, __LINE__,s->recvlist.item_count,FORCE_OUTPUT_PACKET_NUM);
        // force to output
        lpkt=NULL;
        while (s->recvlist.item_count > 0) {
            itemlist_get_head_data(&s->recvlist, (unsigned long)&lpkt);
            if (lpkt != NULL)
                rtpfec_enqueue_outpacket(s,lpkt);
            lpkt=NULL;
        }
    }
}

static void rtpfec_reset_packet(RTPFECPacket *lpkt) {
    if (lpkt == NULL)
        return;

    lpkt->seq=0;
    lpkt->len=0;
    lpkt->payload_type=0;

    if (lpkt->buf != NULL)
        memset(lpkt->buf ,0,FEC_RECVBUF_SIZE);

    if (lpkt->fec != NULL) {
        lpkt->fec->rtp_begin_seq=0;
        lpkt->fec->rtp_end_seq=0;
        lpkt->fec->redund_num=0;
        lpkt->fec->redund_idx=0;
        lpkt->fec->fec_len=0;
        lpkt->fec->rtp_len=0;
        lpkt->fec->rsv=0;
        lpkt->fec->fec_data=NULL;
    }
}

static void *rtpfec_recv_task( void *_RTPFECContext)
{
    av_log(NULL, AV_LOG_INFO, "[%s:%d]recv_buffer_task start running!!!\n", __FUNCTION__, __LINE__);
    RTPFECContext * s=(RTPFECContext *)_RTPFECContext;
    if (NULL == s) {
        av_log(NULL, AV_LOG_INFO, "[%s:%d]Null handle!!!\n", __FUNCTION__, __LINE__);
        goto thread_end;
    }

    RTPFECPacket * lpkt = NULL;
    int datalen=0,ext ;
    uint8_t * lpoffset;
    int ret=0;
    int try_cnt=0;

    while (s->brunning > 0) {
        if (url_interrupt_cb()) {
            goto thread_end;
        }
        /*
        if (lpkt != NULL) {
        rtpfec_free_packet((void *)lpkt);
        lpkt=NULL;
        }
        */
        // malloc the packet buffer
        if (NULL == lpkt) {
            lpkt = av_mallocz(sizeof(RTPFECPacket));
            if (NULL == lpkt)
            goto thread_end;
        }
        else{
            lpkt->len=0;
            lpkt->payload_type=0;
            lpkt->seq=0;
        }
        if (NULL == lpkt->buf) {
            lpkt->buf= av_malloc(FEC_RECVBUF_SIZE);
            if (NULL == lpkt->buf)
                goto thread_end;
            memset(lpkt->buf, 0, FEC_RECVBUF_SIZE);
        }
        else{
            memset(lpkt->buf,0,FEC_RECVBUF_SIZE);
        }

        TRACE()

        // recv data
        lpkt->len = rtpfec_read_data(s, lpkt->buf, FEC_RECVBUF_SIZE);
        if (lpkt->len <=12) {
            av_log(NULL, AV_LOG_INFO, "[%s:%d]receive wrong packet len=%d \n", __FUNCTION__, __LINE__,lpkt->len);
            usleep(10);
            continue;
        }
        TRACE()

        // paser data and buffer the packat
        lpkt->payload_type = lpkt->buf[1] & 0x7f;
        lpkt->seq = AV_RB16(lpkt->buf + 2);
        if (lpkt->payload_type == FEC_PAYLOAD_TYPE) {			 // fec packet
            //av_log(NULL, AV_LOG_INFO, "[%s:%d]datalen=%d\n", __FUNCTION__, __LINE__,lpkt->len);
            // parse the fec header
            datalen = lpkt->len ;
            if (lpkt->buf[0] & 0x20) {			// remove the padding padding (P): 1 bit
                int padding = lpkt->buf[datalen - 1];
                if (datalen >= 12 + padding)
                    datalen -= padding;
                av_log(NULL, AV_LOG_INFO, "[%s:%d]padding=%d\n", __FUNCTION__, __LINE__,padding);
            }

            datalen-=12;						// The first twelve octets are present in every RTP packet
            lpoffset = lpkt->buf + 12;

            // RFC 3550 Section 5.3.1 RTP Header Extension handling
            ext = lpkt->buf[0] & 0x10;
            if (ext > 0) {
                TRACE()
                if (datalen<4) {
                    av_log(NULL, AV_LOG_ERROR, "[%s:%d]datalen<4\n", __FUNCTION__, __LINE__);
                    continue;
                }
                ext = (AV_RB16(lpoffset + 2) + 1) << 2;
                if (datalen < ext) {
                    av_log(NULL, AV_LOG_ERROR, "[%s:%d]ext = %d\n", __FUNCTION__, __LINE__, ext);
                    continue;
                }

                datalen-=ext ;
                lpoffset+=ext ;
                av_log(NULL, AV_LOG_INFO, "[%s:%d]ext=%d\n", __FUNCTION__, __LINE__,ext);
            }

            if (NULL == lpkt->fec) {
                lpkt->fec= av_mallocz(sizeof(FEC_DATA_STRUCT));
                if (NULL == lpkt->fec)
                    goto thread_end;
            }
            else
                memset(lpkt->fec,0,sizeof(FEC_DATA_STRUCT));

            lpkt->fec->rtp_begin_seq=AV_RB16(lpoffset);
            lpkt->fec->rtp_end_seq=AV_RB16(lpoffset+2);
            lpkt->fec->redund_num=*(lpoffset+4);
            lpkt->fec->redund_idx=*(lpoffset+5);
            lpkt->fec->fec_len=AV_RB16(lpoffset+6);
            lpkt->fec->rtp_len=AV_RB16(lpoffset+8);
            lpkt->fec->fec_data=lpoffset+12;
            rtp_fec_calcuate(s);

            av_log(NULL, AV_LOG_ERROR, "[%s:%d]seq=%d,rtp_begin_seq=%d,rtp_end_seq=%d,redund_num=%d,redund_idx=%d,rtp_len=%d\n", __FUNCTION__, __LINE__,
            lpkt->seq,lpkt->fec->rtp_begin_seq,lpkt->fec->rtp_end_seq,lpkt->fec->redund_num,lpkt->fec->redund_idx,lpkt->fec->rtp_len);

            try_cnt=1;
            ret=rtpfec_enqueue_packet(&(s->feclist), lpkt);
            while (ret < 0 && try_cnt <= 6) {		// keyinfo try 6
                if (url_interrupt_cb())
                    goto thread_end;
                amthreadpool_thread_usleep(10);

                // retry
                ret=rtpfec_enqueue_packet(&(s->feclist), lpkt);
                try_cnt++;
            }

            if (ret < 0) {
                av_log(NULL, AV_LOG_ERROR, "[%s:%d]feclist have no room. timeout\n", __FUNCTION__, __LINE__);
                continue;
            }
        }
        else if (lpkt->payload_type == 33) {		// mpegts packet
            //av_log(NULL, AV_LOG_ERROR, "[%s:%d]mpegts packet req = %d\n", __FUNCTION__, __LINE__, lpkt->seq);
            try_cnt=1;
            ret=rtpfec_enqueue_packet(&(s->recvlist), lpkt);
            while (ret < 0 && try_cnt <= 3) {		// try 3
                if (url_interrupt_cb())
                    goto thread_end;
                amthreadpool_thread_usleep(10);

                // retry
                ret=rtpfec_enqueue_packet(&(s->recvlist), lpkt);
                try_cnt++;
            }

            if (ret<0) {
                av_log(NULL, AV_LOG_ERROR, "[%s:%d]recvlist have no room. timeout\n", __FUNCTION__, __LINE__);
                continue;
            }
        }
        else{
            av_log(NULL, AV_LOG_ERROR, "[%s:%d]unknow payload type = %d, seq=%d\n", __FUNCTION__, __LINE__, lpkt->payload_type,lpkt->seq);
            continue;
        }

        TRACE()
        rtpfec_output_packet(s);
        TRACE()
        lpkt = NULL;
    }

    thread_end:
    s->brunning =0;
    rtpfec_free_packet((void *)lpkt);
    av_log(NULL, AV_LOG_ERROR, "[%s:%d]recv_buffer_task end!!!\n", __FUNCTION__, __LINE__);
    return NULL;
}

static int rtpfec_open(URLContext *h, const char *uri, int flags)
{
    RTPFECContext *s;
    int rtp_port=-1;
    int fec_port=-1;
    int connect=0;
    int ttl=-1;
    int local_rtp_port=-1;
    int max_packet_size=-1;
    char hostname[256]={0};
    char buf[1024]={0};
    char path[1024]={0};
    const char *p;

    s = av_mallocz(sizeof(RTPFECContext));
    av_log(NULL, AV_LOG_INFO, "[%s:%d]s= %x\n",__FUNCTION__,__LINE__,s);
    if (!s)
        return AVERROR(ENOMEM);
    h->priv_data = s;

    av_url_split(NULL, 0, NULL, 0, hostname, sizeof(hostname), &rtp_port,
                 path, sizeof(path), uri);

    p = strchr(uri, '?');
    if (p) {
        if (av_find_info_tag(buf, sizeof(buf), "ChannelFECPort", p)) {
            fec_port = strtol(buf, NULL, 10);
        }
        if (av_find_info_tag(buf, sizeof(buf), "pkt_size", p)) {
            max_packet_size = strtol(buf, NULL, 10);
        }
        if (av_find_info_tag(buf, sizeof(buf), "connect", p)) {
            connect = strtol(buf, NULL, 10);
        }
    }

    if (fec_port<0)
    	goto fail;

    build_udp_url(buf, sizeof(buf),
                  hostname, rtp_port, local_rtp_port, ttl, max_packet_size,
                  connect,1);
    av_log(NULL, AV_LOG_INFO, "[%s:%d]build rtp udp url %s\n",__FUNCTION__,__LINE__,buf);
    if (ffurl_open(&s->rtp_hd, buf, flags) < 0)
        goto fail;

    memset(buf,0,sizeof(buf));
    build_udp_url(buf, sizeof(buf),
                  hostname, fec_port, -1, ttl, max_packet_size,
                  connect,0);
    av_log(NULL, AV_LOG_INFO, "[%s:%d]build fec udp url %s\n",__FUNCTION__,__LINE__,buf);
    if (ffurl_open(&s->fec_hd, buf, flags) < 0)
        goto fail;

    /* just to ease handle access. XXX: need to suppress direct handle
       access */
    s->rtp_fd = ffurl_get_file_handle(s->rtp_hd);
    s->fec_fd=ffurl_get_file_handle(s->fec_hd);

    s->recvlist.max_items = 2000;
    s->recvlist.item_ext_buf_size = 0;   
    s->recvlist.muti_threads_access = 1;
    s->recvlist.reject_same_item_data = 0;  
    itemlist_init(&s->recvlist) ;

    s->outlist.max_items = 2000;
    s->outlist.item_ext_buf_size = 0;   
    s->outlist.muti_threads_access = 1;
    s->outlist.reject_same_item_data = 0;  
    itemlist_init(&s->outlist) ;

    s->feclist.max_items = 500;
    s->feclist.item_ext_buf_size = 0;   
    s->feclist.muti_threads_access = 1;
    s->feclist.reject_same_item_data = 0;  
    itemlist_init(&s->feclist) ;

	s->rtp_seq_discontinue=0;
	s->fec_seq_discontinue=0;
    s->cur_fec=NULL;
    s->bdecode=1;		// 0:test 1:decode
    s->brunning = 1;
    s->total_num = 0;
    s->pre_fec_ratio = 0;
    s->after_fec_ratio = 0;
    s->total_num_last = 0;
    s->pre_fec_lost = 0;
    s->after_fec_lost = 0;
    s->pre_fec_lost_last = 0;
    s->after_fec_lost_last = 0;
    s->last_time = 0;

    av_log(NULL, AV_LOG_INFO, "[%s:%d]s= %x,bdecode=%d,brunning=%d\n",__FUNCTION__,__LINE__,s,s->bdecode,s->brunning );
    if (amthreadpool_pthread_create(&(s->recv_thread), NULL, rtpfec_recv_task, s)) {
	av_log(NULL, AV_LOG_ERROR, "[%s:%d]ffmpeg_pthread_create failed\n",__FUNCTION__,__LINE__);
	goto fail;
    }

    h->max_packet_size = s->rtp_hd->max_packet_size;
    h->is_streamed = 1;
    s->bandwidth_measure=bandwidth_measure_alloc(100,0);
    return 0;

 fail:
    if (s->bandwidth_measure != NULL) {
        bandwidth_measure_free(s->bandwidth_measure);
        s->bandwidth_measure = NULL;
    }

    if (s->rtp_hd)
        ffurl_close(s->rtp_hd);
    if (s->fec_hd)
        ffurl_close(s->fec_hd);
    av_free(s);
    return AVERROR(EIO);
}

static int rtpfec_read(URLContext *h, uint8_t *buf, int size)
{
    RTPFECContext *s = h->priv_data;
    if (s == NULL)
        return AVERROR(EIO);

    RTPFECPacket *lpkt = NULL;
    uint8_t * lpkt_buf=NULL;
    int len=0;
    while (s->brunning > 0) {
        if (url_interrupt_cb())
            return AVERROR(EIO);

        if (itemlist_get_head_data(&s->outlist, (unsigned long)&lpkt) != 0 && lpkt == NULL) {
            usleep(30);
            continue;
        }

        lpkt_buf=lpkt->buf;
        len=lpkt->len;

        if (lpkt_buf[0] & 0x20) {					// remove the padding data
            int padding = lpkt_buf[len - 1];
            if (len >= 12 + padding)
                len -= padding;
        }

        if (len <= 12) {
            av_log(NULL, AV_LOG_ERROR, "[%s:%d]len<=12,len=%d\n",__FUNCTION__,__LINE__,len);
            goto read_continue;
        }

        // output the playload data
        int offset = 12 ;
        uint8_t * lpoffset = lpkt_buf + 12;

        int ext = lpkt_buf[0] & 0x10;
        if (ext > 0) {
            TRACE()
            if (len < offset + 4) {
                av_log(NULL, AV_LOG_ERROR, "[%s:%d]len < offset + 4\n",__FUNCTION__,__LINE__);
                goto read_continue;
            }

            ext = (AV_RB16(lpoffset + 2) + 1) << 2;
            if (len < ext + offset) {
                av_log(NULL, AV_LOG_ERROR, "[%s:%d]len < ext + offset\n",__FUNCTION__,__LINE__);
                goto read_continue;
            }
            offset+=ext ;
            lpoffset+=ext ;
        }

        memcpy(buf, lpoffset, len - offset) ;
        len -= offset ;
        break;

        read_continue:
        rtpfec_free_packet((void *)lpkt);
        lpkt = NULL;
        lpkt_buf=NULL;
        len=0;
    }

    rtpfec_free_packet((void *)lpkt);
    return len;
}

static int rtpfec_close(URLContext *h)
{
    RTPFECContext *s = h->priv_data;

    s->brunning = 0;
    amthreadpool_pthread_join(s->recv_thread, NULL);
    s->recv_thread = 0;

    av_log(NULL, AV_LOG_INFO, "[%s:%d]cur_fec=0x%x,media_packet_sum=%d\n",__FUNCTION__,__LINE__,s->cur_fec,s->rtp_media_packet_sum);

    s->cur_fec=NULL;
    av_log(NULL, AV_LOG_INFO, "[%s:%d]recvlist item_count=%d,max_items=%d\n",__FUNCTION__,__LINE__,s->recvlist.item_count,s->recvlist.max_items);
    itemlist_clean(&s->recvlist, rtpfec_free_packet);
    av_log(NULL, AV_LOG_INFO, "[%s:%d]outlist item_count=%d,max_items=%d\n",__FUNCTION__,__LINE__,s->recvlist.item_count,s->recvlist.max_items);
    itemlist_clean(&s->outlist, rtpfec_free_packet);
    av_log(NULL, AV_LOG_INFO, "[%s:%d]feclist item_count=%d,max_items=%d\n",__FUNCTION__,__LINE__,s->recvlist.item_count,s->recvlist.max_items);
    itemlist_clean(&s->feclist, rtpfec_free_packet);

    if (s->fec_handle == NULL)
    	RS_fec_free(s->fec_handle);
    s->fec_handle=NULL;

    ffurl_close(s->rtp_hd);
    ffurl_close(s->fec_hd);

    if (s->bandwidth_measure)
        bandwidth_measure_free(s->bandwidth_measure);
    s->bandwidth_measure = NULL;
    av_free(s);
    return 0;
}
static int rtpfec_get_info(URLContext *h, uint32_t  cmd, uint32_t flag, int64_t *info) {
    if (h == NULL) {
        return -1;
    }

    RTPFECContext *s = h->priv_data;
    SoftProbeInfo  *pfec_ratio;

    if (s == NULL)
        return -1;

    if (cmd == AVCMD_GET_FECRATIOINFO) {
        if (flag == 1) {
            pfec_ratio = (SoftProbeInfo *)info;
            pfec_ratio->pre_fec_ratio = s->pre_fec_ratio;
            pfec_ratio->after_fec_ratio = s->after_fec_ratio;
            av_log(NULL, AV_LOG_INFO, "pre_fec_ratio:%d,after_fec_ratio\n", pfec_ratio->pre_fec_ratio, pfec_ratio->after_fec_ratio);
        }

        return 0;
    } else if (cmd == AVCMD_GET_NETSTREAMINFO) {
        if (flag == 2) {//current streaming bitrate ==>ref download bitrate
            int mean_bps, fast_bps, avg_bps,ret = -1;
            ret = bandwidth_measure_get_bandwidth(s->bandwidth_measure,&fast_bps, &mean_bps, &avg_bps);
            *info = avg_bps;
        }
    }

    return -1;
}

static int rtp_get_info(URLContext *h, uint32_t  cmd, uint32_t flag, int64_t *info) {
    if (h == NULL) {
        return -1;
    }

    RTPContext *s = h->priv_data;

    if (s == NULL)
        return -1;

    if (cmd == AVCMD_GET_NETSTREAMINFO) {
        if (flag == 2) {//current streaming bitrate ==>ref download bitrate
            int mean_bps, fast_bps, avg_bps,ret = -1;
            ret = bandwidth_measure_get_bandwidth(s->bandwidth_measure,&fast_bps, &mean_bps, &avg_bps);
            *info = avg_bps;
        }

        return 0;
    }

    return -1;
}
/* +[SE] [REQ][IPTV-19][jungle.wang]:add fast channel switch module */
static int RtpFccReadOnePac(RtpFccContext * s, uint8_t *buf, int size)
{
 //   av_log(NULL, AV_LOG_INFO, "[%s:%d],enter\n", __FUNCTION__, __LINE__);
    struct sockaddr_storage from;
    socklen_t from_len;
    int len, n;
    int CurFd = -1;
    len = n = 0;
    s->CurSock = NULL;
    struct pollfd p[3] = {{s->Unicast.Fd, POLLIN, 0}, {s->Multicast.Fd, POLLIN, 0},  {s->Signalling.Fd, POLLIN, 0}};
    if (s->Unicast.stopReceive) {
        p[0].fd = -1;
        s->try_direct_read &= ~(1<<0);
    }

    int cnt = 0;
    while (s->try_direct_read) {
        if (!(s->try_direct_read & 1<<cnt)) {
            ++cnt;
            continue;
        }

        switch (cnt) {
        case 0:
            s->CurSock = &s->Unicast;
            break;
        case 1:
            s->CurSock = &s->Multicast;
            break;
        case 2:
            s->CurSock = &s->Signalling;
            break;
        default:
            av_log(NULL, AV_LOG_ERROR, "[%s:%d] error try_direct_read:%x\n", __FUNCTION__, __LINE__, s->try_direct_read);
            s->try_direct_read = 0;
            goto try_poll;
        }

        from_len = sizeof(from);
        len = recvfrom (s->CurSock->Fd, buf, size, MSG_DONTWAIT, (struct sockaddr *)&from, &from_len);
        if (len < 0)
        {
            //av_log(NULL, AV_LOG_INFO, "direct read len:%d, try_direct_read:%#x, errno:%d\n", len, s->try_direct_read, errno);
            if (ff_neterrno() == AVERROR(EAGAIN) || ff_neterrno() == AVERROR(EWOULDBLOCK)) {
                s->try_direct_read &= ~(1<<cnt);
                ++cnt;
                continue;
            }
        }

        return len;
    }

try_poll:
    while (1)
    {
        if (url_interrupt_cb())
        {
            int ValueRet = AVERROR_EXIT;
            av_log(NULL, AV_LOG_INFO, "[%s:%d],ValueRet:%d\n", __FUNCTION__, __LINE__,ValueRet);
            return ValueRet;
        }
        if (s->ThreadStatus < 3) {
            av_log(NULL,NULL, "fcc close quit read\n");
            return AVERROR_EXIT;
        }
        /* build fdset to listen to RTP、 fcc packets */
        n = poll(p, 3, 10);
        if (n > 0)
        {
            int i;
            for (i = 0; i < 3; ++i) {
                if (p[i].revents & POLLIN) {
                    s->try_direct_read |= 1 << i;
                }
            }

            CurFd = -1;
            /* first try unicast */
            if (p[0].revents & POLLIN)
            {
                s->CurSock  = &s->Unicast;
                goto RecvOnePac;
            }

            /* then muticast media */
            else if (p[1].revents & POLLIN)
            {
                s->CurSock = &s->Multicast;
                goto RecvOnePac;

            }
            /* then muticast signalling */
            else if (p[2].revents & POLLIN)
            {
                av_log(NULL, AV_LOG_INFO, "[%s:%d]\n", __FUNCTION__, __LINE__);
                s->CurSock = &s->Signalling;
                goto RecvOnePac;
            }
            else
            {
                continue;
            }

RecvOnePac:

            from_len = sizeof(from);
            len = recvfrom (s->CurSock->Fd, buf, size, 0, (struct sockaddr *)&from, &from_len);
            if (len < 0)
            {
                av_log(NULL, AV_LOG_INFO, "[%s:%d],len :%d\n", __FUNCTION__, __LINE__,len);
                if (ff_neterrno() == AVERROR(EAGAIN) || ff_neterrno() == AVERROR(EINTR))
                {
                    TRACE()
                    usleep(10);
                    continue;
                }
                return AVERROR(EIO);
            }

            break;

        }
        else if (n < 0)
        {
            av_log(NULL, AV_LOG_INFO, "[%s:%d]network error n=%d, errno:%d\n", __FUNCTION__, __LINE__,n, errno);
            if (ff_neterrno() == AVERROR(EINTR))
            {
                usleep(10);
                continue;
            }
            return AVERROR(EIO);
        } else {
            //av_log(NULL, AV_LOG_INFO, "[%s:%d]poll timeout, break!\n", __FUNCTION__, __LINE__);
            s->CurSock = NULL;
            break;
        }
    }

    return len;
}

static int SetupUdpSocket(URLContext **puc,char *StrIp,char *StrPort,int Port,int LocalPort,int flags)
{
    char buf[1024]={0};
    build_udp_url(buf, sizeof(buf), StrIp, Port, LocalPort, -1, -1, 0,0);
    av_log(NULL, AV_LOG_INFO, "[%s:%d]build udp url %s\n",__FUNCTION__,__LINE__,buf);
    //signalling rtcp
    if (ffurl_open(puc, buf, flags) < 0)
    {
       av_log(NULL, AV_LOG_INFO, "[%s:%d]build udp url fail\n",__FUNCTION__,__LINE__);
       return -1;
    }

    return 0;
}
//
static int MakeNewRtcpPac(RtpFccContext *Rfc,uint8_t *BufPac,uint8_t Fmt,int Fmps)
{
    if (NULL == Rfc)
    {
        av_log(NULL, AV_LOG_INFO, "[%s:%d]，NULL == Rfc\n", __FUNCTION__, __LINE__);
    }
    uint16_t LenPac = 0;
    uint32_t MulticastIp = Rfc->Multicast.Ip;
    uint16_t UnicastPort = Rfc->Unicast.LocalPort;
    uint16_t MulticastPort = Rfc->Multicast.Port;
    uint8_t  StbId[16];
    uint8_t  LoopCnt = 0;
    //请求消息
    if (2 == Fmt)
    {
        LenPac      =   9;

        BufPac[12]  =   0;
        BufPac[13]  =   0xff;
        BufPac[14]  =   0xff;
        BufPac[15]  =   0xff;

        BufPac[16]  =   (UnicastPort&0xff00)>>8;
        BufPac[17]  =   UnicastPort&0xff;
        BufPac[18]  =   (MulticastPort&0xff00)>>8;
        BufPac[19]  =   MulticastPort&0xff;

        BufPac[20]  =   (MulticastIp&0xff000000)>>24;
        BufPac[21]  =   (MulticastIp&0x00ff0000)>>16;
        BufPac[22]  =   (MulticastIp&0x0000ff00)>>8;
        BufPac[23]  =   MulticastIp&0xff;


        //2 get serial code
        char Value[100];
        if (property_get("ro.serialno",Value,NULL) > 0)
        {
            for (LoopCnt = 0;LoopCnt  < 16;LoopCnt++)
            {
                BufPac[24+LoopCnt] = Value[2*LoopCnt]<<4;
                BufPac[24+LoopCnt] += Value[2*LoopCnt+1];
                BufPac[24+LoopCnt] -= 0x30;
                if (10 == LoopCnt || 15 == LoopCnt)
                {
                    BufPac[24+LoopCnt] += 0x90;
                }
            }
        }
    }
    //结束消息
    else if (5 == Fmt)
    {
        LenPac      =   3;

        BufPac[13]  =   0;
        if (Fmps >= 0) {
            BufPac[12]  =   0;
            BufPac[14]  =   (Fmps&0xff00)>>8;
            BufPac[15]  =   Fmps&0xff;
        } else {
            BufPac[12]  =   1;
            BufPac[14]  =   0;
            BufPac[15]  =   0;
        }
    }
    //not supported
    else
    {
        return -1;
    }
    //
    BufPac[0]   =   Fmt;
    BufPac[0]   |=  0x80;
    BufPac[1]   =   0xcd;
    BufPac[2]   =   (LenPac&0xff00)>>8;
    BufPac[3]   =   LenPac&0xff;
    BufPac[4]   =   0;
    BufPac[5]   =   0;
    BufPac[6]   =   0;
    BufPac[7]   =   0;
    BufPac[8]   =   (MulticastIp&0xff000000)>>24;
    BufPac[9]   =   (MulticastIp&0x00ff0000)>>16;
    BufPac[10]  =   (MulticastIp&0x0000ff00)>>8;
    BufPac[11]  =   MulticastIp&0xff;

    return 0;
}
//leave unicast
static int SendByeRtcp(RtpFccContext *Rfc,int LastSeq)
{
    if (Rfc->Signalling.Status < 4)
    {
        uint8_t RtcpPac[16];
        uint32_t RtcpLen = 16;

        if (-1 == LastSeq)
        {
            av_log(NULL, AV_LOG_INFO, "[%s,%d],make bye cmd, stop fcc service!\n", __FUNCTION__,__LINE__);
        }
        else
        {
            av_log(NULL, AV_LOG_INFO, "[%s,%d],make bye cmd ,LastSeq:%d\n", __FUNCTION__,__LINE__,LastSeq);
        }
        MakeNewRtcpPac(Rfc,RtcpPac, 5, LastSeq);

        Rfc->Signalling.Uc->flags = AVIO_SEND_CMD_BEFORE_QUIT;
        int ret = Rfc->Signalling.Uc->prot->url_write(Rfc->Signalling.Uc,RtcpPac, RtcpLen);
        av_log(NULL, AV_LOG_INFO, "[%s,%d],send bye cmd ,ret:%d,s->Signalling.Status:%d \n", __FUNCTION__,__LINE__,ret,Rfc->Signalling.Status);
        Rfc->Signalling.Status = 4;
        return ret;
    }
    else
    {
        av_log(NULL,AV_LOG_INFO,"[%s,%d],the bye cmd has already been sent,Rfc->Signalling.Status:%d, Seq:%d\n",__FUNCTION__,__LINE__,Rfc->Signalling.Status, LastSeq);
        return -1;
    }
}
//join multicast
static int JoinMulticast(RtpFccContext *Rfc)
{
    //setup the multicast socket to receive the multicast stream
    URLContext* ptmpMultUc = NULL;
    int ret = SetupUdpSocket(&ptmpMultUc, Rfc->Multicast.StrIp, Rfc->Multicast.StrPort, Rfc->Multicast.Port,-1,1);
    if (0 == ret)
    {
        Rfc->Multicast.Fd = ffurl_get_file_handle(ptmpMultUc);
        Rfc->Multicast.Status = 1;
        Rfc->Multicast.Uc = ptmpMultUc;
        av_log(NULL, AV_LOG_INFO, "[%s:%d],Rfc->Multicast.Fd:%d,Rfc->MultiCastStatus:%d\n", __FUNCTION__,__LINE__,Rfc->Multicast.Fd,Rfc->Multicast.Status);
    }
    else
    {
        av_log(NULL, AV_LOG_INFO, "[%s:%d],build Multicast socekt fail\n", __FUNCTION__, __LINE__);
    }
    return ret;
}
static unsigned int IpToInt(const char* pszIP, int b_hton)
{
    int b_first = 1;
    unsigned int ui_ip = 0;
    unsigned char ucTmp = 0;
    char* psz_token = NULL;
    char szBuf[100] = {'\0'};

    do {
        if ((NULL == pszIP)  && (strlen(pszIP) >= sizeof(szBuf)))
        {
            break;
        }

        strcpy(szBuf, pszIP); // attention!

        psz_token = strtok(szBuf, ".");
        while (NULL != psz_token)
        {
            if (b_first)
            {
                b_first = 0;
            }
            else
            {
                ui_ip <<= 8;
            }

            ucTmp = (unsigned char)atoi(psz_token);
            ui_ip |= ucTmp;

            psz_token = strtok(NULL, ".");
        }
    } while (0);

    if (b_hton)
    {
        ui_ip = htonl(ui_ip);
    }

    return ui_ip;
}
//
static unsigned int IntToIp(char StrIp[], int LenStr,int IntIp)
{
    av_log(NULL, AV_LOG_INFO, "[%s:%d]sizeof(StrIp):%d\n", __FUNCTION__, __LINE__,sizeof(StrIp));

    char Ip[10] = {0};
    uint8_t *p = (uint8_t *)&IntIp;

    snprintf(Ip, 10, "%d",p[3]&0xff);
    strcpy(StrIp,Ip);
    av_strlcat(StrIp,".",LenStr);
    snprintf(Ip, 10, "%d",p[2]&0xff);
    av_strlcat(StrIp,Ip,LenStr);
    av_strlcat(StrIp,".",LenStr);
    snprintf(Ip, 10, "%d",p[1]&0xff);
    av_strlcat(StrIp,Ip,LenStr);
    av_strlcat(StrIp,".",LenStr);
    snprintf(Ip, 10, "%d",p[0]&0xff);
    av_strlcat(StrIp,Ip,LenStr);

    return 0;
}

static int ParseOneRtcpPacket(RtpFccContext *Rfc,uint8_t *Buf)
{
    if (NULL == Buf)
    {
        return -1;
    }

    uint8_t Version =   (Buf[0]&0xc0)>>6;

    if (2 != Version)
    {
        av_log(NULL, AV_LOG_INFO, "[%s:%d]invalid rtcp version number:%d!!!\n", __FUNCTION__, __LINE__,Version);
        return -1;
    }

    uint8_t Padding =   (Buf[0]&0x20)>>5;

    if (0 != Padding)
    {
        av_log(NULL, AV_LOG_INFO, "[%s:%d]invalid rtcp padding:%d!!!\n", __FUNCTION__, __LINE__,Padding);
        return -1;
    }


    uint8_t Fmt =   Buf[0]&0x1f;
    av_log(NULL, AV_LOG_INFO, "[%s:%d]receive rtcp respones fmt type:%d!!!\n", __FUNCTION__, __LINE__,Fmt);
    Rfc->first_rtcp_response = 1;

    //请求响应信息
    if (3 == Fmt)
    {
        uint8_t Result  = Buf[12];
        uint8_t Type    = Buf[13];
        if (0 != Result)
        {
            //TODO: how to avoid duplicated error packet?
            av_log(NULL, AV_LOG_INFO, "[%s:%d]the fcc server not process correctly,Result:%d,Type:%d\n",__FUNCTION__, __LINE__,Result,Type);
            av_log(NULL, AV_LOG_INFO, "[%s:%d]connectState:%d,Multicast.Status:%d,Signalling.Status:%d\n",__FUNCTION__, __LINE__,
                Rfc->connectState, Rfc->Multicast.Status, Rfc->Signalling.Status);
            if (Rfc->connectState == FCC_FAST_CONNECTING) {
                onFccFastStartFailure(Rfc);
                Rfc->connectState = FCC_NORMAL_CONNECTING;
                return 0;
            } else if (Rfc->connectState == FCC_NORMAL_CONNECTING && Rfc->Multicast.Status < 1 && Rfc->Signalling.Status > 0) {
                JoinMulticast(Rfc);
                SendByeRtcp(Rfc, -1);
            } else {
                av_log(NULL, AV_LOG_INFO, "[%s:%d]can't reach here!\n", __FUNCTION__, __LINE__);
            }

            return -1;
        }

        if (1 == Type)
        {
            av_log(NULL, AV_LOG_INFO, "[%s:%d]no need to obtain unicast,igmp join immediatly!!!\n", __FUNCTION__, __LINE__);
            Rfc->connectState = FCC_NORMAL_CONNECTING;
            /* +[SE] [BUG][IPTV-4070][jipeng.zhao] if server not support, use multicast*/
            JoinMulticast(Rfc);
            SendByeRtcp(Rfc, -1);
            return 0;
        }
        uint16_t FccSignalPort  = AV_RB16(Buf+14);
        uint16_t FccMediaPort   = AV_RB16(Buf+16);
        uint32_t FccIpAddress   = AV_RB32(Buf+20);
        uint32_t FccValidTime = AV_RB32(Buf+24);
        //
        char StrFccIp[100] = {0};
        char StrFccPort[50] = {0};
        int ret = -1;
        //
        IntToIp(StrFccIp, 100,FccIpAddress);
        av_log(NULL, AV_LOG_INFO, "[%s:%d]StrFccIp:%s!!\n", __FUNCTION__, __LINE__,StrFccIp);
        snprintf(StrFccPort,sizeof(StrFccPort),"%d",FccSignalPort);
        //unicast media socket
        char hostname[100] = {0};
        char strport[50] = {0};
        //
        if (2 == Type && Rfc->Unicast.Status < 3)
        {
            av_log(NULL, AV_LOG_INFO, "[%s:%d]the normal type to obatin unicast stream!!!, unicast fd:%d\n", __FUNCTION__, __LINE__, Rfc->Unicast.Fd);

            if (Rfc->Unicast.Uc == NULL) {
                av_log(NULL, AV_LOG_INFO, "[%s:%d]FATAL, try rereate unicast socket!\n", __FUNCTION__, __LINE__);
                //setup the unicast socket to receive the unicast stream //unicast stream local socket
                Rfc->Unicast.Port = FccMediaPort;
                ret = SetupUdpSocket(&Rfc->Unicast.Uc, hostname, strport, FccMediaPort,Rfc->Unicast.LocalPort,1);
                if (0 == ret)
                {
                    Rfc->Unicast.Fd = ffurl_get_file_handle(Rfc->Unicast.Uc);
                    Rfc->Unicast.Status = 2;
                    av_log(NULL, AV_LOG_INFO, "[%s:%d],Rfc->Unicast.Fd:%d,Rfc->Status:%d\n", __FUNCTION__, __LINE__,Rfc->Unicast.Fd,Rfc->Unicast.Status);
                }
                else
                {
                    av_log(NULL, AV_LOG_INFO, "[%s:%d],build unicast socekt fail\n", __FUNCTION__, __LINE__);
                }
            }

//we should send stop unicast msg to server rtcp port give by type 3 msg
//notice msg that join mulicast will send from this server rtcp port
//our udp socket is connectless, so we can receive packet from this new rtcp port
#if 0
            if (0 != FccSignalPort)
            {
                av_log(NULL, AV_LOG_INFO, "[%s:%d],FccSignalPort:%d,Rfc->Signalling.Port:%d,make new signalling socket !!!\n", __FUNCTION__,__LINE__,FccSignalPort,Rfc->Signalling.Port);
                //new signalling socket
                if (NULL != Rfc->Signalling.Uc)
                {
                    ffurl_close(Rfc->Signalling.Uc);
//                    s->Signalling.Uc = NULL;
                }
                Rfc->Signalling.Port = FccSignalPort;
                strcpy(Rfc->Signalling.StrPort,StrFccPort);
                //
                av_log(NULL, AV_LOG_INFO, "[%s:%d],Rfc->Signalling.LocalPort:%dn", __FUNCTION__,__LINE__,Rfc->Signalling.LocalPort);
                SetupUdpSocket(&Rfc->Signalling.Uc, Rfc->Signalling.StrIp, StrFccPort, FccSignalPort,Rfc->Signalling.LocalPort ,0);
                av_log(NULL, AV_LOG_INFO, "[%s:%d],Rfc->Signalling.LocalPort:%dn", __FUNCTION__,__LINE__,Rfc->Signalling.LocalPort);
                Rfc->Signalling.Fd = ffurl_get_file_handle(Rfc->Signalling.Uc);
                Rfc->Signalling.Uc->flags = AVIO_FLAG_READ_WRITE;
                av_log(NULL, AV_LOG_INFO, "[%s:%d],Rfc->Signalling.Fd:%d\n", __FUNCTION__, __LINE__,Rfc->Signalling.Fd);
                Rfc->Signalling.LocalPort =ff_udp_get_local_port(Rfc->Signalling.Uc);
                av_log(NULL, AV_LOG_INFO, "[%s:%d],Rfc->Signalling.LocalPort:%d,Rfc->LocalUnicastStreamPort:%d\n", __FUNCTION__,__LINE__,Rfc->Signalling.LocalPort,Rfc->Unicast.LocalPort);
            }
#endif

            Rfc->Unicast.Status = 3;
            Rfc->connectState = FCC_CONNECT_FINISH;
        }
        else if (3 == Type && Rfc->Unicast.Status < 2)
        {
            av_log(NULL, AV_LOG_INFO, "[%s:%d]the fcc server is redirected!!!!\n", __FUNCTION__, __LINE__);
            //new signalling socket
            if (NULL != Rfc->Signalling.Uc)
            {
                ffurl_close(Rfc->Signalling.Uc);
            //        s->Signalling.Uc = NULL;
            }
            Rfc->Signalling.Port = FccSignalPort;
            strcpy(Rfc->Signalling.StrIp,StrFccIp);
            strcpy(Rfc->Signalling.StrPort,StrFccPort);

            s_fccServerInfo.redirectedFccIp = FccIpAddress;
            s_fccServerInfo.redirectedFccPort = FccSignalPort;
            strcpy(s_fccServerInfo.redirectedFccStrIp, StrFccIp);
            strcpy(s_fccServerInfo.redirectedFccStrPort, StrFccPort);
            s_fccServerInfo.expiredTimestamp = getMonotonicTime() + FccValidTime;
            av_log(NULL, AV_LOG_INFO, "[%s:%d] FccValidTime:%d\n", __FUNCTION__, __LINE__, FccValidTime);

            //
            SetupUdpSocket(&Rfc->Signalling.Uc, StrFccIp, StrFccPort, FccSignalPort,-1,0);
            Rfc->Signalling.Fd = ffurl_get_file_handle(Rfc->Signalling.Uc);
            Rfc->Signalling.Uc->flags = AVIO_FLAG_READ_WRITE;
            av_log(NULL, AV_LOG_INFO, "[%s:%d],Rfc->Signalling.Fd:%d\n", __FUNCTION__, __LINE__,Rfc->Signalling.Fd);
            Rfc->Signalling.LocalPort =ff_udp_get_local_port(Rfc->Signalling.Uc);
            Rfc->Unicast.LocalPort = Rfc->Signalling.LocalPort-1;
            av_log(NULL, AV_LOG_INFO, "[%s:%d],Rfc->Signalling.LocalPort:%d,Rfc->LocalUnicastStreamPort:%d\n", __FUNCTION__, __LINE__,Rfc->Signalling.LocalPort,Rfc->Unicast.LocalPort);

            if (NULL != Rfc->Unicast.Uc) {
                av_log(NULL, AV_LOG_INFO, "[%s:%d] close old unicast socket in redirect!\n", __FUNCTION__, __LINE__);
                ffurl_close(Rfc->Unicast.Uc);
                Rfc->Unicast.Uc = NULL;
                Rfc->Unicast.Fd = -1;
            }

            //setup the unicast socket to receive the unicast stream //unicast stream local socket
            Rfc->Unicast.Port = FccMediaPort;
            ret = SetupUdpSocket(&Rfc->Unicast.Uc, hostname, strport, FccMediaPort,Rfc->Unicast.LocalPort,1);
            if (0 == ret)
            {
                Rfc->Unicast.Fd = ffurl_get_file_handle(Rfc->Unicast.Uc);
                Rfc->Unicast.Status = 2;
                av_log(NULL, AV_LOG_INFO, "[%s:%d],Rfc->Unicast.Fd:%d,Rfc->Status:%d\n", __FUNCTION__, __LINE__,Rfc->Unicast.Fd,Rfc->Unicast.Status);
            }
            else
            {
                av_log(NULL, AV_LOG_INFO, "[%s:%d],build unicast socekt fail\n", __FUNCTION__, __LINE__);
            }
            //send new request rtcp pac
            uint8_t RtcpPac[40];
            uint32_t RtcpLen = 40;
            MakeNewRtcpPac(Rfc,RtcpPac, 2,-1);
            ret = ffurl_write(Rfc->Signalling.Uc, RtcpPac, RtcpLen);
            av_log(NULL, AV_LOG_INFO, "[%s:%d],ret:%d\n",__FUNCTION__,__LINE__,ret);
        }
        else
        {
            av_log(NULL, AV_LOG_INFO, "[%s:%d]not surpported type:%d!!!\n", __FUNCTION__, __LINE__,Type);
            return -1;
        }
    }
    //同步通知消息
    else if (4 == Fmt &&  Rfc->Multicast.Status < 1)
    {
        av_log(NULL, AV_LOG_INFO, "[%s:%d]receive rtcp sync fmt type:%d!!!\n", __FUNCTION__, __LINE__,Fmt);
        //setup the multicast socket to receive the multicast stream
        JoinMulticast(Rfc);
    }
    //not surpported
    else
    {
        av_log(NULL, AV_LOG_INFO, "[%s:%d]invalid rtcp fmt type:%d!!!\n", __FUNCTION__, __LINE__,Fmt);
        return -1;
    }

    return 0;
}

//seq1: most old
//seq2: old
//seq3: now
int judge_seq_discontinuity(int seq1, int seq2, int seq3)
{
    int diff1 = abs(seq_subtraction(seq1, seq3));
    int diff2 = abs(seq_subtraction(seq2, seq3));

    if (diff1 < diff2) {
        return 1;
    }

    return 0;
}

int parse_rtp_ts_packet(RTPPacket* lpkt)
{
    int payload_type = lpkt->buf[1] & 0x7f;
    uint8_t * lpoffset=NULL;
    int offset=0;
    uint8_t * lpkt_buf=NULL;
    int len=0;
    int ext=0;
    int csrc = 0;

    if (33 == payload_type) {
        // mpegts packet, parse the rtp playload data
        lpkt_buf=lpkt->buf;
        len=lpkt->len;

        if (lpkt_buf[0] & 0x20)
        {
            // remove the padding data
            int padding = lpkt_buf[len - 1];
            if (len >= 12 + padding)
            {
                len -= padding;
            }
        }

        if (len <= 12)
        {
            av_log(NULL, AV_LOG_ERROR, "[%s:%d]len<=12,len=%d\n",__FUNCTION__,__LINE__,len);
            return 0;
        }
        // output the playload data
        offset = 12 ;
        lpoffset = lpkt_buf + 12;

        csrc = lpkt_buf[0] & 0x0f;
        ext = lpkt_buf[0] & 0x10;
        if (ext > 0)
        {
            offset += 4*csrc;
            lpoffset += 4*csrc;
            if (len < offset + 4)
            {
                av_log(NULL, AV_LOG_ERROR, "[%s:%d]len < offset + 4\n",__FUNCTION__,__LINE__);
                return 0;
            }

            ext = (AV_RB16(lpoffset + 2) + 1) << 2;
            if (len < ext + offset)
            {
                av_log(NULL, AV_LOG_ERROR, "[%s:%d]len < ext + offset\n",__FUNCTION__,__LINE__);
                return 0;
            }
            offset+=ext ;
            lpoffset+=ext ;
        }
        lpkt->valid_data_offset=offset;

    } else {
        av_log(NULL, AV_LOG_ERROR, "[%s:%d]unknow payload type = %d, seq=%d\n", __FUNCTION__, __LINE__, payload_type,lpkt->seq);
        return 0;
    }

    return 1;
}

static void *RtpFccRecvTask( void *_RtpFccContext)
{
    av_log(NULL, AV_LOG_INFO, "[%s:%d]rtp rtp fcc receive task start running!!!\n", __FUNCTION__, __LINE__);
    RtpFccContext * s=(RtpFccContext *)_RtpFccContext;
    if (NULL == s)
    {
        av_log(NULL, AV_LOG_INFO, "[%s:%d]Null handle!!!\n", __FUNCTION__, __LINE__);
        goto EndAbnormal;
    }
    RTPPacket * lpkt = NULL;
    int payload_type=0;
    uint8_t * lpoffset=NULL;
    int offset=0;
    uint8_t * lpkt_buf=NULL;
    int len=0;
    int ext=0;
    int csrc = 0;
    int SleepTime = 0;
    s->ThreadStatus = 3;
    while (3 <= s->ThreadStatus)
    {
        if (url_interrupt_cb())
        {
            av_log(NULL, AV_LOG_INFO, "[%s:%d],call url_interrupt_cb\n", __FUNCTION__, __LINE__);
            goto EndAbnormal;
        }

        if (s->Recvlist.item_count >= max_rtp_buf)
        {
            if (0 == SleepTime ||  1000 <= SleepTime)
            {
                av_log(NULL, AV_LOG_INFO, "[%s:%d]two much rtp pac in buffer,s->Recvlist.item_count:%d,SleepTime:%d\n", __FUNCTION__,  __LINE__,s->Recvlist.item_count,SleepTime);
                SleepTime = 0;
            }

            usleep(10);
            SleepTime++;
            continue;
        }

        if (lpkt != NULL)
        {
            rtp_free_packet((void *)lpkt);
            lpkt=NULL;
        }

        // malloc the packet buffer
        lpkt = av_mallocz(sizeof(RTPPacket));
        if (NULL == lpkt)
        {
            av_log(NULL, AV_LOG_INFO, "[%s:%d]\n", __FUNCTION__, __LINE__);
            goto EndAbnormal;
        }
        lpkt->buf= av_malloc(RTPPROTO_RECVBUF_SIZE);
        if (NULL == lpkt->buf)
        {
            av_log(NULL, AV_LOG_INFO, "[%s:%d]\n", __FUNCTION__, __LINE__);
            goto EndAbnormal;
        }
        // recv data
        /* +[SE] [BUG][IPTV-819][yinli.xia] added: add fcc function to caculate bandwith*/
        bandwidth_measure_start_read(s->bandwidth_measure);
        lpkt->len = RtpFccReadOnePac(s, lpkt->buf, RTPPROTO_RECVBUF_SIZE);
        if (lpkt->len > 0) {
            bandwidth_measure_finish_read(s->bandwidth_measure,lpkt->len);
        } else  {
            bandwidth_measure_finish_read(s->bandwidth_measure,0);
        }
        if (AVERROR_EXIT == lpkt->len)
        {
            av_log(NULL, AV_LOG_INFO, "[%s:%d]\n", __FUNCTION__, __LINE__);
            goto EndAbnormal;
        }

        if (!s->first_rtcp_response && lpkt->len <= 0) {
            int64_t cur_time = av_gettime();
            long diff_time = (cur_time - s->first_rtcp_send_time) / 1000;
            if (s->connectState == FCC_NORMAL_CONNECTING) {
                if (diff_time > normal_wait_first_rtcp_timeout) {
                    av_log(NULL, AV_LOG_WARNING, "[%s:%d] normal_wait_first_rtcp_timeout:%d ms, force join multicast!",
                        __FUNCTION__, __LINE__, diff_time);
                    s->first_rtcp_response = 1;
                    JoinMulticast(s);
                    SendByeRtcp(s, -1);
                    s->connectState = FCC_CONNECT_FINISH;
                    continue;
                }
            } else if (s->connectState == FCC_FAST_CONNECTING) {
                if (diff_time > fast_wait_first_rtcp_timeout) {
                    av_log(NULL, AV_LOG_WARNING, "[%s:%d] fast_wait_first_rtcp_timeout:%d ms, maybe retry with normal mode!",
                        __FUNCTION__, __LINE__, diff_time);
                    onFccFastStartFailure(s);
                    s->connectState = FCC_NORMAL_CONNECTING;
                    continue;
                }
            } else {
                av_log(NULL, AV_LOG_ERROR, "[%s:%d]can't reach here!\n", __FUNCTION__, __LINE__);
            }
        }

        //
        if (s->CurSock == &s->Signalling)
        {
            av_log(NULL, AV_LOG_INFO, "[%s:%d]receive signalling pac, buf:%p, len:%d\n", __FUNCTION__, __LINE__, lpkt->buf, lpkt->len);
            ParseOneRtcpPacket(s,lpkt->buf);
        }
        else if (s->CurSock == &s->Unicast || s->CurSock == &s->Multicast)
        {
            // paser data and buffer the packat
            payload_type = lpkt->buf[1] & 0x7f;
            lpkt->seq = AV_RB16(lpkt->buf + 2);
            if (s->CurSock == &s->Multicast)
            {
                if (-1 == s->Multicast.LastSeqNum)
                {
                    s->Multicast.LastSeqNum = lpkt->seq;
                    // this is conflict with stopReceive logic, disable it temporary
                    //s->FirstMulticastSeq    = lpkt->seq;
                    s->Multicast.firstSeqNum = lpkt->seq;
                    av_log(NULL, AV_LOG_INFO, "[%s:%d]the first multicast seq:%d,s->FirstMulticastSeq:%d,all CntUm:%d\n", __FUNCTION__, __LINE__,lpkt->seq,s->FirstMulticastSeq,s->Unicast.Cnt);
                    SendByeRtcp(s,lpkt->seq);
                    if (s->Unicast.Status > 0)
                    {
//                        av_log(NULL, AV_LOG_INFO, "[%s:%d]discard the first mulitcast pac\n", __FUNCTION__, __LINE__);
//                        continue;
                    }
                    else
                    {
                        av_log(NULL, AV_LOG_INFO, "[%s:%d]unicast is not setted up,the first pac is needed\n", __FUNCTION__, __LINE__);
                    }
                }

                if (s->Multicast.LastSeqNum != -1) {
                    if (s->Multicast.bak_pkt != NULL) {
                        if (judge_seq_discontinuity(s->Multicast.LastSeqNum, s->Multicast.bak_pkt->seq, lpkt->seq)) {
                            av_log(NULL, AV_LOG_INFO,"[%s:%d] multicast discard discontinuity packet, LastSeqNum:%d, discard pkt seq:%d, new pkt seq:%d\n", __FUNCTION__, __LINE__,
                            s->Multicast.LastSeqNum, s->Multicast.bak_pkt->seq, lpkt->seq);
                            rtp_free_packet(s->Multicast.bak_pkt);
                        } else {
                            //enque bak_pkt
                            av_log(NULL, AV_LOG_INFO, "[%s:%d] enqueue discontinuity packet seq:%d, new pkt seq:%d\n", __FUNCTION__, __LINE__,
                            s->Multicast.bak_pkt->seq, lpkt->seq);
                            if (parse_rtp_ts_packet(s->Multicast.bak_pkt)) {
                                rtp_enqueue_packet(&(s->Recvlist), s->Multicast.bak_pkt);
                            } else {
                                rtp_free_packet(s->Multicast.bak_pkt);
                            }
                        }
                        s->Multicast.bak_pkt = NULL;
                    } else if (abs(seq_subtraction(s->Multicast.LastSeqNum, lpkt->seq)) >= sequence_order_range) {
                        av_log(NULL, AV_LOG_INFO,"[%s:%d] multi packet sequence out of range, seq:%d, lastSeq:%d\n", __FUNCTION__, __LINE__,
                        lpkt->seq, s->Multicast.LastSeqNum);
                        s->Multicast.bak_pkt = lpkt;
                        lpkt = NULL;
                        continue;
                    }
                }

                s->Multicast.LastSeqNum = lpkt->seq;
                if (0 == s->Multicast.Cnt%1000)
                {
                    av_log(NULL, AV_LOG_INFO, "[%s:%d]receive muticast pac:%d,s->Recvlist.item_count:%d! LastSeqNum:%d\n", __FUNCTION__, __LINE__,s->Multicast.Cnt,s->Recvlist.item_count, s->Multicast.LastSeqNum);
                }
                s->Multicast.Cnt++;
            }
            else
            {
                if (s->Unicast.LastSeqNum != -1) {
                    if (s->Unicast.bak_pkt != NULL) {
                        if (judge_seq_discontinuity(s->Unicast.LastSeqNum, s->Unicast.bak_pkt->seq, lpkt->seq)) {
                            av_log(NULL, AV_LOG_INFO,"[%s:%d] Unicast discard discontinuity packet, LastSeqNum:%d, discard pkt seq:%d, new pkt seq:%d\n", __FUNCTION__, __LINE__,
                            s->Unicast.LastSeqNum, s->Unicast.bak_pkt->seq, lpkt->seq);
                            rtp_free_packet(s->Unicast.bak_pkt);
                        } else {
                            //enque bak_pkt
                            av_log(NULL, AV_LOG_INFO, "[%s:%d] enqueue discontinuity packet seq:%d, new pkt seq:%d\n", __FUNCTION__, __LINE__,
                            s->Unicast.bak_pkt->seq, lpkt->seq);
                            if (parse_rtp_ts_packet(s->Unicast.bak_pkt)) {
                                rtp_enqueue_packet(&(s->Recvlist), s->Unicast.bak_pkt);
                            } else {
                                rtp_free_packet(s->Unicast.bak_pkt);
                            }
                        }
                        s->Unicast.bak_pkt = NULL;
                    } else if (abs(seq_subtraction(s->Unicast.LastSeqNum, lpkt->seq)) >= sequence_order_range) {
                        av_log(NULL, AV_LOG_INFO,"[%s:%d] Unicast packet sequence out of range, seq:%d, lastSeq:%d\n", __FUNCTION__, __LINE__,
                        lpkt->seq, s->Unicast.LastSeqNum);
                        s->Unicast.bak_pkt = lpkt;
                        lpkt = NULL;
                        continue;
                    }
                }

                s->Unicast.LastSeqNum = lpkt->seq;
                //
                if (s->Unicast.LastSeqNum == s->FirstMulticastSeq && -1 != s->FirstMulticastSeq)
                {
                    if (NULL != s->Unicast.Uc)
                    {
                        ffurl_close(s->Unicast.Uc);
                        s->Unicast.Uc = NULL;
                        av_log(NULL, AV_LOG_INFO, "[%s:%d]it is time to close the unicast,seq num:%d\n", __FUNCTION__, __LINE__,s->FirstMulticastSeq);
                    }
                }
                if (0 == s->Unicast.Cnt%1000)
                {
                    av_log(NULL, AV_LOG_INFO, "[%s:%d]receive unicast pac:%d,s->Recvlist.item_count:%d LastSeqNum:%d\n", __FUNCTION__, __LINE__,s->Unicast.Cnt,s->Recvlist.item_count, s->Unicast.LastSeqNum);
                }
                s->Unicast.Cnt++;

                if (s->Multicast.firstSeqNum!=-1 && seq_greater_and_equal(lpkt->seq, s->Multicast.firstSeqNum)) {
                    av_log(NULL, AV_LOG_INFO, "[%s:%d] stopReceive unicast, unicast seq:%d, multicast seq:%d\n", __FUNCTION__, __LINE__, lpkt->seq, s->Multicast.firstSeqNum);
                    s->Unicast.stopReceive = 1;
                    continue;
                }
            }

            if (33 == payload_type)
            {
                // mpegts packet, parse the rtp playload data
                lpkt_buf=lpkt->buf;
                len=lpkt->len;

                if (lpkt_buf[0] & 0x20)
                {
                    // remove the padding data
                    int padding = lpkt_buf[len - 1];
                    if (len >= 12 + padding)
                    {
                        len -= padding;
                    }
                }

                if (len <= 12)
                {
                    av_log(NULL, AV_LOG_ERROR, "[%s:%d]len<=12,len=%d\n",__FUNCTION__,__LINE__,len);
                    continue;
                }
                // output the playload data
                offset = 12 ;
                lpoffset = lpkt_buf + 12;

                csrc = lpkt_buf[0] & 0x0f;
                ext = lpkt_buf[0] & 0x10;
                if (ext > 0)
                {
                    offset += 4*csrc;
                    lpoffset += 4*csrc;
                    if (len < offset + 4)
                    {
                        av_log(NULL, AV_LOG_ERROR, "[%s:%d]len < offset + 4\n",__FUNCTION__,__LINE__);
                        continue;
                    }

                    ext = (AV_RB16(lpoffset + 2) + 1) << 2;
                    if (len < ext + offset)
                    {
                        av_log(NULL, AV_LOG_ERROR, "[%s:%d]len < ext + offset\n",__FUNCTION__,__LINE__);
                        continue;
                    }
                    offset+=ext ;
                    lpoffset+=ext ;
                }
                lpkt->valid_data_offset=offset;

                if (s->first_packet_get == 0) {
                    s->first_packet_get = 1;
                    av_log(NULL, AV_LOG_INFO, "[%s:%d] first_packet_get, size:%d\n", __FUNCTION__, __LINE__, lpkt->len);
                }

                if (rtp_enqueue_packet(&(s->Recvlist), lpkt)<0)
                {
                    av_log(NULL, AV_LOG_INFO, "[%s:%d]\n", __FUNCTION__, __LINE__);
                    goto EndAbnormal;
                }
            }
            else
            {
                av_log(NULL, AV_LOG_ERROR, "[%s:%d]unknow payload type = %d, seq=%d\n", __FUNCTION__, __LINE__, payload_type,lpkt->seq);
                if (lpkt != NULL)
                {
                    rtp_free_packet((void *)lpkt);
                    lpkt=NULL;
                }
                continue;
            }
        } else {
            if (lpkt != NULL)
            {
                rtp_free_packet((void *)lpkt);
                lpkt=NULL;
            }
        }
        lpkt = NULL;
    }
    EndAbnormal:
    if (lpkt != NULL)
    {
        rtp_free_packet((void *)lpkt);
        lpkt=NULL;
    }
    av_log(NULL, AV_LOG_ERROR, "[%s:%d]rtp fcc receive task end!!!,s->ThreadStatus:%x\n", __FUNCTION__, __LINE__,s->ThreadStatus);
    s->ThreadStatus = 0xb;
    return NULL;
}

//
static int rtpfcc_open(URLContext *h, const char *uri, int flags)
{
    av_log(NULL, AV_LOG_INFO, "[%s,%d]rtpfcc_open %s\n", __FUNCTION__,__LINE__,uri);

    max_rtp_buf = am_getconfig_int_def("media.amplayer.rtp_max",10000);
    wait_order_timeout = am_getconfig_int_def("media.amplayer.rtp_wait_order_time",100);
    wait_min_queue_size = am_getconfig_int_def("media.amplayer.rtp_min_queue_size", 10);
    wait_order_packet_low = am_getconfig_int_def("media.amplayer.rtp_wait_order_pkt",30);
    sequence_order_range = am_getconfig_int_def("media.amplayer.rtp_seq_order_range", 500);
    normal_wait_first_rtcp_timeout = am_getconfig_int_def("media.amplayer.normal_wait_first_rtcp_timeout", 600);
    fast_wait_first_rtcp_timeout = am_getconfig_int_def("media.amplayer.fast_wait_first_rtcp_timeout", 80);
    av_log(NULL, AV_LOG_INFO, "[%s:%d] max_rtp_buf:%d, wait_order_timeout:%dms, wait_min_queue_size:%d, wait_order_packet_low:%d,"
        "sequence_order_range:%d, normal_wait_first_rtcp_timeout:%d, fast_wait_first_rtcp_timeout:%d\n", __FUNCTION__, __LINE__,
        max_rtp_buf, wait_order_timeout, wait_min_queue_size, wait_order_packet_low, sequence_order_range,
        normal_wait_first_rtcp_timeout, fast_wait_first_rtcp_timeout
    );

    RtpFccContext *s = h->priv_data;
    s = av_mallocz(sizeof(RtpFccContext));
    if (NULL == s)
    {
        return AVERROR(ENOMEM);
    }
    h->is_slowmedia = 1;
    s->ThreadStatus = 0;
    s->Unicast.Status = 0;
    s->Multicast.Status = 0;
    s->Signalling.Status = 0;
    struct in_addr addr;
    char buf[1024]={0};
    char path[1024]={0};
    char hostname[256]={0};
    strcpy(s->url, uri);
    s->flags = flags;
    const char *p;
    memset(s->Multicast.StrIp,0,sizeof(s->Multicast.StrIp));
    av_url_split(NULL, 0, NULL, 0, hostname, sizeof(hostname), &s->Multicast.Port, path, sizeof(path), uri);
    strcpy(s->Multicast.StrIp,hostname);
    av_log(NULL, AV_LOG_INFO, "[%s,%d],s->Multicast.StrIp %s,hostname:%s\n", __FUNCTION__,__LINE__,s->Multicast.StrIp,hostname);
    s->Multicast.Ip = IpToInt(s->Multicast.StrIp,0);
    av_log(NULL, AV_LOG_INFO, "[%s,%d],s->Multicast.Ip %x\n", __FUNCTION__,__LINE__,s->Multicast.Ip);
    snprintf(s->Multicast.StrPort,sizeof(s->Multicast.StrPort),"%d",s->Multicast.Port);
    av_log(NULL, AV_LOG_INFO,
    "[%s:%d]s->Multicast.StrIp:%s,MulticastIp:%#x,rtp_port:%d\n",__FUNCTION__,__LINE__,s->Multicast.StrIp,s->Multicast.Ip,s->Multicast.Port);

    h->priv_data = s;
    p = strchr(uri, '?');
    if (NULL != p)
    {
        if (av_find_info_tag(buf, sizeof(buf), "ChannelFCCIP", p))
        {
            av_log(NULL, AV_LOG_INFO, "[%s:%d]buf: %s,p:%s\n",__FUNCTION__,__LINE__,buf,p);
            memcpy(s->Signalling.StrIp, buf,sizeof(s->Signalling.StrIp));
            s->Signalling.Ip = IpToInt(s->Signalling.StrIp,0);
            av_log(NULL, AV_LOG_INFO, "[%s:%d]s->Signalling.StrIp: %s,s->Signalling.Ip:%x\n",__FUNCTION__,__LINE__,s->Signalling.StrIp,s->Signalling.Ip);
        }

        if (av_find_info_tag(buf, sizeof(buf), "ChannelFCCPort", p))
        {
            s->Signalling.Port = strtol(buf, NULL, 10);
            snprintf(s->Signalling.StrPort ,sizeof(s->Signalling.StrPort),"%d",s->Signalling.Port);
            av_log(NULL, AV_LOG_INFO, "[%s:%d]s->Signalling.StrPort: %s,s->Signalling.Port:%d\n", __FUNCTION__,__LINE__,s->Signalling.StrPort,s->Signalling.Port);
        }
    }

    if (s->Signalling.Port < 0)
    {
        av_log(NULL, AV_LOG_INFO, "[%s:%d],s->Signalling.Port:%d\n",__FUNCTION__,__LINE__,s->Signalling.Port);
        goto fail;
    }

    s->connectState = initFccConnectState(__FUNCTION__);

    int RetSock = -1;
    if (s->connectState == FCC_FAST_CONNECTING) {
        av_log(NULL, AV_LOG_INFO, "[%s:%d],fcc_fast_mode ip:%s, port:%d\n",__FUNCTION__,__LINE__,
            s_fccServerInfo.redirectedFccStrIp, s_fccServerInfo.redirectedFccPort);
        RetSock = SetupUdpSocket(&s->Signalling.Uc, s_fccServerInfo.redirectedFccStrIp, s_fccServerInfo.redirectedFccStrPort, s_fccServerInfo.redirectedFccPort,-1,0);
    } else if (s->connectState == FCC_NORMAL_CONNECTING) {
        RetSock = SetupUdpSocket(&s->Signalling.Uc, s->Signalling.StrIp, s->Signalling.StrPort, s->Signalling.Port,-1,0);
    }

    if (-1 == RetSock)
    {
        av_log(NULL, AV_LOG_INFO, "[%s:%d],setup socket fail\n",__FUNCTION__,__LINE__);
        goto fail;
    }
    //
    s->Signalling.Fd = ffurl_get_file_handle(s->Signalling.Uc);
    av_log(NULL, AV_LOG_INFO, "[%s:%d],s->Signalling.Fd:%d\n", __FUNCTION__, __LINE__,s->Signalling.Fd);
    s->Signalling.LocalPort =ff_udp_get_local_port(s->Signalling.Uc);
    s->Unicast.LocalPort = s->Signalling.LocalPort-1;
    av_log(NULL, AV_LOG_INFO, "[%s:%d],s->Signalling.LocalPort:%d,s->Unicast.LocalPort:%d\n", __FUNCTION__,__LINE__,s->Signalling.LocalPort,s->Unicast.LocalPort);
    //
    s->Signalling.Uc->flags = AVIO_FLAG_READ_WRITE;
    s->Signalling.Status = 1;

    av_log(NULL, AV_LOG_INFO, "[%s:%d]create unicast socket!\n", __FUNCTION__, __LINE__);
    //setup the unicast socket to receive the unicast stream //unicast stream local socket
    s->Unicast.Port = 0;
    int ret = SetupUdpSocket(&s->Unicast.Uc, "", "", 0, s->Unicast.LocalPort,1);
    if (0 == ret)
    {
        s->Unicast.Fd = ffurl_get_file_handle(s->Unicast.Uc);
        s->Unicast.Status = 1;
        av_log(NULL, AV_LOG_INFO, "[%s:%d],s->Unicast.Fd:%d,s->Status:%d\n", __FUNCTION__, __LINE__,s->Unicast.Fd,s->Unicast.Status);
    }
    else
    {
        av_log(NULL, AV_LOG_INFO, "[%s:%d],build unicast socekt fail\n", __FUNCTION__, __LINE__);
    }

    //send rtcp request
    uint8_t RtcpPac[40];
    uint32_t RtcpLen = 40;
    MakeNewRtcpPac(s,RtcpPac, 2,-1);
    av_log(NULL, AV_LOG_INFO, "[%s:%d]\n",__FUNCTION__,__LINE__);
    ret = ffurl_write(s->Signalling.Uc, RtcpPac, RtcpLen);
    av_log(NULL, AV_LOG_INFO, "[%s:%d],ret:%d\n",__FUNCTION__,__LINE__,ret);
    if (ret < 0)
    {
        goto fail;
    }
    s->first_rtcp_send_time = av_gettime();

    s->Recvlist.max_items               = -1;
    s->Recvlist.item_ext_buf_size       = 0;
    s->Recvlist.muti_threads_access     = 1;
    s->Recvlist.reject_same_item_data   = 0;
    s->CurItem = NULL;
    itemlist_init(&s->Recvlist) ;
    av_log(NULL, AV_LOG_INFO, "[%s:%d]\n", __FUNCTION__, __LINE__);
    //
    //s->Unicast.Fd   = -1;
    s->Multicast.Fd = -1;
    s->Signalling.Status = 1;
    s->Unicast.Cnt = 0;
    s->Unicast.stopReceive = 0;
    s->Multicast.Cnt = 0;
    s->Signalling.Cnt = 0;
    //
    s->LastSeqNum = -1;
    s->FirstMulticastSeq = -1;
    s->Signalling.LastSeqNum = -1;
    s->Unicast.LastSeqNum = -1;
    s->Multicast.LastSeqNum = -1;
    s->Multicast.firstSeqNum = -1;
    //
    av_log(NULL, AV_LOG_INFO, "[%s:%d]\n", __FUNCTION__, __LINE__);
    s->bandwidth_measure=bandwidth_measure_alloc(100,0);
    if (amthreadpool_pthread_create(&(s->RecvThread), NULL, RtpFccRecvTask, s))
    {
        av_log(NULL, AV_LOG_ERROR, "[%s:%d]creat receive thread failed\n",__FUNCTION__,__LINE__);
        s->ThreadStatus = 2;
        goto fail;
    }
    pthread_setname_np(s->RecvThread, "RecvThread");
    av_log(NULL, AV_LOG_INFO, "[%s:%d]\n", __FUNCTION__, __LINE__);
    return 0;

    fail:
    if (s->bandwidth_measure != NULL) {
        bandwidth_measure_free(s->bandwidth_measure);
        s->bandwidth_measure = NULL;
    }

    av_log(NULL, AV_LOG_INFO, "[%s:%d]\n",__FUNCTION__,__LINE__);
    if (NULL != s->Unicast.Uc)
    {
        ffurl_close(s->Unicast.Uc);
    }
    if (NULL != s->Multicast.Uc)
    {
        ffurl_close(s->Multicast.Uc);
    }
    if (NULL != s->Signalling.Uc)
    {
        ffurl_close(s->Signalling.Uc);
    }

    av_free(s);
    return AVERROR(EIO);
}

static int rtpfcc_reset(URLContext *h)
{
    RtpFccContext *s = h->priv_data;
    char uri[MAX_URL_SIZE];
    strcpy(uri, s->url);
    int flags = s->flags;

    rtpfcc_close(h);
    rtpfcc_open(h,uri,flags);
    return 0;
}

static int rtpfcc_read(URLContext *h, uint8_t *buf, int size)
{
    RtpFccContext *s = h->priv_data;
    struct item *HeadItem = NULL;
    struct item *NextItem = NULL;
    RTPPacket *HeadRtp = NULL;
    RTPPacket *NextRtp = NULL;
    struct list_head *NextList = NULL;
    RTPPacket *lpkt = NULL;
    int readsize=0;
    int single_readsize=0;
    int TimeSleep = 0;
    char isNULL = 0;
    char out_of_sequence = 0;
    //real=valuetime*70


    s->Signalling.Cnt++;
    int64_t start = av_gettime();

    while (s->Signalling.Status > 0 && size>readsize)
    {
        if (url_interrupt_cb())
        {
            av_log(NULL, AV_LOG_ERROR, "[%s:%d]call url_interrupt_cb\n",__FUNCTION__,__LINE__);
            return AVERROR(EIO);
        }

        if (check_net_phy_conn_status() == 0)
        {
            s->network_down = 1;
            break;
        }
        else
        {
            if (s->network_down == 1)
                rtpfcc_reset(h);
            s->network_down = 0;
        }

        if (s->CurItem != NULL)
            goto do_read;

        if (s->Recvlist.item_count < wait_min_queue_size)
        {
            usleep(10);
            ++TimeSleep;
            continue;
        }

        HeadItem = itemlist_peek_head(&s->Recvlist);
        if (NULL == HeadItem)
        {
            av_log(NULL, AV_LOG_ERROR, "[%s:%d](NULL == HeadItem)\n",__FUNCTION__,__LINE__);
            usleep(1);
            ++TimeSleep;
            continue;
        }
        //

        HeadRtp = HeadItem->item_data;
        NextList = HeadItem->list.next;
        isNULL = &s->Recvlist.list == NextList;
        if (isNULL)
        {
            av_log(NULL, AV_LOG_ERROR, "[%s:%d],s->recvlist.item_count:%d\n",__FUNCTION__,__LINE__,s->Recvlist.item_count);
            usleep(1);
            ++TimeSleep;
            continue;
        }
        NextItem = list_entry(NextList, struct item, list);

        if (NULL == NextItem)
        {
            av_log(NULL, AV_LOG_ERROR, "[%s:%d](NULL == NextItem)\n",__FUNCTION__,__LINE__);
            usleep(1);
            ++TimeSleep;
            continue;
        }
        NextRtp = NextItem->item_data;

        if (NULL == s->CurItem && (HeadRtp->seq+1)%MAX_RTP_SEQ != NextRtp->seq)
        {
            out_of_sequence = 1;
            usleep(10);
            ++TimeSleep;
            int timeCost = (int)(av_gettime() - start)/1000;
            int item_count = s->Recvlist.item_count;
            if (item_count < wait_order_packet_low && timeCost < wait_order_timeout) {
                continue;
            }
            av_log(NULL, AV_LOG_INFO, "[%s:%d] break_wait_order item_count:%d, timeCost:%dms\n", __FUNCTION__, __LINE__, item_count, timeCost);
        } else if (out_of_sequence) {
            out_of_sequence = 0;
            int timeCost = (int)(av_gettime() - start)/1000;
            av_log(NULL, AV_LOG_INFO, "[%s:%d] wait_sequence_order success, seq:%d, item_count:%d, timeCost:%dms\n", __FUNCTION__, __LINE__, HeadRtp->seq, s->Recvlist.item_count, timeCost);
        }
        if (NULL == s->CurItem)
        {
            s->CurItem = itemlist_get_head(&s->Recvlist);

            if (NULL == s->CurItem)
            {
                av_log(NULL, AV_LOG_INFO, "[%s,%d] \n", __FUNCTION__,__LINE__);
                usleep(1);
                continue;
            }
        }

do_read:
        lpkt = s->CurItem->item_data;

        single_readsize=min(lpkt->len-lpkt->valid_data_offset, size-readsize);
        memcpy(buf+readsize,lpkt->buf+lpkt->valid_data_offset,single_readsize);
        readsize+=single_readsize;
        lpkt->valid_data_offset+=single_readsize;

        if (lpkt->valid_data_offset >= lpkt->len)
        {
            if ((s->LastSeqNum + 1) % MAX_RTP_SEQ != lpkt->seq && -1 != s->LastSeqNum)
            {
                av_log(NULL, AV_LOG_ERROR, "[%s:%d]discontinuity seq=%d, the right seq=%d\n",__FUNCTION__,__LINE__, lpkt->seq,(s->LastSeqNum+1)%MAX_RTP_SEQ);
            }
            s->LastSeqNum = lpkt->seq;
            // already read, no valid data clean it
            item_free(s->CurItem);
            s->CurItem = NULL;
            rtp_free_packet((void *)lpkt);
            lpkt=NULL;
        }
        if (TimeSleep > 0)
        {
            //av_log(NULL, AV_LOG_ERROR, "[%s:%d]TimeSleep:%d, ThreadStatus:%d\n",__FUNCTION__,__LINE__, TimeSleep, s->ThreadStatus);
            TimeSleep = 0;
        }
    }

    int TimeCost = (int)(av_gettime() - start)/1000;
    if (TimeCost >= 10) {
        av_log(NULL, AV_LOG_INFO, "[%s,%d],size:%d,readsize:%d,item_count:%d,CntRead:%d,use %d ms \n", __FUNCTION__,__LINE__,size,readsize,s->Recvlist.item_count,s->Signalling.Cnt,TimeCost);
    }

    if (readsize <= 0)
    {
        av_log(NULL, AV_LOG_ERROR, "[%s:%d]readsize <= 0:%d\n",__FUNCTION__,__LINE__,readsize);
        return AVERROR(EAGAIN);
    }

    if (s->first_packet_read == 0) {
        s->first_packet_read = 1;
        av_log(NULL, AV_LOG_INFO, "[%s:%d] first_packet_read, size:%d\n", __FUNCTION__, __LINE__, readsize);
    }

    return readsize;
}


static int rtpfcc_close(URLContext *h)
{

    RtpFccContext *s = h->priv_data;
    SendByeRtcp(s,-1);
    s->Unicast.Status = -1;
    s->Multicast.Status = -1;
    s->Signalling.Status = -1;
    s->ThreadStatus = 2;
    #if 1
    amthreadpool_pthread_join(s->RecvThread, NULL);
    #else
    //wait for receive thread quit
    if (3 <= s->ThreadStatus)
    {
        int SleepTime = 0;

        while (0xb != s->ThreadStatus)
        {
            usleep(1);
            SleepTime++;
            if (SleepTime >= 1000000)
            {
                av_log(NULL, AV_LOG_INFO, "[%s,%d],error:wait for receive thread quit timeout,SleepTime:%d,s->ThreadStatus:%x \n", __FUNCTION__,__LINE__,SleepTime,s->ThreadStatus);
                return -1;
            }
        }
        av_log(NULL, AV_LOG_INFO, "[%s,%d],wait for receive thread quit,SleepTime:%d,s->ThreadStatus:%x \n", __FUNCTION__,__LINE__,SleepTime,s->ThreadStatus);
    }
    #endif
    av_log(NULL, AV_LOG_INFO, "[%s,%d],s->ThreadStatus:%x \n", __FUNCTION__,__LINE__,s->ThreadStatus);
    s->RecvThread = 0;
    itemlist_clean(&s->Recvlist, rtp_free_packet);

    if (NULL != s->Unicast.Uc)
    {
        int ret = -1;
        ret = ffurl_close(s->Unicast.Uc);
        av_log(NULL, AV_LOG_INFO, "[%s,%d] close Unicast ret:%d\n", __FUNCTION__,__LINE__, ret);
        s->Unicast.Uc = NULL;
    }
    if (NULL != s->Multicast.Uc)
    {
        int ret  = -1;

        ret = ffurl_close(s->Multicast.Uc);
        av_log(NULL, AV_LOG_INFO, "[%s,%d] close Multicast ret:%d\n", __FUNCTION__,__LINE__, ret);

        s->Multicast.Uc = NULL;
    }
    if (NULL != s->Signalling.Uc)
    {
        int ret = -1;
        ret = ffurl_close(s->Signalling.Uc);
        av_log(NULL, AV_LOG_INFO, "[%s,%d] close Signalling ret:%d\n", __FUNCTION__,__LINE__, ret);
        s->Signalling.Uc = NULL;
    }
    if (s->bandwidth_measure)
        bandwidth_measure_free(s->bandwidth_measure);
    s->bandwidth_measure = NULL;
    av_free(s);
    h->priv_data = NULL;
    av_log(NULL, AV_LOG_INFO, "[%s,%d] \n", __FUNCTION__,__LINE__);
    return 0;
}

/* +[SE] [BUG][IPTV-819][yinli.xia] added: add fcc function to caculate bandwith*/
static int rtpfcc_get_info(URLContext *h, uint32_t  cmd, uint32_t flag, int64_t *info)
{
    if (h == NULL) {
        return -1;
    }

    //RTPContext *s = h->priv_data;
    RtpFccContext *s = h->priv_data;

    if (s == NULL)
        return -1;
    if (cmd == AVCMD_GET_NETSTREAMINFO) {
        if (flag == 2) {//current streaming bitrate ==>ref download bitrate
            int mean_bps, fast_bps, avg_bps,ret = -1;
            ret = bandwidth_measure_get_bandwidth(s->bandwidth_measure,&fast_bps, &mean_bps, &avg_bps);
            *info = avg_bps;
        }
        return 0;
    }
    return -1;
}

URLProtocol ff_rtpfcc_protocol =
{
    .name           = "rtpfcc",
    .url_open       = rtpfcc_open,
    .url_read       = rtpfcc_read,
    .url_write      = NULL,
   .url_close      = rtpfcc_close,
   .url_getinfo    = rtpfcc_get_info,
};

/* -[SE] [REQ][IPTV-19][jungle.wang]:add fast channel switch module file RtpFcc.c */

URLProtocol ff_rtp_protocol = {
    .name                = "rtp",
    .url_open            = rtp_open,
    .url_read            = rtp_read,
    .url_write           = rtp_write,
    .url_close           = rtp_close,
    .url_get_file_handle = rtp_get_file_handle,
    .url_getinfo         = rtp_get_info,
};

URLProtocol ff_rtpfec_protocol = {
    .name                = "rtpfec",
    .url_open            = rtpfec_open,
    .url_read            = rtpfec_read,
    .url_write           = NULL,
    .url_close           = rtpfec_close,
    .url_getinfo         = rtpfec_get_info,
};
