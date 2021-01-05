LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
include $(LOCAL_PATH)/../common.mk


LOCAL_SRC_FILES := $(FFFILES)
LOCAL_C_INCLUDES :=		\
	$(LOCAL_PATH)		\
	$(LOCAL_PATH)/..	\
	$(LOCAL_PATH)/../../amavutils/include/	\
	$(LOCAL_PATH)/../../third_parts/rtmpdump	\
	$(LOCAL_PATH)/../../third_parts/udrm	\
	$(LOCAL_PATH)/../../amplayer/player/include/ \
	$(LOCAL_PATH)/../../amcodec/include/ \
	external/zlib
LOCAL_CFLAGS += $(FFCFLAGS)
LOCAL_CFLAGS += -DANDROID_PLATFORM_SDK_VERSION=$(PLATFORM_SDK_VERSION)
LOCAL_MODULE := $(FFNAME)
#//begin: add by ysten wenglei at 20200526: 广东直播超清频道切屏黑屏
ifeq ($(strip $(PRODUCT_PROVINCE_TYPE)),CM201_GUANGDONG)
LOCAL_CFLAGS += -DPROVINCE_TYPE_CM201_GUANGDONG
endif
#//end: add by ysten wenglei at 20200526: 广东直播超清频道切屏黑屏
#//begin: add by ysten wenglei at 20200514: 广东直播超清频道切屏黑屏
ifeq ($(strip $(PRODUCT_PROVINCE_TYPE)),CM201_GUANGDONG_ZHUOYING)
LOCAL_CFLAGS += -DPROVINCE_TYPE_CM201_GUANGDONG_ZHUOYING
endif
#//end: add by ysten wenglei at 20200514: 广东直播超清频道切屏黑屏
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
include $(LOCAL_PATH)/../common.mk
LOCAL_SRC_FILES := $(FFFILES)
LOCAL_C_INCLUDES :=		\
	$(LOCAL_PATH)		\
	$(LOCAL_PATH)/..	\
	$(LOCAL_PATH)/../../amavutils/include/	\
	$(LOCAL_PATH)/../../third_parts/rtmpdump	\
	$(LOCAL_PATH)/../../third_parts/udrm	\
	$(LOCAL_PATH)/../../amplayer/player/include/ \
	$(LOCAL_PATH)/../../amcodec/include/ \
	external/zlib
LOCAL_CFLAGS += $(FFCFLAGS)
LOCAL_CFLAGS += -DANDROID_PLATFORM_SDK_VERSION=$(PLATFORM_SDK_VERSION)
LOCAL_MODULE := $(FFNAME)
LOCAL_SHARED_LIBRARIES += librtmp  libutils libmedia libz libbinder libdl libcutils libc libavutil libavcodec libamavutils libudrm
LOCAL_PRELINK_MODULE := false
LOCAL_MODULE_TAGS := optional
#//begin: add by ysten wenglei at 20200526: 广东直播超清频道切屏黑屏
ifeq ($(strip $(PRODUCT_PROVINCE_TYPE)),CM201_GUANGDONG)
LOCAL_CFLAGS += -DPROVINCE_TYPE_CM201_GUANGDONG
endif
#//end: add by ysten wenglei at 20200526: 广东直播超清频道切屏黑屏
#//begin: add by ysten wenglei at 20200514: 广东直播超清频道切屏黑屏
ifeq ($(strip $(PRODUCT_PROVINCE_TYPE)),CM201_GUANGDONG_ZHUOYING)
LOCAL_CFLAGS += -DPROVINCE_TYPE_CM201_GUANGDONG_ZHUOYING
endif
#//end: add by ysten wenglei at 20200514: 广东直播超清频道切屏黑屏
include $(BUILD_SHARED_LIBRARY)

# Reset CC as it's overwritten by common.mk
CC := $(HOST_CC)
