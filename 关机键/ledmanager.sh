#!/system/bin/sh
if [ $1 -eq 1 ]
then
#blue
#sample_gpionet 6 0xf8b20040 0x00
#himm 0xf8004010 0x0
himm 0xf8004004 0x0
elif [ $1 -eq 2 ]
then
#red
himm 0xf8004004 0xff
elif [ $1 -eq 3 ]
then
#off
#sample_gpionet 6 0xf8b20040 0xff
himm 0xf8004004 0x0
elif [ $1 -eq 4 ]
then
#net on
himm 0xf8004008 0x0
elif [ $1 -eq 5 ]
then
#net off
himm 0xf8004008 0xff
elif [ $1 -eq 6 ]
then
setprop sys.test_led 1
elif [ $1 -eq 7 ]
then
#/system/bin/cputest.sh &
echo "no cpu test"
elif [ $1 -eq 8 ]
then
touch /data/cputest
chmod 777 /data/cputest
elif [ $1 -eq 10 ]
then
/system/bin/adbcontrol.sh &
elif [ $1 -eq 13 ]
then
/system/bin/irflashadd &
elif [ $1 -eq 14 ]
then
himm 0xf8004008 0xff
himm 0xf8004080 0xff
elif [ $1 -eq 16 ]
then
#net on
himm 0xf8004008 0x0
elif [ $1 -eq 17 ]
then
#net off
himm 0xf8004008 0xff
elif [ $1 -eq 21 ]
then
himm 0xf8b23400  0x30
himm 0xf8b23080 0x0
elif [ $1 -eq 22 ]
then
himm 0xf8b23400  0x30
himm 0xf8b23080 0xff
elif [ $1 -eq 25 ]
then
himm 0xf8b26400 0x18
himm 0xf8b26040 0x0
elif [ $1 -eq 30 ]
then
/system/bin/refreshwifi.sh &
elif [ $1 -eq 31 ]
then
alsa_amixer cset numid=3,iface=MIXER,name='Hdmi Playback Volume' 0
himm 0xf8b26400 0x18
himm 0xf8b26040 0x0
elif [ $1 -eq 32 ]
then
alsa_amixer cset numid=3,iface=MIXER,name='Hdmi Playback Volume' 20
elif [ $1 -eq 100 ]
then
cat system/media/logo.img system/media/Step1.bin system/media/Step2.bin > /dev/block/platform/soc/by-name/logo
elif [ $1 -eq 101 ]
then
cat system/media/logoott.img system/media/Step1.bin system/media/Step2.bin > /dev/block/platform/soc/by-name/logo
elif [ $1 -eq 102 ]
then
cat system/media/logoiptv.img system/media/Step1.bin system/media/Step2.bin > /dev/block/platform/soc/by-name/logo
elif [ $1 -eq 103 ]
then
mv data/local/bootanimation_ott.zip  data/local/bootanimation.zip
elif [ $1 -eq 104 ]
then
if [ ! -f "data/local/bootanimation_ott.zip" ]
then
mv data/local/bootanimation.zip  data/local/bootanimation_ott.zip
fi
cp system/media/bootanimation_iptv.zip /data/local/bootanimation.zip
elif [ $1 -eq 105 ]
then
cat system/media/logo.img system/media/Step1.bin system/media/Step2.bin > /dev/block/platform/soc/by-name/logo
elif [ $1 -eq 15 ]
then
/system/bin/testnet.sh &
elif [ $1 -eq 35 ]
then
testres=`cat /sys/class/switch/camera/state`
nowpacakage = `getprop persist.sys.nowpackagename`
/system/bin/detectcamsera.sh &
if [ "$nowpacakage" == "" ]
then

if [ "$testres" == "1" ]
then
echo "do nothing now"
fi

fi
elif [ $1 -eq 36 ]
then
#testres=`cat /sys/class/switch/camera/state`
#if [ $testres == "1" ]
#then
#himm 0xf8004008 0xff
#else
#himm 0xf8004008 0x0
#fi
/system/bin/detectcamsera.sh &
elif [ $1 -eq 85 ]
then
echo "85 camera open"
#testres=`getprop persist.sys.yst.gesture`
#if [ "$testres" = "1" ]
#then
#setprop sys.yst.gesture 0
#sleep 0.3
#fi
#am broadcast -a com.ysten.hm.monitor -e action close
#sleep 2
elif [ $1 -eq 86 ]
then
echo "86 camera close"
#testres=`getprop persist.sys.yst.gesture`
#if [ "$testres" = "1" ]
#then
#sleep 2
#setprop sys.yst.gesture 1
#fi
elif [ $1 -eq 88 ]
then
am start -n com.ysten.demo/.AllApp &
elif [ $1 -eq 40 ]
then
echo 3 0x76 0x70 0xa0 > /proc/msp/i2c
setprop sys.mic.enable 0
elif [ $1 -eq 41 ]
then
setprop sys.mic.enable 1
test1=`getprop persist.sys.aging`
if [ "$test1" = "1" ]
then
echo 3 0x76 0x70 0xa0 > /proc/msp/i2c
else
echo 3 0x76 0x70 0xb0 > /proc/msp/i2c
fi
elif [ $1 -eq 42 ]
then
#am broadcast -a com.lhxk.voice_recognition_end
am broadcast -a android.ysten.systemupdate -e miccontrol releasemic
elif [ $1 -eq 43 ]
then
am broadcast -a android.ysten.systemupdate -e miccontrol resetmic
#am broadcast -a com.lhxk.voice_recognition_start
#echo "no start";
elif [ $1 -eq 46 ]
then
setprop sys.mic.state 1
elif [ $1 -eq 47 ]
then
setprop sys.mic.state 0
elif [ $1 -eq 48 ]
then
hello1.sh 0
elif [ $1 -eq 49 ]
then
hello1.sh 1
elif [ $1 -eq 51 ]
then
sync;echo 3 > /proc/sys/vm/drop_caches
elif [ $1 -eq 52 ] 
then
setprop sys.mic.enable 1
busybox killall -9 com.ysten.remote.service
busybox killall -9 com.ysten.remote.service:audio
busybox killall -9 com.ysten.remote.service:remote
am startservice -a com.ysten.remote.service
elif [ $1 -eq 55 ]
then
sleep 2
testres=`getprop persist.sys.smartsuspendin`
if [ "$testres" == "1" ]
then
himm 0xf8004004 0xff
else
himm 0xf8004004 0x0
fi
elif [ $1 -eq 56 ]
then
busybox killall -9 sample_tdm
am broadcast -a com.lhxk.voice_recognition_end
testres1=`getprop sys.mic.enable`
loopi=0;
while [ "$testres1" = "0" ]
do
sleep 1
testres1=`getprop sys.mic.enable`
loopi=$(($loopi+1));
am broadcast -a com.lhxk.voice_recognition_end
if(($i>4))
then
testres1=1
fi
done
rm /mnt/audiofifo
busybox mkfifo /mnt/audiofifo
busybox chmod 777  /mnt/audiofifo
#sleep 1;cat /mnt/test4ch > /mnt/audiofifo &
sleep 1;sample_tdm 500 /mnt/audiofifo &
echo 3 0x76 0x70 0xa0 > /proc/msp/i2c
setprop sys.mic.sampleend 1
#sleep 1;alsa_aplay -C  -D hw:0,0  -c 4 -r 48000 -f S16_LE -d 1  /mnt/audiofifo &
elif [ $1 -eq 57 ]
then
busybox killall -9 sample_tdm
setprop sys.mic.sampleend 0
#busybox killall -9 alsa_aplay
elif [ $1 -eq 58 ]
then
/system/bin/detectcamsera.sh &
elif [ $1 -eq 60 ]
then
while true
do
sleep 1
echo userspace > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
sleep 1
echo freq=1200000 volt=1090 > /proc/msp/pm_cpu
sleep 60
done &
elif [ $1 -eq 61 ]
then
sleep 1
echo userspace > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
sleep 1
while true
do
echo userspace > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor
echo freq=800000 volt=890 > /proc/msp/pm_cpu
sleep 1
echo freq=1200000 volt=1090 > /proc/msp/pm_cpu
sleep 1
done &
elif [ $1 -eq 90 ]
then
busybox mkfifo /mnt/memfifo;
chmod 777 /mnt/memfifo;
/data/data/com.softwinner.agingdragonbox/cache/memtester 128m 0 > /mnt/memfifo &
elif [ $1 -eq 94 ]
then
busybox killall -9 com.gitv.tv.launcher:lang
busybox killall -9 com.gitv.tv.launcher
setprop persist.sys.testgitv 94
elif [ $1 -eq 95 ]
then
testres=`himd.l 0xf8b27008 | grep '0000:  00000000 00000000'`
if [ "$testres" = "" ]
then
setprop persist.sys.yst.extendkeystat 0
else
setprop persist.sys.yst.extendkeystat 1
fi

elif [ $1 -eq 0 ]
then
#testres=`himd.l 0xf8b28100 | grep '0000:  00000000 00000000'`
testres1=`himd.l 0xf8b28100 | grep "0000:" | busybox awk '{print $2}'`;
testnum2=`echo ${testres1:6:1}`;
testnow1=`busybox printf %d 0x$testnum2`;
testnow1=`busybox expr $testnow1 % 8`;
testnow1=`busybox expr $testnow1 / 4`;
if [ "$testnow1" = "1" ]
then
setprop persist.sys.yst.resetstat 0
else
setprop persist.sys.yst.resetstat 1
fi

elif [ $1 -eq 200 ]
then
echo fmt pal > proc/msp/disp1
echo event 17 > /proc/msp/hdmi0
elif [ $1 -eq 201 ]
then
echo fmt 1080p50 > proc/msp/disp1
echo event 16 > /proc/msp/hdmi0
fi
