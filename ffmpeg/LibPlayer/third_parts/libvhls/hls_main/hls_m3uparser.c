//coded by peter,20130215

#define LOG_NDEBUG 0
#define LOG_TAG "M3uParser"


#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <sys/types.h>
#include <ctype.h>
#include "hls_m3uparser.h"
#include "hls_utils.h"

#ifdef HAVE_ANDROID_OS
#include "hls_common.h"
#else
#include "hls_debug.h"
#endif
#include <amthreadpool.h>
#include "amconfigutils.h"

#define FOURCC(c1, c2, c3, c4) \
    (c1 << 24 | c2 << 16 | c3 << 8 | c4)

//====================== media group ==============================

static void codecIsType(const char * codec, MediaType * type) {
    if (strlen(codec) < 4) {
        return;
    }
    switch (FOURCC(codec[0], codec[1], codec[2], codec[3])) {
        case 'ac-3':
        case 'alac':
        case 'dra1':
        case 'dtsc':
        case 'dtse':
        case 'dtsh':
        case 'dtsl':
        case 'ec-3':
        case 'enca':
        case 'g719':
        case 'g726':
        case 'm4ae':
        case 'mlpa':
        case 'mp4a':
        case 'raw ':
        case 'samr':
        case 'sawb':
        case 'sawp':
        case 'sevc':
        case 'sqcp':
        case 'ssmv':
        case 'twos':
        case 'agsm':
        case 'alaw':
        case 'dvi ':
        case 'fl32':
        case 'fl64':
        case 'ima4':
        case 'in24':
        case 'in32':
        case 'lpcm':
        case 'Qclp':
        case 'QDM2':
        case 'QDMC':
        case 'ulaw':
        case 'vdva':
            *type |= TYPE_AUDIO;
            return;

        case 'avc1':
        case 'avc2':
        case 'hvc1':
        case 'hev1':
        case 'avcp':
        case 'drac':
        case 'encv':
        case 'mjp2':
        case 'mp4v':
        case 'mvc1':
        case 'mvc2':
        case 'resv':
        case 's263':
        case 'svc1':
        case 'vc-1':
        case 'CFHD':
        case 'civd':
        case 'DV10':
        case 'dvh5':
        case 'dvh6':
        case 'dvhp':
        case 'DVOO':
        case 'DVOR':
        case 'DVTV':
        case 'DVVT':
        case 'flic':
        case 'gif ':
        case 'h261':
        case 'h263':
        case 'HD10':
        case 'jpeg':
        case 'M105':
        case 'mjpa':
        case 'mjpb':
        case 'png ':
        case 'PNTG':
        case 'rle ':
        case 'rpza':
        case 'Shr0':
        case 'Shr1':
        case 'Shr2':
        case 'Shr3':
        case 'Shr4':
        case 'SVQ1':
        case 'SVQ3':
        case 'tga ':
        case 'tiff':
        case 'WRLE':
            *type |= TYPE_VIDEO;
            return;

        default:
            return;
    }
}

static void add_mediaGroup_to_head(M3UParser * var, M3uMediaGroup * group) {
    list_add(&group->mediaGroup_parser_list, &var->mediaGroup_head);
    var->media_group_num++;
}

static void add_mediaItem_to_group_head(M3UParser * var, M3uMediaGroup * group, M3uMediaItem * item) {
    list_add(&item->mediaItem_list, &group->mediaGroup_item_head);
    group->mediaItem_num++;
}

static M3uMediaGroup * get_mediaGroup_by_id(M3UParser * var, MediaType type, const char * groupID) {
    M3uMediaGroup * pos = NULL;
    M3uMediaGroup * tmp = NULL;

    list_for_each_entry_safe(pos, tmp, &var->mediaGroup_head, mediaGroup_parser_list) {
        if (type == pos->type && !strcmp(pos->groupID, groupID)) {
            return pos;
        }
    }

    return NULL;
}

static M3uMediaItem * get_mediaItem_in_group(M3uMediaGroup * group, int index_to_select) {
    M3uMediaItem * pos = NULL;
    M3uMediaItem * tmp = NULL;
    int index = 0;
    list_for_each_entry_safe(pos, tmp, &group->mediaGroup_item_head, mediaItem_list) {
        if (index_to_select >= 0) {
            if (index_to_select == index) {
                return pos;
            }
        } else {
            if (group->selectedIndex >= 0) {
                if (group->selectedIndex == index) {
                    return pos;
                }
            } else if ((pos->flags & FLAG_DEFAULT) != 0) {
                return pos;
            }
        }
        index++;
    }

    if (index_to_select >= 0) {
        pos = NULL;
    } else {
        pos = list_first_entry(&group->mediaGroup_item_head, M3uMediaItem, mediaItem_list);
    }
    return pos;
}

static M3uTrackInfo * get_trackInfo_in_group(M3uMediaGroup * group, int index) {
    M3uTrackInfo * trackInfo = (M3uTrackInfo *)malloc(sizeof(M3uTrackInfo));
    memset(trackInfo, 0, sizeof(M3uTrackInfo));
    int trackType;
    switch (group->type) {
        case TYPE_AUDIO:
            trackType = M3U_MEDIA_TRACK_TYPE_AUDIO;
            break;
        case TYPE_VIDEO:
            trackType = M3U_MEDIA_TRACK_TYPE_VIDEO;
            break;
        case TYPE_SUBS:
            trackType = M3U_MEDIA_TRACK_TYPE_SUBTITLE;
            break;
        default:
            trackType = M3U_MEDIA_TRACK_TYPE_UNKNOWN;
            break;
    }
    trackInfo->track_type = trackType;
    M3uMediaItem * item = get_mediaItem_in_group(group, index);
    if (!item) {
        free(trackInfo);
        return NULL;
    }
    if (item->language[0] != '\0') {
        trackInfo->track_lang = strdup(item->language);
    }
    // TODO: maybe exist other type of subtitle
    if (group->type == TYPE_SUBS) {
        trackInfo->track_mime = strdup(M3U_MEDIA_WEBVTT_MIME_TYPE);
        trackInfo->track_auto = !!(item->flags & FLAG_AUTOSELECT);
        trackInfo->track_default = !!(item->flags & FLAG_DEFAULT);
        trackInfo->track_forced = !!(item->flags & FLAG_FORCED);
    }
    return trackInfo;
}

static MediaType get_media_type_by_codec(const char * codec) {
    if (!codec) {
        return TYPE_NONE;
    }
    char * ptr = codec;
    MediaType type = TYPE_NONE;
    int len = strlen(codec);
    do {
        if (*ptr == ',') {
            ptr++;
        }
        while (isspace(*ptr) && (ptr - codec) < len) {
            ptr++;
        }
        codecIsType(ptr, &type);
    } while ((ptr = strstr(ptr, ",")) != NULL);
    return type;
}

static void clean_all_mediaGroup(M3UParser * var) {
    M3uMediaGroup * pos = NULL;
    M3uMediaGroup * tmp = NULL;
    M3uMediaItem * item_pos = NULL;
    M3uMediaItem * item_tmp = NULL;
    if (!var->media_group_num) {
        return;
    }
    list_for_each_entry_safe(pos, tmp, &var->mediaGroup_head, mediaGroup_parser_list) {
        list_del(&pos->mediaGroup_parser_list);
        if (pos->mediaItem_num > 0) {
            list_for_each_entry_safe(item_pos, item_tmp, &pos->mediaGroup_item_head, mediaItem_list) {
                list_del(&item_pos->mediaItem_list);
                free(item_pos);
                item_pos = NULL;
                pos->mediaItem_num--;
            }
        }
        free(pos);
        pos = NULL;
        var->media_group_num--;
    }
}

//====================== list abouts===============================
#define BASE_NODE_MAX  32*1024 //max to 8k

static int add_node_to_head(M3UParser* var, M3uBaseNode* item)
{
    if (var->base_node_num > var->max_base_node_num) {
        return -1;
    }
    list_add(&item->list, &var->head);
    var->base_node_num++;
    return 0;
}


static M3uBaseNode* get_node_by_time(M3UParser* var, int64_t timeUs)
{
    M3uBaseNode* pos = NULL;
    M3uBaseNode* tmp = NULL;

    list_for_each_entry_safe(pos, tmp, &var->head, list) {
        if (pos->startUs <= timeUs && timeUs < (pos->startUs + pos->durationUs)) {
            return pos;
        }
    }

    return NULL;
}

static M3uBaseNode* get_node_by_datatime(M3UParser* var, int64_t dataTime)
{
    M3uBaseNode* pos = NULL;
    M3uBaseNode* tmp = NULL;

    LOGV("get_node_by_datatime, dataTime: %lld\n", dataTime);
    list_for_each_entry_safe_reverse(pos, tmp, &var->head, list) {
        LOGV("pos->dataTime = %lld, tmp->dataTime = %lld\n", pos->dataTime, tmp->dataTime);
        if ((pos->dataTime <= dataTime) && (dataTime < tmp->dataTime)) {
            LOGV("get_node_by_datatime, index: %d\n", pos->index);
            return pos;
        }
    }
    return NULL;
}

static M3uBaseNode* get_node_by_index(M3UParser* var, int index)
{
    M3uBaseNode* pos = NULL;
    M3uBaseNode* tmp = NULL;
    if (index > var->base_node_num - 1 || index < 0) {
        return NULL;
    }
    list_for_each_entry_safe_reverse(pos, tmp, &var->head, list) {
        if (pos->index  == index) {
            return pos;
        }
    }
    return NULL;
}

static M3uBaseNode* get_node_by_url(M3UParser* var, char *srcurl)
{
    M3uBaseNode* pos = NULL;
    M3uBaseNode* tmp = NULL;
    char * start = NULL;
    char * slash = NULL;
    start = strstr(srcurl, "://");
    if (start) { // just compare relative url.
        slash = strchr(start + 3, '/');
        if (slash) {
            srcurl = slash;
        }
    }
    char * tmp_url = srcurl;
    int len = strlen(srcurl);
    if (srcurl){
        tmp_url = (char*)malloc(len + 1);
        strncpy(tmp_url, srcurl, len);
        tmp_url[len] = '\0';
    }
    list_for_each_entry_safe(pos, tmp, &var->head, list) {
        if (strstr(pos->fileUrl, tmp_url)) {
            if (srcurl) {
                free(tmp_url);
            }
            return pos;
        }
    }
    if (srcurl) {
        free(tmp_url);
    }
    return NULL;
}

static int dump_all_nodes(M3UParser* var)
{
    M3uBaseNode* pos = NULL;
    M3uBaseNode* tmp = NULL;
    if (var->base_node_num == 0) {
        return 0;
    }
    LOGI("*******************Dump All nodes from list start*****************************\n");
    if (var->log_level >= HLS_SHOW_URL) {
        LOGV("***Base url:%s\n", var->baseUrl);
    }
    if (!var->is_variant_playlist) {
        LOGI("***Target duration:%d\n", var->target_duration);
        LOGI("***Have complete tag? %s,Total duration:%lld\n", var->is_complete > 0 ? "YES" : "NO", (long long)var->durationUs);
    }
    list_for_each_entry_safe_reverse(pos, tmp, &var->head, list) {
        if (var->is_variant_playlist && var->log_level >= HLS_SHOW_URL) {
            LOGI("***Stream index:%d,url:%s,bandwidth:%d,program-id:%d\n", pos->index, pos->fileUrl, pos->bandwidth, pos->program_id);
        } else {
            if ((var->log_level >= HLS_SHOW_URL) || (var->is_complete < 1)) {
                LOGI("***Segment index:%d,url:%s,startUs:%lld,duration:%lld\n", pos->index, pos->fileUrl, (long long)pos->startUs, (long long)pos->durationUs);
            }
            LOGI("***Media fileSize:%lld,media readOffset:%lld,media seq:%d", (long long)pos->fileSize, (long long)pos->readOffset, pos->media_sequence);
            LOGI("***With encrypt key info:%s\n", pos->flags & CIPHER_INFO_FLAG ? "YES" : "NO");
            if ((pos->flags & CIPHER_INFO_FLAG) && var->log_level >= HLS_SHOW_URL) {
                LOGI("***Cipher key 's url:%s\n", pos->key != NULL ? pos->key->keyUrl : "unknow");
            }
        }
    }
    LOGI("*******************Dump All nodes from list end*******************************\n");

    return 0;

}
static int clean_all_nodes(M3UParser* var)
{
    M3uBaseNode* pos = NULL;
    M3uBaseNode* tmp = NULL;
    int base_node_num = var->base_node_num;

    if (var->base_node_num == 0) {
        return 0;
    }
    LOGV("*******************Clean %d nodes from list start****************************\n", base_node_num);
    list_for_each_entry_safe(pos, tmp, &var->head, list) {
        list_del(&pos->list);
        if (base_node_num <= 10 && ((var->log_level >= HLS_SHOW_URL) || (var->is_complete < 1))) {
            LOGV("***Release node index:%d,media_seq:%d ,url:%s\n", pos->index, pos->media_sequence, pos->fileUrl);
        }
        if (pos->flags & CIPHER_INFO_FLAG && pos->key != NULL) {
            if (var->log_level >= HLS_SHOW_URL) {
                LOGV("***Release encrypt key info,url:%s\n", pos->key->keyUrl);
            }
            free(pos->key);
            pos->key = NULL;
        }
        free(pos);
        pos = NULL;
        var->base_node_num--;
    }

    LOGV("*******************Clean All nodes from list end******************************\n");
    return 0;

}

static void clean_all_iframe_nodes(M3UParser* var)
{
    if (var->iframe_node_num > 0) {
        int i;
        for (i = 0; i < var->iframe_node_num; i++) {
            M3uIframeNode * node = var->iframe_node_list[i];
            if (node) {
                if (node->uri) {
                    free(node->uri);
                }
                free(node);
            }
        }
        free(var->iframe_node_list);
    }
}

//=====================================utils func============================================
static int startsWith(const char* data, const char *prefix)
{
    return !strncmp(data, prefix, strlen(prefix));
}
static int parseDouble(const char *s, double *x)
{
    char *end;
    double dval = strtod(s, &end);

    if (end == s || (*end != '\0' && *end != ',')) {
        return -1;
    }

    *x = dval;

    return 0;
}

int parseInt32(const char *s, int32_t *x)
{
    char *end;
    long lval = strtol(s, &end, 10);

    if (end == s || (*end != '\0' && *end != ',')) {
        return -1;
    }

    *x = (int32_t)lval;
    return 0;

}

int parseInt64(const char *s, int64_t *x)
{
    if (s == NULL) {
        return -1;
    }
    sscanf(s, "%lld", x);
    return 0;

}
static int trimInvalidSpace(char* line)
{
    const char* emptyString = "";
    if (!strcmp(line, emptyString)) {
        return 0;
    }
    size_t i = 0;
    size_t szSize = strlen(line);
    while (i < szSize && isspace(line[i])) {
        ++i;
    }

    size_t j = szSize;
    while (j > i && isspace(line[j - 1])) {
        --j;
    }

    memmove(line, &line[i], j - i);
    szSize = j - i;
    line[szSize] = '\0';
    return 0;
}
static int parseMetaData(const char*line)
{

    const char *match = strstr(line, ":");

    if (match == NULL) {
        return -1;
    }
    ssize_t colonPos = match - line;

    if (colonPos < 0) {
        return -1;
    }

    int32_t x;
    int err = parseInt32(line + colonPos + 1, &x);

    if (err != 0) {
        return err;
    }

    return x;
}

static int64_t parseDataTime(const char*line)
{

    const char *match = strstr(line, ":");

    if (match == NULL) {
        return -1;
    }
    ssize_t colonPos = match - line;

    if (colonPos < 0) {
        return -1;
    }

    char *dataTime = strdup(line + colonPos + 1);
    if (dataTime == NULL) {
        return -1;
    }

    int len = strlen(dataTime);
    //LOGV("parseDataTime start dataTime: %s, len: %d\n", dataTime, len);
    int i = 0, j = 0;
    for (i = 0; i < len; i++) {
        if ((*(dataTime + i) < '0') || (*(dataTime + i) > '9')) {
            *(dataTime + i) = 0;
            for (j = i + 1; j < len; j++) {
                if ((*(dataTime + j) >= '0') && (*(dataTime + j) <= '9')) {
                    *(dataTime + i) = *(dataTime + j);
                    *(dataTime + j) = 0;
                    break;
                }
            }
        }
    }
    //LOGV("parseDataTime end dataTime: %s\n", dataTime);

    int64_t x = 0;
    int err = parseInt64(dataTime, &x);
    free(dataTime);
    if (err != 0) {
        return err;
    }

    return x;
}

static int64_t parseMetaDataDurationUs(const char* line)
{
    const char *match = strstr(line, ":");

    if (match == NULL) {
        return -1;
    }
    ssize_t colonPos = match - line;

    if (colonPos < 0) {
        return -1;
    }

    double x;
    int err = parseDouble(line + colonPos + 1, &x);

    if (err != 0) {
        return err;
    }
/* fix node  duration less than zero*/
    if (x < 0) {
        LOGI("Warnning parse DurationUs: %lf,don't drop\n", x );
        //x = 0;
    }

    return ((int64_t)(x * 1E6));

}

/**
 * Callback function type for parse_key_value.
 *
 * @param key a pointer to the key
 * @param key_len the number of bytes that belong to the key, including the '='
 *                char
 * @param dest return the destination pointer for the value in *dest, may
 *             be null to ignore the value
 * @param dest_len the length of the *dest buffer
 */
typedef void (*parse_key_val_cb)(void *context, const char *key,
                                 int key_len, char **dest, int *dest_len);
static void parseKeyValue(const char *str, parse_key_val_cb callback_get_buf,
                          void *context)
{
    const char *ptr = str;

    /* Parse key=value pairs. */
    for (;;) {
        const char *key;
        char *dest = NULL, *dest_end;
        int key_len, dest_len = 0;

        /* Skip whitespace and potential commas. */
        while (*ptr && (isspace(*ptr) || *ptr == ',')) {
            ptr++;
        }
        if (!*ptr) {
            break;
        }

        key = ptr;

        if (!(ptr = strchr(key, '='))) {
            break;
        }
        ptr++;
        key_len = ptr - key;

        callback_get_buf(context, key, key_len, &dest, &dest_len);
        dest_end = dest + dest_len - 1;

        if (*ptr == '\"') {
            ptr++;
            while (*ptr && *ptr != '\"') {
                if (*ptr == '\\') {
                    if (!ptr[1]) {
                        break;
                    }
                    if (dest && dest < dest_end) {
                        *dest++ = ptr[1];
                    }
                    ptr += 2;
                } else {
                    if (dest && dest < dest_end) {
                        *dest++ = *ptr;
                    }
                    ptr++;
                }
            }
            if (*ptr == '\"') {
                ptr++;
            }
        } else {
            for (; *ptr && !(isspace(*ptr) || *ptr == ','); ptr++)
                if (dest && dest < dest_end) {
                    *dest++ = *ptr;
                }
        }
        if (dest) {
            *dest = 0;
        }
    }
}

struct variant_info {
    char bandwidth[20];
    char program_id[8];
    char codec[128];
    char audio_groupID[128];
    char video_groupID[128];
    char sub_groupID[128];
    char iframeUri[MAX_URL_SIZE];
};

struct media_info {
    char type[20];
    char groupID[128];
    char language[128];
    char name[128];
    char autoselect[8];
    char groupDefault[8];
    char forced[8];
    char uri[1024];
};

static void handle_variant_args(struct variant_info *info, const char *key,
                                int key_len, char **dest, int *dest_len)
{
    if (!strncmp(key, "BANDWIDTH=", key_len)) {
        *dest     =        info->bandwidth;
        *dest_len = sizeof(info->bandwidth);
    } else if (!strncmp(key, "PROGRAM-ID=", key_len)) {
        *dest     =        info->program_id;
        *dest_len = sizeof(info->program_id);
    } else if (!strncmp(key, "CODECS=", key_len)) {
        *dest     =        info->codec;
        *dest_len = sizeof(info->codec);
    } else if (!strncmp(key, "AUDIO=", key_len)) {
        *dest     =        info->audio_groupID;
        *dest_len = sizeof(info->audio_groupID);
    } else if (!strncmp(key, "VIDEO=", key_len)) {
        *dest     =        info->video_groupID;
        *dest_len = sizeof(info->video_groupID);
    } else if (!strncmp(key, "SUBTITLES=", key_len)) {
        *dest     =        info->sub_groupID;
        *dest_len = sizeof(info->sub_groupID);
    } else if (!strncmp(key, "URI=", key_len)) {
        *dest     =        info->iframeUri;
        *dest_len = sizeof(info->iframeUri);
    }
}

static void handle_media_info(struct media_info *info, const char *key,
                                int key_len, char **dest, int *dest_len) {
    if (!strncmp(key, "TYPE=", key_len)) {
        *dest     = info->type;
        *dest_len = sizeof(info->type);
    } else if (!strncmp(key, "GROUP-ID=", key_len)) {
        *dest     = info->groupID;
        *dest_len = sizeof(info->groupID);
    } else if (!strncmp(key, "LANGUAGE=", key_len)) {
        *dest     = info->language;
        *dest_len = sizeof(info->language);
    } else if (!strncmp(key, "NAME=", key_len)) {
        *dest     = info->name;
        *dest_len = sizeof(info->name);
    } else if (!strncmp(key, "AUTOSELECT=", key_len)) {
        *dest     = info->autoselect;
        *dest_len = sizeof(info->autoselect);
    } else if (!strncmp(key, "DEFAULT=", key_len)) {
        *dest     = info->groupDefault;
        *dest_len = sizeof(info->groupDefault);
    } else if (!strncmp(key, "FORCED=", key_len)) {
        *dest     = info->forced;
        *dest_len = sizeof(info->forced);
    } else if (!strncmp(key, "URI=", key_len)) {
        *dest     = info->uri;
        *dest_len = sizeof(info->uri);
    }
}

static void makeUrl(char *buf, int size, const char *base, const char *rel)
{
    char *sep;
    char* protol_prefix;
    char* option_start;

    // ugly code. for some transformed url of local m3u8
    if (!strncasecmp("android:AmlogicPlayer", base, 21)
        || !strncasecmp("DataSouce:AmlogicPlayerDataSouceProtocol", base, 40)) {
        strncpy(buf, rel, size);
        return;
    }

    if (strncasecmp("http://", base, 7)
        && strncasecmp("https://", base, 8)
        && strncasecmp("file://", base, 7)
        && strncasecmp("/", base, 1)) {
        LOGE("Base URL must be absolute url");
        // Base URL must be absolute
        return;
    }

    if (!strncasecmp("http://", rel, 7) || !strncasecmp("https://", rel, 8)) {
        if ((sep = strstr(rel, "127.0.0.1")) != NULL && strstr(base, "://") != NULL
            && !strstr(base, "voole.com")) { // some apk needs 127.0.0.1
            /*rel url is get from http://127.0.0.1
                    we need changed the 127.0.0.1 to real IP address,for server;
                    for http://127.0.0.1:1234 to http://221.1.1.1:1234
                    as sample
                    */
            char server_address[128];
            int port;
            char *s;
            server_address[0] = '\0';
            av_url_split(NULL, 0, NULL, 0, server_address, sizeof(server_address), &port, NULL, 0, base);
            strncpy(buf, rel, sep - rel); /*get the rel protol header,maybe not same as base.*/
            strlcat(buf, server_address, size - strlen(buf));
            strlcat(buf, sep + (strlen("127.0.0.1")), size - strlen(buf));
            LOGI("base:'%s', url:'%s' => '%s'", base, rel, buf);
            return ;
        }

        // "url" is already an absolute URL, ignore base URL.
        strncpy(buf, rel, size);

        //LOGV("base:'%s', url:'%s' => '%s'", base, rel, buf);
        return;
    }

    /* Absolute path, relative to the current server */
    if (base && strstr(base, "://") && rel[0] == '/') {
        if (base != buf) {
            strlcpy(buf, base, size);
        }
        sep = strstr(buf, "://");
        if (sep) {
            sep += 3;
            sep = strchr(sep, '/');
            if (sep) {
                *sep = '\0';
            }
        }
        strlcat(buf, rel, size);
        return;
    }

    protol_prefix = strstr(rel, "://");
    option_start = strstr(rel, "?");
    /* If rel actually is an absolute url, just copy it */
    if (!base  || rel[0] == '/' || (option_start == NULL  && protol_prefix) || (option_start  && protol_prefix != NULL && protol_prefix < option_start)) {
        /* refurl  have  http://,ftp://,and don't have "?"
          refurl  have  http://,ftp://,and  have "?", so we must ensure it is not a option, link  refurl=filename?authurl=http://xxxxxx
        */
        strlcpy(buf, rel, size);
        return;
    }
    //av_log(NULL, AV_LOG_DEBUG,"[%s:%d],buf:%s\r\n,base:%s\r\n,rel:%s\n",__FUNCTION__,__LINE__,buf,base,rel);
    if (base != buf) {
        strlcpy(buf, base, size);
    }
    /* Remove the file name from the base url */

    option_start = strchr(buf, '?'); /*cut the ? tail, we don't need auth&parameters info..*/
    if (option_start) {
        option_start[0] = '\0';
    }

    sep = strrchr(buf, '/');
    if (sep) {
        sep[1] = '\0';
    } else {
        buf[0] = '\0';
    }
    while (startsWith(rel, "../") && sep) {
        /* Remove the path delimiter at the end */
        sep[0] = '\0';
        sep = strrchr(buf, '/');
        /* If the next directory name to pop off is "..", break here */
        if (!strcmp(sep ? &sep[1] : buf, "..")) {
            /* Readd the slash we just removed */
            strlcat(buf, "/", size);
            break;
        }
        /* Cut off the directory name */
        if (sep) {
            sep[1] = '\0';
        } else {
            buf[0] = '\0';
        }
        rel += 3;
    }
    strlcat(buf, rel, size);

}

static int parseStreamInf(M3UParser* var, char* line, M3uBaseNode * node)
{

    const char *match = strstr(line, ":");

    if (match == NULL) {
        return -1;
    }
    trimInvalidSpace(line);
    ssize_t colonPos = match - line;
    M3uMediaGroup * group = NULL;
    struct variant_info info;
    memset(&info, 0, sizeof(struct variant_info));
    parseKeyValue(line + colonPos + 1, (parse_key_val_cb)handle_variant_args, &info);

    if (info.bandwidth[0] != '\0') {
        node->bandwidth = atoi(info.bandwidth);
    }
    if (info.program_id[0] != '\0') {
        node->program_id = atoi(info.program_id);
    }
    if (info.codec[0] != '\0') {
        memcpy(node->codec, info.codec, sizeof(node->codec));
    }
    if (info.audio_groupID[0] != '\0') {
        group = get_mediaGroup_by_id(var, TYPE_AUDIO, info.audio_groupID);
        if (!group) {
            LOGE("Undefined media group '%s' referenced in stream info.", info.audio_groupID);
        } else {
            memcpy(node->audio_groupID, info.audio_groupID, sizeof(node->audio_groupID));
        }
    }
    if (info.video_groupID[0] != '\0') {
        group = get_mediaGroup_by_id(var, TYPE_VIDEO, info.video_groupID);
        if (!group) {
            LOGE("Undefined media group '%s' referenced in stream info.", info.video_groupID);
        } else {
            memcpy(node->video_groupID, info.video_groupID, sizeof(node->video_groupID));
        }
    }
    if (info.sub_groupID[0] != '\0') {
        group = get_mediaGroup_by_id(var, TYPE_SUBS, info.sub_groupID);
        if (!group) {
            LOGE("Undefined media group '%s' referenced in stream info.", info.sub_groupID);
        } else {
            memcpy(node->sub_groupID, info.sub_groupID, sizeof(node->sub_groupID));
        }
    }
   
    return 0;
}


static int parseStreamIFrameInf(M3UParser* var, char* line, M3uBaseNode * node) {
    const char *match = strstr(line, ":");

    if (match == NULL) {
        return -1;
    }
    trimInvalidSpace(line);
    ssize_t colonPos = match - line;
    struct variant_info info;
    memset(&info, 0, sizeof(struct variant_info));
    parseKeyValue(line + colonPos + 1, (parse_key_val_cb)handle_variant_args, &info);
    if (info.iframeUri[0] != '\0') {
        makeUrl(node->iframeUrl, sizeof(node->iframeUrl), var->baseUrl, info.iframeUri);
    }
    return 0;
}

static int parseMedia(M3UParser* var, char * line) {
    const char * match = strstr(line, ":");
    if (!match) {
        return -1;
    }

    trimInvalidSpace(line);
    ssize_t colonPos = match - line;
    struct media_info info;
    memset(&info, 0, sizeof(struct media_info));
    parseKeyValue(line + colonPos + 1, (parse_key_val_cb)handle_media_info, &info);

    M3uMediaGroup * group = NULL;
    M3uMediaItem * item = (M3uMediaItem *)malloc(sizeof(M3uMediaItem));
    memset(item, 0, sizeof(M3uMediaItem));
    INIT_LIST_HEAD(&item->mediaItem_list);
    int haveType = 0, haveGroupID = 0;
    MediaType type;
    char groupID[128] = {0};
    if (info.type[0] != '\0') {
        if (!strcasecmp("subtitles", info.type)) {
            type = TYPE_SUBS;
        } else if (!strcasecmp("audio", info.type)) {
            type = TYPE_AUDIO;
        } else if (!strcasecmp("video", info.type)) {
            type = TYPE_VIDEO;
        } else if (!strcasecmp("closed-captions", info.type)) {
            type = TYPE_CC;
        } else {
            LOGE("Invalid media group type '%s'", info.type);
            goto FREE;
        }
        haveType = 1;
    }
    if (info.groupID[0] != '\0') {
        memcpy(groupID, info.groupID, sizeof(groupID));
        haveGroupID = 1;
    }
    if (info.language[0] != '\0') {
        memcpy(item->language, info.language, sizeof(item->language));
        item->flags |= FLAG_HAS_LANGUAGE;
    }
    if (info.name[0] != '\0') {
        memcpy(item->name, info.name, sizeof(item->name));
    }
    if (info.autoselect[0] != '\0') {
        if (!strcasecmp("YES", info.autoselect)) {
            item->flags |= FLAG_AUTOSELECT;
        }
    }
    if (info.groupDefault[0] != '\0') {
        if (!strcasecmp("YES", info.groupDefault)) {
            item->flags |= FLAG_DEFAULT;
        }
    }
    if (info.forced[0] != '\0') {
        if (!strcasecmp("YES", info.groupDefault)) {
            item->flags |= FLAG_FORCED;
        }
    }
    if (info.uri[0] != '\0') {
        makeUrl(item->mediaUrl, sizeof(item->mediaUrl), var->baseUrl, info.uri);
        item->flags |= FLAG_HAS_URI;
    }

    if (!haveType || !haveGroupID) {
        LOGE("Incomplete EXT-X-MEDIA element !");
        goto FREE;
    }

    group = get_mediaGroup_by_id(var, type, groupID);
    if (!group) {
        group = (M3uMediaGroup *)malloc(sizeof(M3uMediaGroup));
        memset(group, 0, sizeof(M3uMediaGroup));
        INIT_LIST_HEAD(&group->mediaGroup_item_head);
        INIT_LIST_HEAD(&group->mediaGroup_parser_list);
        group->type = type;
        group->selectedIndex = -1;
        memcpy(group->groupID, groupID, sizeof(group->groupID));
        add_mediaGroup_to_head(var, group);
    }
    add_mediaItem_to_group_head(var, group, item);

    return 0;

FREE:
    free(item);
    return -1;
}


static int parseByteRange(const char* line, uint64_t curOffset, uint64_t *length, uint64_t *offset)
{
    const char *match = strstr(line, ":");

    if (match == NULL) {
        return -1;
    }
    ssize_t colonPos = match - line;

    if (colonPos < 0) {
        return -1;
    }
    match = strstr(line + colonPos + 1, "@");
    ssize_t atPos = match - line;

    char* lenStr = NULL;
    if (atPos < 0) {
        lenStr = strndup(line + colonPos + 1, strlen(line) - colonPos - 1);
    } else {
        lenStr = strndup(line + colonPos + 1, atPos - colonPos - 1);
    }

    trimInvalidSpace(lenStr);

    const char *s = lenStr;
    char *end;
    *length = strtoull(s, &end, 10);

    if (s == end || *end != '\0') {
        return -1;
    }

    if (atPos >= 0) {
        char* offStr = strndup(line + atPos + 1, strlen(line) - atPos - 1);
        trimInvalidSpace(offStr);

        const char *s = offStr;
        *offset = strtoull(s, &end, 10);

        if (s == end || *end != '\0') {
            return -1;
        }
    } else {
        *offset = curOffset;
    }

    return 0;
}


static void handle_key_args(M3uKeyInfo *info, const char *key,
                            int key_len, char **dest, int *dest_len)
{
    if (!strncmp(key, "METHOD=", key_len)) {
        *dest     =        info->method;
        *dest_len = sizeof(info->method);
    } else if (!strncmp(key, "URI=", key_len)) {
        *dest     =        info->keyUrl;
        *dest_len = sizeof(info->keyUrl);
    } else if (!strncmp(key, "IV=", key_len)) {
        *dest     =        info->iv;
        *dest_len = sizeof(info->iv);
    }
}

static int parseCipherInfo(const char* line, const char* baseUrl, M3uKeyInfo* info)
{
    const char *match = strstr(line, ":");

    if (match == NULL) {
        return -1;
    }
    M3uKeyInfo cInfo;
    memset(cInfo.method, 0x00, sizeof(cInfo.method));
    memset(cInfo.iv, 0x00, sizeof(cInfo.iv));

    parseKeyValue(match + 1, (parse_key_val_cb) handle_key_args, &cInfo);
    makeUrl(info->keyUrl, sizeof(info->keyUrl), baseUrl, cInfo.keyUrl);

    //LOGV("MakeUrl,before:url:%s,baseUrl:%s,after:url:%s\n",cInfo.keyUrl,baseUrl,info->keyUrl);
    memcpy(info->iv, cInfo.iv, sizeof(info->iv));
    memcpy(info->method, cInfo.method, sizeof(info->method));

    return 0;

}

//=====================================m3uParse==============================================
#define EXTM3U                      "#EXTM3U"
#define EXTINF                      "#EXTINF"
#define EXT_X_TARGETDURATION        "#EXT-X-TARGETDURATION"
#define EXT_X_MEDIA_SEQUENCE        "#EXT-X-MEDIA-SEQUENCE"
#define EXT_X_KEY                   "#EXT-X-KEY"
#define EXT_X_PROGRAM_DATE_TIME     "#EXT-X-PROGRAM-DATE-TIME"
#define EXT_X_ALLOW_CACHE           "#EXT-X-ALLOW-CACHE"
#define EXT_X_ENDLIST               "#EXT-X-ENDLIST"
#define EXT_X_MEDIA                 "#EXT-X-MEDIA:"
#define EXT_X_STREAM_INF            "#EXT-X-STREAM-INF"
#define EXT_X_I_FRAME_STREAM_INF    "#EXT-X-I-FRAME-STREAM-INF"
#define EXT_X_I_FRAMES_ONLY         "#EXT-X-I-FRAMES-ONLY"
#define EXT_X_DISCONTINUITY         "#EXT-X-DISCONTINUITY"
#define EXT_X_VERSION               "#EXT-X-VERSION"
#define EXT_X_BYTERANGE             "#EXT-X-BYTERANGE"  //>=version 4

#define LINE_SIZE_MAX (1024*16)//according to description, Playready header object should not exceed 15 KB



static int fast_parse(M3UParser* var, const void *_data, int size)
{
    char  line[LINE_SIZE_MAX];
    const char* data = (const char *)_data;
    int offset = 0;
    int offsetLF = 0;
    int lineNo = 0;
    int index = 0;
    int isVariantPlaylist = 0;
    int ret = 0, bandwidth = 0;
    int hasKey = 0;
    int isIframeOnly = 0;
    int last_uri_index = -1;
    uint64_t segmentRangeOffset = 0;
    int64_t duration = 0;
    int64_t totaltime = 0;
    const char* ptr = NULL;
    char * last_uri = (char *)malloc(MAX_URL_SIZE);
    memset(last_uri, 0, MAX_URL_SIZE);
    M3uKeyInfo* keyinfo = NULL;
    M3uBaseNode tmpNode;
    memset(&tmpNode, 0, sizeof(M3uBaseNode));
    tmpNode.media_sequence = 0;
    tmpNode.fileSize= -1;
    tmpNode.readOffset = 0;
    tmpNode.durationUs = -1;

    M3uIframeNode tmpIframeNode;
    memset(&tmpIframeNode, 0, sizeof(M3uIframeNode));
    tmpIframeNode.fileSize= -1;
    tmpIframeNode.readOffset= -1;
    tmpIframeNode.durationUs = -1;
    tmpIframeNode.url_index = -1;

    //cut BOM header
    if (data[0] == 0xEF && data[1] == 0xBB && data[2] == 0xBF) {
        LOGV("Cut file BOM header with UTF8 encoding\n");
        offset = 3;
    } else if ((data[0] == 0xFF && data[1] == 0xFE) || (data[0] == 0xFE && data[1] == 0xFF)) {
        LOGV("Cut file BOM header with common encoding\n");
        offset = 2;
    }
    while (offset < size) {
        //fetch line data
        offsetLF = offset;
        while (offsetLF < size && data[offsetLF] != '\n' && data[offsetLF] != '\0') {
            ++offsetLF;
        }
        if (offsetLF > size) {
            break;
        }
        memset(line, 0, LINE_SIZE_MAX);
        if (offsetLF > offset && data[offsetLF - 1] == '\r') {
            memcpy(line, &data[offset], offsetLF - offset - 1);
            line[offsetLF - offset - 1] = '\0';
        } else {
            memcpy(line, &data[offset], offsetLF - offset);
            line[offsetLF - offset] = '\0';
        }

        if (strlen(line) == 0) {
            offset = offsetLF + 1;
            continue;
        }

        if (lineNo == 0) {
            if(startsWith(line, EXTM3U)) {
                var->is_extm3u = 1;
            }else{
                LOGI("error url is not m3u8\n");
                goto error;
            }
        }

        if (var->is_extm3u > 0) {
            if (startsWith(line, EXT_X_TARGETDURATION)) {
                if (isVariantPlaylist) {
                    return -1;
                }

                var->target_duration  = parseMetaData(line);
                LOGV("Got target duration:%d\n", var->target_duration);
            } else if (startsWith(line, EXT_X_MEDIA_SEQUENCE)) {
                if (isVariantPlaylist) {
                    return -1;
                }
                tmpNode.media_sequence = parseMetaData(line);

            } else if (startsWith(line, EXT_X_KEY)) {
                if (isVariantPlaylist) {
                    return -1;
                }
                keyinfo = (M3uKeyInfo*)malloc(sizeof(M3uKeyInfo));
                if (NULL == keyinfo) {
                    LOGE("Failed to allocate memory for keyinfo\n");
                    var->is_initcheck = -1;
                    break;
                }
                ret = parseCipherInfo(line, var->baseUrl, keyinfo);
                if (ret != 0) {
                    LOGE("Failed to parse cipher info\n");

                    var->is_initcheck = -1;
                    free(keyinfo);
                    keyinfo = NULL;
                    break;
                }
                hasKey = 1;

                tmpNode.flags |= CIPHER_INFO_FLAG;
                if (var->log_level >= HLS_SHOW_URL) {
                    LOGV("Cipher info,url:%s,method:%s\n", keyinfo->keyUrl, keyinfo->method);
                }

            } else if (startsWith(line, EXT_X_ENDLIST)) {
                var->is_complete = 1;
            } else if (startsWith(line, EXTINF)) {
                if (isVariantPlaylist) {
                    var->is_initcheck = -1;
                    break;
                }
                if (isIframeOnly) {
                    tmpIframeNode.durationUs = parseMetaDataDurationUs(line);
                } else {
                    tmpNode.durationUs = parseMetaDataDurationUs(line);
                }
            } else if (startsWith(line, EXT_X_DISCONTINUITY)) {
                if (isVariantPlaylist) {
                    var->is_initcheck = -1;
                    break;
                }
                tmpNode.flags |= DISCONTINUE_FLAG;

            } else if (startsWith(line, EXT_X_MEDIA)) {
                ret = parseMedia(var, line);
                if (ret) {
                    LOGE("Failed to parse media, ret : %d", ret);
                    var->is_initcheck = -1;
                    break;
                }
            } else if (startsWith(line, EXT_X_STREAM_INF)) {
                ret = parseStreamInf(var, line, &tmpNode);
                isVariantPlaylist = 1;
            } else if (startsWith(line, EXT_X_I_FRAME_STREAM_INF)) {
                M3uBaseNode * node = get_node_by_index(var, index - 1);
                ret = parseStreamIFrameInf(var, line, node);
            } else if (startsWith(line, EXT_X_I_FRAMES_ONLY)) {
                isIframeOnly = 1;
            } else if (startsWith(line, EXT_X_ALLOW_CACHE)) {
                ptr = line + strlen(EXT_X_ALLOW_CACHE) + 1;
                if (!strncasecmp(ptr, "YES", strlen("YES"))) {
                    tmpNode.flags |= ALLOW_CACHE_FLAG;
                }

            } else if (startsWith(line, EXT_X_VERSION)) {
                ptr = line + strlen(EXT_X_VERSION) + 1;
                int version = atoi(ptr);
                if(version <= 3) {
                    LOGV("M3u8 version is :%d use tradition trick mode\n", version);
                    am_setconfig("media.hls.tradtion_trick", "1");
                } else {
                    
                    am_setconfig("media.hls.tradtion_trick", "0");
                }
                LOGV("M3u8 version is :%d.\n", version);
            } else if (startsWith(line, EXT_X_PROGRAM_DATE_TIME)) {
                if (isVariantPlaylist) {
                    return -1;
                }

                tmpNode.dataTime = parseDataTime(line);
                //LOGV("M3u8 dataTime is :%lld\n", tmpNode.dataTime);
            } else if (startsWith(line, EXT_X_BYTERANGE)) {
                if (isVariantPlaylist) {
                    var->is_initcheck = -1;
                    break;
                }
                uint64_t length, offset;
                ret = parseByteRange(line, segmentRangeOffset, &length, &offset);

                if (ret == 0) {
                    if (isIframeOnly) {
                        tmpIframeNode.fileSize= length+offset;
                        tmpIframeNode.readOffset = offset;
                    } else {
                        tmpNode.fileSize= length;
                        tmpNode.readOffset = offset;
                    }
                    segmentRangeOffset = offset + length;
                }

            }
        }
        if (strlen(line) > 0 && !startsWith(line, "#")) {

            if (!isVariantPlaylist) {
                int64_t tmpDur = 0;
                if (isIframeOnly) {
                    tmpDur = tmpIframeNode.durationUs;
                } else {
                    tmpDur = tmpNode.durationUs;
                }/*
                if (tmpDur < 0) {
                    var->is_initcheck = -1;
                    LOGE("Get invalid node,failed to parse m3u\n");
                    break;
                }*/

            }

            if (isIframeOnly) {
                M3uIframeNode * node = (M3uIframeNode *)malloc(sizeof(M3uIframeNode));
                if (node == NULL) {
                    LOGE("Failed to malloc memory for iframe node\n");
                    var->is_initcheck = -1;
                    break;
                }
                memcpy(node, &tmpIframeNode, sizeof(M3uIframeNode));
                if (!isVariantPlaylist) {
                    node->startUs = var->durationUs;
                    var->durationUs += node->durationUs;
                }
                node->index = index;
                if (strcmp(last_uri, line)) {
                    strlcpy(last_uri, line, MAX_URL_SIZE);
                    char * tmpUri = (char *)malloc(MAX_URL_SIZE);
                    memset(tmpUri, 0, MAX_URL_SIZE);
                    makeUrl(tmpUri, MAX_URL_SIZE, var->baseUrl, line);
                    node->uri = (char *)malloc(MAX_URL_SIZE);
                    strlcpy(node->uri, tmpUri, MAX_URL_SIZE);
                    last_uri_index = index;
                    free(tmpUri);
                }
                node->url_index = last_uri_index;
                in_dynarray_add(&var->iframe_node_list, &var->iframe_node_num, node);
                memset(&tmpIframeNode, 0, sizeof(M3uIframeNode));
                tmpIframeNode.fileSize= -1;
                tmpIframeNode.readOffset = -1;
                ++index;
            } else {
                M3uBaseNode* node  = (M3uBaseNode*)malloc(sizeof(M3uBaseNode));
                if (node == NULL) {
                    LOGE("Failed to malloc memory for node\n");
                    var->is_initcheck = -1;
                    break;
                }
                
                memcpy(node, &tmpNode, sizeof(M3uBaseNode));
                tmpNode.durationUs = -1;
                if (hasKey && keyinfo) {
                    node->key = keyinfo;
                    hasKey = 0;
                    keyinfo = NULL;
                }
                if (!isVariantPlaylist) {
                    node->startUs = var->durationUs;
                    var->durationUs += node->durationUs;
                }
                makeUrl(node->fileUrl, sizeof(node->fileUrl), var->baseUrl, line);
                node->index = index;
                INIT_LIST_HEAD(&node->list);
                add_node_to_head(var, node);
                ++index;
                memset(&tmpNode, 0, sizeof(M3uBaseNode));
                tmpNode.readOffset = 0;
            }
        }

        offset = offsetLF + 1;
        ++lineNo;

    }
error:
    if (var->is_initcheck < 0) {
        if (hasKey > 0 && keyinfo) {
            free(keyinfo);
            keyinfo = NULL;
        }
    }

    free(last_uri);

    if (isVariantPlaylist) {
        var->is_variant_playlist = 1;
    }

    //dump_all_nodes(var);
    return 0;

}

int m3u_parse(const char *baseUrl, const void *data, size_t size, void** hParse)
{
    M3UParser* p = (M3UParser*)malloc(sizeof(M3UParser));
    //init
    p->baseUrl = strndup(baseUrl, MAX_URL_SIZE);

    p->base_node_num = 0;
    p->max_base_node_num = am_getconfig_int_def("libplayer.hls.maxnodenum", BASE_NODE_MAX);
    p->media_group_num = 0;
    p->iframe_node_num = 0;
    p->durationUs = 0;
    p->is_complete = 0;
    p->is_extm3u = 0;
    p->is_variant_playlist = 0;
    p->target_duration = 0;
    p->log_level = 0;
	p->iframe_node_list = 0;
    p->is_invalid=0;
    p->is_initcheck = 1;
    if (in_get_sys_prop_bool("media.amplayer.disp_url") != 0) {
        p->log_level = HLS_SHOW_URL;
    }
    float db = in_get_sys_prop_float("libplayer.hls.debug");
    if (db > 0) {
        p->log_level = db;
    }
    INIT_LIST_HEAD(&p->head);
    INIT_LIST_HEAD(&p->mediaGroup_head);
    int ret = fast_parse(p, data, size);
    if (ret != 0) {
        LOGE("Failed to parse m3u\n");
        if (p)
            m3u_release(p);
        return -1;
    }
    if (p->is_extm3u > 0 && p->is_initcheck > 0) {
        LOGV("Succeed parse m3u\n");
    }
    *hParse = p;
    return 0;

}
int m3u_is_extm3u(void* hParse)
{
    if (NULL == hParse) {
        return -2;
    }
    M3UParser* p = (M3UParser*)hParse;
    LOGV("Got extm3u tag? %s\n", p->is_extm3u > 0 ? "YES" : "NO");
    return p->is_extm3u;
}
int m3u_is_variant_playlist(void* hParse)
{
    if (NULL == hParse) {
        return -2;
    }
    M3UParser* p = (M3UParser*)hParse;
    LOGV("Is variant playlist? %s\n", p->is_variant_playlist > 0 ? "YES" : "NO");
    return p->is_variant_playlist;
}

int m3u_set_invalid(void *hParse, int value)
{
    if (NULL == hParse) {
        return -2;
    }
    if(value!=0 && value!=1)
        return -1;
    M3UParser* p = (M3UParser*)hParse;
    p->is_invalid = value;
    return 0;
}

int m3u_is_invalid(void *hParse)
{
    if (NULL == hParse) {
        return -2;
    }
    M3UParser* p = (M3UParser*)hParse;
    return p->is_invalid;
}

int m3u_is_complete(void* hParse)
{
    if (NULL == hParse) {
        return -2;
    }
    M3UParser* p = (M3UParser*)hParse;
    // LOGV("Got end tag? %s\n",p->is_complete>0?"YES":"NO");
    return p->is_complete;
}
int m3u_get_node_num(void* hParse)
{
    if (NULL == hParse) {
        return -2;
    }
    M3UParser* p = (M3UParser*)hParse;
    // LOGV("Got node num: %d\n",p->base_node_num);
    return p->base_node_num;
}
int64_t m3u_get_durationUs_from_index(void* hParse, int index)
{
    if (NULL == hParse || index < 0) {
        return 0;
    }
    M3UParser* p = (M3UParser*)hParse;
    M3uBaseNode* node = NULL;
    int64_t duration = 0;
    node = get_node_by_index(p, index);
    if(node) {
        duration = p->durationUs -node->startUs;
    }  else
        duration = 1000000; 

    return duration;
}

int64_t m3u_get_durationUs(void* hParse)
{
    if (NULL == hParse) {
        return -2;
    }
    M3UParser* p = (M3UParser*)hParse;
    // LOGV("Got duration: %lld\n",(long long)p->durationUs);
    return p->durationUs;
}
M3uBaseNode* m3u_get_node_by_index(void* hParse, int index)
{
    if (NULL == hParse || index < 0) {
        return NULL;
    }
    M3UParser* p = (M3UParser*)hParse;
    M3uBaseNode* node = NULL;
    node = get_node_by_index(p, index);
    return node;
}
M3uBaseNode* m3u_get_node_by_time(void* hParse, int64_t timeUs)
{
    if (NULL == hParse || timeUs < 0) {
        return NULL;
    }
    M3UParser* p = (M3UParser*)hParse;
    M3uBaseNode* node = NULL;
    node = get_node_by_time(p, timeUs);
    return node;

}
M3uBaseNode* m3u_get_node_by_datatime(void* hParse, int64_t dataTime)
{
    if (NULL == hParse || dataTime < 0) {
        return NULL;
    }
    M3UParser* p = (M3UParser*)hParse;
    M3uBaseNode* node = NULL;
    node = get_node_by_datatime(p, dataTime);
    return node;

}
M3uBaseNode* m3u_get_node_by_url(void* hParse, char *srcurl)
{
    if (NULL == hParse || srcurl == NULL) {
        return NULL;
    }
    M3UParser* p = (M3UParser*)hParse;
    M3uBaseNode* node = NULL;
    node = get_node_by_url(p, srcurl);
    return node;
}
int64_t m3u_get_node_span_size(void* hParse, int start_index, int end_index)
{
    if (NULL == hParse || start_index < 0 || end_index < 0 || end_index < start_index) {
        return -1;
    }

    if (end_index == start_index) {
        return 0;
    }

    M3UParser* var = (M3UParser*)hParse;
    M3uBaseNode* pos = NULL;
    M3uBaseNode* tmp = NULL;
    int64_t spanfilesize = 0;

    if (start_index > var->base_node_num - 1 || end_index > var->base_node_num - 1) {
        return -1;
    }
    list_for_each_entry_safe_reverse(pos, tmp, &var->head, list) {
        if (start_index <= pos->index && pos->index < end_index) {
            LOGI("[%s:%d]index=%d, fileSize=%lld\n", __FUNCTION__, __LINE__, pos->index, pos->fileSize);
            spanfilesize += pos->fileSize;
        }
    }

    return spanfilesize;
}
int m3u_get_target_duration(void* hParse)
{
    if (NULL == hParse) {
        return -2;
    }
    M3UParser* p = (M3UParser*)hParse;
    //LOGV("Got target duration: %d s\n",p->target_duration);
    return p->target_duration;

}

//////////////////////////////////////////////////////////////
int m3u_get_mediaGroup_num(void * hParse) {
    if (NULL == hParse) {
        return -1;
    }
    M3UParser * p = (M3UParser *)hParse;
    if(p->media_group_num == 0 && in_get_sys_prop_bool("media.amplayer.hlsv5_support") == 1)
        return 1;
    return p->media_group_num;
}

M3uMediaItem * m3u_get_media_by_groupID(void * hParse, MediaType type, const char * groupID) {
    if (NULL == hParse) {
        return NULL;
    }
    M3UParser * p = (M3UParser *)hParse;
    M3uMediaGroup * group = get_mediaGroup_by_id(p, type, groupID);
    if (!group) {
        return NULL;
    }
    M3uMediaItem * item = get_mediaItem_in_group(group, -1);
    return item;
}

MediaType m3u_get_media_type_by_codec(const char * codec) {
    return get_media_type_by_codec(codec);
}

MediaType m3u_get_media_type_by_index(void * hParse, int index) {
    if (NULL == hParse) {
        return TYPE_NONE;
    }
    M3UParser * p = (M3UParser *)hParse;
    M3uMediaGroup * pos = NULL;
    M3uMediaGroup * tmp = NULL;
    list_for_each_entry_safe(pos, tmp, &p->mediaGroup_head, mediaGroup_parser_list) {
        int count = pos->mediaItem_num;
        if (index < count) {
            return pos->type;
        }
        index -= count;
    }
    return TYPE_NONE;
}

int m3u_get_track_count(void * hParse) {
    if (NULL == hParse) {
        return -1;
    }
    M3UParser * p = (M3UParser *)hParse;
    int trackCount = 0;
    M3uMediaGroup * pos = NULL;
    M3uMediaGroup * tmp = NULL;
    list_for_each_entry_safe(pos, tmp, &p->mediaGroup_head, mediaGroup_parser_list) {
        trackCount += pos->mediaItem_num;
    }
    return trackCount;
}

M3uTrackInfo * m3u_get_track_info(void * hParse, int index) {
    if (NULL == hParse) {
        return NULL;
    }
    M3UParser * p = (M3UParser *)hParse;
    M3uMediaGroup * pos = NULL;
    M3uMediaGroup * tmp = NULL;
    M3uTrackInfo * trackInfo = NULL;
    list_for_each_entry_safe(pos, tmp, &p->mediaGroup_head, mediaGroup_parser_list) {
        int count = pos->mediaItem_num;
        if (index < count) {
            trackInfo = get_trackInfo_in_group(pos, index);
            break;
        }
        index -= count;
    }
    return trackInfo;
}

int m3u_select_track(void * hParse, int index, int select) {
    if (NULL == hParse) {
        return -1;
    }
    M3UParser * p = (M3UParser *)hParse;
    M3uMediaGroup * pos = NULL;
    M3uMediaGroup * tmp = NULL;
    list_for_each_entry_safe(pos, tmp, &p->mediaGroup_head, mediaGroup_parser_list) {
        int count = pos->mediaItem_num;
        if (index < count) {
            if (select) {
                if (pos->selectedIndex == index) {
                    LOGE("track %d already selected", index);
                    return -1;
                }
                LOGV("selected track %d", index);
                pos->selectedIndex = index;
            } else {
                if (pos->selectedIndex != index) {
                    LOGE("track %d is not selected", index);
                    return -1;
                }
                LOGV("unselected track %d", index);
                pos->selectedIndex = -1;
            }
            return 0;
        }
        index -= count;
    }
    return -1;
}

int m3u_get_selected_track(void * hParse, MediaTrackType type) {
    if (NULL == hParse) {
        return -1;
    }
    M3UParser * p = (M3UParser *)hParse;
    MediaType m_type;
    switch (type) {
        case M3U_MEDIA_TRACK_TYPE_AUDIO:
            m_type = TYPE_AUDIO;
            break;
        case M3U_MEDIA_TRACK_TYPE_VIDEO:
            m_type = TYPE_VIDEO;
            break;
        case M3U_MEDIA_TRACK_TYPE_SUBTITLE:
            m_type = TYPE_SUBS;
            break;
        default:
            return -1;
    }
    int selectedIndex = 0;
    M3uMediaGroup * pos = NULL;
    M3uMediaGroup * tmp = NULL;
    list_for_each_entry_safe(pos, tmp, &p->mediaGroup_head, mediaGroup_parser_list) {
        int count = pos->mediaItem_num;
        if (pos->type != m_type) {
            selectedIndex += count;
        } else if (pos->selectedIndex >= 0) {
            return selectedIndex + pos->selectedIndex;
        }
    }
    return -1;
}
//////////////////////////////////////////////////////////////

int m3u_release(void* hParse)
{
    if (NULL == hParse) {
        return -2;
    }
    M3UParser* p = (M3UParser*)hParse;

    if (p->baseUrl != NULL) {
        free(p->baseUrl);
    }
    clean_all_nodes(p);
    clean_all_iframe_nodes(p);
    clean_all_mediaGroup(p);
    free(p);
    return 0;
}
