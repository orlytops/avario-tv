package com.avariohome.avario.activity;


import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.avariohome.avario.Application;
import com.avariohome.avario.Constants;
import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.fragment.SettingsDialogFragment;
import com.avariohome.avario.mqtt.MqttConnection;
import com.avariohome.avario.mqtt.MqttManager;
import com.avariohome.avario.util.Connectivity;
import com.avariohome.avario.util.PlatformUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by aeroheart-c6 on 09/12/2016.
 */
public abstract class BaseActivity extends AppCompatActivity {
    public static final String TAG = "Avario/BaseActivity";

    protected boolean settingsOpened;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.checkPlayServices();

        try {
            Application.startWorker(getApplicationContext());
        } catch (NullPointerException ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.checkPlayServices();
    }

    @Override
    public void onWindowFocusChanged(boolean focused) {
        View decorView;

        super.onWindowFocusChanged(focused);

        if (!focused)
            return;

        decorView = this.getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    protected void showSettingsDialog(SettingsDialogFragment.Listener listener) {
        FragmentTransaction transaction = this.getFragmentManager().beginTransaction();

        SettingsDialogFragment settingsDialog = new SettingsDialogFragment();
        settingsDialog.setListener(listener);
        settingsDialog.setCancelable(false);
        settingsDialog.show(transaction, "dialog");

        this.settingsOpened = true;
    }

    protected void connectMQTTNaive(MqttConnection.Listener listener, boolean refresh) throws JSONException, MqttException {
        MqttManager manager = MqttManager.getInstance();
        MqttConnection connection;
        JSONObject mqttJSON;

        try {
            mqttJSON = StateArray
                    .getInstance()
                    .getMQTTSettings();
        } catch (AvarioException exception) {
            PlatformUtil
                    .getErrorToast(this, exception)
                    .show();
            return;
        }

        connection = manager.getConnection();

        if (connection == null) {
            connection = MqttManager.createConnection(
                    this.getApplicationContext(),
                    mqttJSON
            );

            manager.setConnection(connection);
        }

        connection.setListener(listener);

        if (refresh) {
            if (connection.getStatus() == MqttConnection.Status.CONNECTED) {
                connection.disconnect();
                return;
            } else {
                Connectivity connectivity = StateArray.getInstance().getConnectivityDetails();
                Connectivity.Credentials cred = Connectivity.isMacPresent(this) ?
                        connectivity.lan : connectivity.wan;
                if(Connectivity.isMacPresent(this)){
                    Toast.makeText(BaseActivity.this, "Connecting to LAN.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(BaseActivity.this, "Connecting to WAN.", Toast.LENGTH_SHORT).show();
                }
                MqttManager.updateConnection(connection, mqttJSON, cred);
                connection.reset();
            }
        }

        connection.connect();
    }

    protected void connectMQTT(MqttConnection.Listener listener, boolean refresh) {
        try {
            this.connectMQTTNaive(listener, refresh);
        } catch (JSONException | MqttException exception) {
            int code = exception instanceof MqttException
                    ? Constants.ERROR_MQTT_CONNECTION
                    : Constants.ERROR_MQTT_CONFIGURATION;

            PlatformUtil
                    .getErrorToast(this, new AvarioException(code, exception))
                    .show();
        }
    }

    protected void checkPlayServices() {
        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        int code = availability.isGooglePlayServicesAvailable(this);

        if (code == ConnectionResult.SUCCESS)
            return;

        availability.makeGooglePlayServicesAvailable(this);
    }

    abstract class SettingsListener implements SettingsDialogFragment.Listener {
        @Override
        public void onDialogDetached() {
            BaseActivity.this.settingsOpened = false;
        }

        @Override
        public void attemptMQTT(MqttConnection.Listener listener) throws JSONException, MqttException {
            BaseActivity.this.connectMQTTNaive(listener, true);
        }
    }
}
