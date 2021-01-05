#include <stdio.h>
#include <stdbool.h>

#include <sys/types.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <net/if.h>
#include <netdb.h>
#include <linux/if.h>
#include <linux/if_ether.h>
#include <linux/if_arp.h>
#include <linux/netlink.h>
#include <linux/if.h>
#include <linux/route.h>
#include <linux/ipv6_route.h>
#include <cutils/properties.h>
#define LOG_TAG "DHCPV6"
#include <cutils/log.h>

#define DAEMON_LEASES_PID                         "-lf /data/misc/dhcp/dhclient6_%s.leases -D LL" //need space on start
#define MAX_INTERFACE_LENGTH 25

static const char DAEMON_NAME[]                 = "dhclient";
static const char DAEMON_NAME_DNS[]             = "dhclientDns";
static const char DAEMON_PROP_NAME[]            = "init.svc.dhclient";
static const char DAEMON_DNS_PROP_NAME[]        = "init.svc.dhclientDns";
static const char DAEMON_NAME_RELEASE[]         = "release";
static const char VLAN_LEASE_PATH[]             = "-lf /data/misc/dhcp/dhclient6_vlan.leases -D LL";
static const int NAP_TIME = 200;   /* wait for 200ms at a time */
static char errmsg[100];
static const char VERDORSPEC[] = "dhclient.vendorspecinfo";
static const char VENDORID[] = "dhclient.vendorclassid";
static const char USERNAME[] = "dhclient.username";
static const char PASSWD[] = "dhclient.password";

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

//not equal retun 0; else if equal return -1
static int wait_for_property_reverse(const char *name, const char *desired_value, int maxwait)
{
    char value[PROPERTY_VALUE_MAX] = {'\0'};
    int maxnaps = (maxwait * 1000) / NAP_TIME;

    if (maxnaps < 1) {
        maxnaps = 1;
    }

    while (maxnaps-- > 0) {
        usleep(NAP_TIME * 1000);
        if (property_get(name, value, NULL)) {
            ALOGD("--------------------dhcpv6name is: %s, value is:%s, desired_value is:%s", name, value, desired_value );
            if (desired_value == NULL ||
                    strcmp(value, desired_value) != 0) {
                return 0;
            }
        }
    }
    return -1; /* failure */
}

/* HISILICON START */
void dhcpv6_release(const char *interface)
{
    char iface[MAX_INTERFACE_LENGTH] = {'\0'};
    char start_cmd_result_prop[PROPERTY_VALUE_MAX];
    char result_prop[PROPERTY_VALUE_MAX];
    char daemon_cmd[PROPERTY_VALUE_MAX];
    char prop_value[PROPERTY_VALUE_MAX] = {'\0'};
    const char *ctrl_prop = "ctl.start";

    ALOGD("dhcpv6_release start");
    strncpy(iface, interface, MAX_INTERFACE_LENGTH);
    if (strstr(interface, ".") != NULL) { // vlan
        strncpy(iface, "vlan", MAX_INTERFACE_LENGTH);
    }
    snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:"DAEMON_LEASES_PID, DAEMON_NAME_RELEASE, iface, interface);

    /*
     * the result prop is the same with dhcpv6_do_request(), so dhcpv6_release && dhcpv6_do_request
     * can't be excuted at the same time.
    */
    snprintf(result_prop, sizeof(result_prop), "%s.%s.result", DAEMON_NAME, interface);
    property_set(result_prop, "");

    ALOGD("dhcpv6_release cmd='%s'", daemon_cmd);
    property_set(ctrl_prop, daemon_cmd);

    snprintf(start_cmd_result_prop, sizeof(start_cmd_result_prop), "init.svc.%s_%s", DAEMON_NAME_RELEASE, iface);
    if (wait_for_property(start_cmd_result_prop, NULL, 10) < 0) {
        snprintf(errmsg, sizeof(errmsg), "%s", "dhcpv6_release waiting for release to start timeout");
        ALOGD("%s", errmsg);
        return;
    }
    if (!property_get(start_cmd_result_prop, prop_value, NULL)) {
        snprintf(errmsg, sizeof(errmsg), "%s", "dhcpv6_release release start result not set");
        ALOGD("%s", errmsg);
        return;
    }

    ALOGD("dhcpv6_release start waiting for release result...");
    /* Wait for the daemon to return a result */
    if (wait_for_property(result_prop, NULL, 3) < 0) {
        snprintf(errmsg, sizeof(errmsg), "%s", "dhcpv6_release wait for release to finish timeout");
        ALOGD("%s", errmsg);
        return;
    }
    if (!property_get(result_prop, prop_value, NULL)) {
        /* shouldn't ever happen, given the success of wait_for_property() */
        snprintf(errmsg, sizeof(errmsg), "%s", "dhcpv6_release release result prop not set");
        ALOGD("%s", errmsg);
        return;
    }
    property_set(result_prop, "");
    ALOGD("dhcpv6_release return %s", prop_value);

    return;
}
/* HISILICON END */

/**
 * Start the DHCPv6 client daemon.
 */
//int dhcpv6_do_request(const char *interface)
int dhcpv6_do_request(const char *interface,
                    bool supportOption60,
                    const char *login,
                    const char *password,
                    bool supportOption125,
                    const char *option125Info)
{
    char iface[MAX_INTERFACE_LENGTH] = {'\0'};
    char result_prop_name[PROPERTY_VALUE_MAX];
    char daemon_cmd[PROPERTY_VALUE_MAX];
    char prop_value[PROPERTY_VALUE_MAX] = {'\0'};
    const char *ctrl_prop = "ctl.start";
    const char *desired_status = "running";
    char ipaddress6[256] = {'\0'};
    ALOGD("-----------------dhcpv6_do_request");
    strncpy(iface, interface, MAX_INTERFACE_LENGTH);
    if (strstr(interface, ".") != NULL) { // vlan
        strncpy(iface, "vlan", MAX_INTERFACE_LENGTH);
    }

    memset(daemon_cmd, '\0', PROPERTY_VALUE_MAX);
    snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:%s "DAEMON_LEASES_PID, DAEMON_NAME, iface, interface, iface);
    ALOGD("-------------------------------------->dhclient cmd=%s",daemon_cmd);
/***HISI start***///dhcpv6+
    property_set(VERDORSPEC, "");
    property_set(USERNAME, "");
    property_set(PASSWD, "");
    property_set(VENDORID, "");
    ALOGD("-------------------------------------->op16=%d,op17=%d", supportOption60, supportOption125);
    char * passwdEncode = NULL;
    if(password != NULL){
        passwdEncode = (char *)malloc(strlen(password) + 1);
        memset(passwdEncode, '\0', strlen(password) + 1);
        snprintf(passwdEncode, strlen(password) + 1, "%s", password);
        dhcp_passwdEncode(passwdEncode, password); //password encrypt, decrypt at device/hisilicon/bigfish/external/dhcp/client/dhc6.c dhcp_passwdDecode()
        // ALOGD("IPOE password is %s\n", passwdEncode);
    }

    if(supportOption60 && supportOption125 && password != NULL) {//option60 option125
        snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:-op16 des3 -op17 %s "DAEMON_LEASES_PID, DAEMON_NAME, iface, interface, iface);
        ALOGD("-------------------------------------->dhcpv6+ cmd=%s",daemon_cmd);
        property_set(VERDORSPEC, option125Info);
        property_set(USERNAME, login);
        property_set(PASSWD, passwdEncode);
    } else if (supportOption60 && supportOption125 && password == NULL) {//OPTION60 option125
        snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:-op16 id -op17 %s "DAEMON_LEASES_PID, DAEMON_NAME, iface, interface, iface);
        ALOGD("-------------------------------------->dhcpv6+ cmd=%s",daemon_cmd);
        property_set(VERDORSPEC, option125Info);
        property_set(VENDORID, login);
    } else if (supportOption60 && !supportOption125 && password != NULL) {//OPTION60
        snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:-op16 des3 %s "DAEMON_LEASES_PID, DAEMON_NAME, iface, interface, iface);
        ALOGD("-------------------------------------->dhcpv6+ cmd=%s",daemon_cmd);
        property_set(USERNAME, login);
        property_set(PASSWD, passwdEncode);
    } else if (supportOption60 && !supportOption125 && password == NULL) {//OPTION60
        snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:-op16 id %s "DAEMON_LEASES_PID, DAEMON_NAME, iface, interface, iface);
        ALOGD("-------------------------------------->dhcpv6+ cmd=%s",daemon_cmd);
        property_set(VENDORID, login);
    } else if(!supportOption60 && supportOption125) {//OPTION125
        snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:-op17 %s "DAEMON_LEASES_PID, DAEMON_NAME, iface, interface, iface);
        ALOGD("-------------------------------------->dhcpv6+ cmd=%s",daemon_cmd);
        property_set(VERDORSPEC, option125Info);
    }
/***HISI end***///dhcpv6+
    ALOGD("========daemon_cmd[%s],line[%d]", daemon_cmd,__LINE__);
    property_set(ctrl_prop, daemon_cmd);

    memset(result_prop_name, '\0', PROPERTY_VALUE_MAX);
    snprintf(result_prop_name, sizeof(result_prop_name), "%s_%s", DAEMON_PROP_NAME, iface);

    if (wait_for_property(result_prop_name, desired_status, 10) < 0) {
        snprintf(errmsg, sizeof(errmsg), "%s", "Timed out waiting for DHCPv6 to start");
        ALOGE("%s", errmsg);
        return -1;
    }

    memset(result_prop_name, '\0', PROPERTY_VALUE_MAX);
    snprintf(result_prop_name, sizeof(result_prop_name), "%s.%s.result", DAEMON_NAME, interface);
    //if (NULL != strstr(interface, ".")){ // don't change result_prop_name, vlan also need: "dhclient.eth0.85.result"
    //    memset(result_prop_name, '\0', PROPERTY_VALUE_MAX);
    //    snprintf(result_prop_name, sizeof(result_prop_name), "%s.%s.result", DAEMON_NAME,"vlan"); //tian
    //}

    /* Wait for the daemon to return a result */
    if (wait_for_property(result_prop_name, NULL, 30) < 0) {
        snprintf(errmsg, sizeof(errmsg), "%s", "Timed out waiting for DHCPv6 to finish");
        ALOGE("%s", errmsg);
        return -1;
    }

    if (!property_get(result_prop_name, prop_value, NULL)) {
        /* shouldn't ever happen, given the success of wait_for_property() */
        snprintf(errmsg, sizeof(errmsg), "%s", "DHCPv6 result property was not set");
        ALOGE("%s", errmsg);
        return -1;
    }
    if (strcmp(prop_value, "ok") == 0) {
        snprintf(errmsg, sizeof(errmsg), "%s %s", "DHCPv6 Success ", prop_value);
        ALOGD("%s", errmsg);
    } else {
        snprintf(errmsg, sizeof(errmsg), "%s %s", "DHCPv6 Fail ", prop_value);
        ALOGE("%s", errmsg);
        return -1;
    }
    return 0;
}

int dhcpv6_dns_do_request(const char *interface)
{
    ALOGD("-----------------dhcpv6_dns_do_request");
    char result_prop_name[PROPERTY_VALUE_MAX] = {0};
    char daemon_cmd[PROPERTY_VALUE_MAX] = {0};
    char prop_value[PROPERTY_VALUE_MAX] = {0};
    const char *ctrl_prop = "ctl.start";
    const char *desired_status = "running";
    char ipaddress6[256] = {0};

    memset(daemon_cmd, '\0', PROPERTY_VALUE_MAX);
    snprintf(daemon_cmd, sizeof(daemon_cmd), "%s:%s "DAEMON_LEASES_PID, DAEMON_NAME_DNS, interface, interface);
    if (NULL != strstr(interface, ".")){ //vlan need:"dhclientDns_vlan:eth0.85 VLAN_LEASE_PATH"
        memset(daemon_cmd, '\0', PROPERTY_VALUE_MAX);
        snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s:%s %s", DAEMON_NAME_DNS, "vlan", interface, VLAN_LEASE_PATH);//vlan
    }
    ALOGD("========daemon_cmd[%s],line[%d]", daemon_cmd,__LINE__);
    property_get("dhclient.pid", prop_value, NULL); //used by wait_for_property_reverse();
    property_set(ctrl_prop, daemon_cmd);

    memset(result_prop_name, '\0', PROPERTY_VALUE_MAX);
    snprintf(result_prop_name, sizeof(result_prop_name), "%s", DAEMON_DNS_PROP_NAME);
    if (NULL != strstr(interface, ".")){ //vlan need:"init.svc.dhclientDns_vlan"
        memset(daemon_cmd, '\0', PROPERTY_VALUE_MAX);
        snprintf(result_prop_name, sizeof(result_prop_name), "%s_%s", DAEMON_DNS_PROP_NAME, "vlan");
    }

    ALOGD("-------------------------------------->result_prop_name is %s",result_prop_name);
    if (wait_for_property_reverse("dhclient.pid", prop_value, 10) < 0)
    {
        snprintf(errmsg, sizeof(errmsg), "%s", "Timed out waiting for DHCPv6 to start");
        ALOGD("----------------------Timed out waiting for DHCPv6 to start");
        return -1;
    }

    memset(result_prop_name, '\0', PROPERTY_VALUE_MAX);
    snprintf(result_prop_name, sizeof(result_prop_name),"dhclient.%s.dns1", interface);
    //Wait for the daemon to return a result
    if (wait_for_property(result_prop_name, NULL, 30) < 0)
    {
        ALOGD("--------------------time out waiting for dhcpv6dns to finish");
        snprintf(errmsg, sizeof(errmsg), "%s", "Timed out waiting for DHCPv6dns to finish");
        return -1;
    }
    return 0;
}

/**
 * Stop the DHCPv6 client daemon.
 */
int dhcpv6_stop(const char *interface)
{
    char result_prop_name[PROPERTY_VALUE_MAX];
    char daemon_cmd[PROPERTY_VALUE_MAX];
    char prop_value[PROPERTY_VALUE_MAX] = {'\0'};
    const char *ctrl_prop = "ctl.stop";
    const char *desired_status = "stopped";
    int i = 0;
    ALOGD("-----------------dhcpv6_stop");

    memset(daemon_cmd, '\0', PROPERTY_VALUE_MAX);
    snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s", DAEMON_NAME, interface);
    if (NULL != strstr(interface, ".")){ //vlan need only service name:"dhclient_vlan"
        memset(daemon_cmd, '\0', PROPERTY_VALUE_MAX); //tian
        snprintf(daemon_cmd, sizeof(daemon_cmd), "%s_%s", DAEMON_NAME, "vlan");//vlan
    }
    ALOGD("========daemon_cmd[%s],line[%d]", daemon_cmd,__LINE__);
    property_set(ctrl_prop, daemon_cmd);

    /* Stop the daemon and wait until it's reported to be stopped */
    memset(result_prop_name, '\0', PROPERTY_VALUE_MAX);
    snprintf(result_prop_name, sizeof(result_prop_name), "%s_%s", DAEMON_PROP_NAME, interface);
    if (NULL != strstr(interface, ".")){ //vlan need only service name:"dhclient_vlan"
        memset(result_prop_name, '\0', PROPERTY_VALUE_MAX);
        snprintf(result_prop_name, sizeof(result_prop_name), "%s_%s", DAEMON_PROP_NAME, "vlan"); //vlan
    }
    ALOGD("========2daemon_cmd[%s],line[%d]", result_prop_name,__LINE__);
    if (wait_for_property(result_prop_name, desired_status, 10) < 0) {
        return -1;
    }
    snprintf(daemon_cmd, sizeof(daemon_cmd), "%s.ipaddress.%s", DAEMON_NAME, interface);
    property_set(daemon_cmd, "");
    snprintf(daemon_cmd, sizeof(daemon_cmd), "%s.prefixlen.%s", DAEMON_NAME, interface);
    property_set(daemon_cmd, "");
    snprintf(daemon_cmd, sizeof(daemon_cmd), "%s.%s.result", DAEMON_NAME, interface);
    property_set(daemon_cmd, "");
    for(i = 1; i < 5; i++) {
        snprintf(daemon_cmd, sizeof(daemon_cmd), "%s.%s.dns%d", DAEMON_NAME, interface, i);
        memset(prop_value, '\0', PROPERTY_VALUE_MAX);
        property_get(daemon_cmd, prop_value, "");
        if(strlen(prop_value) != 0) {
            property_set(daemon_cmd, "");
        } else {
            ALOGD("no %s", daemon_cmd);
        }
    }
    return 0;
}

int dhcpv6_check_status(const char *interface)
{
    char result_prop_name[PROPERTY_VALUE_MAX];
    char prop_value[PROPERTY_VALUE_MAX] = {'\0'};

    memset(result_prop_name, '\0', PROPERTY_VALUE_MAX);
    snprintf(result_prop_name, sizeof(result_prop_name), "%s.%s.result", DAEMON_NAME, interface);

    if (!property_get(result_prop_name, prop_value, NULL)) {
        /* shouldn't ever happen, given the success of wait_for_property() */
        return -1;
    }
    if (strcmp(prop_value, "ok") == 0) {
        return 0;
    } else {
        return -1;
    }
}

const char *dhcpv6_get_ipaddress(const char *interface, char *ipv6address)
{
    char result_prop_name[PROPERTY_VALUE_MAX];

    if(!dhcpv6_check_status(interface)) {
        memset(result_prop_name, '\0', PROPERTY_VALUE_MAX);
        snprintf(result_prop_name, sizeof(result_prop_name), "%s.ipaddress.%s", DAEMON_NAME, interface);
        if(!property_get(result_prop_name, ipv6address, NULL)) {
            return NULL;
        } else {
            return ipv6address;
        }
    } else {
        return NULL;
    }
}

const char *dhcpv6_get_gateway(const char *interface, char *ipv6_gateway)
{
    char addr6x[80];
    char iface[16];
    int iflags, metric, refcnt, use, prefix_len, slen;
    FILE *fp = fopen("/proc/net/ipv6_route", "r");

    while (1) {
        int r;
        r = fscanf(fp, "%32s%x%*s%x%32s%x%x%x%x%s\n",
                addr6x+14, &prefix_len, &slen, addr6x+40+7,
                &metric, &use, &refcnt, &iflags, iface);
        if (feof(fp))
        {
            ALOGD("fp read end");
            break;
        }
        if (0 != strncmp(interface, iface, strlen(iface))) //not match interface(eth0 or eth0.xx)
        {
            continue;
        }

        if(iflags & RTF_GATEWAY) {
            int i = 0;
            char *p = addr6x+14;

            do {
                if (!*p) {
                    if (i == 40) {
                        addr6x[39] = 0;
                        ++p;
                        continue;
                    }
                    fclose(fp);
                    return NULL;
                }
                addr6x[i++] = *p++;
                if (!((i+1) % 5)) {
                    addr6x[i++] = ':';
                }
            } while (i < 40+28+7);
            strcpy(ipv6_gateway, addr6x+40);
            *(ipv6_gateway + strlen(addr6x+40) + 1) = 0x00;
        }
        if (r != 9) {
            if ((r < 0) && feof(fp)) { /* EOF with no (nonspace) chars read. */
                break;
            }
        }
    }
    fclose(fp);
    if(strlen(ipv6_gateway) != 0) {
        return ipv6_gateway;
    } else {
        return NULL;
    }
}

const char *dhcpv6_get_prefixlen(const char *interface, char *prefixlen)
{
    char result_prop_name[PROPERTY_VALUE_MAX];

    if(!dhcpv6_check_status(interface)) {
        memset(result_prop_name, '\0', PROPERTY_VALUE_MAX);
        snprintf(result_prop_name, sizeof(result_prop_name), "%s.prefixlen.%s", DAEMON_NAME, interface);
        if(!property_get(result_prop_name, prefixlen, NULL)) {
            return NULL;
        } else {
            return prefixlen;
        }
    } else {
        return NULL;
    }
}

const char *dhcpv6_get_dns(const char *interface, char *dns, int dns_cnt)
{
    char result_prop_name[PROPERTY_VALUE_MAX];

    if(!dhcpv6_check_status(interface)) {
        memset(result_prop_name, '\0', PROPERTY_VALUE_MAX);
        snprintf(result_prop_name, sizeof(result_prop_name), "%s.%s.dns%d", DAEMON_NAME, interface, dns_cnt);
        if(!property_get(result_prop_name, dns, NULL)) {
            return NULL;
        } else {
            return dns;
        }
    } else {
        return NULL;
    }
}

#define DHCPV6_DNS_CNT_MAX 6
int dhcpv6_get_dns_cnt(const char *interface)
{
    char result_prop_name[PROPERTY_VALUE_MAX];
    char prop_value[PROPERTY_VALUE_MAX] = {'\0'};
    int i = 1;

    if(!dhcpv6_check_status(interface)) {
        for(i = 1; i < DHCPV6_DNS_CNT_MAX; i++) {
            memset(result_prop_name, '\0', PROPERTY_VALUE_MAX);
            snprintf(result_prop_name, sizeof(result_prop_name), "%s.%s.dns%d", DAEMON_NAME, interface, i);
            if(!property_get(result_prop_name, prop_value, NULL)) {
                ALOGD("dhcpv6_get_dns_cnt dns %d = NULL", i);
                break;
            }
        }
    }
    return i - 1;
}

char *dhcpv6_get_errmsg() {
    return errmsg;
}
