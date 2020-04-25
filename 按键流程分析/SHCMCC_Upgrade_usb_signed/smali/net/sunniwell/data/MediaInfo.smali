.class public Lnet/sunniwell/data/MediaInfo;
.super Ljava/lang/Object;
.source "MediaInfo.java"

# interfaces
.implements Landroid/os/Parcelable;


# static fields
.field public static final CREATOR:Landroid/os/Parcelable$Creator;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Landroid/os/Parcelable$Creator",
            "<",
            "Lnet/sunniwell/data/MediaInfo;",
            ">;"
        }
    .end annotation
.end field


# instance fields
.field private album:Ljava/lang/String;

.field private artist:Ljava/lang/String;

.field private bookmark:I

.field private date_taken:Ljava/lang/String;

.field private description:Ljava/lang/String;

.field private duration:I

.field private height:I

.field private mime_type:Ljava/lang/String;

.field private name:Ljava/lang/String;

.field private orientaition:I

.field private size:I

.field private thumbnails_data:[B

.field private thumbnails_height:I

.field private thumbnails_kind:I

.field private thumbnails_url:Ljava/lang/String;

.field private thumbnails_width:I

.field private title:Ljava/lang/String;

.field private url:Ljava/lang/String;

.field private width:I

.field private year:I


# direct methods
.method static constructor <clinit>()V
    .locals 1

    .prologue
    .line 461
    new-instance v0, Lnet/sunniwell/data/MediaInfo$1;

    invoke-direct {v0}, Lnet/sunniwell/data/MediaInfo$1;-><init>()V

    sput-object v0, Lnet/sunniwell/data/MediaInfo;->CREATOR:Landroid/os/Parcelable$Creator;

    .line 16
    return-void
.end method

.method public constructor <init>()V
    .locals 0

    .prologue
    .line 16
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public describeContents()I
    .locals 1

    .prologue
    .line 431
    const/4 v0, 0x0

    return v0
.end method

.method public final getAlbum()Ljava/lang/String;
    .locals 1

    .prologue
    .line 184
    iget-object v0, p0, Lnet/sunniwell/data/MediaInfo;->album:Ljava/lang/String;

    return-object v0
.end method

.method public final getArtist()Ljava/lang/String;
    .locals 1

    .prologue
    .line 202
    iget-object v0, p0, Lnet/sunniwell/data/MediaInfo;->artist:Ljava/lang/String;

    return-object v0
.end method

.method public final getBookmark()I
    .locals 1

    .prologue
    .line 220
    iget v0, p0, Lnet/sunniwell/data/MediaInfo;->bookmark:I

    return v0
.end method

.method public final getDate_taken()Ljava/lang/String;
    .locals 1

    .prologue
    .line 256
    iget-object v0, p0, Lnet/sunniwell/data/MediaInfo;->date_taken:Ljava/lang/String;

    return-object v0
.end method

.method public final getDescription()Ljava/lang/String;
    .locals 1

    .prologue
    .line 166
    iget-object v0, p0, Lnet/sunniwell/data/MediaInfo;->description:Ljava/lang/String;

    return-object v0
.end method

.method public final getDuration()I
    .locals 1

    .prologue
    .line 238
    iget v0, p0, Lnet/sunniwell/data/MediaInfo;->duration:I

    return v0
.end method

.method public final getHeight()I
    .locals 1

    .prologue
    .line 364
    iget v0, p0, Lnet/sunniwell/data/MediaInfo;->height:I

    return v0
.end method

.method public final getMime_type()Ljava/lang/String;
    .locals 1

    .prologue
    .line 148
    iget-object v0, p0, Lnet/sunniwell/data/MediaInfo;->mime_type:Ljava/lang/String;

    return-object v0
.end method

.method public final getName()Ljava/lang/String;
    .locals 1

    .prologue
    .line 76
    iget-object v0, p0, Lnet/sunniwell/data/MediaInfo;->name:Ljava/lang/String;

    return-object v0
.end method

.method public final getOrientaition()I
    .locals 1

    .prologue
    .line 400
    iget v0, p0, Lnet/sunniwell/data/MediaInfo;->orientaition:I

    return v0
.end method

.method public final getSize()I
    .locals 1

    .prologue
    .line 112
    iget v0, p0, Lnet/sunniwell/data/MediaInfo;->size:I

    return v0
.end method

.method public final getThumbnails_data()[B
    .locals 1

    .prologue
    .line 418
    iget-object v0, p0, Lnet/sunniwell/data/MediaInfo;->thumbnails_data:[B

    return-object v0
.end method

.method public final getThumbnails_height()I
    .locals 1

    .prologue
    .line 310
    iget v0, p0, Lnet/sunniwell/data/MediaInfo;->thumbnails_height:I

    return v0
.end method

.method public final getThumbnails_kind()I
    .locals 1

    .prologue
    .line 328
    iget v0, p0, Lnet/sunniwell/data/MediaInfo;->thumbnails_kind:I

    return v0
.end method

.method public final getThumbnails_url()Ljava/lang/String;
    .locals 1

    .prologue
    .line 274
    iget-object v0, p0, Lnet/sunniwell/data/MediaInfo;->thumbnails_url:Ljava/lang/String;

    return-object v0
.end method

.method public final getThumbnails_width()I
    .locals 1

    .prologue
    .line 292
    iget v0, p0, Lnet/sunniwell/data/MediaInfo;->thumbnails_width:I

    return v0
.end method

.method public final getTitle()Ljava/lang/String;
    .locals 1

    .prologue
    .line 130
    iget-object v0, p0, Lnet/sunniwell/data/MediaInfo;->title:Ljava/lang/String;

    return-object v0
.end method

.method public final getUrl()Ljava/lang/String;
    .locals 1

    .prologue
    .line 94
    iget-object v0, p0, Lnet/sunniwell/data/MediaInfo;->url:Ljava/lang/String;

    return-object v0
.end method

.method public final getWidth()I
    .locals 1

    .prologue
    .line 346
    iget v0, p0, Lnet/sunniwell/data/MediaInfo;->width:I

    return v0
.end method

.method public final getYear()I
    .locals 1

    .prologue
    .line 382
    iget v0, p0, Lnet/sunniwell/data/MediaInfo;->year:I

    return v0
.end method

.method public final setAlbum(Ljava/lang/String;)V
    .locals 0
    .param p1, "album"    # Ljava/lang/String;

    .prologue
    .line 193
    iput-object p1, p0, Lnet/sunniwell/data/MediaInfo;->album:Ljava/lang/String;

    .line 194
    return-void
.end method

.method public final setArtist(Ljava/lang/String;)V
    .locals 0
    .param p1, "artist"    # Ljava/lang/String;

    .prologue
    .line 211
    iput-object p1, p0, Lnet/sunniwell/data/MediaInfo;->artist:Ljava/lang/String;

    .line 212
    return-void
.end method

.method public final setBookmark(I)V
    .locals 0
    .param p1, "bookmark"    # I

    .prologue
    .line 229
    iput p1, p0, Lnet/sunniwell/data/MediaInfo;->bookmark:I

    .line 230
    return-void
.end method

.method public final setDate_taken(Ljava/lang/String;)V
    .locals 0
    .param p1, "date_taken"    # Ljava/lang/String;

    .prologue
    .line 265
    iput-object p1, p0, Lnet/sunniwell/data/MediaInfo;->date_taken:Ljava/lang/String;

    .line 266
    return-void
.end method

.method public final setDescription(Ljava/lang/String;)V
    .locals 0
    .param p1, "description"    # Ljava/lang/String;

    .prologue
    .line 175
    iput-object p1, p0, Lnet/sunniwell/data/MediaInfo;->description:Ljava/lang/String;

    .line 176
    return-void
.end method

.method public final setDuration(I)V
    .locals 0
    .param p1, "duration"    # I

    .prologue
    .line 247
    iput p1, p0, Lnet/sunniwell/data/MediaInfo;->duration:I

    .line 248
    return-void
.end method

.method public final setHeight(I)V
    .locals 0
    .param p1, "height"    # I

    .prologue
    .line 373
    iput p1, p0, Lnet/sunniwell/data/MediaInfo;->height:I

    .line 374
    return-void
.end method

.method public final setMime_type(Ljava/lang/String;)V
    .locals 0
    .param p1, "mime_type"    # Ljava/lang/String;

    .prologue
    .line 157
    iput-object p1, p0, Lnet/sunniwell/data/MediaInfo;->mime_type:Ljava/lang/String;

    .line 158
    return-void
.end method

.method public final setName(Ljava/lang/String;)V
    .locals 0
    .param p1, "name"    # Ljava/lang/String;

    .prologue
    .line 85
    iput-object p1, p0, Lnet/sunniwell/data/MediaInfo;->name:Ljava/lang/String;

    .line 86
    return-void
.end method

.method public final setOrientaition(I)V
    .locals 0
    .param p1, "orientaition"    # I

    .prologue
    .line 409
    iput p1, p0, Lnet/sunniwell/data/MediaInfo;->orientaition:I

    .line 410
    return-void
.end method

.method public final setSize(I)V
    .locals 0
    .param p1, "size"    # I

    .prologue
    .line 121
    iput p1, p0, Lnet/sunniwell/data/MediaInfo;->size:I

    .line 122
    return-void
.end method

.method public final setThumbnails_data([B)V
    .locals 0
    .param p1, "thumbnails_data"    # [B

    .prologue
    .line 427
    iput-object p1, p0, Lnet/sunniwell/data/MediaInfo;->thumbnails_data:[B

    .line 428
    return-void
.end method

.method public final setThumbnails_height(I)V
    .locals 0
    .param p1, "thumbnails_height"    # I

    .prologue
    .line 319
    iput p1, p0, Lnet/sunniwell/data/MediaInfo;->thumbnails_height:I

    .line 320
    return-void
.end method

.method public final setThumbnails_kind(I)V
    .locals 0
    .param p1, "thumbnails_kind"    # I

    .prologue
    .line 337
    iput p1, p0, Lnet/sunniwell/data/MediaInfo;->thumbnails_kind:I

    .line 338
    return-void
.end method

.method public final setThumbnails_url(Ljava/lang/String;)V
    .locals 0
    .param p1, "thumbnails_url"    # Ljava/lang/String;

    .prologue
    .line 283
    iput-object p1, p0, Lnet/sunniwell/data/MediaInfo;->thumbnails_url:Ljava/lang/String;

    .line 284
    return-void
.end method

.method public final setThumbnails_width(I)V
    .locals 0
    .param p1, "thumbnails_width"    # I

    .prologue
    .line 301
    iput p1, p0, Lnet/sunniwell/data/MediaInfo;->thumbnails_width:I

    .line 302
    return-void
.end method

.method public final setTitle(Ljava/lang/String;)V
    .locals 0
    .param p1, "title"    # Ljava/lang/String;

    .prologue
    .line 139
    iput-object p1, p0, Lnet/sunniwell/data/MediaInfo;->title:Ljava/lang/String;

    .line 140
    return-void
.end method

.method public final setUrl(Ljava/lang/String;)V
    .locals 0
    .param p1, "url"    # Ljava/lang/String;

    .prologue
    .line 103
    iput-object p1, p0, Lnet/sunniwell/data/MediaInfo;->url:Ljava/lang/String;

    .line 104
    return-void
.end method

.method public final setWidth(I)V
    .locals 0
    .param p1, "width"    # I

    .prologue
    .line 355
    iput p1, p0, Lnet/sunniwell/data/MediaInfo;->width:I

    .line 356
    return-void
.end method

.method public final setYear(I)V
    .locals 0
    .param p1, "year"    # I

    .prologue
    .line 391
    iput p1, p0, Lnet/sunniwell/data/MediaInfo;->year:I

    .line 392
    return-void
.end method

.method public writeToParcel(Landroid/os/Parcel;I)V
    .locals 8
    .param p1, "dest"    # Landroid/os/Parcel;
    .param p2, "flags"    # I

    .prologue
    .line 435
    new-instance v3, Landroid/os/Bundle;

    invoke-direct {v3}, Landroid/os/Bundle;-><init>()V

    .line 436
    .local v3, "mBundle":Landroid/os/Bundle;
    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/Class;->getDeclaredFields()[Ljava/lang/reflect/Field;

    move-result-object v2

    .line 438
    .local v2, "fields":[Ljava/lang/reflect/Field;
    array-length v6, v2

    const/4 v4, 0x0

    move v5, v4

    :goto_0
    if-lt v5, v6, :cond_0

    .line 458
    invoke-virtual {p1, v3}, Landroid/os/Parcel;->writeBundle(Landroid/os/Bundle;)V

    .line 459
    return-void

    .line 438
    :cond_0
    aget-object v1, v2, v5

    .line 441
    .local v1, "f":Ljava/lang/reflect/Field;
    :try_start_0
    invoke-virtual {v1}, Ljava/lang/reflect/Field;->getType()Ljava/lang/Class;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/Class;->getSimpleName()Ljava/lang/String;

    move-result-object v4

    const-string v7, "String"

    invoke-virtual {v4, v7}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v4

    if-eqz v4, :cond_2

    .line 442
    invoke-virtual {v1}, Ljava/lang/reflect/Field;->getName()Ljava/lang/String;

    move-result-object v7

    invoke-virtual {v1, p0}, Ljava/lang/reflect/Field;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, Ljava/lang/String;

    invoke-virtual {v3, v7, v4}, Landroid/os/Bundle;->putString(Ljava/lang/String;Ljava/lang/String;)V

    .line 438
    :cond_1
    :goto_1
    add-int/lit8 v4, v5, 0x1

    move v5, v4

    goto :goto_0

    .line 443
    :cond_2
    invoke-virtual {v1}, Ljava/lang/reflect/Field;->getType()Ljava/lang/Class;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/Class;->getSimpleName()Ljava/lang/String;

    move-result-object v4

    const-string v7, "Boolean"

    invoke-virtual {v4, v7}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v4

    if-eqz v4, :cond_3

    .line 444
    invoke-virtual {v1}, Ljava/lang/reflect/Field;->getName()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v1, p0}, Ljava/lang/reflect/Field;->getBoolean(Ljava/lang/Object;)Z

    move-result v7

    invoke-virtual {v3, v4, v7}, Landroid/os/Bundle;->putBoolean(Ljava/lang/String;Z)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_1

    .line 454
    :catch_0
    move-exception v0

    .line 455
    .local v0, "e":Ljava/lang/Exception;
    invoke-virtual {v0}, Ljava/lang/Exception;->printStackTrace()V

    goto :goto_1

    .line 445
    .end local v0    # "e":Ljava/lang/Exception;
    :cond_3
    :try_start_1
    invoke-virtual {v1}, Ljava/lang/reflect/Field;->getType()Ljava/lang/Class;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/Class;->getSimpleName()Ljava/lang/String;

    move-result-object v4

    const-string v7, "int[]"

    invoke-virtual {v4, v7}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v4

    if-eqz v4, :cond_4

    .line 446
    invoke-virtual {v1}, Ljava/lang/reflect/Field;->getName()Ljava/lang/String;

    move-result-object v7

    invoke-virtual {v1, p0}, Ljava/lang/reflect/Field;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, [I

    invoke-virtual {v3, v7, v4}, Landroid/os/Bundle;->putIntArray(Ljava/lang/String;[I)V

    goto :goto_1

    .line 447
    :cond_4
    invoke-virtual {v1}, Ljava/lang/reflect/Field;->getType()Ljava/lang/Class;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/Class;->getSimpleName()Ljava/lang/String;

    move-result-object v4

    const-string v7, "int"

    invoke-virtual {v4, v7}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v4

    if-eqz v4, :cond_5

    .line 448
    invoke-virtual {v1}, Ljava/lang/reflect/Field;->getName()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v1, p0}, Ljava/lang/reflect/Field;->getInt(Ljava/lang/Object;)I

    move-result v7

    invoke-virtual {v3, v4, v7}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V

    goto :goto_1

    .line 449
    :cond_5
    invoke-virtual {v1}, Ljava/lang/reflect/Field;->getType()Ljava/lang/Class;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/Class;->getSimpleName()Ljava/lang/String;

    move-result-object v4

    const-string v7, "byte[]"

    invoke-virtual {v4, v7}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v4

    if-eqz v4, :cond_6

    .line 450
    invoke-virtual {v1}, Ljava/lang/reflect/Field;->getName()Ljava/lang/String;

    move-result-object v7

    invoke-virtual {v1, p0}, Ljava/lang/reflect/Field;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    check-cast v4, [B

    invoke-virtual {v3, v7, v4}, Landroid/os/Bundle;->putByteArray(Ljava/lang/String;[B)V

    goto :goto_1

    .line 451
    :cond_6
    invoke-virtual {v1}, Ljava/lang/reflect/Field;->getType()Ljava/lang/Class;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/Class;->getSimpleName()Ljava/lang/String;

    move-result-object v4

    const-string v7, "byte"

    invoke-virtual {v4, v7}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v4

    if-eqz v4, :cond_1

    .line 452
    invoke-virtual {v1}, Ljava/lang/reflect/Field;->getName()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v1, p0}, Ljava/lang/reflect/Field;->getByte(Ljava/lang/Object;)B

    move-result v7

    invoke-virtual {v3, v4, v7}, Landroid/os/Bundle;->putByte(Ljava/lang/String;B)V
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_0

    goto/16 :goto_1
.end method
