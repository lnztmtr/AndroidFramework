/*
  This example code shows how to use the high-level, low-level, and
  server-level interfaces of evdns.

  XXX It's pretty ugly and should probably be cleaned up.
 */

#include <event2/event-config.h>

/* Compatibility for possible missing IPv6 declarations */
#include "../ipv6-internal.h"

#include <sys/types.h>

#ifdef WIN32
#include <winsock2.h>
#include <ws2tcpip.h>
#else
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#endif

#include "event2/dns.h"
#include "event2/dns_struct.h"
#include "event2/dns_compat.h"
#include "event2/util.h"
#include "event2/event.h"
#include "event2/event_struct.h"
#include "event2/thread.h"

#include "event2/bufferevent.h"
#include "event2/bufferevent_struct.h"
#include "bufferevent-internal.h"

#include "defer-internal.h"
#include "log-internal.h"
#include "mm-internal.h"
#include "strlcpy-internal.h"
#include "ipv6-internal.h"
#include "util-internal.h"
#include "evthread-internal.h"



#ifdef _EVENT_HAVE_NETINET_IN6_H
#include <netinet/in6.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <android/log.h>  
#define  LOG_TAG    "libevent"  
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)  

#define u32 ev_uint32_t
#define u8 ev_uint8_t
static int pending = 0;
struct gaic_request_status {
	int magic;
	struct event_base *base;
	struct evdns_base *dns_base;
	struct evdns_getaddrinfo_request *request;
	struct event cancel_event;
	int canceled;
        char* ip;
        int dns_ok;
};

#define GAIC_MAGIC 0x1234abcd

static const char *
debug_ntoa(u32 address)
{
	static char buf[32];
	u32 a = ntohl(address);
	evutil_snprintf(buf, sizeof(buf), "%d.%d.%d.%d",
					(int)(u8)((a>>24)&0xff),
					(int)(u8)((a>>16)&0xff),
					(int)(u8)((a>>8 )&0xff),
					(int)(u8)((a	)&0xff));
	return buf;
}

static void
main_callback(int result, char type, int count, int ttl,
			  void *addrs, void *orig) {
	char *n = (char*)orig;
	int i;
	for (i = 0; i < count; ++i) {
		if (type == DNS_IPv4_A) {
			printf("%s: %s\n", n, debug_ntoa(((u32*)addrs)[i]));
		} else if (type == DNS_PTR) {
			printf("%s: %s\n", n, ((char**)addrs)[i]);
		}
	}
	if (!count) {
		printf("%s: No answer (%d)\n", n, result);
	}
	fflush(stdout);
}

static void
gai_callback(int err, struct evutil_addrinfo *ai, void *arg)
{
	const char *name = arg;
	int i;
	if (err) {
		printf("%s: %s\n", name, evutil_gai_strerror(err));
	}
	if (ai && ai->ai_canonname)
		printf("    %s ==> %s\n", name, ai->ai_canonname);
	for (i=0; ai; ai = ai->ai_next, ++i) {
		char buf[128];
		if (ai->ai_family == PF_INET) {
			struct sockaddr_in *sin =
			    (struct sockaddr_in*)ai->ai_addr;
			evutil_inet_ntop(AF_INET, &sin->sin_addr, buf,
			    sizeof(buf));
			printf("[%d] %s: %s\n",i,name,buf);
		} else {
			struct sockaddr_in6 *sin6 =
			    (struct sockaddr_in6*)ai->ai_addr;
			evutil_inet_ntop(AF_INET6, &sin6->sin6_addr, buf,
			    sizeof(buf));
			printf("[%d] %s: %s\n",i,name,buf);
		}
	}
}

static void
evdns_server_callback(struct evdns_server_request *req, void *data)
{
	int i, r;
	(void)data;
	/* dummy; give 192.168.11.11 as an answer for all A questions,
	 *	give foo.bar.example.com as an answer for all PTR questions. */
	for (i = 0; i < req->nquestions; ++i) {
		u32 ans = htonl(0xc0a80b0bUL);
		if (req->questions[i]->type == EVDNS_TYPE_A &&
		    req->questions[i]->dns_question_class == EVDNS_CLASS_INET) {
			printf(" -- replying for %s (A)\n", req->questions[i]->name);
			r = evdns_server_request_add_a_reply(req, req->questions[i]->name,
										  1, &ans, 10);
			if (r<0)
				printf("eeep, didn't work.\n");
		} else if (req->questions[i]->type == EVDNS_TYPE_PTR &&
		    req->questions[i]->dns_question_class == EVDNS_CLASS_INET) {
			printf(" -- replying for %s (PTR)\n", req->questions[i]->name);
			r = evdns_server_request_add_ptr_reply(req, NULL, req->questions[i]->name,
											"foo.bar.example.com", 10);
			if (r<0)
				printf("ugh, no luck");
		} else {
			printf(" -- skipping %s [%d %d]\n", req->questions[i]->name,
				   req->questions[i]->type, req->questions[i]->dns_question_class);
		}
	}

	r = evdns_server_request_respond(req, 0);
	if (r<0)
		printf("eeek, couldn't send reply.\n");
}

static int verbose = 0;

static void
logfn(int is_warn, const char *msg) {
	if (!is_warn && !verbose)
		return;
	fprintf(stderr, "%s: %s\n", is_warn?"WARN":"INFO", msg);
}


static void
gaic_getaddrinfo_cb(int result, struct evutil_addrinfo *res, void *arg)
{
	struct gaic_request_status *status = arg;
	struct event_base *base = status->base;
	//tt_assert(status->magic == GAIC_MAGIC);
	int i;
        int getaddr = 0;
        char buf[128] = {0};
	if (result) {
		LOGI(": %s\n", evutil_gai_strerror(result));
	}
	if (res && res->ai_canonname)
		LOGI("  ==> %s\n", res->ai_canonname);
	for (i=0; res; res = res->ai_next, ++i) {
		
		if (res->ai_family == PF_INET) {
			struct sockaddr_in *sin =
			    (struct sockaddr_in*)res->ai_addr;
			evutil_inet_ntop(AF_INET, &sin->sin_addr, buf,
			    sizeof(buf));
                        status->dns_ok= 1;
			LOGI("[%d] %s\n",i,buf);
		} else {
			struct sockaddr_in6 *sin6 =
			    (struct sockaddr_in6*)res->ai_addr;
			evutil_inet_ntop(AF_INET6, &sin6->sin6_addr, buf,
			    sizeof(buf));
                        status->dns_ok = 1;
			LOGI("[%d]  %s\n",i,buf);
		}
	}

	if (result == EVUTIL_EAI_CANCEL) {
		//tt_assert(status->canceled);
	}
        if(status->dns_ok){
            strcpy(status->ip,buf);
        }
        LOGI("gaic_getaddrinfo_cb...ret:%d\n",status->dns_ok);
        event_del(&status->cancel_event);

	//memset(status, 0xf0, sizeof(*status));
	//free(status);

end:
	if (--pending <= 0)
		event_base_loopexit(base, NULL);
}

static void
gaic_cancel_request_cb(evutil_socket_t fd, short what, void *arg)
{
	struct gaic_request_status *status = arg;

	if(status->magic != GAIC_MAGIC)
            LOGI( "magic error...\n");
	status->canceled = 1;
        LOGI( "dns timeout gaic_cancel_request_cb...\n");
	evdns_getaddrinfo_cancel(status->request);
	return;
end:
	event_base_loopexit(status->base, NULL);
}

static void
gaic_launch(struct gaic_request_status *status,char*ptr)
{

	event_assign(&status->cancel_event, status->base, -1, 0, gaic_cancel_request_cb,
	    status);
        struct evutil_addrinfo hints;
    	struct timeval tv = { 3, 0 };
        memset(&hints, 0, sizeof(hints));
        hints.ai_family = PF_INET;
        hints.ai_protocol = IPPROTO_TCP;
        hints.ai_flags = EVUTIL_AI_CANONNAME;
	status->request = evdns_getaddrinfo(status->dns_base,
	    ptr, NULL, &hints, gaic_getaddrinfo_cb,
	    status);
	event_add(&status->cancel_event, &tv);
	++pending;
}
/*getaddrinfo async can cancel timeout 3s*/
int getaddrinfo_async_cancel(char*ptr,char* ip)
{
	struct event_base *base;
	struct evdns_base *dns_base = NULL;
	struct evdns_server_port *server = NULL;
	evutil_socket_t fd = -1;
	struct sockaddr_in sin;
	struct sockaddr_storage ss;
	ev_socklen_t slen;
	int i;
        int ret = 0;

	base = event_base_new();
	dns_base = evdns_base_new(base, 1);
	struct gaic_request_status *status = calloc(1,sizeof(*status));

	status->magic = GAIC_MAGIC;
	status->base = base;
	status->dns_base = dns_base;
        status->ip = ip;
        status->dns_ok = 0;


	gaic_launch(status, ptr);
	
	event_base_dispatch(base);
end:
        ret = status->dns_ok==1?0:-1;
    	memset(status, 0xf0, sizeof(*status));
	free(status);
	if (dns_base)
		evdns_base_free(dns_base, 1);
        return ret;
}
#if 0
int
main(int c, char **v) {
	int idx;
	int reverse = 0, servertest = 0, use_getaddrinfo = 0;
	struct event_base *event_base = NULL;
	struct evdns_base *evdns_base = NULL;
	const char *resolv_conf = NULL;
	if (c<2) {
		fprintf(stderr, "syntax: %s [-x] [-v] [-c resolv.conf] hostname\n", v[0]);
		fprintf(stderr, "syntax: %s [-servertest]\n", v[0]);
		return 1;
	}
	idx = 1;
	while (idx < c && v[idx][0] == '-') {
		if (!strcmp(v[idx], "-x"))
			reverse = 1;
		else if (!strcmp(v[idx], "-v"))
			verbose = 1;
		else if (!strcmp(v[idx], "-g"))
			use_getaddrinfo = 1;
		else if (!strcmp(v[idx], "-servertest"))
			servertest = 1;
		else if (!strcmp(v[idx], "-c")) {
			if (idx + 1 < c)
				resolv_conf = v[++idx];
			else
				fprintf(stderr, "-c needs an argument\n");
		} else
			fprintf(stderr, "Unknown option %s\n", v[idx]);
		++idx;
	}

#ifdef WIN32
	{
		WSADATA WSAData;
		WSAStartup(0x101, &WSAData);
	}
#endif
        evdns_set_log_fn(logfn);
    fprintf(stderr, "resolving %s...\n",v[idx]);

    test_getaddrinfo_async_cancel(v[idx]);

#if 0
	event_base = event_base_new();
	evdns_base = evdns_base_new(event_base, 1);
	

	if (servertest) {
		evutil_socket_t sock;
		struct sockaddr_in my_addr;
		sock = socket(PF_INET, SOCK_DGRAM, 0);
		if (sock == -1) {
			perror("socket");
			exit(1);
		}
		evutil_make_socket_nonblocking(sock);
		my_addr.sin_family = AF_INET;
		my_addr.sin_port = htons(10053);
		my_addr.sin_addr.s_addr = INADDR_ANY;
		if (bind(sock, (struct sockaddr*)&my_addr, sizeof(my_addr))<0) {
			perror("bind");
			exit(1);
		}
		evdns_add_server_port_with_base(event_base, sock, 0, evdns_server_callback, NULL);
	}
	if (idx < c) {
		int res;
#ifdef WIN32
		if (resolv_conf == NULL)
			res = evdns_base_config_windows_nameservers(evdns_base);
		else
#endif
			res = evdns_base_resolv_conf_parse(evdns_base,
			    DNS_OPTION_NAMESERVERS,
			    resolv_conf ? resolv_conf : "/etc/resolv.conf");
                fprintf(stderr, "evdns_base_resolv_conf_parse");
		if (res < 0) {
			fprintf(stderr, "Couldn't configure nameservers");
			return 1;
		}
	}

	printf("EVUTIL_AI_CANONNAME in example = %d\n", EVUTIL_AI_CANONNAME);
	for (; idx < c; ++idx) {
		if (reverse) {
			struct in_addr addr;
			if (evutil_inet_pton(AF_INET, v[idx], &addr)!=1) {
				fprintf(stderr, "Skipping non-IP %s\n", v[idx]);
				continue;
			}
			fprintf(stderr, "resolving %s...\n",v[idx]);
			evdns_base_resolve_reverse(evdns_base, &addr, 0, main_callback, v[idx]);
		} else if (use_getaddrinfo) {
			struct evutil_addrinfo hints;
			memset(&hints, 0, sizeof(hints));
                        hints.ai_family = PF_UNSPEC;
			hints.ai_protocol = IPPROTO_TCP;
			hints.ai_flags = EVUTIL_AI_CANONNAME;
			fprintf(stderr, "resolving (fwd)0 %s...\n",v[idx]);
			evdns_getaddrinfo(evdns_base, v[idx], NULL, &hints,
			    gai_callback, v[idx]);
		} else {
			fprintf(stderr, "resolving (fwd)1 %s...\n",v[idx]);
			evdns_base_resolve_ipv4(evdns_base, v[idx], 0, main_callback, v[idx]);
		}
	}
	fflush(stdout);
        fprintf(stderr, "exit..\n");
	event_base_dispatch(event_base);
    #endif
            fprintf(stderr, "exit..\n");
	return 0;
}
#endif
