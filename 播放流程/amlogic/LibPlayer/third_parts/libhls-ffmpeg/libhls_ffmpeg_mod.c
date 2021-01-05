#include "ammodule.h"
#include "libavformat/url.h"
#include "libavformat/avformat.h"
#include <android/log.h>
#define  LOG_TAG    "libhls_ffmpeg_mod"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)


ammodule_methods_t  libhls_ffmpeg_module_methods;
ammodule_t AMPLAYER_MODULE_INFO_SYM = {
tag:
    AMPLAYER_MODULE_TAG,
version_major:
    AMPLAYER_API_MAIOR,
version_minor:
    AMPLAYER_API_MINOR,
    id: 0,
name: "hls_ffmpeg_mod"
    ,
author: "Amlogic"
    ,
descript: "libhls_ffmpeg module binding library"
    ,
methods:
    &libhls_ffmpeg_module_methods,
dso :
    NULL,
reserved :
    {0},
};

extern AVInputFormat aml_hls_demuxer; // hls demuxer for media group

int libhls_ffmpeg_mod_init(const struct ammodule_t* module, int flags)
{
    LOGI("libhls_ffmpeg module init\n");
    av_register_input_format(&aml_hls_demuxer);
    return 0;
}


int libhls_ffmpeg_mod_release(const struct ammodule_t* module)
{
    LOGI("libhls_ffmpeg module release\n");
    return 0;
}


ammodule_methods_t  libhls_ffmpeg_module_methods = {
    .init =  libhls_ffmpeg_mod_init,
    .release =   libhls_ffmpeg_mod_release,
} ;
