package com.tv.avario.mqtt;


import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.tv.avario.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;


/**
 * Created by aeroheart-c6 on 15/01/2017.
 */
public class MqttManager {
    public static final String TAG = "Avario/Mqtt";

    private static final MqttManager instance = new MqttManager();

    public static MqttManager getInstance() {
        return MqttManager.instance;
    }

    public static MqttConnection createConnection(Context context, JSONObject mqttJSON) throws JSONException {
        MqttConnection connection;
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        String macAddress = wInfo.getMacAddress();
        Log.d(TAG, getMacAddr());
        connection = new MqttConnection(
                context,
                getMacAddr(),
                mqttJSON.getString("host"),
                mqttJSON.getString("port"),
                mqttJSON.getBoolean("ssl")
        );

        connection
                .setSubscribeTopic(mqttJSON.getString("topic_sub"))
                .setPublishTopic(mqttJSON.getString("topic_pub"))
                .setKeepAlive(mqttJSON.getInt("keepalive"))
                .setAuthentication(mqttJSON.getString("username"), mqttJSON.getString("password"));

        return connection;
    }

    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "02:00:00:00:00:00";
    }

    public static void updateConnection(MqttConnection connection, JSONObject mqttJSON) throws JSONException {
        String host, port, username, password;
        boolean ssl;
        host = mqttJSON.getString("host");
        port = mqttJSON.getString("port");
        ssl = mqttJSON.getBoolean("ssl");
        username = mqttJSON.getString("username");
        password = mqttJSON.getString("password");
        connection
                .setHost(host)
                .setPort(port)
                .setSSL(ssl)
                .setSubscribeTopic(mqttJSON.getString("topic_sub"))
                .setPublishTopic(mqttJSON.getString("topic_pub"))
                .setKeepAlive(mqttJSON.getInt("keepalive"))
                .setAuthentication(username, password);
    }


    private MqttConnection connection;

    private MqttManager() {
        super();
        this.connection = null;
    }

    public boolean isConnected() {
        return this.connection != null
                && this.connection.getStatus() == MqttConnection.Status.CONNECTED;
    }

    public void setConnection(MqttConnection connection) {
        this.connection = connection;
    }

    public MqttConnection getConnection() {
        return this.connection;
    }
}
