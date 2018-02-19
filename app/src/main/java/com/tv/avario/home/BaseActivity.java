package com.tv.avario.home;


import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.tv.avario.Application;
import com.tv.avario.Constants;
import com.tv.avario.core.StateArray;
import com.tv.avario.exception.AvarioException;
import com.tv.avario.fragment.SettingsDialogFragment;
import com.tv.avario.mqtt.MqttConnection;
import com.tv.avario.mqtt.MqttManager;
import com.tv.avario.util.Log;
import com.tv.avario.util.PlatformUtil;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by aeroheart-c6 on 09/12/2016.
 */
public abstract class BaseActivity extends Activity {
  public static final String TAG = "Avario/BaseActivity";

  protected boolean settingsOpened;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.checkPlayServices();
  }

  @Override
  protected void onResume() {
    super.onResume();
    Application.startWorker(BaseActivity.this);
    this.checkPlayServices();
  }

  @Override
  protected void onPause() {
    super.onPause();
    Application.stopWorker();
  }

  @Override
  public void onWindowFocusChanged(boolean focused) {
    View decorView;

    super.onWindowFocusChanged(focused);

    if (!focused) { return; }

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

  protected void connectMQTTNaive(MqttConnection.Listener listener, boolean refresh,
      boolean firstConnect) throws JSONException, MqttException {
    MqttManager.getInstance();
    MqttManager manager = MqttManager.getInstance();
    MqttConnection connection;
    JSONObject mqttJSON;

    try {
      mqttJSON = StateArray
          .getInstance()
          .getMQTTSettings();
      Log.d("MqttSettings", mqttJSON.toString());

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
    String newHost = mqttJSON.getString("host");

    Log.d(TAG, "Host: " + newHost + " " + connection.getHost() + " " + manager.isConnected() + " " +
        connection.getStatus());
    if (!newHost.equals(connection.getHost())) {
      refresh = true;
    }

    if (refresh) {
      if (connection.getStatus() == MqttConnection.Status.CONNECTED) {
        Log.d(TAG, "not reset");
        connection.disconnect();
        return;
      } else {
        Log.d(TAG, "reset");
               /* MqttManager.updateConnection(connection, mqttJSON);
                connection.reset();*/
        if (firstConnect) {
          MqttManager.updateConnection(connection, mqttJSON);
          connection.reset();
        } else {
          connection = MqttManager.createConnection(
              this.getApplicationContext(),
              mqttJSON
          );

          manager.setConnection(connection);
          connection.reset();

        }
      }
    }


    connection.setListener(listener);
    connection.connect();

  }

  protected void connectMQTT(MqttConnection.Listener listener, boolean refresh) {
    try {
      Log.d(TAG, "listener is null: " + (listener == null));
      this.connectMQTTNaive(listener, refresh, false);
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

    if (code == ConnectionResult.SUCCESS) { return; }

    availability.makeGooglePlayServicesAvailable(this);
  }

  abstract class SettingsListener implements SettingsDialogFragment.Listener {
    @Override
    public void onDialogDetached() {
      BaseActivity.this.settingsOpened = false;
    }

    @Override
    public void attemptMQTT(MqttConnection.Listener listener) throws JSONException, MqttException {
      BaseActivity.this.connectMQTTNaive(listener, true, true);
    }
  }
}
