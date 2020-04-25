/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _VOLUME_H
#define _VOLUME_H

#include <utils/List.h>
#include <fs_mgr.h>

#define MAX_PARTS 16
#define FUNCTION_UMS_PARTITION

class NetlinkEvent;
class VolumeManager;
class UmsDevice;

enum eVolumeType {
    VOLUME_TYPE_UNKNOWN,
    VOLUME_TYPE_SDCARD,
    VOLUME_TYPE_FLASH,
    VOLUME_TYPE_UMS,
    VOLUME_TYPE_SATA,
#ifdef HAS_VIRTUAL_CDROM
    VOLUME_TYPE_LOOP,
#endif
};

class Volume {
private:
    int mState;
    int mFlags;

public:
    static const int State_Init       = -1;
    static const int State_NoMedia    = 0;
    static const int State_Idle       = 1;
    static const int State_Pending    = 2;
    static const int State_Checking   = 3;
    static const int State_Mounted    = 4;
    static const int State_Unmounting = 5;
    static const int State_Formatting = 6;
    static const int State_Shared     = 7;
    static const int State_SharedMnt  = 8;
    static const int State_Deleting   = 9;

    static const char *MEDIA_DIR;
    static const char *FUSE_DIR;
    static const char *SEC_STG_SECIMGDIR;
    static const char *SEC_ASECDIR_EXT;
    static const char *SEC_ASECDIR_INT;
    static const char *ASECDIR;
    static const char *LOOPDIR;
    static const char *BLKID_PATH;

    static bool sFakeSdcard;
    static bool sVirtualSdcard;
	   bool mSingleWholeDisk;
#ifdef HAS_VIRTUAL_CDROM
    static bool sLoopMounted;
    static char * mloopmapdir;
    static char * mloopmountdir;
#endif    
protected:
    char *mLabel;
    char *mMountpoint;
    char* mUuid;
    char* mUserLabel;
    VolumeManager *mVm;
    bool mDebug;
    int mPartIdx;
    int mOrigPartIdx;
    eVolumeType mVolumeType;
    unsigned int mMountedPartMap;
    unsigned int mValidPartMap;
    unsigned int mSdcardPartitionBit;
    char mFakeSdcardLink[255];
    bool mNoParts;
    bool mHasAsec;

    static bool sSdcardMounted;
    static bool sVirtualSdcardMounted;
    static bool sFlashMouted;

	 int tryDecrypt;
	 
	 bool mRetryMount;
	 /*
     * The major/minor tuple of the currently mounted filesystem.
     */
     dev_t mCurrentlyMountedKdev;

public:
    Volume(VolumeManager *vm, const char *label, const char *mount_point);
    virtual ~Volume();

    int mountVol();
    int unmountVol(bool force, bool revert);
    int formatVol(bool wipe);
    int formatVolWithFsType(const char *fsType, bool wipe);
    int unmountPart(int part_index);

    const char* getLabel() { return mLabel; }
    const char* getUuid() { return mUuid; }
    const char* getUserLabel() { return mUserLabel; }
    int getState() { return mState; }
    void setVolumeType(eVolumeType type) { mVolumeType = type; }
    void setVolHasAsec(bool bHas){ mHasAsec = bHas; }
    bool getVolHasAsec(){ return mHasAsec; }
    eVolumeType getVolumeType() { return mVolumeType; }
    int getFlags() { return mFlags; };

    /* Mountpoint of the raw volume */
    virtual const char *getMountpoint() = 0;
    virtual const char *getFuseMountpoint() = 0;

    virtual int handleBlockEvent(NetlinkEvent *evt);
    virtual dev_t getDiskDevice();
    virtual dev_t getShareDevice();
    virtual void handleVolumeShared();
    virtual void handleVolumeUnshared();
    virtual int getVolumeShareIdx() = 0;
    virtual int getVolumeMinor(int index) = 0;

    void setDebug(bool enable);
    virtual int getVolInfo(struct volume_info *v) = 0;

    int unmountbeforepart(int curidx);

#ifdef HAS_VIRTUAL_CDROM
    int mountloop(const char *path);
    int unmountloop(bool force);
    int loopsetfd(const char * path);
    int loopclrfd();
#endif

    int getFSLabel(char **outLabel);

protected:
    void setUuid(const char* uuid);
    void setUserLabel(const char* userLabel);
    void setState(int state);
    void setVirtualSdcardState(int oldState, int newState);

    virtual int getDeviceNodes(dev_t *devs, int max) = 0;
    virtual const char *getDeviceNodesLabel(int partition_index) = 0;
    virtual int updateDeviceInfo(char *new_path, int new_major, int new_minor) = 0;
    virtual void revertDeviceInfo(void) = 0;
    virtual int isDecrypted(void) = 0;

    int createDeviceNode(const char *path, int major, int minor);

private:
    int initializeMbr(const char *deviceNode);
    bool isMountpointMounted(const char *path);
    int mountAsecExternal();
    int doUnmount(const char *path, bool force);
    int createBindMounts();
    int doMoveMount(const char *src, const char *dst, bool force);
    int doFsCheck(const char *devicePath);
    int doMount(const char *devicePath, const char *mountpoint);
#ifndef 	FUNCTION_UMS_PARTITION		
    int unmountdisk(char * path);
#endif
    int unmountFakeSdcard();
    NetlinkEvent* newFakeSdcardEvent(int type, int major, int minor);
    int extractMetadata(const char* devicePath);
};

typedef android::List<Volume *> VolumeCollection;

#endif
