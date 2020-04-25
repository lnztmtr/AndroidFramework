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
#include <stdlib.h>
#include <errno.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>

#include <fcntl.h>
#include <dirent.h>
#include <fs_mgr.h>

#define LOG_TAG "Vold"

#include "cutils/klog.h"
#include "cutils/log.h"
#include "cutils/properties.h"

#include "VolumeManager.h"
#include "CommandListener.h"
#include "NetlinkManager.h"
#include "DirectVolume.h"
#include "cryptfs.h"

static int process_config(VolumeManager *vm);
static void coldboot(const char *path);
static void set_media_poll_time(void);

#define FSTAB_PREFIX "/fstab."
struct fstab *fstab;

int main() {

    VolumeManager *vm;
    CommandListener *cl;
    NetlinkManager *nm;

    SLOGI("Vold 2.1 (the revenge) firing up");

    mkdir("/dev/block/vold", 0755);
    mkdir("/mnt", 0755);

    /* For when cryptfs checks and mounts an encrypted filesystem */
    klog_set_level(6);

    /* Create our singleton managers */
    if (!(vm = VolumeManager::Instance())) {
        SLOGE("Unable to create VolumeManager");
        exit(1);
    };

    if (!(nm = NetlinkManager::Instance())) {
        SLOGE("Unable to create NetlinkManager");
        exit(1);
    };

    char value[PROPERTY_VALUE_MAX];
    property_get("vold.fakesdcard.enable", value, "0");
    Volume::sFakeSdcard = strcmp(value, "1") ? false : true;

    cl = new CommandListener();
    vm->setBroadcaster((SocketListener *) cl);
    nm->setBroadcaster((SocketListener *) cl);

    if (vm->start()) {
        SLOGE("Unable to start VolumeManager (%s)", strerror(errno));
        exit(1);
    }

    if (process_config(vm)) {
        SLOGE("Error reading configuration (%s)... continuing anyways", strerror(errno));
    }

    if (nm->start()) {
        SLOGE("Unable to start NetlinkManager (%s)", strerror(errno));
        exit(1);
    }

    set_media_poll_time();

    coldboot("/sys/devices");
    coldboot("/sys/block");
//    coldboot("/sys/class/switch");
#ifdef HAS_UMS_SWITCH
    vm->coldbootUmsSwitch();
#endif

    /*
     * Now that we're up, we can respond to commands
     */
    if (cl->startListener()) {
        SLOGE("Unable to start CommandListener (%s)", strerror(errno));
        exit(1);
    }

    // Eventually we'll become the monitoring thread
    while(1) {
        sleep(1000);
    }

    SLOGI("Vold exiting");
    exit(0);
}

static void set_media_poll_time(void)
{
	char mediapollstr[PROPERTY_VALUE_MAX];
	property_get("has.media.poll",  mediapollstr, "false");
	
	if(strcmp(mediapollstr,"true")==0)
	{
		int fd;
		fd = open ("/sys/module/block/parameters/events_dfl_poll_msecs", O_WRONLY);
		if (fd >= 0) {
			write(fd, "2000", 4);
			close (fd);
		}else {
			SLOGE("kernel not support media poll uevent!"); 
		}
	}
	return;
}


static void do_coldboot(DIR *d, int lvl)
{
    struct dirent *de;
    int dfd, fd;

    dfd = dirfd(d);

    fd = openat(dfd, "uevent", O_WRONLY);
    if(fd >= 0) {
        write(fd, "add\n", 4);
        close(fd);
    }

    while((de = readdir(d))) {
        DIR *d2;

        if (de->d_name[0] == '.')
            continue;

        if (de->d_type != DT_DIR && lvl > 0)
            continue;

        fd = openat(dfd, de->d_name, O_RDONLY | O_DIRECTORY);
        if(fd < 0)
            continue;

        d2 = fdopendir(fd);
        if(d2 == 0)
            close(fd);
        else {
            do_coldboot(d2, lvl + 1);
            closedir(d2);
        }
    }
}

static void coldboot(const char *path)
{
    DIR *d = opendir(path);
    if(d) {
        do_coldboot(d, 0);
        closedir(d);
    }
}

#if 1
static int parse_mount_flags(char *mount_flags)
{
    char *save_ptr;
    int flags = 0;

    if (strcasestr(mount_flags, "encryptable")) {
        flags |= VOL_ENCRYPTABLE;
    }

    if (strcasestr(mount_flags, "nonremovable")) {
        flags |= VOL_NONREMOVABLE;
    }

    return flags;
}

static int process_config(VolumeManager *vm) {
    FILE *fp;
    int n = 0;
    char line[512];
    char fstab_filename[PROPERTY_VALUE_MAX + sizeof(FSTAB_PREFIX)];
    char propbuf[PROPERTY_VALUE_MAX];

    property_get("ro.hardware", propbuf, "");
    snprintf(fstab_filename, sizeof(fstab_filename), FSTAB_PREFIX"%s", propbuf);

    fstab = fs_mgr_read_fstab(fstab_filename);

    if (!(fp = fopen("/etc/vold.fstab", "r"))) {
        return -1;
    }

    while(fgets(line, sizeof(line), fp)) {
        const char *delim = " \t";
        char *save_ptr;
        char *type, *label, *mount_point, *mount_flags, *sysfs_path;
        int flags;

        n++;
        line[strlen(line)-1] = '\0';

        if (line[0] == '#' || line[0] == '\0')
            continue;

        if (!(type = strtok_r(line, delim, &save_ptr))) {
            SLOGE("Error parsing type");
            goto out_syntax;
        }
        if (!(label = strtok_r(NULL, delim, &save_ptr))) {
            SLOGE("Error parsing label");
            goto out_syntax;
        }

        if (!strcmp(type, "asec")) {
            SLOGI("reading asec:%s",label);
            vm->setAsecVolume(label);
            continue;
        }

        if (!(mount_point = strtok_r(NULL, delim, &save_ptr))) {
            SLOGE("Error parsing mount point");
            goto out_syntax;
        }

        if (!strcmp(type, "dev_mount")) {
            DirectVolume *dv = NULL;
            char *part;

            if (!(part = strtok_r(NULL, delim, &save_ptr))) {
                SLOGE("Error parsing partition");
                goto out_syntax;
            }
            if (strcmp(part, "auto") && atoi(part) == 0) {
                SLOGE("Partition must either be 'auto' or 1 based index instead of '%s'", part);
                goto out_syntax;
            }

            if (!strcmp(part, "auto")) {
                dv = new DirectVolume(vm, label, mount_point, -1);
            } else {
                dv = new DirectVolume(vm, label, mount_point, atoi(part));
            }

            while ((sysfs_path = strtok_r(NULL, delim, &save_ptr))) {
                if (*sysfs_path != '/') {
                    /* If the first character is not a '/', it must be flags */
                    break;
                }
                if (dv->addPath(sysfs_path)) {
                    SLOGE("Failed to add devpath %s to volume %s", sysfs_path,
                         label);
                    goto out_fail;
                }
            }

            if (!strcmp(label, "sdcard")) {
                dv->setVolumeType(VOLUME_TYPE_SDCARD);
                //dv->setVolHasAsec(true);
            } else if (!strcmp(label, "flash")) {
                dv->setVolumeType(VOLUME_TYPE_FLASH);
            } else if (!strcmp(label, "sata")) {
                dv->setVolumeType(VOLUME_TYPE_SATA);
            } else if (!strcmp(label, "ums")) {
                dv->setVolumeType(VOLUME_TYPE_UMS);
            }

            //mkdir(mount_point, 0755);

            /* If sysfs_path is non-null at this point, then it contains
             * the optional flags for this volume
             */
            if (sysfs_path)
                flags = parse_mount_flags(sysfs_path);
            else
                flags = 0;
            dv->setFlags(flags);

            vm->addVolume(dv);
        } else if (!strcmp(type, "map_mount")) {
        }
#ifdef HAS_VIRTUAL_CDROM
        else if (!strcmp(type, "loop_mount")) {
            DirectVolume *loopdv = NULL;

            if (strcmp(label,"loop")) {
                SLOGE("unvalid label '%s'", label);
                goto out_syntax;
            }

            loopdv = new DirectVolume(vm, label, mount_point, -1);
            loopdv->setVolumeType(VOLUME_TYPE_LOOP);
            vm->addVolume(loopdv);
        }
#endif /* HAS_VIRTUAL_CDROM */
        else {
            SLOGE("Unknown type '%s'", type);
            goto out_syntax;
        }
    }

    fclose(fp);
    return 0;

out_syntax:
    SLOGE("Syntax error on config line %d", n);
    errno = -EINVAL;
out_fail:
    fclose(fp);
    return -1;   
}
#else
static int process_config(VolumeManager *vm)
{
    char fstab_filename[PROPERTY_VALUE_MAX + sizeof(FSTAB_PREFIX)];
    char propbuf[PROPERTY_VALUE_MAX];
    int i;
    int ret = -1;
    int flags;

    property_get("ro.hardware", propbuf, "");
    snprintf(fstab_filename, sizeof(fstab_filename), FSTAB_PREFIX"%s", propbuf);

    fstab = fs_mgr_read_fstab(fstab_filename);
    if (!fstab) {
        SLOGE("failed to open %s\n", fstab_filename);
        return -1;
    }

    /* Loop through entries looking for ones that vold manages */
    for (i = 0; i < fstab->num_entries; i++) {
        if (fs_mgr_is_voldmanaged(&fstab->recs[i])) {
            DirectVolume *dv = NULL;
            flags = 0;

            /* Set any flags that might be set for this volume */
            if (fs_mgr_is_nonremovable(&fstab->recs[i])) {
                flags |= VOL_NONREMOVABLE;
            }
            if (fs_mgr_is_encryptable(&fstab->recs[i])) {
                flags |= VOL_ENCRYPTABLE;
            }
<<<<<<< HEAD
#if 0
            if (!strcmp(type, "asec")) {
                SLOGI("reading asec:%s",label);
                vm->setAsecVolume(label);
                continue;
            }
            if (!strcmp(type, "dev_mount")) {
#endif
            char *label = fstab->recs[i].label;
            if (!strcmp(label, "sdcard")) {
                dv->setVolumeType(VOLUME_TYPE_SDCARD);
                dv->setVolHasAsec(true);
            } else if (!strcmp(label, "flash")) {
                dv->setVolumeType(VOLUME_TYPE_FLASH);
            } else if (!strcmp(label, "sata")) {
                dv->setVolumeType(VOLUME_TYPE_SATA);
            } else if (!strcmp(label, "ums")) {
                dv->setVolumeType(VOLUME_TYPE_UMS);
            }

            mkdir(fstab->recs[i].mount_point, 0755);

#if 0
#ifdef HAS_VIRTUAL_CDROM
            else if (!strcmp(type, "loop_mount")) {
                DirectVolume *loopdv = NULL;

                if (strcmp(label,"loop")) {
                    SLOGE("unvalid label '%s'", label);
                    goto out_syntax;
                }

                loopdv = new DirectVolume(vm, label, mount_point, -1);
                loopdv->setVolumeType(VOLUME_TYPE_LOOP);
                vm->addVolume(loopdv);
            }
#endif /* HAS_VIRTUAL_CDROM */
#endif
            dv->setFlags(flags);
=======
            /* Only set this flag if there is not an emulated sd card */
            if (fs_mgr_is_noemulatedsd(&fstab->recs[i]) &&
                !strcmp(fstab->recs[i].fs_type, "vfat")) {
                flags |= VOL_PROVIDES_ASEC;
            }
            dv = new DirectVolume(vm, &(fstab->recs[i]), flags);

            if (dv->addPath(fstab->recs[i].blk_device)) {
                SLOGE("Failed to add devpath %s to volume %s",
                      fstab->recs[i].blk_device, fstab->recs[i].label);
                goto out_fail;
            }
>>>>>>> refs/tags/android-4.4_r1

            vm->addVolume(dv);
        }
    }

    ret = 0;

out_fail:
    return ret;
}
#endif
