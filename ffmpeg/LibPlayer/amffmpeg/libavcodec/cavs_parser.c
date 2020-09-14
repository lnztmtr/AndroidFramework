/*
 * Chinese AVS video (AVS1-P2, JiZhun profile) parser.
 * Copyright (c) 2006  Stefan Gehrer <stefan.gehrer@gmx.de>
 *
 * This file is part of FFmpeg.
 *
 * FFmpeg is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * FFmpeg is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with FFmpeg; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

/**
 * @file
 * Chinese AVS video (AVS1-P2, JiZhun profile) parser
 * @author Stefan Gehrer <stefan.gehrer@gmx.de>
 */

#include "parser.h"
#include "cavs.h"

const AVRational avs2_fps_tab[] = {
    {   0,   0},
    {1001, 24000},
    {   1,   24},
    {   1,   25},
    {1001, 30000},
    {   1,   30},
    {   1,   50},
    {1001, 60000},
    {   1,   60},
    {   1,  100},
    {   1,  120},
    {   1,  200},
    {   1,  240},
    {   1,  300},
    {   0,   0},
};

/**
 * finds the end of the current frame in the bitstream.
 * @return the position of the first byte of the next frame, or -1
 */
static int cavs_find_frame_end(ParseContext *pc, const uint8_t *buf,
                               int buf_size) {
    int pic_found, i;
    uint32_t state;

    pic_found= pc->frame_start_found;
    state= pc->state;

    i=0;
    if(!pic_found){
        for(i=0; i<buf_size; i++){
            state= (state<<8) | buf[i];
            if(state == PIC_I_START_CODE || state == PIC_PB_START_CODE){
                i++;
                pic_found=1;
                break;
            }
        }
    }

    if(pic_found){
        /* EOF considered as end of frame */
        if (buf_size == 0)
            return 0;
        for(; i<buf_size; i++){
            state= (state<<8) | buf[i];
            if((state&0xFFFFFF00) == 0x100){
                if(state > SLICE_MAX_START_CODE){
                    pc->frame_start_found=0;
                    pc->state=-1;
                    return i-3;
                }
            }
        }
    }
    pc->frame_start_found= pic_found;
    pc->state= state;
    return END_NOT_FOUND;
}

static void cavs2video_extract_headers(AVCodecParserContext *s,
                           AVCodecContext *avctx,
                           const uint8_t *buf, int buf_size)
{
    ParseContext1 *pc = s->priv_data;
    const uint8_t *buf_end = buf + buf_size;
    int bytes_left, start_code = -1;
    int profile_id, level_id, progressive_seq, field_coded_seq;
    int chroma_format, sample_precision, enc_precision;
    int aspect_ratio, frame_rate_code;

    while (buf < buf_end) {
        buf = ff_find_start_code(buf, buf_end, &start_code);
        bytes_left = buf_end - buf;

        if ((start_code & 0xFFFFFE00) || buf == buf_end) {
            return ;
        }

	switch (start_code) {
        case CAVS_START_CODE:
            profile_id		= AV_RB8(buf);
            level_id		= AV_RB8(buf + 1);
            progressive_seq	= AV_RB16(buf + 2) >> 15;
            field_coded_seq	= AV_RB16(buf + 2) >> 14 & 0x1;
            chroma_format	= AV_RB16(buf + 4) & 0x3;
            sample_precision	= AV_RB16(buf + 6) >> 13;
            aspect_ratio	= AV_RB16(buf + 6) >> 6 & 0xf;
            frame_rate_code	= AV_RB16(buf + 6) >> 2 & 0xf;

            if (profile_id == 0x22)
                enc_precision	= AV_RB16(buf + 6) >> 10 & 0x3;

            avctx->width	= AV_RB16(buf + 2) & 0x3fff;
            avctx->height	= AV_RB16(buf + 4) >> 2 & 0x3fff;
            avctx->time_base.den = avs2_fps_tab[frame_rate_code].den;
            avctx->time_base.num = avs2_fps_tab[frame_rate_code].num;
            break;
        case PIC_I_START_CODE:
            s->pict_type         = AV_PICTURE_TYPE_I;
            s->key_frame         = 1;
            av_log(NULL, AV_LOG_ERROR, "[%s %d] avs2 I frame\n", __FUNCTION__, __LINE__);
            break;
        case PIC_PB_START_CODE:
            if ((*(buf+4) & 0x3)  == 2) {
                s->pict_type  = AV_PICTURE_TYPE_B;
                s->key_frame = -1;
                //av_log(NULL, AV_LOG_ERROR, "[%s %d] avs B frame\n", __FUNCTION__, __LINE__);
            } else if ((*(buf+4) & 0x3 ) == 1) {
                s->pict_type  = AV_PICTURE_TYPE_P;
                s->key_frame = -1;
                //av_log(NULL, AV_LOG_ERROR, "[%s %d] avs P frame\n", __FUNCTION__, __LINE__);
            }
            break;
        default:
            break;
        }
    }
}

static int cavsvideo_parse(AVCodecParserContext *s,
                           AVCodecContext *avctx,
                           const uint8_t **poutbuf, int *poutbuf_size,
                           const uint8_t *buf, int buf_size)
{
    ParseContext *pc = s->priv_data;
    int next;

    if(s->flags & PARSER_FLAG_COMPLETE_FRAMES){
        next= buf_size;
    }else{
        next= cavs_find_frame_end(pc, buf, buf_size);

        if (ff_combine_frame(pc, next, &buf, &buf_size) < 0) {
            *poutbuf = NULL;
            *poutbuf_size = 0;
            return buf_size;
        }
    }

    if(*buf == 0x0 && *(buf+1) == 0x0 && *(buf+2) == 0x1 && (*(buf+3) == 0xb0 || *(buf+3) == 0xb3)){
        s->pict_type         = AV_PICTURE_TYPE_I;
        s->key_frame         = 1;
        av_log(NULL, AV_LOG_ERROR, "[%s %d] avs I frame\n", __FUNCTION__, __LINE__);
    }else if(*buf == 0x0 && *(buf+1) == 0x0 && *(buf+2) == 0x1 && *(buf+3) == 0xb6){
        s->pict_type         = AV_PICTURE_TYPE_P;
        s->key_frame         = -1;
    }else{
        s->pict_type         = AV_PICTURE_TYPE_P;
        s->key_frame         = -1;
    }

    *poutbuf = buf;
    *poutbuf_size = buf_size;
    return next;
}

static int cavsvideo_parse_init(AVCodecParserContext *s)
{
    av_log(NULL, AV_LOG_ERROR, "[%s %d]\n", __FUNCTION__, __LINE__);
    return 0;
}

static int cavs2video_parse(AVCodecParserContext *s,
                        AVCodecContext *avctx,
                        const uint8_t **poutbuf, int *poutbuf_size,
                        const uint8_t *buf, int buf_size)
{
    ParseContext *pc = s->priv_data;
    int next;

    if (s->flags & PARSER_FLAG_COMPLETE_FRAMES) {
        next= buf_size;
    } else {
        next= cavs_find_frame_end(pc, buf, buf_size);

       if (ff_combine_frame(pc, next, &buf, &buf_size) < 0) {
            *poutbuf = NULL;
            *poutbuf_size = 0;
            return buf_size;
        }
    }

    if (1) cavs2video_extract_headers(s, avctx, buf, buf_size);
    //avctx->time_base.den = 30000;
    //avctx->time_base.num = 1000;

    *poutbuf = buf;
    *poutbuf_size = buf_size;
    return next;
}

AVCodecParser ff_cavs2video_parser = {
    .codec_ids      = { CODEC_ID_CAVS2 },
    .priv_data_size = sizeof(ParseContext),
    .parser_parse   = cavs2video_parse,
    .parser_close   = ff_parse_close,
    .split          = ff_mpeg4video_split,
};

AVCodecParser ff_cavsvideo_parser = {
    { CODEC_ID_CAVS },
    sizeof(ParseContext1),
    cavsvideo_parse_init,
    cavsvideo_parse,
    ff_parse1_close,
    ff_mpeg4video_split,
};
