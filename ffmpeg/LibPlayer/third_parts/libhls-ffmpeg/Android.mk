LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)


LOCAL_ARM_MODE := arm
LOCAL_PRELINK_MODULE := false

LOCAL_SRC_FILES := $(notdir $(wildcard $(LOCAL_PATH)/*.c)) 		

LOCAL_C_INCLUDES := $(LOCAL_PATH)/ \
        $(LOCAL_PATH)/../../amavutils/include \
        $(LOCAL_PATH)/../../amffmpeg  

LOCAL_MODULE_TAGS := optional

LOCAL_CFLAGS += -Wno-multichar

#LOCAL_STATIC_LIBRARIES :=
LOCAL_SHARED_LIBRARIES :=libamplayer libcutils libssl libamavutils libcrypto

LOCAL_SHARED_LIBRARIES +=libdl 
LOCAL_MODULE := libhls_ffmpeg_mod
LOCAL_MODULE_PATH:=$(TARGET_OUT_SHARED_LIBRARIES)/amplayer
include $(BUILD_SHARED_LIBRARY)
