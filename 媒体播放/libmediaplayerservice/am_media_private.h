#ifndef AMLOGIC_MEDIA_PRIVATE_H__
#define AMLOGIC_MEDIA_PRIVATE_H__

#ifndef _LIBS_UTILS_LOG_H
    #error "include this file after  including utils/Log.h"
#endif

#ifndef LOGD
    #define LOGV ALOGV
    #define LOGD ALOGD
    #define LOGI ALOGI
    #define LOGW ALOGW
    #define LOGE ALOGE
#endif

#endif // AMLOGIC_MEDIA_PRIVATE_H__
