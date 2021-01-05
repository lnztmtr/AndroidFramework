#ifndef AMSYSWRITE_UTILS_H
#define AMSYSWRITE_UTILS_H

#ifdef  __cplusplus
extern "C" {
#endif
    int amSystemWriteGetProperty(const char* key, char* value);
    int amSystemWriteGetPropertyStr(const char* key, char* def, char* value);
    int amSystemWriteGetPropertyInt(const char* key, int def);
    long amSystemWriteGetPropertyLong(const char* key, long def);
    int amSystemWriteGetPropertyBool(const char* key, int def);
    void amSystemWriteSetProperty(const char* key, const char* value);
    int amSystemWriteReadSysfs(const char* path, char* value);
    int amSystemWriteReadNumSysfs(const char* path, char* value, int size);
    int amSystemWriteWriteSysfs(const char* path, char* value);

#if ANDROID_PLATFORM_SDK_VERSION >= 21
    int amSystemControlLoopMountUnmount(int isMount, const char *path);
#endif


#ifdef  __cplusplus
}
#endif


#endif
