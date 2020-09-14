LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm
LOCAL_MODULE_TAGS := optional

ifeq ($(LIVEPLAY_SEEK), true)
 LOCAL_CFLAGS += -DLIVEPLAY_SEEK
endif

LOCAL_SRC_FILES :=  hls_m3uparser.c \
	hls_m3ulivesession.c \
	hls_fifo.c \
	hls_simple_cache.c

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/../common \
    $(LOCAL_PATH)/../include \
    $(LOCAL_PATH)/../downloader \
    $(LOCAL_PATH)/../../../amffmpeg \
    $(LOCAL_PATH)/../../../amavutils/include

ifneq (0, $(shell expr $(PLATFORM_SDK_VERSION) \>= 23))
    LOCAL_C_INCLUDES += $(TOP)/external/boringssl/src/include
else
    LOCAL_C_INCLUDES += $(TOP)/external/openssl/include
endif


ifeq ($(BUILD_WITH_VIEWRIGHT_WEB), true)
 LOCAL_CFLAGS += -DENABLE_VIEWRIGHT_WEB
endif

LOCAL_SHARED_LIBRARIES +=libdl libamavutils
LOCAL_MODULE := libhls

ifeq ($(TARGET_ARCH),arm)
    LOCAL_CFLAGS += -Wno-psabi
endif
include $(BUILD_STATIC_LIBRARY)
