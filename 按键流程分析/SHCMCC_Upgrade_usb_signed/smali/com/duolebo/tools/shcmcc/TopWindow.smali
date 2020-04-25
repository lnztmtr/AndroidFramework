.class public Lcom/duolebo/tools/shcmcc/TopWindow;
.super Ljava/lang/Object;
.source "TopWindow.java"


# static fields
.field public static final TYPE_TOPLEVEL_WIN_MODAL:I = 0x2

.field public static final TYPE_TOPLEVEL_WIN_NORMAL:I = 0x1

.field private static final WRAP:I = -0x2


# instance fields
.field private height:I

.field private ismShow:Z

.field private mContext:Landroid/content/Context;

.field private mLayout:I

.field private mSubView:Landroid/view/View;

.field private mWindowManager:Landroid/view/WindowManager;

.field private width:I


# direct methods
.method public constructor <init>(Landroid/content/Context;I)V
    .locals 3
    .param p1, "context"    # Landroid/content/Context;
    .param p2, "layout"    # I

    .prologue
    const/4 v2, 0x0

    .line 29
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 20
    iput-object v2, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->mSubView:Landroid/view/View;

    .line 26
    const/16 v1, 0x1f4

    iput v1, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->width:I

    .line 27
    const/16 v1, 0xc8

    iput v1, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->height:I

    .line 30
    iput-object p1, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->mContext:Landroid/content/Context;

    .line 31
    iput p2, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->mLayout:I

    .line 32
    const-string v1, "window"

    invoke-virtual {p1, v1}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/view/WindowManager;

    iput-object v1, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->mWindowManager:Landroid/view/WindowManager;

    .line 33
    iget-object v1, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->mContext:Landroid/content/Context;

    invoke-static {v1}, Landroid/view/LayoutInflater;->from(Landroid/content/Context;)Landroid/view/LayoutInflater;

    move-result-object v0

    .line 34
    .local v0, "inflater":Landroid/view/LayoutInflater;
    iget v1, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->mLayout:I

    invoke-virtual {v0, v1, v2}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;)Landroid/view/View;

    move-result-object v1

    iput-object v1, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->mSubView:Landroid/view/View;

    .line 35
    return-void
.end method

.method private getWinLayParams(I)Landroid/view/WindowManager$LayoutParams;
    .locals 8
    .param p1, "type"    # I

    .prologue
    const/4 v7, 0x2

    const/high16 v6, 0x3f000000    # 0.5f

    const/4 v5, -0x2

    .line 53
    const-string v4, "....upgradeService...getWinLayParams()....."

    invoke-static {v4}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 54
    new-instance v3, Landroid/view/WindowManager$LayoutParams;

    invoke-direct {v3, v5, v5}, Landroid/view/WindowManager$LayoutParams;-><init>(II)V

    .line 56
    .local v3, "wlp":Landroid/view/WindowManager$LayoutParams;
    const/4 v4, 0x0

    iput v4, v3, Landroid/view/WindowManager$LayoutParams;->dimAmount:F

    .line 57
    iput v5, v3, Landroid/view/WindowManager$LayoutParams;->format:I

    .line 58
    if-ne p1, v7, :cond_2

    .line 59
    iput v7, v3, Landroid/view/WindowManager$LayoutParams;->flags:I

    .line 60
    iput v6, v3, Landroid/view/WindowManager$LayoutParams;->dimAmount:F

    .line 65
    :cond_0
    :goto_0
    iget v4, v3, Landroid/view/WindowManager$LayoutParams;->flags:I

    const/high16 v5, 0x40000

    or-int/2addr v4, v5

    iput v4, v3, Landroid/view/WindowManager$LayoutParams;->flags:I

    .line 66
    iget v4, v3, Landroid/view/WindowManager$LayoutParams;->flags:I

    or-int/lit16 v4, v4, 0x200

    iput v4, v3, Landroid/view/WindowManager$LayoutParams;->flags:I

    .line 68
    const/16 v4, 0x7d3

    iput v4, v3, Landroid/view/WindowManager$LayoutParams;->type:I

    .line 71
    iget-object v4, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->mContext:Landroid/content/Context;

    const-string v5, "window"

    invoke-virtual {v4, v5}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Landroid/view/WindowManager;

    .line 72
    .local v2, "windowManager":Landroid/view/WindowManager;
    new-instance v0, Landroid/graphics/Point;

    invoke-direct {v0}, Landroid/graphics/Point;-><init>()V

    .line 73
    .local v0, "outSize":Landroid/graphics/Point;
    invoke-interface {v2}, Landroid/view/WindowManager;->getDefaultDisplay()Landroid/view/Display;

    move-result-object v4

    invoke-virtual {v4, v0}, Landroid/view/Display;->getSize(Landroid/graphics/Point;)V

    .line 74
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, "....window size="

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    iget v5, v0, Landroid/graphics/Point;->x:I

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v4

    const-string v5, "x"

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    iget v5, v0, Landroid/graphics/Point;->y:I

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-static {v4}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 75
    const/16 v1, 0xa

    .line 76
    .local v1, "ratio":I
    iget v4, v0, Landroid/graphics/Point;->y:I

    const/16 v5, 0x438

    if-ne v4, v5, :cond_1

    .line 78
    const/16 v1, 0xf

    .line 82
    :cond_1
    iget v4, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->width:I

    mul-int/2addr v4, v1

    div-int/lit8 v4, v4, 0xa

    iput v4, v3, Landroid/view/WindowManager$LayoutParams;->width:I

    .line 83
    iget v4, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->height:I

    mul-int/2addr v4, v1

    div-int/lit8 v4, v4, 0xa

    iput v4, v3, Landroid/view/WindowManager$LayoutParams;->height:I

    .line 86
    const/16 v4, 0x11

    iput v4, v3, Landroid/view/WindowManager$LayoutParams;->gravity:I

    .line 87
    return-object v3

    .line 61
    .end local v0    # "outSize":Landroid/graphics/Point;
    .end local v1    # "ratio":I
    .end local v2    # "windowManager":Landroid/view/WindowManager;
    :cond_2
    const/4 v4, 0x1

    if-ne p1, v4, :cond_0

    .line 62
    const/16 v4, 0x8

    iput v4, v3, Landroid/view/WindowManager$LayoutParams;->flags:I

    .line 63
    iput v6, v3, Landroid/view/WindowManager$LayoutParams;->dimAmount:F

    goto :goto_0
.end method


# virtual methods
.method public declared-synchronized dismiss()V
    .locals 2

    .prologue
    .line 46
    monitor-enter p0

    :try_start_0
    iget-boolean v0, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->ismShow:Z

    if-eqz v0, :cond_0

    .line 47
    iget-object v0, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->mWindowManager:Landroid/view/WindowManager;

    iget-object v1, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->mSubView:Landroid/view/View;

    invoke-interface {v0, v1}, Landroid/view/WindowManager;->removeView(Landroid/view/View;)V

    .line 48
    const/4 v0, 0x0

    iput-boolean v0, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->ismShow:Z
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 50
    :cond_0
    monitor-exit p0

    return-void

    .line 46
    :catchall_0
    move-exception v0

    monitor-exit p0

    throw v0
.end method

.method public findViewById(I)Landroid/view/View;
    .locals 1
    .param p1, "id"    # I

    .prologue
    .line 91
    iget-object v0, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->mSubView:Landroid/view/View;

    invoke-virtual {v0, p1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object v0

    return-object v0
.end method

.method public getHeight()I
    .locals 1

    .prologue
    .line 106
    iget v0, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->height:I

    return v0
.end method

.method public getWidth()I
    .locals 1

    .prologue
    .line 98
    iget v0, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->width:I

    return v0
.end method

.method public isShowing()Z
    .locals 1

    .prologue
    .line 95
    iget-boolean v0, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->ismShow:Z

    return v0
.end method

.method public setHeight(I)V
    .locals 0
    .param p1, "height"    # I

    .prologue
    .line 110
    iput p1, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->height:I

    .line 111
    return-void
.end method

.method public setWidth(I)V
    .locals 0
    .param p1, "width"    # I

    .prologue
    .line 102
    iput p1, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->width:I

    .line 103
    return-void
.end method

.method public declared-synchronized show()V
    .locals 3

    .prologue
    .line 38
    monitor-enter p0

    :try_start_0
    iget-boolean v1, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->ismShow:Z

    if-nez v1, :cond_0

    .line 39
    const/4 v1, 0x2

    invoke-direct {p0, v1}, Lcom/duolebo/tools/shcmcc/TopWindow;->getWinLayParams(I)Landroid/view/WindowManager$LayoutParams;

    move-result-object v0

    .line 40
    .local v0, "mWmlp":Landroid/view/WindowManager$LayoutParams;
    iget-object v1, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->mWindowManager:Landroid/view/WindowManager;

    iget-object v2, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->mSubView:Landroid/view/View;

    invoke-interface {v1, v2, v0}, Landroid/view/WindowManager;->addView(Landroid/view/View;Landroid/view/ViewGroup$LayoutParams;)V

    .line 41
    const/4 v1, 0x1

    iput-boolean v1, p0, Lcom/duolebo/tools/shcmcc/TopWindow;->ismShow:Z
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 43
    .end local v0    # "mWmlp":Landroid/view/WindowManager$LayoutParams;
    :cond_0
    monitor-exit p0

    return-void

    .line 38
    :catchall_0
    move-exception v1

    monitor-exit p0

    throw v1
.end method
