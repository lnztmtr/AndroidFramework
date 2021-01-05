/*
 * drivers/amlogic/amports/arch/m8_m8m2/h265/vh265_mc.c
 *
 * Copyright (C) 2015 Amlogic, Inc. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.
 *
*/

#include "../firmware_def.h"
#include "../../../vavs2.h"

#define VERSTR "00000001"

#define MicroCode vavs2_mc
#ifndef AVS2_10B_MMU
#include "avs2_linux.h"
#else
#include "avs2_mmu_linux.h"
#endif

#define FOR_VFORMAT VFORMAT_AVS2

#define REG_FIRMWARE_ALL()\
		DEF_FIRMWARE_VER(vavs2_mc, VERSTR);\

INIT_DEF_FIRMWARE();

