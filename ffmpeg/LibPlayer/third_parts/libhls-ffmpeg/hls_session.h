#ifndef HLS_SESSION_H
#define HLS_SESSION_H

#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include "libavutil/intreadwrite.h"
#include "libavutil/opt.h"
#include <libavformat/avio.h>
#include <libavformat/avformat.h>
#include <libavformat/internal.h>
#include <amthreadpool.h>

#include "hls_macro.h"


enum KeyType {
    KEY_NONE,
    KEY_AES_128,
    KEY_SAMPLE_AES
};

struct segment {
    int64_t duration;
    int64_t url_offset;
    int64_t size;
    char *url;
    char *key;
    enum KeyType key_type;
    uint8_t iv[16];
    /* associated Media Initialization Section, treated as a segment */
    struct segment *init_section;
};

struct rendition;

enum PlaylistType {
    PLS_TYPE_UNSPECIFIED,
    PLS_TYPE_EVENT,
    PLS_TYPE_VOD
};

/*
 * Each playlist has its own demuxer. If it currently is active,
 * it has an open AVIOContext too, and potentially an AVPacket
 * containing the next packet from this stream.
 */
struct playlist {
    char url[MAX_URL_SIZE];
    AVIOContext pb;
    uint8_t* read_buffer;
    AVIOContext *input;
    AVFormatContext *parent;
    int index;
    AVFormatContext *ctx;
    AVPacket pkt;
    int has_noheader_flag;

    /* main demuxer streams associated with this playlist
     * indexed by the subdemuxer stream indexes */
    AVStream **main_streams;
    int n_main_streams;

    int finished;
    enum PlaylistType type;
    int64_t target_duration;
    int start_seq_no;
    int n_segments;
    struct segment **segments;
    int needed, cur_needed;
    int cur_seq_no;
    int64_t cur_seg_offset;
    int64_t last_load_time;

    /* Currently active Media Initialization Section */
    struct segment *cur_init_section;
    uint8_t *init_sec_buf;
    unsigned int init_sec_buf_size;
    unsigned int init_sec_data_len;
    unsigned int init_sec_buf_read_offset;

    char key_url[MAX_URL_SIZE];
    uint8_t key[16];

    /* ID3 timestamp handling (elementary audio streams have ID3 timestamps
     * (and possibly other ID3 tags) in the beginning of each segment) */
    int is_id3_timestamped; /* -1: not yet known */
    int64_t id3_mpegts_timestamp; /* in mpegts tb */
    int64_t id3_offset; /* in stream original tb */
    uint8_t* id3_buf; /* temp buffer for id3 parsing */
    unsigned int id3_buf_size;
    AVDictionary *id3_initial; /* data from first id3 tag */
    int id3_found; /* ID3 tag found at some point */
    int id3_changed; /* ID3 tag data has changed at some point */
    //ID3v2ExtraMeta *id3_deferred_extra; /* stored here until subdemuxer is opened */

    int64_t seek_timestamp;
    int seek_flags;
    int seek_stream_index; /* into subdemuxer stream array */

    /* Renditions associated with this playlist, if any.
     * Alternative rendition playlists have a single rendition associated
     * with them, and variant main Media Playlists may have
     * multiple (playlist-less) renditions associated with them. */
    int n_renditions;
    struct rendition **renditions;

    /* Media Initialization Sections (EXT-X-MAP) associated with this
     * playlist, if any. */
    int n_init_sections;
    struct segment **init_sections;
};

struct key_info {
     char uri[MAX_URL_SIZE];
     char method[11];
     char iv[35];
};

struct init_section_info {
    char uri[MAX_URL_SIZE];
    char byterange[32];
};

struct rendition_info {
    char type[16];
    char uri[MAX_URL_SIZE];
    char group_id[MAX_FIELD_LEN];
    char language[MAX_FIELD_LEN];
    char assoc_language[MAX_FIELD_LEN];
    char name[MAX_FIELD_LEN];
    char defaultr[4];
    char forced[4];
    char characteristics[MAX_CHARACTERISTICS_LEN];
};

/*
 * Renditions are e.g. alternative subtitle or audio streams.
 * The rendition may either be an external playlist or it may be
 * contained in the main Media Playlist of the variant (in which case
 * playlist is NULL).
 */
struct rendition {
    enum AVMediaType type;
    struct playlist *playlist;
    char group_id[MAX_FIELD_LEN];
    char language[MAX_FIELD_LEN];
    char name[MAX_FIELD_LEN];
    int disposition;
};

struct variant_info {
    char bandwidth[20];
    /* variant group ids: */
    char audio[MAX_FIELD_LEN];
    char video[MAX_FIELD_LEN];
    char subtitles[MAX_FIELD_LEN];
};

struct variant {
    int bandwidth;

    /* every variant contains at least the main Media Playlist in index 0 */
    int n_playlists;
    struct playlist **playlists;

    char audio_group[MAX_FIELD_LEN];
    char video_group[MAX_FIELD_LEN];
    char subtitles_group[MAX_FIELD_LEN];
};

struct hls_session {
    AVFormatContext *ctx;
    int n_variants;
    struct variant **variants;
    int n_playlists;
    struct playlist **playlists;
    int n_renditions;
    struct rendition **renditions;

    int cur_seq_no;
    int live_start_index;
    int first_packet;
    int64_t first_timestamp;
    int64_t cur_timestamp;
//    AVIOInterruptCB *interrupt_callback;
    char *user_agent;                    ///< holds HTTP user agent set as an AVOption to the HTTP protocol context
    char *cookies;                       ///< holds HTTP cookie values set in either the initial response or as an AVOption to the HTTP protocol context
    char *headers;                       ///< holds HTTP headers set as an AVOption to the HTTP protocol context
    char *http_proxy;                    ///< holds the address of the HTTP proxy server
    AVDictionary *avio_opts;
    int strict_std_compliance;

   
    // user defined
    char *url;
    char *location;
    int64_t filesize;
    char *content;                      // tmp memory used to store m3u8 content
    int64_t duration;
};

#endif
