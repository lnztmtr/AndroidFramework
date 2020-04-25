.class final enum Lnet/sunniwell/common/log/SWLogger$Level;
.super Ljava/lang/Enum;
.source "SWLogger.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lnet/sunniwell/common/log/SWLogger;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x4018
    name = "Level"
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Enum",
        "<",
        "Lnet/sunniwell/common/log/SWLogger$Level;",
        ">;"
    }
.end annotation


# static fields
.field private static final synthetic ENUM$VALUES:[Lnet/sunniwell/common/log/SWLogger$Level;

.field public static final enum debug:Lnet/sunniwell/common/log/SWLogger$Level;

.field public static final enum error:Lnet/sunniwell/common/log/SWLogger$Level;

.field public static final enum info:Lnet/sunniwell/common/log/SWLogger$Level;

.field public static final enum none:Lnet/sunniwell/common/log/SWLogger$Level;

.field public static final enum warn:Lnet/sunniwell/common/log/SWLogger$Level;


# instance fields
.field intValue:I


# direct methods
.method static constructor <clinit>()V
    .locals 8

    .prologue
    const/4 v7, 0x2

    const/4 v6, 0x1

    const/4 v5, 0x0

    const/4 v4, 0x4

    const/4 v3, 0x3

    .line 179
    new-instance v0, Lnet/sunniwell/common/log/SWLogger$Level;

    const-string v1, "debug"

    invoke-direct {v0, v1, v5, v3}, Lnet/sunniwell/common/log/SWLogger$Level;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lnet/sunniwell/common/log/SWLogger$Level;->debug:Lnet/sunniwell/common/log/SWLogger$Level;

    new-instance v0, Lnet/sunniwell/common/log/SWLogger$Level;

    const-string v1, "info"

    invoke-direct {v0, v1, v6, v4}, Lnet/sunniwell/common/log/SWLogger$Level;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lnet/sunniwell/common/log/SWLogger$Level;->info:Lnet/sunniwell/common/log/SWLogger$Level;

    new-instance v0, Lnet/sunniwell/common/log/SWLogger$Level;

    const-string v1, "warn"

    const/4 v2, 0x5

    invoke-direct {v0, v1, v7, v2}, Lnet/sunniwell/common/log/SWLogger$Level;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lnet/sunniwell/common/log/SWLogger$Level;->warn:Lnet/sunniwell/common/log/SWLogger$Level;

    new-instance v0, Lnet/sunniwell/common/log/SWLogger$Level;

    const-string v1, "error"

    const/4 v2, 0x6

    invoke-direct {v0, v1, v3, v2}, Lnet/sunniwell/common/log/SWLogger$Level;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lnet/sunniwell/common/log/SWLogger$Level;->error:Lnet/sunniwell/common/log/SWLogger$Level;

    new-instance v0, Lnet/sunniwell/common/log/SWLogger$Level;

    const-string v1, "none"

    const/4 v2, 0x7

    invoke-direct {v0, v1, v4, v2}, Lnet/sunniwell/common/log/SWLogger$Level;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lnet/sunniwell/common/log/SWLogger$Level;->none:Lnet/sunniwell/common/log/SWLogger$Level;

    .line 178
    const/4 v0, 0x5

    new-array v0, v0, [Lnet/sunniwell/common/log/SWLogger$Level;

    sget-object v1, Lnet/sunniwell/common/log/SWLogger$Level;->debug:Lnet/sunniwell/common/log/SWLogger$Level;

    aput-object v1, v0, v5

    sget-object v1, Lnet/sunniwell/common/log/SWLogger$Level;->info:Lnet/sunniwell/common/log/SWLogger$Level;

    aput-object v1, v0, v6

    sget-object v1, Lnet/sunniwell/common/log/SWLogger$Level;->warn:Lnet/sunniwell/common/log/SWLogger$Level;

    aput-object v1, v0, v7

    sget-object v1, Lnet/sunniwell/common/log/SWLogger$Level;->error:Lnet/sunniwell/common/log/SWLogger$Level;

    aput-object v1, v0, v3

    sget-object v1, Lnet/sunniwell/common/log/SWLogger$Level;->none:Lnet/sunniwell/common/log/SWLogger$Level;

    aput-object v1, v0, v4

    sput-object v0, Lnet/sunniwell/common/log/SWLogger$Level;->ENUM$VALUES:[Lnet/sunniwell/common/log/SWLogger$Level;

    return-void
.end method

.method private constructor <init>(Ljava/lang/String;II)V
    .locals 0
    .param p3, "intValue"    # I

    .prologue
    .line 183
    invoke-direct {p0, p1, p2}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    .line 184
    iput p3, p0, Lnet/sunniwell/common/log/SWLogger$Level;->intValue:I

    .line 185
    return-void
.end method

.method public static getLevel(I)Lnet/sunniwell/common/log/SWLogger$Level;
    .locals 4
    .param p0, "intValue"    # I

    .prologue
    .line 199
    invoke-static {}, Lnet/sunniwell/common/log/SWLogger$Level;->values()[Lnet/sunniwell/common/log/SWLogger$Level;

    move-result-object v2

    .line 200
    .local v2, "levels":[Lnet/sunniwell/common/log/SWLogger$Level;
    const/4 v0, 0x0

    .local v0, "i":I
    :goto_0
    array-length v3, v2

    if-lt v0, v3, :cond_1

    .line 205
    const/4 v1, 0x0

    :cond_0
    return-object v1

    .line 201
    :cond_1
    aget-object v1, v2, v0

    .line 202
    .local v1, "l":Lnet/sunniwell/common/log/SWLogger$Level;
    iget v3, v1, Lnet/sunniwell/common/log/SWLogger$Level;->intValue:I

    if-eq v3, p0, :cond_0

    .line 200
    add-int/lit8 v0, v0, 0x1

    goto :goto_0
.end method

.method public static getLevel(Ljava/lang/String;)Lnet/sunniwell/common/log/SWLogger$Level;
    .locals 3
    .param p0, "key"    # Ljava/lang/String;

    .prologue
    const/4 v1, 0x0

    .line 188
    if-nez p0, :cond_0

    .line 195
    :goto_0
    return-object v1

    .line 191
    :cond_0
    :try_start_0
    invoke-virtual {p0}, Ljava/lang/String;->toLowerCase()Ljava/lang/String;

    move-result-object v2

    invoke-static {v2}, Lnet/sunniwell/common/log/SWLogger$Level;->valueOf(Ljava/lang/String;)Lnet/sunniwell/common/log/SWLogger$Level;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    move-result-object v1

    goto :goto_0

    .line 192
    :catch_0
    move-exception v0

    .line 193
    .local v0, "e":Ljava/lang/Exception;
    invoke-virtual {v0}, Ljava/lang/Exception;->printStackTrace()V

    goto :goto_0
.end method

.method public static valueOf(Ljava/lang/String;)Lnet/sunniwell/common/log/SWLogger$Level;
    .locals 1

    .prologue
    .line 1
    const-class v0, Lnet/sunniwell/common/log/SWLogger$Level;

    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;

    move-result-object v0

    check-cast v0, Lnet/sunniwell/common/log/SWLogger$Level;

    return-object v0
.end method

.method public static values()[Lnet/sunniwell/common/log/SWLogger$Level;
    .locals 4

    .prologue
    const/4 v3, 0x0

    .line 1
    sget-object v0, Lnet/sunniwell/common/log/SWLogger$Level;->ENUM$VALUES:[Lnet/sunniwell/common/log/SWLogger$Level;

    array-length v1, v0

    new-array v2, v1, [Lnet/sunniwell/common/log/SWLogger$Level;

    invoke-static {v0, v3, v2, v3, v1}, Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V

    return-object v2
.end method
