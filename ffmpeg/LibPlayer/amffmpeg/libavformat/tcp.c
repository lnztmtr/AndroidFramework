/*
 * TCP protocol
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
#include "avformat.h"
#include "libavutil/parseutils.h"
#include "libavutil/random_seed.h"
#include <unistd.h>
#include <errno.h>
#include "internal.h"
#include "network.h"
#include "os_support.h"
#include "url.h"
#if HAVE_POLL_H
#include <poll.h>
#endif
#include <sys/time.h>
#include <pthread.h>

typedef struct GetAddrInfo {
    char addr_portstr[10];
    char addr_hostname[1024];
    int thread_exitflag;
    int getaddr_result;
    struct addrinfo addr_hints;
    struct addrinfo *addr_ai;
} getaddrinfo_t;

typedef struct TCPContext {
    int fd;
} TCPContext;

#define DNS_CACHE_NUM_MAX 32
#define IP_CACHE_NUM_MAX 8
typedef struct DNSCache {
	int used;
	char acDomainServer[64];
	int port;
	struct addrinfo* ai;
	struct addrinfo* ipv4cache[IP_CACHE_NUM_MAX];
	struct addrinfo* ipv6cache[IP_CACHE_NUM_MAX];
	int ipv4_num;
	int ipv6_num;
} DNSCache;

static DNSCache m_sDNSCache[DNS_CACHE_NUM_MAX] = {0};
static int m_nUsedNum = 0;
static int dns_cache_clear;
int m_bconnectfail = 0;
static int m_nLastIsIpv6 = 0;
static char m_cLastHostName[64];
static char m_cLastIPString[128];
static int m_nDeviceIpv6SelectCtrl = 1;

int ff_dns_cache_clear(void);

static int chk_ipv6_string(const char* hostname) {
   av_log(NULL, AV_LOG_INFO, "chk_ipv6_string start.\n");
   int colon = 0;
   int colon_total = 0;
   int dcolon = 0;
   int num = 0;
   char *ip = hostname;

   while (*ip) {
      if (((*ip >= '0') && (*ip <= '9')) || ((*ip >= 'a') && (*ip <= 'f')) || ((*ip >= 'A') && (*ip <= 'F'))) {
         colon = 0;
         num ++;
      } else if (*ip == ':') {
          num = 0;
          colon ++;
          colon_total ++;
          if (colon > 2)
             return 0;
          else if (colon == 2) {
             dcolon ++;
             if (dcolon > 1)
                return 0;
          }
       } else
         return 0;

       if (num > 4)
         return 0;

       ip++;
    }
    av_log(NULL, AV_LOG_INFO, "chk_ipv6_string end ,colon_total:%d.\n",colon_total);
    return (colon_total <= 7) && (colon_total >= 2);
}

/*return value: -1:error; 0:fail; 1:ipv4; 2:ipv6*/
static int check_ip_string(const char* hostname, int size) {
    if (hostname == NULL || size <= 0)
        return -1;

    if (chk_ipv6_string(hostname))
        return 2;

    int i = 0;
    int dot = 0;
    while(i < size) {
        if (hostname[i] != '.' && (hostname[i] > '9' || hostname[i] < '0')) {
            return 0;
        } else if (hostname[i] == '.') {
            dot++;
        }

        i++;
    }

    if (dot != 3)
        return 0;

    int a = 0;
    int b = 0;
    int c = 0;
    int d = 0;

    int ret = sscanf(hostname, "%d.%d.%d.%d", &a, &b , &c, &d);
    if (ret == 4
        && (a >= 0 && a <= 255)
        && (b >= 0 && b <= 255)
        && (c >= 0 && c <= 255)
        && (d >= 0 && d <= 255)) {
        return 1;
    }

    return 0;
}

static int ip_kick (const char* hostname, const char* ipstring, int isv6, int* dnsidx, int* ipidx) {
    if (m_nUsedNum <= 0)
       return 0;
    int i = 0, k = 0;
    struct addrinfo* tmp_ai;
    struct sockaddr_in * sockaddr;
    struct sockaddr_in6 *sockaddr_ipv6;
    char ipbuf[128];
    memset(ipbuf, 0, 128);

    av_log(NULL, AV_LOG_INFO,"ip_kick m_cLastHostName:%s,m_cLastIPString:%s, isv6:%d.\n", hostname, ipstring, isv6);

    for (; i < DNS_CACHE_NUM_MAX; i++) {
        if (m_sDNSCache[i].used == 1 && strcmp(m_sDNSCache[i].acDomainServer , hostname) == 0) {
            if (isv6) {
               for (; k < m_sDNSCache[i].ipv6_num; k++) {
                    tmp_ai = m_sDNSCache[i].ipv6cache[k];
                    sockaddr_ipv6 = (struct sockaddr_in6 *)(tmp_ai->ai_addr);
                    inet_ntop(AF_INET6,&sockaddr_ipv6->sin6_addr, ipbuf, sizeof(ipbuf));
                    if (strcmp(ipstring, ipbuf) == 0) {
                       *dnsidx = i;
                       *ipidx = k;
                       return 1;
                    } else {
                       memset(ipbuf, 0, 128);
                    }
               }
            } else {
               for (; k < m_sDNSCache[i].ipv4_num; k++) {
                    tmp_ai = m_sDNSCache[i].ipv4cache[k];
                    sockaddr = (struct sockaddr_in *)(tmp_ai->ai_addr);
                    inet_ntop(AF_INET,&sockaddr->sin_addr, ipbuf, sizeof(ipbuf));
                    if (strcmp(ipstring, ipbuf) == 0) {
                        *dnsidx = i;
                        *ipidx = k;
                        return 1;
                    } else {
                       memset(ipbuf, 0, 128);
                    }
               }
            }
        }
    }
    return 0;
}

/*
* -1 - not support,  0 - diskick,  >0 kick cache
*/
static int dns_kick(URLContext *h, const char* hostname, int size, int port) {
	
	 av_log(h, AV_LOG_INFO, "dns_kick: %s is ip addr, m_nUsedNum=%d\n", hostname, m_nUsedNum);
    //escape situation that hostname is ip
    if (check_ip_string(hostname, size) == 1 || check_ip_string(hostname, size) == 2) {
        av_log(h, AV_LOG_INFO, "%s is ip addr, escape dns cache flow\n", hostname);
        return -1;
    }

    if (m_nUsedNum <= 0)
        return 0;

    int i = 0;

    for (; i < DNS_CACHE_NUM_MAX; i++) {
        if (m_sDNSCache[i].used == 1 && m_sDNSCache[i].port == port
            && strncmp(m_sDNSCache[i].acDomainServer , hostname, size) == 0) {
            av_log(h, AV_LOG_INFO, "%s kick\n", hostname);
			if (m_sDNSCache[i].ipv4_num + m_sDNSCache[i].ipv6_num == 0) {
				av_log(NULL, AV_LOG_INFO, "ipv4 and ipv6 address is all 0");
			}
            return (i+1);
        }
    }

    return 0;
}

static char* print_ai_addr(struct addrinfo * addr, char* pcAddr, int size) {
	struct sockaddr_in * sockaddr;
	struct sockaddr_in6 *sockaddr_ipv6;
	char ipbuf[128];
	short port = 0;

	if (addr == NULL)
		return 0;

	sockaddr = (struct sockaddr_in *)addr->ai_addr;
	if (sockaddr->sin_family == AF_INET6) {
		sockaddr_ipv6 = (struct sockaddr_in6 *)(addr->ai_addr);
		inet_ntop(AF_INET6,&sockaddr_ipv6->sin6_addr, ipbuf, sizeof(ipbuf));
		port = ntohs(sockaddr_ipv6->sin6_port);
	} else {
		inet_ntop(AF_INET,&sockaddr->sin_addr, ipbuf, sizeof(ipbuf));
		port = ntohs(sockaddr->sin_port);
	}

	snprintf(pcAddr, size, "%s:%d", ipbuf, port);
	return pcAddr;
}

static int dns_cache_add(const char* hostname, int size, struct addrinfo* ai, int port) {
    int i = 0;
    int ret = 0;

    for (; i < DNS_CACHE_NUM_MAX; i++) {
        if (m_sDNSCache[i].used == 0) {
            m_sDNSCache[i].used = 1;
            m_sDNSCache[i].port = port;
            m_sDNSCache[i].ai = ai;
            memcpy(m_sDNSCache[i].acDomainServer, hostname, size);
            m_nUsedNum++;
            ret = i + 1;

            //init ipv4cache and ipv6cache
            struct addrinfo* tmp_ai;
            m_sDNSCache[i].ipv4_num = 0;
            m_sDNSCache[i].ipv6_num = 0;
            for (int index= 0; index < IP_CACHE_NUM_MAX; index++) {
                m_sDNSCache[i].ipv6cache[index] = NULL;
                m_sDNSCache[i].ipv4cache[index] = NULL;
            }

            for (tmp_ai = ai; tmp_ai != NULL; tmp_ai = tmp_ai->ai_next) {
                if (tmp_ai->ai_family == AF_INET6 && m_sDNSCache[i].ipv6_num < IP_CACHE_NUM_MAX) {
                    m_sDNSCache[i].ipv6cache[m_sDNSCache[i].ipv6_num ++] = tmp_ai;
                } else if (tmp_ai->ai_family == AF_INET && m_sDNSCache[i].ipv4_num < IP_CACHE_NUM_MAX) {
                    m_sDNSCache[i].ipv4cache[m_sDNSCache[i].ipv4_num ++] = tmp_ai;
                }
            }
            av_log(NULL, AV_LOG_INFO, "dns_cache_add ipv4_num:%d, ipv6_num:%d.\n",m_sDNSCache[i].ipv4_num,m_sDNSCache[i].ipv6_num);
            break;
        }
    }

    return ret;
}

/* return value: 0,fail; 1,success */
static int ip_cache_del(int dns_idx, int isv6, int ip_idx) {
    if (dns_idx >= m_nUsedNum)
       return 0;

    av_log(NULL, AV_LOG_INFO, "+++ ip_cache_del isv6:%d,ipv6_num:%d,ipv4_num:%d.\n",isv6,m_sDNSCache[dns_idx].ipv6_num,m_sDNSCache[dns_idx].ipv4_num);
    int i = 0;
    if (isv6) {
        if (ip_idx >= m_sDNSCache[dns_idx].ipv6_num)
            return 0;
        for (i = ip_idx; i < m_sDNSCache[dns_idx].ipv6_num - 1; i++)
            m_sDNSCache[dns_idx].ipv6cache[i] = m_sDNSCache[dns_idx].ipv6cache[i + 1];

        m_sDNSCache[dns_idx].ipv6cache[i] = NULL;
        m_sDNSCache[dns_idx].ipv6_num --;
        av_log(NULL, AV_LOG_INFO, "++- ip_cache_del isv6:%d,ipv6_num:%d,ipv4_num:%d.\n",isv6,m_sDNSCache[dns_idx].ipv6_num,m_sDNSCache[dns_idx].ipv4_num);
        return 1;
    } else {
        if (ip_idx >= m_sDNSCache[dns_idx].ipv4_num)
            return 0;
        for (i = ip_idx; i < m_sDNSCache[dns_idx].ipv4_num - 1; i++)
            m_sDNSCache[dns_idx].ipv4cache[i] = m_sDNSCache[dns_idx].ipv4cache[i + 1];

        m_sDNSCache[dns_idx].ipv4cache[i] = NULL;
        m_sDNSCache[dns_idx].ipv4_num --;
        av_log(NULL, AV_LOG_INFO, "+-- ip_cache_del isv6:%d,ipv6_num:%d,ipv4_num:%d.\n",isv6,m_sDNSCache[dns_idx].ipv6_num,m_sDNSCache[dns_idx].ipv4_num);
        return 1;
    }

    return 0;
}

static int dns_cache_del(int idx) {
	int i = 0;

	if (idx >= 0) {
		m_sDNSCache[idx].used = 0;
		if (m_sDNSCache[idx].ai != NULL) {
			av_log(NULL, AV_LOG_INFO, "%s del dns cache %s\n", __FUNCTION__, m_sDNSCache[idx].acDomainServer);
			freeaddrinfo(m_sDNSCache[idx].ai);
			m_sDNSCache[idx].ai = NULL;
			memset(m_sDNSCache[idx].acDomainServer, 0x0, sizeof(m_sDNSCache[idx].acDomainServer));

			m_sDNSCache[idx].ipv4_num = 0;
			m_sDNSCache[idx].ipv6_num = 0;
			for (int index= 0; index < IP_CACHE_NUM_MAX; index++) {
				m_sDNSCache[idx].ipv6cache[index] = NULL;
				m_sDNSCache[idx].ipv4cache[index] = NULL;
            }
			m_nUsedNum--;
		} else {
			av_log(NULL, AV_LOG_INFO, "%s error\n", __FUNCTION__);
		}
	}

	return 0;
}

static int dns_cache_reset(void) {
	int i = 0;
	for (i = 0; i< m_nUsedNum; i++)
		dns_cache_del(i);
	return 0;
}

int ff_dns_cache_clear(void) {
	dns_cache_clear = 1;
	return 0;
}

/* select_type: 0,first; 1, last; 2, random;
   return ip index;*/
static int ip_cache_select(int dns_idx, int isv6, int select_type, struct addrinfo** select_ai) {
    int select_index = -1;
    if (isv6 && m_sDNSCache[dns_idx].ipv6_num) {
         switch (select_type) {
           case 0:
             *select_ai = m_sDNSCache[dns_idx].ipv6cache[0];
		     select_index = 0;
		     break;
           case 1:
             *select_ai = m_sDNSCache[dns_idx].ipv6cache[m_sDNSCache[dns_idx].ipv6_num - 1];
			 select_index = m_sDNSCache[dns_idx].ipv6_num - 1;
		     break;
           case 2:
             select_index = av_get_random_seed() % m_sDNSCache[dns_idx].ipv6_num;
             *select_ai = m_sDNSCache[dns_idx].ipv6cache[select_index];
		     break;
         }
	} else if (!isv6 && m_sDNSCache[dns_idx].ipv4_num) {
		switch (select_type) {
		  case 0:
			*select_ai = m_sDNSCache[dns_idx].ipv4cache[0];
			select_index = 0;
		    break;
		  case 1:
			*select_ai = m_sDNSCache[dns_idx].ipv4cache[m_sDNSCache[dns_idx].ipv4_num - 1];
			select_index = m_sDNSCache[dns_idx].ipv4_num - 1;
		    break;
		  case 2:
			select_index = av_get_random_seed() % m_sDNSCache[dns_idx].ipv4_num;
			*select_ai = m_sDNSCache[dns_idx].ipv4cache[select_index];
			break;
		}
	}
	av_log(NULL, AV_LOG_INFO, "ip_cache_select :%d.\n", select_index);
	if (select_index != -1) {
	   m_nLastIsIpv6 = isv6;
	   memset(m_cLastHostName, 0, 64);
	   memset(m_cLastIPString, 0, 128);
	   memcpy(m_cLastHostName, m_sDNSCache[dns_idx].acDomainServer, 64);
	   struct sockaddr_in * sockaddr;
	   struct sockaddr_in6 *sockaddr_ipv6;
       if (isv6) {
           sockaddr_ipv6 = (struct sockaddr_in6 *)((*select_ai)->ai_addr);
		   inet_ntop(AF_INET6,&sockaddr_ipv6->sin6_addr, m_cLastIPString, 128);
	   } else {
		   sockaddr = (struct sockaddr_in *)((*select_ai)->ai_addr);
		   inet_ntop(AF_INET,&sockaddr->sin_addr, m_cLastIPString, 128);
	   }

	   av_log(NULL, AV_LOG_INFO, "last ipv6:%d,m_cLastHostName:%s,m_cLastIPString:%s.\n", m_nLastIsIpv6,m_cLastHostName,m_cLastIPString);
	}

	return select_index;
}


/* +[SE] [BUG][BUG-168197][yanan.wang] added:fix BesTV get stuck over 6s When exiting*/
static void thread_fun(void *arg)
{
	av_log(NULL, AV_LOG_INFO, "thread_fun enter, getaddrinfo\n");
	pthread_detach(pthread_self());
	getaddrinfo_t *m_addrinfo;
	m_addrinfo = (getaddrinfo_t *)arg;
	m_addrinfo->getaddr_result = getaddrinfo(m_addrinfo->addr_hostname, m_addrinfo->addr_portstr, &(m_addrinfo->addr_hints), &(m_addrinfo->addr_ai));
	m_addrinfo->thread_exitflag = 1;
}


/* return non zero if error */
static int tcp_open(URLContext *h, const char *uri, int flags)
{
    struct addrinfo hints, *ai, *cur_ai, *debug_cur_ai;
    struct sockaddr_in *addr;
    getaddrinfo_t m_addrinfo = {0};
    char ipbuf[16];
    int port, fd = -1;
    TCPContext *s = NULL;
    int listen_socket = 0;
    const char *p;
    char buf[256];
    int ret;
    socklen_t optlen;
    int timeout_ms=am_getconfig_int_def("libplayer.tcp.timeout", 5000);
    int rrsip_support = am_getconfig_int_def("libplayer.rrsip.support", 0);
    int ipv6once_devicelvl_enable = am_getconfig_int_def("media.libplayer.ipv6.once", 0);
    int ipv6cont_tcpopenlvl_enable = am_getconfig_int_def("media.libplayer.continue.ipv6", 1);
    int tcpopen_ipv6select_ctrl = 1;
    int rrsip_num_tmp = 0;
    #define TCP_POLL_WAIT_MS 30
    int timeout = timeout_ms/(TCP_POLL_WAIT_MS);
    int pollcnt = 0;
    int listen_timeout = -1;
    char hostname[1024],proto[1024],path[1024];
    char portstr[10];

    int rcvbuf_oldlen=0;
    int rcvbuf_newlen=0;
    int rcvbuf_len=0;
    int datalen=0;
    int dnsfree = 1;
    int idx = 0;
    int ip_string = 0;
	int dnscache_en =am_getconfig_int_def("libplayer.tcp.dnscache", 0);

    if (dns_cache_clear) {
        dns_cache_reset();
        dns_cache_clear = 0;
    }

    av_log(h, AV_LOG_INFO, "tcp_open begin, flags=0x%x, m_bconnectfail:%d.\n", flags,m_bconnectfail);
    int64_t tcp_starttime=av_gettime();
    if(h->flags & URL_LESS_WAIT) {
        timeout_ms=200; //200 ms
        timeout = timeout_ms/(TCP_POLL_WAIT_MS);
    }

    /* +[SE] [BUG][IPTV-3070][yinli.xia] added: add RRS function */
    if (rrsip_support) {
        timeout_ms = 2000;
        timeout = timeout_ms/TCP_POLL_WAIT_MS;
        av_log(h, AV_LOG_INFO,"rrsip set timeout 2000ms");
    }

    av_url_split(proto, sizeof(proto), NULL, 0, hostname, sizeof(hostname),
        &port, path, sizeof(path), uri);
    if (strcmp(proto, "tcp"))
        return AVERROR(EINVAL);
    if (port <= 0 || port >= 65536) {
        av_log(h, AV_LOG_ERROR, "Port missing in uri\n");
        return AVERROR(EINVAL);
    }
    p = strchr(uri, '?');
    if (p) {
        if (av_find_info_tag(buf, sizeof(buf), "listen", p))
            listen_socket = 1;
        if (av_find_info_tag(buf, sizeof(buf), "timeout", p)) {
            timeout_ms = strtol(buf, NULL, 10);
            if(timeout_ms < 1000)/*Must >1S ,if not think S unit*/
                timeout = timeout_ms*1000/TCP_POLL_WAIT_MS;
            av_log(h, AV_LOG_INFO,"get timeout %d ms\n",timeout *TCP_POLL_WAIT_MS );
        }

        if (av_find_info_tag(buf, sizeof(buf), "listen_timeout", p)) {
            listen_timeout = strtol(buf, NULL, 10);
        }

        if (av_find_info_tag(buf, sizeof(buf), "rcvbuf_size", p)) {
            rcvbuf_len = strtol(buf, NULL, 10);
        }
    }
    memset(&hints, 0, sizeof(hints));
    if(am_getconfig_bool_def("media.libplayer.ipv4only",1))
    		hints.ai_family = AF_INET;
    else
		hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    snprintf(portstr, sizeof(portstr), "%d", port);

redns:
    dnsfree = 1;
    idx = 0;
	av_log(h, AV_LOG_INFO,"tcp will get address from dns! listen_socket=%d\n", listen_socket);
    if (listen_socket)
        hints.ai_flags |= AI_PASSIVE;
    if (!hostname[0])
        ret = getaddrinfo(NULL, portstr, &hints, &ai);
    else {
        if (0 == dnscache_en) {
           ret = getaddrinfo(hostname, portstr, &hints, &ai);
           dns_cache_reset();
           if (0 == ret) {
              if ((idx = dns_cache_add(hostname, strlen( hostname ), ai, port )) > 0) {
                  dnsfree = 0;
                  av_log(h, AV_LOG_INFO,"disable cache, add %s to cache list, index:%d.\n",hostname, idx);
              } else {
                  av_log(h, AV_LOG_INFO,"disable cache, dns_cache_add failed.\n");
                  return -1;
              }
		   } else {
                av_log(h, AV_LOG_INFO,"disable cache, getaddrinfo failed.\n");
                return -1;
           }
        } else if ((idx = dns_kick(h, hostname, strlen(hostname), port)) != -1) {
            if (idx == 0) {
                ret = getaddrinfo(hostname, portstr, &hints, &ai);
                if (ret == 0 && m_nUsedNum < DNS_CACHE_NUM_MAX) {
                    if ((idx = dns_cache_add(hostname, strlen(hostname), ai, port)) > 0) {
                        dnsfree = 0;
                        av_log(h, AV_LOG_INFO,"add %s to cache list\n",hostname);
                    }
                }
			} else if (m_sDNSCache[idx-1].ipv4_num + m_sDNSCache[idx-1].ipv6_num == 0) {
			    dns_cache_del(idx-1);
				av_log(NULL, AV_LOG_INFO, "address %s has no available ip, delet it and retry", m_sDNSCache[idx-1].acDomainServer);
				goto redns;
            } else {
                ai = m_sDNSCache[idx - 1].ai;
                ret = 0;
                dnsfree = 0;
            }
        } else {

            //ret = getaddrinfo(hostname, portstr, &hints, &ai);
            /* +[SE] [BUG][BUG-168197][yanan.wang] added:fix BesTV get stuck over 6s When exiting*/
            int get_addr_info = am_getconfig_int_def("libplayer.tcp.get_addr_info", 0);
            av_log(h, AV_LOG_INFO,"dns_kick failed, getaddrinfo:%d.\n",get_addr_info);
            if (get_addr_info == 1) {
                memset(m_addrinfo.addr_hostname, 0, 1024);
                memcpy(m_addrinfo.addr_hostname, hostname, sizeof(hostname));
                memset(m_addrinfo.addr_portstr, 0, 10);
                memcpy(m_addrinfo.addr_portstr, portstr, sizeof(portstr));
                m_addrinfo.addr_hints = hints;
                pthread_t star_location;
                int tmp = 0;
                tmp = pthread_create(&star_location, NULL,(void *)thread_fun, &m_addrinfo);
                pthread_setname_np(star_location, "Getaddrinfo");
                if (tmp != 0) {
                    av_log(NULL, AV_LOG_ERROR,"creat thread error!\n");
                    return AVERROR(EIO);
                }
                while (m_addrinfo.thread_exitflag == 0) {
                    if (url_interrupt_cb()) {
                        av_log(NULL, NULL,"url_interrupt_cb!\n");
                        return AVERROR(EIO);
                    }
                    usleep(1000);
                }
                ret = m_addrinfo.getaddr_result;
                ai = m_addrinfo.addr_ai;
            }else {
                ret = getaddrinfo(hostname, portstr, &hints, &ai);
            }
            ip_string = 1;
            cur_ai = ai;
            av_log(h, AV_LOG_INFO,"IP is string.\n");
        }
    }

    if (ret) {
        av_log(h, AV_LOG_ERROR,
               "Failed to resolve hostname %s: %s\n",
               hostname, gai_strerror(ret));
        return AVERROR(EIO);
    }
    int cur_dns_num = 0;
    int dns_num = 0;

    //type: 0 ,first; 1, last; 2, random; 3 , value from other set
    int get_dns_type = am_getconfig_int_def("libplayer.tcp.get_dns_type", 0);
    //priority: 0:ipv6; 1:ipv4
    int get_ip_priority = am_getconfig_int_def("libplayer.tcp.get_ip_priority", 0);
	/* +[SE] [BUG][IPTV-2867][zhizhong.zhang] added:change to other dns if first failed*/
	if (flags & URL_CHANGE_DNS_IP)
		get_dns_type = 2;

    int ip_index = -1;
    int switcher = 0;
    int ipv6_prevselflag_device = 0; //have try ipv6 previous flag for once, device level
    int ipv6_prevselflag_tcpopen = 0; //have try ipv6 previous flag for continue, tcpopen level
	if (m_bconnectfail) {
		m_bconnectfail = 0;
		int dnsidx, ipidx;
		if (ip_kick(m_cLastHostName, m_cLastIPString, m_nLastIsIpv6, &dnsidx, &ipidx)) {
			ip_cache_del(dnsidx, m_nLastIsIpv6, ipidx);
			av_log(h, AV_LOG_INFO,"delete failed ip,dnsidx:%d,ipidx:%d.\n", dnsidx,ipidx);
		}
	}

    if (0 == get_ip_priority) {
       ip_index = ip_cache_select(idx-1, 1, get_dns_type, &cur_ai);
       if (ip_index == -1) {
           switcher = 1;
           ip_index = ip_cache_select(idx-1, 0, get_dns_type, &cur_ai);
       }
    } else {
       ip_index = ip_cache_select(idx-1, 0, get_dns_type, &cur_ai);
       if (ip_index == -1) {
           switcher = 1;
           ip_index = ip_cache_select(idx-1, 1, get_dns_type, &cur_ai);
       }
    }


    av_log(h, AV_LOG_INFO,"resolved %s:%d's ipaddress, flags=0x%x, ai=%p,ip_priority:%d,ip_index:%d,ipv6_num:%d,ipv4_num:%d.\n",
        hostname, port, flags, cur_ai,get_ip_priority,ip_index,m_sDNSCache[idx - 1].ipv6_num,m_sDNSCache[idx - 1].ipv4_num);
    /*cur_ai = ai;
    for (debug_cur_ai = ai; debug_cur_ai != NULL; debug_cur_ai = debug_cur_ai->ai_next) {
        dns_num++;
    }
    if (dns_num != 0) {
        if (get_dns_type == 0) {
            cur_dns_num = 1;*/
/* +[SE] [BUG][IPTV-2867][zhizhong.zhang] added:change to other dns if first failed*/
       /*     if (flags & URL_CHANGE_DNS_IP) {
                cur_dns_num = av_get_random_seed() % dns_num + 1;
                av_log(NULL, NULL, "first failed use random cur_dns_num=%d\n",
                        cur_dns_num);
            }
        } else if (get_dns_type == 1) {
            cur_dns_num = dns_num;
        } else if (get_dns_type == 2) {
            cur_dns_num = av_get_random_seed() % dns_num + 1;
        } else {
            cur_dns_num = 1;
        }
    }

    av_log(h, AV_LOG_INFO, "%s, type=%d, cur_dns_num=%d, dns_num=%d\n", __FUNCTION__, get_dns_type, cur_dns_num, dns_num);

    dns_num = 0;


    for (debug_cur_ai = ai; debug_cur_ai != NULL; debug_cur_ai = debug_cur_ai->ai_next) {
        addr = (struct sockaddr_in *)debug_cur_ai->ai_addr;
        dns_num++;
        av_log(h, AV_LOG_INFO,"current server ip:%s, cur_dns_num=%d, dns_num=%d\n",
            print_ai_addr(cur_ai, acIp, sizeof(acIp)), cur_dns_num, dns_num);
        if (cur_dns_num == dns_num) {
            cur_ai = debug_cur_ai;

            /*if (!listen_socket && addr->sin_addr.s_addr == INADDR_ANY) {
                ++cur_dns_num;
                av_log(h, AV_LOG_INFO, "skip INADDR_ANY address, addr flag:%x, cur_dns_num:%d ",
                       debug_cur_ai->ai_flags, cur_dns_num);
            }
        }
        memset(acIp, 0x0, sizeof(acIp));
   }
	*/
	if (ip_index == -1 && ip_string == 0) {
		av_log(h, AV_LOG_INFO,"ip_index -1 ,return.\n");
		return  -1;
	}
	char acIp[256];
	memset(acIp, 0x0, sizeof(acIp));
	/*for (debug_cur_ai = ai; debug_cur_ai != NULL; debug_cur_ai = debug_cur_ai->ai_next) {
		av_log(h, AV_LOG_INFO,"current server ip:%s, debug_cur_ai:%p,cur_dns_num=%d, dns_num=%d\n",
            print_ai_addr(debug_cur_ai, acIp, sizeof(acIp)), debug_cur_ai,cur_dns_num, dns_num);
	}*/

    addr = (struct sockaddr_in *)cur_ai->ai_addr;

 restart:
    av_log(h, AV_LOG_INFO,"tcp open use ip:%s, flags=0x%x, cur_dns_num=%d, dns_num=%d\n",
        print_ai_addr(cur_ai, acIp, sizeof(acIp)), flags, cur_dns_num, dns_num);
     if (cur_ai->ai_family == AF_INET6 && m_sDNSCache[idx - 1].ipv4_num > 0) {
        av_log(NULL, NULL, "disv6 func ipv4_num=%d\n", m_sDNSCache[idx - 1].ipv4_num);
        if (ipv6once_devicelvl_enable == 1) {
            if (m_nDeviceIpv6SelectCtrl == 1) {
                ipv6_prevselflag_device = 1;
                av_log(h, AV_LOG_INFO, "deviceipv6once enable try ipv6 fistly ipv6_prevselflag_device =%d, m_nDeviceIpv6SelectCtrl =%d.\n",ipv6_prevselflag_device,m_nDeviceIpv6SelectCtrl);
            } else {
                av_log(h, AV_LOG_INFO, "deviceipv6once enable not try ipv6 ipv6_prevselflag_device =%d, m_nDeviceIpv6SelectCtrl =%d.\n",ipv6_prevselflag_device,m_nDeviceIpv6SelectCtrl);
                av_log(NULL, NULL, "ret = %d\n", ret);
                ret = AVERROR(EIO);
                av_log(NULL, NULL, "0 force ret EIO=0x%x\n", ret);
                goto fail;
            }
        }
        if (ipv6cont_tcpopenlvl_enable == 0) {
            if (ipv6_prevselflag_tcpopen == 1 && tcpopen_ipv6select_ctrl == 1) {
                tcpopen_ipv6select_ctrl = 0;
            }
            if (tcpopen_ipv6select_ctrl == 1) {
                ipv6_prevselflag_tcpopen = 1;
                av_log(h, AV_LOG_INFO, "tcpopenipv6cont disable try ipv6 fistly ipv6cont_tcpopenlvl_enable =%d, tcpopen_ipv6select_ctrl =%d.\n",ipv6_prevselflag_tcpopen,tcpopen_ipv6select_ctrl);
            } else {
                av_log(h, AV_LOG_INFO, "tcpopenipv6cont disable not try ipv6 fail ipv6cont_tcpopenlvl_enable =%d, tcpopen_ipv6select_ctrl =%d.\n",ipv6_prevselflag_tcpopen,tcpopen_ipv6select_ctrl);
                av_log(NULL, NULL, "ret = %d\n", ret);
                ret = AVERROR(EIO);
                av_log(NULL, NULL, "1 force ret EIO=0x%x\n", ret);
                goto fail;
            }
        }
    }
    ret = AVERROR(EIO);
    int64_t tcp_connect = av_gettime();
    fd = socket(cur_ai->ai_family, cur_ai->ai_socktype, cur_ai->ai_protocol);
    if (fd < 0) {
        av_log(NULL, NULL, "connect fail fd=%d\n",fd);
        goto fail;
    }

    if (listen_socket) {
        int fd1;
        int reuse = 1;
        struct pollfd lp = { fd, POLLIN, 0 };
        setsockopt(fd, SOL_SOCKET, SO_REUSEADDR, &reuse, sizeof(reuse));
        ret = bind(fd, cur_ai->ai_addr, cur_ai->ai_addrlen);
        if (ret) {
            ret = ff_neterrno();
            goto fail1;
        }
        ret = listen(fd, 1);
        if (ret) {
            ret = ff_neterrno();
            goto fail1;
        }
        ret = poll(&lp, 1, listen_timeout >= 0 ? listen_timeout : -1);
        if (ret <= 0) {
            ret = AVERROR(ETIMEDOUT);
            goto fail1;
        }
        fd1 = accept(fd, NULL, NULL);
        if (fd1 < 0) {
            ret = ff_neterrno();
            goto fail1;
        }
        closesocket(fd);
        fd = fd1;
        ff_socket_nonblock(fd, 1);
    } else {
 redo:
        if(rcvbuf_len > 0){
            datalen=sizeof(int);
            if( getsockopt( fd, SOL_SOCKET, SO_RCVBUF, (void *)&rcvbuf_oldlen, &datalen ) < 0 ){
                av_log(h, AV_LOG_WARNING, "getsockopt(SO_RECVBUF): oldlen %s\n", strerror(errno));
            }

            rcvbuf_len = rcvbuf_len * 1024;
            if (setsockopt(fd, SOL_SOCKET, SO_RCVBUF, (void *)&rcvbuf_len, sizeof(rcvbuf_len)) < 0) {
                av_log(h, AV_LOG_WARNING, "setsockopt(SO_RECVBUF): %s\n", strerror(errno));
            }

            datalen=sizeof(int);
            if( getsockopt( fd, SOL_SOCKET, SO_RCVBUF, (void *)&rcvbuf_newlen, &datalen ) < 0 ){
                av_log(h, AV_LOG_WARNING, "getsockopt(SO_RECVBUF): oldlen %s\n", strerror(errno));
            }

            av_log(h, AV_LOG_WARNING, "set recv buf oldlen=%d len=%d newlen=%d \n",rcvbuf_oldlen,rcvbuf_len,rcvbuf_newlen);
        }

        ff_socket_nonblock(fd, 1);
        ret = connect(fd, cur_ai->ai_addr, cur_ai->ai_addrlen);
    }

    if (ret < 0) {
        struct pollfd p = {fd, POLLOUT, 0};
        ret = ff_neterrno();
        if (ret == AVERROR(EINTR)) {
            if (url_interrupt_cb()) {
                ret = AVERROR_EXIT;
                goto fail1;
            }

            av_log(h, AV_LOG_ERROR, "connect system call is Interrupted \n");
            goto redo;
        }
        if (ret != AVERROR(EINPROGRESS) &&
            ret != AVERROR(EAGAIN))
            goto fail;

        /* wait until we are connected or until abort */
        pollcnt = timeout;
        while(pollcnt--) {
            if (url_interrupt_cb()) {
                ret = AVERROR_EXIT;
                goto fail1;
            }
            ret = poll(&p, 1, TCP_POLL_WAIT_MS);
            if (ret > 0)
                break;
        }
        if (ret <= 0) {
            memset(acIp, 0x0, sizeof(acIp));
            av_log(h, AV_LOG_ERROR,
                   "TCP connection to %s timeout failed!,cost time:%dus\n",
                   print_ai_addr(cur_ai, acIp, sizeof(acIp)), (int)(av_gettime()-tcp_connect));
            ret = AVERROR(ETIMEDOUT);
            goto fail;
        }
        /* test error */
        optlen = sizeof(ret);
        if (getsockopt (fd, SOL_SOCKET, SO_ERROR, &ret, &optlen))
            ret = AVUNERROR(ff_neterrno());
        if (ret != 0) {
            char errbuf[100];
            ret = AVERROR(ret);
            av_strerror(ret, errbuf, sizeof(errbuf));
            av_log(h, AV_LOG_ERROR,
                   "TCP connection to %s:%d failed: %s, ret = %d\n",
                   hostname, port, errbuf, ret);
            ret = AVERROR(ret);
            if(ret>0)
                ret = AVERROR(EIO);
            goto fail;
        }
    }
    if (ipv6_prevselflag_device == 1 && cur_ai->ai_family != AF_INET6 && m_nDeviceIpv6SelectCtrl != 0) {
        m_nDeviceIpv6SelectCtrl = 0;
        av_log(h, AV_LOG_INFO, "deviceipv6once enable tcp connect ok m_nDeviceIpv6SelectCtrl =%d\n",m_nDeviceIpv6SelectCtrl);
    }
    av_log(h, AV_LOG_INFO,"tcp  connect %s ok!, ai=%p, cur_dns_num=%d\n",hostname, ai, cur_dns_num);
    s = av_malloc(sizeof(TCPContext));
    if (!s) {
        if (dnsfree == 1)
            freeaddrinfo(ai);
        return AVERROR(ENOMEM);
    }
    h->priv_data = s;
    h->is_streamed = 1;
    s->fd = fd;
    if (dnsfree == 1)
        freeaddrinfo(ai);
    av_log(h, AV_LOG_INFO,"tcp  connect %s used %d us, cur_dns_num=%d\n",hostname,(int)(av_gettime()-tcp_starttime), cur_dns_num);
    return 0;

 fail:
    //if (cur_ai->ai_next) {
        /* Retry with the next sockaddr
        cur_ai = cur_ai->ai_next;
        addr = (struct sockaddr_in *)cur_ai->ai_addr;
        memset(acIp, 0x0, sizeof(acIp));
        av_log(h, AV_LOG_INFO,"switch server ip:%s\n", print_ai_addr(cur_ai, acIp, sizeof(acIp)));
        if (fd >= 0)
            closesocket(fd);
        goto restart*/;
	if (0 == get_ip_priority) {
		if (m_sDNSCache[idx - 1].ipv6_num > 0)
	       ip_cache_del(idx-1,1,ip_index);
	    else if (m_sDNSCache[idx - 1].ipv4_num > 0)
		   ip_cache_del(idx-1,0,ip_index);

		if (m_sDNSCache[idx - 1].ipv6_num == 0 && m_sDNSCache[idx - 1].ipv4_num == 0) {
		   av_log(h, AV_LOG_INFO, "all ip cache are tried, also failed.\n");
		   dns_cache_del(idx-1);
		   goto fail1;
		}
	    if (m_sDNSCache[idx - 1].ipv6_num > 0)
           ip_index = ip_cache_select(idx-1, 1, get_dns_type, &cur_ai);
	    else if (m_sDNSCache[idx - 1].ipv4_num > 0)
           ip_index = ip_cache_select(idx-1, 0, get_dns_type, &cur_ai);
		av_log(h, AV_LOG_INFO, "+++ ip index:%d,ipv6_num:%d,ipv4_num:%d.\n",ip_index,m_sDNSCache[idx - 1].ipv6_num,m_sDNSCache[idx - 1].ipv4_num);
	} else {
		if (m_sDNSCache[idx - 1].ipv4_num > 0)
	       ip_cache_del(idx-1,0,ip_index);
	    else if (m_sDNSCache[idx - 1].ipv6_num > 0)
		   ip_cache_del(idx-1,1,ip_index);
		if (m_sDNSCache[idx - 1].ipv6_num == 0 && m_sDNSCache[idx - 1].ipv4_num == 0) {
		   av_log(h, AV_LOG_INFO, "all ip cache are tried, also failed.\n");
		   dns_cache_del(idx-1);
		   goto fail1;
		}
	    if (m_sDNSCache[idx - 1].ipv4_num > 0)
           ip_index = ip_cache_select(idx-1, 0, get_dns_type, &cur_ai);
	    else if (m_sDNSCache[idx - 1].ipv6_num > 0)
           ip_index = ip_cache_select(idx-1, 1, get_dns_type, &cur_ai);
		av_log(h, AV_LOG_INFO, "--- ip index:%d,ipv6_num:%d,ipv4_num:%d.\n",ip_index,m_sDNSCache[idx - 1].ipv6_num,m_sDNSCache[idx - 1].ipv4_num);
	}
	if (fd >= 0)
       closesocket(fd);
    goto restart;
    /*} else {
        if (dnsfree == 0) {
            if (idx > 0) {
                dns_cache_del(idx-1);
                goto fail1;
            }
        }
	}*/
 fail1:
 	if (ipv6_prevselflag_device == 1 && m_nDeviceIpv6SelectCtrl != 0) {
        m_nDeviceIpv6SelectCtrl = 0;
        av_log(h, AV_LOG_INFO, "deviceipv6once enable tcp connect fail m_nDeviceIpv6SelectCtrl =%d\n",m_nDeviceIpv6SelectCtrl);
  }
    av_log(h, AV_LOG_INFO, "tcp connect fail.\n");
    if (fd >= 0)
        closesocket(fd);
    if (dnsfree == 1)
        freeaddrinfo(ai);
    av_log(NULL, NULL, "tcp_open ret=%d\n",ret);
    return ret;
}

static int tcp_read(URLContext *h, uint8_t *buf, int size)
{
    TCPContext *s = h->priv_data;
    int ret;
    int maxwait_ms=h->flags&URL_LESS_WAIT?300:1000;

    //av_log(h, AV_LOG_INFO, "---tcp_read: h->flags=0x%x, AVIO_FLAG_NONBLOCK=0x%x, maxwait_ms=%d--\n", h->flags, AVIO_FLAG_NONBLOCK, maxwait_ms);

    if (!(h->flags & AVIO_FLAG_NONBLOCK)) {
        //av_log(h, AV_LOG_INFO, "---tcp_read: ff_network_wait_fd_wait_max--\n");
        ret = ff_network_wait_fd_wait_max(s->fd, 0,0,maxwait_ms);
        if (ret < 0){
            av_log(NULL, AV_LOG_INFO,"ff_network_wait_fd return error %d,errmsg:%s \n",ret,strerror(errno)!=NULL?strerror(errno):"unkown");
            return ret;

        }
    }
    ret = recv(s->fd, buf, size, 0);
    if(ret<=0){
        av_log(NULL, AV_LOG_INFO,"tcp_read return error %d,errno:%d,errmsg:%s \n",ret,errno,strerror(errno)!=NULL?strerror(errno):"unkown");
    }
    return ret < 0 ? ff_neterrno() : ret;
}

static int tcp_write(URLContext *h, const uint8_t *buf, int size)
{
    TCPContext *s = h->priv_data;
    int ret;

    if (!(h->flags & AVIO_FLAG_NONBLOCK)) {
        ret = ff_network_wait_fd_wait(s->fd,1,30);
        if (ret < 0)
            return ret;
    }
    ret = send(s->fd, buf, size, 0);
    return ret < 0 ? ff_neterrno() : ret;
}

static int tcp_close(URLContext *h)
{
    TCPContext *s = h->priv_data;
    closesocket(s->fd);
    av_free(s);
    return 0;
}

static int tcp_get_file_handle(URLContext *h)
{
    TCPContext *s = h->priv_data;
    return s->fd;
}

URLProtocol ff_tcp_protocol = {
    .name                = "tcp",
    .url_open            = tcp_open,
    .url_read            = tcp_read,
    .url_write           = tcp_write,
    .url_close           = tcp_close,
    .url_get_file_handle = tcp_get_file_handle,
};
