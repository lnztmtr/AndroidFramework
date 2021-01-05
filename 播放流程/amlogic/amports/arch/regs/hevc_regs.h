/*
 * drivers/amlogic/amports/arch/regs/hevc_regs.h
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

#ifndef HEVC_REGS_HEADERS__
#define HEVC_REGS_HEADERS__
/*add from M8M2*/
#define HEVC_ASSIST_AFIFO_CTRL 0x3001
#define HEVC_ASSIST_AFIFO_CTRL1 0x3002
#define HEVC_ASSIST_GCLK_EN 0x3003
#define HEVC_ASSIST_SW_RESET 0x3004
#define HEVC_ASSIST_AMR1_INT0 0x3025
#define HEVC_ASSIST_AMR1_INT1 0x3026
#define HEVC_ASSIST_AMR1_INT2 0x3027
#define HEVC_ASSIST_AMR1_INT3 0x3028
#define HEVC_ASSIST_AMR1_INT4 0x3029
#define HEVC_ASSIST_AMR1_INT5 0x302a
#define HEVC_ASSIST_AMR1_INT6 0x302b
#define HEVC_ASSIST_AMR1_INT7 0x302c
#define HEVC_ASSIST_AMR1_INT8 0x302d
#define HEVC_ASSIST_AMR1_INT9 0x302e
#define HEVC_ASSIST_AMR1_INTA 0x302f
#define HEVC_ASSIST_AMR1_INTB 0x3030
#define HEVC_ASSIST_AMR1_INTC 0x3031
#define HEVC_ASSIST_AMR1_INTD 0x3032
#define HEVC_ASSIST_AMR1_INTE 0x3033
#define HEVC_ASSIST_AMR1_INTF 0x3034
#define HEVC_ASSIST_AMR2_INT0 0x3035
#define HEVC_ASSIST_AMR2_INT1 0x3036
#define HEVC_ASSIST_AMR2_INT2 0x3037
#define HEVC_ASSIST_AMR2_INT3 0x3038
#define HEVC_ASSIST_AMR2_INT4 0x3039
#define HEVC_ASSIST_AMR2_INT5 0x303a
#define HEVC_ASSIST_AMR2_INT6 0x303b
#define HEVC_ASSIST_AMR2_INT7 0x303c
#define HEVC_ASSIST_AMR2_INT8 0x303d
#define HEVC_ASSIST_AMR2_INT9 0x303e
#define HEVC_ASSIST_AMR2_INTA 0x303f
#define HEVC_ASSIST_AMR2_INTB 0x3040
#define HEVC_ASSIST_AMR2_INTC 0x3041
#define HEVC_ASSIST_AMR2_INTD 0x3042
#define HEVC_ASSIST_AMR2_INTE 0x3043
#define HEVC_ASSIST_AMR2_INTF 0x3044
#define HEVC_ASSIST_MBX_SSEL 0x3045
#define HEVC_ASSIST_TIMER0_LO 0x3060
#define HEVC_ASSIST_TIMER0_HI 0x3061
#define HEVC_ASSIST_TIMER1_LO 0x3062
#define HEVC_ASSIST_TIMER1_HI 0x3063
#define HEVC_ASSIST_DMA_INT 0x3064
#define HEVC_ASSIST_DMA_INT_MSK 0x3065
#define HEVC_ASSIST_DMA_INT2 0x3066
#define HEVC_ASSIST_DMA_INT_MSK2 0x3067
#define HEVC_ASSIST_MBOX0_IRQ_REG 0x3070
#define HEVC_ASSIST_MBOX0_CLR_REG 0x3071
#define HEVC_ASSIST_MBOX0_MASK 0x3072
#define HEVC_ASSIST_MBOX0_FIQ_SEL 0x3073
#define HEVC_ASSIST_MBOX1_IRQ_REG 0x3074
#define HEVC_ASSIST_MBOX1_CLR_REG 0x3075
#define HEVC_ASSIST_MBOX1_MASK 0x3076
#define HEVC_ASSIST_MBOX1_FIQ_SEL 0x3077
#define HEVC_ASSIST_MBOX2_IRQ_REG 0x3078
#define HEVC_ASSIST_MBOX2_CLR_REG 0x3079
#define HEVC_ASSIST_MBOX2_MASK 0x307a
#define HEVC_ASSIST_MBOX2_FIQ_SEL 0x307b
#define HEVC_ASSIST_AXI_CTRL 0x307c
#define HEVC_ASSIST_AXI_STATUS 0x307d
#define HEVC_ASSIST_SCRATCH_0 0x30c0
#define HEVC_ASSIST_SCRATCH_1 0x30c1
#define HEVC_ASSIST_SCRATCH_2 0x30c2
#define HEVC_ASSIST_SCRATCH_3 0x30c3
#define HEVC_ASSIST_SCRATCH_4 0x30c4
#define HEVC_ASSIST_SCRATCH_5 0x30c5
#define HEVC_ASSIST_SCRATCH_6 0x30c6
#define HEVC_ASSIST_SCRATCH_7 0x30c7
#define HEVC_ASSIST_SCRATCH_8 0x30c8
#define HEVC_ASSIST_SCRATCH_9 0x30c9
#define HEVC_ASSIST_SCRATCH_A 0x30ca
#define HEVC_ASSIST_SCRATCH_B 0x30cb
#define HEVC_ASSIST_SCRATCH_C 0x30cc
#define HEVC_ASSIST_SCRATCH_D 0x30cd
#define HEVC_ASSIST_SCRATCH_E 0x30ce
#define HEVC_ASSIST_SCRATCH_F 0x30cf
#define HEVC_ASSIST_SCRATCH_G 0x30d0
#define HEVC_ASSIST_SCRATCH_H 0x30d1
#define HEVC_ASSIST_SCRATCH_I 0x30d2
#define HEVC_ASSIST_SCRATCH_J 0x30d3
#define HEVC_ASSIST_SCRATCH_K 0x30d4
#define HEVC_ASSIST_SCRATCH_L 0x30d5
#define HEVC_ASSIST_SCRATCH_M 0x30d6
#define HEVC_ASSIST_SCRATCH_N 0x30d7
#define HEVC_PARSER_VERSION 0x3100
#define HEVC_STREAM_CONTROL 0x3101
#define HEVC_STREAM_START_ADDR 0x3102
#define HEVC_STREAM_END_ADDR 0x3103
#define HEVC_STREAM_WR_PTR 0x3104
#define HEVC_STREAM_RD_PTR 0x3105
#define HEVC_STREAM_LEVEL 0x3106
#define HEVC_STREAM_FIFO_CTL 0x3107
#define HEVC_SHIFT_CONTROL 0x3108
#define HEVC_SHIFT_STARTCODE 0x3109
#define HEVC_SHIFT_EMULATECODE 0x310a
#define HEVC_SHIFT_STATUS 0x310b
#define HEVC_SHIFTED_DATA 0x310c
#define HEVC_SHIFT_BYTE_COUNT 0x310d
#define HEVC_SHIFT_COMMAND 0x310e
#define HEVC_ELEMENT_RESULT 0x310f
#define HEVC_CABAC_CONTROL 0x3110
#define HEVC_PARSER_SLICE_INFO 0x3111
#define HEVC_PARSER_CMD_WRITE 0x3112
#define HEVC_PARSER_CORE_CONTROL 0x3113
#define HEVC_PARSER_CMD_FETCH 0x3114
#define HEVC_PARSER_CMD_STATUS 0x3115
#define HEVC_PARSER_LCU_INFO 0x3116
#define HEVC_PARSER_HEADER_INFO 0x3117
#define HEVC_PARSER_RESULT_0 0x3118
#define HEVC_PARSER_RESULT_1 0x3119
#define HEVC_PARSER_RESULT_2 0x311a
#define HEVC_PARSER_RESULT_3 0x311b
#define HEVC_CABAC_TOP_INFO 0x311c
#define HEVC_CABAC_TOP_INFO_2 0x311d
#define HEVC_CABAC_LEFT_INFO 0x311e
#define HEVC_CABAC_LEFT_INFO_2 0x311f
#define HEVC_PARSER_INT_CONTROL 0x3120
#define HEVC_PARSER_INT_STATUS 0x3121
#define HEVC_PARSER_IF_CONTROL 0x3122
#define HEVC_PARSER_PICTURE_SIZE 0x3123
#define HEVC_PARSER_LCU_START 0x3124
#define HEVC_PARSER_HEADER_INFO2 0x3125
#define HEVC_PARSER_QUANT_READ 0x3126
#define HEVC_PARSER_RESERVED_27 0x3127
#define HEVC_PARSER_CMD_SKIP_0 0x3128
#define HEVC_PARSER_CMD_SKIP_1 0x3129
#define HEVC_PARSER_CMD_SKIP_2 0x312a
#define HEVC_PARSER_MANUAL_CMD 0x312b
#define HEVC_PARSER_MEM_RD_ADDR 0x312c
#define HEVC_PARSER_MEM_WR_ADDR 0x312d
#define HEVC_PARSER_MEM_RW_DATA 0x312e
#define HEVC_SAO_IF_STATUS 0x3130
#define HEVC_SAO_IF_DATA_Y 0x3131
#define HEVC_SAO_IF_DATA_U 0x3132
#define HEVC_SAO_IF_DATA_V 0x3133
#define HEVC_STREAM_SWAP_ADDR 0x3134
#define HEVC_STREAM_SWAP_CTRL 0x3135
#define HEVC_IQIT_IF_WAIT_CNT 0x3136
#define HEVC_MPRED_IF_WAIT_CNT 0x3137
#define HEVC_SAO_IF_WAIT_CNT 0x3138
#define HEVC_PARSER_DEBUG_IDX 0x313e
#define HEVC_PARSER_DEBUG_DAT 0x313f
#define HEVC_MPRED_VERSION 0x3200
#define HEVC_MPRED_CTRL0 0x3201
#define HEVC_MPRED_CTRL1 0x3202
#define HEVC_MPRED_INT_EN 0x3203
#define HEVC_MPRED_INT_STATUS 0x3204
#define HEVC_MPRED_PIC_SIZE 0x3205
#define HEVC_MPRED_PIC_SIZE_LCU 0x3206
#define HEVC_MPRED_TILE_START 0x3207
#define HEVC_MPRED_TILE_SIZE_LCU 0x3208
#define HEVC_MPRED_REF_NUM 0x3209
#define HEVC_MPRED_LT_REF 0x320a
#define HEVC_MPRED_LT_COLREF 0x320b
#define HEVC_MPRED_REF_EN_L0 0x320c
#define HEVC_MPRED_REF_EN_L1 0x320d
#define HEVC_MPRED_COLREF_EN_L0 0x320e
#define HEVC_MPRED_COLREF_EN_L1 0x320f
#define HEVC_MPRED_AXI_WCTRL 0x3210
#define HEVC_MPRED_AXI_RCTRL 0x3211
#define HEVC_MPRED_ABV_START_ADDR 0x3212
#define HEVC_MPRED_MV_WR_START_ADDR 0x3213
#define HEVC_MPRED_MV_RD_START_ADDR 0x3214
#define HEVC_MPRED_MV_WPTR 0x3215
#define HEVC_MPRED_MV_RPTR 0x3216
#define HEVC_MPRED_MV_WR_ROW_JUMP 0x3217
#define HEVC_MPRED_MV_RD_ROW_JUMP 0x3218
#define HEVC_MPRED_CURR_LCU 0x3219
#define HEVC_MPRED_ABV_WPTR 0x321a
#define HEVC_MPRED_ABV_RPTR 0x321b
#define HEVC_MPRED_CTRL2 0x321c
#define HEVC_MPRED_CTRL3 0x321d
#define HEVC_MPRED_MV_WLCUY 0x321e
#define HEVC_MPRED_MV_RLCUY 0x321f
#define HEVC_MPRED_L0_REF00_POC 0x3220
#define HEVC_MPRED_L0_REF01_POC 0x3221
#define HEVC_MPRED_L0_REF02_POC 0x3222
#define HEVC_MPRED_L0_REF03_POC 0x3223
#define HEVC_MPRED_L0_REF04_POC 0x3224
#define HEVC_MPRED_L0_REF05_POC 0x3225
#define HEVC_MPRED_L0_REF06_POC 0x3226
#define HEVC_MPRED_L0_REF07_POC 0x3227
#define HEVC_MPRED_L0_REF08_POC 0x3228
#define HEVC_MPRED_L0_REF09_POC 0x3229
#define HEVC_MPRED_L0_REF10_POC 0x322a
#define HEVC_MPRED_L0_REF11_POC 0x322b
#define HEVC_MPRED_L0_REF12_POC 0x322c
#define HEVC_MPRED_L0_REF13_POC 0x322d
#define HEVC_MPRED_L0_REF14_POC 0x322e
#define HEVC_MPRED_L0_REF15_POC 0x322f
#define HEVC_MPRED_L1_REF00_POC 0x3230
#define HEVC_MPRED_L1_REF01_POC 0x3231
#define HEVC_MPRED_L1_REF02_POC 0x3232
#define HEVC_MPRED_L1_REF03_POC 0x3233
#define HEVC_MPRED_L1_REF04_POC 0x3234
#define HEVC_MPRED_L1_REF05_POC 0x3235
#define HEVC_MPRED_L1_REF06_POC 0x3236
#define HEVC_MPRED_L1_REF07_POC 0x3237
#define HEVC_MPRED_L1_REF08_POC 0x3238
#define HEVC_MPRED_L1_REF09_POC 0x3239
#define HEVC_MPRED_L1_REF10_POC 0x323a
#define HEVC_MPRED_L1_REF11_POC 0x323b
#define HEVC_MPRED_L1_REF12_POC 0x323c
#define HEVC_MPRED_L1_REF13_POC 0x323d
#define HEVC_MPRED_L1_REF14_POC 0x323e
#define HEVC_MPRED_L1_REF15_POC 0x323f
#define HEVC_MPRED_PIC_SIZE_EXT 0x3240
#define HEVC_MPRED_DBG_MODE0 0x3241
#define HEVC_MPRED_DBG_MODE1 0x3242
#define HEVC_MPRED_DBG2_MODE 0x3243
#define HEVC_MPRED_IMP_CMD0 0x3244
#define HEVC_MPRED_IMP_CMD1 0x3245
#define HEVC_MPRED_IMP_CMD2 0x3246
#define HEVC_MPRED_IMP_CMD3 0x3247
#define HEVC_MPRED_DBG2_DATA_0 0x3248
#define HEVC_MPRED_DBG2_DATA_1 0x3249
#define HEVC_MPRED_DBG2_DATA_2 0x324a
#define HEVC_MPRED_DBG2_DATA_3 0x324b
#define HEVC_MPRED_DBG_DATA_0 0x3250
#define HEVC_MPRED_DBG_DATA_1 0x3251
#define HEVC_MPRED_DBG_DATA_2 0x3252
#define HEVC_MPRED_DBG_DATA_3 0x3253
#define HEVC_MPRED_DBG_DATA_4 0x3254
#define HEVC_MPRED_DBG_DATA_5 0x3255
#define HEVC_MPRED_DBG_DATA_6 0x3256
#define HEVC_MPRED_DBG_DATA_7 0x3257
#define HEVC_MPRED_CUR_POC 0x3260
#define HEVC_MPRED_COL_POC 0x3261
#define HEVC_MPRED_MV_RD_END_ADDR 0x3262
#define HEVCD_IPP_TOP_CNTL 0x3400
#define HEVCD_IPP_TOP_STATUS 0x3401
#define HEVCD_IPP_TOP_FRMCONFIG 0x3402
#define HEVCD_IPP_TOP_TILECONFIG1 0x3403
#define HEVCD_IPP_TOP_TILECONFIG2 0x3404
#define HEVCD_IPP_TOP_TILECONFIG3 0x3405
#define HEVCD_IPP_TOP_LCUCONFIG 0x3406
#define HEVCD_IPP_TOP_FRMCTL 0x3407
#define HEVCD_IPP_CONFIG 0x3408
#define HEVCD_IPP_LINEBUFF_BASE 0x3409
#define HEVCD_IPP_INTR_MASK 0x340a
#define HEVCD_IPP_AXIIF_CONFIG 0x340b
#define HEVCD_IPP_BITDEPTH_CONFIG 0x340c
#define HEVCD_IPP_SWMPREDIF_CONFIG 0x3410
#define HEVCD_IPP_SWMPREDIF_STATUS 0x3411
#define HEVCD_IPP_SWMPREDIF_CTBINFO 0x3412
#define HEVCD_IPP_SWMPREDIF_PUINFO0 0x3413
#define HEVCD_IPP_SWMPREDIF_PUINFO1 0x3414
#define HEVCD_IPP_SWMPREDIF_PUINFO2 0x3415
#define HEVCD_IPP_SWMPREDIF_PUINFO3 0x3416
#define HEVCD_IPP_DYNCLKGATE_CONFIG 0x3420
#define HEVCD_IPP_DYNCLKGATE_STATUS 0x3421
#define HEVCD_IPP_DBG_SEL 0x3430
#define HEVCD_IPP_DBG_DATA 0x3431
#define HEVCD_MPP_ANC2AXI_TBL_CONF_ADDR 0x3460
#define HEVCD_MPP_ANC2AXI_TBL_CMD_ADDR 0x3461
#define HEVCD_MPP_ANC2AXI_TBL_WDATA_ADDR 0x3462
#define HEVCD_MPP_ANC2AXI_TBL_RDATA_ADDR 0x3463
#define HEVCD_MPP_WEIGHTPRED_CNTL_ADDR 0x347b
#define HEVCD_MPP_L0_WEIGHT_FLAG_ADDR 0x347c
#define HEVCD_MPP_L1_WEIGHT_FLAG_ADDR 0x347d
#define HEVCD_MPP_YLOG2WGHTDENOM_ADDR 0x347e
#define HEVCD_MPP_DELTACLOG2WGHTDENOM_ADDR 0x347f
#define HEVCD_MPP_WEIGHT_ADDR 0x3480
#define HEVCD_MPP_WEIGHT_DATA 0x3481
#define HEVCD_MPP_ANC_CANVAS_ACCCONFIG_ADDR 0x34c0
#define HEVCD_MPP_ANC_CANVAS_DATA_ADDR 0x34c1
#define HEVCD_MPP_DECOMP_CTL1 0x34c2
#define HEVCD_MPP_DECOMP_CTL2 0x34c3
#define HEVCD_MPP_DECOMP_PERFMON_CTL 0x34c5
#define HEVCD_MPP_DECOMP_PERFMON_DATA 0x34c6
#define HEVCD_MCRCC_CTL1 0x34f0
#define HEVCD_MCRCC_CTL2 0x34f1
#define HEVCD_MCRCC_CTL3 0x34f2
#define HEVCD_MCRCC_PERFMON_CTL 0x34f3
#define HEVCD_MCRCC_PERFMON_DATA 0x34f4
#define HEVC_DBLK_CFG0 0x3500
#define HEVC_DBLK_CFG1 0x3501
#define HEVC_DBLK_CFG2 0x3502
#define HEVC_DBLK_CFG3 0x3503
#define HEVC_DBLK_CFG4 0x3504
#define HEVC_DBLK_CFG5 0x3505
#define HEVC_DBLK_CFG6 0x3506
#define HEVC_DBLK_CFG7 0x3507
#define HEVC_DBLK_CFG8 0x3508
#define HEVC_DBLK_CFG9 0x3509
#define HEVC_DBLK_CFGA 0x350a
#define HEVC_DBLK_CFGE 0x350e
#define HEVC_DBLK_STS0 0x350b	/* changes the val to 0x350f on g12a */
#define HEVC_DBLK_STS1 0x350c	/* changes the val to 0x3510 on g12a */
#define HEVC_SAO_VERSION 0x3600
#define HEVC_SAO_CTRL0 0x3601
#define HEVC_SAO_CTRL1 0x3602
#define HEVC_SAO_INT_EN 0x3603
#define HEVC_SAO_INT_STATUS 0x3604
#define HEVC_SAO_PIC_SIZE 0x3605
#define HEVC_SAO_PIC_SIZE_LCU 0x3606
#define HEVC_SAO_TILE_START 0x3607
#define HEVC_SAO_TILE_SIZE_LCU 0x3608
#define HEVC_SAO_AXI_WCTRL 0x3609
#define HEVC_SAO_AXI_RCTRL 0x360a
#define HEVC_SAO_Y_START_ADDR 0x360b
#define HEVC_SAO_Y_LENGTH 0x360c
#define HEVC_SAO_C_START_ADDR 0x360d
#define HEVC_SAO_C_LENGTH 0x360e
#define HEVC_SAO_Y_WPTR 0x360f
#define HEVC_SAO_C_WPTR 0x3610
#define HEVC_SAO_ABV_START_ADDR 0x3611
#define HEVC_SAO_VB_WR_START_ADDR 0x3612
#define HEVC_SAO_VB_RD_START_ADDR 0x3613
#define HEVC_SAO_ABV_WPTR 0x3614
#define HEVC_SAO_ABV_RPTR 0x3615
#define HEVC_SAO_VB_WPTR 0x3616
#define HEVC_SAO_VB_RPTR 0x3617
#define HEVC_SAO_DBG_MODE0 0x361e
#define HEVC_SAO_DBG_MODE1 0x361f
#define HEVC_SAO_CTRL2 0x3620
#define HEVC_SAO_CTRL3 0x3621
#define HEVC_SAO_CTRL4 0x3622
#define HEVC_SAO_CTRL5 0x3623
#define HEVC_SAO_CTRL6 0x3624
#define HEVC_SAO_CTRL7 0x3625
#define HEVC_SAO_DBG_DATA_0 0x3630
#define HEVC_SAO_DBG_DATA_1 0x3631
#define HEVC_SAO_DBG_DATA_2 0x3632
#define HEVC_SAO_DBG_DATA_3 0x3633
#define HEVC_SAO_DBG_DATA_4 0x3634
#define HEVC_SAO_DBG_DATA_5 0x3635
#define HEVC_SAO_DBG_DATA_6 0x3636
#define HEVC_SAO_DBG_DATA_7 0x3637
#define HEVC_IQIT_CLK_RST_CTRL 0x3700
#define HEVC_IQIT_DEQUANT_CTRL 0x3701
#define HEVC_IQIT_SCALELUT_WR_ADDR 0x3702
#define HEVC_IQIT_SCALELUT_RD_ADDR 0x3703
#define HEVC_IQIT_SCALELUT_DATA 0x3704
#define HEVC_IQIT_SCALELUT_IDX_4 0x3705
#define HEVC_IQIT_SCALELUT_IDX_8 0x3706
#define HEVC_IQIT_SCALELUT_IDX_16_32 0x3707
#define HEVC_IQIT_STAT_GEN0 0x3708
#define HEVC_QP_WRITE 0x3709
#define HEVC_IQIT_STAT_GEN1 0x370a
#define HEVC_IQIT_BITDEPTH                         0x370b
#define HEVC_IQIT_STAT_GEN2                        0x370c
#define HEVC_IQIT_AVS2_WQP_0123                    0x370d
#define HEVC_IQIT_AVS2_WQP_45                      0x370e
#define HEVC_IQIT_AVS2_QP_DELTA                    0x370f
#define HEVC_PIC_QUALITY_CTRL                      0x3710
#define HEVC_PIC_QUALITY_DATA                      0x3711

/**/

/*add from M8M2*/
#define HEVC_MC_CTRL_REG 0x3900
#define HEVC_MC_MB_INFO 0x3901
#define HEVC_MC_PIC_INFO 0x3902
#define HEVC_MC_HALF_PEL_ONE 0x3903
#define HEVC_MC_HALF_PEL_TWO 0x3904
#define HEVC_POWER_CTL_MC 0x3905
#define HEVC_MC_CMD 0x3906
#define HEVC_MC_CTRL0 0x3907
#define HEVC_MC_PIC_W_H 0x3908
#define HEVC_MC_STATUS0 0x3909
#define HEVC_MC_STATUS1 0x390a
#define HEVC_MC_CTRL1 0x390b
#define HEVC_MC_MIX_RATIO0 0x390c
#define HEVC_MC_MIX_RATIO1 0x390d
#define HEVC_MC_DP_MB_XY 0x390e
#define HEVC_MC_OM_MB_XY 0x390f
#define HEVC_PSCALE_RST 0x3910
#define HEVC_PSCALE_CTRL 0x3911
#define HEVC_PSCALE_PICI_W 0x3912
#define HEVC_PSCALE_PICI_H 0x3913
#define HEVC_PSCALE_PICO_W 0x3914
#define HEVC_PSCALE_PICO_H 0x3915
#define HEVC_PSCALE_PICO_START_X 0x3916
#define HEVC_PSCALE_PICO_START_Y 0x3917
#define HEVC_PSCALE_DUMMY 0x3918
#define HEVC_PSCALE_FILT0_COEF0 0x3919
#define HEVC_PSCALE_FILT0_COEF1 0x391a
#define HEVC_PSCALE_CMD_CTRL 0x391b
#define HEVC_PSCALE_CMD_BLK_X 0x391c
#define HEVC_PSCALE_CMD_BLK_Y 0x391d
#define HEVC_PSCALE_STATUS 0x391e
#define HEVC_PSCALE_BMEM_ADDR 0x391f
#define HEVC_PSCALE_BMEM_DAT 0x3920
#define HEVC_PSCALE_DRAM_BUF_CTRL 0x3921
#define HEVC_PSCALE_MCMD_CTRL 0x3922
#define HEVC_PSCALE_MCMD_XSIZE 0x3923
#define HEVC_PSCALE_MCMD_YSIZE 0x3924
#define HEVC_PSCALE_RBUF_START_BLKX 0x3925
#define HEVC_PSCALE_RBUF_START_BLKY 0x3926
#define HEVC_PSCALE_PICO_SHIFT_XY 0x3928
#define HEVC_PSCALE_CTRL1 0x3929
#define HEVC_PSCALE_SRCKEY_CTRL0 0x392a
#define HEVC_PSCALE_SRCKEY_CTRL1 0x392b
#define HEVC_PSCALE_CANVAS_RD_ADDR 0x392c
#define HEVC_PSCALE_CANVAS_WR_ADDR 0x392d
#define HEVC_PSCALE_CTRL2 0x392e
#define HEVC_HDEC_MC_OMEM_AUTO 0x3930
#define HEVC_HDEC_MC_MBRIGHT_IDX 0x3931
#define HEVC_HDEC_MC_MBRIGHT_RD 0x3932
#define HEVC_MC_MPORT_CTRL 0x3940
#define HEVC_MC_MPORT_DAT 0x3941
#define HEVC_MC_WT_PRED_CTRL 0x3942
#define HEVC_MC_MBBOT_ST_EVEN_ADDR 0x3944
#define HEVC_MC_MBBOT_ST_ODD_ADDR 0x3945
#define HEVC_MC_DPDN_MB_XY 0x3946
#define HEVC_MC_OMDN_MB_XY 0x3947
#define HEVC_MC_HCMDBUF_H 0x3948
#define HEVC_MC_HCMDBUF_L 0x3949
#define HEVC_MC_HCMD_H 0x394a
#define HEVC_MC_HCMD_L 0x394b
#define HEVC_MC_IDCT_DAT 0x394c
#define HEVC_MC_CTRL_GCLK_CTRL 0x394d
#define HEVC_MC_OTHER_GCLK_CTRL 0x394e
#define HEVC_MC_CTRL2 0x394f
#define HEVC_MDEC_PIC_DC_CTRL 0x398e
#define HEVC_MDEC_PIC_DC_STATUS 0x398f
#define HEVC_ANC0_CANVAS_ADDR 0x3990
#define HEVC_ANC1_CANVAS_ADDR 0x3991
#define HEVC_ANC2_CANVAS_ADDR 0x3992
#define HEVC_ANC3_CANVAS_ADDR 0x3993
#define HEVC_ANC4_CANVAS_ADDR 0x3994
#define HEVC_ANC5_CANVAS_ADDR 0x3995
#define HEVC_ANC6_CANVAS_ADDR 0x3996
#define HEVC_ANC7_CANVAS_ADDR 0x3997
#define HEVC_ANC8_CANVAS_ADDR 0x3998
#define HEVC_ANC9_CANVAS_ADDR 0x3999
#define HEVC_ANC10_CANVAS_ADDR 0x399a
#define HEVC_ANC11_CANVAS_ADDR 0x399b
#define HEVC_ANC12_CANVAS_ADDR 0x399c
#define HEVC_ANC13_CANVAS_ADDR 0x399d
#define HEVC_ANC14_CANVAS_ADDR 0x399e
#define HEVC_ANC15_CANVAS_ADDR 0x399f
#define HEVC_ANC16_CANVAS_ADDR 0x39a0
#define HEVC_ANC17_CANVAS_ADDR 0x39a1
#define HEVC_ANC18_CANVAS_ADDR 0x39a2
#define HEVC_ANC19_CANVAS_ADDR 0x39a3
#define HEVC_ANC20_CANVAS_ADDR 0x39a4
#define HEVC_ANC21_CANVAS_ADDR 0x39a5
#define HEVC_ANC22_CANVAS_ADDR 0x39a6
#define HEVC_ANC23_CANVAS_ADDR 0x39a7
#define HEVC_ANC24_CANVAS_ADDR 0x39a8
#define HEVC_ANC25_CANVAS_ADDR 0x39a9
#define HEVC_ANC26_CANVAS_ADDR 0x39aa
#define HEVC_ANC27_CANVAS_ADDR 0x39ab
#define HEVC_ANC28_CANVAS_ADDR 0x39ac
#define HEVC_ANC29_CANVAS_ADDR 0x39ad
#define HEVC_ANC30_CANVAS_ADDR 0x39ae
#define HEVC_ANC31_CANVAS_ADDR 0x39af
#define HEVC_DBKR_CANVAS_ADDR 0x39b0
#define HEVC_DBKW_CANVAS_ADDR 0x39b1
#define HEVC_REC_CANVAS_ADDR 0x39b2
#define HEVC_CURR_CANVAS_CTRL 0x39b3
#define HEVC_MDEC_PIC_DC_THRESH 0x39b8
#define HEVC_MDEC_PICR_BUF_STATUS 0x39b9
#define HEVC_MDEC_PICW_BUF_STATUS 0x39ba
#define HEVC_MCW_DBLK_WRRSP_CNT 0x39bb
#define HEVC_MC_MBBOT_WRRSP_CNT 0x39bc
#define HEVC_MDEC_PICW_BUF2_STATUS 0x39bd
#define HEVC_WRRSP_FIFO_PICW_DBK 0x39be
#define HEVC_WRRSP_FIFO_PICW_MC 0x39bf
#define HEVC_AV_SCRATCH_0 0x39c0
#define HEVC_AV_SCRATCH_1 0x39c1
#define HEVC_AV_SCRATCH_2 0x39c2
#define HEVC_AV_SCRATCH_3 0x39c3
#define HEVC_AV_SCRATCH_4 0x39c4
#define HEVC_AV_SCRATCH_5 0x39c5
#define HEVC_AV_SCRATCH_6 0x39c6
#define HEVC_AV_SCRATCH_7 0x39c7
#define HEVC_AV_SCRATCH_8 0x39c8
#define HEVC_AV_SCRATCH_9 0x39c9
#define HEVC_AV_SCRATCH_A 0x39ca
#define HEVC_AV_SCRATCH_B 0x39cb
#define HEVC_AV_SCRATCH_C 0x39cc
#define HEVC_AV_SCRATCH_D 0x39cd
#define HEVC_AV_SCRATCH_E 0x39ce
#define HEVC_AV_SCRATCH_F 0x39cf
#define HEVC_AV_SCRATCH_G 0x39d0
#define HEVC_AV_SCRATCH_H 0x39d1
#define HEVC_AV_SCRATCH_I 0x39d2
#define HEVC_AV_SCRATCH_J 0x39d3
#define HEVC_AV_SCRATCH_K 0x39d4
#define HEVC_AV_SCRATCH_L 0x39d5
#define HEVC_AV_SCRATCH_M 0x39d6
#define HEVC_AV_SCRATCH_N 0x39d7
#define HEVC_WRRSP_CO_MB 0x39d8
#define HEVC_WRRSP_DCAC 0x39d9
#define HEVC_WRRSP_VLD 0x39da
#define HEVC_MDEC_DOUBLEW_CFG0 0x39db
#define HEVC_MDEC_DOUBLEW_CFG1 0x39dc
#define HEVC_MDEC_DOUBLEW_CFG2 0x39dd
#define HEVC_MDEC_DOUBLEW_CFG3 0x39de
#define HEVC_MDEC_DOUBLEW_CFG4 0x39df
#define HEVC_MDEC_DOUBLEW_CFG5 0x39e0
#define HEVC_MDEC_DOUBLEW_CFG6 0x39e1
#define HEVC_MDEC_DOUBLEW_CFG7 0x39e2
#define HEVC_MDEC_DOUBLEW_STATUS 0x39e3
#define HEVC_DBLK_RST 0x3950
#define HEVC_DBLK_CTRL 0x3951
#define HEVC_DBLK_MB_WID_HEIGHT 0x3952
#define HEVC_DBLK_STATUS 0x3953
#define HEVC_DBLK_CMD_CTRL 0x3954
#define HEVC_DBLK_MB_XY 0x3955
#define HEVC_DBLK_QP 0x3956
#define HEVC_DBLK_Y_BHFILT 0x3957
#define HEVC_DBLK_Y_BHFILT_HIGH 0x3958
#define HEVC_DBLK_Y_BVFILT 0x3959
#define HEVC_DBLK_CB_BFILT 0x395a
#define HEVC_DBLK_CR_BFILT 0x395b
#define HEVC_DBLK_Y_HFILT 0x395c
#define HEVC_DBLK_Y_HFILT_HIGH 0x395d
#define HEVC_DBLK_Y_VFILT 0x395e
#define HEVC_DBLK_CB_FILT 0x395f
#define HEVC_DBLK_CR_FILT 0x3960
#define HEVC_DBLK_BETAX_QP_SEL 0x3961
#define HEVC_DBLK_CLIP_CTRL0 0x3962
#define HEVC_DBLK_CLIP_CTRL1 0x3963
#define HEVC_DBLK_CLIP_CTRL2 0x3964
#define HEVC_DBLK_CLIP_CTRL3 0x3965
#define HEVC_DBLK_CLIP_CTRL4 0x3966
#define HEVC_DBLK_CLIP_CTRL5 0x3967
#define HEVC_DBLK_CLIP_CTRL6 0x3968
#define HEVC_DBLK_CLIP_CTRL7 0x3969
#define HEVC_DBLK_CLIP_CTRL8 0x396a
#define HEVC_DBLK_STATUS1 0x396b
#define HEVC_DBLK_GCLK_FREE 0x396c
#define HEVC_DBLK_GCLK_OFF 0x396d
#define HEVC_DBLK_AVSFLAGS 0x396e
#define HEVC_DBLK_CBPY 0x3970
#define HEVC_DBLK_CBPY_ADJ 0x3971
#define HEVC_DBLK_CBPC 0x3972
#define HEVC_DBLK_CBPC_ADJ 0x3973
#define HEVC_DBLK_VHMVD 0x3974
#define HEVC_DBLK_STRONG 0x3975
#define HEVC_DBLK_RV8_QUANT 0x3976
#define HEVC_DBLK_CBUS_HCMD2 0x3977
#define HEVC_DBLK_CBUS_HCMD1 0x3978
#define HEVC_DBLK_CBUS_HCMD0 0x3979
#define HEVC_DBLK_VLD_HCMD2 0x397a
#define HEVC_DBLK_VLD_HCMD1 0x397b
#define HEVC_DBLK_VLD_HCMD0 0x397c
#define HEVC_DBLK_OST_YBASE 0x397d
#define HEVC_DBLK_OST_CBCRDIFF 0x397e
#define HEVC_DBLK_CTRL1 0x397f
#define HEVC_MCRCC_CTL1 0x3980
#define HEVC_MCRCC_CTL2 0x3981
#define HEVC_MCRCC_CTL3 0x3982
#define HEVC_GCLK_EN 0x3983
#define HEVC_MDEC_SW_RESET 0x3984

/*add from M8M2*/
#define HEVC_VLD_STATUS_CTRL 0x3c00
#define HEVC_MPEG1_2_REG 0x3c01
#define HEVC_F_CODE_REG 0x3c02
#define HEVC_PIC_HEAD_INFO 0x3c03
#define HEVC_SLICE_VER_POS_PIC_TYPE 0x3c04
#define HEVC_QP_VALUE_REG 0x3c05
#define HEVC_MBA_INC 0x3c06
#define HEVC_MB_MOTION_MODE 0x3c07
#define HEVC_POWER_CTL_VLD 0x3c08
#define HEVC_MB_WIDTH 0x3c09
#define HEVC_SLICE_QP 0x3c0a
#define HEVC_PRE_START_CODE 0x3c0b
#define HEVC_SLICE_START_BYTE_01 0x3c0c
#define HEVC_SLICE_START_BYTE_23 0x3c0d
#define HEVC_RESYNC_MARKER_LENGTH 0x3c0e
#define HEVC_DECODER_BUFFER_INFO 0x3c0f
#define HEVC_FST_FOR_MV_X 0x3c10
#define HEVC_FST_FOR_MV_Y 0x3c11
#define HEVC_SCD_FOR_MV_X 0x3c12
#define HEVC_SCD_FOR_MV_Y 0x3c13
#define HEVC_FST_BAK_MV_X 0x3c14
#define HEVC_FST_BAK_MV_Y 0x3c15
#define HEVC_SCD_BAK_MV_X 0x3c16
#define HEVC_SCD_BAK_MV_Y 0x3c17
#define HEVC_VLD_DECODE_CONTROL 0x3c18
#define HEVC_VLD_REVERVED_19 0x3c19
#define HEVC_VIFF_BIT_CNT 0x3c1a
#define HEVC_BYTE_ALIGN_PEAK_HI 0x3c1b
#define HEVC_BYTE_ALIGN_PEAK_LO 0x3c1c
#define HEVC_NEXT_ALIGN_PEAK 0x3c1d
#define HEVC_VC1_CONTROL_REG 0x3c1e
#define HEVC_PMV1_X 0x3c20
#define HEVC_PMV1_Y 0x3c21
#define HEVC_PMV2_X 0x3c22
#define HEVC_PMV2_Y 0x3c23
#define HEVC_PMV3_X 0x3c24
#define HEVC_PMV3_Y 0x3c25
#define HEVC_PMV4_X 0x3c26
#define HEVC_PMV4_Y 0x3c27
#define HEVC_M4_TABLE_SELECT 0x3c28
#define HEVC_M4_CONTROL_REG 0x3c29
#define HEVC_BLOCK_NUM 0x3c2a
#define HEVC_PATTERN_CODE 0x3c2b
#define HEVC_MB_INFO 0x3c2c
#define HEVC_VLD_DC_PRED 0x3c2d
#define HEVC_VLD_ERROR_MASK 0x3c2e
#define HEVC_VLD_DC_PRED_C 0x3c2f
#define HEVC_LAST_SLICE_MV_ADDR 0x3c30
#define HEVC_LAST_MVX 0x3c31
#define HEVC_LAST_MVY 0x3c32
#define HEVC_VLD_C38 0x3c38
#define HEVC_VLD_C39 0x3c39
#define HEVC_VLD_STATUS 0x3c3a
#define HEVC_VLD_SHIFT_STATUS 0x3c3b
#define HEVC_VOFF_STATUS 0x3c3c
#define HEVC_VLD_C3D 0x3c3d
#define HEVC_VLD_DBG_INDEX 0x3c3e
#define HEVC_VLD_DBG_DATA 0x3c3f
#define HEVC_VLD_MEM_VIFIFO_START_PTR 0x3c40
#define HEVC_VLD_MEM_VIFIFO_CURR_PTR 0x3c41
#define HEVC_VLD_MEM_VIFIFO_END_PTR 0x3c42
#define HEVC_VLD_MEM_VIFIFO_BYTES_AVAIL 0x3c43
#define HEVC_VLD_MEM_VIFIFO_CONTROL 0x3c44
#define HEVC_VLD_MEM_VIFIFO_WP 0x3c45
#define HEVC_VLD_MEM_VIFIFO_RP 0x3c46
#define HEVC_VLD_MEM_VIFIFO_LEVEL 0x3c47
#define HEVC_VLD_MEM_VIFIFO_BUF_CNTL 0x3c48
#define HEVC_VLD_TIME_STAMP_CNTL 0x3c49
#define HEVC_VLD_TIME_STAMP_SYNC_0 0x3c4a
#define HEVC_VLD_TIME_STAMP_SYNC_1 0x3c4b
#define HEVC_VLD_TIME_STAMP_0 0x3c4c
#define HEVC_VLD_TIME_STAMP_1 0x3c4d
#define HEVC_VLD_TIME_STAMP_2 0x3c4e
#define HEVC_VLD_TIME_STAMP_3 0x3c4f
#define HEVC_VLD_TIME_STAMP_LENGTH 0x3c50
#define HEVC_VLD_MEM_VIFIFO_WRAP_COUNT 0x3c51
#define HEVC_VLD_MEM_VIFIFO_MEM_CTL 0x3c52
#define HEVC_VLD_MEM_VBUF_RD_PTR 0x3c53
#define HEVC_VLD_MEM_VBUF2_RD_PTR 0x3c54
#define HEVC_VLD_MEM_SWAP_ADDR 0x3c55
#define HEVC_VLD_MEM_SWAP_CTL 0x3c56
/**/

/*add from M8M2*/
#define HEVC_VCOP_CTRL_REG 0x3e00
#define HEVC_QP_CTRL_REG 0x3e01
#define HEVC_INTRA_QUANT_MATRIX 0x3e02
#define HEVC_NON_I_QUANT_MATRIX 0x3e03
#define HEVC_DC_SCALER 0x3e04
#define HEVC_DC_AC_CTRL 0x3e05
#define HEVC_DC_AC_SCALE_MUL 0x3e06
#define HEVC_DC_AC_SCALE_DIV 0x3e07
#define HEVC_POWER_CTL_IQIDCT 0x3e08
#define HEVC_RV_AI_Y_X 0x3e09
#define HEVC_RV_AI_U_X 0x3e0a
#define HEVC_RV_AI_V_X 0x3e0b
#define HEVC_RV_AI_MB_COUNT 0x3e0c
#define HEVC_NEXT_INTRA_DMA_ADDRESS 0x3e0d
#define HEVC_IQIDCT_CONTROL 0x3e0e
#define HEVC_IQIDCT_DEBUG_INFO_0 0x3e0f
#define HEVC_DEBLK_CMD 0x3e10
#define HEVC_IQIDCT_DEBUG_IDCT 0x3e11
#define HEVC_DCAC_DMA_CTRL 0x3e12
#define HEVC_DCAC_DMA_ADDRESS 0x3e13
#define HEVC_DCAC_CPU_ADDRESS 0x3e14
#define HEVC_DCAC_CPU_DATA 0x3e15
#define HEVC_DCAC_MB_COUNT 0x3e16
#define HEVC_IQ_QUANT 0x3e17
#define HEVC_VC1_BITPLANE_CTL 0x3e18


/*add from M8M2*/
#define HEVC_MSP 0x3300
#define HEVC_MPSR 0x3301
#define HEVC_MINT_VEC_BASE 0x3302
#define HEVC_MCPU_INTR_GRP 0x3303
#define HEVC_MCPU_INTR_MSK 0x3304
#define HEVC_MCPU_INTR_REQ 0x3305
#define HEVC_MPC_P 0x3306
#define HEVC_MPC_D 0x3307
#define HEVC_MPC_E 0x3308
#define HEVC_MPC_W 0x3309
#define HEVC_MINDEX0_REG 0x330a
#define HEVC_MINDEX1_REG 0x330b
#define HEVC_MINDEX2_REG 0x330c
#define HEVC_MINDEX3_REG 0x330d
#define HEVC_MINDEX4_REG 0x330e
#define HEVC_MINDEX5_REG 0x330f
#define HEVC_MINDEX6_REG 0x3310
#define HEVC_MINDEX7_REG 0x3311
#define HEVC_MMIN_REG 0x3312
#define HEVC_MMAX_REG 0x3313
#define HEVC_MBREAK0_REG 0x3314
#define HEVC_MBREAK1_REG 0x3315
#define HEVC_MBREAK2_REG 0x3316
#define HEVC_MBREAK3_REG 0x3317
#define HEVC_MBREAK_TYPE 0x3318
#define HEVC_MBREAK_CTRL 0x3319
#define HEVC_MBREAK_STAUTS 0x331a
#define HEVC_MDB_ADDR_REG 0x331b
#define HEVC_MDB_DATA_REG 0x331c
#define HEVC_MDB_CTRL 0x331d
#define HEVC_MSFTINT0 0x331e
#define HEVC_MSFTINT1 0x331f
#define HEVC_CSP 0x3320
#define HEVC_CPSR 0x3321
#define HEVC_CINT_VEC_BASE 0x3322
#define HEVC_CCPU_INTR_GRP 0x3323
#define HEVC_CCPU_INTR_MSK 0x3324
#define HEVC_CCPU_INTR_REQ 0x3325
#define HEVC_CPC_P 0x3326
#define HEVC_CPC_D 0x3327
#define HEVC_CPC_E 0x3328
#define HEVC_CPC_W 0x3329
#define HEVC_CINDEX0_REG 0x332a
#define HEVC_CINDEX1_REG 0x332b
#define HEVC_CINDEX2_REG 0x332c
#define HEVC_CINDEX3_REG 0x332d
#define HEVC_CINDEX4_REG 0x332e
#define HEVC_CINDEX5_REG 0x332f
#define HEVC_CINDEX6_REG 0x3330
#define HEVC_CINDEX7_REG 0x3331
#define HEVC_CMIN_REG 0x3332
#define HEVC_CMAX_REG 0x3333
#define HEVC_CBREAK0_REG 0x3334
#define HEVC_CBREAK1_REG 0x3335
#define HEVC_CBREAK2_REG 0x3336
#define HEVC_CBREAK3_REG 0x3337
#define HEVC_CBREAK_TYPE 0x3338
#define HEVC_CBREAK_CTRL 0x3339
#define HEVC_CBREAK_STAUTS 0x333a
#define HEVC_CDB_ADDR_REG 0x333b
#define HEVC_CDB_DATA_REG 0x333c
#define HEVC_CDB_CTRL 0x333d
#define HEVC_CSFTINT0 0x333e
#define HEVC_CSFTINT1 0x333f
#define HEVC_IMEM_DMA_CTRL 0x3340
#define HEVC_IMEM_DMA_ADR 0x3341
#define HEVC_IMEM_DMA_COUNT 0x3342
#define HEVC_WRRSP_IMEM 0x3343
#define HEVC_LMEM_DMA_CTRL 0x3350
#define HEVC_LMEM_DMA_ADR 0x3351
#define HEVC_LMEM_DMA_COUNT 0x3352
#define HEVC_WRRSP_LMEM 0x3353
#define HEVC_MAC_CTRL1 0x3360
#define HEVC_ACC0REG1 0x3361
#define HEVC_ACC1REG1 0x3362
#define HEVC_MAC_CTRL2 0x3370
#define HEVC_ACC0REG2 0x3371
#define HEVC_ACC1REG2 0x3372
#define HEVC_CPU_TRACE 0x3380
/**/

#endif

