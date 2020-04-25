.class public interface abstract Lnet/dlb/aidl/InstallCallback;
.super Ljava/lang/Object;
.source "InstallCallback.java"

# interfaces
.implements Landroid/os/IInterface;


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lnet/dlb/aidl/InstallCallback$Stub;
    }
.end annotation


# virtual methods
.method public abstract onInstallResult(ILjava/lang/String;)V
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Landroid/os/RemoteException;
        }
    .end annotation
.end method
