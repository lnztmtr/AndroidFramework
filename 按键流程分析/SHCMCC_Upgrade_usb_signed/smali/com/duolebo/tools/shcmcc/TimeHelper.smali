.class public Lcom/duolebo/tools/shcmcc/TimeHelper;
.super Ljava/lang/Object;
.source "TimeHelper.java"


# static fields
.field public static final JAVA_DATE_FORAMTER_1:Ljava/lang/String; = "yyyy-MM-dd"

.field public static final JAVA_DATE_FORAMTER_2:Ljava/lang/String; = "yyyyMMdd"

.field public static final JAVA_DATE_FORAMTER_3:Ljava/lang/String; = "yyyy-MM"

.field public static final JAVA_DATE_FORAMTER_MONTH:Ljava/lang/String; = "yyyyMM"

.field public static final JAVA_TIME_FORAMTER_1:Ljava/lang/String; = "yyyy-MM-dd HH:mm"

.field public static final JAVA_TIME_FORAMTER_2:Ljava/lang/String; = "yyyy-MM-dd HH:mm:ss"

.field public static final JAVA_TIME_FORAMTER_3:Ljava/lang/String; = "yyyyMMddHHmm"

.field public static final JAVA_TIME_FORAMTER_4:Ljava/lang/String; = "yyyyMMddHHmmss"

.field public static final JAVA_TIME_FORAMTER_5:Ljava/lang/String; = "HH:mm"

.field public static final JAVA_TIME_FORAMTER_6:Ljava/lang/String; = "HH:mm:ss"

.field public static final JAVA_TIME_FORAMTER_7:Ljava/lang/String; = "yyyy-MM-dd@HH:mm"

.field public static final JAVA_TIME_FORAMTER_8:Ljava/lang/String; = "yyyy-MM-dd-HHmmss"

.field public static final JAVA_TIME_FORAMTER_9:Ljava/lang/String; = "yyMMddHHmmss"

.field public static final SQL_DATE_FORAMTER_1:Ljava/lang/String; = "yyyy-mm-dd"

.field public static final SQL_DATE_FORAMTER_2:Ljava/lang/String; = "yyyymmdd"

.field public static final SQL_TIME_FORAMTER_1:Ljava/lang/String; = "yyyy-mm-dd hh24:mi"

.field public static final SQL_TIME_FORAMTER_2:Ljava/lang/String; = "yyyy-mm-dd hh24:mi:ss"

.field public static final SQL_TIME_FORAMTER_3:Ljava/lang/String; = "yyyymmddhh24mi"

.field public static final SQL_TIME_FORAMTER_4:Ljava/lang/String; = "yyyyMMddHHmmss"

.field public static final SQL_TIME_FORAMTER_5:Ljava/lang/String; = "hh24mi"

.field public static final SQL_TIME_FORAMTER_6:Ljava/lang/String; = "hh24miss"


# direct methods
.method public constructor <init>()V
    .locals 0

    .prologue
    .line 13
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static calculateDays(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
    .locals 1
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "dateStr1"    # Ljava/lang/String;
    .param p2, "dateStr2"    # Ljava/lang/String;

    .prologue
    .line 294
    invoke-static {p0, p1, p0, p2}, Lcom/duolebo/tools/shcmcc/TimeHelper;->calculateDays(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I

    move-result v0

    return v0
.end method

.method public static calculateDays(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
    .locals 7
    .param p0, "formaterStr1"    # Ljava/lang/String;
    .param p1, "dateStr1"    # Ljava/lang/String;
    .param p2, "formaterStr2"    # Ljava/lang/String;
    .param p3, "dateStr2"    # Ljava/lang/String;

    .prologue
    .line 283
    invoke-static {p0, p1}, Lcom/duolebo/tools/shcmcc/TimeHelper;->str2date(Ljava/lang/String;Ljava/lang/String;)Ljava/util/Date;

    move-result-object v0

    .line 284
    .local v0, "date1":Ljava/util/Date;
    invoke-static {p2, p3}, Lcom/duolebo/tools/shcmcc/TimeHelper;->str2date(Ljava/lang/String;Ljava/lang/String;)Ljava/util/Date;

    move-result-object v1

    .line 285
    .local v1, "date2":Ljava/util/Date;
    invoke-virtual {v1}, Ljava/util/Date;->getTime()J

    move-result-wide v3

    invoke-virtual {v0}, Ljava/util/Date;->getTime()J

    move-result-wide v5

    sub-long/2addr v3, v5

    const-wide/32 v5, 0x5265c00

    div-long/2addr v3, v5

    long-to-int v3, v3

    add-int/lit8 v2, v3, 0x1

    .line 286
    .local v2, "days":I
    return v2
.end method

.method public static calculateDaysToString(Ljava/util/Date;Ljava/util/Date;)Ljava/lang/String;
    .locals 8
    .param p0, "date1"    # Ljava/util/Date;
    .param p1, "date2"    # Ljava/util/Date;

    .prologue
    const-wide/32 v6, 0x36ee80

    .line 579
    invoke-virtual {p1}, Ljava/util/Date;->getTime()J

    move-result-wide v2

    invoke-virtual {p0}, Ljava/util/Date;->getTime()J

    move-result-wide v4

    sub-long v0, v2, v4

    .line 580
    .local v0, "calculates":J
    new-instance v2, Ljava/lang/StringBuilder;

    div-long v3, v0, v6

    invoke-static {v3, v4}, Ljava/lang/String;->valueOf(J)Ljava/lang/String;

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    const-string v3, "\u5c0f\u65f6"

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    .line 581
    rem-long v3, v0, v6

    const-wide/32 v5, 0xea60

    div-long/2addr v3, v5

    invoke-virtual {v2, v3, v4}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    move-result-object v2

    const-string v3, "\u5206\u949f"

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    .line 580
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    return-object v2
.end method

.method public static calculateWeek(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .locals 4
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "param"    # Ljava/lang/String;

    .prologue
    .line 272
    invoke-static {p0, p1}, Lcom/duolebo/tools/shcmcc/TimeHelper;->str2date(Ljava/lang/String;Ljava/lang/String;)Ljava/util/Date;

    move-result-object v0

    .line 273
    .local v0, "date":Ljava/util/Date;
    new-instance v1, Ljava/text/SimpleDateFormat;

    const-string v3, "E"

    invoke-direct {v1, v3}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;)V

    .line 274
    .local v1, "tf":Ljava/text/DateFormat;
    invoke-virtual {v1, v0}, Ljava/text/DateFormat;->format(Ljava/util/Date;)Ljava/lang/String;

    move-result-object v2

    .line 275
    .local v2, "weekStr":Ljava/lang/String;
    invoke-virtual {v2}, Ljava/lang/String;->length()I

    move-result v3

    add-int/lit8 v3, v3, -0x1

    invoke-virtual {v2, v3}, Ljava/lang/String;->substring(I)Ljava/lang/String;

    move-result-object v3

    return-object v3
.end method

.method public static date2str(Ljava/lang/String;Ljava/util/Date;)Ljava/lang/String;
    .locals 2
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "date"    # Ljava/util/Date;

    .prologue
    .line 158
    new-instance v0, Ljava/text/SimpleDateFormat;

    invoke-direct {v0, p0}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;)V

    .line 159
    .local v0, "tf":Ljava/text/DateFormat;
    invoke-virtual {v0, p1}, Ljava/text/DateFormat;->format(Ljava/util/Date;)Ljava/lang/String;

    move-result-object v1

    return-object v1
.end method

.method public static datechange(Ljava/lang/String;)Ljava/lang/String;
    .locals 3
    .param p0, "date"    # Ljava/lang/String;

    .prologue
    .line 568
    const-string v0, "-"

    const-string v1, ""

    invoke-virtual {p0, v0, v1}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    const-string v1, ":"

    const-string v2, ""

    invoke-virtual {v0, v1, v2}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 569
    const-string v1, " "

    const-string v2, ""

    invoke-virtual {v0, v1, v2}, Ljava/lang/String;->replaceAll(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;

    move-result-object v0

    .line 568
    return-object v0
.end method

.method public static formaterTime(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .locals 1
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "time"    # Ljava/lang/String;
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/text/ParseException;
        }
    .end annotation

    .prologue
    .line 389
    invoke-static {p0, p1, p0}, Lcom/duolebo/tools/shcmcc/TimeHelper;->formaterTime(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public static formaterTime(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .locals 1
    .param p0, "proFmtStr"    # Ljava/lang/String;
    .param p1, "time"    # Ljava/lang/String;
    .param p2, "afterFmtStr"    # Ljava/lang/String;

    .prologue
    .line 375
    invoke-static {p0, p1}, Lcom/duolebo/tools/shcmcc/TimeHelper;->str2date(Ljava/lang/String;Ljava/lang/String;)Ljava/util/Date;

    move-result-object v0

    invoke-static {p2, v0}, Lcom/duolebo/tools/shcmcc/TimeHelper;->date2str(Ljava/lang/String;Ljava/util/Date;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public static getCurrentDate()Ljava/util/Date;
    .locals 1

    .prologue
    .line 167
    invoke-static {}, Ljava/util/Calendar;->getInstance()Ljava/util/Calendar;

    move-result-object v0

    invoke-virtual {v0}, Ljava/util/Calendar;->getTime()Ljava/util/Date;

    move-result-object v0

    return-object v0
.end method

.method public static getCurrentDateNotHasDelay()Ljava/lang/String;
    .locals 1

    .prologue
    .line 184
    const-string v0, "yyyy-MM-dd HH:mm:ss"

    invoke-static {v0}, Lcom/duolebo/tools/shcmcc/TimeHelper;->getCurrentDateNotHasDelay(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public static getCurrentDateNotHasDelay(Ljava/lang/String;)Ljava/lang/String;
    .locals 1
    .param p0, "formaterStr"    # Ljava/lang/String;

    .prologue
    .line 176
    invoke-static {}, Lcom/duolebo/tools/shcmcc/TimeHelper;->getCurrentDate()Ljava/util/Date;

    move-result-object v0

    invoke-static {p0, v0}, Lcom/duolebo/tools/shcmcc/TimeHelper;->date2str(Ljava/lang/String;Ljava/util/Date;)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public static getDBDate(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .locals 3
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "dbName"    # Ljava/lang/String;

    .prologue
    .line 339
    new-instance v1, Ljava/lang/StringBuilder;

    const-string v2, "select to_char(sysdate, \'"

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v1, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    .line 340
    const-string v2, "\') dbtime from dual"

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    .line 339
    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    .line 347
    .local v0, "sqlTime":Ljava/lang/String;
    const-string v1, ""

    return-object v1
.end method

.method public static getDay4Month(Ljava/lang/String;Ljava/lang/String;)I
    .locals 2
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "time"    # Ljava/lang/String;

    .prologue
    .line 473
    invoke-static {}, Ljava/util/Calendar;->getInstance()Ljava/util/Calendar;

    move-result-object v0

    .line 474
    .local v0, "rightNow":Ljava/util/Calendar;
    invoke-static {p0, p1}, Lcom/duolebo/tools/shcmcc/TimeHelper;->str2date(Ljava/lang/String;Ljava/lang/String;)Ljava/util/Date;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/util/Calendar;->setTime(Ljava/util/Date;)V

    .line 475
    const/4 v1, 0x5

    invoke-virtual {v0, v1}, Ljava/util/Calendar;->getActualMaximum(I)I

    move-result v1

    return v1
.end method

.method public static getDay4TwainMonth(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I
    .locals 5
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "time"    # Ljava/lang/String;
    .param p2, "formaterStr1"    # Ljava/lang/String;
    .param p3, "time1"    # Ljava/lang/String;

    .prologue
    .line 483
    const/4 v0, 0x0

    .line 484
    .local v0, "days":I
    const-string v4, "yyyy-MM"

    invoke-static {p0, p1, v4}, Lcom/duolebo/tools/shcmcc/TimeHelper;->formaterTime(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v3

    .line 485
    .local v3, "iTime":Ljava/lang/String;
    const-string v4, "yyyy-MM"

    invoke-static {p2, p3, v4}, Lcom/duolebo/tools/shcmcc/TimeHelper;->formaterTime(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v2

    .line 486
    .local v2, "endTime":Ljava/lang/String;
    const-string v4, "yyyy-MM"

    invoke-static {v4, v3, v2}, Lcom/duolebo/tools/shcmcc/TimeHelper;->time1LTEQTime2(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z

    move-result v4

    if-nez v4, :cond_0

    move v1, v0

    .line 494
    .end local v0    # "days":I
    .local v1, "days":I
    :goto_0
    return v1

    .line 491
    .end local v1    # "days":I
    .restart local v0    # "days":I
    :cond_0
    const-string v4, "yyyy-MM"

    invoke-static {v4, v3}, Lcom/duolebo/tools/shcmcc/TimeHelper;->getDay4Month(Ljava/lang/String;Ljava/lang/String;)I

    move-result v4

    add-int/2addr v0, v4

    .line 492
    const-string v4, "yyyy-MM"

    invoke-static {v4, v3}, Lcom/duolebo/tools/shcmcc/TimeHelper;->getNextMonth(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v3

    .line 493
    invoke-virtual {v3, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v4

    if-nez v4, :cond_0

    move v1, v0

    .line 494
    .end local v0    # "days":I
    .restart local v1    # "days":I
    goto :goto_0
.end method

.method public static getMonthFirstDay(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .locals 3
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "time"    # Ljava/lang/String;

    .prologue
    .line 548
    const-string v1, "yyyy-MM"

    invoke-static {p0, p1, v1}, Lcom/duolebo/tools/shcmcc/TimeHelper;->formaterTime(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 549
    .local v0, "m":Ljava/lang/String;
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-static {v0}, Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v2

    invoke-direct {v1, v2}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    const-string v2, "-01"

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    return-object v1
.end method

.method public static getNextMonth(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .locals 6
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "time"    # Ljava/lang/String;

    .prologue
    .line 504
    const-string v4, "yyyy-MM"

    invoke-static {p0, p1, v4}, Lcom/duolebo/tools/shcmcc/TimeHelper;->formaterTime(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 505
    .local v0, "m":Ljava/lang/String;
    const-string v4, "-"

    invoke-virtual {v0, v4}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v4

    const/4 v5, 0x0

    aget-object v4, v4, v5

    invoke-static {v4}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    move-result v3

    .line 506
    .local v3, "y":I
    const-string v4, "-"

    invoke-virtual {v0, v4}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v4

    const/4 v5, 0x1

    aget-object v4, v4, v5

    invoke-static {v4}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    move-result v4

    add-int/lit8 v2, v4, 0x1

    .line 507
    .local v2, "nm":I
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-static {v2}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v5

    invoke-direct {v4, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    .line 508
    .local v1, "nextMonth":Ljava/lang/String;
    const/16 v4, 0xa

    if-ge v2, v4, :cond_1

    .line 509
    new-instance v4, Ljava/lang/StringBuilder;

    const-string v5, "0"

    invoke-direct {v4, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    .line 516
    :cond_0
    :goto_0
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-static {v3}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v5

    invoke-direct {v4, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    const-string v5, "-"

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    return-object v4

    .line 511
    :cond_1
    const/16 v4, 0xd

    if-ne v2, v4, :cond_0

    .line 512
    add-int/lit8 v3, v3, 0x1

    .line 513
    const-string v1, "01"

    goto :goto_0
.end method

.method public static getPreMonth(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .locals 6
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "time"    # Ljava/lang/String;

    .prologue
    .line 526
    const-string v4, "yyyy-MM"

    invoke-static {p0, p1, v4}, Lcom/duolebo/tools/shcmcc/TimeHelper;->formaterTime(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 527
    .local v0, "m":Ljava/lang/String;
    const-string v4, "-"

    invoke-virtual {v0, v4}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v4

    const/4 v5, 0x0

    aget-object v4, v4, v5

    invoke-static {v4}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    move-result v3

    .line 528
    .local v3, "y":I
    const-string v4, "-"

    invoke-virtual {v0, v4}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v4

    const/4 v5, 0x1

    aget-object v4, v4, v5

    invoke-static {v4}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I

    move-result v4

    add-int/lit8 v2, v4, -0x1

    .line 529
    .local v2, "pm":I
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-static {v2}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v5

    invoke-direct {v4, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    .line 530
    .local v1, "nextMonth":Ljava/lang/String;
    const/16 v4, 0xa

    if-ge v2, v4, :cond_1

    .line 531
    new-instance v4, Ljava/lang/StringBuilder;

    const-string v5, "0"

    invoke-direct {v4, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v1

    .line 538
    :cond_0
    :goto_0
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-static {v3}, Ljava/lang/String;->valueOf(I)Ljava/lang/String;

    move-result-object v5

    invoke-direct {v4, v5}, Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V

    const-string v5, "-"

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    return-object v4

    .line 533
    :cond_1
    if-nez v2, :cond_0

    .line 534
    add-int/lit8 v3, v3, -0x1

    .line 535
    const-string v1, "01"

    goto :goto_0
.end method

.method public static getPreMonthLastDay(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
    .locals 3
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "time"    # Ljava/lang/String;

    .prologue
    const/4 v2, 0x5

    .line 559
    invoke-static {}, Ljava/util/Calendar;->getInstance()Ljava/util/Calendar;

    move-result-object v0

    .line 560
    .local v0, "rightNow":Ljava/util/Calendar;
    invoke-static {p0, p1}, Lcom/duolebo/tools/shcmcc/TimeHelper;->str2date(Ljava/lang/String;Ljava/lang/String;)Ljava/util/Date;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/util/Calendar;->setTime(Ljava/util/Date;)V

    .line 561
    const/4 v1, 0x1

    invoke-virtual {v0, v2, v1}, Ljava/util/Calendar;->set(II)V

    .line 562
    const/4 v1, -0x1

    invoke-virtual {v0, v2, v1}, Ljava/util/Calendar;->add(II)V

    .line 563
    const-string v1, "yyyy-MM-dd"

    invoke-virtual {v0}, Ljava/util/Calendar;->getTime()Ljava/util/Date;

    move-result-object v2

    invoke-static {v1, v2}, Lcom/duolebo/tools/shcmcc/TimeHelper;->date2str(Ljava/lang/String;Ljava/util/Date;)Ljava/lang/String;

    move-result-object v1

    return-object v1
.end method

.method public static isFirstDay4Month()Z
    .locals 4

    .prologue
    .line 421
    const-string v1, "yyyy-MM-dd"

    invoke-static {v1}, Lcom/duolebo/tools/shcmcc/TimeHelper;->getCurrentDateNotHasDelay(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 422
    .local v0, "now":Ljava/lang/String;
    const-string v1, "01"

    const-string v2, "-"

    invoke-virtual {v0, v2}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;

    move-result-object v2

    const/4 v3, 0x2

    aget-object v2, v2, v3

    invoke-virtual {v1, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_0

    .line 423
    const/4 v1, 0x1

    .line 425
    :goto_0
    return v1

    :cond_0
    const/4 v1, 0x0

    goto :goto_0
.end method

.method public static isSunday()Z
    .locals 2

    .prologue
    .line 435
    const-string v0, "yyyy-MM-dd"

    .line 436
    const-string v1, "yyyy-MM-dd"

    invoke-static {v1}, Lcom/duolebo/tools/shcmcc/TimeHelper;->getCurrentDateNotHasDelay(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    .line 435
    invoke-static {v0, v1}, Lcom/duolebo/tools/shcmcc/TimeHelper;->isSunday(Ljava/lang/String;Ljava/lang/String;)Z

    move-result v0

    return v0
.end method

.method public static isSunday(Ljava/lang/String;Ljava/lang/String;)Z
    .locals 2
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "day"    # Ljava/lang/String;

    .prologue
    .line 446
    invoke-static {p0, p1}, Lcom/duolebo/tools/shcmcc/TimeHelper;->calculateWeek(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 447
    .local v0, "week":Ljava/lang/String;
    const-string v1, "\u65e5"

    invoke-virtual {v1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-eqz v1, :cond_0

    .line 448
    const/4 v1, 0x1

    .line 450
    :goto_0
    return v1

    :cond_0
    const/4 v1, 0x0

    goto :goto_0
.end method

.method public static isToday(Ljava/lang/String;Ljava/lang/String;)Z
    .locals 3
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "time"    # Ljava/lang/String;

    .prologue
    .line 460
    const-string v2, "yyyy-MM-dd"

    invoke-static {p0, p1, v2}, Lcom/duolebo/tools/shcmcc/TimeHelper;->formaterTime(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;

    move-result-object v0

    .line 461
    .local v0, "day":Ljava/lang/String;
    const-string v2, "yyyy-MM-dd"

    invoke-static {v2}, Lcom/duolebo/tools/shcmcc/TimeHelper;->getCurrentDateNotHasDelay(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v1

    .line 462
    .local v1, "today":Ljava/lang/String;
    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_0

    .line 463
    const/4 v2, 0x1

    .line 465
    :goto_0
    return v2

    :cond_0
    const/4 v2, 0x0

    goto :goto_0
.end method

.method public static isWithin(JJ)Z
    .locals 2
    .param p0, "oldTime"    # J
    .param p2, "duration"    # J

    .prologue
    .line 148
    invoke-static {}, Ljava/lang/System;->currentTimeMillis()J

    move-result-wide v0

    sub-long/2addr v0, p0

    invoke-static {v0, v1}, Ljava/lang/Math;->abs(J)J

    move-result-wide v0

    cmp-long v0, v0, p2

    if-gez v0, :cond_0

    const/4 v0, 0x1

    :goto_0
    return v0

    :cond_0
    const/4 v0, 0x0

    goto :goto_0
.end method

.method public static long2time(Ljava/lang/String;J)Ljava/lang/String;
    .locals 2
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "timeLong"    # J

    .prologue
    .line 228
    new-instance v0, Ljava/text/SimpleDateFormat;

    invoke-direct {v0, p0}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;)V

    .line 229
    .local v0, "tf":Ljava/text/DateFormat;
    new-instance v1, Ljava/util/Date;

    invoke-direct {v1, p1, p2}, Ljava/util/Date;-><init>(J)V

    invoke-virtual {v0, v1}, Ljava/text/DateFormat;->format(Ljava/util/Date;)Ljava/lang/String;

    move-result-object v1

    return-object v1
.end method

.method public static main([Ljava/lang/String;)V
    .locals 3
    .param p0, "args"    # [Ljava/lang/String;

    .prologue
    .line 573
    sget-object v0, Ljava/lang/System;->out:Ljava/io/PrintStream;

    .line 574
    const-string v1, "yyyy-MM-dd HH:mm:ss"

    const-string v2, "2011-03-19 09:30:30"

    invoke-static {v1, v2}, Lcom/duolebo/tools/shcmcc/TimeHelper;->str2date(Ljava/lang/String;Ljava/lang/String;)Ljava/util/Date;

    move-result-object v1

    .line 575
    new-instance v2, Ljava/util/Date;

    invoke-direct {v2}, Ljava/util/Date;-><init>()V

    .line 573
    invoke-static {v1, v2}, Lcom/duolebo/tools/shcmcc/TimeHelper;->calculateDaysToString(Ljava/util/Date;Ljava/util/Date;)Ljava/lang/String;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    .line 576
    return-void
.end method

.method public static setDate(Ljava/util/Date;II)Ljava/util/Date;
    .locals 2
    .param p0, "date"    # Ljava/util/Date;
    .param p1, "field"    # I
    .param p2, "value"    # I

    .prologue
    .line 408
    invoke-static {}, Ljava/util/Calendar;->getInstance()Ljava/util/Calendar;

    move-result-object v0

    .line 409
    .local v0, "calendar":Ljava/util/Calendar;
    invoke-virtual {v0, p0}, Ljava/util/Calendar;->setTime(Ljava/util/Date;)V

    .line 410
    invoke-virtual {v0, p1, p2}, Ljava/util/Calendar;->set(II)V

    .line 411
    invoke-virtual {v0}, Ljava/util/Calendar;->getTime()Ljava/util/Date;

    move-result-object v1

    return-object v1
.end method

.method public static str2date(Ljava/lang/String;Ljava/lang/String;)Ljava/util/Date;
    .locals 3
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "param"    # Ljava/lang/String;

    .prologue
    .line 194
    new-instance v2, Ljava/text/SimpleDateFormat;

    invoke-direct {v2, p0}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;)V

    .line 195
    .local v2, "tf":Ljava/text/DateFormat;
    const/4 v0, 0x0

    .line 197
    .local v0, "date":Ljava/util/Date;
    :try_start_0
    invoke-virtual {v2, p1}, Ljava/text/DateFormat;->parse(Ljava/lang/String;)Ljava/util/Date;
    :try_end_0
    .catch Ljava/text/ParseException; {:try_start_0 .. :try_end_0} :catch_0

    move-result-object v0

    .line 201
    :goto_0
    return-object v0

    .line 198
    :catch_0
    move-exception v1

    .line 199
    .local v1, "e":Ljava/text/ParseException;
    invoke-virtual {v1}, Ljava/text/ParseException;->printStackTrace()V

    goto :goto_0
.end method

.method public static time1LTEQTime2(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z
    .locals 1
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "timeStr1"    # Ljava/lang/String;
    .param p2, "timeStr2"    # Ljava/lang/String;

    .prologue
    .line 355
    invoke-static {p0, p1, p0, p2}, Lcom/duolebo/tools/shcmcc/TimeHelper;->time1LTEQTime2(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z

    move-result v0

    return v0
.end method

.method public static time1LTEQTime2(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z
    .locals 6
    .param p0, "formaterStr1"    # Ljava/lang/String;
    .param p1, "timeStr1"    # Ljava/lang/String;
    .param p2, "formaterStr2"    # Ljava/lang/String;
    .param p3, "timeStr2"    # Ljava/lang/String;

    .prologue
    .line 317
    invoke-static {p0, p1}, Lcom/duolebo/tools/shcmcc/TimeHelper;->str2date(Ljava/lang/String;Ljava/lang/String;)Ljava/util/Date;

    move-result-object v0

    .line 318
    .local v0, "date1":Ljava/util/Date;
    invoke-static {p2, p3}, Lcom/duolebo/tools/shcmcc/TimeHelper;->str2date(Ljava/lang/String;Ljava/lang/String;)Ljava/util/Date;

    move-result-object v1

    .line 319
    .local v1, "date2":Ljava/util/Date;
    invoke-virtual {v1}, Ljava/util/Date;->getTime()J

    move-result-wide v2

    invoke-virtual {v0}, Ljava/util/Date;->getTime()J

    move-result-wide v4

    sub-long/2addr v2, v4

    const-wide/16 v4, 0x0

    cmp-long v2, v2, v4

    if-ltz v2, :cond_0

    .line 320
    const/4 v2, 0x1

    .line 322
    :goto_0
    return v2

    :cond_0
    const/4 v2, 0x0

    goto :goto_0
.end method

.method public static time1LTTime2(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z
    .locals 1
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "timeStr1"    # Ljava/lang/String;
    .param p2, "timeStr2"    # Ljava/lang/String;
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/text/ParseException;
        }
    .end annotation

    .prologue
    .line 331
    invoke-static {p0, p1, p0, p2}, Lcom/duolebo/tools/shcmcc/TimeHelper;->time1LTTime2(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z

    move-result v0

    return v0
.end method

.method public static time1LTTime2(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z
    .locals 6
    .param p0, "formaterStr1"    # Ljava/lang/String;
    .param p1, "timeStr1"    # Ljava/lang/String;
    .param p2, "formaterStr2"    # Ljava/lang/String;
    .param p3, "timeStr2"    # Ljava/lang/String;

    .prologue
    .line 303
    invoke-static {p0, p1}, Lcom/duolebo/tools/shcmcc/TimeHelper;->str2date(Ljava/lang/String;Ljava/lang/String;)Ljava/util/Date;

    move-result-object v0

    .line 304
    .local v0, "date1":Ljava/util/Date;
    invoke-static {p2, p3}, Lcom/duolebo/tools/shcmcc/TimeHelper;->str2date(Ljava/lang/String;Ljava/lang/String;)Ljava/util/Date;

    move-result-object v1

    .line 305
    .local v1, "date2":Ljava/util/Date;
    invoke-virtual {v1}, Ljava/util/Date;->getTime()J

    move-result-wide v2

    invoke-virtual {v0}, Ljava/util/Date;->getTime()J

    move-result-wide v4

    sub-long/2addr v2, v4

    const-wide/16 v4, 0x0

    cmp-long v2, v2, v4

    if-lez v2, :cond_0

    .line 306
    const/4 v2, 0x1

    .line 308
    :goto_0
    return v2

    :cond_0
    const/4 v2, 0x0

    goto :goto_0
.end method

.method public static time2long(Ljava/lang/String;Ljava/lang/String;)J
    .locals 5
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "time"    # Ljava/lang/String;

    .prologue
    .line 211
    new-instance v1, Ljava/text/SimpleDateFormat;

    invoke-direct {v1, p0}, Ljava/text/SimpleDateFormat;-><init>(Ljava/lang/String;)V

    .line 212
    .local v1, "tf":Ljava/text/DateFormat;
    const-wide/16 v2, 0x0

    .line 214
    .local v2, "var":J
    :try_start_0
    invoke-virtual {v1, p1}, Ljava/text/DateFormat;->parse(Ljava/lang/String;)Ljava/util/Date;

    move-result-object v4

    invoke-virtual {v4}, Ljava/util/Date;->getTime()J
    :try_end_0
    .catch Ljava/text/ParseException; {:try_start_0 .. :try_end_0} :catch_0

    move-result-wide v2

    .line 218
    :goto_0
    return-wide v2

    .line 215
    :catch_0
    move-exception v0

    .line 216
    .local v0, "e":Ljava/text/ParseException;
    invoke-virtual {v0}, Ljava/text/ParseException;->printStackTrace()V

    goto :goto_0
.end method

.method public static timeAfterDays(Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/String;
    .locals 1
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "time"    # Ljava/lang/String;
    .param p2, "days"    # I

    .prologue
    .line 240
    mul-int/lit8 v0, p2, 0x18

    invoke-static {p0, p1, v0}, Lcom/duolebo/tools/shcmcc/TimeHelper;->timeAfterHours(Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public static timeAfterHours(Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/String;
    .locals 1
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "time"    # Ljava/lang/String;
    .param p2, "hours"    # I

    .prologue
    .line 251
    mul-int/lit8 v0, p2, 0x3c

    invoke-static {p0, p1, v0}, Lcom/duolebo/tools/shcmcc/TimeHelper;->timeAfterMinute(Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/String;

    move-result-object v0

    return-object v0
.end method

.method public static timeAfterMinute(Ljava/lang/String;Ljava/lang/String;I)Ljava/lang/String;
    .locals 12
    .param p0, "formaterStr"    # Ljava/lang/String;
    .param p1, "time"    # Ljava/lang/String;
    .param p2, "minute"    # I

    .prologue
    const-wide/16 v10, 0x3e8

    .line 262
    invoke-static {p0, p1}, Lcom/duolebo/tools/shcmcc/TimeHelper;->time2long(Ljava/lang/String;Ljava/lang/String;)J

    move-result-wide v0

    .line 263
    .local v0, "curTimeLong":J
    div-long v4, v0, v10

    const-wide/16 v6, 0x3c

    int-to-long v8, p2

    mul-long/2addr v6, v8

    add-long/2addr v4, v6

    mul-long v2, v4, v10

    .line 264
    .local v2, "proTimeLong":J
    invoke-static {p0, v2, v3}, Lcom/duolebo/tools/shcmcc/TimeHelper;->long2time(Ljava/lang/String;J)Ljava/lang/String;

    move-result-object v4

    return-object v4
.end method
