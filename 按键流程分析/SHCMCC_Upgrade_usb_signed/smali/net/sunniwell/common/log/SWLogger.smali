.class public Lnet/sunniwell/common/log/SWLogger;
.super Ljava/lang/Object;
.source "SWLogger.java"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lnet/sunniwell/common/log/SWLogger$Level;
    }
.end annotation


# static fields
.field private static final DEFALUT_FORMAT:Ljava/lang/String; = ""

.field private static final FILE:Ljava/lang/String; = "/data/log.properties"


# instance fields
.field private final FORMAT:Ljava/lang/String;

.field private final TAG:Ljava/lang/String;

.field private final level:Lnet/sunniwell/common/log/SWLogger$Level;


# direct methods
.method protected constructor <init>(Lnet/sunniwell/common/log/SWLogger$Level;Ljava/lang/String;Ljava/lang/String;)V
    .locals 0
    .param p1, "level"    # Lnet/sunniwell/common/log/SWLogger$Level;
    .param p2, "TAG"    # Ljava/lang/String;
    .param p3, "Format"    # Ljava/lang/String;

    .prologue
    .line 24
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 25
    iput-object p1, p0, Lnet/sunniwell/common/log/SWLogger;->level:Lnet/sunniwell/common/log/SWLogger$Level;

    .line 26
    iput-object p2, p0, Lnet/sunniwell/common/log/SWLogger;->TAG:Ljava/lang/String;

    .line 27
    iput-object p3, p0, Lnet/sunniwell/common/log/SWLogger;->FORMAT:Ljava/lang/String;

    .line 28
    return-void
.end method

.method private formatStr(ILjava/lang/StackTraceElement;Ljava/lang/String;)Ljava/lang/String;
    .locals 5
    .param p1, "level_intValue"    # I
    .param p2, "ste"    # Ljava/lang/StackTraceElement;
    .param p3, "msg"    # Ljava/lang/String;

    .prologue
    .line 92
    iget-object v0, p0, Lnet/sunniwell/common/log/SWLogger;->FORMAT:Ljava/lang/String;

    .line 93
    .local v0, "ret":Ljava/lang/String;
    const-string v1, "%p"

    invoke-static {p1}, Lnet/sunniwell/common/log/SWLogger$Level;->getLevel(I)Lnet/sunniwell/common/log/SWLogger$Level;

    move-result-object v2

    invoke-virtual {v2}, Lnet/sunniwell/common/log/SWLogger$Level;->name()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    const-string v2, "%t"

    invoke-static {}, Ljava/lang/Thread;->currentThread()Ljava/lang/Thread;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/Thread;->getName()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v1, v2, v3}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    const-string v2, "%n"

    const-string v3, "line.separator"

    invoke-static {v3}, Ljava/lang/System;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v1, v2, v3}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    .line 94
    const-string v2, "%m"

    invoke-virtual {v1, v2, p3}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    const-string v2, "%d"

    new-instance v3, Ljava/text/SimpleDateFormat;

    const-string v4, "yyyyMMddHHmmssSSS"

    invoke-direct {v3, v4}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;)V

    new-instance v4, Ljava/util/Date;

    invoke-direct {v4}, Ljava/util/Date;-><init>()V

    invoke-virtual {v3, v4}, Ljava/text/SimpleDateFormat;->format(Ljava/util/Date;)Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v1, v2, v3}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 95
    if-eqz p2, :cond_0

    .line 96
    const-string v1, "%c"

    invoke-virtual {p2}, Ljava/lang/StackTraceElement;->getClassName()Ljava/lang/String;

    move-result-object v2

    const-string v3, "$"

    const-string v4, "\\$"

    invoke-virtual {v2, v3, v4}, Ljava/lang/String;->replace(Ljava/lang/CharSequence;Ljava/lang/CharSequence;)Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    const-string v2, "%F"

    invoke-virtual {p2}, Ljava/lang/StackTraceElement;->getFileName()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v1, v2, v3}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    const-string v2, "%M"

    invoke-virtual {p2}, Ljava/lang/StackTraceElement;->getMethodName()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v1, v2, v3}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    const-string v2, "%l"

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-virtual {p2}, Ljava/lang/StackTraceElement;->getLineNumber()I

    move-result v4

    invoke-static {v4}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v4

    invoke-direct {v3, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v1, v2, v3}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 99
    :cond_0
    return-object v0
.end method

.method public static getLogger(Ljava/lang/Class;)Lnet/sunniwell/common/log/SWLogger;
    .locals 16
    .param p0, "cls"    # Ljava/lang/Class;

    .prologue
    .line 111
    if-nez p0, :cond_0

    .line 112
    const/4 v14, 0x0

    .line 175
    :goto_0
    return-object v14

    .line 113
    :cond_0
    invoke-virtual/range {p0 .. p0}, Ljava/lang/Class;->getSimpleName()Ljava/lang/String;

    move-result-object v11

    .line 114
    .local v11, "tag":Ljava/lang/String;
    sget-object v12, Lnet/sunniwell/common/log/SWLogger$Level;->none:Lnet/sunniwell/common/log/SWLogger$Level;

    .line 115
    .local v12, "useLevel":Lnet/sunniwell/common/log/SWLogger$Level;
    const-string v4, ""

    .line 116
    .local v4, "format":Ljava/lang/String;
    new-instance v8, Ljava/util/Properties;

    invoke-direct {v8}, Ljava/util/Properties;-><init>()V

    .line 117
    .local v8, "p":Ljava/util/Properties;
    const/4 v5, 0x0

    .line 119
    .local v5, "in":Ljava/io/InputStream;
    new-instance v3, Ljava/io/File;

    const-string v14, "/data/log.properties"

    invoke-direct {v3, v14}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    .line 120
    .local v3, "file":Ljava/io/File;
    invoke-virtual {v3}, Ljava/io/File;->exists()Z

    move-result v14

    if-eqz v14, :cond_9

    .line 122
    :try_start_0
    new-instance v6, Ljava/io/FileInputStream;

    invoke-direct {v6, v3}, Ljava/io/FileInputStream;-><init>(Ljava/io/File;)V
    :try_end_0
    .catch Ljava/io/FileNotFoundException; {:try_start_0 .. :try_end_0} :catch_0

    .end local v5    # "in":Ljava/io/InputStream;
    .local v6, "in":Ljava/io/InputStream;
    move-object v5, v6

    .line 134
    .end local v6    # "in":Ljava/io/InputStream;
    .restart local v5    # "in":Ljava/io/InputStream;
    :cond_1
    :goto_1
    if-eqz v5, :cond_7

    .line 135
    :try_start_1
    invoke-virtual {v8, v5}, Ljava/util/Properties;->load(Ljava/io/InputStream;)V

    .line 137
    invoke-virtual {v8}, Ljava/util/Properties;->isEmpty()Z

    move-result v14

    if-nez v14, :cond_7

    .line 138
    invoke-virtual/range {p0 .. p0}, Ljava/lang/Class;->getPackage()Ljava/lang/Package;

    move-result-object v14

    invoke-virtual {v14}, Ljava/lang/Package;->getName()Ljava/lang/String;

    move-result-object v9

    .line 139
    .local v9, "packageName":Ljava/lang/String;
    invoke-virtual {v8, v9}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v13

    .line 140
    .local v13, "value":Ljava/lang/String;
    if-eqz v13, :cond_2

    invoke-static {v13}, Lnet/sunniwell/common/log/SWLogger$Level;->getLevel(Ljava/lang/String;)Lnet/sunniwell/common/log/SWLogger$Level;

    move-result-object v14

    if-nez v14, :cond_4

    .line 141
    :cond_2
    const/4 v10, -0x1

    .line 142
    .local v10, "pos":I
    :cond_3
    :goto_2
    const-string v14, "."

    invoke-virtual {v9, v14}, Ljava/lang/String;->lastIndexOf(Ljava/lang/String;)I

    move-result v10

    if-gtz v10, :cond_a

    .line 151
    .end local v10    # "pos":I
    :cond_4
    const-string v14, "level"

    invoke-virtual {v8, v14}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v14

    invoke-static {v14}, Lnet/sunniwell/common/log/SWLogger$Level;->getLevel(Ljava/lang/String;)Lnet/sunniwell/common/log/SWLogger$Level;

    move-result-object v0

    .line 152
    .local v0, "defaultLevel":Lnet/sunniwell/common/log/SWLogger$Level;
    if-eqz v0, :cond_5

    .line 153
    move-object v12, v0

    .line 155
    :cond_5
    invoke-static {v13}, Lnet/sunniwell/common/log/SWLogger$Level;->getLevel(Ljava/lang/String;)Lnet/sunniwell/common/log/SWLogger$Level;

    move-result-object v7

    .line 156
    .local v7, "level":Lnet/sunniwell/common/log/SWLogger$Level;
    if-eqz v7, :cond_6

    .line 157
    move-object v12, v7

    .line 159
    :cond_6
    const-string v14, "format"

    invoke-virtual {v8, v14}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    move-result-object v2

    .line 160
    .local v2, "f":Ljava/lang/String;
    if-eqz v2, :cond_7

    .line 161
    move-object v4, v2

    .line 168
    .end local v0    # "defaultLevel":Lnet/sunniwell/common/log/SWLogger$Level;
    .end local v2    # "f":Ljava/lang/String;
    .end local v7    # "level":Lnet/sunniwell/common/log/SWLogger$Level;
    .end local v9    # "packageName":Ljava/lang/String;
    .end local v13    # "value":Ljava/lang/String;
    :cond_7
    if-eqz v5, :cond_8

    .line 170
    :try_start_2
    invoke-virtual {v5}, Ljava/io/InputStream;->close()V
    :try_end_2
    .catch Ljava/io/IOException; {:try_start_2 .. :try_end_2} :catch_4

    .line 175
    :cond_8
    :goto_3
    new-instance v14, Lnet/sunniwell/common/log/SWLogger;

    invoke-direct {v14, v12, v11, v4}, Lnet/sunniwell/common/log/SWLogger;-><init>(Lnet/sunniwell/common/log/SWLogger$Level;Ljava/lang/String;Ljava/lang/String;)V

    goto :goto_0

    .line 123
    :catch_0
    move-exception v1

    .line 124
    .local v1, "e":Ljava/io/FileNotFoundException;
    invoke-virtual {v1}, Ljava/io/FileNotFoundException;->printStackTrace()V

    goto :goto_1

    .line 128
    .end local v1    # "e":Ljava/io/FileNotFoundException;
    :cond_9
    const-class v14, Lnet/sunniwell/common/log/SWLogger;

    const-string v15, "/log.properties"

    invoke-virtual {v14, v15}, Ljava/lang/Class;->getResourceAsStream(Ljava/lang/String;)Ljava/io/InputStream;

    move-result-object v5

    .line 129
    if-nez v5, :cond_1

    .line 130
    const-class v14, Lnet/sunniwell/common/log/SWLogger;

    const-string v15, "log.properties"

    invoke-virtual {v14, v15}, Ljava/lang/Class;->getResourceAsStream(Ljava/lang/String;)Ljava/io/InputStream;

    move-result-object v5

    goto :goto_1

    .line 143
    .restart local v9    # "packageName":Ljava/lang/String;
    .restart local v10    # "pos":I
    .restart local v13    # "value":Ljava/lang/String;
    :cond_a
    const/4 v14, 0x0

    :try_start_3
    invoke-virtual {v9, v14, v10}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v9

    .line 144
    invoke-virtual {v8, v9}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v13

    .line 145
    if-eqz v13, :cond_3

    invoke-static {v13}, Lnet/sunniwell/common/log/SWLogger$Level;->getLevel(Ljava/lang/String;)Lnet/sunniwell/common/log/SWLogger$Level;
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_1
    .catchall {:try_start_3 .. :try_end_3} :catchall_0

    move-result-object v14

    if-nez v14, :cond_4

    goto :goto_2

    .line 165
    .end local v9    # "packageName":Ljava/lang/String;
    .end local v10    # "pos":I
    .end local v13    # "value":Ljava/lang/String;
    :catch_1
    move-exception v1

    .line 166
    .local v1, "e":Ljava/lang/Exception;
    :try_start_4
    invoke-virtual {v1}, Ljava/lang/Exception;->printStackTrace()V
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_0

    .line 168
    if-eqz v5, :cond_8

    .line 170
    :try_start_5
    invoke-virtual {v5}, Ljava/io/InputStream;->close()V
    :try_end_5
    .catch Ljava/io/IOException; {:try_start_5 .. :try_end_5} :catch_2

    goto :goto_3

    .line 171
    :catch_2
    move-exception v1

    .line 172
    .local v1, "e":Ljava/io/IOException;
    invoke-virtual {v1}, Ljava/io/IOException;->printStackTrace()V

    goto :goto_3

    .line 167
    .end local v1    # "e":Ljava/io/IOException;
    :catchall_0
    move-exception v14

    .line 168
    if-eqz v5, :cond_b

    .line 170
    :try_start_6
    invoke-virtual {v5}, Ljava/io/InputStream;->close()V
    :try_end_6
    .catch Ljava/io/IOException; {:try_start_6 .. :try_end_6} :catch_3

    .line 174
    :cond_b
    :goto_4
    throw v14

    .line 171
    :catch_3
    move-exception v1

    .line 172
    .restart local v1    # "e":Ljava/io/IOException;
    invoke-virtual {v1}, Ljava/io/IOException;->printStackTrace()V

    goto :goto_4

    .line 171
    .end local v1    # "e":Ljava/io/IOException;
    :catch_4
    move-exception v1

    .line 172
    .restart local v1    # "e":Ljava/io/IOException;
    invoke-virtual {v1}, Ljava/io/IOException;->printStackTrace()V

    goto :goto_3
.end method

.method private getStackTraceElement()Ljava/lang/StackTraceElement;
    .locals 6

    .prologue
    .line 75
    new-instance v1, Ljava/lang/Throwable;

    invoke-direct {v1}, Ljava/lang/Throwable;-><init>()V

    .line 76
    .local v1, "ex":Ljava/lang/Throwable;
    invoke-virtual {v1}, Ljava/lang/Throwable;->getStackTrace()[Ljava/lang/StackTraceElement;

    move-result-object v3

    .line 77
    .local v3, "stackElements":[Ljava/lang/StackTraceElement;
    const/4 v0, 0x0

    .line 78
    .local v0, "ele":Ljava/lang/StackTraceElement;
    if-eqz v3, :cond_0

    .line 79
    const/4 v2, 0x0

    .local v2, "i":I
    :goto_0
    array-length v4, v3

    if-lt v2, v4, :cond_1

    .line 88
    .end local v2    # "i":I
    :cond_0
    :goto_1
    return-object v0

    .line 80
    .restart local v2    # "i":I
    :cond_1
    aget-object v4, v3, v2

    invoke-virtual {v4}, Ljava/lang/StackTraceElement;->getClassName()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/Class;->getName()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v4, v5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v4

    if-eqz v4, :cond_2

    .line 79
    add-int/lit8 v2, v2, 0x1

    goto :goto_0

    .line 83
    :cond_2
    aget-object v0, v3, v2

    .line 84
    goto :goto_1
.end method

.method private print(Lnet/sunniwell/common/log/SWLogger$Level;Ljava/lang/String;)V
    .locals 3
    .param p1, "l"    # Lnet/sunniwell/common/log/SWLogger$Level;
    .param p2, "msg"    # Ljava/lang/String;

    .prologue
    .line 67
    iget v0, p1, Lnet/sunniwell/common/log/SWLogger$Level;->intValue:I

    .line 69
    .local v0, "intValue":I
    iget-object v1, p0, Lnet/sunniwell/common/log/SWLogger;->level:Lnet/sunniwell/common/log/SWLogger$Level;

    iget v1, v1, Lnet/sunniwell/common/log/SWLogger$Level;->intValue:I

    if-lt v0, v1, :cond_0

    .line 70
    iget-object v1, p0, Lnet/sunniwell/common/log/SWLogger;->TAG:Ljava/lang/String;

    invoke-direct {p0}, Lnet/sunniwell/common/log/SWLogger;->getStackTraceElement()Ljava/lang/StackTraceElement;

    move-result-object v2

    invoke-direct {p0, v0, v2, p2}, Lnet/sunniwell/common/log/SWLogger;->formatStr(ILjava/lang/StackTraceElement;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v1, v2}, Landroid/util/Log;->println(ILjava/lang/String;Ljava/lang/String;)I

    .line 72
    :cond_0
    return-void
.end method


# virtual methods
.method public d(Ljava/lang/String;)V
    .locals 1
    .param p1, "msg"    # Ljava/lang/String;

    .prologue
    .line 36
    sget-object v0, Lnet/sunniwell/common/log/SWLogger$Level;->debug:Lnet/sunniwell/common/log/SWLogger$Level;

    invoke-direct {p0, v0, p1}, Lnet/sunniwell/common/log/SWLogger;->print(Lnet/sunniwell/common/log/SWLogger$Level;Ljava/lang/String;)V

    .line 37
    return-void
.end method

.method public e(Ljava/lang/String;)V
    .locals 1
    .param p1, "msg"    # Ljava/lang/String;

    .prologue
    .line 63
    sget-object v0, Lnet/sunniwell/common/log/SWLogger$Level;->error:Lnet/sunniwell/common/log/SWLogger$Level;

    invoke-direct {p0, v0, p1}, Lnet/sunniwell/common/log/SWLogger;->print(Lnet/sunniwell/common/log/SWLogger$Level;Ljava/lang/String;)V

    .line 64
    return-void
.end method

.method public i(Ljava/lang/String;)V
    .locals 1
    .param p1, "msg"    # Ljava/lang/String;

    .prologue
    .line 45
    sget-object v0, Lnet/sunniwell/common/log/SWLogger$Level;->info:Lnet/sunniwell/common/log/SWLogger$Level;

    invoke-direct {p0, v0, p1}, Lnet/sunniwell/common/log/SWLogger;->print(Lnet/sunniwell/common/log/SWLogger$Level;Ljava/lang/String;)V

    .line 46
    return-void
.end method

.method public w(Ljava/lang/String;)V
    .locals 1
    .param p1, "msg"    # Ljava/lang/String;

    .prologue
    .line 54
    sget-object v0, Lnet/sunniwell/common/log/SWLogger$Level;->warn:Lnet/sunniwell/common/log/SWLogger$Level;

    invoke-direct {p0, v0, p1}, Lnet/sunniwell/common/log/SWLogger;->print(Lnet/sunniwell/common/log/SWLogger$Level;Ljava/lang/String;)V

    .line 55
    return-void
.end method
