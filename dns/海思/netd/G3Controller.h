
#ifndef _G3_CONTROLLER_H
#define _G3_CONTROLLER_H

#include <unistd.h>
#include <pthread.h>
#include <sysutils/SocketClient.h>
#include "hi_3g_intf.h"

class G3Controller;
struct connect_config{
        G3Controller *handler;
        char ifname[64];
        char user[64];
        char passwd[64];
        char apnName[64];
        char dialNum[64];
        char pinCode[64];
        char pukCode[64];
        char country[64];
    };


class G3Controller {
    public:
        G3Controller();
        virtual ~G3Controller();

        int startG3AutoConnect(SocketClient *cli);
        int startG3ManualConnect(SocketClient *cli, const char *ifname,
                                 const char *user, const char *passwd,
                                 const char *apnName,const char *dialNum,
                                 const char *pinCode);
        int stopG3Connect(SocketClient *cli);

        int autoConnect();
        int manualConnect( const char *ifname, const char *user,
                       const char *passwd,const char *apnName,
                       const char *dialNum, const char *pinCode);
        int disconnect();

    private:
        int getApnPhone(char *currentOperator, const char *xmlFilePath,
                        struct connect_config *arg);
        int setPdp(HI_3G_CARD_S *card, char *apn, int apnSize,
                        char *telephone, int teleSize);

        void showQuality(HI_3G_CARD_S *card);
        int  initCard(HI_3G_CARD_S *card);
        int  scanCard(HI_3G_CARD_S *card, int max_num, int *card_num);
        int  connectCard(HI_3G_CARD_S *card, char *account,
                         char *password, char *telephone);

        static void *maunalConnectThread(void *arg);
        static void *autoConnectThread(void *arg);
    private:
        HI_3G_CARD_S *mConnectedCard;
        bool mThreadIsRuning;
        pthread_t mThread;
        SocketClient *mAutoClient;
        SocketClient *mManualClient;
        SocketClient *disconnectClient;
        struct connect_config *mConfig;
        pthread_mutex_t mMutex;
        pthread_cond_t  mCond;
};

#endif
