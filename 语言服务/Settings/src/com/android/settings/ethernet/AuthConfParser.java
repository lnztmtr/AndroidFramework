package com.android.settings.ethernet;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class AuthConfParser {
    private static final String TAG = "AuthConfParser";
    public static final String AUTH_DHCP_CONF = "/data/misc/dhcp/auth.conf";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String VENDORID = "local_vendorclassid";
    public static final String VENDORID_DEFAULT = "CTCIPTVDHCPAAA";
    public static final String USERNAME_SUFFIX = "@itv";
    public String mUserName = null;
    public String mPassword = null;
    public String mVendorId = null;
    private File conf = new File(AUTH_DHCP_CONF);

    public AuthConfParser() {

    }

    public boolean isAuthDhcp() {
        if(conf.exists())
            return true;
        else
            return false;
    }

    public void deleteAuthConf() {
        if(isAuthDhcp()) {
            conf.delete();
        }
    }

    public void saveAuthConf(String usr, String pwd, String vendorid) {
        Log.d(TAG, "saveAuthConf username: " + usr 
                + " password: " + pwd
                + " vendorid: " + vendorid);

        if(usr == null)
            usr = USERNAME_SUFFIX;
        if(pwd == null)
            pwd = "";
        if(vendorid == null)
            vendorid = VENDORID_DEFAULT;
        deleteAuthConf();
        try {
            //conf.createNewFile();
            conf.setReadable(true, false);
            BufferedWriter confBufWriter = new BufferedWriter(new FileWriter(AUTH_DHCP_CONF));
            confBufWriter.write(USERNAME + " \"" + usr +  USERNAME_SUFFIX + "\"");
            confBufWriter.newLine();
            confBufWriter.write(PASSWORD + " \"" + pwd +  "\"");
            confBufWriter.newLine();
            confBufWriter.write(VENDORID + " \"" + vendorid +  "\"");
            confBufWriter.flush();
            confBufWriter.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void parserConf() {
        try {
            BufferedReader confBufReader = new BufferedReader(new FileReader(AUTH_DHCP_CONF));
            while(true) {
                String line = confBufReader.readLine();
                Log.d(TAG, "parserConf line: " + line);
                if(line == null) {
                    Log.d(TAG, "parserConf end!");
                    break;
                }
                if(line.startsWith(USERNAME)) {
                    String[] cells = line.split("\"");
                    if(cells.length >= 2) {
                        mUserName = cells[1];
                        mUserName = mUserName.replace(USERNAME_SUFFIX,"");
                        Log.d(TAG, "parserConf " + USERNAME + ": " + mUserName);
                    }
                } else if(line.startsWith(PASSWORD)) {
                    String[] cells = line.split("\"");
                    if(cells.length >= 2) {
                        mPassword = cells[1];
                        Log.d(TAG, "parserConf " + PASSWORD + ": " + mPassword);
                    }
                } else if(line.startsWith(VENDORID)) {
                    String[] cells = line.split("\"");
                    if(cells.length >= 2) {
                        mVendorId = cells[1];
                        Log.d(TAG, "parserConf " + VENDORID + ": " + mVendorId);
                    }
                }
            }
            confBufReader.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
