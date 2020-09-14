#!/system/bin/sh
if [ $1 -eq 1 ]
then
#blue
#sample_gpionet 6 0xf8b20040 0x00
sample_gpionet 6 0xf8b20400 0x70
sample_gpionet 6 0xf8004010 0xff
sample_gpionet 6 0xf8b20100 0x00
elif [ $1 -eq 2 ]
then
#red
sample_gpionet 6 0xf8b20400 0x70
sample_gpionet 6 0xf8b20100 0xff
sample_gpionet 6 0xf8004010 0x0
elif [ $1 -eq 3 ]
then
#off
sample_gpionet 6 0xf8b20400 0x70
sample_gpionet 6 0xf8b20100 0xff
sample_gpionet 6 0xf8004010 0xff
elif [ $1 -eq 4 ]
then
#net on
echo "no net light"
sample_gpionet 6 0xf8b20400 0x70
sample_gpionet 6 0xf8b20080 0xff
sample_gpionet 6 0xf8b20040 0x0
elif [ $1 -eq 5 ]
then
#net off
echo "no net light"
sample_gpionet 6 0xf8b20400 0x70
sample_gpionet 6 0xf8b20080 0x0
sample_gpionet 6 0xf8b20040 0xff
elif [ $1 -eq 6 ]
then
setprop sys.test_led 1
elif [ $1 -eq 7 ]
then
/system/bin/cputest.sh &
elif [ $1 -eq 8 ]
then
touch /data/cputest
chmod 777 /data/cputest
elif [ $1 -eq 50 ]
then
/system/bin/svcwifion.sh &
elif [ $1 -eq 54 ]
then
/system/bin/sample_mce_update 2 /system/media/boot.jpg
elif [ $1 -eq 55 ]
then
/system/bin/sample_mce_update 2 /system/media/boot_ott.jpg
elif [ $1 -eq 56 ]
then
/system/bin/sample_mce_update 2 /system/media/boot_iptv.jpg
elif [ $1 -eq 57 ]
then
mv data/local/bootanimation_ott.zip  data/local/bootanimation.zip
elif [ $1 -eq 58 ]
then
if [ ! -f "data/local/bootanimation_ott.zip" ]
then
mv data/local/bootanimation.zip  data/local/bootanimation_ott.zip
fi
cp system/media/bootanimation_iptv.zip /data/local/bootanimation.zip
elif [ $1 -eq 0 ]
then
testres=`himd.l 0xf8b22000 - 0xf8b22004 | grep '00000000 00000000'`
if [ "$testres" = "" ]
then
setprop persist.sys.yst.resetstat 0
else
setprop persist.sys.yst.resetstat 1
fi
fi
