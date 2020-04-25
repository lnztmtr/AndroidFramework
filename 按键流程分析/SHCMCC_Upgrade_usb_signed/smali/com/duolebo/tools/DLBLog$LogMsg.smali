.class public Lcom/duolebo/tools/DLBLog$LogMsg;
.super Ljava/lang/Object;
.source "DLBLog.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/duolebo/tools/DLBLog;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x9
    name = "LogMsg"
.end annotation


# instance fields
.field loglevel:Lcom/duolebo/tools/DLBLog$LogLevel;

.field msg:Ljava/lang/String;

.field tag:Ljava/lang/String;


# direct methods
.method public constructor <init>(Lcom/duolebo/tools/DLBLog$LogLevel;Ljava/lang/String;Ljava/lang/String;)V
    .locals 0
    .param p1, "loglevel"    # Lcom/duolebo/tools/DLBLog$LogLevel;
    .param p2, "tag"    # Ljava/lang/String;
    .param p3, "msg"    # Ljava/lang/String;

    .prologue
    .line 602
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 603
    iput-object p1, p0, Lcom/duolebo/tools/DLBLog$LogMsg;->loglevel:Lcom/duolebo/tools/DLBLog$LogLevel;

    .line 604
    iput-object p2, p0, Lcom/duolebo/tools/DLBLog$LogMsg;->tag:Ljava/lang/String;

    .line 605
    iput-object p3, p0, Lcom/duolebo/tools/DLBLog$LogMsg;->msg:Ljava/lang/String;

    .line 606
    return-void
.end method
