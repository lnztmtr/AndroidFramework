.class public Lcom/duolebo/tools/shcmcc/DESede;
.super Ljava/lang/Object;
.source "DESede.java"


# static fields
.field private static alphabet:[C


# direct methods
.method static constructor <clinit>()V
    .locals 1

    .prologue
    .line 16
    const-string v0, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="

    .line 17
    invoke-virtual {v0}, Ljava/lang/String;->toCharArray()[C

    move-result-object v0

    .line 16
    sput-object v0, Lcom/duolebo/tools/shcmcc/DESede;->alphabet:[C

    .line 17
    return-void
.end method

.method public constructor <init>()V
    .locals 0

    .prologue
    .line 14
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static DES3Decode(Ljava/lang/String;[B)[B
    .locals 5
    .param p0, "key"    # Ljava/lang/String;
    .param p1, "source"    # [B

    .prologue
    .line 164
    :try_start_0
    new-instance v1, Ljavax/crypto/spec/SecretKeySpec;

    const-string v3, "UTF-8"

    invoke-virtual {p0, v3}, Ljava/lang/String;->getBytes(Ljava/lang/String;)[B

    move-result-object v3

    .line 165
    const-string v4, "DESede"

    .line 164
    invoke-direct {v1, v3, v4}, Ljavax/crypto/spec/SecretKeySpec;-><init>([BLjava/lang/String;)V

    .line 166
    .local v1, "deskey":Ljavax/crypto/SecretKey;
    const-string v3, "DESede"

    invoke-static {v3}, Ljavax/crypto/Cipher;->getInstance(Ljava/lang/String;)Ljavax/crypto/Cipher;

    move-result-object v0

    .line 167
    .local v0, "c3des":Ljavax/crypto/Cipher;
    const/4 v3, 0x2

    invoke-virtual {v0, v3, v1}, Ljavax/crypto/Cipher;->init(ILjava/security/Key;)V

    .line 168
    invoke-virtual {v0, p1}, Ljavax/crypto/Cipher;->doFinal([B)[B
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    move-result-object v3

    .line 173
    .end local v0    # "c3des":Ljavax/crypto/Cipher;
    .end local v1    # "deskey":Ljavax/crypto/SecretKey;
    :goto_0
    return-object v3

    .line 170
    :catch_0
    move-exception v2

    .line 171
    .local v2, "e":Ljava/lang/Exception;
    invoke-virtual {v2}, Ljava/lang/Exception;->printStackTrace()V

    .line 173
    const/4 v3, 0x0

    goto :goto_0
.end method

.method public static DES3Encode(Ljava/lang/String;[B)[B
    .locals 5
    .param p0, "key"    # Ljava/lang/String;
    .param p1, "source"    # [B

    .prologue
    .line 151
    :try_start_0
    new-instance v1, Ljavax/crypto/spec/SecretKeySpec;

    const-string v3, "UTF-8"

    invoke-virtual {p0, v3}, Ljava/lang/String;->getBytes(Ljava/lang/String;)[B

    move-result-object v3

    .line 152
    const-string v4, "DESede"

    .line 151
    invoke-direct {v1, v3, v4}, Ljavax/crypto/spec/SecretKeySpec;-><init>([BLjava/lang/String;)V

    .line 153
    .local v1, "deskey":Ljavax/crypto/SecretKey;
    const-string v3, "DESede"

    invoke-static {v3}, Ljavax/crypto/Cipher;->getInstance(Ljava/lang/String;)Ljavax/crypto/Cipher;

    move-result-object v0

    .line 154
    .local v0, "c3des":Ljavax/crypto/Cipher;
    const/4 v3, 0x1

    invoke-virtual {v0, v3, v1}, Ljavax/crypto/Cipher;->init(ILjava/security/Key;)V

    .line 155
    invoke-virtual {v0, p1}, Ljavax/crypto/Cipher;->doFinal([B)[B
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    move-result-object v3

    .line 159
    .end local v0    # "c3des":Ljavax/crypto/Cipher;
    .end local v1    # "deskey":Ljavax/crypto/SecretKey;
    :goto_0
    return-object v3

    .line 156
    :catch_0
    move-exception v2

    .line 157
    .local v2, "e":Ljava/lang/Exception;
    invoke-virtual {v2}, Ljava/lang/Exception;->printStackTrace()V

    .line 159
    const/4 v3, 0x0

    goto :goto_0
.end method

.method public static base64Encode([B)Ljava/lang/String;
    .locals 1
    .param p0, "bt"    # [B
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    .prologue
    .line 132
    invoke-static {p0}, Lcom/duolebo/tools/shcmcc/DESede;->encode([B)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public static encode([B)Ljava/lang/String;
    .locals 10
    .param p0, "data"    # [B

    .prologue
    const/16 v7, 0x40

    .line 177
    array-length v6, p0

    add-int/lit8 v6, v6, 0x2

    div-int/lit8 v6, v6, 0x3

    mul-int/lit8 v6, v6, 0x4

    new-array v2, v6, [C

    .line 178
    .local v2, "out":[C
    const/4 v0, 0x0

    .line 179
    .local v0, "i":I
    const/4 v1, 0x0

    .line 180
    .local v1, "index":I
    :goto_0
    array-length v6, p0

    if-lt v0, v6, :cond_0

    .line 204
    new-instance v6, Ljava/lang/String;

    invoke-direct {v6, v2}, Ljava/lang/String;-><init>([C)V

    return-object v6

    .line 181
    :cond_0
    const/4 v3, 0x0

    .line 182
    .local v3, "quad":Z
    const/4 v4, 0x0

    .line 183
    .local v4, "trip":Z
    aget-byte v6, p0, v0

    and-int/lit16 v5, v6, 0xff

    .line 184
    .local v5, "val":I
    shl-int/lit8 v5, v5, 0x8

    .line 185
    add-int/lit8 v6, v0, 0x1

    array-length v8, p0

    if-ge v6, v8, :cond_1

    .line 186
    add-int/lit8 v6, v0, 0x1

    aget-byte v6, p0, v6

    and-int/lit16 v6, v6, 0xff

    or-int/2addr v5, v6

    .line 187
    const/4 v4, 0x1

    .line 189
    :cond_1
    shl-int/lit8 v5, v5, 0x8

    .line 190
    add-int/lit8 v6, v0, 0x2

    array-length v8, p0

    if-ge v6, v8, :cond_2

    .line 191
    add-int/lit8 v6, v0, 0x2

    aget-byte v6, p0, v6

    and-int/lit16 v6, v6, 0xff

    or-int/2addr v5, v6

    .line 192
    const/4 v3, 0x1

    .line 194
    :cond_2
    add-int/lit8 v8, v1, 0x3

    sget-object v9, Lcom/duolebo/tools/shcmcc/DESede;->alphabet:[C

    if-eqz v3, :cond_3

    and-int/lit8 v6, v5, 0x3f

    :goto_1
    aget-char v6, v9, v6

    aput-char v6, v2, v8

    .line 195
    shr-int/lit8 v5, v5, 0x6

    .line 196
    add-int/lit8 v8, v1, 0x2

    sget-object v9, Lcom/duolebo/tools/shcmcc/DESede;->alphabet:[C

    if-eqz v4, :cond_4

    and-int/lit8 v6, v5, 0x3f

    :goto_2
    aget-char v6, v9, v6

    aput-char v6, v2, v8

    .line 197
    shr-int/lit8 v5, v5, 0x6

    .line 198
    add-int/lit8 v6, v1, 0x1

    sget-object v8, Lcom/duolebo/tools/shcmcc/DESede;->alphabet:[C

    and-int/lit8 v9, v5, 0x3f

    aget-char v8, v8, v9

    aput-char v8, v2, v6

    .line 199
    shr-int/lit8 v5, v5, 0x6

    .line 200
    sget-object v6, Lcom/duolebo/tools/shcmcc/DESede;->alphabet:[C

    and-int/lit8 v8, v5, 0x3f

    aget-char v6, v6, v8

    aput-char v6, v2, v1

    .line 201
    add-int/lit8 v0, v0, 0x3

    .line 202
    add-int/lit8 v1, v1, 0x4

    goto :goto_0

    :cond_3
    move v6, v7

    .line 194
    goto :goto_1

    :cond_4
    move v6, v7

    .line 196
    goto :goto_2
.end method

.method public static encrpyt(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .locals 2
    .param p0, "source"    # Ljava/lang/String;
    .param p1, "key"    # Ljava/lang/String;
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    .prologue
    .line 104
    const-string v1, "UTF-8"

    invoke-virtual {p0, v1}, Ljava/lang/String;->getBytes(Ljava/lang/String;)[B

    move-result-object v1

    invoke-static {p1, v1}, Lcom/duolebo/tools/shcmcc/DESede;->DES3Encode(Ljava/lang/String;[B)[B

    move-result-object v0

    .line 105
    .local v0, "bb":[B
    invoke-static {v0}, Lcom/duolebo/tools/shcmcc/DESede;->base64Encode([B)Ljava/lang/String;

    move-result-object v1

    return-object v1
.end method

.method public static encrpytPwd(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .locals 11
    .param p0, "stbid"    # Ljava/lang/String;
    .param p1, "password"    # Ljava/lang/String;

    .prologue
    .line 20
    new-instance v2, Ljava/text/SimpleDateFormat;

    const-string v8, "yyyyMMddHHmmss"

    invoke-direct {v2, v8}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;)V

    .line 21
    .local v2, "format":Ljava/text/DateFormat;
    new-instance v8, Ljava/util/Date;

    invoke-direct {v8}, Ljava/util/Date;-><init>()V

    invoke-virtual {v2, v8}, Ljava/text/DateFormat;->format(Ljava/util/Date;)Ljava/lang/String;

    move-result-object v3

    .line 22
    .local v3, "formatTime":Ljava/lang/String;
    new-instance v8, Ljava/lang/StringBuilder;

    invoke-static {p1}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v9

    invoke-direct {v8, v9}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    const-string v9, "|"

    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v8

    invoke-static {p1}, Lcom/duolebo/tools/shcmcc/DESede;->toMD5(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v9

    invoke-virtual {v9}, Ljava/lang/String;->toUpperCase()Ljava/lang/String;

    move-result-object v9

    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v8

    const-string v9, "|"

    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v8

    invoke-virtual {v8, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v8

    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v7

    .line 23
    .local v7, "source":Ljava/lang/String;
    move-object v5, p0

    .line 24
    .local v5, "key":Ljava/lang/String;
    invoke-virtual {v5}, Ljava/lang/String;->length()I

    move-result v6

    .line 25
    .local v6, "keyLen":I
    const/4 v8, 0x6

    if-ge v6, v8, :cond_0

    .line 26
    const/4 v4, 0x6

    .local v4, "i":I
    :goto_0
    if-gt v4, v6, :cond_1

    .line 30
    .end local v4    # "i":I
    :cond_0
    invoke-virtual {v5}, Ljava/lang/String;->length()I

    move-result v8

    add-int/lit8 v8, v8, -0x6

    invoke-virtual {v5, v8}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v5

    .line 31
    new-instance v8, Ljava/lang/StringBuilder;

    const-string v9, "000000000"

    invoke-direct {v8, v9}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    const-string v9, "[\\W]"

    const-string v10, "0"

    invoke-virtual {v5, v9, v10}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v9

    invoke-virtual {v9}, Ljava/lang/String;->toUpperCase()Ljava/lang/String;

    move-result-object v9

    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v8

    const-string v9, "111111111"

    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v8

    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    .line 33
    const-string v1, ""

    .line 35
    .local v1, "encryptPassStr":Ljava/lang/String;
    :try_start_0
    invoke-static {v7, v5}, Lcom/duolebo/tools/shcmcc/DESede;->encrpyt(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    move-result-object v1

    .line 39
    :goto_1
    return-object v1

    .line 27
    .end local v1    # "encryptPassStr":Ljava/lang/String;
    .restart local v4    # "i":I
    :cond_1
    new-instance v8, Ljava/lang/StringBuilder;

    const-string v9, "0"

    invoke-direct {v8, v9}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v8, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v8

    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    .line 26
    add-int/lit8 v4, v4, -0x1

    goto :goto_0

    .line 36
    .end local v4    # "i":I
    .restart local v1    # "encryptPassStr":Ljava/lang/String;
    :catch_0
    move-exception v0

    .line 37
    .local v0, "e1":Ljava/lang/Exception;
    invoke-virtual {v0}, Ljava/lang/Exception;->printStackTrace()V

    goto :goto_1
.end method

.method public static encrpytUserToken(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .locals 7
    .param p0, "stbid"    # Ljava/lang/String;
    .param p1, "password"    # Ljava/lang/String;

    .prologue
    .line 42
    new-instance v2, Ljava/text/SimpleDateFormat;

    const-string v5, "yyyyMMddHHmmss"

    invoke-direct {v2, v5}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;)V

    .line 43
    .local v2, "format":Ljava/text/DateFormat;
    new-instance v5, Ljava/util/Date;

    invoke-direct {v5}, Ljava/util/Date;-><init>()V

    invoke-virtual {v2, v5}, Ljava/text/DateFormat;->format(Ljava/util/Date;)Ljava/lang/String;

    move-result-object v3

    .line 44
    .local v3, "formatTime":Ljava/lang/String;
    new-instance v5, Ljava/lang/StringBuilder;

    invoke-static {p0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v6

    invoke-direct {v5, v6}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    const-string v6, "|"

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-static {p1}, Lcom/duolebo/tools/shcmcc/DESede;->toMD5(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v6

    invoke-virtual {v6}, Ljava/lang/String;->toUpperCase()Ljava/lang/String;

    move-result-object v6

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v5

    const-string v6, "|"

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    .line 46
    .local v4, "source":Ljava/lang/String;
    const-string v1, ""

    .line 50
    .local v1, "encryptPassStr":Ljava/lang/String;
    :try_start_0
    new-instance v5, Ljava/lang/StringBuilder;

    const-string v6, "userToken"

    invoke-direct {v5, v6}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {v4}, Lcom/duolebo/tools/shcmcc/DESede;->toMD5(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v6

    invoke-virtual {v6}, Ljava/lang/String;->toUpperCase()Ljava/lang/String;

    move-result-object v6

    invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    move-result-object v1

    .line 54
    :goto_0
    return-object v1

    .line 51
    :catch_0
    move-exception v0

    .line 52
    .local v0, "e1":Ljava/lang/Exception;
    invoke-virtual {v0}, Ljava/lang/Exception;->printStackTrace()V

    goto :goto_0
.end method

.method public static encrpytUserToken_old(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .locals 11
    .param p0, "stbid"    # Ljava/lang/String;
    .param p1, "password"    # Ljava/lang/String;

    .prologue
    .line 57
    new-instance v2, Ljava/text/SimpleDateFormat;

    const-string v8, "yyyyMMddHHmmss"

    invoke-direct {v2, v8}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;)V

    .line 58
    .local v2, "format":Ljava/text/DateFormat;
    new-instance v8, Ljava/util/Date;

    invoke-direct {v8}, Ljava/util/Date;-><init>()V

    invoke-virtual {v2, v8}, Ljava/text/DateFormat;->format(Ljava/util/Date;)Ljava/lang/String;

    move-result-object v3

    .line 59
    .local v3, "formatTime":Ljava/lang/String;
    new-instance v8, Ljava/lang/StringBuilder;

    invoke-static {p0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v9

    invoke-direct {v8, v9}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    const-string v9, "|"

    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v8

    invoke-static {p1}, Lcom/duolebo/tools/shcmcc/DESede;->toMD5(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v9

    invoke-virtual {v9}, Ljava/lang/String;->toUpperCase()Ljava/lang/String;

    move-result-object v9

    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v8

    const-string v9, "|"

    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v8

    invoke-virtual {v8, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v8

    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v7

    .line 60
    .local v7, "source":Ljava/lang/String;
    move-object v5, p0

    .line 61
    .local v5, "key":Ljava/lang/String;
    invoke-virtual {v5}, Ljava/lang/String;->length()I

    move-result v6

    .line 62
    .local v6, "keyLen":I
    const/4 v8, 0x6

    if-ge v6, v8, :cond_0

    .line 63
    const/4 v4, 0x6

    .local v4, "i":I
    :goto_0
    if-gt v4, v6, :cond_1

    .line 67
    .end local v4    # "i":I
    :cond_0
    invoke-virtual {v5}, Ljava/lang/String;->length()I

    move-result v8

    add-int/lit8 v8, v8, -0x6

    invoke-virtual {v5, v8}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v5

    .line 68
    new-instance v8, Ljava/lang/StringBuilder;

    const-string v9, "000000000"

    invoke-direct {v8, v9}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    const-string v9, "[\\W]"

    const-string v10, "0"

    invoke-virtual {v5, v9, v10}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v9

    invoke-virtual {v9}, Ljava/lang/String;->toUpperCase()Ljava/lang/String;

    move-result-object v9

    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v8

    const-string v9, "111111111"

    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v8

    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    .line 70
    const-string v1, ""

    .line 72
    .local v1, "encryptPassStr":Ljava/lang/String;
    :try_start_0
    new-instance v8, Ljava/lang/StringBuilder;

    const-string v9, "userToken||"

    invoke-direct {v8, v9}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {v7, v5}, Lcom/duolebo/tools/shcmcc/DESede;->encrpyt(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v9

    invoke-virtual {v8, v9}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v8

    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    move-result-object v1

    .line 76
    :goto_1
    return-object v1

    .line 64
    .end local v1    # "encryptPassStr":Ljava/lang/String;
    .restart local v4    # "i":I
    :cond_1
    new-instance v8, Ljava/lang/StringBuilder;

    const-string v9, "0"

    invoke-direct {v8, v9}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v8, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v8

    invoke-virtual {v8}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    .line 63
    add-int/lit8 v4, v4, -0x1

    goto :goto_0

    .line 73
    .end local v4    # "i":I
    .restart local v1    # "encryptPassStr":Ljava/lang/String;
    :catch_0
    move-exception v0

    .line 74
    .local v0, "e1":Ljava/lang/Exception;
    invoke-virtual {v0}, Ljava/lang/Exception;->printStackTrace()V

    goto :goto_1
.end method

.method public static main([Ljava/lang/String;)V
    .locals 3
    .param p0, "args"    # [Ljava/lang/String;
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    .prologue
    .line 80
    const-string v1, "000000000102A05111111111"

    .line 82
    .local v1, "roamTokenKey":Ljava/lang/String;
    const-string v2, "300982|81A83E7B2EFAB0EFA84C7A544465C8CE"

    .line 81
    invoke-static {v2, v1}, Lcom/duolebo/tools/shcmcc/DESede;->encrpyt(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 83
    .local v0, "encrypt":Ljava/lang/String;
    sget-object v2, Ljava/lang/System;->out:Ljava/io/PrintStream;

    invoke-virtual {v2, v0}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    .line 89
    return-void
.end method

.method public static toMD5(Ljava/lang/String;)Ljava/lang/String;
    .locals 9
    .param p0, "str"    # Ljava/lang/String;

    .prologue
    .line 209
    :try_start_0
    const-string v7, "MD5"

    invoke-static {v7}, Ljava/security/MessageDigest;->getInstance(Ljava/lang/String;)Ljava/security/MessageDigest;

    move-result-object v4

    .line 210
    .local v4, "md":Ljava/security/MessageDigest;
    invoke-virtual {p0}, Ljava/lang/String;->getBytes()[B

    move-result-object v7

    invoke-virtual {v4, v7}, Ljava/security/MessageDigest;->update([B)V

    .line 211
    invoke-virtual {v4}, Ljava/security/MessageDigest;->digest()[B

    move-result-object v0

    .line 212
    .local v0, "buffer":[B
    new-instance v5, Ljava/lang/StringBuffer;

    invoke-direct {v5}, Ljava/lang/StringBuffer;-><init>()V

    .line 213
    .local v5, "sb":Ljava/lang/StringBuffer;
    const/4 v3, 0x0

    .local v3, "i":I
    :goto_0
    array-length v7, v0

    if-lt v3, v7, :cond_0

    .line 218
    invoke-virtual {v5}, Ljava/lang/StringBuffer;->toString()Ljava/lang/String;

    move-result-object p0

    .line 222
    .end local v0    # "buffer":[B
    .end local v3    # "i":I
    .end local v4    # "md":Ljava/security/MessageDigest;
    .end local v5    # "sb":Ljava/lang/StringBuffer;
    .end local p0    # "str":Ljava/lang/String;
    :goto_1
    return-object p0

    .line 214
    .restart local v0    # "buffer":[B
    .restart local v3    # "i":I
    .restart local v4    # "md":Ljava/security/MessageDigest;
    .restart local v5    # "sb":Ljava/lang/StringBuffer;
    .restart local p0    # "str":Ljava/lang/String;
    :cond_0
    aget-byte v7, v0, v3

    and-int/lit16 v6, v7, 0xff

    .line 215
    .local v6, "val":I
    new-instance v7, Ljava/lang/StringBuilder;

    const-string v8, "00"

    invoke-direct {v7, v8}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {v6}, Ljava/lang/Integer;->toHexString(I)Ljava/lang/String;

    move-result-object v8

    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v7

    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    .line 216
    .local v2, "hval":Ljava/lang/String;
    invoke-virtual {v2}, Ljava/lang/String;->length()I

    move-result v7

    add-int/lit8 v7, v7, -0x2

    invoke-virtual {v2, v7}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v7

    invoke-virtual {v5, v7}, Ljava/lang/StringBuffer;->append(Ljava/lang/String;)Ljava/lang/StringBuffer;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    .line 213
    add-int/lit8 v3, v3, 0x1

    goto :goto_0

    .line 219
    .end local v0    # "buffer":[B
    .end local v2    # "hval":Ljava/lang/String;
    .end local v3    # "i":I
    .end local v4    # "md":Ljava/security/MessageDigest;
    .end local v5    # "sb":Ljava/lang/StringBuffer;
    .end local v6    # "val":I
    :catch_0
    move-exception v1

    .line 220
    .local v1, "e":Ljava/lang/Exception;
    invoke-virtual {v1}, Ljava/lang/Exception;->printStackTrace()V

    goto :goto_1
.end method
