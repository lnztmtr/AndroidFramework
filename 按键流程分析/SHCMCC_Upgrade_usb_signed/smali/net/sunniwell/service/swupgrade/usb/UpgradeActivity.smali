.class public Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;
.super Landroid/app/Activity;
.source "UpgradeActivity.java"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;
    }
.end annotation


# static fields
.field public static mInstance:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;


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

.field private mUpgradeFileStatus:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

.field mVersionLists:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List",
            "<",
            "Ljava/util/Map",
            "<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;>;"
        }
    .end annotation
.end field


# direct methods
.method static constructor <clinit>()V
    .locals 1

    .prologue
    .line 51
    const/4 v0, 0x0

    sput-object v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mInstance:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;

    return-void
.end method

.method public constructor <init>()V
    .locals 1

    .prologue
    .line 49
    invoke-direct {p0}, Landroid/app/Activity;-><init>()V

    .line 50
    const/4 v0, 0x0

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mVersionLists:Ljava/util/List;

    .line 53
    sget-object v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->UNKNOWN:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mUpgradeFileStatus:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    .line 304
    new-instance v0, Ljava/util/HashMap;

    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->keyString:Ljava/util/HashMap;

    return-void
.end method

.method private checkModifyTime(Ljava/io/File;Ljava/io/File;)Z
    .locals 6
    .param p1, "file"    # Ljava/io/File;
    .param p2, "zipFile2"    # Ljava/io/File;

    .prologue
    .line 296
    new-instance v0, Ljava/lang/Long;

    invoke-virtual {p1}, Ljava/io/File;->lastModified()J

    move-result-wide v2

    invoke-direct {v0, v2, v3}, Ljava/lang/Long;-><init>(J)V

    .line 297
    .local v0, "tempLong":Ljava/lang/Long;
    new-instance v1, Ljava/lang/Long;

    invoke-virtual {p2}, Ljava/io/File;->lastModified()J

    move-result-wide v2

    invoke-direct {v1, v2, v3}, Ljava/lang/Long;-><init>(J)V

    .line 298
    .local v1, "tempLong2":Ljava/lang/Long;
    invoke-virtual {v0}, Ljava/lang/Long;->longValue()J

    move-result-wide v2

    invoke-virtual {v1}, Ljava/lang/Long;->longValue()J

    move-result-wide v4

    cmp-long v2, v2, v4

    if-gez v2, :cond_0

    .line 299
    const/4 v2, 0x1

    .line 301
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
    .line 365
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

    .line 366
    sget-object v10, Landroid/os/Build$VERSION;->INCREMENTAL:Ljava/lang/String;

    .line 367
    .local v10, "stbVersion":Ljava/lang/String;
    const-string v13, ":"

    move-object/from16 v0, p1

    invoke-virtual {v0, v13}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v11

    .line 369
    .local v11, "tmp":[Ljava/lang/String;
    const-string v12, ""

    .line 371
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

    .line 372
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

    .line 373
    invoke-virtual {v10}, Ljava/lang/String;->length()I

    move-result v9

    .line 374
    .local v9, "stbLen":I
    invoke-virtual {v12}, Ljava/lang/String;->length()I

    move-result v5

    .line 375
    .local v5, "len":I
    if-eq v5, v9, :cond_0

    .line 376
    const/4 v13, 0x0

    .line 394
    .end local v5    # "len":I
    .end local v9    # "stbLen":I
    :goto_0
    return v13

    .line 378
    .restart local v5    # "len":I
    .restart local v9    # "stbLen":I
    :cond_0
    const/4 v13, 0x0

    add-int/lit8 v14, v9, -0x6

    invoke-virtual {v10, v13, v14}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v6

    .line 379
    .local v6, "s1":Ljava/lang/String;
    add-int/lit8 v13, v9, -0x6

    add-int/lit8 v14, v9, -0x3

    invoke-virtual {v10, v13, v14}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v7

    .line 380
    .local v7, "s2":Ljava/lang/String;
    add-int/lit8 v13, v9, -0x3

    invoke-virtual {v10, v13}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v8

    .line 382
    .local v8, "s3":Ljava/lang/String;
    const/4 v13, 0x0

    add-int/lit8 v14, v5, -0x6

    invoke-virtual {v12, v13, v14}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v2

    .line 383
    .local v2, "f1":Ljava/lang/String;
    add-int/lit8 v13, v5, -0x6

    add-int/lit8 v14, v5, -0x3

    invoke-virtual {v12, v13, v14}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v3

    .line 384
    .local v3, "f2":Ljava/lang/String;
    add-int/lit8 v13, v5, -0x3

    invoke-virtual {v12, v13}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v4

    .line 385
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

    .line 386
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

    .line 387
    invoke-virtual {v6, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v13

    if-eqz v13, :cond_1

    invoke-virtual {v8, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    move-result v13

    if-nez v13, :cond_1

    .line 388
    const/4 v13, 0x1

    goto/16 :goto_0

    .line 390
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

    .line 391
    .local v1, "e":Ljava/lang/Exception;
    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    .line 392
    const/4 v13, 0x0

    goto/16 :goto_0

    .line 394
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

.method private filterUnsuitableZipFile(Ljava/util/List;)Ljava/util/Map;
    .locals 24
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/List",
            "<",
            "Ljava/lang/String;",
            ">;)",
            "Ljava/util/Map",
            "<",
            "Ljava/lang/String;",
            "Ljava/io/File;",
            ">;"
        }
    .end annotation

    .prologue
    .line 173
    .local p1, "zipFileLists":Ljava/util/List;, "Ljava/util/List<Ljava/lang/String;>;"
    if-eqz p1, :cond_0

    invoke-interface/range {p1 .. p1}, Ljava/util/List;->size()I

    move-result v21

    if-nez v21, :cond_2

    .line 174
    :cond_0
    const/4 v7, 0x0

    .line 286
    :cond_1
    return-object v7

    .line 176
    :cond_2
    sget-object v3, Landroid/os/Build$VERSION;->INCREMENTAL:Ljava/lang/String;

    .line 177
    .local v3, "currentVersion":Ljava/lang/String;
    sget-object v13, Landroid/os/Build;->FINGERPRINT:Ljava/lang/String;

    .line 178
    .local v13, "stbPreDevice":Ljava/lang/String;
    const-string v21, ":"

    move-object/from16 v0, v21

    invoke-virtual {v13, v0}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v21

    const/16 v22, 0x0

    aget-object v2, v21, v22

    .line 179
    .local v2, "currentDevice":Ljava/lang/String;
    new-instance v21, Ljava/lang/StringBuilder;

    invoke-direct/range {v21 .. v21}, Ljava/lang/StringBuilder;-><init>()V

    const-string v22, "currentDevice="

    invoke-virtual/range {v21 .. v22}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v21

    move-object/from16 v0, v21

    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v21

    const-string v22, ",currentVersion="

    invoke-virtual/range {v21 .. v22}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v21

    move-object/from16 v0, v21

    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v21

    invoke-virtual/range {v21 .. v21}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v21

    invoke-static/range {v21 .. v21}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 180
    new-instance v7, Ljava/util/HashMap;

    invoke-direct {v7}, Ljava/util/HashMap;-><init>()V

    .line 181
    .local v7, "fileLists":Ljava/util/Map;, "Ljava/util/Map<Ljava/lang/String;Ljava/io/File;>;"
    invoke-interface/range {p1 .. p1}, Ljava/util/List;->iterator()Ljava/util/Iterator;

    move-result-object v21

    :cond_3
    :goto_0
    invoke-interface/range {v21 .. v21}, Ljava/util/Iterator;->hasNext()Z

    move-result v22

    if-eqz v22, :cond_1

    invoke-interface/range {v21 .. v21}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v8

    check-cast v8, Ljava/lang/String;

    .line 182
    .local v8, "fileString":Ljava/lang/String;
    new-instance v6, Ljava/io/File;

    invoke-direct {v6, v8}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    .line 183
    .local v6, "file":Ljava/io/File;
    invoke-virtual {v6}, Ljava/io/File;->exists()Z

    move-result v22

    if-eqz v22, :cond_3

    .line 185
    const/16 v19, 0x0

    .line 186
    .local v19, "zin":Ljava/util/zip/ZipInputStream;
    const/4 v10, 0x0

    .line 187
    .local v10, "inStream":Ljava/io/InputStream;
    const/16 v17, 0x0

    .line 189
    .local v17, "zf":Ljava/util/zip/ZipFile;
    :try_start_0
    new-instance v18, Ljava/util/zip/ZipFile;

    move-object/from16 v0, v18

    invoke-direct {v0, v6}, Ljava/util/zip/ZipFile;-><init>(Ljava/io/File;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_8
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 190
    .end local v17    # "zf":Ljava/util/zip/ZipFile;
    .local v18, "zf":Ljava/util/zip/ZipFile;
    :try_start_1
    const-string v22, "META-INF/com/android/metadata"

    move-object/from16 v0, v18

    move-object/from16 v1, v22

    invoke-virtual {v0, v1}, Ljava/util/zip/ZipFile;->getEntry(Ljava/lang/String;)Ljava/util/zip/ZipEntry;

    move-result-object v20

    .line 192
    .local v20, "zipEntry":Ljava/util/zip/ZipEntry;
    if-nez v20, :cond_6

    .line 193
    sget-object v22, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->NOTFOUND_METADATA:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    move-object/from16 v0, v22

    move-object/from16 v1, p0

    iput-object v0, v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mUpgradeFileStatus:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_b
    .catchall {:try_start_1 .. :try_end_1} :catchall_1

    .line 275
    if-eqz v19, :cond_4

    .line 276
    :try_start_2
    invoke-virtual/range {v19 .. v19}, Ljava/util/zip/ZipInputStream;->close()V

    .line 277
    :cond_4
    if-eqz v10, :cond_5

    .line 278
    invoke-virtual {v10}, Ljava/io/InputStream;->close()V

    .line 279
    :cond_5
    if-eqz v18, :cond_3

    .line 280
    invoke-virtual/range {v18 .. v18}, Ljava/util/zip/ZipFile;->close()V
    :try_end_2
    .catch Ljava/io/IOException; {:try_start_2 .. :try_end_2} :catch_0

    goto :goto_0

    .line 281
    :catch_0
    move-exception v5

    .line 282
    .local v5, "e1":Ljava/io/IOException;
    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_0

    .line 197
    .end local v5    # "e1":Ljava/io/IOException;
    :cond_6
    :try_start_3
    move-object/from16 v0, v18

    move-object/from16 v1, v20

    invoke-virtual {v0, v1}, Ljava/util/zip/ZipFile;->getInputStream(Ljava/util/zip/ZipEntry;)Ljava/io/InputStream;

    move-result-object v9

    .line 198
    .local v9, "in":Ljava/io/InputStream;
    new-instance v12, Ljava/util/Properties;

    invoke-direct {v12}, Ljava/util/Properties;-><init>()V

    .line 199
    .local v12, "pro":Ljava/util/Properties;
    invoke-virtual {v12, v9}, Ljava/util/Properties;->load(Ljava/io/InputStream;)V

    .line 200
    const-string v22, "post-build"

    move-object/from16 v0, v22

    invoke-virtual {v12, v0}, Ljava/util/Properties;->getProperty(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v11

    .line 201
    .local v11, "postBuild":Ljava/lang/String;
    new-instance v22, Ljava/lang/StringBuilder;

    invoke-direct/range {v22 .. v22}, Ljava/lang/StringBuilder;-><init>()V

    const-string v23, "postBuild = "

    invoke-virtual/range {v22 .. v23}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v22

    move-object/from16 v0, v22

    invoke-virtual {v0, v11}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v22

    invoke-virtual/range {v22 .. v22}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v22

    invoke-static/range {v22 .. v22}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 202
    if-nez v11, :cond_9

    .line 203
    const-string v22, "continue:=======\u672a\u53d1\u73b0Post-build\u5c5e\u6027========"

    invoke-static/range {v22 .. v22}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 204
    sget-object v22, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->NOTFOUND_POSTBUILD:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    move-object/from16 v0, v22

    move-object/from16 v1, p0

    iput-object v0, v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mUpgradeFileStatus:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_b
    .catchall {:try_start_3 .. :try_end_3} :catchall_1

    .line 275
    if-eqz v19, :cond_7

    .line 276
    :try_start_4
    invoke-virtual/range {v19 .. v19}, Ljava/util/zip/ZipInputStream;->close()V

    .line 277
    :cond_7
    if-eqz v10, :cond_8

    .line 278
    invoke-virtual {v10}, Ljava/io/InputStream;->close()V

    .line 279
    :cond_8
    if-eqz v18, :cond_3

    .line 280
    invoke-virtual/range {v18 .. v18}, Ljava/util/zip/ZipFile;->close()V
    :try_end_4
    .catch Ljava/io/IOException; {:try_start_4 .. :try_end_4} :catch_1

    goto/16 :goto_0

    .line 281
    :catch_1
    move-exception v5

    .line 282
    .restart local v5    # "e1":Ljava/io/IOException;
    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto/16 :goto_0

    .line 207
    .end local v5    # "e1":Ljava/io/IOException;
    :cond_9
    :try_start_5
    const-string v22, ":"

    move-object/from16 v0, v22

    invoke-virtual {v11, v0}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v14

    .line 208
    .local v14, "tmp":[Ljava/lang/String;
    new-instance v22, Ljava/lang/StringBuilder;

    invoke-direct/range {v22 .. v22}, Ljava/lang/StringBuilder;-><init>()V

    const-string v23, "tmp.length = "

    invoke-virtual/range {v22 .. v23}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v22

    array-length v0, v14

    move/from16 v23, v0

    invoke-virtual/range {v22 .. v23}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v22

    invoke-virtual/range {v22 .. v22}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v22

    invoke-static/range {v22 .. v22}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 209
    array-length v0, v14

    move/from16 v22, v0

    const/16 v23, 0x3

    move/from16 v0, v22

    move/from16 v1, v23

    if-eq v0, v1, :cond_c

    .line 210
    const-string v22, "continue:=======\u7c7b\u578b\u683c\u5f0f\u4e0d\u6b63\u786e========"

    invoke-static/range {v22 .. v22}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 211
    sget-object v22, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->INVALID_POSTBUILD:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    move-object/from16 v0, v22

    move-object/from16 v1, p0

    iput-object v0, v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mUpgradeFileStatus:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;
    :try_end_5
    .catch Ljava/lang/Exception; {:try_start_5 .. :try_end_5} :catch_b
    .catchall {:try_start_5 .. :try_end_5} :catchall_1

    .line 275
    if-eqz v19, :cond_a

    .line 276
    :try_start_6
    invoke-virtual/range {v19 .. v19}, Ljava/util/zip/ZipInputStream;->close()V

    .line 277
    :cond_a
    if-eqz v10, :cond_b

    .line 278
    invoke-virtual {v10}, Ljava/io/InputStream;->close()V

    .line 279
    :cond_b
    if-eqz v18, :cond_3

    .line 280
    invoke-virtual/range {v18 .. v18}, Ljava/util/zip/ZipFile;->close()V
    :try_end_6
    .catch Ljava/io/IOException; {:try_start_6 .. :try_end_6} :catch_2

    goto/16 :goto_0

    .line 281
    :catch_2
    move-exception v5

    .line 282
    .restart local v5    # "e1":Ljava/io/IOException;
    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto/16 :goto_0

    .line 214
    .end local v5    # "e1":Ljava/io/IOException;
    :cond_c
    const/16 v22, 0x0

    :try_start_7
    aget-object v22, v14, v22

    move-object/from16 v0, v22

    invoke-virtual {v0, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v22

    if-nez v22, :cond_f

    .line 215
    const-string v22, "continue:=======\u786c\u4ef6\u7c7b\u578b\u4e0d\u4e00\u81f4========"

    invoke-static/range {v22 .. v22}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 216
    sget-object v22, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->MISMATCH_HARDWARE:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    move-object/from16 v0, v22

    move-object/from16 v1, p0

    iput-object v0, v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mUpgradeFileStatus:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;
    :try_end_7
    .catch Ljava/lang/Exception; {:try_start_7 .. :try_end_7} :catch_b
    .catchall {:try_start_7 .. :try_end_7} :catchall_1

    .line 275
    if-eqz v19, :cond_d

    .line 276
    :try_start_8
    invoke-virtual/range {v19 .. v19}, Ljava/util/zip/ZipInputStream;->close()V

    .line 277
    :cond_d
    if-eqz v10, :cond_e

    .line 278
    invoke-virtual {v10}, Ljava/io/InputStream;->close()V

    .line 279
    :cond_e
    if-eqz v18, :cond_3

    .line 280
    invoke-virtual/range {v18 .. v18}, Ljava/util/zip/ZipFile;->close()V
    :try_end_8
    .catch Ljava/io/IOException; {:try_start_8 .. :try_end_8} :catch_3

    goto/16 :goto_0

    .line 281
    :catch_3
    move-exception v5

    .line 282
    .restart local v5    # "e1":Ljava/io/IOException;
    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto/16 :goto_0

    .line 219
    .end local v5    # "e1":Ljava/io/IOException;
    :cond_f
    const/16 v22, 0x1

    :try_start_9
    aget-object v22, v14, v22

    const-string v23, "/"

    invoke-virtual/range {v22 .. v23}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v15

    .line 220
    .local v15, "tmp1":[Ljava/lang/String;
    new-instance v22, Ljava/lang/StringBuilder;

    invoke-direct/range {v22 .. v22}, Ljava/lang/StringBuilder;-><init>()V

    const-string v23, "tmp1.length="

    invoke-virtual/range {v22 .. v23}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v22

    array-length v0, v15

    move/from16 v23, v0

    invoke-virtual/range {v22 .. v23}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v22

    invoke-virtual/range {v22 .. v22}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v22

    invoke-static/range {v22 .. v22}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 221
    array-length v0, v15

    move/from16 v22, v0

    const/16 v23, 0x3

    move/from16 v0, v22

    move/from16 v1, v23

    if-eq v0, v1, :cond_12

    .line 222
    const-string v22, "continue:=======\u7248\u672c\u53f7\u683c\u5f0f\u4e0d\u7b26\u5408========"

    invoke-static/range {v22 .. v22}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 223
    sget-object v22, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->INVALID_VERSION_FORMAT:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    move-object/from16 v0, v22

    move-object/from16 v1, p0

    iput-object v0, v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mUpgradeFileStatus:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;
    :try_end_9
    .catch Ljava/lang/Exception; {:try_start_9 .. :try_end_9} :catch_b
    .catchall {:try_start_9 .. :try_end_9} :catchall_1

    .line 275
    if-eqz v19, :cond_10

    .line 276
    :try_start_a
    invoke-virtual/range {v19 .. v19}, Ljava/util/zip/ZipInputStream;->close()V

    .line 277
    :cond_10
    if-eqz v10, :cond_11

    .line 278
    invoke-virtual {v10}, Ljava/io/InputStream;->close()V

    .line 279
    :cond_11
    if-eqz v18, :cond_3

    .line 280
    invoke-virtual/range {v18 .. v18}, Ljava/util/zip/ZipFile;->close()V
    :try_end_a
    .catch Ljava/io/IOException; {:try_start_a .. :try_end_a} :catch_4

    goto/16 :goto_0

    .line 281
    :catch_4
    move-exception v5

    .line 282
    .restart local v5    # "e1":Ljava/io/IOException;
    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto/16 :goto_0

    .line 226
    .end local v5    # "e1":Ljava/io/IOException;
    :cond_12
    const/16 v22, 0x2

    :try_start_b
    aget-object v16, v15, v22

    .line 227
    .local v16, "version":Ljava/lang/String;
    new-instance v22, Ljava/lang/StringBuilder;

    invoke-direct/range {v22 .. v22}, Ljava/lang/StringBuilder;-><init>()V

    const-string v23, "version="

    invoke-virtual/range {v22 .. v23}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v22

    move-object/from16 v0, v22

    move-object/from16 v1, v16

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v22

    invoke-virtual/range {v22 .. v22}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v22

    invoke-static/range {v22 .. v22}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 228
    const-string v22, ""

    move-object/from16 v0, v16

    move-object/from16 v1, v22

    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v22

    if-eqz v22, :cond_15

    .line 229
    const-string v22, "continue:=======\u7248\u672c\u53f7\u4e3a\u7a7a========"

    invoke-static/range {v22 .. v22}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 230
    sget-object v22, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->EMPTY_VERSION:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    move-object/from16 v0, v22

    move-object/from16 v1, p0

    iput-object v0, v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mUpgradeFileStatus:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;
    :try_end_b
    .catch Ljava/lang/Exception; {:try_start_b .. :try_end_b} :catch_b
    .catchall {:try_start_b .. :try_end_b} :catchall_1

    .line 275
    if-eqz v19, :cond_13

    .line 276
    :try_start_c
    invoke-virtual/range {v19 .. v19}, Ljava/util/zip/ZipInputStream;->close()V

    .line 277
    :cond_13
    if-eqz v10, :cond_14

    .line 278
    invoke-virtual {v10}, Ljava/io/InputStream;->close()V

    .line 279
    :cond_14
    if-eqz v18, :cond_3

    .line 280
    invoke-virtual/range {v18 .. v18}, Ljava/util/zip/ZipFile;->close()V
    :try_end_c
    .catch Ljava/io/IOException; {:try_start_c .. :try_end_c} :catch_5

    goto/16 :goto_0

    .line 281
    :catch_5
    move-exception v5

    .line 282
    .restart local v5    # "e1":Ljava/io/IOException;
    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto/16 :goto_0

    .line 233
    .end local v5    # "e1":Ljava/io/IOException;
    :cond_15
    :try_start_d
    move-object/from16 v0, v16

    invoke-virtual {v3, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v22

    if-eqz v22, :cond_18

    .line 234
    const-string v22, "continue:=======\u7248\u672c\u53f7\u76f8\u540c========"

    invoke-static/range {v22 .. v22}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 235
    sget-object v22, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->SAME_VERSION:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    move-object/from16 v0, v22

    move-object/from16 v1, p0

    iput-object v0, v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mUpgradeFileStatus:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;
    :try_end_d
    .catch Ljava/lang/Exception; {:try_start_d .. :try_end_d} :catch_b
    .catchall {:try_start_d .. :try_end_d} :catchall_1

    .line 275
    if-eqz v19, :cond_16

    .line 276
    :try_start_e
    invoke-virtual/range {v19 .. v19}, Ljava/util/zip/ZipInputStream;->close()V

    .line 277
    :cond_16
    if-eqz v10, :cond_17

    .line 278
    invoke-virtual {v10}, Ljava/io/InputStream;->close()V

    .line 279
    :cond_17
    if-eqz v18, :cond_3

    .line 280
    invoke-virtual/range {v18 .. v18}, Ljava/util/zip/ZipFile;->close()V
    :try_end_e
    .catch Ljava/io/IOException; {:try_start_e .. :try_end_e} :catch_6

    goto/16 :goto_0

    .line 281
    :catch_6
    move-exception v5

    .line 282
    .restart local v5    # "e1":Ljava/io/IOException;
    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto/16 :goto_0

    .line 238
    .end local v5    # "e1":Ljava/io/IOException;
    :cond_18
    :try_start_f
    move-object/from16 v0, v16

    invoke-interface {v7, v0}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v22

    if-nez v22, :cond_19

    .line 239
    new-instance v22, Ljava/lang/StringBuilder;

    invoke-direct/range {v22 .. v22}, Ljava/lang/StringBuilder;-><init>()V

    const-string v23, "found one suitable file,version="

    invoke-virtual/range {v22 .. v23}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v22

    move-object/from16 v0, v22

    move-object/from16 v1, v16

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v22

    const-string v23, ",file="

    invoke-virtual/range {v22 .. v23}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v22

    move-object/from16 v0, v22

    invoke-virtual {v0, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object v22

    invoke-virtual/range {v22 .. v22}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v22

    invoke-static/range {v22 .. v22}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 240
    move-object/from16 v0, v16

    invoke-interface {v7, v0, v6}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_f
    .catch Ljava/lang/Exception; {:try_start_f .. :try_end_f} :catch_b
    .catchall {:try_start_f .. :try_end_f} :catchall_1

    .line 275
    :cond_19
    if-eqz v19, :cond_1a

    .line 276
    :try_start_10
    invoke-virtual/range {v19 .. v19}, Ljava/util/zip/ZipInputStream;->close()V

    .line 277
    :cond_1a
    if-eqz v10, :cond_1b

    .line 278
    invoke-virtual {v10}, Ljava/io/InputStream;->close()V

    .line 279
    :cond_1b
    if-eqz v18, :cond_1c

    .line 280
    invoke-virtual/range {v18 .. v18}, Ljava/util/zip/ZipFile;->close()V
    :try_end_10
    .catch Ljava/io/IOException; {:try_start_10 .. :try_end_10} :catch_7

    :cond_1c
    move-object/from16 v17, v18

    .line 283
    .end local v18    # "zf":Ljava/util/zip/ZipFile;
    .restart local v17    # "zf":Ljava/util/zip/ZipFile;
    goto/16 :goto_0

    .line 281
    .end local v17    # "zf":Ljava/util/zip/ZipFile;
    .restart local v18    # "zf":Ljava/util/zip/ZipFile;
    :catch_7
    move-exception v5

    .line 282
    .restart local v5    # "e1":Ljava/io/IOException;
    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    move-object/from16 v17, v18

    .line 284
    .end local v18    # "zf":Ljava/util/zip/ZipFile;
    .restart local v17    # "zf":Ljava/util/zip/ZipFile;
    goto/16 :goto_0

    .line 271
    .end local v5    # "e1":Ljava/io/IOException;
    .end local v9    # "in":Ljava/io/InputStream;
    .end local v11    # "postBuild":Ljava/lang/String;
    .end local v12    # "pro":Ljava/util/Properties;
    .end local v14    # "tmp":[Ljava/lang/String;
    .end local v15    # "tmp1":[Ljava/lang/String;
    .end local v16    # "version":Ljava/lang/String;
    .end local v20    # "zipEntry":Ljava/util/zip/ZipEntry;
    :catch_8
    move-exception v4

    .line 272
    .local v4, "e":Ljava/lang/Exception;
    :goto_1
    :try_start_11
    invoke-static {v4}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V
    :try_end_11
    .catchall {:try_start_11 .. :try_end_11} :catchall_0

    .line 275
    if-eqz v19, :cond_1d

    .line 276
    :try_start_12
    invoke-virtual/range {v19 .. v19}, Ljava/util/zip/ZipInputStream;->close()V

    .line 277
    :cond_1d
    if-eqz v10, :cond_1e

    .line 278
    invoke-virtual {v10}, Ljava/io/InputStream;->close()V

    .line 279
    :cond_1e
    if-eqz v17, :cond_3

    .line 280
    invoke-virtual/range {v17 .. v17}, Ljava/util/zip/ZipFile;->close()V
    :try_end_12
    .catch Ljava/io/IOException; {:try_start_12 .. :try_end_12} :catch_9

    goto/16 :goto_0

    .line 281
    :catch_9
    move-exception v5

    .line 282
    .restart local v5    # "e1":Ljava/io/IOException;
    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto/16 :goto_0

    .line 274
    .end local v4    # "e":Ljava/lang/Exception;
    .end local v5    # "e1":Ljava/io/IOException;
    :catchall_0
    move-exception v21

    .line 275
    :goto_2
    if-eqz v19, :cond_1f

    .line 276
    :try_start_13
    invoke-virtual/range {v19 .. v19}, Ljava/util/zip/ZipInputStream;->close()V

    .line 277
    :cond_1f
    if-eqz v10, :cond_20

    .line 278
    invoke-virtual {v10}, Ljava/io/InputStream;->close()V

    .line 279
    :cond_20
    if-eqz v17, :cond_21

    .line 280
    invoke-virtual/range {v17 .. v17}, Ljava/util/zip/ZipFile;->close()V
    :try_end_13
    .catch Ljava/io/IOException; {:try_start_13 .. :try_end_13} :catch_a

    .line 283
    :cond_21
    :goto_3
    throw v21

    .line 281
    :catch_a
    move-exception v5

    .line 282
    .restart local v5    # "e1":Ljava/io/IOException;
    invoke-static {v5}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_3

    .line 274
    .end local v5    # "e1":Ljava/io/IOException;
    .end local v17    # "zf":Ljava/util/zip/ZipFile;
    .restart local v18    # "zf":Ljava/util/zip/ZipFile;
    :catchall_1
    move-exception v21

    move-object/from16 v17, v18

    .end local v18    # "zf":Ljava/util/zip/ZipFile;
    .restart local v17    # "zf":Ljava/util/zip/ZipFile;
    goto :goto_2

    .line 271
    .end local v17    # "zf":Ljava/util/zip/ZipFile;
    .restart local v18    # "zf":Ljava/util/zip/ZipFile;
    :catch_b
    move-exception v4

    move-object/from16 v17, v18

    .end local v18    # "zf":Ljava/util/zip/ZipFile;
    .restart local v17    # "zf":Ljava/util/zip/ZipFile;
    goto :goto_1
.end method

.method private findUpdateFileList(Ljava/util/List;)Ljava/util/List;
    .locals 9
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Ljava/util/List",
            "<",
            "Ljava/lang/String;",
            ">;)",
            "Ljava/util/List",
            "<",
            "Ljava/util/Map",
            "<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;>;"
        }
    .end annotation

    .prologue
    .local p1, "files":Ljava/util/List;, "Ljava/util/List<Ljava/lang/String;>;"
    const/16 v6, 0xbb8

    .line 136
    invoke-direct {p0, p1}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->filterUnsuitableZipFile(Ljava/util/List;)Ljava/util/Map;

    move-result-object v0

    .line 137
    .local v0, "filesMap":Ljava/util/Map;, "Ljava/util/Map<Ljava/lang/String;Ljava/io/File;>;"
    if-eqz v0, :cond_0

    invoke-interface {v0}, Ljava/util/Map;->size()I

    move-result v4

    if-nez v4, :cond_1

    .line 138
    :cond_0
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, "return: not found suitable upgrade files,lastStatus="

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    iget-object v5, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mUpgradeFileStatus:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-static {v4}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 140
    sget-object v4, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$1;->$SwitchMap$net$sunniwell$service$swupgrade$usb$UpgradeActivity$UpgradeFileStatus:[I

    iget-object v5, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mUpgradeFileStatus:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    invoke-virtual {v5}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->ordinal()I

    move-result v5

    aget v4, v4, v5

    packed-switch v4, :pswitch_data_0

    .line 150
    const v4, 0x7f05001c

    invoke-static {p0, v4, v6}, Landroid/widget/Toast;->makeText(Landroid/content/Context;II)Landroid/widget/Toast;

    move-result-object v4

    invoke-virtual {v4}, Landroid/widget/Toast;->show()V

    .line 153
    :goto_0
    const/4 v1, 0x0

    .line 166
    :goto_1
    return-object v1

    .line 143
    :pswitch_0
    const v4, 0x7f05001a

    invoke-static {p0, v4, v6}, Landroid/widget/Toast;->makeText(Landroid/content/Context;II)Landroid/widget/Toast;

    move-result-object v4

    invoke-virtual {v4}, Landroid/widget/Toast;->show()V

    goto :goto_0

    .line 146
    :pswitch_1
    const v4, 0x7f05001b

    invoke-static {p0, v4, v6}, Landroid/widget/Toast;->makeText(Landroid/content/Context;II)Landroid/widget/Toast;

    move-result-object v4

    invoke-virtual {v4}, Landroid/widget/Toast;->show()V

    goto :goto_0

    .line 155
    :cond_1
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, "found suitable upgrade files, filesMap size="

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-interface {v0}, Ljava/util/Map;->size()I

    move-result v5

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-static {v4}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 157
    new-instance v1, Ljava/util/ArrayList;

    invoke-direct {v1}, Ljava/util/ArrayList;-><init>()V

    .line 158
    .local v1, "mVersionLists":Ljava/util/List;, "Ljava/util/List<Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;>;"
    invoke-interface {v0}, Ljava/util/Map;->keySet()Ljava/util/Set;

    move-result-object v4

    invoke-interface {v4}, Ljava/util/Set;->iterator()Ljava/util/Iterator;

    move-result-object v5

    :goto_2
    invoke-interface {v5}, Ljava/util/Iterator;->hasNext()Z

    move-result v4

    if-eqz v4, :cond_2

    invoke-interface {v5}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v3

    check-cast v3, Ljava/lang/String;

    .line 159
    .local v3, "version":Ljava/lang/String;
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string v6, "version="

    invoke-virtual {v4, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-static {v4}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 160
    new-instance v2, Ljava/util/HashMap;

    invoke-direct {v2}, Ljava/util/HashMap;-><init>()V

    .line 161
    .local v2, "map":Ljava/util/Map;, "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;"
    const-string v4, "version"

    new-instance v6, Ljava/lang/StringBuilder;

    invoke-direct {v6}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->getResources()Landroid/content/res/Resources;

    move-result-object v7

    const v8, 0x7f050013

    invoke-virtual {v7, v8}, Landroid/content/res/Resources;->getText(I)Ljava/lang/CharSequence;

    move-result-object v7

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object v6

    invoke-virtual {v6, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v6

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v6

    invoke-interface {v2, v4, v6}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 162
    const-string v6, "file"

    invoke-interface {v0, v3}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Ljava/io/File;

    invoke-virtual {v4}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v4

    invoke-interface {v2, v6, v4}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    .line 163
    invoke-interface {v1, v2}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    goto :goto_2

    .line 165
    .end local v2    # "map":Ljava/util/Map;, "Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;"
    .end local v3    # "version":Ljava/lang/String;
    :cond_2
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, "found suitable upgrade files mVersionLists.size="

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-interface {v1}, Ljava/util/List;->size()I

    move-result v5

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-static {v4}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    goto/16 :goto_1

    .line 140
    nop

    :pswitch_data_0
    .packed-switch 0x1
        :pswitch_0
        :pswitch_1
    .end packed-switch
.end method

.method private initView()V
    .locals 0

    .prologue
    .line 118
    return-void
.end method

.method private showMess(I)V
    .locals 1
    .param p1, "id"    # I
    .annotation build Landroid/annotation/SuppressLint;
        value = {
            "ShowToast"
        }
    .end annotation

    .prologue
    .line 404
    const/4 v0, 0x0

    invoke-static {p0, p1, v0}, Landroid/widget/Toast;->makeText(Landroid/content/Context;II)Landroid/widget/Toast;

    move-result-object v0

    invoke-virtual {v0}, Landroid/widget/Toast;->show()V

    .line 405
    return-void
.end method


# virtual methods
.method public checkVersion(Lnet/sunniwell/service/swupgrade/usb/VersionInfo;)Z
    .locals 7
    .param p1, "vInfo"    # Lnet/sunniwell/service/swupgrade/usb/VersionInfo;

    .prologue
    const v6, 0x7f050009

    const v5, 0x7f050008

    const/4 v2, 0x0

    .line 313
    sget-object v1, Landroid/os/Build;->FINGERPRINT:Ljava/lang/String;

    .line 314
    .local v1, "stbPreDevice":Ljava/lang/String;
    sget-object v0, Landroid/os/Build;->DEVICE:Ljava/lang/String;

    .line 315
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

    .line 316
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

    .line 317
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

    .line 318
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPostTimestamp()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    .line 316
    invoke-static {v3}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 320
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPreDevice()Ljava/lang/String;

    move-result-object v3

    if-nez v3, :cond_0

    .line 321
    invoke-direct {p0, v5}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->showMess(I)V

    .line 352
    :goto_0
    return v2

    .line 324
    :cond_0
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPreDevice()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v0, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_1

    .line 325
    invoke-direct {p0, v5}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->showMess(I)V

    goto :goto_0

    .line 329
    :cond_1
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPostBuild()Ljava/lang/String;

    move-result-object v3

    if-nez v3, :cond_2

    .line 330
    const-string v3, "~~~~~~~~~~~~~~~~~null~~~~~~~~~~1~~~~~~~~~~"

    .line 331
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPostBuild()Ljava/lang/String;

    move-result-object v4

    .line 330
    invoke-static {v3, v4}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;Ljava/lang/String;)V

    .line 332
    invoke-direct {p0, v6}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->showMess(I)V

    goto :goto_0

    .line 340
    :cond_2
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPostBuild()Ljava/lang/String;

    move-result-object v3

    if-ne v3, v0, :cond_3

    .line 341
    const-string v3, "~~~~~~~~~~~~~~~~\u7248\u672c\u76f8\u540c~~~~~"

    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPostBuild()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    const-string v5, "====="

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-static {v3, v4}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;Ljava/lang/String;)V

    .line 343
    const v3, 0x7f05000a

    invoke-direct {p0, v3}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->showMess(I)V

    goto :goto_0

    .line 346
    :cond_3
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPreBuild()Ljava/lang/String;

    move-result-object v3

    if-eqz v3, :cond_4

    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPreBuild()Ljava/lang/String;

    move-result-object v3

    const-string v4, ""

    invoke-virtual {v3, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_4

    .line 347
    invoke-virtual {p1}, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->getPreBuild()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v3, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v3

    if-nez v3, :cond_4

    .line 349
    invoke-direct {p0, v6}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->showMess(I)V

    goto :goto_0

    .line 352
    :cond_4
    const/4 v2, 0x1

    goto :goto_0
.end method

.method protected finalize()V
    .locals 1
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Throwable;
        }
    .end annotation

    .prologue
    .line 130
    invoke-super {p0}, Ljava/lang/Object;->finalize()V

    .line 131
    const-string v0, "upgrade======finalize()"

    invoke-static {v0}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 132
    return-void
.end method

.method protected onCreate(Landroid/os/Bundle;)V
    .locals 4
    .param p1, "savedInstanceState"    # Landroid/os/Bundle;

    .prologue
    .line 73
    invoke-super {p0, p1}, Landroid/app/Activity;->onCreate(Landroid/os/Bundle;)V

    .line 74
    sput-object p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mInstance:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;

    .line 76
    invoke-virtual {p0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->getIntent()Landroid/content/Intent;

    move-result-object v2

    const-string v3, "zipfile"

    invoke-virtual {v2, v3}, Landroid/content/Intent;->getStringArrayListExtra(Ljava/lang/String;)Ljava/util/ArrayList;

    move-result-object v0

    .line 77
    .local v0, "filelist":Ljava/util/List;, "Ljava/util/List<Ljava/lang/String;>;"
    invoke-direct {p0, v0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->findUpdateFileList(Ljava/util/List;)Ljava/util/List;

    move-result-object v2

    iput-object v2, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mVersionLists:Ljava/util/List;

    .line 78
    iget-object v2, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mVersionLists:Ljava/util/List;

    if-eqz v2, :cond_0

    iget-object v2, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mVersionLists:Ljava/util/List;

    invoke-interface {v2}, Ljava/util/List;->size()I

    move-result v2

    if-nez v2, :cond_1

    .line 80
    :cond_0
    invoke-virtual {p0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->finish()V

    .line 91
    :goto_0
    return-void

    .line 83
    :cond_1
    invoke-direct {p0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->initView()V

    .line 85
    new-instance v1, Landroid/content/Intent;

    invoke-direct {v1}, Landroid/content/Intent;-><init>()V

    .line 86
    .local v1, "intent":Landroid/content/Intent;
    const-string v2, "swupgrade.usb.topWindowService.action"

    invoke-virtual {v1, v2}, Landroid/content/Intent;->setAction(Ljava/lang/String;)Landroid/content/Intent;

    .line 88
    const-string v2, "type"

    const-string v3, "0"

    invoke-virtual {v1, v2, v3}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    .line 90
    invoke-virtual {p0, v1}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->startService(Landroid/content/Intent;)Landroid/content/ComponentName;

    goto :goto_0
.end method

.method protected onDestroy()V
    .locals 0

    .prologue
    .line 125
    invoke-super {p0}, Landroid/app/Activity;->onDestroy()V

    .line 126
    return-void
.end method
