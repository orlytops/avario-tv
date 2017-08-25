package com.avariohome.avario.api;


import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.UnsupportedEncodingException;


/**
 * Created by aeroheart-c6 on 19/02/2017.
 */
public class StringAPIRequest extends APIRequest<String> {
    public StringAPIRequest(APIRequest.RequestSpec spec, APIRequestListener listener) {
        super(spec, listener);
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        try {
            return Response.success(
                new String(response.data, HttpHeaderParser.parseCharset(response.headers)),
                HttpHeaderParser.parseCacheHeaders(response)
            );
        }
        catch (UnsupportedEncodingException exception) {
            return Response.error(new ParseError(exception));
        }
    }
}
