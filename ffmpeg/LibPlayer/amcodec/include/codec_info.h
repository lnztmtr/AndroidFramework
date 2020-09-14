/**
* @file codec_info.h
* @brief  Codec control lib functions
* @author chuanqi.wang <chuanqi.wang@amlogic.com>
* @version 1.0.0
* @date 2011-02-24
*/
/* Copyright (C) 2007-2011, Amlogic Inc.
* All right reserved
*
*/
#ifndef _CODEC_INFO_H_
#define _CODEC_INFO_H_
#include <stdio.h>
#include <stdio.h>
#include <string.h>
#include <amports/amstream.h>
#include <codec_type.h>

struct codec_quality_info {
    int blurred_flag;
    int blurred_number_last;
    int blurred_number;
    int unload_flag;
    int last_toggled_num;
    int ratio;
    int64_t cur_time_ms;
    int64_t last_untoggled_time_ms;
    /*[SE][BUG][IPTV-5332][jingtian.wang]:[Sunniwell][S905L3][CMCC][Shandong][Online]:RTSP protocol play disconnect probe report unload,100%*/
    int unload_reset_flag;
    int LastUnloadFlag;
};
int codec_get_upload(struct av_info_t *info,struct codec_quality_info* pupload_info);
int codec_get_blurred_screen(struct av_info_t *info,struct codec_quality_info* pblurred_screen);
int codec_get_upload_cmcc(struct av_info_t *info,struct codec_quality_info* pupload_info, codec_para_t *p_codec, int *checktime);
int codec_get_blurred_screen_cmcc(struct av_info_t *info,struct codec_quality_info* pblurred_screen, codec_para_t *p_codec, int *checktime);
#endif