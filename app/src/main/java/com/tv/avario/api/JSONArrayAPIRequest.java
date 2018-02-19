package com.tv.avario.api;


import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.UnsupportedEncodingException;


/**
 * Created by aeroheart-c6 on 19/02/2017.
 */
public class JSONArrayAPIRequest extends APIRequest<JSONArray> {
    public JSONArrayAPIRequest(APIRequest.RequestSpec spec, APIRequestListener listener) {
        super(spec, listener);
    }

    @Override
    protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
        try {
            return Response.success(
                new JSONArray(new String(
                    response.data,
                    HttpHeaderParser.parseCharset(response.headers)
                )),
                HttpHeaderParser.parseCacheHeaders(response)
            );
        } catch (UnsupportedEncodingException | JSONException exception) {
            return Response.error(new ParseError(exception));
        }
    }
}
