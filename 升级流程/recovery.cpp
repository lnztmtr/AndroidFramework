/*
 * Copyright (C) 2007 The Android Open Source Project
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

#include <fs_mgr.h>
#include <ctype.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <getopt.h>
#include <limits.h>
#include <linux/input.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <time.h>
#include <unistd.h>

#include "bootloader.h"
#include "common.h"
#include "cutils/properties.h"
#include "cutils/android_reboot.h"
#include "install.h"
#include "minui/minui.h"
#include "minzip/DirUtil.h"
#include "roots.h"
#include "efuse.h"
#include "ui.h"
#include "screen_ui.h"
#include "device.h"
#include "adb_install.h"
#include "mtdutils/mtdutils.h"
#include "usb_burning.h"
#include "cmd_excute.h"
#include "recovery_key.h"
#include "adb_listener.h"
#include "make_ext4fs.h"

extern "C" {
#include "minadbd/adb.h"
#include "fw_env.h"
}

extern int iptv_recovery_flag;
/* iptv show wipe ui except ota upgrade */
static bool iptv_has_wipe_ui = true;

struct selabel_handle *sehandle;
#define REBOOT_NORMAL                        0
#define REBOOT_SHUTDOWN                      1
#define REBOOT_FACTORY_TEST                  2
#define REBOOT_RECOVERY_AGAIN                3

static const struct option OPTIONS[] = {
  { "aml_update_version", required_argument, NULL, 'q' },
  { "send_intent", required_argument, NULL, 's' },
  { "update_package", required_argument, NULL, 'u' },
  { "update_patch", required_argument, NULL, 'x' },
  { "reboot_to_factorytest", no_argument, NULL, 'f' },
  { "wipe_data", no_argument, NULL, 'w' },
  { "wipe_cache", no_argument, NULL, 'c' },
  { "usb_burning", no_argument, NULL, 'n' },
  { "without_format", no_argument, NULL, 'o' },   
  { "file_copy_from_partition", required_argument, NULL, 'z' },
#ifdef RECOVERY_HAS_MEDIA
  { "wipe_media", no_argument, NULL, 'm' },
#endif /* RECOVERY_HAS_MEDIA */
#ifdef RECOVERY_HAS_PARAM
  { "wipe_param", no_argument, NULL, 'P' },
#endif /*RECOVERY_HAS_PARAM */
  { "show_text", no_argument, NULL, 't' },
  { "update_from_udisk", no_argument, NULL, 'U' },
  { "just_exit", no_argument, NULL, 'x' },
  { "locale", required_argument, NULL, 'l' },
#ifdef RECOVERY_HAS_EFUSE
  { "set_efuse_version", required_argument, NULL, 'v' },
  { "set_efuse_ethernet_mac", optional_argument, NULL, 'd' },
  { "set_efuse_bluetooth_mac", optional_argument, NULL, 'b' },
#ifdef  EFUSE_LICENCE_ENABLE
  { "set_efuse_audio_license", optional_argument, NULL, 'a' },
#endif /* EFUSE_LICENCE_ENABLE */
#endif /* RECOVERY_HAS_EFUSE */
#ifdef RECOVERY_WRITE_KEY
  { "flash_write_mac", no_argument, NULL, 'B' },
  { "flash_write_mac_force", no_argument, NULL, 'C' },
  { "flash_write_mac_bt", no_argument, NULL, 'D' },
  { "flash_write_mac_bt_force", no_argument, NULL, 'E' },
  { "flash_write_mac_wifi", no_argument, NULL, 'F' },
  { "flash_write_mac_wifi_force", no_argument, NULL, 'G' },
  { "flash_write_hdcp", no_argument, NULL, 'H' },
  { "flash_write_hdcp_force", no_argument, NULL, 'I' },
  { "flash_write_usid", no_argument, NULL, 'J' },
  { "flash_write_usid_force", no_argument, NULL, 'K' },
#endif /* RECOVERY_WRITE_KEY */
  { "run_command", required_argument, NULL, 'r' },
  { "wipe_data_with_keep", no_argument, NULL, 'j' },
  { "restore_system", no_argument, NULL, 'g' },
  { "factoryreset_keepfile", required_argument, NULL, 'h' },
  { NULL, 0, NULL, 0 },
};

#define LAST_LOG_FILE "/cache/recovery/last_log"

static const char *CACHE_LOG_DIR = "/cache/recovery";
static const char *COMMAND_FILE = "/cache/recovery/command";
static const char *INTENT_FILE = "/cache/recovery/intent";
static const char *LOG_FILE = "/cache/recovery/log";
static const char *LAST_INSTALL_FILE = "/cache/recovery/last_install";
static const char *LOCALE_FILE = "/cache/recovery/last_locale";
static const char *CACHE_ROOT = "/cache";
static const char *SDCARD_ROOT = "/sdcard";
static const char *BOOTFILES_ROOT = "/bootfiles";
static const char *BOOTFILES_INSTALL_FILE = "/bootfiles/update.zip";
static const char *SDCARD_COMMAND_FILE = "/sdcard/factory_update_param.aml";
static const char *UDISK_ROOT = "/udisk";
static const char *UDISK_COMMAND_FILE = "/udisk/factory_update_param.aml";
//start by lizheng for hisfactorymode 20190820
static const char *FACTORY_MODE = "/udisk/hisfactorymode";
//end by lizheng for hisfactorymode 20190820

#ifdef RECOVERY_HAS_MEDIA
static const char *MEDIA_ROOT = "/media";
#endif /* RECOVERY_HAS_MEDIA */
#ifdef RECOVERY_HAS_PARAM
static const char *PARAM_ROOT = "/params";
#endif /* RECOVERY_HAS_PARAM */
static const char *TEMPORARY_LOG_FILE = "/tmp/recovery.log";
static const char *TEMPORARY_INSTALL_FILE = "/tmp/last_install";
static const char *SIDELOAD_TEMP_DIR = "/tmp/sideload";
static const char *CURRENT_STATUS_FILE = "/params/update_done_info.txt";
static char *TEMP_KEEP_FILE = "/tmp/keepFile";
static char *DATA_TEMP_ROOT = "/data/tmp";
static int keep_data = 0;
//add by lizheng for led flash 20190821
int ota_finish_flag = 0;

RecoveryUI* ui = NULL;
char* locale = NULL;
#ifdef RECOVERY_HAS_EFUSE
#include "efuse.h"
#endif
char recovery_version[PROPERTY_VALUE_MAX+1];

#define FACTORY_RESET_ABNORMAL_PROTECT      1

/*
 * The recovery tool communicates with the main system through /cache files.
 *   /cache/recovery/command - INPUT - command line for tool, one arg per line
 *   /cache/recovery/log - OUTPUT - combined log file from recovery run(s)
 *   /cache/recovery/intent - OUTPUT - intent that was passed in
 *
 * The arguments which may be supplied in the recovery.command file:
 *   --send_intent=anystring - write the text out to recovery.intent
 *   --update_package=path - verify install an OTA package file
 *   --wipe_data - erase user data (and cache), then reboot
 *   --wipe_cache - wipe cache (but not user data), then reboot
 *   --set_encrypted_filesystem=on|off - enables / diasables encrypted fs
 *   --just_exit - do nothing; exit and reboot
 *
 * After completing, we remove /cache/recovery/command and reboot.
 * Arguments may also be supplied in the bootloader control block (BCB).
 * These important scenarios must be safely restartable at any point:
 *
 * FACTORY RESET
 * 1. user selects "factory reset"
 * 2. main system writes "--wipe_data" to /cache/recovery/command
 * 3. main system reboots into recovery
 * 4. get_args() writes BCB with "boot-recovery" and "--wipe_data"
 *    -- after this, rebooting will restart the erase --
 * 5. erase_volume() reformats /data
 * 6. erase_volume() reformats /cache
 * 7. finish_recovery() erases BCB
 *    -- after this, rebooting will restart the main system --
 * 8. main() calls reboot() to boot main system
 *
 * OTA INSTALL
 * 1. main system downloads OTA package to /cache/some-filename.zip
 * 2. main system writes "--update_package=/cache/some-filename.zip"
 * 3. main system reboots into recovery
 * 4. get_args() writes BCB with "boot-recovery" and "--update_package=..."
 *    -- after this, rebooting will attempt to reinstall the update --
 * 5. install_package() attempts to install the update
 *    NOTE: the package install must itself be restartable from any point
 * 6. finish_recovery() erases BCB
 *    -- after this, rebooting will (try to) restart the main system --
 * 7. ** if install failed **
 *    7a. prompt_and_wait() shows an error icon and waits for the user
 *    7b; the user reboots (pulling the battery, etc) into the main system
 * 8. main() calls maybe_install_firmware_update()
 *    ** if the update contained radio/hboot firmware **:
 *    8a. m_i_f_u() writes BCB with "boot-recovery" and "--wipe_cache"
 *        -- after this, rebooting will reformat cache & restart main system --
 *    8b. m_i_f_u() writes firmware image into raw cache partition
 *    8c. m_i_f_u() writes BCB with "update-radio/hboot" and "--wipe_cache"
 *        -- after this, rebooting will attempt to reinstall firmware --
 *    8d. bootloader tries to flash firmware
 *    8e. bootloader writes BCB with "boot-recovery" (keeping "--wipe_cache")
 *        -- after this, rebooting will reformat cache & restart main system --
 *    8f. erase_volume() reformats /cache
 *    8g. finish_recovery() erases BCB
 *        -- after this, rebooting will (try to) restart the main system --
 * 9. main() calls reboot() to boot main system
 */

static const int MAX_ARG_LENGTH = 4096;
static const int MAX_ARGS = 100;

// open a given path, mounting partitions as necessary
FILE*
fopen_path(const char *path, const char *mode) {
    if (ensure_path_mounted(path) != 0) {
        LOGE("Can't mount %s\n", path);
        return NULL;
    }

    // When writing, try to create the containing directory, if necessary.
    // Use generous permissions, the system (init.rc) will reset them.
    if (strchr("wa", mode[0])) dirCreateHierarchy(path, 0777, NULL, 1, sehandle);

    FILE *fp = fopen(path, mode);
    return fp;
}

// close a file, log an error if the error indicator is set
void
check_and_fclose(FILE *fp, const char *name) {
    fflush(fp);
    if (ferror(fp)) LOGE("Error in %s\n(%s)\n", name, strerror(errno));
    fclose(fp);
}

// command line args come from, in decreasing precedence:
//   - the actual command line
//   - the bootloader control block (one per line, after "recovery")
//   - the contents of COMMAND_FILE (one per line)
static void
get_args(int *argc, char ***argv) {
    struct bootloader_message boot;
    memset(&boot, 0, sizeof(boot));
    get_bootloader_message(&boot);  // this may fail, leaving a zeroed structure

    if (boot.command[0] != 0 && boot.command[0] != 255) {
        LOGI("Boot command: %.*s\n", sizeof(boot.command), boot.command);
    }

    if (boot.status[0] != 0 && boot.status[0] != 255) {
        LOGI("Boot status: %.*s\n", sizeof(boot.status), boot.status);
    }

    // --- if arguments weren't supplied, look in the bootloader control block
    if (*argc <= 1) {
        boot.recovery[sizeof(boot.recovery) - 1] = '\0';  // Ensure termination
        const char *arg = strtok(boot.recovery, "\n");
        if (arg != NULL && !strcmp(arg, "recovery")) {
            *argv = (char **) malloc(sizeof(char *) * MAX_ARGS);
            (*argv)[0] = strdup(arg);
            for (*argc = 1; *argc < MAX_ARGS; ++*argc) {
                if ((arg = strtok(NULL, "\n")) == NULL) break;
                (*argv)[*argc] = strdup(arg);
            }
            LOGI("Got arguments from boot message\n");
        } else if (boot.recovery[0] != 0 && boot.recovery[0] != 255) {
            LOGE("Bad boot message\n\"%.20s\"\n", boot.recovery);
        }
    }

    // --- if that doesn't work, try the command file form bootloader:recovery_command
    if (*argc <= 1) {
        char *parg = NULL;
        char *recovery_command = fw_getenv("recovery_command");
        if (recovery_command != NULL && strcmp(recovery_command, "")) {
            char *argv0 = (*argv)[0];
            *argv = (char **) malloc(sizeof(char *) * MAX_ARGS);
            (*argv)[0] = argv0;  // use the same program name

            char buf[MAX_ARG_LENGTH];
            strcpy(buf, recovery_command);
            
            if((parg = strtok(buf, "#")) == NULL){
                LOGE("Bad bootloader arguments\n\"%.20s\"\n", recovery_command); 
            }else{
                (*argv)[1] = strdup(parg);  // Strip newline.
                for (*argc = 2; *argc < MAX_ARGS; ++*argc) {
                    if((parg = strtok(NULL, "#")) == NULL){
                        break;
                    }else{
                        (*argv)[*argc] = strdup(parg);  // Strip newline.
                    }
                }
                LOGI("Got arguments from bootloader\n");
            }
            
        } else {
            LOGE("Bad bootloader arguments\n\"%.20s\"\n", recovery_command);
        }
    }
    
    // --- if that doesn't work, try the command file
    char * temp_args =NULL;
    if (*argc <= 1) {
        FILE *fp = fopen_path(COMMAND_FILE, "r");
        if (fp != NULL) {
            char *token;
            char *argv0 = (*argv)[0];
            *argv = (char **) malloc(sizeof(char *) * MAX_ARGS);
            (*argv)[0] = argv0;  // use the same program name

            char buf[MAX_ARG_LENGTH];
            for (*argc = 1; *argc < MAX_ARGS; ) {
                if (!fgets(buf, sizeof(buf), fp)) break;
                temp_args = strtok(buf, "\r\n");
                if (temp_args == NULL)  continue;
                (*argv)[*argc]  = strdup(temp_args);   // Strip newline.      
                ++*argc;
                //} else {
                //    --*argc;
                //}
            }

            check_and_fclose(fp, COMMAND_FILE);
            LOGI("Got arguments from %s\n", COMMAND_FILE);
        }
    }
    // -- sleep 1 second to ensure SD card initialization complete
    usleep(1000000);

    // --- if that doesn't work, try the sdcard command file
    if (*argc <= 1) {
        FILE *fp = fopen_path(SDCARD_COMMAND_FILE, "r");
        if (fp != NULL) {
            char *argv0 = (*argv)[0];
            *argv = (char **) malloc(sizeof(char *) * MAX_ARGS);
            (*argv)[0] = argv0;  // use the same program name

            char buf[MAX_ARG_LENGTH];
            for (*argc = 1; *argc < MAX_ARGS; ) {
                if (!fgets(buf, sizeof(buf), fp)) break;
			temp_args = strtok(buf, "\r\n");
			if(temp_args == NULL)  continue;
	       		(*argv)[*argc]  = strdup(temp_args);   // Strip newline.      
                		++*argc;
            }

            check_and_fclose(fp, SDCARD_COMMAND_FILE);
            LOGI("Got arguments from %s\n", SDCARD_COMMAND_FILE);
        }
    }

    // --- if that doesn't work, try the udisk command file
    if (*argc <= 1) {
        FILE *fp = fopen_path(UDISK_COMMAND_FILE, "r");
        if (fp != NULL) {
            char *argv0 = (*argv)[0];
            *argv = (char **) malloc(sizeof(char *) * MAX_ARGS);
            (*argv)[0] = argv0;  // use the same program name

            char buf[MAX_ARG_LENGTH];
            for (*argc = 1; *argc < MAX_ARGS; ) {
                if (!fgets(buf, sizeof(buf), fp)) break;
			temp_args = strtok(buf, "\r\n");
			if(temp_args == NULL)  continue;
	       		(*argv)[*argc]  = strdup(temp_args);   // Strip newline.      
                		++*argc;
            }

            check_and_fclose(fp, UDISK_COMMAND_FILE);
            LOGI("Got arguments from %s\n", UDISK_COMMAND_FILE);
        }
    }

    // --- if no argument, then force show_text
    if (*argc <= 1) {
        char *argv0 = (*argv)[0];
        *argv = (char **) malloc(sizeof(char *) * MAX_ARGS);
        (*argv)[0] = argv0;  // use the same program name
        (*argv)[1] = "--show_text";
        *argc = 2;
    }

    // --> write the arguments we have back into the bootloader control block
    // always boot into recovery after this (until finish_recovery() is called)
    strlcpy(boot.command, "boot-recovery", sizeof(boot.command));
    strlcpy(boot.recovery, "recovery\n", sizeof(boot.recovery));
    int i;
    for (i = 1; i < *argc; ++i) {
        strlcat(boot.recovery, (*argv)[i], sizeof(boot.recovery));
        strlcat(boot.recovery, "\n", sizeof(boot.recovery));
    }
    set_bootloader_message(&boot);
}

#ifdef FACTORY_RESET_ABNORMAL_PROTECT
static void 
factory_reset_wipe(int *argc, char ***argv) {
    int i;
    char *wipe_data = fw_getenv("wipe_data");
    char *wipe_cache = fw_getenv("wipe_cache");
    char *wipe_param = fw_getenv("wipe_param");

    if ((NULL!=wipe_data) && !strcmp(wipe_data, "failed")) {
        printf("---wipe_data=%s\n", wipe_data);
        char *argv0 = (*argv)[0];
        *argv = (char **) malloc(sizeof(char *) * MAX_ARGS);
        (*argv)[0] = argv0;  // use the same program name
        (*argv)[1] = "--wipe_data";
        *argc = 2;
    } else if ((NULL!=wipe_cache) && !strcmp(wipe_cache, "failed")) {
        printf("---wipe_cache=%s\n", wipe_cache);
        char *argv0 = (*argv)[0];
        *argv = (char **) malloc(sizeof(char *) * MAX_ARGS);
        (*argv)[0] = argv0;  // use the same program name
        (*argv)[1] = "--wipe_cache";
        *argc = 2;
    } else if ((NULL!=wipe_param) && !strcmp(wipe_param, "failed")) {
        printf("---wipe_param=%s\n", wipe_param);
        char *argv0 = (*argv)[0];
        *argv = (char **) malloc(sizeof(char *) * MAX_ARGS);
        (*argv)[0] = argv0;  // use the same program name
        (*argv)[1] = "--wipe_param";
        *argc = 2;
    }
}

static void set_wipe_data_flag(int flag) {
    char* env_name = "wipe_data";
    char* fail = "failed";
    char* success = "successful";
    char *fw_argv_failed[] = { "fw_setenv",
        env_name,
        flag?success:fail,
        NULL };

    if(0 == fw_setenv(3, fw_argv_failed)){
        sync();
        printf("fw_setenv \"wipe_data=%s\" ok\n", fw_getenv(env_name));
    }else{
        printf("fw_setenv \"wipe_data=%s\" fail\n", fw_getenv(env_name));
    }
}

static void set_wipe_cache_flag(int flag) {
    char* env_name = "wipe_cache";
    char* fail = "failed";
    char* success = "successful";
    char *fw_argv_failed[] = { "fw_setenv",
        env_name,
        flag?success:fail,
        NULL };

    if(0 == fw_setenv(3, fw_argv_failed)){
        sync();
        printf("fw_setenv \"wipe_cache=%s\" ok\n", fw_getenv(env_name));
    }else{
        printf("fw_setenv \"wipe_cache=%s\" fail\n", fw_getenv(env_name));
    }
}

static void set_wipe_param_flag(int flag) {
    char* env_name = "wipe_param";
    char* fail = "failed";
    char* success = "successful";
    char *fw_argv_failed[] = { "fw_setenv",
            env_name,
            flag?success:fail,
            NULL };

    if (0 == fw_setenv(3, fw_argv_failed)) {
        sync();
        printf("fw_setenv \"wipe_param=%s\" ok\n", fw_getenv(env_name));
    } else {
        printf("fw_setenv \"wipe_param=%s\" fail\n", fw_getenv(env_name));
    }
}
#endif

static void set_def_env_flag() {
    char platform_version[PROPERTY_VALUE_MAX+1] = {0};
    property_get("ro.board.platform", platform_version, "");

    char *step = "4";
    if (!strcmp(platform_version, "gxbaby")) {
        step = "1";
    }

    char* env_name = "upgrade_step";
    char *fw_argv[] = { "fw_setenv",
        env_name,
        step,
        NULL };

    if(0 == fw_setenv(3, fw_argv)){
        sync();
        printf("fw_setenv \"upgrade_step=%s\" ok\n", fw_getenv(env_name));
    }else{
        printf("fw_setenv \"upgrade_step=%s\" fail\n", fw_getenv(env_name));
    }
    char* env_outputmode = "outputmode";
    char *fw_outputmode[] = { "fw_setenv",
        env_outputmode,
        "720p50hz",
        NULL };

    if(0 == fw_setenv(3, fw_outputmode)){
        sync();
        printf("fw_setenv \"outputmode=%s\" ok\n", fw_getenv(env_outputmode));
    }else{
        printf("fw_setenv \"outputmode=%s\" fail\n", fw_getenv(env_outputmode));
    }
    char* env_hdmimode = "hdmimode";
    char *fw_hdmimode[] = { "fw_setenv",
        env_hdmimode,
        "720p50hz",
        NULL };

    if(0 == fw_setenv(3, fw_hdmimode)){
        sync();
        printf("fw_setenv \"hdmimode=%s\" ok\n", fw_getenv(env_hdmimode));
    }else{
        printf("fw_setenv \"hdmimode=%s\" fail\n", fw_getenv(env_hdmimode));
    }
}

static void
set_sdcard_update_bootloader_message() {
    struct bootloader_message boot;
    memset(&boot, 0, sizeof(boot));
    strlcpy(boot.command, "boot-recovery", sizeof(boot.command));
    strlcpy(boot.recovery, "recovery\n", sizeof(boot.recovery));
    set_bootloader_message(&boot);
}

// How much of the temp log we have copied to the copy in cache.
static long tmplog_offset = 0;

static void save_update_status(const char *aml_update_version) {
    char buffer[512] = {0};

    FILE *pfile = fopen_path(CURRENT_STATUS_FILE, "w");
    if (pfile == NULL) {
        LOGE("Can't open %s\n", CURRENT_STATUS_FILE);
    } else {
        sprintf(buffer, "aml_update_version=%s\naml_update_status=success", aml_update_version);
        fwrite(buffer, 1, strlen(buffer), pfile);
        fflush(pfile);
        fsync(fileno(pfile));
        check_and_fclose(pfile, CURRENT_STATUS_FILE);
        if (chmod(CURRENT_STATUS_FILE, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH) < 0) {
            LOGE("Can't chmod %s\n", CURRENT_STATUS_FILE);
        }
    }
}

void
copy_log_file(const char* source, const char* destination, int append) {
    struct stat st;

    if (lstat(source, &st) < 0) {
        return;
    }

    FILE *log = fopen_path(destination, append ? "a" : "w");
    if (log == NULL) {
        LOGE("Can't open %s\n", destination);
    } else {
        FILE *tmplog = fopen(source, "r");
        if (tmplog != NULL) {
            if (append) {
                fseek(tmplog, tmplog_offset, SEEK_SET);  // Since last write
            }
            char buf[4096];
            while (fgets(buf, sizeof(buf), tmplog)) fputs(buf, log);
            if (append) {
                tmplog_offset = ftell(tmplog);
            }
            check_and_fclose(tmplog, source);
        }
        check_and_fclose(log, destination);
    }
}

// Rename last_log -> last_log.1 -> last_log.2 -> ... -> last_log.$max
// Overwrites any existing last_log.$max.
static void
rotate_last_logs(int max) {
    char oldfn[256];
    char newfn[256];

    int i;
    for (i = max-1; i >= 0; --i) {
        snprintf(oldfn, sizeof(oldfn), (i==0) ? LAST_LOG_FILE : (LAST_LOG_FILE ".%d"), i);
        snprintf(newfn, sizeof(newfn), LAST_LOG_FILE ".%d", i+1);
        // ignore errors
        rename(oldfn, newfn);
    }
}

static void
copy_logs() {
    // Copy logs to cache so the system can find out what happened.
    copy_log_file(TEMPORARY_LOG_FILE, LOG_FILE, true);
    copy_log_file(TEMPORARY_LOG_FILE, LAST_LOG_FILE, false);
    copy_log_file(TEMPORARY_INSTALL_FILE, LAST_INSTALL_FILE, false);
    chmod(LOG_FILE, 0600);
    chown(LOG_FILE, 1000, 1000);   // system user
    chmod(LAST_LOG_FILE, 0640);
    chmod(LAST_INSTALL_FILE, 0644);
    sync();
}

// clear the recovery command and prepare to boot a (hopefully working) system,
// copy our log file to cache as well (for the system to read), and
// record any intent we were asked to communicate back to the system.
// this function is idempotent: call it as many times as you like.
static void
finish_recovery(const char *send_intent) {
    // By this point, we're ready to return to the main system...
    if (send_intent != NULL) {
        FILE *fp = fopen_path(INTENT_FILE, "w");
        if (fp == NULL) {
            LOGE("Can't open %s\n", INTENT_FILE);
        } else {
            fputs(send_intent, fp);
            check_and_fclose(fp, INTENT_FILE);
        }
    }

    // Save the locale to cache, so if recovery is next started up
    // without a --locale argument (eg, directly from the bootloader)
    // it will use the last-known locale.
    if (locale != NULL) {
        LOGI("Saving locale \"%s\"\n", locale);
        FILE* fp = fopen_path(LOCALE_FILE, "w");
        fwrite(locale, 1, strlen(locale), fp);
        fflush(fp);
        fsync(fileno(fp));
        check_and_fclose(fp, LOCALE_FILE);
    }

    copy_logs();

    // Reset to normal system boot so recovery won't cycle indefinitely.
    struct bootloader_message boot;
    memset(&boot, 0, sizeof(boot));
    set_bootloader_message(&boot);

    // Remove the command file, so recovery won't repeat indefinitely.
    if (ensure_path_mounted(COMMAND_FILE) != 0 ||
        (unlink(COMMAND_FILE) && errno != ENOENT)) {
        LOGW("Can't unlink %s\n", COMMAND_FILE);
    }

    ensure_path_unmounted(CACHE_ROOT);
    sync();  // For good measure.
}

static char *trim(char * src) {
    int i = 0;
    char *begin = src;

    while (src[i] != '\0') {
        if (src[i] != ' ' && src[i] != 0x09) {      //Tab:0x09
            break;
        } else {
            begin++;
        }
        i++;
    }

    for (i = strlen(begin) - 1; i >= 0;  i --) {
        if (begin[i] != ' ' && begin[i] != 0x09) {  //Tab:0x09
            break;
        } else {
            begin[i] = '\0';
        }
    }
    return begin;
}

static int
write_bootloader_message(void) {
    /* Write bootloader message to misc partition from
        /etc/bootloader_control_block.conf
    */
    FILE *fp = NULL;
    char buffer[1024] = {0};

    const char *CONFIG = "/etc/bootloader_control_block.conf";
    struct bootloader_message boot;
    struct MessageMember {
        char *name;
        char *value;
        int valueLenMax;
    } member[3] = {
        {"commannd", NULL, sizeof(boot.command)},
        {"status",  NULL, sizeof(boot.status)},
        {"recovery", NULL, sizeof(boot.recovery)},
    };

    if (access(CONFIG, F_OK)) {
        printf("can't find %s.\n", CONFIG);
        return -1;
    }

    fp = fopen(CONFIG, "r");
    if (fp == NULL) {
        printf("failed to open %s.\n", CONFIG);
        return -1;
    }

    int i = 0;
    while (fgets(buffer, sizeof(buffer)-1, fp)) {
        for (i = 0; buffer[i] && isspace(buffer[i]); i ++) ;
        if (buffer[i] == '#' || buffer[i] == '\0') continue;

        char *line = strdup((char *)(buffer + i));
        if (!strncmp(line, member[0].name, strlen(member[0].name))) {
            member[0].value = strdup((char *)(line+strlen(member[0].name)+1));
        } else if (!strncmp(line, member[1].name, strlen(member[1].name))) {
            member[1].value = strdup((char *)(line+strlen(member[1].name)+1));
        } else if (!strncmp(line, member[2].name, strlen(member[2].name))) {
            member[2].value = strdup((char *)(line+strlen(member[2].name)+1));
        } else {
            printf("unknow member(%s),skip!\n",line);
        }

        free(line);
    }

    for (i = 0; i < 3; i++) {
        if (member[i].value == NULL) {
            printf("can't get member(%s) value. (NULL).\n",
                member[i].name);
            goto ERR;
        }
        if (member[i].value[strlen(member[i].value) - 1] == 0x0A) {
            member[i].value[strlen(member[i].value) - 1] = '\0';
        }
        member[i].value = trim(member[i].value);
        if (strlen(member[i].value) > member[i].valueLenMax) {
            printf("range out of getting member(%s) value. (%d > %d).\n",
                member[i].name, strlen(member[i].value), member[i].valueLenMax);
            goto ERR;
        }
    }

    memset(&boot, 0, sizeof(boot));
    memcpy(boot.command, member[0].value, strlen(member[0].value));
    memcpy(boot.status, member[1].value, strlen(member[1].value));
    memcpy(boot.recovery, member[2].value, strlen(member[2].value));

    if (set_bootloader_message(&boot) < 0) {
        LOGE("Set bootloader message failed.\n");
        goto ERR;
    } else {
        struct bootloader_message boot_read;
        memset(&boot_read, 0, sizeof(boot_read));
        if (get_bootloader_message(&boot_read) < 0 ) {
            printf("set bootloader message succesful,but get failed.\n");
            goto ERR;
        }

        printf("bootloader_message:\n");
        printf("[commannd:%s]\n[status:%s]\n[recovery:%s]\n",
            boot_read.command, boot_read.status, boot_read.recovery);
        if (memcmp(&boot_read, &boot, sizeof(boot))) {
            printf("set bootloader message not match get.\n");
            goto ERR;
        }
    }

    LOGI("Set bootloader message successful.\n");
    return 0;

ERR:
    if (fp != NULL) {
        fclose(fp);
        fp = NULL;
    }
    return -1;
}

#define CUSTOMIZED_DATA_CMD "\"/sbin/recovery.data.sh / /system/customized_data.tar\""
//begin:add by zhanghk at 20181112:backup wifi config file in recovery and restore it after factory reset
#define CUSTOMIZED_DATA_CMD_JS "\"/sbin/recovery.data.sh / /tmp/customized_data.tar\""
#define CUSTOMIZED_BACKUP_DATA_CMD "\"/sbin/recovery_b.data.sh /tmp/customized_data.tar\""
//end:add by zhanghk at 20181112:backup wifi config file in recovery and restore it after factory reset
//begin:add by zongzy at 20191009:back sc private data
#define CUSTOMIZED_DATA_CMD_SC "\"/sbin/recovery.data.sh / /tmp/customized_data.tar\""
#define CUSTOMIZED_BACKUP_DATA_CMD_SC "\"/sbin/recovery_b.data_sc.sh /tmp/customized_data.tar\""
//end: add by zongzy

int install_customized_data()
{
	int fd = 0;
	printf("install_customized_data.\n");
	printf("install_customized_data\n");
	ensure_path_mounted("/system");
	ensure_path_mounted("/data");
	ensure_path_mounted("/cache");
	sleep(1);
    int ret = INSTALL_SUCCESS;
	//begin:add by zhanghk at 20181112:backup wifi config file in recovery and restore it after factory reset
    char province[92];
	property_get("ro.ysten.province", province, NULL);
	if(!strcmp(province, "CM201_jiangsu")){
        if ((access("/tmp/customized_data.tar", F_OK) == 0)){
            ret = recovery_run_cmd(CUSTOMIZED_DATA_CMD_JS);
        }else{
            printf("Can not found /tmp/customized_data.tar. Continue.\n");
		}
	}else if(!strcmp(province, "A20_sc")){
        if ((access("/tmp/customized_data.tar", F_OK) == 0)){
            ret = recovery_run_cmd(CUSTOMIZED_DATA_CMD_SC);
        }else{
            printf("Can not found /system/customized_data.tar. Continue.\n");
		} 
	}else{
        if ((access("/system/customized_data.tar", F_OK) == 0)){
            ret = recovery_run_cmd(CUSTOMIZED_DATA_CMD);
        }else{
            printf("Can not found /system/customized_data.tar. Continue.\n");
		} 
	}
	//end:add by zhanghk at 20181112:backup wifi config file in recovery and restore it after factory reset

        ensure_path_unmounted("/cache");
        ensure_path_unmounted("/data");
        ensure_path_unmounted("/system");
        return ret;
}

int backup_customized_data()
{
        int fd = 0;
        printf("backup_customized_data.\n");
        printf("backup_customized_data\n");
        ensure_path_mounted("/data");
        sleep(1);
        int ret = INSTALL_SUCCESS;
        ret = recovery_run_cmd(CUSTOMIZED_BACKUP_DATA_CMD);
        ensure_path_unmounted("/data");
        return ret;
}
//begin:add by zongzy at 20191009:back sc private data
int backup_customized_data_sc()
{
        int fd = 0;
        printf("backup_customized_data.\n");
        printf("backup_customized_data\n");
        ensure_path_mounted("/data");
        sleep(1);
        int ret = INSTALL_SUCCESS;
        ret = recovery_run_cmd(CUSTOMIZED_BACKUP_DATA_CMD_SC);
        ensure_path_unmounted("/data");
        return ret;
}
//end:add by zongzy

typedef struct _saved_log_file {
    char* name;
    struct stat st;
    unsigned char* data;
    struct _saved_log_file* next;
} saved_log_file;

int
erase_volume(const char *volume) {
    if(!volume){
        printf("ERR:the volume for erase is NULL! \n");
        return -1;
    }

#ifndef RECOVERY_IPTV
#ifdef FACTORY_RESET_ABNORMAL_PROTECT
    if(strcmp(volume, "/data") == 0) {
        set_wipe_data_flag(0);
    }
    if(strcmp(volume, "/cache") == 0) {
        set_wipe_cache_flag(0);
    }
    if(strcmp(volume, "/params") == 0) {
        set_wipe_param_flag(0);
    }
#endif
#endif

    printf("start erase volume: %s\n",volume);

    bool is_cache = (strcmp(volume, CACHE_ROOT) == 0);

#ifdef RECOVERY_IPTV
    if (iptv_has_wipe_ui) {
        ui->SetBackground(RecoveryUI::ERASING);
        ui->SetProgressType(RecoveryUI::INDETERMINATE);
    }
#else
    ui->SetBackground(RecoveryUI::ERASING);
    ui->SetProgressType(RecoveryUI::INDETERMINATE);
#endif

    saved_log_file* head = NULL;

    if (is_cache) {
        // If we're reformatting /cache, we load any
        // "/cache/recovery/last*" files into memory, so we can restore
        // them after the reformat.

        ensure_path_mounted(volume);

        DIR* d;
        struct dirent* de;
        d = opendir(CACHE_LOG_DIR);
        if (d) {
            char path[PATH_MAX];
            strcpy(path, CACHE_LOG_DIR);
            strcat(path, "/");
            int path_len = strlen(path);
            while ((de = readdir(d)) != NULL) {
                if (strncmp(de->d_name, "last", 4) == 0) {
                    saved_log_file* p = (saved_log_file*) malloc(sizeof(saved_log_file));
                    strcpy(path+path_len, de->d_name);
                    p->name = strdup(path);
                    if (stat(path, &(p->st)) == 0) {
                        // truncate files to 512kb
                        if (p->st.st_size > (1 << 19)) {
                            p->st.st_size = 1 << 19;
                        }
                        p->data = (unsigned char*) malloc(p->st.st_size);
                        FILE* f = fopen(path, "rb");
                        fread(p->data, 1, p->st.st_size, f);
                        fclose(f);
                        p->next = head;
                        head = p;
                    } else {
                        free(p);
                    }
                }
            }
            closedir(d);
        } else {
            if (errno != ENOENT) {
                printf("opendir failed: %s\n", strerror(errno));
            }
        }
    }

    ui->Print("Formatting %s...\n", volume);

    ensure_path_unmounted(volume);
    int result = 0;

    if (strcmp(volume, "/data") == 0){
        // reserve 32 KB for encryption
        result = format_volume(volume, 32);
    } else {
        result = format_volume(volume, 0);
    }

    if (is_cache) {
        while (head) {
            FILE* f = fopen_path(head->name, "wb");
            if (f) {
                fwrite(head->data, 1, head->st.st_size, f);
                fclose(f);
                chmod(head->name, head->st.st_mode);
                chown(head->name, head->st.st_uid, head->st.st_gid);
            }
            free(head->name);
            free(head->data);
            saved_log_file* temp = head->next;
            free(head);
            head = temp;
        }

        // Any part of the log we'd copied to cache is now gone.
        // Reset the pointer so we copy from the beginning of the temp
        // log.
        tmplog_offset = 0;
        copy_logs();
    }

    if(!result){
        sync();
        Volume* v = volume_for_path(volume);
        printf("format volume:%s sucessed!,now fsync the format device:\n",volume);
        int fd = open(v->blk_device, O_RDWR);
        if (fd < 0) {
            if(v->blk_device)
                printf("open device:%s failed \n",v->blk_device);
            else
                printf("open device failed,this device is NULL\n");
#ifdef FACTORY_RESET_ABNORMAL_PROTECT
            if(strcmp(volume, "/data") == 0) {
                set_wipe_data_flag(1);			
            }
#endif
            return -1;
        }
        fsync(fd);
        close(fd);
    }else{
        printf("format volume:%s failed!\n",volume);
	}

#ifndef RECOVERY_IPTV
#ifdef FACTORY_RESET_ABNORMAL_PROTECT
    if(strcmp(volume, "/data") == 0) {
        set_wipe_data_flag(1);			
    }
    if(strcmp(volume, "/cache") == 0) {
        set_wipe_cache_flag(1);
    }
    if(strcmp(volume, "/params") == 0) {
        set_wipe_param_flag(1);
    }
#endif
#endif
	return result;
}

#define RESTORE_SYSTEM_CMD "/sbin/restore_system.sh"

int do_restore_system() {
    int ret;
    printf("Restoring system...\n");
    ui->SetBackground(RecoveryUI::NONE);
    if(iptv_recovery_flag){
        ui->ShowText(false);
    }else{
        ui->ShowText(true);
    }
    ui->Print("\n-- Restoring system...\n");
    ret = recovery_run_cmd(RESTORE_SYSTEM_CMD);
    if(ret == 0) {
	printf("Restore system complete.\n");
	ui->Print("\n-- Restore system complete.\n");
    }
    return ret;
}

static char*
copy_sideloaded_package(const char* original_path) {
  if (ensure_path_mounted(original_path) != 0) {
    LOGE("Can't mount %s\n", original_path);
    return NULL;
  }

  if (ensure_path_mounted(SIDELOAD_TEMP_DIR) != 0) {
    LOGE("Can't mount %s\n", SIDELOAD_TEMP_DIR);
    return NULL;
  }

  if (mkdir(SIDELOAD_TEMP_DIR, 0700) != 0) {
    if (errno != EEXIST) {
      LOGE("Can't mkdir %s (%s)\n", SIDELOAD_TEMP_DIR, strerror(errno));
      return NULL;
    }
  }

  // verify that SIDELOAD_TEMP_DIR is exactly what we expect: a
  // directory, owned by root, readable and writable only by root.
  struct stat st;
  if (stat(SIDELOAD_TEMP_DIR, &st) != 0) {
    LOGE("failed to stat %s (%s)\n", SIDELOAD_TEMP_DIR, strerror(errno));
    return NULL;
  }
  if (!S_ISDIR(st.st_mode)) {
    LOGE("%s isn't a directory\n", SIDELOAD_TEMP_DIR);
    return NULL;
  }
  if ((st.st_mode & 0777) != 0700) {
    LOGE("%s has perms %o\n", SIDELOAD_TEMP_DIR, st.st_mode);
    return NULL;
  }
  if (st.st_uid != 0) {
    LOGE("%s owned by %lu; not root\n", SIDELOAD_TEMP_DIR, st.st_uid);
    return NULL;
  }

  char copy_path[PATH_MAX];
  strcpy(copy_path, SIDELOAD_TEMP_DIR);
  strcat(copy_path, "/package.zip");

  char* buffer = (char*)malloc(BUFSIZ);
  if (buffer == NULL) {
    LOGE("Failed to allocate buffer\n");
    return NULL;
  }

  size_t read;
  FILE* fin = fopen(original_path, "rb");
  if (fin == NULL) {
    LOGE("Failed to open %s (%s)\n", original_path, strerror(errno));
    return NULL;
  }
  FILE* fout = fopen(copy_path, "wb");
  if (fout == NULL) {
    LOGE("Failed to open %s (%s)\n", copy_path, strerror(errno));
    return NULL;
  }

  while ((read = fread(buffer, 1, BUFSIZ, fin)) > 0) {
    if (fwrite(buffer, 1, read, fout) != read) {
      LOGE("Short write of %s (%s)\n", copy_path, strerror(errno));
      return NULL;
    }
  }

  free(buffer);
  fflush(fout);
  fsync(fileno(fout));
  if (fclose(fout) != 0) {
    LOGE("Failed to close %s (%s)\n", copy_path, strerror(errno));
    return NULL;
  }

  if (fclose(fin) != 0) {
    LOGE("Failed to close %s (%s)\n", original_path, strerror(errno));
    return NULL;
  }

  // "adb push" is happy to overwrite read-only files when it's
  // running as root, but we'll try anyway.
  if (chmod(copy_path, 0400) != 0) {
    LOGE("Failed to chmod %s (%s)\n", copy_path, strerror(errno));
    return NULL;
  }

  return strdup(copy_path);
}

const char**
prepend_title(const char* const* headers) {
    // count the number of lines in our title, plus the
    // caller-provided headers.
    int count = 3;   // our title has 3 lines
    const char* const* p;
    for (p = headers; *p; ++p, ++count);

    const char** new_headers = (const char**)malloc((count+1) * sizeof(char*));
    const char** h = new_headers;
    *(h++) = "Android system recovery <" EXPAND(RECOVERY_API_VERSION) "e>";
    *(h++) = recovery_version;
    *(h++) = "";
    for (p = headers; *p; ++p, ++h) *h = *p;
    *h = NULL;

    return new_headers;
}

int
get_menu_selection(const char* const * headers, const char* const * items,
                   int menu_only, int initial_selection, Device* device) {
    // throw away keys pressed previously, so user doesn't
    // accidentally trigger menu items.
    ui->FlushKeys();

    ui->StartMenu(headers, items, initial_selection);
    int selected = initial_selection;
    int chosen_item = -1;

    while (chosen_item < 0) {
        int key = ui->WaitKey();
        int visible = ui->IsTextVisible();

        if (key == -1) {   // ui_wait_key() timed out
            if (ui->WasTextEverVisible()) {
                continue;
            } else {
                LOGI("timed out waiting for key input; rebooting.\n");
                ui->EndMenu();
                return 0; // XXX fixme
            }
        }

        int action = device->HandleMenuKey(key, visible);

        if (action < 0) {
            switch (action) {
                case Device::kHighlightUp:
                    --selected;
                    selected = ui->SelectMenu(selected);
                    break;
                case Device::kHighlightDown:
                    ++selected;
                    selected = ui->SelectMenu(selected);
                    break;
                case Device::kInvokeItem:
                    chosen_item = selected;
                    break;
                case Device::kNoAction:
                    break;
            }
        } else if (!menu_only) {
            chosen_item = action;
        }
    }

    ui->EndMenu();
    return chosen_item;
}

static int compare_string(const void* a, const void* b) {
    return strcmp(*(const char**)a, *(const char**)b);
}

static double get_memfree_size(void)
{
    FILE *fp = NULL;
    double size = 0.0;
    char rBuf[128] = {0};
    const char *meminfo = "/proc/meminfo";
    const char *memfree  = "MemFree:";

    fp = fopen(meminfo, "r");
    if (!fp) {
        printf("failed to open %s\n", meminfo);
        return -1;
    }

    while (fgets(rBuf, sizeof(rBuf), fp) != 0) {
        if (rBuf[strlen(rBuf)-1] == 0x0d || rBuf[strlen(rBuf)-1] == 0x0a) {
            rBuf[strlen(rBuf)-1] = '\0';
        }
        if (strstr(rBuf, memfree)) {
            printf("%s\n", rBuf);
            size = atof(rBuf+strlen(memfree));
            size /= 1024.0;
            break;
        }
        memset(rBuf, 0, sizeof(rBuf));
    }

    if (fp) {
        fclose(fp);
    }

    return size;    // unit is MB
}

static double get_file_size(const char *filepath)
{
    double size = 0.0;
    struct stat statbuff;

    if (stat(filepath, &statbuff) < 0) {
        printf("failed to stat %s(%s)\n", filepath, strerror(errno));
        return -1;
    } else {
        size = (double)statbuff.st_size;
    }

    size = size/1024.0/1024.0;
    printf("%s size=%.1fM\n", filepath, size);

    return size;    // unit is MB
}

static int is_copy_package(const char *zipfile)
{
    int ret = 0;
    double memfree_size = 0.0, zipfile_size = 0.0;
    const double MEMTOTAL_THRESHOLD = 512.0; // unit is MB
    const double ZIP_THRESHOLD = 250.0;

    memfree_size = get_memfree_size();
    zipfile_size = get_file_size(zipfile);
    printf("memtotal_size=%.1fM, zipfile_size=%.1fM\n", memfree_size, zipfile_size);

    if (memfree_size <= 0 || zipfile_size <= 0)
        return ret;

    if (memfree_size <= MEMTOTAL_THRESHOLD) {
        if (zipfile_size > ZIP_THRESHOLD) { // memtotal <= 512, zip > 250, not copy
            ret = 0;
        } else {                            // memtotal <= 512, zip <= 250, copy
            ret = 1;
        }
    } else {                                // memtotal > 512, copy
        ret = 1;
    }

    return ret;
}

static int
update_directory(const char* path, const char* unmount_when_done,
                 int* wipe_cache, Device* device) {
    ensure_path_mounted(path);

    const char* MENU_HEADERS[] = { "Choose a package to install:",
                                   path,
                                   "",
                                   NULL };
    DIR* d;
    struct dirent* de;
    d = opendir(path);
    if (d == NULL) {
        LOGE("error opening %s: %s\n", path, strerror(errno));
        if (unmount_when_done != NULL) {
            ensure_path_unmounted(unmount_when_done);
        }
        return 0;
    }

    const char** headers = prepend_title(MENU_HEADERS);

    int d_size = 0;
    int d_alloc = 10;
    char** dirs = (char**)malloc(d_alloc * sizeof(char*));
    int z_size = 1;
    int z_alloc = 10;
    char** zips = (char**)malloc(z_alloc * sizeof(char*));
    zips[0] = strdup("../");

    while ((de = readdir(d)) != NULL) {
        int name_len = strlen(de->d_name);

        if (de->d_type == DT_DIR) {
            // skip "." and ".." entries
            if (name_len == 1 && de->d_name[0] == '.') continue;
            if (name_len == 2 && de->d_name[0] == '.' &&
                de->d_name[1] == '.') continue;

            if (d_size >= d_alloc) {
                d_alloc *= 2;
                dirs = (char**)realloc(dirs, d_alloc * sizeof(char*));
            }
            dirs[d_size] = (char*)malloc(name_len + 2);
            strcpy(dirs[d_size], de->d_name);
            dirs[d_size][name_len] = '/';
            dirs[d_size][name_len+1] = '\0';
            ++d_size;
        } else if (de->d_type == DT_REG &&
                   name_len >= 4 &&
                   strncasecmp(de->d_name + (name_len-4), ".zip", 4) == 0) {
            if (z_size >= z_alloc) {
                z_alloc *= 2;
                zips = (char**)realloc(zips, z_alloc * sizeof(char*));
            }
            zips[z_size++] = strdup(de->d_name);
        }
    }
    closedir(d);

    qsort(dirs, d_size, sizeof(char*), compare_string);
    qsort(zips, z_size, sizeof(char*), compare_string);

    // append dirs to the zips list
    if (d_size + z_size + 1 > z_alloc) {
        z_alloc = d_size + z_size + 1;
        zips = (char**)realloc(zips, z_alloc * sizeof(char*));
    }
    memcpy(zips + z_size, dirs, d_size * sizeof(char*));
    free(dirs);
    z_size += d_size;
    zips[z_size] = NULL;

    int result;
    int chosen_item = 0;
    do {
        chosen_item = get_menu_selection(headers, zips, 1, chosen_item, device);

        char* item = zips[chosen_item];
        int item_len = strlen(item);
        if (chosen_item == 0) {          // item 0 is always "../"
            // go up but continue browsing (if the caller is update_directory)
            result = -1;
            break;
        } else if (item[item_len-1] == '/') {
            // recurse down into a subdirectory
            char new_path[PATH_MAX];
            strlcpy(new_path, path, PATH_MAX);
            strlcat(new_path, "/", PATH_MAX);
            strlcat(new_path, item, PATH_MAX);
            new_path[strlen(new_path)-1] = '\0';  // truncate the trailing '/'
            result = update_directory(new_path, unmount_when_done, wipe_cache, device);
            if (result >= 0) break;
        } else {
            // selected a zip file:  attempt to install it, and return
            // the status to the caller.
            char new_path[PATH_MAX];
            strlcpy(new_path, path, PATH_MAX);
            strlcat(new_path, "/", PATH_MAX);
            strlcat(new_path, item, PATH_MAX);

            ui->Print("\n-- Install %s ...\n", path);
            set_sdcard_update_bootloader_message();
            if (is_copy_package(new_path)) {
                ui->Print("Start copy %s to %s\n", new_path, SIDELOAD_TEMP_DIR);
                char* copy = copy_sideloaded_package(new_path);
                if (unmount_when_done != NULL) {
                    ensure_path_unmounted(unmount_when_done);
                }
                if (copy) {
                    result = install_package(copy, wipe_cache, TEMPORARY_INSTALL_FILE);
                    free(copy);
                } else {
                    result = INSTALL_ERROR;
                }
            } else {
                ui->Print("Don't copy %s to %s\n", new_path, SIDELOAD_TEMP_DIR);
                const char *update_package = new_path;
                if (ensure_path_mounted(update_package) != 0) {
                    LOGE("Can't mount %s\n", update_package);
                    result = INSTALL_ERROR;
                    break;
                }
                result = install_package(update_package, wipe_cache, TEMPORARY_INSTALL_FILE);
                ui->Print("\nInstall %s %s\n", update_package, (result==INSTALL_SUCCESS) ? "successful" : "failed");
            }

            break;
        }
    } while (true);

    int i;
    for (i = 0; i < z_size; ++i) free(zips[i]);
    free(zips);
    free(headers);

    if (unmount_when_done != NULL) {
        ensure_path_unmounted(unmount_when_done);
    }
    return result;
}

static void
wipe_data(int confirm, Device* device) {
    if (confirm) {
        static const char** title_headers = NULL;

        if (title_headers == NULL) {
            const char* headers[] = { "Confirm wipe of all user data?",
                                      "  THIS CAN NOT BE UNDONE.",
                                      "",
                                      NULL };
            title_headers = prepend_title((const char**)headers);
        }

        const char* items[] = { " No",
                                " No",
                                " No",
                                " No",
                                " No",
                                " No",
                                " No",
                                " Yes -- delete all user data",   // [7]
                                " No",
                                " No",
                                " No",
                                NULL };

        int chosen_item = get_menu_selection(title_headers, items, 1, 0, device);
        if (chosen_item != 7) {
            return;
        }
    }

	//begin:add by zhanghk at 20181112:backup wifi config file in recovery and restore it after factory reset
	char province[92];
	property_get("ro.ysten.province", province, NULL);
	if(!strcmp(province, "CM201_jiangsu")){
        ui->Print("\n-- backuping data...\n");
        if(keep_data)
            backup_customized_data();
	}
	//end:add by zhanghk at 20181112:backup wifi config file in recovery and restore it after factory reset
	//begin:add by zongzy at 20191009:back sc private data
	if(!strcmp(province, "A20_sc")){
        ui->Print("\n-- backuping data...\n");
		backup_customized_data_sc();
	}
	//end:add by zongzy
    ui->Print("\n-- Wiping data...\n");
    device->WipeData();
    erase_volume("/data");
    erase_volume("/cache");
	install_customized_data();
    ui->Print("Data wipe complete.\n");
}

#ifdef RECOVERY_HAS_PARAM
static void
wipe_param(int confirm, Device* device) {
    if (confirm) {
        static const char** title_headers = NULL;

        if (title_headers == NULL) {
            const char* headers[] = { "Confirm wipe of param partition?",
                                      "  THIS CAN NOT BE UNDONE.",
                                      "",
                                      NULL };
            title_headers = prepend_title((const char**)headers);
        }

        const char* items[] = { " No",
                                " No",
                                " No",
                                " No",
                                " No",
                                " No",
                                " No",
                                " Yes -- wipe param partition",   // [7]
                                " No",
                                " No",
                                " No",
                                NULL };

        int chosen_item = get_menu_selection(title_headers, items, 1, 0, device);
        if (chosen_item != 7) {
            return;
        }
    }

    ui->Print("\n-- Wiping param...\n");
    erase_volume("/params");
    ui->Print("Param wipe complete.\n");
}
#endif /* RECOVERY_HAS_PARAM */


#ifdef RECOVERY_HAS_MEDIA
static void
wipe_media(int confirm,  Device* device) {
    if (confirm) {
        const char** title_headers = NULL;

        if (title_headers == NULL) {
            const char* headers[] = { "Confirm wipe of all media data?",
                                "  THIS CAN NOT BE UNDONE.",
                                "",
                                NULL };
            
            title_headers = prepend_title((const char**)headers);
        }

        char* items[] = { " No",
                          " No",
                          " No",
                          " No",
                          " No",
                          " No",
                          " No",
                          " Yes -- delete all media data",   // [7]
                          " No",
                          " No",
                          " No",
                          NULL };

        int chosen_item = get_menu_selection(title_headers, items, 1, 0, device);
        if (chosen_item != 7) {
            return;
        }
    }

    ui->Print("\n-- Wiping media...\n");
    erase_volume(MEDIA_ROOT);
    ui->Print("Media wipe complete.\n");
}
#endif /* RECOVERY_HAS_MEDIA */

static void ext_update(Device* device) {
	int status = 0;
	int wipe_cache = 0;
	const char** title_headers = NULL;
	const char* headers[] = { "Confirm update?",
                            "  THIS CAN NOT BE UNDONE.",
                            "",
                            NULL };
	title_headers = prepend_title((const char**)headers);
	char* items[] = { " ../",
                 " Update from sdcard",
                 " Update from udisk",
                  NULL };
	int chosen_item = get_menu_selection(title_headers, items, 1, 0, device);
	if (chosen_item != 1 && chosen_item != 2){
	    return;
	}

	switch(chosen_item) {
        case 1:
    		// Some packages expect /cache to be mounted (eg,
    		// standard incremental packages expect to use /cache
    		// as scratch space).
            ensure_path_mounted(CACHE_ROOT);
            status = update_directory(SDCARD_ROOT, SDCARD_ROOT, &wipe_cache, device);
            if (status == INSTALL_SUCCESS && wipe_cache){
                ui->Print("\n-- Wiping cache (at package request)...\n");
                if (erase_volume("/cache")) {
                    ui->Print("Cache wipe failed.\n");
                }else {
                    ui->Print("Cache wipe complete.\n");
                }
            }
            if (status >= 0){
                if (status != INSTALL_SUCCESS){
                    ui->SetBackground(RecoveryUI::ERROR);
                    ui->Print("Installation aborted.\n");
                }else if (!ui->IsTextVisible()){
                    return ;  // reboot if logs aren't visible
                }else{
                    ui->Print("\nInstall from sdcard complete.\n");
                }
            }
            break;

    	case 2:
    	    ensure_path_mounted(CACHE_ROOT);
    	    status = update_directory(UDISK_ROOT, UDISK_ROOT, &wipe_cache, device);
    	    if (status == INSTALL_SUCCESS && wipe_cache){
    	        ui->Print("\n-- Wiping cache (at package request)...\n");
    	        if (erase_volume("/cache")) {
    	            ui->Print("Cache wipe failed.\n");
    	        }else {
    	            ui->Print("Cache wipe complete.\n");
    	        }
    	        }
    	    if (status >= 0) {
    	        if (status != INSTALL_SUCCESS) {
    	            ui->SetBackground(RecoveryUI::ERROR);
    	            ui->Print("Installation aborted.\n");
    	        } else if (!ui->IsTextVisible()) {
    	            return ;  // reboot if logs aren't visible
    	        } else {
    	            ui->Print("\nInstall from udisk complete.\n");
    	        }
    	    }
    	    break;
	}
}

static int
prompt_and_wait(Device* device, int status) {
    const char* const* headers = prepend_title(device->GetMenuHeaders());
    for (;;) {
        finish_recovery(NULL);
        switch (status) {
            case INSTALL_SUCCESS:
            case INSTALL_NONE:
                ui->SetBackground(RecoveryUI::NO_COMMAND);
                break;

            case INSTALL_ERROR:
            case INSTALL_CORRUPT:
                ui->SetBackground(RecoveryUI::ERROR);
                break;
        }
        ui->SetProgressType(RecoveryUI::EMPTY);

        int chosen_item = get_menu_selection(headers, device->GetMenuItems(), 0, 0, device);

        // device-specific code may take some action here.  It may
        // return one of the core actions handled in the switch
        // statement below.
        chosen_item = device->InvokeMenuItem(chosen_item);

        int wipe_cache;
        switch (chosen_item) {
            case Device::REBOOT:
                return REBOOT_NORMAL;
#ifdef RECOVERY_HAS_FACTORY_TEST
            case Device::FACTORY_TEST:
		        return REBOOT_FACTORY_TEST;
#endif
            case Device::WIPE_DATA:
                wipe_data(ui->IsTextVisible(), device);
		if (ensure_path_mounted(BOOTFILES_ROOT) != 0) {
			printf("Can't mount 'bootfiles', wipe it!\n");
			if (erase_volume(BOOTFILES_ROOT)) {
				printf("bootfiles wipe failed.\n");
			}
		}
                if (!ui->IsTextVisible()) return REBOOT_NORMAL;
                if ((access("/bootfiles/bootfail.flag", F_OK) == 0))
			remove("/bootfiles/bootfail.flag");
                else
			printf("Can not found /bootfiles/bootfail.flag Continue.\n");
                break;

            case Device::WIPE_CACHE:
                ui->Print("\n-- Wiping cache...\n");
                erase_volume("/cache");
                ui->Print("Cache wipe complete.\n");
                if (!ui->IsTextVisible()) return REBOOT_NORMAL;
                break;

            case Device::FACTORY_RESET:
                ui->Print("\n-- Wiping cache...\n");
                erase_volume("/cache");
                ui->Print("Cache wipe complete.\n");
				erase_volume("/params");
                ui->Print("Cache params complete.\n");
                erase_volume("/data");
                ui->Print("Cache data complete.\n");
		if (ensure_path_mounted(BOOTFILES_ROOT) != 0) {
			printf("Can't mount 'bootfiles', wipe it!\n");
			if (erase_volume(BOOTFILES_ROOT)) {
				printf("bootfiles wipe failed.\n");
			}
		}
                if ((access("/bootfiles/bootfail.flag", F_OK) == 0))
			remove("/bootfiles/bootfail.flag");
                else
			printf("Can not found /bootfiles/bootfail.flag Continue.\n"); 
  				//if (!ui->IsTextVisible())  
				return REBOOT_NORMAL;

                break;


#ifdef RECOVERY_HAS_MEDIA
            case Device::WIPE_MEDIA:
                wipe_media(ui->IsTextVisible(), device);
                if (!ui->IsTextVisible()) return REBOOT_NORMAL;
                break;
#endif /* RECOVERY_HAS_MEDIA */
#ifdef RECOVERY_HAS_PARAM
            case Device::WIPE_PARAM:
                wipe_param(ui->IsTextVisible(), device);
                if (!ui->IsTextVisible()) return REBOOT_NORMAL;
                break;
#endif /* RECOVERY_HAS_PARAM */
#ifdef RECOVERY_HAS_BOOTFILES
            case Device::APPLY_BOOTFILES:
                ensure_path_mounted(CACHE_ROOT);
                //status = update_directory(BOOTFILES_ROOT, NULL, &wipe_cache, device);
                if (ensure_path_mounted(BOOTFILES_INSTALL_FILE) != 0) {
                    LOGE("Can't mount %s\n", BOOTFILES_INSTALL_FILE);
                    status = INSTALL_ERROR;
                    break;
                }
                status = install_package(BOOTFILES_INSTALL_FILE, &wipe_cache, TEMPORARY_INSTALL_FILE);
                if (status == INSTALL_SUCCESS && wipe_cache) {
                    ui->Print("\n-- Wiping cache (at package request)...\n");
                    if (erase_volume("/cache")) {
                        ui->Print("Cache wipe failed.\n");
                    } else {
                        ui->Print("Cache wipe complete.\n");
                    }
                }
                if (status >= 0) {
                    if (status != INSTALL_SUCCESS) {
                        ui->SetBackground(RecoveryUI::ERROR);
                        ui->Print("Installation aborted.\n");
                        break;
                    } else if (!ui->IsTextVisible()) {
                        remove("/bootfiles/bootfail.flag");
                        return REBOOT_NORMAL;  // reboot if logs aren't visible
                    } else {
                        remove("/bootfiles/bootfail.flag");
                        ui->Print("\nInstall from bootfiles complete.\n");
                    }
                }
                return REBOOT_NORMAL;
#endif /* RECOVERY_HAS_BOOTFILES */

            case Device::APPLY_EXT:
                ext_update(device);
                break;

            case Device::APPLY_CACHE:
                // Don't unmount cache at the end of this.
                status = update_directory(CACHE_ROOT, NULL, &wipe_cache, device);
                if (status == INSTALL_SUCCESS && wipe_cache) {
                    ui->Print("\n-- Wiping cache (at package request)...\n");
                    if (erase_volume("/cache")) {
                        ui->Print("Cache wipe failed.\n");
                    } else {
                        ui->Print("Cache wipe complete.\n");
                    }
                }
                if (status >= 0) {
                    if (status != INSTALL_SUCCESS) {
                        ui->SetBackground(RecoveryUI::ERROR);
                        ui->Print("Installation aborted.\n");
                    } else if (!ui->IsTextVisible()) {
                        return REBOOT_NORMAL;  // reboot if logs aren't visible
                    } else {
                        ui->Print("\nInstall from cache complete.\n");
                    }
                }
                break;

            case Device::APPLY_ADB_SIDELOAD:
                status = apply_from_adb(ui, &wipe_cache, TEMPORARY_INSTALL_FILE);
                if (status >= 0) {
                    if (status != INSTALL_SUCCESS) {
                        ui->SetBackground(RecoveryUI::ERROR);
                        ui->Print("Installation aborted.\n");
                        copy_logs();
                    } else if (!ui->IsTextVisible()) {
                        return REBOOT_NORMAL;  // reboot if logs aren't visible
                    } else {
                        ui->Print("\nInstall from ADB complete.\n");
                    }
                }
                break;
#ifdef RECOVERY_HAS_EFUSE
            case Device::OPERATE_EFUSE:
                recovery_efuse(-1, NULL, device);
                if (!ui->IsTextVisible()) return REBOOT_NORMAL;
                break;
#endif /* RECOVERY_HAS_EFUSE */
        }
    }
    return REBOOT_RECOVERY_AGAIN;
}

static void
print_property(const char *key, const char *name, void *cookie) {
    printf("%s=%s\n", key, name);
}

static void
load_locale_from_cache() {
    FILE* fp = fopen_path(LOCALE_FILE, "r");
    char buffer[80];
    if (fp != NULL) {
        fgets(buffer, sizeof(buffer), fp);
        int j = 0;
        unsigned int i;
        for (i = 0; i < sizeof(buffer) && buffer[i]; ++i) {
            if (!isspace(buffer[i])) {
                buffer[j++] = buffer[i];
            }
        }
        buffer[j] = 0;
        locale = strdup(buffer);
        check_and_fclose(fp, LOCALE_FILE);
    }
}

static RecoveryUI* gCurrentUI = NULL;

void
ui_print(const char* format, ...) {
    char buffer[256];

    va_list ap;
    va_start(ap, format);
    vsnprintf(buffer, sizeof(buffer), format, ap);
    va_end(ap);

    if (gCurrentUI != NULL) {
        gCurrentUI->Print("%s", buffer);
    } else {
        fputs(buffer, stdout);
    }
}

static void
volume_damaged_handle(void) {
    char *wipe_cache = fw_getenv("wipe_cache");
    if (!wipe_cache || strcmp(wipe_cache, "failed")) {
        return;
    }

    const char *volume = "/cache";
    ensure_path_unmounted(volume);
    Volume *v = volume_for_path(volume);
    if (v == NULL) {
        printf("unknown volume for path [%s]\n",
            volume);
        return;
    }

    struct selabel_handle *sehandle = NULL;
    struct selinux_opt seopts[] = {
        { SELABEL_OPT_PATH, "/file_contexts" }
    };

    sehandle = selabel_open(SELABEL_CTX_FILE, seopts, 1);
    int ret = make_ext4fs(v->blk_device, 0, v->mount_point, sehandle);
    if (sehandle) selabel_close(sehandle);
    printf("%s partition maybe damaged when last poweroff.\nmake_ext4fs first (%s).\n\n",
        v->blk_device, (ret == 0) ? "successful" : "failed");
}

int
main(int argc, char **argv) {
    time_t start = time(NULL);

    // If these fail, there's not really anywhere to complain...
    // freopen(TEMPORARY_LOG_FILE, "a", stdout); setbuf(stdout, NULL);
    //freopen(TEMPORARY_LOG_FILE, "a", stderr); setbuf(stderr, NULL);
      freopen("/dev/console", "a", stdout); setbuf(stdout, NULL);
      freopen("/dev/console", "a", stderr); setbuf(stderr, NULL);
    // If this binary is started with the single argument "--adbd",
    // instead of being the normal recovery binary, it turns into kind
    // of a stripped-down version of adbd that only supports the
    // 'sideload' command.  Note this must be a real argument, not
    // anything in the command file or bootloader control block; the
    // only way recovery should be run with this argument is when it
    // starts a copy of itself from the apply_from_adb() function.
    if (argc == 2 && strcmp(argv[1], "--adbd") == 0) {
        adb_main();
        return 0;
    }

    printf("Starting recovery on %s", ctime(&start));

    load_volume_table();
    volume_damaged_handle();
    ensure_path_mounted(LAST_LOG_FILE);
    rotate_last_logs(10);
    get_args(&argc, &argv);

#ifdef FACTORY_RESET_ABNORMAL_PROTECT
    factory_reset_wipe(&argc, &argv);
#endif
	
    int previous_runs = 0;
    const char *aml_update_version = NULL;
    const char *send_intent = NULL;
    const char *update_package = NULL;
    int wipe_data = 0, wipe_cache = 0, show_text = 0,update_from_udisk=0;
    int restore_system = 0;
    const char *update_patch = NULL;
    char *file_copy_from_partition_args = NULL;
    int reboot_to_factorymode = 0;
	int usb_burning = 0;
	int without_format = 0;
#ifdef RECOVERY_HAS_MEDIA
    int wipe_media = 0;
#endif /* RECOVERY_HAS_MEDIA */
#ifdef RECOVERY_HAS_PARAM
    int wipe_param = 0;
#endif /* RECOVERY_HAS_PARAM */
#ifdef RECOVERY_HAS_EFUSE
    const char *efuse_version = NULL;
    int set_efuse_version = 0;
    int set_efuse_ethernet_mac = 0;
    int set_efuse_bluetooth_mac = 0;
#ifdef EFUSE_LICENCE_ENABLE
    int set_efuse_audio_license = 0;
#endif /* EFUSE_LICENCE_ENABLE */
#endif /* RECOVERY_HAS_EFUSE */

#ifdef RECOVERY_WRITE_KEY
    int flash_write_mac = 0;
    int flash_write_mac_force = 0;
    int flash_write_mac_bt = 0;
    int flash_write_mac_bt_force = 0;
    int flash_write_mac_wifi = 0;
    int flash_write_mac_wifi_force = 0;
    int flash_write_hdcp = 0;
    int flash_write_hdcp_force = 0;
    int flash_write_usid = 0;
    int flash_write_usid_force = 0;
#endif /* RECOVERY_WRITE_KEY */

    bool just_exit = false;
    int run_cmd = 0;
    char *cmd_args = NULL;
    char *keep_file_path = NULL;

    int arg;
    while ((arg = getopt_long(argc, argv, "", OPTIONS, NULL)) != -1) {
        switch (arg) {
        case 'q': aml_update_version = optarg; break;
        case 'p': previous_runs = atoi(optarg); break;
        case 's': send_intent = optarg; break;
        case 'u': update_package = optarg; break;
        case 'x': update_patch = optarg; break;
#ifdef RECOVERY_HAS_PARAM
        //case 'w': wipe_data = wipe_cache = wipe_param = 1; break;
        case 'w': wipe_data = wipe_cache = 1; break;
#else
        case 'w': wipe_data = wipe_cache = 1; break;
#endif
        case 'g': restore_system = 1; break;
        case 'c': wipe_cache = 1; break;
        case 't': show_text = 1; break;
        //case 'x': just_exit = true; break;
		case 'U': update_from_udisk = 1;break;
        case 'l': locale = optarg; break;
        case 'f': reboot_to_factorymode = 1; break;
		case 'n': usb_burning = 1; break;
		case 'o': without_format = 1; break;	
	case 'z': file_copy_from_partition_args = optarg; break;
#ifdef RECOVERY_HAS_MEDIA
        case 'm': wipe_media = 1; break;
#endif /* RECOVERY_HAS_MEDIA */
#ifdef RECOVERY_HAS_PARAM
        case 'P': wipe_param = 1; break;
#endif /* RECOVERY_HAS_PARAM */

#ifdef RECOVERY_HAS_EFUSE
        case 'v': set_efuse_version = 1; efuse_version = optarg; break;
        case 'd': set_efuse_ethernet_mac = 1; break;
        case 'b': set_efuse_bluetooth_mac = 1; break;
#ifdef EFUSE_LICENCE_ENABLE
        case 'a': set_efuse_audio_license = 1; break;
#endif /* EFUSE_LICENCE_ENABLE */

#endif /* RECOVERY_HAS_EFUSE */

#ifdef RECOVERY_WRITE_KEY
        case 'B': flash_write_mac = 1; break;
        case 'C': flash_write_mac_force = 1; break;
        case 'D': flash_write_mac_bt = 1; break;
        case 'E': flash_write_mac_bt_force = 1; break;
        case 'F': flash_write_mac_wifi = 1; break;
        case 'G': flash_write_mac_wifi_force = 1; break;
        case 'H': flash_write_hdcp = 1; break;
        case 'I': flash_write_hdcp_force = 1; break;
        case 'J': flash_write_usid = 1; break;
        case 'K': flash_write_usid_force = 1; break;
#endif /* RECOVERY_WRITE_KEY */

        case 'e': just_exit = true; break;
        case 'r': run_cmd = 1; cmd_args = optarg; break;
        case 'h': keep_file_path = optarg; wipe_data = wipe_cache = 1; break;
        case 'j': keep_data = 1; wipe_data = wipe_cache = 1; break;
        case '?':
            LOGE("Invalid command argument\n");
            continue;
        }
    }

    if (locale == NULL) {
        load_locale_from_cache();
    }
    printf("locale is [%s]\n", locale);

    Device* device = make_device();
    ui = device->GetUI();
    gCurrentUI = ui;

    ui->Init();
    ui->SetLocale(locale);
    ui->SetBackground(RecoveryUI::NONE);
    if (show_text) ui->ShowText(true);

    struct selinux_opt seopts[] = {
      { SELABEL_OPT_PATH, "/file_contexts" }
    };

    sehandle = selabel_open(SELABEL_CTX_FILE, seopts, 1);

    if (!sehandle) {
        ui->Print("Warning: No file_contexts\n");
    }

    device->StartRecovery();

    printf("Command:");
    for (arg = 0; arg < argc; arg++) {
        printf(" \"%s\"", argv[arg]);
    }
    printf("\n");

    /**
     *  Disable auto reformat, we should *NOT* do this.
     *
     *  For /media partition, we cannot do it because this will break
     *  any file system that's non-FAT.
     * 
     *  For /data & /cache, If it is yaffs, format or not is ok, yaffs can be mounted even haven`t been format,
     *  If it is ubifs or ext4, it is necessary to format it, make sure it can be mounted if user didn`t do that.
	 *  It is recommended to add wipe data/cache in factory_update_param.aml, but uncomment below code
	 *  
     */
#ifdef USB_BURN
    if (ensure_path_mounted("/data") != 0) {
        ui->Print("Can't mount 'data', wipe it!\n");
        if (erase_volume("/data")) {
            ui->Print("Data wipe failed.\n");
        }
    }

    if (ensure_path_mounted("/cache") != 0) {
        ui->Print("Can't mount 'cache', wipe it!\n");
        if (erase_volume("/cache")) {
            ui->Print("Cache wipe failed.\n");
        }
    }

#ifdef RECOVERY_HAS_MEDIA
    if (ensure_path_mounted(MEDIA_ROOT) != 0) {
        ui->Print("Can't mount 'media', wipe it!\n");
        if (erase_volume(MEDIA_ROOT)) {
            ui->Print("Media wipe failed.\n");
        }
    }
#endif

#ifdef RECOVERY_HAS_PARAM
    if (ensure_path_mounted(PARAM_ROOT) != 0) {
        ui->Print("Can't mount 'param', wipe it!\n");
        if (erase_volume(PARAM_ROOT)) {
            ui->Print("Param wipe failed.\n");
        }
    }
#endif

#endif /* 0 */

    if(file_copy_from_partition_args)
    {
        char *file_path = NULL;
        char *partition_type = NULL;
        char *partition = NULL;
        char *size_str = NULL;

        if(((file_path = strtok(file_copy_from_partition_args, ":")) == NULL)
            ||((partition_type = strtok(NULL, ":")) == NULL) 
            ||((partition = strtok(NULL, ":")) == NULL)  
            ||((size_str = strtok(NULL, ":")) == NULL))
        {
            printf("file_copy_from_partition_args Invalid!\n");
        }
        else
        {
            ssize_t file_size = atoi(size_str);
            file_copy_from_partition(file_path, partition_type, partition, file_size);
        }
    }
    
    if (update_package) {
        // For backwards compatibility on the cache partition only, if
        // we're given an old 'root' path "CACHE:foo", change it to
        // "/cache/foo".
        if (strncmp(update_package, "CACHE:", 6) == 0) {
            int len = strlen(update_package) + 10;
            char* modified_path = (char*)malloc(len);
            strlcpy(modified_path, "/cache/", len);
            strlcat(modified_path, update_package+6, len);
            printf("(replacing path \"%s\" with \"%s\")\n",
                   update_package, modified_path);
            update_package = modified_path;
        }
    }
    printf("\n");

    property_list(print_property, NULL);
    property_get("ro.build.display.id", recovery_version, "");
    printf("\n");

    int status = INSTALL_SUCCESS;

	if(usb_burning)
	{
		int opt;
		opt = without_format;
		//usb_burning_main(opt);
	}

#ifdef RECOVERY_IPTV
    if (update_package != NULL || update_patch != NULL) {
        if (wipe_data || wipe_cache) {
	    iptv_has_wipe_ui = true;
        } else {
	    iptv_has_wipe_ui = false;
        }
    }
#endif

    if (update_package != NULL) {
        status = install_package(update_package, &wipe_cache, TEMPORARY_INSTALL_FILE);
        // If wipe_cache in package, so don't erase here, in the following to erase
        /*if (status == INSTALL_SUCCESS && wipe_cache) {
            if (erase_volume("/cache")) {
                LOGE("Cache wipe (requested by package) failed.");
            }
        }*/
        if (status != INSTALL_SUCCESS) ui->Print("Installation aborted.\n");
        char buffer[PROPERTY_VALUE_MAX+1];
        property_get("ro.build.fingerprint", buffer, "");
        if (strstr(buffer, ":userdebug/") || strstr(buffer, ":eng/")) {
            if(iptv_recovery_flag){
                ui->ShowText(false);
            }else{
                ui->ShowText(true);
            }
        }
    }
	
	if (update_from_udisk)
	{
		ui->Print("update_from_udisk.\n");
		const char *zip_path = "/udisk/update.zip";
        if (ensure_path_mounted(zip_path) != 0) {
            LOGE("Can't mount %s\n", zip_path);
            status = INSTALL_ERROR;
        }
        status = install_package(zip_path, &wipe_cache, TEMPORARY_INSTALL_FILE);

        if (status != INSTALL_SUCCESS) ui->Print("Installation aborted.\n");
        char buffer[PROPERTY_VALUE_MAX+1];
        property_get("ro.build.fingerprint", buffer, "");
        if (strstr(buffer, ":userdebug/") || strstr(buffer, ":eng/")) {
            if(iptv_recovery_flag){
                ui->ShowText(false);
            }else{
                ui->ShowText(true);
            }
        }
		
	}

    if (update_patch != NULL) {
        status = install_package(update_patch, &wipe_cache, TEMPORARY_INSTALL_FILE);
        if (status != INSTALL_SUCCESS) ui->Print("Installation patch aborted.\n");
        char buffer[PROPERTY_VALUE_MAX+1];
        property_get("ro.build.fingerprint", buffer, "");
        if (strstr(buffer, ":userdebug/") || strstr(buffer, ":eng/")) {
            if(iptv_recovery_flag){
                ui->ShowText(false);
            }else{
                ui->ShowText(true);
            }
        }
    }
    if(keep_file_path != NULL) {
        LOGW("keep file path: %s \n", keep_file_path);
        if (ensure_path_mounted(TEMP_KEEP_FILE) == 0) {
            if(ensure_path_mounted(keep_file_path)==0){
                copy_custom_data(TEMP_KEEP_FILE, keep_file_path);
            } else {
                LOGE("can't mount %s \n", keep_file_path);
            }
        } else {
            LOGE("can't mount %s \n", TEMP_KEEP_FILE);
        }
    }
    if (wipe_data) {
        //if (device->WipeData()) status = INSTALL_ERROR;
#ifdef RECOVERY_IPTV
        set_wipe_data_flag(0);
#endif
        //begin:add by zhanghk at 20181112:backup wifi config file in recovery and restore it after factory reset
		char province[92];
		property_get("ro.ysten.province", province, NULL);
		if(!strcmp(province, "CM201_jiangsu")){
	        if(keep_data)
                backup_customized_data();
		}
		//end:add by zhanghk at 20181112:backup wifi config file in recovery and restore it after factory reset
		//begin:add by zongzy at 20191009:back sc private data
		if(!strcmp(province, "A20_sc")){
			ui->Print("\n-- backuping data...\n");
			backup_customized_data_sc();
		}
		//end:add by zongzy
        if (erase_volume("/data")) status = INSTALL_ERROR;
        if (wipe_cache && erase_volume("/cache")) status = INSTALL_ERROR;
        if (status != INSTALL_SUCCESS) {
            ui->Print("Data wipe failed.\n");
        } else {
            if (install_customized_data()) status = INSTALL_ERROR;
            if (ensure_path_mounted(BOOTFILES_ROOT) != 0) {
		printf("Can't mount 'bootfiles', wipe it!\n");
		if (erase_volume(BOOTFILES_ROOT)) {
			printf("bootfiles wipe failed.\n");
		}
            }
            if ((access("/bootfiles/bootfail.flag", F_OK) == 0))
		remove("/bootfiles/bootfail.flag");
            else
		printf("Can not found /bootfiles/bootfail.flag Continue.\n");
            set_def_env_flag();
#ifdef RECOVERY_IPTV
            set_wipe_data_flag(1);
#endif
        }
    } else if (wipe_cache) {
        if (wipe_cache && erase_volume("/cache")) status = INSTALL_ERROR;
        if (status != INSTALL_SUCCESS) ui->Print("Cache wipe failed.\n");
    }

    if (restore_system) {
	if (restore_system && do_restore_system()) status = INSTALL_ERROR;
	if (status != INSTALL_SUCCESS) ui->Print("System restore failed.\n");
    }

#ifdef RECOVERY_HAS_MEDIA
    if (wipe_media) {
        if (wipe_media && erase_volume(MEDIA_ROOT)) status = INSTALL_ERROR;
        if (status != INSTALL_SUCCESS) ui->Print("Media wipe failed.\n");
    } 
#endif /* RECOVERY_HAS_MEDIA */ 

    if(keep_file_path != NULL) {
        ensure_path_mounted("/data");
        if(ensure_path_mounted(TEMP_KEEP_FILE)==0){
            copy_custom_data(DATA_TEMP_ROOT, TEMP_KEEP_FILE);
            chown_custom_directory(DATA_TEMP_ROOT, "system:system");
            chmod_custom_directory(DATA_TEMP_ROOT, "777");
        } else {
            LOGE("can't mount %s \n", TEMP_KEEP_FILE);
        }
        ensure_path_unmounted("/data");
    }

#ifdef RECOVERY_HAS_PARAM
    if (wipe_param) {
        if (wipe_param && erase_volume(PARAM_ROOT)) status = INSTALL_ERROR;
        if (status != INSTALL_SUCCESS) ui->Print("Param wipe failed.\n");
    } 
#endif /* RECOVERY_HAS_PARAM */

#ifdef RECOVERY_HAS_EFUSE
    if (set_efuse_version) {
        status = recovery_efuse(EFUSE_VERSION, efuse_version, device);
    }
#ifdef EFUSE_LICENCE_ENABLE
    if (set_efuse_audio_license) {
        status = recovery_efuse(EFUSE_LICENCE, NULL, device);
    }
#endif /* EFUSE_LICENCE_ENABLE */

    if (set_efuse_ethernet_mac) {
        status = recovery_efuse(EFUSE_MAC, NULL, device);
    }

    if (set_efuse_bluetooth_mac) {
        status = recovery_efuse(EFUSE_MAC_BT, NULL, device);
    }
#endif /* RECOVERY_HAS_EFUSE */

#ifdef RECOVERY_WRITE_KEY
    /* Recovery write mac, mac_bt, mac_wifi, hdcp key to flash */
    if(flash_write_mac |flash_write_mac_force |flash_write_mac_bt | flash_write_mac_bt_force |
        flash_write_mac_wifi |flash_write_mac_wifi_force |flash_write_hdcp |flash_write_hdcp_force) {
        if(!access(SDCARD_COMMAND_FILE, F_OK) && !access("/sdcard", W_OK)) {
            if(!update_package)
                ui->SetBackground(RecoveryUI::INSTALLING_UPDATE);
            ui->Print("\nRecovery start to write key ...\n");
            static int force_write_key = 0;
            // mac
            if(flash_write_mac && flash_write_mac_force) {
                ui->Print("flash write mac: not allow set 2 parameters at the same time\n");
                status = INSTALL_ERROR;
                goto PROMPT_AND_WAIT;
            }
            else if(flash_write_mac || flash_write_mac_force) {
                if(flash_write_mac) force_write_key = 0;
                if(flash_write_mac_force) force_write_key = 1;
                if(recovery_flash_write_key("mac", force_write_key, ui)) {
                    status = INSTALL_ERROR;
                    goto PROMPT_AND_WAIT;
                }
            }

            // mac_bt
            if(flash_write_mac_bt && flash_write_mac_bt_force) {
                ui->Print("flash write mac_bt: not allow set 2 parameters at the same time\n");
                status = INSTALL_ERROR;
                goto PROMPT_AND_WAIT;
            }
            else if(flash_write_mac_bt || flash_write_mac_bt_force) {
                if(flash_write_mac_bt) force_write_key = 0;
                if(flash_write_mac_bt_force) force_write_key = 1;
                if(recovery_flash_write_key("mac_bt", force_write_key, ui)) {
                    status = INSTALL_ERROR;
                    goto PROMPT_AND_WAIT;
                }
            }

            // mac_wifi
            if(flash_write_mac_wifi && flash_write_mac_wifi_force) {
                ui->Print("flash write mac_wifi: not allow set 2 parameters at the same time\n");
                status = INSTALL_ERROR;
                goto PROMPT_AND_WAIT;
            }
            else if(flash_write_mac_wifi || flash_write_mac_wifi_force) {
                if(flash_write_mac_wifi) force_write_key = 0;
                if(flash_write_mac_wifi_force) force_write_key = 1;
                if(recovery_flash_write_key("mac_wifi", force_write_key, ui)) {
                    status = INSTALL_ERROR;
                    goto PROMPT_AND_WAIT;
                }
            }

            // usid
            if(flash_write_usid && flash_write_usid_force) {
                ui->Print("flash write usid: not allow set 2 parameters at the same time\n");
                status = INSTALL_ERROR;
                goto PROMPT_AND_WAIT;
            }
            else if(flash_write_usid || flash_write_usid_force) {
                if(flash_write_usid) force_write_key = 0;
                if(flash_write_usid_force) force_write_key = 1;
                if(recovery_flash_write_key("usid", force_write_key, ui)) {
                    status = INSTALL_ERROR;
                    goto PROMPT_AND_WAIT;
                }
            }

            // hdcp
            if(flash_write_hdcp && flash_write_hdcp_force) {
                ui->Print("flash write hdcp: not allow set 2 parameters at the same time\n");
                status = INSTALL_ERROR;
                goto PROMPT_AND_WAIT;
            }
            else if(flash_write_hdcp || flash_write_hdcp_force) {
                if(flash_write_hdcp) force_write_key = 0;
                if(flash_write_hdcp_force) force_write_key = 1;
                if(recovery_flash_write_key("hdcp", force_write_key, ui)) {
                    status = INSTALL_ERROR;
                    goto PROMPT_AND_WAIT;
                }
            }
        }
        else if(access("/sdcard", W_OK)) {
            ui->Print("Warning:sdcard doesn't write permission!\n");
            status = INSTALL_ERROR;
            goto PROMPT_AND_WAIT;
        }
        else {
            ui->Print("Warning:%s not exist!\n", SDCARD_COMMAND_FILE);
            status = INSTALL_ERROR;
            goto PROMPT_AND_WAIT;
        }
    }
#endif /* RECOVERY_WRITE_KEY */

    if (run_cmd) {
    	ui->Print("run command %s\n", cmd_args);
    	status = recovery_run_cmd(cmd_args);
    }

    int howReboot;
    if (status == INSTALL_ERROR || status == INSTALL_CORRUPT) {
        copy_logs();
        ui->SetBackground(RecoveryUI::ERROR);
    }

#ifndef RECOVERY_DISABLE_ADB_SIDELOAD
    adb_listeners(ui, argc, argv);
#endif

    if ((aml_update_version != NULL) && (status == INSTALL_SUCCESS)) {
        save_update_status(aml_update_version);
    }

PROMPT_AND_WAIT:
    if (status != INSTALL_SUCCESS || ui->IsTextVisible()) {
        if(iptv_recovery_flag){
            ui->ShowText(false);
        }else{
            ui->ShowText(true);
        }

#ifdef RECOVERY_IPTV
        iptv_has_wipe_ui = true;
#endif

        howReboot = prompt_and_wait(device, status);
        if (REBOOT_FACTORY_TEST == howReboot)
            reboot_to_factorymode = 1;
    }

    // Otherwise, get ready to boot the main system...
    finish_recovery(send_intent);

#ifdef BOOTLOADER_CONTROL_BLOCK
    if ((update_package != NULL) || (update_patch != NULL)) {
        printf("\nWrite bootloader control block datas to misc partition!\n");
        if (write_bootloader_message() < 0) {
            ui->ShowText(true);
            goto PROMPT_AND_WAIT;
        }
    }
#endif

    sync();
#if 0
    if (reboot_to_factorymode) {
        property_set("androidboot.mode", "factorytest");
        android_reboot(ANDROID_RB_RESTART2, 0, "factory_testl_reboot");
    } else {
        android_reboot(ANDROID_RB_RESTART2, 0, "normal_reboot");
    }
#endif
    //begin by lizheng for led flash 20190821
    ui->Print("ota finished...\n");
	ota_finish_flag = 1;
    ui->Print("check hisfactorymode...\n");
	
	ensure_path_mounted(UDISK_ROOT);
    if((access(FACTORY_MODE,F_OK)==0) && (getifupgrade() == 1))
	{
        ui->SetBackground(RecoveryUI::INSTALLING_UPDATE);
        ui->SetProgressType(RecoveryUI::DETERMINATE);
        ui->ShowProgress(1, 1);
	 ui->Print("not reboot because of hisfactorymode...\n");
     ensure_path_unmounted(UDISK_ROOT);   
	
	 while(1)
	 {
	 sleep(1);
	 }
	 } 
	else
	ensure_path_unmounted(UDISK_ROOT);
	ui->Print("no hisfactorymode,reboot ...\n");
	//end by lizheng for led flash 20190821	
    property_set(ANDROID_RB_PROPERTY, "reboot,");
    return EXIT_SUCCESS;
}
