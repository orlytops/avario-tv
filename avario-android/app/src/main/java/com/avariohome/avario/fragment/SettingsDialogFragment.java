package com.avariohome.avario.fragment;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.admin.SystemUpdatePolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
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
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.ParseError;
import com.android.volley.VolleyError;
import com.avariohome.avario.Constants;
import com.avariohome.avario.R;
import com.avariohome.avario.api.APIClient;
import com.avariohome.avario.api.component.DaggerUserComponent;
import com.avariohome.avario.api.component.UserComponent;
import com.avariohome.avario.apiretro.models.Version;
import com.avariohome.avario.apiretro.services.UpdateService;
import com.avariohome.avario.core.Config;
import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.home.MainActivity;
import com.avariohome.avario.mqtt.MqttConnection;
import com.avariohome.avario.mqtt.MqttManager;
import com.avariohome.avario.presenters.UpdatePresenter;
import com.avariohome.avario.service.AvarioReceiver;
import com.avariohome.avario.util.AssetUtil;
import com.avariohome.avario.util.BlindAssetLoader;
import com.avariohome.avario.util.Connectivity;
import com.avariohome.avario.util.Log;
import com.avariohome.avario.util.SystemUtil;
import com.avariohome.avario.util.Validator;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.messaging.FirebaseMessaging;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;

import okhttp3.ResponseBody;
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

    @Inject
    UpdateService userService;
    private UserComponent userComponent;

    private LinearLayout workingRL;
    private EditText hostET;
    private EditText portET;
    private EditText usernameET;
    private EditText passwordET;
    private CheckBox secureCB;
    private CheckBox kioskCheck;
    private ImageButton settingsButton;
    private Button updateButton;

    private Button clearAssetsB, getAssetsB, saveB, cancelB, getBootstrapB;
    private Button enableUninstallButton;
    private TextView workingTV, errorTV, versionText, bootstrapSource;

    private EditText noneFatalMessage;
    private Button sendNoneFatalMessage, forceCrash;

    private ScrollView mainScrollView;
    private RelativeLayout relativeLayoutBootStrap, relativeLayoutCache;

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


    private AlertDialog.Builder builderUpdate;
    private AlertDialog alertUpdate;


    private boolean reboot = false;

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

        userComponent = DaggerUserComponent.builder().build();
        userComponent.inject(this);

        builder = new AlertDialog.Builder(this.getActivity())
                .setTitle(R.string.setting__title)
                .setView(this.setupViews(LayoutInflater.from(this.getActivity()), null));

//        builder
//                .setPositiveButton(R.string.setting__save, null)
//                .setNegativeButton(R.string.setting__discard, null)
//                .setNeutralButton(R.string.setting_get_new_bootstrap, null);

        dialog = builder.create();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setOnShowListener(new DialogListener());
        return dialog;
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private boolean verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }

        return permission == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onPause() {
        super.onPause();
        android.util.Log.v("DialogSettings", "OnPause");
        if (this.task != null)
            this.task.cancel(true);
    }

    @Override
    public void onDetach() {
        android.util.Log.v("DialogSettings", "OnDetach");
        this.config.restore();
        if (this.listener != null)
            this.listener.onDialogDetached();

        super.onDetach();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private View setupViews(LayoutInflater inflater, ViewGroup container) {
        builderUpdate = new AlertDialog.Builder(getActivity());

        View view = inflater.inflate(R.layout.fragment__settings, container, false);

        this.config = Config.getInstance(view.getContext());
        this.setupSnapshot();

        this.mainScrollView = (ScrollView) view.findViewById(R.id.svMain);

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
        clearAssetsB = (Button) view.findViewById(R.id.btnClearAssets);
        saveB = (Button) view.findViewById(R.id.btnSave);
        cancelB = (Button) view.findViewById(R.id.btnCancel);
        getAssetsB = (Button) view.findViewById(R.id.btnDownloadAssets);
        getBootstrapB = (Button) view.findViewById(R.id.btnDownloadBootstrap);
        bootstrapSource = (TextView) view.findViewById(R.id.tvBootstrapSource);
        settingsButton = (ImageButton) view.findViewById(R.id.button_settings);
        updateButton = (Button) view.findViewById(R.id.button_update);

        noneFatalMessage = (EditText) view.findViewById(R.id.etNoneFatalMessage);
        sendNoneFatalMessage = (Button) view.findViewById(R.id.btnNoneFatal);
        forceCrash = (Button) view.findViewById(R.id.btnForceCrash);

        relativeLayoutBootStrap = (RelativeLayout) view.findViewById(R.id.rlBootstrap);
        relativeLayoutCache = (RelativeLayout) view.findViewById(R.id.rlCache);

        kioskCheck.setChecked(config.isKiosk());
        clearAssetsB.setOnClickListener(new ClickListener());
        saveB.setOnClickListener(new ClickListener());
        cancelB.setOnClickListener(new ClickListener());
        getAssetsB.setOnClickListener(new ClickListener());
        getBootstrapB.setOnClickListener(new ClickListener());
        sendNoneFatalMessage.setOnClickListener(new ClickListener());
        forceCrash.setOnClickListener(new ClickListener());

        //set View.VISIBLE on kiosk mode
        kioskCheck.setVisibility(View.VISIBLE);

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
                    if (mDevicePolicyManager.isDeviceOwnerApp("com.avariohome.avario")) {
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

            //versionText.setText("Version: avario_master_v" + version);
            versionText.setText("Version: avario_orly_v" + version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (this.config.isSet()) {
            this.hostET.setText(this.config.getHttpHost());
            this.portET.setText(this.config.getHttpPort());
            this.usernameET.setText(this.config.getUsername());
            this.passwordET.setText(this.config.getPassword());
            this.secureCB.setChecked(this.config.isHttpSSL());
            clearAssetsB.setVisibility(View.VISIBLE);
            relativeLayoutBootStrap.setVisibility(View.VISIBLE);
            relativeLayoutCache.setVisibility(View.VISIBLE);
            bootstrapSource.setText("Source: " + this.config.getHttpHost() + ":" + this.config.getHttpPort());
        }
        startKiosk();

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
            }
        });

        updateButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (!verifyStoragePermissions(getActivity())) {
                    return;
                }

                currentVersion();
            }
        });

        return view;
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


    private void currentVersion() {

        final UpdatePresenter updatePresenter = new UpdatePresenter(userService);
        final StateArray states = StateArray.getInstance(getActivity());

        String latestVersion = "";
        try {
            latestVersion = states.getStringMessage("0x03050");
        } catch (AvarioException e) {
            e.printStackTrace();
        }

        final String finalLatestVersion = latestVersion;
        updatePresenter.getVersion(new Observer<Version>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Version version) {
                if (needsUpdate(getActivity(), version.getVersion())) {
                    builderUpdate.setTitle("New update Available!");
                    builderUpdate.setMessage(getResources().getString(R.string.update_availabe));

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
                } else {
                    builderUpdate.setTitle("No update available!");
                    builderUpdate.setMessage(finalLatestVersion);
                    builderUpdate.setPositiveButton("", null);
                    builderUpdate.setNegativeButton(
                            "Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                }

                builderUpdate.setCancelable(false);
                alertUpdate = builderUpdate.create();
                if (!alertUpdate.isShowing()) {
                    alertUpdate.show();
                }
            }
        });
    }

    private boolean needsUpdate(Context context, String version) {
        try {

            //get current Version Code
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            String currentVersionName = info.versionName;

            if (!version.equals(currentVersionName)) {
                //version string not the same, version is NOT up to date

                Boolean updateNeeded = false;
                String[] currentVersionCodeArray = currentVersionName.split("\\.");
                String[] storeVersionCodeArray = version.split("\\.");

                int maxLength = currentVersionCodeArray.length;
                if (storeVersionCodeArray.length > maxLength) {
                    maxLength = storeVersionCodeArray.length;
                }

                for (int i = 0; i < maxLength; i++) {

                    try {
                        if (Integer.parseInt(storeVersionCodeArray[i]) > Integer.parseInt(currentVersionCodeArray[i])) {
                            updateNeeded = true;
                            continue;
                        }
                    } catch (IndexOutOfBoundsException e) {
                        //store version code length > current version length = version needs to be updated
                        //if store version length is shorter, the if-statement already did the job
                        if (storeVersionCodeArray.length > currentVersionCodeArray.length) {
                            updateNeeded = true;
                        }
                    }
                }

                if (updateNeeded) {
                    return true;
                }

            } else {
                return false;
            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean writeResponseBodyToDisk(ResponseBody body) {

        // todo change the file location/name according to your needs
        try {
            File futureStudioIconFile = new File(getActivity().getExternalFilesDir(null) + File.separator + "app-release.apk");

            File outputFile = new File(futureStudioIconFile, "app-release.apk");

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                workingTV.setText("Installing update...");
                installPackage(getActivity(), inputStream);
               /* outputStream = new FileOutputStream(futureStudioIconFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.d(TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();*/
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

                /*final ActivityManager am = (ActivityManager) getActivity().getSystemService(
                        Context.ACTIVITY_SERVICE);

                if (am.getLockTaskModeState() ==
                        ActivityManager.LOCK_TASK_MODE_LOCKED) {
                    getActivity().stopLockTask();
                    Config config = Config.getInstance();
                    config.setIsKiosk(false);
                }*/
                /*Log.d("Path", futureStudioIconFile.getAbsolutePath());

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(futureStudioIconFile.getAbsolutePath())),
                        "application/vnd.android.package-archive");
                getActivity().startActivityForResult(intent, 0);*/
                //installPackage(getActivity(), inputStream);
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

    private static IntentSender createIntentSender(Context context, int sessionId) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                new Intent("com.avariohome.avario.INSTALL_COMPLETE"),
                0);
        return pendingIntent.getIntentSender();
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
                                    } catch (Exception ignore) {
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
        this.cancelB.setEnabled(enabled);
        this.getBootstrapB.setEnabled(enabled);
        this.clearAssetsB.setEnabled(enabled);
        this.kioskCheck.setEnabled(enabled);

        this.relativeLayoutCache.setEnabled(enabled);
        this.relativeLayoutBootStrap.setEnabled(enabled);
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

    private void reloadBootstrap() {
        if (this.config.isSet()) {
            this.toggleWorking(true);
            this.loadBootstrap();
        } else
            this.setEnabled(true);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private boolean deleteAssetCache(File dir) {
        Log.i(TAG, "deleting asset cache...");

        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();

            Log.i(TAG, "Length " + children.length);

            for (String child : children)
                deleteAssetCache(new File(dir, child));

            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        }
        return false;
    }

    private void deleteBootstrapCache() {
        Log.i(TAG, "deleting bootstrap...");
        if (StateArray.getInstance().delete()) {
            this.config.setBootstrapFetched(false);
        }
    }

    private void cancelChanges() {
        Log.i(TAG, "Cancelling new changes...");
        if (this.config.getHttpHost() != null && this.config.getHttpPort() != null) {
            this.dismiss();
            this.config.restore();

            this.applySnapshot();
        }
        this.setEnabled(true);
    }

    private void saveChanges() {
        Log.i(TAG, "Attempting to save settings...");

        MqttManager manager = MqttManager.getInstance();

        String host = this.hostET.getText().toString(),
                port = this.portET.getText().toString(),
                username = this.usernameET.getText().toString(),
                password = this.passwordET.getText().toString();
        boolean secure = this.secureCB.isChecked();

        if (!Validator.isValidHost(this.hostET) || !Validator.isValidPort(this.portET)) {
            this.toggleWorking(false);
            this.setEnabled(true);
            return;
        }

        host = host.replaceAll("^.+?://", "");

        //boolean isResourceFetched = !this.config.isResourcesFetched();
        boolean isConnected = !manager.isConnected();
        boolean isHostEqual = !host.equals(this.config.getHttpHost());
        boolean isPortEqual = !port.equals(this.config.getHttpPort());
        boolean isUsernameEqual = !username.equals(this.config.getUsername());
        boolean isPasswordEqual = !password.equals(this.config.getPassword());
        boolean isSecure = secure != this.config.isHttpSSL();

        if (/*isResourceFetched
                || */isConnected
                || isHostEqual
                || isPortEqual
                || isUsernameEqual
                || isPasswordEqual
                || isSecure) {

            this.config.setHttpHost(host);
            this.config.setHttpPort(port);
            this.config.setHttpSSL(secure);
            this.config.setUsername(username);
            this.config.setPassword(password);

            setScrollViewFocus(ScrollView.FOCUS_DOWN);
            if (this.snapshot.initial) {
                this.toggleWorking(true);
                this.loadBootstrap();
            } else
                this.reloadBootstrap();
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
                .getBootstrapJSON(new BootstrapListener(), null);
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
                .postFCMToken(Config.getInstance().getFCM());
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
        } catch (JSONException ignore) {
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
//            SettingsDialogFragment self = SettingsDialogFragment.this;
//            ClickListener listener = new ClickListener();
//            AlertDialog alert = (AlertDialog) dialog;
//            Button button;
//
//            button = alert.getButton(AlertDialog.BUTTON_POSITIVE);
//            button.setId(self.positiveId);
//            button.setOnClickListener(listener);
//            self.saveB = button;
//
//            button = alert.getButton(AlertDialog.BUTTON_NEGATIVE);
//            button.setId(self.negativeId);
//            button.setOnClickListener(listener);
//            self.dropB = button;
//
//            button = alert.getButton(AlertDialog.BUTTON_NEUTRAL);
//            button.setId(self.neutralId);
//            button.setOnClickListener(listener);
//            self.refreshB = button;
        }
    }

    private class ClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            SettingsDialogFragment self = SettingsDialogFragment.this;
            if (view.getId() == R.id.btnSave) {
                self.setEnabled(false);
                self.toggleError(false, "");
                self.saveChanges();
            } else if (view.getId() == R.id.btnCancel) {
                self.setEnabled(false);
                self.toggleError(false, "");
                self.cancelChanges();
            } else if (view.getId() == R.id.btnClearAssets) {
                self.setEnabled(false);
                if (!isFileEmpty(self.getActivity().getCacheDir())) {
                    Toast.makeText(self.getActivity(),
                            self.deleteAssetCache(self.getActivity().getCacheDir())
                                    ? "All assets deleted." : "Failed to delete assets.", Toast.LENGTH_SHORT).show();
                }
                self.setEnabled(true);
            } else if (view.getId() == R.id.btnDownloadAssets) {
                if (config.isSet()) {
                    self.setEnabled(false);
                    self.toggleError(false, "");
                    self.toggleWorking(true);
                    self.deleteAssetCache(self.getActivity().getCacheDir());
                    self.loadAssets();
                }
            } else if (view.getId() == R.id.btnDownloadBootstrap) {
                if (config.isSet()) {
                    Toast.makeText(self.getActivity(), "Getting new bootstrap", Toast.LENGTH_SHORT).show();
                    reboot = true;
                    self.setEnabled(false);
                    self.toggleError(false, "");
                    self.reloadBootstrap();
                }
            } else if (view.getId() == R.id.btnNoneFatal) {
                noneFatalMessage.setText("Test firebase");
                if (noneFatalMessage.getText() == null
                        || noneFatalMessage.getText().toString().isEmpty()) {
                    Toast.makeText(view.getContext(), "None Fatal Message should not be empty", Toast.LENGTH_SHORT).show();
                } else {
                    FirebaseCrash.report(new Throwable(noneFatalMessage.getText().toString()));
                    Toast.makeText(view.getContext(), "None Fatal crash " + noneFatalMessage.getText().toString()
                            + "Has been sent to firebase.", Toast.LENGTH_SHORT).show();
                }
            } else if (view.getId() == R.id.btnForceCrash) {
                String val = null;
                if (val.equals("")) {

                }
            }
        }
    }

    private boolean isFileEmpty(File file) {
        File[] content = file.listFiles();
        return content.length == 0;
    }

    private void setScrollViewFocus(final int focus) {
        mainScrollView.post(new Runnable() {
            @Override
            public void run() {
                mainScrollView.fullScroll(focus);
            }
        });
    }

    private class BootstrapListener extends APIClient.BootstrapListener {
        @Override
        public void onResponse(JSONObject response) {
            SettingsDialogFragment self = SettingsDialogFragment.this;

            Log.i(TAG, "Bootstrap received!");

            self.unsubscribeFCM();
            self.deleteBootstrapCache();
            StateArray
                    .getInstance()
                    .setData(response);
            Connectivity.identifyConnection(getActivity());
            self.config.setBootstrapFetched(true);
            if (reboot) {
                try {
                    StateArray.getInstance().save();
                } catch (AvarioException e) {
                    e.printStackTrace();
                }
                SystemUtil.rebootApp(self.getActivity());
            } else {
                self.sendFCMToken();
                self.connectMQTT();
            }
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

            self.config.restore();
            self.applySnapshot();
            self.toggleWorking(false);
            self.toggleError(true, exception);
            self.setEnabled(true);
        }
    }

    private class UpdateListener extends APIClient.UpdateListener {
        @Override
        public void onResponse(JSONObject response) {
            SettingsDialogFragment self = SettingsDialogFragment.this;

            Log.i(TAG, "Bootstrap received!");

            self.unsubscribeFCM();
            self.deleteBootstrapCache();
            StateArray
                    .getInstance()
                    .setData(response);
            Connectivity.identifyConnection(getActivity());
            self.config.setBootstrapFetched(true);
            if (reboot) {
                try {
                    StateArray.getInstance().save();
                } catch (AvarioException e) {
                    e.printStackTrace();
                }
                SystemUtil.rebootApp(self.getActivity());
            } else {
                self.sendFCMToken();
                self.connectMQTT();
            }
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

            self.config.restore();
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
                self.toggleWorking(false);
                self.setEnabled(true);
            } catch (NullPointerException exception) {
                Log.d(TAG, "Cancelling the task....", exception);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            SettingsDialogFragment self = SettingsDialogFragment.this;

            if (this.exception != null) {
                self.toggleWorking(false);
                self.toggleError(true, this.exception);
                self.setEnabled(true);
            } else {
                self.toggleWorking(false);
                self.setEnabled(true);
                Toast.makeText(getContext(), "Assets downloaded", Toast.LENGTH_SHORT).show();
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
                self.config.apply();
            } catch (AvarioException exception) {
                self.toggleError(true, exception);
            }

            states.broadcastChanges(null, StateArray.FROM_HTTP);

            self.toggleWorking(false);
            self.setEnabled(true);

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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void clearMyRestrictions(Context context) {
        setUserRestriction(UserManager.DISALLOW_INSTALL_APPS, false);
        setUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES, false);
    }

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
                active ? new String[]{getActivity().getPackageName(), "com.google.android.youtube"} : new String[]{});

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
