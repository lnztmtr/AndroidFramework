.class Lnet/sunniwell/service/swupgrade/usb/UpgradeService$3;
.super Ljava/lang/Object;
.source "UpgradeService.java"

# interfaces
.implements Ljava/lang/Runnable;


# annotations
.annotation system Ldalvik/annotation/EnclosingMethod;
    value = Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->copyfile(Ljava/lang/String;)V
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x0
    name = null
.end annotation


# instance fields
.field final synthetic this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

.field final synthetic val$filePath:Ljava/lang/String;


# direct methods
.method constructor <init>(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;Ljava/lang/String;)V
    .locals 0
    .param p1, "this$0"    # Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    .prologue
    .line 223
    iput-object p1, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$3;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    iput-object p2, p0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$3;->val$filePath:Ljava/lang/String;

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public run()V
    .locals 22

    .prologue
    .line 226
    new-instance v13, Ljava/io/File;

    move-object/from16 v0, p0

    iget-object v0, v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$3;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    move-object/from16 v19, v0

    invoke-static/range {v19 .. v19}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->access$000(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)Ljava/lang/String;

    move-result-object v19

    move-object/from16 v0, v19

    invoke-direct {v13, v0}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    .line 227
    .local v13, "path":Ljava/io/File;
    invoke-virtual {v13}, Ljava/io/File;->exists()Z

    move-result v19

    if-nez v19, :cond_0

    .line 228
    invoke-virtual {v13}, Ljava/io/File;->mkdirs()Z

    .line 230
    :cond_0
    new-instance v5, Ljava/io/File;

    new-instance v19, Ljava/lang/StringBuilder;

    invoke-direct/range {v19 .. v19}, Ljava/lang/StringBuilder;-><init>()V

    move-object/from16 v0, p0

    iget-object v0, v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$3;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    move-object/from16 v20, v0

    invoke-static/range {v20 .. v20}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->access$000(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)Ljava/lang/String;

    move-result-object v20

    invoke-virtual/range {v19 .. v20}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v19

    const-string v20, "usb_update.bak"

    invoke-virtual/range {v19 .. v20}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v19

    invoke-virtual/range {v19 .. v19}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v19

    move-object/from16 v0, v19

    invoke-direct {v5, v0}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    .line 231
    .local v5, "f":Ljava/io/File;
    new-instance v19, Ljava/lang/StringBuilder;

    invoke-direct/range {v19 .. v19}, Ljava/lang/StringBuilder;-><init>()V

    const-string v20, "====save file=="

    invoke-virtual/range {v19 .. v20}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v19

    invoke-virtual {v5}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v20

    invoke-virtual/range {v19 .. v20}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v19

    invoke-virtual/range {v19 .. v19}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v19

    invoke-static/range {v19 .. v19}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 233
    invoke-virtual {v5}, Ljava/io/File;->exists()Z

    move-result v19

    if-eqz v19, :cond_1

    .line 234
    invoke-virtual {v5}, Ljava/io/File;->delete()Z

    .line 237
    :cond_1
    :try_start_0
    invoke-virtual {v5}, Ljava/io/File;->createNewFile()Z
    :try_end_0
    .catch Ljava/io/IOException; {:try_start_0 .. :try_end_0} :catch_2

    .line 241
    :goto_0
    const/4 v6, 0x0

    .line 242
    .local v6, "input":Ljava/io/InputStream;
    const/4 v11, 0x0

    .line 244
    .local v11, "output":Ljava/io/OutputStream;
    new-instance v14, Ljava/io/File;

    move-object/from16 v0, p0

    iget-object v0, v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$3;->val$filePath:Ljava/lang/String;

    move-object/from16 v19, v0

    move-object/from16 v0, v19

    invoke-direct {v14, v0}, Ljava/io/File;-><init>(Ljava/lang/String;)V

    .line 245
    .local v14, "pathFile":Ljava/io/File;
    new-instance v19, Ljava/lang/StringBuilder;

    invoke-direct/range {v19 .. v19}, Ljava/lang/StringBuilder;-><init>()V

    const-string v20, "====================PathFile=="

    invoke-virtual/range {v19 .. v20}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v19

    invoke-virtual {v14}, Ljava/io/File;->length()J

    move-result-wide v20

    invoke-virtual/range {v19 .. v21}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    move-result-object v19

    invoke-virtual/range {v19 .. v19}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v19

    invoke-static/range {v19 .. v19}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 248
    :try_start_1
    new-instance v7, Ljava/io/FileInputStream;

    invoke-direct {v7, v14}, Ljava/io/FileInputStream;-><init>(Ljava/io/File;)V
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_9
    .catchall {:try_start_1 .. :try_end_1} :catchall_1

    .line 249
    .end local v6    # "input":Ljava/io/InputStream;
    .local v7, "input":Ljava/io/InputStream;
    :try_start_2
    new-instance v12, Ljava/io/FileOutputStream;

    invoke-direct {v12, v5}, Ljava/io/FileOutputStream;-><init>(Ljava/io/File;)V
    :try_end_2
    .catch Ljava/lang/Exception; {:try_start_2 .. :try_end_2} :catch_a
    .catchall {:try_start_2 .. :try_end_2} :catchall_2

    .line 250
    .end local v11    # "output":Ljava/io/OutputStream;
    .local v12, "output":Ljava/io/OutputStream;
    :try_start_3
    invoke-virtual {v14}, Ljava/io/File;->length()J

    move-result-wide v15

    .line 251
    .local v15, "totalLen":J
    const-wide/16 v17, 0x0

    .line 252
    .local v17, "writeLen":J
    const v19, 0x32000

    move/from16 v0, v19

    new-array v3, v0, [B

    .line 253
    .local v3, "buffer":[B
    const/4 v10, 0x0

    .line 254
    .local v10, "n":I
    :goto_1
    invoke-virtual {v7, v3}, Ljava/io/InputStream;->read([B)I

    move-result v10

    const/16 v19, -0x1

    move/from16 v0, v19

    if-eq v10, v0, :cond_5

    move-object/from16 v0, p0

    iget-object v0, v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$3;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    move-object/from16 v19, v0

    invoke-static/range {v19 .. v19}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->access$100(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;)Ljava/lang/Boolean;

    move-result-object v19

    invoke-virtual/range {v19 .. v19}, Ljava/lang/Boolean;->booleanValue()Z

    move-result v19

    if-eqz v19, :cond_5

    .line 255
    const/16 v19, 0x0

    move/from16 v0, v19

    invoke-virtual {v12, v3, v0, v10}, Ljava/io/OutputStream;->write([BII)V

    .line 256
    invoke-virtual {v12}, Ljava/io/OutputStream;->flush()V

    .line 257
    new-instance v19, Ljava/lang/StringBuilder;

    invoke-direct/range {v19 .. v19}, Ljava/lang/StringBuilder;-><init>()V

    const-string v20, "=======write len ="

    invoke-virtual/range {v19 .. v20}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v19

    move-object/from16 v0, v19

    move-wide/from16 v1, v17

    invoke-virtual {v0, v1, v2}, Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;

    move-result-object v19

    invoke-virtual/range {v19 .. v19}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v19

    invoke-static/range {v19 .. v19}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 258
    int-to-long v0, v10

    move-wide/from16 v19, v0

    add-long v17, v17, v19

    .line 259
    new-instance v9, Landroid/os/Message;

    invoke-direct {v9}, Landroid/os/Message;-><init>()V

    .line 260
    .local v9, "msg":Landroid/os/Message;
    const/16 v19, 0x3

    move/from16 v0, v19

    iput v0, v9, Landroid/os/Message;->what:I

    .line 261
    move-wide/from16 v0, v17

    long-to-float v0, v0

    move/from16 v19, v0

    const/high16 v20, 0x42c80000    # 100.0f

    mul-float v19, v19, v20

    long-to-float v0, v15

    move/from16 v20, v0

    div-float v19, v19, v20

    move/from16 v0, v19

    float-to-double v0, v0

    move-wide/from16 v19, v0

    invoke-static/range {v19 .. v20}, Ljava/lang/Math;->ceil(D)D

    move-result-wide v19

    move-wide/from16 v0, v19

    double-to-int v0, v0

    move/from16 v19, v0

    move/from16 v0, v19

    iput v0, v9, Landroid/os/Message;->arg1:I

    .line 264
    cmp-long v19, v17, v15

    if-ltz v19, :cond_2

    .line 265
    new-instance v19, Ljava/lang/StringBuilder;

    invoke-direct/range {v19 .. v19}, Ljava/lang/StringBuilder;-><init>()V

    const-string v20, "================\u6587\u4ef6\u4e00\u6837\u5927======3==========="

    invoke-virtual/range {v19 .. v20}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v19

    .line 266
    invoke-virtual {v5}, Ljava/io/File;->getAbsolutePath()Ljava/lang/String;

    move-result-object v20

    invoke-virtual/range {v19 .. v20}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v19

    invoke-virtual/range {v19 .. v19}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v19

    .line 265
    invoke-static/range {v19 .. v19}, Lcom/duolebo/tools/DLBLog;->d(Ljava/lang/String;)V

    .line 267
    move-object/from16 v0, p0

    iget-object v0, v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$3;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    move-object/from16 v19, v0

    const-string v20, "update.zip"

    move-object/from16 v0, v19

    move-object/from16 v1, v20

    invoke-static {v0, v5, v1}, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->access$200(Lnet/sunniwell/service/swupgrade/usb/UpgradeService;Ljava/io/File;Ljava/lang/String;)Z

    move-result v19

    if-eqz v19, :cond_2

    .line 268
    move-object/from16 v0, p0

    iget-object v0, v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$3;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    move-object/from16 v19, v0

    move-object/from16 v0, v19

    iget-object v0, v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->myHandler:Landroid/os/Handler;

    move-object/from16 v19, v0

    const/16 v20, 0x6

    invoke-virtual/range {v19 .. v20}, Landroid/os/Handler;->sendEmptyMessage(I)Z

    .line 272
    :cond_2
    move-object/from16 v0, p0

    iget-object v0, v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$3;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    move-object/from16 v19, v0

    move-object/from16 v0, v19

    iget-object v0, v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->myHandler:Landroid/os/Handler;

    move-object/from16 v19, v0

    move-object/from16 v0, v19

    invoke-virtual {v0, v9}, Landroid/os/Handler;->sendMessage(Landroid/os/Message;)Z
    :try_end_3
    .catch Ljava/lang/Exception; {:try_start_3 .. :try_end_3} :catch_1
    .catchall {:try_start_3 .. :try_end_3} :catchall_0

    .line 274
    const-wide/16 v19, 0x2

    :try_start_4
    invoke-static/range {v19 .. v20}, Ljava/lang/Thread;->sleep(J)V
    :try_end_4
    .catch Ljava/lang/Exception; {:try_start_4 .. :try_end_4} :catch_0
    .catchall {:try_start_4 .. :try_end_4} :catchall_0

    goto/16 :goto_1

    .line 275
    :catch_0
    move-exception v4

    .line 276
    .local v4, "e":Ljava/lang/Exception;
    :try_start_5
    invoke-static {v4}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V
    :try_end_5
    .catch Ljava/lang/Exception; {:try_start_5 .. :try_end_5} :catch_1
    .catchall {:try_start_5 .. :try_end_5} :catchall_0

    goto/16 :goto_1

    .line 282
    .end local v3    # "buffer":[B
    .end local v4    # "e":Ljava/lang/Exception;
    .end local v9    # "msg":Landroid/os/Message;
    .end local v10    # "n":I
    .end local v15    # "totalLen":J
    .end local v17    # "writeLen":J
    :catch_1
    move-exception v4

    move-object v11, v12

    .end local v12    # "output":Ljava/io/OutputStream;
    .restart local v11    # "output":Ljava/io/OutputStream;
    move-object v6, v7

    .line 283
    .end local v7    # "input":Ljava/io/InputStream;
    .restart local v4    # "e":Ljava/lang/Exception;
    .restart local v6    # "input":Ljava/io/InputStream;
    :goto_2
    :try_start_6
    invoke-static {v4}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    .line 285
    move-object/from16 v0, p0

    iget-object v0, v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService$3;->this$0:Lnet/sunniwell/service/swupgrade/usb/UpgradeService;

    move-object/from16 v19, v0

    move-object/from16 v0, v19

    iget-object v0, v0, Lnet/sunniwell/service/swupgrade/usb/UpgradeService;->myHandler:Landroid/os/Handler;

    move-object/from16 v19, v0

    const/16 v20, 0x4

    invoke-virtual/range {v19 .. v20}, Landroid/os/Handler;->sendEmptyMessage(I)Z
    :try_end_6
    .catchall {:try_start_6 .. :try_end_6} :catchall_1

    .line 288
    if-eqz v11, :cond_3

    .line 289
    :try_start_7
    invoke-virtual {v11}, Ljava/io/OutputStream;->close()V
    :try_end_7
    .catch Ljava/io/IOException; {:try_start_7 .. :try_end_7} :catch_5

    .line 295
    :cond_3
    :goto_3
    if-eqz v6, :cond_4

    .line 296
    :try_start_8
    invoke-virtual {v6}, Ljava/io/InputStream;->close()V
    :try_end_8
    .catch Ljava/io/IOException; {:try_start_8 .. :try_end_8} :catch_6

    .line 302
    .end local v4    # "e":Ljava/lang/Exception;
    :cond_4
    :goto_4
    return-void

    .line 238
    .end local v6    # "input":Ljava/io/InputStream;
    .end local v11    # "output":Ljava/io/OutputStream;
    .end local v14    # "pathFile":Ljava/io/File;
    :catch_2
    move-exception v4

    .line 239
    .local v4, "e":Ljava/io/IOException;
    invoke-static {v4}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto/16 :goto_0

    .line 279
    .end local v4    # "e":Ljava/io/IOException;
    .restart local v3    # "buffer":[B
    .restart local v7    # "input":Ljava/io/InputStream;
    .restart local v10    # "n":I
    .restart local v12    # "output":Ljava/io/OutputStream;
    .restart local v14    # "pathFile":Ljava/io/File;
    .restart local v15    # "totalLen":J
    .restart local v17    # "writeLen":J
    :cond_5
    cmp-long v19, v17, v15

    if-gez v19, :cond_8

    .line 280
    :try_start_9
    new-instance v19, Ljava/lang/Exception;

    const-string v20, "\u590d\u5236\u6587\u4ef6\u5931\u8d25\uff01"

    invoke-direct/range {v19 .. v20}, Ljava/lang/Exception;-><init>(Ljava/lang/String;)V

    throw v19
    :try_end_9
    .catch Ljava/lang/Exception; {:try_start_9 .. :try_end_9} :catch_1
    .catchall {:try_start_9 .. :try_end_9} :catchall_0

    .line 287
    .end local v3    # "buffer":[B
    .end local v10    # "n":I
    .end local v15    # "totalLen":J
    .end local v17    # "writeLen":J
    :catchall_0
    move-exception v19

    move-object v11, v12

    .end local v12    # "output":Ljava/io/OutputStream;
    .restart local v11    # "output":Ljava/io/OutputStream;
    move-object v6, v7

    .line 288
    .end local v7    # "input":Ljava/io/InputStream;
    .restart local v6    # "input":Ljava/io/InputStream;
    :goto_5
    if-eqz v11, :cond_6

    .line 289
    :try_start_a
    invoke-virtual {v11}, Ljava/io/OutputStream;->close()V
    :try_end_a
    .catch Ljava/io/IOException; {:try_start_a .. :try_end_a} :catch_7

    .line 295
    :cond_6
    :goto_6
    if-eqz v6, :cond_7

    .line 296
    :try_start_b
    invoke-virtual {v6}, Ljava/io/InputStream;->close()V
    :try_end_b
    .catch Ljava/io/IOException; {:try_start_b .. :try_end_b} :catch_8

    .line 300
    :cond_7
    :goto_7
    throw v19

    .line 288
    .end local v6    # "input":Ljava/io/InputStream;
    .end local v11    # "output":Ljava/io/OutputStream;
    .restart local v3    # "buffer":[B
    .restart local v7    # "input":Ljava/io/InputStream;
    .restart local v10    # "n":I
    .restart local v12    # "output":Ljava/io/OutputStream;
    .restart local v15    # "totalLen":J
    .restart local v17    # "writeLen":J
    :cond_8
    if-eqz v12, :cond_9

    .line 289
    :try_start_c
    invoke-virtual {v12}, Ljava/io/OutputStream;->close()V
    :try_end_c
    .catch Ljava/io/IOException; {:try_start_c .. :try_end_c} :catch_3

    .line 295
    :cond_9
    :goto_8
    if-eqz v7, :cond_a

    .line 296
    :try_start_d
    invoke-virtual {v7}, Ljava/io/InputStream;->close()V
    :try_end_d
    .catch Ljava/io/IOException; {:try_start_d .. :try_end_d} :catch_4

    :cond_a
    move-object v11, v12

    .end local v12    # "output":Ljava/io/OutputStream;
    .restart local v11    # "output":Ljava/io/OutputStream;
    move-object v6, v7

    .line 300
    .end local v7    # "input":Ljava/io/InputStream;
    .restart local v6    # "input":Ljava/io/InputStream;
    goto :goto_4

    .line 291
    .end local v6    # "input":Ljava/io/InputStream;
    .end local v11    # "output":Ljava/io/OutputStream;
    .restart local v7    # "input":Ljava/io/InputStream;
    .restart local v12    # "output":Ljava/io/OutputStream;
    :catch_3
    move-exception v8

    .line 292
    .local v8, "ioe":Ljava/io/IOException;
    invoke-static {v8}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_8

    .line 298
    .end local v8    # "ioe":Ljava/io/IOException;
    :catch_4
    move-exception v8

    .line 299
    .restart local v8    # "ioe":Ljava/io/IOException;
    invoke-static {v8}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    move-object v11, v12

    .end local v12    # "output":Ljava/io/OutputStream;
    .restart local v11    # "output":Ljava/io/OutputStream;
    move-object v6, v7

    .line 301
    .end local v7    # "input":Ljava/io/InputStream;
    .restart local v6    # "input":Ljava/io/InputStream;
    goto :goto_4

    .line 291
    .end local v3    # "buffer":[B
    .end local v8    # "ioe":Ljava/io/IOException;
    .end local v10    # "n":I
    .end local v15    # "totalLen":J
    .end local v17    # "writeLen":J
    .local v4, "e":Ljava/lang/Exception;
    :catch_5
    move-exception v8

    .line 292
    .restart local v8    # "ioe":Ljava/io/IOException;
    invoke-static {v8}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_3

    .line 298
    .end local v8    # "ioe":Ljava/io/IOException;
    :catch_6
    move-exception v8

    .line 299
    .restart local v8    # "ioe":Ljava/io/IOException;
    invoke-static {v8}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_4

    .line 291
    .end local v4    # "e":Ljava/lang/Exception;
    .end local v8    # "ioe":Ljava/io/IOException;
    :catch_7
    move-exception v8

    .line 292
    .restart local v8    # "ioe":Ljava/io/IOException;
    invoke-static {v8}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_6

    .line 298
    .end local v8    # "ioe":Ljava/io/IOException;
    :catch_8
    move-exception v8

    .line 299
    .restart local v8    # "ioe":Ljava/io/IOException;
    invoke-static {v8}, Lcom/duolebo/tools/DLBLog;->e(Ljava/lang/Throwable;)V

    goto :goto_7

    .line 287
    .end local v8    # "ioe":Ljava/io/IOException;
    :catchall_1
    move-exception v19

    goto :goto_5

    .end local v6    # "input":Ljava/io/InputStream;
    .restart local v7    # "input":Ljava/io/InputStream;
    :catchall_2
    move-exception v19

    move-object v6, v7

    .end local v7    # "input":Ljava/io/InputStream;
    .restart local v6    # "input":Ljava/io/InputStream;
    goto :goto_5

    .line 282
    :catch_9
    move-exception v4

    goto :goto_2

    .end local v6    # "input":Ljava/io/InputStream;
    .restart local v7    # "input":Ljava/io/InputStream;
    :catch_a
    move-exception v4

    move-object v6, v7

    .end local v7    # "input":Ljava/io/InputStream;
    .restart local v6    # "input":Ljava/io/InputStream;
    goto :goto_2
.end method
