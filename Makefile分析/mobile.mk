ifeq ($(strip $(BUILD_SH_MOBILE_APKS)),true)
include device/amlogic/common/ChinaMobile_apks/sh/preinstallation/preinstall.mk
PRODUCT_COPY_FILES += \
  device/amlogic/common/ChinaMobile_apks/sh/config/keypara.ini:/system/etc/keypara.ini \
  device/amlogic/common/ChinaMobile_apks/sh/libtxcore.so:system/lib/libtxcore.so \
  device/amlogic/common/ChinaMobile_apks/sh/libupnpdevice.so:system/lib/libupnpdevice.so \
  device/amlogic/common/ChinaMobile_apks/sh/libmacaddress.so:system/lib/libmacaddress.so \
  device/amlogic/common/ChinaMobile_apks/sh/libmacaddress.so:system/lib/libmacaddress.so \
  device/amlogic/common/ChinaMobile_apks/sh/libtxcore20141017_59.so:system/lib/libtxcore20141017_59.so \
  device/amlogic/common/ChinaMobile_apks/sh/libtxcore2014-06-05_33.so:system/lib/libtxcore2014-06-05_33.so \
  device/amlogic/common/ChinaMobile_apks/sh/AppStore_YD_V2.5.5_product_quanwang.apk:system/app/AppStore_YD_OTT.apk \
  device/amlogic/common/ChinaMobile_apks/sh/SHCMSYSTEM_useraccount.jar:system/framework/SHCMSYSTEM_useraccount.jar \
  device/amlogic/common/ChinaMobile_apks/sh/shcmsystem.jar:system/framework/shcmsystem.jar \
  device/amlogic/common/ChinaMobile_apks/sh/EVQA_sihua_cmvideo_1.37.170605.apk:system/app/com.vixtel.netvista.ott-1.apk \
  device/amlogic/common/ChinaMobile_apks/sh/MiguAichang_V1.9.012.apk:system/priv-app/SHCMCC_MiGu_AiChang.apk \
  device/amlogic/common/ChinaMobile_apks/sh/plugins_cmcc_full_plugin-release.apk:system/app/plugins_cmcc_full_plugin-release.apk \
  device/amlogic/common/ChinaMobile_apks/sh/migu-aichang-source/libaac-encoder.so:system/lib/libaac-encoder.so \
  device/amlogic/common/ChinaMobile_apks/sh/migu-aichang-source/liballjoyn_java.so:system/lib/liballjoyn_java.so \
  device/amlogic/common/ChinaMobile_apks/sh/migu-aichang-source/libblurjni.so:system/lib/libblurjni.so \
  device/amlogic/common/ChinaMobile_apks/sh/migu-aichang-source/libijkffmpeg.so:system/lib/libijkffmpeg.so \
  device/amlogic/common/ChinaMobile_apks/sh/migu-aichang-source/libijkplayer.so:system/lib/libijkplayer.so \
  device/amlogic/common/ChinaMobile_apks/sh/migu-aichang-source/libijksdl.so:system/lib/libijksdl.so \
  device/amlogic/common/ChinaMobile_apks/sh/migu-aichang-source/libijkutil.so:system/lib/libijkutil.so \
  device/amlogic/common/ChinaMobile_apks/sh/migu-aichang-source/liblocSDK3.so:system/lib/liblocSDK3.so \
  device/amlogic/common/ChinaMobile_apks/sh/migu-aichang-source/libmsc.so:system/lib/libmsc.so \
  device/amlogic/common/ChinaMobile_apks/sh/migu-aichang-source/libndkbitmap.so:system/lib/libndkbitmap.so \
  device/amlogic/common/ChinaMobile_apks/sh/migu-aichang-source/libsamplerateconvert.so:system/lib/libsamplerateconvert.so \
  device/amlogic/common/ChinaMobile_apks/sh/migu-aichang-source/libSesLocalEngine.so:system/lib/libSesLocalEngine.so \
  device/amlogic/common/ChinaMobile_apks/sh/migu-aichang-source/libgenius.so:system/lib/libgenius.so \
  device/amlogic/common/ChinaMobile_apks/sh/lib_All_imgoTV_bitmaps.so:system/lib/lib_All_imgoTV_bitmaps.so \
  device/amlogic/common/ChinaMobile_apks/sh/lib_All_imgoTV_nn_tv_air_control.so:system/lib/lib_All_imgoTV_nn_tv_air_control.so \
  device/amlogic/common/ChinaMobile_apks/sh/lib_All_imgoTV_nn_tv_client.so:system/lib/lib_All_imgoTV_nn_tv_client.so \
  device/amlogic/common/ChinaMobile_apks/sh/libstarcor_xul.so:system/lib/libstarcor_xul.so \
  device/amlogic/common/libesplayer_so/libaacdec.so:system/lib/libaacdec.so \
  device/amlogic/common/libesplayer_so/libesplayer.so:system/lib/libesplayer.so \
  device/amlogic/common/ChinaMobile_apks/sh/libimagepipeline.so:system/lib/libimagepipeline.so \
  device/amlogic/common/ChinaMobile_apks/sh/libgifimage.so:system/lib/libgifimage.so \
  device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_Installer.apk:system/app/SHCMCC_Installer.apk \
  device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_ControlServer.apk:system/app/SHCMCC_ControlServer.apk \
  device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_IME2.apk:system/app/SHCMCC_IME2.apk \
  device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_Player.apk:system/app/SHCMCC_Player_sign.apk \
  device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_LocalPlayer.apk:system/app/SHCMCC_LocalPlayer.apk \
  device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_Upgrade.apk:system/app/SHCMCC_Upgrade.apk \
  device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_Upgrade_usb.apk:system/app/SHCMCC_Upgrade_usb.apk \
  device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_Xmpp.apk:system/app/SHCMCC_Xmpp.apk \
  device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_wimo.apk:system/app/SHCMCC_wimo.apk \
  device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_FJSpeedup.apk:system/app/SHCMCC_FJSpeedup.apk \
  device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_Pay.apk:system/app/SHCMCC_Pay.apk \
  device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_ScreenSaver.apk:system/app/SHCMCC_ScreenSaver.apk \
  device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_DeviceManager.apk:system/app/SHCMCC_DeviceManger.apk \
  device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_Diagnostic.apk:system/app/SHCMCC_Diagnostic.apk \
  device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_DLNA.apk:system/app/SHCMCC_DLNA.apk \
  device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_Launcher.apk:system/app/SHCMCC_Launcher.apk \
  device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_Downloader.apk:system/app/SHCMCC_Downloader.apk \
  device/amlogic/common/ChinaMobile_apks/sh/hotkey.properties:system/etc/hotkey.properties \
  device/amlogic/common/ChinaMobile_apks/sh/CERT.RSA:system/etc/CERT.RSA
  ifeq ($(LICENCE_TAG), washu) 
    PRODUCT_COPY_FILES += device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_WasuOTT_2.1.7.35.apk:system/app/SHCMCC_WasuOTT.apk
    PRODUCT_PROPERTY_OVERRIDES += \
        sys.start.licence=washu
  else ifeq ($(LICENCE_TAG),cntv)
    PRODUCT_COPY_FILES += device/amlogic/common/ChinaMobile_apks/sh/icntv-viper-shmobile-V5.1.021.16.09.05.apk:system/app/icntv-shmobile.apk
    PRODUCT_PROPERTY_OVERRIDES += \
        sys.start.licence=cntv
  else ifeq ($(LICENCE_TAG),voole)
    PRODUCT_COPY_FILES += \
      device/amlogic/common/ChinaMobile_apks/sh/VooleWebEpg.apk:system/app/VooleWebEpg.apk \
      device/amlogic/common/ChinaMobile_apks/sh/VooleWebEpgLive.apk:system/app/VooleWebEpgLive.apk \
      device/amlogic/common/ChinaMobile_apks/sh/libvooleglib.so:system/lib/libvooleglib.so \
      device/amlogic/common/ChinaMobile_apks/sh/libgeneralglib.so:system/lib/libgeneralglib.so
    PRODUCT_PROPERTY_OVERRIDES += \
        sys.start.licence=voole
  else ifeq ($(LICENCE_TAG),mango)
    PRODUCT_COPY_FILES += device/amlogic/common/ChinaMobile_apks/sh/HunanOTT-STD_4.0.86.5.2.SHYD.0.0_Release.apk:system/app/HunanOTT-STD.apk
     PRODUCT_PROPERTY_OVERRIDES += \
        sys.start.licence=mango
  else ifeq ($(LICENCE_TAG),bestv)
    PRODUCT_PROPERTY_OVERRIDES += \
        sys.start.licence=bestv
  else ifeq ($(LICENCE_TAG),cibn)
    PRODUCT_COPY_FILES += device/amlogic/common/ChinaMobile_apks/sh/EPG-SHYD.apk:system/app/EPG-SHYD.apk
    PRODUCT_PROPERTY_OVERRIDES += \
        sys.start.licence=cibn
  else
    PRODUCT_COPY_FILES += \
      device/amlogic/common/ChinaMobile_apks/sh/SHCMCC_WasuOTT_2.1.7.35.apk:system/app/SHCMCC_WasuOTT.apk \
      device/amlogic/common/ChinaMobile_apks/sh/icntv-viper-shmobile-V5.1.021.16.09.05.apk:system/app/icntv-shmobile.apk \
      device/amlogic/common/ChinaMobile_apks/sh/VooleWebEpg.apk:system/app/VooleWebEpg.apk \
      device/amlogic/common/ChinaMobile_apks/sh/VooleWebEpgLive.apk:system/app/VooleWebEpgLive.apk \
      device/amlogic/common/ChinaMobile_apks/sh/libvooleglib.so:system/lib/libvooleglib.so \
      device/amlogic/common/ChinaMobile_apks/sh/libgeneralglib.so:system/lib/libgeneralglib.so \
      device/amlogic/common/ChinaMobile_apks/sh/HunanOTT-STD_4.0.86.5.2.SHYD.0.0_Release.apk:system/app/HunanOTT-STD.apk \
      device/amlogic/common/ChinaMobile_apks/sh/EPG-SHYD.apk:system/app/EPG-SHYD.apk \
      device/amlogic/common/ChinaMobile_apks/sh/BesTV_Lite_YDJD_3.1.1705.2.apk:system/app/BesTV_Lite_YDJD_3.1.1705.2.apk 
  endif

else ifeq ($(strip $(BUILD_JS_MOBILE_APKS)),true)
PRODUCT_COPY_FILES += \
  device/amlogic/common/ChinaMobile_apks/js/AppPayClient_v_1.1_2_2015_01_08_1943.apk:/system/app/AppPayClient_v_1.1_2_2015_01_08_1943.apk \
  device/amlogic/common/ChinaMobile_apks/js/CMCC_AppStore-1.2.1.apk:/system/app/CMCC_AppStore-1.2.1.apk  \
  device/amlogic/common/ChinaMobile_apks/js/CMCC_Launcher-16-07.22.01.apk:/system/app/CMCC_Launcher-16-07.22.01.apk \
  device/amlogic/common/ChinaMobile_apks/js/CMCC_SDK_Upgrade-16.07.22.01.apk:/system/app/CMCC_SDK_Upgrade-16.07.22.01.apk \
  device/amlogic/common/ChinaMobile_apks/js/CMCC_Auth-16.07.15.01.apk:/system/app/CMCC_Auth-16.07.15.01.apk \
  device/amlogic/common/ChinaMobile_apks/js/MediaAnalytics.apk:/system/app/MediaAnalytics.apk \
  device/amlogic/common/ChinaMobile_apks/js/CMCC_hw_Login.apk:/system/app/CMCC_hw_Login.apk \
  device/amlogic/common/ChinaMobile_apks/js/PhotoWidget_v_1.5_6_2014_12_08_1609.apk:/system/app/PhotoWidget_v_1.5_6_2014_12_08_1609.apk \
  device/amlogic/common/ChinaMobile_apks/js/JSVendor.apk:/system/app/JSVendor.apk \
  device/amlogic/common/ChinaMobile_apks/js/tr069/libcwmpd.so:/system/lib/libcwmpd.so \
  device/amlogic/common/ChinaMobile_apks/js/tr069/libtr_jni.so:/system/lib/libtr_jni.so \
  device/amlogic/common/ChinaMobile_apks/js/tr069/device.xml:/system/etc/device.xml \
  device/amlogic/common/ChinaMobile_apks/js/tr069/cwmp.conf:/system/etc/cwmp.conf \
  device/amlogic/common/ChinaMobile_apks/js/tr069/Upgrade.apk:/system/app/Upgrade.apk \
  device/amlogic/common/ChinaMobile_apks/js/tr069/libsyncfile-jni.so:/system/lib/libsyncfile-jni.so \
  device/amlogic/common/ChinaMobile_apks/js/multiscreen-stb-signed.apk:/system/app/multiscreen-stb-signed.apk \
  device/amlogic/common/ChinaMobile_apks/js/libCastScreen.so:/system/lib/libCastScreen.so \
  device/amlogic/common/ChinaMobile_apks/js/config/keypara.ini:/system/etc/keypara.ini \
  device/amlogic/common/ChinaMobile_apks/js/ifly/libiflyblesvc_xiri_d.so:/system/lib/libiflyblesvc_xiri_d.so \
  device/amlogic/common/ChinaMobile_apks/js/ifly/xiriservice_tv-changhong:/system/bin/xiriservice_tv-changhong \
  device/amlogic/common/ChinaMobile_apks/js/hotkey.properties:system/etc/hotkey.properties
  
else ifeq ($(strip $(BUILD_SC_MOBILE_APKS)),true)
PRODUCT_COPY_FILES += \
  device/amlogic/common/ChinaMobile_apks/sc/icntv-scmobile_1230.apk:/system/app/icntv-scmobile.apk \
  device/amlogic/common/ChinaMobile_apks/sc/libCTC_MediaControl.so:/system/lib/libCTC_MediaControl.so \
  device/amlogic/common/ChinaMobile_apks/sc/libgetlocalinfo.so:/system/lib/libgetlocalinfo.so \
  device/amlogic/common/ChinaMobile_apks/sc/libloginservicejni.so:/system/lib/libloginservicejni.so \
  device/amlogic/common/ChinaMobile_apks/sc/libtransfermsg.so:/system/lib/libtransfermsg.so \
  device/amlogic/common/ChinaMobile_apks/sc/libztemobileqcs.so:/system/lib/libztemobileqcs.so \
  device/amlogic/common/ChinaMobile_apks/sc/hotkey.properties:system/etc/hotkey.properties

else ifeq ($(strip $(BUILD_SD_MOBILE_APKS)),true)
PRODUCT_COPY_FILES +=	\
	device/amlogic/common/ChinaMobile_apks/sd/tr069/libcwmpd.so:/system/lib/libcwmpd.so	\
	device/amlogic/common/ChinaMobile_apks/sd/tr069/libtr_jni.so:/system/lib/libtr_jni.so	\
	device/amlogic/common/ChinaMobile_apks/sd/tr069/device.xml:/system/etc/device.xml	\
	device/amlogic/common/ChinaMobile_apks/sd/tr069/cwmp.conf:/system/etc/cwmp.conf	\
	device/amlogic/common/ChinaMobile_apks/sd/tr069/libsyncfile-jni.so:/system/lib/libsyncfile-jni.so	\
	device/amlogic/common/ChinaMobile_apks/sd/tr069/keypara.ini:/system/etc/keypara.ini	\
	device/amlogic/common/ChinaMobile_apks/sd/AppStore_YD_OTT_V2.4.5_product.apk:/system/app/AppStore_YD_OTT_V2.4.5_product.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/basicService:/system/bin/basicService	\
	device/amlogic/common/ChinaMobile_apks/sd/CERT.RSA:/system/etc/CERT.RSA	\
	device/amlogic/common/ChinaMobile_apks/sd/liballjoyn_java.so:/system/lib/liballjoyn_java.so	\
	device/amlogic/common/ChinaMobile_apks/sd/libblurjni.so:/system/lib/libblurjni.so	\
	device/amlogic/common/ChinaMobile_apks/sd/libimagepipeline.so:/system/lib/libimagepipeline.so	\
	device/amlogic/common/ChinaMobile_apks/sd/libjiagu_art.so:/system/lib/libjiagu_art.so	\
	device/amlogic/common/ChinaMobile_apks/sd/libndkbitmap.so:/system/lib/libndkbitmap.so	\
	device/amlogic/common/ChinaMobile_apks/sd/libSesLocalEngine.so:/system/lib/libSesLocalEngine.so	\
	device/amlogic/common/ChinaMobile_apks/sd/MiguGameTVNew_5.4.0.0_20161202_SD_10004029027.apk:/system/app/MiguGameTVNew.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/MiguMusic-v4.2.2.11.28-014B120.apk:/system/app/MiguMusic.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/QosMonLoader_v2.1.1_signed.apk:/system/app/QosMonLoader.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/SD_YD_Launcher_V2.1.1.apk:/system/app/SD_YD_Launcher_V2.1.1.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/blackout_whitelist.txt:/system/etc/blackout_whitelist.txt	\
	device/amlogic/common/ChinaMobile_apks/sd/hotkey.properties:system/etc/hotkey.properties	\
	device/amlogic/common/ChinaMobile_apks/sd/huawei_TM_C60_SDYD_for3rd_AA2_20170225_signed.apk:system/bin/TM_C60_SDYD_for3rd_AA2_20170225_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/zte_auth_sdcm_signed.apk:system/bin/auth_sdcm.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/AboutDevice_SD_signed.apk:/system/app/AboutDevice_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/AppManager_SD_signed.apk:/system/app/AppManager_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/AppManager_signed.apk:/system/app/AppManager_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/BootGuide_SD_signed.apk:/system/app/BootGuide_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/DataSharing_SD_signed.apk:/system/app/DataSharing_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/DateTimeSetting_SD_signed.apk:/system/app/DateTimeSetting_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/DebugToolkit_signed.apk:/system/app/DebugToolkit_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/ErrorCode_SD_signed.apk:/system/app/ErrorCode_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/FactoryMode_signed.apk:/system/app/FactoryMode_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/FactoryUpgrade_signed.apk:/system/app/FactoryUpgrade_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/LogoDownload_SD_signed.apk:/system/app/LogoDownload_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/More_SD_signed.apk:/system/app/More_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/NetworkSetting_SD_signed.apk:/system/app/NetworkSetting_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/NetworkSpeedTest_SD_signed.apk:/system/app/NetworkSpeedTest_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/RecoverySetting_SD_signed.apk:/system/app/RecoverySetting_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/ResolutionSetting_SD_signed.apk:/system/app/ResolutionSetting_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/ScaleSetting_SD_signed.apk:/system/app/ScaleSetting_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/Settings_SD_signed.apk:/system/app/Settings_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/Standby_SD_signed.apk:/system/app/Standby_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/StbParmService_SD_signed.apk:/system/app/StbParmService_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/Upgrade_SD_signed.apk:/system/app/Upgrade_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/UserStorage_SD_signed.apk:/system/app/UserStorage_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/Vendor_SD_signed.apk:/system/app/Vendor_SD_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/sd/CH/WriteStbInfo_signed.apk:/system/app/WriteStbInfo_signed.apk
	

else ifeq ($(strip $(BUILD_YN_MOBILE_APKS)),true)
PRODUCT_COPY_FILES += \
  device/amlogic/common/ChinaMobile_apks/yn/EVQA_vixtel_ynmobile_1.39.170830.apk:/system/app/com.vixtel.netvista.ott-1.apk	\
  device/amlogic/common/ChinaMobile_apks/yn/basicService:/system/bin/basicService	\
	device/amlogic/common/ChinaMobile_apks/yn/libckeygenerator.so:/system/lib/libckeygenerator.so	\
	device/amlogic/common/ChinaMobile_apks/yn/libcurl.so:/system/lib/libcurl.so	\
	device/amlogic/common/ChinaMobile_apks/yn/libcurl_7421.so:/system/lib/libcurl_7421.so	\
	device/amlogic/common/ChinaMobile_apks/yn/libffmpeg-armv6-vfp.so:/system/lib/libffmpeg-armv6-vfp.so	\
	device/amlogic/common/ChinaMobile_apks/yn/libffmpeg-armv7-neon.so:/system/lib/libffmpeg-armv7-neon.so	\
	device/amlogic/common/ChinaMobile_apks/yn/libgifimage.so:/system/lib/libgifimage.so	\
	device/amlogic/common/ChinaMobile_apks/yn/libHCDNClientNet.so:/system/lib/libHCDNClientNet.so	\
	device/amlogic/common/ChinaMobile_apks/yn/libimagepipeline.so:/system/lib/libimagepipeline.so	\
	device/amlogic/common/ChinaMobile_apks/yn/libiqiyi_media_player.so:/system/lib/libiqiyi_media_player.so	\
	device/amlogic/common/ChinaMobile_apks/yn/libiqiyi_player_memalloc.so:/system/lib/libiqiyi_player_memalloc.so	\
	device/amlogic/common/ChinaMobile_apks/yn/libmediacodec.so:/system/lib/libmediacodec.so	\
	device/amlogic/common/ChinaMobile_apks/yn/libqidunkey.so:/system/lib/libqidunkey.so	\
	device/amlogic/common/ChinaMobile_apks/yn/config/keypara.ini:/system/etc/keypara.ini	\
	device/amlogic/common/ChinaMobile_apks/yn/StbconfigProvider.apk:/system/app/StbconfigProvider.apk	\
	device/amlogic/common/ChinaMobile_apks/yn/tr069/libcwmpd.so:/system/lib/libcwmpd.so \
	device/amlogic/common/ChinaMobile_apks/yn/tr069/libtr_jni.so:/system/lib/libtr_jni.so \
	device/amlogic/common/ChinaMobile_apks/yn/tr069/device.xml:/system/etc/device.xml \
	device/amlogic/common/ChinaMobile_apks/yn/tr069/cwmp.conf:/system/etc/cwmp.conf \
	device/amlogic/common/ChinaMobile_apks/yn/tr069/Upgrade.apk:/system/app/Upgrade.apk \
	device/amlogic/common/ChinaMobile_apks/yn/tr069/libsyncfile-jni.so:/system/lib/libsyncfile-jni.so
   ifeq ($(LICENCE_TAG), zte)
      PRODUCT_COPY_FILES += \
		device/amlogic/common/ChinaMobile_apks/yn/zte/gitv-launcher-2.0.02-yunnanzte-default-galaxy-online-release.apk:/system/app/gitv-launcher-2.0.02-yunnanzte-default-galaxy-online-release.apk \
		device/amlogic/common/ChinaMobile_apks/yn/zte/gitv-tv-launcher-1.3.11F-yunnanyidong-YNYDZX-release.apk:/system/app/gitv-tv-launcher-1.3.11F-yunnanyidong-YNYDZX-release.apk \
		device/amlogic/common/ChinaMobile_apks/yn/zte/gitv-tv-live-2.3.18-YNYDZX-release.apk:/system/app/gitv-tv-live-2.3.18-YNYDZX-release.apk
   else ifeq ($(LICENCE_TAG), huawei)
      PRODUCT_COPY_FILES += \
	     device/amlogic/common/ChinaMobile_apks/yn/huawei/gitv-launcher-2.0.02-yunnan-online-release.apk:/system/app/gitv-launcher-2.0.02-yunnan-online-release.apk \
	     device/amlogic/common/ChinaMobile_apks/yn/huawei/gitv-tv-launcher-1.3.11F-yunnanyidong-YNYDHW-release.apk:/system/app/gitv-tv-launcher-1.3.11F-yunnanyidong-YNYDHW-release.apk \
	     device/amlogic/common/ChinaMobile_apks/yn/huawei/gitv-tv-live-2.3.18-YNYDHW-release-0719.apk:/system/app/gitv-tv-live-2.3.18-YNYDHW-release-0719.apk
   endif
else ifeq ($(strip $(BUILD_JX_MOBILE_APKS)),true)
PRODUCT_COPY_FILES += \
  device/amlogic/common/ChinaMobile_apks/jx/IsSetting_V1.3.17.6.23.apk:/system/app/IsSetting_V1.3.17.6.23.apk \
  device/amlogic/common/ChinaMobile_apks/jx/IsSystemUpdate_V2.3.17.10.17.apk:/system/app/IsSystemUpdate_V2.3.17.10.17.apk \
  device/amlogic/common/ChinaMobile_apks/jx/QosMonLoader_v2.1.1_p.apk:/system/app/QosMonLoader_v2.1.1_p.apk \
  device/amlogic/common/ChinaMobile_apks/jx/SilentInstall_signed.apk:/system/app/SilentInstall_signed.apk \
  device/amlogic/common/ChinaMobile_apks/jx/icntv_taipan6.5.3_jx20170804_release.apk:/system/app/icntv_taipan6.5.3_jx20170804_release.apk \
  device/amlogic/common/ChinaMobile_apks/jx/tv.icntv.vendor.apk:/system/app/tv.icntv.vendor.apk \
  device/amlogic/common/ChinaMobile_apks/jx/basicService:/system/bin/basicService \
  device/amlogic/common/ChinaMobile_apks/jx/libBugly.so:/system/lib/libBugly.so

else ifeq ($(strip $(BUILD_LN_MOBILE_APKS)),true)
PRODUCT_COPY_FILES += \
	device/amlogic/common/ChinaMobile_apks/ln/andPhoto_lib/libgetuiext2.so:/system/lib/libgetuiext2.so \
	device/amlogic/common/ChinaMobile_apks/ln/andPhoto_lib/libijkffmpeg.so:/system/lib/libijkffmpeg.so \
	device/amlogic/common/ChinaMobile_apks/ln/andPhoto_lib/libijkplayer.so:/system/lib/libijkplayer.so \
	device/amlogic/common/ChinaMobile_apks/ln/andPhoto_lib/libijksdl.so:/system/lib/libijksdl.so \
	device/amlogic/common/ChinaMobile_apks/ln/andPhoto_lib/libMCloudTv3.so:/system/lib/libMCloudTv3.so \
	device/amlogic/common/ChinaMobile_apks/ln/gitv-launcher_lib/libgifimage.so:/system/lib/libgifimage.so \
	device/amlogic/common/ChinaMobile_apks/ln/gitv-launcher_lib/libimagepipeline.so:/system/lib/libimagepipeline.so \
	device/amlogic/common/ChinaMobile_apks/ln/basicService:/system/bin/basicService \
	device/amlogic/common/ChinaMobile_apks/ln/config/keypara.ini:/system/etc/keypara.ini	\
	device/amlogic/common/ChinaMobile_apks/ln/andPhoto.apk:/system/app/andPhoto.apk	\
	device/amlogic/common/ChinaMobile_apks/ln/SHCMCC_IME2_signed.apk:/system/app/SHCMCC_IME2_signed.apk	\
	device/amlogic/common/ChinaMobile_apks/ln/hotkey.properties:system/etc/hotkey.properties \
	device/amlogic/common/ChinaMobile_apks/ln/gitv-launcher-2.0.03-liaoning-online-release.apk:/system/app/gitv-launcher-2.0.03-liaoning-online-release.apk	\
	device/amlogic/common/ChinaMobile_apks/ln/EVQA_vixtel_lnmobile_1.39.170704.apk:/system/app/EVQA_vixtel_lnmobile_1.39.170704.apk	\
	device/amlogic/common/ChinaMobile_apks/ln/tr069/libcwmpd.so:/system/lib/libcwmpd.so \
	device/amlogic/common/ChinaMobile_apks/ln/tr069/libtr_jni.so:/system/lib/libtr_jni.so \
	device/amlogic/common/ChinaMobile_apks/ln/tr069/device.xml:/system/etc/device.xml \
	device/amlogic/common/ChinaMobile_apks/ln/tr069/cwmp.conf:/system/etc/cwmp.conf \
	device/amlogic/common/ChinaMobile_apks/ln/tr069/Upgrade.apk:/system/app/Upgrade.apk \
	device/amlogic/common/ChinaMobile_apks/ln/tr069/libsyncfile-jni.so:/system/lib/libsyncfile-jni.so \
	device/amlogic/common/ChinaMobile_apks/ln/StbconfigProvider.apk:/system/app/StbconfigProvider.apk
endif
ifneq ($(filter TAG READER ALL, $(NFC_SUPPORT_TYPE)),)
    PRODUCT_PROPERTY_OVERRIDES += \
        sys.support.smpte=false
endif
