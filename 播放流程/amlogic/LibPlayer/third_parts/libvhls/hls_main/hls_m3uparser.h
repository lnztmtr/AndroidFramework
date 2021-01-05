#ifndef _HLS_M3UPARSER_H
#define _HLS_M3UPARSER_H

#include <pthread.h>
#include "hls_list.h"

#ifdef __cplusplus
extern "C" {
#endif

#ifndef MAX_URL_SIZE
#define MAX_URL_SIZE 4096
#endif


#define DISCONTINUE_FLAG                (1<<0)
#define ALLOW_CACHE_FLAG                (1<<1)
#define CIPHER_INFO_FLAG                (1<<5)

// must keep sync with mediadefs.cpp
#define M3U_MEDIA_WEBVTT_MIME_TYPE "text/vtt"

typedef enum {
    TYPE_NONE        = 0,
    TYPE_AUDIO       = 1,
    TYPE_VIDEO       = 2,
    TYPE_SUBS        = 4,
    TYPE_CC          = 8,
} MediaType;

typedef enum {
    FLAG_AUTOSELECT         = 1,
    FLAG_DEFAULT            = 2,
    FLAG_FORCED             = 4,
    FLAG_HAS_LANGUAGE       = 8,
    FLAG_HAS_URI            = 16,
} MediaFlags;

// Keep M3U_MEDIA_TRACK_TYPE_* sync with MediaPlayer.h.
typedef enum {
    M3U_MEDIA_TRACK_TYPE_UNKNOWN = 0,
    M3U_MEDIA_TRACK_TYPE_VIDEO = 1,
    M3U_MEDIA_TRACK_TYPE_AUDIO = 2,
    M3U_MEDIA_TRACK_TYPE_TIMEDTEXT = 3,
    M3U_MEDIA_TRACK_TYPE_SUBTITLE = 4,
} MediaTrackType;

typedef struct _M3uKeyInfo {
    char keyUrl[MAX_URL_SIZE];
    char method[10];
    char iv[35];
} M3uKeyInfo;

typedef struct _M3uBaseNode {
    int index;
    char fileUrl[MAX_URL_SIZE];
    char iframeUrl[MAX_URL_SIZE];
    char audio_groupID[128];
    char video_groupID[128];
    char sub_groupID[128];
    char codec[128];
    int bandwidth;
    int program_id;
    int64_t startUs;
    int64_t durationUs;
    int64_t readOffset;
    int64_t fileSize;
    int64_t dataTime;
    int media_sequence;
    int flags;
    M3uKeyInfo* key;
    struct list_head list;
} M3uBaseNode;

// iframe node
typedef struct _M3uIframeNode {
    int64_t startUs;
    int64_t durationUs;
    int64_t readOffset;
    int64_t fileSize;
    int index;
    int url_index; // point to the node with segment url, to save memory.
    char * uri;
} M3uIframeNode;

typedef struct _M3uMediaItem {
    char name[128];
    char mediaUrl[MAX_URL_SIZE];
    char language[128];
    uint32_t flags;
    struct list_head mediaItem_list;
} M3uMediaItem;

typedef struct _M3uMediaGroup {
    MediaType type;
    char groupID[128];
    int selectedIndex;
    int mediaItem_num;
    struct list_head mediaGroup_item_head;
    struct list_head mediaGroup_parser_list;
} M3uMediaGroup;

typedef struct _M3uTrackInfo {
    int track_type;
    char * track_lang;
    char * track_mime;
    int track_auto;
    int track_default;
    int track_forced;
} M3uTrackInfo;

typedef struct _M3uSubtitleData {
    int64_t sub_timeUs;
    int64_t sub_durationUs;
    int sub_trackIndex;
    int sub_size;
    uint8_t * sub_buffer;
} M3uSubtitleData;

typedef struct _M3UParser {
    int is_variant_playlist;
    int is_extm3u;
    int is_complete;
    int is_initcheck;
    int is_invalid;
    int target_duration;
    int base_node_num;
    int max_base_node_num;
    int media_group_num;
    int iframe_node_num;
    int log_level;
    char *baseUrl;
    int64_t durationUs;
    M3uIframeNode ** iframe_node_list;
    struct list_head  head;
    struct list_head  mediaGroup_head;
} M3UParser;

int parseInt32(const char *s, int32_t *x);
int parseInt64(const char *s, int64_t *x);

int m3u_parse(const char *baseURI, const void *data, size_t size, void** hParse);
int m3u_is_extm3u(void* hParse);
int m3u_is_variant_playlist(void* hParse);
int m3u_is_complete(void* hParse);
int m3u_set_invalid(void* hParse, int value);
int m3u_is_invalid(void* hParse);
int m3u_get_node_num(void* hParse);
int64_t m3u_get_durationUs(void* hParse);
int64_t m3u_get_durationUs_from_index(void* hParse, int index);
int m3u_get_target_duration(void* hParse);
M3uBaseNode* m3u_get_node_by_index(void* hParse, int index);
M3uBaseNode* m3u_get_node_by_time(void* hParse, int64_t timeUs);
M3uBaseNode* m3u_get_node_by_datatime(void* hParse, int64_t dataTime);
M3uBaseNode* m3u_get_node_by_url(void* hParse, char *srcurl);
int64_t m3u_get_node_span_size(void* hParse, int start_index, int end_index);
int m3u_release(void* hParse);

////////////////////////// media group api ///////////////////////////////
int m3u_get_mediaGroup_num(void * hParse);
M3uMediaItem* m3u_get_media_by_groupID(void * hParse, MediaType type, const char * groupID);
MediaType m3u_get_media_type_by_codec(const char * codec);
MediaType m3u_get_media_type_by_index(void * hParse, int index);
int m3u_get_track_count(void * hParse);
M3uTrackInfo * m3u_get_track_info(void * hParse, int index);
int m3u_select_track(void * hParse, int index, int select);
int m3u_get_selected_track(void * hParse, MediaTrackType type);
//////////////////////////////////////////////////////////////////////////


#ifdef __cplusplus
}
#endif


#endif
