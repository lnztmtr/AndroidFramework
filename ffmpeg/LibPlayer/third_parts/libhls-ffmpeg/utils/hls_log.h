#ifndef HLS_LOG_H_
#define HLS_LOG_H_

#include <stdio.h>
#include <stdarg.h>

#include <utils/Log.h>
#include <ctype.h>
#ifdef __cplusplus
extern "C" {
#endif

#ifndef LOG_TAG
#define LOG_TAG "hls-ffmpeg-mod"
#endif
#ifndef LOGV
#define LOGV ALOGV
#endif

#ifndef LOGI
#define LOGI ALOGI

#endif

#ifndef LOGW
#define LOGW ALOGW

#endif

#ifndef LOGE
#define LOGE ALOGE

#endif

#define TRACE()  LOGI("TARCE:%s:%d\n",__FUNCTION__,__LINE__);

#ifdef __cplusplus
}
#endif

#endif
