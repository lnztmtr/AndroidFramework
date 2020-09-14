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

#ifndef ANDROID_AMLOGICPLAYER_H
#define ANDROID_AMLOGICPLAYER_H


#include <utils/threads.h>

#include <drm/DrmManagerClient.h>
#include <media/MediaPlayerInterface.h>
#include <media/AudioTrack.h>
#include <media/stagefright/MediaSource.h>
//#include <ui/Overlay.h>
#include "AmlogicPlayerRender.h"
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <media/stagefright/DataSource.h>
#include <media/stagefright/MediaBuffer.h>
#include <media/stagefright/MediaDefs.h>
#include <media/stagefright/MediaSource.h>
#include <media/stagefright/MetaData.h>

extern "C" {
#include "libavutil/avstring.h"
#include <Amavutils.h>
#include "libavformat/avformat.h"
}
#include "AmlogicPlayerStreamSource.h"
#include "AmlogicPlayerDataSouceProtocol.h"
#include <player.h>
#include <player_ctrl.h>

#include "am_media_private.h"

typedef struct AmlogicPlayer_File {
    char            *datasource; /* Pointer to a FILE *, NULL. */
    int              seekable;
    int               oldfd;
    int               fd;
    int               fd_valid;
    int64_t          mOffset;
    int64_t          mCurPos;
    int64_t          mLength;
} AmlogicPlayer_File;
#define OverlayRef ANativeWindow
namespace android
{

struct ISurfaceTexture;
class TimedTextDriver;

class AmlogicPlayer : public MediaPlayerInterface
{

public:
    static  status_t    BasicInit();
    static  status_t    exitAllThreads();
    static      bool    PropIsEnable(const char* str, bool def = false);
    static  float       PropGetFloat(const char* str, float def = 0.0);
    static void         SetCpuScalingOnAudio(float mul_audio);
    static      int      GetCallingAPKName(char *name, int size);
    AmlogicPlayer();
    ~AmlogicPlayer();

    virtual void        onFirstRef();
    virtual status_t    initCheck();

    virtual status_t    setDataSource(
        const char *uri, const KeyedVector<String8, String8> *headers);

    virtual status_t    setDataSource(int fd, int64_t offset, int64_t length);
    virtual status_t    setDataSource(const sp<IStreamSource> &source) ;
	//virtual status_t    setVideoSurfaceTexture(
    //                            const sp<ISurfaceTexture>& surfaceTexture);
	virtual status_t    setVideoSurfaceTexture(
                                const sp<IGraphicBufferProducer>& bufferProducer);
    virtual status_t    setParameter(int key, const Parcel &request);
    virtual status_t    getParameter(int key, Parcel *reply);
    virtual status_t    setVolume(float leftVolume, float rightVolume);

    virtual status_t    prepare();
    virtual status_t    prepareAsync();
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
    virtual player_type playerType() {
        return AMLOGIC_PLAYER;
    }
    virtual bool        hardwareOutput() {
        return false;
    };
    virtual status_t    invoke(const Parcel& request, Parcel *reply);
    virtual status_t    getMetadata(const media::Metadata::Filter& ids, Parcel *records);

    static int          notifyhandle(int pid, int msg, unsigned long ext1, unsigned long ext2);
    int                 NotifyHandle(int pid, int msg, unsigned long ext1, unsigned long ext2);
    int                 UpdateProcess(int pid, player_info_t *info);
    int                 GetFileType(char **typestr, int *videos, int *audios);
    void                setSessionID(int sessionID) {
        mSessionID = sessionID;
    }
    virtual status_t dump(int fd, const Vector<String16> &args) const;
    virtual status_t stopPlayerIfNeed();
    size_t countTracks() const;
    int match_name(const char *name,const char *machsetting);


private:
    status_t    setdatasource(const char *path, int fd, int64_t offset, int64_t length, const KeyedVector<String8, String8> *headers);
    status_t    reset_nosync();
    status_t    createOutputTrack();
    static  int         renderThread(void*);
    int         render();

    static void         initOverlay_l();
    void         VideoViewOn(void);
    void         VideoViewClose(void);

    int                 getintfromString8(String8 &s, const char*pre);

    status_t            updateMediaInfo(void);
    status_t            initVideoSurface(void);
    status_t            UpdateBufLevel(hwbufstats_t *pbufinfo);


    //helper functions
    const char* getStrAudioCodec(int atype);
    const char* getStrVideoCodec(int vtype);
    int isUseExternalModule(const char* mod_name);
    static int          vp_open(URLContext *h, const char *filename, int flags);
    static int          vp_read(URLContext *h, unsigned char *buf, int size);
    static int          vp_write(URLContext *h, unsigned char *buf, int size);
    static int64_t      vp_seek(URLContext *h, int64_t pos, int whence);
    static int          vp_close(URLContext *h);
    static int          vp_get_file_handle(URLContext *h);
    status_t getTrackInfo(Parcel* reply);
    status_t getMediaInfo(Parcel* reply) const;
    status_t selectPid(int video_pid) const;
    status_t selectTrack(int index, bool select)const;
    status_t dump_videoinfo(int fd, media_info_t mStreamInfo)const;
    status_t dump_streaminfo(int fd, media_info_t mStreamInfo)const;
    status_t dump_audioinfo(int fd, media_info_t mStreamInfo)const;
    status_t dump_subtitleinfo(int fd, media_info_t mStreamInfo)const;
    int get_cur_dispmode();
    int set_cur_dispmode(int mode);
    // audio/sub track function for hls/dash demuxer.
    status_t getStreamingTrackInfo(Parcel * reply) const;
    status_t selectStreamingTrack(int index, bool select) const;
    int getStreamingSelectedTrack(const Parcel& request) const;
    status_t updateReportParam(Parcel * reply);

    sp<Surface>             mSurface;
    sp<ISurfaceTexture>     mSurfaceTexture;
    sp<ANativeWindow>       mNativeWindow;
    sp<AmlogicPlayerRender> mPlayerRender;

    Mutex               mMutex;
    mutable Mutex mLock;
    Condition           mCondition;
    AmlogicPlayer_File  mAmlogicFile;
    int                 mPlayTime;
    int64_t         mLastPlayTimeUpdateUS;
    int64_t         mLastStreamTimeUpdateUS;
    int             LatestPlayerState;
    int         mStreamTime;
    int         mStreamTimeExtAddS;/**/
    int                 mDuration;
    status_t            mState;
    int                 mStreamType;
    bool                mLoop;
    bool                mAndroidLoop;
    volatile bool       mExit;
    bool                mPaused;
    bool                mLatestPauseState;    
    volatile bool       mRunning;
    bool                mChangedCpuFreq;
    play_control_t      mPlay_ctl;
    int                 mPlayer_id;
    int                 mWidth;
    int                 mHeight;
    int                 mAspect_ratio_num;
    int                 mAspect_ratio_den;
    int                 video_rotation_degree;

    char                mTypeStr[64];
    int                 mhasVideo;
    int                 mhasAudio;
    int                 mhasSub;
    bool                mIgnoreMsg;
    bool                mTypeReady;
    media_info_t        mStreamInfo;
    bool                streaminfo_valied;
    int64_t                 PlayerStartTimeUS;
    int                 mDispLastFrame;

    //added extend info
    char* mStrCurrentVideoCodec;
    char* mStrCurrentAudioCodec;

    int                 mAudioTrackNum;
    int                 mVideoTrackNum;
    int                 mInnerSubNum;
    char*           mAudioExtInfo; //just json string,such as "{ "id": id, "format":format,"bitrate":bitrate,...;"id":id,...}",etc
    char*           mSubExtInfo;
    char*                    mVideoExtInfo;
    bool                mInbuffering;
    bool            mInBufferingBroadcast;
    Rect            curLayout;
    int                 mDelayUpdateTime;
    bool                mEnded;
    bool                            mHttpWV;
    bool                isTryDRM;
    int                 mSessionID;
    bool            mNeedResetOnResume;
    bool                         isHDCPFailed;
    bool                        isWidevineStreaming;
    bool                        isSmoothStreaming;

    sp<AmlogicPlayerStreamSource> mStreamSource;
    sp <AmlogicPlayerDataSouceProtocol> mSouceProtocol;
    sp<IStreamSource> mSource;
    int fastNotifyMode;// IStreamSource& netflix need it;
    int     mStopFeedingBuf_ms;
    bool mLowLevelBufMode;/*save less data on player.*/
    sp <DecryptHandle> mDecryptHandle;
    DrmManagerClient *mDrmManagerClient;
    int mHWaudiobufsize;
    int mHWvideobufsize;
    float mHWaudiobuflevel;
    float mHWvideobuflevel;
    int mVideoScalingMode;
    bool isHTTPSource;
    char CallingAPkName[64];
    int  licenseOfApk;  // 1. manguo
    //for new start mode
    wp<MediaPlayerBase> mListener;
    TimedTextDriver *mTextDriver;
    sp<MediaSource> mSubSource;
    bool enableOSDVideo;

	int drop_tiny_seek_ms;/*if seek jump is less than drop_tiny_seek_ms ignore.*/
    bool mFFStatus; //fast backward status indicator
    int mSupportSeek; // -1 not set 0 disable 1 enable
    float mLeftVolume;
    float mRightVolume;
    int mSetVolumeFlag; //when amadec is not ready, but need to set volume, change this flag. after start reset volume 
    int64_t bufferTime;
    float mDelaySendBufferingInfo_s;
    int DtshdApreTotal;
    int DtsHdStreamType;
    int DtsHdMulAssetHint;
    int DtsHdHpsHint;
    int   mAdecoder_Enable;	
    int request_quit;
    int mAudioChannelMode;
    int mAudioChannelModeSetOK;
    int mSeekdone;
    int mLastPlaytime;
    int64_t mLastPosition;
    int64_t mStartTimeUs;
    int disable_buffering_notify;
    int mCurrentTimeMS;
    int mCurAudioInfoID;
    int64_t seek_starttime;
    int mTrackMap[100];    // For select track
    int64_t mLastSeekTimeUS;
    Parcel* mParcel;
	int m_FirstPic;
	int m_notify_decode_start;
	int m_notify_get_playmode;
	int64_t prepare_start;
	int64_t prepare_complete;
	int64_t first_pic_cometime;
	int sgmt_start_num;
	int sgmt_complete_time;
	int mBuffering_time;
	int mFirstFrame_starttime;
        int mTotalBufferdTime;
    int mLastPlayerMsg;
	int64_t mPlayerInitingTimeUsec;
    int mNetErrorPosted;
    int mLastToggleCount;
    float mSpeed;
};

}; // namespace android

#endif // ANDROID_AMLOGICPLAYER_H

