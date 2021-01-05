/*
 * drivers/amlogic/amports/arch/ucode/h264_enc/h264_enc_gx.h
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

const u32 MicroCode[] __initconst = {
	0x06810001, 0x06800000, 0x0d000001, 0x07400040, 0x0c000980,
	0x00000000, 0x0c01ad40, 0x00000000, 0x0c000fc0, 0x00000000,
	0x06bffc40, 0x07c00000, 0x06030400, 0x00400000, 0x00000000,
	0x00000000, 0x0c79fa00, 0x00000000, 0x00000000, 0x00000000,
	0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
	0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
	0x00000000, 0x00000000, 0x0c799000, 0x00000000, 0x0c78e680,
	0x064d3008, 0x0c780a80, 0x00000000, 0x0c799040, 0x00000000,
	0x0cc00000, 0x00400000, 0x080d1a00, 0x06800008, 0x07c003c8,
	0x0cc00000, 0x00000000, 0x06800022, 0x06804008, 0x07c00808,
	0x07c008c8, 0x06800008, 0x07c00888, 0x064ac808, 0x07c00108,
	0x064ac908, 0x07c00148, 0x064ac608, 0x07c00188, 0x064ac508,
	0x07c001c8, 0x064ac708, 0x07c00208, 0x064aca08, 0x07c00388,
	0x064acb08, 0x07c00048, 0x064ace08, 0x07c00508, 0x0cc00000,
	0x00000000, 0x0cc00000, 0x00000000, 0x080d2001, 0x064d2008,
	0x0befc048, 0x0cc00000, 0x00000000, 0x06bfff88, 0x06030408,
	0x00400000, 0x064ac008, 0x0aa44048, 0x0a60c088, 0x0c781840,
	0x00000000, 0x078003c9, 0x0a60c009, 0x0c002940, 0x00000000,
	0x0a60c108, 0x0c785780, 0x00000000, 0x0a6100c8, 0x00000000,
	0x0c782b00, 0x00000000, 0x0c78ae40, 0x00000000, 0x0c07f300,
	0x00000000, 0x06bc0008, 0x060d1f08, 0x064d1e08, 0x09c087c8,
	0x060d1e08, 0x06800008, 0x06c00408, 0x060d1f08, 0x06800048,
	0x06c00408, 0x060d1f08, 0x06a19c08, 0x060d1f08, 0x064d1e08,
	0x098087c8, 0x060d1e08, 0x06a10808, 0x060d1f08, 0x06a00008,
	0x060d1f08, 0x06a0a008, 0x060d1f08, 0x0c07f380, 0x080d2100,
	0x07800148, 0x04404208, 0x0c07f280, 0x060d2108, 0x0c07f200,
	0x080d2100, 0x07800108, 0x04404208, 0x0c07f100, 0x060d2108,
	0x0c07f080, 0x080d2101, 0x080d1f20, 0x064d3109, 0x09010209,
	0x0400f208, 0x05804208, 0x04401208, 0x07c00308, 0x0c07ee40,
	0x060d2108, 0x09210209, 0x0400f208, 0x05804208, 0x04401208,
	0x07c00348, 0x0c07ec80, 0x060d2108, 0x080d1f30, 0x080d1f30,
	0x064d3108, 0x09210208, 0x09004248, 0x0a60c009, 0x0c7803c0,
	0x080d1f20, 0x06800408, 0x06c00008, 0x02409248, 0x05801249,
	0x080d1f30, 0x0c07e8c0, 0x080d2100, 0x0c07e840, 0x080d2100,
	0x0c07e7c0, 0x080d2100, 0x0c07e740, 0x060d2109, 0x080d1f20,
	0x080d1f30, 0x06bc0008, 0x060d1f08, 0x080ac007, 0x0c7899c0,
	0x08007801, 0x0c07de80, 0x00000000, 0x06bc0008, 0x060d1f08,
	0x064d1e08, 0x09c087c8, 0x060d1e08, 0x06800008, 0x06c00408,
	0x060d1f08, 0x06800048, 0x06c00408, 0x060d1f08, 0x06a1a008,
	0x060d1f08, 0x064d1e08, 0x098087c8, 0x060d1e08, 0x0c07e080,
	0x080d2100, 0x0c07e000, 0x080d2100, 0x080d1f20, 0x080d1f20,
	0x0c07df00, 0x080d2100, 0x0c07de80, 0x080d2100, 0x0c07de00,
	0x080d2100, 0x080d1f20, 0x080d1f40, 0x07800048, 0x0c009200,
	0x0441a208, 0x0c07dc40, 0x060d2108, 0x0c009100, 0x06800008,
	0x0c07db40, 0x060d2108, 0x0c07dac0, 0x080d2100, 0x080d1f20,
	0x080d5500, 0x080d1f20, 0x080d1f20, 0x080d1f30, 0x06bc0008,
	0x060d1f08, 0x00000000, 0x00000000, 0x064d1608, 0x0befc108,
	0x00000000, 0x06800008, 0x06e00008, 0x060d1c08, 0x080d1d00,
	0x00000000, 0x00000000, 0x064d1608, 0x0befc108, 0x064d1b08,
	0x09808648, 0x060d1b08, 0x064d1608, 0x0befc108, 0x00000000,
	0x080ac008, 0x0c7887c0, 0x08007801, 0x064d3109, 0x09010289,
	0x0400f28a, 0x0580428a, 0x0440128a, 0x07c0030a, 0x09210289,
	0x0400f28a, 0x0580428a, 0x0440128a, 0x07c0034a, 0x06800009,
	0x0cc00000, 0x07c003c9, 0x0c016f00, 0x00000000, 0x080f3eff,
	0x06490b08, 0x09c08608, 0x09c08648, 0x06090b08, 0x06400908,
	0x09c08748, 0x09c08788, 0x06000908, 0x06400808, 0x09808008,
	0x06000808, 0x09c08008, 0x06000808, 0x0c0173c0, 0x00000000,
	0x0c016a00, 0x00000000, 0x0c07c400, 0x00000000, 0x06400f08,
	0x09808708, 0x06000f08, 0x064f4908, 0x09c08048, 0x060f4908,
	0x06bc0008, 0x060d1f08, 0x064d1e08, 0x09c087c8, 0x060d1e08,
	0x06800008, 0x06c00408, 0x060d1f08, 0x06800048, 0x06c00408,
	0x060d1f08, 0x06a19408, 0x060d1f08, 0x064d1e08, 0x098087c8,
	0x060d1e08, 0x064d3308, 0x09008248, 0x07c00089, 0x09108248,
	0x07c000c9, 0x09210208, 0x0c07c300, 0x060d2108, 0x0c07c280,
	0x080d2107, 0x064d3109, 0x09010209, 0x0400f208, 0x05804208,
	0x04401208, 0x07c00308, 0x09210209, 0x0400f208, 0x05804208,
	0x04401208, 0x07c00348, 0x06800008, 0x07c00248, 0x060f1b08,
	0x07800309, 0x0946d209, 0x060d3208, 0x06940008, 0x06c00008,
	0x064ac109, 0x02008248, 0x060f2008, 0x06803908, 0x064d3309,
	0x09008249, 0x09508209, 0x09808508, 0x09808548, 0x098087c8,
	0x060f1f08, 0x064ad508, 0x07c00448, 0x0ae0c008, 0x04001208,
	0x06800008, 0x07c00488, 0x06800008, 0x07c00408, 0x07800308,
	0x04001208, 0x07c004c8, 0x07800309, 0x07800348, 0x0960f248,
	0x060f1e09, 0x0c07b6c0, 0x080d2100, 0x07800148, 0x07800189,
	0x0680040a, 0x0240a20a, 0x03409289, 0x09605248, 0x060d1f09,
	0x078001c8, 0x0c07b440, 0x060d2108, 0x07800108, 0x07800209,
	0x0680040a, 0x0240a20a, 0x03409289, 0x09605248, 0x060d1f09,
	0x080d1f20, 0x080d1f20, 0x064f1c08, 0x09206208, 0x07800049,
	0x0c006600, 0x02408248, 0x0c07b040, 0x060d2108, 0x080d3d00,
	0x064acd08, 0x060f4208, 0x0c006640, 0x00000000, 0x0680f203,
	0x06819a84, 0x080d300f, 0x064d0008, 0x09808008, 0x09808048,
	0x09808088, 0x09c080c8, 0x09c08148, 0x09c08188, 0x09c081c8,
	0x060d0008, 0x064acf08, 0x060f4808, 0x06808089, 0x06d00009,
	0x07800508, 0x09485248, 0x06c0000a, 0x04820288, 0x0aa2c00a,
	0x00000000, 0x09809289, 0x098092c9, 0x09809309, 0x09809349,
	0x09809389, 0x098093c9, 0x09809409, 0x09809449, 0x09809489,
	0x06c0000a, 0x04840288, 0x0aa1400a, 0x00000000, 0x098094c9,
	0x09809509, 0x09809549, 0x060f4009, 0x080ac005, 0x0c7858c0,
	0x08007801, 0x0c014e40, 0x00000000, 0x06493008, 0x06800009,
	0x09410209, 0x06093008, 0x060d2608, 0x064f4008, 0x09c08048,
	0x060f4008, 0x080f1f00, 0x080f3eff, 0x064d0008, 0x09c08088,
	0x060d0008, 0x09808088, 0x060d0008, 0x064d2508, 0x09808188,
	0x098081c8, 0x09808288, 0x060d2508, 0x064f3408, 0x098085c8,
	0x09808508, 0x09808448, 0x09808408, 0x098083c8, 0x09808148,
	0x060f3408, 0x080f3f08, 0x064acf08, 0x060f4808, 0x06808089,
	0x06d00009, 0x07800508, 0x09485248, 0x06c00008, 0x04820208,
	0x0aa2c008, 0x00000000, 0x09809289, 0x098092c9, 0x09809309,
	0x09809349, 0x09809389, 0x098093c9, 0x09809409, 0x09809449,
	0x09809489, 0x07800508, 0x06c00008, 0x04840208, 0x0aa14008,
	0x00000000, 0x098094c9, 0x09809509, 0x09809549, 0x060f4009,
	0x0c0134c0, 0x00000000, 0x06490b08, 0x09c08608, 0x09808648,
	0x06090b08, 0x06400908, 0x09808748, 0x09c08788, 0x06000908,
	0x06400808, 0x09808008, 0x06000808, 0x09c08008, 0x06000808,
	0x0c0139c0, 0x00000000, 0x0c013000, 0x00000000, 0x0c078a00,
	0x00000000, 0x06400f08, 0x09808708, 0x06000f08, 0x064f4308,
	0x09808008, 0x09c08048, 0x09808088, 0x09c080c8, 0x060f4308,
	0x09c08008, 0x09808048, 0x09c08088, 0x098080c8, 0x060f4308,
	0x064f3b08, 0x098087c8, 0x060f3b08, 0x09c087c8, 0x060f3b08,
	0x064f1508, 0x09c08508, 0x09c084c8, 0x09808748, 0x09c08448,
	0x09c08488, 0x09c08348, 0x09c08308, 0x09808248, 0x09808008,
	0x060f1508, 0x09808508, 0x098084c8, 0x060f1508, 0x080d3400,
	0x080f4a00, 0x09808488, 0x09808448, 0x09808348, 0x09808308,
	0x098085c8, 0x09808608, 0x09808648, 0x09c08688, 0x098086c8,
	0x09c08248, 0x09c08008, 0x060f1508, 0x064f4908, 0x09c08048,
	0x09c081c8, 0x09808008, 0x060f4908, 0x098087c8, 0x09808788,
	0x09808748, 0x098086c8, 0x09808688, 0x09808648, 0x09808608,
	0x09808308, 0x09808288, 0x09c08248, 0x098081c8, 0x09808088,
	0x09808048, 0x060f4908, 0x068000c8, 0x068000c9, 0x09484209,
	0x068000c9, 0x09504209, 0x06800049, 0x09581209, 0x06800009,
	0x095a1209, 0x06800049, 0x095c5209, 0x060f5908, 0x06800388,
	0x06800489, 0x09508209, 0x06800149, 0x09604209, 0x068000c9,
	0x09684209, 0x06800109, 0x09704209, 0x06800089, 0x09704209,
	0x060f5a08, 0x06800008, 0x06800009, 0x09508209, 0x06800809,
	0x09608209, 0x06801409, 0x09708209, 0x060f5b08, 0x06801008,
	0x06800009, 0x09508209, 0x06801809, 0x09608209, 0x06803009,
	0x09708209, 0x060f5c08, 0x06800608, 0x06801409, 0x09508209,
	0x06800809, 0x09608209, 0x06801809, 0x09708209, 0x060f5d08,
	0x068000c8, 0x06801809, 0x0948c209, 0x06800249, 0x09607209,
	0x06800009, 0x096e1209, 0x06800c09, 0x09708209, 0x060f5e08,
	0x068000c8, 0x06800009, 0x09461209, 0x06800049, 0x09481209,
	0x06800009, 0x094a2209, 0x06803009, 0x0950c209, 0x06801c09,
	0x0968c209, 0x060f5f08, 0x064d3109, 0x09010209, 0x0400f208,
	0x05804208, 0x04401208, 0x07c00308, 0x09210209, 0x0400f208,
	0x05804208, 0x04401208, 0x07c00348, 0x07800309, 0x07800348,
	0x0958c248, 0x06499008, 0x09708248, 0x060f4f09, 0x06bc0008,
	0x060d1f08, 0x064d1e08, 0x09c087c8, 0x060d1e08, 0x06800008,
	0x06c00408, 0x060d1f08, 0x06800048, 0x06c00408, 0x060d1f08,
	0x06a10408, 0x060d1f08, 0x064d1e08, 0x098087c8, 0x060d1e08,
	0x064d3308, 0x09008248, 0x07c00089, 0x09108248, 0x07c000c9,
	0x09210208, 0x0c076440, 0x060d2108, 0x0c0763c0, 0x080d2105,
	0x06800048, 0x07c00248, 0x060f1b08, 0x07800309, 0x0946d209,
	0x060d3208, 0x06803908, 0x064d3309, 0x09008249, 0x09508209,
	0x09808508, 0x09808548, 0x098087c8, 0x060f1f08, 0x064ad508,
	0x07c00448, 0x0ae0c008, 0x04001208, 0x06800008, 0x07c00488,
	0x06800008, 0x07c00408, 0x07800308, 0x04001208, 0x07c004c8,
	0x06bfffc8, 0x060d3708, 0x07800309, 0x07800348, 0x0960f248,
	0x060f1e09, 0x0c075b80, 0x080d2100, 0x07800148, 0x07800189,
	0x0680040a, 0x0240a20a, 0x03409289, 0x09605248, 0x060d1f09,
	0x07800108, 0x07800209, 0x0680040a, 0x0240a20a, 0x03409289,
	0x09605248, 0x060d1f09, 0x080d1f20, 0x080d1f20, 0x080d1f20,
	0x064f1d08, 0x09206208, 0x04000248, 0x095a6248, 0x060f3c09,
	0x07800049, 0x0c000a80, 0x02408248, 0x0c0754c0, 0x060d2108,
	0x080d3d00, 0x080d5300, 0x064acd08, 0x060f4208, 0x0c001880,
	0x00000000, 0x06813c83, 0x06819a84, 0x080d300f, 0x06800008,
	0x06c00448, 0x060d2f08, 0x064d0008, 0x09c08008, 0x09c08048,
	0x09c08088, 0x09c08188, 0x09c081c8, 0x060d0008, 0x09808008,
	0x09808048, 0x09808088, 0x098080c8, 0x09808148, 0x09808188,
	0x098081c8, 0x060d0008, 0x06800048, 0x07800309, 0x0948c209,
	0x060f4b08, 0x080ac005, 0x0c780080, 0x08007801, 0x00800000,
	0x07800008, 0x0c7f4300, 0x06030408, 0x06800009, 0x0b005248,
	0x02409209, 0x05401208, 0x0cc00000, 0x04401208, 0x0cc00000,
	0x05401209, 0x06bfffda, 0x06c0001a, 0x06800009, 0x0680001b,
	0x06c0001b, 0x0400071b, 0x0680000b, 0x0680c00e, 0x0680100f,
	0x0740039a, 0x0400138e, 0x07400389, 0x0400138e, 0x0740039b,
	0x0400138e, 0x0740038b, 0x0400138e, 0x0aee004f, 0x044013cf,
	0x0690000e, 0x06c0000e, 0x064ac10f, 0x0200e3ce, 0x0680000f,
	0x0603510e, 0x0680400d, 0x0603520d, 0x06a0c00d, 0x0603500d,
	0x0643500d, 0x0580f34d, 0x0bef804d, 0x00000000, 0x040403cf,
	0x0b611e8f, 0x0680800d, 0x0c7ffd00, 0x0200e34e, 0x064d330d,
	0x0900834d, 0x0680000e, 0x0680100f, 0x0b8053cd, 0x00000000,
	0x0404038e, 0x0c7fff40, 0x040403cf, 0x07c0028e, 0x044013cf,
	0x07c002cf, 0x060d371a, 0x060d391b, 0x060d381a, 0x060d3a1b,
	0x0cc00000, 0x00000000, 0x08098002, 0x08098000, 0x06a0001d,
	0x06d0001d, 0x06a0001e, 0x06d0001e, 0x06a0001f, 0x06d0001f,
	0x06a00020, 0x06d00020, 0x0690000a, 0x06bfffda, 0x06c0001a,
	0x0400085a, 0x0680000d, 0x0680001b, 0x06c0001b, 0x0400071b,
	0x06800009, 0x0680c00e, 0x0680054f, 0x0740039a, 0x0400138e,
	0x0740038d, 0x0400138e, 0x0740039b, 0x0400138e, 0x07400389,
	0x0400138e, 0x0740039d, 0x0400138e, 0x0740038a, 0x0400138e,
	0x0740039e, 0x0400138e, 0x0740038a, 0x0400138e, 0x0740039f,
	0x0400138e, 0x0740038a, 0x0400138e, 0x074003a0, 0x0400138e,
	0x0740038a, 0x0400138e, 0x0aea004f, 0x044013cf, 0x0690000e,
	0x06c0000e, 0x064ac10f, 0x0200e3ce, 0x0680000f, 0x0603510e,
	0x06803f0d, 0x0603520d, 0x06a0c00d, 0x0603500d, 0x0643500d,
	0x0580f34d, 0x0bef804d, 0x00000000, 0x040153cf, 0x0b611e8f,
	0x06807e0d, 0x0c7ffd00, 0x0200e34e, 0x064d330d, 0x0900834d,
	0x0680000e, 0x0680054f, 0x0b8053cd, 0x00000000, 0x0401538e,
	0x0c7fff40, 0x040153cf, 0x07c0028e, 0x044013cf, 0x07c002cf,
	0x060d371a, 0x060d391b, 0x060d4a1d, 0x060d4b1e, 0x060d4c1f,
	0x060d4d20, 0x060d4a1d, 0x060d4a1d, 0x060d381a, 0x060d3a1b,
	0x060d4e1d, 0x060d4f1e, 0x060d501f, 0x060d5120, 0x0cc00000,
	0x00000000, 0x0aa28008, 0x064d1e0e, 0x064d1a0f, 0x0947d38f,
	0x0240f88e, 0x07c0084f, 0x0400088e, 0x064d370e, 0x0cb80003,
	0x064d390f, 0x0c7f1880, 0x00000000, 0x078004c8, 0x0b214048,
	0x04401208, 0x07c004c8, 0x06bfffc9, 0x060d3709, 0x07800488,
	0x0aa38008, 0x04401208, 0x0a630008, 0x07c00488, 0x0c00ce80,
	0x00000000, 0x06bfffc9, 0x060d3709, 0x060d3809, 0x064ad508,
	0x07c00488, 0x07800308, 0x04001208, 0x07c004c8, 0x064d3533,
	0x064d3e08, 0x09104208, 0x09784cc8, 0x064d3b34, 0x064d3c35,
	0x060d371a, 0x064d3510, 0x0908c250, 0x0a620009, 0x060d391b,
	0x06bfffc9, 0x06c00009, 0x060d3809, 0x06800009, 0x06c00009,
	0x060d3a09, 0x09004250, 0x0aa1c249, 0x080d3601, 0x080d3603,
	0x080d3605, 0x080d3606, 0x0c7801c0, 0x00000000, 0x080d3602,
	0x080d3603, 0x080d3604, 0x080d3605, 0x080d3606, 0x064d3708,
	0x064d3e09, 0x080d360f, 0x0780080a, 0x0680400b, 0x0a8152ca,
	0x0780084b, 0x0740028b, 0x06807ccb, 0x0b8112ca, 0x0400128a,
	0x0680400b, 0x0240c2ca, 0x0603520c, 0x064ad20a, 0x0603510a,
	0x06a0400b, 0x0603500b, 0x054012cc, 0x0200a2ca, 0x060ad20a,
	0x0643500b, 0x0580f2cb, 0x0bef804b, 0x00000000, 0x0680400a,
	0x09208333, 0x090882f3, 0x0950830b, 0x0740028c, 0x0400128a,
	0x09384333, 0x090042f3, 0x0950830b, 0x0740028c, 0x0400128a,
	0x09210334, 0x0740028c, 0x0400128a, 0x074002b4, 0x0400128a,
	0x09210335, 0x0740028c, 0x0400128a, 0x074002b5, 0x0400128a,
	0x09346208, 0x09508248, 0x07400289, 0x0400128a, 0x078008c8,
	0x04001208, 0x07c008c8, 0x04803208, 0x064f6809, 0x0aa28008,
	0x064f640b, 0x064f6509, 0x0aa1c048, 0x064f610b, 0x064f6609,
	0x0aa10088, 0x064f620b, 0x064f6709, 0x064f630b, 0x07400289,
	0x0400128a, 0x0740028b, 0x0400128a, 0x06800009, 0x07400289,
	0x0400128a, 0x07400289, 0x0400128a, 0x07c0080a, 0x0908c210,
	0x0680c10a, 0x07800289, 0x0a814248, 0x02409248, 0x04401249,
	0x05402249, 0x0680c00a, 0x0200a289, 0x0740028e, 0x0400128a,
	0x0581038e, 0x0740028e, 0x0400128a, 0x0740028f, 0x0400128a,
	0x058103cf, 0x0740028f, 0x0400528a, 0x078002c9, 0x0b40d248,
	0x07800309, 0x0b40b248, 0x0700029a, 0x0400128a, 0x0700028e,
	0x0961068e, 0x0400128a, 0x0700029b, 0x0400128a, 0x0700028f,
	0x0c7ef0c0, 0x096106cf, 0x0908c210, 0x07800289, 0x02409248,
	0x05402249, 0x0680c00a, 0x0200a289, 0x064d0009, 0x09384249,
	0x0a6f8009, 0x00000000, 0x064d3609, 0x0a6ec009, 0x00000000,
	0x064d370e, 0x064d390f, 0x0740028e, 0x0400128a, 0x0581038e,
	0x0740028e, 0x0400128a, 0x0740028f, 0x0400128a, 0x058103cf,
	0x0740028f, 0x0400128a, 0x0690000e, 0x06c0000e, 0x064ac10f,
	0x0200e3ce, 0x0780028f, 0x054033cf, 0x0200e3ce, 0x0603510e,
	0x0680400d, 0x0603520d, 0x06a0c00d, 0x0603500d, 0x0908c210,
	0x07800309, 0x0b848248, 0x04001208, 0x0920c210, 0x07800349,
	0x0b844248, 0x06800008, 0x080d3000, 0x080d1f30, 0x06bc0008,
	0x060d1f08, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
	0x00000000, 0x00000000, 0x064d1608, 0x0befc108, 0x00000000,
	0x06800008, 0x06e00008, 0x060d1c08, 0x080d1d00, 0x00000000,
	0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
	0x064d1608, 0x0befc108, 0x00000000, 0x064d1b08, 0x09808648,
	0x060d1b08, 0x064d1608, 0x0befc108, 0x00000000, 0x0643500b,
	0x0580f2cb, 0x0bef804b, 0x00000000, 0x0780080a, 0x0780084b,
	0x0740028b, 0x0400128a, 0x0680400b, 0x0240c2ca, 0x0603520c,
	0x064ad20a, 0x0603510a, 0x06a0400b, 0x0603500b, 0x0643500b,
	0x0580f2cb, 0x0bef804b, 0x00000000, 0x07800188, 0x04001208,
	0x07c00188, 0x07800208, 0x04002208, 0x07c00208, 0x080ac009,
	0x06800022, 0x06804008, 0x07c00808, 0x07c008c8, 0x06800008,
	0x07c00888, 0x0c7ed500, 0x08007801, 0x07c00288, 0x0403f248,
	0x07c002c9, 0x05403208, 0x0690000e, 0x06c0000e, 0x064ac10f,
	0x0200e3ce, 0x0200e20e, 0x0680400f, 0x06b0c010, 0x0643500d,
	0x0580f34d, 0x0bef804d, 0x00000000, 0x0643530d, 0x0900c34d,
	0x0a6f800d, 0x00000000, 0x0603510e, 0x0603520f, 0x06035010,
	0x0643500d, 0x0580f34d, 0x0bef804d, 0x00000000, 0x0c7fdb40,
	0x0680c00a, 0x078004c8, 0x0b224048, 0x04401208, 0x07c004c8,
	0x06a00089, 0x06f00009, 0x060d4c09, 0x0b20c048, 0x00000000,
	0x060d4d09, 0x07800488, 0x0aa44008, 0x04401208, 0x0a63c008,
	0x07c00488, 0x0c009300, 0x00000000, 0x06bfffc9, 0x060d3809,
	0x06a00089, 0x06f00009, 0x060d4c09, 0x060d4d09, 0x064ad508,
	0x07c00488, 0x07800308, 0x04001208, 0x07c004c8, 0x064d4a11,
	0x064d4b12, 0x064d4c13, 0x064d4d14, 0x060d4a1d, 0x060d4b1e,
	0x060d4c1f, 0x060d4d20, 0x060d371a, 0x064d3510, 0x0908c250,
	0x0a638009, 0x060d391b, 0x06a00009, 0x06d00009, 0x060d4e09,
	0x060d4f09, 0x060d5009, 0x060d5109, 0x06bfffc9, 0x06c00009,
	0x060d3809, 0x06800009, 0x06c00009, 0x060d3a09, 0x064d3533,
	0x09004250, 0x0b62c249, 0x080d3607, 0x080d3601, 0x0c009a80,
	0x080d3608, 0x080d3609, 0x080d3604, 0x080d3605, 0x080d3606,
	0x0c780500, 0x00000000, 0x064d3e08, 0x09104208, 0x09784cc8,
	0x064d3b34, 0x064d3c35, 0x080d5201, 0x0aa1c249, 0x080d3601,
	0x080d3603, 0x080d3605, 0x080d3606, 0x0c7801c0, 0x00000000,
	0x080d3602, 0x080d3603, 0x080d3604, 0x080d3605, 0x080d3606,
	0x064d0009, 0x09384249, 0x0aaf8209, 0x00000000, 0x09004373,
	0x064d3e09, 0x064d3708, 0x080d360f, 0x0780080a, 0x0680400b,
	0x0a8172ca, 0x0780084b, 0x0740028b, 0x0a60c20d, 0x06807ccb,
	0x068075cb, 0x0b8112ca, 0x0400128a, 0x0680400b, 0x0240c2ca,
	0x0603520c, 0x064ad20a, 0x0603510a, 0x06a0400b, 0x0603500b,
	0x054012cc, 0x0200a2ca, 0x060ad20a, 0x0643500b, 0x0580f2cb,
	0x0bef804b, 0x00000000, 0x0680400a, 0x09208333, 0x090882f3,
	0x0950830b, 0x0740028c, 0x0400128a, 0x09384333, 0x064d3533,
	0x090042f3, 0x0950830b, 0x0740028c, 0x0400128a, 0x0b66024d,
	0x09210323, 0x0740028c, 0x0400128a, 0x074002a3, 0x0400128a,
	0x0aa300cd, 0x0a61820d, 0x0921032b, 0x0c008c40, 0x00000000,
	0x0c7805c0, 0x00000000, 0x0740028c, 0x0400128a, 0x074002ab,
	0x0c780480, 0x0400128a, 0x09210327, 0x0740028c, 0x0400128a,
	0x074002a7, 0x0c780300, 0x0400128a, 0x09210334, 0x0740028c,
	0x0400128a, 0x074002b4, 0x0400128a, 0x09210335, 0x0740028c,
	0x0400128a, 0x074002b5, 0x0400128a, 0x09346208, 0x09508248,
	0x07400289, 0x0400128a, 0x078008c8, 0x04001208, 0x07c008c8,
	0x04803208, 0x064f6809, 0x0aa28008, 0x064f640b, 0x064f6509,
	0x0aa1c048, 0x064f610b, 0x064f6609, 0x0aa10088, 0x064f620b,
	0x064f6709, 0x064f630b, 0x07400289, 0x0400128a, 0x0740028b,
	0x0400128a, 0x06800009, 0x07400289, 0x0400128a, 0x07400289,
	0x0400128a, 0x07c0080a, 0x0908c210, 0x0aa10008, 0x0680bfca,
	0x0c000f00, 0x04401208, 0x0908c210, 0x07800309, 0x0b82c248,
	0x00000000, 0x064d0009, 0x09384249, 0x0a6f8009, 0x00000000,
	0x064d3609, 0x0a6ec009, 0x00000000, 0x0920c290, 0x07800349,
	0x0b80c24a, 0x00000000, 0x064d5309, 0x0aa1c009, 0x00000000,
	0x060d2109, 0x080d2001, 0x064d2009, 0x0befc049, 0x00000000,
	0x0c7fb1c0, 0x00000000, 0x064d370e, 0x064d390f, 0x064d4a11,
	0x064d4b12, 0x064d4c13, 0x064d4d14, 0x0c0006c0, 0x07800308,
	0x0c000f80, 0x06800011, 0x06a00014, 0x06d00014, 0x060d4d14,
	0x0c001900, 0x0680c00a, 0x060d4a1d, 0x060d4b1e, 0x060d4c1f,
	0x060d4d20, 0x0c780280, 0x0680c30a, 0x0401928a, 0x0908c210,
	0x04002208, 0x078002c9, 0x0b004248, 0x0c000b00, 0x0908c450,
	0x0680c60a, 0x0c001500, 0x00000000, 0x0c7e8e80, 0x00000000,
	0x07800289, 0x0240a248, 0x0540324a, 0x0540228a, 0x02009289,
	0x0680c00a, 0x0200a289, 0x0740028e, 0x0400128a, 0x0581038e,
	0x0740028e, 0x0400128a, 0x0740028f, 0x0400128a, 0x058103cf,
	0x0740028f, 0x0400128a, 0x07400291, 0x0400128a, 0x05810451,
	0x07400291, 0x0400128a, 0x07400292, 0x0400128a, 0x05810492,
	0x07400292, 0x0400128a, 0x07400293, 0x0400128a, 0x058104d3,
	0x07400293, 0x0400128a, 0x07400294, 0x0400128a, 0x05810514,
	0x0cc00000, 0x07400294, 0x0690000e, 0x06c0000e, 0x064ac10f,
	0x0200e3ce, 0x0780028f, 0x0540434f, 0x054033cf, 0x0200f34f,
	0x0200e3ce, 0x0603510e, 0x06803f0d, 0x0603520d, 0x06a0c00d,
	0x0603500d, 0x07c00291, 0x04014251, 0x07c002c9, 0x05404251,
	0x05403211, 0x02008248, 0x0690000e, 0x06c0000e, 0x064ac10f,
	0x0200e3ce, 0x0200e20e, 0x06803f0f, 0x06b0c010, 0x0643500d,
	0x0580f34d, 0x0bef804d, 0x00000000, 0x0643530d, 0x0900c34d,
	0x0a6f800d, 0x00000000, 0x0603510e, 0x0603520f, 0x06035010,
	0x0643500d, 0x0580f34d, 0x0bef804d, 0x0cc00000, 0x00000000,
	0x040006a1, 0x070002a1, 0x0400128a, 0x07000289, 0x09610849,
	0x0400128a, 0x040006dc, 0x0700029c, 0x0400128a, 0x07000289,
	0x09610709, 0x0400128a, 0x0700029d, 0x0400128a, 0x07000289,
	0x09610749, 0x0400128a, 0x0700029e, 0x0400128a, 0x07000289,
	0x09610789, 0x0400128a, 0x0700029f, 0x0400128a, 0x07000289,
	0x096107c9, 0x0400128a, 0x070002a0, 0x0400128a, 0x07000289,
	0x0cc00000, 0x09610809, 0x064d1602, 0x09162082, 0x0aa0c002,
	0x0c7fff40, 0x00000000, 0x0c7e70c0, 0x00000000, 0x080f3601,
	0x0cb80004, 0x064f3e08, 0x0be0c088, 0x0c780e40, 0x00000000,
	0x080f3e02, 0x064f1f08, 0x09361248, 0x0aa10009, 0x09042248,
	0x0c7e6d80, 0x00000000, 0x0aa14009, 0x0680324a, 0x0aa0c049,
	0x0680348a, 0x0680390a, 0x07800088, 0x07800309, 0x0b809248,
	0x04001208, 0x078000c9, 0x07800348, 0x0b803209, 0x04001249,
	0x06800009, 0x07c000c9, 0x06800008, 0x07c00088, 0x0950c288,
	0x0a60c008, 0x07800309, 0x0980a50a, 0x0a403248, 0x00000000,
	0x0980a58a, 0x078000c8, 0x0a60c008, 0x060f4108, 0x0980a54a,
	0x07800408, 0x0ac03248, 0x04001208, 0x0980a5ca, 0x0ac03248,
	0x07c00408, 0x0980a54a, 0x07800448, 0x0aa28008, 0x04401208,
	0x0a620008, 0x07c00448, 0x0980a50a, 0x0980a54a, 0x064ad508,
	0x07c00448, 0x06800008, 0x07c00408, 0x0980a7ca, 0x060f1f0a,
	0x064f3e08, 0x0be0c108, 0x0c7e6080, 0x00000000, 0x064f4b08,
	0x09384248, 0x0aa10009, 0x064f4a08, 0x0c7e5f00, 0x00000000,
	0x080f3e04, 0x0920c248, 0x0780034a, 0x0b408289, 0x04001249,
	0x0960c209, 0x060f4a08, 0x06800048, 0x07800309, 0x0948c209,
	0x060f4b08, 0x0c7e5bc0, 0x00000000, 0x0680c008, 0x06094008,
	0x06803009, 0x06804008, 0x0d000009, 0x06094108, 0x06800008,
	0x06094008, 0x0cc00000, 0x00000000, 0x0cc00000, 0x00000000,
	0x0c000ac0, 0x00000000, 0x0780034a, 0x0400128a, 0x040002ca,
	0x07800309, 0x04001249, 0x096102c9, 0x0609520b, 0x054042ca,
	0x0968c2c9, 0x0609080b, 0x0681010a, 0x06c3474a, 0x0609070a,
	0x06490b0a, 0x0980a00a, 0x0980a0ca, 0x06090b0a, 0x09c0a00a,
	0x09c0a0ca, 0x06090b0a, 0x0698000a, 0x06c0000a, 0x064ac109,
	0x0200a24a, 0x0609440a, 0x060f370a, 0x06a0000a, 0x06c0000a,
	0x0200a24a, 0x0609450a, 0x060f380a, 0x0cc00000, 0x00000000,
	0x0c0005c0, 0x00000000, 0x08095003, 0x08095000, 0x06820889,
	0x06c00209, 0x0cc00000, 0x06095109, 0x0683ffc9, 0x0649090a,
	0x0a21004a, 0x0aa0c009, 0x0c7fff40, 0x04401249, 0x06490b0a,
	0x0980a00a, 0x0980a0ca, 0x06090b0a, 0x09c0a00a, 0x09c0a0ca,
	0x06090b0a, 0x0cc00000, 0x00000000, 0x0683ffca, 0x0649530b,
	0x090012cb, 0x0649bb09, 0x0920c249, 0x020092c9, 0x0aa10009,
	0x0aa0c00a, 0x0c7ffe40, 0x0440128a, 0x0cc00000, 0x00000000,
	0x080d1f30, 0x06bc0008, 0x060d1f08, 0x064d1e08, 0x09c087c8,
	0x060d1e08, 0x06800008, 0x06c00408, 0x060d1f08, 0x06800048,
	0x06c00408, 0x060d1f08, 0x06a19408, 0x060d1f08, 0x064d1e08,
	0x098087c8, 0x060d1e08, 0x064d3508, 0x0920c248, 0x0780030a,
	0x0400128a, 0x0e00024a, 0x0908c208, 0x09508209, 0x064d3509,
	0x0908c249, 0x0f000280, 0x0200a24a, 0x0961020a, 0x060d3308,
	0x0c064780, 0x060d210a, 0x0c064700, 0x080d2107, 0x0c064680,
	0x080d2100, 0x07800148, 0x07800189, 0x0680040a, 0x0240a20a,
	0x03409289, 0x09605248, 0x060d1f09, 0x078001c8, 0x0c064400,
	0x060d2108, 0x07800108, 0x07800209, 0x0680040a, 0x0240a20a,
	0x03409289, 0x09605248, 0x060d1f09, 0x080d1f20, 0x080d1f20,
	0x064f1a08, 0x09086208, 0x07800049, 0x0c06f5c0, 0x02408248,
	0x0c064000, 0x060d2108, 0x0cc00000, 0x00000000, 0x064d5308,
	0x0aa1c008, 0x00000000, 0x060d2108, 0x080d2001, 0x064d2008,
	0x0befc048, 0x00000000, 0x080d5300, 0x080d1f30, 0x06bc0008,
	0x060d1f08, 0x064d1e08, 0x09c087c8, 0x060d1e08, 0x06800008,
	0x06c00408, 0x060d1f08, 0x06800048, 0x06c00408, 0x060d1f08,
	0x06a10408, 0x060d1f08, 0x064d1e08, 0x098087c8, 0x060d1e08,
	0x064d3508, 0x0920c248, 0x0780030a, 0x0400128a, 0x0e00024a,
	0x0908c208, 0x09508209, 0x064d3509, 0x0908c249, 0x0f000280,
	0x0200a24a, 0x0961020a, 0x060d3308, 0x0c063540, 0x060d210a,
	0x0c0634c0, 0x080d2105, 0x0c063440, 0x080d2100, 0x07800148,
	0x07800189, 0x0680040a, 0x0240a20a, 0x03409289, 0x09605248,
	0x060d1f09, 0x07800108, 0x07800209, 0x0680040a, 0x0240a20a,
	0x03409289, 0x09605248, 0x060d1f09, 0x080d1f20, 0x080d1f20,
	0x080d1f20, 0x064f1a08, 0x092e2248, 0x0aaf8009, 0x09086208,
	0x07800049, 0x0c06e380, 0x02408248, 0x0c062dc0, 0x060d2108,
	0x0cc00000, 0x00000000, 0x064d340a, 0x091c128a, 0x0aaf800a,
	0x080d4800, 0x064d4923, 0x064d4924, 0x064d4925, 0x064d4926,
	0x064d4927, 0x064d4928, 0x064d4929, 0x064d492a, 0x064d492b,
	0x064d492c, 0x064d492d, 0x064d492e, 0x064d492f, 0x064d4930,
	0x064d4931, 0x064d4932, 0x0cc00000, 0x080d5201, 0x09210324,
	0x0740028c, 0x0400128a, 0x074002a4, 0x0400128a, 0x09210325,
	0x0740028c, 0x0400128a, 0x074002a5, 0x0400128a, 0x09210326,
	0x0740028c, 0x0400128a, 0x074002a6, 0x0400128a, 0x09210327,
	0x0740028c, 0x0400128a, 0x074002a7, 0x0400128a, 0x09210328,
	0x0740028c, 0x0400128a, 0x074002a8, 0x0400128a, 0x09210329,
	0x0740028c, 0x0400128a, 0x074002a9, 0x0400128a, 0x0921032a,
	0x0740028c, 0x0400128a, 0x074002aa, 0x0400128a, 0x0921032b,
	0x0740028c, 0x0400128a, 0x074002ab, 0x0400128a, 0x0921032c,
	0x0740028c, 0x0400128a, 0x074002ac, 0x0400128a, 0x0921032d,
	0x0740028c, 0x0400128a, 0x074002ad, 0x0400128a, 0x0921032e,
	0x0740028c, 0x0400128a, 0x074002ae, 0x0400128a, 0x0921032f,
	0x0740028c, 0x0400128a, 0x074002af, 0x0400128a, 0x09210330,
	0x0740028c, 0x0400128a, 0x074002b0, 0x0400128a, 0x09210331,
	0x0740028c, 0x0400128a, 0x074002b1, 0x0400128a, 0x09210332,
	0x0740028c, 0x0400128a, 0x074002b2, 0x0cc00000, 0x0400128a,
	0x064ac036, 0x0aa1c076, 0x0aa180b6, 0x0aa14136, 0x0aa100f6,
	0x00000000, 0x0c7ffe80, 0x00000000, 0x08030504, 0x0c7ffdc0,
	0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
	0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
	0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000,
	0x00000000, 0x00000000, 0x00000000, 0x00000000, 0x00000000
};
