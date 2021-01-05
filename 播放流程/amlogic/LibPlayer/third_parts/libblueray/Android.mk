LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE := libbluray.so
LOCAL_MODULE_TAGS := optional
LOCAL_IS_HOST_MODULE := true

LOCAL_SRC_FILES := libbluray.so
LOCAL_MODULE_PATH:=$(TARGET_OUT)/lib

include $(BUILD_PREBUILT) 

include $(CLEAR_VARS)

LOCAL_MODULE_CLASS := SHARED_LIBRARIES
LOCAL_MODULE := libbluray_mod.so
LOCAL_MODULE_TAGS := optional
LOCAL_IS_HOST_MODULE := true

LOCAL_SRC_FILES := libbluray_mod.so
LOCAL_MODULE_PATH:=$(TARGET_OUT)/lib/amplayer

include $(BUILD_PREBUILT) 
