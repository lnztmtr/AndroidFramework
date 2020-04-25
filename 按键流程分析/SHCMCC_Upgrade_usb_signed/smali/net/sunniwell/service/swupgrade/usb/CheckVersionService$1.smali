.class Lnet/sunniwell/service/swupgrade/usb/CheckVersionService$1;
.super Landroid/os/Handler;
.source "CheckVersionService.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x0
    name = null
.end annotation


# instance fields
.field final synthetic this$0:Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;


# direct methods
.method constructor <init>(Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;)V
    .locals 0
    .param p1, "this$0"    # Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;

    .prologue
    .line 292
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService$1;->this$0:Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;

    invoke-direct {p0}, Landroid/os/Handler;-><init>()V

    return-void
.end method


# virtual methods
.method public handleMessage(Landroid/os/Message;)V
    .locals 2
    .param p1, "msg"    # Landroid/os/Message;

    .prologue
    .line 294
    iget v0, p1, Landroid/os/Message;->what:I

    packed-switch v0, :pswitch_data_0

    .line 308
    :goto_0
    :pswitch_0
    return-void

    .line 296
    :pswitch_1
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService$1;->this$0:Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;

    const v1, 0x7f050006

    invoke-static {v0, v1}, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->access$000(Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;I)V

    goto :goto_0

    .line 299
    :pswitch_2
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService$1;->this$0:Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;

    const v1, 0x7f050008

    invoke-static {v0, v1}, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->access$000(Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;I)V

    goto :goto_0

    .line 302
    :pswitch_3
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService$1;->this$0:Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;

    const v1, 0x7f050009

    invoke-static {v0, v1}, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->access$000(Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;I)V

    goto :goto_0

    .line 305
    :pswitch_4
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService$1;->this$0:Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;

    const v1, 0x7f05000a

    invoke-static {v0, v1}, Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;->access$000(Lnet/sunniwell/service/swupgrade/usb/CheckVersionService;I)V

    goto :goto_0

    .line 294
    :pswitch_data_0
    .packed-switch 0x2
        :pswitch_1
        :pswitch_0
        :pswitch_2
        :pswitch_3
        :pswitch_4
    .end packed-switch
.end method
