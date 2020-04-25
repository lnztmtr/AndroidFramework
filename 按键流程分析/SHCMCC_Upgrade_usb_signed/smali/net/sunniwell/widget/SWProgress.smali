.class public Lnet/sunniwell/widget/SWProgress;
.super Landroid/widget/ProgressBar;
.source "SWProgress.java"


# instance fields
.field private mContext:Landroid/content/Context;

.field private mPaint:Landroid/graphics/Paint;

.field private text:Ljava/lang/String;


# direct methods
.method public constructor <init>(Landroid/content/Context;)V
    .locals 0
    .param p1, "context"    # Landroid/content/Context;

    .prologue
    .line 25
    invoke-direct {p0, p1}, Landroid/widget/ProgressBar;-><init>(Landroid/content/Context;)V

    .line 26
    iput-object p1, p0, Lnet/sunniwell/widget/SWProgress;->mContext:Landroid/content/Context;

    .line 27
    invoke-direct {p0}, Lnet/sunniwell/widget/SWProgress;->initText()V

    .line 28
    return-void
.end method

.method public constructor <init>(Landroid/content/Context;Landroid/util/AttributeSet;)V
    .locals 0
    .param p1, "context"    # Landroid/content/Context;
    .param p2, "attrs"    # Landroid/util/AttributeSet;

    .prologue
    .line 37
    invoke-direct {p0, p1, p2}, Landroid/widget/ProgressBar;-><init>(Landroid/content/Context;Landroid/util/AttributeSet;)V

    .line 38
    iput-object p1, p0, Lnet/sunniwell/widget/SWProgress;->mContext:Landroid/content/Context;

    .line 39
    invoke-direct {p0}, Lnet/sunniwell/widget/SWProgress;->initText()V

    .line 40
    return-void
.end method

.method public constructor <init>(Landroid/content/Context;Landroid/util/AttributeSet;I)V
    .locals 0
    .param p1, "context"    # Landroid/content/Context;
    .param p2, "attrs"    # Landroid/util/AttributeSet;
    .param p3, "defStyle"    # I

    .prologue
    .line 31
    invoke-direct {p0, p1, p2, p3}, Landroid/widget/ProgressBar;-><init>(Landroid/content/Context;Landroid/util/AttributeSet;I)V

    .line 32
    iput-object p1, p0, Lnet/sunniwell/widget/SWProgress;->mContext:Landroid/content/Context;

    .line 33
    invoke-direct {p0}, Lnet/sunniwell/widget/SWProgress;->initText()V

    .line 34
    return-void
.end method

.method private initText()V
    .locals 4

    .prologue
    .line 63
    new-instance v2, Landroid/graphics/Paint;

    invoke-direct {v2}, Landroid/graphics/Paint;-><init>()V

    iput-object v2, p0, Lnet/sunniwell/widget/SWProgress;->mPaint:Landroid/graphics/Paint;

    .line 64
    iget-object v2, p0, Lnet/sunniwell/widget/SWProgress;->mPaint:Landroid/graphics/Paint;

    const/4 v3, -0x1

    invoke-virtual {v2, v3}, Landroid/graphics/Paint;->setColor(I)V

    .line 65
    iget-object v2, p0, Lnet/sunniwell/widget/SWProgress;->mPaint:Landroid/graphics/Paint;

    const/high16 v3, 0x41900000    # 18.0f

    invoke-virtual {v2, v3}, Landroid/graphics/Paint;->setTextSize(F)V

    .line 67
    iget-object v2, p0, Lnet/sunniwell/widget/SWProgress;->mContext:Landroid/content/Context;

    const-string v3, "window"

    invoke-virtual {v2, v3}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/view/WindowManager;

    .line 68
    .local v1, "windowManager":Landroid/view/WindowManager;
    new-instance v0, Landroid/graphics/Point;

    invoke-direct {v0}, Landroid/graphics/Point;-><init>()V

    .line 69
    .local v0, "outSize":Landroid/graphics/Point;
    invoke-interface {v1}, Landroid/view/WindowManager;->getDefaultDisplay()Landroid/view/Display;

    move-result-object v2

    invoke-virtual {v2, v0}, Landroid/view/Display;->getSize(Landroid/graphics/Point;)V

    .line 70
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "....window size="

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    iget v3, v0, Landroid/graphics/Point;->x:I

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v2

    const-string v3, "x"

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    iget v3, v0, Landroid/graphics/Point;->y:I

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v2}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 72
    iget v2, v0, Landroid/graphics/Point;->y:I

    const/16 v3, 0x438

    if-ne v2, v3, :cond_0

    .line 74
    iget-object v2, p0, Lnet/sunniwell/widget/SWProgress;->mPaint:Landroid/graphics/Paint;

    const/high16 v3, 0x41d80000    # 27.0f

    invoke-virtual {v2, v3}, Landroid/graphics/Paint;->setTextSize(F)V

    .line 76
    :cond_0
    return-void
.end method

.method private setText()V
    .locals 1

    .prologue
    .line 79
    invoke-virtual {p0}, Lnet/sunniwell/widget/SWProgress;->getProgress()I

    move-result v0

    invoke-direct {p0, v0}, Lnet/sunniwell/widget/SWProgress;->setText(I)V

    .line 80
    return-void
.end method

.method private setText(I)V
    .locals 3
    .param p1, "progress"    # I

    .prologue
    .line 84
    mul-int/lit8 v1, p1, 0x64

    invoke-virtual {p0}, Lnet/sunniwell/widget/SWProgress;->getMax()I

    move-result v2

    div-int v0, v1, v2

    .line 85
    .local v0, "i":I
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    invoke-static {v0}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    const-string v2, "%"

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    iput-object v1, p0, Lnet/sunniwell/widget/SWProgress;->text:Ljava/lang/String;

    .line 86
    return-void
.end method


# virtual methods
.method protected declared-synchronized onDraw(Landroid/graphics/Canvas;)V
    .locals 7
    .param p1, "canvas"    # Landroid/graphics/Canvas;

    .prologue
    .line 51
    monitor-enter p0

    :try_start_0
    invoke-super {p0, p1}, Landroid/widget/ProgressBar;->onDraw(Landroid/graphics/Canvas;)V

    .line 53
    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, "text="

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    iget-object v4, p0, Lnet/sunniwell/widget/SWProgress;->text:Ljava/lang/String;

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v3}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 54
    new-instance v0, Landroid/graphics/Rect;

    invoke-direct {v0}, Landroid/graphics/Rect;-><init>()V

    .line 55
    .local v0, "rect":Landroid/graphics/Rect;
    iget-object v3, p0, Lnet/sunniwell/widget/SWProgress;->mPaint:Landroid/graphics/Paint;

    iget-object v4, p0, Lnet/sunniwell/widget/SWProgress;->text:Ljava/lang/String;

    const/4 v5, 0x0

    iget-object v6, p0, Lnet/sunniwell/widget/SWProgress;->text:Ljava/lang/String;

    invoke-virtual {v6}, Ljava/lang/String;->length()I

    move-result v6

    invoke-virtual {v3, v4, v5, v6, v0}, Landroid/graphics/Paint;->getTextBounds(Ljava/lang/String;IILandroid/graphics/Rect;)V

    .line 56
    invoke-virtual {p0}, Lnet/sunniwell/widget/SWProgress;->getWidth()I

    move-result v3

    div-int/lit8 v3, v3, 0x2

    invoke-virtual {v0}, Landroid/graphics/Rect;->centerX()I

    move-result v4

    sub-int v1, v3, v4

    .line 57
    .local v1, "x":I
    invoke-virtual {p0}, Lnet/sunniwell/widget/SWProgress;->getHeight()I

    move-result v3

    div-int/lit8 v3, v3, 0x2

    invoke-virtual {v0}, Landroid/graphics/Rect;->centerY()I

    move-result v4

    sub-int v2, v3, v4

    .line 58
    .local v2, "y":I
    iget-object v3, p0, Lnet/sunniwell/widget/SWProgress;->text:Ljava/lang/String;

    int-to-float v4, v1

    int-to-float v5, v2

    iget-object v6, p0, Lnet/sunniwell/widget/SWProgress;->mPaint:Landroid/graphics/Paint;

    invoke-virtual {p1, v3, v4, v5, v6}, Landroid/graphics/Canvas;->drawText(Ljava/lang/String;FFLandroid/graphics/Paint;)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 59
    monitor-exit p0

    return-void

    .line 51
    .end local v0    # "rect":Landroid/graphics/Rect;
    .end local v1    # "x":I
    .end local v2    # "y":I
    :catchall_0
    move-exception v3

    monitor-exit p0

    throw v3
.end method

.method public declared-synchronized setProgress(I)V
    .locals 1
    .param p1, "progress"    # I

    .prologue
    .line 44
    monitor-enter p0

    :try_start_0
    invoke-direct {p0, p1}, Lnet/sunniwell/widget/SWProgress;->setText(I)V

    .line 45
    invoke-super {p0, p1}, Landroid/widget/ProgressBar;->setProgress(I)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 47
    monitor-exit p0

    return-void

    .line 44
    :catchall_0
    move-exception v0

    monitor-exit p0

    throw v0
.end method
