#!/system/bin/sh

if [ $1 == "mv" ]
then
echo "delete the animation source before"
#busybox find /data/local -type f \( -name "*.jpg" -o -name "*.png" -o -name "*.ts" -o -name "*.mp4" -o -name "bootanimation.zip" -o -name "configs.txt" \) -exec rm -rf {} \;
rm -rf /data/local/*.ts
rm -rf /data/local/*.mp4
rm -rf /data/local/bootanimation.zip
rm -rf /data/local/*.png
rm -rf /data/local/*.jpg
rm -rf /data/local/*.bmp
chmod 666 /data/local/bootanimation/*
if [ ! -n $2 ]
then
rm -rf /data/local/configs.txt
mv $2 /data/local/bootanimation.zip
else
mv /data/local/bootanimation/* /data/local
fi
#rm /data/local/animation/*
elif [ $1 == "p" -o $1 == "v" ]
then
if [ $5 -eq 0 ]
then
rm -rf /data/local/configs.txt
fi
configtxt=$1","$2","$3","$4

echo $configtxt
if [ ! -f /data/local/configs.txt ]
then
touch /data/local/configs.txt
fi
echo $configtxt >> /data/local/configs.txt
elif [ $1 == "cleanall" ]
then
rm -rf /data/local/bootanimation
rm -rf /data/local/logoimg
elif [ $1 == "logo" ]
then
`cat $2 > $3`
`sync`
fi

