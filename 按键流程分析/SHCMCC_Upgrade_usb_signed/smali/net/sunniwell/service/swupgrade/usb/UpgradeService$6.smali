.class Lnet/sunniwell/service/swupgrade/usb/UpgradeService$6;
.super Landroid/os/Handler;
.source "UpgradeService.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lnet/sunniwell/service/swupgrade/usb/UpgradeService;
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
    .line 509
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$6;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    invoke-direct {p0}, Landroid/os/Handler;-><init>()V

    return-void
.end method


# virtual methods
.method public handleMessage(Landroid/os/Message;)V
    .locals 4
    .param p1, "msg"    # Landroid/os/Message;

    .prologue
    const/16 v3, 0x64

    .line 511
    iget v1, p1, Landroid/os/Message;->what:I

    packed-switch v1, :pswitch_data_0

    .line 543
    :cond_0
    :goto_0
    :pswitch_0
    return-void

    .line 513
    :pswitch_1
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$6;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    const/4 v2, 0x0

    invoke-static {v1, v2}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->access$302(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;Z)Z

    goto :goto_0

    .line 517
    :pswitch_2
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$6;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    invoke-static {v1}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->access$400(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)V

    goto :goto_0

    .line 520
    :pswitch_3
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "============start============="

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    iget-object v2, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$6;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    invoke-static {v2}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->access$500(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 521
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$6;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    iget-object v2, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$6;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    invoke-static {v2}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->access$500(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)Ljava/lang/String;

    move-result-object v2

    invoke-static {v1, v2}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->access$600(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;Ljava/lang/String;)V

    goto :goto_0

    .line 524
    :pswitch_4
    iget v0, p1, Landroid/os/Message;->arg1:I

    .line 525
    .local v0, "per":I
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$6;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    invoke-static {v1}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->access$700(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)Landroid/widget/ProgressBar;

    move-result-object v1

    invoke-virtual {v1, v0}, Landroid/widget/ProgressBar;->setProgress(I)V

    .line 526
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "...copy...percent="

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 527
    if-lt v0, v3, :cond_1

    .line 528
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "...copy.100..percent="

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 530
    :cond_1
    if-ge v0, v3, :cond_0

    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$6;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    invoke-static {v1}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->access$100(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)Ljava/lang/Boolean;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v1

    if-nez v1, :cond_0

    .line 531
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$6;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    iget-object v2, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$6;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    .line 532
    invoke-virtual {v2}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->getResources()Landroid/content/res/Resources;

    move-result-object v2

    const v3, 0x7f050010

    invoke-virtual {v2, v3}, Landroid/content/res/Resources;->getText(I)Ljava/lang/CharSequence;

    move-result-object v2

    const/16 v3, 0xbb8

    .line 531
    invoke-static {v1, v2, v3}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;

    move-result-object v1

    .line 532
    invoke-virtual {v1}, Landroid/widget/Toast;->show()V

    goto/16 :goto_0

    .line 536
    .end local v0    # "per":I
    :pswitch_5
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$6;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    iget-object v1, v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->progressDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    invoke-virtual {v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->dismiss()V

    .line 537
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$6;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    invoke-static {v1}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->access$800(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)V

    goto/16 :goto_0

    .line 540
    :pswitch_6
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$6;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    invoke-static {v1}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->access$900(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)V

    goto/16 :goto_0

    .line 511
    nop

    :pswitch_data_0
    .packed-switch 0x0
        :pswitch_1
        :pswitch_2
        :pswitch_3
        :pswitch_4
        :pswitch_5
        :pswitch_0
        :pswitch_6
    .end packed-switch
.end method
