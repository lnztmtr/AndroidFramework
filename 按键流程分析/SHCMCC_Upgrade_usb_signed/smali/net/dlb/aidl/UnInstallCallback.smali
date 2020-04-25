.class public interface abstract Lnet/dlb/aidl/UnInstallCallback;
.super Ljava/lang/Object;
.source "UnInstallCallback.java"

# interfaces
.implements Landroid/os/IInterface;


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lnet/dlb/aidl/UnInstallCallback$Stub;
    }
.end annotation


# virtual methods
.method public abstract onUnInstallResult(ILjava/lang/String;)V
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Landroid/os/RemoteException;
        }
    .end annotation
.end method
