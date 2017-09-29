package com.avariohome.avario.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.*;
import android.util.Log;

import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.exception.AvarioException;
import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Connectivity {
    public String[] lanMac;
    @SerializedName("lan")
    public Credentials lan;
    @SerializedName("wan")
    public Credentials wan;

    public static String getAccessPointMac(Context context) {
        WifiInfo wifiInfo = Connectivity.getWifiInfo(context);

        return wifiInfo.getBSSID();
    }

    public static String getMacAddress(Context context) {
        WifiInfo wifiInfo = Connectivity.getWifiInfo(context);

        return wifiInfo.getMacAddress();
    }

    /**
     * Get the wifi info
     */
    private static WifiInfo getWifiInfo(Context context) {
        WifiManager wifiMan = (WifiManager) context.getApplicationContext().getSystemService(
                Context.WIFI_SERVICE);
        return wifiMan.getConnectionInfo();
    }

    public static boolean isMacPresent(Context context) {
        JSONArray lanMacList;
        boolean result = false;
        try {
            lanMacList = StateArray.getInstance().getLanMacList();
            String accessPointMac = Connectivity.getAccessPointMac(context);
            for (int i = 0; i < lanMacList.length(); i++) {
                if (accessPointMac.equals(lanMacList.getString(i))) {
                    result = true;
                    break;
                }
            }
            Connectivity connectivity = StateArray.getInstance().getConnectivityDetails();
            for (String item : connectivity.lanMac) {
                Log.v("Connectivity", item);
            }
        } catch (AvarioException | JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    public class Credentials {
        public String host;
        public String port;
        public boolean ssl;
        public String username;
        public String password;
    }
}
