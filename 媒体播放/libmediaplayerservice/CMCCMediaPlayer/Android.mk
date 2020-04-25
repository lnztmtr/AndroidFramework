LOCAL_PATH := $(call my-dir)

CMCCMEDIA_RELEASE := $(LOCAL_PATH)
ifeq ($(CMCCPLAYER_VERSION),1.1.16.11)
    include $(CMCCMEDIA_RELEASE)/1.1.16.11/Android4.4_libs/Android.mk
else
    include $(CMCCMEDIA_RELEASE)/Android4.4_libs/Android.mk 
endif
