package com.avario.home.api;


import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.avario.home.Application;
import com.avario.home.core.APITimers;
import com.avario.home.util.Log;


/**
 * Created by aeroheart-c6 on 08/02/2017.
 */
public abstract class APIRequestListener<T> implements Response.Listener<T>,
                                                       Response.ErrorListener {
    private static final String TAG = "Avario/APIListener";

    protected String timerId;
    protected String[] entityIds;

    public APIRequestListener(String timerId, String[] entityIds) {
        this.timerId = timerId;
        this.entityIds = entityIds;
    }

    public void onDone(T response, VolleyError error) {
        Log.i(TAG, "Request finished..");
        APITimers.unlock(this.timerId);
    }

    @Override
    public void onResponse(T response) {
        Log.i(TAG, "Request successful..");
        this.onDone(response, null);
        this.startTimer();
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        Log.i(TAG, "Request failed..", error);
        this.onDone(null, error);

        this.forceTimerExpire();
    }

    protected void forceTimerExpire() {
        Application.mainHandler.post(APITimers.getRunnable(this.timerId, this.entityIds));
    }

    protected void startTimer() {
        APITimers.reset(this.timerId, this.entityIds);
    }
}
