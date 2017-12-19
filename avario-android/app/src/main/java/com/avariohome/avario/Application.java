package com.avariohome.avario;


import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;

import com.android.volley.VolleyLog;
import com.avariohome.avario.api.APIClient;
import com.avariohome.avario.core.BluetoothScanner;
import com.avariohome.avario.core.Config;
import com.avariohome.avario.core.NotificationArray;
import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.core.WorkerThread;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.mqtt.MqttManager;
import com.avariohome.avario.service.KioskService;
import com.avariohome.avario.util.RefStringUtil;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crash.FirebaseCrash;
import com.squareup.leakcanary.LeakCanary;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;


/**
 * <p>This class extends the Application class
 * to manage global data for the app.</p>
 * <p>
 * Created by aeroheart-c6  on 20/12/2016.
 */
public class Application extends android.app.Application {
    public static final String TAG = "Avario/Application";
    public static Handler mainHandler;
    public static Handler workHandler;

    public static WorkerThread worker;
    public static TickerRunnable tickerRunnable;

    private static Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;

    /**
     * Tries to start the worker thread just in case it was killed off before
     */
    public static void startWorker(Context context) {
        if (Application.workHandler != null && Application.worker != null && Application.worker.getState() != Thread.State.TERMINATED) {
            // Clear any instance of tickerRunnable to avoid duplicate
            // and initialize to make sure ticker is running as intended.
            if (Application.workHandler != null) {
                Application.workHandler.removeCallbacks(Application.tickerRunnable);
            }
            Application.tickerRunnable = null;
            Application.tickerRunnable = new TickerRunnable(context, Application.workHandler);
            Application.tickerRunnable.tick();
            return;
        }
        Application.worker = new WorkerThread();
        Application.worker.start();

        Application.workHandler = Application.worker.getHandler();

        Application.tickerRunnable = new TickerRunnable(context, Application.workHandler);
        Application.tickerRunnable.tick();
    }

    public static void stopWorker() {
        Application.workHandler.removeCallbacks(Application.tickerRunnable);
        Application.tickerRunnable = null;
        Application.workHandler = null;
        Application.mainHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        if (BuildConfig.DEBUG) {
            if (LeakCanary.isInAnalyzerProcess(this)) {
                // This process is dedicated to LeakCanary for heap analysis.
                // You should not init your app in this process.
                return;
            }
            LeakCanary.install(this);
            FirebaseCrash.setCrashCollectionEnabled(false);
        } else {
            FirebaseCrash.setCrashCollectionEnabled(true);
        }
        VolleyLog.setTag("AvarioVolley");

        Application.mainHandler = new Handler(Looper.getMainLooper());
        // Initialize all important singletons with the application context
        MqttManager.getInstance();
        RefStringUtil.initJSInterpreter();

        StateArray.getInstance(this);
        APIClient.getInstance(this);
        Config.getInstance(this);

        NotificationArray
                .getInstance()
                .setContext(this);

        BluetoothScanner
                .getInstance()
                .setContext(this);
        startService(new Intent(this, KioskService.class));

        /*mDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);*/
    }


    private Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, final Throwable e) {
            /*final AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
            new Thread() {
                @Override
                public void run() {
                    Looper.prepare();
                    builder.setTitle("Sorry...!");
                    builder.create();
                    builder.setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    System.exit(0);
                                }
                            });
                    builder.setPositiveButton("Report",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    Intent sendIntent = new Intent(
                                            Intent.ACTION_SEND);
                                    String subject = "Your App crashed! Fix it!";
                                    StringBuilder body = new StringBuilder("Yoddle");
                                    body.append('\n').append('\n');
                                    body.append(e.getMessage()).append('\n')
                                            .append('\n');
                                    // sendIntent.setType("text/plain");
                                    sendIntent.setType("message/rfc822");
                                    sendIntent.putExtra(Intent.EXTRA_EMAIL,
                                            new String[]{"coderzheaven@gmail.com"});
                                    sendIntent.putExtra(Intent.EXTRA_TEXT,
                                            body.toString());
                                    sendIntent.putExtra(Intent.EXTRA_SUBJECT,
                                            subject);
                                    sendIntent.setType("message/rfc822");
                                    getApplicationContext().startActivity(sendIntent);
                                    System.exit(0);
                                }
                            });
                    builder.setMessage("Oops,Your application has crashed");
                    builder.show();
                    Looper.loop();
                }
            }.start();*/
        }
    };

    private static class TickerRunnable implements Runnable {
        private Context context;
        private Handler handler;
        private Intent intent;

        private TickerRunnable(Context context, Handler handler) {
            this.context = context;
            this.handler = handler;
            this.intent = new Intent(Constants.BROADCAST_MEDIA);
        }

        private void tick() {
            this.handler.postDelayed(this, 1000);
        }

        @Override
        public void run() {
            try {
                JSONObject mediaRootJSON = StateArray.getInstance().getMediaEntities();

                if (mediaRootJSON == null)
                    return;

                Iterator<String> keys = mediaRootJSON.keys();

                while (keys.hasNext()) {
                    String key = keys.next();

                    try {
                        JSONObject mediaJSON = mediaRootJSON.getJSONObject(key);
                        String state = mediaJSON
                                .getJSONObject("new_state")
                                .getString("state");

                        if (state.equals(Constants.ENTITY_MEDIA_STATE_PLAYING))
                            mediaJSON.put(
                                    "media_position_live",
                                    mediaJSON.getDouble("media_position_live") + 1
                            );

//                        StringBuilder builder = new StringBuilder()
//                            .append("EntityId: ")
//                            .append(mediaJSON.optString("entity_id"))
//                            .append("\n")
//                            .append("State: ")
//                            .append(mediaJSON.optJSONObject("new_state").optString("state"))
//                            .append("\n")
//                            .append("Position: ")
//                            .append(mediaJSON.optDouble("media_position_live", -1.0));
//
//                        Log.d(Constants.LOGTAG_EMERGENCY, builder.toString());
                    } catch (JSONException exception) {
                        exception.printStackTrace();
                    }
                }
            } catch (AvarioException exception) {
                exception.printStackTrace();
            }

            LocalBroadcastManager
                    .getInstance(this.context)
                    .sendBroadcast(this.intent);

            this.tick();
        }
    }
}
