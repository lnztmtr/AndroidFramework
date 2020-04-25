.class public Lnet/sunniwell/service/swupgrade/usb/Upgrade;
.super Ljava/lang/Object;
.source "Upgrade.java"


# instance fields
.field private context:Landroid/content/Context;

.field private final fileName:Ljava/lang/String;

.field public myHandler:Landroid/os/Handler;


# direct methods
.method public constructor <init>(Landroid/content/Context;)V
    .locals 1
    .param p1, "context"    # Landroid/content/Context;

    .prologue
    .line 26
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 24
    const-string v0, "update.zip"

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->fileName:Ljava/lang/String;

    .line 250
    new-instance v0, Lnet/sunniwell/service/swupgrade/usb/Upgrade$3;

    invoke-direct {v0, p0}, Lnet/sunniwell/service/swupgrade/usb/Upgrade$3;-><init>(Lnet/sunniwell/service/swupgrade/usb/Upgrade;)V

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->myHandler:Landroid/os/Handler;

    .line 27
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->context:Landroid/content/Context;

    .line 28
    return-void
.end method

.method static synthetic access$000(Lnet/sunniwell/service/swupgrade/usb/Upgrade;)V
    .locals 0
    .param p0, "x0"    # Lnet/sunniwell/service/swupgrade/usb/Upgrade;

    .prologue
    .line 21
    invoke-direct {p0}, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->showDialog()V

    return-void
.end method

.method private checkProjectVersion(Ljava/lang/String;)Z
    .locals 16
    .param p1, "postBuild"    # Ljava/lang/String;

    .prologue
    .line 139
    new-instance v13, Ljava/lang/StringBuilder;

    invoke-direct {v13}, Ljava/lang/StringBuilder;-><init>()V

    const-string v14, "==checkProjectVersion="

    invoke-virtual {v13, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v13

    move-object/from16 v0, p1

    invoke-virtual {v13, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v13

    invoke-virtual {v13}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v13

    invoke-static {v13}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 140
    sget-object v10, Landroid/os/Build$VERSION;->INCREMENTAL:Ljava/lang/String;

    .line 141
    .local v10, "stbVersion":Ljava/lang/String;
    const-string v13, ":"

    move-object/from16 v0, p1

    invoke-virtual {v0, v13}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v11

    .line 143
    .local v11, "tmp":[Ljava/lang/String;
    const-string v12, ""

    .line 145
    .local v12, "version":Ljava/lang/String;
    const/4 v13, 0x1

    :try_start_0
    aget-object v13, v11, v13

    const/4 v14, 0x1

    aget-object v14, v11, v14

    const-string v15, "/"

    invoke-virtual {v14, v15}, Ljava/lang/String;->lastIndexOf(Ljava/lang/String;)I

    move-result v14

    add-int/lit8 v14, v14, 0x1

    invoke-virtual {v13, v14}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v12

    .line 146
    new-instance v13, Ljava/lang/StringBuilder;

    invoke-direct {v13}, Ljava/lang/StringBuilder;-><init>()V

    const-string v14, "==version="

    invoke-virtual {v13, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v13

    invoke-virtual {v13, v12}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v13

    invoke-virtual {v13}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v13

    invoke-static {v13}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 147
    invoke-virtual {v10}, Ljava/lang/String;->length()I

    move-result v9

    .line 148
    .local v9, "stbLen":I
    invoke-virtual {v12}, Ljava/lang/String;->length()I

    move-result v5

    .line 149
    .local v5, "len":I
    if-eq v5, v9, :cond_0

    .line 150
    const/4 v13, 0x0

    .line 168
    .end local v5    # "len":I
    .end local v9    # "stbLen":I
    :goto_0
    return v13

    .line 152
    .restart local v5    # "len":I
    .restart local v9    # "stbLen":I
    :cond_0
    const/4 v13, 0x0

    add-int/lit8 v14, v9, -0x6

    invoke-virtual {v10, v13, v14}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v6

    .line 153
    .local v6, "s1":Ljava/lang/String;
    add-int/lit8 v13, v9, -0x6

    add-int/lit8 v14, v9, -0x3

    invoke-virtual {v10, v13, v14}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v7

    .line 154
    .local v7, "s2":Ljava/lang/String;
    add-int/lit8 v13, v9, -0x3

    invoke-virtual {v10, v13}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v8

    .line 156
    .local v8, "s3":Ljava/lang/String;
    const/4 v13, 0x0

    add-int/lit8 v14, v5, -0x6

    invoke-virtual {v12, v13, v14}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v2

    .line 157
    .local v2, "f1":Ljava/lang/String;
    add-int/lit8 v13, v5, -0x6

    add-int/lit8 v14, v5, -0x3

    invoke-virtual {v12, v13, v14}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v3

    .line 158
    .local v3, "f2":Ljava/lang/String;
    add-int/lit8 v13, v5, -0x3

    invoke-virtual {v12, v13}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v4

    .line 159
    .local v4, "f3":Ljava/lang/String;
    new-instance v13, Ljava/lang/StringBuilder;

    invoke-direct {v13}, Ljava/lang/StringBuilder;-><init>()V

    const-string v14, "==s1="

    invoke-virtual {v13, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v13

    invoke-virtual {v13, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v13

    const-string v14, ",s2="

    invoke-virtual {v13, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v13

    invoke-virtual {v13, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v13

    const-string v14, ",s3="

    invoke-virtual {v13, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v13

    invoke-virtual {v13, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v13

    invoke-virtual {v13}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v13

    invoke-static {v13}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 160
    new-instance v13, Ljava/lang/StringBuilder;

    invoke-direct {v13}, Ljava/lang/StringBuilder;-><init>()V

    const-string v14, "==f1="

    invoke-virtual {v13, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v13

    invoke-virtual {v13, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v13

    const-string v14, ",f2="

    invoke-virtual {v13, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v13

    invoke-virtual {v13, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v13

    const-string v14, ",f3="

    invoke-virtual {v13, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v13

    invoke-virtual {v13, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v13

    invoke-virtual {v13}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v13

    invoke-static {v13}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 161
    invoke-virtual {v6, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v13

    if-eqz v13, :cond_1

    invoke-virtual {v7, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v13

    if-eqz v13, :cond_1

    invoke-virtual {v8, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    move-result v13

    if-nez v13, :cond_1

    .line 162
    const/4 v13, 0x1

    goto/16 :goto_0

    .line 164
    .end local v2    # "f1":Ljava/lang/String;
    .end local v3    # "f2":Ljava/lang/String;
    .end local v4    # "f3":Ljava/lang/String;
    .end local v5    # "len":I
    .end local v6    # "s1":Ljava/lang/String;
    .end local v7    # "s2":Ljava/lang/String;
    .end local v8    # "s3":Ljava/lang/String;
    .end local v9    # "stbLen":I
    :catch_0
    move-exception v1

    .line 165
    .local v1, "e":Ljava/lang/Exception;
    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    .line 166
    const/4 v13, 0x0

    goto/16 :goto_0

    .line 168
    .end local v1    # "e":Ljava/lang/Exception;
    .restart local v2    # "f1":Ljava/lang/String;
    .restart local v3    # "f2":Ljava/lang/String;
    .restart local v4    # "f3":Ljava/lang/String;
    .restart local v5    # "len":I
    .restart local v6    # "s1":Ljava/lang/String;
    .restart local v7    # "s2":Ljava/lang/String;
    .restart local v8    # "s3":Ljava/lang/String;
    .restart local v9    # "stbLen":I
    :cond_1
    const/4 v13, 0x0

    goto/16 :goto_0
.end method

.method private copyfile(Ljava/io/File;)Z
    .locals 13
    .param p1, "zipFile"    # Ljava/io/File;

    .prologue
    const/4 v9, 0x0

    .line 185
    const v10, 0x7f05000b

    invoke-direct {p0, v10}, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->showMess(I)V

    .line 186
    new-instance v2, Ljava/io/File;

    new-instance v10, Ljava/lang/StringBuilder;

    invoke-direct {v10}, Ljava/lang/StringBuilder;-><init>()V

    iget-object v11, p0, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->context:Landroid/content/Context;

    const v12, 0x7f050001

    invoke-virtual {v11, v12}, Landroid/content/Context;->getString(I)Ljava/lang/String;

    move-result-object v11

    invoke-virtual {v10, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v10

    const-string v11, "update.zip"

    invoke-virtual {v10, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v10

    invoke-virtual {v10}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v10

    invoke-direct {v2, v10}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    .line 187
    .local v2, "f":Ljava/io/File;
    new-instance v10, Ljava/lang/StringBuilder;

    invoke-direct {v10}, Ljava/lang/StringBuilder;-><init>()V

    const-string v11, "====save file=="

    invoke-virtual {v10, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v10

    invoke-virtual {v2}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v11

    invoke-virtual {v10, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v10

    invoke-virtual {v10}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v10

    invoke-static {v10}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 188
    invoke-virtual {v2}, Ljava/io/File;->exists()Z

    move-result v10

    if-eqz v10, :cond_0

    .line 189
    invoke-virtual {v2}, Ljava/io/File;->delete()Z

    .line 192
    :cond_0
    :try_start_0
    invoke-virtual {v2}, Ljava/io/File;->createNewFile()Z
    :try_end_0
    .catch Ljava/io/IOException; {:try_start_0 .. :try_end_0} :catch_1

    .line 197
    const/4 v3, 0x0

    .line 198
    .local v3, "input":Ljava/io/InputStream;
    const/4 v7, 0x0

    .line 200
    .local v7, "output":Ljava/io/OutputStream;
    :try_start_1
    new-instance v4, Ljava/io/FileInputStream;

    invoke-direct {v4, p1}, Ljava/io/FileInputStream;-><init>(Ljava/io/File;)V
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_8
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    .line 201
    .end local v3    # "input":Ljava/io/InputStream;
    .local v4, "input":Ljava/io/InputStream;
    :try_start_2
    new-instance v8, Ljava/io/FileOutputStream;

    invoke-direct {v8, v2}, Ljava/io/FileOutputStream;-><init>(Ljava/io/File;)V
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_9
    .catchall {:try_start_2 .. :try_end_2} :catchall_1

    .line 203
    .end local v7    # "output":Ljava/io/OutputStream;
    .local v8, "output":Ljava/io/OutputStream;
    const/16 v10, 0x1000

    :try_start_3
    new-array v0, v10, [B

    .line 204
    .local v0, "buffer":[B
    const/4 v6, 0x0

    .line 205
    .local v6, "n":I
    :goto_0
    const/4 v10, -0x1

    invoke-virtual {v4, v0}, Ljava/io/InputStream;->read([B)I

    move-result v6

    if-eq v10, v6, :cond_3

    .line 206
    const/4 v10, 0x0

    invoke-virtual {v8, v0, v10, v6}, Ljava/io/OutputStream;->write([BII)V
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_0
    .catchall {:try_start_3 .. :try_end_3} :catchall_2

    goto :goto_0

    .line 209
    .end local v0    # "buffer":[B
    .end local v6    # "n":I
    :catch_0
    move-exception v1

    move-object v7, v8

    .end local v8    # "output":Ljava/io/OutputStream;
    .restart local v7    # "output":Ljava/io/OutputStream;
    move-object v3, v4

    .line 210
    .end local v4    # "input":Ljava/io/InputStream;
    .local v1, "e":Ljava/lang/Exception;
    .restart local v3    # "input":Ljava/io/InputStream;
    :goto_1
    :try_start_4
    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_0

    .line 213
    if-eqz v7, :cond_1

    .line 214
    :try_start_5
    invoke-virtual {v7}, Ljava/io/OutputStream;->close()V
    :try_end_5
    .catch Ljava/io/IOException; {:try_start_5 .. :try_end_5} :catch_4

    .line 220
    :cond_1
    :goto_2
    if-eqz v3, :cond_2

    .line 221
    :try_start_6
    invoke-virtual {v3}, Ljava/io/InputStream;->close()V
    :try_end_6
    .catch Ljava/io/IOException; {:try_start_6 .. :try_end_6} :catch_5

    .line 227
    .end local v1    # "e":Ljava/lang/Exception;
    .end local v3    # "input":Ljava/io/InputStream;
    .end local v7    # "output":Ljava/io/OutputStream;
    :cond_2
    :goto_3
    return v9

    .line 193
    :catch_1
    move-exception v1

    .line 194
    .local v1, "e":Ljava/io/IOException;
    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_3

    .line 208
    .end local v1    # "e":Ljava/io/IOException;
    .restart local v0    # "buffer":[B
    .restart local v4    # "input":Ljava/io/InputStream;
    .restart local v6    # "n":I
    .restart local v8    # "output":Ljava/io/OutputStream;
    :cond_3
    const/4 v9, 0x1

    .line 213
    if-eqz v8, :cond_4

    .line 214
    :try_start_7
    invoke-virtual {v8}, Ljava/io/OutputStream;->close()V
    :try_end_7
    .catch Ljava/io/IOException; {:try_start_7 .. :try_end_7} :catch_3

    .line 220
    :cond_4
    :goto_4
    if-eqz v4, :cond_2

    .line 221
    :try_start_8
    invoke-virtual {v4}, Ljava/io/InputStream;->close()V
    :try_end_8
    .catch Ljava/io/IOException; {:try_start_8 .. :try_end_8} :catch_2

    goto :goto_3

    .line 223
    :catch_2
    move-exception v5

    .line 224
    .local v5, "ioe":Ljava/io/IOException;
    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_3

    .line 216
    .end local v5    # "ioe":Ljava/io/IOException;
    :catch_3
    move-exception v5

    .line 217
    .restart local v5    # "ioe":Ljava/io/IOException;
    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_4

    .line 216
    .end local v0    # "buffer":[B
    .end local v4    # "input":Ljava/io/InputStream;
    .end local v5    # "ioe":Ljava/io/IOException;
    .end local v6    # "n":I
    .end local v8    # "output":Ljava/io/OutputStream;
    .local v1, "e":Ljava/lang/Exception;
    .restart local v3    # "input":Ljava/io/InputStream;
    .restart local v7    # "output":Ljava/io/OutputStream;
    :catch_4
    move-exception v5

    .line 217
    .restart local v5    # "ioe":Ljava/io/IOException;
    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_2

    .line 223
    .end local v5    # "ioe":Ljava/io/IOException;
    :catch_5
    move-exception v5

    .line 224
    .restart local v5    # "ioe":Ljava/io/IOException;
    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_3

    .line 212
    .end local v1    # "e":Ljava/lang/Exception;
    .end local v5    # "ioe":Ljava/io/IOException;
    :catchall_0
    move-exception v9

    .line 213
    :goto_5
    if-eqz v7, :cond_5

    .line 214
    :try_start_9
    invoke-virtual {v7}, Ljava/io/OutputStream;->close()V
    :try_end_9
    .catch Ljava/io/IOException; {:try_start_9 .. :try_end_9} :catch_6

    .line 220
    :cond_5
    :goto_6
    if-eqz v3, :cond_6

    .line 221
    :try_start_a
    invoke-virtual {v3}, Ljava/io/InputStream;->close()V
    :try_end_a
    .catch Ljava/io/IOException; {:try_start_a .. :try_end_a} :catch_7

    .line 225
    :cond_6
    :goto_7
    throw v9

    .line 216
    :catch_6
    move-exception v5

    .line 217
    .restart local v5    # "ioe":Ljava/io/IOException;
    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_6

    .line 223
    .end local v5    # "ioe":Ljava/io/IOException;
    :catch_7
    move-exception v5

    .line 224
    .restart local v5    # "ioe":Ljava/io/IOException;
    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_7

    .line 212
    .end local v3    # "input":Ljava/io/InputStream;
    .end local v5    # "ioe":Ljava/io/IOException;
    .restart local v4    # "input":Ljava/io/InputStream;
    :catchall_1
    move-exception v9

    move-object v3, v4

    .end local v4    # "input":Ljava/io/InputStream;
    .restart local v3    # "input":Ljava/io/InputStream;
    goto :goto_5

    .end local v3    # "input":Ljava/io/InputStream;
    .end local v7    # "output":Ljava/io/OutputStream;
    .restart local v4    # "input":Ljava/io/InputStream;
    .restart local v8    # "output":Ljava/io/OutputStream;
    :catchall_2
    move-exception v9

    move-object v7, v8

    .end local v8    # "output":Ljava/io/OutputStream;
    .restart local v7    # "output":Ljava/io/OutputStream;
    move-object v3, v4

    .end local v4    # "input":Ljava/io/InputStream;
    .restart local v3    # "input":Ljava/io/InputStream;
    goto :goto_5

    .line 209
    :catch_8
    move-exception v1

    goto :goto_1

    .end local v3    # "input":Ljava/io/InputStream;
    .restart local v4    # "input":Ljava/io/InputStream;
    :catch_9
    move-exception v1

    move-object v3, v4

    .end local v4    # "input":Ljava/io/InputStream;
    .restart local v3    # "input":Ljava/io/InputStream;
    goto :goto_1
.end method

.method private getVersionInfo(Ljava/io/File;)Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    .locals 12
    .param p1, "zipFile"    # Ljava/io/File;

    .prologue
    .line 49
    if-eqz p1, :cond_0

    invoke-virtual {p1}, Ljava/io/File;->exists()Z

    move-result v11

    if-nez v11, :cond_2

    .line 50
    :cond_0
    const/4 v6, 0x0

    .line 83
    :cond_1
    :goto_0
    return-object v6

    .line 51
    :cond_2
    const/4 v6, 0x0

    .line 52
    .local v6, "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    const/4 v8, 0x0

    .line 53
    .local v8, "zin":Ljava/util/zip/ZipInputStream;
    const/4 v3, 0x0

    .line 54
    .local v3, "inStream":Ljava/io/InputStream;
    const/4 v10, 0x0

    .line 56
    .local v10, "zipEntry":Ljava/util/zip/ZipEntry;
    :try_start_0
    new-instance v4, Ljava/io/FileInputStream;

    invoke-direct {v4, p1}, Ljava/io/FileInputStream;-><init>(Ljava/io/File;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 57
    .end local v3    # "inStream":Ljava/io/InputStream;
    .local v4, "inStream":Ljava/io/InputStream;
    :try_start_1
    new-instance v9, Ljava/util/zip/ZipInputStream;

    invoke-direct {v9, v4}, Ljava/util/zip/ZipInputStream;-><init>(Ljava/io/InputStream;)V
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_4
    .catchall {:try_start_1 .. :try_end_1} :catchall_1

    .line 58
    .end local v8    # "zin":Ljava/util/zip/ZipInputStream;
    .local v9, "zin":Ljava/util/zip/ZipInputStream;
    :cond_3
    :try_start_2
    invoke-virtual {v9}, Ljava/util/zip/ZipInputStream;->getNextEntry()Ljava/util/zip/ZipEntry;

    move-result-object v10

    if-eqz v10, :cond_4

    .line 59
    invoke-virtual {v10}, Ljava/util/zip/ZipEntry;->getName()Ljava/lang/String;

    move-result-object v2

    .line 60
    .local v2, "entryName":Ljava/lang/String;
    const-string v11, "META-INF/com/android/metadata"

    invoke-virtual {v2, v11}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v11

    if-eqz v11, :cond_3

    .line 61
    new-instance v5, Ljava/util/Properties;

    invoke-direct {v5}, Ljava/util/Properties;-><init>()V

    .line 62
    .local v5, "pro":Ljava/util/Properties;
    invoke-virtual {v5, v9}, Ljava/util/Properties;->load(Ljava/io/InputStream;)V

    .line 63
    new-instance v7, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;

    invoke-direct {v7}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;-><init>()V
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_5
    .catchall {:try_start_2 .. :try_end_2} :catchall_2

    .line 64
    .end local v6    # "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    .local v7, "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    :try_start_3
    const-string v11, "post-build"

    invoke-virtual {v5, v11}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v11

    invoke-virtual {v7, v11}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->setPostBuild(Ljava/lang/String;)V

    .line 65
    const-string v11, "pre-build"

    invoke-virtual {v5, v11}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v11

    invoke-virtual {v7, v11}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->setPreBuild(Ljava/lang/String;)V

    .line 66
    const-string v11, "pre-device"

    invoke-virtual {v5, v11}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v11

    invoke-virtual {v7, v11}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->setPreDevice(Ljava/lang/String;)V

    .line 67
    const-string v11, "pre-timestamp"

    invoke-virtual {v5, v11}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v11

    invoke-virtual {v7, v11}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->setPostTimestamp(Ljava/lang/String;)V
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_6
    .catchall {:try_start_3 .. :try_end_3} :catchall_3

    move-object v6, v7

    .line 75
    .end local v2    # "entryName":Ljava/lang/String;
    .end local v5    # "pro":Ljava/util/Properties;
    .end local v7    # "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    .restart local v6    # "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    :cond_4
    if-eqz v9, :cond_5

    .line 76
    :try_start_4
    invoke-virtual {v9}, Ljava/util/zip/ZipInputStream;->close()V

    .line 77
    :cond_5
    if-eqz v4, :cond_6

    .line 78
    invoke-virtual {v4}, Ljava/io/InputStream;->close()V
    :try_end_4
    .catch Ljava/io/IOException; {:try_start_4 .. :try_end_4} :catch_0

    :cond_6
    move-object v3, v4

    .end local v4    # "inStream":Ljava/io/InputStream;
    .restart local v3    # "inStream":Ljava/io/InputStream;
    move-object v8, v9

    .line 81
    .end local v9    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v8    # "zin":Ljava/util/zip/ZipInputStream;
    goto :goto_0

    .line 79
    .end local v3    # "inStream":Ljava/io/InputStream;
    .end local v8    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v4    # "inStream":Ljava/io/InputStream;
    .restart local v9    # "zin":Ljava/util/zip/ZipInputStream;
    :catch_0
    move-exception v1

    .line 80
    .local v1, "e1":Ljava/io/IOException;
    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    move-object v3, v4

    .end local v4    # "inStream":Ljava/io/InputStream;
    .restart local v3    # "inStream":Ljava/io/InputStream;
    move-object v8, v9

    .line 82
    .end local v9    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v8    # "zin":Ljava/util/zip/ZipInputStream;
    goto :goto_0

    .line 71
    .end local v1    # "e1":Ljava/io/IOException;
    :catch_1
    move-exception v0

    .line 72
    .local v0, "e":Ljava/lang/Exception;
    :goto_1
    :try_start_5
    invoke-static {v0}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V
    :try_end_5
    .catchall {:try_start_5 .. :try_end_5} :catchall_0

    .line 75
    if-eqz v8, :cond_7

    .line 76
    :try_start_6
    invoke-virtual {v8}, Ljava/util/zip/ZipInputStream;->close()V

    .line 77
    :cond_7
    if-eqz v3, :cond_1

    .line 78
    invoke-virtual {v3}, Ljava/io/InputStream;->close()V
    :try_end_6
    .catch Ljava/io/IOException; {:try_start_6 .. :try_end_6} :catch_2

    goto :goto_0

    .line 79
    :catch_2
    move-exception v1

    .line 80
    .restart local v1    # "e1":Ljava/io/IOException;
    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_0

    .line 74
    .end local v0    # "e":Ljava/lang/Exception;
    .end local v1    # "e1":Ljava/io/IOException;
    :catchall_0
    move-exception v11

    .line 75
    :goto_2
    if-eqz v8, :cond_8

    .line 76
    :try_start_7
    invoke-virtual {v8}, Ljava/util/zip/ZipInputStream;->close()V

    .line 77
    :cond_8
    if-eqz v3, :cond_9

    .line 78
    invoke-virtual {v3}, Ljava/io/InputStream;->close()V
    :try_end_7
    .catch Ljava/io/IOException; {:try_start_7 .. :try_end_7} :catch_3

    .line 81
    :cond_9
    :goto_3
    throw v11

    .line 79
    :catch_3
    move-exception v1

    .line 80
    .restart local v1    # "e1":Ljava/io/IOException;
    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_3

    .line 74
    .end local v1    # "e1":Ljava/io/IOException;
    .end local v3    # "inStream":Ljava/io/InputStream;
    .restart local v4    # "inStream":Ljava/io/InputStream;
    :catchall_1
    move-exception v11

    move-object v3, v4

    .end local v4    # "inStream":Ljava/io/InputStream;
    .restart local v3    # "inStream":Ljava/io/InputStream;
    goto :goto_2

    .end local v3    # "inStream":Ljava/io/InputStream;
    .end local v8    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v4    # "inStream":Ljava/io/InputStream;
    .restart local v9    # "zin":Ljava/util/zip/ZipInputStream;
    :catchall_2
    move-exception v11

    move-object v3, v4

    .end local v4    # "inStream":Ljava/io/InputStream;
    .restart local v3    # "inStream":Ljava/io/InputStream;
    move-object v8, v9

    .end local v9    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v8    # "zin":Ljava/util/zip/ZipInputStream;
    goto :goto_2

    .end local v3    # "inStream":Ljava/io/InputStream;
    .end local v6    # "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    .end local v8    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v2    # "entryName":Ljava/lang/String;
    .restart local v4    # "inStream":Ljava/io/InputStream;
    .restart local v5    # "pro":Ljava/util/Properties;
    .restart local v7    # "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    .restart local v9    # "zin":Ljava/util/zip/ZipInputStream;
    :catchall_3
    move-exception v11

    move-object v3, v4

    .end local v4    # "inStream":Ljava/io/InputStream;
    .restart local v3    # "inStream":Ljava/io/InputStream;
    move-object v8, v9

    .end local v9    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v8    # "zin":Ljava/util/zip/ZipInputStream;
    move-object v6, v7

    .end local v7    # "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    .restart local v6    # "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    goto :goto_2

    .line 71
    .end local v2    # "entryName":Ljava/lang/String;
    .end local v3    # "inStream":Ljava/io/InputStream;
    .end local v5    # "pro":Ljava/util/Properties;
    .restart local v4    # "inStream":Ljava/io/InputStream;
    :catch_4
    move-exception v0

    move-object v3, v4

    .end local v4    # "inStream":Ljava/io/InputStream;
    .restart local v3    # "inStream":Ljava/io/InputStream;
    goto :goto_1

    .end local v3    # "inStream":Ljava/io/InputStream;
    .end local v8    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v4    # "inStream":Ljava/io/InputStream;
    .restart local v9    # "zin":Ljava/util/zip/ZipInputStream;
    :catch_5
    move-exception v0

    move-object v3, v4

    .end local v4    # "inStream":Ljava/io/InputStream;
    .restart local v3    # "inStream":Ljava/io/InputStream;
    move-object v8, v9

    .end local v9    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v8    # "zin":Ljava/util/zip/ZipInputStream;
    goto :goto_1

    .end local v3    # "inStream":Ljava/io/InputStream;
    .end local v6    # "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    .end local v8    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v2    # "entryName":Ljava/lang/String;
    .restart local v4    # "inStream":Ljava/io/InputStream;
    .restart local v5    # "pro":Ljava/util/Properties;
    .restart local v7    # "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    .restart local v9    # "zin":Ljava/util/zip/ZipInputStream;
    :catch_6
    move-exception v0

    move-object v3, v4

    .end local v4    # "inStream":Ljava/io/InputStream;
    .restart local v3    # "inStream":Ljava/io/InputStream;
    move-object v8, v9

    .end local v9    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v8    # "zin":Ljava/util/zip/ZipInputStream;
    move-object v6, v7

    .end local v7    # "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    .restart local v6    # "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    goto :goto_1
.end method

.method private showDialog()V
    .locals 3

    .prologue
    .line 231
    new-instance v0, Landroid/app/AlertDialog$Builder;

    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->context:Landroid/content/Context;

    invoke-direct {v0, v1}, Landroid/app/AlertDialog$Builder;-><init>(Landroid/content/Context;)V

    const v1, 0x7f050002

    .line 232
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog$Builder;->setTitle(I)Landroid/app/AlertDialog$Builder;

    move-result-object v0

    const-string v1, "\u786e\u5b9a\u5417\uff1f"

    .line 233
    invoke-virtual {v0, v1}, Landroid/app/AlertDialog$Builder;->setMessage(Ljava/lang/CharSequence;)Landroid/app/AlertDialog$Builder;

    move-result-object v0

    const-string v1, "\u662f"

    new-instance v2, Lnet/sunniwell/service/swupgrade/usb/Upgrade$2;

    invoke-direct {v2, p0}, Lnet/sunniwell/service/swupgrade/usb/Upgrade$2;-><init>(Lnet/sunniwell/service/swupgrade/usb/Upgrade;)V

    .line 234
    invoke-virtual {v0, v1, v2}, Landroid/app/AlertDialog$Builder;->setPositiveButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;

    move-result-object v0

    const-string v1, "\u5426"

    new-instance v2, Lnet/sunniwell/service/swupgrade/usb/Upgrade$1;

    invoke-direct {v2, p0}, Lnet/sunniwell/service/swupgrade/usb/Upgrade$1;-><init>(Lnet/sunniwell/service/swupgrade/usb/Upgrade;)V

    .line 241
    invoke-virtual {v0, v1, v2}, Landroid/app/AlertDialog$Builder;->setNegativeButton(Ljava/lang/CharSequence;Landroid/content/DialogInterface$OnClickListener;)Landroid/app/AlertDialog$Builder;

    move-result-object v0

    .line 248
    invoke-virtual {v0}, Landroid/app/AlertDialog$Builder;->show()Landroid/app/AlertDialog;

    .line 249
    return-void
.end method

.method private showMess(I)V
    .locals 2
    .param p1, "id"    # I

    .prologue
    .line 177
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->context:Landroid/content/Context;

    const/4 v1, 0x1

    invoke-static {v0, p1, v1}, Landroid/widget/Toast;->makeText(Landroid/content/Context;II)Landroid/widget/Toast;

    move-result-object v0

    invoke-virtual {v0}, Landroid/widget/Toast;->show()V

    .line 178
    return-void
.end method


# virtual methods
.method public checkVersion(Lnet/sunniwell/service/swupgrade/usb/VersionInfo;)Z
    .locals 7
    .param p1, "vInfo"    # Lnet/sunniwell/service/swupgrade/usb/VersionInfo;

    .prologue
    const v6, 0x7f050008

    const v5, 0x7f050009

    const/4 v2, 0x0

    .line 93
    const v3, 0x7f050007

    invoke-direct {p0, v3}, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->showMess(I)V

    .line 94
    sget-object v1, Landroid/os/Build;->FINGERPRINT:Ljava/lang/String;

    .line 95
    .local v1, "stbPreDevice":Ljava/lang/String;
    sget-object v0, Landroid/os/Build;->DEVICE:Ljava/lang/String;

    .line 96
    .local v0, "device":Ljava/lang/String;
    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, "STB===stbPreDevice="

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    const-string v4, ",device="

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v3}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 97
    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, "File===PostBuild="

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPostBuild()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    const-string v4, ",device="

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPreDevice()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    const-string v4, ",PreBuild="

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPreBuild()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    const-string v4, ",PostTimestamp="

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPostTimestamp()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v3}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 99
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPreDevice()Ljava/lang/String;

    move-result-object v3

    if-nez v3, :cond_0

    .line 100
    invoke-direct {p0, v6}, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->showMess(I)V

    .line 126
    :goto_0
    return v2

    .line 103
    :cond_0
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPreDevice()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_1

    .line 104
    invoke-direct {p0, v6}, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->showMess(I)V

    goto :goto_0

    .line 108
    :cond_1
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPostBuild()Ljava/lang/String;

    move-result-object v3

    if-nez v3, :cond_2

    .line 109
    invoke-direct {p0, v5}, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->showMess(I)V

    goto :goto_0

    .line 112
    :cond_2
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPostBuild()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v3, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_3

    .line 114
    const v3, 0x7f05000a

    invoke-direct {p0, v3}, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->showMess(I)V

    goto :goto_0

    .line 117
    :cond_3
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPostBuild()Ljava/lang/String;

    move-result-object v3

    invoke-direct {p0, v3}, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->checkProjectVersion(Ljava/lang/String;)Z

    move-result v3

    if-nez v3, :cond_4

    .line 118
    invoke-direct {p0, v5}, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->showMess(I)V

    goto :goto_0

    .line 121
    :cond_4
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPreBuild()Ljava/lang/String;

    move-result-object v3

    if-eqz v3, :cond_5

    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPreBuild()Ljava/lang/String;

    move-result-object v3

    const-string v4, ""

    invoke-virtual {v3, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_5

    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPreBuild()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v3, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_5

    .line 123
    invoke-direct {p0, v5}, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->showMess(I)V

    goto :goto_0

    .line 126
    :cond_5
    const/4 v2, 0x1

    goto :goto_0
.end method

.method public start(Ljava/io/File;)V
    .locals 3
    .param p1, "zipFile"    # Ljava/io/File;

    .prologue
    .line 34
    invoke-direct {p0, p1}, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->getVersionInfo(Ljava/io/File;)Lnet/sunniwell/service/swupgrade/usb/VersionInfo;

    move-result-object v0

    .line 35
    .local v0, "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    if-nez v0, :cond_1

    .line 36
    const v1, 0x7f050006

    invoke-direct {p0, v1}, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->showMess(I)V

    .line 42
    :cond_0
    :goto_0
    return-void

    .line 39
    :cond_1
    invoke-virtual {p0, v0}, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->checkVersion(Lnet/sunniwell/service/swupgrade/usb/VersionInfo;)Z

    move-result v1

    if-eqz v1, :cond_0

    .line 40
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->myHandler:Landroid/os/Handler;

    const/4 v2, 0x0

    invoke-virtual {v1, v2}, Landroid/os/Handler;->sendEmptyMessage(I)Z

    goto :goto_0
.end method

.method public stop()V
    .locals 0

    .prologue
    .line 266
    return-void
.end method
