package com.avario.home.api;


import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.avario.home.core.APITimers;
import com.avario.home.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;


/**
 * Created by aeroheart-c6 on 08/02/2017.
 */
public class APIMultiListener<T> extends APIRequestListener<T> {
    public static final String TAG = "Avario/APIMultiListener";

    protected Queue<Request> requests;
    protected APIClient client;

    protected List<T> responses;
    protected List<VolleyError> errors;

    public APIMultiListener(String timerId, String[] entityIds) {
        super(timerId, entityIds);

        this.client = APIClient.getInstance();
        this.requests = null;

        this.requests = new LinkedList<>();
        this.responses = new ArrayList<>();
        this.errors = new ArrayList<>();
    }

    public void add(Request request) {
        this.requests.add(request);
    }

    public Request pop() {
        return this.requests.remove();
    }

    @Override
    public void onDone(T response, VolleyError error) {
        Log.i(TAG, "A request was finished");

        try {
            this.client.sendRequest(this.pop());
        }
        catch (NoSuchElementException | NullPointerException exception) {
            this.startTimer();
            this.onRequestsDone();
        }
    }

    @Override
    public void onResponse(T response) {
        Log.i(TAG, "Received a successful response");

        this.responses.add(response);
        this.errors.add(null);

        this.onDone(response, null);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.i(TAG, "Received an error");

        this.responses.add(null);
        this.errors.add(error);

        this.onDone(null, error);
    }

    public void onRequestsDone() {
        APITimers.unlock(this.timerId);
    }
}
