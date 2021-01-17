#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <cutils/properties.h>
#define LOG_TAG "G3Controller"
#include <cutils/log.h>
#include "ResponseCode.h"
#include "G3Controller.h"
#include "mxml.h"

#define DEBUG true
#define MAX_CMD_RETRIES          7
#define CMD_SUPPORT_LITE         0 /* for CDMA cards */
#define CMD_SUPPORT_FULL         1 /* for WCDMA cards */

#define WRONG_PASSWARD           1
#define CONNECT_FAIL             2
#define CONNECTING               3
#define DISCONNECTING            4
#define ENOCARD                  -4
#define CONN_WAIT_TIME           500000
#define CONN_WAIT_TRYS           60
#define PROPERTY_MAX_WAIT_TIME   30
#define NETINFO_SIZE             64
#define XML_FILE_PATH            "/etc/networkprovider.xml"

#define CHECK_AND_FREE_POINTER(ptr)        \
        do {                               \
            if(ptr) {                      \
                free(ptr);                 \
                ptr = NULL;                \
            }                              \
        } while(0)

#define CHECK_AND_SET_NULL(ptr)            \
        do {                               \
            if (!strcmp((ptr), "null"))    \
                *(ptr) = '\0';             \
        } while(0)

#define ERR  "3G error:"
#define WARN "3G warning:"

#define CHECK_NULL_POINTER(ptr, ret, msg)  \
        do {                               \
            if(!(ptr)){                    \
                ALOGD(ERR msg);            \
                return ret;                \
            }                              \
        } while(0)

G3Controller::G3Controller()
{
    mConnectedCard  = NULL;
    mAutoClient     = NULL;
    mManualClient   = NULL;
    mThreadIsRuning = false;
    mConfig         = NULL;
    pthread_mutex_init(&mMutex, NULL);
    pthread_cond_init(&mCond, NULL);
}

G3Controller::~G3Controller()
{
    CHECK_AND_FREE_POINTER(mConfig);
    pthread_mutex_destroy(&mMutex);
    pthread_cond_destroy(&mCond);
}

int G3Controller::getApnPhone(char *currentOperator, const char *xmlFilePath,
                              struct connect_config *config)
{
    int ret = -1;
    FILE *fp = NULL;
    mxml_node_t *root, *node;

    CHECK_NULL_POINTER(currentOperator, -1, "getApnPhone(null):currentOperator");
    CHECK_NULL_POINTER(xmlFilePath, -1, "getApnPhone(null):xmlFilePath");
    CHECK_NULL_POINTER(config, -1, "getApnPhone(null):config");

    memset(config, 0, sizeof(struct connect_config));

    fp = fopen(xmlFilePath, "r");
    if (!fp) {
        ALOGD(ERR "fopen(%s) failed", xmlFilePath);
        return -1;
    }

    root = mxmlLoadFile(NULL, fp, MXML_TEXT_CALLBACK);
    if (!root) {
        ALOGD(ERR "mxmlLoadFile error");
        goto fail;
    }

    node = mxmlFindElement(root, root, "NetworkProdiver", "provider",
                           currentOperator, MXML_DESCEND);
    if (node) {
        snprintf(config->user, sizeof(config->user),
                 "%s", mxmlElementGetAttr(node, "user"));
        snprintf(config->passwd, sizeof(config->passwd),
                 "%s", mxmlElementGetAttr(node, "passwd"));
        snprintf(config->apnName, sizeof(config->apnName),
                 "%s", mxmlElementGetAttr(node, "apn"));
        snprintf(config->dialNum, sizeof(config->dialNum),
                 "%s", mxmlElementGetAttr(node, "phone"));
        snprintf(config->pinCode, sizeof(config->pinCode),
                 "%s", mxmlElementGetAttr(node, "pin"));
        snprintf(config->pukCode, sizeof(config->pukCode),
                 "%s", mxmlElementGetAttr(node, "puk"));
        snprintf(config->country, sizeof(config->country),
                 "%s", mxmlElementGetAttr(node, "Country"));

        ALOGD("%-20s: %s\n", "NetworkProvider", currentOperator);
        ALOGD("%-20s: %s\n", "Country", config->country);
        ALOGD("%-20s: %s\n", "apn",     config->apnName);
        ALOGD("%-20s: %s\n", "phone",   config->dialNum);
        ALOGD("%-20s: %s\n", "user",    config->user   );
        ALOGD("%-20s: %s\n", "pin",     config->pinCode);
        ALOGD("%-20s: %s\n", "puk",     config->pukCode);

        ret = 0;
    } else {
        ALOGD(ERR "mxmlFindElement %s error", currentOperator);
    }

fail:
    fclose(fp);
    return ret;
}

int G3Controller::setPdp(HI_3G_CARD_S *card, char *apn, int apnSize,
                         char *telephone, int teleSize)
{
    int ix = 0;
    int ret = -1;
    HI_3G_PDP_S *pdp = NULL; /* need to initialize, or free may be error */
    HI_3G_CARD_OPERATOR_S *curOperator = NULL; /* need to initialize */

    ALOGD("Begin setPdp\n");

    CHECK_NULL_POINTER(card, -1, "setPdp(null):card");
    CHECK_NULL_POINTER(apn, -1, "setPdp(null):apn");
    CHECK_NULL_POINTER(telephone, -1, "setPdp(null):telephone");

    curOperator = (HI_3G_CARD_OPERATOR_S *)malloc(sizeof(HI_3G_CARD_OPERATOR_S));
    if (curOperator == NULL) {
        ALOGD(ERR "malloc memory for currentoperator in setPdp failed\n");
        ret = -1;
        goto pdp_err;
    }
    memset(curOperator, 0, sizeof(HI_3G_CARD_OPERATOR_S));

    do {
        ret = HI_3G_GetCurrentOperator(card, curOperator);
        if (ret == 0)
            break;

        sleep(1);
    } while (++ix < MAX_CMD_RETRIES);

    if (ix >= MAX_CMD_RETRIES) {
        ALOGD(ERR "get card operator failed(%d)!\n", ret);
        goto pdp_err;
    }

    mConfig = (struct connect_config *)malloc(sizeof(struct connect_config));
    if (!mConfig) {
        ALOGD(ERR "malloc memory for connect_config failed\n");
        ret = -1;
        goto pdp_err;
    }

    ALOGD("getApnPhone(%s, %s)", curOperator->aoperatorinfo, XML_FILE_PATH);

    ret = getApnPhone(curOperator->aoperatorinfo, XML_FILE_PATH, mConfig);
    if (ret != 0) {
        ALOGD(ERR "getApnPhone() failed!\n");
        goto pdp_err;
    }

    snprintf(apn, apnSize, "%s", mConfig->apnName);
    snprintf(telephone, teleSize, "%s", mConfig->dialNum);

    ALOGD("%-15s:%s\n"
          "%-15s:%s\n"
          "%-15s:%s\n",
          "operatorinfo", curOperator->aoperatorinfo,
          "apn",apn,
          "telephone", telephone);

    ret = HI_3G_SetApn(card, apn);
    if (ret != 0) {
        ALOGD(ERR "HI_3G_SetApn failed, ret:%d\n", ret);
        goto pdp_err;
    }

    pdp = (HI_3G_PDP_S *)malloc(sizeof(HI_3G_PDP_S));
    if (pdp == NULL) {
        ALOGD(ERR "malloc pdp failed!\n");
        ret = -1;
        goto pdp_err;
    }

    ret = HI_3G_GetApn(card, pdp);
    if (ret != 0) {
        ALOGD(ERR "HI_3G_GetApn failed, ret:%d\n", ret);
        goto pdp_err;
    } else{
        ALOGD("get current pdp successfully:\n"
              "%-13s: %s\n"
              "%-13s: %s\n"
              "%-13s: %s\n"
              "%-13s: %s\n"
              "%-13s: %s\n"
              "%-13s: %s\n",
              "cid",        pdp->acid,
              "pdp_type",   pdp->apdptype,
              "apn",        pdp->aapn,
              "pdp_ipaddr", pdp->apdpipaddr,
              "d_comp",     pdp->adcomp,
              "h_comp",     pdp->ahcomp);
    }

    ALOGD("setPdp Sucess!\n");

pdp_err:
    CHECK_AND_FREE_POINTER(curOperator);
    CHECK_AND_FREE_POINTER(pdp);
    CHECK_AND_FREE_POINTER(mConfig);
    if (ret != 0)
        ALOGD(ERR "setPdp Failed!\n");
    return ret;
}


void G3Controller::showQuality(HI_3G_CARD_S *card)
{
    int issr;
    int ber;
    int ret;

    CHECK_NULL_POINTER(card, , "showQuality(null)");

    ret = HI_3G_GetQuality(card, &issr, &ber);
    if (ret != 0)
        ALOGD(ERR "showQuality failed,ret:%d\n", ret);
    else {
        ALOGD("3G: showQuality success\n"
              "%-6s: %d\n"
              "%-6s: %d\n",
              "issr", issr,
              "ber",  ber);
    }
}

int G3Controller::initCard(HI_3G_CARD_S *card)
{
    int ret = -1;
    int ix = 0;

    CHECK_NULL_POINTER(card, -1, "initCard(null)");

    /* for HUAWEI E303s, should try several times, make sure init card ok*/
    do {
        ret = HI_3G_InitCard(card);
        if (!ret)
            break;

        sleep(1);
    } while (++ix < MAX_CMD_RETRIES);

    if (ix >= MAX_CMD_RETRIES) {
        ALOGD(ERR "initCard failed,ret:%d, retry:%d\n", ret, ix);
        return -1;
    }

    ALOGD("3G: initCard Success, retry times:%d\n", ix);
    ALOGD(" %-20s: %s \n","Card amanufacturer", card->amanufacturer);
    ALOGD(" %-20s: %s \n","Card aproductname", card->aproductname);
    ALOGD(" %-20s: %s \n","Card adevice", card->adevice);
    ALOGD(" %-20s: %s \n","Card apcui", card->apcui);

    return 0;
}

int G3Controller::connectCard(HI_3G_CARD_S *card, char *account,
                              char *password, char *telephone)
{
    int ix  = 0;
    int ret = -1;
    int max_reconnect = 1;
    char *argoption[] = {NULL};

    CHECK_NULL_POINTER(card, -1, "connectCard(null)");

    do {
        ret = HI_3G_ConnectCard(card, account, password,
                  telephone, 0, argoption);
        if (ret == 0)
            break;

        //sleep(1);
    } while (++ix < max_reconnect);

    if (ix >= max_reconnect) {
        ALOGD(ERR "connect card failed, ret:%d\n", ret);
        return -1;
    }

    ALOGD("3G: g3_connect_card success");
    return 0;
}

int G3Controller::scanCard(HI_3G_CARD_S *card, int max_num, int *card_num)
{
    int ret = -1;
    int ix = 0;

    CHECK_NULL_POINTER(card, -1, "scanCard(null):card");
    CHECK_NULL_POINTER(card_num, -1, "scanCard(null):card_num");

    do {
        ret = HI_3G_ScanCard(card, max_num, card_num);
        if (!ret)
            return 0;

        sleep(1);
    } while (++ix < MAX_CMD_RETRIES);

    if (ix >= MAX_CMD_RETRIES) {
        ALOGD(ERR "scanCard failed,ret:%d, retry:%d\n", ret, ix);
    }

    return ret;
}

int G3Controller::startG3AutoConnect(SocketClient *cli)
{
    char *msg = NULL;

    CHECK_NULL_POINTER(cli, -1, "startG3AutoConnect(null):cli");

    if (mThreadIsRuning) {
        asprintf(&msg, "G3 status: already_runing");

        ALOGD("startG3AutoConnect: SND:%s", msg);
        cli->sendMsg(ResponseCode::G3StatusResult, msg, false);

        CHECK_AND_FREE_POINTER(msg);
        return -1;
    }

    mAutoClient = cli;
    mAutoClient->incRef();

    if (pthread_create(&mThread, NULL, autoConnectThread, (void *)this)) {
        ALOGE("startG3AutoConnect: pthread_create failed: %s", strerror(errno));
        return -1;
    }

    ALOGD("startG3AutoConnect: success");
    return 0;
}

void* G3Controller::autoConnectThread(void *arg)
{
    char *msg = NULL;
    int rc = -1;

    CHECK_NULL_POINTER(arg, NULL, "autoConnectThread(null)");

    G3Controller *handler = reinterpret_cast<G3Controller *>(arg);
    handler->mThreadIsRuning = true;
    rc = handler->autoConnect();

    if (!rc) {
        asprintf(&msg, "G3 status: success");
    } else if(rc == WRONG_PASSWARD) {
        asprintf(&msg, "G3 status: wrong_passwd");
    } else if(rc == ENOCARD) {
        asprintf(&msg, "G3 status: no_card");
    } else {
        asprintf(&msg, "G3 status: failed");
    }
    ALOGD("autoConnect:%s, ret:%d", msg, rc);

    if (handler->mAutoClient) {
        handler->mAutoClient->sendMsg(ResponseCode::G3StatusResult, msg, false);
        ALOGD("autoConnectThread: SND: %s", msg);
        handler->mAutoClient->decRef();
        handler->mAutoClient = NULL;
    }

    if (rc == 0) {
        ALOGD("autoConnectThread: connect success, wait to disconnect");

        pthread_mutex_lock(&(handler->mMutex));
        pthread_cond_wait(&(handler->mCond), &(handler->mMutex));
        pthread_mutex_unlock(&(handler->mMutex));
        //while (handler->mIsContinue)
        //    sleep(1);

        handler->disconnect();
    } else
        ALOGD("autoConnectThread: connect failed, pthread_exit()");

    CHECK_AND_FREE_POINTER(msg);
    handler->mThreadIsRuning = false;

    pthread_exit(NULL);

    return NULL;
}

int G3Controller::stopG3Connect(SocketClient *cli)
{
    CHECK_NULL_POINTER(cli, -1, "stopG3Connect(null)");

    disconnectClient = cli;
    disconnectClient->incRef();
    //mIsContinue = false;
    pthread_mutex_lock(&mMutex);
    pthread_cond_broadcast(&mCond);
    pthread_mutex_unlock(&mMutex);

    char *msg = NULL;
    if ((mThreadIsRuning == false) && cli) {
        asprintf(&msg, "G3 status: success");
        ALOGD("stopG3Connect: SND: %s", msg);

        cli->sendMsg(ResponseCode::G3StatusResult, msg, false);
        cli->decRef();
        CHECK_AND_FREE_POINTER(msg);
    }

    return 0;
}

int G3Controller::startG3ManualConnect(SocketClient *cli, const char *ifname,
                   const char *user, const char *passwd, const char *apnName,
                   const char *dialNum, const char *pinCode)
{
    CHECK_NULL_POINTER((cli && ifname && user && passwd
       && apnName && dialNum && pinCode), -1, "startG3ManualConnect(null)");

    if (mThreadIsRuning) {
        char *msg = NULL;

        asprintf(&msg, "G3 status: already_runing");
        ALOGD("startG3ManualConnect: SND: %s", msg);

        cli->sendMsg(ResponseCode::G3StatusResult, msg, false);

        CHECK_AND_FREE_POINTER(msg);
        return -1;
    }

    mConfig = (struct connect_config *)malloc(sizeof(struct connect_config));
    if (!mConfig) {
        ALOGD(ERR "malloc failed in startG3ManualConnect");;
        return -1;
    }
    memset(mConfig, 0, sizeof(struct connect_config));

    mConfig->handler = this;
    snprintf(mConfig->ifname,  sizeof(mConfig->ifname),  "%s", ifname);
    snprintf(mConfig->user,    sizeof(mConfig->user),    "%s", user);
    snprintf(mConfig->passwd,  sizeof(mConfig->passwd),  "%s", passwd);
    snprintf(mConfig->apnName, sizeof(mConfig->apnName), "%s", apnName);
    snprintf(mConfig->dialNum, sizeof(mConfig->dialNum), "%s", dialNum);
    snprintf(mConfig->pinCode, sizeof(mConfig->pinCode), "%s", pinCode);

    CHECK_AND_SET_NULL(mConfig->ifname);
    CHECK_AND_SET_NULL(mConfig->user);
    CHECK_AND_SET_NULL(mConfig->passwd);
    CHECK_AND_SET_NULL(mConfig->apnName);
    CHECK_AND_SET_NULL(mConfig->dialNum);
    CHECK_AND_SET_NULL(mConfig->pinCode);

    mManualClient = cli;
    mManualClient->incRef();

    if (pthread_create(&mThread, NULL, maunalConnectThread, (void *)mConfig)) {
        ALOGE(ERR "startG3ManualConnect: pthread_create (%s)", strerror(errno));
        return -1;
    }

    ALOGD("startG3ManualConnect: create a new thread to connect 3G success");
    return 0;
}

void *G3Controller::maunalConnectThread(void *arg)
{
    char *msg = NULL;
    int rc = -1;

    CHECK_NULL_POINTER(arg, NULL, "maunalConnectThread(null)");

    struct connect_config *obj = reinterpret_cast<struct connect_config *>(arg);
    G3Controller *handler = obj->handler;

    handler->mThreadIsRuning = true;

    rc = handler->manualConnect(obj->ifname,  obj->user,    obj->passwd,
                                obj->apnName, obj->dialNum, obj->pinCode);

    if (!rc) {
        asprintf(&msg, "G3 status: success");
    } else if(rc == WRONG_PASSWARD) {
        asprintf(&msg, "G3 status: wrong_passwd");
    } else if(rc == ENOCARD) {
        asprintf(&msg, "G3 status: no_card");
    }else {
        asprintf(&msg, "G3 status: failed");
    }
    ALOGD("maunalConnectThread: %s, ret:%d", msg, rc);

    if (handler->mManualClient) {
        int r = handler->mManualClient->sendMsg(ResponseCode::G3StatusResult,
                                                msg, false);
        ALOGD("maunalConnectThread: SND: %s, status:%d", msg, r);
        handler->mManualClient->decRef();
        handler->mManualClient = NULL;
    }

    if (rc == 0) {
        ALOGD("maunalConnectThread:connect success, waiting to disconnect");
        pthread_mutex_lock(&(handler->mMutex));
        pthread_cond_wait(&(handler->mCond), &(handler->mMutex));
        pthread_mutex_unlock(&(handler->mMutex));

        handler->disconnect();
        ALOGD("maunalConnectThread disconnected, pthread_exit()");
    } else
        ALOGD("maunalConnectThread connect failed, pthread_exit()");

    CHECK_AND_FREE_POINTER(msg);
    CHECK_AND_FREE_POINTER(obj);

    handler->mThreadIsRuning = false;
    pthread_exit(NULL);

    return NULL;
}

int G3Controller::autoConnect()
{
    int ret = -1;
    int card_num      = 0;
    char account[NETINFO_SIZE]   = {0};
    char password[NETINFO_SIZE]  = {0};
    char telephone[NETINFO_SIZE] = "*99#";
    char apn[NETINFO_SIZE]       = "3gnet";

    HI_3G_CARD_S *card;
    HI_3G_CARD_STATE_E status = HI_3G_CARD_STATUS_UNAVAILABLE;

    card = (HI_3G_CARD_S *)malloc(sizeof(HI_3G_CARD_S) * MAX_CARD_NUM);
    if (card == NULL) {
        ALOGD(ERR "malloc memory failed in autoConnect()\n");
        return -1;
    }

    ret = scanCard(card, (int)MAX_CARD_NUM, &card_num);
    if (ret != 0) {
        ALOGD(ERR "scan card failed in autoConnect(),ret:%d\n", ret);
        goto auto_err;
    }
    ALOGD("Scan cards Success!\n");

    ret = initCard(card);
    if(ret != 0)
        goto auto_err;

    ret = HI_3G_GetCardStatus(card, &status);
    if (ret != 0) {
        ALOGD(ERR "get card status failed in autoConnect,ret:%d\n", ret);
        goto auto_err;
    }

    if (status != HI_3G_CARD_STATUS_DISCONNECTED) {
        ALOGD("autoConnect: card status is not ready,status:%d, should be %d\n",
               status, HI_3G_CARD_STATUS_DISCONNECTED);
        ret = -1;
        goto auto_err;
    }
    ALOGD("Get card status Success!\n");

    ret = setPdp(card, apn, sizeof(apn), telephone, sizeof(telephone));
    if (ret != 0)
        goto auto_err;

    showQuality(card);
    ALOGD("Begin Connect card\n");

    /* try several times, make sure to connect ok */
    ret = connectCard(card, account, password, telephone);
    if (ret != 0)
        goto auto_err;

    if (card) {
        mConnectedCard = card;
        card = NULL;
    }

    return 0;

auto_err:
    HI_3G_DisConnectCard(card); /* pppd may cause scan card failed */
    HI_3G_DeInitCard(card);
    CHECK_AND_FREE_POINTER(card);

    return ret;
}

int G3Controller::manualConnect( const char *ifname,  const char *user,
                                 const char *passwd,  const char *apnName,
                                 const char *dialNum, const char *pinCode)
{
    int  ret = -1;
    int  card_num = 0;
    char account[NETINFO_SIZE] = {0};
    char password[NETINFO_SIZE] = {0};
    char telephone[NETINFO_SIZE] = "";
    char apn[NETINFO_SIZE] = "";

    HI_3G_CARD_S *card;
    HI_3G_CARD_STATE_E status = HI_3G_CARD_STATUS_UNAVAILABLE;

    CHECK_NULL_POINTER((ifname && user && passwd && apnName
                      && dialNum && pinCode), -1, "manualConnect(null)");

    snprintf(telephone, sizeof(telephone), "%s", dialNum);
    snprintf(account, sizeof(account), "%s", user);
    snprintf(password, sizeof(password), "%s", passwd);
    snprintf(apn, sizeof(apn), "%s", apnName);

    ALOGD("%-10s: %s \n","telephone",telephone);
    ALOGD("%-10s: %s \n","account",account);
    ALOGD("%-10s: %s \n","password",password);
    ALOGD("%-10s: %s \n","apn",apn);

    card = (HI_3G_CARD_S *)malloc(sizeof(HI_3G_CARD_S) * MAX_CARD_NUM);
    if (card == NULL) {
        ALOGD(ERR "malloc memory failed in manualConnect\n");
        return -1;
    }

    ret = scanCard(card, (int)MAX_CARD_NUM, &card_num);
    if (ret) {
        ALOGD(ERR "scan card failed in manualConnect, ret:%d\n", ret);
        goto manual_err;
    }

    ret = initCard(card);
    if (ret != 0) {
        ALOGD(ERR "g3_init_card failed in manualConnect, ret:%d\n", ret);
        goto manual_err;
    }

    ret = HI_3G_GetCardStatus(card, &status);
    if (ret) {
        ALOGD(ERR "HI_3G_GetCardStatus failed in manualConnect, ret:%d\n", ret);
        goto manual_err;
    }

    if (status != HI_3G_CARD_STATUS_DISCONNECTED) {
        ALOGD("manualConnect: card status is not ready, status:%d, should be %d\n",
               status, HI_3G_CARD_STATUS_DISCONNECTED);
        ret = -1;
        goto manual_err;
    }
    ALOGD("3G: Get card status Success!\n");

    //ret = g3_do_pdp(card, apn, sizeof(apn), telephone, sizeof(telephone), false);
    //    if(ret != 0) goto manual_err;

    showQuality(card);
    ALOGD("3G: Begin Connect card\n");

    /* try several times, make sure to connect ok */
    if ((ret = connectCard(card, account, password, telephone)) != 0)
        goto manual_err;

    /* don't free card when connected success, free it when call g3_disconnect,
       because HI_3G_InitCard may cost 6 seconds in g3_disconnect, reserve card
       can improve efficiency */
    if (card) {
        mConnectedCard = card;
        card = NULL;
    }
    return 0;

manual_err:
    HI_3G_DisConnectCard(card); /* pppd may cause scan card failed */
    HI_3G_DeInitCard(card);
    CHECK_AND_FREE_POINTER(card);

    return ret;
}

int G3Controller::disconnect()
{
    int   ret = -1;
    int   card_num = 0;
    char  *msg = NULL;
    HI_3G_CARD_S *card;
    HI_3G_CARD_STATE_E status = HI_3G_CARD_STATUS_UNAVAILABLE;

    if (mConnectedCard) {
        ret = HI_3G_DisConnectCard(mConnectedCard);

        ALOGD("disconnect: HI_3G_DisConnectCard ret=%d",ret);
        HI_3G_DeInitCard(mConnectedCard);

        CHECK_AND_FREE_POINTER(mConnectedCard);

        /* rc!=0 may caused by card removed, alse return success */
        if (disconnectClient) {
            asprintf(&msg, "G3 status: success");
            int r = disconnectClient->sendMsg(ResponseCode::G3StatusResult,
                                                    msg, false);
            ALOGD("disconnect: SND: %s,status:%d, ret:%d, \
                   use reserved handler", msg, r, ret);
            CHECK_AND_FREE_POINTER(msg);
            disconnectClient->decRef();
            disconnectClient = NULL;
        }
        return 0;
    }

    ALOGD("3G: init a new card to disconnect");
    card = (HI_3G_CARD_S *)malloc(sizeof(HI_3G_CARD_S) * MAX_CARD_NUM);
    if (card == NULL) {
        ALOGD("disconnect: malloc memory for cards failed!\n");
        return -1;
    }

    ret = scanCard(card, (int)MAX_CARD_NUM, &card_num);
    if (ret) {
        ALOGD(ERR "scanCard failed in disconnect(),ret:%d\n", ret);
        goto discon_err;
    }
    ALOGD("3G: Scan cards Success!\n");

    if (initCard(card))
        goto discon_err;

    ret = HI_3G_DisConnectCard(card);
    if (ret)
        goto discon_err;

discon_err:

    if (disconnectClient) {
        if (ret == 0)
            asprintf(&msg, "G3 status: success");
        else
            asprintf(&msg, "G3 status: failed");

        int r = disconnectClient->sendMsg(ResponseCode::G3StatusResult,
                                          msg, false);
        ALOGD("disconnect: SND: %s send status:%d, ret:%d", msg, r, ret);
        CHECK_AND_FREE_POINTER(msg);
        disconnectClient->decRef();
        disconnectClient = NULL;
    }

    HI_3G_DeInitCard(card);
    CHECK_AND_FREE_POINTER(card);
    CHECK_AND_FREE_POINTER(msg);

    return ret;
}
