package com.avariohome.avario.util;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.exception.AvarioException;

import org.json.JSONArray;
import org.json.JSONException;

public class Connectivity {
    private static volatile Connectivity instance;
    public String[] lanMac;
    private Boolean isLan = false;
    public String myMacAddress = "";

    private Connectivity() {

    }

    public static Connectivity getInstance() {
        if (instance == null) {
            synchronized (Connectivity.class) {
                if (instance == null) {
                    instance = new Connectivity();
                }
            }
        }
        return instance;
    }

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

    public static boolean identifyConnection(Context context) {
        JSONArray lanMacList;
        boolean result = false;
        try {
            lanMacList = StateArray.getInstance().getLanMacList();
            if (lanMacList != null) {
                String accessPointMac = Connectivity.getAccessPointMac(context);
                for (int i = 0; i < lanMacList.length(); i++) {
                    if (accessPointMac.equals(lanMacList.getString(i))) {
                        result = true;
                        break;
                    }
                }
            }
        } catch (AvarioException | JSONException | NullPointerException e) {
            e.printStackTrace();
        }
        getInstance().isLan = result;
        return result;
    }

    public static boolean isConnectedToLan() {
        return getInstance().isLan;
    }
}
