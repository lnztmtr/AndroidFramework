#ifndef _UMS_H_
#define _UMS_H_

#include <utils/List.h>

// actually, we judge the UMS device by uevent of 'DEVTYPE=usb_interface'
class UmsDevice {
public:
    const static char MASS_STORAGE_CLASS_ID = '8';
    UmsDevice(const char *path);
    ~UmsDevice();
    bool isInUmsDevPath(const char *path);
    bool isUmsDev(char *interface);
    void setUmsDevPath(char *path);
    bool isUmsDevPath(const char *path);
    const char* getUmsDevPath();
private:
    char *mUmsDevPath;
};

typedef android::List<UmsDevice *> UmsCollection;

#endif
