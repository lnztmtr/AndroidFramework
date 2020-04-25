LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES:= AmThumbnail.cpp

LOCAL_C_INCLUDES:= \
    $(TOP)/frameworks/av/include \
    $(LOCAL_PATH)/include \
    $(TOP)/external/ffmpeg \

LOCAL_SHARED_LIBRARIES := \
    libcutils \
    libamffmpeg \
    libmedia \
    libutils

LOCAL_CFLAGS := -D__STDC_CONSTANT_MACROS # For stdint macros used in FFmpeg.

LOCAL_MODULE_TAGS := optional
LOCAL_ARM_MODE := arm
LOCAL_PRELINK_MODULE := false

LOCAL_MODULE:= libamthumbnail

include $(BUILD_SHARED_LIBRARY)
