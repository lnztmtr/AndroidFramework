/**
* @file codec_info.c
* @brief  Codec control lib functions
* @author chuanqi.wang <chuanqi.wang@amlogic.com>
* @version 1.0.0
* @date 2011-02-24
*/
/* Copyright (C) 2007-2011, Amlogic Inc.
* All right reserved
*
*/
#include <stdio.h>
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/poll.h>
#include <errno.h>
#include <sys/ioctl.h>
#include <fcntl.h>
#include <codec_error.h>
#include <codec_type.h>
#include <codec.h>
#include <audio_priv.h>
#include <amsub_ctr.h>    // amsub
#include "codec_h_ctrl.h"
#include <adec-external-ctrl.h>
#include <Amvideoutils.h>
#include "codec_info.h"

#define  log_e(fmt, ...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,"[%s:%d]" fmt "\n", __FUNCTION__, __LINE__, ##__VA_ARGS__)

static int64_t codec_av_gettime(void)
{
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return (int64_t)tv.tv_sec * 1000000 + tv.tv_usec;
}
int codec_get_blurred_screen(struct av_info_t *info,struct codec_quality_info* pblurred_screen)
{
        static int blurred_number = 0;
        static int blurred_number_first = 0;
        static int checkin_number_prev = 0;
        static unsigned int last_toggle_count = 0;
        int ratio = 0;
        static int checkin_number_after = 0;
        int cur_toggle_count = 0;
        static int64_t s_nlast_blur_time_ms = 0;
        int64_t cur_time_ms = 0;
        int send = 0;
        if (pblurred_screen == NULL ||info == NULL) {
            CODEC_PRINT("codec_get_blurred_screen NULL Pointer\n");
            return 0;
        }
        /* +[SE] [BUG][BUG-173477][zhizhong] modify:Not decrease drop count which cause blur_num 0*/
        blurred_number = info->dec_err_frame_count;
        cur_toggle_count = info->toggle_frame_count;
        #if 0
        CODEC_PRINT("blurred_number = %d,cur_toggle_count = %d,pblurred_screen->blurred_number_last = %d,pblurred_screen->blurred_flag = %d\n",
            blurred_number, cur_toggle_count, pblurred_screen->blurred_number_last, pblurred_screen->blurred_flag);
        #endif
        if ((blurred_number != pblurred_screen->blurred_number_last) && (pblurred_screen->blurred_flag == 0)) {
            pblurred_screen->blurred_flag = 1;
            blurred_number_first = blurred_number;
            pblurred_screen->blurred_number_last = blurred_number_first;
            cur_time_ms = codec_av_gettime()/1000;
            s_nlast_blur_time_ms = cur_time_ms;
            last_toggle_count = cur_toggle_count;
            checkin_number_prev = info->dec_frame_count;
            pblurred_screen->cur_time_ms = cur_time_ms;
            CODEC_PRINT("blur start: cnt:%d (err:%d,drop:%d) total:%d\n", blurred_number, info->dec_err_frame_count, info->dec_drop_frame_count,  checkin_number_prev);
            send = 1;
        }else if (pblurred_screen->blurred_flag == 1){
            cur_time_ms = codec_av_gettime()/1000;
            if(blurred_number == pblurred_screen->blurred_number_last) {
                if (cur_time_ms - s_nlast_blur_time_ms >= am_getconfig_int_def("media.amplayer.blurdelayms", 800)
                    && cur_toggle_count - last_toggle_count >= am_getconfig_int_def("media.amplayer.togglenum", 15)) {
                    pblurred_screen->blurred_flag = 0;
                    checkin_number_after =  info->dec_frame_count;
                    int decode_total = 0;
                    int decoder_total_err = 0;
                    decode_total = abs(checkin_number_after - checkin_number_prev);
                    CODEC_PRINT("blur end:total(%d - > %d), error(%d - > %d)\n",
                        checkin_number_prev, checkin_number_after, blurred_number_first, blurred_number);
                    if (decode_total > 0) {
                        decoder_total_err = abs(blurred_number - blurred_number_first);
                        ratio = 100 *decoder_total_err/decode_total;
                    }
                    pblurred_screen->cur_time_ms = cur_time_ms;
                    pblurred_screen->blurred_number_last = blurred_number;
                    pblurred_screen->ratio = ratio;
                    last_toggle_count = cur_toggle_count;
                    send = 1;
                }
            } else {
                s_nlast_blur_time_ms = cur_time_ms;
                pblurred_screen->blurred_number_last = blurred_number;
                last_toggle_count = cur_toggle_count;
            }
        }
        return send;
}
#if 0
/* +[SE] [BUG][IPTV-1021][yinli.xia] added: add probe event for blurred and unload event*/
int codec_get_upload(struct av_info_t *info,struct codec_quality_info* pupload_info)
{
        int ncur_toggle_count = 0;
        int64_t ncur_time_ms = 0;
        int send = 0;
        if (pupload_info == NULL ||info == NULL) {
            CODEC_PRINT("codec_get_upload NULL Pointer\n");
            return 0;
        }
        ncur_toggle_count = info->toggle_frame_count;
        ncur_time_ms = (int64_t)codec_av_gettime() / 1000;
        if (pupload_info->unload_flag == 0) {
            if (ncur_toggle_count - pupload_info->last_toggled_num >= info->fps - 5 &&
                (ncur_toggle_count >= 25) && (info->fps != 0)) {
                pupload_info->unload_flag = 0;
            } else if ((ncur_toggle_count - pupload_info->last_toggled_num < info->fps - 5) &&
                (ncur_toggle_count >= 25)) {
                pupload_info->unload_flag = 1;
                send = 1;
            }
            if (ncur_toggle_count > 10 && ncur_toggle_count == pupload_info->last_toggled_num) {
                if ((ncur_time_ms - pupload_info->last_untoggled_time_ms) >= am_getconfig_int_def("media.amplayer.unloadstartms", 500)){
                    pupload_info->unload_flag = 1;
                    CODEC_PRINT("unload start\n");
                    send = 1;
                    pupload_info->last_untoggled_time_ms = ncur_time_ms;
                }
            } else {
                pupload_info->last_toggled_num = ncur_toggle_count;
                pupload_info->last_untoggled_time_ms = ncur_time_ms;
            }
        } else if (pupload_info->unload_flag == 1) {
            int toggled = ncur_toggle_count - pupload_info->last_toggled_num;
            //if (pupload_info->blurred_flag == 0) {
                if ((ncur_time_ms - pupload_info->last_untoggled_time_ms) >= am_getconfig_int_def("media.amplayer.unloadendms", 500)) {
                    if (toggled >= am_getconfig_int_def("media.amplayer.togglenum", 10)) {
                        CODEC_PRINT("unload end toggle(%d -> %d):\n", pupload_info->last_toggled_num, ncur_toggle_count);
                        pupload_info->unload_flag = 0;
                        send = 1;
                    }
                    //pupload_info->last_untoggled_time_ms = ncur_time_ms;
                    //pupload_info->last_toggled_num = ncur_toggle_count;
                //}
            } else {
                pupload_info->last_untoggled_time_ms = ncur_time_ms;
                pupload_info->last_toggled_num = ncur_toggle_count;
            }
        }
        return send;
}
#else
/* +[SE] [BUG][IPTV-1332][IPTV-1345][jipeng.zhao] revert IPTV-1021 change, use cmcc 20180721 version */
int codec_get_upload(struct av_info_t *info,struct codec_quality_info* pupload_info)
{
        int ncur_toggle_count = 0;
        int64_t ncur_time_ms = 0;
        int send = 0;
        if (pupload_info == NULL ||info == NULL) {
            CODEC_PRINT("codec_get_upload NULL Pointer\n");
            return 0;
        }
        ncur_toggle_count = info->toggle_frame_count;
        ncur_time_ms = (int64_t)codec_av_gettime() / 1000;
        /*[SE][BUG][IPTV-5332][jingtian.wang]:[Sunniwell][S905L3][CMCC][Shandong][Online]:RTSP protocol play disconnect probe report unload,100%*/
        #if 0
            CODEC_PRINT("[%s]unload_flag=%d, toggle=(%d,%d), time(%lld->%lld), fps=(%d,%d,%d)\n",
                __FUNCTION__, pupload_info->unload_flag,
                pupload_info->last_toggled_num, ncur_toggle_count,
                pupload_info->last_untoggled_time_ms, ncur_time_ms,
                info->fps, info->current_fps, info->frame_format);
        #endif
        if (pupload_info->unload_reset_flag == 1) {
            pupload_info->last_untoggled_time_ms = ncur_time_ms;
        }
        if (pupload_info->unload_flag == 0) {
            if (ncur_toggle_count > 10 && ncur_toggle_count == pupload_info->last_toggled_num) {
                if ((ncur_time_ms - pupload_info->last_untoggled_time_ms) >= am_getconfig_int_def("media.amplayer.unloadstartms", 500)) {
                    pupload_info->unload_flag = 1;
                    CODEC_PRINT("unload start\n");
                    send = 1;
                    pupload_info->last_untoggled_time_ms = ncur_time_ms;
                }
            } else {
                pupload_info->last_toggled_num = ncur_toggle_count;
                pupload_info->last_untoggled_time_ms = ncur_time_ms;
            }
        } else if (pupload_info->unload_flag == 1) {
            int toggled = ncur_toggle_count - pupload_info->last_toggled_num;
            if (pupload_info->blurred_flag == 0) {
                if ((ncur_time_ms - pupload_info->last_untoggled_time_ms) >= am_getconfig_int_def("media.amplayer.unloadendms", 800)) {
                    if (toggled >= am_getconfig_int_def("media.amplayer.togglenum", 15)) {
                        CODEC_PRINT("unload end toggle(%d -> %d):\n", pupload_info->last_toggled_num, ncur_toggle_count);
                        pupload_info->unload_flag = 0;
                        send = 1;
                    }
                    pupload_info->last_untoggled_time_ms = ncur_time_ms;
                    pupload_info->last_toggled_num = ncur_toggle_count;
                }
            } else {
                pupload_info->last_untoggled_time_ms = ncur_time_ms;
                pupload_info->last_toggled_num = ncur_toggle_count;
            }
        }
        return send;
}
#endif


/*********************************
 * mobile version
 */

static int unload_switching = 0;
static int blur_switching = 0;

/* [SE] [BUG][IPTV-4695][IPTV-4165] [jipeng] blur event frequent*/
int codec_get_blurred_screen_cmcc(struct av_info_t *info,struct codec_quality_info* pblurred_screen,codec_para_t  *p_codec, int *checktime)
{
    static int blur_debug_count=0;
    static int blurred_number = 0;
    static int blurred_number_first = 0;
    static int checkin_number_prev = 0;
    static unsigned int last_toggle_count = 0;
    static int blur_number_diff_start = 0;
    static int blur_out_toggle_start = 0;
    int ratio = 0;
    static int checkin_number_after = 0;
    int cur_toggle_count = 0;
    static int64_t s_nlast_blur_time_ms = 0;
    int64_t cur_time_ms = codec_av_gettime()/1000;
    int send = 0;
    static int force_start = 0;
    int decode_total = 0;
    int decoder_total_err = 0;
    int diff_no_blurtime = 0;
    int out_period0 = am_getconfig_int_def("amplayer.blur.outperoid", 1500);
    int out_period1 = am_getconfig_int_def("amplayer.blur.outperoid1", 1000);

    if (pblurred_screen == NULL ||info == NULL) {
        CODEC_PRINT("codec_get_blurred_screen NULL Pointer\n");
        return 0;
    }

    blurred_number = info->dec_err_frame_count;
    cur_toggle_count = info->toggle_frame_count;
    if ( info->toggle_frame_count <= 10 )
    {
        pblurred_screen->blurred_number_last = blurred_number;
        return 0;
    }
    #if 0
    CODEC_PRINT("[%s:%d]flag=(%d,%d), switching=(%d,%d), err=(%d->%d),dec=(%d/%d), time(%lld->%lld), fps=(%d,%d,%d)\n",
        __FUNCTION__, __LINE__, pblurred_screen->unload_flag, pblurred_screen->blurred_flag, unload_switching, blur_switching,
        pblurred_screen->blurred_number_last, blurred_number,
        info->dec_frame_count, info->toggle_frame_count,
        s_nlast_blur_time_ms, cur_time_ms,
        info->fps, info->current_fps, info->frame_format);
    #endif

    if (pblurred_screen->blurred_flag == 0) {
        if (blur_switching == 0) {
            if (blurred_number != pblurred_screen->blurred_number_last || force_start) {
                blur_switching = 1;
                blur_number_diff_start = pblurred_screen->blurred_number_last;//blurred_number;
                s_nlast_blur_time_ms = cur_time_ms;
                CODEC_PRINT("[%s]try to in, force_start=%d\n", __FUNCTION__, force_start);
                if (force_start == 1)
                    force_start = 0;
            }
        } else {
            if ( (cur_time_ms - s_nlast_blur_time_ms) >= am_getconfig_int_def("amplayer.blur.inperoid", 200)) {
                blur_switching = 0;
                int delta_err = blurred_number - blur_number_diff_start;
                if ( delta_err >= am_getconfig_int_def("amplayer.blur.inlevel", 1) ) {
                    pblurred_screen->blurred_flag = 1;
                    blurred_number_first = blurred_number;
                    pblurred_screen->blurred_number_last = blurred_number_first;

                    s_nlast_blur_time_ms = cur_time_ms;
                    last_toggle_count = cur_toggle_count;
                    checkin_number_prev = info->dec_frame_count;
                    pblurred_screen->cur_time_ms = cur_time_ms;
                    send = 1;
                    CODEC_PRINT("[%s]try to in succed, delta_err=%d\n", __FUNCTION__, delta_err);

                    //case:unload blur mutex
                    if (am_getconfig_int_def("amplayer.codec_info.mutex", 0) == 1) {
                        if (pblurred_screen->unload_flag) {
                            pblurred_screen->blurred_flag = 0;
                            send = 0;
                            CODEC_PRINT("[%s]mutex: unload_flag=1\n", __FUNCTION__);
                        }
                    }
                } else {
                    CODEC_PRINT("[%s]blur in failed,  delta_err=%d\n", __FUNCTION__, delta_err);
                }
            }
        }
    }else if (pblurred_screen->blurred_flag == 1) {
        if (blur_switching == 0) {
            if (blurred_number == pblurred_screen->blurred_number_last) {
                blur_switching = 1;
                blur_number_diff_start = blurred_number;
                s_nlast_blur_time_ms = cur_time_ms;
                blur_out_toggle_start = info->toggle_frame_count;
                CODEC_PRINT("[%s]try to blur out\n", __FUNCTION__);
            }
        } else {
            diff_no_blurtime = cur_time_ms - s_nlast_blur_time_ms;
            if (diff_no_blurtime >= out_period0) {
                blur_switching = 0;
                int delta_err = blurred_number - blur_number_diff_start;
                int blur_out_toggle_end=info->toggle_frame_count;
                int toggle_diff = blur_out_toggle_end-blur_out_toggle_start;
                int toggled_limit = (float)info->fps/2 * (float)out_period0/1000;
                log_e("delta_err:%d,toggle_diff:%d>%d?",delta_err,toggle_diff,toggled_limit);
                if (delta_err <= am_getconfig_int_def("amplayer.blur.outlevel", 0) &&
                    toggle_diff >= toggled_limit)
                {
                    pblurred_screen->blurred_flag = 0;
                    checkin_number_after =  info->dec_frame_count;

                    decode_total = abs(checkin_number_after - checkin_number_prev);
                    if (decode_total > 0) {
                        decoder_total_err = abs(blurred_number - blurred_number_first);
                        ratio = 100 *decoder_total_err/decode_total;
                    }
                    pblurred_screen->cur_time_ms = cur_time_ms;
                    pblurred_screen->blurred_number_last = blurred_number;
                    pblurred_screen->ratio = ratio;
                    last_toggle_count = cur_toggle_count;
                    *checktime = diff_no_blurtime;
                    send = 1;
                    CODEC_PRINT("[%s]blur out succed, delta_err:%d, toggle_diff=%d(0x%X-0x%X)\n",
                    __FUNCTION__, delta_err, toggle_diff, blur_out_toggle_end, blur_out_toggle_start);
                } else {
                    CODEC_PRINT("[%s]blur out failed, delta_err:%d, toggle_diff=%d(0x%X-0x%X)\n",
                    __FUNCTION__,delta_err, toggle_diff, blur_out_toggle_end, blur_out_toggle_start);
                }
            } else if (blurred_number != pblurred_screen->blurred_number_last) {
                if (diff_no_blurtime >= out_period1) {
                    force_start = 1;
                    CODEC_PRINT("already no blur for :%d ms force quit and start\n",
                                diff_no_blurtime);
                    pblurred_screen->blurred_flag = 0;
                    checkin_number_after =  info->dec_frame_count;

                    decode_total = abs(checkin_number_after - checkin_number_prev);
                    if (decode_total > 0) {
                        decoder_total_err = abs(blurred_number - blurred_number_first);
                        ratio = 100 *decoder_total_err/decode_total;
                    }
                    pblurred_screen->cur_time_ms = cur_time_ms;
                    pblurred_screen->blurred_number_last = blurred_number;
                    pblurred_screen->ratio = ratio;
                    last_toggle_count = cur_toggle_count;
                    *checktime = diff_no_blurtime;
                    send = 1;
                    CODEC_PRINT("[%s]blur out succed1\n", __FUNCTION__);
                }
                blur_switching = 0;
            }
        }
    }

    pblurred_screen->blurred_number_last = blurred_number;
    if (send) {
        CODEC_PRINT("[%s]%s\n", __FUNCTION__,
            pblurred_screen->blurred_flag?"blur start":"blur end");
    }
    return send;
}

static int get_vbuf_read_ptr(struct codec_para_t* pcodec, float* fLevel)
{
    struct buf_status bs;
    codec_get_vbuf_state(pcodec, &bs);
    *fLevel = (float)bs.data_len/bs.size;
    return bs.read_pointer;
}

static int get_scan_fps()
{
    static int last_scan_fps = 0;
    //case:1/2 packet loss, input_fps
    //parse "input_fps:0x1d output_fps:0x1d drop_fps:0x0"
    int scan_fps = 30;
    char val[2][64]={0,0};
    amsysfs_get_sysfs_str("/sys/class/video/fps_info", val[0], sizeof(val[0]));
    sscanf(val[0], "%[^:]:%x", val[1], &scan_fps);
    if (scan_fps<24)
        return 24;
    if (scan_fps>60)
        return 60;
    if (scan_fps>32)
    {
        last_scan_fps = scan_fps;
        return 50;
    }
    if (scan_fps < last_scan_fps)
        return last_scan_fps;

    last_scan_fps = scan_fps;
    return scan_fps;
}

static int get_toggle_limit_by_scan_fps(int scan_fps)
{
    if (scan_fps <= 30)
        return am_getconfig_int_def("amplayer.unload.outlevel30",18);
    if (scan_fps <= 50)
        return am_getconfig_int_def("amplayer.unload.outlevel50",40);
    return 40;
}

#define UNLOAD_INPEROID    150
/* +[SE] [BUG][IPTV-4695][IPTV-4165][jipeng]unload event not paired*/
int codec_get_upload_cmcc(struct av_info_t *info,struct codec_quality_info* pupload_info,codec_para_t  *p_codec, int *checktime)
{
    static int unload_toggled_diff_start = 0;
    static int unload_error_diff_start = 0;
    static int64_t unload_timed_diff_start = 0;
    int toggled_diff3 = 0;
    static int unload_last_dec_frame_count = 0;
    static int unload_err_frame_check_start = 0;
    static int unload_drop_frame_check_start = 0;
    static int unload_debug_count = 0;
    static int unload_last_toggle_num = 0;
    static int vbuf_read_ptr_start = 0;
    int ncur_toggle_count = 0;
    int64_t ncur_time_ms = 0;
    int send = 0;
    if (pupload_info == NULL ||info == NULL) {
        CODEC_PRINT("codec_get_upload NULL Pointer\n");
        return 0;
    }
    ncur_toggle_count = info->toggle_frame_count;
    ncur_time_ms = (int64_t)codec_av_gettime() / 1000;

    if ( info->toggle_frame_count <= 10 )
    {
        unload_last_dec_frame_count = info->dec_frame_count;
        unload_last_toggle_num = info->toggle_frame_count;
        pupload_info->last_untoggled_time_ms = ncur_time_ms;
        pupload_info->last_toggled_num = ncur_toggle_count;
        return 0;
    }

    #if 0
        CODEC_PRINT("[%s:%d]flag=(%d,%d), switching=(%d,%d), toggle=(%d,%d), time(%lld->%lld), fps=(%d,%d,%d)\n",
            __FUNCTION__, __LINE__, pupload_info->unload_flag, pupload_info->blurred_flag, unload_switching, blur_switching,
            unload_last_toggle_num, ncur_toggle_count,
            pupload_info->last_untoggled_time_ms, ncur_time_ms,
            info->fps, info->current_fps, info->frame_format);
    #endif

    int64_t diff_time3 = ncur_time_ms-unload_timed_diff_start;
    int inperoid3 = am_getconfig_int_def("amplayer.unload.inperoid3",1000);
    if (diff_time3 >= inperoid3)
    {
        toggled_diff3 = info->toggle_frame_count - unload_toggled_diff_start;
        int error_diff = info->dec_err_frame_count - unload_error_diff_start;
        int scan_fps = get_scan_fps();
        log_e("scan_fps:%d,toggled_diff:%d,error_diff:%d",scan_fps,toggled_diff3,error_diff);

        unload_timed_diff_start = ncur_time_ms;
        unload_toggled_diff_start = info->toggle_frame_count;
        unload_error_diff_start = info->dec_err_frame_count;
    }

    if (pupload_info->unload_flag == 0) {
        if (unload_switching == 0 ) {
            if (ncur_toggle_count == unload_last_toggle_num) {
                pupload_info->last_untoggled_time_ms = ncur_time_ms;
                pupload_info->last_toggled_num = ncur_toggle_count;
                unload_switching = 1;
                float fLevel=0.0f;
                vbuf_read_ptr_start = get_vbuf_read_ptr(p_codec, &fLevel);
                unload_err_frame_check_start = info->dec_err_frame_count;
                unload_drop_frame_check_start = info->dec_drop_frame_count;
                CODEC_PRINT("[%s]try to in\n", __FUNCTION__);
            }
        } else {
            if ((ncur_time_ms - pupload_info->last_untoggled_time_ms)
                    >=am_getconfig_int_def("amplayer.unload.inperoid",UNLOAD_INPEROID))
            {
                float fLevel = 0.0f;
                int toggled_diff = ncur_toggle_count - pupload_info->last_toggled_num;
                int read_ptr_end = get_vbuf_read_ptr(p_codec, &fLevel);
                int read_ptr_diff = read_ptr_end-vbuf_read_ptr_start;
                int err_frame_diff = info->dec_err_frame_count - unload_err_frame_check_start;
                int drop_frame_diff = info->dec_drop_frame_count - unload_drop_frame_check_start;
                log_e("toggled_diff:%d, read_ptr_diff:%d, fLevel:%f,current_fps:%d,err_frame_diff:%d,drop_frame_diff:%d",
                    toggled_diff, read_ptr_diff, fLevel, info->current_fps, err_frame_diff,drop_frame_diff);
                if ((toggled_diff <= 0 && read_ptr_diff == 0) ||
                    (toggled_diff <= 0 && (ncur_time_ms - pupload_info->last_untoggled_time_ms) >= am_getconfig_int_def("amplayer.unload.inperoid", UNLOAD_INPEROID)*2) ||
                    ((toggled_diff3 < (get_scan_fps()*0.8)) && (diff_time3>=inperoid3) ) )
                {
                    pupload_info->unload_flag = 1;
                    send = 1;
                    CODEC_PRINT("[%s]try to in succed, toggled_diff:%d, read_ptr_diff=%d(0x%X-0x%X)\n", __FUNCTION__,
                        toggled_diff, read_ptr_diff, read_ptr_end, vbuf_read_ptr_start);
                    //case:unload blur mutex
                    if (am_getconfig_int_def("amplayer.codec_info.mutex", 0) == 1) {
                        if (pupload_info->blurred_flag) {
                            pupload_info->unload_flag = 0;
                            send = 0;
                        }
                    }
                    unload_switching = 0;
                } else {
                    CODEC_PRINT("[%s]try to in failed,toggled_diff:%d, read_ptr_diff=%d(0x%X-0x%X)\n", __FUNCTION__,
                        toggled_diff, read_ptr_diff,read_ptr_end, vbuf_read_ptr_start);
                    if (toggled_diff>0 || ((ncur_time_ms - pupload_info->last_untoggled_time_ms) >= am_getconfig_int_def("amplayer.unload.inperoid",UNLOAD_INPEROID)*2))
                    {
                        unload_switching = 0;
                    }
                }
            }else if (ncur_toggle_count != unload_last_toggle_num) {
                unload_switching = 0;
            }
        }
    } else if (pupload_info->unload_flag == 1) {
        if ( unload_switching == 0 ) {
            if (ncur_toggle_count != unload_last_toggle_num) {
                pupload_info->last_untoggled_time_ms = ncur_time_ms;
                pupload_info->last_toggled_num = ncur_toggle_count;
                unload_last_dec_frame_count = info->dec_frame_count;
                unload_error_diff_start = info->dec_err_frame_count;
                unload_switching = 1;
                CODEC_PRINT("[%s]try to out...\n", __FUNCTION__);
            }
        } else {
            int toggled = ncur_toggle_count - pupload_info->last_toggled_num;
            int framed = info->dec_frame_count - unload_last_dec_frame_count;
            int err_diff = info->dec_err_frame_count - unload_error_diff_start;
            int outperoid = am_getconfig_int_def("amplayer.unload.outperoid", 1000);
            if ((ncur_time_ms - pupload_info->last_untoggled_time_ms) >= outperoid)
            {
                unload_switching = 0;
                int scan_fps = get_scan_fps();
                float outlevel = (float)get_toggle_limit_by_scan_fps(scan_fps)*((float)outperoid/1000);
                log_e("scan_fps:%d, toggled:%d>=%.1f?, err_diff:%d",scan_fps, toggled, outlevel, err_diff);
                if ((toggled >= outlevel) || (toggled >= outlevel/2 && err_diff == 0))
                {
                    pupload_info->unload_flag = 0;
                    send = 1;
                    *checktime = outperoid - am_getconfig_int_def("amplayer.unload.inperoid",UNLOAD_INPEROID);
                    log_e("try to out succed, toggled:%d,outlevel:%.f", toggled, outlevel);
                    if (am_getconfig_int_def("amplayer.codec_info.sync", 0) == 1) {
                        if (pupload_info->blurred_flag) {
                            pupload_info->unload_flag = 1;
                            send = 0;
                        }
                    }
                } else {
                    log_e("try to out failed,toggled:%d,outlevel:%.f", toggled, outlevel);
                }
            }
        }
    }

    unload_last_toggle_num = ncur_toggle_count;

    if (send) {
        CODEC_PRINT("[%s]%s\n", __FUNCTION__,
            pupload_info->unload_flag?"unload start":"unload end");
    }
    return send;
}
