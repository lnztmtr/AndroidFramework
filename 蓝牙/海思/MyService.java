package com.android.hiBTAutopair;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothInputDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.SystemProperties;
import java.io.IOException;
import android.bluetooth.BluetoothUuid;
import android.text.TextUtils;
import android.widget.Toast;
import android.os.SystemProperties;
import android.util.Log;
import java.util.UUID;


import java.util.Set;

public class MyService extends Service {

    protected  static final int MSG_BLE_START_SCAN = 0x10000001;
    protected  static final int MSG_BLE_STOP_SCAN = 0x10000002;
    protected  static final int MSG_BLE_KILL_SELF = 0x10000003;
    protected  static final int MSG_BLE_TRY_CONNECT = 0x10000004;

    static final int COMPLETE_NAME_FLAG = 0x09;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mTargetRcDevice = null;
    private BluetoothDevice mBondedDevice = null;
    private BluetoothDevice mBondedDevice2 = null;
    private BluetoothSocket mBluetoothSocket;
    private boolean mBleScanning=false;
    private Toast toast;

    private BluetoothInputDevice mInputDevice;
    private boolean mInputDeviceReady = false;
    private boolean mServiceInitReady = false;
    private String info = "";
    private UUID mactekHartModemUuid;
    private String uuidValue = "00001812-0000-1000-8000-00805f9B34FB";
    Handler handler = new Handler();

    @Override
    public void onCreate() {
        Common.printLog("MyService BT autopair onCreate");
        super.onCreate();
    	info = getString(R.string.started);
        //showToast();//add by ysten xumiao at 20181221 do not show enable Buletools toast
  	mHandler.sendEmptyMessageDelayed(MSG_BLE_KILL_SELF,60*10*1000);
        onBleServiceInit();
    }

    @Override
    public void onDestroy() {
        Common.printLog("MyService BT autopairnDestroy...");
        info = getString(R.string.exited);
        //showToast();//add by ysten xumiao at 20181221 do not show  Buletools off toast
        onMyServiceUnInit();
        mHandler.removeMessages(MSG_BLE_KILL_SELF);
        mHandler.removeMessages(MSG_BLE_START_SCAN);
        mHandler.removeMessages(MSG_BLE_STOP_SCAN);
        mHandler.removeMessages(MSG_BLE_TRY_CONNECT);
        cancelToast();
        super.onDestroy();
    }

    private void onBleServiceInit(){
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Common.printLog("device is no support ble.");
            stopSelf();
            return ;
        }
        //register  bluetooth receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_UUID);
        intentFilter.addAction(BluetoothInputDevice.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(mBluetoothReceiver, intentFilter);

        mBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter==null){
            Common.printLog("mBluetoothAdapter is null, stop BT autopair MyService");
            stopSelf();
            return ;
        }
        //TODO:read autopair name from config file        

        mBluetoothAdapter.getProfileProxy(getBaseContext(),mInputDeviceServiceListener,BluetoothProfile.INPUT_DEVICE);
        setDebug();

        /*we'll only trigger LE scan when RC unpaired*/
        if(!hasRcInBondedList()) {
           Common.printLog("RC is not paired,do LE scan");
           mHandler.sendEmptyMessage(MSG_BLE_START_SCAN);
        } else {
	    mactekHartModemUuid = UUID.fromString(uuidValue);
	    //每次都走重连流程
	    if(mInputDevice != null) {
	        //已经获取到inputdevice，直接连接
            Common.printLog("already bond and has input device connect directly");
	    	doHogpConnect(mBondedDevice);
	    }else {
	        //未获取到inputdevice，等待5s后尝试重连
            Common.printLog("already bond and no input device connect 5s later");
            mHandler.sendEmptyMessageDelayed(MSG_BLE_TRY_CONNECT,5*1000);
        }
	    Common.printLog("RC already paired, stop BT autopair MyService");
	    mHandler.sendEmptyMessage(MSG_BLE_KILL_SELF);
        }

        mServiceInitReady=true;
    }

    private void onMyServiceUnInit(){
        //unregister Receiver
        mTargetRcDevice = null;
        try{
            unregisterReceiver(mBluetoothReceiver);
        }catch (Exception e){
            Common.printLog("unregisterReceiver error:"+e.getMessage());
        }

        if(mBluetoothAdapter==null){
            Common.printLog("onMyServiceUnInit mBluetoothAdapter is null.");
            return;
        }
        if(mServiceInitReady){
            mHandler.sendEmptyMessage(MSG_BLE_STOP_SCAN);
            mBluetoothAdapter.closeProfileProxy(BluetoothProfile.INPUT_DEVICE, mInputDevice);
        }
        mServiceInitReady=false;
    }

    private void setDebug(){
       try{
           Common.IS_DEBUG = SystemProperties.getBoolean("persist.btautopair.debug", false);
       } catch(Exception e) {
           Common.printLog("set btautopair debug error:"+e.getMessage());
       }    
    }
    private boolean hasRcInBondedList() {
        Set<BluetoothDevice> bList = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice mlist : bList) {
            if (mlist.getName() != null && Common.checkBluetoothName(mlist.getName())) {
		        mBondedDevice = mlist;
                return true;
            }
        }
        return false;
    }

    //the receiver
    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            Common.printLog("mBluetoothReceiver onReceive() action=" + action);
	        int preState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, 0xff);
            int curState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0xff);
            if (mTargetRcDevice == null) {
		Common.printLog("mTargetRcDevice is null, ignore broadcast");
		return;
            }

            BluetoothDevice remoteDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (remoteDevice == null) {
                Common.printLog("remoteDevice is null, ignore broadcast");
                return;
            }
            Common.printLog("mTargetRcDevice = " + mTargetRcDevice +", remoteDevice = " +remoteDevice);

            if (!mTargetRcDevice.toString().equals(remoteDevice.toString())) {
                Common.printLog("remoteDevice != mTargetRcDevice , ignore broadcast");
                return;
	    }
            Common.printLog(remoteDevice + " type = " + remoteDevice.getType());
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {				
                //int preState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
               // int curState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                Common.printLog("10:bond none, 11:bonding, 12:bonded");
                Common.printLog("Bond state change("+remoteDevice+"): "+preState+"->"+curState);
                if(curState == BluetoothDevice.BOND_NONE && preState != BluetoothDevice.BOND_NONE){
                    mHandler.sendEmptyMessageDelayed(MSG_BLE_START_SCAN, 2000);					
                } else if (curState == BluetoothDevice.BOND_BONDING && preState == BluetoothDevice.BOND_NONE){
                    mHandler.sendEmptyMessage(MSG_BLE_STOP_SCAN);
                }
		if(curState == BluetoothProfile.STATE_DISCONNECTED) {
		    Common.printLog("ystenlk,try connect");
		    doHogpConnect(remoteDevice);
		}
            } else if(action.equals(BluetoothDevice.ACTION_UUID)){
		Common.printLog("Receive BluetoothDevice.ACTION_UUID action,do HOGP connect");
                mHandler.sendEmptyMessage(MSG_BLE_STOP_SCAN);
                doHogpConnect(remoteDevice);				
            }else if(action.equals(BluetoothInputDevice.ACTION_CONNECTION_STATE_CHANGED)){                
                //int preState = intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, 0xff);
                //int curState = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, 0xff);
                Common.printLog("0:disconnected, 1:connecting, 2:connected, 3:disconnecting");
                Common.printLog("Connection state change("+remoteDevice+"): "+preState+"->"+curState);
                if (curState == BluetoothProfile.STATE_CONNECTED) {
		            SystemProperties.set("persist.sys.bt.flag", "false");
                    Common.printLog("show connected");
                    info = getString(R.string.connected);
                    showToast();
                    Common.printLog("connect success kill process 5s later");
                    mHandler.sendEmptyMessage(MSG_BLE_KILL_SELF);
                }
		if(curState == BluetoothProfile.STATE_DISCONNECTED) {
		    Common.printLog("ystenlk,try connect");
		    doHogpConnect(remoteDevice);
		}


                if(curState != BluetoothProfile.STATE_DISCONNECTED && preState == BluetoothProfile.STATE_DISCONNECTED){
                    mHandler.sendEmptyMessage(MSG_BLE_STOP_SCAN);
                }
            }
        }
    };

    private BluetoothProfile.ServiceListener mInputDeviceServiceListener = new BluetoothProfile.ServiceListener(){

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Common.printLog("onServiceConnected profile="+profile);
            mInputDevice = (BluetoothInputDevice) proxy;
            mInputDeviceReady=true;
        }

        @Override
        public void onServiceDisconnected(int profile) {
            Common.printLog("onServiceDisconnected profile="+profile);
            mInputDevice = null;
            mInputDeviceReady=false;
        }
    };

    private void doHogpConnect(BluetoothDevice device){
        ParcelUuid[] uuids = device.getUuids();
        if (uuids == null){
	    if(mInputDevice !=null){
                mInputDevice.connect(device);
            } else {
                Common.printLog("doHogpConnect uuids is null, doHogpConnect fail");
		return;
            }
        }

        if((device.getType()== BluetoothDevice.DEVICE_TYPE_CLASSIC)) {
            Common.printLog( "classic type device,Not a LE device, doHogpConnect fail");
            return;
        }

        if(!BluetoothUuid.isUuidPresent(uuids, BluetoothUuid.Hogp)) {
            Common.printLog("Not support HOGP, doHogpConnect fail");
            return;
        }

        if(mInputDevice !=null){
            Common.printLog("HOGP connect to "+device);
            mInputDevice.connect(device);
        } else {
            Common.printLog("mInputDevice is null, doHogpConnect fail");
        }
    }
    private Handler mHandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_BLE_START_SCAN:
                    handleBleStartScan();
                    break;
                case MSG_BLE_STOP_SCAN:
                    handleBleStopScan();
                    break;
		        case MSG_BLE_TRY_CONNECT:
		            if(mBondedDevice != null) {
		                doHogpConnect(mBondedDevice);
		            }
		        case MSG_BLE_KILL_SELF:
		            stopSelf();
		            handleBleKillSelf();
		            break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    private void handleBleKillSelf(){
        android.os.Process.killProcess(android.os.Process.myPid());
	System.exit(0);
    }
    private void handleBleStartScan(){
		
	Common.printLog("handleBleStartScan enter");
        if(!mBleScanning) {
            mBleScanning = true;
			Common.printLog("===>Start BLE scan");
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
    }

    private void handleBleStopScan(){    
        Common.printLog("handleBleStopScan enter");
        if(mHandler.hasMessages(MSG_BLE_START_SCAN)) {
            mHandler.removeMessages(MSG_BLE_START_SCAN);
        } else if(mHandler.hasMessages(MSG_BLE_STOP_SCAN)) {
            mHandler.removeMessages(MSG_BLE_STOP_SCAN);
        }

        if(mBleScanning) {
            mBleScanning=false;
            Common.printLog("===>Stop BLE scan");
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =  new BluetoothAdapter.LeScanCallback(){

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            Common.printLog("onLeScan device="+ device + ", name = "+device.getName()+ ", rssi=" + rssi);
            if(isGoodHogpRecord(scanRecord)){
                if (device.getName() == null) {
                    Common.printLog("scan record mached,but device name is null ,stop le scan and start le scan after 2s ");
                    mHandler.sendEmptyMessage(MSG_BLE_STOP_SCAN);
                    try  {
                        Thread.currentThread().sleep(2000);
                    } catch(Exception e){
                        Common.printLog("Le scanCallback sleep exception " + e.getMessage());
                    }
                    mHandler.sendEmptyMessage(MSG_BLE_START_SCAN);			
                } else {
	            info = getString(R.string.pair);
				/**
				 * date: 2020-11-10 10:51:13 
				 * modify: by chenweizhong
				 * for bug 9863: 30键蓝牙遥控器自动配对功能不成功
				 * desc:Android 4.4中BluetoothAdapter.LeScanCallback的回调是运行在子线程
				 * 此处由于在部分型号机顶盒中未报异常，保留提示，仅抓取异常
				 */
				 try{
				     showToast();
				 }catch(Exception e){
				 	 Common.printLog("onLeScan 配对提示异常");
				 }
                    Common.printLog("RC "+device +"name "+device.getName() +" matched, stop LE scan and bind to RC");		
	            mTargetRcDevice = device;
                    mHandler.sendEmptyMessage(MSG_BLE_STOP_SCAN);
                    if(device.getBondState() == BluetoothDevice.BOND_NONE) {
                        Common.printLog("creat bond to "+ device);
                        if(device.createBond() == false) {
                            Common.printLog("creat bond failed to "+ device);
                        }
                    } 
                }
            }
        }
    };

private static boolean isGoodHogpRecord(byte[] scanRecord)
{
        int i, length=scanRecord.length;
        i = 0;
        byte[] RcName= new byte[50];
        String decodedName = null;

        while (i< length-2) {
            int element_len = scanRecord[i];
            byte element_type = scanRecord[i+1];
            if(element_type == COMPLETE_NAME_FLAG) {
                System.arraycopy(scanRecord, i+2, RcName, 0, element_len-1);
                try {
                    decodedName = new String(RcName, "UTF-8");					
                    if(Common.checkBluetoothName(decodedName)== true) {						
                        return true;
                    }
                } catch (Exception e) {
                    Common.printLog("MatchExtracName error:"+e.getMessage());
                }      
            }
            i+= element_len+1;
        }
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Common.printLog("BT autopair MyService onStartCommand...");
        return super.onStartCommand(intent, flags, startId);
    }

    private void showToast() {
        if (TextUtils.isEmpty(info))
            return;

        if (toast == null) {
            toast = Toast.makeText(MyService.this, info, Toast.LENGTH_LONG);
        } else {
            toast.setText(info);
        }
        toast.show();
    }

    private void cancelToast() {
        if (null != toast) {
            toast.cancel();
            toast = null;
        }
    }

    //for bindService
    private final LocalBinder mBinder=new LocalBinder();
    public class LocalBinder extends Binder{
         public MyService getService(){
             return MyService.this;
         }
    }
    @Override
    public IBinder onBind(Intent intent) {
        Common.printLog("BT autopair MyService onBind...");
        return mBinder;
    }
}

