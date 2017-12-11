package com.avariohome.avario.mqtt;


import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import org.json.JSONException;
import org.json.JSONObject;


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
        connection = new MqttConnection(
                context,
                macAddress,
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
