
#include <string.h>
#include <malloc.h>

#include "Ums.h"

UmsDevice::UmsDevice(const char *path) {
    mUmsDevPath = strdup(path);
}

UmsDevice::~UmsDevice() {
    if(mUmsDevPath) {
        free(mUmsDevPath);
        mUmsDevPath = NULL;
    }
}

bool UmsDevice::isInUmsDevPath(const char *path) {
    if(!strncmp(mUmsDevPath, path, strlen(mUmsDevPath)))
		return true;
	else
        return false;
}

void UmsDevice::setUmsDevPath(char *path) {
    if(mUmsDevPath) {
        free(mUmsDevPath);
        mUmsDevPath = NULL;
    }
    mUmsDevPath = strdup(path);
}

bool UmsDevice::isUmsDevPath(const char *path) {
    if(!strcmp(mUmsDevPath, path))
		return true;
    else
        return false;
}

const char* UmsDevice::getUmsDevPath() {
    return (const char*)mUmsDevPath;
}

