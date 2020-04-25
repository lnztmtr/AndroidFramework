.class Lnet/sunniwell/service/swupgrade/usb/Upgrade$3;
.super Landroid/os/Handler;
.source "Upgrade.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lnet/sunniwell/service/swupgrade/usb/Upgrade;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x0
    name = null
.end annotation


# instance fields
.field final synthetic this$0:Lnet/sunniwell/service/swupgrade/usb/Upgrade;


# direct methods
.method constructor <init>(Lnet/sunniwell/service/swupgrade/usb/Upgrade;)V
    .locals 0
    .param p1, "this$0"    # Lnet/sunniwell/service/swupgrade/usb/Upgrade;

    .prologue
    .line 250
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/Upgrade$3;->this$0:Lnet/sunniwell/service/swupgrade/usb/Upgrade;

    invoke-direct {p0}, Landroid/os/Handler;-><init>()V

    return-void
.end method


# virtual methods
.method public handleMessage(Landroid/os/Message;)V
    .locals 1
    .param p1, "msg"    # Landroid/os/Message;

    .prologue
    .line 253
    iget v0, p1, Landroid/os/Message;->what:I

    packed-switch v0, :pswitch_data_0

    .line 261
    :goto_0
    return-void

    .line 255
    :pswitch_0
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/Upgrade$3;->this$0:Lnet/sunniwell/service/swupgrade/usb/Upgrade;

    invoke-static {v0}, Lnet/sunniwell/service/swupgrade/usb/Upgrade;->access$000(Lnet/sunniwell/service/swupgrade/usb/Upgrade;)V

    goto :goto_0

    .line 253
    :pswitch_data_0
    .packed-switch 0x0
        :pswitch_0
    .end packed-switch
.end method
