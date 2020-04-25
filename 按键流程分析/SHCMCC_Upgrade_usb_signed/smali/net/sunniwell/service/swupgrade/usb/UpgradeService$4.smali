.class Lnet/sunniwell/service/swupgrade/usb/UpgradeService$4;
.super Landroid/content/BroadcastReceiver;
.source "UpgradeService.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingMethod;
    value = Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->registerReceiverUpdate()V
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
    .line 330
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$4;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    invoke-direct {p0}, Landroid/content/BroadcastReceiver;-><init>()V

    return-void
.end method


# virtual methods
.method public onReceive(Landroid/content/Context;Landroid/content/Intent;)V
    .locals 3
    .param p1, "context"    # Landroid/content/Context;
    .param p2, "intent"    # Landroid/content/Intent;

    .prologue
    .line 334
    invoke-virtual {p2}, Landroid/content/Intent;->getAction()Ljava/lang/String;

    move-result-object v1

    const-string v2, "net.sunniwell.action.UPGRADE"

    invoke-virtual {v1, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_1

    .line 335
    const-string v1, "apk_type"

    invoke-virtual {p2, v1}, Landroid/content/Intent;->getStringExtra(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 336
    .local v0, "usb":Ljava/lang/String;
    const-string v1, "usb"

    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_1

    .line 337
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$4;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    iget-object v1, v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->progressDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    invoke-virtual {v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->isShowing()Z

    move-result v1

    if-eqz v1, :cond_0

    .line 338
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$4;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    iget-object v1, v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->progressDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    invoke-virtual {v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->dismiss()V

    .line 340
    :cond_0
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$4;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    const/4 v2, 0x0

    invoke-static {v2}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object v2

    invoke-static {v1, v2}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->access$102(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;Ljava/lang/Boolean;)Ljava/lang/Boolean;

    .line 341
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$4;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    invoke-virtual {v1}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->delTempFile()V

    .line 342
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$4;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    invoke-virtual {v1}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->finish()V

    .line 345
    .end local v0    # "usb":Ljava/lang/String;
    :cond_1
    return-void
.end method
