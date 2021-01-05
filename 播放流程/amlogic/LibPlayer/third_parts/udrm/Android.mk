LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm

LOCAL_C_INCLUDES:= \
    $(LOCAL_PATH)/libudrm \
    $(LOCAL_PATH)/../../amavutils/include/

LOCAL_SRC_FILES := udrm.c

LOCAL_SHARED_LIBRARIES += libcutils libutils libc libudrm2w2_Android libamavutils

LOCAL_MODULE_TAGS:= optional
LOCAL_MODULE := libudrm
#LOCAL_MODULE_PATH:=$(TARGET_OUT_SHARED_LIBRARIES)/amplayer

include $(BUILD_SHARED_LIBRARY)
include $(call all-makefiles-under,$(LOCAL_PATH))
