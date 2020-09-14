LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := dns-example 
LOCAL_MODULE_TAGS := samples
LOCAL_ARM_MODE := arm
LIB_SRC := \
	dns-example.c
LOCAL_SRC_FILES := $(LIB_SRC)
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/../	\
	$(LOCAL_PATH)/../include \
	$(LOCAL_PATH)/../compat \
	$(LOCAL_PATH)/../ARM-Code \

LOCAL_STATIC_LIBRARIES :=libevent-stable
LOCAL_SHARED_LIBRARIES := libutils liblog libdl
include $(BUILD_EXECUTABLE)
