.class public Lnet/sunniwell/common/tools/DateTime;
.super Ljava/lang/Object;
.source "DateTime.java"


# instance fields
.field private can:Ljava/util/Calendar;


# direct methods
.method public constructor <init>()V
    .locals 1

    .prologue
    .line 17
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 15
    invoke-static {}, Ljava/util/Calendar;->getInstance()Ljava/util/Calendar;

    move-result-object v0

    iput-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    .line 20
    return-void
.end method

.method public constructor <init>(IIIIII)V
    .locals 1
    .param p1, "year"    # I
    .param p2, "month"    # I
    .param p3, "date"    # I
    .param p4, "hours"    # I
    .param p5, "minutes"    # I
    .param p6, "seconds"    # I

    .prologue
    .line 22
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 15
    invoke-static {}, Ljava/util/Calendar;->getInstance()Ljava/util/Calendar;

    move-result-object v0

    iput-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    .line 23
    invoke-virtual {p0, p1}, Lnet/sunniwell/common/tools/DateTime;->setYear(I)V

    .line 24
    invoke-virtual {p0, p2}, Lnet/sunniwell/common/tools/DateTime;->setMonth(I)V

    .line 25
    invoke-virtual {p0, p3}, Lnet/sunniwell/common/tools/DateTime;->setDate(I)V

    .line 26
    invoke-virtual {p0, p4}, Lnet/sunniwell/common/tools/DateTime;->setHours(I)V

    .line 27
    invoke-virtual {p0, p5}, Lnet/sunniwell/common/tools/DateTime;->setMinutes(I)V

    .line 28
    invoke-virtual {p0, p6}, Lnet/sunniwell/common/tools/DateTime;->setSeconds(I)V

    .line 29
    return-void
.end method


# virtual methods
.method public format(Ljava/lang/String;)Ljava/lang/String;
    .locals 4
    .param p1, "format"    # Ljava/lang/String;

    .prologue
    .line 200
    if-nez p1, :cond_0

    .line 201
    const-string v2, ""

    .line 204
    :goto_0
    return-object v2

    .line 202
    :cond_0
    new-instance v1, Ljava/text/SimpleDateFormat;

    invoke-direct {v1, p1}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;)V

    .line 203
    .local v1, "dateformat":Ljava/text/SimpleDateFormat;
    new-instance v0, Ljava/util/Date;

    invoke-virtual {p0}, Lnet/sunniwell/common/tools/DateTime;->getTime()J

    move-result-wide v2

    invoke-direct {v0, v2, v3}, Ljava/util/Date;-><init>(J)V

    .line 204
    .local v0, "d":Ljava/util/Date;
    invoke-virtual {v1, v0}, Ljava/text/SimpleDateFormat;->format(Ljava/util/Date;)Ljava/lang/String;

    move-result-object v2

    goto :goto_0
.end method

.method public formatDefault()Ljava/lang/String;
    .locals 1

    .prologue
    .line 190
    const-string v0, "yyyy-MM-dd hh:mm:ss.SSS"

    invoke-virtual {p0, v0}, Lnet/sunniwell/common/tools/DateTime;->format(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public getDate()I
    .locals 2

    .prologue
    .line 73
    iget-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    const/4 v1, 0x5

    invoke-virtual {v0, v1}, Ljava/util/Calendar;->get(I)I

    move-result v0

    return v0
.end method

.method public getHours()I
    .locals 2

    .prologue
    .line 91
    iget-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    const/16 v1, 0xb

    invoke-virtual {v0, v1}, Ljava/util/Calendar;->get(I)I

    move-result v0

    return v0
.end method

.method public getMillisecond()I
    .locals 2

    .prologue
    .line 145
    iget-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    const/16 v1, 0xe

    invoke-virtual {v0, v1}, Ljava/util/Calendar;->get(I)I

    move-result v0

    return v0
.end method

.method public getMinutes()I
    .locals 2

    .prologue
    .line 109
    iget-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    const/16 v1, 0xc

    invoke-virtual {v0, v1}, Ljava/util/Calendar;->get(I)I

    move-result v0

    return v0
.end method

.method public getMonth()I
    .locals 2

    .prologue
    .line 55
    iget-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    const/4 v1, 0x2

    invoke-virtual {v0, v1}, Ljava/util/Calendar;->get(I)I

    move-result v0

    add-int/lit8 v0, v0, 0x1

    return v0
.end method

.method public getSeconds()I
    .locals 2

    .prologue
    .line 127
    iget-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    const/16 v1, 0xd

    invoke-virtual {v0, v1}, Ljava/util/Calendar;->get(I)I

    move-result v0

    return v0
.end method

.method public getTime()J
    .locals 2

    .prologue
    .line 163
    iget-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    invoke-virtual {v0}, Ljava/util/Calendar;->getTime()Ljava/util/Date;

    move-result-object v0

    invoke-virtual {v0}, Ljava/util/Date;->getTime()J

    move-result-wide v0

    return-wide v0
.end method

.method public getTimeZone()I
    .locals 2

    .prologue
    .line 181
    iget-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    const/16 v1, 0xf

    invoke-virtual {v0, v1}, Ljava/util/Calendar;->get(I)I

    move-result v0

    const v1, 0x36ee80

    div-int/2addr v0, v1

    return v0
.end method

.method public getYear()I
    .locals 2

    .prologue
    .line 37
    iget-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    const/4 v1, 0x1

    invoke-virtual {v0, v1}, Ljava/util/Calendar;->get(I)I

    move-result v0

    return v0
.end method

.method public setDate(I)V
    .locals 2
    .param p1, "date"    # I

    .prologue
    .line 82
    iget-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    const/4 v1, 0x5

    invoke-virtual {v0, v1, p1}, Ljava/util/Calendar;->set(II)V

    .line 83
    return-void
.end method

.method public setHours(I)V
    .locals 2
    .param p1, "hours"    # I

    .prologue
    .line 100
    iget-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    const/16 v1, 0xb

    invoke-virtual {v0, v1, p1}, Ljava/util/Calendar;->set(II)V

    .line 101
    return-void
.end method

.method public setMillisecond(I)V
    .locals 2
    .param p1, "milisecond"    # I

    .prologue
    .line 154
    iget-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    const/16 v1, 0xe

    invoke-virtual {v0, v1, p1}, Ljava/util/Calendar;->set(II)V

    .line 155
    return-void
.end method

.method public setMinutes(I)V
    .locals 2
    .param p1, "minutes"    # I

    .prologue
    .line 118
    iget-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    const/16 v1, 0xc

    invoke-virtual {v0, v1, p1}, Ljava/util/Calendar;->set(II)V

    .line 119
    return-void
.end method

.method public setMonth(I)V
    .locals 3
    .param p1, "month"    # I

    .prologue
    .line 64
    iget-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    const/4 v1, 0x2

    add-int/lit8 v2, p1, -0x1

    invoke-virtual {v0, v1, v2}, Ljava/util/Calendar;->set(II)V

    .line 65
    return-void
.end method

.method public setSeconds(I)V
    .locals 2
    .param p1, "seconds"    # I

    .prologue
    .line 136
    iget-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    const/16 v1, 0xd

    invoke-virtual {v0, v1, p1}, Ljava/util/Calendar;->set(II)V

    .line 137
    return-void
.end method

.method public setTime(J)V
    .locals 2
    .param p1, "time"    # J

    .prologue
    .line 172
    iget-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    new-instance v1, Ljava/util/Date;

    invoke-direct {v1, p1, p2}, Ljava/util/Date;-><init>(J)V

    invoke-virtual {v0, v1}, Ljava/util/Calendar;->setTime(Ljava/util/Date;)V

    .line 173
    return-void
.end method

.method public setYear(I)V
    .locals 2
    .param p1, "year"    # I

    .prologue
    .line 46
    iget-object v0, p0, Lnet/sunniwell/common/tools/DateTime;->can:Ljava/util/Calendar;

    const/4 v1, 0x1

    invoke-virtual {v0, v1, p1}, Ljava/util/Calendar;->set(II)V

    .line 47
    return-void
.end method
