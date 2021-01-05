LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm
LOCAL_MODULE_TAGS := optional

ifeq ($(LIVEPLAY_SEEK), true)
 LOCAL_CFLAGS += -DLIVEPLAY_SEEK
endif

LOCAL_SRC_FILES := \
	hls_utils.c\
	hls_rand.c
	
LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/../../../amavutils/include \
    $(LOCAL_PATH)/


ifneq (0, $(shell expr $(PLATFORM_SDK_VERSION) \>= 23))
    LOCAL_C_INCLUDES += $(TOP)/external/boringssl/src/include
else
    LOCAL_C_INCLUDES += $(TOP)/external/openssl/include
endif


ifeq ($(TARGET_ARCH),arm)
    LOCAL_CFLAGS += -Wno-psabi
endif

LOCAL_MODULE := libhls_common

include $(BUILD_STATIC_LIBRARY)
