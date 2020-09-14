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

#include <ctype.h>
#include <errno.h>
#include <fcntl.h>
#include <limits.h>
#include <sys/stat.h>
#include <sys/wait.h>
#include <unistd.h>
#include <string.h>
#include <pthread.h>

#include "common.h"
#include "install.h"
#include "mincrypt/rsa.h"
#include "minui/minui.h"
#include "minzip/SysUtil.h"
#include "minzip/Zip.h"
#include "mtdutils/mounts.h"
#include "mtdutils/mtdutils.h"
#include "cmd_excute.h"

#include "roots.h"
#include "verifier.h"
#include "ui.h"
#include "secure_check.h"

extern "C"{
#include "fw_env.h"
#include "check/dtbcheck.h"
}

extern RecoveryUI* ui;

#define ASSUMED_UPDATE_BINARY_NAME  "META-INF/com/google/android/update-binary"
#define PUBLIC_KEYS_FILE "/res/keys"
//add by lizheng for led flash 20190821
#define LED_OFF_CMD "\"/sbin/led_flash.sh  off\""
#define LED_ON_CMD "\"/sbin/led_flash.sh  on\""

// Default allocation of progress bar segments to operations
static const int VERIFICATION_PROGRESS_TIME = 60;
static const float VERIFICATION_PROGRESS_FRACTION = 0.25;
static const float DEFAULT_FILES_PROGRESS_FRACTION = 0.4;
static const float DEFAULT_IMAGE_PROGRESS_FRACTION = 0.1;

//begin by lizheng for led flash 20190821
pthread_t  led_t;
extern int ota_finish_flag;
//add by chenfeng for led flash 20200120
int ifupgrade=0;

//If upgrade error set uprade_step 2
int iptv_recovery_flag = 0;
static void set_upgrade_step(char * step)
{
	if(!step)
		return;
	char *fw_argv[] = { "fw_setenv",
    				"upgrade_step",
    				step,
    				NULL };
              
    if(0 == fw_setenv(3, fw_argv)){
        sync();
        LOGI("Successful to set\"upgrade_step=2\" before install_package!\n");
    }else{
        LOGE("Failed to set\"upgrade_step=2\" before install_package!\n");
    }
#ifdef RECOVERY_IPTV_NOT_SHOW_TEXT
    iptv_recovery_flag = 1;
#endif
}

// If the package contains an update binary, extract it and run it.
static int
try_update_binary(const char *path, ZipArchive *zip, int* wipe_cache) {
    const ZipEntry* binary_entry =
            mzFindZipEntry(zip, ASSUMED_UPDATE_BINARY_NAME);
    if (binary_entry == NULL) {
        mzCloseZipArchive(zip);
        return INSTALL_CORRUPT;
    }

    const char* binary = "/tmp/update_binary";
    unlink(binary);
    int fd = creat(binary, 0755);
    if (fd < 0) {
        mzCloseZipArchive(zip);
        LOGE("Can't make %s\n", binary);
        return INSTALL_ERROR;
    }
    bool ok = mzExtractZipEntryToFile(zip, binary_entry, fd);
    close(fd);
    mzCloseZipArchive(zip);

    if (!ok) {
        LOGE("Can't copy %s\n", ASSUMED_UPDATE_BINARY_NAME);
        return INSTALL_ERROR;
    }

    int pipefd[2];
    pipe(pipefd);

    // When executing the update binary contained in the package, the
    // arguments passed are:
    //
    //   - the version number for this interface
    //
    //   - an fd to which the program can write in order to update the
    //     progress bar.  The program can write single-line commands:
    //
    //        progress <frac> <secs>
    //            fill up the next <frac> part of of the progress bar
    //            over <secs> seconds.  If <secs> is zero, use
    //            set_progress commands to manually control the
    //            progress of this segment of the bar
    //
    //        set_progress <frac>
    //            <frac> should be between 0.0 and 1.0; sets the
    //            progress bar within the segment defined by the most
    //            recent progress command.
    //
    //        firmware <"hboot"|"radio"> <filename>
    //            arrange to install the contents of <filename> in the
    //            given partition on reboot.
    //
    //            (API v2: <filename> may start with "PACKAGE:" to
    //            indicate taking a file from the OTA package.)
    //
    //            (API v3: this command no longer exists.)
    //
    //        ui_print <string>
    //            display <string> on the screen.
    //
    //   - the name of the package zip file.
    //

    const char** args = (const char**)malloc(sizeof(char*) * 5);
    args[0] = binary;
    args[1] = EXPAND(RECOVERY_API_VERSION);   // defined in Android.mk
    char* temp = (char*)malloc(10);
    sprintf(temp, "%d", pipefd[1]);
    args[2] = temp;
    args[3] = (char*)path;
    args[4] = NULL;

    pid_t pid = fork();
    if (pid == 0) {
        close(pipefd[0]);
        execv(binary, (char* const*)args);
        fprintf(stdout, "E:Can't run %s (%s)\n", binary, strerror(errno));
        _exit(-1);
    }
    close(pipefd[1]);

    //don`t overlay the value from recovery command file
    //*wipe_cache = 0;

    char buffer[1024];
    FILE* from_child = fdopen(pipefd[0], "r");
    while (fgets(buffer, sizeof(buffer), from_child) != NULL) {
        char* command = strtok(buffer, " \n");
        if (command == NULL) {
            continue;
        } else if (strcmp(command, "progress") == 0) {
            char* fraction_s = strtok(NULL, " \n");
            char* seconds_s = strtok(NULL, " \n");

            float fraction = strtof(fraction_s, NULL);
            int seconds = strtol(seconds_s, NULL, 10);

            ui->ShowProgress(fraction * (1-VERIFICATION_PROGRESS_FRACTION), seconds);
        } else if (strcmp(command, "set_progress") == 0) {
            char* fraction_s = strtok(NULL, " \n");
            float fraction = strtof(fraction_s, NULL);
            ui->SetProgress(fraction);
        } else if (strcmp(command, "ui_print") == 0) {
            char* str = strtok(NULL, "\n");
            if (str) {
                ui->Print("%s", str);
            } else {
                ui->Print("\n");
            }
            fflush(stdout);
        } else if (strcmp(command, "wipe_cache") == 0) {
            *wipe_cache = 1;
        } else if (strcmp(command, "clear_display") == 0) {
            ui->SetBackground(RecoveryUI::NONE);
        } else {
            LOGE("unknown command [%s]\n", command);
        }
    }
    fclose(from_child);

    int status;
    waitpid(pid, &status, 0);
#if 0
    if (!WIFEXITED(status) || WEXITSTATUS(status) != 0) {
        LOGE("Error in %s\n(Status %d)\n", path, WEXITSTATUS(status));
        return INSTALL_ERROR;
    }
#else
    int result = -1;
    if (WIFEXITED(status)) {
        if (WEXITSTATUS(status) != 0) {
            LOGE("the child process end code is %d\n", WEXITSTATUS(status));
            result = -1;
        } else {
            result = 0;
        }
    } else if (WIFSIGNALED(status)) {
        LOGE("some signal caused the child process to die,this signal number is %d\n", WTERMSIG(status));
        result = -1;
    }

    if (result < 0) {
        LOGE("Error in %s\n(Status %d)\n", path, WEXITSTATUS(status));
        return INSTALL_ERROR;
    }
#endif

    return INSTALL_SUCCESS;
}

#ifdef RECOVERY_WIPE_BOOT_BEFORE_UPGRADE
static int 
is_update_bootloader(const unsigned char *updater_script)
{
    char *str = NULL;
    const char *WRITE_RAW_IMAGE = "write_raw_image";
    const char *PACKAGE_EXTRACT_FILE = "package_extract_file";
    const char *BOOTLOADER_IMG = "bootloader.img";
    const char *BOOTLOADER = "bootloader";

    // updater-script: write_raw_image(package_extract_file("bootloader.img"), "bootloader");
    // printf("updater-script:\n%s", updater_script);
    str = strtok((char *)updater_script, "\n");
    while (str != NULL) {
        if (!strncmp(str, WRITE_RAW_IMAGE, strlen(WRITE_RAW_IMAGE))) {
            if (strstr(str, PACKAGE_EXTRACT_FILE) && strstr(str, BOOTLOADER_IMG) && strstr(str, BOOTLOADER)) {
                printf("Found %s in updater-script.\n", str);
                return 1;
            }
        }
        str = strtok(NULL, "\n");
    }

    printf("Can't find %s in updater-script.\n", 
                "write_raw_image(package_extract_file(\"bootloader.img\"), \"bootloader\");");
    return 0;
}

static int 
wipe_boot_before_upgrade(ZipArchive pZipArchive)
{
    int i = 0;
    int count = 0, fd = 0;
    int updaterScriptSize = 0;
    int isUpdateBootloader = 0;
    bool success = false;
    char emmcPartitionPath[64] = {0};
    unsigned char *updaterScriptBuf = NULL;
    char wBuf[1024*1024] = {0};     // default: 1M
    const char *emmcPartitionName[] = {"mmcblk0boot0", "mmcblk0boot1", "mmcblk1boot0", "mmcblk1boot1"};
    const char *ASSUMED_UPDATER_SCRIPT_NAME = "META-INF/com/google/android/updater-script";

    const ZipEntry *pZipEntry = mzFindZipEntry(&pZipArchive, ASSUMED_UPDATER_SCRIPT_NAME);
    if (pZipEntry == NULL) {
        LOGE("Can't find %s in ota package\n", ASSUMED_UPDATER_SCRIPT_NAME);
        return -1;
    }

    updaterScriptSize = mzGetZipEntryUncompLen(pZipEntry);
    if (updaterScriptSize <= 0) {
        LOGE("Can't get zip entry uncomp len(%s, updater-script file size=%d)\n", strerror(errno), updaterScriptSize);
        return -1;
    }

    updaterScriptBuf = (unsigned char*)calloc(updaterScriptSize, sizeof(unsigned char));
    if (!updaterScriptBuf) {
        LOGE("Can't calloc %d size space(%s) in memory\n", updaterScriptSize, strerror(errno));
        return -1;
    }
    // printf("ota package updater-script data address:0x%x, size:%d\n", updaterScriptBuf, updaterScriptSize);

    success = mzExtractZipEntryToBuffer(&pZipArchive, pZipEntry, updaterScriptBuf);
    if (!success) {
        LOGE("Can't extract zip entry to buffer\n");
        if (updaterScriptBuf) {
            free(updaterScriptBuf);
        }
        return -1;
    }

    if (is_update_bootloader(updaterScriptBuf)) {
        printf("We will clear boot0 & boot1 partition.\n");
        isUpdateBootloader = 1;
    } else {
        printf("We don't clear boot0 & boot1 partition.\n");
        isUpdateBootloader = 0;
    }

    if (isUpdateBootloader) {
        for(i=0; i<sizeof(emmcPartitionName)/sizeof(emmcPartitionName[0]); i++) {
            memset(emmcPartitionPath, 0, sizeof(emmcPartitionPath));
            sprintf(emmcPartitionPath, "/dev/block/%s", emmcPartitionName[i]);
            if(!access(emmcPartitionPath, F_OK)) {
                fd = open(emmcPartitionPath, O_WRONLY);
                if (fd < 0) {
                    LOGE("Can't to open %s.%s\n", emmcPartitionPath, strerror(errno));
                    return -1;
                }
                count = write(fd, wBuf, sizeof(wBuf));
                close(fd);
                LOGI("Have written %d bytes 0 to %s\n", count, emmcPartitionPath);
            }
        }
    }

    if (updaterScriptBuf) {
        free(updaterScriptBuf);
    }

    return 0;
}
#endif /* RECOVERY_WIPE_BOOT_BEFORE_UPGRADE */

//begin by lizheng for led flash 20190821
void* led_thread(void* args)
{   
    while(ota_finish_flag == 0){
	recovery_flash_led(LED_OFF_CMD);
	sleep(1);
	recovery_flash_led(LED_ON_CMD);
	sleep(1);
	}
	recovery_flash_led(LED_OFF_CMD);
    return NULL;
}
//end by lizheng for led flash 20190821

//add by chenfeng for led flash 20200120
int getifupgrade()
{
return ifupgrade;
}

static int
really_install_package(const char *path, int* wipe_cache)
{
    set_upgrade_step("3");
    ui->SetBackground(RecoveryUI::INSTALLING_UPDATE);
    ui->Print("Finding update package...\n");
    // Give verification half the progress bar...
    ui->SetProgressType(RecoveryUI::DETERMINATE);
    ui->ShowProgress(VERIFICATION_PROGRESS_FRACTION, VERIFICATION_PROGRESS_TIME);
    LOGI("Update location: %s\n", path);

    if (ensure_path_mounted(path) != 0) {
        LOGE("Can't mount %s\n", path);
        set_upgrade_step("2");
        return INSTALL_CORRUPT;
    }
    
    //add by chenfeng for led flash 20200120
    ifupgrade = 1;

    /* Check board whether is encrypted and rsa whether is the same.
     */
    int retSecureCheck = -1;
    ui->Print("\nStart to check secure update...\n");
    retSecureCheck = aml_secure_upgrade_check(path);
    if(retSecureCheck == 0x1234) {
        ui->Print("Kernel not support check secure upgrade package,skip...\n\n");
    } else if(retSecureCheck <= 0) {
        ui->Print("Check secure upgrade package doesn't pass!\n\n");
        set_upgrade_step("2");
        return INSTALL_CORRUPT;
    } else {
        ui->Print("Check secure upgrade package pass!\n\n");
    }
#ifdef RECOVERY_BACKUP_RECOVERY
    RecoveryDtbParse();
#endif
    ui->Print("Opening update package...\n");
    //begin by lizheng for led flash 20190821
	ui->Print("led flash...\n");
	pthread_create(&led_t, NULL, led_thread, NULL);
	//end by lizheng for led flash 20190821
	

    int numKeys;
    Certificate* loadedKeys = load_keys(PUBLIC_KEYS_FILE, &numKeys);
    if (loadedKeys == NULL) {
        LOGE("Failed to load keys\n");
        set_upgrade_step("2");
        return INSTALL_CORRUPT;
    }
    LOGI("%d key(s) loaded from %s\n", numKeys, PUBLIC_KEYS_FILE);

    ui->Print("Verifying update package...\n");

    int err;
    err = verify_file(path, loadedKeys, numKeys);
    free(loadedKeys);
    LOGI("verify_file returned %d\n", err);
    if (err != VERIFY_SUCCESS) {
        set_upgrade_step("2");
        LOGE("signature verification failed\n");
        return INSTALL_CORRUPT;
    }

    /* Try to open the package.
     */
    ZipArchive zip;
    err = mzOpenZipArchive(path, &zip);
    if (err != 0) {
        set_upgrade_step("2");
        LOGE("Can't open %s\n(%s)\n", path, err != -1 ? strerror(err) : "bad");
        return INSTALL_CORRUPT;
    }

#ifdef RECOVERY_WIPE_BOOT_BEFORE_UPGRADE
    if (wipe_boot_before_upgrade(zip) < 0) {
        set_upgrade_step("2");
        return INSTALL_CORRUPT;
    }
#endif

    /* Verify and install the contents of the package.
     */
    ui->Print("Installing update...\n");
    return try_update_binary(path, &zip, wipe_cache);
}

int
install_package(const char* path, int* wipe_cache, const char* install_file)
{
    FILE* install_log = fopen_path(install_file, "w");
    if (install_log) {
        fputs(path, install_log);
        fputc('\n', install_log);
    } else {
        LOGE("failed to open last_install: %s\n", strerror(errno));
    }
    int result;
    if (setup_install_mounts() != 0) {
        LOGE("failed to set up expected mounts for install; aborting\n");
        set_upgrade_step("2");
        result = INSTALL_ERROR;
    } else {
        result = really_install_package(path, wipe_cache);
        if(result != INSTALL_SUCCESS){
            LOGE("failed to really_install_package\n");
            set_upgrade_step("2");
        }
    }
    if (install_log) {
        fputc(result == INSTALL_SUCCESS ? '1' : '0', install_log);
        fputc('\n', install_log);
        fclose(install_log);
    }
    return result;
}
