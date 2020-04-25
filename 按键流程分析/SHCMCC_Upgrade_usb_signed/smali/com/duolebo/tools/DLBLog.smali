.class public Lcom/duolebo/tools/DLBLog;
.super Ljava/lang/Object;
.source "DLBLog.java"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/duolebo/tools/DLBLog$LogLevel;,
        Lcom/duolebo/tools/DLBLog$LogMsg;
    }
.end annotation


# static fields
.field private static synthetic $SWITCH_TABLE$com$duolebo$tools$DLBLog$LogLevel:[I = null

.field private static final DEF_Format:Ljava/lang/String; = "noinit|%d|%p|%t@%c:%L-%M(?)|%m%n"

.field private static final DEF_Project:Ljava/lang/String; = "noinit"

.field private static final MSG_SPLIT_SIZE:I = 0x1000

.field private static final PName_D:Ljava/lang/String; = "stdout.DEBUG.format"

.field private static final PName_E:Ljava/lang/String; = "stdout.ERROR.format"

.field private static final PName_I:Ljava/lang/String; = "stdout.INFO.format"

.field private static final PName_Level:Ljava/lang/String; = "stdout.level"

.field private static final PName_Project:Ljava/lang/String; = "stdout.project.name"

.field private static final PName_Tag:Ljava/lang/String; = "stdout.tag.name"

.field private static final PName_V:Ljava/lang/String; = "stdout.VERBOSE.format"

.field private static final PName_W:Ljava/lang/String; = "stdout.WARN.format"

.field private static PValue_D:Ljava/lang/String;

.field private static PValue_E:Ljava/lang/String;

.field private static PValue_I:Ljava/lang/String;

.field private static PValue_Project:Ljava/lang/String;

.field private static PValue_Tag:Ljava/lang/String;

.field private static PValue_V:Ljava/lang/String;

.field private static PValue_W:Ljava/lang/String;

.field private static file_timestampe:Ljava/lang/String;

.field private static initLogInfos:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List",
            "<",
            "Lcom/duolebo/tools/DLBLog$LogMsg;",
            ">;"
        }
    .end annotation
.end field

.field private static isLoaded:Z

.field private static logLevel:Lcom/duolebo/tools/DLBLog$LogLevel;


# instance fields
.field private Log_Class_SimpleName:Ljava/lang/String;

.field private Log_Class_name:Ljava/lang/String;

.field private Log_Tag:Ljava/lang/String;


# direct methods
.method static synthetic $SWITCH_TABLE$com$duolebo$tools$DLBLog$LogLevel()[I
    .locals 3

    .prologue
    .line 11
    sget-object v0, Lcom/duolebo/tools/DLBLog;->$SWITCH_TABLE$com$duolebo$tools$DLBLog$LogLevel:[I

    if-eqz v0, :cond_0

    :goto_0
    return-object v0

    :cond_0
    invoke-static {}, Lcom/duolebo/tools/DLBLog$LogLevel;->values()[Lcom/duolebo/tools/DLBLog$LogLevel;

    move-result-object v0

    array-length v0, v0

    new-array v0, v0, [I

    :try_start_0
    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Debug:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-virtual {v1}, Lcom/duolebo/tools/DLBLog$LogLevel;->ordinal()I

    move-result v1

    const/4 v2, 0x2

    aput v2, v0, v1
    :try_end_0
    .catch Ljava/lang/NoSuchFieldError; {:try_start_0 .. :try_end_0} :catch_5

    :goto_1
    :try_start_1
    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Error:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-virtual {v1}, Lcom/duolebo/tools/DLBLog$LogLevel;->ordinal()I

    move-result v1

    const/4 v2, 0x5

    aput v2, v0, v1
    :try_end_1
    .catch Ljava/lang/NoSuchFieldError; {:try_start_1 .. :try_end_1} :catch_4

    :goto_2
    :try_start_2
    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Info:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-virtual {v1}, Lcom/duolebo/tools/DLBLog$LogLevel;->ordinal()I

    move-result v1

    const/4 v2, 0x3

    aput v2, v0, v1
    :try_end_2
    .catch Ljava/lang/NoSuchFieldError; {:try_start_2 .. :try_end_2} :catch_3

    :goto_3
    :try_start_3
    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->NoLog:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-virtual {v1}, Lcom/duolebo/tools/DLBLog$LogLevel;->ordinal()I

    move-result v1

    const/4 v2, 0x6

    aput v2, v0, v1
    :try_end_3
    .catch Ljava/lang/NoSuchFieldError; {:try_start_3 .. :try_end_3} :catch_2

    :goto_4
    :try_start_4
    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Verbose:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-virtual {v1}, Lcom/duolebo/tools/DLBLog$LogLevel;->ordinal()I

    move-result v1

    const/4 v2, 0x1

    aput v2, v0, v1
    :try_end_4
    .catch Ljava/lang/NoSuchFieldError; {:try_start_4 .. :try_end_4} :catch_1

    :goto_5
    :try_start_5
    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Warn:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-virtual {v1}, Lcom/duolebo/tools/DLBLog$LogLevel;->ordinal()I

    move-result v1

    const/4 v2, 0x4

    aput v2, v0, v1
    :try_end_5
    .catch Ljava/lang/NoSuchFieldError; {:try_start_5 .. :try_end_5} :catch_0

    :goto_6
    sput-object v0, Lcom/duolebo/tools/DLBLog;->$SWITCH_TABLE$com$duolebo$tools$DLBLog$LogLevel:[I

    goto :goto_0

    :catch_0
    move-exception v1

    goto :goto_6

    :catch_1
    move-exception v1

    goto :goto_5

    :catch_2
    move-exception v1

    goto :goto_4

    :catch_3
    move-exception v1

    goto :goto_3

    :catch_4
    move-exception v1

    goto :goto_2

    :catch_5
    move-exception v1

    goto :goto_1
.end method

.method static constructor <clinit>()V
    .locals 1

    .prologue
    .line 39
    const-string v0, "class.getSimpleName()"

    sput-object v0, Lcom/duolebo/tools/DLBLog;->PValue_Tag:Ljava/lang/String;

    .line 40
    const-string v0, "noinit"

    sput-object v0, Lcom/duolebo/tools/DLBLog;->PValue_Project:Ljava/lang/String;

    .line 41
    const-string v0, "noinit|%d|%p|%t@%c:%L-%M(?)|%m%n"

    sput-object v0, Lcom/duolebo/tools/DLBLog;->PValue_V:Ljava/lang/String;

    .line 42
    const-string v0, "noinit|%d|%p|%t@%c:%L-%M(?)|%m%n"

    sput-object v0, Lcom/duolebo/tools/DLBLog;->PValue_D:Ljava/lang/String;

    .line 43
    const-string v0, "noinit|%d|%p|%t@%c:%L-%M(?)|%m%n"

    sput-object v0, Lcom/duolebo/tools/DLBLog;->PValue_I:Ljava/lang/String;

    .line 44
    const-string v0, "noinit|%d|%p|%t@%c:%L-%M(?)|%m%n"

    sput-object v0, Lcom/duolebo/tools/DLBLog;->PValue_W:Ljava/lang/String;

    .line 45
    const-string v0, "noinit|%d|%p|%t@%c:%L-%M(?)|%m%n"

    sput-object v0, Lcom/duolebo/tools/DLBLog;->PValue_E:Ljava/lang/String;

    .line 55
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Info:Lcom/duolebo/tools/DLBLog$LogLevel;

    sput-object v0, Lcom/duolebo/tools/DLBLog;->logLevel:Lcom/duolebo/tools/DLBLog$LogLevel;

    .line 60
    const/4 v0, 0x0

    sput-boolean v0, Lcom/duolebo/tools/DLBLog;->isLoaded:Z

    .line 61
    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    sput-object v0, Lcom/duolebo/tools/DLBLog;->initLogInfos:Ljava/util/List;

    .line 62
    const/4 v0, 0x0

    sput-object v0, Lcom/duolebo/tools/DLBLog;->file_timestampe:Ljava/lang/String;

    .line 65
    invoke-static {}, Lcom/duolebo/tools/DLBLog;->init()V

    .line 66
    return-void
.end method

.method public constructor <init>()V
    .locals 1

    .prologue
    const/4 v0, 0x0

    .line 11
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 56
    iput-object v0, p0, Lcom/duolebo/tools/DLBLog;->Log_Tag:Ljava/lang/String;

    .line 57
    iput-object v0, p0, Lcom/duolebo/tools/DLBLog;->Log_Class_name:Ljava/lang/String;

    .line 58
    iput-object v0, p0, Lcom/duolebo/tools/DLBLog;->Log_Class_SimpleName:Ljava/lang/String;

    .line 11
    return-void
.end method

.method public static d(Ljava/lang/String;)V
    .locals 2
    .param p0, "msg"    # Ljava/lang/String;

    .prologue
    .line 227
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Debug:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {v0, v1, p0}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 228
    return-void
.end method

.method public static d(Ljava/lang/String;Ljava/lang/String;)V
    .locals 1
    .param p0, "tag"    # Ljava/lang/String;
    .param p1, "msg"    # Ljava/lang/String;

    .prologue
    .line 83
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Debug:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {p0, v0, p1}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 84
    return-void
.end method

.method public static d(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V
    .locals 3
    .param p0, "tag"    # Ljava/lang/String;
    .param p1, "msg"    # Ljava/lang/String;
    .param p2, "tr"    # Ljava/lang/Throwable;

    .prologue
    .line 91
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Debug:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v2

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {p2}, Landroid/util/Log;->getStackTraceString(Ljava/lang/Throwable;)Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {p0, v0, v1}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 92
    return-void
.end method

.method public static d(Ljava/lang/String;Ljava/lang/Throwable;)V
    .locals 4
    .param p0, "msg"    # Ljava/lang/String;
    .param p1, "tr"    # Ljava/lang/Throwable;

    .prologue
    .line 235
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Debug:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-static {p0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {p1}, Landroid/util/Log;->getStackTraceString(Ljava/lang/Throwable;)Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 236
    return-void
.end method

.method public static d(Ljava/lang/Throwable;)V
    .locals 3
    .param p0, "tr"    # Ljava/lang/Throwable;

    .prologue
    .line 231
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Debug:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {p0}, Landroid/util/Log;->getStackTraceString(Ljava/lang/Throwable;)Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 232
    return-void
.end method

.method public static e(Ljava/lang/String;)V
    .locals 2
    .param p0, "msg"    # Ljava/lang/String;

    .prologue
    .line 267
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Error:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {v0, v1, p0}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 268
    return-void
.end method

.method public static e(Ljava/lang/String;Ljava/lang/String;)V
    .locals 1
    .param p0, "tag"    # Ljava/lang/String;
    .param p1, "msg"    # Ljava/lang/String;

    .prologue
    .line 123
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Warn:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {p0, v0, p1}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 124
    return-void
.end method

.method public static e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V
    .locals 3
    .param p0, "tag"    # Ljava/lang/String;
    .param p1, "msg"    # Ljava/lang/String;
    .param p2, "tr"    # Ljava/lang/Throwable;

    .prologue
    .line 127
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Warn:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v2

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {p2}, Landroid/util/Log;->getStackTraceString(Ljava/lang/Throwable;)Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {p0, v0, v1}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 128
    return-void
.end method

.method public static e(Ljava/lang/String;Ljava/lang/Throwable;)V
    .locals 4
    .param p0, "msg"    # Ljava/lang/String;
    .param p1, "tr"    # Ljava/lang/Throwable;

    .prologue
    .line 271
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Error:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-static {p0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {p1}, Landroid/util/Log;->getStackTraceString(Ljava/lang/Throwable;)Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 272
    return-void
.end method

.method public static e(Ljava/lang/Throwable;)V
    .locals 3
    .param p0, "tr"    # Ljava/lang/Throwable;

    .prologue
    .line 263
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Error:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {p0}, Landroid/util/Log;->getStackTraceString(Ljava/lang/Throwable;)Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 264
    return-void
.end method

.method private static getMsg(Ljava/lang/StackTraceElement;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .locals 8
    .param p0, "stackEl"    # Ljava/lang/StackTraceElement;
    .param p1, "level"    # Ljava/lang/String;
    .param p2, "format"    # Ljava/lang/String;
    .param p3, "msg"    # Ljava/lang/String;

    .prologue
    const/4 v7, 0x0

    .line 574
    const-string v1, ""

    .line 577
    .local v1, "ret":Ljava/lang/String;
    :try_start_0
    new-instance v3, Ljava/text/SimpleDateFormat;

    const-string v4, "yyyy-MM-dd HH:mm:ss.SSS"

    invoke-direct {v3, v4}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;)V

    new-instance v4, Ljava/util/Date;

    invoke-direct {v4}, Ljava/util/Date;-><init>()V

    invoke-virtual {v3, v4}, Ljava/text/SimpleDateFormat;->format(Ljava/util/Date;)Ljava/lang/String;

    move-result-object v2

    .line 578
    .local v2, "strDate":Ljava/lang/String;
    const-string v3, "%P"

    sget-object v4, Lcom/duolebo/tools/DLBLog;->PValue_Project:Ljava/lang/String;

    invoke-virtual {p2, v3, v4}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v3

    const-string v4, "%p"

    invoke-virtual {v3, v4, p1}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v3

    const-string v4, "%t"

    invoke-static {}, Ljava/lang/Thread;->currentThread()Ljava/lang/Thread;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/Thread;->getName()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v3, v4, v5}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v3

    const-string v4, "%n"

    const-string v5, "line.separator"

    invoke-static {v5}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v3, v4, v5}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v3

    const-string v4, "%d"

    invoke-virtual {v3, v4, v2}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v1

    .line 579
    if-nez p0, :cond_0

    .line 581
    const-string v3, "%c"

    const-string v4, "noclass"

    invoke-virtual {v1, v3, v4}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v3

    const-string v4, "%F"

    const-string v5, "no"

    invoke-virtual {v3, v4, v5}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v3

    const-string v4, "%M"

    const-string v5, "nomethod"

    invoke-virtual {v3, v4, v5}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v3

    const-string v4, "%L"

    const-string v5, "0"

    invoke-virtual {v3, v4, v5}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v1

    .line 588
    :goto_0
    const-string v3, "%m"

    invoke-virtual {v1, v3, p3}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v1

    .line 596
    .end local v2    # "strDate":Ljava/lang/String;
    :goto_1
    return-object v1

    .line 586
    .restart local v2    # "strDate":Ljava/lang/String;
    :cond_0
    const-string v3, "%c"

    invoke-virtual {p0}, Ljava/lang/StackTraceElement;->getClassName()Ljava/lang/String;

    move-result-object v4

    const-string v5, "^.*\\."

    const-string v6, ""

    invoke-virtual {v4, v5, v6}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v1, v3, v4}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v3

    const-string v4, "%M"

    invoke-virtual {p0}, Ljava/lang/StackTraceElement;->getMethodName()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v3, v4, v5}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v3

    const-string v4, "%L"

    new-instance v5, Ljava/lang/StringBuilder;

    invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p0}, Ljava/lang/StackTraceElement;->getLineNumber()I

    move-result v6

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v3, v4, v5}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    move-result-object v1

    goto :goto_0

    .line 590
    .end local v2    # "strDate":Ljava/lang/String;
    :catch_0
    move-exception v0

    .line 592
    .local v0, "E":Ljava/lang/Exception;
    new-instance v3, Ljava/lang/StringBuilder;

    const-string v4, "throws Exception when combine msg:"

    invoke-direct {v3, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v7, v3}, Lcom/duolebo/tools/DLBLog;->w(Ljava/lang/String;Ljava/lang/String;)V

    .line 593
    new-instance v3, Ljava/lang/StringBuilder;

    const-string v4, "throws Exception when combine msg:"

    invoke-direct {v3, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v3, p3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v7, v3}, Lcom/duolebo/tools/DLBLog;->w(Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_1
.end method

.method private static getProperties()Ljava/util/Properties;
    .locals 7

    .prologue
    .line 453
    const/4 v5, 0x0

    sput-object v5, Lcom/duolebo/tools/DLBLog;->file_timestampe:Ljava/lang/String;

    .line 455
    const/4 v4, 0x0

    .line 456
    .local v4, "ret_p":Ljava/util/Properties;
    const-string v5, "/dlblog.properties"

    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->loadFileAsProperties(Ljava/lang/String;)Ljava/util/Properties;

    move-result-object v3

    .line 457
    .local v3, "data_r":Ljava/util/Properties;
    const-string v5, "/data/dlblog.properties"

    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->loadFileAsProperties(Ljava/lang/String;)Ljava/util/Properties;

    move-result-object v2

    .line 458
    .local v2, "data_p":Ljava/util/Properties;
    const-string v5, "/dlblog.properties"

    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->loadClassFileAsProperties(Ljava/lang/String;)Ljava/util/Properties;

    move-result-object v1

    .line 459
    .local v1, "classpath_p":Ljava/util/Properties;
    const-string v5, "dlblog.properties"

    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->loadClassFileAsProperties(Ljava/lang/String;)Ljava/util/Properties;

    move-result-object v0

    .line 461
    .local v0, "class_p":Ljava/util/Properties;
    if-nez v4, :cond_2

    if-eqz v3, :cond_2

    .line 462
    move-object v4, v3

    .line 463
    new-instance v5, Ljava/lang/StringBuilder;

    invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V

    const-string v6, "#_file_timestampe"

    invoke-virtual {v3, v6}, Ljava/util/Properties;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v6

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    sput-object v5, Lcom/duolebo/tools/DLBLog;->file_timestampe:Ljava/lang/String;

    .line 476
    :cond_0
    :goto_0
    if-eqz v4, :cond_1

    if-eqz v1, :cond_1

    if-eq v4, v1, :cond_1

    .line 478
    const-string v5, "stdout.project.name"

    const-string v6, "stdout.project.name"

    invoke-virtual {v1, v6}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v6

    invoke-virtual {v4, v5, v6}, Ljava/util/Properties;->setProperty(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;

    .line 481
    :cond_1
    return-object v4

    .line 465
    :cond_2
    if-nez v4, :cond_3

    if-eqz v2, :cond_3

    .line 466
    move-object v4, v2

    .line 467
    new-instance v5, Ljava/lang/StringBuilder;

    invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V

    const-string v6, "#_file_timestampe"

    invoke-virtual {v2, v6}, Ljava/util/Properties;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v6

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    sput-object v5, Lcom/duolebo/tools/DLBLog;->file_timestampe:Ljava/lang/String;

    goto :goto_0

    .line 469
    :cond_3
    if-nez v4, :cond_4

    if-eqz v1, :cond_4

    .line 470
    move-object v4, v1

    .line 471
    goto :goto_0

    .line 472
    :cond_4
    if-nez v4, :cond_0

    if-eqz v0, :cond_0

    .line 473
    move-object v4, v0

    goto :goto_0
.end method

.method private static getSTE()Ljava/lang/StackTraceElement;
    .locals 5

    .prologue
    const/4 v4, 0x3

    .line 554
    new-instance v0, Ljava/lang/Throwable;

    invoke-direct {v0}, Ljava/lang/Throwable;-><init>()V

    .line 555
    .local v0, "ex":Ljava/lang/Throwable;
    invoke-virtual {v0}, Ljava/lang/Throwable;->getStackTrace()[Ljava/lang/StackTraceElement;

    move-result-object v2

    .line 556
    .local v2, "stackElements":[Ljava/lang/StackTraceElement;
    const/4 v1, 0x0

    .line 557
    .local v1, "stackEl":Ljava/lang/StackTraceElement;
    if-eqz v2, :cond_0

    .line 558
    if-nez v1, :cond_0

    array-length v3, v2

    if-le v3, v4, :cond_0

    .line 560
    aget-object v1, v2, v4

    .line 563
    :cond_0
    return-object v1
.end method

.method private static getStackTrace()Ljava/lang/String;
    .locals 5

    .prologue
    .line 194
    new-instance v3, Ljava/lang/Throwable;

    invoke-direct {v3}, Ljava/lang/Throwable;-><init>()V

    .line 200
    .local v3, "tr":Ljava/lang/Throwable;
    new-instance v0, Ljava/lang/StringBuffer;

    invoke-direct {v0}, Ljava/lang/StringBuffer;-><init>()V

    .line 201
    .local v0, "buf":Ljava/lang/StringBuffer;
    const-string v4, "\ncurrent stack trace:\n"

    invoke-virtual {v0, v4}, Ljava/lang/StringBuffer;->append(Ljava/lang/String;)Ljava/lang/StringBuffer;

    .line 203
    invoke-virtual {v3}, Ljava/lang/Throwable;->getStackTrace()[Ljava/lang/StackTraceElement;

    move-result-object v2

    .line 204
    .local v2, "stack":[Ljava/lang/StackTraceElement;
    if-eqz v2, :cond_0

    .line 205
    const/4 v1, 0x2

    .local v1, "i":I
    :goto_0
    array-length v4, v2

    if-lt v1, v4, :cond_1

    .line 211
    .end local v1    # "i":I
    :cond_0
    invoke-virtual {v0}, Ljava/lang/StringBuffer;->toString()Ljava/lang/String;

    move-result-object v4

    return-object v4

    .line 206
    .restart local v1    # "i":I
    :cond_1
    const-string v4, "\tat "

    invoke-virtual {v0, v4}, Ljava/lang/StringBuffer;->append(Ljava/lang/String;)Ljava/lang/StringBuffer;

    .line 207
    aget-object v4, v2, v1

    invoke-virtual {v4}, Ljava/lang/StackTraceElement;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v0, v4}, Ljava/lang/StringBuffer;->append(Ljava/lang/String;)Ljava/lang/StringBuffer;

    .line 208
    const-string v4, "\n"

    invoke-virtual {v0, v4}, Ljava/lang/StringBuffer;->append(Ljava/lang/String;)Ljava/lang/StringBuffer;

    .line 205
    add-int/lit8 v1, v1, 0x1

    goto :goto_0
.end method

.method private static getTag(Ljava/lang/StackTraceElement;)Ljava/lang/String;
    .locals 4
    .param p0, "stackEl"    # Ljava/lang/StackTraceElement;

    .prologue
    .line 568
    if-nez p0, :cond_0

    const-string v0, ""

    .line 569
    .local v0, "logTag":Ljava/lang/String;
    :goto_0
    return-object v0

    .line 568
    .end local v0    # "logTag":Ljava/lang/String;
    :cond_0
    invoke-virtual {p0}, Ljava/lang/StackTraceElement;->getClassName()Ljava/lang/String;

    move-result-object v1

    const-string v2, "\\$.*"

    const-string v3, ""

    invoke-virtual {v1, v2, v3}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    goto :goto_0
.end method

.method public static i(Ljava/lang/String;)V
    .locals 2
    .param p0, "msg"    # Ljava/lang/String;

    .prologue
    .line 239
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Info:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {v0, v1, p0}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 240
    return-void
.end method

.method public static i(Ljava/lang/String;Ljava/lang/String;)V
    .locals 1
    .param p0, "tag"    # Ljava/lang/String;
    .param p1, "msg"    # Ljava/lang/String;

    .prologue
    .line 95
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Info:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {p0, v0, p1}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 96
    return-void
.end method

.method public static i(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V
    .locals 3
    .param p0, "tag"    # Ljava/lang/String;
    .param p1, "msg"    # Ljava/lang/String;
    .param p2, "tr"    # Ljava/lang/Throwable;

    .prologue
    .line 103
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Info:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v2

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {p2}, Landroid/util/Log;->getStackTraceString(Ljava/lang/Throwable;)Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {p0, v0, v1}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 104
    return-void
.end method

.method public static i(Ljava/lang/String;Ljava/lang/Throwable;)V
    .locals 4
    .param p0, "msg"    # Ljava/lang/String;
    .param p1, "tr"    # Ljava/lang/Throwable;

    .prologue
    .line 247
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Info:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-static {p0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {p1}, Landroid/util/Log;->getStackTraceString(Ljava/lang/Throwable;)Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 248
    return-void
.end method

.method public static i(Ljava/lang/Throwable;)V
    .locals 3
    .param p0, "tr"    # Ljava/lang/Throwable;

    .prologue
    .line 243
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Info:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {p0}, Landroid/util/Log;->getStackTraceString(Ljava/lang/Throwable;)Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 244
    return-void
.end method

.method private static declared-synchronized init()V
    .locals 8

    .prologue
    .line 388
    const-class v4, Lcom/duolebo/tools/DLBLog;

    monitor-enter v4

    :try_start_0
    sget-boolean v3, Lcom/duolebo/tools/DLBLog;->isLoaded:Z
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    if-eqz v3, :cond_0

    .line 423
    .local v1, "p":Ljava/util/Properties;
    :goto_0
    monitor-exit v4

    return-void

    .line 393
    .end local v1    # "p":Ljava/util/Properties;
    :cond_0
    :try_start_1
    invoke-static {}, Lcom/duolebo/tools/DLBLog;->getProperties()Ljava/util/Properties;

    move-result-object v1

    .line 394
    .restart local v1    # "p":Ljava/util/Properties;
    if-nez v1, :cond_1

    .line 396
    const/4 v3, 0x0

    const-string v5, "not found: dlblog.properties,set loglevel to Warn"

    invoke-static {v3, v5}, Lcom/duolebo/tools/DLBLog;->w(Ljava/lang/String;Ljava/lang/String;)V

    .line 397
    sget-object v3, Lcom/duolebo/tools/DLBLog$LogLevel;->Warn:Lcom/duolebo/tools/DLBLog$LogLevel;

    sput-object v3, Lcom/duolebo/tools/DLBLog;->logLevel:Lcom/duolebo/tools/DLBLog$LogLevel;
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_1

    .line 421
    const/4 v3, 0x1

    :try_start_2
    sput-boolean v3, Lcom/duolebo/tools/DLBLog;->isLoaded:Z
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    goto :goto_0

    .line 388
    :catchall_0
    move-exception v3

    monitor-exit v4

    throw v3

    .line 402
    :cond_1
    const/4 v2, 0x0

    .line 403
    .local v2, "value":Ljava/lang/String;
    :try_start_3
    const-string v3, "stdout.level"

    invoke-virtual {v1, v3}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v2

    if-eqz v2, :cond_2

    invoke-static {v2}, Lcom/duolebo/tools/DLBLog;->parseLevel(Ljava/lang/String;)Lcom/duolebo/tools/DLBLog$LogLevel;

    move-result-object v3

    sput-object v3, Lcom/duolebo/tools/DLBLog;->logLevel:Lcom/duolebo/tools/DLBLog$LogLevel;

    .line 404
    :cond_2
    const-string v3, "stdout.tag.name"

    invoke-virtual {v1, v3}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v2

    if-eqz v2, :cond_3

    sput-object v2, Lcom/duolebo/tools/DLBLog;->PValue_Tag:Ljava/lang/String;

    .line 405
    :cond_3
    const-string v3, "stdout.project.name"

    invoke-virtual {v1, v3}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v2

    if-eqz v2, :cond_4

    sput-object v2, Lcom/duolebo/tools/DLBLog;->PValue_Project:Ljava/lang/String;

    .line 407
    :cond_4
    const-string v3, "stdout.VERBOSE.format"

    invoke-virtual {v1, v3}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v2

    if-eqz v2, :cond_5

    sput-object v2, Lcom/duolebo/tools/DLBLog;->PValue_V:Ljava/lang/String;

    .line 408
    :cond_5
    const-string v3, "stdout.DEBUG.format"

    invoke-virtual {v1, v3}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v2

    if-eqz v2, :cond_6

    sput-object v2, Lcom/duolebo/tools/DLBLog;->PValue_D:Ljava/lang/String;

    .line 409
    :cond_6
    const-string v3, "stdout.INFO.format"

    invoke-virtual {v1, v3}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v2

    if-eqz v2, :cond_7

    sput-object v2, Lcom/duolebo/tools/DLBLog;->PValue_I:Ljava/lang/String;

    .line 410
    :cond_7
    const-string v3, "stdout.WARN.format"

    invoke-virtual {v1, v3}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v2

    if-eqz v2, :cond_8

    sput-object v2, Lcom/duolebo/tools/DLBLog;->PValue_W:Ljava/lang/String;

    .line 411
    :cond_8
    const-string v3, "stdout.ERROR.format"

    invoke-virtual {v1, v3}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v2

    if-eqz v2, :cond_9

    sput-object v2, Lcom/duolebo/tools/DLBLog;->PValue_E:Ljava/lang/String;

    .line 413
    :cond_9
    const/4 v3, 0x0

    sget-object v5, Lcom/duolebo/tools/DLBLog$LogLevel;->Debug:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v6, Ljava/lang/StringBuilder;

    const-string v7, "logLevel="

    invoke-direct {v6, v7}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    const-string v7, "stdout.level"

    invoke-virtual {v1, v7}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v7

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v6

    const-string v7, ","

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v6

    sget-object v7, Lcom/duolebo/tools/DLBLog;->logLevel:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object v6

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v6

    invoke-static {v3, v5, v6}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_0
    .catchall {:try_start_3 .. :try_end_3} :catchall_1

    .line 418
    :goto_1
    :try_start_4
    invoke-static {}, Lcom/duolebo/tools/DLBLog;->outputlogmsg()V
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_1

    .line 421
    const/4 v3, 0x1

    :try_start_5
    sput-boolean v3, Lcom/duolebo/tools/DLBLog;->isLoaded:Z
    :try_end_5
    .catchall {:try_start_5 .. :try_end_5} :catchall_0

    goto/16 :goto_0

    .line 414
    :catch_0
    move-exception v0

    .line 415
    .local v0, "E":Ljava/lang/Exception;
    const/4 v3, 0x0

    :try_start_6
    new-instance v5, Ljava/lang/StringBuilder;

    const-string v6, "load dlblog.properties error: "

    invoke-direct {v5, v6}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0}, Ljava/lang/Exception;->getMessage()Ljava/lang/String;

    move-result-object v6

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v5

    const-string v6, ". set loglevel to Warn"

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-static {v3, v5}, Lcom/duolebo/tools/DLBLog;->w(Ljava/lang/String;Ljava/lang/String;)V

    .line 416
    sget-object v3, Lcom/duolebo/tools/DLBLog$LogLevel;->Warn:Lcom/duolebo/tools/DLBLog$LogLevel;

    sput-object v3, Lcom/duolebo/tools/DLBLog;->logLevel:Lcom/duolebo/tools/DLBLog$LogLevel;
    :try_end_6
    .catchall {:try_start_6 .. :try_end_6} :catchall_1

    goto :goto_1

    .line 420
    .end local v0    # "E":Ljava/lang/Exception;
    .end local v2    # "value":Ljava/lang/String;
    :catchall_1
    move-exception v3

    .line 421
    const/4 v5, 0x1

    :try_start_7
    sput-boolean v5, Lcom/duolebo/tools/DLBLog;->isLoaded:Z

    .line 422
    throw v3
    :try_end_7
    .catchall {:try_start_7 .. :try_end_7} :catchall_0
.end method

.method private static loadClassFileAsProperties(Ljava/lang/String;)Ljava/util/Properties;
    .locals 9
    .param p0, "fileName"    # Ljava/lang/String;

    .prologue
    .line 514
    const/4 v3, 0x0

    .line 516
    .local v3, "props":Ljava/util/Properties;
    :try_start_0
    const-class v6, Lcom/duolebo/tools/DLBLog;

    invoke-virtual {v6, p0}, Ljava/lang/Class;->getResourceAsStream(Ljava/lang/String;)Ljava/io/InputStream;

    move-result-object v2

    .line 517
    .local v2, "in":Ljava/io/InputStream;
    if-eqz v2, :cond_0

    .line 519
    const-class v6, Lcom/duolebo/tools/DLBLog;

    invoke-virtual {v6, p0}, Ljava/lang/Class;->getResource(Ljava/lang/String;)Ljava/net/URL;

    move-result-object v5

    .line 520
    .local v5, "url":Ljava/net/URL;
    const/4 v7, 0x0

    new-instance v8, Ljava/lang/StringBuilder;

    const-string v6, "dlblog.properties="

    invoke-direct {v8, v6}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    if-nez v5, :cond_1

    const-string v6, ""

    :goto_0
    invoke-virtual {v8, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v6

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v6

    invoke-static {v7, v6}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;Ljava/lang/String;)V

    .line 522
    new-instance v4, Ljava/util/Properties;

    invoke-direct {v4}, Ljava/util/Properties;-><init>()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1

    .line 524
    .end local v3    # "props":Ljava/util/Properties;
    .local v4, "props":Ljava/util/Properties;
    :try_start_1
    invoke-virtual {v4, v2}, Ljava/util/Properties;->load(Ljava/io/InputStream;)V

    .line 525
    invoke-virtual {v2}, Ljava/io/InputStream;->close()V
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_0

    move-object v3, v4

    .line 533
    .end local v2    # "in":Ljava/io/InputStream;
    .end local v4    # "props":Ljava/util/Properties;
    .end local v5    # "url":Ljava/net/URL;
    .restart local v3    # "props":Ljava/util/Properties;
    :cond_0
    :goto_1
    return-object v3

    .line 520
    .restart local v2    # "in":Ljava/io/InputStream;
    .restart local v5    # "url":Ljava/net/URL;
    :cond_1
    :try_start_2
    invoke-virtual {v5}, Ljava/net/URL;->getFile()Ljava/lang/String;
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_1

    move-result-object v6

    goto :goto_0

    .line 526
    .end local v3    # "props":Ljava/util/Properties;
    .restart local v4    # "props":Ljava/util/Properties;
    :catch_0
    move-exception v0

    .line 527
    .local v0, "E":Ljava/lang/Exception;
    const/4 v6, 0x0

    :try_start_3
    new-instance v7, Ljava/lang/StringBuilder;

    const-string v8, "load "

    invoke-direct {v7, v8}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v7, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v7

    const-string v8, " error: "

    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v7

    invoke-virtual {v0}, Ljava/lang/Exception;->getMessage()Ljava/lang/String;

    move-result-object v8

    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v7

    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v7

    invoke-static {v6, v7}, Lcom/duolebo/tools/DLBLog;->w(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_2

    move-object v3, v4

    .line 530
    .end local v4    # "props":Ljava/util/Properties;
    .restart local v3    # "props":Ljava/util/Properties;
    goto :goto_1

    .end local v0    # "E":Ljava/lang/Exception;
    .end local v2    # "in":Ljava/io/InputStream;
    .end local v5    # "url":Ljava/net/URL;
    :catch_1
    move-exception v1

    .line 531
    .local v1, "e":Ljava/lang/Exception;
    :goto_2
    invoke-virtual {v1}, Ljava/lang/Exception;->printStackTrace()V

    goto :goto_1

    .line 530
    .end local v1    # "e":Ljava/lang/Exception;
    .end local v3    # "props":Ljava/util/Properties;
    .restart local v0    # "E":Ljava/lang/Exception;
    .restart local v2    # "in":Ljava/io/InputStream;
    .restart local v4    # "props":Ljava/util/Properties;
    .restart local v5    # "url":Ljava/net/URL;
    :catch_2
    move-exception v1

    move-object v3, v4

    .end local v4    # "props":Ljava/util/Properties;
    .restart local v3    # "props":Ljava/util/Properties;
    goto :goto_2
.end method

.method private static loadFileAsProperties(Ljava/lang/String;)Ljava/util/Properties;
    .locals 9
    .param p0, "fileName"    # Ljava/lang/String;

    .prologue
    .line 486
    const/4 v4, 0x0

    .line 488
    .local v4, "props":Ljava/util/Properties;
    :try_start_0
    new-instance v2, Ljava/io/File;

    invoke-direct {v2, p0}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    .line 489
    .local v2, "file":Ljava/io/File;
    invoke-virtual {v2}, Ljava/io/File;->isFile()Z

    move-result v6

    if-eqz v6, :cond_0

    .line 491
    new-instance v3, Ljava/io/FileInputStream;

    invoke-direct {v3, v2}, Ljava/io/FileInputStream;-><init>(Ljava/io/File;)V

    .line 493
    .local v3, "in":Ljava/io/InputStream;
    if-eqz v3, :cond_0

    .line 495
    const/4 v6, 0x0

    new-instance v7, Ljava/lang/StringBuilder;

    const-string v8, "dlblog.properties="

    invoke-direct {v7, v8}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v7, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v7

    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v7

    invoke-static {v6, v7}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;Ljava/lang/String;)V

    .line 497
    new-instance v5, Ljava/util/Properties;

    invoke-direct {v5}, Ljava/util/Properties;-><init>()V
    :try_end_0
    .catch Ljava/io/FileNotFoundException; {:try_start_0 .. :try_end_0} :catch_1

    .line 499
    .end local v4    # "props":Ljava/util/Properties;
    .local v5, "props":Ljava/util/Properties;
    :try_start_1
    invoke-virtual {v5, v3}, Ljava/util/Properties;->load(Ljava/io/InputStream;)V

    .line 500
    invoke-virtual {v3}, Ljava/io/InputStream;->close()V

    .line 501
    const-string v6, "#_file_timestampe"

    invoke-virtual {v2}, Ljava/io/File;->lastModified()J

    move-result-wide v7

    invoke-static {v7, v8}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object v7

    invoke-virtual {v5, v6, v7}, Ljava/util/Properties;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_0
    .catch Ljava/io/FileNotFoundException; {:try_start_1 .. :try_end_1} :catch_2

    move-object v4, v5

    .line 510
    .end local v2    # "file":Ljava/io/File;
    .end local v3    # "in":Ljava/io/InputStream;
    .end local v5    # "props":Ljava/util/Properties;
    .restart local v4    # "props":Ljava/util/Properties;
    :cond_0
    :goto_0
    return-object v4

    .line 502
    .end local v4    # "props":Ljava/util/Properties;
    .restart local v2    # "file":Ljava/io/File;
    .restart local v3    # "in":Ljava/io/InputStream;
    .restart local v5    # "props":Ljava/util/Properties;
    :catch_0
    move-exception v0

    .line 503
    .local v0, "E":Ljava/lang/Exception;
    const/4 v6, 0x0

    :try_start_2
    new-instance v7, Ljava/lang/StringBuilder;

    const-string v8, "load "

    invoke-direct {v7, v8}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v7, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v7

    const-string v8, " error: "

    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v7

    invoke-virtual {v0}, Ljava/lang/Exception;->getMessage()Ljava/lang/String;

    move-result-object v8

    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v7

    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v7

    invoke-static {v6, v7}, Lcom/duolebo/tools/DLBLog;->w(Ljava/lang/String;Ljava/lang/String;)V
    :try_end_2
    .catch Ljava/io/FileNotFoundException; {:try_start_2 .. :try_end_2} :catch_2

    move-object v4, v5

    .line 507
    .end local v5    # "props":Ljava/util/Properties;
    .restart local v4    # "props":Ljava/util/Properties;
    goto :goto_0

    .end local v0    # "E":Ljava/lang/Exception;
    .end local v2    # "file":Ljava/io/File;
    .end local v3    # "in":Ljava/io/InputStream;
    :catch_1
    move-exception v1

    .line 508
    .local v1, "e":Ljava/io/FileNotFoundException;
    :goto_1
    invoke-virtual {v1}, Ljava/io/FileNotFoundException;->printStackTrace()V

    goto :goto_0

    .line 507
    .end local v1    # "e":Ljava/io/FileNotFoundException;
    .end local v4    # "props":Ljava/util/Properties;
    .restart local v2    # "file":Ljava/io/File;
    .restart local v3    # "in":Ljava/io/InputStream;
    .restart local v5    # "props":Ljava/util/Properties;
    :catch_2
    move-exception v1

    move-object v4, v5

    .end local v5    # "props":Ljava/util/Properties;
    .restart local v4    # "props":Ljava/util/Properties;
    goto :goto_1
.end method

.method private static outputlogmsg()V
    .locals 6

    .prologue
    .line 426
    sget-object v1, Lcom/duolebo/tools/DLBLog;->initLogInfos:Ljava/util/List;

    invoke-interface {v1}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v1

    .local v0, "logmsg":Lcom/duolebo/tools/DLBLog$LogMsg;
    :cond_0
    :goto_0
    invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z

    move-result v2

    if-nez v2, :cond_1

    .line 449
    sget-object v1, Lcom/duolebo/tools/DLBLog;->initLogInfos:Ljava/util/List;

    invoke-interface {v1}, Ljava/util/List;->clear()V

    .line 450
    return-void

    .line 426
    :cond_1
    invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v0

    .end local v0    # "logmsg":Lcom/duolebo/tools/DLBLog$LogMsg;
    check-cast v0, Lcom/duolebo/tools/DLBLog$LogMsg;

    .line 428
    .restart local v0    # "logmsg":Lcom/duolebo/tools/DLBLog$LogMsg;
    iget-object v2, v0, Lcom/duolebo/tools/DLBLog$LogMsg;->loglevel:Lcom/duolebo/tools/DLBLog$LogLevel;

    iget v2, v2, Lcom/duolebo/tools/DLBLog$LogLevel;->logLevel:I

    sget-object v3, Lcom/duolebo/tools/DLBLog;->logLevel:Lcom/duolebo/tools/DLBLog$LogLevel;

    iget v3, v3, Lcom/duolebo/tools/DLBLog$LogLevel;->logLevel:I

    if-lt v2, v3, :cond_0

    .line 430
    invoke-static {}, Lcom/duolebo/tools/DLBLog;->$SWITCH_TABLE$com$duolebo$tools$DLBLog$LogLevel()[I

    move-result-object v2

    iget-object v3, v0, Lcom/duolebo/tools/DLBLog$LogMsg;->loglevel:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-virtual {v3}, Lcom/duolebo/tools/DLBLog$LogLevel;->ordinal()I

    move-result v3

    aget v2, v2, v3

    packed-switch v2, :pswitch_data_0

    goto :goto_0

    .line 433
    :pswitch_0
    iget-object v2, v0, Lcom/duolebo/tools/DLBLog$LogMsg;->tag:Ljava/lang/String;

    iget-object v3, v0, Lcom/duolebo/tools/DLBLog$LogMsg;->msg:Ljava/lang/String;

    const-string v4, "noinit"

    sget-object v5, Lcom/duolebo/tools/DLBLog;->PValue_Project:Ljava/lang/String;

    invoke-virtual {v3, v4, v5}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v3

    invoke-static {v2, v3}, Landroid/util/Log;->v(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_0

    .line 436
    :pswitch_1
    iget-object v2, v0, Lcom/duolebo/tools/DLBLog$LogMsg;->tag:Ljava/lang/String;

    iget-object v3, v0, Lcom/duolebo/tools/DLBLog$LogMsg;->msg:Ljava/lang/String;

    const-string v4, "noinit"

    sget-object v5, Lcom/duolebo/tools/DLBLog;->PValue_Project:Ljava/lang/String;

    invoke-virtual {v3, v4, v5}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v3

    invoke-static {v2, v3}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_0

    .line 439
    :pswitch_2
    iget-object v2, v0, Lcom/duolebo/tools/DLBLog$LogMsg;->tag:Ljava/lang/String;

    iget-object v3, v0, Lcom/duolebo/tools/DLBLog$LogMsg;->msg:Ljava/lang/String;

    const-string v4, "noinit"

    sget-object v5, Lcom/duolebo/tools/DLBLog;->PValue_Project:Ljava/lang/String;

    invoke-virtual {v3, v4, v5}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v3

    invoke-static {v2, v3}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_0

    .line 442
    :pswitch_3
    iget-object v2, v0, Lcom/duolebo/tools/DLBLog$LogMsg;->tag:Ljava/lang/String;

    iget-object v3, v0, Lcom/duolebo/tools/DLBLog$LogMsg;->msg:Ljava/lang/String;

    const-string v4, "noinit"

    sget-object v5, Lcom/duolebo/tools/DLBLog;->PValue_Project:Ljava/lang/String;

    invoke-virtual {v3, v4, v5}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v3

    invoke-static {v2, v3}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_0

    .line 445
    :pswitch_4
    iget-object v2, v0, Lcom/duolebo/tools/DLBLog$LogMsg;->tag:Ljava/lang/String;

    iget-object v3, v0, Lcom/duolebo/tools/DLBLog$LogMsg;->msg:Ljava/lang/String;

    const-string v4, "noinit"

    sget-object v5, Lcom/duolebo/tools/DLBLog;->PValue_Project:Ljava/lang/String;

    invoke-virtual {v3, v4, v5}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v3

    invoke-static {v2, v3}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_0

    .line 430
    :pswitch_data_0
    .packed-switch 0x1
        :pswitch_0
        :pswitch_1
        :pswitch_2
        :pswitch_3
        :pswitch_4
    .end packed-switch
.end method

.method private static parseLevel(Ljava/lang/String;)Lcom/duolebo/tools/DLBLog$LogLevel;
    .locals 1
    .param p0, "pLevel"    # Ljava/lang/String;

    .prologue
    .line 537
    const-string v0, "VERBOSE"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_0

    .line 538
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Verbose:Lcom/duolebo/tools/DLBLog$LogLevel;

    .line 549
    :goto_0
    return-object v0

    .line 539
    :cond_0
    const-string v0, "DEBUG"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_1

    .line 540
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Debug:Lcom/duolebo/tools/DLBLog$LogLevel;

    goto :goto_0

    .line 541
    :cond_1
    const-string v0, "INFO"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_2

    .line 542
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Info:Lcom/duolebo/tools/DLBLog$LogLevel;

    goto :goto_0

    .line 543
    :cond_2
    const-string v0, "WARN"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_3

    .line 544
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Warn:Lcom/duolebo/tools/DLBLog$LogLevel;

    goto :goto_0

    .line 545
    :cond_3
    const-string v0, "ERROR"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_4

    .line 546
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Error:Lcom/duolebo/tools/DLBLog$LogLevel;

    goto :goto_0

    .line 547
    :cond_4
    const-string v0, "FATAL"

    invoke-virtual {v0, p0}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z

    move-result v0

    if-eqz v0, :cond_5

    .line 548
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->NoLog:Lcom/duolebo/tools/DLBLog$LogLevel;

    goto :goto_0

    .line 549
    :cond_5
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Debug:Lcom/duolebo/tools/DLBLog$LogLevel;

    goto :goto_0
.end method

.method private static printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V
    .locals 12
    .param p0, "tag"    # Ljava/lang/String;
    .param p1, "level"    # Lcom/duolebo/tools/DLBLog$LogLevel;
    .param p2, "msg"    # Ljava/lang/String;

    .prologue
    .line 275
    sget-object v8, Ljava/lang/System;->out:Ljava/io/PrintStream;

    new-instance v9, Ljava/lang/StringBuilder;

    const-string v10, "zzzzz1 "

    invoke-direct {v9, v10}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v9, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v9

    const-string v10, " "

    invoke-virtual {v9, v10}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v9

    invoke-virtual {v9, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v9

    const-string v10, "  "

    invoke-virtual {v9, v10}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v9

    invoke-virtual {v9, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object v9

    invoke-virtual {v9}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v9

    invoke-virtual {v8, v9}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    .line 276
    sget-boolean v8, Lcom/duolebo/tools/DLBLog;->isLoaded:Z

    if-nez v8, :cond_3

    .line 278
    invoke-static {}, Lcom/duolebo/tools/DLBLog;->getSTE()Ljava/lang/StackTraceElement;

    move-result-object v7

    .line 280
    .local v7, "stackEl":Ljava/lang/StackTraceElement;
    const-string v8, "noinit|%d|%p|%t@%c:%L-%M(?)|%m%n"

    const-string v9, "%p"

    iget-object v10, p1, Lcom/duolebo/tools/DLBLog$LogLevel;->levelName:Ljava/lang/String;

    invoke-virtual {v8, v9, v10}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v8

    const-string v9, "%t"

    invoke-static {}, Ljava/lang/Thread;->currentThread()Ljava/lang/Thread;

    move-result-object v10

    invoke-virtual {v10}, Ljava/lang/Thread;->getName()Ljava/lang/String;

    move-result-object v10

    invoke-virtual {v8, v9, v10}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v8

    const-string v9, "%n"

    const-string v10, "line.separator"

    invoke-static {v10}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v10

    invoke-virtual {v8, v9, v10}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v8

    const-string v9, "%d"

    new-instance v10, Ljava/text/SimpleDateFormat;

    const-string v11, "yyyy-MM-dd HH:mm:ss.SSS"

    invoke-direct {v10, v11}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;)V

    new-instance v11, Ljava/util/Date;

    invoke-direct {v11}, Ljava/util/Date;-><init>()V

    invoke-virtual {v10, v11}, Ljava/text/SimpleDateFormat;->format(Ljava/util/Date;)Ljava/lang/String;

    move-result-object v10

    invoke-virtual {v8, v9, v10}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v6

    .line 281
    .local v6, "ret":Ljava/lang/String;
    if-nez v7, :cond_1

    .line 283
    const-string v8, "%c"

    const-string v9, "noclass"

    invoke-virtual {v6, v8, v9}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v8

    const-string v9, "%F"

    const-string v10, "no"

    invoke-virtual {v8, v9, v10}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v8

    const-string v9, "%M"

    const-string v10, "nomethod"

    invoke-virtual {v8, v9, v10}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v8

    const-string v9, "%L"

    const-string v10, "0"

    invoke-virtual {v8, v9, v10}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v6

    .line 290
    :goto_0
    const-string v8, "%m"

    invoke-virtual {v6, v8, p2}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v6

    .line 291
    sget-object v8, Lcom/duolebo/tools/DLBLog;->initLogInfos:Ljava/util/List;

    new-instance v9, Lcom/duolebo/tools/DLBLog$LogMsg;

    if-eqz p0, :cond_2

    .end local p0    # "tag":Ljava/lang/String;
    :goto_1
    invoke-direct {v9, p1, p0, v6}, Lcom/duolebo/tools/DLBLog$LogMsg;-><init>(Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;Ljava/lang/String;)V

    invoke-interface {v8, v9}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 385
    .end local v6    # "ret":Ljava/lang/String;
    .end local v7    # "stackEl":Ljava/lang/StackTraceElement;
    :cond_0
    :goto_2
    return-void

    .line 288
    .restart local v6    # "ret":Ljava/lang/String;
    .restart local v7    # "stackEl":Ljava/lang/StackTraceElement;
    .restart local p0    # "tag":Ljava/lang/String;
    :cond_1
    const-string v8, "%c"

    invoke-virtual {v7}, Ljava/lang/StackTraceElement;->getClassName()Ljava/lang/String;

    move-result-object v9

    const-string v10, "^.*\\."

    const-string v11, ""

    invoke-virtual {v9, v10, v11}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v9

    invoke-virtual {v6, v8, v9}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v8

    const-string v9, "%M"

    invoke-virtual {v7}, Ljava/lang/StackTraceElement;->getMethodName()Ljava/lang/String;

    move-result-object v10

    invoke-virtual {v8, v9, v10}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v8

    const-string v9, "%L"

    new-instance v10, Ljava/lang/StringBuilder;

    invoke-direct {v10}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v7}, Ljava/lang/StackTraceElement;->getLineNumber()I

    move-result v11

    invoke-virtual {v10, v11}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v10

    invoke-virtual {v10}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v10

    invoke-virtual {v8, v9, v10}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v6

    goto :goto_0

    .line 291
    :cond_2
    invoke-static {v7}, Lcom/duolebo/tools/DLBLog;->getTag(Ljava/lang/StackTraceElement;)Ljava/lang/String;

    move-result-object p0

    goto :goto_1

    .line 295
    .end local v6    # "ret":Ljava/lang/String;
    .end local v7    # "stackEl":Ljava/lang/StackTraceElement;
    :cond_3
    sget-boolean v8, Lcom/duolebo/tools/DLBLog;->isLoaded:Z

    if-eqz v8, :cond_6

    .line 296
    const/4 v3, 0x0

    .line 297
    .local v3, "isNeedReload":I
    new-instance v1, Ljava/io/File;

    const-string v8, "/data/dlblog.properties"

    invoke-direct {v1, v8}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    .line 298
    .local v1, "file":Ljava/io/File;
    invoke-virtual {v1}, Ljava/io/File;->exists()Z

    move-result v8

    if-eqz v8, :cond_8

    .line 299
    sget-object v8, Lcom/duolebo/tools/DLBLog;->file_timestampe:Ljava/lang/String;

    if-eqz v8, :cond_4

    .line 300
    sget-object v8, Lcom/duolebo/tools/DLBLog;->file_timestampe:Ljava/lang/String;

    new-instance v9, Ljava/lang/StringBuilder;

    invoke-direct {v9}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v1}, Ljava/io/File;->lastModified()J

    move-result-wide v10

    invoke-virtual {v9, v10, v11}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    move-result-object v9

    invoke-virtual {v9}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v9

    invoke-virtual {v8, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v8

    if-nez v8, :cond_5

    .line 301
    :cond_4
    const/16 v3, 0x457

    .line 306
    :cond_5
    :goto_3
    if-eqz v3, :cond_6

    .line 308
    :try_start_0
    const-string v8, "DetailLog"

    new-instance v9, Ljava/lang/StringBuilder;

    const-string v10, "-----------------dlblog.properties------------file changed="

    invoke-direct {v9, v10}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v9, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v9

    invoke-virtual {v9}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v9

    invoke-static {v8, v9}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    .line 309
    const/4 v8, 0x0

    sput-boolean v8, Lcom/duolebo/tools/DLBLog;->isLoaded:Z

    .line 310
    invoke-static {}, Lcom/duolebo/tools/DLBLog;->init()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    .line 317
    .end local v1    # "file":Ljava/io/File;
    .end local v3    # "isNeedReload":I
    :cond_6
    :goto_4
    iget v8, p1, Lcom/duolebo/tools/DLBLog$LogLevel;->logLevel:I

    sget-object v9, Lcom/duolebo/tools/DLBLog;->logLevel:Lcom/duolebo/tools/DLBLog$LogLevel;

    iget v9, v9, Lcom/duolebo/tools/DLBLog$LogLevel;->logLevel:I

    if-lt v8, v9, :cond_0

    .line 320
    invoke-static {}, Lcom/duolebo/tools/DLBLog;->getSTE()Ljava/lang/StackTraceElement;

    move-result-object v7

    .line 322
    .restart local v7    # "stackEl":Ljava/lang/StackTraceElement;
    if-eqz p2, :cond_7

    invoke-virtual {p2}, Ljava/lang/String;->length()I

    move-result v8

    const/16 v9, 0x1000

    if-gt v8, v9, :cond_e

    .line 324
    :cond_7
    const/4 v5, 0x0

    .line 325
    .local v5, "outMsg":Ljava/lang/String;
    invoke-static {}, Lcom/duolebo/tools/DLBLog;->$SWITCH_TABLE$com$duolebo$tools$DLBLog$LogLevel()[I

    move-result-object v8

    invoke-virtual {p1}, Lcom/duolebo/tools/DLBLog$LogLevel;->ordinal()I

    move-result v9

    aget v8, v8, v9

    packed-switch v8, :pswitch_data_0

    goto/16 :goto_2

    .line 328
    :pswitch_0
    iget-object v8, p1, Lcom/duolebo/tools/DLBLog$LogLevel;->levelName:Ljava/lang/String;

    sget-object v9, Lcom/duolebo/tools/DLBLog;->PValue_V:Ljava/lang/String;

    invoke-static {v7, v8, v9, p2}, Lcom/duolebo/tools/DLBLog;->getMsg(Ljava/lang/StackTraceElement;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v5

    .line 329
    if-eqz p0, :cond_9

    .end local p0    # "tag":Ljava/lang/String;
    :goto_5
    invoke-static {p0, v5}, Landroid/util/Log;->v(Ljava/lang/String;Ljava/lang/String;)I

    goto/16 :goto_2

    .line 303
    .end local v5    # "outMsg":Ljava/lang/String;
    .end local v7    # "stackEl":Ljava/lang/StackTraceElement;
    .restart local v1    # "file":Ljava/io/File;
    .restart local v3    # "isNeedReload":I
    .restart local p0    # "tag":Ljava/lang/String;
    :cond_8
    sget-object v8, Lcom/duolebo/tools/DLBLog;->file_timestampe:Ljava/lang/String;

    if-eqz v8, :cond_5

    .line 304
    const/16 v3, 0x8ae

    goto :goto_3

    .line 311
    :catch_0
    move-exception v0

    .line 312
    .local v0, "E":Ljava/lang/Exception;
    invoke-virtual {v0}, Ljava/lang/Exception;->printStackTrace()V

    goto :goto_4

    .line 329
    .end local v0    # "E":Ljava/lang/Exception;
    .end local v1    # "file":Ljava/io/File;
    .end local v3    # "isNeedReload":I
    .restart local v5    # "outMsg":Ljava/lang/String;
    .restart local v7    # "stackEl":Ljava/lang/StackTraceElement;
    :cond_9
    invoke-static {v7}, Lcom/duolebo/tools/DLBLog;->getTag(Ljava/lang/StackTraceElement;)Ljava/lang/String;

    move-result-object p0

    goto :goto_5

    .line 332
    :pswitch_1
    iget-object v8, p1, Lcom/duolebo/tools/DLBLog$LogLevel;->levelName:Ljava/lang/String;

    sget-object v9, Lcom/duolebo/tools/DLBLog;->PValue_D:Ljava/lang/String;

    invoke-static {v7, v8, v9, p2}, Lcom/duolebo/tools/DLBLog;->getMsg(Ljava/lang/StackTraceElement;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v5

    .line 333
    if-eqz p0, :cond_a

    .end local p0    # "tag":Ljava/lang/String;
    :goto_6
    invoke-static {p0, v5}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    goto/16 :goto_2

    .restart local p0    # "tag":Ljava/lang/String;
    :cond_a
    invoke-static {v7}, Lcom/duolebo/tools/DLBLog;->getTag(Ljava/lang/StackTraceElement;)Ljava/lang/String;

    move-result-object p0

    goto :goto_6

    .line 336
    :pswitch_2
    iget-object v8, p1, Lcom/duolebo/tools/DLBLog$LogLevel;->levelName:Ljava/lang/String;

    sget-object v9, Lcom/duolebo/tools/DLBLog;->PValue_I:Ljava/lang/String;

    invoke-static {v7, v8, v9, p2}, Lcom/duolebo/tools/DLBLog;->getMsg(Ljava/lang/StackTraceElement;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v5

    .line 337
    if-eqz p0, :cond_b

    .end local p0    # "tag":Ljava/lang/String;
    :goto_7
    invoke-static {p0, v5}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    goto/16 :goto_2

    .restart local p0    # "tag":Ljava/lang/String;
    :cond_b
    invoke-static {v7}, Lcom/duolebo/tools/DLBLog;->getTag(Ljava/lang/StackTraceElement;)Ljava/lang/String;

    move-result-object p0

    goto :goto_7

    .line 340
    :pswitch_3
    iget-object v8, p1, Lcom/duolebo/tools/DLBLog$LogLevel;->levelName:Ljava/lang/String;

    sget-object v9, Lcom/duolebo/tools/DLBLog;->PValue_W:Ljava/lang/String;

    invoke-static {v7, v8, v9, p2}, Lcom/duolebo/tools/DLBLog;->getMsg(Ljava/lang/StackTraceElement;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v5

    .line 341
    if-eqz p0, :cond_c

    .end local p0    # "tag":Ljava/lang/String;
    :goto_8
    invoke-static {p0, v5}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    goto/16 :goto_2

    .restart local p0    # "tag":Ljava/lang/String;
    :cond_c
    invoke-static {v7}, Lcom/duolebo/tools/DLBLog;->getTag(Ljava/lang/StackTraceElement;)Ljava/lang/String;

    move-result-object p0

    goto :goto_8

    .line 344
    :pswitch_4
    iget-object v8, p1, Lcom/duolebo/tools/DLBLog$LogLevel;->levelName:Ljava/lang/String;

    sget-object v9, Lcom/duolebo/tools/DLBLog;->PValue_E:Ljava/lang/String;

    invoke-static {v7, v8, v9, p2}, Lcom/duolebo/tools/DLBLog;->getMsg(Ljava/lang/StackTraceElement;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v5

    .line 345
    if-eqz p0, :cond_d

    .end local p0    # "tag":Ljava/lang/String;
    :goto_9
    invoke-static {p0, v5}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    goto/16 :goto_2

    .restart local p0    # "tag":Ljava/lang/String;
    :cond_d
    invoke-static {v7}, Lcom/duolebo/tools/DLBLog;->getTag(Ljava/lang/StackTraceElement;)Ljava/lang/String;

    move-result-object p0

    goto :goto_9

    .line 351
    .end local v5    # "outMsg":Ljava/lang/String;
    :cond_e
    const/4 v2, 0x0

    .line 352
    .local v2, "iOffset":I
    :goto_a
    invoke-virtual {p2}, Ljava/lang/String;->length()I

    move-result v8

    if-ge v2, v8, :cond_0

    .line 354
    add-int/lit16 v4, v2, 0x1000

    .line 355
    .local v4, "newPosi":I
    invoke-virtual {p2}, Ljava/lang/String;->length()I

    move-result v8

    if-le v4, v8, :cond_f

    .line 356
    invoke-virtual {p2}, Ljava/lang/String;->length()I

    move-result v4

    .line 357
    :cond_f
    const/4 v5, 0x0

    .line 358
    .restart local v5    # "outMsg":Ljava/lang/String;
    invoke-static {}, Lcom/duolebo/tools/DLBLog;->$SWITCH_TABLE$com$duolebo$tools$DLBLog$LogLevel()[I

    move-result-object v8

    invoke-virtual {p1}, Lcom/duolebo/tools/DLBLog$LogLevel;->ordinal()I

    move-result v9

    aget v8, v8, v9

    packed-switch v8, :pswitch_data_1

    .line 381
    :goto_b
    add-int/lit16 v2, v2, 0x1000

    goto :goto_a

    .line 361
    :pswitch_5
    iget-object v8, p1, Lcom/duolebo/tools/DLBLog$LogLevel;->levelName:Ljava/lang/String;

    sget-object v9, Lcom/duolebo/tools/DLBLog;->PValue_V:Ljava/lang/String;

    invoke-virtual {p2, v2, v4}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v10

    invoke-static {v7, v8, v9, v10}, Lcom/duolebo/tools/DLBLog;->getMsg(Ljava/lang/StackTraceElement;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v5

    .line 362
    if-eqz p0, :cond_10

    move-object v8, p0

    :goto_c
    invoke-static {v8, v5}, Landroid/util/Log;->v(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_b

    :cond_10
    invoke-static {v7}, Lcom/duolebo/tools/DLBLog;->getTag(Ljava/lang/StackTraceElement;)Ljava/lang/String;

    move-result-object v8

    goto :goto_c

    .line 365
    :pswitch_6
    iget-object v8, p1, Lcom/duolebo/tools/DLBLog$LogLevel;->levelName:Ljava/lang/String;

    sget-object v9, Lcom/duolebo/tools/DLBLog;->PValue_D:Ljava/lang/String;

    invoke-virtual {p2, v2, v4}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v10

    invoke-static {v7, v8, v9, v10}, Lcom/duolebo/tools/DLBLog;->getMsg(Ljava/lang/StackTraceElement;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v5

    .line 366
    if-eqz p0, :cond_11

    move-object v8, p0

    :goto_d
    invoke-static {v8, v5}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_b

    :cond_11
    invoke-static {v7}, Lcom/duolebo/tools/DLBLog;->getTag(Ljava/lang/StackTraceElement;)Ljava/lang/String;

    move-result-object v8

    goto :goto_d

    .line 369
    :pswitch_7
    iget-object v8, p1, Lcom/duolebo/tools/DLBLog$LogLevel;->levelName:Ljava/lang/String;

    sget-object v9, Lcom/duolebo/tools/DLBLog;->PValue_I:Ljava/lang/String;

    invoke-virtual {p2, v2, v4}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v10

    invoke-static {v7, v8, v9, v10}, Lcom/duolebo/tools/DLBLog;->getMsg(Ljava/lang/StackTraceElement;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v5

    .line 370
    if-eqz p0, :cond_12

    move-object v8, p0

    :goto_e
    invoke-static {v8, v5}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_b

    :cond_12
    invoke-static {v7}, Lcom/duolebo/tools/DLBLog;->getTag(Ljava/lang/StackTraceElement;)Ljava/lang/String;

    move-result-object v8

    goto :goto_e

    .line 373
    :pswitch_8
    iget-object v8, p1, Lcom/duolebo/tools/DLBLog$LogLevel;->levelName:Ljava/lang/String;

    sget-object v9, Lcom/duolebo/tools/DLBLog;->PValue_W:Ljava/lang/String;

    invoke-virtual {p2, v2, v4}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v10

    invoke-static {v7, v8, v9, v10}, Lcom/duolebo/tools/DLBLog;->getMsg(Ljava/lang/StackTraceElement;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v5

    .line 374
    if-eqz p0, :cond_13

    move-object v8, p0

    :goto_f
    invoke-static {v8, v5}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_b

    :cond_13
    invoke-static {v7}, Lcom/duolebo/tools/DLBLog;->getTag(Ljava/lang/StackTraceElement;)Ljava/lang/String;

    move-result-object v8

    goto :goto_f

    .line 377
    :pswitch_9
    iget-object v8, p1, Lcom/duolebo/tools/DLBLog$LogLevel;->levelName:Ljava/lang/String;

    sget-object v9, Lcom/duolebo/tools/DLBLog;->PValue_E:Ljava/lang/String;

    invoke-virtual {p2, v2, v4}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v10

    invoke-static {v7, v8, v9, v10}, Lcom/duolebo/tools/DLBLog;->getMsg(Ljava/lang/StackTraceElement;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v5

    .line 378
    if-eqz p0, :cond_14

    move-object v8, p0

    :goto_10
    invoke-static {v8, v5}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;)I

    goto :goto_b

    :cond_14
    invoke-static {v7}, Lcom/duolebo/tools/DLBLog;->getTag(Ljava/lang/StackTraceElement;)Ljava/lang/String;

    move-result-object v8

    goto :goto_10

    .line 325
    nop

    :pswitch_data_0
    .packed-switch 0x1
        :pswitch_0
        :pswitch_1
        :pswitch_2
        :pswitch_3
        :pswitch_4
    .end packed-switch

    .line 358
    :pswitch_data_1
    .packed-switch 0x1
        :pswitch_5
        :pswitch_6
        :pswitch_7
        :pswitch_8
        :pswitch_9
    .end packed-switch
.end method

.method public static printStackTraceD()V
    .locals 3

    .prologue
    .line 146
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Debug:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {}, Lcom/duolebo/tools/DLBLog;->getStackTrace()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 147
    return-void
.end method

.method public static printStackTraceD(Ljava/lang/String;)V
    .locals 4
    .param p0, "msg"    # Ljava/lang/String;

    .prologue
    .line 150
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Debug:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-static {p0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Lcom/duolebo/tools/DLBLog;->getStackTrace()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 151
    return-void
.end method

.method public static printStackTraceD(Ljava/lang/String;Ljava/lang/String;)V
    .locals 3
    .param p0, "tag"    # Ljava/lang/String;
    .param p1, "msg"    # Ljava/lang/String;

    .prologue
    .line 154
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Debug:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v2

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Lcom/duolebo/tools/DLBLog;->getStackTrace()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {p0, v0, v1}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 155
    return-void
.end method

.method public static printStackTraceE()V
    .locals 3

    .prologue
    .line 182
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Error:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {}, Lcom/duolebo/tools/DLBLog;->getStackTrace()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 183
    return-void
.end method

.method public static printStackTraceE(Ljava/lang/String;)V
    .locals 4
    .param p0, "msg"    # Ljava/lang/String;

    .prologue
    .line 186
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Error:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-static {p0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Lcom/duolebo/tools/DLBLog;->getStackTrace()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 187
    return-void
.end method

.method public static printStackTraceE(Ljava/lang/String;Ljava/lang/String;)V
    .locals 3
    .param p0, "tag"    # Ljava/lang/String;
    .param p1, "msg"    # Ljava/lang/String;

    .prologue
    .line 190
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Error:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v2

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Lcom/duolebo/tools/DLBLog;->getStackTrace()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {p0, v0, v1}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 191
    return-void
.end method

.method public static printStackTraceI()V
    .locals 4

    .prologue
    .line 158
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Info:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    invoke-static {}, Lcom/duolebo/tools/DLBLog;->getStackTrace()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 159
    return-void
.end method

.method public static printStackTraceI(Ljava/lang/String;)V
    .locals 4
    .param p0, "msg"    # Ljava/lang/String;

    .prologue
    .line 162
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Info:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-static {p0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Lcom/duolebo/tools/DLBLog;->getStackTrace()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 163
    return-void
.end method

.method public static printStackTraceI(Ljava/lang/String;Ljava/lang/String;)V
    .locals 3
    .param p0, "tag"    # Ljava/lang/String;
    .param p1, "msg"    # Ljava/lang/String;

    .prologue
    .line 166
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Info:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v2

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Lcom/duolebo/tools/DLBLog;->getStackTrace()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {p0, v0, v1}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 167
    return-void
.end method

.method public static printStackTraceV()V
    .locals 3

    .prologue
    .line 134
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Verbose:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {}, Lcom/duolebo/tools/DLBLog;->getStackTrace()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 135
    return-void
.end method

.method public static printStackTraceV(Ljava/lang/String;)V
    .locals 4
    .param p0, "msg"    # Ljava/lang/String;

    .prologue
    .line 138
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Verbose:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-static {p0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Lcom/duolebo/tools/DLBLog;->getStackTrace()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 139
    return-void
.end method

.method public static printStackTraceV(Ljava/lang/String;Ljava/lang/String;)V
    .locals 3
    .param p0, "tag"    # Ljava/lang/String;
    .param p1, "msg"    # Ljava/lang/String;

    .prologue
    .line 142
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Verbose:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v2

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Lcom/duolebo/tools/DLBLog;->getStackTrace()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {p0, v0, v1}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 143
    return-void
.end method

.method public static printStackTraceW()V
    .locals 3

    .prologue
    .line 170
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Warn:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {}, Lcom/duolebo/tools/DLBLog;->getStackTrace()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 171
    return-void
.end method

.method public static printStackTraceW(Ljava/lang/String;)V
    .locals 4
    .param p0, "msg"    # Ljava/lang/String;

    .prologue
    .line 174
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Warn:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-static {p0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Lcom/duolebo/tools/DLBLog;->getStackTrace()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 175
    return-void
.end method

.method public static printStackTraceW(Ljava/lang/String;Ljava/lang/String;)V
    .locals 3
    .param p0, "tag"    # Ljava/lang/String;
    .param p1, "msg"    # Ljava/lang/String;

    .prologue
    .line 178
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Warn:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v2

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {}, Lcom/duolebo/tools/DLBLog;->getStackTrace()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {p0, v0, v1}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 179
    return-void
.end method

.method public static v(Ljava/lang/String;)V
    .locals 2
    .param p0, "msg"    # Ljava/lang/String;

    .prologue
    .line 215
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Verbose:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {v0, v1, p0}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 216
    return-void
.end method

.method public static v(Ljava/lang/String;Ljava/lang/String;)V
    .locals 1
    .param p0, "tag"    # Ljava/lang/String;
    .param p1, "msg"    # Ljava/lang/String;

    .prologue
    .line 71
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Verbose:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {p0, v0, p1}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 72
    return-void
.end method

.method public static v(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V
    .locals 3
    .param p0, "tag"    # Ljava/lang/String;
    .param p1, "msg"    # Ljava/lang/String;
    .param p2, "tr"    # Ljava/lang/Throwable;

    .prologue
    .line 79
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Verbose:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v2

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {p2}, Landroid/util/Log;->getStackTraceString(Ljava/lang/Throwable;)Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {p0, v0, v1}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 80
    return-void
.end method

.method public static v(Ljava/lang/String;Ljava/lang/Throwable;)V
    .locals 4
    .param p0, "msg"    # Ljava/lang/String;
    .param p1, "tr"    # Ljava/lang/Throwable;

    .prologue
    .line 223
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Verbose:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-static {p0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {p1}, Landroid/util/Log;->getStackTraceString(Ljava/lang/Throwable;)Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 224
    return-void
.end method

.method public static v(Ljava/lang/Throwable;)V
    .locals 3
    .param p0, "tr"    # Ljava/lang/Throwable;

    .prologue
    .line 219
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Verbose:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {p0}, Landroid/util/Log;->getStackTraceString(Ljava/lang/Throwable;)Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 220
    return-void
.end method

.method public static w(Ljava/lang/String;)V
    .locals 2
    .param p0, "msg"    # Ljava/lang/String;

    .prologue
    .line 251
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Warn:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {v0, v1, p0}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 252
    return-void
.end method

.method public static w(Ljava/lang/String;Ljava/lang/String;)V
    .locals 1
    .param p0, "tag"    # Ljava/lang/String;
    .param p1, "msg"    # Ljava/lang/String;

    .prologue
    .line 107
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Warn:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {p0, v0, p1}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 108
    return-void
.end method

.method public static w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V
    .locals 3
    .param p0, "tag"    # Ljava/lang/String;
    .param p1, "msg"    # Ljava/lang/String;
    .param p2, "tr"    # Ljava/lang/Throwable;

    .prologue
    .line 115
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Warn:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v1, Ljava/lang/StringBuilder;

    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v2

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {p2}, Landroid/util/Log;->getStackTraceString(Ljava/lang/Throwable;)Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {p0, v0, v1}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 116
    return-void
.end method

.method public static w(Ljava/lang/String;Ljava/lang/Throwable;)V
    .locals 4
    .param p0, "msg"    # Ljava/lang/String;
    .param p1, "tr"    # Ljava/lang/Throwable;

    .prologue
    .line 259
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Warn:Lcom/duolebo/tools/DLBLog$LogLevel;

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-static {p0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {p1}, Landroid/util/Log;->getStackTraceString(Ljava/lang/Throwable;)Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 260
    return-void
.end method

.method public static w(Ljava/lang/Throwable;)V
    .locals 3
    .param p0, "tr"    # Ljava/lang/Throwable;

    .prologue
    .line 255
    const/4 v0, 0x0

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Warn:Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {p0}, Landroid/util/Log;->getStackTraceString(Ljava/lang/Throwable;)Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Lcom/duolebo/tools/DLBLog;->printLog(Ljava/lang/String;Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;)V

    .line 256
    return-void
.end method
