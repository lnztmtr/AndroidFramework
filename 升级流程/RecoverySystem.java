/*
 * Copyright (C) 2010 The Android Open Source Project
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

package android.os;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.os.Environment;
import android.os.SystemProperties;
import java.lang.Process;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.nio.channels.FileChannel;

import org.apache.harmony.security.asn1.BerInputStream;
import org.apache.harmony.security.pkcs7.ContentInfo;
import org.apache.harmony.security.pkcs7.SignedData;
import org.apache.harmony.security.pkcs7.SignerInfo;
import org.apache.harmony.security.provider.cert.X509CertImpl;
import com.droidlogic.instaboot.InstabootManager;
import android.provider.Settings;
import android.content.ContentResolver;
import android.util.Slog;
import android.net.ethernet.EthernetDevInfo;

/**
 * RecoverySystem contains methods for interacting with the Android
 * recovery system (the separate partition that can be used to install
 * system updates, wipe user data, etc.)
 */
public class RecoverySystem {
    private static final String TAG = "RecoverySystem";
    private static final String SIGN_UPDATE_UPGRADE_STEP ="ubootenv.var.upgrade_step";
    /**
     * Default location of zip file containing public keys (X509
     * certs) authorized to sign OTA updates.
     */
    private static final File DEFAULT_KEYSTORE =
        new File("/system/etc/security/otacerts.zip");

    /** Send progress to listeners no more often than this (in ms). */
    private static final long PUBLISH_PROGRESS_INTERVAL_MS = 500;

    /** Used to communicate with recovery.  See bootable/recovery/recovery.c. */
    private static File RECOVERY_DIR = new File("/cache/recovery");
    private static File COMMAND_FILE = new File(RECOVERY_DIR, "command");
    private static File LOG_FILE = new File(RECOVERY_DIR, "log");
    private static String LAST_PREFIX = "last_";

    // Length limits for reading files.
    private static int LOG_FILE_MAX_LENGTH = 64 * 1024;

    /**
     * @hide
    */
    public static boolean isNeedWipeMedia = false; 

    /**
     * Interface definition for a callback to be invoked regularly as
     * verification proceeds.
     */
    public interface ProgressListener {
        /**
         * Called periodically as the verification progresses.
         *
         * @param progress  the approximate percentage of the
         *        verification that has been completed, ranging from 0
         *        to 100 (inclusive).
         */
        public void onProgress(int progress);
    }

    /** @return the set of certs that can be used to sign an OTA package. */
    private static HashSet<Certificate> getTrustedCerts(File keystore)
        throws IOException, GeneralSecurityException {
        HashSet<Certificate> trusted = new HashSet<Certificate>();
        if (keystore == null) {
            keystore = DEFAULT_KEYSTORE;
        }
        ZipFile zip = new ZipFile(keystore);
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                InputStream is = zip.getInputStream(entry);
                try {
                    trusted.add(cf.generateCertificate(is));
                } finally {
                    is.close();
                }
            }
        } finally {
            zip.close();
        }
        return trusted;
    }

    /**
     * Verify the cryptographic signature of a system update package
     * before installing it.  Note that the package is also verified
     * separately by the installer once the device is rebooted into
     * the recovery system.  This function will return only if the
     * package was successfully verified; otherwise it will throw an
     * exception.
     *
     * Verification of a package can take significant time, so this
     * function should not be called from a UI thread.  Interrupting
     * the thread while this function is in progress will result in a
     * SecurityException being thrown (and the thread's interrupt flag
     * will be cleared).
     *
     * @param packageFile  the package to be verified
     * @param listener     an object to receive periodic progress
     * updates as verification proceeds.  May be null.
     * @param deviceCertsZipFile  the zip file of certificates whose
     * public keys we will accept.  Verification succeeds if the
     * package is signed by the private key corresponding to any
     * public key in this file.  May be null to use the system default
     * file (currently "/system/etc/security/otacerts.zip").
     *
     * @throws IOException if there were any errors reading the
     * package or certs files.
     * @throws GeneralSecurityException if verification failed
     */
    public static void verifyPackage(File packageFile,
                                     ProgressListener listener,
                                     File deviceCertsZipFile)
        throws IOException, GeneralSecurityException {
        long fileLen = packageFile.length();

        RandomAccessFile raf = new RandomAccessFile(packageFile, "r");
        try {
            int lastPercent = 0;
            long lastPublishTime = System.currentTimeMillis();
            if (listener != null) {
                listener.onProgress(lastPercent);
            }

            raf.seek(fileLen - 6);
            byte[] footer = new byte[6];
            raf.readFully(footer);

            if (footer[2] != (byte)0xff || footer[3] != (byte)0xff) {
                throw new SignatureException("no signature in file (no footer)");
            }

            int commentSize = (footer[4] & 0xff) | ((footer[5] & 0xff) << 8);
            int signatureStart = (footer[0] & 0xff) | ((footer[1] & 0xff) << 8);

            byte[] eocd = new byte[commentSize + 22];
            raf.seek(fileLen - (commentSize + 22));
            raf.readFully(eocd);

            // Check that we have found the start of the
            // end-of-central-directory record.
            if (eocd[0] != (byte)0x50 || eocd[1] != (byte)0x4b ||
                eocd[2] != (byte)0x05 || eocd[3] != (byte)0x06) {
                throw new SignatureException("no signature in file (bad footer)");
            }

            for (int i = 4; i < eocd.length-3; ++i) {
                if (eocd[i  ] == (byte)0x50 && eocd[i+1] == (byte)0x4b &&
                    eocd[i+2] == (byte)0x05 && eocd[i+3] == (byte)0x06) {
                    throw new SignatureException("EOCD marker found after start of EOCD");
                }
            }

            // The following code is largely copied from
            // JarUtils.verifySignature().  We could just *call* that
            // method here if that function didn't read the entire
            // input (ie, the whole OTA package) into memory just to
            // compute its message digest.

            BerInputStream bis = new BerInputStream(
                new ByteArrayInputStream(eocd, commentSize+22-signatureStart, signatureStart));
            ContentInfo info = (ContentInfo)ContentInfo.ASN1.decode(bis);
            SignedData signedData = info.getSignedData();
            if (signedData == null) {
                throw new IOException("signedData is null");
            }
            Collection encCerts = signedData.getCertificates();
            if (encCerts.isEmpty()) {
                throw new IOException("encCerts is empty");
            }
            // Take the first certificate from the signature (packages
            // should contain only one).
            Iterator it = encCerts.iterator();
            X509Certificate cert = null;
            if (it.hasNext()) {
                cert = new X509CertImpl((org.apache.harmony.security.x509.Certificate)it.next());
            } else {
                throw new SignatureException("signature contains no certificates");
            }

            List sigInfos = signedData.getSignerInfos();
            SignerInfo sigInfo;
            if (!sigInfos.isEmpty()) {
                sigInfo = (SignerInfo)sigInfos.get(0);
            } else {
                throw new IOException("no signer infos!");
            }

            // Check that the public key of the certificate contained
            // in the package equals one of our trusted public keys.

            HashSet<Certificate> trusted = getTrustedCerts(
                deviceCertsZipFile == null ? DEFAULT_KEYSTORE : deviceCertsZipFile);

            PublicKey signatureKey = cert.getPublicKey();
            boolean verified = false;
            for (Certificate c : trusted) {
                if (c.getPublicKey().equals(signatureKey)) {
                    verified = true;
                    break;
                }
            }
            if (!verified) {
                throw new SignatureException("signature doesn't match any trusted key");
            }

            // The signature cert matches a trusted key.  Now verify that
            // the digest in the cert matches the actual file data.

            // The verifier in recovery only handles SHA1withRSA and
            // SHA256withRSA signatures.  SignApk chooses which to use
            // based on the signature algorithm of the cert:
            //
            //    "SHA256withRSA" cert -> "SHA256withRSA" signature
            //    "SHA1withRSA" cert   -> "SHA1withRSA" signature
            //    "MD5withRSA" cert    -> "SHA1withRSA" signature (for backwards compatibility)
            //    any other cert       -> SignApk fails
            //
            // Here we ignore whatever the cert says, and instead use
            // whatever algorithm is used by the signature.

            String da = sigInfo.getDigestAlgorithm();
            String dea = sigInfo.getDigestEncryptionAlgorithm();
            String alg = null;
            if (da == null || dea == null) {
                // fall back to the cert algorithm if the sig one
                // doesn't look right.
                alg = cert.getSigAlgName();
            } else {
                alg = da + "with" + dea;
            }
            Signature sig = Signature.getInstance(alg);
            sig.initVerify(cert);

            // The signature covers all of the OTA package except the
            // archive comment and its 2-byte length.
            long toRead = fileLen - commentSize - 2;
            long soFar = 0;
            raf.seek(0);
            byte[] buffer = new byte[4096];
            boolean interrupted = false;
            while (soFar < toRead) {
                interrupted = Thread.interrupted();
                if (interrupted) break;
                int size = buffer.length;
                if (soFar + size > toRead) {
                    size = (int)(toRead - soFar);
                }
                int read = raf.read(buffer, 0, size);
                sig.update(buffer, 0, read);
                soFar += read;

                if (listener != null) {
                    long now = System.currentTimeMillis();
                    int p = (int)(soFar * 100 / toRead);
                    if (p > lastPercent &&
                        now - lastPublishTime > PUBLISH_PROGRESS_INTERVAL_MS) {
                        lastPercent = p;
                        lastPublishTime = now;
                        listener.onProgress(lastPercent);
                    }
                }
            }
            if (listener != null) {
                listener.onProgress(100);
            }

            if (interrupted) {
                throw new SignatureException("verification was interrupted");
            }

            if (!sig.verify(sigInfo.getEncryptedDigest())) {
                throw new SignatureException("signature digest verification failed");
            }
        } finally {
            raf.close();
        }
    }

    public static String getSaveParttPath(){
        return new String("/data/media/0/");
    }

    /**
     * Reboots the device in order to install the given update
     * package.
     * Requires the {@link android.Manifest.permission#REBOOT} permission.
     *
     * @param context      the Context to use
     * @param packageFile  the update package to install.  Must be on
     * a partition mountable by recovery.  (The set of partitions
     * known to recovery may vary from device to device.  Generally,
     * /cache and /data are safe.)
     *
     * @throws IOException  if writing the recovery command file
     * fails, or if the reboot itself fails.
     */
    private static String getInstallPackageArg(Context context ,File packageFile)
            throws IOException {
        String filename = packageFile.getCanonicalPath();
        String strExt2Path = Environment.getExternalStorage2Directory().toString();
        String arg = null;

        writeSysfs("/sys/class/amhdmitx/amhdmitx0/cec_config" , "0");
        Log.i(TAG, "disable all cec feature while updating system");

        if(filename.startsWith(strExt2Path)) {
            if(Environment.isExternalStorageBeSdcard()) {
                String newpath = filename.substring(4); 
                Log.w(TAG, "!!! REBOOTING TO INSTALL 1 " + newpath + " !!!");
                arg = "--update_package=" + newpath;

        String ibEnableStatus = SystemProperties.get("persist.sys.instaboot.enable");
        if ("enable".equals(ibEnableStatus) || "prepare".equals(ibEnableStatus)) {
            Log.w(TAG, "clean instaboot image");
            InstabootManager im = new InstabootManager(context);
            im.disable();
        }
                arg += "\n--locale=" + Locale.getDefault().toString();
            } else {
                String newpath = new String("/sdcard") + filename.substring(strExt2Path.length()); 
                Log.w(TAG, "!!! REBOOTING TO INSTALL 2 " + newpath + " !!!");
                arg = "--update_package=" + newpath;
                arg += "\n--locale=" + Locale.getDefault().toString();
            }
        } else if(filename.startsWith(Environment.getExternalStorageDirectory().toString())) {
            if(Environment.isExternalStorageBeSdcard()) {
                String absPath = packageFile.getAbsolutePath();
                if(SystemProperties.getInt("vold.fakesdcard.enable",0)==1 && absPath.startsWith("/mnt/sda1/")) {
                    String newpath =new String("/udisk/")+absPath.substring(10); 
                    Log.w(TAG, "!!! REBOOTING TO INSTALL 3-1 " + newpath + " !!!");
                    arg = "--update_package=" + newpath;
                    arg += "\n--locale=" + Locale.getDefault().toString();
                } else {
                    String newpath = filename.substring(4); 
                    Log.w(TAG, "!!! REBOOTING TO INSTALL 3-2 " + newpath + " !!!");
                    arg = "--update_package=" + newpath;
                    arg += "\n--locale=" + Locale.getDefault().toString();
                }
            } else {
                String newpath = new String("/media/" + packageFile.getName()); 
                Log.w(TAG, "!!! REBOOTING TO INSTALL 4 " + newpath + " !!!");
                arg = "--update_package=" + newpath;
                arg += "\n--locale=" + Locale.getDefault().toString();
            }
        } else if(filename.startsWith(Environment.getInternalStorageDirectory().toString())) {
            String newpath = new String("/media/"+packageFile.getName()); 
            Log.w(TAG, "!!! REBOOTING TO INSTALL 5 " + newpath + " !!!");
            arg = "--update_package=" + newpath;
            arg += "\n--locale=" + Locale.getDefault().toString();
        } else if(filename.startsWith("/udisk")) {
            String newpath =new String("/udisk/")+filename.substring(7);
            Log.w(TAG, "!!! REBOOTING TO INSTALL 6 " + newpath + " !!!");
            arg = "--update_package=" + newpath;
            arg += "\n--locale=" + Locale.getDefault().toString();
        } else {
            Log.w(TAG, "!!! REBOOTING TO INSTALL 7 " + filename + " !!!");
            arg = "--update_package=" + filename;
            arg += "\n--locale=" + Locale.getDefault().toString();
        }
        return arg;
    }

    public static void installPackage(Context context, File packageFile, Boolean reboot)
            throws IOException {
        //begin:add by zhanghk at20190813:copy update.zip to bootfiles 
        if(SystemProperties.get("ro.ysten.province").contains("jiangsu")){
            copyFiles(packageFile.toString(),"/bootfiles/update.zip");
        }
        //end:add by zhanghk at20190813:copy update.zip to bootfiles
				
        String arg = getInstallPackageArg(context, packageFile);
        bootCommand(context, arg, false, reboot);
    }

    private static boolean copyFiles(String srcFile,String dstFile) {
        Log.i(TAG,"copyFiles start ");
        int bytesum = 0;
        int byteread = 0;

        File oldfile = new File(srcFile);
        File newfile = new File(dstFile);
        
        if(!oldfile.exists()){
            Log.i(TAG,"copyFiles srcFile not exist " + srcFile);
            return false;
        }
        
        if(newfile.exists()){
            newfile.delete();
        }
        
        if (oldfile.exists()) {
            InputStream inStream = null;
            FileOutputStream fs = null;
            try{
                File desfile = new File(dstFile);
                //desfile.createNewFile();
                inStream = new FileInputStream(srcFile);
                fs = new FileOutputStream(dstFile);
                byte[] buffer = new byte[10240];
                int length;
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread;
                    fs.write(buffer, 0, byteread);
                }
                if(inStream!=null)
                    inStream.close();
                if(fs!=null)
                    fs.close();
            }catch(Exception e){
                e.printStackTrace();
                Log.i(TAG,"copyFiles exception "+e.toString());
                try {
                    if(inStream!=null)
                        inStream.close();
                    if(fs!=null)
                        fs.close();   
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return false;
            }
            Log.i(TAG,"copyFiles succeed "); 
			try{
			    Process process = Runtime.getRuntime().exec("sync");	
			}catch(Exception e){
			    e.printStackTrace();
			}
            return true;
        }
		
        Log.i(TAG,"copyFiles end ");
        return false;

    }
	
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

    public static void installPackage(Context context, File packageFile)
            throws IOException {
        String arg = getInstallPackageArg(context, packageFile);
        bootCommand(context, arg, true);
    }

    /**
     * Reboots the device and wipes the user data partition.  This is
     * sometimes called a "factory reset", which is something of a
     * misnomer because the system partition is not restored to its
     * factory state.
     * Requires the {@link android.Manifest.permission#REBOOT} permission.
     *
     * @param context  the Context to use
     *
     * @throws IOException  if writing the recovery command file
     * fails, or if the reboot itself fails.
     */
    public static void rebootWipeUserData(Context context) throws IOException {
        final ConditionVariable condition = new ConditionVariable();

        Intent intent = new Intent("android.intent.action.MASTER_CLEAR_NOTIFICATION");
        context.sendOrderedBroadcastAsUser(intent, UserHandle.OWNER,
                android.Manifest.permission.MASTER_CLEAR,
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        condition.open();
                    }
                }, null, 0, null, null);

        // Block until the ordered broadcast has completed.
        condition.block();
        Boolean hasMassStorage = SystemProperties.getBoolean("ro.has.mass.storage",false);
        //force enter recovery for uboot even poweroff when rebooting
        //begin:add by zhanghk at 20181112:backup pppoe config and restore it after factory reset
        if(SystemProperties.get("ro.ysten.province","master").equals("CM201_jiangsu")){
                write_keep_net_state(context);
        }
        //end:add by zhanghk at 20181112:backup pppoe config and restore it after factory reset
        SystemProperties.set("ubootenv.var.wipe_data", "failed");
        Log.w(TAG, "!!! set ubootenv.var.wipe_data to failed!!!\n");
        writeSysfs("/sys/class/amhdmitx/amhdmitx0/cec_config" , "0");
        Log.i(TAG, "disable all cec feature while reset to factory");
        if(isNeedWipeMedia && hasMassStorage){
            bootCommand(context, "--wipe_data\n--wipe_media\n--locale=" + Locale.getDefault().toString(), false);
            isNeedWipeMedia = false;
        }else{
            bootCommand(context, "--wipe_data\n--locale=" + Locale.getDefault().toString(), false);
        }
    }

    public static void rebootWipeUserDataKeepData(Context context) throws IOException {
        final ConditionVariable condition = new ConditionVariable();

        Intent intent = new Intent("android.intent.action.MASTER_CLEAR_NOTIFICATION");
        context.sendOrderedBroadcastAsUser(intent, UserHandle.OWNER,
                android.Manifest.permission.MASTER_CLEAR,
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        condition.open();
                    }
                }, null, 0, null, null);

        // Block until the ordered broadcast has completed.
        condition.block();
        Boolean hasMassStorage = SystemProperties.getBoolean("ro.has.mass.storage",false);
        //force enter recovery for uboot even poweroff when rebooting
  //      SystemProperties.set("ubootenv.var.wipe_data", "failed");
        Log.w(TAG, "!!! set ubootenv.var.wipe_data to failed!!!\n");
        writeSysfs("/sys/class/amhdmitx/amhdmitx0/cec_config" , "0");
        Log.i(TAG, "disable all cec feature while reset to factory");
        if(isNeedWipeMedia && hasMassStorage){
            bootCommand(context, "--wipe_data_with_keep\n--wipe_media\n--locale=" + Locale.getDefault().toString(), false);
            isNeedWipeMedia = false;
        }else{
            bootCommand(context, "--wipe_data_with_keep\n--locale=" + Locale.getDefault().toString(), false);
        }
    }

	
    /**
     * Reboot into the recovery system to wipe the /cache partition.
     * @throws IOException if something goes wrong.
     */
    public static void rebootWipeCache(Context context) throws IOException {
        bootCommand(context, "--wipe_cache\n--locale=" + Locale.getDefault().toString(), false);
    }

    public static void rebootRestoreSystem(Context context) throws IOException {
        bootCommand(context, "--restore_system\n--locale=" + Locale.getDefault().toString(), false);
    }

    public static void rebootWipeUserData_Iptv(Context context, String path) {
        if(path == null) {
            Log.w(TAG, "File path is null");
            return;
        }
        Log.d(TAG, "rebootWipeUserData_Iptv, path: " + path);
        try {
            bootCommand(context, "--factoryreset_keepfile=" + path, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void userDataRestore_Iptv(Context context, String path) {
        if(path == null) {
            Log.w(TAG, "File path is null");
            return;
        }
        Log.d(TAG, "userDataRestore_Iptv, path: " + path);
        File sourceFile = new File("/data/tmp");
        File outFile = new File(path);
        if(!fileChannelCopy(sourceFile, outFile)) {
            Log.w(TAG, "Copy /data/tmp file to " + path + " failed");
        }
    }
    
    private static boolean fileChannelCopy(File s, File t) {
        if((s == null) || !s.isFile() || !s.exists()) {
            Log.w(TAG, "fileChannelCopy source file is error, please check it is exist or this is not file!");
            return false;
        }
        FileInputStream fi = null;
        FileOutputStream fo = null;
        FileChannel in = null;
        FileChannel out = null;
        try {
            fi = new FileInputStream(s);
            fo = new FileOutputStream(t);
            /*byte[] buffer = new byte[2048];
            int byteread = 0;
            while ( (byteread = fi.read(buffer)) != -1) {
                fo.write(buffer, 0, byteread);
            }*/
            in = fi.getChannel();
            out = fo.getChannel();
            in.transferTo(0, in.size(), out);
            fi.close();
            in.close();
            fo.close();
            out.close();
            s.delete();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Reboot into the recovery system with the supplied argument.
     * @param arg to pass to the recovery utility.
     * @throws IOException if something goes wrong.
     */
    private static void bootCommand(Context context, String arg, Boolean update, Boolean reboot)
            throws IOException {
        RECOVERY_DIR.mkdirs();  // In case we need it
        COMMAND_FILE.delete();  // In case it's not writable
        LOG_FILE.delete();
        
        FileWriter command = new FileWriter(COMMAND_FILE);
        FileOutputStream fos = new FileOutputStream(COMMAND_FILE);
        try {
            command.write(arg);
            command.write("\n");
        } finally {
            command.close();
            FileUtils.sync(fos);
        }

        if(!reboot) {
            Process pid = Runtime.getRuntime().exec("sync");
            if (pid != null) {
                try {
                    pid.waitFor();
                } catch (InterruptedException unused) {
                }
            }
            SystemProperties.set("ubootenv.var.upgrade_step", "3");
        }
        // Having written the command file, go ahead and reboot
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if(update)
            pm.reboot("update");
        else if(reboot)
            pm.reboot("recovery");
        else {
            Log.d(TAG, "user not reboot when has update zip");
            return;
        }

        throw new IOException("Reboot failed (no permissions?)");
    }

    private static void bootCommand(Context context, String arg, Boolean update) throws IOException {
        RECOVERY_DIR.mkdirs();  // In case we need it
        COMMAND_FILE.delete();  // In case it's not writable
        LOG_FILE.delete();

        FileWriter command = new FileWriter(COMMAND_FILE);
        FileOutputStream fos = new FileOutputStream(COMMAND_FILE);
        try {
            command.write(arg);
            command.write("\n");
        } finally {
            command.close();
            FileUtils.sync(fos);
        }
        if(update){
            Process pid = Runtime.getRuntime().exec("sync");
            if (pid != null) {
                try {
                    pid.waitFor();
                } catch (InterruptedException unused) {
                }
            }
            SystemProperties.set(SIGN_UPDATE_UPGRADE_STEP, "3");
        }
        // Having written the command file, go ahead and reboot
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if(update)
            pm.reboot("update");
        else 
            pm.reboot("recovery");

        throw new IOException("Reboot failed (no permissions?)");
    }

    /**
     * Called after booting to process and remove recovery-related files.
     * @return the log file from recovery, or null if none was found.
     *
     * @hide
     */
    public static String handleAftermath() {
        // Record the tail of the LOG_FILE
        String log = null;
        try {
            log = FileUtils.readTextFile(LOG_FILE, -LOG_FILE_MAX_LENGTH, "...\n");
        } catch (FileNotFoundException e) {
            Log.i(TAG, "No recovery log file");
        } catch (IOException e) {
            Log.e(TAG, "Error reading recovery log", e);
        }

        // Delete everything in RECOVERY_DIR except those beginning
        // with LAST_PREFIX
        String proj_md = SystemProperties.get("sys.proj.middleware", null);
        if ("sy".equals(proj_md)) {
            Log.w(TAG, "Do not delete files from " + RECOVERY_DIR);
            return log;
        }
        String[] names = RECOVERY_DIR.list();
        for (int i = 0; names != null && i < names.length; i++) {
            if (names[i].startsWith(LAST_PREFIX)) continue;
            if (names[i].contains("command")) continue; //delete it earlier , when boot_complete was send immediately
            File f = new File(RECOVERY_DIR, names[i]);
            if (!f.delete()) {
                Log.e(TAG, "Can't delete: " + f);
            } else {
                Log.i(TAG, "Deleted: " + f);
            }
        }

        return log;
    }

    private void RecoverySystem() { }  // Do not instantiate
	
    
	private static void file_backup(final String s_path) {
		//String old_path_ipoe = "/data/misc/etc/eth_auth.conf";
		//String old_path_pppoe = "/data/misc/etc/ppp/eth_padt_bin";
		//String old_path_pppoe = "/data/misc/etc/ppp/wlan_padt_bin";
	
		String old_path = s_path;
		String new_path = "/params/eth_auth.conf";
		
		File f_old = new File(old_path);
		if(f_old.exists()) {
			File f_new = new File(new_path);
			if(f_new.exists())
				f_new.delete();
			
			try {
				FileInputStream fis = new FileInputStream(f_old);
				FileOutputStream fos = new FileOutputStream(f_new);
				byte[] buf = new byte[1024];
				int byteread = 0;
				while((byteread = fis.read(buf)) != -1) {
					fos.write(buf, 0, byteread);
				}
				
				fis.close();
				fos.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}

	private static void write_keep_net_state(Context context) {
		
		Slog.w(TAG, "write_keep_net_state()");
		
		String keep_path = "/params/keepnet";

		String ipoe_eth_auth_path = "/data/misc/etc/eth_auth.conf";
		String pppoe_eth_padt_path = "/data/misc/etc/ppp/eth_padt_bin";
		String pppoe_wlan_padt_path = "/data/misc/etc/ppp/wlan_padt_bin";
		
		File f_keep = new File(keep_path);
		
		if(f_keep.exists()){
			f_keep.delete();
			Slog.w(TAG, "deleted /params/keepnet ");
		}
		
		try {
			ContentResolver cr = context.getContentResolver();
			int eth_conf = Settings.Secure.getInt(cr, Settings.Secure.ETH_CONF, 1);
			String eth_mode = Settings.Secure.getString(cr, Settings.Secure.ETH_MODE);

			Slog.w(TAG, "get settings.secure : eth_conf = " + eth_conf + "; eth_mode = " + eth_mode);
            //add by zhanghk at 20181113:fix after reset dhcp cannot connect problem  
			//eth_mode = EthernetDevInfo.ETH_CONN_MODE_PPPOE;
			StringBuffer strbuf = new StringBuffer();
			if(eth_mode.equals(EthernetDevInfo.ETH_CONN_MODE_MANUAL)) {
				String eth_ifname = Settings.Secure.getString(cr, Settings.Secure.ETH_IFNAME);
				String eth_ip = Settings.Secure.getString(cr, Settings.Secure.ETH_IP);
				String eth_mask = Settings.Secure.getString(cr, Settings.Secure.ETH_MASK);
				String eth_dns1 = Settings.Secure.getString(cr, Settings.Secure.ETH_DNS1);
				String eth_dns2 = Settings.Secure.getString(cr, Settings.Secure.ETH_DNS2);
				String eth_route = Settings.Secure.getString(cr, Settings.Secure.ETH_ROUTE);

				strbuf.append("eth_conf").append("=").append(eth_conf).append("\n");
				strbuf.append("eth_mode").append("=").append(eth_mode).append("\n");
				strbuf.append("eth_ifname").append("=").append(eth_ifname).append("\n");
				strbuf.append("eth_ip").append("=").append(eth_ip).append("\n");
				strbuf.append("eth_mask").append("=").append(eth_mask).append("\n");
				strbuf.append("eth_dns1").append("=").append(eth_dns1).append("\n");
				strbuf.append("eth_dns2").append("=").append(eth_dns2).append("\n");
				strbuf.append("eth_route").append("=").append(eth_route).append("\n");

			} else if(eth_mode.equals(EthernetDevInfo.ETH_CONN_MODE_PPPOE)) {
				String pppoe_usr = Settings.Secure.getString(cr, Settings.Secure.PPPOE_USR);
				String pppoe_pwd = Settings.Secure.getString(cr, Settings.Secure.PPPOE_PWD);
				String pppoe_itf = Settings.Secure.getString(cr, Settings.Secure.PPPOE_ITF);
				String pppoe_autod = Settings.Secure.getString(cr, Settings.Secure.PPPOE_AUTOD);
//china mobile setting start
				String pppoe_usr1 = Settings.Secure.getString(cr, "pppoe_username");
                                String pppoe_pwd1 = Settings.Secure.getString(cr, "pppoe_pswd");
//china mobile setting end
				strbuf.append("eth_conf").append("=").append(eth_conf).append("\n");
				strbuf.append("eth_mode").append("=").append(eth_mode).append("\n");
				strbuf.append("pppoe_usr").append("=").append(pppoe_usr).append("\n");
				strbuf.append("pppoe_pwd").append("=").append(pppoe_pwd).append("\n");
				strbuf.append("pppoe_itf").append("=").append(pppoe_itf).append("\n");
				strbuf.append("pppoe_autod").append("=").append(pppoe_autod).append("\n");
				strbuf.append("pppoe_1usr").append("=").append(pppoe_usr1).append("\n");
                                strbuf.append("pppoe_1pwd").append("=").append(pppoe_pwd1).append("\n");

				file_backup(pppoe_eth_padt_path);
				file_backup(pppoe_wlan_padt_path);
				
			} else if(eth_mode.equals(EthernetDevInfo.ETH_CONN_MODE_DHCP_AUTH)) {

				strbuf.append("eth_conf").append("=").append(eth_conf).append("\n");
				strbuf.append("eth_mode").append("=").append(eth_mode).append("\n");
				file_backup(ipoe_eth_auth_path);
			}

			if(!eth_mode.equals(EthernetDevInfo.ETH_CONN_MODE_DHCP)) {
				
				f_keep.createNewFile();
				FileWriter fw = new FileWriter(keep_path);
				fw.write(strbuf.toString());
				fw.flush();
				fw.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
}
