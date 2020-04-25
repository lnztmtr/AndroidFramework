.class final enum Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;
.super Ljava/lang/Enum;
.source "UpgradeActivity.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x4018
    name = "UpgradeFileStatus"
.end annotation

.annotation system Ldalvik/annotation/Signature;
    value = {
        "Ljava/lang/Enum",
        "<",
        "Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;",
        ">;"
    }
.end annotation


# static fields
.field private static final synthetic $VALUES:[Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

.field public static final enum EMPTY_VERSION:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

.field public static final enum INVALID_POSTBUILD:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

.field public static final enum INVALID_VERSION_FORMAT:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

.field public static final enum MISMATCH_HARDWARE:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

.field public static final enum NOTFOUND_METADATA:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

.field public static final enum NOTFOUND_POSTBUILD:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

.field public static final enum SAME_VERSION:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

.field public static final enum UNKNOWN:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;


# instance fields
.field private status:I


# direct methods
.method static constructor <clinit>()V
    .locals 9

    .prologue
    const/4 v8, 0x5

    const/4 v7, 0x4

    const/4 v6, 0x3

    const/4 v5, 0x2

    const/4 v4, 0x1

    .line 56
    new-instance v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    const-string v1, "UNKNOWN"

    const/4 v2, 0x0

    const/4 v3, -0x1

    invoke-direct {v0, v1, v2, v3}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->UNKNOWN:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    .line 57
    new-instance v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    const-string v1, "NOTFOUND_METADATA"

    invoke-direct {v0, v1, v4, v4}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->NOTFOUND_METADATA:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    .line 58
    new-instance v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    const-string v1, "NOTFOUND_POSTBUILD"

    invoke-direct {v0, v1, v5, v5}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->NOTFOUND_POSTBUILD:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    .line 59
    new-instance v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    const-string v1, "INVALID_POSTBUILD"

    invoke-direct {v0, v1, v6, v6}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->INVALID_POSTBUILD:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    .line 60
    new-instance v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    const-string v1, "MISMATCH_HARDWARE"

    invoke-direct {v0, v1, v7, v7}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->MISMATCH_HARDWARE:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    .line 61
    new-instance v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    const-string v1, "INVALID_VERSION_FORMAT"

    invoke-direct {v0, v1, v8, v8}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->INVALID_VERSION_FORMAT:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    .line 62
    new-instance v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    const-string v1, "EMPTY_VERSION"

    const/4 v2, 0x6

    const/4 v3, 0x6

    invoke-direct {v0, v1, v2, v3}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->EMPTY_VERSION:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    .line 63
    new-instance v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    const-string v1, "SAME_VERSION"

    const/4 v2, 0x7

    const/4 v3, 0x7

    invoke-direct {v0, v1, v2, v3}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;-><init>(Ljava/lang/String;II)V

    sput-object v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->SAME_VERSION:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    .line 55
    const/16 v0, 0x8

    new-array v0, v0, [Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    const/4 v1, 0x0

    sget-object v2, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->UNKNOWN:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    aput-object v2, v0, v1

    sget-object v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->NOTFOUND_METADATA:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    aput-object v1, v0, v4

    sget-object v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->NOTFOUND_POSTBUILD:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    aput-object v1, v0, v5

    sget-object v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->INVALID_POSTBUILD:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    aput-object v1, v0, v6

    sget-object v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->MISMATCH_HARDWARE:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    aput-object v1, v0, v7

    sget-object v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->INVALID_VERSION_FORMAT:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    aput-object v1, v0, v8

    const/4 v1, 0x6

    sget-object v2, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->EMPTY_VERSION:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    aput-object v2, v0, v1

    const/4 v1, 0x7

    sget-object v2, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->SAME_VERSION:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    aput-object v2, v0, v1

    sput-object v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->$VALUES:[Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    return-void
.end method

.method private constructor <init>(Ljava/lang/String;II)V
    .locals 1
    .param p3, "status"    # I
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(I)V"
        }
    .end annotation

    .prologue
    .line 67
    invoke-direct {p0, p1, p2}, Ljava/lang/Enum;-><init>(Ljava/lang/String;I)V

    .line 65
    const/4 v0, -0x1

    iput v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->status:I

    .line 68
    iput p3, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->status:I

    .line 69
    return-void
.end method

.method public static valueOf(Ljava/lang/String;)Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;
    .locals 1
    .param p0, "name"    # Ljava/lang/String;

    .prologue
    .line 55
    const-class v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    invoke-static {v0, p0}, Ljava/lang/Enum;->valueOf(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;

    move-result-object v0

    check-cast v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    return-object v0
.end method

.method public static values()[Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;
    .locals 1

    .prologue
    .line 55
    sget-object v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->$VALUES:[Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    invoke-virtual {v0}, [Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;->clone()Ljava/lang/Object;

    move-result-object v0

    check-cast v0, [Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity$UpgradeFileStatus;

    return-object v0
.end method
