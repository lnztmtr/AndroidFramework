.class public Lcom/duolebo/tools/shcmcc/FileSystem;
.super Ljava/lang/Object;
.source "FileSystem.java"


# annotations
.annotation build Landroid/annotation/SuppressLint;
    value = {
        "NewApi"
    }
.end annotation

.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;
    }
.end annotation


# direct methods
.method public constructor <init>()V
    .locals 0

    .prologue
    .line 20
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method static synthetic access$0(Ljava/lang/String;)J
    .locals 2

    .prologue
    .line 128
    invoke-static {p0}, Lcom/duolebo/tools/shcmcc/FileSystem;->getTotalSize(Ljava/lang/String;)J

    move-result-wide v0

    return-wide v0
.end method

.method static synthetic access$1(Ljava/lang/String;)J
    .locals 2

    .prologue
    .line 116
    invoke-static {p0}, Lcom/duolebo/tools/shcmcc/FileSystem;->getAvailSize(Ljava/lang/String;)J

    move-result-wide v0

    return-wide v0
.end method

.method private static getAvailSize(Ljava/lang/String;)J
    .locals 5
    .param p0, "path"    # Ljava/lang/String;

    .prologue
    .line 117
    new-instance v0, Landroid/os/StatFs;

    invoke-direct {v0, p0}, Landroid/os/StatFs;-><init>(Ljava/lang/String;)V

    .line 118
    .local v0, "fileStats":Landroid/os/StatFs;
    invoke-virtual {v0, p0}, Landroid/os/StatFs;->restat(Ljava/lang/String;)V

    .line 119
    invoke-virtual {v0}, Landroid/os/StatFs;->getAvailableBlocks()I

    move-result v1

    int-to-long v1, v1

    invoke-virtual {v0}, Landroid/os/StatFs;->getBlockSize()I

    move-result v3

    int-to-long v3, v3

    mul-long/2addr v1, v3

    return-wide v1
.end method

.method public static getHumanSize(J)Ljava/lang/String;
    .locals 8
    .param p0, "size"    # J

    .prologue
    const-wide/16 v6, 0x400

    const-wide/high16 v4, 0x4090000000000000L    # 1024.0

    .line 140
    div-long v2, p0, v6

    div-long/2addr v2, v6

    long-to-double v0, v2

    .line 141
    .local v0, "d":D
    cmpl-double v2, v0, v4

    if-lez v2, :cond_0

    .line 142
    new-instance v2, Ljava/lang/StringBuilder;

    div-double v3, v0, v4

    invoke-static {v3, v4}, Ljava/lang/Double;->valueOf(D)Ljava/lang/Double;

    move-result-object v3

    const/4 v4, 0x2

    invoke-static {v3, v4}, Lcom/duolebo/tools/shcmcc/FileSystem;->round(Ljava/lang/Double;I)D

    move-result-wide v3

    invoke-static {v3, v4}, Ljava/lang/String;->valueOf(D)Ljava/lang/String;

    move-result-object v3

    invoke-static {v3}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    const-string v3, " GB"

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    .line 144
    :goto_0
    return-object v2

    :cond_0
    new-instance v2, Ljava/lang/StringBuilder;

    double-to-int v3, v0

    invoke-static {v3}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    const-string v3, " MB"

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    goto :goto_0
.end method

.method public static getStorageList()Ljava/util/List;
    .locals 34
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()",
            "Ljava/util/List",
            "<",
            "Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;",
            ">;"
        }
    .end annotation

    .prologue
    .line 23
    new-instance v30, Ljava/util/ArrayList;

    invoke-direct/range {v30 .. v30}, Ljava/util/ArrayList;-><init>()V

    .line 25
    .local v30, "list":Ljava/util/List;, "Ljava/util/List<Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;>;"
    invoke-static {}, Landroid/os/Environment;->getDataDirectory()Ljava/io/File;

    move-result-object v2

    invoke-virtual {v2}, Ljava/io/File;->getPath()Ljava/lang/String;

    move-result-object v3

    .line 26
    .local v3, "self_path":Ljava/lang/String;
    invoke-static {}, Landroid/os/Environment;->getExternalStorageDirectory()Ljava/io/File;

    move-result-object v2

    invoke-virtual {v2}, Ljava/io/File;->getPath()Ljava/lang/String;

    move-result-object v9

    .line 27
    .local v9, "def_path":Ljava/lang/String;
    invoke-static {}, Landroid/os/Environment;->isExternalStorageRemovable()Z

    move-result v11

    .line 28
    .local v11, "def_path_removable":Z
    invoke-static {}, Landroid/os/Environment;->getExternalStorageState()Ljava/lang/String;

    move-result-object v26

    .line 30
    .local v26, "def_path_state":Ljava/lang/String;
    const-string v2, "mounted"

    move-object/from16 v0, v26

    invoke-virtual {v0, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-nez v2, :cond_2

    .line 31
    const-string v2, "mounted_ro"

    move-object/from16 v0, v26

    invoke-virtual {v0, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-nez v2, :cond_2

    .line 30
    const/16 v25, 0x0

    .line 33
    .local v25, "def_path_available":Z
    :goto_0
    invoke-static {}, Landroid/os/Environment;->getExternalStorageState()Ljava/lang/String;

    move-result-object v2

    const-string v4, "mounted_ro"

    invoke-virtual {v2, v4}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v10

    .line 35
    .local v10, "def_path_readonly":Z
    new-instance v31, Ljava/util/HashSet;

    invoke-direct/range {v31 .. v31}, Ljava/util/HashSet;-><init>()V

    .line 36
    .local v31, "paths":Ljava/util/HashSet;, "Ljava/util/HashSet<Ljava/lang/String;>;"
    const/16 v19, 0x1

    .line 37
    .local v19, "cur_removable_number":I
    const/16 v7, 0x3e8

    .line 39
    .local v7, "id":I
    move-object/from16 v0, v31

    invoke-virtual {v0, v3}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z

    .line 40
    new-instance v2, Ljava/io/File;

    invoke-direct {v2, v3}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    invoke-static {v2}, Lcom/duolebo/tools/shcmcc/FileSystem;->isDirectoryWriteable(Ljava/io/File;)Z

    move-result v2

    if-eqz v2, :cond_b

    .line 41
    const/4 v15, 0x0

    new-instance v2, Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;

    const/4 v4, 0x0

    const/4 v5, 0x0

    const/4 v6, 0x0

    add-int/lit8 v13, v7, 0x1

    .end local v7    # "id":I
    .local v13, "id":I
    const/4 v8, 0x0

    invoke-direct/range {v2 .. v8}, Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;-><init>(Ljava/lang/String;ZZIII)V

    move-object/from16 v0, v30

    invoke-interface {v0, v15, v2}, Ljava/util/List;->add(ILjava/lang/Object;)V

    .line 44
    :goto_1
    if-eqz v25, :cond_a

    new-instance v2, Ljava/io/File;

    invoke-direct {v2, v9}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    invoke-static {v2}, Lcom/duolebo/tools/shcmcc/FileSystem;->isDirectoryWriteable(Ljava/io/File;)Z

    move-result v2

    if-eqz v2, :cond_a

    .line 45
    move-object/from16 v0, v31

    invoke-virtual {v0, v9}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z

    .line 46
    if-eqz v11, :cond_3

    add-int/lit8 v24, v19, 0x1

    .end local v19    # "cur_removable_number":I
    .local v24, "cur_removable_number":I
    move/from16 v12, v19

    move/from16 v19, v24

    .line 47
    .end local v24    # "cur_removable_number":I
    .local v12, "number":I
    .restart local v19    # "cur_removable_number":I
    :goto_2
    if-eqz v11, :cond_4

    const/4 v14, 0x2

    .line 48
    .local v14, "type":I
    :goto_3
    new-instance v8, Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;

    add-int/lit8 v7, v13, 0x1

    .end local v13    # "id":I
    .restart local v7    # "id":I
    invoke-direct/range {v8 .. v14}, Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;-><init>(Ljava/lang/String;ZZIII)V

    move-object/from16 v0, v30

    invoke-interface {v0, v8}, Ljava/util/List;->add(Ljava/lang/Object;)Z

    .line 51
    .end local v12    # "number":I
    .end local v14    # "type":I
    :goto_4
    const/16 v22, 0x0

    .line 53
    .local v22, "buf_reader":Ljava/io/BufferedReader;
    :try_start_0
    new-instance v23, Ljava/io/BufferedReader;

    new-instance v2, Ljava/io/FileReader;

    const-string v4, "/proc/mounts"

    invoke-direct {v2, v4}, Ljava/io/FileReader;-><init>(Ljava/lang/String;)V

    move-object/from16 v0, v23

    invoke-direct {v0, v2}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;)V
    :try_end_0
    .catch Ljava/io/FileNotFoundException; {:try_start_0 .. :try_end_0} :catch_0
    .catch Ljava/io/IOException; {:try_start_0 .. :try_end_0} :catch_2
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .end local v22    # "buf_reader":Ljava/io/BufferedReader;
    .local v23, "buf_reader":Ljava/io/BufferedReader;
    move v13, v7

    .line 55
    .end local v7    # "id":I
    .restart local v13    # "id":I
    :cond_0
    :goto_5
    :try_start_1
    invoke-virtual/range {v23 .. v23}, Ljava/io/BufferedReader;->readLine()Ljava/lang/String;
    :try_end_1
    .catch Ljava/io/FileNotFoundException; {:try_start_1 .. :try_end_1} :catch_8
    .catch Ljava/io/IOException; {:try_start_1 .. :try_end_1} :catch_6
    .catchall {:try_start_1 .. :try_end_1} :catchall_1

    move-result-object v29

    .local v29, "line":Ljava/lang/String;
    if-nez v29, :cond_5

    .line 87
    if-eqz v23, :cond_9

    .line 89
    :try_start_2
    invoke-virtual/range {v23 .. v23}, Ljava/io/BufferedReader;->close()V
    :try_end_2
    .catch Ljava/io/IOException; {:try_start_2 .. :try_end_2} :catch_4

    move-object/from16 v22, v23

    .end local v23    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v22    # "buf_reader":Ljava/io/BufferedReader;
    move v7, v13

    .line 93
    .end local v13    # "id":I
    .end local v29    # "line":Ljava/lang/String;
    .restart local v7    # "id":I
    :cond_1
    :goto_6
    return-object v30

    .line 30
    .end local v7    # "id":I
    .end local v10    # "def_path_readonly":Z
    .end local v19    # "cur_removable_number":I
    .end local v22    # "buf_reader":Ljava/io/BufferedReader;
    .end local v25    # "def_path_available":Z
    .end local v31    # "paths":Ljava/util/HashSet;, "Ljava/util/HashSet<Ljava/lang/String;>;"
    :cond_2
    const/16 v25, 0x1

    goto :goto_0

    .line 46
    .restart local v10    # "def_path_readonly":Z
    .restart local v13    # "id":I
    .restart local v19    # "cur_removable_number":I
    .restart local v25    # "def_path_available":Z
    .restart local v31    # "paths":Ljava/util/HashSet;, "Ljava/util/HashSet<Ljava/lang/String;>;"
    :cond_3
    const/4 v12, -0x1

    goto :goto_2

    .line 47
    .restart local v12    # "number":I
    :cond_4
    const/4 v14, 0x1

    goto :goto_3

    .line 56
    .end local v12    # "number":I
    .restart local v23    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v29    # "line":Ljava/lang/String;
    :cond_5
    :try_start_3
    const-string v2, "vfat"

    move-object/from16 v0, v29

    invoke-virtual {v0, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v2

    if-nez v2, :cond_6

    const-string v2, "/mnt"

    move-object/from16 v0, v29

    invoke-virtual {v0, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v2

    if-eqz v2, :cond_0

    .line 57
    :cond_6
    new-instance v32, Ljava/util/StringTokenizer;

    const-string v2, " "

    move-object/from16 v0, v32

    move-object/from16 v1, v29

    invoke-direct {v0, v1, v2}, Ljava/util/StringTokenizer;-><init>(Ljava/lang/String;Ljava/lang/String;)V

    .line 58
    .local v32, "tokens":Ljava/util/StringTokenizer;
    invoke-virtual/range {v32 .. v32}, Ljava/util/StringTokenizer;->nextToken()Ljava/lang/String;

    move-result-object v33

    .line 59
    .local v33, "unused":Ljava/lang/String;
    invoke-virtual/range {v32 .. v32}, Ljava/util/StringTokenizer;->nextToken()Ljava/lang/String;

    move-result-object v16

    .line 60
    .local v16, "mount_point":Ljava/lang/String;
    move-object/from16 v0, v31

    move-object/from16 v1, v16

    invoke-virtual {v0, v1}, Ljava/util/HashSet;->contains(Ljava/lang/Object;)Z

    move-result v2

    if-nez v2, :cond_0

    .line 63
    invoke-virtual/range {v32 .. v32}, Ljava/util/StringTokenizer;->nextToken()Ljava/lang/String;

    move-result-object v33

    .line 64
    invoke-virtual/range {v32 .. v32}, Ljava/util/StringTokenizer;->nextToken()Ljava/lang/String;

    move-result-object v2

    const-string v4, ","

    invoke-virtual {v2, v4}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v2

    invoke-static {v2}, Ljava/util/Arrays;->asList([Ljava/lang/Object;)Ljava/util/List;

    move-result-object v28

    .line 65
    .local v28, "flags":Ljava/util/List;, "Ljava/util/List<Ljava/lang/String;>;"
    const-string v2, "ro"

    move-object/from16 v0, v28

    invoke-interface {v0, v2}, Ljava/util/List;->contains(Ljava/lang/Object;)Z

    move-result v17

    .line 67
    .local v17, "readonly":Z
    const-string v2, "/dev/block/vold"

    move-object/from16 v0, v29

    invoke-virtual {v0, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v2

    if-eqz v2, :cond_0

    .line 68
    const-string v2, "/mnt/secure"

    move-object/from16 v0, v29

    invoke-virtual {v0, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v2

    if-nez v2, :cond_0

    .line 69
    const-string v2, "/mnt/asec"

    move-object/from16 v0, v29

    invoke-virtual {v0, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v2

    if-nez v2, :cond_0

    .line 70
    const-string v2, "/mnt/obb"

    move-object/from16 v0, v29

    invoke-virtual {v0, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v2

    if-nez v2, :cond_0

    .line 71
    const-string v2, "/dev/mapper"

    move-object/from16 v0, v29

    invoke-virtual {v0, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v2

    if-nez v2, :cond_0

    .line 72
    const-string v2, "tmpfs"

    move-object/from16 v0, v29

    invoke-virtual {v0, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z

    move-result v2

    if-nez v2, :cond_0

    .line 73
    move-object/from16 v0, v31

    move-object/from16 v1, v16

    invoke-virtual {v0, v1}, Ljava/util/HashSet;->add(Ljava/lang/Object;)Z

    .line 74
    const/4 v2, 0x1

    move/from16 v0, v19

    if-le v0, v2, :cond_7

    const/4 v14, 0x3

    .line 75
    .restart local v14    # "type":I
    :goto_7
    new-instance v2, Ljava/io/File;

    move-object/from16 v0, v16

    invoke-direct {v2, v0}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    invoke-static {v2}, Lcom/duolebo/tools/shcmcc/FileSystem;->isDirectoryWriteable(Ljava/io/File;)Z

    move-result v2

    if-eqz v2, :cond_0

    .line 76
    new-instance v15, Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;
    :try_end_3
    .catch Ljava/io/FileNotFoundException; {:try_start_3 .. :try_end_3} :catch_8
    .catch Ljava/io/IOException; {:try_start_3 .. :try_end_3} :catch_6
    .catchall {:try_start_3 .. :try_end_3} :catchall_1

    const/16 v18, 0x1

    add-int/lit8 v24, v19, 0x1

    .end local v19    # "cur_removable_number":I
    .restart local v24    # "cur_removable_number":I
    add-int/lit8 v7, v13, 0x1

    .end local v13    # "id":I
    .restart local v7    # "id":I
    move/from16 v20, v13

    move/from16 v21, v14

    :try_start_4
    invoke-direct/range {v15 .. v21}, Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;-><init>(Ljava/lang/String;ZZIII)V

    move-object/from16 v0, v30

    invoke-interface {v0, v15}, Ljava/util/List;->add(Ljava/lang/Object;)Z
    :try_end_4
    .catch Ljava/io/FileNotFoundException; {:try_start_4 .. :try_end_4} :catch_9
    .catch Ljava/io/IOException; {:try_start_4 .. :try_end_4} :catch_7
    .catchall {:try_start_4 .. :try_end_4} :catchall_2

    move v13, v7

    .end local v7    # "id":I
    .restart local v13    # "id":I
    move/from16 v19, v24

    .end local v24    # "cur_removable_number":I
    .restart local v19    # "cur_removable_number":I
    goto/16 :goto_5

    .line 74
    .end local v14    # "type":I
    :cond_7
    const/4 v14, 0x2

    goto :goto_7

    .line 82
    .end local v13    # "id":I
    .end local v16    # "mount_point":Ljava/lang/String;
    .end local v17    # "readonly":Z
    .end local v23    # "buf_reader":Ljava/io/BufferedReader;
    .end local v28    # "flags":Ljava/util/List;, "Ljava/util/List<Ljava/lang/String;>;"
    .end local v29    # "line":Ljava/lang/String;
    .end local v32    # "tokens":Ljava/util/StringTokenizer;
    .end local v33    # "unused":Ljava/lang/String;
    .restart local v7    # "id":I
    .restart local v22    # "buf_reader":Ljava/io/BufferedReader;
    :catch_0
    move-exception v27

    .line 83
    .local v27, "ex":Ljava/io/FileNotFoundException;
    :goto_8
    :try_start_5
    invoke-virtual/range {v27 .. v27}, Ljava/io/FileNotFoundException;->printStackTrace()V
    :try_end_5
    .catchall {:try_start_5 .. :try_end_5} :catchall_0

    .line 87
    if-eqz v22, :cond_1

    .line 89
    :try_start_6
    invoke-virtual/range {v22 .. v22}, Ljava/io/BufferedReader;->close()V
    :try_end_6
    .catch Ljava/io/IOException; {:try_start_6 .. :try_end_6} :catch_1

    goto/16 :goto_6

    .line 90
    :catch_1
    move-exception v2

    goto/16 :goto_6

    .line 84
    .end local v27    # "ex":Ljava/io/FileNotFoundException;
    :catch_2
    move-exception v27

    .line 85
    .local v27, "ex":Ljava/io/IOException;
    :goto_9
    :try_start_7
    invoke-virtual/range {v27 .. v27}, Ljava/io/IOException;->printStackTrace()V
    :try_end_7
    .catchall {:try_start_7 .. :try_end_7} :catchall_0

    .line 87
    if-eqz v22, :cond_1

    .line 89
    :try_start_8
    invoke-virtual/range {v22 .. v22}, Ljava/io/BufferedReader;->close()V
    :try_end_8
    .catch Ljava/io/IOException; {:try_start_8 .. :try_end_8} :catch_3

    goto/16 :goto_6

    .line 90
    :catch_3
    move-exception v2

    goto/16 :goto_6

    .line 86
    .end local v27    # "ex":Ljava/io/IOException;
    :catchall_0
    move-exception v2

    .line 87
    :goto_a
    if-eqz v22, :cond_8

    .line 89
    :try_start_9
    invoke-virtual/range {v22 .. v22}, Ljava/io/BufferedReader;->close()V
    :try_end_9
    .catch Ljava/io/IOException; {:try_start_9 .. :try_end_9} :catch_5

    .line 92
    :cond_8
    :goto_b
    throw v2

    .line 90
    .end local v7    # "id":I
    .end local v22    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v13    # "id":I
    .restart local v23    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v29    # "line":Ljava/lang/String;
    :catch_4
    move-exception v2

    move-object/from16 v22, v23

    .end local v23    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v22    # "buf_reader":Ljava/io/BufferedReader;
    move v7, v13

    .end local v13    # "id":I
    .restart local v7    # "id":I
    goto/16 :goto_6

    .end local v29    # "line":Ljava/lang/String;
    :catch_5
    move-exception v4

    goto :goto_b

    .line 86
    .end local v7    # "id":I
    .end local v22    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v13    # "id":I
    .restart local v23    # "buf_reader":Ljava/io/BufferedReader;
    :catchall_1
    move-exception v2

    move-object/from16 v22, v23

    .end local v23    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v22    # "buf_reader":Ljava/io/BufferedReader;
    move v7, v13

    .end local v13    # "id":I
    .restart local v7    # "id":I
    goto :goto_a

    .end local v19    # "cur_removable_number":I
    .end local v22    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v14    # "type":I
    .restart local v16    # "mount_point":Ljava/lang/String;
    .restart local v17    # "readonly":Z
    .restart local v23    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v24    # "cur_removable_number":I
    .restart local v28    # "flags":Ljava/util/List;, "Ljava/util/List<Ljava/lang/String;>;"
    .restart local v29    # "line":Ljava/lang/String;
    .restart local v32    # "tokens":Ljava/util/StringTokenizer;
    .restart local v33    # "unused":Ljava/lang/String;
    :catchall_2
    move-exception v2

    move-object/from16 v22, v23

    .end local v23    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v22    # "buf_reader":Ljava/io/BufferedReader;
    move/from16 v19, v24

    .end local v24    # "cur_removable_number":I
    .restart local v19    # "cur_removable_number":I
    goto :goto_a

    .line 84
    .end local v7    # "id":I
    .end local v14    # "type":I
    .end local v16    # "mount_point":Ljava/lang/String;
    .end local v17    # "readonly":Z
    .end local v22    # "buf_reader":Ljava/io/BufferedReader;
    .end local v28    # "flags":Ljava/util/List;, "Ljava/util/List<Ljava/lang/String;>;"
    .end local v29    # "line":Ljava/lang/String;
    .end local v32    # "tokens":Ljava/util/StringTokenizer;
    .end local v33    # "unused":Ljava/lang/String;
    .restart local v13    # "id":I
    .restart local v23    # "buf_reader":Ljava/io/BufferedReader;
    :catch_6
    move-exception v27

    move-object/from16 v22, v23

    .end local v23    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v22    # "buf_reader":Ljava/io/BufferedReader;
    move v7, v13

    .end local v13    # "id":I
    .restart local v7    # "id":I
    goto :goto_9

    .end local v19    # "cur_removable_number":I
    .end local v22    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v14    # "type":I
    .restart local v16    # "mount_point":Ljava/lang/String;
    .restart local v17    # "readonly":Z
    .restart local v23    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v24    # "cur_removable_number":I
    .restart local v28    # "flags":Ljava/util/List;, "Ljava/util/List<Ljava/lang/String;>;"
    .restart local v29    # "line":Ljava/lang/String;
    .restart local v32    # "tokens":Ljava/util/StringTokenizer;
    .restart local v33    # "unused":Ljava/lang/String;
    :catch_7
    move-exception v27

    move-object/from16 v22, v23

    .end local v23    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v22    # "buf_reader":Ljava/io/BufferedReader;
    move/from16 v19, v24

    .end local v24    # "cur_removable_number":I
    .restart local v19    # "cur_removable_number":I
    goto :goto_9

    .line 82
    .end local v7    # "id":I
    .end local v14    # "type":I
    .end local v16    # "mount_point":Ljava/lang/String;
    .end local v17    # "readonly":Z
    .end local v22    # "buf_reader":Ljava/io/BufferedReader;
    .end local v28    # "flags":Ljava/util/List;, "Ljava/util/List<Ljava/lang/String;>;"
    .end local v29    # "line":Ljava/lang/String;
    .end local v32    # "tokens":Ljava/util/StringTokenizer;
    .end local v33    # "unused":Ljava/lang/String;
    .restart local v13    # "id":I
    .restart local v23    # "buf_reader":Ljava/io/BufferedReader;
    :catch_8
    move-exception v27

    move-object/from16 v22, v23

    .end local v23    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v22    # "buf_reader":Ljava/io/BufferedReader;
    move v7, v13

    .end local v13    # "id":I
    .restart local v7    # "id":I
    goto :goto_8

    .end local v19    # "cur_removable_number":I
    .end local v22    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v14    # "type":I
    .restart local v16    # "mount_point":Ljava/lang/String;
    .restart local v17    # "readonly":Z
    .restart local v23    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v24    # "cur_removable_number":I
    .restart local v28    # "flags":Ljava/util/List;, "Ljava/util/List<Ljava/lang/String;>;"
    .restart local v29    # "line":Ljava/lang/String;
    .restart local v32    # "tokens":Ljava/util/StringTokenizer;
    .restart local v33    # "unused":Ljava/lang/String;
    :catch_9
    move-exception v27

    move-object/from16 v22, v23

    .end local v23    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v22    # "buf_reader":Ljava/io/BufferedReader;
    move/from16 v19, v24

    .end local v24    # "cur_removable_number":I
    .restart local v19    # "cur_removable_number":I
    goto :goto_8

    .end local v7    # "id":I
    .end local v14    # "type":I
    .end local v16    # "mount_point":Ljava/lang/String;
    .end local v17    # "readonly":Z
    .end local v22    # "buf_reader":Ljava/io/BufferedReader;
    .end local v28    # "flags":Ljava/util/List;, "Ljava/util/List<Ljava/lang/String;>;"
    .end local v32    # "tokens":Ljava/util/StringTokenizer;
    .end local v33    # "unused":Ljava/lang/String;
    .restart local v13    # "id":I
    .restart local v23    # "buf_reader":Ljava/io/BufferedReader;
    :cond_9
    move-object/from16 v22, v23

    .end local v23    # "buf_reader":Ljava/io/BufferedReader;
    .restart local v22    # "buf_reader":Ljava/io/BufferedReader;
    move v7, v13

    .end local v13    # "id":I
    .restart local v7    # "id":I
    goto/16 :goto_6

    .end local v7    # "id":I
    .end local v22    # "buf_reader":Ljava/io/BufferedReader;
    .end local v29    # "line":Ljava/lang/String;
    .restart local v13    # "id":I
    :cond_a
    move v7, v13

    .end local v13    # "id":I
    .restart local v7    # "id":I
    goto/16 :goto_4

    :cond_b
    move v13, v7

    .end local v7    # "id":I
    .restart local v13    # "id":I
    goto/16 :goto_1
.end method

.method private static getTotalSize(Ljava/lang/String;)J
    .locals 5
    .param p0, "path"    # Ljava/lang/String;

    .prologue
    .line 129
    new-instance v0, Landroid/os/StatFs;

    invoke-direct {v0, p0}, Landroid/os/StatFs;-><init>(Ljava/lang/String;)V

    .line 130
    .local v0, "fileStats":Landroid/os/StatFs;
    invoke-virtual {v0, p0}, Landroid/os/StatFs;->restat(Ljava/lang/String;)V

    .line 131
    invoke-virtual {v0}, Landroid/os/StatFs;->getBlockCount()I

    move-result v1

    int-to-long v1, v1

    invoke-virtual {v0}, Landroid/os/StatFs;->getBlockSize()I

    move-result v3

    int-to-long v3, v3

    mul-long/2addr v1, v3

    return-wide v1
.end method

.method public static isDirectoryWriteable(Ljava/io/File;)Z
    .locals 5
    .param p0, "dir"    # Ljava/io/File;

    .prologue
    const/4 v2, 0x1

    .line 97
    invoke-virtual {p0}, Ljava/io/File;->exists()Z

    move-result v3

    if-eqz v3, :cond_1

    invoke-virtual {p0}, Ljava/io/File;->isDirectory()Z

    move-result v3

    if-eqz v3, :cond_1

    .line 98
    invoke-virtual {p0}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v1

    .line 99
    .local v1, "dirStr":Ljava/lang/String;
    new-instance v0, Ljava/io/File;

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-static {v1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v4

    invoke-direct {v3, v4}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    sget-object v4, Ljava/io/File;->separator:Ljava/lang/String;

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    const-string v4, ".dlbCheck"

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-direct {v0, v3}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    .line 100
    .local v0, "check":Ljava/io/File;
    invoke-virtual {v0}, Ljava/io/File;->exists()Z

    move-result v3

    if-eqz v3, :cond_0

    .line 107
    .end local v0    # "check":Ljava/io/File;
    .end local v1    # "dirStr":Ljava/lang/String;
    :goto_0
    return v2

    .line 102
    .restart local v0    # "check":Ljava/io/File;
    .restart local v1    # "dirStr":Ljava/lang/String;
    :cond_0
    invoke-virtual {v0}, Ljava/io/File;->mkdir()Z

    move-result v3

    if-eqz v3, :cond_1

    .line 103
    invoke-virtual {v0}, Ljava/io/File;->delete()Z

    goto :goto_0

    .line 107
    .end local v0    # "check":Ljava/io/File;
    .end local v1    # "dirStr":Ljava/lang/String;
    :cond_1
    const/4 v2, 0x0

    goto :goto_0
.end method

.method private static round(Ljava/lang/Double;I)D
    .locals 4
    .param p0, "v"    # Ljava/lang/Double;
    .param p1, "scale"    # I

    .prologue
    .line 149
    if-gez p1, :cond_0

    .line 150
    new-instance v2, Ljava/lang/IllegalArgumentException;

    const-string v3, "The scale must be a positive integer or zero"

    invoke-direct {v2, v3}, Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V

    throw v2

    .line 153
    :cond_0
    if-nez p0, :cond_1

    new-instance v0, Ljava/math/BigDecimal;

    const-string v2, "0.0"

    invoke-direct {v0, v2}, Ljava/math/BigDecimal;-><init>(Ljava/lang/String;)V

    .line 154
    .local v0, "b":Ljava/math/BigDecimal;
    :goto_0
    new-instance v1, Ljava/math/BigDecimal;

    const-string v2, "1"

    invoke-direct {v1, v2}, Ljava/math/BigDecimal;-><init>(Ljava/lang/String;)V

    .line 155
    .local v1, "one":Ljava/math/BigDecimal;
    const/4 v2, 0x4

    invoke-virtual {v0, v1, p1, v2}, Ljava/math/BigDecimal;->divide(Ljava/math/BigDecimal;II)Ljava/math/BigDecimal;

    move-result-object v2

    invoke-virtual {v2}, Ljava/math/BigDecimal;->doubleValue()D

    move-result-wide v2

    return-wide v2

    .line 153
    .end local v0    # "b":Ljava/math/BigDecimal;
    .end local v1    # "one":Ljava/math/BigDecimal;
    :cond_1
    new-instance v0, Ljava/math/BigDecimal;

    invoke-virtual {p0}, Ljava/lang/Double;->doubleValue()D

    move-result-wide v2

    invoke-static {v2, v3}, Ljava/lang/Double;->toString(D)Ljava/lang/String;

    move-result-object v2

    invoke-direct {v0, v2}, Ljava/math/BigDecimal;-><init>(Ljava/lang/String;)V

    goto :goto_0
.end method
