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
import com.avariohome.avario.util.Log;
import com.avariohome.avario.util.RefStringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;


/**
 * <p>This class extends the Application class
 * to manage global data for the app.</p>
 *
 * Created by aeroheart-c6  on 20/12/2016.
 */
public class Application extends android.app.Application {
    public static final String TAG = "Avario/Application";
    public static Handler mainHandler;
    public static Handler workHandler;

    public static WorkerThread worker;

    /**
     * Tries to start the worker thread just in case it was killed off before
     */
    public static void startWorker(Context context) {
        if (Application.worker != null && Application.worker.getState() != Thread.State.TERMINATED)
            return;

        try {
            Application.worker = new WorkerThread();
            Application.worker.start();

            Application.workHandler = Application.worker.getHandler();
        }
        catch (IllegalThreadStateException exception) {
            // worker thread already started
        }

        TickerRunnable runnable;

        runnable = new TickerRunnable(context, Application.workHandler);
        runnable.tick();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        VolleyLog.setTag("AvarioVolley");

        Application.startWorker(this);
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
    }


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
                    }
                    catch (JSONException exception) {}
                }
            }
            catch (AvarioException exception) {}

            LocalBroadcastManager
                .getInstance(this.context)
                .sendBroadcast(this.intent);

            this.tick();
        }
    }
}
