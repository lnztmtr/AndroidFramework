.class public interface abstract Lnet/dlb/aidl/InstallService;
.super Ljava/lang/Object;
.source "InstallService.java"

# interfaces
.implements Landroid/os/IInterface;


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lnet/dlb/aidl/InstallService$Stub;
    }
.end annotation


# virtual methods
.method public abstract install(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/dlb/aidl/InstallCallback;)V
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Landroid/os/RemoteException;
        }
    .end annotation
.end method

.method public abstract uninstall(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lnet/dlb/aidl/UnInstallCallback;)V
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Landroid/os/RemoteException;
        }
    .end annotation
.end method
