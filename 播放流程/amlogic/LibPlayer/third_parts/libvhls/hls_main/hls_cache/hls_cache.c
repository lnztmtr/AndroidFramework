// author: amlogic mm team

#define LOG_NDEBUG 0
#define LOG_TAG "HLS_CACHE"

#include "hls_common.h"
#include "hls_utils.h"

#include "hls_cache.h"

static int cache_valid_check(struct hls_cache *cache)
{
    if (!cache) {
        LOGV("cache not valid: cache null");
        return 0;
    }
    if (cache->mode == HLS_CACHE_MODE_RAW && !cache->hls_fifo) {
        LOGV("cache not valid: raw mode with no fifo");
        return 0;
    }
    return 1;
}

static enum hls_cache_mode cache_get_mode(struct hls_cache *cache)
{
    return cache->mode;
}

static uint8_t*  cache_get_fifo_rp(struct hls_cache *cache)
{
    return cache->hls_fifo->rptr;
}

static uint8_t*  cache_get_fifo_wp(struct hls_cache *cache)
{
    return cache->hls_fifo->wptr;
}

static int  cache_get_fifo_size(struct hls_cache *cache)
{
    return hls_fifo_size(cache->hls_fifo);
}

static int  cache_get_fifo_space(struct hls_cache *cache)
{
    return hls_fifo_space(cache->hls_fifo);
}

static int  cache_get_fifo_level(struct hls_cache *cache)
{
    return cache_get_fifosize(cache) - cache_get_fifo_space(cache);
}

static struct cache_item* cache_peek_item_head(struct hls_cache *cache)
{
    if (list_empty(&cache->item_list.list)) {
        return NULL;
    }
    return list_first_entry(&cache->item_list.list, struct cache_item, list);
}

// NOT IMPL
static struct cache_item* cache_peek_item_tail(struct hls_cache *cache)
{
    return NULL;
}

static void cache_item_clean_room(struct hls_cache *cache)
{
    if (cache->mode != HLS_CACHE_MODE_ITEM) {
        return;
    }

    int need_clean = 0;
    int reserve = cache->item_list.reserve;
    int max = cache->item_list.max_size;
    if (cache->item_list.ctrl_reserve_mode == 0) { // ratio
        if (reserve >= (max / 100)*cache->item_list.ctrl_reserve_ratio) {
            need_clean = 1;
        }
    } else {
        if (reserve >= cache->item_list.ctrl_reserve_size) {
            need_clean = 1;
        }
    }
    if (!need_clean) {
        return;
    }

    LOGV("reserve:%d thres:%d \n", reserve, (max / 100)*cache->item_list.ctrl_reserve_ratio);

    struct cache_item *first = list_first_entry(&cache->item_list.list, struct cache_item, list);
    if (first == cache->item_list.read_item) {
        return;
    }
    // remove item
    LOGV("Remove index:%d cache item.\n", first->index);
    cache->item_list.reserve -= first->real_size;
    cache->item_list.total -= first->real_size;
    list_del(&first->list);
    if (first->fifo) {
        hls_fifo_free(first->fifo);
    }
    free(first);
    return;
}

struct hls_cache *hls_cache_create(unsigned int size, enum hls_cache_mode mode)
{
    if (size == 0) {
        return NULL;
    }
    struct hls_cache *cache = (struct hls_cache *)malloc(sizeof(struct hls_cache));
    if (!cache) {
        return NULL;
    }
    memset(cache, 0, sizeof(*cache));

    if (mode == HLS_CACHE_MODE_RAW) {
        cache->hls_fifo = hls_fifo_alloc(size);
        if (!cache->hls_fifo) {
            return NULL;
        }
    } else if (mode == HLS_CACHE_MODE_ITEM) {
        cache->item_list.max_size = size;
        cache->item_list.read_item = cache->item_list.write_item = NULL;
    }
    cache->mode = mode;
    INIT_LIST_HEAD(&cache->item_list.list);
    hls_cache_lock_init(&cache->mutex, NULL);
    LOGV("cache create. cache_max_size:%u(bytes) mode:%d \n", size, mode);
    return cache;
}

/* Not impl */
int hls_cache_insert_item_head(struct hls_cache *cache, struct cache_item *item)
{
    return 0;
}

int hls_cache_insert_item_tail(struct hls_cache *cache, struct cache_item *item)
{
    if (cache_valid_check(cache) <= 0) {
        return -1;
    }
    if (cache_get_mode(cache) != HLS_CACHE_MODE_ITEM) {
        LOGV("[%s:%d] insert item failed, not in item mode \n", __FUNCTION__, __LINE__);
        return -1;
    }
    int ret = 0;
    hls_cache_lock(&cache->mutex);
    INIT_LIST_HEAD(&item->list);
    list_add_tail(&item->list, &cache->item_list.list);
    if (!cache->item_list.read_item) {
        cache->item_list.read_item = item;
    }
    cache->item_list.write_item = item;
    hls_cache_unlock(&cache->mutex);
    LOGV("insert item ok. index:%d\n", item->index);
end:
    return 0;
}

int hls_cache_remove_item_head(struct hls_cache *cache, struct cache_item *item)
{
    if (cache_valid_check(cache) <= 0) {
        return -1;
    }
    if (cache_get_mode(cache) != HLS_CACHE_MODE_ITEM) {
        LOGV("[%s:%d] insert item failed, not in item mode \n");
        return -1;
    }
    int ret = 0;
    hls_cache_lock(&cache->mutex);
    list_del(&item->list);
    hls_cache_unlock(&cache->mutex);
end:
    return 0;
}

/* Not impl */
int hls_cache_remove_item_tail(struct hls_cache *cache, struct cache_item *item)
{
    if (cache_valid_check(cache) <= 0) {
        return -1;
    }
    if (cache_get_mode(cache) != HLS_CACHE_MODE_ITEM) {
        LOGV("[%s:%d] insert item failed, not in item mode \n");
        return -1;
    }
    int ret = 0;
    hls_cache_lock(&cache->mutex);

    if (item == cache->item_list.read_item) {
        // both point to last segment
        if (item->list.prev) {
            cache->item_list.read_item = list_entry(item->list.prev, struct cache_item, list);
        }
        cache->item_list.write_item = NULL;
    }
    // remove item
    LOGV("Remove index:%d cache item.\n", item->index);
    cache->item_list.reserve -= item->real_size;
    cache->item_list.total -= item->real_size;
    list_del(&item->list);
    if (item->fifo) {
        hls_fifo_free(item->fifo);
    }
    free(item);

    hls_cache_unlock(&cache->mutex);
end:
    return 0;
}
int hls_cache_read(struct hls_cache *cache, uint8_t *buf, unsigned int size)
{
    if (cache_valid_check(cache) == 0) {
        return -1;
    }

    hls_cache_lock(&cache->mutex);
    int ret = 0;
    // get read item

    if (cache->mode == HLS_CACHE_MODE_ITEM) {
        struct cache_item_list *items = &cache->item_list;
        struct cache_item *item = items->read_item;
        if (!item) {
            goto read_end;
        }

        int fifo_lev = (int)hls_fifo_size(item->fifo);
        if (fifo_lev == 0) { // maybe need to jump to next item
            // next segment exists
            if (item == items->write_item) {
                goto read_end;
            }
            item = items->read_item = list_entry(item->list.next, struct cache_item, list);
            LOGV("switch to next segment :%d \n", item->index);
        }

        fifo_lev = (int)hls_fifo_size(item->fifo);
        int read_length = HLSMIN(size, fifo_lev);
        if (read_length == 0) {
            LOGV("date pool empty :%d - %d\n", size, fifo_lev);
            goto read_end;
        }

        hls_fifo_generic_read(item->fifo, buf, read_length, NULL);
        ret = read_length;
        items->level -= ret;
        items->reserve += ret;
#if 0
        // dump mode
        {
            char name[1024];
            sprintf(name, "/data/tmp/%d_read.ts", item->index);
            FILE *fp = fopen(name, "ab+");
            if (fp) {
                int wlen = fwrite(buf, 1, read_length, fp);
                if (wlen != size) {
                    LOGV("Dump data error.");
                }
                fclose(fp);
            }
        }
#endif


    } else if (cache->mode == HLS_CACHE_MODE_RAW) {
        // not support yet
        LOGV("read Error enterance");
    }
read_end:
    //LOGV("cache read :%d bytes\n", ret);
    hls_cache_unlock(&cache->mutex);
    return ret;
}

int hls_cache_write(struct hls_cache *cache, uint8_t *buf, unsigned int size)
{
    if (cache_valid_check(cache) == 0) {
        return -1;
    }
    int ret = 0;
    hls_cache_lock(&cache->mutex);
    if (cache->mode == HLS_CACHE_MODE_ITEM) {
        cache_item_clean_room(cache);
    }
    if (cache->mode == HLS_CACHE_MODE_ITEM) {
        struct cache_item_list *items = &cache->item_list;
        struct cache_item *item = items->write_item;
        assert(item == NULL);
        if (hls_fifo_space(item->fifo) >= size) {
            ret = hls_fifo_generic_write(item->fifo, buf, size, NULL);
        }
        item->real_size += ret;
        items->level += ret;
        items->total += ret;
#if 0
        // dump mode
        {
            char name[1024];
            sprintf(name, "/data/tmp/%d_write.ts", item->index);
            FILE *fp = fopen(name, "ab+");
            if (fp) {
                int wlen = fwrite(buf, 1, size, fp);
                if (wlen != size) {
                    LOGV("Dump data error.");
                }
                fclose(fp);
            }
        }
#endif
        if (item->real_size == item->filesize) {
            LOGV("cache write ok:[%d %d] [realsize:%lld filesize:%lld] \n", ret, size, item->real_size, item->filesize);
        }
    } else if (cache->mode == HLS_CACHE_MODE_RAW) {
        // not support yet
    }
    hls_cache_unlock(&cache->mutex);
    return ret;
}

int hls_cache_seek(struct hls_cache *cache, int64_t seekTimeUs)
{
    if (cache_valid_check(cache) == 0) {
        return -1;
    }

    int ret = 0;
    hls_cache_lock(&cache->mutex);
    struct cache_item_list *items = &cache->item_list;
    struct cache_item *item;
    struct list_head *list1, *list2;

    // First reset segment read pointer
    list_for_each_safe(list1, list2, &items->list) {
        item = list_entry(list1, struct cache_item, list);
        item->fifo->rptr = item->fifo->buffer;
        item->fifo->rndx = 0;
        LOGV("Segment fifo index-size:[%d:%d] \n", item->index, (int)hls_fifo_size(item->fifo));
    }

    // Find match pos
    items->reserve = 0;
    list_for_each_safe(list1, list2, &items->list) {
        item = list_entry(list1, struct cache_item, list);
        if (seekTimeUs >= item->start_us && seekTimeUs <= item->end_us) {
            ret = 1;
            items->read_item = item;
            items->level = items->total - items->reserve;
            // update reserve-level-totel count
            LOGV("Find match segment. seekTimeUs:%lld. index:%d [%lld:%lld]", seekTimeUs, item->index, item->start_us, item->end_us);
            break;
        } else {
            items->reserve += item->real_size;
        }
    }
    hls_cache_unlock(&cache->mutex);

    return ret;
}

void hls_cache_reset(struct hls_cache *cache)
{
    if (cache_valid_check(cache) == 0) {
        return;
    }
    hls_cache_lock(&cache->mutex);
    if (cache->mode == HLS_CACHE_MODE_ITEM) {
        struct cache_item_list *items = &cache->item_list;
        struct cache_item *item;
        struct list_head *list1, *list2;
        list_for_each_safe(list1, list2, &items->list) {
            item = list_entry(list1, struct cache_item, list);
            list_del(&item->list);
            if (item->fifo) {
                hls_fifo_free(item->fifo);
            }
            free(item);
        }
        items->reserve = items->level = items->total = 0;
        items->read_item = items->write_item = NULL;
    } else {
        // Fixme
    }

    hls_cache_unlock(&cache->mutex);
    LOGV("cache release finish.\n");
    return;
}

int hls_cache_release(struct hls_cache *cache)
{
    if (cache_valid_check(cache) == 0) {
        return -1;
    }
    hls_cache_reset(cache);
    if (cache) {
        free(cache);
    }
    LOGV("cache release finish.\n");
    return 0;
}

int hls_cache_get_info(struct hls_cache *cache, enum HLS_CACHE_CMD cmd, unsigned long arg)
{
    if (cache_valid_check(cache) == 0) {
        return -1;
    }
    switch (cmd) {
    case HLS_CACHE_CMD_GET_LEVEL: {
        if (cache->mode == HLS_CACHE_MODE_ITEM) {
            *(int64_t *) arg = cache->item_list.level;
        }
        break;
    }
    default:
        break;
    }
    return 0;
}

int hls_cache_set_info(struct hls_cache *cache, enum HLS_CACHE_CMD cmd, unsigned long arg)
{
    if (cache_valid_check(cache) == 0) {
        return -1;
    }
    switch (cmd) {
    case HLS_CACHE_CMD_SET_RESERVE_MODE:
        if (cache->mode == HLS_CACHE_MODE_ITEM) {
            cache->item_list.ctrl_reserve_mode = *(int *)arg;
        }
        break;
    case HLS_CACHE_CMD_SET_RESERVE_SIZE:
        if (cache->mode == HLS_CACHE_MODE_ITEM) {
            cache->item_list.ctrl_reserve_size = *(int *)arg;
        }
        break;
    case HLS_CACHE_CMD_SET_RESERVE_RATIO:
        if (cache->mode == HLS_CACHE_MODE_ITEM) {
            cache->item_list.ctrl_reserve_ratio = *(int *)arg;
        }
        break;
    default:
        break;
    }
    return 0;
}

void hls_cache_dump(struct hls_cache *cache)
{
    LOGV("==================================");
    LOGV("| MODE: %s \n", (cache->mode == HLS_CACHE_MODE_RAW) ? "RAW MODE" : "ITEM MODE");
    if (cache->mode == HLS_CACHE_MODE_RAW) {
        LOGV("space:%d level:%d size:%d",
             hls_fifo_space(cache->hls_fifo),
             hls_fifo_size(cache->hls_fifo),
             hls_fifo_size(cache->hls_fifo) + hls_fifo_space(cache->hls_fifo)
            );
    } else {
        struct cache_item_list *items = &cache->item_list;
        LOGV("level:%d", items->level);
        LOGV("reserve:%d [mode:%d size:%d ratio:%d/100]", items->reserve, items->ctrl_reserve_mode, items->ctrl_reserve_size, items->ctrl_reserve_ratio);
        LOGV("total:%d", items->total);
        LOGV("max:%d", items->max_size);
        struct cache_item *item = cache_peek_item_head(cache);
        if (item) {
            LOGV("first item:%d", item->index);
        }
        item = items->read_item;
        if (item) {
            LOGV("read item:%d", item->index);
        }
        item = items->write_item;
        if (item) {
            LOGV("write item:%d", item->index);
        }

    }
    LOGV("==================================");
    return;
}
