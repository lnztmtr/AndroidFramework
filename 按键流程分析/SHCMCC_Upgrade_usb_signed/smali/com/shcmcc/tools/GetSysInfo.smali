.class public Lcom/shcmcc/tools/GetSysInfo;
.super Ljava/lang/Object;
.source "GetSysInfo.java"


# static fields
.field private static PNAME_DEVICE_ID:Ljava/lang/String;

.field private static PNAME_EPG_AccountIdentity:Ljava/lang/String;

.field private static PNAME_EPG_CityCode:Ljava/lang/String;

.field private static PNAME_EPG_CpCode:Ljava/lang/String;

.field private static PNAME_EPG_DisasterRecovery:Ljava/lang/String;

.field private static PNAME_EPG_IndexUrl:Ljava/lang/String;

.field private static PNAME_EPG_IndexUrl_old:Ljava/lang/String;

.field private static PNAME_EPG_LoginStatus:Ljava/lang/String;

.field private static PNAME_EPG_MobileDeviceId:Ljava/lang/String;

.field private static PNAME_EPG_MobileToken:Ljava/lang/String;

.field private static PNAME_EPG_MobileUSERID:Ljava/lang/String;

.field private static PNAME_EPG_Token:Ljava/lang/String;

.field private static PNAME_EPG_UPGRADEMODEL:Ljava/lang/String;

.field private static PNAME_EPG_USERID:Ljava/lang/String;

.field private static PNAME_EPG_UserGroup:Ljava/lang/String;

.field private static PNAME_EPG_cmcchomeurl:Ljava/lang/String;

.field private static PNAME_EPG_copyrightid:Ljava/lang/String;

.field private static PNAME_EPG_eccode:Ljava/lang/String;

.field private static PNAME_EPG_eccoporationcode:Ljava/lang/String;

.field private static PNAME_EPG_gd_tvid:Ljava/lang/String;

.field private static PNAME_EPG_province:Ljava/lang/String;

.field private static PNAME_FRAME_WORK_VERSION:Ljava/lang/String;

.field private static PNAME_FirmwareVersion:Ljava/lang/String;

.field private static PNAME_HardwareVersion:Ljava/lang/String;

.field private static PNAME_SECURE_EPG_BUSINESS_PARAM:Ljava/lang/String;

.field private static PNAME_TerminalType:Ljava/lang/String;

.field private static PNAME_serialno:Ljava/lang/String;

.field private static c:Ljava/lang/Class;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/lang/Class",
            "<*>;"
        }
    .end annotation
.end field

.field private static context:Landroid/content/Context;

.field private static mInstance:Lcom/shcmcc/tools/GetSysInfo;


# direct methods
.method static constructor <clinit>()V
    .locals 1

    .prologue
    const/4 v0, 0x0

    .line 14
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_serialno:Ljava/lang/String;

    .line 15
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_FirmwareVersion:Ljava/lang/String;

    .line 16
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_TerminalType:Ljava/lang/String;

    .line 17
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_HardwareVersion:Ljava/lang/String;

    .line 19
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_USERID:Ljava/lang/String;

    .line 20
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_province:Ljava/lang/String;

    .line 21
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_LoginStatus:Ljava/lang/String;

    .line 22
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_Token:Ljava/lang/String;

    .line 23
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_MobileUSERID:Ljava/lang/String;

    .line 24
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_MobileToken:Ljava/lang/String;

    .line 25
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_MobileDeviceId:Ljava/lang/String;

    .line 26
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_IndexUrl:Ljava/lang/String;

    .line 27
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_IndexUrl_old:Ljava/lang/String;

    .line 28
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_cmcchomeurl:Ljava/lang/String;

    .line 29
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_gd_tvid:Ljava/lang/String;

    .line 30
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_eccode:Ljava/lang/String;

    .line 31
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_copyrightid:Ljava/lang/String;

    .line 32
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_eccoporationcode:Ljava/lang/String;

    .line 33
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_CpCode:Ljava/lang/String;

    .line 34
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_CityCode:Ljava/lang/String;

    .line 35
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_UserGroup:Ljava/lang/String;

    .line 36
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_AccountIdentity:Ljava/lang/String;

    .line 37
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_SECURE_EPG_BUSINESS_PARAM:Ljava/lang/String;

    .line 38
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_FRAME_WORK_VERSION:Ljava/lang/String;

    .line 39
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_DEVICE_ID:Ljava/lang/String;

    .line 40
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_DisasterRecovery:Ljava/lang/String;

    .line 41
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_UPGRADEMODEL:Ljava/lang/String;

    .line 238
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->c:Ljava/lang/Class;

    .line 250
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->mInstance:Lcom/shcmcc/tools/GetSysInfo;

    .line 251
    sput-object v0, Lcom/shcmcc/tools/GetSysInfo;->context:Landroid/content/Context;

    return-void
.end method

.method private constructor <init>()V
    .locals 0

    .prologue
    .line 301
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 302
    return-void
.end method

.method public static getInstance(Ljava/lang/String;Ljava/lang/String;Landroid/content/Context;)Lcom/shcmcc/tools/GetSysInfo;
    .locals 3
    .param p0, "key"    # Ljava/lang/String;
    .param p1, "value"    # Ljava/lang/String;
    .param p2, "context"    # Landroid/content/Context;

    .prologue
    .line 254
    :try_start_0
    const-string v1, "10086"

    invoke-virtual {v1, p0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_0

    .line 255
    new-instance v1, Ljava/lang/Exception;

    const-string v2, "invalid key!"

    invoke-direct {v1, v2}, Ljava/lang/Exception;-><init>(Ljava/lang/String;)V

    throw v1
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    .line 296
    :catch_0
    move-exception v0

    .line 297
    .local v0, "e":Ljava/lang/Exception;
    const-string v1, "GetSysInfo"

    invoke-virtual {v0}, Ljava/lang/Exception;->getMessage()Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    .line 299
    const/4 v1, 0x0

    .end local v0    # "e":Ljava/lang/Exception;
    :goto_0
    return-object v1

    .line 256
    :cond_0
    if-nez p2, :cond_1

    .line 257
    :try_start_1
    new-instance v1, Ljava/lang/Exception;

    const-string v2, "invalid context,context cannot be null!"

    invoke-direct {v1, v2}, Ljava/lang/Exception;-><init>(Ljava/lang/String;)V

    throw v1

    .line 258
    :cond_1
    sget-object v1, Lcom/shcmcc/tools/GetSysInfo;->mInstance:Lcom/shcmcc/tools/GetSysInfo;

    if-nez v1, :cond_2

    .line 259
    new-instance v1, Lcom/shcmcc/tools/GetSysInfo;

    invoke-direct {v1}, Lcom/shcmcc/tools/GetSysInfo;-><init>()V

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->mInstance:Lcom/shcmcc/tools/GetSysInfo;

    .line 260
    const-string v1, "android.os.SystemProperties"

    invoke-static {v1}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v1

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->c:Ljava/lang/Class;

    .line 261
    sput-object p2, Lcom/shcmcc/tools/GetSysInfo;->context:Landroid/content/Context;

    .line 263
    const-string v1, "ro.serialno"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_serialno:Ljava/lang/String;

    .line 264
    const-string v1, "ro.build.fingerprint"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_FirmwareVersion:Ljava/lang/String;

    .line 265
    const-string v1, "ro.product.model"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_TerminalType:Ljava/lang/String;

    .line 266
    const-string v1, "ro.build.version.incremental"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_HardwareVersion:Ljava/lang/String;

    .line 268
    const-string v1, "epg.userid"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_USERID:Ljava/lang/String;

    .line 269
    const-string v1, "epg.province"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_province:Ljava/lang/String;

    .line 270
    const-string v1, "epg.login"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_LoginStatus:Ljava/lang/String;

    .line 271
    const-string v1, "epg.token"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_Token:Ljava/lang/String;

    .line 272
    const-string v1, "epg.mobile.userid"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_MobileUSERID:Ljava/lang/String;

    .line 273
    const-string v1, "epg.mobile.token"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_MobileToken:Ljava/lang/String;

    .line 274
    const-string v1, "epg.mobile.deviceid"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_MobileDeviceId:Ljava/lang/String;

    .line 275
    const-string v1, "epg.indexurl"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_IndexUrl:Ljava/lang/String;

    .line 276
    const-string v1, "epg.indexwsurl"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_IndexUrl_old:Ljava/lang/String;

    .line 277
    const-string v1, "epg.cmcchomeurl"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_cmcchomeurl:Ljava/lang/String;

    .line 278
    const-string v1, "epg.authcode"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_gd_tvid:Ljava/lang/String;

    .line 279
    const-string v1, "epg.eccode"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_eccode:Ljava/lang/String;

    .line 280
    const-string v1, "epg.copyrightid"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_copyrightid:Ljava/lang/String;

    .line 281
    const-string v1, "epg.eccoporationcode"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_eccoporationcode:Ljava/lang/String;

    .line 282
    const-string v1, "epg.cpcode"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_CpCode:Ljava/lang/String;

    .line 283
    const-string v1, "epg.citycode"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_CityCode:Ljava/lang/String;

    .line 284
    const-string v1, "epg.usergroup"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_UserGroup:Ljava/lang/String;

    .line 285
    const-string v1, "epg.accountidentity"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_AccountIdentity:Ljava/lang/String;

    .line 286
    const-string v1, "epg.businessparam"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_SECURE_EPG_BUSINESS_PARAM:Ljava/lang/String;

    .line 287
    const-string v1, "com.shcmcc.setting"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_FRAME_WORK_VERSION:Ljava/lang/String;

    .line 288
    const-string v1, "ro.deviceid"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_DEVICE_ID:Ljava/lang/String;

    .line 289
    const-string v1, "epg.disasterrecovery"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_DisasterRecovery:Ljava/lang/String;

    .line 290
    const-string v1, "upgradeMode"

    sput-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_UPGRADEMODEL:Ljava/lang/String;

    .line 295
    :goto_1
    sget-object v1, Lcom/shcmcc/tools/GetSysInfo;->mInstance:Lcom/shcmcc/tools/GetSysInfo;

    goto/16 :goto_0

    .line 293
    :cond_2
    sput-object p2, Lcom/shcmcc/tools/GetSysInfo;->context:Landroid/content/Context;
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_0

    goto :goto_1
.end method

.method private getSystemProperties(Ljava/lang/String;)Ljava/lang/String;
    .locals 9
    .param p1, "key"    # Ljava/lang/String;

    .prologue
    .line 240
    const/4 v3, 0x0

    .line 242
    .local v3, "str":Ljava/lang/String;
    :try_start_0
    sget-object v4, Lcom/shcmcc/tools/GetSysInfo;->c:Ljava/lang/Class;

    const-string v5, "get"

    const/4 v6, 0x1

    new-array v6, v6, [Ljava/lang/Class;

    const/4 v7, 0x0

    const-class v8, Ljava/lang/String;

    aput-object v8, v6, v7

    invoke-virtual {v4, v5, v6}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v2

    .line 243
    .local v2, "get":Ljava/lang/reflect/Method;
    sget-object v4, Lcom/shcmcc/tools/GetSysInfo;->c:Ljava/lang/Class;

    const/4 v5, 0x1

    new-array v5, v5, [Ljava/lang/Object;

    const/4 v6, 0x0

    aput-object p1, v5, v6

    invoke-virtual {v2, v4, v5}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    move-object v0, v4

    check-cast v0, Ljava/lang/String;

    move-object v3, v0
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    .line 247
    .end local v2    # "get":Ljava/lang/reflect/Method;
    :goto_0
    return-object v3

    .line 244
    :catch_0
    move-exception v1

    .line 245
    .local v1, "e":Ljava/lang/Exception;
    const-string v4, "GetSysInfo"

    invoke-virtual {v1}, Ljava/lang/Exception;->getMessage()Ljava/lang/String;

    move-result-object v5

    invoke-static {v4, v5}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_0
.end method


# virtual methods
.method public getDeviceId()Ljava/lang/String;
    .locals 1

    .prologue
    .line 222
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_DEVICE_ID:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getDisasterRecovery()Ljava/lang/String;
    .locals 2

    .prologue
    .line 227
    const-string v0, "1"

    sget-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_DisasterRecovery:Ljava/lang/String;

    invoke-direct {p0, v1}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_0

    const-string v0, "1"

    :goto_0
    return-object v0

    :cond_0
    const-string v0, "0"

    goto :goto_0
.end method

.method public getEpgAccountIdentity()Ljava/lang/String;
    .locals 1

    .prologue
    .line 200
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_AccountIdentity:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getEpgBusinessParam()Ljava/lang/String;
    .locals 2

    .prologue
    .line 204
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->context:Landroid/content/Context;

    invoke-virtual {v0}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v0

    sget-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_SECURE_EPG_BUSINESS_PARAM:Ljava/lang/String;

    invoke-static {v0, v1}, Landroid/provider/Settings$Secure;->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getEpgCityCode()Ljava/lang/String;
    .locals 1

    .prologue
    .line 165
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_CityCode:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getEpgCmccHomeUrl()Ljava/lang/String;
    .locals 1

    .prologue
    .line 178
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_cmcchomeurl:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getEpgCopyrightId()Ljava/lang/String;
    .locals 1

    .prologue
    .line 186
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_copyrightid:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getEpgCpCode()Ljava/lang/String;
    .locals 1

    .prologue
    .line 159
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_CpCode:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getEpgEccode()Ljava/lang/String;
    .locals 1

    .prologue
    .line 182
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_eccode:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getEpgEccoporationCode()Ljava/lang/String;
    .locals 1

    .prologue
    .line 190
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_eccoporationcode:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getEpgIndexUrl()Ljava/lang/String;
    .locals 2

    .prologue
    .line 172
    sget-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_IndexUrl:Ljava/lang/String;

    invoke-direct {p0, v1}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 173
    .local v0, "indexUrl":Ljava/lang/String;
    if-eqz v0, :cond_0

    .end local v0    # "indexUrl":Ljava/lang/String;
    :goto_0
    return-object v0

    .restart local v0    # "indexUrl":Ljava/lang/String;
    :cond_0
    sget-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_IndexUrl_old:Ljava/lang/String;

    invoke-direct {p0, v1}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    goto :goto_0
.end method

.method public getEpgMobileDeviceId()Ljava/lang/String;
    .locals 1

    .prologue
    .line 147
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_MobileDeviceId:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getEpgMobileToken()Ljava/lang/String;
    .locals 1

    .prologue
    .line 142
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_MobileToken:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getEpgMobileUserId()Ljava/lang/String;
    .locals 1

    .prologue
    .line 136
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_MobileUSERID:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getEpgProvince()Ljava/lang/String;
    .locals 1

    .prologue
    .line 153
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_province:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getEpgToken()Ljava/lang/String;
    .locals 1

    .prologue
    .line 130
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_Token:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getEpgUserGroup()Ljava/lang/String;
    .locals 1

    .prologue
    .line 195
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_UserGroup:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getEpgUserId()Ljava/lang/String;
    .locals 1

    .prologue
    .line 111
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_USERID:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getFirmwareVersion()Ljava/lang/String;
    .locals 1

    .prologue
    .line 66
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_FirmwareVersion:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getFrameworkVersion()Ljava/lang/String;
    .locals 5

    .prologue
    .line 209
    :try_start_0
    sget-object v3, Lcom/shcmcc/tools/GetSysInfo;->context:Landroid/content/Context;

    invoke-virtual {v3}, Landroid/content/Context;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object v1

    .line 210
    .local v1, "PKG_MGR":Landroid/content/pm/PackageManager;
    sget-object v3, Lcom/shcmcc/tools/GetSysInfo;->PNAME_FRAME_WORK_VERSION:Ljava/lang/String;

    const/4 v4, 0x1

    invoke-virtual {v1, v3, v4}, Landroid/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;

    move-result-object v2

    .line 212
    .local v2, "pi":Landroid/content/pm/PackageInfo;
    iget-object v3, v2, Landroid/content/pm/PackageInfo;->versionName:Ljava/lang/String;
    :try_end_0
    .catch Landroid/content/pm/PackageManager$NameNotFoundException; {:try_start_0 .. :try_end_0} :catch_0

    .line 217
    .end local v1    # "PKG_MGR":Landroid/content/pm/PackageManager;
    .end local v2    # "pi":Landroid/content/pm/PackageInfo;
    :goto_0
    return-object v3

    .line 214
    :catch_0
    move-exception v0

    .line 217
    .local v0, "E":Landroid/content/pm/PackageManager$NameNotFoundException;
    const-string v3, "unknown,not found SWSettings."

    goto :goto_0
.end method

.method public getHardwareVersion()Ljava/lang/String;
    .locals 1

    .prologue
    .line 105
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_HardwareVersion:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getSnNum()Ljava/lang/String;
    .locals 1

    .prologue
    .line 78
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_serialno:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getTVID()Ljava/lang/String;
    .locals 1

    .prologue
    .line 124
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_gd_tvid:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getTerminalType()Ljava/lang/String;
    .locals 1

    .prologue
    .line 91
    sget-object v0, Lcom/shcmcc/tools/GetSysInfo;->PNAME_TerminalType:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getUpgradeMode()Ljava/lang/String;
    .locals 3

    .prologue
    .line 231
    sget-object v1, Lcom/shcmcc/tools/GetSysInfo;->context:Landroid/content/Context;

    invoke-virtual {v1}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v1

    sget-object v2, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_UPGRADEMODEL:Ljava/lang/String;

    invoke-static {v1, v2}, Landroid/provider/Settings$Secure;->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 232
    .local v0, "model":Ljava/lang/String;
    invoke-static {v0}, Landroid/text/TextUtils;->isEmpty(Ljava/lang/CharSequence;)Z

    move-result v1

    if-eqz v1, :cond_0

    .line 233
    const-string v0, "stable"

    .line 235
    :cond_0
    return-object v0
.end method

.method public isEpgLogined()Z
    .locals 2

    .prologue
    .line 117
    const-string v0, "1"

    sget-object v1, Lcom/shcmcc/tools/GetSysInfo;->PNAME_EPG_LoginStatus:Ljava/lang/String;

    invoke-direct {p0, v1}, Lcom/shcmcc/tools/GetSysInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    return v0
.end method
