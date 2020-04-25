.class public Lcom/shcmcc/tools/UserAccountInfo;
.super Ljava/lang/Object;
.source "UserAccountInfo.java"


# static fields
.field private static PNAME_DeviceId:Ljava/lang/String;

.field private static PNAME_EPG_CityCode:Ljava/lang/String;

.field private static PNAME_EPG_DisasterRecovery:Ljava/lang/String;

.field private static PNAME_EPG_province:Ljava/lang/String;

.field private static PNAME_FirmwareVersion:Ljava/lang/String;

.field private static PNAME_HardwareBrand:Ljava/lang/String;

.field private static PNAME_HardwareManufacturer:Ljava/lang/String;

.field private static PNAME_HardwareVersion:Ljava/lang/String;

.field public static PNAME_SECURE_HOME_PAGE:Ljava/lang/String;

.field public static PNAME_SECURE_NTVUSERACCOUNT:Ljava/lang/String;

.field public static PNAME_SECURE_NTVUSERPASSWORD:Ljava/lang/String;

.field public static PNAME_SECURE_NTVUSERSUFFIX:Ljava/lang/String;

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

.field private static mInstance:Lcom/shcmcc/tools/UserAccountInfo;


# instance fields
.field private context:Landroid/content/Context;


# direct methods
.method static constructor <clinit>()V
    .locals 1

    .prologue
    const/4 v0, 0x0

    .line 12
    sput-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_serialno:Ljava/lang/String;

    .line 13
    sput-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_FirmwareVersion:Ljava/lang/String;

    .line 14
    sput-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_TerminalType:Ljava/lang/String;

    .line 15
    sput-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_HardwareVersion:Ljava/lang/String;

    .line 16
    sput-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_HardwareBrand:Ljava/lang/String;

    .line 17
    sput-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_HardwareManufacturer:Ljava/lang/String;

    .line 18
    sput-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_DeviceId:Ljava/lang/String;

    .line 20
    sput-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_SECURE_HOME_PAGE:Ljava/lang/String;

    .line 21
    sput-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_SECURE_NTVUSERACCOUNT:Ljava/lang/String;

    .line 22
    sput-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_SECURE_NTVUSERSUFFIX:Ljava/lang/String;

    .line 23
    sput-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_SECURE_NTVUSERPASSWORD:Ljava/lang/String;

    .line 25
    sput-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_EPG_province:Ljava/lang/String;

    .line 26
    sput-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_EPG_CityCode:Ljava/lang/String;

    .line 27
    sput-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_EPG_DisasterRecovery:Ljava/lang/String;

    .line 46
    sput-object v0, Lcom/shcmcc/tools/UserAccountInfo;->c:Ljava/lang/Class;

    .line 58
    sput-object v0, Lcom/shcmcc/tools/UserAccountInfo;->mInstance:Lcom/shcmcc/tools/UserAccountInfo;

    return-void
.end method

.method private constructor <init>()V
    .locals 1

    .prologue
    .line 167
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 11
    const/4 v0, 0x0

    iput-object v0, p0, Lcom/shcmcc/tools/UserAccountInfo;->context:Landroid/content/Context;

    .line 168
    return-void
.end method

.method public static getInstance(Landroid/content/Context;)Lcom/shcmcc/tools/UserAccountInfo;
    .locals 5
    .param p0, "context_"    # Landroid/content/Context;

    .prologue
    .line 61
    :try_start_0
    const-string v1, "10086"

    .line 62
    .local v1, "key":Ljava/lang/String;
    const-string v2, "10086"

    invoke-virtual {v2, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-nez v2, :cond_0

    .line 63
    new-instance v2, Ljava/lang/Exception;

    const-string v3, "invalid key!"

    invoke-direct {v2, v3}, Ljava/lang/Exception;-><init>(Ljava/lang/String;)V

    throw v2
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    .line 87
    .end local v1    # "key":Ljava/lang/String;
    :catch_0
    move-exception v0

    .line 88
    .local v0, "e":Ljava/lang/Exception;
    const-string v2, "UserAccountInfo"

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v0}, Ljava/lang/Exception;->getMessage()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v2, v3}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    .line 90
    const/4 v2, 0x0

    .end local v0    # "e":Ljava/lang/Exception;
    :goto_0
    return-object v2

    .line 65
    .restart local v1    # "key":Ljava/lang/String;
    :cond_0
    :try_start_1
    sget-object v2, Lcom/shcmcc/tools/UserAccountInfo;->mInstance:Lcom/shcmcc/tools/UserAccountInfo;

    if-nez v2, :cond_1

    .line 66
    new-instance v2, Lcom/shcmcc/tools/UserAccountInfo;

    invoke-direct {v2}, Lcom/shcmcc/tools/UserAccountInfo;-><init>()V

    sput-object v2, Lcom/shcmcc/tools/UserAccountInfo;->mInstance:Lcom/shcmcc/tools/UserAccountInfo;

    .line 67
    sget-object v2, Lcom/shcmcc/tools/UserAccountInfo;->mInstance:Lcom/shcmcc/tools/UserAccountInfo;

    iput-object p0, v2, Lcom/shcmcc/tools/UserAccountInfo;->context:Landroid/content/Context;

    .line 68
    const-string v2, "android.os.SystemProperties"

    invoke-static {v2}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v2

    sput-object v2, Lcom/shcmcc/tools/UserAccountInfo;->c:Ljava/lang/Class;

    .line 70
    const-string v2, "ro.serialno"

    sput-object v2, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_serialno:Ljava/lang/String;

    .line 71
    const-string v2, "ro.build.fingerprint"

    sput-object v2, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_FirmwareVersion:Ljava/lang/String;

    .line 72
    const-string v2, "ro.product.model"

    sput-object v2, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_TerminalType:Ljava/lang/String;

    .line 73
    const-string v2, "ro.build.version.incremental"

    sput-object v2, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_HardwareVersion:Ljava/lang/String;

    .line 74
    const-string v2, "ro.product.brand"

    sput-object v2, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_HardwareBrand:Ljava/lang/String;

    .line 75
    const-string v2, "ro.product.manufacturer"

    sput-object v2, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_HardwareManufacturer:Ljava/lang/String;

    .line 76
    const-string v2, "ro.deviceid"

    sput-object v2, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_DeviceId:Ljava/lang/String;

    .line 78
    const-string v2, "home_page"

    sput-object v2, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_SECURE_HOME_PAGE:Ljava/lang/String;

    .line 79
    const-string v2, "ntvuseraccount"

    sput-object v2, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_SECURE_NTVUSERACCOUNT:Ljava/lang/String;

    .line 80
    const-string v2, "ntvusersuffix"

    sput-object v2, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_SECURE_NTVUSERSUFFIX:Ljava/lang/String;

    .line 81
    const-string v2, "ntvuserpassword"

    sput-object v2, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_SECURE_NTVUSERPASSWORD:Ljava/lang/String;

    .line 82
    const-string v2, "epg.province"

    sput-object v2, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_EPG_province:Ljava/lang/String;

    .line 83
    const-string v2, "epg.citycode"

    sput-object v2, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_EPG_CityCode:Ljava/lang/String;

    .line 84
    const-string v2, "epg.disasterrecovery"

    sput-object v2, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_EPG_DisasterRecovery:Ljava/lang/String;

    .line 86
    :cond_1
    sget-object v2, Lcom/shcmcc/tools/UserAccountInfo;->mInstance:Lcom/shcmcc/tools/UserAccountInfo;
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_0

    goto :goto_0
.end method

.method private getSystemProperties(Ljava/lang/String;)Ljava/lang/String;
    .locals 9
    .param p1, "key"    # Ljava/lang/String;

    .prologue
    .line 48
    const/4 v3, 0x0

    .line 50
    .local v3, "str":Ljava/lang/String;
    :try_start_0
    sget-object v4, Lcom/shcmcc/tools/UserAccountInfo;->c:Ljava/lang/Class;

    const-string v5, "get"

    const/4 v6, 0x1

    new-array v6, v6, [Ljava/lang/Class;

    const/4 v7, 0x0

    const-class v8, Ljava/lang/String;

    aput-object v8, v6, v7

    invoke-virtual {v4, v5, v6}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v2

    .line 51
    .local v2, "get":Ljava/lang/reflect/Method;
    sget-object v4, Lcom/shcmcc/tools/UserAccountInfo;->c:Ljava/lang/Class;

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

    .line 55
    .end local v2    # "get":Ljava/lang/reflect/Method;
    :goto_0
    return-object v3

    .line 52
    :catch_0
    move-exception v1

    .line 53
    .local v1, "e":Ljava/lang/Exception;
    const-string v4, "UserAccountInfo"

    new-instance v5, Ljava/lang/StringBuilder;

    invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v1}, Ljava/lang/Exception;->getMessage()Ljava/lang/String;

    move-result-object v6

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-static {v4, v5}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_0
.end method

.method public static main([Ljava/lang/String;)V
    .locals 4
    .param p0, "args"    # [Ljava/lang/String;

    .prologue
    .line 170
    const/4 v1, 0x0

    invoke-static {v1}, Lcom/shcmcc/tools/UserAccountInfo;->getInstance(Landroid/content/Context;)Lcom/shcmcc/tools/UserAccountInfo;

    move-result-object v0

    .line 171
    .local v0, "sysinfo":Lcom/shcmcc/tools/UserAccountInfo;
    sget-object v1, Ljava/lang/System;->out:Ljava/io/PrintStream;

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v3, "getLoginUrl():="

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0}, Lcom/shcmcc/tools/UserAccountInfo;->getLoginUrl()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    .line 172
    sget-object v1, Ljava/lang/System;->out:Ljava/io/PrintStream;

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v3, "getLoginAcount():="

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0}, Lcom/shcmcc/tools/UserAccountInfo;->getLoginAcount()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    .line 173
    sget-object v1, Ljava/lang/System;->out:Ljava/io/PrintStream;

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v3, "getLoginPassword():="

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0}, Lcom/shcmcc/tools/UserAccountInfo;->getLoginPassword()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    .line 176
    return-void
.end method


# virtual methods
.method public getDeviceId()Ljava/lang/String;
    .locals 1

    .prologue
    .line 165
    sget-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_DeviceId:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/UserAccountInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getDisasterRecovery()Ljava/lang/String;
    .locals 2

    .prologue
    .line 162
    const-string v0, "1"

    sget-object v1, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_EPG_DisasterRecovery:Ljava/lang/String;

    invoke-direct {p0, v1}, Lcom/shcmcc/tools/UserAccountInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

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

.method public getEpgCityCode()Ljava/lang/String;
    .locals 1

    .prologue
    .line 157
    sget-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_EPG_CityCode:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/UserAccountInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getEpgProvince()Ljava/lang/String;
    .locals 1

    .prologue
    .line 152
    sget-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_EPG_province:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/UserAccountInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getFrameWorkVersion()Ljava/lang/String;
    .locals 5

    .prologue
    .line 103
    const-string v1, "0"

    .line 105
    .local v1, "verName":Ljava/lang/String;
    :try_start_0
    iget-object v2, p0, Lcom/shcmcc/tools/UserAccountInfo;->context:Landroid/content/Context;

    invoke-virtual {v2}, Landroid/content/Context;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object v2

    const-string v3, "com.shcmcc.setting"

    const/4 v4, 0x1

    invoke-virtual {v2, v3, v4}, Landroid/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;

    move-result-object v2

    iget-object v1, v2, Landroid/content/pm/PackageInfo;->versionName:Ljava/lang/String;
    :try_end_0
    .catch Landroid/content/pm/PackageManager$NameNotFoundException; {:try_start_0 .. :try_end_0} :catch_0

    .line 109
    :goto_0
    return-object v1

    .line 106
    :catch_0
    move-exception v0

    .line 107
    .local v0, "e":Landroid/content/pm/PackageManager$NameNotFoundException;
    const-string v2, "UserAccountInfo"

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v0}, Landroid/content/pm/PackageManager$NameNotFoundException;->getMessage()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v2, v3}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_0
.end method

.method public getHardwareManufacturer()Ljava/lang/String;
    .locals 2

    .prologue
    .line 113
    sget-object v1, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_HardwareManufacturer:Ljava/lang/String;

    invoke-direct {p0, v1}, Lcom/shcmcc/tools/UserAccountInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 114
    .local v0, "ret":Ljava/lang/String;
    if-eqz v0, :cond_0

    const-string v1, ""

    invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_0

    const-string v1, "unknown"

    invoke-virtual {v1, v0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_2

    .line 116
    :cond_0
    sget-object v1, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_HardwareBrand:Ljava/lang/String;

    invoke-direct {p0, v1}, Lcom/shcmcc/tools/UserAccountInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 117
    if-eqz v0, :cond_1

    const-string v1, ""

    invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_2

    .line 118
    :cond_1
    const-string v0, "0"

    .line 120
    :cond_2
    return-object v0
.end method

.method public getHardwareManufacturerId()Ljava/lang/String;
    .locals 4

    .prologue
    .line 124
    invoke-virtual {p0}, Lcom/shcmcc/tools/UserAccountInfo;->getSnNum()Ljava/lang/String;

    move-result-object v1

    .line 125
    .local v1, "ret":Ljava/lang/String;
    if-eqz v1, :cond_0

    const-string v2, ""

    invoke-virtual {v2, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_1

    .line 127
    :cond_0
    const-string v1, "0"

    .line 142
    :goto_0
    return-object v1

    .line 131
    :cond_1
    invoke-virtual {v1}, Ljava/lang/String;->length()I

    move-result v2

    const/16 v3, 0xb

    if-le v2, v3, :cond_2

    const/4 v2, 0x6

    const/16 v3, 0xc

    invoke-virtual {v1, v2, v3}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v1

    .line 134
    :cond_2
    :try_start_0
    invoke-static {v1}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    .line 136
    :catch_0
    move-exception v0

    .line 138
    .local v0, "E":Ljava/lang/Exception;
    const-string v1, "0"

    .line 139
    const-string v2, "getHardwareManufacturerId"

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v2, v3}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_0
.end method

.method public getHardwareVersion()Ljava/lang/String;
    .locals 1

    .prologue
    .line 99
    sget-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_HardwareVersion:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/UserAccountInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getLoginAcount()Ljava/lang/String;
    .locals 3

    .prologue
    .line 40
    new-instance v0, Ljava/lang/StringBuilder;

    .line 37
    iget-object v1, p0, Lcom/shcmcc/tools/UserAccountInfo;->context:Landroid/content/Context;

    invoke-virtual {v1}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v1

    sget-object v2, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_SECURE_NTVUSERACCOUNT:Ljava/lang/String;

    invoke-static {v1, v2}, Landroid/provider/Settings$Secure;->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    invoke-static {v1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v1

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    .line 38
    iget-object v1, p0, Lcom/shcmcc/tools/UserAccountInfo;->context:Landroid/content/Context;

    invoke-virtual {v1}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v1

    .line 39
    sget-object v2, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_SECURE_NTVUSERSUFFIX:Ljava/lang/String;

    .line 38
    invoke-static {v1, v2}, Landroid/provider/Settings$Secure;->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getLoginPassword()Ljava/lang/String;
    .locals 2

    .prologue
    .line 44
    iget-object v0, p0, Lcom/shcmcc/tools/UserAccountInfo;->context:Landroid/content/Context;

    invoke-virtual {v0}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v0

    sget-object v1, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_SECURE_NTVUSERPASSWORD:Ljava/lang/String;

    .line 43
    invoke-static {v0, v1}, Landroid/provider/Settings$Secure;->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getLoginUrl()Ljava/lang/String;
    .locals 3

    .prologue
    .line 31
    iget-object v1, p0, Lcom/shcmcc/tools/UserAccountInfo;->context:Landroid/content/Context;

    invoke-virtual {v1}, Landroid/content/Context;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v1

    const-string v2, "home_page"

    invoke-static {v1, v2}, Landroid/provider/Settings$Secure;->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 32
    .local v0, "homepage":Ljava/lang/String;
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-static {v0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v1

    invoke-direct {v2, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    const-string v1, "/"

    invoke-virtual {v0, v1}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_0

    const-string v1, ""

    :goto_0
    invoke-virtual {v2, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    const-string v2, "scspProxy"

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    return-object v1

    :cond_0
    const-string v1, "/"

    goto :goto_0
.end method

.method public getSnNum()Ljava/lang/String;
    .locals 1

    .prologue
    .line 95
    sget-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_serialno:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/UserAccountInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getTerminalType()Ljava/lang/String;
    .locals 1

    .prologue
    .line 146
    sget-object v0, Lcom/shcmcc/tools/UserAccountInfo;->PNAME_TerminalType:Ljava/lang/String;

    invoke-direct {p0, v0}, Lcom/shcmcc/tools/UserAccountInfo;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method
