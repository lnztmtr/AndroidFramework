.class public Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;
.super Landroid/app/Service;
.source "DiagnoseUsbTV.java"

# interfaces
.implements Ljava/lang/Runnable;


# instance fields
.field private INSTALL_APK_NAME:Ljava/lang/String;

.field private INSTALL_APP_CODE:Ljava/lang/String;

.field private final INSTALL_SERVICE_ACTION:Ljava/lang/String;

.field public PKG_MGR:Landroid/content/pm/PackageManager;

.field private mInstallConn:Landroid/content/ServiceConnection;

.field private mInstallService:Lnet/dlb/aidl/InstallService;

.field private mountPath:Ljava/lang/String;

.field private final upgradeFile:Ljava/lang/String;


# direct methods
.method public constructor <init>()V
    .locals 2

    .prologue
    const/4 v1, 0x0

    .line 27
    invoke-direct {p0}, Landroid/app/Service;-><init>()V

    .line 28
    const-string v0, "net.dlb.shcmcc.installservice"

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->INSTALL_SERVICE_ACTION:Ljava/lang/String;

    .line 29
    const-string v0, "cmccFramework"

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->INSTALL_APK_NAME:Ljava/lang/String;

    .line 30
    const-string v0, "1350537921780591"

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->INSTALL_APP_CODE:Ljava/lang/String;

    .line 32
    const-string v0, "/diagnose"

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->upgradeFile:Ljava/lang/String;

    .line 33
    iput-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->mountPath:Ljava/lang/String;

    .line 34
    iput-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->PKG_MGR:Landroid/content/pm/PackageManager;

    .line 36
    new-instance v0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV$1;

    invoke-direct {v0, p0}, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV$1;-><init>(Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;)V

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->mInstallConn:Landroid/content/ServiceConnection;

    return-void
.end method

.method static synthetic access$002(Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;Lnet/dlb/aidl/InstallService;)Lnet/dlb/aidl/InstallService;
    .locals 0
    .param p0, "x0"    # Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;
    .param p1, "x1"    # Lnet/dlb/aidl/InstallService;

    .prologue
    .line 27
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->mInstallService:Lnet/dlb/aidl/InstallService;

    return-object p1
.end method

.method static synthetic access$100(Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;Ljava/lang/String;)Ljava/util/Map;
    .locals 1
    .param p0, "x0"    # Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;
    .param p1, "x1"    # Ljava/lang/String;

    .prologue
    .line 27
    invoke-direct {p0, p1}, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->startAppByPkgName(Ljava/lang/String;)Ljava/util/Map;

    move-result-object v0

    return-object v0
.end method

.method private bindInstallService()V
    .locals 3

    .prologue
    .line 157
    new-instance v0, Landroid/content/Intent;

    const-string v1, "net.dlb.shcmcc.installservice"

    invoke-direct {v0, v1}, Landroid/content/Intent;-><init>(Ljava/lang/String;)V

    .line 158
    .local v0, "service":Landroid/content/Intent;
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->mInstallConn:Landroid/content/ServiceConnection;

    const/4 v2, 0x1

    invoke-virtual {p0, v0, v1, v2}, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->bindService(Landroid/content/Intent;Landroid/content/ServiceConnection;I)Z

    .line 159
    return-void
.end method

.method private getDiskFilePkgInfo(Ljava/lang/String;)Ljava/util/Map;
    .locals 5
    .param p1, "apkPath"    # Ljava/lang/String;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            ")",
            "Ljava/util/Map",
            "<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation

    .prologue
    .line 189
    new-instance v2, Ljava/util/HashMap;

    const/4 v3, 0x3

    invoke-direct {v2, v3}, Ljava/util/HashMap;-><init>(I)V

    .line 192
    .local v2, "ret":Ljava/util/Map;, "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;"
    :try_start_0
    iget-object v3, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->PKG_MGR:Landroid/content/pm/PackageManager;

    const/4 v4, 0x1

    invoke-virtual {v3, p1, v4}, Landroid/content/pm/PackageManager;->getPackageArchiveInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;

    move-result-object v1

    .line 194
    .local v1, "info":Landroid/content/pm/PackageInfo;
    const-string v3, "package_name"

    iget-object v4, v1, Landroid/content/pm/PackageInfo;->packageName:Ljava/lang/String;

    invoke-interface {v2, v3, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 195
    const-string v3, "version"

    iget-object v4, v1, Landroid/content/pm/PackageInfo;->versionName:Ljava/lang/String;

    invoke-interface {v2, v3, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 196
    const-string v3, "md5sum"

    invoke-static {p1}, Lcom/duolebo/tools/MD5sum;->md5sum(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v4

    invoke-interface {v2, v3, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    .line 201
    .end local v1    # "info":Landroid/content/pm/PackageInfo;
    :goto_0
    return-object v2

    .line 197
    :catch_0
    move-exception v0

    .line 198
    .local v0, "e":Ljava/lang/Exception;
    invoke-virtual {v0}, Ljava/lang/Exception;->printStackTrace()V

    goto :goto_0
.end method

.method private getInstalledPkgInfo(Ljava/lang/String;)Ljava/util/Map;
    .locals 5
    .param p1, "packageName"    # Ljava/lang/String;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            ")",
            "Ljava/util/Map",
            "<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation

    .prologue
    .line 174
    new-instance v2, Ljava/util/HashMap;

    const/4 v3, 0x3

    invoke-direct {v2, v3}, Ljava/util/HashMap;-><init>(I)V

    .line 177
    .local v2, "ret":Ljava/util/Map;, "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;"
    :try_start_0
    iget-object v3, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->PKG_MGR:Landroid/content/pm/PackageManager;

    const/4 v4, 0x1

    invoke-virtual {v3, p1, v4}, Landroid/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;

    move-result-object v1

    .line 179
    .local v1, "info":Landroid/content/pm/PackageInfo;
    const-string v3, "package_name"

    iget-object v4, v1, Landroid/content/pm/PackageInfo;->packageName:Ljava/lang/String;

    invoke-interface {v2, v3, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 180
    const-string v3, "version"

    iget-object v4, v1, Landroid/content/pm/PackageInfo;->versionName:Ljava/lang/String;

    invoke-interface {v2, v3, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_0
    .catch Landroid/content/pm/PackageManager$NameNotFoundException; {:try_start_0 .. :try_end_0} :catch_0

    .line 186
    .end local v1    # "info":Landroid/content/pm/PackageInfo;
    :goto_0
    return-object v2

    .line 182
    :catch_0
    move-exception v0

    .line 183
    .local v0, "e":Landroid/content/pm/PackageManager$NameNotFoundException;
    invoke-virtual {v0}, Landroid/content/pm/PackageManager$NameNotFoundException;->printStackTrace()V

    goto :goto_0
.end method

.method private getSystemProperties(Ljava/lang/String;)Ljava/lang/String;
    .locals 8
    .param p1, "key"    # Ljava/lang/String;

    .prologue
    .line 205
    const-string v3, ""

    .line 206
    .local v3, "str":Ljava/lang/String;
    if-eqz p1, :cond_0

    const-string v4, ""

    invoke-virtual {p1}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v4, v5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v4

    if-nez v4, :cond_0

    .line 208
    :try_start_0
    const-string v4, "android.os.SystemProperties"

    invoke-static {v4}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v0

    .line 209
    .local v0, "c":Ljava/lang/Class;, "Ljava/lang/Class<*>;"
    const-string v4, "get"

    const/4 v5, 0x1

    new-array v5, v5, [Ljava/lang/Class;

    const/4 v6, 0x0

    const-class v7, Ljava/lang/String;

    aput-object v7, v5, v6

    invoke-virtual {v0, v4, v5}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v2

    .line 210
    .local v2, "get":Ljava/lang/reflect/Method;
    const/4 v4, 0x1

    new-array v4, v4, [Ljava/lang/Object;

    const/4 v5, 0x0

    aput-object p1, v4, v5

    invoke-virtual {v2, v0, v4}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3

    .end local v3    # "str":Ljava/lang/String;
    check-cast v3, Ljava/lang/String;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    .line 217
    .end local v0    # "c":Ljava/lang/Class;, "Ljava/lang/Class<*>;"
    .end local v2    # "get":Ljava/lang/reflect/Method;
    .restart local v3    # "str":Ljava/lang/String;
    :cond_0
    :goto_0
    if-nez v3, :cond_1

    .line 218
    const-string v3, ""

    .line 221
    :cond_1
    return-object v3

    .line 211
    .end local v3    # "str":Ljava/lang/String;
    :catch_0
    move-exception v1

    .line 212
    .local v1, "e":Ljava/lang/Exception;
    const-string v3, ""

    .line 213
    .restart local v3    # "str":Ljava/lang/String;
    invoke-virtual {v1}, Ljava/lang/Exception;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-static {v4}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/String;)V

    goto :goto_0
.end method

.method private startAppByPkgName(Ljava/lang/String;)Ljava/util/Map;
    .locals 4
    .param p1, "packageName"    # Ljava/lang/String;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/lang/String;",
            ")",
            "Ljava/util/Map",
            "<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation

    .prologue
    .line 162
    new-instance v2, Ljava/util/HashMap;

    const/4 v3, 0x3

    invoke-direct {v2, v3}, Ljava/util/HashMap;-><init>(I)V

    .line 165
    .local v2, "ret":Ljava/util/Map;, "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;"
    :try_start_0
    iget-object v3, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->PKG_MGR:Landroid/content/pm/PackageManager;

    invoke-virtual {v3, p1}, Landroid/content/pm/PackageManager;->getLaunchIntentForPackage(Ljava/lang/String;)Landroid/content/Intent;

    move-result-object v1

    .line 166
    .local v1, "intent":Landroid/content/Intent;
    invoke-virtual {p0, v1}, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->startActivity(Landroid/content/Intent;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    .line 171
    .end local v1    # "intent":Landroid/content/Intent;
    :goto_0
    return-object v2

    .line 167
    :catch_0
    move-exception v0

    .line 168
    .local v0, "e":Ljava/lang/Exception;
    invoke-virtual {v0}, Ljava/lang/Exception;->printStackTrace()V

    goto :goto_0
.end method


# virtual methods
.method public onBind(Landroid/content/Intent;)Landroid/os/IBinder;
    .locals 1
    .param p1, "intent"    # Landroid/content/Intent;

    .prologue
    .line 51
    const/4 v0, 0x0

    return-object v0
.end method

.method public onCreate()V
    .locals 1

    .prologue
    .line 72
    invoke-virtual {p0}, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object v0

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->PKG_MGR:Landroid/content/pm/PackageManager;

    .line 73
    invoke-direct {p0}, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->bindInstallService()V

    .line 74
    return-void
.end method

.method public onStartCommand(Landroid/content/Intent;II)I
    .locals 3
    .param p1, "intent"    # Landroid/content/Intent;
    .param p2, "flags"    # I
    .param p3, "startId"    # I

    .prologue
    .line 55
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "....onStartCommand()....flags="

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, p2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v1

    const-string v2, ",startId="

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, p3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 58
    const-string v1, "epg.login"

    invoke-direct {p0, v1}, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->getSystemProperties(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 60
    .local v0, "login":Ljava/lang/String;
    const-string v1, "1"

    invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_0

    if-eqz p1, :cond_0

    .line 61
    const-string v1, "mount"

    invoke-virtual {p1, v1}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    iput-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->mountPath:Ljava/lang/String;

    .line 62
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->mountPath:Ljava/lang/String;

    if-eqz v1, :cond_0

    .line 63
    new-instance v1, Ljava/lang/Thread;

    invoke-direct {v1, p0}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V

    invoke-virtual {v1}, Ljava/lang/Thread;->start()V

    .line 67
    :cond_0
    invoke-super {p0, p1, p2, p3}, Landroid/app/Service;->onStartCommand(Landroid/content/Intent;II)I

    move-result v1

    return v1
.end method

.method public run()V
    .locals 15

    .prologue
    .line 80
    :try_start_0
    new-instance v7, Ljava/io/File;

    iget-object v11, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->mountPath:Ljava/lang/String;

    invoke-direct {v7, v11}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    .line 81
    .local v7, "mountFile":Ljava/io/File;
    new-instance v2, Ljava/io/File;

    new-instance v11, Ljava/lang/StringBuilder;

    invoke-direct {v11}, Ljava/lang/StringBuilder;-><init>()V

    iget-object v12, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->mountPath:Ljava/lang/String;

    invoke-virtual {v11, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v11

    const-string v12, "/diagnose"

    invoke-virtual {v11, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v11

    invoke-virtual {v11}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v11

    invoke-direct {v2, v11}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    .line 82
    .local v2, "diagnoseFile":Ljava/io/File;
    const-string v11, "mountPath=%s,mountPath is exists=%s,upgrade path=%s,path is exists=%s"

    const/4 v12, 0x4

    new-array v12, v12, [Ljava/lang/Object;

    const/4 v13, 0x0

    .line 83
    invoke-virtual {v7}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v14

    aput-object v14, v12, v13

    const/4 v13, 0x1

    invoke-virtual {v7}, Ljava/io/File;->exists()Z

    move-result v14

    invoke-static {v14}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object v14

    aput-object v14, v12, v13

    const/4 v13, 0x2

    invoke-virtual {v2}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v14

    aput-object v14, v12, v13

    const/4 v13, 0x3

    invoke-virtual {v2}, Ljava/io/File;->exists()Z

    move-result v14

    invoke-static {v14}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object v14

    aput-object v14, v12, v13

    .line 82
    invoke-static {v11, v12}, Ljava/lang/String;->format(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v11

    invoke-static {v11}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 85
    invoke-virtual {v2}, Ljava/io/File;->exists()Z

    move-result v11

    if-eqz v11, :cond_0

    .line 87
    new-instance v6, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV$2;

    invoke-direct {v6, p0}, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV$2;-><init>(Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;)V

    .line 96
    .local v6, "filter":Ljava/io/FilenameFilter;
    invoke-virtual {v2, v6}, Ljava/io/File;->list(Ljava/io/FilenameFilter;)[Ljava/lang/String;

    move-result-object v4

    .line 97
    .local v4, "fileNames":[Ljava/lang/String;
    if-eqz v4, :cond_0

    array-length v11, v4

    if-lez v11, :cond_0

    .line 98
    new-instance v11, Ljava/lang/StringBuilder;

    invoke-direct {v11}, Ljava/lang/StringBuilder;-><init>()V

    iget-object v12, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->mountPath:Ljava/lang/String;

    invoke-virtual {v11, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v11

    const-string v12, "/diagnose"

    invoke-virtual {v11, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v11

    const-string v12, "/"

    invoke-virtual {v11, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v11

    const/4 v12, 0x0

    aget-object v12, v4, v12

    invoke-virtual {v11, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v11

    invoke-virtual {v11}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    .line 99
    .local v1, "apkFileName":Ljava/lang/String;
    new-instance v11, Ljava/lang/StringBuilder;

    invoke-direct {v11}, Ljava/lang/StringBuilder;-><init>()V

    const-string v12, "find file="

    invoke-virtual {v11, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v11

    invoke-virtual {v11, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v11

    invoke-virtual {v11}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v11

    invoke-static {v11}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 100
    invoke-direct {p0, v1}, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->getDiskFilePkgInfo(Ljava/lang/String;)Ljava/util/Map;

    move-result-object v5

    .line 101
    .local v5, "filePkgInfo":Ljava/util/Map;, "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;"
    const-string v11, "package_name"

    invoke-interface {v5, v11}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v8

    check-cast v8, Ljava/lang/String;

    .line 102
    .local v8, "pkgName":Ljava/lang/String;
    new-instance v11, Ljava/lang/StringBuilder;

    invoke-direct {v11}, Ljava/lang/StringBuilder;-><init>()V

    const-string v12, "find file="

    invoke-virtual {v11, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v11

    invoke-virtual {v11, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v11

    invoke-virtual {v11}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v11

    invoke-static {v11}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 104
    if-nez v8, :cond_1

    .line 105
    new-instance v11, Ljava/lang/StringBuilder;

    invoke-direct {v11}, Ljava/lang/StringBuilder;-><init>()V

    const-string v12, "ignore:not a valid file="

    invoke-virtual {v11, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v11

    invoke-virtual {v11, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v11

    invoke-virtual {v11}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v11

    invoke-static {v11}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_2

    .line 154
    .end local v1    # "apkFileName":Ljava/lang/String;
    .end local v2    # "diagnoseFile":Ljava/io/File;
    .end local v4    # "fileNames":[Ljava/lang/String;
    .end local v5    # "filePkgInfo":Ljava/util/Map;, "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;"
    .end local v6    # "filter":Ljava/io/FilenameFilter;
    .end local v7    # "mountFile":Ljava/io/File;
    .end local v8    # "pkgName":Ljava/lang/String;
    :cond_0
    :goto_0
    return-void

    .line 117
    .restart local v1    # "apkFileName":Ljava/lang/String;
    .restart local v2    # "diagnoseFile":Ljava/io/File;
    .restart local v4    # "fileNames":[Ljava/lang/String;
    .restart local v5    # "filePkgInfo":Ljava/util/Map;, "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;"
    .restart local v6    # "filter":Ljava/io/FilenameFilter;
    .restart local v7    # "mountFile":Ljava/io/File;
    .restart local v8    # "pkgName":Ljava/lang/String;
    :cond_1
    const/16 v0, 0x1388

    .line 118
    .local v0, "MaxSleepTime":I
    const/16 v9, 0x64

    .line 119
    .local v9, "sleepInterval":I
    const/4 v10, 0x0

    .line 120
    .local v10, "sleepTime":I
    :goto_1
    :try_start_1
    iget-object v11, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->mInstallService:Lnet/dlb/aidl/InstallService;
    :try_end_1
    .catch Landroid/os/RemoteException; {:try_start_1 .. :try_end_1} :catch_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_2

    if-nez v11, :cond_2

    if-ge v10, v0, :cond_2

    .line 121
    const-wide/16 v11, 0x64

    :try_start_2
    invoke-static {v11, v12}, Ljava/lang/Thread;->sleep(J)V
    :try_end_2
    .catch Ljava/lang/InterruptedException; {:try_start_2 .. :try_end_2} :catch_0
    .catch Landroid/os/RemoteException; {:try_start_2 .. :try_end_2} :catch_1
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_2

    .line 122
    add-int/lit8 v10, v10, 0x64

    goto :goto_1

    .line 123
    :catch_0
    move-exception v3

    .line 124
    .local v3, "e":Ljava/lang/InterruptedException;
    :try_start_3
    invoke-virtual {v3}, Ljava/lang/InterruptedException;->printStackTrace()V
    :try_end_3
    .catch Landroid/os/RemoteException; {:try_start_3 .. :try_end_3} :catch_1
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_2

    goto :goto_1

    .line 144
    .end local v3    # "e":Ljava/lang/InterruptedException;
    :catch_1
    move-exception v3

    .line 145
    .local v3, "e":Landroid/os/RemoteException;
    :try_start_4
    invoke-virtual {v3}, Landroid/os/RemoteException;->printStackTrace()V
    :try_end_4
    .catch Ljava/lang/Exception; {:try_start_4 .. :try_end_4} :catch_2

    goto :goto_0

    .line 151
    .end local v0    # "MaxSleepTime":I
    .end local v1    # "apkFileName":Ljava/lang/String;
    .end local v2    # "diagnoseFile":Ljava/io/File;
    .end local v3    # "e":Landroid/os/RemoteException;
    .end local v4    # "fileNames":[Ljava/lang/String;
    .end local v5    # "filePkgInfo":Ljava/util/Map;, "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;"
    .end local v6    # "filter":Ljava/io/FilenameFilter;
    .end local v7    # "mountFile":Ljava/io/File;
    .end local v8    # "pkgName":Ljava/lang/String;
    .end local v9    # "sleepInterval":I
    .end local v10    # "sleepTime":I
    :catch_2
    move-exception v3

    .line 152
    .local v3, "e":Ljava/lang/Exception;
    invoke-virtual {v3}, Ljava/lang/Exception;->printStackTrace()V

    goto :goto_0

    .line 126
    .end local v3    # "e":Ljava/lang/Exception;
    .restart local v0    # "MaxSleepTime":I
    .restart local v1    # "apkFileName":Ljava/lang/String;
    .restart local v2    # "diagnoseFile":Ljava/io/File;
    .restart local v4    # "fileNames":[Ljava/lang/String;
    .restart local v5    # "filePkgInfo":Ljava/util/Map;, "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;"
    .restart local v6    # "filter":Ljava/io/FilenameFilter;
    .restart local v7    # "mountFile":Ljava/io/File;
    .restart local v8    # "pkgName":Ljava/lang/String;
    .restart local v9    # "sleepInterval":I
    .restart local v10    # "sleepTime":I
    :cond_2
    :try_start_5
    iget-object v11, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->mInstallService:Lnet/dlb/aidl/InstallService;

    if-nez v11, :cond_3

    .line 127
    const-string v11, "mInstallService is null"

    invoke-static {v11}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/String;)V

    goto :goto_0

    .line 130
    :cond_3
    iget-object v11, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->mInstallService:Lnet/dlb/aidl/InstallService;

    iget-object v12, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->INSTALL_APK_NAME:Ljava/lang/String;

    iget-object v13, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->INSTALL_APP_CODE:Ljava/lang/String;

    new-instance v14, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV$3;

    invoke-direct {v14, p0, v8}, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV$3;-><init>(Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;Ljava/lang/String;)V

    invoke-interface {v11, v1, v12, v13, v14}, Lnet/dlb/aidl/InstallService;->install(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/dlb/aidl/InstallCallback;)V
    :try_end_5
    .catch Landroid/os/RemoteException; {:try_start_5 .. :try_end_5} :catch_1
    .catch Ljava/lang/Exception; {:try_start_5 .. :try_end_5} :catch_2

    goto :goto_0
.end method
