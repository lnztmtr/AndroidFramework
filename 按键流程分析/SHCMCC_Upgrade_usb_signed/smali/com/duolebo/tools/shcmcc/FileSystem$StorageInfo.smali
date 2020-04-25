.class public Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;
.super Ljava/lang/Object;
.source "FileSystem.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcom/duolebo/tools/shcmcc/FileSystem;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x9
    name = "StorageInfo"
.end annotation


# static fields
.field public static final FLASH:I = 0x0

.field public static final INTERNAL_SD_CARD:I = 0x1

.field public static final SD_CARD:I = 0x2

.field public static final U_DISK:I = 0x3


# instance fields
.field public final id:I

.field public final number:I

.field public final path:Ljava/lang/String;

.field public final readonly:Z

.field public final removable:Z

.field public final type:I


# direct methods
.method public constructor <init>(Ljava/lang/String;ZZIII)V
    .locals 0
    .param p1, "path"    # Ljava/lang/String;
    .param p2, "readonly"    # Z
    .param p3, "removable"    # Z
    .param p4, "number"    # I
    .param p5, "id"    # I
    .param p6, "type"    # I

    .prologue
    .line 172
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 173
    iput-object p1, p0, Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;->path:Ljava/lang/String;

    .line 174
    iput-boolean p2, p0, Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;->readonly:Z

    .line 175
    iput-boolean p3, p0, Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;->removable:Z

    .line 176
    iput p4, p0, Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;->number:I

    .line 177
    iput p5, p0, Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;->id:I

    .line 178
    iput p6, p0, Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;->type:I

    .line 179
    return-void
.end method


# virtual methods
.method public getAvailSize()J
    .locals 2

    .prologue
    .line 207
    iget-object v0, p0, Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;->path:Ljava/lang/String;

    invoke-static {v0}, Lcom/duolebo/tools/shcmcc/FileSystem;->access$1(Ljava/lang/String;)J

    move-result-wide v0

    return-wide v0
.end method

.method public getDisplayName()Ljava/lang/String;
    .locals 2

    .prologue
    .line 182
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    .line 183
    .local v0, "res":Ljava/lang/StringBuilder;
    iget v1, p0, Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;->type:I

    packed-switch v1, :pswitch_data_0

    .line 196
    :goto_0
    iget-boolean v1, p0, Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;->readonly:Z

    if-eqz v1, :cond_0

    .line 197
    const-string v1, " (Read only)"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 199
    :cond_0
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    return-object v1

    .line 185
    :pswitch_0
    const-string v1, "\u8bbe\u5907\u5b58\u50a8"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    goto :goto_0

    .line 188
    :pswitch_1
    const-string v1, "\u5185\u7f6eSD\u5361"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    goto :goto_0

    .line 191
    :pswitch_2
    const-string v1, "\u5916\u7f6eSD\u5361"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    goto :goto_0

    .line 194
    :pswitch_3
    const-string v1, "U\u76d8"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    goto :goto_0

    .line 183
    :pswitch_data_0
    .packed-switch 0x0
        :pswitch_0
        :pswitch_1
        :pswitch_2
        :pswitch_3
    .end packed-switch
.end method

.method public getTotalSize()J
    .locals 2

    .prologue
    .line 203
    iget-object v0, p0, Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;->path:Ljava/lang/String;

    invoke-static {v0}, Lcom/duolebo/tools/shcmcc/FileSystem;->access$0(Ljava/lang/String;)J

    move-result-wide v0

    return-wide v0
.end method

.method public isFlash()Z
    .locals 1

    .prologue
    .line 211
    iget v0, p0, Lcom/duolebo/tools/shcmcc/FileSystem$StorageInfo;->type:I

    if-nez v0, :cond_0

    const/4 v0, 0x1

    :goto_0
    return v0

    :cond_0
    const/4 v0, 0x0

    goto :goto_0
.end method
