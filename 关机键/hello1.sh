#!/system/bin/sh
if [ $1 -eq 1 ]
then
#sample_gpionet 6 0xf8004010 0x0
echo 0x01 0x01 > /proc/msp/pm
#himm 0xf8004004 0xff
else

if [ $1 -eq 2 ]
then
#echo 0x01 0x01 > /proc/msp/pm
#sample_gpionet 6 0xf8004010 0x0
echo 0x01 0x01 > /proc/msp/pm
#himm 0xf8004004 0xff
echo 2
else
echo 0x0 0x0 > /proc/msp/pm
#sample_gpionet 6 0xf8004010 0xff
#himm 0xf8004004 0x0

fi
fi

