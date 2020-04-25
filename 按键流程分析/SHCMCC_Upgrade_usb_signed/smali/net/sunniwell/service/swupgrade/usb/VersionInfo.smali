.class public Lnet/sunniwell/service/swupgrade/usb/VersionInfo;
.super Ljava/lang/Object;
.source "VersionInfo.java"


# instance fields
.field private postBuild:Ljava/lang/String;

.field private postTimestamp:Ljava/lang/String;

.field private preBuild:Ljava/lang/String;

.field private preDevice:Ljava/lang/String;


# direct methods
.method public constructor <init>()V
    .locals 0

    .prologue
    .line 3
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public final getPostBuild()Ljava/lang/String;
    .locals 1

    .prologue
    .line 9
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->postBuild:Ljava/lang/String;

    return-object v0
.end method

.method public final getPostTimestamp()Ljava/lang/String;
    .locals 1

    .prologue
    .line 27
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->postTimestamp:Ljava/lang/String;

    return-object v0
.end method

.method public final getPreBuild()Ljava/lang/String;
    .locals 1

    .prologue
    .line 15
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->preBuild:Ljava/lang/String;

    return-object v0
.end method

.method public final getPreDevice()Ljava/lang/String;
    .locals 1

    .prologue
    .line 21
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->preDevice:Ljava/lang/String;

    return-object v0
.end method

.method public final setPostBuild(Ljava/lang/String;)V
    .locals 0
    .param p1, "postBuild"    # Ljava/lang/String;

    .prologue
    .line 12
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->postBuild:Ljava/lang/String;

    .line 13
    return-void
.end method

.method public final setPostTimestamp(Ljava/lang/String;)V
    .locals 0
    .param p1, "postTimestamp"    # Ljava/lang/String;

    .prologue
    .line 30
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->postTimestamp:Ljava/lang/String;

    .line 31
    return-void
.end method

.method public final setPreBuild(Ljava/lang/String;)V
    .locals 0
    .param p1, "preBuild"    # Ljava/lang/String;

    .prologue
    .line 18
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->preBuild:Ljava/lang/String;

    .line 19
    return-void
.end method

.method public final setPreDevice(Ljava/lang/String;)V
    .locals 0
    .param p1, "preDevice"    # Ljava/lang/String;

    .prologue
    .line 24
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/VersionInfo;->preDevice:Ljava/lang/String;

    .line 25
    return-void
.end method
