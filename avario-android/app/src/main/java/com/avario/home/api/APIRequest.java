package com.avario.home.api;


import android.util.Base64;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonRequest;
import com.avario.home.core.StateArray;
import com.avario.home.exception.AvarioException;
import com.avario.home.util.RefStringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


/**
 * Base class for API requests over to the server
 *
 * Created by aeroheart-c6 on 08/02/2017.
 */
abstract class APIRequest<T> extends JsonRequest<T> {
    private String username;
    private String password;

    APIRequest (RequestSpec spec, APIRequestListener listener) {
        super(
            spec.method,
            spec.url,
            spec.payload,
            listener,
            listener
        );

        this.username = spec.username;
        this.password = spec.password;

        this.setRetryPolicy(new DefaultRetryPolicy(
            5000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers;

        headers = new HashMap<>();
        headers.put("Authorization", "Basic " + Base64.encodeToString(
            (this.username + ":" + this.password).getBytes(),
            Base64.DEFAULT
        ));

        return headers;
    }

    /*
     ***********************************************************************************************
     * Request Spec
     ***********************************************************************************************
     */
    static class RequestSpec {
        static RequestSpec fromJSONSpec(JSONObject specJSON) throws AvarioException,
                                                                    JSONException {
            StateArray states = StateArray.getInstance();

            Map<String, String> result = RefStringUtil.processUrl(specJSON.getString("url"));

            String method = specJSON.getString("method").toUpperCase(),
                   confId = result.get("confId");

            RequestSpec spec;

            spec = new RequestSpec();
            spec.url = result.get("url");
            spec.method = method.equals("POST") ? Request.Method.POST
                : method.equals("PUT") ? Request.Method.PUT
                : method.equals("DELETE") ? Request.Method.DELETE
                : method.equals("OPTIONS") ? Request.Method.OPTIONS
                : method.equals("HEAD") ? Request.Method.HEAD
                : method.equals("TRACE") ? Request.Method.TRACE
                : Request.Method.GET;

            spec.payload = specJSON.optString("payload");
            spec.username = states.getHTTPUsername(confId);
            spec.password = states.getHTTPPassword(confId);

            return spec;
        }

        int method;
        String url;
        String payload;
        String username;
        String password;

        private RequestSpec() {}
    }
}
