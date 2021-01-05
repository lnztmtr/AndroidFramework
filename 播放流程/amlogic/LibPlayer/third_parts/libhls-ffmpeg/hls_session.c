#include "hls_session.h"
#include "utils/hls_log.h"

#define LOG_TAG "hls-ffmpeg"

static int read_chomp_line(AVIOContext *s, char *buf, int maxlen)
{
    int len = ff_get_line(s, buf, maxlen);
    while (len > 0 && isspace(buf[len - 1]))
        buf[--len] = '\0';
    return len;
}

static void free_segment_list(struct playlist *pls)
{
    int i;
    for (i = 0; i < pls->n_segments; i++) {
        av_freep(&pls->segments[i]->key);
        av_freep(&pls->segments[i]->url);
        av_freep(&pls->segments[i]);
    }
    av_freep(&pls->segments);
    pls->n_segments = 0;
}

static void free_init_section_list(struct playlist *pls)
{
    int i;
    for (i = 0; i < pls->n_init_sections; i++) {
        av_freep(&pls->init_sections[i]->url);
        av_freep(&pls->init_sections[i]);
    }
    av_freep(&pls->init_sections);
    pls->n_init_sections = 0;
}

static void free_playlist_list(struct hls_session *c)
{
    int i;
    for (i = 0; i < c->n_playlists; i++) {
        struct playlist *pls = c->playlists[i];
        free_segment_list(pls);
        free_init_section_list(pls);
        av_freep(&pls->main_streams);
        av_freep(&pls->renditions);
        av_freep(&pls->id3_buf);
        av_dict_free(&pls->id3_initial);
        //ff_id3v2_free_extra_meta(&pls->id3_deferred_extra);
        av_freep(&pls->init_sec_buf);
        av_free_packet(&pls->pkt); // replace - av_packet_unref(&pls->pkt);
        av_freep(&pls->pb.buffer);
        if (pls->input)
            avio_close(pls->input); // replace - ff_format_io_close(c->ctx, &pls->input);
        if (pls->ctx) {
            pls->ctx->pb = NULL;
            av_close_input_file(pls->ctx); // replace - avformat_close_input(&pls->ctx);
        }
        av_free(pls);
    }
    av_freep(&c->playlists);
    av_freep(&c->cookies);
    av_freep(&c->user_agent);
    av_freep(&c->headers);
    av_freep(&c->http_proxy);
    c->n_playlists = 0;
}

static void free_variant_list(struct hls_session *c)
{
    int i;
    for (i = 0; i < c->n_variants; i++) {
        struct variant *var = c->variants[i];
        av_freep(&var->playlists);
        av_free(var);
    }
    av_freep(&c->variants);
    c->n_variants = 0;
}

static void free_rendition_list(struct hls_session *c)
{
    int i;
    for (i = 0; i < c->n_renditions; i++)
        av_freep(&c->renditions[i]);
    av_freep(&c->renditions);
    c->n_renditions = 0;
}

/*
 * Used to reset a statically allocated AVPacket to a clean slate,
 * containing no data.
 */
static void reset_packet(AVPacket *pkt)
{
    av_init_packet(pkt);
    pkt->data = NULL;
}

static struct playlist *new_playlist(struct hls_session *c, const char *url,
                                     const char *base)
{
    struct playlist *pls = av_mallocz(sizeof(struct playlist));
    if (!pls)
        return NULL;
    reset_packet(&pls->pkt);
    ff_make_absolute_url(pls->url, sizeof(pls->url), base, url);
    pls->seek_timestamp = AV_NOPTS_VALUE;

    pls->is_id3_timestamped = -1;
    pls->id3_mpegts_timestamp = AV_NOPTS_VALUE;

    dynarray_add(&c->playlists, &c->n_playlists, pls);
    return pls;
}

static struct variant *new_variant(struct hls_session *c, struct variant_info *info,
                                   const char *url, const char *base)
{
    struct variant *var;
    struct playlist *pls;

    pls = new_playlist(c, url, base);
    if (!pls)
        return NULL;

    var = av_mallocz(sizeof(struct variant));
    if (!var)
        return NULL;

    if (info) {
        var->bandwidth = atoi(info->bandwidth);
        strcpy(var->audio_group, info->audio);
        strcpy(var->video_group, info->video);
        strcpy(var->subtitles_group, info->subtitles);
    }

    dynarray_add(&c->variants, &c->n_variants, var);
    dynarray_add(&var->playlists, &var->n_playlists, pls);
    return var;
}

static void handle_variant_args(struct variant_info *info, const char *key,
                                int key_len, char **dest, int *dest_len)
{
    if (!strncmp(key, "BANDWIDTH=", key_len)) {
        *dest     =        info->bandwidth;
        *dest_len = sizeof(info->bandwidth);
    } else if (!strncmp(key, "AUDIO=", key_len)) {
        *dest     =        info->audio;
        *dest_len = sizeof(info->audio);
    } else if (!strncmp(key, "VIDEO=", key_len)) {
        *dest     =        info->video;
        *dest_len = sizeof(info->video);
    } else if (!strncmp(key, "SUBTITLES=", key_len)) {
        *dest     =        info->subtitles;
        *dest_len = sizeof(info->subtitles);
    }
}

static struct segment *new_init_section(struct playlist *pls,
                                        struct init_section_info *info,
                                        const char *url_base)
{
    struct segment *sec;
    char *ptr;
    char tmp_str[MAX_URL_SIZE];

    if (!info->uri[0])
        return NULL;

    sec = av_mallocz(sizeof(*sec));
    if (!sec)
        return NULL;

    ff_make_absolute_url(tmp_str, sizeof(tmp_str), url_base, info->uri);
    sec->url = av_strdup(tmp_str);
    if (!sec->url) {
        av_free(sec);
        return NULL;
    }

    if (info->byterange[0]) {
        sec->size = strtoll(info->byterange, NULL, 10);
        ptr = strchr(info->byterange, '@');
        if (ptr)
            sec->url_offset = strtoll(ptr+1, NULL, 10);
    } else {
        /* the entire file is the init section */
        sec->size = -1;
    }

    dynarray_add(&pls->init_sections, &pls->n_init_sections, sec);

    return sec;
}

static void handle_init_section_args(struct init_section_info *info, const char *key,
                                           int key_len, char **dest, int *dest_len)
{
    if (!strncmp(key, "URI=", key_len)) {
        *dest     =        info->uri;
        *dest_len = sizeof(info->uri);
    } else if (!strncmp(key, "BYTERANGE=", key_len)) {
        *dest     =        info->byterange;
        *dest_len = sizeof(info->byterange);
    }
}

static struct rendition *new_rendition(struct hls_session *c, struct rendition_info *info,
                                      const char *url_base)
{
    struct rendition *rend;
    enum AVMediaType type = AVMEDIA_TYPE_UNKNOWN;
    char *characteristic;
    char *chr_ptr;
    char *saveptr;

    if (!strcmp(info->type, "AUDIO"))
        type = AVMEDIA_TYPE_AUDIO;
    else if (!strcmp(info->type, "VIDEO"))
        type = AVMEDIA_TYPE_VIDEO;
    else if (!strcmp(info->type, "SUBTITLES"))
        type = AVMEDIA_TYPE_SUBTITLE;
    else if (!strcmp(info->type, "CLOSED-CAPTIONS"))
        /* CLOSED-CAPTIONS is ignored since we do not support CEA-608 CC in
         * AVC SEI RBSP anyway */
        return NULL;

    if (type == AVMEDIA_TYPE_UNKNOWN)
        return NULL;

    /* URI is mandatory for subtitles as per spec */
    if (type == AVMEDIA_TYPE_SUBTITLE && !info->uri[0])
        return NULL;
#if 0
    /* TODO: handle subtitles (each segment has to parsed separately) */
    if (c->strict_std_compliance > FF_COMPLIANCE_EXPERIMENTAL)
        if (type == AVMEDIA_TYPE_SUBTITLE)
            return NULL;
#endif
    rend = av_mallocz(sizeof(struct rendition));
    if (!rend)
        return NULL;

    dynarray_add(&c->renditions, &c->n_renditions, rend);

    rend->type = type;
    strcpy(rend->group_id, info->group_id);
    strcpy(rend->language, info->language);
    strcpy(rend->name, info->name);

    /* add the playlist if this is an external rendition */
    if (info->uri[0]) {
        rend->playlist = new_playlist(c, info->uri, url_base);
        if (rend->playlist)
            dynarray_add(&rend->playlist->renditions,
                         &rend->playlist->n_renditions, rend);
    }

    if (info->assoc_language[0]) {
        int langlen = strlen(rend->language);
        if (langlen < sizeof(rend->language) - 3) {
            rend->language[langlen] = ',';
            strncpy(rend->language + langlen + 1, info->assoc_language,
                    sizeof(rend->language) - langlen - 2);
        }
    }

    if (!strcmp(info->defaultr, "YES"))
        rend->disposition |= AV_DISPOSITION_DEFAULT;
    if (!strcmp(info->forced, "YES"))
        rend->disposition |= AV_DISPOSITION_FORCED;

    chr_ptr = info->characteristics;
    while ((characteristic = av_strtok(chr_ptr, ",", &saveptr))) {
        if (!strcmp(characteristic, "public.accessibility.describes-music-and-sound"))
            rend->disposition |= AV_DISPOSITION_HEARING_IMPAIRED;
        else if (!strcmp(characteristic, "public.accessibility.describes-video"))
            rend->disposition |= AV_DISPOSITION_VISUAL_IMPAIRED;

        chr_ptr = NULL;
    }

    return rend;
}

static void handle_rendition_args(struct rendition_info *info, const char *key,
                                  int key_len, char **dest, int *dest_len)
{
    if (!strncmp(key, "TYPE=", key_len)) {
        *dest     =        info->type;
        *dest_len = sizeof(info->type);
    } else if (!strncmp(key, "URI=", key_len)) {
        *dest     =        info->uri;
        *dest_len = sizeof(info->uri);
    } else if (!strncmp(key, "GROUP-ID=", key_len)) {
        *dest     =        info->group_id;
        *dest_len = sizeof(info->group_id);
    } else if (!strncmp(key, "LANGUAGE=", key_len)) {
        *dest     =        info->language;
        *dest_len = sizeof(info->language);
    } else if (!strncmp(key, "ASSOC-LANGUAGE=", key_len)) {
        *dest     =        info->assoc_language;
        *dest_len = sizeof(info->assoc_language);
    } else if (!strncmp(key, "NAME=", key_len)) {
        *dest     =        info->name;
        *dest_len = sizeof(info->name);
    } else if (!strncmp(key, "DEFAULT=", key_len)) {
        *dest     =        info->defaultr;
        *dest_len = sizeof(info->defaultr);
    } else if (!strncmp(key, "FORCED=", key_len)) {
        *dest     =        info->forced;
        *dest_len = sizeof(info->forced);
    } else if (!strncmp(key, "CHARACTERISTICS=", key_len)) {
        *dest     =        info->characteristics;
        *dest_len = sizeof(info->characteristics);
    }
    /*
     * ignored:
     * - AUTOSELECT: client may autoselect based on e.g. system language
     * - INSTREAM-ID: EIA-608 closed caption number ("CC1".."CC4")
     */
}

/* used by parse_playlist to allocate a new variant+playlist when the
 * playlist is detected to be a Media Playlist (not Master Playlist)
 * and we have no parent Master Playlist (parsing of which would have
 * allocated the variant and playlist already)
 * *pls == NULL  => Master Playlist or parentless Media Playlist
 * *pls != NULL => parented Media Playlist, playlist+variant allocated */
static int ensure_playlist(struct hls_session *c, struct playlist **pls, const char *url)
{
    if (*pls)
        return 0;
    if (!new_variant(c, NULL, url, NULL))
        return AVERROR(ENOMEM);
    *pls = c->playlists[c->n_playlists - 1];
    return 0;
}

static void update_options(char **dest, const char *name, void *src)
{
    av_freep(dest);
    av_opt_get(src, name, AV_OPT_SEARCH_CHILDREN, (uint8_t**)dest);
    if (*dest && !strlen(*dest))
        av_freep(dest);
}

static int open_url(AVFormatContext *s, AVIOContext **pb, const char *url,
                    AVDictionary *opts, AVDictionary *opts2, int *is_http)
{
    struct hls_session *c = s->priv_data;
    AVDictionary *tmp = NULL;
    const char *proto_name = NULL;
    int ret;

    //av_dict_copy(&tmp, opts, 0);
    //av_dict_copy(&tmp, opts2, 0);

#if 0
    if (av_strstart(url, "crypto", NULL)) {
        if (url[6] == '+' || url[6] == ':')
            proto_name = avio_find_protocol_name(url + 7);
    }

    if (!proto_name)
        proto_name = avio_find_protocol_name(url);

    if (!proto_name)
        return AVERROR_INVALIDDATA;

    // only http(s) & file are allowed
    if (!av_strstart(proto_name, "http", NULL) && !av_strstart(proto_name, "file", NULL))
        return AVERROR_INVALIDDATA;
    if (!strncmp(proto_name, url, strlen(proto_name)) && url[strlen(proto_name)] == ':')
        ;
    else if (av_strstart(url, "crypto", NULL) && !strncmp(proto_name, url + 7, strlen(proto_name)) && url[7 + strlen(proto_name)] == ':')
        ;
    else if (strcmp(proto_name, "file") || !strncmp(url, "file,", 5))
        return AVERROR_INVALIDDATA;
#endif
    
    //ret = s->io_open(s, pb, url, AVIO_FLAG_READ, &tmp);
    ret = avio_open_h2(pb, url, AVIO_FLAG_READ | URL_NO_LP_BUFFER, NULL, &tmp);
    if (ret >= 0) {
        // update cookies on http response with setcookies.
        void *u = (s->flags & AVFMT_FLAG_CUSTOM_IO) ? NULL : s->pb;
        //update_options(&c->cookies, "cookies", u);
        //av_dict_set(&opts, "cookies", c->cookies, 0);
    }

    av_dict_free(&tmp);

    if (is_http) {
        //*is_http = av_strstart(proto_name, "http", NULL);
        *is_http = av_strstart(url, "http", NULL);
    }
    return ret;
}

static int parse_playlist(struct hls_session *session, const char *url, struct playlist *pls, AVIOContext *in)
{
    int ret = 0, is_segment = 0, is_variant = 0;
    int64_t duration = 0;
    enum KeyType key_type = KEY_NONE;
    uint8_t iv[16] = "";
    int has_iv = 0;
    char key[MAX_URL_SIZE] = "";
    char line[MAX_URL_SIZE];
    const char *ptr;
    int close_in = 0;
    int64_t seg_offset = 0;
    int64_t seg_size = -1;
    uint8_t *new_url = NULL;
    struct variant_info variant_info;
    char tmp_str[MAX_URL_SIZE];
    struct segment *cur_init_section = NULL;

    LOGI("Enter parse playlist. %s \n", url);
    if (!in) {
        AVDictionary *opts = NULL;
        close_in = 1;
        ret = avio_open_h2(&in, url, AVIO_FLAG_READ | URL_NO_LP_BUFFER, NULL, &opts);
        if (ret < 0)
            return ret;
    }
    read_chomp_line(in, line, sizeof(line));
    if (strcmp(line, "#EXTM3U")) {
        ret = AVERROR_INVALIDDATA;
        goto fail;
    }

    if (pls) {
        free_segment_list(pls);
        pls->finished = 0;
        pls->type = PLS_TYPE_UNSPECIFIED;
    }
 
    while (!url_feof(in)) {
        read_chomp_line(in, line, sizeof(line));
        if (av_strstart(line, "#EXT-X-STREAM-INF:", &ptr)) {
            is_variant = 1;
            memset(&variant_info, 0, sizeof(variant_info));
            ff_parse_key_value(ptr, (ff_parse_key_val_cb) handle_variant_args,
                               &variant_info);
        } else if (av_strstart(line, "#EXT-X-KEY:", &ptr)) {
#if 0
            struct key_info info = {{0}};
            ff_parse_key_value(ptr, (ff_parse_key_val_cb) handle_key_args,
                               &info);
            key_type = KEY_NONE;
            has_iv = 0;
            if (!strcmp(info.method, "AES-128"))
                key_type = KEY_AES_128;
            if (!strcmp(info.method, "SAMPLE-AES"))
                key_type = KEY_SAMPLE_AES;
            if (!strncmp(info.iv, "0x", 2) || !strncmp(info.iv, "0X", 2)) {
                ff_hex_to_data(iv, info.iv + 2);
                has_iv = 1;
            }
            av_strlcpy(key, info.uri, sizeof(key));
#endif
            continue;
        } else if (av_strstart(line, "#EXT-X-MEDIA:", &ptr)) {
            struct rendition_info info = {{0}};
            ff_parse_key_value(ptr, (ff_parse_key_val_cb) handle_rendition_args,
                               &info);
            new_rendition(session, &info, url);
        } else if (av_strstart(line, "#EXT-X-TARGETDURATION:", &ptr)) {
            ret = ensure_playlist(session, &pls, url);
            if (ret < 0)
                goto fail;
            pls->target_duration = strtoll(ptr, NULL, 10) * AV_TIME_BASE;
        } else if (av_strstart(line, "#EXT-X-MEDIA-SEQUENCE:", &ptr)) {
            ret = ensure_playlist(session, &pls, url);
            if (ret < 0)
                goto fail;
            pls->start_seq_no = atoi(ptr);
        } else if (av_strstart(line, "#EXT-X-PLAYLIST-TYPE:", &ptr)) {
            ret = ensure_playlist(session, &pls, url);
            if (ret < 0)
                goto fail;
            if (!strcmp(ptr, "EVENT"))
                pls->type = PLS_TYPE_EVENT;
            else if (!strcmp(ptr, "VOD"))
                pls->type = PLS_TYPE_VOD;
        } else if (av_strstart(line, "#EXT-X-MAP:", &ptr)) {
            struct init_section_info info = {{0}};
            ret = ensure_playlist(session, &pls, url);
            if (ret < 0)
                goto fail;
            ff_parse_key_value(ptr, (ff_parse_key_val_cb) handle_init_section_args,
                               &info);
            cur_init_section = new_init_section(pls, &info, url);
        } else if (av_strstart(line, "#EXT-X-ENDLIST", &ptr)) {
            if (pls)
                pls->finished = 1;
        } else if (av_strstart(line, "#EXTINF:", &ptr)) {
            is_segment = 1;
            duration   = atof(ptr) * AV_TIME_BASE;
        } else if (av_strstart(line, "#EXT-X-BYTERANGE:", &ptr)) {
            seg_size = strtoll(ptr, NULL, 10);
            ptr = strchr(ptr, '@');
            if (ptr)
                seg_offset = strtoll(ptr+1, NULL, 10);
        } else if (av_strstart(line, "#", NULL)) {
            continue;
        } else if (line[0]) {
            if (is_variant) {
                if (!new_variant(session, &variant_info, line, url)) {
                    ret = AVERROR(ENOMEM);
                    goto fail;
                }
                is_variant = 0;
            }
            if (is_segment) {
                struct segment *seg;
                if (!pls) {
                    if (!new_variant(session, 0, url, NULL)) {
                        ret = AVERROR(ENOMEM);
                        goto fail;
                    }
                    pls = session->playlists[session->n_playlists - 1];
                }
                seg = av_malloc(sizeof(struct segment));
                if (!seg) {
                    ret = AVERROR(ENOMEM);
                    goto fail;
                }
                seg->duration = duration;
                seg->key_type = key_type;
                if (has_iv) {
                    memcpy(seg->iv, iv, sizeof(iv));
                } else {
                    int seq = pls->start_seq_no + pls->n_segments;
                    memset(seg->iv, 0, sizeof(seg->iv));
                    AV_WB32(seg->iv + 12, seq);
                }

                if (key_type != KEY_NONE) {
                    ff_make_absolute_url(tmp_str, sizeof(tmp_str), url, key);
                    seg->key = av_strdup(tmp_str);
                    if (!seg->key) {
                        av_free(seg);
                        ret = AVERROR(ENOMEM);
                        goto fail;
                    }
                } else {
                    seg->key = NULL;
                }

                ff_make_absolute_url(tmp_str, sizeof(tmp_str), url, line);
                seg->url = av_strdup(tmp_str);
                if (!seg->url) {
                    av_free(seg->key);
                    av_free(seg);
                    ret = AVERROR(ENOMEM);
                    goto fail;
                }

                dynarray_add(&pls->segments, &pls->n_segments, seg);
                is_segment = 0;

                seg->size = seg_size;
                if (seg_size >= 0) {
                    seg->url_offset = seg_offset;
                    seg_offset += seg_size;
                    seg_size = -1;
                } else {
                    seg->url_offset = 0;
                    seg_offset = 0;
                }

                seg->init_section = cur_init_section;
            }
        }
    }
    if (pls)
        pls->last_load_time = av_gettime();

    LOGI("parse playlist ok. ret:%d\n", ret);
fail:
    av_free(new_url);
    if (close_in) {
        avio_close(in);
        in = NULL;
    }
    return ret;
}

static struct segment *current_segment(struct playlist *pls)
{
    return pls->segments[pls->cur_seq_no - pls->start_seq_no];
}

enum ReadFromURLMode {
    READ_NORMAL,
    READ_COMPLETE,
};

static int read_from_url(struct playlist *pls, struct segment *seg,
                         uint8_t *buf, int buf_size,
                         enum ReadFromURLMode mode)
{
    int ret;

     /* limit read if the segment was only a part of a file */
    if (seg->size >= 0)
        buf_size = FFMIN(buf_size, seg->size - pls->cur_seg_offset);

    if (mode == READ_COMPLETE) {
        ret = avio_read(pls->input, buf, buf_size);
        if (ret != buf_size)
            av_log(NULL, AV_LOG_ERROR, "Could not read complete segment.\n");
    } else
        ret = avio_read(pls->input, buf, buf_size);

    if (ret > 0)
        pls->cur_seg_offset += ret;

    return ret;
}

static int av_dict_set_int(AVDictionary **pm, const char *key, int64_t value, int flags)
{
    char valuestr[22];                                                                                                                                                   
    snprintf(valuestr, sizeof(valuestr), "%"PRId64, value);                                                                                                              
    flags &= ~AV_DICT_DONT_STRDUP_VAL;                                                                                                                                   
    return av_dict_set(pm, key, valuestr, flags);                                                                                                                        
}

static int open_input(struct hls_session *c, struct playlist *pls, struct segment *seg)
{
    AVDictionary *opts = NULL;
    int ret;
    int is_http = 0;
#if 0
    // broker prior HTTP options that should be consistent across requests
    av_dict_set(&opts, "user-agent", c->user_agent, 0);
    av_dict_set(&opts, "cookies", c->cookies, 0);
    av_dict_set(&opts, "headers", c->headers, 0);
    av_dict_set(&opts, "http_proxy", c->http_proxy, 0);
    av_dict_set(&opts, "seekable", "0", 0);
#endif
    if (seg->size >= 0) {
        /* try to restrict the HTTP request to the part we want
         * (if this is in fact a HTTP request) */
        av_dict_set_int(&opts, "offset", seg->url_offset, 0);
        av_dict_set_int(&opts, "end_offset", seg->url_offset + seg->size, 0);
    }

    av_log(pls->parent, AV_LOG_VERBOSE, "HLS request for url '%s', offset %"PRId64", playlist %d\n",
           seg->url, seg->url_offset, pls->index);
    LOGI("HLS request for url '%s', offset %"PRId64", playlist %d\n",
           seg->url, seg->url_offset, pls->index);


    if (seg->key_type == KEY_NONE) {
        ret = open_url(pls->parent, &pls->input, seg->url, c->avio_opts, opts, &is_http);
    } else if (seg->key_type == KEY_AES_128) {
        AVDictionary *opts2 = NULL;
        char iv[33], key[33], url[MAX_URL_SIZE];
        if (strcmp(seg->key, pls->key_url)) {
            AVIOContext *pb;
            if (open_url(pls->parent, &pb, seg->key, c->avio_opts, opts, NULL) == 0) {
                ret = avio_read(pb, pls->key, sizeof(pls->key));
                if (ret != sizeof(pls->key)) {
                    av_log(NULL, AV_LOG_ERROR, "Unable to read key file %s\n",
                           seg->key);
                }
                //ff_format_io_close(pls->parent, &pb);
                if(pb)
                    avio_close(pb);
            } else {
                av_log(NULL, AV_LOG_ERROR, "Unable to open key file %s\n",
                       seg->key);
            }
            av_strlcpy(pls->key_url, seg->key, sizeof(pls->key_url));
        }
        ff_data_to_hex(iv, seg->iv, sizeof(seg->iv), 0);
        ff_data_to_hex(key, pls->key, sizeof(pls->key), 0);
        iv[32] = key[32] = '\0';
        if (strstr(seg->url, "://"))
            snprintf(url, sizeof(url), "crypto+%s", seg->url);
        else
            snprintf(url, sizeof(url), "crypto:%s", seg->url);

        av_dict_copy(&opts2, c->avio_opts, 0);
        av_dict_set(&opts2, "key", key, 0);
        av_dict_set(&opts2, "iv", iv, 0);

        ret = open_url(pls->parent, &pls->input, url, opts2, opts, &is_http);

        av_dict_free(&opts2);

        if (ret < 0) {
            goto cleanup;
        }
        ret = 0;
    } else if (seg->key_type == KEY_SAMPLE_AES) {
        av_log(pls->parent, AV_LOG_ERROR,
               "SAMPLE-AES encryption is not supported yet\n");
        ret = AVERROR_PATCHWELCOME;
    }
    else
      ret = AVERROR(ENOSYS);

    /* Seek to the requested position. If this was a HTTP request, the offset
     * should already be where want it to, but this allows e.g. local testing
     * without a HTTP server.
     *
     * This is not done for HTTP at all as avio_seek() does internal bookkeeping
     * of file offset which is out-of-sync with the actual offset when "offset"
     * AVOption is used with http protocol, causing the seek to not be a no-op
     * as would be expected. Wrong offset received from the server will not be
     * noticed without the call, though.
     */
    if (ret == 0 && !is_http && seg->key_type == KEY_NONE && seg->url_offset) {
        int64_t seekret = avio_seek(pls->input, seg->url_offset, SEEK_SET);
        if (seekret < 0) {
            av_log(pls->parent, AV_LOG_ERROR, "Unable to seek to offset %"PRId64" of HLS segment '%s'\n", seg->url_offset, seg->url);
            ret = seekret;
            //ff_format_io_close(pls->parent, &pls->input);
            if(pls->input){
                avio_close(pls->input);
                pls->input = NULL;
            }
        }
    }

cleanup:
    av_dict_free(&opts);
    pls->cur_seg_offset = 0;
    return ret;
}

static int update_init_section(struct playlist *pls, struct segment *seg)
{
    static const int max_init_section_size = 1024*1024;
    struct hls_session *c = pls->parent->priv_data;
    int64_t sec_size;
    int64_t urlsize;
    int ret;

    if (seg->init_section == pls->cur_init_section)
        return 0;

    pls->cur_init_section = NULL;

    if (!seg->init_section)
        return 0;

    ret = open_input(c, pls, seg->init_section);
    if (ret < 0) {
        av_log(pls->parent, AV_LOG_WARNING,
               "Failed to open an initialization section in playlist %d\n",
               pls->index);
        return ret;
    }

    LOGI("Got one init section\n");
    if (seg->init_section->size >= 0)
        sec_size = seg->init_section->size;
    else if ((urlsize = avio_size(pls->input)) >= 0)
        sec_size = urlsize;
    else
        sec_size = max_init_section_size;

    av_log(pls->parent, AV_LOG_DEBUG,
           "Downloading an initialization section of size %"PRId64"\n",
           sec_size);

    sec_size = FFMIN(sec_size, max_init_section_size);

    av_fast_malloc(&pls->init_sec_buf, &pls->init_sec_buf_size, sec_size);

    ret = read_from_url(pls, seg->init_section, pls->init_sec_buf,
                        pls->init_sec_buf_size, READ_COMPLETE);
    //ff_format_io_close(pls->parent, &pls->input);
    if(pls->input) {
        avio_close(pls->input);
        pls->input = NULL;
    }

    if (ret < 0)
        return ret;

    pls->cur_init_section = seg->init_section;
    pls->init_sec_data_len = ret;
    pls->init_sec_buf_read_offset = 0;

    /* spec says audio elementary streams do not have media initialization
     * sections, so there should be no ID3 timestamps */
    //pls->is_id3_timestamped = 0;

    return 0;
}

static int64_t default_reload_interval(struct playlist *pls)
{
    return pls->n_segments > 0 ?
                          pls->segments[pls->n_segments - 1]->duration :
                          pls->target_duration;
}

static int read_data(void *opaque, uint8_t *buf, int buf_size)
{

    // ugly code - used to fix amlogicextractordatasource crash
    URLContext * uc = (URLContext *)opaque;
    struct playlist *v = (struct playlist *)uc->priv_data;
    
    //struct playlist *v = opaque;
    struct hls_session *c = v->parent->priv_data;
    int ret, i;
    int just_opened = 0;

restart:
    if (!v->needed)
        return AVERROR_EOF;

    if (!v->input) {
        int64_t reload_interval;
        struct segment *seg;

        /* Check that the playlist is still needed before opening a new
         * segment. */
        if (v->ctx && v->ctx->nb_streams) {
            v->needed = 0;
            for (i = 0; i < v->n_main_streams; i++) {
                if (v->main_streams[i]->discard < AVDISCARD_ALL) {
                    v->needed = 1;
                    break;
                }
            }
        }
        if (!v->needed) {
            av_log(v->parent, AV_LOG_INFO, "No longer receiving playlist %d\n",
                v->index);
            return AVERROR_EOF;
        }

        /* If this is a live stream and the reload interval has elapsed since
         * the last playlist reload, reload the playlists now. */
        reload_interval = default_reload_interval(v);

reload:
        if (!v->finished &&
            av_gettime() - v->last_load_time >= reload_interval) {
            if ((ret = parse_playlist(c, v->url, v, NULL)) < 0) {
                av_log(v->parent, AV_LOG_WARNING, "Failed to reload playlist %d\n",
                       v->index);
                return ret;
            }
            /* If we need to reload the playlist again below (if
             * there's still no more segments), switch to a reload
             * interval of half the target duration. */
            reload_interval = v->target_duration / 2;
        }
        if (v->cur_seq_no < v->start_seq_no) {
            av_log(NULL, AV_LOG_WARNING,
                   "skipping %d segments ahead, expired from playlists\n",
                   v->start_seq_no - v->cur_seq_no);
            v->cur_seq_no = v->start_seq_no;
        }
        if (v->cur_seq_no >= v->start_seq_no + v->n_segments) {
            if (v->finished)
                return AVERROR_EOF;
            while (av_gettime() - v->last_load_time < reload_interval) {
                //if (ff_check_interrupt(c->interrupt_callback))
                //    return AVERROR_EXIT;
                if (url_interrupt_cb()) {
                    av_log(NULL, AV_LOG_INFO, "read_data interrupt, err :-%d\n", AVERROR(EINTR));
                    return AVERROR(EINTR);
                }
                usleep(100*1000);
            }
            /* Enough time has elapsed since the last reload */
            goto reload;
        }

        seg = current_segment(v);

        /* load/update Media Initialization Section, if any */
        ret = update_init_section(v, seg);
        if (ret)
            return ret;

        ret = open_input(c, v, seg);
        if (ret < 0) {
            //if (ff_check_interrupt(c->interrupt_callback))
            //    return AVERROR_EXIT;
            if (url_interrupt_cb()) {
                av_log(NULL, AV_LOG_INFO, "read_data interrupt, err :-%d\n", AVERROR(EINTR));
                return AVERROR(EINTR);
            }
            av_log(v->parent, AV_LOG_WARNING, "Failed to open segment of playlist %d\n",
                   v->index);
            v->cur_seq_no += 1;
            goto reload;
        }
        just_opened = 1;
    }

    if (v->init_sec_buf_read_offset < v->init_sec_data_len) {
        /* Push init section out first before first actual segment */
        int copy_size = FFMIN(v->init_sec_data_len - v->init_sec_buf_read_offset, buf_size);
        memcpy(buf, v->init_sec_buf, copy_size);
        v->init_sec_buf_read_offset += copy_size;
        return copy_size;
    }

    ret = read_from_url(v, current_segment(v), buf, buf_size, READ_NORMAL);
    if (ret > 0) {
#if 0
        if (just_opened && v->is_id3_timestamped != 0) {
            /* Intercept ID3 tags here, elementary audio streams are required
             * to convey timestamps using them in the beginning of each segment. */
            intercept_id3(v, buf, buf_size, &ret);
        }
#endif
        return ret;
    } else {
        LOGI("[%s:%d]read failed. ret:%d \n", __FUNCTION__, __LINE__);
    }
    //ff_format_io_close(v->parent, &v->input);
    if(v->input) {
        LOGI("download one segment finish.url:%s \n", v->url);
        avio_close(v->input);
        v->input = NULL;
    }
    v->cur_seq_no++;

    c->cur_seq_no = v->cur_seq_no;

    goto restart;
}



static void add_renditions_to_variant(struct hls_session *c, struct variant *var,
                                      enum AVMediaType type, const char *group_id)
{
    int i;

    for (i = 0; i < c->n_renditions; i++) {
        struct rendition *rend = c->renditions[i];

        if (rend->type == type && !strcmp(rend->group_id, group_id)) {

            if (rend->playlist)
                /* rendition is an external playlist
                 * => add the playlist to the variant */
                dynarray_add(&var->playlists, &var->n_playlists, rend->playlist);
            else
                /* rendition is part of the variant main Media Playlist
                 * => add the rendition to the main Media Playlist */
                dynarray_add(&var->playlists[0]->renditions,
                             &var->playlists[0]->n_renditions,
                             rend);
        }
    }
}

static void add_metadata_from_renditions(AVFormatContext *s, struct playlist *pls,
                                         enum AVMediaType type)
{
    int rend_idx = 0;
    int i;

    for (i = 0; i < pls->n_main_streams; i++) {
        AVStream *st = pls->main_streams[i];

        if (st->codec->codec_type != type)
            continue;

        for (; rend_idx < pls->n_renditions; rend_idx++) {
            struct rendition *rend = pls->renditions[rend_idx];

            if (rend->type != type)
                continue;

            if (rend->language[0])
                av_dict_set(&st->metadata, "language", rend->language, 0);
            if (rend->name[0])
                av_dict_set(&st->metadata, "comment", rend->name, 0);

            st->disposition |= rend->disposition;
        }
        if (rend_idx >=pls->n_renditions)
            break;
    }
}

/* if timestamp was in valid range: returns 1 and sets seq_no
 * if not: returns 0 and sets seq_no to closest segment */
static int find_timestamp_in_playlist(struct hls_session *c, struct playlist *pls,
                                      int64_t timestamp, int *seq_no)
{
    int i;
    int64_t pos = c->first_timestamp == AV_NOPTS_VALUE ?
                  0 : c->first_timestamp;

    if (timestamp < pos) {
        *seq_no = pls->start_seq_no;
        return 0;
    }

    for (i = 0; i < pls->n_segments; i++) {
        int64_t diff = pos + pls->segments[i]->duration - timestamp;
        if (diff > 0) {
            *seq_no = pls->start_seq_no + i;
            return 1;
        }
        pos += pls->segments[i]->duration;
    }

    *seq_no = pls->start_seq_no + pls->n_segments - 1;

    return 0;
}

static int save_avio_options(AVFormatContext *s)
{
    struct hls_session *c = s->priv_data;
    static const char *opts[] = {
        "headers", "http_proxy", "user_agent", "user-agent", "cookies", NULL };
    const char **opt = opts;
    uint8_t *buf;
    int ret = 0;

    while (*opt) {
        //if (av_opt_get(s->pb, *opt, AV_OPT_SEARCH_CHILDREN | AV_OPT_ALLOW_NULL, &buf) >= 0) {
        if (av_opt_get(s->pb, *opt, AV_OPT_SEARCH_CHILDREN, &buf) >= 0) {
            ret = av_dict_set(&c->avio_opts, *opt, buf,
                              AV_DICT_DONT_STRDUP_VAL);
            if (ret < 0)
                return ret;
        }
        opt++;
    }

    return ret;
}

static int nested_io_open(AVFormatContext *s, AVIOContext **pb, const char *url,
                          int flags, AVDictionary **opts)
{
    av_log(s, AV_LOG_ERROR,
           "A HLS playlist item '%s' referred to an external file '%s'. "
           "Opening this file was forbidden for security reasons\n",
           s->filename, url);
    return AVERROR(EPERM);
}

static void add_stream_to_programs(AVFormatContext *s, struct playlist *pls, AVStream *stream)
{
    struct hls_session *c = s->priv_data;
    int i, j;
    int bandwidth = -1;

    for (i = 0; i < c->n_variants; i++) {
        struct variant *v = c->variants[i];

        for (j = 0; j < v->n_playlists; j++) {
            if (v->playlists[j] != pls)
                continue;

            //av_program_add_stream_index(s, i, stream->index);
            ff_program_add_stream_index(s, i, stream->index);

            if (bandwidth < 0)
                bandwidth = v->bandwidth;
            else if (bandwidth != v->bandwidth)
                bandwidth = -1; /* stream in multiple variants with different bandwidths */
        }
    }

    if (bandwidth >= 0)
        av_dict_set_int(&stream->metadata, "variant_bitrate", bandwidth, 0);
}

/* add new subdemuxer streams to our context, if any */
static int update_streams_from_subdemuxer(AVFormatContext *s, struct playlist *pls)
{
    while (pls->n_main_streams < pls->ctx->nb_streams) {
        int ist_idx = pls->n_main_streams;
        AVStream *st = av_new_stream(s, NULL);
        AVStream *ist = pls->ctx->streams[ist_idx];

        if (!st)
            return AVERROR(ENOMEM);

        st->id = pls->index;

        LOGI("Stream id:%d \n", pls->index);
        //avcodec_parameters_copy(st->codecpar, ist->codecpar);
        avcodec_copy_context(st->codec, ist->codec);
#if 0
        if (pls->is_id3_timestamped) /* custom timestamps via id3 */
            avpriv_set_pts_info(st, 33, 1, MPEG_TIME_BASE);
        else
            avpriv_set_pts_info(st, ist->pts_wrap_bits, ist->time_base.num, ist->time_base.den);
#endif
        dynarray_add(&pls->main_streams, &pls->n_main_streams, st);

        add_stream_to_programs(s, pls, st);
    }

    return 0;
}

static void update_noheader_flag(AVFormatContext *s)
{
    struct hls_session *c = s->priv_data;
    int flag_needed = 0;
    int i;

    for (i = 0; i < c->n_playlists; i++) {
        struct playlist *pls = c->playlists[i];

        if (pls->has_noheader_flag) {
            flag_needed = 1;
            break;
        }
    }

    if (flag_needed)
        s->ctx_flags |= AVFMTCTX_NOHEADER;
    else
        s->ctx_flags &= ~AVFMTCTX_NOHEADER;
}


static int select_cur_seq_no(struct hls_session *c, struct playlist *pls)
{
    int seq_no;

    if (!pls->finished && !c->first_packet &&
        av_gettime() - pls->last_load_time >= default_reload_interval(pls))
        /* reload the playlist since it was suspended */
        parse_playlist(c, pls->url, pls, NULL);

    /* If playback is already in progress (we are just selecting a new
     * playlist) and this is a complete file, find the matching segment
     * by counting durations. */
    if (pls->finished && c->cur_timestamp != AV_NOPTS_VALUE) {
        find_timestamp_in_playlist(c, pls, c->cur_timestamp, &seq_no);
        return seq_no;
    }

    if (!pls->finished) {
        if (!c->first_packet && /* we are doing a segment selection during playback */
            c->cur_seq_no >= pls->start_seq_no &&
            c->cur_seq_no < pls->start_seq_no + pls->n_segments)
            /* While spec 3.4.3 says that we cannot assume anything about the
             * content at the same sequence number on different playlists,
             * in practice this seems to work and doing it otherwise would
             * require us to download a segment to inspect its timestamps. */
            return c->cur_seq_no;

        /* If this is a live stream, start live_start_index segments from the
         * start or end */
        if (c->live_start_index < 0)
            return pls->start_seq_no + FFMAX(pls->n_segments + c->live_start_index, 0);
        else
            return pls->start_seq_no + FFMIN(c->live_start_index, pls->n_segments - 1);
    }

    /* Otherwise just start on the first segment. */
    return pls->start_seq_no;
}



int hls_session_open(struct hls_session *session)
{
    AVFormatContext *s = (AVFormatContext *)session->ctx;
    int ret = 0, i;
    int highest_cur_seq_no = 0;
    
    if(parse_playlist(session, session->url, NULL, NULL) < 0) 
        goto fail;

    if(session->n_variants == 0) {
        LOGI("Empty playlist\n");
        ret = AVERROR_EOF;
        goto fail;
    }

    /* If the playlist only contained playlists (Master Playlist),
     * parse each individual playlist. */
    if (session->n_playlists > 1 || session->playlists[0]->n_segments == 0) {
        for (i = 0; i < session->n_playlists; i++) {
            struct playlist *pls = session->playlists[i];
            if ((ret = parse_playlist(session, pls->url, pls, NULL)) < 0) {
                goto fail;
            }
        }
    }

    if (session->variants[0]->playlists[0]->n_segments == 0) {
        LOGI("Empty playlist\n");
        ret = AVERROR_EOF;
        goto fail;
    }
    
    /* If this isn't a live stream, calculate the total duration of the
     * stream. */
    if (session->variants[0]->playlists[0]->finished) {
        int64_t duration = 0;
        for (i = 0; i < session->variants[0]->playlists[0]->n_segments; i++)
            duration += session->variants[0]->playlists[0]->segments[i]->duration;
        s->duration = duration;
    }

    LOGI("Got Duration: %lld s\n", (int64_t)s->duration);
    /* Associate renditions with variants */
    for (i = 0; i < session->n_variants; i++) {
        struct variant *var = session->variants[i];

        if (var->audio_group[0])
            add_renditions_to_variant(session, var, AVMEDIA_TYPE_AUDIO, var->audio_group);
        if (var->video_group[0])
            add_renditions_to_variant(session, var, AVMEDIA_TYPE_VIDEO, var->video_group);
        if (var->subtitles_group[0])
            add_renditions_to_variant(session, var, AVMEDIA_TYPE_SUBTITLE, var->subtitles_group);
    }

    /* Create a program for each variant */
    for (i = 0; i < session->n_variants; i++) {
        struct variant *v = session->variants[i];
        char bitrate_str[20];
        snprintf(bitrate_str, sizeof(bitrate_str), "%d", v->bandwidth);
        AVProgram *program;

        program = av_new_program(s, i);
        if (!program)
            goto fail;
        //av_dict_set_int(&program->metadata, "variant_bitrate", v->bandwidth, 0);
        av_dict_set(&program->metadata, "variant_bitrate", bitrate_str, 0);
    }

    /* Select the starting segments */
    for (i = 0; i < session->n_playlists; i++) {
        struct playlist *pls = session->playlists[i];

        if (pls->n_segments == 0)
            continue;

        pls->cur_seq_no = select_cur_seq_no(session, pls);
        highest_cur_seq_no = FFMAX(highest_cur_seq_no, pls->cur_seq_no);
    }

    /* Open the demuxer for each playlist */
    for (i = 0; i < session->n_playlists; i++) {
        struct playlist *pls = session->playlists[i];
        AVInputFormat *in_fmt = NULL;

        if (!(pls->ctx = avformat_alloc_context())) {
            ret = AVERROR(ENOMEM);
        TRACE();
            goto fail;
        }

        if (pls->n_segments == 0)
            continue;

        pls->index  = i;
        pls->needed = 1;
        pls->parent = s;

        /*
         * If this is a live stream and this playlist looks like it is one segment
         * behind, try to sync it up so that every substream starts at the same
         * time position (so e.g. avformat_find_stream_info() will see packets from
         * all active streams within the first few seconds). This is not very generic,
         * though, as the sequence numbers are technically independent.
         */
        if (!pls->finished && pls->cur_seq_no == highest_cur_seq_no - 1 &&
            highest_cur_seq_no < pls->start_seq_no + pls->n_segments) {
            pls->cur_seq_no = highest_cur_seq_no;
        }

        pls->read_buffer = av_malloc(INITIAL_BUFFER_SIZE);
        if (!pls->read_buffer){
            ret = AVERROR(ENOMEM);
            avformat_free_context(pls->ctx);
            pls->ctx = NULL;
            goto fail;
        }

        // ugly code - used to fix amlogicextractordatasource crash
        // ====
        URLContext * uc = (URLContext *)av_mallocz(sizeof(URLContext));
        if (!uc) {
            return AVERROR(ENOMEM);
        }
        uc->priv_data = (void *)(pls);
        uc->stream_index = i;
        uc->prot = NULL;
        // ====
        ffio_init_context(&pls->pb, pls->read_buffer, INITIAL_BUFFER_SIZE, 0, uc,
                          read_data, NULL, NULL);
        pls->pb.seekable = 0;
        ret = av_probe_input_buffer(&pls->pb, &in_fmt, pls->segments[0]->url,
                                    NULL, 0, 0);
        if (ret < 0) {
            /* Free the ctx - it isn't initialized properly at this point,
             * so avformat_close_input shouldn't be called. If
             * avformat_open_input fails below, it frees and zeros the
             * context, so it doesn't need any special treatment like this. */
            av_log(s, AV_LOG_ERROR, "Error when loading first segment '%s'\n", pls->segments[0]->url);

            TRACE();
            avformat_free_context(pls->ctx);
            pls->ctx = NULL;
            goto fail;
        }
        pls->ctx->pb       = &pls->pb;
        //pls->ctx->io_open  = nested_io_open;
        //if ((ret = ff_copy_whiteblacklists(pls->ctx, s)) < 0)
        //    goto fail;

        ret = avformat_open_input(&pls->ctx, pls->segments[0]->url, in_fmt, NULL);
        if (ret < 0) {
            goto fail;
        }

#if 0
        if (pls->id3_deferred_extra && pls->ctx->nb_streams == 1) {
            ff_id3v2_parse_apic(pls->ctx, &pls->id3_deferred_extra);
            avformat_queue_attached_pictures(pls->ctx);
            ff_id3v2_free_extra_meta(&pls->id3_deferred_extra);
            pls->id3_deferred_extra = NULL;
        }

        if (pls->is_id3_timestamped == -1)
            av_log(s, AV_LOG_WARNING, "No expected HTTP requests have been made\n");

        /*
         * For ID3 timestamped raw audio streams we need to detect the packet
         * durations to calculate timestamps in fill_timing_for_id3_timestamped_stream(),
         * but for other streams we can rely on our user calling avformat_find_stream_info()
         * on us if they want to.
         */
        if (pls->is_id3_timestamped) {
            ret = avformat_find_stream_info(pls->ctx, NULL);
            if (ret < 0)
                goto fail;
        }
#endif
        pls->has_noheader_flag = !!(pls->ctx->ctx_flags & AVFMTCTX_NOHEADER);

        /* Create new AVStreams for each stream in this playlist */
        ret = update_streams_from_subdemuxer(s, pls);
        if (ret < 0)
            goto fail;

        add_metadata_from_renditions(s, pls, AVMEDIA_TYPE_AUDIO);
        add_metadata_from_renditions(s, pls, AVMEDIA_TYPE_VIDEO);
        add_metadata_from_renditions(s, pls, AVMEDIA_TYPE_SUBTITLE);
    }

    update_noheader_flag(s);

    LOGI("HLS SESSION OPEN OK \n");
    return 0;
fail:
    free_playlist_list(session);
    free_variant_list(session);
    free_rendition_list(session);
    LOGI("HLS SESSION OPEN Failed \n");
    return ret;
}

static int recheck_discard_flags(struct hls_session *c, int first)
{
    //struct hls_session *c = s->priv_data;
    AVFormatContext *s = (AVFormatContext *)c->ctx;
    int i, changed = 0;

    /* Check if any new streams are needed */
    for (i = 0; i < c->n_playlists; i++)
        c->playlists[i]->cur_needed = 0;

    for (i = 0; i < s->nb_streams; i++) {
        AVStream *st = s->streams[i];
        LOGI("i:%d id:%d nb_streams:%d \n", i, s->streams[i]->id, s->nb_streams);
        struct playlist *pls = c->playlists[s->streams[i]->id];
        if (st->discard < AVDISCARD_ALL)
            pls->cur_needed = 1;
    }
    for (i = 0; i < c->n_playlists; i++) {
        struct playlist *pls = c->playlists[i];
        if (pls->cur_needed && !pls->needed) {
            pls->needed = 1;
            changed = 1;
            pls->cur_seq_no = select_cur_seq_no(c, pls);
            pls->pb.eof_reached = 0;
            if (c->cur_timestamp != AV_NOPTS_VALUE) {
                /* catch up */
                pls->seek_timestamp = c->cur_timestamp;
                pls->seek_flags = AVSEEK_FLAG_ANY;
                pls->seek_stream_index = -1;
            }
            av_log(s, AV_LOG_INFO, "Now receiving playlist %d, segment %d\n", i, pls->cur_seq_no);
        } else if (first && !pls->cur_needed && pls->needed) {
            if (pls->input) {
                avio_close(pls->input);
                pls->input = NULL;
                //ff_format_io_close(pls->parent, &pls->input);
            }
            pls->needed = 0;
            changed = 1;
            av_log(s, AV_LOG_INFO, "No longer receiving playlist %d\n", i);
        }
    }
    return changed;
}

static void fill_timing_for_id3_timestamped_stream(struct playlist *pls)
{
    if (pls->id3_offset >= 0) {
        pls->pkt.dts = pls->id3_mpegts_timestamp +
                                 av_rescale_q(pls->id3_offset,
                                              pls->ctx->streams[pls->pkt.stream_index]->time_base,
                                              MPEG_TIME_BASE_Q);
        if (pls->pkt.duration)
            pls->id3_offset += pls->pkt.duration;
        else
            pls->id3_offset = -1;
    } else {
        /* there have been packets with unknown duration
         * since the last id3 tag, should not normally happen */
        pls->pkt.dts = AV_NOPTS_VALUE;
    }

    if (pls->pkt.duration)
        pls->pkt.duration = av_rescale_q(pls->pkt.duration,
                                         pls->ctx->streams[pls->pkt.stream_index]->time_base,
                                         MPEG_TIME_BASE_Q);

    pls->pkt.pts = AV_NOPTS_VALUE;
}


static AVRational get_timebase(struct playlist *pls)
{
    if (pls->is_id3_timestamped)
        return MPEG_TIME_BASE_Q;

    return pls->ctx->streams[pls->pkt.stream_index]->time_base;
}

static int compare_ts_with_wrapdetect(int64_t ts_a, struct playlist *pls_a,
                                      int64_t ts_b, struct playlist *pls_b)
{
    int64_t scaled_ts_a = av_rescale_q(ts_a, get_timebase(pls_a), MPEG_TIME_BASE_Q);
    int64_t scaled_ts_b = av_rescale_q(ts_b, get_timebase(pls_b), MPEG_TIME_BASE_Q);

    return av_compare_mod(scaled_ts_a, scaled_ts_b, 1LL << 33);
}

int hls_session_read_packet(struct hls_session *c, AVPacket *pkt)
{
    AVFormatContext *s = (AVFormatContext *)c->ctx;
    int ret, i, minplaylist = -1;

    LOGI("[%s:%d] Enter read packet \n", __FUNCTION__, __LINE__);
    recheck_discard_flags(c, c->first_packet);
    c->first_packet = 0;

    for (i = 0; i < c->n_playlists; i++) {
        struct playlist *pls = c->playlists[i];
        /* Make sure we've got one buffered packet from each open playlist
         * stream */
        if (pls->needed && !pls->pkt.data) {
            while (1) {
                int64_t ts_diff;
                AVRational tb;
                ret = av_read_frame(pls->ctx, &pls->pkt);
                if (ret < 0) {
                    if (!url_feof(&pls->pb) && ret != AVERROR_EOF)
                        return ret;
                    reset_packet(&pls->pkt);
                    break;
                } else {
                    /* stream_index check prevents matching picture attachments etc. */
                    if (pls->is_id3_timestamped && pls->pkt.stream_index == 0) {
                        /* audio elementary streams are id3 timestamped */
                        fill_timing_for_id3_timestamped_stream(pls);
                    }

                    if (c->first_timestamp == AV_NOPTS_VALUE &&
                        pls->pkt.dts       != AV_NOPTS_VALUE)
                        c->first_timestamp = av_rescale_q(pls->pkt.dts,
                            get_timebase(pls), AV_TIME_BASE_Q);
                }

                if (pls->seek_timestamp == AV_NOPTS_VALUE)
                    break;

                if (pls->seek_stream_index < 0 ||
                    pls->seek_stream_index == pls->pkt.stream_index) {

                    if (pls->pkt.dts == AV_NOPTS_VALUE) {
                        pls->seek_timestamp = AV_NOPTS_VALUE;
                        break;
                    }

                    tb = get_timebase(pls);
                    ts_diff = av_rescale_rnd(pls->pkt.dts, AV_TIME_BASE,
                                            tb.den, AV_ROUND_DOWN) -
                            pls->seek_timestamp;
                    if (ts_diff >= 0 && (pls->seek_flags  & AVSEEK_FLAG_ANY ||
                                        pls->pkt.flags & AV_PKT_FLAG_KEY)) {
                        pls->seek_timestamp = AV_NOPTS_VALUE;
                        break;
                    }
                }
                //av_packet_unref(&pls->pkt);
                av_free_packet(&pls->pkt);
                reset_packet(&pls->pkt);
            }
        }
        /* Check if this stream has the packet with the lowest dts */
        if (pls->pkt.data) {
            struct playlist *minpls = minplaylist < 0 ?
                                     NULL : c->playlists[minplaylist];
            if (minplaylist < 0) {
                minplaylist = i;
            } else {
                int64_t dts     =    pls->pkt.dts;
                int64_t mindts  = minpls->pkt.dts;

                if (dts == AV_NOPTS_VALUE ||
                    (mindts != AV_NOPTS_VALUE && compare_ts_with_wrapdetect(dts, pls, mindts, minpls) < 0))
                    minplaylist = i;
            }
        }
    }

    /* If we got a packet, return it */
    if (minplaylist >= 0) {
        struct playlist *pls = c->playlists[minplaylist];

        ret = update_streams_from_subdemuxer(s, pls);
        if (ret < 0) {
            //av_packet_unref(&pls->pkt);
            av_free_packet(&pls->pkt);
            reset_packet(&pls->pkt);
            return ret;
        }

        /* check if noheader flag has been cleared by the subdemuxer */
        if (pls->has_noheader_flag && !(pls->ctx->ctx_flags & AVFMTCTX_NOHEADER)) {
            pls->has_noheader_flag = 0;
            update_noheader_flag(s);
        }

        if (pls->pkt.stream_index >= pls->n_main_streams) {
            av_log(s, AV_LOG_ERROR, "stream index inconsistency: index %d, %d main streams, %d subdemuxer streams\n",
                   pls->pkt.stream_index, pls->n_main_streams, pls->ctx->nb_streams);
            //av_packet_unref(&pls->pkt);
            av_free_packet(&pls->pkt);
            reset_packet(&pls->pkt);
            return AVERROR(EAGAIN);
            //return AVERROR_BUG;
        }

        *pkt = pls->pkt;
        pkt->stream_index = pls->main_streams[pls->pkt.stream_index]->index;
        reset_packet(&c->playlists[minplaylist]->pkt);

        if (pkt->dts != AV_NOPTS_VALUE)
            c->cur_timestamp = av_rescale_q(pkt->dts,
                                            pls->ctx->streams[pls->pkt.stream_index]->time_base,
                                            AV_TIME_BASE_Q);

        LOGI("[%s:%d] Exit read packet \n", __FUNCTION__, __LINE__);
        return 0;
    }
    LOGI("[%s:%d] Exit read packet \n", __FUNCTION__, __LINE__);
    return AVERROR_EOF;

}

int hls_session_close(struct hls_session *c)
{
    free_playlist_list(c);
    free_variant_list(c);
    free_rendition_list(c);

    av_dict_free(&c->avio_opts);
    return 0;
}

int hls_session_read_seek(struct hls_session *c, int stream_index, int64_t timestamp, int flags)
{
    AVFormatContext *s = (AVFormatContext *)c->ctx;
    struct playlist *seek_pls = NULL;
    int i, seq_no;
    int j;
    int stream_subdemuxer_index;
    int64_t first_timestamp, seek_timestamp, duration;

    if ((flags & AVSEEK_FLAG_BYTE) ||
        !(c->variants[0]->playlists[0]->finished || c->variants[0]->playlists[0]->type == PLS_TYPE_EVENT))
        return AVERROR(ENOSYS);

    first_timestamp = c->first_timestamp == AV_NOPTS_VALUE ?
                      0 : c->first_timestamp;

    seek_timestamp = av_rescale_rnd(timestamp, AV_TIME_BASE,
                                    s->streams[stream_index]->time_base.den,
                                    flags & AVSEEK_FLAG_BACKWARD ?
                                    AV_ROUND_DOWN : AV_ROUND_UP);

    duration = s->duration == AV_NOPTS_VALUE ?
               0 : s->duration;

    if (0 < duration && duration < seek_timestamp - first_timestamp)
        return AVERROR(EIO);

    /* find the playlist with the specified stream */
    for (i = 0; i < c->n_playlists; i++) {
        struct playlist *pls = c->playlists[i];
        for (j = 0; j < pls->n_main_streams; j++) {
            if (pls->main_streams[j] == s->streams[stream_index]) {
                seek_pls = pls;
                stream_subdemuxer_index = j;
                break;
            }
        }
    }
    /* check if the timestamp is valid for the playlist with the
     * specified stream index */
    if (!seek_pls || !find_timestamp_in_playlist(c, seek_pls, seek_timestamp, &seq_no))
        return AVERROR(EIO);

    /* set segment now so we do not need to search again below */
    seek_pls->cur_seq_no = seq_no;
    seek_pls->seek_stream_index = stream_subdemuxer_index;

    for (i = 0; i < c->n_playlists; i++) {
        /* Reset reading */
        struct playlist *pls = c->playlists[i];
        if (pls->input) {
            //ff_format_io_close(pls->parent, &pls->input);
            avio_close(pls->input);
            pls->input = NULL;
        }
        //av_packet_unref(&pls->pkt);
        av_free_packet(&pls->pkt);
        reset_packet(&pls->pkt);
        pls->pb.eof_reached = 0;
        /* Clear any buffered data */
        pls->pb.buf_end = pls->pb.buf_ptr = pls->pb.buffer;
        /* Reset the pos, to let the mpegts demuxer know we've seeked. */
        pls->pb.pos = 0;
        /* Flush the packet queue of the subdemuxer. */
        ff_read_frame_flush(pls->ctx);

        pls->seek_timestamp = seek_timestamp;
        pls->seek_flags = flags;

        if (pls != seek_pls) {
            /* set closest segment seq_no for playlists not handled above */
            find_timestamp_in_playlist(c, pls, seek_timestamp, &pls->cur_seq_no);
            /* seek the playlist to the given position without taking
             * keyframes into account since this playlist does not have the
             * specified stream where we should look for the keyframes */
            pls->seek_stream_index = -1;
            pls->seek_flags |= AVSEEK_FLAG_ANY;
        }
    }

    c->cur_timestamp = seek_timestamp;

    return 0;

}
