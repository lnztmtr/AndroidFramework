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

#ifndef _USB_MODEM_MGR_H_
#define _USB_MODEM_MGR_H_

#include <utils/List.h>
#include "UsbModem.h"

#define  VENDOR_SPEC_CLASS_ID "255"

#define RIL_PROP_EMBEDDED_MODEM               "ril.hw.embedded.modem"

#define RIL_PROP_USBMODEM_VID                 "ril.usbmodem.vid"
#define RIL_PROP_USBMODEM_PID                 "ril.usbmodem.pid"

#define MODEM_PROPHET                         "/dev/ttyS20"

#define MODEM_TTYUSB (0)
#define MODEM_TTYACM (1)
#define MODEM_PCIE   (2)

class UsbMdmMgr {

public:
    UsbMdmMgr();
    ~UsbMdmMgr();
    int check_MdmTty(void);
    int switchMode(int vid, int pid,const char *devpath);    // usb switch mode
    bool isSameParent(const char *devpath);    // jduge this devpath same as usb dongle?
    int addMdmDev(int vid, int pid );
    void delMdmDev(int vid, int pid );
    int isUsbMdmIn(void);
    int call_rild_work(int vid,int pid);
    void ClearDevPath(void);
    int SetDevPath(const char *src);
    
private:
    int isKnownMdmDev(int vid, int pid);


private:
    int mType;
    int mCurMdmVid;
    int mCurMdmPid;
    char mDevPath[256];
    MdmCollection *mMdms;
};


#endif
