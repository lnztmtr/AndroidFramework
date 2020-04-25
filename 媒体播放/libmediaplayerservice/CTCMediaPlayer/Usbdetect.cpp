/**
 * @addtogroup module_genericGateway
 * @{
 */

/**
 * @file
 * @brief USB控制器，管理USB插拔及挂载。
 * @details
 * @version 1.0.0
 * @author sky.houfei
 * @date 2016-3-18
 */

//******************************************************
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <sys/un.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <linux/types.h>
#include <linux/netlink.h>
#include <errno.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include "UsbController.h"
#include "GenericGateway.h"

//******************************************************
#define UEVENT_BUFFER_SIZE 2048

//******************************************************
static bool isUsbConnected = false;
static int s_hotplugSock = 0;
static bool s_isMounted = false;

 //******************************************************
static void* UsbController_HotPlugMonitor(void);  // USB监听，监听USB的插拔事件，并进行挂载和卸载USB设备。


//******************************************************
static int UsbController_HotplugSockInit(void)
{
    const int buffersize = 1024;
    int ret;

    struct sockaddr_nl snl;
    bzero(&snl, sizeof(struct sockaddr_nl));
    snl.nl_family = AF_NETLINK;
    snl.nl_pid = getpid();
    snl.nl_groups = 1;

    int s = socket(PF_NETLINK, SOCK_DGRAM, NETLINK_KOBJECT_UEVENT);
    if (s == -1)
    {
        perror("socket");
        return -1;
    }
    setsockopt(s, SOL_SOCKET, SO_RCVBUF, &buffersize, sizeof(buffersize));

    ret = bind(s, (struct sockaddr *)&snl, sizeof(struct sockaddr_nl));
    if (ret < 0)
    {
        perror("bind");
        close(s);
        return -1;
    }

    return s;
}


/**
 * @brief USB控制器初始化，准备USB的监听服务。
 * @return ret, int，如果初始化成功，则返回0，否则为-1.
 */
int UsbController_Init(void)
{
    const int buffersize = 1024;
    int ret;
    pthread_t id;

    struct sockaddr_nl snl;
    bzero(&snl, sizeof(struct sockaddr_nl));
    snl.nl_family = AF_NETLINK;
    snl.nl_pid = getpid();
    snl.nl_groups = 1;

    if (access("/dev/sda1", 0) == 0)
    {
        // USB已经连接成功
        isUsbConnected = true;
    }


    UsbController_MountMonitor();   // 首次检查USB是否挂载
    s_hotplugSock = socket(PF_NETLINK, SOCK_DGRAM, NETLINK_KOBJECT_UEVENT);
    if (s_hotplugSock == -1)
    {
        perror("socket error");
        return -1;
    }
    setsockopt(s_hotplugSock, SOL_SOCKET, SO_RCVBUF, &buffersize, sizeof(buffersize));

    ret = bind(s_hotplugSock, (struct sockaddr *)&snl, sizeof(struct sockaddr_nl));
    if (ret < 0)
    {
        perror("bind error");
        close(s_hotplugSock);
        return -1;
    }

    ret = pthread_create(&id, NULL, UsbController_HotPlugMonitor, NULL);
    if (ret != 0)
    {
        printf("pthread_create error = %d\n", ret);
    }

    return 0;
}


/**
 * @brief USB监听热插拔，监听USB的插拔事件，并进行挂载和卸载USB设备。
 */
static void* UsbController_HotPlugMonitor(void)
{
    pthread_detach(pthread_self());
    char *result = NULL;
    char buf[UEVENT_BUFFER_SIZE * 2] = {0};     //  Netlink message buffer

    while (1)
    {
        recv(s_hotplugSock, &buf, sizeof(buf), 0); // 获取 USB 设备的插拔会出现字符信息，
        result = strtok(buf, "@");                // 查看 USB的插入还是拔出信息
        if (result != NULL)
        {
            if ((strcmp(result, "add") == 0))
            {
                if (isUsbConnected == false)
                {
                    isUsbConnected = true;
                }

            }
            else if ((strcmp(result, "remove") == 0))
            {
                if (isUsbConnected == true)
                {
                    isUsbConnected = false;
                }
            }
        }
        memset(buf, 0, UEVENT_BUFFER_SIZE * 2);
    }
}


/**
* @brief 是否连接成功。
* @return bool isConnnected, USB设备连接成功，则返回 true, 否则返回false。
*/
static bool UsbController_IsConnected(void)
{
    return isUsbConnected;
}


/**
 * @brief 挂载文件系统。
 * @details 创建文件夹 /tmp/usb，将USB设备挂在在该目录下。尝试挂在 sda1和sdb1，如果都挂在失败，则认为挂载失败。
 * @return 如果挂载成功，则返回0，否则为-1。
 */
static int UsbController_MountFileSystem(void)
{
    const char directory[] = "/tmp/usb";
    int ret = 0;

    printf("Try to mount the usb device\n");
    // 检测是否存在文件夹
    if (access(directory, 0) == -1)
    {
        // 文件夹不存在
        if (mkdir(directory, 0777)) // 创建文件夹
        {
            printf("creat directory(%s) failed!!!", directory);
            return -1;
        }
    }

    if (system("mount -t vfat /dev/sda1  /tmp/usb") < 0)  // 挂载USB的文件系统
    {
        if (system("mount -t vfat /dev/sdb1  /tmp/usb") < 0)
        {
            return -1;
        }
    }

    return 0;
}


/**
 * @brief 卸载文件系统。
 * @return 如果挂载成功，则返回0，否则为-1。
 */
static int UsbController_UnmountFileSystem(void)
{
    int ret = 0;

    if (system("umount /tmp/usb") < 0)  // 挂载USB的文件系统
    {
        printf("Umount the usb device failed\n");
        ret =  -1;
    }

    printf("Umount the usb device success\n");
    return ret;
}


/**
 * @brief USB设备是否可以挂载。
 * @details 设备处于连接状态，且在/dev/目录下创建了sda1或者sdb1节点，则视为可以挂载。
 * @return 如果可以挂在，则返回true，否则为false。
 */
static bool UsbController_IsMountable(void)
{
    bool isMountable = false;
    bool isPartitionExist = false;

    if (access("/dev/sda1", 0) == 0 || access("/dev/sdb1", 0) == 0)
    {
        // 存在分区 /dev/sda1 或者 /dev/sdb1
        isPartitionExist = true;
    }

    if (isUsbConnected == true && isPartitionExist == true)
    {
        isMountable = true;
    }

    return isMountable;
}


/**
 * @brief USB设备挂载监听。
 * @details 如果USB之前没有挂载且当前可以挂载，则挂载。
 * \n 如果USB之前挂载成功，此时设备已经被拔出，则卸载。
 */
void UsbController_MountMonitor(void)
{
    if (s_isMounted == false && UsbController_IsMountable() == true)
    {
        // 之前没有挂载且当前可以挂载，挂载文件系统
        if (0 == UsbController_MountFileSystem())
        {
            printf("Mount success\n");
            s_isMounted = true;
            GenericGateway_SetUsbMounted(s_isMounted);
        }
    }
    else if (s_isMounted == true && UsbController_IsConnected() == false)
    {
        // 之前挂载成功，此时设备已经被拔出，卸载设备
        if (0 == UsbController_UnmountFileSystem())
        {
            s_isMounted = false;
            GenericGateway_SetUsbMounted(s_isMounted);
        }
    }
}


/**
* @brief 是否已经挂载成功。
* @return bool s_isMounted, USB设备挂载成功，则返回 true, 否则返回false。
*/
bool UsbController_IsMounted(void)
{
    return s_isMounted;
}


/** @} */

