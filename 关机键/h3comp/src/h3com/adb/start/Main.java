package h3com.adb.start;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.File;

import android.os.SystemProperties;
import android.net.NetworkInfo;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.content.Context;
import android.net.ethernet.EthernetManager;
import android.os.Message;
import android.os.Handler;
import com.hisilicon.android.hisysmanager.HiSysManager;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import android.os.SystemProperties;
import com.android.internal.policy.impl.PhoneWindowManager;

import java.io.FileWriter;

import android.os.PowerManager;
import android.app.DevInfoManager;
import android.os.SystemClock;
import android.app.ActivityManager;

import java.util.List;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Main extends BroadcastReceiver {
    // static NetworkInfo  now;
    private EthernetManager mEthManager;
    private PowerManager mPowerManager;
    public Context mcontext;
    public boolean DEBUG = true;
    public String TAG = "xue";
    public String Paramac = "";
    private static String WMAC_BURNING_CMD = "iwpriv wlan0 efuse_set wmap,d7,%s";
    private static String WMAC_BURNING_CMD_MT7601 = "iwpriv wlan0 e2p %d=%s";
    private static String WMAC_BURNING_CMD_M8723BU = "iwpriv wlan0 efuse_set mac,%s";

    //add by guangchao.su for the sdio8822bs
    private static String WMAC_BURNING_CMD_SDIO8822BS_START = "rtwpriv wlan0 mp_start";
    private static String WMAC_BURNING_CMD_SDIO8822BS_SET = "rtwpriv wlan0 efuse_set mac,%s";
    private static String WMAC_BURNING_CMD_SDIO8822BS_GET = "rtwpriv wlan0 efuse_get mac";

    private TopActivityInfo test1, test1pre;

    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            HiSysManager hisys = new HiSysManager();
            Intent intent;
            DevInfoManager devInfo;
            String wifimodulestr;
            String s9083str;
            String newHisupdate;
            switch (msg.what) {
                case 1:
                    hisys = new HiSysManager();
                    newHisupdate = SystemProperties.get("persist.sys.newhisupdate", "0");
                    if (newHisupdate.contains("1")) {
                        Log.d("xue", "enter when persist.sys.newhisupdate is 1 and enter cache update");
                        hisys.upgrade("/cache");
                        intent = new Intent("android.intent.action.MASTER_CLEAR");
                        intent.putExtra("mount_point", "/cache");

                    } else {
                        Log.d("xue", "enter when persist.sys.newhisupdate is 0 and enter sdcard update");
                        hisys.upgrade("/storage/emulated/0");
                        intent = new Intent("android.intent.action.MASTER_CLEAR");
                        intent.putExtra("mount_point", "/storage/emulated/0");
                    }
                    mcontext.sendBroadcast(intent);
                    break;
                case 2:
                    //button.setText(R.string.text2);
                /*   hisys = new HiSysManager();
            SystemProperties.set("sys.stb.silentupdate.canceled", "true");
            
			newHisupdate=SystemProperties.get("persist.sys.newhisupdate","0");
			  if(newHisupdate.contains("1"))
			  {
                          Log.d("xue", "enter when persist.sys.newhisupdate is 1 and enter cache update silent");
			         hisys.upgrade("/cache");
					 intent = new Intent("android.intent.action.MASTER_CLEAR");
		    intent.putExtra("mount_point", "/cache");
					 
			  }
			  else
			  {
                        Log.d("xue", "enter when persist.sys.newhisupdate is 0 and enter cache update silent");
                  hisys.upgrade("/storage/emulated/0");
            intent = new Intent("android.intent.action.MASTER_CLEAR");
            intent.putExtra("mount_point", "/storage/emulated/0");
			}
            mcontext.sendBroadcast(intent);*/
                    hisys = new HiSysManager();
                    hisys.upgrade("/cache");
                    installROM("/cache/update.zip", true);
                    break;
                case 4:
                    devInfo = (DevInfoManager) mcontext.getSystemService(Context.DATA_SERVICE);
                    devInfo.releaseMic();
                    break;
                case 5:
                    test1 = getTopActivityInfo(mcontext);
                    if (test1.packageName.equals("com.cmcc.tvclient") || test1.packageName.equals("com.cmcc.hemuyi.stb")) {
                    } else {
                        devInfo = (DevInfoManager) mcontext.getSystemService(Context.DATA_SERVICE);
                        devInfo.resetMic();
                    }
                    break;
                case 15:
                    PhoneWindowManager.ledpmanager(52);//killapp RemoteService
                    break;
                case 6:
                    poweroff();
                    break;
                case 7:
                    hdmipoweroff();
                    break;
                case 10:

                    if (0 == hisys.setFlashInfo("deviceinfo", 0, 17, Paramac)) {
                        Log.d("xue", "write mac ok");
                    } else {
                        Log.d("xue", "write mac fail");
                    }

                    break;
                case 11:
                    wifimodulestr = SystemProperties.get("persist.sys.wifimodule", "");
                    try {
                        PhoneWindowManager.ledpmanager(50);//make sure wifi if on
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (wifimodulestr.contains("m7601")) {
                        if (wmacAddrBurningMT7601(Paramac) == true)
                            Log.d("xue", "write wmac 7601 ok");
                        else
                            Log.d("xue", "write wmac 7601 fail");
                    } else if (wifimodulestr.contains("sdio8822bs")) {//add by guangchao.su for the sdio8822bs
                        System.out.println("*** wifimodel sdio8822bs");
                        Log.d("sgc", "use this *** wifimodel sdio8822bs");
                        if (wmacAddrBurningSDIO8822BS(Paramac) == true)
                            Log.d("xue", "write wmac sdio8822bs ok");
                        else
                            Log.d("xue", "write wmac sdio8822bs fail");
                    } else if (wifimodulestr.contains("m8822bu")) {
                        System.out.println("*** wifimodel 8822bu");
                        if (wmacAddrBurningRTW8723nocheck(Paramac) == true)
                            Log.d("xue", "write wmac 8822bu ok");
                        else
                            Log.d("xue", "write wmac 8822bu fail");
                    } else if (wifimodulestr.contains("m8723") || wifimodulestr.contains("s9083")) {
                        if (wmacAddrBurningRTW8723(Paramac) == true)
                            Log.d("xue", "write wmac 8723 ok");
                        else
                            Log.d("xue", "write wmac 8723 fail");
                        try {
                            s9083str = s9083macRead();
                            Log.d("xue", "write wmac 9083 read" + s9083macRead());
                            if (s9083str != Paramac) {
                                if (wmacAddrBurningRTW8723(Paramac) == true)
                                    Log.d("xue", "write wmac 8723 ok again");
                                else
                                    Log.d("xue", "write wmac 8723 fail again");

                            }
                        } catch (IOException e) {
                            Log.d("xue", "write wmac 8723 fail again2");
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            Log.d("xue", "write wmac 8723 fail again2");
                            e.printStackTrace();
                        }

                    } else {
                        // if(wmacAddrBurningRTW(Paramac)==true)
                        //   Log.d("xue", "write wmac ok");
                        // else
                        Log.d("xue", "write wmac fail no module");

                    }
                    break;

                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("xue", "broadcast action:  " + intent.getAction() + "," + intent.getExtras());
        mcontext = context;
        if (intent.getAction().equals("android.ysten.systemupdate")) {

            Message message = new Message();
            try {
                if (intent.getExtras().get("recovermode") != null) {
                    if (intent.getExtras().get("recovermode").equals("immediately"))
                        message.what = 1;
                    else if (intent.getExtras().get("recovermode").equals("silent"))
                        message.what = 2;
                } else if (intent.getExtras().get("miccontrol") != null) {
                    if (intent.getExtras().get("miccontrol").equals("releasemic"))
                        message.what = 4;
                    else if (intent.getExtras().get("miccontrol").equals("resetmic"))
                        message.what = 5;
                    else if (intent.getExtras().get("miccontrol").equals("killapp"))
                        message.what = 15;

                } else if (intent.getExtras().get("powercontrol") != null) {
                    if (intent.getExtras().get("powercontrol").equals("poweroff"))
                        message.what = 6;

                } else if (intent.getExtras().get("hdmipowercontrol") != null) {
                    if (intent.getExtras().get("hdmipowercontrol").equals("hdmipoweroff")) {
                        message.what = 7;
                    }
                } //add by sunjh for HDC-32A at 20200305:setprop persist.com.ott.login
                else if (intent.getExtras().get("setprop") != null) {
                    Log.d("sjh_login", "Extras:" + intent.getExtras().get("setprop"));
                    String value = intent.getExtras().get("setprop").toString();
                    if (value.startsWith("persist.com.ott.login")) {
                        String[] login = value.split("=");
                        if (login.length == 2 && login[1] != null) {
                            for (int i = 0; i < login.length; i++) {
                                Log.d("sjh_login", login[i]);
                            }
                            SystemProperties.set(login[0], login[1]);
                        }
                    }
                }
                //end by sunjh for HDC-32A at 20200305:setprop persist.com.ott.login
                else {
                    Log.d("xue", "intent.getExtras().get(mac):  " + intent.getExtras().get("mac"));
                    if (intent.getExtras().get("mac") != null) {
                        Paramac = intent.getExtras().get("mac").toString();
                        message.what = 10;
                    } else if (intent.getExtras().get("wmac") != null) {
                        Paramac = intent.getExtras().get("wmac").toString();
                        message.what = 11;
                    }

                }
                mHandler.sendMessage(message);
            } catch (Exception e) {
                Log.d("xue", "broadcast action:  " + intent.getAction() + "error");
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                Log.e("xue", " timer Exception is " + sw.toString());
            }
        }


    }

    public class TopActivityInfo {
        public String packageName = "";
        public String topActivityName = "";
    }

    private TopActivityInfo getTopActivityInfo(Context context) {
        ActivityManager manager = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE));
        TopActivityInfo info = new TopActivityInfo();

        {
            List localList = manager.getRunningTasks(1);
            ActivityManager.RunningTaskInfo localRunningTaskInfo = (ActivityManager.RunningTaskInfo) localList.get(0);
            info.packageName = localRunningTaskInfo.topActivity.getPackageName();
            info.topActivityName = localRunningTaskInfo.topActivity.getClassName();
        }
        return info;
    }

    public void hdmipoweroff() {
        HiSysManager hisys = new HiSysManager();
        if ("cm201_hebei".equals(SystemProperties.get("ro.ysten.province"))
                || "cm201_zhejiang".equals(SystemProperties.get("ro.ysten.province"))
                || "cm201_shaanxi".equals(SystemProperties.get("ro.ysten.province"))
                || "cm201_neimeng".equals(SystemProperties.get("ro.ysten.province"))
                || "CM201-2_shanxi".equals(SystemProperties.get("ro.ysten.province"))
                || "cm201_xinjiang".equals(SystemProperties.get("ro.ysten.province"))
                || "cm201_anhui_iptv".equals(SystemProperties.get("ro.ysten.province"))//add by zhangy for anhui iptv HDMI @20191206
                || "cm201_henan".equals(SystemProperties.get("ro.ysten.province"))//add by zhangy for henan HDMI @20191026
                || "cm201_hunan".equals(SystemProperties.get("ro.ysten.province"))
                || "CM201-2_shanxi_iptv".equals(SystemProperties.get("ro.ysten.province"))) {
            hisys.setProperty("persist.suspend.mode", "deep_restart");
            SystemProperties.set("persist.sys.smartsuspendin", "0");
            Log.i("zhangy", "20191024  00 ");
        } else {
            hisys.setProperty("persist.suspend.mode", "smart_suspend");
            SystemProperties.set("persist.sys.smartsuspendin", "1");
            Log.i("zhangy", "20191024   01  ");
        }
        poweroff();
    }

    public void poweroff() {
        //   SystemProperties.set("persist.suspend.mode", "deep_restart");
        String standbymode = SystemProperties.get("persist.suspend.mode", "test");
        String mode1 = "1";
        if (standbymode.contains("deep_restart")) {
            PhoneWindowManager.ledpmanager(5);
            mode1 = "1";
        } else if (standbymode.contains("smart_suspend")) {
            mode1 = "0";
        }

        try {
            Process p = Runtime.getRuntime().exec("/system/bin/sh /system/bin/hello1.sh " + mode1);
            Log.i("ysten_sgc", "set hello1.sh to  " + mode1);

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
            Log.i("xwj", "Show Suspend Dialog ok" + data);
        } catch (IOException e) {
            Log.i("xwj", "Show Suspend Dialog error");
        }

        PhoneWindowManager.ledpmanager(55);

        mPowerManager = (PowerManager) mcontext.getSystemService(Context.POWER_SERVICE);
        mPowerManager.goToSleep(SystemClock.uptimeMillis());


    }

    public void setEthEnabled1(boolean enable) {
        mEthManager = (EthernetManager) mcontext.getSystemService(Context.ETHERNET_SERVICE);
        int state = mEthManager.getEthernetState();
        if (DEBUG)
            Log.i(TAG, "try get Ethernet State : " + state);

        if (state != EthernetManager.ETHERNET_STATE_ENABLED && enable) {
            if (mEthManager.isEthernetConfigured() != true) {
                if (DEBUG)
                    Log.i(TAG,
                            "mEthManager.isEthConfigured:"
                                    + mEthManager.isEthernetConfigured());
            } else {
                mEthManager.setEthernetEnabled(enable);
                if (DEBUG)
                    Log.i(TAG, "try set EthernetEnabled ");
            }
        } else {
            if (DEBUG)
                Log.i(TAG, "Ethernet is Configured show this ");
            mEthManager.setEthernetEnabled(enable);
        }
        if (DEBUG)
            Log.i(TAG, "begin to check net info ");
    }


    public boolean wmacAddrBurningRTW8723nocheck(String value) {
        try {
            PhoneWindowManager.ledpmanager(50);//make sure wifi if on
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (value == null) return false;
        value = value.trim();

        if (value.length() != 17) return false;
        value = value.replaceAll(":", "");
        System.out.println("*** wmac addr burning value:" + value);
        if (value.length() != 12) return false;
//通过iwpriv烧写MAC地址
        Runtime runtime = Runtime.getRuntime();
        String command = String.format(WMAC_BURNING_CMD_M8723BU, value);
        System.out.println("*** wmac addr burning command:" + command);
        try {
            Process proc = runtime.exec(command);
            proc.waitFor();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    //add by guangchao.su for the sdio8822bs
    private boolean wmacAddrBurningSDIO8822BS(String value) {
        String readmac = "";
        String valudemac = "";
        if (value == null) return false;
        value = value.trim();
        if (value.length() != 17) return false;
        value = value.replaceAll(":", "");
        System.out.println("*** wmac addr burning value:" + value);
        if (value.length() != 12) return false;

        //String command = String.format(WMAC_BURNING_CMD_SDIO8822BS_SET, value);
        //Log.d("sgc","use this rtwpriv command:"+ command);
        //通过rtwpriv烧写MAC地址111
        Runtime runtime = Runtime.getRuntime();

        String command = String.format(WMAC_BURNING_CMD_SDIO8822BS_SET, value);
        Log.d("sgc", "use this rtwpriv command:" + command);
        System.out.println("*** wmac addr burning command:" + command);
        try {
            //进入sdio8822bs的start模式
            Process proc1 = runtime.exec("rtwpriv wlan0 mp_start");
            proc1.waitFor();
            Thread.sleep(1000);

            Process proc = runtime.exec(command);
            proc.waitFor();
            Thread.sleep(1000);
            readmac = sdio8822bsmacRead();
            System.out.println("readmac=" + readmac + "valudemac=" + valudemac);
            Log.d("sgc", "use this readmac = " + readmac + ", valudemac = " + valudemac);
            if (readmac.contains(valudemac))
                return true;
            else
                return false;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String sdio8822bsmacRead() throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        //进入sdio8822bs的start模式
        Process proc1 = runtime.exec("rtwpriv wlan0 mp_start");
        proc1.waitFor();
        Thread.sleep(1000);

        String cmd = "rtwpriv wlan0 efuse_get mac";
        Process proc = runtime.exec(cmd);
        proc.waitFor();
        // 输出结果
        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String mac;
        //mac = br.readLine();
        //System.out.println("*** " + cmd + " return line1 = " + mac);
        mac = br.readLine();
        System.out.println("*** " + cmd + " return line2 = " + mac);
        Log.d("sgc", "use this sdio8822bsmacRead() *** " + cmd + " return line2 = " + mac);
        if (mac == null) return "";
        mac = mac.trim();
        System.out.println("*** wmac read:" + mac + "mac.length:" + mac.length());
        Log.d("sgc", "use this sdio8822bsmacRead() *** wmac read:" + mac + "mac.length:" + mac.length());
        if (mac.length() == 12)
            return mac.substring(mac.length() - 12, mac.length());
        else
            return "";
    }

    public boolean wmacAddrBurningRTW8723(String value) {
        String readmac = "";
        String valudemac = "";
        try {
            PhoneWindowManager.ledpmanager(50);//make sure wifi if on
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (value == null) return false;
        value = value.trim();
        valudemac = value;
        if (value.length() != 17) return false;
        value = value.replaceAll(":", "");
        System.out.println("*** wmac addr burning value:" + value);
        if (value.length() != 12) return false;
        //通过iwpriv烧写MAC地址
        Runtime runtime = Runtime.getRuntime();
        String command = String.format(WMAC_BURNING_CMD_M8723BU, value);
        System.out.println("*** wmac addr burning command:" + command);
        try {
            Process proc = runtime.exec(command);
            proc.waitFor();
            readmac = s9083macRead();
            if (readmac == valudemac)
                return true;
            else
                return false;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String s9083macRead() throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        String cmd = "iwpriv wlan0 efuse_get mac";
        Process proc = runtime.exec(cmd);
        proc.waitFor();
        // 输出结果
        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String mac;
        //mac = br.readLine();
        //System.out.println("*** " + cmd + " return line1 = " + mac);
        mac = br.readLine();
        System.out.println("*** " + cmd + " return line2 = " + mac);
        if (mac == null) return "";
        mac = mac.trim();
        if (mac.length() == 37)
            return mac.substring(mac.length() - 17, mac.length());
        else
            return "";
    }

    public boolean wmacAddrBurningRTW(String value) {
        if (value == null) return false;
        value = value.trim();
        if (value.length() != 17) return false;
        value = value.replaceAll(":", "");
        System.out.println("*** wmac addr burning value:" + value);
        if (value.length() != 12) return false;
        //通过iwpriv烧写MAC地址
        Runtime runtime = Runtime.getRuntime();
        String command = String.format(WMAC_BURNING_CMD, value);
        System.out.println("*** wmac addr burning command:" + command);
        try {
            Process proc = runtime.exec(command);
            proc.waitFor();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }


    private boolean wmacAddrBurningMT7601(String value) {
        if (value == null) return false;
        value = value.trim();
        if (value.length() != 17) return false;
        value = value.replaceAll(":", "");
        System.out.println("*** wmac addr burning value:" + value);
        if (value.length() != 12) return false;
        String mac1 = value.substring(2, 4) + value.substring(0, 2);
        String mac2 = value.substring(6, 8) + value.substring(4, 6);
        String mac3 = value.substring(10, 12) + value.substring(8, 10);

        String cmd1 = String.format(WMAC_BURNING_CMD_MT7601, 4, mac1);
        String cmd2 = String.format(WMAC_BURNING_CMD_MT7601, 6, mac2);
        String cmd3 = String.format(WMAC_BURNING_CMD_MT7601, 8, mac3);
        System.out.println("*** wmac addr burning command:" + cmd1);
        System.out.println("*** wmac addr burning command:" + cmd2);
        System.out.println("*** wmac addr burning command:" + cmd3);

        Runtime runtime = Runtime.getRuntime();
        try {
            //进入MT7601的ATE模式
            Process proc = runtime.exec("iwpriv wlan0 set ATE=ATESTART");
            proc.waitFor();
            Thread.sleep(1000);

            //通过iwpriv烧写MAC地址
            proc = runtime.exec(cmd1);
            proc.waitFor();
            boolean exitVal = proc.exitValue() == 0;
            proc = runtime.exec(cmd2);
            proc.waitFor();
            exitVal &= proc.exitValue() == 0;
            proc = runtime.exec(cmd3);
            proc.waitFor();
            exitVal &= proc.exitValue() == 0;

            //读取mac地址，并校验
            Thread.sleep(1000);
            String mac_r1 = MT7601macRead(4);
            String mac_r2 = MT7601macRead(6);
            String mac_r3 = MT7601macRead(8);
            System.out.println("*** MT7601 e2p[4]=" + mac_r1 + ", e2p[6]=" + mac_r2 + ", e2p[8]=" + mac_r3);

            // 打印日志信息到Reserve0分区
            //    String kmsg[] = {"/system/xbin/su", "-c", "dmesg > /mnt/Reserve0/kmsg.log"};
            //  proc = runtime.exec(kmsg);
            // proc.waitFor();
            // String logcat[] = {"/system/xbin/su", "-c", "logcat -vtime -d > /mnt/Reserve0/android.log"};
            // proc = runtime.exec(logcat);
            // proc.waitFor();

            if (!exitVal ||
                    mac1.compareToIgnoreCase(mac_r1) != 0 ||
                    mac2.compareToIgnoreCase(mac_r2) != 0 ||
                    mac3.compareToIgnoreCase(mac_r3) != 0)
                return false;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }

    private String MT7601macRead(int val) throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        String cmd = String.format("iwpriv wlan0 e2p %d", val);
        Process proc = runtime.exec(cmd);
        proc.waitFor();
        // 输出结果
        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String mac;
        mac = br.readLine();
        System.out.println("*** " + cmd + " return line1 = " + mac);
        mac = br.readLine();
        System.out.println("*** " + cmd + " return line2 = " + mac);
        if (mac == null) return "";
        mac = mac.trim();
        return mac.substring(mac.length() - 4, mac.length());
    }

    public void installROM(String path, boolean isSlience) {
        Log.i(TAG, "installROM " + path + " " + isSlience);
        final String k_RecoveryDirPath = "/cache/recovery";
        final String k_CommandFilePath = k_RecoveryDirPath + "/command";
        final String k_LogFilePath = k_RecoveryDirPath + "/log";

        String arg = "--update_package=" + path;

        File recoveryDir = new File(k_RecoveryDirPath);
        recoveryDir.mkdirs();
        File commandFile = new File(k_CommandFilePath);
        commandFile.delete();
        File logFile = new File(k_LogFilePath);
        logFile.delete();
        try {
            FileWriter command = new FileWriter(commandFile);
            try {
                command.write(arg);
                command.write("\n");
            } finally {
                command.close();
            }
            Log.i(TAG, "installROM write command file end");
        } catch (Exception e) {
            // TODO: handle exception
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        Log.i(TAG, "installROM begin to install " + isSlience);
        // Having written the command file, go ahead and reboot
	/*	
               if(!isSlience){
			PowerManager pm = (PowerManager) mcontext
					.getSystemService(Context.POWER_SERVICE);
			pm.reboot("recovery");
			Log.i(TAG, "installROM begin to force install error");       
		}
		else{
			SystemProperties.set("sys.powerctl", "reboot,silence");
		}
       */
    }


}
