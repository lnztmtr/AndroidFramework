.class Lnet/sunniwell/service/swupgrade/usb/UpgradeService$1;
.super Ljava/lang/Object;
.source "UpgradeService.java"

# interfaces
.implements Landroid/view/View$OnKeyListener;


# annotations
.annotation system Ldalvik/annotation/EnclosingMethod;
    value = Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->initView()V
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
    .line 144
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$1;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public onKey(Landroid/view/View;ILandroid/view/KeyEvent;)Z
    .locals 2
    .param p1, "view"    # Landroid/view/View;
    .param p2, "keyCode"    # I
    .param p3, "event"    # Landroid/view/KeyEvent;

    .prologue
    .line 146
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "keyCode="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, p2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 148
    const/4 v0, 0x4

    if-ne p2, v0, :cond_0

    .line 150
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$1;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    invoke-virtual {v0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->finish()V

    .line 151
    const/4 v0, 0x1

    .line 153
    :goto_0
    return v0

    :cond_0
    const/4 v0, 0x0

    goto :goto_0
.end method
