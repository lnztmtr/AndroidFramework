
/*
**
** Copyright 2008, The Android Open Source Project
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

#ifndef ANDROID_AMSUPERPLAYER_H
#define ANDROID_AMSUPERPLAYER_H


#include <utils/threads.h>

#include <drm/DrmInfoRequest.h>
#include <media/MediaPlayerInterface.h>
#include <media/AudioTrack.h>

#include "am_media_private.h"

namespace android {

struct ISurfaceTexture;

typedef struct notify_msg{
	int msg,ext1,ext2;
	const Parcel *obj;
}notify_msg_t;

class AmSuperPlayer : public MediaPlayerInterface{ 
public:
	                    AmSuperPlayer();
                        ~AmSuperPlayer();

    virtual void        onFirstRef();
    virtual status_t    initCheck();
	
    virtual status_t    setDataSource(
            const char *uri, const KeyedVector<String8, String8> *headers);

    virtual status_t    setDataSource(int fd, int64_t offset, int64_t length);
	//virtual status_t    setVideoSurfaceTexture(
    //                            const sp<ISurfaceTexture>& surfaceTexture);
	virtual status_t    setVideoSurfaceTexture(
                                const sp<IGraphicBufferProducer>& bufferProducer);
    virtual status_t    prepare();
    virtual status_t    prepareAsync();
    virtual status_t    prepareAsync_nolock();
    virtual status_t    start();
    virtual status_t    stop();
    virtual status_t    seekTo(int msec);
    virtual status_t    pause();
    virtual bool        isPlaying();
    virtual status_t    getCurrentPosition(int* msec);
    virtual status_t    getDuration(int* msec);
    virtual status_t    release();
    virtual status_t    reset();
    virtual status_t    setLooping(int loop);
    virtual player_type playerType() { return AMSUPER_PLAYER; }
	virtual bool        hardwareOutput();
	virtual void 		setAudioSink(const sp<AudioSink> &audioSink);
    virtual status_t    invoke(const Parcel& request, Parcel *reply);
	virtual status_t    getMetadata(const media::Metadata::Filter& ids,Parcel *records);
	virtual void        setNotifyCallback(void* cookie, notify_callback_f notifyFunc);
	virtual status_t    setParameter(int key, const Parcel &request);
    virtual status_t    getParameter(int key, Parcel *reply);
	
	static  void        notify(void* cookie, int msg, int ext1, int ext2,const Parcel *obj);
	   void        		Notify(void* cookie, int msg, int ext1, int ext2,const Parcel *obj);
	static	player_type  Str2PlayerType(const char *str);
	static  const char * PlayerType2Str(player_type type);
	virtual status_t dump(int fd, const Vector<String16> &args) const;
	virtual status_t  stopPlayerIfNeed();


	
private:
	player_type 		SuperGetPlayerType(char *type,int videos,int audios);
	int 				match_codecs(const char *filefmtstr,const char *fmtsetting);
	
	bool				PropIsEnable(const char* str);
	int				PropGetInt(const char* str, int def);
	sp<MediaPlayerBase>	CreatePlayer();
	static  int         startThread(void*);
	
            int         initThread();
	
	Mutex               mMutex;
	Mutex               mNotifyMutex;
	notify_msg_t		oldmsg[10];
	int 				oldmsg_num;
	
	Condition           mCondition;
	sp<MediaPlayerBase>	mPlayer;
	player_type			current_type;
	sp<AudioSink>		mAudioSink;
	sp<Surface>   		mSurface;
	//sp<ISurfaceTexture> msurfaceTexture;
	sp<IGraphicBufferProducer> msurfaceTexture;
	bool 				url_valid;
	bool 				mLoop;

	const char 			*muri;
	String8 				mOUrl; 
	KeyedVector<String8, String8> mheaders;

	bool 				fd_valid;
	int 				mfd;
	int64_t 			moffset;
	int64_t 			mlength;
	void*				mCookie;
	notify_callback_f		mNotify;
	int 				steps;
	status_t            		mState;
	bool 				mTypeReady;
	bool 				Prepared;
	bool		                mRequsetEXIT;
	bool		                mEXIT;
	bool 				subplayer_inited;;
	pid_t				mRenderTid;
	const char 			*mSoftPara;
	const char			*mHardPara;
	bool				        isRestartCreate;
	bool					 isSwitchURL;

	bool                              isHEVC;
	bool				isSoftDec;
	int					mSessionID;
	int 					mVideoScalingMode;
	int isStartedPrepared;
	int mRequestPrepared;
	int mPrepareErr;
	int mFirstPcr;
	int mFirstSeekMs;
	int mSpecialState; //0-initial, 1-started, 2-(First start->seekTo), 3-normal
	int64_t mBufferingEndTimeS;
	bool mbDropBufferingEvent;
	int mDisableBufferingTimeS;
};
	
}; // namespace android


#endif // ANDROID_AMSUPERPLAYER_H


