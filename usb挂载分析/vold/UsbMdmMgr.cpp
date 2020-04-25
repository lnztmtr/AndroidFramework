/*
 * Copyright (C) 2011 Amlogic, Inc.
 *
 *  Usb Modem Manager
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Author:  Frank Chen <frank.chen@amlogic.com>
 */

#include <string.h>
#include <stdio.h>
#include <malloc.h>
#include <errno.h>
#include <dirent.h>
#include <fcntl.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <sys/types.h>

#include "UsbMdmMgr.h"
#include <cutils/properties.h>
#include <cutils/log.h>

#define LOG_TAG "Vold"

typedef struct {
	int vid;
	int pid;
} MdmMgrInfo;

static MdmMgrInfo g_KnownMdms[] = {
    /*vid,   ,pid,    */
    //ZTE MU351 dongle
    { 0x19d2, 0x0003 },
    //ZTE MF110 RoHS
    { 0x19d2, 0x0016 },
    //ZTE MF633,MF637U,MF110 dongle
    { 0x19d2, 0x0031 },
    //ZTE-T A355
    { 0x19d2, 0x0079},
    //ZTE AC590
    { 0x19d2, 0x0094},
    //ZTE K4505-Z
    { 0x19d2, 0x0104},
    //ZTE MF691
    { 0x19d2, 0x0143},
    //ZTE K3570-Z
    { 0x19d2, 0x1008},
    //ZTE K3770-Z
    { 0x19d2, 0x1177 },
    //ZTE AD3812 & MG3732 module
    { 0x19d2, 0xFFEB },
    //ZTE MC8630 module
    { 0x19d2, 0xFFFE },
    //ZTE MC2700 & MC2716 module
    { 0x19d2, 0xFFED },
    //ZTE MC2718 module
    { 0x19d2, 0xFFE8 },
    //ZTE AC682
    { 0x19d2, 0xFFDD },
    //ZTE MC2816 module
    { 0x19d2, 0xFFF1 },
    //ZTE MF210 module
    { 0x19d2, 0x0117 },
    //ZTE MF226 module
    { 0x19d2, 0x0144 },
    // ZTE AC580
    { 0x19d2, 0x0152 },
    //ZTE MF880T
    { 0x19d2, 0x0167 },
    //ZTE 3G-200
    { 0x19d2, 0x1516 },
    //ZTE MF637 Orange FR
    { 0x19d2, 0x0121 },
    //ZTE MF110 Movistar
    { 0x19d2, 0x0124 },
    //ZTE MF180
    { 0x19d2, 0x2003 },
    //LONGCHEER U6300/U6100 module
    { 0x1C9E, 0x9603 },
    //LONGCHEER SU9605
    { 0x1C9E, 0x9605 },
    //HUAWEI E1750/E620 module
    { 0x12d1, 0x1001 },	
    //HUAWEI E173s module
    { 0x12d1, 0x1c05 },	
    //HUAWEI E173s-2
    { 0x12d1, 0x1c08 },
    { 0x12d1, 0x1003 },
    //HUAWEI E153
    { 0x12d1, 0x1004 },
    { 0x12d1, 0x1402 },
    //HUAWEI EW770W module
    { 0x12d1, 0x1404 },		    
    { 0x12d1, 0x1406 },	
    { 0x12d1, 0x1407 },	
    { 0x12d1, 0x140A },	
    { 0x12d1, 0x140B },	
    //HUAWEI E173u module
    { 0x12d1, 0x140C },
    { 0x12d1, 0x1411 },
    { 0x12d1, 0x1412 },
    { 0x12d1, 0x1413 },
    { 0x12d1, 0x1414 },
    { 0x12d1, 0x1416 },
    { 0x12d1, 0x1417 },
    { 0x12d1, 0x1418 },
    { 0x12d1, 0x1419 },
    { 0x12d1, 0x141A },
    { 0x12d1, 0x141B },
    { 0x12d1, 0x141E },
    { 0x12d1, 0x1420 },
    { 0x12d1, 0x1422 },
    { 0x12d1, 0x1427 },
    { 0x12d1, 0x1428 },
    { 0x12d1, 0x1429 },
    { 0x12d1, 0x142A },
    { 0x12d1, 0x142B },
    { 0x12d1, 0x1433 },
    { 0x12d1, 0x1434 },
    //HUAWEI E173 module
    { 0x12d1, 0x1436 },
    { 0x12d1, 0x1438 },
    { 0x12d1, 0x1439 },
    { 0x12d1, 0x143A },
    { 0x12d1, 0x143B },
    { 0x12d1, 0x143E },
    { 0x12d1, 0x143F },
    //HUAWEI EC315
    { 0x12d1, 0x1442 },
    { 0x12d1, 0x1448 },
    { 0x12d1, 0x144A },
    { 0x12d1, 0x144B },
    { 0x12d1, 0x144C },
    { 0x12d1, 0x143D },
    { 0x12d1, 0x144F },
    //HUAWEI K3765 dongle
    { 0x12d1, 0x1465 },
    //HUAWEI E1820 module
    { 0x12d1, 0x14ac },	
    //HUAWEI K3806-Z
    { 0x12d1, 0x14ae },
    //Vodafone (Huawei) K3770
    { 0x12d1, 0x14c9 },
    //HUAWEI E3772
    { 0x12d1, 0x14cf },
    //MU733
    { 0x12d1, 0x1506 },
    //Huawei E3131
    { 0x12d1, 0x151d },
    { 0x12d1, 0x15FF },
    //Huawei ET188
    { 0x12d1, 0x1d09 },
    //Huawei MT509
    { 0x12d1, 0x1d50 },
    //ruisibo WH700G		
    { 0x05c6, 0x6000 },
    //alcatel X220D
    { 0x1bbb, 0x0017 },
    //alcatel onetouch
    { 0x1bbb, 0x011e },
    //LINKTOP
    { 0x230d, 0x000d },
    //Philips Semiconductors
    { 0x04cc, 0x225a },
    //A-LINK 3GU
    { 0x1e0e, 0x9200 },
    //ThinkWill MI900
    { 0x19f5, 0x9013 },
    //ThinkWill MI700c
    { 0x19f5, 0x9909 },
    //USI UNA
    { 0x0e8d, 0x00a2 },
    //USI UNA(uart)
    { 0x0e8d, 0x00a0 },
    //Longcheer SU9800
    { 0x1c9e, 0x9800},
    //Longcheer SU7300U
    { 0x1c9e, 0x9e00},
    //Advan Jetz XL
    { 0x19f5, 0x9013},
    //INOVIA SEW858
    { 0x20a6, 0x1105},
    //strongrising SEW868
    { 0x21f5, 0x2012},
    //StrongRising SEV789
    { 0x21f5, 0x2008},
    //ANYDATA
    { 0x16d5, 0x6604},
    //STRONGRISING 1010
    { 0x1d09, 0x1010},
    //DLINK DWM-156
    { 0x2001, 0x7d00},
    //Tenda EVDO
    { 0x15eb, 0x0001},
    //Option GI1515
    { 0x0af0, 0xd157},
};

UsbMdmMgr::UsbMdmMgr() {
	mCurMdmVid = 0;
	mCurMdmPid = 0;
	mType = 0 ;
	mMdms = new MdmCollection();
}

UsbMdmMgr::~UsbMdmMgr() {
	delete mMdms;
	mMdms = NULL ;

       mCurMdmVid = 0;
	mCurMdmPid = 0;
	mType = 0 ;
}

int UsbMdmMgr:: check_MdmTty(void){
    DIR *pDevDir = NULL ;
    struct dirent *dirp = NULL;
    int cnt = 0 ;
    int isttyACM = MODEM_TTYUSB; ;
 
    pDevDir = opendir("/dev");
    if(pDevDir == NULL){
        SLOGW("failed to opendir(/dev),err:%s",strerror(errno));
        return -1 ;
    }
    
    while((dirp=readdir(pDevDir)) != NULL){
        if(strstr(dirp->d_name, "ttyUSB") != NULL){
            cnt++ ;
        }
        if(strstr(dirp->d_name, "ttyACM") != NULL){
            isttyACM = MODEM_TTYACM ;
            cnt++ ;
        }
    }
    closedir(pDevDir);
    if(cnt>=2){
        return isttyACM ;
    }
    return -1 ;
}


int UsbMdmMgr:: call_rild_work(int vid,int pid){
    int i = 0; 
    int ctlfd = -1 ;
    int number =0;
    char str[28];
    const char *RIL_FIFO = "/data/system/RIL_FIFO" ;

    snprintf(str,sizeof(str),"%x/%x/",vid,pid);
    number  = 10>strlen(str)? strlen(str): 10 ;

    for(i=0; i<4; i++){
        if(0==access(RIL_FIFO,  R_OK)){
            ctlfd = open(RIL_FIFO, O_NONBLOCK|O_WRONLY);
            if(ctlfd <= 0){
                SLOGW("vold failed to open(%s) ,err:%s",RIL_FIFO,strerror(errno));
                sleep(1);
            }
            else{
                if(0 > write(ctlfd, str, number)){
                    SLOGW("failed to write RIL_FIFO !!!!!!!! \n") ;
                }
                close(ctlfd) ;
                ctlfd = -1;
                break;
            }
        }
    }
    if(ctlfd<0){
        close(ctlfd);
    }
    return 0 ;
}

int UsbMdmMgr:: switchMode(int vid, int pid,const char *devpath)
{
    int i = 0;
    int ret = -1 ;
    char buf[128]={0};
    char exeBuf[256] = {0};

    SLOGW("%s,%d SetDevPath :%s \n",  __FUNCTION__, __LINE__, devpath);

    snprintf(buf,sizeof(buf),"/system/etc/usb_modeswitch.d/%.4x_%.4x",vid,pid);
    if(0>access(buf, F_OK)){
        SLOGW("%s is not existing\n",buf);
        mType=check_MdmTty() ;
        if(0<=isKnownMdmDev(vid,pid) && (isUsbMdmIn() ||mType==MODEM_TTYACM)){
            SLOGW("%x_%x is already switched to ttyACM or ttyUSB,call rild\n",vid,pid);
            goto FINAL;
        }
        return -1;
    }

    if(isUsbMdmIn()){
        return mType ; 
    }
    sleep(3);
    for(i=0; i<2;i++){
        if(0 <= (mType=check_MdmTty())){
            break;
        }
        SLOGW("UsbMdmMgr:: switchMode use %.4x_%.4x begain system %d times\n", vid,pid,i+1);
        strncpy(exeBuf,"/system/bin/usb_modeswitch -I -W -c ",sizeof(exeBuf));
        strcat(exeBuf,buf);
        system(exeBuf);
        sleep(3);
    }
    if(i == 5){
        SLOGW("usb_mode_switch failed\n");
        return -2 ;
    }
    
FINAL:    
    SetDevPath(devpath);
    if(MODEM_TTYACM == mType){
        addMdmDev(vid, pid);
    } 

    return 0;
}


int UsbMdmMgr::isUsbMdmIn(void)
{
    return mCurMdmVid>0 ? 1: mCurMdmPid>0;
}

void UsbMdmMgr::ClearDevPath(void)
{
    memset(mDevPath,0,sizeof(mDevPath));
}

int UsbMdmMgr::SetDevPath(const char *src)
{
    ClearDevPath();
    if(src != NULL){
        snprintf(mDevPath,sizeof(mDevPath),"%s",src);
    }
    return 0 ;
}

bool UsbMdmMgr::isSameParent(const char *devpath)
{
    return !strncmp(devpath,mDevPath, strlen(devpath)-3);
}

int UsbMdmMgr::isKnownMdmDev(int vid, int pid){
    //SLOGW("isKnownMdmDev num(%d)", sizeof(g_KnownMdms)/sizeof(MdmMgrInfo));
	for(int i = 0; i< sizeof(g_KnownMdms)/sizeof(MdmMgrInfo); i++){
        if((vid == g_KnownMdms[i].vid) && (pid ==g_KnownMdms[i].pid))
            return i;
    }
    return -1;
}

int UsbMdmMgr::addMdmDev( int vid, int pid ){
    int index = isKnownMdmDev(vid,pid);
    if(index < 0)
    {
        SLOGW("addMdmDev don't find a match usb device(%x_%x) in isKnownMdmDev\n",vid,pid);
        return -1;
    }
    if(mCurMdmVid == vid && mCurMdmPid == pid)
        return -1;

    UsbModem *mdm = NULL;
    mdm = new UsbModem(vid,pid);
    mMdms->push_back(mdm);

    if(mCurMdmVid==0 && mCurMdmPid ==0){
        SLOGW("addMdmDev vid(0x%x) pid(0x%x) index(%d)", vid, pid,index);

        char value[PROPERTY_VALUE_MAX];
        snprintf(value, sizeof(value), "%d", vid);
        property_set(RIL_PROP_USBMODEM_VID, value);

        snprintf(value, sizeof(value), "%d", pid);
        property_set(RIL_PROP_USBMODEM_PID, value);

        mCurMdmVid = vid;
        mCurMdmPid = pid;
    }

    call_rild_work(vid,pid);

    return 0 ;
}

void UsbMdmMgr::delMdmDev(  int vid, int pid ){
    if(mCurMdmVid == vid && mCurMdmPid ==pid){
        mCurMdmVid = 0;
        mCurMdmPid = 0;
        mType = 0;

        //ClearDevPath();
        property_set(RIL_PROP_USBMODEM_VID, "0");
        property_set(RIL_PROP_USBMODEM_PID, "0");
    }

    MdmCollection::iterator iter;
    for (iter = mMdms->begin(); iter != mMdms->end(); ++iter) {
        if (((*iter)->getVid() == vid)&&((*iter)->getPid() == pid)) {
            mMdms->erase(iter);
            break;
        }
    }
}


