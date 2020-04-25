/*
 * Copyright (C) 2011 Amlogic, Inc.
 *
 *  Usb Modem Device
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

#ifndef _USB_MODEM_H_
#define _USB_MODEM_H_

#include <utils/List.h>


class UsbModem {
public:
	UsbModem(int vid, int pid);
	~UsbModem();
	int getVid();
	int getPid();
private:
	int m_vid;
	int m_pid;
};

typedef android::List<UsbModem *> MdmCollection;

#endif
