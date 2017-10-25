package com.avariohome.avario.mqtt;

import android.util.Log;

import com.avariohome.avario.Constants;
import com.avariohome.avario.activity.MainActivity;
import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.util.Connectivity;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by aeroheart-c6 on 7/12/17.
 */

class MqttMessageParser implements Runnable {
    private MqttMessage message;

    public MqttMessageParser(MqttMessage message) {
        this.message = message;
    }

    @Override
    public void run() {
        StateArray states = StateArray.getInstance();
        JSONObject payloadJSON;
        String type;

        try {
            payloadJSON = new JSONObject(this.message.toString());
            type = payloadJSON.getString("event_type");
        }
        catch (JSONException exception) {
            return;
        }

        // TODO will become more complicated. Should probably refactor..?
        switch (type) {
            case Constants.MQTT_EVENT_TYPE_STATE_CHANGED:
                try {
                    states.broadcastChanges(states.updateFromMQTT(payloadJSON), StateArray.FROM_MQTT);
                } catch (AvarioException ignored) {
                }
                break;
            case Constants.MQTT_EVENT_TYPE_ROOM_CHANGED:
                Log.v("BootstrapChange", "received event data " + payloadJSON);
                states.broadcastRoomChange(payloadJSON);
                break;
            case Constants.MQTT_EVENT_TYPE_BOOTSTRAP_CHANGED:
                Log.v("BootstrapChange", "received event data " + payloadJSON);
                states.broadcastBootstrapChange(payloadJSON);
                break;
        }
    }
}
