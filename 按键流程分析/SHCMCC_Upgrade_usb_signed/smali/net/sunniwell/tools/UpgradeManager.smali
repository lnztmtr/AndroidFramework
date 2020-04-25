.class public Lnet/sunniwell/tools/UpgradeManager;
.super Ljava/lang/Object;
.source "UpgradeManager.java"


# static fields
.field private static COMMAND_FILE:Ljava/io/File; = null

.field private static LAST_PREFIX:Ljava/lang/String; = null

.field private static LOG_FILE:Ljava/io/File; = null

.field private static RECOVERY_DIR:Ljava/io/File; = null

.field private static final UPDATE_PACKAGE:Ljava/lang/String; = "--update_package="

.field private static log:Lnet/sunniwell/common/log/SWLogger;


# direct methods
.method static constructor <clinit>()V
    .locals 3

    .prologue
    .line 10
    const-class v0, Lnet/sunniwell/tools/UpgradeManager;

    invoke-static {v0}, Lnet/sunniwell/common/log/SWLogger;->getLogger(Ljava/lang/Class;)Lnet/sunniwell/common/log/SWLogger;

    move-result-object v0

    sput-object v0, Lnet/sunniwell/tools/UpgradeManager;->log:Lnet/sunniwell/common/log/SWLogger;

    .line 11
    new-instance v0, Ljava/io/File;

    const-string v1, "/cache/recovery"

    invoke-direct {v0, v1}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    sput-object v0, Lnet/sunniwell/tools/UpgradeManager;->RECOVERY_DIR:Ljava/io/File;

    .line 12
    new-instance v0, Ljava/io/File;

    sget-object v1, Lnet/sunniwell/tools/UpgradeManager;->RECOVERY_DIR:Ljava/io/File;

    const-string v2, "command"

    invoke-direct {v0, v1, v2}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    sput-object v0, Lnet/sunniwell/tools/UpgradeManager;->COMMAND_FILE:Ljava/io/File;

    .line 13
    new-instance v0, Ljava/io/File;

    sget-object v1, Lnet/sunniwell/tools/UpgradeManager;->RECOVERY_DIR:Ljava/io/File;

    const-string v2, "log"

    invoke-direct {v0, v1, v2}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    sput-object v0, Lnet/sunniwell/tools/UpgradeManager;->LOG_FILE:Ljava/io/File;

    .line 15
    const-string v0, "last_"

    sput-object v0, Lnet/sunniwell/tools/UpgradeManager;->LAST_PREFIX:Ljava/lang/String;

    return-void
.end method

.method public constructor <init>()V
    .locals 0

    .prologue
    .line 9
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static setUpgradeMode(ZLjava/io/File;)Z
    .locals 7
    .param p0, "isUpgrade"    # Z
    .param p1, "zipFile"    # Ljava/io/File;

    .prologue
    const/4 v6, 0x0

    .line 18
    if-eqz p0, :cond_2

    .line 19
    if-nez p1, :cond_0

    .line 20
    const-string v4, "zipFile is null."

    invoke-static {v4}, Lcom/duolebo/tools/DLBLog;->i(Ljava/lang/String;)V

    .line 49
    :goto_0
    return v6

    .line 24
    :cond_0
    :try_start_0
    invoke-virtual {p1}, Ljava/io/File;->exists()Z

    move-result v4

    if-eqz v4, :cond_1

    .line 25
    invoke-virtual {p1}, Ljava/io/File;->getCanonicalPath()Ljava/lang/String;

    move-result-object v3

    .line 26
    .local v3, "filename":Ljava/lang/String;
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, "----INSTALL=="

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    const-string v5, "!!!"

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-static {v4}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 27
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, "--update_package="

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    .line 28
    .local v0, "arg":Ljava/lang/String;
    sget-object v4, Lnet/sunniwell/tools/UpgradeManager;->RECOVERY_DIR:Ljava/io/File;

    invoke-virtual {v4}, Ljava/io/File;->mkdirs()Z

    .line 29
    sget-object v4, Lnet/sunniwell/tools/UpgradeManager;->COMMAND_FILE:Ljava/io/File;

    invoke-virtual {v4}, Ljava/io/File;->delete()Z

    .line 30
    sget-object v4, Lnet/sunniwell/tools/UpgradeManager;->LOG_FILE:Ljava/io/File;

    invoke-virtual {v4}, Ljava/io/File;->delete()Z

    .line 31
    new-instance v1, Ljava/io/FileWriter;

    sget-object v4, Lnet/sunniwell/tools/UpgradeManager;->COMMAND_FILE:Ljava/io/File;

    invoke-direct {v1, v4}, Ljava/io/FileWriter;-><init>(Ljava/io/File;)V
    :try_end_0
    .catch Ljava/io/IOException; {:try_start_0 .. :try_end_0} :catch_0

    .line 33
    .local v1, "command":Ljava/io/FileWriter;
    :try_start_1
    invoke-virtual {v1, v0}, Ljava/io/FileWriter;->write(Ljava/lang/String;)V

    .line 34
    const-string v4, "\n"

    invoke-virtual {v1, v4}, Ljava/io/FileWriter;->write(Ljava/lang/String;)V
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    .line 36
    :try_start_2
    invoke-virtual {v1}, Ljava/io/FileWriter;->close()V
    :try_end_2
    .catch Ljava/io/IOException; {:try_start_2 .. :try_end_2} :catch_0

    goto :goto_0

    .line 43
    .end local v0    # "arg":Ljava/lang/String;
    .end local v1    # "command":Ljava/io/FileWriter;
    .end local v3    # "filename":Ljava/lang/String;
    :catch_0
    move-exception v2

    .line 44
    .local v2, "e":Ljava/io/IOException;
    invoke-static {v2}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_0

    .line 36
    .end local v2    # "e":Ljava/io/IOException;
    .restart local v0    # "arg":Ljava/lang/String;
    .restart local v1    # "command":Ljava/io/FileWriter;
    .restart local v3    # "filename":Ljava/lang/String;
    :catchall_0
    move-exception v4

    :try_start_3
    invoke-virtual {v1}, Ljava/io/FileWriter;->close()V

    throw v4

    .line 39
    .end local v0    # "arg":Ljava/lang/String;
    .end local v1    # "command":Ljava/io/FileWriter;
    .end local v3    # "filename":Ljava/lang/String;
    :cond_1
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, "zipFile("

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {p1}, Ljava/io/File;->getCanonicalPath()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    const-string v5, ") is not exists."

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-static {v4}, Lcom/duolebo/tools/DLBLog;->i(Ljava/lang/String;)V
    :try_end_3
    .catch Ljava/io/IOException; {:try_start_3 .. :try_end_3} :catch_0

    goto/16 :goto_0

    .line 47
    :cond_2
    sget-object v4, Lnet/sunniwell/tools/UpgradeManager;->COMMAND_FILE:Ljava/io/File;

    invoke-virtual {v4}, Ljava/io/File;->delete()Z

    goto/16 :goto_0
.end method
