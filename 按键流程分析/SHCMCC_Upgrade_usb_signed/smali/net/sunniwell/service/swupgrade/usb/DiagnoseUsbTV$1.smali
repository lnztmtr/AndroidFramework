.class Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV$1;
.super Ljava/lang/Object;
.source "DiagnoseUsbTV.java"

# interfaces
.implements Landroid/content/ServiceConnection;


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x0
    name = null
.end annotation


# instance fields
.field final synthetic this$0:Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;


# direct methods
.method constructor <init>(Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;)V
    .locals 0
    .param p1, "this$0"    # Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;

    .prologue
    .line 36
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV$1;->this$0:Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public onServiceConnected(Landroid/content/ComponentName;Landroid/os/IBinder;)V
    .locals 2
    .param p1, "name"    # Landroid/content/ComponentName;
    .param p2, "service"    # Landroid/os/IBinder;

    .prologue
    .line 40
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV$1;->this$0:Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;

    invoke-static {p2}, Lnet/dlb/aidl/InstallService$Stub;->asInterface(Landroid/os/IBinder;)Lnet/dlb/aidl/InstallService;

    move-result-object v1

    invoke-static {v0, v1}, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->access$002(Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;Lnet/dlb/aidl/InstallService;)Lnet/dlb/aidl/InstallService;

    .line 41
    return-void
.end method

.method public onServiceDisconnected(Landroid/content/ComponentName;)V
    .locals 2
    .param p1, "name"    # Landroid/content/ComponentName;

    .prologue
    .line 45
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV$1;->this$0:Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;

    const/4 v1, 0x0

    invoke-static {v0, v1}, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->access$002(Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;Lnet/dlb/aidl/InstallService;)Lnet/dlb/aidl/InstallService;

    .line 46
    return-void
.end method
