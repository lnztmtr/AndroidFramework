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
#define LOG_TAG "AmlogicPlayerDataSouceProtocol"
#include <utils/Log.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#include <media/stagefright/DataSource.h>
#include <media/stagefright/FileSource.h>
#include <media/stagefright/MediaErrors.h>
#include <media/stagefright/MediaDefs.h>

#include <utils/String8.h>
#include "../libstagefright/include/NuCachedSource2.h"
#include "../libstagefright/include/HTTPBase.h"
#include <cutils/properties.h>

#include"AmlogicPlayerDataSouceProtocol.h"
#include"AmlogicPlayer.h"

namespace android
{

String8 AmlogicPlayerDataSouceProtocol::mConvertUrl ;

AmlogicPlayerDataSouceProtocol::AmlogicPlayerDataSouceProtocol(const sp<DataSource> &source, const char *url, int flags)
    : mSource(source)
{
    memset(sourcestring, 0, 128);
    mOffset = 0;
    mSize = 0;
    mFLags = flags;
    mOurl = url;
}

AmlogicPlayerDataSouceProtocol::~AmlogicPlayerDataSouceProtocol()
{
    LOGV("L%d", __LINE__);
    AmlogicPlayerDataSouceProtocol::mConvertUrl = String8();
    mSource.clear();
}

//static
int AmlogicPlayerDataSouceProtocol::BasicInit()
{
    static URLProtocol AmlogicPlayerDataSouce_protocol;
    static URLProtocol AmlogicPlayerHttps_protocol;
    static URLProtocol AmlogicPlayerShttps_protocol;
	static URLProtocol AmlogicPlayerHttp_protocol;
	static URLProtocol AmlogicPlayerSHttp_protocol;
    static int inited = 0;
    URLProtocol *prot = &AmlogicPlayerDataSouce_protocol;
    URLProtocol *https_prot = &AmlogicPlayerHttps_protocol;
    URLProtocol *shttps_prot = &AmlogicPlayerShttps_protocol;
	URLProtocol *http_prot = &AmlogicPlayerHttp_protocol;
	URLProtocol *shttp_prot = &AmlogicPlayerSHttp_protocol;
    if (inited > 0) {
        return 0;
    }
    inited++;
    prot->name = "DataSouce";
    prot->url_open = (int (*)(URLContext *, const char *, int))data_open;
    prot->url_read = (int (*)(URLContext *, unsigned char *, int))data_read;
    prot->url_write = (int (*)(URLContext *, unsigned char *, int))data_write;
    prot->url_seek = (int64_t (*)(URLContext *, int64_t , int))data_seek;
    prot->url_close = (int (*)(URLContext *))data_close;
    prot->url_get_file_handle = (int (*)(URLContext *))data_get_file_handle;
    av_register_protocol(prot);
    https_prot->name = "https";
    https_prot->url_open = (int (*)(URLContext *, const char *, int))data_open;
    https_prot->url_read = (int (*)(URLContext *, unsigned char *, int))data_read;
    https_prot->url_write = (int (*)(URLContext *, unsigned char *, int))data_write;
    https_prot->url_seek = (int64_t (*)(URLContext *, int64_t , int))data_seek;
    https_prot->url_close = (int (*)(URLContext *))data_close;
    https_prot->url_get_file_handle = (int (*)(URLContext *))data_get_file_handle;
    av_register_protocol(https_prot);
    shttps_prot->name = "shttps";
    shttps_prot->url_open = (int (*)(URLContext *, const char *, int))data_open;
    shttps_prot->url_read = (int (*)(URLContext *, unsigned char *, int))data_read;
    shttps_prot->url_write = (int (*)(URLContext *, unsigned char *, int))data_write;
    shttps_prot->url_seek = (int64_t (*)(URLContext *, int64_t , int))data_seek;
    shttps_prot->url_close = (int (*)(URLContext *))data_close;
    shttps_prot->url_get_file_handle = (int (*)(URLContext *))data_get_file_handle;
    av_register_protocol(shttps_prot);
	if(AmlogicPlayer::PropIsEnable("media.amplayer.use-androidhttp")){
		http_prot->name = "http";
		http_prot->url_open = (int (*)(URLContext *, const char *, int))data_open;
	    http_prot->url_read = (int (*)(URLContext *, unsigned char *, int))data_read;
	    http_prot->url_write = (int (*)(URLContext *, unsigned char *, int))data_write;
	    http_prot->url_seek = (int64_t (*)(URLContext *, int64_t , int))data_seek;
	    http_prot->url_close = (int (*)(URLContext *))data_close;
	    http_prot->url_get_file_handle = (int (*)(URLContext *))data_get_file_handle;
	    av_register_protocol(http_prot);
		shttp_prot->name = "shttp";
		shttp_prot->url_open = (int (*)(URLContext *, const char *, int))data_open;
	    shttp_prot->url_read = (int (*)(URLContext *, unsigned char *, int))data_read;
	    shttp_prot->url_write = (int (*)(URLContext *, unsigned char *, int))data_write;
	    shttp_prot->url_seek = (int64_t (*)(URLContext *, int64_t , int))data_seek;
	    shttp_prot->url_close = (int (*)(URLContext *))data_close;
	    shttp_prot->url_get_file_handle = (int (*)(URLContext *))data_get_file_handle;
	    av_register_protocol(shttp_prot);
	}
    return 0;
}

//static
void AmlogicPlayerDataSouceProtocol::RegisterAndroidHttpProt()
{
    static URLProtocol AmlogicPlayerHttp_protocol;
    static URLProtocol AmlogicPlayerSHttp_protocol;
    URLProtocol *http_prot = &AmlogicPlayerHttp_protocol;
    URLProtocol *shttp_prot = &AmlogicPlayerSHttp_protocol;

    static int inited = 0;
    if (inited > 0) {
        return;
    }
    inited++;
    LOGI("[%s%d] ==", __FUNCTION__, __LINE__);
    
    http_prot->name = "http";
    http_prot->url_open = (int (*)(URLContext *, const char *, int))data_open;
    http_prot->url_read = (int (*)(URLContext *, unsigned char *, int))data_read;
    http_prot->url_write = (int (*)(URLContext *, unsigned char *, int))data_write;
    http_prot->url_seek = (int64_t (*)(URLContext *, int64_t , int))data_seek;
    http_prot->url_close = (int (*)(URLContext *))data_close;
    http_prot->url_get_file_handle = (int (*)(URLContext *))data_get_file_handle;
    av_register_protocol(http_prot);
    shttp_prot->name = "shttp";
    shttp_prot->url_open = (int (*)(URLContext *, const char *, int))data_open;
    shttp_prot->url_read = (int (*)(URLContext *, unsigned char *, int))data_read;
    shttp_prot->url_write = (int (*)(URLContext *, unsigned char *, int))data_write;
    shttp_prot->url_seek = (int64_t (*)(URLContext *, int64_t , int))data_seek;
    shttp_prot->url_close = (int (*)(URLContext *))data_close;
    shttp_prot->url_get_file_handle = (int (*)(URLContext *))data_get_file_handle;
    av_register_protocol(shttp_prot);
}


sp<AmlogicPlayerDataSouceProtocol> AmlogicPlayerDataSouceProtocol::CreateFromUrl(
    const char *uri, const KeyedVector<String8, String8> *headers)
{
    sp<DataSource> source;
    bool is_widevine = false;
    int ret;
    int flags = 0;
    char *mUri = (char *) malloc(strlen(uri) + 4);
    if (!mUri) {
        LOGV("CreateFromUrl malloc failed size=%d", strlen(uri) + 4);
        return NULL;
    }
    mConvertUrl = String8();
    uint32_t num;
    if (!strncasecmp("widevine://", uri, 11)) {
        is_widevine = true;
        num = sprintf(mUri, "http://%s", uri + 11);
        mUri[num] = '\0';
        mConvertUrl = String8(mUri);
    } else {
        strcpy(mUri, uri);
        mUri[strlen(uri)] = '\0';
    }

    if (!strncasecmp("file://", uri, 7)) {
        source = new FileSource(uri + 7);
    } else if (!strncasecmp("http://", uri, 7)
               || !strncasecmp("https://", uri, 8)
               || is_widevine) {
        sp<HTTPBase> httpSource = HTTPBase::Create();
        if ((ret = httpSource->connect(mUri, headers)) != OK) {
            free(mUri);
            LOGV("httpSource connect failed size ret=%d", ret);
            return NULL;
        }
        source = httpSource;
    } else {
        // Assume it's a filename.
        source = new FileSource(uri);
    }
    free(mUri);
    if (source == NULL || source->initCheck() != OK) {
        LOGV("CreateFromUrl source=%x", source.get());
        return NULL;
    }
    flags |= 1;
    return new AmlogicPlayerDataSouceProtocol(source, uri, flags);
}
sp<AmlogicPlayerDataSouceProtocol> AmlogicPlayerDataSouceProtocol::CreateFromFD(int fd, int64_t offset, int64_t length)
{
    sp<DataSource> source;
    int flags = 0;
    source = new FileSource(dup(fd), offset, length);
    flags |= 1;
    return new AmlogicPlayerDataSouceProtocol(source, "NA", flags);
}
char  *AmlogicPlayerDataSouceProtocol::GetPathString()
{
    if (sourcestring[0] == '\0') {
        int num;
        num = sprintf(sourcestring, "DataSouce:AmlogicPlayerDataSouceProtocol=[%x:%x]", (unsigned int)this, (~(unsigned int)this));
        sourcestring[num] = '\0';
    }
    LOGV("GetPathString =[%s]", sourcestring);
    return sourcestring;
}

//static
int     AmlogicPlayerDataSouceProtocol::data_open(URLContext *h, const char *filename, int flags)
{
    LOGI("::open =[%s]", filename);
    if (!strncasecmp("shttp://", filename, 8) || !strncasecmp("http://", filename, 7) ||
		!strncasecmp("shttps://", filename, 9) || !strncasecmp("https://", filename, 8)) {
        int retry = 0;
        sp<HTTPBase> httpSource = HTTPBase::Create();
		int haveSprex=(filename[0]=='s' || filename[0]=='S')?1:0;
retry:
        if (httpSource->connect(filename + haveSprex, NULL) != OK) {
            retry++;
            if (retry < 2) {
                goto retry;
            } else {
                return -1;
            }
        }
        sp<DataSource> source = new NuCachedSource2(httpSource);
        AmlogicPlayerDataSouceProtocol*mAmpSource = new AmlogicPlayerDataSouceProtocol(source, filename, flags | DSP_FLAG_CREATE_FROM_STATIC_OPEN);
        h->priv_data = (void*)mAmpSource;
        mAmpSource->DataOpen(h);

        return 0;
    }

    if (strncmp(filename, "DataSouce", strlen("DataSouce"))) {
        return -1;    //
    }


    unsigned int pf = 0, pf1 = 0;
    char *str = strstr(filename, "AmlogicPlayerDataSouceProtocol");
    if (str == NULL) {
        return -1;
    }
    sscanf(str, "AmlogicPlayerDataSouceProtocol=[%x:%x]\n", (unsigned int*)&pf, (unsigned int*)&pf1);
    if (pf != 0 && ((unsigned int)pf1 == ~(unsigned int)pf)) {

        AmlogicPlayerDataSouceProtocol* me = (AmlogicPlayerDataSouceProtocol*)pf;
        h->priv_data = (void*) me;
        return me->DataOpen(h);
    }
    return -1;
}
//static
int     AmlogicPlayerDataSouceProtocol::data_read(URLContext *h, unsigned char *buf, int size)
{
    AmlogicPlayerDataSouceProtocol *prot = (AmlogicPlayerDataSouceProtocol *)h->priv_data;
    return prot->DataRead(buf, size);
}
//static
int     AmlogicPlayerDataSouceProtocol::data_write(URLContext *h, unsigned char *buf, int size)
{
    AmlogicPlayerDataSouceProtocol *prot = (AmlogicPlayerDataSouceProtocol *)h->priv_data;
    return prot->DataWrite(buf, size);
}
//static
int64_t AmlogicPlayerDataSouceProtocol::data_seek(URLContext *h, int64_t pos, int whence)
{
    AmlogicPlayerDataSouceProtocol *prot = (AmlogicPlayerDataSouceProtocol *)h->priv_data;
    return prot->DataSeek(pos, whence);
}
//static
int     AmlogicPlayerDataSouceProtocol::data_close(URLContext *h)
{
    AmlogicPlayerDataSouceProtocol *prot = (AmlogicPlayerDataSouceProtocol *)h->priv_data;
    prot->DataClose();
    if (prot->DataGetFlags() & DSP_FLAG_CREATE_FROM_STATIC_OPEN) {
        delete prot;
    }
    return 0;
}
//static
int     AmlogicPlayerDataSouceProtocol::data_get_file_handle(URLContext *h)
{
    return (int)h->priv_data;
}

int     AmlogicPlayerDataSouceProtocol::DataOpen(URLContext *h)
{
    if ((mFLags & 1) && h) {
        h->priv_flags |= FLAGS_LOCALMEDIA;
    }
    mURLContent = h;
    mOffset = 0;
    return 0;
}
int     AmlogicPlayerDataSouceProtocol::DataRead(unsigned char *buf, int size)
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
int     AmlogicPlayerDataSouceProtocol::DataWrite(unsigned char *buf, int size)
{
    return -1;
}
int64_t AmlogicPlayerDataSouceProtocol::DataSeek(int64_t pos, int whence)
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
int AmlogicPlayerDataSouceProtocol::DataGetFlags()
{
    return mFLags;
}
int     AmlogicPlayerDataSouceProtocol::DataClose()
{
    return 0;
}
sp<DecryptHandle> AmlogicPlayerDataSouceProtocol::DrmInitialization(const char *mime)
{
    if (mSource.get() != NULL) {
        return mSource->DrmInitialization(mime);
    }
    return NULL;
}

void AmlogicPlayerDataSouceProtocol::getDrmInfo(sp<DecryptHandle> &handle, DrmManagerClient **client)
{
    if (mSource.get() != NULL) {
        mSource->getDrmInfo(handle, client);
    }
}

}//namespace
