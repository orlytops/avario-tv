package com.avariohome.avario.api;


import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.avariohome.avario.Constants;
import com.avariohome.avario.core.APITimers;
import com.avariohome.avario.core.Config;
import com.avariohome.avario.core.StateArray;
import com.avariohome.avario.exception.AvarioException;
import com.avariohome.avario.service.FCMInstanceService;
import com.avariohome.avario.util.Log;
import com.avariohome.avario.util.PlatformUtil;
import com.avariohome.avario.util.RefStringUtil;
import com.google.firebase.iid.FirebaseInstanceId;

import org.eclipse.paho.client.mqttv3.internal.websocket.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;


/**
 * Created by aeroheart-c6 on 10/01/2017.
 */
public class APIClient {
    public static final String TAG = "Avario/APIClient";

    private static APIClient instance = null;

    public static APIClient getInstance() {
        return APIClient.instance;
    }

    /**
     * Retrieves the singleton instance of this class. It is important to pass the application
     * context here because this object will be alive throughout the duration of the app.
     *
     * @param context the application context
     * @return instance of the APIClient singleton
     */
    public static APIClient getInstance(Context context) {
        if (APIClient.instance == null) {
            APIClient.instance = new APIClient(context);
        }

        return APIClient.instance;
    }

    public static boolean isValidRequestSpec(JSONObject specJSON) {
        return specJSON.has("url") && specJSON.has("method");
    }

    public static HostnameVerifier getDevHostnameVerifier() {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
    }


    private RequestQueue queue;

    private APIClient(Context context) {
        HostnameVerifier verifier = APIClient.getDevHostnameVerifier();

        if (verifier == null) {
            this.queue = Volley.newRequestQueue(context);
        } else {
            this.queue = Volley.newRequestQueue(context, new HurlStack() {
                @Override
                protected HttpURLConnection createConnection(URL url) throws IOException {
                    HttpURLConnection connection = super.createConnection(url);
                    if (connection instanceof HttpsURLConnection) {
                        HttpsURLConnection httpsConn;

                        httpsConn = (HttpsURLConnection) connection;
                        httpsConn.setHostnameVerifier(APIClient.getDevHostnameVerifier());
                    }

                    return connection;
                }
            });
        }
    }

    /**
     * Sends the request over to Volley's request queue for execution
     *
     * @param request
     */
    public void sendRequest(Request request) {
        if (request == null) {
            return;
        }

        Log.i(TAG, String.format("Executing Request: %s %s",
                request.getMethod() == Request.Method.POST ? "POST" :
                        request.getMethod() == Request.Method.GET ? "GET" :
                                request.getMethod() == Request.Method.PUT ? "PUT" : "DELETE",
                request.getUrl()
                        + " " + request.getBodyContentType()));

        Log.i("RequestUrl", request.toString());
        try {
            Log.i(TAG, "Request Payload: " + new String(request.getBody()));
            Log.i(TAG, "Request Payload Type: " + request.getBodyContentType());
        } catch (AuthFailureError | NullPointerException ignored) {
        }
        this.queue.add(request);

    }

    /**
     * Runs a single request. For now, this is only being used by the dial
     */
    public void executeRequest(JSONObject specJSON, String entityId, String lockId,
                               APIRequestListener<String> listener) throws AvarioException {
        Request request;

        if (APITimers.isLocked(lockId)) {
            return;
        }

        try {
            request = new StringAPIRequest(
                    APIRequest.RequestSpec.fromJSONSpec(specJSON),
                    listener
            );
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_API_OBJECTS,
                    exception,
                    new Object[]{entityId, 0}
            );
        }

        APITimers.lock(lockId);

        this.sendRequest(request);
    }

    public void executeRequest(JSONObject specJSON,
                               APIRequestListener<String> listener) throws AvarioException {
        Request request;


        try {
            request = new StringAPIRequest(
                    APIRequest.RequestSpec.fromJSONSpec(specJSON),
                    listener
            );
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_API_OBJECTS,
                    exception,
                    new Object[]{"", 0}
            );
        }

        this.sendRequest(request);
    }

    /**
     * Runs the requests in sequence. For following the instructions in the StateArray when an
     * entity demands to have multiple requests executed when interacted upon.
     * <p>
     * Each item of the requestSpec JSONArray must be a JSONObject containing the following
     * properties:
     * URL - String
     * method - String
     * Payload - String
     *
     * @param requestsSpec JSONArray of JSONObjects similar to the bootstrap JSON. Contents are
     *                     described in the method description
     * @param listener     listener when all the requests are done
     */
    public void sequenceRequests(JSONArray requestsSpec, String entityId, String lockId,
                                 APIMultiListener<String> listener) throws AvarioException {
        if (APITimers.isLocked(lockId)) {
            return;
        }

        if (listener == null) {
            listener = new APIMultiListener<>(entityId, new String[]{entityId});
        }

        for (int index = 0, limit = requestsSpec.length(); index < limit; index++) {
            try {
                listener.add(new StringAPIRequest(
                        APIRequest.RequestSpec.fromJSONSpec(requestsSpec.getJSONObject(index)),
                        listener
                ));
            } catch (JSONException exception) {
                throw new AvarioException(
                        Constants.ERROR_STATE_API_OBJECTS,
                        exception,
                        new Object[]{entityId, index}
                );
            }
        }

        try {
            APITimers.lock(lockId);
            this.sendRequest(listener.pop());
        } catch (NoSuchElementException exception) {
            APITimers.unlock(lockId);
        }
    }

    public void getBootstrapJSON(BootstrapListener listener, String path) {
        final Config config = Config.getInstance();
        JsonObjectRequest request;

        StateArray stateArray = StateArray.getInstance();

        String domain = "https://192.168.0.18:22443/";
        Log.d("Domain", config.getHttpDomain());
        try {
            domain = stateArray.getHTTPHost("ip1");
        } catch (AvarioException e) {
            if (config.getHttpHost() != null) {
                domain = config.getHttpDomain();
            }
            e.printStackTrace();
        }

        if (domain == null) {
            domain = "https://192.168.0.18:22443/";
        }

        // Path can be null. If null or empty then
        // get default bootstrap path.
        if (path == null || path.isEmpty()) {
            path = config.getBootstrapURL();
        } else {
            path = String.format("%s%s",
                    domain,
                    path);
        }

        request = new JsonObjectRequest(
                Request.Method.GET,
                path,
                null,
                listener,
                listener
        ) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();

                headers.put("Authorization", String.format("Basic %s", Base64.encode(String.format(
                        "%s:%s",
                        config.getUsername(),
                        config.getPassword()
                ))));

                return headers;
            }
        };
        request.setShouldCache(false);
        request.setRetryPolicy(new DefaultRetryPolicy(5000, 2, 1.5f));

        this.sendRequest(request);
    }

    public void getCurrentState(APIRequestListener<JSONArray> listener) throws AvarioException {
        StateArray states = StateArray.getInstance();
        JSONArrayAPIRequest request;
        final Config config = Config.getInstance();

        try {
            request = new JSONArrayAPIRequest(
                    APIRequest.RequestSpec.fromJSONSpec(states.getCurrentStateRequest()),
                    listener
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();

                    headers.put("Authorization", String.format("Basic %s", Base64.encode(String.format(
                            "%s:%s",
                            config.getUsername(),
                            config.getPassword()
                    ))));

                    return headers;
                }
            };
            request.setShouldCache(false);
            request.setRetryPolicy(new DefaultRetryPolicy(5000, 2, 1.5f));
        } catch (JSONException exception) {
            throw new AvarioException(
                    Constants.ERROR_STATE_API_OBJECTS,
                    exception
            );
        }

        this.sendRequest(request);
    }

    public void postFCMToken(String token) {
        token = token != null
                ? token
                : FirebaseInstanceId.getInstance().getToken();

        if (token == null) {
            return;
        }

        // get request spec
        JSONObject requestJSON;

        try {
            requestJSON = StateArray
                    .getInstance()
                    .getFCMRequest();
        } catch (AvarioException ignored) {
            // TODO notify application of error. But err...how?
            return;
        }

        // replace refs
        Map<String, String> mapping;

        mapping = new HashMap<>();
        mapping.put("token", token);
        mapping.put("id", PlatformUtil.getTabletId());

        if (requestJSON == null) {
            return;
        }

        try {
            requestJSON.put("payload", RefStringUtil.replaceMarkers(
                    RefStringUtil.extractMarkers(requestJSON.getString("payload"), null),
                    mapping
            ));
        } catch (JSONException exception) {
            return;
        }

        // send the request
        try {
            APIClient.getInstance().executeRequest(
                    requestJSON,
                    FCMInstanceService.TIMER_ID,
                    FCMInstanceService.TIMER_ID,
                    new FCMAPIListener()
            );
        } catch (AvarioException ignored) {
        }
    }

    /*
     ***********************************************************************************************
     * Inner Classes - Listeners
     ***********************************************************************************************
     */
    private class FCMAPIListener extends APIRequestListener<String> {
        public FCMAPIListener() {
            super(
                    FCMInstanceService.TIMER_ID,
                    new String[]{FCMInstanceService.TIMER_ID}
            );
        }

        protected void forceTimerExpire() {
        }

        protected void startTimer() {
        }
    }

    public static abstract class BootstrapListener implements Response.Listener<JSONObject>,
            Response.ErrorListener {
    }

    public static abstract class UpdateListener implements Response.Listener<JSONObject>,
            Response.ErrorListener {
    }
}
