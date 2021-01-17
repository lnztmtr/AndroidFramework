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

#define LOG_TAG "Netd"
#define LOCAL_DEFAULT_ROUTE_V6 "::/0"
#define ETH_INTERFACE_MATCHES "eth"
#define HISI_IPV6_ROUTE_PRIORITY "persist.hisi.ipv6.rtpriority"

#include <cutils/log.h>
#include <cutils/properties.h>

#include <sysutils/NetlinkEvent.h>
#include "NetlinkHandler.h"
#include "NetlinkManager.h"
#include "ResponseCode.h"
#include <logwrap/logwrap.h>
#include <sys/socket.h>

const int MAX_ROUTE_LNE = 64;
const int PLANE_A_DEF_ROUTE_METRIC = 128;
const int PLANE_B_DEF_ROUTE_METRIC = 138;
void RunRouteCmd(const char *gateway, const char *routeStr, const char *iface,
                 const char *priority, const char *metricStr);

NetlinkHandler::NetlinkHandler(NetlinkManager *nm, int listenerSocket,
                               int format) :
                        NetlinkListener(listenerSocket, format) {
    mNm = nm;
}

NetlinkHandler::~NetlinkHandler() {
}

int NetlinkHandler::start() {
    return this->startListener();
}

int NetlinkHandler::stop() {
    return this->stopListener();
}

void NetlinkHandler::onEvent(NetlinkEvent *evt) {
    const char *subsys = evt->getSubsystem();
    char route_priority[PROPERTY_VALUE_MAX];
    if (!subsys) {
        ALOGW("No subsystem found in netlink event");
        return;
    }

    if (!strcmp(subsys, "net")) {
        int action = evt->getAction();
        const char *iface = evt->findParam("INTERFACE");

        if (action == evt->NlActionAdd) {
            notifyInterfaceAdded(iface);
        } else if (action == evt->NlActionRemove) {
            notifyInterfaceRemoved(iface);
        } else if (action == evt->NlActionChange) {
            evt->dump();
            notifyInterfaceChanged("nana", true);
        } else if (action == evt->NlActionLinkUp) {
            notifyInterfaceLinkChanged(iface, true);
        } else if (action == evt->NlActionLinkDown) {
            notifyInterfaceLinkChanged(iface, false);
        } else if (action == evt->NlActionAddressUpdated ||
                   action == evt->NlActionAddressRemoved) {
            const char *address = evt->findParam("ADDRESS");
            const char *flags = evt->findParam("FLAGS");
            const char *scope = evt->findParam("SCOPE");
            if (iface && flags && scope) {
                notifyAddressChanged(action, address, iface, flags, scope);
            }
        }  else if (action == evt->NlActionRouteUpdated ||
                    action == evt->NlActionRouteRemoved) {
            const char *route = evt->findParam("ROUTE");
            const char *gateway = evt->findParam("GATEWAY");
            const char *iface = evt->findParam("INTERFACE");
            const char *priority = evt->findParam("PRIORITY");
            const int family = atoi(evt->findParam("FAMILY"));
            // increase eth route priority by default,
            // set prop to "none" for not modify route priority.
            property_get(HISI_IPV6_ROUTE_PRIORITY, route_priority, "eth");
            ALOGD("route_priority=%s", route_priority);
            if (!strcmp(route_priority, "eth") &&
                (action == evt->NlActionRouteUpdated) &&
                (family == AF_INET6) && route && (gateway || iface)) {
                increaseEthRoutePriorityV6(route, gateway, iface, priority);
            }
        } else if (action == evt->NlActionMOFlagChange) {
            const char *flags = evt->findParam("MOFLAGS");
            if (flags) {
                //ALOGE("++++++++++iface=%s,flags=%s", iface,flags);
                notifyMOFlagChanged(action, iface, flags);
            }
        }

    } else if (!strcmp(subsys, "qlog")) {
        const char *alertName = evt->findParam("ALERT_NAME");
        const char *iface = evt->findParam("INTERFACE");
        notifyQuotaLimitReached(alertName, iface);

    } else if (!strcmp(subsys, "xt_idletimer")) {
        int action = evt->getAction();
        const char *label = evt->findParam("LABEL");
        const char *state = evt->findParam("STATE");
        // if no LABEL, use INTERFACE instead
        if (label == NULL) {
            label = evt->findParam("INTERFACE");
        }
        if (state)
            notifyInterfaceClassActivity(label, !strcmp("active", state));

#if !LOG_NDEBUG
    } else if (strcmp(subsys, "platform") && strcmp(subsys, "backlight")) {
        /* It is not a VSYNC or a backlight event */
        ALOGV("unexpected event from subsystem %s", subsys);
#endif
    }
}

void NetlinkHandler::notifyInterfaceAdded(const char *name) {
    char msg[255];
    snprintf(msg, sizeof(msg), "Iface added %s", name);

    mNm->getBroadcaster()->sendBroadcast(ResponseCode::InterfaceChange,
            msg, false);
}

void NetlinkHandler::notifyInterfaceRemoved(const char *name) {
    char msg[255];
    snprintf(msg, sizeof(msg), "Iface removed %s", name);

    mNm->getBroadcaster()->sendBroadcast(ResponseCode::InterfaceChange,
            msg, false);
}

void NetlinkHandler::notifyInterfaceChanged(const char *name, bool isUp) {
    char msg[255];
    snprintf(msg, sizeof(msg), "Iface changed %s %s", name,
             (isUp ? "up" : "down"));

    mNm->getBroadcaster()->sendBroadcast(ResponseCode::InterfaceChange,
            msg, false);
}

void NetlinkHandler::notifyInterfaceLinkChanged(const char *name, bool isUp) {
    char msg[255];
    snprintf(msg, sizeof(msg), "Iface linkstate %s %s", name,
             (isUp ? "up" : "down"));

    mNm->getBroadcaster()->sendBroadcast(ResponseCode::InterfaceChange,
            msg, false);
}

void NetlinkHandler::notifyQuotaLimitReached(const char *name, const char *iface) {
    char msg[255];
    snprintf(msg, sizeof(msg), "limit alert %s %s", name, iface);

    mNm->getBroadcaster()->sendBroadcast(ResponseCode::BandwidthControl,
            msg, false);
}

void NetlinkHandler::notifyInterfaceClassActivity(const char *name,
                                                  bool isActive) {
    char msg[255];

    snprintf(msg, sizeof(msg), "IfaceClass %s %s",
             isActive ? "active" : "idle", name);
    ALOGV("Broadcasting interface activity msg: %s", msg);
    mNm->getBroadcaster()->sendBroadcast(
        ResponseCode::InterfaceClassActivity, msg, false);
}

void NetlinkHandler::notifyAddressChanged(int action, const char *addr,
                                          const char *iface, const char *flags,
                                          const char *scope) {
    char msg[255];
    snprintf(msg, sizeof(msg), "Address %s %s %s %s %s",
             (action == NetlinkEvent::NlActionAddressUpdated) ?
             "updated" : "removed", addr, iface, flags, scope);

    mNm->getBroadcaster()->sendBroadcast(ResponseCode::InterfaceAddressChange,
            msg, false);
}

void NetlinkHandler::notifyMOFlagChanged(int action,
                                          const char *iface, const char *flags) {
    char msg[48];
    snprintf(msg, sizeof(msg), "MOFlag %s %s", iface, flags);

    mNm->getBroadcaster()->sendBroadcast(ResponseCode::InterfaceMOFlagChange,
            msg, false);
}

void NetlinkHandler::increaseEthRoutePriorityV6(const char *route, const char *gateway,
                                                const char *iface, const char *priority)
{
    char routeStr[MAX_ROUTE_LNE] = "default";
    int metric = PLANE_A_DEF_ROUTE_METRIC;
    char metricStr[4] = ""; // use metric 128 and 138, thus set length as 4
    if (route == NULL || gateway == NULL || iface == NULL) {
        return;
    }
    if (strncmp(ETH_INTERFACE_MATCHES, iface, strlen(ETH_INTERFACE_MATCHES)) != 0) {
        // only handle ethernet route
        return;
    }
    if (NULL != strstr(iface, ".")) { // vlan
        char vlanDefRouteProp[PROPERTY_VALUE_MAX] = "false";
        property_get("persist.vlan.def.route.on", vlanDefRouteProp, "false");
        if (!strcmp(vlanDefRouteProp, "true")) {
            metric = PLANE_B_DEF_ROUTE_METRIC;
        } else {
            return;
        }
    }
    sprintf(metricStr, "%d", metric);
    if (strcmp(LOCAL_DEFAULT_ROUTE_V6, route)) { // has route
        strncpy(routeStr, route, sizeof(routeStr));
    }
    ALOGE("increaseEthRoutePriorityV6: route = %s, gateway = %s, iface = %s, priority = %s, metric = %s",
          routeStr, gateway, iface, priority, metricStr);
    RunRouteCmd(gateway, routeStr, iface, priority, metricStr);
}

void RunRouteCmd(const char *gateway, const char *routeStr, const char *iface,
                 const char *priority, const char *metricStr)
{
    int res;
    if (!strcmp("", gateway)) { // no gateway
        const char *route6_del_cmd[] = {"/system/bin/ip", "-6", "route", "del",
                                        routeStr, "dev", iface, "metric", priority};
        const char *route6_cmd[] = {"/system/bin/ip", "-6", "route", "add",
                                    routeStr, "dev", iface, "metric", metricStr};
        res = android_fork_execvp((sizeof(route6_del_cmd) / sizeof(*route6_del_cmd)),
                                  (char **)route6_del_cmd, NULL, false, false);
        res = android_fork_execvp((sizeof(route6_cmd) / sizeof(*route6_cmd)),
                                  (char **)route6_cmd, NULL, false, false);
    } else { // has gateway
        const char *route6_del_cmd[] = {"/system/bin/ip", "-6", "route", "del",
                                        routeStr, "via", gateway, "dev", iface, "metric", priority};
        const char *route6_cmd[] = {"/system/bin/ip", "-6", "route", "add",
                                    routeStr, "via", gateway, "dev", iface, "metric", metricStr};
        res = android_fork_execvp((sizeof(route6_del_cmd) / sizeof(*route6_del_cmd)),
                                  (char **)route6_del_cmd, NULL, false, false);
        res = android_fork_execvp((sizeof(route6_cmd) / sizeof(*route6_cmd)),
                                  (char **)route6_cmd, NULL, false, false);
    }
    if (res < 0) {
        SLOGE("increaseEthRoutePriorityV6 failed!\n");
    }
}
