.class Lnet/sunniwell/service/swupgrade/usb/UpgradeService$2;
.super Ljava/lang/Object;
.source "UpgradeService.java"

# interfaces
.implements Landroid/widget/AdapterView$OnItemClickListener;


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
    .line 181
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$2;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public onItemClick(Landroid/widget/AdapterView;Landroid/view/View;IJ)V
    .locals 3
    .param p2, "view"    # Landroid/view/View;
    .param p3, "position"    # I
    .param p4, "id"    # J
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "(",
            "Landroid/widget/AdapterView",
            "<*>;",
            "Landroid/view/View;",
            "IJ)V"
        }
    .end annotation

    .prologue
    .line 184
    .local p1, "parent":Landroid/widget/AdapterView;, "Landroid/widget/AdapterView<*>;"
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "\u6bd4\u8f83 position="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, p3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v0

    const-string v1, ",zipfile.get("

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, p3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v0

    const-string v1, ")"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$2;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    iget-object v1, v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mVersionLists:Ljava/util/List;

    .line 185
    invoke-interface {v1, p3}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    .line 184
    invoke-static {v0}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 186
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$2;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$2;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    iget-object v0, v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mVersionLists:Ljava/util/List;

    invoke-interface {v0, p3}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/util/Map;

    const-string v2, "file"

    invoke-interface {v0, v2}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    invoke-virtual {v1, v0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->startUpgrade(Ljava/lang/String;)V

    .line 187
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "\u70b9\u51fb listview,"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$2;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    iget-object v1, v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mVersionLists:Ljava/util/List;

    invoke-interface {v1, p3}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object v0

    const-string v1, " position="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, p3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v0

    const-string v1, ",zipfiles.size()="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$2;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    iget-object v1, v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mVersionLists:Ljava/util/List;

    .line 188
    invoke-interface {v1}, Ljava/util/List;->size()I

    move-result v1

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    .line 187
    invoke-static {v0}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 189
    return-void
.end method
