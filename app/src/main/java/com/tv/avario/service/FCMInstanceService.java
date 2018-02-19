package com.tv.avario.service;


import com.avario.core.Config;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.tv.avario.api.APIClient;
import com.tv.avario.util.Log;


/**
 * Created by aeroheart-c6 on 07/07/2017.
 */
public class FCMInstanceService extends FirebaseInstanceIdService {
    private static final String TAG = "Avario/FCMInstanceService";
    public static final String TIMER_ID = "fcm-instance";

    @Override
    public void onTokenRefresh() {
        String token = FirebaseInstanceId.getInstance().getToken();

        Log.d(TAG, "Token:\n" + token);

        if (token != null) {
            Config.getInstance().setFCM(token);
        }

        APIClient
                .getInstance()
                .postFCMToken(token);
    }
}
