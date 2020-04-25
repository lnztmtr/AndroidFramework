.class final enum Lcom/duolebo/tools/DLBLog$LogLevel;
.super Ljava/lang/Enum;
.source "DLBLog.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/duolebo/tools/DLBLog;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x401a
    name = "LogLevel"
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Enum",
        "<",
        "Lcom/duolebo/tools/DLBLog$LogLevel;",
        ">;"
    }
.end annotation


# static fields
.field public static final enum Debug:Lcom/duolebo/tools/DLBLog$LogLevel;

.field private static final synthetic ENUM$VALUES:[Lcom/duolebo/tools/DLBLog$LogLevel;

.field public static final enum Error:Lcom/duolebo/tools/DLBLog$LogLevel;

.field public static final enum Info:Lcom/duolebo/tools/DLBLog$LogLevel;

.field public static final enum NoLog:Lcom/duolebo/tools/DLBLog$LogLevel;

.field public static final enum Verbose:Lcom/duolebo/tools/DLBLog$LogLevel;

.field public static final enum Warn:Lcom/duolebo/tools/DLBLog$LogLevel;


# instance fields
.field levelName:Ljava/lang/String;

.field logLevel:I


# direct methods
.method static constructor <clinit>()V
    .locals 10

    .prologue
    const/4 v9, 0x4

    const/4 v8, 0x3

    const/4 v7, 0x2

    const/4 v6, 0x1

    const/4 v5, 0x0

    .line 13
    new-instance v0, Lcom/duolebo/tools/DLBLog$LogLevel;

    const-string v1, "Verbose"

    const-string v2, "VERBOSE"

    invoke-direct {v0, v1, v5, v5, v2}, Lcom/duolebo/tools/DLBLog$LogLevel;-><init>(Ljava/lang/String;IILjava/lang/String;)V

    sput-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Verbose:Lcom/duolebo/tools/DLBLog$LogLevel;

    .line 14
    new-instance v0, Lcom/duolebo/tools/DLBLog$LogLevel;

    const-string v1, "Debug"

    const/16 v2, 0x64

    const-string v3, "DEBUG"

    invoke-direct {v0, v1, v6, v2, v3}, Lcom/duolebo/tools/DLBLog$LogLevel;-><init>(Ljava/lang/String;IILjava/lang/String;)V

    sput-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Debug:Lcom/duolebo/tools/DLBLog$LogLevel;

    .line 15
    new-instance v0, Lcom/duolebo/tools/DLBLog$LogLevel;

    const-string v1, "Info"

    const/16 v2, 0xc8

    const-string v3, "INFO"

    invoke-direct {v0, v1, v7, v2, v3}, Lcom/duolebo/tools/DLBLog$LogLevel;-><init>(Ljava/lang/String;IILjava/lang/String;)V

    sput-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Info:Lcom/duolebo/tools/DLBLog$LogLevel;

    .line 16
    new-instance v0, Lcom/duolebo/tools/DLBLog$LogLevel;

    const-string v1, "Warn"

    const/16 v2, 0x12c

    const-string v3, "WARN"

    invoke-direct {v0, v1, v8, v2, v3}, Lcom/duolebo/tools/DLBLog$LogLevel;-><init>(Ljava/lang/String;IILjava/lang/String;)V

    sput-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Warn:Lcom/duolebo/tools/DLBLog$LogLevel;

    .line 17
    new-instance v0, Lcom/duolebo/tools/DLBLog$LogLevel;

    const-string v1, "Error"

    const/16 v2, 0x190

    const-string v3, "ERROR"

    invoke-direct {v0, v1, v9, v2, v3}, Lcom/duolebo/tools/DLBLog$LogLevel;-><init>(Ljava/lang/String;IILjava/lang/String;)V

    sput-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->Error:Lcom/duolebo/tools/DLBLog$LogLevel;

    .line 18
    new-instance v0, Lcom/duolebo/tools/DLBLog$LogLevel;

    const-string v1, "NoLog"

    const/4 v2, 0x5

    const/16 v3, 0x270f

    const-string v4, "NOLOG"

    invoke-direct {v0, v1, v2, v3, v4}, Lcom/duolebo/tools/DLBLog$LogLevel;-><init>(Ljava/lang/String;IILjava/lang/String;)V

    sput-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->NoLog:Lcom/duolebo/tools/DLBLog$LogLevel;

    .line 12
    const/4 v0, 0x6

    new-array v0, v0, [Lcom/duolebo/tools/DLBLog$LogLevel;

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Verbose:Lcom/duolebo/tools/DLBLog$LogLevel;

    aput-object v1, v0, v5

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Debug:Lcom/duolebo/tools/DLBLog$LogLevel;

    aput-object v1, v0, v6

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Info:Lcom/duolebo/tools/DLBLog$LogLevel;

    aput-object v1, v0, v7

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Warn:Lcom/duolebo/tools/DLBLog$LogLevel;

    aput-object v1, v0, v8

    sget-object v1, Lcom/duolebo/tools/DLBLog$LogLevel;->Error:Lcom/duolebo/tools/DLBLog$LogLevel;

    aput-object v1, v0, v9

    const/4 v1, 0x5

    sget-object v2, Lcom/duolebo/tools/DLBLog$LogLevel;->NoLog:Lcom/duolebo/tools/DLBLog$LogLevel;

    aput-object v2, v0, v1

    sput-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->ENUM$VALUES:[Lcom/duolebo/tools/DLBLog$LogLevel;

    return-void
.end method

.method private constructor <init>(Ljava/lang/String;IILjava/lang/String;)V
    .locals 1
    .param p3, "logLevel"    # I
    .param p4, "levelName"    # Ljava/lang/String;

    .prologue
    .line 22
    invoke-direct {p0, p1, p2}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    .line 20
    const/4 v0, -0x1

    iput v0, p0, Lcom/duolebo/tools/DLBLog$LogLevel;->logLevel:I

    .line 21
    const-string v0, ""

    iput-object v0, p0, Lcom/duolebo/tools/DLBLog$LogLevel;->levelName:Ljava/lang/String;

    .line 24
    iput p3, p0, Lcom/duolebo/tools/DLBLog$LogLevel;->logLevel:I

    .line 25
    iput-object p4, p0, Lcom/duolebo/tools/DLBLog$LogLevel;->levelName:Ljava/lang/String;

    .line 26
    return-void
.end method

.method public static valueOf(Ljava/lang/String;)Lcom/duolebo/tools/DLBLog$LogLevel;
    .locals 1

    .prologue
    .line 1
    const-class v0, Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;

    move-result-object v0

    check-cast v0, Lcom/duolebo/tools/DLBLog$LogLevel;

    return-object v0
.end method

.method public static values()[Lcom/duolebo/tools/DLBLog$LogLevel;
    .locals 4

    .prologue
    const/4 v3, 0x0

    .line 1
    sget-object v0, Lcom/duolebo/tools/DLBLog$LogLevel;->ENUM$VALUES:[Lcom/duolebo/tools/DLBLog$LogLevel;

    array-length v1, v0

    new-array v2, v1, [Lcom/duolebo/tools/DLBLog$LogLevel;

    invoke-static {v0, v3, v2, v3, v1}, Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V

    return-object v2
.end method
