.class Lnet/sunniwell/service/swupgrade/usb/Upgrade$1;
.super Ljava/lang/Object;
.source "Upgrade.java"

# interfaces
.implements Landroid/content/DialogInterface$OnClickListener;


# annotations
.annotation system Ldalvik/annotation/EnclosingMethod;
    value = Lnet/sunniwell/service/swupgrade/usb/Upgrade;->showDialog()V
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
    .line 241
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/Upgrade$1;->this$0:Lnet/sunniwell/service/swupgrade/usb/Upgrade;

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public onClick(Landroid/content/DialogInterface;I)V
    .locals 0
    .param p1, "dialog"    # Landroid/content/DialogInterface;
    .param p2, "which"    # I

    .prologue
    .line 245
    invoke-interface {p1}, Landroid/content/DialogInterface;->cancel()V

    .line 246
    return-void
.end method
