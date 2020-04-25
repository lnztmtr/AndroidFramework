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
#include <dirent.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <netutils/ifc.h>
#include <cutils/properties.h>

#include <android/log.h>
#define LOCAL_TAG "DHCPUTIL"
#define LOGD(...) ((void)__android_log_print(ANDROID_LOG_DEBUG, LOCAL_TAG, __VA_ARGS__))
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, LOCAL_TAG, __VA_ARGS__))
#define LOGW(...) ((void)__android_log_print(ANDROID_LOG_WARN, LOCAL_TAG, __VA_ARGS__))
#define LOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, LOCAL_TAG, __VA_ARGS__))

static const char *DHCP6V_DAEMON_PREFIX             = "init.svc";
static const char *DHCP6V_DAEMON_SRV_NAME           = "odhcp6c";

static const char *DHCP6V_RESULT_PROP_PREFIX        = "dhcp6c";

#ifdef VLAN_SUPPORT
static const char DAEMON_NAME[]        = "udhcpc";
static const char DAEMON_PROP_NAME[]   = "init.svc.udhcpc";
#else
static const char DAEMON_NAME[]        = "dhcpcd";
static const char DAEMON_PROP_NAME[]   = "init.svc.dhcpcd";
#endif
static const char HOSTNAME_PROP_NAME[] = "net.hostname";
static const char DHCP_PROP_NAME_PREFIX[]  = "dhcp";
static const char DHCP_CONFIG_PATH[]   = "/system/etc/dhcpcd/dhcpcd.conf";
static const int NAP_TIME = 10;   /* wait for 10ms at a time */
                                  /* when polling for property values */
static const char DUALSTACK_DHCP_CONFIG_PATH[]   = "/data/misc/etc/dhcpcd.conf";                         /* when polling for property values */
static const char DAEMON_NAME_RENEW[]  = "iprenew";
static char errmsg[100];
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
 * vlan interface names increase sequentially ethx.1, ethx.2.. after
 * group formation. This does not work well with system properties which can quickly
 * exhaust or for specifiying a dhcp start target in init which requires
 * interface to be pre-defined in init.rc file.
 *
 * This function returns a common string vlan for all vlan interfaces.
 */
void get_vlan_interface_replacement(const char *interface, char *vlan_interface) {
    /* Use vlan for any interface starting with eth0. */
    if (strncmp(interface, "eth0.",5) == 0 || strncmp(interface, "wlan0.",6) == 0) {
        strncpy(vlan_interface, "vlan", MAX_INTERFACE_LENGTH);
    } else {
        strncpy(vlan_interface, interface, MAX_INTERFACE_LENGTH);
    }
}

/*
 * Wait for a system property to be assigned a specified value.
 * If desired_value is NULL, then just wait for the property to
 * be created with any value. maxwait is the maximum amount of
 * time in Mseconds to wait before giving up.
 */
static int wait_for_property(const char *name, const char *desired_value, int maxwait)
{
    char value[PROPERTY_VALUE_MAX] = {'\0'};
    int maxnaps = (maxwait) / NAP_TIME;
    int ret;
    static char x = 0;
    if (maxnaps < 1) {
        maxnaps = 1;
    }
    x = 0;
    while (1) {
        ret = property_get(name, value, NULL);
        if (!((x++)&0x7)) LOGI("%d wait_for_property: %s=%s\n", x, name, value);
        if (ret) {
            if (desired_value == NULL || 
                    strcmp(value, desired_value) == 0) {
                return 0;
            }
        }
        if (maxnaps-- > 0)
            usleep(NAP_TIME * 1000);
        else
            break;
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

static int fill_ipv6_info(const char *interface,
                     char *ipaddr,
                     char *gateway,
                     uint32_t *prefixLength,
                     char *dns[],
                     char *server,
                     uint32_t *lease,
                     char *domain,
                     char *mtu)
{
    char prop_name[PROPERTY_KEY_MAX] = {0};
    char prop_value[PROPERTY_VALUE_MAX] = {0};
    /* Interface name after converting p2p0-p2p0-X to p2p to reuse system properties */
    // char p2p_interface[MAX_INTERFACE_LENGTH];
    int x;

    // get_p2p_interface_replacement(interface, p2p_interface);

    /*snprintf(prop_name, sizeof(prop_name), "%s.%s.domain", DHCP6V_PROP_NAME_PREFIX, interface);
    property_get(prop_name, domain, NULL);*/

    /*snprintf(prop_name, sizeof(prop_name), "%s.%s.mtu", DHCP6V_PROP_NAME_PREFIX,interface);
    property_get(prop_name, mtu, NULL);*/
    *prefixLength = 64;
    snprintf(prop_name, sizeof(prop_name), "%s.%s.ipaddress", DHCP6V_RESULT_PROP_PREFIX, interface);
    if (property_get(prop_name, ipaddr, NULL)) {
        char* slash = NULL, *dup_ipaddr = strdup(ipaddr);
        size_t ip_len = strlen(ipaddr);
        if (!dup_ipaddr) {
            LOGE("[fill_ipv6_info] strdup failed");
            return -1;
        }
        if ((slash = strrchr(dup_ipaddr, '/'))) {
            if ((slash-dup_ipaddr) > (int)ip_len) {
                LOGE("[fill_ipv6_info] unexpected.");
                return -1;
            }
            memset(ipaddr, 0, ip_len);
            strncpy(ipaddr, dup_ipaddr, slash-dup_ipaddr);
            slash += 1;
            *prefixLength = atoi(slash);

        }
    }

    snprintf(prop_name, sizeof(prop_name), "%s.%s.gateway", DHCP6V_RESULT_PROP_PREFIX, interface);
    property_get(prop_name, gateway, NULL);

    for (x=0; dns[x] != NULL; x++) {
        snprintf(prop_name, sizeof(prop_name), "%s.%s.dns%d", DHCP6V_RESULT_PROP_PREFIX, interface, x+1);
        property_get(prop_name, dns[x], NULL);
    }

    /*snprintf(prop_name, sizeof(prop_name), "%s.%s.leasetime", DHCP6V_PROP_NAME_PREFIX, interface);
    if (property_get(prop_name, prop_value, NULL)) {
        *lease = atol(prop_value);
    }*/
    *lease  = 0xffffffff;
    *server = 0;
    *domain = 0;
    *mtu    = 0;

    // snprintf(prop_name, sizeof(prop_name), "%s.%s.server", DHCP6V_PROP_NAME_PREFIX, interface);
    // property_get(prop_name, server, NULL);

    return 0;
}

static const char *ipaddr_to_string(in_addr_t addr)
{
    struct in_addr in_addr;

    in_addr.s_addr = addr;
    return inet_ntoa(in_addr);
}

static int is_dual_stack()
{
    char tmp[PROPERTY_VALUE_MAX] = {'\0'};
    property_get("net.dualstack", tmp, "false");
    LOGW("%s: %s\n", __FUNCTION__, tmp);
    return (0 != strcmp(tmp, "false"));
}

static int getTimeout()
{
    char tmp[PROPERTY_VALUE_MAX] = {'\0'};
    property_get("net.dualstack.timeout", tmp, "30");
    LOGW("%s: %s\n", __FUNCTION__, tmp);
    return atoi(tmp);
}

static int getDhcpTimeout()
{
    char tmp[PROPERTY_VALUE_MAX] = {'\0'};
    property_get("net.dhcp.timeout", tmp, "30");
    LOGW("%s: %s\n", __FUNCTION__, tmp);
    return atoi(tmp);
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
                    char *mtu)
{
    char result_prop_name[PROPERTY_KEY_MAX];
    char daemon_prop_name[PROPERTY_KEY_MAX];
    char prop_value[PROPERTY_VALUE_MAX] = {'\0'};
    char daemon_cmd[PROPERTY_VALUE_MAX * 2 + sizeof(DHCP_CONFIG_PATH)];
    const char *ctrl_prop = "ctl.start";
    const char *desired_status = "running";
    /* Interface name after converting p2p0-p2p0-X to p2p to reuse system properties */
    char p2p_interface[MAX_INTERFACE_LENGTH];
    int timeout = 30;
#ifdef VLAN_SUPPORT
    char *service_name_eth = "dhcp_restart_e";
    char *service_name_wlan = "dhcp_restart_w";
    char *service_name = "unknowndhcpstart";
#endif

    if (0 == strncmp(interface, "eth0.", strlen("eth0.")) ||
            0 == strncmp(p2p_interface, "wlan0.", strlen("wlan0."))) {
        get_vlan_interface_replacement(interface, p2p_interface);
        snprintf(result_prop_name, sizeof(result_prop_name), "%s.%s.result",
                DHCP_PROP_NAME_PREFIX,
                interface);
    } else {
        get_p2p_interface_replacement(interface, p2p_interface);
        snprintf(result_prop_name, sizeof(result_prop_name), "%s.%s.result",
                DHCP_PROP_NAME_PREFIX,
                p2p_interface);
    }

#ifdef VLAN_SUPPORT
    if (0 == strncmp(p2p_interface, "eth", strlen("eth"))) {
        service_name = service_name_eth;
    }
    else if (0 == strncmp(p2p_interface, "wlan", strlen("wlan"))) {
        service_name = service_name_wlan;
    }

    snprintf(daemon_prop_name, sizeof(daemon_prop_name), "init.svc.%s", service_name);
    LOGI("daemon_prop: %s\n", daemon_prop_name);
#else
    snprintf(daemon_prop_name, sizeof(daemon_prop_name), "%s_%s",
            DAEMON_PROP_NAME,
            p2p_interface);
    LOGE("dhcp_do_request: daemon_prop: %s\n", daemon_prop_name);
#endif

    /* Erase any previous setting of the dhcp result property */
    property_set(result_prop_name, "");

    /* Start the daemon and wait until it's ready */
#ifdef VLAN_SUPPORT
    LOGI("dhcp_do_request: start service %s\n", service_name);
    snprintf(daemon_cmd, sizeof(daemon_cmd), "%s", service_name);
#else
    if (is_dual_stack()) {
        timeout = getTimeout();
        if (property_get(HOSTNAME_PROP_NAME, prop_value, NULL) && (prop_value[0] != '\0'))
            snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:-f %s -h %s %s", DAEMON_NAME,
                     p2p_interface, DUALSTACK_DHCP_CONFIG_PATH, prop_value, interface);
        else
            snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:-f %s %s", DAEMON_NAME,
                     p2p_interface, DUALSTACK_DHCP_CONFIG_PATH, interface);
        LOGI("daemon_cmd: %s\n", daemon_cmd);
    } else {
        timeout = getDhcpTimeout();
        if (property_get(HOSTNAME_PROP_NAME, prop_value, NULL) && (prop_value[0] != '\0'))
            snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:-f %s -h %s %s", DAEMON_NAME,
                     p2p_interface, DHCP_CONFIG_PATH, prop_value, interface);
        else
            snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:-f %s %s", DAEMON_NAME,
                     p2p_interface, DHCP_CONFIG_PATH, interface);
    }
#endif

    memset(prop_value, '\0', PROPERTY_VALUE_MAX);
    LOGE("dhcp_do_request: start service %s\n", daemon_cmd);

#ifdef VLAN_SUPPORT
    int count = 3;
#else
    int count = 1;
#endif
    int i = 0;
    for(i=0; i<count; i++) {
        property_set(ctrl_prop, daemon_cmd);
        if (wait_for_property(daemon_prop_name, desired_status, 10*1000) < 0) {
            if(i<(count - 1)) {
                LOGW("Timed out waiting for dhcpcd to start, try %d", i);
            }
            else {
                snprintf(errmsg, sizeof(errmsg), "%s", "Timed out waiting for dhcpcd to start");
                return -1;
            }
        }
        else {
            break;
        }
    }
    LOGI("dhcp_do_request: wait_for_property: %s: desired_status:NULL>>>", result_prop_name);
    /* Wait for the daemon to return a result */
#ifdef DHCP_RESULT
    if (wait_for_property(result_prop_name, NULL, 3600*24*7*1000) < 0)
    /*add add by zhaolianghua for jiangxi dhcp time @20190117*/
#elif PROVINCE_TYPE_CM201_JX
    if (wait_for_property(result_prop_name, NULL, 60*1000) < 0)
    /*add end*/
#else
    if (wait_for_property(result_prop_name, NULL, timeout*1000) < 0)
#endif
    {
        snprintf(errmsg, sizeof(errmsg), "%s", "Timed out waiting for DHCP to finish");
        return -1;
    }
    LOGI("dhcp_do_request: wait_for_property<<<");

    if (!property_get(result_prop_name, prop_value, NULL)) {
        /* shouldn't ever happen, given the success of wait_for_property() */
        snprintf(errmsg, sizeof(errmsg), "%s", "DHCP result property was not set");
        return -1;
    }
    if (strcmp(prop_value, "ok") == 0) {
        if (fill_ip_info(interface, ipaddr, gateway, prefixLength, dns,
                server, lease, vendorInfo, domain, mtu) == -1) {
            return -1;
        }
#ifdef MAC_VLAN_SUPPORT
        char prop_value1[PROPERTY_VALUE_MAX] = {'\0'};
        property_get("persist.sys.doublenet", prop_value1, NULL);
        if ((0 == strcmp(prop_value1, "true")) 
                && (0 == strncmp(interface, "eth", strlen("eth")))) {
            LOGI("start dhcpcd_peth0\n");
            property_set("ctl.start", "dhcpcd_peth0");
        }
#endif
        return 0;
    } else {
        snprintf(errmsg, sizeof(errmsg), "DHCP result was %s", prop_value);
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
#ifdef VLAN_SUPPORT
    char daemon_prop1_name[PROPERTY_KEY_MAX];
    char *service_name_eth = "dhcp_stop_e";
    char *service1_name_eth = "dhcp_restart_e";
    char *service_name_wlan = "dhcp_stop_w";
    char *service1_name_wlan = "dhcp_restart_w";
    char *service_name = "unknowndhcpstop";
    char *service1_name = "unknowndhcpstop";
#endif

    char p2p_interface[MAX_INTERFACE_LENGTH];

    if (0 == strncmp(interface, "eth0.", strlen("eth0.")) ||
            0 == strncmp(p2p_interface, "wlan0.", strlen("wlan0.")))
        get_vlan_interface_replacement(interface, p2p_interface);
    else
        get_p2p_interface_replacement(interface, p2p_interface);

#ifdef VLAN_SUPPORT
    if (0 == strncmp(p2p_interface, "eth", strlen("eth"))) {
        service_name = service_name_eth;
        service1_name = service1_name_eth;
    }
    else if (0 == strncmp(p2p_interface, "wlan", strlen("wlan"))) {
        service_name = service_name_wlan;
        service1_name = service1_name_wlan;
    }
    
    LOGI("dhcp_stop: start service %s\n", service_name);
    property_set("ctl.start", service_name);
    snprintf(daemon_prop_name, sizeof(daemon_prop_name), "init.svc.%s", service_name);
    LOGI("dhcp_stop: check prop %s\n", daemon_prop_name);
    if (wait_for_property(daemon_prop_name, desired_status, 5*1000) < 0) {
        LOGW("Timed out waiting for stop DHCP to finish");
        return -1;
    }
    snprintf(daemon_prop1_name, sizeof(daemon_prop1_name), "init.svc.%s", service1_name);
    LOGI("dhcp_stop: check prop1 %s\n", daemon_prop1_name);
    if (wait_for_property(daemon_prop1_name, desired_status, 5*1000) < 0) {
        LOGW("Timed out waiting for start DHCP to finish");
        return -1;
    }
    LOGI("dhcp_stop: start service end!");
    //usleep(1000000);
    return 0;
#endif

    if (0 == strncmp(interface, "eth0.", strlen("eth0.")) ||
            0 == strncmp(p2p_interface, "wlan0.", strlen("wlan0.")))
        snprintf(result_prop_name, sizeof(result_prop_name), "%s.%s.result",
                DHCP_PROP_NAME_PREFIX,
                interface);
    else
        snprintf(result_prop_name, sizeof(result_prop_name), "%s.%s.result",
                DHCP_PROP_NAME_PREFIX,
                p2p_interface);

    snprintf(daemon_prop_name, sizeof(daemon_prop_name), "%s_%s",
            DAEMON_PROP_NAME,
            p2p_interface);

    snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s", DAEMON_NAME, p2p_interface);

    LOGI("dhcp_stop on interface: %s. result_prop_name=%s daemon_cmd=%s",
            interface, result_prop_name, daemon_cmd);

#ifdef MAC_VLAN_SUPPORT
    char prop_value1[PROPERTY_VALUE_MAX] = {'\0'};
    property_get("persist.sys.doublenet", prop_value1, NULL);
    if ((0 == strcmp(prop_value1, "true"))
            && (0 == strncmp(interface, "eth", strlen("eth")))) {
        LOGI("stop dhcpcd_peth0\n");
        property_set("ctl.stop", "dhcpcd_peth0");
    }
    ifc_reset_connections("peth0", RESET_IPV4_ADDRESSES | RESET_IPV6_ADDRESSES);
#endif

    /* Stop the daemon and wait until it's reported to be stopped */
    LOGI("dhcp_stop: wait_for_property: %s: desired_status:%s>>>", daemon_prop_name, desired_status);
    property_set(ctrl_prop, daemon_cmd);
    if (wait_for_property(daemon_prop_name, desired_status, 200) < 0) {
        property_set(result_prop_name, "stopped");
        return -1;
    }
    LOGI("dhcp_stop: wait_for_property<<<");
    property_set(result_prop_name, "stopped");
    return 0;
}

/**
 * Release the current DHCP client lease.
 */
int dhcp_release_lease(const char *interface)
{
    char value[PROPERTY_VALUE_MAX];
    char daemon_cmd[PROPERTY_VALUE_MAX * 2];
    char daemon_prop_name[PROPERTY_KEY_MAX];
    char result_prop_name[PROPERTY_KEY_MAX];
    const char *ctrl_start_prop = "ctl.start";
    const char *ctrl_stop_prop = "ctl.stop";
    const char *desired_released_status = "released";
    const char *desired_stopped_status = "stopped";
    char p2p_interface[MAX_INTERFACE_LENGTH];

    if (0 == strncmp(interface, "eth0.", strlen("eth0.")) ||
            0 == strncmp(p2p_interface, "wlan0.", strlen("wlan0.")))
        get_vlan_interface_replacement(interface, p2p_interface);
    else
        get_p2p_interface_replacement(interface, p2p_interface);

    property_get("net.dhcp.release", value, NULL);
    if(strcmp(value, "true") == 0) {
        snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_r_%s", DAEMON_NAME, p2p_interface);
        if (0 == strncmp(interface, "eth0.", strlen("eth0.")) ||
                0 == strncmp(p2p_interface, "wlan0.", strlen("wlan0.")))
            snprintf(result_prop_name, sizeof(result_prop_name), "dhcp.%s.result",interface);
        else
            snprintf(result_prop_name, sizeof(result_prop_name), "dhcp.%s.result",p2p_interface);
        memset(value, 0, PROPERTY_VALUE_MAX);
        property_get(result_prop_name, value, "");
        if (strcmp(value, "ok") == 0) {
            property_set(ctrl_start_prop, daemon_cmd);
            if (wait_for_property(result_prop_name, desired_released_status, 1*1000) < 0) {
                return -1;
            }
        }
    } else {
        snprintf(daemon_prop_name, sizeof(daemon_prop_name), "%s_%s",
                DAEMON_PROP_NAME,
                p2p_interface);

        snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s", DAEMON_NAME, p2p_interface);

        /* Stop the daemon and wait until it's reported to be stopped */
        property_set(ctrl_stop_prop, daemon_cmd);
        if (wait_for_property(daemon_prop_name, desired_stopped_status, 5*1000) < 0) {
            return -1;
        }
    }
    return 0;
}

int dhcpv6_release_lease(const char *interface)
{
    char value[PROPERTY_VALUE_MAX];
    char daemon_cmd[PROPERTY_VALUE_MAX * 2];
    char daemon_prop_name[PROPERTY_KEY_MAX];
    char result_prop_name[PROPERTY_KEY_MAX];
    const char *ctrl_start_prop = "ctl.start";
    const char *ctrl_stop_prop = "ctl.stop";
    const char *desired_released_status = "released";
    const char *desired_stopped_status = "stopped";
    char p2p_interface[MAX_INTERFACE_LENGTH];

    property_get("net.dhcp.release", value, NULL);
    if(strcmp(value, "true") == 0) {
        snprintf(daemon_cmd, sizeof(daemon_cmd), "dhcpcdv6_r");
		snprintf(result_prop_name, sizeof(result_prop_name), "dhcp6c.%s.result",interface);

        memset(value, 0, PROPERTY_VALUE_MAX);
        property_get(result_prop_name, value, "");

		LOGI("dhcpv6_release_lease: %s : %s", result_prop_name, value);		

		if (strcasecmp(value, "ok") == 0) {
			LOGI("property_set: %s : %s", ctrl_start_prop, daemon_cmd);	
            property_set(ctrl_start_prop, daemon_cmd);
            if (wait_for_property(result_prop_name, desired_released_status, 1*1000) < 0) {
				LOGI("wait_for_property: %s : %s  -> return", result_prop_name, desired_released_status);	
                return -1;
            }
        }
    }
	LOGI("dhcpv6_release_lease return -");	
    return 0;
}

char *dhcp_get_errmsg() {
    return errmsg;
}

const char *change_ipaddr(in_addr_t addr)
{
    struct in_addr in_addr;

    in_addr.s_addr = addr;
    return inet_ntoa(in_addr);
}

int dump_interface(const char *name, char *ipaddr)
{
    unsigned addr, flags;
    int prefixLength;

    if(ifc_get_info(name, &addr, &prefixLength, &flags)) {
        return -1;
    }
    sprintf(ipaddr, "%s", change_ipaddr(addr));
    LOGI("dump_interface: name=%s, addr=%d, ipaddr=%s\n", name, addr, ipaddr);
    if(addr <= 0)
        return -1;
    return 0;
}

int get_BPlane_IpAddr(char *ipaddr) 
{
    if(ifc_init()) {
        LOGW("get_BPlane_IpAddr: ifc init failed!\n");
        return -1;
    }

    DIR *d;
    struct dirent *de;

    d = opendir("/sys/class/net");
    if(d == 0) {
        LOGW("get_BPlane_IpAddr: open dir(/sys/class/net) failed!\n");
        return -1;
    }

    int fail = 1;
    while((de = readdir(d))) {
        if(de->d_name[0] == '.') continue;
        if((strncmp(de->d_name, "eth0.", strlen("eth0.")) == 0) 
                || (strncmp(de->d_name, "wlan0.", strlen("wlan0.")) == 0)) {
            if(dump_interface(de->d_name, ipaddr) == 0) {
                LOGI("get_BPlane_IpAddr: name=%s, ipaddr=%s\n", de->d_name, ipaddr);
                fail = 0;
                break;
            }
        }
    }
    closedir(d);
    ifc_close();
    return (fail == 1) ? -1 : 0;
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
    if (0 == strncmp(interface, "eth0.", strlen("eth0.")) ||
            0 == strncmp(p2p_interface, "wlan0.", strlen("wlan0."))) {
        get_vlan_interface_replacement(interface, p2p_interface);
        snprintf(result_prop_name, sizeof(result_prop_name), "%s.%s.result",
                DHCP_PROP_NAME_PREFIX,
                interface);
    } else {
        get_p2p_interface_replacement(interface, p2p_interface);
        snprintf(result_prop_name, sizeof(result_prop_name), "%s.%s.result",
                DHCP_PROP_NAME_PREFIX,
                p2p_interface);
    }

    /* Erase any previous setting of the dhcp result property */
    property_set(result_prop_name, "");

    /* Start the renew daemon and wait until it's ready */
    snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:%s", DAEMON_NAME_RENEW,
            p2p_interface, interface);
    memset(prop_value, '\0', PROPERTY_VALUE_MAX);
    property_set(ctrl_prop, daemon_cmd);

    /* Wait for the daemon to return a result */
    if (wait_for_property(result_prop_name, NULL, 30*1000) < 0) {
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


/**
 * The device init.rc file needs a corresponding entry.
 *
 * Example:
 * service arping /system/bin/arping -c 3 -f -D -I
 * return: -1:arping error
 *			0:ip addr conflict
 *			1:ip addr unconfilct
 */
static const char ARPING_PROP_NAME_PREFIX[]  = "arping";
int arping_check_addressconflict(const char *interface,
                    const char *ipaddr)
{
    char result_prop_name[PROPERTY_KEY_MAX];
    char prop_value[PROPERTY_VALUE_MAX] = {'\0'};
    char daemon_cmd[PROPERTY_VALUE_MAX * 2];
    char desired_status[PROPERTY_VALUE_MAX]="running";
    char daemon_prop_name[PROPERTY_VALUE_MAX]="init.svc.arping";
    const char *ctrl_prop = "ctl.start";
    int retry_cnt = 3;
    char p2p_interface[MAX_INTERFACE_LENGTH];

    get_p2p_interface_replacement(interface, p2p_interface);

    snprintf(result_prop_name, sizeof(result_prop_name), "net.%s.result",
            ARPING_PROP_NAME_PREFIX);
    /* Erase any previous setting of the dhcp result property */
    property_set(result_prop_name, "");
    snprintf(daemon_cmd, sizeof(daemon_cmd), "%s:%s %s", "arping",
            p2p_interface,ipaddr );
    LOGD("arping_check_addressconflict:start  %s", daemon_cmd);
    memset(prop_value, '\0', PROPERTY_VALUE_MAX);

    /* Start the arping daemon and wait until it's ready */
    while(--retry_cnt){
        property_set(ctrl_prop, daemon_cmd);
        if (wait_for_property(daemon_prop_name, desired_status, 5*1000) < 0) {
            LOGD("Time out waiting for arping to start,retrying...");
            if(retry_cnt == 0)
                return -1;
        }else {
            break;
        }
    }

    /* Wait for the daemon to return a result */
    if (wait_for_property(result_prop_name, NULL, 10*1000) < 0) {
        snprintf(errmsg, sizeof(errmsg), "%s", "Timed out waiting for arping to finish");
        LOGD("Timed out waiting for arping to finish");
        return -1;
    }

    if (!property_get(result_prop_name, prop_value, NULL)) {
        /* shouldn't ever happen, given the success of wait_for_property() */
        snprintf(errmsg, sizeof(errmsg), "%s", "arping result property was not set");
	    LOGD("arping result property was not set");
        return -1;
    }
    if(strcmp(prop_value,"conflict") == 0){
        LOGD("return arping result property :confict");
        return 0;
    }else{
        LOGD("return arping result property :unconfilct");
        return 1;
    }
}

char* replacement_interface(char* iface) {
    char p2p_interface[MAX_INTERFACE_LENGTH] = {0};
    char vlan_interface[MAX_INTERFACE_LENGTH] = {0};

    get_p2p_interface_replacement(iface, p2p_interface);

    get_vlan_interface_replacement(p2p_interface, vlan_interface);

    /* if (strncmp(vlan_interface, "eth", 3) == 0) {
        return strdup("start");
    } else {
        return strdup(vlan_interface);
    }*/
    return strdup(vlan_interface);
}

/**
 * Stop the odhcp6c client daemon.
 */
int dhcpv6_stop(const char *interface)
{
    //char result_prop_name[PROPERTY_KEY_MAX] = {0};
    char daemon_prop_full_name[PROPERTY_KEY_MAX] = {0};
    char daemon_cmd[PROPERTY_VALUE_MAX * 2] = {0};
    const char *ctrl_prop                 = "ctl.stop";
    const char *desired_status            = "stopped";

    char* suffix = replacement_interface((char*)interface);

    /*snprintf(result_prop_name, sizeof(result_prop_name), "%s.%s.result",
            DHCP_PROP_NAME_PREFIX,
            p2p_interface);*/

    snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s", DHCP6V_DAEMON_SRV_NAME,suffix);

    LOGI("dhcpv6_stop(%s). daemon_cmd=%s", interface, daemon_cmd);
    property_set(ctrl_prop, daemon_cmd);

    snprintf(daemon_prop_full_name, sizeof(daemon_prop_full_name), "%s.%s_%s",
            DHCP6V_DAEMON_PREFIX, DHCP6V_DAEMON_SRV_NAME, suffix);
    free(suffix);
    /* Stop the daemon and wait until it's reported to be stopped */
    LOGI("dhcpv6_stop: wait_for_property(%s), desired_status:%s", daemon_prop_full_name, desired_status);
    if (wait_for_property(daemon_prop_full_name, desired_status, 200) < 0) {
        LOGW("dhcpv6_stop [ Unexpected ] <<<");
        return -1;
    }
    LOGI("dhcpv6_stop [expected] <<<");
    //property_set(result_prop_name, "stopped");
    return 0;
}

int dhcpv6_do_request(int timeout,
                    const char *interface,
                    char *ipaddr,
                    char *gateway,
                    uint32_t *prefixLength,
                    char *dns[],
                    char *server,
                    uint32_t *lease,
                    char *domain,
                    char *mtu)
{
    char ipaddr_prop_name[PROPERTY_KEY_MAX] = {0};
    char result_prop_name[PROPERTY_KEY_MAX] = {0};
    char daemon_prop_name[PROPERTY_KEY_MAX] = {0};
    char prop_value[PROPERTY_VALUE_MAX] = {0};
    char ipaddr_prop_value[PROPERTY_VALUE_MAX] = {0};
    char daemon_cmd[PROPERTY_VALUE_MAX * 2] = {0};
    const char *ctrl_prop = "ctl.start";

    /* Interface name after converting p2p0-p2p0-X to p2p to reuse system properties */
    char p2p_interface[MAX_INTERFACE_LENGTH];
    char vlan_interface[MAX_INTERFACE_LENGTH];

    char* suffix = replacement_interface((char*)interface);

    snprintf(result_prop_name, sizeof(result_prop_name), "%s.%s.result",
            DHCP6V_RESULT_PROP_PREFIX,
            interface);
    snprintf(ipaddr_prop_name, sizeof(ipaddr_prop_name), "%s.%s.ipaddress",
            DHCP6V_RESULT_PROP_PREFIX,
            interface);

    snprintf(daemon_prop_name, sizeof(daemon_prop_name), "%s.%s_%s",
            DHCP6V_DAEMON_PREFIX,DHCP6V_DAEMON_SRV_NAME,suffix);
    LOGE("dhcpv6_do_request: daemon_prop: %s\n", daemon_prop_name);

    snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:%s ",
            DHCP6V_DAEMON_SRV_NAME,suffix, interface);

    /* Erase any previous setting of the dhcp result property */
    property_set(result_prop_name, "");

    /* Start the daemon and wait until it's ready */

    LOGE("dhcpv6_do_request: start service %s\n", daemon_cmd);

    int count = 1;

    int i = 0;
    for (i=0; i<count; i++) {
        property_set(ctrl_prop, daemon_cmd);
        if (wait_for_property(daemon_prop_name, "running", 1000) < 0) {
            if (i<(count - 1)) {
                LOGW("Timed out waiting for odhcp6c to start, try %d", i);
            }
            else {
                snprintf(errmsg, sizeof(errmsg), "%s", "Timed out waiting for odhcp6c to start");
                return -1;
            }
        }
        else {
            break;
        }
    }
    LOGI("dhcpv6_do_request: wait_for_property >>> %s", result_prop_name);
    int times = timeout*1000/NAP_TIME;
    for (;times;times--) {
        /* Wait for the daemon to return a result */
        if (0 == wait_for_property(result_prop_name, "OK", NAP_TIME) ||
            0 != property_get(ipaddr_prop_name, ipaddr_prop_value, ""))
        {
            break;
        }
    }
    if (0 == times) {
        snprintf(errmsg, sizeof(errmsg), "%s", "Timed out waiting for odhcp6c to finish");
        return -1;
    }
    LOGI("dhcpv6_do_request: wait_for_property <<<");

    //if (!property_get(result_prop_name, prop_value, NULL)) {
        /* shouldn't ever happen, given the success of wait_for_property() */
        /*
        snprintf(errmsg, sizeof(errmsg), "%s", "DHCP result property was not set");
        return -1;
    }
    */
    free(suffix);
    if (fill_ipv6_info(interface, ipaddr, gateway, prefixLength, dns,
            server, lease, domain, mtu) == -1) {
        return -1;
    }
    return 0;
    /*
    if (strcmp(prop_value, "OK") == 0) {

        if (fill_ipv6_info(interface, ipaddr, gateway, prefixLength, dns,
                server, lease, domain, mtu) == -1) {
            return -1;
        }
        return 0;
    } else {
        snprintf(errmsg, sizeof(errmsg), "DHCP result was %s", prop_value);
        return -1;
    }
    */
}

