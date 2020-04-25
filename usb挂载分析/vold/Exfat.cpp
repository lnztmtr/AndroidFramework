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

#include <stdio.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/mman.h>
#include <sys/mount.h>

#include <linux/kdev_t.h>

#define LOG_TAG "Vold"

#include <cutils/log.h>
#include <cutils/properties.h>

#include "Exfat.h"

#ifdef HAS_EXFAT_FUSE
static char FSCK_EXFAT_PATH[] = "/system/bin/fsck.exfat";
static char MKEXFAT_PATH[] = "/system/bin/mkfs.exfat";
static char MOUNT_EXFAT_PATH[] = "/system/bin/mount.exfat";
#endif /* HAS_EXFAT_PATH */
extern "C" int logwrap(int argc, const char **argv, int background);
extern "C" int mount(const char *, const char *, const char *, unsigned long, const void *);

int Exfat::check(const char *fsPath) {
#ifdef HAS_EXFAT_FUSE
    bool rw = true;
    if (access(FSCK_EXFAT_PATH, X_OK)) {
        SLOGW("Skipping exfat checks\n");
        return 0;
    }

    int pass = 1;
    int rc = 0;
    do {
        const char *args[3];
        args[0] = FSCK_EXFAT_PATH;
        args[1] = fsPath;
        args[2] = NULL;

        rc = logwrap(2, args, 1);

        switch(rc) {
        case 0:
            SLOGI("exFAT check completed OK");
            return 0;

        case 2:
            SLOGE("Filesystem check failed (not a EXFAT filesystem)");
            errno = ENODATA;
            return -1;

        default:
            SLOGE("Filesystem check failed (unknown exit code %d)", rc);
            errno = EIO;
            return -1;
        }
    } while (0);

    return 0;
#else	
    SLOGW("Skipping exFAT check\n");
    return 0;
#endif
}

int Exfat::doMount(const char *fsPath, const char *mountPoint,
                 bool ro, bool remount, int ownerUid, int ownerGid,
                 int permMask, bool createLost) {
/* kernel 3.14 disable exfat mout default */
#ifdef HAS_EXFAT_FUSE
    int rc;
    const char *args[4];
 
    args[0] = MOUNT_EXFAT_PATH;
    args[1] = fsPath;
    args[2] = mountPoint;
    args[3] = NULL;
    rc = logwrap(3, args, 1);
#else
				 
    int rc;
    unsigned long flags;
    char mountData[255];

    flags = MS_NODEV | MS_NOEXEC | MS_NOSUID | MS_DIRSYNC;

    flags |= (ro ? MS_RDONLY : 0);
    flags |= (remount ? MS_REMOUNT : 0);

    sprintf(mountData,
            "uid=%d,gid=%d,fmask=%o,dmask=%o",
            ownerUid, ownerGid, permMask, permMask);

    rc = mount(fsPath, mountPoint, "exfat", flags, mountData);

    if (rc && errno == EROFS) {
        SLOGE("%s appears to be a read only filesystem - retrying mount RO", fsPath);
        flags |= MS_RDONLY;
        rc = mount(fsPath, mountPoint, "exfat", flags, mountData);
    }
#endif
    return rc;
}

int Exfat::format(const char *fsPath, unsigned int numSectors) {
#ifdef HAS_EXFAT_FUSE
    const char *args[11];
    int rc;
    args[0] = MKEXFAT_PATH;
    
    if (numSectors) {
        char tmp[32];
        snprintf(tmp, sizeof(tmp), "%u", numSectors);
        const char *size = tmp;
        args[1] = "-s";
        args[2] = size;
        args[3] = fsPath;
        args[4] = NULL;
        rc = logwrap(4, args, 1);
    } else {
        args[7] = fsPath;
        args[8] = NULL;
        rc = logwrap(9, args, 1);
    }

    if (rc == 0) {
        SLOGI("Filesystem exfat formatted OK");
        return 0;
    } else {
        SLOGE("Format exfat failed (unknown exit code %d)", rc);
        errno = EIO;
        return -1;
    }
#else	
    SLOGE("Skipping exFAT format\n");
    errno = EIO;
    return -1;
#endif
}
