package com.avariohome.avario.activity;


import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.view.Window;
import android.widget.Toast;

import com.avariohome.avario.R;
import com.avariohome.avario.api.APIClient;
import com.avariohome.avario.core.Config;
import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.mqtt.MqttConnection;
import com.avariohome.avario.mqtt.MqttManager;
import com.avariohome.avario.service.DeviceAdminReceiver;
import com.avariohome.avario.util.Connectivity;
import com.avariohome.avario.util.PlatformUtil;


/**
 * Created by aeroheart-c6 on 08/12/2016.
 */
public class BootActivity extends BaseActivity {
    private static final String TAG = "Avario/BootActivity";

    private ProgressDialog progressPD;
    private SettingsListener settingsListener;

    private ComponentName mAdminComponentName;
    private DevicePolicyManager mDevicePolicyManager;
    private PackageManager mPackageManager;
    private static final String Battery_PLUGGED_ANY = Integer.toString(
            BatteryManager.BATTERY_PLUGGED_AC |
                    BatteryManager.BATTERY_PLUGGED_USB |
                    BatteryManager.BATTERY_PLUGGED_WIRELESS);

    private static final String DONT_STAY_ON = "0";

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity__boot);
        mDevicePolicyManager = (DevicePolicyManager)
                getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (MqttManager.getInstance().isConnected()) {
            this.startMainActivity();
            return;
        }

        this.progressPD = new ProgressDialog(this);
        this.progressPD.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.progressPD.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        this.progressPD.setCancelable(false);
        this.progressPD.setIndeterminate(true);
        this.progressPD.setMessage(this.getString(R.string.message__mqtt__connecting));

        this.settingsListener = new SettingsListener();

        // Set Default COSU policy
        mAdminComponentName = DeviceAdminReceiver.getComponentName(this);
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        mPackageManager = getPackageManager();
        if (mDevicePolicyManager.isDeviceOwnerApp(getPackageName())) {
//            setDefaultCosuPolicies(true);
        } else {
            Toast.makeText(getApplicationContext(),
                    "Not Device owner", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStart() {
        super.onStart();
        if (mDevicePolicyManager.isLockTaskPermitted(this.getPackageName())) {
            ActivityManager am = (ActivityManager) getSystemService(
                    Context.ACTIVITY_SERVICE);
            if (am.getLockTaskModeState() ==
                    ActivityManager.LOCK_TASK_MODE_NONE) {
//                startLockTask();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        Config config = Config.getInstance();

        if (config.isSet() && config.isResourcesFetched())
            this.loadBootstrap();
        else
            this.showSettingsDialog(this.settingsListener);
    }

    protected void loadBootstrap() {
        StateArray states = StateArray.getInstance(this.getApplicationContext());

        try {
            states.load();
        } catch (AvarioException exception) {
        }

        if (states.hasData()) {
            Connectivity.identifyConnection(getApplicationContext());
            this.sendFCMToken();
            this.connectMQTT(new MqttConnectionListener(), false);
            this.progressPD.show();
        } else
            this.showSettingsDialog(this.settingsListener);
    }

    private void sendFCMToken() {
        APIClient
                .getInstance()
                .postFCMToken(null);
    }

    protected void startMainActivity() {
        this.startActivity(new Intent(this, MainActivity.class));
        this.finish();
    }

    /*
     ***********************************************************************************************
     * Inner Classes - Listeners
     ***********************************************************************************************
     */
    private class SettingsListener extends BaseActivity.SettingsListener {
        @Override
        public void onSettingsChange() {
            BootActivity.this.startMainActivity();
        }
    }

    private class MqttConnectionListener implements MqttConnection.Listener {
        @Override
        public void onConnection(MqttConnection connection, boolean reconnection) {
        }

        @Override
        public void onConnectionFailed(MqttConnection connection, AvarioException exception) {
            BootActivity self = BootActivity.this;

            if (connection.getRetryCount() < connection.getRetryMax())
                return;

            PlatformUtil
                    .getErrorToast(self, exception)
                    .show();

            self.progressPD.dismiss();
            self.showSettingsDialog(self.settingsListener);
        }

        @Override
        public void onDisconnection(MqttConnection connection, AvarioException exception) {
        }

        @Override
        public void onSubscription(MqttConnection connection) {
            BootActivity self = BootActivity.this;

            connection.setListener(null);
            self.progressPD.dismiss();
            self.startMainActivity();
        }

        @Override
        public void onSubscriptionError(MqttConnection connection, AvarioException exception) {
        }

        @Override
        public void onStatusChanged(MqttConnection connection, MqttConnection.Status previous, MqttConnection.Status current) {
        }
    }


}
