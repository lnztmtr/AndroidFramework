/*
** Copyright 2007, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/


#define LOG_NDEBUG 0
#define LOG_TAG "AmSuperPlayer"
#include "utils/Log.h"

#include <stdio.h>
#include <assert.h>
#include <limits.h>
#include <unistd.h>
#include <fcntl.h>
#include <sched.h>
#include <sys/types.h>
#include <sys/stat.h>
//#include <gui/ISurface.h>
//#include <ui/Overlay.h>

#include <gui/Surface.h>
#include <gui/ISurfaceTexture.h>
//#include <gui/SurfaceTextureClient.h>
#include <gui/ISurfaceComposer.h>

#include <cutils/properties.h>

#include "AmSuperPlayer.h"
#include "AmlogicPlayer.h"

#include "MidiFile.h"
#include "TestPlayerStub.h"
#include "StagefrightPlayer.h"
#include "nuplayer/NuPlayerDriver.h"


static int myTid() {
#ifdef HAVE_GETTID
    return gettid();
#else
    return getpid();
#endif
}

namespace android {
	static status_t ERROR_NOT_OPEN = -1;
	static status_t ERROR_OPEN_FAILED = -2;
	static status_t ERROR_ALLOCATE_FAILED = -4;
	static status_t ERROR_NOT_SUPPORTED = -8;
	static status_t ERROR_NOT_READY = -16;
	static status_t STATE_INIT = 0;
	static status_t STATE_ERROR = 1;
	static status_t STATE_OPEN = 2;

#define  TRACE()	LOGV("[%s::%d]\n",__FUNCTION__,__LINE__)
//#define  TRACE()	
#define IS_LOCAL_HTTP(uri) (uri && (strcasestr(uri,"://127.0.0.1") || strcasestr(uri,"://localhost")))
#define IS_HTTP(uri) (uri && (strncmp(uri, "http", strlen("http")) == 0 || strncmp(uri, "shttp", strlen("shttp")) == 0 || strncmp(uri, "https", strlen("https")) == 0))
bool IsManifestUrl( const char* url);
AmSuperPlayer::AmSuperPlayer() :
    mPlayer(0),
	mState(STATE_ERROR)
{
	TRACE();
	Mutex::Autolock l(mMutex);
	TRACE();
	muri=NULL;
	url_valid=false;
	fd_valid=false;
	mEXIT=0;
	mRequsetEXIT = 0;
	Prepared=false;
	subplayer_inited=false;
	oldmsg_num=0;
	mLoop = false;
	current_type=AMSUPER_PLAYER;
	mCookie=NULL;
	mRenderTid=-1;
	mSoftPara = NULL;
	mHardPara = NULL;
	isRestartCreate = false;
	isSwitchURL=false;
	isHEVC=false;
	isSoftDec=false;
	mVideoScalingMode=NATIVE_WINDOW_SCALING_MODE_SCALE_TO_WINDOW;
	isStartedPrepared=false;
	mRequestPrepared=false;
    mPrepareErr = false;
    mNotify = NULL;
	mFirstPcr = 0;
	mSpecialState = 0;
	mFirstSeekMs = 0;
    LOGV("AmSuperPlayer init now\n");
	
}

AmSuperPlayer::~AmSuperPlayer() 
{
	TRACE();
	release();
}



void        AmSuperPlayer::onFirstRef()
{
	TRACE();
	Mutex::Autolock l(mMutex);
	mState = NO_ERROR;
	
}
status_t    AmSuperPlayer::initCheck()
{
	TRACE();
	Mutex::Autolock l(mMutex);
	TRACE();
	return mState;
}

status_t    AmSuperPlayer::setDataSource(const char *uri, const KeyedVector<String8, String8> *headers)
{
	TRACE();
	Mutex::Autolock l(mMutex);
	TRACE();
	if(muri!=NULL) free((void*)muri);
	if((strncmp(uri,"http://",7)==0 || strncmp(uri,"https://",8)==0) &&
		!IS_LOCAL_HTTP(uri) && /*not local http server.*/
		PropIsEnable("media.amplayer.widevineenable")&&!IsManifestUrl(uri)&&
		!strstr(uri, ".m3u8")){
		char *turi;
		turi=(char *)malloc(strlen(uri)+16);
		sprintf(turi,"widevine%s",strchr(uri,':'));///changed the xxxx://..... to widevine:///
		muri=turi;
		mOUrl=uri;
		isSwitchURL=true;
	}else{
		muri=strdup(uri);
	}
	if (headers) {
        mheaders = *headers;
    }
	url_valid=true;
	mState = STATE_OPEN;
	/*
	 uri: contains "wasuott.itv.cmvideo.cn", ".m3u8", "livemode=1"
	*/

	if (PropIsEnable("media.player.seekX")) {
		if (strncmp(uri,"http://",7)==0 && strstr(uri, "wasuott.itv.cmvideo.cn") != NULL
			&& strstr(uri, ".m3u8") != NULL && strstr(uri, "livemode=1")) {
			mSpecialState = 1;
		}
	}

	return NO_ERROR;
}
static int fdcound=0;
status_t    AmSuperPlayer::setDataSource(int fd, int64_t offset, int64_t length)
{
	TRACE();
	mMutex.lock();
	
	TRACE();
	mfd=dup(fd);
	moffset=offset;
	mlength=length;
	fd_valid=true;
	mState = STATE_OPEN;
    mPrepareErr = false;
///#define DUMP_TO_FILE
#ifdef DUMP_TO_FILE
	int testfd=mfd;
	int dumpfd;
	int tlen;
	char buf[1028];
	int oldoffset=lseek(testfd, 0, SEEK_CUR);
	sprintf(buf,"/data/test/dump.media%d-%d.mp4",fdcound,fd);
	fdcound++;
	dumpfd=open(buf,O_CREAT | O_TRUNC | O_WRONLY,0666);
	LOGV("Dump media to file %s:%d\n",buf,dumpfd);
	lseek(testfd, 0, SEEK_SET);
	tlen=read(testfd,buf,1024);
	while(dumpfd>=0 && tlen>0){
		write(dumpfd,buf,tlen);
		tlen=read(testfd,buf,1024);
	}
	close(dumpfd);
	lseek(testfd, oldoffset, SEEK_SET);
#endif
	// 0 origin mode --default,  player start when prepare request commign
	// 1  new start mode ,player will start in setdatasource method
	if(PropIsEnable("media.amplayer.startmode")==0){
                	mMutex.unlock();
			return NO_ERROR;
       }
	prepareAsync_nolock();
	mMutex.unlock();
#if 0
	//waite until libplayer prepare 
	while(1){
		if(mEXIT||mRequsetEXIT || mPrepareErr)
			break;
		else if (Prepared && mPlayer.get() != NULL)
			break;

		usleep(1000);
	}
	if((mPlayer.get()!=NULL) && (!mPrepareErr))
		return NO_ERROR;
	else
                return UNKNOWN_ERROR;
    #endif
		return NO_ERROR;
}

status_t AmSuperPlayer::setVideoSurfaceTexture(const sp<IGraphicBufferProducer>& bufferProducer)
{
	TRACE();
	Mutex::Autolock l(mMutex);
	TRACE();
	mSurface.clear();
	//msurfaceTexture = surfaceTexture;
	msurfaceTexture = bufferProducer;
	if (mPlayer == 0) {

	    return OK;
	}
    return mPlayer->setVideoSurfaceTexture(msurfaceTexture);
}	

status_t    AmSuperPlayer::prepare()
{
	int ret;
	TRACE();
	ret=prepareAsync();
	if(ret!=NO_ERROR)
		return ret;
	while(!Prepared){
                if(mEXIT ||mRequsetEXIT||(mRenderTid<=0))
                    break;
            usleep(1000*10);
        }
	if(mEXIT || !Prepared || (mPlayer.get()==NULL))
		return UNKNOWN_ERROR;
	return NO_ERROR;	
	////
}
status_t    AmSuperPlayer::prepareAsync()
{
	TRACE();
	Mutex::Autolock l(mMutex);
	mRequestPrepared=true;
	if(!isStartedPrepared)
		return prepareAsync_nolock();
	return NO_ERROR;
}
status_t    AmSuperPlayer::prepareAsync_nolock()
{
    TRACE();
    sp<MediaPlayerBase> p;
    TRACE();
    Prepared=false;
    mEXIT = 0;
    mRequsetEXIT =0;
    mRenderTid = 1;
    createThreadEtc(startThread, this, "StartThread", ANDROID_PRIORITY_NORMAL);
    ///mCondition.wait(mMutex);
    if (mRenderTid > 0) {
        LOGV("initThread(%d) started", mRenderTid);
    }
	isStartedPrepared=true;
	return NO_ERROR;
}
status_t    AmSuperPlayer::start()
{
	TRACE();
	Mutex::Autolock l(mMutex);
	TRACE();
	if (mPlayer == 0) {
		return UNKNOWN_ERROR;
	}
	mPlayer->setLooping(mLoop);
	if(isRestartCreate == true)
	{	isRestartCreate = false;
		LOGV("clear isRestartCreate");
	}

	if (mSpecialState == 1) {
		int dur = 0;
		dur = mPlayer->getDuration(&dur);
		LOGI("start dur:%d, mFirstPcr=%d\n", dur, mFirstPcr);
		if (dur <= 0 && mFirstPcr == 0)
			mSpecialState = 2;
	}

	return mPlayer->start();
}
status_t    AmSuperPlayer::stop()
{
	Mutex::Autolock l(mMutex);
        mRequsetEXIT = true;
	isStartedPrepared = false;	
	if (mPlayer == 0) {
		return UNKNOWN_ERROR;
	}
	return mPlayer->stop();
}
status_t    AmSuperPlayer::seekTo(int msec)
{
	int current=-1;
	TRACE();
	Mutex::Autolock l(mMutex);
	TRACE();
	if (mPlayer == 0) return UNKNOWN_ERROR;
        mPlayer->getCurrentPosition(&current);
#if 0
        if(current>=0 && (msec<(current+10) && msec>(current-10)))
        {
            //sendEvent(MEDIA_SEEK_START);
            sendEvent(MEDIA_SEEK_COMPLETE);
            return NO_ERROR;
        }
#endif

	if (mSpecialState == 2) {
		int dur = 0;
		mPlayer->getDuration(&dur);
		if (dur <= 0 && mFirstPcr == 0) {
			mSpecialState = 3;
			mFirstSeekMs = msec;
			return NO_ERROR;
		}
	}

	return mPlayer->seekTo(msec);
}
status_t    AmSuperPlayer::pause()
{
	TRACE();
	Mutex::Autolock l(mMutex);
	TRACE();
	if (mPlayer == 0) return UNKNOWN_ERROR;
	return mPlayer->pause();
}
bool        AmSuperPlayer::isPlaying()
{
	TRACE();
	Mutex::Autolock l(mMutex);
	TRACE();
	if (mPlayer == 0) return false;
	return mPlayer->isPlaying();
}
status_t    AmSuperPlayer::getCurrentPosition(int* msec)
{
	TRACE();
	Mutex::Autolock l(mMutex);
	TRACE();
	*msec = 0;
	if (mPlayer == 0) //return UNKNOWN_ERROR;
		return 0;
	status_t ret = mPlayer->getCurrentPosition(msec);
	if(*msec < 0)
		*msec = 0;
	return ret;	
}
status_t    AmSuperPlayer::getDuration(int* msec)
{
	TRACE();
	Mutex::Autolock l(mMutex);
	TRACE();
	if (mPlayer == 0)// return UNKNOWN_ERROR;
		return 0;
	return mPlayer->getDuration(msec);		
}
status_t    AmSuperPlayer::release()
{
	TRACE();
	stop();
	while(mRenderTid>0){	
		Mutex::Autolock l(mMutex);
		mRequsetEXIT =  true;
		mCondition.signal();
		mCondition.wait(mMutex);
	}
	if(muri!=NULL) free((void*)muri);
	
	TRACE();
	if (mPlayer == NULL) return NO_ERROR;
	mPlayer.clear();
	if(NULL!=mHardPara){
		free((void*)mHardPara);
		mHardPara = NULL;
	}
	if(NULL!=mSoftPara){
		free((void*)mSoftPara);
		mSoftPara = NULL;
	}	
	return NO_ERROR;
}
status_t    AmSuperPlayer::reset()
{
	TRACE();

        {
            Mutex::Autolock l(mMutex);
            if (fd_valid) {
                fd_valid=false;
                close(mfd);
                mfd=-1;
            }
            mheaders.clear();
            mLoop = false;
        }

	if (mPlayer != 0){
            mPlayer->stop();
            mPlayer.clear();
            return NO_ERROR;
        }
        ///do stop the creat thread.
	stop();
        while(mRenderTid>0){
                Mutex::Autolock l(mMutex);
                mRequsetEXIT = true;
                mCondition.signal();
                mCondition.wait(mMutex);
        }
        if (mPlayer != 0)
            mPlayer.clear();
        return NO_ERROR;
}
status_t    AmSuperPlayer::setLooping(int loop)
{
	TRACE();
	Mutex::Autolock l(mMutex);
	TRACE();
	mLoop = (loop != 0);
	if (mPlayer == 0) return NO_ERROR;
	return mPlayer->setLooping(loop);
}
bool        AmSuperPlayer::hardwareOutput()
{
	TRACE();
	Mutex::Autolock l(mMutex);
	TRACE();
	if (mPlayer == 0) return false;
	return mPlayer->hardwareOutput();	
}
status_t    AmSuperPlayer::invoke(const Parcel& request, Parcel *reply) 
{
	TRACE();
	int64_t starttimeUs = ALooper::GetNowUs();
#define INVOKE_MAX_HTTP_WAIT_TIMEUS 2000000
	//if old start mode, use old invoke code
	if((!IS_HTTP(muri))&&isStartedPrepared&&PropIsEnable("media.amplayer.startmode")==1)
	{
		while(mPlayer==0)
		{
			if(mEXIT||mRequsetEXIT)
				return ERROR_UNSUPPORTED;
			usleep(1000*10);
		}
		return mPlayer->invoke(request,reply);
	}
	int32_t methodId;
	int datapos=request.dataPosition();
	status_t ret = request.readInt32(&methodId);
	if(ret == android::OK && methodId == INVOKE_ID_SET_VIDEO_SCALING_MODE){
		mVideoScalingMode=request.readInt32();
		request.setDataPosition(datapos);
	}else{
		ret=ERROR_UNSUPPORTED;
		request.setDataPosition(datapos);
	}
	while(mPlayer == 0 && ALooper::GetNowUs()< starttimeUs + INVOKE_MAX_HTTP_WAIT_TIMEUS);

	if(mPlayer != 0){
		return mPlayer->invoke(request,reply);	
	}
	return ret;/*no player,def unsupport,scaling can support.*/
}
status_t    AmSuperPlayer::getMetadata(const media::Metadata::Filter& ids,Parcel *records)
{
	TRACE();
	Mutex::Autolock l(mMutex);
	if (mPlayer == 0) return UNKNOWN_ERROR;
	return mPlayer->getMetadata(ids,records);	

}
status_t    AmSuperPlayer::setParameter(int key, const Parcel &request)
{
	TRACE();
	Mutex::Autolock l(mMutex);
	
	if(key ==KEY_PARAMETER_AML_PLAYER_FORCE_SOFT_DECODE){
		const String16 uri16 = request.readString16();
		String8 keyStr = String8(uri16);
		LOGI("setParameter %d=[%s]\n",key,keyStr.string());
		if(NULL== mSoftPara){
			mSoftPara = strndup(keyStr.string(),1024); 
		}
		return OK;
	}else if(key ==KEY_PARAMETER_AML_PLAYER_FORCE_HARD_DECODE){
		const String16 uri16 = request.readString16();
		String8 keyStr = String8(uri16);
		LOGI("setParameter %d=[%s]\n",key,keyStr.string());
		if(NULL==mHardPara){
			mHardPara = strndup(keyStr.string(),1024); 
		}
		return OK;
	}else if(key == KEY_PARAMETER_AML_PLAYER_PR_CUSTOM_DATA){
	if(IsManifestUrl(muri)){
            const String16 uri16 = request.readString16();
            String8 keyStr = String8(uri16);
            DrmManagerClient* drmSetCustomData = new DrmManagerClient();
            DrmInfoRequest drmInfoRequest(key, String8("application/vnd.ms-playready"));
            drmInfoRequest.put(String8("PRCustomData"), keyStr);
            drmInfoRequest.put(String8("PRContentPath"), String8("dummy"));
            DrmInfo* drmInfoReq = drmSetCustomData->acquireDrmInfo(&drmInfoRequest);
            delete drmSetCustomData;
            TRACE();
            return OK;
        }
	}
	if (mPlayer == 0) return UNKNOWN_ERROR;
	return mPlayer->setParameter(key,request);	
}

status_t    AmSuperPlayer::getParameter(int key, Parcel *reply)
{
	TRACE();
	Mutex::Autolock l(mMutex);
	if (mPlayer == 0){
		if(key==KEY_PARAMETER_AML_PLAYER_TYPE_STR){
			reply->writeString16(String16(AmSuperPlayer::PlayerType2Str(this->playerType())));
			return 0;
		}	
		if(key==KEY_PARAMETER_AML_PLAYER_VIDEO_OUT_TYPE){
			reply->writeInt32(VIDEO_OUT_SOFT_RENDER);
			return 0;
		}
		return UNKNOWN_ERROR;
	}else{
		if(key==KEY_PARAMETER_AML_PLAYER_TYPE_STR){
			reply->writeString16(String16(AmSuperPlayer::PlayerType2Str(mPlayer->playerType())));
			return 0;
		}	
		if(key==KEY_PARAMETER_AML_PLAYER_VIDEO_OUT_TYPE && mPlayer->playerType()!=AMLOGIC_PLAYER){
			reply->writeInt32(VIDEO_OUT_SOFT_RENDER);
			return 0;
		}

        if(key==KEY_PARAMETER_AML_PLAYER_GET_MEDIA_INFO &&
            ((mPlayer->playerType()!=AMLOGIC_PLAYER) && (mPlayer->playerType()!=STAGEFRIGHT_PLAYER))){
            LOGV("[%s::%d] playertype=%d, \n",__FUNCTION__,__LINE__, mPlayer->playerType());
            return 0;
        }

        /*+[SE][BUG][BUG-172654][chengshun] awesomeplayer not need get frameinfo.*/
        if (key == KEY_PARAMETER_AML_PLAYER_GET_QOS_PARAM) {
            LOGV("[%s::%d] playertype=%d, not need get frameinfo\n",__FUNCTION__,__LINE__, mPlayer->playerType());
            return 0;
        }
            
		return mPlayer->getParameter(key,reply);	
	}
}

void AmSuperPlayer::notify(void* cookie, int msg, int ext1, int ext2,const Parcel *obj)
{
	TRACE();
	 AmSuperPlayer* m=(AmSuperPlayer*)cookie;
	if(m!=0)
		m->Notify(cookie,msg,ext1,ext2,obj);
}
void AmSuperPlayer::Notify(void* cookie, int msg, int ext1, int ext2,const Parcel *obj)
{
	TRACE();
	Mutex::Autolock N(mNotifyMutex);
	LOGV("cookie=%p,msg=%x,ext1=%x,ext2=%x\n",cookie,msg,ext1,ext2);
    if (msg == MEDIA_BLURAY_INFO) {
        if(mPlayer==NULL && oldmsg_num<9 && !subplayer_inited){
            oldmsg[oldmsg_num].msg=msg;
            oldmsg[oldmsg_num].ext1=ext1;
            oldmsg[oldmsg_num].ext2=ext2;
            oldmsg[oldmsg_num].obj=obj;
            oldmsg_num++;
            return;
        }
    }
	switch(msg){
			case MEDIA_NOP:	
				break;
			case 0x11000:/*Amlogic File type ready*/
				TRACE();
				mTypeReady=true;
				mCondition.signal();
				break;
			case 0x12000:/*Amlogic Found HTTP_WV*/				
				if(mRenderTid == -1){
					isRestartCreate = true;					
					char *turi = (char *)malloc(strlen(muri) + 8);
					int length;
					if(turi) {
					length = sprintf(turi, "widevine://%s", muri+7);	
					LOGV("Drm retry turi=%s\n", turi);
					}					
					muri=turi;
					LOGV("muri=%s prepare\n", muri);
                                        mRenderTid = 1;
    					createThreadEtc(startThread, this, "StartThread", ANDROID_PRIORITY_DEFAULT);
				}	
				break;	
			case MEDIA_BUFFERING_UPDATE:
				if (mPlayer!=NULL && mNotify!=NULL)
					mNotify(mCookie,msg, ext1, ext2,obj);
				break;
		    case MEDIA_PREPARED:
		    case MEDIA_SET_VIDEO_SIZE:
				Prepared=true;
				if(mPlayer==NULL&&oldmsg_num<9 && !subplayer_inited)//if not request prepared , store msg, otherwise send 
				{
        				oldmsg[oldmsg_num].msg=msg;
        				oldmsg[oldmsg_num].ext1=ext1;
        				oldmsg[oldmsg_num].ext2=ext2;
        				oldmsg[oldmsg_num].obj=obj;
        				oldmsg_num++;
				break;
				}
		    case MEDIA_PLAYBACK_COMPLETE:
		    case MEDIA_SEEK_COMPLETE:
		    case MEDIA_INFO:
#ifdef RECORD_LOG
                        if (ext1 == MEDIA_INFO_DOWNLOAD_START ||
                        ext1 == MEDIA_INFO_DOWNLOAD_END ||
                        ext1 == MEDIA_INFO_DOWNLOAD_ERROR ||
                        ext1 == MEDIA_INFO_HTTP_CONNECT_OK ||
                        ext1 == MEDIA_INFO_HTTP_CODE ||
                        ext1 == MEDIA_INFO_LIVE_SHIFT ||
                        ext1 == MEDIA_INFO_HTTP_REDIRECT ||
                        ext1 == MEDIA_INFO_HLS_SEGMENT ||
                        (ext1 == MEDIA_ERROR_PLAYER_REPORT && ext2 == 10001)) {
                        if (mNotify != NULL)
                            mNotify(mCookie,msg, ext1, ext2,obj);
                        break;
                    }
#else
                if (ext1 == MEDIA_ERROR_PLAYER_REPORT
                    && ext2 == 10001) {
                    if (mNotify != NULL) {
                        mNotify(mCookie,msg, ext1, ext2,obj);
                        break;
                    }
                }

                if (mPlayer!=NULL && mNotify!=NULL) {
                    mNotify(mCookie,msg, ext1, ext2,obj);
                    break;
                }
#endif
		    case MEDIA_ERROR:
			if((msg == MEDIA_ERROR) && !Prepared && (mPlayer != NULL)) {
				mPrepareErr = true;
			}
			if((msg == MEDIA_ERROR||msg == MEDIA_PREPARED)&&!mTypeReady){
				mTypeReady=true;
				mCondition.signal();
			}	
			if(mPlayer==NULL && oldmsg_num<9 && !subplayer_inited){
					oldmsg[oldmsg_num].msg=msg;
					oldmsg[oldmsg_num].ext1=ext1;
					oldmsg[oldmsg_num].ext2=ext2;
					oldmsg[oldmsg_num].obj=obj;
					oldmsg_num++;
			}
			default:
			if (mPlayer!=NULL&& mNotify!=NULL) {
				LOGI("MEDIA_ERROR:msg=%x,ext1=%x,ext2=%x\n",msg,ext1,ext2);
				if (msg == MEDIA_GET_FIRST_PCR) {
					mFirstPcr = 1;
					LOGI("First Pcr Comming \n");
				}
				mNotify(mCookie,msg, ext1, ext2,obj);
			}
	}
	
}
void	AmSuperPlayer::setNotifyCallback(void* cookie, notify_callback_f notifyFunc)
{
	TRACE();
	Mutex::Autolock l(mMutex);
	Mutex::Autolock N(mNotifyMutex);
	
	mCookie = cookie; 
	mNotify = notifyFunc;
	if (mPlayer == 0) return;
}

void AmSuperPlayer::setAudioSink(const sp<AudioSink> &audioSink) {
    MediaPlayerInterface::setAudioSink(audioSink);
	mAudioSink=audioSink;
	if(mAudioSink.get()!=NULL)
		mSessionID = mAudioSink->getSessionId();
}



bool AmSuperPlayer::PropIsEnable(const char* str)
{ 
	char value[PROPERTY_VALUE_MAX];
	if(property_get(str, value, NULL)>0)
	{
		if ((!strcmp(value, "1") || !strcmp(value, "true")))
		{
			LOGI("%s is enabled\n",str);
			return true;
		}
	}
	LOGI("%s is disabled\n",str);
	return false;
}
player_type  AmSuperPlayer::Str2PlayerType(const char *str)
{
	if(strcmp(str,"PV_PLAYER")==0)
		return PV_PLAYER;
	else if(strcmp(str,"SONIVOX_PLAYER")==0)
		return SONIVOX_PLAYER;
	else if(strcmp(str,"STAGEFRIGHT_PLAYER")==0)
		return STAGEFRIGHT_PLAYER;
	else if(strcmp(str,"AMLOGIC_PLAYER")==0)
		return AMLOGIC_PLAYER;
	else if(strcmp(str,"AMSUPER_PLAYER")==0)
		return AMSUPER_PLAYER;
	/*default*/
	return STAGEFRIGHT_PLAYER;
}

const char *  AmSuperPlayer::PlayerType2Str(player_type type)
{
	switch(type){
		case     PV_PLAYER: 			return "PV_PLAYER";
		case     SONIVOX_PLAYER: 		return "SONIVOX_PLAYER";
		case     STAGEFRIGHT_PLAYER: 	return "STAGEFRIGHT_PLAYER";
		case     AMLOGIC_PLAYER: 		return "AMLOGIC_PLAYER";
		case     AMSUPER_PLAYER: 		return "AMSUPER_PLAYER";
		case     TEST_PLAYER: 			return "TEST_PLAYER";
		default:						return "UNKNOWN_PLAYER";
	}
}

int AmSuperPlayer::match_codecs(const char *filefmtstr,const char *fmtsetting)
{
        const char * psets=fmtsetting;
        const char *psetend;
        int psetlen=0;
        char codecstr[64]="";
		if(filefmtstr==NULL || fmtsetting==NULL)
			return 0;

        while(psets && psets[0]!='\0'){
                psetlen=0;
                psetend=strchr(psets,',');
                if(psetend!=NULL && psetend>psets && psetend-psets<64){
                        psetlen=psetend-psets;
                        memcpy(codecstr,psets,psetlen);
                        codecstr[psetlen]='\0';
                        psets=&psetend[1];//skip ";"
                }else{
                        strcpy(codecstr,psets);
                        psets=NULL;
                }
                if(strlen(codecstr)>0){
                        if(strstr(filefmtstr,codecstr)!=NULL)
                                return 1;
                }
        }
        return 0;
}

player_type AmSuperPlayer::SuperGetPlayerType(char *type,int videos,int audios)
{
	int ret;
	char value[PROPERTY_VALUE_MAX];
	bool amplayer_enabed=PropIsEnable("media.amplayer.enable");

	if (PropIsEnable("media.amplayer.stagefright") ||
			match_codecs(type, "amsoftdec")){
		LOGE("SuperGetPlayerType use stagefright player\n");
		isSoftDec = true;
		return STAGEFRIGHT_PLAYER;
	}
	if(NULL!=mHardPara){
		if(match_codecs(type,mHardPara)){
			LOGV("%s type will use hard-decoder,force-list:%s\n",type,mHardPara);
			return AMLOGIC_PLAYER;
		}
	}
	if(NULL!=mSoftPara&&match_codecs(type,mSoftPara)){
		LOGV("%s type will use soft-decoder,force-list:%s\n",type,mSoftPara);
		if(match_codecs(type,"midi,mmf,mdi")){			
			return SONIVOX_PLAYER;
		}else {
			return STAGEFRIGHT_PLAYER;
		}
	}
	if(amplayer_enabed && type!=NULL)
	{
		bool audio_all,no_audiofile;
		//if(audios==0 && videos==0){
			/*parser get type but have not finised get videos and audios*/
			if(match_codecs(type,"mpeg,mpegts,rtsp,rtp")  /*some can't parser stream info in header parser*/
				&& !strstr(type, "hevc"))   /* hevc/h.265 in ts format not support by libplayer now */
			        return AMLOGIC_PLAYER;
		//}
		if (PropIsEnable("media.amplayer.widevineenable") && match_codecs(type, "drm,DRM,DRMdemux"))
			return AMLOGIC_PLAYER;	/* 	if DRM allways goto AMLOGIC_PLAYER	*/
		else if (match_codecs(type, "Demux_no_prot")) {
		    return AMLOGIC_PLAYER;  //for SS
		}else if (!strcmp(type, "asf-pr"))
                    return AMLOGIC_PLAYER;
		if (match_codecs(type, "webm,vp8,vp6,hevc,rmhw,rmsoft,wmv2,wmv1")) {
			if(match_codecs(type, "hevcHW")) {
			    goto PASS_THROUGH;
			}
			if(match_codecs(type, "hevc")) {
				isHEVC = true;
			}
			if(match_codecs(type, "rmhw")) {
				return AMLOGIC_PLAYER;
			}
			if (isHEVC && url_valid && (!strncasecmp("http://", muri, 7)
			    || !strncasecmp("https://", muri, 8))) { //if m3u8 file, only NU_Player can handle.
				size_t len = strlen(muri);
				if (len >= 5 && !strcasecmp(".m3u8", &muri[len - 5])) {
				return NU_PLAYER;
				}

				if (strstr(muri,"m3u8")) {
				return NU_PLAYER;
				}
			}
			return STAGEFRIGHT_PLAYER;
		}
PASS_THROUGH:
		if(videos>0)
			return AMLOGIC_PLAYER;
		audio_all=PropIsEnable("media.amplayer.audio-all");
		if (audios > 0 && (audio_all || (muri && strstr(muri, "m3u8"))))
			return AMLOGIC_PLAYER;


		ret=property_get("media.amplayer.enable-acodecs",value,NULL);
		if(ret>0 && (match_codecs(type,value)|| (!match_codecs(type,"ogg") && url_valid && IS_LOCAL_HTTP(muri)))){
			/*some local http(127.0.0.1) dont support switch http clinet,use old AmlogicPlayer*/
			return AMLOGIC_PLAYER;
		}		
			
	}

	if(match_codecs(type,"midi,mmf,mdi"))
		return SONIVOX_PLAYER;
	
	if(match_codecs(type,"m4a")) {
		ret=property_get("media.amsuperplayer.m4aplayer",value,NULL);
		if (ret>0) {
			LOGI("media.amsuperplayer.m4aplayer=%s\n",value);
			return AmSuperPlayer::Str2PlayerType(value);
		}
	}
    // if mp2, return AMLogic Player
#if 0
    if(match_codecs(type,"mp2")){
      LOGI("[%s, %d]:This is MP3 LAYER 2!!!\n", __func__, __LINE__);
      return AMLOGIC_PLAYER;
    }
#endif
    if (PropIsEnable("media.stagefright.enable-player")) {
        return STAGEFRIGHT_PLAYER;
    }

    return STAGEFRIGHT_PLAYER;
    

}

static sp<MediaPlayerBase> createPlayer(player_type playerType, void* cookie,
        notify_callback_f notifyFunc)
{

    sp<MediaPlayerBase> p;
	LOGV("createPlayer");
    switch (playerType) {
        case SONIVOX_PLAYER:
            LOGV(" create MidiFile");
            p = new MidiFile();
            break;
        case STAGEFRIGHT_PLAYER:
            LOGV(" create StagefrightPlayer");
            p = new StagefrightPlayer;
            break;
		case NU_PLAYER:
            LOGV(" create NuPlayer");
            p = new NuPlayerDriver;
            break;	
        case TEST_PLAYER:
            LOGV("Create Test Player stub");
            p = new TestPlayerStub();
            break;
		case AMSUPER_PLAYER:
			 LOGV("Create AmSuperPlayer ");
			p = new AmSuperPlayer();
			break;
		case AMLOGIC_PLAYER:
			#ifdef BUILD_WITH_AMLOGIC_PLAYER
            LOGV("Create Amlogic Player");
            p = new AmlogicPlayer();
			#else
			LOGV("Have not Buildin Amlogic Player Support");
			#endif
			
            break;	
    }
    if (p != NULL) {
        if (p->initCheck() == NO_ERROR) {
            p->setNotifyCallback(cookie, notifyFunc);
        } else {
            p.clear();
        }
    }
    if (p == NULL) {
        LOGE("Failed to create player object");
    }
    return p;
}


sp<MediaPlayerBase>	AmSuperPlayer::CreatePlayer()
{
	sp<MediaPlayerBase> p;
	char *filetype;
	int mvideo,maudio;
	status_t sret;
	int needretry=0;
	
	player_type newtype=AMLOGIC_PLAYER;
	//p= new AmlogicPlayer();
Retry:
	mTypeReady=false;
	needretry=0;
	oldmsg_num=0;
	{
        /*
		Mutex::Autolock N(mNotifyMutex);
		oldmsg_num=0;
		memset(oldmsg,0,sizeof(oldmsg));
        */
	}

	if(muri != NULL && strstr(muri, ".mp3")){
		newtype = STAGEFRIGHT_PLAYER;
		LOGI("use mp3 STAGEFRIGHT_PLAYER\n");
	}
	p=android::createPlayer(newtype, this,notify);
	if (!p->hardwareOutput()) {
        static_cast<MediaPlayerInterface*>(p.get())->setAudioSink(mAudioSink);
    }
	TRACE();
	if(p->playerType()==STAGEFRIGHT_PLAYER&&isHEVC) {
		p->setHEVCFlag(true);
	}
	if (p->playerType() == STAGEFRIGHT_PLAYER && isSoftDec) {
		LOGI("use amlogic soft decoder\n");
		Parcel tmprequest;
		p->setParameter(KEY_PARAMETER_AML_PLAYER_USE_SOFT_DEMUX, tmprequest);
	}
	if(url_valid)
		sret=p->setDataSource(muri,&mheaders);
	else if(fd_valid)
		sret=p->setDataSource(mfd,moffset,mlength);
	else 
		return NULL;
	if(sret!=OK){
		LOGV("player setDataSource failed,error num %d,player type %d \n",sret,	p->playerType());		
		int isamplayer=(p->playerType()==AMLOGIC_PLAYER);
		p->stop();
		p.clear();
		p=NULL;
		if(isamplayer){
			newtype=SuperGetPlayerType(NULL,0,0);
			goto Retry;
		}
		return NULL;
	}
	if(p->playerType()==AMLOGIC_PLAYER){
		AmlogicPlayer* amplayer1;
		amplayer1=(AmlogicPlayer *)p.get();
		amplayer1->setSessionID(mSessionID);
	}
	{
		Parcel tmprequest;
		Parcel reply;
		tmprequest.setDataPosition(0);
		tmprequest.writeInt32(INVOKE_ID_SET_VIDEO_SCALING_MODE);
		tmprequest.writeInt32(mVideoScalingMode);
		tmprequest.setDataPosition(0);
		p->invoke(tmprequest,&reply);	
	}
	p->prepareAsync();
	
	if (muri!=NULL && !strncasecmp("tvin:", muri, 5)) {
	    mTypeReady = true;
	}
	
	if(!mTypeReady && p->playerType()==AMLOGIC_PLAYER){
		AmlogicPlayer* amplayer; 
		bool FileTypeReady=false;
		amplayer=(AmlogicPlayer *)p.get();
		while(!mTypeReady)
		{
			int ret;
			Mutex::Autolock l(mMutex);
			if(mEXIT||mRequsetEXIT)
                                break;
			mCondition.wait(mMutex);
		}
		if(mTypeReady){
				FileTypeReady=!amplayer->GetFileType(&filetype,&mvideo,&maudio);
				TRACE();		
		}
		if(FileTypeReady){
			LOGV("SuperGetPlayerType:type=%s,videos=%d,audios=%d\n",filetype,mvideo,maudio);
			newtype=SuperGetPlayerType(filetype,mvideo,maudio);
			LOGV("GET New type =%d\n",newtype);
			if(isSwitchURL)
			{
				/*in drm switch mode,and if not drm,we may need to more check,*/
				if(strstr(filetype,"DRMdemux")==NULL && strcasestr(filetype,"drm")==NULL &&  mOUrl.length()>0)
				{	/*not drm streaming,goto old url,*/
					if(muri!=NULL) free((void*)muri);
					muri=strdup(mOUrl.string());
					needretry=1;	
					isSwitchURL=false;
				}
			}
		}else if(isSwitchURL && mOUrl.length()>0){		
		 	/*is switched url for widevine,switched to orignal url,and try again*/
			if(muri!=NULL) free((void*)muri);
			muri=strdup(mOUrl.string());
			needretry=1;	
			isSwitchURL=false;
		}else{
			
			newtype=SuperGetPlayerType(NULL,0,0);
		}
	}
	TRACE();
	if((mEXIT||mRequsetEXIT)&& !mTypeReady){
                 LOGV("amlogicplayer already ok donn't clear client\n");
		p->stop();
		p.clear();
		p=NULL;
	}
	else if(needretry||newtype!=p->playerType()){
		LOGV("Need to creat new player=%d\n",newtype);
		p->stop();
		p.clear();
		if(fd_valid){
			lseek(mfd,moffset,SEEK_SET);/*reset to before*/
		}
		p=NULL;
		goto Retry;
	}
	TRACE();
	LOGV("Start new player now=%d\n",newtype);
	return p;
}

int AmSuperPlayer::startThread(void*arg)
{
	AmSuperPlayer * p=(AmSuperPlayer *)arg;
	return p->initThread();
}
int AmSuperPlayer::initThread()
{		
        subplayer_inited = false;
	if (isRestartCreate == true) {
		stop();
		if (mPlayer == NULL) return NO_ERROR;
		mPlayer.clear();
		if(NULL!=mHardPara){
			free((void*)mHardPara);
			mHardPara = NULL;
		}
		if(NULL!=mSoftPara){
			free((void*)mSoftPara);
			mSoftPara = NULL;
		}
	}
	{
	  // 	Mutex::Autolock l(mMutex);
	    //mRenderTid = myTid();
	    ///mCondition.signal();
	}
	{
		sp<MediaPlayerBase> p=CreatePlayer();
		{
			if(p.get()==NULL)
				mEXIT = true;/*exit on ERRORS...*/
			Mutex::Autolock l(mMutex);/*lock surface setting.*/
			if(msurfaceTexture != NULL && p.get()!=NULL)
        			p->setVideoSurfaceTexture(msurfaceTexture);
			mPlayer=p;
		}
		
	}
	//wait prepare request comming
	while(!mRequestPrepared)
	{
		int ret;
		usleep(1000*10);
		if(mRequsetEXIT)
			break;
	}
	if (mPlayer!=NULL &&  mNotify!=NULL) {
		int i;
		Mutex::Autolock N(mNotifyMutex);
		for(i=0;i<oldmsg_num;i++){
			mNotify(mCookie,oldmsg[i].msg,oldmsg[i].ext1,oldmsg[i].ext2,oldmsg[i].obj);
		}
		oldmsg_num=0;
		subplayer_inited=true;
	}else if(mPlayer==NULL){
		if(mNotify!=NULL) {
			if(mRequsetEXIT) {
				LOGI("don't send init error,when stoped\n");
			} else{
			        int error_notifyed = 0;
                    	        int i;
                                Mutex::Autolock N(mNotifyMutex);
                                for(i=0;i<oldmsg_num;i++){
                                    mNotify(mCookie,oldmsg[i].msg,oldmsg[i].ext1,oldmsg[i].ext2,oldmsg[i].obj);
                                    if(oldmsg[i].msg == MEDIA_ERROR){ //init fail only send error msg
                                        error_notifyed=1;
                                    }
                                }
                                oldmsg_num=0;
                                subplayer_inited=true;
                                if (!error_notifyed){
                                    mNotify(mCookie,MEDIA_ERROR,MEDIA_ERROR_UNKNOWN,-1,NULL);
                                }
                        }
                    }
	}
	//Mutex::Autolock l(mMutex);
	mMutex.lock();
	mRenderTid = -1;
	if(isRestartCreate == false)
		mCondition.signal();
	TRACE();
	mMutex.unlock();
	LOGI("mSpecialState:%d, mFirstPcr:%d\n", mSpecialState, mFirstPcr);
	if (mSpecialState == 1) {
		while(!mRequsetEXIT) {
			LOGI("test mSpecialState:%d, mFirstPcr:%d\n", mSpecialState, mFirstPcr);
			if (mFirstPcr == 1) {
				if (mSpecialState == 3) {
					LOGI("exec delay seekTo %dms\n", mFirstSeekMs);
					mPlayer->seekTo(mFirstSeekMs);
				}

				mSpecialState = 0;
				break;
			}
			usleep(5000);
		}
	}

	return 0;
}

status_t   AmSuperPlayer::stopPlayerIfNeed()
{
	TRACE();
	Mutex::Autolock l(mMutex);
	TRACE();
	if (mPlayer == 0) {
		return UNKNOWN_ERROR;
	}
	return mPlayer->stopPlayerIfNeed();
}


status_t AmSuperPlayer::dump(int fd, const Vector<String16> &args) const
{		
	if (mPlayer == 0) return UNKNOWN_ERROR;	
	return mPlayer->dump(fd, args);
}


}

