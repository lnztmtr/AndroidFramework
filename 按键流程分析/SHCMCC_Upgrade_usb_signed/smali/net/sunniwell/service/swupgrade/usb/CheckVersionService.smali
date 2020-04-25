.class public Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;
.super Landroid/app/Service;
.source "CheckVersionService.java"


# instance fields
.field private keyString:Ljava/util/HashMap;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/HashMap",
            "<",
            "Ljava/lang/String;",
            "Ljava/io/File;",
            ">;"
        }
    .end annotation
.end field

.field private mContext:Landroid/content/Context;

.field public final myHandler:Landroid/os/Handler;

.field private pro:Ljava/util/Properties;

.field private zipFile:Ljava/io/File;

.field private zipFilePath:Ljava/io/File;

.field private zipfile:Ljava/util/ArrayList;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/ArrayList",
            "<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation
.end field

.field private zipfilesName:Ljava/util/ArrayList;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/ArrayList",
            "<",
            "Ljava/lang/String;",
            ">;"
        }
    .end annotation
.end field


# direct methods
.method public constructor <init>()V
    .locals 1

    .prologue
    const/4 v0, 0x0

    .line 23
    invoke-direct {p0}, Landroid/app/Service;-><init>()V

    .line 24
    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->zipFile:Ljava/io/File;

    .line 26
    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->zipfilesName:Ljava/util/ArrayList;

    .line 27
    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->zipFilePath:Ljava/io/File;

    .line 29
    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->zipfile:Ljava/util/ArrayList;

    .line 133
    new-instance v0, Ljava/util/HashMap;

    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->keyString:Ljava/util/HashMap;

    .line 292
    new-instance v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService$1;

    invoke-direct {v0, p0}, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService$1;-><init>(Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;)V

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->myHandler:Landroid/os/Handler;

    return-void
.end method

.method private ZipFilelist(Ljava/io/File;)V
    .locals 18
    .param p1, "zipFile"    # Ljava/io/File;

    .prologue
    .line 63
    sget-object v9, Landroid/os/Build;->FINGERPRINT:Ljava/lang/String;

    .line 64
    .local v9, "stbPreDevice":Ljava/lang/String;
    if-eqz p1, :cond_0

    invoke-virtual/range {p1 .. p1}, Ljava/io/File;->exists()Z

    move-result v13

    if-nez v13, :cond_1

    .line 116
    :cond_0
    :goto_0
    return-void

    .line 67
    :cond_1
    const/4 v10, 0x0

    .line 68
    .local v10, "zin":Ljava/util/zip/ZipInputStream;
    const/4 v6, 0x0

    .line 69
    .local v6, "inStream":Ljava/io/InputStream;
    const/4 v12, 0x0

    .line 71
    .local v12, "zipEntry":Ljava/util/zip/ZipEntry;
    :try_start_0
    new-instance v7, Ljava/io/FileInputStream;

    move-object/from16 v0, p1

    invoke-direct {v7, v0}, Ljava/io/FileInputStream;-><init>(Ljava/io/File;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_4
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 72
    .end local v6    # "inStream":Ljava/io/InputStream;
    .local v7, "inStream":Ljava/io/InputStream;
    :try_start_1
    new-instance v11, Ljava/util/zip/ZipInputStream;

    invoke-direct {v11, v7}, Ljava/util/zip/ZipInputStream;-><init>(Ljava/io/InputStream;)V
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_5
    .catchall {:try_start_1 .. :try_end_1} :catchall_1

    .line 73
    .end local v10    # "zin":Ljava/util/zip/ZipInputStream;
    .local v11, "zin":Ljava/util/zip/ZipInputStream;
    :cond_2
    :try_start_2
    invoke-virtual {v11}, Ljava/util/zip/ZipInputStream;->getNextEntry()Ljava/util/zip/ZipEntry;

    move-result-object v12

    if-eqz v12, :cond_3

    .line 74
    invoke-virtual {v12}, Ljava/util/zip/ZipEntry;->getName()Ljava/lang/String;

    move-result-object v4

    .line 75
    .local v4, "entryName":Ljava/lang/String;
    const-string v13, "META-INF/com/android/metadata"

    invoke-virtual {v4, v13}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v13

    if-eqz v13, :cond_2

    .line 76
    new-instance v13, Ljava/util/Properties;

    invoke-direct {v13}, Ljava/util/Properties;-><init>()V

    move-object/from16 v0, p0

    iput-object v13, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->pro:Ljava/util/Properties;

    .line 77
    move-object/from16 v0, p0

    iget-object v13, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->pro:Ljava/util/Properties;

    invoke-virtual {v13, v11}, Ljava/util/Properties;->load(Ljava/io/InputStream;)V

    .line 78
    move-object/from16 v0, p0

    iget-object v13, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->pro:Ljava/util/Properties;

    const-string v14, "post-build"

    invoke-virtual {v13, v14}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v13

    invoke-virtual {v13, v9}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v13

    if-nez v13, :cond_3

    .line 79
    new-instance v13, Ljava/lang/StringBuilder;

    invoke-direct {v13}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual/range {p0 .. p0}, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->getResources()Landroid/content/res/Resources;

    move-result-object v14

    const v15, 0x7f050013

    invoke-virtual {v14, v15}, Landroid/content/res/Resources;->getText(I)Ljava/lang/CharSequence;

    move-result-object v14

    invoke-virtual {v13, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object v13

    move-object/from16 v0, p0

    iget-object v14, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->pro:Ljava/util/Properties;

    const-string v15, "post-build"

    invoke-virtual {v14, v15}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v14

    move-object/from16 v0, p0

    iget-object v15, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->pro:Ljava/util/Properties;

    const-string v16, "post-build"

    invoke-virtual/range {v15 .. v16}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v15

    invoke-virtual {v15}, Ljava/lang/String;->length()I

    move-result v15

    add-int/lit8 v15, v15, -0x17

    move-object/from16 v0, p0

    iget-object v0, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->pro:Ljava/util/Properties;

    move-object/from16 v16, v0

    const-string v17, "post-build"

    .line 80
    invoke-virtual/range {v16 .. v17}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v16

    invoke-virtual/range {v16 .. v16}, Ljava/lang/String;->length()I

    move-result v16

    add-int/lit8 v16, v16, -0xf

    .line 79
    invoke-virtual/range {v14 .. v16}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v14

    invoke-virtual {v13, v14}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v13

    invoke-virtual {v13}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v8

    .line 81
    .local v8, "postbuild":Ljava/lang/String;
    move-object/from16 v0, p0

    iget-object v13, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->keyString:Ljava/util/HashMap;

    move-object/from16 v0, p0

    iget-object v14, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->pro:Ljava/util/Properties;

    const-string v15, "post-build"

    invoke-virtual {v14, v15}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v14

    invoke-virtual {v13, v14}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v13

    if-nez v13, :cond_6

    .line 82
    const-string v13, "____________________________"

    const-string v14, "\u6ca1\u6709\u76f8\u540c\u7248\u672c\u53f7"

    invoke-static {v13, v14}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;Ljava/lang/String;)V

    .line 83
    move-object/from16 v0, p0

    iget-object v13, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->zipfilesName:Ljava/util/ArrayList;

    invoke-virtual {v13, v8}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 84
    move-object/from16 v0, p0

    iget-object v13, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->zipfile:Ljava/util/ArrayList;

    invoke-virtual/range {p1 .. p1}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v14

    invoke-virtual {v13, v14}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 85
    move-object/from16 v0, p0

    iget-object v13, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->keyString:Ljava/util/HashMap;

    move-object/from16 v0, p0

    iget-object v14, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->pro:Ljava/util/Properties;

    const-string v15, "post-build"

    invoke-virtual {v14, v15}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v14

    move-object/from16 v0, p1

    invoke-virtual {v13, v14, v0}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_0
    .catchall {:try_start_2 .. :try_end_2} :catchall_2

    .line 108
    .end local v4    # "entryName":Ljava/lang/String;
    .end local v8    # "postbuild":Ljava/lang/String;
    :cond_3
    :goto_1
    if-eqz v11, :cond_4

    .line 109
    :try_start_3
    invoke-virtual {v11}, Ljava/util/zip/ZipInputStream;->close()V

    .line 110
    :cond_4
    if-eqz v7, :cond_5

    .line 111
    invoke-virtual {v7}, Ljava/io/InputStream;->close()V
    :try_end_3
    .catch Ljava/io/IOException; {:try_start_3 .. :try_end_3} :catch_2

    :cond_5
    move-object v6, v7

    .end local v7    # "inStream":Ljava/io/InputStream;
    .restart local v6    # "inStream":Ljava/io/InputStream;
    move-object v10, v11

    .line 114
    .end local v11    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v10    # "zin":Ljava/util/zip/ZipInputStream;
    goto/16 :goto_0

    .line 87
    .end local v6    # "inStream":Ljava/io/InputStream;
    .end local v10    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v4    # "entryName":Ljava/lang/String;
    .restart local v7    # "inStream":Ljava/io/InputStream;
    .restart local v8    # "postbuild":Ljava/lang/String;
    .restart local v11    # "zin":Ljava/util/zip/ZipInputStream;
    :cond_6
    :try_start_4
    move-object/from16 v0, p0

    iget-object v13, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->keyString:Ljava/util/HashMap;

    move-object/from16 v0, p0

    iget-object v14, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->pro:Ljava/util/Properties;

    const-string v15, "post-build"

    invoke-virtual {v14, v15}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v14

    invoke-virtual {v13, v14}, Ljava/util/HashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v5

    check-cast v5, Ljava/io/File;

    .line 88
    .local v5, "file":Ljava/io/File;
    const-string v13, "____________________________"

    const-string v14, "\u6709\u76f8\u540c\u7248\u672c\u53f7"

    invoke-static {v13, v14}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;Ljava/lang/String;)V

    .line 89
    move-object/from16 v0, p0

    move-object/from16 v1, p1

    invoke-direct {v0, v5, v1}, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->checkModifyTime(Ljava/io/File;Ljava/io/File;)Z

    move-result v13

    if-eqz v13, :cond_3

    .line 90
    const-string v13, "____________________________"

    const-string v14, "\u6bd4\u4e4b\u524d\u6587\u4ef6\u65b0"

    invoke-static {v13, v14}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;Ljava/lang/String;)V

    .line 91
    move-object/from16 v0, p0

    iget-object v13, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->zipfilesName:Ljava/util/ArrayList;

    move-object/from16 v0, p0

    iget-object v14, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->zipfile:Ljava/util/ArrayList;

    invoke-virtual {v14, v5}, Ljava/util/ArrayList;->indexOf(Ljava/lang/Object;)I

    move-result v14

    invoke-virtual {v13, v14}, Ljava/util/ArrayList;->remove(I)Ljava/lang/Object;

    .line 92
    move-object/from16 v0, p0

    iget-object v13, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->zipfile:Ljava/util/ArrayList;

    invoke-virtual {v13, v5}, Ljava/util/ArrayList;->remove(Ljava/lang/Object;)Z

    .line 93
    move-object/from16 v0, p0

    iget-object v13, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->zipfile:Ljava/util/ArrayList;

    invoke-virtual/range {p1 .. p1}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v14

    invoke-virtual {v13, v14}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 94
    move-object/from16 v0, p0

    iget-object v13, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->zipfilesName:Ljava/util/ArrayList;

    invoke-virtual {v13, v8}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z

    .line 95
    move-object/from16 v0, p0

    iget-object v13, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->keyString:Ljava/util/HashMap;

    move-object/from16 v0, p0

    iget-object v14, v0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->pro:Ljava/util/Properties;

    const-string v15, "post-build"

    invoke-virtual {v14, v15}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v14

    move-object/from16 v0, p1

    invoke-virtual {v13, v14, v0}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_4
    .catch Ljava/lang/Exception; {:try_start_4 .. :try_end_4} :catch_0
    .catchall {:try_start_4 .. :try_end_4} :catchall_2

    goto :goto_1

    .line 104
    .end local v4    # "entryName":Ljava/lang/String;
    .end local v5    # "file":Ljava/io/File;
    .end local v8    # "postbuild":Ljava/lang/String;
    :catch_0
    move-exception v2

    move-object v6, v7

    .end local v7    # "inStream":Ljava/io/InputStream;
    .restart local v6    # "inStream":Ljava/io/InputStream;
    move-object v10, v11

    .line 105
    .end local v11    # "zin":Ljava/util/zip/ZipInputStream;
    .local v2, "e":Ljava/lang/Exception;
    .restart local v10    # "zin":Ljava/util/zip/ZipInputStream;
    :goto_2
    :try_start_5
    invoke-static {v2}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V
    :try_end_5
    .catchall {:try_start_5 .. :try_end_5} :catchall_0

    .line 108
    if-eqz v10, :cond_7

    .line 109
    :try_start_6
    invoke-virtual {v10}, Ljava/util/zip/ZipInputStream;->close()V

    .line 110
    :cond_7
    if-eqz v6, :cond_0

    .line 111
    invoke-virtual {v6}, Ljava/io/InputStream;->close()V
    :try_end_6
    .catch Ljava/io/IOException; {:try_start_6 .. :try_end_6} :catch_1

    goto/16 :goto_0

    .line 112
    :catch_1
    move-exception v3

    .line 113
    .local v3, "e1":Ljava/io/IOException;
    invoke-static {v3}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto/16 :goto_0

    .line 112
    .end local v2    # "e":Ljava/lang/Exception;
    .end local v3    # "e1":Ljava/io/IOException;
    .end local v6    # "inStream":Ljava/io/InputStream;
    .end local v10    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v7    # "inStream":Ljava/io/InputStream;
    .restart local v11    # "zin":Ljava/util/zip/ZipInputStream;
    :catch_2
    move-exception v3

    .line 113
    .restart local v3    # "e1":Ljava/io/IOException;
    invoke-static {v3}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    move-object v6, v7

    .end local v7    # "inStream":Ljava/io/InputStream;
    .restart local v6    # "inStream":Ljava/io/InputStream;
    move-object v10, v11

    .line 115
    .end local v11    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v10    # "zin":Ljava/util/zip/ZipInputStream;
    goto/16 :goto_0

    .line 107
    .end local v3    # "e1":Ljava/io/IOException;
    :catchall_0
    move-exception v13

    .line 108
    :goto_3
    if-eqz v10, :cond_8

    .line 109
    :try_start_7
    invoke-virtual {v10}, Ljava/util/zip/ZipInputStream;->close()V

    .line 110
    :cond_8
    if-eqz v6, :cond_9

    .line 111
    invoke-virtual {v6}, Ljava/io/InputStream;->close()V
    :try_end_7
    .catch Ljava/io/IOException; {:try_start_7 .. :try_end_7} :catch_3

    .line 114
    :cond_9
    :goto_4
    throw v13

    .line 112
    :catch_3
    move-exception v3

    .line 113
    .restart local v3    # "e1":Ljava/io/IOException;
    invoke-static {v3}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_4

    .line 107
    .end local v3    # "e1":Ljava/io/IOException;
    .end local v6    # "inStream":Ljava/io/InputStream;
    .restart local v7    # "inStream":Ljava/io/InputStream;
    :catchall_1
    move-exception v13

    move-object v6, v7

    .end local v7    # "inStream":Ljava/io/InputStream;
    .restart local v6    # "inStream":Ljava/io/InputStream;
    goto :goto_3

    .end local v6    # "inStream":Ljava/io/InputStream;
    .end local v10    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v7    # "inStream":Ljava/io/InputStream;
    .restart local v11    # "zin":Ljava/util/zip/ZipInputStream;
    :catchall_2
    move-exception v13

    move-object v6, v7

    .end local v7    # "inStream":Ljava/io/InputStream;
    .restart local v6    # "inStream":Ljava/io/InputStream;
    move-object v10, v11

    .end local v11    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v10    # "zin":Ljava/util/zip/ZipInputStream;
    goto :goto_3

    .line 104
    :catch_4
    move-exception v2

    goto :goto_2

    .end local v6    # "inStream":Ljava/io/InputStream;
    .restart local v7    # "inStream":Ljava/io/InputStream;
    :catch_5
    move-exception v2

    move-object v6, v7

    .end local v7    # "inStream":Ljava/io/InputStream;
    .restart local v6    # "inStream":Ljava/io/InputStream;
    goto :goto_2
.end method

.method static synthetic access$000(Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;I)V
    .locals 0
    .param p0, "x0"    # Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;
    .param p1, "x1"    # I

    .prologue
    .line 23
    invoke-direct {p0, p1}, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->showMess(I)V

    return-void
.end method

.method private checkModifyTime(Ljava/io/File;Ljava/io/File;)Z
    .locals 6
    .param p1, "file"    # Ljava/io/File;
    .param p2, "zipFile2"    # Ljava/io/File;

    .prologue
    .line 125
    new-instance v0, Ljava/lang/Long;

    invoke-virtual {p1}, Ljava/io/File;->lastModified()J

    move-result-wide v2

    invoke-direct {v0, v2, v3}, Ljava/lang/Long;-><init>(J)V

    .line 126
    .local v0, "tempLong":Ljava/lang/Long;
    new-instance v1, Ljava/lang/Long;

    invoke-virtual {p2}, Ljava/io/File;->lastModified()J

    move-result-wide v2

    invoke-direct {v1, v2, v3}, Ljava/lang/Long;-><init>(J)V

    .line 127
    .local v1, "tempLong2":Ljava/lang/Long;
    invoke-virtual {v0}, Ljava/lang/Long;->longValue()J

    move-result-wide v2

    invoke-virtual {v1}, Ljava/lang/Long;->longValue()J

    move-result-wide v4

    cmp-long v2, v2, v4

    if-gez v2, :cond_0

    .line 128
    const/4 v2, 0x1

    .line 130
    :goto_0
    return v2

    :cond_0
    const/4 v2, 0x0

    goto :goto_0
.end method

.method private checkProjectVersion(Ljava/lang/String;)Z
    .locals 16
    .param p1, "postBuild"    # Ljava/lang/String;

    .prologue
    .line 260
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

    .line 261
    sget-object v10, Landroid/os/Build$VERSION;->INCREMENTAL:Ljava/lang/String;

    .line 262
    .local v10, "stbVersion":Ljava/lang/String;
    const-string v13, ":"

    move-object/from16 v0, p1

    invoke-virtual {v0, v13}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v11

    .line 264
    .local v11, "tmp":[Ljava/lang/String;
    const-string v12, ""

    .line 266
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

    .line 267
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

    .line 268
    invoke-virtual {v10}, Ljava/lang/String;->length()I

    move-result v9

    .line 269
    .local v9, "stbLen":I
    invoke-virtual {v12}, Ljava/lang/String;->length()I

    move-result v5

    .line 270
    .local v5, "len":I
    if-eq v5, v9, :cond_0

    .line 271
    const/4 v13, 0x0

    .line 289
    .end local v5    # "len":I
    .end local v9    # "stbLen":I
    :goto_0
    return v13

    .line 273
    .restart local v5    # "len":I
    .restart local v9    # "stbLen":I
    :cond_0
    const/4 v13, 0x0

    add-int/lit8 v14, v9, -0x6

    invoke-virtual {v10, v13, v14}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v6

    .line 274
    .local v6, "s1":Ljava/lang/String;
    add-int/lit8 v13, v9, -0x6

    add-int/lit8 v14, v9, -0x3

    invoke-virtual {v10, v13, v14}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v7

    .line 275
    .local v7, "s2":Ljava/lang/String;
    add-int/lit8 v13, v9, -0x3

    invoke-virtual {v10, v13}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v8

    .line 277
    .local v8, "s3":Ljava/lang/String;
    const/4 v13, 0x0

    add-int/lit8 v14, v5, -0x6

    invoke-virtual {v12, v13, v14}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v2

    .line 278
    .local v2, "f1":Ljava/lang/String;
    add-int/lit8 v13, v5, -0x6

    add-int/lit8 v14, v5, -0x3

    invoke-virtual {v12, v13, v14}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v3

    .line 279
    .local v3, "f2":Ljava/lang/String;
    add-int/lit8 v13, v5, -0x3

    invoke-virtual {v12, v13}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v4

    .line 280
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

    .line 281
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

    .line 282
    invoke-virtual {v6, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v13

    if-eqz v13, :cond_1

    invoke-virtual {v8, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    move-result v13

    if-nez v13, :cond_1

    .line 283
    const/4 v13, 0x1

    goto/16 :goto_0

    .line 285
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

    .line 286
    .local v1, "e":Ljava/lang/Exception;
    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    .line 287
    const/4 v13, 0x0

    goto/16 :goto_0

    .line 289
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

.method private getVersionInfo(Ljava/io/File;)Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    .locals 12
    .param p1, "zipFile"    # Ljava/io/File;

    .prologue
    .line 170
    if-eqz p1, :cond_0

    invoke-virtual {p1}, Ljava/io/File;->exists()Z

    move-result v11

    if-nez v11, :cond_2

    .line 171
    :cond_0
    const/4 v6, 0x0

    .line 204
    :cond_1
    :goto_0
    return-object v6

    .line 172
    :cond_2
    const/4 v6, 0x0

    .line 173
    .local v6, "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    const/4 v8, 0x0

    .line 174
    .local v8, "zin":Ljava/util/zip/ZipInputStream;
    const/4 v3, 0x0

    .line 175
    .local v3, "inStream":Ljava/io/InputStream;
    const/4 v10, 0x0

    .line 177
    .local v10, "zipEntry":Ljava/util/zip/ZipEntry;
    :try_start_0
    new-instance v4, Ljava/io/FileInputStream;

    invoke-direct {v4, p1}, Ljava/io/FileInputStream;-><init>(Ljava/io/File;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_1
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 178
    .end local v3    # "inStream":Ljava/io/InputStream;
    .local v4, "inStream":Ljava/io/InputStream;
    :try_start_1
    new-instance v9, Ljava/util/zip/ZipInputStream;

    invoke-direct {v9, v4}, Ljava/util/zip/ZipInputStream;-><init>(Ljava/io/InputStream;)V
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_4
    .catchall {:try_start_1 .. :try_end_1} :catchall_1

    .line 179
    .end local v8    # "zin":Ljava/util/zip/ZipInputStream;
    .local v9, "zin":Ljava/util/zip/ZipInputStream;
    :cond_3
    :try_start_2
    invoke-virtual {v9}, Ljava/util/zip/ZipInputStream;->getNextEntry()Ljava/util/zip/ZipEntry;

    move-result-object v10

    if-eqz v10, :cond_4

    .line 180
    invoke-virtual {v10}, Ljava/util/zip/ZipEntry;->getName()Ljava/lang/String;

    move-result-object v2

    .line 181
    .local v2, "entryName":Ljava/lang/String;
    const-string v11, "META-INF/com/android/metadata"

    invoke-virtual {v2, v11}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v11

    if-eqz v11, :cond_3

    .line 182
    new-instance v5, Ljava/util/Properties;

    invoke-direct {v5}, Ljava/util/Properties;-><init>()V

    .line 183
    .local v5, "pro":Ljava/util/Properties;
    invoke-virtual {v5, v9}, Ljava/util/Properties;->load(Ljava/io/InputStream;)V

    .line 184
    new-instance v7, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;

    invoke-direct {v7}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;-><init>()V
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_5
    .catchall {:try_start_2 .. :try_end_2} :catchall_2

    .line 185
    .end local v6    # "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    .local v7, "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    :try_start_3
    const-string v11, "post-build"

    invoke-virtual {v5, v11}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v11

    invoke-virtual {v7, v11}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->setPostBuild(Ljava/lang/String;)V

    .line 186
    const-string v11, "pre-build"

    invoke-virtual {v5, v11}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v11

    invoke-virtual {v7, v11}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->setPreBuild(Ljava/lang/String;)V

    .line 187
    const-string v11, "pre-device"

    invoke-virtual {v5, v11}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v11

    invoke-virtual {v7, v11}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->setPreDevice(Ljava/lang/String;)V

    .line 188
    const-string v11, "pre-timestamp"

    invoke-virtual {v5, v11}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v11

    invoke-virtual {v7, v11}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->setPostTimestamp(Ljava/lang/String;)V
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_6
    .catchall {:try_start_3 .. :try_end_3} :catchall_3

    move-object v6, v7

    .line 196
    .end local v2    # "entryName":Ljava/lang/String;
    .end local v5    # "pro":Ljava/util/Properties;
    .end local v7    # "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    .restart local v6    # "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    :cond_4
    if-eqz v9, :cond_5

    .line 197
    :try_start_4
    invoke-virtual {v9}, Ljava/util/zip/ZipInputStream;->close()V

    .line 198
    :cond_5
    if-eqz v4, :cond_6

    .line 199
    invoke-virtual {v4}, Ljava/io/InputStream;->close()V
    :try_end_4
    .catch Ljava/io/IOException; {:try_start_4 .. :try_end_4} :catch_0

    :cond_6
    move-object v3, v4

    .end local v4    # "inStream":Ljava/io/InputStream;
    .restart local v3    # "inStream":Ljava/io/InputStream;
    move-object v8, v9

    .line 202
    .end local v9    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v8    # "zin":Ljava/util/zip/ZipInputStream;
    goto :goto_0

    .line 200
    .end local v3    # "inStream":Ljava/io/InputStream;
    .end local v8    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v4    # "inStream":Ljava/io/InputStream;
    .restart local v9    # "zin":Ljava/util/zip/ZipInputStream;
    :catch_0
    move-exception v1

    .line 201
    .local v1, "e1":Ljava/io/IOException;
    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    move-object v3, v4

    .end local v4    # "inStream":Ljava/io/InputStream;
    .restart local v3    # "inStream":Ljava/io/InputStream;
    move-object v8, v9

    .line 203
    .end local v9    # "zin":Ljava/util/zip/ZipInputStream;
    .restart local v8    # "zin":Ljava/util/zip/ZipInputStream;
    goto :goto_0

    .line 192
    .end local v1    # "e1":Ljava/io/IOException;
    :catch_1
    move-exception v0

    .line 193
    .local v0, "e":Ljava/lang/Exception;
    :goto_1
    :try_start_5
    invoke-static {v0}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V
    :try_end_5
    .catchall {:try_start_5 .. :try_end_5} :catchall_0

    .line 196
    if-eqz v8, :cond_7

    .line 197
    :try_start_6
    invoke-virtual {v8}, Ljava/util/zip/ZipInputStream;->close()V

    .line 198
    :cond_7
    if-eqz v3, :cond_1

    .line 199
    invoke-virtual {v3}, Ljava/io/InputStream;->close()V
    :try_end_6
    .catch Ljava/io/IOException; {:try_start_6 .. :try_end_6} :catch_2

    goto :goto_0

    .line 200
    :catch_2
    move-exception v1

    .line 201
    .restart local v1    # "e1":Ljava/io/IOException;
    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_0

    .line 195
    .end local v0    # "e":Ljava/lang/Exception;
    .end local v1    # "e1":Ljava/io/IOException;
    :catchall_0
    move-exception v11

    .line 196
    :goto_2
    if-eqz v8, :cond_8

    .line 197
    :try_start_7
    invoke-virtual {v8}, Ljava/util/zip/ZipInputStream;->close()V

    .line 198
    :cond_8
    if-eqz v3, :cond_9

    .line 199
    invoke-virtual {v3}, Ljava/io/InputStream;->close()V
    :try_end_7
    .catch Ljava/io/IOException; {:try_start_7 .. :try_end_7} :catch_3

    .line 202
    :cond_9
    :goto_3
    throw v11

    .line 200
    :catch_3
    move-exception v1

    .line 201
    .restart local v1    # "e1":Ljava/io/IOException;
    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_3

    .line 195
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

    .line 192
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

.method private showMess(I)V
    .locals 1
    .param p1, "id"    # I

    .prologue
    .line 318
    const/4 v0, 0x0

    invoke-static {p0, p1, v0}, Landroid/widget/Toast;->makeText(Landroid/content/Context;II)Landroid/widget/Toast;

    move-result-object v0

    invoke-virtual {v0}, Landroid/widget/Toast;->show()V

    .line 319
    return-void
.end method


# virtual methods
.method public checkVersion(Lnet/sunniwell/service/swupgrade/usb/VersionInfo;)Z
    .locals 7
    .param p1, "vInfo"    # Lnet/sunniwell/service/swupgrade/usb/VersionInfo;

    .prologue
    const/4 v6, 0x4

    const/4 v5, 0x5

    const/4 v2, 0x0

    .line 215
    sget-object v1, Landroid/os/Build;->FINGERPRINT:Ljava/lang/String;

    .line 216
    .local v1, "stbPreDevice":Ljava/lang/String;
    sget-object v0, Landroid/os/Build;->DEVICE:Ljava/lang/String;

    .line 217
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

    .line 218
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

    .line 220
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPreDevice()Ljava/lang/String;

    move-result-object v3

    if-nez v3, :cond_0

    .line 221
    iget-object v3, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->myHandler:Landroid/os/Handler;

    invoke-virtual {v3, v6}, Landroid/os/Handler;->sendEmptyMessage(I)Z

    .line 247
    :goto_0
    return v2

    .line 224
    :cond_0
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPreDevice()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_1

    .line 225
    iget-object v3, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->myHandler:Landroid/os/Handler;

    invoke-virtual {v3, v6}, Landroid/os/Handler;->sendEmptyMessage(I)Z

    goto :goto_0

    .line 229
    :cond_1
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPostBuild()Ljava/lang/String;

    move-result-object v3

    if-nez v3, :cond_2

    .line 230
    iget-object v3, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->myHandler:Landroid/os/Handler;

    invoke-virtual {v3, v5}, Landroid/os/Handler;->sendEmptyMessage(I)Z

    goto :goto_0

    .line 233
    :cond_2
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPostBuild()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v3, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-eqz v3, :cond_3

    .line 235
    iget-object v3, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->myHandler:Landroid/os/Handler;

    const/4 v4, 0x6

    invoke-virtual {v3, v4}, Landroid/os/Handler;->sendEmptyMessage(I)Z

    goto :goto_0

    .line 238
    :cond_3
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPostBuild()Ljava/lang/String;

    move-result-object v3

    invoke-direct {p0, v3}, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->checkProjectVersion(Ljava/lang/String;)Z

    move-result v3

    if-nez v3, :cond_4

    .line 239
    iget-object v3, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->myHandler:Landroid/os/Handler;

    invoke-virtual {v3, v5}, Landroid/os/Handler;->sendEmptyMessage(I)Z

    goto :goto_0

    .line 242
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

    .line 244
    iget-object v3, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->myHandler:Landroid/os/Handler;

    invoke-virtual {v3, v5}, Landroid/os/Handler;->sendEmptyMessage(I)Z

    goto :goto_0

    .line 247
    :cond_5
    const/4 v2, 0x1

    goto :goto_0
.end method

.method public onBind(Landroid/content/Intent;)Landroid/os/IBinder;
    .locals 1
    .param p1, "intent"    # Landroid/content/Intent;

    .prologue
    .line 324
    const/4 v0, 0x0

    return-object v0
.end method

.method public onCreate()V
    .locals 0

    .prologue
    .line 35
    invoke-super {p0}, Landroid/app/Service;->onCreate()V

    .line 37
    return-void
.end method

.method public onStart(Landroid/content/Intent;I)V
    .locals 4
    .param p1, "intent"    # Landroid/content/Intent;
    .param p2, "startId"    # I

    .prologue
    .line 42
    invoke-super {p0, p1, p2}, Landroid/app/Service;->onStart(Landroid/content/Intent;I)V

    .line 44
    iput-object p0, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->mContext:Landroid/content/Context;

    .line 45
    const-string v2, "zipfile"

    invoke-virtual {p1, v2}, Landroid/content/Intent;->getStringArrayListExtra(Ljava/lang/String;)Ljava/util/ArrayList;

    move-result-object v0

    .line 46
    .local v0, "files":Ljava/util/ArrayList;, "Ljava/util/ArrayList<Ljava/lang/String;>;"
    new-instance v2, Ljava/util/ArrayList;

    invoke-direct {v2}, Ljava/util/ArrayList;-><init>()V

    iput-object v2, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->zipfilesName:Ljava/util/ArrayList;

    .line 47
    if-eqz v0, :cond_0

    const-string v2, ""

    invoke-virtual {v0, v2}, Ljava/util/ArrayList;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_1

    .line 56
    :cond_0
    return-void

    .line 51
    :cond_1
    const/4 v1, 0x0

    .local v1, "i":I
    :goto_0
    invoke-virtual {v0}, Ljava/util/ArrayList;->size()I

    move-result v2

    if-ge v1, v2, :cond_0

    .line 52
    new-instance v3, Ljava/io/File;

    invoke-virtual {v0, v1}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/lang/String;

    invoke-direct {v3, v2}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    iput-object v3, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->zipFilePath:Ljava/io/File;

    .line 53
    iget-object v2, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->zipFilePath:Ljava/io/File;

    invoke-direct {p0, v2}, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->ZipFilelist(Ljava/io/File;)V

    .line 51
    add-int/lit8 v1, v1, 0x1

    goto :goto_0
.end method

.method public start()V
    .locals 3

    .prologue
    .line 142
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->zipFile:Ljava/io/File;

    invoke-direct {p0, v1}, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->getVersionInfo(Ljava/io/File;)Lnet/sunniwell/service/swupgrade/usb/VersionInfo;

    move-result-object v0

    .line 143
    .local v0, "vInfo":Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
    if-nez v0, :cond_1

    .line 145
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->myHandler:Landroid/os/Handler;

    const/4 v2, 0x2

    invoke-virtual {v1, v2}, Landroid/os/Handler;->sendEmptyMessage(I)Z

    .line 152
    :cond_0
    :goto_0
    return-void

    .line 149
    :cond_1
    invoke-virtual {p0, v0}, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->checkVersion(Lnet/sunniwell/service/swupgrade/usb/VersionInfo;)Z

    move-result v1

    if-eqz v1, :cond_0

    .line 150
    invoke-virtual {p0}, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->startActivity()V

    goto :goto_0
.end method

.method public startActivity()V
    .locals 3

    .prologue
    .line 156
    const-string v1, "==\u542f\u52a8activity=startActivity()==="

    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 157
    new-instance v0, Landroid/content/Intent;

    const-class v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;

    invoke-direct {v0, p0, v1}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V

    .line 158
    .local v0, "mIntent":Landroid/content/Intent;
    const/high16 v1, 0x10000000

    invoke-virtual {v0, v1}, Landroid/content/Intent;->setFlags(I)Landroid/content/Intent;

    .line 159
    const-string v1, "zipfile"

    iget-object v2, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->zipFile:Ljava/io/File;

    invoke-virtual {v2}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v0, v1, v2}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    .line 160
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->mContext:Landroid/content/Context;

    invoke-virtual {v1, v0}, Landroid/content/Context;->startActivity(Landroid/content/Intent;)V

    .line 161
    return-void
.end method
