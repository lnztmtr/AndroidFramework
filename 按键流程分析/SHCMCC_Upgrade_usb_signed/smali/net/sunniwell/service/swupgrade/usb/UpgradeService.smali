.class public Lnet/sunniwell/service/swupgrade/usb/UpgradeService;
.super Landroid/app/Service;
.source "UpgradeService.java"


# instance fields
.field private final FILENAME:Ljava/lang/String;

.field private final FILENAME_Bak:Ljava/lang/String;

.field OnClickListener:Landroid/widget/AdapterView$OnItemClickListener;

.field private PATH:Ljava/lang/String;

.field private btnCancel:Landroid/widget/Button;

.field private btnOk:Landroid/widget/Button;

.field private copyFilePath:Ljava/lang/String;

.field private flag:Z

.field private isCopy:Ljava/lang/Boolean;

.field private linLayout:Landroid/widget/LinearLayout;

.field private linear1:Landroid/widget/LinearLayout;

.field private listView:Landroid/widget/ListView;

.field private mProgressPar:Landroid/widget/ProgressBar;

.field private mReceiver:Landroid/content/BroadcastReceiver;

.field mVersionLists:Ljava/util/List;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/List",
            "<",
            "Ljava/util/Map",
            "<",
            "Ljava/lang/String;",
            "Ljava/lang/String;",
            ">;>;"
        }
    .end annotation
.end field

.field mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

.field private mainLinearLayout:Landroid/widget/LinearLayout;

.field public final myHandler:Landroid/os/Handler;

.field progressDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

.field private simpleAdapter:Landroid/widget/SimpleAdapter;

.field private text:Landroid/widget/TextView;

.field private textPostBuild:Landroid/widget/TextView;

.field private textone:Landroid/widget/TextView;

.field private updateReceiver:Landroid/content/BroadcastReceiver;


# direct methods
.method public constructor <init>()V
    .locals 2

    .prologue
    const/4 v1, 0x0

    .line 39
    invoke-direct {p0}, Landroid/app/Service;-><init>()V

    .line 44
    const-string v0, "update.zip"

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->FILENAME:Ljava/lang/String;

    .line 45
    const-string v0, "/cache/update/"

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->PATH:Ljava/lang/String;

    .line 46
    const-string v0, "usb_update.bak"

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->FILENAME_Bak:Ljava/lang/String;

    .line 56
    const/4 v0, 0x1

    invoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;

    move-result-object v0

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->isCopy:Ljava/lang/Boolean;

    .line 57
    iput-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->copyFilePath:Ljava/lang/String;

    .line 59
    iput-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    .line 60
    iput-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->progressDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    .line 63
    iput-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mVersionLists:Ljava/util/List;

    .line 181
    new-instance v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$2;

    invoke-direct {v0, p0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$2;-><init>(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)V

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->OnClickListener:Landroid/widget/AdapterView$OnItemClickListener;

    .line 508
    const/4 v0, 0x0

    iput-boolean v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->flag:Z

    .line 509
    new-instance v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$6;

    invoke-direct {v0, p0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$6;-><init>(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)V

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->myHandler:Landroid/os/Handler;

    return-void
.end method

.method private PostBuild(Ljava/lang/String;)Ljava/lang/String;
    .locals 4
    .param p1, "s"    # Ljava/lang/String;

    .prologue
    .line 502
    const-string v2, "\\:"

    invoke-virtual {p1, v2}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v0

    .line 503
    .local v0, "str":[Ljava/lang/String;
    const/4 v2, 0x1

    aget-object v2, v0, v2

    const-string v3, "\\/"

    invoke-virtual {v2, v3}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v1

    .line 505
    .local v1, "strm":[Ljava/lang/String;
    const/4 v2, 0x2

    aget-object v2, v1, v2

    return-object v2
.end method

.method static synthetic access$000(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)Ljava/lang/String;
    .locals 1
    .param p0, "x0"    # Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    .prologue
    .line 39
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->PATH:Ljava/lang/String;

    return-object v0
.end method

.method static synthetic access$100(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)Ljava/lang/Boolean;
    .locals 1
    .param p0, "x0"    # Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    .prologue
    .line 39
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->isCopy:Ljava/lang/Boolean;

    return-object v0
.end method

.method static synthetic access$102(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;Ljava/lang/Boolean;)Ljava/lang/Boolean;
    .locals 0
    .param p0, "x0"    # Lnet/sunniwell/service/swupgrade/usb/UpgradeService;
    .param p1, "x1"    # Ljava/lang/Boolean;

    .prologue
    .line 39
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->isCopy:Ljava/lang/Boolean;

    return-object p1
.end method

.method static synthetic access$200(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;Ljava/io/File;Ljava/lang/String;)Z
    .locals 1
    .param p0, "x0"    # Lnet/sunniwell/service/swupgrade/usb/UpgradeService;
    .param p1, "x1"    # Ljava/io/File;
    .param p2, "x2"    # Ljava/lang/String;
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    .prologue
    .line 39
    invoke-direct {p0, p1, p2}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->renameFile(Ljava/io/File;Ljava/lang/String;)Z

    move-result v0

    return v0
.end method

.method static synthetic access$302(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;Z)Z
    .locals 0
    .param p0, "x0"    # Lnet/sunniwell/service/swupgrade/usb/UpgradeService;
    .param p1, "x1"    # Z

    .prologue
    .line 39
    iput-boolean p1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->flag:Z

    return p1
.end method

.method static synthetic access$400(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)V
    .locals 0
    .param p0, "x0"    # Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    .prologue
    .line 39
    invoke-direct {p0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->showDialog2()V

    return-void
.end method

.method static synthetic access$500(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)Ljava/lang/String;
    .locals 1
    .param p0, "x0"    # Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    .prologue
    .line 39
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->copyFilePath:Ljava/lang/String;

    return-object v0
.end method

.method static synthetic access$600(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;Ljava/lang/String;)V
    .locals 0
    .param p0, "x0"    # Lnet/sunniwell/service/swupgrade/usb/UpgradeService;
    .param p1, "x1"    # Ljava/lang/String;

    .prologue
    .line 39
    invoke-direct {p0, p1}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->copyfile(Ljava/lang/String;)V

    return-void
.end method

.method static synthetic access$700(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)Landroid/widget/ProgressBar;
    .locals 1
    .param p0, "x0"    # Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    .prologue
    .line 39
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mProgressPar:Landroid/widget/ProgressBar;

    return-object v0
.end method

.method static synthetic access$800(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)V
    .locals 0
    .param p0, "x0"    # Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    .prologue
    .line 39
    invoke-direct {p0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->showDialog3()V

    return-void
.end method

.method static synthetic access$900(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)V
    .locals 0
    .param p0, "x0"    # Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    .prologue
    .line 39
    invoke-direct {p0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->reboot()V

    return-void
.end method

.method private copyfile(Ljava/lang/String;)V
    .locals 2
    .param p1, "filePath"    # Ljava/lang/String;

    .prologue
    .line 223
    new-instance v0, Ljava/lang/Thread;

    new-instance v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$3;

    invoke-direct {v1, p0, p1}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$3;-><init>(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;Ljava/lang/String;)V

    invoke-direct {v0, v1}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;)V

    .line 303
    invoke-virtual {v0}, Ljava/lang/Thread;->start()V

    .line 304
    return-void
.end method

.method private initView()V
    .locals 9

    .prologue
    const/4 v8, 0x2

    const/4 v7, 0x0

    const/4 v5, 0x1

    .line 126
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    if-nez v0, :cond_0

    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mVersionLists:Ljava/util/List;

    if-eqz v0, :cond_0

    .line 128
    new-instance v0, Lcom/duolebo/tools/shcmcc/TopWindow;

    const v1, 0x7f030002

    invoke-direct {v0, p0, v1}, Lcom/duolebo/tools/shcmcc/TopWindow;-><init>(Landroid/content/Context;I)V

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    .line 129
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    const/16 v1, 0x1f4

    invoke-virtual {v0, v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->setWidth(I)V

    .line 130
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    const/16 v1, 0x12c

    invoke-virtual {v0, v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->setHeight(I)V

    .line 132
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    const v1, 0x7f070008

    invoke-virtual {v0, v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/LinearLayout;

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->linLayout:Landroid/widget/LinearLayout;

    .line 133
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    const v1, 0x7f070005

    invoke-virtual {v0, v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/LinearLayout;

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainLinearLayout:Landroid/widget/LinearLayout;

    .line 134
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    const v1, 0x7f070006

    invoke-virtual {v0, v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/LinearLayout;

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->linear1:Landroid/widget/LinearLayout;

    .line 135
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    const v1, 0x7f07000e

    invoke-virtual {v0, v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/TextView;

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->text:Landroid/widget/TextView;

    .line 136
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    const v1, 0x7f070010

    invoke-virtual {v0, v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/Button;

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->btnOk:Landroid/widget/Button;

    .line 137
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    const v1, 0x7f070011

    invoke-virtual {v0, v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/Button;

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->btnCancel:Landroid/widget/Button;

    .line 138
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    const v1, 0x7f07000b

    invoke-virtual {v0, v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/ListView;

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->listView:Landroid/widget/ListView;

    .line 139
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    const v1, 0x7f07000a

    invoke-virtual {v0, v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/TextView;

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->textPostBuild:Landroid/widget/TextView;

    .line 140
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->textPostBuild:Landroid/widget/TextView;

    sget-object v1, Landroid/os/Build;->FINGERPRINT:Ljava/lang/String;

    invoke-direct {p0, v1}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->PostBuild(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 141
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    const v1, 0x7f07000c

    invoke-virtual {v0, v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/TextView;

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->textone:Landroid/widget/TextView;

    .line 143
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->listView:Landroid/widget/ListView;

    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->OnClickListener:Landroid/widget/AdapterView$OnItemClickListener;

    invoke-virtual {v0, v1}, Landroid/widget/ListView;->setOnItemClickListener(Landroid/widget/AdapterView$OnItemClickListener;)V

    .line 144
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->listView:Landroid/widget/ListView;

    new-instance v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$1;

    invoke-direct {v1, p0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$1;-><init>(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)V

    invoke-virtual {v0, v1}, Landroid/widget/ListView;->setOnKeyListener(Landroid/view/View$OnKeyListener;)V

    .line 157
    :cond_0
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    invoke-virtual {v0}, Lcom/duolebo/tools/shcmcc/TopWindow;->show()V

    .line 159
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->progressDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    if-nez v0, :cond_1

    .line 161
    new-instance v0, Lcom/duolebo/tools/shcmcc/TopWindow;

    const/high16 v1, 0x7f030000

    invoke-direct {v0, p0, v1}, Lcom/duolebo/tools/shcmcc/TopWindow;-><init>(Landroid/content/Context;I)V

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->progressDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    .line 162
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->progressDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    const/16 v1, 0x2bc

    invoke-virtual {v0, v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->setWidth(I)V

    .line 163
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->progressDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    const/16 v1, 0xc8

    invoke-virtual {v0, v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->setHeight(I)V

    .line 164
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->progressDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    const v1, 0x7f070001

    invoke-virtual {v0, v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->findViewById(I)Landroid/view/View;

    move-result-object v0

    check-cast v0, Landroid/widget/ProgressBar;

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mProgressPar:Landroid/widget/ProgressBar;

    .line 165
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->progressDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    const/high16 v1, 0x7f070000

    invoke-virtual {v0, v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->findViewById(I)Landroid/view/View;

    move-result-object v6

    check-cast v6, Landroid/widget/TextView;

    .line 166
    .local v6, "mProgressMessage":Landroid/widget/TextView;
    const-string v0, "\u5e73\u53f0\u6b63\u5728\u590d\u5236\u6587\u4ef6\u4e2d\uff0c\u8bf7\u52ff\u5173\u673a\uff01"

    invoke-virtual {v6, v0}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 169
    .end local v6    # "mProgressMessage":Landroid/widget/TextView;
    :cond_1
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mVersionLists:Ljava/util/List;

    invoke-interface {v0}, Ljava/util/List;->size()I

    move-result v0

    if-le v0, v5, :cond_3

    .line 171
    new-instance v0, Landroid/widget/SimpleAdapter;

    iget-object v2, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mVersionLists:Ljava/util/List;

    const v3, 0x7f030001

    new-array v4, v8, [Ljava/lang/String;

    const-string v1, "version"

    aput-object v1, v4, v7

    const-string v1, "check"

    aput-object v1, v4, v5

    new-array v5, v8, [I

    fill-array-data v5, :array_0

    move-object v1, p0

    invoke-direct/range {v0 .. v5}, Landroid/widget/SimpleAdapter;-><init>(Landroid/content/Context;Ljava/util/List;I[Ljava/lang/String;[I)V

    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->simpleAdapter:Landroid/widget/SimpleAdapter;

    .line 174
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->listView:Landroid/widget/ListView;

    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->simpleAdapter:Landroid/widget/SimpleAdapter;

    invoke-virtual {v0, v1}, Landroid/widget/ListView;->setAdapter(Landroid/widget/ListAdapter;)V

    .line 180
    :cond_2
    :goto_0
    return-void

    .line 175
    :cond_3
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mVersionLists:Ljava/util/List;

    invoke-interface {v0}, Ljava/util/List;->size()I

    move-result v0

    if-ne v0, v5, :cond_2

    .line 177
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->linLayout:Landroid/widget/LinearLayout;

    const/16 v1, 0x8

    invoke-virtual {v0, v1}, Landroid/widget/LinearLayout;->setVisibility(I)V

    .line 178
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mVersionLists:Ljava/util/List;

    invoke-interface {v0, v7}, Ljava/util/List;->get(I)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/util/Map;

    const-string v1, "file"

    invoke-interface {v0, v1}, Ljava/util/Map;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Ljava/lang/String;

    invoke-virtual {p0, v0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->startUpgrade(Ljava/lang/String;)V

    goto :goto_0

    .line 171
    nop

    :array_0
    .array-data 4
        0x7f070003
        0x7f070004
    .end array-data
.end method

.method private reboot()V
    .locals 4

    .prologue
    .line 430
    const-string v1, ".....reboot()....._____________@@@@@@@@@@@@@@"

    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 432
    :try_start_0
    new-instance v1, Ljava/io/File;

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    iget-object v3, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->PATH:Ljava/lang/String;

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    const-string v3, "update.zip"

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-direct {v1, v2}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    invoke-static {p0, v1}, Landroid/os/RecoverySystem;->installPackage(Landroid/content/Context;Ljava/io/File;)V
    :try_end_0
    .catch Ljava/io/IOException; {:try_start_0 .. :try_end_0} :catch_0

    .line 437
    :goto_0
    return-void

    .line 434
    :catch_0
    move-exception v0

    .line 435
    .local v0, "e":Ljava/io/IOException;
    invoke-static {v0}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_0
.end method

.method private registerReceiver()V
    .locals 2

    .prologue
    .line 454
    const-string v1, ".........registerReceiver....."

    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 456
    new-instance v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$5;

    invoke-direct {v1, p0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$5;-><init>(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)V

    iput-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mReceiver:Landroid/content/BroadcastReceiver;

    .line 466
    new-instance v0, Landroid/content/IntentFilter;

    invoke-direct {v0}, Landroid/content/IntentFilter;-><init>()V

    .line 467
    .local v0, "intent2":Landroid/content/IntentFilter;
    const-string v1, "android.intent.action.MEDIA_UNMOUNTED"

    invoke-virtual {v0, v1}, Landroid/content/IntentFilter;->addAction(Ljava/lang/String;)V

    .line 468
    const-string v1, "android.intent.action.MEDIA_BAD_REMOVAL"

    invoke-virtual {v0, v1}, Landroid/content/IntentFilter;->addAction(Ljava/lang/String;)V

    .line 469
    const-string v1, "file"

    invoke-virtual {v0, v1}, Landroid/content/IntentFilter;->addDataScheme(Ljava/lang/String;)V

    .line 470
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mReceiver:Landroid/content/BroadcastReceiver;

    invoke-virtual {p0, v1, v0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->registerReceiver(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;)Landroid/content/Intent;

    .line 471
    return-void
.end method

.method private registerReceiverUpdate()V
    .locals 2

    .prologue
    .line 329
    const-string v1, ".........registerReceiverUpdate....."

    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 330
    new-instance v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$4;

    invoke-direct {v1, p0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$4;-><init>(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)V

    iput-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->updateReceiver:Landroid/content/BroadcastReceiver;

    .line 347
    new-instance v0, Landroid/content/IntentFilter;

    invoke-direct {v0}, Landroid/content/IntentFilter;-><init>()V

    .line 348
    .local v0, "filter":Landroid/content/IntentFilter;
    const-string v1, "net.sunniwell.action.UPGRADE"

    invoke-virtual {v0, v1}, Landroid/content/IntentFilter;->addAction(Ljava/lang/String;)V

    .line 349
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->updateReceiver:Landroid/content/BroadcastReceiver;

    invoke-virtual {p0, v1, v0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->registerReceiver(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;)Landroid/content/Intent;

    .line 351
    return-void
.end method

.method private renameFile(Ljava/io/File;Ljava/lang/String;)Z
    .locals 5
    .param p1, "src"    # Ljava/io/File;
    .param p2, "newFileNAME"    # Ljava/lang/String;
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    .prologue
    .line 314
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "start rename==============\u4fee\u6539\u524d\uff0c\u6587\u4ef6\u5927\u5c0f================="

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    .line 315
    invoke-virtual {p1}, Ljava/io/File;->length()J

    move-result-wide v3

    invoke-virtual {v2, v3, v4}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    .line 314
    invoke-static {v2}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 316
    const/4 v0, 0x0

    .line 317
    .local v0, "bool":Z
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p1}, Ljava/io/File;->getParentFile()Ljava/io/File;

    move-result-object v3

    invoke-virtual {v3}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    const-string v3, "/"

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2, p2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    .line 319
    .local v1, "rename":Ljava/lang/String;
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {p1}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v3

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    const-string v3, "renameTo "

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v2}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 320
    new-instance v2, Ljava/io/File;

    invoke-direct {v2, v1}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    invoke-virtual {p1, v2}, Ljava/io/File;->renameTo(Ljava/io/File;)Z

    move-result v0

    .line 321
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "....renameFile()... end...bool="

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v2}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 322
    return v0
.end method

.method private showDialog1(Ljava/lang/String;)V
    .locals 4
    .param p1, "zipfiFile"    # Ljava/lang/String;

    .prologue
    .line 375
    const-string v0, "===============showDialog1======="

    invoke-static {v0}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 376
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->linear1:Landroid/widget/LinearLayout;

    const/16 v1, 0x8

    invoke-virtual {v0, v1}, Landroid/widget/LinearLayout;->setVisibility(I)V

    .line 377
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainLinearLayout:Landroid/widget/LinearLayout;

    const/4 v1, 0x0

    invoke-virtual {v0, v1}, Landroid/widget/LinearLayout;->setBackgroundColor(I)V

    .line 378
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->copyFilePath:Ljava/lang/String;

    .line 379
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->myHandler:Landroid/os/Handler;

    const/4 v1, 0x1

    invoke-virtual {v0, v1}, Landroid/os/Handler;->sendEmptyMessage(I)Z

    .line 380
    iget-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->myHandler:Landroid/os/Handler;

    const/4 v1, 0x2

    const-wide/16 v2, 0x3e8

    invoke-virtual {v0, v1, v2, v3}, Landroid/os/Handler;->sendEmptyMessageDelayed(IJ)Z

    .line 381
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "=====copyfile="

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->copyFilePath:Ljava/lang/String;

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 382
    return-void
.end method

.method private showDialog2()V
    .locals 3

    .prologue
    .line 388
    const-string v1, "===============showDialog2======="

    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 391
    :try_start_0
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mProgressPar:Landroid/widget/ProgressBar;

    iget-object v2, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mProgressPar:Landroid/widget/ProgressBar;

    invoke-virtual {v2}, Landroid/widget/ProgressBar;->getProgress()I

    move-result v2

    neg-int v2, v2

    invoke-virtual {v1, v2}, Landroid/widget/ProgressBar;->incrementProgressBy(I)V

    .line 392
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mProgressPar:Landroid/widget/ProgressBar;

    const/4 v2, 0x0

    invoke-virtual {v1, v2}, Landroid/widget/ProgressBar;->setProgress(I)V

    .line 395
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->progressDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    invoke-virtual {v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->show()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    .line 399
    :goto_0
    return-void

    .line 396
    :catch_0
    move-exception v0

    .line 397
    .local v0, "e":Ljava/lang/Exception;
    invoke-static {v0}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_0
.end method

.method private showDialog3()V
    .locals 4

    .prologue
    .line 405
    const-string v1, "===============showDialog3======="

    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 419
    :try_start_0
    invoke-virtual {p0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->getApplicationContext()Landroid/content/Context;

    move-result-object v1

    const v2, 0x7f05000d

    const/4 v3, 0x1

    invoke-static {v1, v2, v3}, Landroid/widget/Toast;->makeText(Landroid/content/Context;II)Landroid/widget/Toast;

    move-result-object v1

    invoke-virtual {v1}, Landroid/widget/Toast;->show()V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    .line 423
    :goto_0
    return-void

    .line 420
    :catch_0
    move-exception v0

    .line 421
    .local v0, "e":Ljava/lang/Exception;
    invoke-static {v0}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_0
.end method


# virtual methods
.method public delTempFile()V
    .locals 5

    .prologue
    .line 357
    const-string v3, ""

    invoke-static {v3}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 358
    new-instance v1, Ljava/io/File;

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    iget-object v4, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->PATH:Ljava/lang/String;

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    const-string v4, "usb_update.bak"

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-direct {v1, v3}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    .line 359
    .local v1, "f":Ljava/io/File;
    invoke-virtual {v1}, Ljava/io/File;->exists()Z

    move-result v3

    if-eqz v3, :cond_0

    .line 360
    invoke-virtual {v1}, Ljava/io/File;->delete()Z

    move-result v0

    .line 361
    .local v0, "b":Z
    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, "Delete file= "

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v1}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    const-string v4, ":::"

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v3}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 363
    .end local v0    # "b":Z
    :cond_0
    new-instance v2, Ljava/io/File;

    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    iget-object v4, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->PATH:Ljava/lang/String;

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    const-string v4, "update.zip"

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    .line 364
    .local v2, "f1":Ljava/io/File;
    invoke-virtual {v2}, Ljava/io/File;->exists()Z

    move-result v3

    if-eqz v3, :cond_1

    .line 365
    invoke-virtual {v1}, Ljava/io/File;->delete()Z

    move-result v0

    .line 366
    .restart local v0    # "b":Z
    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, "Delete file= :::"

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3, v0}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v3}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 369
    .end local v0    # "b":Z
    :cond_1
    return-void
.end method

.method protected finalize()V
    .locals 1
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Throwable;
        }
    .end annotation

    .prologue
    .line 497
    invoke-super {p0}, Ljava/lang/Object;->finalize()V

    .line 498
    const-string v0, "upgrade======finalize()"

    invoke-static {v0}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 499
    return-void
.end method

.method public finish()V
    .locals 2

    .prologue
    .line 477
    :try_start_0
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mReceiver:Landroid/content/BroadcastReceiver;

    if-eqz v1, :cond_0

    .line 478
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mReceiver:Landroid/content/BroadcastReceiver;

    invoke-virtual {p0, v1}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->unregisterReceiver(Landroid/content/BroadcastReceiver;)V

    .line 479
    :cond_0
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->updateReceiver:Landroid/content/BroadcastReceiver;

    if-eqz v1, :cond_1

    .line 480
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->updateReceiver:Landroid/content/BroadcastReceiver;

    invoke-virtual {p0, v1}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->unregisterReceiver(Landroid/content/BroadcastReceiver;)V
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    .line 485
    :cond_1
    :goto_0
    :try_start_1
    invoke-virtual {p0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->stopSelf()V
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_2

    .line 487
    :goto_1
    :try_start_2
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    invoke-virtual {v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->dismiss()V

    .line 488
    const/4 v1, 0x0

    iput-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    .line 490
    sget-object v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mInstance:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;

    invoke-virtual {v1}, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->finish()V

    .line 494
    :goto_2
    return-void

    .line 481
    :catch_0
    move-exception v0

    .line 482
    .local v0, "E":Ljava/lang/Exception;
    invoke-static {v0}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_1

    goto :goto_0

    .line 491
    .end local v0    # "E":Ljava/lang/Exception;
    :catch_1
    move-exception v0

    .line 492
    .restart local v0    # "E":Ljava/lang/Exception;
    invoke-static {v0}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_2

    .line 485
    .end local v0    # "E":Ljava/lang/Exception;
    :catch_2
    move-exception v0

    .restart local v0    # "E":Ljava/lang/Exception;
    :try_start_3
    invoke-static {v0}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_1

    goto :goto_1
.end method

.method public onBind(Landroid/content/Intent;)Landroid/os/IBinder;
    .locals 1
    .param p1, "intent"    # Landroid/content/Intent;

    .prologue
    .line 67
    const/4 v0, 0x0

    return-object v0
.end method

.method public onCreate()V
    .locals 3

    .prologue
    .line 72
    invoke-virtual {p0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->getContentResolver()Landroid/content/ContentResolver;

    move-result-object v1

    const-string v2, "upgrade_path"

    invoke-static {v1, v2}, Landroid/provider/Settings$Secure;->getString(Landroid/content/ContentResolver;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 73
    .local v0, "upgrade_path":Ljava/lang/String;
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "get upgrade_path from Setting="

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 74
    if-eqz v0, :cond_0

    const-string v1, ""

    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_1

    .line 85
    :cond_0
    :goto_0
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "using upgrade_path="

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    iget-object v2, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->PATH:Ljava/lang/String;

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 86
    sget-object v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mInstance:Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;

    iget-object v1, v1, Lnet/sunniwell/service/swupgrade/usb/UpgradeActivity;->mVersionLists:Ljava/util/List;

    iput-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mVersionLists:Ljava/util/List;

    .line 88
    invoke-super {p0}, Landroid/app/Service;->onCreate()V

    .line 89
    return-void

    .line 76
    :cond_1
    const-string v1, "/"

    invoke-virtual {v0, v1}, Ljava/lang/String;->endsWith(Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_2

    .line 77
    iput-object v0, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->PATH:Ljava/lang/String;

    .line 81
    :goto_1
    const-string v1, "/"

    invoke-virtual {v0, v1}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v1

    if-nez v1, :cond_0

    .line 82
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "/"

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    iput-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->PATH:Ljava/lang/String;

    goto :goto_0

    .line 79
    :cond_2
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    const-string v2, "/"

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    iput-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->PATH:Ljava/lang/String;

    goto :goto_1
.end method

.method public onDestroy()V
    .locals 0

    .prologue
    .line 197
    invoke-super {p0}, Landroid/app/Service;->onDestroy()V

    .line 198
    return-void
.end method

.method public onStart(Landroid/content/Intent;I)V
    .locals 0
    .param p1, "intent"    # Landroid/content/Intent;
    .param p2, "startId"    # I

    .prologue
    .line 93
    invoke-super {p0, p1, p2}, Landroid/app/Service;->onStart(Landroid/content/Intent;I)V

    .line 95
    return-void
.end method

.method public onStartCommand(Landroid/content/Intent;II)I
    .locals 3
    .param p1, "intent"    # Landroid/content/Intent;
    .param p2, "flags"    # I
    .param p3, "startId"    # I

    .prologue
    .line 99
    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    if-eqz v1, :cond_0

    iget-object v1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->mainDialog:Lcom/duolebo/tools/shcmcc/TopWindow;

    invoke-virtual {v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->isShowing()Z

    move-result v1

    if-nez v1, :cond_1

    :cond_0
    const/4 v0, 0x0

    .line 100
    .local v0, "isShowing":Z
    :goto_0
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "....onStartCommand()....isShow="

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 102
    invoke-direct {p0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->registerReceiver()V

    .line 103
    invoke-direct {p0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->registerReceiverUpdate()V

    .line 105
    invoke-direct {p0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->initView()V

    .line 107
    invoke-super {p0, p1, p2, p3}, Landroid/app/Service;->onStartCommand(Landroid/content/Intent;II)I

    move-result v1

    return v1

    .line 99
    .end local v0    # "isShowing":Z
    :cond_1
    const/4 v0, 0x1

    goto :goto_0
.end method

.method public startUpgrade(Ljava/lang/String;)V
    .locals 3
    .param p1, "zipfiFile"    # Ljava/lang/String;

    .prologue
    .line 206
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "zipfiFile="

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    invoke-static {v1}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 207
    new-instance v0, Landroid/content/Intent;

    invoke-direct {v0}, Landroid/content/Intent;-><init>()V

    .line 208
    .local v0, "intent":Landroid/content/Intent;
    const-string v1, "net.sunniwell.action.UPGRADE"

    invoke-virtual {v0, v1}, Landroid/content/Intent;->setAction(Ljava/lang/String;)Landroid/content/Intent;

    .line 209
    const-string v1, "apk_type"

    const-string v2, "usb"

    invoke-virtual {v0, v1, v2}, Landroid/content/Intent;->putExtra(Ljava/lang/String;Ljava/lang/String;)Landroid/content/Intent;

    .line 210
    invoke-virtual {p0, v0}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->sendBroadcast(Landroid/content/Intent;)V

    .line 211
    invoke-direct {p0, p1}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->showDialog1(Ljava/lang/String;)V

    .line 212
    return-void
.end method
