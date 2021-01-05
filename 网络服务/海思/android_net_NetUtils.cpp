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

#define LOG_TAG "NetUtils"

#include "jni.h"
#include "JNIHelp.h"
#include <utils/misc.h>
#include <android_runtime/AndroidRuntime.h>
#include <utils/Log.h>
#include <arpa/inet.h>
#include <cutils/properties.h>

extern "C" {
int ifc_enable(const char *ifname);
int ifc_disable(const char *ifname);
int ifc_reset_connections(const char *ifname, int reset_mask);
int ifc_get_default_route(const char *ifname);

int dhcp_do_request(const char * const ifname,
                    const char *ipaddr,
                    const char *gateway,
                    uint32_t *prefixLength,
                    const char *dns[],
                    const char *server,
                    uint32_t *lease,
                    const char *vendorInfo,
                    const char *domains,
                    char *mtu,
                    bool supportOption60,
                    const char *login,
                    const char *password,
                    bool supportOption125,
                    const char *option125Info);

int dhcp_do_request_renew(const char * const ifname,
                    const char *ipaddr,
                    const char *gateway,
                    uint32_t *prefixLength,
                    const char *dns[],
                    const char *server,
                    uint32_t *lease,
                    const char *vendorInfo,
                    const char *domains,
                    const char *mtu);

int dhcp_stop(const char *ifname);
int dhcp_release_lease(const char *ifname);
char *dhcp_get_errmsg();
const char *ifc_get_ipaddr(const char *ifname);
const char *ifc_get_netmask(const char *ifname);
int ifc_get_netlink_status(const char *ifname);
int ifc_configure(const char *ifname, in_addr_t ipaddr, in_addr_t netmask, in_addr_t gateway, in_addr_t dns1, in_addr_t dns2);
int ifc_remove_default_route(const char *ifname);
int ifc_remove_net_routes(const char *ifname);
void ifc_clear_ipv4_addresses(const char *name);
int check_ip_conflict(const char *interface, const char *ipaddr);
#ifdef IPV6
//int dhcpv6_do_request(const char *interface);
int dhcpv6_do_request(const char *interface,
                    bool supportOption60,
                    const char *login,
                    const char *password,
                    bool supportOption125,
                    const char *option125Info);
int dhcpv6_dns_do_request(const char *interface);
void dhcpv6_release(const char *interface);
int dhcpv6_stop(const char *interface);
int dhcpv6_check_status(const char *interface);
const char *dhcpv6_get_ipaddress(const char *interface, char *ipv6address);
const char *ifc_get_ipv6_linklocal_addresses(const char *interface, char *ipv6address);
const char *dhcpv6_get_gateway(const char *interface, char *ipv6_gateway);
const char *dhcpv6_get_prefixlen(const char *interface, char *prefixlen);
const char *dhcpv6_get_dns(const char *interface, char *dns, int dns_cnt);
int dhcpv6_get_dns_cnt(const char *interface);
char *dhcpv6_get_errmsg();
int ifc_configure6(const char *ifname, const char *address, uint32_t prefixLength, const char *gateway, const char *dns1, const char *dns2);
int ifc_clear_ipv6_addresses(const char *name);
void ifc_clear_ipv6_static_dns(const char *name);
#endif
}

#define NETUTILS_PKG_NAME "android/net/NetworkUtils"

namespace android {

/*
 * The following remembers the jfieldID's of the fields
 * of the DhcpInfo Java object, so that we don't have
 * to look them up every time.
 */
static struct fieldIds {
    jmethodID clear;
    jmethodID setInterfaceName;
    jmethodID addLinkAddress;
    jmethodID addGateway;
    jmethodID addDns;
    jmethodID setDomains;
    jmethodID setServerAddress;
    jmethodID setLeaseDuration;
    jmethodID setVendorInfo;
} dhcpResultsFieldIds;

static jint android_net_utils_enableInterface(JNIEnv* env, jobject clazz, jstring ifname)
{
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    result = ::ifc_enable(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jint)result;
}

static jint android_net_utils_disableInterface(JNIEnv* env, jobject clazz, jstring ifname)
{
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    result = ::ifc_disable(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jint)result;
}

static jint android_net_utils_resetConnections(JNIEnv* env, jobject clazz,
      jstring ifname, jint mask)
{
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);

    ALOGD("android_net_utils_resetConnections in env=%p clazz=%p iface=%s mask=0x%x\n",
          env, clazz, nameStr, mask);

    result = ::ifc_reset_connections(nameStr, mask);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jint)result;
}

static jboolean android_net_utils_runDhcpCommon(JNIEnv* env, jobject clazz, jstring ifname,
        jobject dhcpResults, bool renew, jint option60, jstring Login, jstring Password,
        jint option125, jstring option125Info)
{
    int result;
    char  ipaddr[PROPERTY_VALUE_MAX];
    uint32_t prefixLength;
    char gateway[PROPERTY_VALUE_MAX];
    char    dns1[PROPERTY_VALUE_MAX];
    char    dns2[PROPERTY_VALUE_MAX];
    char    dns3[PROPERTY_VALUE_MAX];
    char    dns4[PROPERTY_VALUE_MAX];
    const char *dns[5] = {dns1, dns2, dns3, dns4, NULL};
    char  server[PROPERTY_VALUE_MAX];
    uint32_t lease;
    char vendorInfo[PROPERTY_VALUE_MAX];
    char domains[PROPERTY_VALUE_MAX];
    char mtu[PROPERTY_VALUE_MAX];
    bool option60Support = (1 == option60);
    bool option125Support = (1 == option125);

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    if (nameStr == NULL) return (jboolean)false;

    const char *LoginStr= env->GetStringUTFChars(Login, NULL);
    const char *PasswordStr= env->GetStringUTFChars(Password, NULL);
    if (option60Support == true && LoginStr == NULL) return (jboolean)false;

    const char *option125Str = env->GetStringUTFChars(option125Info, NULL);
    if (option125Support == true && option125Str == NULL) return (jboolean)false;

    if (renew) {
        result = ::dhcp_do_request_renew(nameStr, ipaddr, gateway, &prefixLength,
                dns, server, &lease, vendorInfo, domains, mtu);
    } else {
        result = ::dhcp_do_request(nameStr, ipaddr, gateway, &prefixLength,
                dns, server, &lease, vendorInfo, domains, mtu,
                option60Support, LoginStr, PasswordStr, option125Support, option125Str);
    }
    if (result != 0) {
        ALOGD("dhcp_do_request failed : %s (%s)", nameStr, renew ? "renew" : "new");
    }

    env->ReleaseStringUTFChars(ifname, nameStr);
    env->ReleaseStringUTFChars(Login, LoginStr);
    env->ReleaseStringUTFChars(Password, PasswordStr);
    env->ReleaseStringUTFChars(option125Info, option125Str);
    if (result == 0) {
        env->CallVoidMethod(dhcpResults, dhcpResultsFieldIds.clear);

        // set mIfaceName
        // dhcpResults->setInterfaceName(ifname)
        env->CallVoidMethod(dhcpResults, dhcpResultsFieldIds.setInterfaceName, ifname);

        // set the linkAddress
        // dhcpResults->addLinkAddress(inetAddress, prefixLength)
        result = env->CallBooleanMethod(dhcpResults, dhcpResultsFieldIds.addLinkAddress,
                env->NewStringUTF(ipaddr), prefixLength);
    }

    if (result == 0) {
        // set the gateway
        // dhcpResults->addGateway(gateway)
        result = env->CallBooleanMethod(dhcpResults,
                dhcpResultsFieldIds.addGateway, env->NewStringUTF(gateway));
    }

    if (result == 0) {
        // dhcpResults->addDns(new InetAddress(dns1))
        result = env->CallBooleanMethod(dhcpResults,
                dhcpResultsFieldIds.addDns, env->NewStringUTF(dns1));
    }

    if (result == 0) {
        env->CallVoidMethod(dhcpResults, dhcpResultsFieldIds.setDomains,
                env->NewStringUTF(domains));

        result = env->CallBooleanMethod(dhcpResults,
                dhcpResultsFieldIds.addDns, env->NewStringUTF(dns2));

        if (result == 0) {
            result = env->CallBooleanMethod(dhcpResults,
                    dhcpResultsFieldIds.addDns, env->NewStringUTF(dns3));
            if (result == 0) {
                result = env->CallBooleanMethod(dhcpResults,
                        dhcpResultsFieldIds.addDns, env->NewStringUTF(dns4));
            }
        }
    }

    if (result == 0) {
        // dhcpResults->setServerAddress(new InetAddress(server))
        result = env->CallBooleanMethod(dhcpResults, dhcpResultsFieldIds.setServerAddress,
                env->NewStringUTF(server));
    }

    if (result == 0) {
        // dhcpResults->setLeaseDuration(lease)
        env->CallVoidMethod(dhcpResults,
                dhcpResultsFieldIds.setLeaseDuration, lease);

        // dhcpResults->setVendorInfo(vendorInfo)
        env->CallVoidMethod(dhcpResults, dhcpResultsFieldIds.setVendorInfo,
                env->NewStringUTF(vendorInfo));
    }
    return (jboolean)(result == 0);
}

static jint android_net_utils_checkIpConflict(JNIEnv* env, jobject clazz, jstring ifname, jstring ipaddr)
{
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    const char *ipAddr = env->GetStringUTFChars(ipaddr, NULL);

    result = ::check_ip_conflict(nameStr, ipAddr);

    env->ReleaseStringUTFChars(ifname, nameStr);
    env->ReleaseStringUTFChars(ipaddr, ipAddr);
    return (jint)result;
}

static jboolean android_net_utils_runDhcpv6(JNIEnv* env, jobject clazz, jstring ifname,
           jint option60, jstring Login, jstring Password, jint option125, jstring option125Info)
//static jboolean android_net_utils_runDhcpv6(JNIEnv* env, jobject clazz, jstring ifname)
{
#ifdef IPV6
    int result;
    bool option60Support = (1 == option60);
    bool option125Support = (1 == option125);
    const char *LoginStr= env->GetStringUTFChars(Login, NULL);
    if (option60Support == true && LoginStr == NULL){
        ALOGD("android_net_utils_runDhcpv6 return: option60 login is NULL");
        return (jboolean)false;
    }
    const char *PasswordStr= env->GetStringUTFChars(Password, NULL);

    const char *option125Str = env->GetStringUTFChars(option125Info, NULL);
    if (option125Support == true && option125Str == NULL) {
        if(LoginStr) env->ReleaseStringUTFChars(Login, LoginStr);
        if(PasswordStr) env->ReleaseStringUTFChars(Password, PasswordStr);
        ALOGD("android_net_utils_runDhcpv6 return: option125 string is NULL");
        return (jboolean)false;
    }

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    ALOGD("--------%s dhcpv6_do_request began: ",nameStr);
//    result = ::dhcpv6_do_request(nameStr);
    result = ::dhcpv6_do_request(nameStr,
            option60Support, LoginStr, PasswordStr, option125Support, option125Str);
    ALOGD("--------%s dhcpv6_do_request result: %d ",nameStr, result);
    env->ReleaseStringUTFChars(ifname, nameStr);
    if(LoginStr) env->ReleaseStringUTFChars(Login, LoginStr);
    if(PasswordStr) env->ReleaseStringUTFChars(Password, PasswordStr);
    if(option125Str) env->ReleaseStringUTFChars(option125Info, option125Str);
    return (jboolean)(result == 0);
#else
    return false;
#endif
}

static jboolean android_net_utils_releaseDhcpv6lease(JNIEnv* env, jobject clazz, jstring ifname)
{
    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    /*
     * ipoe need to send username/passwd in dhcp solicit
     * packet, for supporting server to verify them,
     * so release old ip here && send solicit again.
    */
    if (nameStr == NULL) {
        ALOGE("releaseDhcpv6lease error, ifname is NULL");
        return false;
    }
    dhcpv6_release(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);

    return true;
}

static jboolean android_net_utils_runDhcpv6Dns(JNIEnv* env, jobject clazz, jstring ifname)
{
#ifdef IPV6
    int result;
    ALOGD("----------------------------->begin to execute android_net_utils_runDhcpv6Dns");
    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    result = ::dhcpv6_dns_do_request(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jboolean)(result == 0);
#else
    ALOGD("---------------------------->not IPV6");
    return false;
#endif
}

static jboolean android_net_utils_stopDhcpv6(JNIEnv* env, jobject clazz, jstring ifname)
{
#ifdef IPV6
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    result = ::dhcpv6_stop(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jboolean)(result == 0);
#else
    return 0;
#endif
}

static jboolean android_net_utils_checkDhcpv6Status(JNIEnv* env, jobject clazz, jstring ifname)
{
#ifdef IPV6
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    result = ::dhcpv6_check_status(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jboolean)(result == 0);
#else
    return 0;
#endif
}

static jstring android_net_utils_getDhcpv6Ipaddress(JNIEnv* env, jobject clazz, jstring ifname)
{
#ifdef IPV6
    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    char ipv6address[50] = {0x00};
    jstring result = env->NewStringUTF(::dhcpv6_get_ipaddress(nameStr,(char *)ipv6address));
    if(nameStr){
        env->ReleaseStringUTFChars(ifname, nameStr);
    }
    return result;
#else
    return NULL;
#endif
}

static jstring android_net_utils_getIpv6LinklocalAddress(JNIEnv* env, jobject clazz, jstring ifname)
{
#ifdef IPV6
    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    char ipv6address[50] = {0x00};
    jstring result = env->NewStringUTF(::ifc_get_ipv6_linklocal_addresses(nameStr,(char *)ipv6address));
    if(nameStr){
        env->ReleaseStringUTFChars(ifname, nameStr);
    }
    return result;
#else
    return NULL;
#endif
}

static jstring android_net_utils_getDhcpv6Gateway(JNIEnv* env, jobject clazz, jstring ifname)
{
#ifdef IPV6
    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    char gateway[50] = {0x00};
    jstring result = env->NewStringUTF(::dhcpv6_get_gateway(nameStr,(char *)gateway));
    if(nameStr){
        env->ReleaseStringUTFChars(ifname, nameStr);
    }
    return result;
#else
    return NULL;
#endif
}

static jstring android_net_utils_getDhcpv6Prefixlen(JNIEnv* env, jobject clazz, jstring ifname)
{
#ifdef IPV6
    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    char prefixlen[10] = {0x00};
    jstring result = env->NewStringUTF(::dhcpv6_get_prefixlen(nameStr,(char *)prefixlen));
    if(nameStr){
        env->ReleaseStringUTFChars(ifname, nameStr);
    }
    return result;
#else
    return NULL;
#endif
}

static jstring android_net_utils_getDhcpv6Dns(JNIEnv* env, jobject clazz, jstring ifname, jint cnt)
{
#ifdef IPV6
    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    char dns[50] = {0x00};
    jstring result = env->NewStringUTF(::dhcpv6_get_dns(nameStr,(char *)dns, cnt));
    if(nameStr){
        env->ReleaseStringUTFChars(ifname, nameStr);
    }
    return result;
#else
    return NULL;
#endif
}

static jint android_net_utils_getDhcpv6DnsCnt(JNIEnv* env, jobject clazz, jstring ifname)
{
#ifdef IPV6
    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    int result = (jint)(::dhcpv6_get_dns_cnt(nameStr));
    if(nameStr){
        env->ReleaseStringUTFChars(ifname, nameStr);
    }
    return (jint)result;
#else
    return 0;
#endif
}

static jstring android_net_utils_getDhcpv6Error(JNIEnv* env, jobject clazz)
{
#ifdef IPV6
    return env->NewStringUTF(::dhcpv6_get_errmsg());
#else
    return NULL;
#endif
}

static jboolean android_net_utils_configure6Interface(JNIEnv* env,
        jobject clazz,
        jstring ifname,
        jstring ipaddr,
        jint prefixLength,
        jstring gateway,
        jstring dns1,
        jstring dns2)
{
#ifdef IPV6
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    const char *ipaddrStr = env->GetStringUTFChars(ipaddr, NULL);
    const char *gatewayStr = env->GetStringUTFChars(gateway, NULL);
    const char *dns1Str = env->GetStringUTFChars(dns1, NULL);
    const char *dns2Str = env->GetStringUTFChars(dns2, NULL);
    result = ::ifc_configure6(nameStr, ipaddrStr, prefixLength, gatewayStr, dns1Str, dns2Str);
    env->ReleaseStringUTFChars(ifname, nameStr);
    env->ReleaseStringUTFChars(ipaddr, ipaddrStr);
    env->ReleaseStringUTFChars(gateway, gatewayStr);
    env->ReleaseStringUTFChars(dns1, dns1Str);
    env->ReleaseStringUTFChars(dns2, dns2Str);
    return (jboolean)(result == 0);
#else
    return false;
#endif
}

static jboolean android_net_utils_clearIpv6Addresses(JNIEnv* env,
        jobject clazz,
        jstring ifname)
{
#ifdef IPV6
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    result = ::ifc_clear_ipv6_addresses(nameStr);
    ifc_clear_ipv6_static_dns(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jboolean)(result == 0);
#else
    return false;
#endif
}

static jboolean android_net_utils_clearIpv4Addresses(JNIEnv* env,
        jobject clazz,
        jstring ifname)
{
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    ::ifc_clear_ipv4_addresses(nameStr);
    //ifc_clear_ipv4_static_dns(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jboolean)(true);
}

static jboolean android_net_utils_runDhcp(JNIEnv* env, jobject clazz, jstring ifname, jobject info,
           jint option60, jstring Login, jstring Password, jint option125, jstring option125Info)
{
    return android_net_utils_runDhcpCommon(env, clazz, ifname, info, false,
               option60, Login, Password, option125, option125Info);
}

static jboolean android_net_utils_runDhcpRenew(JNIEnv* env, jobject clazz, jstring ifname, jobject info)
{
    return android_net_utils_runDhcpCommon(env, clazz, ifname, info, true, 0, NULL, NULL, 0, NULL);
}


static jboolean android_net_utils_stopDhcp(JNIEnv* env, jobject clazz, jstring ifname)
{
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    result = ::dhcp_stop(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jboolean)(result == 0);
}

static jboolean android_net_utils_releaseDhcpLease(JNIEnv* env, jobject clazz, jstring ifname)
{
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    result = ::dhcp_release_lease(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jboolean)(result == 0);
}

static jstring android_net_utils_getDhcpError(JNIEnv* env, jobject clazz)
{
    return env->NewStringUTF(::dhcp_get_errmsg());
}

static void android_net_utils_markSocket(JNIEnv *env, jobject thiz, jint socket, jint mark)
{
    if (setsockopt(socket, SOL_SOCKET, SO_MARK, &mark, sizeof(mark)) < 0) {
        jniThrowException(env, "java/lang/IllegalStateException", "Error marking socket");
    }
}

static jstring android_net_utils_getIpaddr(JNIEnv* env, jobject clazz, jstring ifname)
{
    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    const char *ipaddr = ::ifc_get_ipaddr(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return env->NewStringUTF(ipaddr);
}

static jstring android_net_utils_getNetmask(JNIEnv* env, jobject clazz, jstring ifname)
{
    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    const char *netmask = ::ifc_get_netmask(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return env->NewStringUTF(netmask);
}

static jint android_net_utils_getNetlinkStatus(JNIEnv* env, jobject clazz, jstring ifname)
{
    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    int status = ::ifc_get_netlink_status(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jint)(status);
}

static jint android_net_utils_getDefaultRoute(JNIEnv* env, jobject clazz, jstring ifname)
{
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    result = ::ifc_get_default_route(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jint)result;
}

static jboolean android_net_utils_configureInterface(JNIEnv* env,
        jobject clazz,
        jstring ifname,
        jint ipaddr,
        jint mask,
        jint gateway,
        jint dns1,
        jint dns2)
{
    int result;
    uint32_t lease;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    result = ::ifc_configure(nameStr, ipaddr, mask, gateway, dns1, dns2);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jboolean)(result == 0);
}

static jint android_net_utils_removeDefaultRoute(JNIEnv* env, jobject clazz, jstring ifname)
{
    int result;

    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    result = ::ifc_remove_default_route(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jint)result;
}

static jint android_net_utils_removeNetRoute(JNIEnv* env, jobject clazz, jstring ifname)
{
    int result;
    const char *nameStr = env->GetStringUTFChars(ifname, NULL);
    result = ::ifc_remove_net_routes(nameStr);
    env->ReleaseStringUTFChars(ifname, nameStr);
    return (jint)result;
}
// ----------------------------------------------------------------------------

/*
 * JNI registration.
 */
static JNINativeMethod gNetworkUtilMethods[] = {
    /* name, signature, funcPtr */

    { "enableInterface", "(Ljava/lang/String;)I",  (void *)android_net_utils_enableInterface },
    { "checkIpConflict", "(Ljava/lang/String;Ljava/lang/String;)I",  (void *)android_net_utils_checkIpConflict },
    { "disableInterface", "(Ljava/lang/String;)I",  (void *)android_net_utils_disableInterface },
    { "resetConnections", "(Ljava/lang/String;I)I",  (void *)android_net_utils_resetConnections },
    { "runDhcpPlus", "(Ljava/lang/String;Landroid/net/DhcpResults;ILjava/lang/String;Ljava/lang/String;ILjava/lang/String;)Z",  (void *)android_net_utils_runDhcp },
    { "runDhcpRenew", "(Ljava/lang/String;Landroid/net/DhcpResults;)Z",  (void *)android_net_utils_runDhcpRenew },
    { "stopDhcp", "(Ljava/lang/String;)Z",  (void *)android_net_utils_stopDhcp },
    { "releaseDhcpLease", "(Ljava/lang/String;)Z",  (void *)android_net_utils_releaseDhcpLease },
    { "runDhcpv6Plus", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;ILjava/lang/String;)Z",  (void *)android_net_utils_runDhcpv6 },
    { "runDhcpv6Dns", "(Ljava/lang/String;)Z",  (void *)android_net_utils_runDhcpv6Dns },
    { "releaseDhcpv6lease", "(Ljava/lang/String;)Z",  (void *)android_net_utils_releaseDhcpv6lease },
    { "stopDhcpv6", "(Ljava/lang/String;)Z",  (void *)android_net_utils_stopDhcpv6 },
    { "checkDhcpv6Status", "(Ljava/lang/String;)Z",  (void *)android_net_utils_checkDhcpv6Status },
    { "getDhcpv6Ipaddress", "(Ljava/lang/String;)Ljava/lang/String;",  (void *)android_net_utils_getDhcpv6Ipaddress },
    { "getIpv6LinklocalAddress", "(Ljava/lang/String;)Ljava/lang/String;",  (void *)android_net_utils_getIpv6LinklocalAddress },
    { "getDhcpv6Gateway", "(Ljava/lang/String;)Ljava/lang/String;",  (void *)android_net_utils_getDhcpv6Gateway},
    { "getDhcpv6Prefixlen", "(Ljava/lang/String;)Ljava/lang/String;",  (void *)android_net_utils_getDhcpv6Prefixlen},
    { "getDhcpv6Dns", "(Ljava/lang/String;I)Ljava/lang/String;",  (void *)android_net_utils_getDhcpv6Dns},
    { "getDhcpv6DnsCnt", "(Ljava/lang/String;)I",  (void *)android_net_utils_getDhcpv6DnsCnt},
    { "getDhcpv6Error", "()Ljava/lang/String;", (void*) android_net_utils_getDhcpv6Error },
    { "configure6Interface", "(Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z",  (void *)android_net_utils_configure6Interface },
    { "clearIpv6Addresses", "(Ljava/lang/String;)Z",  (void *)android_net_utils_clearIpv6Addresses},
    { "clearIpv4Addresses", "(Ljava/lang/String;)Z",  (void *)android_net_utils_clearIpv4Addresses},
    { "getDhcpError", "()Ljava/lang/String;", (void*) android_net_utils_getDhcpError },
    { "markSocket", "(II)V", (void*) android_net_utils_markSocket },
    { "getIpaddr", "(Ljava/lang/String;)Ljava/lang/String;", (void*) android_net_utils_getIpaddr },
    { "getNetmask", "(Ljava/lang/String;)Ljava/lang/String;", (void*) android_net_utils_getNetmask },
    { "getNetlinkStatus", "(Ljava/lang/String;)I",  (void *)android_net_utils_getNetlinkStatus },
    { "getDefaultRoute", "(Ljava/lang/String;)I",  (void *)android_net_utils_getDefaultRoute },
    { "configureNative", "(Ljava/lang/String;IIIII)Z",  (void *)android_net_utils_configureInterface },
    { "removeDefaultRoute", "(Ljava/lang/String;)I",  (void *)android_net_utils_removeDefaultRoute },
    { "removeNetRoute", "(Ljava/lang/String;)I",  (void *)android_net_utils_removeNetRoute },
};

int register_android_net_NetworkUtils(JNIEnv* env)
{
    jclass dhcpResultsClass = env->FindClass("android/net/DhcpResults");
    LOG_FATAL_IF(dhcpResultsClass == NULL, "Unable to find class android/net/DhcpResults");
    dhcpResultsFieldIds.clear =
            env->GetMethodID(dhcpResultsClass, "clear", "()V");
    dhcpResultsFieldIds.setInterfaceName =
            env->GetMethodID(dhcpResultsClass, "setInterfaceName", "(Ljava/lang/String;)V");
    dhcpResultsFieldIds.addLinkAddress =
            env->GetMethodID(dhcpResultsClass, "addLinkAddress", "(Ljava/lang/String;I)Z");
    dhcpResultsFieldIds.addGateway =
            env->GetMethodID(dhcpResultsClass, "addGateway", "(Ljava/lang/String;)Z");
    dhcpResultsFieldIds.addDns =
            env->GetMethodID(dhcpResultsClass, "addDns", "(Ljava/lang/String;)Z");
    dhcpResultsFieldIds.setDomains =
            env->GetMethodID(dhcpResultsClass, "setDomains", "(Ljava/lang/String;)V");
    dhcpResultsFieldIds.setServerAddress =
            env->GetMethodID(dhcpResultsClass, "setServerAddress", "(Ljava/lang/String;)Z");
    dhcpResultsFieldIds.setLeaseDuration =
            env->GetMethodID(dhcpResultsClass, "setLeaseDuration", "(I)V");
    dhcpResultsFieldIds.setVendorInfo =
            env->GetMethodID(dhcpResultsClass, "setVendorInfo", "(Ljava/lang/String;)V");

    return AndroidRuntime::registerNativeMethods(env,
            NETUTILS_PKG_NAME, gNetworkUtilMethods, NELEM(gNetworkUtilMethods));
}

}; // namespace android
