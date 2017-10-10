package com.avariohome.avario.fragment;


import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.admin.DevicePolicyManager;
import android.app.admin.SystemUpdatePolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.ParseError;
import com.android.volley.VolleyError;
import com.avariohome.avario.Constants;
import com.avariohome.avario.R;
import com.avariohome.avario.activity.MainActivity;
import com.avariohome.avario.api.APIClient;
import com.avariohome.avario.core.Config;
import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.mqtt.MqttConnection;
import com.avariohome.avario.mqtt.MqttManager;
import com.avariohome.avario.service.AvarioReceiver;
import com.avariohome.avario.util.AssetUtil;
import com.avariohome.avario.util.BlindAssetLoader;
import com.avariohome.avario.util.Connectivity;
import com.avariohome.avario.util.Log;
import com.google.firebase.messaging.FirebaseMessaging;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


/**
 * TODO invalidate cache in Picasso using Picasso.with().invalidate(). Iterate through R.arrays
 * <p>
 * Created by aeroheart-c6 on 17/01/2017.
 */

public class SettingsDialogFragment extends DialogFragment {
    private static final String TAG = "Avario/Settings";

    private LinearLayout workingRL;
    private EditText hostET;
    private EditText portET;
    private EditText usernameET;
    private EditText passwordET;
    private CheckBox secureCB;
    private CheckBox kioskCheck;

    private Button saveB;
    private Button dropB;
    private Button refreshB;
    private Button enableUninstallButton;
    private TextView workingTV;
    private TextView errorTV;
    private TextView versionText;

    private BatchAssetLoaderTask task;
    private MqttConnection.Listener mqttListener;
    private Listener listener;
    private Snapshot snapshot;
    private Config config;
    private int positiveId;
    private int negativeId;
    private int neutralId;


    private ComponentName mAdminComponentName;
    private DevicePolicyManager mDevicePolicyManager;
    private PackageManager mPackageManager;

    public SettingsDialogFragment() {
        super();

        this.positiveId = View.generateViewId();
        this.negativeId = View.generateViewId();
        this.neutralId = View.generateViewId();

        this.mqttListener = new MqttConnectionListener();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder;
        AlertDialog dialog;

        builder = new AlertDialog.Builder(this.getActivity())
                .setTitle(R.string.setting__title)
                .setView(this.setupViews(LayoutInflater.from(this.getActivity()), null));

        builder
                .setPositiveButton(R.string.setting__save, null)
                .setNegativeButton(R.string.setting__discard, null)
                .setNeutralButton(R.string.setting__refresh, null);

        dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setOnShowListener(new DialogListener());

        return dialog;
    }

    @Override
    public void onPause() {
        super.onPause();

        if (this.task != null)
            this.task.cancel(true);
    }

    @Override
    public void onDetach() {
        if (this.listener != null)
            this.listener.onDialogDetached();

        super.onDetach();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private View setupViews(LayoutInflater inflater, ViewGroup container) {
        View view = inflater.inflate(R.layout.fragment__settings, container, false);

        this.config = Config.getInstance(view.getContext());
        this.setupSnapshot();

        this.workingRL = (LinearLayout) view.findViewById(R.id.working__holder);
        this.workingTV = (TextView) this.workingRL.findViewById(R.id.working__label);

        this.errorTV = (TextView) view.findViewById(R.id.error);

        this.hostET = (EditText) view.findViewById(R.id.setting__host_ip);
        this.portET = (EditText) view.findViewById(R.id.setting__host_port);
        this.usernameET = (EditText) view.findViewById(R.id.setting__username);
        this.passwordET = (EditText) view.findViewById(R.id.setting__password);
        this.secureCB = (CheckBox) view.findViewById(R.id.setting__ssl);
        versionText = (TextView) view.findViewById(R.id.text_version);
        kioskCheck = (CheckBox) view.findViewById(R.id.check_kiosk);

        kioskCheck.setChecked(config.isKiosk());

        mAdminComponentName = AvarioReceiver.getComponentName(getActivity());
        mDevicePolicyManager = (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        mPackageManager = getActivity().getPackageManager();

        kioskCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (mDevicePolicyManager.isDeviceOwnerApp(getActivity().getPackageName())) {
                        setDefaultCosuPolicies(true);
                    }

                    getActivity().startLockTask();
                    Config config = Config.getInstance();
                    config.setIsKiosk(true);
                } else {
                    if (mDevicePolicyManager.isDeviceOwnerApp(getActivity().getPackageName())) {
                        setDefaultCosuPolicies(false);
                    }
                    Config config = Config.getInstance();
                    config.setIsKiosk(false);
                    getActivity().stopLockTask();
                    getActivity().getPackageManager().clearPackagePreferredActivities(getActivity().getPackageName());
                    if (mDevicePolicyManager.isDeviceOwnerApp(getActivity().getPackageName())) {
                        mDevicePolicyManager.clearDeviceOwnerApp("com.avariohome.avario");
                    }
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    startActivity(intent);
                }
            }
        });

        try {
            PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            String version = pInfo.versionName;
            //versionText.setText("Version: avario_v" + version + "b" + pInfo.versionCode);
            versionText.setText("Version: avario_orly_v0.3.2");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (this.config.isSet()) {
            this.hostET.setText(this.config.getHttpHost());
            this.portET.setText(this.config.getHttpPort());
            this.usernameET.setText(this.config.getUsername());
            this.passwordET.setText(this.config.getPassword());
            this.secureCB.setChecked(this.config.isHttpSSL());
        }
        startKiosk();
        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void startKiosk() {
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                mAdminComponentName = AvarioReceiver.getComponentName(getActivity());
                mDevicePolicyManager = (DevicePolicyManager) getActivity().getSystemService(
                        Context.DEVICE_POLICY_SERVICE);
                mPackageManager = getActivity().getPackageManager();
                if (mDevicePolicyManager.isDeviceOwnerApp(getActivity().getPackageName())) {
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
                        if (mDevicePolicyManager.isLockTaskPermitted(getActivity().getPackageName())) {
                            final ActivityManager am = (ActivityManager) getActivity().getSystemService(
                                    Context.ACTIVITY_SERVICE);
                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        if (am.getLockTaskModeState() ==
                                                ActivityManager.LOCK_TASK_MODE_NONE) {
                                            if (mDevicePolicyManager.isDeviceOwnerApp(getActivity().getPackageName())) {
                                                getActivity().startLockTask();
                                                kioskCheck.setChecked(true);
                                                Config config = Config.getInstance();
                                                config.setIsKiosk(true);
                                            }
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
    private void provisionOwner() {
        DevicePolicyManager manager =
                (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName componentName = AvarioReceiver.getComponentName(getActivity());


        if (!manager.isAdminActive(componentName)) {
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
            startActivityForResult(intent, 0);
            return;
        }


        if (manager.isDeviceOwnerApp(getActivity().getPackageName())) {
            manager.setLockTaskPackages(componentName, new String[]{getActivity().getPackageName()});
        }
    }

    private void setupSnapshot() {
        Snapshot snapshot;

        snapshot = new Snapshot();
        snapshot.initial = !this.config.isSet();
        snapshot.secure = this.config.isHttpSSL();
        snapshot.host = this.config.getHttpHost();
        snapshot.port = this.config.getHttpPort();
        snapshot.username = this.config.getUsername();
        snapshot.password = this.config.getPassword();

        Log.d(TAG, String.format(
                "Initial: %s\n" +
                        "Secure: %s\n" +
                        "Host: %s\n" +
                        "Port: %s\n" +
                        "Username: %s\n" +
                        "Password: %s",
                String.valueOf(snapshot.initial),
                String.valueOf(snapshot.secure),
                snapshot.host,
                snapshot.port,
                snapshot.username,
                snapshot.password
        ));

        this.snapshot = snapshot;
    }

    private void applySnapshot() {
        if (this.snapshot.initial)
            this.config.clear();
        else {
            this.config.setHttpSSL(this.snapshot.secure);
            this.config.setHttpHost(this.snapshot.host);
            this.config.setHttpPort(this.snapshot.port);
            this.config.setUsername(this.snapshot.username);
            this.config.setPassword(this.snapshot.password);
        }
    }

    private void setEnabled(boolean enabled) {
        this.hostET.setEnabled(enabled);
        this.portET.setEnabled(enabled);
        this.usernameET.setEnabled(enabled);
        this.passwordET.setEnabled(enabled);
        this.secureCB.setEnabled(enabled);

        this.saveB.setEnabled(enabled);
        this.dropB.setEnabled(enabled);
        this.refreshB.setEnabled(enabled);
    }

    private void toggleWorking(boolean show) {
        this.workingRL.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    private void toggleError(boolean show, AvarioException exception) {
        String message;

        if (exception == null)
            message = "";
        else {
            message = StateArray.getInstance().getErrorMessage(exception.getCode());
            message = String.format(message, exception.getMessageArguments());
        }

        this.toggleError(show, message);
    }

    private void toggleError(boolean show, String message) {
        this.errorTV.setVisibility(show ? View.VISIBLE : View.GONE);
        this.errorTV.setText(message);
    }

    private void deleteCaches() {
        this.unsubscribeFCM();
        this.deleteAssetCache(this.getActivity().getCacheDir());
        this.deleteBootstrapCache();

        Toast
                .makeText(this.getActivity(), R.string.setting__toast__cleared, Toast.LENGTH_SHORT)
                .show();

        this.config.setResourcesFetched(false);

        if (this.config.isSet()) {
            this.toggleWorking(true);
            this.loadBootstrap();
        } else
            this.setEnabled(true);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void deleteAssetCache(File dir) {
        Log.i(TAG, "deleting asset cache...");

        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();

            for (String child : children)
                deleteAssetCache(new File(dir, child));

            dir.delete();
        } else if (dir != null && dir.isFile())
            dir.delete();
    }

    private void deleteBootstrapCache() {
        Log.i(TAG, "deleting bootstrap...");
        StateArray.getInstance().delete();
    }

    private void dropChanges() {
        Log.i(TAG, "Cancelling new changes...");

        this.applySnapshot();
        this.setEnabled(true);

        if (this.config.isSet() && this.config.isResourcesFetched())
            this.dismiss();
        else
            Toast
                    .makeText(this.getActivity(), R.string.setting__toast__empty, Toast.LENGTH_SHORT)
                    .show();
    }

    private void saveChanges() {
        Log.i(TAG, "Attempting to save settings...");

        MqttManager manager = MqttManager.getInstance();

        String host = this.hostET.getText().toString(),
                port = this.portET.getText().toString(),
                username = this.usernameET.getText().toString(),
                password = this.passwordET.getText().toString();
        boolean secure = this.secureCB.isChecked();

        if (host.isEmpty() || port.isEmpty() ||
                username.isEmpty() ||
                password.isEmpty()) {
            Toast.makeText(this.getActivity(), R.string.setting__toast__empty, Toast.LENGTH_SHORT)
                    .show();

            this.toggleWorking(false);
            this.setEnabled(true);
            return;
        }

        host = host.replaceAll("^.+?://", "");

        if (!this.config.isResourcesFetched()
                || !manager.isConnected()
                || !host.equals(this.config.getHttpHost())
                || !port.equals(this.config.getHttpPort())
                || !username.equals(this.config.getUsername())
                || !password.equals(this.config.getPassword())
                || secure != this.config.isHttpSSL()) {

            this.config.setHttpHost(host);
            this.config.setHttpPort(port);
            this.config.setHttpSSL(secure);
            this.config.setUsername(username);
            this.config.setPassword(password);

            if (this.snapshot.initial) {
                this.config.setResourcesFetched(false);
                this.toggleWorking(true);
                this.loadBootstrap();
            } else
                this.deleteCaches();
        } else {
            this.toggleWorking(false);
            this.setEnabled(true);
            this.dismiss();
        }
    }

    private void loadBootstrap() {
        Log.i(TAG, "Loading bootstrap JSON...");
        this.workingTV.setText(this.getString(R.string.setting__working, "(Bootstrap)"));

        APIClient
                .getInstance(this.getActivity().getApplicationContext())
                .getBootstrapJSON(new BootstrapListener());
    }

    private void loadAssets() {
        Context context = this.getActivity();
        Resources res = context.getResources();
        String pkg = context.getPackageName();

        Pattern pattern = Pattern.compile("^(ic|bg)__.+");
        Field[] fields = R.array.class.getDeclaredFields();

        List<String> urls = new ArrayList<>();

        Log.i(TAG, "Pre-fetching all assets...");
        this.workingTV.setText(this.getString(R.string.setting__working, "(Assets)"));

        for (Field field : fields) {
            if (!pattern.matcher(field.getName()).matches())
                continue;

            Log.d(TAG, String.format("Preparing: %s", field.getName()));

            String[] paths;

            paths = res.getStringArray(res.getIdentifier(field.getName(), "array", pkg));
            paths = AssetUtil.toAbsoluteURLs(context, paths);

            urls.addAll(Arrays.asList(paths));
        }

        this.task = new BatchAssetLoaderTask(this.getActivity());
        this.task.execute(urls);
    }

    private void sendFCMToken() {
        APIClient
                .getInstance()
                .postFCMToken(null);
    }

    /**
     * Handles calling the listener to notify that it wants to connect to the MQTT server and handle
     * any errors within this fragment.
     * <p>
     * This method REQUIRES that the listener is not null. Or else it will be stuck </3
     */
    private void connectMQTT() {
        Log.i(TAG, "Connecting to MQTT server....");

        this.workingTV.setText(this.getString(R.string.setting__working, "(MQTT Attempt #1)"));

        try {
            if (this.listener == null)
                throw new AvarioException(Constants.ERROR_APP_SETTINGS, null);

            this.listener.attemptMQTT(this.mqttListener);
        } catch (JSONException | MqttException exception) {
            int errorCode = exception instanceof MqttException
                    ? Constants.ERROR_MQTT_CONNECTION
                    : Constants.ERROR_MQTT_CONFIGURATION;

            this.applySnapshot();

            this.toggleWorking(false);
            this.toggleError(true, new AvarioException(errorCode, exception));
            this.setEnabled(true);
        } catch (AvarioException exception) {
            this.toggleError(true, exception);
        }
    }

    private void unsubscribeFCM() {
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
                fcm.unsubscribeFromTopic(topics.getString(index));
        } catch (JSONException exception) {
        }
    }


    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /*
     ***********************************************************************************************
     * Inner Classes - Listeners
     ***********************************************************************************************
     */
    private class DialogListener implements DialogInterface.OnShowListener {
        @Override
        public void onShow(DialogInterface dialog) {
            SettingsDialogFragment self = SettingsDialogFragment.this;
            ClickListener listener = new ClickListener();
            AlertDialog alert = (AlertDialog) dialog;
            Button button;

            button = alert.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setId(self.positiveId);
            button.setOnClickListener(listener);
            self.saveB = button;

            button = alert.getButton(AlertDialog.BUTTON_NEGATIVE);
            button.setId(self.negativeId);
            button.setOnClickListener(listener);
            self.dropB = button;

            button = alert.getButton(AlertDialog.BUTTON_NEUTRAL);
            button.setId(self.neutralId);
            button.setOnClickListener(listener);
            self.refreshB = button;
        }
    }

    private class ClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            SettingsDialogFragment self = SettingsDialogFragment.this;

            self.setEnabled(false);
            self.toggleError(false, "");

            if (view.getId() == self.positiveId)
                self.saveChanges();
            else if (view.getId() == self.negativeId)
                self.dropChanges();
            else
                self.deleteCaches();
        }
    }

    private class BootstrapListener extends APIClient.BootstrapListener {
        @Override
        public void onResponse(JSONObject response) {
            SettingsDialogFragment self = SettingsDialogFragment.this;

            Log.i(TAG, "Bootstrap received!");

            StateArray
                    .getInstance()
                    .setData(response);
            Connectivity.identifyConnection(getActivity());

            self.sendFCMToken();
            self.loadAssets();
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            SettingsDialogFragment self = SettingsDialogFragment.this;
            AvarioException exception = new AvarioException(
                    error instanceof AuthFailureError ? Constants.ERROR_BOOTSTRAP_AUTHENTICATION :
                            error instanceof ParseError ? Constants.ERROR_BOOTSTRAP_INVALID :
                                    Constants.ERROR_BOOTSTRAP_UNREACHABLE,

                    error
            );

            self.applySnapshot();

            self.toggleWorking(false);
            self.toggleError(true, exception);
            self.setEnabled(true);
        }
    }


    /*
     ***********************************************************************************************
     * Inner Classes - Subtypes
     ***********************************************************************************************
     */
    private class BatchAssetLoaderTask extends BlindAssetLoader {
        BatchAssetLoaderTask(Context context) {
            super(context, null);
        }

        @Override
        protected void onCancelled(Void result) {
            SettingsDialogFragment self = SettingsDialogFragment.this;

            try {
                self.applySnapshot();

                self.toggleWorking(false);
                self.setEnabled(true);
            } catch (NullPointerException exception) {
                Log.d(TAG, "Cancelling the task....", exception);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            SettingsDialogFragment self = SettingsDialogFragment.this;

            if (this.exception == null)
                self.connectMQTT();
            else {
                self.applySnapshot();

                self.toggleWorking(false);
                self.toggleError(true, this.exception);
                self.setEnabled(true);
            }
        }
    }

    private class MqttConnectionListener implements MqttConnection.Listener {
        @Override
        public void onConnection(MqttConnection connection, boolean reconnection) {
        }

        @Override
        public void onConnectionFailed(MqttConnection connection, AvarioException exception) {
            SettingsDialogFragment self = SettingsDialogFragment.this;

            if (connection.getRetryCount() < connection.getRetryMax())
                // max retries still not reached
                self.workingTV.setText(self.getString(
                        R.string.setting__working,
                        "(MQTT Attempt #" + (connection.getRetryCount() + 1) + ")"
                ));
            else {
                // max retries reached
                self.applySnapshot();
                self.toggleWorking(false);
                self.toggleError(true, exception);
                self.setEnabled(true);
            }
        }

        @Override
        public void onDisconnection(MqttConnection connection, AvarioException exception) {
            if (exception != null)
                return;

            SettingsDialogFragment.this.connectMQTT();
        }

        @Override
        public void onSubscription(MqttConnection connection) {
            SettingsDialogFragment self = SettingsDialogFragment.this;
            StateArray states = StateArray.getInstance();

            connection.setListener(null);

            try {
                states.save();
            } catch (AvarioException exception) {
                self.toggleError(true, exception);
            }

            states.broadcastChanges(null, StateArray.FROM_HTTP);

            self.toggleWorking(false);
            self.setEnabled(true);
            self.config.setResourcesFetched(true);

            if (self.listener != null)
                self.listener.onSettingsChange();

            self.dismiss();
        }

        @Override
        public void onSubscriptionError(MqttConnection connection, AvarioException exception) {
        }

        @Override
        public void onStatusChanged(MqttConnection connection, MqttConnection.Status previous, MqttConnection.Status current) {
        }
    }

    private class Snapshot {
        String host;
        String port;
        String username;
        String password;

        boolean secure;
        boolean initial;
    }


    /*
     ***********************************************************************************************
     * Inner Classes - Interfaces
     ***********************************************************************************************
     */
    public interface Listener {
        void attemptMQTT(MqttConnection.Listener listener) throws JSONException,
                MqttException;

        void onSettingsChange();

        void onDialogDetached();
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
                active ? new String[]{getActivity().getPackageName()} : new String[]{});

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MAIN);
        intentFilter.addCategory(Intent.CATEGORY_HOME);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);

        if (active) {
            // set Cosu activity as home intent receiver so that it is started
            // on reboot
            mDevicePolicyManager.addPersistentPreferredActivity(
                    mAdminComponentName, intentFilter, new ComponentName(
                            getActivity().getPackageName(), MainActivity.class.getName()));
        } else {
            mDevicePolicyManager.clearPackagePersistentPreferredActivities(
                    mAdminComponentName, getActivity().getPackageName());
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
}
