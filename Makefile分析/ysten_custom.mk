#$(call inherit-product-if-exists, vendor/nui/nui.mk)
$(call inherit-product-if-exists, YSTen_Patch/common/common.mk)
$(shell mkdir -p `pwd`/out/target/product/$(TARGET_PRODUCT))
#$(shell cp -rf `pwd`/YSTen_Patch/common/target/* `pwd`/out/target)
$(shell cp -rf `pwd`/YSTen_Patch/common/ysten_out/* `pwd`/out/target/product/$(TARGET_PRODUCT))
$(shell cp -rf $(LOCAL_PATH)/ysten_out/* `pwd`/out/target/product/$(TARGET_PRODUCT))

# for China mobile version
ifeq ($(PRODUCT_MODEL_TYPE),CM201-1)
BUILD_INFO_CM201_1 := true
PRODUCT_MODEL_YS := CM201-1-YS
PRODUCT_MANUFACTURER_YS := CMDC
else ifeq ($(PRODUCT_MODEL_TYPE),A20)
BUILD_INFO_A20 := true
PRODUCT_MODEL_YS := A20
PRODUCT_MANUFACTURER_YS := CMIOT
else
PRODUCT_MODEL_YS := p201_iptv
PRODUCT_MANUFACTURER_YS := amlogic
endif

PRODUCT_PACKAGES += \
        LocalFileManager \
        poweroff \
        YstenBootReceiver \
		Auth \
        Vendor

PRODUCT_PROPERTY_OVERRIDES += ro.dolby.dmacert.enable=true

PRODUCT_PROPERTY_OVERRIDES += \
        ro.ysten.province=CM201_guangdong \
		ro.ysten.pn.software=GD0801 \
        persist.sys.sw.firstLaunch=true \
        persist.jsmoblie.startloadtest=true \
        persist.sys.yst.app=1 \
        persist.sys.cvbs.and.hdmi=true \
        persist.sys.yst.product.id=CM201-1-YS \
        persist.sys.yst.logineth=1 \
        persist.sys.product.id=CM201-1-YS \
        persist.sys.wifistate=1 \
        persist.sys.wifistate.check=0 \
        persist.sys.softdetector.enable=true \
        persist.sys.newhisupdate=1 \
        persist.hdmi.suspend.enable=0 \
        persist.hdmi.suspend.time=5 \
        persist.sys.firstboot.flag=1 \
        persist.sys.softdetect.name=mobile \
        ro.dolby.dmacert.enable=true \
        ro.icntv=0

