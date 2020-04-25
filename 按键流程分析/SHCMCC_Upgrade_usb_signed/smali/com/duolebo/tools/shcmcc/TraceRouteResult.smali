.class public Lcom/duolebo/tools/shcmcc/TraceRouteResult;
.super Ljava/lang/Object;
.source "TraceRouteResult.java"


# instance fields
.field private mErrorDescription:Ljava/lang/StringBuffer;

.field private mIP:Ljava/lang/String;

.field private mResult:Ljava/util/ArrayList;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/ArrayList",
            "<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation
.end field

.field private mTime:Ljava/lang/String;


# direct methods
.method public constructor <init>()V
    .locals 1

    .prologue
    .line 11
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 13
    const-string v0, "127.0.0.1"

    iput-object v0, p0, Lcom/duolebo/tools/shcmcc/TraceRouteResult;->mIP:Ljava/lang/String;

    .line 14
    const-string v0, "30"

    iput-object v0, p0, Lcom/duolebo/tools/shcmcc/TraceRouteResult;->mTime:Ljava/lang/String;

    .line 15
    new-instance v0, Ljava/util/ArrayList;

    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V

    iput-object v0, p0, Lcom/duolebo/tools/shcmcc/TraceRouteResult;->mResult:Ljava/util/ArrayList;

    .line 16
    new-instance v0, Ljava/lang/StringBuffer;

    invoke-direct {v0}, Ljava/lang/StringBuffer;-><init>()V

    iput-object v0, p0, Lcom/duolebo/tools/shcmcc/TraceRouteResult;->mErrorDescription:Ljava/lang/StringBuffer;

    .line 11
    return-void
.end method

.method public static netCheck(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)Lcom/duolebo/tools/shcmcc/TraceRouteResult;
    .locals 2
    .param p0, "context"    # Landroid/content/Context;
    .param p1, "ip"    # Ljava/lang/String;
    .param p2, "time"    # Ljava/lang/String;

    .prologue
    .line 19
    new-instance v0, Lcom/duolebo/tools/shcmcc/TraceRouteResult;

    invoke-direct {v0}, Lcom/duolebo/tools/shcmcc/TraceRouteResult;-><init>()V

    .line 21
    .local v0, "result":Lcom/duolebo/tools/shcmcc/TraceRouteResult;
    :try_start_0
    iput-object p1, v0, Lcom/duolebo/tools/shcmcc/TraceRouteResult;->mIP:Ljava/lang/String;

    .line 22
    iput-object p2, v0, Lcom/duolebo/tools/shcmcc/TraceRouteResult;->mTime:Ljava/lang/String;

    .line 23
    invoke-virtual {v0, p0}, Lcom/duolebo/tools/shcmcc/TraceRouteResult;->startCheck(Landroid/content/Context;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    .line 26
    :goto_0
    return-object v0

    .line 24
    :catch_0
    move-exception v1

    goto :goto_0
.end method


# virtual methods
.method public empty()Z
    .locals 1

    .prologue
    .line 65
    iget-object v0, p0, Lcom/duolebo/tools/shcmcc/TraceRouteResult;->mResult:Ljava/util/ArrayList;

    invoke-virtual {v0}, Ljava/util/ArrayList;->isEmpty()Z

    move-result v0

    return v0
.end method

.method public getError()Ljava/lang/String;
    .locals 1

    .prologue
    .line 61
    iget-object v0, p0, Lcom/duolebo/tools/shcmcc/TraceRouteResult;->mErrorDescription:Ljava/lang/StringBuffer;

    invoke-virtual {v0}, Ljava/lang/StringBuffer;->toString()Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getResult()Ljava/util/ArrayList;
    .locals 1
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/ArrayList",
            "<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation

    .prologue
    .line 69
    iget-object v0, p0, Lcom/duolebo/tools/shcmcc/TraceRouteResult;->mResult:Ljava/util/ArrayList;

    return-object v0
.end method

.method public startCheck(Landroid/content/Context;)V
    .locals 9
    .param p1, "context"    # Landroid/content/Context;

    .prologue
    .line 31
    :try_start_0
    iget-object v7, p0, Lcom/duolebo/tools/shcmcc/TraceRouteResult;->mIP:Ljava/lang/String;

    invoke-static {v7}, Ljava/net/Inet4Address;->getByName(Ljava/lang/String;)Ljava/net/InetAddress;

    move-result-object v2

    .line 32
    .local v2, "inetAddress":Ljava/net/InetAddress;
    invoke-virtual {v2}, Ljava/net/InetAddress;->getHostAddress()Ljava/lang/String;

    move-result-object v6

    .line 34
    .local v6, "traceIp":Ljava/lang/String;
    const/4 v1, 0x0

    .line 35
    .local v1, "cmd":Ljava/lang/String;
    const/4 v5, 0x0

    .line 36
    .local v5, "mProcess":Ljava/lang/Process;
    const/4 v3, 0x0

    .line 37
    .local v3, "input":Ljava/io/InputStream;
    const/4 v0, 0x0

    .line 38
    .local v0, "bufReader":Ljava/io/BufferedReader;
    const-string v4, ""

    .line 40
    .local v4, "line":Ljava/lang/String;
    new-instance v7, Ljava/lang/StringBuilder;

    const-string v8, "traceroute -n -m "

    invoke-direct {v7, v8}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    iget-object v8, p0, Lcom/duolebo/tools/shcmcc/TraceRouteResult;->mTime:Ljava/lang/String;

    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v7

    const-string v8, " "

    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v7

    invoke-virtual {v7, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v7

    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    .line 41
    invoke-static {}, Ljava/lang/Runtime;->getRuntime()Ljava/lang/Runtime;

    move-result-object v7

    invoke-virtual {v7, v1}, Ljava/lang/Runtime;->exec(Ljava/lang/String;)Ljava/lang/Process;

    move-result-object v5

    .line 43
    invoke-virtual {v5}, Ljava/lang/Process;->getInputStream()Ljava/io/InputStream;

    move-result-object v3

    .line 44
    new-instance v0, Ljava/io/BufferedReader;

    .end local v0    # "bufReader":Ljava/io/BufferedReader;
    new-instance v7, Ljava/io/InputStreamReader;

    invoke-direct {v7, v3}, Ljava/io/InputStreamReader;-><init>(Ljava/io/InputStream;)V

    invoke-direct {v0, v7}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;)V

    .line 45
    .restart local v0    # "bufReader":Ljava/io/BufferedReader;
    :goto_0
    invoke-virtual {v0}, Ljava/io/BufferedReader;->readLine()Ljava/lang/String;

    move-result-object v4

    if-nez v4, :cond_0

    .line 50
    invoke-virtual {v5}, Ljava/lang/Process;->getErrorStream()Ljava/io/InputStream;

    move-result-object v3

    .line 51
    new-instance v0, Ljava/io/BufferedReader;

    .end local v0    # "bufReader":Ljava/io/BufferedReader;
    new-instance v7, Ljava/io/InputStreamReader;

    invoke-direct {v7, v3}, Ljava/io/InputStreamReader;-><init>(Ljava/io/InputStream;)V

    invoke-direct {v0, v7}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;)V

    .line 52
    .restart local v0    # "bufReader":Ljava/io/BufferedReader;
    :goto_1
    invoke-virtual {v0}, Ljava/io/BufferedReader;->readLine()Ljava/lang/String;

    move-result-object v4

    if-nez v4, :cond_1

    .line 58
    .end local v0    # "bufReader":Ljava/io/BufferedReader;
    .end local v1    # "cmd":Ljava/lang/String;
    .end local v2    # "inetAddress":Ljava/net/InetAddress;
    .end local v3    # "input":Ljava/io/InputStream;
    .end local v4    # "line":Ljava/lang/String;
    .end local v5    # "mProcess":Ljava/lang/Process;
    .end local v6    # "traceIp":Ljava/lang/String;
    :goto_2
    return-void

    .line 46
    .restart local v0    # "bufReader":Ljava/io/BufferedReader;
    .restart local v1    # "cmd":Ljava/lang/String;
    .restart local v2    # "inetAddress":Ljava/net/InetAddress;
    .restart local v3    # "input":Ljava/io/InputStream;
    .restart local v4    # "line":Ljava/lang/String;
    .restart local v5    # "mProcess":Ljava/lang/Process;
    .restart local v6    # "traceIp":Ljava/lang/String;
    :cond_0
    iget-object v7, p0, Lcom/duolebo/tools/shcmcc/TraceRouteResult;->mResult:Ljava/util/ArrayList;

    invoke-virtual {v7, v4}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    goto :goto_0

    .line 56
    .end local v0    # "bufReader":Ljava/io/BufferedReader;
    .end local v1    # "cmd":Ljava/lang/String;
    .end local v2    # "inetAddress":Ljava/net/InetAddress;
    .end local v3    # "input":Ljava/io/InputStream;
    .end local v4    # "line":Ljava/lang/String;
    .end local v5    # "mProcess":Ljava/lang/Process;
    .end local v6    # "traceIp":Ljava/lang/String;
    :catch_0
    move-exception v7

    goto :goto_2

    .line 53
    .restart local v0    # "bufReader":Ljava/io/BufferedReader;
    .restart local v1    # "cmd":Ljava/lang/String;
    .restart local v2    # "inetAddress":Ljava/net/InetAddress;
    .restart local v3    # "input":Ljava/io/InputStream;
    .restart local v4    # "line":Ljava/lang/String;
    .restart local v5    # "mProcess":Ljava/lang/Process;
    .restart local v6    # "traceIp":Ljava/lang/String;
    :cond_1
    iget-object v7, p0, Lcom/duolebo/tools/shcmcc/TraceRouteResult;->mErrorDescription:Ljava/lang/StringBuffer;

    invoke-virtual {v7, v4}, Ljava/lang/StringBuffer;->append(Ljava/lang/String;)Ljava/lang/StringBuffer;

    .line 54
    iget-object v7, p0, Lcom/duolebo/tools/shcmcc/TraceRouteResult;->mErrorDescription:Ljava/lang/StringBuffer;

    const-string v8, "\n"

    invoke-virtual {v7, v8}, Ljava/lang/StringBuffer;->append(Ljava/lang/String;)Ljava/lang/StringBuffer;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_1
.end method
