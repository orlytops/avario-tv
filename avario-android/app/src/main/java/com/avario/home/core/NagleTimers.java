package com.avario.home.core;


import com.avario.home.Application;
import com.avario.home.util.Log;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by aeroheart-c6 on 10/03/2017.
 */
public class NagleTimers {
    private static final String TAG = "Avario/NagleTimers";

    private static Map<String, Runnable> runnables = new HashMap<>();

    public static void reset(String entityId, Runnable runnable, int delay) {
        if (entityId == null)
            return;

        ExpireRunnable expireRunnable = new ExpireRunnable(entityId, runnable);

        NagleTimers.invalidate(entityId);
        NagleTimers.runnables.put(entityId, expireRunnable);

        if (Application.workHandler != null) {
            Application.workHandler.postDelayed(expireRunnable, delay);
        }
    }

    public static void invalidate(String entityId) {
        if (entityId == null || !NagleTimers.runnables.containsKey(entityId))
            return;

        if (Application.workHandler != null) {
            Application.workHandler.removeCallbacks(NagleTimers.runnables.get(entityId));
        }
        NagleTimers.forget(entityId);
    }

    public static void forget(String entityId) {
        if (entityId == null || !NagleTimers.runnables.containsKey(entityId))
            return;

        NagleTimers.runnables.remove(entityId);
    }

    /*
     ***********************************************************************************************
     * Inner Classes
     ***********************************************************************************************
     */
    private static class ExpireRunnable implements Runnable {
        private String entityId;
        private Runnable runnable;

        public ExpireRunnable(String entityId, Runnable runnable) {
            this.entityId = entityId;
            this.runnable = runnable;
        }

        @Override
        public void run() {
            Log.d(TAG, "Nagle Timer expired with id: " + this.entityId + "\n");

            if (this.runnable != null)
                this.runnable.run();

            NagleTimers.forget(this.entityId);
        }
    }
}
