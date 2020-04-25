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
#include <string.h>
#include <errno.h>

#include <fcntl.h>
#include <dirent.h>

#include <linux/kdev_t.h>
#include <sys/stat.h>

#define LOG_TAG "DirectVolume"

#include <cutils/log.h>
#include <sysutils/NetlinkEvent.h>

#include "DirectVolume.h"
#include "VolumeManager.h"
#include "ResponseCode.h"
#include "cryptfs.h"
#include <cutils/properties.h>

DirectVolume::DirectVolume(VolumeManager *vm, const char *label,
                           const char *mount_point, int partIdx) :
              Volume(vm, label, mount_point) {
    mPartIdx = partIdx;

    mPaths = new PathCollection();
    for (int i = 0; i < MAX_PARTITIONS; i++) {
        mPartitionLabel[i] = NULL;
        mPartMinors[i] = -1;
    }
    mNumPendingParts = 0;
    mDiskMajor = -1;
    mDiskMinor = -1;
    mDiskNumParts = 0;
    mNoParts = false;

    if (strcmp(mount_point, "auto") != 0) {
        ALOGE("Vold managed volumes must have auto mount point; ignoring %s",
              mount_point);
    }

    char mount[PATH_MAX];
    //snprintf(mount, PATH_MAX, "%s/%s", Volume::MEDIA_DIR, label);
    //mMountpoint = strdup(mount);
	mMountpoint = strdup(mount_point);
	
    snprintf(mount, PATH_MAX, "%s/%s", Volume::FUSE_DIR, label);
    mFuseMountpoint = strdup(mount);

    setState(Volume::State_NoMedia);
    sr_mounted = false;
}

DirectVolume::~DirectVolume() {
    PathCollection::iterator it;

    for (it = mPaths->begin(); it != mPaths->end(); ++it)
        free(*it);
    delete mPaths;
}

int DirectVolume::addPath(const char *path) {
    mPaths->push_back(strdup(path));
    return 0;
}

void DirectVolume::setFlags(int flags) {
    mFlags = flags;
}

dev_t DirectVolume::getDiskDevice() {
    return MKDEV(mDiskMajor, mDiskMinor);
}

dev_t DirectVolume::getShareDevice() {
    if (mPartIdx != -1) {
        return MKDEV(mDiskMajor, mPartIdx);
    } else {
        return MKDEV(mDiskMajor, mDiskMinor);
    }
}

void DirectVolume::handleVolumeShared() {
    setState(Volume::State_Shared);
}

void DirectVolume::handleVolumeUnshared() {
    setState(Volume::State_Idle);
}

#define SR_DISK_STRING	"sr"
int DirectVolume::handleBlockEvent(NetlinkEvent *evt) {
    const char *dp = evt->findParam("DEVPATH");
    const char *devname = evt->findParam("DEVNAME");
    bool sr_disk=false;
	
    PathCollection::iterator  it;
    if(!strncmp(devname,SR_DISK_STRING,strlen(SR_DISK_STRING)))
    {
	   sr_disk =true;
    }

    for (it = mPaths->begin(); it != mPaths->end(); ++it) {
        if (mVolumeType != VOLUME_TYPE_UMS && !strncmp(dp, *it, strlen(*it)) || !strcmp(dp, *it)) {
            /* We can handle this disk */
            int action = evt->getAction();
            const char *devtype = evt->findParam("DEVTYPE");

	     if(sr_disk && action ==NetlinkEvent::NlActionChange )
	     {
	     		if(!sr_mounted)
				action = NetlinkEvent::NlActionAdd;
			else
				action = NetlinkEvent::NlActionRemove;
	      }
            if (action == NetlinkEvent::NlActionAdd) {
                int major = atoi(evt->findParam("MAJOR"));
                int minor = atoi(evt->findParam("MINOR"));
                char nodepath[255];

                snprintf(nodepath,
                         sizeof(nodepath), "/dev/block/vold/%d:%d",
                         major, minor);
                if (createDeviceNode(nodepath, major, minor)) {
                    SLOGE("Error making device node '%s' (%s)", nodepath,
                                                               strerror(errno));
                }
                if (!strcmp(devtype, "disk")) {
                    handleDiskAdded(dp, evt);
		      if(sr_disk)
			   sr_mounted = true;	
                } else {
#ifdef   FUNCTION_UMS_PARTITION
                    if (mVolumeType == VOLUME_TYPE_UMS) {
                        handleUMSPartitionAdded(dp, evt);
                    }
                    else
                    {
                        handlePartitionAdded(dp, evt);
                    }
#else
                    /* Send notification iff disk is ready (ie all partitions found) */
                    //??
                    if (getState() == Volume::State_Idle) {
                        handlePartitionAdded(dp, evt);
                    }
#endif
                    handlePartitionAdded(dp, evt);
                }
                /* Send notification iff disk is ready (ie all partitions found) */
                if (getState() == Volume::State_Idle) {
                    char msg[255];

                    snprintf(msg, sizeof(msg),
                             "Volume %s %s disk inserted (%d:%d)", getLabel(),
                             getMountpoint(), mDiskMajor, mDiskMinor);
                    mVm->getBroadcaster()->sendBroadcast(ResponseCode::VolumeDiskInserted,
                                                         msg, false);
                }
            } else if (action == NetlinkEvent::NlActionRemove) {
                if (!strcmp(devtype, "disk")) {
                    handleDiskRemoved(dp, evt);
		      if(sr_disk)
			   sr_mounted = false;	
                } else {
 #ifdef FUNCTION_UMS_PARTITION
 		      if (mVolumeType == VOLUME_TYPE_UMS) {
				handleUMSPartitionRemoved(dp, evt);
		      }
		      else
		      {
                    handlePartitionRemoved(dp, evt);
                }
#else
                    handlePartitionRemoved(dp, evt);
#endif
                }
            } else if (action == NetlinkEvent::NlActionChange) {
                if (!strcmp(devtype, "disk")) {
                    handleDiskChanged(dp, evt);
                } else {
 #ifdef FUNCTION_UMS_PARTITION
		     if (mVolumeType == VOLUME_TYPE_UMS) {
				handleUMSPartitionChanged(dp, evt);
		      }
		      else
		      {
                    handlePartitionChanged(dp, evt);
                }
 #else
                    handlePartitionChanged(dp, evt);
 #endif
                }
            } else {
                    SLOGW("Ignoring non add/remove/change event");
            }

            return 0;
        }
    }
    errno = ENODEV;
    return -1;
}

void DirectVolume::handleDiskAdded(const char *devpath, NetlinkEvent *evt) {
    mDiskMajor = atoi(evt->findParam("MAJOR"));
    mDiskMinor = atoi(evt->findParam("MINOR"));

    const char *tmp = evt->findParam("NPARTS");
    if (tmp) {
        mDiskNumParts = atoi(tmp);
    } else {
        SLOGW("Kernel block uevent missing 'NPARTS'");
        mDiskNumParts = 1;
    }

    char msg[255];

    mValidPartMap = 0;
    mNumPendingParts = mDiskNumParts;

    if (mDiskNumParts == 0) {
#ifdef PARTITION_DEBUG
        SLOGD("Dv::diskIns(%s) - No partitions - good to go son!", getLabel());
#endif
        mNoParts = true;
        setState(Volume::State_Idle);

        snprintf(msg, sizeof(msg), "Volume %s %s disk inserted (%d:%d)",
                 getLabel(), getMountpoint(), mDiskMajor, mDiskMinor);
        mVm->getBroadcaster()->sendBroadcast(ResponseCode::VolumeDiskInserted,
                                                 msg, false);
    } else {
#ifdef PARTITION_DEBUG
        SLOGD("Dv::diskIns(%s) - waiting for %d partitions (%d pending)",
             getLabel(), mDiskNumParts, mNumPendingParts);
#endif
        mNoParts = false;
        setState(Volume::State_Pending);
    }
}

void DirectVolume::handlePartitionAdded(const char *devpath, NetlinkEvent *evt) {
    int major = atoi(evt->findParam("MAJOR"));
    int minor = atoi(evt->findParam("MINOR"));
    const char *devname = evt->findParam("DEVNAME");
    char msg[255];

    int part_num;

    const char *tmp = evt->findParam("PARTN");

    if (tmp) {
        part_num = atoi(tmp);
    } else {
        SLOGW("Kernel block uevent missing 'PARTN'");
        part_num = 1;
    }

    mPartitionLabel[part_num - 1] = strdup(devname);

    if (part_num > mDiskNumParts) {
        mDiskNumParts = part_num;
    }

    if (major != mDiskMajor && major!=259) {
        SLOGE("Partition '%s' has a different major than its disk!", devpath);
        return;
    }
#ifdef PARTITION_DEBUG
    SLOGD("Dv:partAdd: part_num = %d, minor = %d label = %s\n",
            part_num, minor, mPartitionLabel[part_num - 1]);
#endif
        mPartMinors[part_num -1] = minor;

    mValidPartMap |= 1 << (part_num - 1);
    mNumPendingParts--;
    if (mNumPendingParts == 0) {
#ifdef PARTITION_DEBUG
        SLOGD("Dv:partAdd: Got all partitions - ready to rock!");
#endif
        if (getState() != Volume::State_Formatting) {
            setState(Volume::State_Idle);
        }
        snprintf(msg, sizeof(msg), "Volume %s %s disk inserted (%d:%d)",
                 getLabel(), getMountpoint(), mDiskMajor, mDiskMinor);
        mVm->getBroadcaster()->sendBroadcast(ResponseCode::VolumeDiskInserted,
                                                 msg, false);
    } else {
#ifdef PARTITION_DEBUG
        SLOGD("Dv:partAdd: number of pending partitions now = %d", mNumPendingParts);
#endif
    }
}

#ifdef FUNCTION_UMS_PARTITION 
void DirectVolume::handleUMSPartitionAdded(const char *devpath, NetlinkEvent *evt) {
    mDiskMajor = atoi(evt->findParam("MAJOR"));
    mDiskMinor = atoi(evt->findParam("MINOR"));
	
    char msg[255];

    mValidPartMap = 0;

    mNoParts = true;
    setState(Volume::State_Idle);

 #ifdef PARTITION_DEBUG
    SLOGD("Dv:ums partAdd: major = %d, minor = %d, label = %s\n", mDiskMajor, mDiskMinor, mLabel);
#endif
//Send msg only when State_Idle
        snprintf(msg, sizeof(msg), "Volume %s %s disk inserted (%d:%d)",
                 getLabel(), getMountpoint(), mDiskMajor, mDiskMinor);
        mVm->getBroadcaster()->sendBroadcast(ResponseCode::VolumeDiskInserted,
                                                 msg, false);

}
#endif

void DirectVolume::handleDiskChanged(const char *devpath, NetlinkEvent *evt) {
    int major = atoi(evt->findParam("MAJOR"));
    int minor = atoi(evt->findParam("MINOR"));

    if ((major != mDiskMajor) || (minor != mDiskMinor)) {
        return;
    }

    SLOGI("Volume %s disk has changed", getLabel());
    const char *tmp = evt->findParam("NPARTS");
    if (tmp) {
        mDiskNumParts = atoi(tmp);
    } else {
        SLOGW("Kernel block uevent missing 'NPARTS'");
        mDiskNumParts = 1;
    }

    char mediapollstr[PROPERTY_VALUE_MAX];
    property_get("has.media.poll",  mediapollstr, "false");
    if(strcmp(mediapollstr,"true")==0)
    {
    	if((mDiskNumParts==0)&&(getState() == Volume::State_Mounted)&&(mVolumeType == VOLUME_TYPE_UMS))
    	{
        	const char *devname = evt->findParam("DEVNAME");
	 	char nodepath[255];
        	int fd;
        	snprintf(nodepath,sizeof(nodepath), "/dev/block/%s",devname);
        	fd = open (nodepath, O_RDONLY);
        	if (fd >= 0) {
            		close (fd);
        	}
		else
        	{
        		SLOGI("open %s dev failed", getLabel());
        		SLOGI("Volume %s disk should remove", getLabel());
	 		handleDiskRemoved(devpath,evt);
		}
	 	return;
    	}
    }
    mValidPartMap = 0;
    mNumPendingParts = mDiskNumParts;

    if (getState() != Volume::State_Formatting) {
        if (mDiskNumParts == 0) {
            setState(Volume::State_Idle);
        } else {
            setState(Volume::State_Pending);
        }
    }
}

void DirectVolume::handlePartitionChanged(const char *devpath, NetlinkEvent *evt) {
    int major = atoi(evt->findParam("MAJOR"));
    int minor = atoi(evt->findParam("MINOR"));
    SLOGD("Volume %s %s partition %d:%d changed\n", getLabel(), getMountpoint(), major, minor);
}

#ifdef FUNCTION_UMS_PARTITION 
void DirectVolume::handleUMSPartitionChanged(const char *devpath, NetlinkEvent *evt) {
    int major = atoi(evt->findParam("MAJOR"));
    int minor = atoi(evt->findParam("MINOR"));

    if ((major != mDiskMajor) || (minor != mDiskMinor)) {
        return;
    }

    SLOGI("Volume %s partition has changed", getLabel());
    
    mValidPartMap = 0;
    
    if (getState() != Volume::State_Formatting) {
        setState(Volume::State_Idle);
    }
}
#endif

void DirectVolume::handleDiskRemoved(const char *devpath, NetlinkEvent *evt) {
    int major = atoi(evt->findParam("MAJOR"));
    int minor = atoi(evt->findParam("MINOR"));
    char msg[255];
    bool enabled;

    if (mVm->shareEnabled(getLabel(), "ums", &enabled) == 0 && enabled) {
        mVm->unshareVolume(getLabel(), "ums");
    }

    SLOGD("Volume %s %s disk %d:%d removed\n", getLabel(), getMountpoint(), major, minor);
    if (mMountedPartMap & (1 << 31)) {
        char move2sdcard[PROPERTY_VALUE_MAX];
	property_get("ro.storage.move2sdcard",  move2sdcard, "");
       if((mHasAsec ||(0 == strcmp(move2sdcard, "true")))
           &&(VOLUME_TYPE_SDCARD == mVolumeType))  {
            if (mVm->cleanupAsec(this, true))
                SLOGE("Failed to cleanup ASEC - unmount will probably fail!");
            unmountVol(true,true);
        } else
            unmountVol(false,true);
    }
    snprintf(msg, sizeof(msg), "Volume %s %s disk removed (%d:%d)",
             getLabel(), getMountpoint(), major, minor);
    mVm->getBroadcaster()->sendBroadcast(ResponseCode::VolumeDiskRemoved,
                                             msg, false);
    setState(Volume::State_NoMedia);
    if (mVolumeType != VOLUME_TYPE_FLASH && mVolumeType != VOLUME_TYPE_SDCARD)
        rmdir(getMountpoint());

    if (mVolumeType == VOLUME_TYPE_UMS) {
        setState(Volume::State_Idle);
        setState(Volume::State_Deleting);
    }
    if (Volume::sVirtualSdcard && (VOLUME_TYPE_SDCARD == mVolumeType)) {
 	if (Volume::sFlashMouted) {
	    	SLOGW("VSDCARD: Mount virtual sdcard");
      		char vsd_path[255];
		snprintf(vsd_path,
	                         sizeof(vsd_path), "%s/.vsdcard",
	                         "/mnt/flash");
			       
		SLOGW("VSDCARD: symlink %s -> %s", "/mnt/sdcard", vsd_path);
		rmdir("/mnt/sdcard");
		mkdir(vsd_path, 0755);
		symlink(vsd_path, "/mnt/sdcard");
		setVirtualSdcardState(Volume::State_Idle, Volume::State_Mounted);
	}
			
    }     
}

void DirectVolume::handlePartitionRemoved(const char *devpath, NetlinkEvent *evt) {
    int major = atoi(evt->findParam("MAJOR"));
    int minor = atoi(evt->findParam("MINOR"));
    const char *part = evt->findParam("PARTN");
    int state;
    int part_num, part_index;
    char msg[255];

    SLOGD("Volume %s %s partition %d:%d removed\n", getLabel(), getMountpoint(), major, minor);

    if (part)
        part_num = atoi(part);
    else {
        SLOGW("Kernel block uevent missing 'PARTN'");
        part_num = 1;
    }
    part_index = part_num - 1;

retry:
    /*
     * The framework doesn't need to get notified of
     * partition removal unless it's mounted. Otherwise
     * the removal notification will be sent on the Disk
     * itself
     */
    state = getState();
    if (state != Volume::State_Mounted && state != Volume::State_Shared) {
		 if(state == Volume::State_Checking)
		 {
				SLOGD("this volume is mounting, retry until mount is end");
				sleep(1);
				goto retry;
		 }
			
        if (VOLUME_TYPE_UMS != mVolumeType ||
            (VOLUME_TYPE_UMS == mVolumeType && getState() != Volume::State_Unmounting)) {
            SLOGD("Removing unmounted partition part=%d state=%d", part_index, state);
            if(mPartitionLabel[part_index]) {
                free(mPartitionLabel[part_index]);
                mPartitionLabel[part_index] = NULL;
            }
        return;
    }
    }
        
    SLOGD("Volume %s partition %d removed. mounted partitions: 0x%x",
            getLabel(), part_index, mMountedPartMap);
    if (mMountedPartMap & (1 << part_index)) {
        /*
         * Yikes, our mounted partition is going away!
         */

        if ((mMountedPartMap & ~(1 << part_index)) == 0) {
            // last mounted partition removed
            /*by duanqi,
                skip here,if send bad removal msg, then removed msg wil faill
            	  so if disk has partitions,it will always unsafe removal
            	  fix it
            */
            //snprintf(msg, sizeof(msg), "Volume %s %s bad removal (%d:%d)",
            //         getLabel(), getMountpoint(), major, minor);
            //mVm->getBroadcaster()->sendBroadcast(ResponseCode::VolumeBadRemoval,
            //                                     msg, false);
        }


        char move2sdcard[PROPERTY_VALUE_MAX];
	property_get("ro.storage.move2sdcard",  move2sdcard, "");
       if((mHasAsec ||(0 == strcmp(move2sdcard, "true")))
            &&(VOLUME_TYPE_SDCARD == mVolumeType)) {
	if (mVm->cleanupAsec(this, true)) {
            SLOGE("Failed to cleanup ASEC - unmount will probably fail!");
        }

            // FIXME? this will unmount all sdcard partitions
            if (Volume::unmountVol(true,true)) {
                SLOGE("Failed to unmount volume on bad removal (%s)",
                     strerror(errno));
                // XXX: At this point we're screwed for now
            } else {
                SLOGD("Crisis averted");
            }
        } else if(VOLUME_TYPE_FLASH == mVolumeType || VOLUME_TYPE_SDCARD == mVolumeType) {
            // FIXME? this will unmount all sdcard partitions
            if (Volume::unmountVol(true,true)) {
            SLOGE("Failed to unmount volume on bad removal (%s)", 
                 strerror(errno));
            // XXX: At this point we're screwed for now
        } else {
            SLOGD("Crisis averted");
        }
        }
        else {
            SLOGD("Unmounting %s partition %d", getLabel(), part_index);
            if (Volume::unmountPart(part_index))
                SLOGE("Failed to unmount volume %s, part %d", getLabel(), part_index);
        }
    } else if (state == Volume::State_Shared) {
        SLOGD("%s partition %d gone, unsharing", getLabel(), part_index);
        snprintf(msg, sizeof(msg), "Volume %s bad removal (%d:%d)",
                 getLabel(), major, minor);
        mVm->getBroadcaster()->sendBroadcast(ResponseCode::VolumeBadRemoval,
                                             msg, false);

        if (mVm->unshareVolume(getLabel(), "ums")) {
            SLOGE("Failed to unshare volume on bad removal (%s)",
                strerror(errno));
        } else {
            SLOGD("Crisis averted");
        }
    }
    if (mPartitionLabel[part_index]) {
        free(mPartitionLabel[part_index]);
        mPartitionLabel[part_index] = NULL;
    }
}

#ifdef FUNCTION_UMS_PARTITION
void DirectVolume::handleUMSPartitionRemoved(const char *devpath, NetlinkEvent *evt) {
    int major = atoi(evt->findParam("MAJOR"));
    int minor = atoi(evt->findParam("MINOR"));
    char msg[255];
    int state;
	
retry1:
    /*
     * The framework doesn't need to get notified of
     * partition removal unless it's mounted. Otherwise
     * the removal notification will be sent on the Disk
     * itself
     */
    state = getState();
    if (state != Volume::State_Mounted && state != Volume::State_Shared) {
	 if(state == Volume::State_Checking)
	 {
		SLOGD("this volume is mounting, retry until mount is end");
		sleep(1);
		goto retry1;
	 }			
    }

    SLOGD("Volume %s %s disk %d:%d removed\n", getLabel(), getMountpoint(), major, minor);

    if(mMountedPartMap & (1 << 31)){
        if(VOLUME_TYPE_SDCARD == mVolumeType) {
            if (mVm->cleanupAsec(this, true)) 
                SLOGE("Failed to cleanup ASEC - unmount will probably fail!");
            unmountVol(true,true);
       }
	else
          unmountVol(true,true);
    }
    snprintf(msg, sizeof(msg), "Volume %s %s disk removed (%d:%d)",
             getLabel(), getMountpoint(), major, minor);
    mVm->getBroadcaster()->sendBroadcast(ResponseCode::VolumeDiskRemoved,
                                             msg, false);
    setState(Volume::State_NoMedia);
    if (mVolumeType != VOLUME_TYPE_FLASH && mVolumeType != VOLUME_TYPE_SDCARD)
        rmdir(getMountpoint());

    if (mVolumeType == VOLUME_TYPE_UMS) {
        setState(Volume::State_Idle);
        setState(Volume::State_Deleting);
    }
}
#endif

/*
 * Called from base to get a list of devicenodes for mounting
 */
int DirectVolume::getDeviceNodes(dev_t *devs, int max) {
    if (max == 1) {
        devs[0] = MKDEV(mDiskMajor, mDiskMinor);
        return 1;
    }

    if (mPartIdx == -1) {
        // If the disk has no partitions, try the disk itself
        if (!mDiskNumParts) {
            devs[0] = MKDEV(mDiskMajor, mDiskMinor);
            return 1;
        }

        int i;
        for (i = 0; i < mDiskNumParts; i++) {
            if (i == max)
                break;
            devs[i] = MKDEV(mDiskMajor, mPartMinors[i]);
        }
        return mDiskNumParts;
    }
    else {
        // If the disk has no partitions, try the disk itself
        if (!mDiskNumParts)
            devs[0] = MKDEV(mDiskMajor, mDiskMinor);
        else
    devs[0] = MKDEV(mDiskMajor, mPartMinors[mPartIdx -1]);
    return 1;
    }
}

const char* DirectVolume::getDeviceNodesLabel(int partition_index) {
    return mPartitionLabel[partition_index];
}

int DirectVolume::getVolumeShareIdx() {
    return mPartIdx;
}

int DirectVolume::getVolumeMinor(int index) {
    if (index < 0 || index > MAX_PARTITIONS)
        return -1;
    return mPartMinors[index];
}
/*
 * Called from base to update device info,
 * e.g. When setting up an dm-crypt mapping for the sd card.
 */
int DirectVolume::updateDeviceInfo(char *new_path, int new_major, int new_minor)
{
    PathCollection::iterator it;

    if (mPartIdx == -1) {
        SLOGE("Can only change device info on a partition\n");
        return -1;
    }

    /*
     * This is to change the sysfs path associated with a partition, in particular,
     * for an internal SD card partition that is encrypted.  Thus, the list is
     * expected to be only 1 entry long.  Check that and bail if not.
     */
    if (mPaths->size() != 1) {
        SLOGE("Cannot change path if there are more than one for a volume\n");
        return -1;
    }

    it = mPaths->begin();
    free(*it); /* Free the string storage */
    mPaths->erase(it); /* Remove it from the list */
    addPath(new_path); /* Put the new path on the list */

    /* Save away original info so we can restore it when doing factory reset.
     * Then, when doing the format, it will format the original device in the
     * clear, otherwise it just formats the encrypted device which is not
     * readable when the device boots unencrypted after the reset.
     */
    mOrigDiskMajor = mDiskMajor;
    mOrigDiskMinor = mDiskMinor;
    mOrigPartIdx = mPartIdx;
    memcpy(mOrigPartMinors, mPartMinors, sizeof(mPartMinors));

    mDiskMajor = new_major;
    mDiskMinor = new_minor;
    /* Ugh, virual block devices don't use minor 0 for whole disk and minor > 0 for
     * partition number.  They don't have partitions, they are just virtual block
     * devices, and minor number 0 is the first dm-crypt device.  Luckily the first
     * dm-crypt device is for the userdata partition, which gets minor number 0, and
     * it is not managed by vold.  So the next device is minor number one, which we
     * will call partition one.
     */
    mPartIdx = new_minor;
    mPartMinors[new_minor-1] = new_minor;

    mIsDecrypted = 1;

    return 0;
}

/*
 * Called from base to revert device info to the way it was before a
 * crypto mapping was created for it.
 */
void DirectVolume::revertDeviceInfo(void)
{
    if (mIsDecrypted) {
        mDiskMajor = mOrigDiskMajor;
        mDiskMinor = mOrigDiskMinor;
        mPartIdx = mOrigPartIdx;
        memcpy(mPartMinors, mOrigPartMinors, sizeof(mPartMinors));

        mIsDecrypted = 0;
    }

    return;
}

/*
 * Called from base to give cryptfs all the info it needs to encrypt eligible volumes
 */
int DirectVolume::getVolInfo(struct volume_info *v)
{
    strcpy(v->label, mLabel);
    strcpy(v->mnt_point, mMountpoint);
    v->flags = getFlags();
    /* Other fields of struct volume_info are filled in by the caller or cryptfs.c */

    return 0;
}
