package com.android.silenceinstaller;

import java.io.IOException;
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import android.os.SystemProperties;//add by xumiao at 2019/11/21 jiling silent install
import android.content.ComponentName;//add by xumiao at 2019/11/21 jiling silent install
import android.net.Uri;
import java.io.InputStream;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageParser;
import android.content.pm.PackageUserState;
import android.content.pm.ResolveInfo;
import java.io.File;
import android.content.pm.PackageInfo;
import java.util.List;
import android.text.TextUtils;


public class InstallService extends IntentService {
	private String TAG = "SilenceInstallService";
	private String UNKNOW_PACKAGE_NAME = "com.unknow.package";

	public InstallService() {  
        super("com.android.silenceinstaller.InstallService");  
    }  
  
    @Override  
    protected void onHandleIntent(Intent intent) {
    	
    	String Action = intent.getAction();
    	String ApkPath = "unknown";
    	String PkgName = "unknown";
    	
    	if (Action.equals("com.android.SilenceInstall.Start"))
    	{
    		ApkPath = intent.getData().getPath();
			String packageName = this.getApplicationContext().getPackageName();//add by xumiao at 2019/11/21 jiling silent install
        	Log.i("SilenceInstallService", "Receive a " + Action +" intent to install " + ApkPath);
        	installPackage(intent.getData());
    	}
    	else if (Action.equals("com.android.SilenceUninstall.Start"))
    	{
    		PkgName = intent.getData().getPath();
        	Log.i("SilenceInstallService", "Receive a " + Action +" intent to Uninstall " + PkgName);
        	uninstallPackage(PkgName);
    	}
    	else
    	{
    		Log.e("SilenceInstallService", "Receive a unknown intent");
    	}
    }
    
    public void installPackage(Uri mPackageURI)
    {
		int result = PackageManager.INSTALL_SUCCEEDED;
		PackageInfo mPkgInfo = null;
    	try
    	{
			boolean installSuccess = true;
			PackageManager mPm = getPackageManager();

			String ApkPath = mPackageURI.getPath();

			Log.i("SilenceInstallService", "install " + ApkPath);

			String scheme = mPackageURI.getScheme();
            if (scheme != null && !"file".equals(scheme) && !"package".equals(scheme)) {
                Log.w(TAG, "Unsupported scheme " + scheme);
                installSuccess = false;
                result = PackageManager.INSTALL_FAILED_INVALID_URI;
                //发送失败广播
                installFailForGDZhuoYing(UNKNOW_PACKAGE_NAME, result);
                return;
            }
            if ("package".equals(mPackageURI.getScheme())) {
                try {
                    mPkgInfo = mPm.getPackageInfo(mPackageURI.getSchemeSpecificPart(),
                            PackageManager.GET_PERMISSIONS | PackageManager.GET_UNINSTALLED_PACKAGES);
                } catch (NameNotFoundException e) {
                }
                if (mPkgInfo == null) {
                    Log.w(TAG, "Requested package " + mPackageURI.getScheme()
                            + " not available. Discontinuing installation");
                    installSuccess = false;
                    result = PackageManager.INSTALL_FAILED_INVALID_APK;
                }
            } else {
                File sourceFile = new File(mPackageURI.getPath());
                PackageParser.Package parsed = PackageUtil.getPackageInfo(sourceFile);
    
                // Check for parse errors
                if (parsed == null) {
                    Log.w(TAG, "Parse error when parsing manifest. Discontinuing installation");
                    installSuccess = false;
                    result = PackageManager.INSTALL_FAILED_INVALID_APK;
                }else{
                    mPkgInfo = PackageParser.generatePackageInfo(parsed, null,
                        PackageManager.GET_PERMISSIONS, 0, 0, null,new PackageUserState());
                }
            }

            if(!installSuccess){
                Log.w(TAG, "get mPkgInfo failed ");
                // 发送广播
                installFailForGDZhuoYing(UNKNOW_PACKAGE_NAME, result);
                return;
            }
			
            Runtime rt = Runtime.getRuntime();
            Log.i("SilenceInstallService", "pm install -r " + ApkPath);
            Process proc = rt.exec("pm install -r -f " + ApkPath);
           
	    StreamGobbler outputGobbler = new 
            StreamGobbler(proc.getInputStream(), "stdout");
                    
            StreamGobbler errorGobbler = new 
            StreamGobbler(proc.getErrorStream(), "stderr");            
                
            outputGobbler.start();
            errorGobbler.start();

            proc.waitFor();

            Intent intent = new Intent("com.android.SilenceInstall.Over");
			intent.putExtra("ApkPath", ApkPath);
			
			Intent mLaunchIntent;

			if(outputGobbler.GetLastResponse().equals("Success")) {
				Log.i("SilenceInstallService", "outputGobbler.GetLastResponse " + outputGobbler.GetLastResponse());
				intent.putExtra("Result", outputGobbler.GetLastResponse());
				
				mLaunchIntent = getPackageManager().getLaunchIntentForPackage(
                    mPkgInfo.packageName);
                boolean enabled = false;
                if(mLaunchIntent != null) {
                    List<ResolveInfo> list = getPackageManager().
                            queryIntentActivities(mLaunchIntent, 0);
                    if (list != null && list.size() > 0) {
                        enabled = true;
                    }
                    Log.i("SilenceInstallService", "mLaunchIntent " + mLaunchIntent.toString());
                    if (enabled) {
                        installSuccessForGDZhuoYing(mLaunchIntent, mPkgInfo.packageName,
                            PackageManager.INSTALL_SUCCEEDED);
                    }
				}
				
			}
            else {
				Log.i("SilenceInstallService", "errorGobbler.GetLastResponse " + errorGobbler.GetLastResponse());

				installSuccess = false;
				
				String errResult = errorGobbler.GetLastResponse();
				if( errResult.contains("INSTALL_FAILED_INSUFFICIENT_STORAGE") ) {
					errResult = "TA-001";
				} else if( errResult.contains("INSTALL_FAILED_VERSION_DOWNGRADE") ) {
					errResult = "TA-002";
				} else if( (errResult.contains("INSTALL_FAILED_VERIFICATION_FAILURE"))
							|| (errResult.contains("INSTALL_FAILED_VERIFICATION_TIMEOUT"))
							|| (errResult.contains("INSTALL_PARSE_FAILED_NO_CERTIFICATES"))
							|| (errResult.contains("INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES")) ) {
					errResult = "TA-003";
				} else if( errResult.contains("INSTALL_FAILED_INVALID_APK") ) {
					errResult = "TA-004";
				} else if( errResult.contains("INSTALL_FAILED_DUPLICATE_PACKAGE") ) {
					errResult = "TA-005";
				}
				Log.i("SilenceInstallService", "errResult " + errResult);
				intent.putExtra("Result", errResult);

				processInstallError(mPkgInfo.packageName, errResult);
			}
        	sendBroadcast(intent);
    	}
    	catch (IOException e)
    	{
			Log.e("SilenceInstallService", "Failed for a IOException");
			Log.d(TAG,Log.getStackTraceString(e));
			processInstallException(mPkgInfo);
    	}
    	catch (InterruptedException e)
    	{
			Log.e("SilenceInstallService", "Failed for a InterruptedException");
			Log.d(TAG,Log.getStackTraceString(e));
			processInstallException(mPkgInfo);
		}
		catch (Exception e){
			Log.d(TAG,Log.getStackTraceString(e));
			processInstallException(mPkgInfo);
        }
	}
	
	private void processInstallException(PackageInfo packageInfo){
		if(packageInfo != null && !TextUtils.isEmpty(packageInfo.packageName)){
			installFailForGDZhuoYing(packageInfo.packageName, PackageManager.INSTALL_FAILED_INTERNAL_ERROR);
		}else{
			installFailForGDZhuoYing(UNKNOW_PACKAGE_NAME, PackageManager.INSTALL_FAILED_INTERNAL_ERROR);
		}
	}

	/**
     * 根据错误信息返回PackageManager中对应的错误码
     * @param packageName
     * @param errorresult
     */
    private void processInstallError(String packageName, String errorresult){
        int errorCode = PackageManager.INSTALL_FAILED_INTERNAL_ERROR;
        errorCode = getErrorCode(errorresult);
        installFailForGDZhuoYing(packageName, errorCode);
    }

    private int getErrorCode(String errorString){
        if (TextUtils.isEmpty(errorString)) {
            return PackageManager.INSTALL_FAILED_INTERNAL_ERROR;
        }
        if (errorString.contains("INSTALL_FAILED_ALREADY_EXISTS")) {
            return PackageManager.INSTALL_FAILED_ALREADY_EXISTS;
        }
        if (errorString.contains("INSTALL_FAILED_INVALID_APK")) {
            return PackageManager.INSTALL_FAILED_INVALID_APK;
        }
        if (errorString.contains("INSTALL_FAILED_INVALID_URI")) {
            return PackageManager.INSTALL_FAILED_INVALID_URI;
        }
        if (errorString.contains("INSTALL_FAILED_INSUFFICIENT_STORAGE")) {
            return PackageManager.INSTALL_FAILED_INSUFFICIENT_STORAGE;
        }
        if (errorString.contains("INSTALL_FAILED_DUPLICATE_PACKAGE")) {
            return PackageManager.INSTALL_FAILED_DUPLICATE_PACKAGE;
        }
        if (errorString.contains("INSTALL_FAILED_NO_SHARED_USER")) {
            return PackageManager.INSTALL_FAILED_NO_SHARED_USER;
        }
        if (errorString.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE")) {
            return PackageManager.INSTALL_FAILED_UPDATE_INCOMPATIBLE;
        }
        if (errorString.contains("INSTALL_FAILED_SHARED_USER_INCOMPATIBLE")) {
            return PackageManager.INSTALL_FAILED_SHARED_USER_INCOMPATIBLE;
        }
        if (errorString.contains("INSTALL_FAILED_MISSING_SHARED_LIBRARY")) {
            return PackageManager.INSTALL_FAILED_MISSING_SHARED_LIBRARY;
        }
        if (errorString.contains("INSTALL_FAILED_REPLACE_COULDNT_DELETE")) {
            return PackageManager.INSTALL_FAILED_REPLACE_COULDNT_DELETE;
        }
        if (errorString.contains("INSTALL_FAILED_DEXOPT")) {
            return PackageManager.INSTALL_FAILED_DEXOPT;
        }
        if (errorString.contains("INSTALL_FAILED_OLDER_SDK")) {
            return PackageManager.INSTALL_FAILED_OLDER_SDK;
        }
        if (errorString.contains("INSTALL_FAILED_CONFLICTING_PROVIDER")) {
            return PackageManager.INSTALL_FAILED_CONFLICTING_PROVIDER;
        }
        if (errorString.contains("INSTALL_FAILED_NEWER_SDK")) {
            return PackageManager.INSTALL_FAILED_NEWER_SDK;
        }
        if (errorString.contains("INSTALL_FAILED_TEST_ONLY")) {
            return PackageManager.INSTALL_FAILED_TEST_ONLY;
        }
        if (errorString.contains("INSTALL_FAILED_CPU_ABI_INCOMPATIBLE")) {
            return PackageManager.INSTALL_FAILED_CPU_ABI_INCOMPATIBLE;
        }
        if (errorString.contains("INSTALL_FAILED_MISSING_FEATURE")) {
            return PackageManager.INSTALL_FAILED_MISSING_FEATURE;
        }
        if (errorString.contains("INSTALL_FAILED_CONTAINER_ERROR")) {
            return PackageManager.INSTALL_FAILED_CONTAINER_ERROR;
        }
        if (errorString.contains("INSTALL_FAILED_INVALID_INSTALL_LOCATION")) {
            return PackageManager.INSTALL_FAILED_INVALID_INSTALL_LOCATION;
        }
        if (errorString.contains("INSTALL_FAILED_MEDIA_UNAVAILABLE")) {
            return PackageManager.INSTALL_FAILED_MEDIA_UNAVAILABLE;
        }
        if (errorString.contains("INSTALL_FAILED_VERIFICATION_TIMEOUT")) {
            return PackageManager.INSTALL_FAILED_VERIFICATION_TIMEOUT;
        }
        if (errorString.contains("INSTALL_FAILED_VERIFICATION_FAILURE")) {
            return PackageManager.INSTALL_FAILED_VERIFICATION_FAILURE;
        }
        if (errorString.contains("INSTALL_FAILED_PACKAGE_CHANGED")) {
            return PackageManager.INSTALL_FAILED_PACKAGE_CHANGED;
        }
        if (errorString.contains("INSTALL_FAILED_UID_CHANGED")) {
            return PackageManager.INSTALL_FAILED_UID_CHANGED;
        }
        if (errorString.contains("INSTALL_PARSE_FAILED_NOT_APK")) {
            return PackageManager.INSTALL_PARSE_FAILED_NOT_APK;
        }
        if (errorString.contains("INSTALL_PARSE_FAILED_BAD_MANIFEST")) {
            return PackageManager.INSTALL_PARSE_FAILED_BAD_MANIFEST;
        }
        if (errorString.contains("INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION")) {
            return PackageManager.INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION;
        }
        if (errorString.contains("INSTALL_PARSE_FAILED_NO_CERTIFICATES")) {
            return PackageManager.INSTALL_PARSE_FAILED_NO_CERTIFICATES;
        }
        if (errorString.contains("INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES")) {
            return PackageManager.INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES;
        }
        if (errorString.contains("INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING")) {
            return PackageManager.INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING;
        }
        if (errorString.contains("INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME")) {
            return PackageManager.INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME;
        }
        if (errorString.contains("INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID")) {
            return PackageManager.INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID;
        }
        if (errorString.contains("INSTALL_PARSE_FAILED_MANIFEST_MALFORMED")) {
            return PackageManager.INSTALL_PARSE_FAILED_MANIFEST_MALFORMED;
        }
        if (errorString.contains("INSTALL_PARSE_FAILED_MANIFEST_EMPTY")) {
            return PackageManager.INSTALL_PARSE_FAILED_MANIFEST_EMPTY;
        }
        if (errorString.contains("INSTALL_FAILED_INTERNAL_ERROR")) {
            return PackageManager.INSTALL_FAILED_INTERNAL_ERROR;
        }
        if (errorString.contains("INSTALL_FAILED_USER_RESTRICTED")) {
            return PackageManager.INSTALL_FAILED_USER_RESTRICTED;
        }
        return PackageManager.INSTALL_FAILED_INTERNAL_ERROR;
    }

   /**
     * 广东卓影安装成功操作-自动打开应用，发成功广播
     */
    private void installSuccessForGDZhuoYing(Intent mLaunchIntent, 
        String packageName, int resultCode){
        Log.e(TAG, "installSuccessForGDZhuoYing packageName:"+packageName+" resultCode:"+resultCode);
        // begin: add by ysten.wenglei at 20200404: 广东卓影自动安装
        installResultBroadcastForGDZhuoYing(packageName, resultCode);
        startActivity(mLaunchIntent);
        // end: add by ysten.wenglei at 20200404: 广东卓影自动安装
    }

    /**
     * 广东卓影安装失败操作-发失败广播
     */
    private void installFailForGDZhuoYing(String packageName, int resultCode){
        // begin: add by ysten.wenglei at 20200404: 广东卓影自动安装
        installResultBroadcastForGDZhuoYing(packageName, resultCode);
        // end: add by ysten.wenglei at 20200404: 广东卓影自动安装
    }

    /**
     * 广东卓影安装结果操作-发广播
     */
    private void installResultBroadcastForGDZhuoYing(String packageName, int resultCode){
         Log.e(TAG, "installResultBroadcastForGDZhuoYing packageName:"+packageName+" resultCode:"+resultCode);
         // begin: add by ysten.wenglei at 20200404: 广东卓影自动安装
         Intent intent = new Intent();
         intent.setAction("com.unionman.APPINSTALL.ACTION");
         intent.putExtra("packagename", packageName);
         intent.putExtra("resultcode", resultCode);
         sendBroadcast(intent);
         // end: add by ysten.wenglei at 20200404: 广东卓影自动安装
    }

    public void uninstallPackage(String PkgName)
    {
    	Log.i("SilenceInstallService", "uninstall " + PkgName);
    	try
    	{
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec("pm uninstall " + PkgName);
            
            StreamGobbler outputGobbler = new 
            StreamGobbler(proc.getInputStream(), "stdout");
                    
            StreamGobbler errorGobbler = new 
            StreamGobbler(proc.getErrorStream(), "stderr");            
                
            outputGobbler.start();
            errorGobbler.start();

            proc.waitFor();

            Intent intent = new Intent("com.android.SilenceUninstall.Over");
            intent.putExtra("PkgName", PkgName);
            intent.putExtra("Result", outputGobbler.GetLastResponse());
        	sendBroadcast(intent);
    	}
    	catch (IOException e)
    	{
    		Log.e("SilenceInstallService", "Failed for a IOException");
    	}
    	catch (InterruptedException e)
    	{
    		Log.e("SilenceInstallService", "Failed for a InterruptedException");
    	}
    }
}
