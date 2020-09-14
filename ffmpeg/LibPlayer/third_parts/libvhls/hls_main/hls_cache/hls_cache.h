#ifndef HLS_CACHE_H
#define HLS_CACHE_H

#include <pthread.h>
#include <assert.h>

#include "hls_list.h"
#include "hls_fifo.h"

enum hls_cache_mode {
    HLS_CACHE_MODE_RAW = 0,
    HLS_CACHE_MODE_ITEM = 1
};

struct cache_item {
    struct list_head list;
    const char *url;
    int64_t filesize;
    int64_t real_size;
    int index;
    int64_t duration_us;
    int64_t start_us;
    int64_t end_us;

    HLSFifoBuffer *fifo;
    int valid;
};

struct cache_item_list {
    struct list_head list;
    int reserve;  // reserve count, used to seek backward
    int level;    // data count can be read
    int total;    // ALL memory used
    int max_size; // reserve + level > max_size, need to remove reserve items
    struct cache_item *read_item;  // pointer to current reading item
    struct cache_item *write_item; // pointer to current writing item

    int ctrl_reserve_mode;
    int ctrl_reserve_size;
    int ctrl_reserve_ratio;
};

#define hls_cache_lock_t             pthread_mutex_t
#define hls_cache_lock_init(x,v)     pthread_mutex_init(x,v)
#define hls_cache_lock(x)            pthread_mutex_lock(x)
#define hls_cache_unlock(x)          pthread_mutex_unlock(x)

struct hls_cache {
    HLSFifoBuffer *hls_fifo; // raw mode
    struct cache_item_list item_list;
    enum hls_cache_mode mode;
    hls_cache_lock_t mutex;
};

/**
 * Create hls cache structure with fifo size and cache mode parameter
 * comes from user
 *
 * @param size FIFO Size
 * @param mode cache mode
 *
 * @return cache ontext for success, NULL otherwise
 *
 */
struct hls_cache *hls_cache_init(unsigned int size, enum hls_cache_mode mode);

/**
 * Insert a cache item in header of list
 *
 * @param cache cache context
 *
 * @return 0 for success, negative errorcode otherwise
 *
 */
int hls_cache_insert_item_head(struct hls_cache *cache, struct cache_item *item);

/**
 * Insert a cache item in tail of list
 *
 * @param cache cache context
 *
 * @return 0 for success, negative errorcode otherwise
 *
 */
int hls_cache_insert_item_tail(struct hls_cache *cache, struct cache_item *item);

/**
 * Remove first cache item of list.
 *
 * @param cache cache context
 * @param item  item to be removed
 *
 * @return 0 for success, negative errorcode otherwise
 *
 */
int hls_cache_remove_item_head(struct hls_cache *cache, struct cache_item *item);

/**
 * Remove last cache item of list.
 *
 * @param cache cache context
 * @param item  item to be removed
 *
 * @return 0 for success, negative errorcode otherwise
 *
 */
int hls_cache_remove_item_tail(struct hls_cache *cache, struct cache_item *item);

/**
 * Read data from hls cache to user provided buffer.
 * need update cache item info in HLS_CACHE_MODE_ITEM mode
 *
 * @param cache cache context
 * @param buf   user provide buffer
 * @param size  size to read
 *
 * @return number of bytes read from the fifo
 *
 */
int hls_cache_read(struct hls_cache *cache, uint8_t *buf, unsigned int size);

/**
 * Write data from user provided buffer to hls cache.
 * need update cache item info in HLS_CACHE_MODE_ITEM mode
 *
 * @param cache cache context
 * @param buf   user provide buffer
 * @param size  size to write
 *
 * @return number of bytes written to the fifo
 *
 */
int hls_cache_write(struct hls_cache *cache, uint8_t *buf, unsigned int size);

//int hls_cache_peek(struct hls_cache *cache, int timestamp);


enum HLS_CACHE_CMD {
    HLS_CACHE_CMD_UNKOWN = -1,

    HLS_CACHE_CMD_GET_LEVEL,
    HLS_CACHE_CMD_GET_RESERVE,
    HLS_CACHE_CMD_GET_TOTAL,
    HLS_CACHE_CMD_GET_MODE,


    HLS_CACHE_CMD_SET_RESERVE_MODE,
    HLS_CACHE_CMD_SET_RESERVE_RATIO,
    HLS_CACHE_CMD_SET_RESERVE_SIZE,

    HLS_CACHE_CMD_MAX
};

/**
 * Query info about hls cache
 *
 * @param cache cache context
 * @param cmd
 * @param arg
 *
 * @return
 *
 */
int hls_cache_get_info(struct hls_cache *cache, enum HLS_CACHE_CMD cmd, unsigned long arg);

/**
 * Release hls cache structure: FIFO & cache list items
 *
 * @param cache      cache context
 * @param seektimeUs seek point
 *
 * @return 1 for success, negative errorcode otherwise
 *
 */
int hls_cache_seek(struct hls_cache *cache, int64_t seektimeUs);


/**
 * Reset hls cache structure: FIFO & cache list items
 *
 * @param cache cache context
 *
 * @return 0 for success, negative errorcode otherwise
 *
 */
void hls_cache_reset(struct hls_cache *cache);

/**
 * Release hls cache structure: FIFO & cache list items
 *
 * @param cache cache context
 *
 * @return 0 for success, negative errorcode otherwise
 *
 */
int hls_cache_release(struct hls_cache *cache);

/**
 * Query hls cache
 *
 * @param cache cache context
 * @param cmd
 * @param arg
 *
 * @return
 *
 */
int hls_cache_get_info(struct hls_cache *cache, enum HLS_CACHE_CMD cmd, unsigned long arg);

/**
 * Config hls cache
 *
 * @param cache cache context
 * @param cmd
 * @param arg
 *
 * @return
 *
 */
int hls_cache_set_info(struct hls_cache *cache, enum HLS_CACHE_CMD cmd, unsigned long arg);

/**
 * Dump hls cache Info: FIFO & cache list items
 *
 * @param cache cache context
 *
 */
void hls_cache_dump(struct hls_cache *cache);

#endif
