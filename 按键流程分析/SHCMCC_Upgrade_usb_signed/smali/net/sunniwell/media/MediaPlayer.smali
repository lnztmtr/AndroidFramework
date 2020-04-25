.class public Lnet/sunniwell/media/MediaPlayer;
.super Landroid/media/MediaPlayer;
.source "MediaPlayer.java"


# instance fields
.field private final ROBOT_PLAYER_PLATFORM_ACTION_INVALID:I

.field private final ROBOT_PLAYER_PLATFORM_GET:I

.field private final ROBOT_PLAYER_PLATFORM_SET:I

.field private final ROBOT_PLAYER_PROPERTY_AUDIO_TRACK:I

.field private final ROBOT_PLAYER_PROPERTY_CA:I

.field private final ROBOT_PLAYER_PROPERTY_DOWNLOAD_DATA_SIZE:I

.field private final ROBOT_PLAYER_PROPERTY_INVALID:I

.field private final ROBOT_PLAYER_PROPERTY_PLAY_RATE:I

.field private final ROBOT_PLAYER_PROPERTY_SUBTITLE:I

.field private final ROBOT_PLAYER_PROPERTY_VIDEO_POSITION:I

.field private log:Lnet/sunniwell/common/log/SWLogger;


# direct methods
.method public constructor <init>()V
    .locals 4

    .prologue
    const/4 v3, 0x2

    const/4 v2, 0x1

    const/4 v1, 0x0

    .line 12
    invoke-direct {p0}, Landroid/media/MediaPlayer;-><init>()V

    .line 13
    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v0

    invoke-static {v0}, Lnet/sunniwell/common/log/SWLogger;->getLogger(Ljava/lang/Class;)Lnet/sunniwell/common/log/SWLogger;

    move-result-object v0

    iput-object v0, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    .line 15
    iput v1, p0, Lnet/sunniwell/media/MediaPlayer;->ROBOT_PLAYER_PROPERTY_AUDIO_TRACK:I

    .line 17
    iput v2, p0, Lnet/sunniwell/media/MediaPlayer;->ROBOT_PLAYER_PROPERTY_PLAY_RATE:I

    .line 20
    iput v3, p0, Lnet/sunniwell/media/MediaPlayer;->ROBOT_PLAYER_PROPERTY_DOWNLOAD_DATA_SIZE:I

    .line 23
    const/4 v0, 0x3

    iput v0, p0, Lnet/sunniwell/media/MediaPlayer;->ROBOT_PLAYER_PROPERTY_VIDEO_POSITION:I

    .line 25
    const/4 v0, 0x4

    iput v0, p0, Lnet/sunniwell/media/MediaPlayer;->ROBOT_PLAYER_PROPERTY_CA:I

    .line 26
    const/4 v0, 0x5

    iput v0, p0, Lnet/sunniwell/media/MediaPlayer;->ROBOT_PLAYER_PROPERTY_SUBTITLE:I

    .line 27
    const/4 v0, 0x6

    iput v0, p0, Lnet/sunniwell/media/MediaPlayer;->ROBOT_PLAYER_PROPERTY_INVALID:I

    .line 28
    iput v1, p0, Lnet/sunniwell/media/MediaPlayer;->ROBOT_PLAYER_PLATFORM_SET:I

    .line 29
    iput v2, p0, Lnet/sunniwell/media/MediaPlayer;->ROBOT_PLAYER_PLATFORM_GET:I

    .line 30
    iput v3, p0, Lnet/sunniwell/media/MediaPlayer;->ROBOT_PLAYER_PLATFORM_ACTION_INVALID:I

    .line 12
    return-void
.end method

.method private callNative(IILjava/lang/String;)Ljava/lang/String;
    .locals 8
    .param p1, "type"    # I
    .param p2, "action"    # I
    .param p3, "url"    # Ljava/lang/String;

    .prologue
    .line 245
    :try_start_0
    iget-object v3, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    new-instance v4, Ljava/lang/StringBuilder;

    const-string v5, "callNative() type="

    invoke-direct {v4, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v4, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v4

    const-string v5, ",action="

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4, p2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v4

    const-string v5, ",url="

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4, p3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    .line 246
    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v3

    const-string v4, "controlRobot"

    const/4 v5, 0x3

    new-array v5, v5, [Ljava/lang/Class;

    const/4 v6, 0x0

    sget-object v7, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    aput-object v7, v5, v6

    const/4 v6, 0x1

    sget-object v7, Ljava/lang/Integer;->TYPE:Ljava/lang/Class;

    aput-object v7, v5, v6

    const/4 v6, 0x2

    const-class v7, Ljava/lang/String;

    aput-object v7, v5, v6

    invoke-virtual {v3, v4, v5}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v1

    .line 247
    .local v1, "method":Ljava/lang/reflect/Method;
    const/4 v3, 0x3

    new-array v3, v3, [Ljava/lang/Object;

    const/4 v4, 0x0

    invoke-static {p1}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v5

    aput-object v5, v3, v4

    const/4 v4, 0x1

    invoke-static {p2}, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;

    move-result-object v5

    aput-object v5, v3, v4

    const/4 v4, 0x2

    aput-object p3, v3, v4

    invoke-virtual {v1, p0, v3}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/lang/String;

    .line 248
    .local v2, "str":Ljava/lang/String;
    iget-object v3, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    new-instance v4, Ljava/lang/StringBuilder;

    const-string v5, "controlRobot return="

    invoke-direct {v4, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    .line 253
    .end local v1    # "method":Ljava/lang/reflect/Method;
    .end local v2    # "str":Ljava/lang/String;
    :goto_0
    return-object v2

    .line 250
    :catch_0
    move-exception v0

    .line 251
    .local v0, "e":Ljava/lang/Exception;
    iget-object v3, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    invoke-virtual {v0}, Ljava/lang/Exception;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Lnet/sunniwell/common/log/SWLogger;->e(Ljava/lang/String;)V

    .line 253
    const/4 v2, 0x0

    goto :goto_0
.end method

.method private toJSON(Ljava/lang/String;)Lorg/json/JSONObject;
    .locals 9
    .param p1, "str"    # Ljava/lang/String;

    .prologue
    const/4 v5, 0x0

    .line 257
    if-nez p1, :cond_0

    .line 258
    const/4 v1, 0x0

    .line 279
    :goto_0
    return-object v1

    .line 259
    :cond_0
    new-instance v1, Lorg/json/JSONObject;

    invoke-direct {v1}, Lorg/json/JSONObject;-><init>()V

    .line 260
    .local v1, "json":Lorg/json/JSONObject;
    const-string v6, ";"

    invoke-virtual {p1, v6}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v3

    .line 261
    .local v3, "strs":[Ljava/lang/String;
    iget-object v6, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    new-instance v7, Ljava/lang/StringBuilder;

    const-string v8, "toJSON() strs.length="

    invoke-direct {v7, v8}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    array-length v8, v3

    invoke-virtual {v7, v8}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v7

    invoke-virtual {v7}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v7

    invoke-virtual {v6, v7}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    .line 262
    array-length v6, v3

    :goto_1
    if-lt v5, v6, :cond_1

    .line 278
    iget-object v5, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    new-instance v6, Ljava/lang/StringBuilder;

    const-string v7, "toJSON() json="

    invoke-direct {v6, v7}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1}, Lorg/json/JSONObject;->toString()Ljava/lang/String;

    move-result-object v7

    invoke-virtual {v6, v7}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v6

    invoke-virtual {v6}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v6

    invoke-virtual {v5, v6}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    goto :goto_0

    .line 262
    :cond_1
    aget-object v2, v3, v5

    .line 263
    .local v2, "s":Ljava/lang/String;
    const-string v7, "="

    invoke-virtual {v2, v7}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v4

    .line 264
    .local v4, "tmp":[Ljava/lang/String;
    array-length v7, v4

    const/4 v8, 0x2

    if-ne v7, v8, :cond_2

    .line 266
    const/4 v7, 0x0

    :try_start_0
    aget-object v7, v4, v7

    const/4 v8, 0x1

    aget-object v8, v4, v8

    invoke-virtual {v1, v7, v8}, Lorg/json/JSONObject;->put(Ljava/lang/String;Ljava/lang/Object;)Lorg/json/JSONObject;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    .line 262
    :cond_2
    :goto_2
    add-int/lit8 v5, v5, 0x1

    goto :goto_1

    .line 267
    :catch_0
    move-exception v0

    .line 268
    .local v0, "e":Ljava/lang/Exception;
    iget-object v7, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    invoke-virtual {v0}, Ljava/lang/Exception;->toString()Ljava/lang/String;

    move-result-object v8

    invoke-virtual {v7, v8}, Lnet/sunniwell/common/log/SWLogger;->e(Ljava/lang/String;)V

    goto :goto_2
.end method


# virtual methods
.method public fbwd()V
    .locals 3

    .prologue
    .line 54
    iget-object v0, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    const-string v1, "===="

    invoke-virtual {v0, v1}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    .line 55
    const/4 v0, 0x1

    const/4 v1, 0x0

    const-string v2, "fbwd"

    invoke-direct {p0, v0, v1, v2}, Lnet/sunniwell/media/MediaPlayer;->callNative(IILjava/lang/String;)Ljava/lang/String;

    .line 56
    return-void
.end method

.method public fbwd(I)V
    .locals 4
    .param p1, "scale"    # I

    .prologue
    .line 64
    iget-object v0, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "====scale="

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    .line 65
    const/4 v0, 0x1

    const/4 v1, 0x0

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v3, "scale=-"

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {p1}, Ljava/lang/Math;->abs(I)I

    move-result v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-direct {p0, v0, v1, v2}, Lnet/sunniwell/media/MediaPlayer;->callNative(IILjava/lang/String;)Ljava/lang/String;

    .line 66
    return-void
.end method

.method public ffwd()V
    .locals 3

    .prologue
    .line 36
    iget-object v0, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    const-string v1, "===="

    invoke-virtual {v0, v1}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    .line 37
    const/4 v0, 0x1

    const/4 v1, 0x0

    const-string v2, "ffwd"

    invoke-direct {p0, v0, v1, v2}, Lnet/sunniwell/media/MediaPlayer;->callNative(IILjava/lang/String;)Ljava/lang/String;

    .line 38
    return-void
.end method

.method public ffwd(I)V
    .locals 4
    .param p1, "scale"    # I

    .prologue
    .line 46
    iget-object v0, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "====scale="

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    .line 47
    const/4 v0, 0x1

    const/4 v1, 0x0

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v3, "scale="

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-static {p1}, Ljava/lang/Math;->abs(I)I

    move-result v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-direct {p0, v0, v1, v2}, Lnet/sunniwell/media/MediaPlayer;->callNative(IILjava/lang/String;)Ljava/lang/String;

    .line 48
    return-void
.end method

.method public getAudio()I
    .locals 5

    .prologue
    const/4 v1, -0x1

    .line 102
    iget-object v2, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    const-string v3, "===="

    invoke-virtual {v2, v3}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    .line 103
    const/4 v2, 0x0

    const/4 v3, 0x1

    const/4 v4, 0x0

    invoke-direct {p0, v2, v3, v4}, Lnet/sunniwell/media/MediaPlayer;->callNative(IILjava/lang/String;)Ljava/lang/String;

    move-result-object v2

    invoke-direct {p0, v2}, Lnet/sunniwell/media/MediaPlayer;->toJSON(Ljava/lang/String;)Lorg/json/JSONObject;

    move-result-object v0

    .line 104
    .local v0, "obj":Lorg/json/JSONObject;
    if-eqz v0, :cond_0

    .line 105
    const-string v2, "index"

    invoke-virtual {v0, v2, v1}, Lorg/json/JSONObject;->optInt(Ljava/lang/String;I)I

    move-result v1

    .line 107
    :cond_0
    return v1
.end method

.method public getAudios()[Ljava/lang/String;
    .locals 7

    .prologue
    const/4 v0, 0x0

    .line 126
    iget-object v4, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    const-string v5, "===="

    invoke-virtual {v4, v5}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    .line 127
    const/4 v4, 0x0

    const/4 v5, 0x1

    invoke-direct {p0, v4, v5, v0}, Lnet/sunniwell/media/MediaPlayer;->callNative(IILjava/lang/String;)Ljava/lang/String;

    move-result-object v4

    invoke-direct {p0, v4}, Lnet/sunniwell/media/MediaPlayer;->toJSON(Ljava/lang/String;)Lorg/json/JSONObject;

    move-result-object v2

    .line 128
    .local v2, "obj":Lorg/json/JSONObject;
    if-eqz v2, :cond_0

    .line 129
    const-string v4, "total"

    const/4 v5, -0x1

    invoke-virtual {v2, v4, v5}, Lorg/json/JSONObject;->optInt(Ljava/lang/String;I)I

    move-result v3

    .line 130
    .local v3, "size":I
    if-lez v3, :cond_0

    .line 131
    new-array v0, v3, [Ljava/lang/String;

    .line 132
    .local v0, "audios":[Ljava/lang/String;
    const/4 v1, 0x0

    .local v1, "i":I
    :goto_0
    if-lt v1, v3, :cond_1

    .line 138
    .end local v0    # "audios":[Ljava/lang/String;
    .end local v1    # "i":I
    .end local v3    # "size":I
    :cond_0
    return-object v0

    .line 133
    .restart local v0    # "audios":[Ljava/lang/String;
    .restart local v1    # "i":I
    .restart local v3    # "size":I
    :cond_1
    new-instance v4, Ljava/lang/StringBuilder;

    const-string v5, "language"

    invoke-direct {v4, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    new-instance v5, Ljava/lang/StringBuilder;

    const-string v6, "unkown"

    invoke-direct {v5, v6}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v5, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v2, v4, v5}, Lorg/json/JSONObject;->optString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v4

    aput-object v4, v0, v1

    .line 132
    add-int/lit8 v1, v1, 0x1

    goto :goto_0
.end method

.method public getPlayRate()I
    .locals 6

    .prologue
    const/4 v5, 0x1

    const/4 v2, 0x0

    .line 84
    iget-object v3, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    const-string v4, "===="

    invoke-virtual {v3, v4}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    .line 85
    const-string v3, "scale="

    invoke-direct {p0, v5, v5, v3}, Lnet/sunniwell/media/MediaPlayer;->callNative(IILjava/lang/String;)Ljava/lang/String;

    move-result-object v1

    .line 86
    .local v1, "rate":Ljava/lang/String;
    if-nez v1, :cond_0

    .line 93
    :goto_0
    return v2

    .line 89
    :cond_0
    :try_start_0
    invoke-static {v1}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    move-result v2

    goto :goto_0

    .line 90
    :catch_0
    move-exception v0

    .line 91
    .local v0, "e":Ljava/lang/Exception;
    invoke-virtual {v0}, Ljava/lang/Exception;->printStackTrace()V

    goto :goto_0
.end method

.method public getSubtitle()I
    .locals 5

    .prologue
    const/4 v1, -0x1

    .line 171
    iget-object v2, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    const-string v3, "===="

    invoke-virtual {v2, v3}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    .line 172
    const/4 v2, 0x5

    const/4 v3, 0x1

    const/4 v4, 0x0

    invoke-direct {p0, v2, v3, v4}, Lnet/sunniwell/media/MediaPlayer;->callNative(IILjava/lang/String;)Ljava/lang/String;

    move-result-object v2

    invoke-direct {p0, v2}, Lnet/sunniwell/media/MediaPlayer;->toJSON(Ljava/lang/String;)Lorg/json/JSONObject;

    move-result-object v0

    .line 173
    .local v0, "obj":Lorg/json/JSONObject;
    if-eqz v0, :cond_0

    .line 174
    const-string v2, "index"

    invoke-virtual {v0, v2, v1}, Lorg/json/JSONObject;->optInt(Ljava/lang/String;I)I

    move-result v1

    .line 176
    :cond_0
    return v1
.end method

.method public getSubtitleLanguage()Ljava/lang/String;
    .locals 4

    .prologue
    const/4 v1, 0x0

    .line 185
    iget-object v2, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    const-string v3, "===="

    invoke-virtual {v2, v3}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    .line 186
    const/4 v2, 0x5

    const/4 v3, 0x1

    invoke-direct {p0, v2, v3, v1}, Lnet/sunniwell/media/MediaPlayer;->callNative(IILjava/lang/String;)Ljava/lang/String;

    move-result-object v2

    invoke-direct {p0, v2}, Lnet/sunniwell/media/MediaPlayer;->toJSON(Ljava/lang/String;)Lorg/json/JSONObject;

    move-result-object v0

    .line 187
    .local v0, "obj":Lorg/json/JSONObject;
    if-eqz v0, :cond_0

    .line 188
    const-string v1, "preferred"

    const-string v2, ""

    invoke-virtual {v0, v1, v2}, Lorg/json/JSONObject;->optString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    .line 190
    :cond_0
    return-object v1
.end method

.method public getSubtitles()[Ljava/lang/String;
    .locals 7

    .prologue
    const/4 v3, 0x0

    .line 219
    iget-object v4, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    const-string v5, "===="

    invoke-virtual {v4, v5}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    .line 220
    const/4 v4, 0x5

    const/4 v5, 0x1

    invoke-direct {p0, v4, v5, v3}, Lnet/sunniwell/media/MediaPlayer;->callNative(IILjava/lang/String;)Ljava/lang/String;

    move-result-object v4

    invoke-direct {p0, v4}, Lnet/sunniwell/media/MediaPlayer;->toJSON(Ljava/lang/String;)Lorg/json/JSONObject;

    move-result-object v1

    .line 221
    .local v1, "obj":Lorg/json/JSONObject;
    if-eqz v1, :cond_0

    .line 222
    const-string v4, "total"

    const/4 v5, -0x1

    invoke-virtual {v1, v4, v5}, Lorg/json/JSONObject;->optInt(Ljava/lang/String;I)I

    move-result v2

    .line 223
    .local v2, "size":I
    if-lez v2, :cond_0

    .line 224
    new-array v3, v2, [Ljava/lang/String;

    .line 225
    .local v3, "subtitles":[Ljava/lang/String;
    const/4 v0, 0x0

    .local v0, "i":I
    :goto_0
    if-lt v0, v2, :cond_1

    .line 231
    .end local v0    # "i":I
    .end local v2    # "size":I
    .end local v3    # "subtitles":[Ljava/lang/String;
    :cond_0
    return-object v3

    .line 226
    .restart local v0    # "i":I
    .restart local v2    # "size":I
    .restart local v3    # "subtitles":[Ljava/lang/String;
    :cond_1
    new-instance v4, Ljava/lang/StringBuilder;

    const-string v5, "language"

    invoke-direct {v4, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v4, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    new-instance v5, Ljava/lang/StringBuilder;

    const-string v6, "unkown"

    invoke-direct {v5, v6}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v5, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v5

    invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v1, v4, v5}, Lorg/json/JSONObject;->optString(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v4

    aput-object v4, v3, v0

    .line 225
    add-int/lit8 v0, v0, 0x1

    goto :goto_0
.end method

.method public setAudio(I)V
    .locals 4
    .param p1, "index"    # I

    .prologue
    const/4 v3, 0x0

    .line 116
    iget-object v0, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "====index="

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    .line 117
    new-instance v0, Ljava/lang/StringBuilder;

    const-string v1, "index="

    invoke-direct {v0, v1}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-direct {p0, v3, v3, v0}, Lnet/sunniwell/media/MediaPlayer;->callNative(IILjava/lang/String;)Ljava/lang/String;

    .line 118
    return-void
.end method

.method public setSubtitle(I)V
    .locals 4
    .param p1, "index"    # I

    .prologue
    .line 199
    iget-object v0, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "====index="

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    .line 200
    const/4 v0, 0x5

    const/4 v1, 0x0

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v3, "index="

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v2, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-direct {p0, v0, v1, v2}, Lnet/sunniwell/media/MediaPlayer;->callNative(IILjava/lang/String;)Ljava/lang/String;

    .line 201
    return-void
.end method

.method public setSubtitle(Ljava/lang/String;)V
    .locals 4
    .param p1, "language"    # Ljava/lang/String;

    .prologue
    .line 209
    iget-object v0, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "====language="

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    .line 210
    const/4 v0, 0x5

    const/4 v1, 0x0

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v3, "language="

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v2, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-direct {p0, v0, v1, v2}, Lnet/sunniwell/media/MediaPlayer;->callNative(IILjava/lang/String;)Ljava/lang/String;

    .line 211
    return-void
.end method

.method public setTrackMode(Z)V
    .locals 4
    .param p1, "b"    # Z

    .prologue
    .line 74
    iget-object v0, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    const-string v1, "===="

    invoke-virtual {v0, v1}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    .line 75
    const/4 v0, 0x1

    const/4 v1, 0x0

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v3, "enable="

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v2, p1}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-direct {p0, v0, v1, v2}, Lnet/sunniwell/media/MediaPlayer;->callNative(IILjava/lang/String;)Ljava/lang/String;

    .line 76
    return-void
.end method

.method public showSubtitle(Z)V
    .locals 4
    .param p1, "b"    # Z

    .prologue
    .line 147
    iget-object v0, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "====b="

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    .line 148
    const/4 v0, 0x5

    const/4 v1, 0x0

    new-instance v2, Ljava/lang/StringBuilder;

    const-string v3, "show="

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v2, p1}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-direct {p0, v0, v1, v2}, Lnet/sunniwell/media/MediaPlayer;->callNative(IILjava/lang/String;)Ljava/lang/String;

    .line 149
    return-void
.end method

.method public subtitleIsShow()Z
    .locals 5

    .prologue
    const/4 v1, 0x0

    .line 157
    iget-object v2, p0, Lnet/sunniwell/media/MediaPlayer;->log:Lnet/sunniwell/common/log/SWLogger;

    const-string v3, "===="

    invoke-virtual {v2, v3}, Lnet/sunniwell/common/log/SWLogger;->d(Ljava/lang/String;)V

    .line 158
    const/4 v2, 0x5

    const/4 v3, 0x1

    const/4 v4, 0x0

    invoke-direct {p0, v2, v3, v4}, Lnet/sunniwell/media/MediaPlayer;->callNative(IILjava/lang/String;)Ljava/lang/String;

    move-result-object v2

    invoke-direct {p0, v2}, Lnet/sunniwell/media/MediaPlayer;->toJSON(Ljava/lang/String;)Lorg/json/JSONObject;

    move-result-object v0

    .line 159
    .local v0, "obj":Lorg/json/JSONObject;
    if-eqz v0, :cond_0

    .line 160
    const-string v2, "show"

    invoke-virtual {v0, v2, v1}, Lorg/json/JSONObject;->optBoolean(Ljava/lang/String;Z)Z

    move-result v1

    .line 162
    :cond_0
    return v1
.end method
