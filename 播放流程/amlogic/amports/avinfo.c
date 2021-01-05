/*
 * drivers/amlogic/amports/avinfo.c
 *
 * Copyright (C) 2017 Amlogic, Inc. All rights reserved.
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

#include <linux/module.h>
#include <linux/spinlock.h>
#include <linux/kernel.h>
#include "avinfo.h"
#include "vdec.h"

#define AV_INFO_INTERNAL  (HZ)  /*1 s*/
#define AV_PLAY_INFO_LEN 4096
#define BSIC_AV_INFO_LEN 4096
char basic_av_info_1[BSIC_AV_INFO_LEN];
int av_play_info[AV_PLAY_INFO_LEN];

int play_info_debug = 1;
int get_info_count = 0;
int64_t last_checkin_vpts_count = 0;
int64_t last_checkout_vpts_count = 0;

DEFINE_MUTEX(avinfo_lock);
DEFINE_SPINLOCK(avinfo_get_lock);

static char *player_vformat2str(enum vformat_e value)
{
	switch (value) {
	case VFORMAT_MPEG12:
		return "VFORMAT_MPEG12";
	case VFORMAT_MPEG4:
		return "VFORMAT_MPEG4";
	case VFORMAT_H264:
		return "VFORMAT_H264";
	case VFORMAT_HEVC:
		return "VFORMAT_HEVC";
	case VFORMAT_MJPEG:
		return "VFORMAT_MJPEG";
	case VFORMAT_REAL:
		return "VFORMAT_REAL";
	case VFORMAT_JPEG:
		return "VFORMAT_JPEG";
	case VFORMAT_VC1:
		return "VFORMAT_VC1";
	case VFORMAT_AVS:
		return "VFORMAT_AVS";
	case VFORMAT_H264MVC:
		return "VFORMAT_H264MVC";
	case VFORMAT_H264_4K2K:
		return "VFORMAT_H264_4K2K";
	default:
		return "NOT_SUPPORT VFORMAT";
	}
	return NULL;
}

static char *player_aformat2str(enum aformat_e value)
{
	switch (value) {
	case AFORMAT_MPEG:
		return "AFORMAT_MPEG";
	case AFORMAT_PCM_S16LE:
		return "AFORMAT_PCM_S16LE";
	case AFORMAT_AAC:
		return "AFORMAT_AAC";
	case AFORMAT_AC3:
		return "AFORMAT_AC3";
	case AFORMAT_ALAW:
		return "AFORMAT_ALAW";
	case AFORMAT_MULAW:
		return "AFORMAT_MULAW";
	case AFORMAT_DTS:
		return "AFORMAT_DTS";
	case AFORMAT_PCM_S16BE:
		return "AFORMAT_PCM_S16BE";
	case AFORMAT_FLAC:
		return "AFORMAT_FLAC";
	case AFORMAT_COOK:
		return "AFORMAT_COOK";
	case AFORMAT_PCM_U8:
		return "AFORMAT_PCM_U8";
	case AFORMAT_ADPCM:
		return "AFORMAT_ADPCM";
	case AFORMAT_AMR:
		return "AFORMAT_AMR";
	case AFORMAT_RAAC:
		return "AFORMAT_RAAC";
	case AFORMAT_WMA:
		return "AFORMAT_WMA";
	case AFORMAT_WMAPRO:
		return "AFORMAT_WMAPRO";
	case AFORMAT_PCM_BLURAY:
		return "AFORMAT_PCM_BLURAY";
	case AFORMAT_ALAC:
		return "AFORMAT_ALAC";
	case AFORMAT_VORBIS:
		return "AFORMAT_VORBIS";
	case AFORMAT_AAC_LATM:
		return "AFORMAT_AAC_LATM";
	case AFORMAT_APE:
		return "AFORMAT_APE";
	case AFORMAT_EAC3:
		return "AFORMAT_EAC3";
	case AFORMAT_TRUEHD:
		return "AFORMAT_TRUEHD";
	case AFORMAT_MPEG1:
		return "AFORMAT_MPEG1";
	case AFORMAT_MPEG2:
		return "AFORMAT_MPEG2";
	case AFORMAT_WMAVOI:
		return "AFORMAT_WMAVOI";
	default:
		return "NOT_SUPPORT AFORMAT";
	}
	return NULL;
}


static void am_av_get_check_pts_info(struct in_out_pts_info *pts_info)
{
	ulong flags;
	int cur_checkin_number = get_checkin_number();
	int cur_checkout_number = get_checkout_number();
	pts_info->checkin_vpts_count =
		cur_checkin_number - last_checkin_vpts_count;
	pts_info->checkout_vpts_count =
		cur_checkout_number - last_checkout_vpts_count;
	last_checkin_vpts_count = cur_checkin_number;
	last_checkout_vpts_count = cur_checkout_number;
	return;
}

static void am_av_get_basic_info(struct am_av_info_t *av_info)
{
	struct vdec_info vstatus;
	struct vdec_s vdec;
	/*struct adec_status astatus;*/
	struct audio_info *ainfo;
	if (get_avinfo_vdec_status(&vdec, &vstatus) < 0) {
		pr_err("GET_AVINFO no amstream_vdec_status\n");
	} else {
		av_info->width  = vstatus.frame_width;
		av_info->height = vstatus.frame_height;
		av_info->fps = vstatus.frame_rate;
		av_info->dec_error_count = vstatus.error_count;
		av_info->dec_err_frame_count =
			vstatus.error_frame_count;
		av_info->dec_frame_count =
			vstatus.frame_count;
		av_info->dec_drop_frame_count =
			vstatus.drop_frame_count;
	}

	av_info->frame_format = get_frame_format_for_avinfo();
	av_info->first_pic_coming = get_first_pic_coming();
	av_info->vformat_type = get_stream_vformat();
	/*av_info->current_fps = get_current_fps();*/
	av_info->vpts = timestamp_vpts_get();
	av_info->apts = timestamp_apts_get();
	av_info->vpts_err_num = tsync_get_vpts_error_num();
	av_info->apts_err_num = tsync_get_apts_error_num();
	av_info->ts_error_num = get_discontinue_counter();
	av_info->first_vpts = timestamp_firstvpts_get();
	av_info->toggle_frame_count = get_toggle_frame_count();
	return;

}

static void am_av_pts_discontinue_info(
	struct pts_discontinue_param *pts_dis_param)
{
	int ret = 0;
	ret = tsync_get_discontinue_info(pts_dis_param->pts_dis_info);
	pts_dis_param->pts_dis_num = ret;
	return;
}

/*static void am_av_get_qos_info(struct vframe_qos_s *vframe_qos)
{
	struct vframe_qos_s *vframe_qos1 = vdec_get_qos_info();
	if (vframe_qos1 != NULL) {
		memcpy(vframe_qos, vframe_qos1,
			QOS_FRAME_NUM*sizeof(struct vframe_qos_s));
	}
	return;
}*/

static void am_av_get_buf_info(struct av_buffer_status *buf_status)
{

	int ret = 0;
	ret = amstream_get_buf_status(buf_status);
	return;
}


static void am_av_info_string(struct am_av_param_info_t *am_av_info)
{
	int i = 0;
	char *pbuf = &basic_av_info_1;
	pbuf += sprintf(pbuf, "get_num:%d,", get_info_count);
	/*pts info*/
	pbuf += sprintf(pbuf, "checkin_vpts_count:%d,",
		am_av_info->check_pts_info.checkin_vpts_count);
	pbuf += sprintf(pbuf, "checkout_vpts_count: %d,",
		am_av_info->check_pts_info.checkin_vpts_count);

	/*buf info*/
	/*pbuf += sprintf(pbuf, "vbuf_len:%d,",
		am_av_info->buf_status.vbuf_len);
	pbuf += sprintf(pbuf, "vbuf_size:%d,",
		am_av_info->buf_status.vbuf_size);
	pbuf += sprintf(pbuf, "abuf_len:%d,", am_av_info->buf_status.abuf_len);
	pbuf += sprintf(pbuf, "abuf_size:%d,",
		am_av_info->buf_status.abuf_size);*/
	/*basic av info*/
	pbuf += sprintf(pbuf, "first_pic_coming:%d,",
		am_av_info->av_info.first_pic_coming);
	pbuf += sprintf(pbuf, "vformat_type:%s,",
		player_vformat2str(am_av_info->av_info.vformat_type));
	if (am_av_info->av_info.frame_format == 1) {
		pbuf += sprintf(pbuf, "frame_format:%s,",
		"progressive");
	} else if (am_av_info->av_info.frame_format == 2) {
		pbuf += sprintf(pbuf, "frame_format:%s,",
		"interlace");
	} else {
		pbuf += sprintf(pbuf, "frame_format:%s,",
		"FORMAT_UNKNOW");
	}
	pbuf += sprintf(pbuf, "width:%d,", am_av_info->av_info.width);
	pbuf += sprintf(pbuf, "height:%d,", am_av_info->av_info.height);
	pbuf += sprintf(pbuf, "fps:%d,", am_av_info->av_info.fps);
	pbuf += sprintf(pbuf, "current_fps:%d,",
		am_av_info->av_info.current_fps);
	pbuf += sprintf(pbuf, "first_vpts:%d,", am_av_info->av_info.first_vpts);
	pbuf += sprintf(pbuf, "vpts:0x%x,", am_av_info->av_info.vpts);
	pbuf += sprintf(pbuf, "toggle_frame_count:%d,",
		am_av_info->av_info.toggle_frame_count);
	pbuf += sprintf(pbuf, "dec_error_count:%d,",
		am_av_info->av_info.dec_error_count);
	pbuf += sprintf(pbuf, "dec_err_frame_count:%d,",
		am_av_info->av_info.dec_err_frame_count);
	pbuf += sprintf(pbuf, "dec_frame_count:%d,",
		am_av_info->av_info.dec_frame_count);
	pbuf += sprintf(pbuf, "dec_drop_frame_count:%d,",
		am_av_info->av_info.dec_drop_frame_count);
	pbuf += sprintf(pbuf, "apts:0x%x,", am_av_info->av_info.apts);

	pbuf++;
	pbuf = '\n';
	/*pts discontinue info*/
	/*int dis_pts_num = am_av_info->pts_dis_param.pts_dis_num;

	for (i = 0; i < dis_pts_num; i++) {
		int format =
			am_av_info
			->pts_dis_param.pts_dis_info[i].pts_dis_format;
		pbuf += sprintf(pbuf, "pts_dis_type:%s,",
		am_av_info->pts_dis_param.pts_dis_info[i].pts_dis_format);
		if (format == 1) {
			pbuf += sprintf(pbuf, "num:%d,",
			am_av_info->pts_dis_param.pts_dis_info[i].vpts_dis_num);
		} else if (format == 2) {
			pbuf += sprintf(pbuf, "num:%d,",
			am_av_info->pts_dis_param.pts_dis_info[i].apts_dis_num);
		}

		unsigned int old_apts =
			am_av_info->pts_dis_param.pts_dis_info[i].old_apts;
		unsigned int old_vpts =
			am_av_info->pts_dis_param.pts_dis_info[i].old_vpts;
		unsigned int old_pcr =
			am_av_info->pts_dis_param.pts_dis_info[i].old_pcr;

		pbuf += sprintf(pbuf, "old_info: %d,",
			am_av_info->pts_dis_param.pts_dis_info[i].old_mode);
		pbuf += sprintf(pbuf, "old_apts:0x%x,", old_apts);
		pbuf += sprintf(pbuf, "old_vpts:0x%x,", old_vpts);
		pbuf += sprintf(pbuf, "old_pcr:0x%x,", old_pcr);
		if (old_apts >= old_vpts)
			pbuf += sprintf(pbuf, "av_diff:+0x%x,",
			old_apts - old_vpts);
		else
			pbuf += sprintf(pbuf, "av_diff:-0x%x,",
			old_vpts - old_apts);

		if (old_vpts >= old_pcr)
			pbuf += sprintf(pbuf, "vp_diff:+0x%x",
			old_vpts - old_pcr);
		else
			pbuf += sprintf(pbuf, "vp_diff:-0x%x",
			old_pcr - old_vpts);

		unsigned int new_apts =
			am_av_info->pts_dis_param.pts_dis_info[i].new_apts;
		unsigned int new_vpts =
			am_av_info->pts_dis_param.pts_dis_info[i].new_vpts;
		unsigned int new_pcr =
			am_av_info->pts_dis_param.pts_dis_info[i].new_pcr;

		pbuf += sprintf(pbuf, "new_info: %d,",
			am_av_info->pts_dis_param.pts_dis_info[i].new_mode);
		pbuf += sprintf(pbuf, "new_apts:0x%x,", new_apts);
		pbuf += sprintf(pbuf, "new_vpts:0x%x,", new_vpts);
		pbuf += sprintf(pbuf, "new_pcr:0x%x", new_pcr);
		if (new_apts >= new_vpts)
			pbuf += sprintf(pbuf, "av_diff:+0x%x,",
			new_apts - new_vpts);
		else
			pbuf += sprintf(pbuf, "av_diff:-0x%x,",
			new_vpts - new_apts);
		if (timestamp_vpts_get() >= timestamp_pcrscr_get())
			pbuf += sprintf(pbuf, "vp_diff:+0x%x\n",
			new_vpts - new_pcr);
		else
			pbuf += sprintf(pbuf, "vp_diff:-0x%x\n",
			new_pcr - new_vpts);

	}*/
	pr_info("--1-%s\n", basic_av_info_1);
}


static void am_av_info_get(struct am_av_param_info_t *am_av_info)
{
	if (get_av_info_flag()) {
		get_info_count++;
		am_av_get_check_pts_info(&am_av_info->check_pts_info);
		am_av_get_basic_info(&am_av_info->av_info);
		/*am_av_pts_discontinue_info(&am_av_info.pts_dis_param);*/
		/*am_av_get_qos_info(am_av_info.vframe_qos);*/
		/*am_av_get_buf_info(&am_av_info.buf_status);*/
		am_av_info_string(am_av_info);
	} else {
		get_info_count = 0;
	}
}


static void av_info_timer_func(unsigned long arg)
{
	/*if (play_info_debug == 1)
		am_av_info_get(am_av_info);*/
}


static ssize_t show_play_info(struct class *class,
		struct class_attribute *attr, char *buf)
{
	return sprintf(buf, "%s\n", av_play_info);
}

static ssize_t store_play_info(struct class *class,
		struct class_attribute *attr,
		const char *buf, size_t size)
{
	unsigned mode;
	ssize_t r;

	if (size > AV_PLAY_INFO_LEN)
		size = AV_PLAY_INFO_LEN;

	memset(av_play_info, 0, AV_PLAY_INFO_LEN);
	memcpy(av_play_info, buf, size);
	return size;
}

static ssize_t show_av_info(struct class *class,
		struct class_attribute *attr, char *buf)
{
	mutex_lock(&avinfo_lock);
	if (get_av_info_flag()) {
		get_info_count++;
		char *pbuf = &basic_av_info_1;
		int cur_checkin_number =
			get_checkin_number() - last_checkin_vpts_count;
		int cur_checkout_number =
			get_checkout_number() - last_checkout_vpts_count;
		last_checkin_vpts_count = cur_checkin_number;
		last_checkout_vpts_count = cur_checkout_number;
		pbuf += strlen(basic_av_info_1);
		pbuf += sprintf(pbuf, "get_num:%d,", get_info_count);
		pbuf += sprintf(pbuf, "checkin_vpts_count:%d,",
		cur_checkin_number);
		pbuf += sprintf(pbuf, "checkout_vpts_count: %d,",
		cur_checkout_number);
	} else {
		get_info_count = 0;
	}
	mutex_unlock(&avinfo_lock);
	return sprintf(buf, "%s\n", basic_av_info_1);
}

static ssize_t store_av_info(struct class *class,
		struct class_attribute *attr,
		const char *buf, size_t size)
{
	if (size > BSIC_AV_INFO_LEN)
		size = BSIC_AV_INFO_LEN;
	mutex_lock(&avinfo_lock);
	memset(basic_av_info_1, 0, AV_PLAY_INFO_LEN);
	memcpy(basic_av_info_1, buf, size);
	mutex_unlock(&avinfo_lock);
	return size;
}




static struct class_attribute avinfo_class_attrs[] = {
	__ATTR(play_info, S_IRUGO | S_IWUSR | S_IWGRP | S_IWOTH, show_play_info,
	store_play_info),
	__ATTR(am_av_info, S_IRUGO | S_IWUSR | S_IWGRP | S_IWOTH, show_av_info,
	store_av_info),
	__ATTR_NULL
};

static struct class avinfo_class = {
		.name = "avinfo",
		.class_attrs = avinfo_class_attrs,
	};

static int __init am_avinfo_init(void)
{
	int r;

	r = class_register(&avinfo_class);

	if (r) {
		pr_info("avinfo class create fail.\n");
		return r;
	}

	return 0;
}

static void __exit am_avinfo_exit(void)
{
	class_unregister(&avinfo_class);
}

module_init(am_avinfo_init);
module_exit(am_avinfo_exit);

MODULE_DESCRIPTION("AMLOGIC av info get driver");
MODULE_LICENSE("GPL");
MODULE_AUTHOR("Chengshun Wang <chengshun.wang@amlogic.com>");
