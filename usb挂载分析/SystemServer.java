/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.server;

import android.app.ActivityManagerNative;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.AudioService;
import android.net.wifi.p2p.WifiP2pService;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.service.dreams.DreamService;
import android.util.DisplayMetrics;
import android.util.EventLog;
import android.util.Log;
import android.util.Slog;
import android.view.WindowManager;

import com.android.internal.R;
import com.android.internal.os.BinderInternal;
import com.android.internal.os.SamplingProfilerIntegration;
import com.android.server.accessibility.AccessibilityManagerService;
import com.android.server.accounts.AccountManagerService;
import com.android.server.am.ActivityManagerService;
import com.android.server.am.BatteryStatsService;
import com.android.server.content.ContentService;
import com.android.server.display.DisplayManagerService;
import com.android.server.dreams.DreamManagerService;
import com.android.server.input.InputManagerService;
import com.android.server.media.MediaRouterService;
import com.android.server.net.NetworkPolicyManagerService;
import com.android.server.net.NetworkStatsService;
import com.android.server.os.SchedulingPolicyService;
import com.android.server.pm.Installer;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.UserManagerService;
import com.android.server.power.PowerManagerService;
import com.android.server.power.ShutdownThread;
import com.android.server.print.PrintManagerService;
import com.android.server.search.SearchManagerService;
import com.android.server.usb.UsbService;
import com.android.server.wifi.WifiService;
import com.android.server.wm.WindowManagerService;
import com.cmcc.media.MicphoneService;
import com.cmcc.media.RTSoundEffectsService;

import dalvik.system.VMRuntime;
import dalvik.system.Zygote;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import com.droidlogic.instaboot.InstabootManagerService;

class ServerThread {
    private static final String TAG = "SystemServer";
    private static final String ENCRYPTING_STATE = "trigger_restart_min_framework";
    private static final String ENCRYPTED_STATE = "1";

    ContentResolver mContentResolver;
    private boolean isNetworkFinished = false;

    void reportWtf(String msg, Throwable e) {
        Slog.w(TAG, "***********************************************");
        Log.wtf(TAG, "BOOT FAILURE " + msg, e);
    }

    public void initAndLoop() {
        EventLog.writeEvent(EventLogTags.BOOT_PROGRESS_SYSTEM_RUN,
            SystemClock.uptimeMillis());

        Looper.prepareMainLooper();

        android.os.Process.setThreadPriority(
                android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        BinderInternal.disableBackgroundScheduling(true);
        android.os.Process.setCanSelfBackground(false);

        // Check whether we failed to shut down last time we tried.
        {
            final String shutdownAction = SystemProperties.get(
                    ShutdownThread.SHUTDOWN_ACTION_PROPERTY, "");
            if (shutdownAction != null && shutdownAction.length() > 0) {
                boolean reboot = (shutdownAction.charAt(0) == '1');

                final String reason;
                if (shutdownAction.length() > 1) {
                    reason = shutdownAction.substring(1, shutdownAction.length());
                } else {
                    reason = null;
                }

                ShutdownThread.rebootOrShutdown(reboot, reason);
            }
        }

        String factoryTestStr = SystemProperties.get("ro.factorytest");
        int factoryTest = "".equals(factoryTestStr) ? SystemServer.FACTORY_TEST_OFF
                : Integer.parseInt(factoryTestStr);
        final boolean headless = "1".equals(SystemProperties.get("ro.config.headless", "0"));

        Installer installer = null;
        AccountManagerService accountManager = null;
        ContentService contentService = null;
        DevInfoManagerService devInfoManager = null;
        LightsService lights = null;
        PowerManagerService power = null;
        DisplayManagerService display = null;
        DisplayService displaySetting = null;
        BatteryService battery = null;
        VibratorService vibrator = null;
        AlarmManagerService alarm = null;
        GpioManagerService gpio = null;
        MountService mountService = null;
        IPackageManager pm = null;
        Context context = null;
        WindowManagerService wm = null;
        BluetoothManagerService bluetooth = null;
        DockObserver dock = null;
        UsbService usb = null;
        SerialService serial = null;
        TwilightService twilight = null;
        UiModeManagerService uiMode = null;
        RecognitionManagerService recognition = null;
        NetworkTimeUpdateService networkTimeUpdater = null;
        CommonTimeManagementService commonTimeMgmtService = null;
        InputManagerService inputManager = null;
        TelephonyRegistry telephonyRegistry = null;
        OverlayViewService overlayview = null;
        ConsumerIrService consumerIr = null;
        InstabootManagerService mInstabootService = null;
        NetworkThread mNetThread = null;

        // Create a handler thread just for the window manager to enjoy.
        HandlerThread wmHandlerThread = new HandlerThread("WindowManager");
        wmHandlerThread.start();
        Handler wmHandler = new Handler(wmHandlerThread.getLooper());
        wmHandler.post(new Runnable() {
            @Override
            public void run() {
                //Looper.myLooper().setMessageLogging(new LogPrinter(
                //        android.util.Log.DEBUG, TAG, android.util.Log.LOG_ID_SYSTEM));
                android.os.Process.setThreadPriority(
                        android.os.Process.THREAD_PRIORITY_DISPLAY);
                android.os.Process.setCanSelfBackground(false);

                // For debug builds, log event loop stalls to dropbox for analysis.
                if (StrictMode.conditionallyEnableDebugLogging()) {
                    Slog.i(TAG, "Enabled StrictMode logging for WM Looper");
                }
            }
        });

        // bootstrap services
        boolean onlyCore = false;
        boolean firstBoot = false;
        try {
            // Wait for installd to finished starting up so that it has a chance to
            // create critical directories such as /data/user with the appropriate
            // permissions.  We need this to complete before we initialize other services.
            Slog.i(TAG, "Waiting for installd to be ready.");
            installer = new Installer();
            installer.ping();

            Slog.i(TAG, "Power Manager");
            power = new PowerManagerService();
            ServiceManager.addService(Context.POWER_SERVICE, power);

            Slog.i(TAG, "Activity Manager");
            context = ActivityManagerService.main(factoryTest);
        } catch (RuntimeException e) {
            Slog.e("System", "******************************************");
            Slog.e("System", "************ Failure starting bootstrap service", e);
        }

        boolean disableStorage = SystemProperties.getBoolean("config.disable_storage", false);
        boolean disableMedia = SystemProperties.getBoolean("config.disable_media", false);
        boolean disableBluetooth = SystemProperties.getBoolean("config.disable_bluetooth", false);
        boolean disableTelephony = SystemProperties.getBoolean("config.disable_telephony", false);
        boolean disableLocation = SystemProperties.getBoolean("config.disable_location", false);
        boolean disableSystemUI = SystemProperties.getBoolean("config.disable_systemui", false);
        boolean disableNonCoreServices = SystemProperties.getBoolean("config.disable_noncore", false);
        boolean disableNetwork = SystemProperties.getBoolean("config.disable_network", false);
        boolean enableQuickBoot = SystemProperties.getBoolean("config.enable_quickboot", false);
        boolean disableVibrator = SystemProperties.getBoolean("config.disable_vibrator", false);
        boolean disableInstaboot = SystemProperties.getBoolean("config.disable_instaboot", true) ||
            !(new File("/system/bin/instabootserver").exists());

        try {
            // other service may require system write, start it first!
            Slog.i(TAG, "System Write Manager");
            SystemWriteService systemWrite = new SystemWriteService(context);
            ServiceManager.addService(Context.SYSTEM_WRITE_SERVICE, systemWrite);

            Slog.i(TAG, "Display Manager");
            display = new DisplayManagerService(context, wmHandler);
            ServiceManager.addService(Context.DISPLAY_SERVICE, display, true);


            Slog.i(TAG, "Chinamobile IPTV Display Manager");
            displaySetting = new DisplayService(context);
            ServiceManager.addService(Context.DISPLAY_MANAGER_SERVICE, displaySetting);

            if (!disableInstaboot) {
                try {
                    Slog.i(TAG, "Instaboot Service");
                    mInstabootService = new InstabootManagerService(context);
                    ServiceManager.addService("instaboot", mInstabootService);
                } catch (Throwable e) {
                    reportWtf("starting InstabootService", e);
                }
            }


            if(!disableTelephony) {
                Slog.i(TAG, "Telephony Registry");
                telephonyRegistry = new TelephonyRegistry(context);
                ServiceManager.addService("telephony.registry", telephonyRegistry);
            }

            Slog.i(TAG, "Scheduling Policy");
            ServiceManager.addService("scheduling_policy", new SchedulingPolicyService());

            AttributeCache.init(context);

            if (!display.waitForDefaultDisplay()) {
                reportWtf("Timeout waiting for default display to be initialized.",
                        new Throwable());
            }

            Slog.i(TAG, "Package Manager");
            // Only run "core" apps if we're encrypting the device.
            String cryptState = SystemProperties.get("vold.decrypt");
            if (ENCRYPTING_STATE.equals(cryptState)) {
                Slog.w(TAG, "Detected encryption in progress - only parsing core apps");
                onlyCore = true;
            } else if (ENCRYPTED_STATE.equals(cryptState)) {
                Slog.w(TAG, "Device encrypted - only parsing core apps");
                onlyCore = true;
            }
            Slog.i(TAG, "Before PMS's main");

            pm = PackageManagerService.main(context, installer,
                    factoryTest != SystemServer.FACTORY_TEST_OFF,
                    onlyCore);
            Slog.i(TAG, "After AMS's main");

            try {
                firstBoot = pm.isFirstBoot();
            } catch (RemoteException e) {
            }
            Slog.i(TAG, "Before AMS's setSystemProcess");

            ActivityManagerService.setSystemProcess();

            Slog.i(TAG, "Entropy Mixer");
            ServiceManager.addService("entropy", new EntropyMixer(context));

            Slog.i(TAG, "User Service");
            ServiceManager.addService(Context.USER_SERVICE,
                    UserManagerService.getInstance());

            mContentResolver = context.getContentResolver();

            // The AccountManager must come before the ContentService
            try {
                // TODO: seems like this should be disable-able, but req'd by ContentService
                Slog.i(TAG, "Account Manager");
                accountManager = new AccountManagerService(context);
                ServiceManager.addService(Context.ACCOUNT_SERVICE, accountManager);
            } catch (Throwable e) {
                Slog.e(TAG, "Failure starting Account Manager", e);
            }

            Slog.i(TAG, "Content Manager");
            contentService = ContentService.main(context,
                    factoryTest == SystemServer.FACTORY_TEST_LOW_LEVEL);

            Slog.i(TAG, "System Content Providers");
            ActivityManagerService.installSystemProviders();

            Slog.i(TAG, "Lights Service");
            lights = new LightsService(context);

            Slog.i(TAG, "Battery Service");
            battery = new BatteryService(context, lights);
            ServiceManager.addService("battery", battery);

            if(!disableVibrator) {
                Slog.i(TAG, "Vibrator Service");
                vibrator = new VibratorService(context);
                ServiceManager.addService("vibrator", vibrator);
            }

            Slog.i(TAG, "Consumer IR Service");
            consumerIr = new ConsumerIrService(context);
            ServiceManager.addService(Context.CONSUMER_IR_SERVICE, consumerIr);

            // only initialize the power service after we have started the
            // lights service, content providers and the battery service.
            power.init(context, lights, ActivityManagerService.self(), battery,
                    BatteryStatsService.getService(),
                    ActivityManagerService.self().getAppOpsService(), display);

            Slog.i(TAG, "Alarm Manager");
            alarm = new AlarmManagerService(context);
            ServiceManager.addService(Context.ALARM_SERVICE, alarm);

            mNetThread = new NetworkThread(context, alarm, power);
            mNetThread.start();

            Slog.i(TAG, "Gpio Manager");
            gpio = new GpioManagerService(context);
            ServiceManager.addService(Context.GPIO_SERVICE, gpio);

            Slog.i(TAG, "OverlayView Service");
            overlayview = new OverlayViewService(context);
            ServiceManager.addService(Context.OVERLAYVIEW_SERVICE, overlayview);

            Slog.i(TAG, "CMCC migu karaok Micphone");
            MicphoneService micphone = new MicphoneService(context);
            ServiceManager.addService("Micphone", micphone);

            Slog.i(TAG, "CMCC migu karaok RTSoundEffects");
            RTSoundEffectsService RTSoundEffects = new RTSoundEffectsService(context);
            ServiceManager.addService("RTSoundEffects", RTSoundEffects);

	        Boolean hasMbxUI = SystemProperties.getBoolean("ro.platform.has.mbxuimode",false);
            if (hasMbxUI){
                Slog.i(TAG, "Mbox Outputmode Manager");
                MboxOutputModeService output = new MboxOutputModeService(context);
                ServiceManager.addService(Context.MBOX_OUTPUTMODE_SERVICE, output);
            }

            Boolean enableSysLog = SystemProperties.getBoolean("ro.platform.has.systemlog", false);
            if (enableSysLog) {
                Slog.i(TAG, "System Log Manager");
                SystemLogService log_service = new SystemLogService(context);
                ServiceManager.addService(Context.SYSTEM_LOG_SERVICE, log_service);
            }

            Boolean enableFTest = SystemProperties.getBoolean("ro.platform.has.factorytest", false);
            if (enableFTest) {
                Slog.i(TAG, "Factory Test Manager");
                FactoryTestService ftest_service = new FactoryTestService(context);
                ServiceManager.addService(Context.FACTORY_TEST_SERVICE, ftest_service);
            }

            if(!enableQuickBoot) {
                Slog.i(TAG, "System Key Services");
                SystemKeyServices keyServices =  new SystemKeyServices(context);
                keyServices.registerSystemKeyReceiver();
            }

			//add by zhanghk at 20181031 begin:jaingsu cp deviceinfo manager service
			try {
	            Slog.i(TAG, "DevInfo Manager Service");
	            devInfoManager = new DevInfoManagerService(context);
	            ServiceManager.addService(Context.DATA_SERVICE, devInfoManager);
	        } catch (Throwable e) {
	            reportWtf("starting DevInfoManagerService", e);
	        }
            //add by zhanghk at 20181031 end:jaingsu cp deviceinfo manager service

            Slog.i(TAG, "Init Watchdog");
            Watchdog.getInstance().init(context, battery, power, alarm,
                    ActivityManagerService.self());
            Watchdog.getInstance().addThread(wmHandler, "WindowManager thread");

            Slog.i(TAG, "Input Manager");
            inputManager = new InputManagerService(context, wmHandler);

            Slog.i(TAG, "Window Manager");
            wm = WindowManagerService.main(context, power, display, inputManager,
                    wmHandler, factoryTest != SystemServer.FACTORY_TEST_LOW_LEVEL,
                    !firstBoot, onlyCore);
            ServiceManager.addService(Context.WINDOW_SERVICE, wm);
            ServiceManager.addService(Context.INPUT_SERVICE, inputManager);

            ActivityManagerService.self().setWindowManager(wm);
            ActivityManagerService.self().setInstabootManager(mInstabootService);

            inputManager.setWindowManagerCallbacks(wm.getInputMonitor());
            inputManager.start();

            display.setWindowManager(wm);
            display.setInputManager(inputManager);

            // Skip Bluetooth if we have an emulator kernel
            // TODO: Use a more reliable check to see if this product should
            // support Bluetooth - see bug 988521
            if (SystemProperties.get("ro.kernel.qemu").equals("1")) {
                Slog.i(TAG, "No Bluetooh Service (emulator)");
            } else if (factoryTest == SystemServer.FACTORY_TEST_LOW_LEVEL) {
                Slog.i(TAG, "No Bluetooth Service (factory test)");
            } else if (!context.getPackageManager().hasSystemFeature
                       (PackageManager.FEATURE_BLUETOOTH)) {
                Slog.i(TAG, "No Bluetooth Service (Bluetooth Hardware Not Present)");
            } else if (disableBluetooth) {
                Slog.i(TAG, "Bluetooth Service disabled by config");
            } else {
                Slog.i(TAG, "Bluetooth Manager Service");
                bluetooth = new BluetoothManagerService(context);
                ServiceManager.addService(BluetoothAdapter.BLUETOOTH_MANAGER_SERVICE, bluetooth);
            }
        } catch (RuntimeException e) {
            Slog.e("System", "******************************************");
            Slog.e("System", "************ Failure starting core service", e);
        }


        DevicePolicyManagerService devicePolicy = null;
        StatusBarManagerService statusBar = null;
        InputMethodManagerService imm = null;
        AppWidgetService appWidget = null;
        NotificationManagerService notification = null;
        WallpaperManagerService wallpaper = null;
        LocationManagerService location = null;
        CountryDetectorService countryDetector = null;
        TextServicesManagerService tsms = null;
        LockSettingsService lockSettings = null;
        DreamManagerService dreamy = null;
        AssetAtlasService atlas = null;
        PrintManagerService printManager = null;
        MediaRouterService mediaRouter = null;

        // Bring up services needed for UI.
        if (factoryTest != SystemServer.FACTORY_TEST_LOW_LEVEL) {
            //if (!disableNonCoreServices) { // TODO: View depends on these; mock them?
            if (true) {
                try {
                    Slog.i(TAG, "Input Method Service");
                    imm = new InputMethodManagerService(context, wm);
                    ServiceManager.addService(Context.INPUT_METHOD_SERVICE, imm);
                } catch (Throwable e) {
                    reportWtf("starting Input Manager Service", e);
                }

                try {
                    Slog.i(TAG, "Accessibility Manager");
                    ServiceManager.addService(Context.ACCESSIBILITY_SERVICE,
                            new AccessibilityManagerService(context));
                } catch (Throwable e) {
                    reportWtf("starting Accessibility Manager", e);
                }
            }
        }

        try {
            wm.displayReady();
        } catch (Throwable e) {
            reportWtf("making display ready", e);
        }

        boolean tag = false;
        try {
            tag = pm.performBootDexOpt();
        } catch (Throwable e) {
            reportWtf("performing boot dexopt", e);
        }
        if (!"mobile".equals(SystemProperties.get("sys.proj.type", null))){
            try {
                if(tag) {
                    ActivityManagerNative.getDefault().showBootMessage(
                            context.getResources().getText(
                                    com.android.internal.R.string.android_upgrading_starting_apps),
                                    false);
                }
            } catch (RemoteException e) {
            }
        }

        if (factoryTest != SystemServer.FACTORY_TEST_LOW_LEVEL) {
            if (!disableStorage &&
                !"0".equals(SystemProperties.get("system_init.startmountservice"))) {
                try {
                    /*
                     * NotificationManagerService is dependant on MountService,
                     * (for media / usb notifications) so we must start MountService first.
                     */
                    Slog.i(TAG, "Mount Service");
                    mountService = new MountService(context);
                    ServiceManager.addService("mount", mountService);
                } catch (Throwable e) {
                    reportWtf("starting Mount Service", e);
                }
            }

            if (!disableNonCoreServices) {
                try {
                    Slog.i(TAG,  "LockSettingsService");
                    lockSettings = new LockSettingsService(context);
                    ServiceManager.addService("lock_settings", lockSettings);
                } catch (Throwable e) {
                    reportWtf("starting LockSettingsService service", e);
                }

                try {
                    Slog.i(TAG, "Device Policy");
                    devicePolicy = new DevicePolicyManagerService(context);
                    ServiceManager.addService(Context.DEVICE_POLICY_SERVICE, devicePolicy);
                } catch (Throwable e) {
                    reportWtf("starting DevicePolicyService", e);
                }
            }

            if (!disableSystemUI) {
                try {
                    Slog.i(TAG, "Status Bar");
                    statusBar = new StatusBarManagerService(context, wm);
                    ServiceManager.addService(Context.STATUS_BAR_SERVICE, statusBar);
                } catch (Throwable e) {
                    reportWtf("starting StatusBarManagerService", e);
                }
            }

            if (!disableNonCoreServices) {
                try {
                    Slog.i(TAG, "Clipboard Service");
                    ServiceManager.addService(Context.CLIPBOARD_SERVICE,
                            new ClipboardService(context));
                } catch (Throwable e) {
                    reportWtf("starting Clipboard Service", e);
                }
            }

            if (!disableNonCoreServices) {
                try {
                    Slog.i(TAG, "Text Service Manager Service");
                    tsms = new TextServicesManagerService(context);
                    ServiceManager.addService(Context.TEXT_SERVICES_MANAGER_SERVICE, tsms);
                } catch (Throwable e) {
                    reportWtf("starting Text Service Manager Service", e);
                }
            }

            //    mNetThread = new NetworkThread(context, alarm, power);
            //    mNetThread.start();

            if (!disableNonCoreServices) {
                try {
                    Slog.i(TAG, "UpdateLock Service");
                    ServiceManager.addService(Context.UPDATE_LOCK_SERVICE,
                            new UpdateLockService(context));
                } catch (Throwable e) {
                    reportWtf("starting UpdateLockService", e);
                }
            }

            /*
             * MountService has a few dependencies: Notification Manager and
             * AppWidget Provider. Make sure MountService is completely started
             * first before continuing.
             */
/*
            if (mountService != null && !onlyCore) {
                mountService.waitForAsecScan();
            }
*/
            try {
                if (accountManager != null)
                    accountManager.systemReady();
            } catch (Throwable e) {
                reportWtf("making Account Manager Service ready", e);
            }

            try {
                if (contentService != null)
                    contentService.systemReady();
            } catch (Throwable e) {
                reportWtf("making Content Service ready", e);
            }

            try {
                Slog.i(TAG, "Notification Manager");
                notification = new NotificationManagerService(context, statusBar, lights);
                ServiceManager.addService(Context.NOTIFICATION_SERVICE, notification);
                //networkPolicy.bindNotificationManager(notification);
            } catch (Throwable e) {
                reportWtf("starting Notification Manager", e);
            }

            try {
                Slog.i(TAG, "Device Storage Monitor");
                ServiceManager.addService(DeviceStorageMonitorService.SERVICE,
                        new DeviceStorageMonitorService(context));
            } catch (Throwable e) {
                reportWtf("starting DeviceStorageMonitor service", e);
            }

            if (!disableLocation) {
                try {
                    Slog.i(TAG, "Location Manager");
                    location = new LocationManagerService(context);
                    ServiceManager.addService(Context.LOCATION_SERVICE, location);
                } catch (Throwable e) {
                    reportWtf("starting Location Manager", e);
                }

                try {
                    Slog.i(TAG, "Country Detector");
                    countryDetector = new CountryDetectorService(context);
                    ServiceManager.addService(Context.COUNTRY_DETECTOR, countryDetector);
                } catch (Throwable e) {
                    reportWtf("starting Country Detector", e);
                }
            }

            if (!disableNonCoreServices) {
                try {
                    Slog.i(TAG, "Search Service");
                    ServiceManager.addService(Context.SEARCH_SERVICE,
                            new SearchManagerService(context));
                } catch (Throwable e) {
                    reportWtf("starting Search Service", e);
                }
            }

            try {
                Slog.i(TAG, "DropBox Service");
                ServiceManager.addService(Context.DROPBOX_SERVICE,
                        new DropBoxManagerService(context, new File("/data/system/dropbox")));
            } catch (Throwable e) {
                reportWtf("starting DropBoxManagerService", e);
            }

            if (!disableNonCoreServices && context.getResources().getBoolean(
                        R.bool.config_enableWallpaperService)) {
                try {
                    Slog.i(TAG, "Wallpaper Service");
                    if (!headless) {
                        wallpaper = new WallpaperManagerService(context);
                        ServiceManager.addService(Context.WALLPAPER_SERVICE, wallpaper);
                    }
                } catch (Throwable e) {
                    reportWtf("starting Wallpaper Service", e);
                }
            }

            if (!disableMedia && !"0".equals(SystemProperties.get("system_init.startaudioservice"))) {
                try {
                    Slog.i(TAG, "Audio Service");
                    ServiceManager.addService(Context.AUDIO_SERVICE, new AudioService(context));
                } catch (Throwable e) {
                    reportWtf("starting Audio Service", e);
                }
            }

            if (!disableNonCoreServices) {
                try {
                    Slog.i(TAG, "Dock Observer");
                    // Listen for dock station changes
                    dock = new DockObserver(context);
                } catch (Throwable e) {
                    reportWtf("starting DockObserver", e);
                }
            }

            if (!disableMedia) {
                try {
                    Slog.i(TAG, "Wired Accessory Manager");
                    // Listen for wired headset changes
                    inputManager.setWiredAccessoryCallbacks(
                            new WiredAccessoryManager(context, inputManager));
                } catch (Throwable e) {
                    reportWtf("starting WiredAccessoryManager", e);
                }
            }

            if (!disableNonCoreServices) {
                try {
                    Slog.i(TAG, "USB Service");
                    // Manage USB host and device support
                    usb = new UsbService(context);
                    ServiceManager.addService(Context.USB_SERVICE, usb);
                } catch (Throwable e) {
                    reportWtf("starting UsbService", e);
                }

                try {
                    Slog.i(TAG, "Serial Service");
                    // Serial port support
                    serial = new SerialService(context);
                    ServiceManager.addService(Context.SERIAL_SERVICE, serial);
                } catch (Throwable e) {
                    Slog.e(TAG, "Failure starting SerialService", e);
                }
            }

            try {
                Slog.i(TAG, "Twilight Service");
                twilight = new TwilightService(context);
            } catch (Throwable e) {
                reportWtf("starting TwilightService", e);
            }

            try {
                Slog.i(TAG, "UI Mode Manager Service");
                // Listen for UI mode changes
                uiMode = new UiModeManagerService(context, twilight);
            } catch (Throwable e) {
                reportWtf("starting UiModeManagerService", e);
            }

            if (!disableNonCoreServices) {
                try {
                    Slog.i(TAG, "Backup Service");
                    ServiceManager.addService(Context.BACKUP_SERVICE,
                            new BackupManagerService(context));
                } catch (Throwable e) {
                    Slog.e(TAG, "Failure starting Backup Service", e);
                }

                try {
                    Slog.i(TAG, "AppWidget Service");
                    appWidget = new AppWidgetService(context);
                    ServiceManager.addService(Context.APPWIDGET_SERVICE, appWidget);
                } catch (Throwable e) {
                    reportWtf("starting AppWidget Service", e);
                }

                try {
                    Slog.i(TAG, "Recognition Service");
                    recognition = new RecognitionManagerService(context);
                } catch (Throwable e) {
                    reportWtf("starting Recognition Service", e);
                }
            }

            try {
                Slog.i(TAG, "DiskStats Service");
                ServiceManager.addService("diskstats", new DiskStatsService(context));
            } catch (Throwable e) {
                reportWtf("starting DiskStats Service", e);
            }

            try {
                // need to add this service even if SamplingProfilerIntegration.isEnabled()
                // is false, because it is this service that detects system property change and
                // turns on SamplingProfilerIntegration. Plus, when sampling profiler doesn't work,
                // there is little overhead for running this service.
                Slog.i(TAG, "SamplingProfiler Service");
                ServiceManager.addService("samplingprofiler",
                            new SamplingProfilerService(context));
            } catch (Throwable e) {
                reportWtf("starting SamplingProfiler Service", e);
            }

            if (!disableNetwork) {
                try {
                    Slog.i(TAG, "NetworkTimeUpdateService");
                    networkTimeUpdater = new NetworkTimeUpdateService(context);
                } catch (Throwable e) {
                    reportWtf("starting NetworkTimeUpdate service", e);
                }
            }

            if (!disableMedia) {
                try {
                    Slog.i(TAG, "CommonTimeManagementService");
                    commonTimeMgmtService = new CommonTimeManagementService(context);
                    ServiceManager.addService("commontime_management", commonTimeMgmtService);
                } catch (Throwable e) {
                    reportWtf("starting CommonTimeManagementService service", e);
                }
            }

            if (!disableNetwork) {
                try {
                    Slog.i(TAG, "CertBlacklister");
                    CertBlacklister blacklister = new CertBlacklister(context);
                } catch (Throwable e) {
                    reportWtf("starting CertBlacklister", e);
                }
            }

            if (!disableNonCoreServices &&
                context.getResources().getBoolean(R.bool.config_dreamsSupported)) {
                try {
                    Slog.i(TAG, "Dreams Service");
                    // Dreams (interactive idle-time views, a/k/a screen savers)
                    dreamy = new DreamManagerService(context, wmHandler);
                    ServiceManager.addService(DreamService.DREAM_SERVICE, dreamy);
                } catch (Throwable e) {
                    reportWtf("starting DreamManagerService", e);
                }
            }

            if (!disableNonCoreServices && !enableQuickBoot) {
                try {
                    Slog.i(TAG, "Assets Atlas Service");
                    atlas = new AssetAtlasService(context);
                    ServiceManager.addService(AssetAtlasService.ASSET_ATLAS_SERVICE, atlas);
                } catch (Throwable e) {
                    reportWtf("starting AssetAtlasService", e);
                }
            }

            try {
                Slog.i(TAG, "IdleMaintenanceService");
                new IdleMaintenanceService(context, battery);
            } catch (Throwable e) {
                reportWtf("starting IdleMaintenanceService", e);
            }

            try {
                Slog.i(TAG, "Print Service");
                printManager = new PrintManagerService(context);
                ServiceManager.addService(Context.PRINT_SERVICE, printManager);
            } catch (Throwable e) {
                reportWtf("starting Print Service", e);
            }

            if (!disableNonCoreServices) {
                try {
                    Slog.i(TAG, "Media Router Service");
                    mediaRouter = new MediaRouterService(context);
                    ServiceManager.addService(Context.MEDIA_ROUTER_SERVICE, mediaRouter);
                } catch (Throwable e) {
                    reportWtf("starting MediaRouterService", e);
                }
            }
        }

        // Before things start rolling, be sure we have decided whether
        // we are in safe mode.
        final boolean safeMode = wm.detectSafeMode();
        if (safeMode) {
            ActivityManagerService.self().enterSafeMode();
            // Post the safe mode state in the Zygote class
            Zygote.systemInSafeMode = true;
            // Disable the JIT for the system_server process
            VMRuntime.getRuntime().disableJitCompilation();
        } else {
            // Enable the JIT for the system_server process
            VMRuntime.getRuntime().startJitCompilation();
        }

        // It is now time to start up the app processes...

        if(vibrator != null) {
            try {
                 vibrator.systemReady();
            } catch (Throwable e) {
                reportWtf("making Vibrator Service ready", e);
            }
        }

        if (lockSettings != null) {
            try {
                lockSettings.systemReady();
            } catch (Throwable e) {
                reportWtf("making Lock Settings Service ready", e);
            }
        }

        if (devicePolicy != null) {
            try {
                devicePolicy.systemReady();
            } catch (Throwable e) {
                reportWtf("making Device Policy Service ready", e);
            }
        }

        if (notification != null) {
            try {
                notification.systemReady();
            } catch (Throwable e) {
                reportWtf("making Notification Service ready", e);
            }
        }

        try {
            wm.systemReady();
        } catch (Throwable e) {
            reportWtf("making Window Manager Service ready", e);
        }

        if (safeMode) {
            ActivityManagerService.self().showSafeModeOverlay();
        }

        // Update the configuration for this context by hand, because we're going
        // to start using it before the config change done in wm.systemReady() will
        // propagate to it.
        Configuration config = wm.computeNewConfiguration();
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager w = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        w.getDefaultDisplay().getMetrics(metrics);
        context.getResources().updateConfiguration(config, metrics);

        try {
            power.systemReady(twilight, dreamy);
        } catch (Throwable e) {
            reportWtf("making Power Manager Service ready", e);
        }

        try {
            pm.systemReady();
        } catch (Throwable e) {
            reportWtf("making Package Manager Service ready", e);
        }

        try {
            display.systemReady(safeMode, onlyCore);
        } catch (Throwable e) {
            reportWtf("making Display Manager Service ready", e);
        }

        mNetThread.getNetworkPolicy().bindNotificationManager(notification);

        // These are needed to propagate to the runnable below.
        final Context contextF = context;
        final MountService mountServiceF = mountService;
        final BatteryService batteryF = battery;
        final NetworkManagementService networkManagementF = mNetThread.getNetworkManagement();
        final NetworkStatsService networkStatsF = mNetThread.getNetworkStats();
        final NetworkPolicyManagerService networkPolicyF = mNetThread.getNetworkPolicy();
        final DockObserver dockF = dock;
        final UsbService usbF = usb;
        final TwilightService twilightF = twilight;
        final UiModeManagerService uiModeF = uiMode;
        final AppWidgetService appWidgetF = appWidget;
        final WallpaperManagerService wallpaperF = wallpaper;
        final InputMethodManagerService immF = imm;
        final RecognitionManagerService recognitionF = recognition;
        final LocationManagerService locationF = location;
        final CountryDetectorService countryDetectorF = countryDetector;
        final NetworkTimeUpdateService networkTimeUpdaterF = networkTimeUpdater;
        final CommonTimeManagementService commonTimeMgmtServiceF = commonTimeMgmtService;
        final TextServicesManagerService textServiceManagerServiceF = tsms;
        final StatusBarManagerService statusBarF = statusBar;
        final DreamManagerService dreamyF = dreamy;
        final AssetAtlasService atlasF = atlas;
        final InputManagerService inputManagerF = inputManager;
        final TelephonyRegistry telephonyRegistryF = telephonyRegistry;
        final PrintManagerService printManagerF = printManager;
        final MediaRouterService mediaRouterF = mediaRouter;
        final WifiService wifiF = mNetThread.getWifi();
        final InstabootManagerService instabootServiceF = mInstabootService;

        /*if (mInstabootService != null){
            mInstabootService.setLaterServiceCallback(new Runnable() {
                public void run() {
                    Slog.i(TAG, "Later start services ready");
                    wifiF.checkAndStartWifiorAP();
                    try {
                        ActivityManagerService.self().startObservingNativeCrashes();
                    } catch (Throwable e) {
                        reportWtf("observing native crashes", e);
                    }
                    try {
                        if (connectivityF != null) connectivityF.systemReady();
                    } catch (Throwable e) {
                        reportWtf("making Connectivity Service ready", e);
                    }
                }
            });
        }*/

        // We now tell the activity manager it is okay to run third party
        // code.  It will call back into us once it has gotten to the state
        // where third party code can really run (but before it has actually
        // started launching the initial applications), for us to complete our
        // initialization.
        final NetworkThread fnetThread = mNetThread;
        ActivityManagerService.self().systemReady(new Runnable() {
            public void run() {
                Slog.i(TAG, "BootStage Making services ready");
                if ( instabootServiceF == null || !instabootServiceF.isEnable()) {
                    try {
                        ActivityManagerService.self().startObservingNativeCrashes();
                    } catch (Throwable e) {
                        reportWtf("observing native crashes", e);
                    }
                }
                Slog.i(TAG, "BootStage Making startObservingNativeCrashes ready");

                Thread th = new Thread("test") {
                    public void run() {
                        try {
                            Thread.sleep(3000);
                        } catch (Exception e) {}
                        if (!headless) {
                            startSystemUi(contextF);
                        }
                    }
                };
                th.start();

                try {
                    if (mountServiceF != null) mountServiceF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Mount Service ready", e);
                }
                Slog.i(TAG, "BootStage Making Mount Service ready");
				
                /*try {
                    if (batteryF != null) batteryF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Battery Service ready", e);
                }
                Slog.i(TAG, "BootStage Making Battery Service ready");*/
				
                try {
                    if (networkManagementF != null) networkManagementF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Network Managment Service ready", e);
                }
                Slog.i(TAG, "BootStage Making Network Managment Service ready");

                final ConnectivityService connectivityF = fnetThread.getConnectivity();
                try {
                    if (networkStatsF != null) networkStatsF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Network Stats Service ready", e);
                }
                Slog.i(TAG, "BootStage Making Network Stats Service ready");

                try {
                    if (networkPolicyF != null) networkPolicyF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Network Policy Service ready", e);
                }
                Slog.i(TAG, "BootStage Network Policy Service ready");
                if ( instabootServiceF == null || !instabootServiceF.isEnable()) {
                    try {
                        if (connectivityF != null) connectivityF.systemReady();
                    } catch (Throwable e) {
                        reportWtf("making Connectivity Service ready", e);
                    }
                }
                Slog.i(TAG, "BootStage Connectivity Service ready");
                try {
                    if (dockF != null) dockF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Dock Service ready", e);
                }
                try {
                    if (usbF != null) usbF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making USB Service ready", e);
                }
                Slog.i(TAG, "BootStage USB Service ready");

                try {
                    if (twilightF != null) twilightF.systemReady();
                } catch (Throwable e) {
                    reportWtf("makin Twilight Service ready", e);
                }
                Slog.i(TAG, "BootStage Twilight Service ready");

                try {
                    if (uiModeF != null) uiModeF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making UI Mode Service ready", e);
                }
                Slog.i(TAG, "BootStage UI Mode Service ready");

                try {
                    if (recognitionF != null) recognitionF.systemReady();
                } catch (Throwable e) {
                    reportWtf("making Recognition Service ready", e);
                }
                Slog.i(TAG, "BootStage Recognition Service ready");

                Watchdog.getInstance().start();
                Slog.i(TAG, "BootStage Watchdog start");

                // It is now okay to let the various system services start their
                // third party code...

                try {
                    if (appWidgetF != null) appWidgetF.systemRunning(safeMode);
                } catch (Throwable e) {
                    reportWtf("Notifying AppWidgetService running", e);
                }
                Slog.i(TAG, "BootStage AppWidgetService running");

                try {
                    if (wallpaperF != null) wallpaperF.systemRunning();
                } catch (Throwable e) {
                    reportWtf("Notifying WallpaperService running", e);
                }
                Slog.i(TAG, "BootStage Notifying WallpaperService running");

                try {
                    if (immF != null) immF.systemRunning(statusBarF);
                } catch (Throwable e) {
                    reportWtf("Notifying InputMethodService running", e);
                }
                Slog.i(TAG, "BootStage Notifying InputMethodService running");

                try {
                    if (locationF != null) locationF.systemRunning();
                } catch (Throwable e) {
                    reportWtf("Notifying Location Service running", e);
                }
                Slog.i(TAG, "BootStage Notifying Location Service running");

                try {
                    if (countryDetectorF != null) countryDetectorF.systemRunning();
                } catch (Throwable e) {
                    reportWtf("Notifying CountryDetectorService running", e);
                }
                Slog.i(TAG, "BootStage Notifying CountryDetectorService running");

                try {
                    if (networkTimeUpdaterF != null) networkTimeUpdaterF.systemRunning();
                } catch (Throwable e) {
                    reportWtf("Notifying NetworkTimeService running", e);
                }
                Slog.i(TAG, "BootStage Notifying NetworkTimeService running");

                try {
                    if (commonTimeMgmtServiceF != null) commonTimeMgmtServiceF.systemRunning();
                } catch (Throwable e) {
                    reportWtf("Notifying CommonTimeManagementService running", e);
                }
                Slog.i(TAG, "BootStage Notifying CommonTimeManagementService running");

                try {
                    if (textServiceManagerServiceF != null)
                        textServiceManagerServiceF.systemRunning();
                } catch (Throwable e) {
                    reportWtf("Notifying TextServicesManagerService running", e);
                }
                Slog.i(TAG, "BootStage Notifying TextServicesManagerService running");

                try {
                    if (dreamyF != null) dreamyF.systemRunning();
                } catch (Throwable e) {
                    reportWtf("Notifying DreamManagerService running", e);
                }
                Slog.i(TAG, "BootStage Notifying DreamManagerService running");

                try {
                    if (atlasF != null) atlasF.systemRunning();
                } catch (Throwable e) {
                    reportWtf("Notifying AssetAtlasService running", e);
                }
                Slog.i(TAG, "BootStage Notifying AssetAtlasService running");

                try {
                    // TODO(BT) Pass parameter to input manager
                    if (inputManagerF != null) inputManagerF.systemRunning();
                } catch (Throwable e) {
                    reportWtf("Notifying InputManagerService running", e);
                }
                Slog.i(TAG, "BootStage Notifying InputManagerService running");

                if (telephonyRegistryF != null) {
                    try {
                         telephonyRegistryF.systemRunning();
                    } catch (Throwable e) {
                        reportWtf("Notifying TelephonyRegistry running", e);
                    }
                }
                Slog.i(TAG, "BootStage Notifying TelephonyRegistry running");

                try {
                    if (printManagerF != null) printManagerF.systemRuning();
                } catch (Throwable e) {
                    reportWtf("Notifying PrintManagerService running", e);
                }
                Slog.i(TAG, "BootStage Notifying PrintManagerService running");

                try {
                    if (mediaRouterF != null) mediaRouterF.systemRunning();
                } catch (Throwable e) {
                    reportWtf("Notifying MediaRouterService running", e);
                }
                Slog.i(TAG, "BootStage Notifying MediaRouterService running");

                Slog.i(TAG, "BootStage all ams related services ready");

                android.os.Process.setThreadPriority(
                            android.os.Process.THREAD_PRIORITY_FOREGROUND);
            }
        });

        // For debug builds, log event loop stalls to dropbox for analysis.
        if (StrictMode.conditionallyEnableDebugLogging()) {
            Slog.i(TAG, "Enabled StrictMode for system server main thread.");
        }

        Looper.loop();
        Slog.d(TAG, "System ServerThread is exiting!");
    }

    static final void startSystemUi(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.android.systemui",
                    "com.android.systemui.SystemUIService"));
        //Slog.d(TAG, "Starting service: " + intent);
        context.startServiceAsUser(intent, UserHandle.OWNER);
    }
}
class NetworkThread extends Thread {
    private Context mcontext;
    private AlarmManagerService mAlarm;
    private PowerManagerService mPower;
    private static final String TAG = "SystemServer.Network";
    public static boolean finished;

    private NetworkManagementService networkManagement = null;
    private NetworkStatsService networkStats = null;
    private NetworkPolicyManagerService networkPolicy = null;
    private ConnectivityService connectivity = null;
    private WifiP2pService wifiP2p = null;
    private WifiService wifi = null;
    private NsdService serviceDiscovery= null;

    private Object mNmLock = new Object();
    private Object mNsLock = new Object();
    private Object mNpmLock = new Object();
    private Object mCsLock = new Object();
    private Object mWifiP2pLock = new Object();
    private Object mWifiLock = new Object();
    private Object mNsdiLock = new Object();

    public NetworkThread(Context context, AlarmManagerService alarm, PowerManagerService power) {
        super("ActivityManager");
        mcontext = context;
        mAlarm = alarm;
        mPower = power;
    }
    private void myWait(Object lock) {
        try {
            Slog.i(TAG, "wait..");
            lock.wait();
            Thread.sleep(4);
        } catch (Exception e) {
            Slog.i(TAG, "wait.."+e);
        }
    }

    private void myNotifyAll(Object lock) {
        try {
            lock.notifyAll();
        } catch (Exception e)
        {}
    }



    void reportWtf(String msg, Throwable e) {
        Slog.w(TAG, "***********************************************");
        Log.wtf(TAG, "BOOT FAILURE " + msg, e);
    }

    public NetworkManagementService getNetworkManagement() {
        synchronized(mNmLock) {
            while (networkManagement == null)
                myWait(mNmLock);

            Slog.i(TAG, "getNetworkManagement");
            return networkManagement;
        }
    }
    public NetworkPolicyManagerService getNetworkPolicy() {
        synchronized(mNpmLock) {
            while (networkPolicy == null)
               myWait(mNpmLock);
                Slog.i(TAG, "getNetworkPolicy");
            return networkPolicy;
        }
    }

    public NetworkStatsService getNetworkStats() {
        synchronized(mNsLock) {
            while (networkStats == null)
                myWait(mNsLock);

            Slog.i(TAG, "getNetworkStats");
            return networkStats;
        }
    }

    public WifiService getWifi() {
        synchronized(mWifiLock) {
            while (wifi == null)
                myWait(mWifiLock);

            Slog.i(TAG, "getWifi");
            return wifi;
        }
    }

    public ConnectivityService getConnectivity() {
        synchronized(mCsLock) {
            while (connectivity == null)
                myWait(mCsLock);

            Slog.i(TAG, "getConnectivity");
            return connectivity;
        }
    }


    @Override
    public void run() {
        boolean disableNetwork = SystemProperties.getBoolean("config.disable_network", false);
        android.os.Process.setThreadPriority(
                android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        Looper.prepare();

        synchronized(mNmLock) {
            if (!disableNetwork) {
                try {
                    Slog.i(TAG, "NetworkManagement Service");
                    networkManagement = NetworkManagementService.create(mcontext);
                    ServiceManager.addService(Context.NETWORKMANAGEMENT_SERVICE, networkManagement);
                    myNotifyAll(mNmLock);
                } catch (Throwable e) {
                    reportWtf("starting NetworkManagement Service", e);
                }
            }
        }


        if (!disableNetwork) {
            synchronized(mNsLock) {
                try {
                    Slog.i(TAG, "NetworkStats Service");
                    networkStats = new NetworkStatsService(mcontext, networkManagement, mAlarm);
                    ServiceManager.addService(Context.NETWORK_STATS_SERVICE, networkStats);
                    myNotifyAll(mNsLock);
                } catch (Throwable e) {
                    reportWtf("starting NetworkStats Service", e);
                }
            }


            synchronized(mNpmLock) {
                try {
                    Slog.i(TAG, "NetworkPolicy Service");
                    networkPolicy = new NetworkPolicyManagerService(
                            mcontext, ActivityManagerService.self(), mPower,
                            networkStats, networkManagement);
                    ServiceManager.addService(Context.NETWORK_POLICY_SERVICE, networkPolicy);
                    myNotifyAll(mNpmLock);
                } catch (Throwable e) {
                    reportWtf("starting NetworkPolicy Service", e);
                }
            }


           //synchronized(mWifiP2pLock) {  // no get service, no lock!
               try {
                    Slog.i(TAG, "Wi-Fi P2pService");
                    wifiP2p = new WifiP2pService(mcontext);
                    ServiceManager.addService(Context.WIFI_P2P_SERVICE, wifiP2p);
                     //myNotifyAll();
                } catch (Throwable e) {
                    reportWtf("starting Wi-Fi P2pService", e);
                }
            //}

           synchronized(mWifiLock) {
               try {
                    Slog.i(TAG, "Wi-Fi Service");
                    wifi = new WifiService(mcontext);
                    ServiceManager.addService(Context.WIFI_SERVICE, wifi);
                    myNotifyAll(mWifiLock);
                } catch (Throwable e) {
                    reportWtf("starting Wi-Fi Service", e);
                }
            }


            synchronized(mCsLock) {
                try {
                    Slog.i(TAG, "Connectivity Service");
                    connectivity = new ConnectivityService(
                            mcontext, networkManagement, networkStats, networkPolicy);
                    ServiceManager.addService(Context.CONNECTIVITY_SERVICE, connectivity);
                    networkStats.bindConnectivityManager(connectivity);
                    networkPolicy.bindConnectivityManager(connectivity);

                    wifiP2p.connectivityServiceReady();
                    wifi.checkAndStartWifiorAP();

                    myNotifyAll(mCsLock);
                } catch (Throwable e) {
                    reportWtf("starting Connectivity Service", e);
                }
            }


            //synchronized(mLock) { // no getService, no lock

            try {
                Slog.i(TAG, "Network Service Discovery Service");
                serviceDiscovery = NsdService.create(mcontext);
                ServiceManager.addService(
                        Context.NSD_SERVICE, serviceDiscovery);
                //myNotifyAll();
            } catch (Throwable e) {
                reportWtf("starting Service Discovery Service", e);
            }//}
            android.os.Process.setThreadPriority(
                        android.os.Process.THREAD_PRIORITY_FOREGROUND);

            Looper.loop();
        }
    }
}

public class SystemServer {
    private static final String TAG = "SystemServer";

    public static final int FACTORY_TEST_OFF = 0;
    public static final int FACTORY_TEST_LOW_LEVEL = 1;
    public static final int FACTORY_TEST_HIGH_LEVEL = 2;

    static Timer timer;
    static final long SNAPSHOT_INTERVAL = 60 * 60 * 1000; // 1hr

    // The earliest supported time.  We pick one day into 1970, to
    // give any timezone code room without going into negative time.
    private static final long EARLIEST_SUPPORTED_TIME = 86400 * 1000;

    /**
     * Called to initialize native system services.
     */
    private static native void nativeInit();

    public static void main(String[] args) {

        /*
         * In case the runtime switched since last boot (such as when
         * the old runtime was removed in an OTA), set the system
         * property so that it is in sync. We can't do this in
         * libnativehelper's JniInvocation::Init code where we already
         * had to fallback to a different runtime because it is
         * running as root and we need to be the system user to set
         * the property. http://b/11463182
         */
        SystemProperties.set("persist.sys.dalvik.vm.lib",
                             VMRuntime.getRuntime().vmLibrary());

        if (System.currentTimeMillis() < EARLIEST_SUPPORTED_TIME) {
            // If a device's clock is before 1970 (before 0), a lot of
            // APIs crash dealing with negative numbers, notably
            // java.io.File#setLastModified, so instead we fake it and
            // hope that time from cell towers or NTP fixes it
            // shortly.
            Slog.w(TAG, "System clock is before 1970; setting to 1970.");
            SystemClock.setCurrentTimeMillis(EARLIEST_SUPPORTED_TIME);
        }
		//begin: add by zhuyu at 20190724 for set default time :2019-07-15
		 if(SystemProperties.get("ro.ysten.province","master").equalsIgnoreCase("CM201_homeschool")){
			SystemClock.setCurrentTimeMillis(1563148800000L);
		}
		//end:add by zhuyu at 20190724 for set default time :2019-07-15

        if (SamplingProfilerIntegration.isEnabled()) {
            SamplingProfilerIntegration.start();
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    SamplingProfilerIntegration.writeSnapshot("system_server", null);
                }
            }, SNAPSHOT_INTERVAL, SNAPSHOT_INTERVAL);
        }

        // Mmmmmm... more memory!
        dalvik.system.VMRuntime.getRuntime().clearGrowthLimit();

        // The system server has to run all of the time, so it needs to be
        // as efficient as possible with its memory usage.
        VMRuntime.getRuntime().setTargetHeapUtilization(0.8f);

	Boolean hasMassStorage = SystemProperties.getBoolean("ro.has.mass.storage",false);
	if(hasMassStorage)
            Environment.setUserRequired(false);
	else
            Environment.setUserRequired(true);

        System.loadLibrary("android_servers");

    	if(SystemProperties.getBoolean("ro.app.optimization",false)){
    		System.loadLibrary("optimization");
    	}

        Slog.i(TAG, "Entered the Android system server!");

        // Initialize native services.
        if(!SystemProperties.getBoolean("config.disable_vibrator", false)) {
            nativeInit();
        }
        // This used to be its own separate thread, but now it is
        // just the loop we run on the main thread.
        ServerThread thr = new ServerThread();
        thr.initAndLoop();
    }
}
