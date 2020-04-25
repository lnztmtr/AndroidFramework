/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

///#define LOG_NDEBUG 0
#define LOG_TAG "AmlogicPlayerStreamSource"
#include <utils/Log.h>

#include "am_media_private.h"

#include <media/stagefright/foundation/ABuffer.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/MetaData.h>
#include "AmlogicPlayerStreamSource.h"
#include <media/stagefright/MediaErrors.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
//#define DUMP_DATA
#ifdef DUMP_DATA
static int dumpindex = 0;
int dumpfd = -1;
#endif
namespace android
{
#ifndef MIN
#define MIN(x,y) ((x)<(y)?(x):(y))
#endif

AmlogicPlayerStreamSource::AmlogicPlayerStreamSource(const sp<IStreamSource> &source)
    : mSource(source)
{
    memset(sourcestring, 0, 128);
}

AmlogicPlayerStreamSource::~AmlogicPlayerStreamSource()
{

}
char * AmlogicPlayerStreamSource::GetPathString()
{

    if (sourcestring[0] == '\0') {
        int num;
        num = sprintf(sourcestring, "IStreamSource:AmlogicPlayerStreamSource=[%x:%x]", (unsigned int)this, (~(unsigned int)this));
        sourcestring[num] = '\0';
    }
    LOGV("GetPathString =[%s]", sourcestring);
    return sourcestring;
}
//static
int AmlogicPlayerStreamSource::init(void)
{

    static URLProtocol androidstreamsource_protocol;
    static int inited = 0;
    URLProtocol *prot = &androidstreamsource_protocol;
    if (inited > 0) {
        return 0;
    }
    inited++;
    prot->name = "IStreamSource";
    prot->url_open = (int (*)(URLContext *, const char *, int))amopen;
    prot->url_read = (int (*)(URLContext *, unsigned char *, int))amread;
    prot->url_write = (int (*)(URLContext *, unsigned char *, int))amwrite;
    prot->url_seek = (int64_t (*)(URLContext *, int64_t , int))amseek;
    prot->url_close = (int (*)(URLContext *))amclose;
    prot->url_get_file_handle = (int (*)(URLContext *))get_file_handle;
    av_register_protocol(prot);
    return 0;
}

//static
int AmlogicPlayerStreamSource::amopen(URLContext *h, const char *filename, int flags)
{
    LOGI("AmlogicPlayerStreamSource::open =[%s]", filename);
    if (strncmp(filename, "IStreamSource", strlen("IStreamSource"))) {
        return -1;    //
    }
    unsigned int pf = 0, pf1 = 0;
    char *str = strstr(filename, "AmlogicPlayerStreamSource");
    if (str == NULL) {
        return -1;
    }
    sscanf(str, "AmlogicPlayerStreamSource=[%x:%x]\n", (unsigned int*)&pf, (unsigned int*)&pf1);
    if (pf != 0 && ((unsigned int)pf1 == ~(unsigned int)pf)) {

        AmlogicPlayerStreamSource* me = (AmlogicPlayerStreamSource*)pf;
        h->priv_data = (void*) me;
        h->is_slowmedia = 1;
        h->is_streamed = 1;
        h->fastdetectedinfo = 1;
        ///h->flags |= URL_NO_LP_BUFFER;
        return me->Source_open();
    }
    return -2;
}
//static
int AmlogicPlayerStreamSource::amread(URLContext *h, unsigned char *buf, int size)

{
    AmlogicPlayerStreamSource* me = (AmlogicPlayerStreamSource*)h->priv_data;
    return me->Source_read(buf, size);
}
//static
int AmlogicPlayerStreamSource::amwrite(URLContext *h, unsigned char *buf, int size)

{
    AmlogicPlayerStreamSource* me = (AmlogicPlayerStreamSource*)h->priv_data;
    return me->Source_write(buf, size);

}
//static
int64_t AmlogicPlayerStreamSource::amseek(URLContext *h, int64_t pos, int whence)
{
    AmlogicPlayerStreamSource* me = (AmlogicPlayerStreamSource*)h->priv_data;
    return me->Source_seek(pos, whence);
}
//static
int AmlogicPlayerStreamSource::amclose(URLContext *h)
{
    AmlogicPlayerStreamSource* me = (AmlogicPlayerStreamSource*)h->priv_data;
    return me->Source_close();
}
//static
int AmlogicPlayerStreamSource::get_file_handle(URLContext *h)
{
    return (int)h->priv_data;
}
///*-------------------------------------------------------*//
int AmlogicPlayerStreamSource::Source_open()
{
    if (mStreamListener.get() != NULL) {
        return -1;    /*have opend before */
    }
    mStreamListener = new AmlogicPlayerStreamSourceListener(mSource, 0);
    mStreamListener->start();
    localdatasize = 0;
#ifdef DUMP_DATA
    char filename[256];
    sprintf(filename, "/temp/dump.data-%d.ts", dumpindex++);
    dumpfd = open(filename, O_RDWR | O_CREAT, 0644);
#endif
    pos = 0;
    dropdatalen = 0;
    return 0;
}
int AmlogicPlayerStreamSource::Source_read(unsigned char *buf, int size)
{
    sp<AMessage> extra;
    ssize_t n = AVERROR(EAGAIN);
    int bufelselen = size;
    int retry = 0;
    unsigned char *pbuf = buf;
    int waitretry = 1000; /*100s*/
    int oncereadmax = 188 * 10;
    int readlen = 0;
#define READ_DROPDATA (0)


    if (dropdatalen > 0) {
        LOGI("SEEK .... drop data=%d *****", dropdatalen);
        bufelselen = 188;
    }
    while (oncereadmax > 0 && bufelselen > 0 && !url_interrupt_cb()) {
        char *buffer = localbuf;
        int rlen;
        int newread = 0;
        if (localdatasize > 0) {
            n = localdatasize;
        } else {
            n = mStreamListener->read(buffer, 188, &extra,(size - bufelselen)==0);/*only wait on first */
            newread = 1;
            if (n > 0) {
                localdatasize = n;
            }
        }
        if (n > 0) {
            if (newread && buffer[0] == 0x00) {
                //FIXME
                if (buffer[1] == 0x00)
                    ;///DISCONTINUITY_SEEK
                else
                    ;///DISCONTINUITY_FORMATCHANGE
                LOGI("DISCONTINUITY_SEEK=%d *****", n);
                dropdatalen = READ_DROPDATA;
                continue;//to next packets
            }
            if (dropdatalen > 0) {
                if (dropdatalen >= n) {
                    dropdatalen -= n;
                    continue;//to next packets
                } else {
                    n -= dropdatalen;
                    dropdatalen = 0;
                }
            }
            rlen = MIN(n, bufelselen);
            memcpy(pbuf, buffer, rlen);
            pbuf += rlen;
            bufelselen -= rlen;
            oncereadmax -= rlen;
            if (n > bufelselen) {
                /*read buf is small than 188*/
                localdatasize = n - rlen;
                memmove(buffer, buffer + rlen, localdatasize);
            } else {
                localdatasize = 0;
            }
        } else if (n == -11 && retry++ < 200) {
            dropdatalen = 0; //upper buffer have empty now,
            n = AVERROR(EAGAIN);
            if ((size - bufelselen) != 0) {
                break;    /*have read data before,return first*/
            }
            ///usleep(1000 * 10); /*10ms *100 =1S,same as tcp read*/
        } else {
            if (n == INFO_DISCONTINUITY) {
                LOGI("STREAM INFO DISCONTINUITY message=%d\n", n);
                dropdatalen = READ_DROPDATA;
                continue;/*ignore this INFO,FIXME*/
            } else if (n == INFO_FORMAT_CHANGED) {
                LOGI("STREAM INFO INFO_FORMAT_CHANGED message=%d\n", n);
                continue;/*ignore this INFO,FIXME*/
            } else if (n == -11) {
                n = AVERROR(EAGAIN);
            }
            LOGV("Source_read error=%d");
            break;//errors
        }

        LOGV(" Source_read=%d,retry=%d", n, retry);
    }
#ifdef DUMP_DATA
    if (dumpfd >= 0 && (size - bufelselen) > 0) {
        write(dumpfd, buf, (size - bufelselen));
    }
#endif
    readlen = (size - bufelselen);
    if (readlen > 0) { /*readed data,lock and del readed size*/
        Mutex::Autolock autoLock(mMoreDataLock);
        pos += readlen;
    }
    return readlen > 0 ? readlen : n;
}
int AmlogicPlayerStreamSource::Source_write(unsigned char *buf, int size)
{
    return -1;
}
int64_t AmlogicPlayerStreamSource::Source_seek(int64_t tpos, int whence)
{
    LOGI("AmlogicPlayerStreamSource Source_seek=%lld,whence=%d\n", pos, whence);
    if (whence == AVSEEK_SIZE) {
        return -1;
    }
    if (whence == AVSEEK_BUFFERED_TIME) {
        return -1;
    }
    if (whence == AVSEEK_FULLTIME) {
        return -1;
    }
    if (whence == AVSEEK_TO_TIME) {
        return -1;
    }
    if (whence == SEEK_CUR) {
        return pos + tpos;
    }
    return -1;
}
int AmlogicPlayerStreamSource::Source_close()
{
    LOGI("AmlogicPlayerStreamSource Source_close");
#ifdef DUMP_DATA
    if (dumpfd >= 0) {
        close(dumpfd);
    }
    dumpfd = -1;
    mStreamListener.clear();
#endif
    mStreamListener->stop();
    return 0;
}


}//end android space

