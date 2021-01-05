package com.android.server;
import android.graphics.Rect;
import android.os.RemoteException;
import android.util.Log;
import android.os.display.IDisplayManager;
public class DisplayService extends IDisplayManager.Stub{

    private final String TAG = "DisplayService";

    public native int nativesetFmt(int fmt);

    public native int nativegetFmt();

    public native int nativesetOutRange(int x, int y, int a, int b);

    public native int[] nativegetOutRange();

    public native void nativeSaveParams();

    public native boolean nativeisSupport3D();

    public native int nativesethdmipassthrouth (int mode);

    public native int nativesetspdfpassthrough(int mode);

    public native int nativeGetTvStatus();

    public native int[] nativeGetTvEdid();
    public native int[] nativegetDisplayCapability();

    public native int nativesetOptimalFormatEnable(int sign);
    public int setFmt(int fmt) throws RemoteException {
        return nativesetFmt(fmt);
    }

    public int getFmt() throws RemoteException {
        Log.d(TAG, "into getFmt()");
        return nativegetFmt();
    }

    public int setOutRange(int x, int y, int a, int b)
        throws RemoteException {
        return nativesetOutRange(x, y, a, b);
    }

    public Rect getOutRange() throws RemoteException {
        Rect rect = new Rect(0, 0, 0, 0);
        int[] ret = new int[4];
        ret = nativegetOutRange();
        rect.left = ret[0];
        rect.top = ret[1];
        rect.right = ret[2];
        rect.bottom = ret[3];
        return rect;
    }

    public void saveParams() throws RemoteException {
        nativeSaveParams();
    }

    public boolean isSupport3D() throws RemoteException {
        return nativeisSupport3D();
    }

    public int setHdmiPassThrough(int mode) throws RemoteException {
        return nativesethdmipassthrouth(mode);
    }

    public int setSpdfPassThrough(int mode) throws RemoteException {
        return nativesetspdfpassthrough(mode);
    }

    public int SetOptimalFormatEnable(int sign)throws RemoteException{
	return nativesetOptimalFormatEnable(sign);
    }

    public int getTvStatus() throws RemoteException {

        return nativeGetTvStatus();
    }

    public int[] getTVEDID() throws RemoteException {
        Log.d(TAG, "get tv edid > native");
        int [] ret = new int[256];
        ret = nativeGetTvEdid();
        return ret;
    }
    public int[] getDisplayCapability()
    {
        return nativegetDisplayCapability();
    }


}
