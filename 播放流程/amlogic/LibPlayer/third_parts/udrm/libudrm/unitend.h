/*
 * Copyright Unitend Technologies Inc. 
 * 
 * This file and the information contained herein are the subject of copyright
 * and intellectual property rights under international convention. All rights
 * reserved. No part of this file may be reproduced, stored in a retrieval
 * system or transmitted in any form by any means, electronic, mechanical or
 * optical, in whole or in part, without the prior written permission of Unitend 
 * Technologies Inc.
 *
 * File name: unitend.h,
 * Author: baihuisheng
 * Version:0.0.0.1
 * Date:2009-07-17
 * Description:this file define basic data type for UTI
 * History:
 *         Date:2009-07-17    Author:baihuisheng    Modification:Creation
 */
 
#ifndef _UNITEND_H_
#define _UNITEND_H_

#define IN
#define OUT
#define INOUT

#define K   (1024)
#define M   (1024*1024)

#ifndef NULL
#define NULL 0
#endif

/************************************************************/
/*                                                          */
/*  below is basic DATA type define for UNITEND co.ltd           */
/*                                                          */
/************************************************************/

typedef unsigned char            UTI_BYTE;    /*  range :  0 to 255                   */
typedef signed char              UTI_CHAR;    /*  range :  0 to 255 or -128 to 127    */
typedef signed long              UTI_LONG;    /*  range :  -2147483648 to 2147483647  */
typedef unsigned long            UTI_ULONG;   /*  range :  0 to 4294967295            */
typedef unsigned short           UTI_WORD;    /*  range :  0 to 65535                 */
typedef unsigned long            UTI_DWORD;	 /*  range :  0 to 4294967295            */

typedef unsigned char            UTI_UINT8;   /*  range :  0 to 255                   */
typedef signed char              UTI_SINT8;	 /*  range :  0 to 255 or -128 to 127    */

typedef unsigned short           UTI_UINT16;  /*  range :  0 to 65535                 */
typedef signed short             UTI_SINT16;  /*  range :  -32767 to 32767            */

typedef unsigned long            UTI_UINT32;   /*  range :  0 to 4294967295            */
typedef signed long              UTI_SINT32;  /*  range :  -2147483648 to 2147483647  */

typedef signed int               UTI_SINT;
typedef int                      UTI_INT;
typedef unsigned int             UTI_UINT;

typedef unsigned char            UTI_BOOL;    /*  range :  TRUE or FALSE           */
typedef void                     UTI_VOID;    /*  range :  n.a.                    */

typedef UTI_BYTE                 *UTI_PBYTE;
typedef UTI_CHAR                 *UTI_PCHAR;
typedef UTI_LONG                 *UTI_PLONG;
typedef UTI_ULONG                *UTI_PULONG;
typedef UTI_WORD                 *UTI_PWORD;
typedef UTI_DWORD                *UTI_PDWORD;

typedef UTI_UINT8                *UTI_PUINT8;
typedef UTI_SINT8                *UTI_PSINT8;

typedef UTI_UINT16               *UTI_PUINT16;
typedef UTI_SINT16               *UTI_PSINT16;

typedef UTI_UINT32               *UTI_PUINT32;
typedef UTI_SINT32               *UTI_PSINT32;

typedef UTI_INT                  *UTI_PINT;
typedef UTI_SINT                 *UTI_PSINT;

typedef UTI_BOOL                 *UTI_PBOOL;
typedef UTI_VOID                 *UTI_PVOID;

#ifndef UTI_TRUE
#define UTI_TRUE (UTI_BOOL) (1)
#endif

#ifndef UTI_FALSE
#define UTI_FALSE (UTI_BOOL) (!UTI_TRUE)
#endif

#ifndef UTI_NULL
#define UTI_NULL (UTI_PVOID)(0)
#endif



#define UDRM_LOG_OFF                               0
#define UDRM_LOG_CONSOLE                           1
#define UDRM_LOG_FILE                              2
#define UDRM_LOG_CONSOLE_FILE                      3

#define UDRM_LOG_ERROR                             0
#define UDRM_LOG_WARN                              1
#define UDRM_LOG_INFO                              2
#define UDRM_LOG_DEBUG                             3


/************************************************************************/
/*									*/
/*below is some common error for unitend software			*/
/*									*/
/*									*/
/************************************************************************/

typedef UTI_UINT32               UTI_UDRM_HANDLE;
typedef UTI_INT                  UTI_RESULT;


#define UDRM_ERROR_OK                                    0      // Success                                    成功
#define UDRM_ERROR_UNKNOWN_ERROR                         -1	    // Unknown error                              未知错误
#define UDRM_ERROR_ENCRYPT_FAILED                        -2	    // Encrypt failed                             加密错误
#define UDRM_ERROR_DECRYPT_FAILED                        -3	    // Decrypt failed                             解密错误
#define UDRM_ERROR_WAIT_TIMEOUT                          -4	    // Wait time out                              等待超时
#define UDRM_ERROR_INVALID_ARGUMENT                      -5	    // Argument is invalid                        参数错误
#define UDRM_ERROR_NOT_ENOUGH_MEMORY                     -6	    // Memory is not enough                       内容不足
#define UDRM_ERROR_NOT_ENOUGH_RESOURCE                   -7	    // Resource is not enough                     资源不足
#define UDRM_ERROR_NOT_SUPPORT                           -8	    // Not support                                功能不支持
#define UDRM_ERROR_DEVICE_NOT_PRESENT                    -9	    // Device is not present                      设备不存在
#define UDRM_ERROR_NOT_ENOUGH_BUFFER                     -10    // Buffer is not enough                       缓冲不足
#define UDRM_ERROR_CONNNECT_FAILED                       -11    // Connect server failed                      连接失败
#define UDRM_ERROR_UDRMCLIENT_NOT_EXIST                  -12    // DRM Client not exist                       设备未知
#define UDRM_ERROR_CONTENT_NOT_EXIST                     -13    // Content not exist                          内容不存在
#define UDRM_ERROR_CREDIT_NOT_ENOUGH                     -14    // Credit not enough                          资金不足
#define UDRM_ERROR_SYSTEM_BUSY                           -15    // System is busy                             系统正忙
#define UDRM_ERROR_DEVICE_INVALID                        -16    // Device is invalid                          设备无效
#define UDRM_ERROR_HMAC_ERROR                            -17    // HMAC error                                 HMAC错误
#define UDRM_ERROR_DICTATE_INVALID                       -18    // Dictate is invalid                         指令错误
#define UDRM_ERROR_SYSTEM_ERROR                          -19    // System error                               系统错误
#define UDRM_ERROR_TYPE_ERROR                            -20    // Type is error                              类型错误
#define UDRM_ERROR_MESSAGE_ERROR                         -21    // Message is error                           消息错误
#define UDRM_ERROR_INDEX_ERROR                           -22    // Index is error                             索引错误
#define UDRM_ERROR_DEVICE_CERT_EXIST                     -23    // Device cert exist                          设备证书存在
#define UDRM_ERROR_DEVICE_CERT_NOT_MATCH                 -24    // Device cert not match                      设备证书不匹配
#define UDRM_ERROR_CERT_OK                               -25    // Cert is OK                                 证书正常
#define UDRM_ERROR_CERT_NOT_EXIST                        -26    // Cert is not exist                          证书不存在
#define UDRM_ERROR_CERT_NOT_ISSUE                        -27    // Cert is not issue                          证书未颁发
#define UDRM_ERROR_CERT_SUSPEND                          -28    // Cert is suspend                            证书已停用
#define UDRM_ERROR_CERT_FORBID                           -29    // Cert is forbid                             证书已禁用
#define UDRM_ERROR_CERT_UPDATE                           -30    // Cert need update                           证书需更新
#define UDRM_ERROR_CERT_REVOKE                           -31    // Cert is revoke                             证书已撤销
#define UDRM_ERROR_CERT_EXPIRATION                       -32    // Cert is expiration                         证书已过期
#define UDRM_ERROR_CERT_VERIFY_ERROR                     -33    // Cert varify error                          证书验证错误
#define UDRM_ERROR_MANUAL_SETUP                          -34    // Manual setup                               手动配置
#define UDRM_ERROR_LICENSE_NOT_EXIST                     -35    // License is not exist                       许可证不存在
#define UDRM_ERROR_LICENSE_INVALID                       -36    // License is invalid                         许可证无效
#define UDRM_ERROR_LICENSE_UPDATE                        -37    // License need update                        许可证需更新
#define UDRM_ERROR_LICENSE_EXPIRATION                    -38    // License is expiration                      许可证已过期
#define UDRM_ERROR_CERTREQ_HAVE_NO_DEAL                  -39    // Cert request exist no deal                 证书申请已提交
#define UDRM_ERROR_DB_CONNECT_ERROR                      -40    // Database connect error                     数据库连接异常
#define UDRM_ERROR_DB_OPERATE_ERROR                      -41    // Database operate error                     数据库操作异常
#define UDRM_ERROR_ROOT_CERT_ERROR                       -42    // Root ca error                              根证书错误
#define UDRM_ERROR_ROOT_PKEY_ERROR                       -43    // Root private key error                     根私钥错误
#define UDRM_ERROR_GEN_CERT_ERROR                        -44    // Cert genereate error                       证书生成异常
#define UDRM_ERROR_MALLOC_ERROR                          -45    // Malloc or new error                        Malloc或New异常
#define UDRM_ERROR_DEVICE_OPERATE_ERROR                  -46    // Device operate error                       设备操作异常
#define UDRM_ERROR_MY_CERT_ERROR                         -47    // Myself cert error                          我的证书异常
#define UDRM_ERROR_SOAP_GEN_PAR_ERROR                    -48    // Soap parameter generate error              SOAP参数生成错误
#define UDRM_ERROR_SOAP_GEN_REQUEST_ERROR                -49    // Soap request generate error                SOAP请求生成错误
#define UDRM_ERROR_SOAP_PARSE_RESPONSE_ERROR             -50    // Soap response parse error                  SOAP响应解析错误
#define UDRM_ERROR_HTTP_REQUEST_ERROR                    -51    // HTTP request error                         HTTP请求异常
#define UDRM_ERROR_GEN_PKEY_ERROR                        -52    // Private key generate error                 私钥生成异常
#define UDRM_ERROR_GEN_CERTREQ_ERROR                     -53    // Cert request generate error                证书申请生成异常
#define UDRM_ERROR_DEVICE_NO_RIGHT                       -54    // Device no right                            设备无权限
#define UDRM_ERROR_CERT_TYPE_INVALID                     -55    // Cert type invalid                          证书类型无效
#define UDRM_ERROR_BOSS_CONTENT_NOT_EXIST                -56    // BOSS content not exist                     BOSS内容不存在
#define UDRM_ERROR_BOSS_DEVICE_NOT_EXIST                 -57    // BOSS device not exist                      BOSS设备不存在
#define UDRM_ERROR_BOSS_DEVICE_INVALID                   -58    // BOSS device invalid                        BOSS设备无效
#define UDRM_ERROR_BOSS_DEVICE_NO_RIGHT                  -59    // BOSS device no right                       BOSS设备无权限
#define UDRM_ERROR_BOSS_CREDIT_NOT_ENOUGH                -60    // BOSS credit not enough                     BOSS资金不足
#define UDRM_ERROR_BOSS_CONTENT_NO_RIGHT                 -61    // BOSS content no right                      BOSS内容无权限
#define UDRM_ERROR_BOSS_OPERATE_ERROR                    -62    // BOSS operate error                         BOSS操作异常
#define UDRM_ERROR_SEND_ERROR                            -63    // Send data error                            数据发送异常
#define UDRM_ERROR_RECV_ERROR                            -64    // Receive data error                         数据接收异常
#define UDRM_ERROR_SSL_NEW_ERROR                         -65    // SSL new error                              SSL创建失败
#define UDRM_ERROR_SSL_USE_CERT_ERROR                    -66    // SSL use cert error                         SSL加载证书失败
#define UDRM_ERROR_SSL_USE_PKEY_ERROR                    -67    // SSL use private key error                  SSL加载私钥失败
#define UDRM_ERROR_SSL_CONNECT_ERROR                     -68    // SSL connect error                          SSL连接失败
#define UDRM_ERROR_SSL_SERVER_CERT_INVALID               -69    // SSL server cert check error                SSL服务器证书验证错误
#define UDRM_ERROR_TIMESTAMP_ERROR                       -70    // Timestamp error                            时间戳错误
#define UDRM_ERROR_SIGNATURE_CERT_NO_TRUST               -71    // Certificate signature not trusted          证书签名不受信
#define UDRM_ERROR_SIGNATURE_PROCESS_ERROR               -72    // Security processing failed                 签名处理错误
#define UDRM_ERROR_CERTNAME_EXIST                        -73    // Cert name already exist                    证书名已存在
#define UDRM_ERROR_MY_PKEY_ERROR                         -74    // Myself private key error                   我的私钥异常
#define UDRM_ERROR_LOAD_CERT_ERROR                       -75    // Load cert or cert link error               证书加载异常
#define UDRM_ERROR_SSL_CERT_ERROR                        -76    // SSL connect cert error                     SSL连接证书错误
#define UDRM_ERROR_SSL_PKEY_ERROR                        -77    // SSL connect pkey error                     SSL连接私钥错误
#define UDRM_ERROR_DEVICE_ID_ERROR                       -78    // Device id error                            设备ID错误
#define UDRM_ERROR_DEVICE_TYPE_ERROR                     -79    // Device type error                          设备类型错误
#define UDRM_ERROR_DEVICE_NO_INIT                        -80    // Device no initial                          设备未初始化
#define UDRM_ERROR_DEVICE_BIND_NOT_MATCH                 -81    // Device bind not match                      设备绑定不匹配
#define UDRM_ERROR_BOSS_USERID_ERROR                     -82    // BOSS user id error                         BOSS用户不存在
#define UDRM_ERROR_BOSS_PASSWORD_ERROR                   -83    // BOSS user password error                   BOSS密码错误
#define UDRM_ERROR_DEVICE_USER_ERROR                     -84    // Device bind user error                     设备绑定用户错误
#define UDRM_ERROR_DEVICE_MAC_ERROR                      -85    // Device bind mac error                      设备绑定MAC错误
#define UDRM_ERROR_BOSS_USER_NO_RIGHT                    -86    // BOSS user no right                         BOSS用户无权限
#define UDRM_ERROR_BOSS_BIND_NUM_OVER                    -87    // BOSS bind number over                      BOSS绑定超限
#define UDRM_ERROR_BOSS_NO_RIGHT                         -88    // BOSS no right                              BOSS无权限
#define UDRM_ERROR_BOSS_NOT_SUPPORT                      -89    // BOSS not support                           BOSS功能不支持
#define UDRM_ERROR_BOSS_BIND_ERROR                       -90    // BOSS bind error                            BOSS绑定错误
#define UDRM_ERROR_BOSS_USERID_ALREADY_EXIST             -91    // BOSS user id already exist                 BOSS用户ID已存在
#define UDRM_ERROR_BOSS_USER_DEVICE_NOT_MATCH            -92    // BOSS user device not match                 BOSS用户设备不匹配
#define UDRM_ERROR_BOSS_VALIDATION_ERROR                 -93    // BOSS validation error                      BOSS验证码错误
#define UDRM_ERROR_BOSS_DEVICE_ID_ALREADY_EXIST          -94    // BOSS device id already exist               BOSS设备ID已存在
#define UDRM_ERROR_BOSS_DEVICE_TYPE_ERROR                -95    // BOSS device type error                     BOSS设备类型错误
#define UDRM_ERROR_BOSS_DEVICE_ALREADY_REGISTER          -96    // BOSS device already register               BOSS设备已注册
#define UDRM_ERROR_BOSS_DEVICE_ALREADY_BIND              -97    // BOSS device already bind                   BOSS设备已绑定
#define UDRM_ERROR_BOSS_CONTENT_ID_ALREADY_EXIST         -98    // BOSS content id already exist              BOSS内容ID已存在
#define UDRM_ERROR_BOSS_DB_OPERATE_ERROR                 -99    // BOSS db operate error                      BOSS数据库操作异常
#define UDRM_ERROR_BOSS_SERVICE_CONNECT_ERROR            -100   // BOSS service connect error                 BOSS服务连接异常
#define UDRM_ERROR_BOSS_SERVICE_OPERATE_ERROR            -101   // BOSS service operate error                 BOSS服务操作异常
#define UDRM_ERROR_BOSS_LICENSE_ID_ERROR                 -102   // BOSS license id error                      BOSS许可证ID错误
#define UDRM_ERROR_BOSS_USER_NOT_MATCH                   -103   // BOSS user not match                        BOSS用户不匹配
#define UDRM_ERROR_LICENSE_PLAYCOUNT_OVER                -104   // License play count over                    许可次数超限
#define UDRM_ERROR_LICENSE_PLAYTIME_OVER                 -105   // License play time over                     许可时长超限
#define UDRM_ERROR_LICENSE_PLAYPERIOD_OVER               -106   // License play period over                   许可时间段超限
#define UDRM_ERROR_LICENSE_HMAC_ERROR                    -107   // License HMAC error                         许可证HMAC错误
#define UDRM_ERROR_LICENSE_TIME_INVALID                  -108   // License time invalid                       许可证时间无效
#define UDRM_ERROR_LICENSE_CHECK_TIME_OVER               -109   // License check time over                    许可证检测时间超限
#define UDRM_ERROR_LICENSE_CONTENT_NOT_MATCH             -110   // License check content not match            许可内容不匹配
#define UDRM_ERROR_URL_ERROR                             -111   // URL error                                  URL错误
#define UDRM_ERROR_PROTOCOL_ERROR                        -112   // Protocol error                             协议错误
#define UDRM_ERROR_HOSTNAME_ERROR                        -113   // Host name error                            主机名错误
#define UDRM_ERROR_BAD_REQUEST                           -114   // 400 Bad Request                            400请求错误
#define UDRM_ERROR_SERVICE_TEMP_UNAVAILABLE              -115   // 503 Service Temporarily Unavailable        503服务暂时无效
#define UDRM_ERROR_USERID_EMPTY                          -116   // User id is empty                           用户ID为空
#define UDRM_ERROR_PASSWORD_EMPTY                        -117   // Password is empty                          密码为空
#define UDRM_ERROR_AGENT_SERVER_TIME_ERROR               -118   // Agent server time error                    DRM时间错误
#define UDRM_ERROR_GROUP_ID_ERROR                        -119   // Group id error                             组ID错误
#define UDRM_ERROR_GROUP_ID_NOT_EXIST                    -120   // Group id not exist                         组不存在
#define UDRM_ERROR_GROUP_ID_ALREADY_EXIST                -121   // Group id already exist                     组ID已存在
#define UDRM_ERROR_GROUP_NO_RIGHT                        -122   // Group no right                             组无权限
#define UDRM_ERROR_GROUP_NUM_OVER                        -123   // Group number over                          组用户超限
#define UDRM_ERROR_GROUP_CERT_ERROR                      -124   // Group cert error                           组证书错误
#define UDRM_ERROR_GROUP_PKEY_ERROR                      -125   // Group private key error                    组私钥错误
#define UDRM_ERROR_DEVICE_FILE_ERROR                     -126   // Device file error                          设备文件错误
#define UDRM_ERROR_DEVICE_FILE_OPEN_ERROR                -127   // Device file open error                     设备文件打开异常
#define UDRM_ERROR_DEVICE_FILE_READ_ERROR                -128   // Device file read error                     设备文件读取异常
#define UDRM_ERROR_DEVICE_FILE_WRITE_ERROR               -129   // Device file write error                    设备文件写入异常
#define UDRM_ERROR_DEVICE_FILE_CLOSE_ERROR               -130   // Device file close error                    设备文件关闭异常
#define UDRM_ERROR_DEVICE_DATA_TYPE_ERROR                -131   // Device data type error                     设备数据类型错误
#define UDRM_ERROR_RULE_NOT_EXIST                        -132   // Rule is not exist                          使用规则不存在
#define UDRM_ERROR_RULE_ALREADY_EXIST                    -133   // Rule already exist                         使用规则已存在
#define UDRM_ERROR_RULE_INVALID                          -134   // Rule is invalid                            使用规则无效
#define UDRM_ERROR_RULE_UPDATE                           -135   // Rule need update                           使用规则需更新
#define UDRM_ERROR_RULE_EXPIRATION                       -136   // Rule is expiration                         使用规则已过期
#define UDRM_ERROR_GZIP_ERROR                            -137   // Gzip error                                 GZIP错误
#define UDRM_ERROR_GZIP_NOT_SUPPORT                      -138   // Gzip not support                           GZIP不支持
#define UDRM_ERROR_ENV_ERROR                             -139   // Env value error                            环境变量错误
#define UDRM_ERROR_ENV_EMPTY                             -140   // Env is empty                               环境变量为空
#define UDRM_ERROR_NOT_MATCH                             -141   // Information not match                      信息不匹配
#define UDRM_ERROR_PARSE_XML_ERROR                       -142   // Parse xml error                            XML解析异常
#define UDRM_ERROR_SYNC_EVENT_ERROR                      -143   // Sync event error                           事件同步异常
#define UDRM_ERROR_BOSS_UNBIND_ERROR                     -144   // BOSS unbind error                          BOSS解绑错误
#define UDRM_ERROR_BOSS_NOTBIND_ERROR                    -145   // BOSS not bind                              BOSS未绑定
#define UDRM_ERROR_BOSS_URL_ERROR                        -146   // BOSS URL error                             BOSS URL错误
#define UDRM_ERROR_TRANSFER_CERT_ERROR                   -147   // Transfer cert error                        传输证书错误
#define UDRM_ERROR_REQUEST_CERT_ERROR                    -148   // Request cert error                         请求证书错误
#define UDRM_ERROR_CONTENT_NO_RIGHT                      -149   // Content no right                           内容无权限
#define UDRM_ERROR_BIND_ERROR                            -150   // Bind error                                 绑定异常
#define UDRM_ERROR_UNBIND_ERROR                          -151   // Unbind error                               解绑异常
#define UDRM_ERROR_NOT_BIND_ERROR                        -152   // Not bind error                             未绑定
#define UDRM_ERROR_VERSION_ERROR                         -153   // Version error                              版本错误
#define UDRM_ERROR_VERSION_NOT_SUPPORT                   -154   // Version not support                        版本不支持
#define UDRM_ERROR_VERSION_NEED_UPDATE                   -155   // Version need update                        版本需更新
#define UDRM_ERROR_TRANSFER_DCERT_NOT_MATCH              -156   // Transfer device cert not match             传输设备证书不匹配
#define UDRM_ERROR_NEED_BIND                             -157   // Need bind                                  需要绑定
#define UDRM_ERROR_UG_NOT_EXIST                          -158   // UG not exist                               网关不存在
#define UDRM_ERROR_UG_ALREADY_EXIST                      -159   // UG already exist                           网关已存在
#define UDRM_ERROR_UG_NO_RIGHT                           -160   // UG no right                                网关无权限
#define UDRM_ERROR_UG_BIND_ERROR                         -161   // UG bind error                              网关绑定错误
#define UDRM_ERROR_UG_UNBIND_ERROR                       -162   // UG unbind error                            网关解绑错误
#define UDRM_ERROR_UG_ALREADY_REGISTER                   -163   // UG already register                        网关已注册
#define UDRM_ERROR_UG_ALREADY_BIND                       -164   // UG already bind                            网关已绑定
#define UDRM_ERROR_UG_NOT_BIND                           -165   // UG not bind                                网关未绑定
#define UDRM_ERROR_UG_BIND_NUM_OVER                      -166   // UG bind number over                        网关绑定数超限
#define UDRM_ERROR_UG_TYPE_ERROR                         -167   // UG type error                              网关类型错误
#define UDRM_ERROR_BOSS_UG_NOT_EXIST                     -168   // BOSS UG not exist                          BOSS网关不存在
#define UDRM_ERROR_BOSS_UG_ALREADY_EXIST                 -169   // BOSS UG already exist                      BOSS网关已存在
#define UDRM_ERROR_BOSS_UG_NO_RIGHT                      -170   // BOSS UG no right                           BOSS网关无权限
#define UDRM_ERROR_BOSS_UG_BIND_ERROR                    -171   // BOSS UG bind error                         BOSS网关绑定错误
#define UDRM_ERROR_BOSS_UG_UNBIND_ERROR                  -172   // BOSS UG unbind error                       BOSS网关解绑错误
#define UDRM_ERROR_BOSS_UG_ALREADY_REGISTER              -173   // BOSS UG already register                   BOSS网关已注册
#define UDRM_ERROR_BOSS_UG_ALREADY_BIND                  -174   // BOSS UG already bind                       BOSS网关已绑定
#define UDRM_ERROR_BOSS_UG_NOT_BIND                      -175   // BOSS UG not bind                           BOSS网关未绑定
#define UDRM_ERROR_BOSS_UG_BIND_NUM_OVER                 -176   // BOSS UG bind number over                   BOSS网关绑定数超限
#define UDRM_ERROR_BOSS_UG_TYPE_ERROR                    -177   // BOSS UG type error                         BOSS网关类型错误
#define UDRM_ERROR_PROGRAM_NOT_EXIST                     -178   // Program not exist                          套餐不存在
#define UDRM_ERROR_PROGRAM_ALREADY_EXIST                 -179   // Program already exist                      套餐已存在
#define UDRM_ERROR_PROGRAM_ERROR                         -180   // Program error                              套餐异常
#define UDRM_ERROR_PROGRAM_NO_RIGHT                      -181   // Program no right                           套餐无权限
#define UDRM_ERROR_BOSS_PROGRAM_NOT_EXIST                -182   // BOSS Program not exist                     BOSS套餐不存在
#define UDRM_ERROR_BOSS_PROGRAM_ALREADY_EXIST            -183   // BOSS Program already exist                 BOSS套餐已存在
#define UDRM_ERROR_BOSS_PROGRAM_ERROR                    -184   // BOSS Program error                         BOSS套餐异常
#define UDRM_ERROR_BOSS_PROGRAM_NO_RIGHT                 -185   // BOSS Program no right                      BOSS套餐无权限
#define UDRM_ERROR_SUB_NOT_EXIST                         -186   // Subscription not exist                     订购不存在
#define UDRM_ERROR_SUB_ALREADY_EXIST                     -187   // Subscription already exit                  订购已经存在
#define UDRM_ERROR_SUB_ERROR                             -188   // Subscription error                         订购异常
#define UDRM_ERROR_SUB_NO_RIGHT                          -189   // Subscription no right                      订购无权限
#define UDRM_ERROR_BOSS_SUB_NOT_EXIST                    -190   // BOSS Subscription not exist                BOSS订购不存在
#define UDRM_ERROR_BOSS_SUB_ALREADY_EXIST                -191   // BOSS Subscription already exit             BOSS订购已经存在
#define UDRM_ERROR_BOSS_SUB_ERROR                        -192   // BOSS Subscription error                    BOSS订购异常
#define UDRM_ERROR_BOSS_SUB_NO_RIGHT                     -193   // BOSS Subscription no right                 BOSS订购无权限
#define UDRM_ERROR_SSL_SESSION_ERROR                     -194   // SSL session error                          SSL Session 异常

#define UDRM_ERROR_NONE									 -195 /*use to count the total error message*/


#define UDRM_DEVICELIST_ACCOUNTNAME_LEN                  256
#define UDRM_DEVICELIST_ACCOUNTPASSWORD_LEN              256
#define UDRM_DEVICELIST_DRMID_LEN                        64
#define UDRM_DEVICELIST_MACADDR_LEN                      256
#define UDRM_DEVICELIST_DEVICENAME_LEN                   256


#define UDRM_CERT_TYPE_ROOT                        0
#define UDRM_CERT_TYPE_DEVICE                      1
#define UDRM_CERT_TYPE_ACS                         2
#define UDRM_CERT_TYPE_KMS                         3
#define UDRM_CERT_TYPE_CAS                         4
#define UDRM_CERT_TYPE_OCSP                        5
#define UDRM_CERT_TYPE_BROWSER                     6
#define UDRM_CERT_TYPE_SERVER                      7
#define UDRM_CERT_TYPE_TEST                        8

#define UDRMCA_REQ_COUNTRY_NAME_LEN                16
#define UDRMCA_REQ_STATE_OR_PROVINCE_NAME_LEN      32
#define UDRMCA_REQ_LOCALITY_NAME_LEN               64
#define UDRMCA_REQ_ORGANIZATION_NAME_LEN           64
#define UDRMCA_REQ_ORGANIZATION_UNIT_NAME_LEN      64
#define UDRMCA_REQ_COMMON_NAME_LEN                 64
#define UDRMCA_REQ_EMAIL_ADDRESS_LEN               24
#define UDRMCA_REQ_EMAIL_PROTECT_LEN               24
#define UDRMCA_REQ_TITLE_LEN                       12
#define UDRMCA_REQ_DESCRIPTION_LEN                 12
#define UDRMCA_REQ_GIVEN_NAME_LEN                  12
#define UDRMCA_REQ_INITIALS_LEN                    64
#define UDRMCA_REQ_NAME_LEN                        12
#define UDRMCA_REQ_SURNAME_LEN                     12
#define UDRMCA_REQ_DN_QUALIFIRE_LEN                12
#define UDRMCA_REQ_UNSTRUCTURED_NAME_LEN           12
#define UDRMCA_REQ_CHALLENGE_PASSWORD_LEN          12
#define UDRMCA_REQ_UNSTRUCTURE_ADDRESS_LEN         12
#define UDRMCA_REQ_UDRM_ID_LEN                     32

#define UDRM_CA_EXT_BASIC_CONSTRAINTS_LEN          512
#define UDRM_CA_EXT_NS_COMMENT_LEN                 512
#define UDRM_CA_EXT_SUBJECT_KEY_IDENTIFIRE_LEN     512
#define UDRM_CA_EXT_AUTHORITY_KEY_IDENTIFIRE_LEN   512
#define UDRM_CA_EXT_KEY_USAGE_LEN                  512
#define UDRM_CA_EXT_EKEY_USAGE_LEN                 512
#define UDRM_CA_EXT_CRL_DISTRIBUTION_POINTS_LEN    512
#define UDRM_CA_EXT_AUTHORITY_INFO_ACCESS_LEN      512
#define UDRM_CA_EXT_POLICY_CONSTRAINT_LEN          512


#define UDRMCA_CERT_SUBJECT_LEN                     256
#define UDRMCA_CERT_COUNTRY_NAME_LEN                16
#define UDRMCA_CERT_STATE_OR_PROVINCE_NAME_LEN      32
#define UDRMCA_CERT_LOCALITY_NAME_LEN               64
#define UDRMCA_CERT_ORGANIZATION_NAME_LEN           64
#define UDRMCA_CERT_ORGANIZATION_UNIT_NAME_LEN      64
#define UDRMCA_CERT_COMMON_NAME_LEN                 64
#define UDRMCA_CERT_UDRM_ID_LEN                     32
#define UDRMCA_CERT_NOT_BEFORE_LEN                  64
#define UDRMCA_CERT_NOT_AFTER_LEN                   64
#define UDRMCA_CERT_ISSUER_SUBJECT_LEN              256
#define UDRMCA_CERT_SIGN_ALGORITHM_LEN              64


#define UDRM_CA_CERT_MAX_LEN                       8192
#define UDRM_CA_REQ_MAX_LEN                        4096
#define UDRM_CA_KEY_MAX_LEN                        4096
#define UDRM_LICENSE_MAX_LEN                       8192

#define UDRM_CRYPTO_AES_KEY_LEN                    16

#define UDRM_DEVICE_TYPE_NONE                      -1
#define UDRM_DEVICE_TYPE_HARDWARE                  0
#define UDRM_DEVICE_TYPE_SOFT                      1

#define UDRM_MAX_MSG_LEGNTH 1024

typedef enum _Decrypt_Status
{
	UDRM_DECRYPT_NONE =0,
	UDRM_DECRYPT_ACCESSING_LICENSE,
	UDRM_DECRYPT_DECRYPTING,
	UDRM_DECRYPT_ERROR,
	UDRM_DECRYPT_STOPPED,
} decrypt_status;

typedef struct ContentRule_st {
	UTI_INT playFlag;
	UTI_INT playMethod;
	UTI_INT playCount;
	UTI_INT playTime;
	UTI_LONG playStartTime;
	UTI_LONG playEndTime;
	UTI_INT contentRecFlag;
	UTI_INT contentStoreFlag;
	UTI_INT rightStoreFlag;
	UTI_INT contentTransferFlag;
	UTI_INT rightTransferFlag;
	UTI_INT parentControlFlag;
}ContentRule, *PContentRule;

typedef struct UDRMDevice_st
{
	UTI_CHAR pchAccountName[UDRM_DEVICELIST_ACCOUNTNAME_LEN];
	UTI_CHAR pchAccountPassword[UDRM_DEVICELIST_ACCOUNTPASSWORD_LEN];
	UTI_CHAR pchDRMID [UDRM_DEVICELIST_DRMID_LEN];
	UTI_CHAR pchMACAddr[UDRM_DEVICELIST_MACADDR_LEN];
	UTI_CHAR pchDeviceName[UDRM_DEVICELIST_DEVICENAME_LEN];
	UTI_INT nStatus;
	struct UDRMDevice_st * next;
}UDRMDevice, *PUDRMDevice;

typedef struct UDRMDeviceList_st {
	UTI_INT count;
	UDRMDevice * first;
}UDRMDeviceList, *PUDRMDeviceList;

typedef enum _UDRM_Stage
{
	UDRM_STAGE_NONE = 0,
	UDRM_STAGE_DRM_AGENT_CERT_NOT_FOUND,//CERT INIT
	UDRM_STAGE_DRM_AGENT_CERT_INIT_START,
	UDRM_STAGE_DRM_AGENT_CERT_INITINIZING,
	UDRM_STAGE_DRM_AGENT_CERT_INIT_SUCCESS,
	UDRM_STAGE_DRM_AGENT_CERT_INIT_FAILED,
	UDRM_STAGE_DRM_AGENT_CERT_INVALID_FOUND,//CERT UPDATE
	UDRM_STAGE_DRM_AGENT_CERT_UPDATE_START,
	UDRM_STAGE_DRM_AGENT_CERT_UPDATING,
	UDRM_STAGE_DRM_AGENT_CERT_UPDATE_SUCCESS,
	UDRM_STAGE_DRM_AGENT_CERT_UPDATE_FAILED,
	UDRM_STAGE_DRM_AGENT_INIT_START,//AGENT INIT
	UDRM_STAGE_DRM_AGENT_INIT_SUCCESS,
	UDRM_STAGE_DRM_AGENT_INIT_FAILED,
	UDRM_STAGE_DRM_CONTENT_FOUND,
	UDRM_STAGE_START_ACCESS_LICENSE,//LICENSE ACCESS
	UDRM_STAGE_DRM_SESSION_CREATED,
	UDRM_STAGE_DRM_SESSION_CREATE_FAILED,
	UDRM_STAGE_ACCESSING_LICENSE,
	UDRM_STAGE_ACCESS_LICENSE_FAILED_RETRY,
	UDRM_STAGE_ACCESS_LICENSE_SUCCESS,
	UDRM_STAGE_ACCESS_LICENSE_FAILED,
	UDRM_STAGE_DRM_SESSION_CLOSED,
	UDRM_STAGE_DRM_AGENT_DESTORY,
} UDRM_Stage;


typedef UTI_SINT32 (*UDRMMsgCallback)(IN UTI_UINT8 *pu8Msg, IN UTI_UINT32 u32Len, IN UTI_SINT32 s32UDRMError, IN decrypt_status status, IN UTI_VOID* pCallbackData);
UTI_SINT32 UDRMPlaySetUDRMMsgFunc(IN UTI_VOID* u32PlayHandle,  IN UDRMMsgCallback pfUDRMMsgCallback, IN UTI_VOID* pCallbackData);
//UTI_SINT32	UDRMMsg(IN UTI_UINT8 *pu8Msg, IN UTI_UINT32 u32Len, IN UTI_SINT32 s32UDRMError, IN decrypt_status status, IN UTI_VOID* pCallbackData);


UTI_RESULT UDRMAgentInit();

UTI_RESULT UDRMAgentInitLog(UTI_INT logtype, UTI_INT loglevel, UTI_CHAR * logfile);
UTI_INT UDRMAgentInitDeviceLocalPath(UTI_CHAR * localpath);

UTI_RESULT UDRMAgentSetEnv(IN UTI_CHAR * pchAccountName, IN UTI_CHAR *pchAccountPassword, IN UTI_CHAR *pchMACAddr, IN UTI_CHAR *pchDeviceName,IN UTI_CHAR *pchDRMURL);

UTI_RESULT UDRMAgentCheckBindStatus();

UTI_RESULT UDRMAgentBindDevice(IN UTI_CHAR *pchAccountName, IN UTI_CHAR *pchAccountPassword, IN UTI_CHAR *pchMACAddr, IN UTI_CHAR *pchDeviceName);

UTI_VOID* UDRMAgentDecryptStart(IN UTI_UINT16 u16ProgramNumber,IN UTI_CHAR *pchAuthorizationURL);



UTI_SINT32 UDRMAgentDecryptTS(IN UTI_VOID* u32PlayHandle, IN UTI_UINT8 *pu8InTsPackets,
								 IN UTI_UINT32 u32InPacketsLength, OUT UTI_UINT8 *pu8OutTsPackets, IN UTI_UINT32 u32OutPacketsLength);

UTI_SINT32 UDRMAgentDecryptStop(IN UTI_VOID* u32PlayHandle);

UTI_VOID UDRMAgentDestroy();

UTI_SINT32 UDRMSetMp4PsshData(IN UTI_VOID* u32PlayHandle, IN UTI_UINT8 *pu8InTsPackets,IN UTI_UINT32 u32InPacketsLength);
UTI_SINT32 UDRMPlayLoadMp4_data(IN UTI_VOID* u32PlayHandle,IN char* pc_iv_value,IN char* encdata, IN UTI_UINT32 buflong, OUT char *outdecdata);

UTI_RESULT UDRMAgentGetCertCommonName(OUT UTI_CHAR *pchCommonName, IN UTI_UINT32 u32CommanNameLength);
UTI_RESULT UDRMAgentGetCertValidTime(OUT UTI_CHAR *pchValidFrom, IN UTI_UINT32 u32ValidFromLength, OUT UTI_CHAR *pchValidTo, IN UTI_UINT32 u32ValidToLength);

UTI_INT UDRMAgentGetDRMID(UTI_CHAR * DRMID, UTI_UINT DRMIDBuflen);

UTI_RESULT UDRMAgentGetErrorType(OUT UTI_CHAR *pchErrorMsg,  IN UTI_UINT32 u32ErrorMsgLength);
UTI_RESULT UDRMAgentGetErrorNumber(OUT UTI_CHAR *pchErrorMsgDebug,  IN UTI_UINT32 u32ErrorMsgDebugLength);

UTI_RESULT UDRMAgentGetDRMVersion(OUT UTI_CHAR *pchDRMVersion,IN UTI_UINT32 u32DRMVersionLength);



UTI_SINT32 UDRMPlayGetContentID(UTI_VOID* pu32PlayHandle, UTI_CHAR* pu8ContentID, UTI_INT nContentIDLength);

#define UDRM_LICENSE_ID_LEN                        64
#define UDRM_LICENSE_CID_LEN                       64
#define UDRM_LICENSE_USN_LEN                       64


UTI_INT		UDRMAgentInitializeDevice();
UTI_INT		UDRMAgentCheckDeviceStatus();



#endif  /* _UTI_TYPE_H_ */
