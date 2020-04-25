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

#define LOG_NDEBUG 0
#define LOG_TAG "AmCTCDataSouceProtocol"
#include <utils/Log.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <media/stagefright/DataSource.h>
#include <media/stagefright/FileSource.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaDefs.h>

#include <utils/String8.h>

#include <cutils/properties.h>

#include "AmCTCDataSouceProtocol.h"

namespace android
{

//String8 AmCTCDataSouceProtocol::mConvertUrl ;

AmCTCDataSouceProtocol::AmCTCDataSouceProtocol(const sp<DataSource> &source, const char *url, int flags)
    : mSource(source)
{
    memset(sourcestring, 0, 128);
    mOffset = 0;
    mSize = 0;
    mFLags = flags;
    mOurl = url;
}

AmCTCDataSouceProtocol::~AmCTCDataSouceProtocol()
{
    LOGV("L%d", __LINE__);
    //AmCTCDataSouceProtocol::mConvertUrl = String8();
    if (mSource.get() != NULL) {
        //DEBUG("---destruct ~CTCMediaPlayer mSouceProtocol.clear();\n");
         mSource.clear();
    }


    LOGV("~AmCTCDataSouceProtocol ok");
}

int AmCTCDataSouceProtocol::BasicInit()
{
    LOGV("---AmCTCDataSouceProtocol::BasicInit");
    static URLProtocol AmCTCDataSouce_protocol;

    //int (*pffurl_register_protocol)(URLProtocol *);

    URLProtocol *prot = &AmCTCDataSouce_protocol;

    LOGV("-BasicInit--prot->name = %s---\n", prot->name);
    if (prot->name != NULL && strncasecmp(prot->name, "DataSouce", 9) == 0) {
        return 0;
    }


    dl_handle = dlopen("/system/lib/libffmpeg30.so", RTLD_LAZY);
    int (*pffurl_register_protocol)(URLProtocol *);

    prot->name = "DataSouce";
    prot->url_open = (int (*)(URLContext *, const char *, int))data_open;
    prot->url_read = (int (*)(URLContext *, unsigned char *, int))data_read;
    prot->url_write = (int (*)(URLContext *, const unsigned char *, int))data_write;
    prot->url_seek = (int64_t (*)(URLContext *, int64_t , int))data_seek;
    prot->url_close = (int (*)(URLContext *))data_close;
    prot->url_get_file_handle = (int (*)(URLContext *))data_get_file_handle;
    LOGV("---AmCTCDataSouceProtocol::BasicInit, --> ffurl_register_protocol");

    if (dl_handle == NULL) {
        LOGV("dlopen failed errmsg: %s", dlerror());
    } else {
        pffurl_register_protocol = (int(*)(URLProtocol*))dlsym(dl_handle, "ffurl_register_protocol");

        if (pffurl_register_protocol != NULL) {
            (*pffurl_register_protocol)(prot);
        } else {
            LOGV("dlopen failed ffurl_register_protocol is null!");
        }
    }

    if (dl_handle != NULL) {
        LOGV("---dlclose dlopen--\n");
        dlclose(dl_handle);
        dl_handle = NULL;
    }

    return 0;
}

sp<AmCTCDataSouceProtocol> AmCTCDataSouceProtocol::CreateFromFD(int fd, int64_t offset, int64_t length)
{
    sp<DataSource> source;
    int flags = 0;
    source = new FileSource(dup(fd), offset, length);
    flags |= 1;
    return new AmCTCDataSouceProtocol(source, "NA", flags);
}
char *AmCTCDataSouceProtocol::GetPathString()
{
    if (sourcestring[0] == '\0') {
        int num;
        num = sprintf(sourcestring, "DataSouce:AmCTCDataSouceProtocol=[%x:%x]", (unsigned int)this, (~(unsigned int)this));
        sourcestring[num] = '\0';
    }
    LOGV("GetPathString =[%s]", sourcestring);
    return sourcestring;
}

//static
int     AmCTCDataSouceProtocol::data_open(URLContext *h, const char *filename, int flags)
{
    LOGV("::open =[%s]", filename);


    LOGV("::data_open 2=[%s]", filename);

    if (strncmp(filename, "DataSouce", strlen("DataSouce"))) {
        return -1;    //
    }


    unsigned int pf = 0, pf1 = 0;
    char *str = strstr(filename, "AmCTCDataSouceProtocol");
    if (str == NULL) {
        return -1;
    }
    LOGV("::data_open3=[%s]", filename);
    sscanf(str, "AmCTCDataSouceProtocol=[%x:%x]\n", (unsigned int*)&pf, (unsigned int*)&pf1);
    if (pf != 0 && ((unsigned int)pf1 == ~(unsigned int)pf)) {

        AmCTCDataSouceProtocol* me = (AmCTCDataSouceProtocol*)pf;
        h->priv_data = (void*) me;
        return me->DataOpen(h);
    }
    return -1;
}
//static
int     AmCTCDataSouceProtocol::data_read(URLContext *h, unsigned char *buf, int size)
{
    //LOGV("---AmCTCDataSouceProtocol::data_read, size=%d\n",size);
    AmCTCDataSouceProtocol *prot = (AmCTCDataSouceProtocol *)h->priv_data;
    return prot->DataRead(buf, size);
}
//static
int     AmCTCDataSouceProtocol::data_write(URLContext *h, const unsigned char *buf, int size)
{
    AmCTCDataSouceProtocol *prot = (AmCTCDataSouceProtocol *)h->priv_data;
    return prot->DataWrite(buf, size);
}
//static
int64_t AmCTCDataSouceProtocol::data_seek(URLContext *h, int64_t pos, int whence)
{
    AmCTCDataSouceProtocol *prot = (AmCTCDataSouceProtocol *)h->priv_data;
    return prot->DataSeek(pos, whence);
}
//static
int     AmCTCDataSouceProtocol::data_close(URLContext *h)
{
    AmCTCDataSouceProtocol *prot = (AmCTCDataSouceProtocol *)h->priv_data;
    prot->DataClose();

    //delete prot;

    return 0;
}
//static
int     AmCTCDataSouceProtocol::data_get_file_handle(URLContext *h)
{
    return (int)h->priv_data;
}

int     AmCTCDataSouceProtocol::DataOpen(URLContext *h)
{
    mURLContent = h;
    mOffset = 0;
    return 0;
}
int     AmCTCDataSouceProtocol::DataRead(unsigned char *buf, int size)
{
    int ret = -1;
    ret = mSource->readAt(mOffset, buf, size);
    if (ret > 0) {
        mOffset += ret;
    }else if(ret == ERROR_END_OF_STREAM){
    	LOGI("Get DataSource EOS\n");
	ret = 0;
    }

    return ret;
}
int     AmCTCDataSouceProtocol::DataWrite(const unsigned char *buf, int size)
{
    return -1;
}
int64_t AmCTCDataSouceProtocol::DataSeek(int64_t pos, int whence)
{
    int64_t needpos = 0;
    if (whence == AVSEEK_SIZE) {
        mSource->getSize(&mSize);
	if(mSize>0){
	    LOGI("Get Source size:lld\n",mSize);
	}else{
	    LOGI("Can't get Source size\n");
	    mSize = -1;
	}
        return mSize;
    }
#if 0
    if (whence == AVSEEK_BUFFERED_TIME) {
        return -1;
    }
    if (whence == AVSEEK_FULLTIME) {
        return -1;
    }
    if (whence == AVSEEK_TO_TIME) {
        return -1;
    }
#endif
    if (whence == SEEK_CUR) {
        needpos = mOffset + pos;
    } else if (whence == SEEK_END && mSize > 0) {
        needpos = mSize + pos;
    } else if (whence == SEEK_SET) {
        needpos = pos;
    } else {
        return -2;
    }

    if (needpos < 0 || (mSize > 0 && needpos > mSize)) {
        return -3;
    }

    mOffset = needpos;
    return 0;
}
int AmCTCDataSouceProtocol::DataGetFlags()
{
    return mFLags;
}
int     AmCTCDataSouceProtocol::DataClose()
{
    return 0;
}

}//namespace
