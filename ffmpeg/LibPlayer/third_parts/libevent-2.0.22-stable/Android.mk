LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_ARM_MODE := arm
LIB_SRC := \
	event.c \
	evthread.c \
	buffer.c \
	bufferevent.c \
	bufferevent_filter.c \
	bufferevent_pair.c \
	listener.c \
	bufferevent_ratelim.c \
	evmap.c \
	log.c \
	evutil.c \
	evutil_rand.c \
	select.c \
	poll.c \
	epoll.c \
	signal.c \
	event_tagging.c \
	http.c \
	evdns.c \
	evrpc.c \
	bufferevent_sock.c\
	dns-ffmpeg.c

LOCAL_SRC_FILES := $(LIB_SRC)
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)	\
	$(LOCAL_PATH)/include \
	$(LOCAL_PATH)/compat \
	$(LOCAL_PATH)/ARM-Code \

LOCAL_SHARED_LIBRARIES := libutils liblog libdl
LOCAL_MODULE    := libevent
LOCAL_MODULE_TAGS := tests
include $(BUILD_SHARED_LIBRARY)


include $(CLEAR_VARS)
LOCAL_ARM_MODE := arm
LIB_SRC := \
	event.c \
	evthread.c \
	buffer.c \
	bufferevent.c \
	bufferevent_filter.c \
	bufferevent_pair.c \
	listener.c \
	bufferevent_ratelim.c \
	evmap.c \
	log.c \
	evutil.c \
	evutil_rand.c \
	select.c \
	poll.c \
	epoll.c \
	signal.c \
	event_tagging.c \
	http.c \
	evdns.c \
	evrpc.c \
	bufferevent_sock.c\
	dns-ffmpeg.c

LOCAL_SRC_FILES := $(LIB_SRC)
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)	\
	$(LOCAL_PATH)/include \
	$(LOCAL_PATH)/compat \
	$(LOCAL_PATH)/ARM-Code \

LOCAL_MODULE    := libevent-stable
LOCAL_MODULE_TAGS := tests
include $(BUILD_STATIC_LIBRARY)