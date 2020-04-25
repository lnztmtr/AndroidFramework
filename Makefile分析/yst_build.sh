#/bin/bash

##### config start ######
## if set YSTEN_CONFIG_CLEAN_ANDROID to Y, then will rm out dir before build android
YSTEN_CONFIG_CLEAN_ANDROID=Y

## if set YSTEN_CONFIG_COPY_PATCH to Y, then copy patch to out dir
YSTEN_CONFIG_COPY_PATCH=Y


## if set YSTEN_CONFIG_COMPILE_PATCH to Y, then copy patch to out dir
YSTEN_CONFIG_COMPILE_PATCH=Y

## value name equase patch name which is same as YSTen_Patch dir 
YSTEN_CONFIG_SLINK=Y

#choose CM201-1 or A20
export PRODUCT_MODEL_TYPE=CM201-1

#CM201
#A20
YSTEN_CONFIG_BORADTYPE=CM201

export YSTEN_CUSTOM_PATCH=CM201_guangdong
YSTEN_CHIP_TYPE=p201_iptv

export root_dir=$(pwd)
echo $root_dir

if [ ! -d "$root_dir/YSTen_Patch/" ];then
echo "错误，编译路径不正确，请检查当前目录在sdk根目录！"
exit 0
else
echo "开始一键编译"
fi

export JAVA_HOME=$root_dir/toolchain/jdk1.6.0_37
export JRE_HOME=$root_dir/toolchain/jdk1.6.0_37
export CLASSPATH=.:$JAVA_HOME/lib:$JRE_HOME/lib:$CLASSPATH
export PATH=$JAVA_HOME/bin:$JRE_HOME/bin:$root_dir/toolchain/CodeSourcery/Sourcery_G++_Lite/bin:$root_dir/toolchain/gnutools/arc-4.8-amlogic-20130904-r2/bin:$root_dir/toolchain/gcc-linaro-arm-linux-gnueabihf/bin:$root_dir/toolchain/gcc-linaro-aarch64-none-elf-4.8-2013.11_linux/bin:$root_dir/toolchain/gcc-linaro-aarch64-linux-gnu-4.9-2014.09_linux/bin:$root_dir/toolchain/gcc-arm-none-eabi-6-2017-q2-update/bin:$PATH

export PROJECT_TYPE=shmobile 
export PROJECT_ID=p211 
export MOBILE_UNIT_TYPE=CM201-1-YS 
export MOBILE_VERSION=CM201-1-YS.GD.08.20022402
export LICENCE_TAG=bestv

SDK_DIR=`pwd`
YSTEN_ANDROID_TOP_DIR=$SDK_DIR
YSTEN_PATCH_TOP_DIR=$SDK_DIR/YSTen_Patch/$YSTEN_CUSTOM_PATCH


##### config end ######

echo "+++++ compiling android sdk +++++"

rm $YSTEN_ANDROID_TOP_DIR/amlogic_905_L3_ysten

#cp -rf $SDK_DIR/YSTen_Patch/cm201_common/* out
#rm -r $YSTEN_ANDROID_TOP_DIR/packages
rm -r $YSTEN_ANDROID_TOP_DIR/packages/ysten_apps/common_apps
rm -r $YSTEN_ANDROID_TOP_DIR/packages/ysten_apps/custom_apps
#rm -r $YSTEN_PATCH_TOP_DIR/packages
ln -s $YSTEN_PATCH_TOP_DIR/ysten_custom/custom_apps $YSTEN_ANDROID_TOP_DIR/packages/ysten_apps/custom_apps
ln -s $YSTEN_ANDROID_TOP_DIR/YSTen_Patch/common/packages/ysten_apps/common_apps $YSTEN_ANDROID_TOP_DIR/packages/ysten_apps/common_apps
if [[ $YSTEN_CONFIG_SLINK = "Y" ]]
then
rm -r $YSTEN_ANDROID_TOP_DIR/device/amlogic/$YSTEN_CHIP_TYPE
ln -s $YSTEN_PATCH_TOP_DIR/device/amlogic/$YSTEN_CHIP_TYPE $YSTEN_ANDROID_TOP_DIR/device/amlogic/$YSTEN_CHIP_TYPE
#rm -r $YSTEN_ANDROID_TOP_DIR/device/amlogic/common/mobile.mk
#ln -s $YSTEN_PATCH_TOP_DIR/common/mobile.mk $YSTEN_ANDROID_TOP_DIR/device/amlogic/common/
else
rm -r $YSTEN_ANDROID_TOP_DIR/device/amlogic/$YSTEN_CHIP_TYPE
cp $YSTEN_PATCH_TOP_DIR/device/amlogic/$YSTEN_CHIP_TYPE $YSTEN_ANDROID_TOP_DIR/device/amlogic/$YSTEN_CHIP_TYPE -raf
fi


#dts
rm -r $YSTEN_ANDROID_TOP_DIR/common/arch/arm64/boot/dts/amlogic
ln -s $YSTEN_ANDROID_TOP_DIR/YSTen_Patch/common/boardtype/$YSTEN_CONFIG_BORADTYPE/dts/amlogic $YSTEN_ANDROID_TOP_DIR/common/arch/arm64/boot/dts/amlogic
#uboot
cp $YSTEN_ANDROID_TOP_DIR/YSTen_Patch/common/boardtype/$YSTEN_CONFIG_BORADTYPE/uboot/gxl_p211_v1.h $YSTEN_ANDROID_TOP_DIR/uboot/include/configs/gxl_p211_v1.h -raf

cp $YSTEN_ANDROID_TOP_DIR/YSTen_Patch/common/boardtype/$YSTEN_CONFIG_BORADTYPE/uboot/u-boot.bin $YSTEN_ANDROID_TOP_DIR/device/amlogic/$YSTEN_CHIP_TYPE/upgrade/gxl/720p/u-boot.bin -raf
cp $YSTEN_ANDROID_TOP_DIR/YSTen_Patch/common/boardtype/$YSTEN_CONFIG_BORADTYPE/uboot/u-boot.bin.sd.bin $YSTEN_ANDROID_TOP_DIR/device/amlogic/$YSTEN_CHIP_TYPE/upgrade/gxl/720p/u-boot.bin.sd.bin -raf
cp $YSTEN_ANDROID_TOP_DIR/YSTen_Patch/common/boardtype/$YSTEN_CONFIG_BORADTYPE/uboot/u-boot.bin.usb.bl2 $YSTEN_ANDROID_TOP_DIR/device/amlogic/$YSTEN_CHIP_TYPE/upgrade/gxl/720p/u-boot.bin.usb.bl2 -raf
cp $YSTEN_ANDROID_TOP_DIR/YSTen_Patch/common/boardtype/$YSTEN_CONFIG_BORADTYPE/uboot/u-boot.bin.usb.tpl $YSTEN_ANDROID_TOP_DIR/device/amlogic/$YSTEN_CHIP_TYPE/upgrade/gxl/720p/u-boot.bin.usb.tpl -raf


cp -r $YSTEN_PATCH_TOP_DIR/TR069/sjTR069.apk $YSTEN_ANDROID_TOP_DIR/sjos/apps/sjTR069/sjTR069.apk
if [[ $YSTEN_CONFIG_BORADTYPE = "CM201" ]]
then
#do something
echo "CM201"
elif [[ $YSTEN_CONFIG_BORADTYPE = "A20" ]]
then
#do something
echo "A20"
fi

source build/envsetup.sh
lunch p201_iptv-user
make otapackage -j32 2>&1 | tee compile.log
