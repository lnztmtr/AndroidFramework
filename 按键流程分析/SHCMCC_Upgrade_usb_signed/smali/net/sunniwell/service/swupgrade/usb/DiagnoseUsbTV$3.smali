.class Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV$3;
.super Lnet/dlb/aidl/InstallCallback$Stub;
.source "DiagnoseUsbTV.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingMethod;
    value = Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->run()V
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x0
    name = null
.end annotation


# instance fields
.field final synthetic this$0:Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;

.field final synthetic val$pkgName:Ljava/lang/String;


# direct methods
.method constructor <init>(Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;Ljava/lang/String;)V
    .locals 0
    .param p1, "this$0"    # Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;

    .prologue
    .line 131
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV$3;->this$0:Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;

    iput-object p2, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV$3;->val$pkgName:Ljava/lang/String;

    invoke-direct {p0}, Lnet/dlb/aidl/InstallCallback$Stub;-><init>()V

    return-void
.end method


# virtual methods
.method public onInstallResult(ILjava/lang/String;)V
    .locals 2
    .param p1, "result"    # I
    .param p2, "msg"    # Ljava/lang/String;
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Landroid/os/RemoteException;
        }
    .end annotation

    .prologue
    .line 135
    if-nez p1, :cond_0

    .line 136
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV$3;->this$0:Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;

    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV$3;->val$pkgName:Ljava/lang/String;

    invoke-static {v0, v1}, Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;->access$100(Lnet/sunniwell/service/swupgrade/usb/DiagnoseUsbTV;Ljava/lang/String;)Ljava/util/Map;

    .line 141
    :cond_0
    return-void
.end method
