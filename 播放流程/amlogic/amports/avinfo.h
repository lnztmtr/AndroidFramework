/*
 * avinfo.h
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

#ifndef AVINFO_H_H
#define AVINFO_H_H

#include <linux/amlogic/amports/vformat.h>
#include <linux/amlogic/amports/aformat.h>
#include <linux/module.h>
#include <linux/spinlock.h>
#include <linux/kernel.h>
#include <linux/platform_device.h>
#include <linux/amlogic/amports/amstream.h>
#include <linux/amlogic/amports/vformat.h>
#include <linux/amlogic/amports/aformat.h>
#include <linux/amlogic/amports/tsync.h>
#include <linux/amlogic/amports/ptsserv.h>
#include <linux/amlogic/amports/timestamp.h>


/*get some time pts count to know write data and decode speed*/
struct in_out_pts_info {
	int check_time_ms;
	int64_t checkin_vpts_count;
	int64_t checkout_vpts_count;
};


/*basic audio and video info */
struct am_av_info_t {
	/*auido info*/
	int sample_rate;
	int channels;
	enum aformat_e aformat_type;
	unsigned int apts;
	unsigned int apts_err_num;
	/*video info*/
	unsigned int width;
	unsigned int height;
	enum vformat_e vformat_type;
	enum FRAME_FORMAT frame_format;
	unsigned int dec_error_count;
	unsigned int first_pic_coming;
	unsigned int fps;
	unsigned int current_fps;
	unsigned int vpts;
	unsigned int vpts_err_num;
	unsigned int ts_error_num;
	unsigned int first_vpts;

	unsigned int toggle_frame_count;/*toggle frame count*/
	unsigned int dec_err_frame_count;/*vdec error frame count*/
	unsigned int dec_frame_count;/*vdec frame count*/
	unsigned int dec_drop_frame_count;/*drop frame num*/

	enum tsync_mode_e tsync_mode;
};

struct pts_discontinue_param {
	int pts_dis_num;
	struct pts_discontinue_info pts_dis_info[PTS_DIS_NUM_PER_TIME];
};

struct am_av_param_info_t {
	struct am_av_info_t av_info;
	/*struct vframe_qos_s vframe_qos[QOS_FRAME_NUM];*/
	struct pts_discontinue_param pts_dis_param;
	struct in_out_pts_info check_pts_info;
	struct av_buffer_status buf_status;
};

#endif				/* AVINFO_H_H */
