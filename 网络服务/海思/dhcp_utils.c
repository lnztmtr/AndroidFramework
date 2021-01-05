/*
 * Copyright 2008, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* Utilities for managing the dhcpcd DHCP client daemon */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <stdbool.h>

#include <cutils/properties.h>

#ifdef ANDROID
#define LOG_TAG "DHCP_UTILS"
#include <cutils/log.h>
#endif
static const char DAEMON_NAME[]        = "dhcpcd";
static const char DAEMON_PROP_NAME[]   = "init.svc.dhcpcd";
static const char HOSTNAME_PROP_NAME[] = "net.hostname";
static const char DHCP_PROP_NAME_PREFIX[]  = "dhcp";
static const char DHCP_CONFIG_PATH[]   = "/system/etc/dhcpcd/dhcpcd.conf";
static const int NAP_TIME = 10;   /* wait for 200ms at a time */
                                  /* when polling for property values */
static const char DAEMON_NAME_RENEW[]  = "iprenew";
static char errmsg[100];
static const char VERDORSPEC[] = "dhcp.vendorspecinfo";
static const char VERDORID[] = "dhcp.vendorclassid";
static const char USERNAME[] = "dhcp.username";
static const char PASSWD[] = "dhcp.password";
/* interface length for dhcpcd daemon start (dhcpcd_<interface> as defined in init.rc file)
 * or for filling up system properties dhcpcd.<interface>.ipaddress, dhcpcd.<interface>.dns1
 * and other properties on a successful bind
 */
#define MAX_INTERFACE_LENGTH 25

/*
 * P2p interface names increase sequentially p2p-p2p0-1, p2p-p2p0-2.. after
 * group formation. This does not work well with system properties which can quickly
 * exhaust or for specifiying a dhcp start target in init which requires
 * interface to be pre-defined in init.rc file.
 *
 * This function returns a common string p2p for all p2p interfaces.
 */
void get_p2p_interface_replacement(const char *interface, char *p2p_interface) {
    /* Use p2p for any interface starting with p2p. */
    if (strncmp(interface, "p2p",3) == 0) {
        strncpy(p2p_interface, "p2p", MAX_INTERFACE_LENGTH);
    } else {
        strncpy(p2p_interface, interface, MAX_INTERFACE_LENGTH);
    }
}

/*
 * Wait for a system property to be assigned a specified value.
 * If desired_value is NULL, then just wait for the property to
 * be created with any value. maxwait is the maximum amount of
 * time in seconds to wait before giving up.
 */
static int wait_for_property(const char *name, const char *desired_value, int maxwait)
{
    char value[PROPERTY_VALUE_MAX] = {'\0'};
    int maxnaps = (maxwait * 1000) / NAP_TIME;

    if (maxnaps < 1) {
        maxnaps = 1;
    }

    while (maxnaps-- > 0) {
        usleep(NAP_TIME * 1000);
        if (property_get(name, value, NULL)) {
            if (desired_value == NULL ||
                    strcmp(value, desired_value) == 0) {
                return 0;
            }
        }
    }
    return -1; /* failure */
}

static int fill_ip_info(const char *interface,
                     char *ipaddr,
                     char *gateway,
                     uint32_t *prefixLength,
                     char *dns[],
                     char *server,
                     uint32_t *lease,
                     char *vendorInfo,
                     char *domain,
                     char *mtu)
{
    char prop_name[PROPERTY_KEY_MAX];
    char prop_value[PROPERTY_VALUE_MAX];
    /* Interface name after converting p2p0-p2p0-X to p2p to reuse system properties */
    char p2p_interface[MAX_INTERFACE_LENGTH];
    int x;

    get_p2p_interface_replacement(interface, p2p_interface);

    snprintf(prop_name, sizeof(prop_name), "%s.%s.ipaddress", DHCP_PROP_NAME_PREFIX, p2p_interface);
    property_get(prop_name, ipaddr, NULL);

    snprintf(prop_name, sizeof(prop_name), "%s.%s.gateway", DHCP_PROP_NAME_PREFIX, p2p_interface);
    property_get(prop_name, gateway, NULL);

    snprintf(prop_name, sizeof(prop_name), "%s.%s.server", DHCP_PROP_NAME_PREFIX, p2p_interface);
    property_get(prop_name, server, NULL);

    //TODO: Handle IPv6 when we change system property usage
    if (gateway[0] == '\0' || strncmp(gateway, "0.0.0.0", 7) == 0) {
        //DHCP server is our best bet as gateway
        strncpy(gateway, server, PROPERTY_VALUE_MAX);
    }

    snprintf(prop_name, sizeof(prop_name), "%s.%s.mask", DHCP_PROP_NAME_PREFIX, p2p_interface);
    if (property_get(prop_name, prop_value, NULL)) {
        int p;
        // this conversion is v4 only, but this dhcp client is v4 only anyway
        in_addr_t mask = ntohl(inet_addr(prop_value));
        // Check netmask is a valid IP address.  ntohl gives NONE response (all 1's) for
        // non 255.255.255.255 inputs.  if we get that value check if it is legit..
        if (mask == INADDR_NONE && strcmp(prop_value, "255.255.255.255") != 0) {
            snprintf(errmsg, sizeof(errmsg), "DHCP gave invalid net mask %s", prop_value);
            return -1;
        }
        for (p = 0; p < 32; p++) {
            if (mask == 0) break;
            // check for non-contiguous netmask, e.g., 255.254.255.0
            if ((mask & 0x80000000) == 0) {
                snprintf(errmsg, sizeof(errmsg), "DHCP gave invalid net mask %s", prop_value);
                return -1;
            }
            mask = mask << 1;
        }
        *prefixLength = p;
    }

    for (x=0; dns[x] != NULL; x++) {
        snprintf(prop_name, sizeof(prop_name), "%s.%s.dns%d", DHCP_PROP_NAME_PREFIX, p2p_interface, x+1);
        property_get(prop_name, dns[x], NULL);
    }

    snprintf(prop_name, sizeof(prop_name), "%s.%s.leasetime", DHCP_PROP_NAME_PREFIX, p2p_interface);
    if (property_get(prop_name, prop_value, NULL)) {
        *lease = atol(prop_value);
    }

    snprintf(prop_name, sizeof(prop_name), "%s.%s.vendorInfo", DHCP_PROP_NAME_PREFIX,
            p2p_interface);
    property_get(prop_name, vendorInfo, NULL);

    snprintf(prop_name, sizeof(prop_name), "%s.%s.domain", DHCP_PROP_NAME_PREFIX,
            p2p_interface);
    property_get(prop_name, domain, NULL);

    snprintf(prop_name, sizeof(prop_name), "%s.%s.mtu", DHCP_PROP_NAME_PREFIX,
            p2p_interface);
    property_get(prop_name, mtu, NULL);

    return 0;
}

static const char *ipaddr_to_string(in_addr_t addr)
{
    struct in_addr in_addr;

    in_addr.s_addr = addr;
    return inet_ntoa(in_addr);
}

void dhcp_passwdEncode(char* passwdEncode, const char* passwd){
    if(passwd == NULL)
       return;

    unsigned int i;
    char * keyPS ="Hi6si0li7con1For2Chi8na3Set4Top5Bo9x";
    char ckey = 0;
    int  keylen = strlen(keyPS);

    char prop_value[PROPERTY_VALUE_MAX];
    property_get("sys.ethernet.password.encrypt", prop_value, "false");
    if (!strcmp(prop_value, "false")){/*sys.ethernet.password.encrypt = false , don't encrypt just return passwd*/
        snprintf(passwdEncode, strlen(passwd) + 1, "%s", passwd);
        return;
    }

    for(i = 0; i < strlen(passwd); i++){
        ckey = keyPS[i%keylen];// 循环获取key
        if(ckey != passwdEncode[i]){
            passwdEncode[i] = passwd[i] ^ ckey;
        }else{ //key equals passwd, after xor, passwdEncode[i]='\0'. this will cause decode error,so we don't encode it
            passwdEncode[i] = passwd[i];
        }
    }
    //ALOGD("dhcp_connect passwd[%s]", passwd);
    //ALOGD("dhcp_connect passwdEncode[%s]", passwdEncode);
    return;
}

/*
 * Start the dhcp client daemon, and wait for it to finish
 * configuring the interface.
 *
 * The device init.rc file needs a corresponding entry for this work.
 *
 * Example:
 * service dhcpcd_<interface> /system/bin/dhcpcd -ABKL -f dhcpcd.conf
 */
int dhcp_do_request(const char *interface,
                    char *ipaddr,
                    char *gateway,
                    uint32_t *prefixLength,
                    char *dns[],
                    char *server,
                    uint32_t *lease,
                    char *vendorInfo,
                    char *domain,
                    char *mtu,
                    bool supportOption60,
                    const char *login,
                    const char *password,
                    bool supportOption125,
                    const char *option125Info)
{
    char result_prop_name[PROPERTY_KEY_MAX];
    char daemon_prop_name[PROPERTY_KEY_MAX];
    char prop_value[PROPERTY_VALUE_MAX] = {'\0'};
    char daemon_cmd[PROPERTY_VALUE_MAX * 2 + sizeof(DHCP_CONFIG_PATH)];
    const char *ctrl_prop = "ctl.start";
    const char *desired_status = "running";
    /* Interface name after converting p2p0-p2p0-X to p2p to reuse system properties */
    char p2p_interface[MAX_INTERFACE_LENGTH];

    get_p2p_interface_replacement(interface, p2p_interface);

    int dhcp_timeout = 30; // default: 30

    //begin: add by tianchining at 20191125: set timeout
    {
        char prop_province_value[PROPERTY_VALUE_MAX] = {'\0'};
        //ro.ysten.province
        property_get("ro.ysten.province", prop_province_value, NULL);
        if(strcmp(prop_province_value, "cm201_shandong") == 0){
            dhcp_timeout = 300; //5*60
            
        }
        /*add by zhaolianghua for guizhou IPOE requirment start @20191129*/
        else if(strcmp(prop_province_value, "cm201_guizhou") == 0){
            if(supportOption60){
                dhcp_timeout = 20;//add by zhaolianghua IPOE only may fail 3 times
            }else{
                dhcp_timeout = 300;//this is dhcp time
            }
        }
        /*add by zhaolianghua end*/
    }
    //end: add by tianchining at 20191125: set timeout

    char double_stack[PROPERTY_VALUE_MAX] = {'\0'};
    property_get("persist.ethernet.doublestack", prop_value, NULL);
    if (!strcmp(prop_value, "true"))
        dhcp_timeout = 10;

    snprintf(result_prop_name, sizeof(result_prop_name), "%s.%s.result",
            DHCP_PROP_NAME_PREFIX,
            p2p_interface);

    ALOGD("dhcp_do_request start");
    if (NULL != strstr(interface, "."))
    {
        strncpy(p2p_interface, "vlan", MAX_INTERFACE_LENGTH);
    }

    snprintf(daemon_prop_name, sizeof(daemon_prop_name), "%s_%s",
            DAEMON_PROP_NAME,
            p2p_interface);

    /* Erase any previous setting of the dhcp result property */
    property_set(result_prop_name, "");

    if(supportOption60 && login == NULL)
        return -1;

    if(supportOption125 && option125Info == NULL)
        return -1;

     property_set(VERDORSPEC, "");
     property_set(USERNAME, "");
     property_set(PASSWD, "");
     property_set(VERDORID, "");
    /** password encrypt start **/
    //ALOGD("IPOE password is %s\n", password);
    char * passwdEncode = NULL;
    if(password != NULL) {
        passwdEncode = (char *)malloc(strlen(password) + 1);
        memset(passwdEncode, '\0', strlen(password) + 1);
        snprintf(passwdEncode, strlen(password) + 1, "%s", password);
        dhcp_passwdEncode(passwdEncode, password);//password encrypt, decrypt at external/dhcpcd/if-options.c dhcp_passwdDecode()
       // ALOGD("IPOE password is %s\n", passwdEncode);
    }
    /** password encrypt end**/
    /* Start the daemon and wait until it's ready */
    if (property_get(HOSTNAME_PROP_NAME, prop_value, NULL) && (prop_value[0] != '\0')) {
        if (NULL != strstr(interface, "."))
        {
            strncpy(prop_value, "android", PROPERTY_VALUE_MAX);
        }
        if(supportOption60 && supportOption125 && password != NULL) {//DHCP+ OPTION125
            snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:-h %s --username --password -j --require 125 -t %d %s", DAEMON_NAME,
                     p2p_interface, prop_value, dhcp_timeout, interface);
            property_set(VERDORSPEC, option125Info);
            property_set(USERNAME, login);
            //property_set(PASSWD, password);
            property_set(PASSWD, passwdEncode);
        } else if(supportOption60 && supportOption125 && password == NULL) {//OPTION60 OPTION125
            snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:-h %s -i -j --require 125 -t %d %s", DAEMON_NAME,
                     p2p_interface, prop_value, dhcp_timeout, interface);
            property_set(VERDORID, login);
            property_set(VERDORSPEC, option125Info);
        } else if (supportOption60 && !supportOption125 && password != NULL) {//DHCP+
            snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:-h %s --username --password -t %d %s", DAEMON_NAME,
                     p2p_interface, prop_value, dhcp_timeout, interface);
            property_set(USERNAME, login);
            //property_set(PASSWD, password);
            property_set(PASSWD, passwdEncode);
        } else if (supportOption60 && !supportOption125 && password == NULL) {//OPTION60
            snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:-h %s -i -t %d %s", DAEMON_NAME,
                     p2p_interface, prop_value, dhcp_timeout, interface);
             property_set(VERDORID, login);
        } else if (!supportOption60 && supportOption125) {//OPTION125
            snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:-h %s -j --require 125 -t %d %s", DAEMON_NAME,
                     p2p_interface, prop_value, dhcp_timeout, interface);
            property_set(VERDORSPEC, option125Info);
        } else {
            snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:-h %s -t %d %s", DAEMON_NAME,
                     p2p_interface, prop_value, dhcp_timeout, interface);
        }
    }
    else {
        if(supportOption60 && password != NULL && supportOption125) {//DHCP+ OPTION125
            snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:--username --password -j --require 125 -t %d %s", DAEMON_NAME,
                     p2p_interface, dhcp_timeout, interface);
            property_set(VERDORSPEC, option125Info);
            property_set(USERNAME, login);
            //property_set(PASSWD, password);
            property_set(PASSWD, passwdEncode);
        } else if (supportOption60 && password == NULL && supportOption125) {//OPTION60 OPTION125
            snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:-i -j  --require 125 -t %d %s", DAEMON_NAME,
                     p2p_interface, dhcp_timeout, interface);
            property_set(VERDORSPEC, option125Info);
            property_set(VERDORID, login);
        } else if (supportOption60 && password != NULL && !supportOption125) {//DHCP+
            snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:--username --password -t %d %s", DAEMON_NAME,
                     p2p_interface, dhcp_timeout, interface);
            property_set(USERNAME, login);
            //property_set(PASSWD, password);
            property_set(PASSWD, passwdEncode);
        } else if (supportOption60 && password == NULL && !supportOption125) {//OPTION60
            snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:-i -t %d %s", DAEMON_NAME,
                     p2p_interface, dhcp_timeout, interface);
            property_set(VERDORID, login);
        } else if (!supportOption60 && supportOption125) {//OPTION125
            snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:-j --require 125 -t %d %s", DAEMON_NAME,
                     p2p_interface, dhcp_timeout, interface);
            property_set(VERDORSPEC, option125Info);
        } else {
            snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:%s", DAEMON_NAME,
                     p2p_interface, interface);
        }
    }

    memset(prop_value, '\0', PROPERTY_VALUE_MAX);
    property_set(ctrl_prop, daemon_cmd);
    if (wait_for_property(daemon_prop_name, desired_status, 10) < 0) {
        snprintf(errmsg, sizeof(errmsg), "%s", "Timed out waiting for dhcpcd to start");
        return -1;
    }

    /* Wait for the daemon to return a result */
    if (wait_for_property(result_prop_name, NULL, dhcp_timeout) < 0) {
        snprintf(errmsg, sizeof(errmsg), "%s", "Timed out waiting for DHCP to finish");
        //dhcp_stop(interface);
        return -1;
    }

    if (!property_get(result_prop_name, prop_value, NULL)) {
        /* shouldn't ever happen, given the success of wait_for_property() */
        snprintf(errmsg, sizeof(errmsg), "%s", "DHCP result property was not set");
        //dhcp_stop(interface);
        return -1;
    }
    if (strcmp(prop_value, "ok") == 0) {
        char dns_prop_name[PROPERTY_KEY_MAX];
        if (fill_ip_info(interface, ipaddr, gateway, prefixLength, dns,
                server, lease, vendorInfo, domain, mtu) == -1) {
            //dhcp_stop(interface);
            return -1;
        }
        return 0;
    } else {
        snprintf(errmsg, sizeof(errmsg), "DHCP result was %s", prop_value);
        //dhcp_stop(interface);
        return -1;
    }
}

/**
 * Stop the DHCP client daemon.
 */
int dhcp_stop(const char *interface)
{
    char result_prop_name[PROPERTY_KEY_MAX];
    char daemon_prop_name[PROPERTY_KEY_MAX];
    char daemon_cmd[PROPERTY_VALUE_MAX * 2];
    const char *ctrl_prop = "ctl.stop";
    const char *desired_status = "stopped";

    property_set(VERDORSPEC, "");
    property_set(USERNAME, "");
    property_set(PASSWD, "");
    property_set(VERDORID, "");
    char p2p_interface[MAX_INTERFACE_LENGTH];

    get_p2p_interface_replacement(interface, p2p_interface);

    ALOGD("dhcp_stop '%s' in", interface);

    snprintf(result_prop_name, sizeof(result_prop_name), "%s.%s.result",
            DHCP_PROP_NAME_PREFIX,
            p2p_interface);
    if (NULL != strstr(interface, "."))
    {
        strncpy(p2p_interface, "vlan", MAX_INTERFACE_LENGTH);
    }

    snprintf(daemon_prop_name, sizeof(daemon_prop_name), "%s_%s",
            DAEMON_PROP_NAME,
            p2p_interface);

    snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s", DAEMON_NAME, p2p_interface);

    /* Stop the daemon and wait until it's reported to be stopped */
    property_set(ctrl_prop, daemon_cmd);
    if (wait_for_property(daemon_prop_name, desired_status, 5) < 0) {
        ALOGE("dhcp_stop '%s' time out", interface);
        return -1;
    }
    property_set(result_prop_name, "failed");
    ALOGD("dhcp_stop '%s' out", interface);

    return 0;
}

/**
 * Release the current DHCP client lease.
 */
int dhcp_release_lease(const char *interface)
{
    char daemon_prop_name[PROPERTY_KEY_MAX];
    char daemon_cmd[PROPERTY_VALUE_MAX * 2];
    const char *ctrl_prop = "ctl.stop";
    const char *desired_status = "stopped";

    char p2p_interface[MAX_INTERFACE_LENGTH];

    get_p2p_interface_replacement(interface, p2p_interface);

    if (NULL != strstr(interface, "."))
    {
        strncpy(p2p_interface, "vlan", MAX_INTERFACE_LENGTH);
    }

    snprintf(daemon_prop_name, sizeof(daemon_prop_name), "%s_%s",
            DAEMON_PROP_NAME,
            p2p_interface);

    snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s", DAEMON_NAME, p2p_interface);

    char prop_dhcp_pid_name[PROPERTY_KEY_MAX];
    char prop_dhcp_pid_value[PROPERTY_VALUE_MAX];
    snprintf(prop_dhcp_pid_name, sizeof(prop_dhcp_pid_name), "dhcp.%s.pid",
                interface);
    property_get(prop_dhcp_pid_name, prop_dhcp_pid_value, "0");
    if(!strcmp(prop_dhcp_pid_value, "0") || !strcmp(prop_dhcp_pid_value, "")) {
        return -1;
    }
    property_set(prop_dhcp_pid_name, "");
    ALOGD("dhcp_release_lease kill %d to release ip", atoi(prop_dhcp_pid_value));
    kill(atoi(prop_dhcp_pid_value), 1);
    usleep(100000);

    /* Stop the daemon and wait until it's reported to be stopped */
    property_set(ctrl_prop, daemon_cmd);
    if (wait_for_property(daemon_prop_name, desired_status, 5) < 0) {
        return -1;
    }
    return 0;
}

char *dhcp_get_errmsg() {
    return errmsg;
}

/**
 * The device init.rc file needs a corresponding entry.
 *
 * Example:
 * service iprenew_<interface> /system/bin/dhcpcd -n
 *
 */
int dhcp_do_request_renew(const char *interface,
                    char *ipaddr,
                    char *gateway,
                    uint32_t *prefixLength,
                    char *dns[],
                    char *server,
                    uint32_t *lease,
                    char *vendorInfo,
                    char *domain,
                    char *mtu)
{
    char result_prop_name[PROPERTY_KEY_MAX];
    char prop_value[PROPERTY_VALUE_MAX] = {'\0'};
    char daemon_cmd[PROPERTY_VALUE_MAX * 2];
    const char *ctrl_prop = "ctl.start";

    char p2p_interface[MAX_INTERFACE_LENGTH];

    get_p2p_interface_replacement(interface, p2p_interface);


    snprintf(result_prop_name, sizeof(result_prop_name), "%s.%s.result",
            DHCP_PROP_NAME_PREFIX,
            p2p_interface);

    if (NULL != strstr(interface, "."))
    {
        strncpy(p2p_interface, "vlan", MAX_INTERFACE_LENGTH);
    }
    /* Erase any previous setting of the dhcp result property */
    property_set(result_prop_name, "");

    /* Start the renew daemon and wait until it's ready */
    snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:%s", DAEMON_NAME_RENEW,
            p2p_interface, interface);
    memset(prop_value, '\0', PROPERTY_VALUE_MAX);
    property_set(ctrl_prop, daemon_cmd);

    /* Wait for the daemon to return a result */
    if (wait_for_property(result_prop_name, NULL, 30) < 0) {
        snprintf(errmsg, sizeof(errmsg), "%s", "Timed out waiting for DHCP Renew to finish");
        return -1;
    }

    if (!property_get(result_prop_name, prop_value, NULL)) {
        /* shouldn't ever happen, given the success of wait_for_property() */
        snprintf(errmsg, sizeof(errmsg), "%s", "DHCP Renew result property was not set");
        return -1;
    }
    if (strcmp(prop_value, "ok") == 0) {
        return fill_ip_info(interface, ipaddr, gateway, prefixLength, dns,
                server, lease, vendorInfo, domain, mtu);
    } else {
        snprintf(errmsg, sizeof(errmsg), "DHCP Renew result was %s", prop_value);
        return -1;
    }
}

int check_ip_conflict(const char *interface, const char *ipaddr)
{
	const char *ctl_start = "ctl.start";
	char prop_ipcflt_start[PROPERTY_VALUE_MAX];
	char prop_ipcflt_result[PROPERTY_KEY_MAX];
	char value[PROPERTY_VALUE_MAX];
	struct in_addr ip;
	char *ipstr;

	memset(value, 0, PROPERTY_VALUE_MAX);
	snprintf(prop_ipcflt_start, sizeof(prop_ipcflt_start),
			"init.svc.%s_%s:", "ipconflict", interface);
	if (!strcmp(value, "running")) {
        snprintf(errmsg, sizeof(errmsg), "%s", "ipconflict is running now");
        return -1;
    }

	memset(value, 0, PROPERTY_VALUE_MAX);
	snprintf(prop_ipcflt_result, sizeof(prop_ipcflt_result),
			"dhcp.%s.%s.result", "ipconflict", interface);
	property_set(prop_ipcflt_result, "");
	ipstr = (char *)ipaddr;
	if (*ipaddr == '/')
		ipstr = (char*)ipaddr + 1;
    snprintf(prop_ipcflt_start, sizeof(prop_ipcflt_start),
			"%s_%s:%s %s", "ipconflict",
            interface, interface, ipstr);
    property_set(ctl_start, prop_ipcflt_start);

    /* Wait for the daemon to return a result */
    if (wait_for_property(prop_ipcflt_result, NULL, 15) < 0) {
        snprintf(errmsg, sizeof(errmsg), "%s", "Timed out waiting for ipconflict finish");
        return -1;
    }

    if (!property_get(prop_ipcflt_result, value, NULL)) {
        /* shouldn't ever happen, given the success of wait_for_property() */
        snprintf(errmsg, sizeof(errmsg), "%s", "ipconflict result property was not set");
        return -1;
    }

	if (inet_pton(AF_INET, value, &ip))
		return 1;
	return 0;
}
