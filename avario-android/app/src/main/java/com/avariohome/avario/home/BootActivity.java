package com.avariohome.avario.home;


import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.app.admin.SystemUpdatePolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.UserManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.avariohome.avario.R;
import com.avariohome.avario.api.APIClient;
import com.avariohome.avario.bus.WifiChange;
import com.avariohome.avario.bus.WifiConnected;
import com.avariohome.avario.core.Config;
import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.mqtt.MqttConnection;
import com.avariohome.avario.mqtt.MqttManager;
import com.avariohome.avario.receiver.WifiReceiver;
import com.avariohome.avario.service.AvarioReceiver;
import com.avariohome.avario.util.Connectivity;
import com.avariohome.avario.util.Log;
import com.avariohome.avario.util.MyCountDownTimer;
import com.avariohome.avario.util.PlatformUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


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
    private boolean timerIsStarted = false;
    private int timerTimeOut = 25000;
    private boolean isHasWifi = false;
    private LocalBroadcastManager localBroadcastManager;

    private AlertDialog.Builder builder;
    private AlertDialog alert11;


    private WifiReceiver wifiReceiver = new WifiReceiver();

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
//        try {
//            android.util.Log.v("FirebaseReport", getIntent().getStringExtra("data"));
//        } catch (NullPointerException ex){
//            FirebaseCrash.report(ex);
//        }

        mDevicePolicyManager = (DevicePolicyManager)
                getSystemService(Context.DEVICE_POLICY_SERVICE);
        builder = new AlertDialog.Builder(BootActivity.this);
        alert11 = builder.create();

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
        mAdminComponentName = AvarioReceiver.getComponentName(this);
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        mPackageManager = getPackageManager();
        if (mDevicePolicyManager.isDeviceOwnerApp(getPackageName())) {
            setDefaultCosuPolicies(true);
        } else {
        }

        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
               /* if (BluetoothScanner.getInstance().isEnabled())
                    BluetoothScanner.getInstance().scanLeDevice(true);*/
                mAdminComponentName = AvarioReceiver.getComponentName(BootActivity.this);
                mDevicePolicyManager = (DevicePolicyManager) getSystemService(
                        Context.DEVICE_POLICY_SERVICE);
                mPackageManager = getPackageManager();
                if (mDevicePolicyManager.isDeviceOwnerApp(getPackageName())) {
                    //setDefaultCosuPolicies(true);
                }

                subscriber.onNext(new Object());
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Object>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Object o) {
                        if (mDevicePolicyManager.isLockTaskPermitted(BootActivity.this.getPackageName())) {
                            final ActivityManager am = (ActivityManager) getSystemService(
                                    Context.ACTIVITY_SERVICE);
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if (am.getLockTaskModeState() ==
                                                ActivityManager.LOCK_TASK_MODE_NONE) {
                                            startLockTask();
                                            Config config = Config.getInstance();
                                            config.setIsKiosk(true);

                                        }
                                    } catch (Exception exception) {
                                    }
                                }
                            }, 100);
                        }
                    }
                });

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStart() {
        super.onStart();
        android.util.Log.v(TAG, "onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        try {
            unregisterReceiver(wifiReceiver);
        } catch (Exception e) {

        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timerIsStarted = false;
        android.util.Log.v(TAG, "onStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timerIsStarted = false;
        android.util.Log.v(TAG, "onPause");
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    protected void onResume() {
        super.onResume();
        try {
            EventBus.getDefault().register(this);
        } catch (Exception e) {

        }
        android.util.Log.v(TAG, "onResume");
        if (alert11 != null && alert11.isShowing()) {
            alert11.hide();
        }
        IntentFilter mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
        mIntentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        try {
            registerReceiver(wifiReceiver, mIntentFilter);
        } catch (Exception e) {

        }

        PowerManager.WakeLock wl;
        KeyguardManager.KeyguardLock kl;

        Log.d(getClass().getName(), "On reboot completed");
       /* PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "INFO");
        wl.acquire();
*/
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        kl = km.newKeyguardLock("name");
        kl.disableKeyguard();
        Config config = Config.getInstance();
        boolean isConfigSet = config.isSet();
//        boolean isConfigFetched = config.isResourcesFetched();
        if (isConfigSet)
            this.loadBootstrap();
        else
            this.showSettingsDialog(this.settingsListener);
    }

    protected void loadBootstrap() {
        final StateArray states = StateArray.getInstance(this.getApplicationContext());

        try {
            states.load();
        } catch (AvarioException exception) {
        }

        final ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (progressPD != null && !progressPD.isShowing()) {
            this.progressPD.show();
        }
        if (states.hasData()) {
            if (mWifi.isConnected()) {
                isHasWifi = true;
                Connectivity.identifyConnection(getApplicationContext());
                sendFCMToken();
                Log.d("Connect Mqtt", "Bootactivity");
                connectMQTT(new MqttConnectionListener(), false);
                progressPD.setMessage(getString(R.string.message__mqtt__connecting));
                countDownTimer.cancel();
            } else {
                Log.d("Wifi", "Attempting connect WIFI");
                TextView tv = (TextView) progressPD.findViewById(android.R.id.message);
                if (!tv.getText().toString().equals("Connecting to WiFi...")) {
                    progressPD.setMessage("Connecting to WiFi...");
                }
                if (!timerIsStarted) {
                    final Handler mHandler = new Handler();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {

                                    try {
                                        timerTimeOut = states.getWifiTimeout();
                                        Log.d("Timeout", states.getWifiTimeout() + "");
                                    } catch (AvarioException e) {
                                        e.printStackTrace();
                                    }
                                    countDownTimer.setMillisInFuture(timerTimeOut);
                                    if (!timerIsStarted) {
                                        timerIsStarted = true;
                                        countDownTimer.start();
                                    }
                                }
                            });
                        }
                    }).start();
                }
                if (isHasWifi) {
                    loadBootstrap();
                }
            }
        } else {
            progressPD.hide();
            showSettingsDialog(settingsListener);
        }

    }

    public MyCountDownTimer countDownTimer = new MyCountDownTimer(timerTimeOut, 1000) {

        final StateArray states = StateArray.getInstance(getBaseContext());

        @Override
        public void onTick(long millisUntilFinished) {
            long seconds = millisUntilFinished / 1000;
            Log.i("Seconds", "seconds remaining: " + millisUntilFinished / 1000);

            if (seconds == 26) {
                @SuppressLint("WifiManagerLeak") final WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

                final Handler mHandler = new Handler();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {

                                if (!wifi.isWifiEnabled()) {
                                    String message = "";
                                    countDownTimer.cancel();
                                    try {
                                        message = states.getStringMessage("0x03010");
                                    } catch (AvarioException e) {
                                        e.printStackTrace();
                                    }

                                    builder.setTitle("Wifi is not enabled");
                                    builder.setMessage(message);
                                    builder.setCancelable(false);

                                    builder.setPositiveButton(
                                            "Ok",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int id) {
                                                    dialog.cancel();
                                                    wifi.setWifiEnabled(true);
                                                    timerIsStarted = true;
                                                    countDownTimer.start();
                                                }
                                            });

                                    alert11 = builder.create();

                                    if (!alert11.isShowing()) {
                                        alert11.show();
                                    }
                                }
                            }
                        });
                    }
                }).start();

            }
        }

        @Override
        public void onFinish() {

            isHasWifi = false;
            final Handler mHandler = new Handler();
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    String title = "";
                    String message = "";

                    try {
                        title = states.getStringMessage("0x03020");
                        message = states.getStringMessage("0x03030");
                    } catch (AvarioException e) {
                        e.printStackTrace();
                    }

                    builder.setTitle(title);
                    builder.setMessage(message);
                    builder.setCancelable(false);

                    builder.setPositiveButton(
                            "Ok",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                                    countDownTimer.cancel();
                                }
                            });

                    alert11 = builder.create();
                    try {
                        unregisterReceiver(wifiReceiver);
                    } catch (Exception e) {

                    }
                    try {
                        if (!alert11.isShowing()) {
                            alert11.show();
                        }
                    } catch (Exception exception) {

                    }
                }
            });
        }
    };


    private void sendFCMToken() {
        APIClient
                .getInstance()
                .postFCMToken(Config.getInstance().getFCM());
    }

    protected void startMainActivity() {
        this.finish();
        this.startActivity(new Intent(this, MainActivity.class));
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
            Log.d("Error mqtt connect", exception.getMessage());
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
            Log.d(TAG, "onSubscription");
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
 /*
    *****************************************************************************************************************
    * FOR THE KIOSK MODE
    * ***************************************************************************************************************
    * */

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void setDefaultCosuPolicies(boolean active) {
        // set user restrictions
        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, active);
        setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, active);
        setUserRestriction(UserManager.DISALLOW_ADD_USER, active);
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, active);
        setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, active);

        // disable keyguard and status bar
        KeyguardManager km = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
        Log.d("Device Policy", km.isDeviceLocked() + " " + km.isDeviceSecure() + " " +
                km.isKeyguardLocked() + " " + km.isKeyguardSecure());
        mDevicePolicyManager.setKeyguardDisabled(mAdminComponentName, active);
        mDevicePolicyManager.setStatusBarDisabled(mAdminComponentName, active);

        // enable STAY_ON_WHILE_PLUGGED_IN
        //enableStayOnWhilePluggedIn(active);

        // set system update policy
        if (active) {
            mDevicePolicyManager.setSystemUpdatePolicy(mAdminComponentName,
                    SystemUpdatePolicy.createWindowedInstallPolicy(60, 120));
        } else {
            mDevicePolicyManager.setSystemUpdatePolicy(mAdminComponentName,
                    null);
        }

        // set this Activity as a lock task package

        mDevicePolicyManager.setLockTaskPackages(mAdminComponentName,
                active ? new String[]{getPackageName(), "com.google.android.youtube"} : new String[]{});

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MAIN);
        intentFilter.addCategory(Intent.CATEGORY_HOME);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);

        if (active) {
            // set Cosu activity as home intent receiver so that it is started
            // on reboot
            mDevicePolicyManager.addPersistentPreferredActivity(
                    mAdminComponentName, intentFilter, new ComponentName(
                            getPackageName(), BootActivity.class.getName()));
        } else {
            mDevicePolicyManager.clearPackagePersistentPreferredActivities(
                    mAdminComponentName, getPackageName());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setUserRestriction(String restriction, boolean disallow) {
        if (disallow) {
            mDevicePolicyManager.addUserRestriction(mAdminComponentName,
                    restriction);
        } else {
            mDevicePolicyManager.clearUserRestriction(mAdminComponentName,
                    restriction);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void enableStayOnWhilePluggedIn(boolean enabled) {
        if (enabled) {
            mDevicePolicyManager.setGlobalSetting(
                    mAdminComponentName,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    Integer.toString(BatteryManager.BATTERY_PLUGGED_AC
                            | BatteryManager.BATTERY_PLUGGED_USB
                            | BatteryManager.BATTERY_PLUGGED_WIRELESS));
        } else {
            mDevicePolicyManager.setGlobalSetting(
                    mAdminComponentName,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    "0"
            );
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(WifiChange event) {
        if (event.isAuthError()) {
            countDownTimer.cancel();
            isHasWifi = false;
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(WifiConnected event) {
        if (event.isConnected()) {
            Log.d("BootActivity", "Connected");
            countDownTimer.cancel();
            isHasWifi = true;
            loadBootstrap();
        } else {
            isHasWifi = false;
        }

    }

}
