/*
 * player_ffmpeg_cache.c
 *
 * Import PacketQueue From ffmpeg
 *
 * */

#include <string.h>
#include "player_ffmpeg_cache.h"
#include "player_priv.h"

/*============================================
	Macro
============================================
*/
//#define DEBUG_CACHE_SEEK 1
//#define DEBUG_CACHE_PUT  1
//#define DEBUG_CACHE_GET  1
//#define DEBUG_CACHE_SUB  1
//#define DEBUG_CACHE_AUDIO 1
//#define DEBUG_CACHE_FRAMES 1
#define DEBUG_CACHE_IN_OUT 1

#define PTS_90K 90000
#define PTS_DROP_EDGE (1*PTS_90K)
#define PTS_DISCONTINUE_MS (500)
#define PTS_DURATION_CALC_TIME_MS (2000)
#define CACHE_THREAD_SLEEP_US (10*1000)
#define MSG_UPDATE_STATE_DURATION_MS (300)
#define CURRENT_TIME_MS_DISCONT (500)
//diff 500ms [300ms is update duration, should be bigger than 300ms]
#define CURRENT_TIME_MS_DISCONTINUE (MSG_UPDATE_STATE_DURATION_MS + CURRENT_TIME_MS_DISCONT)
#define SEEK_KEY_FRAME_MAX_NUM (60)

#define AVSYNC_TAG "AVSYNC"

typedef enum {
	EC_OK = 0,
	EC_STATE_CHANGED = -2,
	EC_NOT_FRAME_NEEDED = -3,
}ERROR_CODE_e;

/*============================================
	static function
============================================
*/
static int avpkt_cache_param_init(av_packet_cache_t *cache_ptr, void *context);
static int avpkt_cache_release(av_packet_cache_t *cache_ptr);
static int avpkt_cache_reset(av_packet_cache_t *cache_ptr);
static int avpkt_cache_put(void);
static int avpkt_cache_get_byindex(av_packet_cache_t *cache_ptr, AVPacket *pkt, int stream_idx);
static int avpkt_cache_get_byindex_bigpkt(PacketQueue *q, AVPacket *pkt);
static int avpkt_cache_check_can_put(av_packet_cache_t *cache_ptr, AVPacket *pkt);
static int avpkt_cache_check_can_get(av_packet_cache_t *cache_ptr, int* stream_idx, int want_idx);
static int64_t avpkt_cache_queue_search(PacketQueue *q, int64_t seekTimeSec);
static int avpkt_cache_queue_seek(PacketQueue *q, int64_t seekTimeMs);
static int avpkt_cache_queue_seek_bypts(PacketQueue *q, int64_t pts, int small_flag);
static int avpkt_cache_queue_seek_bypts_frameid(PacketQueue *q, int64_t pts, int64_t frame_id, int small_flag);
static int avpkt_cache_queue_seektoend(PacketQueue *q);
static int avpkt_cache_switch_sub(av_packet_cache_t *cache_ptr);
static int avpkt_cache_switch_audio(av_packet_cache_t *cache_ptr, int stream_index);
static int avpkt_cache_interrupt_read(av_packet_cache_t *cache_ptr, int blackout);
static int avpkt_cache_uninterrupt_read(av_packet_cache_t * cache_ptr);
static int avpkt_cache_check_netlink(void);
static int avpkt_cache_check_frames_reseved_enough(av_packet_cache_t *cache_ptr);
static int avpkt_cache_check_streaminfo_status(av_packet_cache_t *cache_ptr);
static int avpkt_cache_update_bufed_time(void);
static int avpkt_cache_update_player_param(av_packet_cache_t *cache_ptr);
static int avpkt_cache_new_packet(AVPacket *dst_pkt, AVPacket *src_pkt);
static int64_t avpkt_cache_reduce_keepframes_ms(PacketQueue *q);
void *avpkt_cache_reset_thread(void *arg);
int avpkt_cache_reset_thread_t(av_packet_cache_t *cache_ptr);
static int  avpkt_cache_interrupt(av_packet_cache_t *cache_ptr);
static int avpkt_cache_queue_used(av_packet_cache_t *cache_ptr,PacketQueue *q);

/*============================================
	static var
============================================
*/
static av_packet_cache_t * s_avpkt_cache = NULL;

#ifdef DEBUG_CACHE_PUT
static int s_put_cnt = 0;
#endif

#ifdef DEBUG_CACHE_GET
static int s_get_max = 0;
static int s_get_cnt = 0;
static int s_get_fail = 0;
#endif

#ifdef DEBUG_CACHE_IN_OUT
static int s_cpu_get_cnt = 0;
static int s_pkt_used_error = 0;
#endif
/*============================================
	function realize
============================================
*/
static int avpkt_cache_new_packet(AVPacket *dst_pkt, AVPacket *src_pkt)
{
	int ret = 0;
	int i = 0;

    if((unsigned)src_pkt->size< (unsigned)src_pkt->size + FF_INPUT_BUFFER_PADDING_SIZE)
        dst_pkt->data = av_mallocz(src_pkt->size + FF_INPUT_BUFFER_PADDING_SIZE);

    if (dst_pkt->data == NULL){
    	log_print("%s no mem", __FUNCTION__);
		return -1;
	}

	dst_pkt->pts   = src_pkt->pts;
    dst_pkt->dts   = src_pkt->dts;
    dst_pkt->pos   = src_pkt->pos;
    dst_pkt->duration = src_pkt->duration;
    dst_pkt->convergence_duration = src_pkt->convergence_duration;
    dst_pkt->flags = src_pkt->flags;
    dst_pkt->stream_index = src_pkt->stream_index;
    dst_pkt->side_data       = NULL;
    dst_pkt->side_data_elems = 0;
    dst_pkt->size = src_pkt->size;

	memcpy(dst_pkt->data, src_pkt->data, src_pkt->size);
	if (src_pkt->side_data_elems > 0) {
		dst_pkt->side_data = av_mallocz(src_pkt->side_data_elems * sizeof(*src_pkt->side_data));
		if (dst_pkt->side_data == NULL)
			goto failed_alloc;
		memcpy(dst_pkt->side_data, src_pkt->side_data,src_pkt->side_data_elems * sizeof(*src_pkt->side_data));
		if (src_pkt != dst_pkt) {
			memset(dst_pkt->side_data, 0, src_pkt->side_data_elems * sizeof(*src_pkt->side_data));
		}
		for (i = 0; i < src_pkt->side_data_elems; i++) {
			if ((unsigned)(src_pkt->side_data[i].size) > (unsigned)(src_pkt->side_data[i].size) + FF_INPUT_BUFFER_PADDING_SIZE)
				goto failed_alloc;
			dst_pkt->side_data[i].data = av_mallocz((unsigned)(src_pkt->side_data[i].size) + FF_INPUT_BUFFER_PADDING_SIZE);
			if (dst_pkt->side_data[i].data == NULL)
				goto failed_alloc;
			memcpy(dst_pkt->side_data[i].data, src_pkt->side_data[i].data, src_pkt->side_data[i].size);
			memcpy(dst_pkt->side_data[i].data, src_pkt->side_data[i].data, src_pkt->side_data[i].size);
			dst_pkt->side_data_elems++;
		}

		dst_pkt->side_data_elems = src_pkt->side_data_elems;
	}

	dst_pkt->destruct = av_destruct_packet;
	return 0;
failed_alloc:
	av_destruct_packet(dst_pkt);
	av_init_packet(dst_pkt);
	return -1;
}

static int close_to(int a, int b, int m)
{
	return (abs(a - b) < m) ? 1 : 0;
}

#define RATE_CORRECTION_THRESHOLD 90
#define RATE_24_FPS  3750   /* 24.04  pts*/
#define RATE_25_FPS  3600   /* 25 */
#define RATE_26_FPS  3461   /* 26 */
#define RATE_30_FPS  3000   /*30*/
#define RATE_50_FPS  1800   /*50*/
#define RATE_60_FPS  1500   /*60*/

static int duration_pts_invalid_check(int pts_duration)
{
	int fm_duration = 0;

	if (pts_duration < 0) {
		return 0;
	}

	if (close_to(pts_duration,
			RATE_24_FPS,
			RATE_CORRECTION_THRESHOLD) == 1) {
		fm_duration = RATE_24_FPS;
	} else if (close_to(pts_duration,
			RATE_25_FPS,
			RATE_CORRECTION_THRESHOLD) == 1) {
		fm_duration = RATE_25_FPS;
	} else if (close_to(pts_duration,
			RATE_26_FPS,
			RATE_CORRECTION_THRESHOLD) == 1) {
		fm_duration = RATE_26_FPS;
	} else if (close_to(pts_duration,
			RATE_30_FPS,
			RATE_CORRECTION_THRESHOLD) == 1) {
		fm_duration = RATE_30_FPS;
	} else if (close_to(pts_duration,
			RATE_50_FPS,
			RATE_CORRECTION_THRESHOLD) == 1) {
		fm_duration = RATE_50_FPS;
	} else if (close_to(pts_duration,
			RATE_60_FPS,
			RATE_CORRECTION_THRESHOLD) == 1) {
		fm_duration = RATE_60_FPS;
	}

	return fm_duration;

}

static int packet_queue_put_update(PacketQueue *q, AVPacket *pkt)
{
    MyAVPacketList *pkt1;
	MyAVPacketList *pkttmp;
	int64_t diff_pts = 0;
	int64_t avg_duration_ms = 0;
	int64_t cache_pts;
	int64_t discontinue_pts = 0;
	int64_t bak_cache_pts = 0;
	int 	pts_discontinue_flag = 0;
	int 	ret = -1;

    if (s_avpkt_cache->state != 2)
       return EC_STATE_CHANGED;

	cache_lock(&q->lock);
    #if 0
	if ((q->nb_packets == q->max_packets) && (q->frames_for_seek_backward >= 1) ) {
		if (q->queue_max_kick == 0) {
			q->queue_max_kick = 1;
			q->queue_maxtime_pts = q->bak_cache_pts; //pts value
			log_print("%d reach maxpackets, mem:%d, firstpts:0x%llx, lastpts:0x%llx, diff_pts_ms:%lld",
				pkt->stream_index, q->size, q->head_valid_pts, q->tail_valid_pts, 
				(int64_t)(((q->tail_valid_pts - q->head_valid_pts) * q->timebase) / 90));
		}

		//delete frist_pkt
		pkttmp = q->first_pkt;
		if (q->cur_pkt == NULL) {
			log_error("%s Bad Exception\n", __FUNCTION__);
		} else {
			if (q->cur_pkt == q->first_pkt) {
				q->cur_pkt = pkttmp->next;
				log_print("cache full of unused, cur->used:%d, in:%lld,out:%lld, sb:%d\n",
					q->cur_pkt->used, q->frames_in, q->frames_out, q->frames_for_seek_backward);
			}
		}
		q->first_pkt = pkttmp->next;
		pkttmp->priv = NULL;
		pkttmp->next = NULL;

		q->backwardsize -= pkttmp->pkt.size;

		//calc free mem for this packet
		q->size -= pkttmp->pkt.size;
		{
			if (pkttmp->pkt.side_data_elems > 0) {
				q->size -= (pkttmp->pkt.side_data_elems * sizeof(pkttmp->pkt.side_data));
			}
		}
		q->size -= sizeof(*pkttmp);
		//end

		av_free_packet(&pkttmp->pkt);
		av_free(pkttmp);
		pkttmp = NULL;

		q->frames_for_seek_backward--;

		//update queue first pts
		pkttmp = q->first_pkt;
		while(pkttmp != NULL) {
			if (pkttmp->pkt.pts != AV_NOPTS_VALUE) {
				q->head_valid_pts = pkttmp->pkt.pts;
				break;
			}
			pkttmp = pkttmp->next;
		}

		pkttmp = NULL;
	}
#endif
	/*
	from first pkt, check video keyframe, if not drop it, until max(61frames)
	*/
	if (q->first_keyframe == -1) {
		#ifdef DEBUG_CACHE_PUT
		log_print("pkt[%d]-pts:0x%llx, dts:0x%llx, key:%d\n", pkt->stream_index, pkt->pts, pkt->dts, ((pkt->flags & AV_PKT_FLAG_KEY)));
		#endif
		if (pkt->flags & AV_PKT_FLAG_KEY) {
			q->first_keyframe = 1;
			q->first_keyframe_pts = pkt->pts;
			if (pkt->stream_index == s_avpkt_cache->video_index) {
				s_avpkt_cache->key_time = av_gettime();
				log_print("keytime = %lld\n", s_avpkt_cache->key_time);
				int cost_time = s_avpkt_cache->key_time - s_avpkt_cache->start_time;
				log_print("find first video keyframe in %d frames, key_pts:0x%llx, cost_time= %d ms\n", q->keyframe_check_cnt, pkt->pts, cost_time/1000);
			} else if (pkt->stream_index == s_avpkt_cache->audio_index) {
				log_print("find first audio keyframe in %d frames, key_pts:0x%llx", q->keyframe_check_cnt, pkt->pts);
			}
		} else {
			q->keyframe_check_cnt++;
		}
	} else {
		#ifdef DEBUG_CACHE_PUT
		if (q->frames_in <= 60) {
			log_print("[%d][%lld]-pts:0x%llx, dts:0x%llx, key:%d\n",
				pkt->stream_index, q->frames_in, pkt->pts, pkt->dts, (pkt->flags & AV_PKT_FLAG_KEY));
		}
		#endif
	}

    pkt1 = av_mallocz(sizeof(MyAVPacketList));
    if (!pkt1) {
		log_print("[%s]no mem, nb_packets:%d, max_packets(%d)\n", __FUNCTION__,q->nb_packets, q->max_packets);
		if (q->nb_packets > 0)
			q->max_packets = q->nb_packets;
		log_print("[%s]no mem, change max_packets to nb_packets(%d)\n", __FUNCTION__, q->max_packets);
		cache_unlock(&q->lock);
		return -1;
	}

	//add to tail
	if ((ret = avpkt_cache_new_packet(&pkt1->pkt, pkt)) < 0) {
		av_free(pkt1);
		pkt1 = NULL;
		cache_unlock(&q->lock);
		return ret;
	}

	pkt1->next = NULL;
	pkt1->used = 0;
	pkt1->frame_id = s_avpkt_cache->read_frames + 1;

	/*
		calc frame_dur, trust keyframe pts
	*/
	int frame_dur = 0;
	int frame_dur_pts = 0;
	int allow_no_key_flag = 0;//default
	if (q->dur_calc_done == 0) {
		//find first keyframe pts
		if (pkt->stream_index == s_avpkt_cache->video_index
			|| pkt->stream_index == s_avpkt_cache->audio_index) {
			if (pkt->stream_index == s_avpkt_cache->audio_index)
				allow_no_key_flag = 1;
			if (q->dur_calc_cnt >= 1)
				q->dur_calc_cnt++;

			if (((pkt->flags & AV_PKT_FLAG_KEY) == AV_PKT_FLAG_KEY || allow_no_key_flag == 1)
				&& pkt->pts != AV_NOPTS_VALUE) {
				if (q->dur_calc_pts_start == -1) {
					q->dur_calc_pts_start = pkt->pts;
					q->dur_calc_pts_end = pkt->pts;
					q->dur_calc_cnt = 1;//0->1
				} else {
					q->dur_calc_pts_end = pkt->pts;
					if ((q->dur_calc_pts_end - q->dur_calc_pts_start) >= (int64_t)((PTS_DURATION_CALC_TIME_MS * 90) / q->timebase)) {
						//keep first keyframe vpts
						if (q->first_keyframe_pts == -1) {
							q->first_keyframe_pts = pkt->pts;
						}

						//calc pts enough
						if (q->dur_calc_cnt > 1) {
							frame_dur = (int)((q->dur_calc_pts_end - q->dur_calc_pts_start)/(q->dur_calc_cnt - 1));
							frame_dur_pts = duration_pts_invalid_check((int)(frame_dur * q->timebase));
							if (frame_dur_pts > 0) {
								q->frame_dur_pts = frame_dur_pts;
								q->dur_calc_pts_start = q->dur_calc_pts_end;
							} else {
								q->dur_calc_pts_start = q->dur_calc_pts_end;
							}
						}

						q->dur_calc_cnt = 1;
					}
				}
			}else if (((pkt->flags & AV_PKT_FLAG_KEY) == AV_PKT_FLAG_KEY && allow_no_key_flag == 0)
			&& pkt->pts == AV_NOPTS_VALUE) {
			/* +[SE] [BUG][BUG-169774][yanan.wang] added:soft probe cannot obtain the time that can be played for some sources*/
				int frame_toggled = get_sysfs_int("/sys/module/amvideo/parameters/first_frame_toggled");
				if (frame_toggled == 1) {
					char fps_info[200] = {0};
					int fps_input_value = 0;
					amsysfs_get_sysfs_str("/sys/class/video/fps_info", fps_info, sizeof(fps_info));
					char * split = ": ";
					char *fps_input= strtok(fps_info, split);
					fps_input = strtok (NULL, split);
					sscanf(fps_input, "%x", &fps_input_value);
					log_print("fps_info=%s, fps_input=%s\n", fps_info, fps_input);
					if (fps_input_value != 0) {
						q->frame_dur_pts = 90000 / fps_input_value;
					}
					log_print("fps_input_value=%d, frame_dur_pts=%d\n", fps_input_value, q->frame_dur_pts);
				}
			}
		}
	}
	//end

	/*
	* pts
	*/
	pts_discontinue_flag = q->pts_discontinue_flag;
	discontinue_pts = q->discontinue_pts;
	cache_pts = q->cache_pts;
	bak_cache_pts = q->bak_cache_pts;

	pkt1->pts = (pkt1->pkt.pts == AV_NOPTS_VALUE? pkt1->pkt.dts : pkt1->pkt.pts);
	if (pkt1->pts != AV_NOPTS_VALUE) {
		if (q->pts1 == -1) {
			q->pts1 = pkt1->pts;
			q->pts2 = pkt1->pts;
		} else {
			q->pts1 = q->pts2;
			q->pts2 = pkt1->pts;
		}

		if (pts_discontinue_flag == 1) {
			cache_pts -= discontinue_pts;
			if (q->frame_dur_pts > 0) {
				cache_pts += q->frame_dur_pts;
			}

			pts_discontinue_flag = 0;
		}

		bak_cache_pts = cache_pts;
		q->lastPkt_playtime_pts = bak_cache_pts;

		diff_pts = q->pts2 - q->pts1;
		discontinue_pts = diff_pts;
		cache_pts += discontinue_pts;

		/*
		*  need to be optimized:
		*  "PTS_DISCONTINUE" should refer in audio and video when audio and video both exists,
		*  in some case, any pts_discontinue value maybe rational
		*/
		if (abs(discontinue_pts) >= (int64_t)((PTS_DISCONTINUE_MS * 90) / q->timebase)) {
			pts_discontinue_flag = 1;
		}
	}

	q->pts_discontinue_flag = pts_discontinue_flag;
	q->discontinue_pts = discontinue_pts;
	q->cache_pts = cache_pts;
	q->bak_cache_pts = bak_cache_pts;

	/*
	* End
	*/

    if (!q->last_pkt) {
		pkt1->priv = NULL;
        q->first_pkt = pkt1;
		q->cur_pkt = q->first_pkt;
		if (q->cur_pkt->pts != AV_NOPTS_VALUE)
			q->cur_valid_pts = q->cur_pkt->pts;
	} else {
		pkt1->priv = q->last_pkt;
        q->last_pkt->next = pkt1;
	}

    q->last_pkt = pkt1;
    q->nb_packets++;

	//calc mem used for this packet
	q->size += pkt1->pkt.size;
	{
		if (pkt->side_data_elems > 0) {
			q->size += (pkt1->pkt.side_data_elems * sizeof(pkt1->pkt.side_data));
		}
	}
	q->size += sizeof(*pkt1);
	//end

	q->frames_in++;

	{
		q->forwardsize += pkt1->pkt.size;
		q->frames_for_seek_forward++;
	}

	if (pkt1->pts != AV_NOPTS_VALUE) {
		q->tail_valid_pts = pkt1->pts;
	}

	q->lastPkt_playtime_pts = q->bak_cache_pts;

	if (q->head_valid_pts == -1 && pkt1->pts != AV_NOPTS_VALUE) {
		q->head_valid_pts = pkt1->pts;
		q->first_valid_pts = pkt1->pts;
	}

	if (q->pts_discontinue_flag == 1) {
		pkt1->offset_pts = q->bak_cache_pts;
	} else {
		pkt1->offset_pts = q->cache_pts;
	}

    cache_unlock(&q->lock);
	return 0;
}

/* packet queue handling */
static void packet_queue_init(PacketQueue *q, av_packet_cache_t *cache_ptr, int stream_index, SteamType_E steam_type)
{
    memset(q, 0, sizeof(PacketQueue));
    cache_lock_init(&q->lock, NULL);
    
        q->steam_type = steam_type;
        q->stream_index = stream_index;
	if (q->steam_type == STEAM_TYPE_AUDIO) {
		q->max_packets = cache_ptr->audio_max_packet;
        q->frames_backward_level = (float)am_getconfig_int_def("libplayer.cache.backseek", 1)/1000;
	} else if (q->steam_type == STEAM_TYPE_VIDEO) {
		q->max_packets = cache_ptr->video_max_packet;
        q->frames_backward_level = (float)am_getconfig_int_def("libplayer.cache.backseek", 1)/1000;
	} else if (q->steam_type == STEAM_TYPE_SUB) {
		q->max_packets = cache_ptr->sub_max_packet;
	}

	q->size = 0;
	q->backwardsize = 0;
	q->forwardsize = 0;
	q->nb_packets = 0;

	q->first_pkt = NULL;
	q->cur_pkt = NULL;
	q->last_pkt = NULL;

	q->first_valid_pts = -1;
	q->head_valid_pts = -1;
	q->tail_valid_pts = -1;
	q->cur_valid_pts = -1;

	q->pts1 = -1;
	q->pts2 = -1;
	q->last_pts2 = -1;
	q->dur_calc_pts_start = -1;
	q->dur_calc_pts_end = -1;
	q->dur_calc_cnt = 0;
	q->dur_calc_done = 0;
	q->cache_pts = 0;
	q->bak_cache_pts = 0;
	q->pts_discontinue_flag = 0;
	q->discontinue_pts = 0;
	q->frame_dur_pts = 0;

	q->firstPkt_playtime_pts = -1;
	q->curPkt_playtime_pts = -1;
	q->lastPkt_playtime_pts = 0;

	q->queue_max_kick = 0;
	q->queue_maxtime_pts = 0;

	q->frames_in = 0;
	q->frames_out = 0;

	q->first_keyframe_pts = -1;
	q->first_keyframe = -1;
	q->keyframe_check_cnt = 0;
	q->keyframe_check_max = 61;

	q->frames_max_seekbackword = (int)(q->frames_backward_level * q->max_packets);
	if (q->frames_max_seekbackword == 0) {
		q->frames_max_seekbackword = 1;
	}
	q->frames_max_seekforword = (q->max_packets - q->frames_for_seek_backward);
	q->frames_for_seek_forward = 0;
	q->frames_for_seek_backward = 0;
}

static void packet_queue_flush(PacketQueue *q) {
	MyAVPacketList *pkt, *pkt1;

	cache_lock(&q->lock);

	log_print("%s nb_packets:%d\n", __FUNCTION__, q->nb_packets);
	int i = 0;
	for (pkt = q->first_pkt; pkt; pkt = pkt1) {
		pkt1 = pkt->next;
		av_free_packet(&pkt->pkt);
		av_free(pkt);
		i++;
	}

	q->first_pkt = NULL;
	q->cur_pkt = NULL;
	q->last_pkt = NULL;

	q->max_packets = 0;
	q->nb_packets = 0;
	q->size = 0;
	q->backwardsize = 0;
	q->forwardsize = 0;

	q->first_valid_pts = -1;
	q->head_valid_pts = -1;
	q->tail_valid_pts = -1;
	q->cur_valid_pts = -1;

	q->pts1 = -1;
	q->pts2 = -1;
	q->last_pts2 = -1;
	q->dur_calc_pts_start = -1;
	q->dur_calc_pts_end = -1;
	q->dur_calc_cnt = 0;
	q->dur_calc_done = 0;
	q->discontinue_pts = 0;
	q->pts_discontinue_flag = 0;
	q->cache_pts = 0;
	q->bak_cache_pts = 0;

	q->firstPkt_playtime_pts = -1;
	q->curPkt_playtime_pts = -1;
	q->lastPkt_playtime_pts = -1;

	q->queue_maxtime_pts = 0;
	q->queue_max_kick = 0;

	q->frames_in = 0;
	q->frames_out = 0;

	q->first_keyframe_pts = -1;
	q->keyframe_check_cnt = 0;
	q->first_keyframe = -1;

	q->frames_for_seek_forward = 0;
	q->frames_for_seek_backward = 0;

	q->prior = 0;
	cache_unlock(&q->lock);

}

static void packet_queue_reset(PacketQueue *q, int stream_index)
{
	MyAVPacketList *pkt, *pkt1;

	cache_lock(&q->lock);

	log_print("%s nb_packets:%d\n", __FUNCTION__, q->nb_packets);
	int i = 0;
	for (pkt = q->first_pkt; pkt; pkt = pkt1) {
		pkt1 = pkt->next;
		av_free_packet(&pkt->pkt);
		av_free(pkt);
		i++;
	}

	q->size = 0;
	q->backwardsize = 0;
	q->forwardsize = 0;
	q->nb_packets = 0;

	q->first_pkt = NULL;
	q->cur_pkt = NULL;
	q->last_pkt = NULL;

	q->first_valid_pts = -1;
	q->head_valid_pts = -1;
	q->tail_valid_pts = -1;
	q->cur_valid_pts = -1;

	q->pts1 = -1;
	q->pts2 = -1;
	q->last_pts2 = -1;
	q->dur_calc_pts_start = -1;
	q->dur_calc_pts_end = -1;
	q->dur_calc_cnt = 0;
	q->dur_calc_done = 0;
	q->cache_pts = 0;
	q->bak_cache_pts = 0;
	q->pts_discontinue_flag = 0;
	q->discontinue_pts = 0;
	q->frame_dur_pts = 0;

	q->firstPkt_playtime_pts = -1;
	q->curPkt_playtime_pts = -1;
	q->lastPkt_playtime_pts = 0;

	q->queue_max_kick = 0;
	q->queue_maxtime_pts = 0;

	q->frames_in = 0;
	q->frames_out = 0;

	q->first_keyframe_pts = -1;
	q->first_keyframe = -1;
	q->keyframe_check_cnt = 0;
	q->keyframe_check_max = 61;

	q->frames_for_seek_forward = 0;
	q->frames_for_seek_backward = 0;

	q->stream_index = stream_index;
	cache_unlock(&q->lock);
}

static void packet_queue_destroy(PacketQueue *q)
{
    packet_queue_flush(q);
	cache_lock_uninit(&q->lock);
}
int64_t avpkt_cache_get_keytime()
{
    log_print("return cache key_time=%lld\n",s_avpkt_cache->key_time);
    return s_avpkt_cache->key_time;
}
/*
call pre-condition:
	can get, cur_pkt != NULL
*/
int avpkt_cache_queue_get(PacketQueue *q, AVPacket *pkt)
{
	MyAVPacketList *pkt1;
	int ret = 0;
	int64_t pts2 = 0;
	int64_t pts1 = 0;
	int erro_case = 0;

	if (!q || !pkt) {
		log_print("%s invalid param\n", __FUNCTION__);
		return -1;
	}

	cache_lock(&q->lock);

	if (q->cur_pkt == NULL) {
		pkt->destruct = NULL;
		cache_unlock(&q->lock);
		return -1;
	}

	pkt1 = q->cur_pkt;
	if (pkt1->used == 1) {
		if (pkt1->next == NULL) {
			pkt->destruct = NULL;
			cache_unlock(&q->lock);
			return -1;
		} else {
			q->cur_pkt = pkt1->next;
			pkt1 = q->cur_pkt;
		}
	}

	{
		if (pkt1->pkt.pts != AV_NOPTS_VALUE)
			q->cur_valid_pts = pkt1->pkt.pts;

		q->frames_for_seek_forward--;
		q->frames_for_seek_backward++;
		q->frames_out++;
		q->backwardsize += pkt1->pkt.size;
		q->forwardsize -= pkt1->pkt.size;
		if (pkt1->used == 0) {
			*pkt = pkt1->pkt;
		} else {
			erro_case = 1;
		}

		pkt->destruct = NULL;
		//log_print("out id:%lld idx:%d, pts2:0x%llx, first id:%lld",pkt1->frame_id,pkt1->pkt.stream_index,pkt1->pts, q->first_pkt->frame_id);

		q->cur_pkt->used = 1;
		if (q->cur_pkt->next != NULL) {
			q->cur_pkt = q->cur_pkt->next;
		}

		#ifdef DEBUG_CACHE_GET
		s_get_fail = 0;
		#endif

		#ifdef DEBUG_CACHE_IN_OUT
		if (pkt1->used == 1) {
			if (s_pkt_used_error >= 0)
				s_pkt_used_error++;

			if (s_pkt_used_error == 1 || (s_pkt_used_error % 200) == 0) {
				if (q->stream_index == s_avpkt_cache->audio_index) {
					log_print("audio[%lld] used:%d, in:%lld, out:%lld, forward:%d, backward:%d\n",
						pkt1->frame_id, pkt1->used, q->frames_in, q->frames_out,
						q->frames_for_seek_forward, q->frames_for_seek_backward);
				} else if (q->stream_index == s_avpkt_cache->video_index) {
					log_print("video[%lld] used:%d, in:%lld, out:%lld, forward:%d, backward:%d\n",
						pkt1->frame_id, pkt1->used, q->frames_in, q->frames_out,
						q->frames_for_seek_forward, q->frames_for_seek_backward);
				}
			} else {
				if (s_pkt_used_error >= 10000) {//50x200
					s_pkt_used_error = -1;
				}
			}
		}
		#endif
	}

	if (q->prior == 1)
		q->prior = 0;

	cache_unlock(&q->lock);

	if (erro_case == 1) {
		log_print("%s pkt->used error, idx:%d\n", __FUNCTION__, pkt1->pkt.stream_index);
		return -1;
	}
	return ret;
}


// ret: pkt num need to give amplayer
static int audio_once(play_para_t *player)
{
	int bitpktnum = am_getconfig_int_def("libplayer.cache.bigpktnum", 0);

    int afmt = player->astream_info.audio_format;
    int channel = player->astream_info.audio_channel;

    if(afmt == AFORMAT_PCM_BLURAY || afmt == AFORMAT_TRUEHD) {

        log_print("afmt:%d channel:%d ret:%d \n", afmt, channel, channel);
        return (channel >= bitpktnum)?channel:bitpktnum;
    }
	return 0;
}

static int avpkt_cache_update_player_param(av_packet_cache_t *cache_ptr)
{
	int is_bestv = 0;
	play_para_t *player = (play_para_t *)cache_ptr->context;
	if (player->pFormatCtx->pb) {
		cache_ptr->local_play = player->pFormatCtx->pb->local_playback;
		cache_ptr->is_segment_media = av_is_segment_media(player->pFormatCtx);
	}

	if(strstr(player->file_name, "bestv") != NULL
	    && strstr(player->file_name, ".m3u8") != NULL) {
		is_bestv = 1;
	}


	int hls_media_quality = am_getconfig_int_def("media.amplayer.quality", 0);
	int bitrate_change = am_getconfig_int_def("libplayer.cache.ratechange", 1);
	if (is_bestv == 1 && cache_ptr->is_segment_media == 1 && hls_media_quality == 2 && bitrate_change == 1) {
		cache_ptr->bitrate_change_flag = 1;
	} else {
		cache_ptr->bitrate_change_flag = 0;
	}

	log_print("%s, bitrate_change_flag=%d,hls_media_quality=%d,bitrate_change=%d,is_segment_media=%d\n",
		__FUNCTION__, cache_ptr->bitrate_change_flag, hls_media_quality, bitrate_change, cache_ptr->is_segment_media);

	cache_ptr->audio_max_packet = am_getconfig_int_def("libplayer.cache.amaxframes", 7000);
	cache_ptr->video_max_packet = am_getconfig_int_def("libplayer.cache.vmaxframes", 3500);
	cache_ptr->sub_max_packet = am_getconfig_int_def("libplayer.cache.smaxframes", 1000);

	if(cache_ptr->local_play)
		cache_ptr->max_cache_mem = am_getconfig_int_def("libplayer.cache.maxmem_local", 60*1024*1024);
	 else if ( cache_ptr->is_segment_media == 1 && player->start_param->is_livemode == 1)
	    cache_ptr->max_cache_mem = am_getconfig_int_def("libplayer.livemode.cache.maxmem", 67108864);
	else
		cache_ptr->max_cache_mem = am_getconfig_int_def("libplayer.cache.maxmem", 67108864);
    log_print("cache max cache mem:%d\n", cache_ptr->max_cache_mem);
	cache_ptr->reach_maxmem_flag = 0;
	cache_ptr->enable_seek_in_cache = am_getconfig_int_def("libplayer.cache.seekenable", 0);
    log_print("--------------penghui-------2----\n");
	if (player->astream_info.has_audio == 1 && player->astream_info.audio_index != -1) {
		cache_ptr->has_audio   = 1;
		cache_ptr->audio_index = player->astream_info.audio_index;
	} else {
		cache_ptr->has_audio   = 0;
		cache_ptr->audio_index = -1;
	}

	if (player->vstream_info.has_video == 1 && player->vstream_info.video_index != -1) {
		cache_ptr->has_video   = 1;
		cache_ptr->video_index = player->vstream_info.video_index;
	} else {
		cache_ptr->has_video   = 0;
		cache_ptr->video_index = -1;
	}

	if (player->sstream_info.has_sub == 1 && player->sstream_info.sub_index != -1) {
		cache_ptr->has_sub     = 1;
		cache_ptr->sub_index   = player->sstream_info.sub_index;
	} else {
		cache_ptr->has_sub     = 0;
		cache_ptr->sub_index   = -1;
	}

	if (!(am_getconfig_bool("media.amplayer.sublowmem"))) {
		cache_ptr->sub_stream = player->sstream_info.sub_stream;
		log_print("sub_stream:0x%x\n", cache_ptr->sub_stream);
	} else {
		cache_ptr->sub_stream = -1;
	}

    cache_ptr->first_apts = -1;
    cache_ptr->first_vpts = -1;
    cache_ptr->first_spts = -1;
	cache_ptr->seekTimeMs = 0;

	if (player->playctrl_info.time_point > 0) {
		cache_ptr->seekTimeMs = (int64_t)(player->playctrl_info.time_point*1000);
		cache_ptr->starttime_ms = cache_ptr->seekTimeMs;
	} else {
		if (player->state.current_ms > 0) {
			cache_ptr->starttime_ms = (int64_t)(player->state.current_ms);
		} else {
			cache_ptr->starttime_ms = 0;
		}
	}

	cache_ptr->currenttime_ms = cache_ptr->starttime_ms;
	cache_ptr->last_currenttime_ms = cache_ptr->starttime_ms;
	cache_ptr->discontinue_current_ms = 0;
	cache_ptr->seek_discontinue_current_ms = -1;

	cache_ptr->read_frames = 0;
	cache_ptr->video_cachems = 0;
	cache_ptr->audio_cachems = 0;
	cache_ptr->sub_cachems = 0;

	cache_ptr->netdown = 0;
	cache_ptr->last_netdown_state = 0;
	cache_ptr->start_time = player->start_time;
	cache_ptr->audio_num =0;
	cache_ptr->sub_num=0;
	cache_ptr->cur_mem_size = 0;
	cache_ptr->discard_frame_id = 0;
	log_print("cache start time:%lld\n", cache_ptr->start_time);
	cache_ptr->queue_audio = &cache_ptr->queue_all_audio[0];
	if(cache_ptr->has_audio){
	int i;
	int num =0;
	if (am_getconfig_int_def("libplayer.cache.multitrack", 1)) {
			log_print("multi audio track:%d\n", player->astream_num);
			for (i=0;i<player->astream_num;i++) {
				packet_queue_init(&cache_ptr->queue_all_audio[i], cache_ptr, player->media_info.audio_info[i]->index, STEAM_TYPE_AUDIO);
				if (player->astream_info.audio_duration > 0) {
					cache_ptr->queue_all_audio[i].timebase = player->astream_info.audio_duration;
				} else {
					cache_ptr->queue_all_audio[i].timebase = 1.0;
				}

                cache_ptr->audio_num++;
                if(cache_ptr->queue_all_audio[i].stream_index == cache_ptr->audio_index)
                 cache_ptr->queue_audio = &cache_ptr->queue_all_audio[i];
            }
        } else {
            packet_queue_init(cache_ptr->queue_audio, cache_ptr, cache_ptr->audio_index, STEAM_TYPE_AUDIO);
            if (player->astream_info.audio_duration > 0) {
                cache_ptr->queue_all_audio[0].timebase = player->astream_info.audio_duration;
            } else {
                cache_ptr->queue_all_audio[0].timebase = 1.0;
            }
            cache_ptr->audio_num++;
        }
            
    }
    cache_ptr->queue_video = &cache_ptr->queue_all_video[0];
    if(cache_ptr->has_video) {
                packet_queue_init(cache_ptr->queue_video, cache_ptr, cache_ptr->video_index, STEAM_TYPE_VIDEO);
		if (player->vstream_info.video_pts > 0) {
			cache_ptr->queue_video->timebase = player->vstream_info.video_pts;
		} else {
			cache_ptr->queue_video->timebase = 1.0;
		}
                cache_ptr->video_num++;
     
    }
    cache_ptr->queue_sub = &cache_ptr->queue_all_sub[0];
    if(cache_ptr->has_sub){
		if (am_getconfig_int_def("libplayer.cache.multitrack", 1)) {
            int i;
            for( i=0;i<player->sstream_num;i++ ){
                packet_queue_init(&cache_ptr->queue_all_sub[i], cache_ptr, player->media_info.sub_info[i]->index, STEAM_TYPE_SUB);
                cache_ptr->sub_num++;
                if(cache_ptr->queue_all_sub[i].stream_index == cache_ptr->sub_index)
                 cache_ptr->queue_sub = &cache_ptr->queue_all_sub[i];
            }
        } else {
            cache_ptr->queue_sub = &cache_ptr->queue_all_sub[0];
            packet_queue_init(cache_ptr->queue_sub, cache_ptr, cache_ptr->sub_index, STEAM_TYPE_SUB);
            cache_ptr->sub_num++;
        }
    }

	cache_ptr->error = 0;

	if (cache_ptr->local_play == 0
		&& cache_ptr->has_video == 1
		&& cache_ptr->video_index != -1) {
		cache_ptr->enable_keepframes = am_getconfig_int_def("libplayer.cache.keepframe_en", 0);

		if(is_bestv == 1 && player->state.full_time <= 0) {
			cache_ptr->enable_keepframes = 0;
			log_print("bestv online live stream, disable keep\n");
		}

		if (!strncmp(player->file_name, "udp:", strlen("udp:"))) {
			cache_ptr->enable_keepframes = 0;
			log_print("udp stream, disable keep\n");
		}

		if (cache_ptr->enable_keepframes == 1) {
			cache_ptr->keeframesstate = 0;
			cache_ptr->leftframes = -1;
			cache_ptr->keepframes = am_getconfig_int_def("libplayer.cache.keepframes", 125);//5s
			cache_ptr->enterkeepframems = am_getconfig_int_def("libplayer.cache.enterkeepms", 5000);//5s
		}
	}

	if (cache_ptr->has_video == 1 && cache_ptr->video_index != -1)
		cache_ptr->seek_by_keyframe = am_getconfig_int_def("libplayer.cache.seekbykeyframe", 1);//default seek by keyframe

	cache_ptr->trickmode = 0;
	cache_ptr->fffb_out_frames = am_getconfig_int_def("libplayer.cache.fffbframes", 61);

	if (cache_ptr->has_audio == 1) {
		int bitpktnum = audio_once(player);
		if (bitpktnum > 0) {
			#define BIG_PKT_DATA_SIZE_MAX (2*1024*1024)  //(8*1024*1024+0x400) in player
			if (cache_ptr->bigpkt.data == NULL) {
				cache_ptr->bigpkt_size = am_getconfig_int_def("libplayer.cache.bigpktsize", BIG_PKT_DATA_SIZE_MAX);
				av_init_packet(&cache_ptr->bigpkt);
				cache_ptr->bigpkt.data = av_mallocz(cache_ptr->bigpkt_size);
				if (cache_ptr->bigpkt.data != NULL) {
					cache_ptr->bigpkt.size = cache_ptr->bigpkt_size;
					cache_ptr->bigpkt_enable = 1;
					cache_ptr->bigpkt_num = bitpktnum;
				} else {
					log_print("BigPkt malloc fail\n");
				}
			}
		}
	}

	log_print("[%s:%d]has_audio:%d,aidx:%d,has_video:%d,vidx:%d,has_sub:%d,sidx:%d,bigpktnum:%d,bigpktsize:%d\n",
		__FUNCTION__, __LINE__, cache_ptr->has_audio, cache_ptr->audio_index,
		cache_ptr->has_video, cache_ptr->video_index,
		cache_ptr->has_sub, cache_ptr->sub_index,
		cache_ptr->bigpkt_num,
		cache_ptr->bigpkt_size);
	log_print("a_timebase:%f, v_timebase:%f\n",
		cache_ptr->queue_audio?cache_ptr->queue_audio->timebase:0,
		cache_ptr->queue_video?cache_ptr->queue_video->timebase:0);

	#ifdef DEBUG_CACHE_PUT
	s_put_cnt = 0;
	#endif

	#ifdef DEBUG_CACHE_GET
	s_get_max = am_getconfig_int_def("libplayer.cache.dgfs", 100);
	s_get_cnt = 0;
	#endif

	#ifdef DEBUG_CACHE_IN_OUT
	s_cpu_get_cnt = 0;
	s_pkt_used_error = 0;
	#endif

	return 0;
}

static int avpkt_cache_param_init(av_packet_cache_t *cache_ptr, void *context)
{
	memset(cache_ptr, 0x0, sizeof(av_packet_cache_t));
	cache_ptr->context = context;
        cache_ptr->interrupt = 0;
        cache_ptr->discard_disable_cnt = 1;
	avpkt_cache_update_player_param(cache_ptr);

	return 0;
}

static int packet_check_add_adts_header(AVPacket *pkt, uint8_t *dst_buf) {
	if (pkt == NULL || pkt->size <= 0 || dst_buf == NULL)
		return -1;

	if ((pkt->flags & AV_PKT_FLAG_WITH_HEADER)
		|| (pkt->flags & AV_PKT_FLAG_ISDECRYPTINFO))
		return -1;

	play_para_t *player = (play_para_t *)s_avpkt_cache->context;
	uint8_t adts_header[ADTS_HEADER_SIZE] = {0x0};
	int size = ADTS_HEADER_SIZE + pkt->size;
	unsigned char *buf = player->astream_info.extradata;
	size &= 0x1fff;

	if (pkt->size >= ADTS_HEADER_SIZE) {
		memcpy(adts_header, pkt->data, ADTS_HEADER_SIZE);
		if ((((adts_header[0] << 4) | (adts_header[1] & 0xF0) >> 4) == 0xFFF)
			&& ((((*(adts_header + 3) & 0x2) << 11) | ((*(adts_header + 4) & 0xFF) << 3) | ((*(adts_header + 5) & 0xE0) >> 5)) == pkt->size)) {
			return -1;
		}
	}

	if (player->astream_info.extradata) {
		buf[3] = (buf[3] & 0xfc) | (size >> 11);
		buf[4] = (size >> 3) & 0xff;
		buf[5] = (buf[5] & 0x1f) | ((size & 0x7) << 5);
		if (player->astream_info.extradata_size == ADTS_HEADER_SIZE) {
			MEMCPY(dst_buf, player->astream_info.extradata, player->astream_info.extradata_size);
			return ADTS_HEADER_SIZE;
		}
	}

	return 0;
}

#define AV_PKT_FLAG_MULTI_PKTS 0x0004
static int avpkt_cache_get_byindex_bigpkt(PacketQueue *q, AVPacket *pkt) {
	if (q == NULL || pkt == NULL) {
		log_print("%s:%d invalid param", __FUNCTION__, __LINE__);
		return -1;
	}

	int ret = -1;
	AVPacket pkttmp;
	AVPacket *pkt_big = &s_avpkt_cache->bigpkt;
	play_para_t *player = (play_para_t *)s_avpkt_cache->context;
	int muxpktnum = 0;

	int i = 0;
	uint8_t * buf_ptr = NULL;

	/*core code Start*/
	pkt_big->size = 0;
	buf_ptr = pkt_big->data;

	for (; i < s_avpkt_cache->bigpkt_num; i++) {
		if (q->cur_pkt->pkt.side_data != NULL)
			break;

		if ((pkt_big->size + q->cur_pkt->pkt.size + 16)
			> (s_avpkt_cache->bigpkt_size))
			break;

		av_init_packet(&pkttmp);
		ret = avpkt_cache_queue_get(q, &pkttmp);
		if (ret < 0) {
			break;
		}

		if (muxpktnum == 0) {
			pkt_big->pts   = pkttmp.pts;
			pkt_big->dts   = pkttmp.dts;
			pkt_big->pos   = pkttmp.pos;
			pkt_big->duration = 0;
			pkt_big->convergence_duration = 0;
			pkt_big->flags = pkttmp.flags;
			pkt_big->stream_index = pkttmp.stream_index;
			pkt_big->destruct= NULL;
			pkt_big->side_data       = NULL;
			pkt_big->side_data_elems = 0;
			pkt_big->size = 0;
		}

		{
			//add audio header
			if (!(memcmp(player->pFormatCtx->iformat->name, "mpegts", 6) == 0
				&& player->file_type != MPEG_FILE
				&& (player->astream_info.audio_format == AFORMAT_AAC
				|| player->astream_info.audio_format == AFORMAT_AAC_LATM))) {
				ret = packet_check_add_adts_header(&pkttmp, buf_ptr);
				if (ret > 0) {
					buf_ptr += ret;
					pkt_big->size += ret;
				}
			}

			if ((player->astream_info.audio_format == AFORMAT_ALAC) ||
				((player->astream_info.audio_format == AFORMAT_ADPCM) &&
				(!player->acodec->audio_info.block_align) &&
				((player->acodec->audio_info.codec_id == CODEC_ID_ADPCM_IMA_WAV) ||
				(player->acodec->audio_info.codec_id == CODEC_ID_ADPCM_MS)))) {
				*buf_ptr = 0x11;
				buf_ptr++;
				*buf_ptr = 0x22;
				buf_ptr++;
				*buf_ptr = 0x33;
				buf_ptr++;
				*buf_ptr = 0x33;
				buf_ptr++;
				*buf_ptr = (pkt->size >> 8) & 0xff;
				buf_ptr++;
				*buf_ptr = (pkt->size) & 0xff;
				buf_ptr++;
				pkt_big->size += 6;
            }

			if (player->astream_info.audio_format == AFORMAT_APE) {
				int extra_data = 8;
				*buf_ptr = 'A';
				buf_ptr++;
				*buf_ptr = 'P';
				buf_ptr++;
				*buf_ptr = 'T';
				buf_ptr++;
				*buf_ptr = 'S';
				buf_ptr++;
				*buf_ptr = (pkt->size - extra_data) & 0xff;
				buf_ptr++;
				*buf_ptr = (pkt->size - extra_data >> 8) & 0xff;
				buf_ptr++;
				*buf_ptr = (pkt->size - extra_data >> 16) & 0xff;
				buf_ptr++;
				*buf_ptr = (pkt->size - extra_data >> 24) & 0xff;
				buf_ptr++;
				pkt_big->size += 8;
            }
		}

		memcpy(buf_ptr, pkttmp.data, pkttmp.size);
		pkt_big->size += pkttmp.size;
		pkt_big->duration += pkttmp.duration;
		pkt_big->convergence_duration += pkttmp.convergence_duration;
		buf_ptr += pkttmp.size;
		muxpktnum++;
	}

	if (muxpktnum == 0) {
		ret = avpkt_cache_queue_get(q, pkt);
	} else {
		pkt_big->flags |= AV_PKT_FLAG_MULTI_PKTS;
		ret = 0;
		*pkt = s_avpkt_cache->bigpkt;
	}

	/*core code End*/

	av_init_packet(&pkttmp);
	return ret;
}

static int avpkt_cache_get_byindex(av_packet_cache_t *cache_ptr, AVPacket *pkt, int stream_idx) {
	if (cache_ptr == NULL || pkt == NULL) {
		log_print("%s:%d invalid param", __FUNCTION__, __LINE__);
		return -1;
	}

	int ret = -1;
	PacketQueue *q = NULL;
	int avs_flag = 0;
	if (stream_idx == cache_ptr->audio_index) {
		q = cache_ptr->queue_audio;
		avs_flag = 1;
	} else if (stream_idx == cache_ptr->video_index) {
		q = cache_ptr->queue_video;
		avs_flag = 2;
	} else if (stream_idx == cache_ptr->sub_index) {
		q = cache_ptr->queue_sub;
		avs_flag = 3;
	}

	if (cache_ptr->bigpkt_enable == 1
		&& avs_flag == 1
		&& q
		&& (q->cur_pkt != NULL && ((q->cur_pkt->pkt.flags & AV_PKT_FLAG_KEY) != 0))
		&& q->frames_max_seekforword >= (cache_ptr->bigpkt_num + 2)) {
		ret = avpkt_cache_get_byindex_bigpkt(q, pkt);
	} else {
		ret = avpkt_cache_queue_get(q, pkt);
	}

	return ret;
}

int avpkt_cache_put_update(av_packet_cache_t *cache_ptr, AVPacket *pkt) {
	int ret = -1;
	play_para_t *player = (play_para_t *)cache_ptr->context;
	if(cache_ptr->has_audio && pkt->stream_index == cache_ptr->audio_index)
    {
		ret = packet_queue_put_update(cache_ptr->queue_audio, pkt);
		if (ret < 0)
			return ret;
        cache_ptr->audio_count = cache_ptr->queue_audio->nb_packets;
        cache_ptr->audio_size = cache_ptr->queue_audio->size;
		cache_ptr->audio_cachems = (int64_t)((cache_ptr->queue_audio->bak_cache_pts * cache_ptr->queue_audio->timebase) / 90);
        if(cache_ptr->first_apts == -1 && cache_ptr->queue_audio->first_valid_pts != -1) {
			cache_ptr->first_apts = cache_ptr->queue_audio->first_valid_pts;
		}

		if (0)
			log_print("aidx:%d, nb_pkts:%d, canread:%d, firstpts:%llx, cur_pktpts:%llx, playpts:0x%x, lastpts:%llx, ret:%d\n",
			cache_ptr->audio_index, cache_ptr->queue_audio->nb_packets, cache_ptr->queue_audio->frames_for_seek_forward,
			cache_ptr->queue_audio->head_valid_pts, cache_ptr->queue_audio->cur_valid_pts, player->state.current_pts, cache_ptr->queue_audio->tail_valid_pts, ret);
	} else if(cache_ptr->has_video && pkt->stream_index == cache_ptr->video_index) {
        ret = packet_queue_put_update(cache_ptr->queue_video, pkt);
		if (ret < 0)
			return ret;
        cache_ptr->video_count = cache_ptr->queue_video->nb_packets;
        cache_ptr->video_size = cache_ptr->queue_video->size;
		cache_ptr->video_cachems = (int64_t)((cache_ptr->queue_video->bak_cache_pts * cache_ptr->queue_video->timebase) / 90);
        if(cache_ptr->first_vpts == -1 && cache_ptr->queue_video->first_valid_pts != -1) {
			cache_ptr->first_vpts = cache_ptr->queue_video->first_valid_pts;
		}

		if (0)
			log_print("vidx:%d, nb_pkts:%d, canread:%d, firstpts:%llx, cur_pktpts:%llx, playpts:0x%x, lastpts:%llx, ret:%d\n",
			cache_ptr->video_index, cache_ptr->queue_video->nb_packets, cache_ptr->queue_video->frames_for_seek_forward,
			cache_ptr->queue_video->head_valid_pts, cache_ptr->queue_video->cur_valid_pts, player->state.current_pts, cache_ptr->queue_video->tail_valid_pts, ret);
    } else if(cache_ptr->has_sub && ((cache_ptr->sub_stream == -1) ? (pkt->stream_index == cache_ptr->sub_index)
		: ((1 << (pkt->stream_index))&cache_ptr->sub_stream))) {
            ret = packet_queue_put_update(cache_ptr->queue_sub, pkt);
		if (ret < 0)
			return ret;
                cache_ptr->sub_count = cache_ptr->queue_sub->nb_packets;
                cache_ptr->sub_size = cache_ptr->queue_sub->size;
		cache_ptr->sub_cachems = (int64_t)((cache_ptr->queue_sub->bak_cache_pts * cache_ptr->queue_sub->timebase) / 90);
            if(cache_ptr->first_spts == -1 && cache_ptr->queue_sub->first_valid_pts != -1) {
            cache_ptr->first_spts = cache_ptr->queue_sub->first_valid_pts;
            }

		if (0)
			log_print("sidx:%d, nb_pkts:%d, canread:%d, firstpts:%llx, cur_pktpts:%llx, playpts:0x%x, lastpts:%llx, ret:%d\n",
			cache_ptr->sub_index, cache_ptr->queue_sub->nb_packets, cache_ptr->queue_sub->frames_for_seek_forward,
			cache_ptr->queue_sub->head_valid_pts, cache_ptr->queue_sub->cur_valid_pts, player->state.current_pts, cache_ptr->queue_sub->tail_valid_pts, ret);
    } else {
        int i =0;
        PacketQueue* queue = NULL;
        for(i =0; i<cache_ptr->audio_num;i++) {
            if(pkt->stream_index == cache_ptr->queue_all_audio[i].stream_index)
                queue = &cache_ptr->queue_all_audio[i];
        }
        if (queue)
            ret = packet_queue_put_update(queue, pkt);
        if (ret < 0) {
            //log_print("steam index:%d ,discard\n",pkt->stream_index);
           // return ret;
        }
    }

	return 0;
}

static int avpkt_cache_check_can_get(av_packet_cache_t *cache_ptr, int* stream_idx, int want_idx)
{
	int64_t audio_cur_pkt_frame_id = -1;
	int64_t video_cur_pkt_frame_id = -1;
	int64_t sub_cur_pkt_frame_id = -1;
	int64_t small_frame_id = -1;
	play_para_t *player = (play_para_t *)s_avpkt_cache->context;
        float maxlevel = 0;
        SteamType_E low_prio = STEAM_TYPE_NONE;
        SteamType_E steam = STEAM_TYPE_NONE;
        static int debug_cnt = 0;
        if (cache_ptr->has_video) {
            if(player->state.video_bufferlevel > 0.5 && player->state.video_bufferlevel > maxlevel) {
                maxlevel =  player->state.video_bufferlevel;
                low_prio = STEAM_TYPE_VIDEO;
            }
            if (cache_ptr->queue_video->frames_for_seek_forward > 0)
                steam |= STEAM_TYPE_VIDEO;
        }
        if (cache_ptr->has_audio ) {
            if (player->state.audio_bufferlevel > 0.5 && player->state.audio_bufferlevel > maxlevel) {
                maxlevel =  player->state.audio_bufferlevel;
                low_prio = STEAM_TYPE_AUDIO;
            }
            if (cache_ptr->queue_audio->frames_for_seek_forward > 0)
                steam |= STEAM_TYPE_AUDIO;
        }
        
        if (cache_ptr->has_sub)  {
            if (player->state.sub_bufferlevel > 0.5 && player->state.sub_bufferlevel > maxlevel) {
                maxlevel =  player->state.sub_bufferlevel;
                low_prio = STEAM_TYPE_SUB;
            }
            if (cache_ptr->queue_sub->frames_for_seek_forward > 0)
                  steam |= STEAM_TYPE_SUB;
        }
        /*
        log_info("%d,%d,%0.08f, forward:%d,%d,%d\n", steam,low_prio,player->state.sub_bufferlevel,
            cache_ptr->queue_video->frames_for_seek_forward,
            cache_ptr->queue_audio->frames_for_seek_forward,
            cache_ptr->queue_sub->frames_for_seek_forward);*/

	if (cache_ptr->has_video
		&& cache_ptr->queue_video->frames_for_seek_forward > 0) {
		cache_lock(&cache_ptr->queue_video->lock);
		if (cache_ptr->queue_video->cur_pkt == NULL) {
			log_error("%s video Bad Exception\n", __FUNCTION__);
		}
		video_cur_pkt_frame_id = cache_ptr->queue_video->cur_pkt->frame_id;
		cache_unlock(&cache_ptr->queue_video->lock);
                if ((!((steam ^ STEAM_TYPE_VIDEO) && (STEAM_TYPE_VIDEO & low_prio))) || cache_ptr->trickmode == 1) {
                    small_frame_id = video_cur_pkt_frame_id;
                    *stream_idx = cache_ptr->video_index;
                }
	}

	if (cache_ptr->trickmode == 0) {
		if (cache_ptr->has_audio
			&& cache_ptr->queue_audio->frames_for_seek_forward > 0) {
			cache_lock(&cache_ptr->queue_audio->lock);
			if (cache_ptr->queue_audio->cur_pkt == NULL) {
				log_error("%s audio Bad Exception\n", __FUNCTION__);
			}
			audio_cur_pkt_frame_id = cache_ptr->queue_audio->cur_pkt->frame_id;
			cache_unlock(&cache_ptr->queue_audio->lock);
                	if (!((steam ^ STEAM_TYPE_AUDIO) &&(STEAM_TYPE_AUDIO&low_prio))) {
        			if (small_frame_id == -1) {
        				small_frame_id = audio_cur_pkt_frame_id;
        				*stream_idx = cache_ptr->audio_index;
        			} else {
        				if (audio_cur_pkt_frame_id < small_frame_id) {
        				small_frame_id = audio_cur_pkt_frame_id;
        				*stream_idx = cache_ptr->audio_index;
        				}
        			}
                       }
		}

		if (cache_ptr->has_sub
			&& cache_ptr->queue_sub->frames_for_seek_forward > 0) {
			cache_lock(&cache_ptr->queue_sub->lock);
			if (cache_ptr->queue_sub->cur_pkt == NULL) {
				log_error("%s sub Bad Exception\n", __FUNCTION__);
			}
			sub_cur_pkt_frame_id = cache_ptr->queue_sub->cur_pkt->frame_id;
			cache_unlock(&cache_ptr->queue_sub->lock);
                	if (!((steam ^ STEAM_TYPE_SUB) &&(STEAM_TYPE_SUB&low_prio))) {
        			if (small_frame_id == -1) {
        				small_frame_id = sub_cur_pkt_frame_id;
        				*stream_idx = cache_ptr->sub_index;
        			} else {
        				if (sub_cur_pkt_frame_id < small_frame_id) {
        					small_frame_id = sub_cur_pkt_frame_id;
        					*stream_idx = cache_ptr->sub_index;
        				}
        			}
                       }
		}
	}

prior_ret:
	if (*stream_idx < 0 && (cache_ptr->has_audio && (cache_ptr->queue_audio->frames_for_seek_forward > 0) ||
				(cache_ptr->has_video && cache_ptr->queue_video->frames_for_seek_forward > 0)) && (debug_cnt%1000)) {
		debug_cnt++;
		if (cache_ptr->queue_audio)
			log_print("idx:%d,a_nb:%d, acread:%d,steam:%d, low_prio:%d",
					*stream_idx, cache_ptr->queue_audio->nb_packets, cache_ptr->queue_audio->frames_for_seek_forward, steam, low_prio
					);
		if (cache_ptr->queue_video)
			log_print("idx:%d, v_nb:%d, vcanread:%d,steam:%d, low_prio:%d",
					*stream_idx, cache_ptr->queue_video->nb_packets, cache_ptr->queue_video->frames_for_seek_forward, steam, low_prio
					);
	}
        if (want_idx > -1)
            *stream_idx = want_idx;

	return (*stream_idx < 0 ? 0 : 1);
}

int avpkt_cache_queue_check_can_put(PacketQueue *q, int64_t current_ms)
{
	int ret = 0;
	int64_t firstPkt_ms = 0;
	int64_t curPkt_ms = 0;
	int64_t lastPkt_ms = 0;

	cache_lock(&q->lock);

	if (q->frames_backward_level > 0.0) {
		#ifdef DEBUG_CACHE_PUT
		if (s_put_cnt <= 20) {
			if (q->stream_index == s_avpkt_cache->audio_index) {
				log_print("%d aput: seekback:%d, maxseekback:%d\n",
					s_put_cnt, q->frames_for_seek_backward, q->frames_max_seekbackword);

			} else if (q->stream_index == s_avpkt_cache->video_index) {
				log_print("%d vput: seekback:%d, maxseekback:%d\n",
					s_put_cnt, q->frames_for_seek_backward, q->frames_max_seekbackword);
			}
		}
		#endif
		if (q->frames_for_seek_backward > q->frames_max_seekbackword) {
			ret = 1;
		} else {
			ret = 0;
		}
	} else {
		if ((q->frames_for_seek_backward > 1 || q->nb_packets < q->max_packets)
			&& q->frames_for_seek_backward <= q->max_packets) {
			ret = 1;
		} else {
			ret = 0;
		}
	}

	cache_unlock(&q->lock);
	return ret;
}
static void packet_queue_update_param(av_packet_cache_t *cache_ptr)
{
    PacketQueue * queue = NULL;
    int frames = 0;
    int i;
    int debug = 0;
    
    if (cache_ptr->has_video == 1 &&cache_ptr->queue_video->max_packets !=cache_ptr->queue_video->nb_packets) {
        queue = cache_ptr->queue_video;
        cache_lock(&queue->lock);
        if (cache_ptr->queue_video->nb_packets > 0)
            cache_ptr->video_max_packet = cache_ptr->queue_video->nb_packets;
        cache_ptr->queue_video->max_packets = cache_ptr->video_max_packet;
    
        cache_ptr->queue_video->frames_max_seekbackword = (int)(cache_ptr->queue_video->frames_backward_level * cache_ptr->queue_video->max_packets);
        if (cache_ptr->queue_video->frames_max_seekbackword == 0) {
            cache_ptr->queue_video->frames_max_seekbackword = 1;
        }
        cache_ptr->queue_video->frames_max_seekforword = (cache_ptr->queue_video->max_packets - cache_ptr->queue_video->frames_max_seekbackword);
    
        frames = (int)(cache_ptr->queue_video->frames_max_seekforword/5);
        if (cache_ptr->keepframes > frames) {
            cache_ptr->keepframes = frames;
            if (cache_ptr->keepframes < 5) {
                cache_ptr->keepframes = 0;
            }
        }
        cache_unlock(&queue->lock);
        if (debug) {
        log_print("%s cache use mem reach %dB\n", __FUNCTION__, cache_ptr->cur_mem_size);
        log_print("%s modify max_packets,v_max:%d, v_maxback:%d, keepframes:%d\n", __FUNCTION__,
        cache_ptr->queue_video->max_packets, cache_ptr->queue_video->frames_max_seekbackword,
        cache_ptr->keepframes);
        }
    }
        
    //audio
    if (cache_ptr->has_audio == 1) {
        for(i=0; i<cache_ptr->audio_num; i++) {
            queue = &cache_ptr->queue_all_audio[i];
            cache_lock(&queue->lock);
            if (queue->max_packets !=queue->nb_packets) {
                if (queue->nb_packets > 0)
                    cache_ptr->audio_max_packet = queue->nb_packets;
                queue->max_packets = cache_ptr->audio_max_packet;
            
                queue->frames_max_seekbackword = (int)(queue->frames_backward_level * queue->max_packets);
                if (queue->frames_max_seekbackword == 0) {
                    queue->frames_max_seekbackword = 1;
                }
                queue->frames_max_seekforword = (queue->max_packets - queue->frames_max_seekbackword);
            }
            
            cache_unlock(&queue->lock);
        }
        if (debug) 
        log_print("%s modify max_packets, a_max:%d, a_maxback:%d\n", __FUNCTION__,
        cache_ptr->queue_audio->max_packets, cache_ptr->queue_audio->frames_max_seekbackword);
    }
        
    //subtitle
    if (cache_ptr->has_sub == 1) {
        for(i=0; i<cache_ptr->sub_num; i++) {
            queue = &cache_ptr->queue_all_sub[i];
            cache_lock(&queue->lock);
            if (queue->max_packets !=queue->nb_packets) {
                if (queue->nb_packets > 0)
                    cache_ptr->sub_max_packet = queue->nb_packets;
                if (cache_ptr->sub_max_packet < 10) {
                    cache_ptr->sub_max_packet = 10;
                }
                queue->max_packets = cache_ptr->sub_max_packet;

                queue->frames_max_seekbackword = (int)(queue->frames_backward_level * queue->max_packets);
                if (queue->frames_max_seekbackword == 0) {
                    queue->frames_max_seekbackword = 1;
                }
                queue->frames_max_seekforword = (queue->max_packets - queue->frames_max_seekbackword);
            }
            cache_unlock(&queue->lock);
        }
        if (debug)
        log_print("%s modify max_packets, s_max:%d, s_maxback:%d,\n", __FUNCTION__,
        cache_ptr->queue_sub->max_packets, cache_ptr->queue_sub->frames_max_seekbackword);
        }
    }


static void packet_queue_discard_frame(av_packet_cache_t *cache_ptr, PacketQueue *q)
{
    MyAVPacketList *pkt1;
    MyAVPacketList *pkttmp;
    MyAVPacketList *pkt = NULL;
    int64_t free_cnt =0;

    pkt = q->first_pkt;
    cache_lock(&q->lock);

    while (pkt && pkt->frame_id <= cache_ptr->discard_frame_id) {
            free_cnt++;
            pkt = pkt->next;
             /*free  under back level*/
           if (q->frames_backward_level > 0.0) {
                    if ((q->frames_for_seek_backward -free_cnt) <= q->frames_max_seekbackword) {
                        break;
                } else {
                if(q->frames_for_seek_backward - free_cnt < 0)
                    break;
                }
            }
    }
    while (free_cnt > 0) {
            free_cnt--;
            //delete frist_pkt
            pkttmp = q->first_pkt;
            if (q->cur_pkt == NULL) {
                log_error("%s Bad Exception\n", __FUNCTION__);
            }else {
            	if (q->cur_pkt == q->first_pkt) {
            		q->cur_pkt = pkttmp->next;
            	}
            }
            q->first_pkt = pkttmp->next;
            pkttmp->priv = NULL;
            pkttmp->next = NULL;

            q->backwardsize -= pkttmp->pkt.size;

            //calc free mem for this packet
            q->size -= pkttmp->pkt.size;
            {
                if (pkttmp->pkt.side_data_elems > 0) {
                    q->size -= (pkttmp->pkt.side_data_elems * sizeof(pkttmp->pkt.side_data));
                }
            }
            q->size -= sizeof(*pkttmp);
            //end
            q->nb_packets--;
            av_free_packet(&pkttmp->pkt);
            av_free(pkttmp);
            pkttmp = NULL;

            if (avpkt_cache_queue_used(cache_ptr, q))
                q->frames_for_seek_backward--;
            else {
                q->frames_for_seek_forward--;
                q->frames_out++;
            }
            //update queue first pts
            pkttmp = q->first_pkt;
            while(pkttmp != NULL) {
                if (pkttmp->pkt.pts != AV_NOPTS_VALUE) {
                    q->head_valid_pts = pkttmp->pkt.pts;
                    break;
                }
                pkttmp = pkttmp->next;
            }

            pkttmp = NULL;
    }
    cache_unlock(&q->lock);
}
static int avpkt_cache_queue_used(av_packet_cache_t *cache_ptr,PacketQueue *q)
{
    PacketQueue *queue = NULL;
    int ret = 0;
    if (cache_ptr->has_video && cache_ptr->queue_video) {
        ret |= (q == cache_ptr->queue_video);
    }
    if (cache_ptr->has_audio && cache_ptr->queue_audio) {
        ret |= (q == cache_ptr->queue_audio);
    }
    if (cache_ptr->has_sub && cache_ptr->queue_sub) {
        ret |= (q == cache_ptr->queue_sub);
    }
    return ret;
}
static int avpkt_cache_queue_valid_frame_id(PacketQueue *q,int64_t try_frame_id)
{
    MyAVPacketList *pkt = NULL;
    int try_cnt = 0;
    int ret = 0;

    cache_lock(&q->lock);
    pkt = q->first_pkt;
    if (!pkt) {
        cache_unlock(&q->lock);
        return ret;
    }
    if (try_frame_id > q->cur_pkt->frame_id) {
        cache_unlock(&q->lock);
        return ret;
    }
    while (pkt && pkt->frame_id <= try_frame_id) {
            try_cnt++;
            pkt = pkt->next;
    }

    if (q->frames_backward_level > 0.0) {
        if ((q->frames_for_seek_backward -try_cnt) > q->frames_max_seekbackword) {
            ret = 1;
        }
    } else {
        if ((q->frames_for_seek_backward -try_cnt) >= 0) {
            ret = 1;
        }
    }
    if(s_avpkt_cache->discard_disable_cnt % 20000==0){
      //log_print("[%d] ret:%d,cnt:%d, back:%d,maxback:%d",q->steam_type,ret,try_cnt,q->frames_for_seek_backward ,q->frames_max_seekbackword);
    }
    cache_unlock(&q->lock);
    return ret;
}

static int avpkt_cache_valid_frame_id(av_packet_cache_t *cache_ptr,int64_t try_frame_id)
{
    PacketQueue *q = NULL;
    MyAVPacketList *pkt = NULL;
    SteamType_E steam_valid = 0;
    SteamType_E steam_type = 0;
    int try_cnt;
    int ret = 0;
    if (cache_ptr->has_video && cache_ptr->queue_video->frames_in > 0) {
            steam_type |= STEAM_TYPE_VIDEO;
    }
    if (cache_ptr->has_audio && cache_ptr->queue_audio->frames_in > 0) {
            steam_type |= STEAM_TYPE_AUDIO;
    }
    
    if (cache_ptr->has_video && cache_ptr->queue_video->frames_for_seek_backward > 0) {
        ret = avpkt_cache_queue_valid_frame_id(cache_ptr->queue_video, try_frame_id);
        if(ret)
            steam_valid |= STEAM_TYPE_VIDEO;
    }
    if (cache_ptr->has_audio && cache_ptr->queue_audio->frames_for_seek_backward > 0) {
        ret = avpkt_cache_queue_valid_frame_id(cache_ptr->queue_audio, try_frame_id);
        if(ret)
            steam_valid |= STEAM_TYPE_AUDIO;
    }
    return (steam_valid == steam_type);
}
static int avpkt_cache_queue_check_free_mem(av_packet_cache_t *cache_ptr,PacketQueue *q, int64_t try_frame_id)
{
	int ret = 0;
        int try_cnt = 0;
        int free_mem =0;
        int i;

	cache_lock(&q->lock);
	MyAVPacketList *pkt = q->first_pkt;
	while (pkt && pkt->frame_id <= try_frame_id) {
		/*free  under back level*/
		if (avpkt_cache_queue_used(cache_ptr, q)) {
			if (q->frames_backward_level > 0.0) {
				if ((q->frames_for_seek_backward -try_cnt) <= q->frames_max_seekbackword) {
					break;
				} 
                        } else {
				if (q->frames_for_seek_backward - try_cnt < 0)
					break;
			}
		}
                try_cnt++;
                pkt = pkt->next;
	}
	pkt = q->first_pkt;

	for (i = 0; i < try_cnt; i++) {
		free_mem+= pkt->pkt.size;
		pkt = pkt->next;
	}
        if(s_avpkt_cache->discard_disable_cnt % 20000==0){
            //log_print("[%d] ret:%d,cnt:%d,free_mem:%d, %f,  back:%d,maxback:%d",q->steam_type,ret,try_cnt,free_mem,q->frames_backward_level,q->frames_for_seek_backward ,q->frames_max_seekbackword);
        }
	cache_unlock(&q->lock);
	return free_mem;
}
static void avpkt_cache_free_exceeding_mem(av_packet_cache_t *cache_ptr)
{
    int64_t cur_video_frame_id = -1;
    int64_t cur_audio_frame_id = -1;
    int64_t cur_sub_frame_id = -1;
      int may_free_mem =0;
      int canput = 0;
      int i =0;
      int audio_size = 0;
      int video_size = 0;
      int sub_size = 0;
      PacketQueue * queue = NULL;
      for(i =0; i<cache_ptr->audio_num;i++)
          audio_size += cache_ptr->queue_all_audio[i].size;
      for(i =0; i<cache_ptr->sub_num;i++)
          sub_size += cache_ptr->queue_all_sub[i].size;
      if (cache_ptr->queue_video)
            video_size = cache_ptr->queue_video->size;
      cache_ptr->cur_mem_size = audio_size + sub_size + video_size;
      
     if (cache_ptr->cur_mem_size > cache_ptr->max_cache_mem) {
          if (cache_ptr->has_video && cache_ptr->queue_video->frames_in > 0) {
              queue = cache_ptr->queue_video;
              packet_queue_discard_frame(cache_ptr,queue);
    
          }
          if (cache_ptr->has_audio && cache_ptr->queue_audio->frames_in > 0) {
              for(i=0; i<cache_ptr->audio_num; i++)
              {
                    queue = &cache_ptr->queue_all_audio[i];
                    packet_queue_discard_frame(cache_ptr,queue);
              }
          }
          if (cache_ptr->has_sub && cache_ptr->queue_sub->frames_in > 0) {
              for(i=0; i<cache_ptr->sub_num; i++)
              {
                    queue = &cache_ptr->queue_all_sub[i];
                    packet_queue_discard_frame(cache_ptr,queue);
              }
          }
         packet_queue_update_param(cache_ptr);
      }
}

#if 1 
static int avpkt_cache_check_can_put(av_packet_cache_t *cache_ptr, AVPacket* pkt)
{
    int may_free_mem =0;
    int canput = 0;
    int i =0,j = 0;
    int audio_size = 0;
    int video_size = 0;;
    int sub_size = 0;;
    int64_t try_frame_id = cache_ptr->discard_frame_id;
    //int64_t last_frame_id = cache_ptr->discard_frame_id;
    PacketQueue * queue = NULL;
    for(i =0; i<cache_ptr->audio_num;i++)
        audio_size += cache_ptr->queue_all_audio[i].size;
    for(i =0; i<cache_ptr->sub_num;i++)
        sub_size += cache_ptr->queue_all_sub[i].size;
    if (cache_ptr->queue_video)
        video_size = cache_ptr->queue_video->size;
    cache_ptr->cur_mem_size = audio_size + sub_size + video_size;

   if(cache_ptr->cur_mem_size + pkt->size > cache_ptr->max_cache_mem) {
        packet_queue_update_param(cache_ptr);
 
        while (1) {
            try_frame_id++;
            may_free_mem = 0;
            if(avpkt_cache_interrupt(cache_ptr))
                break;
            if (!avpkt_cache_valid_frame_id(cache_ptr, try_frame_id))
                break;
            if (cache_ptr->has_video) {
                queue = cache_ptr->queue_video;
                may_free_mem += avpkt_cache_queue_check_free_mem(cache_ptr, queue, try_frame_id);
            }
            if (cache_ptr->has_audio) {
                for(i=0; i<cache_ptr->audio_num; i++)
                {
                      queue = &cache_ptr->queue_all_audio[i];
                      may_free_mem += avpkt_cache_queue_check_free_mem(cache_ptr, queue, try_frame_id);
                }
            }
            if (cache_ptr->has_sub) {
                for(i=0; i<cache_ptr->sub_num; i++)
                {
                      queue = &cache_ptr->queue_all_sub[i];
                       may_free_mem += avpkt_cache_queue_check_free_mem(cache_ptr, queue, try_frame_id);
                }
            }
            if (may_free_mem >= pkt->size) {
                canput = 1;
                cache_ptr->discard_frame_id = try_frame_id;
                //log_print("discard frame id:%lld",cache_ptr->discard_frame_id);
                break;
            }
            if(s_avpkt_cache->discard_disable_cnt % 2000==0) {
                  //log_print("pkt.size:%d,free_mem:%d,try_frame_id:%lld",pkt->size,may_free_mem,try_frame_id);
             }
        }
    } else {
        canput =1;
    }
    if (canput == 0)
        cache_ptr->discard_disable_cnt++;
   else
        cache_ptr->discard_disable_cnt = 1 ;
    return canput;
}
#else
static int avpkt_cache_check_can_put(av_packet_cache_t *cache_ptr, AVPacket* pkt)
{
	play_para_t *player = (play_para_t *)cache_ptr->context;
	int64_t current_ms = (int64_t)(player->state.current_ms);
	int canput = 0;
	int ret = 0;
	int avpkt_total_mem = 0;
	int frames = 0;


	if (cache_ptr->reach_maxmem_flag == 0) {
		avpkt_total_mem = cache_ptr->queue_audio->size + cache_ptr->queue_video->size + cache_ptr->queue_sub->size;
		if (avpkt_total_mem >= cache_ptr->max_cache_mem) {
			cache_ptr->reach_maxmem_flag = 1;
			if ((cache_ptr->has_audio && cache_ptr->queue_audio->nb_packets < cache_ptr->queue_audio->max_packets)
				|| (cache_ptr->has_video && cache_ptr->queue_video->nb_packets < cache_ptr->queue_video->max_packets)
				|| (cache_ptr->has_sub && cache_ptr->queue_sub->nb_packets < cache_ptr->queue_sub->max_packets)) {
				//video
				if (cache_ptr->has_video == 1) {
					if (cache_ptr->queue_video->nb_packets > 0)
						cache_ptr->video_max_packet = cache_ptr->queue_video->nb_packets;
					cache_ptr->queue_video->max_packets = cache_ptr->video_max_packet;

					cache_ptr->queue_video->frames_max_seekbackword = (int)(cache_ptr->queue_video->frames_backward_level * cache_ptr->queue_video->max_packets);
					if (cache_ptr->queue_video->frames_max_seekbackword == 0) {
						cache_ptr->queue_video->frames_max_seekbackword = 1;
					}
					cache_ptr->queue_video->frames_max_seekforword = (cache_ptr->queue_video->max_packets - cache_ptr->queue_video->frames_max_seekbackword);

					frames = (int)(cache_ptr->queue_video->frames_max_seekforword/5);
					if (cache_ptr->keepframes > frames) {
						cache_ptr->keepframes = frames;
						if (cache_ptr->keepframes < 5) {
							cache_ptr->keepframes = 0;
						}
					}
				}

				//audio
				if (cache_ptr->has_audio == 1) {
					if (cache_ptr->queue_audio->nb_packets > 0)
						cache_ptr->audio_max_packet = cache_ptr->queue_audio->nb_packets;
					cache_ptr->queue_audio->max_packets = cache_ptr->audio_max_packet;

					cache_ptr->queue_audio->frames_max_seekbackword = (int)(cache_ptr->queue_audio->frames_backward_level * cache_ptr->queue_audio->max_packets);
					if (cache_ptr->queue_audio->frames_max_seekbackword == 0) {
						cache_ptr->queue_audio->frames_max_seekbackword = 1;
					}
					cache_ptr->queue_audio->frames_max_seekforword = (cache_ptr->queue_audio->max_packets - cache_ptr->queue_audio->frames_max_seekbackword);
				}

				//subtitle
				if (cache_ptr->has_sub == 1) {
					if (cache_ptr->queue_sub->nb_packets > 0)
						cache_ptr->sub_max_packet = cache_ptr->queue_sub->nb_packets;
					if (cache_ptr->sub_max_packet < 10) {
						cache_ptr->sub_max_packet = 10;
					}
					cache_ptr->queue_sub->max_packets = cache_ptr->sub_max_packet;

					cache_ptr->queue_sub->frames_max_seekbackword = (int)(cache_ptr->queue_sub->frames_backward_level * cache_ptr->queue_sub->max_packets);
					if (cache_ptr->queue_sub->frames_max_seekbackword == 0) {
						cache_ptr->queue_sub->frames_max_seekbackword = 1;
					}
					cache_ptr->queue_sub->frames_max_seekforword = (cache_ptr->queue_sub->max_packets - cache_ptr->queue_sub->frames_max_seekbackword);
				}

				log_print("%s cache use mem reach %dB, high, should not malloc again\n", __FUNCTION__, avpkt_total_mem);
				log_print("%s modify max_packets, a_max:%d, a_maxback:%d, v_max:%d, v_maxback:%d,s_max:%d, s_maxback:%d, keepframes:%d\n", __FUNCTION__,
					cache_ptr->queue_audio->max_packets, cache_ptr->queue_audio->frames_max_seekbackword,
					cache_ptr->queue_video->max_packets, cache_ptr->queue_video->frames_max_seekbackword,
					cache_ptr->queue_sub->max_packets, cache_ptr->queue_sub->frames_max_seekbackword,
					cache_ptr->keepframes);
				return 0;
			}
		} else {
			return 1;
		}
	}

	canput = 1;
	if (cache_ptr->has_audio && cache_ptr->queue_audio->frames_in > 0) {
		canput &= avpkt_cache_queue_check_can_put(&cache_ptr->queue_audio, current_ms);
		if (canput == 0) {
			//drop this put, wait for next time put, but mark this queue and higher get priority
			cache_ptr->queue_audio->prior = 1;
		}
	}

	if (canput == 1 && cache_ptr->has_video && cache_ptr->queue_video->frames_in > 0) {
		canput &= avpkt_cache_queue_check_can_put(&cache_ptr->queue_video, current_ms);
		if (canput == 0) {
			cache_ptr->queue_video->prior = 1;
		}
	}

	if (canput == 1 && cache_ptr->has_sub && cache_ptr->queue_sub->frames_in > 0) {
		//TODO
		canput &= avpkt_cache_queue_check_can_put(&cache_ptr->queue_sub, current_ms);
		if (canput == 0) {
			cache_ptr->queue_sub->prior = 1;
		}
	}

	return canput;
}
#endif
static int avpkt_cache_do_after_bitchange(PacketQueue *q, AVPacket *pkt)
{
	int ret = -1;

	if (q == NULL || q->first_pkt == NULL || q->last_pkt == NULL || q->cur_pkt == NULL) {
		return -1;
	}

	cache_lock(&q->lock);

	if (q->last_pkt->pts != AV_NOPTS_VALUE && pkt->pts != AV_NOPTS_VALUE && pkt->pts <= q->last_pkt->pts &&
		q->last_pkt->pts - pkt->pts < 15 * PTS_90K) { // sometimes pts large than one ts segment
		//avoid normal packet has some pkt pts out of order
                int pts_diff = q->last_pkt->pts - pkt->pts;
		if (pts_diff > 27000) {
			ret = -1;
		} else {
			if (s_avpkt_cache->video_bitrate_change_flag == 1) {
				ret = -1;
			} else {
				ret = 0;
			}
		}
		if (pts_diff < 45000 && s_avpkt_cache->video_bitrate_change_flag == 1 && pkt->flags == 1) {
			ret = 0;
		}
		if (s_avpkt_cache->video_bitrate_change_flag == 1 || pts_diff > 7200 ) {
			log_print("[pktpts]-0-ret=%d,head:0x%llx, cur:0x%llx, tail:0x%llx, pkt.pts:0x%llx, diff=%d, stream_index=%d, flag=0x%x\n",
			ret,q->first_pkt->pts, q->cur_pkt->pts, q->last_pkt->pts, pkt->pts, pts_diff, pkt->stream_index, pkt->flags);
		}
	} else if (q->last_pkt->pts == AV_NOPTS_VALUE ||  pkt->pts == AV_NOPTS_VALUE){
	   //log_print("[pktpts] head:0x%llx, cur:0x%llx, tail:0x%llx, pkt->pts:0x%llx, pkt.stream_index=%d,pkt_dts:0x%llx\n",
		//			  q->first_pkt->pts, q->cur_pkt->pts, q->last_pkt->pts, pkt->pts, pkt->stream_index, pkt->dts);

		ret = -2;
	} else {
		ret = 0;
	}

	cache_unlock(&q->lock);

	return ret;
}


/*
small_flag: 1-use small, 0-use big, -1-use the small abs(diff)
*/
static int avpkt_cache_queue_seek_bypts(PacketQueue *q, int64_t pts, int small_flag)
{
	/*1.seek by pts*/
	int ret = -1;
	int left = 0;
	int right = 0;
	int findkey_ok = 0; //-1 left, 0 fail, 1 right
	int seek_sucess =0;
	int64_t queueHeadPktPts = -1;
	int64_t queueTailPktPts = -1;
	int64_t queueCurPktPts = -1;
	int64_t seekPts = -1;
	MyAVPacketList *mypktl = NULL;
	MyAVPacketList *mypktl_1 = NULL;
	MyAVPacketList *mypktl_2 = NULL;
	MyAVPacketList *mypktl_left = NULL;
	MyAVPacketList *mypktl_right = NULL;

	if (q == NULL || q->first_pkt == NULL || q->last_pkt == NULL || q->cur_pkt == NULL) {
		return -1;
	}

	cache_lock(&q->lock);

	queueHeadPktPts = q->first_pkt->offset_pts;
	queueCurPktPts  = q->cur_pkt->offset_pts;
	queueTailPktPts = q->last_pkt->offset_pts;
	seekPts = pts;

	log_print("[offsetpts]head:0x%llx, cur:0x%llx, tail:0x%llx, seek:0x%llx\n",
		queueHeadPktPts, queueCurPktPts, queueTailPktPts, seekPts);
	log_print("[pktpts]head:0x%llx, cur:0x%llx, tail:0x%llx\n",
		q->first_pkt->pts, q->cur_pkt->pts, q->last_pkt->pts);

	mypktl = NULL;
	mypktl_1 = NULL;
	mypktl_2 = NULL;
	if (seekPts <= queueCurPktPts) {
		mypktl = q->first_pkt;
		for (; mypktl != NULL && mypktl->frame_id <= q->cur_pkt->frame_id; mypktl = mypktl->next) {
			if (mypktl_1 == NULL) {
				mypktl_1 = mypktl;
				mypktl_2 = mypktl;
			} else {
				mypktl_1 = mypktl_2;
				mypktl_2 = mypktl;
			}

			if (mypktl_2->offset_pts >= seekPts) {
				seek_sucess = 1;
				break;
			}
		}

		if (seek_sucess == 1) {
			if (mypktl_2 && mypktl_2->offset_pts > seekPts && small_flag == 1) {
				mypktl_2 = mypktl_1;
			}
		}
	}
	else if (seekPts <= queueTailPktPts) {
		mypktl = q->cur_pkt;
		for (; mypktl != NULL && mypktl->frame_id <= q->last_pkt->frame_id; mypktl = mypktl->next) {
			if (mypktl_1 == NULL) {
				mypktl_1 = mypktl;
				mypktl_2 = mypktl;
			} else {
				mypktl_1 = mypktl_2;
				mypktl_2 = mypktl;
			}

			if (mypktl_2->offset_pts >= seekPts) {
				seek_sucess = 1;
				break;
			}
		}

		if (seek_sucess == 1) {
			if (mypktl_2 && mypktl_2->offset_pts > seekPts && small_flag == 1) {
				mypktl_2 = mypktl_1;
			}
		}
	}
	else {
		log_print("%s out of cache\n", __FUNCTION__);
	}

	if (seek_sucess == 1) {
		/*
		*  find keyframe in area [-60, 60], special skill
		*/
		if (q->seek_bykeyframe == 1) {
			q->seek_bykeyframe = 0;
			mypktl_left = mypktl_2;
			for (; left < SEEK_KEY_FRAME_MAX_NUM; left++) {
				if (mypktl_left == NULL) {
					left = SEEK_KEY_FRAME_MAX_NUM;
					log_print("find key by left fails\n");
					break;
				} else {
					if (mypktl_left->pkt.flags & AV_PKT_FLAG_KEY) {
						break;
					}
					mypktl_left = mypktl_left->priv;
				}
			}

			mypktl_right = mypktl_2;
			for (; right < SEEK_KEY_FRAME_MAX_NUM; right++) {
				if (mypktl_right == NULL) {
					right = SEEK_KEY_FRAME_MAX_NUM;
					log_print("find key by right fails\n");
					break;
				} else {
					if (mypktl_right->pkt.flags & AV_PKT_FLAG_KEY) {
						break;
					}
					mypktl_right = mypktl_right->next;
				}
			}

			if (left < SEEK_KEY_FRAME_MAX_NUM) {
				if (right < SEEK_KEY_FRAME_MAX_NUM) {
					if (left < right) {
						findkey_ok = -1;
					} else {
						findkey_ok = 1;
					}
				} else {
					findkey_ok = -1;
				}
			} else if (right < SEEK_KEY_FRAME_MAX_NUM) {
				findkey_ok = 1;
			} else {
				findkey_ok = 0;
			}

			if (findkey_ok == 1) {
				mypktl_2 = mypktl_right;
			} else if (findkey_ok == -1) {
				mypktl_2 = mypktl_left;
			} else {
				seek_sucess = 0;
			}
		}

		if (mypktl_2 && mypktl_2->frame_id < q->cur_pkt->frame_id) {
			mypktl = mypktl_2;
			for (; mypktl != NULL && mypktl->frame_id <= q->cur_pkt->frame_id; mypktl = mypktl->next) {
				if (mypktl->used == 1) {
					mypktl->used = 0;
					q->frames_out--;
					q->frames_for_seek_forward++;
					q->frames_for_seek_backward--;
				}
			}
		} else {
			mypktl = q->cur_pkt;
			for (; mypktl != NULL && mypktl_2 != NULL && mypktl->frame_id < mypktl_2->frame_id; mypktl = mypktl->next) {
				if (mypktl->used == 0) {
					mypktl->used = 1;
					q->frames_out++;
					q->frames_for_seek_forward--;
					q->frames_for_seek_backward++;
				}
			}
		}

		q->cur_pkt = mypktl_2;
		q->cur_valid_pts = mypktl_2->pts;

		#ifdef DEBUG_CACHE_SEEK
		int i = 0;
		mypktl = q->cur_pkt;
		log_print("%s seek succeeds [%s:%lld, off_pts:0x%llx, pts:0x%llx]\n",
			__FUNCTION__,
			(q->stream_index == s_avpkt_cache->audio_index ? "a":"v"),
			mypktl->frame_id, mypktl->offset_pts, mypktl->pts);

		for (; i < 16; i++) {
			if (mypktl != NULL) {
				log_print("[%s:%lld]key:%d, used:%d, offset_pts:0x%llx, pts:0x%llx, back:%d, forw:%d\n",
					(q->stream_index == s_avpkt_cache->audio_index ? "a":"v"),
					mypktl->frame_id,
					(mypktl->pkt.flags & AV_PKT_FLAG_KEY),
					mypktl->used, mypktl->offset_pts, mypktl->pts,
					q->frames_for_seek_backward, q->frames_for_seek_forward);
				mypktl = mypktl->next;
			}
		}
		#endif

		ret = 0;
	}

	cache_unlock(&q->lock);

	return ret;
}

/*
*  seek by frame_id , refer in video frame_id and vpts
*  if video frame_id=100, find audio frame_id arounds 100([40,160]),
*  search audio frame_id which apts <= vpts,
*  if search ok, return seek ok;
*  else point to the 99th pkt
*  TODO://
*/
static int avpkt_cache_queue_seek_bypts_frameid(PacketQueue *q,
	int64_t pts, int64_t frame_id, int small_flag)
{
	int ret = -1;
	MyAVPacketList *mypktl = NULL;
	MyAVPacketList *mypktl_1 = NULL; //left
	MyAVPacketList *mypktl_2 = NULL; //right
	MyAVPacketList *mypktl_tmp = NULL;
	int cnt = 0;

	cache_lock(&q->lock);

	log_print("curpkt:%lld, pts_offset:0x%llx, firstpts:0x%llx, curpts:0x%llx, lastpts:0x%llx, ref_pts:0x%llx, frame_id:%lld\n",
		q->cur_pkt->frame_id, q->cur_pkt->offset_pts, q->first_pkt->pts, q->cur_pkt->pts,q->last_pkt->pts, pts, frame_id);

	if (frame_id < q->first_pkt->frame_id
		|| frame_id > q->last_pkt->frame_id) {
		log_print("[out: %d [%d-%d]\n", frame_id, q->first_pkt->frame_id, q->last_pkt->frame_id);
		cache_unlock(&q->lock);
		return ret;
	} else if (frame_id < q->cur_pkt->frame_id) {
		mypktl = q->first_pkt;
	} else if (frame_id < q->last_pkt->frame_id) { //< q->last_pkt->frame_id
		mypktl = q->cur_pkt;
	} else {
		//DO NOTHING
	}

	for (; mypktl != NULL && mypktl->frame_id < frame_id; mypktl = mypktl->next) {
		//find pkt which id > frame_id
	}

	mypktl_tmp = mypktl;
	log_print("curpkt:%lld [%lld:%lld:%lld], pts_offset:0x%llx, pts:0x%llx, ref_pts:0x%llx\n",
		mypktl->frame_id,
		q->first_pkt->frame_id,
		q->cur_pkt->frame_id,
		q->last_pkt->frame_id,
		mypktl->offset_pts, mypktl->pts, pts);

	//search to the right pkt
	if (mypktl->pts == pts
		|| pts == AV_NOPTS_VALUE) {
		mypktl_1 = mypktl;
		mypktl_2 = mypktl;
	} else {
		if (mypktl->pts == AV_NOPTS_VALUE) {
			cnt = 0;
			for (; cnt < SEEK_KEY_FRAME_MAX_NUM; cnt++) {
				if (mypktl == NULL) {
					cnt = SEEK_KEY_FRAME_MAX_NUM;
					break;
				}
				if (mypktl != NULL && mypktl->pts != AV_NOPTS_VALUE) {
					break;
				}

				mypktl = mypktl->next;
			}

			if (cnt == SEEK_KEY_FRAME_MAX_NUM) {
				cnt = 0;
				for (; cnt < SEEK_KEY_FRAME_MAX_NUM; cnt++) {
					if (mypktl == NULL) {
						cnt = SEEK_KEY_FRAME_MAX_NUM;
						break;
					}

					if (mypktl != NULL && mypktl->pts != AV_NOPTS_VALUE) {
						break;
					}

					mypktl = mypktl->priv;
				}
			}
		}

		if (cnt < SEEK_KEY_FRAME_MAX_NUM) {
			cnt = 0;
			log_print("curpkt:%lld [%lld:%lld:%lld], pts_offset:0x%llx, pts:0x%llx, ref_pts:0x%llx\n",
				mypktl->frame_id,
				q->first_pkt->frame_id,
				q->cur_pkt->frame_id,
				q->last_pkt->frame_id,
				mypktl->offset_pts, mypktl->pts, pts);
			if (mypktl->pts < pts) {
				//find a bigger pts pkt
				for (; mypktl != NULL &&
					mypktl->frame_id <= q->last_pkt->frame_id &&
					cnt < SEEK_KEY_FRAME_MAX_NUM;
					mypktl = mypktl->next) {
					if (mypktl->pts != AV_NOPTS_VALUE) {
						if (mypktl_1 == NULL) {
							mypktl_1 = mypktl;
							mypktl_2 = mypktl;
						} else {
							mypktl_1 = mypktl_2;
							mypktl_2 = mypktl;
						}

						if (mypktl->pts > pts
							&& abs(mypktl->pts - pts) <= (int64_t)((PTS_DISCONTINUE_MS  * 90) / q->timebase)) {
							log_print("cnt:%d, 0x%llx, 0x%llx, diff_pts:0x%llx, cmppts:0x%llx\n",
								cnt, mypktl_1->pts, mypktl_2->pts, abs(mypktl->pts - pts), 
								(int64_t)((PTS_DISCONTINUE_MS * 90) /q->timebase));
							break;
						}
					}

					cnt++;
				}

  				if (mypktl == NULL) {
					cnt = SEEK_KEY_FRAME_MAX_NUM;
				}
			} else {
				//find a smaller pts pkt
				for (; mypktl != NULL &&
					mypktl->frame_id >= q->first_pkt->frame_id &&
					cnt < SEEK_KEY_FRAME_MAX_NUM;
					mypktl = mypktl->priv) {
					if (mypktl->pts != AV_NOPTS_VALUE) {
						if (mypktl_1 == NULL) {
							mypktl_1 = mypktl;
							mypktl_2 = mypktl;
						} else {
							mypktl_2 = mypktl_1;
							mypktl_1 = mypktl;
						}

						if (mypktl->pts < pts
							&& abs(mypktl->pts - pts) <= (int64_t)((PTS_DISCONTINUE_MS * 90 ) / q->timebase)) {
							break;
						}
					}

					cnt++;
				}

				if (mypktl == NULL) {
					cnt = SEEK_KEY_FRAME_MAX_NUM;
				}
			}
		}
	}

	if (cnt < SEEK_KEY_FRAME_MAX_NUM) {
		if (small_flag == 1) {
			mypktl_2 = mypktl_1;
		}
	} else {
		mypktl_2 = mypktl_tmp;
	}

	if (mypktl_2->frame_id <= q->cur_pkt->frame_id) {
		mypktl = mypktl_2;
		for (; mypktl != NULL && mypktl->frame_id <= q->cur_pkt->frame_id;
			mypktl = mypktl->next) {
			if (mypktl->used == 1) {
				mypktl->used = 0;
				q->frames_out--;
				q->frames_for_seek_forward++;
				q->frames_for_seek_backward--;
			}
		}
	} else {
		mypktl = q->cur_pkt;
		for (; mypktl != NULL && mypktl->frame_id < mypktl_2->frame_id;
			mypktl = mypktl->next) {
			if (mypktl->used == 0) {
				mypktl->used = 1;
				q->frames_out++;
				q->frames_for_seek_forward--;
				q->frames_for_seek_backward++;
			}
		}
	}

	q->cur_pkt = mypktl_2;
	q->cur_valid_pts = mypktl_2->pts;
	ret = 0;

	#ifdef DEBUG_CACHE_SEEK
	int i = 0;
	mypktl = q->cur_pkt;
	log_print("%s cnt:%d, seek succeeds [%s:%lld, off_pts:0x%llx, pts:0x%llx]\n",
		__FUNCTION__, cnt,
		(q->stream_index == s_avpkt_cache->audio_index ? "a":"v"),
		mypktl->frame_id, mypktl->offset_pts, mypktl->pts);

	for (; i < 16; i++) {
		if (mypktl != NULL) {
			log_print("[%s:%lld]key:%d, used:%d, offset_pts:0x%llx, pts:0x%llx, back:%d, forw:%d\n",
				(q->stream_index == s_avpkt_cache->audio_index ? "a":"v"),
				mypktl->frame_id,
				(mypktl->pkt.flags & AV_PKT_FLAG_KEY),
				mypktl->used, mypktl->offset_pts, mypktl->pts,
				q->frames_for_seek_backward, q->frames_for_seek_forward);
			mypktl = mypktl->next;
		}
	}
	#endif

	cache_unlock(&q->lock);
	return ret;
}

static int avpkt_cache_queue_seektoend(PacketQueue *q)
{
	if (q->first_pkt == NULL || q->last_pkt == NULL || q->cur_pkt == NULL) {
		return -1;
	}

	cache_lock(&q->lock);
	MyAVPacketList *mypktl = NULL;
	mypktl = q->cur_pkt;
	for (; mypktl != NULL && mypktl->frame_id <= q->last_pkt->frame_id; mypktl = mypktl->next) {
		if (mypktl->used == 0) {
			mypktl->used = 1;
			q->frames_for_seek_backward++;
			q->frames_for_seek_forward--;
			q->frames_out++;
		}

		if (mypktl->pkt.pts != AV_NOPTS_VALUE) {
			q->cur_valid_pts = mypktl->pkt.pts;
		}
	}

	q->cur_pkt = q->last_pkt;
	cache_unlock(&q->lock);
	return 0;
}

static int avpkt_cache_queue_seek(PacketQueue *q, int64_t seekTimeMs)
{
	/*1.seek by pts*/
	int ret = -1;
	int64_t seekPts = -1;
	int64_t seek_offset_ms = 0;
	int64_t seek_offset_pts = -1;

	if (q->first_pkt == NULL || q->last_pkt == NULL || q->cur_pkt == NULL) {
		return -1;
	}

	seek_offset_ms = seekTimeMs - (s_avpkt_cache->starttime_ms + s_avpkt_cache->seek_discontinue_current_ms);
	seekPts = (int64_t)((seek_offset_ms * 90) / q->timebase);

	if (q->stream_index == s_avpkt_cache->video_index) {
		ret = avpkt_cache_queue_seek_bypts(q, seekPts, 0);//use big one
	} else if (q->stream_index == s_avpkt_cache->audio_index) {
		ret = avpkt_cache_queue_seek_bypts(q, seekPts, 1);//use small one
	} else if (q->stream_index == s_avpkt_cache->sub_index) {
		ret = avpkt_cache_queue_seek_bypts(q, seekPts, 1);//use small one
	}

	return ret;
}

/*
call this function when can seektime be searched in both audio and video
*/
static int avpkt_cache_seek(av_packet_cache_t *cache_ptr, int64_t seekTimeMSec, int master_idx)
{
	int ret = -1;
	int seek_suc = 0;
        SteamType_E master_type = STEAM_TYPE_NONE;
        PacketQueue *master_q = NULL;
        PacketQueue *slave_q = NULL;
        if (cache_ptr->has_video) {
                if (master_idx == cache_ptr->queue_video->stream_index) {
                    master_type = STEAM_TYPE_VIDEO;
                    master_q = cache_ptr->queue_video;
                }
        }
        if (cache_ptr->has_audio) {
            if (master_idx == cache_ptr->queue_audio->stream_index) {
                master_type = STEAM_TYPE_AUDIO;
                master_q = cache_ptr->queue_audio;
            } else {
                slave_q = cache_ptr->queue_audio;
            }

        }
	/*trust keyframe pts*/
	if (master_type != STEAM_TYPE_NONE) {
		log_print("%s: %s master seek start\n", __FUNCTION__,master_type==STEAM_TYPE_VIDEO?"video":"audio");
		ret = avpkt_cache_queue_seek(master_q, seekTimeMSec);
		if (ret == -1) {
			log_print("master seek fails");
			return ret;
		} else {
			log_print("master seek succeeds, cread:%d, headPts:0x%llx, tailPts:0x%llx, seekPktPts:0x%llx",
				master_q->frames_for_seek_forward, master_q->head_valid_pts,
				master_q->tail_valid_pts, master_q->cur_valid_pts);
			seek_suc = 1;
		}
	} else
            slave_q = NULL;
    
	if (slave_q) {
		log_print("%s: %s slave seek start, seekms:%lld\n", __FUNCTION__, master_type==STEAM_TYPE_VIDEO?"audio":"video", seekTimeMSec);
		if (seek_suc == 1) {
			log_print("%s: slave seek refer master pts_offset:0x%llx, pts:0x%llx\n",
				__FUNCTION__,
				slave_q->cur_pkt->offset_pts,
				slave_q->cur_pkt->pts);
			#if 1
			ret = avpkt_cache_queue_seek_bypts_frameid(slave_q,
				master_q->cur_pkt->pts,
				master_q->cur_pkt->frame_id, 1);
			#else
			ret = avpkt_cache_queue_seek_bypts(&cache_ptr->queue_audio,
				cache_ptr->queue_video->cur_pkt->offset_pts, 1);//use small one
			int64_t diff_pts = 0;
			diff_pts = cache_ptr->queue_audio->cur_pkt->pts - cache_ptr->queue_video->cur_pkt->pts;
			if (ret == -1 || abs(diff_pts) > (int64_t)((PTS_DISCONTINUE_MS * 90) /cache_ptr->queue_video->timebase)) {
				ret = avpkt_cache_queue_seek_bypts_frameid(&cache_ptr->queue_audio,
					cache_ptr->queue_video->cur_pkt->pts,
					cache_ptr->queue_video->cur_pkt->frame_id, 1);
			}
			#endif
		} else {
			ret = avpkt_cache_queue_seek(slave_q, seekTimeMSec);
		}

		if (ret == -1) {
			log_print("slave seek fails");
			if (cache_ptr->trickmode == 1) {
				ret = 0;
				//seek to the end pkt
				log_print("status:ff/fb seek to the end pkt");
				avpkt_cache_queue_seektoend(slave_q);
			}
			seek_suc = 0;
		} else {
			log_print("slave seek succeeds, cread:%d, headPts:0x%llx, tailPts:0x%llx, seekPktPts:0x%llx",
				slave_q->frames_for_seek_forward, slave_q->head_valid_pts,
				slave_q->tail_valid_pts, slave_q->cur_valid_pts);
			seek_suc = 1;
		}
	}

	ret = (seek_suc == 1 ? 0 : -1);
	return ret;
}

static int avpkt_cache_avsync_mode(av_packet_cache_t *cache_ptr) {
	if (NULL == cache_ptr)
		return 0;

	play_para_t *player = (play_para_t *)cache_ptr->context;
	if (NULL == player)
		return 0;

	log_print("[%s]vfmt:%d, vct:%d\n", AVSYNC_TAG, player->vstream_info.video_format, player->vstream_info.video_codec_type);
	if (cache_ptr->local_play == 1
		&& player->vstream_info.has_video == 1
		&& player->vstream_info.video_index != -1
		&& player->vstream_info.video_format == VFORMAT_VC1
		&& player->vstream_info.video_codec_type == VIDEO_DEC_FORMAT_WMV3) {
		if (cache_ptr->seek_by_keyframe == 0) {
			log_print("[%s]force wmv3 seek by keyframe\n", AVSYNC_TAG);
			cache_ptr->seek_by_keyframe = 1;
			cache_ptr->queue_video->seek_bykeyframe = cache_ptr->seek_by_keyframe;
			cache_ptr->seek_by_keyframe_maxnum = am_getconfig_int_def("libplayer.cache.syncframes", 240);
		}
		return 1;
	}

	return 0;
}

int avpkt_cache_reset_thread_t(av_packet_cache_t *cache_ptr)
{
    int ret = 0;
    pthread_t       tid;
    pthread_attr_t pthread_attr;
    play_para_t *player = (play_para_t *)cache_ptr->context;
    player->player_cache_reset_status = 1;
    pthread_attr_init(&pthread_attr);
    pthread_attr_setstacksize(&pthread_attr, 0);   //default stack size maybe better
    log_print("open avpkt_cache_reset_thread_t\n");
    ret = amthreadpool_pthread_create(&tid, &pthread_attr, (void*)&avpkt_cache_reset_thread, (void*)cache_ptr);
    if (ret != 0) {
        log_print("creat player thread failed !\n");
        return ret;
    }
    log_print("[avpkt_cache_reset_thread_t:%d]creat cache thread success,tid=%lu\n", __LINE__, tid);
    player->cache_reset_tid = tid;
    pthread_attr_destroy(&pthread_attr);
    return PLAYER_SUCCESS;
}
void *avpkt_cache_reset_thread(void *arg)
{
    int64_t newstarttime_ms = 0;
    PacketQueue * queue = NULL;
    int retry = 0;
    int i = 0;
    av_packet_cache_t *cache_ptr = (av_packet_cache_t *)arg;
    play_para_t *player = (play_para_t *)cache_ptr->context;
    if (cache_ptr->reading == 1) {
        avpkt_cache_interrupt_read(cache_ptr, 0);
        while (retry < 1000) {
            if (cache_ptr->reading == 0)
                break;
            retry++;
            amthreadpool_thread_usleep(2000);
        }
        avpkt_cache_uninterrupt_read(cache_ptr);
    }
    player->player_cache_read_frame_end = 1;
    if(cache_ptr->has_audio)
    {
        log_print("%s release queue audio\n", __FUNCTION__);
        for(i=0; i<cache_ptr->audio_num; i++)
        {
              queue = &cache_ptr->queue_all_audio[i];
              packet_queue_flush(queue);
        }

        cache_ptr->audio_count = 0;
        cache_ptr->audio_size = 0;
        cache_ptr->audio_cachems = 0;
    }
    if(cache_ptr->has_video)
    {
        log_print("%s release queue video\n", __FUNCTION__);
        packet_queue_flush(cache_ptr->queue_video);
        cache_ptr->video_count = 0;
        cache_ptr->video_size = 0;
        cache_ptr->video_cachems = 0;
    }
    if(cache_ptr->has_sub)
    {
        log_print("%s release queue sub\n", __FUNCTION__);
        for(i=0; i<cache_ptr->sub_num; i++)
            {
                queue = &cache_ptr->queue_all_sub[i];
                packet_queue_flush(queue);
            }

        cache_ptr->sub_count = 0;
        cache_ptr->sub_size = 0;
        cache_ptr->sub_cachems = 0;
    }
    avpkt_cache_update_player_param(cache_ptr);
    if (cache_ptr->has_video == 1 && cache_ptr->video_index != -1) {
        cache_ptr->seek_by_keyframe = player->playctrl_info.seek_keyframe;
        cache_ptr->queue_video->seek_bykeyframe = cache_ptr->seek_by_keyframe;
        cache_ptr->seek_by_keyframe_maxnum = am_getconfig_int_def("libplayer.cache.seekkeynum", SEEK_KEY_FRAME_MAX_NUM);
        log_print("[%s]seek_by_keyframe:%d\n", AVSYNC_TAG, cache_ptr->seek_by_keyframe);
        if (avpkt_cache_avsync_mode(cache_ptr) == 1) {
            cache_ptr->avsync_mode = am_getconfig_int_def("libplayer.cache.avsync", 1);
            if (cache_ptr->avsync_mode == 1) {
                cache_ptr->avsyncing = 1;
                log_print("[%s]avsyncing==1\n", AVSYNC_TAG);
            }
        }
    }
    cache_ptr->state = 1;
    #ifdef DEBUG_CACHE_GET
    s_get_cnt = 0;
    #endif
    #ifdef DEBUG_CACHE_PUT
    s_put_cnt = 0;
    #endif
    #ifdef DEBUG_CACHE_IN_OUT
    s_cpu_get_cnt = 0;
    s_pkt_used_error = 0;
    #endif
    player->player_cache_reset_status = 2;
    log_print("%s, end reset\n", __FUNCTION__);
    return 0;
}
static int avpkt_cache_reset(av_packet_cache_t *cache_ptr)
{
	play_para_t *player = (play_para_t *)cache_ptr->context;
	int64_t newstarttime_ms = 0;
	int retry = 0;
        PacketQueue * queue = NULL;
        int i = 0;
	if (cache_ptr->reading == 1) {
		avpkt_cache_interrupt_read(cache_ptr, 0);
		while (retry < 1000) {
			if (cache_ptr->reading == 0)
				break;
			retry++;
			amthreadpool_thread_usleep(2000);
		}
		avpkt_cache_uninterrupt_read(cache_ptr);
	}

    if(cache_ptr->has_audio)
    {
    	log_print("%s release queue audio\n", __FUNCTION__);
        for(i=0; i<cache_ptr->audio_num; i++)
        {
              queue = &cache_ptr->queue_all_audio[i];
              packet_queue_flush(queue);
        }
        cache_ptr->audio_count = 0;
        cache_ptr->audio_size = 0;
        cache_ptr->audio_cachems = 0;
    }

    if(cache_ptr->has_video)
    {
    	log_print("%s release queue video\n", __FUNCTION__);
        packet_queue_flush(cache_ptr->queue_video);
		cache_ptr->video_count = 0;
        cache_ptr->video_size = 0;
		cache_ptr->video_cachems = 0;
    }

    if(cache_ptr->has_sub)
    {
		log_print("%s release queue sub\n", __FUNCTION__);
                for(i=0; i<cache_ptr->sub_num; i++)
                {
                    queue = &cache_ptr->queue_all_sub[i];
                    packet_queue_flush(queue);
                }
		cache_ptr->sub_count = 0;
		cache_ptr->sub_size = 0;
		cache_ptr->sub_cachems = 0;
    }

	avpkt_cache_update_player_param(cache_ptr);
	if (cache_ptr->has_video == 1 && cache_ptr->video_index != -1) {
		cache_ptr->seek_by_keyframe = player->playctrl_info.seek_keyframe;
		cache_ptr->queue_video->seek_bykeyframe = cache_ptr->seek_by_keyframe;
		cache_ptr->seek_by_keyframe_maxnum = am_getconfig_int_def("libplayer.cache.seekkeynum", SEEK_KEY_FRAME_MAX_NUM);
		log_print("[%s]seek_by_keyframe:%d\n", AVSYNC_TAG, cache_ptr->seek_by_keyframe);
		if (avpkt_cache_avsync_mode(cache_ptr) == 1) {
			cache_ptr->avsync_mode = am_getconfig_int_def("libplayer.cache.avsync", 1);
			if (cache_ptr->avsync_mode == 1) {
				cache_ptr->avsyncing = 1;
				log_print("[%s]avsyncing==1\n", AVSYNC_TAG);
			}
		}
	}

	cache_ptr->state = 1;
	#ifdef DEBUG_CACHE_GET
	s_get_cnt = 0;
	#endif

	#ifdef DEBUG_CACHE_PUT
	s_put_cnt = 0;
	#endif

	#ifdef DEBUG_CACHE_IN_OUT
	s_cpu_get_cnt = 0;
	s_pkt_used_error = 0;
	#endif

    return 0;
}

static int avpkt_cache_release(av_packet_cache_t *cache_ptr)
{
    PacketQueue * queue = NULL;
    int i = 0;
	log_print("release total mem");
    if(cache_ptr->has_audio)
    {
		log_print("%s release queue audio, mem size:%d\n", __FUNCTION__, cache_ptr->queue_audio->size);
        for(i=0; i<cache_ptr->audio_num; i++)
        {
              queue = &cache_ptr->queue_all_audio[i];
              packet_queue_destroy(queue);
        }
        cache_ptr->audio_count = cache_ptr->queue_audio->nb_packets;
        cache_ptr->audio_size = cache_ptr->queue_audio->size;
        cache_ptr->audio_cachems = cache_ptr->queue_audio->bak_cache_pts;
    }

    if(cache_ptr->has_video)
    {
		log_print("%s release queue video, mem size:%d\n", __FUNCTION__, cache_ptr->queue_video->size);
        packet_queue_destroy(cache_ptr->queue_video);
        cache_ptr->video_count = cache_ptr->queue_video->nb_packets;
        cache_ptr->video_size = cache_ptr->queue_video->size;
        cache_ptr->video_cachems = cache_ptr->queue_video->bak_cache_pts;
    }

	if(cache_ptr->has_sub)
	{
		log_print("%s release queue sub, mem size:%d\n", __FUNCTION__, cache_ptr->queue_sub->size);
               for(i=0; i<cache_ptr->sub_num; i++)
                {
                    queue = &cache_ptr->queue_all_sub[i];
                    packet_queue_destroy(queue);
                }
		cache_ptr->sub_count = cache_ptr->queue_sub->nb_packets;
		cache_ptr->sub_size = cache_ptr->queue_sub->size;
		cache_ptr->sub_cachems = cache_ptr->queue_sub->bak_cache_pts;
	}

	cache_ptr->first_apts = -1;
	cache_ptr->first_vpts = -1;
	cache_ptr->first_spts = -1;
	cache_ptr->context = NULL;
	cache_ptr->error = 0;
	cache_ptr->read_frames = 0;
	cache_ptr->max_cache_mem = 0;
	cache_ptr->reach_maxmem_flag = 0;
	cache_ptr->seek_discontinue_current_ms = -1;

	if (cache_ptr->enable_keepframes == 1) {
		cache_ptr->keeframesstate = 0;
		cache_ptr->leftframes = -1;
		cache_ptr->keepframes = am_getconfig_int_def("libplayer.cache.keepframes", 125);
		cache_ptr->enterkeepframems = am_getconfig_int_def("libplayer.cache.enterkeepms", 5000);//5s
	}

	if (cache_ptr->bigpkt.data != NULL) {
		av_free(cache_ptr->bigpkt.data);
		cache_ptr->bigpkt.data = NULL;
	}

	memset(cache_ptr, 0, sizeof(*cache_ptr));

	#ifdef DEBUG_CACHE_PUT
	s_put_cnt = 0;
	#endif

	#ifdef DEBUG_CACHE_GET
	s_get_cnt = 0;
	#endif

	#ifdef DEBUG_CACHE_IN_OUT
	s_cpu_get_cnt = 0;
	s_pkt_used_error = 0;
	#endif

    return 0;
}

int avpkt_cache_checkvlevel(av_packet_cache_t *cache_ptr, float level) {
	play_para_t *player = (play_para_t *)s_avpkt_cache->context;

	if (player->vstream_info.has_video && player->state.video_bufferlevel >= level) {
		return 1;
	}

	return 0;
}

static int avpkt_cache_update_bufed_time(void) {
	int frame_dur_pts = 0;
	int64_t total_cache_ms = 0;
	int64_t cache_ms_bypts = 0;
	int64_t bak_calc_pts = 0;
	int64_t discontinue_pts = 0;
	int bufed_time = 0;

	frame_dur_pts = s_avpkt_cache->queue_video->frame_dur_pts;
	total_cache_ms = (int64_t)(
		(s_avpkt_cache->queue_video->frames_in * frame_dur_pts * s_avpkt_cache->queue_video->timebase) / 90);
	bak_calc_pts = s_avpkt_cache->queue_video->bak_cache_pts;
	discontinue_pts = s_avpkt_cache->queue_video->discontinue_pts;

	if (s_avpkt_cache->queue_video->pts_discontinue_flag == 1) {
		bak_calc_pts -= discontinue_pts;
	}

	if (bak_calc_pts > 0) {
		cache_ms_bypts = (int64_t)((bak_calc_pts * 90) / s_avpkt_cache->queue_video->timebase);
		total_cache_ms = cache_ms_bypts;
	}

	bufed_time = (int)(total_cache_ms/1000);

	return bufed_time;
}

static int64_t avpkt_cache_reduce_keepframes_ms(PacketQueue *q)
{
	int64_t keep_ms = 0;

	if (s_avpkt_cache->keepframes <= 0) {
		return 0;
	}

	//default
	int frame_dur_pts = q->frame_dur_pts;
	if (frame_dur_pts == 0)
		frame_dur_pts = 3600;

	keep_ms = (int64_t)(((s_avpkt_cache->keepframes * frame_dur_pts * q->timebase) / 90));

	//calc 125fps duration pts
	//cache_ms -= (q->lastPkt_playtime_pts - q->curPkt_playtime_pts);
	//end

	return keep_ms;
}

static int64_t packet_calc_cachetime_by_player_current_ms(PacketQueue *q, int64_t player_current_ms, int stream_idx, int debug)
{
	int64_t cache_ms = 0;
	int64_t total_cache_ms = 0;
	int64_t playtime_ms = 0;
	int64_t queue_left_ms = 0;
	int64_t left_frames = 0;
	int frame_dur_pts = 0;
	int64_t pts_last = q->tail_valid_pts;
	int64_t pts_first = q->first_keyframe_pts;
	int64_t diff_pts = -1;
	int64_t cache_ms_bypts = 0;
        PacketQueue * queue;
        int i=0;

	play_para_t *player = (play_para_t *)s_avpkt_cache->context;

	cache_lock(&q->lock);

	if (q->frame_dur_pts <= 0 && q->frames_in) {
		frame_dur_pts = (int)(q->bak_cache_pts/q->frames_in);
		if ((frame_dur_pts = duration_pts_invalid_check(frame_dur_pts)) <= 0) {
			frame_dur_pts = RATE_25_FPS;//default 25fps
		}
		q->frame_dur_pts = frame_dur_pts;
	}

	frame_dur_pts = q->frame_dur_pts;
	left_frames = (q->frames_in - q->frames_out);

	if (debug) {
		if (q->stream_index == s_avpkt_cache->audio_index) {
			log_print("\n[a] idx:%d,nb_packets:%d, max_seekback:%d, frame_dur:%d, in:%lld, out:%lld, left:%lld, backnum:%d, forwnum:%d\n",
				q->stream_index, q->nb_packets, q->frames_max_seekbackword, q->frame_dur_pts, q->frames_in, q->frames_out, left_frames,
				q->frames_for_seek_backward, q->frames_for_seek_forward);
		} else if (q->stream_index == s_avpkt_cache->video_index) {
			log_print("\n[v] idx:%d,nb_packets:%d, max_seekback:%d, frame_dur:%d, in:%lld, out:%lld, left:%lld, backnum:%d, forwnum:%d\n",
				q->stream_index, q->nb_packets, q->frames_max_seekbackword, q->frame_dur_pts, q->frames_in, q->frames_out, left_frames,
				q->frames_for_seek_backward, q->frames_for_seek_forward);
			if (q->frames_in > 0 && s_avpkt_cache->audio_index != -1 && s_avpkt_cache->queue_audio->frames_in > 0) {
				log_print("\n[a] idx:%d,nb_packets:%d, max_seekback:%d, in:%lld, out:%lld, backnum:%d, forwnum:%d\n",
						s_avpkt_cache->queue_audio->stream_index,
						s_avpkt_cache->queue_audio->nb_packets,
						s_avpkt_cache->queue_audio->frames_max_seekbackword,
						s_avpkt_cache->queue_audio->frames_in,
						s_avpkt_cache->queue_audio->frames_out,
						s_avpkt_cache->queue_audio->frames_for_seek_backward,
						s_avpkt_cache->queue_audio->frames_for_seek_forward);
				log_print("[v]discard fid:%lld,first_fid:%lld,cur_fid:%lld,lst_fid:%lld\n",s_avpkt_cache->discard_frame_id,q->first_pkt->frame_id,q->cur_pkt->frame_id,q->last_pkt->frame_id);
                		log_print("[a]first_fid:%lld,cur_fid:%lld,last_fid:%lld\n", s_avpkt_cache->queue_audio->first_pkt->frame_id,
                                    s_avpkt_cache->queue_audio->cur_pkt->frame_id,s_avpkt_cache->queue_audio->last_pkt->frame_id);
			}
		} else if (q->stream_index == s_avpkt_cache->sub_index) {
			log_print("\n[s]nb_packets:%d, max_seekback:%d, frame_dur:%d, in:%lld, out:%lld, left:%lld, backnum:%d, forwnum:%d\n",
				q->nb_packets, q->frames_max_seekbackword, q->frame_dur_pts, q->frames_in, q->frames_out, left_frames,
				q->frames_for_seek_backward, q->frames_for_seek_forward);
		}
	}

	{
		//reduce seek discontinue TODO
		playtime_ms = (player_current_ms - (s_avpkt_cache->starttime_ms
			+ s_avpkt_cache->seek_discontinue_current_ms + s_avpkt_cache->discontinue_current_ms));

		//if frame_dur normal value, refer pts
		if (q->frame_dur_pts > 0) {
			int64_t bak_calc_pts = q->bak_cache_pts;
			int64_t discontinue_pts = 0;

			total_cache_ms = (int64_t)((q->frames_in * frame_dur_pts * q->timebase) / 90);
			cache_ms = total_cache_ms - playtime_ms;
			if (q->pts_discontinue_flag == 1) {
				bak_calc_pts += frame_dur_pts;
				discontinue_pts = q->discontinue_pts;
			}

			cache_ms_bypts = ((int64_t)((bak_calc_pts * q->timebase) / 90) - playtime_ms);
			if (debug) {
				log_print("bak_calc_pts:0x%llx, 0x%llx", bak_calc_pts, discontinue_pts);
				log_print("cache_ms_by_duration:%lld, cache_ms_by_pts:%lld", cache_ms, cache_ms_bypts);
			}
			cache_ms = cache_ms_bypts;
		}
		//end

		/*
		*  dirty code, should be suitable to audio too
		*  keepframes should use time unit[eg:5s]
		*/
		if (s_avpkt_cache->enable_keepframes == 1) {
			if (s_avpkt_cache->has_video == 1
				&& stream_idx == s_avpkt_cache->video_index
				&& left_frames >= s_avpkt_cache->keepframes) {
				cache_ms -= avpkt_cache_reduce_keepframes_ms(q);
			}
		}

		//compare pts
		if (player->state.status == PLAYER_BUFFERING) {
			cache_ms = 0;
		}
		//end

		if (debug) {
			log_print("mem:%d,current_ms:%lld, starttime_ms:%lld, discont_ms:%lld, seek_discont_ms:%lld",
                                s_avpkt_cache->cur_mem_size,
				player_current_ms, s_avpkt_cache->starttime_ms,
				s_avpkt_cache->discontinue_current_ms,
				s_avpkt_cache->seek_discontinue_current_ms);
			log_print("duration:in:%lld, out:%lld, frame_dur_pts:%d, total_cache_ms:%lld, playtime_ms:%lld, cache_ms:%lld",
				q->frames_in, q->frames_out, frame_dur_pts, total_cache_ms, playtime_ms, cache_ms);

                    if (s_avpkt_cache->has_audio) {
                        for(i=0; i<s_avpkt_cache->audio_num; i++)
                        {
                            queue = &s_avpkt_cache->queue_all_audio[i];
                            if (queue != s_avpkt_cache->queue_audio && queue->frames_in > 0) {
                                log_print("\n[a] unusd idx:%d,nb_packets:%d, max_seekback:%d, in:%lld, out:%lld, backnum:%d, forwnum:%d\n",
                                                queue->stream_index,
                                                queue->nb_packets,
                                                queue->frames_max_seekbackword,
                                                queue->frames_in,
                                                queue->frames_out,
                                                queue->frames_for_seek_backward,
                                                queue->frames_for_seek_forward);
                                log_print("[a]unusd first_fid:%lld,cur_fid:%lld,last_fid:%lld\n", queue->first_pkt->frame_id,queue->cur_pkt->frame_id,queue->last_pkt->frame_id);
                            }
                        }
                    }
                    if (s_avpkt_cache->has_sub) {
                        for(i=0; i<s_avpkt_cache->sub_num; i++)
                        {
                            queue = &s_avpkt_cache->queue_all_sub[i];
                            if (queue->frames_in > 0) {
                                log_print("\n[s] idx:%d,nb_packets:%d, max_seekback:%d, in:%lld, out:%lld, backnum:%d, forwnum:%d\n",
                                                queue->stream_index,
                                                queue->nb_packets,
                                                queue->frames_max_seekbackword,
                                                queue->frames_in,
                                                queue->frames_out,
                                                queue->frames_for_seek_backward,
                                                queue->frames_for_seek_forward);
                                log_print("[s] first_fid:%lld,cur_fid:%lld,last_fid:%lld\n", queue->first_pkt->frame_id,queue->cur_pkt->frame_id,queue->last_pkt->frame_id);
                            }
                        }
                    }
		}
	}

	if (cache_ms < 0)
		cache_ms = 0;
	cache_unlock(&q->lock);

	return cache_ms;
}

static int get_codec_cachetime(play_para_t *player)
{
	int avdelayms = 0;
	int adelayms = 0;
	int vdelayms = 0;

	if (player->vstream_info.has_video && get_video_codec(player)) {
        codec_get_video_cur_delay_ms(get_video_codec(player), &vdelayms);
        avdelayms = vdelayms;
    }
    if (player->astream_info.has_audio && get_audio_codec(player)) {
        codec_get_audio_cur_delay_ms(get_audio_codec(player), &adelayms);
        avdelayms = adelayms;
    }
    if (vdelayms >= 0 && adelayms >= 0) {
        avdelayms = MIN(vdelayms, adelayms);
    }

	return avdelayms;
}

/*
	refer in player current_ms
*/
int64_t avpkt_cache_getcache_time_by_streamindex(play_para_t *player, int stream_idx)
{
	int ret = 0;
	int64_t cache_ms = 0;
	int64_t audio_cache_ms = 0;

	av_packet_cache_t *cache_ptr = s_avpkt_cache;

	if (cache_ptr->state != 2) {
		log_print("cache state:%d\n", cache_ptr->state);
		return 0;
	}

	int64_t current_ms = (int64_t)player->state.current_ms;
	int64_t diff_ms = 0;

	{
		s_avpkt_cache->currenttime_ms = current_ms;
		diff_ms = s_avpkt_cache->currenttime_ms - s_avpkt_cache->last_currenttime_ms;
		if (abs(diff_ms) >= CURRENT_TIME_MS_DISCONTINUE) {
			diff_ms -= MSG_UPDATE_STATE_DURATION_MS;
			if (s_avpkt_cache->seek_discontinue_current_ms == -1) {
				if (s_avpkt_cache->last_currenttime_ms == s_avpkt_cache->starttime_ms) {
					s_avpkt_cache->seek_discontinue_current_ms = diff_ms;
				} else {
					s_avpkt_cache->seek_discontinue_current_ms = 0;
					s_avpkt_cache->discontinue_current_ms += diff_ms;
				}
			} else {
				s_avpkt_cache->discontinue_current_ms += diff_ms;
			}

			log_print("current time discontinue: %lld - > %lld", s_avpkt_cache->last_currenttime_ms, current_ms);
		}
		s_avpkt_cache->last_currenttime_ms = current_ms;
	}

	if (stream_idx == cache_ptr->audio_index) {
		if (cache_ptr->audio_count > 0 && cache_ptr->queue_audio->frames_in) {
			cache_ms = packet_calc_cachetime_by_player_current_ms(cache_ptr->queue_audio, current_ms, stream_idx, 1);
		}
		log_print("audio cache_ms:%lld\n", cache_ms);
	} else if (stream_idx == cache_ptr->video_index) {
		if (cache_ptr->video_count > 0) {
			cache_ms = packet_calc_cachetime_by_player_current_ms(cache_ptr->queue_video, current_ms, stream_idx, 1);
		}

		#ifdef DEBUG_CACHE_SUB
		if (cache_ptr->has_sub && cache_ptr->queue_sub->frames_in > 0) {
			packet_calc_cachetime_by_player_current_ms(cache_ptr->queue_sub, current_ms, stream_idx, 1);
		}
		#endif

		#ifdef DEBUG_CACHE_video
		if (cache_ptr->has_audio == 1) {
			log_print("\n[a]nb_packets:%d, max_packets:%d, frame_dur:%d, in:%lld, out:%lld, left:%lld, backnum:%d, forwnum:%d\n",
				cache_ptr->queue_audio->nb_packets, cache_ptr->queue_audio->max_packets,
				cache_ptr->queue_audio->frame_dur_pts, cache_ptr->queue_audio->frames_in,
				cache_ptr->queue_audio->frames_out, (cache_ptr->queue_audio->frames_in - cache_ptr->queue_audio->frames_out),
				cache_ptr->queue_audio->frames_for_seek_backward, cache_ptr->queue_audio->frames_for_seek_forward);
		}

		if (cache_ptr->has_sub == 1) {
			log_print("\n[s]nb_packets:%d, max_packets:%d, frame_dur:%d, in:%lld, out:%lld, left:%lld, backnum:%d, forwnum:%d\n",
				cache_ptr->queue_sub->nb_packets, cache_ptr->queue_sub->max_packets,
				cache_ptr->queue_sub->frame_dur_pts, cache_ptr->queue_sub->frames_in,
				cache_ptr->queue_sub->frames_out, (cache_ptr->queue_sub->frames_in - cache_ptr->queue_sub->frames_out),
				cache_ptr->queue_sub->frames_for_seek_backward, cache_ptr->queue_sub->frames_for_seek_forward);
		}
		#endif

		if (cache_ptr->queue_video->frame_dur_pts == 0) {
			if (cache_ptr->audio_count > 0 && cache_ptr->queue_audio->frames_in) {
				audio_cache_ms = packet_calc_cachetime_by_player_current_ms(cache_ptr->queue_audio, current_ms, stream_idx, 1);
			}

			log_print("video cache_ms:%lld, audio cache_ms:%lld\n", cache_ms, audio_cache_ms);
			cache_ms = audio_cache_ms;
		} else {
			#ifdef DEBUG_CACHE_AUDIO
			if (cache_ptr->audio_count > 0) {
				audio_cache_ms = packet_calc_cachetime_by_player_current_ms(cache_ptr->queue_audio, current_ms, stream_idx, 1);
			}
			log_print("[debug]video cache_ms:%lld, audio cache_ms:%lld\n", cache_ms, audio_cache_ms);
			#else
			log_print("video cache_ms:%lld\n", cache_ms);
			#endif
		}
	} else if (stream_idx == cache_ptr->sub_index) {
		if (cache_ptr->video_count > 0&& cache_ptr->queue_sub->frames_in) {
			cache_ms = packet_calc_cachetime_by_player_current_ms(cache_ptr->queue_sub, current_ms, stream_idx, 1);
		}
		log_print("sub cache_ms:%lld\n", cache_ms);
	} else {

	}

	/*
	* ref audio/video pts
	* if avcachepts_diff is small ,use audio cache_ms;
	* if video no pts, use audio cache_ms
	*/
	return cache_ms;
}

/*
	refer in player current_ms; the same as avpkt_cache_getcache_time_by_streamindex
	for player buffering mechanism
*/
int64_t avpkt_cache_getcache_time(play_para_t *player, int stream_idx)
{
	int ret = 0;
	int64_t cache_ms = 0;
	av_packet_cache_t *cache_ptr = s_avpkt_cache;

	if (cache_ptr->state != 2) {
		//log_print("cache state:%d\n", cache_ptr->state);
		return 0;
	}

	if ((cache_ptr->queue_audio && cache_ptr->queue_audio->bak_cache_pts <= 0) &&
		(cache_ptr->queue_video && cache_ptr->queue_video->bak_cache_pts <=0)) {
		return 0;
	}

	int64_t current_ms = (int64_t)player->state.current_ms;
	int64_t diff_ms = 0;

	{
		s_avpkt_cache->currenttime_ms = current_ms;
		diff_ms = s_avpkt_cache->currenttime_ms - s_avpkt_cache->last_currenttime_ms;
		if (abs(diff_ms) >= CURRENT_TIME_MS_DISCONTINUE) {
			s_avpkt_cache->discontinue_current_ms += diff_ms;
			//log_print("current time discontinue: %lld - > %lld", s_avpkt_cache->last_currenttime_ms, current_ms);
		}
		s_avpkt_cache->last_currenttime_ms = current_ms;
	}

	if (stream_idx == cache_ptr->audio_index) {
		if (cache_ptr->audio_count > 0) {
			cache_ms = packet_calc_cachetime_by_player_current_ms(cache_ptr->queue_audio, current_ms, stream_idx, 0);
		}
		//log_print("audio cache_ms:%lld\n", cache_ms);
	} else if (stream_idx == cache_ptr->video_index) {
		if (cache_ptr->video_count > 0) {
			cache_ms = packet_calc_cachetime_by_player_current_ms(cache_ptr->queue_video, current_ms, stream_idx, 0);
		}
		//log_print("video cache_ms:%lld\n", cache_ms);
	} else if (stream_idx == cache_ptr->sub_index) {
		if (cache_ptr->video_count > 0) {
			cache_ms = packet_calc_cachetime_by_player_current_ms(cache_ptr->queue_sub, current_ms, stream_idx, 0);
		}
		//log_print("sub cache_ms:%lld\n", cache_ms);
	} else {

	}

	return cache_ms;
}


//recv stop,seek,start,pause,resume
int avpkt_cache_set_cmd(AVPacket_Cache_E cmd)
{
	int ret = 0;
	int retry = 20;
	while (retry > 0) {
		if (s_avpkt_cache->state == 0) {
			log_print("%s:%d not inited, return\n", __FUNCTION__, __LINE__);
			amthreadpool_thread_usleep(5*1000);
			retry--;
		} else {
			break;
		}
	}

	if (retry == 0)
		return -1;

	play_para_t *player = (play_para_t *)s_avpkt_cache->context;
	s_avpkt_cache->cmd = cmd;
	if (s_avpkt_cache->cmd == CACHE_CMD_STOP) {
		s_avpkt_cache->state = 0;
	} else if (cmd == CACHE_CMD_START) {
		s_avpkt_cache->state = 2;
	} else if (cmd == CACHE_CMD_SEARCH_START){
		s_avpkt_cache->state = 1;
		if (s_avpkt_cache->trickmode == 1 && player->playctrl_info.f_step == 0) {
			s_avpkt_cache->trickmode = 0;
		}
	} else if (cmd == CACHE_CMD_SEARCH_OK) {
		s_avpkt_cache->trickmode = 0;
		s_avpkt_cache->fffb_out_frames = am_getconfig_int_def("libplayer.cache.fffbframes", 61);
		s_avpkt_cache->state = 2;
	} else if (cmd == CACHE_CMD_SEEK_OUT_OF_CACHE) {
		s_avpkt_cache->state = 1;
	} else if (cmd == CACHE_CMD_SEEK_IN_CACHE) {
		s_avpkt_cache->state = 1;
	} else if (cmd == CACHE_CMD_RESET) {
		s_avpkt_cache->state = 1;
		avpkt_cache_reset(s_avpkt_cache);
	} else if (cmd == CACHE_CMD_RESET_OK) {
		s_avpkt_cache->trickmode = 0;
		s_avpkt_cache->fffb_out_frames = am_getconfig_int_def("libplayer.cache.fffbframes", 61);
		s_avpkt_cache->state = 2;
		if (player->seek_async) {
			if (player->cache_reset_tid) {
				ret = amthreadpool_pthread_join(player->cache_reset_tid, NULL);
				player->cache_reset_tid = 0;
				log_print("player->cache_reset_tid=%lu\n", player->cache_reset_tid);
			}
		}
	} else if (cmd == CACHE_CMD_FFFB) {
		s_avpkt_cache->state = 1;
		avpkt_cache_reset(s_avpkt_cache);
		s_avpkt_cache->trickmode = 1;
		s_avpkt_cache->seek_by_keyframe = 0;//i frame stream, no need set key
		s_avpkt_cache->fffb_out_frames = am_getconfig_int_def("libplayer.cache.fffbframes", 61);
	} else if (cmd == CACHE_CMD_FFFB_OK) {
		s_avpkt_cache->fffb_out_frames = am_getconfig_int_def("libplayer.cache.fffbframes", 61);
		s_avpkt_cache->state = 2;
	} else if (cmd == CACHE_CMD_SWITCH_SUB) {
		s_avpkt_cache->state = 1;
		avpkt_cache_switch_sub(s_avpkt_cache);
		s_avpkt_cache->state = 2;
	} else if (cmd == CACHE_CMD_SWITCH_AUDIO) {
		s_avpkt_cache->state = 1;
		ret = avpkt_cache_switch_audio(s_avpkt_cache, player->astream_info.audio_index);
		if (ret == EC_OK) {
			s_avpkt_cache->state = 2;
		}
	}else if (cmd == CACHE_CMD_SET_KEEPFRAME){
		s_avpkt_cache->keepframes = 0;
	}

	log_print("%s cmd:%d, state:%d, vin:%lld, out:%lld, forward:%d, backward:%d, fbfr:%d, tm:%d\n",
		__FUNCTION__, s_avpkt_cache->cmd, s_avpkt_cache->state,
		s_avpkt_cache->queue_video?s_avpkt_cache->queue_video->frames_in:0, 
		s_avpkt_cache->queue_video?s_avpkt_cache->queue_video->frames_out:0,
		s_avpkt_cache->queue_video?s_avpkt_cache->queue_video->frames_for_seek_forward:0,
		s_avpkt_cache->queue_video?s_avpkt_cache->queue_video->frames_for_seek_backward:0,
		s_avpkt_cache->fffb_out_frames,
		s_avpkt_cache->trickmode);

	return ret;
}

static int64_t avpkt_cache_queue_search(PacketQueue *q, int64_t seekTimeMs)
{
	/*method1: trust pts*/
	int64_t queueHeadPktPlaytimePts = -1;
	int64_t queueTailPktPlaytimePts = -1;
	int64_t seek_offset_ms = 0;
	int64_t seekPts = -1;

	if (q->first_pkt == NULL || q->last_pkt == NULL || q->nb_packets <= 0) {
		return -1;
	}

	cache_lock(&q->lock);
	seek_offset_ms = seekTimeMs - (s_avpkt_cache->starttime_ms +
		s_avpkt_cache->seek_discontinue_current_ms);
	seekPts = (int64_t)((seek_offset_ms * 90) /q->timebase);

	queueHeadPktPlaytimePts = q->first_pkt->offset_pts;
	queueTailPktPlaytimePts = q->last_pkt->offset_pts;

	log_print("%s queueHeadPktPlaytimePts:0x%llx, queueTailPktPlaytimePts:0x%llx, seekPts:0x%llx",
		__FUNCTION__, queueHeadPktPlaytimePts, queueTailPktPlaytimePts, seekPts);

	if (seekPts <= queueTailPktPlaytimePts && seekPts >= queueHeadPktPlaytimePts) {
		//do nothing
	} else {
		seekPts = -1;
	}

	cache_unlock(&q->lock);

	return seekPts;
}
static int  avpkt_cache_interrupt(av_packet_cache_t *cache_ptr)
{
    return cache_ptr->interrupt;
}

/*
blackout: -1 do nothing about blackout
           0 no blackout
           1 blackout
*/
static int avpkt_cache_interrupt_read(av_packet_cache_t *cache_ptr, int blackout)
{
	if (cache_ptr == NULL) {
		return 0;
	}
        cache_ptr->interrupt = 1;
	play_para_t *player = (play_para_t *)cache_ptr->context;

	player->playctrl_info.ignore_ffmpeg_errors = 1;
	player->playctrl_info.temp_interrupt_ffmpeg = 1;
	if (blackout != -1) {
		set_black_policy(blackout);
	}

	ffmpeg_interrupt_light(player->thread_mgt.pthread_id);

	return 0;
}

static int avpkt_cache_uninterrupt_read(av_packet_cache_t * cache_ptr)
{
	if (cache_ptr == NULL) {
		return 0;
	}
        cache_ptr->interrupt = 0;
	play_para_t *player = (play_para_t *)cache_ptr->context;
	if (player->playctrl_info.temp_interrupt_ffmpeg) {
		player->playctrl_info.temp_interrupt_ffmpeg = 0;
		log_print("ffmpeg_uninterrupt tmped by avpkt cache!\n");
		ffmpeg_uninterrupt_light(player->thread_mgt.pthread_id);
		player->playctrl_info.ignore_ffmpeg_errors = 0;
	}

	return 0;
}

/*
*  check whether audio/video/sub index is changed
*  if anyone of them is changed, return 1;
*  then need to reset cache queue,which equal seek fail in cache
*/
static int avpkt_cache_check_streaminfo_status(av_packet_cache_t *cache_ptr)
{
	int changed = 0;
	play_para_t *player = (play_para_t *)cache_ptr->context;

	if (player->astream_info.has_audio == 1
		&& s_avpkt_cache->has_audio
		&& s_avpkt_cache->audio_index != player->astream_info.audio_index) {
		log_print("[avpkt_cache]aidx: %d -> %d\n",
			s_avpkt_cache->audio_index, player->astream_info.audio_index);
		s_avpkt_cache->audio_index = player->astream_info.audio_index;
		changed = 1;
	}

	if (player->vstream_info.has_video == 1
		&& s_avpkt_cache->has_video
		&& s_avpkt_cache->video_index != player->vstream_info.video_index) {
		log_print("[avpkt_cache]vidx: %d -> %d\n",
			s_avpkt_cache->video_index, player->vstream_info.video_index);
		s_avpkt_cache->video_index = player->vstream_info.video_index;
		changed = 1;
	}

	/*if (player->sstream_info.has_sub == 1
		&& s_avpkt_cache->has_sub
		&& s_avpkt_cache->sub_index != player->sstream_info.sub_index) {
		log_print("[avpkt_cache]sidx: %d -> %d\n",
			s_avpkt_cache->sub_index, player->sstream_info.sub_index);
		s_avpkt_cache->sub_index = player->sstream_info.sub_index;
		changed = 1;
	}*/

	return changed;
}

/*
*  just switch index,and read from current position,
*  do not seek to the current playtime
*/
static int avpkt_cache_switch_sub(av_packet_cache_t *cache_ptr)
{
	play_para_t *player = (play_para_t *)cache_ptr->context;
	int sub_cnt = 0;
	MyAVPacketList *mypktl = NULL;
	MyAVPacketList *mypktl_1 = NULL;

	log_print("%s: subidx %d -> %d\n", __FUNCTION__, cache_ptr->sub_index, player->sstream_info.sub_index);
	//reset queue subtitle
	if (cache_ptr->has_sub
		&& cache_ptr->sub_index != -1
		&& player->sstream_info.sub_index != -1
		&& cache_ptr->sub_index != player->sstream_info.sub_index) {
		#if 1
		packet_queue_reset(cache_ptr->queue_sub, cache_ptr->sub_index);
		#else
		mypktl = cache_ptr->queue_sub->cur_pkt;
		for (;mypktl != NULL &&
			mypktl->frame_id > cache_ptr->queue_sub->first_pkt->frame_id && sub_cnt < SSTREAM_MAX_NUM;
			mypktl = mypktl->priv) {
			if (mypktl->pkt.stream_index == player->sstream_info.sub_index) {
				sub_cnt++;
			}
		}

		//repoint sub to
		mypktl_1 = mypktl;
		for (; mypktl != NULL && mypktl->frame_id <= cache_ptr->queue_sub->cur_pkt->frame_id; mypktl = mypktl->next) {
			if (mypktl->used == 1) {
				mypktl->used = 0;
				cache_ptr->queue_sub->frames_out--;
				cache_ptr->queue_sub->frames_for_seek_forward++;
				cache_ptr->queue_sub->frames_for_seek_backward--;
			}
		}
		cache_ptr->queue_sub->cur_pkt = mypktl_1;
		cache_ptr->queue_sub->cur_valid_pts = mypktl_1->pts;
		#endif
		cache_ptr->sub_index = player->sstream_info.sub_index;
	}
	//end

	return 0;
}
static int avpkt_cache_check_reset(play_para_t *player)
{
	int live_reset = 0;
	int streaminfo_changed = 0;
	if (player->start_param != NULL) {
		log_print("%s:is_livemode:%d, seekTimeSec:%lld\n", __FUNCTION__, player->start_param->is_livemode, s_avpkt_cache->seekTimeMs );
		if (player->start_param->is_livemode == 1 && s_avpkt_cache->seekTimeMs == 0) {
			live_reset = 1;
			log_print("%s:hls live play reset\n", __FUNCTION__);
		}
	}

	streaminfo_changed = avpkt_cache_check_streaminfo_status(s_avpkt_cache);
	if (s_avpkt_cache->enable_seek_in_cache == 0
		|| live_reset == 1
		|| streaminfo_changed == 1) {
		log_print("seek out of cache ");
		avpkt_cache_set_cmd(CACHE_CMD_SEEK_OUT_OF_CACHE);
		avpkt_cache_reset(s_avpkt_cache);
		return 1;
	}
    return 0;
}
static int avpkt_cache_switch_audio(av_packet_cache_t *cache_ptr, int stream_index)
{
	int ret = -1;
        int i = 0;
        PacketQueue *last_q =NULL;
        PacketQueue *q = NULL;
    	MyAVPacketList *mypktl = NULL;
        last_q = s_avpkt_cache->queue_audio;
        cache_lock(&last_q->lock);
        mypktl = last_q->first_pkt;
        for (; mypktl != NULL && mypktl->used == 1; mypktl = mypktl->next) {
                mypktl->used = 0;
                last_q->frames_out--;
                last_q->frames_for_seek_forward++;
        }
        last_q->frames_for_seek_backward = 0;
        last_q->cur_pkt = last_q->first_pkt;
        last_q->frames_for_seek_backward = 0;
        for (i=0; i < s_avpkt_cache->audio_num; i++)
        {
            q = &s_avpkt_cache->queue_all_audio[i];
            if(q->stream_index == stream_index) {
                s_avpkt_cache->queue_audio = q;
                s_avpkt_cache->audio_index = stream_index;
            }
        }
        /*reset keep frame*/
        cache_ptr->keeframesstate = 0;
        cache_unlock(&last_q->lock);
	return ret;
}

int avpkt_cache_search(play_para_t *player, int64_t seekTimeSec)
{
	int ret = 0;
	int cache_kick = 0;
	int64_t SeekPts = -1;
        int switch_audio = 0;
        int index = -1;
        switch_audio = (s_avpkt_cache->cmd == CACHE_CMD_SWITCH_AUDIO);

	if (player == NULL || seekTimeSec < 0) {
		log_print("[%s]invalid param \n", __FUNCTION__);
		return -1;
	}

	if (s_avpkt_cache->state == 0) {
		log_print("[%s]state 0 \n", __FUNCTION__);
		return -1;
	}

	avpkt_cache_set_cmd(CACHE_CMD_SEARCH_START);
        player->player_cache_reset_status = 0;
        player->player_cache_read_frame_end = 0;

	/* +[SE] [BUG][IPTV-576][jiwei.sun] fix thread_join caused dead-lock issue */
	player->cache_reset_tid = 0;
	s_avpkt_cache->seekTimeMs = seekTimeSec*1000;
        if (!switch_audio && avpkt_cache_check_reset(player))
            return -1;
       if (s_avpkt_cache->has_audio == 1 && switch_audio) {
                s_avpkt_cache->seek_by_keyframe = player->playctrl_info.seek_keyframe;
                s_avpkt_cache->queue_audio->seek_bykeyframe = s_avpkt_cache->seek_by_keyframe;
                s_avpkt_cache->seek_by_keyframe_maxnum = am_getconfig_int_def("libplayer.cache.seekkeynum", SEEK_KEY_FRAME_MAX_NUM);
                log_print("[%s]audio seek_bykeyframe:%d\n", AVSYNC_TAG, s_avpkt_cache->queue_audio->seek_bykeyframe);
        } else if (s_avpkt_cache->has_video == 1 && s_avpkt_cache->video_index != -1) {
		s_avpkt_cache->seek_by_keyframe = player->playctrl_info.seek_keyframe;
		s_avpkt_cache->queue_video->seek_bykeyframe = s_avpkt_cache->seek_by_keyframe;
		s_avpkt_cache->seek_by_keyframe_maxnum = am_getconfig_int_def("libplayer.cache.seekkeynum", SEEK_KEY_FRAME_MAX_NUM);
        	log_print("[%s]video seek_bykeyframe:%d\n", AVSYNC_TAG, s_avpkt_cache->queue_video->seek_bykeyframe);
	}
	{
                if (s_avpkt_cache->has_audio == 1 && switch_audio) {
			if ((SeekPts = avpkt_cache_queue_search(s_avpkt_cache->queue_audio, s_avpkt_cache->seekTimeMs)) == -1) {
				log_print("%s search audio out of cache\n", __FUNCTION__);
			}
		}else if (s_avpkt_cache->has_video == 1) {
			if ((SeekPts = avpkt_cache_queue_search(s_avpkt_cache->queue_video, s_avpkt_cache->seekTimeMs)) == -1) {
				log_print("%s search video out of cache, nb_packets:%d\n", __FUNCTION__, s_avpkt_cache->queue_video->nb_packets);
			}
		} 

		if (SeekPts != -1) {
                        index = switch_audio?s_avpkt_cache->audio_index:s_avpkt_cache->video_index;
			ret = avpkt_cache_seek(s_avpkt_cache, s_avpkt_cache->seekTimeMs, index);
			if (ret == 0) {
				cache_kick = 1;
			}
		}
	}

        log_print("[%s]cache_kick:%d\n", __FUNCTION__, cache_kick);
	if (cache_kick == 0 && !switch_audio) {
		avpkt_cache_set_cmd(CACHE_CMD_SEEK_OUT_OF_CACHE);
                if (player->seek_async) {
                    avpkt_cache_reset_thread_t(s_avpkt_cache);
                    int count = 0;
                    while (player->player_cache_read_frame_end == 0 && count++<100) {
                        usleep(2000);
                    }
                    log_print("player_cache_read_frame_end=%d, count=%d\n",player->player_cache_read_frame_end, count);
                } else {
                    avpkt_cache_reset(s_avpkt_cache);
                }
		ret = -1;
	} else {
		avpkt_cache_set_cmd(CACHE_CMD_SEEK_IN_CACHE);
		s_avpkt_cache->currenttime_ms = s_avpkt_cache->seekTimeMs;
		s_avpkt_cache->last_currenttime_ms = s_avpkt_cache->seekTimeMs;
		s_avpkt_cache->discontinue_current_ms = 0;

		if (s_avpkt_cache->has_video == 1 && s_avpkt_cache->video_index != -1) {
			s_avpkt_cache->seek_by_keyframe = 0;
			s_avpkt_cache->queue_video->seek_bykeyframe = 0;
			s_avpkt_cache->seek_by_keyframe_maxnum = 0;
			s_avpkt_cache->avsync_mode = 0;
			s_avpkt_cache->avsyncing = 0;
		}

		#ifdef DEBUG_CACHE_PUT
		s_put_cnt = 0;
		#endif

		#ifdef DEBUG_CACHE_GET
		s_get_cnt = 0;
		#endif
	}

	s_avpkt_cache->state = 1;
	if (ret == 0) {
		avpkt_cache_set_cmd(CACHE_CMD_SEARCH_OK);
	}
	return ret;
}

/*
  check to save some (1.5s)
  return: 1 - player can get, 0-player cannot get
*/
static int avpkt_cache_check_frames_reseved_enough(av_packet_cache_t *cache_ptr)
{
	int64_t current_ms = 0;
        int frame_dur_pts = 0;
        int64_t frames_in = 0;
        int64_t frames_out = 0;
	int64_t amstream_buf_ms = 0;
	int64_t keepframe_ms = 0;
    	play_para_t *player = (play_para_t *)cache_ptr->context;
        if (!cache_ptr->queue_video)
            return 1;
	 frame_dur_pts = cache_ptr->queue_video->frame_dur_pts;
	 frames_in = cache_ptr->queue_video->frames_in;
	 frames_out = cache_ptr->queue_video->frames_out;

	frame_dur_pts = (int)(cache_ptr->queue_video->bak_cache_pts/cache_ptr->queue_video->frames_in);
	if (player->first_pcr && frame_dur_pts > 0) {
		current_ms = (int64_t)(player->state.current_ms);
		amstream_buf_ms = (int64_t)((frames_out * frame_dur_pts * cache_ptr->queue_video->timebase) / 90) - (current_ms -
			(s_avpkt_cache->starttime_ms + s_avpkt_cache->seek_discontinue_current_ms + s_avpkt_cache->discontinue_current_ms));

		{
			//new version for keep frames feature
			if (cache_ptr->keeframesstate == 0) {
				if (amstream_buf_ms > cache_ptr->enterkeepframems) {
					cache_ptr->keeframesstate = 1;
				}
			} else if (cache_ptr->keeframesstate == 1) {
				if (frames_in - frames_out <= cache_ptr->keepframes) {
					return 0;
				}
			} else if (cache_ptr->keeframesstate == 2) {
				if (cache_ptr->leftframes == -1) {
					cache_ptr->leftframes = (frames_in - frames_out);
				}

				if (cache_ptr->leftframes <= 0) {
					cache_ptr->leftframes = -1;
					cache_ptr->keeframesstate = 0;
					log_print("reset keeframesstate=0\n");
				} else if (cache_ptr->leftframes > 0 && cache_ptr->leftframes <= cache_ptr->keepframes) {
					cache_ptr->leftframes--;
				} else if (cache_ptr->leftframes > cache_ptr->keepframes) {
					cache_ptr->leftframes = cache_ptr->keepframes;
					cache_ptr->leftframes--;
				}
			}

			return 1;
		}
	}

	return 1;
}

int avpkt_cache_get(AVPacket *pkt, int want_index)
{
	int stream_idx = -1;
	int ret = 0;
	int netdown = 0;
	int netdown_last = s_avpkt_cache->last_netdown_state;
	int64_t keyframe_vpts = -1;
	int64_t keyframe_apts = -1;
	int get_ok = 0;
	play_para_t *player = (play_para_t *)s_avpkt_cache->context;
	int64_t naudio_frames = 0;
	int64_t nvideo_frames = 0;
	int64_t nsub_frames = 0;

	while (1) {
		if (s_avpkt_cache->state != 2) {
			break;
		}

		if (s_avpkt_cache->has_audio == 1) {
			naudio_frames = s_avpkt_cache->queue_audio->frames_in - s_avpkt_cache->queue_audio->frames_out;
		}

		if (s_avpkt_cache->has_video == 1) {
			nvideo_frames = s_avpkt_cache->queue_video->frames_in - s_avpkt_cache->queue_video->frames_out;
		}

		if (s_avpkt_cache->has_sub == 1) {
			nsub_frames = s_avpkt_cache->queue_sub->frames_in - s_avpkt_cache->queue_sub->frames_out;
		}
		#ifdef DEBUG_CACHE_GET
		s_get_cnt++;
		if (s_get_cnt <= s_get_max) {
			log_print("%d get naudio_frames:%lld, nvideo_frames:%lld\n",
				s_get_cnt, naudio_frames, nvideo_frames);
		}
		#endif

		if (naudio_frames == 0 && nvideo_frames == 0 && nsub_frames == 0) {
			break;
		}

		if (s_avpkt_cache->trickmode == 1 && s_avpkt_cache->queue_video->frames_for_seek_forward <= 0) {
			amthreadpool_thread_usleep(10*1000);
			break;
		}

		if (s_avpkt_cache->avsyncing == 1) {
			amthreadpool_thread_usleep(1000);
			break;
		}

		if ((ret = avpkt_cache_check_can_get(s_avpkt_cache, &stream_idx, want_index)) == 1) {
			if (s_avpkt_cache->enable_keepframes == 1
				&& s_avpkt_cache->queue_video->frames_in > 0) {
				netdown = avpkt_cache_check_netlink();
				{
					if (netdown_last == 1 && netdown == 0) {
						log_print("eth0 net down -> up\n");
						s_avpkt_cache->keeframesstate = 2;
					} else if (netdown_last == 0 && netdown == 1) {
						log_print("eth0 net up -> down\n");
					}

					if (netdown != netdown_last)
						s_avpkt_cache->last_netdown_state = s_avpkt_cache->netdown;

					if (s_avpkt_cache->error != 0
						&& s_avpkt_cache->error != AVERROR(EAGAIN)) {
						//go to get, maybe eof
						ret = s_avpkt_cache->error;
					} else {
						if (avpkt_cache_check_frames_reseved_enough(s_avpkt_cache) == 0) {
							amthreadpool_thread_usleep(1000);
							ret = AVERROR(EAGAIN);
							break;
						}
					}
				}
			}

			if (s_avpkt_cache->trickmode == 1) {
				if (s_avpkt_cache->fffb_out_frames == 0) {
					get_ok = 1;
					ret = 0;
					player->playctrl_info.no_need_more_data = 1;

					break;
				}

				player->playctrl_info.no_need_more_data = 0;
				s_avpkt_cache->fffb_out_frames--;
			}

			ret = avpkt_cache_get_byindex(s_avpkt_cache, pkt, stream_idx);

			#ifdef DEBUG_CACHE_GET
			if (s_get_cnt <= s_get_max) {
				log_print("naudio_frames:%lld, nvideo_frames:%lld, ret:%d(%s), pkt.size:%d, sidedata:%s\n",
					naudio_frames, nvideo_frames, ret,
					(stream_idx==s_avpkt_cache->audio_index ? "audio" : "video"), pkt->size,
					(pkt->side_data == NULL ? "NULL" : "side"));
			}
			#endif

			if (ret == -1) {
				if (s_avpkt_cache->error != AVERROR(EAGAIN)) {
					ret = s_avpkt_cache->error;
					log_print("avpkt_cache_get_byindex fail, error:%d, stream_idx:%d\n", ret, stream_idx);
				} else {
					//ret = AVERROR(EAGAIN);
					ret = 0;
				}

				break;
			} else {
				/*
				*  for switch subtitile /audio flow
				*/
				if (!((s_avpkt_cache->has_audio && pkt->stream_index == s_avpkt_cache->audio_index)
					|| (s_avpkt_cache->has_video && pkt->stream_index == s_avpkt_cache->video_index)
					|| (s_avpkt_cache->has_sub && pkt->stream_index == s_avpkt_cache->sub_index))) {
					//av_init_packet(pkt);
					#ifdef DEBUG_CACHE_IN_OUT
					if ((s_cpu_get_cnt % 100) == 0) {
						log_print("s_cpu_get_cnt:%d, ret:%d, error:%d, state:%d aidx:(%d,%d) vidx(%d,%d)\n",
							ret, s_cpu_get_cnt, s_avpkt_cache->error, s_avpkt_cache->state,
							pkt->stream_index, s_avpkt_cache->audio_index,
							pkt->stream_index, s_avpkt_cache->video_index);
					}
					#endif
				}

				//end
				get_ok = 1;
				break;
			}
		} else {
			break;
		}
	}

	if (get_ok == 0) {
		if ((naudio_frames == 0 && nvideo_frames == 0)
			&& s_avpkt_cache->error != 0) {
			ret = s_avpkt_cache->error;
		}
                if (player->playctrl_info.ignore_ffmpeg_errors && s_avpkt_cache->error != 0){
                    ret = 0;
                }
	}

	return ret;
}

int avpkt_cache_get_netlink(void)
{
	return s_avpkt_cache->netdown;
}

static int avpkt_cache_check_netlink(void) {
	if (s_avpkt_cache->local_play == 1) {
		return 0;
	}

	char acNetStatus[16]= {0};
	//net.eth0.hw.status
	property_get("net.eth0.hw.status", acNetStatus, NULL);
	if (acNetStatus[0] == 'c')
		s_avpkt_cache->netdown = 0;
	else if (acNetStatus[0] == 'd')
		s_avpkt_cache->netdown = 1;

	if (s_avpkt_cache->netdown == 1) {
		memset(acNetStatus, 0x0, sizeof(acNetStatus));
		property_get("net.ethwifi.up", acNetStatus, NULL);
		if (atoi(acNetStatus) > 0) {
			s_avpkt_cache->netdown = 0;
		}
	}
	return s_avpkt_cache->netdown;
}

static int avpkt_cache_validate_stream_idx(AVPacket *pkt)
{
	if (pkt == NULL) {
		log_print("Invalid input pkt\n");
		return 0;
	}
#if 0
	if ((s_avpkt_cache->has_audio && pkt->stream_index == s_avpkt_cache->audio_index)
			|| (s_avpkt_cache->has_video && pkt->stream_index == s_avpkt_cache->video_index)
			|| (s_avpkt_cache->has_sub &&
				((s_avpkt_cache->sub_stream == -1) ? (pkt->stream_index == s_avpkt_cache->sub_index)
				: ((1 << (pkt->stream_index))&s_avpkt_cache->sub_stream)))) {
		return 1;
	}

	return 0;
    #else
        return 1;
    #endif 
}

static int avpkt_cache_avsync_pre_process(av_packet_cache_t *cache_ptr, AVPacket *pkt)
{
	int ret = 0;

	if (cache_ptr == NULL || pkt == NULL) {
		return -1;
	}

	if (cache_ptr->seek_by_keyframe_maxnum > 0) {
		cache_ptr->seek_by_keyframe_maxnum--;
		if (cache_ptr->avsync_mode == 1) {
			if (pkt->stream_index == cache_ptr->video_index) {
				if (pkt->flags & AV_PKT_FLAG_KEY) {
					log_print("[%s]first key video, seek_by_keyframe_maxnum:%d\n",
						AVSYNC_TAG, cache_ptr->seek_by_keyframe_maxnum);
					cache_ptr->seek_by_keyframe = 0;
					cache_ptr->queue_video->seek_bykeyframe = 0;
				} else {
					ret = -1;
				}
			} else {
				log_print("[%s]put a/s pkt, seek_by_keyframe_maxnum:%d\n",
					AVSYNC_TAG, cache_ptr->seek_by_keyframe_maxnum);
			}
		} else {
			if (pkt->stream_index == cache_ptr->video_index
				&& (pkt->flags & AV_PKT_FLAG_KEY)) {
				cache_ptr->seek_by_keyframe = 0;
				cache_ptr->queue_video->seek_bykeyframe = 0;
				cache_ptr->seek_by_keyframe_maxnum = 0;
			} else {
				ret = EC_NOT_FRAME_NEEDED;
				log_print("a:pts:0x%llx, dts:0x%llx, key:%d\n", pkt->pts, pkt->dts, (pkt->flags & AV_PKT_FLAG_KEY));
			}
		}
	} else {
		cache_ptr->seek_by_keyframe = 0;
		cache_ptr->queue_video->seek_bykeyframe = 0;
		cache_ptr->seek_by_keyframe_maxnum = 0;
		ret = 0;
	}

	return ret;
}

static int avpkt_cache_avsync_done(av_packet_cache_t *cache_ptr)
{
	if (NULL == cache_ptr) {
		log_print("[%s]Invalid input cache_ptr\n", AVSYNC_TAG);
		return 1;
	}

	if (!(cache_ptr->queue_audio && cache_ptr->queue_audio->frames_in > 0 && 
               cache_ptr->queue_video && cache_ptr->queue_video->frames_in > 0
		&& cache_ptr->queue_audio->cur_pkt != NULL && cache_ptr->queue_video->cur_pkt != NULL)) {
		return 0;
	}

	MyAVPacketList *mypktl = NULL;
	/*1.find first valid apts*/
	int64_t first_apts = AV_NOPTS_VALUE;
	MyAVPacketList *mypktl_a = NULL;
	PacketQueue *aqueue = &cache_ptr->queue_audio;

	for (mypktl = aqueue->cur_pkt; mypktl != NULL; mypktl = mypktl->next) {
		if (mypktl->pkt.pts != AV_NOPTS_VALUE) {
			first_apts = mypktl->pkt.pts;
			mypktl_a = mypktl;
			break;
		}
	}

	if (first_apts == AV_NOPTS_VALUE) {
		return 0;
	}

	/*2.find first valid vpts*/
	int64_t first_vpts = AV_NOPTS_VALUE;
	MyAVPacketList *mypktl_v = NULL;
	PacketQueue *vqueue = &cache_ptr->queue_video;

	for (mypktl = vqueue->cur_pkt; mypktl != NULL; mypktl = mypktl->next) {
		if ((mypktl->pkt.flags & AV_PKT_FLAG_KEY)
			&& mypktl->pkt.pts != AV_NOPTS_VALUE) {
			first_vpts = mypktl->pkt.pts;
			mypktl_v = mypktl;
			break;
		}
	}

	if (first_vpts == AV_NOPTS_VALUE) {
		log_print("[%s]find first key v fail, force finish strong avsync\n", AVSYNC_TAG);
		return 1;
	}

	/*3.compare a v pts*/
	MyAVPacketList *mypktl_a_tmp = NULL;
	if (first_apts > first_vpts) {
		/*4.apts > vpts*/
		//find next key video
		first_vpts = AV_NOPTS_VALUE;
		for (mypktl = mypktl_v->next; mypktl != NULL; mypktl = mypktl->next) {
			if ((mypktl->pkt.flags & AV_PKT_FLAG_KEY)
				&& mypktl->pkt.pts != AV_NOPTS_VALUE
				&& mypktl->pkt.pts >= first_apts) {
				first_vpts = mypktl->pkt.pts;
				mypktl_v = mypktl;
				break;
			}
		}

		if (first_vpts == AV_NOPTS_VALUE) {
			return 0;
		}

		//find apts
		for (mypktl = mypktl_a->next; mypktl != NULL; mypktl = mypktl->next) {
			if ((mypktl->pkt.flags & AV_PKT_FLAG_KEY)
				&& mypktl->pkt.pts != AV_NOPTS_VALUE) {
				if (mypktl->pkt.pts > first_vpts) {
					break;
				} else {
					mypktl_a_tmp = mypktl;
				}
			}
		}

		if (mypktl_a_tmp != NULL) {
			mypktl_a = mypktl_a_tmp;
		}

		//relocate a to mypktl_a
		for (mypktl = aqueue->cur_pkt;
			mypktl != NULL && mypktl->frame_id < mypktl_a->frame_id;
			mypktl = mypktl->next) {
			if (mypktl->used == 0) {
				mypktl->used = 1;
				aqueue->frames_out++;
				aqueue->frames_for_seek_forward--;
				aqueue->frames_for_seek_backward++;
			}
		}

		if (mypktl_a->used == 1) {
			mypktl_a->used = 0;
			aqueue->frames_out--;
			aqueue->frames_for_seek_forward++;
			aqueue->frames_for_seek_backward--;
		}

		aqueue->cur_pkt = mypktl_a;

		//relocate v to mypktl_v
		for (mypktl = vqueue->cur_pkt;
			mypktl != NULL && mypktl->frame_id < mypktl_v->frame_id;
			mypktl = mypktl->next) {
			if (mypktl->used == 0) {
				mypktl->used = 1;
				vqueue->frames_out++;
				vqueue->frames_for_seek_forward--;
				vqueue->frames_for_seek_backward++;
			}
		}

		if (mypktl_v != NULL && mypktl_v->used == 1) {
			mypktl_v->used = 0;
			vqueue->frames_out--;
			vqueue->frames_for_seek_forward++;
			vqueue->frames_for_seek_backward--;
		}

		vqueue->cur_pkt = mypktl_v;

		log_print("[%s][pts:a>v]sync done: first_apts:0x%llx, first_vpts:0x%llx\n",
			AVSYNC_TAG, mypktl_a->pkt.pts, mypktl_v->pkt.pts);
	} else {
		/*5.apts < vpts*/
		//find first apts > vpts
		for (mypktl = mypktl_a->next; mypktl != NULL; mypktl = mypktl->next) {
			if ((mypktl->pkt.flags & AV_PKT_FLAG_KEY)
				&& mypktl->pkt.pts != AV_NOPTS_VALUE) {
				if (mypktl->pkt.pts > first_vpts) {
					break;
				} else {
					mypktl_a_tmp = mypktl;
				}
			}
		}

		if (mypktl_a_tmp != NULL) {
			mypktl_a = mypktl_a_tmp;
		}

		//relocate a to a
		for (mypktl = aqueue->cur_pkt;
			mypktl != NULL && mypktl->frame_id < mypktl_a->frame_id;
			mypktl = mypktl->next) {
			if (mypktl->used == 0) {
				mypktl->used = 1;
				aqueue->frames_out++;
				aqueue->frames_for_seek_forward--;
				aqueue->frames_for_seek_backward++;
			}
		}

		if (mypktl_a->used == 1) {
			mypktl_a->used = 0;
			aqueue->frames_out--;
			aqueue->frames_for_seek_forward++;
			aqueue->frames_for_seek_backward--;
		}

		aqueue->cur_pkt = mypktl_a;

		//relocate v to mypktl_v
		for (mypktl = vqueue->cur_pkt;
			mypktl != NULL && mypktl->frame_id < mypktl_v->frame_id;
			mypktl = mypktl->next) {
			if (mypktl->used == 0) {
				mypktl->used = 1;
				vqueue->frames_out++;
				vqueue->frames_for_seek_forward--;
				vqueue->frames_for_seek_backward++;
			}
		}

		if (mypktl_v != NULL && mypktl_v->used == 1) {
			mypktl_v->used = 0;
			vqueue->frames_out--;
			vqueue->frames_for_seek_forward++;
			vqueue->frames_for_seek_backward--;
		}

		vqueue->cur_pkt = mypktl_v;

		log_print("[%s][pts:a<v]sync done: first_apts:0x%llx, first_vpts:0x%llx\n",
			AVSYNC_TAG, mypktl_a->pkt.pts, mypktl_v->pkt.pts);
	}

	return 1;
}

static int avpkt_cache_put(void)
{
	play_para_t *player = (play_para_t *)s_avpkt_cache->context;
	AVPacket pkt;
	int ret = 0;
        int canput = 0;

        if (avpkt_cache_interrupt(s_avpkt_cache)) {
            if (s_avpkt_cache->backup_pkt.data != NULL) {
                av_free_packet(&s_avpkt_cache->backup_pkt);
            }
            s_avpkt_cache->reading = 0;
            return -1;
        }

     if(!s_avpkt_cache->reading) {
		 av_init_packet(&pkt);
		 s_avpkt_cache->reading = 1;

		 ret = av_read_frame(player->pFormatCtx, &pkt);
		 if (s_avpkt_cache->state != 2) {
			 log_print("av_read_frame ret:%d", ret);
		 }
	 } else {
		 pkt = s_avpkt_cache->backup_pkt;
		 av_init_packet(&s_avpkt_cache->backup_pkt);
	 }
	 canput = avpkt_cache_check_can_put(s_avpkt_cache, &pkt);

	#ifdef DEBUG_CACHE_PUT
	s_put_cnt++;
	if (s_put_cnt <= 20) {
		log_print("%d avpkt_cache_put ret:%d\n", s_put_cnt, canput);
	}
	#endif

        //drop repleate data when bitrate change
	if (s_avpkt_cache->bitrate_change_flag == 1 && get_player_state(player) == PLAYER_RUNNING) {
		if (s_avpkt_cache->has_video && pkt.stream_index == s_avpkt_cache->video_index
			&& s_avpkt_cache->queue_video->first_keyframe == 1) {
			ret = avpkt_cache_do_after_bitchange(s_avpkt_cache->queue_video, &pkt);
			if(ret != -1){
				ret = 0;
			}
			if (ret == -1) {
				ret = EC_NOT_FRAME_NEEDED;
				if (s_avpkt_cache->video_bitrate_change_flag == 0) {
					log_print("[%s], begin video cache bitrate change, ret=%d\n", __FUNCTION__, ret);
				}
				s_avpkt_cache->video_bitrate_change_flag = 1;
			} else if (ret == 0 && s_avpkt_cache->video_bitrate_change_flag == 1) {
				s_avpkt_cache->video_bitrate_change_flag = 0;
				log_print("[%s], 0 end cache bitrate change\n", __FUNCTION__);
			}
		} else if (s_avpkt_cache->has_audio && pkt.stream_index == s_avpkt_cache->audio_index
			&& s_avpkt_cache->queue_audio->first_keyframe == 1) {
			ret = avpkt_cache_do_after_bitchange(s_avpkt_cache->queue_audio, &pkt);
			//according apts whether NO_APTS or not ,use different case to drop audio
			int ret1 = ret;
			if (ret == -2 && s_avpkt_cache->video_bitrate_change_flag == 1) {
				ret = -1;
			} else if(ret != -1) {
				ret = 0;
			}
			if (ret == -1) {
				ret = EC_NOT_FRAME_NEEDED;
				log_print("[%s], audio cache bitrate change, ret=%d, ret1=%d, video_bitrate_change_flag=%d\n",
					__FUNCTION__, ret, ret1, s_avpkt_cache->video_bitrate_change_flag);
			}
		}
	} else if (s_avpkt_cache->bitrate_change_flag == 1 && get_player_state(player) != PLAYER_RUNNING) {
		s_avpkt_cache->video_bitrate_change_flag =0;
	}
    if (ret >= 0 && s_avpkt_cache->cmd == CACHE_CMD_SEEK_OUT_OF_CACHE) {
        ret = EC_NOT_FRAME_NEEDED;
        log_print("%s, EC_NOT_FRAME_NEEDED\n", __FUNCTION__);
	}

	s_avpkt_cache->error = ret;
	if(ret < 0)
	{
		if(ret == AVERROR_EOF) {
			log_print("read eof !");//if eof ,should not read again?
		}
	} else {
		if (avpkt_cache_validate_stream_idx(&pkt) == 1) {
			if (s_avpkt_cache->seek_by_keyframe == 1) {
				ret = avpkt_cache_avsync_pre_process(s_avpkt_cache, &pkt);
			}
			if (canput == 0) {
                                s_avpkt_cache->backup_pkt = pkt;
				return -1;
			}
			if (ret == 0){
				for (; ;) {
					if (s_avpkt_cache->state != 2) {
						ret = -1;
						break;
					}
					ret = avpkt_cache_put_update(s_avpkt_cache, &pkt);
					if (ret == 0) {
                                                avpkt_cache_free_exceeding_mem(s_avpkt_cache);
						break;
					} else if (ret == EC_STATE_CHANGED) {
						//state change
						break;
					} else if (ret == -1) {
						if (s_avpkt_cache->avsync_mode == 1
							&& s_avpkt_cache->avsyncing == 1) {
							log_print("[%s]put fail, force finish strong sync, in frames:%d\n", AVSYNC_TAG, s_avpkt_cache->seek_by_keyframe_maxnum);
							s_avpkt_cache->avsyncing = 0;
							s_avpkt_cache->avsync_mode = 0;
							s_avpkt_cache->seek_by_keyframe_maxnum = 0;
						}
						amthreadpool_thread_usleep(10*1000);
					}
				}
			}

			if (s_avpkt_cache->avsync_mode == 1
				&& s_avpkt_cache->avsyncing == 1) {
				if (s_avpkt_cache->seek_by_keyframe_maxnum <= 0) {
					log_print("[%s]force exit strong sync\n", AVSYNC_TAG);
					s_avpkt_cache->avsyncing = 0;
				} else {
					if (avpkt_cache_avsync_done(s_avpkt_cache) == 1) {
						log_print("[%s]finish strong sync, in frames:%d\n", AVSYNC_TAG, s_avpkt_cache->seek_by_keyframe_maxnum);
						s_avpkt_cache->avsyncing = 0;
						s_avpkt_cache->avsync_mode = 0;
						s_avpkt_cache->seek_by_keyframe_maxnum = 0;
					} else {
						s_avpkt_cache->seek_by_keyframe_maxnum--;
					}
				}
			}
		} else {
			ret = EC_NOT_FRAME_NEEDED;
		}
	}

	s_avpkt_cache->reading = 0;
	if (ret >= 0 || ret == EC_STATE_CHANGED)
    {
		s_avpkt_cache->read_frames++;
	}

	if (pkt.size != 0) {
		av_free_packet(&pkt);
	}

	av_init_packet(&pkt);

    return ret;
}

void* avpkt_cache_release_thread(void *arg)
{
    av_packet_cache_t *cache_ptr = (av_packet_cache_t *)arg;

    avpkt_cache_release(cache_ptr);

    return NULL;
}


int avpkt_cache_release_thread_t(av_packet_cache_t *cache_ptr)
{
    int ret = 0;
    pthread_t       tid;
    pthread_attr_t pthread_attr;
    play_para_t *player = (play_para_t *)cache_ptr->context;
    player->player_cache_reset_status = 1;
    pthread_attr_init(&pthread_attr);
    pthread_attr_setstacksize(&pthread_attr, 0);   //default stack size maybe better
    log_print("open %s\n", __FUNCTION__);
    ret = amthreadpool_pthread_create(&tid, &pthread_attr, (void*)&avpkt_cache_release_thread, (void*)cache_ptr);
    if (ret != 0) {
        log_print("creat player thread failed !\n");
        return ret;
    }
    log_print("[%s:%d]creat cache thread success,tid=%lu\n", __FUNCTION__, __LINE__, tid);
    player->cache_release_tid = tid;
    pthread_attr_destroy(&pthread_attr);
    return PLAYER_SUCCESS;
}


void *cache_worker(void *arg)
{
	int64_t diff_ms;
	int64_t last_current_ms = 0;
	int64_t starttime_us = 0;
	int64_t curtime_us = 0;
	int64_t current_ms = 0;
	play_para_t *player = (play_para_t*)arg;
        avpkt_cache_param_init(s_avpkt_cache, (void *)player);
	int nRunning = 1;
	int ret = 0;
	int64_t read_frames = 20;
	int am_kpi = am_getconfig_int_def("media.amplayer.chmobilekpi", 0);

	s_avpkt_cache->state = 1;
	while (nRunning == 1) {
		if (s_avpkt_cache->state == 2) {
			if (player->playctrl_info.pause_cache != 0) {
				amthreadpool_thread_usleep(CACHE_THREAD_SLEEP_US);
				continue;
			}

			//update bufed_time
			player->state.bufed_time = avpkt_cache_update_bufed_time();
			//end

			if (s_avpkt_cache->error == AVERROR_EOF) {
				amthreadpool_thread_usleep(CACHE_THREAD_SLEEP_US);
				continue;
			}

			if (avpkt_cache_check_netlink() == 1) {
				//net down, sleep
				amthreadpool_thread_usleep(CACHE_THREAD_SLEEP_US);
				continue;
			}

			if ((s_avpkt_cache->trickmode == 1) && s_avpkt_cache->queue_video->frames_for_seek_forward >= 61) {
				amthreadpool_thread_usleep(5*CACHE_THREAD_SLEEP_US);
			}

			if (s_avpkt_cache->keeframesstate == 2) {
				amthreadpool_thread_usleep(CACHE_THREAD_SLEEP_US);
				continue;
			}

			if ((ret = avpkt_cache_put()) < 0) {
				if (s_avpkt_cache->state == 2) {
					if (ret == EC_STATE_CHANGED || ret == EC_NOT_FRAME_NEEDED) {
						//drop audio,continue read frames
						//amthreadpool_thread_usleep(CACHE_THREAD_SLEEP_US);
					} else {
						if (ret != AVERROR(EAGAIN)
							|| (avpkt_cache_check_netlink() == 1)) {
							//may be eof
							//may be netdown
							amthreadpool_thread_usleep(CACHE_THREAD_SLEEP_US);
						} else {
							//check decoder cache time,sleep some microsencond to avoid hold cpu
							if (avpkt_cache_checkvlevel(s_avpkt_cache, 0.5) > 0) {
								amthreadpool_thread_usleep(CACHE_THREAD_SLEEP_US);
							} else {
								if (avpkt_cache_check_netlink() == 1) {
									amthreadpool_thread_usleep(CACHE_THREAD_SLEEP_US);
								}
							}
							//end
						}
					}
				}
			}

			/*other function*/
			if (avpkt_cache_checkvlevel(s_avpkt_cache, 0.5) > 0) {
				read_frames--;
				if (read_frames == 0) {
					amthreadpool_thread_usleep(CACHE_THREAD_SLEEP_US);
					read_frames = 20;
				}
			}
			/*end*/
			continue;
		} else if (s_avpkt_cache->state == 0) {
			break;
		} else if (s_avpkt_cache->state == 1) {
			last_current_ms = 0;
			current_ms = 0;
			curtime_us = 0;
			starttime_us = 0;
			amthreadpool_thread_usleep(5000);
		}
	}

	if (am_kpi & 0x2) {
		avpkt_cache_release_thread_t(s_avpkt_cache);
	} else {
		avpkt_cache_release(s_avpkt_cache);
	}
	log_print("%s:%d end\n", __FUNCTION__, __LINE__);
	return NULL;
}
int avpkt_cache_init(play_para_t *player) 
{
    if (s_avpkt_cache == NULL) {
        s_avpkt_cache = (av_packet_cache_t*) malloc(sizeof(av_packet_cache_t));
        if (s_avpkt_cache ==  NULL) {
            log_print("malloc fail\n", __FUNCTION__, __LINE__);
            return -1;
        }
        memset(s_avpkt_cache, 0x0, sizeof(av_packet_cache_t));
    }
    return 0;
}

int avpkt_cache_task_open(play_para_t *player)
{
	int ret = 0;
    pthread_t       tid;
    pthread_attr_t pthread_attr;

    pthread_attr_init(&pthread_attr);
    pthread_attr_setstacksize(&pthread_attr, 0);   //default stack size maybe better
    log_print("open avpacket cache worker\n");

    ret = amthreadpool_pthread_create(&tid, &pthread_attr, (void*)&cache_worker, (void*)player);
    if (ret != 0) {
        log_print("creat player thread failed !\n");
        return ret;
    }

    log_print("[avpkt_cache_task_open:%d]creat cache thread success,tid=%lu\n", __LINE__, tid);
    pthread_setname_np(tid, "AVPacket_Cache");
	player->cache_thread_id = tid;
    pthread_attr_destroy(&pthread_attr);

    return PLAYER_SUCCESS;
}

int avpkt_cache_task_close(play_para_t *player)
{
	int ret = 0;
	int retry = 0;
	if (player->cache_thread_id != 0) {
		play_para_t *player = (play_para_t *)s_avpkt_cache->context;
		if (s_avpkt_cache->reading == 1) {
			avpkt_cache_interrupt_read(s_avpkt_cache, -1);
			while (retry < 1000) {
				if (s_avpkt_cache->reading == 0)
					break;
				retry++;
				amthreadpool_thread_usleep(2000);
			}
            avpkt_cache_interrupt_read(s_avpkt_cache, -1);
            log_print("cache interrupt read\n");
		}
		log_print("[%s:%d]start join cache thread,tid=%lu\n", __FUNCTION__, __LINE__, player->cache_thread_id);
		avpkt_cache_set_cmd(CACHE_CMD_STOP);
		ret = amthreadpool_pthread_join(player->cache_thread_id, NULL);
	}

	log_print("[%s:%d]join cache thread tid=%lu, ret=%d\n", __FUNCTION__, __LINE__, player->cache_thread_id, ret);
	return ret;
}

