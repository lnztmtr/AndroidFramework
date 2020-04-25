# Copyright (C) 2011 Amlogic Inc
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#
# This file is the build configuration for a full Android
# build for Meson reference board.
#

# for China mobile version
ifeq ($(PROJECT_TYPE),shmobile)
CHINA_MOBILE_ENABLE := true
BUILD_SH_MOBILE_APKS := true
TARGET_NO_USE_SYSWRITE := true
BUILD_MOBILE_MIGU_BOOTANIMATION := false
else ifeq ($(PROJECT_TYPE),jsmobile)
CHINA_MOBILE_ENABLE := true
BUILD_JS_MOBILE_APKS := true
TARGET_NO_USE_SYSWRITE := false
else ifeq ($(PROJECT_TYPE),scmobile)
DEFENV_IN_FACTORY_RESET := true
DISABLE_BUILD_UTC_CHECK := true
BUILD_SC_MOBILE_APKS := true
TARGET_NO_USE_SYSWRITE := false
else ifeq ($(PROJECT_TYPE),jxmobile)
BUILD_JX_MOBILE_APKS := true
TARGET_NO_USE_SYSWRITE := false
else ifeq ($(PROJECT_TYPE),ynmobile)
BUILD_YN_MOBILE_APKS := true
TARGET_NO_USE_SYSWRITE := false
else ifeq ($(PROJECT_TYPE),lnmobile)
BUILD_LN_MOBILE_APKS := true
TARGET_NO_USE_SYSWRITE := false
else ifeq ($(PROJECT_TYPE),sdmobile)
BUILD_SD_MOBILE_APKS := true
TARGET_NO_USE_SYSWRITE := false
endif

# for China telecom version
ifeq ($(PROJECT_TYPE),telecom)
CHINA_TELECOM_ENABLE := true
TARGET_NO_USE_SYSWRITE := false
endif

# for China unicom version
ifeq ($(PROJECT_TYPE),unicom)
CHINA_UNICOM_ENABLE := true
TARGET_NO_USE_SYSWRITE := false
endif

# Inherit from those products. Most specific first.
$(call inherit-product-if-exists, vendor/google/products/gms.mk)
$(call inherit-product, device/amlogic/common/mbx_amlogic.mk)
#$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base.mk)
$(call inherit-product, device/amlogic/p201_iptv/device.mk)

# add by ysten
$(call inherit-product, sjos/build/sjos.mk)
$(call inherit-product-if-exists, $(LOCAL_PATH)/../../../ysten_custom/ysten_custom.mk)
BOARD_AUTO_COLLECT_MANIFEST := false

# Replace definitions used by tablet in mid_amlogic.mk above
# Overrides
PRODUCT_BRAND := MBX
PRODUCT_DEVICE := Android Reference Device
PRODUCT_NAME := Android Reference Design
PRODUCT_CHARACTERISTICS := mbx

NUM_FRAMEBUFFER_SURFACE_BUFFERS := 3
TARGET_USE_TRIPLE_FB_BUFFERS := true
PRODUCT_PROPERTY_OVERRIDES += \
        sys.fb.bits=32

PRODUCT_NAME := p201_iptv
PRODUCT_DEVICE := p201_iptv
PRODUCT_MODEL := p201_iptv
PRODUCT_BRAND := Android
PRODUCT_MANUFACTURER := amlogic

WITH_LIBPLAYER_MODULE := false

ifeq ($(PROJECT_ID),p211)
	P211_IPTV_ENABLE := true
else ifeq ($(PROJECT_ID),p261)
	P261_IPTV_ENABLE := true
endif

# for China mobile version

ifeq ($(BUILD_SH_MOBILE_APKS), true)
PRODUCT_MODEL := p201_iptv
ifdef MOBILE_UNIT_TYPE
	PRODUCT_MODEL:=$(MOBILE_UNIT_TYPE)
endif
endif

#china mobile
ifeq ($(CHINA_MOBILE_ENABLE), true)
TARGET_BUILD_WIPE_CACHE := true
DISABLE_BUILD_UTC_CHECK := true
LIVEPLAY_SEEK := true
DEFENV_IN_FACTORY_RESET := true
USB_BURN := true
CHINA_MOBILE_PPPOE := true
CUSTOMER_SERIALNO_MAC := true
NO_ORIGINAL_APKS := true
UNUSE_SCREEN_MODE := true
RECORD_LOG := true
TARGET_NO_USE_SYSWRITE := true
endif

#China telecom
ifeq ($(CHINA_TELECOM_ENABLE), true)
DEFENV_IN_FACTORY_RESET := true
DISABLE_BUILD_UTC_CHECK := true
BUILD_CHINA_TELECOM_JICAI_APKS := true
CTC_MEDIAPLAYER_ENABLE := true
BUILD_SH_TELECOM_APKS := false
BUILD_GD_TELECOM_APKS := false
BUILD_SC_TELECOM_APKS := false
BUILD_JX_TELECOM_APKS := false
BUILD_YM3_TELECOM_APKS := false
TARGET_NO_USE_SYSWRITE := false
#ShangHai telecom jicai
ifeq ($(BUILD_CHINA_TELECOM_JICAI_APKS), true)
BUILD_SY_APKS := true
BUILD_IPANEL_APKS := false
TELECOM_JICAI_STBID_SN := true
TARGET_NO_USE_SYSWRITE := true
UISOURCE_1080P_SUPPORT := true
TARGET_SUPPORT_TELECOM_CTC_IMG := true
TARGET_SUPPORT_TELECOM_CTC_OTA_IMG := false
TARGET_AMLOGIC_TELECOM_CTC := $(TARGET_PRODUCT_DIR)/telecom_ctc

#ShangHai telecom
else ifeq ($(BUILD_SH_TELECOM_APKS), true)
BUILD_WITH_VLAN := true
BUILD_SY_APKS := true
TARGET_NO_USE_SYSWRITE := false

#GuangDong telecom
else ifeq ($(BUILD_GD_TELECOM_APKS), true)
BUILD_SY_APKS := true
BUILD_IPANEL_APKS := false
TARGET_NO_USE_SYSWRITE := false

#SiChuan telecom
else ifeq ($(BUILD_SC_TELECOM_APKS), true)
BUILD_SY_APKS := true
BUILD_IPANEL_APKS := false
TARGET_NO_USE_SYSWRITE := false

#JiangXi telecom
else ifeq ($(BUILD_JX_TELECOM_APKS), true)
BUILD_SY_APKS := true
TARGET_NO_USE_SYSWRITE := false

#Yue me teltecom
else ifeq ($(BUILD_YM3_TELECOM_APKS), true)
UISOURCE_1080P_SUPPORT := true
CUSTOMER_STBID := true

else
BUILD_CHINA_TELECOM_JICAI_APKS := true
BUILD_SY_APKS := true
BUILD_IPANEL_APKS := false
TELECOM_JICAI_STBID_SN := true
TARGET_NO_USE_SYSWRITE := false
UISOURCE_1080P_SUPPORT := true
endif
endif

#China unicom
ifeq ($(CHINA_UNICOM_ENABLE), true)
DEFENV_IN_FACTORY_RESET := true
DISABLE_BUILD_UTC_CHECK := true
BUILD_BJ_UNICOM_JICAI_APKS := true
BUILD_SD_UNICOM := false
TARGET_NO_USE_SYSWRITE := false
endif

#BeiJing unicom jicai
ifeq ($(BUILD_BJ_UNICOM_JICAI_APKS), true)
      ifeq ($(LICENCE_TAG), suying)
          BUILD_SY_APKS := true
      else ifeq ($(LICENCE_TAG), ipanel)
          BUILD_IPANEL_APKS := true
      endif
UISOURCE_1080P_SUPPORT := true
endif
ifeq ($(BUILD_CHINA_TELECOM_JICAI_APKS), true)
  TARGET_RECOVERY_BACKUP_RECOVERY := true
endif
ifeq ($(CHINA_MOBILE_ENABLE),true)
  BUILD_PACKAGE_BOOTLOADER := true
else
  BUILD_PACKAGE_BOOTLOADER := true
endif

#########Support compiling out encrypted zip/aml_upgrade_package.img directly
#PRODUCT_BUILD_SECURE_BOOT_IMAGE_DIRECTLY := true
ifeq ($(P211_IPTV_ENABLE),true)
PRODUCT_AML_SECUREBOOT_USERKEY := ./uboot/board/amlogic/gxl_p211_v1/aml-user-key.sig
PRODUCT_AML_SECUREBOOT_SIGNTOOL := ./uboot/fip/gxl/aml_encrypt_gxl
PRODUCT_AML_SECUREBOOT_SIGNBOOTLOADER := $(PRODUCT_AML_SECUREBOOT_SIGNTOOL) --bootsig \
						--amluserkey $(PRODUCT_AML_SECUREBOOT_USERKEY) \
						--aeskey enable
PRODUCT_AML_SECUREBOOT_SIGNIMAGE := $(PRODUCT_AML_SECUREBOOT_SIGNTOOL) --imgsig \
					--amluserkey $(PRODUCT_AML_SECUREBOOT_USERKEY)
else ifeq ($(P261_IPTV_ENABLE),true)
PRODUCT_AML_SECUREBOOT_USERKEY := ./uboot/board/amlogic/gxlx_p261_v1/aml-user-key.sig
PRODUCT_AML_SECUREBOOT_SIGNTOOL := ./uboot/fip/gxl/aml_encrypt_gxl
PRODUCT_AML_SECUREBOOT_SIGNBOOTLOADER := $(PRODUCT_AML_SECUREBOOT_SIGNTOOL) --bootsig \
						--amluserkey $(PRODUCT_AML_SECUREBOOT_USERKEY) \
						--aeskey enable
PRODUCT_AML_SECUREBOOT_SIGNIMAGE := $(PRODUCT_AML_SECUREBOOT_SIGNTOOL) --imgsig \
					--amluserkey $(PRODUCT_AML_SECUREBOOT_USERKEY)
else
PRODUCT_AML_SECUREBOOT_USERKEY := ./uboot/board/amlogic/gxb_p201_v1/aml-user-key.sig
PRODUCT_AML_SECUREBOOT_SIGNTOOL := ./uboot/fip/gxb/aml_encrypt_gxb
PRODUCT_AML_SECUREBOOT_SIGNBOOTLOADER := $(PRODUCT_AML_SECUREBOOT_SIGNTOOL) --bootsig \
						--amluserkey $(PRODUCT_AML_SECUREBOOT_USERKEY) \
						--aeskey enable
PRODUCT_AML_SECUREBOOT_SIGNIMAGE := $(PRODUCT_AML_SECUREBOOT_SIGNTOOL) --imgsig \
					--amluserkey $(PRODUCT_AML_SECUREBOOT_USERKEY)
endif


#########################################################################
#
#                                                Dm-Verity
#
#########################################################################
#BUILD_WITH_DM_VERITY := true
#TARGET_USE_SECURITY_DM_VERITY_MODE_WITH_TOOL := true
ifeq ($(TARGET_USE_SECURITY_DM_VERITY_MODE_WITH_TOOL),true)
BUILD_WITH_DM_VERITY := true
endif #ifeq ($(TARGET_USE_SECURITY_DM_VERITY_MODE_WITH_TOOL),true)

ifeq ($(BUILD_WITH_DM_VERITY),true)
PRODUCT_PACKAGES += \
	pem2mincrypt \
	genverityimg \
	mkverity \
	gentable \
	gen_verity_key \
	img2simg_host
ifneq ($(TARGET_USE_SECURITY_DM_VERITY_MODE_WITH_TOOL),true)
PRODUCT_COPY_FILES += \
	$(LOCAL_PATH)/verity_key:root/verity_key \
	$(LOCAL_PATH)/verity-key.pem:verity-key.pem \
	$(LOCAL_PATH)/verity-key.pub:verity-key.pub
endif # ifneq ($(TARGET_USE_SECURITY_DM_VERITY_MODE_WITH_TOOL),true)
ifeq ($(BUILD_CHINA_TELECOM_JICAI_APKS),true)
PRODUCT_COPY_FILES += \
    device/amlogic/p201_iptv/fstab.verity.amlogic:root/fstab.system.amlogic \
    device/amlogic/p201_iptv/fstab.data.amlogic:root/fstab.data.amlogic \
    device/amlogic/p201_iptv/fstab.chinatelecom.other.amlogic:root/fstab.other.amlogic
else
PRODUCT_COPY_FILES += \
    device/amlogic/p201_iptv/fstab.verity.amlogic:root/fstab.system.amlogic \
    device/amlogic/p201_iptv/fstab.data.amlogic:root/fstab.data.amlogic \
    device/amlogic/p201_iptv/fstab.other.amlogic:root/fstab.other.amlogic
endif
else #following not for dm-verify
ifeq ($(BUILD_CHINA_TELECOM_JICAI_APKS),true)
PRODUCT_COPY_FILES += \
    device/amlogic/p201_iptv/fstab.chinatelecom.system.amlogic:root/fstab.system.amlogic \
    device/amlogic/p201_iptv/fstab.chinatelecom.data.amlogic:root/fstab.data.amlogic \
    device/amlogic/p201_iptv/fstab.chinatelecom.other.amlogic:root/fstab.other.amlogic 
else
PRODUCT_COPY_FILES += \
    device/amlogic/p201_iptv/fstab.system.amlogic:root/fstab.system.amlogic \
    device/amlogic/p201_iptv/fstab.data.amlogic:root/fstab.data.amlogic \
    device/amlogic/p201_iptv/fstab.other.amlogic:root/fstab.other.amlogic
endif
endif # ifeq ($(BUILD_WITH_DM_VERITY),true)

#########################################################################
#
#                                                WiFi
#
#########################################################################

#WIFI_MODULE := rtl8189es
MULTI_WIFI_SUPPORT := true
include device/amlogic/common/wifi.mk

# Change this to match target country
# 11 North America; 14 Japan; 13 rest of world
#PRODUCT_DEFAULT_WIFI_CHANNELS := 11


#########################################################################
#
#                                                Bluetooth
#
#########################################################################

BOARD_HAVE_BLUETOOTH := true

# we need define BLUETOOTH_MODULE to one of the following vendor module according to your project,
# especially for realtek bluetooth, you can define any one from rtl8761 rtl8723bs rtl8723ds rtl8822bs rtl8723bu,
# such as rtl8723bu, also support rtl8761,rtl8822bs,rtl8723bs module
BLUETOOTH_MODULE := rtl8822bs
#BLUETOOTH_MODULE := rtl8723bu
#BLUETOOTH_MODULE := CSR8510
#BLUETOOTH_MODULE := mt7668

ifeq ($(BLUETOOTH_MODULE),CSR8510)
PRODUCT_PROPERTY_OVERRIDES += \
    bt.board.enable=false
else ifeq ($(BLUETOOTH_MODULE),rtl8723bu)
PRODUCT_PROPERTY_OVERRIDES += \
    bt.board.enable=false
else
PRODUCT_PROPERTY_OVERRIDES += \
    bt.board.enable=true
endif

include device/amlogic/common/bluetooth.mk


#########################################################################
#
#                                                ConsumerIr
#
#########################################################################

#PRODUCT_PACKAGES += \
#    consumerir.amlogic \
#    SmartRemote
#PRODUCT_COPY_FILES += \
#    frameworks/native/data/etc/android.hardware.consumerir.xml:system/etc/permissions/android.hardware.consumerir.xml


#PRODUCT_PACKAGES += libbt-vendor

ifeq ($(SUPPORT_HDMIIN),true)
PRODUCT_PACKAGES += \
    libhdmiin \
    HdmiIn
endif

#PRODUCT_COPY_FILES += \
#    frameworks/native/data/etc/android.hardware.ethernet.xml:system/etc/permissions/android.hardware.ethernet.xml

# Audio
#
BOARD_ALSA_AUDIO=tiny
BOARD_USE_USB_AUDIO := true

#IFLYTEK_SUPPORTED := true
ifeq ($(IFLYTEK_SUPPORTED), true)
    AUDIO_CAPTURE_SUPPORTED := true
endif

include device/amlogic/common/audio.mk

#########################################################################
#
#                                                Camera
#
#########################################################################

PRODUCT_COPY_FILES += \
    frameworks/native/data/etc/android.hardware.camera.front.xml:system/etc/permissions/android.hardware.camera.front.xml

#########################################################################
#
#                                                Software features
#
#########################################################################

BUILD_WITH_AMLOGIC_PLAYER := true
BUILD_WITH_APP_OPTIMIZATION := true
#BUILD_WITH_WIDEVINE_DRM := true
#BUILD_WITH_PLAYREADY_DRM := true
#BUILD_WITH_EREADER := true 
BUILD_WITH_MIRACAST := true
#BUILD_WITH_THIRDPART_APK := true
BUILD_WITH_BOOT_PLAYER:= true
BUILD_AMVIDEO_CAPTURE_TEST:=false
ifeq ($(wildcard vendor/google/products/gms.mk),)
# facelock enable, board should have front camera
BUILD_WITH_FACE_UNLOCK := true
endif

#########################################################################
#
#                                                Instaboot
#
#########################################################################

BOARD_SUPPORT_INSTABOOT := false

BUILD_WITH_SAMBA := true
ifeq ($(BUILD_WITH_SAMBA),true)
PRODUCT_PACKAGES += \
    libsmbbase \
    libsmbmnt \
    smb.conf \
    smbproxyd \
    smbc \
    smbtree \
    tdbdump \
    tdbtool \
    msgtest \
    smbpasswd \
    smbcontrol \
    pdbedit \
    pdbtest \
    nmblookup \
    smbclient \
    smbstatus \
    smbd \
    nmbd
PRODUCT_PROPERTY_OVERRIDES += \
    ro.platform.has.samba=true
endif
include device/amlogic/common/software.mk

ifeq ($(BOARD_SUPPORT_INSTABOOT),true)
local_instaboot := $(strip $(wildcard $(LOCAL_PATH)/instabootserver))
ifneq ($(local_instaboot),)
PRODUCT_COPY_FILES += \
  $(LOCAL_PATH)/instabootserver:system/bin/instabootserver
endif
local_instaboot_xml := $(strip $(wildcard $(LOCAL_PATH)/instaboot_config.xml))
ifneq ($(local_instaboot_xml),)
PRODUCT_COPY_FILES += \
  $(LOCAL_PATH)/instaboot_config.xml:system/etc/instaboot_config.xml
endif
endif

#########################################################################
#
#                                                Security mode
#
#########################################################################
ifeq ($(BUILD_SC_TELECOM_APKS), true)
TARGET_USE_SECURITY_MODE := true
endif

#TARGET_USE_SECURITY_MODE := true
ifeq ($(TARGET_USE_SECURITY_MODE), true)
PRODUCT_PACKAGES += \
	libtelecom_secure_api
endif

#TARGET_USE_OPTEEOS := true

#########################################################################
#
#                                                PlayReady DRM
#
#########################################################################
#BUILD_WITH_PLAYREADY_DRM := true
ifeq ($(BUILD_WITH_PLAYREADY_DRM), true)
#playready license process in smoothstreaming(default)
BOARD_PLAYREADY_LP_IN_SS := true
#BOARD_PLAYREADY_TVP := true
endif

#########################################################################
#
#                                                Verimatrix DRM
##########################################################################
#verimatrix web
BUILD_WITH_VIEWRIGHT_WEB := false
#verimatrix stb
BUILD_WITH_VIEWRIGHT_STB := false
#########################################################################


#DRM Widevine
BOARD_WIDEVINE_OEMCRYPTO_LEVEL := 3

$(call inherit-product, $(LOCAL_PATH)/media.mk)

#########################################################################
#
#                                                Languages
#
#########################################################################

# For all locales, $(call inherit-product, build/target/product/languages_full.mk)
PRODUCT_LOCALES := zh_CN en_AU en_US en_IN fr_FR it_IT es_ES et_EE de_DE nl_NL cs_CZ pl_PL ja_JP \
  zh_TW zh_HK ru_RU ko_KR nb_NO es_US da_DK el_GR tr_TR pt_PT pt_BR rm_CH sv_SE bg_BG \
  ca_ES en_GB fi_FI hi_IN hr_HR hu_HU in_ID iw_IL lt_LT lv_LV ro_RO sk_SK sl_SI sr_RS uk_UA \
  vi_VN tl_PH ar_EG fa_IR th_TH sw_TZ ms_MY af_ZA zu_ZA am_ET hi_IN en_XA ar_XB fr_CA km_KH \
  lo_LA ne_NP si_LK mn_MN hy_AM az_AZ ka_GE my_MM mr_IN ml_IN is_IS mk_MK ky_KG eu_ES gl_ES \
  bn_BD ta_IN kn_IN te_IN uz_UZ ur_PK kk_KZ

#################################################################################
#
#                                                PPPOE
#
#################################################################################
#ifeq ($(CHINA_MOBILE_ENABLE),true)
PRODUCT_PACKAGES += \
    libcbpppoejni \
    pppoe_wrapper \
    pppoe \
	rp-pppoe \
    amlogic.pppoe \
    amlogic.pppoe.xml
PRODUCT_PROPERTY_OVERRIDES += \
    ro.platform.has.pppoe=true
#else
#BUILD_WITH_PPPOE := true

#ifeq ($(BUILD_WITH_PPPOE),true)
#PRODUCT_PACKAGES += \
#    PPPoE \
#    libpppoejni \
#    libpppoe \
#    pppoe_wrapper \
#    pppoe \
#    droidlogic.frameworks.pppoe \
#    droidlogic.external.pppoe \
#    droidlogic.external.pppoe.xml
#PRODUCT_PROPERTY_OVERRIDES += \
#    ro.platform.has.pppoe=true
#endif
#endif

#################################################################################
#
#                                                DEFAULT LOWMEMORYKILLER CONFIG
#
#################################################################################
BUILD_WITH_LOWMEM_COMMON_CONFIG := true

# MediaCodec lowlatency support
PRODUCT_PROPERTY_OVERRIDES += \
    ro.media.lowlatency=true

ifeq ($(strip $(CHINA_MOBILE_ENABLE)),true)
EPG_ENABLE := true

PRODUCT_PACKAGES += \
	SubTitle \
	Samba \
	smbd\
	libsmbbase \
	libsmbmnt \
	remotecfg \
	RC_Server \
	Discovery.apk \
	IpRemote.apk \
	DLNA \
	AmlMiracast \
	PromptUser \
	libasound \
	alsalib-alsaconf \
	alsalib-pcmdefaultconf \
	alsalib-cardsaliasesconf \
	libamstreaming \
	bootplayer \
	dhcpcd \
	configserver \
	libcastscreen \
	PicturePlayer
ifeq ($(strip $(BUILD_JS_MOBILE_APKS)),true)
PRODUCT_PACKAGES += \
	Settings \
	RemoteIME \
	FileBrowser \
	VideoPlayer \
	Music
endif

ifeq ($(strip $(BUILD_SH_MOBILE_APKS)),true)
PRODUCT_PACKAGES += \
	PicturePlayer
endif

#ifneq ($(filter TAG READER ALL, $(NFC_SUPPORT_TYPE)),)
 #   include device/amlogic/common/mobile_migu.mk
#else
 #   include device/amlogic/common/mobile.mk
#endif

else ifeq ($(strip $(BUILD_SC_MOBILE_APKS)),true)
PRODUCT_PACKAGES += \
	MboxSetting \
	Settings \
	FileBrowser \
	SubTitle \
	RemoteIME \
	remotecfg \
	DLNA \
	OTAUpgrade \
	RC_Server \
	AmlMiracast \
	Discovery.apk \
	IpRemote.apk \
	PromptUser \
	libasound \
	alsalib-alsaconf \
	alsalib-pcmdefaultconf \
	alsalib-cardsaliasesconf \
	libamstreaming \
	VideoPlayer \
	Music \
	PicturePlayer \
	configserver \
	dhcpcd \
	libCTC_MediaProcessor

include device/amlogic/common/mobile.mk

else ifeq ($(strip $(BUILD_SD_MOBILE_APKS)),true)
PRODUCT_PACKAGES += \
	MboxSetting \
	FileBrowser \
	SubTitle \
	RemoteIME \
	remotecfg \
	DLNA \
	OTAUpgrade \
	RC_Server \
	AmlMiracast \
	Discovery.apk \
	IpRemote.apk \
	PromptUser \
	libasound \
	alsalib-alsaconf \
	alsalib-pcmdefaultconf \
	alsalib-cardsaliasesconf \
	libamstreaming \
	VideoPlayer \
	Music \
	PicturePlayer \
	configserver \
	dhcpcd \
	libCTC_MediaProcessor

include device/amlogic/common/mobile.mk

else ifeq ($(strip $(BUILD_JX_MOBILE_APKS)),true)
PRODUCT_PACKAGES += \
	MboxSetting \
	Settings \
	FileBrowser \
	SubTitle \
	RemoteIME \
	remotecfg \
	DLNA \
	OTAUpgrade \
	RC_Server \
	AmlMiracast \
	Discovery.apk \
	IpRemote.apk \
	PromptUser \
	libasound \
	alsalib-alsaconf \
	alsalib-pcmdefaultconf \
	alsalib-cardsaliasesconf \
	libamstreaming \
	VideoPlayer \
	Music \
	PicturePlayer \
	configserver \
	dhcpcd \
	libCTC_MediaProcessor

include device/amlogic/common/mobile.mk

else ifeq ($(strip $(BUILD_YN_MOBILE_APKS)),true)
PRODUCT_PACKAGES += \
	Settings \
	FileBrowser \
	SubTitle \
	RemoteIME \
	remotecfg \
	VideoPlayer \
	configserver

include device/amlogic/common/mobile.mk

else ifeq ($(strip $(BUILD_LN_MOBILE_APKS)),true)
PRODUCT_PACKAGES += \
	Settings \
	FileBrowser \
	SubTitle \
	RemoteIME \
	remotecfg \
	VideoPlayer \
	configserver

include device/amlogic/common/mobile.mk

else ifeq ($(strip $(CHINA_TELECOM_ENABLE)),true)

#ifeq ($(BUILD_SH_TELECOM_APKS)$(BUILD_GD_TELECOM_APKS)$(BUILD_SC_TELECOM_APKS)$(BUILD_CHINA_TELECOM_JICAI_APKS),falsefalsefalsefalse)
#PRODUCT_PACKAGES += \
#	MediaBoxLauncher \
#	Settings \
#	mediaProcessorDemo
#endif

ifeq ($(BUILD_CHINA_TELECOM_JICAI_APKS),true)

ifeq ($(BUILD_SY_APKS),true)
PRODUCT_PACKAGES += \
	dhcpcd \
	NetworkService
endif

PRODUCT_PACKAGES += \
	MusicPlayer \
	amlpictureKit 
else
PRODUCT_PACKAGES += \
	dhcpcd \
	OTAUpgrade 
endif

PRODUCT_PACKAGES += \
	FileBrowser \
	SubTitle \
	RemoteIME \
	remotecfg \
	DLNA \
	RC_Server \
	AmlMiracast \
	Discovery.apk \
	IpRemote.apk \
	PromptUser \
	libasound \
	alsalib-alsaconf \
	alsalib-pcmdefaultconf \
	alsalib-cardsaliasesconf \
	libamstreaming \
	bootplayer \
	VideoPlayer \
	PicturePlayer \
	configserver \
	libCTC_MediaProcessor \
	libCTC_AmlPlayer

ifeq ($(BUILD_CHINA_TELECOM_JICAI_APKS),false)
PRODUCT_PACKAGES += \
	Music
endif

PRODUCT_COPY_FILES += \
  $(LOCAL_PATH)/IPV6/ipv6-android-script.sh:system/bin/ipv6-android-script.sh \
  $(LOCAL_PATH)/IPV6/odhcp6c:system/bin/odhcp6c

include device/amlogic/common/telecom.mk

else ifeq ($(strip $(CHINA_UNICOM_ENABLE)),true)

ifeq ($(BUILD_BJ_UNICOM_JICAI_APKS),false)
PRODUCT_PACKAGES += \
	MediaBoxLauncher
endif

ifeq ($(BUILD_BJ_UNICOM_JICAI_APKS),true)
PRODUCT_PACKAGES += \
	FileBrowser \
	UnicomNetDialog
endif

ifeq ($(strip $(BUILD_SD_UNICOM)),true)
PRODUCT_PACKAGES += \
  ftpserver \
	ftp 
endif

PRODUCT_PACKAGES += \
	SubTitle \
	RemoteIME \
	remotecfg \
	DLNA \
	MusicPlayer \
	RC_Server \
	BluetoothSet \
	AmlMiracast \
	IpRemote.apk \
	PromptUser \
	libasound \
	alsalib-alsaconf \
	alsalib-pcmdefaultconf \
	alsalib-cardsaliasesconf \
	libamstreaming \
	bootplayer \
	VideoPlayer \
	PicturePlayer \
	dhcpcd \
	configserver \
	libCTC_MediaProcessor

PRODUCT_COPY_FILES += \
  $(LOCAL_PATH)/IPV6/ipv6-android-script.sh:system/bin/ipv6-android-script.sh \
  $(LOCAL_PATH)/IPV6/odhcp6c:system/bin/odhcp6c

include device/amlogic/common/unicom.mk

else
PRODUCT_PACKAGES += \
	MediaBoxLauncher \
	MboxSetting \
	FileBrowser \
	AppInstaller \
	VideoPlayer \
	SubTitle \
	RemoteIME \
	remotecfg \
	DLNA \
	OTAUpgrade \
	RC_Server \
	AmlMiracast \
	Discovery.apk \
	IpRemote.apk \
	PromptUser \
	libasound \
	alsalib-alsaconf \
	alsalib-pcmdefaultconf \
	alsalib-cardsaliasesconf \
	libamstreaming \
	bootplayer \
	dhcpcd \
	configserver \
	libcastscreen 
endif

#for nfc build
ifeq ($(NFC_SUPPORT_TYPE), TAG)
include device/amlogic/common/nxp/nfc/nfc_tag/nfc_tag.mk
else ifeq ($(NFC_SUPPORT_TYPE), READER)
include device/amlogic/common/nxp/nfc/nfc_reader/nfc_reader.mk
else ifeq ($(NFC_SUPPORT_TYPE), ALL)
include device/amlogic/common/nxp/nfc/nfc_tag/nfc_tag.mk
include device/amlogic/common/nxp/nfc/nfc_reader/nfc_reader.mk
endif

#for tb build
include device/amlogic/common/tb_detect.mk
# CMCC karaok recorder lib
PRODUCT_PACKAGES += \
  libkaraok_recorder
  
# ESplayer for yun os game  
PRODUCT_PACKAGES += \
 	libesplayer \
	libaacdec

# for hevc enc in omx lib  
PRODUCT_PACKAGES += \
	libge2d

ifeq ($(strip $(CHINA_TELECOM_ENABLE)),true)
	ifeq ($(strip $(BUILD_CHINA_TELECOM_JICAI_APKS)),true)
		PRODUCT_COPY_FILES += \
			device/amlogic/common/init/mbx/init.shtelecom_jicai.rc:root/init.amlogic.rc
	else ifeq ($(strip $(BUILD_SH_TELECOM_APKS)),true)
		PRODUCT_COPY_FILES += \
			device/amlogic/common/init/mbx/init.shtelecom.rc:root/init.amlogic.rc
	else ifeq ($(strip $(BUILD_YM3_TELECOM_APKS)),true)
		PRODUCT_COPY_FILES += \
			device/amlogic/common/init/mbx/init.amlogic_chinatelecom_ym3.rc:root/init.amlogic.rc
	else
		PRODUCT_COPY_FILES += \
			device/amlogic/common/init/mbx/init.amlogic_chinatelecom.rc:root/init.amlogic.rc
	endif
else ifeq ($(strip $(CHINA_UNICOM_ENABLE)),true)
    ifeq ($(strip $(BUILD_SD_UNICOM)),true)
	    PRODUCT_COPY_FILES += \
		    device/amlogic/common/init/mbx/init.amlogic_sdunicom.rc:root/init.amlogic.rc \
		    device/amlogic/common/ChinaUnicom_apks/ext_bootfail.sh:system/bin/ext_bootfail.sh
    else
		PRODUCT_COPY_FILES += \
			device/amlogic/common/init/mbx/init.amlogic_chinaunicom.rc:root/init.amlogic.rc \
			device/amlogic/common/ChinaUnicom_apks/ext_bootfail.sh:system/bin/ext_bootfail.sh
	endif
else ifeq ($(strip $(BUILD_JS_MOBILE_APKS)),true)
	PRODUCT_COPY_FILES += \
		device/amlogic/common/init/mbx/init.amlogic_jsmobile.rc:root/init.amlogic.rc
else ifeq ($(strip $(BUILD_SC_MOBILE_APKS)),true)
	PRODUCT_COPY_FILES += \
		device/amlogic/common/init/mbx/init.amlogic_scmobile.rc:root/init.amlogic.rc
else ifeq ($(strip $(BUILD_JX_MOBILE_APKS)),true)
	PRODUCT_COPY_FILES += \
		device/amlogic/common/init/mbx/init.amlogic_jxmobile.rc:root/init.amlogic.rc	\
		$(LOCAL_PATH)/remote_chinamobile.conf:system/etc/remote.conf \
		$(LOCAL_PATH)/remote_chinamobile1.conf:system/etc/remote1.conf \
		$(LOCAL_PATH)/remote_chinamobile2.conf:system/etc/remote2.conf \
		$(LOCAL_PATH)/Vendor_0001_Product_0001_chinamobile_jx.kl:/system/usr/keylayout/Vendor_0001_Product_0001.kl
else ifeq ($(strip $(BUILD_SH_MOBILE_APKS)),true)
    ifeq ($(YSTEN_CUSTOM_PATCH),CM201_guangdong)
	    PRODUCT_COPY_FILES += \
		    device/amlogic/common/init/mbx/init.amlogic_gdmobile.rc:root/init.amlogic.rc
	else
	    PRODUCT_COPY_FILES += \
		    device/amlogic/common/init/mbx/init.amlogic_shmobile.rc:root/init.amlogic.rc
	endif
else ifeq ($(strip $(BUILD_YN_MOBILE_APKS)),true)
	PRODUCT_COPY_FILES += \
		device/amlogic/common/init/mbx/init.amlogic_ynmobile.rc:root/init.amlogic.rc
else ifeq ($(strip $(BUILD_LN_MOBILE_APKS)),true)
	PRODUCT_COPY_FILES += \
		device/amlogic/common/init/mbx/init.amlogic_lnmobile.rc:root/init.amlogic.rc
else ifeq ($(strip $(BUILD_SD_MOBILE_APKS)),true)
	PRODUCT_COPY_FILES +=	\
		device/amlogic/common/init/mbx/init.amlogic_sdmobile.rc:root/init.amlogic.rc
else
	PRODUCT_COPY_FILES += \
		device/amlogic/common/init/mbx/init.amlogic.rc:root/init.amlogic.rc
endif

# remote IME config file
PRODUCT_COPY_FILES += \
	$(LOCAL_PATH)/remotecfg.sh:/system/bin/remotecfg.sh

ifeq ($(strip $(CHINA_MOBILE_ENABLE)),true)
	PRODUCT_COPY_FILES += \
		$(LOCAL_PATH)/remote_chinamobile.conf:system/etc/remote.conf \
		$(LOCAL_PATH)/remote_chinamobile1.conf:system/etc/remote1.conf \
		$(LOCAL_PATH)/remote_chinamobile2.conf:system/etc/remote2.conf
	PRODUCT_COPY_FILES += \
	frameworks/native/data/etc/android.hardware.location.xml:system/etc/permissions/android.hardware.location.xml
else ifeq ($(strip $(CHINA_TELECOM_ENABLE)),true)
	ifeq ($(strip $(BUILD_SH_TELECOM_APKS)),true)
		PRODUCT_COPY_FILES += \
			$(LOCAL_PATH)/remote_chinatelecom_sh.conf:system/etc/remote.conf 
	else
		PRODUCT_COPY_FILES += \
			$(LOCAL_PATH)/remote_chinatelecom.conf:system/etc/remote.conf
	endif

	PRODUCT_COPY_FILES += \
		$(LOCAL_PATH)/remote_chinatelecom1.conf:system/etc/remote1.conf
else ifeq ($(strip $(CHINA_UNICOM_ENABLE)),true)
	PRODUCT_COPY_FILES += \
		$(LOCAL_PATH)/remote_chinaunicom.conf:system/etc/remote.conf \
		$(LOCAL_PATH)/remote1.conf:system/etc/remote1.conf
    PRODUCT_COPY_FILES += \
	frameworks/native/data/etc/android.hardware.location.xml:system/etc/permissions/android.hardware.location.xml
else ifeq ($(strip $(BUILD_SD_MOBILE_APKS)),true)
	PRODUCT_COPY_FILES +=	\
		$(LOCAL_PATH)/remote_chinamobile2.conf:system/etc/remote2.conf
else
	PRODUCT_COPY_FILES += \
		$(LOCAL_PATH)/remote.conf:system/etc/remote.conf \
		$(LOCAL_PATH)/remote1.conf:system/etc/remote1.conf
endif

#cp kl file for adc keyboard
ifeq ($(CHINA_TELECOM_ENABLE), true)
	ifeq ($(BUILD_SH_TELECOM_APKS), true)
		PRODUCT_COPY_FILES += \
			$(LOCAL_PATH)/Vendor_0001_Product_0001_chinatelecom_sh.kl:/system/usr/keylayout/Vendor_0001_Product_0001.kl
	else ifeq ($(BUILD_SC_TELECOM_APKS), true)
		PRODUCT_COPY_FILES += \
			$(LOCAL_PATH)/Vendor_0001_Product_0001_chinatelecom_sc.kl:/system/usr/keylayout/Vendor_0001_Product_0001.kl
	else
		PRODUCT_COPY_FILES += \
			$(LOCAL_PATH)/Vendor_0001_Product_0001_chinatelecom.kl:/system/usr/keylayout/Vendor_0001_Product_0001.kl
	endif
else ifeq ($(CHINA_UNICOM_ENABLE), true)
	PRODUCT_COPY_FILES += \
		$(LOCAL_PATH)/Vendor_0001_Product_0001_chinaunicom.kl:/system/usr/keylayout/Vendor_0001_Product_0001.kl
else ifeq ($(CHINA_MOBILE_ENABLE), true)
	PRODUCT_COPY_FILES += \
		$(LOCAL_PATH)/Vendor_0001_Product_0001_chinamobile.kl:/system/usr/keylayout/Vendor_0001_Product_0001.kl
else
	PRODUCT_COPY_FILES += \
		$(LOCAL_PATH)/Vendor_0001_Product_0001.kl:/system/usr/keylayout/Vendor_0001_Product_0001.kl
endif

PRODUCT_COPY_FILES += \
	$(LOCAL_PATH)/btplay.sh:system/bin/btplay.sh \
        $(LOCAL_PATH)/unzipbootvideozip.sh:system/bin/unzipbootvideozip.sh

# bootanimation and bootvideo
ifeq ($(strip $(BUILD_SH_MOBILE_APKS)),true)
    ifeq ($(strip $(BUILD_MOBILE_MIGU_BOOTANIMATION)), true)
	PRODUCT_COPY_FILES += \
		$(call find-copy-subdir-files,*,device/amlogic/common/ChinaMobile_apks/sh/Migu/mobile_migu_res,system/media) \
		$(LOCAL_PATH)/bootanimation_chinamobile.zip:system/media/bootanimation.zip \
		$(LOCAL_PATH)/mbox.mp4:system/etc/bootvideo
	else
	PRODUCT_COPY_FILES += \
		$(LOCAL_PATH)/bootanimation_chinamobile.zip:system/media/bootanimation.zip \
		$(LOCAL_PATH)/mbox.mp4:system/etc/bootvideo
	endif
else ifeq ($(strip $(BUILD_JS_MOBILE_APKS)),true)
	PRODUCT_COPY_FILES += \
		$(LOCAL_PATH)/bootanimation_chinamobile_js.zip:system/media/bootanimation.zip
else ifeq ($(strip $(BUILD_SC_MOBILE_APKS)),true)
	PRODUCT_COPY_FILES += \
		$(LOCAL_PATH)/bootanimation_chinamobile_sc.zip:system/media/bootanimation.zip
else ifeq ($(strip $(BUILD_SD_MOBILE_APKS)),true)
	PRODUCT_COPY_FILES +=	\
		$(LOCAL_PATH)/bootanimation_chinamobile_sd.zip:system/media/bootanimation.zip
else ifeq ($(strip $(BUILD_JX_MOBILE_APKS)),true)
	PRODUCT_COPY_FILES += \
		$(LOCAL_PATH)/bootanimation_chinamobile_jx.zip:system/media/bootanimation.zip
else ifeq ($(strip $(CHINA_TELECOM_ENABLE)),true)
	ifeq ($(strip $(BUILD_SH_TELECOM_APKS)),true)
		PRODUCT_COPY_FILES += \
			$(LOCAL_PATH)/bootanimation.zip:system/media/bootanimation.zip \
			$(LOCAL_PATH)/mbox_shtelecom.mp4:system/etc/bootvideo
	else ifeq ($(strip $(BUILD_JX_TELECOM_APKS)),true)
		PRODUCT_COPY_FILES += \
			$(LOCAL_PATH)/bootanimation_chinatelecom_jx.zip:system/media/bootanimation.zip \
			$(LOCAL_PATH)/mbox_chinatelecom.mp4:system/etc/bootvideo
	else ifeq ($(strip $(BUILD_SC_TELECOM_APKS)),true)
		PRODUCT_COPY_FILES += \
			$(LOCAL_PATH)/bootanimation.zip:system/media/bootanimation.zip \
			$(LOCAL_PATH)/mbox_sctelecom.mp4:system/etc/bootvideo
	else ifeq ($(strip $(BUILD_YM3_TELECOM_APKS)),true)
		PRODUCT_COPY_FILES += \
			$(LOCAL_PATH)/bootanimation_chinatelecom_ym3.zip:system/media/bootanimation.zip \
			$(LOCAL_PATH)/mbox_chinatelecom_ym3.mp4:system/etc/bootvideo
	else
		PRODUCT_COPY_FILES += \
			$(LOCAL_PATH)/bootanimation_chinatelecom.zip:system/media/bootanimation.zip \
			$(LOCAL_PATH)/mbox_chinatelecom.mp4:system/etc/bootvideo
	endif
else ifeq ($(strip $(CHINA_UNICOM_ENABLE)),true)
	ifeq ($(UISOURCE_1080P_SUPPORT),true)
		PRODUCT_COPY_FILES += \
			$(LOCAL_PATH)/bootanimation_chinaunicom_1080.zip:system/media/bootanimation.zip \
			$(LOCAL_PATH)/mbox_chinaunicom.mp4:system/etc/bootvideo
	else
		PRODUCT_COPY_FILES += \
			$(LOCAL_PATH)/bootanimation_chinaunicom_720.zip:system/media/bootanimation.zip \
			$(LOCAL_PATH)/mbox_chinaunicom.mp4:system/etc/bootvideo
	endif
else
	PRODUCT_COPY_FILES += \
		$(LOCAL_PATH)/bootanimation.zip:system/media/bootanimation.zip \
		$(LOCAL_PATH)/mbox.mp4:system/etc/bootvideo
endif

ifeq ($(CHINA_TELECOM_ENABLE), true)
ifeq ($(BUILD_YM3_TELECOM_APKS), true)
	DEVICE_PACKAGE_OVERLAYS := $(TARGET_PRODUCT_DIR)/overlay_chinatelecom_ym3
else
	DEVICE_PACKAGE_OVERLAYS := $(TARGET_PRODUCT_DIR)/overlay_chinatelecom
endif
else ifeq ($(CHINA_UNICOM_ENABLE), true)
	DEVICE_PACKAGE_OVERLAYS := $(TARGET_PRODUCT_DIR)/overlay_chinaunicom
else ifeq ($(BUILD_JS_MOBILE_APKS), true)
	DEVICE_PACKAGE_OVERLAYS := $(TARGET_PRODUCT_DIR)/overlay_chinamobile_js
else ifeq ($(BUILD_SH_MOBILE_APKS), true)
	DEVICE_PACKAGE_OVERLAYS := $(TARGET_PRODUCT_DIR)/overlay_chinamobile_sh
else ifeq ($(BUILD_SC_MOBILE_APKS), true)
	DEVICE_PACKAGE_OVERLAYS := $(TARGET_PRODUCT_DIR)/overlay_chinamobile_sc
else ifeq ($(BUILD_SD_MOBILE_APKS), true)
	DEVICE_PACKAGE_OVERLAYS := $(TARGET_PRODUCT_DIR)/overlay_chinamobile_sd
else ifeq ($(BUILD_JX_MOBILE_APKS), true)
	DEVICE_PACKAGE_OVERLAYS := $(TARGET_PRODUCT_DIR)/overlay_chinamobile_jx
else ifeq ($(BUILD_YN_MOBILE_APKS), true)
	DEVICE_PACKAGE_OVERLAYS := $(TARGET_PRODUCT_DIR)/overlay_chinamobile_yn
else ifeq ($(BUILD_LN_MOBILE_APKS), true)
	DEVICE_PACKAGE_OVERLAYS := $(TARGET_PRODUCT_DIR)/overlay_chinamobile_ln
else
	DEVICE_PACKAGE_OVERLAYS := $(TARGET_PRODUCT_DIR)/overlay
endif

PRODUCT_COPY_FILES += \
  $(LOCAL_PATH)/videochat/libvpcodec.so:system/lib/libvpcodec.so

#nanosic vaudio
PRODUCT_COPY_FILES += \
  $(LOCAL_PATH)/nanosic/audio.vaudio.default.so:system/lib/hw/audio.vaudio.default.so

ifeq ($(BUILD_SC_TELECOM_APKS), true)
LIMIT_INSTALL_APP := true
else
LIMIT_INSTALL_APP := false
endif

ifeq ($(LIMIT_INSTALL_APP), true)
PRODUCT_COPY_FILES += \
	$(LOCAL_PATH)/CERT.RSA:system/etc/CERT.RSA
PRODUCT_PROPERTY_OVERRIDES += \
        sys.limit.install.app=true
endif

PRODUCT_PACKAGES += \
       accelerateboot

ifeq ($(IFLYTEK_SUPPORTED), true)
    include device/amlogic/common/iflytek.mk
PRODUCT_COPY_FILES += \
    device/amlogic/p201_iptv/init.amlogic.board.rc.iflytek:root/init.amlogic.board.rc
endif

