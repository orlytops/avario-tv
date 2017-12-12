
package com.avariohome.avario.home;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.app.admin.SystemUpdatePolicy;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.SslError;
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
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.NetworkError;
import com.android.volley.ParseError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.avariohome.avario.Application;
import com.avariohome.avario.Constants;
import com.avariohome.avario.R;
import com.avariohome.avario.api.APIClient;
import com.avariohome.avario.api.APIRequestListener;
import com.avariohome.avario.api.component.DaggerUserComponent;
import com.avariohome.avario.api.component.UserComponent;
import com.avariohome.avario.apiretro.services.UpdateService;
import com.avariohome.avario.bus.ShowNotification;
import com.avariohome.avario.bus.TriggerUpdate;
import com.avariohome.avario.bus.UpdateDownload;
import com.avariohome.avario.bus.WifiChange;
import com.avariohome.avario.bus.WifiConnected;
import com.avariohome.avario.core.BluetoothScanner;
import com.avariohome.avario.core.Config;
import com.avariohome.avario.core.Light;
import com.avariohome.avario.core.NagleTimers;
import com.avariohome.avario.core.Notification;
import com.avariohome.avario.core.NotificationArray;
import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.fragment.DialFragment;
import com.avariohome.avario.fragment.NotifListDialogFragment;
import com.avariohome.avario.fragment.NotificationDialogFragment;
import com.avariohome.avario.mqtt.MqttConnection;
import com.avariohome.avario.mqtt.MqttManager;
import com.avariohome.avario.presenters.UpdatePresenter;
import com.avariohome.avario.receiver.AlarmReceiver;
import com.avariohome.avario.receiver.WifiReceiver;
import com.avariohome.avario.service.AvarioReceiver;
import com.avariohome.avario.util.AssetUtil;
import com.avariohome.avario.util.Connectivity;
import com.avariohome.avario.util.CrossfadeWrapper;
import com.avariohome.avario.util.EntityUtil;
import com.avariohome.avario.util.Log;
import com.avariohome.avario.util.MyCountDownTimer;
import com.avariohome.avario.util.PlatformUtil;
import com.avariohome.avario.util.SystemUtil;
import com.avariohome.avario.widget.BatteryWifi;
import com.avariohome.avario.widget.DevicesList;
import com.avariohome.avario.widget.ElementsBar;
import com.avariohome.avario.widget.MediaList;
import com.avariohome.avario.widget.MediaSourcesList;
import com.avariohome.avario.widget.RoomSelector;
import com.avariohome.avario.widget.adapter.DeviceAdapter;
import com.avariohome.avario.widget.adapter.ElementAdapter;
import com.avariohome.avario.widget.adapter.Entity;
import com.avariohome.avario.widget.adapter.EventAdapter;
import com.avariohome.avario.widget.adapter.MediaAdapter;
import com.avariohome.avario.widget.adapter.RoomEntity;
import com.google.firebase.messaging.FirebaseMessaging;
import com.mikepenz.crossfader.Crossfader;
import com.mikepenz.crossfader.util.UIUtils;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.MiniDrawer;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;

import okhttp3.ResponseBody;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


/**
 * Created by aeroheart-c6 on 08/12/2016.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends BaseActivity {

    @Inject
    UpdateService userService;
    private UserComponent userComponent;
    private UpdatePresenter updatePresenter;


    public static final String TAG = "Avario/MainActivity";

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int CODE_DRAW_OVER_OTHER_APP_PERMISSION = 2084;

    private RelativeLayout contentRL;
    private FrameLayout controlsFL;
    private FrameLayout deviceLayout;
    private DrawerLayout drawer;
    private RoomSelector roomSelector;
    private DevicesList devicesList;
    private ElementsBar elementsBar;
    private MediaList mediaList;
    private MediaSourcesList sourcesList;
    private ProgressDialog progressPD;

    private ImageButton devicesIB;
    private ImageButton homeIB;
    private ImageButton cctvIB;
    private ImageButton boltIB;
    private ImageButton tempIB;
    private ImageButton activeModeIB;
    private BatteryWifi battery;

    private ImageButton playIB;
    private ImageButton nextIB;
    private ImageButton prevIB;
    private ImageButton volumeIB;

    private ImageButton notifIB;

    private Spinner eventsSpinner;

    private WebView contentWV;
    private DialFragment dialFragment;

    private MqttConnectionListener mqttListener;
    private Handler handler;
    private Runnable settingsRunnable;
    private Runnable inactiveRunnable;
    private boolean visible;
    private CurrentStateListener stateListener;
    private SettingsListener confListener;

    private BroadcastReceiver bluetoothReceiver;

    private ComponentName mAdminComponentName;
    private DevicePolicyManager mDevicePolicyManager;
    private PackageManager mPackageManager;

    private boolean isInActive = false;
    private boolean isMediaAvailable = false;

    private Config config;

    private AccountHeader headerResult = null;
    private Drawer result = null;
    private MiniDrawer miniResult = null;

    private Crossfader crossFader;

    private AlertDialog.Builder builder;
    private AlertDialog alert11;

    private List<IDrawerItem> itemDrawers = new ArrayList<>();
    private List<Integer> deviceSelected = new ArrayList<>();
    private Bundle savedInstanceState;

    private boolean timerIsStarted = false;
    private int timerTimeOut = 25000;
    private boolean isHasWifi = false;

    private WifiReceiver wifiReceiver = new WifiReceiver();
    private static Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;

    private AlertDialog.Builder builderUpdate;
    private AlertDialog alertUpdate;

    private ProgressDialog progressDownload;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        super.setContentView(R.layout.activity__main);
        this.savedInstanceState = savedInstanceState;
        EventBus.getDefault().register(this);

        config = Config.getInstance();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        userComponent = DaggerUserComponent.builder().build();
        userComponent.inject(this);
        updatePresenter = new UpdatePresenter(userService);

        progressDownload = new ProgressDialog(this);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        float yInches = metrics.heightPixels / metrics.ydpi;
        float xInches = metrics.widthPixels / metrics.xdpi;
        double diagonalInches = Math.sqrt(xInches * xInches + yInches * yInches);
        if (diagonalInches >= 6.5) {
            // 6.5inch device or bigger
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            config.setIsTablet(true);
        } else {
            // smaller device
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            config.setIsTablet(false);
        }

        builder = new AlertDialog.Builder(MainActivity.this);
        alert11 = builder.create();

        builderUpdate = new AlertDialog.Builder(this);

        this.handler = Application.mainHandler;

        this.mqttListener = new MqttConnectionListener();
        this.confListener = new SettingsListener();
        this.bluetoothReceiver = new BluetoothReceiver();

        this.inactiveRunnable = new InactivityRunnable();
        this.settingsRunnable = new Runnable() {
            @Override
            public void run() {
                MainActivity.this.showSettingsDialog(false);
            }
        };

        this.initFCM();
        this.initViews();
        this.initListingViews(savedInstanceState);
        this.initViewConf();

        this.registerEvents();
        if (config.isTablet()) {
            this.loadTabAssets();
        } else {
            this.loadPhoneAssets();
        }

        this.activeModeIB = this.homeIB;
        this.activeModeIB.setActivated(true);
        this.activeModeIB.performClick();
        this.isBluetoothAvailable();
        this.checkNotifications();

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(this.bluetoothReceiver, filter);
        Log.d("MainActivity", "onCreate");

        //Thread.setDefaultUncaughtExceptionHandler(new UnCaughtException(MainActivity.this));

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(MainActivity.this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 123, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 4);
        calendar.set(Calendar.MINUTE, 30);
        calendar.set(Calendar.SECOND, 00);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, calendar.getTimeInMillis(), 24 * 60 * 60 * 1000, pendingIntent);

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        timerIsStarted = false;

        try {
            EventBus.getDefault().register(this);
        } catch (Exception e) {

        }
        this.visible = true;

        /*if (alert11.isShowing()) {
            alert11.cancel();
        }*/

        final StateArray states = StateArray.getInstance(this.getApplicationContext());

        try {
            states.load();
        } catch (AvarioException exception) {
        }
        MqttManager manager = MqttManager.getInstance();

        if (manager.isConnected()) {
            manager
                    .getConnection()
                    .setListener(this.mqttListener);

            loadFromStateArray();
            fetchCurrentStates();

            android.util.Log.v("ProgressDialog", "OnResume");
            //this.showBusyDialog(null);
        } else if (!this.settingsOpened) {
            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (!mWifi.isConnected()) {
                Log.d("MainActivity", "handle no wifi");
                handleNoWifi();
            } else {
                Log.d("MainActivity", "connectMQTT");
                this.connectMQTT(this.getString(R.string.message__mqtt__connecting));
            }
        }

       /* if (BluetoothScanner.getInstance().isEnabled())
            BluetoothScanner.getInstance().scanLeDevice(true);*/

        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                /*if (BluetoothScanner.getInstance().isEnabled())
                    BluetoothScanner.getInstance().scanLeDevice(true);*/
                mAdminComponentName = AvarioReceiver.getComponentName(MainActivity.this);
                mDevicePolicyManager = (DevicePolicyManager) getSystemService(
                        Context.DEVICE_POLICY_SERVICE);
                mPackageManager = getPackageManager();
                if (mDevicePolicyManager.isDeviceOwnerApp(getPackageName())) {
                    setDefaultCosuPolicies(true);
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
                        if (mDevicePolicyManager.isLockTaskPermitted(MainActivity.this.getPackageName())) {
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

        // add stored algo from shared preferences.
        Light.addAllAlgo(Config.getInstance().getLightAlgo());
        // delete algo stored to avoid redundancy.
        Config.getInstance().deleteAlgo();
        if (config.isTablet()) {
            battery.setIsLan(Connectivity.identifyConnection(MainActivity.this));
        }
        PowerManager.WakeLock wl;
        KeyguardManager.KeyguardLock kl;

        /*PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "INFO");
        wl.acquire();
*/
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        kl = km.newKeyguardLock("name");
        kl.disableKeyguard();
        Log.d("MainActivity", "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();

        this.hideBusyDialog();
        this.progressPD = null;
        this.visible = false;
        //BluetoothScanner.getInstance().scanLeDevice(false);

        // Store algo to be use later when app restarts.
        Config.getInstance().setLightAlgo(Light.getInstance().algos);
    }


    @Override
    public void onBackPressed() {
    }

    @Override
    protected void onDestroy() {
        Application.worker.quitSafely();
        super.onDestroy();

        unregisterReceiver(this.bluetoothReceiver);
        config.setRoomSelected("");
    }

    @Override
    public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);

        if (StateArray.getInstance().isRefreshing())
            this.showBusyDialog(null);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        this.startInactivityCountdown();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("Result---------------", requestCode + " " + requestCode + " ");
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK)
            BluetoothScanner.getInstance().scanLeDevice(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onStart() {
        super.onStart();
        /*if (mDevicePolicyManager.isLockTaskPermitted(this.getPackageName())) {
            ActivityManager am = (ActivityManager) getSystemService(
                    Context.ACTIVITY_SERVICE);
            if (am.getLockTaskModeState() ==
                    ActivityManager.LOCK_TASK_MODE_NONE) {
                startLockTask();
            }
        }*/
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
        try {
            unregisterReceiver(wifiReceiver);
        } catch (Exception e) {

        }
    }

    private void initFCM() {
//        this.startService(
//            new Intent(this, FCMIntentService.class)
//                .setAction(FCMIntentService.ACTION_REFRESH)
//        );

        IntentFilter notificationIntentFilter = new IntentFilter();
        notificationIntentFilter.addAction(Constants.BROADCAST_NOTIF);
        notificationIntentFilter.addAction(Constants.BROADCAST_BOOTSTRAP_CHANGED);
        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(new NotificationReceiver(), notificationIntentFilter);

        this.initFCMTopics();
    }

    private void initFCMTopics() {
        StateArray states = StateArray.getInstance();
        JSONArray topics;

        try {
            topics = states.getFCMTopics();
        } catch (AvarioException exception) {
            return;
        }

        if (topics == null)
            return;

        try {
            FirebaseMessaging fcm = FirebaseMessaging.getInstance();

            for (int index = 0, length = topics.length(); index < length; index++)
                fcm.subscribeToTopic(topics.getString(index));
        } catch (JSONException exception) {
        }
    }


    private void initViews() {
        this.drawer = (DrawerLayout) this.findViewById(R.id.drawer);
        this.contentRL = (RelativeLayout) this.findViewById(R.id.content);
        this.controlsFL = (FrameLayout) this.findViewById(R.id.controls__holder);
        //this.deviceLayout = (FrameLayout) this.findViewById(R.id.layout_device);
        this.roomSelector = (RoomSelector) this.findViewById(R.id.selector);
        this.elementsBar = (ElementsBar) this.findViewById(R.id.elements);
        this.sourcesList = (MediaSourcesList) this.findViewById(R.id.sources);

        this.devicesIB = (ImageButton) this.findViewById(R.id.devices);

        this.homeIB = (ImageButton) this.findViewById(R.id.home);
        this.boltIB = (ImageButton) this.findViewById(R.id.bolt);
        this.cctvIB = (ImageButton) this.findViewById(R.id.cctv);
        this.tempIB = (ImageButton) this.findViewById(R.id.temperature);
        this.contentWV = (WebView) this.findViewById(R.id.webview);
        this.eventsSpinner = (Spinner) this.findViewById(R.id.spinner_events);

        this.playIB = (ImageButton) this.findViewById(R.id.play);
        this.nextIB = (ImageButton) this.findViewById(R.id.next);
        this.prevIB = (ImageButton) this.findViewById(R.id.prev);
        this.volumeIB = (ImageButton) this.findViewById(R.id.volume);

        this.notifIB = (ImageButton) this.findViewById(R.id.notif);

        this.battery = (BatteryWifi) this.findViewById(R.id.battery);

        this.dialFragment = (DialFragment) this
                .getSupportFragmentManager()
                .findFragmentById(R.id.dial);

        if (!config.isTablet()) {
            initSpinner();
        }
    }

    private void initSpinner() {
        final int[] check = {0};

        List<Integer> eventsIdList = new ArrayList<>();
        eventsIdList.add(R.array.ic__mode__temp);
        eventsIdList.add(R.array.ic__mode__bolt);
        eventsIdList.add(R.array.ic__mode__cctv);

        EventAdapter eventAdapter = new EventAdapter(this, eventsIdList);
        eventsSpinner.setAdapter(eventAdapter);

        eventsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (++check[0] > 1) {
                    switch (position) {
                        case 0:
                            activateModeClimate();
                            break;
                        case 1:
                            activateModeEnergy();
                            break;
                        case 2:
                            activateModeCCTV();
                            break;
                    }

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @SuppressLint("RtlHardcoded")
    private void initListingViews(Bundle savedInstanceState) {
        DisplayMetrics metrics = this.getResources().getDisplayMetrics();
        FrameLayout.LayoutParams params;

        params = new FrameLayout.LayoutParams(
                (int) (275.00 * metrics.density + this.getResources().getDimensionPixelSize(R.dimen.deviceslist__inset)),
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        params.gravity = Gravity.LEFT | Gravity.TOP;

        this.devicesList = new DevicesList(this);
        this.devicesList.setLayoutParams(params);
        this.devicesList.setId(View.generateViewId());
        this.devicesList.setVisibility(View.GONE);
        if (config.isTablet()) {
            this.controlsFL.addView(this.devicesList);
        }

        params = new FrameLayout.LayoutParams(
                (int) (375.00 * metrics.density),
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        params.gravity = Gravity.LEFT | Gravity.TOP;
        params.setMargins(
                0,
                0,
                0,
                0
        );

        this.mediaList = new MediaList(this);
        this.mediaList.setLayoutParams(params);
        this.mediaList.setId(View.generateViewId());
        this.mediaList.setVisibility(View.GONE);
        disableMediaPlay();
        if (config.isTablet()) {
            this.controlsFL.addView(this.mediaList);
        }
    }

    private void disableMediaPlay() {
        playIB.setEnabled(false);
        nextIB.setEnabled(false);
        prevIB.setEnabled(false);
        volumeIB.setEnabled(false);
    }

    private void enableMediaPlay() {
        playIB.setEnabled(true);
        nextIB.setEnabled(true);
        prevIB.setEnabled(true);
        volumeIB.setEnabled(true);
    }

    private void initViewConf() {
        this.drawer.setScrimColor(Color.TRANSPARENT);
    }

    private boolean isBluetoothAvailable() {
        if (BluetoothScanner.getInstance().isEnabled())
            return true;
        else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return false;
        }
    }

    private boolean isNotifListVisible() {
        FragmentManager manager = MainActivity.this.getFragmentManager();

        NotifListDialogFragment notifListFragment;

        notifListFragment = (NotifListDialogFragment)
                manager.findFragmentByTag("notif-list-dialog");

        if (notifListFragment != null) {
            notifListFragment.refresh();
            return true;
        }
        return false;
    }

    private void checkNotifications() {
        List<Notification> notifications = NotificationArray.getInstance().getNotifications();

        notifIB.setVisibility(
                (notifications != null && !notifications.isEmpty())
                        ? View.VISIBLE : View.GONE
        );
    }

    private void loadFromStateArray() {
        try {
            this.roomSelector.setup();
        } catch (AvarioException exception) {
            PlatformUtil
                    .getErrorDialog(this, exception)
                    .show();
        }
    }

    private void loadPhoneAssets() {
        int[][] resourceIds = new int[][]{
                new int[]{
                        R.id.home,

                        R.id.devices,
                        R.id.notif,

                        R.id.prev,
                        R.id.play,
                        R.id.next,
                        R.id.volume,
                },
                new int[]{
                        R.array.ic__mode__home,

                        R.array.ic__topbar__dropdown,
                        R.array.ic__topbar__notiff,

                        R.array.ic__topbar__prev,
                        R.array.ic__topbar__play,
                        R.array.ic__topbar__next,
                        R.array.ic__topbar__volume,
                },
        };

        for (int index = 0; index < resourceIds[0].length; index++) {
            AssetUtil.loadImage(
                    this,
                    resourceIds[1][index],
                    new AssetUtil.ImageViewCallback((ImageButton) this.findViewById(resourceIds[0][index])), (ImageButton) this.findViewById(resourceIds[0][index])
            );
        }
    }

    private void loadTabAssets() {
        int[][] resourceIds = new int[][]{
                new int[]{
                        R.id.home,
                        R.id.cctv,
                        R.id.bolt,
                        R.id.temperature,

                        R.id.devices,
                        R.id.notif,

                        R.id.prev,
                        R.id.play,
                        R.id.next,
                        R.id.volume,
                },
                new int[]{
                        R.array.ic__mode__home,
                        R.array.ic__mode__cctv,
                        R.array.ic__mode__bolt,
                        R.array.ic__mode__temp,

                        R.array.ic__topbar__dropdown,
                        R.array.ic__topbar__notiff,

                        R.array.ic__topbar__prev,
                        R.array.ic__topbar__play,
                        R.array.ic__topbar__next,
                        R.array.ic__topbar__volume,
                },
        };

        for (int index = 0; index < resourceIds[0].length; index++) {
            AssetUtil.toDrawable(
                    this,
                    resourceIds[1][index],
                    new AssetUtil.ImageViewCallback((ImageButton) this.findViewById(resourceIds[0][index]))
            );
        }
    }

    private void startInactivityCountdown() {
        Config config = Config.getInstance();

        this.handler.removeCallbacks(this.inactiveRunnable);
        this.handler.postDelayed(
                this.inactiveRunnable,
                config.getInactivityDelay()
        );
    }

    /**
     * Convenience function to swap between devicesList and mediaList automatically
     *
     * @return the view it is currently showing
     */
    private View toggleLeftView() {
        int visibility = this.devicesList.getVisibility();
        boolean showDevices = visibility == View.GONE || visibility == View.INVISIBLE;
        View view = showDevices ? this.devicesList : this.mediaList;

        this.toggleLeftView(view);

        return view;
    }

    /**
     * Forces the specified view to show inside the `controlsFL` container. Method is currently set
     * to limit from either the:
     * * devicesList
     * * mediaList
     * Provide other views and it does nothing
     *
     * @param view
     */
    private void toggleLeftView(View view) {
        if ((view != this.devicesList && view != this.mediaList) ||
                (view.getVisibility() == View.VISIBLE))
            return;

        if (view.getId() == this.devicesList.getId()) {
            this.devicesList.setVisibility(View.VISIBLE);
            this.mediaList.setVisibility(View.GONE);
            disableMediaPlay();
        } else {
            this.devicesList.setVisibility(View.GONE);
            this.mediaList.setVisibility(View.VISIBLE);
        }
    }

    private void registerEvents() {
        UIListener uiListener = new UIListener();
        WidgetListener widgetListener = new WidgetListener();
        if (config.isTablet()) {
            this.battery.setOnTouchListener(uiListener);
        } else {
            this.homeIB.setOnTouchListener(uiListener);
        }

        this.contentRL.setOnClickListener(uiListener);

        for (ImageButton button : new ImageButton[]{
                this.devicesIB,
                this.homeIB,
                this.boltIB,
                this.cctvIB,
                this.tempIB,
                this.playIB,
                this.nextIB,
                this.prevIB,
                this.volumeIB
        })
            button.setOnClickListener(uiListener);

        if (config.isTablet()) {
            battery.setOnClickListener(uiListener);
        }
        this.notifIB.setOnClickListener(uiListener);

        this.roomSelector.setSelectionListener(widgetListener);
        this.elementsBar.setListener(widgetListener);
        this.devicesList.setListener(widgetListener);
        this.mediaList.setListener(widgetListener);
        this.sourcesList.setListener(widgetListener);
    }

    private void unloadWebView() {
        if (this.contentWV == null)
            return;

        // Found this post on how to kill an Android WebView.
        // Sad to say there's no real way to "kill" a Webview but instead make it load a bogus page.
        // See for more info: http://www.mcseven.me/2011/12/how-to-kill-an-android-webview/#Solutions-1
        this.contentWV.loadUrl("about:blank");
        this.contentWV.setVisibility(View.GONE);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void loadWebView(String url) {
        this.contentWV.setVisibility(View.VISIBLE);
        this.contentWV.getSettings().setJavaScriptEnabled(true);
        this.contentWV.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });
        this.contentWV.loadUrl(url);
    }

    private void activateModeHome() {
        DeviceAdapter adapter = this.devicesList.getAdapter();
        int size = adapter.size();

        this.elementsBar
                .getAdapter()
                .setMode(ElementAdapter.MODE_HOME);

        adapter.setMode(DeviceAdapter.MODE_HOME);
        adapter.clear();
        adapter.notifyItemRangeRemoved(0, size);

        this.devicesIB.setEnabled(true);

        this.roomSelector.setSelectedRoom(this.roomSelector.getSelectedRoom());

        //handleMediaSelected();
        this.unloadWebView();
    }

    private void activateModeClimate() {
        StateArray states = StateArray.getInstance();
        DeviceAdapter adapter = this.devicesList.getAdapter();
        int size = adapter.size();

        this.elementsBar
                .getAdapter()
                .setMode(ElementAdapter.MODE_CLIMATE);

        adapter.setMode(DeviceAdapter.MODE_CLIMATE);
        adapter.clear();
        adapter.notifyItemRangeRemoved(0, size);

        this.devicesIB.setEnabled(false);
        this.roomSelector.setTitle(this.getString(R.string.mode__climate));
        this.unloadWebView();

        try {
            JSONObject climateJSON = states.getClimate();
            RoomEntity climate;

            climate = new RoomEntity();
            climate.id = climateJSON.optString("entity_id");
            climate.name = climateJSON.optString("name", this.getString(R.string.mode__climate));
            climate.data = climateJSON;

            this.updateForRoom(climate);
            this.toggleLeftView(this.devicesList);
        } catch (AvarioException exception) {
            PlatformUtil
                    .getErrorToast(this, exception)
                    .show();
        }
    }

    private void activateModeEnergy() {

        StateArray states = StateArray.getInstance();
        String url = "";
        try {
            url = states.getSettingsPowerTab().getString("url");
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (AvarioException e) {
            e.printStackTrace();
        }

        this.elementsBar
                .getAdapter()
                .setMode(ElementAdapter.MODE_HOME);

        this.devicesList
                .getAdapter()
                .setMode(DeviceAdapter.MODE_HOME);

        this.devicesIB.setEnabled(false);
        this.roomSelector.setTitle(this.getString(R.string.mode__energy));
        //this.unloadWebView();
        this.loadWebView(url);
    }

    /**
     * @todo use CCTV IP for the url
     */
    private void activateModeCCTV() {
        StateArray states = StateArray.getInstance();
        String url = "";
        try {
            url = states.getSettingsSecurityTab().getString("url");
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (AvarioException e) {
            e.printStackTrace();
        }

        this.elementsBar
                .getAdapter()
                .setMode(ElementAdapter.MODE_HOME);

        this.devicesList
                .getAdapter()
                .setMode(DeviceAdapter.MODE_HOME);

        this.devicesIB.setEnabled(false);
        this.playIB.setEnabled(false);
        this.nextIB.setEnabled(false);
        this.prevIB.setEnabled(false);
        this.volumeIB.setEnabled(false);
        this.roomSelector.setTitle(this.getString(R.string.mode__cctv));
        this.loadWebView(url);
    }

    private void updateForRoom(RoomEntity room) {
        StateArray state = StateArray.getInstance();
        String[] backgroundUrls;

        if (room == null)
            return;

        try {
            this.updateElements(state, room.data.getJSONArray("elements"));
            this.updateDevices(state, room.data.getJSONArray("list_devices"));
            this.updateDefaultsForEntity(room);
        } catch (JSONException exception) {
            String[] msgArgs = new String[]{
                    String.format("%s.elements and/or %s.list_devices", room.id, room.id)
            };

            PlatformUtil
                    .getErrorToast(this, new AvarioException(Constants.ERROR_STATE_MISSINGKEY, exception, msgArgs))
                    .show();
        }

        try {
            String temp = room.data.getString("background");

            if (temp == null || temp.length() <= 0)
                throw new JSONException("background is not valid");

            backgroundUrls = new String[]{temp};
        } catch (JSONException exception) {
            backgroundUrls = this.getResources().getStringArray(R.array.bg__app__light);
        }

        AssetUtil.toDrawable(
                this,
                AssetUtil.toAbsoluteURLs(this, backgroundUrls),
                new AppBGCallback(this.contentRL)
        );
    }

    /**
     * Updates the dial depending on the currently selected room. This should only be called when:
     * - a room is selected
     *
     * @param entity a RoomSelector.Room instance to base the dial devices on. When null is passed,
     *               the currently selected room will be automatically retrieved
     */
    private void updateDefaultsForEntity(Entity entity) {
        if (entity == null)
            entity = this.roomSelector.getSelectedRoom();

        JSONArray devicesJSON;
        List<String> entityIds;

        try {
            devicesJSON = entity.data.getJSONArray("dial_devices");
        } catch (JSONException exception) {
            PlatformUtil
                    .getErrorToast(this, new AvarioException(
                            Constants.ERROR_STATE_MISSINGKEY,
                            exception,
                            new Object[]{entity.id + ".dial_devices"}
                    ))
                    .show();

            return;
        }
        entityIds = this.devicesList.setSelections(devicesJSON);

        if (entityIds.size() > 0) {
            this.dialFragment.setEnabled(true);
            this.dialFragment.setEntities(entityIds);
        } else
            this.dialFragment.setEnabled(false);
    }

    /**
     * "re-adapts" the dial based on the selections from the provided widget. Currently the widgets
     * being supported are:
     * * DevicesList
     * * MediaList
     * <p>
     * The method does nothing when it is passed with an unsupported widget
     */
    private void updateDialFromSelections(View view) {
        if (view != this.mediaList && view != this.devicesList)
            return;

        // media list
        isMediaAvailable = false;
        if (view == this.mediaList) {
            MediaAdapter adapter = this.mediaList.getAdapter();
            Entity media = adapter.getSelected();

            if (media != null) {
                isMediaAvailable = true;
                this.dialFragment.setMediaEntity(media.id);
            }
        }
        // devices list
        else {
            DeviceAdapter adapter = this.devicesList.getAdapter();
            List<String> entityIds = new ArrayList<>();

            for (Entity device : adapter.getSelected()) {
                entityIds.add(device.id);
            }

            this.dialFragment.setEntities(entityIds);

            // initial click action on selected dial button.
            this.dialFragment.click(Light.isPresentOnAlgoList(entityIds));
        }
    }

    private void updateElements(StateArray state, JSONArray elementsJSON) {
        ElementAdapter adapter = this.elementsBar.getAdapter();
        int newSize = elementsJSON.length(),
                oldSize = adapter.size(),
                diff = oldSize - newSize,
                end,
                misses;

        // changes
        end = Math.min(newSize, oldSize);
        misses = 0;

        for (int index = 0, loop = 0; loop < end && index < newSize; index++) {
            try {
                JSONObject entityJSON = state.getEntity(elementsJSON.optString(index));
                Entity entity;

                entity = adapter.get(index);
                entity.id = entityJSON.optString("entity_id");
                entity.data = entityJSON;
                entity.selected = false;

                loop++;
            } catch (NullPointerException exception) {
                loop++;
            } catch (AvarioException exception) {
                misses++;

                PlatformUtil
                        .getErrorToast(this, exception)
                        .show();
            }
        }

        adapter.notifyItemRangeChanged(0, oldSize - misses);

        // removal
        end = oldSize - diff - misses;

        for (int index = oldSize - 1; index >= end; index--)
            adapter.remove(index);

        adapter.notifyItemRangeRemoved(end, diff);

        // additions
        for (int index = oldSize + misses; index < newSize; index++) {
            try {
                JSONObject entityJSON = state.getEntity(elementsJSON.optString(index));
                Entity element;

                element = new Entity();
                element.id = entityJSON.optString("entity_id");
                element.data = entityJSON;
                element.selected = false;

                adapter.append(element);
            } catch (AvarioException exception) {
                newSize--;

                PlatformUtil
                        .getErrorToast(this, exception)
                        .show();
            }
        }

        adapter.notifyItemRangeInserted(oldSize, newSize);

        Log.d("Element", adapter.size() + "");
    }

    private void updateDevices(StateArray state, JSONArray controlsJSON) {
        DeviceAdapter adapter = this.devicesList.getAdapter();
        int newSize = controlsJSON.length(),
                oldSize = adapter.size(),
                diff = oldSize - newSize,
                end,
                misses;

        // changes
        end = Math.min(newSize, oldSize);
        misses = 0;

        for (int index = 0, loop = 0; loop < end && index < newSize; index++) {
            try {
                JSONObject entityJSON = state.getEntity(controlsJSON.optString(index));
                Entity entity;

                entity = adapter.get(index);
                entity.id = entityJSON.optString("entity_id");
                entity.data = entityJSON;
                entity.selected = false;

                loop++;
            } catch (NullPointerException exception) {
                loop++;
            } catch (AvarioException exception) {
                misses++;

                PlatformUtil
                        .getErrorToast(this, exception)
                        .show();
            }
        }

        adapter.notifyItemRangeChanged(0, oldSize - misses);

        // removal
        end = oldSize - diff - misses;

        for (int index = oldSize - 1; index >= end; index--)
            adapter.remove(index);

        adapter.notifyItemRangeRemoved(end, diff);

        // additions
        for (int index = oldSize + misses; index < newSize; index++) {
            try {
                JSONObject entityJSON = state.getEntity(controlsJSON.optString(index));
                Entity entity;

                entity = new Entity();
                entity.id = entityJSON.optString("entity_id");
                entity.data = entityJSON;
                entity.selected = false;

                adapter.append(entity);
                itemDrawers.add(new PrimaryDrawerItem().withIcon(AssetUtil.toDrawable(MainActivity.this,
                        EntityUtil.getStateIconUrl(MainActivity.this, entity.data))).withSelectable(false).withIdentifier(index));

            } catch (AvarioException exception) {
                PlatformUtil
                        .getErrorToast(this, exception)
                        .show();
            }
        }

        adapter.notifyItemRangeInserted(oldSize, newSize);

        if (!config.isTablet()) {
            try {
                handleCrossFader();
            } catch (Exception e) {

            }
        }
    }

    private void handleCrossFader() {

        result = new DrawerBuilder()
                .withActivity(this)
                .withDrawerItems(itemDrawers)
                .withTranslucentStatusBar(false)
                .withCustomView(this.devicesList)
                .withGenerateMiniDrawer(true)
                .withSavedInstance(savedInstanceState)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        Log.d("Selected", drawerItem.isSelected() + " " + position);

                        if (deviceSelected.size() == 0) {
                            view.setBackgroundColor(getResources().getColor(R.color.gray1));
                            deviceSelected.add(position);
                            return false;
                        }

                        for (int i = 0; i < deviceSelected.size(); i++) {
                            if (i == position) {
                                view.setBackgroundColor(getResources().getColor(R.color.trasnparent));
                                deviceSelected.remove(deviceSelected.get(i));
                            } else {
                                view.setBackgroundColor(getResources().getColor(R.color.gray1));
                                deviceSelected.add(position);
                            }
                        }

                        return false;
                    }
                })
                .buildView();

        //the MiniDrawer is managed by the Drawer and we just get it to hook it into the Crossfader
        miniResult = result.getMiniDrawer();

        //get the widths in px for the first and second panel
        int firstWidth = (int) UIUtils.convertDpToPixel(300, this);
        int secondWidth = (int) UIUtils.convertDpToPixel(72, this);

        //create and build our crossfader (see the MiniDrawer is also builded in here, as the build method returns the view to be used in the crossfader)
        //the crossfader library can be found here: https://github.com/mikepenz/Crossfader
        crossFader = new Crossfader()
                .withContent(findViewById(R.id.crossfade_content))
                .withFirst(result.getSlider(), firstWidth)
                .withSecond(miniResult.build(this), secondWidth)
                .withSavedInstance(savedInstanceState)
                .build();

        //define the crossfader to be used with the miniDrawer. This is required to be able to automatically toggle open / close
        miniResult.withCrossFader(new CrossfadeWrapper(crossFader));

        //define a shadow (this is only for normal LTR layouts if you have a RTL app you need to define the other one
        crossFader.getCrossFadeSlidingPaneLayout().setShadowResourceLeft(R.drawable.material_drawer_shadow_left);
    }

    private void showSettingsDialog(boolean silent) {
        // stop listening to the MQTT object when opening the settings dialog
        MqttManager
                .getInstance()
                .getConnection()
                .setListener(null);

        this.showSettingsDialog(this.confListener);
    }

    private void showNotifListDialog() {
        FragmentTransaction transaction = this.getFragmentManager().beginTransaction();

        NotifListDialogFragment notifListDialogFragment = new NotifListDialogFragment();
        notifListDialogFragment.setListener(new NotificationListener());
        notifListDialogFragment.show(transaction, "notif-list-dialog");
    }

    private void showNotifDialog(Notification notification) {
        FragmentManager manager = this.getFragmentManager();
        // show notifications dialog
        String fragmentTag = "notif-dialog";
        NotificationDialogFragment notificationFragment;
        Bundle bundle;


        bundle = new Bundle();
        bundle.putParcelable("notification", notification);

        notificationFragment = (NotificationDialogFragment) manager.findFragmentByTag(
                fragmentTag);


        Log.d("Notif is null", NotificationDialogFragment.shown + " ");

        if (!NotificationDialogFragment.shown) {
            FragmentTransaction transaction = manager.beginTransaction();

            notificationFragment = new NotificationDialogFragment();
            notificationFragment.setListener(new NotificationListener());
            notificationFragment.setCancelable(false);
            notificationFragment.setArguments(bundle);
            notificationFragment.show(transaction, fragmentTag);
        } else if (notificationFragment != null) {
            notificationFragment.resetArguments(bundle);
        }
    }

    private void showBusyDialog(String message) {
        android.util.Log.v("ProgressDialog", "Showing dialog");
        if (!this.visible)
            return;

        if (this.progressPD == null) {
            this.progressPD = new ProgressDialog(this);
            this.progressPD.requestWindowFeature(Window.FEATURE_NO_TITLE);
            this.progressPD.setCancelable(false);
            this.progressPD.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            this.progressPD.setIndeterminate(true);
        }

        if (message == null)
            message = this.getString(R.string.message__state__fetching);

        this.progressPD.setMessage(message);

        if (!this.progressPD.isShowing() && !this.isFinishing() && !this.isDestroyed())
            this.progressPD.show();
    }

    private void hideBusyDialog() {
        if (this.progressPD == null)
            return;

        try {
            this.progressPD.dismiss();
        } catch (IllegalArgumentException ignored) {
        }
    }

    // TODO remove obsolete code
    private void fetchCurrentStates() {
        if (this.stateListener == null)
            this.stateListener = new CurrentStateListener();

        try {
            APIClient
                    .getInstance()
                    .getCurrentState(this.stateListener);
        } catch (AvarioException exception) {
            Log.d("Exception here", "Exception" + exception.getMessage());
            Log.d("Exception: ", exception.getMessage());
            PlatformUtil
                    .getErrorToast(this, exception)
                    .show();
        }
    }

    private void connectMQTT(String message) {
        Connectivity.identifyConnection(getApplicationContext());
        this.showBusyDialog(message);
        Log.d("Connect Mqtt", "Main Activity");
        super.connectMQTT(new MqttConnectionListener(), false);

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(this.bluetoothReceiver, filter);
    }

    /*
     ***********************************************************************************************
     * Inner Classes - Events
     ***********************************************************************************************
     */
    private class UIListener implements View.OnClickListener,
            View.OnTouchListener {
        @Override
        public void onClick(View view) {
            MainActivity self = MainActivity.this;
            int viewId = view.getId();

            switch (viewId) {
                case R.id.home:
                case R.id.cctv:
                case R.id.bolt:
                case R.id.temperature:
                    this.handleModeClicks(view);
                    break;

                case R.id.play:
                case R.id.prev:
                case R.id.next:
                    this.handleMediaClicks(view);
                    break;

                case R.id.volume:
                    this.handleVolumeClicks(view);
                    break;

                case R.id.devices:
                    self.updateDialFromSelections(self.toggleLeftView());
                    break;

                case R.id.notif:
                    self.showNotifListDialog();
                    break;
                case R.id.content:
                    handleInactive(self);
                    break;
            }
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            MainActivity self = MainActivity.this;
            int action = event.getAction();

            if (action == MotionEvent.ACTION_DOWN)
                self.handler.postDelayed(
                        self.settingsRunnable,
                        Config.getInstance().getSettingsHoldDelay()
                );

            else if (action == MotionEvent.ACTION_UP)
                self.handler.removeCallbacks(self.settingsRunnable);

            return false;
        }

        private void handleModeClicks(View view) {
            MainActivity self = MainActivity.this;

            // Close our web view on navigating to another page
            self.unloadWebView();

            self.activeModeIB.setActivated(false);
            self.activeModeIB = (ImageButton) view;
            self.activeModeIB.setActivated(true);

            self.roomSelector.setEnabled(view.getId() == R.id.home);

            switch (view.getId()) {
                case R.id.home:
                    self.activateModeHome();
                    break;

                case R.id.cctv:
                    self.activateModeCCTV();
                    break;

                case R.id.bolt:
                    self.activateModeEnergy();
                    break;

                case R.id.temperature:
                    self.activateModeClimate();

                    /*byte[] buffer;
                    try {
                        InputStream is = getAssets().open("notif.json");
                        int size = 0;

                        size = is.available();

                        buffer = new byte[size];
                        is.read(buffer);
                        is.close();


                        String myJson = new String(buffer, "UTF-8");


                        try {
                            JSONObject obj = new JSONObject(myJson);
                            Notification notification = new Notification(obj);


                            Intent intent = new Intent()
                                    .setAction(Constants.BROADCAST_NOTIF)
                                    .putExtra("notification", notification);

                            LocalBroadcastManager
                                    .getInstance(MainActivity.this)
                                    .sendBroadcast(intent);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }*/

                    break;
            }
        }

        private void handleMediaClicks(View view) {
            MainActivity self = MainActivity.this;
            Entity media = self.mediaList
                    .getAdapter()
                    .getSelected();
            if (media == null)
                return;

            // get appropriate directive name
            String directive;

            switch (view.getId()) {
                case R.id.play:
                    directive = "play_pause";
                    break;

                case R.id.next:
                    directive = "skip_next";
                    break;

                case R.id.prev:
                    directive = "skip_prev";
                    break;

                default:
                    return;
            }
            Log.d("media", media.id);
            NagleTimers.reset(
                    media.id,
                    new MediaNagleRunnable(media, directive),
                    EntityUtil.getMediaNagleDelay(media.data)
            );
        }

        private void handleVolumeClicks(View view) {
            MainActivity self = MainActivity.this;
            Entity media = self.mediaList
                    .getAdapter()
                    .getSelected();

            if (media == null)
                return;

            self.dialFragment.setVolumeEntity(media.id);
        }
    }

    private void handleInactive(MainActivity self) {
        if (isInActive) {
            self.homeIB.performClick();
            isInActive = false;
        }
    }

    private class WidgetListener implements RoomSelector.SelectionListener,
            ElementsBar.Listener,
            DevicesList.Listener,
            MediaList.Listener,
            MediaSourcesList.Listener {

        // region Room Selector
        @Override
        public void onRoomSelected(RoomSelector selector, RoomEntity room) {
            MainActivity self = MainActivity.this;

            if (self.activeModeIB != self.homeIB)
                return;
            self.updateForRoom(room);
            self.toggleLeftView(self.devicesList);  // force swap to devices list when media list shown in the screen
        }

        @Override
        public void onRoomMediaSelected(RoomSelector selector) {
            handleMediaSelected();
        }

        // endregion
        // region Elements Bar
        @Override
        public void onDialCommand(ElementsBar view, Entity entity) {
            MainActivity self = MainActivity.this;
            JSONArray devicesJSON;

            try {
                devicesJSON = entity.data.getJSONArray("dial_devices");
            } catch (JSONException exception) {
                PlatformUtil
                        .getErrorToast(self, new AvarioException(
                                Constants.ERROR_STATE_MISSINGKEY,
                                exception
                        ))
                        .show();

                return;
            }

            self.dialFragment.setEntities(devicesJSON);
        }

        @Override
        public void onListCommand(ElementsBar view, Entity entity) {
            MainActivity self = MainActivity.this;
            JSONArray devicesJSON;

            Log.d("onlist", "command " + entity.selected);
            devicesJSON = entity.data.optJSONArray("list_devices");

            if (devicesJSON == null)
                return;

            self.updateDevices(StateArray.getInstance(), devicesJSON);
            self.updateDefaultsForEntity(entity);
            if (entity.selected) {
                devicesList.setVisibility(View.VISIBLE);
                mediaList.setVisibility(View.GONE);
            } else {
                self.updateForRoom(self.roomSelector.getSelectedRoom());
            }

        }
        // endregion

        // region Devices List
        @Override
        public void onSelectionsCleared(DevicesList source) {
            MainActivity.this.dialFragment
                    .setEnabled(false);
        }

        @Override
        public void onSelectionsChanged(DevicesList source) {
            MainActivity self = MainActivity.this;

            self.dialFragment.setEnabled(true);
            self.updateDialFromSelections(source);
        }
        // endregion

        // region MediaList
        @Override
        public void onMediaListUpdated() {
            MainActivity self = MainActivity.this;
            if (self.mediaList.getVisibility() == View.VISIBLE)
                self.updateDialFromSelections(self.mediaList);
        }

        @Override
        public void onMediaSelected(Entity entity) {
            MainActivity.this.updateDialFromSelections(MainActivity.this.mediaList);
            enableMediaPlay();
        }

        @Override
        public void onMediaState(String state) {
            if (state.equals(Constants.ENTITY_MEDIA_STATE_STOPPED)) {
                disableMediaPlay();
                Log.d("State", "Idle");
            }

        }
        // endregion

        // region MediaSourcesList
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onMediaSourceSelected(String name, String appId) {
            MainActivity self = MainActivity.this;
            Intent intent = self
                    .getPackageManager()
                    .getLaunchIntentForPackage(appId);

            if (intent != null) {
                final ActivityManager am = (ActivityManager) getSystemService(
                        Context.ACTIVITY_SERVICE);

                DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Activity.DEVICE_POLICY_SERVICE);
                if (am.getLockTaskModeState() ==
                        ActivityManager.LOCK_TASK_MODE_LOCKED) {
                    stopLockTask();
                    Config config = Config.getInstance();
                    config.setIsKiosk(false);
                }

                self.startActivity(intent);
            } else if (URLUtil.isValidUrl(appId)) {
                loadWebView(appId);
                drawer.closeDrawers();

            } else {
                String[] exceptionArgs = new String[]{name};

                PlatformUtil
                        .getErrorToast(self, new AvarioException(
                                Constants.ERROR_APP_NOT_INSTALLED,
                                null,
                                exceptionArgs
                        ))
                        .show();
            }
        }
        // endregion
    }

    private void handleMediaSelected() {
        MediaList mediaList = MainActivity.this.mediaList;
        MediaSourcesList sourcesList = MainActivity.this.sourcesList;

        List<RoomEntity> rooms = MainActivity.this.roomSelector
                .getAdapter()
                .getMediaSelections();
        mediaList.setup(rooms);
        sourcesList.setup(rooms);
    }

    private class SettingsListener extends BaseActivity.SettingsListener {
        @Override
        public void onSettingsChange() {
            MainActivity self = MainActivity.this;

            self.startInactivityCountdown();
            self.loadFromStateArray();
            if (config.isTablet()) {
                self.loadTabAssets();
            } else {
                self.loadPhoneAssets();
            }
            self.showBusyDialog(null);
            self.fetchCurrentStates();
            self.initFCMTopics();
        }

        @Override
        public void onDialogDetached() {
            MainActivity self = MainActivity.this;

            super.onDialogDetached();

            MqttManager manager = MqttManager.getInstance();
            Log.d("On Detach", "detach");
            if (manager.isConnected()) {
                isHasWifi = true;
                manager
                        .getConnection()
                        .setListener(self.mqttListener);
            } else {
                ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                if (!mWifi.isConnected()) {
                    handleNoWifi();
                } else {
                    isHasWifi = true;
                    self.connectMQTT(self.getString(R.string.message__mqtt__connecting));
                }
            }
        }
    }

    private class NotificationListener implements NotificationDialogFragment.Listener,
            NotifListDialogFragment.Listener {

        @Override
        public void onDialogDetached() {
            MainActivity self = MainActivity.this;

            self.isNotifListVisible();
            self.checkNotifications();
        }

        @Override
        public void onSelectedItem(Notification notification) {
            MainActivity self = MainActivity.this;
            self.showNotifDialog(notification);
        }
    }

    private class MqttConnectionListener implements MqttConnection.Listener {
        @Override
        public void onConnection(MqttConnection connection, boolean reconnection) {
            android.util.Log.v("ProgressDialog", "onConnection");
        }

        @Override
        public void onConnectionFailed(MqttConnection connection, AvarioException exception) {
            android.util.Log.v("ProgressDialog", "onConnectionFailed");
            // when connection fails, continue connecting to MQTT and show errors
            MainActivity self = MainActivity.this;
            String message;

            final ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            // max retries not reached: silently retry
            if (connection.getRetryCount() < 10)
                return;

            // express to user the error
            try {
                message = StateArray
                        .getInstance()
                        .getErrorMessage(exception.getCode());
            } catch (NullPointerException nullE) {
                message = null;
            }

            if (!mWifi.isConnected()) {
                handleNoWifi();
            } else {
                Connectivity.identifyConnection(getApplicationContext());
                showBusyDialog(getString(R.string.message__mqtt__connecting));
                Log.d("Connect Mqtt", "Main Activity");
                connectMQTT(getString(R.string.message__mqtt__connecting));
            }

        }

        @Override
        public void onDisconnection(MqttConnection connection, AvarioException exception) {
            android.util.Log.v("ProgressDialog", "onDisconnection");
            MainActivity self = MainActivity.this;

            final ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            final NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);


            if (exception == null || self.settingsOpened)
                return; // disconnected with no errors or we just shouldn't care because settings is opened

            String message;

            try {
                message = StateArray
                        .getInstance()
                        .getErrorMessage(exception.getCode());
            } catch (NullPointerException e) {
                message = null;
            }
            if (!mWifi.isConnected()) {
                handleNoWifi();
            } else {
                //self.connectMQTT(message);

                Connectivity.identifyConnection(getApplicationContext());
                showBusyDialog(getString(R.string.message__mqtt__connecting));
                Log.d("Connect Mqtt", "Main Activity");
                connectMQTT(getString(R.string.message__mqtt__connecting));

            }
        }

        @Override
        public void onSubscription(MqttConnection connection) {
            android.util.Log.v("ProgressDialog", "onSubscription");
            MainActivity self = MainActivity.this;
            self.showBusyDialog(null);
            self.fetchCurrentStates();

            MqttManager manager = MqttManager.getInstance();

            if (manager.isConnected()) {
                manager
                        .getConnection()
                        .setListener(mqttListener);

                loadFromStateArray();
                fetchCurrentStates();

                showBusyDialog(null);
            } else if (!settingsOpened) {
                connectMQTT(getString(R.string.message__mqtt__connecting));
            }

            if (BluetoothScanner.getInstance().isEnabled())
                BluetoothScanner.getInstance().scanLeDevice(true);
        }

        @Override
        public void onSubscriptionError(MqttConnection connection, AvarioException exception) {
            android.util.Log.v("ProgressDialog", "onSubscriptionError");
            PlatformUtil
                    .getErrorToast(MainActivity.this, exception)
                    .show();
        }

        @Override
        public void onStatusChanged(MqttConnection connection, MqttConnection.Status previous, MqttConnection.Status current) {
            android.util.Log.v("ProgressDialog", "onStatusChanged");
        }
    }

    private void handleNoWifi() {

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


        showBusyDialog("Connecting to WiFi...");

        final StateArray states = StateArray.getInstance(this.getApplicationContext());
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
                                                    wifi.setWifiEnabled(true);
                                                    timerIsStarted = true;
                                                    countDownTimer.start();
                                                    if (alert11.isShowing()) {
                                                        alert11.cancel();
                                                    }

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
                                                    dialog.cancel();
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

    private class CurrentStateListener extends APIRequestListener<JSONArray> {
        private int retries;
        private int retriesMax;

        CurrentStateListener() {
            super("connection", null);

            this.retries = 0;
            this.retriesMax = 5;
        }

        @Override
        public void onDone(JSONArray response, VolleyError error) {
        }

        @Override
        public void onResponse(final JSONArray response) {
            super.onResponse(response);

            Handler handler = Application.workHandler;
            if (handler != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            StateArray.getInstance()
                                    .updateFromHTTP(response)
                                    .broadcastChanges(null, StateArray.FROM_HTTP);
                        } catch (AvarioException exception) {
                            CurrentStateListener.this.reportError(exception);
                        }

                    }
                });

                MainActivity.this.hideBusyDialog();
            }
        }

        // TODO call super.onErrorResponse() and then override super.forceTimerExpire()
        @Override
        public void onErrorResponse(final VolleyError error) {
            Log.i(TAG, "Request failed..", error);
            this.onDone(null, error);

            Application.mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    MainActivity activity = MainActivity.this;
                    CurrentStateListener self = CurrentStateListener.this;
                    int code;

                    Log.d(TAG, "Request failed", error);

                    if (error.networkResponse != null)
                        code = PlatformUtil.responseCodeToErrorCode("2", error.networkResponse.statusCode);
                    else if (error instanceof TimeoutError)
                        code = Constants.ERROR_CURRENTSTATE_TIMEOUT;
                    else if (error instanceof ParseError)
                        code = Constants.ERROR_CURRENTSTATE_PARSE;
                    else if (error instanceof NetworkError)
                        code = Constants.ERROR_CURRENTSTATE_NETWORK;
                    else
                        code = Constants.ERROR_CURRENTSTATE_HTTP_SERVER;

                    if (code != Constants.ERROR_CURRENTSTATE_NETWORK && self.retries >= self.retriesMax) {
                        self.retries = 0;
                        self.reportError(new AvarioException(code, error));
                    } else if (code != Constants.ERROR_CURRENTSTATE_NETWORK)
                        self.reportError(new AvarioException(code, error));
                    self.retries++;
                    activity.fetchCurrentStates();
                }
            }, 2000);
        }

        private void reportError(AvarioException exception) {
            String message;

            try {
                message = StateArray
                        .getInstance()
                        .getErrorMessage(exception.getCode());
            } catch (NullPointerException nullEx) {
                message = null;
            }

            MainActivity.this.showBusyDialog(message);
        }
    }

    private class MediaAPIsListener extends APIRequestListener<String> {
        public MediaAPIsListener(String timerId, String[] entityIds) {
            super(timerId, entityIds);
        }
    }

    private class AppBGCallback extends AssetUtil.BackgroundCallback {
        private AppBGCallback(View view) {
            super(view);
        }

        @Override
        public void onFailure(AvarioException exception) {
            AssetUtil.toDrawable(MainActivity.this, R.array.bg__app__light, this);
        }
    }

    /**
     * Catch any bootstrap changes. This is mostly called during push notification
     * to get new bootstrap.
     */
    private class BootstrapListener extends APIClient.BootstrapListener {
        @Override
        public void onResponse(JSONObject response) {
            Log.i(TAG, "Bootstrap received!");
            try {
                StateArray
                        .getInstance()
                        .setData(response)
                        .save();
            } catch (AvarioException e) {
                e.printStackTrace();
            }
            if (StateArray.getInstance().tempReboot) {
                SystemUtil.rebootApp(MainActivity.this);
            } else {
                Toast.makeText(MainActivity.this,
                        getString(R.string.message_new_bootstrap),
                        Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onErrorResponse(VolleyError error) {

        }
    }


    /*
             ***********************************************************************************************
             * Receivers
             ***********************************************************************************************
             */
    private class NotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            MainActivity self = MainActivity.this;
            String action = intent.getAction();
            if (action.equals(Constants.BROADCAST_BOOTSTRAP_CHANGED)) {
                StateArray.getInstance().tempReboot = intent.getBooleanExtra("reboot", false);
                APIClient
                        .getInstance(getApplicationContext())
                        .getBootstrapJSON(new BootstrapListener(), intent.getStringExtra("bs_name"));
            } else {
                Notification notification = intent.getParcelableExtra("notification");
                EventBus.getDefault().post(new ShowNotification(notification));
            }
        }
    }

    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        BluetoothScanner.getInstance().scanLeDevice(false);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        BluetoothScanner.getInstance().scanLeDevice(true);
                        break;
                }
            }
        }
    }

    /*
     ***********************************************************************************************
     * Inner Classes - Timers
     ***********************************************************************************************
     */
    // region timers
    private class MediaNagleRunnable implements Runnable {
        private Entity media;
        private String directive;

        private MediaNagleRunnable(Entity media, String directive) {
            this.media = media;
            this.directive = directive;
        }

        @Override
        public void run() {
            // get and execute api directive
            JSONObject specJSON;

            try {
                try {
                    specJSON = new JSONObject(
                            this.media.data
                                    .getJSONObject("controls")
                                    .getJSONObject(this.directive)
                                    .toString()
                    );
                    specJSON.put("timeout", EntityUtil.getNagleDelay(this.media.data));
                } catch (JSONException exception) {
                    throw new AvarioException(
                            Constants.ERROR_STATE_MISSINGKEY,
                            exception,
                            new Object[]{
                                    String.format("%s.controls.%s",
                                            this.media.id,
                                            this.directive
                                    )
                            }
                    );
                }

                APIClient
                        .getInstance()
                        .executeRequest(specJSON, this.media.id, this.media.id, new MediaAPIsListener(
                                this.media.id,
                                new String[]{this.media.id}
                        ));
            } catch (final AvarioException exception) {
                Application.mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        PlatformUtil
                                .getErrorToast(MainActivity.this, exception)
                                .show();
                    }
                });
            }
        }
    }

    private class InactivityRunnable implements Runnable {
        @Override
        public void run() {
            MainActivity self = MainActivity.this;

            if (self.activeModeIB != self.homeIB) {
                self.homeIB.performClick();
            }

            self.toggleLeftView(self.mediaList);
            self.updateDialFromSelections(self.mediaList);

            if (isMediaAvailable) {
                try {
                    RoomEntity roomSelection = MainActivity.this.roomSelector
                            .getAdapter().getRoomSelection();
                    JSONArray devicesJSON = roomSelection.data.getJSONArray("list_devices");
                    List<String> entityIds = new ArrayList<>();

                    for (int i = 0; i < devicesJSON.length(); i++) {
                        String device = devicesJSON.getString(i);
                        if (device.contains("light.")) {
                            entityIds.add(device);
                        }
                    }
                    dialFragment.setEntities(entityIds);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            isInActive = true;
        }
    }

    // endregion timers

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
                active ? new String[]{getPackageName()} : new String[]{});

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

    private Observer<ResponseBody> observerUpdate = new Observer<ResponseBody>() {
        @Override
        public void onCompleted() {

        }

        @Override
        public void onError(Throwable e) {

        }

        public void onNext(ResponseBody responseBody) {
            writeResponseBodyToDisk(responseBody);
        }
    };


    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean writeResponseBodyToDisk(ResponseBody body) {

        // todo change the file location/name according to your needs
        try {
            File futureStudioIconFile = new File(getExternalFilesDir(null) + File.separator + "app-release.apk");

            File outputFile = new File(futureStudioIconFile, "app-release.apk");

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                inputStream = body.byteStream();
                installPackage(this, inputStream);
                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }

        } catch (IOException e) {
            return false;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void installPackage(Context context, InputStream inputStream)
            throws IOException {
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        int sessionId = packageInstaller.createSession(new PackageInstaller
                .SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL));
        PackageInstaller.Session session = packageInstaller.openSession(sessionId);

        long sizeBytes = 0;

        OutputStream out = null;
        out = session.openWrite("my_app_session", 0, sizeBytes);

        int total = 0;
        byte[] buffer = new byte[65536];
        int c;
        while ((c = inputStream.read(buffer)) != -1) {
            total += c;
            out.write(buffer, 0, c);
        }
        session.fsync(out);
        inputStream.close();
        out.close();

        // fake intent
        IntentSender statusReceiver = null;
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                1337111117, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        session.commit(pendingIntent.getIntentSender());
        session.close();
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
            Log.d("MainActivity", "Connected");
            countDownTimer.cancel();
            isHasWifi = true;
            //connectMQTT(this.getString(R.string.message__mqtt__connecting));
            Connectivity.identifyConnection(getApplicationContext());
            showBusyDialog(getString(R.string.message__mqtt__connecting));
            connectMQTT(getString(R.string.message__mqtt__connecting));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUpdateTrigger(final TriggerUpdate triggerUpdate) {

        final StateArray states = StateArray.getInstance(getBaseContext());

        String updateAvailableMessage = "";
        countDownTimer.cancel();
        try {
            updateAvailableMessage = states.getStringMessage("0x03040");
        } catch (AvarioException e) {
            e.printStackTrace();
        }

        builderUpdate.setTitle("New update Available!");
        builderUpdate.setMessage(updateAvailableMessage);
        builderUpdate.setCancelable(false);

        builderUpdate.setPositiveButton(
                "Update",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        updatePresenter.getUpdate(observerUpdate);
                    }
                });

        builderUpdate.setNegativeButton(
                "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        if (alertUpdate == null) {
            alertUpdate = builderUpdate.create();
        }

        alertUpdate.setButton(AlertDialog.BUTTON_NEUTRAL, "Don't show again", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                config.setToIgnore(triggerUpdate.getVersion());
                dialog.cancel();
            }
        });

        if (!alertUpdate.isShowing()) {
            alertUpdate.show();
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUpdateProgress(UpdateDownload updateDownload) {
        progressDownload.setMessage("Downloading Update...");

        if (!progressDownload.isShowing()) {
            progressDownload.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDownload.show();
        }
        progressDownload.setProgress(updateDownload.getProgress());

        if (updateDownload.getProgress() == 100) {
            progressDownload.cancel();
            progressDownload = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
            progressDownload.setMessage("Installing Update...");
            progressDownload.show();
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onOpenNotif(ShowNotification notification) {
        if (settingsOpened)
            return;

        if (!isNotifListVisible())
            showNotifDialog(notification.getNotification());
    }
}
