package com.tv.avario.util;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.tv.avario.core.StateArray;
import com.tv.avario.exception.AvarioException;

import org.json.JSONArray;
import org.json.JSONException;

public class Connectivity {
    private static volatile Connectivity instance;
    public String[] lanMac;
    private Boolean isLan = false;
    public String myMacAddress = "";

    private Connectivity() {

    }

    /**
     * To ensure that there is ony a single instance.
     *
     * @return instance;
     */
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

    /**
     * Get router mac address.
     *
     * @param context Application context.
     * @return return router mac address.
     */
    public static String getAccessPointMac(Context context) {
        WifiInfo wifiInfo = Connectivity.getWifiInfo(context);
        return wifiInfo.getBSSID();
    }

    /**
     * Get device mac address.
     *
     * @param context application context.
     * @return device mac address.
     */
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

    /**
     * Check if mac address from state array is similar to device mac address.
     *
     * @param context applicaiton context.
     * @return boolean.
     */
    public static boolean identifyConnection(Context context) {
        JSONArray lanMacList;
        boolean result = false;
        try {
            lanMacList = StateArray.getInstance().getLanMacList();
            if (lanMacList != null) {
                String accessPointMac = Connectivity.getAccessPointMac(context);
                for (int i = 0; i < lanMacList.length(); i++) {
                    if (accessPointMac != null) {
                        if (accessPointMac.equals(lanMacList.getString(i))) {
                            result = true;
                            break;
                        }
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
        return true;
    }
}
