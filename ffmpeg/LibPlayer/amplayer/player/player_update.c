/***************************************************
 * name     : player_update.c
 * function : update player parameters, information, status etc.
 * date     :  2010.3.2
 ***************************************************/
#include <fcntl.h>
#include <stdlib.h>
#include <sys/time.h>
#include <string.h>
#include <codec_type.h>
#include <player_set_sys.h>

#include "player_update.h"
#include "player_av.h"
#include "thread_mgt.h"
#include <cutils/properties.h>
#include <amconfigutils.h>

#define CHAPTER_DISCONTINUE_THRESHOLD          (90000*30)
char av_info[4096];
#define AV_INFO		"/sys/class/avinfo/am_av_info"
#define AV_INFO_STR	"first_pic_coming:0,vformat_type:NULL,frame_format:NULL,\
width:0,height:0,fps:0,current_fps:0,first_vpts:0,vpts:0,vpts_err_num:0,\
toggle_frame_count:0,dec_error_count:0,dec_err_frame_count:0,dec_frame_count:0,\
dec_drop_frame_count:0,ts_error_num:0,tsync_mode:0,apts:0,apts_err_num:0,get_num:0,\
checkin_vpts_count:0,checkout_vpts_count: 0,"


static int64_t hls_get_estimate_bps(play_para_t *p_para);
static int player_report_para(play_para_t *p_para,
                                 struct buf_status *vbuf,
                                 struct buf_status *abuf,
                                 struct vdec_status *vdec);

void media_info_init(media_info_t *info)
{
    //MEMSET(info, 0, sizeof(media_info_t));
    info->stream_info.filename = NULL;
    info->stream_info.cur_audio_index   = -1;
    info->stream_info.cur_sub_index     = -1;
    info->stream_info.cur_audio_index   = -1;
    info->stream_info.type              = UNKNOWN_FILE;
}
static int set_stream_info(play_para_t *p_para)
{
    mstream_info_t *info = &p_para->media_info.stream_info;
    AVFormatContext *ctx = p_para->pFormatCtx;
    if (!info || !ctx) {
        return -1;
    }
    info->bitrate   = ctx->bit_rate;
    info->duration  = (int)(ctx->duration / AV_TIME_BASE);
    info->filename  = p_para->file_name;
    info->file_size = p_para->file_size;
    info->type      = p_para->file_type;
    info->has_video = p_para->vstream_info.has_video;
    info->has_audio = p_para->astream_info.has_audio;
    info->has_sub   = p_para->sstream_info.has_sub;
    info->nb_streams        = ctx->nb_streams;
    info->total_video_num   = p_para->vstream_num;
    info->total_audio_num   = p_para->astream_num;
    info->total_sub_num     = p_para->sstream_num;
    if ((p_para->file_type == AVI_FILE && !ctx->seekable) ||
        (p_para->file_type == MKV_FILE && !ctx->support_seek) ||
        (p_para->file_type == H264_FILE)) {
        info->seekable = 0;
    } else {
        info->seekable = 1;
    }

    if (info->seekable == 0) {
        log_print("amplayer send PLAYER_EVENTS_NOT_SUPPORT_SEEKABLE\n");
        send_event(p_para, PLAYER_EVENTS_NOT_SUPPORT_SEEKABLE, 0, 0);
    }

    if (info->total_video_num > MAX_VIDEO_STREAMS) {
        log_error("[set_stream_info]too much video streams(%d)!\n ", info->total_video_num);
        return -2;
    }
    if (info->total_audio_num > MAX_AUDIO_STREAMS) {
        log_error("[set_stream_info]too much audio streams(%d)!\n ", info->total_audio_num);
        return -3;
    }
    if (info->total_audio_num > MAX_SUB_STREAMS) {
        log_error("[set_stream_info]too much sub streams(%d)!\n ", p_para->astream_num);
        return -4;
    }
    info->cur_video_index   = p_para->vstream_info.video_index;
    info->cur_audio_index   = p_para->astream_info.audio_index;
    log_print("set stream info,current audio id:%d\n", p_para->media_info.stream_info.cur_audio_index);
    info->cur_sub_index     = p_para->sstream_info.sub_index;
    info->drm_check         = 0;//(p_para->pFormatCtx->drm.drm_check_value > 0) ? 1 : 0;
    return 0;
}

static int set_vstream_info(play_para_t *p_para)
{
    mstream_info_t *info = &p_para->media_info.stream_info;
    mvideo_info_t *vinfo;
    AVFormatContext *pCtx = p_para->pFormatCtx;
    if (!info || !pCtx) {
        return -1;
    }
    if (info->has_video) {
        unsigned int i;
        int vnum = 0;
        AVStream *pStream;

        for (i = 0; i < pCtx->nb_streams; i ++) {
            pStream = pCtx->streams[i];

            if (pStream->no_program) {
                log_print("[%s:%d]stream %d is no_program\n", __FUNCTION__, __LINE__, i);
                continue;
            }

            if (pStream->codec->codec_type == CODEC_TYPE_VIDEO) {
                vinfo = MALLOC(sizeof(mvideo_info_t));
                MEMSET(vinfo, 0, sizeof(mvideo_info_t));
                vinfo->index       = i;
                vinfo->id          = pStream->id;
                vinfo->width       = pStream->codec->width;
                vinfo->height      = pStream->codec->height;
                if(!pStream->codec->width && !pStream->codec->height)
                {
                      vinfo->width       = 1;
                      vinfo->height      = 1;
                }
                vinfo->duartion    = (int)(pStream->duration * pStream->time_base.num / pStream->time_base.den);
                vinfo->bit_rate    = pStream->codec->bit_rate;
                vinfo->format      = p_para->vstream_info.video_format;
                if (pStream->codec->sample_aspect_ratio.num) {
                    vinfo->aspect_ratio_num = pStream->codec->sample_aspect_ratio.num;
                    vinfo->aspect_ratio_den = pStream->codec->sample_aspect_ratio.den;
                } else {
                    vinfo->aspect_ratio_num = pStream->sample_aspect_ratio.num;
                    vinfo->aspect_ratio_den = pStream->sample_aspect_ratio.den;
                }
                if (p_para->vstream_info.video_codec_type == VIDEO_DEC_FORMAT_H263) {
                    /*Android CTS have bug on check H263's width&height not used aspect ratio,
                                    we set to 1:1 for ignore.*/
                    vinfo->aspect_ratio_num = 1;
                    vinfo->aspect_ratio_den = 1;
                }
                vinfo->frame_rate_num   = pStream->r_frame_rate.num;
                vinfo->frame_rate_den   = pStream->r_frame_rate.den;
                vinfo->video_rotation_degree = pStream->rotation_degree;
                p_para->media_info.video_info[vnum] = vinfo;
                vnum ++;
                if (vnum > p_para->media_info.stream_info.total_video_num) {
                    log_error("[set_vstream_info]video streams exceed!\n");
                    return -2;
                }
            }
        }
    }
    return 0;
}
static int metadata_set_string(AVFormatContext *s, const char *key,
                               char *buf, int buf_size)
{
    AVMetadataTag *tag;
    if ((tag = av_metadata_get(s->metadata, key, NULL, 0))) {
        strncpy(buf, tag->value, buf_size);
    }
    return !!tag;
}
static int get_id3v1_tag(AVFormatContext *s, audio_tag_info *audio_tag)
{
    AVMetadataTag *tag;
    int count = 0;

    if (!audio_tag->title[0]) {
        count += metadata_set_string(s, "title",   audio_tag->title, 30);
    }
    if (!audio_tag->author[0]) {
        count += metadata_set_string(s, "author",  audio_tag->author, 30);
    }
    if (!audio_tag->album[0]) {
        count += metadata_set_string(s, "album",   audio_tag->album, 30);
    }
    if (!audio_tag->year[0]) {
        count += metadata_set_string(s, "year",    audio_tag->year,  4);
    }
    if (!audio_tag->comment[0]) {
        count += metadata_set_string(s, "comment", audio_tag->comment, 30);
    }
    if (!audio_tag->genre[0]) {
        count += metadata_set_string(s, "genre", audio_tag->genre, 32);
    }

    if ((tag = av_metadata_get(s->metadata, "track", NULL, 0))) {
        if (!audio_tag->track) {
            audio_tag->track = atoi(tag->value);
        }
        count++;
    }
    return count;
}
static int get_id3v2_tag(AVFormatContext *s, audio_tag_info *audio_tag)
{
    AVMetadataTag *tag;
    int count = 0;

    if (!audio_tag->title[0]) {
        count += metadata_set_string(s, "TIT2",   audio_tag->title, 512);
    }
    if (!audio_tag->author[0]) {
        count += metadata_set_string(s, "TPE1",  audio_tag->author, 512);
    }
    if (!audio_tag->album[0]) {
        count += metadata_set_string(s, "TALB",   audio_tag->album, 512);
    }
    if (!audio_tag->year[0]) {
        count += metadata_set_string(s, "TYER",    audio_tag->year,  4);
    }
    if (!audio_tag->comment[0]) {
        count += metadata_set_string(s, "COMM", audio_tag->comment, 512);
    }
    if (!audio_tag->genre[0]) {
        count += metadata_set_string(s, "TCON", audio_tag->genre, 32);
    }

    if ((tag = av_metadata_get(s->metadata, "TRCK", NULL, 0))) {
        if (!audio_tag->track) {
            audio_tag->track = atoi(tag->value);
        }
        count++;
    }
    return count;
}
static void get_tag_from_metadata(AVFormatContext *s, audio_tag_info *tag)
{

    get_id3v2_tag(s, tag);
    get_id3v1_tag(s, tag);
}
static int set_astream_info(play_para_t *p_para)
{
    mstream_info_t *info = &p_para->media_info.stream_info;
    maudio_info_t *ainfo;
    AVFormatContext *pCtx = p_para->pFormatCtx;
    if (!info || !pCtx) {
        return -1;
    }
    if (info->has_audio) {
        unsigned int i, j;
        int anum = 0;
        AVStream *pStream;
        for (i = 0; i < pCtx->nb_streams; i ++) {
            pStream = pCtx->streams[i];

            if (pStream->no_program || !pStream->stream_valid) {
                log_print("[%s:%d]stream %d no_program:%d, stream_valid:%d, \n", __FUNCTION__, __LINE__, i, pStream->no_program, pStream->stream_valid);
                continue;
            }

            if (pStream->codec->codec_type == CODEC_TYPE_AUDIO) {
                ainfo = MALLOC(sizeof(maudio_info_t));
                MEMSET(ainfo, 0, sizeof(maudio_info_t));
                ainfo->index     = i;
                ainfo->id           = pStream->id;
                ainfo->channel      = pStream->codec->channels;
                ainfo->sample_rate  = pStream->codec->sample_rate;
                ainfo->duration     = (int)(pStream->duration * pStream->time_base.num / pStream->time_base.den);
                ainfo->bit_rate     = pStream->codec->bit_rate;
                ainfo->aformat      = audio_type_convert(pStream->codec->codec_id, p_para->file_type, 1);
                if (pCtx->drmcontent) {
                    log_print("[%s:%d]DRM content found, not support yet.\n", __FUNCTION__, __LINE__);
                    ainfo->aformat = AFORMAT_UNSUPPORT;
                }
                if (p_para->stream_type == STREAM_AUDIO) {
                    if (ainfo->bit_rate == 0) {
                        ainfo->bit_rate = info->bitrate;
                    }
                    ainfo->audio_tag = MALLOC(sizeof(audio_tag_info));
                    get_tag_from_metadata(pCtx, ainfo->audio_tag);
                }

                AVDictionaryEntry *lang = av_dict_get(pStream->metadata, "language", NULL, 0);
                if (lang) {
                    log_print("[%s %d]Stream %d Audio_Lang_INFO: %s",__FUNCTION__,__LINE__,i,lang->value);
                    int langlen=strlen(lang->value);
                    if (langlen > 128) {
                        log_print("Audio_Lang_INFO too long: cut it to 128 bytes");
                        langlen = 128;
                    }
                    memcpy(ainfo->language,lang->value,langlen);
                }else{
                    log_print("[%s %d]Stream %d Audio_Language_INFO:unkbown\n",__FUNCTION__,__LINE__,i);
                }

                p_para->media_info.audio_info[anum] = ainfo;
                anum ++;
                if (anum > p_para->media_info.stream_info.total_audio_num) {
                    log_error("[set_astream_info]audio streams exceed!\n");
                    return -2;
                }
            }
        }
        p_para->media_info.stream_info.total_audio_num = anum;
    }
    return 0;
}

static void get_sub_metadata(AVDictionary *m, char* tmp, int len)
{
    if (m && !(av_metadata_get(m, "language", NULL, 0)))
    {
        AVDictionaryEntry *tag = NULL;
        while ((tag = av_metadata_get(m, "", tag, AV_DICT_IGNORE_SUFFIX)))
        {
            if (strcmp("language", tag->key))
            {
                av_strlcpy(tmp, tag->value, len);
                //log_print("%s, tag->value=%s\n", __FUNCTION__, tag->value);

            }
        }
    }
}

static int set_sstream_info(play_para_t *p_para)
{
    mstream_info_t *info = &p_para->media_info.stream_info;
    msub_info_t *sinfo;
    AVFormatContext *pCtx = p_para->pFormatCtx;
    if (!info || !pCtx) {
        return -1;
    }
    if (info->has_sub) {
        unsigned int i;
        int snum = 0;
        AVStream *pStream;
        for (i = 0; i < pCtx->nb_streams; i ++) {
            pStream = pCtx->streams[i];

            if (pStream->no_program || !pStream->stream_valid) {
                log_print("[%s:%d]stream %d is no_program %d,stream_valid:%d\n", __FUNCTION__, __LINE__, i, pStream->no_program, pStream->stream_valid);
                continue;
            }

            if (pStream->codec->codec_type == CODEC_TYPE_SUBTITLE) {
                AVMetadataTag *lang = av_metadata_get(pStream->metadata, "language", NULL, 0);
                sinfo = MALLOC(sizeof(msub_info_t));
                MEMSET(sinfo, 0, sizeof(msub_info_t));
                sinfo->index = i;
                sinfo->id       = pStream->id;
                sinfo->internal_external = 0;
                sinfo->width    = pStream->codec->width;
                sinfo->height   = pStream->codec->height;
                sinfo->sub_type = pStream->codec->codec_id;
                int sub_max_len = sizeof(sinfo->sub_language);
                //sinfo->subtitle_size;
                if (lang) {
                    int langlen = strlen(lang->value);
                    if (langlen > sub_max_len) {
                        log_print("Subtitle_Lang_INFO/%s too long: cut it to 128 bytes",lang->value);
                        langlen = sub_max_len;
                    }
                    memcpy(sinfo->sub_language,lang->value,langlen);
                } else {
                    get_sub_metadata(pStream->metadata, &sinfo->sub_language, sub_max_len);
                    log_print("set_sstream_info, lang==NULL, sub_max_len=%d,sinfo->sub_language=%s\n",
                        sub_max_len, sinfo->sub_language);
                }
                p_para->media_info.sub_info[snum] = sinfo;
                snum ++;
                if (snum > p_para->media_info.stream_info.total_sub_num) {
                    log_error("[set_sstream_info]sub streams exceed!\n");
                    return -2;
                }
            }
        }
    }
    return 0;
}

int set_media_info(play_para_t *p_para)
{
    int ret = -1;
    media_info_init(&p_para->media_info);

    ret = set_stream_info(p_para);
    if (ret < 0) {
        log_error("[set_media_info]set_stream_info failed! ret=%d\n", ret);
    }

    ret = set_vstream_info(p_para);
    if (ret < 0) {
        log_error("[set_media_info]set_vstream_info failed! ret=%d\n", ret);
    }

    ret = set_astream_info(p_para);
    if (ret < 0) {
        log_error("[set_media_info]set_astream_info failed ret=%d!\n", ret);
    }


    ret = set_sstream_info(p_para);
    if (ret < 0) {
        log_error("[set_media_info]set_sstream_info failed ret=%d!\n", ret);
    }

    return 0;
}

int set_ps_subtitle_info(play_para_t *p_para, subtitle_info_t *sub_info, int sub_num)
{
    mstream_info_t *info = &p_para->media_info.stream_info;
    msub_info_t *sinfo;
    int i;

    if (!info) {
        log_error("[%s]invalid parameters!\n", __FUNCTION__);
        return PLAYER_EMPTY_P;
    }
    log_print("[%s]total_sub_num=%d new_sub_num=%d\n", __FUNCTION__, info->total_sub_num, sub_num);

    for (i = info->total_sub_num; i < sub_num; i ++) {
        sinfo = MALLOC(sizeof(msub_info_t));
        if (sinfo) {
            MEMSET(sinfo, 0, sizeof(msub_info_t));
            sinfo->id       = sub_info[i].id;
            p_para->media_info.sub_info[i] = sinfo;
            log_print("[%s]sub[%d].id=0x%x\n", __FUNCTION__, i, sinfo->id);
        } else {
            log_error("[%s]malloc [%d] failed!\n", __FUNCTION__, i);
            return PLAYER_NOMEM;
        }
    }
    if (sub_num > 0) {
        info->has_sub = 1;
        info->total_sub_num = sub_num;
        p_para->sstream_info.has_sub = 1;
        p_para->sstream_num = sub_num;
    }
    return PLAYER_SUCCESS;
}


static int check_vcodec_state(codec_para_t *codec, struct vdec_status *dec, struct buf_status *buf)
{
    int ret = 0;

    ret = codec_get_vbuf_state(codec,  buf);
    if (ret != 0) {
        log_error("codec_get_vbuf_state error: %x\n", -ret);
    }

    ret = codec_get_vdec_state(codec, dec);
    if (ret != 0) {
        log_error("codec_get_vdec_state error: %x\n", -ret);
        ret = PLAYER_CHECK_CODEC_ERROR;
    }

    return ret;
}

static int check_acodec_state(codec_para_t *codec, struct adec_status *dec, struct buf_status *buf)
{
    int ret = PLAYER_SUCCESS;

    ret = codec_get_abuf_state(codec,  buf);
    if (ret != 0) {
        log_error("codec_get_abuf_state error: %x\n", -ret);
    }

    ret = codec_get_adec_state(codec, dec);
    if (ret != 0) {
        log_error("codec_get_adec_state error: %x\n", -ret);
        ret = PLAYER_FAILED;
    }
    return 0;
}

static int check_scodec_state(codec_para_t *codec, struct buf_status *buf)
{
    int ret = PLAYER_SUCCESS;

    ret = codec_get_sbuf_state(codec,  buf);
    if (ret != 0) {
        log_error("codec_get_sbuf_state error: %x\n", -ret);
    }
    return 0;
}

int64_t getlpbuffer_buffedsize(play_para_t *p_para)
{
    int64_t buffedsize = -1;
    if (p_para->pFormatCtx && p_para->pFormatCtx->pb) {
        buffedsize =  url_buffed_size(p_para->pFormatCtx->pb);
        //log_print("lpbuffer buffedsize = [%d]\n",buffedsize);
    } else {
        buffedsize = 0;
    }
    return buffedsize;
}
int64_t getstreambuffer_buffedsize(play_para_t *p_para)
{
    int64_t vbuffedsize = -1;
    int64_t abuffedsize = -1;
    int64_t buffedsize = -1;
    int vret = -1;
    int aret = -1;
    codec_para_t    *vcodec = NULL;
    codec_para_t    *acodec = NULL;
    struct buf_status vbuf;
    struct buf_status abuf;
    if ((p_para->stream_type == STREAM_ES)
        || (p_para->stream_type == STREAM_AUDIO)
        || (p_para->stream_type == STREAM_VIDEO)) {
        if (p_para->astream_info.has_audio && p_para->acodec) {
            acodec = p_para->acodec;
        }
        if (p_para->vstream_info.has_video && p_para->vcodec) {
            vcodec = p_para->vcodec;
        }
    } else if (p_para->codec) {
        vcodec = p_para->codec;
        acodec = p_para->codec;
    }

    if (vcodec && p_para->vstream_info.has_video) {
        vret = codec_get_vbuf_state(vcodec,  &vbuf);
        if (vret == 0) {
            vbuffedsize = vbuf.data_len;
            //log_print("vbuf buffedsize[%d]  vbufsize[%d]\n",vbuf.data_len,vbuf.size);
        } else {
            log_error("codec_get_vbuf_state error: %x\n", -vret);
        }
    }
    if (acodec && p_para->astream_info.has_audio) {
        aret = codec_get_abuf_state(acodec, &abuf);
        if (aret == 0) {
            abuffedsize = abuf.data_len;
            //log_print("abuf buffedsize[%d]  abufsize[%d]\n",abuf.data_len,abuf.size);
        } else {
            log_error("codec_get_abuf_state error: %x\n", -aret);
        }
    }
    if (!aret && !vret) {
        buffedsize = vbuffedsize + abuffedsize;
        //log_print("streambuf buffedsize [%d]\n",buffedsize);
    } else {
        buffedsize = 0;
    }
    return buffedsize;
}

static int update_codec_info(play_para_t *p_para,
                             struct buf_status *vbuf,
                             struct buf_status *abuf,
                             struct buf_status *sbuf,
                             struct vdec_status *vdec,
                             struct adec_status *adec)
{
    codec_para_t    *vcodec = NULL;
    codec_para_t    *acodec = NULL;
    codec_para_t    *scodec = NULL;
    if ((p_para->stream_type == STREAM_ES)
        || (p_para->stream_type == STREAM_AUDIO)
        || (p_para->stream_type == STREAM_VIDEO)) {
        if (p_para->astream_info.has_audio && p_para->acodec) {
            acodec = p_para->acodec;
        }
        if (p_para->vstream_info.has_video && p_para->vcodec) {
            vcodec = p_para->vcodec;
        }
        if (p_para->sstream_info.has_sub && p_para->scodec) {
            scodec = p_para->scodec;
        }
    } else if (p_para->codec) {
        vcodec = p_para->codec;
        acodec = p_para->codec;
        scodec = p_para->codec;
    }
    if (vcodec && p_para->vstream_info.has_video) {
        if (check_vcodec_state(vcodec, vdec, vbuf) != 0) {
            log_error("check_vcodec_state error!\n");
            return PLAYER_FAILED;
        }
    }
    if (acodec && p_para->astream_info.has_audio) {
        if (check_acodec_state(acodec, adec, abuf) != 0) {
            log_error("check_acodec_state error!\n");
            return PLAYER_FAILED;
        }
    }
    if (scodec && p_para->sstream_info.has_sub) {
        if (check_scodec_state(scodec, sbuf) != 0) {
            log_error("check_scodec_state error!\n");
            return PLAYER_FAILED;
        }
    }
    return 0;
}
static unsigned int handle_current_time(play_para_t *para, unsigned int scr, unsigned int pts)
{
    player_status sta = get_player_state(para);

    if (!para->playctrl_info.pts_valid) {
        log_debug("[handle_current_time:sta=0x%x]scr=0x%x pts=0x%x\n", sta, scr, pts);
    }

    //if(sta == PLAYER_STOPED || sta == PLAYER_INITING)
    if (sta < PLAYER_RUNNING) {
        return 0;
    }
    if (pts == 0xffffffff) {
        return 0;
    }
    if (!para->playctrl_info.pts_valid) {
        // when diff(pcr, apts/vpts)<1s, think as pcr valid and can used for
        // updating current time
        if (scr > 0 && abs(scr - pts) <= PTS_FREQ) { //in tsync_avevent, pts as u32
            para->playctrl_info.pts_valid = 1;
            log_print("[%s:%d]scr=0x%x pts=0x%x diff=0x%x \n", __FUNCTION__, __LINE__, scr, pts, (scr - pts));
        }
    }

    if (para->playctrl_info.pts_valid) {
        if ((!para->astream_info.has_audio) && (scr > 0 && abs(scr - pts) >= PTS_FREQ / 2)) {
            // no audio, pcr will wait video for 3s, so compare
            return 0;
        }
        /* tsync.c timestamp_pcrscr_set(param -VIDEO_HOLD_THRESHOLD)
                    PCR may be set to a negative number */
        if ( scr > ~(PTS_FREQ *3) && pts < PTS_FREQ *3) {
            log_print("[%s] scr %x, give pts %x\n", __FUNCTION__,  scr, pts);
            return pts;
        }
        return scr;
    } else {
            //video dts valid, use apts update current play time
        if (scr > 0 && abs(scr - pts) > PTS_FREQ * 60 * 10 && !para->playctrl_info.v_dts_valid) {
            //log_print("[%s],video dts valid,apts = 0x%x\n",__FUNCTION__,pts);
            return pts;
        }
        return 0;
    }

}

unsigned int get_pts_pcrscr(play_para_t *p_para)
{
    int handle = 0;
    int size = 0;
    char s[16] = {0};
    unsigned int value = 0;
    codec_para_t *pcodec = NULL;
#if 0
    handle = open("/sys/class/tsync/pts_pcrscr", O_RDONLY);
    if (handle < 0) {
        log_error("[player_get_ctime]open pts_pcrscr error!\n");
        return -1;
    }
    size = read(handle, s, sizeof(s));
    if (size > 0) {
        value = strtoul(s, NULL, 16);
        log_debug("\npcrscr=%x(%d) ", value, value / PTS_FREQ);
    }
    close(handle);
#else
    if (p_para->codec) {
        pcodec = p_para->codec;
    } else if (p_para->vcodec) {
        pcodec = p_para->vcodec;
    } else if (p_para->acodec) {
        pcodec = p_para->acodec;
    } else {
        log_print("[%s]No codec handler\n", __FUNCTION__);
        return -1;
    }

    value = codec_get_pcrscr(pcodec);
#endif
    return value;
}

int get_pts_video(play_para_t *p_para)
{
    int handle = 0;
    int size = 0;
    char s[16] = {0};
    int value = 0;
    codec_para_t *pcodec = NULL;

#if 0
    handle = open("/sys/class/tsync/pts_video", O_RDONLY);
    if (handle < 0) {
        log_print("[player_get_ctime]open pts_pcrscr error!\n");
        return -1;
    }
    size = read(handle, s, sizeof(s));
    if (size > 0) {
        value = strtoul(s, NULL, 16);
        log_debug("video=%x(%d)\n", value, value / PTS_FREQ);
    }
    close(handle);
#else
    if (p_para->codec) {
        pcodec = p_para->codec;
    } else if (p_para->vcodec) {
        pcodec = p_para->vcodec;
    } else {
        log_print("[%s]No codec handler\n", __FUNCTION__);
        return -1;
    }

    value = codec_get_vpts(pcodec);
#endif
    return value;
}

int get_pts_audio(play_para_t *p_para)
{
    int handle = 0;
    int size = 0;
    char s[16] = {0};
    int value = 0;
    codec_para_t *pcodec = NULL;

#if 0
    handle = open("/sys/class/tsync/pts_audio", O_RDONLY);
    if (handle < 0) {
        log_error("[player_get_ctime]open pts_audio error!\n");
        return -1;
    }
    size = read(handle, s, sizeof(s));
    if (size > 0) {
        value = strtoul(s, NULL, 16);
        log_debug("audio=%x(%d)\n", value, value / PTS_FREQ);
    }
    close(handle);
#else
    if (p_para->codec) {
        pcodec = p_para->codec;
    } else if (p_para->acodec) {
        pcodec = p_para->acodec;
    } else {
        log_print("[%s]No codec handler\n", __FUNCTION__);
        return -1;
    }

    value = codec_get_apts(pcodec);
#endif
    return value;
}

static int match_ext(const char *filename, const char *extensions)//get file type, .vob,.mp4,.ts...
{
    const char *ext, *p;
    char ext1[32], *q;

    if (!filename) {
        return 0;
    }

    ext = strrchr(filename, '.');
    if (ext) {
        ext++;
        p = extensions;
        for (;;) {
            q = ext1;
            while (*p != '\0' && *p != ',' && q - ext1 < sizeof(ext1) - 1) {
                *q++ = *p++;
            }
            *q = '\0';
            if (!strcasecmp(ext1, ext)) {
                return 1;
            }
            if (*p == '\0') {
                break;
            }
            p++;
        }
    }
    return 0;
}

static unsigned int is_chapter_discontinue(play_para_t *p_para)
{
    char *extensions[4] = {"vob", "VOB", "iso", "ISO"};
    int i = 0;
    if (p_para->pFormatCtx && p_para->pFormatCtx->pb &&
        av_is_segment_media(p_para->pFormatCtx)) {
        return 1;
    }
    if (!strncmp(p_para->pFormatCtx->iformat->name, "cmf", 3)) {
        return 1;
    }
#if 0
    /*
    * Lujian.Hu 2013-01-04
    * for vob file, after seek the first check in pts is 0(so as to other format which is demuxed using the hardware),
    * which could leading to video discontinue in vpts_expire(video.c) function, in get_current_time the
    * discontinue_point is added to the ctime, so the display time is two times the correct time. after comment the
    * following can avoid the error.
    */
    for (i = 0; i < 4; i ++) {
        //log_print("[%s]file_name=%s ext=%s\n", __FUNCTION__, p_para->file_name, extensions[i]);

        if (match_ext(p_para->file_name, extensions[i])) {
            log_print("[%s]return true\n", __FUNCTION__);
            return 1;
        }
    }
#endif
    return 0;
}

/*
  *brief: check if a-v discontinue event both occued
  *         a-v diff <threshold means a-v discontinue both occued
  *param: diff, a-v diff
  *return: 1 if both occued 0 if not
*/
static unsigned int av_interrupted_finished(int diff)
{
    int threshold = PTS_FREQ * 2; //2s
    return (diff > threshold) ? 0 : 1;
}
static unsigned int get_current_time(play_para_t *p_para)
{
    unsigned int pcr_scr = 0, vpts = 0;
	int apts = 0;
    static unsigned int ctime = 0;
    int set_discontinue = 0;
    int audio_pts_discontinue = 0, video_pts_discontinue = 0;
    unsigned long video_pts_discontinue_diff = 0;
    unsigned long audio_pts_discontinue_diff = 0;
    codec_para_t *codec = NULL;
    int use_apts_as_time = 0;
    int time_adjust_flag = 0;
    static unsigned int pts_error = 0;
    static unsigned int last_pts = 0;
    static unsigned int last_ctime = 0;
    if (p_para->vstream_info.has_video) {
        if (p_para->vcodec) {
            codec = p_para->vcodec;
        } else if (p_para->codec) {
            codec = p_para->codec;
        }
    }
    if (codec) {
        audio_pts_discontinue = codec_get_sync_audio_discont(codec);
        video_pts_discontinue = codec_get_sync_video_discont(codec);
        audio_pts_discontinue_diff = codec_get_sync_audio_discont_diff(codec);
        video_pts_discontinue_diff = codec_get_sync_video_discont_diff(codec);
    }
    if (video_pts_discontinue > 0) {
        //log_info("video pts discontinue!, adiff=%lu,vdiff=%lu,\n",audio_pts_discontinue_diff,video_pts_discontinue_diff);
        if (p_para->astream_info.has_audio) {
            use_apts_as_time = 1;
            if (av_interrupted_finished(video_pts_discontinue_diff)) {
                time_adjust_flag = 1;
                use_apts_as_time = 0;
            }
        } else {
            time_adjust_flag = 1;
        }
        if (time_adjust_flag == 1) {
            if (!set_discontinue && is_chapter_discontinue(p_para)) {
                p_para->discontinue_point = p_para->state.current_time;
                set_discontinue = 1;
                p_para->discontinue_flag = 0;
                log_info("vpts discontinue, point=%d\n", p_para->discontinue_point);
                //discontinue handled, remove audio discontinue flag
                if (p_para->astream_info.has_audio && codec) {
                    codec_set_sync_audio_discont(codec, 0);
                    codec_set_sync_audio_discont_diff(codec, 0);
                    audio_pts_discontinue = 0;
                    audio_pts_discontinue_diff = 0;
                }
            }
            if (codec) {
                codec_set_sync_video_discont(codec, 0);
                codec_set_sync_video_discont_diff(codec, 0);
            }
        }
        log_info("vpts discontinue, vpts=0x%x scr=0x%x apts=0x%x vdiff=%lu\n",
                 get_pts_video(p_para), get_pts_pcrscr(p_para), get_pts_audio(p_para), video_pts_discontinue_diff);
    }
    time_adjust_flag = 0;
    if (audio_pts_discontinue > 0) {
        //log_info("audio pts discontinue, curtime=%d lasttime=%d\n", p_para->state.current_time, p_para->state.last_time);
        use_apts_as_time = 0;
        if (p_para->vstream_info.has_video) {
            if (av_interrupted_finished(audio_pts_discontinue_diff)) {
                time_adjust_flag = 1;
                use_apts_as_time = 1;
            }
        } else {
            time_adjust_flag = 1;
        }
        if (time_adjust_flag == 1) {
            if (!set_discontinue && is_chapter_discontinue(p_para)/* &&
            (p_para->state.current_time < p_para->state.last_time)*/) {
                p_para->discontinue_point = p_para->state.current_time;
                set_discontinue = 1;
                p_para->discontinue_flag = 0;
                log_info("apts discontinue, point=%d\n", p_para->discontinue_point);
                if (p_para->vstream_info.has_video && codec) {
                    codec_set_sync_video_discont(codec, 0);
                    codec_set_sync_video_discont_diff(codec, 0);
                    video_pts_discontinue = 0;
                    video_pts_discontinue_diff = 0;
                }
            }
            if (codec) {
                codec_set_sync_audio_discont(codec, 0);
                codec_set_sync_audio_discont_diff(codec, 0);
            }
        }
        log_info("apts discontinue, vpts=0x%x scr=0x%x apts=0x%x adiff=%lu\n",
                 get_pts_video(p_para), get_pts_pcrscr(p_para), get_pts_audio(p_para), audio_pts_discontinue_diff);
    }
    log_debug("***[get_current_time:%d]p_para->state.last_pts=0x%x p_para->state.current_pts:0x%x\n", __LINE__, p_para->state.last_pts,p_para->state.current_pts);
           
    if (p_para->vstream_info.has_video && p_para->astream_info.has_audio) {
        pcr_scr = get_pts_pcrscr(p_para);
        apts = get_pts_audio(p_para);
        vpts = get_pts_video(p_para);
        if (p_para->playctrl_info.pts_valid && use_apts_as_time == 1) {
            ctime = apts;
        } else if (p_para->state.full_time <= 120) {
            log_debug("***[get_current_time:%d]last_pts=0x%x vpts=0x%x p_para->state.current_time:%d pts_error:%d\n", __LINE__, last_pts,vpts,p_para->state.current_time,pts_error);
            if (p_para->state.current_time > 0) {
                if ((last_pts > vpts) && (last_pts - vpts > PTS_FREQ_MS*60*10)) {
                    pts_error = 1;
                    log_debug("***[get_current_time:%d]last_pts=0x%x vpts=0x%x pts_error:%d\n", __LINE__, last_pts,vpts,pts_error);
                }
                
            } else {         
                pts_error = 0;
                log_debug("***[get_current_time:%d]last_pts=0x%x pts_error:%d\n", __LINE__, last_pts,pts_error);
            }
          
            if (pts_error == 1) {
                if (last_pts < vpts) {
                    ctime = last_ctime + (vpts-last_pts);
                    log_debug("***[get_current_time:%d] ctime=0x%x last_ctime:0x%x p_para->state.current_time:0x%x vpts-last_pts:0x%x\n", __LINE__, ctime, last_ctime,vpts-last_pts);
           
                } else
                    ctime = last_ctime;
                log_debug("***[get_current_time:%d] ctime=0x%x p_para->state.current_time:0x%x \n", __LINE__, ctime, p_para->state.current_time);
            } else {
                ctime = handle_current_time(p_para, pcr_scr, apts);
                log_debug("***[get_current_time:%d] p_para->state.current_time: ctime=0x%x\n", __LINE__, ctime, p_para->state.current_time);
            }
            last_pts = vpts;
            log_debug("***[get_current_time:%d]last_pts=0x%x\n", __LINE__, last_pts);
        } else {
            ctime = handle_current_time(p_para, pcr_scr, apts);
            log_debug("***[get_current_time:%d]ctime=0x%x\n", __LINE__, ctime);
        }
        last_ctime = ctime;
        log_debug("***[get_current_time:%d]ctime=0x%x\n", __LINE__, ctime);
    } else if (p_para->astream_info.has_audio)/* &&
            (p_para->stream_type == STREAM_ES) &&
            (p_para->astream_info.audio_format != AFORMAT_WMA)) */{
        apts = get_pts_audio(p_para);
        ctime = apts;
    } else {
        pcr_scr = get_pts_pcrscr(p_para);
        vpts = get_pts_video(p_para);
        ctime = handle_current_time(p_para, pcr_scr, vpts);

        if(p_para->playctrl_info.hls_forward || p_para->playctrl_info.hls_backward){
            ctime = vpts;
			if(p_para->vstream_info.video_format == VFORMAT_HEVC){
				ctime = amsysfs_get_sysfs_int("/sys/module/amvdec_h265/parameters/h265_lookup_vpts");
				if(amsysfs_get_sysfs_int("/sys/module/amvdec_h265/parameters/h265_lookup_vpts")==-1){
					if ((unsigned int)time > 0 && (unsigned int)p_para->state.start_time > 0) {
                			if ((unsigned int)p_para->state.start_time <= (unsigned int)time) {
							ctime = p_para->state.current_ms * PTS_FREQ_MS + 
									(unsigned int)p_para->state.start_time;
							}
					}
					log_print("use current time:%d ms\n",ctime);
				}
			}else {

                ctime = amsysfs_get_sysfs_ulong("/sys/class/video/current_vpts");
                if(ctime >= 0xffffff00){
                    ctime = p_para->state.current_ms * PTS_FREQ_MS + 
									(unsigned int)p_para->state.start_time;
                }

                log_print("use current vf vpts:%x\n",ctime);
            }
		}

    }
    if (ctime == 0) {
        log_debug("[get_current_time] curtime=0x%x pcr=0x%x apts=0x%x vpts=0x%x\n", ctime, pcr_scr, apts, vpts);
    }
    log_debug("===[get_current_time] curtime=0x%x(%d) pcr=0x%x apts=0x%x vpts=0x%x\n", ctime, ctime / PTS_FREQ, pcr_scr, apts, vpts);
    p_para->state.current_pts = pcr_scr;
    return ctime;
}

static void update_current_time(play_para_t *p_para)
{
#define REFRESH_CURTIME_INTERVAL    (100)
    unsigned int time = p_para->state.current_time;
    if (check_time_interrupt(&p_para->state.curtime_old_time, REFRESH_CURTIME_INTERVAL) ||
        !p_para->playctrl_info.pts_valid ||
        (p_para->playctrl_info.end_flag && !p_para->playctrl_info.search_flag)) {
        if (p_para->playctrl_info.f_step > 0 && (p_para->playctrl_info.hls_forward == 0 && p_para->playctrl_info.hls_backward == 0)) {
            time = (unsigned int)p_para->playctrl_info.time_point;
            p_para->state.current_time = time;
            log_print("[%s:%d],p_para->state.current_time:%d", __FUNCTION__,__LINE__,p_para->state.current_time);
            p_para->state.current_ms = (unsigned int)(p_para->playctrl_info.time_point * 1000);
            log_print("[update_current_time]ff/fb:time=%d\n", time);
#ifdef DEBUG_VARIABLE_DUR
            if (p_para->playctrl_info.info_variable) {
                update_variable_info(p_para);
            }
#endif
        } else  if (!p_para->playctrl_info.end_flag) {
            time = get_current_time(p_para);
            /*av delay is too low ,ignore update time.*/
            if (time <= PTS_FREQ_MS * 300 || (p_para->latest_lowlevel_av_delay_ms > 0 && p_para->latest_lowlevel_av_delay_ms <= 10)) {
                return ; /*don't do update if time <=300ms, because it maybe have not init ok.*/
            }
            if (p_para->state.start_time == -1) {
                if (p_para->vstream_info.start_time != -1 && p_para->vstream_info.has_start_time) {
                    p_para->state.start_time = p_para->vstream_info.start_time;
                } else if (p_para->astream_info.start_time != -1 && p_para->astream_info.has_start_time) {
                    p_para->state.start_time = p_para->astream_info.start_time;
                }
                //choose smaller one, if av start time both exist
                if (p_para->vstream_info.start_time >= 0 && p_para->astream_info.start_time >= 0&&
                    p_para->vstream_info.has_start_time && p_para->astream_info.has_start_time) {
                    if (p_para->astream_info.start_time > p_para->vstream_info.start_time) {
                        p_para->state.start_time = p_para->astream_info.start_time;
                    } else {
                        p_para->state.start_time = p_para->vstream_info.start_time;
                    }
                }

            }
            log_debug("[update_current_time]time=%d astart_time=%d  vstart_time=%d last_time=%d\n", time / PTS_FREQ, ((unsigned int)p_para->astream_info.start_time / PTS_FREQ), ((unsigned int)p_para->vstream_info.start_time / PTS_FREQ), p_para->state.last_time);
            if (p_para->state.first_time == -1) {
                p_para->state.first_time = (int64_t)time;
            }

            if ((unsigned int)p_para->state.first_time > 0) {
                if ((unsigned int)p_para->state.first_time < (unsigned int)p_para->state.start_time) {
                    log_print("[update_current_time:%d]time=0x%x start_time=0x%x\n", __LINE__, time, ((unsigned int)p_para->astream_info.start_time));
                    p_para->state.start_time = p_para->state.first_time;
                } else if (((unsigned int)p_para->state.first_time - (unsigned int)p_para->state.start_time) >  0 &&
                           (p_para->state.start_time == 0) && p_para->playctrl_info.time_point == 0) {
                    p_para->state.start_time = p_para->state.first_time;
                    log_print("[update_current_time:%d]reset start_time=0x%x time=0x%x\n", __LINE__, p_para->state.start_time, time);
                }
            }
            if ((unsigned int)time > 0 && (unsigned int)p_para->state.start_time > 0) {
                if ((unsigned int)p_para->state.start_time <= (unsigned int)time || p_para->discontinue_point > 0) {
                    log_debug("[update_current_time:%d]time=0x%x start_time=0x%x\n", __LINE__, time, p_para->state.start_time);
                    time -= (unsigned int)p_para->state.start_time;
                    log_debug("[update_current_time:%d]time=0x%x (%d)\n", __LINE__, time, time / PTS_FREQ);
                } else if ((unsigned int)p_para->state.start_time > (unsigned int)time ) {
                    log_debug("[update_current_time:%d]time=0x%x start_time=0x%x\n", __LINE__, time, p_para->state.start_time);
                    time = 0;
                    log_debug("[update_current_time:%d]time=0x%x (%d)\n", __LINE__, time, time / PTS_FREQ);
                }
            }
            log_debug("[update_current_time:%d]time=%d discontinue=%d\n", __LINE__, time / PTS_FREQ, p_para->discontinue_point);
            if ((p_para->playctrl_info.hls_forward == 0)&&
				(p_para->playctrl_info.hls_backward == 0)&&
				(p_para->discontinue_point != 0)) {
                log_debug("[update_current_time:%d]time=%d dpoint=%d  p_para->discontinue_flag=%d\n", __LINE__, time / PTS_FREQ,
                          p_para->discontinue_point, p_para->discontinue_flag);
                if (p_para->pFormatCtx && p_para->pFormatCtx->pb &&
                    url_support_time_seek(p_para->pFormatCtx->pb) &&
                    (!p_para->discontinue_flag)) {
                    p_para->discontinue_point = p_para->discontinue_point - time / PTS_FREQ;
                    log_print("[update_current_time:%d]time<dpoint dpoint=%d\n", __LINE__, p_para->discontinue_point);
                    p_para->discontinue_flag = 1;
                    p_para->discontinue_time = 0;
                }
                time += p_para->discontinue_point * PTS_FREQ;
                if (((int)(time / PTS_FREQ) > p_para->state.full_time) && (p_para->state.full_time > 0)) {
                    time = p_para->state.full_time * PTS_FREQ;
                }
            }
            log_debug("[update_current_time]time=%d curtime=%d lasttime=%d\n", time / PTS_FREQ, p_para->state.current_time, p_para->state.last_time);
            p_para->state.current_ms = time / PTS_FREQ_MS;
            time /= PTS_FREQ;
        } else if (!p_para->playctrl_info.reset_flag && !p_para->playctrl_info.search_flag && !p_para->playctrl_info.request_end_flag) {
            time = p_para->state.full_time;
            log_print("[update_current_time:%d]play end, curtime: %d\n", __LINE__, time);
        }

        if (p_para->state.current_time != p_para->state.last_time) {
            p_para->state.last_time = p_para->state.current_time;
        }
        p_para->state.current_time = (int)time + 1;

        log_debug("[update_current_time:%d]curtime=%d lasttime=%d tpos=%d full_time=%d\n", __LINE__, p_para->state.current_time, p_para->state.last_time, p_para->playctrl_info.time_point, p_para->state.full_time);

        if (p_para->state.current_time == 0 && 
			p_para->playctrl_info.hls_forward == 0 &&
			p_para->playctrl_info.hls_backward == 0 &&
			p_para->playctrl_info.time_point > 0) {
            p_para->state.current_time = p_para->playctrl_info.time_point;
            log_print("[%s:%d],p_para->state.current_time:%d", __FUNCTION__,__LINE__,p_para->state.current_time);
            p_para->state.current_ms = p_para->playctrl_info.time_point * 1000;
            log_print("[update_current_time:%d]curtime: 0->%f\n", __LINE__, p_para->playctrl_info.time_point);
        }
        if ((p_para->state.current_time > p_para->state.full_time) && (p_para->state.full_time > 0)) {
            //log_print("[update_current_time:%d]time=%d fulltime=%d\n", __LINE__, time, p_para->state.full_time);
            if (p_para->state.current_time > p_para->state.full_time) {
                p_para->state.current_time = p_para->state.full_time;
                log_print("[%s:%d],p_para->state.current_time:%d", __FUNCTION__,__LINE__,p_para->state.current_time);
                p_para->state.current_ms = p_para->state.current_time * 1000;
            }
        }
        log_debug("[update_current_time:%d]time=%d last_time=%d time_point=%d\n", __LINE__, p_para->state.current_time, p_para->state.last_time, p_para->playctrl_info.time_point);

#ifdef DEBUG_VARIABLE_DUR
        if (p_para->playctrl_info.info_variable) {
            update_variable_info(p_para);
        }
#endif
    }
    if (p_para->state.current_time < p_para->state.last_time)
    {
        p_para->state.delta_current_time += p_para->state.last_time;
        log_print("[%s:%d],delta time:%d\n",__FUNCTION__ ,__LINE__, p_para->state.last_time - p_para->state.current_time);
    }

    if (1 == p_para->state.status_overflow)
    {
        if (p_para->state.current_time == p_para->state.start_overflow_time)
        {
            log_print("[%s:%d],during overflow,do nothing\n",__FUNCTION__ ,__LINE__);
        }
        else
        {
            log_print("[%s:%d],finish overflow\n",__FUNCTION__ ,__LINE__);
            p_para->state.status_overflow = 2;
            p_para->state.start_overflow_time = 0;
        }
    }
    else
    {
        if (p_para->state.current_time < p_para->state.last_time)
        {
            log_print("[%s:%d],enter overflow\n",__FUNCTION__ ,__LINE__);
            p_para->state.status_overflow = 1;
            p_para->state.start_overflow_time = p_para->state.current_time;
            p_para->state.delta_current_time += p_para->state.last_time;
            log_print("[%s:%d],delta time:%d\n",__FUNCTION__ ,__LINE__, p_para->state.last_time - p_para->state.current_time);
        }
    }


}

static void update_dec_info(play_para_t *p_para,
                            struct vdec_status *vdec,
                            struct adec_status *adec,
                            struct buf_status *vbuf,
                            struct buf_status *abuf)
{
    int i =0;
    int kernel_fps = 0;
    int video_fps =0;
    int update = 0;
    mvideo_info_t *video_info = NULL;
    if (p_para->vstream_info.has_video && (vdec->status == 0x3F)) { //video dec is ready
        //modified by yzs 
        //if (p_para->vstream_info.video_width == 0) {
	int first_pic = get_sysfs_int("/sys/module/amvideo/parameters/first_frame_toggled");
        if (p_para->vstream_info.video_width <= 16 && first_pic) {
            p_para->vstream_info.video_width = vdec->width;
            p_para->vstream_info.video_height = vdec->height;
            update = 1;//added by yzs
            log_print("first pic coming,width:%d,height:%d\n", p_para->vstream_info.video_width, p_para->vstream_info.video_height);//added by yzs
        } else if (((p_para->vstream_info.video_width != vdec->width) || (p_para->vstream_info.video_height != vdec->height)) && vdec->width > 0 && vdec->width <= 3840 && vdec->height > 0 && vdec->height <= 2160) {
            //some size changed info,temporary changed size ,don't the info;
            //p_para->vstream_info.video_width = vdec->width;
            //p_para->vstream_info.video_height = vdec->height;
            if (!(p_para->playctrl_info.write_end_header_flag && (vdec->width == 16) && (vdec->height == 16))) {
                /* end header is 16x16, skip it */
                p_para->vstream_info.video_width = vdec->width;
                p_para->vstream_info.video_height = vdec->height;
                send_event(p_para, PLAYER_EVENTS_VIDEO_SIZE_CHANGED, vdec->width, vdec->height);
            }
        }
        p_para->state.video_error_cnt = vdec->error_count;
        kernel_fps = vdec->fps;

        for (i = 0; i < p_para->media_info.stream_info.total_video_num; i ++) {
            if (p_para->media_info.video_info[i]) {
                if (p_para->media_info.video_info[i]->index == p_para->vstream_info.video_index) {
                    video_info = p_para->media_info.video_info[i];
                }
            }
        }
        if (video_info&&video_info->frame_rate_num) {
            video_fps = video_info->frame_rate_num/video_info->frame_rate_den;
        }
        //log_print(" video_info:%p,video_info->frame_rate_num:%d\n", video_info,video_info->frame_rate_num);
        if (kernel_fps>1&&kernel_fps<240&&abs(video_fps-kernel_fps)>20) {
            update = 1;
            log_print("fps:%d is error,use kernel fps:%d\n", video_fps, kernel_fps);
        }
        //update mediainfo
        if (update) {
            for (i = 0; i < p_para->media_info.stream_info.total_video_num; i ++) {
                if (p_para->media_info.video_info[i]) {
                    if (p_para->media_info.video_info[i]->index == p_para->vstream_info.video_index) {
                        p_para->media_info.video_info[i]->frame_rate_num = (UNIT_FREQ);
                        p_para->media_info.video_info[i]->frame_rate_den = (UNIT_FREQ)/kernel_fps;
			//added by yzs begin
                        p_para->media_info.video_info[i]->height = p_para->vstream_info.video_height;
                        p_para->media_info.video_info[i]->width = p_para->vstream_info.video_width;
                        log_print("update width:%d,height:%d, num:%d,den:%d\n", p_para->media_info.video_info[i]->width,
                            p_para->media_info.video_info[i]->width,
                            p_para->media_info.video_info[i]->frame_rate_num,
                            p_para->media_info.video_info[i]->frame_rate_den);
                        //added by yzs end
                    }
                }
            }
        }
    }
    if (p_para->astream_info.has_audio) {
        p_para->state.audio_error_cnt = adec->error_count;
        p_para->state.audio_decode_error_cnt = adec->decode_error_count;
    }
}

static void check_avbuf_end(play_para_t *p_para, struct buf_status *vbuf, struct buf_status *abuf)
{
    int vlimit = 0;

    if (p_para->vstream_info.has_video) {
        if (!p_para->playctrl_info.video_low_buffer) {
            if ((p_para->vstream_info.video_format == VFORMAT_MPEG4) &&
                (p_para->vstream_info.video_codec_type == VIDEO_DEC_FORMAT_H263)) {
                vlimit = RESERVE_VIDEO_SIZE << 2;
            } else if (p_para->vstream_info.video_format == VFORMAT_MJPEG) {
                vlimit = RESERVE_VIDEO_SIZE >> 2;
            } else {
                vlimit = RESERVE_VIDEO_SIZE;
            }
            if (vbuf->data_len < vlimit) {
                log_print("[%s:%d]vbuf=0x%x	(limit=0x%x) video_low_buffer\n", __FUNCTION__, __LINE__, vbuf->data_len, vlimit);
                p_para->playctrl_info.video_low_buffer = 1;
            }
        }
    } else {
        p_para->playctrl_info.video_end_flag = 1;
        p_para->playctrl_info.video_low_buffer = 1;
    }

    if (p_para->astream_info.has_audio) {
        if (!p_para->playctrl_info.audio_low_buffer) {
            if (abuf->data_len < RESERVE_AUDIO_SIZE) {
                log_print("[%s:%d]abuf=0x%x	(limit=0x%x) audio_low_buffer\n", __FUNCTION__, __LINE__, abuf->data_len, RESERVE_AUDIO_SIZE);
                p_para->playctrl_info.audio_low_buffer = 1;
            } else if (p_para->astream_info.audio_format == AFORMAT_WMAPRO) {
                int frame_size = p_para->pFormatCtx->streams[p_para->astream_info.audio_index]->codec->block_align;
                if (abuf->data_len < frame_size) {
                    p_para->playctrl_info.audio_low_buffer = 1;
                    log_print("[%s:%d]wmapro audio audio low buffer\n", __FUNCTION__, __LINE__);
                }
            }
        }
    } else {
        p_para->playctrl_info.audio_end_flag = 1;
        p_para->playctrl_info.audio_low_buffer = 1;
    }
    //log_print("[%s:%d]abuf=0x%x   vbuf=0x%x vlow=%d alow=%d\n", __FUNCTION__, __LINE__, \
    //abuf->data_len, vbuf->data_len,p_para->playctrl_info.video_low_buffer,p_para->playctrl_info.audio_low_buffer);

    if ((p_para->playctrl_info.video_low_buffer ||
         p_para->playctrl_info.audio_low_buffer) &&
        (!p_para->playctrl_info.end_flag)) {
        //p_para->state.vdec_buf_rp = vbuf->read_pointer;
        //p_para->state.adec_buf_rp = abuf->read_pointer;

        /* //FFT: do not disable sync for video low level case
               if (p_para->playctrl_info.avsync_enable && p_para->state.current_time >= 3) {
                   set_tsync_enable(0);
                   p_para->playctrl_info.avsync_enable = 0;
                   log_print("[%s:%d]audio or video low buffer ,close av sync!\n", __FUNCTION__, __LINE__);
               }
        */
        if (p_para->playctrl_info.video_low_buffer && 
			p_para->playctrl_info.audio_low_buffer &&
			!p_para->playctrl_info.hls_forward &&
			!p_para->playctrl_info.hls_backward) {
            p_para->playctrl_info.end_flag = 1;
            player_thread_wait(p_para, 1000 * 1000);  // wait 1s for display out all frame
            if ((p_para->state.full_time - p_para->state.current_time) < 20) {
				log_print("set current time full time\n");
                p_para->state.current_time = p_para->state.full_time;
            }
            if (!p_para->playctrl_info.loop_flag && (get_player_state(p_para) != PLAYER_SEARCHING)) {
                set_player_state(p_para, PLAYER_PLAYEND);
                update_playing_info(p_para);
                update_player_states(p_para, 1);
                p_para->state.status = get_player_state(p_para);
                player_clear_ctrl_flags(&p_para->playctrl_info);
                set_black_policy(p_para->playctrl_info.black_out);
                log_print("[%s]low buffer, black=%d\n", __FUNCTION__, p_para->playctrl_info.black_out);
            }
        }
    }
}

static void check_force_end(play_para_t *p_para, struct buf_status *vbuf, struct buf_status *abuf)
{
    int check_flag = 0;
    int result = 0;
    int has_video = p_para->vstream_info.has_video;
    int has_audio = p_para->astream_info.has_audio;
    int aidx = p_para->astream_info.audio_index;
    float vbuf_level = p_para->state.video_bufferlevel;
    float abuf_level = p_para->state.audio_bufferlevel;
    aformat_t audio_fmt = p_para->astream_info.audio_format;
    AVStream *astream = p_para->astream_info.has_audio ? p_para->pFormatCtx->streams[aidx] : NULL;
    int abuf_datalen = abuf->data_len;

    if (has_video) {
        result = vbuf_level < 0.04;
    } else {
        result = 1;
    }
    if (has_audio) {
        result = result && (abuf_level < 0.04);
    }
    //log_print("[%s:%d]result=%d abuf_level=%f\n", __FUNCTION__, __LINE__, result,abuf_level);
    if (has_audio && audio_fmt == AFORMAT_WMAPRO) {
        result = result && (abuf_datalen < astream->codec->block_align);
    }
    //log_print("[%s:%d]end=%d result=%d afmt=%d\n", __FUNCTION__, __LINE__, p_para->playctrl_info.end_flag, result, audio_fmt);
    if (!p_para->playctrl_info.end_flag && result) {
        //log_print("v:%d vlen=0x%x a:%d alen=0x%x count=%d, vrp 0x%x, arp 0x%x\n",
        //    p_para->vstream_info.has_video,vbuf->data_len, p_para->astream_info.has_audio,abuf->data_len,p_para->check_end.end_count,vbuf->read_pointer,abuf->read_pointer);
        int check_cnt = CHECK_END_COUNT;
        int is_bootplay = p_para->start_param->is_bootplay;
        if(is_bootplay)
            check_cnt = am_getconfig_int_def("media.libplayer.boot.checkcnt", 40);//modified by yzs from 5 to 40
        if (has_video) {
            if (p_para->vbuffer.rp_is_changed) {
                p_para->check_end.end_count = check_cnt;
            } else {    //video buffer rp not move
                check_flag = 1;
                log_print("[%s]vrp not move,vrp=vbufrp=0x%x,vlevel=%.03f cnt=%d\n", __FUNCTION__, vbuf->read_pointer, vbuf_level, p_para->check_end.end_count);
            }
        }
        if (has_audio) {
            if (audio_fmt == AFORMAT_AMR ||
                audio_fmt == AFORMAT_PCM_S16LE ||
                audio_fmt == AFORMAT_APE  ||
                audio_fmt == AFORMAT_MPEG  ||
                audio_fmt == AFORMAT_AAC  ||
                audio_fmt == AFORMAT_FLAC ||
                audio_fmt == AFORMAT_MPEG2) {
                if (p_para->state.current_time < p_para->state.full_time && check_audio_output()) {
                    p_para->check_end.end_count = check_cnt;
                }
            }
            if (p_para->abuffer.rp_is_changed) {
                p_para->check_end.end_count = check_cnt;
            } else {
                check_flag = 1;
                log_print("[%s]arp not move,arp=abufrp=0x%x alevel=%.03f cnt=%d\n", __FUNCTION__, abuf->read_pointer, p_para->state.audio_bufferlevel, p_para->check_end.end_count);
            }
        }

        if (check_flag) {
            int dec_unit = 1;
            float total_level = (p_para->state.video_bufferlevel + p_para->state.audio_bufferlevel) + 0.000001;
            while (total_level * dec_unit < 0.02 && dec_unit < 9) {
                dec_unit *= 2;
            }
            p_para->check_end.end_count -= dec_unit;
            if (!p_para->playctrl_info.reset_flag) {
                player_thread_wait(p_para, 100 * 1000); //100ms
            }
            log_debug("[%s %d]full_time:%d,current_time=%d loop_flag:%d\n", __FUNCTION__,__LINE__,p_para->state.full_time,p_para->state.current_time,p_para->playctrl_info.loop_flag);   
            if ((p_para->state.full_time - p_para->state.current_time) <= 1 && (p_para->state.full_time < 120)) {
                sleep(1);
                p_para->playctrl_info.end_flag = 1;
                p_para->playctrl_info.search_flag = 0;
                set_player_state(p_para, PLAYER_PLAYEND);
                update_playing_info(p_para);
                update_player_states(p_para, 1);
                p_para->state.status = get_player_state(p_para);
                player_clear_ctrl_flags(&p_para->playctrl_info);
                set_black_policy(p_para->playctrl_info.black_out);
                log_debug("[%s %d]force end, black=%d\n", __FUNCTION__,__LINE__, p_para->playctrl_info.black_out);  
                return;
            }
            
            if (has_video && p_para->check_end.end_count <= 0) {

                /*if video pts is changed,
                   we think not finished playing video.
                   just wait vpts notchanged.
                  or in some condition,
                  pcr < vpts lead video not show
                */
                unsigned int vpts = get_pts_video(p_para);
                unsigned int pcrscr = get_pts_pcrscr(p_para);
                unsigned int vf_next_pts = get_vf_next_pts();

                //log_print("vpts=0x%x,vf_next_pts=%u,pcrscr=0x%x,latest_pts=0x%x\n",vpts,vf_next_pts,pcrscr,p_para->vbuffer.latest_pts);

                if (vpts != p_para->vbuffer.latest_pts) {
                    p_para->check_end.end_count += check_cnt;
                    p_para->vbuffer.latest_pts = vpts;
                    return ;
                } else {
                    if (pcrscr < vpts && pcrscr + 3 * 90000 > vpts && vf_next_pts != 0) {
                        p_para->check_end.end_count += check_cnt;
                        p_para->vbuffer.latest_pts = vpts;
                        return;

                    } else if (pcrscr >= vpts && pcrscr < vf_next_pts) {
                        p_para->check_end.end_count += check_cnt;
                        p_para->vbuffer.latest_pts = vpts;
                        return;
                    }
                    log_print("pcrscr=0x%x,vpts=0x%x,vf_next_pts=%d\n",pcrscr,vpts,vf_next_pts);
                }
            }
            if (p_para->playctrl_info.switch_param_flag) {
                //log_info("[check_force_end] do nothing when switching format\n");
                return;
            }
            if (p_para->check_end.end_count <= 0 && 
				p_para->playctrl_info.hls_forward == 0 &&
				p_para->playctrl_info.hls_backward == 0) {
                if (!p_para->playctrl_info.video_end_flag) {
                    p_para->playctrl_info.video_end_flag = 1;
                    log_print("[check_force_end]video force end!v:%d vlen=%d count=%d\n", p_para->vstream_info.has_video, vbuf->data_len, p_para->check_end.end_count);
                }
                if (!p_para->playctrl_info.audio_end_flag) {
                    p_para->playctrl_info.audio_end_flag = 1;
                    log_print("[check_force_end]audio force end!a:%d alen=%d count=%d\n", p_para->astream_info.has_audio, abuf->data_len, p_para->check_end.end_count);
                }
                if (p_para->playctrl_info.video_end_flag && p_para->playctrl_info.audio_end_flag) {
                    p_para->playctrl_info.end_flag = 1;
                    p_para->playctrl_info.search_flag = 0;
                    if ((p_para->state.full_time - p_para->state.current_time) < 20) {
                        p_para->state.current_time = p_para->state.full_time;
                        log_print("[%s,%d],p_para->state.current_time=%d\n", __FUNCTION__,__LINE__ ,p_para->state.current_time);
                    }
                    if (!p_para->playctrl_info.loop_flag) {
                        set_player_state(p_para, PLAYER_PLAYEND);
                        update_playing_info(p_para);
                        update_player_states(p_para, 1);
                        p_para->state.status = get_player_state(p_para);
                        player_clear_ctrl_flags(&p_para->playctrl_info);
                        set_black_policy(p_para->playctrl_info.black_out);
                        log_print("[%s]force end, black=%d\n", __FUNCTION__, p_para->playctrl_info.black_out);
                    }
                }
            }
        }
    }
}

static int64_t hls_get_estimate_bps(play_para_t *p_para)
{
    int64_t value = 0;
    ffmpeg_geturl_netstream_info(p_para, 6, &value);
    log_print("Get hls estimate bandwidth: %lld\n", value);
    return value;
}

static int64_t get_measured_bandwidth(play_para_t *p_para)
{
    int64_t value = 0;
    ffmpeg_geturl_netstream_info(p_para, 1, &value);
    //log_print("Get measured bandwidth: %lld\n",value);
    return value;
}
static int64_t get_playing_bandwidth(play_para_t *p_para)
{
    int64_t value = 0;
    ffmpeg_geturl_netstream_info(p_para, 2, &value);
    //log_print("Get measured bandwidth: %lld\n",value);
    return value;
}

int force_buffering_enter(play_para_t *p_para)
{
    int force_buf_enable =  am_getconfig_bool_def("media.amplayer.force_buf_enable", 1);
    int play_after_seek =  am_getconfig_bool_def("media.amplayer.play_after_seek", 0);
	log_print("player->pFormatCtx=%p,pb=%p,Is_segment=%d\n",
			p_para->pFormatCtx,p_para->pFormatCtx->pb,av_is_segment_media(p_para->pFormatCtx));
    if (play_after_seek == 1) {
        p_para->playctrl_info.pause_flag = 0;    // clear pause flag
    }
    if (p_para->playctrl_info.pause_flag) {
        force_buf_enable = 0;    // if pause , no need buffering
    }

    // disable first buffering for resume play case
    if (p_para->resume_play_flag) {
        p_para->resume_play_flag = 0;
        force_buf_enable = 0;
    }

    if (p_para->pFormatCtx->pb == NULL || p_para->pFormatCtx->pb->local_playback == 0) {
        p_para->force_enter_buffering = force_buf_enable;
        // enter buffering here, for quick pause
        if (force_buf_enable) {
            codec_pause(p_para->codec);
            set_player_state(p_para, PLAYER_BUFFERING);
            p_para->state.enter_buffering_time = av_gettime() / 1000;
            log_print("Force enter buffering p_para->state.enter_buffering_time = %lld \n", p_para->state.enter_buffering_time);
            update_player_states(p_para, 1);
            p_para->buffering_enter_time = av_gettime() / 1000;
            log_print("Force enter buffering!!!\n");
        }
    }

    log_print("play_after_seek:%d for_buf_enable:%d pause_flag:%d \n", play_after_seek, force_buf_enable, p_para->playctrl_info.pause_flag);

    return 0;
}

int  audio_digital_mode_change(play_para_t *p_para)
{
    if (p_para->astream_info.audio_format == AFORMAT_AC3 || p_para->astream_info.audio_format == AFORMAT_EAC3 || \
        p_para->astream_info.audio_format == AFORMAT_DTS || p_para->astream_info.audio_format == AFORMAT_TRUEHD) {
        if (get_audio_digital_output_mode() != p_para->audio_digital_raw) {
            p_para->audio_digital_raw = get_audio_digital_output_mode();
            return 1;
        }
    }
    return  0;
}

/*
return : 0 - down  >1-up
*/
static int check_net_phy_conn_status(void)
{
    int nNetDownOrUp = am_getconfig_int_def("net.ethwifi.up",3);//0-eth&wifi both down, 1-eth up, 2-wifi up, 3-eth&wifi both up

    return nNetDownOrUp;
}

int player_avlevel_underflow_reset(play_para_t *p_para)
{
    //called by update_buffering_states, just set reset_flag =1,and other param
    //if avbufflevel underflow timeout 1min, and in livemode =1 ,then recorde reset_flag->1, when do reset, must make sure
    //vdec reset, if not, video shine maybe happen
    player_status eCurPlayStatus = get_player_state(p_para);

    // Not support in vod mode
    if(p_para->start_param->is_livemode != 1)
        return 0;
    // Not Support in timeshift Mode
    int64_t have_end_list = 0;
    int ret = ffmpeg_geturl_netstream_info(p_para,7,&have_end_list);
    if(have_end_list==1)
        return 0;

    // Handle LiveMode==1 Network Disconnect case
    // Direct reset when network down->up
    if(check_net_phy_conn_status() == 0) {
        if(p_para->network_disconnect_starttime <= 0) {
            p_para->network_disconnect_starttime = av_gettime();
            log_print("[%s:%d]Network down in live mode. start:%lld \n", __FUNCTION__, __LINE__, av_gettime());
        }
        return 0;
    }

    if(check_net_phy_conn_status() > 0 && p_para->network_disconnect_starttime > 0) {
        // network reconnect seekto 0s
        player_timesearch(p_para->player_id, 0);
        log_print("[%s:%d]Network up in live mode. end:%lld diff:%lld \n", __FUNCTION__, __LINE__, av_gettime(), (av_gettime()-p_para->network_disconnect_starttime)/1000000);
        log_print("Network Connect Need Reset Player. Disconnect Time:%d s \n", (av_gettime()-p_para->network_disconnect_starttime)/1000000);
        p_para->network_disconnect_starttime = 0;
        return 0;
    }

    // Handle Buffering Case
    // maybe route network down
    if(eCurPlayStatus != PLAYER_BUFFERING) {
        p_para->buffering_state_starttime_sec = 0;
        p_para->player_need_reset = 0;
        return 0;
    }

    if(p_para->buffering_state_starttime_sec > 0 && (av_gettime()/1000000 - p_para->buffering_state_starttime_sec >= am_getconfig_int_def("media.amplayer.bufing_timeout", 60))){
        if (p_para->player_need_reset == 0) { 
            p_para->playctrl_info.time_point = 0;
            p_para->playctrl_info.reset_flag = 1;
            set_black_policy(0);
            p_para->playctrl_info.end_flag = 1;
            p_para->last_network_err_time = 0;
            p_para->last_network_stable_time = 0;
            p_para->player_need_reset = 1;
            p_para->buffering_state_starttime_sec = 0;
            log_print("player buffering state timeout , maybe network block or other problem, need force reset decoder \n");

            return 0;
        }
    }
    
    if(p_para->buffering_state_starttime_sec <= 0)
        p_para->buffering_state_starttime_sec = av_gettime()/1000000;	
    
    return 0;
}

#define FLOAT_ABS(x) ({                               \
                float __x = (x);                 \
                (__x < 0) ? -__x : __x;         \
                })


static int  update_buffering_states(play_para_t *p_para,
                                    struct buf_status *vbuf,
                                    struct buf_status *abuf,
                                    struct buf_status *sbuf)
{
    float alevel, vlevel, slevel;
    float minlevel, maxlevel;
    float avlevel;
    int adelayms = -1, vdelayms = -1, avdelayms = 0;
    float force_exit_buffering_level = 0.0;
    int force_buf_thres = 0;
    int64_t system_time_ms = 0;
    int buffering_timeout = 0;
    int buffering_thres = 0;
    int buffering_wait_audio_s = 0;
	int64_t cache_ms = 0;
	char proj_type[CONFIG_VALUE_MAX];

		float errorbufferringlevel = am_getconfig_float_def("media.amplayer.errorbuflevel", 0.6);
    system_time_ms = (int64_t)av_gettime() / 1000;
    buffering_thres = am_getconfig_int_def("media.amplayer.buf_timeout", 0); // ms
    buffering_wait_audio_s = am_getconfig_int_def("media.amplayer.buffing_wait_audio",2000);//ms
    
    if(buffering_wait_audio_s == 0)//0 buffing wiat audio forever
        buffering_wait_audio_s = p_para->playctrl_info.check_audio_ready_ms;
    
    if (buffering_thres > 0 && p_para->buffering_enter_time > 0) {
        buffering_timeout = ((int)(system_time_ms - p_para->buffering_enter_time) > buffering_thres) ? 1 : 0;
    }

    if (am_getconfig_float("media.amplayer.force_buf_exit", &force_exit_buffering_level) < 0) {
        force_exit_buffering_level = 0.008;//default level equal to 400ms
    }

    force_buf_thres = am_getconfig_int_def("media.amplayer.force_buf_thres", 500); // default 500ms

    if (abuf->size > 0) {
        alevel = (float)abuf->data_len / abuf->size;
        if(p_para->thread_mgt.pthread_id == pthread_self()) {
            ffmepg_seturl_codec_buf_info(p_para, 2, abuf->size);
            ffmepg_seturl_codec_buf_info(p_para, 4, abuf->data_len);
        }
        ffmpeg_set_format_codec_buffer_info(p_para, 2, (int64_t)abuf->size);
        ffmpeg_set_format_codec_buffer_info(p_para, 4, (int64_t)abuf->data_len);
        alevel = alevel > 1 ? 1 : alevel; //maybe big than 1,when the limit buf < bufsize.
    } else {
        alevel = 0;
    }
    if (vbuf->size > 0) {
        vlevel = (float)vbuf->data_len / vbuf->size;
        vlevel = vlevel > 1 ? 1 : vlevel;
        if(p_para->thread_mgt.pthread_id == pthread_self()) {
            ffmepg_seturl_codec_buf_info(p_para, 1, vbuf->size);
            ffmepg_seturl_codec_buf_info(p_para, 3, vbuf->data_len);
        }
        ffmpeg_set_format_codec_buffer_info(p_para, 1, (int64_t)vbuf->size);
        ffmpeg_set_format_codec_buffer_info(p_para, 3, (int64_t)vbuf->data_len);

    } else {
        vlevel = 0;
    }
    if (sbuf->size > 0) {
        slevel = (float)sbuf->data_len / sbuf->size;
        slevel = slevel > 1 ? 1 : slevel;
    } else {
        slevel = 0;
    }

    if (p_para->astream_info.has_audio && p_para->vstream_info.has_video) {
        minlevel = MIN(alevel, vlevel);
        maxlevel = MAX(alevel, vlevel);
    } else if (p_para->astream_info.has_audio) {
        minlevel = alevel;
        maxlevel = alevel;
    } else {
        minlevel = vlevel;
        maxlevel = vlevel;
    }
    avlevel = minlevel;
    if (maxlevel > 0.8) {
        avlevel += maxlevel - 0.8;
        avlevel = MIN(avlevel, 1);
    }
    ffmpeg_seturl_buffered_level(p_para, (int)(10000 * avlevel));
    if (p_para->vstream_info.has_video && get_video_codec(p_para)) {
        codec_get_video_cur_delay_ms(get_video_codec(p_para), &vdelayms);
        avdelayms = vdelayms;
    }
    if (p_para->astream_info.has_audio && get_audio_codec(p_para)) {
        codec_get_audio_cur_delay_ms(get_audio_codec(p_para), &adelayms);
        avdelayms = adelayms;
    }
    if (vdelayms >= 0 && adelayms >= 0) {
        avdelayms = MIN(vdelayms, adelayms);
    }
    p_para->latest_lowlevel_av_delay_ms = avdelayms;
    p_para->state.audio_bufferlevel = alevel;
    p_para->state.video_bufferlevel = vlevel;
    p_para->state.sub_bufferlevel = slevel;

    player_status status = get_player_state(p_para);

    if (p_para->pFormatCtx && p_para->pFormatCtx->pb && p_para->thread_mgt.pthread_id == pthread_self()) {
		if (p_para->playctrl_info.cache_enable == 0) {
			int buftime = -1;;
			p_para->state.bufed_pos = url_buffed_pos(p_para->pFormatCtx->pb);
			p_para->state.download_speed = get_measured_bandwidth(p_para);
			buftime = (int)url_fbuffered_time(p_para->pFormatCtx->pb);
			if (buftime < 0) {
				buftime = (int)av_buffering_data(p_para->pFormatCtx, -1);
			}
			p_para->state.bufed_time = (int)buftime;
		}
    } else {
        p_para->state.bufed_pos = 0;
        p_para->state.bufed_time = 0;
    }
    if (p_para->astream_info.has_audio && 0)
        log_print("update_buffering_states,alevel=%d,asize=%d,level=%f,status=%d\n",
                  abuf->data_len, abuf->size, alevel, get_player_state(p_para));
    if (p_para->vstream_info.has_video && 0)
        log_print("update_buffering_states,vlevel=%d,vsize=%d,level=%f,status=%d\n",
                  vbuf->data_len, vbuf->size, vlevel, get_player_state(p_para));

    if (!p_para->playctrl_info.audio_ready && (system_time_ms-p_para->playctrl_info.check_audio_ready_ms < buffering_wait_audio_s)
            && am_getconfig_int_def("net.ethwifi.up",3) != 0) {
        return 0;
    }

    if (p_para->buffering_force_delay_s > 0 && p_para->force_enter_buffering != 1) {
        if (p_para->buffering_check_point == 0) {
            check_time_interrupt(&p_para->buffering_check_point, -1);
            return 0;
        }
        int download_speed = get_measured_bandwidth(p_para);
        int ds_thres = am_getconfig_int_def("media.amplayer.download_min_thres", (int)(1.5*1024*1024)); // default 500ms
        log_debug("buffering speed:%d min-speed:%d \n", download_speed, ds_thres);
        log_debug("buffering second check point:%d,%f\n",(int)p_para->buffering_check_point,(double)p_para->buffering_force_delay_s);
        if (download_speed >= ds_thres &&!check_time_interrupt(&p_para->buffering_check_point, (int)(p_para->buffering_force_delay_s * 1000))) {
            //delay buffering
            return 0;
        }
        p_para->buffering_force_delay_s = 0;
        p_para->buffering_check_point = 0;
    }
	/*for mobile soft detector test*/
	if ((p_para->first_pcr == 0) && (p_para->vstream_info.has_video)){
		if (am_getconfig("sys.proj.type", proj_type, NULL) > 0) {
			log_print("sys.proj.type:%s\n", proj_type);
			if ((strncmp(proj_type, "mobile", 6) == 0)) {
				log_print("mobile not get first pcr delay buffering\n");
				return 0;
			}
		}
	}
    if((p_para->vstream_info.video_width * p_para->vstream_info.video_height) <= 730 *580){
        float min = 0.04;
        if (am_getconfig_float("media.amplayer.minlelevel.576p", &min) >= 0) {
            p_para->buffering_threshhold_min = min;
            log_debug("576p set buffering_threshhold_min %f\n",min);
        }
    }

    if(p_para->vstream_info.video_width > 1920 && p_para->vstream_info.video_height > 1200){
        float middlelevel = 0.04;
        if (am_getconfig_float("media.amplayer.middlelevel.4k", &middlelevel) >= 0) {
            p_para->buffering_threshhold_middle = middlelevel;
            log_debug("4k set buffering_threshhold_middle %f\n",middlelevel);
        }
    }
    // discontineu bypass code
    unsigned long audio_pts_discontinue = 0;
    unsigned long video_pts_discontinue = 0;
    unsigned long audio_pts_discontinue_diff = 0;
    unsigned long video_pts_discontinue_diff = 0;
    unsigned long discontinue_vpts = 0;
    int discontinue_flag = 0;
    codec_para_t *codec = NULL;

    if (p_para->vstream_info.has_video) {
        if (p_para->vcodec) {
            codec = p_para->vcodec;
        } else if (p_para->codec) {
            codec = p_para->codec;
        }
    }
    if (codec) {
        audio_pts_discontinue = codec_get_sync_audio_discont(codec);
        video_pts_discontinue = codec_get_sync_video_discont(codec);
        audio_pts_discontinue_diff = codec_get_sync_audio_discont_diff(codec);
        video_pts_discontinue_diff = codec_get_sync_video_discont_diff(codec);
        discontinue_vpts = (unsigned long)(p_para->buffering_enter_time_s * 1000 * 90);
#define PTS_DISCONTINUE_VALID_SEC (1 * 90 * 1000)
        if ((audio_pts_discontinue > 0 && audio_pts_discontinue_diff > discontinue_vpts && audio_pts_discontinue_diff < PTS_DISCONTINUE_VALID_SEC) ||
            (video_pts_discontinue > 0 && video_pts_discontinue_diff > discontinue_vpts && video_pts_discontinue_diff < PTS_DISCONTINUE_VALID_SEC)) {
            discontinue_flag = 1;
        }
    }

	if (p_para->buffering_enable && status != PLAYER_PAUSE) {
		if (p_para->playctrl_info.cache_enable == 1 && p_para->playctrl_info.cache_buffering == 1) {
			if (status == PLAYER_RUNNING || status == PLAYER_BUFFERING) {
				cache_ms = avpkt_cache_getcache_time(p_para, p_para->vstream_info.video_index);
			}

			if ((status == PLAYER_RUNNING)
				&& (cache_ms >= 0 && cache_ms < (p_para->buffering_enter_time_s*1000/p_para->div_buf_time))
				&& !p_para->playctrl_info.read_end_flag) {
				codec_pause(p_para->codec);
				set_player_state(p_para, PLAYER_BUFFERING);
				update_player_states(p_para, 1);
				log_print("enter buffering, cache_ms:%lldms, enter_time_s:%fs, div:%d\n",
					cache_ms, p_para->buffering_enter_time_s, p_para->div_buf_time);

				if (p_para->div_buf_time > 1) {
					p_para->div_buf_time--;
				}
				p_para->buffering_enter_time = (int64_t)av_gettime() / 1000;
	
				// Handle Network Down
				if(check_net_phy_conn_status() == 0)
					p_para->force_enter_buffering = 1;

                if (p_para->media_info.report_para.enable == 1) {
                    p_para->media_info.report_para.vdec_underflow++;
                    p_para->media_info.report_para.adec_underflow++;
                    p_para->media_info.report_para.carton_times++;
                }
			} else if ((status == PLAYER_BUFFERING)
				&& ((cache_ms > p_para->buffering_exit_time_s*1000/((p_para->div_buf_time>2)?(p_para->div_buf_time>>1):1))
					|| (p_para->force_enter_buffering == 1 && (cache_ms > force_buf_thres))
					|| buffering_timeout
					|| p_para->playctrl_info.read_end_flag)) {
				codec_resume(p_para->codec);
				set_player_state(p_para, PLAYER_BUFFER_OK);
				update_player_states(p_para, 1);
				log_print("leave buffering, cache_ms=%lldmS, force_buffering:%d, exit_time_s:%fs, div:%d, end_flag:%d\n",
					cache_ms, p_para->force_enter_buffering, p_para->buffering_exit_time_s,
					p_para->div_buf_time, p_para->playctrl_info.read_end_flag);
	
				set_player_state(p_para, PLAYER_RUNNING);
				update_player_states(p_para, 1);
				if (p_para->div_buf_time > 1) {
					p_para->div_buf_time = 1;
				}

                if (p_para->media_info.report_para.enable == 1) {
                    p_para->media_info.report_para.carton_time +=
                    (int)((int64_t)(av_gettime()/1000) - p_para->buffering_enter_time);
                }

				p_para->buffering_enter_time = 0;
				if (p_para->force_enter_buffering == 1) {
					p_para->force_enter_buffering = 0;
					if (p_para->codec->has_audio == 1) {
						log_print("[%s:%d]unmute audio after codec_resume in condition force entering buffering", __FUNCTION__, __LINE__);
						codec_set_mute(p_para->codec, 0);
					}
				}
			}
		} else {
			if (
					(get_player_state(p_para) == PLAYER_RUNNING) &&
					( (!discontinue_flag && (avdelayms >= 0 && avdelayms < (p_para->buffering_enter_time_s*1000/p_para->div_buf_time)))  ||
					((alevel < p_para->buffering_threshhold_min/10) && (vlevel < p_para->buffering_threshhold_min/10)) )&&
					(minlevel < p_para->buffering_threshhold_min)&&
					(maxlevel < p_para->buffering_threshhold_max) &&
					(FLOAT_ABS(maxlevel - minlevel) <= errorbufferringlevel) &&
					!p_para->playctrl_info.read_end_flag
				) {
				codec_pause(p_para->codec);
				set_player_state(p_para, PLAYER_BUFFERING);
				update_player_states(p_para, 1);
				log_print("enter buffering!!!,avdelayms=%d mS,adelayms %d mS,vdelayms %d mS, discontinue_flag:%d, \n", avdelayms, adelayms, vdelayms, discontinue_flag);
				log_print("enter buffering!!!,minlevel(%f,%f), maxlevel(%f,%f), div:%d\n", minlevel, p_para->buffering_threshhold_min, maxlevel, p_para->buffering_threshhold_max, p_para->div_buf_time);

				p_para->state.enter_buffering_time = av_gettime() / 1000;
				log_print("buffering enter time = %lld\n", p_para->state.enter_buffering_time);
	
				if (p_para->div_buf_time > 1) {
					p_para->div_buf_time--;
				}
				p_para->buffering_enter_time = (int64_t)av_gettime() / 1000;
	
				// Handle Network Down
				if(check_net_phy_conn_status() == 0)
					p_para->force_enter_buffering = 1;
			} else if ((get_player_state(p_para) == PLAYER_BUFFERING) &&
					   ((!discontinue_flag && (avdelayms > p_para->buffering_exit_time_s*1000/((p_para->div_buf_time>2)?(p_para->div_buf_time>>1):1)) && minlevel > p_para->buffering_threshhold_middle*2/3 )  ||
						(minlevel > p_para->buffering_threshhold_middle)  ||
						(p_para->force_enter_buffering == 1 && (avdelayms > force_buf_thres || minlevel > force_exit_buffering_level))	||
						(maxlevel > p_para->buffering_threshhold_max) ||
						buffering_timeout ||
						p_para->playctrl_info.read_end_flag)) {
				codec_resume(p_para->codec);
				p_para->state.buffer_ok_time = av_gettime() / 1000;
				log_print("buffering ok time = %lld\n", p_para->state.buffer_ok_time);
				set_player_state(p_para, PLAYER_BUFFER_OK);
				update_player_states(p_para, 1);
				log_print("leave buffering!!!,avdelayms=%d mS,adelayms %d mS,vdelayms %d mS discontinue_flag:%d, p_para->force_enter_buffering:%d\n", avdelayms, adelayms, vdelayms, discontinue_flag, p_para->force_enter_buffering);
				log_print("leave buffering!!!,minlevel=(%f %f), maxlevel (%f %f), force_buf_thres:%dms, force_exit_buffering_level:%f div:%d \n", minlevel, p_para->buffering_threshhold_middle, maxlevel, p_para->buffering_threshhold_max, force_buf_thres, force_exit_buffering_level, p_para->div_buf_time);
	
				//FFT
				//amthreadpool_thread_usleep(1000);
				log_print("leave buffering!!!,avdelayms=%d mS,adelayms %d mS,vdelayms %d mS\n", avdelayms, adelayms, vdelayms);
				log_print("leave buffering!!!,p_para->buffering_exit_time_s=%f	,p_para->div_buf_time=%d \n", p_para->buffering_exit_time_s, p_para->div_buf_time);
				log_print("leave buffering!!!,minlevel=%f ,p_para->buffering_threshhold_middle=%f ,p_para->force_enter_buffering %d \n", minlevel, p_para->buffering_threshhold_middle, p_para->force_enter_buffering);
				log_print("leave buffering!!!,p_para->buffering_threshhold_max=%f, p_para->playctrl_info.read_end_flag %d \n", p_para->buffering_threshhold_max, p_para->playctrl_info.read_end_flag);
	
				set_player_state(p_para, PLAYER_RUNNING);
				update_player_states(p_para, 1);
				if (p_para->div_buf_time > 1) {
					p_para->div_buf_time = 1;
				}
				p_para->buffering_enter_time = 0;
				if (p_para->force_enter_buffering == 1) {
					p_para->force_enter_buffering = 0;
					if (p_para->codec->has_audio == 1) {
						log_print("[%s:%d]unmute audio after codec_resume in condition force entering buffering", __FUNCTION__, __LINE__);
						codec_set_mute(p_para->codec, 0);
					}
				}
			}

		}
	}
    return 0;
}
static void update_decbuf_states(play_para_t *p_para, struct buf_status *vbuf, struct buf_status *abuf)
{
    if (p_para->astream_info.has_audio) {
        if (p_para->abuffer.buffer_size == 0) {
            p_para->abuffer.buffer_size = abuf->size;
        }
        p_para->abuffer.data_level = abuf->data_len;
        if (abuf->read_pointer != p_para->abuffer.buffer_rp) {
            p_para->abuffer.rp_is_changed = 1;
            p_para->abuffer.buffer_rp = abuf->read_pointer;
        } else {
            p_para->abuffer.rp_is_changed = 0;
        }
    }
    if (p_para->vstream_info.has_video) {
        if (p_para->vbuffer.buffer_size == 0) {
            p_para->vbuffer.buffer_size = vbuf->size;
        }
        p_para->vbuffer.data_level = vbuf->data_len;
        if (vbuf->read_pointer != p_para->vbuffer.buffer_rp) {
            p_para->vbuffer.rp_is_changed = 1;
            p_para->vbuffer.buffer_rp = vbuf->read_pointer;
        } else {
            p_para->vbuffer.rp_is_changed = 0;
        }
    }
}

static void update_av_sync_for_audio(play_para_t *p_para)
{
    if (!p_para->abuffer.rp_is_changed && !check_time_interrupt(&p_para->playctrl_info.avsync_check_old_time, 60)) {
        return ;    //no changed and time is no changed.do count---,3S no changesd..60*50
    }
    if (p_para->playctrl_info.video_low_buffer == 1 ||
        p_para->playctrl_info.audio_low_buffer == 1) {
        return;
    }

    if (p_para->playctrl_info.audio_ready &&
        p_para->vstream_info.has_video &&
        p_para->astream_info.has_audio &&
        get_player_state(p_para) == PLAYER_RUNNING) {
        if (!p_para->abuffer.rp_is_changed) {
            p_para->abuffer.check_rp_change_cnt --;
            if (!p_para->playctrl_info.pts_valid && p_para->abuffer.data_level == 0) {
                p_para->abuffer.buf_empty ++;
            }
            //log_print("[%s:%d]arp not change, cnt=%d! empty=%d\n", __FUNCTION__, __LINE__, p_para->abuffer.check_rp_change_cnt, p_para->abuffer.buf_empty);
        } else {
            p_para->abuffer.check_rp_change_cnt = CHECK_AUDIO_HALT_CNT;
            if (!p_para->playctrl_info.avsync_enable) {
                set_tsync_enable(1);
                p_para->playctrl_info.avsync_enable = 1;
                log_print("[%s:%d]arp alived, enable sync!\n", __FUNCTION__, __LINE__);
            }
        }
        if (p_para->playctrl_info.avsync_enable &&
            p_para->abuffer.check_rp_change_cnt <= 0) {
            //FFT:  do not disable sync for audio low level case
            //set_tsync_enable(0);
            p_para->playctrl_info.avsync_enable = 0;
            if (p_para->abuffer.buf_empty == CHECK_AUDIO_HALT_CNT && p_para->playctrl_info.pts_valid == 0) {
                p_para->playctrl_info.pts_valid = 1;
                p_para->abuffer.buf_empty = 0;
            }
            log_print("[%s:%d]arp not alived, disable sync\n", __FUNCTION__, __LINE__);
            p_para->abuffer.check_rp_change_cnt = CHECK_AUDIO_HALT_CNT;
        }
    }
}

static int check_avdiff_time(play_para_t *p_para)
{
    int ret = 0;
    int vpts = 0, apts = 0;
    int diff_threshold = 90000 * 8;
    AVFormatContext *pCtx = p_para->pFormatCtx;
    int64_t time_point;

    apts = get_pts_audio(p_para);
    vpts = get_pts_video(p_para);

    if ((((apts > vpts) && (apts - vpts > diff_threshold))
         || ((apts < vpts) && (vpts - apts > diff_threshold)))
        && (0 == p_para->playctrl_info.avdiff_next_reset_timepoint)
        && (get_player_state(p_para) == PLAYER_RUNNING)) {
        if (0 == p_para->playctrl_info.avdiff_check_old_time) {
            log_print("avsync diff started\n");
            check_time_interrupt(&p_para->playctrl_info.avdiff_check_old_time, 0);
            return 0;
        } else {
            if (check_time_interrupt(&p_para->playctrl_info.avdiff_check_old_time, 60 * 1000)) {
                if (pCtx->pb && pCtx->pb->opaque) {
                    time_point = url_fseekitemtime(pCtx->pb, p_para->state.current_time);
                    if (-1 == time_point) {
                        log_print("[%s:%d]avsync diff is -1\n", __FUNCTION__, __LINE__);
                        if (url_support_time_seek(pCtx->pb)) {
                            return 0;
                        } else {
                            return 1;
                        }
                    } else if (p_para->state.full_time == time_point) {
                        log_print("[%s:%d]avsync diff is the full_time\n", __FUNCTION__, __LINE__);
                        p_para->playctrl_info.avdiff_check_old_time = 0;
                        return 0;
                    } else if (p_para->state.current_time == time_point) {
                        log_print("[%s:%d]avsync diff is the current_time\n", __FUNCTION__, __LINE__);
                        return 1;
                    } else if (p_para->state.current_time > time_point) {
                        log_print("[%s:%d]avsync diff some error happened\n", __FUNCTION__, __LINE__);
                        return 1;
                    } else {
                        p_para->playctrl_info.avdiff_next_reset_timepoint = time_point;
                        return 0;
                    }
                } else {
                    log_print("[%s:%d]avsync diff no opaque\n", __FUNCTION__, __LINE__);
                    return 1;
                }
            } else {
                return 0;
            }
        }
    } else if (p_para->playctrl_info.avdiff_next_reset_timepoint) {
        if (p_para->state.current_time >= p_para->playctrl_info.avdiff_next_reset_timepoint) {
            p_para->playctrl_info.avdiff_next_reset_timepoint = 0;
            log_print("[%s:%d]avsync diff wait time out\n", __FUNCTION__, __LINE__);
            return 1;
        } else {
            return 0;
        }
    } else {
        p_para->playctrl_info.avdiff_check_old_time = 0;
        return 0;
    }
}

static int64_t hls_get_report_para_info(play_para_t *p_para)
{
    int64_t value = 0;
    hls_para_t  hlspara;
    log_print("--hls_get_report_para_info\n");

    ffmpeg_get_hls_info(p_para, 1, &hlspara);

    memcpy(&p_para->media_info.report_para.hls_para, &hlspara, sizeof(hls_para_t));
    log_print("%s, m3u8 info: serv_addr=%s, avg_time=%d, max_time=%d, bitrate=%d\n", __FUNCTION__, p_para->media_info.report_para.hls_para.m3u8_server,
        p_para->media_info.report_para.hls_para.m3u8_get_delay_avg_time, p_para->media_info.report_para.hls_para.m3u8_get_delay_max_time,
        p_para->media_info.report_para.hls_para.bitrate);
    log_print("%s, ts info: serv_addr=%s, avg_time=%d, max_time=%d, get_times:%d, suc_times=%d\n", __FUNCTION__, p_para->media_info.report_para.hls_para.ts_server,
        p_para->media_info.report_para.hls_para.ts_get_delay_avg_time, p_para->media_info.report_para.hls_para.ts_get_delay_max_time,
        p_para->media_info.report_para.hls_para.ts_get_times, p_para->media_info.report_para.hls_para.ts_get_suc_times);

    return value;
}

static char* player_vformat2str(vformat_t value)
{
    switch (value) {
    case VFORMAT_MPEG12:
        return "MPEG12";

    case VFORMAT_MPEG4:
        return "MPEG4";

    case VFORMAT_H264:
        return "H264";

    case VFORMAT_HEVC:
        return "HEVC";

    case VFORMAT_MJPEG:
        return "MJPEG";

    case VFORMAT_REAL:
        return "REAL";

    case VFORMAT_JPEG:
        return "JPEG";

    case VFORMAT_VC1:
        return "VC1";

    case VFORMAT_AVS:
        return "AVS";

    case VFORMAT_SW:
        return "SW";

    case VFORMAT_H264MVC:
        return "H264MVC";

    case VFORMAT_H264_4K2K:
        return "H264_4K2K";

    default:
        return "NOT_SUPPORT";
    }
    return NULL;
}

static int update_report_para(play_para_t *p_para,
								struct buf_status *vbuf,
								struct buf_status *abuf,
								struct av_param_info_t *av_param_info) {
	char* pcTemp = NULL;
	int nLen = -1;
	int i = 0;
	int kick = 0;

	if (p_para->vstream_info.video_format > VFORMAT_UNKNOWN
		&& p_para->vstream_info.video_format < VFORMAT_MAX) {
		pcTemp = player_vformat2str(p_para->vstream_info.video_format);
		if (pcTemp != NULL) {
			nLen = strlen(pcTemp);
			memcpy(&p_para->media_info.report_para.vformat, pcTemp, nLen);
			p_para->media_info.report_para.vformat[nLen] = '\0';
		}
	}

	if (p_para->vstream_info.video_width == 640 && p_para->vstream_info.video_height == 480) {
		p_para->media_info.report_para.video_aspect = 0;
	} else if (p_para->vstream_info.video_width == 720 && p_para->vstream_info.video_height == 576) {
		p_para->media_info.report_para.video_aspect = 1;
	} else if (p_para->vstream_info.video_width == 1280 && p_para->vstream_info.video_height == 720) {
		p_para->media_info.report_para.video_aspect = 2;
	} else if (p_para->vstream_info.video_width == 1920 && p_para->vstream_info.video_height == 1080) {
		p_para->media_info.report_para.video_aspect = 3;
	} else if (p_para->vstream_info.video_width == 3840 && p_para->vstream_info.video_height == 2160) {
		p_para->media_info.report_para.video_aspect = 4;
	} else {
		p_para->media_info.report_para.video_aspect = 5;
	}

	int rVideoHeight = av_param_info->av_info.height;
	int rVideoWidth = av_param_info->av_info.width;
	float videoWH = 0;
	if(rVideoWidth > 0 && rVideoHeight > 0)
        videoWH =(float)rVideoWidth / (float)rVideoHeight;

        if (videoWH < 1.34) {
            p_para->media_info.report_para.video_ratio = 0;
        } else if(videoWH > 1.7) {
            p_para->media_info.report_para.video_ratio = 1;
        }
	if(av_param_info->av_info.frame_format == FRAME_FORMAT_PROGRESS) {
		p_para->media_info.report_para.progress = 1;
	} else if (av_param_info->av_info.frame_format == FRAME_FORMAT_INTERLACE) {
		p_para->media_info.report_para.progress = 0;
	} else {
		p_para->media_info.report_para.progress = -1;
	}

	p_para->media_info.report_para.vbuf_size = vbuf->size;//amstream buf
	p_para->media_info.report_para.vbuf_used_size = vbuf->data_len;
	p_para->media_info.report_para.vdec_error = av_param_info->av_info.dec_error_count;
	p_para->media_info.report_para.vdec_drop = av_param_info->av_info.dec_drop_frame_count;
	p_para->media_info.report_para.vdec_pts_error = av_param_info->av_info.vpts_err;

	if (p_para->astream_info.audio_format > AFORMAT_UNKNOWN
		&& p_para->astream_info.audio_format < AFORMAT_MAX) {
		pcTemp = player_aformat2str(p_para->astream_info.audio_format);
		if (pcTemp != NULL) {
			nLen = strlen(pcTemp);
			memcpy(&p_para->media_info.report_para.aformat, pcTemp, nLen);
			p_para->media_info.report_para.aformat[nLen] = '\0';
		}
	}

	if (p_para->sstream_info.has_sub == 1
		&& p_para->sstream_info.sub_index != -1
		&& p_para->media_info.sub_info != NULL) {
		for (i = 0; i < p_para->media_info.stream_info.total_sub_num; i++) {
			if (p_para->media_info.sub_info[i]->index == p_para->sstream_info.sub_index) {
				log_print("cur sub index:%d\n", p_para->sstream_info.sub_index);
				kick = 1;
				break;
			}
		}

		if (kick == 1) {
			pcTemp = p_para->media_info.sub_info[i]->sub_language;
			nLen = strlen(pcTemp);
			memcpy(&p_para->media_info.report_para.audio_sub_language, pcTemp, nLen);
			p_para->media_info.report_para.audio_sub_language[nLen] = '\0';
		}
	}

	if (p_para->astream_info.has_audio == 1
		&& p_para->astream_info.audio_index != -1
		&& p_para->media_info.audio_info != NULL) {
		codec_get_audio_basic_info(p_para->codec);
		kick = 0;
		for (i = 0; i < p_para->media_info.stream_info.total_audio_num; i++) {
			if (p_para->media_info.audio_info[i]->index == p_para->astream_info.audio_index) {
				log_print("cur audio index:%d\n", p_para->astream_info.audio_index);
				kick = 1;
				break;
			}
		}

		if (kick == 1) {
			p_para->media_info.report_para.audio_bitrate =
				p_para->codec->audio_info.bitrate;
			p_para->media_info.audio_info[i]->bit_rate =
				p_para->codec->audio_info.bitrate;
			p_para->media_info.report_para.audio_channels =
				p_para->codec->audio_info.channels;
			p_para->media_info.audio_info[i]->channel =
				p_para->codec->audio_info.channels;
			p_para->media_info.report_para.audio_sr =
				p_para->codec->audio_info.sample_rate;
			p_para->media_info.audio_info[i]->sample_rate =
				p_para->codec->audio_info.sample_rate;
		} else {
			p_para->media_info.report_para.audio_bitrate =
				p_para->codec->audio_info.bitrate;
			p_para->media_info.report_para.audio_channels =
				p_para->codec->audio_info.channels;
			p_para->media_info.report_para.audio_sr =
				p_para->codec->audio_info.sample_rate;
		}

		p_para->media_info.report_para.adec_error =
			p_para->codec->audio_info.error_num;
		p_para->media_info.report_para.adec_drop =
			p_para->codec->audio_info.error_num;
	}

	p_para->media_info.report_para.abuf_size = abuf->size;
	p_para->media_info.report_para.abuf_used_size = abuf->data_len;
	p_para->media_info.report_para.adec_pts_error = av_param_info->av_info.apts_err;

	p_para->media_info.report_para.ts_cc_discont = 0;
	p_para->media_info.report_para.ts_sync_lost_num = 0;

	int64_t apts = av_param_info->av_info.apts;
	int64_t vpts = av_param_info->av_info.vpts;

	p_para->media_info.report_para.avpts_diff = abs(((int)(vpts - apts)));

	URLContext *h = NULL;
	if (p_para->pFormatCtx != NULL
		&& p_para->pFormatCtx->pb != NULL
		&& p_para->pFormatCtx->pb->opaque != NULL) {
		h = (URLContext *)p_para->pFormatCtx->pb->opaque;
		if (h != NULL && h->prot != NULL) {
			if (strcmp(h->prot->name, "rtp") == 0
				|| strcmp(h->prot->name, "rtpfec") == 0) {
				memcpy(&p_para->media_info.report_para.transport_protocol, "rtp", 3);
			} else if (strcmp(h->prot->name, "udp") == 0) {
				memcpy(&p_para->media_info.report_para.transport_protocol, "udp", 3);
			} else {
				memcpy(&p_para->media_info.report_para.transport_protocol, "tcp", 3);
			}
		} else {
			memcpy(&p_para->media_info.report_para.transport_protocol, "nul", 4);//default tcp
		}
	} else if (strncmp(p_para->pFormatCtx->iformat->name, "mhls", 4) == 0) {
		memcpy(&p_para->media_info.report_para.transport_protocol, "tcp", 3);
	} else {
		memcpy(&p_para->media_info.report_para.transport_protocol, "null", 4);//default tcp
	}

	nLen = strlen(p_para->media_info.report_para.transport_protocol);
	p_para->media_info.report_para.transport_protocol[nLen] = '\0';

	if (p_para->pFormatCtx != NULL
		&& p_para->pFormatCtx->iformat != NULL
		&& strstr(p_para->pFormatCtx->iformat->name, "mpegts") != NULL) {
		p_para->media_info.report_para.ts_cc_discont = p_para->pFormatCtx->ts_cc_discont;
		p_para->media_info.report_para.ts_sync_lost_num = p_para->pFormatCtx->ts_sync_lost_num;
	}

	if (p_para->pFormatCtx != NULL
		&& (strstr(p_para->pFormatCtx->filename, "vhls") != NULL
			|| (p_para->pFormatCtx->pb != NULL
				&& p_para->pFormatCtx->pb->is_mhls == 1))) {
		hls_get_report_para_info(p_para);
	}

	return 0;
}

/*
call this function after PLAYER_INITOK
*/
static int player_report_para(play_para_t *p_para,
                                 struct buf_status *vbuf,
                                 struct buf_status *abuf,
                                 struct vdec_status *vdec)
{
    player_status status = get_player_state(p_para);
    int checktime = 0;
    BlurInfo  blur_info;
    MEMSET(&blur_info, 0, sizeof(BlurInfo));
    UnloadInfo unload_info;
    memset(&unload_info, 0, sizeof(UnloadInfo));

    if(status <= PLAYER_INITOK)
        return 0;
    codec_para_t    *vcodec = NULL;
    struct av_param_info_t av_param_info;
     int is_avs2 = 0;
    if ((p_para->stream_type == STREAM_ES)
        || (p_para->stream_type == STREAM_AUDIO)
        || (p_para->stream_type == STREAM_VIDEO)) {
        if (p_para->vstream_info.has_video && p_para->vcodec) {
            vcodec = p_para->vcodec;
        }
    } else if (p_para->codec) {
        vcodec = p_para->codec;
    }
    if (vcodec && p_para->vstream_info.has_video) {
        codec_get_av_param_info(vcodec, &av_param_info);
        /*pts info*/
        memset(av_info, 0x0, sizeof(av_info));
        char* pbuf = &av_info;
        /*basic av info*/
        pbuf += sprintf(pbuf, "first_pic_coming:%d,",
            av_param_info.av_info.first_pic_coming);
        pbuf += sprintf(pbuf, "vformat_type:%s,",
            player_vformat2str(av_param_info.av_info.vformat_type));
        if (av_param_info.av_info.frame_format == 1) {
            pbuf += sprintf(pbuf, "frame_format:%s,",
                "progressive");
        } else if (av_param_info.av_info.frame_format == 2) {
            pbuf += sprintf(pbuf, "frame_format:%s,",
                "interlace");
        } else {
            pbuf += sprintf(pbuf, "frame_format:%s,",
                "FORMAT_UNKNOW");
        }
        pbuf += sprintf(pbuf, "width:%d,", av_param_info.av_info.width);
        pbuf += sprintf(pbuf, "height:%d,", av_param_info.av_info.height);
        pbuf += sprintf(pbuf, "fps:%d,", av_param_info.av_info.fps);
        pbuf += sprintf(pbuf, "current_fps:%d,",
            av_param_info.av_info.current_fps);
        pbuf += sprintf(pbuf, "first_vpts:%d,", av_param_info.av_info.first_vpts);
        pbuf += sprintf(pbuf, "vpts:0x%x,", av_param_info.av_info.vpts);
        pbuf += sprintf(pbuf, "vpts_err_num:%d,",
            av_param_info.av_info.vpts_err);
        pbuf += sprintf(pbuf, "toggle_frame_count:%d,",
            av_param_info.av_info.toggle_frame_count);
        pbuf += sprintf(pbuf, "dec_error_count:%d,",
            av_param_info.av_info.dec_error_count);
        pbuf += sprintf(pbuf, "dec_err_frame_count:%d,",
            av_param_info.av_info.dec_err_frame_count);
        pbuf += sprintf(pbuf, "dec_frame_count:%d,",
            av_param_info.av_info.dec_frame_count);
        pbuf += sprintf(pbuf, "dec_drop_frame_count:%d,",
            av_param_info.av_info.dec_drop_frame_count);
        pbuf += sprintf(pbuf, "ts_error_num:%d,",
            av_param_info.av_info.ts_error);
        pbuf += sprintf(pbuf, "tsync_mode:%d,", av_param_info.av_info.tsync_mode);
        pbuf += sprintf(pbuf, "apts:0x%x,", av_param_info.av_info.apts);
        pbuf += sprintf(pbuf, "apts_err_num:%d,",
            av_param_info.av_info.apts_err);
        pbuf ++;
        pbuf = '\n';
        if (amsysfs_set_sysfs_str(AV_INFO, av_info) == -1) {
            log_print("cannot open file %s,err: %s", AV_INFO, strerror(errno));
        }
    }
    if ((status == PLAYER_ERROR || status == PLAYER_EXIT || status == PLAYER_STOPED || status == PLAYER_PLAYEND)
        && p_para->pFormatCtx != NULL && p_para->pFormatCtx->pb != NULL && p_para->pFormatCtx->pb->opaque != NULL) {
        memset(&av_info, 0, sizeof(av_info));
        if (amsysfs_set_sysfs_str(AV_INFO, AV_INFO_STR) == -1) {
            log_print("cannot open file %s,err: %s", AV_INFO, strerror(errno));
        }
    }
    if (status != PLAYER_PAUSE && p_para->pFormatCtx != NULL
        && p_para->pFormatCtx->pb != NULL && p_para->pFormatCtx->pb->opaque != NULL) {
        URLContext *h = (URLContext *)p_para->pFormatCtx->pb->opaque;
        if (h != NULL && h->prot != NULL
            && (strcmp(h->prot->name, "rtp") == 0
                || strcmp(h->prot->name, "rtpfec") == 0 || strcmp(h->prot->name, "udp") == 0)) {
                struct codec_quality_info *pquality_info = &p_para->media_info.report_para.quality_info;
                /* +[SE] [BUG][IPTV-4185][yinli.xia] added: AVS2 needn't report event */
                is_avs2 = ((p_para->vstream_info.video_format == VFORMAT_AVS2) ? 1:0);
                if (codec_get_upload_cmcc(&av_param_info.av_info, pquality_info,vcodec,&checktime)) {
                    if (pquality_info->unload_flag) {
                        p_para->state.unload_start_time = av_gettime();
                        send_event(p_para, PLAYER_EVENTS_UNLOAD_START, 0, 0);
                    } else {
                        p_para->state.unload_end_time = av_gettime();
                        unload_info.checktime = checktime;
                        send_event(p_para, PLAYER_EVENTS_UNLOAD_END, &p_para->state, &unload_info);
                        log_print("Unload end: checktime:%d\n", checktime);
                    }
                }
                if (p_para->error_recovery_mode == 1 && codec_get_blurred_screen_cmcc(&av_param_info.av_info, pquality_info,vcodec, &checktime)) {
                    if (pquality_info->blurred_flag) {
                        send_event(p_para, PLAYER_EVENTS_BLURREDSCREEN_START, (unsigned long)(pquality_info->cur_time_ms/1000), p_para->state.current_time);
                        send_event(p_para, PLAYER_EVENTS_ERROR, 30002, 0);
                    } else {
                        blur_info.ratio = pquality_info->ratio;
                        blur_info.checktime = checktime;
                        log_print("blur end ratio:%d, checktime=%d\n",
                            blur_info.ratio, blur_info.checktime);
                        send_event(p_para, PLAYER_EVENTS_BLURREDSCREEN_END, (unsigned long)(pquality_info->cur_time_ms/1000), &blur_info);
                    }
                }
        }
    }

    if (p_para->media_info.report_para.enable == 0)
        return 0;

    if (p_para->media_info.report_para.last_report_time == 0)
        p_para->media_info.report_para.last_report_time = av_gettime()/1000;

    if(check_time_interrupt(&p_para->media_info.report_para.last_report_time, p_para->media_info.report_para.report_period_ms)) {
        update_report_para(p_para, vbuf, abuf, &av_param_info);
        //send_event(p_para, PLAYER_EVENTS_PLAYER_PARAM_REPORT, &p_para->media_info, 0);
        send_event(p_para, PLAYER_EVENTS_PLAYER_PARAM_REPORT, 0, 0);
		p_para->media_info.report_para.last_report_time = av_gettime()/1000;
    }

    return 0;
}

int update_playing_info(play_para_t *p_para)
{
    struct buf_status vbuf, abuf,sbuf;
    struct vdec_status vdec;
    struct adec_status adec;
    player_status sta;
    unsigned long delay_ms;
    int ret;

    MEMSET(&vbuf, 0, sizeof(struct buf_status));
    MEMSET(&abuf, 0, sizeof(struct buf_status));
    MEMSET(&sbuf, 0, sizeof(struct buf_status));

    sta = get_player_state(p_para);
    if (sta > PLAYER_INITOK) {
       
        // switch live mode to timeshift when pause exceed 30s . HLS only
        if (sta == PLAYER_PAUSE) {
            if(p_para->playctrl_info.pause_start_time <= 0)
                p_para->playctrl_info.pause_start_time = (int64_t)player_get_systemtime_ms();
            int64_t now_ms = (int64_t)player_get_systemtime_ms();
            int livemode_switch_ms = am_getconfig_int_def("hls.livemode.switch_ms", 30*1000);
            if((now_ms - p_para->playctrl_info.pause_start_time) >= livemode_switch_ms) {
                ffmpeg_seturl_hls_setopt(p_para, 1, 1);
            }
        } else {
            p_para->playctrl_info.pause_start_time = 0;
        }
        
        if (sta != PLAYER_SEARCHING) {
            ret = update_codec_info(p_para, &vbuf, &abuf, &sbuf, &vdec, &adec);
            if (ret != 0) {
                return PLAYER_FAILED;
            }
        }

        player_report_para(p_para, &vbuf, &abuf, &vdec);
        update_player_cachetime(p_para, &vbuf, &abuf);
        update_dec_info(p_para, &vdec, &adec, &vbuf, &abuf);
        update_decbuf_states(p_para, &vbuf, &abuf);
        update_buffering_states(p_para, &vbuf, &abuf, &sbuf);
        player_check_program_change(p_para);

        update_av_sync_for_audio(p_para);

        if (p_para->first_pcr == 0) {
            //modified by yzs begin
            //int pcr = get_sysfs_int("/sys/class/tsync/pts_pcrscr");
            //if (pcr > 0) {
            int first_pic = get_sysfs_int("/sys/module/amvideo/parameters/first_frame_toggled");
            if (first_pic == 1) {
            //modified by yzs end             
                int pcr = get_sysfs_int("/sys/class/tsync/firstvpts");
                send_event(p_para, PLAYER_EVENTS_GET_FIRST_PCR, pcr, 0);
                p_para->first_pcr = pcr;
                log_print("send_event: get_first_pcr 0x%x\n", pcr);
            }
        }
#if 1
        /* set pcm resampling for wfd */
        if (am_getconfig_bool("media.libplayer.wfd")) {
            codec_para_t *avcodec = NULL;
            int resample_enable;
            int pcm_len = 0, pcm_ms = 0;
            unsigned int last_checkout_apts = 0, apts = 0, last_checkin_apts = 0, dsp_apts = 0;
            static unsigned l_last_checkout_apts = 0, l_last_checkin_apts = 0, l_dsp_apts = 0, l_delay_ms = 0;
            static int skipped = 0;
            if (p_para->codec) {
                avcodec = p_para->codec;
            } else if (p_para->acodec) {
                avcodec = p_para->acodec;
            }
            if (0) { //avcodec) {
                codec_get_audio_cur_delay_ms(avcodec, &delay_ms);
                codec_get_last_checkout_apts(avcodec, &last_checkout_apts);
                last_checkout_apts /= 90;
                codec_get_last_checkin_apts(avcodec, &last_checkin_apts);
                last_checkin_apts /= 90;
                pcm_len = codec_get_pcm_level(avcodec);
                apts = codec_get_apts(avcodec);
                pcm_ms = last_checkout_apts - apts; // total PCM not playbacked
                resample_enable = codec_get_audio_resample_ena(avcodec);
                dsp_apts = codec_get_dsp_apts(avcodec) / 90;
#if 0
                if (l_delay_ms != delay_ms || l_last_checkout_apts != last_checkout_apts || l_last_checkin_apts != last_checkin_apts || l_dsp_apts != dsp_apts) {
                    log_print("delay_ms = %d, pts %d->%d, dsp pts->%d", delay_ms, last_checkin_apts, last_checkout_apts, dsp_apts);
                    l_delay_ms = delay_ms;
                    l_last_checkin_apts = last_checkin_apts;
                    l_last_checkout_apts = last_checkout_apts;
                    l_dsp_apts = dsp_apts;
                }
#endif
                if (delay_ms > 800) {
                    codec_set_skip_bytes(avcodec, 0);
                    skipped = 1;
                } else if (skipped) {
                    codec_set_skip_bytes(avcodec, 0x7fffffff);
                    skipped = 0;
                }
#if 0
                if (delay_ms > 300) { // total delayed ms
                    if (!resample_enable && (pcm_ms > 200)) {// 200ms
                        codec_set_audio_resample_type(avcodec, 1);  // down resample
                        codec_set_audio_resample_ena(avcodec, 1);  // enable resample
                        log_print("start resample : %d:%d\n", delay_ms, pcm_ms);
                    } else if (resample_enable && pcm_ms < 200) {
                        codec_set_audio_resample_ena(avcodec, 0);
                        log_print("stop resample[1] : %d\n", pcm_ms);
                    } else if (resample_enable) {
                        log_print("keep resample: %d\n", pcm_ms);
                    }
                    /*
                      if (delay_ms > 800) {
                        codec_set_skip_bytes(avcodec, 0);//target_level);
                        log_print("skip bytes start : %d\n", delay_ms);
                      }else{
                        codec_set_skip_bytes(avcodec, 0x7fffffff);
                        log_print("stop skipe bytes: %d\n", delay_ms);
                      }
                      */
                } else {
                    //codec_set_skip_bytes(avcodec, 0x7fffffff);
                    codec_set_audio_resample_ena(avcodec, 0);
                    log_print("stop resample and bytes skip: %d\n", delay_ms);
                }
#endif
            }
        }
#endif

        if (sta > PLAYER_INITOK && sta < PLAYER_ERROR) {
            if (p_para->playctrl_info.audio_ready != 1) {
                p_para->playctrl_info.audio_ready  = codec_audio_isready(p_para->codec);
                if (p_para->playctrl_info.audio_ready) {
                    log_print("[%s:%d]audio_ready=%d\n", __FUNCTION__, __LINE__, p_para->playctrl_info.audio_ready);
                }
            }
            if (p_para->astream_info.has_audio  && (p_para->playctrl_info.audio_ready != 1)) {
                if (check_audiodsp_fatal_err() == AUDIO_DSP_INIT_ERROR) {
                    p_para->playctrl_info.audio_ready = 1;
                    log_print("[%s]dsp init failed, set audio_ready for time update\n", __FUNCTION__);
                } else if (0 == p_para->abuffer.data_level) {
                    if (check_audio_ready_time(&p_para->playctrl_info.check_audio_ready_ms)) {
                        p_para->playctrl_info.audio_ready = 1;
                        log_print("[%s]no audio data, set audio_ready for time update\n", __FUNCTION__);
                    }
                }
            }
        }

        if ((p_para->playctrl_info.audio_ready == 1 ||
             p_para->playctrl_info.fast_backward ||
             p_para->playctrl_info.fast_forward ||
             p_para->playctrl_info.hls_forward ||
             p_para->playctrl_info.hls_backward) &&
            !p_para->playctrl_info.end_flag) {
            update_current_time(p_para);
        }
        if (get_player_state(p_para) == PLAYER_PLAYEND && p_para->state.seek_point > p_para->state.full_time)
        {
            //log_print("## [%s:%d] use seek_point: seek_pos=%d, cur_time=%d, seek_delay=%d, \n", __FUNCTION__, __LINE__,p_para->state.seek_point,p_para->state.current_time,p_para->state.seek_delay);
            p_para->state.current_time = p_para->state.seek_point;
            log_print("[%s:%d],p_para->state.current_time:%d", __FUNCTION__,__LINE__,p_para->state.current_time);
            p_para->state.current_ms = p_para->state.current_time * 1000;
        }
        if (get_player_state(p_para) == PLAYER_PLAYEND && p_para->state.current_time == p_para->state.full_time) {
            p_para->state.current_ms = p_para->state.current_time * 1000;
        }
        p_para->state.pts_video = get_pts_video(p_para);
    }

    if (p_para->playctrl_info.read_end_flag && (get_player_state(p_para) != PLAYER_PAUSE)) {
        check_avbuf_end(p_para, &vbuf, &abuf);
		 if(p_para->playctrl_info.hls_forward == 0 && 
                p_para->playctrl_info.hls_backward == 0)
        	check_force_end(p_para, &vbuf, &abuf);
    }
    return PLAYER_SUCCESS;
}

int     update_player_cachetime(play_para_t *p_para,
                                    struct buf_status *vbuf,
                                    struct buf_status *abuf)
{
    callback_t *cb = &p_para->cachetime_cb;
    SoftProbeInfo probe_info;
    MEMSET(&probe_info, 0, sizeof(SoftProbeInfo));

    if(get_player_state(p_para) <= PLAYER_INITOK)
        return 0;
    if (cb->callback_old_time == 0)
        cb->callback_old_time = av_gettime()/1000;
    if(check_time_interrupt(&cb->callback_old_time, cb->update_interval)) {
        int adelayms = -1,vdelayms = -1,delayms = 0, delays = 0;
        int cached_bytes = 0;
        struct timeval  new_time;
        gettimeofday(&new_time, NULL);
        if(p_para && p_para->pFormatCtx && p_para->pFormatCtx->pb && p_para->pFormatCtx->pb->opaque != NULL) {
            URLContext *h = (URLContext *)p_para->pFormatCtx->pb->opaque;
            if (h != NULL && h->prot != NULL && (strcmp(h->prot->name, "rtpfec") == 0)) 
                avio_getinfo(p_para->pFormatCtx->pb, AVCMD_GET_FECRATIOINFO, 1, &probe_info);
        }


        if (p_para->playctrl_info.cache_enable == 1) {
            unsigned long delay_ms = (unsigned long)avpkt_cache_getcache_time_by_streamindex(p_para, p_para->vstream_info.video_index);
            log_print("**[%s:%d]pid:%d, cache_time=%ld, cur_time=%d", __FUNCTION__,__LINE__,p_para->player_id, delay_ms/1000, new_time.tv_sec);
            p_para->state.cache_time = delay_ms / 1000;
            send_event(p_para, PLAYER_EVENTS_PLAYER_CACHETIME, delay_ms/1000, &probe_info);
            return 1;
        }

        // get stream buffer delay first
        if(p_para->astream_info.has_audio && get_audio_codec(p_para)){
            codec_get_audio_cur_delay_ms(get_audio_codec(p_para),&adelayms);
            delayms = adelayms;
            log_print("[%s:%d]delayms:%d", __FUNCTION__,__LINE__,delayms);
        }else if(p_para->vstream_info.has_video && get_video_codec(p_para)) {
            codec_get_video_cur_delay_ms(get_video_codec(p_para),&vdelayms);
            delayms = vdelayms;
            log_print("[%s:%d]delayms:%d", __FUNCTION__,__LINE__,delayms);
        }
        if(p_para->astream_info.has_audio && get_audio_codec(p_para)) {
            if (abuf->size > 0) {
                cached_bytes += abuf->data_len;
            }
        }
        if(p_para->vstream_info.has_video && get_video_codec(p_para)) {
            if (vbuf->size > 0) {
                cached_bytes += vbuf->data_len;
            } 
        }

        // then get loopbuffer delay
        if (p_para->pFormatCtx != NULL && p_para->pFormatCtx->iformat != NULL && (p_para->pFormatCtx->iformat->flags & AVFMT_NOFILE) && p_para->pFormatCtx->pb != NULL && p_para->pFormatCtx->pb->opaque != NULL) {
            int forward_data = 0,back_data = 0;
            url_lp_getbuffering_size((URLContext *)p_para->pFormatCtx->pb->opaque, &forward_data, &back_data);
            if(p_para->pFormatCtx->bit_rate > 0)
            {
                delayms += ((int64_t)forward_data*1000*8)/p_para->pFormatCtx->bit_rate;
                log_print("[%s:%d]delayms:%d", __FUNCTION__,__LINE__,delayms);
            }
            cached_bytes += forward_data;
        }

        delays = (delayms/1000);
        log_print("[%s:%d],delays:%d,delayms:%d", __FUNCTION__,__LINE__,delays,delayms);
        if((p_para->pFormatCtx != NULL) && (p_para->pFormatCtx->pb != NULL) && av_is_segment_media(p_para->pFormatCtx))
	{
            if(p_para->thread_mgt.pthread_id == pthread_self()){
                int64_t BufTime = url_fbuffered_time(p_para->pFormatCtx->pb);
                int CurTime = p_para->state.current_time;
                int64_t Delta = p_para->state.delta_current_time;
                delays =BufTime   - (CurTime + Delta);
                log_print("[%s:%d],BufTime=%lld, CurTime%d,Delta:%lld,delays:%d\n",  __FUNCTION__,__LINE__,BufTime, CurTime,Delta,delays);
                if(p_para->hls_discontinue_point > 0) {
                    delays += p_para->hls_discontinue_point;
                    log_print("**[%s:%d]pid:%d cache time=%d p_para->hls_discontinue_point:%d",  __FUNCTION__,__LINE__,p_para->player_id, delays,p_para->hls_discontinue_point);
                }
            }
            if(!(p_para->pFormatCtx->pb->is_mhls)){
                int64_t bytes = 0;
                ffmpeg_geturl_netstream_info(p_para, 9, &bytes);
                cached_bytes +=bytes;
            }else{// mhls

            }
    }
	if(cached_bytes) 
		probe_info.cached_bytes = cached_bytes;
	if(delays < 0)
	    delays = 0;
        log_print("**[%s:%d]pid:%d cache time=%d bytes:%d cur_time=%d", __FUNCTION__,__LINE__,p_para->player_id, delays, cached_bytes, new_time.tv_sec);
        send_event(p_para, PLAYER_EVENTS_PLAYER_CACHETIME, delays, &probe_info);
    }  

    return 1;
}

long player_get_systemtime_ms(void)
{
    struct timeval  time;
    gettimeofday(&time, NULL);
    return (time.tv_usec / 1000 + time.tv_sec * 1000);
}
int check_time_interrupt(long *old_msecond, int interval_ms)
{
    int ret = 0;
    struct timeval  new_time;
    long new_time_mseconds;
    gettimeofday(&new_time, NULL);
    new_time_mseconds = (new_time.tv_usec / 1000 + new_time.tv_sec * 1000);
    if (new_time_mseconds > (*old_msecond + interval_ms)) {
        ret = 1;
        *old_msecond = new_time_mseconds;
    } else if (new_time_mseconds < *old_msecond) {
        *old_msecond = new_time_mseconds; /*update time only.*/
    }
    return ret;
}

void set_drm_rental(play_para_t *p_para, unsigned int rental_value)
{
    p_para->state.drm_rental = rental_value;

    return;
}
int check_audio_ready_time(int64_t *first_time)
{
    struct timeval  new_time;
    int64_t new_time_mseconds;
    gettimeofday(&new_time, NULL);
    new_time_mseconds = (new_time.tv_usec / 1000 + (int64_t)new_time.tv_sec * 1000);

    if (*first_time == 0) {
        *first_time = new_time_mseconds;
    }

    if (new_time_mseconds - *first_time > 25000) {// 25s watchdog
        return 1;
    }

    return 0;
}
int player_hwbuflevel_update(play_para_t *player)
{
    struct buf_status vbuf, abuf, sbuf;
    struct vdec_status vdec;
    struct adec_status adec;
    player_status sta;
    int ret;
    hwbufstats_t hwbufs;

    sta = get_player_state(player);
    if (sta < PLAYER_INITOK  || sta >= PLAYER_ERROR) {
        return 0;
    }
    MEMSET(&vbuf, 0, sizeof(struct buf_status));
    MEMSET(&abuf, 0, sizeof(struct buf_status));
    MEMSET(&sbuf, 0, sizeof(struct buf_status));
    ret = update_codec_info(player, &vbuf, &abuf,&sbuf, &vdec, &adec);
    if (ret == 0) {
        hwbufs.vbufused = player->media_info.stream_info.has_video;
        hwbufs.abufused = player->media_info.stream_info.has_audio;
        hwbufs.sbufused = 0;
        if (hwbufs.vbufused) {
            hwbufs.vbufsize = vbuf.size;
            hwbufs.vdatasize = vbuf.data_len;
        }
        if (hwbufs.abufused) {
            hwbufs.abufsize = abuf.size;
            hwbufs.adatasize = abuf.data_len;
        }
        if (hwbufs.vbufused || hwbufs.abufused) {
            send_event(player, PLAYER_EVENTS_HWBUF_DATA_SIZE_CHANGED, &hwbufs, 0);
        }
    }
    return 0;
}
int player_read_streaming_subtitle(play_para_t * player)
{
    int ret = -1;
    if (!player->pFormatCtx || !player->pFormatCtx->iformat)
        return ret;

    if(strncmp(player->pFormatCtx->iformat->name, "mhls", 4))
        return ret;

    if(!player->pFormatCtx->iformat->read_subtitle)
        return ret;
    
    AVSubtitleData * subData = NULL;
    subData = player->pFormatCtx->iformat->read_subtitle(player->pFormatCtx);
    if (!subData) {
        return ret;
    }
    send_event(player, PLAYER_EVENTS_SUBTITLE_DATA, subData, 0);
    return ret;
}

void check_avdiff_status(play_para_t *p_para)
{
    if (p_para->playctrl_info.audio_ready == 1
        && (!p_para->playctrl_info.search_flag)
        && (!p_para->playctrl_info.fast_backward)
        && (!p_para->playctrl_info.fast_forward)
        && (!p_para->playctrl_info.hls_forward)
        && (!p_para->playctrl_info.hls_backward)
        && p_para->astream_info.has_audio
        && p_para->vstream_info.has_video
        && get_tsync_enable()) {
        if (check_avdiff_time(p_para)
            && (p_para->state.current_time < p_para->state.full_time - 1)) {
            p_para->playctrl_info.time_point = p_para->state.current_time + 1;
            p_para->playctrl_info.reset_flag = 1;
            set_black_policy(0);
            p_para->playctrl_info.end_flag = 1;
            log_print("[%s:%d]AV diff is too long, need reset\n", __FUNCTION__, __LINE__);
        }
    }

    return;
}
int check_to_retry(play_para_t *p_para)
{
    if (url_interrupt_cb() > 0) {               // interrupt  to retry
        return -1;
    }

    if ((p_para->pFormatCtx->pb && p_para->pFormatCtx->pb->is_slowmedia) && !p_para->playctrl_info.read_end_flag) {
        player_status sta;
        sta = get_player_state(p_para);
        if (sta == PLAYER_SEARCHING || sta == PLAYER_SEARCHOK || sta == PLAYER_FF_END || sta == PLAYER_FB_END) {
            p_para->retry_cnt++;            // retry many times
        } else if (sta == PLAYER_ERROR || sta == PLAYER_PLAYEND || sta == PLAYER_STOPED || sta == PLAYER_EXIT) {
            return -1;                      // no retry
        } else {
            p_para->retry_cnt = 0;    // retry for ever
        }

        if (p_para->retry_cnt < 500) {
            p_para->pFormatCtx->pb->error = 0;
            return 0;
        } else {
            log_print("[%s:%d]The player state is=%d,retry_cnt=%d\n", __FUNCTION__, __LINE__, sta, p_para->retry_cnt);
        }
    }
    return -1;
}

