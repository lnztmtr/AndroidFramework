
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
import android.app.MboxOutputModeManager;
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
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
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
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UEventObserver;
import android.os.UserHandle;
import android.os.Vibrator;
import android.os.display.DisplayManager;
import android.provider.Settings;
import android.widget.Toast;
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
import android.hardware.input.IInputManager;
import android.hardware.input.InputManager;

import com.android.internal.R;
import com.android.internal.policy.PolicyManager;
import com.android.internal.policy.impl.keyguard.KeyguardServiceDelegate;
import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.telephony.ITelephony;
import com.android.internal.widget.PointerLocationView;

import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.Locale;
import android.text.TextUtils;
import android.provider.Settings;

import static android.view.WindowManager.LayoutParams.*;
import static android.view.WindowManagerPolicy.WindowManagerFuncs.LID_ABSENT;
import static android.view.WindowManagerPolicy.WindowManagerFuncs.LID_OPEN;
import static android.view.WindowManagerPolicy.WindowManagerFuncs.LID_CLOSED;

import android.app.SystemWriteManager;
import android.app.SystemLogManager;
import android.app.IActivityManager;
import android.view.AmlScreenShot;

import com.hisilicon.android.hisysmanager.HiSysManager;

import android.net.Uri;
import android.database.Cursor;
import android.content.ContentValues;
import android.bluetooth.BluetoothAdapter;
import com.softwinner.dragonbox.NativeFetch;
import android.app.DevInfoManager;
import android.app.ActivityThread;
import android.content.pm.IPackageManager;
import android.app.AlertDialog;
import android.widget.TextView;
import android.widget.EditText;
import android.content.DialogInterface;
import java.util.Arrays;

import com.android.internal.policy.impl.CallingDialog.OnclickListener;

class GameDimInfo{
	public GameDimInfo(String Name, int w, int h){
		name = Name;
		width = w;
		height = h;
    }
	@Override
    public String toString(){
		StringBuilder sb = new StringBuilder(128);
		sb.append("GameDimInfo{");
		sb.append(Integer.toHexString(System.identityHashCode(this)));
		sb.append(" ");
		sb.append(name);
		sb.append(" ");
		sb.append(width);
		sb.append(" ");
		sb.append(height);
		sb.append('}');
		return sb.toString();
    }
	public String name;
	public int width;
	public int height;
}




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
    static final boolean DEBUG_ISTV = true;   // add voice recognition

    // Whether to allow dock apps with METADATA_DOCK_HOME to temporarily take over the Home key.
    // No longer recommended for desk docks; still useful in car docks.
    static final boolean ENABLE_CAR_DOCK_HOME_CAPTURE = true;
    static final boolean ENABLE_DESK_DOCK_HOME_CAPTURE = false;

    static final int LONG_PRESS_POWER_NOTHING = 0;
    static final int LONG_PRESS_POWER_GLOBAL_ACTIONS = 1;
    static final int LONG_PRESS_POWER_SHUT_OFF = 2;
    static final int LONG_PRESS_POWER_SHUT_OFF_NO_CONFIRM = 3;

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

    static public final String SYSTEM_DIALOG_REASON_KEY = "reason";
    static public final String SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS = "globalactions";
    static public final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
    static public final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
    static public final String SYSTEM_DIALOG_REASON_ASSIST = "assist";

    public static final String DISPLAY_MODE_PATH = "/sys/class/display/mode";
    public static final String HDMI_PLUG_STATE_PATH="/sys/class/amhdmitx/amhdmitx0/hpd_state";
    public static final String SYS_AVMUTE = "/sys/class/amhdmitx/amhdmitx0/avmute";
    public static final String SYS_HDMIPHY = "/sys/class/amhdmitx/amhdmitx0/phy";
    public static final String HDMI_TX_PLUG_UEVENT="DEVPATH=/devices/virtual/switch/hdmi_hpd";
    private ArrayList<GameDimInfo> GameList = null;
    private int mkeyPicCount = 0;
    private int mkeyAgingDragonboxCount = 0;
    private int mkeyAllappCount = 0;
    private int mkeyAdbCount = 0;
    private String mbootlogopicpath = null;
    private Timer mOberserCecSuspendTimer = null;
    private BluetoothAdapter mBTAdapter = null;
    private int ganSuLoginfalg = 1; // add by ysten xumiao at 20190327:gansu add login homekey 
    private AutoLogMultKeyTrigger mAutoLogMultKeyTrigger;//added by yzs at 20190424:multikey trigger autolog
    /**
     * These are the system UI flags that, when changing, can cause the layout
     * of the screen to change.
     */
    static final int SYSTEM_UI_CHANGING_LAYOUT =
              View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.STATUS_BAR_TRANSLUCENT
            | View.NAVIGATION_BAR_TRANSLUCENT;

    /**
     * Keyguard stuff
     */
    private WindowState mKeyguardScrim;

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
//                KeyEvent.KEYCODE_MUSIC, Intent.CATEGORY_APP_MUSIC);
//        sApplicationLaunchKeyCategories.append(
                KeyEvent.KEYCODE_CALCULATOR, Intent.CATEGORY_APP_CALCULATOR);
        parserHotKeyConfFile();
    }
    
    private static final String HOTKEY_COUNT = "HOTKEY_COUNT";
    private static final String HOTKEY_KEYCODE = "HOTKEY_KEYCODE_";
    private static final String HOTKEY_PACKAGE = "HOTKEY_PACKAGE_";
    private static final String HOTKEY_CLASS = "HOTKEY_CLASS_";

	private class KeyMobiletestMap{
        public KeyMobiletestMap(String pkey, int akey){
            physicskeycode = pkey;
            andriodkeycode = akey;
		}
		public String physicskeycode;
		public int andriodkeycode;
	}
    private ArrayList<KeyMobiletestMap> mMobilekeyMapList = null;

    private  void InitMobileKeyMap()
    {
        if(null == mMobilekeyMapList)
        { 
            KeyMobiletestMap item= null;
            mMobilekeyMapList  = new ArrayList<KeyMobiletestMap>();
            item = new KeyMobiletestMap("dc", 179);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("9c", 164);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("cd", 136);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("91", 137);  
            mMobilekeyMapList.add(item);
              
            item = new KeyMobiletestMap("83", 138);    
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("c3", 139);  
            mMobilekeyMapList.add(item);		
            
            item = new KeyMobiletestMap("86", 167);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("85", 166);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("81", 25);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("80", 24);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("8d", 176);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("82", 82);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("ce", 23);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("ca", 19);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("d2", 20);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("99", 21);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("c1", 22);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("95", 4);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("88", 3);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("92", 8);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("93", 9);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("cc", 10);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("8e", 11);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("8f", 12);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("c8", 13);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("8a", 14);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("8b", 15);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("c4", 16);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("87", 7);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("c0", 67);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("da", 18);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("c9", 175);  
            mMobilekeyMapList.add(item);
            
            item = new KeyMobiletestMap("d9", 85);  
            mMobilekeyMapList.add(item);
        }
	}
	
    private static class HotKeyObj {
        int keyCode = KeyEvent.KEYCODE_UNKNOWN;
        String packageName = null;
        String className = null;
    }
    
    private static class HotCombKeyObj extends HotKeyObj {
        ArrayList<Integer> keyCodes = null;
        int keyCur = 0;
    }

    private static HashMap<Integer, HotKeyObj> mHotKeyMap;
    private static ArrayList<HotCombKeyObj> mHotCombKeyList;

    private static Properties loadProperties(File file) {
        Properties properties = new Properties();

        try {
            FileInputStream input = new FileInputStream(file);
            properties.load(input);
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return properties;
    }

    private static void parserHotKeyConfFile() {
        String mPath = "/system/etc/hotkey.properties";
        
        mHotKeyMap = new HashMap<Integer, HotKeyObj>();
        mHotKeyMap.clear();
        
        mHotCombKeyList = new ArrayList<HotCombKeyObj>();
        mHotCombKeyList.clear();
        
        File mConf = new File(mPath);
        if(mConf.exists()) {
            Properties prop = loadProperties(mConf);
            String hotkeyCountStr = prop.getProperty(HOTKEY_COUNT, "0");
            Log.d(TAG, "hot key count: " + hotkeyCountStr);
            int hotkey_count = 0;
            try {
                hotkey_count = Integer.parseInt(hotkeyCountStr);
            } catch(Exception e) {
                Log.w(TAG, "parser hotkey count error!");
            }
            for(int i=0; i<hotkey_count; i++) {
                String hotkeyCodeStr = prop.getProperty(HOTKEY_KEYCODE + Integer.toString(i+1), null);
                String hotkeyPackStr = prop.getProperty(HOTKEY_PACKAGE + Integer.toString(i+1), null);
                String hotkeyClsStr = prop.getProperty(HOTKEY_CLASS + Integer.toString(i+1), null);
                Log.d(TAG, "hot key code: " + hotkeyCodeStr);
                Log.d(TAG, "hot key package: " + hotkeyPackStr);
                Log.d(TAG, "hot key class: " + hotkeyClsStr);
                if((hotkeyCodeStr != null) && (hotkeyPackStr != null) && (hotkeyClsStr != null)) {
                    int keycode = KeyEvent.KEYCODE_UNKNOWN;
                    ArrayList<Integer> keycodes = new ArrayList<Integer>();
                    int keycount = 0;
                    try {
                        String[] keyArray = hotkeyCodeStr.split("/");
                        for(int j=0; j<keyArray.length; j++) {
                            keycode = Integer.parseInt(keyArray[j]);
                            //Log.d(TAG, "Index " + j + ": " + keyArray[j] + " " + keycode);
                            if(keycode > KeyEvent.KEYCODE_UNKNOWN)
                                keycodes.add(keycode);
                        }
                    } catch(Exception e) {
                        Log.w(TAG, "parser hotkey keycode error!");
                    }
                    keycount = keycodes.size();
                    if(keycount == 1) {
                        HotKeyObj mHotKey = new HotKeyObj();
                        mHotKey.keyCode = keycodes.get(0);
                        mHotKey.packageName = hotkeyPackStr;
                        mHotKey.className= hotkeyClsStr;
                        mHotKeyMap.put(keycode, mHotKey);
                    } else if(keycount > 1) {
                        HotCombKeyObj mHotCombKey = new HotCombKeyObj();
                        mHotCombKey.keyCodes = keycodes;
                        mHotCombKey.keyCur = 0;
                        mHotCombKey.packageName = hotkeyPackStr;
                        mHotCombKey.className= hotkeyClsStr;
                        mHotCombKeyList.add(mHotCombKey);
                    }
                }
            }
        } else {
            Log.w(TAG, mPath + " is not exist!!!");
        }
    }
    /**
     * Lock protecting internal state.  Must not call out into window
     * manager with lock held.  (This lock will be acquired in places
     * where the window manager is calling in with its own lock held.)
     */
    private final Object mLock = new Object();

    Context mContext;
    IWindowManager mWindowManager;
    static  WindowManagerFuncs mWindowManagerFuncs;
    ActivityManager mActivityManager;
    PowerManager mPowerManager;
    DisplayManager mDisplayManager;
    IStatusBarService mStatusBarService;
    boolean mPreloadedRecentApps;
    final Object mServiceAquireLock = new Object();
    Vibrator mVibrator; // Vibrator for giving feedback of orientation changes
    SearchManager mSearchManager;
    MboxOutputModeManager mMboxOutputModeManager;
    SystemWriteManager mSystemWriteManager;
    AudioManager mAudioManager;
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
    int mStatusBarHeight;
    WindowState mNavigationBar = null;
    boolean mHasNavigationBar = false;
    boolean mCanHideNavigationBar = false;
    boolean mNavigationBarCanMove = false; // can the navigation bar ever move to the side?
    boolean mNavigationBarOnBottom = true; // is the navigation bar on the bottom *right now*?
    int[] mNavigationBarHeightForRotation = new int[4];
    int[] mNavigationBarWidthForRotation = new int[4];

    WindowState mKeyguard = null;
    KeyguardServiceDelegate mKeyguardDelegate;
    GlobalActions mGlobalActions;
    volatile boolean mPowerKeyHandled; // accessed from input reader and handler thread
    boolean mPendingPowerKeyUpCanceled;
    boolean mSendHomeKeyTag = false;
    boolean mPowerKeyLongEventHandled;
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
    boolean mHdmiHwPlugged;
    final Object mHdmiHwPluggedLock = new Object();
    boolean mHdmiPlugged;
    boolean mDisplayChanged = false;
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
    SystemLogManager mSystemLogManager;
    private boolean mIsLogRecorded;

    // The last window we were told about in focusChanged.
    WindowState mFocusedWindow;
    IApplicationToken mFocusedApp;
    boolean mbPackageExist = false;
    boolean mNavShow = true;
    boolean suspendbycec = false;
    HiSysManager mhisys =  new HiSysManager();

    private final class PointerLocationPointerEventListener implements PointerEventListener {
        @Override
        public void onPointerEvent(MotionEvent motionEvent) {
            if (mPointerLocationView != null) {
		try {
                mPointerLocationView.addPointerEvent(motionEvent);
		}
		catch(IndexOutOfBoundsException e) {
	                Log.e(TAG, "IndexOutOfBoundsException error when on     PointerEvent call addPointerEvent " + e.getMessage());
                }
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
    boolean mMenuKeyUp;
    boolean mAssistKeyLongPressed;
    //add by liuxl at 20181127 for a20
    boolean mKeyMicFlag;
    long mKeyMicPressTime = 0;
	//add by ysten-mark
	boolean isIcntvLive = false;

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
    boolean isTvSuspend = false;

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

    /* The number of steps between min and max brightness */
    private static final int BRIGHTNESS_STEPS = 10;

    SettingsObserver mSettingsObserver;
    ShortcutManager mShortcutManager;
    PowerManager.WakeLock mBroadcastWakeLock;
    boolean mHavePendingMediaKeyRepeatWithWakeLock;

    private int mCurrentUserId;

    //QB featrue
    final Object mKeyDispatchLock = new Object();
    public static final String QB_DISABLE = "android.intent.action.ACTION_BOOT_QB";
    public static final String QB_ENABLE = "android.intent.action.ACTION_SHUTDOWN_QB";
    public static final String QB_TURNON_SCREEN = "android.intent.action.ACTION_TURNON_SCREEN_QB";
    public static final String QB_TURNOFF_SCREEN = "android.intent.action.ACTION_TURNOFF_SCREEN_QB";
    static final int KEY_DISPATCH_MODE_ALL_ENABLE = 0;
    static final int KEY_DISPATCH_MODE_ALL_DISABLE = 1;
    int mKeyDispatcMode = KEY_DISPATCH_MODE_ALL_ENABLE;

    // Maps global key codes to the components that will handle them.
    private GlobalKeyManager mGlobalKeyManager;

    // Fallback actions by key code.
    private final SparseArray<KeyCharacterMap.FallbackAction> mFallbackActions =
            new SparseArray<KeyCharacterMap.FallbackAction>();

    private static final int MSG_ENABLE_POINTER_LOCATION = 1;
    private static final int MSG_DISABLE_POINTER_LOCATION = 2;
    private static final int MSG_DISPATCH_MEDIA_KEY_WITH_WAKE_LOCK = 3;
    private static final int MSG_DISPATCH_MEDIA_KEY_REPEAT_WITH_WAKE_LOCK = 4;
    private static final int MSG_UPDATE_BOOT_LOGO = 5;
    private static final int MSG_OPEN_BLUETOOTH_SWICH = 6;
    private static final int MSG_OPEN_WIFI_DIALOG = 7;
    private static final int MSG_HOME_OPEN_CALLING_DIALOG = 8;
    private static final int MSG_SETTING_OPEN_CALLING_DIALOG = 9;
    private static final int MSG_CALLING_DIALOG_DELAY_DISMISS = 10;

	//add by ysten.huanghongyan 2018.11.28 for CM201_henan
	private static final int SLEEP_DELAY_UNIT=60*1000;
    private static Handler jsHandler;
    private static boolean mShowPowerDialog = false;
    private static Context jsContext;
	private int wifiCount=0;
    //add by ysten zhuhengxuan at 20190413 for 江西广告apk下发开机logo
    private String strlineNew = "";
    //begin:add by zhanghk at 20190916:add jiangsu app white list
    String apkNameListJS[] = {
		"tv.icntv.ott",
		"com.ysten.systemupdate",
		"com.ysten.hmdemo",
		"com.ysten.setting",
                "com.ysten.tr069",
                "com.allwinnertech.dragonter",
                "com.gitv.launcher",
                "com.chinamobile.activate",
                "com.chinamobile.launcherjs",
                "com.softwinner.agingdragonbox",
                "com.softwinner.dragonbox",
                "com.ysten.secure",
                "com.aspirecn.jshdc.appstore",
                "com.ysten.filebrowser"
    };
    //end:add by zhanghk at 20190916:add jiangsu app white list


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
                case MSG_UPDATE_BOOT_LOGO:
                    mMboxOutputModeManager.updateLogo(mbootlogopicpath);
                    Log.i(TAG,"MSG_UPDATE_BOOT_LOGO update ["+mbootlogopicpath+"]");
                    break;
                case MSG_OPEN_BLUETOOTH_SWICH:
                    File dir = new File("/data/data/com.btdev.btdevmanager");
                    if (null != dir) {
                        if (!dir.exists()) {
                            Log.i(TAG,"need to enable bt first");
                            if (null == mBTAdapter) {
                                mBTAdapter = BluetoothAdapter.getDefaultAdapter();
                            }
                            if (null != mBTAdapter) {
                                if (mBTAdapter.STATE_OFF == mBTAdapter.getState()) {
                                    mBTAdapter.enable();
                                }
                            }
                        }
                    }
                    Log.d(TAG,"send KehWin bluetooth autopair broadcast");
                    Intent inten = new Intent("com.btdev.action.PARING_REMOTECONTROL");
                    mContext.sendStickyBroadcastAsUser(inten, UserHandle.ALL);

                    Log.d(TAG,"send Xiri bluetooth autopair broadcast");
                    Intent inten1 = new Intent("com.android.BluetoothMTKAutoPairService.show");
                    mContext.sendStickyBroadcastAsUser(inten1, UserHandle.ALL);
 					break;
				case MSG_OPEN_WIFI_DIALOG:
					showWifiDialog();
                    break;
                case MSG_HOME_OPEN_CALLING_DIALOG:
                    showCallingDialog("home");
                    break;
                case MSG_SETTING_OPEN_CALLING_DIALOG:
                    showCallingDialog("setting");
                    break;
                case MSG_CALLING_DIALOG_DELAY_DISMISS:
                    Log.d("sjh", "MSG_callingDialog=" + callingDialog + ", callingDialog.isShowing()=" + callingDialog.isShowing() + ", time=" + time);
                    if (callingDialog != null && callingDialog.isShowing()) {
                        if (time <= 0) {
                            callingDialog.btn_No.performClick();
                            //callingDialog = null;
                        } else {
                            time--;
                            mHandler.sendEmptyMessageDelayed(MSG_CALLING_DIALOG_DELAY_DISMISS, 1000);
                            /* not show countdown 
                            if (time <= 5) {
                                callingDialog.btn_No.setText("否(" + String.valueOf(time) + ")");
                            }*/
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private UEventObserver mCedObserver = new UEventObserver() {
		@Override
		public void onUEvent(UEventObserver.UEvent event) {

			String mNewLanguage = event.get("SWITCH_STATE");
			Log.d(TAG, "get the language code is : " + mNewLanguage);
			int i = -1;
			String[] cec_language_list = mContext.getResources().getStringArray(
					com.android.internal.R.array.cec_language);
			for (int j = 0; j < cec_language_list.length; j++) {
				if (mNewLanguage != null
						&& mNewLanguage.trim().equals(cec_language_list[j])) {
					i = j;
					break;
				}
			}
			if (i >= 0) {
				String[] language_list = mContext.getResources().getStringArray(
						com.android.internal.R.array.language);
				String[] country_list = mContext.getResources().getStringArray(
						com.android.internal.R.array.country);
					Locale l = new Locale(language_list[i] , country_list[i]);
					//Log.d(TAG, "change the language right now !!!");
					//updateLanguage(l);
			} else {
				Log.d(TAG, "the language code is not support right now !!!");
			}
		}
    };

    private UEventObserver mCecSuspendObserver = new UEventObserver() {
		@Override
		public void onUEvent(UEventObserver.UEvent event) {
			if ("1".equals(event.get("SWITCH_STATE"))) {
				Log.d(TAG , "suspend by cec");
				suspendbycec = true;
				int mtime = SystemProperties.getInt("persist.sys.autosuspend.cec.time", 0);
				if (0 == mtime) {
					mPowerManager.goToSleep(SystemClock.uptimeMillis());
				} else if (0 < mtime) {
				try{
					if (null == mOberserCecSuspendTimer) {
						mOberserCecSuspendTimer = new Timer();
					} else {
						mOberserCecSuspendTimer.cancel();
						mOberserCecSuspendTimer = null;
						mOberserCecSuspendTimer = new Timer();
					}
					TimerTask task = new TimerTask() {
						public void run() {
							mOberserCecSuspendTimer.cancel();
							mOberserCecSuspendTimer = null;
							mPowerManager.goToSleep(SystemClock.uptimeMillis());
						}
					};
					mOberserCecSuspendTimer.schedule(task, mtime);
					} catch(Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
    };

   private UEventObserver mHdrObserver = new UEventObserver() {
		@Override
		public void onUEvent(UEventObserver.UEvent event) {
			if("0".equals(event.get("SWITCH_STATE"))){
				Log.d(TAG , "close hdr mode");
				try {
					writeSysfs(SYS_AVMUTE , "1");
					Thread.sleep(100);
					writeSysfs(SYS_HDMIPHY , "0");
					Thread.sleep(200);
					writeSysfs(SYS_HDMIPHY , "1");
					Thread.sleep(50);
					writeSysfs(SYS_AVMUTE , "-1");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
    };
	
    private UEventObserver mHDMIObserver = new UEventObserver() {
        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            Log.d(TAG , "mHDMIObserver");
            setHdmiHwPlugged("1".equals(event.get("SWITCH_STATE")));
        }
    };

    private Timer mOberserRxsenceTimer = null;
    private  UEventObserver mRxSenseObserver = new UEventObserver() {
        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            Log.d(TAG, " enter mRxSenseObserver when tv suspend");
            try{
				//add by liuxl at 20181128 for A20 start
				String isA20 = SystemProperties.get("persist.sys.isa20", "false");
				if("true".equals(isA20)){
					String rx_sense = mSystemWriteManager.readSysfs("/sys/class/switch/hdmi_rxsense/state");
					String hpd_state = mSystemWriteManager.readSysfs("/sys/class/amhdmitx/amhdmitx0/hpd_state");

					Log.d(TAG, "111hpd_state  =" + hpd_state + "	rx_sense  =" + rx_sense);
					if(rx_sense.contains("0") && hpd_state.contains("1")){
						Log.d(TAG, "switchSoundOutput hpd_state");
						switchSoundOutput(false);					
					}
				}
				//add by liuxl at 20181128 for A20 end
                if (null == mOberserRxsenceTimer) {
                    mOberserRxsenceTimer = new Timer();
                } else {
                    mOberserRxsenceTimer.cancel();
                    mOberserRxsenceTimer = null;
                    mOberserRxsenceTimer = new Timer();
                }
                TimerTask task = new TimerTask() {
                    public void run() {
                        String rx_sense = mSystemWriteManager.readSysfs("/sys/class/switch/hdmi_rxsense/state");
                        String hpd_state = mSystemWriteManager.readSysfs("/sys/class/amhdmitx/amhdmitx0/hpd_state");

                        Log.d(TAG, "hpd_state  =" + hpd_state + "	rx_sense  =" + rx_sense);
                        if (rx_sense.contains("0") && hpd_state.contains("1")) {
                            isTvSuspend = true;
                            setHdmiHwPlugged(true);
                        } else if (SystemProperties.getBoolean("persist.sys.autosuspend.cec.enable", false)
                                  && rx_sense.contains("1")
                                  && hpd_state.contains("1")) {
                                  disableCecSuspend();
                        }
                        mOberserRxsenceTimer.cancel();
                        mOberserRxsenceTimer = null;
                    }
                };
                mOberserRxsenceTimer.schedule(task, 5000);
             }catch(Exception e){
                 e.printStackTrace(); 
             }
        }
    };

    private UEventObserver mHoldObserver = new UEventObserver() {
        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            Log.d("PhoneWindownManager","holdkey"+event.get("SWITCH_STATE"));
            try {
                IWindowManager wm = IWindowManager.Stub.asInterface(
                    ServiceManager.getService(Context.WINDOW_SERVICE));
                if("1".equals(event.get("SWITCH_STATE"))){
                    wm.thawRotation();
                }else{
                    wm.freezeRotation(-1);//use current orientation
                }
            } catch (RemoteException exc) {
                Log.w(TAG, "Unable to save auto-rotate setting");
            }
        }
    };

    BroadcastReceiver mHdmiPluggedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            boolean plugged
                = intent.getBooleanExtra(WindowManagerPolicy.EXTRA_HDMI_PLUGGED_STATE, false); 
            setHdmiPlugged(plugged);
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
	     resolver.registerContentObserver(Settings.Secure.getUriFor(
                    Settings.Secure.DEFAULT_PLAYER_QUALITY), false, this,
                    UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.Global.getUriFor(
                    Settings.Global.DISPLAY_OUTPUTMODE_AUTO), false, this,
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
    private void updateLanguage(Locale locale) {
	try {
		//if(null != mActivityManager){
			Configuration config = ActivityManagerNative.getDefault().getConfiguration();
			config.locale = locale;
			ActivityManagerNative.getDefault().updateConfiguration(config);
		//}
        } catch (RemoteException e) {}
    }
    private void interceptPowerKeyDown(boolean handled) {
        mPowerKeyHandled = handled;
        if (!handled) {
            // begin: add by tianchining at 20190327: handle PowerKey
            //mHandler.postDelayed(mPowerLongPress, ViewConfiguration.getGlobalActionKeyTimeout());
            long time=ViewConfiguration.getGlobalActionKeyTimeout();
            if(SystemProperties.get("ro.ysten.province","master").contains("ningxia")){
                time=5000;
            }else if(SystemProperties.get("ro.ysten.province","master").contains("hubei")){
                time=3000;
            }
            mHandler.postDelayed(mPowerLongPress, time);
            //end: add by tianchining at 20190327: handle PowerKey
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
    private Toast mDefault720PToast = null;
    private Toast no720pToast = null;
    private Toast cvbToast = null;
    private Toast batteryToast = null;

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
                mPowerKeyHandled = true;
                if (!performHapticFeedbackLw(null, HapticFeedbackConstants.LONG_PRESS, false)) {
                    performAuditoryFeedbackForAccessibilityIfNeed();
                }
                sendCloseSystemWindows(SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS);
                showGlobalActionsDialog();
                break;
            case LONG_PRESS_POWER_SHUT_OFF:
            case LONG_PRESS_POWER_SHUT_OFF_NO_CONFIRM:
                String proj_type = SystemProperties.get("sys.proj.type", "ott");
                Log.d(TAG, "press long power key, proj_type is " + proj_type);
                if ("mobile".equals(proj_type)) {
                    //begin:add by zhanghk at 20191205:start poweroff.apk when long press power key
                    if(SystemProperties.get("ro.ysten.province","master").contains("hubei")){
                        Intent poweroffIntent = new Intent();  
                        poweroffIntent.setAction("com.ysten.action.poweroff"); 
                        mContext.startService(poweroffIntent);  
                        mPowerKeyHandled = true;
                        mPowerKeyLongEventHandled = true;
                        break;
                    }
                    //end:add by zhanghk at 20191205:start poweroff.apk when long press power key  
                    String hdmi_pluged = mSystemWriteManager.readSysfs(HDMI_PLUG_STATE_PATH);
                    if(hdmi_pluged != null && hdmi_pluged.contains("1")) {
                        if(mDisplayManager.isSupportStandard(8)) { //DISPLAY_STANDARD_720P_50
                            SystemProperties.set("ubootenv.var.hdmimode","720p50hz");
                            mPowerKeyHandled = true;
			                mPowerKeyLongEventHandled = true;
                            mDisplayManager.getScreenMargin();
                            if((mFocusedApp != null) && (mFocusedApp.toString() != null)){
                                String mFocusApp = mFocusedApp.toString();
                                String mSettingPkg = new String("net.sunniwell.app.swsettings.chinamobile/.SWSettingsActivity");
                                if(mFocusApp.contains(mSettingPkg)){
                                    Slog.w(TAG, "--focus app is SWSettingsActivity,restart");
                                    mDisplayManager.setDisplayStandardWithSettingRestart(8);
                                } else {
                                    mDisplayManager.setDisplayStandard(8);
                                }
                            } else {
                                mDisplayManager.setDisplayStandard(8);
                            }
                            if(null == mDefault720PToast){
                                mDefault720PToast = Toast.makeText(mContext, R.string.outputmode_recover, Toast.LENGTH_LONG);
                                mDefault720PToast.setGravity(Gravity.CENTER, 0, 0);
                            }
                            if(mDefault720PToast.getView()!=null && mDefault720PToast.getView().isShown())
                                break;
                            else
                                mDefault720PToast.show();

                        } else {
                            mPowerKeyHandled = true;
			    mPowerKeyLongEventHandled = true;
				if (null == no720pToast) {
					no720pToast =Toast.makeText(mContext, R.string.outputmode_recover_deny1, Toast.LENGTH_LONG);
					no720pToast.setGravity(Gravity.CENTER, 0, 0);
				}
				if (no720pToast.getView() != null && no720pToast.getView().isShown())
					break;
				else
					no720pToast.show();
                        }
                    } else {
                        mPowerKeyHandled = true;
			mPowerKeyLongEventHandled = true;
                        if (null == cvbToast) {
				cvbToast = Toast.makeText(mContext, R.string.outputmode_recover_deny, Toast.LENGTH_LONG);
				cvbToast.setGravity(Gravity.CENTER, 0, 0);
			}
			if (cvbToast.getView() != null && cvbToast.getView().isShown())
				break;
			else
				cvbToast.show();
                    }
                } else {
                    //telecom unicom ott use it
                    mPowerKeyHandled = true;
                    performHapticFeedbackLw(null, HapticFeedbackConstants.LONG_PRESS, false);
                    mWindowManagerFuncs.shutdown(resolvedBehavior == LONG_PRESS_POWER_SHUT_OFF);
                }
                break;
            }
        }
    };

    private final Runnable showAdbToast = new Runnable() {
        public void run() {
            Toast mToast =Toast.makeText(mContext, "You have entered ADB mode!", Toast.LENGTH_LONG);
            mToast.setGravity(Gravity.CENTER, 0, 0);
            mToast.show();
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
	
    //add by zhanghk at 20181017 begin:press some key to start allapp
    private final Runnable startAllApp = new Runnable() {
        public void run() {
                Log.d("zhanghk","start allapp");
                Intent intent = new Intent();         
                intent.setComponent(new ComponentName("com.ysten.demo", "com.ysten.demo.AllApp"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
        }
    };
    //add by zhanghk at 20181017 end:press some key to start allapp

    private final Runnable mPowerShortPress = new Runnable() {
        public void run() {
            // begin: add by ysten.tianchining at 20190327: showSleepDelayDialog
            if(SystemProperties.get("ro.ysten.province","master").contains("ningxia")){
                showSleepDelayDialog();
                return;
            }
            //end: add by ysten.tianchining at 20190327: showSleepDelayDialog
            sendCloseSystemWindows(SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS);
            mWindowManagerFuncs.shutdown(true);
        }
    };

    private final Runnable mSendHomeKey = new Runnable(){
        public void run(){
            mSendHomeKeyTag = true;
            sendKeyEvent(KeyEvent.KEYCODE_HOME);
        }
    };

    private final Runnable mScreenshotRunnable = new Runnable() {
        @Override
        public void run() {
            takeScreenshot();
        }
    };

    /*For now, only support PNG file*/
    private final Runnable mAmlScreenshotRunnable = new Runnable() {
        @Override
        public void run() {
            if (getCurDisplayMode().contains("1080"))
				AmlScreenShot.screenshot("/tmp/picinfo/aa.png",1920,1080);
			else if (getCurDisplayMode().contains("720"))
				AmlScreenShot.screenshot("/tmp/picinfo/aa.png",1280,720);
			else
				AmlScreenShot.screenshot("/tmp/picinfo/aa.png",1920,1080);
        }
    };

    final Runnable remoteControlLowBatteryToastRunnable = new Runnable() {
        @Override
        public void run() {
            if (null == batteryToast) {
                 batteryToast = Toast.makeText(mContext, R.string.remote_control_battery_low, Toast.LENGTH_LONG);
                 batteryToast.setGravity(Gravity.CENTER, 0, 0);
             }
             if (batteryToast.getView() == null || !batteryToast.getView().isShown())
                 batteryToast.show();
        }
     };

    private final Runnable analogKeyRunnable = new Runnable() {
        @Override
        public void run() {
			Log.d(TAG,"analog KeyEvent.KEYCODE_TV");
		    long now = SystemClock.uptimeMillis();
		    KeyEvent keyEvent = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_TV, 0);
		    InputManager inputManager = (InputManager) mContext.getSystemService(Context.INPUT_SERVICE);
		    inputManager.injectInputEvent(keyEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
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
        } else if ("m202".equals(SystemProperties.get("ro.product.name")) && !mMapMenuKey) {
            mMapMenuKey = true;
            sendMapKey(KeyEvent.KEYCODE_MENU, true);
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
    private boolean checkPackageExist(String strPackageName)
	  {
	  	if (strPackageName == null || "".equals(strPackageName))
	  		return false;
	  	try {
	  		ApplicationInfo info = mContext.getPackageManager().getApplicationInfo(strPackageName, PackageManager.GET_UNINSTALLED_PACKAGES);
	            return true;
	        } catch (NameNotFoundException e) {
	            return false;
	        }
		}

	private final class Observer extends ContentObserver {	
		  
			public Observer(Handler handler) {	
				super(handler);  
				
			}  
		  
			@Override  
			public void onChange(boolean selfChange, Uri uri) {  

				String name	= null;
				Log.i(TAG,"onChange --------- pic change -----------");
				Cursor cursor = mContext.getContentResolver().query(  
						Uri.parse("content://stbconfig/authentication/BootFilesDir"), null, null, null, null);	
				if(cursor != null)
				{
					while(cursor.moveToNext())
					{
					    mbootlogopicpath = cursor.getString(cursor.getColumnIndex("value"));
					}
					cursor.close();
					Message msg = Message.obtain();
			        msg.what = MSG_UPDATE_BOOT_LOGO;
					mHandler.sendMessage(msg);
					//mMboxOutputModeManager.updateLogo(mbootlogopicpath);
					Log.i(TAG,"onChange --------- pic change path ["+mbootlogopicpath+"]");
				}
	
				}  
			}  
	
	
    /** {@inheritDoc} */
    @Override
    public void init(Context context, IWindowManager windowManager,
            WindowManagerFuncs windowManagerFuncs) {
        mContext = context;
        jsContext=context;
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
        String jsmobile = SystemProperties.get("ro.jsmobile.launcher" , "");
        String def_launcher_pkg = SystemProperties.get("sys.deflauncher.pkg" , null);
        String def_launcher_cls = SystemProperties.get("sys.deflauncher.cls" , null);
        if ((def_launcher_pkg != null) && (def_launcher_cls != null)) {
            mbPackageExist = checkPackageExist(def_launcher_pkg);
        } else
        	mbPackageExist = false;
        if(mbPackageExist) {
            mHomeIntent  =  new Intent(Intent.ACTION_MAIN);
            ComponentName componentName = new ComponentName(def_launcher_pkg, def_launcher_cls);
            mHomeIntent.addCategory(Intent.CATEGORY_HOME);
            mHomeIntent.setComponent(componentName);
        } else {
            mHomeIntent =  new Intent(Intent.ACTION_MAIN, null);
            mHomeIntent.addCategory(Intent.CATEGORY_HOME);
        }
        mHomeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        //begin:add by zhanghk at 20190530:add Intent.CATEGORY_LAUNCHER for Huawei Launcher 
        if(SystemProperties.get("ro.ysten.province","master").contains("CM201_zhejiang")){
	    mHomeIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        }
        //end:add by zhanghk at 20190530:add Intent.CATEGORY_LAUNCHER for Huawei Launcher 
        mCarDockIntent =  new Intent(Intent.ACTION_MAIN, null);
        mCarDockIntent.addCategory(Intent.CATEGORY_CAR_DOCK);
        mCarDockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        mDeskDockIntent =  new Intent(Intent.ACTION_MAIN, null);
        mDeskDockIntent.addCategory(Intent.CATEGORY_DESK_DOCK);
        mDeskDockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

        mActivityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);

        mPowerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mDisplayManager = (DisplayManager)context.getSystemService(Context.DISPLAY_MANAGER_SERVICE);
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

	IntentFilter keyDispatchFilter = new IntentFilter();
        keyDispatchFilter.addAction(QB_ENABLE);
        keyDispatchFilter.addAction(QB_DISABLE);
        keyDispatchFilter.addAction(QB_TURNON_SCREEN);
        keyDispatchFilter.addAction(QB_TURNOFF_SCREEN);
        context.registerReceiver(mKeyDispatchReceiver, keyDispatchFilter);

        // register for dream-related broadcasts
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DREAMING_STARTED);
        filter.addAction(Intent.ACTION_DREAMING_STOPPED);
        context.registerReceiver(mDreamReceiver, filter);

        // register for multiuser-relevant broadcasts
        filter = new IntentFilter(Intent.ACTION_USER_SWITCHED);
        context.registerReceiver(mMultiuserReceiver, filter);

        // register for hdmi plugged events
        filter = new IntentFilter();
        filter.addAction(WindowManagerPolicy.ACTION_HDMI_PLUGGED);
        intent = context.registerReceiver(mHdmiPluggedReceiver, filter);
        if (intent != null) {
            // Retrieve current sticky dock event broadcast.
            boolean plugged = intent.getBooleanExtra(WindowManagerPolicy.EXTRA_HDMI_PLUGGED_STATE, false);
                    
            // This dance forces the code in setHdmiPlugged to run.
            // Always do this so the sticky intent is stuck (to false) if there is no hdmi.
            mHdmiPlugged = !plugged;
            setHdmiPlugged(!mHdmiPlugged);
        }

        filter = new IntentFilter();
        filter.addAction("com.droidlogic.instaboot.RESTORE_COMPLETED");
        context.registerReceiver(mInstabootReceiver, filter);

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
        mMboxOutputModeManager = (MboxOutputModeManager) mContext.getSystemService(Context.MBOX_OUTPUTMODE_SERVICE);
	//begin by ysten zhuhengxuan at 20190413 for 江西广告apk 下发第一张图作为开机logo
	if(SystemProperties.get("ro.ysten.province").contains("jiangxi") && SystemProperties.get("persist.sys.launcher.value","0").equals("1")){
		mHandler.postDelayed(changeLogoRunnable,8*60*1000);
		Log.i(TAG, "----zhuhengxuan----8分钟后开启更新开机logo线程");
	}
	//end by ysten zhuhengxuan at 20190413 for 江西广告apk 下发第一张图作为开机logo

        initializeHdmiState();
        if (!("unicom".equals(SystemProperties.get("sys.proj.type")))){
            initHdmiRxSenseState();
        }
        initializedHoldkeyState( windowManager);
        initializedHdmiCec();
        initializedHdmiCecSuspend();
        initializedHdmiHDR();
        mSystemWriteManager = (SystemWriteManager) mContext.getSystemService(Context.SYSTEM_WRITE_SERVICE);
        mAudioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        // Match current screen state.
        if (mPowerManager.isScreenOn()) {
            screenTurningOn(null);
        } else {
            screenTurnedOff(WindowManagerPolicy.OFF_BECAUSE_OF_USER);
        }
//add by ysten.huanghongyan 2018.11.27 for CM201_henan
        if(SystemProperties.get("ro.ysten.province","master").contains("CM201_henan")
            //add by zhanghk at 20190522:for 4 hour standby
            ||SystemProperties.get("ro.ysten.province","master").contains("CM201_zhejiang")
			//add by huxiang at 20190524 for jike default 4 hour sleepTime
			||SystemProperties.get("ro.ysten.province","master").contains("CM202_jike")){
	        filter = new IntentFilter();
            filter.addAction("com.ysten.delay.poweroff");
	        context.registerReceiver(mDelayPowerOffReceiver, filter);
	        mHandler.removeCallbacks(mSystemSleep);
	        int sleepDelay = Settings.System.getInt(mContext.getContentResolver(),SYSTEM_SLEEP_TIME_DELAY,-1);
	      if(sleepDelay == -1){
	 	    Settings.System.putInt(mContext.getContentResolver(),SYSTEM_SLEEP_TIME_DELAY,240);
	      }
          if(sleepDelay > 0){
		    mHandler.postDelayed(mSystemSleep,sleepDelay*SLEEP_DELAY_UNIT); 
		  }
        }
	    //end by ysten.huanghongyan 2018.11.27 for CM201_henan 
        // begin: add by ysten.tianchining at 20180325: add mGoToSleepReceiver for ningxia
        else if(SystemProperties.get("ro.ysten.province","master").contains("ningxia")){
             filter = new IntentFilter();
            filter.addAction("com.ysten.ningxia.GoToSleep");
            context.registerReceiver(mGoToSleepReceiver, filter);

        }
        //end: add by ysten.tianchining at 20180325:add mGoToSleepReceiver for ningxia
        //tellen 20130201 add for parse key map file
        parseMapFile();
        mSystemLogManager = (SystemLogManager) mContext.getSystemService(Context.SYSTEM_LOG_SERVICE);
        if(mSystemLogManager != null && !mIsLogRecorded){
			mIsLogRecorded = true;
			mSystemLogManager.startRecord();
        }
        //begin by ysten.zhangjunjian ,20181128,add for hubei
        if (SystemProperties.get("ro.ysten.province","master").equals("CM201_hubei")) { 
		    Log.d(TAG, "mRefershTokenReceiver add com.iptv.refersh.token ");
            filter = new IntentFilter();
            filter.addAction("com.iptv.refersh.token");
            context.registerReceiver(mRefershTokenReceiver, filter);	
        }	
        //end by ysten.zhangjunjian,20181128,for hubei
        if(SystemProperties.get("ro.ysten.custom.type","none").equals("viper"))
        {
            Log.d(TAG, "custom.type:viper1");
            //add by wangxin
            filter = new IntentFilter();
            filter.addAction("com.ysten.viper.startplay");
            filter.addAction("com.ysten.viper.endplay");
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            context.registerReceiver(mScreenSaverReceiver, filter);
            Log.d(TAG, "PhoneWindowManager init()");
            mHandler.removeCallbacks(mLaunchScreenSaver);
            int delay = Settings.System.getInt(mContext.getContentResolver(), SCREEN_SAVER_DELAY, -1);
            if(delay > 0){
                mHandler.postDelayed(mLaunchScreenSaver, delay*60*1000);
            } 
        //add by tanhy
         mHandler.removeCallbacks(mSystemSleep);
          int sleepDelay = Settings.System.getInt(mContext.getContentResolver(),SYSTEM_SLEEP_TIME_DELAY,-1);

              if(sleepDelay > 0)
                  mHandler.postDelayed(mSystemSleep,sleepDelay*60*1000);
        }

        startkeepcputemp(0 , 1 * 60 * 1000);
		//listen on sd data for auto change bootlogo 
		mContext.getContentResolver().registerContentObserver(	
		Uri.parse("content://stbconfig/authentication/BootFilesDir"),
		false,  
		new Observer(mHandler));  

		//for 2017 mobile cc key test
		InitMobileKeyMap();
        mAutoLogMultKeyTrigger = new AutoLogMultKeyTrigger(mContext);//added by yzs at 20190424:multikey trigger autolog
		/*begin:add by zhanghk at 20191009:timing analog key to prevent network disconnect*/
		if(SystemProperties.get("ro.ysten.province","master").contains("homeschool")){
			mHandler.removeCallbacks(analogKeyRunnable);
			int analogKeyTime = SystemProperties.getInt("persist.sys.analogkey.time",-1);
			if(analogKeyTime > 0)
			    mHandler.postDelayed(analogKeyRunnable, analogKeyTime*60*1000);
		}
		/*end:add by zhanghk at 20191009:timing analog key to prevent network disconnect*/
    }

	    private boolean mInPlaying = false;
    BroadcastReceiver mScreenSaverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        if(SystemProperties.get("ro.ysten.custom.type","none").equals("viper"))
        {
            Log.d(TAG, "custom.type:viper2");
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
			if(sleepDelay > 0)
				mHandler.postDelayed(mSystemSleep,sleepDelay*60*1000);
		}else if(action.equals(Intent.ACTION_SCREEN_OFF))
		  {       
                        Log.i("THY","PhoneWindowManager receive broadcast: Intent.ACTION_SCREEN_OFF");          
        
                }
            }
        }
        }
    };

    private void startkeepcputemp(long delay, long period){
        Timer keeptimer = new Timer();
        keeptimer.schedule(
            new TimerTask(){
                public void run(){ ///sys/class/thermal/thermal_zone0/temp
                    String temp = mSystemWriteManager.readSysfs("/sys/class/thermal/thermal_zone0/temp");
                    Slog.d(TAG , "get cpu temp is :"+temp);
                    SystemProperties.set("persist.sys.cpu.temp", temp);
                }
            },
            delay,
            period);
    }
//add by ysten.huanghongyan 2018.11.27 for CM201_henan
	BroadcastReceiver mDelayPowerOffReceiver = new BroadcastReceiver() {
       @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
           Log.d(TAG, "onReceive action=" + action);
            if(!TextUtils.isEmpty(action)){
                if(action.equals("com.ysten.delay.poweroff")){
                   mHandler.removeCallbacks(mSystemSleep);
               	  int sleepDelay = Settings.System.getInt(mContext.getContentResolver(),SYSTEM_SLEEP_TIME_DELAY,-1); 

                          if(sleepDelay > 0)
                	          mHandler.postDelayed(mSystemSleep,sleepDelay*SLEEP_DELAY_UNIT);
                }
            }
        }
    };
    //end by ysten.huanghongyan 2018.11.27 for CM201_henan 

    // begin: add by ysten.tianchining at 20190325: register BroadcastReceiver
    BroadcastReceiver mGoToSleepReceiver = new BroadcastReceiver() {
       @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "TCN_ADD: onReceive action=" + action);
            if("com.ysten.ningxia.GoToSleep".equals(action)){   
              mPowerManager.goToSleep(SystemClock.uptimeMillis());                
            }
        }
    };
    //end: add by ysten.tianchining at 20190325: register BroadcastReceiver
    
    private BroadcastReceiver mInstabootReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
                // TODO Auto-generated method stub
            final String action = intent.getAction();
            if ("com.droidlogic.instaboot.RESTORE_COMPLETED".equals(action)) {
                Log.i(TAG,"restore system completed");
                synchronized(mHdmiHwPluggedLock){
                    boolean plugged = false;
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
                    mHdmiHwPlugged =  plugged;
                    Log.i(TAG,"restore hdmi status as "+mHdmiHwPlugged);
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
        //Log.e(TAG,"-----width:"+width+"height:"+height);
        //Log.e(TAG,"-----density:"+density);
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
        if(mHdmiPlugged){
            mNavigationBarCanMove = false;
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

         if (mHasNavigationBar) {
            // The navigation bar is at the right in landscape; it seems always
            // useful to hide it for showing a video.
            mCanHideNavigationBar = true;
        } else {
            mCanHideNavigationBar = false;
        }
        if(SystemProperties.getBoolean("ro.platform.has.mbxuimode",false)){
            mNavigationBarCanMove = false;
            mCanHideNavigationBar = true;
        }

        if (SystemProperties.getBoolean("persist.sys.hideStatusBar", false))
            mHasNavigationBar = false;

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
        final String player_quality = Settings.Secure.getString(resolver, Settings.Secure.DEFAULT_PLAYER_QUALITY);
        SystemProperties.set("media.amplayer.quality", player_quality);
        Settings.Secure.putInt(resolver, Settings.Secure.ANR_SHOW_BACKGROUND, 1);
        String proj_type = SystemProperties.get("sys.proj.type", null);
        String tender_type = SystemProperties.get("sys.proj.tender.type", null);
        float mWindowAnimationScale = Settings.Global.getFloat(resolver,
                Settings.Global.WINDOW_ANIMATION_SCALE, 1.0f);
        float mTransitionAnimationScale = Settings.Global.getFloat(resolver,
                Settings.Global.TRANSITION_ANIMATION_SCALE, 1.0f);
        if (("mobile".equals(proj_type) || "unicom".equals(proj_type)) && (null != mWindowManager)) {
            try {
                if ((mWindowAnimationScale !=0.0f) || (mTransitionAnimationScale != 0.0f)) {
                    mWindowManager.setAnimationScale(0, 0.0f);
                    mWindowManager.setAnimationScale(1, 0.0f);
                    //mWindowManager.setAnimationScale(2, 0.0f);
                    Settings.Global.putFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0.0f);
                    Settings.Global.putFloat(resolver, Settings.Global.WINDOW_ANIMATION_SCALE, 0.0f);
                    //Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0.0f);
                }
            } catch (RemoteException e) {}
        }
        if ("unicom".equals(proj_type) || ("telecom".equals(proj_type) && "yueme".equals(tender_type))) {
            if (null != mMboxOutputModeManager) {
                int isAutoHdmiMode = 0;
                try {
                    isAutoHdmiMode = Settings.Global.getInt(resolver, Settings.Global.DISPLAY_OUTPUTMODE_AUTO);
                } catch (Settings.SettingNotFoundException se) {
                    Slog.e(TAG, "Error: " + se);
                }
                Log.d(TAG , "isAutoHdmiMode is : "+isAutoHdmiMode);
                if (isAutoHdmiMode != 0) {
                    mMboxOutputModeManager.setOutputMode(mMboxOutputModeManager.getBestMatchResolution());
                }
            }
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
/*            if (ActivityManager.isHighEndGfx()) {
                lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
                lp.privateFlags |=
                        WindowManager.LayoutParams.PRIVATE_FLAG_FORCE_HARDWARE_ACCELERATED;
            }*/
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
        if (mHasNavigationBar && mNavShow) {
            // For a basic navigation bar, when we are in landscape mode we place
            // the navigation bar to the side.
            if (mNavigationBarCanMove && fullWidth > fullHeight) {
                return fullWidth - mNavigationBarWidthForRotation[rotation];
            }
        }
        return fullWidth;
    }

    public int getNonDecorDisplayHeight(int fullWidth, int fullHeight, int rotation) {
        if (mHasNavigationBar && mNavShow) {
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

    private Handler toastHandle = new Handler() {
         @Override
         public void handleMessage(Message msg) {
             // TODO Auto-generated method stub
             switch (msg.what) {
             case 1:
                 Toast.makeText(mContext, "Please open orign", Toast.LENGTH_SHORT).show();
                 break;
        //begin add by ysten zengzhiliang at 20181120:sync from CM201-2_beijing zu he key control adbd
             case 3:
				Toast.makeText(mContext, mContext.getResources().getString(com.android.internal.R.string.adbd_open_note), Toast.LENGTH_SHORT).show();
				SystemProperties.set("sys.service.adbd.enable",  "0");
            	break;  
        //end add by ysten zengzhiliang at 20181120:sync from CM201-2_beijing zu he key control adbd
             }
         }
     };
    boolean keyguardOn() {
        return keyguardIsShowingTq() || inKeyguardRestrictedKeyInputMode();
    }

    private static final int[] WINDOW_TYPES_WHERE_HOME_DOESNT_WORK = {
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
        };

	   //add by wangxin, for ScreenSavers
	   private Runnable mLaunchScreenSaver = new Runnable() {
		   @Override
		   public void run() {

        if(SystemProperties.get("ro.ysten.custom.type","none").equals("viper"))
        {
            Log.d(TAG, "custom.type:viper3");
			   if(mInPlaying)return;
			   int delay = Settings.System.getInt(mContext.getContentResolver(), SCREEN_SAVER_DELAY, -1);
			   if(delay < 0)return;
			   
			final AudioManager am = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);//determin music is action
			   boolean isPlayingAudio = false;
			   if(null != am)isPlayingAudio = SystemProperties.get("service.media.playstatus","none").equals("running");
			
			   boolean isUpgrading=(Settings.System.getInt(mContext.getContentResolver(), ROOM_UPGRADE_FLAG, -1)==1);
			   Log.d(TAG, "about to start ScreenSaver = " + delay);
			   if (/*MediaPlayer.isPlayingVideo()||*/isUpgrading || isPlayingAudio) {
				   mHandler.removeCallbacks(mLaunchScreenSaver);
				   mHandler.postDelayed(mLaunchScreenSaver, delay*60*1000);
				   return;
			   }
			   Intent intent = new Intent("com.ysten.screensaver.ScreenSaver");
			   intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			   mContext.startActivity(intent);
		}
           }
	   };
		//add by ysten.huanghongyan 2018.11.27 for CM201_henan
		private boolean mLongPressBack = false;
	   
	 //add by tanhy
			private Runnable mSystemSleep = new Runnable()
		   {
					public void run()
				{ 
        if(SystemProperties.get("ro.ysten.custom.type","none").equals("viper")||SystemProperties.get("ro.ysten.province").contains("CM201_hubei"))
        {
            Log.d(TAG, "custom.type:viper4");
			 if(mInPlaying)return;
				 int sleepDelay = Settings.System.getInt(mContext.getContentResolver(), SYSTEM_SLEEP_TIME_DELAY, -1);
				 //add by guangchao.su 2018.10.12 for A20_shandong settings devicesleep begin
				 int devicesleep = SystemProperties.getInt("persist.sys.devicesleep", 1);
				 Log.d("ysten_sgc_shandong"," use this devicesleep123 is "+devicesleep);
				 if(sleepDelay < 0)return;
				 if(devicesleep == 0)return;
				 //add by guangchao.su 2018.10.12 for A20_shandong settings devicesleep end
				 boolean isUpgrading=(Settings.System.getInt(mContext.getContentResolver(), ROOM_UPGRADE_FLAG, -1)==1);
				 Log.d(TAG, "about to start sleep  = " + sleepDelay);
					 final AudioManager am = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);//determin music is action
					 boolean isPlayingAudio = false;
					 if(null != am)isPlayingAudio = SystemProperties.get("service.media.playstatus","none").equals("running");
	 
				//add by guangchao.su 2019.02.20 for A20_fujian  begin
				if (SystemProperties.get("ro.ysten.province").contains("fujian")||SystemProperties.get("ro.ysten.province").contains("CM201_hubei")){
                    if (isUpgrading) {
                    mHandler.removeCallbacks(mSystemSleep);
                    mHandler.postDelayed(mSystemSleep,sleepDelay*60*1000);
                    return;
                    }
                    PowerManager mananger = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
					mananger.goToSleep(SystemClock.uptimeMillis());
                }else if (/*MediaPlayer.isPlayingVideo()||*/isUpgrading||isPlayingAudio) {//add by guangchao.su 2019.02.20 for A20_fujian  end
						mHandler.removeCallbacks(mSystemSleep);
						  mHandler.postDelayed(mSystemSleep, sleepDelay*60*1000);
						   return;
				   } 
			PowerManager mananger = (PowerManager)mContext.getSystemService(Context.POWER_SERVICE);
					mananger.goToSleep(SystemClock.uptimeMillis());
					}
                 //add by zhanghk at 20181109 begin:show jiangsu poweroff tip
                 if("CM201_jiangsu".equals(SystemProperties.get("ro.ysten.province"))
				 ||"cm201_jiangxi".equals(SystemProperties.get("ro.ysten.province"))
				 ||"cm201_guizhou".equals(SystemProperties.get("ro.ysten.province"))){//add by zhaolianghua for jiangxi
                         showSleepDelayDialog();
                 }
                 //add by zhanghk at 20181109 end:show jiangsu poweroff tip
				//add by ysten.huanghongyan 2018.11.27 for CM201_henan	
				if(SystemProperties.get("ro.ysten.province","master").contains("CM201_henan")){
					showSleepDelayDialog();
				}
				//end by ysten.huanghongyan 2018.11.27 for CM201_henan 
                                //begin:add by zhanghk at 20190522:for 4 hour standby
                                if(SystemProperties.get("ro.ysten.province","master").contains("CM201_zhejiang")){
                                    mPowerManager.goToSleep(SystemClock.uptimeMillis());
                                }
                                //end:add by zhanghk at 20190522:for 4 hour standby   
                }
			};

	private static boolean mKeyF12Pressed = false;
	  //add by wangxin
	  private static final String SCREEN_SAVER_DELAY = "screen_save_delay";
	  private static final String ROOM_UPGRADE_FLAG = "room_upgrade_flag";
	 
	  //add by tanhy
	 private static final String SYSTEM_SLEEP_TIME_DELAY = "system_sleep_time_delay";
     private boolean mistvlongpress=false; //add voice recognition

     private static final String KEY_DOWN_HAPPEN_LOCK = "com.android.internal.policy.impl.KEY_DOWN_HAPPEN_LOCK";
     private static final String KEY_DOWN_HAPPEN_CHILD_LOCK = "com.android.internal.policy.impl.KEY_DOWN_HAPPEN_SHILD_LOCK";
     private static boolean press3Second = false;//add by caishuo press sj key for neimeng A20c at 20190525
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
		//add by huxiang at 2019/8/17 for sleepTime invalid
		if(SystemProperties.get("ro.ysten.province","master").contains("CM201_hubei")){
		   final boolean up = event.getAction() == KeyEvent.ACTION_UP;
           if(up){
	            mHandler.removeCallbacks(mSystemSleep);
                int sleepDelay = Settings.System.getInt(mContext.getContentResolver(),SYSTEM_SLEEP_TIME_DELAY,-1);
                
                Log.d("mark-add", "sleepDelay = " + sleepDelay);
        
                if(sleepDelay > 0)
                   mHandler.postDelayed(mSystemSleep,sleepDelay*SLEEP_DELAY_UNIT);
 
            }
		}
		//add end by huxiang at 2019/8/17 for sleepTime invalid
		//add by ysten.huanghongyan 2018.11.27 for CM201_henan
		if(SystemProperties.get("ro.ysten.province","master").contains("CM201_henan")){
		   final boolean up = event.getAction() == KeyEvent.ACTION_UP;
           if(up){
	    /*if(isCancelDialog){
	    	mHandler.removeCallbacks(mSleepCancleTimer);
		    mCount=60;
		    mDialog.dismiss();
		    isCancelDialog=false;
	    }*/
	            //add by tanhy
	            mHandler.removeCallbacks(mSystemSleep);
                int sleepDelay = Settings.System.getInt(mContext.getContentResolver(),SYSTEM_SLEEP_TIME_DELAY,-1);
        
                if(sleepDelay > 0)
                   mHandler.postDelayed(mSystemSleep,sleepDelay*SLEEP_DELAY_UNIT);
 
            }
			if(down){
				Log.d(TAG," wifiCount : "+wifiCount+" keyCode="+keyCode);	
			
			
				if(down&&keyCode==19 && ( wifiCount==0 || wifiCount==1)){
					wifiCount++;	
				   
				
				}else if(down&&keyCode==20 && ( wifiCount==2 || wifiCount==3)){
					wifiCount++;
									
				
				}else if(down&&keyCode==21 && wifiCount==4){
					wifiCount++;
									   
				
				}else if(down&&keyCode==22 && wifiCount==5){
					wifiCount++;
									   
				
				}else if(down&&keyCode==21 && wifiCount==6){
					wifiCount++;
									  
				
				}else if(down&&keyCode==22 && wifiCount==7){
					wifiCount++;
									   
				
				}else if(down&&keyCode==23 && wifiCount==8){
					wifiCount++;
									  
				
				}else if(down&&keyCode==23 && wifiCount==9){
					
					mHandler.sendEmptyMessage(MSG_OPEN_WIFI_DIALOG);
									
			
				}else {
				
					wifiCount=0;
				} 
			}		 				
		}
        //end by ysten.huanghongyan 2018.11.27 for CM201_henan 
 
        if (DEBUG_INPUT) {
            Log.d(TAG, "interceptKeyTi keyCode=" + keyCode + " down=" + down + " repeatCount="
                    + repeatCount + " keyguardOn=" + keyguardOn + " mHomePressed=" + mHomePressed
                    + " canceled=" + canceled);
        }
        
		//begin by lizheng 20181205 remove mSystemSleep if keycode down----add by guangchao.su 2019.02.20 for A20_fujian  begin
		if(down){
				int sleepDelay = Settings.System.getInt(mContext.getContentResolver(),SYSTEM_SLEEP_TIME_DELAY,-1);
				if(sleepDelay>0){
			        mHandler.removeCallbacks(mSystemSleep);
			        mHandler.postDelayed(mSystemSleep,sleepDelay*60*1000);
				}
			}
		//end by lizheng 20181205 remove mSystemSleep if keycode down----add by guangchao.su 2019.02.20 for A20_fujian  end
		/*begin:add by zhanghk at 20191009:timing analog key to prevent network disconnect*/
		if(SystemProperties.get("ro.ysten.province","master").contains("homeschool")){
			mHandler.removeCallbacks(analogKeyRunnable);
			int analogKeyTime = SystemProperties.getInt("persist.sys.analogkey.time",-1);
			if(analogKeyTime > 0)
			    mHandler.postDelayed(analogKeyRunnable, analogKeyTime*60*1000);
		}
		/*end:add by zhanghk at 20191009:timing analog key to prevent network disconnect*/
               
        //added by yzs at 20190424 multikey trigger autolog begin
        if(down){
			handlerMultKey(keyCode, event);
        }
        //added by yzs at 20190424 multikey trigger autolog end
               
        //add by zhanghk at 20181109 begin:add jiangsu content    
		if(SystemProperties.get("ro.ysten.province","master").contains("CM201_jiangsu")
				||"cm201_jiangxi".equals(SystemProperties.get("ro.ysten.province"))
				||"cm201_guizhou".equals(SystemProperties.get("ro.ysten.province"))){//add by zhaolianghua for jiangxi
			if(isCancelDialog){
				mHandler.removeCallbacks(mSleepCancleTimer);
				mCount=30;
				mDialog.dismiss();
				isCancelDialog=false;
			}
			if(down){
				//add by zhanghk at 20181204:fix sleep abnormally problem
				mHandler.removeCallbacks(mSystemSleep);
				int sleepDelay = Settings.System.getInt(mContext.getContentResolver(),SYSTEM_SLEEP_TIME_DELAY,-1);
				if(sleepDelay>0){
			        mHandler.postDelayed(mSystemSleep, sleepDelay*60*1000);
				}
			}
		}
        //add by zhanghk at 20181109 end:add jiangsu content
     
        //begin:add by ysten zengzhiliang at 20181123:ISTV HOME Key proc
       if (keyCode == KeyEvent.KEYCODE_HOME) {
            //add by sunjh at 20191126 for CM502:合家固话中断处理
            if (("true").equals(SystemProperties.get("persist.sys.iscm502", "false"))) {
                ActivityManager activityManager=(ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
                String currentPackage=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
                Log.d("sjh", "currentPackage=" + currentPackage);
                if (!SystemProperties.get("sys.hejiaguhua.incalling", "0").equals("0")) {
                    if (down) {
                        Log.d("sjh", "callingDialog start");
                        mHandler.sendEmptyMessage(MSG_HOME_OPEN_CALLING_DIALOG);
                        Log.d("sjh", "callingDialog end");
                    }
                    return -1;
                } else if (!TextUtils.isEmpty(currentPackage)
                        && currentPackage.contains("scanbindandfamily")
                        && !SystemProperties.getBoolean("persist.sys.bindandfamily",false)) {
                    return -1;
                }
            }
            //add by sunjh at 20191126 for CM502:合家固话中断处理
            
            int isResponHomeKey=Settings.System.getInt(mContext.getContentResolver(), "IS_RESPOND_HOME_KEY", 0);
            if(isResponHomeKey!=0 && SystemProperties.get("ro.ysten.province").equals("CM201_beijing"))
            {
                Log.d(TAG, "do not response home key 2");
                return -1;
            }
			//add by huxiang at 20190506 for fix home press bug
			if(SystemProperties.get("ro.ysten.province").equals("CM202_jike")||SystemProperties.get("ro.ysten.province").equals("CM202_hunan_gate")){
				ActivityManager activityManager=(ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
                String currentPackage=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
			    Log.d(TAG," currentPackage = " + currentPackage);
				if("com.ysten.com.gatewayprogress.MainActivity".equals(currentPackage)||"com.ysten.com.gatewayprogress.SplashActivity".equals(currentPackage)){
			       return -1;
				}
			}

			//add by ysten-mark for heilongjiang with forbidden homekey at auth apk
			if(SystemProperties.get("ro.ysten.province").equals("CM201_heilongjiang")){
				ActivityManager activityManager=(ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
                String currentPackage=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
			    Log.d("mark"," currentPackage = " + currentPackage);
				if("com.android.iptvauth".equals(currentPackage)){
			       return -1;
				}
				else
				{
					Intent homeIntent=new Intent();
					ComponentName homeName = new ComponentName("tv.icntv.ott","tv.icntv.ott.icntv");
					homeIntent.setComponent(homeName);
					homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
					mContext.startActivity(homeIntent);
				}
			}
			// end : mark add


			//add end by huxiang at 20190506 for fix home press bug
			//begin : add by zhuyu at 20190725 for  forbidden  homeKey when calling 
            if(SystemProperties.get("ro.ysten.province").equals("A20_neimeng")){
               ActivityManager activityManager=(ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
               String currentPackage=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
               Log.d(TAG," currentPackage = " + currentPackage);
               if("com.ysten.hejiaguhua.CallActivity".equals(currentPackage)){
                   return -1;
                }
            }
            //end : add by zhuyu at 20190725 for  forbidden  homeKey when calling 
		    //add by huxiang at 20190625 for anhui_gate fix home press bug
			if(SystemProperties.get("ro.ysten.province").equals("CM202_anhui_gate")){
				ActivityManager activityManager=(ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
                String currentPackage=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
			    Log.d(TAG," currentPackage = " + currentPackage);
				if(currentPackage.contains("com.android.provision")){
			       return -1;
				}
			}
			//add end by huxiang at 20190625 for anhui_gate fix home press bug

                    //add by zhangjunjian at 20191118 for guangdong fix home press bug
                        if(SystemProperties.get("ro.ysten.province").equals("CM201_guangdong")){
                                ActivityManager activityManager=(ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
                                String currentPackage=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
                                Log.d(TAG," currentPackage = " + currentPackage);
                                if(currentPackage.contains("com.ysten.auth.guangdong.MainActivity")||("false".equals(SystemProperties.get("sys.logined","false")))){
                                     return -1;
                                }
                        }
                        //add end by zhangjunjian at 2019118 for guangdong fix home press bug
		    //add by huxiang at 20190817 for hubei forbidden home key
			if(SystemProperties.get("ro.ysten.province").equals("CM201_hubei")){
				ActivityManager activityManager=(ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
                String currentPackage=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
			    Log.d(TAG," currentPackage = " + currentPackage);
				if(currentPackage.contains("com.android.iptvauth")){
			       return -1;
				}
			}
			//add end 
       }
        //end:add by ysten zengzhiliang at 20181123:ISTV HOME Key proc


        //begin add by ysten zengzhiliang at 20181120:sync from CM201-2_beijing guo an yu yin model proc
		if(SystemProperties.get("ro.ysten.province","master").contains("CM201_beijing"))
		{
           ActivityManager activityManager=(ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
           String currentPackage=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
            Log.d(TAG," currentPackage = " + currentPackage);
            //begin:add by ysten zengzhiliang at 20181127:F5 replace F17
	        if(down && repeatCount == 0&& keyCode == KeyEvent.KEYCODE_F5) {
				Intent intent = new Intent();
				ComponentName cn = null;
				if(keyCode == KeyEvent.KEYCODE_F5){/*添加语音识别按键KEYCODE_F17*/
				cn = new ComponentName("com.citicguoanbn.duer", "com.citicguoanbn.duer.service.DuerService");
				intent.putExtra("actionType", "start");
				intent.setComponent(cn);
				mContext.startServiceAsUser(intent, UserHandle.CURRENT);
				return -1;
			  } 
            }
		
            if(!down && keyCode == KeyEvent.KEYCODE_F5) {
			    Intent intent = new Intent();     
			    ComponentName cn = null;
			    cn = new ComponentName("com.citicguoanbn.duer", "com.citicguoanbn.duer.service.DuerService");
				intent.putExtra("actionType", "end");
				intent.setComponent(cn);
				mContext.startServiceAsUser(intent, UserHandle.CURRENT);
			return -1;
            }
			
            //end:add by ysten zengzhiliang at 20181127:F5 replace F17
			if(down && keyCode == KeyEvent.KEYCODE_F24) {
				Log.d(TAG,"start guo an she qu");
				startAppWithScheme(mContext, "launcher://com.guoan.tv");
			}
			if(down && keyCode == KeyEvent.KEYCODE_F23) {
				Log.d(TAG,"start yun shi jie");
                if(Settings.System.getInt(mContext.getContentResolver(), "IS_RESPOND_HOME_KEY", 0)==0)
                {
                    String TopPackage=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
                    if(!TopPackage.equals("com.istv.ui.app.usercenter.QrcodeActivity"))
                    {
                        Intent intent = new Intent();
				        ComponentName comp =new ComponentName("tv.icntv.ott","com.istv.ui.app.usercenter.QrcodeActivity");
                        intent.setComponent(comp);
	                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                    }

                }
                else
                {
                    String TopPackage=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
                    if(!(TopPackage.equals("com.istv.ui.app.login.LoginActivity")
                            ||TopPackage.equals("com.istv.ui.app.login.SupplementPhoneActivity")))
                    {
                        Intent intent = new Intent();
				        ComponentName comp =new ComponentName("tv.icntv.ott","com.istv.ui.app.login.LoginActivity");
                        intent.setComponent(comp);
                        intent.putExtra("isReLogin", true);  
                        intent.putExtra("canBack", false);  
	                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent); 
                    }

                }
			}
		}
        //end add by ysten zengzhiliang at 20181120:sync from CM201-2_beijing guo an yu yin model proc
        

    //begin add by ysten zengzhiliang at 20181120:sync from CM201-2_beijing
	ActivityManager activityManager=(ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);  
	String currentPackage=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();  
	Log.d(TAG," currentPackage = " + currentPackage);
	if (down && currentPackage != null && currentPackage.startsWith("com.ysten.setting") 
            && SystemProperties.get("ro.ysten.province","master").contains("CM201_beijing"))
		{
	                 Log.d(TAG,"adbEanbledSetp:"+adbEanbledSetp);
		if((keyCode==KeyEvent.KEYCODE_DPAD_UP||keyCode==KeyEvent.KEYCODE_DPAD_DOWN||keyCode==KeyEvent.KEYCODE_DPAD_LEFT||keyCode==KeyEvent.KEYCODE_DPAD_RIGHT))
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
	                 Log.d(TAG," ZZZZZZZZZZZZZZZZZZZ ");
					if("true".equals(produce)){

	                 Log.d(TAG," UUUUUUUUUUUUUUUUUU");
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
		
    //end add by ysten zengzhiliang at 20181120:sync from CM201-2_beijing
        if("unicom".equals(SystemProperties.get("sys.proj.type", "ott"))) {
            if(keyCode == KeyEvent.KEYCODE_6 && true == down) {
                mkeyPicCount = 1;
            } else if (keyCode == KeyEvent.KEYCODE_3 && 1 == mkeyPicCount && true == down) {
                mkeyPicCount = 2;
            } else if (keyCode == KeyEvent.KEYCODE_2 && 2 == mkeyPicCount && true == down) {
                mkeyPicCount = 3;
            } else if (keyCode == KeyEvent.KEYCODE_1 && 3 == mkeyPicCount && true == down) {
                mkeyPicCount = 4;
            } else if (keyCode == KeyEvent.KEYCODE_PROG_RED && 4 == mkeyPicCount && true == down) {
                mHandler.post(mAmlScreenshotRunnable);
            } else if (true == down) {
                mkeyPicCount = 0;
            }
        }
        
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
        
	//add by zhanghk at 20181018 begin:press some key to start allapp
        if("CM201_hunan".equals(SystemProperties.get("ro.ysten.province"))) {
            Log.d("zhanghk","mkeyAllappCount:"+mkeyAllappCount);
            if(keyCode == KeyEvent.KEYCODE_CHANNEL_UP && true == down) {
                mkeyAllappCount = 1;
            } else if (keyCode == KeyEvent.KEYCODE_1 && 1 == mkeyAllappCount && true == down) {
                mkeyAllappCount = 2;
            } else if (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN && 2 == mkeyAllappCount && true == down) {	
                mHandler.removeCallbacks(startAllApp);
                mHandler.postDelayed(startAllApp, 100);
            } else if (true == down) {
                mkeyPicCount = 0;
            }
        }	
        //add by zhanghk at 20181018 end:press some key to start allapp		
		
        if(keyCode == KeyEvent.KEYCODE_POUND && 0 == mkeyAdbCount && true == down) {
            mkeyAdbCount = 1;
        } else if (keyCode == KeyEvent.KEYCODE_1 && 1 == mkeyAdbCount && true == down) {
            mkeyAdbCount = 2;
        } else if (keyCode == KeyEvent.KEYCODE_2 && 2 == mkeyAdbCount && true == down) {
            mkeyAdbCount = 3;
        } else if (keyCode == KeyEvent.KEYCODE_3 && 3 == mkeyAdbCount && true == down) {
            mkeyAdbCount = 4;
        } else if (keyCode == KeyEvent.KEYCODE_4 && 4 == mkeyAdbCount && true == down) {
            mkeyAdbCount = 5;
        } else if (keyCode == KeyEvent.KEYCODE_POUND && 5 == mkeyAdbCount && true == down) {
            Log.d(TAG, "open adbd");
            SystemProperties.set("service.adb.tcp.port", "5555");
            SystemProperties.set("sys.start.adb", "0");
            SystemProperties.set("sys.start.adb", "1");
            mHandler.removeCallbacks(showAdbToast);
            mHandler.postDelayed(showAdbToast, 100);
        } else if (true == down) {
            mkeyAdbCount = 0;
        }

        if ("mobile".equals(SystemProperties.get("sys.proj.type", "ott"))) {
            broadcastMobileKeyEvent(event);
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
	
        if (keyCode == KeyEvent.KEYCODE_F10 && down&& !SystemProperties.getBoolean("mbx.hdmiin.pipfocus", true)) {
            mContext.sendBroadcast(new Intent("com.amlogic.hdmiin.pipfocus"));
        }

		if("jiangxi".equalsIgnoreCase(SystemProperties.get("sys.proj.tender.type"))){
			//2017.10,home broadcast
			if (keyCode == KeyEvent.KEYCODE_HOME && down){
				Slog.d(TAG,"===,receive home,send a broadcast:android.intent.action.HOME_KEY_PRESSED");
				mContext.sendBroadcast(new Intent("android.intent.action.HOME_KEY_PRESSED"));
			}

			//2017.10, add colorkey.
			if(	down
				&&
				(keyCode == KeyEvent.KEYCODE_RED ||
				keyCode == KeyEvent.KEYCODE_GREEN ||
				keyCode == KeyEvent.KEYCODE_YELLOW ||
				keyCode == KeyEvent.KEYCODE_BLUE)){
				Slog.d(TAG,"===,receive colorfunkey");
				ColorFunKey(keyCode);
			}
		}
        //begin:add by zhuhengxuan at 20190624 for jiangxi OTT IPTV four color key
        if("cm201_jiangxi".equals(SystemProperties.get("ro.ysten.province"))){
            if(down&&
                (keyCode == KeyEvent.KEYCODE_RED ||
                    keyCode == KeyEvent.KEYCODE_GREEN ||
                    keyCode == KeyEvent.KEYCODE_YELLOW ||
                    keyCode == KeyEvent.KEYCODE_BLUE)){
                if("2".equals(SystemProperties.get("persist.sys.launcher.value"))){
                    doJXFunAction(keyCode);
                    return -1;
                }
            }
        }
        //end:add by zhuhengxuan at 20190624 for jiangxi OTT IPTV four color key
		//begin:add by zhuhengxuan at 20190624 for hebei  IPTV volume key
        /*if(SystemProperties.get("ro.ysten.province", "master").contains("cm201_hebei")){
            if(down && (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                keyCode == KeyEvent.KEYCODE_VOLUME_MUTE)){
                String topPackage = getTopPackage(mContext);
                Log.d(TAG,"hebei  volumedispatch topPackage = "+topPackage);
                boolean topHEActivity = false;
                if(topPackage.equals("com.android.smart.terminal.iptv")) {
                    topHEActivity = true;
                }
                if(topHEActivity){
                    doIPTVFuncationHE(keyCode);
                    return -1;
                }
	        }
        }*/
        //end:add by zhuhengxuan at 20190930 for hebei  IPTV volume key
    //begin add by ysten zengzhiliang at 20181120:sync from CM201-2_beijing 4 color key proc
        if(SystemProperties.get("ro.ysten.province","master").equals("CM201_beijing"))
        {
            if(	down
				&&
				(keyCode == KeyEvent.KEYCODE_RED ||
				keyCode == KeyEvent.KEYCODE_GREEN ||
				keyCode == KeyEvent.KEYCODE_YELLOW ||
				keyCode == KeyEvent.KEYCODE_BLUE)){
				Slog.d(TAG,"===,receive colorfunkey");
               	doFunAction_beijing(keyCode);
			}

        }
    //end add by ysten zengzhiliang at 20181120:sync from CM201-2_beijing 4 color key proc
    
    //begin:add  by ysten zengzhiliang at 20190123 yunnan 4 color key proc
	    if(SystemProperties.get("ro.ysten.province","master").equalsIgnoreCase("CM201_yunnan"))
        {
            if(	down
				&&
				(keyCode == KeyEvent.KEYCODE_RED ||
				keyCode == KeyEvent.KEYCODE_GREEN ||
				keyCode == KeyEvent.KEYCODE_YELLOW ||
				keyCode == KeyEvent.KEYCODE_BLUE)){
				Slog.d(TAG,"===,receive colorfunkey");
               	doFunAction_cm201_yunnan(keyCode);
			}

        }
    //end:add  by ysten zengzhiliang at 20190123 yunnan 4 color key proc
        if(SystemProperties.get("ro.ysten.province","master").equalsIgnoreCase("CM201_homeschool"))
        {
            if(down && (keyCode == KeyEvent.KEYCODE_F6 || keyCode == KeyEvent.KEYCODE_F7)){
                doFunAction_school(keyCode);
            }
        }
            //begin: add by ysten xumiao at 20181218:hubei add KEYCODE
	    if (SystemProperties.get("ro.ysten.province","master").contains("hubei")) {
		    if(	down && (keyCode == KeyEvent.KEYCODE_F6 ||
				    keyCode == KeyEvent.KEYCODE_F7 ||
				    keyCode == KeyEvent.KEYCODE_F8 ||
				    keyCode == KeyEvent.KEYCODE_F9 ||
					keyCode == KeyEvent.KEYCODE_RED||
				    keyCode == KeyEvent.KEYCODE_GREEN ||
				    keyCode == KeyEvent.KEYCODE_YELLOW ||
				    keyCode == KeyEvent.KEYCODE_BLUE)){
                      String bussiness = getBussinessPlatform(mContext);
                      Log.d(TAG, "bussiness:" + bussiness);
                      if(TextUtils.isEmpty(bussiness)){
                          bussiness = SystemProperties.get("persist.sys.hb.bussiness", "");
			  Log.d(TAG, "get persist bussiness:" + bussiness);
                       }
                      if(!TextUtils.isEmpty(bussiness) && !bussiness.equalsIgnoreCase("iptv")) {
			Log.d(TAG, "event.getAction :" + event.getAction()+" ACTION_UP : "+KeyEvent.ACTION_UP);
                         if(!SystemProperties.get("sys.key.home").equals("on")&&Settings.System.getInt(mContext.getContentResolver(), "IS_RESPOND_HOME_KEY", 0)==0){
                             doFunActionHB(keyCode); 
                          } else {
			     Log.d(TAG, "doFunActionHB else");
		          }		   
	              } else {
                             if(!SystemProperties.get("sys.key.home").equals("on")&&Settings.System.getInt(mContext.getContentResolver(), "IS_RESPOND_HOME_KEY", 0)==0){
                               doIPTVFuncationHB(keyCode); 
                             } else {
			       Log.d(TAG, "doIPTVFunActionHB else");
		             }		   			   
			}
                  }
	    }
	    //end: add by ysten xumiao at 20181218:hubei add KEYCODE
        
        //add by sunjh at 20190726:CM502 和家亲解绑
        Toast hjqtoast = null;
        if (("true").equals(SystemProperties.get("persist.sys.iscm502", "false"))) {
            boolean hjqTrigger = false;
            String topPackageName = getTopActivity(mContext);
            //单5键
            if (down && keyCode == KeyEvent.KEYCODE_5) {
                if (repeatCount < 100) {
                    Log.d("sjh", "5 is down, repeatCount is " + repeatCount);
                } else if (repeatCount == 100) {
                    //Intent intent = new Intent("com.ysten.andlink.change");
                    //mContext.sendBroadcast(intent);
                    //Log.d("sjh", "send : " + intent);
                    openapk("com.ysten.scanbindandfamily", "com.ysten.scanbindandfamily.MainActivity");
                    // new Thread(new Runnable() {
                        // public void run() {
                            // Looper.prepare();
                            // Toast.makeText(mContext, "设备处于配网状态中，可打开和家亲App进行绑定", Toast.LENGTH_LONG).show();
                            // Looper.loop();
                        // }
                    // }).start();
                }
            }
            //1&9组合键
            // if (down) {
                // if (keyCode == KeyEvent.KEYCODE_1) {
                    // hjqTrigger = true;
                    // Log.d("sjh", "1 is down and hjqTrigger = " + hjqTrigger);
                // } else if (hjqTrigger) {
                    // int currentBLLevel = adioGetBLLevel();
                    // int keyValue = event.getKeyCode();
                    // if (keyCode == KeyEvent.KEYCODE_9 && repeatCount < 40) {
                        // Log.d("sjh", "9 is down and hjqTrigger = " + hjqTrigger + " repeatCount is " + repeatCount);
                    // } else if (keyCode == KeyEvent.KEYCODE_9 && repeatCount == 40) {
                        // Intent intent = new Intent("com.ysten.andlink.exchange");
                        // mContext.sendBroadcast(intent);
                        // Log.d("sjh", "send : " + intent);
                    // }
                // }
            // } else if (up) {
                // if (keyCode == KeyEvent.KEYCODE_1) {
                    // hjqTrigger = false;
                // }
            // }
        }
        //end by sunjh at 20190726:CM502 和家亲解绑
        
        final boolean hdmiInPortKey = SystemProperties.getBoolean("mbx.hdmiin.portkey", true);
        if (hdmiInPortKey) {
            final int hdmiInPortMenuKeyCode = SystemProperties.getInt("mbx.hdmiin.portkeycode", -1);
            if (keyCode == hdmiInPortMenuKeyCode) {
                if (!down) {
                    Intent intent = new Intent("com.amlogic.hdmiin.portkey");
                    intent.putExtra("keycode", keyCode);
                    mContext.sendBroadcast(intent);
                }
            }
        }

        if((keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_SETTINGS 
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE 
                || keyCode == KeyEvent.KEYCODE_POWER || keyCode == KeyEvent.KEYCODE_STB_POWER) 
                && (is_topwindow("net.sunniwell.service.swupgrade.usb") || is_topwindow("net.sunniwell.service.swupgrade.chinamobile"))) {
            return -1;
        }
        
	    //add voice recognition	
		if ( 1 == SystemProperties.getInt("persist.sys.support_orionstar", 0) ) {
            if (keyCode == KeyEvent.KEYCODE_F11) {
                if (1 == SystemProperties.getInt("persist.sys.orionstar.status", 0)) {
		            if (down) {
                        if ((event.getFlags() & KeyEvent.FLAG_LONG_PRESS) != 0) {  
                            if (!keyguardOn) {
                                mistvlongpress=true;
                                if(!SystemProperties.get("ro.ysten.province","master").contains("neimeng")){//begain by xiulong press sj key toast voice at 20190607 for neimeng
									handleLongPressForIsTV(keyCode,true);
								}  //end  by xiulong press sj key toast voice at 20190607
								//begain by caishuo 20190525 for neimeng A20c
								press3Second = true;
								mHandler.postDelayed(new Runnable() { 
									@Override
									public void run() { 
										if(press3Second){
											Intent ethNetStatusIntent = new Intent();
											ethNetStatusIntent.setAction("com.net.switcher.NET_STATE_SWITCH");
											mContext.sendBroadcast(ethNetStatusIntent);
											Log.d(TAG,"pressed 3  second");
										}
									} 
								}, 1500); //end by caishuo for neimeng A20c 
                            }
                        }
                    } else {
						press3Second = false;//add by caishuo at 20190525 for neimeng A20c
                        if(mistvlongpress) {
                            mistvlongpress=false;
                            if(!SystemProperties.get("ro.ysten.province","master").contains("neimeng")){//begain by xiulong press sj key toast voice at 20190607 for neimeng
									handleLongPressForIsTV(keyCode,true);
								}//end  by xiulong press sj key toast voice at 20190607 
                        } else {
                            // andy start voice_recognition broadcast
							if(mKeyMicFlag || "1".equals(SystemProperties.get("sys.yst.netstate.switch", "0"))){
								Log.d(TAG, "send broadcast mKeyMicFlag is true");
								return -1;					
							}
                            Intent intent = new Intent("com.ysten.manual_voice_recognition_start");         
                            intent.putExtra("action", "manual_start_voice");
                            mContext.sendBroadcast(intent);
                        }
                    }
                } else {
                    toastHandle.sendEmptyMessage(1);            
                }
            }
            if(keyCode == KeyEvent.KEYCODE_F11)
                return -1;
        }
		
		//istv key dispatch add  by xue
		if(DEBUG_ISTV) {
		    if (down) {
                if ((event.getFlags() & KeyEvent.FLAG_LONG_PRESS) != 0) {  
                    if (!keyguardOn) {
                        mistvlongpress=true;
                        handleLongPressForIsTV(keyCode,true);  
                    } 
                }
            } else {
                if(mistvlongpress) {
                    mistvlongpress=false;
                    handleLongPressForIsTV(keyCode,false);
                }
            }
        }
        //istv key dispatch
		
         /*if ( 1 == SystemProperties.getInt("persist.sys.support_orionstar", 0) ) {
             if (keyCode == KeyEvent.KEYCODE_APPS) {
                 if (1 == SystemProperties.getInt("persist.sys.orionstar.status", 0)) {
                     if (!down) {
                         // andy start voice_recognition broadcast
                         Intent intent = new Intent("com.ysten.manual_voice_recognition_start");
                         intent.putExtra("action", "manual_start_voice");
                         mContext.sendBroadcast(intent);
                     }
                 }else{
                     //toastHandle.sendEmptyMessage(1);
                 }
             }
             if(keyCode == KeyEvent.KEYCODE_APPS)
                 return -1;
         }*/
        /*if(((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) || (keyCode == KeyEvent.KEYCODE_VOLUME_UP))
                && (mFocusedApp.toString().contains("com.bestv.ott.mediaplayer.activity.BestvPlayer")
                || mFocusedApp.toString().contains("com.bestv.online.activity.ChainNewsActivity"))) {
            if(mAudioManager.isMute() || !down)
                return -1;
        }
        if(((keyCode == KeyEvent.KEYCODE_VOLUME_MUTE) || (keyCode == KeyEvent.KEYCODE_MUTE))
                && (mFocusedApp.toString().contains("com.bestv.ott.mediaplayer.activity.BestvPlayer")
                || mFocusedApp.toString().contains("com.bestv.online.activity.ChainNewsActivity"))) {
            return -1;
        }*/
        // First we always handle the home key here, so applications
        // can never break it, although if keyguard is on, we do let
        // it handle it, because that gives us the correct 5 second
        // timeout.
        if (keyCode == KeyEvent.KEYCODE_HOME) {
            //add by ysten.lijg,20190328,for paizhang apk
            if (!down && SystemProperties.get("sys.key.home").equals("discon")&&SystemProperties.get("ro.ysten.province").contains("hubei")) {
                Log.d(TAG, "home discon");
                Intent intent = new Intent("android.intent.action.MAIN");
                intent.setClassName("com.ysten.netdiscon", "com.ysten.netdiscon.MainActivity");
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(intent);
                Log.d(TAG, "home discon intent");
                return -1;
            }
            //end by ysten.lijg,20190328,for paizhang apk
            //add by zhanghk at 20181031 begin:get packageName
            DevInfoManager manager = (DevInfoManager) mContext.getSystemService(DevInfoManager.DATA_SERVER);
            String packageName = manager.getValue(DevInfoManager.Launcher);
            //add by zhanghk at 20181031 end:get packageName
            // If we have released the home key, and didn't do anything else
            // while it was pressed, then it is time to go home!
	    //add by zhaolianghua start for hebei @20181119
	    if(isActivityRunning(mContext,"m.amt.app.ZeroHelpActivity")){
		    return -1;
	    }
	    if(!down&&SystemProperties.get("ro.ysten.province").contains("hebei")){
		    doIPTVFuncationHE(keyCode);
		    return -1;
	    }
	    //add by zhaolianghua end
        if (!down && SystemProperties.get("ro.ysten.province", "master").contains("CM201_ahaschool")) {
            goAhaschoolHome();
            return -1;
        }
        if (!down && SystemProperties.get("ro.ysten.province", "master").contains("CM201_homeschool")) {
            goHomeschoolHome();
            return -1;
        }
	    //add by zhaolianghua for jiangxi HOME keycode @20181129
	    if (!down&&SystemProperties.get("ro.ysten.province").contains("jiangxi")) {
	            //begin by zhuhengxuan 判断ott，和判断是否是错误弹框状态，有错误弹框状态就不能响应首页键
		    if(SystemProperties.get("persist.sys.launcher.value","0").equals("1")){
			    Log.d(TAG,"zhuhengxuan----HOME键，且是OTT模式，接下来进行错误提示框ProvisionJX阶段判断");
			    if(SystemProperties.get("sys.ysten.checkloading.ott").equals("true")){
				    Log.d(TAG,"zhuhengxuan----HOME键，且是OTT模式，现在是错误提示框ProvisionJX阶段");
				    return -1;
		            }
		    }
	            //end by zhuhengxuan for jiangxi HOME keycode @20181129
		    ActivityManager am1 = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
		    ComponentName cn1 = am1.getRunningTasks(1).get(0).topActivity;
		    packageName = cn1.getPackageName();
		    String className =cn1.getClassName();
		    String launcher=SystemProperties.get("persist.sys.launcher.value");
		    Log.d("YurneroPWM","pkgName = "+packageName+" ; clsName = "+className);
		    if (packageName.contains("com.jiangxi.provision")
				    ||packageName.contains("com.huawei.stb.tm1")||"0".equals(launcher)) {
			    return -1;
		    }
		    doJXFunAction(keyCode);
		    return -1;
	    }
	    //add by zhaolianghua end
		//add by ysten.huanghongyan 2018.11.27 for CM201_henan
		//add by wuguoqing 2019.03.03 for iptv-gs home start
		//add by xumiao 2019.0419 for iptv-gs home start
		if (!down&&SystemProperties.get("ro.ysten.province").contains("CM201_IPTV_gansu")) {
			ActivityManager am1 = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
			ComponentName cn1 = am1.getRunningTasks(1).get(0).topActivity;
			packageName = cn1.getPackageName();
			//Log.d("gansutopActivity","pkgName = "+packageName);
			List<ActivityManager.RunningAppProcessInfo> appList = mActivityManager.getRunningAppProcesses() ;
                        for (ActivityManager.RunningAppProcessInfo running : appList) {
                            if ((running.processName) != null) {
                                //Log.d("xumiao gansutopActivity","running.processName : "+running.processName);
                                if((running.processName).equals("com.chinamobile.middleware.startup")){
                                   ganSuLoginfalg=3;
                                  }
                             }
			}
			if (packageName.contains("com.cmcc.loginauth")) {
				return -1;
			}
		}
		//add by wuguoqing 2019.03.03 for iptv-gs home end
		//add by xumiao 2019.0419 for iptv-gs home end
		if(SystemProperties.get("ro.ysten.province","master").contains("CM201_henan")){
		    if (keyCode == KeyEvent.KEYCODE_HOME) {
				String username = getValueFromStb("username")+"";		       
		        Log.d(TAG,"username:"+username);
		        if(username==null || "".equals(username) || TextUtils.isEmpty(username)){
		           return -1;
		        }
		        if(!down) {
			        ActivityManager mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
			        Intent mintent = new Intent();
			        mintent.setAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
			        mContext.sendBroadcast(mintent);
		
		            String packagename = SystemProperties.get("epg.launcher.packagename");
		            if(packagename == null || packagename.equals("")) {
                       Log.d(TAG,"KEYCODE_HOME start com.huawei.tvbox");
					   Intent homeIntent=new Intent();
						ComponentName homeName = new ComponentName("com.huawei.tvbox","com.huawei.tvbox.activity.LauncherActivity");
						homeIntent.setComponent(homeName);
						homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						mContext.startActivity(homeIntent);			           
		            }else {
					   Log.d(TAG,"KEYCODE_HOME start "+packagename);
			           Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(packagename);
			           intent.putExtra("GoHome", "true");
			           mContext.startActivity(intent);
		            }
		           return -1;
		        }  

	        }
			
			
				
		}
        //end by ysten.huanghongyan 2018.11.27 for CM201_henan  
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
                if(SystemProperties.get("ro.ysten.custom.type","none").equals("viper")){
                         //2017.10,home broadcast
                	Intent panelIntent=new Intent("android.intent.action.HOME_KEY_PRESSED");
                 	mContext.sendBroadcast(panelIntent);
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
                if ("m202".equals(SystemProperties.get("ro.product.name")) && mMapMenuKey) {
                    mMapMenuKey = false;
                    sendMapKey(KeyEvent.KEYCODE_MENU, false);
                    return -1;
                }

                if(isApplicationExsit("com.ctgd.launcher")) {
                    launchHomeFromHotKeyForSY();
                } else {
                //begin by ysten.zhangjunjian,20181128,for hubei
				//update by huxiang at 20190817 for fix hubei industry apk home press bug
                if (!(SystemProperties.get("ro.ysten.province").contains("fujian")||SystemProperties.get("ro.ysten.province","master").contains("CM201_henan")
					||(SystemProperties.get("ro.ysten.province","master").contains("CM201_hubei")&&"iptv".equals(SystemProperties.get("persist.sys.hb.bussiness"))))) {
                    launchHomeFromHotKey();
					}
                }
				Log.i(TAG,"ro.ysten.province="+SystemProperties.get("ro.ysten.province","master"));
                if(SystemProperties.get("ro.ysten.province","master").equalsIgnoreCase("CM201_hubei")){
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
                Log.d(TAG,"username:"+username);
                  String bussiness = getBussinessPlatform(mContext);
                if(TextUtils.isEmpty(bussiness)){
                    bussiness = SystemProperties.get("persist.sys.hb.bussiness", "");
	                Log.d(TAG, "get persist bussiness:" + bussiness);
                }
                Log.d(TAG, "bussiness:" + bussiness);
                String businessplatform=queryDBValue(mContext,"bussiness_platform");
				Log.i(TAG,"businessplatform="+businessplatform);
				String topPackage = getTopActivity(mContext);
				//add by fenghao,20181128,for hubei
				String secondActivity = getSecondActivity(mContext); 
				Log.i(TAG,"topPackage="+topPackage);
				//add by fenghao,20181128,for hubei
				Log.i(TAG,"secondActivity="+secondActivity);
				if(TextUtils.isEmpty(businessplatform)||TextUtils.isEmpty(topPackage)){
                    return -1;
                }
				//begin by fenghao,20181128,for hubei
				if(topPackage.contains("com.android.iptvauth")){
				    Log.i(TAG,"keep in iptvauth");
					return -1;
				}
				if((topPackage.contains("com.ysten.setting") && !TextUtils.isEmpty(secondActivity) && secondActivity.contains("com.android.iptvauth"))){
					Log.i(TAG,"keep in iptvauth and setting");
					return -1;
				}
				//end by fenghao,20181128,for hubei
                if(!TextUtils.isEmpty(bussiness) && bussiness.equalsIgnoreCase("iptv")) {
	                Log.d(TAG, "launcher iptv");
                    if(!down && !TextUtils.isEmpty(topPackage) && !topPackage.contains("com.android.iptvauth")){
                    String launcher = SystemProperties.get("persist.sys.luncher","0");
						Log.i(TAG,"launcher="+launcher);
                        if(TextUtils.isEmpty(username)){
                             return -1;
                          }
                        if(!down){
                             Log.d("zjj", "111"+topPackage); 
                              if(topPackage.contains("com.SyMedia.SyIptv.SyIptv")){

                               try{
                                    java.lang.Process p = Runtime.getRuntime().exec("input keyevent 181 ");
		                  }
        	        	 catch(Exception e){
                                   Log.i("zjj", "key error");
	                 	}

                              }else{
	                        Intent iptvIntent = new Intent();
				            iptvIntent.setAction("com.android.smart.terminal.iptv");
	                        iptvIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				            iptvIntent.putExtra("intentMsg", "EPGDomain");
	                        mContext.startActivity(iptvIntent);
                            Intent panelIntent=new Intent("KEY_HOME");
                            mContext.sendBroadcast(panelIntent);
                            Intent homeIntent=new Intent("HOME_KEYC");
                            mContext.sendBroadcast(homeIntent);
                               }
		                }
		                return -1;
		            } else {
                        return 0;
		            }
               } else {
                   String topPackageName = getTopActivity(mContext);
                   if(!down && !TextUtils.isEmpty(topPackageName) && !topPackageName.contains("com.android.iptvauth")){
	                   Log.d(TAG, "launcher bestv");
	                   Intent bestv = new Intent();//("bestv.ott.action.launcher");
	                   bestv.setAction("bestv.ott.action.launcher");
	                   bestv.addCategory(Intent.CATEGORY_DEFAULT);
	                   bestv.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	                   mContext.startActivity(bestv);
                       Intent homeIntent=new Intent("HOME_KEYC");
                       mContext.sendBroadcast(homeIntent);
					   //add by huxiang at 2019/09/09 for bestTV require this broadcast
					   Intent bestIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                       bestIntent.putExtra("reason","homekey");
                       mContext.sendBroadcast(bestIntent);
					   //add end
	                   return -1;
                    }else{
						return 0;
					}
	           }
			}
			//end by ysten.zhangjunjian,20181128,for hubei
			//begin by ysten.zhangjunjian,20181016,for guangdong
		if(SystemProperties.get("ro.ysten.province","master").equals("CM201_guangdong")){
                    if(SystemProperties.get("sys.logined","false").equals("true")){
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
            //end by ysten.zhangjunjian,20181016,for guangdong
				//begin by ysten.zhangy,20190521,for neimeng home  key
			if(SystemProperties.get("ro.ysten.province","master").contains("neimeng")){
					Log.d(TAG, "neimeng homekey");
                    String topPackageName = getTopActivity(mContext);
                    if(!TextUtils.isEmpty(topPackageName) && !topPackageName.equals("com.udte.launcher.cmcc")) {
                        if(!down){
							Log.d(TAG, "neimeng homekey !down");
							if(!topPackageName.contains("com.ysten.auth")){
								Log.d(TAG, "neimeng homekey will start launcher!");
								Intent iptvIntent = new Intent();
                                iptvIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                ComponentName comp =new ComponentName("com.udte.launcher.cmcc","com.udte.launcher.cmcc.activity.HomeActivity");
                                iptvIntent.setComponent(comp);
                                mContext.startActivity(iptvIntent);
							}
                                
                            }
                                return -1;
                        } else {
                            return 0;
                            }
                }
            //end by ysten.zhangy,20190521,for neimeng
            //begin by ysten.xuyunfeng, 20191025 for ningxia handle home key
            if(SystemProperties.get("ro.ysten.province","master").equals("CM201_ningxia"))
            {
                String topPackageName = getTopActivity(mContext);
                Log.d(TAG, "CM201_xingxia: topPackageName = "+topPackageName);
                PackageManager mPackageManager = mContext.getPackageManager();
                Log.d(TAG, "CM201_xingxia: mPackageManager = "+mPackageManager);
                if(!TextUtils.isEmpty(topPackageName) && !topPackageName.equals("nx.mobile.iptv"))
                {
                    if (mPackageManager != null)
                    {
                        Intent tempIntent = mPackageManager.getLaunchIntentForPackage("nx.mobile.iptv");
                        if (tempIntent != null)
                        {
                            mContext.startActivity(tempIntent);
                            Log.d(TAG, "CM201_xingxia: startActivity according packageName nx.mobile.iptv!");
                        }
                        else
                        {
                            Log.d(TAG, "CM201_xingxia: startActivity but LaunchIntent is null!!");
                        }
                    }
                    else
                    {
                        Intent iptvIntent = new Intent();
                        iptvIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        ComponentName comp =new ComponentName("nx.mobile.iptv","cn.gd.snm.snmcm.LauncherActivity");
                        iptvIntent.setComponent(comp);
                        mContext.startActivity(iptvIntent);
                        Log.d(TAG, "CM201_xingxia: startActivity according nx.mobile.iptv/cn.gd.snm.snmcm.LauncherActivity!");
                    }
                    Log.d(TAG, "CM201_xingxia: Response Home Key, Jump LauncherActivity!");
                    return -1;
                }
                else
                {
                    Log.d(TAG, "CM201_xingxia: Release Home Key!");
                    return 0;
                }
            }
            //end by ysten.xuyunfeng, 20191025 for ningxia handle home key
               //ben by lizheng 20181129 for fujian keycode_home
            if (!down&&SystemProperties.get("ro.ysten.province").contains("fujian")) {
				Log.d(TAG, "fujian homekey");
				//add by guangchao.su 2019.02.20 for A20_fujian  begin
				boolean launcherStarted=SystemProperties.getBoolean("persist.sys.launcher.started",true);
				if (!launcherStarted)
                {
				return -1;
                }
				//add by guangchao.su 2019.02.20 for A20_fujian  end
                packageName = getTopPackage(mContext);
                 if (packageName.contains("com.huawei.stb.tm")|| packageName.contains("com.fujian.provision")
					 || packageName.contains("com.ysten.stbguide")) {
                    return -1;
                }
				String launcher=SystemProperties.get("persist.sys.launcher.value");
			      if("1".equals(launcher)){
                     sendBroadcastToHome();
                 }else if("2".equals(launcher)){
					 doIPTVFuncationFJ(keyCode);
                 }
			return -1;
		}		
		//end by lizheng 20181129 for fujian keycode_home
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
            //begin:add by zhangjunjian at 20190215:enable menu key useless 
              Log.d("zjj", "c1111111");
            if(SystemProperties.get("ro.ysten.province","master").contains("hubei")){
                Log.d("zjj", "00000000"+getTopActivity(mContext));
                if(getTopActivity(mContext).contains("Iptv")){
                       return -1;
                }
            }
            //end:add by zhangjunjian at 20190215:enable menu key useless 
            // Hijack modified menu keys for debugging features
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
            }else if(!mMenuKeyUp && repeatCount > 8 && SystemProperties.getBoolean("ro.platform.has.mbxuimode", false)){
				String proj_type = SystemProperties.get("sys.proj.type", null);
                if(!"mobile".equals(proj_type)) {
                    mContext.sendBroadcast(new Intent(Intent.ACTION_ONEKEY_CLEAN));
                    mMenuKeyUp = true;
                }
            }else if(!down){
				  mMenuKeyUp = false;
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
        }  else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
			//add by sunjiahui at 20190228 for CM502
			Log.d(TAG, "send broadcast mic");
			if("1".equals(SystemProperties.get("sys.yst.netstate.switch", "0")))//begain add by xiulong for neimeng net switch flag at 20190605
			{
				Log.d(TAG, "send broadcast sys.yst.netstate.switch is 1");
				return -1;
			}//end  by xiulong for neimeng net switch flag at 20190605
            if (down) {
					long time = System.currentTimeMillis();
				if(time - mKeyMicPressTime > 3000){
					mKeyMicPressTime = System.currentTimeMillis();
				}   else {
					Log.d(TAG, "send broadcast mic small 3s");
					return -1;
				}
                //add by sunjh at 20191212 for CM502:静麦场景
                if(needDisableMicKey(mContext)){
                    mHandler.post(micRunnable);
                    return -1;
                }
                //end by sunjh at 20191212 for CM502:静麦场景
                mKeyMicFlag = !(SystemProperties.getBoolean("persist.sys.yst.isspeechon", true));
				if(mKeyMicFlag){
					mKeyMicFlag = false;
					Intent intent = null;
					// if("true".equals(SystemProperties.get("persist.sys.iscm502", "false"))){
						// intent = new Intent("com.lhxk.voice_recognition_start");
					// }
					if("true".equals(SystemProperties.get("persist.sys.isa20", "false"))){
						intent = new Intent("com.lhxk.voice_eventcallback_start");
                        Log.d(TAG, "sjh:send " + intent);
					}else{
						intent = new Intent("com.lhxk.voice_recognition_start");
					}
					intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
					mContext.sendBroadcast(intent);  
				}else{
					mKeyMicFlag = true;
                    Intent intent = null;
					// if("true".equals(SystemProperties.get("persist.sys.iscm502", "false"))){
						// intent = new Intent("com.lhxk.voice_recognition_end");
					// }
					if("true".equals(SystemProperties.get("persist.sys.isa20", "false"))){
						intent = new Intent("com.lhxk.voice_eventcallback_end");
                        Log.d(TAG, "sjh:send " + intent);
					}else{
						intent = new Intent("com.lhxk.voice_recognition_end");
					}
					intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
					mContext.sendBroadcast(intent);  
				}
            } else {

            }
            return -1;
			//end by sunjiahui at 20190228 for CM502
         }/*else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
			//add by liuxl at 20181127 for A20, support bluetooth mickey start
			String isA20 = SystemProperties.get("persist.sys.isa20", "false");
			if("true".equals(isA20)){
				Log.d(TAG, "send broadcast mic");
				if (down) {
				long time = System.currentTimeMillis();
				if(time - mKeyMicPressTime > 5000){
					mKeyMicPressTime = System.currentTimeMillis();
				}else {
					Log.d(TAG, "send broadcast mic small 5s");
					return -1;
				}
					if(mKeyMicFlag){
						mKeyMicFlag = false;
						Intent intent = new Intent("com.lhxk.voice_recognition_start");		   
						intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
						mContext.sendBroadcast(intent);  
					}else{
						mKeyMicFlag = true;
						Intent intent = new Intent("com.lhxk.voice_recognition_end");		   
						intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
						mContext.sendBroadcast(intent);  
					}
				} else {

				}
			}
            return -1;
			//add by liuxl at 20181127 for A20, support bluetooth mickey end
        }*/else if(keyCode == KeyEvent.KEYCODE_F12 || keyCode == KeyEvent.KEYCODE_F5){
		//add by ysten liuxl for support blutooth remote voice
				if(mKeyMicFlag || "1".equals(SystemProperties.get("sys.yst.netstate.switch", "0"))){
						Log.d(TAG, "send broadcast mKeyMicFlag is true");
						return -1;					
				}		
               if(down && !mKeyF12Pressed){
                   Log.i(TAG, "send broadcast com.ystenbroadcast.voicereg -e action start");
				   Intent intent = new Intent("com.ystenbroadcast.voicereg");		   
				   intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
				   intent.putExtra("action", "start");
				   mContext.sendBroadcast(intent);   
				   mKeyF12Pressed = true;
               }else if(!down){
				   if(mKeyF12Pressed){
						mKeyF12Pressed = false;
						Log.i(TAG, "send broadcast com.ystenbroadcast.voicereg -e action stop");
						Intent intent = new Intent("com.ystenbroadcast.voicereg");		   
						intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
						intent.putExtra("action", "stop");
						mContext.sendBroadcast(intent);
				   }
               	}
		return 0;
        } else if (keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            if (!keyguardOn) {
                if (down && repeatCount == 0) {
                    preloadRecentApps();
                } else if (!down) {
                    toggleRecentApps();
                }
            }
            return -1;
        } else if (keyCode == KeyEvent.KEYCODE_ASSIST) {
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
	    //modify by zhaolianghua for jiangxi @20181229
	    if(!"cm201_jiangxi".equals(SystemProperties.get("ro.ysten.province"))){
		    if (down && repeatCount == 0) {
			    mHandler.post(mScreenshotRunnable);
		    }
		    return -1;
	    }
	    //modify end
        } else if (keyCode == KeyEvent.KEYCODE_BREAK) {
            if (down && repeatCount == 0) {
                mHandler.post(mAmlScreenshotRunnable);
            }
            return -1;
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
        } else {
            HotKeyObj mHotKeyObj = mHotKeyMap.get(keyCode);
            if((repeatCount == 0) && !down) {
                int size = mHotCombKeyList.size();
                for(int i=0; i<size; i++) {
                    HotCombKeyObj mHotCombKeyObj = mHotCombKeyList.get(i);
                    Log.i(TAG, "KeyCur: " + mHotCombKeyObj.keyCur + ", keyCode: " + keyCode);
                    if((mHotCombKeyObj.keyCur < mHotCombKeyObj.keyCodes.size()) 
                            && (mHotCombKeyObj.keyCodes.get(mHotCombKeyObj.keyCur) == keyCode)) {
                        mHotCombKeyObj.keyCur += 1;
                        if(mHotCombKeyObj.keyCur == mHotCombKeyObj.keyCodes.size()) {
                            mHotCombKeyObj.keyCur = 0;
                            if (!canceled) {
                                Log.i(TAG, "the key is is not canceled");
                                launchFromHotKey(mHotCombKeyObj);
                            } else {
                                Log.i(TAG, "Ignoring Setting; event canceled.");
                            }
                            return -1;
                        }
                        Log.i(TAG, "Ignoring hot key, Because the current key is the key combination!");
                        mHotKeyObj = null;
                    } else {
                        mHotCombKeyObj.keyCur = 0;
                    }
                }
            }

            if((mHotKeyObj != null) && (mHotKeyObj.keyCode == keyCode)) {
                if ("telecom".equals(SystemProperties.get("sys.proj.type", "ott"))
                    && keyCode == KeyEvent.KEYCODE_SETTINGS
                    && down
                    && repeatCount == 0) {
                        Log.i(TAG, "3 seconds to open setting window for kewinrc.");
                        if (!canceled) {
                        Log.i(TAG, "the key is is not canceled");
                        launchFromHotKey(mHotKeyObj);
                    } else {
                        Log.i(TAG, "Ignoring Setting; event canceled.");
                    }
                    return -1;
                }

                if ((repeatCount == 0) && !down) {
                    if ("telecom".equals(SystemProperties.get("sys.proj.type", "ott"))
                    && keyCode == KeyEvent.KEYCODE_SETTINGS) {
                        Log.i(TAG, "not need to pop setting window again.");
                        return 0;
                    }
                    if (!canceled) {
                        Log.i(TAG, "the key is is not canceled");
                        launchFromHotKey(mHotKeyObj);
                    } else {
                        Log.i(TAG, "Ignoring Setting; event canceled.");
                    }
                    return -1;
                }
                return 0;
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

        if(!down && mMapKeyLongPress && (mMapKey.srcKey == keyCode)){
            mMapKeyLongPress = false;
            mMapKey = null;
            return -1;
        }

        if (mGlobalKeyManager.handleGlobalKey(mContext, keyCode, event)) {
            return -1;
        }
        if(SystemProperties.get("ro.ysten.custom.type","none").equals("viper"))
        {
            Log.d(TAG, "custom.type:viper5");
			//add by wangxin
			IntentFilter filter = new IntentFilter();
			filter.addAction("com.ysten.viper.startplay");
			filter.addAction("com.ysten.viper.endplay");
			filter.addAction(Intent.ACTION_SCREEN_ON);
			filter.addAction(Intent.ACTION_SCREEN_OFF);
			mContext.registerReceiver(mScreenSaverReceiver, filter);

			Log.d(TAG, "PhoneWindowManager init()");
			mHandler.removeCallbacks(mLaunchScreenSaver);
			int delay = Settings.System.getInt(mContext.getContentResolver(), SCREEN_SAVER_DELAY, -1);
			if(delay > 0){
				mHandler.postDelayed(mLaunchScreenSaver, delay*60*1000);
			} 
		//add by tanhy
		 mHandler.removeCallbacks(mSystemSleep);
		 String devicesleep = SystemProperties.get("persist.sys.tvdevicesleep", "1");
		Log.d("ysten_sgc_shandong"," use this devicesleep is "+devicesleep);
		  int sleepDelay = Settings.System.getInt(mContext.getContentResolver(),SYSTEM_SLEEP_TIME_DELAY,-1);
		Log.d("ysten_sgc_shandong"," use this sleepDelay is "+sleepDelay);
			  if(sleepDelay > 0)
				  mHandler.postDelayed(mSystemSleep,sleepDelay*60*1000);
        }
		
        if(SystemProperties.get("ro.ysten.province","master").contains("jidi")) {
            if(down && robotKeyComponent(keyCode)) {
		      Slog.d(TAG,"catchrobot");
		      Intent intentset = new Intent(Intent.ACTION_MAIN);
		      intentset.setClassName("com.ysten.bootreceiver","com.ysten.bootreceiver.MainActivity");
		      intentset.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		      mContext.startActivity(intentset);
		      Slog.w(TAG, "robot start.");
            }
            if(down && robotKeyComponent1(keyCode)) {
		      Slog.d(TAG,"jump wifi");
		      Intent intentset = new Intent(Intent.ACTION_MAIN);
		      intentset.setClassName("com.ysten.bootreceiver","com.ysten.bootreceiver.WifiControl");
		      intentset.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		      mContext.startActivity(intentset);
		      Slog.w(TAG, "jidi wifi control start.");
            }
        }
        //begin: add by tianchining at 20191118: switch mode
        else if(SystemProperties.get("ro.ysten.province", "master").equals("CM201_ningxia")){
            if(down && robotKeyComponent2(keyCode)){
               Intent tempIntent = new Intent();
               ComponentName tempComponentName = new ComponentName("com.ysten.auth", 
                                       "com.ysten.auth.ningxia.ModeSelectActivity");
               tempIntent.setComponent(tempComponentName);
               tempIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
               mContext.startActivity(tempIntent); 
            }
        }
        //end: add by tianchining at 20191118: switch mode

        // Let the application handle the key.
        return 0;
    }
	
    //begin: add by zongzy 20190117: add for jidi wifi control && delaycheck
    private static final int[] ROBOT_KEY_LIST = {
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_DPAD_LEFT
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
            if((keyCode == ROBOT_KEY_LIST1[mCurrentRobotKeyIndex1]||(keyCode==0&&mCurrentRobotKeyIndex1==0)||(keyCode==0&&mCurrentRobotKeyIndex1==keyCount-1))
                && getTopActivityInfo(mContext).equals("com.shcmcc.setting")) {
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

        // begin: add by tianchining at 20191118: menu+up,up,up 
        private static final int[] ROBOT_KEY_LIST2 = {
            KeyEvent.KEYCODE_MENU,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_UP
        };
        private static int mCurrentRobotKeyIndex2 = 0;
        private static boolean robotRunning2 = false;
        private boolean robotKeyComponent2(int keyCode){
            final int keyCount = ROBOT_KEY_LIST2.length;
            if(mCurrentRobotKeyIndex2 >= keyCount) {
                mCurrentRobotKeyIndex2 = 0;
            }
            Log.d(TAG, "TCN_ADD: robotKeyComponent2, keyCode: " + keyCode + ", i: " + mCurrentRobotKeyIndex2); 
            if(keyCode == ROBOT_KEY_LIST2[mCurrentRobotKeyIndex2] 
                //&& getTopActivityInfo(mContext).equals("nx.mobile.iptv") 
                ) {
            
                 mCurrentRobotKeyIndex2++;
                 if(mCurrentRobotKeyIndex2 == keyCount) {
                     return true;
                 }
            }else{
                mCurrentRobotKeyIndex2 = 0;
                if(keyCode == ROBOT_KEY_LIST2[0]) {
                    mCurrentRobotKeyIndex2++;
                    return false;
                }
            }
            return false; 
        }
        //end: add by tianchining at 20191118: menu+up,up,up

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
	
	
	    //begin by ysten.lizheng,20181127,for fujian
    private void sendBroadcastToHome(){
		int homeKey = Settings.System.getInt(mContext.getContentResolver(), "IS_RESPOND_HOME_KEY", 0);
		//begin by lizheng to solve home key no response 20190309
		if (homeKey==1 && getTopPackage(mContext).equals("tv.icntv.ott")){
			Log.d(TAG, "fujian homekey444");
			return;
		}
		//end by lizheng to solve home key no response 20190309
		
 		String authStatus=getValueFromStb("authStatus")+"";
		String user_token=getValueFromStb("user_token")+"";
		Log.d(TAG,"authStatus="+authStatus+","+"user_token="+user_token);
		if(authStatus.equals("AuthSuccess")&&!user_token.equals("")){
            sendCloseSystemWindows(SYSTEM_DIALOG_REASON_HOME_KEY);			
			Intent homeIntent=new Intent();
			ComponentName homeName = new ComponentName("tv.icntv.ott","tv.icntv.ott.icntv");
			homeIntent.setComponent(homeName);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            mContext.startActivity(homeIntent);
			
			Intent intent=new Intent();
	    	intent.setAction("android.intent.action.HOME_KEY_PRESSED");
	   	    Log.d(TAG,"send android.intent.action.HOME_KEY_PRESSED");
            mContext.sendBroadcast(intent);
		}
	}
	
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
	 
	//end by ysten.lizheng,20181127,for fujian
     //add by zhaolianghua for jiangxi start OTT @20181129
     private void startOTTHome(){
	     String username = getValueFromStb("username");
	     String pwd = getValueFromStb("password");
	     //String domain = getValueFromStb("domain");
	     if(!TextUtils.isEmpty(username)&&!TextUtils.isEmpty(pwd)){
		     Intent intent=new Intent();
		     intent.setAction("android.intent.action.HOME_KEY_PRESSED");
		     mContext.sendBroadcast(intent);
		     ComponentName homecomp = new ComponentName("tv.ott.launcher","tv.ott.launcher.WelcomeActivity");
		     Intent homeIntent=new Intent();
		     homeIntent.setComponent(homecomp);
		     homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		     mContext.startActivity(homeIntent);
	     }
     }
     //add by zhaolianghua end
//2017.10, add colorkey.
	private final int FUNTIONKEY_LIVE = 0;
	private final int FUNTIONKEY_LOOKBACK = 1;
	private final int FUNTIONKEY_INFO = 2;
	private final int FUNTIONKEY_VOD = 3;
	private final int FUNTIONKEY_APP = 4;
	private final int FUNTIONKEY_COLLECTION = 5;
	private final int FUNTIONKEY_NULL = -1;
	private int[] mFunkeyArray = new int[6];
    
    
        //begin add by ysten zengzhiliang at 20181120:sync from CM201-2_beijing guo an yu yin model proc
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
        //end add by ysten zengzhiliang at 20181120:sync from CM201-2_beijing guo an yu yin model proc
    //begin add by ysten zengzhiliang at 20181120:sync from CM201-2_beijing 4 color key proc
    private void doFunAction_beijing(int keyCode) {
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

        switch (keyCode) {
            case KeyEvent.KEYCODE_RED:		
                if (keyNumber[0].equals("1")){			
                funActionUrl = "live";
				}else{
				return;
				}
                break;
            case KeyEvent.KEYCODE_GREEN:
                if (keyNumber[3].equals("1")){			
                funActionUrl = "vod";
				}else{
				return;
				} 
                break;
            case KeyEvent.KEYCODE_YELLOW:
                if (keyNumber[4].equals("1")){			
                funActionUrl = "app";
				}else{
				return;
				} 
                break;
            case KeyEvent.KEYCODE_BLUE:
                if (keyNumber[5].equals("1")){			
                funActionUrl = "collection";
				}else{
				return;
				} 
                break;
        }


            funAction = "com.ysten.action.OpenApp";
        

        funIntent.setAction(funAction);
        funIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        funIntent.putExtra("actionUrl", funActionUrl);
        mContext.startActivity(funIntent);
    }
    //end add by ysten zengzhiliang at 20181120:sync from CM201-2_beijing 4 color key proc
	public void ColorFunKey(int keycode){
		ColorFunCtrl();
		if(keycode == KeyEvent.KEYCODE_RED){
			ColorFunKeyStartActivity(mFunkeyArray[0]);
		}
		if(keycode == KeyEvent.KEYCODE_GREEN){
			ColorFunKeyStartActivity(mFunkeyArray[3]);
		}
		if(keycode == KeyEvent.KEYCODE_YELLOW){
			ColorFunKeyStartActivity(mFunkeyArray[4]);
		}
		if(keycode == KeyEvent.KEYCODE_BLUE){
			ColorFunKeyStartActivity(mFunkeyArray[5]);
		}
	}

	public void ColorFunCtrl(){
	//	String mFunkeyString = "100111";
		String mFunkeyString = Settings.System.getString(mContext.getContentResolver(), "funtionkey"); 
	if(mFunkeyString == null){
			mFunkeyString = "100111";
		}
		mFunkeyArray[0]=-1;
		mFunkeyArray[1]=-1;
		mFunkeyArray[2]=-1;
		mFunkeyArray[3]=-1;
		mFunkeyArray[4]=-1;
		mFunkeyArray[5]=-1;

		if(mFunkeyString != null){
			Slog.d(TAG,"===,funtionkey:" + mFunkeyString);
			char [] stringArr = mFunkeyString.toCharArray();
			Slog.d(TAG,"===,arr.len:"+stringArr.length);
			for(int i=0,j=0;i<stringArr.length&&i<6&&j<6;i++,j++){
				if(stringArr[i] =='1'){
					mFunkeyArray[j]=i;
				}
			}
		}
	}

	public void ColorFunKeyStartActivity(int funkey){
		Intent action = new Intent("com.ysten.action.OpenApp");
		action.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		switch(funkey){
			case FUNTIONKEY_LIVE:
				Slog.d(TAG,"===,FUNTIONKEY_LIVE");
				action.putExtra("actionUrl", "live");
				mContext.startActivity(action);
				break;
			case FUNTIONKEY_LOOKBACK:
				Slog.d(TAG,"===,FUNTIONKEY_LOOKBACK");
				action.putExtra("actionUrl", "lookback");
				mContext.startActivity(action);
				break;
			case FUNTIONKEY_INFO:
				Slog.d(TAG,"===,FUNTIONKEY_INFO");
				action.putExtra("actionUrl", "info");
				mContext.startActivity(action);
				break;
			case FUNTIONKEY_VOD:
				Slog.d(TAG,"===,FUNTIONKEY_VOD");
				action.putExtra("actionUrl", "vod");
				mContext.startActivity(action);
				break;
			case FUNTIONKEY_APP:
				Slog.d(TAG,"===,FUNTIONKEY_APP");
				action.putExtra("actionUrl", "app");
				mContext.startActivity(action);
				break;
			case FUNTIONKEY_COLLECTION:
				Slog.d(TAG,"===,FUNTIONKEY_COLLECTION");
				action.putExtra("actionUrl", "collection");
				mContext.startActivity(action);
				break;

			default:
				Slog.d(TAG,"===,colorkey no nothing");
				break;
		}
	}
//2017.10, add colorkey,end

    public boolean isApplicationExsit(String packageName) {
        if (packageName == null || "".equals(packageName)) {
            return false;
        }
        try {
            mContext.getPackageManager().getApplicationInfo(packageName,
                    PackageManager.GET_UNINSTALLED_PACKAGES);
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    private void launchHomeFromHotKeyForSY() {
        String result = Settings.System.getString(mContext.getContentResolver(),
                "Service/ServiceInfo/CLASSICORFASHION");
        if("0".equals(result.trim())) {
            Intent guiseLauncher = new Intent();
            guiseLauncher.setAction("com.CTC_SmartHome.Android.Launcher");
            mContext.sendBroadcast(guiseLauncher);
        }
    }

    private void launchFromHotKey(final HotKeyObj mHotKeyObj) {
        if (mKeyguardDelegate != null && mKeyguardDelegate.isShowingAndNotHidden()) {
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
                        sendCloseSystemWindows(SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS);
                        openapk(mHotKeyObj.packageName, mHotKeyObj.className);
                    }
                }
            });
        } else {
            // no keyguard stuff to worry about, just launch home!
            try {
                ActivityManagerNative.getDefault().stopAppSwitches();
            } catch (RemoteException e) {
            }
            sendCloseSystemWindows(SYSTEM_DIALOG_REASON_GLOBAL_ACTIONS);
            openapk(mHotKeyObj.packageName, mHotKeyObj.className);
        }
    }

    private boolean is_topwindow(String packagename){
        if((packagename == null) || (mFocusedWindow == null))
            return false;
        if(mFocusedWindow.getOwningPackage().equals(packagename))
            return true;
        return false;
    }

    private void openapk(String packageName, String className) {
        Log.d(TAG, "openapk, package: " + packageName + " class: " + className);
        if(checkProcessForeground(packageName)){
            return;
        }
        Intent mintent = new Intent();
        mintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        ComponentName componentName = new ComponentName(packageName, className);
        mintent.setComponent(componentName);
        try {
            mContext.startActivity(mintent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private boolean checkProcessForeground(String packageName) {
        if(packageName == null) {
            Log.w(TAG, "checkProcessForeground, packageName is null!");
            return false;
        }
        List<RunningTaskInfo> list = (mActivityManager != null) ?
                mActivityManager.getRunningTasks(1) : null;
        if(list == null) {
            Log.w(TAG, "checkProcessForeground, list is null!");
            return false;
        }
        Log.d(TAG, "packageName= " + packageName);
        for (RunningTaskInfo info : list) {
            String topPkg = null;
            String basePkg = null;
            if((info != null) && (info.topActivity != null) && (info.baseActivity != null)) {
                topPkg = info.topActivity.getPackageName();
                basePkg = info.baseActivity.getPackageName();
            }
            Log.d(TAG, "topPkg= " + topPkg + " basePkg= " + basePkg);
            if (packageName.equals(topPkg) && packageName.equals(basePkg)) {
                Log.w(TAG, "This process(" + packageName + ") is on foreground");
                return true;
            }
        }
        return false;
    }
    
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
        //begin:add by zhanghk at 20190916:kill some apk when press home key
        if(SystemProperties.get("ro.ysten.province","master").contains("jiangsu")){
	    freeMemory(apkNameListJS);
        }
	//end:add by zhanghk at 20190916:kill some apk when press home key
        if (mKeyguardDelegate != null && mKeyguardDelegate.isShowingAndNotHidden()) {
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
        } else {
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
                                mHandler.postDelayed(mClearHideNavigationFlag, 300/*1000*/);
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
            navVisible |= !mCanHideNavigationBar;
			
			// Loosen conditions for Application Fullscreen
            if (SystemProperties.getBoolean("vplayer.hideStatusBar.enable", false)) {
                final WindowManager.LayoutParams lp = (null != mTopFullscreenOpaqueWindowState)
                    ? mTopFullscreenOpaqueWindowState.getAttrs()
                        : null;
                if( null != lp ){
                    boolean topIsFullscreen = (lp.flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
                    if(topIsFullscreen){
                        navVisible = false;
                    }
                }
            }
            //if never need statusbar , set ro.hideStatusBar=true'', otherwise ,do not set it.
            //if statusbar is shown, but need to hide it for a while , pls set "sys.hideStatusBar.enable=true" 
            boolean mDefHideNavBar = SystemProperties.getBoolean("ro.platform.has.mbxuimode", false);
            if (SystemProperties.getBoolean("persist.sys.hideStatusBar", mDefHideNavBar)) {
                navVisible = false;
            }else {
                if (SystemProperties.getBoolean("sys.hideStatusBar.enable", false))
                    navVisible = false;
            }

            if (mDefHideNavBar) {
                mNavShow = navVisible;
            }

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
            if(SystemProperties.getBoolean("persist.sys.hideStatusBar", SystemProperties.getBoolean("ro.platform.has.mbxuimode", false)))
                cf.bottom = vf.bottom = mDockBottom;
            else
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
                if (!SystemProperties.getBoolean("persist.sys.hideStatusBar", SystemProperties.getBoolean("ro.platform.has.mbxuimode", false))
                    &&(fl & WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION) == 0
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

		if(GameList == null){
			Log.v(TAG,"/system/etc/game_dimension_list.txt");
			GameList = new ArrayList<GameDimInfo>();
			try{
				BufferedReader br = new BufferedReader(new InputStreamReader(
						  new FileInputStream("/system/etc/game_dimension_list.txt")));    
				String line =""; 		   
				GameDimInfo item= null;
				while ((line = br.readLine()) != null){
					if (localLOGV)  Log.d(TAG, "game dimension :" +line);
					String game[] = line.split(":");	  
					String dim[] = game[1].split("X");
					int w=Integer.parseInt(dim[0].trim());
					int h=Integer.parseInt(dim[1].trim());
					item = new GameDimInfo(game[0].trim(), w,h ); 
					GameList.add(item);			 
				}			  
				br.close();  
			}catch(java.io.FileNotFoundException ex){	   
			}catch(java.io.IOException ex){		  
			} 	 
		}
        
		for(int i = 0; i < GameList.size(); i++){
			if(attrs.getTitle().toString().contains(GameList.get(i).name)){
                pf.left = df.left = cf.left = (mUnrestrictedScreenWidth-GameList.get(i).width)/2;
                pf.top = df.top = cf.top = (mRestrictedScreenHeight-GameList.get(i).height)/2;
                pf.right = df.right = cf.right = mUnrestrictedScreenWidth-cf.left;
                pf.bottom = df.bottom = cf.bottom = mRestrictedScreenHeight-cf.top;		  
                Log.v(TAG,"dim: "+cf.left+" "+cf.top+" "+cf.right+" "+cf.bottom );
			}						
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
                    if (mAppsToBeHidden.isEmpty()) {
                        if (showWhenLocked) {
                            if (DEBUG_LAYOUT) Slog.v(TAG, "Setting mHideLockScreen to true by win " + win);
                            mHideLockScreen = true;
                            mForceStatusBarFromKeyguard = false;
                        }
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
        	if(SystemProperties.getBoolean("sys.statusbar.forcehide",false)== true){
                mStatusBar.hideLw(false);
                if (mTopFullscreenOpaqueWindowState != null) 
                    topIsFullscreen = (lp.flags & WindowManager.LayoutParams.FLAG_FULLSCREEN) != 0;
        	}
            else{
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
    
    private Timer mAutoSuspendTimer = new Timer();
 
    public void enableAutoSuspend() {
        disableAutoSuspend();
        int def_timeout = 2 * 60 * 1000; //default 2min
        int timeout = SystemProperties.getInt("persist.sys.autosuspend.timeout", def_timeout);
	//begin:add by zhanghk at 20190604:add zhejiang hdmi standby time
	if(SystemProperties.get("ro.ysten.province","master").contains("CM201_zhejiang")){
	    timeout = 5 * 60 * 1000; //5 minute
	}
	//end:add by zhanghk at 20190604:add zhejiang hdmi standby time
        Log.d(TAG, "enableAutoSuspend timeout: " + timeout);
        
        if (timeout > 0) {
            Slog.d(TAG, "enable auto suspend");
            mAutoSuspendTimer = new Timer();
            TimerTask task = new TimerTask(){
                public void run() {
                    Slog.d(TAG, "goto auto suspend");
                    String proj_type = SystemProperties.get("sys.proj.type", "ott");
                    String tender_type = SystemProperties.get("sys.proj.tender.type", null);
                    Log.i(TAG, "Auto suspend sys.proj.type: " + proj_type + ", sys.proj.tender.type: " + tender_type);
                    if ("telecom".equals(proj_type) && "jicai".equals(tender_type)) {
                        sendKeyEvent(KeyEvent.KEYCODE_HOME);
                    }
                    String rx_sense = mSystemWriteManager.readSysfs("/sys/class/switch/hdmi_rxsense/state");
                    if("0".equals(rx_sense))
                        mPowerManager.goToSleep(SystemClock.uptimeMillis());
                }
            };

            mAutoSuspendTimer.schedule(task, timeout);
            isTvSuspend = false;
        }
    }
	
    private void disableAutoSuspend() {
        Slog.d(TAG, "disable auto suspend");
        if(mAutoSuspendTimer != null) {
            mAutoSuspendTimer.cancel();
            mAutoSuspendTimer = null;
        }
    }

    private void disableCecSuspend() {
        Slog.d(TAG, "disable cec suspend");
        if (null != mOberserCecSuspendTimer) {
            mOberserCecSuspendTimer.cancel();
            mOberserCecSuspendTimer = null;
        }
    }

	//add by liuxl at 20181127 for A20 start
	private void switchSoundOutput(boolean plugged){
        String isCM502 = SystemProperties.get("persist.sys.iscm502", "false");
               if("true".equals(isCM502)){
                       switchSoundOutput_CM502(plugged);
                       return;
               }
		String isA20 = SystemProperties.get("persist.sys.isa20", "false");
		if("true".equals(isA20)){
			String rx_sense = mSystemWriteManager.readSysfs("/sys/class/switch/hdmi_rxsense/state");
			String hpd_state = mSystemWriteManager.readSysfs("/sys/class/amhdmitx/amhdmitx0/hpd_state");

			Log.d(TAG, "switchSoundOutput hpd_state  =" + hpd_state + "	rx_sense  =" + rx_sense);
			if(rx_sense.contains("1") && hpd_state.contains("1"))
			{
		   // HDMI in
				Log.d(TAG, "HDMI IN");
				mContext.sendBroadcast(new Intent("com.ysten.hdmi.in"));
				String audioMode = SystemProperties.get("persist.sys.audio.mode", "0");
				String setMode = SystemProperties.get("persist.sys.audio.setmode", "0");
				if (audioMode.equals("0") || setMode.equals("0")) {
					Log.d(TAG, "HDMI IN TV mode");
					SystemProperties.set("persist.sys.audio.mode", "0");
					mhisys.rootSystem("echo unmute >  /sys/devices/aml_m8_snd.50/cvbs_mute");
					mhisys.rootSystem("echo 180:0 > /sys/class/gpio/gpio_dbg");
					mhisys.rootSystem("echo audio_on > /sys/class/amhdmitx/amhdmitx0/config");
					//remove by zhaolianghua cause this make cvbs mute in DragonBox test
					/*mhisys.rootSystem("echo 170:0 > /sys/class/gpio/gpio_dbg");
					mhisys.rootSystem("echo 176:0 > /sys/class/gpio/gpio_dbg");
					mhisys.rootSystem("echo 169:0 > /sys/class/gpio/gpio_dbg");*/
				}
			}
			else
			{
				ActivityManager activityManager=(ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);  
				String currentPackage=activityManager.getRunningTasks(1).get(0).topActivity.getPackageName();
				Log.d(TAG, "HDMI currentPackage:" + currentPackage);
				if("com.softwinner.dragonbox".equals(currentPackage)){
					return;
				}
				Log.d(TAG, "HDMI out");
				mContext.sendBroadcast(new Intent("com.ysten.hdmi.out"));
				SystemProperties.set("persist.sys.audio.mode", "1");
				//mhisys.rootSystem("echo audio_off > /sys/class/amhdmitx/amhdmitx0/config");
				mhisys.rootSystem("echo mute >  /sys/devices/aml_m8_snd.50/cvbs_mute");
				String speakerClose = SystemProperties.get("persist.sys.speaker.close", "false");
				String speakerouting = SystemProperties.get("sys.yst.speaker.outing", "false");//add by xiulong for neimeng at 20190605
				if(speakerClose.equals("false") || speakerClose.equals("true"))//add by xiulong for neimeng at 20190605
					mhisys.rootSystem("echo 180:1 > /sys/class/gpio/gpio_dbg");	
			}
		}
	}
	//add by liuxl end
	
	private void switchSoundOutput_CM502(boolean plugged){
               String isCM502 = SystemProperties.get("persist.sys.iscm502", "false");
               if("true".equals(isCM502)){
                       String rx_sense = mSystemWriteManager.readSysfs("/sys/class/switch/hdmi_rxsense/state");
                       String hpd_state = mSystemWriteManager.readSysfs("/sys/class/amhdmitx/amhdmitx0/hpd_state");

                       Log.d(TAG, "switchSoundOutput hpd_state  =" + hpd_state + "     rx_sense  =" + rx_sense);
                       if(rx_sense.contains("1") && hpd_state.contains("1"))
                       {
                  // HDMI in
                               Log.d(TAG, "HDMI IN");
                               mContext.sendBroadcast(new Intent("com.ysten.hdmi.in"));
                               String audioMode = SystemProperties.get("persist.sys.audio.mode", "0");
                               String setMode = SystemProperties.get("persist.sys.audio.setmode", "0");
                              if (audioMode.equals("0") || setMode.equals("0")) {
                                       Log.d(TAG, "HDMI IN TV mode");
                                       SystemProperties.set("persist.sys.audio.mode", "0");
                                       //mhisys.rootSystem("echo unmute >  /sys/devices/aml_m8_snd.50/cvbs_mute");
                                       mhisys.rootSystem("echo 151:0 > /sys/class/gpio/gpio_dbg");
                                       mhisys.rootSystem("echo audio_on > /sys/class/amhdmitx/amhdmitx0/config");
                               }
                       }
                       else
                       {
                               ActivityManager activityManager=(ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);  
                               String currentPackage=activityManager.getRunningTasks(1).get(0).topActivity.getPackageName();
                               Log.d(TAG, "HDMI currentPackage:" + currentPackage);
                              if("com.softwinner.dragonbox".equals(currentPackage)){
                                       return;
                               }
                               Log.d(TAG, "HDMI out");
                               mContext.sendBroadcast(new Intent("com.ysten.hdmi.out"));
                               SystemProperties.set("persist.sys.audio.mode", "1");
                               //mhisys.rootSystem("echo audio_off > /sys/class/amhdmitx/amhdmitx0/config");
                               //mhisys.rootSystem("echo mute >  /sys/devices/aml_m8_snd.50/cvbs_mute");
                               mhisys.rootSystem("echo 151:1 > /sys/class/gpio/gpio_dbg");
                       }
               }
       }
	
    void setHdmiHwPlugged(boolean plugged) {
        int ubootenv_reloading = SystemProperties.getInt("sys.ubootenv.reload", 1);
        if (ubootenv_reloading == -1 || ubootenv_reloading == 0) {
            return;
        }
        String hdmi_pluged = mSystemWriteManager.readSysfs(HDMI_PLUG_STATE_PATH);
        /* walkaround, if parameter plugged is not as the real value, return */
        if (hdmi_pluged != null && hdmi_pluged.contains("1") && plugged == false)
            return;
        else if (hdmi_pluged != null && hdmi_pluged.contains("0") && plugged == true)
            return;
        synchronized(mHdmiHwPluggedLock){
            if ((mHdmiHwPlugged != plugged) || isTvSuspend ) {
                Slog.e(TAG, "setHdmiHwPlugged " + plugged);
                Log.d("PhoneWindownManager", "isTvSuspend : " + isTvSuspend);
                mHdmiHwPlugged = plugged;
                if(SystemProperties.getBoolean("persist.sys.autosuspend.hdmi", false)) {  
                    if (plugged && !isTvSuspend) {
                        disableAutoSuspend();
                     } else {
                        enableAutoSuspend();
                    }
                }

                if (SystemProperties.getBoolean("ro.platform.has.mbxuimode", false)){
                    if (plugged)
                        mMboxOutputModeManager.setHdmiPlugged();
                    else
                        mMboxOutputModeManager.setHdmiUnPlugged();
                }
				//add by liuxl at 20181127 for A20
				switchSoundOutput(plugged);
                Intent intent = new Intent(WindowManagerPolicy.ACTION_HDMI_HW_PLUGGED);
                intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
                intent.putExtra(EXTRA_HDMI_HW_PLUGGED_STATE, plugged);
                mContext.sendStickyBroadcastAsUser(intent, UserHandle.OWNER);

                if (SystemProperties.getBoolean("ro.vout.dualdisplay", false)) {
                    setDualDisplay(plugged);
                    
                    Intent it = new Intent(WindowManagerPolicy.ACTION_HDMI_PLUGGED);
                    it.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
                    it.putExtra(WindowManagerPolicy.EXTRA_HDMI_PLUGGED_STATE, plugged);
                    mContext.sendStickyBroadcastAsUser(it, UserHandle.OWNER);
                }
            }
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

    private static final String VIDEO2_CTRL_PATH = "/sys/class/video2/clone";
    private static final String VFM_CTRL_PATH = "/sys/class/vfm/map";
    private static int writeSysfs(String path, String val) {
        if (!new File(path).exists()) {
            Log.e(TAG, "File not found: " + path);
            return 1; 
        }
        
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(path), 64);
            try {
                writer.write(val);
            } finally {
                writer.close();
            }    		
            return 0;
        		
        } catch (IOException e) { 
            Log.e(TAG, "IO Exception when write: " + path, e);
            return 1;
        }                 
    }
    private static void setDualDisplay(boolean hdmiPlugged) {
        String isCameraBusy = SystemProperties.get("camera.busy", "0");

        if (!isCameraBusy.equals("0")) {
            Log.w(TAG, "setDualDisplay, camera is busy");
            return;
        }    
        
        if (hdmiPlugged) {
            writeSysfs(VIDEO2_CTRL_PATH, "0");
            writeSysfs(VFM_CTRL_PATH, "rm default_ext");
            writeSysfs(VFM_CTRL_PATH, "add default_ext vdin0 amvideo2");
            writeSysfs(VIDEO2_CTRL_PATH, "1");
        } else {
            writeSysfs(VIDEO2_CTRL_PATH, "0");
            writeSysfs(VFM_CTRL_PATH, "rm default_ext");
            writeSysfs(VFM_CTRL_PATH, "add default_ext vdin vm amvideo");
        }    	
    }

    public static String getCurDisplayMode() {
        String modeStr;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(DISPLAY_MODE_PATH), 32);
            try {
                modeStr = reader.readLine();  
            } finally {
                reader.close();
            } 
            return (modeStr == null)? "panel" : modeStr; 

        } catch (IOException e) { 
            Log.e(TAG, "IO Exception when read: " + DISPLAY_MODE_PATH, e);
            return "panel";
        }
    }

    void initializedHdmiCec(){
	if (new File("/sys/devices/virtual/switch/lang_config/state").exists())
		mCedObserver.startObserving("DEVPATH=/devices/virtual/switch/lang_config");
        if (SystemProperties.getBoolean("persist.sys.autosuspend.cec.enable", false))
        {
            writeSysfs("/sys/class/amhdmitx/amhdmitx0/cec_config", "0x2f");
        }
        else
        {		
            writeSysfs("/sys/class/amhdmitx/amhdmitx0/cec_config", "0x0");
        }
			
	
    }
	
    void initializedHdmiCecSuspend(){
	if (new File("/sys/devices/virtual/switch/cec_suspend/state").exists())
		mCecSuspendObserver.startObserving("DEVPATH=/devices/virtual/switch/cec_suspend");
    }

    void initializedHdmiHDR(){
	if (new File("/sys/devices/virtual/switch/hdmi_hdr/state").exists())
		mHdrObserver.startObserving("DEVPATH=/devices/virtual/switch/hdmi_hdr");
    }

	void initHdmiRxSenseState(){
        if(new File("/sys/class/switch/hdmi_rxsense/state").exists()){
            mRxSenseObserver.startObserving("SWITCH_NAME=hdmi_rxsense");
            //mRxSenseObserver.startObserving("DEVPATH=/devices/virtual/switch/hdmi_rxsense");
        }
    }
    void initializeHdmiState() {
        boolean plugged = false;
        // watch for HDMI plug messages if the hdmi switch exists
        if (new File("/sys/class/switch/hdmi_hpd/state").exists()) {
            if (SystemProperties.getBoolean("ro.platform.has.mbxuimode", false)){
                SystemProperties.set("sys.boot.logo", "android");
                if(SystemProperties.getBoolean("ro.hw.cvbs.onboard", true) && !SystemProperties.getBoolean("ro.hdmiplugdetect.dis", false)){
                    mMboxOutputModeManager.initOutputMode();
                    mHDMIObserver.startObserving(HDMI_TX_PLUG_UEVENT);
                }
            } else {
                mHDMIObserver.startObserving(HDMI_TX_PLUG_UEVENT);
            }


            final String filename = "/sys/class/switch/hdmi_hpd/state";
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
        //mHdmiHwPlugged = !plugged;
        //setHdmiHwPlugged(!mHdmiHwPlugged);
        
        mHdmiHwPlugged =  plugged;
        if (!SystemProperties.getBoolean("ro.vout.dualdisplay", false)) {
            if (getCurDisplayMode().equals("panel") || !plugged || SystemProperties.getBoolean("ro.platform.has.mbxuimode", false)) {
                plugged = false;
            }
        }
        
        if (SystemProperties.getBoolean("ro.vout.dualdisplay", false)) {
            setDualDisplay(plugged);
        }
        
        if (SystemProperties.getBoolean("ro.vout.dualdisplay2", false)) {
            plugged = false;
            setDualDisplay(plugged);
        }        
		 
        Intent it = new Intent(WindowManagerPolicy.ACTION_HDMI_PLUGGED);
        it.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        it.putExtra(WindowManagerPolicy.EXTRA_HDMI_PLUGGED_STATE, plugged);
        mContext.sendStickyBroadcastAsUser(it, UserHandle.OWNER);
    }

    void initializedHoldkeyState(  IWindowManager windowManager ) {
        boolean hold = false;
        if (new File("/sys/devices/virtual/switch/hold_key/state").exists()) {
            mHoldObserver.startObserving("DEVPATH=/devices/virtual/switch/hold_key");
            final String filename = "/sys/class/switch/hold_key/state";
            FileReader reader = null;
            try {
                reader = new FileReader(filename);
                char[] buf = new char[15];
                int n = reader.read(buf);
                if (n > 1) {
                    hold = 0 != Integer.parseInt(new String(buf, 0, n-1));
                }

                if(hold){
                    Log.d("TabletStatusBar","rotateStatus = 1");
                    windowManager.thawRotation();
                }else{
                    Log.d("TabletStatusBar","rotateStatus = 0");
                    windowManager.freezeRotation(-1);//use current orientation
                }
            } catch (IOException ex) {
                Slog.w(TAG, "Couldn't read hold_key state from " + filename + ": " + ex);
            } catch (NumberFormatException ex) {
                Slog.w(TAG, "Couldn't read hold_key state from " + filename + ": " + ex);
            }catch (RemoteException exc) {
                Log.w(TAG, "Unable to save auto-rotate setting");
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ex) {
                    }
                }
            }
        }
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

    class MapKey{
        int srcKey;
        int desKey;
        int longTime;
        int period;
        
        @Override 
        public String toString() {
            return "srcKey:" + KeyEvent.keyCodeToString(srcKey) + 
                " desKey:" + KeyEvent.keyCodeToString(desKey) + 
                " longTime:" + longTime + " period:" + period;
        }
    }

    private static final String FILE_PATH   = "/system/etc/keymapping.txt";
    private ArrayList<MapKey> mMapKeyList = new ArrayList<MapKey>();
    private MapKey mMapKey = null;
    private boolean mMapKeyLongPress = false;
    private boolean mMapKeyTimerStarted = false;
    private boolean mMapMenuKey = false;
    private java.util.Timer mKeyTimer;
    
    private void sendMapKey(int keyCode, boolean actionDown) {
        //Log.i(TAG, "sendMapKey key:" + KeyEvent.keyCodeToString(keyCode) + (actionDown?" down":" up"));
        
        long now = SystemClock.uptimeMillis();
        try {
            KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0);
            KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0);
            if(actionDown){
                (IInputManager.Stub
                    .asInterface(ServiceManager.getService("input")))
                    .injectInputEvent(down, 0);
            }else{
                (IInputManager.Stub
                    .asInterface(ServiceManager.getService("input")))
                    .injectInputEvent(up, 0);
            }
        } catch (RemoteException e) {
            Log.i(TAG, "DeadOjbectException:" + e);
        }
    }
    
    /*
        parse map key file, from a origin key map to destination key.
        now, support volume down/up map to other key
    */
    private void parseMapFile(){
        BufferedReader br = null;
        try{
            br = new BufferedReader(new InputStreamReader(new FileInputStream(FILE_PATH)));    
            String line ="";           
            while ((line = br.readLine()) != null){
                if('#' == line.charAt(0)){//this is mark information, skip
                    continue;
                }
                
                String strs[] = line.split(" "); 
                if(4 != strs.length){//the config file invalid, skip
                    continue;
                }
                
                MapKey key = new MapKey();
                key.srcKey = KeyEvent.keyCodeFromString("KEYCODE_" + strs[0]);
                key.desKey = KeyEvent.keyCodeFromString("KEYCODE_" + strs[1]);
                key.longTime = Integer.parseInt(strs[2]);
                key.period = Integer.parseInt(strs[3]);
                
                if((KeyEvent.KEYCODE_UNKNOWN == key.srcKey) || 
                   (KeyEvent.KEYCODE_UNKNOWN == key.desKey)){
                    Log.w(TAG, "mapKey not found key code");
                    continue;
                }
                mMapKeyList.add(key);
                
                Log.i(TAG, "mapkey info:" + key);
            }             
        }catch(IOException ex){ 
            Log.e(TAG, "mapkey exception:" + ex);
        }finally{
            try {
                if(null != br){
                    br.close(); 
                }
            } catch (IOException ex) {
            }
        }  
    }

    private void startSendKeyTimer(long delay, long period){
		//Log.v(TAG, "start send key timer delay:" + delay + " ms" + " period:" + period + " ms");
		
		stopSendKeyTimer();
		
        mKeyTimer = new java.util.Timer();
        mKeyTimer.schedule(
            new java.util.TimerTask(){   
                public void run(){ 
                    if(mMapKeyTimerStarted){
                        mMapKeyLongPress = true;
                        sendMapKey(mMapKey.desKey, true);
                    }
                }   
            },
            delay, 
            period); //period:100ms
    			
        mMapKeyTimerStarted = true;
	}

	private void stopSendKeyTimer(){
		//Log.v(TAG, "stop send key timer, timer:" + mKeyTimer);
        mMapKeyTimerStarted = false;
		if( null != mKeyTimer ){
			mKeyTimer.cancel();
			mKeyTimer.purge();
			mKeyTimer = null;
		}
	}
    
    private void processMapKey(boolean down, int keyCode){
        if(mMapKeyList.isEmpty()){
            Log.i(TAG, "mapkey no map key list");
            return;
        }

        int i;
        MapKey key = null;
        for(i = 0; i < mMapKeyList.size(); i++){
            key = mMapKeyList.get(i);
            if(key.srcKey == keyCode){
                break;
            }
        }

        if(i >= mMapKeyList.size()){//not find need map key
            return;
        }

        mMapKey = key;
        if (down) {
            startSendKeyTimer(key.longTime, key.period);
        } else {
            stopSendKeyTimer();
            sendMapKey(key.desKey, false);
        }
    }

    private void killIcntvApk() {
        String[] cmds ={"tv.icntv.ott:sub"};
        int[] pids = Process.getPidsForCommands(cmds);
        if (pids != null) {
            for (int pid : pids) {
                Log.d(TAG , "killIcntvApk pid is: " + pid);
                Process.killProcess(pid);
            }
        }
    }

    // handle ISTV demand
    private void handleLongPressForIsTV(int keyCode,boolean istvstart)
    {
        String keyvalueistv="";
		
        switch(keyCode) {
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

        if(istvstart) {
            Intent mIntent = new Intent("com.ysten.intent.action.KEY_LONG_PRESSED_START");
	        mIntent.putExtra("keyName",keyvalueistv);
            mIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
	        mContext.sendBroadcast(mIntent);
		    Log.e("ysten", "com.ysten.intent.action.KEY_LONG_PRESSED_START "+keyvalueistv);
        } else {
            Intent mIntent = new Intent("com.ysten.intent.action.KEY_LONG_PRESSED_END");
	        mIntent.putExtra("keyName",keyvalueistv);
		    mIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
	        mContext.sendBroadcast(mIntent);
            Log.e("ysten", "com.ysten.intent.action.KEY_LONG_PRESSED_END "+keyvalueistv);
        }
    }

    private void doFuncationFJ(int keyCode) {
        String packageName = getTopPackage(mContext);
        Log.i(TAG,"top activity is "+packageName);
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
                funActionUrl = "collection";
                break;
        }

        //if(isActivityRunning(mContext, "tv.icntv.ott.Gefo")) {
           // funAction = "com.ysten.action.OpenGefo"; 
        //} else {
            funAction = "com.ysten.action.OpenApp";
        //}

        funIntent.setAction(funAction);
        funIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        funIntent.putExtra("actionUrl", funActionUrl);
		//funIntent.putExtra("type", "1");
        mContext.startActivity(funIntent);
    }
	
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
    /** {@inheritDoc} */
    @Override
    public int interceptKeyBeforeQueueing(KeyEvent event, int policyFlags, boolean isScreenOn) {
        if (!mSystemBooted) {
            // If we have not yet booted, don't let key events do anything.
            return 0;
        }

        //for quick boot
        synchronized (mKeyDispatchLock) {
            if (KEY_DISPATCH_MODE_ALL_DISABLE == mKeyDispatcMode){
                return 0;
            }
        }

        if ("mobile".equals(SystemProperties.get("sys.proj.type", "ott"))
                && mWindowManagerFuncs.isShutdownProcessDialogShow()) {
            return 0;
        }

        final boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        final boolean canceled = event.isCanceled();
        final int keyCode = event.getKeyCode();

        final boolean isInjected = (policyFlags & WindowManagerPolicy.FLAG_INJECTED) != 0;

        // If screen is off then we treat the case where the keyguard is open but hidden
        // the same as if it were open and in front.
        // This will prevent any keys other than the power button from waking the screen
        // when the keyguard is hidden by another activity.
        final boolean keyguardActive = (mKeyguardDelegate == null ? false :
                                            (isScreenOn ?
                                                mKeyguardDelegate.isShowingAndNotHidden() :
                                                mKeyguardDelegate.isShowing()));

        if (keyCode == KeyEvent.KEYCODE_POWER||keyCode == KeyEvent.KEYCODE_STB_POWER) {
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
            Log.d(TAG, "screen off! down="+down+" isWakeKey="+isWakeKey+" isWakeKeyWhenScreenOff(keyCode)="+isWakeKeyWhenScreenOff(keyCode));
            if (down && isWakeKey && isWakeKeyWhenScreenOff(keyCode)) {
                if(mWindowManagerFuncs.isReboot() || 
				(SystemProperties.get("sys.proj.type", "ott").equals("mobile") && suspendbycec)){
                    //begin: add by xuyunfeng at 20191122: ningxia iptv fake standby
                    if(SystemProperties.get("ro.ysten.province", "master").contains("master")){ //add by xuyunfeng at 20191224:fallback ningxia fake standby
                        Log.d(TAG, "press power key, fake standby!");
                        result |= ACTION_WAKE_UP;
                    //end: add by xuyunfeng at 20191122: ningxia iptv fake standby
                    }else{
                        Log.d(TAG, "press power key, reboot system!");
                        PowerManager pm = (PowerManager) mContext.getSystemService(mContext.POWER_SERVICE);
                        pm.reboot("");
                        result &= ~ACTION_PASS_TO_USER;
                        return result;
                    }
                } else {
                    result |= ACTION_WAKE_UP;
                }
            }
        }

        //tellen 20130201 add for long press key mapping
        processMapKey(down, keyCode);

        // If the key would be handled globally, just return the result, don't worry about special
        // key processing.
        if (mGlobalKeyManager.shouldHandleGlobalKey(keyCode, event)) {
            return result;
        }

        //begin: add by ysten.tianchi at 20190708: handle home key
        if(keyCode == KeyEvent.KEYCODE_HOME){
            if(SystemProperties.get("ro.ysten.province", "master").contains("ningxia")){
                String authStatus = getValueFromStb("authStatus")+"";
                if(!"AuthSuccess".equals(authStatus)){
                    return 0;
                }
            }
        }
        //end: add by ysten.tianchi at 20190708: handle home key


        if((keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_SETTINGS 
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE 
                || keyCode == KeyEvent.KEYCODE_POWER || keyCode == KeyEvent.KEYCODE_STB_POWER) 
                && (is_topwindow("net.sunniwell.service.swupgrade.usb") || is_topwindow("net.sunniwell.service.swupgrade.chinamobile"))) {
            //Log.d(TAG, "discard key when usb copy upgrade file.");
            if ("mobile".equals(SystemProperties.get("sys.proj.type", "ott"))) {
                broadcastMobileKeyEvent(event);
            }
            result &= ~ACTION_PASS_TO_USER;
            return result;
        }
		//add by ysten-mark for heilongjiang broadcast button
		if((keyCode != 136) && SystemProperties.get("ro.ysten.province", "master").contains("heilongjiang")){
           isIcntvLive=false;
		   Log.d(TAG, "heilongjiang isIcntvLive false");
		}

        // Handle special keys.
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_MUTE: {
                /*boolean mIsBtvPlyShow = false;
                if((mFocusedApp.toString().contains("com.bestv.ott.mediaplayer.activity.BestvPlayer")
                        || mFocusedApp.toString().contains("com.bestv.online.activity.ChainNewsActivity")))
                    mIsBtvPlyShow = true;*/
                if (SystemProperties.get("ro.ysten.province").equals("cm201_hebei")){
                    if((keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                              keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                              keyCode == KeyEvent.KEYCODE_VOLUME_MUTE)){
                        String topPackage = getTopPackage(mContext);
                        Log.d(TAG,"hebei  volumequeue topPackage = "+topPackage);
                        boolean topHEActivity = false;
                        if(topPackage.equals("com.android.smart.terminal.iptv")) {
                            Log.d(TAG,"hebei  test-true");
                            topHEActivity = true;
                        }
                        if(topHEActivity){
                            Log.d(TAG,"hebei  test-return 0");
                            return 1;
                        }
	                }
                }
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    /*if(mIsBtvPlyShow) {
                        if(mAudioManager.isMute()) {
                            mAudioManager.setUnMute(true);
                            return 0;
                        }
                        if(!down) {
                            mVolumeDownKeyTriggered = false;
                            mAudioManager.volumePanelDismiss();
                            return 0;
                        }
                    }*/
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
                   /* if(mIsBtvPlyShow) {
                        if(mAudioManager.isMute()) {
                            mAudioManager.setUnMute(true);
                            return 0;
                        }
                        if(!down) {
                            mVolumeUpKeyTriggered = false;
                            mAudioManager.volumePanelDismiss();
                            return 0;
                        }
                    }*/
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
                }else if((keyCode == KeyEvent.KEYCODE_VOLUME_MUTE)
                        || (keyCode == KeyEvent.KEYCODE_MUTE)) {
                        Log.d(TAG, "........................mute");   
                        //begin by lizheng 20181129 for fujian iptv mute key						
						if(SystemProperties.get("ro.ysten.province","master").contains("fujian")
							&& (SystemProperties.getInt("persist.sys.launcher.value",0)==2)){
								if (down) {
							doIPTVFuncationFJ(keyCode);
								}
						}else{
						SystemProperties.set("sys.keycode_mute", "true");	
						}
						//end by lizheng 20181129 for fujian iptv mute key
                   /* if(mIsBtvPlyShow) {
                        if(down) {
                            if(mAudioManager.isMute()) {
                                mAudioManager.setUnMute(true);
                            } else {
                                mAudioManager.setMute(true);
                            }
                        }
                        return 0;
                    }*/
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
            case KeyEvent.KEYCODE_SHIFT_LEFT: {
                if (down) {
                    Message msg = Message.obtain();
                    msg.what = MSG_OPEN_BLUETOOTH_SWICH;
                    mHandler.sendMessage(msg);
                }
                break;
            }

            case KeyEvent.KEYCODE_F11: {
                if (down) {
                    Log.d(TAG,"cec_cancel_key");
                    disableCecSuspend();
                }
                break;
            }
            case KeyEvent.KEYCODE_MOBILE_M: {
                if (down&&(SystemProperties.get("ro.ysten.province","master").equals("CM201_guangdong"))){
                    Log.d("zjj","M key");
                     Intent intent = new Intent();
               intent.setComponent(new ComponentName("cn.gd.snm.appstore", "cn.gd.snm.appstore.MainActivity"));
               intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
               mContext.startActivity(intent);  
                }
                else{
                  Toast.makeText(mContext, "this is M KEY", Toast.LENGTH_LONG).show();
                }
                break;
            }
           //end by ysten.zhangjunjian,20190910 for M key

            case KeyEvent.KEYCODE_F12: {
                if (down) {
                     //Log.d(TAG,"remote_control_battery_low");
                     //mHandler.post(remoteControlLowBatteryToastRunnable);
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

            //case KeyEvent.KEYCODE_POWER:
            case KeyEvent.KEYCODE_STB_POWER: {
                String bootvideo_state = SystemProperties.get("init.svc.bootvideo", "stopped");
                if ("running".equals(bootvideo_state)) {
                    Log.d(TAG, "bootvideo is running, break the power press thread");
                    break;
                }
                disableCecSuspend();
				if(SystemProperties.get("ro.ysten.custom.type","none").equals("viper")
					&& !SystemProperties.get("ro.ysten.province","master").contains("jidi")
					&& !SystemProperties.get("ro.ysten.province","master").contains("beijing"))
				{
					Log.d(TAG, "custom.type:viper7");
					if (down && isScreenOn) {
						mContext.startService(new Intent("com.ysten.action.poweroff"));
					}
					break;
				}
                if ("mobile".equals(SystemProperties.get("sys.proj.type", "ott"))) {
                    broadcastMobileKeyEvent(event);
                }
                result &= ~ACTION_PASS_TO_USER;				
                if (down) {
                    if("true".equalsIgnoreCase(SystemProperties.get("ro.platform.has.mbxuimode"))) {
                        Intent inte=new Intent("com.android.music.musicservicecommand.pause");
                        mContext.sendBroadcast(inte);
                        
                        OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
                            public void onAudioFocusChange(int focusChange) {
                                Log.e(TAG, "+++****====: onAudioFocusChange");
                            }
                        };
                        AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
                        mAudioManager.abandonAudioFocus(mAudioFocusListener);

                        inte = new Intent("com.amlogic.hdmiin.pause");
                        mContext.sendBroadcast(inte);
                    }

                    mImmersiveModeConfirmation.onPowerKeyDown(isScreenOn, event.getDownTime(),
                            isImmersiveMode(mLastSystemUiFlags));
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
                    interceptPowerKeyDown(!isScreenOn || hungUp
                            || mVolumeDownKeyTriggered || mVolumeUpKeyTriggered);
                } else {
                    mPowerKeyTriggered = false;
                    cancelPendingScreenshotChordAction();
                    if (interceptPowerKeyUp(mHdmiPlugged || canceled || mPendingPowerKeyUpCanceled)) {
                        String proj_type = SystemProperties.get("sys.proj.type", "ott");
                        String tender_type = SystemProperties.get("sys.proj.tender.type", null);
                        Log.d(TAG, "press power key, proj_type is " + proj_type + ", tender_type is " + tender_type);
                        //begin:add by zhanghk at 20190522:directly powerOff when press power key
                        if(SystemProperties.get("ro.ysten.province","master").contains("CM201_zhejiang")){
                            mPowerManager.goToSleep(SystemClock.uptimeMillis());
                            break;
                        }
                        //begin:add by zhanghk at 20190522:directly powerOff when press power key
                        //begin:add by zhanghk at 20191224:screen off when press power key
                        if(SystemProperties.get("ro.ysten.province","master").contains("hubei")){
                            if(SystemProperties.get("sys.fake.standby", "1").equals("1")){
                                if(!TextUtils.isEmpty(SystemProperties.get("dhcp6c.eth0.dns1", "")) && ("0".equals(SystemProperties.get("sys.v6net.ok","0")))){
                                    SystemProperties.set("sys.v6net.ok","1");
                                }
                                isScreenOn = mPowerManager.isScreenOn();
                                if(isScreenOn){
                                   try{
                                      (new File("/params/standbyFlag")).createNewFile();
                                      Log.d(TAG,"standbyFlag is exist? "+(new File("/params/standbyFlag")).exists());
                                   }catch(Exception e){
                                      e.printStackTrace();
                                   }
                                   mPowerManager.forceScreenOff();
                                }else{
                                   mPowerManager.wakeUp(SystemClock.uptimeMillis());
                                }
                                break;
                            }
                        }
                        //end:add by zhanghk at 20191221:screen off when press power key
                        if (suspendbycec) {
                            SystemProperties.set("sys.cec_off", "1");
                        }

                        if (("mobile".equals(proj_type) && !suspendbycec)
                                || ("telecom".equals(proj_type) && "sichuan".equals(tender_type))) {
				if(!("mobile".equals(proj_type) && mPowerKeyLongEventHandled)) {
					if(!mWindowManagerFuncs.isShutdownDialogShow() || !mWindowManagerFuncs.isShutdownProcessDialogShow()){
						Intent vPlayerPowerKey=new Intent("com.amlogic.vplayer.powerkey");
					        mContext.sendBroadcast(vPlayerPowerKey);
						mHandler.post(mPowerShortPress);
					} else {
						Log.w(TAG, "Shutdown window is show, do not show again!" );
					}
				}
                        } else if ("mobile".equals(proj_type) && "sichuan".equals(tender_type)) {
                            result = (result & ~ACTION_WAKE_UP) | ACTION_GO_TO_SLEEP;
                            killIcntvApk();
                            Intent vPlayerPowerKey=new Intent("com.amlogic.vplayer.powerkey");
                            mContext.sendBroadcast(vPlayerPowerKey);
                        } else {
                            result = (result & ~ACTION_WAKE_UP) | ACTION_GO_TO_SLEEP;
                            Intent vPlayerPowerKey=new Intent("com.amlogic.vplayer.powerkey");
                            mContext.sendBroadcast(vPlayerPowerKey);
                            Log.i(TAG, "Suspend sys.proj.type: " + proj_type + ", sys.proj.tender.type: " + tender_type);
                            if ("telecom".equals(proj_type) && "jicai".equals(tender_type)) {
                                sendKeyEvent(KeyEvent.KEYCODE_HOME);
                            }else if("unicom".equals(proj_type)){
                                Log.i(TAG, "sys.proj.type:  unicom");
                                if(mSendHomeKeyTag){
                                    mHandler.removeCallbacks(mSendHomeKey);
                                    mSendHomeKeyTag = false;
                                 }
                                 mHandler.postDelayed(mSendHomeKey, 300);
                            }
                        }
                    } else {
                        if (mHdmiPlugged && !mPowerKeyHandled) {
                            sendKeyEvent(KeyEvent.KEYCODE_BACK);
                        }
                    }

                    mPendingPowerKeyUpCanceled = false;
		    mPowerKeyLongEventHandled = false;
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

            case KeyEvent.KEYCODE_EARLY_POWER: {
                if(SystemProperties.getBoolean("sys.cold-reboot.enable", false)){
                    SystemProperties.set("ubootenv.var.suspend", "on");
                    Log.d(TAG, "reboot system for cool reboot");
                    PowerManager pm = (PowerManager) mContext.getSystemService(mContext.POWER_SERVICE);
                    //pm.reboot("cool_reboot");
                    pm.reboot("lock_system");
                    
                }
                break;
            }
            case KeyEvent.KEYCODE_SETTINGS: {
			
					if(down)
					{
                            Intent intent = new Intent();
							//add by guangchao.su 2018.09.21 for A20_shandong settings
							String productName = SystemProperties.get("ro.ysten.province");
							Log.d("ysten_sgc","use this productName is "+productName);
							if(productName.equals("A20_shandong")){
								intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.MainActivity_new"));
								Log.d("ysten_sgc","start jiuzhou settings ");
							//add by guangchao.su 2018.09.21 for A20_shandong settings
							//begin added by hyz at 2019.05.07: add setting of jidi      							
							}else if (productName.contains("A20_jidi")){
								intent = new Intent("android.settings.chinamobile.SETTINGS");
								Log.d("hyz","start settings of jidi ");	
							//end added by hyz at 2019.05.07: add setting of jidi 
							//add by ysten.huanghongyan 2018.11.21 for CM201_henan settings       							
							}else if(productName.contains("CM201_henan")){
								intent.setComponent(new ComponentName("com.ysten.setting", "com.ysten.setting.MainActivity"));
								Log.d("ysten_hhy","start henan settings ");
							}else if(productName.equals("A20_anhui")){
								/*  add by ysten.mark for anhui A20c  */
								intent.setComponent(new ComponentName("com.ysten.setting", "com.ysten.setting.MainActivity"));
							//end by ysten.huanghongyan 2018.11.21 for CM201_henan settings 
							}else if (productName.contains("heilongjiang")){
								intent.setComponent(new ComponentName("com.ysten.setting", "com.ysten.setting.MainActivity"));
							}else if (productName.contains("fujian")){//add by guangchao.su 2019.02.20 for A20_fujian  begin
								intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings"));
							}
                                                        else if(productName.contains("guangdong")){
                                                               mContext.sendBroadcast(new Intent("android.intent.action.SETTINGS_KEY_PRESSED"));
                                                              intent.setComponent(new ComponentName("tv.icntv.vendor", "tv.icntv.vendor.main"));
                                                        }
							else if (productName.contains("neimeng")){//add by zhangy 2019.05.21 for A20_neimeng  begin
							//begin : add by zhuyu at 20190731 for  forbidden   setting Key when calling 
                                ActivityManager activityManager=(ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
                                String currentPackage=activityManager.getRunningTasks(1).get(0).topActivity.getClassName();
                                Log.d(TAG," zhuyu currentPackage = " + currentPackage);
                                if("com.ysten.hejiaguhua.CallActivity".equals(currentPackage)){
                                   return 0;
                                }
                                //end : add by zhuyu at 20190731 for  forbidden  setting Key when calling 
								intent.setComponent(new ComponentName("com.ysten.setting", "com.ysten.setting.MainActivity"));
							}
							else if (productName.contains("A20_sc")){//add by zhangy 2019.05.21 for A20_neimeng  begin
								intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.MainActivity"));
							}
							else if(productName.contains("CM201_IPTV_gansu")){//add by wuguoqing 2019 03.03 for IPTV-gs begin
								intent = new Intent("android.settings.chinamobile.SETTINGS");
								Log.d("ysten_gs","start shcmcc android.settings.chinamobile.SETTINGS ");
								//begin by ysten.xumiao,20190319,for CM201_IPTV_gansu GO HOME
								String topActivityPactage=getTopPackage(mContext);
								Log.d("xumiao","topActivityPactage " + topActivityPactage);
								if(topActivityPactage != null && topActivityPactage.equals("com.cmcc.loginauth")){
                                                                  ganSuLoginfalg=2;
								}else if(topActivityPactage != null && topActivityPactage.equals("com.chinamobile.middleware.startup")){
                                                                  ganSuLoginfalg=3;
								}
                                 Log.d("xumiao","ganSuLoginfalg " + ganSuLoginfalg);
								//end by ysten.xumiao,20190319,for CM201_IPTV_gansu GO HOME
                            //add by caishuo 2019.03.15 for CM502//motify by sunjh at 20191219 for CM502_newtv
							} else if (("true").equals(SystemProperties.get("persist.sys.iscm502", "false"))){
                                if (!SystemProperties.get("sys.hejiaguhua.incalling", "0").equals("0")) {
                                    Log.d("sjh", "setting_callingDialog start");
                                    mHandler.sendEmptyMessage(MSG_SETTING_OPEN_CALLING_DIALOG);
                                    Log.d("sjh", "setting_callingDialog end");
                                    return 0;
                                } 
                                mContext.sendBroadcast(new Intent("android.intent.action.SETTINGS_KEY_PRESSED"));
                                intent.setComponent(new ComponentName("com.ysten.setting", "com.ysten.setting.MainActivity"));
                            //end by caishuo 2019.03.15 for CM502//end by sunjh at 20191219 for CM502_newtv
                            //add by wuguoqing 2019 03.03 for IPTV-gs end
                            } else if(productName.contains("M302A_gw")||productName.contains("M301A_gw")){
								intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings"));
							}
							else{//add by guangchao.su 2019.02.20 for A20_fujian  end
                                intent.setComponent(new ComponentName("com.ysten.setting", "com.ysten.setting.MainActivity"));
							}
                            //intent.putExtra("show_lock_in_setting",true);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mContext.startActivity(intent);
                        
					}
					break;
            	}
				// begin by YSTen.lizheng,20181119 fujian four color key
		   case KeyEvent.KEYCODE_TV: 
           case KeyEvent.KEYCODE_MOVIE:
           case KeyEvent.KEYCODE_APPS:
           case KeyEvent.KEYCODE_FAVOURITE:
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
               if(SystemProperties.get("ro.ysten.province","master").contains("fujian")){
			   //begin by lizheng to solve four color key bug 20190306
				boolean launcherStarted=SystemProperties.getBoolean("persist.sys.launcher.started",true);
				if (!launcherStarted)
                {

				break;
                }
				//end by lizheng to solve four color key bug 20190306
				   //begin by ysten.lizheng,20181109,fix four color key bug
			       if(event.getAction() == KeyEvent.ACTION_UP) {
			   if (SystemProperties.getInt("persist.sys.launcher.value",0)==2){
				   doIPTVFuncationFJ(keyCode);
			   }else if(SystemProperties.getInt("persist.sys.launcher.value",0)==1){
				   String authStatus=getValueFromStb("authStatus")+"";
		           String user_token=getValueFromStb("user_token")+"";
		           Log.d(TAG,"authStatus="+authStatus+","+"user_token="+user_token);
		           if(authStatus.equals("AuthSuccess")&&!user_token.equals("")){
				   doFuncationFJ(keyCode);
				   }
				   }}
				   break;
				   // end by YSTen.lizheng,20181119 fujian four color key
               	}
		   // add by YSTen.zhangjunjian,20181119,for sise key

	   //add by zhaolianghua for hebei IPTVKEY @20181205
	   case KeyEvent.KEYCODE_PROG_RED:
	   case KeyEvent.KEYCODE_PROG_YELLOW:
	   {
		if(SystemProperties.get("ro.ysten.province","master").contains("hebei")){
			if(down) {
				doIPTVFuncationHE(keyCode);
			}
		}
		break;
	   }
	   //add by zhaolianghua end
           case KeyEvent.KEYCODE_RED:
           case KeyEvent.KEYCODE_GREEN:
           case KeyEvent.KEYCODE_YELLOW:
           case KeyEvent.KEYCODE_BLUE:
           case KeyEvent.KEYCODE_POUND:
	   {
		   if (down) {
			   if(SystemProperties.get("ro.ysten.province","master").equals("CM201_guangdong")){
				   Log.i("zjj", "zjj  guangdong"+keyCode);
				   doGDFunAction(keyCode);
			   }
			   //add by zhaolianghua for fourcolor key @20181213
			   /*if(SystemProperties.get("ro.ysten.province","master").equals("cm201_jiangxi")){
				   doJXFunAction(keyCode);
			   }*/
			   //add by zhaolianghua end
                // begin: add by ysten.tianchining at 20190326: add ningxia for fourcolor key
               if(SystemProperties.get("ro.ysten.province", "master").contains("ningxia")){
                    doNXFunction(keyCode);
                }
                // end: add by ysten.tianchining at 20190326: add ningxia for fourcolor key

			   //add by mark add heilongjiang fourcolor key
			   if (SystemProperties.get("ro.ysten.province", "master").contains("heilongjiang")){
				   if (SystemProperties.getInt("persist.sys.launcher.value", 1) == 1)
				   {
		               if(SystemProperties.get("sys.key.home").equals("off"))
					   {
				           doFunActionHLJ(keyCode);
				       }
				   }
			   }
			   //end add by mark add heilongjiang fourcolor key

		   }
		   break;
	   }
	   //end by ysten.zhangjunjian,20181119,for sise key

	case KeyEvent.KEYCODE_POWER: {
					// yst
					Log.d(TAG, "key KEYCODE_POWER isScreenOn--->>" + isScreenOn);
				   // mContext.startService(new Intent("com.ysten.action.poweroff"));
                  if(SystemProperties.get("ro.ysten.custom.type","none").equals("viper")
                        && !SystemProperties.get("ro.ysten.province","master").contains("jidi"))
                  {
                        Log.d(TAG, "custom.type:viper7");
                       if (down && isScreenOn) {
                        //Intent intent = new Intent("com.ysten.action.poweroff");
                        //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        //mContext.startActivity(intent);
                        mContext.startService(new Intent("com.ysten.action.poweroff"));
                       }
                  }
                  else
                  {
                result &= ~ACTION_PASS_TO_USER;				
                if (down) {
                    if("true".equalsIgnoreCase(SystemProperties.get("ro.platform.has.mbxuimode"))) {
                        Intent inte=new Intent("com.android.music.musicservicecommand.pause");
                        mContext.sendBroadcast(inte);
                        
                        OnAudioFocusChangeListener mAudioFocusListener = new OnAudioFocusChangeListener() {
                            public void onAudioFocusChange(int focusChange) {
                                Log.e(TAG, "+++****====: onAudioFocusChange");
                            }
                        };
                        AudioManager mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
                        mAudioManager.requestAudioFocus(mAudioFocusListener, AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
                        mAudioManager.abandonAudioFocus(mAudioFocusListener);

                        inte = new Intent("com.amlogic.hdmiin.pause");
                        mContext.sendBroadcast(inte);
                    }

                    mImmersiveModeConfirmation.onPowerKeyDown(isScreenOn, event.getDownTime(),
                            isImmersiveMode(mLastSystemUiFlags));
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
                    interceptPowerKeyDown(!isScreenOn || hungUp
                            || mVolumeDownKeyTriggered || mVolumeUpKeyTriggered);
                } else {
                    mPowerKeyTriggered = false;
                    cancelPendingScreenshotChordAction();
                    if (interceptPowerKeyUp(mHdmiPlugged || canceled || mPendingPowerKeyUpCanceled)) {
                        String proj_type = SystemProperties.get("sys.proj.type", "ott");
                        String tender_type = SystemProperties.get("sys.proj.tender.type", null);
                        Log.d(TAG, "press power key, proj_type is " + proj_type + ", tender_type is " + tender_type);
                        if (("mobile".equals(proj_type) && !suspendbycec)
                                || ("telecom".equals(proj_type) && "sichuan".equals(tender_type))) {
                            if(!mWindowManagerFuncs.isShutdownDialogShow() || !mWindowManagerFuncs.isShutdownProcessDialogShow()){
                                //begin:add by zhanghk at 20191023:power off directly when press power key
                                if(SystemProperties.get("ro.ysten.province","master").contains("zhejiang")){
                                    mPowerManager.goToSleep(SystemClock.uptimeMillis());
                                    return 0;
                                }
                                //end:add by zhanghk at 20191023:power off directly when press power key
                                Intent vPlayerPowerKey=new Intent("com.amlogic.vplayer.powerkey");
                                mContext.sendBroadcast(vPlayerPowerKey);
                                mHandler.post(mPowerShortPress);
                            } else {
                                Log.w(TAG, "Shutdown window is show, do not show again!" );
                            }
                        } else if ("mobile".equals(proj_type) && "sichuan".equals(tender_type)) {
                            result = (result & ~ACTION_WAKE_UP) | ACTION_GO_TO_SLEEP;
                            killIcntvApk();
                            Intent vPlayerPowerKey=new Intent("com.amlogic.vplayer.powerkey");
                            mContext.sendBroadcast(vPlayerPowerKey);
                        } else {
                            result = (result & ~ACTION_WAKE_UP) | ACTION_GO_TO_SLEEP;
                            Intent vPlayerPowerKey=new Intent("com.amlogic.vplayer.powerkey");
                            mContext.sendBroadcast(vPlayerPowerKey);
                            Log.i(TAG, "Suspend sys.proj.type: " + proj_type + ", sys.proj.tender.type: " + tender_type);
                            if ("telecom".equals(proj_type) && "jicai".equals(tender_type)) {
                                sendKeyEvent(KeyEvent.KEYCODE_HOME);
                            }
                        }
                    } else {
                        if (mHdmiPlugged && !mPowerKeyHandled) {
                            sendKeyEvent(KeyEvent.KEYCODE_BACK);
                        }
                    }

                    mPendingPowerKeyUpCanceled = false;
                }
                     
		  }			
		break;
	}
            case KeyEvent.KEYCODE_AVR_POWER: {
                if (!checkProcessForeground("com.android.smart.terminal.iptv")) {
                    result &= ~ACTION_PASS_TO_USER;
                    if (!down) {
                        if (null != mContext) {
                            Intent mintent = new Intent();
                            //begin by ysten zhuhengxuan at 20190315 IPTV的home键信息上报
                            //mintent.putExtra("intentMsg", "HOME");
			    if(SystemProperties.get("ro.ysten.province","master").equals("cm201_jiangxi")){
				mintent.putExtra("intentMsg", "EPGDomain");
			    }else{
                                mintent.putExtra("intentMsg", "HOME");
			    }
                            //end by ysten zhuhengxuan at 20190315 IPTV的home键信息上报
                            mintent.putExtra("intentMsg", "HOME");
                            mintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                            mintent.setAction("com.android.smart.terminal.iptv");
                            try {
                                mContext.startActivity(mintent);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                break;
            }
        }
        return result;
    }
    
//begin:add  by ysten zengzhiliang at 20190123 yunnan 4 color key proc
private void doFunAction_cm201_yunnan( int keyCode)
{
    switch (keyCode) {
            case KeyEvent.KEYCODE_RED:
                {
                    Intent intent = new Intent();
                    intent.setComponent(new ComponentName("com.gitv.tv.live", "com.gitv.tv.live.activity.VodActivity"));
    	            //intent.putExtra("show_lock_in_setting",true);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                     mContext.startActivity(intent);
                }
                break;
            case KeyEvent.KEYCODE_GREEN:
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
            case KeyEvent.KEYCODE_YELLOW:
                break;
            case KeyEvent.KEYCODE_BLUE:
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
//end:add  by ysten zengzhiliang at 20190123 yunnan 4 color key proc
	//begin by ysten lizheng 20181127 Iptv key
	private String getTopPackage(Context context){
		mActivityManager = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
        String packageName  = mActivityManager.getRunningTasks(1).get(0).topActivity.getPackageName();
		return packageName;
	}

	private void doIPTVFuncationHE(int keyCode){
		Log.i(TAG,"hebei doIPTVFuncationHE "+keyCode);
		String topPackage = getTopPackage(mContext);
		Log.d(TAG,"hebei topPackage = "+topPackage);
		if(!topPackage.equals("com.android.smart.terminal.iptv")) {
			Intent mIntent = new Intent();
			ComponentName compName= new ComponentName("com.android.smart.terminal.iptv","com.amt.app.IPTVActivity");
			switch (keyCode) {
				case KeyEvent.KEYCODE_HOME:
					mIntent.putExtra("intentMsg", "EPGDomain");
					break;
				case KeyEvent.KEYCODE_PROG_RED:
					mIntent.putExtra("intentMsg", "LIVE_CHANNEL_LIST");
					break;
				case KeyEvent.KEYCODE_PROG_YELLOW:
					mIntent.putExtra("intentMsg", "VOD_COTEGORY_PAGE");
					break;
			}
			mIntent.setComponent(compName);
			mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(mIntent);
			return;
		}
		int IPTVKey=0;
		switch (keyCode){
			case KeyEvent.KEYCODE_HOME:
				IPTVKey=181;
				break;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				IPTVKey=260;
				break;
			case KeyEvent.KEYCODE_VOLUME_UP:
				IPTVKey=259;
				break;
			case KeyEvent.KEYCODE_VOLUME_MUTE:
				IPTVKey=261;
				break;
			case KeyEvent.KEYCODE_MUTE:
				IPTVKey=261;
				break;
		}
		if(IPTVKey!=0){
			try{
				/*Log.d(TAG,"hebei IPTVkey = "+IPTVKey);
				java.lang.Process p = Runtime.getRuntime().exec("input keyevent "+ IPTVKey);*/
				long now = SystemClock.uptimeMillis();
				KeyEvent keyEvent = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, IPTVKey, 0);
				InputManager inputManager = (InputManager) mContext.getSystemService(Context.INPUT_SERVICE);
				inputManager.injectInputEvent(keyEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
			}catch(Exception e){
				Log.i(TAG, "doIPTVFuncationHE error");
			}
		}
	}
	
	private void doIPTVFuncationFJ(int keyCode){
		Log.i(TAG,"doIPTVFuncationFJ"+keyCode);
		String topPackage = getTopPackage(mContext);
		if(!topPackage.equals("com.android.smart.terminal.iptv")) {
			Intent mIntent = new Intent();
			ComponentName compName= new ComponentName("com.android.smart.terminal.iptv","com.amt.app.IPTVActivity");	
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
			mIntent.setComponent(compName);
			//add by guangchao.su 2019.02.20 for A20_fujian  begin
            mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			mContext.startActivityAsUser(mIntent, UserHandle.CURRENT);
			//add by guangchao.su 2019.02.20 for A20_fujian  end
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
        case KeyEvent.KEYCODE_VOLUME_MUTE:
                IPTVKey=KeyEvent.KEYCODE_MUTE;
				break;
		}
		Log.i(TAG,"IPTVKey is "+IPTVKey);
		if (IPTVKey!=0){
		//add by guangchao.su 2019.02.20 for A20_fujian  begin
		long now = SystemClock.uptimeMillis();
		KeyEvent keyEvent = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, IPTVKey, 0);
		InputManager inputManager = (InputManager) mContext.getSystemService(Context.INPUT_SERVICE);
		inputManager.injectInputEvent(keyEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
		//add by guangchao.su 2019.02.20 for A20_fujian  end
		}
	}
	//end by ysten lizheng 20181127 Iptv key
	//add by zhaolianghua for jiangxi fourcolor key @20181213
	private void doJXFunAction(int keyCode) {
                Log.d(TAG,"jiangxi----get keyevent keyCode:"+keyCode);
		int IPTVKey = 0;
		int OTTKey = 0;
		/*String keyEnable = Settings.System.getString(mContext.getContentResolver(), "funtionkey");
		String [] keyNumber = new String[6];
		if (!TextUtils.isEmpty(keyEnable)){
			for (int i=0; i<6; i++) {
				keyNumber[i]=keyEnable.substring(i,i+1);
			}
		}*/
		String launcherValue = SystemProperties.get("persist.sys.launcher.value","0");
		boolean needSendInfo = launcherValue.equals("2")
			&&!getTopPackage(mContext).equals("com.android.smart.terminal.iptv");
		Intent mIntent = new Intent();
		switch (keyCode) {
			case KeyEvent.KEYCODE_HOME:
				IPTVKey=181;
				mIntent.putExtra("intentMsg", "EPGDomain");
				OTTKey=3;
				break;
			case KeyEvent.KEYCODE_RED:
				IPTVKey=183;
				mIntent.putExtra("intentMsg", "LIVE_CHANNEL_LIST");
				/*if (keyNumber[0].equals("1"))
					OTTKey=170;*/
				break;
			case KeyEvent.KEYCODE_GREEN:
				IPTVKey=185;
				mIntent.putExtra("intentMsg", "VOD_COTEGORY_PAGE");
				/*if (keyNumber[0].equals("1"))
					OTTKey=10010;*/
				break;
			case KeyEvent.KEYCODE_YELLOW:
				IPTVKey=184;
                                mIntent.putExtra("intentMsg", "TVOD_CHANNEL_LIST");
				/*if (keyNumber[0].equals("1"))
					OTTKey=10011;*/
				break;
			case KeyEvent.KEYCODE_BLUE:
				IPTVKey=186;
                                mIntent.putExtra("intentMsg", "Infomation");
				/*if (keyNumber[0].equals("1"))
					OTTKey=10006;*/
				break;
		}
		if(launcherValue.equals("1")){
			if(OTTKey==3){
				startOTTHome();
				return;
			}
			/*if(OTTKey!=0){
				inputkeyevent(OTTKey);
			}*/
		}
		if(launcherValue.equals("2")){
			if(needSendInfo){
				ComponentName compName= new ComponentName("com.android.smart.terminal.iptv","com.amt.app.IPTVActivity");
				mIntent.setComponent(compName);
				mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				mContext.startActivity(mIntent);
				return;
			}
			if(IPTVKey!=0){
				inputkeyevent(IPTVKey);
                                Log.d(TAG,"jiangxi----inputkeyevent IPTV-keycode: "+IPTVKey);
			}
		}
	}
	private void inputkeyevent(int keycode){
		try{
			Runtime.getRuntime().exec("input keyevent "+ keycode);
		}catch(Exception e){
			Log.d(TAG,"here key error");
		}
	}
    // begin: add by ysten.tianchining at 20190326: add for fourcolor key
    private void doNXFunction(int keyCode){
        Log.d(TAG, "TCN_ADD: doNXFunction: four color key!!");
        int IPTVKey=0;
        switch (keyCode) {
            case KeyEvent.KEYCODE_RED:
                IPTVKey=183;
                break;
            case KeyEvent.KEYCODE_GREEN:
                IPTVKey=185;
                break;
            case KeyEvent.KEYCODE_YELLOW:
                IPTVKey=0;
                break;
            case KeyEvent.KEYCODE_BLUE:
                IPTVKey=186;
                break;
        }
        if (IPTVKey!=0){
            //long now = SystemClock.uptimeMillis();
            //KeyEvent keyEvent = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, IPTVKey, 0);
            //InputManager inputManager = (InputManager) mContext.getSystemService(Context.INPUT_SERVICE);
            //inputManager.injectInputEvent(keyEvent, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
            try{
                Runtime.getRuntime().exec("input keyevent "+ IPTVKey);
            }
            catch(Exception e){
                Log.i(TAG, "TCN_ADD: doIPTVFuncation orror");
            }
        }
    }
    //end: add by ysten.tianchining at 20190326: add for fourcolor key

    
	// add by ysten-mark for heilongjiang fourcolor key
	private void doFunActionHLJ(int keyCode) {
		ActivityManager am = (ActivityManager)mContext.getSystemService(Context.ACTIVITY_SERVICE);
		ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
		String packageName = cn.getPackageName();
		String className =cn.getClassName();
		Log.d("mark","top activity is "+packageName +"/"+className);
		
		Intent funIntent = new Intent();
		String funAction = null;
		String funActionUrl = null;

		switch (keyCode) {
			case KeyEvent.KEYCODE_RED:
				Log.d("mark", "TV");
				
				funActionUrl = "live";
				if(isIcntvLive){
				    return;
				   }else{
				     isIcntvLive=true;
				   }
			break;
			case KeyEvent.KEYCODE_GREEN:
				Log.d("mark", "MOVIE");
				funActionUrl = "vod";
			break;
			case KeyEvent.KEYCODE_YELLOW:
				Log.d("mark", "APPS");
				funActionUrl = "app";
			break;
			case KeyEvent.KEYCODE_BLUE:
				Log.d("mark", "collection");
				funActionUrl = "collection";
			break;
		}

		if (isActivityRunning(mContext, "tv.icntv.ott.Gefo")){
			funAction = "com.ysten.action.OpenGefo";
		}
		else{
			funAction = "com.ysten.action.OpenApp";
		}
		funIntent.setAction(funAction);
		funIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		funIntent.putExtra("actionUrl", funActionUrl);
		mContext.startActivity(funIntent);
	}
	// end:add by ysten-mark for heilongjiang fourcolor key

	//add by zhaolianghua for jiangxi fourcolor key
     //ysten.zhangjunjian,20181119,add for sishe key
     private void doGDFunAction(int keyCode) {
         Log.i(TAG,"doIPTVFuncation"+keyCode);
         int IPTVKey=0;
        switch (keyCode) {
            case KeyEvent.KEYCODE_RED:
                IPTVKey=183;
                break;
            case KeyEvent.KEYCODE_POUND:
                IPTVKey=183;
                break;
            case KeyEvent.KEYCODE_GREEN:
                IPTVKey=184;
                break;
            case KeyEvent.KEYCODE_YELLOW:
                IPTVKey=185;
                break;
            case KeyEvent.KEYCODE_BLUE:
                IPTVKey=186;
                break;
        }
          if (IPTVKey!=0){
                try{
                    Runtime.getRuntime().exec("input keyevent "+ IPTVKey);
                   }
                   catch(Exception e){
                       Log.i("zjj", "doIPTVFuncation orror");
                   }
               }
    }     
    //end by ysten.zhangjunjian,20181119,for guangdong key
    //begin by ysten.zhangjunjian,20181011,for hubei 
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
	
	public static String getRefershTokenTime(Context context)
	{
		String refershTokenTime = queryDBValue(context, "refresh_token_time");
		Log.i(TAG,"getRefershTokenTime:" + refershTokenTime);
		return refershTokenTime;
	}	
	
	public static String getBussinessPlatform(Context context)
    {
        String platform = queryDBValue(context, "bussiness_platform");
        Log.d(TAG, "getBussinessPlatform:" + platform);
	    return platform;
    }	

    public static String getTopActivity(Context context) {
	    try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List localList = manager.getRunningTasks(1);
            ActivityManager.RunningTaskInfo localRunningTaskInfo = (ActivityManager.RunningTaskInfo)localList.get(0);
            return localRunningTaskInfo.topActivity.getClassName();
        } catch (Exception e) {
            Log.d(TAG, "current app:" + e);
        }
        return "";
    }
	//begin by fenghao ,20181127,for hubei 
	public static String getSecondActivity(Context context) {
	    try {
              ActivityManager managers = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
              List localList = managers.getRunningTasks(2);
              ActivityManager.RunningTaskInfo localRunningTaskInfos = (ActivityManager.RunningTaskInfo)localList.get(1);
              return localRunningTaskInfos.topActivity.getClassName();
            } catch (Exception e) {
              Log.d(TAG, "current app:" + e);
            }
           return "";
       }
	//end by fenghao,20181127,for hubei 
    //end by ysten.zhangjunjian,20181011,for hubei 
	
    /**
     * Send a single key event.
     *
     * @param event is a string representing the keycode of the key event you
     * want to execute.
     */
    private void sendKeyEvent(int keyCode) {
        int eventCode = keyCode;
        long now = SystemClock.uptimeMillis();
        try {
            KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, eventCode, 0);
            KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, eventCode, 0);
            (IInputManager.Stub
                .asInterface(ServiceManager.getService("input")))
                .injectInputEvent(down, 0);
            (IInputManager.Stub
                .asInterface(ServiceManager.getService("input")))
                .injectInputEvent(up, 0);
        } catch (RemoteException e) {
            Log.i(TAG, "DeadOjbectException");
        }
    }

    /** When the screen is off we ignore some keys that might otherwise typically
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
            case KeyEvent.KEYCODE_MUTE:
                return mDockMode != Intent.EXTRA_DOCK_STATE_UNDOCKED;

            // ignore media and camera keys
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
            result |= ACTION_WAKE_UP;
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

    BroadcastReceiver mKeyDispatchReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            Log.v(TAG, "mKeyDispatchReceiver -- onReceive -- entry");

            synchronized (mKeyDispatchLock) {
                if (action.equals(QB_ENABLE)) {
                    Log.i(TAG, "Receive QB_ENABLE");
                    mKeyDispatcMode = KEY_DISPATCH_MODE_ALL_DISABLE;
                } else if (action.equals(QB_DISABLE)) {
                    Log.i(TAG, "Receive QB_DISABLE");
                    mKeyDispatcMode = KEY_DISPATCH_MODE_ALL_ENABLE;
                } else if(action.equals(QB_TURNON_SCREEN)) {
		    Log.i(TAG, "Receive QB_TURNON_SCREEN");
                    mPowerManager.wakeUp(SystemClock.uptimeMillis());
		} else if(action.equals(QB_TURNOFF_SCREEN)) {
                    Log.i(TAG, "Receive QB_TURNOFF_SCREEN");
                    mPowerManager.goToSleep(SystemClock.uptimeMillis());
                } else {
                    Log.i(TAG, "Receive Fake Intent");
                }
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
            boolean hasCvbsOutput = SystemProperties.getBoolean("ro.platform.has.cvbsmode", false);
            
            boolean isOpenDualDisplay = false;
            try{
                isOpenDualDisplay = Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.DUAL_DISPLAY) == 1 ? true : false;
            } catch (Settings.SettingNotFoundException se) {
                Log.d(TAG, "Error: "+se);
            }
            if(hasCvbsOutput){
                writeSysfs(VFM_CTRL_PATH, "rm default_ext");
                writeSysfs(VFM_CTRL_PATH, "add default_ext vdin0 amvideo2");
            }
            if (hasCvbsOutput && isOpenDualDisplay){
                Handler cvbsHandler = new Handler(); 
                cvbsHandler.postDelayed(new Runnable() { 
                    public void run() { 	            
                        String cvbsmode = SystemProperties.get("ubootenv.var.cvbsmode");
                        String outputMode = SystemProperties.get("ubootenv.var.outputmode");
                        writeSysfs(VIDEO2_CTRL_PATH, "1");
                        if(outputMode.equals("480i")||outputMode.equals("576i")){
                            writeSysfs("/sys/class/display2/mode","null");
                        }
                        else{
                            writeSysfs("/sys/class/display2/mode","null");
                            writeSysfs("/sys/class/display2/mode",cvbsmode);
                        }			  
                    } 
                }, 3000); 
            }
            boolean hasHdmiOnly = SystemProperties.getBoolean("ro.platform.hdmionly", false);
            if(hasHdmiOnly){
               String outputMode = SystemProperties.get("ubootenv.var.outputmode");
               if(!outputMode.contains("cvbs")){
                  writeSysfs("/sys/class/aml_mod/mod_off","vdac");
               }
            }	
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
            /*try {
                Thread.sleep(2000);
                writeSysfs(SYS_AVMUTE , "-1");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
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

    private void changeDisplaySize(int w, int h){
        try {
            if (w >= 0 && h >= 0) {
                if(!mDisplayChanged){
                    mDisplayChanged = true;

                    SystemProperties.set("ubootenv.var.disp.fromleft", "false");
                    //physical portrait device need first change surface to landscape
                    if(SystemProperties.getBoolean("ro.screen.portrait", false)){
                        SystemProperties.set("sys.portrait.orientation", "" + mLandscapeRotation);
                    }
                    
                    mWindowManager.setForcedDisplaySize(Display.DEFAULT_DISPLAY, w, h);
                    //don't save this value, when power down by no normal, this value will display error
                    Settings.Global.putString(mContext.getContentResolver(),
                        Settings.Global.DISPLAY_SIZE_FORCED, "");
                }
            } else {
                if(mDisplayChanged){
                    mDisplayChanged = false;

                    SystemProperties.set("ubootenv.var.disp.fromleft", "true");
                    if(SystemProperties.getBoolean("ro.screen.portrait", false)){
                        SystemProperties.set("sys.portrait.orientation", "");
                    }
                    
                    mWindowManager.clearForcedDisplaySize(Display.DEFAULT_DISPLAY);
                }
            }
        } catch (RemoteException e) {
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

            if(!SystemProperties.getBoolean("ubootenv.var.has.accelerometer", false) 
                || mHdmiPlugged){
                android.graphics.Point p = new android.graphics.Point();
                Display display = android.hardware.display.DisplayManagerGlobal.getInstance()
                    .getRealDisplay(Display.DEFAULT_DISPLAY);
                display.getRealSize(p);
                int shortSize, longSize, w, h;
                if (p.x > p.y) {
                    shortSize = p.y;
                    longSize = p.x;
                }
                else{
                    shortSize = p.x;
                    longSize = p.y;
                }
                w = shortSize*shortSize/longSize;
                h = shortSize;

                if(DEBUG)
                    Log.v(TAG, "orientation:" + orientation + " longSize:" + longSize + " shortSize:" + 
                        shortSize + " w:" + w + " h:" + h);
                
                if((ActivityInfo.SCREEN_ORIENTATION_PORTRAIT == orientation) || 
                    (ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT == orientation) ||
                    (ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT == orientation)){
                    changeDisplaySize(-1, -1);
                    return 0;//mPortraitRotation;
                }
                else{
                    changeDisplaySize(-1, -1);
                    return mLandscapeRotation;
                }
            }
            else{
                changeDisplaySize(-1, -1);
            }

            /*
            if( !SystemProperties.getBoolean("ubootenv.var.has.accelerometer", false) ){
                switch (orientation) {
                    case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                    case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                    case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                    case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                    case ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE:
                    case ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT:
					default:
                        return mLandscapeRotation;
                }
            }*/

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

	   if( false == SystemProperties.getBoolean("ubootenv.var.has.accelerometer", false) 
	       || mHdmiPlugged){
    		    return true;
    	  }
			  
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
                    mLockScreenTimeout=(mLockScreenTimeout<0)?Integer.MAX_VALUE:mLockScreenTimeout;
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
        awakenDreams();
		
        String packageName = SystemProperties.get("epg.launcher.packagename");
        if(!packageName.equals("")) {
		 Log.d(TAG, "start with extra");
		Intent mIntent = mContext.getPackageManager().getLaunchIntentForPackage(packageName); 
		mIntent.putExtra ("GoHome", "true");
		IActivityManager mAm = ActivityManagerNative.getDefault();
		String licence = SystemProperties.get("sys.start.licence", "all");
		try {
            //begin by ysten.xumiao,20190419,for CM201_IPTV_gansu GO HOME
			if(SystemProperties.get("ro.ysten.province").contains("CM201_IPTV_gansu")){
				Log.i(TAG,"wuguoqing CM201_IPTV_gansu GO HOME");
				Intent gsLauncherIntent = new Intent();
				ComponentName componentName =null;
				switch(ganSuLoginfalg){
                                      case 1:
                                             componentName = new ComponentName("com.pukka.gslauncher","com.pukka.gslauncher.main.WebEPGActivity");
                                             break;
                                      case 2:
                                             componentName = new ComponentName("com.cmcc.loginauth","com.cmcc.loginauth.activity.LoginActivity");
                                             break;
                                      case 3:
                                             componentName = new ComponentName("com.chinamobile.middleware.startup","com.shcmcc.chinamobile.activity.StartUpMainActivity");
                                             break;
                                      default:
                                             componentName = new ComponentName("com.pukka.gslauncher","com.pukka.gslauncher.main.WebEPGActivity");
                                             break;
                                 }
				//Log.d("xumiao","home ganSuLoginfalg " + ganSuLoginfalg);
                ganSuLoginfalg =1;				
                gsLauncherIntent.setComponent(componentName);
                gsLauncherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                //end by ysten.xumiao,20190419,for CM201_IPTV_gansu GO HOME
				try {
					Log.i(TAG,"CM201_IPTV_gansu START IPTV mIntent ");
					mContext.startActivity(gsLauncherIntent);
					}
					catch (ActivityNotFoundException e) {
						Slog.w(TAG, "No activity to handle assist action.", e);
						mContext.startActivityAsUser(mHomeIntent, UserHandle.CURRENT);
						}
		    }
			/*for special case */
			if((packageName.equals("com.voole.webepg") && is_topwindow("net.sunniwell.app.swsettings.chinamobile"))
				|| (mFocusedApp !=null && mFocusedApp.toString().contains("com.voole.epg.player.standard.VoolePlayActivity"))){
				mAm.forceStopPackage(packageName, 0);
				Thread.sleep(250);
			}
			if("washu".equals(licence))
				mAm.forceStopPackage("net.sunniwell.app.swplayer", 0);
			/*for special case end*/
			mContext.startActivityAsUser(mIntent, UserHandle.CURRENT);
		} catch (ActivityNotFoundException e) {
			Slog.w(TAG, "No activity to handle assist action.", e);
			mContext.startActivityAsUser(mHomeIntent, UserHandle.CURRENT);
		}/*for special case */
		catch (RemoteException e) {
			Slog.w(TAG, "ERROR to stop home before launch.", e);
			mContext.startActivityAsUser(mHomeIntent, UserHandle.CURRENT);
		}catch (InterruptedException e) {
			Slog.w(TAG, "ERROR to stop home before launch.", e);
			mContext.startActivityAsUser(mHomeIntent, UserHandle.CURRENT);
		}/*for special case end*/
	}else if(mbPackageExist){
		mContext.startActivityAsUser(mHomeIntent, UserHandle.CURRENT);
	}//begin by ysten.xumiao,20190303,for CM201_IPTV_gansu GO HOME
	else if(SystemProperties.get("ro.ysten.province").contains("CM201_IPTV_gansu")){
		Log.i(TAG,"CM201_IPTV_gansu GO HOME");
		Intent mIntent1 = mContext.getPackageManager().getLaunchIntentForPackage("com.pukka.gslauncher");
		Intent gsLauncherIntent = new Intent();
		//begin by ysten.xumiao,20190419,for CM201_IPTV_gansu GO HOME
		ComponentName componentName =null;
        switch(ganSuLoginfalg){
            case 1:
                 componentName = new ComponentName("com.pukka.gslauncher","com.pukka.gslauncher.main.WebEPGActivity");
                 break;
            case 2:
                 componentName = new ComponentName("com.cmcc.loginauth","com.cmcc.loginauth.activity.LoginActivity");
                 break;
            case 3:
                 componentName = new ComponentName("com.chinamobile.middleware.startup","com.shcmcc.chinamobile.activity.StartUpMainActivity");
                 break;
            default:
                 componentName = new ComponentName("com.pukka.gslauncher","com.pukka.gslauncher.main.WebEPGActivity");
                 break;
				}
         ganSuLoginfalg =1;
		 //end by ysten.xumiao,20190419,for CM201_IPTV_gansu GO HOME
		gsLauncherIntent.setComponent(componentName);
		gsLauncherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			Log.i(TAG,"CM201_IPTV_gansu START IPTV mIntent ");
			mContext.startActivity(gsLauncherIntent); 
		}catch (ActivityNotFoundException e) {
			Slog.w(TAG, "No activity to handle assist action.", e);
			mContext.startActivityAsUser(mHomeIntent, UserHandle.CURRENT);
			}
		}
		//begin by ysten.xumiao,20190303,for CM201_IPTV_gansu GO HOME
	    //begin by huxiang,20190526,for CM202_jike GO HOME
	else if(SystemProperties.get("ro.ysten.province").contains("CM202_jike")){
		Log.i(TAG,"CM202_jike GO HOME");
		//Intent mIntent2 = mContext.getPackageManager().getLaunchIntentForPackage(packageName);
		Intent jkLauncherIntent = new Intent();
		ComponentName componentName =null;

        componentName = new ComponentName("tv.icntv.ott","tv.icntv.ott.icntv");
		jkLauncherIntent.setComponent(componentName);
		jkLauncherIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			Log.i(TAG,"CM202_jike mIntent ");
			mContext.startActivity(jkLauncherIntent); 
		}catch (ActivityNotFoundException e) {
			Slog.w(TAG, "CM202_jike No activity to handle assist action.", e);
			mContext.startActivityAsUser(mHomeIntent, UserHandle.CURRENT);
			}
		}
		//end by huxiang,20190526,for CM202_jike GO HOME
	else {
		Log.d(TAG, "normal start home");
		Intent dock = createHomeDockIntent();
		if (dock != null) {
			try {
				mContext.startActivityAsUser(dock, UserHandle.CURRENT);
				return;
			} catch (ActivityNotFoundException e) {}
		}
		mContext.startActivityAsUser(mHomeIntent, UserHandle.CURRENT);
	}
    }

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

    private void broadcastMobileKeyEvent(KeyEvent keyEvent) {
        final boolean down = keyEvent.getAction() == KeyEvent.ACTION_DOWN;
        final int keyCode = keyEvent.getKeyCode();
        final int repeatCount = keyEvent.getRepeatCount();
        if("mobile".equals(SystemProperties.get("sys.proj.type", "false"))) {
            if(down && repeatCount == 0) {
                if (DEBUG_INPUT) {
                    Log.d(TAG, "[mobile] send keyevent broadcast=" + keyCode + " repeatCount:" +repeatCount);
                }

                if(null != mMobilekeyMapList)
                {
                    int i = 0;
                    for(i = 0; i < mMobilekeyMapList.size();i++)
                    {
                        if(keyCode == mMobilekeyMapList.get(i).andriodkeycode)
                        {
                    	    break;
                        }
                    }
	//add by ysten.huanghongyan 2018.11.27 for CM201_henan
					if(SystemProperties.get("ro.ysten.province","master").contains("CM201_henan")){
					String lirphycode ="0x00" + mMobilekeyMapList.get(i).physicskeycode + "DD22"; 
                    
					Intent phicmiguKeyEventIntent =new Intent("com.chinamobile.action.IR_TEST");
					Bundle bundle=new Bundle();
					bundle.putString("irphycode", lirphycode);
					Log.d(TAG, "com.chinamobile.action.IR_TEST  lirphycode = "+lirphycode);
					phicmiguKeyEventIntent.putExtras(bundle);
					mContext.sendBroadcast(phicmiguKeyEventIntent);	
					//end by ysten.huanghongyan 2018.11.27 for CM201_henan  
					}else{
						if( i < mMobilekeyMapList.size()) {
							String lirphycode ="0x00" +mMobilekeyMapList.get(i).physicskeycode + "DD22"; 
	                    
							Intent phicmiguKeyEventIntent =new Intent("com.chinamobile.action.IR_TEST");
							Bundle bundle=new Bundle();
							bundle.putString("irphycode", lirphycode);
							Log.d(TAG, "com.chinamobile.action.IR_TEST  lirphycode = "+lirphycode);
							phicmiguKeyEventIntent.putExtras(bundle);
							mContext.sendBroadcast(phicmiguKeyEventIntent);	
						}	
					}
                }

                
                Intent miguKeyEventIntent =new Intent("com.chinamobile.action.KEY_PRESS_DOWN");
                //for mobile apk test key del
                if(67 == keyCode)
                {
                    miguKeyEventIntent.putExtra("keyCode",112);
                }
                else  
                {
                    miguKeyEventIntent.putExtra("keyCode",keyCode);  
                }   
				
                mContext.sendBroadcast(miguKeyEventIntent);   

                
            }    
        }
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
 	//add by ysten.huanghongyan 2018.11.27 for CM201_henan
	void showSuspendDialog() {
        if(mShowPowerDialog)
           return ;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mShowPowerDialog = true;
                Slog.d(TAG, "chinamobile showSuspendDialog");
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
               builder.setTitle(com.android.internal.R.string.suspend_off);
                builder.setMessage(com.android.internal.R.string.suspend_progress);
                
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
                try{
                    Thread.sleep(5000);
                    }catch(Exception e){
                            Slog.d(TAG, "Show Suspend Dialog");
                          }
                            Slog.d(TAG, "Show Suspend Dialog");
                            mShowPowerDialog = false;
							mPowerManager.goToSleep(SystemClock.uptimeMillis());
                  }
            }).start();

    }
    //end by ysten.huanghongyan 2018.11.27 for CM201_henan 
    
    //begin: add by ysten chenfeng at 20180829:for factory tool
    public static void ledmanager(int flag)
    {
        String testcmd="";
        testcmd=testcmd+flag;
        NativeFetch.nativesetsystem(testcmd);
    }
    //begin: add by ysten chenfeng at 20180829: for factory tool
	//add by ysten.huanghongyan 2018.11.27 for CM201_henan
	private int mCount = 60;
    Timer mTimer;
    SleepDelayTimerTask mTimerTask = null;
    private static final int NOTIFY_COUNT_RETRY = 0x1001;
    private static final int NOTIFY_SLEEP = 0x1002;
    class SleepDelayTimerTask extends TimerTask{
        @Override
        public void run() {
            if( --mCount <= 0){
                mTimerTask.cancel();
                Message msg = sleepDelayHandler.obtainMessage(NOTIFY_SLEEP);
                msg.sendToTarget();
            }else{
                Slog.d(TAG, "mSleepDelayTextView 4 " + mSleepDelayTextView);
                Message msg = sleepDelayHandler.obtainMessage(NOTIFY_COUNT_RETRY);
                msg.sendToTarget();
                }
         	}
    }
	private Handler sleepDelayHandler = new Handler() {
            @Override 
            public void handleMessage(Message msg) {
                Slog.d(TAG, "msg.what--->>>" + msg.what);
             	synchronized(this){
             		switch (msg.what) {
         		
                        case NOTIFY_COUNT_RETRY:
                        {
                             Slog.d(TAG, "mCount--->>>" + mCount);
                             Slog.d(TAG, "mSleepDelayTextView 2 " + mSleepDelayTextView);
                             Slog.d(TAG, "mBuilder 2 " + mBuilder);
                             Slog.d(TAG, "mContext 2 " + mContext);
                             //mSleepDelayTextView.setText(mContext.getResources().getString(com.android.internal.R.string.action_suspend_hold)+"  "+mCount+"s");
                             break;
                        }
                        case NOTIFY_SLEEP:
                        {
                            //showDownload();
                            break;
                        }
                     
                      }   
                 } 
                 super.handleMessage(msg); 
             }
         };
    //end by ysten.huanghongyan 2018.11.27 for CM201_henan 
    
    /* launch the defualt launcher when the system boots for the first time */
    private boolean mFirstLaunch = false;
    private void setDefaultLauncher(int userId)
    {
        //add by zhanghk at 20181031 begin: get default component
        DevInfoManager manager = (DevInfoManager) mContext.getSystemService(DevInfoManager.DATA_SERVER);
        String packageName = manager.getValue(DevInfoManager.Launcher);
        //add by zhanghk at 20181031 end: get default component
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

    //add by sunjh at 20191212 for CM502:静麦场景
    private static Toast micToast = null;
    /*disableMicClsName 包含的是会自动切为静麦状态的className 在该类下不允许切换远场*/
    private static List<String> disableMicClsName = Arrays.asList("littlec.conference.talk.activity.CallVideoActivity",
                                                                    "com.cmcc.tvclient.firstbundle.FirstBundleActivity",
                                                                    "com.ysten.camerrecord.MainActivity",
                                                                    "com.istv.call.rlt.ui.call.ConverseActivity");

    public static boolean needDisableMicKey(Context context){
        String topActivity = getTopActivity(context);
        if(topActivity!=null&&(disableMicClsName.contains(topActivity))){
            return true;
        }
        return false;
    }

    private Runnable micRunnable = new Runnable(){
        public void run(){
           Toast.makeText(mContext, "当前场景不支持切换远场语音!", Toast.LENGTH_SHORT).show(); 
        }
    };
    //end by sunjh at 20191212 for CM502:静麦场景
    //add by sunjh at 20191121 for CM502:合家固话挂断提示
    private AlertDialog.Builder mCallingBuilder;
    AlertDialog mCallingDialog;
    private EditText CallingEt;
    private View mContentView;
    private CallingDialog callingDialog = null;
    private int time;

    void showCallingDialog(){
        CallingEt = new EditText(mContext);
        CallingEt.setTextSize(20); 
        CallingEt.setText("前往其他功能将结束通话");
        mCallingBuilder = new AlertDialog.Builder(mContext);
        mCallingBuilder.setTitle("是否结束通话？");
        mCallingBuilder.setView(CallingEt,110,50,60,50);
        mCallingBuilder.setPositiveButton(mContext.getResources().getString(com.android.internal.R.string.confirm_hn), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int which) {
                //if (!SystemProperties.get("sys.hejiaguhua.incalling", "0").equals("0")) {
                    Intent intent = new Intent("com.ysten.hejiaguhua.call");
                    intent.putExtra("action", "hangup");
                    mContext.sendBroadcast(intent);
                    String  packageName = SystemProperties.get("sys.deflauncher.pkg", "tv.newtv.tvlauncher");
                    String  className = SystemProperties.get("sys.deflauncher.cls", "tv.newtv.tvlauncher.MainActivity");
                    openapk(packageName, className);
                    Log.d("sjh", "pst");
                //}
            }
        });
        mCallingBuilder.setNegativeButton(mContext.getResources().getString(com.android.internal.R.string.cancel_hn), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int which) {
                mCallingDialog.dismiss();
                Log.d("sjh", "neg");
            }
        });
        mCallingBuilder.setCancelable(false);
        mCallingDialog = mCallingBuilder.create();
        mCallingDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        if (mCallingDialog != null && !mCallingDialog.isShowing()) {
            mCallingDialog.show();
        }
        CallingEt.setTextSize(20);
    }
    void showCallingDialog(String index) {
        final String keyCode = index;
        time = 10;
        mHandler.removeMessages(MSG_CALLING_DIALOG_DELAY_DISMISS);
        if (callingDialog != null) {
            Log.d("sjh", "before:callingDialog=" + callingDialog + ", callingDialog.isShowing()=" + callingDialog.isShowing());
            callingDialog.dismiss();
            callingDialog = null;
        }

        callingDialog = new CallingDialog(mContext, R.style.MyMiddleDialogStyle);
        callingDialog.setOnclickListener(new OnclickListener() {
            @Override
            public void confirm() {
            // TODO Auto-generated method stub
                //if (!SystemProperties.get("sys.hejiaguhua.incalling", "0").equals("0")) {
                    Log.d("sjh", "confirm");
                    Intent intent = new Intent();
                    intent.setAction("com.ysten.hejiaguhua.call");
                    intent.putExtra("action", "hangup");
                    mContext.sendBroadcast(intent);
                    String packageName = "";
                    String className = "";
                    if (keyCode!=null && (keyCode.contains("home"))) {
                        packageName = SystemProperties.get("sys.deflauncher.pkg", "tv.newtv.tvlauncher");
                        className = SystemProperties.get("sys.deflauncher.cls", "tv.newtv.tvlauncher.MainActivity");
                    } else if (keyCode!=null && (keyCode.contains("setting"))) {
                        packageName = "com.ysten.setting";
                        className = "com.ysten.setting.MainActivity";
                    }
                    Log.d("sjh", "openapk" + keyCode);
                    openapk(packageName, className);
                //}
            }

            @Override
            public void cancel() {
                // TODO Auto-generated method stub
                Log.d("sjh", "cancel");
            }
        });

        Log.d("sjh", "after:callingDialog=" + callingDialog + ", callingDialog.isShowing()=" + callingDialog.isShowing());
        callingDialog.show();
        mHandler.sendEmptyMessage(MSG_CALLING_DIALOG_DELAY_DISMISS);
    }
    //end by sunjh at 20191121 for CM502:合家固话挂断提示

    //add by zhanghk at 20181109 begin:add jiangsu auto poweroff tip	
	private AlertDialog.Builder mBuilder,mWifiBuilder;
	AlertDialog mDialog,mWifiDialog;
	private TextView mSleepDelayTextView;
	private EditText wifiEt;
	int shutHandlerDelay = 1000;
    boolean  isCancelDialog=false;
 	//add by ysten.huanghongyan 2019.01.01 for CM201-1 henan
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
	//end by ysten.huanghongyan 2019.01.01 for CM201-1 henan 
	
	void showSleepDelayDialog() {
 		//add by ysten.huanghongyan 2018.11.27 for CM201_henan
		if(SystemProperties.get("ro.ysten.province","master").contains("CM201_henan")){
			mCount = 60;
         //mHandler.post(new Runnable() {
         //    @Override
         //    public void run() {
            Slog.d(TAG, "chinamobile showSleepDelayDialog");
                 //if(mSleepDelayTextView == null) {
            mSleepDelayTextView = new TextView(mContext);
                 //}
            Slog.d(TAG, "mSleepDelayTextView 1 "  + mSleepDelayTextView);
            mSleepDelayTextView.setText(
			mContext.getResources().getString(com.android.internal.R.string.standby_warn,mCount));
            mSleepDelayTextView.setTextSize(20);
 	            //if(mBuilder == null) {
            mBuilder = new AlertDialog.Builder(mContext);
                 //}
            Slog.d(TAG, "mBuilder 1 "  + mBuilder);
            Slog.d(TAG, "mContext 1 " +  mContext);
                 //abandon  Title
 		        //mBuilder.setTitle(com.android.internal.R.string.action_suspend_title);
                 //builder.setMessage(com.android.internal.R.string.action_suspend_hold);
            int sleepDelay = Settings.System.getInt(mContext.getContentResolver(), SYSTEM_SLEEP_TIME_DELAY, -1);
            if(sleepDelay >= 60) {
                mBuilder.setTitle(mContext.getResources().getString(com.android.internal.R.string.standby_warn_title,sleepDelay/60));
            }else if(sleepDelay < 60 && sleepDelay > 0) {
                mBuilder.setTitle(mContext.getResources().getString(com.android.internal.R.string.standby_warn_title1,sleepDelay));
            }else {
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
            mSleepDelayTextView.setText(mContext.getResources().getString(com.android.internal.R.string.standby_warn,mCount));
            mSleepDelayTextView.setTextSize(20);
 		    Slog.d(TAG, "mSleepDelayTextView 3 " +  mSleepDelayTextView);
            mHandler.removeCallbacks(mSleepCancleTimer);
            mHandler.postDelayed(mSleepCancleTimer, 1000);
         	isCancelDialog=true; 
		}
        //end by ysten.huanghongyan 2018.11.27 for CM201_henan   
		else if(SystemProperties.get("ro.ysten.province","master").contains("ningxia")){
        //begin: add by tianchining at 20190327: showSleepDelayDialog for ningxia
            mSleepDelayTextView = new TextView(mContext);
            mSleepDelayTextView.setText(mContext.getResources().getString(com.android.internal.R.string.standby_warn_nx));
            mSleepDelayTextView.setTextSize(25);
            mBuilder = new AlertDialog.Builder(mContext);
            mBuilder.setView(mSleepDelayTextView,110,50,60,50);
            mBuilder.setCancelable(false);
            mDialog = mBuilder.create();
            mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
            mDialog.show(); 
            mHandler.removeCallbacks(powerOff);
            mHandler.postDelayed(powerOff, 5000);
            isCancelDialog=true;
        }
        //end: add by tianchining at 20190327: showSleepDelayDialog for ningxia
        else{
			mCount = 30;
			Slog.d(TAG, "chinamobile showSleepDelayDialog");
			mSleepDelayTextView = new TextView(mContext);
			Slog.d(TAG, "mSleepDelayTextView 1 "  + mSleepDelayTextView);
			/*add by zhaolianghua for jiangxi string @20190221*/
			if("cm201_jiangxi".equals(SystemProperties.get("ro.ysten.province"))){
				mSleepDelayTextView.setText(
						mContext.getResources().getString(com.android.internal.R.string.standby_warn_jx,mCount));
			}else{
				mSleepDelayTextView.setText(
						mContext.getResources().getString(com.android.internal.R.string.standby_warn,mCount));
			}
			/*add end by zhaolianghua*/
			mSleepDelayTextView.setTextSize(25);
			mBuilder = new AlertDialog.Builder(mContext);
			Slog.d(TAG, "mBuilder 1 " +  mBuilder);
			Slog.d(TAG, "mContext 1 "  + mContext);
			mBuilder.setView(mSleepDelayTextView,110,50,60,50);
			mBuilder.setCancelable(false);
			mDialog = mBuilder.create();
			mDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
			mDialog.show(); 
			//remove by zhaolianghua
			/*mSleepDelayTextView.setText(mContext.getResources().getString(com.android.internal.R.string.standby_warn,mCount));
			mSleepDelayTextView.setTextSize(25);*/
			Slog.d(TAG, "mSleepDelayTextView 3 "  + mSleepDelayTextView);
			mHandler.removeCallbacks(mSleepCancleTimer);
			mHandler.postDelayed(mSleepCancleTimer, 1000);
			isCancelDialog=true; 	
		}

	}

    //begin: add by tianchining at 20190327: add Runnable of powerOff
    private Runnable powerOff = new Runnable(){
       public void run(){
           //mDialog.dismiss(); //add by xuyunfeng at 20191122: ningxia iptv fake standby
           mPowerManager.goToSleep(SystemClock.uptimeMillis());
       }
    };
	// end: add by tianchining at 20190327: add Runnable of powerOff
    private Runnable mSleepCancleTimer = new Runnable()
    {
        public void run()
        {
            mCount --;
            if(mCount>=0) {
                mHandler.removeCallbacks(mSleepCancleTimer);
                mHandler.postDelayed(mSleepCancleTimer, 1000);
                Slog.d(TAG, "mSleepDelayTextView 5 " + mSleepDelayTextView);
		/*modify by zhaolianghua @20190221*/
		if("cm201_jiangxi".equals(SystemProperties.get("ro.ysten.province"))){
			mSleepDelayTextView.setText(mContext.getResources().getString(com.android.internal.R.string.standby_warn_jx,mCount));
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
	
    void showSuspendDialog_JS() {
        if(mShowPowerDialog)
            return ;
	jsHandler=new Handler();
        jsHandler.post(new Runnable() {
            @Override
            public void run() {			
                mShowPowerDialog = true;
                Slog.d(TAG, "chinamobile showSuspendDialog");
                AlertDialog.Builder builder = new AlertDialog.Builder(jsContext);
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

        final Runnable shutdown = new Runnable() {
            @Override
            public void run() {
		    try{
			    Thread.sleep(5000);
			    Slog.d(TAG, "Show Suspend Dialog");
			    mShowPowerDialog = false;
			    //auto poweroff
			    /*modify by zhaolianghua for jiangxi @20190221*/
			    if("cm201_jiangxi".equals(SystemProperties.get("ro.ysten.province"))
				||"cm201_guizhou".equals(SystemProperties.get("ro.ysten.province"))){
				    mPowerManager.goToSleep(SystemClock.uptimeMillis());
			    }else{
				    mWindowManagerFuncs.shutdown(true);
			    }
			    /*modify end*/
		    }catch (Exception e){
			    e.printStackTrace();
		    }
	    }
        };
		
        new Thread() {
			public void run() {
				new Handler(Looper.getMainLooper()).post(shutdown);
			}
        }.start();

    }
    //add by zhanghk at 20181109 end:add jiangsu auto poweroff tip
    //add by zhaolianghua for hebei @20181119
    public static boolean isActivityRunning(Context mContext, String activityClassName){
	    ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
	    List<RunningTaskInfo> info = activityManager.getRunningTasks(1);
	    if(info != null && info.size() > 0){
		    ComponentName component = info.get(0).topActivity;
		    if(component.getClassName().contains(activityClassName)){
			    return true;
		    }
	    }
	    return false;
    }
    //add by zhaolianghua end
    //begin: add by ysten xumiao at 20181218:hubei add KEYCODE
    private void doFunActionHB(int keyCode) {
       Log.i(TAG,"doFuncationHB"+keyCode);
       Intent funIntent = new Intent();
       String funAction = null;
       int funActionKey = 0;
       switch (keyCode) {
            case 136: 
                 funActionKey = 188; 
                 break;
            case 137:
                 funActionKey = 185; 
                 break;
            case 138:
                funActionKey = 0; // not used,because bestv not support
                break;
            case 139:
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
		Log.i(TAG,"doIPTVFuncation"+keyCode);
		int IPTVKey=0;
	
        switch (keyCode) {
            case 136: 
                IPTVKey = 1187; 
                break;
            case 137:
                IPTVKey = 1182; 
                break;
            case 138:
                IPTVKey = 1186; // not used,because bestv not support
                break;
            case 139:
                IPTVKey = 1188; 
                break;
        }
		Log.i(TAG,"IPTVKey111111111111111="+IPTVKey);
		if (IPTVKey!=0){
		    try{
                java.lang.Process p = Runtime.getRuntime().exec("input keyevent "+ IPTVKey);
		    } catch(Exception e){
                Log.i(TAG, "doIPTVFuncation orror");
		    }
		}
	}
    //end: add by ysten xumiao at 20181218:hubei add KEYCODE	
	private void goAhaschoolHome() {
        Intent homeIntent = new Intent();
        ComponentName componentName = new ComponentName("tv.familyschool.tob", "tv.havingclass.xinba.ui.splash.SplashActivity");
        homeIntent.setComponent(componentName);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(homeIntent);
    }
    private void goHomeschoolHome() {
        Intent homeIntent = new Intent();
        ComponentName componentName = new ComponentName("tv.homeschool.toc", "tv.havingclass.xinba.ui.splash.SplashActivity");
        homeIntent.setComponent(componentName);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(homeIntent);
    }
    private void doFunAction_school(int keyCode) {
        Intent homeIntent = new Intent();
        ComponentName componentName;
        if(keyCode==KeyEvent.KEYCODE_F6){
            componentName = new ComponentName("tv.homeschool.toc", "tv.havingclass.xinba.ui.search.SearchActivity");
        }else if(keyCode==KeyEvent.KEYCODE_F7){
            componentName = new ComponentName("tv.homeschool.toc", "tv.havingclass.xinba.ui.filter.FilterActivity");
        }else {
            return;
        }
        homeIntent.setComponent(componentName);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(homeIntent);
    }
        //begin: add by ysten zhuhengxuan at 20190410:jiangxi 江西广告apk下发定制logo
    Runnable changeLogoRunnable = new Runnable(){
    	public void run(){
	    changeLogoPath();
	}
    };
    private void changeLogoPath(){
	File file = new File("/data/local/configs.txt");
	if(file.exists()){
	    //SystemProperties.set("service.bootvideo", "1");
       	    BufferedReader reader = null;
       	    try{
		mhisys.rootSystem("chmod 777 /data/local/configs.txt");
               	Log.i(TAG, "----zhuhengxuan----/data/local/configs.txt exist");
               	reader=new BufferedReader(new FileReader(file));
               	String strline;
              	strline=reader.readLine();
               	String[] s = strline.split(",");
               	Log.i(TAG, "----zhuhengxuan----type:"+s[3]);
               	if(s[3].equals("bmp")||s[3].equals("png")||s[3].equals("jpeg")||s[3].equals("jpg")){
               	    String pictureName = s[1];
               	    Log.i(TAG, "----zhuhengxuan----pictureName:"+s[1]);
               	    String picturePath = "/data/local/"+s[1];
               	    Log.i(TAG, "----zhuhengxuan----picturePath:"+picturePath);
               	    File file_picture = new File(picturePath);
               	    if(file_picture.exists()){
			Log.i(TAG, "----zhuhengxuan----更新开机动画地址");
			mbootlogopicpath = picturePath;
			Message msg = Message.obtain();
			msg.what = MSG_UPDATE_BOOT_LOGO;
			mHandler.sendMessage(msg);
			strlineNew = s[0]+","+s[1]+",0,"+s[3];
			Log.i(TAG,"----zhuhengxuan---- need to write new firstline strlineNew"+strlineNew);
			//msg.what = MSG_CHANGE_CONFIG_FIIRSTLINE;
			//mHandler.sendMessage(msg);
			//replaceTxtFirstLineByStr("/data/local/configs.txt",strlineNew);
			Log.i(TAG,"----zhuhengxuan---- logo change handle sended path ["+mbootlogopicpath+"]");
               	    }else{
			Log.i(TAG, "----zhuhengxuan----picture did not exist");
	 	    }
               	}
       	    }catch(Exception e){
                Slog.w(TAG, "----zhuhengxuan----LoadlogoPath_JX():ERROR"+e);
       	    }finally{
                try{
                    reader.close();
               	}catch(IOException e){
               	    reader = null;
             	}
            }
	}else{
            //SystemProperties.set("service.bootvideo", "0");
	    Log.d(TAG,"----zhuhengxuan----migu configs.txt did not exist");
	}
    }

    private void freeMemory(String apkNameList[]){
	ActivityManager activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> info = activityManager.getRunningTasks(1);
        if(info != null && info.size() > 0){
            ComponentName component = info.get(0).topActivity;
	    Log.d("freeMemory", "top activity " + component.getClassName());
            String topPkg=component.getPackageName();
            int j = 0;
	    for(j=0;j<apkNameList.length;j++){
		String whiteList = apkNameList[j];
		if(topPkg.contains(whiteList)){
	            Log.d(TAG,"pkg in whitelist "+ topPkg);
                    break;
                }
	   }
           if(j == apkNameList.length) {
                Log.d(TAG,"pkg not in whitelist, kill "+ topPkg);
                activityManager.forceStopPackage(topPkg);
           }	
        }
    }

}
