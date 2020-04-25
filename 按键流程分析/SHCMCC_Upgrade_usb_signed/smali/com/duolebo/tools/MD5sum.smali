.class public Lcom/duolebo/tools/MD5sum;
.super Ljava/lang/Object;
.source "MD5sum.java"


# direct methods
.method public constructor <init>()V
    .locals 0

    .prologue
    .line 11
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static md5sum(Ljava/io/File;)Ljava/lang/String;
    .locals 10
    .param p0, "file"    # Ljava/io/File;

    .prologue
    .line 24
    const-string v7, ""

    invoke-static {v7}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 25
    const/4 v5, 0x0

    .line 27
    .local v5, "md5":Ljava/lang/String;
    :try_start_0
    new-instance v1, Ljava/io/BufferedInputStream;

    new-instance v7, Ljava/io/FileInputStream;

    invoke-direct {v7, p0}, Ljava/io/FileInputStream;-><init>(Ljava/io/File;)V

    invoke-direct {v1, v7}, Ljava/io/BufferedInputStream;-><init>(Ljava/io/InputStream;)V

    .line 28
    .local v1, "bis":Ljava/io/BufferedInputStream;
    const/16 v7, 0x2800

    new-array v2, v7, [B

    .line 29
    .local v2, "buf":[B
    const/4 v4, 0x0

    .line 30
    .local v4, "iCount":I
    const-string v7, "MD5"

    invoke-static {v7}, Ljava/security/MessageDigest;->getInstance(Ljava/lang/String;)Ljava/security/MessageDigest;

    move-result-object v0

    .line 31
    .local v0, "alg":Ljava/security/MessageDigest;
    :goto_0
    invoke-virtual {v1, v2}, Ljava/io/BufferedInputStream;->read([B)I

    move-result v4

    const/4 v7, -0x1

    if-ne v4, v7, :cond_0

    .line 35
    invoke-virtual {v1}, Ljava/io/BufferedInputStream;->close()V

    .line 36
    invoke-virtual {v0}, Ljava/security/MessageDigest;->digest()[B

    move-result-object v6

    .line 37
    .local v6, "ret":[B
    invoke-static {v6}, Lcom/duolebo/tools/StringTool;->toHex([B)Ljava/lang/String;
    :try_end_0
    .catch Ljava/io/FileNotFoundException; {:try_start_0 .. :try_end_0} :catch_0
    .catch Ljava/security/NoSuchAlgorithmException; {:try_start_0 .. :try_end_0} :catch_1
    .catch Ljava/io/IOException; {:try_start_0 .. :try_end_0} :catch_2
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    move-result-object v5

    .line 47
    new-instance v7, Ljava/lang/StringBuilder;

    const-string v8, "md5="

    invoke-direct {v7, v8}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v7, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v7

    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v7

    invoke-static {v7}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 49
    .end local v0    # "alg":Ljava/security/MessageDigest;
    .end local v1    # "bis":Ljava/io/BufferedInputStream;
    .end local v2    # "buf":[B
    .end local v4    # "iCount":I
    .end local v6    # "ret":[B
    :goto_1
    return-object v5

    .line 33
    .restart local v0    # "alg":Ljava/security/MessageDigest;
    .restart local v1    # "bis":Ljava/io/BufferedInputStream;
    .restart local v2    # "buf":[B
    .restart local v4    # "iCount":I
    :cond_0
    const/4 v7, 0x0

    :try_start_1
    invoke-virtual {v0, v2, v7, v4}, Ljava/security/MessageDigest;->update([BII)V
    :try_end_1
    .catch Ljava/io/FileNotFoundException; {:try_start_1 .. :try_end_1} :catch_0
    .catch Ljava/security/NoSuchAlgorithmException; {:try_start_1 .. :try_end_1} :catch_1
    .catch Ljava/io/IOException; {:try_start_1 .. :try_end_1} :catch_2
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    goto :goto_0

    .line 38
    .end local v0    # "alg":Ljava/security/MessageDigest;
    .end local v1    # "bis":Ljava/io/BufferedInputStream;
    .end local v2    # "buf":[B
    .end local v4    # "iCount":I
    :catch_0
    move-exception v3

    .line 39
    .local v3, "e":Ljava/io/FileNotFoundException;
    :try_start_2
    invoke-static {v3}, Lcom/duolebo/tools/DLBLog;->w(Ljava/lang/Throwable;)V
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    .line 47
    new-instance v7, Ljava/lang/StringBuilder;

    const-string v8, "md5="

    invoke-direct {v7, v8}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v7, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v7

    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v7

    invoke-static {v7}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    goto :goto_1

    .line 40
    .end local v3    # "e":Ljava/io/FileNotFoundException;
    :catch_1
    move-exception v3

    .line 41
    .local v3, "e":Ljava/security/NoSuchAlgorithmException;
    :try_start_3
    invoke-static {v3}, Lcom/duolebo/tools/DLBLog;->w(Ljava/lang/Throwable;)V
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_0

    .line 47
    new-instance v7, Ljava/lang/StringBuilder;

    const-string v8, "md5="

    invoke-direct {v7, v8}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v7, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v7

    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v7

    invoke-static {v7}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    goto :goto_1

    .line 42
    .end local v3    # "e":Ljava/security/NoSuchAlgorithmException;
    :catch_2
    move-exception v3

    .line 43
    .local v3, "e":Ljava/io/IOException;
    :try_start_4
    invoke-static {v3}, Lcom/duolebo/tools/DLBLog;->w(Ljava/lang/Throwable;)V
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_0

    .line 47
    new-instance v7, Ljava/lang/StringBuilder;

    const-string v8, "md5="

    invoke-direct {v7, v8}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v7, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v7

    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v7

    invoke-static {v7}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    goto :goto_1

    .line 46
    .end local v3    # "e":Ljava/io/IOException;
    :catchall_0
    move-exception v7

    .line 47
    new-instance v8, Ljava/lang/StringBuilder;

    const-string v9, "md5="

    invoke-direct {v8, v9}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v8, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v8

    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v8

    invoke-static {v8}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 48
    throw v7
.end method

.method public static md5sum(Ljava/lang/String;)Ljava/lang/String;
    .locals 1
    .param p0, "fileName"    # Ljava/lang/String;

    .prologue
    .line 19
    new-instance v0, Ljava/io/File;

    invoke-direct {v0, p0}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    invoke-static {v0}, Lcom/duolebo/tools/MD5sum;->md5sum(Ljava/io/File;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public static md5sum([B)Ljava/lang/String;
    .locals 2
    .param p0, "src"    # [B
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    .prologue
    .line 14
    const-string v1, "MD5"

    invoke-static {v1}, Ljava/security/MessageDigest;->getInstance(Ljava/lang/String;)Ljava/security/MessageDigest;

    move-result-object v0

    .line 15
    .local v0, "alg":Ljava/security/MessageDigest;
    invoke-virtual {v0, p0}, Ljava/security/MessageDigest;->digest([B)[B

    move-result-object v1

    invoke-static {v1}, Lcom/duolebo/tools/StringTool;->toHex([B)Ljava/lang/String;

    move-result-object v1

    return-object v1
.end method
