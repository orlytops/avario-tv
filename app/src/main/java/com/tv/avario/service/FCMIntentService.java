package com.tv.avario.service;


import android.app.IntentService;
import android.content.Intent;

import com.tv.avario.util.Log;
import com.google.firebase.iid.FirebaseInstanceId;

import java.io.IOException;


/**
 * Created by aeroheart-c6 on 07/07/2017.
 */
public class FCMIntentService extends IntentService {
    private static final String TAG = "Avario/FCMIntentService";

    public static final String ACTION_DELETE = "com.avar.io.ACTION_DELETE";
    public static final String ACTION_REFRESH = "com.avar.io.ACTION_REFRESH";

    public FCMIntentService() {
        super("FCMIntentService");
    }



    @Override
    public void onHandleIntent(Intent intent) {
        FirebaseInstanceId firebase = FirebaseInstanceId.getInstance();
        String action = intent.getAction();

        if (action.equals(FCMIntentService.ACTION_DELETE) ||
            action.equals(FCMIntentService.ACTION_REFRESH))
            try {
                firebase.deleteInstanceId();

                Log.d(TAG, "Instance ID deleted");
            }
            catch (IOException exception) {
                Log.d(TAG, "Instance ID Delete Error", exception);
            }

        if (action.equals(FCMIntentService.ACTION_REFRESH))
            firebase.getToken();
    }
}
