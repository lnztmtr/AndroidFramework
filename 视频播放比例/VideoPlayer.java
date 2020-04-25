package com.meson.videoplayer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.SystemWriteManager; 
import android.app.MboxOutputModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.Metadata;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayerAmlogic;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.IWindowManager;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerPolicy;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.LayoutInflater;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
//import android.widget.VideoView;
import android.text.TextUtils;

import com.android.internal.app.LocalePicker;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.Process;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import java.util.Random;
import android.media.TimedText;
import android.media.SubtitleData;
import java.util.Locale;

public class VideoPlayer extends Activity { 
    private static String TAG = "VideoPlayer";
    private boolean DEBUG = false;
    private Context mContext;
    
    private static SystemWriteManager sw; 
    PowerManager.WakeLock mScreenLock = null;

    private boolean backToOtherAPK = true;
    private Uri mUri = null;
    private Map<String, String> mHeaders;
    private boolean mHdmiPlugged;

    private LinearLayout ctlbar = null; //for OSD bar layer 1; controller bar 
    private LinearLayout optbar = null; //for OSD bar layer 2; option bar
    private LinearLayout subwidget = null; //for subtitle switch
    private LinearLayout otherwidget = null; //for audio track, resume play on/off, repeat mode, display mode
    private LinearLayout infowidget = null; //for video infomation showing 

    private RelativeLayout msgwidget = null; //for message show on playing interface
    private TextView msgName;
    private TextView msgResolution;
    private TextView msgTime;
    private TextView msgCpuloading;
    private TextView msgType;
    private TextView msgBitrate;
    private TextView msgFps;
    private TextView msgVFormat;
    private TextView msgAFormat;
    private TextView msgSFormat;
    
    private SeekBar progressBar; // all the follow for OSD bar layer 1
    private TextView curTimeTx = null;
    private TextView totalTimeTx = null;
    private ImageButton browserBtn = null;
    private ImageButton preBtn = null;
    private ImageButton fastreverseBtn = null;
    private ImageButton playBtn = null;
    private ImageButton nextBtn = null;
    private ImageButton fastforwordBtn = null;
    private ImageButton optBtn = null;

    private ImageButton ctlBtn = null; // all the follow for OSD bar layer 2
    private ImageButton resumeModeBtn = null;
    private ImageButton repeatModeBtn = null; 
    private ImageButton audiooptionBtn = null;
    private ImageButton subtitleSwitchBtn = null;
    private ImageButton chapterBtn = null;
    private ImageButton displayModeBtn = null;
    private ImageButton brigtnessBtn = null;
    private AlertDialog mtimeSetting = null;
    private ImageButton fileinfoBtn = null;
    private ImageButton play3dBtn = null;
    private TextView otherwidgetTitleTx = null;
    private boolean progressBarSeekFlag = false;

    //for subtitle
    private TextView subtitleTV = null;
    private ImageView subtitleIV = null;
    //certification view
    private ImageView certificationDoblyView = null;
    private ImageView certificationDoblyPlusView = null;
    private ImageView certificationDTSView = null;
    private ImageView certificationDTSExpressView = null;
    private ImageView certificationDTSHDMasterAudioView = null;

    //store index of file postion for back to file list
    private int item_position_selected; // for back to file list view
    private int item_position_first;
    private int fromtop_piexl;
    private int item_position_selected_init; 
    private boolean item_init_flag = true;
    private ArrayList<Integer> fileDirectory_position_selected = new ArrayList<Integer>();
    private ArrayList<Integer> fileDirectory_position_piexl = new ArrayList<Integer>();

     // All the stuff we need for playing and showing a video
    //private VideoView mSurfaceView;
    private SurfaceView mSurfaceView;
    private View mSurfaceViewRoot; 
    private SurfaceHolder mSurfaceHolder = null;
    private boolean surfaceDestroyedFlag = true;
    private int totaltime = 0;
    private int curtime = 0;
    //@@private int mVideoWidth;
    //@@private int mVideoHeight;
    //@@private int mSurfaceWidth;
    //@@private int mSurfaceHeight;
    private OnCompletionListener mOnCompletionListener;

    Option mOption = null;
    Bookmark mBookmark = null;
    ResumePlay mResumePlay = null;
    PlayList mPlayList = null;
    MediaInfo mMediaInfo = null;
    ErrorInfo mErrorInfo = null;
    MediaPlayer.TrackInfo[] mTrackInfo;
    
    private boolean browserBackDoing = false;
    private boolean browserBackInvokeFromOnPause = false; //browserBack invoked by back keyevent and browserBack button as usually, if invoked from OnPause suppose to meaning HOME key pressed
    private boolean playmode_switch = true;

    private float mTransitionAnimationScale = 1.0f;
    private long mResumeableTime = Long.MAX_VALUE;
    private int mVideoPosition = 0;
    private boolean mHasPaused = false;
    private boolean intouch_flag = false;
    private boolean set_3d_flag = false;

    private AudioManager mAudioManager;
    private boolean mAudioFocused = false;
    

    Toast channel_display = null;
    private int channel_id = 0;
    private boolean mIsBluray = false;
    private ArrayList<String> mBlurayVideoLang = null;
    private ArrayList<String> mBlurayAudioLang = null;
    private ArrayList<String> mBluraySubLang = null;
    private static List<LocalePicker.LocaleInfo> LOCALES;
    private int mSubIndex = 0;
    private static final int SUBTITLE_PGS = 2;
    private static final int SUBTITLE_DVB = 6;
    private static final int SUBTITLE_TMD_TXT = 7;
    private ArrayList<ChapterInfo> mBlurayChapter = null;
    private int mListViewHeight = ViewGroup.LayoutParams.WRAP_CONTENT;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ArrayAdapter<LocalePicker.LocaleInfo> locales = LocalePicker.constructAdapter(this);
        int count = locales.getCount();
        LOCALES = new ArrayList<LocalePicker.LocaleInfo>(count);
        for (int i = 0; i < count; i++) {
            LOCALES.add(locales.getItem(i));
        }
        sw = (SystemWriteManager) getSystemService("system_write"); 
        
        LOGI(TAG,"[onCreate]");
        setContentView(R.layout.control_bar);
        setTitle(null);

        mAudioManager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        mScreenLock = ((PowerManager)this.getSystemService(Context.POWER_SERVICE)).newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK |PowerManager.ON_AFTER_RELEASE,TAG);
        
        //uncaughtException execute
        /*Thread.currentThread().setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            public void uncaughtException(Thread thread, Throwable ex) {    
                VideoPlayer.this.sendBroadcast(new Intent("com.meson.videoplayer.PLAYER_CRASHED"));
                LOGI(TAG,"----------------uncaughtException--------------------");
                showSystemUi(true);
                stop();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });*/

        init();
        if(0 != checkUri()) return;
        storeFilePos();
        ////showCtlBar();
    }

    @Override
    public void onResume() {
        super.onResume();
        LOGI(TAG,"[onResume]mResumePlay.getEnable():"+mResumePlay.getEnable()+",isHdmiPlugged:"+isHdmiPlugged);

        //close transition animation
        mTransitionAnimationScale = Settings.System.getFloat(mContext.getContentResolver(),
            Settings.System.TRANSITION_ANIMATION_SCALE, mTransitionAnimationScale);
        IWindowManager iWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        try {
            iWindowManager.setAnimationScale(1, 0.0f);
        }
            catch (RemoteException e) {
        }

        browserBackDoing = false;
        browserBackInvokeFromOnPause = false;

        //WakeLock acquire
        closeScreenOffTimeout();
        
        // Tell the music playback service to pause
        Intent intent = new Intent("com.android.music.musicservicecommand");
        intent.putExtra("command", "pause");
        mContext.sendBroadcast(intent);

        //init time store
        mErrorTime = java.lang.System.currentTimeMillis();
        mErrorTimeBac= java.lang.System.currentTimeMillis();

        if(mResumePlay != null && mPlayList != null) {
            if(true == mResumePlay.getEnable()) {
                //we dont need to stop video when do resume
                /*if(isHdmiPlugged == true) {
                    browserBack();
                    return;
                }*/
                
                String path = mResumePlay.getFilepath();
                for(int i=0;i<mPlayList.getSize();i++) {
                    String tempP = mPlayList.get(i);
                    if(tempP.equals(path)) {
                        // find the same file in the list and play
                        LOGI(TAG,"[onResume] start resume play, path:"+path+",surfaceDestroyedFlag:"+surfaceDestroyedFlag);
                        if(new File(path).exists()) {
                            LOGI(TAG,"[onResume] resume play file exists,  path:"+path);
                            if(surfaceDestroyedFlag) { //add for press power key quickly 
                                initVideoView(); //file play will do in surface create
                            }
                            else {
                                //browserBack();
                                initPlayer();
                                mPath = path;
                                sendPlayFileMsg();
                            }
                        }
                        else {
                            /*if(mContext != null)
                                Toast.makeText(mContext,mContext.getText(R.string.str_no_file),Toast.LENGTH_SHORT).show();  
                            browserBack();*/
                            retryPlay();
                        }
                        break;
                    }
                }
            }
        }

        registerHdmiReceiver();
        registerMountReceiver();
        registerPowerReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LOGI(TAG,"[onDestroy]");
        if(mResumePlay != null) {
            mResumePlay.setEnable(false); //disable resume play 
        }
        release();
        surfaceDestroyedFlag = true;
        LOGI(TAG,"[onDestroy] surfaceDestroyedFlag:"+surfaceDestroyedFlag);
    }

    @Override
    public void onPause() {
        super.onPause();
        LOGI(TAG,"[onPause] curtime:"+curtime);

        mErrorTime = 0;
        mErrorTimeBac = 0;

        if(randomSeekEnable()) { // random seek for test
            randomSeekTestFlag = true;
        }

        //stop switch timer
        stopSwitchTimeout();

        //close certification
        closeCertification();

        //close msg widget
        exitMsgWidget();

        //set book mark
        if(mBookmark != null) {
            if(confirm_dialog != null && confirm_dialog.isShowing() && exitAbort == false) {
                bmPos = 0;
                exitAbort = true;
                confirm_dialog.dismiss();
                mBookmark.set(mPlayList.getcur(), 0);
            }
            else {
                mBookmark.set(mPlayList.getcur(), curtime);
            }
        }

        if(progressBar != null) //add for focus changed to highlight playing item in file list
            progressBar.requestFocus();
        resetVariate();
        openScreenOffTimeout();
        unregisterHdmiReceiver();
        unregisterMountReceiver();
        unregisterPowerReceiver();

        if(mHandler != null) {
            mHandler.removeMessages(MSG_UPDATE_PROGRESS);
            mHandler.removeMessages(MSG_PLAY);
            mHandler.removeMessages(MSG_STOP);
            mHandler.removeMessages(MSG_RETRY_PLAY);
            mHandler.removeMessages(MSG_RETRY_END);
            mHandler.removeMessages(MSG_UPDATE_ICON);
            mHandler.removeMessages (MSG_SEEK_BY_BAR);
        }

        IWindowManager iWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        try {
            iWindowManager.setAnimationScale(1, mTransitionAnimationScale);
        }
        catch (RemoteException e) {
        }

        if(mResumePlay != null) {
            if(mContext != null) {
                boolean resumeEnable = mContext.getResources().getBoolean(R.bool.config_resume_play_enable); 
                LOGI(TAG,"[onPause] resumeEnable:"+resumeEnable);
                if(resumeEnable == true) {
                    mResumePlay.setEnable(true); //resume play function ON/OFF
                    if(true == mResumePlay.getEnable()) {
                        mResumePlay.set(mPlayList.getcur(), curtime);
                        LOGI(TAG,"[onPause]mStateBac:"+mState);
                        mStateBac = mState;
                        sendStopMsg();
                    }
                }
                else {
                    browserBackInvokeFromOnPause = true;
                    browserBack();
                }
            }
        }
    }

    //@@--------this part for message handle---------------------------------------------------------------------
    private static final long MSG_SEND_DELAY = 0; //1000;//1s
    private static final int MSG_UPDATE_PROGRESS = 0xF1;//random value
    private static final int MSG_PLAY = 0xF2;
    private static final int MSG_STOP = 0xF3;
    private static final int MSG_RETRY_PLAY = 0xF4;
    private static final int MSG_RETRY_END = 0xF5;
    private static final int MSG_SUB_OPTION_UPDATE = 0xF6;
    private static final int MSG_UPDATE_ICON = 0xF7;
    private static final int MSG_SEEK_BY_BAR = 0xF8;
    private static final int MSG_UPDATE_SUB = 0xF9;//random value
    private boolean ignoreUpdateProgressbar = false;
    private boolean mUpdateHadling = false;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int pos;
            //LOGI(TAG,"[handleMessage]msg:"+msg);
            switch (msg.what) {
                case MSG_UPDATE_PROGRESS:
                    //LOGI(TAG,"[handleMessage]MSG_UPDATE_PROGRESS mState:"+mState+",mSeekState:"+mSeekState);
                    /*if(mSurfaceView != null) { 
                        int left = 0;
                        int top = 0;
                        int right = 0;
                        int bottom = 0;
                        left = mSurfaceView.getLeft();
                        top = mSurfaceView.getTop();
                        right = mSurfaceView.getRight();
                        bottom = mSurfaceView.getBottom();
                        LOGI(TAG,"[handleMessage]MSG_UPDATE_PROGRESS left:"+left+",top:"+top+",right:"+right+",bottom:"+bottom);
                    } */
                    if((mState == STATE_PLAYING 
                        || mState == STATE_PAUSED
                        || mState == STATE_SEARCHING)  && (mSeekState == SEEK_END) /*&& osdisshowing*/ && !ignoreUpdateProgressbar) {
                        if(!mUpdateHadling) {
                            mUpdateHadling = true;
                            pos = getCurrentPosition();
                            updateProgressbar();
                            mUpdateHadling = false;
                            removeMessages(MSG_UPDATE_PROGRESS);
                            msg = obtainMessage(MSG_UPDATE_PROGRESS);
                            sendMessageDelayed(msg, 500 - (pos % 500));
                        }
                    }
                    break;
                case MSG_PLAY:
                    LOGI(TAG,"[handleMessage]resume mode:"+mOption.getResumeMode()+",mPath:"+mPath);
                    if(mOption != null && mPath != null) {
                        resetVariate();
                        showOsdView();
                        if(mResumePlay.getEnable() == true) {
                            setVideoPath(mPath);
                        }
                        else {
                            if(mOption.getResumeMode() == true) {
                                bmPlay(mPath);
                            }
                            else {
                                setVideoPath(mPath);
                            }
                        }
                    }
                    break;
                case MSG_STOP:
                    stopShowCpuloading();
                    stop();
                    break;
                case MSG_RETRY_PLAY:
                    LOGI(TAG,"[handleMessage]MSG_RETRY_PLAY");
                    String path = mResumePlay.getFilepath();
                    if(new File(path).exists()) {
                        if(surfaceDestroyedFlag) { //add for press power key quickly 
                            initVideoView();
                        }
                        else {
                            //browserBack();
                            initPlayer();
                            mPath = path;
                            sendPlayFileMsg();
                        }
                    }
                    else {
                        LOGI(TAG,"retry fail, retry again.");
                        retryPlay();
                    }
                    break;
                case MSG_RETRY_END:
                    LOGI(TAG,"[handleMessage]MSG_RETRY_END");
                    if(mContext != null)
                        Toast.makeText(mContext,mContext.getText(R.string.str_no_file),Toast.LENGTH_SHORT).show();  
                    browserBack();
                    break;
                case MSG_SUB_OPTION_UPDATE:
                    if (isShowImgSubtitle) {
                        disableSubSetOptions();
                    }
                    else {
                        initSubSetOptions(mcolor_text);
                    }
                    break;
                case MSG_UPDATE_ICON:
                    updateIconResource();
                    break;
                case MSG_SEEK_BY_BAR:
                    seekByProgressBar();
                    break;
		 case MSG_UPDATE_SUB:
		    mMediaPlayer.subtitleHide();
		    break;
            }
        }
    };

    private void updateProgressbar() {
        if((mState >= STATE_PREPARED) && (mState != STATE_PLAY_COMPLETED) && (mState <= STATE_SEARCHING)) { //avoid error (-38, 0), caused by getDuration before prepared
            curtime = getCurrentPosition();
            totaltime = getDuration();

            if (!seekBarEnableMsgSended) {
                seekBarEnableMsgSended = true;
                sendUpdateIconMsg();
            }

            // add for seeking to head
            if(curtime <= 1000) { //current time is equal or smaller than 1S stop fw/fb
                stopFWFB();
                mState = STATE_PLAYING;
                if (mMediaPlayer != null && mMediaPlayer.isPlaying() == false) {
                    mState = STATE_PAUSED;
                }
                updateIconResource();
            }

            // update msg show for current time
            msgTimeUpdate();

            //LOGI(TAG,"[updateProgressbar]curtime:"+curtime+",totaltime:"+totaltime);
            if(curTimeTx!=null && totalTimeTx!=null && progressBar!=null) {
                int flag = getCurOsdViewFlag();
                if((OSD_CTL_BAR == flag) &&  (null!=ctlbar)&&(View.VISIBLE==ctlbar.getVisibility())) { // check control bar is showing
                    curTimeTx.setText(secToTime(curtime/1000));
                    totalTimeTx.setText(secToTime(totaltime/1000));
                    seekPosBac = curtime/100;
                    if(totaltime != 0) {
                        int curtimetmp = curtime / 1000;
                        int totaltimetmp = totaltime / 1000;
                        if(totaltimetmp != 0) {
                            int step = curtimetmp*100/totaltimetmp;
                            progressBar.setProgress(step/*curtime*100/totaltime*/);
                        }
                    }
                    else {
                        progressBar.setProgress(0);
                    }
                }
            }
        }
    }

    private boolean isTimedTextDisable() {
        boolean ret = sw.getPropertyBoolean("sys.timedtext.disable", true);
        return ret;
    }
    private boolean getImgSubRatioEnable() {
        boolean ret = sw.getPropertyBoolean("sys.imgsubratio.enable", true);
        return ret;
    }

    private boolean getDebugEnable() {
        if(sw == null) {
            sw = (SystemWriteManager) getSystemService("system_write"); 
        }
        boolean ret = sw.getPropertyBoolean("sys.videoplayer.debug",false);
        return ret;
    }

    private int getSubtitleStyle() {
        if(sw == null) {
            sw = (SystemWriteManager) getSystemService("system_write"); 
        }
        int ret = sw.getPropertyInt("sys.subtitle.style", 0);
        return ret;
    }

    private void LOGI(String tag, String msg) {
         if(DEBUG || getDebugEnable()) Log.i(tag, msg);
    }

    private void LOGD(String tag, String msg) {
         if(DEBUG || getDebugEnable()) Log.d(tag, msg);
    }

    private void LOGE(String tag, String msg) {
         /*if(DEBUG)*/ Log.e(tag, msg);
    }

    protected void closeScreenOffTimeout() {
    	if(mScreenLock.isHeld() == false)
    		mScreenLock.acquire();
    }
    
    protected void openScreenOffTimeout() {
    	if(mScreenLock.isHeld() == true)
    		mScreenLock.release();
    }

    private void init() {
        initView();
        initOption();
        initBookmark();
        initResumePlay();
        initPlayList();
        initErrorInfo();
    }

    private void initOption() {
        mOption = new Option(VideoPlayer.this);
    }

    private void initBookmark() {
        mBookmark = new Bookmark(VideoPlayer.this);
    }

    private void initResumePlay() {
        mResumePlay = new ResumePlay(VideoPlayer.this);
    }

    private void initPlayList() {
        mPlayList = PlayList.getinstance();
    }

    private void initMediaInfo() {
        mMediaInfo = new MediaInfo(mMediaPlayer, VideoPlayer.this);
        mMediaInfo.initMediaInfo();

        //prepare for audio track
        if(mMediaInfo != null) {
            int audio_init_list_idx = mMediaInfo.getCurAudioIdx();
            int audio_total_num = mMediaInfo.getAudioTotalNum();
            if(audio_init_list_idx >= audio_total_num) {
                audio_init_list_idx = audio_total_num -1;
            }
            Locale loc = Locale.getDefault();
            if (loc != null && mIsBluray) {
                int index = getLanguageIndex(MediaInfo.BLURAY_STREAM_TYPE_AUDIO, loc.getISO3Language());
                if (index >= 0)
                    audio_init_list_idx = index;
            }
            mOption.setAudioTrack(audio_init_list_idx);
            mOption.setAudioDtsAsset(0); //dts test // default 0, should get current asset from player core 20140717

            int video_init_list_idx = mMediaInfo.getCurVideoIdx();
            int video_total_num = mMediaInfo.getVideoTotalNum();
            if (video_init_list_idx >= video_total_num) {
                video_init_list_idx = video_total_num -1;
            }
            if (video_init_list_idx <= mMediaInfo.getTsTotalNum()) {
                mOption.setVideoTrack(video_init_list_idx);
            }
        }
    }

    private void initErrorInfo() {
        mErrorInfo = new ErrorInfo(VideoPlayer.this);
    }
    
    private void initView() {
        LOGI(TAG,"initView");
        mContext = this.getApplicationContext();
        initVideoView();

        channel_display =Toast.makeText(VideoPlayer.this, "",Toast.LENGTH_SHORT );
        channel_display.setGravity(Gravity.TOP | Gravity.RIGHT,10,10);
        channel_display.setDuration(0x00000001);
		
        ff_fb =Toast.makeText(VideoPlayer.this, "",Toast.LENGTH_SHORT );
        ff_fb.setGravity(Gravity.TOP | Gravity.RIGHT,10,10);
        ff_fb.setDuration(0x00000001);

        //subtitle
        subtitleTV = (TextView)findViewById(R.id.SubtitleTV);
        subtitleIV = (ImageView)findViewById(R.id.SubtitleIV);
        subtitleShow();

        //msg showing
        msgwidget = (RelativeLayout)findViewById(R.id.MsgView);
		msgwidget.setVisibility(View.GONE);
        msgName = (TextView)findViewById(R.id.msgname);
        msgResolution = (TextView)findViewById(R.id.msgresolution);
        msgTime = (TextView)findViewById(R.id.msgtime);
        msgCpuloading = (TextView)findViewById(R.id.msgcpuloading);
        msgType = (TextView)findViewById(R.id.msgtype);
        msgBitrate = (TextView)findViewById(R.id.msgbitrate);
        msgFps = (TextView)findViewById(R.id.msgfps);
        msgVFormat = (TextView)findViewById(R.id.msgvformat);
        msgAFormat = (TextView)findViewById(R.id.msgaformat);
        msgSFormat = (TextView)findViewById(R.id.msgsformat);

        //certification image
        certificationDoblyView = (ImageView)findViewById(R.id.CertificationDobly);
        certificationDoblyPlusView = (ImageView)findViewById(R.id.CertificationDoblyPlus);
        certificationDTSView = (ImageView)findViewById(R.id.CertificationDTS);
        certificationDTSExpressView = (ImageView)findViewById(R.id.CertificationDTSExpress);
        certificationDTSHDMasterAudioView = (ImageView)findViewById(R.id.CertificationDTSHDMasterAudio);
        certificationDoblyView.setVisibility(View.GONE);
        certificationDoblyPlusView.setVisibility(View.GONE);
        certificationDTSView.setVisibility(View.GONE);
        certificationDTSExpressView.setVisibility(View.GONE);
        certificationDTSHDMasterAudioView.setVisibility(View.GONE);

        ctlbar = (LinearLayout)findViewById(R.id.infobarLayout);
        optbar = (LinearLayout)findViewById(R.id.morebarLayout);
        subwidget = (LinearLayout)findViewById(R.id.LinearLayout_sub);
        otherwidget = (LinearLayout)findViewById(R.id.LinearLayout_other);
        infowidget = (LinearLayout)findViewById(R.id.dialog_layout);
        ctlbar.setVisibility(View.GONE);
        optbar.setVisibility(View.GONE);
        subwidget.setVisibility(View.GONE);
        otherwidget.setVisibility(View.GONE);
        infowidget.setVisibility(View.GONE);

        //layer 1
        progressBar = (SeekBar)findViewById(R.id.SeekBar);
        curTimeTx = (TextView)findViewById(R.id.CurTime);
        totalTimeTx = (TextView)findViewById(R.id.TotalTime);
        curTimeTx.setText(secToTime(curtime));
        totalTimeTx.setText(secToTime(totaltime));
        browserBtn = (ImageButton)findViewById(R.id.BrowserBtn);
        preBtn = (ImageButton)findViewById(R.id.PreBtn);
        fastforwordBtn = (ImageButton)findViewById(R.id.FastForwardBtn);
        playBtn = (ImageButton)findViewById(R.id.PlayBtn);
        fastreverseBtn = (ImageButton)findViewById(R.id.FastReverseBtn);
        nextBtn = (ImageButton)findViewById(R.id.NextBtn);
        optBtn = (ImageButton)findViewById(R.id.moreBtn);

        //layer 2
        ctlBtn = (ImageButton) findViewById(R.id.BackBtn);
        resumeModeBtn = (ImageButton) findViewById(R.id.ResumeBtn);
        repeatModeBtn = (ImageButton) findViewById(R.id.PlaymodeBtn);
        audiooptionBtn = (ImageButton) findViewById(R.id.ChangetrackBtn);
        subtitleSwitchBtn = (ImageButton) findViewById(R.id.SubtitleBtn);
        chapterBtn = (ImageButton) findViewById (R.id.ChapterBtn);
        displayModeBtn = (ImageButton) findViewById(R.id.DisplayBtn);
        brigtnessBtn = (ImageButton) findViewById(R.id.BrightnessBtn);
        fileinfoBtn = (ImageButton) findViewById(R.id.InfoBtn);
        play3dBtn = (ImageButton) findViewById(R.id.Play3DBtn);
        otherwidgetTitleTx = (TextView)findViewById(R.id.more_title);

        String proj_type = sw.getPropertyString("sys.proj.type",null);
        if ("telecom".equals(proj_type)) {
            displayModeBtn.setVisibility(View.GONE);
        }

        //layer 1
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
                progressBarSeekFlag = false; 
                progressBar.requestFocusFromTouch();
                startOsdTimeout();
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                progressBarSeekFlag = true; 
            }

            public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser) {
                if(totaltime == -1)
                    return;
                if(fromUser == true){
                    int curtime = totaltime * (progress) / 100;
                    if(curTimeTx!=null && progressBar!=null) {
                        int flag = getCurOsdViewFlag();
                        if((OSD_CTL_BAR == flag) && (null != ctlbar) && (View.VISIBLE==ctlbar.getVisibility())) { // check control bar is showing
                            curTimeTx.setText(secToTime(curtime/1000));
                        }
                    }
                    ignoreUpdateProgressbar = true;
                    startOsdTimeout();
                    mHandler.removeMessages (MSG_SEEK_BY_BAR);
                    progressBarSeekFlag = true; 
                    sendSeekByProgressBarMsg();
                }
            }
        });
        
        browserBtn.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {
                LOGI(TAG,"browserBtn onClick");
                browserBack();
            }
        });

        preBtn.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v) {
                LOGI(TAG,"preBtn onClick");
                playPrev();
            }
        });

        fastforwordBtn.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v) {
                LOGI(TAG,"fastforwordBtn onClick");
                if(mCanSeek) 
                    fastForward();
            }
        });

        playBtn.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v) {
                LOGI(TAG,"playBtn onClick");
                playPause();
            }
        });

        fastreverseBtn.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v) {
                LOGI(TAG,"fastreverseBtn onClick");
                if(mCanSeek) 
                    fastBackward();
            }
        });

        nextBtn.setOnClickListener(new Button.OnClickListener(){
            public void onClick(View v) {
                LOGI(TAG,"nextBtn onClick");
                playNext();
            }
        });

        optBtn.setOnClickListener(new ImageButton.OnClickListener() {
            public void onClick(View v) {
                LOGI(TAG,"optBtn onClick");
                switchOsdView();
            }
        });

        //layer 2
        ctlBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LOGI(TAG,"ctlBtn onClick");
                switchOsdView();
            } 
        }); 

        resumeModeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LOGI(TAG,"resumeModeBtn onClick");
                resumeSelect();
            } 
        });

        if(playmode_switch) {
            repeatModeBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    LOGI(TAG,"repeatModeBtn onClick");
                    repeatSelect();
                }
            });
        }
        else {
            repeatModeBtn.setImageDrawable(getResources().getDrawable(R.drawable.mode_disable));
        }

        audiooptionBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LOGI(TAG,"audiooptionBtn onClick");
                audioOption();
            } 
        });

        subtitleSwitchBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LOGI(TAG,"subtitleSwitchBtn onClick");
                subtitleSelect();
            } 
        });
        chapterBtn.setOnClickListener (new View.OnClickListener() {
            public void onClick (View v) {
                LOGI (TAG, "chapterBtn onClick");
                chapterSelect();
            }
        });
        displayModeBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LOGI(TAG,"displayModeBtn onClick");
                displayModeSelect();
            } 
        });

        brigtnessBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LOGI(TAG,"brigtnessBtn onClick");
                brightnessSelect();
            } 
        }); 

        ImageButton timeSet = (ImageButton) findViewById(R.id.TimeBtn);
        timeSet.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                optbar.setVisibility(View.GONE);
                stopOsdTimeout();
                Log.d("get total time:","total time is:"+totaltime);
                timeSetDailog();
            } 
        });

        fileinfoBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LOGI(TAG,"fileinfoBtn onClick");
                if(randomSeekEnable()) { // random seek, shield for default
                    randomSeekTest();
                }
                else {
                    fileinfoShow();
                }
            } 
        }); 

        play3dBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LOGI(TAG,"play3dBtn onClick");
                /*Toast toast =Toast.makeText(VideoPlayer.this, "this function is not opened right now",Toast.LENGTH_SHORT );
                toast.setGravity(Gravity.BOTTOM,0,0);
                toast.setDuration(0x00000001);
                toast.show();
                startOsdTimeout();*/
                play3DSelect();
            } 
        }); 
        
    }

    private void initVideoView() {
        LOGI(TAG,"[initVideoView]");
        mSurfaceViewRoot = findViewById(R.id.SurfaceViewRoot);
        mSurfaceView = (SurfaceView) mSurfaceViewRoot.findViewById(R.id.SurfaceView);
        setOnSystemUiVisibilityChangeListener(); // TODO:ATTENTION: this is very import to keep osd bar show or hide synchronize with touch event, bug86905
        //showSystemUi(false);
        //getHolder().setFormat(PixelFormat.VIDEO_HOLE_REAL);
        if(mSurfaceView != null) {
            mSurfaceView.getHolder().addCallback(mSHCallback);
            mSurfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            //@@mSurfaceView.addOnLayoutChangeListener(this);
            mSurfaceView.setFocusable(true);
            mSurfaceView.setFocusableInTouchMode(true);
            mSurfaceView.requestFocus();
            //@@mVideoWidth = 0;
            //@@mVideoHeight = 0;
            //@@mCurrentState = STATE_IDLE;
            //@@mTargetState  = STATE_IDLE;
        }
    }

    private int getCurDirFile(Uri uri, List<String> list){
        LOGI(TAG,"[getCurDirFile]uri:"+uri);
        String path = uri.getPath(); 
        int pos=-1;
        list.clear();

        if(null!=path) {
            String dir=null;
            int index=-1;
            index=path.lastIndexOf("/");
            if(index>=0) {
                dir=path.substring(0,index);
            }

            File dirFile = new File(dir);

            if (dirFile.exists() && dirFile.isDirectory() && dirFile.listFiles() != null && dirFile.listFiles().length > 0) {
                for (File file : dirFile.listFiles()) {
                    String pathFull = file.getAbsolutePath();
                    String name = (new File(pathFull)).getName();
                    String ext = name.substring(name.lastIndexOf(".")+1,name.length()).toLowerCase();

                    if(ext.equals("rm") || ext.equals("rmvb") || ext.equals("avi")|| ext.equals("mkv") || 
                        ext.equals("mp4")|| ext.equals("wmv") || ext.equals("mov")|| ext.equals("flv") ||
                        ext.equals("asf")|| ext.equals("3gp")|| ext.equals("mpg") || ext.equals("mvc")||
                        ext.equals("m2ts")|| ext.equals("ts")|| ext.equals("swf") || ext.equals("mlv") ||
                        ext.equals("divx")|| ext.equals("3gp2")|| ext.equals("3gpp") || ext.equals("h265") ||
                        ext.equals("m4v")|| ext.equals("mts")|| ext.equals("tp") || ext.equals("bit") || 
                        ext.equals("webm")|| ext.equals("3g2")|| ext.equals("f4v") || ext.equals("pmp") ||
                        ext.equals("mpeg") || ext.equals("vob") || ext.equals("dat") || ext.equals("m2v") ||
                        ext.equals("iso") || ext.equals("vp9") || ext.equals("trp") ||ext.equals("hm10")) {
                        list.add(pathFull); 
                    }
                }
            } 

            for(int i=0;i<list.size();i++) {
                String tempP = list.get(i);
                if(tempP.equals(path)) {
                    pos = i;
                }
            }
        }
        return pos;
    }

    private int checkUri() {
        // TODO: should check mUri=null
        LOGI(TAG,"[checkUri]");
        Intent it = getIntent();
        mUri = it.getData();
        LOGI(TAG,"[checkUri]mUri:"+mUri);
        if(it.getData() != null) {
            if(it.getData().getScheme() != null && it.getData().getScheme().equals("file")) {
                List<String> paths = new ArrayList<String>();
                int pos = getCurDirFile(mUri, paths);
                //paths.add(it.getData().getPath());
                if(pos != -1){
                    mPlayList.setlist(paths, pos);
                    mPlayList.rootPath = null;
                    backToOtherAPK = true;
                }
                else
                    return -1;
            }
            else if(it.getData().getScheme() != null && it.getData().getScheme().equals("http")) {
                Log.d(TAG, "getScheme:"+it.getData().getScheme());
                List<String> paths = new ArrayList<String>();
                paths.add(it.getData().toString());
                Log.d(TAG, "path:"+it.getData().toString());
                mPlayList.setlist(paths, 0);
                mPlayList.rootPath = null;
                backToOtherAPK = true;
            }
            else {
                Cursor cursor = managedQuery(it.getData(), null, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    int index = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
                    if((index == -1) || (cursor.getCount() <= 0)) {
                        LOGE(TAG, "Cursor empty or failed\n"); 
                    }
                    else {
                        List<String> paths = new ArrayList<String>();
                        cursor.moveToFirst();
                        paths.add(cursor.getString(index));
                        mPlayList.setlist(paths, 0);
                    }
                } 
                else {
                    // unsupported mUri, exit directly
                    android.os.Process.killProcess(android.os.Process.myPid());
                    return -1;
                }
            }
        }
        return 0;
    }

    private void storeFilePos() {
        Bundle bundle = new Bundle();
        try{
            bundle = VideoPlayer.this.getIntent().getExtras();
            if (bundle != null) {
                item_position_selected = bundle.getInt("item_position_selected");
                item_position_first = bundle.getInt("item_position_first");
                fromtop_piexl = bundle.getInt("fromtop_piexl");
                fileDirectory_position_selected = bundle.getIntegerArrayList("fileDirectory_position_selected");
                fileDirectory_position_piexl = bundle.getIntegerArrayList("fileDirectory_position_piexl");
                backToOtherAPK = bundle.getBoolean("backToOtherAPK", true);			
                if(item_init_flag) {
                    item_position_selected_init = item_position_selected - mPlayList.getindex();
                    item_init_flag = false;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bundle getFilePos() {
        Bundle bundle = new Bundle();
        try{
            if (bundle != null) {
                bundle.putInt("item_position_selected", item_position_selected);
                bundle.putInt("item_position_first", item_position_first);
                bundle.putInt("fromtop_piexl", fromtop_piexl);
                bundle.putIntegerArrayList("fileDirectory_position_selected", fileDirectory_position_selected);
                bundle.putIntegerArrayList("fileDirectory_position_piexl", fileDirectory_position_piexl);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return bundle;
    }

    private String secToTime(int i) {
        String retStr = null;
        int hour = 0;
        int minute = 0;
        int second = 0;
        if(i <= 0) {
            return "00:00:00";
        }
        else {
            minute = i/60;
            if(minute < 60) {
                second = i%60;
                retStr = "00:" + unitFormat(minute) + ":" + unitFormat(second);
            }
            else {
                hour = minute/60;
                if (hour > 99)
                    return "99:59:59";
                minute = minute%60;
                second = i%60;
                retStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
            }
        }
        return retStr;
    }

    private String unitFormat(int i) {
        String retStr = null;
        if(i >= 0 && i < 10)
            retStr = "0" + Integer.toString(i);
        else
            retStr = Integer.toString(i);
        return retStr;
    }

    SurfaceHolder.Callback mSHCallback = new SurfaceHolder.Callback() {
        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            LOGI(TAG,"[surfaceChanged]format:"+format+",w:"+w+",h:"+h);
                if (mSurfaceView != null && mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                displayModeImpl(); 
                //mSurfaceView.requestLayout();
                //mSurfaceView.invalidate();
            }
            //@@
            /*mSurfaceWidth = w;
            mSurfaceHeight = h;
            boolean isValidState =  (mTargetState == STATE_PLAYING);
            boolean hasValidSize = (mVideoWidth == w && mVideoHeight == h);
            if (mMediaPlayer != null && isValidState && hasValidSize) {
                if (mSeekWhenPrepared != 0) {
                    seekTo(mSeekWhenPrepared);
                }
                start();
            }*/
        }

        public void surfaceCreated(SurfaceHolder holder) {
            LOGI(TAG,"[surfaceCreated]");
            mSurfaceHolder = holder;
            surfaceDestroyedFlag = false;
            initPlayer();
            LOGI(TAG,"[surfaceCreated]mResumePlay:"+mResumePlay+",surfaceDestroyedFlag:"+surfaceDestroyedFlag);
            if(mResumePlay != null) {
                LOGI(TAG,"[surfaceCreated]mResumePlay.getEnable():"+mResumePlay.getEnable());
                if(mResumePlay.getEnable() == true) {
                    LOGI(TAG,"[surfaceCreated] mResumePlay.getFilepath():"+mResumePlay.getFilepath());
                    String path = mResumePlay.getFilepath();

                    if(mPlayList != null) {
                        if(mPlayList.getindex()<0) {
                            List<String> paths = new ArrayList<String>();
                            Uri uri = Uri.parse(path);
                            if(uri != null) {
                                int pos = getCurDirFile(uri, paths);
                                if(pos != -1) {
                                    mPlayList.setlist(paths, pos);
                                }
                            }
                        }
                        path = mPlayList.getcur();
                        LOGI(TAG,"[surfaceCreated]mResumePlay prepare path:"+path);
                        if(path != null) {
                            mPath = path;
                            sendPlayFileMsg();
                        }
                        else {
                            browserBack();
                        }
                    }
                    else {
                        browserBack(); // mPlayList is null, resume play function error, and then back to file list
                    }
                }
                else {
                    LOGI(TAG,"[surfaceCreated]0path:"+mPlayList.getcur());
                    mPath = mPlayList.getcur();
                    sendPlayFileMsg();
                }
            }
            else {
                LOGI(TAG,"[surfaceCreated]1path:"+mPlayList.getcur());
                mPath = mPlayList.getcur();
                sendPlayFileMsg();
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            LOGI(TAG,"[surfaceDestroyed]");
            mSurfaceHolder = null;
            mSurfaceView = null;
            release();
            surfaceDestroyedFlag = true;
            LOGI(TAG,"[surfaceDestroyed]surfaceDestroyedFlag:"+surfaceDestroyedFlag);
        }
    };
    
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        if(mSurfaceView != null) {
            displayModeImpl(); 
            mSurfaceView.requestLayout();
            mSurfaceView.invalidate();
        }
    }
    //@@--------this part for broadcast receiver-------------------------------------------------------------------------------------
    private final String POWER_KEY_SUSPEND_ACTION = "com.amlogic.vplayer.powerkey";
    private boolean isEjectOrUnmoutProcessed = false;
    private boolean isHdmiPluggedbac = false;
    private boolean isHdmiPlugged = false;
    private boolean needPause = true;

    private BroadcastReceiver mHdmiReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (needPause){
                needPause = intent.getBooleanExtra("videoplayer.need.pause", false); 
            }
            LOGI(TAG,"[mHdmiReceiver]onReceive---------");
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    displayModeImpl();
                }

            if (!needPause)
                return;
            isHdmiPlugged = intent.getBooleanExtra(WindowManagerPolicy.EXTRA_HDMI_PLUGGED_STATE, false); 
            if((isHdmiPluggedbac != isHdmiPlugged) && (isHdmiPlugged == false)) {
                 if(mState == STATE_PLAYING) {
                    pause();

                    //close 3D
                    close3D();
                }
                startOsdTimeout();
            }
            isHdmiPluggedbac = isHdmiPlugged;
        }
    };

    private BroadcastReceiver mMountReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Uri uri = intent.getData();
            String path = uri.getPath();  
            
            LOGI(TAG, "[mMountReceiver] action=" + action + ",uri=" + uri + ",path=" + path +", mRetrying:"+mRetrying);
            if (action == null ||path == null) {
                return;
            }

            if(mRetrying == true) {
                return;
            }

            if ((action.equals(Intent.ACTION_MEDIA_EJECT)) ||(action.equals(Intent.ACTION_MEDIA_UNMOUNTED))) {
                if(mPlayList.getcur()!=null) {
                    if(mPlayList.getcur().startsWith(path)) {
                        if(isEjectOrUnmoutProcessed)
                            return;
                        else
                            isEjectOrUnmoutProcessed = true;
                        browserBack();
                    }
                }				
            } 
            else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {  
                isEjectOrUnmoutProcessed = false;
                // Nothing				
            } 
        }
    };

    private BroadcastReceiver mPowerReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction(); 
                if (action == null)
                    return;
                if(action.equals(POWER_KEY_SUSPEND_ACTION)) {
                if(mResumePlay != null) {
                    mResumePlay.setEnable(true);
                }
            }
        }
    };
    
    private void registerHdmiReceiver() {
        IntentFilter intentFilter = new IntentFilter(WindowManagerPolicy.ACTION_HDMI_PLUGGED);
        intentFilter.addAction(WindowManagerPolicy.ACTION_HDMI_HW_PLUGGED);
        Intent intent = registerReceiver(mHdmiReceiver, intentFilter);
        if (intent != null) {
            mHdmiPlugged = intent.getBooleanExtra(WindowManagerPolicy.EXTRA_HDMI_PLUGGED_STATE, false);
        } 
        LOGI(TAG,"[registerHdmiReceiver]mHdmiReceiver:"+mHdmiReceiver);
    }
    
    private void registerMountReceiver() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        intentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        intentFilter.addDataScheme("file");
        registerReceiver(mMountReceiver, intentFilter);
        LOGI(TAG,"[registerMountReceiver]mMountReceiver:"+mMountReceiver);
    }

    private void registerPowerReceiver() {
           IntentFilter intentFilter = new IntentFilter(POWER_KEY_SUSPEND_ACTION);
           registerReceiver(mPowerReceiver, intentFilter);
           LOGI(TAG,"[registerPowerReceiver]mPowerReceiver:"+mPowerReceiver);
       }

    private void unregisterHdmiReceiver() {
        LOGI(TAG,"[unregisterHdmiReceiver]mHdmiReceiver:"+mHdmiReceiver);
        if(mHdmiReceiver != null) {
            unregisterReceiver(mHdmiReceiver);
            mHdmiReceiver = null;
        }
    }

    private void unregisterMountReceiver() {
        LOGI(TAG,"[unregisterMountReceiver]mMountReceiver:"+mMountReceiver);
        if(mMountReceiver != null) {
            unregisterReceiver(mMountReceiver);
            isEjectOrUnmoutProcessed = false;
            mMountReceiver = null;
        }
    }

    private void unregisterPowerReceiver() {
        LOGI(TAG,"[unregisterPowerReceiver]mPowerReceiver:"+mPowerReceiver);
        if(mPowerReceiver != null) {
            unregisterReceiver(mPowerReceiver);
            mPowerReceiver = null;
        }
    }

    //@@--------this part for option function implement------------------------------------------------------------------------------
    private final int apresentationMax = 32;
    private int[] assetsArrayNum = new int[apresentationMax];
    private int mApresentIdx = -1;
        private static final String DISPLAY_MODE_SYSFS = "/sys/class/display/mode";
    private void resumeSelect() {
        LOGI(TAG,"[resumeSelect]");
        ListView listView = (ListView)findViewById(R.id.ListView);
        listView.setAdapter(getMorebarListAdapter(RESUME_MODE, mOption.getResumeMode() ? 0 : 1));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0)
                    mOption.setResumeMode(true);
                else if (position == 1)
                    mOption.setResumeMode(false);
                exitOtherWidget(resumeModeBtn);
            }
        });
        showOtherWidget(R.string.setting_resume);
    }

    private void repeatSelect() {
        LOGI(TAG,"[repeatSelect]");
        ListView listView = (ListView)findViewById(R.id.ListView);
        listView.setAdapter(getMorebarListAdapter(REPEAT_MODE, mOption.getRepeatMode()));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {	
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0)
                    mOption.setRepeatMode(0);
                else if (position == 1)
                    mOption.setRepeatMode(1);
                exitOtherWidget(repeatModeBtn);
            }
        });
        showOtherWidget(R.string.setting_playmode);
    }

    private void audioOption() {
        LOGI(TAG,"[audioOption] mMediaInfo:"+mMediaInfo);
        if(mMediaInfo != null) {
            LOGI(TAG,"[audioOption] mMediaInfo.getAudioTotalNum():"+mMediaInfo.getAudioTotalNum());
            if(/*(audio_flag == Errorno.PLAYER_NO_AUDIO) || */(mMediaInfo.getAudioTotalNum() <= 0 ) ) {
                Toast toast =Toast.makeText(VideoPlayer.this, R.string.file_have_no_audio,Toast.LENGTH_SHORT );
                toast.setGravity(Gravity.BOTTOM,/*110*/0,0);
                toast.setDuration(0x00000001);
                toast.show();
                startOsdTimeout();
                return;
            }
        }

        if(mState == STATE_SEARCHING) {
            Toast toast_track_switch =Toast.makeText(VideoPlayer.this, R.string.cannot_switch_track,Toast.LENGTH_SHORT );
            toast_track_switch.show();
            return;
        }

        SimpleAdapter audiooptionarray = getMorebarListAdapter(AUDIO_OPTION, 0);
        ListView listView = (ListView)findViewById(R.id.ListView);
        listView.setAdapter(audiooptionarray);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {	
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    audiotrackSelect();
                }
                else if (position == 1) {
                    soundtrackSelect();
                }
                else if (position == 2) {
                    videotrackSelect();
                }
            }	
        });
        showOtherWidget(R.string.setting_audiooption);
    }

    private void audiotrackSelect() {
        LOGI(TAG,"[audiotrackSelect]");
        SimpleAdapter audioarray = null;
        if (mIsBluray)
            audioarray = getLeftAlignMorebarListAdapter(AUDIO_TRACK, mOption.getAudioTrack());
        else
            audioarray = getMorebarListAdapter(AUDIO_TRACK, mOption.getAudioTrack());
        ListView listView = (ListView)findViewById(R.id.ListView);
        listView.setAdapter(audioarray);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {	
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                boolean ret = mMediaInfo.checkAudioisDTS(mMediaInfo.getAudioFormat(position));
                if(ret) {
                    showDtsAseetFromInfoLis = false;
                    audioDtsApresentSelect();
                }
                else {
                    exitOtherWidget(audiooptionBtn);
                }
                mOption.setAudioTrack(position);
                audioTrackImpl(position);
                msgAFormatUpdate(); // update msg aformat
                mDtsType = DTS_NOR;
                showCertification(); //update certification status and icon
            }	
        });
        showOtherWidget(R.string.setting_audiotrack);
    }

    private void soundtrackSelect() {
        LOGI(TAG,"[soundtrackSelect]");
        SimpleAdapter soundarray = getMorebarListAdapter(SOUND_TRACK, mOption.getSoundTrack());
        ListView listView = (ListView)findViewById(R.id.ListView);
        listView.setAdapter(soundarray);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {	
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mOption.setSoundTrack(position);
                soundTrackImpl(position);
                exitOtherWidget(audiooptionBtn);
            }	
        });
        showOtherWidget(R.string.setting_soundtrack);
    }

    private void videotrackSelect() {
        LOGI(TAG,"[videotrackSelect]");
        SimpleAdapter videoarray = getMorebarListAdapter(VIDEO_TRACK, mOption.getVideoTrack());
        ListView listView = (ListView)findViewById(R.id.ListView);
        listView.setAdapter(videoarray);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mOption.setVideoTrack(position);
                videoTrackImpl(position);
                exitOtherWidget(audiooptionBtn);
            }
        });
        showOtherWidget(R.string.setting_videotrack);
    }

    private void audioDtsApresentSelect() {
        SimpleAdapter audiodtsApresentarray = getMorebarListAdapter(AUDIO_DTS_APRESENT, mOption.getAudioDtsApresent());
        ListView listView = (ListView)findViewById(R.id.ListView);
        listView.setAdapter(audiodtsApresentarray);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {	
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mApresentIdx = position;
                mOption.setAudioDtsApresent(position);
                audioDtsAseetSelect();
                ///audioDtsAseetImpl(position);
                ///exitOtherWidget(audiooptionBtn);
                ///showCertification(); //update certification status and icon
            }	
        });
        showOtherWidget(R.string.setting_audiodtsapresent);

        if(getDtsApresentTotalNum() == 0) { // dts test
            exitOtherWidget(audiooptionBtn);
        }

        if(showDtsAseetFromInfoLis) {
            startOsdTimeout();
        }
    }

    private void audioDtsAseetSelect() {
        SimpleAdapter audiodtsAssetarray = getMorebarListAdapter(AUDIO_DTS_ASSET, mOption.getAudioDtsAsset());
        ListView listView = (ListView)findViewById(R.id.ListView);
        listView.setAdapter(audiodtsAssetarray);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {	
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mOption.setAudioDtsAsset(position);
                audioDtsAseetImpl(mApresentIdx, position);
                exitOtherWidget(audiooptionBtn);
                showCertification(); //update certification status and icon
            }	
        });
        showOtherWidget(R.string.setting_audiodtsasset);

        if(getDtsApresentTotalNum() == 0) { // dts test
            exitOtherWidget(audiooptionBtn);
        }

        if(showDtsAseetFromInfoLis) {
            startOsdTimeout();
        }
    }

    private void play3DSelect() {
        LOGI(TAG,"[play3DSelect]");
        SimpleAdapter _3darray = getMorebarListAdapter(PLAY3D, mOption.get3DMode());
        ListView listView = (ListView)findViewById(R.id.ListView);
        listView.setAdapter(_3darray);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {	
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mOption.set3DMode(position);
                play3DImpl(position);
                //exitOtherWidget(play3dBtn);
                if ((null != otherwidget) && (View.VISIBLE == otherwidget.getVisibility())) {
                    otherwidget.setVisibility(View.GONE);
                    if ((null != ctlbar) && (View.GONE == ctlbar.getVisibility()))
                        ctlbar.setVisibility(View.VISIBLE);
                    play3dBtn.requestFocus();
                    play3dBtn.requestFocusFromTouch();
                    startOsdTimeout();
                }
            }	
        });
        showOtherWidget(R.string.setting_3d_mode);
    }

    private void subtitleSelect() {
        /*Toast toast =Toast.makeText(VideoPlayer.this, "this function is not opened right now",Toast.LENGTH_SHORT );
        toast.setGravity(Gravity.BOTTOM,110,0);
        toast.setDuration(0x00000001);
        toast.show();
        startOsdTimeout();
        return;*/
        
        subtitle_prepare();
        LOGI(TAG,"[subtitleSelect] sub_para.totalnum:"+sub_para.totalnum);
        if(sub_para.totalnum<=0) {
            Toast toast =Toast.makeText(VideoPlayer.this, R.string.sub_no_subtitle,Toast.LENGTH_SHORT );
            toast.setGravity(Gravity.BOTTOM,/*110*/0,0);
            toast.setDuration(0x00000001);
            toast.show();
            startOsdTimeout();
            return;
        }
        showSubWidget(R.string.setting_subtitle);
        subtitle_control();
    }

    private void setListViewHeight(int height) {
        ListView listView = (ListView) findViewById (R.id.ListView);
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        if (params instanceof LinearLayout.LayoutParams) {
            params.height = height;
            listView.setLayoutParams(params);
            mListViewHeight = height;
        }
    }

    private void chapterSelect() {
        LOGI (TAG, "[chapterSelect]");
        ListView listView = (ListView) findViewById (R.id.ListView);
        setListViewHeight((int)(getWindowManager().getDefaultDisplay().getHeight() * 0.4));
        listView.setAdapter (getLeftAlignMorebarListAdapter(CHAPTER_MODE, 0));
        listView.setOnItemClickListener (new AdapterView.OnItemClickListener() {
            public void onItemClick (AdapterView<?> parent, View view, int position, long id) {
                if (mMediaPlayer != null && mMediaInfo != null) {
                    seekTo(mBlurayChapter.get(position).start * 1000);
                }
                exitOtherWidget (chapterBtn);
            }
        });
        showOtherWidget (R.string.setting_chapter);
    }

    private void displayModeSelect() {
        LOGI(TAG,"[displayModeSelect]");
        // TODO: check 3D
        if(mMediaInfo != null) {
            int videoNum = mMediaInfo.getVideoTotalNum();
            if(videoNum <= 0) {
                Toast toast =Toast.makeText(VideoPlayer.this, R.string.file_have_no_video,Toast.LENGTH_SHORT );
                toast.setGravity(Gravity.BOTTOM,/*110*/0,0);
                toast.setDuration(0x00000001);
                toast.show();
                startOsdTimeout();
                return;
            }
        }
        ListView listView = (ListView)findViewById(R.id.ListView);
        if(sw == null) {
            sw = (SystemWriteManager) getSystemService("system_write"); 
        }
        String screen_mode = null;
        String proj_type = sw.getPropertyString("sys.proj.type",null);
        if ("unicom".equals(proj_type)) {
            int mode = 0;
            screen_mode = sw.getPropertyString("ubootenv.var.screenmode",null);//sw.readSysfs("/sys/class/video/screen_mode");
            if (screen_mode.contains("normal") && mOption.getDisplayMode() != 4) {
                mode  = 0;
            } else if (screen_mode.contains("full")) {
                mode = 1;
            } else if (screen_mode.contains("4_3")) {
                mode  = 2;
            } else if (screen_mode.contains("16_9")) {
                mode = 3;
            } else {
                mode = 4;
            }
            listView.setAdapter(getMorebarListAdapter(DISPLAY_MODE, mode));
        } else {
            listView.setAdapter(getMorebarListAdapter(DISPLAY_MODE, mOption.getDisplayMode()));
        }
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String proj_type = sw.getPropertyString("sys.proj.type",null);
                switch (position) {
                    case 0://mOption.DISP_MODE_NORMAL:
                        mOption.setDisplayMode(mOption.DISP_MODE_NORMAL);
                        if ("unicom".equals(proj_type))
                            sw.setProperty("ubootenv.var.screenmode" , "normal");
                        break;
                    case 1://mOption.DISP_MODE_FULLSTRETCH:
                        mOption.setDisplayMode(mOption.DISP_MODE_FULLSTRETCH);
                        if ("unicom".equals(proj_type))
                            sw.setProperty("ubootenv.var.screenmode" , "full");
                        break;
                    case 2://mOption.DISP_MODE_RATIO4_3:
                        mOption.setDisplayMode(mOption.DISP_MODE_RATIO4_3);
                        if ("unicom".equals(proj_type))
                            sw.setProperty("ubootenv.var.screenmode" , "4_3");
                        break;
                    case 3://mOption.DISP_MODE_RATIO16_9:
                        mOption.setDisplayMode(mOption.DISP_MODE_RATIO16_9);
                        if ("unicom".equals(proj_type))
                            sw.setProperty("ubootenv.var.screenmode" , "16_9");
                        break;
                    case 4://mOption.DISP_MODE_ORIGINAL
                        mOption.setDisplayMode (mOption.DISP_MODE_ORIGINAL);
                        if ("unicom".equals(proj_type))
                            sw.setProperty("ubootenv.var.screenmode" , "normal");
                        break;
                    default:
                        break;
                }
                displayModeImpl();
                exitOtherWidget(displayModeBtn);
            }
        });   
        showOtherWidget(R.string.setting_displaymode);
    }

    private void brightnessSelect() {
        LOGI(TAG,"[brightnessSelect]");
        ListView listView = (ListView)findViewById(R.id.ListView);
        int mBrightness = 0;
        try {
            mBrightness = Settings.System.getInt(VideoPlayer.this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } 
        catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
        int item;
        if (mBrightness <= (/*android.os.PowerManager.BRIGHTNESS_OFF*/20 + 10))
            item = 0;
        else if (mBrightness <= (android.os.PowerManager.BRIGHTNESS_ON * 0.2f))
            item = 1;
        else if (mBrightness <= (android.os.PowerManager.BRIGHTNESS_ON * 0.4f))
            item = 2;
        else if (mBrightness <= (android.os.PowerManager.BRIGHTNESS_ON * 0.6f))
            item = 3;
        else if (mBrightness <= (android.os.PowerManager.BRIGHTNESS_ON * 0.8f))
            item = 4;
        else
            item = 5;

        listView.setAdapter(getMorebarListAdapter(BRIGHTNESS, item));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int brightness;
                switch(position) {
                    case 0:
                        brightness = /*android.os.PowerManager.BRIGHTNESS_OFF*/20 + 10;
                    break;
                    case 1:
                        brightness = (int)(android.os.PowerManager.BRIGHTNESS_ON * 0.2f);
                    break;
                    case 2:
                        brightness = (int)(android.os.PowerManager.BRIGHTNESS_ON * 0.4f);
                    break;
                    case 3:
                        brightness = (int)(android.os.PowerManager.BRIGHTNESS_ON * 0.6f);
                    break;	 
                    case 4:
                        brightness = (int)(android.os.PowerManager.BRIGHTNESS_ON * 0.8f);
                    break;
                    case 5:
                        brightness = android.os.PowerManager.BRIGHTNESS_ON;
                    break;
                    default:
                        brightness = /*android.os.PowerManager.BRIGHTNESS_OFF*/20 + 30;
                    break;
                }
                try {
                    IPowerManager power = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
                    if (power != null) {
                        //power.setBacklightBrightness(brightness);
                        Settings.System.putInt(VideoPlayer.this.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightness);
                        power.setTemporaryScreenBrightnessSettingOverride(brightness);
                    }
                } 
                catch (RemoteException doe) {
                }  
                exitOtherWidget(brigtnessBtn);
            }
        });
        showOtherWidget(R.string.setting_brightness);
    }

    private void fileinfoShow() {
        LOGI(TAG,"[fileinfoShow]");
        showInfoWidget(R.string.str_file_name);
        String fileinf = null;
        TextView filename = (TextView)findViewById(R.id.filename);
        fileinf = VideoPlayer.this.getResources().getString(R.string.str_file_name)
            + "\t: " + mMediaInfo.getFileName(mPlayList.getcur());
        filename.setText(fileinf);

        TextView filetype = (TextView)findViewById(R.id.filetype);
        fileinf = VideoPlayer.this.getResources().getString(R.string.str_file_format)
            //+ "\t: " + mMediaInfo.getFileType();
            + "\t: " + mMediaInfo.getFileType(mPlayList.getcur());
        filetype.setText(fileinf);

        TextView filesize = (TextView)findViewById(R.id.filesize);
        fileinf = VideoPlayer.this.getResources().getString(R.string.str_file_size)
            + "\t: " + mMediaInfo.getFileSize();
        filesize.setText(fileinf);

        TextView resolution = (TextView)findViewById(R.id.resolution);
        fileinf = VideoPlayer.this.getResources().getString(R.string.str_file_resolution)
            + "\t: " + mMediaInfo.getResolution();
        resolution.setText(fileinf);

        TextView duration = (TextView)findViewById(R.id.duration);
        fileinf = VideoPlayer.this.getResources().getString(R.string.str_file_duration)
            + "\t: " + secToTime(mMediaInfo.getDuration());
        duration.setText(fileinf);

        Button ok = (Button)findViewById(R.id.info_ok);
        ok.setText("OK");
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                exitInfoWidget(fileinfoBtn);
            }
        });
    }

    private void displayModeImpl () {
        /*if (mMediaPlayer != null && mOption != null) {
            LOGI(TAG,"[displayModeImpl]mode:"+mOption.getDisplayMode());
            mMediaPlayer.setParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_FORCE_SCREEN_MODE,mOption.getDisplayMode());
        }*/
        int videoNum= -1;
        int videoWidth = -1;
        int videoHeight = -1;
        int dispWidth = -1;
        int dispHeight = -1;
        int frameWidth = -1;
        int frameHeight = -1;
        int width = -1;
        int height = -1;
        boolean skipImgSubRatio = false;
        MboxOutputModeManager mMboxOutputModeManager = (MboxOutputModeManager) getSystemService(Context.MBOX_OUTPUTMODE_SERVICE);
        String mode = sw.readSysfs(DISPLAY_MODE_SYSFS).replaceAll("\n","");
        int[] curPosition = mMboxOutputModeManager.getPosition(mode);
        dispWidth = curPosition[2];
        dispHeight = curPosition[3];
        DisplayMetrics dm = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        frameWidth = dm.widthPixels;
        frameHeight = dm.heightPixels;
        if (dispWidth == 0 || dispHeight == 0) {
            LOGI(TAG,"[displayModeImpl]Width or Height is zero, dispWidth:"+dispWidth+",dispHeight:"+dispHeight);
            dispWidth = frameWidth;
            dispHeight = frameHeight;
        }

        //skip image subtitle ratio set for display width and height, use framebuffer size to calculate video rect
        if (!getImgSubRatioEnable()) {
            dispWidth = frameWidth;
            dispHeight = frameHeight;
        }
        LOGI(TAG,"[displayModeImpl]dispWidth:"+dispWidth+",dispHeight:"+dispHeight);

        if(mMediaInfo != null) {
            videoNum = mMediaInfo.getVideoTotalNum();
            if(videoNum <= 0) {
                return;
            }
            //videoWidth = mMediaInfo.getVideoWidth();
            //videoHeight = mMediaInfo.getVideoHeight();
            //LOGI(TAG,"[displayModeImpl]videoWidth:"+videoWidth+",videoHeight:"+videoHeight);
        }

        if(mMediaPlayer != null && mOption != null && mSurfaceView != null) {
            LOGI(TAG,"[displayModeImpl]mode:"+mOption.getDisplayMode());
            //ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
            videoWidth = mMediaPlayer.getVideoWidth();
            videoHeight = mMediaPlayer.getVideoHeight();
            LOGI(TAG,"[displayModeImpl]videoWidth:"+videoWidth+",videoHeight:"+videoHeight);
            if(mOption.getDisplayMode() == 0) { // normal
                if ( videoWidth * dispHeight  < dispWidth * videoHeight ) {
                    //image too wide
                    width = dispHeight * videoWidth / videoHeight;
                    height = dispHeight;
                } else if ( videoWidth * dispHeight  > dispWidth * videoHeight ) {
                    //image too tall
                    width = dispWidth;
                    height = dispWidth * videoHeight / videoWidth;
                }
                else {
                    width = dispWidth;
                    height = dispHeight;
                }
            }
            else if(mOption.getDisplayMode() == 1) { // full screen
                width = dispWidth;
                height = dispHeight;
            }
            else if(mOption.getDisplayMode() == 2) { // 4:3
                videoWidth = 4*videoHeight /3;
                if ( videoWidth * dispHeight  < dispWidth * videoHeight ) {
                    //image too wide
                    width = dispHeight * videoWidth / videoHeight;
                    height = dispHeight;
                } else if ( videoWidth * dispHeight  > dispWidth * videoHeight ) {
                    //image too tall
                    width = dispWidth;
                    height = dispWidth * videoHeight / videoWidth;
                }
                else {
                    width = dispWidth;
                    height = dispHeight;
                }
            }
            else if(mOption.getDisplayMode() == 3) { // 16:9
               videoWidth = 16*videoHeight /9;
               if ( videoWidth * dispHeight  < dispWidth * videoHeight ) {
                    //image too wide
                    width = dispHeight * videoWidth / videoHeight;
                    height = dispHeight;
                } else if ( videoWidth * dispHeight  > dispWidth * videoHeight ) {
                    //image too tall
                    width = dispWidth;
                    height = dispWidth * videoHeight / videoWidth;
                }
                else {
                    width = dispWidth;
                    height = dispHeight;
                }
            }
                else if (mOption.getDisplayMode() == 4) { // original
                    videoWidth = mMediaInfo.getVideoWidth();
                    videoHeight = mMediaInfo.getVideoHeight();
                    float fbratio_div_outputratio = ((float)frameWidth / frameHeight) / ((float)dispWidth / dispHeight);
                    if (videoWidth * fbratio_div_outputratio * frameHeight > videoHeight * frameWidth) {
                        width = frameWidth;
                        height = (int)((float)(frameWidth * videoHeight) / ((float)videoWidth * fbratio_div_outputratio));
                    }
                    else {
                        width = (int)((float)(videoWidth * fbratio_div_outputratio * frameHeight) / (float)videoHeight);
                        height = frameHeight;
                    }
                    skipImgSubRatio = true;
                }
            LOGI(TAG,"[displayModeImpl]width:"+width+",height:"+height);
                if (getImgSubRatioEnable() && dispWidth != 0 && dispHeight != 0 && !skipImgSubRatio) {
                width = width * frameWidth / dispWidth;
                height = height * frameHeight / dispHeight;
                float ratioW = 1.000f;
                float ratioH = 1.000f;
                float ratioMax = 2.000f;
                float ratioMin = 1.250f;
                int maxW = dispWidth;
                int maxH = dispHeight;
                if (videoWidth != 0 & videoHeight != 0) {
                    ratioW = ((float)width) / videoWidth;
                    ratioH = ((float)height) / videoHeight;
                    if (ratioW > ratioMax || ratioH > ratioMax) {
                        ratioW = ratioMax;
                        ratioH = ratioMax;
                    }
                    else if (ratioW < ratioMin || ratioH < ratioMin) {
                        ratioW = ratioMin;
                        ratioH = ratioMin;
                    }

                    if(mMediaPlayer != null) {
                        mMediaPlayer.subtitleSetImgSubRatio(ratioW, ratioH, maxW, maxH);
                    }
                }
                LOGI(TAG,"[displayModeImpl]after change width:" + width + ",height:" + height + ", ratioW:" + ratioW + ",ratioH:" + ratioH +", maxW:" + maxW + ",maxH:" + maxH);
            }

            if(width > 0 && height > 0) {
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(width, height);
                lp.addRule(RelativeLayout.CENTER_IN_PARENT);
                mSurfaceViewRoot.setLayoutParams(lp); 
                //mSurfaceViewRoot.requestLayout();
            }
        }
    }

    private void audioTrackImpl(int idx) {
        if (mMediaPlayer != null && mMediaInfo != null) {
            LOGI(TAG,"[audioTrackImpl]idx:"+idx);

            int audioTrack = -1;
            if(mTrackInfo != null) {
                for(int i = 0; i < mTrackInfo.length; i++) {
                    int trackType = mTrackInfo[i].getTrackType();
                    if (trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
                        audioTrack ++;
                        if (audioTrack == idx) {
                            LOGI(TAG,"[audioTrackImpl]selectTrack track num:"+i);
                            mMediaPlayer.selectTrack(i);
                        }
                    }
                }
            }
            else {
                int id = mMediaInfo.getAudioIdx(idx);
                String str = Integer.toString(id);
                StringBuilder builder = new StringBuilder();
                builder.append("aid:"+str);
                LOGI(TAG,"[audioTrackImpl]"+builder.toString());
                mMediaPlayer.setParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_SWITCH_AUDIO_TRACK,builder.toString());
            }
        }
    }

    private void videoTrackImpl(int idx) {
        if (mMediaPlayer != null && mMediaInfo != null) {
            int id = mMediaInfo.getVideoIdx(idx);
            String str = Integer.toString(id);
            StringBuilder builder = new StringBuilder();
            builder.append("vid:"+str);
            LOGI(TAG,"[videoTrackImpl]"+builder.toString());
            mMediaPlayer.setParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_SWITCH_VIDEO_TRACK,builder.toString());
        }
    }

    private void soundTrackImpl(int idx) {
        if (mMediaPlayer != null && mMediaInfo != null) {
            LOGI(TAG,"[soundTrackImpl]idx:"+idx);
            String soundTrackStr = "stereo";
            if(idx == 0) {
                soundTrackStr = "stereo";
            }
            else if(idx == 1) {
                soundTrackStr = "lmono";
            }
            else if(idx == 2) {
                soundTrackStr = "rmono";
            }
            else if(idx == 3) {
                soundTrackStr = "lrmix";
            }
            LOGI(TAG,"[soundTrackImpl]soundTrackStr:"+soundTrackStr);
            mMediaPlayer.setParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_SWITCH_SOUND_TRACK, soundTrackStr);
        }
    }

    private void audioDtsAseetImpl(int apre, int asset) {
        if (mMediaPlayer != null && mMediaInfo != null) {
            String aprestr = Integer.toString(apre);
            String assetstr = Integer.toString(asset);
            StringBuilder builder = new StringBuilder();
            builder.append("dtsApre:"+aprestr);
            builder.append("dtsAsset:"+assetstr);
            LOGI(TAG,"[audioDtsAseetImpl]"+builder.toString());
            mMediaPlayer.setParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_SET_DTS_ASSET, builder.toString());
        }
    }

    private int getDtsApresentTotalNum() {
        int num = 0;
        Parcel p = Parcel.obtain();
        if (mMediaPlayer != null ) {
            p = mMediaPlayer.getParcelParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_GET_DTS_ASSET_TOTAL);
            num = p.readInt();
            p.readIntArray(assetsArrayNum);
        }
        p.recycle();

        LOGI(TAG,"[getDtsApresentTotalNum] num:"+num);
        for(int i = 0; i < num; i++) {
            LOGI(TAG,"[getDtsApresentTotalNum] assetsArrayNum["+i+"]:"+assetsArrayNum[i]);
        }
        return num;
    }

    private int getDtsAssetTotalNum(int apre) {
        int num = 0;
        if(apre < getDtsApresentTotalNum()) {
            if(assetsArrayNum != null) {
                num = assetsArrayNum[apre];
                LOGI(TAG,"[getDtsAssetTotalNum] assetsArrayNum["+apre+"]:"+assetsArrayNum[apre]);
            }
        }
        return num;
    }
    
    private void play3DImpl(int idx) {
        LOGI(TAG,"[play3DSelect]idx:"+idx);
        boolean ret = false;
        if (mMediaPlayer != null) {
            // TODO: should open after 3d function debug ok
            ret = mMediaPlayer.setParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_SET_DISPLAY_MODE,idx);
            if(idx > 0) {
                set_3d_flag = true;
            }
            else {
                set_3d_flag = false;
            }
            LOGI(TAG,"[play3DSelect]ret:"+ret);
            if(!ret) {
                if(mOption != null) {
                    mOption.set3DMode(0);
                }
                set_3d_flag = false;
                Toast toast =Toast.makeText(VideoPlayer.this, getResources().getString(R.string.not_support_3d),Toast.LENGTH_SHORT );
                toast.setGravity(Gravity.BOTTOM,/*110*/0,0);
                toast.setDuration(0x00000001);
                toast.show();
                startOsdTimeout();
            }
        }
    }

    private void close3D() {
        LOGI(TAG,"[close3D]mMediaPlayer:"+mMediaPlayer+",mOption:"+mOption);
        if (mMediaPlayer != null && mOption != null) {
            if(set_3d_flag) {
                mOption.set3DMode(0);
                mMediaPlayer.setParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_SET_DISPLAY_MODE,0);
            }
        }
    }

    private static final int OUTPUT480_FULL_WIDTH = 720;
    private static final int OUTPUT480_FULL_HEIGHT = 480;
    private static final int OUTPUT576_FULL_WIDTH = 720;
    private static final int OUTPUT576_FULL_HEIGHT = 576;
    private static final int OUTPUT720_FULL_WIDTH = 1280;
    private static final int OUTPUT720_FULL_HEIGHT = 720;
    private static final int OUTPUT768_FULL_WIDTH = 1366;
    private static final int OUTPUT768_FULL_HEIGHT = 768;
    private static final int OUTPUT1080_FULL_WIDTH = 1920;
    private static final int OUTPUT1080_FULL_HEIGHT = 1080;
    private static final int OUTPUT4k2k_FULL_WIDTH = 3840;
    private static final int OUTPUT4k2k_FULL_HEIGHT = 2160;
    private static final int OUTPUT4k2ksmpte_FULL_WIDTH = 4096;
    private static final int OUTPUT4k2ksmpte_FULL_HEIGHT = 2160;
    private final String mDisplayMode = "/sys/class/display/mode";
    private final String mDisplayModePanel = "panel";

    private int getOutputWidth(String mode) {
        int w = 0;
        if (mode == null) {
            return w;
        }

        if (mode.contains("480")) {
            w = OUTPUT480_FULL_WIDTH;
        }
        else if (mode.contains("576")) {
            w = OUTPUT576_FULL_WIDTH;
        }
        else if (mode.contains("720")) {
            w = OUTPUT720_FULL_WIDTH;
        }
        else if (mode.contains("768")) {
            w = OUTPUT768_FULL_WIDTH;
        }
        else if (mode.contains("1080")) {
            w = OUTPUT1080_FULL_WIDTH;
        }
        else if (mode.contains("4k2k")) {
            if (mode.equals("4k2ksmpte")) {
                w = OUTPUT4k2ksmpte_FULL_WIDTH;
            }
            else {
                w = OUTPUT4k2k_FULL_WIDTH;
            }
        }

        return w;
    }

    private int getOutputHeight(String mode) {
        int h = 0;
        if (mode == null) {
            return h;
        }

        if (mode.contains("480")) {
            h = OUTPUT480_FULL_HEIGHT;
        }
        else if (mode.contains("576")) {
            h = OUTPUT576_FULL_HEIGHT;
        }
        else if (mode.contains("720")) {
            h = OUTPUT720_FULL_HEIGHT;
        }
        else if (mode.contains("768")) {
            h = OUTPUT768_FULL_HEIGHT;
        }
        else if (mode.contains("1080")) {
            h = OUTPUT1080_FULL_HEIGHT;
        }
        else if (mode.contains("4k2k")) {
            if (mode.equals("4k2ksmpte")) {
                h = OUTPUT4k2ksmpte_FULL_HEIGHT;
            }
            else {
                h = OUTPUT4k2k_FULL_HEIGHT;
            }
        }

        return h;
    }

    private String getCurrentDisplayMode() {
        String mode = sw.readSysfs(mDisplayMode).replaceAll("\n","");
        if(mode == null) {
            mode = mDisplayModePanel;
        }
        return mode;
    }

    //@@--------random seek function-------------------------------------------------------------------------------------------
    private boolean randomSeekTestFlag = false;
    private Random r = new Random(99);  
    private boolean randomSeekEnable() {
        if(sw == null) {
            sw = (SystemWriteManager) getSystemService("system_write"); 
        }
        boolean ret = sw.getPropertyBoolean("sys.vprandomseek.enable",false);
        return ret;
    }
    private void randomSeekTest() {
        if(!randomSeekEnable())
            return;
        
        if(r == null) {
            r = new Random();  
        }
        int i = r.nextInt();
        LOGI(TAG,"[randomSeekTest]0i:"+i);
        if(i<0) {
            i = i * -1;
        }

        i = i%80;
        LOGI(TAG,"[randomSeekTest]1i:"+i);
        
        int pos = totaltime * (i+1) / 100;
        randomSeekTestFlag = true;

        //check for small stream while seeking
        int pos_check = totaltime * (i+1) - pos * 100;
        if(pos_check>0) 
            pos += 1;
        if(pos>=totaltime)
            pos = totaltime;

        LOGI(TAG,"[randomSeekTest]seekTo:"+pos);
        seekTo(pos);
    }

    //@@--------this part for play function implement--------------------------------------------------------------------------------
    // The ffmpeg step is 2*step
    private Toast ff_fb = null;
    private boolean FF_FLAG = false;
    private boolean FB_FLAG = false;
    private int FF_LEVEL = 0;
    private int FB_LEVEL = 0;
    private static int FF_MAX = 6;
    private static int FB_MAX = 6;
    private static int FF_SPEED[] = {0, 2, 4, 8, 16, 32, 64};
    private static int FB_SPEED[] = {0, 2, 4, 8, 16, 32, 64};
    private static int FF_STEP[] =  {0, 2, 4, 8, 16, 32, 64};
    private static int FB_STEP[] =  {0, 2, 4, 8, 16, 32, 64};
    private static int mRetryTimesMax = 5; // retry play after volume unmounted 
    private static int mRetryTimes = mRetryTimesMax; 
    private static int mRetryStep = 1000; //1000ms
    private boolean mRetrying = false;
    private Timer retryTimer = new Timer();
    private String mPath;
    private int seekPosBac = 0;
    private boolean seekBarEnableMsgSended = false;

    private void sendUpdateIconMsg() {
        if(mHandler != null) {
            Message msg = mHandler.obtainMessage(MSG_UPDATE_ICON);
            mHandler.sendMessage(msg);
        }
    }

    private void updateIconResource() {
        if((progressBar == null) || (fastforwordBtn == null) || (fastreverseBtn == null) || (playBtn == null)) {
            return;
        }
        
        if(mState == STATE_PLAYING) {
            playBtn.setImageResource(R.drawable.pause);
        }
        else if(mState == STATE_PAUSED) {
            playBtn.setImageResource(R.drawable.play);
        }
        else if(mState == STATE_SEARCHING) {
            playBtn.setImageResource(R.drawable.play);
        }

        if(mCanSeek && seekBarEnableMsgSended) {
            progressBar.setEnabled(true);

            if(mMediaPlayer != null) {
				  /*
                String playerTypeStr = mMediaPlayer.getStringParameter(mMediaPlayer.KEY_PARAMETER_AML_PLAYER_TYPE_STR);
                if((playerTypeStr != null) && (playerTypeStr.equals("AMLOGIC_PLAYER"))) {
                    fastforwordBtn.setEnabled(true);
                    fastreverseBtn.setEnabled(true);
                    fastforwordBtn.setImageResource(R.drawable.ff);
                    fastreverseBtn.setImageResource(R.drawable.rewind);
                }
                else {
                    fastforwordBtn.setEnabled(false);
                    fastreverseBtn.setEnabled(false);
                    fastforwordBtn.setImageResource(R.drawable.ff_disable);
                    fastreverseBtn.setImageResource(R.drawable.rewind_disable);
                }
                */
                    fastforwordBtn.setEnabled(true);
                    fastreverseBtn.setEnabled(true);
                    fastforwordBtn.setImageResource(R.drawable.ff);
                    fastreverseBtn.setImageResource(R.drawable.rewind);
            }
            else {
                fastforwordBtn.setEnabled(false);
                fastreverseBtn.setEnabled(false);
                fastforwordBtn.setImageResource(R.drawable.ff_disable);
                fastreverseBtn.setImageResource(R.drawable.rewind_disable);
            }
        }
        else {
            progressBar.setEnabled(false);
            fastforwordBtn.setEnabled(false);
            fastreverseBtn.setEnabled(false);
            fastforwordBtn.setImageResource(R.drawable.ff_disable);
            fastreverseBtn.setImageResource(R.drawable.rewind_disable);
        }
    }

    private void resetVariate() {
        showDtsAseetFromInfoLis = false;
        progressBarSeekFlag = false;
        haveTried = false;
        mRetrying = false;
        mRetryTimes = mRetryTimesMax;
        mUpdateHadling = false;
        seekPosBac = 0;
        mSubNum = 0;
        mSubOffset = -1;
        isShowImgSubtitle = false;
        seekBarEnableMsgSended = false;
        updateIconResource();
    }

    private void sendPlayFileMsg() {
        showOsdView();
        if(mHandler != null) {
            Message msg = mHandler.obtainMessage(MSG_PLAY);
            mHandler.sendMessageDelayed(msg, MSG_SEND_DELAY);
            LOGI(TAG,"[sendPlayFileMsg]sendMessageDelayed MSG_PLAY");
        }
    }
    
    private void playFile(String path) {
        LOGI(TAG,"[playFile]resume mode:"+mOption.getResumeMode()+",path:"+path);
        if(mOption == null)
            return;

        resetVariate();

        if(mResumePlay.getEnable() == true) {
            setVideoPath(path);
            //showCtlBar();
            showOsdView();
            return;
        }
        
        if(mOption.getResumeMode() == true) {
            bmPlay(path);
        }
        else {
            setVideoPath(path);
        }
        //showCtlBar();
        showOsdView();
    }

    private void retryPlay() {
        LOGI(TAG,"[retryPlay]mRetryTimes:"+mRetryTimes+",mRetryStep:"+mRetryStep+",mResumePlay:"+mResumePlay);
        if(mResumePlay == null) {
            browserBack(); // no need to retry, back to file list
            return;
        }

        LOGI(TAG,"[retryPlay]mResumePlay.getEnable():"+mResumePlay.getEnable());
        if(false == mResumePlay.getEnable()) {
            browserBack(); // no need to retry, back to file list
            return;
        }

        mRetrying = true;

        TimerTask task = new TimerTask(){   
            public void run() {
                LOGI(TAG,"[retryPlay]TimerTask run mRetryTimes:"+mRetryTimes);
                if(mRetryTimes > 0) {
                    mRetryTimes--;
                    if(mHandler != null) {
                        Message msg = mHandler.obtainMessage(MSG_RETRY_PLAY);
                        mHandler.sendMessageDelayed(msg, MSG_SEND_DELAY);
                        LOGI(TAG,"[retryPlay]sendMessageDelayed MSG_RETRY_PLAY");
                    }
                }
                else {
                    retryTimer.cancel();
                    retryTimer = null;
                    mRetrying = false;
                    if(mHandler != null) {
                        Message msg = mHandler.obtainMessage(MSG_RETRY_END);
                        mHandler.sendMessageDelayed(msg, MSG_SEND_DELAY);
                        LOGI(TAG,"[retryPlay]sendMessageDelayed MSG_RETRY_END");
                    }
                }
            }     
        };   
        
        retryTimer = new Timer();
        retryTimer.schedule(task, mRetryStep);
    }

    private void browserBack() {
        LOGI(TAG,"[browserBack]backToOtherAPK:"+backToOtherAPK+",browserBackDoing:"+browserBackDoing);
        if(browserBackDoing == true)
            return;
        
        item_position_selected = item_position_selected_init + mPlayList.getindex();
        browserBackDoing = true;
        if(browserBackInvokeFromOnPause == true) {
            mPlayList.rootPath =null;
        }
        else {
            if(!backToOtherAPK) {
                Intent selectFileIntent = new Intent();
                Bundle bundle = getFilePos();
                selectFileIntent.setClass(VideoPlayer.this, FileList.class);
                selectFileIntent.putExtras(bundle);
                startActivity(selectFileIntent);
            }
        }
        showNoOsdView(); 
        //close 3D
        close3D();
        
        sendStopMsg();
        finish();
    }

    private void playPause() {
        LOGI(TAG,"[playPause]mState:"+mState);
        if(mState == STATE_PLAYING) {
            pause();
        }
        else if(mState == STATE_PAUSED) {
            start();
        }
        else if(mState == STATE_SEARCHING) {
           stopFWFB();
           start();
        }

        startOsdTimeout();
    }

    private void playPrev() {
        LOGI(TAG,"[playPrev]mState:"+mState);
        if (!getSwitchEnable()) return;
        startSwitchTimeout();
        stopOsdTimeout();
        if(mState != STATE_PREPARING) { // avoid status error for preparing
            close3D();
            stopFWFB();
            sendStopMsg();
            mBookmark.set(mPlayList.getcur(), curtime);
            mStateBac = STATE_STOP;
            mPath = mPlayList.moveprev();
            sendPlayFileMsg();
        }
        else {
            LOGI(TAG,"[playPrev]mState=STATE_PREPARING, error status do nothing only waitting");
        }
    }

    private void playNext() {
        LOGI(TAG,"[playNext]mState:"+mState);
        if (!getSwitchEnable()) return;
        startSwitchTimeout();
        stopOsdTimeout();
         if(mState != STATE_PREPARING) { // avoid status error for preparing
            close3D();
            stopFWFB();
            sendStopMsg();
            mBookmark.set(mPlayList.getcur(), curtime);
            mStateBac = STATE_STOP;
            mPath = mPlayList.movenext();
            sendPlayFileMsg();
        }
        else {
            LOGI(TAG,"[playNext]mState=STATE_PREPARING, error status do nothing only waitting");
        }
    }

    private void playCur() {
        LOGI(TAG,"[playCur]");
        if (!getSwitchEnable()) return;
        startSwitchTimeout();
        stopOsdTimeout();
        stopFWFB();
        sendStopMsg();
        curtime = 0;
        totaltime = 0;
        mBookmark.set(mPlayList.getcur(), curtime);
        mStateBac = STATE_STOP;
        mPath = mPlayList.getcur();
        sendPlayFileMsg();
    }

    private void fastForward() {
        LOGI(TAG,"[fastForward]mState:"+mState+",FF_FLAG:"+FF_FLAG+",FF_LEVEL:"+FF_LEVEL+",FB_FLAG:"+FB_FLAG+",FB_LEVEL:"+FB_LEVEL);
        progressBarSeekFlag = false;
        if(mState == STATE_SEARCHING) {
            if(FF_FLAG) {
                if(FF_LEVEL < FF_MAX) {
                    FF_LEVEL = FF_LEVEL + 1;
                }
                else {
                    FF_LEVEL = 0;
                }

                FFimpl(FF_STEP[FF_LEVEL]);
                
                if(FF_LEVEL == 0) {
                    ff_fb.cancel();
                    FF_FLAG = false;
                    start();
                }
                else {
                    ff_fb.setText(new String("FF x" + Integer.toString(FF_SPEED[FF_LEVEL])));
                    ff_fb.show();
                }
            }

            if(FB_FLAG) {
/*
                if(FB_LEVEL > 0) {
                    FB_LEVEL = FB_LEVEL - 1;
                }
                else {
                    FB_LEVEL = 0;
                }
                
                FBimpl(FB_STEP[FB_LEVEL]);

                if(FB_LEVEL == 0) {
                    ff_fb.cancel();
                    FB_FLAG = false;
                    start();
                }
                else {
                    ff_fb.setText(new String("FB x" + Integer.toString(FB_SPEED[FB_LEVEL])));
                    ff_fb.show();
                }
*/           
                FB_FLAG = false;
                FF_FLAG = true;
                FB_LEVEL = 0;
                FF_LEVEL = 1;
                FFimpl(FF_STEP[FF_LEVEL]);
                ff_fb.setText(new String("FF x"+FF_SPEED[FF_LEVEL]));
                ff_fb.show();
            }
        }
        else {
            FFimpl(FF_STEP[1]);
            FF_FLAG = true;
            FF_LEVEL = 1;
            ff_fb.setText(new String("FF x"+FF_SPEED[FF_LEVEL]));
            ff_fb.show();
        }

        startOsdTimeout();
    }

    private void fastBackward() {
        progressBarSeekFlag = false;
        if(mState == STATE_SEARCHING) {
            if(FB_FLAG) {
                if(FB_LEVEL < FB_MAX) {
                    FB_LEVEL = FB_LEVEL + 1;
                }
                else {
                    FB_LEVEL = 0;
                }
                
                FBimpl(FB_STEP[FB_LEVEL]);

                if(FB_LEVEL == 0) {
                    ff_fb.cancel();
                    FB_FLAG = false;
                    start();
                }
                else {
                    ff_fb.setText(new String("FB x" + Integer.toString(FB_SPEED[FB_LEVEL])));
                    ff_fb.show();
                }
            }

            if(FF_FLAG) {
/*
                if(FF_LEVEL > 0) {
                    FF_LEVEL = FF_LEVEL - 1;
                }
                else {
                    FF_LEVEL = 0;
                }
                
                FFimpl(FF_STEP[FF_LEVEL]);

                if(FF_LEVEL == 0) {
                    ff_fb.cancel();
                    FF_FLAG = false;
                    start();
                }
                else {
                    ff_fb.setText(new String("FF x" + Integer.toString(FF_SPEED[FF_LEVEL])));
                    ff_fb.show();
                }
*/            
                FB_FLAG = true;
                FF_FLAG = false;
                FB_LEVEL = 1;
                FF_LEVEL = 0;
                FBimpl(FB_STEP[1]);
                ff_fb.setText(new String("FB x"+FB_SPEED[FB_LEVEL]));
                ff_fb.show();
            }
        }
        else {
            FBimpl(FB_STEP[1]);
            FB_FLAG = true;
            FB_LEVEL = 1;
            ff_fb.setText(new String("FB x"+FB_SPEED[FB_LEVEL]));
            ff_fb.show();
        }
        
        startOsdTimeout();
    }

    private void stopFWFB() {
        if(ff_fb != null)
            ff_fb.cancel();
        if(FF_FLAG)
            FFimpl(0);
        if(FB_FLAG)
            FBimpl(0);
        FF_FLAG = false;
        FB_FLAG = false;
        FF_LEVEL = 0;
        FB_LEVEL = 0;
    }

    private void FFimpl(int para) {
        if (mMediaPlayer != null) {
            LOGI(TAG,"[FFimpl]para:"+para);
            if(para > 0) {
                mState = STATE_SEARCHING;
            }
            else if(para == 0) {
                mState = STATE_PLAYING;
            }
            updateIconResource();
 /*           
            String str = Integer.toString(para);
            StringBuilder builder = new StringBuilder();
            builder.append("forward:"+str);
            LOGI(TAG,"[FFimpl]"+builder.toString());
            mMediaPlayer.setParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_TRICKPLAY_FORWARD,builder.toString());
*/
	    mMediaPlayer.fastForward(para);
        }
    }

    private void FBimpl(int para) {
        if (mMediaPlayer != null) {
            LOGI(TAG,"[FBimpl]para:"+para);
            if(para > 0) {
                mState = STATE_SEARCHING;
                mStateBac = STATE_PLAYING; // add to update icon resource and status for FB to head 
            }
            else if(para == 0) {
                mState = STATE_PLAYING;
            }
            updateIconResource();
/*            
            String str = Integer.toString(para);
            StringBuilder builder = new StringBuilder();
            builder.append("backward:"+str);
            LOGI(TAG,"[FBimpl]"+builder.toString());
            mMediaPlayer.setParameter(MediaPlayer.KEY_PARAMETER_AML_PLAYER_TRICKPLAY_BACKWARD,builder.toString());
*/           
	     mMediaPlayer.fastBackward(para);
        }
    }

    private void sendSeekByProgressBarMsg() {
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage (MSG_SEEK_BY_BAR);
            mHandler.sendMessageDelayed (msg, 500);
            LOGI (TAG, "[sendSeekByProgressBarMsg]sendMessageDelayed MSG_SEEK_BY_BAR");
        }
    }

    private void seekByProgressBar() {
    	  int step = 0;
        int dest = progressBar.getProgress();
        if (totaltime <= 30 * 1000) {//smaller than 30s
            step = dest + 1;
        }
        else {
        	  step = dest;
        }
        int pos = totaltime * (step) / 100;
        if (pos == 0 || pos == totaltime / 100) { //add dest is 0 and pos is totaltime / 100 ,start
            if (pos == 0) {
                seekTo(0);
            }
            start();
            startOsdTimeout();
            return;
        }

        if (pos - seekPosBac >= 1000 || seekPosBac - pos >= 1000) {//1000ms
            seekPosBac = pos;
        }
        else {
            return;
        }

        //check for small stream while seeking
        int pos_check = totaltime * (step) - pos * 100;
        if(pos_check>0) 
            pos += 1;
        if(pos>=totaltime)
            pos = totaltime;

        if(dest <= 1) {
            //pos = 0;
        }

        LOGI(TAG,"[seekByProgressBar]seekTo:"+pos);
        seekTo(pos);
        startOsdTimeout();
        //curtime=pos;
    }

    //@@--------this part for play control------------------------------------------------------------------------------------------
    private MediaPlayerAmlogic mMediaPlayer = null;
    private static final int STATE_ERROR = -1;
    private static final int STATE_STOP = 0;
    private static final int STATE_PREPARING = 1;
    private static final int STATE_PREPARED = 2;
    private static final int STATE_PLAYING = 3;
    private static final int STATE_PAUSED = 4;
    private static final int STATE_PLAY_COMPLETED = 5;
    private static final int STATE_SEARCHING = 6;
    private int mState = STATE_STOP;
    private int mStateBac = STATE_STOP;
    private static final int SEEK_START = 0;//flag for seek stability to stop update progressbar
    private static final int SEEK_END = 1;
    private int mSeekState = SEEK_END;
    
    //@@private int mCurrentState = STATE_IDLE;
    //@@private int mTargetState  = STATE_IDLE;
    
    private OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
        public void onAudioFocusChange(int focusChange) {
            Log.d(TAG, "onAudioFocusChange, focusChange: " + focusChange);
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                    if (mMediaPlayer != null) {
                        Log.d(TAG, "onAudioFocusChange, setVolume 0");
                        mMediaPlayer.setVolume(0.0f);
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (mMediaPlayer != null) {
                        Log.d(TAG, "onAudioFocusChange, setVolume 1");
                        mMediaPlayer.setVolume(1.0f);
                    }
                    break;
            }
        }
    };

    private void sendStopMsg() {
        if(mHandler != null) {
            Message msg = mHandler.obtainMessage(MSG_STOP);
            mHandler.sendMessageDelayed(msg, MSG_SEND_DELAY);
            LOGI(TAG,"[sendStopMsg]sendMessageDelayed MSG_STOP");
        }
    }

    private void start() {
        LOGI(TAG,"[start]mMediaPlayer:"+mMediaPlayer);
        if(mMediaPlayer != null) {
            if (!mAudioFocused) {
                mAudioFocused = true;
                Log.d(TAG, "start, requestAudioFocus");
                mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN);
            }
            mMediaPlayer.start();
            Locale loc = Locale.getDefault();
            if (loc != null && mIsBluray) {
                if (mSubIndex == 0) {
                    mSubIndex = getLanguageIndex(MediaInfo.BLURAY_STREAM_TYPE_SUB, loc.getISO3Language());
                    mMediaPlayer.subtitleOpenIdx(mSubIndex);
                }
            }/* else {
                //mSubIndex = 0;
                if(mSubIndex == mMediaPlayer.subtitleTotal()){
			if(mHandler != null) {
	                mHandler.removeMessages(MSG_UPDATE_SUB);
	                Message msg = mHandler.obtainMessage(MSG_UPDATE_SUB);
	                mHandler.sendMessageDelayed(msg, 500);
	            }
                }
                if (mSubIndex >= 0 && mSubIndex < mMediaPlayer.subtitleTotal())
			mMediaPlayer.subtitleOpenIdx(mSubIndex);
            }*/
            mState = STATE_PLAYING;
            updateIconResource();
            
            if(mHandler != null) {
                mHandler.removeMessages(MSG_UPDATE_PROGRESS);
                Message msg = mHandler.obtainMessage(MSG_UPDATE_PROGRESS);
                mHandler.sendMessageDelayed(msg, MSG_SEND_DELAY);
            }
        }
    }

    private void pause() {
        LOGI(TAG,"[pause]mMediaPlayer:"+mMediaPlayer);
        if(mMediaPlayer != null) {
            if(/*isPlaying()*/mState == STATE_PLAYING) {
                mMediaPlayer.pause();
                mState = STATE_PAUSED;
                updateIconResource();
            }
        }
    }

    private void stop() {
        LOGI(TAG,"[stop]mMediaPlayer:"+mMediaPlayer+",mState:"+mState);
        if(mMediaPlayer != null && mState != STATE_STOP) {
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mState = STATE_STOP;
        }
    }

    private void release() {
        LOGI(TAG,"[release]mMediaPlayer:"+mMediaPlayer);
        if(mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mState = STATE_STOP;
            //mStateBac = STATE_STOP; //shield for resume play while is in pause status
            mSeekState = SEEK_END;
            if (mAudioFocused) {
                Log.d(TAG, "release, abandonAudioFocus");
                mAudioManager.abandonAudioFocus(mAudioFocusListener);
                mAudioFocused = false;
            }
            if (mBlurayVideoLang != null)
                mBlurayVideoLang.clear();
            if (mBlurayAudioLang != null)
                mBlurayAudioLang.clear();
            if (mBluraySubLang != null)
                mBluraySubLang.clear();
            if (mBlurayChapter != null)
                mBlurayChapter.clear();
            mIsBluray = false;
            mSubIndex = 0;
        }
    }

    private int getDuration() {
        //LOGI(TAG,"[getDuration]mMediaPlayer:"+mMediaPlayer);
        if(mMediaPlayer != null) {
            return mMediaPlayer.getDuration();
        }
        return 0;
    }

    private int getCurrentPosition() {
        //LOGI(TAG,"[getCurrentPosition]mMediaPlayer:"+mMediaPlayer);
        if(mMediaPlayer != null) {
            return mMediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public boolean isPlaying() {
        //LOGI(TAG,"[isPlaying]mMediaPlayer:"+mMediaPlayer);
        boolean ret = false;
        if(mMediaPlayer != null) {
            if(mState != STATE_ERROR &&
                mState != STATE_STOP &&
                mState != STATE_PREPARING) {
                ret = mMediaPlayer.isPlaying();
            }
        }
        return ret;
    }

    private void seekTo(int msec) {
        LOGI(TAG,"[seekTo]msec:"+msec+",mState:"+mState);
        if (mMediaPlayer != null && mCanSeek == true) {
            // stop update progress bar
            mSeekState = SEEK_START;
            if(mHandler != null) {
                mHandler.removeMessages(MSG_UPDATE_PROGRESS);
            }
            
            if(mState == STATE_SEARCHING) {
                mStateBac = STATE_PLAYING;
            }
            else if(mState == STATE_PLAYING || mState == STATE_PAUSED){
                mStateBac = mState;
            }
            else {
                mStateBac = STATE_ERROR;
                LOGI(TAG,"[seekTo]state error for seek, state:"+mState);
                return;
            }

            stopFWFB();
            mMediaPlayer.seekTo(msec);
            //mState = STATE_SEARCHING;
            //updateIconResource();
        } 
    }

    private void setVideoPath(String path) {
        //LOGI(TAG,"[setVideoPath]path:"+path);
        /*Uri uri = null;
        String[] cols = new String[] {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA
        };
        
        if(mContext == null) {
            LOGE(TAG,"[setVideoPath]mContext=null error!!!");
            return;
        }

        //change path to uri such as content://media/external/video/media/8206
        ContentResolver resolver = mContext.getContentResolver();
        String where = MediaStore.Video.Media.DATA + "=?" + path;
        Cursor cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cols, where , null, null);
        if (cursor != null && cursor.getCount() == 1) {
            int colidx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            cursor.moveToFirst();
            int id = cursor.getInt(colidx);
            uri = MediaStore.Video.Media.getContentUri("external");
            String uriStr = uri.toString() + "/" + Integer.toString(id);
            uri = Uri.parse(uriStr);
            LOGI(TAG,"[setVideoPath]uri:"+uri.toString());
        }

        if(uri == null) {
            LOGE(TAG,"[setVideoPath]uri=null error!!!");
            return;
        }
        setVideoURI(uri);*/

        /*LOGI(TAG,"[setVideoPath]Uri.parse(path):"+Uri.parse(path));
        Uri uri = Uri.parse(path);
        AssetFileDescriptor fd = null;
        try {
            ContentResolver resolver = mContext.getContentResolver();
            fd = resolver.openAssetFileDescriptor(uri, "r");
            if (fd == null) {
                LOGE(TAG,"[setVideoPath]fd =null error!!!");
                return;
            }
            if (fd.getDeclaredLength() < 0) {
                mMediaPlayer.setDataSource(fd.getFileDescriptor());
            } else {
                mMediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getDeclaredLength());
            }
            mMediaPlayer.prepare();
            return;
        } catch (SecurityException ex) {
            LOGE(TAG, "[SecurityException]Unable to open content: " + mUri+",ex:"+ex);
            mState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        } catch (IOException ex) {
            LOGE(TAG, "[IOException]Unable to open content: " + mUri+",ex:"+ex);
            mState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        } catch (IllegalArgumentException ex) {
            LOGE(TAG, "[IllegalArgumentException]Unable to open content: " + mUri+",ex:"+ex);
            mState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }finally {
            if (fd != null) {
                try {
                    fd.close();
                } catch (IOException ex) {}
            }
        }*/

        LOGI(TAG,"[setVideoPath]Uri.parse(path):"+Uri.parse(path));
        path = changeForIsoFile(path);
        setVideoURI(Uri.parse(path), path); //add path to resolve special character for uri, such as  ";" | "/" | "?" | ":" | "@" | "&" | "=" | "+" |"$" | ","
        if (!isTimedTextDisable()) {
            searchExternalSubtitle(path);
        }
    }

    private String changeForIsoFile(String path) {
        File file = new File(path);
        String fpath = file.getPath();
        if (fpath.toLowerCase().endsWith(".iso")) {
            FileList.execCmd("vdc loop unmount");
            FileList.execCmd("vdc loop mount " + "\"" + fpath + "\"");
            try {
                Thread.sleep(100);
            } catch (Exception e) {
            }
            fpath = "bluray:/storage/external_storage/VIRTUAL_CDROM";
            mIsBluray = true;
            mBlurayVideoLang = new ArrayList<String>();
            mBlurayAudioLang = new ArrayList<String>();
            mBluraySubLang = new ArrayList<String>();
            mBlurayChapter = new ArrayList<ChapterInfo>();
            chapterBtn.setVisibility(View.VISIBLE);
            progressBar.setEnabled(false);
        } else {
            mIsBluray = false;
            chapterBtn.setVisibility(View.GONE);
        }
        LOGI(TAG, "[changeForIsoFile]fpath: " + fpath);
        return fpath;
    }

    private void setVideoURI(Uri uri, String path) {
        LOGI(TAG,"[setVideoURI]uri:"+uri+",path:"+path);
        setVideoURI(uri, null, path);
    }

    private void setVideoURI(Uri uri, Map<String, String> headers, String path) {
        LOGI(TAG,"[setVideoURI]uri:"+uri+",headers:"+headers+",mState:"+mState);
        mUri = uri;
        mHeaders = headers;
        try{
            //mMediaPlayer.setDataSource(mContext, mUri, mHeaders);
            mMediaPlayer.setDataSource(path);
            mState = STATE_PREPARING;
            mMediaPlayer.prepare();
        } catch (IOException ex) {
            LOGE(TAG, "Unable to open content: " + mUri+",ex:"+ex);
            if(haveTried == false) {
                haveTried = true;
                trySetVideoPathAgain(uri, headers, path);
            }
            else {
                mState = STATE_ERROR;
                mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
            }
        } catch (IllegalArgumentException ex) {
            LOGE(TAG, "Unable to open content: " + mUri+",ex:"+ex);
            mState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        } catch (IllegalStateException ex) {
            LOGE(TAG, "Unable to open content: " + mUri+",ex:"+ex);
            mState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }
        //requestLayout();
        //invalidate();
    }

    private boolean haveTried = false;
    private void trySetVideoURIAgain(Uri uri, Map<String, String> headers, String paramPath) {
        if(uri == null) {
            LOGE(TAG,"[trySetVideoURIAgain]init uri=null error!!!");
            return;
        }

        if(mContext == null) {
            LOGE(TAG,"[trySetVideoURIAgain]mContext=null error!!!");
            return;
        }
        
        LOGI (TAG, "[trySetVideoURIAgain]path:" + uri.getPath());
        Uri uriTmp = null;
        String path = uri.getPath();
        String[] cols = new String[] {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA
        };
        
        //change path to uri such as content://media/external/video/media/8206
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cols, null, null, null);
        if(cursor != null && cursor.getCount() > 0) {
            int destIdx = -1;
            int len = cursor.getCount();
            LOGI (TAG, "[trySetVideoURIAgain]len:" + len);
            String [] pathList = new String[len];
            cursor.moveToFirst();
            int dataIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            int idIdx = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            for(int i=0;i<len;i++) {
                LOGI (TAG, "[trySetVideoURIAgain]cursor.getString(dataIdx):" + cursor.getString (dataIdx));
                if((cursor.getString(dataIdx)).startsWith(path)) {
                    destIdx = cursor.getInt(idIdx);
                    LOGI (TAG, "[trySetVideoURIAgain]destIdx:" + destIdx);
                    break;
                }
                else {
                    cursor.moveToNext();
                }
            }

            if(destIdx >= 0) {
                uriTmp = MediaStore.Video.Media.getContentUri("external");
                String uriStr = uriTmp.toString() + "/" + Integer.toString(destIdx);
                uriTmp = Uri.parse(uriStr);
                LOGI (TAG, "[trySetVideoURIAgain]uriTmp:" + uriTmp.toString());
            }
        }

        cursor.close();

        if(uriTmp == null) {
            LOGE(TAG,"[trySetVideoURIAgain]uriTmp=null error!!!");
            Toast.makeText(mContext,mContext.getText(R.string.wait_for_scan),Toast.LENGTH_SHORT).show();
            browserBack();
            return;
        }
        LOGI (TAG, "[trySetVideoURIAgain]setVideoURI uriTmp:" + uriTmp);
        setVideoURI(uriTmp, paramPath);
    }

    private void trySetVideoPathAgain(Uri uri, Map<String, String> headers, String path) {
        LOGI(TAG,"[trySetVideoPathAgain]path:"+path);
        try{
            mMediaPlayer.setDataSource(path);
            mState = STATE_PREPARING;
            mMediaPlayer.prepare();
        } catch (IOException ex) {
            LOGE(TAG, "[trySetVideoPathAgain] Unable to open content: " + path+",ex:"+ex);
            trySetVideoURIAgain(uri, headers, path); // should debug, maybe some error
            mState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        } catch (IllegalArgumentException ex) {
            LOGE(TAG, "[trySetVideoPathAgain] Unable to open content: " + path+",ex:"+ex);
            mState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }
    }

    private void initPlayer() {
        LOGI(TAG,"[initPlayer]mSurfaceHolder:"+mSurfaceHolder);
        if (mSurfaceHolder == null) {
            return;
        }
        
        release();
        mMediaPlayer = new MediaPlayerAmlogic();
        mMediaPlayer.setOnPreparedListener(mPreparedListener);
        mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
        mMediaPlayer.setOnCompletionListener(mCompletionListener);
        mMediaPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
        mMediaPlayer.setOnErrorListener(mErrorListener);
        mMediaPlayer.setOnInfoListener(mInfoListener);
        mMediaPlayer.setOnBlurayInfoListener(mBlurayListener);
        mMediaPlayer.setDisplay(mSurfaceHolder);
        mMediaPlayer.setOnTimedTextListener(mTimedTextListener);
        mMediaPlayer.setOnSubtitleDataListener(mSubtitleDataListener);
    }

    //@@--------this part for listener----------------------------------------------------------------------------------------------
		private boolean mCanPause;
		private boolean mCanSeek;
		private boolean mCanSeekBack;
		private boolean mCanSeekForward;
		private boolean showDtsAseetFromInfoLis;
		private long mErrorTime = 0;
		private long mErrorTimeBac = 0;
    MediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
        new MediaPlayer.OnVideoSizeChangedListener() {
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                LOGI(TAG,"[onVideoSizeChanged]");
                if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                    displayModeImpl();
                }
                if(mMediaPlayer != null && mSurfaceView != null) {
                    int videoWidth = mMediaPlayer.getVideoWidth();
                    int videoHeight = mMediaPlayer.getVideoHeight();
                    LOGI(TAG,"[onVideoSizeChanged]videoWidth:"+videoWidth+",videoHeight:"+videoHeight);
                    if (videoWidth != 0 && videoHeight != 0) {
                        ////displayModeImpl();
                        mSurfaceView.requestLayout();
                        /*mSurfaceView.getHolder().setFixedSize(videoWidth, videoHeight);
                        mSurfaceView.requestLayout();*/
                    }
                }
            }
    };

    MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        public void onPrepared(MediaPlayer mp) {
            LOGI(TAG,"[mPreparedListener]onPrepared mp:"+mp);
            mState = STATE_PREPARED;

            Metadata data = mp.getMetadata(MediaPlayer.METADATA_ALL, MediaPlayer.BYPASS_METADATA_FILTER);
            if (data != null) {
                mCanPause = !data.has(Metadata.PAUSE_AVAILABLE)
                    || data.getBoolean(Metadata.PAUSE_AVAILABLE);
                mCanSeekBack = !data.has(Metadata.SEEK_BACKWARD_AVAILABLE)
                    || data.getBoolean(Metadata.SEEK_BACKWARD_AVAILABLE);
                mCanSeekForward = !data.has(Metadata.SEEK_FORWARD_AVAILABLE)
                    || data.getBoolean(Metadata.SEEK_FORWARD_AVAILABLE);
                mCanSeek = mCanSeekBack && mCanSeekForward;
                LOGI(TAG,"[mPreparedListener]mCanSeek:"+mCanSeek);
            } else {
                mCanPause = mCanSeek = mCanSeekBack = mCanSeekForward = true;
            }

            data.recycleParcel();

            // TODO: some error should debug 20150525
            if (!isTimedTextDisable()) {
                mTrackInfo = mp.getTrackInfo();
                if(mTrackInfo != null) {
                    LOGI(TAG,"[mPreparedListener]mTrackInfo.length:"+mTrackInfo.length);
                    for(int i = 0; i < mTrackInfo.length; i++) {
                        int trackType = mTrackInfo[i].getTrackType();
                        LOGI(TAG,"[mPreparedListener]trackInfo["+i+"].trackType:"+trackType);
                        
                        if (trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT) {
                            mSubNum ++;
                            if (mSubOffset == -1) {
                                mSubOffset = i;
                            }
                        }
                    }
                }

                LOGI(TAG,"[mPreparedListener]mSubOffset:"+mSubOffset);
                if (mSubOffset >= 0) {
                    mMediaPlayer.selectTrack(mSubOffset);
                }
            }

            if(mStateBac != STATE_PAUSED) {
                start();
            }
            initSubtitle();
            initMediaInfo();
            displayModeImpl(); // init display mode //useless because it will reset when start playing, it should set after the moment playing
            showCertification(); // show certification 
            showMsgWidget(); // show msg widget
            msgShow();
            
            if(mResumePlay.getEnable() == true) {
                mResumePlay.setEnable(false);
                int targetState = mStateBac; //get mStateBac first for seekTo will change mStateBac
                mState = mStateBac; //prepare mState before seekTo 
                seekTo(mResumePlay.getTime());
                LOGI(TAG,"[mPreparedListener]targetState:"+targetState);
                if(targetState == STATE_PAUSED) {
                    start();
                    pause();
                }
                return;
            }
            
            if(mOption.getResumeMode() == true) {
                seekTo(bmPos);
            }
        }
    };

    private MediaPlayer.OnCompletionListener mCompletionListener =
        new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mp) {
            LOGI(TAG, "[onCompletion] mOption.getRepeatMode():"+mOption.getRepeatMode());
            mState = STATE_PLAY_COMPLETED;
            curtime = 0; // reset current time
            if(progressBar != null) {
                progressBar.setProgress(0);
            }
            if (mBlurayVideoLang != null)
                mBlurayVideoLang.clear();
            if (mBlurayAudioLang != null)
                mBlurayAudioLang.clear();
            if (mBluraySubLang != null)
                mBluraySubLang.clear();
            if (mBlurayChapter != null)
                mBlurayChapter.clear();
            mIsBluray = false;
            mSubIndex = 0;
            if(mOption.getRepeatMode() == mOption.REPEATONE) {
                playCur();
            }
            else if(mOption.getRepeatMode() == mOption.REPEATLIST) {
                playNext();
            }
            else {
                LOGE(TAG, "[onCompletion] Wrong mOption.getRepeatMode():"+mOption.getRepeatMode());
            }
            /*mCurrentState = STATE_PLAYBACK_COMPLETED;
            mTargetState = STATE_PLAYBACK_COMPLETED;
            if (mMediaController != null) {
                mMediaController.hide();
            }
            if (mOnCompletionListener != null) {
                mOnCompletionListener.onCompletion(mMediaPlayer);
            }*/
        }
    };

    private MediaPlayer.OnSeekCompleteListener mSeekCompleteListener = 
        new MediaPlayer.OnSeekCompleteListener() {
        public void onSeekComplete(MediaPlayer mp) {
            LOGI(TAG,"[onSeekComplete] progressBarSeekFlag:"+progressBarSeekFlag+",mStateBac:"+mStateBac);

            ignoreUpdateProgressbar = false;

            if (isTimedTextDisable()) {
                if(mMediaPlayer != null) {
                    mMediaPlayer.subtitleResetForSeek();
                }
            }

            if(progressBarSeekFlag == false) { //onStopTrackingTouch
                stopFWFB(); //reset fw/fb status
                if(mStateBac == STATE_PLAYING) {
                    start();
                }
                else if(mStateBac == STATE_PAUSED) {
                    pause();
                }
                else if(mStateBac == STATE_SEARCHING) {
                    // do nothing
                }
                else {
                    mStateBac = STATE_ERROR;
                    LOGI(TAG,"[onSeekComplete]mStateBac = STATE_ERROR.");
                }

                mStateBac = STATE_STOP;
            }

            //start update progress bar
            mSeekState = SEEK_END;
            if(mHandler != null) {
                mHandler.removeMessages(MSG_UPDATE_PROGRESS);
                Message msg = mHandler.obtainMessage(MSG_UPDATE_PROGRESS);
                mHandler.sendMessageDelayed(msg, MSG_SEND_DELAY+1000);
            }

            if(randomSeekEnable()) {
                if(randomSeekTestFlag) {
                    randomSeekTest();
                }
            }
        }
    };

    private MediaPlayer.OnErrorListener mErrorListener =
        new MediaPlayer.OnErrorListener() {
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log.e(TAG, "Error: " + what + "," + extra);
            mErrorTime = java.lang.System.currentTimeMillis();
            int offset = (int)(mErrorTime - mErrorTimeBac);
            //Log.e(TAG, "[onError]mErrorTime:" + mErrorTime + ",mErrorTimeBac:" + mErrorTimeBac + ", offset:" + offset);
            if(offset > 200) {
                mState = STATE_ERROR;
                mErrorTimeBac = mErrorTime;
                String infoStr = mErrorInfo.getErrorInfo(what, mPlayList.getcur());
                Toast toast =Toast.makeText(VideoPlayer.this, "Status Error:"+infoStr,Toast.LENGTH_SHORT );
                toast.setGravity(Gravity.BOTTOM,/*110*/0,0);
                toast.setDuration(0x00000001);
                toast.show();

                if(mOption.getRepeatMode() == mOption.REPEATLIST) {
                    playNext();
                }
                else {
                    browserBack();
                }
            }
            return true;
        }
    };

    private final int DTS_NOR = 0;
    private final int DTS_EXPRESS = 1;
    private final int DTS_HD_MASTER_AUDIO = 2;
    private int mDtsType = DTS_NOR;
    private MediaPlayer.OnInfoListener mInfoListener =
        new MediaPlayer.OnInfoListener() {
        public  boolean onInfo(MediaPlayer mp, int arg1, int arg2) {
            LOGI(TAG, "[onInfo] mp: " + mp + ",arg1:" + arg1 + ",arg2:"+arg2);
            if (mp != null) {
                boolean needShow= MediaInfo.needShowOnUI(arg1);
                LOGI(TAG, "[onInfo] needShow: " + needShow);
                if(needShow == true) {
                    String infoStr = MediaInfo.getInfo(arg1, VideoPlayer.this);
                    LOGI(TAG, "[onInfo] infoStr: " + infoStr);
                    Toast toast =Toast.makeText(VideoPlayer.this, /*"Media Info:"+*/infoStr,Toast.LENGTH_SHORT );
                    toast.setGravity(Gravity.BOTTOM,/*110*/0,0);
                    toast.setDuration(0x00000001);
                    toast.show();
                }

                if(arg1 == mMediaInfo.MEDIA_INFO_AMLOGIC_SHOW_DTS_ASSET) {
                    if(getDtsApresentTotalNum() > 0) {
                        showDtsAseetFromInfoLis = true;
                        audioDtsApresentSelect();
                    }
                }
                else if(arg1 == mMediaInfo.MEDIA_INFO_AMLOGIC_SHOW_DTS_EXPRESS) {
                    mDtsType = DTS_EXPRESS;
                    showCertification();
                }
                else if(arg1 == mMediaInfo.MEDIA_INFO_AMLOGIC_SHOW_DTS_HD_MASTER_AUDIO) {
                    mDtsType = DTS_HD_MASTER_AUDIO;
                    showCertification();
                }
                else if(arg1 == mMediaInfo.MEDIA_INFO_AMLOGIC_SHOW_AUDIO_LIMITED) {
                    String ainfoStr = getResources().getString(R.string.audio_dec_enable);
                    Toast toast =Toast.makeText(VideoPlayer.this, ainfoStr, Toast.LENGTH_SHORT );
                    toast.setGravity(Gravity.BOTTOM,/*110*/0,0);
                    toast.setDuration(0x00000001);
                    toast.show();
                }
				else if(arg1 == mMediaInfo.MEDIA_INFO_AMLOGIC_SHOW_DTS_MULASSETHINT) {
                    Toast toast =Toast.makeText(VideoPlayer.this, "MulAssetAudio",Toast.LENGTH_SHORT );
                    toast.setGravity(Gravity.BOTTOM,/*110*/0,0);
                    toast.setDuration(0x00000001);
                    toast.show();
                }
                else if(arg1 == mMediaInfo.MEDIA_INFO_AMLOGIC_SHOW_DTS_HPS_NOTSUPPORT) {
                    String dtshpsUnsupportStr = getResources().getString(R.string.dts_hps_unsupport);
                    Toast toast =Toast.makeText(VideoPlayer.this, dtshpsUnsupportStr, Toast.LENGTH_SHORT );
                    toast.setGravity(Gravity.BOTTOM,/*110*/0,110);
                    toast.setDuration(0x00000001);
                    toast.show();
                }
            }
            return true;
        }
    };

    private MediaPlayer.OnTimedTextListener mTimedTextListener =
        new MediaPlayer.OnTimedTextListener() {
        public void onTimedText(MediaPlayer mp, TimedText text) {
            LOGI(TAG, "[onTimedText]text:"+text);
            if(text!=null) {
                LOGE(TAG, "[onTimedText]text:"+text+", text.getText():"+text.getText());
                isShowImgSubtitle = false;
                subtitleTV.setText(text.getText());
                //@@//subtitleTV.setTextColor(0xFF990066);
                //@@//subtitleTV.setTypeface(null,Typeface.BOLD);
            }
            else {
                LOGE(TAG, "[onTimedText]text:"+text);
                subtitleTV.setText("");
            }
        }
    };

    private MediaPlayer.OnSubtitleDataListener mSubtitleDataListener =
        new MediaPlayer.OnSubtitleDataListener() {
        public void onSubtitleData(MediaPlayer mp, SubtitleData data) {
            LOGI(TAG, "[onSubtitleData]data:"+data);
			if (null == subtitleIV) {
				return;
			}
            if (data!=null) {
                if (View.INVISIBLE == subtitleIV.getVisibility()) {
                    subtitleIV.setVisibility(View.VISIBLE);
                }
                //if (data.getType() == 0/*data.TYPE_STRING*/) {
                //    try {
                //        String subStr = new String((byte[])data.getData(),"UTF8");
                //        LOGE(TAG, "[onSubtitleData]data:"+data+", subStr:"+subStr);
                //        subtitleTV.setText(subStr);
                //   }catch (UnsupportedEncodingException e) {
                //        e.printStackTrace();
                //    }
                //}
                //else if (data.getType() == 1/*data.TYPE_PICTURE*/) {
                    ///Bitmap bm = null;
                    ///bm = Bitmap.createBitmap ((int[])data.getBitmapData(), 
                    ///                       data.getWidth(), 
                    ///                        data.getHeight(),
                    ///                        Config.ARGB_8888);
                    ///subtitleIV.setImageBitmap(bm);
                //}

                isShowImgSubtitle = true;
                Bitmap bm = data.getBitmap();
                subtitleIV.setImageBitmap(bm);
            }
            else {
                LOGE(TAG, "[onSubtitleData]data:"+data);
                //subtitleTV.setText("");
                if (View.GONE != subtitleIV.getVisibility()) {
                    subtitleIV.setVisibility(View.INVISIBLE);
                }
            }
        }
    };

    private MediaPlayer.OnBlurayListener mBlurayListener = new MediaPlayer.OnBlurayListener() {
        @Override
        public void onBlurayInfo(MediaPlayer mp, int arg1, int arg2, Object obj) {
            LOGI (TAG, "[onBlurayInfo] mp: " + mp + ",arg1:" + arg1 + ",arg2:" + arg2);
            if (mp == null)
                return;

            if (arg1 == MediaInfo.MEDIA_INFO_AMLOGIC_BLURAY_STREAM_PATH) {
                if (obj instanceof Parcel) {
                    Parcel parcel = (Parcel)obj;
                    String path = parcel.readString();
                    int streamNum = parcel.readInt();
                    mBlurayVideoLang.clear();
                    mBlurayAudioLang.clear();
                    mBluraySubLang.clear();
                    for (int i = 0; i < streamNum; i++) {
                        int type = parcel.readInt();
                        String lang = parcel.readString();
                        LOGI(TAG, "[onBlurayInfo]sub[" + i + "] type(" + type + ") lang: " + lang);
                        switch (type) {
                            case MediaInfo.BLURAY_STREAM_TYPE_VIDEO:
                                mBlurayVideoLang.add(lang);
                                break;
                            case MediaInfo.BLURAY_STREAM_TYPE_AUDIO:
                                mBlurayAudioLang.add(lang);
                                break;
                            case MediaInfo.BLURAY_STREAM_TYPE_SUB:
                                mBluraySubLang.add(lang);
                                break;
                            default:
                                break;
                        }
                    }
                    int chapterNum = parcel.readInt();
                    mBlurayChapter.clear();
                    for (int i = 0; i < chapterNum; i++) {
                        int start = parcel.readInt();
                        int duration = parcel.readInt();
                        LOGI(TAG, "chapter[" + i + "]: start(" + start + ") duration(" + duration + ")");
                        ChapterInfo info = new ChapterInfo();
                        info.start = start;
                        info.duration = duration;
                        mBlurayChapter.add(info);
                    }
                    progressBar.setEnabled(true);
                    parcel.recycle();
                    mMediaPlayer.subtitleOpen(path);
                }
            }
        }
    };

    //@@--------this part for book mark play-------------------------------------------------------------------
    private AlertDialog confirm_dialog = null;
    private int bmPos = 0; // book mark postion
    private int resumeSecondMax = 8; //resume max second 8s
    private int resumeSecond = resumeSecondMax;
    private static final int MSG_COUNT_DOWN = 0xE1;//random value
    private boolean exitAbort = false; //indicate exit with abort
    private int bmPlay(String path) {
        bmPos = 0; //reset value for bmPos
        final int pos = mBookmark.get(path);
        if(pos > 0) {
            confirm_dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.setting_resume)  
                .setMessage(R.string.str_resume_play) 
                .setPositiveButton(R.string.str_ok,  
                    new DialogInterface.OnClickListener() {  
                        public void onClick(DialogInterface dialog, int whichButton) {  
                        bmPos = pos;
                    }  
                })  
                .setNegativeButton(VideoPlayer.this.getResources().getString(R.string.str_cancel) + " ( "+resumeSecond+" )",  
                    new DialogInterface.OnClickListener() {  
                        public void onClick(DialogInterface dialog, int whichButton) {  
                        bmPos = 0;
                    }  
                })  
                .show(); 
            confirm_dialog.setOnDismissListener(new confirmDismissListener());
            ResumeCountdown();
            return pos;
        }
        else {
            setVideoPath(path);
        }
        LOGI(TAG, "[bmPlay]pos is :"+pos);
        return pos;
    }

    protected void ResumeCountdown() {
        final Handler handler = new Handler(){   	  
            public void handleMessage(Message msg) {   
                switch (msg.what) {       
                    case MSG_COUNT_DOWN:
                    if(confirm_dialog.isShowing()) {
                        if(resumeSecond > 0) {
                            String cancel = VideoPlayer.this.getResources().getString(R.string.str_cancel);
                            confirm_dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                                .setText(cancel+" ( "+(--resumeSecond)+" )");
                            ResumeCountdown();
                        }
                        else {
                            bmPos = 0;
                            confirm_dialog.dismiss();
                            resumeSecond = resumeSecondMax;
                        }
                    }
                    break;       
                }       
                super.handleMessage(msg);   
            }  
        };

        TimerTask task = new TimerTask(){   
            public void run() {   
                Message message = Message.obtain();
                message.what = MSG_COUNT_DOWN;       
                handler.sendMessage(message);     
            }   
        };   
        
        Timer resumeTimer = new Timer();
        resumeTimer.schedule(task, 1000);
    }
	
    private class confirmDismissListener implements DialogInterface.OnDismissListener {
        public void onDismiss(DialogInterface arg0) {
            if(!exitAbort) {
                setVideoPath(mPlayList.getcur());
                resumeSecond = resumeSecondMax;
            }
        }
    }

    //@@--------this part for slow down switching next or previous file frequency--------------------------------------------------------
    private Timer switchtimer = new Timer();
    private static final int MSG_SWITCH_TIME_OUT = 0xc1;
    private final int SWITCH_WAIT_TIME = 500; // switch file timeout
    private boolean mSwitchEnable = true;
    protected void startSwitchTimeout() {
        final Handler handler = new Handler() {   
            public void handleMessage(Message msg) {   
                switch (msg.what) {       
                    case MSG_SWITCH_TIME_OUT: 
                        setSwitchEnable(true);
                    break;       
                }       
                super.handleMessage(msg);   
            }
        };
        
        TimerTask task = new TimerTask() {   
            public void run() {   
                Message message = Message.obtain();
                message.what = MSG_SWITCH_TIME_OUT;       
                handler.sendMessage(message);    
            }
        }; 

        stopSwitchTimeout();
        setSwitchEnable(false);
        if(switchtimer == null) {
            switchtimer = new Timer();
        }
        if(switchtimer != null) {
            switchtimer.schedule(task, SWITCH_WAIT_TIME);
        }
    }

    private void stopSwitchTimeout() {
        if(switchtimer != null) {
            switchtimer.cancel();
            switchtimer = null;
            setSwitchEnable(true);
        }
    }

    private void setSwitchEnable(boolean enable) {
        mSwitchEnable = enable;
    }

    private boolean getSwitchEnable() {
        return mSwitchEnable;
    }

    //@@--------this part for control bar, option bar, other widget, sub widget and info widget showing or not--------------------------------
    private Timer timer = new Timer();
    private static final int MSG_OSD_TIME_OUT = 0xd1;
    private static final int OSD_CTL_BAR = 0;
    private static final int OSD_OPT_BAR = 1;   
    private static final int OSD_CHILD = 2;
    private int curOsdViewFlag = -1;
    private final int OSD_FADE_TIME = 5000; // osd showing timeout

    private final int RESUME_MODE = 0;
    private final int REPEAT_MODE = 1;
    private final int AUDIO_OPTION = 2;
    private final int AUDIO_TRACK = 3;
    private final int SOUND_TRACK = 4;
    private final int AUDIO_DTS_APRESENT = 5;
    private final int AUDIO_DTS_ASSET = 6;
    private final int DISPLAY_MODE = 7;
    private final int BRIGHTNESS = 8;
    private final int PLAY3D = 9;
    private final int VIDEO_TRACK = 10;
    private final int CHAPTER_MODE = 11;
    private int otherwidgetStatus = 0;
	
    protected void startOsdTimeout() {
        final Handler handler = new Handler() {   
            public void handleMessage(Message msg) {   
                switch (msg.what) {       
                    case MSG_OSD_TIME_OUT: 
                        showNoOsdView();
                    break;       
                }       
                super.handleMessage(msg);   
            }
        };  
        
        TimerTask task = new TimerTask() {   
            public void run() {   
                //@@if(!touchVolFlag) {
                    Message message = Message.obtain();
                    message.what = MSG_OSD_TIME_OUT;       
                    handler.sendMessage(message);     
                //@@}   
            }
        }; 

        stopOsdTimeout();
        if(timer == null) {
            timer = new Timer();
        }
        if(timer != null) {
            timer.schedule(task, OSD_FADE_TIME);
        }
    }

    private void stopOsdTimeout() {
        if(timer!=null)
            timer.cancel();
            timer = null;
    }

    private int getCurOsdViewFlag() {
        LOGI(TAG,"[getCurOsdViewFlag]curOsdViewFlag:"+curOsdViewFlag);
        return curOsdViewFlag;
    }

    private void setCurOsdViewFlag(int osdView) {
        curOsdViewFlag = osdView;
    }

    private void showOtherWidget(int StrId) {
        if(null!=otherwidget) {
            if(View.GONE==otherwidget.getVisibility()) {
                otherwidget.setVisibility(View.VISIBLE);
                if ((null!=optbar)&&(View.VISIBLE==optbar.getVisibility()))
                    optbar.setVisibility(View.GONE);
                if ((null!=ctlbar)&&(View.VISIBLE==ctlbar.getVisibility()))
                    ctlbar.setVisibility(View.GONE);
            }
            otherwidgetTitleTx.setText(StrId);
            otherwidget.requestFocus();
            otherwidgetStatus = StrId;
            stopOsdTimeout();
        }
    }

    private void showSubWidget(int StrId) {
        if((null!=subwidget)&&(View.GONE==subwidget.getVisibility())) {
            subwidget.setVisibility(View.VISIBLE);
            setCurOsdViewFlag(OSD_CHILD);
            if ((null!=optbar)&&(View.VISIBLE==optbar.getVisibility()))
                optbar.setVisibility(View.GONE);
            subwidget.requestFocus();
            otherwidgetStatus = StrId;
            stopOsdTimeout();
        }
    }

    private void showInfoWidget(int StrId) {
        TextView title;
        if((null!=infowidget)&&(View.GONE==infowidget.getVisibility())) {
            infowidget.setVisibility(View.VISIBLE);
            if ((null!=optbar)&&(View.VISIBLE==optbar.getVisibility()))
                optbar.setVisibility(View.GONE);
            title = (TextView)findViewById(R.id.info_title);
            title.setText(R.string.str_file_information);
            otherwidgetStatus = StrId;
            infowidget.requestFocus();
            stopOsdTimeout();
        }
    }

    private void exitOtherWidget(ImageButton btn) {
        if((null!=otherwidget)&&(View.VISIBLE==otherwidget.getVisibility())) {
            otherwidget.setVisibility(View.GONE);
            if (mIsBluray && (mListViewHeight != ViewGroup.LayoutParams.WRAP_CONTENT)) {
                setListViewHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            if ((null!=optbar)&&(View.GONE==optbar.getVisibility()))
                optbar.setVisibility(View.VISIBLE);
            btn.requestFocus();
            btn.requestFocusFromTouch();
            startOsdTimeout();
        }
    }

    private void exitSubWidget(ImageButton btn) {
        if((null!=subwidget)&&(View.VISIBLE==subwidget.getVisibility())) {
            subwidget.setVisibility(View.GONE);
            setCurOsdViewFlag(OSD_OPT_BAR);
            if ((null!=optbar)&&(View.GONE==optbar.getVisibility()))
                optbar.setVisibility(View.VISIBLE);
            btn.requestFocus();
            btn.requestFocusFromTouch();
            startOsdTimeout();
        }
    }

    private void exitInfoWidget(ImageButton btn) {
        if((null!=infowidget)&&(View.VISIBLE==infowidget.getVisibility())) {
            infowidget.setVisibility(View.GONE);
            if ((null!=optbar)&&(View.GONE==optbar.getVisibility()))
                optbar.setVisibility(View.VISIBLE);
            btn.requestFocus();
            btn.requestFocusFromTouch();
            startOsdTimeout();
        }
    }

    private void showCtlBar()
    {
        LOGI(TAG,"[showCtlBar]ctlbar:"+ctlbar+",ctlbar.getVisibility():"+ctlbar.getVisibility());
        LOGI(TAG,"[showCtlBar]optbar:"+optbar+",optbar.getVisibility():"+optbar.getVisibility());
        if ((null!=ctlbar)&&(View.GONE==ctlbar.getVisibility())) {
            //@@getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            if ((null!=optbar)&&(View.VISIBLE==optbar.getVisibility()))
                optbar.setVisibility(View.GONE);
            if ((null!=subwidget)&&(View.VISIBLE==subwidget.getVisibility()))
                subwidget.setVisibility(View.GONE);
            if ((null != otherwidget) && (View.VISIBLE == otherwidget.getVisibility())) {
                otherwidget.setVisibility (View.GONE);
                if (mIsBluray && (mListViewHeight != ViewGroup.LayoutParams.WRAP_CONTENT)) {
                    setListViewHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                }
            }
            if ((null!=infowidget)&&(View.VISIBLE==infowidget.getVisibility()))
                infowidget.setVisibility(View.GONE);

            ctlbar.setVisibility(View.VISIBLE);
            ctlbar.requestFocus();
            //ctlbar.requestFocusFromTouch();
            //optBtn.requestFocus();
            //optBtn.requestFocusFromTouch();
            setCurOsdViewFlag(OSD_CTL_BAR);
        }

        startOsdTimeout();
        updateProgressbar();
    }

    private void showOptBar() {
        if ((null!=optbar)&&(View.GONE==optbar.getVisibility())) {
            //@@getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

            if ((null!=ctlbar)&&(View.VISIBLE==ctlbar.getVisibility()))
                ctlbar.setVisibility(View.GONE);
            if ((null!=subwidget)&&(View.VISIBLE==subwidget.getVisibility()))
                subwidget.setVisibility(View.GONE);
            if ((null != otherwidget) && (View.VISIBLE == otherwidget.getVisibility())) {
                otherwidget.setVisibility (View.GONE);
                if (mIsBluray && (mListViewHeight != ViewGroup.LayoutParams.WRAP_CONTENT)) {
                    setListViewHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                }
            }
            if ((null!=infowidget)&&(View.VISIBLE==infowidget.getVisibility()))
                infowidget.setVisibility(View.GONE);

            optbar.setVisibility(View.VISIBLE);
            optbar.requestFocus();
            //optbar.requestFocusFromTouch();
            //ctlBtn.requestFocus();
            //ctlBtn.requestFocusFromTouch();
            setCurOsdViewFlag(OSD_OPT_BAR);
        }
        startOsdTimeout();
    }

    private void showNoOsdView() {
        stopOsdTimeout();
        if ((null!=ctlbar)&&(View.VISIBLE==ctlbar.getVisibility()))
            ctlbar.setVisibility(View.GONE);
        if ((null!=optbar)&&(View.VISIBLE==optbar.getVisibility()))
            optbar.setVisibility(View.GONE);
        if ((null!=subwidget)&&(View.VISIBLE==subwidget.getVisibility()))
            subwidget.setVisibility(View.GONE);
        if ((null != otherwidget) && (View.VISIBLE == otherwidget.getVisibility())) {
            otherwidget.setVisibility (View.GONE);
            if (mIsBluray && (mListViewHeight != ViewGroup.LayoutParams.WRAP_CONTENT)) {
                setListViewHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
        if ((null!=infowidget)&&(View.VISIBLE==infowidget.getVisibility()))
            infowidget.setVisibility(View.GONE);
        showSystemUi(false);
        //@@getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
        //@@WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // update msg sformat
        msgSFormatUpdate();  // wxl add 20141104
    }

    private void showOsdView() {
        LOGI(TAG,"[showOsdView]");
        if(null==ctlbar)
            return;
        if(null==optbar)
            return;
		if (OSD_CHILD == getCurOsdViewFlag()) 
            return;

        int flag = getCurOsdViewFlag();
        LOGI(TAG,"[showOsdView]flag:"+flag);
        switch(flag) {
            case OSD_CTL_BAR:
                showCtlBar();
            break;

            case OSD_OPT_BAR:
                showOptBar();
            break;

            default:
                LOGE(TAG,"[showOsdView]getCurOsdView error flag:"+flag+",set CurOsdView default");
                showCtlBar();
            break;
        }
        showSystemUi(true);
    }

    private void switchOsdView() {
        if(null==ctlbar)
            return;
        if(null==optbar)
            return;

        int flag = getCurOsdViewFlag();
        switch(flag) {
            case OSD_CTL_BAR:
                showOptBar();
            break;

            case OSD_OPT_BAR:
                showCtlBar();
            break;

            default:
                LOGE(TAG,"[switchOsdView]getCurOsdView error flag:"+flag+",set CurOsdView default");
                showCtlBar();
            break;
        }
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void showSystemUi(boolean visible) {
        LOGI(TAG,"[showSystemUi]visible:"+visible+",mSurfaceView:"+mSurfaceView);
        if(mSurfaceView == null) {
            return;
        }
        
        int flag = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if (!visible) {
        // We used the deprecated "STATUS_BAR_HIDDEN" for unbundling
        flag |= View.STATUS_BAR_HIDDEN | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
        mSurfaceView.setSystemUiVisibility(flag);
    }

    private int mLastSystemUiVis = 0;
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void setOnSystemUiVisibilityChangeListener() {
        //if (!ApiHelper.HAS_VIEW_SYSTEM_UI_FLAG_HIDE_NAVIGATION) return;
        mSurfaceView.setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                LOGI(TAG,"[onSystemUiVisibilityChange]visibility:"+visibility+",mLastSystemUiVis:"+mLastSystemUiVis);
                int diff = mLastSystemUiVis ^ visibility;
                mLastSystemUiVis = visibility;
                LOGI(TAG,"[onSystemUiVisibilityChange]diff:"+diff);
                if ((diff & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
                        && (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                    showOsdView();
                }
            }
        });
    }

    //@@--------this part for showing massage on OSD View ----------------------------------------------------
    private String mLoadingStr = "";
    private boolean CpuloadUpdating = false;
    final Handler updateCpuSetTextHandler = new Handler();
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
            //msgCpuloading
            LOGI(TAG, "[mUpdateResults]mLoadingStr:"+mLoadingStr);
            //if(!mLoadingStr.equals("0%") && !mLoadingStr.equals("1%") && !mLoadingStr.equals("100%")) {
                String fileinf = null;
                fileinf = VideoPlayer.this.getResources().getString(R.string.str_msg_cpuloading)
                    + " : " + mLoadingStr;
                VideoPlayer.this.msgCpuloading.setText(fileinf);
            //}
        }
    };

    final Handler updateCpuLoadingHandler = new Handler();   
    Runnable updateCpuRunnable = new Runnable(){ 
    public void run(){   
            showCpuloading();
            updateCpuLoadingHandler.postDelayed(updateCpuRunnable, 1000);  
        }   
    }; 

    private void startShowCpuloading() {    
        LOGI(TAG, "startShowCpuloading");    
        updateCpuLoadingHandler.post(updateCpuRunnable);   
    }

    private void stopShowCpuloading() {    
        LOGI(TAG, "stopShowCpuloading");    
        updateCpuLoadingHandler.removeCallbacks(updateCpuRunnable);   
    }

    //pcpu =100* (total-idle)/total
    //user, nice, system, idle, iowait, irq, softirq, stealstolen, guest
    //CPUusr[0]:cpu
    //CPUusr[1]:
    //CPUusr[2]:1301238
    //CPUusr[3]:19889
    //CPUusr[4]:493838
    //CPUusr[5]:6997783
    //CPUusr[6]:8083
    //CPUusr[7]:0
    //CPUusr[8]:15361
    //CPUusr[9]:0
    //CPUusr[10]:0
    //CPUusr[11]:0
    
    private long userL = 0;             //user last
    private long niceL = 0;             //nice last
    private long systemL = 0;           //system last
    private long idleL = 0;             //idle last
    private long iowaitL = 0;           //iowait last
    private long irqL = 0;              //irq last
    private long softirqL = 0;          //softirq last
    private long stealstolenL = 0;      //stealstolen last
    private long guestL = 0;            //guest last
    private long nonameL = 0;           //noname last
    
    private long userC = 0;             //user current
    private long niceC = 0;             //nice current
    private long systemC = 0;           //system current
    private long idleC = 0;             //idle current
    private long iowaitC = 0;           //iowait current
    private long irqC = 0;              //irq current
    private long softirqC = 0;          //softirq current
    private long stealstolenC = 0;      //stealstolen current
    private long guestC = 0;            //guest current
    private long nonameC = 0;           //noname current

    private long totalL = 0;
    private long totalC = 0;
    private int totalCpuTime = 0;
    private int idleCpuTime = 0;
    private int pCpu = 0;
    
    private String getCpuLoading() {
        String result;
        String loadingStr = "";
        try {
            Process p = Runtime.getRuntime().exec("cat /proc/stat");
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while((result = br.readLine()) != null)
            {
                if(result.trim().length() < 1){
                    continue;
                }
                else{
                    String[] cpudata = result.split(" ");
                    //for(int i = 0; i < cpudata.length; i++) {
                        //LOGE(TAG, "[getCpuLoading]cpudata[" + i +"]:"+cpudata[i].trim());
                    //}
                    
                    // read current cpu data
                    /*userC = cpudata[2].trim();
                    niceC = cpudata[3].trim();
                    systemC = cpudata[4].trim();
                    idleC = cpudata[5].trim();
                    iowaitC = cpudata[6].trim();
                    irqC = cpudata[7].trim();
                    softirqC = cpudata[8].trim();
                    stealstolenC = cpudata[9].trim();
                    guestC = cpudata[10].trim();
                    nonameC = cpudata[11].trim();*/

                    userC = (long)Integer.parseInt(cpudata[2].trim());
                    niceC = (long)Integer.parseInt(cpudata[3].trim());
                    systemC = (long)Integer.parseInt(cpudata[4].trim());
                    idleC = (long)Integer.parseInt(cpudata[5].trim());
                    iowaitC = (long)Integer.parseInt(cpudata[6].trim());
                    irqC = (long)Integer.parseInt(cpudata[7].trim());
                    softirqC = (long)Integer.parseInt(cpudata[8].trim());
                    stealstolenC = (long)Integer.parseInt(cpudata[9].trim());
                    guestC = (long)Integer.parseInt(cpudata[10].trim());
                    nonameC = (long)Integer.parseInt(cpudata[11].trim());
                    
                    totalC = userC + niceC + systemC + idleC + iowaitC + irqC + softirqC + stealstolenC + guestC + nonameC;
                    totalL = userL+ niceL + systemL + idleL + iowaitL + irqL + softirqL + stealstolenL + guestL + nonameL;
                    totalCpuTime = (int) (totalC - totalL);
                    idleCpuTime = (int) (idleC - idleL);
                    pCpu = 100* (totalCpuTime - idleCpuTime) / totalCpuTime;
                    if(pCpu < 0) {
                        pCpu = 0;
                    }
                    loadingStr = Integer.toString(pCpu) + "%";

                    // save current cpu data
                    userL = userC;
                    niceL = niceC;
                    systemL = systemC;
                    idleL = idleC;
                    iowaitL = iowaitC;
                    irqL = irqC;
                    softirqL = softirqC;
                    stealstolenL = stealstolenC;
                    guestL = guestC;
                    nonameL = nonameC;
                    break;
                }
            }
        } catch (IOException e) {				
			LOGE(TAG, "IOException: " + e.toString());
		}	
        return loadingStr;
    }

    private void showTime() {
        if((null != msgwidget) && (View.GONE == msgwidget.getVisibility())) {
            return;
        }
        String fileinf = null;
        LOGI(TAG, "[showTime]");
        
        //msgTime
        curTimeTx.setText(secToTime(curtime/1000));
        totalTimeTx.setText(secToTime(totaltime/1000));
        String ratioStr = "0%";
        if(totaltime != 0) {
            ratioStr = Integer.toString((curtime * 100) / totaltime) + "%";
        }
        fileinf = VideoPlayer.this.getResources().getString(R.string.str_msg_time)
            + " : " + secToTime(curtime/1000) + "/" + secToTime(totaltime/1000) + "(" + ratioStr + ")";
        msgTime.setText(fileinf);
    }

    private void showBitrate() {
        if((null != msgwidget) && (View.GONE == msgwidget.getVisibility())) {
            return;
        }
        String fileinf = null;
        int bitrate = 0;
        String bitrateStr = "NULL";
        LOGI(TAG, "[showBitrate]");
        
        bitrate = mMediaInfo.getBitrate();
        if(bitrate > 0 && bitrate < 1000) {
            bitrateStr = Integer.toString(bitrate) + "bps";
        }
        else if(bitrate >= 1000 && bitrate < (1000 * 1000)) {
            bitrate = bitrate / 1000;
            bitrateStr = Integer.toString(bitrate) + "Kbps";
        }
        else if(bitrate >= (1000 * 1000) && bitrate < (1000 * 1000 * 1000)) {
            bitrate = bitrate / (1000 * 1000);
            bitrateStr = Integer.toString(bitrate) + "Mbps";
        }
        else if(bitrate >= (1000 * 1000 * 1000) && bitrate < (1000 * 1000 * 1000 * 1000)) {
            bitrate = bitrate / (1000 * 1000 * 1000);
            bitrateStr = Integer.toString(bitrate) + "Gbps";
        }

        fileinf = VideoPlayer.this.getResources().getString(R.string.str_msg_bitrate)
            + " : " + bitrateStr;
        msgBitrate.setText(fileinf);
    }
    
    private void showCpuloading() {
        if((null != msgwidget) && (View.GONE == msgwidget.getVisibility())) {
            return;
        }
        LOGI(TAG, "[showCpuloading]");

        new Thread("showCpuloadingThread") {
            @Override
            public void run() {   
                mLoadingStr = getCpuLoading();
                updateCpuSetTextHandler.post(mUpdateResults);
                CpuloadUpdating = false;
            }

        }.start(); 
    }

    private void showAFormat() {
        if((null != msgwidget) && (View.GONE == msgwidget.getVisibility())) {
            return;
        }
        String fileinf = null;
        LOGI(TAG, "[showAFormat]");

        //msgAFormat
        int track = mOption.getAudioTrack();
        if(track < 0) {
            track = 0;
        }
        else if(track >= mMediaInfo.getAudioTotalNum()) {
            track = mMediaInfo.getAudioTotalNum() - 1;
        }
        fileinf = VideoPlayer.this.getResources().getString(R.string.str_msg_aformat)
            + " : " + mMediaInfo.getAudioFormatStr(mMediaInfo.getAudioFormat(track));
        msgAFormat.setText(fileinf);
    }
    
    private void showSFormat() {
        if((null != msgwidget) && (View.GONE == msgwidget.getVisibility())) {
            return;
        }
        String fileinf = null;
        int total = 0;
        LOGI(TAG, "[showSFormat] sub_para:"+sub_para);

        //msgSFormat
        String sformat = "NULL";
        if (!isTimedTextDisable()) {
            total = getSubtitleTotal();
        }
        else {
            if(mMediaPlayer != null) {
                total = mMediaPlayer.subtitleTotal();
            }
        }
        if(sub_para != null && total > 0) {
            if(sub_para.curid != total) {
                sformat = mMediaInfo.getSFormat(sub_para.curid);
            }
        }
        fileinf = VideoPlayer.this.getResources().getString(R.string.str_msg_sformat)
            + " : " + sformat;
        msgSFormat.setText(fileinf);
    }

    /*private String getCpuLoading() {
        String result;
        String loadingStr = "";
        try {
            Process p = Runtime.getRuntime().exec("top -n 1 -m 1");
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while((result = br.readLine()) != null)
            {
                if(result.trim().length() < 1){
                    continue;
                }
                else{
                    String[] CPUusr = result.split("%");
                    //tv.append("USER:"+CPUusr[0]+"\n");
                    String[] CPUusage = CPUusr[0].split("User");
                    String[] SYSusage = CPUusr[1].split("System");
                    loadingStr = CPUusage[1].trim() + "%";
                    LOGI(TAG, "[getCpuLoading]CPUusage[1]:" + CPUusage[1].trim());
                    //tv.append("CPU:"+CPUusage[1].trim()+" length:"+CPUusage[1].trim().length()+"\n");
                    //tv.append("SYS:"+SYSusage[1].trim()+" length:"+SYSusage[1].trim().length()+"\n");
                    break;
                }
            }
        } catch (IOException e) {				
			LOGE(TAG, "IOException: " + e.toString());
		}	
        return loadingStr;
    }*/

    private void msgTimeUpdate() {
        LOGI(TAG, "[msgTimeUpdate]");
        showTime();

        /*
        int curtimeS = curtime/1000;
        if((curtimeS % 2) == 0) {
            if(!CpuloadUpdating) {
                CpuloadUpdating = true;
                msgCpuloadingUpdate();
            }
        }*/
    }

    private void msgCpuloadingUpdate() {
        LOGI(TAG, "[msgCpuloadingUpdate]");
        showCpuloading();
    }

    private void msgAFormatUpdate() {
        LOGI(TAG, "[msgAFormatUpdate]");
        showAFormat();
    }

    private void msgSFormatUpdate() {
        LOGI(TAG, "[msgSFormatUpdate]");
        showSFormat();
    }
    
    private void showMsgWidget() {
        if(!sw.getPropertyBoolean("sys.videoplayer.msgshow",false)) {
            return;
        }
        
        if((null != msgwidget) && (View.GONE == msgwidget.getVisibility())) {
            msgwidget.setVisibility(View.VISIBLE);
        }
    }

    private void exitMsgWidget() {
        if((null != msgwidget) && (View.VISIBLE == msgwidget.getVisibility())) {
            msgwidget.setVisibility(View.GONE);
        }
    }

    private void msgShow() {
        LOGI(TAG, "[msgShow]");
        if((null != msgwidget) && (View.GONE == msgwidget.getVisibility())) {
            return;
        }
        String fileinf = null;
        
        //msgName
        fileinf = VideoPlayer.this.getResources().getString(R.string.str_msg_name)
            + " : " + mMediaInfo.getFileName(mPlayList.getcur());
        msgName.setText(fileinf);

        //msgResolution
        fileinf = VideoPlayer.this.getResources().getString(R.string.str_file_resolution)
            + " : " + mMediaInfo.getResolution();
        msgResolution.setText(fileinf);

        //msgTime
        showTime();

        //msgCpuloading
        //fileinf = VideoPlayer.this.getResources().getString(R.string.str_msg_cpuloading)
        //    + " : " + "0%";
        //VideoPlayer.this.msgCpuloading.setText(fileinf);
        startShowCpuloading();

        //msgType
        fileinf = VideoPlayer.this.getResources().getString(R.string.str_msg_type)
            + " : " + mMediaInfo.getFileType();
        msgType.setText(fileinf);

        //msgBitrate
        showBitrate();

        //msgFps
        fileinf = VideoPlayer.this.getResources().getString(R.string.str_msg_fps)
            + " : " + mMediaInfo.getFps() + "fps";
        msgFps.setText(fileinf);

        //msgVFormat
        fileinf = VideoPlayer.this.getResources().getString(R.string.str_msg_vformat)
            + " : " + mMediaInfo.getVFormat();
        msgVFormat.setText(fileinf);

        //msgAFormat
        showAFormat();

        //msgSFormat
        showSFormat();
    }

    //@@--------this part for showing certification of Dolby and DTS----------------------------------------------------
    private void showCertification() {
        if(certificationDoblyView == null && certificationDoblyPlusView == null && certificationDTSView == null && certificationDTSExpressView == null && certificationDTSHDMasterAudioView == null)
            return;
        if(mMediaInfo == null) 
            return;
        if(mOption == null)
            return;
        
        closeCertification();
        if(mMediaInfo.getAudioTotalNum() <= 0)
            return;

        int track = mOption.getAudioTrack();
        if(track < 0) {
            track = 0;
        }
        else if(track >= mMediaInfo.getAudioTotalNum()) {
            track = mMediaInfo.getAudioTotalNum() - 1;
        }
        
        int ret = mMediaInfo.checkAudioCertification(mMediaInfo.getAudioFormat(track/*mMediaInfo.getCurAudioIdx()*/));
        LOGI(TAG,"[showCertification]ret:"+ret);
        if(ret == mMediaInfo.CERTIFI_Dolby) {
            if(sw.getPropertyBoolean("ro.platform.support.dolby",false)) {
                certificationDoblyView.setVisibility(View.VISIBLE);
                certificationDoblyPlusView.setVisibility(View.GONE);
                certificationDTSView.setVisibility(View.GONE);
                certificationDTSExpressView.setVisibility(View.GONE);
                certificationDTSHDMasterAudioView.setVisibility(View.GONE);
            }
        }
        else if(ret == mMediaInfo.CERTIFI_Dolby_Plus) {
            if(sw.getPropertyBoolean("ro.platform.support.dolby",false)) {
                certificationDoblyView.setVisibility(View.GONE);
                certificationDoblyPlusView.setVisibility(View.VISIBLE);
                certificationDTSView.setVisibility(View.GONE);
                certificationDTSExpressView.setVisibility(View.GONE);
                certificationDTSHDMasterAudioView.setVisibility(View.GONE);
            }
        }
        else if(ret == mMediaInfo.CERTIFI_DTS) {
            if(sw.getPropertyBoolean("ro.platform.support.dts",false)) {
                certificationDoblyView.setVisibility(View.GONE);
                certificationDoblyPlusView.setVisibility(View.GONE);
                if(mDtsType == DTS_NOR) {
                    certificationDTSView.setVisibility(View.VISIBLE);
                    certificationDTSExpressView.setVisibility(View.GONE);
                    certificationDTSHDMasterAudioView.setVisibility(View.GONE);
                }
                else if(mDtsType == DTS_EXPRESS) {
                    certificationDTSView.setVisibility(View.GONE);
                    certificationDTSExpressView.setVisibility(View.VISIBLE);
                    certificationDTSHDMasterAudioView.setVisibility(View.GONE);
                }
                else if(mDtsType == DTS_HD_MASTER_AUDIO) {
                    certificationDTSView.setVisibility(View.GONE);
                    certificationDTSExpressView.setVisibility(View.GONE);
                    certificationDTSHDMasterAudioView.setVisibility(View.VISIBLE);
                }
            }
        }
        else {
            certificationDoblyView.setVisibility(View.GONE);
            certificationDoblyPlusView.setVisibility(View.GONE);
            certificationDTSView.setVisibility(View.GONE);
            certificationDTSExpressView.setVisibility(View.GONE);
            certificationDTSHDMasterAudioView.setVisibility(View.GONE);
        }
    }

    private void closeCertification() {
        if(certificationDoblyView != null && certificationDoblyPlusView != null && certificationDTSView != null && certificationDTSExpressView != null && certificationDTSHDMasterAudioView != null) {
            if(certificationDoblyView.getVisibility() == View.VISIBLE) {
                certificationDoblyView.setVisibility(View.GONE);
            }
            if(certificationDoblyPlusView.getVisibility() == View.VISIBLE) {
                certificationDoblyPlusView.setVisibility(View.GONE);
            }
            if(certificationDTSView.getVisibility() == View.VISIBLE) {
                certificationDTSView.setVisibility(View.GONE);
            }
            if(certificationDTSExpressView.getVisibility() == View.VISIBLE) {
                certificationDTSExpressView.setVisibility(View.GONE);
            }
            if(certificationDTSHDMasterAudioView.getVisibility() == View.VISIBLE) {
                certificationDTSHDMasterAudioView.setVisibility(View.GONE);
            }
        }
    }

    //@@--------this part for touch and key event-------------------------------------------------------------------
    public boolean onTouchEvent (MotionEvent event) {
        LOGI(TAG,"[onTouchEvent]ctlbar.getVisibility():"+ctlbar.getVisibility()+",event.getAction():"+event.getAction());
        super.onTouchEvent(event);
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            if((ctlbar.getVisibility() == View.VISIBLE) || (optbar.getVisibility() == View.VISIBLE)) {
                showNoOsdView();
            }
            else if((View.VISIBLE==otherwidget.getVisibility())
                    ||(View.VISIBLE==infowidget.getVisibility())
                    ||(View.VISIBLE==subwidget.getVisibility())) {
                showNoOsdView();
            }
            else {
                showOsdView();
            }

            int flag = getCurOsdViewFlag(); 
            if((OSD_CTL_BAR == flag) && ((mState == STATE_PLAYING)||(mState == STATE_SEARCHING))) {
                updateProgressbar();
            }
            intouch_flag = true;
        }
        return true;
    }

    public boolean onKeyUp(int keyCode, KeyEvent msg) {
        LOGI(TAG,"[onKeyUp]keyCode:"+keyCode+",ctlbar.getVisibility():"+ctlbar.getVisibility());
        /*if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            startOsdTimeout();
        }*/
        if(!browserBackDoing && ((ctlbar.getVisibility() == View.VISIBLE) || (optbar.getVisibility() == View.VISIBLE)))
            startOsdTimeout();
        return false;
    }

    public boolean onKeyDown(int keyCode, KeyEvent msg) {
        LOGI(TAG,"[onKeyDown]keyCode:"+keyCode+",ctlbar.getVisibility():"+ctlbar.getVisibility()+",intouch_flag:"+intouch_flag);
        if((ctlbar.getVisibility() == View.VISIBLE) || (optbar.getVisibility() == View.VISIBLE)) {
            if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
                startOsdTimeout();
            }
            else {
                stopOsdTimeout();
            }
        }

        if(intouch_flag){
            if((ctlbar.getVisibility() == View.VISIBLE) || (optbar.getVisibility() == View.VISIBLE)) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP
                    || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                    || keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                    || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    int flag = getCurOsdViewFlag();
                    if(OSD_CTL_BAR==flag) {
                        ctlbar.requestFocusFromTouch();
                    }
                    else if(OSD_OPT_BAR==flag) {
                        optbar.requestFocusFromTouch();
                    }
                    intouch_flag = false;
                }
            }
        }

        if(keyCode == KeyEvent.KEYCODE_DPAD_UP) { // add for progressBar request focus fix bug 87713
            if((getCurOsdViewFlag() == OSD_CTL_BAR) && (ctlbar.getVisibility() == View.VISIBLE)){
                if(progressBar != null) 
                    progressBar.requestFocus();
            }
            else{
                return false;
            }
        }
        else if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if((ctlbar.getVisibility() == View.GONE) && (optbar.getVisibility() == View.GONE)
                    && (otherwidget.getVisibility() == View.GONE) && (infowidget.getVisibility() == View.GONE)
                    && (subwidget.getVisibility() == View.GONE)) {
                LOGI(TAG,"fastBackward pad right");
                if(mCanSeek) 
                    fastBackward();
                return false;
            }
            if (progressBar != null && progressBar.isFocused()) {
                if (progressBar.getProgress() <= 1) {
                    seekTo(0);
                }
                progressBar.setNextFocusLeftId(R.id.SeekBar);
            }
        }
        else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if((ctlbar.getVisibility() == View.GONE) && (optbar.getVisibility() == View.GONE)
                    && (otherwidget.getVisibility() == View.GONE) && (infowidget.getVisibility() == View.GONE)
                    && (subwidget.getVisibility() == View.GONE)) {
                LOGI(TAG,"fastForward pad left");
                if(mCanSeek) 
                    fastForward();
                return false;
            }
        }
        
        if(keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            if((ctlbar.getVisibility() == View.GONE) && (optbar.getVisibility() == View.GONE)) {
                showOsdView();

                int flag = getCurOsdViewFlag();
                if(OSD_CTL_BAR==flag) {
                    playBtn.requestFocusFromTouch();
                    playBtn.requestFocus();
                }
            }
            else {
                showNoOsdView();
            }
        }
        else if (keyCode == KeyEvent.KEYCODE_POWER) {
            if (mState == STATE_PLAYING 
                || mState == STATE_PAUSED
                || mState == STATE_SEARCHING) {
                pause();
                //stop();
            }
        }
        else if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (OSD_CHILD == getCurOsdViewFlag()) {
                setCurOsdViewFlag(OSD_OPT_BAR);
            }        	
            int flag = getCurOsdViewFlag();
            if(OSD_OPT_BAR==flag) {
                if((otherwidget.getVisibility() == View.VISIBLE) 
                    || (infowidget.getVisibility() == View.VISIBLE)
                    || (subwidget.getVisibility() == View.VISIBLE)) {
                    showOsdView();
                    switch(otherwidgetStatus){
                        case R.string.setting_resume:
                            resumeModeBtn.requestFocusFromTouch();
                            resumeModeBtn.requestFocus();
                        break;
                        case R.string.setting_playmode:
                            repeatModeBtn.requestFocusFromTouch();
                            repeatModeBtn.requestFocus();
                        break;
                        case R.string.setting_3d_mode:
                            play3dBtn.requestFocusFromTouch();
                            play3dBtn.requestFocus();
                        break;
                        case R.string.setting_audiooption:
                            audiooptionBtn.requestFocusFromTouch();
                            audiooptionBtn.requestFocus();
                        break;
                        case R.string.setting_audiotrack:
                        case R.string.setting_soundtrack:
                        case R.string.setting_videotrack:
                            audioOption();
                        break;
                        case R.string.setting_audiodtsapresent:
                            if(!showDtsAseetFromInfoLis) {
                                audiotrackSelect();
                            }
                            break;
                        case R.string.setting_audiodtsasset:
                            if(!showDtsAseetFromInfoLis) {
                                audioDtsApresentSelect();
                            }
                        break;
                        case R.string.setting_subtitle:
                            subtitleSwitchBtn.requestFocusFromTouch();
                            subtitleSwitchBtn.requestFocus();
                            break;
                        case R.string.setting_chapter:
                            chapterBtn.requestFocusFromTouch();
                            chapterBtn.requestFocus();
                        break;
                        case R.string.setting_displaymode:
                            displayModeBtn.requestFocusFromTouch();
                            displayModeBtn.requestFocus();
                        break;
                        case R.string.setting_brightness:
                            brigtnessBtn.requestFocusFromTouch();
                            brigtnessBtn.requestFocus();
                        break;
                        case R.string.str_file_name:
                            fileinfoBtn.requestFocusFromTouch();
                            fileinfoBtn.requestFocus();	
                        break;
                        default:
                            optbar.requestFocus();
                        break;
                    }
                }
                else {
                    switchOsdView();
                }
            }
            else if(OSD_CTL_BAR==flag) {
                if ((null != otherwidget) &&(otherwidget.getVisibility() == View.VISIBLE) && (otherwidgetStatus == R.string.setting_3d_mode)) {
                    otherwidget.setVisibility(View.GONE);
                    showOsdView();
                    play3dBtn.requestFocusFromTouch();
                    play3dBtn.requestFocus();
                }
                else {
                    if ((null != ctlbar) && (View.VISIBLE == ctlbar.getVisibility())) {
                        showNoOsdView();
                    }
                    else {
                        browserBack();
                    }
                }
            }
        }
        else if (keyCode == KeyEvent.KEYCODE_MENU) {
            if((ctlbar.getVisibility() == View.VISIBLE)||(optbar.getVisibility() == View.VISIBLE)){
                showNoOsdView();
            }
            else {
                showOsdView();
                int flag = getCurOsdViewFlag();
                if(OSD_CTL_BAR == flag) {
                    playBtn.requestFocusFromTouch();
                    playBtn.requestFocus();
                }
            }
        }
        else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            int flag = getCurOsdViewFlag();
            if(OSD_CTL_BAR==flag){
                showOsdView();
            }
            else if(OSD_OPT_BAR==flag){
                switchOsdView();
            }
            playPause();
            playBtn.requestFocusFromTouch();
            playBtn.requestFocus();
        }
        else if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
            playPrev(); 
        }
        else if(keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
            playNext();    			
        }
        else if(keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            if(mCanSeek) {
                fastForward();
                fastforwordBtn.requestFocusFromTouch();
                fastforwordBtn.requestFocus();
            }
        }
        else if(keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
            if(mCanSeek) {
                fastBackward();
                fastreverseBtn.requestFocusFromTouch();
                fastreverseBtn.requestFocus(); 
            }
        } 
        /*else if (keyCode == KeyEvent.KEYCODE_MUTE) {
            int flag = getCurOsdViewFlag();
            if(OSD_CTL_BAR==flag) {
                showOsdView();
                playBtn.requestFocusFromTouch();
                playBtn.requestFocus();
            }
            else if(OSD_OPT_BAR==flag) {
                if(!(otherwidget.getVisibility() == View.VISIBLE) 
                    && !(infowidget.getVisibility() == View.VISIBLE)
                    && !(subwidget.getVisibility() == View.VISIBLE)){
                    showOsdView();
                }
            }
        }*/
        else if (keyCode == KeyEvent.KEYCODE_F10) {//3D switch
            // TODO: 3D switch
        }
        else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            int flag = getCurOsdViewFlag();
            if(OSD_CTL_BAR==flag){
                showOsdView();
            }
            else if(OSD_OPT_BAR==flag){
                switchOsdView();
            }
            
            if(mState == STATE_PAUSED) {
                start();
            }
            else if(mState == STATE_SEARCHING) {
                stopFWFB();
                start();
            }
            playBtn.requestFocusFromTouch();
            playBtn.requestFocus();
        }
        else if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            int flag = getCurOsdViewFlag();
            if(OSD_CTL_BAR==flag){
                showOsdView();
            }
            else if(OSD_OPT_BAR==flag){
                switchOsdView();
            }
            
            if(mState == STATE_PLAYING) {
                pause();
            }
            playBtn.requestFocusFromTouch();
            playBtn.requestFocus();
        }
        else if(keyCode == KeyEvent.KEYCODE_MUSIC) {
            //sound 0:stereo, 1:left, 2:right
            Log.d(TAG,"1 channel_id:"+channel_id);
            try {
                ChannelSwitch.switchChannel(channel_id);
            } catch(Exception exception) {
                Log.d(TAG,"channel switch exception");
            }
            String soundText[]  = {"stereo","left channel","right channel" };
            channel_display.setText(soundText[channel_id]);
            channel_display.show();
            if(channel_id == 2)
                channel_id = 0;
            else
                channel_id++;
            Log.d(TAG,"2 channel_id:" + channel_id);	    		   		
        }
        else
            return super.onKeyDown(keyCode, msg);
        return true;
    }

    //@@--------this part for subtitle switch wedgit------------------------------------------------------------------
    private subview_set sub_para = null;
    public static final String subSettingStr = "subtitlesetting"; 
    private int sub_switch_state = 0;
    private int sub_font_state = 0;
    private int sub_color_state = 0;
    private int sub_position_v_state = 0;
    private TextView t_subswitch =null ;
    private TextView t_subinfo = null ;
    private TextView t_subsfont=null ;
    private TextView t_subscolor=null ;
    private TextView t_subsposition_v=null;
    private int mSubNum = 0;
    private int mSubOffset = -1;
    private boolean isShowImgSubtitle = false;
    private String mcolor_text[];

    /*private String color_text[]={ 
        VideoPlayer.this.getResources().getString(R.string.color_white),
        VideoPlayer.this.getResources().getString(R.string.color_yellow),
        VideoPlayer.this.getResources().getString(R.string.color_blue)
    };*/

    private static final String[] extensions = {
        "idx",
        "aqt",
        "ass",
        "lrc",
        "smi",
        "sami",
        "txt",
        "srt",
        "ssa",
        "xml",
        "jss",
        "js",
        "mpl",
        "rt",
        "sub",
        /* "may be need add new types--------------" */};

    private int getSubtitleTotal() {
        return mSubNum;
    }

    private boolean idxFileAdded = false;
    private boolean subtitleSkipedSubForIdx(String file, File DirFile) {
        LOGI(TAG,"[subtitleSkipedSubForIdx]idxFileAdded:"+idxFileAdded);
        String fileLow = file.toLowerCase();
        if (fileLow.endsWith("idx")) {
            idxFileAdded = true;
            return false;
        }

        if (fileLow.endsWith("sub")) {
            if (idxFileAdded) {
                return true;
            }
            else {
                if (DirFile.isDirectory()) {
                    for (String filetmp : DirFile.list()) {
                        String filetmpLow = filetmp.toLowerCase();
                        if (filetmpLow.endsWith("idx")) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }

    private void searchExternalSubtitle(String path) {
        LOGI(TAG,"[searchExternalSubtitle]path:"+path);
        if (path != null) {
            idxFileAdded = false;
            File playFile = new File(path);
            String playName=playFile.getName();
            String prefix = playName.substring(0, playName.lastIndexOf('.'));
            File DirFile= playFile.getParentFile();
            String parentPath = DirFile.getPath() + "/";
            if (DirFile.isDirectory()) {
                for (String file : DirFile.list()) {
                    String fileLow = file.toLowerCase();
                    String prefixLow = prefix.toLowerCase();
                    if (fileLow.startsWith(prefixLow)) {
                        for (String ext : extensions) {
                            if (fileLow.endsWith(ext)) {
                                try {
                                    LOGI(TAG,"[searchExternalSubtitle]file:"+(parentPath + file));
                                    if (!subtitleSkipedSubForIdx(file, DirFile)) {
                                        LOGI(TAG,"[searchExternalSubtitle]addTimedTextSource file:"+file);
                                        mMediaPlayer.addTimedTextSource(mContext, Uri.parse(parentPath + file), mMediaPlayer.MEDIA_MIMETYPE_TEXT_SUBOTHER);
                                    }
                                    break;
                                } catch (IOException ex) {
                                    LOGE(TAG, "[searchExternalSubtitle]IOException ex:"+ex);
                                } catch (IllegalArgumentException ex) {
                                    LOGE(TAG, "[searchExternalSubtitle]IllegalArgumentException ex:"+ex);
                                }
                                
                            }
                        }
                    }
                }
            }
        }
    }

    private void subtitleHide() {
        if ((null != subtitleIV) && (View.VISIBLE == subtitleIV.getVisibility())) {
            subtitleIV.setVisibility(View.GONE);
        }
        if ((null != subtitleTV) && (View.VISIBLE == subtitleTV.getVisibility())) {
            subtitleTV.setVisibility(View.GONE);
        }
    }

    private void subtitleShow() {
        if ((null != subtitleIV) && (View.GONE == subtitleIV.getVisibility())) {
            subtitleIV.setVisibility(View.VISIBLE);
        }
        if ((null != subtitleTV) && (View.GONE == subtitleTV.getVisibility())) {
            subtitleTV.setVisibility(View.VISIBLE);
        }
    }

    private void subtitleSetFont(int size) {
        if (!isShowImgSubtitle) {
            if ((null != subtitleTV) && (View.VISIBLE == subtitleTV.getVisibility())) {
                subtitleTV.setTextSize(size);
            }
        }
    }

    private void subtitleSetColor(int color) {
        if (!isShowImgSubtitle) {
            if ((null != subtitleTV) && (View.VISIBLE == subtitleTV.getVisibility())) {
                subtitleTV.setTextColor(color);
            }
        }
    }

    private void subtitleSetStyle(int style) {
        if (!isShowImgSubtitle) {
            if ((null != subtitleTV) && (View.VISIBLE == subtitleTV.getVisibility())) {
                subtitleTV.setTypeface(null, style);
            }
        }
    }

    private void subtitleSetPosHeight(int height) {
        if (!isShowImgSubtitle) {
            if ((null != subtitleTV) && (View.VISIBLE == subtitleTV.getVisibility())) {
                subtitleTV.setPadding(
                    subtitleTV.getPaddingLeft(),
                    subtitleTV.getPaddingTop(),
                    subtitleTV.getPaddingRight(),height);
            }
        }
    }

    private void sendSubOptionUpdateMsg() {
        if(mHandler != null) {
            Message msg = mHandler.obtainMessage(MSG_SUB_OPTION_UPDATE);
            mHandler.sendMessageDelayed(msg, MSG_SEND_DELAY);
            LOGI(TAG,"[sendSubOptionUpdateMsg]sendMessageDelayed MSG_SUB_OPTION_UPDATE");
        }
    }
    
    private int getLanguageIndex(int type, String lang) {
        int index = -1;

        switch (type) {
            case MediaInfo.BLURAY_STREAM_TYPE_VIDEO:
                index = mBlurayVideoLang.indexOf(lang);
                break;
            case MediaInfo.BLURAY_STREAM_TYPE_AUDIO:
                index = mBlurayAudioLang.indexOf(lang);
                break;
            case MediaInfo.BLURAY_STREAM_TYPE_SUB:
                index = mBluraySubLang.indexOf(lang);
                break;
            default:
                break;
        }
        LOGI(TAG, "getLanguageIndex, type: " + type + " lang: " + lang + " index: " + index);
        return index;
    }

    private String getDisplayLanguage(String lang) {
        if (TextUtils.isEmpty(lang))
            return null;

        for (LocalePicker.LocaleInfo info : LOCALES) {
            Locale l = info.getLocale();
            if (lang.equals(l.getISO3Language()))
                return l.getDisplayLanguage();
        }

        return null;
    }

    private String getLanguageInfoDisplayString(int type, int index) {
        String str = "";
        switch (type) {
            case MediaInfo.BLURAY_STREAM_TYPE_VIDEO: {
                break;
            }
            case MediaInfo.BLURAY_STREAM_TYPE_AUDIO: {
                if (index + 1 < 10)
                    str += "0" + String.valueOf(index + 1) + "/";
                else
                    str += String.valueOf(index + 1) + "/";
                int total = mMediaInfo.getAudioTotalNum();
                if (total < 10)
                    str += "0" + String.valueOf(total) + " ";
                else
                    str += String.valueOf(total) + " ";

                int audlang_size = mBlurayAudioLang.size();
                if ((index >= 0) && (index < audlang_size))
                    str += getDisplayLanguage(mBlurayAudioLang.get(index)) + " ";
                else
                    Log.e(TAG, "getLanguageInfoDisplayString, audio index: " + index + " is over 0~" + audlang_size);
                str += mMediaInfo.getAudioFormatStr(mMediaInfo.getAudioFormat(index)) + " ";
                // TODO: audio channel
                str += String.valueOf(mMediaInfo.getAudioSampleRate(index)) + "Hz";
                break;
            }
            case MediaInfo.BLURAY_STREAM_TYPE_SUB: {
                str += mContext.getResources().getString(R.string.setting_subtitle) + ": ";
                String subType = mMediaPlayer.subtitleGetSubTypeStr();
                if (subType.equals("INSUB")) {
                    str += mContext.getResources().getString(R.string.subtitle_insub) + " ";
                    int typeDetail = mMediaPlayer.subtitleGetSubTypeDetial();
                    switch (typeDetail) {
                        case SUBTITLE_PGS:
                            str += "PGS ";
                            break;
                        case SUBTITLE_DVB:
                            str += "DVB ";
                            break;
                        case SUBTITLE_TMD_TXT:
                            str += "TMD TXT ";
                            break;
                        default:
                            break;
                    }
                } else
                    str += subType;
                int sublang_size = mBluraySubLang.size();
                if ((index >= 0) && (index < sublang_size))
                    str += getDisplayLanguage(mBluraySubLang.get(index));
                else
                    Log.e(TAG, "getLanguageInfoDisplayString, sub index: " + index + " is over 0~" + sublang_size);
                break;
            }
            default:
                break;
        }

        return str;
    }

    private int getChapterIndex(int current) {
        int count = mBlurayChapter.size();
        int index;
        for (index = 0; index < count; index++) {
            if (current < mBlurayChapter.get(index).start) {
                return index - 1;
            }
        }
        if (index == count) {
            index--;
            return index;
        }

        return -1;
    }

    private String getChapterInfoDisplayString(int index) {
        String str = "";
        if (index + 1 < 10)
            str += "0" + String.valueOf(index + 1) + "/";
        else
            str += String.valueOf(index + 1) + "/";
        int total = mBlurayChapter.size();
        if (total < 10)
            str += "0" + String.valueOf(total) + "    ";
        else
            str += String.valueOf(total) + "    ";
        ChapterInfo info = mBlurayChapter.get(index);
        str += secToTime(info.start) + " - ";
        str += secToTime(info.start + info.duration);

        return str;
    }

    private void initSubtitle() {
        SharedPreferences subSp = getSharedPreferences(subSettingStr, 0); 
        sub_para = new subview_set();
        sub_para.totalnum = 0;
        sub_para.curid = mSubIndex;
        sub_para.curidbac = 0;
        sub_para.color = android.graphics.Color.WHITE;
        sub_para.font=30;
        sub_para.position_v=0;
        //sub_para.color = subSp.getInt("color", android.graphics.Color.WHITE);
        //sub_para.font=subSp.getInt("font", 20);
        //sub_para.position_v=subSp.getInt("position_v", 0);
        setSubtitleView();
    }

    private void subtitle_prepare() {
        if (!isTimedTextDisable()) {
            sub_para.totalnum = getSubtitleTotal();
        }
        else {
            if(mMediaPlayer != null) {
                sub_para.totalnum = mMediaPlayer.subtitleTotal();
            }
        }
    }

    private void setSubtitleView() {
        if (!isTimedTextDisable()) {
            subtitleSetFont(sub_para.font);
            subtitleSetColor(sub_para.color);
            subtitleSetStyle(Typeface.BOLD);
            subtitleSetPosHeight(getWindowManager().getDefaultDisplay().getHeight()*sub_para.position_v/20+10);
        }
        else {
            if(mMediaPlayer != null) {
            //mMediaPlayer.subtitleClear();
            mMediaPlayer.subtitleSetGravity(Gravity.CENTER);
            mMediaPlayer.subtitleSetTextColor(sub_para.color);
            mMediaPlayer.subtitleSetTextSize(sub_para.font);
            mMediaPlayer.subtitleSetTextStyle(Typeface.BOLD);
            mMediaPlayer.subtitleSetPosHeight(getWindowManager().getDefaultDisplay().getHeight()*sub_para.position_v/20+2);
        }
        }
    }

    private void subtitle_control() {
        LOGI(TAG,"[subtitle_control]");
        
        Button ok = (Button) findViewById(R.id.button_ok);
        Button cancel = (Button) findViewById(R.id.button_canncel);
        ImageButton Bswitch_l = (ImageButton) findViewById(R.id.switch_l);	
        ImageButton Bswitch_r = (ImageButton) findViewById(R.id.switch_r);
        ImageButton Bfont_l = (ImageButton) findViewById(R.id.font_l);	
        ImageButton Bfont_r = (ImageButton) findViewById(R.id.font_r);
        ImageButton Bcolor_l = (ImageButton) findViewById(R.id.color_l);	
        ImageButton Bcolor_r = (ImageButton) findViewById(R.id.color_r);
        ImageButton Bposition_v_l = (ImageButton) findViewById(R.id.position_v_l);	
        ImageButton Bposition_v_r = (ImageButton) findViewById(R.id.position_v_r);
        TextView font =(TextView)findViewById(R.id.font_title);
        TextView color =(TextView)findViewById(R.id.color_title);
        TextView position_v =(TextView)findViewById(R.id.position_v_title);

        final String color_text[]={ 
            VideoPlayer.this.getResources().getString(R.string.color_white),
            VideoPlayer.this.getResources().getString(R.string.color_yellow),
            VideoPlayer.this.getResources().getString(R.string.color_blue)
        };
        mcolor_text = color_text;

        initSubSetOptions(color_text);
        ok.setOnClickListener(new View.OnClickListener() {	
            public void onClick(View v) {
                sub_para.curid = sub_switch_state;
                mSubIndex = sub_switch_state;
                sub_para.font = sub_font_state;
                sub_para.position_v = sub_position_v_state;

                LOGI(TAG,"[subtitle_control]sub_para.curid:"+sub_para.curid+",sub_para.curidbac:"+sub_para.curidbac);
                if(mMediaPlayer != null) {
                    if(sub_para.curid==sub_para.totalnum) {
                        if (!isTimedTextDisable()) {
                            subtitleHide();
                        }
                        else {
                            mMediaPlayer.subtitleHide();
                        }
                    }
                    else {
                        if(sub_para.curidbac != sub_para.curid) {
                            LOGI(TAG,"[subtitle_control]selectTrack :"+(sub_para.curid + mSubOffset));
                            //mMediaPlayer.selectTrack(sub_para.curid + mSubOffset);
                            if (!isTimedTextDisable()) {
                                int subTrack = -1;
                                if(mTrackInfo != null) {
                                    for(int i = 0; i < mTrackInfo.length; i++) {
                                        int trackType = mTrackInfo[i].getTrackType();
                                        if (trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT) {
                                            subTrack ++;
                                            if (subTrack == sub_para.curid) {
                                                LOGI(TAG,"[subtitle_control]selectTrack track num:"+i);
                                                mMediaPlayer.selectTrack(i);
                                            }
                                        }
                                    }
                                }
                            }
                            else {
                                mMediaPlayer.subtitleOpenIdx(sub_para.curid);
                            }
                
                            sub_para.curidbac = sub_para.curid;
                        }
                        mMediaPlayer.subtitleOpenIdx(sub_para.curid);
                        //else {
                            if (!isTimedTextDisable()) {
                                subtitleShow();
                            }
                            else {
                                mMediaPlayer.subtitleDisplay();
                            }
                        //}
                    }
                }

                // update msg sformat
                msgSFormatUpdate(); 
                
                if(sub_color_state==0)
                    sub_para.color =android.graphics.Color.WHITE;
                else if(sub_color_state==1) 
                    sub_para.color =android.graphics.Color.YELLOW;
                else
                    sub_para.color =android.graphics.Color.BLUE;

                SharedPreferences settings = getSharedPreferences(subSettingStr, 0); 
                SharedPreferences.Editor editor = settings.edit(); 
                editor.putInt("color", sub_para.color); 
                editor.putInt("font", sub_para.font); 
                editor.putInt("position_v", sub_para.position_v);  
                editor.commit();  

                setSubtitleView();
                if (isTimedTextDisable()) {
                    if(mMediaPlayer != null) {
                        //still have error with new method
                        /*if(mMediaPlayer.subtitleGetSubType() == 1) { //bitmap
                            disableSubSetOptions();
                        }
                        else {
                            initSubSetOptions(color_text);
                        }*/
                        String subNameStr = mMediaPlayer.subtitleGetCurName();
                        if(subNameStr != null) {
                            if(subNameStr.equals("INSUB") || subNameStr.endsWith(".idx")) {
                                disableSubSetOptions();
                            }
                            else {
                                initSubSetOptions(color_text);
                            }
                        }
                        else {
                            disableSubSetOptions();
                        }
                    }
                    else {
                        initSubSetOptions(color_text);
                    }
                }
                else {
                    sendSubOptionUpdateMsg();
                }
                
                exitSubWidget(subtitleSwitchBtn);
            } 
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // do nothing
                exitSubWidget(subtitleSwitchBtn);
            } 
        });

        Bswitch_l.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(sub_switch_state <= 0)
                    sub_switch_state =sub_para.totalnum;
                else
                    sub_switch_state --;

                if (sub_switch_state == sub_para.totalnum) {
                    t_subswitch.setText (R.string.str_off);
                    t_subinfo.setVisibility(View.GONE);
                } else {
                    t_subswitch.setText (String.valueOf (sub_switch_state + 1) + "/" + String.valueOf (sub_para.totalnum));
                    if (mIsBluray) {
                        if (t_subinfo.getVisibility() == View.GONE)
                            t_subinfo.setVisibility(View.VISIBLE);
                        t_subinfo.setText(getLanguageInfoDisplayString(MediaInfo.BLURAY_STREAM_TYPE_SUB, sub_switch_state));
                    }
                }
            } 
        });
        Bswitch_r.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(sub_switch_state >= sub_para.totalnum)
                    sub_switch_state =0;
                else
                    sub_switch_state ++;

                if (sub_switch_state == sub_para.totalnum) {
                    t_subswitch.setText (R.string.str_off);
                    t_subinfo.setVisibility(View.GONE);
                } else {
                    t_subswitch.setText (String.valueOf (sub_switch_state + 1) + "/" + String.valueOf (sub_para.totalnum));
                    if (mIsBluray) {
                        if (t_subinfo.getVisibility() == View.GONE)
                            t_subinfo.setVisibility(View.VISIBLE);
                        t_subinfo.setText(getLanguageInfoDisplayString(MediaInfo.BLURAY_STREAM_TYPE_SUB, sub_switch_state));
                    }
                }
            } 
        });

        Bfont_l.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(sub_font_state > 20)
                    sub_font_state =sub_font_state-2;
                else
                    sub_font_state =50;

                t_subsfont.setText(String.valueOf(sub_font_state));	 
            } 
        });
        Bfont_r.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(sub_font_state < 50)
                    sub_font_state =sub_font_state +2;
                else
                    sub_font_state =20;

                t_subsfont.setText(String.valueOf(sub_font_state));
            } 
        });

        Bcolor_l.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(sub_color_state<= 0)
                    sub_color_state=2;
                else 
                    sub_color_state-- ;

                t_subscolor.setText(color_text[sub_color_state]);
            } 
        });
        Bcolor_r.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(sub_color_state>=2)
                    sub_color_state=0;
                else 
                    sub_color_state++ ;

                t_subscolor.setText(color_text[sub_color_state]);
            } 
        });

        Bposition_v_l.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(sub_position_v_state<= 0)
                    sub_position_v_state=15;
                else 
                    sub_position_v_state-- ;

                t_subsposition_v.setText(String.valueOf(sub_position_v_state));
            } 
        });
        Bposition_v_r.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(sub_position_v_state>=15)
                    sub_position_v_state=0;
                else 
                    sub_position_v_state++ ;

                t_subsposition_v.setText(String.valueOf(sub_position_v_state));
            } 
        });

        if (isTimedTextDisable()) {
            if(mMediaPlayer != null) {
                //still have error with new method
                /*if(mMediaPlayer.subtitleGetSubType() == 1) { //bitmap
                    disableSubSetOptions();
                }
                else {
                    initSubSetOptions(color_text);
                }*/
                String subNameStr = mMediaPlayer.subtitleGetCurName();
                if(subNameStr != null) {
                    if(subNameStr.equals("INSUB") || subNameStr.endsWith(".idx")) {
                        disableSubSetOptions();
                    }
                    else {
                        initSubSetOptions(color_text);
                    }
                }
                else {
                    disableSubSetOptions();
                }
            }
            else {
                initSubSetOptions(color_text);
            }
        }
        else {
            sendSubOptionUpdateMsg();
        }
    }

    private void updateCurSubIdx() {
        if (mMediaPlayer != null) {
            int idx = mMediaPlayer.subtitleGetCurSubIdx();
            LOGI(TAG,"[updateCurSubIdx]idx:" + idx + ", sub_para.curid:" + sub_para.curid);
            if (idx != sub_para.curid) {
                sub_switch_state = idx;
                sub_para.curid = idx;
            }
        }
    }
	
    private void initSubSetOptions(String color_text[]) {
        t_subswitch =(TextView)findViewById(R.id.sub_swith111);
        t_subinfo = (TextView) findViewById (R.id.sub_info);
        t_subsfont =(TextView)findViewById(R.id.sub_font111);
        t_subscolor =(TextView)findViewById(R.id.sub_color111);
        t_subsposition_v =(TextView)findViewById(R.id.sub_position_v111);

        sub_switch_state = sub_para.curid;
        sub_font_state = sub_para.font;
        sub_position_v_state = sub_para.position_v;

        updateCurSubIdx();

        if(sub_para.color==android.graphics.Color.WHITE)
            sub_color_state =0;
        else if(sub_para.color==android.graphics.Color.YELLOW)
            sub_color_state =1;
        else
            sub_color_state =2;

        if(sub_para.curid==sub_para.totalnum) {
            sub_para.curid=sub_para.totalnum;
            mSubIndex = sub_para.totalnum;
            t_subswitch.setText(R.string.str_off);
            t_subinfo.setVisibility(View.GONE);
        } else {
            t_subswitch.setText (String.valueOf (sub_para.curid + 1) + "/" + String.valueOf (sub_para.totalnum));
            if (mIsBluray) {
                if (t_subinfo.getVisibility() == View.GONE)
                    t_subinfo.setVisibility(View.VISIBLE);
                t_subinfo.setText(getLanguageInfoDisplayString(MediaInfo.BLURAY_STREAM_TYPE_SUB, mSubIndex));
            }
        }
        t_subsfont.setText(String.valueOf(sub_font_state));
        t_subscolor.setText(color_text[sub_color_state]);
        t_subsposition_v.setText(String.valueOf(sub_position_v_state));

        Button ok = (Button) findViewById(R.id.button_ok);
        Button cancel = (Button) findViewById(R.id.button_canncel);
        ImageButton Bswitch_l = (ImageButton) findViewById(R.id.switch_l);	
        ImageButton Bswitch_r = (ImageButton) findViewById(R.id.switch_r);
        ImageButton Bfont_l = (ImageButton) findViewById(R.id.font_l);	
        ImageButton Bfont_r = (ImageButton) findViewById(R.id.font_r);
        ImageButton Bcolor_l = (ImageButton) findViewById(R.id.color_l);	
        ImageButton Bcolor_r = (ImageButton) findViewById(R.id.color_r);
        ImageButton Bposition_v_l = (ImageButton) findViewById(R.id.position_v_l);	
        ImageButton Bposition_v_r = (ImageButton) findViewById(R.id.position_v_r);
        TextView font =(TextView)findViewById(R.id.font_title);
        TextView color =(TextView)findViewById(R.id.color_title);
        TextView position_v =(TextView)findViewById(R.id.position_v_title);

        font.setTextColor(android.graphics.Color.BLACK);
        color.setTextColor(android.graphics.Color.BLACK);
        position_v.setTextColor(android.graphics.Color.BLACK);

        t_subsfont.setTextColor(android.graphics.Color.BLACK);
        t_subscolor.setTextColor(android.graphics.Color.BLACK);
        t_subsposition_v.setTextColor(android.graphics.Color.BLACK);	

        Bfont_l.setEnabled(true);
        Bfont_r.setEnabled(true);
        Bcolor_l.setEnabled(true);
        Bcolor_r.setEnabled(true);
        Bposition_v_l.setEnabled(true);
        Bposition_v_r.setEnabled(true);
        Bfont_l.setImageResource(R.drawable.fondsetup_larrow_unfocus);
        Bfont_r.setImageResource(R.drawable.fondsetup_rarrow_unfocus);
        Bcolor_l.setImageResource(R.drawable.fondsetup_larrow_unfocus);
        Bcolor_r.setImageResource(R.drawable.fondsetup_rarrow_unfocus);
        Bposition_v_l.setImageResource(R.drawable.fondsetup_larrow_unfocus);
        Bposition_v_r.setImageResource(R.drawable.fondsetup_rarrow_unfocus);

        Bswitch_l.setNextFocusUpId(R.id.switch_l);
        Bswitch_l.setNextFocusDownId(R.id.font_l);
        Bswitch_l.setNextFocusLeftId(R.id.switch_l);
        Bswitch_l.setNextFocusRightId(R.id.switch_r);

        Bswitch_r.setNextFocusUpId(R.id.switch_r);
        Bswitch_r.setNextFocusDownId(R.id.font_r);
        Bswitch_r.setNextFocusLeftId(R.id.switch_l);
        Bswitch_r.setNextFocusRightId(R.id.switch_r);

        Bfont_l.setNextFocusUpId(R.id.switch_l);
        Bfont_l.setNextFocusDownId(R.id.color_l);
        Bfont_l.setNextFocusLeftId(R.id.font_l);
        Bfont_l.setNextFocusRightId(R.id.font_r);

        Bfont_r.setNextFocusUpId(R.id.switch_r);
        Bfont_r.setNextFocusDownId(R.id.color_r);
        Bfont_r.setNextFocusLeftId(R.id.font_l);
        Bfont_r.setNextFocusRightId(R.id.font_r);

        Bcolor_l.setNextFocusUpId(R.id.font_l);
        Bcolor_l.setNextFocusDownId(R.id.position_v_l);
        Bcolor_l.setNextFocusLeftId(R.id.color_l);
        Bcolor_l.setNextFocusRightId(R.id.color_r);

        Bcolor_r.setNextFocusUpId(R.id.font_r);
        Bcolor_r.setNextFocusDownId(R.id.position_v_r);
        Bcolor_r.setNextFocusLeftId(R.id.color_l);
        Bcolor_r.setNextFocusRightId(R.id.color_r);

        Bposition_v_l.setNextFocusUpId(R.id.color_l);
        Bposition_v_l.setNextFocusDownId(R.id.button_ok);
        Bposition_v_l.setNextFocusLeftId(R.id.position_v_l);
        Bposition_v_l.setNextFocusRightId(R.id.position_v_r);

        Bposition_v_r.setNextFocusUpId(R.id.color_r);
        Bposition_v_r.setNextFocusDownId(R.id.button_canncel);
        Bposition_v_r.setNextFocusLeftId(R.id.position_v_l);
        Bposition_v_r.setNextFocusRightId(R.id.position_v_r);

        cancel.setNextFocusUpId(R.id.position_v_r);
        cancel.setNextFocusDownId(R.id.button_canncel);
        cancel.setNextFocusLeftId(R.id.button_ok);
        cancel.setNextFocusRightId(R.id.button_canncel);

        ok.setNextFocusUpId(R.id.position_v_l);
        ok.setNextFocusDownId(R.id.button_ok);
        ok.setNextFocusLeftId(R.id.button_ok);
        ok.setNextFocusRightId(R.id.button_canncel);
    }

    private void disableSubSetOptions() {
        Button ok = (Button) findViewById(R.id.button_ok);
        Button cancel = (Button) findViewById(R.id.button_canncel);
        ImageButton Bswitch_l = (ImageButton) findViewById(R.id.switch_l);	
        ImageButton Bswitch_r = (ImageButton) findViewById(R.id.switch_r);
        ImageButton Bfont_l = (ImageButton) findViewById(R.id.font_l);	
        ImageButton Bfont_r = (ImageButton) findViewById(R.id.font_r);
        ImageButton Bcolor_l = (ImageButton) findViewById(R.id.color_l);	
        ImageButton Bcolor_r = (ImageButton) findViewById(R.id.color_r);
        ImageButton Bposition_v_l = (ImageButton) findViewById(R.id.position_v_l);	
        ImageButton Bposition_v_r = (ImageButton) findViewById(R.id.position_v_r);
        TextView font =(TextView)findViewById(R.id.font_title);
        TextView color =(TextView)findViewById(R.id.color_title);
        TextView position_v =(TextView)findViewById(R.id.position_v_title);

        font.setTextColor(android.graphics.Color.LTGRAY);
        color.setTextColor(android.graphics.Color.LTGRAY);
        position_v.setTextColor(android.graphics.Color.LTGRAY);

        t_subsfont.setTextColor(android.graphics.Color.LTGRAY);
        t_subscolor.setTextColor(android.graphics.Color.LTGRAY);
        t_subsposition_v.setTextColor(android.graphics.Color.LTGRAY);	

        Bfont_l.setEnabled(false);
        Bfont_r.setEnabled(false);
        Bcolor_l.setEnabled(false);
        Bcolor_r.setEnabled(false);
        Bposition_v_l.setEnabled(false);
        Bposition_v_r.setEnabled(false);
        Bfont_l.setImageResource(R.drawable.fondsetup_larrow_disable);
        Bfont_r.setImageResource(R.drawable.fondsetup_rarrow_disable);
        Bcolor_l.setImageResource(R.drawable.fondsetup_larrow_disable);
        Bcolor_r.setImageResource(R.drawable.fondsetup_rarrow_disable);
        Bposition_v_l.setImageResource(R.drawable.fondsetup_larrow_disable);
        Bposition_v_r.setImageResource(R.drawable.fondsetup_rarrow_disable);

        Bswitch_l.setNextFocusUpId(R.id.switch_l);
        Bswitch_l.setNextFocusDownId(R.id.button_ok);
        Bswitch_l.setNextFocusLeftId(R.id.switch_l);
        Bswitch_l.setNextFocusRightId(R.id.switch_r);

        Bswitch_r.setNextFocusUpId(R.id.switch_r);
        Bswitch_r.setNextFocusDownId(R.id.button_canncel);
        Bswitch_r.setNextFocusLeftId(R.id.switch_l);
        Bswitch_r.setNextFocusRightId(R.id.switch_r);

        ok.setNextFocusUpId(R.id.switch_l);
        ok.setNextFocusDownId(R.id.button_ok);
        ok.setNextFocusLeftId(R.id.button_ok);
        ok.setNextFocusRightId(R.id.button_canncel);

        cancel.setNextFocusUpId(R.id.switch_r);
        cancel.setNextFocusDownId(R.id.button_canncel);
        cancel.setNextFocusLeftId(R.id.button_ok);
        cancel.setNextFocusRightId(R.id.button_canncel);
    }

    //@@--------this part for other widget list view--------------------------------------------------------------------------------
    private String[] m_brightness= {"1","2","3","4","5","6"}; // for brightness
    private int[] string_3d_id = {
        R.string.setting_3d_diable,
        R.string.setting_3d_lr,
        //R.string.setting_3d_lr_switch,
        R.string.setting_3d_bt,
        /*R.string.setting_3d_auto,
        R.string.setting_3d_2d_l,
        R.string.setting_3d_2d_r,
        R.string.setting_3d_2d_t,
        R.string.setting_3d_2d_b,
        R.string.setting_3d_2d_auto1,
        R.string.setting_3d_2d_auto2,	
        R.string.setting_2d_3d,
        R.string.setting_3d_field_depth,
        R.string.setting_3d_auto_switch,
        R.string.setting_3d_lr_switch,
        R.string.setting_3d_tb_switch,
        R.string.setting_3d_full_off,
        R.string.setting_3d_lr_full,
        R.string.setting_3d_tb_full,
        R.string.setting_3d_grating_open,
        R.string.setting_3d_grating_close,*/
    };
    
    private SimpleAdapter getMorebarListAdapter(int id, int pos) {
        return new SimpleAdapter(this, getMorebarListData(id, pos),
            R.layout.list_row, 
            new String[] {"item_name", "item_sel"},
            new int[] {R.id.Text01, R.id.imageview}
        );
    }

    private SimpleAdapter getLeftAlignMorebarListAdapter (int id, int pos) {
        return new SimpleAdapter (this, getMorebarListData (id, pos),
                R.layout.left_align_list_row,
                new String[] {"item_name", "item_sel"},
                new int[] {R.id.Text01, R.id.imageview}
                );
    }

    private List<? extends Map<String, ?>> getMorebarListData(int id, int pos) {
        // TODO Auto-generated method stub
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map;
        switch (id) {
            case RESUME_MODE:
                map = new HashMap<String, Object>();
                map.put("item_name", getResources().getString(R.string.str_on));
                map.put("item_sel", R.drawable.item_img_unsel);
                list.add(map);
                map = new HashMap<String, Object>();
                map.put("item_name", getResources().getString(R.string.str_off));
                map.put("item_sel", R.drawable.item_img_unsel);
                list.add(map);

                list.get(pos).put("item_sel", R.drawable.item_img_sel);
            break;

            case REPEAT_MODE:
                map = new HashMap<String, Object>();
                map.put("item_name", getResources().getString(R.string.setting_playmode_repeatall));
                map.put("item_sel", R.drawable.item_img_unsel);
                list.add(map);
                map = new HashMap<String, Object>();
                map.put("item_name", getResources().getString(R.string.setting_playmode_repeatone));
                map.put("item_sel", R.drawable.item_img_unsel);
                list.add(map);

                list.get(pos).put("item_sel", R.drawable.item_img_sel);
            break;

            case AUDIO_OPTION:
                map = new HashMap<String, Object>();
                map.put("item_name", getResources().getString(R.string.setting_audiotrack));
                map.put("item_sel", R.drawable.item_img_unsel);
                list.add(map);
                map = new HashMap<String, Object>();
                map.put("item_name", getResources().getString(R.string.setting_soundtrack));
                map.put("item_sel", R.drawable.item_img_unsel);
                list.add(map);
                map = new HashMap<String, Object>();
                map.put("item_name", getResources().getString(R.string.setting_videotrack));
                map.put("item_sel", R.drawable.item_img_unsel);
                list.add(map);
            break;

            case SOUND_TRACK:
                map = new HashMap<String, Object>();
                map.put("item_name", getResources().getString(R.string.setting_soundtrack_stereo));
                map.put("item_sel", R.drawable.item_img_unsel);
                list.add(map);
                map = new HashMap<String, Object>();
                map.put("item_name", getResources().getString(R.string.setting_soundtrack_lmono));
                map.put("item_sel", R.drawable.item_img_unsel);
                list.add(map);
                map = new HashMap<String, Object>();
                map.put("item_name", getResources().getString(R.string.setting_soundtrack_rmono));
                map.put("item_sel", R.drawable.item_img_unsel);
                list.add(map);
                map = new HashMap<String, Object>();
                map.put("item_name", getResources().getString(R.string.setting_soundtrack_lrmix));
                map.put("item_sel", R.drawable.item_img_unsel);
                list.add(map);

                list.get(pos).put("item_sel", R.drawable.item_img_sel);
            break;
            
            case AUDIO_TRACK:
                if (mMediaInfo != null) {
                    int audio_total_num = mMediaInfo.getAudioTotalNum();
                    for (int i = 0; i < audio_total_num; i++) {
                            map = new HashMap<String, Object>();
                            if (mIsBluray)
                                map.put ("item_name", getLanguageInfoDisplayString(MediaInfo.BLURAY_STREAM_TYPE_AUDIO, i));
                            else
                                map.put ("item_name", mMediaInfo.getAudioFormatStr(mMediaInfo.getAudioFormat(i)));
                            map.put("item_sel", R.drawable.item_img_unsel);
                            list.add(map);
                    }
                    LOGI(TAG,"list.size():"+list.size()+",pos:"+pos+",audio_total_num:"+audio_total_num);
                    if(pos < 0) {
                        pos = 0;
                    }
                    list.get(pos).put("item_sel", R.drawable.item_img_sel);
                }
            break;

            case CHAPTER_MODE: {
                int count = mBlurayChapter.size();
                for (int i = 0; i < count; i++) {
                    map = new HashMap<String, Object>();
                    map.put("item_name", getChapterInfoDisplayString(i));
                    map.put("item_sel", R.drawable.item_img_unsel);
                    list.add(map);
                }
                int index = getChapterIndex(getCurrentPosition() / 1000);
                if (index >= 0 && index < count)
                    list.get(index).put("item_sel", R.drawable.item_img_sel);
                break;
            }

            case VIDEO_TRACK:
                if (mMediaInfo != null) {
                    int ts_total_num = mMediaInfo.getTsTotalNum();
                    for (int i = 0; i < ts_total_num; i++) {
                        map = new HashMap<String, Object>();
                        map.put("item_name", mMediaInfo.getTsTitle(i));
                        map.put("item_sel", R.drawable.item_img_unsel);
                        list.add(map);
                    }
                    LOGI(TAG,"list.size():" + list.size() + ",pos:" + pos + ",ts_total_num:" + ts_total_num);
                    if (ts_total_num == 0) {
                        int video_total_num = mMediaInfo.getVideoTotalNum();
                        for (int i = 0; i < video_total_num; i++) {
                            map = new HashMap<String, Object>();
                            map.put("item_name", mMediaInfo.getVideoFormat(i));
                            map.put("item_sel", R.drawable.item_img_unsel);
                            list.add(map);
                        }
                    }
                    if(pos < 0) {
                        pos = 0;
                    }
                    list.get(pos).put("item_sel", R.drawable.item_img_sel);
                }
            break;

            case AUDIO_DTS_APRESENT:
                int dts_apresent_total_num = getDtsApresentTotalNum(); //dts test 
                for (int i = 0; i < dts_apresent_total_num; i++) {
                    map = new HashMap<String, Object>();
                    map.put("item_name", "Apresentation"+Integer.toString(i));
                    map.put("item_sel", R.drawable.item_img_unsel);
                    list.add(map);
                }
                LOGI(TAG,"list.size():"+list.size()+",pos:"+pos+",dts_apresent_total_num:"+dts_apresent_total_num);
                if(pos < 0) {
                    pos = 0;
                }
                //list.get(pos).put("item_sel", R.drawable.item_img_sel);
            break;

            case AUDIO_DTS_ASSET:
                int dts_asset_total_num = getDtsAssetTotalNum(mApresentIdx); //dts test 
                for (int i = 0; i < dts_asset_total_num; i++) {
                    map = new HashMap<String, Object>();
                    map.put("item_name", "Asset"+Integer.toString(i));
                    map.put("item_sel", R.drawable.item_img_unsel);
                    list.add(map);
                }
                LOGI(TAG,"list.size():"+list.size()+",pos:"+pos+",dts_asset_total_num:"+dts_asset_total_num);
                if(pos < 0) {
                    pos = 0;
                }
                break;
            
            case DISPLAY_MODE:
                map = new HashMap<String, Object>();
                map.put("item_name", getResources().getString(R.string.setting_displaymode_normal));
                map.put("item_sel", R.drawable.item_img_unsel);
                list.add(map);
                map = new HashMap<String, Object>();
                map.put("item_name", getString(R.string.setting_displaymode_fullscreen));
                map.put("item_sel", R.drawable.item_img_unsel);
                list.add(map);
                map = new HashMap<String, Object>();
                map.put("item_name", "4:3");
                map.put("item_sel", R.drawable.item_img_unsel);
                list.add(map);
                map = new HashMap<String, Object>();
                map.put("item_name", "16:9");
                map.put("item_sel", R.drawable.item_img_unsel);
                list.add(map);
                    map = new HashMap<String, Object>();
                    map.put ("item_name", getString (R.string.setting_displaymode_original));
                    map.put ("item_sel", R.drawable.item_img_unsel);
                    list.add (map);
                // TODO: 3D
                /*
                if(sw.getPropertyBoolean("3D_setting.enable", false)){ 
                    if(is3DVideoDisplayFlag){//judge is 3D                		
                        map = new HashMap<String, Object>();
                        map.put("item_name", getResources().getString(R.string.setting_displaymode_normal_noscaleup));
                        map.put("item_sel", R.drawable.item_img_unsel);
                        list.add(map);
                    }
                }*/
                list.get(pos).put("item_sel", R.drawable.item_img_sel);
            break;

            case BRIGHTNESS:
                int size_bgh = m_brightness.length;
                for (int i = 0; i < size_bgh; i++) {
                    map = new HashMap<String, Object>();
                    map.put("item_name", m_brightness[i].toString());
                    map.put("item_sel", R.drawable.item_img_unsel);
                    list.add(map);
                }

                list.get(pos).put("item_sel", R.drawable.item_img_sel);
            break;
            
            case PLAY3D:
                int size_3d = string_3d_id.length;
                for (int i = 0; i < size_3d; i++) {
                    map = new HashMap<String, Object>();
                    map.put("item_name", getResources().getString(string_3d_id[i]));
                    map.put("item_sel", R.drawable.item_img_unsel);
                    list.add(map);
                } 
                if(mOption != null) {
                    list.get(mOption.get3DMode()).put("item_sel", R.drawable.item_img_sel); 
                }
            break;
            
            default:
            break;
        }

        return list;
    }

    public void timeSetDailog(){
        String displaytime = getText(R.string.setting_time_total)+" "+secToTime(totaltime/1000);
        LayoutInflater layoutInflater = LayoutInflater.from(VideoPlayer.this);
        final View timeSetView =layoutInflater.inflate(R.layout.time_set, null);
        mtimeSetting = new AlertDialog.Builder(VideoPlayer.this)
                .setTitle(R.string.setting_time_title)
                .setMessage(displaytime)
                .setView(timeSetView)
                .setPositiveButton(R.string.setting_time_ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialoginterface, int i) {
                            EditText hourInpuText=(EditText)timeSetView.findViewById(R.id.hourinput);
                            EditText minuteInpuText=(EditText)timeSetView.findViewById(R.id.minuteinput);
                            EditText secondInpuText=(EditText)timeSetView.findViewById(R.id.secondinput);
                            String hourString = hourInpuText.getText().toString();
                            String minuteString = minuteInpuText.getText().toString();
                            String secondString = secondInpuText.getText().toString();
                            Log.d(TAG, "hour:"+hourString+" minute:"+minuteString+" second:"+secondString);
                            int hour = 0;
                            int minute = 0;
                            int second = 0;
                            if (!hourString.equals("")) {
                                hour = Integer.parseInt(hourString);
                            }
                            if (!minuteString.equals("")) {
                                minute = Integer.parseInt(minuteString);
                            }
                            if (!secondString.equals("")) {
                                second = Integer.parseInt(secondString);
                            }
                            if (hour>24) {
                                hour=24;
                            }
                            if (minute>60) {
                                minute=60;
                            }
                            if (second>60) {
                                second = 60;
                            }
                            Log.d(TAG, "hour:" + hour + " minute:" + minute + " second:" + second);
                            Log.d(TAG, "total time is:" + totaltime);
                            progressBar = (SeekBar)findViewById(R.id.SeekBar);
                            curTimeTx = (TextView)findViewById(R.id.CurTime);
                            int skipTimeSecond = (hour*60*60 + minute*60 + second) * 1000;
                            Log.d(TAG, "skip time is:" + skipTimeSecond);
                            if(skipTimeSecond < totaltime) {
                                curTimeTx.setText(secToTime(skipTimeSecond));
                                try {
                                    mMediaPlayer.seekTo(skipTimeSecond);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Log.d(TAG, "exception1");
                                }
                                Log.d(TAG, "OK-");
                            } else if(skipTimeSecond > totaltime) {
                                /*try {
                                    mMediaPlayer.seekTo((totaltime/1000 - 1) * 1000);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Log.d(TAG, "exception2");
                                }*/
                                Toast toast = Toast.makeText(VideoPlayer.this,
                                        R.string.setting_time_toast,Toast.LENGTH_SHORT);
                                toast.setGravity(Gravity.TOP, 0, 20);
                                toast.setDuration(0x00000001);
                                toast.show();
                            }
                            optbar.setVisibility(View.VISIBLE);
                            mtimeSetting.dismiss();
                            optbar.requestFocus();
                            startOsdTimeout();
                        }
                    })
                .setNegativeButton(R.string.setting_time_cancel, 
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialoginterface, int i) {
                            optbar.setVisibility(View.VISIBLE);
                            mtimeSetting.dismiss();
                            optbar.requestFocus();
                            startOsdTimeout();
                        }
                    })
                .setOnKeyListener(new OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK){
                                optbar.setVisibility(View.VISIBLE);
                                mtimeSetting.dismiss();
                                optbar.requestFocus();
                                startOsdTimeout();
                            } 
                            return false;
                        }
                    })
                .show();        
    }
}


//@@-------- this part for option item value read and write-----------------------------------------------------------
class Option {
    private static String TAG = "Option";
    private Activity mAct;
    private static SharedPreferences sp = null;
    
    private boolean resume = false;
    private int repeat = 0;
    private int audiotrack = -1;
    private int soundtrack = -1;
    private int videotrack = -1;
    private int audiodtsasset = -1;
    private int audiodtsapresent = -1;
    private int display = 0;
    private int _3dmode = 0;
    
    public static final int REPEATLIST = 0;
    public static final int REPEATONE = 1;
    public static final int DISP_MODE_NORMAL = 0;
    public static final int DISP_MODE_FULLSTRETCH = 1;
    public static final int DISP_MODE_RATIO4_3 = 2;
    public static final int DISP_MODE_RATIO16_9 = 3;
        public static final int DISP_MODE_ORIGINAL = 4;
    private String RESUME_MODE = "ResumeMode";
    private String REPEAT_MODE = "RepeatMode";
    private String AUDIO_TRACK = "AudioTrack";
    private String SOUND_TRACK = "SoundTrack";
    private String VIDEO_TRACK = "VideoTrack";
    private String AUDIO_DTS_APRESENT = "AudioDtsApresent";
    private String AUDIO_DTS_ASSET = "AudioDtsAsset";
    private String DISPLAY_MODE = "DisplayMode";

    public Option(Activity act) {
        mAct = act;
        sp = mAct.getSharedPreferences("optionSp", Activity.MODE_PRIVATE);
    }

    public boolean getResumeMode() { //book mark
        if(sp != null)
            resume = sp.getBoolean(RESUME_MODE, true);
        return resume;
    }

    public int getRepeatMode() {
        if(sp != null)
            repeat = sp.getInt(REPEAT_MODE, 0);
        return repeat;
    }

    public int getAudioTrack() {
        if(sp != null)
            audiotrack = sp.getInt(AUDIO_TRACK, 0);
        return audiotrack;
    }

    public int getSoundTrack() {
        if(sp != null)
            soundtrack = sp.getInt(SOUND_TRACK, 0);
        return soundtrack;
    }

    public int getVideoTrack() {
        if (sp != null)
            videotrack = sp.getInt(VIDEO_TRACK, 0);
        return videotrack;
    }

    public int getAudioDtsApresent() {
        if(sp != null)
            audiodtsapresent = sp.getInt(AUDIO_DTS_APRESENT, 0);
        return audiodtsapresent;
    }

    public int getAudioDtsAsset() {
        if(sp != null)
            audiodtsasset = sp.getInt(AUDIO_DTS_ASSET, 0);
        return audiodtsasset;
    }

    public int getDisplayMode() {
        if(sp != null)
            display = sp.getInt(DISPLAY_MODE, DISP_MODE_FULLSTRETCH);
        return display;
    }

    public int get3DMode() {
        return _3dmode;
    }

    public void setResumeMode(boolean para) {
        if(sp != null) {
            sp.edit()
                .putBoolean(RESUME_MODE, para)
                .commit();
        }
    }

    public void setRepeatMode(int para) {
        if(sp != null) {
            sp.edit()
                .putInt(REPEAT_MODE, para)
                .commit();
        }
    }

    public void setAudioTrack(int para) {
        if(sp != null) {
            sp.edit()
                .putInt(AUDIO_TRACK, para)
                .commit();
        }
    }

    public void setSoundTrack(int para) {
        if(sp != null) {
            sp.edit()
                .putInt(SOUND_TRACK, para)
                .commit();
        }
    }

    public void setVideoTrack(int para) {
        if(sp != null) {
            sp.edit()
                .putInt(VIDEO_TRACK, para)
                .commit();
        }
    }

    public void setAudioDtsApresent(int para) {
        if(sp != null) {
            sp.edit()
                .putInt(AUDIO_DTS_APRESENT, para)
                .commit();
        }
    }

    public void setAudioDtsAsset(int para) {
        if(sp != null) {
            sp.edit()
                .putInt(AUDIO_DTS_ASSET, para)
                .commit();
        }
    }

    public void setDisplayMode(int para) {
        if(sp != null) {
            sp.edit()
                .putInt(DISPLAY_MODE, para)
                .commit();
        }
    }

    public void set3DMode(int para) {
        _3dmode = para;
    }
}

//@@--------this part for book mark---------------------------------------------------------------------------------
class Bookmark {
    private static String TAG = "Bookmark";
    private Activity mAct;
    private static SharedPreferences sp = null;
    private static final int BOOKMARK_NUM_MAX = 10;

    public Bookmark(Activity act) {
        mAct = act;
        sp = mAct.getSharedPreferences("bookmarkSp", Activity.MODE_PRIVATE);
    }

    private String getSpStr(String name) {
        String str = null;
        if(sp != null) {
            str = sp.getString(name, "");
        }
        return str;
    }

    private int getSpInt(String name) {
        int ret = -1;
        if(sp != null) {
            ret = sp.getInt(name, 0);
        }
        return ret;
    }

    private void putSpStr(String name, String para) {
        if(sp != null) {
            sp.edit()
                .putString(name, para)
                .commit();
        }
    }

    private void putSpInt(String name, int para) {
        if(sp != null) {
            sp.edit()
                .putInt(name, para)
                .commit();
        }
    }
	
    public int get(String filename) {
        for (int i=0; i<BOOKMARK_NUM_MAX; i++) {
            if (filename.equals(getSpStr("filename"+i))) {
                int position = getSpInt("filetime"+i);
                return position;
            }
        }
        return 0;
    }
	
    public int set(String filename, int time) {
        String isNull = null;
        int i = -1;
        for (i=0; i<BOOKMARK_NUM_MAX;) {
            isNull = getSpStr("filename"+i);
            if (isNull == null 
                || isNull.length() == 0
                || isNull.equals(filename))
            break;
            i++;
        }
        if (i<BOOKMARK_NUM_MAX) {
            putSpStr("filename"+i, filename);
            putSpInt("filetime"+i, time);
        }
        else {
            for (int j=0; j<BOOKMARK_NUM_MAX-1; j++) {
                putSpStr("filename"+j, 
                getSpStr("filename"+(j+1)));
                putSpInt("filetime"+j, 
                getSpInt("filetime"+(j+1)));
            }
            putSpStr("filename"+(BOOKMARK_NUM_MAX-1), filename);
            putSpInt("filetime"+(BOOKMARK_NUM_MAX-1), time);
        }
        return 0;
    }
}

class ResumePlay {
    private Activity mAct;
    private static SharedPreferences sp = null;
    boolean enable = false; // the flag will reset to false if invoke onDestroy, in this case to distinguish resume play and bookmark play
    
    public ResumePlay(Activity act) {
        mAct = act;
        sp = mAct.getSharedPreferences("ResumePlaySp", Activity.MODE_PRIVATE);
    }

    private String getSpStr(String name) {
        String str = null;
        if(sp != null) {
            str = sp.getString(name, "");
        }
        return str;
    }

    private int getSpInt(String name) {
        int ret = -1;
        if(sp != null) {
            ret = sp.getInt(name, 0);
        }
        return ret;
    }

    private void putSpStr(String name, String para) {
        if(sp != null) {
            sp.edit()
                .putString(name, para)
                .commit();
        }
    }

    private void putSpInt(String name, int para) {
        if(sp != null) {
            sp.edit()
                .putInt(name, para)
                .commit();
        }
    }

    public void setEnable(boolean en) {
        enable = en;
    }

    public boolean getEnable() {
        return enable;
    }

    public void set(String filename, int time) {
        putSpStr("filename", filename);
        putSpInt("filetime", time);
    }

    public String getFilepath() {
        String path = getSpStr("filename");
        return path;
    }

    public int getTime() {
        int time = getSpInt("filetime");
        return time;
    }
    
}

class subview_set{
    public int totalnum; 
    public int curid;
    public int curidbac;
    public int color;
    public int font; 
    public int position_v;
}

class ChapterInfo {
    public int start;
    public int duration;
}

