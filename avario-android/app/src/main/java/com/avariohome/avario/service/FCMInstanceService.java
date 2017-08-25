package com.avariohome.avario.service;


import com.avariohome.avario.api.APIClient;
import com.avariohome.avario.api.APIRequestListener;
import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.util.Log;
import com.avariohome.avario.util.PlatformUtil;
import com.avariohome.avario.util.RefStringUtil;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by aeroheart-c6 on 07/07/2017.
 */
public class FCMInstanceService extends FirebaseInstanceIdService{
    private static final String TAG = "Avario/FCMInstanceService";
    public static final String TIMER_ID = "fcm-instance";

    @Override
    public void onTokenRefresh() {
        String token = FirebaseInstanceId.getInstance().getToken();

        Log.d(TAG, "Token:\n" + token);

        APIClient
            .getInstance()
            .postFCMToken(token);
    }
}
