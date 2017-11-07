package com.avariohome.avario.core;


import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;

import com.avariohome.avario.Application;
import com.avariohome.avario.Constants;
import com.avariohome.avario.R;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.util.Connectivity;
import com.avariohome.avario.util.Log;
import com.avariohome.avario.util.PlatformUtil;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;


/**
 * Created by aeroheart-c6 on 10/01/2017.
 */
public class StateArray {
    public static final String TAG = "Avario/StateArray";
    public static final int FROM_MQTT = 1;
    public static final int FROM_HTTP = 2;
    public static final int FROM_TIMER = 4;

    private static StateArray instance = null;

    /**
     * Returns the StateArray instance regardless if it was instatiated or not. Great for parts of
     * the app where it is assumed that the instance is already properly instantiated.
     *
     * @return instance of the StateArray singleton or null because it wasn't initialized yet.
     */
    public static StateArray getInstance() {
        return StateArray.instance;
    }

    /**
     * Attempts to load the StateArray from a file in the filesystem.  It is important to pass the
     * application context here because this object will be alive throughout the duration of the
     * app.
     *
     * @param context the application context
     * @return instance of the StateArray singleton or null if the filesystem cache does not exist
     * yet
     */
    public static StateArray getInstance(Context context) {
        if (StateArray.instance == null)
            StateArray.instance = new StateArray(context);

        return StateArray.instance;
    }


    private Context context;
    private LocalBroadcastManager broadcaster;

    private JSONObject data;
    private Handler handler;
    private boolean dirty;
    private boolean refreshing;

    public boolean tempReboot = false;

    private StateArray(Context context) {
        this.broadcaster = LocalBroadcastManager.getInstance(context);
        this.context = context;
        this.data = null;
        this.dirty = false;
        this.refreshing = false;
        this.handler = Application.mainHandler;
    }

    /*
     ***********************************************************************************************
     * Utility Methods
     ***********************************************************************************************
     */
    public StateArray broadcastChanges(final String entityId, final int from) {
        // ensure that this is always run in the main thread
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent()
                        .setAction(Constants.BROADCAST_STATE_CHANGED)
                        .putExtra("entity_id", entityId)
                        .putExtra("from", from);

                Log.d(TAG, "Broadcasting state change!");
                StateArray.this.broadcaster.sendBroadcast(intent);
            }
        });

        return this;
    }

    public StateArray broadcastChanges() {
        return this.broadcastChanges(null, StateArray.FROM_MQTT);
    }

    public StateArray broadcastRoomChange(final JSONObject payload) {
        final String roomId;

        try {
            // TODO finalize
            roomId = payload
                    .getJSONObject("event_data")
                    .getString("entity_id");

            if (!BluetoothScanner.getInstance().isEnabled() ||
                    this.getRoom(roomId).optBoolean("active", false))
                return this;

            try {
                if (!payload.getJSONObject("event_data").getString("tablet_id")
                        .equals(PlatformUtil.getTabletId()))
                    return this;
            } catch (JSONException exception) {
                return this;
            }
        } catch (AvarioException exception) {
            return this;
        } catch (JSONException exception) {
            return this;
        }

        this.handler.post(new Runnable() {
            @Override
            public void run() {

                Intent intent = new Intent()
                        .setAction(Constants.BROADCAST_ROOM_CHANGED)
                        .putExtra("entity_id", roomId);

                Log.d(TAG, "Broadcasting room change!");
                StateArray.this.broadcaster.sendBroadcast(intent);
            }
        });

        return this;
    }

    /**
     * Parse {"event_type": "bootstrap_changed","event_data": {"tablet_id": "00:11:22:333:44:55"}} payload
     *
     * @param payloadJSON
     */
    public StateArray broadcastBootstrapChange(final JSONObject payloadJSON) {
        try {
            JSONArray tabletIdList = payloadJSON.getJSONObject("event_data").
                    getJSONArray("tablet_id");

            String deviceId = PlatformUtil.getTabletId();
            for (int i = 0; i < tabletIdList.length(); i++) {
                if (tabletIdList.get(i).toString().equalsIgnoreCase(deviceId)) {
                    this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent()
                                    .setAction(Constants.BROADCAST_BOOTSTRAP_CHANGED);
                            try {
                                intent.putExtra("bs_name", payloadJSON.getJSONObject("event_data")
                                        .getString("bs_name"));
                                intent.putExtra("reboot", payloadJSON.getJSONObject("event_data")
                                        .getBoolean("reboot"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Log.d(TAG, "Broadcasting bootstrap change!");
                            StateArray.this.broadcaster.sendBroadcast(intent);
                        }
                    });
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    public StateArray setData(JSONObject data) {
        this.data = data;
        this.dirty = true;

        return this;
    }

    public boolean hasData() {
        return this.data != null;
    }

    public StateArray unsetStateData() {
        Log.i(TAG, "Unsetting state data");

        // entities & media
        String[] properties = new String[]{
                "entities",
                "media",
                "dial_buttons",
        };

        for (String property : properties) {
            JSONObject entitiesJSON;
            Iterator<String> entities;

            try {
                entitiesJSON = this.data.getJSONObject(property);
            } catch (JSONException exception) {
                continue;
            }

            entities = entitiesJSON.keys();

            while (entities.hasNext()) {
                String entityId = entities.next();
                ;
                JSONObject entityJSON;

                try {
                    entityJSON = entitiesJSON.getJSONObject(entityId);

                    if (entityJSON.has("new_state"))
                        entityJSON.put("new_state", new JSONObject());

                    if (entityJSON.has("old_state"))
                        entityJSON.put("old_state", new JSONObject());
                } catch (JSONException exception) {
                }
            }
        }

        this.dirty = true;
        return this;
    }

    public boolean isRefreshing() {
        return this.refreshing;
    }


    /*
     ***********************************************************************************************
     * JSON StateData
     ***********************************************************************************************
     */
    public JSONObject getEntity(String entityId) throws AvarioException {
        if (!this.hasData())
            return null;

        try {
            return this.data
                    .getJSONObject("entities")
                    .getJSONObject(entityId);
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{
                            String.format("entities.%s", entityId)
                    }
            );
        }
    }

    public JSONObject getEntities() throws AvarioException {
        if (!this.hasData())
            return null;

        try {
            return this.data.getJSONObject("entities");
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"entities"}
            );
        }
    }

    public JSONObject getMediaEntity(String entityId) throws AvarioException {
        if (!this.hasData())
            return null;
        android.util.Log.d("Entity", entityId);

        try {
            return this.data
                    .getJSONObject("media")
                    .getJSONObject(entityId);
        } catch (JSONException exception) {
            //prevent throwing exception on firebase the entity throwed from the HTTP
            //is not available in the bootstrap
            /*throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"media." + entityId}
            );*/
        }
    }

    public JSONObject getMediaEntities() throws AvarioException {
        if (!this.hasData())
            return null;

        try {
            return this.data.getJSONObject("media");
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"media"}
            );
        }
    }

    public String getMediaSourceAppId(String name) throws AvarioException {
        if (!this.hasData())
            return null;

        try {
            return this.data
                    .getJSONObject("media.sources")
                    .getString(name);
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"media.sources." + name}
            );
        }
    }

    public String getStringsWifi(String name) throws AvarioException {
        if (!this.hasData())
            return null;

        try {
            return this.data
                    .getJSONObject("strings")
                    .getString(name);
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"media.sources." + name}
            );
        }
    }

    public String getWiFiHandlingStrings(String name) throws AvarioException {
        if (!this.hasData())
            return null;

        try {
            return this.data
                    .getJSONObject("wifi.handling")
                    .getJSONObject("strings")
                    .getString(name);
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"media.sources." + name}
            );
        }
    }

    public int getWiFiHandlingTimeOut() throws AvarioException {
        if (!this.hasData())
            return 0;

        try {
            return this.data
                    .getJSONObject("wifi.handling")
                    .getInt("timeout");
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"wifi.handling.timeout"}
            );
        }
    }

    public JSONObject getRoom(String entityId) throws AvarioException {
        if (!this.hasData())
            return null;

        try {
            return this.data
                    .getJSONObject("rooms")
                    .getJSONObject(entityId);
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{
                            String.format("rooms.%s", entityId)
                    }
            );
        }
    }

    public JSONArray getRooms() throws AvarioException {
        if (!this.hasData())
            return null;

        JSONArray outputJSON = new JSONArray(),
                orderJSON;
        JSONObject roomsJSON;

        try {
            orderJSON = this.data.getJSONArray("roomorder");
            roomsJSON = this.data.getJSONObject("rooms");
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"rooms and roomorder"}
            );
        }

        for (int index = 0, limit = orderJSON.length(); index < limit; index++) {
            String roomId = orderJSON.optString(index);

            try {
                outputJSON.put(roomsJSON.getJSONObject(roomId));
            } catch (JSONException exception) {
                throw new AvarioException(
                        Constants.ERROR_STATE_MISSINGKEY,
                        exception,
                        new Object[]{"rooms" + roomId}
                );
            }
        }

        return outputJSON;
    }

    public JSONObject getClimate() throws AvarioException {
        if (!this.hasData())
            return null;

        try {
            return this.data.getJSONObject("climate");
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"climate"}
            );
        }
    }

    public JSONObject getDial(String dialId) throws AvarioException {
        if (!this.hasData() || dialId == null)
            return null;

        try {
            return this.data
                    .getJSONObject("dials")
                    .getJSONObject(dialId);
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{String.format("dials.%s", dialId)}
            );
        }
    }

    public JSONObject getDialButton(String dialbuttonId) throws AvarioException {
        if (!this.hasData() || dialbuttonId == null)
            return null;

        try {
            return this.data
                    .getJSONObject("dial_buttons")
                    .getJSONObject(dialbuttonId);
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{String.format("dial_buttons.%s", dialbuttonId)}
            );
        }
    }

    public JSONObject getDialButtonStates() throws AvarioException {
        if (!this.hasData())
            return null;

        try {
            return this.data
                    .getJSONObject("states");
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"states"}
            );
        }
    }

    public JSONObject getDialButtonState(String stateId) throws AvarioException {
        if (!this.hasData() || stateId == null)
            return null;

        try {
            return this.data
                    .getJSONObject("states")
                    .getJSONObject(stateId);
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{String.format("states.%s", stateId)}
            );
        }
    }

    public String getErrorMessage(String errorCode) {
        String message;

        try {
            return this.data
                    .getJSONObject("strings")
                    .getString(errorCode);
        } catch (JSONException | NullPointerException exception) {
        }

        // Should there be an exception, continue execution
        try {
            Field field = R.string.class.getDeclaredField(String.format("error__%s", errorCode));

            message = this.context.getString(this.context.getResources().getIdentifier(
                    field.getName(),
                    "string",
                    this.context.getPackageName()
            ));
        } catch (NoSuchFieldException exception) {
            message = this.context.getString(R.string.error__generic);
        }

        return message;
    }

    public int getWifiTimeout() throws AvarioException {
        try {
            return this.data
                    .getJSONObject("settings")
                    .getJSONObject("delays")
                    .getInt("timeout");
        } catch (NullPointerException | JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"settings.delays.timeout"}
            );
        }
    }

    public int getSettingsHoldDelay() throws AvarioException {
        try {
            return this.data
                    .getJSONObject("settings")
                    .getJSONObject("delays")
                    .getInt("settings");
        } catch (NullPointerException | JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"settings.delays.settings"}
            );
        }
    }

    public int getAPIErrorDelay() throws AvarioException {
        try {
            return this.data
                    .getJSONObject("settings")
                    .getJSONObject("delays")
                    .getInt("api_error");
        } catch (NullPointerException | JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"settings.delays.api_error"}
            );
        }
    }

    public int getIdleDelay() throws AvarioException {
        try {
            return this.data
                    .getJSONObject("settings")
                    .getJSONObject("delays")
                    .getInt("idle_delay");
        } catch (NullPointerException | JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"settings.delays.idle_delay"}
            );
        }
    }

    public int getNagleDelay() throws AvarioException {
        try {
            return this.data
                    .getJSONObject("settings")
                    .getJSONObject("delays")
                    .getInt("nagle");
        } catch (NullPointerException | JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"settings.delays.nagle"}
            );
        }
    }

    public int getNagleMediaDelay() throws AvarioException {
        try {
            return this.data
                    .getJSONObject("settings")
                    .getJSONObject("delays")
                    .getInt("nagle_media");
        } catch (NullPointerException | JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"settings.delays.nagle_media"}
            );
        }
    }

    public int getInactivityDelay() throws AvarioException {
        try {
            return this.data
                    .getJSONObject("settings")
                    .getJSONObject("delays")
                    .getInt("inactivity");
        } catch (NullPointerException | JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"settings.delays.inactivity"}
            );
        }
    }

    public int getPostBLEDelay() throws AvarioException {
        try {
            return this.data
                    .getJSONObject("settings")
                    .getJSONObject("delays")
                    .getInt("post_ble");
        } catch (NullPointerException | JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"settings.delays.post_ble"}
            );
        }
    }

    /*
     ***********************************************************************************************
     * API
     ***********************************************************************************************
     */
    public String getHTTPUsername(String configId) throws AvarioException {
        if (!this.hasData())
            return null;
        try {
            return Connectivity.isConnectedToLan() ?
                    this.data
                            .getJSONObject("settings")
                            .getJSONObject("connectivity")
                            .getJSONObject("lan")
                            .getJSONObject("http")
                            .getJSONObject(configId)
                            .getString("username") :
                    this.data
                            .getJSONObject("settings")
                            .getJSONObject("connectivity")
                            .getJSONObject("wan")
                            .getJSONObject("http")
                            .getJSONObject(configId)
                            .getString("username");
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{
                            String.format("settings.http.%s.username", configId)
                    }
            );
        }
    }

    public String getHTTPPassword(String configId) throws AvarioException {
        if (!this.hasData())
            return null;

        try {
            return Connectivity.isConnectedToLan() ?
                    this.data
                            .getJSONObject("settings")
                            .getJSONObject("connectivity")
                            .getJSONObject("lan")
                            .getJSONObject("http")
                            .getJSONObject(configId)
                            .getString("password") :
                    this.data
                            .getJSONObject("settings")
                            .getJSONObject("connectivity")
                            .getJSONObject("wan")
                            .getJSONObject("http")
                            .getJSONObject(configId)
                            .getString("password");
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{
                            String.format("settings.http.%s.password", configId)
                    }
            );
        }
    }

    /**
     * Consolidates the protocol, host, and port settings into one string
     *
     * @param configId
     * @return
     * @throws AvarioException
     */
    public String getHTTPHost(String configId) throws AvarioException {
        if (!this.hasData())
            return null;

        try {
            JSONObject confJSON = Connectivity.isConnectedToLan() ?
                    this.data
                            .getJSONObject("settings")
                            .getJSONObject("connectivity")
                            .getJSONObject("lan")
                            .getJSONObject("http")
                            .getJSONObject(configId) :
                    this.data
                            .getJSONObject("settings")
                            .getJSONObject("connectivity")
                            .getJSONObject("wan")
                            .getJSONObject("http")
                            .getJSONObject(configId);

            return String.format("http%s://%s:%s",
                    confJSON.getBoolean("ssl") ? "s" : "",
                    confJSON.getString("host"),
                    confJSON.getString("port")
            );
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{
                            String.format("settings.http.%s.(host|port|ssl)", configId)
                    }
            );
        }
    }

    public JSONObject getCurrentStateRequest() throws AvarioException {
        try {
            return new JSONObject(
                    this.data
                            .getJSONObject("settings")
                            .getJSONObject("api")
                            .getJSONObject("states")
                            .toString()
            );
        } catch (NullPointerException | JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"settings.api.states"}
            );
        }
    }

    public JSONObject getBluetoothEndpointRequest() throws AvarioException {
        try {
            JSONObject specJSON;

            specJSON = new JSONObject(
                    this.data
                            .getJSONObject("settings")
                            .getJSONObject("api")
                            .getJSONObject("bluetooth")
                            .toString()
            );

            return specJSON;
        } catch (NullPointerException | JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"settings.api.bluetooth"}
            );
        }
    }

    public JSONObject getFCMRequest() throws AvarioException {
        try {
            return new JSONObject(
                    this.data
                            .getJSONObject("settings")
                            .getJSONObject("api")
                            .getJSONObject("fcm")
                            .toString()
            );
        } catch (NullPointerException | JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"settings.api.fcm"}
            );
        }
    }

    public JSONArray getFCMTopics() throws AvarioException {
        try {
            return this.data
                    .getJSONObject("settings")
                    .getJSONObject("fcm")
                    .getJSONArray("topics");
        } catch (NullPointerException | JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"settings.fcm.topics"}
            );
        }
    }

    /*
     ***********************************************************************************************
     * MQTT Stuff
     ***********************************************************************************************
     */
    public JSONObject getMQTTSettings() throws AvarioException {
        try {
            return Connectivity.isConnectedToLan() ?
                    this.data.getJSONObject("settings")
                            .getJSONObject("connectivity")
                            .getJSONObject("lan")
                            .getJSONObject("mqtt") :
                    this.data.getJSONObject("settings")
                            .getJSONObject("connectivity")
                            .getJSONObject("wan")
                            .getJSONObject("mqtt");

        } catch (JSONException | NullPointerException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"settings.mqtt"}
            );
        }
    }

    public JSONObject getSettingsSecurityTab() throws AvarioException {
        try {
            return this.data
                    .getJSONObject("settings")
                    .getJSONObject("securityTab");
        } catch (NullPointerException | JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"settings.securityTab"}
            );
        }
    }

    public Connectivity getConnectivityDetails() throws JSONException {
        String result = this.data
                .getJSONObject("settings")
                .getJSONObject("connectivity").toString();
        return new Gson().fromJson(result, Connectivity.class);
    }

    public JSONArray getLanMacList() throws AvarioException {
        try {
            return this.data
                    .getJSONObject("settings")
                    .getJSONObject("connectivity")
                    .getJSONArray("lanMac");
        } catch (NullPointerException | JSONException ignore) {
            return null;
        }
    }

    /**
     * Choose default algo for selected device.
     *
     * @param key default algo key
     * @return defalt algo value
     */
    public String getSettingsDefaultLightAlgo(String key) {
        String val = null;
        try {
            val = this.data.getJSONObject("settings")
                    .getJSONObject("default.algo")
                    .getString(key);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return val;
    }

    public JSONObject getSettingsPowerTab() throws AvarioException {
        try {
            return this.data
                    .getJSONObject("settings")
                    .getJSONObject("powerTab");
        } catch (NullPointerException | JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_MISSINGKEY,
                    exception,
                    new Object[]{"settings.powerTab"}
            );
        }
    }

    /**
     * Updates the internal StateArray from an MQTT response update.
     *
     * @param mqttJSON the JSON data from the MQTT update
     * @return the entity id being updated
     * @throws AvarioException
     */
    public String updateFromMQTT(JSONObject mqttJSON) throws AvarioException {
        JSONObject eventJSON;
        String entityId;

        try {
            // check the format of the JSON
            if (!mqttJSON.getString("event_type").equalsIgnoreCase(Constants.MQTT_EVENT_TYPE_STATE_CHANGED))
                throw new AvarioException(Constants.ERROR_STATE_MQTT_TYPE, null);

            eventJSON = mqttJSON.getJSONObject("event_data");

            for (String key : new String[]{"new_state", "old_state"}) {
                JSONObject stateJSON;

                if (eventJSON.isNull(key))
                    continue;

                stateJSON = eventJSON.getJSONObject(key);
                stateJSON.getString("entity_id");
                stateJSON.getJSONObject("attributes");
                stateJSON.get("state");
            }
        } catch (JSONException exception) {
            throw new AvarioException(Constants.ERROR_STATE_MQTT_INVALID, exception);
        }

        // transfer data into the entity
        for (String key : new String[]{"new_state", "old_state"}) {
            if (eventJSON.isNull(key))
                continue;

            try {
                this.updateState(key, eventJSON.getJSONObject(key));
            } catch (AvarioException | JSONException ignored) {
            }
        }

        try {
            entityId = mqttJSON
                    .getJSONObject("event_data")
                    .getJSONObject("new_state")
                    .getString("entity_id");

            APITimers.invalidate(entityId);

            return entityId;
        } catch (JSONException exception) {
            return null;
        }
    }

    public StateArray updateFromHTTP(JSONArray httpJSON) throws AvarioException {
        for (int index = 0, length = httpJSON.length(); index < length; index++) {
            JSONObject updateJSON = httpJSON.optJSONObject(index),
                    entityJSON = null;

            // try with usual entity
            try {
                entityJSON = this.getEntity(updateJSON.optString("entity_id"));
            } catch (AvarioException ignored) {
            }

            // try with media entity
            if (entityJSON == null)
                try {
                    entityJSON = this.getMediaEntity(updateJSON.optString("entity_id"));
                } catch (AvarioException ignored) {
                }

            // properly populate the old_state
            if (entityJSON != null)
                try {
                    JSONObject newstateJSON = entityJSON.optJSONObject("new_state");

                    if (newstateJSON == null || newstateJSON.length() == 0)
                        // entity so new no state is present whatsoever. use API current state as old state too
                        this.updateState("old_state", updateJSON);
                    else {
                        // entity has a prior current state
                        newstateJSON.put("entity_id", entityJSON.getString("entity_id"));
                        this.updateState("old_state", newstateJSON);
                    }
                } catch (JSONException ignored) {
                }

            try {
                this.updateState("new_state", updateJSON);
            } catch (AvarioException | JSONException ignored) {
            }
        }

        this.dirty = true;

        return this;
    }

    private void updateState(String property, JSONObject updateJSON) throws AvarioException,
            JSONException {
        JSONObject entityJSON;
        boolean mediaEntity;

        try {
            entityJSON = this.getEntity(updateJSON.getString("entity_id"));
            mediaEntity = false;
        } catch (AvarioException exception) {
            try {
                entityJSON = this.getMediaEntity(updateJSON.getString("entity_id"));
                mediaEntity = true;
            } catch (AvarioException exception2) {
                throw exception;
            }
        }

        // always overwrite state
        Log.i(TAG, String.format("Update %s of: %s", property, updateJSON.getString("entity_id")));
        entityJSON.put(property, updateJSON);

        if (mediaEntity && property.equals("new_state")) {
            String state = updateJSON.optString("state");

            // TODO inspect fix on server-side for media_position when switching between media
            try {
                switch (state) {
                    case Constants.ENTITY_MEDIA_STATE_PLAYING:
                    case Constants.ENTITY_MEDIA_STATE_PAUSED:
                        entityJSON.put(
                                "media_position_live",
                                updateJSON
                                        .getJSONObject("attributes")
                                        .getDouble("media_position")
                        );
                        break;

                    case Constants.ENTITY_MEDIA_STATE_STOPPED:
                        entityJSON.remove("media_position_live");
                        break;
                }
            } catch (JSONException exception) {
                entityJSON.remove("media_position_live");
            }
        }
    }

    /*
     ***********************************************************************************************
     * Persistence
     ***********************************************************************************************
     */
    public boolean delete() {
        File file = this.getFile();
        try {
            if (file != null) {
                return !file.exists() || file.delete();
            }
        } catch (SecurityException exception) {
            Log.d(TAG, "Error occurred", exception);
        }
        return false;
    }

    public StateArray save() throws AvarioException {
        File file = this.getFile();

        if (this.data == null || !this.dirty)
            return this;

        try {
            BufferedWriter writer;

            writer = new BufferedWriter(new FileWriter(file));
            writer.write(this.data.toString());
            writer.close();

            Log.d(TAG, "Saved to: " + file.getAbsolutePath());
        } catch (IOException exception) {
            throw new AvarioException(Constants.ERROR_STATE_FORBIDDEN, exception);
        }

        return this;
    }

    public StateArray load() throws AvarioException {
        StringBuilder builder;
        File file;

        file = this.getFile();

        if (file == null || !file.exists()) {
            this.setData(null);
            return this;
        }

        builder = new StringBuilder();

        try {
            BufferedReader stream = new BufferedReader(new FileReader(file));
            String line;

            while ((line = stream.readLine()) != null)
                builder.append(line);

            stream.close();
        } catch (IOException exception) {
            Log.d(TAG, "Unexpected error occurred", exception);
            // only happens when reading is done after closing the stream. Not happening, I assume.
        }

        try {
            this.setData(new JSONObject(builder.toString()));
        } catch (JSONException exception) {
            this.setData(null);
            throw new AvarioException(Constants.ERROR_BOOTSTRAP_INVALID, exception);
        }

        return this;
    }

    private File getFile() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            return null;

        File file = new File(this.context.getExternalFilesDir(null),
                this.context.getString(R.string.app__path__bootstrap)),
                dirs = file.getParentFile();

        if (!dirs.exists())
            dirs.mkdirs();
        return file;
    }

    /**
     * Data might be empty so use this to check.
     *
     * @return boolean.
     */
    public boolean isDataEmpty() {
        return data == null;
    }
}
