.class Lnet/sunniwell/service/swupgrade/usb/UpgradeService$5;
.super Landroid/content/BroadcastReceiver;
.source "UpgradeService.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingMethod;
    value = Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->registerReceiver()V
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x0
    name = null
.end annotation


# instance fields
.field final synthetic this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;


# direct methods
.method constructor <init>(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)V
    .locals 0
    .param p1, "this$0"    # Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    .prologue
    .line 456
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$5;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    invoke-direct {p0}, Landroid/content/BroadcastReceiver;-><init>()V

    return-void
.end method


# virtual methods
.method public onReceive(Landroid/content/Context;Landroid/content/Intent;)V
    .locals 2
    .param p1, "context"    # Landroid/content/Context;
    .param p2, "intent"    # Landroid/content/Intent;

    .prologue
    .line 458
    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object v0

    const-string v1, "android.intent.action.MEDIA_UNMOUNTED"

    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-nez v0, :cond_0

    .line 459
    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object v0

    const-string v1, "android.intent.action.MEDIA_BAD_REMOVAL"

    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_1

    .line 460
    :cond_0
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$5;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    const/4 v1, 0x0

    invoke-static {v1}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object v1

    invoke-static {v0, v1}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->access$102(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;Ljava/lang/Boolean;)Ljava/lang/Boolean;

    .line 461
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$5;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    invoke-virtual {v0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->delTempFile()V

    .line 462
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$5;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    invoke-virtual {v0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->finish()V

    .line 464
    :cond_1
    return-void
.end method
