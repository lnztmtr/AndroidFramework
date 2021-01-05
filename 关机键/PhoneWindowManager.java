/*
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

package com.android.internal.policy.impl;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.ActivityManagerNative;
import android.app.AppOpsManager;
import android.app.IUiModeManager;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.StatusBarManager;
import android.app.UiModeManager;
import android.app.DevInfoManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.IAudioService;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.FactoryTest;
import android.os.Handler;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.media.MediaPlayer;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamManager;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.IApplicationToken;
import android.view.IWindowManager;
import android.view.InputChannel;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.InputEventReceiver;
import android.view.KeyCharacterMap;
import android.view.KeyCharacterMap.FallbackAction;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.WindowManagerPolicy;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.net.Uri;
import android.database.Cursor;

import com.android.internal.R;
import com.android.internal.policy.PolicyManager;
import com.android.internal.policy.impl.keyguard.KeyguardServiceDelegate;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.telephony.ITelephony;
import com.android.internal.widget.PointerLocationView;
import com.hisilicon.android.hiaudiomanager.HiAudioManager;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.Date;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import android.text.TextUtils;
import static android.view.WindowManager.LayoutParams.*;
import static android.view.WindowManagerPolicy.WindowManagerFuncs.LID_ABSENT;
import static android.view.WindowManagerPolicy.WindowManagerFuncs.LID_OPEN;
import static android.view.WindowManagerPolicy.WindowManagerFuncs.LID_CLOSED;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;
//HISILICON add begin
import android.net.Uri;
import android.database.Cursor;
import android.content.pm.PackageInfo;
import android.content.ComponentName;
import android.content.pm.IPackageManager;
import android.app.ActivityThread;
import java.util.ArrayList;
import java.util.List;
import com.hisilicon.android.hisysmanager.HiSysManager;
import com.hisilicon.android.hidisplaymanager.HiDisplayManager;
//HISILICON add end
import com.softwinner.dragonbox.NativeFetch;
import android.app.ActivityManager.RunningTaskInfo;
import android.preference.PreferenceActivity;
import android.widget.TextView;
import android.hardware.input.IInputManager;
import android.hardware.input.InputManager;
//begain add by zhangy for henan wifi @20191025
import android.widget.EditText;
//end add by zhangy for henan wifi @20191025
import java.io.DataOutputStream;
import android.text.TextUtils;
/**
 * WindowManagerPolicy implementation for the Android phone UI.  This
 * introduces a new method suffix, Lp, for an internal lock of the
 * PhoneWindowManager.  This is used to protect some internal state, and
 * can be acquired with either the Lw and Li lock held, so has the restrictions
 * of both of those when held.
 */
public class PhoneWindowManager implements WindowManagerPolicy {
    static final String TAG = "WindowManager";
    static int adbEanbledSetp = 0;
    static final boolean DEBUG = false;
    static final boolean localLOGV = false;
    static final boolean DEBUG_LAYOUT = false;
    static final boolean DEBUG_INPUT = true;
    static final boolean DEBUG_STARTING_WINDOW = false;
    static final boolean SHOW_STARTING_ANIMATIONS = true;
    static final boolean SHOW_PROCESSES_ON_ALT_MENU = false;
	static final boolean DEBUG_ISTV = true;

    // Whether to allow dock apps with METADATA_DOCK_HOME to temporarily take over the Home key.
    // No longer recommended for desk docks; still useful in car docks.
    static final boolean ENABLE_CAR_DOCK_HOME_CAPTURE = true;
    static final boolean ENABLE_DESK_DOCK_HOME_CAPTURE = false;

    static final int LONG_PRESS_POWER_NOTHING = 0;
    static final int LONG_PRESS_POWER_GLOBAL_ACTIONS = 1;
    static final int LONG_PRESS_POWER_SHUT_OFF = 2;
    static final int LONG_PRESS_POWER_SHUT_OFF_NO_CONFIRM = 3;
    static final int LONG_PRESS_SETTING_GLOBAL_ACTIONS  = 4;

    // These need to match the documentation/constant in
    // core/res/res/values/config.xml
    static final int LONG_PRESS_HOME_NOTHING = 0;
    static final int LONG_PRESS_HOME_RECENT_SYSTEM_UI = 1;
    static final int LONG_PRESS_HOME_ASSIST = 2;

    static final int DOUBLE_TAP_HOME_NOTHING = 0;
    static final int DOUBLE_TAP_HOME_RECENT_SYSTEM_UI = 1;

    static final int APPLICATION_MEDIA_SUBLAYER = -2;
    static final int APPLICATION_MEDIA_OVERLAY_SUBLAYER = -1;
    static final int APPLICATION_PANEL_SUBLAYER = 1;
    static final int APPLICATION_SUB_PANEL_SUBLAYER = 2;
    boolean mKeyMicFlag;
    long mKeyMicPressTime = 0;
	
	private static final int MSG_OPEN_WIFI_DIALOG = 7;
	private int wifiCount=0;

    static public final String SYSTEM_DIALOG_REASON_KEY = "reason";
    static public final String SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions";
    static public final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
    static public final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
    static public final String SYSTEM_DIALOG_REASON_ASSIST = "assist";

    static public final String PROPERTY_SWITCH_IME = "persist.sys.imeswitch.key";
    static public final String PROPERTY_SCREEN_CAPTURE ="persist.sys.screencapture.key";
    static public final String PATH_SCREEN_CAPTURE ="persist.sys.screencapture.path";
    
    private int mkeyAgingDragonboxCount = 0;
    
    /**
     * These are the system UI flags that, when changing, can cause the layout
     * of the screen to change.
     */
    static final int SYSTEM_UI_CHANGING_LAYOUT =
              View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.STATUS_BAR_TRANSLUCENT
            | View.NAVIGATION_BAR_TRANSLUCENT;

	String apkNameListSC[] = {
							"tv.icntv.ott",
							"com.ysten.systemupdate",
							"com.ysten.hmdemo",
							"com.ysten.setting",
							"com.sichuan.chinamobile.android.hejiaqin.tv",
                            "com.ysten.tr069",
                            "com.allwinnertech.dragonter",
                            "com.gitv.launcher",
                            "com.chinamobile.activate",
                            "com.chinamobile.launcherjs",
                            "com.softwinner.agingdragonbox",
                            "com.softwinner.dragonbox",
                            "com.ysten.secure",
                            "com.aspirecn.jshdc.appstore",
                            "com.android.settings",
                            "com.ysten.filebrowser"
							};
    /*begin:add by zhanghk at 20190114:add shaanxi whitelist*/							
	String apkNameListSX[] = {
            "com.sys.migusmartlink.ott.tv",
            "com.ysten.systemupdate",
            "com.ysten.setting",
            "com.android.sj",
            "com.android.smart.terminal.iptv",
            "com.softwinner.agingdragonbox",
            "com.softwinner.dragonbox",
            "com.ysten.secure",
            "com.yd.appstore.ott",
            "com.ysten.filebrowser"
    };
    /*end:add by zhanghk at 20190114:add shaanxi whitelist*/ 
	/*begin:add by zhanghy at 20190123:add anhui whitelist*/                                                        
       String apkNameListAH[] = {
            "com.zte.iptvclient.android.launcher_ah",
            "com.zte.iptvclient.android.remoteserver:remote",
            "com.zte.iptvclient.android.remoteserver",
            "com.zte.iptvclient.android.boot_advertising",
            "tv.icntv.ott",
            "com.iflytek.xiri",
            "tv.ysten.xinba.ah.movie",
            "tv.icntv.ott:terminal",
            "com.istv.basicservice",
            "com.bestv.ott.baseservices",
            "com.bestv.ott.baseservices:migu",
                       "com.bestv.mishitong.ott",
                       "com.yd.appstore.ott",
                       "com.iflytek.xiri2.system",
                       "/data/data/com.certus.ottstb.qosmonloader/files/QosMon/qosmon_android",
                      "com.certus.ottstb.qosmonloader",
                       "/system/bin/basicService",
                       "com.migusmartlink.ott.tv",
                       "com.migusmartlink.ott.tv.helper:remote",
                       "com.migusmartlink.ott.tv.helper",
                       "com.android.systemui",
                       "system_server",
                       "/system/bin/mediaserver",
                       "com.hpplay.happyplay.aw",
                       "com.hpplay.happyplay.aw:service1",
                       "com.hpplay.happyplay.aw:service2",
                       "com.android.settings",
                       "zygote",
                       "com.cmcc.wimo",
                       "com.mipt.dlnarender",
                       "com.hisilicon.android.inputmethod.remote",
                       "android.process.media",
                       "com.hisilicon.android.hiRMService",
                       "com.skyworth.android.tr069",
                       "com.skyworth.autolog"
                      
    };
    /*end:add by zhanghy at 20190123:add anhui whitelist*/            						
	String apkNameListCQJD[] = {
			    "tv.icntv.ott",							
			    "com.ysten.hmdemo",
			    "com.ysten.setting",							
                            "com.ysten.tr069",
                            "com.allwinnertech.dragonter",
                            "com.softwinner.agingdragonbox",
                            "com.softwinner.dragonbox",
                            "com.ysten.remote.service",
                            "com.chinamobile.sh.playservice",
							};
							
    String apkNameListJX[] = {
                        "tv.icntv.ott",
                        "tv.ott.launcher",
                        "com.ysten.systemupdate",
                        "com.ysten.hmdemo",
                        "com.ysten.setting",
                        "com.ysten.tr069",
                        "com.allwinnertech.dragonter",
                        "com.gitv.launcher",
                        "com.chinamobile.launcherjs",
                        "com.softwinner.agingdragonbox",
                        "com.softwinner.dragonbox",
                        "com.ysten.secure",
                        "com.android.settings",
                        "com.ysten.filebrowser",
                        "com.fujian.provision",
                        "com.huawei.stb.tm1",
                        "com.cmri.andhome ",
                        "com.cmci.andhome.key",
                        "com.iflytek.xiri",
                        "com.iflytek.xiri2.system",
                        "com.ysten.xmppapp"
		   };
    /**
     * Keyguard stuff
     */
    private WindowState mKeyguardScrim;
	
	//add by sunlei 191213 for  False standby
    private String isHdmiDetach;
	//end by sunlei 191213 for  False standby

    /* Table of Application Launch keys.  Maps from key codes to intent categories.
     *
     * These are special keys that are used to launch particular kinds of applications,
     * such as a web browser.  HID defines nearly a hundred of them in the Consumer (0x0C)
     * usage page.  We don't support quite that many yet...
     */
    static SparseArray<String> sApplicationLaunchKeyCategories;
    static {
        sApplicationLaunchKeyCategories = new SparseArray<String>();
        sApplicationLaunchKeyCategories.append(
                KeyEvent.KEYCODE_EXPLORER, Intent.CATEGORY_APP_BROWSER);
        sApplicationLaunchKeyCategories.append(
                KeyEvent.KEYCODE_ENVELOPE, Intent.CATEGORY_APP_EMAIL);
        sApplicationLaunchKeyCategories.append(
                KeyEvent.KEYCODE_CONTACTS, Intent.CATEGORY_APP_CONTACTS);
        sApplicationLaunchKeyCategories.append(
                KeyEvent.KEYCODE_CALENDAR, Intent.CATEGORY_APP_CALENDAR);
        sApplicationLaunchKeyCategories.append(
                KeyEvent.KEYCODE_MUSIC, Intent.CATEGORY_APP_MUSIC);
        sApplicationLaunchKeyCategories.append(
                KeyEvent.KEYCODE_CALCULATOR, Intent.CATEGORY_APP_CALCULATOR);
    }

    /**
     * Lock protecting internal state.  Must not call out into window
     * manager with lock held.  (This lock will be acquired in places
     * where the window manager is calling in with its own lock held.)
     */
    private final Object mLock = new Object();

    Context mContext;
    IWindowManager mWindowManager;
    WindowManagerFuncs mWindowManagerFuncs;
    PowerManager mPowerManager;
    IStatusBarService mStatusBarService;
    boolean mPreloadedRecentApps;
    final Object mServiceAquireLock = new Object();
    Vibrator mVibrator; // Vibrator for giving feedback of orientation changes
    SearchManager mSearchManager;

    // Vibrator pattern for haptic feedback of a long press.
    long[] mLongPressVibePattern;

    // Vibrator pattern for haptic feedback of virtual key press.
    long[] mVirtualKeyVibePattern;

    // Vibrator pattern for a short vibration.
    long[] mKeyboardTapVibePattern;

    // Vibrator pattern for haptic feedback during boot when safe mode is disabled.
    long[] mSafeModeDisabledVibePattern;

    // Vibrator pattern for haptic feedback during boot when safe mode is enabled.
    long[] mSafeModeEnabledVibePattern;

    /** If true, hitting shift & menu will broadcast Intent.ACTION_BUG_REPORT */
    boolean mEnableShiftMenuBugReports = false;

    boolean mHeadless;
    boolean mSafeMode;
    WindowState mStatusBar = null;
    boolean mHasSystemNavBar;
    int mStatusBarHeight;
    WindowState mNavigationBar = null;
    boolean mHasNavigationBar = false;
    static int mStartBluetoothSettingFlag = 0;
    boolean mCanHideNavigationBar = false;
    boolean mNavigationBarCanMove = false; // can the navigation bar ever move to the side?
    boolean mNavigationBarOnBottom = true; // is the navigation bar on the bottom *right now*?
    boolean mLongPressSetting = false;
    int[] mNavigationBarHeightForRotation = new int[4];
    int[] mNavigationBarWidthForRotation = new int[4];
    private AutoLogMultKeyTrigger mAutoLogMultKeyTrigger;//added by yzs at 20190424:multikey trigger autolog

    int imeSwitchKey;
    int screenCaptureKey;
    String screenCapturePath;

    WindowState mKeyguard = null;
    KeyguardServiceDelegate mKeyguardDelegate;
    GlobalActions mGlobalActions;
    volatile boolean mPowerKeyHandled; // accessed from input reader and handler thread
    boolean mPendingPowerKeyUpCanceled;
    Handler mHandler;
    WindowState mLastInputMethodWindow = null;
    WindowState mLastInputMethodTargetWindow = null;

    static final int RECENT_APPS_BEHAVIOR_SHOW_OR_DISMISS = 0;
    static final int RECENT_APPS_BEHAVIOR_EXIT_TOUCH_MODE_AND_SHOW = 1;
    static final int RECENT_APPS_BEHAVIOR_DISMISS = 2;
    static final int RECENT_APPS_BEHAVIOR_DISMISS_AND_SWITCH = 3;

    RecentApplicationsDialog mRecentAppsDialog;
    int mRecentAppsDialogHeldModifiers;
    boolean mLanguageSwitchKeyPressed;

    int mLidState = LID_ABSENT;
    boolean mHaveBuiltInKeyboard;

    boolean mSystemReady;
    boolean mSystemBooted;
    boolean mHdmiPlugged;
    int mUiMode;
    int mDockMode = Intent.EXTRA_DOCK_STATE_UNDOCKED;
    int mLidOpenRotation;
    int mCarDockRotation;
    int mDeskDockRotation;
    int mUndockedHdmiRotation;
    int mDemoHdmiRotation;
    boolean mDemoHdmiRotationLock;

    // Default display does not rotate, apps that require non-default orientation will have to
    // have the orientation emulated.
    private boolean mForceDefaultOrientation = false;

    int mUserRotationMode = WindowManagerPolicy.USER_ROTATION_FREE;
    int mUserRotation = Surface.ROTATION_0;
    boolean mAccelerometerDefault;

    int mAllowAllRotations = -1;
    boolean mCarDockEnablesAccelerometer;
    boolean mDeskDockEnablesAccelerometer;
    int mLidKeyboardAccessibility;
    int mLidNavigationAccessibility;
    boolean mLidControlsSleep;
    int mLongPressOnPowerBehavior = -1;
    boolean mScreenOnEarly = false;
    boolean mScreenOnFully = false;
    boolean mOrientationSensorEnabled = false;
    int mCurrentAppOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    boolean mHasSoftInput = false;
    boolean mTouchExplorationEnabled = false;
    boolean mTranslucentDecorEnabled = true;

    int mPointerLocationMode = 0; // guarded by mLock
    boolean isHubeiShowDialog = false;

    // The last window we were told about in focusChanged.
    WindowState mFocusedWindow;
    IApplicationToken mFocusedApp;
    Keyfunction keyfun;
    private boolean mShowPowerDialog = false;
    private HiDisplayManager mDisp = null;
    private final class PointerLocationPointerEventListener implements PointerEventListener {
        @Override
        public void onPointerEvent(MotionEvent motionEvent) {
            if (mPointerLocationView != null) {
                mPointerLocationView.addPointerEvent(motionEvent);
            }
        }
    }

    // Pointer location view state, only modified on the mHandler Looper.
    PointerLocationPointerEventListener mPointerLocationPointerEventListener;
    PointerLocationView mPointerLocationView;

    // The current size of the screen; really; extends into the overscan area of
    // the screen and doesn't account for any system elements like the status bar.
    int mOverscanScreenLeft, mOverscanScreenTop;
    int mOverscanScreenWidth, mOverscanScreenHeight;
    // The current visible size of the screen; really; (ir)regardless of whether the status
    // bar can be hidden but not extending into the overscan area.
    int mUnrestrictedScreenLeft, mUnrestrictedScreenTop;
    int mUnrestrictedScreenWidth, mUnrestrictedScreenHeight;
    // Like mOverscanScreen*, but allowed to move into the overscan region where appropriate.
    int mRestrictedOverscanScreenLeft, mRestrictedOverscanScreenTop;
    int mRestrictedOverscanScreenWidth, mRestrictedOverscanScreenHeight;
    // The current size of the screen; these may be different than (0,0)-(dw,dh)
    // if the status bar can't be hidden; in that case it effectively carves out
    // that area of the display from all other windows.
    int mRestrictedScreenLeft, mRestrictedScreenTop;
    int mRestrictedScreenWidth, mRestrictedScreenHeight;
    // During layout, the current screen borders accounting for any currently
    // visible system UI elements.
    int mSystemLeft, mSystemTop, mSystemRight, mSystemBottom;
    // For applications requesting stable content insets, these are them.
    int mStableLeft, mStableTop, mStableRight, mStableBottom;
    // For applications requesting stable content insets but have also set the
    // fullscreen window flag, these are the stable dimensions without the status bar.
    int mStableFullscreenLeft, mStableFullscreenTop;
    int mStableFullscreenRight, mStableFullscreenBottom;
    // During layout, the current screen borders with all outer decoration
    // (status bar, input method dock) accounted for.
    int mCurLeft, mCurTop, mCurRight, mCurBottom;
    // During layout, the frame in which content should be displayed
    // to the user, accounting for all screen decoration except for any
    // space they deem as available for other content.  This is usually
    // the same as mCur*, but may be larger if the screen decor has supplied
    // content insets.
    int mContentLeft, mContentTop, mContentRight, mContentBottom;
    // During layout, the current screen borders along which input method
    // windows are placed.
    int mDockLeft, mDockTop, mDockRight, mDockBottom;
    // During layout, the layer at which the doc window is placed.
    int mDockLayer;
    // During layout, this is the layer of the status bar.
    int mStatusBarLayer;
    int mLastSystemUiFlags;
    // Bits that we are in the process of clearing, so we want to prevent
    // them from being set by applications until everything has been updated
    // to have them clear.
    int mResettingSystemUiFlags = 0;
    // Bits that we are currently always keeping cleared.
    int mForceClearedSystemUiFlags = 0;
    // What we last reported to system UI about whether the compatibility
    // menu needs to be displayed.
    boolean mLastFocusNeedsMenu = false;

    FakeWindow mHideNavFakeWindow = null;

    static final Rect mTmpParentFrame = new Rect();
    static final Rect mTmpDisplayFrame = new Rect();
    static final Rect mTmpOverscanFrame = new Rect();
    static final Rect mTmpContentFrame = new Rect();
    static final Rect mTmpVisibleFrame = new Rect();
    static final Rect mTmpDecorFrame = new Rect();
    static final Rect mTmpNavigationFrame = new Rect();

    WindowState mTopFullscreenOpaqueWindowState;
    HashSet<IApplicationToken> mAppsToBeHidden = new HashSet<IApplicationToken>();
    boolean mTopIsFullscreen;
    boolean mForceStatusBar;
    boolean mForceStatusBarFromKeyguard;
    boolean mHideLockScreen;
    boolean mForcingShowNavBar;
    int mForcingShowNavBarLayer;

    // States of keyguard dismiss.
    private static final int DISMISS_KEYGUARD_NONE = 0; // Keyguard not being dismissed.
    private static final int DISMISS_KEYGUARD_START = 1; // Keyguard needs to be dismissed.
    private static final int DISMISS_KEYGUARD_CONTINUE = 2; // Keyguard has been dismissed.
    int mDismissKeyguard = DISMISS_KEYGUARD_NONE;

    /** The window that is currently dismissing the keyguard. Dismissing the keyguard must only
     * be done once per window. */
    private WindowState mWinDismissingKeyguard;

    boolean mShowingLockscreen;
    boolean mShowingDream;
    boolean mDreamingLockscreen;
    boolean mHomePressed;
    boolean mHomeConsumed;
    boolean mHomeDoubleTapPending;
    Intent mHomeIntent;
    Intent mCarDockIntent;
    Intent mDeskDockIntent;
    boolean mSearchKeyShortcutPending;
    boolean mConsumeSearchKeyUp;
    boolean mAssistKeyLongPressed;

    // support for activating the lock screen while the screen is on
    boolean mAllowLockscreenWhenOn;
    int mLockScreenTimeout;
    boolean mLockScreenTimerActive;

    // Behavior of ENDCALL Button.  (See Settings.System.END_BUTTON_BEHAVIOR.)
    int mEndcallBehavior;

    // Behavior of POWER button while in-call and screen on.
    // (See Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR.)
    int mIncallPowerBehavior;

    Display mDisplay;

    int mLandscapeRotation = 0;  // default landscape rotation
    int mSeascapeRotation = 0;   // "other" landscape rotation, 180 degrees from mLandscapeRotation
    int mPortraitRotation = 0;   // default portrait rotation
    int mUpsideDownRotation = 0; // "other" portrait rotation

    int mOverscanLeft = 0;
    int mOverscanTop = 0;
    int mOverscanRight = 0;
    int mOverscanBottom = 0;

    // What we do when the user long presses on home
    private int mLongPressOnHomeBehavior;

    // What we do when the user double-taps on home
    private int mDoubleTapOnHomeBehavior;

    // Screenshot trigger states
    // Time to volume and power must be pressed within this interval of each other.
    private static final long SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS = 150;
    // Increase the chord delay when taking a screenshot from the keyguard
    private static final float KEYGUARD_SCREENSHOT_CHORD_DELAY_MULTIPLIER = 2.5f;
    private boolean mScreenshotChordEnabled;
    private boolean mVolumeDownKeyTriggered;
    private long mVolumeDownKeyTime;
    private boolean mVolumeDownKeyConsumedByScreenshotChord;
    private boolean mVolumeUpKeyTriggered;
    private boolean mPowerKeyTriggered;
    private long mPowerKeyTime;

    //HISILICON add begin
    private boolean mBootCompleted = false;
    //HISILICON add end

    /* The number of steps between min and max brightness */
    private static final int BRIGHTNESS_STEPS = 10;
    private String mLastVolume = SystemProperties.get("persist.sys.lastvolume", "10");

    SettingsObserver mSettingsObserver;
    ShortcutManager mShortcutManager;
    PowerManager.WakeLock mBroadcastWakeLock;
    boolean mHavePendingMediaKeyRepeatWithWakeLock;

    private int mCurrentUserId;

    // Maps global key codes to the components that will handle them.
    private GlobalKeyManager mGlobalKeyManager;

    // Fallback actions by key code.
    private final SparseArray<KeyCharacterMap.FallbackAction> mFallbackActions =
            new SparseArray<KeyCharacterMap.FallbackAction>();

    private static final int MSG_ENABLE_POINTER_LOCATION = 1;
    private static final int MSG_DISABLE_POINTER_LOCATION = 2;
    private static final int MSG_DISPATCH_MEDIA_KEY_WITH_WAKE_LOCK = 3;
    private static final int MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK = 4;
    
    /*add by zhaolianghua for anhui wifi control @20200327*/
    private static final int MSG_AH_SHOW_WIFI_IN_SETTING = 10;

    private class PolicyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ENABLE_POINTER_LOCATION:
                    enablePointerLocation();
                    break;
                case MSG_DISABLE_POINTER_LOCATION:
                    disablePointerLocation();
                    break;
                case MSG_DISPATCH_MEDIA_KEY_WITH_WAKE_LOCK:
                    dispatchMediaKeyWithWakeLock((KeyEvent)msg.obj);
                    break;
                case MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK:
                    dispatchMediaKeyRepeatWithWakeLock((KeyEvent)msg.obj);
                    break;
				case MSG_OPEN_WIFI_DIALOG:
     				showWifiDialog();
                     break;
                /*add by zhaolianghua for anhui wifi control start @20200327*/
                case MSG_AH_SHOW_WIFI_IN_SETTING:
                    isWifiOpenAH = true;
                    Settings.System.putString(mContext.getContentResolver(), "ah_wifi_visible", "visible");
                    Toast.makeText(mContext,"wifi is open now",Toast.LENGTH_SHORT).show();
                    break;
                /*add by zhaolianghua end*/
            }
        }
    }

    private UEventObserver mHDMIObserver = new UEventObserver() {
        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            setHdmiPlugged("1".equals(event.get("SWITCH_STATE")));
        }
    };

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            // Observe all users' changes
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.END_BUTTON_BEHAVIOR), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.ACCELEROMETER_ROTATION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.USER_ROTATION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SCREEN_OFF_TIMEOUT), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.POINTER_LOCATION), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.DEFAULT_INPUT_METHOD), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.Secure.IMMERSIVE_MODE_CONFIRMATIONS), false, this,
                    UserHandle.USER_ALL);
            updateSettings();
        }

        @Override public void onChange(boolean selfChange) {
            updateSettings();
            updateRotation(false);
        }
    }

    class MyOrientationListener extends WindowOrientationListener {
        MyOrientationListener(Context context, Handler handler) {
            super(context, handler);
        }

        @Override
        public void onProposedRotationChanged(int rotation) {
            if (localLOGV) Slog.v(TAG, "onProposedRotationChanged, rotation=" + rotation);
            updateRotation(false);
        }
    }
    MyOrientationListener mOrientationListener;

    private final BarController mStatusBarController = new BarController("StatusBar",
            View.STATUS_BAR_TRANSIENT,
            View.STATUS_BAR_UNHIDE,
            View.STATUS_BAR_TRANSLUCENT,
            StatusBarManager.WINDOW_STATUS_BAR,
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

    private final BarController mNavigationBarController = new BarController("NavigationBar",
            View.NAVIGATION_BAR_TRANSIENT,
            View.NAVIGATION_BAR_UNHIDE,
            View.NAVIGATION_BAR_TRANSLUCENT,
            StatusBarManager.WINDOW_NAVIGATION_BAR,
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);

    private ImmersiveModeConfirmation mImmersiveModeConfirmation;

    private SystemGesturesPointerEventListener mSystemGestures;

    IStatusBarService getStatusBarService() {
        synchronized (mServiceAquireLock) {
            if (mStatusBarService == null) {
                mStatusBarService = IStatusBarService.Stub.asInterface(
                        ServiceManager.getService("statusbar"));
            }
            return mStatusBarService;
        }
    }

    /*
     * We always let the sensor be switched on by default except when
     * the user has explicitly disabled sensor based rotation or when the
     * screen is switched off.
     */
    boolean needSensorRunningLp() {
        if (mCurrentAppOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR
                || mCurrentAppOrientation == ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                || mCurrentAppOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                || mCurrentAppOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
            // If the application has explicitly requested to follow the
            // orientation, then we need to turn the sensor or.
            return true;
        }
        if ((mCarDockEnablesAccelerometer && mDockMode == Intent.EXTRA_DOCK_STATE_CAR) ||
                (mDeskDockEnablesAccelerometer && (mDockMode == Intent.EXTRA_DOCK_STATE_DESK
                        || mDockMode == Intent.EXTRA_DOCK_STATE_LE_DESK
                        || mDockMode == Intent.EXTRA_DOCK_STATE_HE_DESK))) {
            // enable accelerometer if we are docked in a dock that enables accelerometer
            // orientation management,
            return true;
        }
        if (mUserRotationMode == USER_ROTATION_LOCKED) {
            // If the setting for using the sensor by default is enabled, then
            // we will always leave it on.  Note that the user could go to
            // a window that forces an orientation that does not use the
            // sensor and in theory we could turn it off... however, when next
            // turning it on we won't have a good value for the current
            // orientation for a little bit, which can cause orientation
            // changes to lag, so we'd like to keep it always on.  (It will
            // still be turned off when the screen is off.)
            return false;
        }
        return true;
    }

    /*
     * Various use cases for invoking this function
     * screen turning off, should always disable listeners if already enabled
     * screen turned on and current app has sensor based orientation, enable listeners
     * if not already enabled
     * screen turned on and current app does not have sensor orientation, disable listeners if
     * already enabled
     * screen turning on and current app has sensor based orientation, enable listeners if needed
     * screen turning on and current app has nosensor based orientation, do nothing
     */
    void updateOrientationListenerLp() {
        if (!mOrientationListener.canDetectOrientation()) {
            // If sensor is turned off or nonexistent for some reason
            return;
        }
        //Could have been invoked due to screen turning on or off or
        //change of the currently visible window's orientation
        if (localLOGV) Slog.v(TAG, "Screen status="+mScreenOnEarly+
                ", current orientation="+mCurrentAppOrientation+
                ", SensorEnabled="+mOrientationSensorEnabled);
        boolean disable = true;
        if (mScreenOnEarly) {
            if (needSensorRunningLp()) {
                disable = false;
                //enable listener if not already enabled
                if (!mOrientationSensorEnabled) {
                    mOrientationListener.enable();
                    if(localLOGV) Slog.v(TAG, "Enabling listeners");
                    mOrientationSensorEnabled = true;
                }
            }
        }
        //check if sensors need to be disabled
        if (disable && mOrientationSensorEnabled) {
            mOrientationListener.disable();
            if(localLOGV) Slog.v(TAG, "Disabling listeners");
            mOrientationSensorEnabled = false;
        }
    }

    private void interceptPowerKeyDown(boolean handled) {
        mPowerKeyHandled = handled;
        if (!handled) {
            mHandler.postDelayed(mPowerLongPress, ViewConfiguration.getGlobalActionKeyTimeout());
        }
    }

    private boolean interceptPowerKeyUp(boolean canceled) {
        if (!mPowerKeyHandled) {
            mHandler.removeCallbacks(mPowerLongPress);
            return !canceled;
        }
        return false;
    }

    private void cancelPendingPowerKeyAction() {
        if (!mPowerKeyHandled) {
            mHandler.removeCallbacks(mPowerLongPress);
        }
        if (mPowerKeyTriggered) {
            mPendingPowerKeyUpCanceled = true;
        }
    }

    private void interceptScreenshotChord() {
        if (mScreenshotChordEnabled
                && mVolumeDownKeyTriggered && mPowerKeyTriggered && !mVolumeUpKeyTriggered) {
            final long now = SystemClock.uptimeMillis();
            if (now <= mVolumeDownKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS
                    && now <= mPowerKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS) {
                mVolumeDownKeyConsumedByScreenshotChord = true;
                cancelPendingPowerKeyAction();

                mHandler.postDelayed(mScreenshotRunnable, getScreenshotChordLongPressDelay());
            }
        }
    }

    private long getScreenshotChordLongPressDelay() {
        if (mKeyguardDelegate.isShowing()) {
            // Double the time it takes to take a screenshot from the keyguard
            return (long) (KEYGUARD_SCREENSHOT_CHORD_DELAY_MULTIPLIER *
                    ViewConfiguration.getGlobalActionKeyTimeout());
        }
        return ViewConfiguration.getGlobalActionKeyTimeout();
    }

    private void cancelPendingScreenshotChordAction() {
        mHandler.removeCallbacks(mScreenshotRunnable);
    }

    private final Runnable mPowerLongPress = new Runnable() {
        @Override
        public void run() {
            // The context isn't read
            if (mLongPressOnPowerBehavior < 0) {
                mLongPressOnPowerBehavior = mContext.getResources().getInteger(
                        com.android.internal.R.integer.config_longPressOnPowerBehavior);
            }
            int resolvedBehavior = mLongPressOnPowerBehavior;
            if (FactoryTest.isLongPressOnPowerOffEnabled()) {
                resolvedBehavior = LONG_PRESS_POWER_SHUT_OFF_NO_CONFIRM;
            }

            switch (resolvedBehavior) {
            case LONG_PRESS_POWER_NOTHING:
                break;
            case LONG_PRESS_POWER_GLOBAL_ACTIONS:
                if(SystemProperties.get("ro.product.target").equals("shcmcc")){
                    mPowerKeyHandled = true;
                    // begin: add by tianchining at 20191111: PowerLongPress
                    if(SystemProperties.get("ro.ysten.province").contains("shandong")){
                        Log.d(TAG, "TCN_ADD: PowerLongPress !!");
                        mHandler.post(mStandbyPopViewRunnable);
                        return;
                    }
                    // end: add by tianchining at 20191111: PowerLongPress
                    if(SystemProperties.get("ro.ysten.province").contains("anhui")){
                       Log.d(TAG, "zhangy: PowerLongPress !!");
                       return;
                    }
                    if(mDisp == null)
                        mDisp = new HiDisplayManager();
					//add by sunlei 191127 for 湖北长按powerkey进入power option{{
                    if(!SystemProperties.get("ro.ysten.province", "master").contains("cm201_hubei")&&mDisp.getOutputEnable(0) == 1) {
				    //end by sunlei 191127 for 湖北长按powerkey进入power option}}
                        keyfun.keyToResolution();
                        Toast toast = Toast.makeText(mContext, mContext.getResources().getString(com.android.internal.R.string.display_720p_toast), Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }
                else{
                //mPowerKeyHandled = true;
                //if (!performHapticFeedbackLw(null, HapticFeedbackConstants.LONG_PRESS, false)) {
                //    performAuditoryFeedbackForAccessibilityIfNeed();
               // }
                //sendCloseSystemWindows(SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS);
                //showGlobalActionsDialog();
              }
              break;
            case LONG_PRESS_POWER_SHUT_OFF:
            case LONG_PRESS_POWER_SHUT_OFF_NO_CONFIRM:
             if(SystemProperties.get("ro.product.target").equals("shcmcc")){
             }else{
                mPowerKeyHandled = true;
                performHapticFeedbackLw(null, HapticFeedbackConstants.LONG_PRESS, false);
                sendCloseSystemWindows(SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS);
                mWindowManagerFuncs.shutdown(resolvedBehavior == LONG_PRESS_POWER_SHUT_OFF);
             }
                break;
            }
        }
    };

    // begin: add by tianchining at 20191111: shandong standby
    private void showStandbyPopView() {
        
        mSleepDelayTextView = new TextView(mContext);
        mSleepDelayTextView.setText(mContext.getResources().getString(com.android.internal.R.string.action_suspend_hold));
        mSleepDelayTextView.setTextSize(25);
        mBuilder = new AlertDialog.Builder(mContext);
        mBuilder.setView(mSleepDelayTextView,110,50,60,50);
        mBuilder.setCancelable(false);
        mDialog = mBuilder.create();
        mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        mDialog.show();
        mHandler.removeCallbacks(powerOff);
        mHandler.postDelayed(powerOff, 2000);
        isCancelDialog=true;
    }

    private Runnable mStandbyPopViewRunnable = new Runnable(){
       public void run(){
           showStandbyPopView();
       }
    };

    private Runnable powerOff = new Runnable(){
       public void run(){
           PhoneWindowManager.sleepmode(0);
           HiSysManager hisys = new HiSysManager();
           hisys.setProperty("persist.suspend.mode", "deep_restart");
           SystemProperties.set("persist.sys.smartsuspendin", "0");
           Intent mIntent = new Intent("android.ysten.systemupdate");
           mIntent.putExtra("powercontrol", "poweroff");
           mContext.sendBroadcast(mIntent);
       }
    };
    // end: add by tianchining at 20191111: shandong standby


    private final Runnable mScreenshotRunnable = new Runnable() {
        @Override
        public void run() {
            takeScreenshot();
        }
    };

    void showGlobalActionsDialog() {
        if (mGlobalActions == null) {
            mGlobalActions = new GlobalActions(mContext, mWindowManagerFuncs);
        }
        final boolean keyguardShowing = keyguardIsShowingTq();
        mGlobalActions.showDialog(keyguardShowing, isDeviceProvisioned());
        if (keyguardShowing) {
            // since it took two seconds of long press to bring this up,
            // poke the wake lock so they have some time to see the dialog.
            mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        }
    }

    boolean isDeviceProvisioned() {
        return Settings.Global.getInt(
                mContext.getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 0) != 0;
    }

    private void handleLongPressOnHome() {
        if (mLongPressOnHomeBehavior != LONG_PRESS_HOME_NOTHING) {
            mHomeConsumed = true;
            performHapticFeedbackLw(null, HapticFeedbackConstants.LONG_PRESS, false);

            if (mLongPressOnHomeBehavior == LONG_PRESS_HOME_RECENT_SYSTEM_UI) {
                toggleRecentApps();
            } else if (mLongPressOnHomeBehavior == LONG_PRESS_HOME_ASSIST) {
                launchAssistAction();
            }
        }
    }

    private void handleDoubleTapOnHome() {
        if (mDoubleTapOnHomeBehavior == DOUBLE_TAP_HOME_RECENT_SYSTEM_UI) {
            mHomeConsumed = true;
            toggleRecentApps();
        }
    }

    private final Runnable mHomeDoubleTapTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (mHomeDoubleTapPending) {
                mHomeDoubleTapPending = false;
                launchHomeFromHotKey();
            }
        }
    };

    /**
     * Create (if necessary) and show or dismiss the recent apps dialog according
     * according to the requested behavior.
     */
    void showOrHideRecentAppsDialog(final int behavior) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mRecentAppsDialog == null) {
                    mRecentAppsDialog = new RecentApplicationsDialog(mContext);
                }
                if (mRecentAppsDialog.isShowing()) {
                    switch (behavior) {
                        case RECENT_APPS_BEHAVIOR_SHOW_OR_DISMISS:
                        case RECENT_APPS_BEHAVIOR_DISMISS:
                            mRecentAppsDialog.dismiss();
                            break;
                        case RECENT_APPS_BEHAVIOR_DISMISS_AND_SWITCH:
                            mRecentAppsDialog.dismissAndSwitch();
                            break;
                        case RECENT_APPS_BEHAVIOR_EXIT_TOUCH_MODE_AND_SHOW:
                        default:
                            break;
                    }
                } else {
                    switch (behavior) {
                        case RECENT_APPS_BEHAVIOR_SHOW_OR_DISMISS:
                            mRecentAppsDialog.show();
                            break;
                        case RECENT_APPS_BEHAVIOR_EXIT_TOUCH_MODE_AND_SHOW:
                            try {
                                mWindowManager.setInTouchMode(false);
                            } catch (RemoteException e) {
                            }
                            mRecentAppsDialog.show();
                            break;
                        case RECENT_APPS_BEHAVIOR_DISMISS:
                        case RECENT_APPS_BEHAVIOR_DISMISS_AND_SWITCH:
                        default:
                            break;
                    }
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void init(Context context, IWindowManager windowManager,
            WindowManagerFuncs windowManagerFuncs) {
        mContext = context;
        mWindowManager = windowManager;
        mWindowManagerFuncs = windowManagerFuncs;
        mHeadless = "1".equals(SystemProperties.get("ro.config.headless", "0"));
        mHandler = new PolicyHandler();
        mOrientationListener = new MyOrientationListener(mContext, mHandler);
        try {
            mOrientationListener.setCurrentRotation(windowManager.getRotation());
        } catch (RemoteException ex) { }
        mSettingsObserver = new SettingsObserver(mHandler);
        mSettingsObserver.observe();
        mShortcutManager = new ShortcutManager(context, mHandler);
        mShortcutManager.observe();
        mUiMode = context.getResources().getInteger(
                com.android.internal.R.integer.config_defaultUiModeType);
        mHomeIntent =  new Intent(Intent.ACTION_MAIN, null);
        mHomeIntent.addCategory(Intent.CATEGORY_HOME);
        if(SystemProperties.get("ro.ysten.province","master").contains("cm201_zhejiang")){
                mHomeIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        }
        mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        mCarDockIntent =  new Intent(Intent.ACTION_MAIN, null);
        mCarDockIntent.addCategory(Intent.CATEGORY_CAR_DOCK);
        mCarDockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        mDeskDockIntent =  new Intent(Intent.ACTION_MAIN, null);
        mDeskDockIntent.addCategory(Intent.CATEGORY_DESK_DOCK);
        mDeskDockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        mPowerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mBroadcastWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "PhoneWindowManager.mBroadcastWakeLock");
        mEnableShiftMenuBugReports = "1".equals(SystemProperties.get("ro.debuggable"));
        mLidOpenRotation = readRotation(
                com.android.internal.R.integer.config_lidOpenRotation);
        mCarDockRotation = readRotation(
                com.android.internal.R.integer.config_carDockRotation);
        mDeskDockRotation = readRotation(
                com.android.internal.R.integer.config_deskDockRotation);
        mUndockedHdmiRotation = readRotation(
                com.android.internal.R.integer.config_undockedHdmiRotation);
        mCarDockEnablesAccelerometer = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_carDockEnablesAccelerometer);
        mDeskDockEnablesAccelerometer = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_deskDockEnablesAccelerometer);
        mLidKeyboardAccessibility = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_lidKeyboardAccessibility);
        mLidNavigationAccessibility = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_lidNavigationAccessibility);
        mLidControlsSleep = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_lidControlsSleep);
        mTranslucentDecorEnabled = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_enableTranslucentDecor);
        readConfigurationDependentBehaviors();

        // register for dock events
        IntentFilter filter = new IntentFilter();
        filter.addAction(UiModeManager.ACTION_ENTER_CAR_MODE);
        filter.addAction(UiModeManager.ACTION_EXIT_CAR_MODE);
        filter.addAction(UiModeManager.ACTION_ENTER_DESK_MODE);
        filter.addAction(UiModeManager.ACTION_EXIT_DESK_MODE);
        filter.addAction(Intent.ACTION_DOCK_EVENT);
        Intent intent = context.registerReceiver(mDockReceiver, filter);
        if (intent != null) {
            // Retrieve current sticky dock event broadcast.
            mDockMode = intent.getIntExtra(Intent.EXTRA_DOCK_STATE,
                    Intent.EXTRA_DOCK_STATE_UNDOCKED);
        }

        imeSwitchKey = SystemProperties.getInt(PROPERTY_SWITCH_IME, KeyEvent.KEYCODE_POUND);
        screenCaptureKey = SystemProperties.getInt(PROPERTY_SCREEN_CAPTURE, KeyEvent.KEYCODE_STAR);
        screenCapturePath = SystemProperties.get(PATH_SCREEN_CAPTURE, "/data");

        // register for dream-related broadcasts
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DREAMING_STARTED);
        filter.addAction(Intent.ACTION_DREAMING_STOPPED);
        context.registerReceiver(mDreamReceiver, filter);

        // register for multiuser-relevant broadcasts
        filter = new IntentFilter(Intent.ACTION_USER_SWITCHED);
        context.registerReceiver(mMultiuserReceiver, filter);

        //HISILICON add begin
        filter = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
        context.registerReceiver(mBootCompletedReceiver, filter);
        //HISILICON add end

        // monitor for system gestures
        mSystemGestures = new SystemGesturesPointerEventListener(context,
                new SystemGesturesPointerEventListener.Callbacks() {
                    @Override
                    public void onSwipeFromTop() {
                        if (mStatusBar != null) {
                            requestTransientBars(mStatusBar);
                        }
                    }
                    @Override
                    public void onSwipeFromBottom() {
                        if (mNavigationBar != null && mNavigationBarOnBottom) {
                            requestTransientBars(mNavigationBar);
                        }
                    }
                    @Override
                    public void onSwipeFromRight() {
                        if (mNavigationBar != null && !mNavigationBarOnBottom) {
                            requestTransientBars(mNavigationBar);
                        }
                    }
                    @Override
                    public void onDebug() {
                        // no-op
                    }
                });
        mImmersiveModeConfirmation = new ImmersiveModeConfirmation(mContext);
        mWindowManagerFuncs.registerPointerEventListener(mSystemGestures);

        mVibrator = (Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);
        mLongPressVibePattern = getLongIntArray(mContext.getResources(),
                com.android.internal.R.array.config_longPressVibePattern);
        mVirtualKeyVibePattern = getLongIntArray(mContext.getResources(),
                com.android.internal.R.array.config_virtualKeyVibePattern);
        mKeyboardTapVibePattern = getLongIntArray(mContext.getResources(),
                com.android.internal.R.array.config_keyboardTapVibePattern);
        mSafeModeDisabledVibePattern = getLongIntArray(mContext.getResources(),
                com.android.internal.R.array.config_safeModeDisabledVibePattern);
        mSafeModeEnabledVibePattern = getLongIntArray(mContext.getResources(),
                com.android.internal.R.array.config_safeModeEnabledVibePattern);

        mScreenshotChordEnabled = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_enableScreenshotChord);

        mGlobalKeyManager = new GlobalKeyManager(mContext);

        // Controls rotation and the like.
        initializeHdmiState();
        keyfun = new Keyfunction(mContext);
        // Match current screen state.
        if (mPowerManager.isScreenOn()) {
            screenTurningOn(null);
        } else {
            screenTurnedOff(WindowManagerPolicy.OFF_BECAUSE_OF_USER);
        }
		 mAutoLogMultKeyTrigger = new AutoLogMultKeyTrigger(mContext);//added by yzs at 20190424:multikey trigger autolog
 //add by wangxin
        filter = new IntentFilter();
        filter.addAction("com.ysten.viper.startplay");
        filter.addAction("com.ysten.viper.endplay");
  //      filter.addAction("smart_suspend_broadcast_enter");
        filter.addAction(Intent.ACTION_SCREEN_ON);
	filter.addAction(Intent.ACTION_SCREEN_OFF);
	context.registerReceiver(mScreenSaverReceiver, filter);

		if (SystemProperties.get("ro.ysten.province","master").equals("cm201_hubei")) { 
		    filter = new IntentFilter();
		    filter.addAction("com.iptv.refersh.token");
		    context.registerReceiver(mRefershTokenReceiver, filter);	
		}	
        if (SystemProperties.get("ro.ysten.province","master").contains("c60_heilongjiang")) { 
		    filter = new IntentFilter();
		    filter.addAction("com.hlj.refersh.token");
		    context.registerReceiver(mHLJRefershTokenReceiver, filter);	
		}	
        Log.d(TAG, "PhoneWindowManager init()");
        mHandler.removeCallbacks(mLaunchScreenSaver);
        int delay = Settings.System.getInt(mContext.getContentResolver(), SCREEN_SAVER_DELAY, -1);
        if(delay > 0){
            mHandler.postDelayed(mLaunchScreenSaver, delay*60*1000);
        } 
      //add by tanhy
      mHandler.removeCallbacks(mSystemSleep);
	  int sleepDelay = Settings.System.getInt(mContext.getContentResolver(),SYSTEM_SLEEP_TIME_DELAY,-1);
       if(SystemProperties.get("ro.ysten.province","master").contains("cm201_shaanxi")){
          sleepDelay = SystemProperties.getInt("persist.sys.autosuspend.timeout",3*60);
       }else if(SystemProperties.get("ro.ysten.province","master").contains("cm201_zhejiang")
           ||SystemProperties.get("ro.ysten.province","master").contains("cm201_henan")){
          sleepDelay=SystemProperties.getInt("persist.sys.sleep",240);
       }else if(SystemProperties.get("persist.sys.autosleep.enable", "false").equals("true")){
           //begin: add by tianchining at 20191106: 通过属性控制自动待机的开关
           sleepDelay = SystemProperties.getInt("persist.sys.autosleep.delay", 240);
           //end: add by tianchining at 20191106: 通过属性控制自动待机的开关
       //begin: add by ysten xumiao at 20181106:liaoning set stand_by time		  
       }else if(SystemProperties.get("ro.ysten.province","master").contains("liaoning")
               || SystemProperties.get("ro.ysten.province","master").equals("cm201_yunnan")){  //add by ysten xyf at 20200619:liaoning set stand_by time
          sleepDelay=Settings.System.getInt(mContext.getContentResolver(),SYSTEM_SLEEP_TIME_DELAY,-1);
       }else if(SystemProperties.get("ro.ysten.province","master").contains("jiangxi")){
          sleepDelay=Settings.System.getInt(mContext.getContentResolver(),SLEEP_TIME,-1);
           Log.d(TAG,"new jiangxi sleepdelay = " + sleepDelay);
       }else{
          if(SystemProperties.get("ro.ysten.province","master").contains("master"))
             sleepDelay=-1;
        }
        //end: add by ysten xumiao at 20181106:liaoning set stand_by time	
        if(sleepDelay > 0&&!SystemProperties.get("ro.ysten.province","master").contains("hengxin")){
           mHandler.postDelayed(mSystemSleep,sleepDelay*60*1000);
    }
    //begin:add ysten liangkai at 20201023 : 凌晨待机
    if(SystemProperties.get("ro.ysten.province","master").contains("neimeng") 
        && SystemProperties.get("persist.sys.thrsp.enable", "false").equals("true")){
	    Log.d("ystenlk","sleepDelay = " + sleepDelay);
      	mHandler.removeCallbacks(mSystemSleep);
	    Timer timer = new Timer();
	    timer.schedule(task,15000);
    }
    //end:add ysten liangkai at 20201023 : 凌晨待机
	   
	    Log.d(TAG, "PhoneWindowManager sleepDelay(): "+sleepDelay);
        
    }
    
	private static String queryDBValue(Context context, String key){
		Log.i(TAG,"queryDBValue " + key);
		String value = "";
		Uri uri = Uri.parse("content://stbconfig/summary");
		try{
			Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
			if(cursor != null){
				while(cursor.moveToNext()){
					String name = cursor.getString(cursor.getColumnIndex("name"));
					if (key.equals(name)) {
						value = cursor.getString(cursor.getColumnIndex("value"));
						break;
					}
				}
				cursor.close();
			}
		} catch(Exception e){
			Log.d(TAG, "queryDBValue Execption "+e.toString());
		}
		return value;

	} 
    private static String HLJqueryDBValue(Context context, String key){
		Log.i(TAG,"queryDBValue " + key);
		String value = "";
		Uri uri = Uri.parse("content://stbconfig/authentication");
		try{
			Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
			if(cursor != null){
				while(cursor.moveToNext()){
					String name = cursor.getString(cursor.getColumnIndex("name"));
					if (key.equals(name)) {
						value = cursor.getString(cursor.getColumnIndex("value"));
						break;
					}
				}
				cursor.close();
			}
		} catch(Exception e){
			Log.d(TAG, "queryDBValue Execption "+e.toString());
		}
		Log.i(TAG,"queryDBValue value:" + value);
		return value;

	}   
	public static String HLJgetRefershTokenTime(Context context)
	{
		String refershTokenTime = HLJqueryDBValue(context, "heartbit_interval");
		//Log.i(TAG,"HLJgetRefershTokenTime:" + refershTokenTime);
		return refershTokenTime;
	}   
	public static String getRefershTokenTime(Context context)
	{
		String refershTokenTime = queryDBValue(context, "refresh_token_time");
		Log.i(TAG,"getRefershTokenTime:" + refershTokenTime);
		return refershTokenTime;
	}	
    private Runnable mHLJLaunchRefershToken = new Runnable() {
        @Override
        public void run() {
        	Log.d(TAG, "mHLGLaunchRefershToken ");
            Intent service = new Intent();
            //service.setClassName(mContext, "com.android.iptvauth.shanxi.RefershTokenService");
            service.setAction("com.ysten.action.hljrefershToken");
            mContext.startService(service);
        } 
    };     
    
    //add by chenliang at 20181120 begin:press some key to start agingdragonbox
    private final Runnable startAgingDragonbox = new Runnable() {
        public void run() {
            Log.d("chenl", "start agingdragonbox");
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.softwinner.agingdragonbox", "com.softwinner.agingdragonbox.Main"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        }
    };
    //add by chenliang at 20181120 end:press some key to start agingdragonbox
    
    private Runnable mLaunchRefershToken = new Runnable() {
        @Override
        public void run() {
        	Log.d(TAG, "mLaunchRefershToken ");
            Intent service = new Intent();
            //service.setClassName(mContext, "com.android.iptvauth.shanxi.RefershTokenService");
            service.setAction("com.ysten.action.refershToken");
            mContext.startService(service);
        } 
    };    
        BroadcastReceiver mHLJRefershTokenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	Log.d(TAG, "mRefershTokenReceiver onReceive action=" + action);
        	String referTime = HLJgetRefershTokenTime(mContext);
           // String referTime = "260000";
            if(referTime == null)
            {
        	Log.d(TAG, "get refresh token time fail");
                referTime = "0";
            }
        	int delay = Integer.parseInt(referTime)*1000;
            if(delay < (60*1000))
            {
               delay = 60*1000; 
            }
            else
            {
                delay = delay - 60000;
            }
        	mHandler.removeCallbacks(mHLJLaunchRefershToken);
        	Log.d(TAG, "mRefershTokenReceiver delay=" + delay);
            mHandler.postDelayed(mHLJLaunchRefershToken, delay);
        }
    };
    BroadcastReceiver mRefershTokenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	String action = intent.getAction();
        	Log.d(TAG, "mRefershTokenReceiver onReceive action=" + action);
        	String referTime = getRefershTokenTime(mContext);
        	int delay = Integer.parseInt(referTime);
        	mHandler.removeCallbacks(mLaunchRefershToken);
        	Log.d(TAG, "mRefershTokenReceiver delay=" + delay);
            if(delay > 0){
                mHandler.postDelayed(mLaunchRefershToken, delay*1000);
            }
        }
    };

    private boolean mInPlaying = false;
    BroadcastReceiver mScreenSaverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive action=" + action);
            if(!TextUtils.isEmpty(action)){
                if(action.equals("com.ysten.viper.startplay")){
                    mInPlaying = true;
                } else if(action.equals("com.ysten.viper.endplay")){
                    mInPlaying = false;
                    int delay = Settings.System.getInt(mContext.getContentResolver(), SCREEN_SAVER_DELAY, -1);
                    mHandler.removeCallbacks(mLaunchScreenSaver);
                    if(delay > 0){
                        mHandler.postDelayed(mLaunchScreenSaver, delay*60*1000);
                    }                    
                }else if(action.equals(Intent.ACTION_SCREEN_ON))
			
			{
			Log.i("THY","PhoneWindowManager receive broadcast: Intent.ACTION_SCREEN_ON");		
			int sleepDelay = Settings.System.getInt(mContext.getContentResolver(),SYSTEM_SLEEP_TIME_DELAY,-1);
            if(SystemProperties.get("ro.ysten.province","master").contains("cm201_shaanxi")){
                    sleepDelay = SystemProperties.getInt("persist.sys.autosuspend.timeout",3*60);
            }else if(SystemProperties.get("ro.ysten.province","master").contains("cm201_zhejiang")
                ||SystemProperties.get("ro.ysten.province","master").contains("cm201_henan")){
                    sleepDelay=SystemProperties.getInt("persist.sys.sleep",240);
					
            }else if (SystemProperties.get("ro.ysten.province","master").contains("jiangxi")){
                sleepDelay=Settings.System.getInt(mContext.getContentResolver(),SLEEP_TIME,-1);
                Log.d(TAG,"new jiangxi sleepdelay in mScreenSaverReceiver is " + sleepDelay);
            }else
			  {
			  if(SystemProperties.get("ro.ysten.province","master").contains("master"))
			  sleepDelay=-1;
			  }
			
			if(sleepDelay > 0)
				mHandler.postDelayed(mSystemSleep,sleepDelay*60*1000);
		}else if(action.equals(Intent.ACTION_SCREEN_OFF))
		  {       
                        Log.i("THY","PhoneWindowManager receive broadcast: Intent.ACTION_SCREEN_OFF");          
        
                }else if(action.equals("smart_suspend_broadcast_enter")){
                        Log.i("THY","gotosleep"); 
                        //swithOffIOAndShutdownLater();                                      
                  }
            }
        }
    };


    /**
     * Read values from config.xml that may be overridden depending on
     * the configuration of the device.
     * eg. Disable long press on home goes to recents on sw600dp.
     */
    private void readConfigurationDependentBehaviors() {
        mLongPressOnHomeBehavior = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_longPressOnHomeBehavior);
        if (mLongPressOnHomeBehavior < LONG_PRESS_HOME_NOTHING ||
                mLongPressOnHomeBehavior > LONG_PRESS_HOME_ASSIST) {
            mLongPressOnHomeBehavior = LONG_PRESS_HOME_NOTHING;
        }

        mDoubleTapOnHomeBehavior = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_doubleTapOnHomeBehavior);
        if (mDoubleTapOnHomeBehavior < DOUBLE_TAP_HOME_NOTHING ||
                mDoubleTapOnHomeBehavior > DOUBLE_TAP_HOME_RECENT_SYSTEM_UI) {
            mDoubleTapOnHomeBehavior = LONG_PRESS_HOME_NOTHING;
        }
    }

    @Override
    public void setInitialDisplaySize(Display display, int width, int height, int density) {
        // This method might be called before the policy has been fully initialized
        // or for other displays we don't care about.
        if (mContext == null || display.getDisplayId() != Display.DEFAULT_DISPLAY) {
            return;
        }
        mDisplay = display;

        final Resources res = mContext.getResources();
        int shortSize, longSize;
        if (width > height) {
            shortSize = height;
            longSize = width;
            mLandscapeRotation = Surface.ROTATION_0;
            mSeascapeRotation = Surface.ROTATION_180;
            if (res.getBoolean(com.android.internal.R.bool.config_reverseDefaultRotation)) {
                mPortraitRotation = Surface.ROTATION_90;
                mUpsideDownRotation = Surface.ROTATION_270;
            } else {
                mPortraitRotation = Surface.ROTATION_270;
                mUpsideDownRotation = Surface.ROTATION_90;
            }
        } else {
            shortSize = width;
            longSize = height;
            mPortraitRotation = Surface.ROTATION_0;
            mUpsideDownRotation = Surface.ROTATION_180;
            if (res.getBoolean(com.android.internal.R.bool.config_reverseDefaultRotation)) {
                mLandscapeRotation = Surface.ROTATION_270;
                mSeascapeRotation = Surface.ROTATION_90;
            } else {
                mLandscapeRotation = Surface.ROTATION_90;
                mSeascapeRotation = Surface.ROTATION_270;
            }
        }

        mStatusBarHeight =
                res.getDimensionPixelSize(com.android.internal.R.dimen.status_bar_height);

        // Height of the navigation bar when presented horizontally at bottom
        mNavigationBarHeightForRotation[mPortraitRotation] =
        mNavigationBarHeightForRotation[mUpsideDownRotation] =
                res.getDimensionPixelSize(com.android.internal.R.dimen.navigation_bar_height);
        mNavigationBarHeightForRotation[mLandscapeRotation] =
        mNavigationBarHeightForRotation[mSeascapeRotation] = res.getDimensionPixelSize(
                com.android.internal.R.dimen.navigation_bar_height_landscape);

        // Width of the navigation bar when presented vertically along one side
        mNavigationBarWidthForRotation[mPortraitRotation] =
        mNavigationBarWidthForRotation[mUpsideDownRotation] =
        mNavigationBarWidthForRotation[mLandscapeRotation] =
        mNavigationBarWidthForRotation[mSeascapeRotation] =
                res.getDimensionPixelSize(com.android.internal.R.dimen.navigation_bar_width);

        // SystemUI (status bar) layout policy
        int shortSizeDp = shortSize * DisplayMetrics.DENSITY_DEFAULT / density;
        int longSizeDp = longSize * DisplayMetrics.DENSITY_DEFAULT / density;

        // Allow the navigation bar to move on small devices (phones).
        mNavigationBarCanMove = shortSizeDp < 600;
        if (shortSizeDp < 600) {
            // 0-599dp: "phone" UI with a separate status & navigation bar
            mHasSystemNavBar = false;
            mNavigationBarCanMove = true;
        } else if (shortSizeDp < 720) {
            // 600+dp: "phone" UI with modifications for larger screens
            mHasSystemNavBar = false;
            mNavigationBarCanMove = false;
        }else{//shortSizeDp >= 720
            mHasSystemNavBar = true;
            mNavigationBarCanMove = false;
        }


        if (!mHasSystemNavBar) {
            mHasNavigationBar = mContext.getResources().getBoolean(
                    com.android.internal.R.bool.config_showNavigationBar);
            // Allow a system property to override this. Used by the emulator.
            // See also hasNavigationBar().
            String navBarOverride = SystemProperties.get("qemu.hw.mainkeys");
            if (! "".equals(navBarOverride)) {
                if      (navBarOverride.equals("1")) mHasNavigationBar = false;
                else if (navBarOverride.equals("0")) mHasNavigationBar = true;
            }
        } else {
            mHasNavigationBar = false;
        }

        mHasNavigationBar = res.getBoolean(com.android.internal.R.bool.config_showNavigationBar);
        // Allow a system property to override this. Used by the emulator.
        // See also hasNavigationBar().
        String navBarOverride = SystemProperties.get("qemu.hw.mainkeys");
        if ("1".equals(navBarOverride)) {
            mHasNavigationBar = false;
        } else if ("0".equals(navBarOverride)) {
            mHasNavigationBar = true;
        }

        // For demo purposes, allow the rotation of the HDMI display to be controlled.
        // By default, HDMI locks rotation to landscape.
        if ("portrait".equals(SystemProperties.get("persist.demo.hdmirotation"))) {
            mDemoHdmiRotation = mPortraitRotation;
        } else {
            mDemoHdmiRotation = mLandscapeRotation;
        }
        mDemoHdmiRotationLock = SystemProperties.getBoolean("persist.demo.hdmirotationlock", false);

        // Only force the default orientation if the screen is xlarge, at least 960dp x 720dp, per
        // http://developer.android.com/guide/practices/screens_support.html#range
        mForceDefaultOrientation = longSizeDp >= 960 && shortSizeDp >= 720 &&
                res.getBoolean(com.android.internal.R.bool.config_forceDefaultOrientation) &&
                // For debug purposes the next line turns this feature off with:
                // $ adb shell setprop config.override_forced_orient true
                // $ adb shell wm size reset
                !"true".equals(SystemProperties.get("config.override_forced_orient"));
    }

    /**
     * @return whether the navigation bar can be hidden, e.g. the device has a
     *         navigation bar and touch exploration is not enabled
     */
    private boolean canHideNavigationBar() {
        return mHasNavigationBar && !mTouchExplorationEnabled;
    }

    @Override
    public boolean isDefaultOrientationForced() {
        return mForceDefaultOrientation;
    }

    @Override
    public void setDisplayOverscan(Display display, int left, int top, int right, int bottom) {
        if (display.getDisplayId() == Display.DEFAULT_DISPLAY) {
            mOverscanLeft = left;
            mOverscanTop = top;
            mOverscanRight = right;
            mOverscanBottom = bottom;
        }
    }

    public void updateSettings() {
        ContentResolver resolver = mContext.getContentResolver();
        boolean updateRotation = false;
        synchronized (mLock) {
            mEndcallBehavior = Settings.System.getIntForUser(resolver,
                    Settings.System.END_BUTTON_BEHAVIOR,
                    Settings.System.END_BUTTON_BEHAVIOR_DEFAULT,
                    UserHandle.USER_CURRENT);
            mIncallPowerBehavior = Settings.Secure.getIntForUser(resolver,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT,
                    UserHandle.USER_CURRENT);

            // Configure rotation lock.
            int userRotation = Settings.System.getIntForUser(resolver,
                    Settings.System.USER_ROTATION, Surface.ROTATION_0,
                    UserHandle.USER_CURRENT);
            if (mUserRotation != userRotation) {
                mUserRotation = userRotation;
                updateRotation = true;
            }
            int userRotationMode = Settings.System.getIntForUser(resolver,
                    Settings.System.ACCELEROMETER_ROTATION, 0, UserHandle.USER_CURRENT) != 0 ?
                            WindowManagerPolicy.USER_ROTATION_FREE :
                                    WindowManagerPolicy.USER_ROTATION_LOCKED;
            if (mUserRotationMode != userRotationMode) {
                mUserRotationMode = userRotationMode;
                updateRotation = true;
                updateOrientationListenerLp();
            }

            if (mSystemReady) {
                int pointerLocation = Settings.System.getIntForUser(resolver,
                        Settings.System.POINTER_LOCATION, 0, UserHandle.USER_CURRENT);
                if (mPointerLocationMode != pointerLocation) {
                    mPointerLocationMode = pointerLocation;
                    mHandler.sendEmptyMessage(pointerLocation != 0 ?
                            MSG_ENABLE_POINTER_LOCATION : MSG_DISABLE_POINTER_LOCATION);
                }
            }
            // use screen off timeout setting as the timeout for the lockscreen
            mLockScreenTimeout = Settings.System.getIntForUser(resolver,
                    Settings.System.SCREEN_OFF_TIMEOUT, 0, UserHandle.USER_CURRENT);
            String imId = Settings.Secure.getStringForUser(resolver,
                    Settings.Secure.DEFAULT_INPUT_METHOD, UserHandle.USER_CURRENT);
            boolean hasSoftInput = imId != null && imId.length() > 0;
            if (mHasSoftInput != hasSoftInput) {
                mHasSoftInput = hasSoftInput;
                updateRotation = true;
            }
            if (mImmersiveModeConfirmation != null) {
                mImmersiveModeConfirmation.loadSetting();
            }
        }
        if (updateRotation) {
            updateRotation(true);
        }
    }

    private void enablePointerLocation() {
        if (mPointerLocationView == null) {
            mPointerLocationView = new PointerLocationView(mContext);
            mPointerLocationView.setPrintCoords(false);

            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
            lp.type = WindowManager.LayoutParams.TYPE_SECURE_SYSTEM_OVERLAY;
            lp.flags = WindowManager.LayoutParams.FLAG_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            if (ActivityManager.isHighEndGfx()) {
                lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
                lp.privateFlags |=
                        WindowManager.LayoutParams.PRIVATE_FLAG_FORCE_HARDWARE_ACCELERATED;
            }
            lp.format = PixelFormat.TRANSLUCENT;
            lp.setTitle("PointerLocation");
            WindowManager wm = (WindowManager)
                    mContext.getSystemService(Context.WINDOW_SERVICE);
            lp.inputFeatures |= WindowManager.LayoutParams.INPUT_FEATURE_NO_INPUT_CHANNEL;
            wm.addView(mPointerLocationView, lp);

            mPointerLocationPointerEventListener = new PointerLocationPointerEventListener();
            mWindowManagerFuncs.registerPointerEventListener(mPointerLocationPointerEventListener);
        }
    }

    private void disablePointerLocation() {
        if (mPointerLocationPointerEventListener != null) {
            mWindowManagerFuncs.unregisterPointerEventListener(
                    mPointerLocationPointerEventListener);
            mPointerLocationPointerEventListener = null;
        }

        if (mPointerLocationView != null) {
            WindowManager wm = (WindowManager)
                    mContext.getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(mPointerLocationView);
            mPointerLocationView = null;
        }
    }

    private int readRotation(int resID) {
        try {
            int rotation = mContext.getResources().getInteger(resID);
            switch (rotation) {
                case 0:
                    return Surface.ROTATION_0;
                case 90:
                    return Surface.ROTATION_90;
                case 180:
                    return Surface.ROTATION_180;
                case 270:
                    return Surface.ROTATION_270;
            }
        } catch (Resources.NotFoundException e) {
            // fall through
        }
        return -1;
    }

    /** {@inheritDoc} */
    @Override
    public int checkAddPermission(WindowManager.LayoutParams attrs, int[] outAppOp) {
        int type = attrs.type;

        outAppOp[0] = AppOpsManager.OP_NONE;

        if (type < WindowManager.LayoutParams.FIRST_SYSTEM_WINDOW
                || type > WindowManager.LayoutParams.LAST_SYSTEM_WINDOW) {
            return WindowManagerGlobal.ADD_OKAY;
        }
        String permission = null;
        switch (type) {
            case TYPE_TOAST:
                // XXX right now the app process has complete control over
                // this...  should introduce a token to let the system
                // monitor/control what they are doing.
                break;
            case TYPE_DREAM:
            case TYPE_INPUT_METHOD:
            case TYPE_WALLPAPER:
            case TYPE_PRIVATE_PRESENTATION:
                // The window manager will check these.
                break;
            case TYPE_PHONE:
            case TYPE_PRIORITY_PHONE:
            case TYPE_SYSTEM_ALERT:
            case TYPE_SYSTEM_ERROR:
            case TYPE_SYSTEM_OVERLAY:
                permission = android.Manifest.permission.SYSTEM_ALERT_WINDOW;
                outAppOp[0] = AppOpsManager.OP_SYSTEM_ALERT_WINDOW;
                break;
            default:
                permission = android.Manifest.permission.INTERNAL_SYSTEM_WINDOW;
        }
        if (permission != null) {
            if (mContext.checkCallingOrSelfPermission(permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return WindowManagerGlobal.ADD_PERMISSION_DENIED;
            }
        }
        return WindowManagerGlobal.ADD_OKAY;
    }

    @Override
    public boolean checkShowToOwnerOnly(WindowManager.LayoutParams attrs) {

        // If this switch statement is modified, modify the comment in the declarations of
        // the type in {@link WindowManager.LayoutParams} as well.
        switch (attrs.type) {
            default:
                // These are the windows that by default are shown only to the user that created
                // them. If this needs to be overridden, set
                // {@link WindowManager.LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS} in
                // {@link WindowManager.LayoutParams}. Note that permission
                // {@link android.Manifest.permission.INTERNAL_SYSTEM_WINDOW} is required as well.
                if ((attrs.privateFlags & PRIVATE_FLAG_SHOW_FOR_ALL_USERS) == 0) {
                    return true;
                }
                break;

            // These are the windows that by default are shown to all users. However, to
            // protect against spoofing, check permissions below.
            case TYPE_APPLICATION_STARTING:
            case TYPE_BOOT_PROGRESS:
            case TYPE_DISPLAY_OVERLAY:
            case TYPE_HIDDEN_NAV_CONSUMER:
            case TYPE_KEYGUARD:
            case TYPE_KEYGUARD_SCRIM:
            case TYPE_KEYGUARD_DIALOG:
            case TYPE_MAGNIFICATION_OVERLAY:
            case TYPE_NAVIGATION_BAR:
            case TYPE_NAVIGATION_BAR_PANEL:
            case TYPE_PHONE:
            case TYPE_POINTER:
            case TYPE_PRIORITY_PHONE:
            case TYPE_RECENTS_OVERLAY:
            case TYPE_SEARCH_BAR:
            case TYPE_STATUS_BAR:
            case TYPE_STATUS_BAR_PANEL:
            case TYPE_STATUS_BAR_SUB_PANEL:
            case TYPE_SYSTEM_DIALOG:
            case TYPE_UNIVERSE_BACKGROUND:
            case TYPE_VOLUME_OVERLAY:
            case TYPE_PRIVATE_PRESENTATION:
                break;
        }

        // Check if third party app has set window to system window type.
        return mContext.checkCallingOrSelfPermission(
                android.Manifest.permission.INTERNAL_SYSTEM_WINDOW)
                        != PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void adjustWindowParamsLw(WindowManager.LayoutParams attrs) {
        switch (attrs.type) {
            case TYPE_SYSTEM_OVERLAY:
            case TYPE_SECURE_SYSTEM_OVERLAY:
                // These types of windows can't receive input events.
                attrs.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                attrs.flags &= ~WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
                break;
        }
    }
    
    void readLidState() {
        mLidState = mWindowManagerFuncs.getLidState();
    }
    
    private boolean isHidden(int accessibilityMode) {
        switch (accessibilityMode) {
            case 1:
                return mLidState == LID_CLOSED;
            case 2:
                return mLidState == LID_OPEN;
            default:
                return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void adjustConfigurationLw(Configuration config, int keyboardPresence,
            int navigationPresence) {
        mHaveBuiltInKeyboard = (keyboardPresence & PRESENCE_INTERNAL) != 0;

        readConfigurationDependentBehaviors();
        readLidState();
        applyLidSwitchState();

        if (config.keyboard == Configuration.KEYBOARD_NOKEYS
                || (keyboardPresence == PRESENCE_INTERNAL
                        && isHidden(mLidKeyboardAccessibility))) {
            config.hardKeyboardHidden = Configuration.HARDKEYBOARDHIDDEN_YES;
            if (!mHasSoftInput) {
                config.keyboardHidden = Configuration.KEYBOARDHIDDEN_YES;
            }
        }

        if (config.navigation == Configuration.NAVIGATION_NONAV
                || (navigationPresence == PRESENCE_INTERNAL
                        && isHidden(mLidNavigationAccessibility))) {
            config.navigationHidden = Configuration.NAVIGATIONHIDDEN_YES;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int windowTypeToLayerLw(int type) {
        if (type >= FIRST_APPLICATION_WINDOW && type <= LAST_APPLICATION_WINDOW) {
            return 2;
        }
        switch (type) {
        case TYPE_UNIVERSE_BACKGROUND:
            return 1;
        case TYPE_PRIVATE_PRESENTATION:
            return 2;
        case TYPE_WALLPAPER:
            // wallpaper is at the bottom, though the window manager may move it.
            return 2;
        case TYPE_PHONE:
            return 3;
        case TYPE_SEARCH_BAR:
            return 4;
        case TYPE_RECENTS_OVERLAY:
        case TYPE_SYSTEM_DIALOG:
            return 5;
        case TYPE_TOAST:
            // toasts and the plugged-in battery thing
            return 6;
        case TYPE_PRIORITY_PHONE:
            // SIM errors and unlock.  Not sure if this really should be in a high layer.
            return 7;
        case TYPE_DREAM:
            // used for Dreams (screensavers with TYPE_DREAM windows)
            return 8;
        case TYPE_SYSTEM_ALERT:
            // like the ANR / app crashed dialogs
            return 9;
        case TYPE_INPUT_METHOD:
            // on-screen keyboards and other such input method user interfaces go here.
            return 10;
        case TYPE_INPUT_METHOD_DIALOG:
            // on-screen keyboards and other such input method user interfaces go here.
            return 11;
        case TYPE_KEYGUARD_SCRIM:
            // the safety window that shows behind keyguard while keyguard is starting
            return 12;
        case TYPE_KEYGUARD:
            // the keyguard; nothing on top of these can take focus, since they are
            // responsible for power management when displayed.
            return 13;
        case TYPE_KEYGUARD_DIALOG:
            return 14;
        case TYPE_STATUS_BAR_SUB_PANEL:
            return 15;
        case TYPE_STATUS_BAR:
            return 16;
        case TYPE_STATUS_BAR_PANEL:
            return 17;
        case TYPE_VOLUME_OVERLAY:
            // the on-screen volume indicator and controller shown when the user
            // changes the device volume
            return 18;
        case TYPE_SYSTEM_OVERLAY:
            // the on-screen volume indicator and controller shown when the user
            // changes the device volume
            return 19;
        case TYPE_NAVIGATION_BAR:
            // the navigation bar, if available, shows atop most things
            return 20;
        case TYPE_NAVIGATION_BAR_PANEL:
            // some panels (e.g. search) need to show on top of the navigation bar
            return 21;
        case TYPE_SYSTEM_ERROR:
            // system-level error dialogs
            return 22;
        case TYPE_MAGNIFICATION_OVERLAY:
            // used to highlight the magnified portion of a display
            return 23;
        case TYPE_DISPLAY_OVERLAY:
            // used to simulate secondary display devices
            return 24;
        case TYPE_DRAG:
            // the drag layer: input for drag-and-drop is associated with this window,
            // which sits above all other focusable windows
            return 25;
        case TYPE_SECURE_SYSTEM_OVERLAY:
            return 26;
        case TYPE_BOOT_PROGRESS:
            return 27;
        case TYPE_POINTER:
            // the (mouse) pointer layer
            return 28;
        case TYPE_HIDDEN_NAV_CONSUMER:
            return 29;
        }
        Log.e(TAG, "Unknown window type: " + type);
        return 2;
    }

    /** {@inheritDoc} */
    @Override
    public int subWindowTypeToLayerLw(int type) {
        switch (type) {
        case TYPE_APPLICATION_PANEL:
        case TYPE_APPLICATION_ATTACHED_DIALOG:
            return APPLICATION_PANEL_SUBLAYER;
        case TYPE_APPLICATION_MEDIA:
            return APPLICATION_MEDIA_SUBLAYER;
        case TYPE_APPLICATION_MEDIA_OVERLAY:
            return APPLICATION_MEDIA_OVERLAY_SUBLAYER;
        case TYPE_APPLICATION_SUB_PANEL:
            return APPLICATION_SUB_PANEL_SUBLAYER;
        }
        Log.e(TAG, "Unknown sub-window type: " + type);
        return 0;
    }

    @Override
    public int getMaxWallpaperLayer() {
        return windowTypeToLayerLw(TYPE_STATUS_BAR);
    }

    @Override
    public int getAboveUniverseLayer() {
        return windowTypeToLayerLw(TYPE_SYSTEM_ERROR);
    }

    public int getNonDecorDisplayWidth(int fullWidth, int fullHeight, int rotation) {
        if (mHasNavigationBar) {
            // For a basic navigation bar, when we are in landscape mode we place
            // the navigation bar to the side.
            if (mNavigationBarCanMove && fullWidth > fullHeight) {
                return fullWidth - mNavigationBarWidthForRotation[rotation];
            }
        }
        return fullWidth;
    }

    public int getNonDecorDisplayHeight(int fullWidth, int fullHeight, int rotation) {
        if (mHasNavigationBar) {
            // For a basic navigation bar, when we are in portrait mode we place
            // the navigation bar to the bottom.
            if (!mNavigationBarCanMove || fullWidth < fullHeight) {
                return fullHeight - mNavigationBarHeightForRotation[rotation];
            }
        }
        return fullHeight;
    }

    public int getConfigDisplayWidth(int fullWidth, int fullHeight, int rotation) {
        return getNonDecorDisplayWidth(fullWidth, fullHeight, rotation);
    }

    public int getConfigDisplayHeight(int fullWidth, int fullHeight, int rotation) {
        // There is a separate status bar at the top of the display.  We don't count that as part
        // of the fixed decor, since it can hide; however, for purposes of configurations,
        // we do want to exclude it since applications can't generally use that part
        // of the screen.
        return getNonDecorDisplayHeight(fullWidth, fullHeight, rotation) - mStatusBarHeight;
    }

    @Override
    public boolean doesForceHide(WindowState win, WindowManager.LayoutParams attrs) {
        return attrs.type == WindowManager.LayoutParams.TYPE_KEYGUARD;
    }

    @Override
    public boolean canBeForceHidden(WindowState win, WindowManager.LayoutParams attrs) {
        switch (attrs.type) {
            case TYPE_STATUS_BAR:
            case TYPE_NAVIGATION_BAR:
            case TYPE_WALLPAPER:
            case TYPE_DREAM:
            case TYPE_UNIVERSE_BACKGROUND:
            case TYPE_KEYGUARD:
            case TYPE_KEYGUARD_SCRIM:
                return false;
            default:
                return true;
        }
    }

    /** {@inheritDoc} */
    @Override
    public View addStartingWindow(IBinder appToken, String packageName, int theme,
            CompatibilityInfo compatInfo, CharSequence nonLocalizedLabel, int labelRes,
            int icon, int logo, int windowFlags) {
        if (!SHOW_STARTING_ANIMATIONS) {
            return null;
        }
        if (packageName == null) {
            return null;
        }

        WindowManager wm = null;
        View view = null;

        try {
            Context context = mContext;
            if (DEBUG_STARTING_WINDOW) Slog.d(TAG, "addStartingWindow " + packageName
                    + ": nonLocalizedLabel=" + nonLocalizedLabel + " theme="
                    + Integer.toHexString(theme));
            if (theme != context.getThemeResId() || labelRes != 0) {
                try {
                    context = context.createPackageContext(packageName, 0);
                    context.setTheme(theme);
                } catch (PackageManager.NameNotFoundException e) {
                    // Ignore
                }
            }

            Window win = PolicyManager.makeNewWindow(context);
            final TypedArray ta = win.getWindowStyle();
            if (ta.getBoolean(
                        com.android.internal.R.styleable.Window_windowDisablePreview, false)
                || ta.getBoolean(
                        com.android.internal.R.styleable.Window_windowShowWallpaper,false)) {
                return null;
            }

            Resources r = context.getResources();
            win.setTitle(r.getText(labelRes, nonLocalizedLabel));

            win.setType(
                WindowManager.LayoutParams.TYPE_APPLICATION_STARTING);
            // Force the window flags: this is a fake window, so it is not really
            // touchable or focusable by the user.  We also add in the ALT_FOCUSABLE_IM
            // flag because we do know that the next window will take input
            // focus, so we want to get the IME window up on top of us right away.
            win.setFlags(
                windowFlags|
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                windowFlags|
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);

            win.setDefaultIcon(icon);
            win.setDefaultLogo(logo);

            win.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);

            final WindowManager.LayoutParams params = win.getAttributes();
            params.token = appToken;
            params.packageName = packageName;
            params.windowAnimations = win.getWindowStyle().getResourceId(
                    com.android.internal.R.styleable.Window_windowAnimationStyle, 0);
            params.privateFlags |=
                    WindowManager.LayoutParams.PRIVATE_FLAG_FAKE_HARDWARE_ACCELERATED;
            params.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;

            if (!compatInfo.supportsScreen()) {
                params.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_COMPATIBLE_WINDOW;
            }

            params.setTitle("Starting " + packageName);

            wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
            view = win.getDecorView();

            if (win.isFloating()) {
                // Whoops, there is no way to display an animation/preview
                // of such a thing!  After all that work...  let's skip it.
                // (Note that we must do this here because it is in
                // getDecorView() where the theme is evaluated...  maybe
                // we should peek the floating attribute from the theme
                // earlier.)
                return null;
            }

            if (DEBUG_STARTING_WINDOW) Slog.d(
                TAG, "Adding starting window for " + packageName
                + " / " + appToken + ": "
                + (view.getParent() != null ? view : null));

            wm.addView(view, params);

            // Only return the view if it was successfully added to the
            // window manager... which we can tell by it having a parent.
            return view.getParent() != null ? view : null;
        } catch (WindowManager.BadTokenException e) {
            // ignore
            Log.w(TAG, appToken + " already running, starting window not displayed");
        } catch (RuntimeException e) {
            // don't crash if something else bad happens, for example a
            // failure loading resources because we are loading from an app
            // on external storage that has been unmounted.
            Log.w(TAG, appToken + " failed creating starting window", e);
        } finally {
            if (view != null && view.getParent() == null) {
                Log.w(TAG, "view not successfully added to wm, removing view");
                wm.removeViewImmediate(view);
            }
        }

        return null;
    }

    /** {@inheritDoc} */
    public void removeStartingWindow(IBinder appToken, View window) {
        if (DEBUG_STARTING_WINDOW) {
            RuntimeException e = new RuntimeException("here");
            e.fillInStackTrace();
            Log.v(TAG, "Removing starting window for " + appToken + ": " + window, e);
        }

        if (window != null) {
            WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
            wm.removeView(window);
        }
    }

    /**
     * Preflight adding a window to the system.
     * 
     * Currently enforces that three window types are singletons:
     * <ul>
     * <li>STATUS_BAR_TYPE</li>
     * <li>KEYGUARD_TYPE</li>
     * </ul>
     * 
     * @param win The window to be added
     * @param attrs Information about the window to be added
     * 
     * @return If ok, WindowManagerImpl.ADD_OKAY.  If too many singletons,
     * WindowManagerImpl.ADD_MULTIPLE_SINGLETON
     */
    public int prepareAddWindowLw(WindowState win, WindowManager.LayoutParams attrs) {
        switch (attrs.type) {
            case TYPE_STATUS_BAR:
                mContext.enforceCallingOrSelfPermission(
                        android.Manifest.permission.STATUS_BAR_SERVICE,
                        "PhoneWindowManager");
                if (mStatusBar != null) {
                    if (mStatusBar.isAlive()) {
                        return WindowManagerGlobal.ADD_MULTIPLE_SINGLETON;
                    }
                }
                mStatusBar = win;
                mStatusBarController.setWindow(win);
                break;
            case TYPE_NAVIGATION_BAR:
                mContext.enforceCallingOrSelfPermission(
                        android.Manifest.permission.STATUS_BAR_SERVICE,
                        "PhoneWindowManager");
                if (mNavigationBar != null) {
                    if (mNavigationBar.isAlive()) {
                        return WindowManagerGlobal.ADD_MULTIPLE_SINGLETON;
                    }
                }
                mNavigationBar = win;
                mNavigationBarController.setWindow(win);
                if (DEBUG_LAYOUT) Slog.i(TAG, "NAVIGATION BAR: " + mNavigationBar);
                break;
            case TYPE_NAVIGATION_BAR_PANEL:
                mContext.enforceCallingOrSelfPermission(
                        android.Manifest.permission.STATUS_BAR_SERVICE,
                        "PhoneWindowManager");
                break;
            case TYPE_STATUS_BAR_PANEL:
                mContext.enforceCallingOrSelfPermission(
                        android.Manifest.permission.STATUS_BAR_SERVICE,
                        "PhoneWindowManager");
                break;
            case TYPE_STATUS_BAR_SUB_PANEL:
                mContext.enforceCallingOrSelfPermission(
                        android.Manifest.permission.STATUS_BAR_SERVICE,
                        "PhoneWindowManager");
                break;
            case TYPE_KEYGUARD:
                if (mKeyguard != null) {
                    return WindowManagerGlobal.ADD_MULTIPLE_SINGLETON;
                }
                mKeyguard = win;
                break;
            case TYPE_KEYGUARD_SCRIM:
                if (mKeyguardScrim != null) {
                    return WindowManagerGlobal.ADD_MULTIPLE_SINGLETON;
                }
                mKeyguardScrim = win;
                break;

        }
        return WindowManagerGlobal.ADD_OKAY;
    }

    /** {@inheritDoc} */
    public void removeWindowLw(WindowState win) {
        if (mStatusBar == win) {
            mStatusBar = null;
            mStatusBarController.setWindow(null);
        } else if (mKeyguard == win) {
            Log.v(TAG, "Removing keyguard window (Did it crash?)");
            mKeyguard = null;
            mKeyguardDelegate.showScrim();
        } else if (mKeyguardScrim == win) {
            Log.v(TAG, "Removing keyguard scrim");
            mKeyguardScrim = null;
        } if (mNavigationBar == win) {
            mNavigationBar = null;
            mNavigationBarController.setWindow(null);
        }
    }

    static final boolean PRINT_ANIM = false;
    
    /** {@inheritDoc} */
    @Override
    public int selectAnimationLw(WindowState win, int transit) {
        if (PRINT_ANIM) Log.i(TAG, "selectAnimation in " + win
              + ": transit=" + transit);
        if (win == mStatusBar) {
            if (transit == TRANSIT_EXIT
                    || transit == TRANSIT_HIDE) {
                return R.anim.dock_top_exit;
            } else if (transit == TRANSIT_ENTER
                    || transit == TRANSIT_SHOW) {
                return R.anim.dock_top_enter;
            }
        } else if (win == mNavigationBar) {
            // This can be on either the bottom or the right.
            if (mNavigationBarOnBottom) {
                if (transit == TRANSIT_EXIT
                        || transit == TRANSIT_HIDE) {
                    return R.anim.dock_bottom_exit;
                } else if (transit == TRANSIT_ENTER
                        || transit == TRANSIT_SHOW) {
                    return R.anim.dock_bottom_enter;
                }
            } else {
                if (transit == TRANSIT_EXIT
                        || transit == TRANSIT_HIDE) {
                    return R.anim.dock_right_exit;
                } else if (transit == TRANSIT_ENTER
                        || transit == TRANSIT_SHOW) {
                    return R.anim.dock_right_enter;
                }
            }
        }

        if (transit == TRANSIT_PREVIEW_DONE) {
            if (win.hasAppShownWindows()) {
                if (PRINT_ANIM) Log.i(TAG, "**** STARTING EXIT");
                return com.android.internal.R.anim.app_starting_exit;
            }
        } else if (win.getAttrs().type == TYPE_DREAM && mDreamingLockscreen
                && transit == TRANSIT_ENTER) {
            // Special case: we are animating in a dream, while the keyguard
            // is shown.  We don't want an animation on the dream, because
            // we need it shown immediately with the keyguard animating away
            // to reveal it.
            return -1;
        }

        return 0;
    }

    @Override
    public void selectRotationAnimationLw(int anim[]) {
        if (PRINT_ANIM) Slog.i(TAG, "selectRotationAnimation mTopFullscreen="
                + mTopFullscreenOpaqueWindowState + " rotationAnimation="
                + (mTopFullscreenOpaqueWindowState == null ?
                        "0" : mTopFullscreenOpaqueWindowState.getAttrs().rotationAnimation));
        if (mTopFullscreenOpaqueWindowState != null && mTopIsFullscreen) {
            switch (mTopFullscreenOpaqueWindowState.getAttrs().rotationAnimation) {
                case ROTATION_ANIMATION_CROSSFADE:
                    anim[0] = R.anim.rotation_animation_xfade_exit;
                    anim[1] = R.anim.rotation_animation_enter;
                    break;
                case ROTATION_ANIMATION_JUMPCUT:
                    anim[0] = R.anim.rotation_animation_jump_exit;
                    anim[1] = R.anim.rotation_animation_enter;
                    break;
                case ROTATION_ANIMATION_ROTATE:
                default:
                    anim[0] = anim[1] = 0;
                    break;
            }
        } else {
            anim[0] = anim[1] = 0;
        }
    }

    @Override
    public boolean validateRotationAnimationLw(int exitAnimId, int enterAnimId,
            boolean forceDefault) {
        switch (exitAnimId) {
            case R.anim.rotation_animation_xfade_exit:
            case R.anim.rotation_animation_jump_exit:
                // These are the only cases that matter.
                if (forceDefault) {
                    return false;
                }
                int anim[] = new int[2];
                selectRotationAnimationLw(anim);
                return (exitAnimId == anim[0] && enterAnimId == anim[1]);
            default:
                return true;
        }
    }

    @Override
    public Animation createForceHideEnterAnimation(boolean onWallpaper) {
        return AnimationUtils.loadAnimation(mContext, onWallpaper
                ? com.android.internal.R.anim.lock_screen_wallpaper_behind_enter
                : com.android.internal.R.anim.lock_screen_behind_enter);
    }

    private static void awakenDreams() {
        IDreamManager dreamManager = getDreamManager();
        if (dreamManager != null) {
            try {
                dreamManager.awaken();
            } catch (RemoteException e) {
                // fine, stay asleep then
            }
        }
    }

    static IDreamManager getDreamManager() {
        return IDreamManager.Stub.asInterface(
                ServiceManager.checkService(DreamService.DREAM_SERVICE));
    }

    static ITelephony getTelephonyService() {
        return ITelephony.Stub.asInterface(
                ServiceManager.checkService(Context.TELEPHONY_SERVICE));
    }

    static IAudioService getAudioService() {
        IAudioService audioService = IAudioService.Stub.asInterface(
                ServiceManager.checkService(Context.AUDIO_SERVICE));
        if (audioService == null) {
            Log.w(TAG, "Unable to find IAudioService interface.");
        }
        return audioService;
    }

    boolean keyguardOn() {
        return keyguardIsShowingTq() || inKeyguardRestrictedKeyInputMode();
    }

    private static final int[] WINDOW_TYPES_WHERE_HOME_DOESNT_WORK = {
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
        };

    private boolean mLongPressBack = false;
 //add by wangxin, for ScreenSavers
    private Runnable mLaunchScreenSaver = new Runnable() {
        @Override
        public void run() {
            if(mInPlaying)return;
            int delay = Settings.System.getInt(mContext.getContentResolver(), SCREEN_SAVER_DELAY, -1);
            if(delay < 0)return;
			
	     final AudioManager am = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);//determin music is action
            boolean isPlayingAudio = false;
            if(null != am)isPlayingAudio = am.isMusicActive();
	     
            boolean isUpgrading=(Settings.System.getInt(mContext.getContentResolver(), ROOM_UPGRADE_FLAG, -1)==1);
            Log.d(TAG, "about to start ScreenSaver = " + delay);
            if (MediaPlayer.isPlayingVideo()||isUpgrading || isPlayingAudio 
			    || "running".equals(SystemProperties.get("service.media.playstatus"))) {
                mHandler.removeCallbacks(mLaunchScreenSaver);
                mHandler.postDelayed(mLaunchScreenSaver, delay*60*1000);
                return;
            }
			
				Intent intent = new Intent("com.ysten.screensaver.ScreenSaver");
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mContext.startActivity(intent);
        } 
    };
    
  //add by tanhy
         private Runnable mSystemSleep = new Runnable()
        {
                 public void run()
             { 
                if(SystemProperties.get("ro.ysten.province","master").contains("c60_heilongjiang")
                    ||SystemProperties.get("ro.ysten.province","master").contains("xinjiang")||SystemProperties.get("ro.ysten.province","master").contains("jidi"))
                {
                    return; 
                }
                
                if("cm201_henan".equals(SystemProperties.get("ro.ysten.province"))){
                    showHenanSleepDelayDialog();
                    return;
                }
				
				//delete by lizheng 20190327 to solve not suspend when playing video				 
                //end:add by ysten zhanghk at 20180816:can standby when playing
           	  int sleepDelay = Settings.System.getInt(mContext.getContentResolver(), SYSTEM_SLEEP_TIME_DELAY, -1);
              if(SystemProperties.get("ro.ysten.province","master").contains("cm201_shaanxi")){
                      sleepDelay = SystemProperties.getInt("persist.sys.autosuspend.timeout",3*60);
              }else if(SystemProperties.get("ro.ysten.province","master").contains("cm201_zhejiang")){
                      sleepDelay=SystemProperties.getInt("persist.sys.sleep",240);
					  
              }else if(SystemProperties.get("persist.sys.autosleep.enable", "false").equals("true")){
              //begin: add by tianchining at 20191106: 设置的待机时间
                  sleepDelay = SystemProperties.getInt("persist.sys.autosleep.delay", 240);
                  Log.d(TAG, "TCN_ADD: enter mSystemSleep, sleepDelay: " + sleepDelay);
              //end: add by tianchining at 20191106: 设置的待机时间
			  //add by ysten wuguoqing for liaoning at 20190327:4 hours go to sleep
			  }else if(SystemProperties.get("ro.ysten.province","master").contains("liaoning")&&SystemProperties.get("persist.sys.yst.noautostandby","1").contains("0")){
					Log.d("xwj", "persist.sys.yst.noautostandby: " + SystemProperties.get("persist.sys.yst.noautostandby","1"));
					showLiaoningSleepDelayDialog();
					return;
			  //end by ysten wuguoqing for liaoning at 20190327:4 hours go to sleep
			  //add by ysten wuguoqing for liaoning at 20200619:4 hours go to sleep
                         }else if(SystemProperties.get("ro.ysten.province","master").contains("jilin") 
                            || SystemProperties.get("ro.ysten.province","master").contains("neimeng")){
					Log.d(TAG, "ystenlk: enter mSystemSleep, sleepDelay: " + sleepDelay);
                                       showLiaoningSleepDelayDialog();
                                       return;
                         }//end by ysten liangk for jilin at 20200831:4 hours go to sleep
              else if(SystemProperties.get("ro.ysten.province","master").equals("cm201_yunnan")){
                  Log.d(TAG, "yunan timing standby");
                  showYunnanSleepDelayDialog();
                  return;
              }//end by ysten xyf for yunan at 20200619:4 hours go to sleep
              //add by ysten guxin for jiangxi 
              else if(SystemProperties.get("ro.ysten.province","master").contains("jiangxi")){
                   sleepDelay = Settings.System.getInt(mContext.getContentResolver(),SLEEP_TIME, -1);
                   Log.d(TAG,"new jiangxi SLEEP_TIME  = "+sleepDelay);
              }
			  else
	     //begin: add by ysten xumiao at 20181106:shanxi not set stand_by
              {
                 if(SystemProperties.get("ro.ysten.province","master").contains("master")
                   || SystemProperties.get("ro.ysten.province","master").contains("shanxi"))
                    sleepDelay=-1;
	           }
              //end: add by ysten xumiao at 20181106:shanxi not set stand_by 
           	  if(sleepDelay < 0)return;
           	  boolean isUpgrading=(Settings.System.getInt(mContext.getContentResolver(), ROOM_UPGRADE_FLAG, -1)==1);
           	  Log.d(TAG, "about to start sleep  = " + sleepDelay);
              final AudioManager am = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);//determin music is action
              boolean isPlayingAudio = false;
              if(null != am)isPlayingAudio = am.isMusicActive();
              if(SystemProperties.get("ro.ysten.province","master").contains("cm201_zhejiang")
			||SystemProperties.get("ro.ysten.province","master").contains("jiangxi")){
                 if(isUpgrading){
                       mHandler.removeCallbacks(mSystemSleep);
                       mHandler.postDelayed(mSystemSleep, sleepDelay*60*1000);
                       return;
                 }   
              }else if(SystemProperties.get("ro.ysten.province","master").contains("fujian")){
                 if(isUpgrading){
                       mHandler.removeCallbacks(mSystemSleep);
                       mHandler.postDelayed(mSystemSleep, sleepDelay*60*1000);
                       return;
                 }   
              }else if(SystemProperties.get("ro.ysten.province", "master").contains("cm201_hubei")){
				  if(isUpgrading){
                       mHandler.removeCallbacks(mSystemSleep);
                       mHandler.postDelayed(mSystemSleep, sleepDelay*60*1000);
                       return;
				  }
			  }else if(SystemProperties.get("ro.ysten.province", "master").contains("cm201_beijing")){
                 if(isUpgrading){
                       mHandler.removeCallbacks(mSystemSleep);
                       mHandler.postDelayed(mSystemSleep, sleepDelay*60*1000);
                       return;
                 }
              }else{
                 if (MediaPlayer.isPlayingVideo()||isUpgrading || isPlayingAudio 
			        || "running".equals(SystemProperties.get("service.media.playstatus"))) {
               		    mHandler.removeCallbacks(mSystemSleep);
             		    mHandler.postDelayed(mSystemSleep, sleepDelay*60*1000);
            		    return;
        	    }
              }
		 if("c60_jiangsu".equals(SystemProperties.get("ro.ysten.province"))
			||SystemProperties.get("ro.ysten.province","master").contains("jiangxi")
			||SystemProperties.get("ro.ysten.province","master").contains("heilongjiang")){
			//add by liangk at 20200428
			showSleepDelayDialog();
         }else if("cm201_hebei".equals(SystemProperties.get("ro.ysten.province"))){
			//add by zhaolianghua hebei need text warning @20190130
            showSleepDelayDialog();
         }else if(SystemProperties.get("ro.ysten.province","master").contains("fujian")){
            //showSuspendDialog_FJ();
			gotoHome();
            Message msg = new Message();
            msg.what = 100;
            toastHandle.sendMessageDelayed(msg, 20);
         }else if(SystemProperties.get("persist.sys.autosleep.enable", "false").equals("true")){
         //begin: add by tianchining at 20191106: 收到了待机的消息，弹出对话框
             showSleepDelayDialog();
         //end: add by tianchining at 20191106: 收到了待机的消息，弹出对话框
		 }else{
            sleepmode(1);
		 //PowerManager mananger = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
                 //mananger.goToSleep(SystemClock.uptimeMillis());
            HiSysManager hisys = new HiSysManager();
            if(SystemProperties.get("ro.ysten.province","master").equals("cm201_hubei")
               || SystemProperties.get("ro.ysten.province","master").equals("cm201_yunnan")
               || SystemProperties.get("ro.ysten.province","master").equals("c60_yunnan")
               || SystemProperties.get("ro.ysten.province","master").equals("cm201_beijing")
               //begin:add by zengzhiliang at 20200403:guizhou real standby
               || SystemProperties.get("ro.ysten.province","master").equals("cm201_guizhou")
               //end:add by zengzhiliang at 20200403:guizhou real standby
			   || SystemProperties.get("ro.ysten.province","master").equals("cm201_henan")
               || SystemProperties.get("ro.ysten.province","master").equals("cm201_hunan")
               //add by zhanghk at 20180813:fix sleep problem
               || SystemProperties.get("ro.ysten.province","master").equals("cm201_zhejiang")
               || SystemProperties.get("ro.ysten.province","master").equals("cm201_shaanxi")){
                hisys.setProperty("persist.suspend.mode", "deep_restart");
                SystemProperties.set("persist.sys.smartsuspendin", "0");
            } else {
                hisys.setProperty("persist.suspend.mode", "smart_suspend");
                SystemProperties.set("persist.sys.smartsuspendin", "1");
            }
         Intent mIntent = new Intent("android.ysten.systemupdate");
                       mIntent.putExtra("powercontrol", "poweroff");
                       mContext.sendBroadcast(mIntent);

        }
             
      } 
  };
		 
    private Runnable mSleepCancleTimer = new Runnable()
    {
        public void run()
        {
            mCount --;
            if(mCount>=0) {
                mHandler.removeCallbacks(mSleepCancleTimer);
                mHandler.postDelayed(mSleepCancleTimer, 1000);
                Slog.d(TAG, "mSleepDelayTextView 5 " + mSleepDelayTextView);
                /*modify by zhaolianghua for hebei&jiangxi text warning @20190130*/
               if("cm201_hebei".equals(SystemProperties.get("ro.ysten.province"))){
                  mSleepDelayTextView.setText(mContext.getResources().getString(com.android.internal.R.string.standby_warn_he,mCount));
               }else if(SystemProperties.get("ro.ysten.province").contains("jiangxi")){
		  mSleepDelayTextView.setText(mContext.getResources().getString(com.android.internal.R.string.standby_warn_jx,mCount));
	           } else if(SystemProperties.get("persist.sys.autosleep.enable", "false").equals("true")){
               // begin: add by tianchining at 20191106: 设置待机提示框文字 
                   mSleepDelayTextView.setText(mContext.getResources().getString(com.android.internal.R.string.standby_warn_jx, mCount));
               // end: add by tianchining at 20191106: 设置待机提示框文字
               }else{
                  mSleepDelayTextView.setText(mContext.getResources().getString(com.android.internal.R.string.standby_warn,mCount));
               }
               /*modify end*/
               mSleepDelayTextView.setTextSize(25);
            } else {
                  mDialog.dismiss();
                  showSuspendDialog_JS();
            }
        }
    };
		private AlertDialog.Builder mBuilder ,mWifiBuilder;
		AlertDialog mDialog ,mWifiDialog;
		private TextView mSleepDelayTextView;
		private EditText wifiEt;
		int mCount = 30;
		 	//add by ysten.zhangy 20191028 for CM201-2 henan
	void showWifiDialog(){
		Slog.d(TAG, "显示wifi弹框  start" );
            wifiEt = new EditText(mContext);                          
            wifiEt.setTextSize(20); 
			
            mWifiBuilder = new AlertDialog.Builder(mContext);                     
            mWifiBuilder.setTitle("wifi控制");        
            mWifiBuilder.setView(wifiEt,110,50,60,50);
            mWifiBuilder.setNegativeButton(mContext.getResources().getString(com.android.internal.R.string.cancel_hn), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {                           
                           mWifiDialog.dismiss();                           
                    }                    
                 }).setPositiveButton(mContext.getResources().getString(com.android.internal.R.string.confirm_hn), new DialogInterface.OnClickListener() {
           
                    public void onClick(DialogInterface dialogInterface, int i) {
				           String pwd=wifiEt.getText().toString();
				           if("10086".equals(pwd)){
					       SystemProperties.set("persist.sys.wifistate", "1");
				           }else{
					       Toast.makeText(mContext,mContext.getResources().getString(com.android.internal.R.string.password_error_please_reenter),Toast.LENGTH_SHORT).show();
				}					
            }
			});
            mWifiBuilder.setCancelable(false);
  		    mWifiDialog = mWifiBuilder.create();
            mWifiDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
 		    mWifiDialog.show();           
            wifiEt.setTextSize(20);
 		    Slog.d(TAG, "显示wifi弹框 end" );
	}
	//end by ysten.zhangy 20191028 for CM201-2 henan 
		void showSleepDelayDialog() {
	            mCount = 30;
                Slog.d(TAG, "chinamobile showSleepDelayDialog");
                mSleepDelayTextView = new TextView(mContext);
                Slog.d(TAG, "mSleepDelayTextView 1 " + mSleepDelayTextView);
                /*modify by zhaolianghua for hebei&jiangxi text warning @20190130*/
                if("cm201_hebei".equals(SystemProperties.get("ro.ysten.province"))){
                   mCount = 20;
                   mSleepDelayTextView.setText(mContext.getResources().getString(com.android.internal.R.string.standby_warn_he,mCount));
		}else if(SystemProperties.get("ro.ysten.province").contains("jiangxi")){
		   mSleepDelayTextView.setText(mContext.getResources().getString(com.android.internal.R.string.standby_warn_jx,mCount));
                 }else if(SystemProperties.get("persist.sys.autosleep.enable", "false").equals("true")){
                //begin: add by tianchining at 20191106: 待机对话框提示
                    mSleepDelayTextView.setText(mContext.getResources().getString(com.android.internal.R.string.standby_warn_jx,mCount));
                //end: add by tianchining at 20191106: 待机对话框提示
                 }else{
                   mSleepDelayTextView.setText(
                   mContext.getResources().getString(com.android.internal.R.string.standby_warn,mCount));
                }
                /*modify end*/
                mSleepDelayTextView.setTextSize(25);
                mBuilder = new AlertDialog.Builder(mContext);
                Slog.d(TAG, "mBuilder 1 " + mBuilder);
                Slog.d(TAG, "mContext 1 " + mContext);
                mBuilder.setView(mSleepDelayTextView,110,50,60,50);
                mBuilder.setCancelable(false);
		        mDialog = mBuilder.create();
                mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
		        mDialog.show(); 
                /*mSleepDelayTextView.setText(mContext.getResources().getString(com.android.internal.R.string.standby_warn,mCount));
                mSleepDelayTextView.setTextSize(25);*/
		        Slog.d(TAG, "mSleepDelayTextView 3 " + mSleepDelayTextView);
                mHandler.removeCallbacks(mSleepCancleTimer);
                mHandler.postDelayed(mSleepCancleTimer, 1000);
        	    isCancelDialog=true; 
		}
    
    
    
	void showHenanSleepDelayDialog() {
	            mCount = 60;
                Slog.d(TAG, "chinamobile showSleepDelayDialog");
                mSleepDelayTextView = new TextView(mContext);
                Slog.d(TAG, "mSleepDelayTextView 1 " + mSleepDelayTextView);
                mSleepDelayTextView.setText(
				mContext.getResources().getString(com.android.internal.R.string.standby_warn,mCount));
                mSleepDelayTextView.setTextSize(20);
                mBuilder = new AlertDialog.Builder(mContext);
                Slog.d(TAG, "mBuilder 1 " + mBuilder);
                Slog.d(TAG, "mContext 1 " + mContext);
                int sleepDelay = SystemProperties.getInt("persist.sys.sleep",240);
                
                if(sleepDelay >= 60) {
                    mBuilder.setTitle(mContext.getResources().getString(com.android.internal.R.string.standby_warn_title,sleepDelay/60));
                } else if(sleepDelay < 60 && sleepDelay > 0) {
                    mBuilder.setTitle(mContext.getResources().getString(com.android.internal.R.string.standby_warn_title1,sleepDelay));
                } else {
                    mBuilder.setTitle(mContext.getResources().getString(com.android.internal.R.string.standby_warn_title,4));
                }
                //mBuilder.setView(mSleepDelayTitleView,110,50,60,50);
                mBuilder.setView(mSleepDelayTextView,110,50,60,50);
                mBuilder.setNegativeButton(R.string.continue_watch, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(isCancelDialog){
                            mHandler.removeCallbacks(mSleepCancleTimer);
                            mCount=60;
                            mDialog.dismiss();
                            isCancelDialog=false;
                        }
                    }
                });
                mBuilder.setCancelable(false);

		        mDialog = mBuilder.create();
                mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
		        mDialog.show(); 

                mSleepDelayTextView.setText(
				mContext.getResources().getString(com.android.internal.R.string.standby_warn,mCount));
                mSleepDelayTextView.setTextSize(20);
		        Slog.d(TAG, "mSleepDelayTextView 3 " + mSleepDelayTextView);

                mHandler.removeCallbacks(mSleepCancleTimer);
                mHandler.postDelayed(mSleepCancleTimer, 1000);
        	    isCancelDialog=true; 

    }

    //begin by ysten xyf for yunnan at 20200619:4 hours go to sleep
    void showYunnanSleepDelayDialog() {
                   mCount = 20;
                Slog.d(TAG, "chinamobile showSleepDelayDialog");
                mSleepDelayTextView = new TextView(mContext);
                Slog.d(TAG, "mSleepDelayTextView 1 " + mSleepDelayTextView);
               mSleepDelayTextView.setText(mContext.getResources().getString(com.android.internal.R.string.continue_show1,mCount));
                //mContext.getResources().getString(com.android.internal.R.string.standby_warn,mCount));
                mSleepDelayTextView.setTextSize(20);
                mBuilder = new AlertDialog.Builder(mContext);
                Slog.d(TAG, "mBuilder 1 " + mBuilder);
                int sleepDelay = Settings.System.getInt(mContext.getContentResolver(),SYSTEM_SLEEP_TIME_DELAY,-1);
                if(sleepDelay >= 60) {
                    mBuilder.setTitle(mContext.getResources().getString(com.android.internal.R.string.standby_warn_title,sleepDelay/60));
                } else if(sleepDelay < 60 && sleepDelay > 0) {
                    mBuilder.setTitle(mContext.getResources().getString(com.android.internal.R.string.standby_warn_title1,sleepDelay));
                } else {
                    mBuilder.setTitle(mContext.getResources().getString(com.android.internal.R.string.standby_warn_title,4));
                }
                mBuilder.setView(mSleepDelayTextView,110,50,60,50);
                mBuilder.setCancelable(false);

                       mDialog = mBuilder.create();
                mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
                       mDialog.show(); 
               mSleepDelayTextView.setText(mContext.getResources().getString(com.android.internal.R.string.continue_show1,mCount));
                mSleepDelayTextView.setTextSize(20);
                       Slog.d(TAG, "mSleepDelayTextView 3 " + mSleepDelayTextView);

                mHandler.removeCallbacks(mSleepCancleTimer);
                mHandler.postDelayed(mSleepCancleTimer, 1000);
                   isCancelDialog=true; 

    }
    //end by ysten xyf for yunnan at 20200619:4 hours go to sleep


	//add by ysten wuguoqing for liaoning at 20190327:4 hours go to sleep
	void showLiaoningSleepDelayDialog() {
	            mCount = 20;
                Slog.d(TAG, "chinamobile showSleepDelayDialog");
                mSleepDelayTextView = new TextView(mContext);
                Slog.d(TAG, "mSleepDelayTextView 1 " + mSleepDelayTextView);
		mSleepDelayTextView.setText(mContext.getResources().getString(com.android.internal.R.string.continue_show1,mCount));
                //mContext.getResources().getString(com.android.internal.R.string.standby_warn,mCount));
                mSleepDelayTextView.setTextSize(20);
                mBuilder = new AlertDialog.Builder(mContext);
                Slog.d(TAG, "mBuilder 1 " + mBuilder);
                Slog.d(TAG, "mContext 1 " + mContext);
                int sleepDelay = Settings.System.getInt(mContext.getContentResolver(),SYSTEM_SLEEP_TIME_DELAY,-1);
                if(sleepDelay >= 60) {
                    mBuilder.setTitle(mContext.getResources().getString(com.android.internal.R.string.standby_warn_title,sleepDelay/60));
                } else if(sleepDelay < 60 && sleepDelay > 0) {
                    mBuilder.setTitle(mContext.getResources().getString(com.android.internal.R.string.standby_warn_title1,sleepDelay));
                } else {
                    mBuilder.setTitle(mContext.getResources().getString(com.android.internal.R.string.standby_warn_title,4));
                }
                mBuilder.setView(mSleepDelayTextView,110,50,60,50);
                mBuilder.setNegativeButton(R.string.continue_watch1, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                            mHandler.removeCallbacks(mSleepCancleTimer);
                            mCount=60;
                            mDialog.dismiss();
                            HiSysManager hisys = new HiSysManager();
                            Intent mIntent;
                            hisys.setProperty("persist.suspend.mode", "deep_restart");
                            SystemProperties.set("persist.sys.smartsuspendin", "0");
                            try
                            {
                               Process p = Runtime.getRuntime().exec("/system/bin/sh /system/bin/hello1.sh 1");
                               Log.i(TAG, "xumiao set hello1.sh to 1 ");
                               String data = "";
                               BufferedReader ie = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                               BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                               String error = null;
                               while ((error = ie.readLine()) != null && !error.equals("null")) {
                                     data += error + "\n";
                               }
                               String line = null;
                               while ((line = in.readLine()) != null && !line.equals("null")) {
                                   data += line + "\n";
                               }
                               Log.i("xwj", "Show Suspend Dialog ok"+data);
                               }catch(IOException e){
                                   Log.i("xwj", "Show Suspend Dialog error");
                               }
                               mIntent = new Intent("android.ysten.systemupdate");
                               mIntent.putExtra("powercontrol", "poweroff");
                               mContext.sendBroadcast(mIntent);	
                            }
                });
		        mBuilder.setPositiveButton(R.string.continue_no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if(isCancelDialog){
                            mHandler.removeCallbacks(mSleepCancleTimer);
                            mCount=60;
                            mDialog.dismiss();
                            isCancelDialog=false;
                            //begin:add ysten liangkai at 20201023 : 凌晨待机
                            if(SystemProperties.get("ro.ysten.province","master").contains("neimeng")){
                                Timer timer = new Timer();
                                timer.schedule(task,60*1000);
                            }
                            //end:add ysten liangkai at 20201023 : 凌晨待机
                        }
                    }
                });
                mBuilder.setCancelable(false);

		        mDialog = mBuilder.create();
                mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
		        mDialog.show(); 
				mSleepDelayTextView.setText(R.string.continue_show1);
                mSleepDelayTextView.setTextSize(20);
		        Slog.d(TAG, "mSleepDelayTextView 3 " + mSleepDelayTextView);

                mHandler.removeCallbacks(mSleepCancleTimer);
                mHandler.postDelayed(mSleepCancleTimer, 1000);
        	    isCancelDialog=true; 

    }
	//end by ysten wuguoqing for liaoning at 20190327:4 hours go to sleep
	
     //added by yzs at 20190424 multikey trigger autolog begin
    public boolean handlerMultKey(int keyCode, KeyEvent event) {
        boolean vaildKey = false;
        if (keyCode >= KeyEvent.KEYCODE_0
                && keyCode <= KeyEvent.KEYCODE_9 && mAutoLogMultKeyTrigger.allowTrigger()) {
					
            // 是否是有效按键输入
            vaildKey = mAutoLogMultKeyTrigger.checkKey(keyCode, event.getEventTime());
            // 是否触发组合键
            if (vaildKey && mAutoLogMultKeyTrigger.checkMultKey()) {
                //执行触发
                mAutoLogMultKeyTrigger.onTrigger();
                //触发完成后清除掉原先的输入
                mAutoLogMultKeyTrigger.clearKeys();
            }
        }
        return vaildKey;
    }
    //added by yzs at 20190424 multikey tirgger autolog end
        
    //add by wangxin
    private static final String SCREEN_SAVER_DELAY = "screen_save_delay";
    private static final String ROOM_UPGRADE_FLAG = "room_upgrade_flag";
	
    public static String getBussinessPlatform(Context context)
    {
        String platform = queryDBValue(context, "bussiness_platform");
        Log.d(TAG, "getBussinessPlatform:" + platform);
	    return platform;
    }	

    public static String getTopActivity(Context context) {
	    try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1);
            if (runningTaskInfos != null && runningTaskInfos.size() != 0) {
                return (runningTaskInfos.get(0).baseActivity).getPackageName();
            }
        } catch (Exception e) {
            Log.d(TAG, "current app:" + e);
        }
        return "";
   }
   public void startAppWithScheme(Context context, String url) {
        Uri uri = Uri.parse(url);
        Intent action = new Intent("android.intent.action.VIEW");
		action.setData(uri);
	    action.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PackageManager manager = context.getPackageManager();
        List list = manager.queryIntentActivities(action, 64);
		if (!list.isEmpty()) {
            Log.d(TAG, "zzl start guoan!!!!!!!!!!");
			context.startActivity(action);
        } else {
            Log.d(TAG, "zzl no install guo an");
                    toastHandle.sendEmptyMessage(2);
         //   Toast toast =Toast.makeText(context, "hahahaha",Toast.LENGTH_LONG);
           //     toast.setGravity(Gravity.CENTER, 0, 0);
             //   toast.show();
        }
    }

    //add by tanhy
   private static final String SYSTEM_SLEEP_TIME_DELAY = "system_sleep_time_delay";
   boolean  isCancelDialog=false;
   private boolean mistvlongpress=false;  
   private boolean misPhoneWindowLed=false;
   private boolean ispressnowled=false;
   private boolean misASRstart=false; 
   private int countaccess=0; 
   private int repeatCountpre=0; 
   
   //add by guxin for jiangxi setting
   private static final String SLEEP_TIME = "sleep_time";
    /** {@inheritDoc} */
    @Override
    public long interceptKeyBeforeDispatching(WindowState win, KeyEvent event, int policyFlags) {
        final boolean keyguardOn = keyguardOn();
        final int keyCode = event.getKeyCode();
        final int repeatCount = event.getRepeatCount();
        final int metaState = event.getMetaState();
        final int flags = event.getFlags();
        final boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        final boolean canceled = event.isCanceled();
		
        //add by chenliang at 20181120 begin:press some key to start agingdragonbox
        Log.d("chenl", "mkeyAgingDragonboxCount: " + mkeyAgingDragonboxCount);
        if (keyCode == KeyEvent.KEYCODE_MENU && 0 == mkeyAgingDragonboxCount && true == down) {
            mkeyAgingDragonboxCount = 1;
        } else if (keyCode == KeyEvent.KEYCODE_9 && 1 == mkeyAgingDragonboxCount && true == down) {
            mkeyAgingDragonboxCount = 2;
        } else if (keyCode == KeyEvent.KEYCODE_5 && 2 == mkeyAgingDragonboxCount && true == down) {
            mkeyAgingDragonboxCount = 3;
        } else if (keyCode == KeyEvent.KEYCODE_2 && 3 == mkeyAgingDragonboxCount && true == down) {
            mkeyAgingDragonboxCount = 4;
        } else if (keyCode == KeyEvent.KEYCODE_7 && 4 == mkeyAgingDragonboxCount && true == down) {
            mkeyAgingDragonboxCount = 5;
        } else if (keyCode == KeyEvent.KEYCODE_MENU && 5 == mkeyAgingDragonboxCount && true == down) {
            mkeyAgingDragonboxCount = 0;
            mHandler.removeCallbacks(startAgingDragonbox);
            mHandler.postDelayed(startAgingDragonbox, 100);
        } else if (true == down) {
            mkeyAgingDragonboxCount = 0;
        }
        //add by chenliang at 20181120 end:press some key to start agingdragonbox

        //add by sunjh at 20191212 for c60_zhejiang:open packagelist
        if (("true").equals(SystemProperties.get("persist.sys.ysten.allapp", "false"))) {
            if (down && keyCode == KeyEvent.KEYCODE_1) {
                if (repeatCount < 30) {
                    Log.d("TAG", "sjh:1 is down, repeatCount is " + repeatCount);
                } else if (repeatCount == 30) {
                    Intent homeIntent=new Intent();
                    ComponentName homeName = new ComponentName("com.ysten.demo","com.ysten.demo.AllApp");
                    homeIntent.setComponent(homeName);
                    homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    mContext.startActivity(homeIntent);
                    Log.d("TAG", "sjh:alert");
                }
            }
        }
        //add by sunjh at 20191212 for c60_zhejiang:open packagelist

		//add by ysten.zhangy 20191029 for CM201_henan some key for wifi dialog
		if(SystemProperties.get("ro.ysten.province","master").contains("henan")){

			if(down){
				Log.d(TAG," wifiCount start : "+wifiCount+" keyCode="+keyCode);	
			
			
				if(down&&keyCode==19 &&  wifiCount==0 ){
					wifiCount=1;	
				 Log.d(TAG," wifiCount : "+wifiCount+" keyCode="+keyCode);	  
				
				}else if(down&&keyCode==19 &&  wifiCount==1){
					wifiCount=2;
									
				Log.d(TAG," wifiCount : "+wifiCount+" keyCode="+keyCode);	
				}else if(down&&keyCode==20 && wifiCount==2){
					wifiCount=3;
									   
				Log.d(TAG," wifiCount : "+wifiCount+" keyCode="+keyCode);	
				}else if(down&&keyCode==20 && wifiCount==3){
					wifiCount=4;
									   
				Log.d(TAG," wifiCount : "+wifiCount+" keyCode="+keyCode);	
				}else if(down&&keyCode==21 && wifiCount==4){
					wifiCount=5;
									   
				Log.d(TAG," wifiCount : "+wifiCount+" keyCode="+keyCode);	
				}else if(down&&keyCode==22 && wifiCount==5){
					wifiCount=6;
									   
				Log.d(TAG," wifiCount : "+wifiCount+" keyCode="+keyCode);	
				}else if(down&&keyCode==21 && wifiCount==6){
					wifiCount=7;
									  
				Log.d(TAG," wifiCount : "+wifiCount+" keyCode="+keyCode);	
				}else if(down&&keyCode==22 && wifiCount==7){
					wifiCount=8;
									   
				Log.d(TAG," wifiCount : "+wifiCount+" keyCode="+keyCode);	
				}else if(down&&keyCode==23 && wifiCount==8){
					wifiCount=9;
									  
				Log.d(TAG," wifiCount : "+wifiCount+" keyCode="+keyCode);	
				}else if(down&&keyCode==23 && wifiCount==9){
					Log.d(TAG," wifiCount : "+wifiCount+" keyCode="+keyCode);	
					mHandler.sendEmptyMessage(MSG_OPEN_WIFI_DIALOG);
									
			
				}else {
				
					wifiCount=0;
				} 
			}		 				
		}
        //end by ysten.zhangy 20191029 for CM201_henan 

		//add by sunlei 191127 for 湖北长按powerkey进入power option
		if(SystemProperties.get("ro.ysten.province","master").contains("hubei")){
			 if (down && keyCode == KeyEvent.KEYCODE_POWER) {
				Log.d(TAG,"sunlei start22222");
                if (repeatCount < 35) {
                    Log.d(TAG,"sunlei power is down, repeatCount is " + repeatCount);
                } else if (repeatCount == 50) {
					Log.d(TAG,"sunlei send : ");                                                                                                           
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.ysten.poweroff", "com.ysten.poweroff.YstPowerOffService"));
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startService(intent);
                    isHubeiShowDialog = true;
                }
            }
		}
		//end by sunlei 191127 for 湖北长按powerkey进入power option
		
		//add by xue
		
	    if(!down&&repeatCount==0)
		{
		if((repeatCountpre>=3&&repeatCountpre<=5))
		{
		if(countaccess>=2)
		{
		countaccess=0;
		repeatCountpre=0;
		ledpmanager(53);
		//reset accessibility
		}
		else
		{
		countaccess++;
		}
		}
		else
		{
		countaccess=0;
		repeatCountpre=0;
		}
		
		}
		
     
		repeatCountpre=repeatCount;

		//add by xue
		//add by wangxin
		
  
		
        final boolean up = event.getAction() == KeyEvent.ACTION_UP;
        if(SystemProperties.get("ro.ysten.province","master").contains("fujian")) {
            if(down && !ispressnowled) {
                ispressnowled=true;
                ledpmanager(2);
                //added by yzs begin
                int value=SystemProperties.getInt("persist.sys.launcher.value",0);//1-ott 2--iptv
				if(DEBUG)
                    Log.i("yzs007", "pressed power key and value is " + value);
                if(1== value) {
                    ledpmanager(103);
                } else if(2 == value) {
                    ledpmanager(104);
                }
                //added by yzs end
            }
            if(up) {
                ispressnowled=false;
				ledpmanager(1);
			}
		}

        if(up&&SystemProperties.get("ro.ysten.province","master").equals("cm201_shaanxi")){
            doIPTVFuncationSAX(keyCode);
        }

	if(SystemProperties.get("ro.ysten.province","master").contains("cm201_beijing")){
           ActivityManager activityManager=(ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
           String currentPackage=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
            Log.d(TAG," currentPackage = " + currentPackage);

	        if(down && repeatCount == 0&& keyCode == KeyEvent.KEYCODE_F17) {
				Intent intent = new Intent();
				ComponentName cn = null;
				if(keyCode == KeyEvent.KEYCODE_F17){
				cn = new ComponentName("com.citicguoanbn.duer", "com.citicguoanbn.duer.service.DuerService");
				intent.putExtra("actionType", "start");
				intent.setComponent(cn);
				mContext.startServiceAsUser(intent, UserHandle.CURRENT);
				return -1;
			  } 
            }
		
            if(!down && keyCode == KeyEvent.KEYCODE_F17) {
			    Intent intent = new Intent();     
			    ComponentName cn = null;
			    cn = new ComponentName("com.citicguoanbn.duer", "com.citicguoanbn.duer.service.DuerService");
				intent.putExtra("actionType", "end");
				intent.setComponent(cn);
				mContext.startServiceAsUser(intent, UserHandle.CURRENT);
			return -1;
            }
			
			if(down && keyCode == KeyEvent.KEYCODE_F24) {
				Log.d(TAG,"start guo an she qu");
				startAppWithScheme(mContext, "launcher://com.guoan.tv");
			}
			if(down && keyCode == KeyEvent.KEYCODE_F23) {
				Log.d(TAG,"start yun shi jie");
				 Intent intent = new Intent();
				ComponentName comp =new ComponentName("tv.icntv.ott","com.istv.ui.app.usercenter.QrcodeActivity");
                intent.setComponent(comp);
	            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
			}
        }else if(SystemProperties.get("ro.ysten.province","master").contains("shandong")){
            //begin: add by tianchining at 20191106: 山东四色按键
            Log.d(TAG, "TCN_ADD: interceptKeyBeforeDispatching keycode: " + keyCode);
            if(down && keyCode == KeyEvent.KEYCODE_TV){
                String topPackage = getTopActivity(mContext);
                Log.d(TAG, "TCN_ADD: interceptKeyBeforeDispatching keycode: TV, topPackage: " + topPackage);
                if(!"com.xike.xkliveplay".equals(topPackage)){
                    Intent intent = new Intent();
                    ComponentName comp = new ComponentName("com.xike.xkliveplay",
                        "com.shandong.shandonglive.MainActivity");
                    intent.setComponent(comp);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);
                    return -1;
                }
            }else if(down && keyCode == KeyEvent.KEYCODE_MOVIE){
                String topPackage = getTopActivity(mContext);
                Log.d(TAG, "TCN_ADD: interceptKeyBeforeDispatching keycode: MOVIE, topPackage: " + topPackage);
                if(!"com.gitv.tv.launcher".equals(topPackage)){
                    Intent intent = new Intent();
                    ComponentName comp = new ComponentName("com.gitv.tv.launcher",
                        "com.gitv.tv.launcher.activity.WelcomeActivity");
                    intent.setComponent(comp);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);
                    return -1;
                }


            }else if(down && keyCode == KeyEvent.KEYCODE_APPS){ //
                Log.d(TAG, "TCN_ADD: interceptKeyBeforeDispatching keycode: APPS");

            }else if(down && keyCode == KeyEvent.KEYCODE_FAVOURITE){
                Log.d(TAG, "TCN_ADD: interceptKeyBeforeDispatching keycode: FAVOURITE");

            }

         }
        //end: add by tianchining at 20191106: 山东四色按键	    
	
        if(up){
			if(isCancelDialog){
				mHandler.removeCallbacks(mSleepCancleTimer);
				mCount=30;
				mDialog.dismiss();
				isCancelDialog=false;
			}
            int delay = Settings.System.getInt(mContext.getContentResolver(), SCREEN_SAVER_DELAY, -1);
            Log.d(TAG, "delay is " + delay);
            mHandler.removeCallbacks(mLaunchScreenSaver);
            if(delay > 0){
                mHandler.postDelayed(mLaunchScreenSaver, delay*60*1000);
            }
            
            /*begin:add by zhanghk at 20190214:judge login status*/ 
            if(SystemProperties.get("ro.ysten.province","master").contains("cm201_hainan")){
                String username = "";
                Uri uri = Uri.parse("content://stbconfig/authentication/username");
                try{
                    Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
                    if(cursor != null){
                        while (cursor.moveToNext()) {
                            username = cursor.getString(cursor.getColumnIndex("value"));
                        }
                        cursor.close();
                    }
                } catch(Exception e){
                    Log.d("zhanghk", "queryDBValue Execption "+e.toString());
                }
                Log.d("zhanghk","username:"+username);
                if(username==null || "".equals(username) || TextUtils.isEmpty(username)){
                    //enable home key useless	
                    Settings.System.putInt(mContext.getContentResolver(), "HomeKeyPermission", 0);
                }else{
                    Settings.System.putInt(mContext.getContentResolver(), "HomeKeyPermission", 1);
                }
            }
            /*end:add by zhanghk at 20190214:judge login status*/ 

	//String currentPackage = getCurProcessName(mContext);
	ActivityManager activityManager=(ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);  
	String currentPackage=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();  
	Log.d(TAG," currentPackage = " + currentPackage);
    //add by ysten zengzhiliang at 20180928:add ro.ysten.province=cm201_beijing control 
	if (currentPackage != null && currentPackage.startsWith("com.ysten.setting") 
            && SystemProperties.get("ro.ysten.province","master").contains("cm201_beijing"))
		{
		if(keyCode==KeyEvent.KEYCODE_DPAD_UP||keyCode==KeyEvent.KEYCODE_DPAD_DOWN||keyCode==KeyEvent.KEYCODE_DPAD_LEFT||keyCode==KeyEvent.KEYCODE_DPAD_RIGHT)
		{
			if((keyCode==KeyEvent.KEYCODE_DPAD_UP)&&(adbEanbledSetp==0||adbEanbledSetp==1))
			{
				adbEanbledSetp++;
			}
			else if((keyCode==KeyEvent.KEYCODE_DPAD_DOWN)&&(adbEanbledSetp==2||adbEanbledSetp==3))
			{			
				adbEanbledSetp++;
			}
			else if((keyCode==KeyEvent.KEYCODE_DPAD_LEFT)&&(adbEanbledSetp==4||adbEanbledSetp==5))
			{
				adbEanbledSetp++;
			}		
			else if((keyCode==KeyEvent.KEYCODE_DPAD_RIGHT)&&(adbEanbledSetp==6||adbEanbledSetp==7))
			{
				if(adbEanbledSetp==6)
				{
					adbEanbledSetp++;
				}
				else if(adbEanbledSetp==7)
				{
					//ledpmanager(10);
					String produce = SystemProperties.get("persist.sys.yst.keyadb",  "");
					if("true".equals(produce)){

                        //begin: add by ysten zengzhiliang at 20180911 modify adb port and add totast
						SystemProperties.set("service.adb.tcp.port",  "5555");
						SystemProperties.set("sys.service.adbd.enable",  "1");
                        toastHandle.sendEmptyMessage(3);
                        //end: add by ysten zengzhiliang at 20180911 modify adb port and add totast
					}
					adbEanbledSetp=0;
				}
			}				
			else
			{
				adbEanbledSetp=0;
			}
		}
		else
		{
			adbEanbledSetp = 0;
		}
		}
	   //add by tanhy
	    mHandler.removeCallbacks(mSystemSleep);
            int sleepDelay = Settings.System.getInt(mContext.getContentResolver(),SYSTEM_SLEEP_TIME_DELAY,-1);
            Log.d(TAG,"test sleep = " +sleepDelay);
            if(SystemProperties.get("ro.ysten.province","master").contains("cm201_shaanxi")){
                    sleepDelay = SystemProperties.getInt("persist.sys.autosuspend.timeout",3*60);
            }else if(SystemProperties.get("ro.ysten.province","master").contains("cm201_zhejiang")
                ||SystemProperties.get("ro.ysten.province","master").contains("cm201_henan")){
                    sleepDelay=SystemProperties.getInt("persist.sys.sleep",240);
					
            }else if (SystemProperties.get("ro.ysten.province","master").contains("jiangxi")){
                sleepDelay=Settings.System.getInt(mContext.getContentResolver(),SLEEP_TIME,-1);
                Log.d(TAG,"jiangxi SLEEP_TIME sleepdelay in  is " + sleepDelay);
            }else
			  {
			  if(SystemProperties.get("ro.ysten.province","master").contains("master"))
			  sleepDelay=-1;
			  }
            if(sleepDelay > 0&&!SystemProperties.get("ro.ysten.province","master").contains("hengxin"))
                  mHandler.postDelayed(mSystemSleep,sleepDelay*60*1000);
 
        }

        if (DEBUG_INPUT) {
            Log.d(TAG, "interceptKeyTi keyCode=" + keyCode + " down=" + down + " repeatCount="
                    + repeatCount + " keyguardOn=" + keyguardOn + " mHomePressed=" + mHomePressed
                    + " canceled=" + canceled);
        }
		
        if(event.getAction() == KeyEvent.ACTION_UP) {
			if(SystemProperties.get("ro.ysten.province","master").contains("fujian")) {
                if(keyCode==811 || KeyEvent.KEYCODE_MMM_Z == keyCode) {
                    Log.i("yzs007", "您刚刚按的是#键");
                    Toast.makeText(mContext, "您刚刚按的是#键", Toast.LENGTH_SHORT).show();
                }
            } 
			
            if(SystemProperties.get("ro.ysten.province","master").contains("neimeng")){
                int nmKey=0;
                String packeageNM=getTopActivity(mContext);
			  if(!packeageNM.equals("com.ysten.auth")){
			    Log.d(TAG,"into 1");
				if(keyCode==KeyEvent.KEYCODE_TV){
				   nmKey=KeyEvent.KEYCODE_F6;
				}else if(keyCode==KeyEvent.KEYCODE_MOVIE){
				   nmKey=KeyEvent.KEYCODE_F7;
				}else if(keyCode==KeyEvent.KEYCODE_APPS){
				   nmKey=KeyEvent.KEYCODE_F8;
				}else if(keyCode==KeyEvent.KEYCODE_FAVOURITE){
				   nmKey=KeyEvent.KEYCODE_F9;
				}
				if (nmKey!=0){
                   try{
                       Process p = Runtime.getRuntime().exec("input keyevent "+ nmKey);
                      }catch(Exception e){
                       Log.i(TAG, "dokey error -1");
                       return -1;
					}
                }
                if(keyCode==KeyEvent.KEYCODE_TV &&  (!packeageNM.equals("com.ysten.auth"))){
                   Log.d(TAG,"into LIVE");
                   try{
                       Process p = Runtime.getRuntime().exec("input keyevent "+ KeyEvent.KEYCODE_F6);
                      }catch(Exception e){
                       Log.i(TAG, "dokey error -1");
                       return -1;
					}
                }                  
            }
			}
        }

        // If we think we might have a volume down & power key chord on the way
        // but we're not sure, then tell the dispatcher to wait a little while and
        // try again later before dispatching.
        if (mScreenshotChordEnabled && (flags & KeyEvent.FLAG_FALLBACK) == 0) {
            if (mVolumeDownKeyTriggered && !mPowerKeyTriggered) {
                final long now = SystemClock.uptimeMillis();
                final long timeoutTime = mVolumeDownKeyTime + SCREENSHOT_CHORD_DEBOUNCE_DELAY_MILLIS;
                if (now < timeoutTime) {
                    return timeoutTime - now;
                }
            }
            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                    && mVolumeDownKeyConsumedByScreenshotChord) {
                if (!down) {
                    mVolumeDownKeyConsumedByScreenshotChord = false;
                }
                return -1;
            }
        }

        if(SystemProperties.get("ro.product.target").equals("shcmcc")){
            if (keyCode == KeyEvent.KEYCODE_BACK
                    && ((ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE)).getRunningTasks(1).get(0).topActivity.getClassName().contains("net.sunniwell.app.ott.chinamobile")) {
                Log.d(TAG, "mLongPressBack = " + mLongPressBack);
                if(down && repeatCount >= 1){
                    if(repeatCount >= 5 && !mLongPressBack){
                        Log.d(TAG, "LongPressBack,reset net.sunniwell.app.ott.chinamobile");
                        mLongPressBack = true;
                        launchHomeFromHotKey();
                    }

                    return -1;
                }else if(event.getAction() == KeyEvent.ACTION_UP){
                    mLongPressBack = false;
                }
            }
        }
		
		//added by yzs at 20190424 multikey trigger autolog begin
        if(down){
			handlerMultKey(keyCode, event);
        }
        //added by yzs at 20190424 multikey trigger autolog end
        // voice recognition
		/*
        if ( 1 == SystemProperties.getInt("persist.sys.support_orionstar", 0) ) {
            if (keyCode == KeyEvent.KEYCODE_F10) {
                if (1 == SystemProperties.getInt("persist.sys.orionstar.status", 0)) {
                    if (!down) {
						if (!SystemProperties.get("ro.ysten.province").contains("fujian")) {						
						
                        // andy start voice_recognition broadcast
                        Intent intent = new Intent("com.ysten.manual_voice_recognition_start");         
                        intent.putExtra("action", "manual_start_voice");
                        mContext.sendBroadcast(intent);
						}
                    }
                }else{
                    //Toast.makeText(mContext, "please open voice_recognition switch", Toast.LENGTH_SHORT).show();
                    toastHandle.sendEmptyMessage(1);
            }
          }
            if(keyCode == KeyEvent.KEYCODE_APPS)
                return -1;
        }*/
	    //add voice recognition	
		if ( 1 == SystemProperties.getInt("persist.sys.support_orionstar", 0) ) {
            if (keyCode == KeyEvent.KEYCODE_F11&&SystemProperties.get("sys.mic.i2c","true").contains("true")) {
                if (1 == SystemProperties.getInt("persist.sys.orionstar.status", 0)) {
		            if (down) {
                        if ((event.getFlags() & KeyEvent.FLAG_LONG_PRESS) != 0) {  
                            if (!keyguardOn) {
                                mistvlongpress=true;
								misASRstart=true;
                                handleLongPressForIsTV(keyCode,true);  
                            }
                        }
                    } else {
                        if(mistvlongpress) {
                            mistvlongpress=false;
							misASRstart=false;
                            handleLongPressForIsTV(keyCode,false);
                        } else {
                            // andy start voice_recognition broadcast
                            Intent intent = new Intent("com.ysten.manual_voice_recognition_start");         
                            intent.putExtra("action", "manual_start_voice");
                            mContext.sendBroadcast(intent);
                        }
                    }
                } else {
                    toastHandle.sendEmptyMessage(1);            
                }
				//begin by ysten.lizheng,20181126,perform keycode_enter
            }else if (keyCode == KeyEvent.KEYCODE_F11&&SystemProperties.get("sys.mic.i2c","true").contains("false")) {
				if (down) {
				Log.i(TAG, "perform keycode_enter");     
				try{                                                                                                                                 
                    Process p = Runtime.getRuntime().exec("input keyevent 66");                                                                       
                   }                                                                                                                                 
                        catch(Exception e){                                                                                                          
                    Log.i(TAG, "keycode_enter orror");                                                                                               
                }}
			}
			//end by ysten.lizheng,20181126,perform keycode_enter
            if(keyCode == KeyEvent.KEYCODE_F11)
                return -1;
			if(misASRstart==true)
			{
				misASRstart=false;	
			handleLongPressForIsTV(KeyEvent.KEYCODE_F11,false);
			}
        }    
		//istv key dispatch add  by xue
		if(DEBUG_ISTV)
		{
		if (down)
{
if ((event.getFlags() & KeyEvent.FLAG_LONG_PRESS) != 0) {  
                    if (!keyguardOn) {
                        mistvlongpress=true;
                        handleLongPressForIsTV(keyCode,true);  
                    } 
}
}
else
{
if(mistvlongpress)
{
mistvlongpress=false;
handleLongPressForIsTV(keyCode,false);
}
}
}
		//istv key dispatch add by xue
        // First we always handle the home key here, so applications
        // can never break it, although if keyguard is on, we do let
        // it handle it, because that gives us the correct 5 second
        // timeout.
        if (keyCode == KeyEvent.KEYCODE_HOME) {
               //add by wangrongke  for  tianjin home key
            if(SystemProperties.get("ro.ysten.province","master").contains("tianjin")){
               Intent intent = new Intent(Intent.ACTION_MAIN);
               intent.setClassName("com.ysten.laucher", "com.ysten.laucher.MainActivity");
               intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
               intent.addCategory(Intent.CATEGORY_LAUNCHER);
               mContext.startActivity(intent);
               return -1;    
            //end by wangrongke for tianjin home key
             }else if(SystemProperties.get("ro.ysten.province","master").contains("neimeng")){
                String packeageNM=getTopActivity(mContext);
                Log.d(TAG,"packeageNM = "+packeageNM);
            	if(packeageNM.equals("com.ysten.auth")){
            		return -1;
            	}
            }
			if(isActivityRunning(mContext,"com.ysten.tr069")) {
			if(!SystemProperties.get("sys.key.home","on").equals("on")&& SystemProperties.get("ro.ysten.province","master").equals("CM201-2_shanxi_iptv")){
				return -1;}
			}
			
                 if(event.getAction() == KeyEvent.ACTION_UP&&SystemProperties.get("ro.ysten.province","master").equals("cm201_hebei")){
                        doIPTVFuncationHE(keyCode);
			return -1;
                 }
                        //Ysten.zhangjinjian,20180913,add for home key
                        if(SystemProperties.get("ro.ysten.province","master").equals("cm201_guizhou")){
                           doGZFunAction(keyCode);
                        }
                        //end by ysten.zhangjuinjian 
						//begin by ysten.zhangjunjian,20190722,for home key
			Log.d("zjj", "begin");
			if(SystemProperties.get("ro.ysten.province","master").equals("cm201_yunnan_iptv")){
				Log.d("zjj", "home key");
                    String topPackageName = getTopActivity(mContext);
                    if(!TextUtils.isEmpty(topPackageName) && !topPackageName.equals("yn.mobile.iptv")) {
                        if(!down){
                                Intent iptvIntent = new Intent();
                                iptvIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                ComponentName comp =new ComponentName("yn.mobile.iptv", "yn.mobile.iptv.LauncherActivity");
                                iptvIntent.setComponent(comp);
                                mContext.startActivity(iptvIntent);
                                }
                                return -1;
                            } else {
                                return 0;
                            }
                   }
			//end by ysten.zhangjunjian,20190722,for home key
       
            // begin: add by tianchining at 20191111, home key
            if(SystemProperties.get("ro.ysten.province","master").contains("shandong")){
                if(down){
                    return -1;
                }
                
                String topPackage = getTopActivity(mContext);
                Log.d(TAG, "TCN_ADD: home key up, topPackage: " + topPackage + ", authsucc: " + SystemProperties.get("sys.ysten.authsucc", "0"));
                if(SystemProperties.get("sys.ysten.authsucc", "0").equals("0")){ // 没有认证成功直接截断
                    return -1; // 截断按键
                }
                if("com.huawei.sd.yd.launcher".equals(topPackage)){
                    return -1; 
                }

                Log.d(TAG, "TCN_ADD: start home Activity! ");
                Intent mIntent = new Intent();
                ComponentName comp = new ComponentName("com.huawei.sd.yd.launcher", "com.huawei.sd.yd.launcher.LauncherActivity");
                mIntent.setComponent(comp);
                mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(mIntent);
                return -1;
            }
            //end: add by tianchining at 20191111, home key

            //begin:add by zhanghk at 20191119:add for anhui IPTV home key
            if(SystemProperties.get("ro.ysten.province","master").equals("cm201_anhui_iptv")){
                if (!down){
                    doIPTVFunctionAH(keyCode);
                    return -1;
                }
            }
            //end:add by zhanghk at 20191119:add for anhui IPTV home key
						
        //Ysten.zhangjinjian,20180913,add for home key
        if(event.getAction() == KeyEvent.ACTION_UP&&SystemProperties.get("ro.ysten.province","master").contains("guizhou")){
			// doAHIPTVFuncation(keyCode);
				doIPTVFuncationGZ(keyCode);
				return -1;
        }
        //end by ysten.zhangjuinjian 
            DevInfoManager manager = (DevInfoManager) mContext.getSystemService(DevInfoManager.DATA_SERVER);
            String packageName = manager.getValue(DevInfoManager.Launcher);
            int isResponHomeKey=Settings.System.getInt(mContext.getContentResolver(), "IS_RESPOND_HOME_KEY", 0);
            //edit by lingyun ad 20181229:edit jiangxi iptv home
	    if(SystemProperties.get("ro.ysten.province","master").contains("jiangxi")
		&& !SystemProperties.get("persist.sys.launcher.value","1").equals("2")){
		if(isResponHomeKey != 0){
			Log.w("ysten_cm201", "isResponHomeKey = " + isResponHomeKey);
			return -1;
		}
		if(SystemProperties.get("sys.yst.outwaitactivity","0").equals("0")){//disable home key in waitActivity
			return -1;
		}
	    }
	    /*if(SystemProperties.get("ro.ysten.province","master").equals("cm201_jiangxi") 
		&& SystemProperties.get("persist.sys.launcher.value","1").equals("2")){
                doIPTVFuncationHE(keyCode);
                Log.w("ysten_cm201", " use this KEYCODE_HOME ");
            }*/
            //edit by lingyun ad 20181229:edit jiangxi iptv home	
			if(SystemProperties.get("ro.ftserialno","master").equals("ysten"))
				{
				isResponHomeKey=0;
				}
            if(SystemProperties.get("ro.ysten.province","master").equals("c60_jiangsu")){
                //begin:add by zhuhengxuan for kill setting
                if("com.ysten.setting".equals(getTopActivity(mContext))) {
                    HiSysManager hisys = new HiSysManager();
                    hisys.rootSystem("busybox killall -9 com.ysten.setting");
                }
                //end:add by zhuhengxuan for kill setting
                if(packageName.equals("tv.icntv.ott")) {
                    if(isResponHomeKey!=0){
                        Log.d(TAG, "do not response home key 1");
                        return -1;
                    }
                }
            } else {
                String isIcntv = SystemProperties.get("ro.icntv", "1");
                if(isResponHomeKey!=0 && !SystemProperties.get("ro.ysten.province").equals("cm201_yunnan")
                	&&!SystemProperties.get("ro.ysten.province").equals("c60_yunnan")	
					&&!SystemProperties.get("ro.ysten.province","master").contains("fujian")
                    //add by zhanghk at 201809-5:fix home key useless when use huawei launcher
                    &&!SystemProperties.get("ro.ysten.province","master").contains("zhejiang")
                    //add by xumiao at 20181025:fix home key do not response
                    &&!SystemProperties.get("ro.ysten.province","master").contains("heilongjiang")
                    //add by zengzhiliang at 20181109:fix home key do not response
                    &&!SystemProperties.get("ro.ysten.province","master").contains("anhui")
				    &&!SystemProperties.get("ro.ysten.province","master").contains("jiangxi")
                    &&!SystemProperties.get("ro.ysten.province","master").contains("hengxin")
                    &&!SystemProperties.get("ro.ysten.province","master").contains("shanxi")   //add by xyf at 20200401:fix home key useless when poweroff
                    &&!SystemProperties.get("ro.ysten.province","master").contains("master")){
                    Log.d(TAG, "do not response home key 2");
                    return -1;
                }
            }
            
            /*begin:add by zhanghk at 20190214:judge login status*/
            if(SystemProperties.get("ro.ysten.province","master").equals("cm201_hainan")){
                int homeStatus=Settings.System.getInt(mContext.getContentResolver(), "HomeKeyPermission", 1);
                Log.d("zhanghk","HomeKeyPermission:"+homeStatus);
                if(homeStatus==0){
                    return -1;
                }
            }
            /*end:add by zhanghk at 20190214:judge login status*/

			if(SystemProperties.get("ro.ysten.province","master").equals("cm201_hubei")){
			    Uri uri = Uri.parse("content://stbconfig/summary");
                             String username = "";
                      try{
                       Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
                       if(cursor != null){
                               while (cursor.moveToNext()) {
                                  String name = cursor.getString(cursor.getColumnIndex("name"));
                                    if ("username".equals(name)) {
                                            username = cursor.getString(cursor.getColumnIndex("value"));
                                            break;
                                        }
                }
                                cursor.close();
                        }
                } catch(Exception e){
                        Log.d(TAG, "queryDBValue Execption "+e.toString());
                }
                Log.d("zjj","username:"+username);
                  String bussiness = getBussinessPlatform(mContext);
                if(TextUtils.isEmpty(bussiness)){
                    bussiness = SystemProperties.get("persist.sys.hb.bussiness", "");
	                Log.d(TAG, "get persist bussiness:" + bussiness);
                }
                Log.d(TAG, "bussiness:" + bussiness);
                String businessplatform=queryDBValue(mContext,"bussiness_platform");
                if(TextUtils.isEmpty(businessplatform)){
                   return -1;
                }
				
				//begin:add by ysten lijg at 20190325:自助排障跳到设置页面后点首页按键跳到自助排障
                Log.d(TAG, "home");
                if (!down && SystemProperties.get("sys.key.home").equals("discon")) {
                    Log.d(TAG, "home discon");
                    Intent intent = new Intent("android.intent.action.MAIN");
                    intent.setClassName("com.ysten.netdiscon", "com.ysten.netdiscon.MainActivity");
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);
                    Log.d(TAG, "home discon intent");
                    return -1;
                }
                //end:add by ysten lijg at 20190325
				
                if(!TextUtils.isEmpty(bussiness) && bussiness.equalsIgnoreCase("iptv")) {
	                Log.d(TAG, "launcher iptv");
		            String topPackageName = getTopActivity(mContext);
                    if(!TextUtils.isEmpty(topPackageName) && !topPackageName.equals("com.android.smart.terminal.iptv")){
                 String launcher = SystemProperties.get("persist.sys.luncher","0");
         
                        if(TextUtils.isEmpty(username)||launcher.contains("0")){
                             return -1;
                          }
                        if(!down){
	                        Intent iptvIntent = new Intent();;
				iptvIntent.setAction("com.android.smart.terminal.iptv");
	                        iptvIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				iptvIntent.putExtra("intentMsg", "EPGDomain");
	                        mContext.startActivity(iptvIntent);
                                Intent panelIntent=new Intent("KEY_HOME");
                                mContext.sendBroadcast(panelIntent);
                                Intent homeIntent=new Intent("HOME_KEYC");
                                mContext.sendBroadcast(homeIntent);
		                }
		                return -1;
		            } else {
                        return 0;
		            }
               } else {
                   String topPackageName = getTopActivity(mContext);
                   if(!down && !topPackageName.equals("com.android.iptvauth")){
	                   /*Log.d(TAG, "launcher bestv");
	                   Intent bestv = new Intent();//("bestv.ott.action.launcher");
	                   bestv.setAction("bestv.ott.action.launcher");
	                   bestv.addCategory(Intent.CATEGORY_DEFAULT);
	                   bestv.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	                   mContext.startActivity(bestv);
                           Intent homeIntent=new Intent("HOME_KEYC");
                           mContext.sendBroadcast(homeIntent);*/
						   //add by huxiang at 2019/09/09 for bestTV require this broadcast
						   Intent bestIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                           bestIntent.putExtra("reason","homekey");
                           mContext.sendBroadcast(bestIntent);
						   launchHomeFromHotKey();
						   //add end
	                   return -1;
                   }else{return 0;}
	           }
			}

                if(SystemProperties.get("ro.ysten.province","master").equals("cm201_guangdong")){
                    String topPackageName = getTopActivity(mContext);
                    if(!TextUtils.isEmpty(topPackageName) && !topPackageName.equals("cn.gd.snm.snmcm")) {
                        if(!down){
                                Intent iptvIntent = new Intent();
                                iptvIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                ComponentName comp =new ComponentName("cn.gd.snm.snmcm","cn.gd.snm.snmcm.LauncherActivity");
                                iptvIntent.setComponent(comp);
                                mContext.startActivity(iptvIntent);
                                Intent panelIntent=new Intent("KEY_HOME");
                                mContext.sendBroadcast(panelIntent);
                                }
                                return -1;
                            } else {
                                return 0;
                            }
                   }
				   
				//begin:add by ysten zengzhiliang at 20191109:sync from zhangyong@ysten.com change 
				if(SystemProperties.get("ro.ysten.province","master").equals("cm201_anhui")
                        //add by ysten zengzhiliang at 20181109:add m301h_anhui
                     ||SystemProperties.get("ro.ysten.province","master").equals("m301h_anhui"))
                {
                     Log.d("zhangy", "start anhui");
                    String topPackageName = getTopActivity(mContext);
		            if(!TextUtils.isEmpty(topPackageName) 
                            && Settings.Global.getInt(mContext.getContentResolver(),Settings.Global.DEVICE_PROVISIONED, 0) == 1
                            && !topPackageName.equals("com.zte.iptvclient.android.launcher_ah")) 
                        {
                            if(!down)
                            {
                                Intent iptvIntent = new Intent();
                                iptvIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                ComponentName comp =new ComponentName("com.zte.iptvclient.android.launcher_ah","com.chinamobile.launcherjs.main");
                                iptvIntent.setComponent(comp);
                                mContext.startActivity(iptvIntent);
                            }
							     //add by zhanghy at 20190123:kill some apk not in whitelist
                                freeMemory(apkNameListAH);
                                return -1;
                            } 
                            else 
                            {
								   //add by zhanghy at 20190123:kill some apk not in whitelist
                                freeMemory(apkNameListAH);
                                return 0;
                            }
                   }
				   
				//end:add by ysten zengzhiliang at 20191109:sync from zhangyang@ysten.com change 

		if(SystemProperties.get("ro.ysten.province","master").equals("cm201_shaanxi")){
                Log.d(TAG, "start shaanxi  iptv");
                String topPackageName = getTopActivity(mContext);
                if(!TextUtils.isEmpty(topPackageName) && !topPackageName.equals("com.android.smart.terminal.iptv")) {
                        if(!down){
                                Intent iptvIntent = new Intent();
                                iptvIntent.setAction("com.android.smart.terminal.iptv");
                                iptvIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                iptvIntent.putExtra("intentMsg", "EPGDomain");
                                mContext.startActivity(iptvIntent);
                        }
                        //add by zhanghk at 20190114:kill some apk not in whitelist
                        freeMemory(apkNameListSX);
                        return -1;
                }else{
                        //add by zhanghk at 20190114:kill some apk not in whitelist
                        freeMemory(apkNameListSX);
                        return 0;
                }
        }
		if(SystemProperties.get("ro.ysten.province","master").equals("cm201_xinjiang")){
                Log.d(TAG, "start xingjiang  iptv");
				doIPTVFuncationXJ(keyCode);
				return -1;
        }
        if(SystemProperties.get("ro.ysten.province","master").equals("cm201_neimeng")){
            Log.d("zhangy", "start neimeng  cm201-2");
            String topPackageName = getTopActivity(mContext);
            //begin: add by ysten wenglei at 20201025: neimeng cm211 home key
            if("CM211".equals(SystemProperties.get("persist.sys.nm.product",""))){
                if(!TextUtils.isEmpty(topPackageName) && !topPackageName.equals("com.gitv.launcher")) {
                    if(!down){
                        Intent iptvIntent = new Intent();
                        ComponentName componentIPTVName = new ComponentName("com.gitv.launcher",
                            "com.gitv.launcher.ui.WelcomeActivity");
                        iptvIntent.setComponent(componentIPTVName);
                        iptvIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(iptvIntent);
                    }
                    return -1;
                }else{
                    return 0;
                }
            }
            //end: add by ysten wenglei at 20201025: neimeng cm211 home key
            if(!TextUtils.isEmpty(topPackageName) && !topPackageName.equals("com.udte.launcher.cmcc")) {
                if(!down){
                    Intent iptvIntent = new Intent();
                    ComponentName componentIPTVName = new ComponentName("com.udte.launcher.cmcc","com.udte.launcher.cmcc.activity.SplashActivity");
                    iptvIntent.setComponent(componentIPTVName);
                    iptvIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(iptvIntent);
                }
                return -1;
            }else{
                return 0;
            }
        }
          String packeageJL=getTopActivity(mContext);
          //begin:add by xumiao at 20190725:home key
          if(SystemProperties.get("ro.ysten.province","master").equals("cm201_jilin") && (!packeageJL.equals("com.ysten.auth"))){
               Log.d(TAG, "start jilin  home");
               String topPackageName = getTopActivity(mContext);
               if(!TextUtils.isEmpty(topPackageName) && !topPackageName.equals("com.udte.launcher.cmcc")) {
                  if(!down){
                       Intent iptvIntent = new Intent();
                       ComponentName componentIPTVName = new ComponentName("com.wise.androidnativeiptv","com.jise.androidnativeiptv.login.activity.LoginActivity");
		  			 iptvIntent.setComponent(componentIPTVName);
		  			 iptvIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		  			 mContext.startActivity(iptvIntent);
                  }
                  return -1;
            }else{
               return 0;
             }
        }
        //end:add by xumiao at 20190725:home key
          //begin:add by xumiao at 20191119:liaoning iptv home key
          String packeageLNIPTV=getTopActivity(mContext);
          if(SystemProperties.get("ro.ysten.province","master").equals("CM201-2_liaoning_iptv")){
               Log.d(TAG, "start liaoning iptv  home");
               Intent intent = new Intent();
               intent.setAction("android.intent.action.HOME_KEY_PRESSED");
               mContext.sendBroadcast(intent);
               return 0;
           }
          //end:add by xumiao at 20190725:home key

		//add by xumiao at 20190725:home key
		if(SystemProperties.get("ro.ysten.province","master").equals("CM201-2_shanxi_iptv")
            || SystemProperties.get("ro.ysten.province","master").equals("M301H_shanxi")){
                Log.d(TAG, "start shanxi_iptv  iptv");
                String topPackageName = getTopActivity(mContext);
		//begin: add by ysten xumiao at 20181018:set home key trun on laucher 
		Log.d(TAG, "start shanxi_iptv  topPackageName " + topPackageName);
                if((SystemProperties.get("persist.sys.launcher.value","default").equals("iptv")) && (!TextUtils.isEmpty(topPackageName) && !topPackageName.equals("com.udte.launcher"))) {
                        if(!down){
                              	try {
			                        Intent iptvIntent = new Intent();
			                        ComponentName componentIPTVName = new ComponentName("com.udte.launcher","com.udte.launcher.SplashActivity");
			                        iptvIntent.setComponent(componentIPTVName);
			                        iptvIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			                        mContext.startActivity(iptvIntent);
		                            } catch (Exception e) {
			                            Log.e(TAG, "startICNTV error: " + e.toString());
		                        }
                        }
                        return -1;
                }else if((SystemProperties.get("persist.sys.launcher.value","default").equals("ott")) && (!TextUtils.isEmpty(topPackageName) && !topPackageName.equals("com.gitv.launcher"))) {
				       if(!down){
                              	try {
			                        Intent iptvIntent = new Intent();
			                        ComponentName componentIPTVName = new ComponentName("com.gitv.launcher","com.gitv.launcher.ui.WelcomeActivity");
			                        iptvIntent.setComponent(componentIPTVName);
			                        iptvIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			                        mContext.startActivity(iptvIntent);
		                            } catch (Exception e) {
			                            Log.e(TAG, "startICNTV error: " + e.toString());
		                        }
                        }
                        return -1;
				}else{
					return 0;
				}
		   //end: add by ysten xumiao at 20181018:set home key trun on laucher 
				
        }
            
            // If we have released the home key, and didn't do anything else
            // while it was pressed, then it is time to go home!
       if (!down&&SystemProperties.get("ro.ysten.province").contains("fujian")) {
		   
		    boolean launcherStarted=SystemProperties.getBoolean("persist.sys.launcher.started",true);
            if (!launcherStarted)
            {
            return -1;
            }
			
                ActivityManager am1 = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
                ComponentName cn1 = am1.getRunningTasks(1).get(0).topActivity;
                packageName = cn1.getPackageName();
                String className =cn1.getClassName();
				//begin by ysten.lizheng,20181012,fix homekey bug
                 if (packageName.contains("com.huawei.stb.tm")|| packageName.contains("com.fujian.provision")
					 || packageName.contains("com.ysten.stbguide")) {
                    return -1;
                }
				//end by ysten.lizheng,20181012,fix homekey bug
				String launcher=SystemProperties.get("persist.sys.launcher.value");
				
			      if("1".equals(launcher)){
				//start by lizheng 20190408 to kill process when back to home
				if(packageName.contains("com.holyblade.epg.platform.es.fujianyidong")
				||packageName.contains("tv.icntv.migu")){
				am1.forceStopPackage (packageName); 
				}
				  //end by lizheng 20190408 to kill process when back to home
                     sendBroadcastToHome();
                 }else if("2".equals(launcher)){
				 //start by lizheng 20190326
					 doIPTVFuncationFJ(keyCode);
				//end by lizheng 20190326	
                 }
            //if(!mHomeConsumed){
                			
            //}
			return -1;
		}
		
	    if (!down&&SystemProperties.get("ro.ysten.province").contains("jiangxi")) {
                ActivityManager am1 = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
                ComponentName cn1 = am1.getRunningTasks(1).get(0).topActivity;
                packageName = cn1.getPackageName();
                // edit by lingyun for jiangxi HOME keycode @20181129
                if (packageName.contains("com.jiangxi.provision") || packageName.contains("com.huawei.stb.tm1")) {
                    return -1;
                }
                String launcher = SystemProperties.get("persist.sys.launcher.value");
                Intent mIntent = new Intent();
                ComponentName lacomp = null;
                lacomp = new ComponentName("tv.icntv.ott", "tv.icntv.ott.icntv");
                Log.d(TAG,"jiangxi  home pressed");
                if ("1".equals(launcher)) {
                    String username = getValueFromStb("username");
                    String pwd = getValueFromStb("password");
                    if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(pwd)) {
                        Intent intent = new Intent();
                        intent.setAction("android.intent.action.HOME_KEY_PRESSED");
                        mContext.sendBroadcast(intent);
                        ComponentName homecomp = new ComponentName("tv.ott.launcher",
                                "tv.ott.launcher.WelcomeActivity");
                        Log.d(TAG,"jiangxi  home pressed go to tv.ott.launcher.WelcomeActivity");
                        Intent homeIntent = new Intent();
                        homeIntent.setComponent(homecomp);
                        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(homeIntent);
                    }
                } else if ("2".equals(launcher)) {
                    lacomp = new ComponentName("com.ysten.sjiptv", "com.ysten.sjiptv.app.SJIPTVActivity");
                    // add by guangchao.su ad 20180811:add jiangxi iptv home
                    String topPackageName = getTopActivity(mContext);
                    if(!TextUtils.isEmpty(topPackageName)){
                        if(topPackageName.equals("com.ysten.sjiptv")){
                            doIPTVFuncationJX(keyCode);
                            return -1;
                        }else{
                            mIntent.putExtra("intentMsg", "EPGDomain");
                        }
                    }
                    Log.i("ysten_homekey", "com.ysten.sjiptv" + ", KeyEvent.KEYCODE_HOME is "
                            + KeyEvent.KEYCODE_HOME);
                    // add by guangchao.su ad 20180811:add jiangxi iptv home
                    mIntent.setComponent(lacomp);
                    mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(mIntent);
                }
                // edit by lingyun for jiangxi HOME keycode @20181129 end
				      freeMemory(apkNameListJX,packageName);
			          return -1;
		}
           if(!down&&SystemProperties.get("ro.product.target").equals("shcmcc")&&!SystemProperties.get("ro.ysten.province","master").contains("fujian")){
                String packagename = SystemProperties.get("epg.launcher.packagename");
                 //Log.w(TAG, "get packagname="+packagename);
                 //HISILICON add begin
                 if(packagename!=null&&!packagename.equals("")){
                     //Intent intent = new Intent(Intent.ACTION_MAIN);
                     //intent.setPackage(packagename);
                     Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(packagename);
                     intent.putExtra("GoHome", "true");
                    intent.setAction("com.ysten.sjiptv.homepage");
                     //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                     //intent.addCategory(Intent.CATEGORY_LAUNCHER);
                     mContext.startActivity(intent);
                     return -1;
                 }else if(isPackageExist("net.sunniwell.launcher.chinamobile")) {
                     Intent intent = new Intent(Intent.ACTION_MAIN);
                     intent.setClassName("net.sunniwell.launcher.chinamobile", "net.sunniwell.launcher.chinamobile.MainActivity");
                     intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    intent.setAction("com.ysten.sjiptv.homepage");
                     intent.addCategory(Intent.CATEGORY_LAUNCHER);
                     mContext.startActivity(intent);
                    // Log.w(TAG, "net.sunniwell.launcher.chinamobile");
                    return -1;
                } else {
                    /*
                     * if net.sunniwell.launcher.chinamobile does not exist,
                     * using Android Launcher
                    */
                    Log.d(TAG, "using android launcher");
                }
                //HISILICON add end
              }
		   //begain add by zhangy @ 20191028 for henan home key
		   	if(SystemProperties.get("ro.ysten.province","master").contains("henan")){
				String username = "";
		        Uri uri = Uri.parse("content://stbconfig/authentication/username");
		
		        try{
			        Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
			        if(cursor != null){
				    while (cursor.moveToNext()) {
                            username = cursor.getString(cursor.getColumnIndex("value"));
                    }
				    cursor.close();
			        }
		        } catch(Exception e){
			       Log.d(TAG, "queryDBValue Execption "+e.toString());
		        }
		       Log.d("zhanghk","username:"+username);
		       if(TextUtils.isEmpty(username)){
		            return -1;
		        }
		       if(down && repeatCount == 0) {
			   ActivityManager mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
			   Intent mintent = new Intent();
			   mintent.setAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
			   mContext.sendBroadcast(mintent);
		
		       String packagename = SystemProperties.get("epg.launcher.packagename");
		       if(packagename == null || packagename.equals("")) {
               Slog.d(TAG,"KEYCODE_HOME start com.huawei.tvbox");
			   Intent intent = new Intent(Intent.ACTION_MAIN);
			   intent.setClassName("com.huawei.tvbox","com.huawei.tvbox.activity.LauncherActivity");
			   intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			   intent.addCategory(Intent.CATEGORY_LAUNCHER);
               intent.addCategory(Intent.CATEGORY_HOME);
			   mContext.startActivity(intent);	
		       }else {
			      Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(packagename);
			      intent.putExtra("GoHome", "true");
			      mContext.startActivity(intent);
		        }
		       return -1;
		      }
			}	   
		   //end add by zhangy @ 20191028 for hennan home key
           /*begin:add by zhanghk at 20190308:start hunan launcher*/
           if(!down&&SystemProperties.get("ro.product.target").equals("shcmcc")&&!SystemProperties.get("ro.ysten.province","master").contains("hunan")){
               String packagename = SystemProperties.get("epg.launcher.packagename");
               if(packagename!=null&&!packagename.equals("")){
                   Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(packagename);
                   intent.putExtra("GoHome", "true");
                   mContext.startActivity(intent);
                   return -1;
               }else if(isPackageExist("net.sunniwell.launcher.chinamobile")) {
                   Intent intent = new Intent(Intent.ACTION_MAIN);
                   intent.setClassName("net.sunniwell.launcher.chinamobile", "net.sunniwell.launcher.chinamobile.MainActivity");
                   intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                   intent.addCategory(Intent.CATEGORY_LAUNCHER);
                   mContext.startActivity(intent);
                  return -1;
               }else{
                   /*
                    * if net.sunniwell.launcher.chinamobile does not exist,
                    * using Android Launcher
                    */
                    Log.d(TAG, "using android launcher");
               }
            }
           /*end:add by zhanghk at 20190308:start hunan launcher*/
			  
            if (!down) {
                cancelPreloadRecentApps();

                mHomePressed = false;
                if (mHomeConsumed) {
                    mHomeConsumed = false;
                    return -1;
                }

                if (canceled) {
                    Log.i(TAG, "Ignoring HOME; event canceled.");
                    return -1;
                }

                // If an incoming call is ringing, HOME is totally disabled.
                // (The user is already on the InCallScreen at this point,
                // and his ONLY options are to answer or reject the call.)
                try {
                    ITelephony telephonyService = getTelephonyService();
                    if (telephonyService != null && telephonyService.isRinging()) {
                        Log.i(TAG, "Ignoring HOME; there's a ringing incoming call.");
                        return -1;
                    }
                } catch (RemoteException ex) {
                    Log.w(TAG, "RemoteException from getPhoneInterface()", ex);
                }

                // Delay handling home if a double-tap is possible.
                if (mDoubleTapOnHomeBehavior != DOUBLE_TAP_HOME_NOTHING) {
                    mHandler.removeCallbacks(mHomeDoubleTapTimeoutRunnable); // just in case
                    mHomeDoubleTapPending = true;
                    mHandler.postDelayed(mHomeDoubleTapTimeoutRunnable,
                            ViewConfiguration.getDoubleTapTimeout());
                    return -1;
                }

                // Go home!
				Intent panelIntent=new Intent("android.intent.action.HOME_KEY_PRESSED");
				mContext.sendBroadcast(panelIntent);
                launchHomeFromHotKey();
                return -1;
            }

            // If a system window has focus, then it doesn't make sense
            // right now to interact with applications.
            WindowManager.LayoutParams attrs = win != null ? win.getAttrs() : null;
            if (attrs != null) {
                final int type = attrs.type;
                if (type == WindowManager.LayoutParams.TYPE_KEYGUARD
                        || type == WindowManager.LayoutParams.TYPE_KEYGUARD_SCRIM
                        || type == WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG) {
                    // the "app" is keyguard, so give it the key
                    return 0;
                }
                final int typeCount = WINDOW_TYPES_WHERE_HOME_DOESNT_WORK.length;
                for (int i=0; i<typeCount; i++) {
                    if (type == WINDOW_TYPES_WHERE_HOME_DOESNT_WORK[i]) {
                        // don't do anything, but also don't pass it to the app
                        return -1;
                    }
                }
            }
             Log.i(TAG, "mHomePressed repeatCount = "+repeatCount);
            // Remember that home is pressed and handle special actions.
            if (repeatCount == 0) {
                mHomePressed = true;
                if (mHomeDoubleTapPending) {
                    mHomeDoubleTapPending = false;
                    mHandler.removeCallbacks(mHomeDoubleTapTimeoutRunnable);
                    handleDoubleTapOnHome();
                } else if (mLongPressOnHomeBehavior == LONG_PRESS_HOME_RECENT_SYSTEM_UI
                        || mDoubleTapOnHomeBehavior == DOUBLE_TAP_HOME_RECENT_SYSTEM_UI) {
                    preloadRecentApps();
                }
            } else if ((event.getFlags() & KeyEvent.FLAG_LONG_PRESS) != 0) {
                if (!keyguardOn) {
                    handleLongPressOnHome();
                }
            }
            return -1;
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            // Hijack modified menu keys for debugging features
			
		if(SystemProperties.get("ro.ysten.province","master").contains("fujian")){
           if (SystemProperties.getInt("persist.sys.launcher.value",0)==2){
           return -1;
        }
        }
            //begin:add by zhanghk at 20190124:enable menu key useless 
            if(SystemProperties.get("ro.ysten.province","master").contains("hubei")){
		if(getTopActivity(mContext).contains("iptv")){
		       return -1;
		}
            }
            //end:add by zhanghk at 20190124:enable menu key useless 
		
            final int chordBug = KeyEvent.META_SHIFT_ON;

            if (down && repeatCount == 0) {
                if (mEnableShiftMenuBugReports && (metaState & chordBug) == chordBug) {
                    Intent intent = new Intent(Intent.ACTION_BUG_REPORT);
                    mContext.sendOrderedBroadcastAsUser(intent, UserHandle.CURRENT,
                            null, null, null, 0, null, null);
                    return -1;
                } else if (SHOW_PROCESSES_ON_ALT_MENU &&
                        (metaState & KeyEvent.META_ALT_ON) == KeyEvent.META_ALT_ON) {
                    Intent service = new Intent();
                    service.setClassName(mContext, "com.android.server.LoadAverageService");
                    ContentResolver res = mContext.getContentResolver();
                    boolean shown = Settings.Global.getInt(
                            res, Settings.Global.SHOW_PROCESSES, 0) != 0;
                    if (!shown) {
                        mContext.startService(service);
                    } else {
                        mContext.stopService(service);
                    }
                    Settings.Global.putInt(
                            res, Settings.Global.SHOW_PROCESSES, shown ? 0 : 1);
                    return -1;
                }
            }
        } else if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            if (down) {
                if (repeatCount == 0) {
                    mSearchKeyShortcutPending = true;
                    mConsumeSearchKeyUp = false;
                }
            } else {
                mSearchKeyShortcutPending = false;
                if (mConsumeSearchKeyUp) {
                    mConsumeSearchKeyUp = false;
                    return -1;
                }
            }
            return 0;
        }  
       //begin:add by ysten zengzhiliang at 20190321:touch key control mic
        else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
			Log.d(TAG, "send broadcast mic");
            if (down) {
					long time = System.currentTimeMillis();
				if(time - mKeyMicPressTime > 3000){
					mKeyMicPressTime = System.currentTimeMillis();
				}   else {
					Log.d(TAG, "send broadcast mic small 3s");
					return -1;
				}
				if(mKeyMicFlag){
			        Log.d(TAG, "XXXXXXXXXXXX start mic");
					mKeyMicFlag = false;
					Intent intent = null;
					intent = new Intent("com.lhxk.voice_recognition_start");
					intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
					mContext.sendBroadcast(intent);  
					if(SystemProperties.get("ro.ysten.province","master").contains("cm201_hebei")){
						Log.d(TAG, "隐藏toast-打开MIC,河北CM201-2暂无小加语音需求");
					}else{
						toastHandle.sendEmptyMessage(4);
					}
				}else{
			        Log.d(TAG, "XXXXXXXXXXXX stop mic");
					mKeyMicFlag = true;
                    Intent intent = null;
					intent = new Intent("com.lhxk.voice_recognition_end");
					intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
					mContext.sendBroadcast(intent); 
					if(SystemProperties.get("ro.ysten.province","master").contains("cm201_hebei")){
						Log.d(TAG, "隐藏toast-关闭MIC,河北CM201-2暂无小加语音需求");
					}else{
						toastHandle.sendEmptyMessage(5);
					}
				}
            } else {
            }
            return -1;
        //end:add by ysten zengzhiliang at 20190321:touch key control mic
         } else if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            if (!keyguardOn) {
                if (down && repeatCount == 0) {
                    preloadRecentApps();
                } else if (!down) {
                    toggleRecentApps();
                }
            }
            return -1;
        }else if(keyCode == KeyEvent.KEYCODE_MMM_Z)
        {
            //begin:add by zengzhiliang at 20200403:guizhou support M KEY
            if(down && (SystemProperties.get("ro.ysten.province","master").contains("guizhou")))
            {
                Log.d(TAG,"M Key");
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.yd.appstore.ott", "wd.android.app.ui.activity.InitActivity"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                return -1;
            }
            return 0;
            //end:add by zengzhiliang at 20200403:guizhou support M KEY
        }
        else if (keyCode == KeyEvent.KEYCODE_SETTINGS) {
            Log.i(TAG, " INTO  mLongPressSetting KeyEvent.KEYCODE_SETTINGS");
            //begin: add by tianchining at 20190925: shandong
            if(SystemProperties.get("ro.ysten.province","master").equals("cm201_shandong") || SystemProperties.get("ro.ysten.province","master").equals("cm201_anhui_iptv")){
                Log.d(TAG, "TCN_ADD cm201_shandong settings keycode");
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.ysten.setting", "com.ysten.setting.MainActivity"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                return -1;
            }
            //end: add by tianchining at 20190925: shandong
            if((SystemProperties.get("ro.ysten.province","master").equals("cm201_anhui")
                        // add by ysten zengzhiliang at 20181109:add m301h_anhui
                        ||SystemProperties.get("ro.ysten.province","master").equals("m301h_anhui"))
                    &&SystemProperties.get("sys.key.settings", "on").equals("on")){
                Intent intent = new Intent();
                           intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.SettingsMainActivity"));
		                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                           mContext.startActivity(intent);
            return -1;
            }
              if(SystemProperties.get("ro.ysten.province","master").equals("cm201_neimeng")){
                 Log.d("zhangy", " cm201_neimeng key set");
                Intent intent = new Intent();
                           intent.setComponent(new ComponentName("com.ysten.setting", "com.ysten.setting.style1.MainActivity"));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                           mContext.startActivity(intent);
            return -1;
            }
            //begin add setting by weichuyan for jilin
	     if(SystemProperties.get("ro.ysten.province","master").equals("cm201_jilin")){
                 Log.d("weichuyan", " cm201_jilin key set");
                Intent intent = new Intent();
                           intent.setComponent(new ComponentName("com.ysten.setting", "com.ysten.setting.MainActivity"));
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                           mContext.startActivity(intent);
            return -1;
            }
	    //end add setting by weichuyan for jilin
            if(SystemProperties.get("ro.ysten.province","master").equals("cm201_yunnan") ||
            		SystemProperties.get("ro.ysten.province","master").equals("c60_yunnan") ||
                    SystemProperties.get("ro.ysten.province","master").equals("m301h_yunnan")){
					int isResponSettingKey=SystemProperties.getInt("persist.sys.yst.key.setting", 1);
					if(isResponSettingKey!=1){
                        return -1;
                    }
                    if(event.getAction() == KeyEvent.ACTION_UP)
					{
				    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.ysten.setting", "com.ysten.setting.MainActivity"));
    	            intent.putExtra("show_lock_in_setting",true);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                     mContext.startActivity(intent);
					
					}

            return -1;
            }
            //add for jiangxi setting
            if(SystemProperties.get("ro.ysten.province","master").contains("jiangxi")){
                if(event.getAction() == KeyEvent.ACTION_UP){
				    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.ysten.setting", "com.ysten.setting.style1.MainActivity"));
    	            intent.putExtra("show_lock_in_setting",true);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);
					
				}
                return -1;
            }
            if (!keyguardOn) {
                if (down && repeatCount > 10) {
                     Log.i(TAG, "  KeyEvent.KEYCODE_SETTINGS repeatCount >10");
                    if (SystemProperties.get("ro.product.target").equals("shcmcc")){
                           mLongPressSetting = true;
                           Intent intent = new Intent();
                           intent.setComponent(new ComponentName("com.ysten.setting", "com.ysten.setting.SubActivity"));
    	                   intent.putExtra("show_lock_in_setting",true);
                           intent.putExtra("position",2);
		           intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                           mContext.startActivity(intent);
			   Log.i("ysten_sgc", " SHYD start action com.ysten.setting33333 ");
                         
                       }
                }
            }
            return -1;
        }else if (keyCode == KeyEvent.KEYCODE_ASSIST) {
            if (down) {
                if (repeatCount == 0) {
                    mAssistKeyLongPressed = false;
                } else if (repeatCount == 1) {
                    mAssistKeyLongPressed = true;
                    if (!keyguardOn) {
                         launchAssistLongPressAction();
                    }
                }
            } else {
                if (mAssistKeyLongPressed) {
                    mAssistKeyLongPressed = false;
                } else {
                    if (!keyguardOn) {
                        launchAssistAction();
                    }
                }
            }
            return -1;
        } else if (keyCode == KeyEvent.KEYCODE_SYSRQ) {
            // start edit by lingyun jiangxi  @20181211
            if (down && repeatCount == 0 && !SystemProperties.get("ro.ysten.province","master").contains("jiangxi")) {
            // end edit by lingyun for jiangxi  @20181211
                mHandler.post(mScreenshotRunnable);
                return -1;
            }
        } else if (keyCode == KeyEvent.KEYCODE_BRIGHTNESS_UP
                || keyCode == KeyEvent.KEYCODE_BRIGHTNESS_DOWN) {
            if (down) {
                int direction = keyCode == KeyEvent.KEYCODE_BRIGHTNESS_UP ? 1 : -1;

                // Disable autobrightness if it's on
                int auto = Settings.System.getIntForUser(
                        mContext.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                        UserHandle.USER_CURRENT_OR_SELF);
                if (auto != 0) {
                    Settings.System.putIntForUser(mContext.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL,
                            UserHandle.USER_CURRENT_OR_SELF);
                }

                int min = mPowerManager.getMinimumScreenBrightnessSetting();
                int max = mPowerManager.getMaximumScreenBrightnessSetting();
                int step = (max - min + BRIGHTNESS_STEPS - 1) / BRIGHTNESS_STEPS * direction;
                int brightness = Settings.System.getIntForUser(mContext.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS,
                        mPowerManager.getDefaultScreenBrightnessSetting(),
                        UserHandle.USER_CURRENT_OR_SELF);
                brightness += step;
                // Make sure we don't go beyond the limits.
                brightness = Math.min(max, brightness);
                brightness = Math.max(min, brightness);

                Settings.System.putIntForUser(mContext.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, brightness,
                        UserHandle.USER_CURRENT_OR_SELF);
                Intent intent = new Intent(Intent.ACTION_SHOW_BRIGHTNESS_DIALOG);
                mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT_OR_SELF);
            }
            return -1;
        }else if(keyCode == KeyEvent.KEYCODE_TV || keyCode == KeyEvent.KEYCODE_MOVIE){
            if(!down&&SystemProperties.get("ro.ysten.province","master").contains("cm201_hebei")){ 
                doIPTVFuncationHE(keyCode); 
				return -1;
            }
		}else if(keyCode == KeyEvent.KEYCODE_FAVOURITE){
            if(!down&&SystemProperties.get("ro.ysten.province","master").contains("cm201_xinjiang")){ 
                //doIPTVFuncationXJ(keyCode); 
				Log.d("ysten_cm201", " use this KEYCODE_FAVOURITE exit");
				return -1;
            }			
		}

        // Shortcuts are invoked through Search+key, so intercept those here
        // Any printing key that is chorded with Search should be consumed
        // even if no shortcut was invoked.  This prevents text from being
        // inadvertently inserted when using a keyboard that has built-in macro
        // shortcut keys (that emit Search+x) and some of them are not registered.
        if (mSearchKeyShortcutPending) {
            final KeyCharacterMap kcm = event.getKeyCharacterMap();
            if (kcm.isPrintingKey(keyCode)) {
                mConsumeSearchKeyUp = true;
                mSearchKeyShortcutPending = false;
                if (down && repeatCount == 0 && !keyguardOn) {
                    Intent shortcutIntent = mShortcutManager.getIntent(kcm, keyCode, metaState);
                    if (shortcutIntent != null) {
                        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        try {
                            mContext.startActivityAsUser(shortcutIntent, UserHandle.CURRENT);
                        } catch (ActivityNotFoundException ex) {
                            Slog.w(TAG, "Dropping shortcut key combination because "
                                    + "the activity to which it is registered was not found: "
                                    + "SEARCH+" + KeyEvent.keyCodeToString(keyCode), ex);
                        }
                    } else {
                        Slog.i(TAG, "Dropping unregistered shortcut key combination: "
                                + "SEARCH+" + KeyEvent.keyCodeToString(keyCode));
                    }
                }
                return -1;
            }
        }

        // Invoke shortcuts using Meta.
        if (down && repeatCount == 0 && !keyguardOn
                && (metaState & KeyEvent.META_META_ON) != 0) {
            final KeyCharacterMap kcm = event.getKeyCharacterMap();
            if (kcm.isPrintingKey(keyCode)) {
                Intent shortcutIntent = mShortcutManager.getIntent(kcm, keyCode,
                        metaState & ~(KeyEvent.META_META_ON
                                | KeyEvent.META_META_LEFT_ON | KeyEvent.META_META_RIGHT_ON));
                if (shortcutIntent != null) {
                    shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        mContext.startActivityAsUser(shortcutIntent, UserHandle.CURRENT);
                    } catch (ActivityNotFoundException ex) {
                        Slog.w(TAG, "Dropping shortcut key combination because "
                                + "the activity to which it is registered was not found: "
                                + "META+" + KeyEvent.keyCodeToString(keyCode), ex);
                    }
                    return -1;
                }
            }
        }

        // Handle application launch keys.
        if (down && repeatCount == 0 && !keyguardOn) {
            String category = sApplicationLaunchKeyCategories.get(keyCode);
            if (category != null) {
                Intent intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    mContext.startActivityAsUser(intent, UserHandle.CURRENT);
                } catch (ActivityNotFoundException ex) {
                    Slog.w(TAG, "Dropping application launch key because "
                            + "the activity to which it is registered was not found: "
                            + "keyCode=" + keyCode + ", category=" + category, ex);
                }
                return -1;
            }
        }

        // Display task switcher for ALT-TAB or Meta-TAB.
        if (down && repeatCount == 0 && keyCode == KeyEvent.KEYCODE_TAB) {
            if (mRecentAppsDialogHeldModifiers == 0 && !keyguardOn) {
                final int shiftlessModifiers = event.getModifiers() & ~KeyEvent.META_SHIFT_MASK;
                if (KeyEvent.metaStateHasModifiers(shiftlessModifiers, KeyEvent.META_ALT_ON)
                        || KeyEvent.metaStateHasModifiers(
                                shiftlessModifiers, KeyEvent.META_META_ON)) {
                    mRecentAppsDialogHeldModifiers = shiftlessModifiers;
                    showOrHideRecentAppsDialog(RECENT_APPS_BEHAVIOR_EXIT_TOUCH_MODE_AND_SHOW);
                    return -1;
                }
            }
        } else if (!down && mRecentAppsDialogHeldModifiers != 0
                && (metaState & mRecentAppsDialogHeldModifiers) == 0) {
            mRecentAppsDialogHeldModifiers = 0;
            showOrHideRecentAppsDialog(keyguardOn ? RECENT_APPS_BEHAVIOR_DISMISS :
                    RECENT_APPS_BEHAVIOR_DISMISS_AND_SWITCH);
        }

        // Handle keyboard language switching.
        if (down && repeatCount == 0
                && (keyCode == KeyEvent.KEYCODE_LANGUAGE_SWITCH
                        || (keyCode == KeyEvent.KEYCODE_SPACE
                                && (metaState & KeyEvent.META_CTRL_MASK) != 0))) {
            int direction = (metaState & KeyEvent.META_SHIFT_MASK) != 0 ? -1 : 1;
            mWindowManagerFuncs.switchKeyboardLayout(event.getDeviceId(), direction);
            return -1;
        }
        if (mLanguageSwitchKeyPressed && !down
                && (keyCode == KeyEvent.KEYCODE_LANGUAGE_SWITCH
                        || keyCode == KeyEvent.KEYCODE_SPACE)) {
            mLanguageSwitchKeyPressed = false;
            return -1;
        }

        if (mGlobalKeyManager.handleGlobalKey(mContext, keyCode, event)) {
            return -1;
        }
        if(SystemProperties.get("ro.ysten.province","master").contains("jidi")) {
            if(down && robotKeyComponent(keyCode)) {
		      Slog.d(TAG,"catchrobot");
		      Intent intentset = new Intent(Intent.ACTION_MAIN);
		      intentset.setClassName("cmccwasu.systemserver","cmccwasu.systemserver.MainActivity");
		      intentset.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		      mContext.startActivity(intentset);
		      Slog.w(TAG, "robot start.");
            }
            if(down && robotKeyComponent1(keyCode)) {
		      Slog.d(TAG,"jump wifi");
		      Intent intentset = new Intent(Intent.ACTION_MAIN);
		      intentset.setClassName("cmccwasu.systemserver","cmccwasu.systemserver.WifiControl");
		      intentset.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		      mContext.startActivity(intentset);
		      Slog.w(TAG, "jidi wifi control start.");
            }
        }
        
        // Let the application handle the key.
        return 0;
    }
    //begin: add by zongzy 20190117: add for jidi delaycheck
    private static final int[] ROBOT_KEY_LIST = {
            KeyEvent.KEYCODE_MENU,
            //begin: add by penghui 20191117: add for 基地框架酒店模式修改组合键
	    KeyEvent.KEYCODE_6,
            KeyEvent.KEYCODE_6,
            KeyEvent.KEYCODE_6
            // end: add by penghui 20191117
            //KeyEvent.KEYCODE_DPAD_LEFT,
           // KeyEvent.KEYCODE_MENU,
           // KeyEvent.KEYCODE_DPAD_LEFT,
           // KeyEvent.KEYCODE_MENU,
           // KeyEvent.KEYCODE_DPAD_LEFT
    };
    private static int mCurrentRobotKeyIndex = 0;
    private static boolean robotRunning = false;

    private boolean robotKeyComponent(int keyCode) {
            final int keyCount = ROBOT_KEY_LIST.length;
            Slog.d(TAG, "keyCode " + keyCode + " mCurrentRobotKeyIndex" + mCurrentRobotKeyIndex);
            if(mCurrentRobotKeyIndex >= keyCount) {
                mCurrentRobotKeyIndex = 0;
            }
            if(keyCode == ROBOT_KEY_LIST[mCurrentRobotKeyIndex]||(keyCode==0&&mCurrentRobotKeyIndex==0)||(keyCode==0&&mCurrentRobotKeyIndex==keyCount-1)) {
                    mCurrentRobotKeyIndex++;
                    if(mCurrentRobotKeyIndex == keyCount) {
                        return true;
                    }
            }else{
                    mCurrentRobotKeyIndex = 0;
                    if(keyCode == ROBOT_KEY_LIST[0]) {
                        mCurrentRobotKeyIndex++;
                            return false;
                    }
            }
            return false;
    }
    private static final int[] ROBOT_KEY_LIST1 = {
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_LEFT
    };
    private static int mCurrentRobotKeyIndex1 = 0;
    private static boolean robotRunning1 = false;

    private boolean robotKeyComponent1(int keyCode) {
            final int keyCount = ROBOT_KEY_LIST1.length;
            Slog.d(TAG, "keyCode " + keyCode + " mCurrentRobotKeyIndex" + mCurrentRobotKeyIndex1);
            if(mCurrentRobotKeyIndex1 >= keyCount) {
                mCurrentRobotKeyIndex1 = 0;
            }
 
            //begin: add by xuyunfeng 20191014 for Shield wifi in DisplayActivity
            ActivityManager activityManager=(ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
            String currentPackageClassName=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
			 Slog.d(TAG, "currentPackageClassName " + currentPackageClassName);
            if((keyCode == ROBOT_KEY_LIST1[mCurrentRobotKeyIndex1]||(keyCode==0&&mCurrentRobotKeyIndex1==0)||(keyCode==0&&mCurrentRobotKeyIndex1==keyCount-1))
                && getTopActivityInfo(mContext).equals("com.shcmcc.setting")
			&& (currentPackageClassName.equals("com.shcmcc.setting.NetworkActivity"))) {
					//com.shcmcc.setting.DisplayActivity
                //end: add by xuyunfeng 20191014 for Shield wifi in DisplayActivity
                    mCurrentRobotKeyIndex1++;
                    if(mCurrentRobotKeyIndex1 == keyCount) {
                        return true;
                    }
            }else{
                    mCurrentRobotKeyIndex1 = 0;
                    if(keyCode == ROBOT_KEY_LIST1[0]) {
                        mCurrentRobotKeyIndex1++;
                            return false;
                    }
            }
            return false;
    }
        private String getTopActivityInfo(Context context) {
            ActivityManager manager = ((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE));
            //TopActivityInfo info = new TopActivityInfo();
                List localList = manager.getRunningTasks(1);
                ActivityManager.RunningTaskInfo localRunningTaskInfo = (ActivityManager.RunningTaskInfo)localList.get(0);
                String packageName = localRunningTaskInfo.topActivity.getPackageName();
                //info.topActivityName = localRunningTaskInfo.topActivity.getClassName();
            return packageName;
        }
    //end: add by zongzy 20190117: add for jidi delaycheck
    
    //baiyh add for sendbroadcast to launcher begin
    private void sendBroadcastToHome(){
		int homeKey = Settings.System.getInt(mContext.getContentResolver(), "IS_RESPOND_HOME_KEY", 0);
		if (homeKey!=0){
			return;
		}
		
 		String authStatus=getValueFromStb("authStatus")+"";
		String user_token=getValueFromStb("user_token")+"";
		Log.d(TAG,"authStatus="+authStatus+","+"user_token="+user_token);
		if(authStatus.equals("AuthSuccess")&&!user_token.equals("")){
            sendCloseSystemWindows(SYSTEM_DIALOG_REASON_HOME_KEY);
            try {
                Intent home=new Intent();
                home = new Intent(Intent.ACTION_MAIN);
                home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//|Intent.FLAG_ACTIVITY_CLEAR_TOP);//|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                home.addCategory(Intent.CATEGORY_HOME);
                mContext.startActivity(home);
                //解决打开persist.sys.launcher.switch后待机唤醒时ott直播按首页键无法返回
                /*Intent homeIntent=new Intent();
                ComponentName homeName = new ComponentName("tv.icntv.ott","tv.icntv.ott.icntv");
                homeIntent.setComponent(homeName);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                mContext.startActivity(homeIntent);*/
            } catch(Exception ex) {
                /*am.forceStopPackage ("tv.icntv.ott");
                Intent homeIntent=new Intent();
                ComponentName homeName = new ComponentName("tv.icntv.ott","tv.icntv.ott.icntv");
                homeIntent.setComponent(homeName);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                mContext.startActivity(homeIntent);	*/
            }
            Intent intent=new Intent();
            intent.setAction("android.intent.action.HOME_KEY_PRESSED");
	   	    Log.d(TAG,"send android.intent.action.HOME_KEY_PRESSED");
            mContext.sendBroadcast(intent);
		}
	}
    //baiyh add for sendbroadcast to launcher end
	public String getValueFromStb(String name){
		Uri uri = Uri.parse("content://stbconfig/authentication/"+name);
		Cursor mCursor = mContext.getContentResolver().query(uri, null, null, null, null);
		if (mCursor != null) {
	    	while (mCursor.moveToNext()) {
	       		String value = mCursor.getString(mCursor.getColumnIndex("value"));
	       		return value;
	    	 }
	    	 mCursor.close();
		 }
			
		return "";
     }


    private Handler toastHandle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            switch (msg.what) {
            case 1:
				Toast.makeText(mContext, mContext.getResources().getString(com.android.internal.R.string.open_voice), Toast.LENGTH_SHORT).show();
            	break;
            case 2:
				Toast.makeText(mContext, mContext.getResources().getString(com.android.internal.R.string.ysten_app_not_found), Toast.LENGTH_SHORT).show();
                break;
            //begin: add by ysten zengzhiliang at 20180911 add totast
            case 3:
				Toast.makeText(mContext, mContext.getResources().getString(com.android.internal.R.string.adbd_open_note), Toast.LENGTH_SHORT).show();
                //add by ysten zengzhiliang at 20180921: fix LocalFileManager can not install apk
				SystemProperties.set("sys.service.adbd.enable",  "0");
            	break;  
            //end: add by ysten zengzhiliang at 20180911 add totast
            case 4:
				Toast.makeText(mContext, mContext.getResources().getString(com.android.internal.R.string.start_mic), Toast.LENGTH_SHORT).show();
            	break;  
            case 5:
				Toast.makeText(mContext, mContext.getResources().getString(com.android.internal.R.string.stop_mic), Toast.LENGTH_SHORT).show();
                break;
            case 100:
                showSuspendDialog_FJ();
            	break;  
            }
        }
    };  

    /** {@inheritDoc} */
    @Override
    public KeyEvent dispatchUnhandledKey(WindowState win, KeyEvent event, int policyFlags) {
        // Note: This method is only called if the initial down was unhandled.
        if (DEBUG_INPUT) {
            Slog.d(TAG, "Unhandled key: win=" + win + ", action=" + event.getAction()
                    + ", flags=" + event.getFlags()
                    + ", keyCode=" + event.getKeyCode()
                    + ", scanCode=" + event.getScanCode()
                    + ", metaState=" + event.getMetaState()
                    + ", repeatCount=" + event.getRepeatCount()
                    + ", policyFlags=" + policyFlags);
        }

        KeyEvent fallbackEvent = null;
        if ((event.getFlags() & KeyEvent.FLAG_FALLBACK) == 0) {
            final KeyCharacterMap kcm = event.getKeyCharacterMap();
            final int keyCode = event.getKeyCode();
            final int metaState = event.getMetaState();
            final boolean initialDown = event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getRepeatCount() == 0;

            // Check for fallback actions specified by the key character map.
            final FallbackAction fallbackAction;
            if (initialDown) {
                fallbackAction = kcm.getFallbackAction(keyCode, metaState);
            } else {
                fallbackAction = mFallbackActions.get(keyCode);
            }

            if (fallbackAction != null) {
                if (DEBUG_INPUT) {
                    Slog.d(TAG, "Fallback: keyCode=" + fallbackAction.keyCode
                            + " metaState=" + Integer.toHexString(fallbackAction.metaState));
                }

                final int flags = event.getFlags() | KeyEvent.FLAG_FALLBACK;
                fallbackEvent = KeyEvent.obtain(
                        event.getDownTime(), event.getEventTime(),
                        event.getAction(), fallbackAction.keyCode,
                        event.getRepeatCount(), fallbackAction.metaState,
                        event.getDeviceId(), event.getScanCode(),
                        flags, event.getSource(), null);

                if (!interceptFallback(win, fallbackEvent, policyFlags)) {
                    fallbackEvent.recycle();
                    fallbackEvent = null;
                }

                if (initialDown) {
                    mFallbackActions.put(keyCode, fallbackAction);
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    mFallbackActions.remove(keyCode);
                    fallbackAction.recycle();
                }
            }
        }

        if (DEBUG_INPUT) {
            if (fallbackEvent == null) {
                Slog.d(TAG, "No fallback.");
            } else {
                Slog.d(TAG, "Performing fallback: " + fallbackEvent);
            }
        }
        return fallbackEvent;
    }

    private boolean interceptFallback(WindowState win, KeyEvent fallbackEvent, int policyFlags) {
        int actions = interceptKeyBeforeQueueing(fallbackEvent, policyFlags, true);
        if ((actions & ACTION_PASS_TO_USER) != 0) {
            long delayMillis = interceptKeyBeforeDispatching(
                    win, fallbackEvent, policyFlags);
            if (delayMillis == 0) {
                return true;
            }
        }
        return false;
    }

    private void launchAssistLongPressAction() {
        performHapticFeedbackLw(null, HapticFeedbackConstants.LONG_PRESS, false);
        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_ASSIST);

        // launch the search activity
        Intent intent = new Intent(Intent.ACTION_SEARCH_LONG_PRESS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            // TODO: This only stops the factory-installed search manager.  
            // Need to formalize an API to handle others
            SearchManager searchManager = getSearchManager();
            if (searchManager != null) {
                searchManager.stopSearch();
            }
            mContext.startActivityAsUser(intent, UserHandle.CURRENT);
        } catch (ActivityNotFoundException e) {
            Slog.w(TAG, "No activity to handle assist long press action.", e);
        }
    }

    private void launchAssistAction() {
        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_ASSIST);
        Intent intent = ((SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE))
                .getAssistIntent(mContext, true, UserHandle.USER_CURRENT);
        if (intent != null) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            try {
                mContext.startActivityAsUser(intent, UserHandle.CURRENT);
            } catch (ActivityNotFoundException e) {
                Slog.w(TAG, "No activity to handle assist action.", e);
            }
        }
    }

    private SearchManager getSearchManager() {
        if (mSearchManager == null) {
            mSearchManager = (SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE);
        }
        return mSearchManager;
    }

    private void preloadRecentApps() {
        mPreloadedRecentApps = true;
        try {
            IStatusBarService statusbar = getStatusBarService();
            if (statusbar != null) {
                statusbar.preloadRecentApps();
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException when preloading recent apps", e);
            // re-acquire status bar service next time it is needed.
            mStatusBarService = null;
        }
    }

    private void cancelPreloadRecentApps() {
        if (mPreloadedRecentApps) {
            mPreloadedRecentApps = false;
            try {
                IStatusBarService statusbar = getStatusBarService();
                if (statusbar != null) {
                    statusbar.cancelPreloadRecentApps();
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "RemoteException when showing recent apps", e);
                // re-acquire status bar service next time it is needed.
                mStatusBarService = null;
            }
        }
    }

    private void toggleRecentApps() {
        mPreloadedRecentApps = false; // preloading no longer needs to be canceled
        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_RECENT_APPS);
        try {
            IStatusBarService statusbar = getStatusBarService();
            if (statusbar != null) {
                statusbar.toggleRecentApps();
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "RemoteException when showing recent apps", e);
            // re-acquire status bar service next time it is needed.
            mStatusBarService = null;
        }
    }

    /**
     * A home key -> launch home action was detected.  Take the appropriate action
     * given the situation with the keyguard.
     */
    void launchHomeFromHotKey() {
        if(SystemProperties.get("ro.ysten.province","master").equals("c60_jiangsu")){
		
		/*jiangsu cp*/
        if(SystemProperties.get("persist.sys.yst.homefree","on").equals("on")){
		    freeMemory(apkNameListSC);
        }
        }
        if(SystemProperties.get("ro.ysten.province","master").equals("c60_chongqing")){
		
		/*jiangsu cp*/
        if(SystemProperties.get("persist.sys.yst.homefree","on").equals("on")){
		    freeMemory(apkNameListCQJD);
        }
        }
        /*if (mKeyguardDelegate != null && mKeyguardDelegate.isShowingAndNotHidden()) {
            // don't launch home if keyguard showing
        } else if (!mHideLockScreen && mKeyguardDelegate.isInputRestricted()) {
            // when in keyguard restricted mode, must first verify unlock
            // before launching home
            mKeyguardDelegate.verifyUnlock(new OnKeyguardExitResult() {
                public void onKeyguardExitResult(boolean success) {
                    if (success) {
                        try {
                            ActivityManagerNative.getDefault().stopAppSwitches();
                        } catch (RemoteException e) {
                        }
                        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_HOME_KEY);
                        startDockOrHome();
                    }
                }
            });
        } else */{
            // no keyguard stuff to worry about, just launch home!
            try {
                ActivityManagerNative.getDefault().stopAppSwitches();
            } catch (RemoteException e) {
            }
            sendCloseSystemWindows(SYSTEM_DIALOG_REASON_HOME_KEY);
            startDockOrHome();
        }
    }

    private final Runnable mClearHideNavigationFlag = new Runnable() {
        @Override
        public void run() {
            synchronized (mWindowManagerFuncs.getWindowManagerLock()) {
                // Clear flags.
                mForceClearedSystemUiFlags &=
                        ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }
            mWindowManagerFuncs.reevaluateStatusBarVisibility();
        }
    };

    /**
     * Input handler used while nav bar is hidden.  Captures any touch on the screen,
     * to determine when the nav bar should be shown and prevent applications from
     * receiving those touches.
     */
    final class HideNavInputEventReceiver extends InputEventReceiver {
        public HideNavInputEventReceiver(InputChannel inputChannel, Looper looper) {
            super(inputChannel, looper);
        }

        @Override
        public void onInputEvent(InputEvent event) {
            boolean handled = false;
            try {
                if (event instanceof MotionEvent
                        && (event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0) {
                    final MotionEvent motionEvent = (MotionEvent)event;
                    if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                        // When the user taps down, we re-show the nav bar.
                        boolean changed = false;
                        synchronized (mWindowManagerFuncs.getWindowManagerLock()) {
                            // Any user activity always causes us to show the
                            // navigation controls, if they had been hidden.
                            // We also clear the low profile and only content
                            // flags so that tapping on the screen will atomically
                            // restore all currently hidden screen decorations.
                            int newVal = mResettingSystemUiFlags |
                                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                    View.SYSTEM_UI_FLAG_LOW_PROFILE |
                                    View.SYSTEM_UI_FLAG_FULLSCREEN;
                            if (mResettingSystemUiFlags != newVal) {
                                mResettingSystemUiFlags = newVal;
                                changed = true;
                            }
                            // We don't allow the system's nav bar to be hidden
                            // again for 1 second, to prevent applications from
                            // spamming us and keeping it from being shown.
                            newVal = mForceClearedSystemUiFlags |
                                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                            if (mForceClearedSystemUiFlags != newVal) {
                                mForceClearedSystemUiFlags = newVal;
                                changed = true;
                                mHandler.postDelayed(mClearHideNavigationFlag, 1000);
                            }
                        }
                        if (changed) {
                            mWindowManagerFuncs.reevaluateStatusBarVisibility();
                        }
                    }
                }
            } finally {
                finishInputEvent(event, handled);
            }
        }
    }
    final InputEventReceiver.Factory mHideNavInputEventReceiverFactory =
            new InputEventReceiver.Factory() {
        @Override
        public InputEventReceiver createInputEventReceiver(
                InputChannel inputChannel, Looper looper) {
            return new HideNavInputEventReceiver(inputChannel, looper);
        }
    };

    @Override
    public int adjustSystemUiVisibilityLw(int visibility) {
        mStatusBarController.adjustSystemUiVisibilityLw(mLastSystemUiFlags, visibility);
        mNavigationBarController.adjustSystemUiVisibilityLw(mLastSystemUiFlags, visibility);

        // Reset any bits in mForceClearingStatusBarVisibility that
        // are now clear.
        mResettingSystemUiFlags &= visibility;
        // Clear any bits in the new visibility that are currently being
        // force cleared, before reporting it.
        return visibility & ~mResettingSystemUiFlags
                & ~mForceClearedSystemUiFlags;
    }

    @Override
    public void getContentInsetHintLw(WindowManager.LayoutParams attrs, Rect contentInset) {
        final int fl = attrs.flags;
        final int systemUiVisibility = (attrs.systemUiVisibility|attrs.subtreeSystemUiVisibility);

        if ((fl & (FLAG_LAYOUT_IN_SCREEN | FLAG_LAYOUT_INSET_DECOR))
                == (FLAG_LAYOUT_IN_SCREEN | FLAG_LAYOUT_INSET_DECOR)) {
            int availRight, availBottom;
            if (canHideNavigationBar() &&
                    (systemUiVisibility & View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION) != 0) {
                availRight = mUnrestrictedScreenLeft + mUnrestrictedScreenWidth;
                availBottom = mUnrestrictedScreenTop + mUnrestrictedScreenHeight;
            } else {
                availRight = mRestrictedScreenLeft + mRestrictedScreenWidth;
                availBottom = mRestrictedScreenTop + mRestrictedScreenHeight;
            }
            if ((systemUiVisibility & View.SYSTEM_UI_FLAG_LAYOUT_STABLE) != 0) {
                if ((fl & FLAG_FULLSCREEN) != 0) {
                    contentInset.set(mStableFullscreenLeft, mStableFullscreenTop,
                            availRight - mStableFullscreenRight,
                            availBottom - mStableFullscreenBottom);
                } else {
                    contentInset.set(mStableLeft, mStableTop,
                            availRight - mStableRight, availBottom - mStableBottom);
                }
            } else if ((fl & FLAG_FULLSCREEN) != 0 || (fl & FLAG_LAYOUT_IN_OVERSCAN) != 0) {
                contentInset.setEmpty();
            } else if ((systemUiVisibility & (View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)) == 0) {
                contentInset.set(mCurLeft, mCurTop,
                        availRight - mCurRight, availBottom - mCurBottom);
            } else {
                contentInset.set(mCurLeft, mCurTop,
                        availRight - mCurRight, availBottom - mCurBottom);
            }
            return;
        }
        contentInset.setEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public void beginLayoutLw(boolean isDefaultDisplay, int displayWidth, int displayHeight,
                              int displayRotation) {
        final int overscanLeft, overscanTop, overscanRight, overscanBottom;
        if (isDefaultDisplay) {
            switch (displayRotation) {
                case Surface.ROTATION_90:
                    overscanLeft = mOverscanTop;
                    overscanTop = mOverscanRight;
                    overscanRight = mOverscanBottom;
                    overscanBottom = mOverscanLeft;
                    break;
                case Surface.ROTATION_180:
                    overscanLeft = mOverscanRight;
                    overscanTop = mOverscanBottom;
                    overscanRight = mOverscanLeft;
                    overscanBottom = mOverscanTop;
                    break;
                case Surface.ROTATION_270:
                    overscanLeft = mOverscanBottom;
                    overscanTop = mOverscanLeft;
                    overscanRight = mOverscanTop;
                    overscanBottom = mOverscanRight;
                    break;
                default:
                    overscanLeft = mOverscanLeft;
                    overscanTop = mOverscanTop;
                    overscanRight = mOverscanRight;
                    overscanBottom = mOverscanBottom;
                    break;
            }
        } else {
            overscanLeft = 0;
            overscanTop = 0;
            overscanRight = 0;
            overscanBottom = 0;
        }
        mOverscanScreenLeft = mRestrictedOverscanScreenLeft = 0;
        mOverscanScreenTop = mRestrictedOverscanScreenTop = 0;
        mOverscanScreenWidth = mRestrictedOverscanScreenWidth = displayWidth;
        mOverscanScreenHeight = mRestrictedOverscanScreenHeight = displayHeight;
        mSystemLeft = 0;
        mSystemTop = 0;
        mSystemRight = displayWidth;
        mSystemBottom = displayHeight;
        mUnrestrictedScreenLeft = overscanLeft;
        mUnrestrictedScreenTop = overscanTop;
        mUnrestrictedScreenWidth = displayWidth - overscanLeft - overscanRight;
        mUnrestrictedScreenHeight = displayHeight - overscanTop - overscanBottom;
        mRestrictedScreenLeft = mUnrestrictedScreenLeft;
        mRestrictedScreenTop = mUnrestrictedScreenTop;
        mRestrictedScreenWidth = mSystemGestures.screenWidth = mUnrestrictedScreenWidth;
        mRestrictedScreenHeight = mSystemGestures.screenHeight = mUnrestrictedScreenHeight;
        mDockLeft = mContentLeft = mStableLeft = mStableFullscreenLeft
                = mCurLeft = mUnrestrictedScreenLeft;
        mDockTop = mContentTop = mStableTop = mStableFullscreenTop
                = mCurTop = mUnrestrictedScreenTop;
        mDockRight = mContentRight = mStableRight = mStableFullscreenRight
                = mCurRight = displayWidth - overscanRight;
        mDockBottom = mContentBottom = mStableBottom = mStableFullscreenBottom
                = mCurBottom = displayHeight - overscanBottom;
        mDockLayer = 0x10000000;
        mStatusBarLayer = -1;

        // start with the current dock rect, which will be (0,0,displayWidth,displayHeight)
        final Rect pf = mTmpParentFrame;
        final Rect df = mTmpDisplayFrame;
        final Rect of = mTmpOverscanFrame;
        final Rect vf = mTmpVisibleFrame;
        final Rect dcf = mTmpDecorFrame;
        pf.left = df.left = of.left = vf.left = mDockLeft;
        pf.top = df.top = of.top = vf.top = mDockTop;
        pf.right = df.right = of.right = vf.right = mDockRight;
        pf.bottom = df.bottom = of.bottom = vf.bottom = mDockBottom;
        dcf.setEmpty();  // Decor frame N/A for system bars.

        if (isDefaultDisplay) {
            // For purposes of putting out fake window up to steal focus, we will
            // drive nav being hidden only by whether it is requested.
            final int sysui = mLastSystemUiFlags;
            boolean navVisible = (sysui & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
            boolean navTranslucent = (sysui & View.NAVIGATION_BAR_TRANSLUCENT) != 0;
            boolean immersive = (sysui & View.SYSTEM_UI_FLAG_IMMERSIVE) != 0;
            boolean immersiveSticky = (sysui & View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) != 0;
            boolean navAllowedHidden = immersive || immersiveSticky;
            navTranslucent &= !immersiveSticky;  // transient trumps translucent
            navTranslucent &= areTranslucentBarsAllowed();

            // When the navigation bar isn't visible, we put up a fake
            // input window to catch all touch events.  This way we can
            // detect when the user presses anywhere to bring back the nav
            // bar and ensure the application doesn't see the event.
            if (navVisible || navAllowedHidden) {
                if (mHideNavFakeWindow != null) {
                    mHideNavFakeWindow.dismiss();
                    mHideNavFakeWindow = null;
                }
            } else if (mHideNavFakeWindow == null) {
                mHideNavFakeWindow = mWindowManagerFuncs.addFakeWindow(
                        mHandler.getLooper(), mHideNavInputEventReceiverFactory,
                        "hidden nav", WindowManager.LayoutParams.TYPE_HIDDEN_NAV_CONSUMER, 0,
                        0, false, false, true);
            }

            // For purposes of positioning and showing the nav bar, if we have
            // decided that it can't be hidden (because of the screen aspect ratio),
            // then take that into account.
            navVisible |= !canHideNavigationBar();

            boolean updateSysUiVisibility = false;
            if (mNavigationBar != null) {
                boolean transientNavBarShowing = mNavigationBarController.isTransientShowing();
                // Force the navigation bar to its appropriate place and
                // size.  We need to do this directly, instead of relying on
                // it to bubble up from the nav bar, because this needs to
                // change atomically with screen rotations.
                mNavigationBarOnBottom = (!mNavigationBarCanMove || displayWidth < displayHeight);
                if (mNavigationBarOnBottom) {
                    // It's a system nav bar or a portrait screen; nav bar goes on bottom.
                    int top = displayHeight - overscanBottom
                            - mNavigationBarHeightForRotation[displayRotation];
                    mTmpNavigationFrame.set(0, top, displayWidth, displayHeight - overscanBottom);
                    mStableBottom = mStableFullscreenBottom = mTmpNavigationFrame.top;
                    if (transientNavBarShowing) {
                        mNavigationBarController.setBarShowingLw(true);
                    } else if (navVisible) {
                        mNavigationBarController.setBarShowingLw(true);
                        mDockBottom = mTmpNavigationFrame.top;
                        mRestrictedScreenHeight = mDockBottom - mRestrictedScreenTop;
                        mRestrictedOverscanScreenHeight = mDockBottom - mRestrictedOverscanScreenTop;
                    } else {
                        // We currently want to hide the navigation UI.
                        mNavigationBarController.setBarShowingLw(false);
                    }
                    if (navVisible && !navTranslucent && !mNavigationBar.isAnimatingLw()
                            && !mNavigationBarController.wasRecentlyTranslucent()) {
                        // If the opaque nav bar is currently requested to be visible,
                        // and not in the process of animating on or off, then
                        // we can tell the app that it is covered by it.
                        mSystemBottom = mTmpNavigationFrame.top;
                    }
                } else {
                    // Landscape screen; nav bar goes to the right.
                    int left = displayWidth - overscanRight
                            - mNavigationBarWidthForRotation[displayRotation];
                    mTmpNavigationFrame.set(left, 0, displayWidth - overscanRight, displayHeight);
                    mStableRight = mStableFullscreenRight = mTmpNavigationFrame.left;
                    if (transientNavBarShowing) {
                        mNavigationBarController.setBarShowingLw(true);
                    } else if (navVisible) {
                        mNavigationBarController.setBarShowingLw(true);
                        mDockRight = mTmpNavigationFrame.left;
                        mRestrictedScreenWidth = mDockRight - mRestrictedScreenLeft;
                        mRestrictedOverscanScreenWidth = mDockRight - mRestrictedOverscanScreenLeft;
                    } else {
                        // We currently want to hide the navigation UI.
                        mNavigationBarController.setBarShowingLw(false);
                    }
                    if (navVisible && !navTranslucent && !mNavigationBar.isAnimatingLw()
                            && !mNavigationBarController.wasRecentlyTranslucent()) {
                        // If the nav bar is currently requested to be visible,
                        // and not in the process of animating on or off, then
                        // we can tell the app that it is covered by it.
                        mSystemRight = mTmpNavigationFrame.left;
                    }
                }
                // Make sure the content and current rectangles are updated to
                // account for the restrictions from the navigation bar.
                mContentTop = mCurTop = mDockTop;
                mContentBottom = mCurBottom = mDockBottom;
                mContentLeft = mCurLeft = mDockLeft;
                mContentRight = mCurRight = mDockRight;
                mStatusBarLayer = mNavigationBar.getSurfaceLayer();
                // And compute the final frame.
                mNavigationBar.computeFrameLw(mTmpNavigationFrame, mTmpNavigationFrame,
                        mTmpNavigationFrame, mTmpNavigationFrame, mTmpNavigationFrame, dcf);
                if (DEBUG_LAYOUT) Slog.i(TAG, "mNavigationBar frame: " + mTmpNavigationFrame);
                if (mNavigationBarController.checkHiddenLw()) {
                    updateSysUiVisibility = true;
                }
            }
            if (DEBUG_LAYOUT) Slog.i(TAG, String.format("mDock rect: (%d,%d - %d,%d)",
                    mDockLeft, mDockTop, mDockRight, mDockBottom));

            // decide where the status bar goes ahead of time
            if (mStatusBar != null) {
                // apply any navigation bar insets
                pf.left = df.left = of.left = mUnrestrictedScreenLeft;
                pf.top = df.top = of.top = mUnrestrictedScreenTop;
                pf.right = df.right = of.right = mUnrestrictedScreenWidth + mUnrestrictedScreenLeft;
                pf.bottom = df.bottom = of.bottom = mUnrestrictedScreenHeight
                        + mUnrestrictedScreenTop;
                vf.left = mStableLeft;
                vf.top = mStableTop;
                vf.right = mStableRight;
                vf.bottom = mStableBottom;

                mStatusBarLayer = mStatusBar.getSurfaceLayer();

                // Let the status bar determine its size.
                mStatusBar.computeFrameLw(pf, df, vf, vf, vf, dcf);

                // For layout, the status bar is always at the top with our fixed height.
                mStableTop = mUnrestrictedScreenTop + mStatusBarHeight;

                boolean statusBarTransient = (sysui & View.STATUS_BAR_TRANSIENT) != 0;
                boolean statusBarTranslucent = (sysui & View.STATUS_BAR_TRANSLUCENT) != 0;
                statusBarTranslucent &= areTranslucentBarsAllowed();

                // If the status bar is hidden, we don't want to cause
                // windows behind it to scroll.
                if (mStatusBar.isVisibleLw() && !statusBarTransient) {
                    // Status bar may go away, so the screen area it occupies
                    // is available to apps but just covering them when the
                    // status bar is visible.
                    mDockTop = mUnrestrictedScreenTop + mStatusBarHeight;

                    mContentTop = mCurTop = mDockTop;
                    mContentBottom = mCurBottom = mDockBottom;
                    mContentLeft = mCurLeft = mDockLeft;
                    mContentRight = mCurRight = mDockRight;

                    if (DEBUG_LAYOUT) Slog.v(TAG, "Status bar: " +
                        String.format(
                            "dock=[%d,%d][%d,%d] content=[%d,%d][%d,%d] cur=[%d,%d][%d,%d]",
                            mDockLeft, mDockTop, mDockRight, mDockBottom,
                            mContentLeft, mContentTop, mContentRight, mContentBottom,
                            mCurLeft, mCurTop, mCurRight, mCurBottom));
                }
                if (mStatusBar.isVisibleLw() && !mStatusBar.isAnimatingLw()
                        && !statusBarTransient && !statusBarTranslucent
                        && !mStatusBarController.wasRecentlyTranslucent()) {
                    // If the opaque status bar is currently requested to be visible,
                    // and not in the process of animating on or off, then
                    // we can tell the app that it is covered by it.
                    mSystemTop = mUnrestrictedScreenTop + mStatusBarHeight;
                }
                if (mStatusBarController.checkHiddenLw()) {
                    updateSysUiVisibility = true;
                }
            }
            if (updateSysUiVisibility) {
                updateSystemUiVisibilityLw();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getSystemDecorLayerLw() {
        if (mStatusBar != null) return mStatusBar.getSurfaceLayer();
        if (mNavigationBar != null) return mNavigationBar.getSurfaceLayer();
        return 0;
    }

    @Override
    public void getContentRectLw(Rect r) {
        r.set(mContentLeft, mContentTop, mContentRight, mContentBottom);
    }

    void setAttachedWindowFrames(WindowState win, int fl, int adjust, WindowState attached,
            boolean insetDecors, Rect pf, Rect df, Rect of, Rect cf, Rect vf) {
        if (win.getSurfaceLayer() > mDockLayer && attached.getSurfaceLayer() < mDockLayer) {
            // Here's a special case: if this attached window is a panel that is
            // above the dock window, and the window it is attached to is below
            // the dock window, then the frames we computed for the window it is
            // attached to can not be used because the dock is effectively part
            // of the underlying window and the attached window is floating on top
            // of the whole thing.  So, we ignore the attached window and explicitly
            // compute the frames that would be appropriate without the dock.
            df.left = of.left = cf.left = vf.left = mDockLeft;
            df.top = of.top = cf.top = vf.top = mDockTop;
            df.right = of.right = cf.right = vf.right = mDockRight;
            df.bottom = of.bottom = cf.bottom = vf.bottom = mDockBottom;
        } else {
            // The effective display frame of the attached window depends on
            // whether it is taking care of insetting its content.  If not,
            // we need to use the parent's content frame so that the entire
            // window is positioned within that content.  Otherwise we can use
            // the display frame and let the attached window take care of
            // positioning its content appropriately.
            if (adjust != SOFT_INPUT_ADJUST_RESIZE) {
                cf.set(attached.getOverscanFrameLw());
            } else {
                // If the window is resizing, then we want to base the content
                // frame on our attached content frame to resize...  however,
                // things can be tricky if the attached window is NOT in resize
                // mode, in which case its content frame will be larger.
                // Ungh.  So to deal with that, make sure the content frame
                // we end up using is not covering the IM dock.
                cf.set(attached.getContentFrameLw());
                if (attached.getSurfaceLayer() < mDockLayer) {
                    if (cf.left < mContentLeft) cf.left = mContentLeft;
                    if (cf.top < mContentTop) cf.top = mContentTop;
                    if (cf.right > mContentRight) cf.right = mContentRight;
                    if (cf.bottom > mContentBottom) cf.bottom = mContentBottom;
                }
            }
            df.set(insetDecors ? attached.getDisplayFrameLw() : cf);
            of.set(insetDecors ? attached.getOverscanFrameLw() : cf);
            vf.set(attached.getVisibleFrameLw());
        }
        // The LAYOUT_IN_SCREEN flag is used to determine whether the attached
        // window should be positioned relative to its parent or the entire
        // screen.
        pf.set((fl & FLAG_LAYOUT_IN_SCREEN) == 0
                ? attached.getFrameLw() : df);
    }

    private void applyStableConstraints(int sysui, int fl, Rect r) {
        if ((sysui & View.SYSTEM_UI_FLAG_LAYOUT_STABLE) != 0) {
            // If app is requesting a stable layout, don't let the
            // content insets go below the stable values.
            if ((fl & FLAG_FULLSCREEN) != 0) {
                if (r.left < mStableFullscreenLeft) r.left = mStableFullscreenLeft;
                if (r.top < mStableFullscreenTop) r.top = mStableFullscreenTop;
                if (r.right > mStableFullscreenRight) r.right = mStableFullscreenRight;
                if (r.bottom > mStableFullscreenBottom) r.bottom = mStableFullscreenBottom;
            } else {
                if (r.left < mStableLeft) r.left = mStableLeft;
                if (r.top < mStableTop) r.top = mStableTop;
                if (r.right > mStableRight) r.right = mStableRight;
                if (r.bottom > mStableBottom) r.bottom = mStableBottom;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void layoutWindowLw(WindowState win, WindowManager.LayoutParams attrs,
            WindowState attached) {
        // we've already done the status bar
        if (win == mStatusBar || win == mNavigationBar) {
            return;
        }
        final boolean isDefaultDisplay = win.isDefaultDisplay();
        final boolean needsToOffsetInputMethodTarget = isDefaultDisplay &&
                (win == mLastInputMethodTargetWindow && mLastInputMethodWindow != null);
        if (needsToOffsetInputMethodTarget) {
            if (DEBUG_LAYOUT) Slog.i(TAG, "Offset ime target window by the last ime window state");
            offsetInputMethodWindowLw(mLastInputMethodWindow);
        }

        final int fl = attrs.flags;
        final int sim = attrs.softInputMode;
        final int sysUiFl = win.getSystemUiVisibility();

        final Rect pf = mTmpParentFrame;
        final Rect df = mTmpDisplayFrame;
        final Rect of = mTmpOverscanFrame;
        final Rect cf = mTmpContentFrame;
        final Rect vf = mTmpVisibleFrame;
        final Rect dcf = mTmpDecorFrame;
        dcf.setEmpty();

        final boolean hasNavBar = (isDefaultDisplay && mHasNavigationBar
                && mNavigationBar != null && mNavigationBar.isVisibleLw());

        final int adjust = sim & SOFT_INPUT_MASK_ADJUST;

        if (!isDefaultDisplay) {
            if (attached != null) {
                // If this window is attached to another, our display
                // frame is the same as the one we are attached to.
                setAttachedWindowFrames(win, fl, adjust, attached, true, pf, df, of, cf, vf);
            } else {
                // Give the window full screen.
                pf.left = df.left = of.left = cf.left = mOverscanScreenLeft;
                pf.top = df.top = of.top = cf.top = mOverscanScreenTop;
                pf.right = df.right = of.right = cf.right
                        = mOverscanScreenLeft + mOverscanScreenWidth;
                pf.bottom = df.bottom = of.bottom = cf.bottom
                        = mOverscanScreenTop + mOverscanScreenHeight;
            }
        } else  if (attrs.type == TYPE_INPUT_METHOD) {
            pf.left = df.left = of.left = cf.left = vf.left = mDockLeft;
            pf.top = df.top = of.top = cf.top = vf.top = mDockTop;
            pf.right = df.right = of.right = cf.right = vf.right = mDockRight;
            // IM dock windows layout below the nav bar...
            pf.bottom = df.bottom = of.bottom = mUnrestrictedScreenTop + mUnrestrictedScreenHeight;
            // ...with content insets above the nav bar
            cf.bottom = vf.bottom = mStableBottom;
            // IM dock windows always go to the bottom of the screen.
            attrs.gravity = Gravity.BOTTOM;
            mDockLayer = win.getSurfaceLayer();
        } else {

            // Default policy decor for the default display
            dcf.left = mSystemLeft;
            dcf.top = mSystemTop;
            dcf.right = mSystemRight;
            dcf.bottom = mSystemBottom;
            final boolean inheritTranslucentDecor = (attrs.privateFlags
                    & WindowManager.LayoutParams.PRIVATE_FLAG_INHERIT_TRANSLUCENT_DECOR) != 0;
            final boolean isAppWindow =
                    attrs.type >= WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW &&
                    attrs.type <= WindowManager.LayoutParams.LAST_APPLICATION_WINDOW;
            final boolean topAtRest =
                    win == mTopFullscreenOpaqueWindowState && !win.isAnimatingLw();
            if (isAppWindow && !inheritTranslucentDecor && !topAtRest) {
                if ((sysUiFl & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0
                        && (fl & WindowManager.LayoutParams.FLAG_FULLSCREEN) == 0
                        && (fl & WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS) == 0) {
                    // Ensure policy decor includes status bar
                    dcf.top = mStableTop;
                }
                if ((fl & WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION) == 0
                        && (sysUiFl & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                    // Ensure policy decor includes navigation bar
                    dcf.bottom = mStableBottom;
                    dcf.right = mStableRight;
                }
            }

            if ((fl & (FLAG_LAYOUT_IN_SCREEN | FLAG_LAYOUT_INSET_DECOR))
                    == (FLAG_LAYOUT_IN_SCREEN | FLAG_LAYOUT_INSET_DECOR)) {
                if (DEBUG_LAYOUT) Slog.v(TAG, "layoutWindowLw(" + attrs.getTitle() 
                            + "): IN_SCREEN, INSET_DECOR");
                // This is the case for a normal activity window: we want it
                // to cover all of the screen space, and it can take care of
                // moving its contents to account for screen decorations that
                // intrude into that space.
                if (attached != null) {
                    // If this window is attached to another, our display
                    // frame is the same as the one we are attached to.
                    setAttachedWindowFrames(win, fl, adjust, attached, true, pf, df, of, cf, vf);
                } else {
                    if (attrs.type == TYPE_STATUS_BAR_PANEL
                            || attrs.type == TYPE_STATUS_BAR_SUB_PANEL) {
                        // Status bar panels are the only windows who can go on top of
                        // the status bar.  They are protected by the STATUS_BAR_SERVICE
                        // permission, so they have the same privileges as the status
                        // bar itself.
                        //
                        // However, they should still dodge the navigation bar if it exists.

                        pf.left = df.left = of.left = hasNavBar
                                ? mDockLeft : mUnrestrictedScreenLeft;
                        pf.top = df.top = of.top = mUnrestrictedScreenTop;
                        pf.right = df.right = of.right = hasNavBar
                                ? mRestrictedScreenLeft+mRestrictedScreenWidth
                                : mUnrestrictedScreenLeft + mUnrestrictedScreenWidth;
                        pf.bottom = df.bottom = of.bottom = hasNavBar
                                ? mRestrictedScreenTop+mRestrictedScreenHeight
                                : mUnrestrictedScreenTop + mUnrestrictedScreenHeight;

                        if (DEBUG_LAYOUT) Slog.v(TAG, String.format(
                                        "Laying out status bar window: (%d,%d - %d,%d)",
                                        pf.left, pf.top, pf.right, pf.bottom));
                    } else if ((fl & FLAG_LAYOUT_IN_OVERSCAN) != 0
                            && attrs.type >= WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW
                            && attrs.type <= WindowManager.LayoutParams.LAST_SUB_WINDOW) {
                        // Asking to layout into the overscan region, so give it that pure
                        // unrestricted area.
                        pf.left = df.left = of.left = mOverscanScreenLeft;
                        pf.top = df.top = of.top = mOverscanScreenTop;
                        pf.right = df.right = of.right = mOverscanScreenLeft + mOverscanScreenWidth;
                        pf.bottom = df.bottom = of.bottom = mOverscanScreenTop
                                + mOverscanScreenHeight;
                    } else if (canHideNavigationBar()
                            && (sysUiFl & View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION) != 0
                            && (attrs.type == WindowManager.LayoutParams.TYPE_KEYGUARD || (
                                attrs.type >= WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW
                             && attrs.type <= WindowManager.LayoutParams.LAST_SUB_WINDOW))) {
                        // Asking for layout as if the nav bar is hidden, lets the
                        // application extend into the unrestricted overscan screen area.  We
                        // only do this for application windows to ensure no window that
                        // can be above the nav bar can do this.
                        pf.left = df.left = mOverscanScreenLeft;
                        pf.top = df.top = mOverscanScreenTop;
                        pf.right = df.right = mOverscanScreenLeft + mOverscanScreenWidth;
                        pf.bottom = df.bottom = mOverscanScreenTop + mOverscanScreenHeight;
                        // We need to tell the app about where the frame inside the overscan
                        // is, so it can inset its content by that amount -- it didn't ask
                        // to actually extend itself into the overscan region.
                        of.left = mUnrestrictedScreenLeft;
                        of.top = mUnrestrictedScreenTop;
                        of.right = mUnrestrictedScreenLeft + mUnrestrictedScreenWidth;
                        of.bottom = mUnrestrictedScreenTop + mUnrestrictedScreenHeight;
                    } else {
                        pf.left = df.left = mRestrictedOverscanScreenLeft;
                        pf.top = df.top = mRestrictedOverscanScreenTop;
                        pf.right = df.right = mRestrictedOverscanScreenLeft
                                + mRestrictedOverscanScreenWidth;
                        pf.bottom = df.bottom = mRestrictedOverscanScreenTop
                                + mRestrictedOverscanScreenHeight;
                        // We need to tell the app about where the frame inside the overscan
                        // is, so it can inset its content by that amount -- it didn't ask
                        // to actually extend itself into the overscan region.
                        of.left = mUnrestrictedScreenLeft;
                        of.top = mUnrestrictedScreenTop;
                        of.right = mUnrestrictedScreenLeft + mUnrestrictedScreenWidth;
                        of.bottom = mUnrestrictedScreenTop + mUnrestrictedScreenHeight;
                    }

                    if ((fl & FLAG_FULLSCREEN) == 0) {
                        if (adjust != SOFT_INPUT_ADJUST_RESIZE) {
                            cf.left = mDockLeft;
                            cf.top = mDockTop;
                            cf.right = mDockRight;
                            cf.bottom = mDockBottom;
                        } else {
                            cf.left = mContentLeft;
                            cf.top = mContentTop;
                            cf.right = mContentRight;
                            cf.bottom = mContentBottom;
                        }
                    } else {
                        // Full screen windows are always given a layout that is as if the
                        // status bar and other transient decors are gone.  This is to avoid
                        // bad states when moving from a window that is not hding the
                        // status bar to one that is.
                        cf.left = mRestrictedScreenLeft;
                        cf.top = mRestrictedScreenTop;
                        cf.right = mRestrictedScreenLeft + mRestrictedScreenWidth;
                        cf.bottom = mRestrictedScreenTop + mRestrictedScreenHeight;
                    }
                    applyStableConstraints(sysUiFl, fl, cf);
                    if (adjust != SOFT_INPUT_ADJUST_NOTHING) {
                        vf.left = mCurLeft;
                        vf.top = mCurTop;
                        vf.right = mCurRight;
                        vf.bottom = mCurBottom;
                    } else {
                        vf.set(cf);
                    }
                }
            } else if ((fl & FLAG_LAYOUT_IN_SCREEN) != 0 || (sysUiFl
                    & (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)) != 0) {
                if (DEBUG_LAYOUT) Slog.v(TAG, "layoutWindowLw(" + attrs.getTitle() +
                        "): IN_SCREEN");
                // A window that has requested to fill the entire screen just
                // gets everything, period.
                if (attrs.type == TYPE_STATUS_BAR_PANEL
                        || attrs.type == TYPE_STATUS_BAR_SUB_PANEL) {
                    pf.left = df.left = of.left = cf.left = hasNavBar
                            ? mDockLeft : mUnrestrictedScreenLeft;
                    pf.top = df.top = of.top = cf.top = mUnrestrictedScreenTop;
                    pf.right = df.right = of.right = cf.right = hasNavBar
                                        ? mRestrictedScreenLeft+mRestrictedScreenWidth
                                        : mUnrestrictedScreenLeft + mUnrestrictedScreenWidth;
                    pf.bottom = df.bottom = of.bottom = cf.bottom = hasNavBar
                                          ? mRestrictedScreenTop+mRestrictedScreenHeight
                                          : mUnrestrictedScreenTop + mUnrestrictedScreenHeight;
                    if (DEBUG_LAYOUT) Slog.v(TAG, String.format(
                                    "Laying out IN_SCREEN status bar window: (%d,%d - %d,%d)",
                                    pf.left, pf.top, pf.right, pf.bottom));
                } else if (attrs.type == TYPE_NAVIGATION_BAR
                        || attrs.type == TYPE_NAVIGATION_BAR_PANEL) {
                    // The navigation bar has Real Ultimate Power.
                    pf.left = df.left = of.left = mUnrestrictedScreenLeft;
                    pf.top = df.top = of.top = mUnrestrictedScreenTop;
                    pf.right = df.right = of.right = mUnrestrictedScreenLeft
                            + mUnrestrictedScreenWidth;
                    pf.bottom = df.bottom = of.bottom = mUnrestrictedScreenTop
                            + mUnrestrictedScreenHeight;
                    if (DEBUG_LAYOUT) Slog.v(TAG, String.format(
                                    "Laying out navigation bar window: (%d,%d - %d,%d)",
                                    pf.left, pf.top, pf.right, pf.bottom));
                } else if ((attrs.type == TYPE_SECURE_SYSTEM_OVERLAY
                                || attrs.type == TYPE_BOOT_PROGRESS)
                        && ((fl & FLAG_FULLSCREEN) != 0)) {
                    // Fullscreen secure system overlays get what they ask for.
                    pf.left = df.left = of.left = cf.left = mOverscanScreenLeft;
                    pf.top = df.top = of.top = cf.top = mOverscanScreenTop;
                    pf.right = df.right = of.right = cf.right = mOverscanScreenLeft
                            + mOverscanScreenWidth;
                    pf.bottom = df.bottom = of.bottom = cf.bottom = mOverscanScreenTop
                            + mOverscanScreenHeight;
                } else if (attrs.type == TYPE_BOOT_PROGRESS
                        || attrs.type == TYPE_UNIVERSE_BACKGROUND) {
                    // Boot progress screen always covers entire display.
                    pf.left = df.left = of.left = cf.left = mOverscanScreenLeft;
                    pf.top = df.top = of.top = cf.top = mOverscanScreenTop;
                    pf.right = df.right = of.right = cf.right = mOverscanScreenLeft
                            + mOverscanScreenWidth;
                    pf.bottom = df.bottom = of.bottom = cf.bottom = mOverscanScreenTop
                            + mOverscanScreenHeight;
                } else if (attrs.type == TYPE_WALLPAPER) {
                    // The wallpaper also has Real Ultimate Power.
                    pf.left = df.left = of.left = cf.left = mUnrestrictedScreenLeft;
                    pf.top = df.top = of.top = cf.top = mUnrestrictedScreenTop;
                    pf.right = df.right = of.right = cf.right
                            = mUnrestrictedScreenLeft + mUnrestrictedScreenWidth;
                    pf.bottom = df.bottom = of.bottom = cf.bottom
                            = mUnrestrictedScreenTop + mUnrestrictedScreenHeight;
                } else if ((fl & FLAG_LAYOUT_IN_OVERSCAN) != 0
                        && attrs.type >= WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW
                        && attrs.type <= WindowManager.LayoutParams.LAST_SUB_WINDOW) {
                    // Asking to layout into the overscan region, so give it that pure
                    // unrestricted area.
                    pf.left = df.left = of.left = cf.left = mOverscanScreenLeft;
                    pf.top = df.top = of.top = cf.top = mOverscanScreenTop;
                    pf.right = df.right = of.right = cf.right
                            = mOverscanScreenLeft + mOverscanScreenWidth;
                    pf.bottom = df.bottom = of.bottom = cf.bottom
                            = mOverscanScreenTop + mOverscanScreenHeight;
                } else if (canHideNavigationBar()
                        && (sysUiFl & View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION) != 0
                        && (attrs.type == TYPE_TOAST
                            || (attrs.type >= WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW
                            && attrs.type <= WindowManager.LayoutParams.LAST_SUB_WINDOW))) {
                    // Asking for layout as if the nav bar is hidden, lets the
                    // application extend into the unrestricted screen area.  We
                    // only do this for application windows (or toasts) to ensure no window that
                    // can be above the nav bar can do this.
                    // XXX This assumes that an app asking for this will also
                    // ask for layout in only content.  We can't currently figure out
                    // what the screen would be if only laying out to hide the nav bar.
                    pf.left = df.left = of.left = cf.left = mUnrestrictedScreenLeft;
                    pf.top = df.top = of.top = cf.top = mUnrestrictedScreenTop;
                    pf.right = df.right = of.right = cf.right = mUnrestrictedScreenLeft
                            + mUnrestrictedScreenWidth;
                    pf.bottom = df.bottom = of.bottom = cf.bottom = mUnrestrictedScreenTop
                            + mUnrestrictedScreenHeight;
                } else {
                    pf.left = df.left = of.left = cf.left = mRestrictedScreenLeft;
                    pf.top = df.top = of.top = cf.top = mRestrictedScreenTop;
                    pf.right = df.right = of.right = cf.right = mRestrictedScreenLeft
                            + mRestrictedScreenWidth;
                    pf.bottom = df.bottom = of.bottom = cf.bottom = mRestrictedScreenTop
                            + mRestrictedScreenHeight;
                }

                applyStableConstraints(sysUiFl, fl, cf);

                if (adjust != SOFT_INPUT_ADJUST_NOTHING) {
                    vf.left = mCurLeft;
                    vf.top = mCurTop;
                    vf.right = mCurRight;
                    vf.bottom = mCurBottom;
                } else {
                    vf.set(cf);
                }
            } else if (attached != null) {
                if (DEBUG_LAYOUT) Slog.v(TAG, "layoutWindowLw(" + attrs.getTitle() +
                        "): attached to " + attached);
                // A child window should be placed inside of the same visible
                // frame that its parent had.
                setAttachedWindowFrames(win, fl, adjust, attached, false, pf, df, of, cf, vf);
            } else {
                if (DEBUG_LAYOUT) Slog.v(TAG, "layoutWindowLw(" + attrs.getTitle() +
                        "): normal window");
                // Otherwise, a normal window must be placed inside the content
                // of all screen decorations.
                if (attrs.type == TYPE_STATUS_BAR_PANEL) {
                    // Status bar panels are the only windows who can go on top of
                    // the status bar.  They are protected by the STATUS_BAR_SERVICE
                    // permission, so they have the same privileges as the status
                    // bar itself.
                    pf.left = df.left = of.left = cf.left = mRestrictedScreenLeft;
                    pf.top = df.top = of.top = cf.top = mRestrictedScreenTop;
                    pf.right = df.right = of.right = cf.right = mRestrictedScreenLeft
                            + mRestrictedScreenWidth;
                    pf.bottom = df.bottom = of.bottom = cf.bottom = mRestrictedScreenTop
                            + mRestrictedScreenHeight;
                } else if (attrs.type == TYPE_TOAST || attrs.type == TYPE_SYSTEM_ALERT) {
                    // Toasts are stable to interim decor changes.
                    pf.left = df.left = of.left = cf.left = mStableLeft;
                    pf.top = df.top = of.top = cf.top = mStableTop;
                    pf.right = df.right = of.right = cf.right = mStableRight;
                    pf.bottom = df.bottom = of.bottom = cf.bottom = mStableBottom;
                } else {
                    pf.left = mContentLeft;
                    pf.top = mContentTop;
                    pf.right = mContentRight;
                    pf.bottom = mContentBottom;
                    if (adjust != SOFT_INPUT_ADJUST_RESIZE) {
                        df.left = of.left = cf.left = mDockLeft;
                        df.top = of.top = cf.top = mDockTop;
                        df.right = of.right = cf.right = mDockRight;
                        df.bottom = of.bottom = cf.bottom = mDockBottom;
                    } else {
                        df.left = of.left = cf.left = mContentLeft;
                        df.top = of.top = cf.top = mContentTop;
                        df.right = of.right = cf.right = mContentRight;
                        df.bottom = of.bottom = cf.bottom = mContentBottom;
                    }
                    if (adjust != SOFT_INPUT_ADJUST_NOTHING) {
                        vf.left = mCurLeft;
                        vf.top = mCurTop;
                        vf.right = mCurRight;
                        vf.bottom = mCurBottom;
                    } else {
                        vf.set(cf);
                    }
                }
            }
        }

        // TYPE_SYSTEM_ERROR is above the NavigationBar so it can't be allowed to extend over it.
        if ((fl & FLAG_LAYOUT_NO_LIMITS) != 0 && attrs.type != TYPE_SYSTEM_ERROR) {
            df.left = df.top = of.left = of.top = cf.left = cf.top = vf.left = vf.top = -10000;
            df.right = df.bottom = of.right = of.bottom = cf.right = cf.bottom
                    = vf.right = vf.bottom = 10000;
        }

        if (DEBUG_LAYOUT) Slog.v(TAG, "Compute frame " + attrs.getTitle()
                + ": sim=#" + Integer.toHexString(sim)
                + " attach=" + attached + " type=" + attrs.type 
                + String.format(" flags=0x%08x", fl)
                + " pf=" + pf.toShortString() + " df=" + df.toShortString()
                + " of=" + of.toShortString()
                + " cf=" + cf.toShortString() + " vf=" + vf.toShortString()
                + " dcf=" + dcf.toShortString());

        win.computeFrameLw(pf, df, of, cf, vf, dcf);

        // Dock windows carve out the bottom of the screen, so normal windows
        // can't appear underneath them.
        if (attrs.type == TYPE_INPUT_METHOD && win.isVisibleOrBehindKeyguardLw()
                && !win.getGivenInsetsPendingLw()) {
            setLastInputMethodWindowLw(null, null);
            offsetInputMethodWindowLw(win);
        }
    }

    private void offsetInputMethodWindowLw(WindowState win) {
        int top = win.getContentFrameLw().top;
        top += win.getGivenContentInsetsLw().top;
        if (mContentBottom > top) {
            mContentBottom = top;
        }
        top = win.getVisibleFrameLw().top;
        top += win.getGivenVisibleInsetsLw().top;
        if (mCurBottom > top) {
            mCurBottom = top;
        }
        if (DEBUG_LAYOUT) Slog.v(TAG, "Input method: mDockBottom="
                + mDockBottom + " mContentBottom="
                + mContentBottom + " mCurBottom=" + mCurBottom);
    }

    /** {@inheritDoc} */
    @Override
    public void finishLayoutLw() {
        return;
    }

    /** {@inheritDoc} */
    @Override
    public void beginPostLayoutPolicyLw(int displayWidth, int displayHeight) {
        mTopFullscreenOpaqueWindowState = null;
        mAppsToBeHidden.clear();
        mForceStatusBar = false;
        mForceStatusBarFromKeyguard = false;
        mForcingShowNavBar = false;
        mForcingShowNavBarLayer = -1;
        
        mHideLockScreen = false;
        mAllowLockscreenWhenOn = false;
        mDismissKeyguard = DISMISS_KEYGUARD_NONE;
        mShowingLockscreen = false;
        mShowingDream = false;
    }

    /** {@inheritDoc} */
    @Override
    public void applyPostLayoutPolicyLw(WindowState win,
                                WindowManager.LayoutParams attrs) {
        if (DEBUG_LAYOUT) Slog.i(TAG, "Win " + win + ": isVisibleOrBehindKeyguardLw="
                + win.isVisibleOrBehindKeyguardLw());
        if (mTopFullscreenOpaqueWindowState == null && (win.getAttrs().privateFlags
                &WindowManager.LayoutParams.PRIVATE_FLAG_FORCE_SHOW_NAV_BAR) != 0
                || (win.isVisibleLw() && attrs.type == TYPE_INPUT_METHOD)) {
            if (mForcingShowNavBarLayer < 0) {
                mForcingShowNavBar = true;
                mForcingShowNavBarLayer = win.getSurfaceLayer();
            }
        }
        if (mTopFullscreenOpaqueWindowState == null &&
                win.isVisibleOrBehindKeyguardLw() && !win.isGoneForLayoutLw()) {
            if ((attrs.flags & FLAG_FORCE_NOT_FULLSCREEN) != 0) {
                if (attrs.type == TYPE_KEYGUARD) {
                    mForceStatusBarFromKeyguard = true;
                } else {
                    mForceStatusBar = true;
                }
            }
            if (attrs.type == TYPE_KEYGUARD) {
                mShowingLockscreen = true;
            }
            boolean appWindow = attrs.type >= FIRST_APPLICATION_WINDOW
                    && attrs.type <= LAST_APPLICATION_WINDOW;
            if (attrs.type == TYPE_DREAM) {
                // If the lockscreen was showing when the dream started then wait
                // for the dream to draw before hiding the lockscreen.
                if (!mDreamingLockscreen
                        || (win.isVisibleLw() && win.hasDrawnLw())) {
                    mShowingDream = true;
                    appWindow = true;
                }
            }

            final boolean showWhenLocked = (attrs.flags & FLAG_SHOW_WHEN_LOCKED) != 0;
            if (appWindow) {
                if (showWhenLocked) {
                    mAppsToBeHidden.remove(win.getAppToken());
                } else {
                    mAppsToBeHidden.add(win.getAppToken());
                }
                if (attrs.x == 0 && attrs.y == 0
                        && attrs.width == WindowManager.LayoutParams.MATCH_PARENT
                        && attrs.height == WindowManager.LayoutParams.MATCH_PARENT) {
                    if (DEBUG_LAYOUT) Slog.v(TAG, "Fullscreen window: " + win);
                    mTopFullscreenOpaqueWindowState = win;
                    if (showWhenLocked && mAppsToBeHidden.isEmpty()) {
                        if (DEBUG_LAYOUT) Slog.v(TAG, "Setting mHideLockScreen to true by win " + win);
                        mHideLockScreen = true;
                        mForceStatusBarFromKeyguard = false;
                    }
                    if ((attrs.flags & FLAG_DISMISS_KEYGUARD) != 0
                            && mDismissKeyguard == DISMISS_KEYGUARD_NONE) {
                        if (DEBUG_LAYOUT) Slog.v(TAG, "Setting mDismissKeyguard true by win " + win);
                        mDismissKeyguard = mWinDismissingKeyguard == win ?
                                DISMISS_KEYGUARD_CONTINUE : DISMISS_KEYGUARD_START;
                        mWinDismissingKeyguard = win;
                        mForceStatusBarFromKeyguard = mShowingLockscreen && isKeyguardSecure();
                    }
                    if ((attrs.flags & FLAG_ALLOW_LOCK_WHILE_SCREEN_ON) != 0) {
                        mAllowLockscreenWhenOn = true;
                    }
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public int finishPostLayoutPolicyLw() {
        int changes = 0;
        boolean topIsFullscreen = false;

        final WindowManager.LayoutParams lp = (mTopFullscreenOpaqueWindowState != null)
                ? mTopFullscreenOpaqueWindowState.getAttrs()
                : null;

        // If we are not currently showing a dream then remember the current
        // lockscreen state.  We will use this to determine whether the dream
        // started while the lockscreen was showing and remember this state
        // while the dream is showing.
        if (!mShowingDream) {
            mDreamingLockscreen = mShowingLockscreen;
        }

        if (mStatusBar != null) {
            if (DEBUG_LAYOUT) Slog.i(TAG, "force=" + mForceStatusBar
                    + " forcefkg=" + mForceStatusBarFromKeyguard
                    + " top=" + mTopFullscreenOpaqueWindowState);
            if (mForceStatusBar || mForceStatusBarFromKeyguard) {
                if (DEBUG_LAYOUT) Slog.v(TAG, "Showing status bar: forced");
                if (mStatusBarController.setBarShowingLw(true)) {
                    changes |= FINISH_LAYOUT_REDO_LAYOUT;
                }
                // Maintain fullscreen layout until incoming animation is complete.
                topIsFullscreen = mTopIsFullscreen && mStatusBar.isAnimatingLw();
                // Transient status bar on the lockscreen is not allowed
                if (mForceStatusBarFromKeyguard && mStatusBarController.isTransientShowing()) {
                    mStatusBarController.updateVisibilityLw(false /*transientAllowed*/,
                            mLastSystemUiFlags, mLastSystemUiFlags);
                }
            } else if (mTopFullscreenOpaqueWindowState != null) {
                if (localLOGV) {
                    Slog.d(TAG, "frame: " + mTopFullscreenOpaqueWindowState.getFrameLw()
                            + " shown frame: " + mTopFullscreenOpaqueWindowState.getShownFrameLw());
                    Slog.d(TAG, "attr: " + mTopFullscreenOpaqueWindowState.getAttrs()
                            + " lp.flags=0x" + Integer.toHexString(lp.flags));
                }
                topIsFullscreen = (lp.flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0
                        || (mLastSystemUiFlags & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0;
                // The subtle difference between the window for mTopFullscreenOpaqueWindowState
                // and mTopIsFullscreen is that that mTopIsFullscreen is set only if the window
                // has the FLAG_FULLSCREEN set.  Not sure if there is another way that to be the
                // case though.
                if (mStatusBarController.isTransientShowing()) {
                    if (mStatusBarController.setBarShowingLw(true)) {
                        changes |= FINISH_LAYOUT_REDO_LAYOUT;
                    }
                } else if (topIsFullscreen) {
                    if (DEBUG_LAYOUT) Slog.v(TAG, "** HIDING status bar");
                    if (mStatusBarController.setBarShowingLw(false)) {
                        changes |= FINISH_LAYOUT_REDO_LAYOUT;
                    } else {
                        if (DEBUG_LAYOUT) Slog.v(TAG, "Status bar already hiding");
                    }
                } else {
                    if (DEBUG_LAYOUT) Slog.v(TAG, "** SHOWING status bar: top is not fullscreen");
                    if (mStatusBarController.setBarShowingLw(true)) {
                        changes |= FINISH_LAYOUT_REDO_LAYOUT;
                    }
                }
            }
        }

        if (mTopIsFullscreen != topIsFullscreen) {
            if (!topIsFullscreen) {
                // Force another layout when status bar becomes fully shown.
                changes |= FINISH_LAYOUT_REDO_LAYOUT;
            }
            mTopIsFullscreen = topIsFullscreen;
        }

        // Hide the key guard if a visible window explicitly specifies that it wants to be
        // displayed when the screen is locked.
        if (mKeyguard != null) {
            if (localLOGV) Slog.v(TAG, "finishPostLayoutPolicyLw: mHideKeyguard="
                    + mHideLockScreen);
            if (mDismissKeyguard != DISMISS_KEYGUARD_NONE && !isKeyguardSecure()) {
                if (mKeyguard.hideLw(true)) {
                    changes |= FINISH_LAYOUT_REDO_LAYOUT
                            | FINISH_LAYOUT_REDO_CONFIG
                            | FINISH_LAYOUT_REDO_WALLPAPER;
                }
                if (mKeyguardDelegate.isShowing()) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mKeyguardDelegate.keyguardDone(false, false);
                        }
                    });
                }
            } else if (mHideLockScreen) {
                if (mKeyguard.hideLw(true)) {
                    changes |= FINISH_LAYOUT_REDO_LAYOUT
                            | FINISH_LAYOUT_REDO_CONFIG
                            | FINISH_LAYOUT_REDO_WALLPAPER;
                }
                if (!mShowingDream) {
                    mKeyguardDelegate.setHidden(true);
                }
            } else if (mDismissKeyguard != DISMISS_KEYGUARD_NONE) {
                // This is the case of keyguard isSecure() and not mHideLockScreen.
                if (mDismissKeyguard == DISMISS_KEYGUARD_START) {
                    // Only launch the next keyguard unlock window once per window.
                    if (mKeyguard.showLw(true)) {
                        changes |= FINISH_LAYOUT_REDO_LAYOUT
                                | FINISH_LAYOUT_REDO_CONFIG
                                | FINISH_LAYOUT_REDO_WALLPAPER;
                    }
                    mKeyguardDelegate.setHidden(false);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mKeyguardDelegate.dismiss();
                        }
                    });
                }
            } else {
                mWinDismissingKeyguard = null;
                if (mKeyguard.showLw(true)) {
                    changes |= FINISH_LAYOUT_REDO_LAYOUT
                            | FINISH_LAYOUT_REDO_CONFIG
                            | FINISH_LAYOUT_REDO_WALLPAPER;
                }
                mKeyguardDelegate.setHidden(false);
            }
        }

        if ((updateSystemUiVisibilityLw()&SYSTEM_UI_CHANGING_LAYOUT) != 0) {
            // If the navigation bar has been hidden or shown, we need to do another
            // layout pass to update that window.
            changes |= FINISH_LAYOUT_REDO_LAYOUT;
        }

        // update since mAllowLockscreenWhenOn might have changed
        updateLockScreenTimeout();
        return changes;
    }

    public boolean allowAppAnimationsLw() {
        if (mKeyguard != null && mKeyguard.isVisibleLw() && !mKeyguard.isAnimatingLw()
                || mShowingDream) {
            // If keyguard or dreams is currently visible, no reason to animate behind it.
            return false;
        }
        return true;
    }

    public int focusChangedLw(WindowState lastFocus, WindowState newFocus) {
        mFocusedWindow = newFocus;
        if ((updateSystemUiVisibilityLw()&SYSTEM_UI_CHANGING_LAYOUT) != 0) {
            // If the navigation bar has been hidden or shown, we need to do another
            // layout pass to update that window.
            return FINISH_LAYOUT_REDO_LAYOUT;
        }
        return 0;
    }

    /** {@inheritDoc} */
    public void notifyLidSwitchChanged(long whenNanos, boolean lidOpen) {
        // do nothing if headless
        if (mHeadless) return;

        // lid changed state
        final int newLidState = lidOpen ? LID_OPEN : LID_CLOSED;
        if (newLidState == mLidState) {
            return;
        }

        mLidState = newLidState;
        applyLidSwitchState();
        updateRotation(true);

        if (lidOpen) {
            mPowerManager.wakeUp(SystemClock.uptimeMillis());
        } else if (!mLidControlsSleep) {
            mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        }
    }

    void setHdmiPlugged(boolean plugged) {
        if (mHdmiPlugged != plugged) {
            mHdmiPlugged = plugged;
            updateRotation(true, true);
            Intent intent = new Intent(ACTION_HDMI_PLUGGED);
            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
            intent.putExtra(EXTRA_HDMI_PLUGGED_STATE, plugged);
            mContext.sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    void initializeHdmiState() {
        boolean plugged = false;
        // watch for HDMI plug messages if the hdmi switch exists
        if (new File("/sys/devices/virtual/switch/hdmi/state").exists()) {
            mHDMIObserver.startObserving("DEVPATH=/devices/virtual/switch/hdmi");

            final String filename = "/sys/class/switch/hdmi/state";
            FileReader reader = null;
            try {
                reader = new FileReader(filename);
                char[] buf = new char[15];
                int n = reader.read(buf);
                if (n > 1) {
                    plugged = 0 != Integer.parseInt(new String(buf, 0, n-1));
                }
            } catch (IOException ex) {
                Slog.w(TAG, "Couldn't read hdmi state from " + filename + ": " + ex);
            } catch (NumberFormatException ex) {
                Slog.w(TAG, "Couldn't read hdmi state from " + filename + ": " + ex);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ex) {
                    }
                }
            }
        }
        // This dance forces the code in setHdmiPlugged to run.
        // Always do this so the sticky intent is stuck (to false) if there is no hdmi.
        mHdmiPlugged = !plugged;
        setHdmiPlugged(!mHdmiPlugged);
    }

    /**
     * @return Whether music is being played right now "locally" (e.g. on the device's speakers
     *    or wired headphones) or "remotely" (e.g. on a device using the Cast protocol and
     *    controlled by this device, or through remote submix).
     */
    boolean isMusicActive() {
        final AudioManager am = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) {
            Log.w(TAG, "isMusicActive: couldn't get AudioManager reference");
            return false;
        }
        return am.isLocalOrRemoteMusicActive();
    }

    /**
     * Tell the audio service to adjust the volume appropriate to the event.
     * @param keycode
     */
    void handleVolumeKey(int stream, int keycode) {
        IAudioService audioService = getAudioService();
        if (audioService == null) {
            return;
        }
        try {
            // when audio is playing locally, we shouldn't have to hold a wake lock
            // during the call, but we do it as a precaution for the rare possibility
            // that the music stops right before we call this.
            // Otherwise we might also be in a remote playback case.
            // TODO: Actually handle MUTE.
            mBroadcastWakeLock.acquire();
            if (stream == AudioSystem.STREAM_MUSIC) {
                audioService.adjustLocalOrRemoteStreamVolume(stream,
                        keycode == KeyEvent.KEYCODE_VOLUME_UP
                                ? AudioManager.ADJUST_RAISE
                                : AudioManager.ADJUST_LOWER,
                        mContext.getOpPackageName());
            } else {
                audioService.adjustStreamVolume(stream,
                        keycode == KeyEvent.KEYCODE_VOLUME_UP
                                ? AudioManager.ADJUST_RAISE
                                : AudioManager.ADJUST_LOWER,
                        0,
                        mContext.getOpPackageName());
            }
        } catch (RemoteException e) {
            Log.w(TAG, "IAudioService.adjust*StreamVolume() threw RemoteException " + e);
        } finally {
            mBroadcastWakeLock.release();
        }
    }

    final Object mScreenshotLock = new Object();
    ServiceConnection mScreenshotConnection = null;

    final Runnable mScreenshotTimeout = new Runnable() {
        @Override public void run() {
            synchronized (mScreenshotLock) {
                if (mScreenshotConnection != null) {
                    mContext.unbindService(mScreenshotConnection);
                    mScreenshotConnection = null;
                }
            }
        }
    };

    // Assume this is called from the Handler thread.
    private void takeScreenshot() {
        synchronized (mScreenshotLock) {
            if (mScreenshotConnection != null) {
                return;
            }
            ComponentName cn = new ComponentName("com.android.systemui",
                    "com.android.systemui.screenshot.TakeScreenshotService");
            Intent intent = new Intent();
            intent.setComponent(cn);
            ServiceConnection conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    synchronized (mScreenshotLock) {
                        if (mScreenshotConnection != this) {
                            return;
                        }
                        Messenger messenger = new Messenger(service);
                        Message msg = Message.obtain(null, 1);
                        final ServiceConnection myConn = this;
                        Handler h = new Handler(mHandler.getLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                synchronized (mScreenshotLock) {
                                    if (mScreenshotConnection == myConn) {
                                        mContext.unbindService(mScreenshotConnection);
                                        mScreenshotConnection = null;
                                        mHandler.removeCallbacks(mScreenshotTimeout);
                                    }
                                }
                            }
                        };
                        msg.replyTo = new Messenger(h);
                        msg.arg1 = msg.arg2 = 0;
                        if (mStatusBar != null && mStatusBar.isVisibleLw())
                            msg.arg1 = 1;
                        if (mNavigationBar != null && mNavigationBar.isVisibleLw())
                            msg.arg2 = 1;
                        try {
                            messenger.send(msg);
                        } catch (RemoteException e) {
                        }
                    }
                }
                @Override
                public void onServiceDisconnected(ComponentName name) {}
            };
            if (mContext.bindServiceAsUser(
                    intent, conn, Context.BIND_AUTO_CREATE, UserHandle.CURRENT)) {
                mScreenshotConnection = conn;
                mHandler.postDelayed(mScreenshotTimeout, 10000);
            }
        }
    }

        private void doIPTVFuncationHE(int keyCode){
                Log.i(TAG,"doIPTVFuncation"+keyCode);
		String topPackage = getTopActivity(mContext);
		Log.d(TAG,"hebei topPackage = "+topPackage);
		if(!TextUtils.isEmpty(topPackage)&&!topPackage.equals("com.android.smart.terminal.iptv")) {
			Intent mIntent = new Intent();
			mIntent.setAction("com.android.smart.terminal.iptv");
			mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			switch (keyCode) {
				case KeyEvent.KEYCODE_HOME:
					mIntent.putExtra("intentMsg", "EPGDomain");
					break;
				case KeyEvent.KEYCODE_TV:
					mIntent.putExtra("intentMsg", "LIVE_CHANNEL_LIST");
					break;
				case KeyEvent.KEYCODE_MOVIE:
					mIntent.putExtra("intentMsg", "VOD_COTEGORY_PAGE");
					break;
			}
			mContext.startActivity(mIntent);
			return;
		}
                int IPTVKey=0;
                switch (keyCode) {
                case KeyEvent.KEYCODE_HOME:
                IPTVKey=181;
                break;
                case KeyEvent.KEYCODE_TV:
                IPTVKey=183;
                if(SystemProperties.get("ro.ysten.province","master").contains("jiangxi")){
                    IPTVKey=117;
                }
                break;
                case KeyEvent.KEYCODE_FAVOURITE:
                //IPTVKey=186;
                if(SystemProperties.get("ro.ysten.province","master").contains("jiangxi")){
                    IPTVKey=120;
                }
                break;
                case KeyEvent.KEYCODE_MOVIE:
                //IPTVKey=185;
                if(SystemProperties.get("ro.ysten.province","master").contains("jiangxi")){
                    IPTVKey=118;
                }
                break;
                case KeyEvent.KEYCODE_APPS:
                //IPTVKey=185;
                if(SystemProperties.get("ro.ysten.province","master").contains("jiangxi")){
                    IPTVKey=119;
                }
                break;
                case KeyEvent.KEYCODE_VOLUME_MUTE:                                                                                                   
                IPTVKey=91;                                                                                                                          
                break;                                                                                                                               
                case KeyEvent.KEYCODE_MENU:                                                                                                          
                IPTVKey=226;                                                                                                                         
                break;
                }                                                                                                                                    
                Log.i(TAG,"IPTVKey is "+IPTVKey);                                                                                                    
                if (IPTVKey!=0){                                                                                                                     
                try{                                                                                                                                 
            Process p = Runtime.getRuntime().exec("input keyevent "+ IPTVKey);                                                                       
                   }                                                                                                                                 
                        catch(Exception e){                                                                                                          
            Log.i(TAG, "doIPTVFuncation orror");                                                                                               
                }                                                                                                                                    
                }
        }                                              
    // add by YSTen {{

    private void doIPTVFuncationSAX(int keyCode){
        Log.i(TAG,"shaanxi keycode doIPTVFuncationSAX"+keyCode);
        String topPackage = getTopActivity(mContext);
        if(!topPackage.equals(SystemProperties.get("sys.class.launcher.pkname","com.android.smart.terminal.iptv"))) {
            Intent mIntent = new Intent();
            ComponentName compName= new ComponentName(SystemProperties.get("sys.class.launcher.pkname","com.android.smart.terminal.iptv"),
                SystemProperties.get("sys.class.launcher.clsname","com.amt.app.IPTVActivity"));
            switch (keyCode) {
                case KeyEvent.KEYCODE_LIVE:
                case KeyEvent.KEYCODE_TV:
                case KeyEvent.KEYCODE_XIRIRED:
                mIntent.putExtra("intentMsg", "LIVE_CHANNEL_LIST");
                break;
                case KeyEvent.KEYCODE_MOVIE:
                case KeyEvent.KEYCODE_XIRIGREEN:
                mIntent.putExtra("intentMsg", "VOD_CATEGORY_PAGE");
                break;
                case KeyEvent.KEYCODE_APPS:
                mIntent.putExtra("intentMsg", "Infomation");
                break;
                case KeyEvent.KEYCODE_FAVOURITE:
                case KeyEvent.KEYCODE_XIRIBLUE:
                mIntent.putExtra("intentMsg", "TVOD_CHANNEL_LIST");
                break;
            }
            if(mIntent.hasExtra("intentMsg")){
                mIntent.setComponent(compName);
                mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                mContext.startActivityAsUser(mIntent, UserHandle.CURRENT);
            }
            return;
        }
        int IPTVKey = 0;
        switch (keyCode) {
            case KeyEvent.KEYCODE_LIVE:
            case KeyEvent.KEYCODE_TV:
            case KeyEvent.KEYCODE_XIRIRED:
            IPTVKey = 275;
            break;
            case KeyEvent.KEYCODE_MOVIE:
            case KeyEvent.KEYCODE_XIRIGREEN:
            IPTVKey = 276;
            break;
            case KeyEvent.KEYCODE_APPS:
            IPTVKey = 277;
            break;
            case KeyEvent.KEYCODE_FAVOURITE:
            case KeyEvent.KEYCODE_XIRIBLUE:
            IPTVKey = 278;
            break;
        }
        Log.i(TAG,"shaanxi iptv keycode = "+IPTVKey);
        if (IPTVKey!=0){
            long now = SystemClock.uptimeMillis();
            InputManager inputManager = (InputManager) mContext.getSystemService(Context.INPUT_SERVICE);
            inputManager.injectInputEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, IPTVKey, 0), InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
            inputManager.injectInputEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, IPTVKey, 0), InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        }
    }

    private void doFunctionLogin(int keycode){
		Log.i(TAG,"doFunctionLogin " + keycode);
        String topPackage = getTopActivity(mContext);
        if(!topPackage.equals("tv.icntv.ott")) {
			switch (keycode) {
				case KeyEvent.KEYCODE_HOME:
				case KeyEvent.KEYCODE_TV:
				case KeyEvent.KEYCODE_XIRIRED:
				case KeyEvent.KEYCODE_MOVIE:
				case KeyEvent.KEYCODE_XIRIGREEN:
				case KeyEvent.KEYCODE_FAVOURITE:
				case KeyEvent.KEYCODE_XIRIBLUE:				
					Intent intent11 = new Intent();
					ComponentName comp11 = new ComponentName("com.huawei.stb.tm1", "com.huawei.stb.tm.ui.service.BootService"); 
					intent11.setComponent(comp11);
                    intent11.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
					mContext.startService(intent11);
				break;
			}				
		}
	}
	
    private void doIPTVFuncationFJ(int keyCode) {
        Log.i(TAG,"doIPTVFuncationFJ"+keyCode);
        String topPackage = getTopActivity(mContext);
        //start by lizheng 20190318 for IPTV four color key
        if(!topPackage.equals("com.ysten.sjiptv")) {
            if(keyCode == 1113) {
                return;
            }

			Intent mIntent = new Intent();
            ComponentName compName= new ComponentName("com.ysten.sjiptv","com.ysten.sjiptv.app.SJIPTVActivity");
			
			int started = SystemProperties.getInt("persist.sys.launcher.displayed", 0);
			//未登录时先进setting后按四色键黑屏
			if(started == 1){				
				switch (keyCode) {
		        case KeyEvent.KEYCODE_HOME:		
                mIntent.putExtra("intentMsg", "EPGDomain");
                break;
		        case KeyEvent.KEYCODE_TV:
		        case KeyEvent.KEYCODE_XIRIRED:					
                mIntent.putExtra("intentMsg", "LIVE_CHANNEL_LIST");
                break;
		        case KeyEvent.KEYCODE_MOVIE:
		        case KeyEvent.KEYCODE_XIRIGREEN:		
                mIntent.putExtra("intentMsg", "VOD_CATEGORY_PAGE");
                break;
		        case KeyEvent.KEYCODE_FAVOURITE:
		        case KeyEvent.KEYCODE_XIRIBLUE:				
                mIntent.putExtra("intentMsg", "Infomation");
                break;
				}	
			}
			mIntent.setComponent(compName);
            mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			mContext.startActivityAsUser(mIntent, UserHandle.CURRENT);
            return;	
			
		}
		//end by lizheng 20190318 for IPTV four color key
		int IPTVKey=0;
		switch (keyCode) {
		case KeyEvent.KEYCODE_HOME:		
                IPTVKey=181;
                break;
		case KeyEvent.KEYCODE_TV:		
                IPTVKey=183;
                break;
		case KeyEvent.KEYCODE_FAVOURITE:		
                IPTVKey=186;
                break;
		case KeyEvent.KEYCODE_MOVIE:		
                IPTVKey=185;
                break;
		case KeyEvent.KEYCODE_XIRIRED:		
                IPTVKey=183;
                break;
		case KeyEvent.KEYCODE_XIRIGREEN:		
                IPTVKey=185;
                break;
		case KeyEvent.KEYCODE_XIRIBLUE:		
                IPTVKey=186;
                break;
		//start by lizheng 20190327 to add mute key
        case KeyEvent.KEYCODE_VOLUME_MUTE:		
                IPTVKey=91;
		//end by lizheng 20190327 to add mute key
                break;
		}
		Log.i(TAG,"IPTVKey is "+IPTVKey);
		//start by lizheng 20190318 for IPTV four color key
		if (IPTVKey!=0){
		long now = SystemClock.uptimeMillis();
		InputManager inputManager = (InputManager) mContext.getSystemService(Context.INPUT_SERVICE);
		//edit by lizheng to solve that  LED keeps red after pressing home key
		inputManager.injectInputEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, IPTVKey, 0), InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
		inputManager.injectInputEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, IPTVKey, 0), InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
		}
		//end by lizheng 20190318 for IPTV four color key
		
	}
    //add for jiangxi iptv four color key
	private void doIPTVFuncationJX(int keyCode){
        Log.i(TAG,"doIPTVFuncationJX"+keyCode);
		int IPTVKey=0;
        switch (keyCode) {
            case KeyEvent.KEYCODE_HOME:
                IPTVKey=181;
                break;
            case KeyEvent.KEYCODE_TV:
                IPTVKey=117;
                break;
            case KeyEvent.KEYCODE_FAVOURITE:
                IPTVKey=120;
                break;
            case KeyEvent.KEYCODE_MOVIE:
                IPTVKey=118;
                break;
            case KeyEvent.KEYCODE_APPS:
                IPTVKey=119;
                break;    
        }                                                                                                                                    
        Log.i(TAG,"IPTVKey is "+IPTVKey);                                                                                                    
        if (IPTVKey!=0){                                                                                                                     
            try{                                                                                                                                 
                Process p = Runtime.getRuntime().exec("input keyevent "+ IPTVKey);                                                                       
            }catch(Exception e){                                                                                                          
                Log.i(TAG, "doIPTVFuncation orror");                                                                                               
            }                                                                                                                                    
        }
    } 
	//begin by zhangjunjian,20190812 for IPTV four color key
	private void doIPTVFuncationGZ(int keyCode){
		Log.i(TAG,"doIPTVFuncation"+keyCode);
		String topPackage = getTopActivity(mContext);
		if(!topPackage.equals("com.android.smart.terminal.iptv")) {
			Intent mIntent = new Intent();
			ComponentName compName= new ComponentName("com.android.smart.terminal.iptv","com.amt.app.IPTVActivity");
			switch (keyCode) {
		        case KeyEvent.KEYCODE_HOME:		
                mIntent.putExtra("intentMsg", "EPGDomain");
                break;
		        case KeyEvent.KEYCODE_TV:
		        case KeyEvent.KEYCODE_XIRIRED:					
                mIntent.putExtra("intentMsg", "LIVE_CHANNEL_LIST");
                break;
				case KeyEvent.KEYCODE_APPS:					
                mIntent.putExtra("intentMsg", "Infomation");
                break;
		        case KeyEvent.KEYCODE_MOVIE:
		        case KeyEvent.KEYCODE_XIRIGREEN:		
                mIntent.putExtra("intentMsg", "VOD_CATEGORY_PAGE");
                break;
		        case KeyEvent.KEYCODE_FAVOURITE:
		        case KeyEvent.KEYCODE_XIRIBLUE:				
                mIntent.putExtra("intentMsg", "Infomation");
                break;
				}	
			mIntent.setComponent(compName);
            mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			mContext.startActivityAsUser(mIntent, UserHandle.CURRENT);
            return;	
			
		}
		int IPTVKey=0;
		switch (keyCode) {
		case KeyEvent.KEYCODE_HOME:		
				IPTVKey=181;
                break;
		case KeyEvent.KEYCODE_TV:		
                IPTVKey=136;
                break;
		case KeyEvent.KEYCODE_APPS:		
                IPTVKey=138;
                break;
		case KeyEvent.KEYCODE_FAVOURITE:		
                IPTVKey=139;
                break;
		case KeyEvent.KEYCODE_MOVIE:		
                IPTVKey=137;
                break;
		case KeyEvent.KEYCODE_XIRIRED:		
                IPTVKey=183;
                break;
		case KeyEvent.KEYCODE_XIRIGREEN:		
                IPTVKey=185;
                break;
		case KeyEvent.KEYCODE_XIRIBLUE:		
                IPTVKey=186;
                break;
        case KeyEvent.KEYCODE_VOLUME_MUTE:		
                IPTVKey=91;
                break;
		}
		Log.i(TAG,"IPTVKey is "+IPTVKey);
		if (IPTVKey!=0){
            try{
				Process p = Runtime.getRuntime().exec("input keyevent "+ IPTVKey);
			}catch(Exception e){
				Log.i(TAG, "dokey error");
			}
		}
		
	}
	//begin by zhangjunjian,20190812 for IPTV four color key
	
    private void doFunAction(int keyCode) {
        Intent funIntent = new Intent();
        String funAction = null;
        String funActionUrl = null;
		
		String keyEnable = Settings.System.getString(mContext.getContentResolver(), "funtionkey");
		Log.w(TAG, "keyEnable= "+keyEnable);
                Log.w(TAG, "doFunAction keyCode= "+keyCode);
		String [] keyNumber = new String[6];
		if (!TextUtils.isEmpty(keyEnable)){
			for (int i=0; i<6; i++) {
            keyNumber[i]=keyEnable.substring(i,i+1);
			}
		}

        // start edit by lingyun for jiangxi HOME keycode @20181207
        switch (keyCode) {
          case KeyEvent.KEYCODE_TV:
              if (keyNumber[0].equals("1")) {
                  funActionUrl = "live";
				  //begin by wcy solve hlj F1
                  //inputkeyevent(136);//F6
				  //end by wcy solve hlj F1
              } else {
                  return;
              }
              break;
          case KeyEvent.KEYCODE_MOVIE:
              if (keyNumber[3].equals("1")) {
                  funActionUrl = "vod";
                  inputkeyevent(137);//F7
              } else {
                  return;
              }
              break;
          case KeyEvent.KEYCODE_APPS:
              if (keyNumber[4].equals("1")) {
                  funActionUrl = "app";
                  inputkeyevent(138);//F8
              } else {
                  return;
              }
              break;
          case KeyEvent.KEYCODE_FAVOURITE:
              if (keyNumber[5].equals("1")) {
                  funActionUrl = "collection";
                  inputkeyevent(139);//F9
              } else {
                  return;
              }
              break;
          }
  
          if (!SystemProperties.get("ro.ysten.province","master").contains("jiangxi")){
              funAction = "com.ysten.action.OpenApp";
              funIntent.setAction(funAction);
              funIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
              funIntent.putExtra("actionUrl", funActionUrl);
              mContext.startActivity(funIntent);
          }
    }

    //begin:add by zhuhengxuan for four color key at 20200629 BUGID:7926
    private void doJSFunAction(int keyCode) {
        Intent funIntent = new Intent();
        String funAction = null;
        String funActionUrl = null;
        Log.w(TAG, "doJSFunAction keyCode= "+keyCode);
        int OTTKey = 0;
        int killsetting = 0;
        switch (keyCode) {
            case KeyEvent.KEYCODE_HOME:
                killsetting=1;
                break;
            case KeyEvent.KEYCODE_TV:
                OTTKey=136;
                break;
            case KeyEvent.KEYCODE_MOVIE:
                OTTKey=137;
                break;
            case KeyEvent.KEYCODE_APPS:
                OTTKey=138;
                break;
            case KeyEvent.KEYCODE_FAVOURITE:
                OTTKey=139;
                break;
            default:
                return;
        }
        if (OTTKey!=0 || killsetting==1){
            try{
                if("com.ysten.setting".equals(getTopActivity(mContext))){
                    HiSysManager hisys = new HiSysManager();
                    hisys.rootSystem("busybox killall -9 com.ysten.setting");
                }
                Log.i(TAG,"doJSFunAction,OTTKey = "+OTTKey);
                Process p = Runtime.getRuntime().exec("input keyevent "+ OTTKey);
            }
            catch(Exception e){
                Log.i("jiangsu", "doJSFunAction error");
            }
        }
    }
    //end:add by zhuhengxuan for four color key at 20200629 BUGID:7926
    private void doFunActionJX(int keyCode) {
        Intent funIntent = new Intent();
        String funAction = null;
        String funActionUrl = null;
		
		String keyEnable = Settings.System.getString(mContext.getContentResolver(), "funtionkey");
		Log.w(TAG, "keyEnable= "+keyEnable);
        Log.w(TAG, "doFunActionJX keyCode= "+keyCode);
		String [] keyNumber = new String[6];
		if (!TextUtils.isEmpty(keyEnable)){
			for (int i=0; i<6; i++) {
            keyNumber[i]=keyEnable.substring(i,i+1);
			}
		}

        // start edit by lingyun for jiangxi HOME keycode @20181207
        switch (keyCode) {
            case KeyEvent.KEYCODE_TV:
                if (keyNumber[0].equals("1")) {
                    funActionUrl = "live";
                } else {
                    return;
                }
                break;
            case KeyEvent.KEYCODE_MOVIE:
                if (keyNumber[3].equals("1")) {
                    funActionUrl = "vod";
                } else {
                    return;
                }
                break;
            case KeyEvent.KEYCODE_APPS:
                if (keyNumber[4].equals("1")) {
                    funActionUrl = "app";   
                } else {
                    return;
                }
                break;
            case KeyEvent.KEYCODE_FAVOURITE:
                if (keyNumber[5].equals("1")) {
                    funActionUrl = "collection"; 
                } else {
                    return;
                }
                break;
            default:
                return;
        }
  
        funAction = "com.ysten.action.OpenApp";
        funIntent.setAction(funAction);
        funIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        funIntent.putExtra("actionUrl", funActionUrl);
        mContext.startActivity(funIntent);
         
    }
    private void inputkeyevent(int keycodetest){
      if(!SystemProperties.get("ro.ysten.province","master").contains("jiangxi")){
        return;
      }	
      try{
        Process p = Runtime.getRuntime().exec("input keyevent "+ keycodetest);
        }catch(Exception e){
        Log.i(TAG, "dokey error");
        }
      }
    
      // end edit by lingyun for jiangxi HOME keycode @20181207


    private void doGDFunAction(int keyCode) {
         Log.i(TAG,"doIPTVFuncation"+keyCode);
         int IPTVKey=0;
        switch (keyCode) {
            case KeyEvent.KEYCODE_TV:
                IPTVKey=183;
                break;
            case KeyEvent.KEYCODE_MOVIE:
                IPTVKey=184;
                break;
            case KeyEvent.KEYCODE_APPS:
                IPTVKey=185;
                break;
            case KeyEvent.KEYCODE_FAVOURITE:
                IPTVKey=186;
                break;
        }
          if (IPTVKey!=0){
                try{
                    Process p = Runtime.getRuntime().exec("input keyevent "+ IPTVKey);
                   }
                        catch(Exception e){
                       Log.i("zjj", "doIPTVFuncation orror");
                   }
                }
    }

     //begin:add by xuyunfeng at 20200403 add color key
	 private void doShanXiIptvFunAction(int keyCode) {
         Log.i(TAG,"doShanXiIptvFunAction"+keyCode);
         int IPTVKey=0;
        switch (keyCode) {
            case KeyEvent.KEYCODE_TV:
                IPTVKey=186;
                break;
            case KeyEvent.KEYCODE_MOVIE:
                IPTVKey=184;
                break;
            case KeyEvent.KEYCODE_APPS:
                IPTVKey=183;
                break;
            case KeyEvent.KEYCODE_FAVOURITE:
                IPTVKey=185;
                break;
        }
        if (IPTVKey!=0){
            try{
                Log.i(TAG,"doShanXiIptvFunAction,IPTVKey = "+IPTVKey);
                Process p = Runtime.getRuntime().exec("input keyevent "+ IPTVKey);
            }
            catch(Exception e){
                Log.i("shanxi", "doShanXiIptvFunAction orror");
            }
        }
    }
    //end:add by xuyunfeng at 20200403 add color key
	
	 //begin:add by xumiao at 20190725 add color key
	 private void doJiLinFunAction(int keyCode) {
         Log.i(TAG,"doJiLinFunAction"+keyCode);
         int IPTVKey=0;
        switch (keyCode) {
            case KeyEvent.KEYCODE_TV:
                IPTVKey=131;
                break;
            case KeyEvent.KEYCODE_MOVIE:
                IPTVKey=132;
                break;
            case KeyEvent.KEYCODE_APPS:
                IPTVKey=133;
                break;
            case KeyEvent.KEYCODE_FAVOURITE:
                IPTVKey=134;
                break;
        }
        if (IPTVKey!=0){
            try{
                Process p = Runtime.getRuntime().exec("input keyevent "+ IPTVKey);
            }
            catch(Exception e){
                Log.i("jilin", "doIPTVFuncation orror");
            }
        }
    }
    //end:add by xumiao at 20190725 add color key
    // add by YSTen }}
 private void handleLongPressForIsTV(int keyCode,boolean istvstart)
{
String keyvalueistv="";
   switch(keyCode)
   {
     case KeyEvent.KEYCODE_MENU:keyvalueistv="menu";break;
     case KeyEvent.KEYCODE_HOME:keyvalueistv="home";break;
     case KeyEvent.KEYCODE_SETTINGS:keyvalueistv="settings";break;
     case KeyEvent.KEYCODE_TV:keyvalueistv="live";break;

     case KeyEvent.KEYCODE_MOVIE:keyvalueistv="vod";break;
     case KeyEvent.KEYCODE_APPS:keyvalueistv="app";break;
     case KeyEvent.KEYCODE_FAVOURITE:keyvalueistv="collect";break;
	 case KeyEvent.KEYCODE_F11:keyvalueistv="enter";break;

    }

if(keyvalueistv.equals(""))
return;
if(istvstart)
{
            Intent mIntent = new Intent("com.ysten.intent.action.KEY_LONG_PRESSED_START");
	    mIntent.putExtra("keyName",keyvalueistv);
        mIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
	    mContext.sendBroadcast(mIntent);
		Log.d("xwj", "com.ysten.intent.action.KEY_LONG_PRESSED_START "+keyvalueistv);
}
else
{
            Intent mIntent = new Intent("com.ysten.intent.action.KEY_LONG_PRESSED_END");
	    mIntent.putExtra("keyName",keyvalueistv);
		mIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
	    mContext.sendBroadcast(mIntent);
Log.d("xwj", "com.ysten.intent.action.KEY_LONG_PRESSED_END "+keyvalueistv);
}
}

private void ComKey_startBluetooth(KeyEvent event)
{
    final int keyCode = event.getKeyCode();
     if(event.getAction() == KeyEvent.ACTION_DOWN)
        {
        if((keyCode == KeyEvent.KEYCODE_DPAD_UP)&& (event.getAction() == KeyEvent.ACTION_DOWN))
        {
            mStartBluetoothSettingFlag = 1;
        }
        else if((keyCode == KeyEvent.KEYCODE_DPAD_DOWN)&& (event.getAction() == KeyEvent.ACTION_DOWN))
        {
            if(mStartBluetoothSettingFlag == 1)
            {
                mStartBluetoothSettingFlag = 2;
            }
            else
            {
                mStartBluetoothSettingFlag = 0;
            }
        }
        else if((keyCode == KeyEvent.KEYCODE_DPAD_LEFT)&& (event.getAction() == KeyEvent.ACTION_DOWN))
        {
            if(mStartBluetoothSettingFlag == 2)
            {
                mStartBluetoothSettingFlag = 3;
            }
            else
            {
                mStartBluetoothSettingFlag = 0;
            }
        }
        else if((keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)&& (event.getAction() == KeyEvent.ACTION_DOWN))
        {
            if(mStartBluetoothSettingFlag == 3)
            {
                mStartBluetoothSettingFlag = 4;
            }
            else
            {
                mStartBluetoothSettingFlag = 0;
            }
        }
        else if((keyCode == KeyEvent.KEYCODE_BACK)&& (event.getAction() == KeyEvent.ACTION_DOWN))
        {
            if(mStartBluetoothSettingFlag == 4)
            {
                mStartBluetoothSettingFlag = 5;
            }
            else
            {
                mStartBluetoothSettingFlag = 0;
            }
        }
        else if((keyCode == KeyEvent.KEYCODE_DPAD_CENTER)&& (event.getAction() == KeyEvent.ACTION_DOWN))
        {
            if(mStartBluetoothSettingFlag == 5)
            {
					Intent intent = new Intent();
                	intent.setComponent(new ComponentName("com.amlogic.bluetooth", "com.amlogic.bluetooth.MainActivity"));
		            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent); 
                mStartBluetoothSettingFlag = 0;
            }
            else
            {
                mStartBluetoothSettingFlag = 0;
            }
        }
        else if(event.getAction() == KeyEvent.ACTION_DOWN)
        {
            mStartBluetoothSettingFlag = 0;
        }
        }

}
private void doFunAction_cm201_yunnan( int keyCode)
{
    switch (keyCode) {
            case KeyEvent.KEYCODE_TV:
                {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.gitv.tv.live", "com.gitv.tv.live.activity.VodActivity"));
    	            //intent.putExtra("show_lock_in_setting",true);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                     mContext.startActivity(intent);
                }
                break;
            case KeyEvent.KEYCODE_MOVIE:
                {
					Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.gitv.tv.launcher", "com.gitv.tv.launcher.activity.GitvInterfaceActivity"));
    	            intent.putExtra("type","3");
    	            intent.putExtra("chnId","1000001");
    	            intent.putExtra("cpId","1");
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                     mContext.startActivity(intent);
                }
                break;
            case KeyEvent.KEYCODE_APPS:
                break;
            case KeyEvent.KEYCODE_FAVOURITE:
                { 
				    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.gitv.tv.launcher", "com.gitv.tv.launcher.activity.GitvInterfaceActivity"));
    	            intent.putExtra("type","6");
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                     mContext.startActivity(intent);
                }
                break;
            default :
                break;


         }



}
    //add by ysten.zhangjunjian 20190722 for yunnan iptv
	private void doFunAction_cm201_yunnan_iptv( int keyCode)
    {
		 Log.i("zjj","top activity is "+keyCode);
		 int IPTVKey=0;
        switch (keyCode) {
            case KeyEvent.KEYCODE_TV:
                IPTVKey=136;
                break;
            case KeyEvent.KEYCODE_MOVIE:
                IPTVKey=137;
                break;
            case KeyEvent.KEYCODE_APPS:
					/*Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.gitv.tv.launcher", "com.gitv.tv.launcher.activity.GitvInterfaceActivity"));
    	            intent.putExtra("type","3");
    	            intent.putExtra("chnId","1000001");
    	            intent.putExtra("cpId","1");
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                     mContext.startActivity(intent);*/
			   IPTVKey=138;
                break;
            case KeyEvent.KEYCODE_FAVOURITE:
                IPTVKey=139;
                break;
            default :
			    Log.d(TAG,"default ");
                break;
         }
		if (IPTVKey!=0){
			Log.d(TAG,"IPTVKEY="+IPTVKey);
		long now = SystemClock.uptimeMillis();
		InputManager inputManager = (InputManager) mContext.getSystemService(Context.INPUT_SERVICE);
		//edit by lizheng to solve that  LED keeps red after pressing home key
		inputManager.injectInputEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, IPTVKey, 0), InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
		inputManager.injectInputEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, IPTVKey, 0), InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
		}
    }
	//end by ysten.zhangjunjian,20190722 for yunnan iptv

    private void doFunActionFJ(int keyCode) {
		ActivityManager am = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
        String packageName = cn.getPackageName();
        String className =cn.getClassName();
        Log.i(TAG,"top activity is "+packageName +"/"+className);
		//begin by ysten.lizheng,20181012,fix four color key bug
		if (packageName.contains("com.huawei.stb.tm")||packageName.contains("com.fujian.provision")
			|| packageName.contains("com.ysten.stbguide")){
		return;
		}
		//end by ysten.lizheng,20181012,fix four color key bug
		if (Settings.System.getInt(mContext.getContentResolver(), "IS_RESPOND_HOME_KEY", 0)==1){
		return;  
		}
		
        Intent funIntent = new Intent();
        String funAction = null;
        String funActionUrl = null;

        String keyEnable = Settings.System.getString(mContext.getContentResolver(), "funtionkey");
		Log.w(TAG, "keyEnable= "+keyEnable);
		String [] keyNumber = new String[8];
		if (TextUtils.isEmpty(keyEnable)){
            return;
        }else{
			for (int i=0; i<8; i++) {
            keyNumber[i]=keyEnable.substring(i,i+1);
			}
		}

        switch (keyCode) {
            case KeyEvent.KEYCODE_TV:		
                if (keyNumber[0].equals("1")){			
                funActionUrl = "live";
				}else{
				return;
				}
                break;
            case KeyEvent.KEYCODE_MOVIE:
                if (keyNumber[3].equals("1")){			
                funActionUrl = "vod";
				}else{
				return;
				} 
                break;
            case KeyEvent.KEYCODE_APPS:
                if (keyNumber[4].equals("1")){			
                funActionUrl = "app";
				}else{
				return;
				} 
                break;
            case KeyEvent.KEYCODE_FAVOURITE:
                if (keyNumber[5].equals("1")){			
                funActionUrl = "collection";
				}else{
				return;
				} 
                break;
			case KeyEvent.KEYCODE_XIRISEARCH:                			
                funActionUrl = "search";
                break;
			case KeyEvent.KEYCODE_XIRIRECORD:                			
                funActionUrl = "footprint";
                break;
			case KeyEvent.KEYCODE_DIAL:                			
                funActionUrl = "app";
				funIntent.putExtra("type", "4");
                break;
			case KeyEvent.KEYCODE_CHILD:                			
                funActionUrl = "vod";
				funIntent.putExtra("type", "1");
                break;
			case KeyEvent.KEYCODE_EDUCATION:                			
                funActionUrl = "vod";
				funIntent.putExtra("type", "2");
                break;
			case KeyEvent.KEYCODE_XIRIMUSIC:                			
                funActionUrl = "app";
				funIntent.putExtra("type", "2");
                break;
			case KeyEvent.KEYCODE_PE:                			
                funActionUrl = "vod";
				funIntent.putExtra("type", "3");
                break;
			case KeyEvent.KEYCODE_GAME:                			
                funActionUrl = "app";
				funIntent.putExtra("type", "1");
                break;
			case KeyEvent.KEYCODE_APPLIANCE:                			
                funActionUrl = "app";
				funIntent.putExtra("type", "3");
                break;
			case KeyEvent.KEYCODE_XIRIRED:                			
                funActionUrl = "live";
                break;
			case KeyEvent.KEYCODE_XIRIGREEN:                			
                funActionUrl = "vod";
                break;
			case KeyEvent.KEYCODE_XIRIBLUE:                			
                funActionUrl = "app";
                break;
        }

        //if(isActivityRunning(mContext, "tv.icntv.ott.Gefo")) {
           // funAction = "com.ysten.action.OpenGefo"; 
        //} else {
            funAction = "com.ysten.action.OpenApp";
        //}

        funIntent.setAction(funAction);
        funIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);// | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        funIntent.putExtra("actionUrl", funActionUrl);
		//funIntent.putExtra("type", "1");
        mContext.startActivity(funIntent);
    }
	
    private void doFunActionHB(int keyCode) {
        Log.i(TAG,"doFuncationHB"+keyCode);
        Intent funIntent = new Intent();
        String funAction = null;
        int funActionKey = 0;

        switch (keyCode) {
            case 170: 
                funActionKey = 188; 
                break;
            case 1112:
                funActionKey = 185; 
                break;
            case 1113:
                funActionKey = 0; // not used,because bestv not support
                break;
            case 1114:
                funActionKey = 187; 
                break;
        }
        funAction = "bestv.ott.action.launcher.shortcut"; 

        funIntent.setAction(funAction);
        funIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        funIntent.putExtra("keycode", funActionKey);
        mContext.startActivity(funIntent);
    }
	
	private void doIPTVFuncationHB(int keyCode){
		Log.i(TAG,"doIPTVFuncationiiiiiiiiiiiiiiiiiiiiiiiiiii"+keyCode);
		int IPTVKey=0;
	
        switch (keyCode) {
            case 170: 
                IPTVKey = 1187; 
                break;
            case 1112:
                IPTVKey = 1182; 
                break;
            case 1113:
                IPTVKey = 1186; // not used,because bestv not support
                break;
            case 1114:
                IPTVKey = 1188; 
                break;
        }
		Log.i(TAG,"IPTVKey111111111111111="+IPTVKey);
		if (IPTVKey!=0){
		    try{
                Process p = Runtime.getRuntime().exec("input keyevent "+ IPTVKey);
		    } catch(Exception e){
                Log.i(TAG, "doIPTVFuncation orror");
		    }
		}
	}			
	
    private boolean isWifiOpenAH = false;
    // add by YSTen }}
    /** {@inheritDoc} */
    @Override
    public int interceptKeyBeforeQueueing(KeyEvent event, int policyFlags, boolean isScreenOn) {
		//begin add by ysten hyz at 20190311:find problem of four color key 
		final boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        final boolean canceled = event.isCanceled();
        final int keyCode = event.getKeyCode();
		
        if("factory".equals(SystemProperties.get("persist.sys.yst.testf"))
            &&"com.softwinner.agingdragonbox".equals(getTopActivityInfo(mContext))
			&&keyCode != KeyEvent.KEYCODE_VOLUME_DOWN
			&&keyCode != KeyEvent.KEYCODE_VOLUME_UP
			&&keyCode != KeyEvent.KEYCODE_VOLUME_MUTE ){
            return 0;
        }
       
        /*add by zhaolianghua for anhui wifi control start @20200327*/
        if(keyCode == KeyEvent.KEYCODE_MENU
            &&SystemProperties.get("ro.ysten.province","master").contains("anhui")){
            /*begin:add by zhanghk at 20200819:not show WIFI in STB that is no wifi momodule*/
	    if(!TextUtils.isEmpty(SystemProperties.get("persist.sys.wifimodule"))){
            	if(!"visible".equals(Settings.System.getString(mContext.getContentResolver(), "ah_wifi_visible"))){
                    if(down){
                    	mHandler.sendEmptyMessageDelayed(MSG_AH_SHOW_WIFI_IN_SETTING,5000);
                    }else{
                        mHandler.removeMessages(MSG_AH_SHOW_WIFI_IN_SETTING);
                    }
                }else{
                    if(isWifiOpenAH&&!down){
                        isWifiOpenAH = false;
                        return 0;
                    }
               }
	   }
	  /*end:add by zhanghk at 20200819:not show WIFI in STB that is no wifi momodule*/
        }
        /*add by zhaolianghua end*/

        
        if(SystemProperties.get("ro.ysten.province","master").contains("liaoning")){
           mHandler.removeCallbacks(mSystemSleep);    
           int sleepDelay=Settings.System.getInt(mContext.getContentResolver(),SYSTEM_SLEEP_TIME_DELAY,240);
		   Log.d(TAG, "liaoning sleep Delay:" + sleepDelay);
           if(sleepDelay > 0)
              mHandler.postDelayed(mSystemSleep,sleepDelay*60*1000);
        }else if(SystemProperties.get("ro.ysten.province","master").equals("cm201_yunnan")){
           mHandler.removeCallbacks(mSystemSleep);    
           int sleepDelay=Settings.System.getInt(mContext.getContentResolver(),SYSTEM_SLEEP_TIME_DELAY,240);
                  Log.d(TAG, "yunnan sleep Delay:" + sleepDelay);
           if(sleepDelay > 0)
              mHandler.postDelayed(mSystemSleep,sleepDelay*60*1000);
        }else if(down && SystemProperties.get("persist.sys.autosleep.enable", "false").equals("true")){
         //begin: add by tianchining at 20191106: 重置待机倒计时消息
            if(isCancelDialog){
                mHandler.removeCallbacks(mSleepCancleTimer);
                isCancelDialog = false;
                mDialog.dismiss();
            }

            int sleepDelay = SystemProperties.getInt("persist.sys.autosleep.delay", 240);
            Log.d(TAG, "TCN_ADD: interceptKeyBeforeQueueing, isCancelDialog: " + isCancelDialog + ", sleepDelay: " + sleepDelay);
            mHandler.removeCallbacks(mSystemSleep);
            mHandler.postDelayed(mSystemSleep,sleepDelay*60*1000);
        }
        //end: add by tianchining at 20191106: 重置待机倒计时消息
		
		//end add by ysten hyz at 20190311:find problem of four color key 
        if (!mSystemBooted) {
            // If we have not yet booted, don't let key events do anything.
            return 0;
        }

        /* final boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        final boolean canceled = event.isCanceled();
        final int keyCode = event.getKeyCode(); */

        final boolean isInjected = (policyFlags & WindowManagerPolicy.FLAG_INJECTED) != 0;

        // If screen is off then we treat the case where the keyguard is open but hidden
        // the same as if it were open and in front.
        // This will prevent any keys other than the power button from waking the screen
        // when the keyguard is hidden by another activity.
        final boolean keyguardActive = (mKeyguardDelegate == null ? false :
                                            (isScreenOn ?
                                                mKeyguardDelegate.isShowingAndNotHidden() :
                                                mKeyguardDelegate.isShowing()));

        if (keyCode == KeyEvent.KEYCODE_POWER || keyCode == KeyEvent.KEYCODE_STB_POWER) {
            policyFlags |= WindowManagerPolicy.FLAG_WAKE;
        }
        final boolean isWakeKey = (policyFlags & (WindowManagerPolicy.FLAG_WAKE
                | WindowManagerPolicy.FLAG_WAKE_DROPPED)) != 0;

        if (DEBUG_INPUT) {
            Log.d(TAG, "interceptKeyTq keycode=" + keyCode
                    + " screenIsOn=" + isScreenOn + " keyguardActive=" + keyguardActive
                    + " policyFlags=" + Integer.toHexString(policyFlags)
                    + " isWakeKey=" + isWakeKey);
        }
        if(SystemProperties.get("ro.ysten.province","master").equals("cm201_yunnan")||
        		SystemProperties.get("ro.ysten.province","master").equals("c60_yunnan"))
        {
            ComKey_startBluetooth(event); 
        }
        if (down && (policyFlags & WindowManagerPolicy.FLAG_VIRTUAL) != 0
                && event.getRepeatCount() == 0) {
            performHapticFeedbackLw(null, HapticFeedbackConstants.VIRTUAL_KEY, false);
        }


        // Basic policy based on screen state and keyguard.
        // FIXME: This policy isn't quite correct.  We shouldn't care whether the screen
        //        is on or off, really.  We should care about whether the device is in an
        //        interactive state or is in suspend pretending to be "off".
        //        The primary screen might be turned off due to proximity sensor or
        //        because we are presenting media on an auxiliary screen or remotely controlling
        //        the device some other way (which is why we have an exemption here for injected
        //        events).
        int result;
        if ((isScreenOn && !mHeadless) || (isInjected && !isWakeKey)) {
            // When the screen is on or if the key is injected pass the key to the application.
            result = ACTION_PASS_TO_USER;
        } else {
            // When the screen is off and the key is not injected, determine whether
            // to wake the device but don't pass the key to the application.
            result = 0;
            if (down && isWakeKey && isWakeKeyWhenScreenOff(keyCode)) {
                result |= ACTION_WAKE_UP;
            }
        }

        // If the key would be handled globally, just return the result, don't worry about special
        // key processing.
        if (mGlobalKeyManager.shouldHandleGlobalKey(keyCode, event)) {
            return result;
        }

        if( null == keyfun){
            keyfun = new Keyfunction(mContext);
        }
        // one key to do VGA setting
        if (keyCode == KeyEvent.KEYCODE_RESOLUTION_RATIO) {
            if (down) {
                keyfun.readSwitch();
            } else {
                keyfun.keySwitchFormat();
            }
        }
        //one key to switch ime
        if (keyCode == imeSwitchKey) {
            if (down) {
            } else {
                keyfun.switchInputMethod();
            }
        }
		if (keyCode == screenCaptureKey) {
            if (down) {
            } else {
                // remove by wusw {{
		if(!"cm201_hebei".equals(SystemProperties.get("ro.ysten.province")) && !"cm201_anhui".equals(SystemProperties.get("ro.ysten.province"))){
            if(!"cm201_neimeng".equals(SystemProperties.get("ro.ysten.province")) && !"cm201_zhejiang".equals(SystemProperties.get("ro.ysten.province"))) {
					keyfun.screenCapture(screenCapturePath);
			}
		}
                // remove by wusw }}
            }
        }
        if(down){// one key to app
            if(keyfun.iskeyfunction(keyCode)){
                result &= ~ACTION_PASS_TO_USER;
            }
            Intent intent = new Intent("com.chinamobile.action.KEY_PRESS_DOWN");
            intent.putExtra("keyCode", keyCode);
            mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT_OR_SELF);
        }
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> info = activityManager.getRunningTasks(1);
            ComponentName component = info.get(0).topActivity;
            String pkg = component.getPackageName();
            String pkgclassname = component.getClassName();
            
            Log.d(TAG,"top pkg:"+pkg+"    pkg classname:"+pkgclassname);
        
        switch (keyCode) {
            case KeyEvent.KEYCODE_0:
            case KeyEvent.KEYCODE_1:
            case KeyEvent.KEYCODE_2:
            case KeyEvent.KEYCODE_3:
            case KeyEvent.KEYCODE_4:
            case KeyEvent.KEYCODE_5:
            case KeyEvent.KEYCODE_6:
            case KeyEvent.KEYCODE_7:
            case KeyEvent.KEYCODE_8:
            case KeyEvent.KEYCODE_9:
			
                if(pkg.equals("com.bestv.jingxuan")&&pkgclassname.equals("com.bestv.ott.jingxuan.JX_Activity")
                        //add by ysten zengzhiliang at 20181109:add m301h_anhui
                        && (SystemProperties.get("ro.ysten.province","master").equals("cm201_anhui")
						||SystemProperties.get("ro.ysten.province","master").equals("cm201_anhui_iptv")
						||SystemProperties.get("ro.ysten.province","master").equals("m301h_anhui"))
                        && event.getAction() == KeyEvent.ACTION_DOWN)
                {
               result &= ~ACTION_PASS_TO_USER;
               return result;
                }
                else
                {
                    break;
                }

        }
        // Handle special keys.
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_MUTE: {
				                if (down) {
					if (keyCode == KeyEvent.KEYCODE_VOLUME_MUTE)
					{
					if(SystemProperties.get("ro.ysten.province","master").equals("cm201_hebei")){
					doIPTVFuncationHE(keyCode);
					}
					//start by lizheng to solve IPTV mute 20190321
                    /*if(SystemProperties.get("ro.ysten.province","master").contains("fujian") &&
                    SystemProperties.getInt("persist.sys.launcher.value",0)==2){
                    doIPTVFuncationFJ(keyCode);
                    }*/
					//end by lizheng to solve IPTV mute 20190321
					//start by zhangy for anhui_iptv IPTV mute	20190310
					if(SystemProperties.get("ro.ysten.province","master").equals("cm201_anhui_iptv")){
						doIPTVFunctionAH(keyCode);
                    }
					//end by zhangy for anhui_iptv IPTV mute 20190310
							//add by guangchao.su ad 20180811:add jiangxi iptv home
						/*if(SystemProperties.get("ro.ysten.province","master").equals("cm201_jiangxi") && SystemProperties.get("persist.sys.launcher.value","1").equals("2")){
                                        doIPTVFuncationHE(keyCode);
										Log.w("ysten_cm201", " use this KEYCODE_VOLUME_MUTE ");
										return 0;
										//Log.w("ysten_cm201", " use this KEYCODE_VOLUME_MUTE return 0 ");
                        }*/
						//add by guangchao.su ad 20180811:add jiangxi iptv home			
					AudioManager am1 = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
					am1.setMusicStreamMuteInvalid(1);
				//	HiSysManager hisys = new HiSysManager();
                   // hisys.setProperty("sys.nowmute.invalid", "1");
								}
							}
				
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    if (down) {
                        if (isScreenOn && !mVolumeDownKeyTriggered
                                && (event.getFlags() & KeyEvent.FLAG_FALLBACK) == 0) {
                            mVolumeDownKeyTriggered = true;
                            mVolumeDownKeyTime = event.getDownTime();
                            mVolumeDownKeyConsumedByScreenshotChord = false;
                            cancelPendingPowerKeyAction();
                            interceptScreenshotChord();
                        }
                    } else {
                        mVolumeDownKeyTriggered = false;
                        cancelPendingScreenshotChordAction();
                    }
                } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    if (down) {
                        if (isScreenOn && !mVolumeUpKeyTriggered
                                && (event.getFlags() & KeyEvent.FLAG_FALLBACK) == 0) {
                            mVolumeUpKeyTriggered = true;
                            cancelPendingPowerKeyAction();
                            cancelPendingScreenshotChordAction();
                        }
                    } else {
                        mVolumeUpKeyTriggered = false;
                        cancelPendingScreenshotChordAction();
                    }
                }
                if (down) {
                    ITelephony telephonyService = getTelephonyService();
                    if (telephonyService != null) {
                        try {
                            if (telephonyService.isRinging()) {
                                // If an incoming call is ringing, either VOLUME key means
                                // "silence ringer".  We handle these keys here, rather than
                                // in the InCallScreen, to make sure we'll respond to them
                                // even if the InCallScreen hasn't come to the foreground yet.
                                // Look for the DOWN event here, to agree with the "fallback"
                                // behavior in the InCallScreen.
                                Log.i(TAG, "interceptKeyBeforeQueueing:"
                                      + " VOLUME key-down while ringing: Silence ringer!");

                                // Silence the ringer.  (It's safe to call this
                                // even if the ringer has already been silenced.)
                                telephonyService.silenceRinger();

                                // And *don't* pass this key thru to the current activity
                                // (which is probably the InCallScreen.)
                                result &= ~ACTION_PASS_TO_USER;
                                break;
                            }
                            if (telephonyService.isOffhook()
                                    && (result & ACTION_PASS_TO_USER) == 0) {
                                // If we are in call but we decided not to pass the key to
                                // the application, handle the volume change here.
                                handleVolumeKey(AudioManager.STREAM_VOICE_CALL, keyCode);
                                break;
                            }
                        } catch (RemoteException ex) {
                            Log.w(TAG, "ITelephony threw RemoteException", ex);
                        }
                    }

                    if (isMusicActive() && (result & ACTION_PASS_TO_USER) == 0) {
                        // If music is playing but we decided not to pass the key to the
                        // application, handle the volume change here.
                        handleVolumeKey(AudioManager.STREAM_MUSIC, keyCode);
                        break;
                    }
                }
                break;
            }

            case KeyEvent.KEYCODE_ENDCALL: {
                result &= ~ACTION_PASS_TO_USER;
                if (down) {
                    ITelephony telephonyService = getTelephonyService();
                    boolean hungUp = false;
                    if (telephonyService != null) {
                        try {
                            hungUp = telephonyService.endCall();
                        } catch (RemoteException ex) {
                            Log.w(TAG, "ITelephony threw RemoteException", ex);
                        }
                    }
                    interceptPowerKeyDown(!isScreenOn || hungUp);
                } else {
                    if (interceptPowerKeyUp(canceled)) {
                        if ((mEndcallBehavior
                                & Settings.System.END_BUTTON_BEHAVIOR_HOME) != 0) {
                            if (goHome()) {
                                break;
                            }
                        }
                        if ((mEndcallBehavior
                                & Settings.System.END_BUTTON_BEHAVIOR_SLEEP) != 0) {
                            result = (result & ~ACTION_WAKE_UP) | ACTION_GO_TO_SLEEP;
                        }
                    }
                }
                break;
            }
            case KeyEvent.KEYCODE_STB_POWER:{
                 //if(!SystemProperties.get("ro.product.target").equals("shcmcc"))
                  if( !(SystemProperties.get("ro.ysten.province","master").contains("hubei") || SystemProperties.get("ro.ysten.province","master").contains("jiangxi"))) {
                     return result;
		  }
                }
            case KeyEvent.KEYCODE_POWER: {
                 //test by xue

                  //start edit by lizheng 20190304 to solve fujian power key bug
            if(SystemProperties.get("ro.ysten.province","master").contains("fujian")) {


                Message msg = new Message();
                msg.what=11;
                toastHandle.sendEmptyMessage(11);

                toastHandle.sendMessageDelayed(msg, 3000);
					  Log.d(TAG, "time passed " + (SystemClock.uptimeMillis()-bootCompleteTime)+" ms since bootcomplete");
					  if (mBootCompleted && (SystemClock.uptimeMillis()-bootCompleteTime)<3000){
						  break;
					  }else if(!mBootCompleted){
                      break;
					  }
                   }
				   //end edit by lizheng 20190304 to solve fujian power key bug
                  //test by xue
				if(isActivityRunning(mContext,"com.ysten.tr069")) {
				if(!SystemProperties.get("sys.key.power","on").equals("on")){
					return result;
				}
				}

                //add by sunlei 191127 for 湖北长按powerkey进入power option
			    /*if( SystemProperties.get("ro.ysten.province","master").contains("hubei")) {
					Log.d(TAG, "sunlei+++++hubei++++ ");

                                Log.d(TAG, "key KEYCODE_POWER isScreenOn--->>" + isScreenOn);                                           
                                                                                                                                        
                                if (down && isScreenOn) {                                                                                                                   mContext.startService(new Intent("com.ysten.action.poweroff"));                                         
                                }                                                                                                            
                                break;           
                            }*/
                //end by sunlei 191127 for 湖北长按powerkey进入power option

                // begin: add by tianchining at 20191111: showStandbyPopView
                if(SystemProperties.get("ro.ysten.province", "master").contains("shandong")){
                    Log.d(TAG, "TCN_ADD power key, showStandbyPopView !!");
                    if(event.getAction() == KeyEvent.ACTION_UP ){
                        mHandler.post(mStandbyPopViewRunnable);
                    }
                    break;
                }
                // end: add by tianchining at 20191111: showStandbyPopView

                if(event.getAction() == KeyEvent.ACTION_UP &&
                        (SystemProperties.get("ro.ysten.province","master").equals("cm201_anhui")
                        //add by ysten zengzhiliang at 20181109:add m301h_anhui
                         ||SystemProperties.get("ro.ysten.province","master").equals("m301h_anhui")
						 ||SystemProperties.get("ro.ysten.province","master").equals("cm201_anhui_iptv")
                         || SystemProperties.get("ro.ysten.province","master").contains("liaoning")
                         //add by ysten zhanghk at 20180814:standby directly when press power key 
                         || SystemProperties.get("ro.ysten.province","master").equals("cm201_zhejiang")
                         || SystemProperties.get("ro.ysten.province","master").equals("cm201_shaanxi")
						 || SystemProperties.get("ro.ysten.province","master").contains("henan")
                         || SystemProperties.get("ro.ysten.province","master").contains("cm201_jilin")))//add by xumiao at 20190725 add power key
                {
                    //begin:add by xumiao at 20190725 add power key
                    if(SystemProperties.get("ro.ysten.province","master").contains("cm201_jilin")){
                       mShowPowerDialog=false;
                       showJilinSuspendDialog();
                       break;
                     }
                    //end:add by xumiao at 20190725 add power key
                    /*begin:add by zhanghk at 20181220:send shutdown broadcast*/
                    if(SystemProperties.get("ro.ysten.province","master").equals("cm201_shaanxi")){
                            Log.d(TAG,"send shutdown broadcast");
                            Intent shutdownIntent = new Intent("android.intent.action.ACTION_SHUTDOWN");
                            HiSysManager hisys = new HiSysManager();
                            HiDisplayManager display = new HiDisplayManager();
                            hisys.adjustDevState("/proc/inputblock/status","1");
                            display.setOutputEnable(1,0);
                            display.setHDMIClose(); 
                            mContext.sendBroadcastAsUser(shutdownIntent, UserHandle.ALL);
                    }
                    Log.d(TAG,"after sleep time:"+System.currentTimeMillis());
                    /*end:add by zhanghk at 20181220:send shutdown broadcast*/
                    HiSysManager hisys = new HiSysManager();
			        Intent mIntent;
				    hisys.setProperty("persist.suspend.mode", "deep_restart");
				    SystemProperties.set("persist.sys.smartsuspendin", "0");
                	try
				   {
                    Process p = Runtime.getRuntime().exec("/system/bin/sh /system/bin/hello1.sh 1");
                    Log.i("ysten_sgc", "set hello1.sh to 1 ");
                    String data = "";
                    BufferedReader ie = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                    BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String error = null;
                    while ((error = ie.readLine()) != null && !error.equals("null")) {
                          data += error + "\n";
                    }
                    String line = null;
                    while ((line = in.readLine()) != null && !line.equals("null")) {
                        data += line + "\n";
                    }
                    Log.i("xwj", "Show Suspend Dialog ok"+data);
                    }catch(IOException e){
                        Log.i("xwj", "Show Suspend Dialog error");
                    }
		//		mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		//		mPowerManager.goToSleep(SystemClock.uptimeMillis());
        		/*begin:add by zhanghk at 20181220:shutdown later 3s*/
                if(SystemProperties.get("ro.ysten.province","master").equals("cm201_shaanxi")){
                        Log.d(TAG,"shaanxi shutdown");
                        mHandler.postDelayed(new Runnable() {
                           @Override
                           public void run() {
                              synchronized (this) {
                                  Intent mIntent = new Intent("android.ysten.systemupdate");
                                  mIntent.putExtra("powercontrol", "poweroff");
                                   mContext.sendBroadcast(mIntent);
                               }
                           }
                       }, 3000);
                }else{
                    Log.d(TAG, "-----------------------------------power tosystemupdate");
                    mIntent = new Intent("android.ysten.systemupdate");
                    mIntent.putExtra("powercontrol", "poweroff");
                    mContext.sendBroadcast(mIntent);                    
                }
                /*end:add by zhanghk at 20181220:shutdown later 3s*/

                break;

                }
                result &= ~ACTION_PASS_TO_USER;
                if (down) {
                    mImmersiveModeConfirmation.onPowerKeyDown(isScreenOn, event.getDownTime(),
                            isImmersiveMode(mLastSystemUiFlags));
					Log.i("ysten_sgc", " SHYD start action KEYCODE_POWER ");		
                    if (isScreenOn && !mPowerKeyTriggered
                            && (event.getFlags() & KeyEvent.FLAG_FALLBACK) == 0) {
                        mPowerKeyTriggered = true;
                        mPowerKeyTime = event.getDownTime();
                        interceptScreenshotChord();
                    }

                    ITelephony telephonyService = getTelephonyService();
                    boolean hungUp = false;
                    if (telephonyService != null) {
                        try {
                            if (telephonyService.isRinging()) {
                                // Pressing Power while there's a ringing incoming
                                // call should silence the ringer.
                                telephonyService.silenceRinger();
                            } else if ((mIncallPowerBehavior
                                    & Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP) != 0
                                    && telephonyService.isOffhook()) {
                                // Otherwise, if "Power button ends call" is enabled,
                                // the Power button will hang up any current active call.
                                hungUp = telephonyService.endCall();
                            }
                        } catch (RemoteException ex) {
                            Log.w(TAG, "ITelephony threw RemoteException", ex);
                        }
                    }
                    Log.d(TAG, "-----------------------------------power down to interceptPowerKeyDown");
                    interceptPowerKeyDown(!isScreenOn || hungUp
                            || mVolumeDownKeyTriggered || mVolumeUpKeyTriggered);
                } else {
                    mPowerKeyTriggered = false;
					Log.i("ysten_sgc", " SHYD start action KEYCODE_POWER boolean is "+interceptPowerKeyUp(canceled || mPendingPowerKeyUpCanceled));
                    cancelPendingScreenshotChordAction();
                    if (interceptPowerKeyUp(canceled || mPendingPowerKeyUpCanceled)) {
                        if (SystemProperties.get("ro.product.target").equals("shcmcc") 
                                    //add by ysten zengzhiliang at 20181109:add m301h_anhui
                                    ||SystemProperties.get("ro.ysten.province","master").equals("m301h_anhui")
									||SystemProperties.get("ro.ysten.province","master").equals("cm201_anhui_iptv")
                                    ||SystemProperties.get("ro.ysten.province","master").equals("cm201_anhui")
                                    ||SystemProperties.get("ro.ysten.province","master").contains("hubei")) {
                            Log.i(TAG,"true no show dialog :"+SystemProperties.get("suspend.from.tv.cec", "false"));
                            if(!SystemProperties.get("suspend.from.tv.cec", "false").equals("true")) {
								if (SystemProperties.get("ro.ysten.province","master").contains("fujian")){
									gotoHome();
                                	Message msg = new Message();
                                	msg.what = 100;
                                	toastHandle.sendMessageDelayed(msg, 20);
								}else{
									//add by sunlei 191127 for 湖北短按powerkey进入待机
                                    Log.d(TAG, "-----------------------------------isHubeiShowDialog = " + isHubeiShowDialog);
									if(SystemProperties.get("ro.ysten.province","master").contains("hubei")){
                                        if(isHubeiShowDialog) {
                                            isHubeiShowDialog = false;
                                            break;
                                        }
                                        Log.d(TAG, "-----------------------------------hubei one press power off");
										showSuspendDialog_HB();
										break;
									//end by sunlei 191127 for 湖北短按powerkey进入待机
									}else{
										showSuspendDialog();
									}
								}
                            } else {
                                swithOffIOAndShutdownLater();
                            }
                        } else if(SystemProperties.get("ro.ysten.province","master").equals("c60_jiangsu")){
                            showSuspendDialog_JS();
						} else if (SystemProperties.get("ro.ysten.province","master").contains("fujian")) {
                            gotoHome();
                            Message msg = new Message();
                            msg.what = 100;
                            toastHandle.sendMessageDelayed(msg, 20);
                        } else {
                          					//add by guangchao.su for the Power Off
                            //result = (result & ~ACTION_WAKE_UP) | ACTION_GO_TO_SLEEP;
							String suspendstr=SystemProperties.get("persist.suspend.mode","smart_suspend");
							String samrtsuspendstr=SystemProperties.get("persist.sys.smartsuspendin","2");
                            if(suspendstr.contains("smart_suspend")&&samrtsuspendstr.contains("1")) {
							SystemProperties.set("persist.sys.smartsuspendin","0");

							String UNIQUE_STRING="android.ysten.systemupdate";
                          Intent intent=new Intent(UNIQUE_STRING);
                          intent.putExtra("powercontrol","poweroff");
                              mContext.sendBroadcast(intent);
							

                            } else {
							Intent intent = new Intent();
                            intent.setComponent(new ComponentName("com.ysten.poweroff", "com.ysten.poweroff.YstPowerOffService"));
					        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mContext.startService(intent);
							}
                        }
                    }
                    HiSysManager mhisys = new HiSysManager();
                    mhisys.setProperty("suspend.from.tv.cec","false");
                    mPendingPowerKeyUpCanceled = false;
                }
                break;
            }

            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                if (down) {
                    ITelephony telephonyService = getTelephonyService();
                    if (telephonyService != null) {
                        try {
                            if (!telephonyService.isIdle()) {
                                // Suppress PLAY/PAUSE toggle when phone is ringing or in-call
                                // to avoid music playback.
                                break;
                            }
                        } catch (RemoteException ex) {
                            Log.w(TAG, "ITelephony threw RemoteException", ex);
                        }
                    }
                }
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MUTE:
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_RECORD:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK: {
                if ((result & ACTION_PASS_TO_USER) == 0) {
                    // Only do this if we would otherwise not pass it to the user. In that
                    // case, the PhoneWindow class will do the same thing, except it will
                    // only do it if the showing app doesn't process the key on its own.
                    // Note that we need to make a copy of the key event here because the
                    // original key event will be recycled when we return.
                    mBroadcastWakeLock.acquire();
                    Message msg = mHandler.obtainMessage(MSG_DISPATCH_MEDIA_KEY_WITH_WAKE_LOCK,
                            new KeyEvent(event));
                    msg.setAsynchronous(true);
                    msg.sendToTarget();
                }
                break;
            }

            case KeyEvent.KEYCODE_CALL: {
                if (down) {
                    ITelephony telephonyService = getTelephonyService();
                    if (telephonyService != null) {
                        try {
                            if (telephonyService.isRinging()) {
                                Log.i(TAG, "interceptKeyBeforeQueueing:"
                                      + " CALL key-down while ringing: Answer the call!");
                                telephonyService.answerRingingCall();

                                // And *don't* pass this key thru to the current activity
                                // (which is presumably the InCallScreen.)
                                result &= ~ACTION_PASS_TO_USER;
                            }
                        } catch (RemoteException ex) {
                            Log.w(TAG, "ITelephony threw RemoteException", ex);
                        }
                    }
                }
                break;
            }
            case KeyEvent.KEYCODE_F5:{
               if(down){
                   Log.i(TAG, "send broadcast com.btdev.action.PARING_REMOTECONTROL");
                   Intent intent = new Intent("com.btdev.action.PARING_REMOTECONTROL");
                   mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT_OR_SELF);
               }
			   if(down){
                   Log.i(TAG, "send broadcast com.ystenbroadcast.voicereg -e action start");
				   Intent intent = new Intent("com.ystenbroadcast.voicereg");		   
				   intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
				   intent.putExtra("action", "start");
				   mContext.sendBroadcast(intent);   
               }else{
                    Log.i(TAG, "send broadcast com.ystenbroadcast.voicereg -e action stop");
				   Intent intent = new Intent("com.ystenbroadcast.voicereg");		   
				   intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
				   intent.putExtra("action", "stop");
				   mContext.sendBroadcast(intent);              
               	}
               break;
           }
           case KeyEvent.KEYCODE_F11:{
                if(!down){
                    if(SystemProperties.get("ro.ysten.province","master").contains("jiangxi")
                        &&keyCode == KeyEvent.KEYCODE_F11){
                        Log.d(TAG,"keycode F11 in jiangxi");
                        try{                                                                                                                                 
                            Process p = Runtime.getRuntime().exec("input keyevent 66");                                                                       
                        }catch(Exception e){                                                                                                          
                            Log.i(TAG, "keycode_enter orror");                                                                                               
                        }
                    }
                }
                break;                
            }
               
		   case KeyEvent.KEYCODE_F12:{
               if(down){
                   Log.i(TAG, "send broadcast com.ystenbroadcast.voicereg -e action start");
				   Intent intent = new Intent("com.ystenbroadcast.voicereg");		   
				   intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
				   intent.putExtra("action", "start");
				   mContext.sendBroadcast(intent);   
               }else{
                    Log.i(TAG, "send broadcast com.ystenbroadcast.voicereg -e action stop");
				   Intent intent = new Intent("com.ystenbroadcast.voicereg");		   
				   intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
				   intent.putExtra("action", "stop");
				   mContext.sendBroadcast(intent);              
               	}
		   		break;
		   	}
           case KeyEvent.KEYCODE_SETTINGS: {
			    
                if(SystemProperties.get("ro.ysten.province","master").equals("cm201_anhui")
                        //add by ysten zengzhiliang at 20181109:add m301h_anhui
                        ||SystemProperties.get("ro.ysten.province","master").equals("m301h_anhui")){
                    break;
                }
                 Log.i(TAG, "111111111111111111111111111111111111 :"+SystemProperties.get("ro.product.target"));
                 Log.i(TAG, "getRepeatCount :"+event.getRepeatCount());
                 if(mLongPressSetting && SystemProperties.get("ro.product.target").equals("shcmcc") ){
                    mLongPressSetting =false;
                   Log.i(TAG, "111111111111111111111111111111111111");
                  }
                 else {
                    if(event.getAction() == KeyEvent.ACTION_UP){
					if (SystemProperties.get("ro.product.target").equals("shcmcc")&& 
					!SystemProperties.get("ro.ysten.province","master").contains("fujian") && 
					!(SystemProperties.get("ro.ysten.province","master").equals("cm201_hubei"))&& 
					!(SystemProperties.get("ro.ysten.province","master").contains("xinjiang"))){
                
                    Log.i(TAG, "start shcmcc setting");
                    Intent intent = new Intent("android.settings.chinamobile.SETTINGS");
                    //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    //intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(intent);
                     } else if (SystemProperties.get("ro.ysten.province","master").contains("fujian")){
						Log.i(TAG, "start fujian setting");						 
						//added by yzs for start Setiing but local play not stop begin at 2020-04-07

						Intent intent2 = new Intent("com.android.ysten_localplayer");
						intent2.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
						mContext.sendBroadcast(intent2);
						
						//added by yzs for start Setiing but local play not stop end
						Intent intent = new Intent();
                        //intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings"));
                        intent.setComponent(new ComponentName("com.ysten.setting", "com.ysten.setting.MainActivity"));
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						mContext.startActivity(intent);
                    } else if (SystemProperties.get("ro.ysten.province","master").contains("cm201_shaanxi")){  
                            Log.i(TAG, "start shaanxi setting");
                            Intent intent = new Intent();
                            //add by zhanghk at 20181214:update setting name
                            intent.setComponent(new ComponentName("com.ysten.setting", "com.ysten.setting.MainActivity"));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mContext.startActivity(intent);
				    }
              //begin: add by ysten zengzhiliang at 20180829 
                    else if (SystemProperties.get("ro.ysten.province","master").contains("cm201_beijing")){  
                            Log.i(TAG, "start beijing setting");
                           // Intent intent = new Intent();
                           // intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.MainActivity_new"));
                           // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                           // mContext.startActivity(intent);
                           //begin: add by ysten zengzhiliang at 20180906
                           if(pkg.equals("com.ysten.setting"))
                            {
                                Log.d(TAG,"current is setting");
                                break;
                            }
                           //end: add by ysten zengzhiliang at 20180906
                            Bundle data = new Bundle();
                            data.putString("action", "settings");
                            //data.putString("data", "");
                            Intent mIntent = new Intent();
                            ComponentName comp = new ComponentName("tv.icntv.vendor","tv.icntv.vendor.Main");
                            mIntent.setComponent(comp);
                            mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mIntent.setAction("android.intent.action.VIEW");
                            mIntent.putExtras(data);
                            mContext.startActivity(mIntent);

				    }
              //end: add by ysten zengzhiliang at 20180829 
                    /*begin:add by zhanghk at 20190306:start hunan setting*/
                    else if (SystemProperties.get("ro.ysten.province","master").contains("cm201_hunan")){ 
                        Log.d(TAG,"start hunan setting");			  
                        Intent intent = new Intent("android.settings.SETTINGS");
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                    }
                    /*end:add by zhanghk at 20190306:start hunan setting*/
                    else if (SystemProperties.get("ro.ysten.province","master").contains("cm201_master")
                    || SystemProperties.get("ro.ysten.province","master").contains("m302h_master")){ 			  
                        Intent intent = new Intent();
                        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings"));
                        //intent.setComponent(new ComponentName("com.ysten.setting", "com.ysten.setting.MainActivity"));
                        intent.putExtra("show_lock_in_setting",true);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                    }
                    else{
			        Intent intent = new Intent();
                	   //intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings"));
                           intent.setComponent(new ComponentName("com.ysten.setting", "com.ysten.setting.MainActivity"));
    	                   intent.putExtra("show_lock_in_setting",true);
		           intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                           mContext.startActivity(intent);
			   
			   Log.i("ysten_sgc", " SHYD start action com.ysten.setting33333 ");
			  }
					//break;
			 }
                       }
			 break;
               }
		  
		  //add by sunlei 191009 for Adaptive SUOI'S remote control{{
		   case KeyEvent.KEYCODE_SUOI_OPEN:  {
		       Log.i(TAG,"sunlei KEYCODE_SUOI_OPEN = "+KeyEvent.KEYCODE_SUOI_OPEN);
			   {
			       Bundle data = new Bundle();
                   data.putString("XY_OPEN", "suoi");
                   Intent mIntent = new Intent();
                   ComponentName comp = new ComponentName("com.ilanchuang.xiaoitv","com.ilanchuang.xiaoitv.activity.LoginActivity");
                   mIntent.setComponent(comp);
                   mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                   mIntent.putExtras(data);
                   mContext.startActivity(mIntent);
			   }
			   break;
		   }
		   
		   case KeyEvent.KEYCODE_TV_OPEN: {
			   if(!down){
		           Intent intent = new Intent();
			       intent.setComponent(new ComponentName("tv.newtv.tvlauncher", "tv.newtv.tvlauncher.MainActivity"));
			       intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			       mContext.startActivity(intent);	
			   }
			   break;
		   }
		   
		   case KeyEvent.KEYCODE_SOS_OPEN: {
		       Log.i(TAG,"sunlei KEYCODE_SOS_OPEN = "+KeyEvent.KEYCODE_SOS_OPEN);
			   if(!down){
			       Bundle data = new Bundle();
                   data.putString("XY_OPEN", "sos");
                   Intent mIntent = new Intent();
                   ComponentName comp = new ComponentName("com.ilanchuang.xiaoitv","com.ilanchuang.xiaoitv.activity.LoginActivity");
                   mIntent.setComponent(comp);
                   mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                   mIntent.putExtras(data);
                   mContext.startActivity(mIntent);
			   }
			   break;
		   }
		   
		    case KeyEvent.KEYCODE_ALBUM_OPEN: {
		       Log.i(TAG,"sunlei KEYCODE_ALBUM_OPEN = "+KeyEvent.KEYCODE_ALBUM_OPEN);
			   if(!down){
			       Bundle data = new Bundle();
                   data.putString("XY_OPEN", "album");
                   Intent mIntent = new Intent();
                   ComponentName comp = new ComponentName("com.ilanchuang.xiaoitv","com.ilanchuang.xiaoitv.activity.LoginActivity");
                   mIntent.setComponent(comp);
                   mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                   mIntent.putExtras(data);
                   mContext.startActivity(mIntent);
			   }
			   break;
		   }
		   
		    case KeyEvent.KEYCODE_CHAT_OPEN: {
		      Log.i(TAG,"sunlei KEYCODE_CHAT_OPEN = "+KeyEvent.KEYCODE_CHAT_OPEN);
			  if(!down){
			      Bundle data = new Bundle();
                  data.putString("XY_OPEN", "chat");
                  Intent mIntent = new Intent();
                  ComponentName comp = new ComponentName("com.ilanchuang.xiaoitv","com.ilanchuang.xiaoitv.activity.LoginActivity");
                  mIntent.setComponent(comp);
                  mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                  mIntent.putExtras(data);
                  mContext.startActivity(mIntent);
			  }
			   break;
		   }
		   
		    case KeyEvent.KEYCODE_FRIEND_OPEN: {
		       Log.i(TAG,"sunlei KEYCODE_FRIEND_OPEN = "+KeyEvent.KEYCODE_FRIEND_OPEN);
			   if(!down){
			       Bundle data = new Bundle();
                   data.putString("XY_OPEN", "friend");
                   Intent mIntent = new Intent();
                   ComponentName comp = new ComponentName("com.ilanchuang.xiaoitv","com.ilanchuang.xiaoitv.activity.LoginActivity");
                   mIntent.setComponent(comp);
                   mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                   mIntent.putExtras(data);
                   mContext.startActivity(mIntent);
			   }
			   break;
		   }
		   
		    case KeyEvent.KEYCODE_HEALTH_OPEN: {
		       Log.i(TAG,"sunlei KEYCODE_HEALTH_OPEN = "+KeyEvent.KEYCODE_HEALTH_OPEN);
			   if(!down){
			       Bundle data = new Bundle();
                   data.putString("XY_OPEN", "health");
                   Intent mIntent = new Intent();
                   ComponentName comp = new ComponentName("com.ilanchuang.xiaoitv","com.ilanchuang.xiaoitv.activity.LoginActivity");
                   mIntent.setComponent(comp);
                   mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                   mIntent.putExtras(data);
                   mContext.startActivity(mIntent);
			   }
			   break;
		   }
		   
		    case KeyEvent.KEYCODE_CHILD_OPEN: {
		      Log.i(TAG,"sunlei KEYCODE_CHILD_OPEN = "+KeyEvent.KEYCODE_CHILD_OPEN);
			  if(!down){
			      Bundle data = new Bundle();
                  data.putString("XY_OPEN", "child");
                  Intent mIntent = new Intent();
                  ComponentName comp = new ComponentName("com.ilanchuang.xiaoitv","com.ilanchuang.xiaoitv.activity.LoginActivity");
                  mIntent.setComponent(comp);
                  mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                  mIntent.putExtras(data);
                  mContext.startActivity(mIntent);
			  }
			   break;
		   }
		   
		    case KeyEvent.KEYCODE_MALL_OPEN: {
		      Log.i(TAG,"sunlei KEYCODE_MALL_OPEN = "+KeyEvent.KEYCODE_MALL_OPEN);
			  if(!down){
			      Bundle data = new Bundle();
                  data.putString("XY_OPEN", "mall");
                  Intent mIntent = new Intent();
                  ComponentName comp = new ComponentName("com.ilanchuang.xiaoitv","com.ilanchuang.xiaoitv.activity.LoginActivity");
                  mIntent.setComponent(comp);
                  mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                  mIntent.putExtras(data);
                  mContext.startActivity(mIntent);
			  }
			   break;
		   }
		   
		    case KeyEvent.KEYCODE_ENT_OPEN: {
		       Log.i(TAG,"sunlei KEYCODE_ENT_OPEN = "+KeyEvent.KEYCODE_ENT_OPEN);
			   if(!down){
			       Bundle data = new Bundle();
                   data.putString("XY_OPEN", "ent");
                   Intent mIntent = new Intent();
                   ComponentName comp = new ComponentName("com.ilanchuang.xiaoitv","com.ilanchuang.xiaoitv.activity.LoginActivity");
                   mIntent.setComponent(comp);
                   mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                   mIntent.putExtras(data);
                   mContext.startActivity(mIntent);
			   }
			   break;
		   }
		   
		    case KeyEvent.KEYCODE_EXPERT_OPEN: {
		       Log.i(TAG,"sunlei KEYCODE_EXPERT_OPEN = "+KeyEvent.KEYCODE_EXPERT_OPEN);
			   if(!down){
			       Bundle data = new Bundle();
                   data.putString("XY_OPEN", "expert");
                   Intent mIntent = new Intent();
                   ComponentName comp = new ComponentName("com.ilanchuang.xiaoitv","com.ilanchuang.xiaoitv.activity.LoginActivity");
                   mIntent.setComponent(comp);
                   mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                   mIntent.putExtras(data);
                   mContext.startActivity(mIntent);
			   }
			   break;
		   }
		   
		    case KeyEvent.KEYCODE_WEATHER_OPEN: {
		       Log.i(TAG,"sunlei KEYCODE_WEATHER_OPEN = "+KeyEvent.KEYCODE_WEATHER_OPEN);
			   if(!down){
			       Bundle data = new Bundle();
                   data.putString("XY_OPEN", "weather");
                   Intent mIntent = new Intent();
                   ComponentName comp = new ComponentName("com.ilanchuang.xiaoitv","com.ilanchuang.xiaoitv.activity.LoginActivity");
                   mIntent.setComponent(comp);
                   mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                   mIntent.putExtras(data);
                   mContext.startActivity(mIntent);
			   }
			   break;
		   }
		   
		    case KeyEvent.KEYCODE_NOTICE_OPEN: {
		       Log.i(TAG,"sunlei KEYCODE_NOTICE_OPEN = "+KeyEvent.KEYCODE_NOTICE_OPEN);
			   if(!down){
			       Bundle data = new Bundle();
                   data.putString("XY_OPEN", "notice");
                   Intent mIntent = new Intent();
                   ComponentName comp = new ComponentName("com.ilanchuang.xiaoitv","com.ilanchuang.xiaoitv.activity.LoginActivity");
                   mIntent.setComponent(comp);
                   mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                   mIntent.putExtras(data);
                   mContext.startActivity(mIntent);
			   }
			   break;
		   }
		   
		   case KeyEvent.KEYCODE_HP_OPEN: {
		       Log.i(TAG,"sunlei KEYCODE_HP_OPEN = "+KeyEvent.KEYCODE_HP_OPEN);
			   if(!down){
			       Bundle data = new Bundle();
                   data.putString("XY_OPEN", "suoi");
                   Intent mIntent = new Intent();
                   ComponentName comp = new ComponentName("com.ilanchuang.xiaoitv","com.ilanchuang.xiaoitv.activity.LoginActivity");
                   mIntent.setComponent(comp);
                   mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                   mIntent.putExtras(data);
                   mContext.startActivity(mIntent);
			   }
			   break;
		   }
		  //end by sunlei 191009}}
		  
          // add by YSTen {{
           case KeyEvent.KEYCODE_TV: 
           case KeyEvent.KEYCODE_MOVIE:
           case KeyEvent.KEYCODE_FAVOURITE:
		   //begin: add by ysten hyz at 20190311: remove keyevent down of KEYCODE_MOVIE of xinjiang
		   if(SystemProperties.get("ro.ysten.province","master").contains("xinjiang")){{
					
					
			       if(event.getAction() == KeyEvent.ACTION_DOWN) {
					   
			             return 0;
				      }
				    }
               	}
		   //end: add by ysten hyz at 20190311: remove keyevent down of KEYCODE_MOVIE of xinjiang 
           case KeyEvent.KEYCODE_APPS:
           case KeyEvent.KEYCODE_XIRISEARCH:
		   case KeyEvent.KEYCODE_XIRIRECORD:
 	       case KeyEvent.KEYCODE_DIAL:
    	   case KeyEvent.KEYCODE_CHILD:
		   case KeyEvent.KEYCODE_EDUCATION:
		   case KeyEvent.KEYCODE_XIRIMUSIC:
	       case KeyEvent.KEYCODE_PE:
	       case KeyEvent.KEYCODE_GAME:
	       case KeyEvent.KEYCODE_APPLIANCE:
	       case KeyEvent.KEYCODE_XIRIRED:
	       case KeyEvent.KEYCODE_XIRIGREEN:
    	   case KeyEvent.KEYCODE_XIRIBLUE:
		   case KeyEvent.KEYCODE_F4:
		   case KeyEvent.KEYCODE_F1:
		     			   
               if(event.getAction() == KeyEvent.ACTION_UP) {
               if(!SystemProperties.get("sys.key.home").equals("on")&&Settings.System.getInt(mContext.getContentResolver(), "IS_RESPOND_HOME_KEY", 0)==0
			   &&SystemProperties.get("ro.ysten.province","master").contains("sichuan")){
                doFunAction(keyCode); 
               }                  
               }
               if(SystemProperties.get("ro.ysten.province","master").contains("fujian")){
			   //start by lizheng 20190408 to solve four color key bug
				boolean launcherStarted=SystemProperties.getBoolean("persist.sys.launcher.started",true);
				if (!launcherStarted)
                {
				break;
                }
				//end by lizheng 20190408 to solve four color key bug
				   //begin by ysten.lizheng,20181012,fix four color key bug
			       if(event.getAction() == KeyEvent.ACTION_UP) {
			   if (SystemProperties.getInt("persist.sys.launcher.value",0)==2){
				   doIPTVFuncationFJ(keyCode);
			   }else if(SystemProperties.getInt("persist.sys.launcher.value",0)==1){
				   String authStatus=getValueFromStb("authStatus")+"";
		           String user_token=getValueFromStb("user_token")+"";
		           Log.d(TAG,"authStatus="+authStatus+","+"user_token="+user_token);
		           if(authStatus.equals("AuthSuccess")&&!user_token.equals("")){
				   doFunActionFJ(keyCode);
                        }else{
							doFunctionLogin(keyCode);
				   }
                    }
                }
				   //end by ysten.lizheng,20181012,fix four color key bug
               	}
				
				//begin add by hyz at 20190311:resolve bug of four color key of xinjiang 
				if(SystemProperties.get("ro.ysten.province","master").contains("xinjiang")){
					if(event.getAction() == KeyEvent.ACTION_UP) {
						int ret = doIPTVFuncationXJ(keyCode);
						if(ret > 0 && keyCode == KeyEvent.KEYCODE_FAVOURITE){
							Log.d("ysten_cm201", " use this KEYCODE_FAVOURITE ");
							return 0;
						}
					}
               	}
				//begin add by hyz at 20190311:resolve bug of four color key of xinjiang
								
               if(SystemProperties.get("ro.ysten.province","master").equals("cm201_anhui_iptv")
                   /*add by zhaolianghua for AH ott fourcolor key @20200319*/
                   || SystemProperties.get("ro.ysten.province","master").equals("cm201_anhui")){
                   doIPTVFunctionAH(keyCode);
               }
		//add by guangchao.su ad 20180811:add jiangxi iptv home
		if(event.getAction() == KeyEvent.ACTION_DOWN){
			if(SystemProperties.get("ro.ysten.province","master").contains("jiangxi") 
				&& SystemProperties.get("persist.sys.launcher.value","1").equals("2")){         
                     //KeyEvent.KEYCODE_XIRIBLUE conflict KeyEvent.KEYCODE_FACOURITE
                    doIPTVFuncationJX(keyCode);
               	}else if(SystemProperties.get("ro.ysten.province","master").contains("jiangxi")){
                    doFunActionJX(keyCode);
                    Log.w(TAG, " use this sisejian");
               	}
            //begin:add by zhuhengxuan for four color key at 20200629 BUGID:7926
            if(SystemProperties.get("ro.ysten.province","master").equals("c60_jiangsu")){
                doJSFunAction(keyCode);
            }
            //end:add by zhuhengxuan for four color key at 20200629 BUGID:7926
		}
		//add by guangchao.su ad 20180828:add jiangxi sisejian
              if(SystemProperties.get("ro.ysten.province","master").equals("c60_chongqing")){
               	doFunAction(keyCode);
               	}
	      //begin:add by liangk at 20200302: add four color key for heilongj
              if(SystemProperties.get("ro.ysten.province","master").equals("cm201_heilongjiang")){
                       doFunAction(keyCode);
                }
	      //end:add by liangk at 20200302: add four color key for heilongj
              if(SystemProperties.get("ro.ysten.province","master").equals("cm201_guangdong")){
                 Log.i("zjj", "zjj fujian setting"+keyCode); 
                 doGDFunAction(keyCode);
                }
			  //begin:add by xumiao at 20190725 add color key
              if(SystemProperties.get("ro.ysten.province","master").contains("cm201_jilin")){
                 Log.i("cm201_jilin", "color key"+keyCode); 
				 String packeageJL=getTopActivity(mContext);
                 if(!packeageJL.equals("com.ysten.auth")){
                    doJiLinFunAction(keyCode);
                  }
                }
			  //end:add by xumiao at 20190725 add color key
              //begin:add by xuyunfeng at 20200403 add color key
              if(SystemProperties.get("ro.ysten.province","master").contains("CM201-2_shanxi")){
                  Log.i("CM201-2_shanxi", "color key"+keyCode);
                  doShanXiIptvFunAction(keyCode);
              }
              //end:add by xuyunfeng at 20200403 add color key
              //begin: add by ysten zengzhiliang at 20180829 add APP TV MOVIE Collect key
                if(SystemProperties.get("ro.ysten.province","master").contains("c60_heilongjiang")
                        ||SystemProperties.get("ro.ysten.province","master").equals("cm201_beijing")){
				//begin by wcy solve hlj F1
					if(event.getAction() == KeyEvent.ACTION_UP)
               	doFunAction(keyCode);
			        result &= ~ACTION_PASS_TO_USER;
				//end by wcy solve hlj F1
               	}
              //end: add by ysten zengzhiliang at 20180829 add APP TV MOVIE Collect key
              if(event.getAction() == KeyEvent.ACTION_UP) {
                if(SystemProperties.get("ro.ysten.province","master").equals("cm201_yunnan")||
                		SystemProperties.get("ro.ysten.province","master").equals("c60_yunnan")
                        ||SystemProperties.get("ro.ysten.province","master").equals("m301h_yunnan")){
               	doFunAction_cm201_yunnan(keyCode);
               	}
				if(SystemProperties.get("ro.ysten.province","master").equals("cm201_yunnan_iptv")){
				doFunAction_cm201_yunnan_iptv(keyCode);	
				}
               }
			  //begin by ysten.zhangjunjian,for guizhou four color key
                if(event.getAction() == KeyEvent.ACTION_UP) {			   
                  if(SystemProperties.get("ro.ysten.province","master").equals("cm201_guizhou")){
                    doIPTVFuncationGZ(keyCode);
                    }
                }
                //end by ysten.zhangjunjian,for guizhou four color key
				//add by xue for futuretv
				if(SystemProperties.get("ro.ftserialno","master").equals("ysten"))
				{
				doFTmasterFunAction(keyCode);
				}
				//add by xue for futuretv
			   
               if (SystemProperties.get("ro.ysten.province","master").equals("cm201_hubei")) {
                   String bussiness = getBussinessPlatform(mContext);
                   Log.d(TAG, "bussiness:" + bussiness);
                   if(TextUtils.isEmpty(bussiness)){
                       bussiness = SystemProperties.get("persist.sys.hb.bussiness", "");
			           Log.d(TAG, "get persist bussiness:" + bussiness);
                   }
                   if(!TextUtils.isEmpty(bussiness) && !bussiness.equalsIgnoreCase("iptv")) {
                       if(event.getAction() == KeyEvent.ACTION_UP) {
                           if(!SystemProperties.get("sys.key.home").equals("on")&&Settings.System.getInt(mContext.getContentResolver(), "IS_RESPOND_HOME_KEY", 0)==0){
                               doFunActionHB(keyCode); 
                           } else {
			       Log.d(TAG, "doFunActionHB else");
		           }		   
                       }
	               } else {
                       if(event.getAction() == KeyEvent.ACTION_UP) {
                           if(!SystemProperties.get("sys.key.home").equals("on")&&Settings.System.getInt(mContext.getContentResolver(), "IS_RESPOND_HOME_KEY", 0)==0){
                               doIPTVFuncationHB(keyCode); 
                           } else {
			       Log.d(TAG, "doIPTVFunActionHB else");
		           }		   
                       }				   
				   }
			   }
			   
               break;
           // add by YSTen }}
        }
        return result;
    }

    //begin:add ysten liangkai at 20201023 : 凌晨待机
    TimerTask task = new TimerTask(){
	    @Override
	    public void run() {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date curDate = new Date(System.currentTimeMillis());
            long mCurDateTime = (curDate.getTime())/1000;
            long mClockTime = strToDateLong(formatter.format(curDate) + " 03:00:00");
            if(mClockTime>mCurDateTime){
                mClockTime = mClockTime-mCurDateTime;
            } else {
                mClockTime = 24*60*60-(mCurDateTime - mClockTime);
            }
            Log.d("ystenlk","mClockTime = " + mClockTime);
            mHandler.removeCallbacks(mSystemSleep);
            mHandler.postDelayed(mSystemSleep,mClockTime*1000);
	    }
    };

    public static long strToDateLong(String strDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    Date curDate =  new Date(System.currentTimeMillis());
	    String stringDate =   formatter.format(curDate);  
	    Log.d("ystenlk","time = " + curDate.getTime() + "; strTime = " + stringDate);
        ParsePosition pos = new ParsePosition(0);
        Date strtodate = formatter.parse(strDate, pos);
	    Log.d("ystenlk","time = " + strtodate.getTime());
        return (long)(strtodate.getTime()/1000);
    }
    //end:add ysten liangkai at 20201023 : 凌晨待机
	
	//begin add by ysten hyz at 20190311：add method of four color key of xinjiang down 
	private int doIPTVFuncationXJ(int keyCode){
		
		Intent mIntent = new Intent();		
		
		int IPTVKey=0;
		switch (keyCode) {
		case KeyEvent.KEYCODE_HOME:		
//                ComponentName compNameHOME= new ComponentName("com.zte.iptvclient.android.launcher_xj","com.zte.iptvclient.android.launcher.main");
//				mIntent.setComponent(compNameHOME);
//                mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
//			    mContext.startActivityAsUser(mIntent, UserHandle.CURRENT);
				String topPackage = getTopActivity(mContext);
				Log.d(TAG,"xinjiang topPackage = "+topPackage);
				if(!TextUtils.isEmpty(topPackage)&&!topPackage.equals("com.android.smart.terminal.iptv")) {
					mIntent = new Intent();
					mIntent.setAction("com.android.smart.terminal.iptv");
					mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
					mIntent.putExtra("intentMsg", "EPGDomain");

					mContext.startActivity(mIntent);
					return -1;
				}else{
					IPTVKey= 82;//181;
				}
				break;
	    case KeyEvent.KEYCODE_MENU:		
                //ComponentName compNameMENU= new ComponentName("com.android.smart.terminal.iptv","com.amt.app.IPTVActivity");
				//mIntent.setComponent(compNameMENU);
                //mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			    //mContext.startActivityAsUser(mIntent, UserHandle.CURRENT);
				
				return -1;
		case KeyEvent.KEYCODE_TV:
                		
                IPTVKey=136;
                break;
		case KeyEvent.KEYCODE_MOVIE:	
                		
                IPTVKey=137;
                break;
		case KeyEvent.KEYCODE_FAVOURITE:		
                IPTVKey=139;
				
                break;
		case KeyEvent.KEYCODE_APPS:		
                IPTVKey=138;
				
                break;
		case KeyEvent.KEYCODE_XIRIRED:		
                IPTVKey=183;
				
                break;
		case KeyEvent.KEYCODE_XIRIGREEN:		
                IPTVKey=185;
				
                break;
		case KeyEvent.KEYCODE_XIRIBLUE:		
                IPTVKey=186;
				
                break;
        case KeyEvent.KEYCODE_VOLUME_MUTE:
                IPTVKey=KeyEvent.KEYCODE_MUTE;
				
				break;
		case KeyEvent.KEYCODE_F4:		
                IPTVKey=KeyEvent.KEYCODE_STAR;
				
                break;
		case KeyEvent.KEYCODE_F1:
                IPTVKey=KeyEvent.KEYCODE_POUND;
				
                break;
		}
		Log.i(TAG,"IPTVKey is "+IPTVKey);
		if (IPTVKey!=0){
			long now = SystemClock.uptimeMillis();
			KeyEvent keyEvent = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, IPTVKey, 0);
			InputManager inputManager = (InputManager) mContext.getSystemService(Context.INPUT_SERVICE);
			inputManager.injectInputEvent(keyEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
		}
		return IPTVKey;
	}
	//end  add by ysten hyz at 20190311：add method of four color key of xinjiang down 
	

	//add by xue for futureTV
    private void doFTmasterFunAction(int keyCode) {
        Intent funIntent = new Intent();
        String funAction = null;
        String funActionKey = null;

        switch (keyCode) {
            case KeyEvent.KEYCODE_TV:
                funActionKey = "live";
                break;
            case KeyEvent.KEYCODE_MOVIE:
                funActionKey = "vod";
                break;
            case KeyEvent.KEYCODE_APPS:
                funActionKey = "app"; // not used,because bestv not support
                break;
            case KeyEvent.KEYCODE_FAVOURITE:
                funActionKey = "collection";
                break;
        }

        funAction = "android.ysten.systemupdate";
        funIntent.setAction(funAction);
        funIntent.putExtra("keysp", funActionKey);
		
        mContext.sendBroadcast(funIntent);
    }
//add by xue for futureTV

    private void doGZFunAction(int keyCode) {
        Intent funIntent = new Intent();
        String funAction = null;
        String funActionKey = null;

        switch (keyCode) {
            case KeyEvent.KEYCODE_TV:
                funActionKey = "LIVE_CHANNEL_LIST";
                break;
            case KeyEvent.KEYCODE_MOVIE:
                funActionKey = "VOD_CATEGORY_PAGE";
                break;
            case KeyEvent.KEYCODE_APPS:
                funActionKey = "TVOD_CHANNEL_LIST"; // not used,because bestv not support
                break;
            case KeyEvent.KEYCODE_FAVOURITE:
                funActionKey = "Infomation";
                break;
            case KeyEvent.KEYCODE_HOME:
                funActionKey = "EPGDomain";
                Log.d("zhanghk","doFunAction");
                break;
        }

        funAction = "com.android.smart.terminal.iptv";
        funIntent.setAction(funAction);
        funIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        funIntent.putExtra("intentMsg", funActionKey);
        mContext.startActivity(funIntent);
    }
    // add by YSTen }}
    /**
     * When the screen is off we ignore some keys that might otherwise typically
     * be considered wake keys.  We filter them out here.
     *
     * {@link KeyEvent#KEYCODE_POWER} is notably absent from this list because it
     * is always considered a wake key.
     */
    private boolean isWakeKeyWhenScreenOff(int keyCode) {
        switch (keyCode) {
            // ignore volume keys unless docked
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                return mDockMode != Intent.EXTRA_DOCK_STATE_UNDOCKED;

            // ignore media and camera keys
            case KeyEvent.KEYCODE_MUTE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_RECORD:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK:
            case KeyEvent.KEYCODE_CAMERA:
                return false;
        }
        return true;
    }


    /** {@inheritDoc} */
    @Override
    public int interceptMotionBeforeQueueingWhenScreenOff(int policyFlags) {
        int result = 0;

        final boolean isWakeMotion = (policyFlags
                & (WindowManagerPolicy.FLAG_WAKE | WindowManagerPolicy.FLAG_WAKE_DROPPED)) != 0;
        if (isWakeMotion) {
            //result |= ACTION_WAKE_UP;
        }
        return result;
    }

    void dispatchMediaKeyWithWakeLock(KeyEvent event) {
        if (DEBUG_INPUT) {
            Slog.d(TAG, "dispatchMediaKeyWithWakeLock: " + event);
        }

        if (mHavePendingMediaKeyRepeatWithWakeLock) {
            if (DEBUG_INPUT) {
                Slog.d(TAG, "dispatchMediaKeyWithWakeLock: canceled repeat");
            }

            mHandler.removeMessages(MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK);
            mHavePendingMediaKeyRepeatWithWakeLock = false;
            mBroadcastWakeLock.release(); // pending repeat was holding onto the wake lock
        }

        dispatchMediaKeyWithWakeLockToAudioService(event);

        if (event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0) {
            mHavePendingMediaKeyRepeatWithWakeLock = true;

            Message msg = mHandler.obtainMessage(
                    MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK, event);
            msg.setAsynchronous(true);
            mHandler.sendMessageDelayed(msg, ViewConfiguration.getKeyRepeatTimeout());
        } else {
            mBroadcastWakeLock.release();
        }
    }

    void dispatchMediaKeyRepeatWithWakeLock(KeyEvent event) {
        mHavePendingMediaKeyRepeatWithWakeLock = false;

        KeyEvent repeatEvent = KeyEvent.changeTimeRepeat(event,
                SystemClock.uptimeMillis(), 1, event.getFlags() | KeyEvent.FLAG_LONG_PRESS);
        if (DEBUG_INPUT) {
            Slog.d(TAG, "dispatchMediaKeyRepeatWithWakeLock: " + repeatEvent);
        }

        dispatchMediaKeyWithWakeLockToAudioService(repeatEvent);
        mBroadcastWakeLock.release();
    }

    void dispatchMediaKeyWithWakeLockToAudioService(KeyEvent event) {
        if (ActivityManagerNative.isSystemReady()) {
            IAudioService audioService = getAudioService();
            if (audioService != null) {
                try {
                    audioService.dispatchMediaKeyEventUnderWakelock(event);
                } catch (RemoteException e) {
                    Log.e(TAG, "dispatchMediaKeyEvent threw exception " + e);
                }
            }
        }
    }

    BroadcastReceiver mDockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_DOCK_EVENT.equals(intent.getAction())) {
                mDockMode = intent.getIntExtra(Intent.EXTRA_DOCK_STATE,
                        Intent.EXTRA_DOCK_STATE_UNDOCKED);
            } else {
                try {
                    IUiModeManager uiModeService = IUiModeManager.Stub.asInterface(
                            ServiceManager.getService(Context.UI_MODE_SERVICE));
                    mUiMode = uiModeService.getCurrentModeType();
                } catch (RemoteException e) {
                }
            }
            updateRotation(true);
            synchronized (mLock) {
                updateOrientationListenerLp();
            }
        }
    };

    BroadcastReceiver mDreamReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_DREAMING_STARTED.equals(intent.getAction())) {
                if (mKeyguardDelegate != null) {
                    mKeyguardDelegate.onDreamingStarted();
                }
            } else if (Intent.ACTION_DREAMING_STOPPED.equals(intent.getAction())) {
                if (mKeyguardDelegate != null) {
                    mKeyguardDelegate.onDreamingStopped();
                }
            }
        }
    };

    BroadcastReceiver mMultiuserReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_USER_SWITCHED.equals(intent.getAction())) {
                // tickle the settings observer: this first ensures that we're
                // observing the relevant settings for the newly-active user,
                // and then updates our own bookkeeping based on the now-
                // current user.
                mSettingsObserver.onChange(false);

                // force a re-application of focused window sysui visibility.
                // the window may never have been shown for this user
                // e.g. the keyguard when going through the new-user setup flow
                synchronized (mWindowManagerFuncs.getWindowManagerLock()) {
                    mLastSystemUiFlags = 0;
                    updateSystemUiVisibilityLw();
                }
            }
        }
    };

    //HISILICON add begin
	long bootCompleteTime;
    BroadcastReceiver mBootCompletedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                mBootCompleted = true;
				//start edit by lizheng 20190304 to solve fujian power key bug
				bootCompleteTime=SystemClock.uptimeMillis();
				//end edit by lizheng 20190304 to solve fujian power key bug
            }
        }
    };
    //HISILICON add end

    private void requestTransientBars(WindowState swipeTarget) {
        synchronized (mWindowManagerFuncs.getWindowManagerLock()) {
            boolean sb = mStatusBarController.checkShowTransientBarLw();
            boolean nb = mNavigationBarController.checkShowTransientBarLw();
            if (sb || nb) {
                WindowState barTarget = sb ? mStatusBar : mNavigationBar;
                if (sb ^ nb && barTarget != swipeTarget) {
                    if (DEBUG) Slog.d(TAG, "Not showing transient bar, wrong swipe target");
                    return;
                }
                if (sb) mStatusBarController.showTransient();
                if (nb) mNavigationBarController.showTransient();
                mImmersiveModeConfirmation.confirmCurrentPrompt();
                updateSystemUiVisibilityLw();
            }
        }
    }

    @Override
    public void screenTurnedOff(int why) {
        EventLog.writeEvent(70000, 0);
        synchronized (mLock) {
            mScreenOnEarly = false;
            mScreenOnFully = false;
        }
        if (mKeyguardDelegate != null) {
            mKeyguardDelegate.onScreenTurnedOff(why);
        }
        synchronized (mLock) {
            updateOrientationListenerLp();
            updateLockScreenTimeout();
        }
    }

    @Override
    public void screenTurningOn(final ScreenOnListener screenOnListener) {
        EventLog.writeEvent(70000, 1);
        if (false) {
            RuntimeException here = new RuntimeException("here");
            here.fillInStackTrace();
            Slog.i(TAG, "Screen turning on...", here);
        }

        synchronized (mLock) {
            mScreenOnEarly = true;
            updateOrientationListenerLp();
            updateLockScreenTimeout();
        }

        waitForKeyguard(screenOnListener);
    }

    private void waitForKeyguard(final ScreenOnListener screenOnListener) {
        if (mKeyguardDelegate != null) {
            if (screenOnListener != null) {
                mKeyguardDelegate.onScreenTurnedOn(new KeyguardServiceDelegate.ShowListener() {
                    @Override
                    public void onShown(IBinder windowToken) {
                        waitForKeyguardWindowDrawn(windowToken, screenOnListener);
                    }
                });
                return;
            } else {
                mKeyguardDelegate.onScreenTurnedOn(null);
            }
        } else {
            Slog.i(TAG, "No keyguard interface!");
        }
        finishScreenTurningOn(screenOnListener);
    }

    private void waitForKeyguardWindowDrawn(IBinder windowToken,
            final ScreenOnListener screenOnListener) {
        if (windowToken != null && !mHideLockScreen) {
            try {
                if (mWindowManager.waitForWindowDrawn(
                        windowToken, new IRemoteCallback.Stub() {
                    @Override
                    public void sendResult(Bundle data) {
                        Slog.i(TAG, "Lock screen displayed!");
                        finishScreenTurningOn(screenOnListener);
                    }
                })) {
                    return;
                }
                Slog.i(TAG, "No lock screen! waitForWindowDrawn false");

            } catch (RemoteException ex) {
                // Can't happen in system process.
            }
        }

        Slog.i(TAG, "No lock screen! windowToken=" + windowToken);
        finishScreenTurningOn(screenOnListener);
    }

    private void finishScreenTurningOn(ScreenOnListener screenOnListener) {
        synchronized (mLock) {
            mScreenOnFully = true;
        }

        try {
            mWindowManager.setEventDispatching(true);
        } catch (RemoteException unhandled) {
        }

        if (screenOnListener != null) {
            screenOnListener.onScreenOn();
        }
    }

    @Override
    public boolean isScreenOnEarly() {
        return mScreenOnEarly;
    }

    @Override
    public boolean isScreenOnFully() {
        return mScreenOnFully;
    }

    /** {@inheritDoc} */
    public void enableKeyguard(boolean enabled) {
        if (mKeyguardDelegate != null) {
            mKeyguardDelegate.setKeyguardEnabled(enabled);
        }
    }

    /** {@inheritDoc} */
    public void exitKeyguardSecurely(OnKeyguardExitResult callback) {
        if (mKeyguardDelegate != null) {
            mKeyguardDelegate.verifyUnlock(callback);
        }
    }

    private boolean keyguardIsShowingTq() {
        if (mKeyguardDelegate == null) return false;
        return mKeyguardDelegate.isShowingAndNotHidden();
    }


    /** {@inheritDoc} */
    public boolean isKeyguardLocked() {
        return keyguardOn();
    }

    /** {@inheritDoc} */
    public boolean isKeyguardSecure() {
        if (mKeyguardDelegate == null) return false;
        return mKeyguardDelegate.isSecure();
    }

    /** {@inheritDoc} */
    public boolean inKeyguardRestrictedKeyInputMode() {
        if (mKeyguardDelegate == null) return false;
        return mKeyguardDelegate.isInputRestricted();
    }

    public void dismissKeyguardLw() {
        if (mKeyguardDelegate != null && mKeyguardDelegate.isShowing()) { 
            mHandler.post(new Runnable() {
                public void run() {
                    if (mKeyguardDelegate.isDismissable()) {
                        // Can we just finish the keyguard straight away?
                        mKeyguardDelegate.keyguardDone(false, true);
                    } else {
                        // ask the keyguard to prompt the user to authenticate if necessary
                        mKeyguardDelegate.dismiss();
                    }
                }
            });
        }
    }

    void sendCloseSystemWindows() {
        sendCloseSystemWindows(mContext, null);
    }

    void sendCloseSystemWindows(String reason) {
        sendCloseSystemWindows(mContext, reason);
    }

    static void sendCloseSystemWindows(Context context, String reason) {
        if (ActivityManagerNative.isSystemReady()) {
            try {
                ActivityManagerNative.getDefault().closeSystemDialogs(reason);
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    public int rotationForOrientationLw(int orientation, int lastRotation) {
        if (false) {
            Slog.v(TAG, "rotationForOrientationLw(orient="
                        + orientation + ", last=" + lastRotation
                        + "); user=" + mUserRotation + " "
                        + ((mUserRotationMode == WindowManagerPolicy.USER_ROTATION_LOCKED)
                            ? "USER_ROTATION_LOCKED" : "")
                        );
        }

        if (mForceDefaultOrientation) {
            return Surface.ROTATION_0;
        }

        synchronized (mLock) {
            int sensorRotation = mOrientationListener.getProposedRotation(); // may be -1
            if (sensorRotation < 0) {
                sensorRotation = lastRotation;
            }

            final int preferredRotation;
            if (mLidState == LID_OPEN && mLidOpenRotation >= 0) {
                // Ignore sensor when lid switch is open and rotation is forced.
                preferredRotation = mLidOpenRotation;
            } else if (mDockMode == Intent.EXTRA_DOCK_STATE_CAR
                    && (mCarDockEnablesAccelerometer || mCarDockRotation >= 0)) {
                // Ignore sensor when in car dock unless explicitly enabled.
                // This case can override the behavior of NOSENSOR, and can also
                // enable 180 degree rotation while docked.
                preferredRotation = mCarDockEnablesAccelerometer
                        ? sensorRotation : mCarDockRotation;
            } else if ((mDockMode == Intent.EXTRA_DOCK_STATE_DESK
                    || mDockMode == Intent.EXTRA_DOCK_STATE_LE_DESK
                    || mDockMode == Intent.EXTRA_DOCK_STATE_HE_DESK)
                    && (mDeskDockEnablesAccelerometer || mDeskDockRotation >= 0)) {
                // Ignore sensor when in desk dock unless explicitly enabled.
                // This case can override the behavior of NOSENSOR, and can also
                // enable 180 degree rotation while docked.
                preferredRotation = mDeskDockEnablesAccelerometer
                        ? sensorRotation : mDeskDockRotation;
            } else if (mHdmiPlugged && mDemoHdmiRotationLock) {
                // Ignore sensor when plugged into HDMI when demo HDMI rotation lock enabled.
                // Note that the dock orientation overrides the HDMI orientation.
                preferredRotation = mDemoHdmiRotation;
            } else if (mHdmiPlugged && mDockMode == Intent.EXTRA_DOCK_STATE_UNDOCKED
                    && mUndockedHdmiRotation >= 0) {
                // Ignore sensor when plugged into HDMI and an undocked orientation has
                // been specified in the configuration (only for legacy devices without
                // full multi-display support).
                // Note that the dock orientation overrides the HDMI orientation.
                preferredRotation = mUndockedHdmiRotation;
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_LOCKED) {
                // Application just wants to remain locked in the last rotation.
                preferredRotation = lastRotation;
            } else if ((mUserRotationMode == WindowManagerPolicy.USER_ROTATION_FREE
                            && (orientation == ActivityInfo.SCREEN_ORIENTATION_USER
                                    || orientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    || orientation == ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
                                    || orientation == ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
                                    || orientation == ActivityInfo.SCREEN_ORIENTATION_FULL_USER))
                    || orientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR
                    || orientation == ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                    || orientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    || orientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT) {
                // Otherwise, use sensor only if requested by the application or enabled
                // by default for USER or UNSPECIFIED modes.  Does not apply to NOSENSOR.
                if (mAllowAllRotations < 0) {
                    // Can't read this during init() because the context doesn't
                    // have display metrics at that time so we cannot determine
                    // tablet vs. phone then.
                    mAllowAllRotations = mContext.getResources().getBoolean(
                            com.android.internal.R.bool.config_allowAllRotations) ? 1 : 0;
                }
                if (sensorRotation != Surface.ROTATION_180
                        || mAllowAllRotations == 1
                        || orientation == ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                        || orientation == ActivityInfo.SCREEN_ORIENTATION_FULL_USER) {
                    preferredRotation = sensorRotation;
                } else {
                    preferredRotation = lastRotation;
                }
            } else if (mUserRotationMode == WindowManagerPolicy.USER_ROTATION_LOCKED
                    && orientation != ActivityInfo.SCREEN_ORIENTATION_NOSENSOR) {
                // Apply rotation lock.  Does not apply to NOSENSOR.
                // The idea is that the user rotation expresses a weak preference for the direction
                // of gravity and as NOSENSOR is never affected by gravity, then neither should
                // NOSENSOR be affected by rotation lock (although it will be affected by docks).
                preferredRotation = mUserRotation;
            } else {
                // No overriding preference.
                // We will do exactly what the application asked us to do.
                preferredRotation = -1;
            }

            switch (orientation) {
                case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                    // Return portrait unless overridden.
                    if (isAnyPortrait(preferredRotation)) {
                        return preferredRotation;
                    }
                    return mPortraitRotation;

                case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                    // Return landscape unless overridden.
                    if (isLandscapeOrSeascape(preferredRotation)) {
                        return preferredRotation;
                    }
                    return mLandscapeRotation;

                case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                    // Return reverse portrait unless overridden.
                    if (isAnyPortrait(preferredRotation)) {
                        return preferredRotation;
                    }
                    return mUpsideDownRotation;

                case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                    // Return seascape unless overridden.
                    if (isLandscapeOrSeascape(preferredRotation)) {
                        return preferredRotation;
                    }
                    return mSeascapeRotation;

                case ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE:
                case ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE:
                    // Return either landscape rotation.
                    if (isLandscapeOrSeascape(preferredRotation)) {
                        return preferredRotation;
                    }
                    if (isLandscapeOrSeascape(lastRotation)) {
                        return lastRotation;
                    }
                    return mLandscapeRotation;

                case ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT:
                case ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT:
                    // Return either portrait rotation.
                    if (isAnyPortrait(preferredRotation)) {
                        return preferredRotation;
                    }
                    if (isAnyPortrait(lastRotation)) {
                        return lastRotation;
                    }
                    return mPortraitRotation;

                default:
                    // For USER, UNSPECIFIED, NOSENSOR, SENSOR and FULL_SENSOR,
                    // just return the preferred orientation we already calculated.
                    if (preferredRotation >= 0) {
                        return preferredRotation;
                    }
                    return Surface.ROTATION_0;
            }
        }
    }

    @Override
    public boolean rotationHasCompatibleMetricsLw(int orientation, int rotation) {
        switch (orientation) {
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
            case ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT:
                return isAnyPortrait(rotation);

            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
            case ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE:
                return isLandscapeOrSeascape(rotation);

            default:
                return true;
        }
    }

    @Override
    public void setRotationLw(int rotation) {
        mOrientationListener.setCurrentRotation(rotation);
    }

    private boolean isLandscapeOrSeascape(int rotation) {
        return rotation == mLandscapeRotation || rotation == mSeascapeRotation;
    }

    private boolean isAnyPortrait(int rotation) {
        return rotation == mPortraitRotation || rotation == mUpsideDownRotation;
    }

    public int getUserRotationMode() {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0, UserHandle.USER_CURRENT) != 0 ?
                        WindowManagerPolicy.USER_ROTATION_FREE :
                                WindowManagerPolicy.USER_ROTATION_LOCKED;
    }

    // User rotation: to be used when all else fails in assigning an orientation to the device
    public void setUserRotationMode(int mode, int rot) {
        ContentResolver res = mContext.getContentResolver();

        // mUserRotationMode and mUserRotation will be assigned by the content observer
        if (mode == WindowManagerPolicy.USER_ROTATION_LOCKED) {
            Settings.System.putIntForUser(res,
                    Settings.System.USER_ROTATION,
                    rot,
                    UserHandle.USER_CURRENT);
            Settings.System.putIntForUser(res,
                    Settings.System.ACCELEROMETER_ROTATION,
                    0,
                    UserHandle.USER_CURRENT);
        } else {
            Settings.System.putIntForUser(res,
                    Settings.System.ACCELEROMETER_ROTATION,
                    1,
                    UserHandle.USER_CURRENT);
        }
    }

    public void setSafeMode(boolean safeMode) {
        mSafeMode = safeMode;
        performHapticFeedbackLw(null, safeMode
                ? HapticFeedbackConstants.SAFE_MODE_ENABLED
                : HapticFeedbackConstants.SAFE_MODE_DISABLED, true);
    }

    static long[] getLongIntArray(Resources r, int resid) {
        int[] ar = r.getIntArray(resid);
        if (ar == null) {
            return null;
        }
        long[] out = new long[ar.length];
        for (int i=0; i<ar.length; i++) {
            out[i] = ar[i];
        }
        return out;
    }

    /** {@inheritDoc} */
    @Override
    public void systemReady() {
        if (!mHeadless) {
            mKeyguardDelegate = new KeyguardServiceDelegate(mContext, null);
            mKeyguardDelegate.onSystemReady();
        }
        synchronized (mLock) {
            updateOrientationListenerLp();
            mSystemReady = true;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateSettings();
                }
            });
        }
    }

    /** {@inheritDoc} */
    public void systemBooted() {
        if (mKeyguardDelegate != null) {
            mKeyguardDelegate.onBootCompleted();
        }
        synchronized (mLock) {
            mSystemBooted = true;
        }
    }

    ProgressDialog mBootMsgDialog = null;

    /** {@inheritDoc} */
    public void showBootMessage(final CharSequence msg, final boolean always) {
        if (mHeadless) return;
        mHandler.post(new Runnable() {
            @Override public void run() {
                if (mBootMsgDialog == null) {
                    mBootMsgDialog = new ProgressDialog(mContext) {
                        // This dialog will consume all events coming in to
                        // it, to avoid it trying to do things too early in boot.
                        @Override public boolean dispatchKeyEvent(KeyEvent event) {
                            return true;
                        }
                        @Override public boolean dispatchKeyShortcutEvent(KeyEvent event) {
                            return true;
                        }
                        @Override public boolean dispatchTouchEvent(MotionEvent ev) {
                            return true;
                        }
                        @Override public boolean dispatchTrackballEvent(MotionEvent ev) {
                            return true;
                        }
                        @Override public boolean dispatchGenericMotionEvent(MotionEvent ev) {
                            return true;
                        }
                        @Override public boolean dispatchPopulateAccessibilityEvent(
                                AccessibilityEvent event) {
                            return true;
                        }
                    };
                    mBootMsgDialog.setTitle(R.string.android_upgrading_title);
                    mBootMsgDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    mBootMsgDialog.setIndeterminate(true);
                    mBootMsgDialog.getWindow().setType(
                            WindowManager.LayoutParams.TYPE_BOOT_PROGRESS);
                    mBootMsgDialog.getWindow().addFlags(
                            WindowManager.LayoutParams.FLAG_DIM_BEHIND
                            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
                    mBootMsgDialog.getWindow().setDimAmount(1);
                    WindowManager.LayoutParams lp = mBootMsgDialog.getWindow().getAttributes();
                    lp.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR;
                    mBootMsgDialog.getWindow().setAttributes(lp);
                    mBootMsgDialog.setCancelable(false);
                    mBootMsgDialog.show();
                }
                mBootMsgDialog.setMessage(msg);
            }
        });
    }

    /** {@inheritDoc} */
    public void hideBootMessages() {
        mHandler.post(new Runnable() {
            @Override public void run() {
                if (mBootMsgDialog != null) {
                    mBootMsgDialog.dismiss();
                    mBootMsgDialog = null;
                }
            }
        });
    }

    /** {@inheritDoc} */
    public void userActivity() {
        // ***************************************
        // NOTE NOTE NOTE NOTE NOTE NOTE NOTE NOTE
        // ***************************************
        // THIS IS CALLED FROM DEEP IN THE POWER MANAGER
        // WITH ITS LOCKS HELD.
        //
        // This code must be VERY careful about the locks
        // it acquires.
        // In fact, the current code acquires way too many,
        // and probably has lurking deadlocks.

        synchronized (mScreenLockTimeout) {
            if (mLockScreenTimerActive) {
                // reset the timer
                mHandler.removeCallbacks(mScreenLockTimeout);
                mHandler.postDelayed(mScreenLockTimeout, mLockScreenTimeout);
            }
        }
    }

    class ScreenLockTimeout implements Runnable {
        Bundle options;

        @Override
        public void run() {
            synchronized (this) {
                if (localLOGV) Log.v(TAG, "mScreenLockTimeout activating keyguard");
                if (mKeyguardDelegate != null) {
                    mKeyguardDelegate.doKeyguardTimeout(options);
                }
                mLockScreenTimerActive = false;
                options = null;
            }
        }

        public void setLockOptions(Bundle options) {
            this.options = options;
        }
    }

    ScreenLockTimeout mScreenLockTimeout = new ScreenLockTimeout();

    public void lockNow(Bundle options) {
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.DEVICE_POWER, null);
        mHandler.removeCallbacks(mScreenLockTimeout);
        if (options != null) {
            // In case multiple calls are made to lockNow, we don't wipe out the options
            // until the runnable actually executes.
            mScreenLockTimeout.setLockOptions(options);
        }
        mHandler.post(mScreenLockTimeout);
    }

    private void updateLockScreenTimeout() {
        synchronized (mScreenLockTimeout) {
            boolean enable = (mAllowLockscreenWhenOn && mScreenOnEarly &&
                    mKeyguardDelegate != null && mKeyguardDelegate.isSecure());
            if (mLockScreenTimerActive != enable) {
                if (enable) {
                    if (localLOGV) Log.v(TAG, "setting lockscreen timer");
                    mHandler.postDelayed(mScreenLockTimeout, mLockScreenTimeout);
                } else {
                    if (localLOGV) Log.v(TAG, "clearing lockscreen timer");
                    mHandler.removeCallbacks(mScreenLockTimeout);
                }
                mLockScreenTimerActive = enable;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void enableScreenAfterBoot() {
        readLidState();
        applyLidSwitchState();
        updateRotation(true);
    }

    private void applyLidSwitchState() {
        if (mLidState == LID_CLOSED && mLidControlsSleep) {
            mPowerManager.goToSleep(SystemClock.uptimeMillis());
        }
    }

    void updateRotation(boolean alwaysSendConfiguration) {
        try {
            //set orientation on WindowManager
            mWindowManager.updateRotation(alwaysSendConfiguration, false);
        } catch (RemoteException e) {
            // Ignore
        }
    }

    void updateRotation(boolean alwaysSendConfiguration, boolean forceRelayout) {
        try {
            //set orientation on WindowManager
            mWindowManager.updateRotation(alwaysSendConfiguration, forceRelayout);
        } catch (RemoteException e) {
            // Ignore
        }
    }

    /**
     * Return an Intent to launch the currently active dock app as home.  Returns
     * null if the standard home should be launched, which is the case if any of the following is
     * true:
     * <ul>
     *  <li>The device is not in either car mode or desk mode
     *  <li>The device is in car mode but ENABLE_CAR_DOCK_HOME_CAPTURE is false
     *  <li>The device is in desk mode but ENABLE_DESK_DOCK_HOME_CAPTURE is false
     *  <li>The device is in car mode but there's no CAR_DOCK app with METADATA_DOCK_HOME
     *  <li>The device is in desk mode but there's no DESK_DOCK app with METADATA_DOCK_HOME
     * </ul>
     * @return A dock intent.
     */
    Intent createHomeDockIntent() {
        Intent intent = null;

        // What home does is based on the mode, not the dock state.  That
        // is, when in car mode you should be taken to car home regardless
        // of whether we are actually in a car dock.
        if (mUiMode == Configuration.UI_MODE_TYPE_CAR) {
            if (ENABLE_CAR_DOCK_HOME_CAPTURE) {
                intent = mCarDockIntent;
            }
        } else if (mUiMode == Configuration.UI_MODE_TYPE_DESK) {
            if (ENABLE_DESK_DOCK_HOME_CAPTURE) {
                intent = mDeskDockIntent;
            }
        }

        if (intent == null) {
            return null;
        }

        ActivityInfo ai = null;
        ResolveInfo info = mContext.getPackageManager().resolveActivityAsUser(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY | PackageManager.GET_META_DATA,
                mCurrentUserId);
        if (info != null) {
            ai = info.activityInfo;
        }
        if (ai != null
                && ai.metaData != null
                && ai.metaData.getBoolean(Intent.METADATA_DOCK_HOME)) {
            intent = new Intent(intent);
            intent.setClassName(ai.packageName, ai.name);
            return intent;
        }

        return null;
    }

    void startDockOrHome() {
		/* add by Gary. start {{----------------------------------- */
        /* 2012-9-8 */
        /* launch the defualt launcher*/
        if(SystemProperties.get("ro.ysten.province","master").equals("c60_jiangsu")){
        //setDefaultLauncher(mCurrentUserId);
			try {
            Intent intentJS = new Intent();
            intentJS = setDefaultLauncher_JS();
            Log.i(TAG,"intentJS: " + intentJS);
            mContext.startActivity(intentJS); 
			}catch (ActivityNotFoundException e) {
				Slog.w(TAG, "jiangsu No activity to handle assist action.", e);
			}
			return;
        }
        /* add by Gary. end   -----------------------------------}} */
		
        awakenDreams();

        Intent dock = createHomeDockIntent();
        if (dock != null) {
            try {
                mContext.startActivityAsUser(dock, UserHandle.CURRENT);
                return;
            } catch (ActivityNotFoundException e) {
            }
        }

        mContext.startActivityAsUser(mHomeIntent, UserHandle.CURRENT);
    }
	
	//begin add by chenfeng for jiangsu at 20200722:start home for jiangsu
    private Intent setDefaultLauncher_JS()
    {
        // get default component 
        DevInfoManager manager = (DevInfoManager) mContext.getSystemService(DevInfoManager.DATA_SERVER);
        String packageName = manager.getValue(DevInfoManager.Launcher);
        //String  packageName = SystemProperties.get("persist.sys.deflauncherpkg", "no");
        String className = getActivities(packageName);
        if(TextUtils.isEmpty(className)){
            if(packageName.equals("tv.icntv.ott"))
            {
                className = "tv.icntv.ott.icntv";
            } else if(packageName.equals("com.gitv.launcher"))
            {
                className = "com.gitv.launcher.ui.WelcomeActivity";
            } else if(packageName.equals("com.chinamobile.activate"))
            {
                className = "com.chinamobile.activate.activity.MainActivity";
            }
            else
            {   
                className = SystemProperties.get("persist.sys.deflauncherclass", "no");
            }
        }
        //add by huxiang at 20190504 for jike default launcher
        if(SystemProperties.get("ro.ysten.province","master").equals("CM202_jike")){
            packageName="tv.icntv.ott";
            className="tv.icntv.ott.icntv";
        }
        //add end 
        ComponentName componentName = new ComponentName(packageName,className);
        Intent intent = new Intent();
        intent.setComponent(componentName);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }
    //end add by chenfeng for jiangsu at 20200722:start home for jiangsu

    /**
     * goes to the home screen
     * @return whether it did anything
     */
    boolean goHome() {
        if (false) {
            // This code always brings home to the front.
            try {
                ActivityManagerNative.getDefault().stopAppSwitches();
            } catch (RemoteException e) {
            }
            sendCloseSystemWindows();
            startDockOrHome();
        } else {
            // This code brings home to the front or, if it is already
            // at the front, puts the device to sleep.
            try {
                if (SystemProperties.getInt("persist.sys.uts-test-mode", 0) == 1) {
                    /// Roll back EndcallBehavior as the cupcake design to pass P1 lab entry.
                    Log.d(TAG, "UTS-TEST-MODE");
                } else {
                    ActivityManagerNative.getDefault().stopAppSwitches();
                    sendCloseSystemWindows();
                    Intent dock = createHomeDockIntent();
                    if (dock != null) {
                        int result = ActivityManagerNative.getDefault()
                                .startActivityAsUser(null, null, dock,
                                        dock.resolveTypeIfNeeded(mContext.getContentResolver()),
                                        null, null, 0,
                                        ActivityManager.START_FLAG_ONLY_IF_NEEDED,
                                        null, null, null, UserHandle.USER_CURRENT);
                        if (result == ActivityManager.START_RETURN_INTENT_TO_CALLER) {
                            return false;
                        }
                    }
                }
                int result = ActivityManagerNative.getDefault()
                        .startActivityAsUser(null, null, mHomeIntent,
                                mHomeIntent.resolveTypeIfNeeded(mContext.getContentResolver()),
                                null, null, 0,
                                ActivityManager.START_FLAG_ONLY_IF_NEEDED,
                                null, null, null, UserHandle.USER_CURRENT);
                if (result == ActivityManager.START_RETURN_INTENT_TO_CALLER) {
                    return false;
                }
            } catch (RemoteException ex) {
                // bummer, the activity manager, which is in this process, is dead
            }
        }
        return true;
    }

    @Override
    public void setCurrentOrientationLw(int newOrientation) {
        synchronized (mLock) {
            if (newOrientation != mCurrentAppOrientation) {
                mCurrentAppOrientation = newOrientation;
                updateOrientationListenerLp();
            }
        }
    }

    private void performAuditoryFeedbackForAccessibilityIfNeed() {
        if (!isGlobalAccessibilityGestureEnabled()) {
            return;
        }
        AudioManager audioManager = (AudioManager) mContext.getSystemService(
                Context.AUDIO_SERVICE);
        if (audioManager.isSilentMode()) {
            return;
        }
        Ringtone ringTone = RingtoneManager.getRingtone(mContext,
                Settings.System.DEFAULT_NOTIFICATION_URI);
        ringTone.setStreamType(AudioManager.STREAM_MUSIC);
        ringTone.play();
    }

    private boolean isGlobalAccessibilityGestureEnabled() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ENABLE_ACCESSIBILITY_GLOBAL_GESTURE_ENABLED, 0) == 1;
    }

    @Override
    public boolean performHapticFeedbackLw(WindowState win, int effectId, boolean always) {
        if (!mVibrator.hasVibrator()) {
            return false;
        }
        final boolean hapticsDisabled = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 0, UserHandle.USER_CURRENT) == 0;
        if (!always && (hapticsDisabled || mKeyguardDelegate.isShowingAndNotHidden())) {
            return false;
        }
        long[] pattern = null;
        switch (effectId) {
            case HapticFeedbackConstants.LONG_PRESS:
                pattern = mLongPressVibePattern;
                break;
            case HapticFeedbackConstants.VIRTUAL_KEY:
                pattern = mVirtualKeyVibePattern;
                break;
            case HapticFeedbackConstants.KEYBOARD_TAP:
                pattern = mKeyboardTapVibePattern;
                break;
            case HapticFeedbackConstants.SAFE_MODE_DISABLED:
                pattern = mSafeModeDisabledVibePattern;
                break;
            case HapticFeedbackConstants.SAFE_MODE_ENABLED:
                pattern = mSafeModeEnabledVibePattern;
                break;
            default:
                return false;
        }
        int owningUid;
        String owningPackage;
        if (win != null) {
            owningUid = win.getOwningUid();
            owningPackage = win.getOwningPackage();
        } else {
            owningUid = android.os.Process.myUid();
            owningPackage = mContext.getOpPackageName();
        }
        if (pattern.length == 1) {
            // One-shot vibration
            mVibrator.vibrate(owningUid, owningPackage, pattern[0]);
        } else {
            // Pattern vibration
            mVibrator.vibrate(owningUid, owningPackage, pattern, -1);
        }
        return true;
    }

    @Override
    public void keepScreenOnStartedLw() {
    }

    @Override
    public void keepScreenOnStoppedLw() {
        if (mKeyguardDelegate != null && !mKeyguardDelegate.isShowingAndNotHidden()) {
            long curTime = SystemClock.uptimeMillis();
            mPowerManager.userActivity(SystemClock.uptimeMillis(), false);
        }
    }

    private int updateSystemUiVisibilityLw() {
        // If there is no window focused, there will be nobody to handle the events
        // anyway, so just hang on in whatever state we're in until things settle down.
        WindowState win = mFocusedWindow != null ? mFocusedWindow : mTopFullscreenOpaqueWindowState;
        if (win == null) {
            return 0;
        }
        if (win.getAttrs().type == TYPE_KEYGUARD && mHideLockScreen == true) {
            // We are updating at a point where the keyguard has gotten
            // focus, but we were last in a state where the top window is
            // hiding it.  This is probably because the keyguard as been
            // shown while the top window was displayed, so we want to ignore
            // it here because this is just a very transient change and it
            // will quickly lose focus once it correctly gets hidden.
            return 0;
        }

        int tmpVisibility = win.getSystemUiVisibility()
                & ~mResettingSystemUiFlags
                & ~mForceClearedSystemUiFlags;
        if (mForcingShowNavBar && win.getSurfaceLayer() < mForcingShowNavBarLayer) {
            tmpVisibility &= ~View.SYSTEM_UI_CLEARABLE_FLAGS;
        }
        final int visibility = updateSystemBarsLw(win, mLastSystemUiFlags, tmpVisibility);
        final int diff = visibility ^ mLastSystemUiFlags;
        final boolean needsMenu = win.getNeedsMenuLw(mTopFullscreenOpaqueWindowState);
        if (diff == 0 && mLastFocusNeedsMenu == needsMenu
                && mFocusedApp == win.getAppToken()) {
            return 0;
        }
        mLastSystemUiFlags = visibility;
        mLastFocusNeedsMenu = needsMenu;
        mFocusedApp = win.getAppToken();
        mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        IStatusBarService statusbar = getStatusBarService();
                        if (statusbar != null) {
                            statusbar.setSystemUiVisibility(visibility, 0xffffffff);
                            statusbar.topAppWindowChanged(needsMenu);
                        }
                    } catch (RemoteException e) {
                        // re-acquire status bar service next time it is needed.
                        mStatusBarService = null;
                    }
                }
            });
        return diff;
    }

    private int updateSystemBarsLw(WindowState win, int oldVis, int vis) {
        // apply translucent bar vis flags
        WindowState transWin = mKeyguard != null && mKeyguard.isVisibleLw() && !mHideLockScreen
                ? mKeyguard
                : mTopFullscreenOpaqueWindowState;
        vis = mStatusBarController.applyTranslucentFlagLw(transWin, vis, oldVis);
        vis = mNavigationBarController.applyTranslucentFlagLw(transWin, vis, oldVis);

        // prevent status bar interaction from clearing certain flags
        boolean statusBarHasFocus = win.getAttrs().type == TYPE_STATUS_BAR;
        if (statusBarHasFocus) {
            int flags = View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.STATUS_BAR_TRANSLUCENT
                    | View.NAVIGATION_BAR_TRANSLUCENT;
            vis = (vis & ~flags) | (oldVis & flags);
        }

        if (!areTranslucentBarsAllowed()) {
            vis &= ~(View.NAVIGATION_BAR_TRANSLUCENT | View.STATUS_BAR_TRANSLUCENT);
        }

        // update status bar
        boolean immersiveSticky =
                (vis & View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) != 0;
        boolean hideStatusBarWM =
                mTopFullscreenOpaqueWindowState != null &&
                (mTopFullscreenOpaqueWindowState.getAttrs().flags
                        & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
        boolean hideStatusBarSysui =
                (vis & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0;
        boolean hideNavBarSysui =
                (vis & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0;

        boolean transientStatusBarAllowed =
                mStatusBar != null && (
                hideStatusBarWM
                || (hideStatusBarSysui && immersiveSticky)
                || statusBarHasFocus);

        boolean transientNavBarAllowed =
                mNavigationBar != null &&
                hideNavBarSysui && immersiveSticky;

        boolean denyTransientStatus = mStatusBarController.isTransientShowRequested()
                && !transientStatusBarAllowed && hideStatusBarSysui;
        boolean denyTransientNav = mNavigationBarController.isTransientShowRequested()
                && !transientNavBarAllowed;
        if (denyTransientStatus || denyTransientNav) {
            // clear the clearable flags instead
            clearClearableFlagsLw();
        }

        vis = mStatusBarController.updateVisibilityLw(transientStatusBarAllowed, oldVis, vis);

        // update navigation bar
        boolean oldImmersiveMode = isImmersiveMode(oldVis);
        boolean newImmersiveMode = isImmersiveMode(vis);
        if (win != null && oldImmersiveMode != newImmersiveMode) {
            final String pkg = win.getOwningPackage();
            mImmersiveModeConfirmation.immersiveModeChanged(pkg, newImmersiveMode);
        }

        vis = mNavigationBarController.updateVisibilityLw(transientNavBarAllowed, oldVis, vis);

        return vis;
    }

    private void clearClearableFlagsLw() {
        int newVal = mResettingSystemUiFlags | View.SYSTEM_UI_CLEARABLE_FLAGS;
        if (newVal != mResettingSystemUiFlags) {
            mResettingSystemUiFlags = newVal;
            mWindowManagerFuncs.reevaluateStatusBarVisibility();
        }
    }

    private boolean isImmersiveMode(int vis) {
        final int flags = View.SYSTEM_UI_FLAG_IMMERSIVE | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        return mNavigationBar != null
                && (vis & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0
                && (vis & flags) != 0
                && canHideNavigationBar();
    }

    /**
     * @return whether the navigation or status bar can be made translucent
     *
     * This should return true unless touch exploration is not enabled or
     * R.boolean.config_enableTranslucentDecor is false.
     */
    private boolean areTranslucentBarsAllowed() {
        return mTranslucentDecorEnabled && !mTouchExplorationEnabled;
    }

    // Use this instead of checking config_showNavigationBar so that it can be consistently
    // overridden by qemu.hw.mainkeys in the emulator.
    @Override
    public boolean hasNavigationBar() {
        return mHasNavigationBar;
    }

    @Override
    public void setLastInputMethodWindowLw(WindowState ime, WindowState target) {
        mLastInputMethodWindow = ime;
        mLastInputMethodTargetWindow = target;
    }

    @Override
    public void setCurrentUserLw(int newUserId) {
        mCurrentUserId = newUserId;
        if (mKeyguardDelegate != null) {
            mKeyguardDelegate.setCurrentUser(newUserId);
        }
        if (mStatusBarService != null) {
            try {
                mStatusBarService.setCurrentUser(newUserId);
            } catch (RemoteException e) {
                // oh well
            }
        }
        setLastInputMethodWindowLw(null, null);
    }

    @Override
    public boolean canMagnifyWindow(int windowType) {
        switch (windowType) {
            case WindowManager.LayoutParams.TYPE_INPUT_METHOD:
            case WindowManager.LayoutParams.TYPE_INPUT_METHOD_DIALOG:
            case WindowManager.LayoutParams.TYPE_NAVIGATION_BAR:
            case WindowManager.LayoutParams.TYPE_MAGNIFICATION_OVERLAY: {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setTouchExplorationEnabled(boolean enabled) {
        mTouchExplorationEnabled = enabled;
    }

    @Override
    public boolean isTopLevelWindow(int windowType) {
        if (windowType >= WindowManager.LayoutParams.FIRST_SUB_WINDOW
                && windowType <= WindowManager.LayoutParams.LAST_SUB_WINDOW) {
            return (windowType == WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
        }
        return true;
    }

    @Override
    public void dump(String prefix, PrintWriter pw, String[] args) {
        pw.print(prefix); pw.print("mSafeMode="); pw.print(mSafeMode);
                pw.print(" mSystemReady="); pw.print(mSystemReady);
                pw.print(" mSystemBooted="); pw.println(mSystemBooted);
        pw.print(prefix); pw.print("mLidState="); pw.print(mLidState);
                pw.print(" mLidOpenRotation="); pw.print(mLidOpenRotation);
                pw.print(" mHdmiPlugged="); pw.println(mHdmiPlugged);
        if (mLastSystemUiFlags != 0 || mResettingSystemUiFlags != 0
                || mForceClearedSystemUiFlags != 0) {
            pw.print(prefix); pw.print("mLastSystemUiFlags=0x");
                    pw.print(Integer.toHexString(mLastSystemUiFlags));
                    pw.print(" mResettingSystemUiFlags=0x");
                    pw.print(Integer.toHexString(mResettingSystemUiFlags));
                    pw.print(" mForceClearedSystemUiFlags=0x");
                    pw.println(Integer.toHexString(mForceClearedSystemUiFlags));
        }
        if (mLastFocusNeedsMenu) {
            pw.print(prefix); pw.print("mLastFocusNeedsMenu=");
                    pw.println(mLastFocusNeedsMenu);
        }
        pw.print(prefix); pw.print("mUiMode="); pw.print(mUiMode);
                pw.print(" mDockMode="); pw.print(mDockMode);
                pw.print(" mCarDockRotation="); pw.print(mCarDockRotation);
                pw.print(" mDeskDockRotation="); pw.println(mDeskDockRotation);
        pw.print(prefix); pw.print("mUserRotationMode="); pw.print(mUserRotationMode);
                pw.print(" mUserRotation="); pw.print(mUserRotation);
                pw.print(" mAllowAllRotations="); pw.println(mAllowAllRotations);
        pw.print(prefix); pw.print("mCurrentAppOrientation="); pw.println(mCurrentAppOrientation);
        pw.print(prefix); pw.print("mCarDockEnablesAccelerometer=");
                pw.print(mCarDockEnablesAccelerometer);
                pw.print(" mDeskDockEnablesAccelerometer=");
                pw.println(mDeskDockEnablesAccelerometer);
        pw.print(prefix); pw.print("mLidKeyboardAccessibility=");
                pw.print(mLidKeyboardAccessibility);
                pw.print(" mLidNavigationAccessibility="); pw.print(mLidNavigationAccessibility);
                pw.print(" mLidControlsSleep="); pw.println(mLidControlsSleep);
        pw.print(prefix); pw.print("mLongPressOnPowerBehavior=");
                pw.print(mLongPressOnPowerBehavior);
                pw.print(" mHasSoftInput="); pw.println(mHasSoftInput);
        pw.print(prefix); pw.print("mScreenOnEarly="); pw.print(mScreenOnEarly);
                pw.print(" mScreenOnFully="); pw.print(mScreenOnFully);
                pw.print(" mOrientationSensorEnabled="); pw.println(mOrientationSensorEnabled);
        pw.print(prefix); pw.print("mOverscanScreen=("); pw.print(mOverscanScreenLeft);
                pw.print(","); pw.print(mOverscanScreenTop);
                pw.print(") "); pw.print(mOverscanScreenWidth);
                pw.print("x"); pw.println(mOverscanScreenHeight);
        if (mOverscanLeft != 0 || mOverscanTop != 0
                || mOverscanRight != 0 || mOverscanBottom != 0) {
            pw.print(prefix); pw.print("mOverscan left="); pw.print(mOverscanLeft);
                    pw.print(" top="); pw.print(mOverscanTop);
                    pw.print(" right="); pw.print(mOverscanRight);
                    pw.print(" bottom="); pw.println(mOverscanBottom);
        }
        pw.print(prefix); pw.print("mRestrictedOverscanScreen=(");
                pw.print(mRestrictedOverscanScreenLeft);
                pw.print(","); pw.print(mRestrictedOverscanScreenTop);
                pw.print(") "); pw.print(mRestrictedOverscanScreenWidth);
                pw.print("x"); pw.println(mRestrictedOverscanScreenHeight);
        pw.print(prefix); pw.print("mUnrestrictedScreen=("); pw.print(mUnrestrictedScreenLeft);
                pw.print(","); pw.print(mUnrestrictedScreenTop);
                pw.print(") "); pw.print(mUnrestrictedScreenWidth);
                pw.print("x"); pw.println(mUnrestrictedScreenHeight);
        pw.print(prefix); pw.print("mRestrictedScreen=("); pw.print(mRestrictedScreenLeft);
                pw.print(","); pw.print(mRestrictedScreenTop);
                pw.print(") "); pw.print(mRestrictedScreenWidth);
                pw.print("x"); pw.println(mRestrictedScreenHeight);
        pw.print(prefix); pw.print("mStableFullscreen=("); pw.print(mStableFullscreenLeft);
                pw.print(","); pw.print(mStableFullscreenTop);
                pw.print(")-("); pw.print(mStableFullscreenRight);
                pw.print(","); pw.print(mStableFullscreenBottom); pw.println(")");
        pw.print(prefix); pw.print("mStable=("); pw.print(mStableLeft);
                pw.print(","); pw.print(mStableTop);
                pw.print(")-("); pw.print(mStableRight);
                pw.print(","); pw.print(mStableBottom); pw.println(")");
        pw.print(prefix); pw.print("mSystem=("); pw.print(mSystemLeft);
                pw.print(","); pw.print(mSystemTop);
                pw.print(")-("); pw.print(mSystemRight);
                pw.print(","); pw.print(mSystemBottom); pw.println(")");
        pw.print(prefix); pw.print("mCur=("); pw.print(mCurLeft);
                pw.print(","); pw.print(mCurTop);
                pw.print(")-("); pw.print(mCurRight);
                pw.print(","); pw.print(mCurBottom); pw.println(")");
        pw.print(prefix); pw.print("mContent=("); pw.print(mContentLeft);
                pw.print(","); pw.print(mContentTop);
                pw.print(")-("); pw.print(mContentRight);
                pw.print(","); pw.print(mContentBottom); pw.println(")");
        pw.print(prefix); pw.print("mDock=("); pw.print(mDockLeft);
                pw.print(","); pw.print(mDockTop);
                pw.print(")-("); pw.print(mDockRight);
                pw.print(","); pw.print(mDockBottom); pw.println(")");
        pw.print(prefix); pw.print("mDockLayer="); pw.print(mDockLayer);
                pw.print(" mStatusBarLayer="); pw.println(mStatusBarLayer);
        pw.print(prefix); pw.print("mShowingLockscreen="); pw.print(mShowingLockscreen);
                pw.print(" mShowingDream="); pw.print(mShowingDream);
                pw.print(" mDreamingLockscreen="); pw.println(mDreamingLockscreen);
        if (mLastInputMethodWindow != null) {
            pw.print(prefix); pw.print("mLastInputMethodWindow=");
                    pw.println(mLastInputMethodWindow);
        }
        if (mLastInputMethodTargetWindow != null) {
            pw.print(prefix); pw.print("mLastInputMethodTargetWindow=");
                    pw.println(mLastInputMethodTargetWindow);
        }
        if (mStatusBar != null) {
            pw.print(prefix); pw.print("mStatusBar=");
                    pw.println(mStatusBar);
        }
        if (mNavigationBar != null) {
            pw.print(prefix); pw.print("mNavigationBar=");
                    pw.println(mNavigationBar);
        }
        if (mKeyguard != null) {
            pw.print(prefix); pw.print("mKeyguard=");
                    pw.println(mKeyguard);
        }
        if (mFocusedWindow != null) {
            pw.print(prefix); pw.print("mFocusedWindow=");
                    pw.println(mFocusedWindow);
        }
        if (mFocusedApp != null) {
            pw.print(prefix); pw.print("mFocusedApp=");
                    pw.println(mFocusedApp);
        }
        if (mWinDismissingKeyguard != null) {
            pw.print(prefix); pw.print("mWinDismissingKeyguard=");
                    pw.println(mWinDismissingKeyguard);
        }
        if (mTopFullscreenOpaqueWindowState != null) {
            pw.print(prefix); pw.print("mTopFullscreenOpaqueWindowState=");
                    pw.println(mTopFullscreenOpaqueWindowState);
        }
        if (mForcingShowNavBar) {
            pw.print(prefix); pw.print("mForcingShowNavBar=");
                    pw.println(mForcingShowNavBar); pw.print( "mForcingShowNavBarLayer=");
                    pw.println(mForcingShowNavBarLayer);
        }
        pw.print(prefix); pw.print("mTopIsFullscreen="); pw.print(mTopIsFullscreen);
                pw.print(" mHideLockScreen="); pw.println(mHideLockScreen);
        pw.print(prefix); pw.print("mForceStatusBar="); pw.print(mForceStatusBar);
                pw.print(" mForceStatusBarFromKeyguard=");
                pw.println(mForceStatusBarFromKeyguard);
        pw.print(prefix); pw.print("mDismissKeyguard="); pw.print(mDismissKeyguard);
                pw.print(" mWinDismissingKeyguard="); pw.print(mWinDismissingKeyguard);
                pw.print(" mHomePressed="); pw.println(mHomePressed);
        pw.print(prefix); pw.print("mAllowLockscreenWhenOn="); pw.print(mAllowLockscreenWhenOn);
                pw.print(" mLockScreenTimeout="); pw.print(mLockScreenTimeout);
                pw.print(" mLockScreenTimerActive="); pw.println(mLockScreenTimerActive);
        pw.print(prefix); pw.print("mEndcallBehavior="); pw.print(mEndcallBehavior);
                pw.print(" mIncallPowerBehavior="); pw.print(mIncallPowerBehavior);
                pw.print(" mLongPressOnHomeBehavior="); pw.println(mLongPressOnHomeBehavior);
        pw.print(prefix); pw.print("mLandscapeRotation="); pw.print(mLandscapeRotation);
                pw.print(" mSeascapeRotation="); pw.println(mSeascapeRotation);
        pw.print(prefix); pw.print("mPortraitRotation="); pw.print(mPortraitRotation);
                pw.print(" mUpsideDownRotation="); pw.println(mUpsideDownRotation);
        pw.print(prefix); pw.print("mDemoHdmiRotation="); pw.print(mDemoHdmiRotation);
                pw.print(" mDemoHdmiRotationLock="); pw.println(mDemoHdmiRotationLock);
        pw.print(prefix); pw.print("mUndockedHdmiRotation="); pw.println(mUndockedHdmiRotation);
        mStatusBarController.dump(pw, prefix);
        mNavigationBarController.dump(pw, prefix);
    }
        //begin:add by xumiao at 20190725 add Suspend Dialog
	void showJilinSuspendDialog() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mShowPowerDialog = true;
                Slog.d(TAG, "jilin showSuspendDialog");
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(com.android.internal.R.string.action_suspend_title);
                builder.setMessage(com.android.internal.R.string.action_suspend_jilin_message);
                mHandler.postDelayed(new Runnable(){
                    public void run(){
                    Log.i(TAG,"go to sleep......");
                    // mPowerManager.goToSleep(SystemClock.uptimeMillis());
                    PhoneWindowManager.sleepmode(0);
                    HiSysManager hisys = new HiSysManager();
                    hisys.setProperty("persist.suspend.mode", "deep_restart");
                    SystemProperties.set("persist.sys.smartsuspendin", "0");
                    Intent mIntent = new Intent("android.ysten.systemupdate");
                    mIntent.putExtra("powercontrol", "poweroff");
                    mContext.sendBroadcast(mIntent);
                    }
                },3000);
                AlertDialog dialog = builder.create();
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
                dialog.show();
            }
        });
    }
    //end:add by xumiao at 20190725 add Suspend Dialog

    void showSuspendDialog() {
        if(mShowPowerDialog)
            return ;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mShowPowerDialog = true;
                Slog.d(TAG, "chinamobile showSuspendDialog");
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(com.android.internal.R.string.action_suspend_title);
                builder.setMessage(com.android.internal.R.string.action_suspend_message);
                builder.setCancelable(false);
                builder.setNegativeButton(com.android.internal.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Slog.d(TAG, "cancel Suspend Dialog");
                                mShowPowerDialog = false;
                            }
                        });
                builder.setOnKeyListener(
                        new DialogInterface.OnKeyListener() {
                            @Override
                            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                                if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                                    Slog.d(TAG, "KEYCODE_BACK Suspend Dialog");
                                    mShowPowerDialog = false;
                                }
                                return false;
                            }
                        });
                builder.setPositiveButton(com.android.internal.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Slog.d(TAG, "Show Suspend Dialog");
				//				swithOffIOAndShutdownLater();
                                mShowPowerDialog = false;
                               // Intent intent = new Intent("android.intent.action.ACTION_SHUTDOWN");
                                //HiSysManager hisys = new HiSysManager();
                               // HiDisplayManager display = new HiDisplayManager();
                               // hisys.adjustDevState("/proc/inputblock/status","1");
                               // display.detachIntf();
                               // display.setHDMIClose();
                               // mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT_OR_SELF);
                            if(SystemProperties.get("ro.ysten.province","master").contains("jidi")
								//add by penghui at 20190821: add for ningxia_jidi
								&&!SystemProperties.get("ro.ysten.province","master").equals("cm201_ningxia_jidi")) {
                            }else{
                                mHandler.postDelayed(new Runnable(){
                                    public void run(){
                                        Log.i(TAG,"go to sleep......");
                                       // mPowerManager.goToSleep(SystemClock.uptimeMillis());
                                       PhoneWindowManager.sleepmode(0);
                                       HiSysManager hisys = new HiSysManager();
                                       hisys.setProperty("persist.suspend.mode", "deep_restart");
                                       SystemProperties.set("persist.sys.smartsuspendin", "0");
                                       Intent mIntent = new Intent("android.ysten.systemupdate");
                                       mIntent.putExtra("powercontrol", "poweroff");
                                       mContext.sendBroadcast(mIntent);
                                    }
                                },3000);
                            }
				swithOffIOAndShutdownLater();
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
                dialog.show();
                if(dialog.getButton(AlertDialog.BUTTON_NEGATIVE)==null){
                     Slog.d(TAG, "SDialog button is null");
                }else{
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setFocusable(true);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setFocusableInTouchMode(true);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
                }
            }
        });
    }

	//add by sunlei 191127 for 湖北短按powerkey进入待机
	void showSuspendDialog_HB() {
		Log.d("sunlei","smart_suspend Intent start");
		SystemProperties.set("persist.suspend.mode", "smart_suspend");
		SystemProperties.set("persist.sys.smartsuspendin", "1");
		Intent intent = new Intent("android.ysten.systemupdate");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("powercontrol", "poweroff");
        mContext.sendBroadcast(intent);

    }
	//end by sunlei 191127 for 湖北短按powerkey进入待机

	private void doSync() {
        try {
            Process syncPro = Runtime.getRuntime().exec("sh");
            DataOutputStream os = new DataOutputStream(syncPro
                    .getOutputStream());
            os.writeBytes("sync" + "\n");
            os.writeBytes("exit\n");
            os.flush();
            syncPro.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("baiyh",
                  "Settings  execCmd exception " + e.toString());
        }
    }

    void gotoHome() {
        //SystemProperties.set("persist.sys.suspend.value2", "1");//待机唤醒不显示网络提示框
        SystemProperties.set("persist.sys.suspend.value", "1");
        SystemProperties.set("persist.sys.launcher.displayed", "0");//iptv待机唤醒时不弹8010提示框
        doSync();
    }

    private boolean isServiceRunning(Context context, String ServiceName) {
        Log.d(TAG," enter isRunning");
        if (("").equals(ServiceName) || ServiceName == null)
            return false;
        ActivityManager myManager = (ActivityManager) context
                                    .getSystemService(Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                .getRunningServices(Integer.MAX_VALUE);
        for (int i = 0; i < runningService.size(); i++) {
            Log.d(TAG,"runningService:"+runningService.get(i).service.getClassName().toString());
            if (runningService.get(i).service.getClassName().toString().contains(ServiceName)) {
                return true;
            }
        }
        return false;
    }

	//added by ysten yzs begin at 20200505 
	//已登陆OTT后回首页广播
	private void sendBroadcastToHomeByOttLogined(){
		int homeKey = Settings.System.getInt(mContext.getContentResolver(), "IS_RESPOND_HOME_KEY", 0);
		if(DEBUG)
	        Log.i("yzs007", "sendBroadcastToHome is called and homKey is " + homeKey);
		if (homeKey!=0){
			return;
		}				
        Intent intent=new Intent();
        intent.setAction("android.intent.action.HOME_KEY_PRESSED");
		mContext.sendBroadcast(intent);
		//sendCloseSystemWindows("homekey");
		try {
			ComponentName comp = new ComponentName("tv.icntv.ott", "tv.icntv.ott.icntv"); 		
			Intent home=new Intent();	
			home.setComponent(comp);
			home = new Intent(Intent.ACTION_MAIN);
			home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//|Intent.FLAG_ACTIVITY_CLEAR_TOP);//|Intent.FLAG_ACTIVITY_CLEAR_TOP);
			home.addCategory(Intent.CATEGORY_HOME);
			home.addCategory(Intent.CATEGORY_LAUNCHER);
			mContext.startActivity(home);
		} catch(Exception ex) {
		}

	}
	
	
	private void suspendGotoHomeOtt(int value)
	{	
		Intent home = new Intent();
		if(value == 1){//ott
		
	        SystemProperties.set("persist.sys.start_auto", "true");//待机唤醒直接回EPG
			//if(SystemProperties.get("persist.sys.launcher.started","false").contains("true")){
			String authStatus=getValueFromStb("authStatus")+"";
			String user_token=getValueFromStb("user_token")+"";
			if(DEBUG)
			     Log.d("yzs007","authStatus="+authStatus+","+"user_token="+user_token);
			if(authStatus.equals("AuthSuccess")&&!user_token.equals("")) {
				sendBroadcastToHomeByOttLogined();
			}else{						
				//Intent intent = new Intent();
				ComponentName comp = new ComponentName("com.huawei.stb.tm1", "com.huawei.stb.tm.ui.service.BootService"); 
				home.setComponent(comp);
				mContext.startService(home);	
			}
	    }else if(SystemProperties.getInt("persist.sys.launcher.value", 0) == 0){//persist.sys.launcher.value 为0则直接进Provision
			ComponentName componentName = new ComponentName("com.fujian.provision", "com.fujian.provision.DefaultActivity");
			Log.d(TAG,"start fujian provision");
			home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			home.setComponent(componentName);
			mContext.startActivity(home);
		}				
	}
	//added by ysten yzs end 
	
	void showSuspendDialog_FJ() {
	    //Toast.makeText(mContext, "系统正在进入待机模式...", Toast.LENGTH_LONG).show();
	    //SystemProperties.set("persist.suspend.mode", "smart_suspend");
        SystemProperties.set("persist.suspend.mode", "deep_launcher");
        SystemProperties.set("persist.sys.smartsuspendin", "1");
        SystemProperties.set("persist.sys.tm.killed","false");
        SystemProperties.set("persist.sys.launcher.provision", "true");
	
		SystemProperties.set("sys.suspend.state", "true");//关闭静帧
		int value = SystemProperties.getInt("persist.sys.launcher.value", 0);		
		ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		if(value == 1){
			try {
			    //am.forceStopPackage("tv.icntv.ott");
			} catch (RuntimeException e) {
				Log.e(TAG, "killApp RuntimeException !!!");            
			}			
		} else if(value == 2){
			try {
				am.forceStopPackage("com.ysten.sjiptv");
			} catch (RuntimeException e) {
				Log.e(TAG, "killApp RuntimeException !!!");            
			}
		}
		try {
			am.forceStopPackage("com.ysten.setting");
		}catch (RuntimeException e) {
			Log.e(TAG, "killApp RuntimeException !!!");            
		}
		PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		powerManager.goToSleep(SystemClock.uptimeMillis());
    }
    
	private void suspendGotoHomeIPTV(){
		Intent intent = new Intent();
		ComponentName compName= new ComponentName("com.ysten.sjiptv","com.ysten.sjiptv.app.SJIPTVActivity");
		
		int started = SystemProperties.getInt("persist.sys.launcher.displayed", 0);
		//未登录时先进setting后按四色键黑屏
		if(started == 1){			
			intent.putExtra("intentMsg", "EPGDomain");                
		}
		intent.setComponent(compName);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		mContext.startActivityAsUser(intent, android.os.UserHandle.CURRENT);
			
	}
	
    void showSuspendDialog_JS() {
         if(mShowPowerDialog)
           return ;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mShowPowerDialog = true;
                Slog.d(TAG, "chinamobile showSuspendDialog");
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(com.android.internal.R.string.action_suspend_title);
                builder.setMessage(com.android.internal.R.string.action_suspend_hold);
                
                AlertDialog dialog = builder.create();
                dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
                dialog.show();
                if(dialog.getButton(AlertDialog.BUTTON_NEGATIVE)==null){
                     Slog.d(TAG, "SDialog button is null");
                }else{
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setFocusable(true);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setFocusableInTouchMode(true);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).requestFocus();
                }
            }
        });
         new Thread(new Runnable(){
                   @Override
            public void run() {
                              try
                                {
                                Thread.sleep(5000);
                                }
                               catch(Exception e)
                               {
                               Slog.d(TAG, "Show Suspend Dialog");
                                }
                                Slog.d(TAG, "Show Suspend Dialog");
                                mShowPowerDialog = false;
                                HiSysManager hisys = new HiSysManager();
                                hisys.setProperty("persist.suspend.mode", "deep_restart");
                                SystemProperties.set("persist.sys.smartsuspendin", "0");
                                Intent intent = new Intent("android.ysten.systemupdate");
                                intent.putExtra("powercontrol", "poweroff");
                                mContext.sendBroadcast(intent);
                               }
                  }).start();

    }

    void swithOffIOAndShutdownLater() {
        String cec_suspend  = SystemProperties.get("persist.sys.hdmi.cec", "false");
        String hdmi_cec_support = SystemProperties.get("persist.sys.cecsupport", "false");
        Intent intent = new Intent("android.intent.action.ACTION_SHUTDOWN");
        HiSysManager hisys = new HiSysManager();
        HiDisplayManager display = new HiDisplayManager();
        hisys.adjustDevState("/proc/inputblock/status","1");
        display.setOutputEnable(1,0);
        display.setHDMIClose();
        display.clearResumeState();
        if ("true".equals(cec_suspend) && "true".equals(hdmi_cec_support)) {
            SystemProperties.set("persist.sys.cec.status", "true");
            display = new HiDisplayManager();
            display.setCECSuspend();
        }
        mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT_OR_SELF);
        if(SystemProperties.get("ro.ysten.province","master").equals("cm201_ningxia_jidi")){		
			//add by penghui at 20190821: add for ningxia_jidi
           mHandler.postDelayed(new Runnable(){
                    public void run(){
                        Log.i(TAG,"go to sleep......");
                        mPowerManager.goToSleep(SystemClock.uptimeMillis());
                        PhoneWindowManager.sleepmode(0);
                        HiSysManager hisys = new HiSysManager();
                        hisys.setProperty("persist.suspend.mode", "deep_restart");
                        SystemProperties.set("persist.sys.smartsuspendin", "0");
                        Intent mIntent = new Intent("android.ysten.systemupdate");
                        mIntent.putExtra("powercontrol", "poweroff");
                        mContext.sendBroadcast(mIntent);
                    }
                },3000);
        }else if(SystemProperties.get("ro.ysten.province","master").equals("cm201_hainan_jidi")){
            PhoneWindowManager.sleepmode(0);                                                    
            HiSysManager mHisys = new HiSysManager();                                       
            mHisys.setProperty("persist.suspend.mode", "deep_restart");                     
            SystemProperties.set("persist.sys.smartsuspendin", "0");                       
            Intent mIntent = new Intent("android.ysten.systemupdate");                     
            mIntent.putExtra("powercontrol", "poweroff");                                  
            mContext.sendBroadcast(mIntent);        
        }else{
            mPowerManager.goToSleep(SystemClock.uptimeMillis());
            PhoneWindowManager.sleepmode(0);
		}
    }

//HISILICON add begin
boolean isPackageExist(String packageName)  {
    PackageInfo packageInfo;
    String name;
    boolean ret = false;
    Log.d(TAG, "package name " + packageName);
    List<PackageInfo> packages = mContext.getPackageManager().getInstalledPackages(0);
    for(int i=0;i<packages.size();i++) {
        packageInfo = packages.get(i);
        name = packageInfo.packageName;
        if(name.equals(packageName)) {
            Log.d(TAG, "match package " + name);
            ret = true;
        }
    }

    return ret;
}
//HISILICON add end

	public static void addprocess(int flag)
	{
					try
								{
								
								Process p;
								if(flag==1)
								 p = Runtime.getRuntime().exec("/system/bin/sample_gpionet 6 0xf8b20040 0x0");
								 else if(flag==0)
								 p = Runtime.getRuntime().exec("/system/bin/sample_gpionet 6 0xf8b20040 0xff");
			                     else
								 p = Runtime.getRuntime().exec("/system/bin/sample_gpionet 6 0xf8b20400 0x70");					 

                String data = "";
		BufferedReader ie = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String error = null;
		while ((error = ie.readLine()) != null && !error.equals("null")) {
			data += error + "\n";
			}
		String line = null;
		while ((line = in.readLine()) != null && !line.equals("null")) {
			data += line + "\n";
			//Log.v("ls", line);
			}
								Log.i("xwj", "Show Suspend Dialog ok"+data);
								}
								catch(IOException e)
								{
								Log.i("xwj", "Show Suspend Dialog error");
								}
	}
	
        //add by sunlei 1206 for 湖北假待机
        private static final int COLOR_RED = 0; 
        private static final int COLOR_GREEN = 1; 
	private static void changeLedColor(int flag)
	{
        if(flag==COLOR_GREEN) {
            PhoneWindowManager.ledpmanager(5);
			PhoneWindowManager.ledpmanager(3);
        } else {
            PhoneWindowManager.ledpmanager(4);
			PhoneWindowManager.ledpmanager(2);
        }
	}
	//end by sunlei 1206 for 湖北假待机
	
		public static void ledprocess(int flag)
	{
	//ledpmanager(3);
	//ledpmanager(1);
	
	  
					try
								{
								
								Process p;
								if(flag==1)
								 p = Runtime.getRuntime().exec("sample_gpionet 45 0xf8004010 0xff &");
			                     else
								 p = Runtime.getRuntime().exec("sample_gpionet 45 0xf8004010 0xff &");					 

                String data = "";
		BufferedReader ie = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String error = null;
		while ((error = ie.readLine()) != null && !error.equals("null")) {
			data += error + "\n";
			}
		String line = null;
		while ((line = in.readLine()) != null && !line.equals("null")) {
			data += line + "\n";
			//Log.v("ls", line);
			}
								Log.i("xwj", "Show Suspend Dialog ok"+data);
								}
								catch(IOException e)
								{
								Log.i("xwj", "Show Suspend Dialog error");
								}
                                
                                try
								{
								
								Process p;
								if(flag==1)
                                {
								 p = Runtime.getRuntime().exec("sample_gpionet 6 0xf8b20400 0x40 &");

                                    String data = "";
                                    BufferedReader ie = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                                    BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                                    String error = null;
                                    while ((error = ie.readLine()) != null && !error.equals("null")) {
                                        data += error + "\n";
                                    }
                                String line = null;
                                while ((line = in.readLine()) != null && !line.equals("null")) {
                                    data += line + "\n";
                                    //Log.v("ls", line);
                                    }
								    Log.i("zzl", "Show Suspend Dialog ok"+data);
                                }
								}
								catch(IOException e)
								{
								    Log.i("zzl", "Show Suspend Dialog error");
								}

								
	}
	public static void ledpmanager(int flag)
	{/*
					try
								{
								
								Process p;
								String testcmd="echo ";
								testcmd=testcmd+flag;
								testcmd=testcmd+"  > /sdcard/fifoled";
                              Log.i("xwj", "Show Suspend Dialog ok"+testcmd);
								 p = Runtime.getRuntime().exec(testcmd);
			                   
													 

                String data = "";
		BufferedReader ie = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String error = null;
		while ((error = ie.readLine()) != null && !error.equals("null")) {
			data += error + "\n";
			}
		String line = null;
		while ((line = in.readLine()) != null && !line.equals("null")) {
			data += line + "\n";
			//Log.v("ls", line);
			}
								Log.i("xwj", "Show Suspend Dialog ok"+data);
								}
								catch(IOException e)
								{
								Log.i("xwj", "Show Suspend Dialog error"+e);
								}
								*/
								String testcmd="";
								testcmd=testcmd+flag;
                                                                if(flag<7)
								NativeFetch.nativesetsystem(testcmd);
                                                                else
                                                                NativeFetch.nativesetsystemcpu(testcmd);
								
								
	}
         public static void runresetlauncher()
        {
               ledpmanager(9);
              /*                          try
                                                                {

                                                                Process p;
                                                                 p = Runtime.getRuntime().exec("echo 1 > /data/testfifo &");

                String data = "";
                BufferedReader ie = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String error = null;
                while ((error = ie.readLine()) != null && !error.equals("null")) {
                        data += error + "\n";
                        }
                String line = null;
                while ((line = in.readLine()) != null && !line.equals("null")) {
                        data += line + "\n";
                        //Log.v("ls", line);
                        }
                                                                Log.i("xwj", "Show Suspend Dialog ok"+data);
                                                                }
                                                                catch(IOException e)
                                                                {
                                                                Log.i("xwj", "Show Suspend Dialog error");
                                                                } */
        }
        public static void resetwlan0()
        {
                                        try
                                                                {

                                                                Process p;
                                                                p = Runtime.getRuntime().exec("/system/bin/testwlan0.sh &");
                String data = "";
                BufferedReader ie = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String error = null;
                while ((error = ie.readLine()) != null && !error.equals("null")) {
                        data += error + "\n";
                        }
                String line = null;
                while ((line = in.readLine()) != null && !line.equals("null")) {
                        data += line + "\n";
                        //Log.v("ls", line);
                        }
                                                                Log.i("xwj", "Show Suspend Dialog ok"+data);
                                                                }
                                                                catch(IOException e)
                                                                {
                                                                Log.i("xwj", "Show Suspend Dialog error");
                                                                }
        }
		public static void sleepmode(int mode1)
        {
                                        try
                                                                {


                                                                Process p;
																if(mode1==0)
                                                                p = Runtime.getRuntime().exec("/system/bin/sh /system/bin/hello1.sh 0");
																else
																p = Runtime.getRuntime().exec("/system/bin/sh /system/bin/hello1.sh 1");
                String data = "";
                BufferedReader ie = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String error = null;
                while ((error = ie.readLine()) != null && !error.equals("null")) {
                        data += error + "\n";
                        }
                String line = null;
                while ((line = in.readLine()) != null && !line.equals("null")) {
                        data += line + "\n";
                        //Log.v("ls", line);
                        }
                                                                Log.i("xwj", "Show Suspend Dialog ok"+data);
                                                                }
                                                                catch(IOException e)
                                                                {
                                                                Log.i("xwj", "Show Suspend Dialog error");
                                                                }
        }
		/*
		public static void poweroffnow()
		{
		sleepmode(0);
		PowerManager mananger = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
        mananger.goToSleep(SystemClock.uptimeMillis());
		}*/

	/* add by Gary. start {{----------------------------------- */
    /* 2012-9-8 */
    /* launch the defualt launcher when the system boots for the first time */
    private boolean mFirstLaunch = false;
    private void setDefaultLauncher(int userId)
    {
        // get default component
        DevInfoManager manager = (DevInfoManager) mContext.getSystemService(DevInfoManager.DATA_SERVER);
        String packageName = manager.getValue(DevInfoManager.Launcher);
        //String  packageName = SystemProperties.get("persist.sys.deflauncherpkg", "no");
        String className = getActivities(packageName);
        if(TextUtils.isEmpty(className)){
            if(packageName.equals("tv.icntv.ott"))
            {
                className = "tv.icntv.ott.icntv";
            } else if(packageName.equals("com.gitv.launcher"))
            {
                className = "com.gitv.launcher.ui.WelcomeActivity";
            } else if(packageName.equals("com.chinamobile.activate"))
            {
                className = "com.chinamobile.activate.activity.MainActivity";
            }
            else
            {   
                className = SystemProperties.get("persist.sys.deflauncherclass", "no");
            }
        }
        Slog.i(TAG, "default packageName = " + packageName + ", default className = " + className);
        if(!packageName.equals("no") && !className.equals("no")){
            boolean firstLaunch = SystemProperties.getBoolean("persist.sys.sw.firstLaunch", true);
            Slog.d(TAG, "firstLaunch = " + firstLaunch);
            if(firstLaunch){
                mFirstLaunch = true;
                // do this only for the first boot
                //SystemProperties.set("persist.sys.sw.firstLaunch", "false");
            }
            Slog.d(TAG, "mFirstLaunch = " + mFirstLaunch);
            if(mFirstLaunch){
                IPackageManager pm = ActivityThread.getPackageManager();

                //clear the current preferred launcher
                ArrayList<IntentFilter> intentList = new ArrayList<IntentFilter>();
                ArrayList<ComponentName> cnList = new ArrayList<ComponentName>();
                mContext.getPackageManager().getPreferredActivities(intentList, cnList, null);
                IntentFilter dhIF;
                for(int i = 0; i < cnList.size(); i++)
                {
                    dhIF = intentList.get(i);
                    if(dhIF.hasAction(Intent.ACTION_MAIN) &&
                            dhIF.hasCategory(Intent.CATEGORY_HOME))
                    {
                        mContext.getPackageManager().clearPackagePreferredActivities(cnList.get(i).getPackageName());
                    }
                }

                // get all Launcher activities
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                List<ResolveInfo> list = new ArrayList<ResolveInfo>();
                try
                {
                    list = pm.queryIntentActivities(intent,
                            intent.resolveTypeIfNeeded(mContext.getContentResolver()),
                            PackageManager.MATCH_DEFAULT_ONLY, userId);
                }catch (RemoteException e) {
                    throw new RuntimeException("Package manager has died", e);
                }
                // get all components and the best match
                IntentFilter filter = new IntentFilter();
                filter.addAction(Intent.ACTION_MAIN);
                filter.addCategory(Intent.CATEGORY_HOME);
                filter.addCategory(Intent.CATEGORY_DEFAULT);
                final int N = list.size();
                ComponentName[] set = new ComponentName[N];
                int bestMatch = 0;
                for (int i = 0; i < N; i++)
                {
                    ResolveInfo r = list.get(i);
                    set[i] = new ComponentName(r.activityInfo.packageName,
                            r.activityInfo.name);
                    if (r.match > bestMatch) bestMatch = r.match;
                }
                // add the default launcher as the preferred launcher
                ComponentName launcher = new ComponentName(packageName, className);
                try
                {
                    pm.addPreferredActivity(filter, bestMatch, set, launcher, userId);
                } catch (RemoteException e) {
                    throw new RuntimeException("Package manager has died", e);
                }
            }
        }
    }
    /* add by Gary. end   -----------------------------------}} */
	
	public static boolean isActivityRunning(Context mContext, String activityClassName){
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> info = activityManager.getRunningTasks(1);
        if(info != null && info.size() > 0){
            ComponentName component = info.get(0).topActivity;
			Log.v(TAG, "isActivityRunning component.getClassName()= " + component.getClassName());
            //if(activityClassName.equals(component.getClassName())){
			if(component.getClassName().contains(activityClassName)){
                return true;
            }
        }
        return false;
    }
    
    private String getActivities(String packageName)
    {
        String className = null;
        Intent localIntent = new Intent("android.intent.action.MAIN", null);
        localIntent.addCategory("android.intent.category.LAUNCHER");
        List<ResolveInfo> appList =  mContext.getPackageManager().queryIntentActivities(localIntent, 0);
        for (int i = 0; i < appList.size(); i++) {
            ResolveInfo r = appList.get(i);
            String packageStr = r.activityInfo.packageName;
            if (packageStr.equals(packageName)) {
                className = r.activityInfo.name;  
                Slog.i(TAG, "MAIN Activity = " + className);
                break;
            }
        }
        return className;
    }
    
    private void freeMemory(String apkNameList[]){
		ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> info = activityManager.getRunningTasks(1);
        if(info != null && info.size() > 0){
            ComponentName component = info.get(0).topActivity;
			Log.v("freeMemory", "top activity " + component.getClassName());
            String topPkg=component.getPackageName();
            int j = 0;
			for(j=0;j<apkNameList.length;j++){
			    String whiteList = apkNameList[j];

			    if(topPkg.contains(whiteList)){
			        Log.v(TAG,"pkg in whitelist "+ topPkg);
                    break;
                }
			}
            if(j == apkNameList.length) {
                Log.v(TAG,"pkg not in whitelist, kill "+ topPkg);
                activityManager.forceStopPackage(topPkg);
            }	
        }
	}
	
	private void doIPTVFunctionAH(int keyCode){
        Log.d(TAG,"doIPTVFunctionAH"+keyCode);
        /*add by zhaolianghua for AH four color @20200319*/
        if(SystemProperties.get("ro.ysten.province","master").equals("cm201_anhui")){
            int ottKey = 0;
            switch (keyCode) {
                case KeyEvent.KEYCODE_TV:
                    ottKey = 136;
                    break;
                case KeyEvent.KEYCODE_MOVIE:
                    ottKey = 137;
                    break;
                case KeyEvent.KEYCODE_APPS:
                    ottKey = 138;
                    break;
                case KeyEvent.KEYCODE_FAVOURITE:
                    ottKey = 139;
                    break;
            }
            if (ottKey!=0){
                long now = SystemClock.uptimeMillis();
                InputManager mInputManager = (InputManager) mContext.getSystemService(Context.INPUT_SERVICE);
                mInputManager.injectInputEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, ottKey, 0), InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
                mInputManager.injectInputEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, ottKey, 0), InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
            }
            return;
        }
        /*add by zhaolianghua end*/
        String topPackage = getTopActivity(mContext);
        if(!topPackage.equals("com.ysten.sjiptv")) {
            Intent iptvIntent = new Intent();
            iptvIntent.setAction("com.ysten.sjiptv.homepage");
            switch (keyCode) {
                case KeyEvent.KEYCODE_HOME:		
                    iptvIntent.putExtra("intentMsg", "EPGDomain");
                    break;
                case KeyEvent.KEYCODE_TV:
                case KeyEvent.KEYCODE_XIRIRED:					
                    iptvIntent.putExtra("intentMsg", "LIVE_CHANNEL_LIST");
                    break;
                case KeyEvent.KEYCODE_MOVIE:
                case KeyEvent.KEYCODE_XIRIGREEN:		
                    iptvIntent.putExtra("intentMsg", "VOD_CATEGORY_PAGE");
                    break;
                case KeyEvent.KEYCODE_FAVOURITE:
                case KeyEvent.KEYCODE_XIRIBLUE:				
                    iptvIntent.putExtra("intentMsg", "Infomation");
                    break;
            }	
            iptvIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            mContext.startActivityAsUser(iptvIntent, UserHandle.CURRENT);
            return;	
        }
        int IPTVKey=0;
        switch (keyCode) {
            case KeyEvent.KEYCODE_HOME:		
                IPTVKey=181;
                break;
            case KeyEvent.KEYCODE_TV:		
                IPTVKey=183;
                break;
            case KeyEvent.KEYCODE_FAVOURITE:		
                IPTVKey=186;
                break;
            case KeyEvent.KEYCODE_MOVIE:		
                IPTVKey=184;
                break;
            case KeyEvent.KEYCODE_APPS:		
                IPTVKey=185;
                break;
            case KeyEvent.KEYCODE_MENU:		
                IPTVKey=226;
                break;
            case KeyEvent.KEYCODE_VOLUME_MUTE:		
                IPTVKey=91;
                break;
        }
        if (IPTVKey!=0){
            long now = SystemClock.uptimeMillis();
            InputManager inputManager = (InputManager) mContext.getSystemService(Context.INPUT_SERVICE);
            inputManager.injectInputEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, IPTVKey, 0), InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
            inputManager.injectInputEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, IPTVKey, 0), InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        }
    }
	private void freeMemory(String apkNameList[],String topPkgName){
		ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		for(int i = 0;i<apkNameList.length;i++){
			String whiteList = apkNameList[i];
			if(whiteList.equals(topPkgName)){
				Log.v(TAG,"topPkgName in whitelist "+ topPkgName);
                return;
			}
		}
		Log.v(TAG,"topPkgName not in whitelist, kill "+ topPkgName);
        activityManager.forceStopPackage(topPkgName);
	}
}


