#include "hls_cache.h"

int main(int argc, char **argv)
{
    unsigned int size = 10 * 1024 * 1024;
    enum hls_cache_mode mode = HLS_CACHE_MODE_ITEM;
    struct hls_cache *cache = hls_cache_create(size, mode);
    hls_cache_release(cache);
    return 0;
}
